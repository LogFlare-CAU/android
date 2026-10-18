package com.logflare.android.push

import com.example.logflare.core.model.FcmTokenParams
import com.example.logflare.core.network.LogflareApi
import com.logflare.android.data.DeviceRepository
import com.logflare.android.data.session.SessionKey
import com.logflare.android.data.session.SessionRepository
import com.logflare.android.data.session.SessionState
import com.logflare.android.data.store.attempt
import com.logflare.android.di.ApplicationScope
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import retrofit2.HttpException

/** Firebase behind a seam so [PushRegistrar] is testable without it. Implemented by [DeviceRepository]. */
interface PushTokenSource {
    /** Loads the FCM config (server, else the persisted copy) and initializes Firebase. */
    suspend fun ensureFirebase(): Boolean

    /** Initializes Firebase from the persisted FCM config only, without calling the server. */
    suspend fun ensureFirebaseFromCache(): Boolean

    /** This device's FCM token, or null when Firebase isn't ready. */
    suspend fun getFcmTokenOrNull(): String?
}

/**
 * Keeps this device's FCM token registered with the server for the signed-in session, in the
 * background — never on a screen's loading path.
 *
 * After [start], every new sign-in (new [SessionKey] or new bearer) registers once (HTTP 200 and 409 "already
 * registered" both count). A token refresh ([onNewToken]) re-registers. [unregister] runs before
 * sign-out, while the session's bearer is still available to the auth interceptor.
 *
 * The accepted (session, FCM token) pair is persisted ([PushRegistrationStore]): after a process
 * restart the same pair is not sent again (no `GET /fcm/data`, no `POST /fcm/token`). It is
 * forgotten on [unregister] and whenever the session signs out, so the next sign-in registers.
 */
@Singleton
class PushRegistrar internal constructor(
    private val sessions: StateFlow<SessionState>,
    private val api: LogflareApi,
    private val tokens: PushTokenSource,
    private val registrations: PushRegistrationStore,
    private val scope: CoroutineScope,
) {
    @Inject
    constructor(
        sessionRepository: SessionRepository,
        api: LogflareApi,
        deviceRepository: DeviceRepository,
        registrations: DataStorePushRegistrationStore,
        @ApplicationScope scope: CoroutineScope,
    ) : this(sessionRepository.state, api, deviceRepository, registrations, scope)

    private val started = AtomicBoolean(false)

    /** Registration of the current session, if still running. */
    private val registration = AtomicReference<Job?>(null)

    /** Serializes register/unregister calls so a DELETE never overlaps a POST. */
    private val lock = Mutex()

    /**
     * Bearer of the session being signed out: set by [unregister] so a token refresh
     * ([onNewToken]) can't register again after the DELETE. A later sign-in has a new bearer.
     */
    @Volatile
    private var closedBearer: String? = null

    /** FCM token the server last accepted, so [unregister] doesn't depend on Firebase. */
    @Volatile
    private var registeredToken: String? = null

    private val _registeredFor = MutableStateFlow<SessionKey?>(null)

    /** The signed-in session this device is registered for; null while not (yet) registered. */
    val registeredFor: StateFlow<SessionKey?> = _registeredFor.asStateFlow()

    /** Starts following the session. Idempotent; called from `LogflareApp.onCreate`. */
    fun start() {
        if (!started.compareAndSet(false, true)) return
        scope.launch {
            // Loading is not "signed out": it must not forget the persisted registration.
            sessions.filter { it != SessionState.Loading }
                .map { it as? SessionState.SignedIn }
                // A new sign-in has a new bearer even for the same key, so it registers again
                // even if the SignedOut in between was conflated away.
                .distinctUntilChangedBy { it?.let { s -> s.key to s.token } }
                .collect { session ->
                    registration.getAndSet(null)?.cancel()
                    _registeredFor.value = null
                    if (session != null) {
                        registration.set(scope.launch { registerCurrentToken(session) })
                    } else {
                        // Also after a local sign-out (401), which never unregisters.
                        lock.withLock { forgetRegistration() }
                    }
                }
        }
    }

    /** Firebase issued a new token: register it for the signed-in session (no-op when signed out). */
    suspend fun onNewToken(token: String) {
        val session = sessions.first { it != SessionState.Loading } as? SessionState.SignedIn ?: return
        register(session, token)
    }

    /**
     * Removes this device's token from the server, giving up after [timeoutMs]. Must run while
     * still signed in. Returns whether the server confirmed it.
     */
    suspend fun unregister(timeoutMs: Long = 3_000): Boolean = withTimeoutOrNull(timeoutMs) {
        closedBearer = (sessions.value as? SessionState.SignedIn)?.token
        // A registration landing after the DELETE would leave the device registered.
        registration.getAndSet(null)?.cancelAndJoin()
        lock.withLock {
            // Signing out: the next sign-in registers again, whatever the server answers now.
            forgetRegistration()
            val token = registeredToken ?: tokens.getFcmTokenOrNull() ?: return@withLock false
            val removed = attempt { api.unregisterFcmToken(FcmTokenParams(token)).success }.getOrDefault(false)
            if (removed) {
                registeredToken = null
                _registeredFor.value = null
            }
            removed
        }
    } ?: false

    private suspend fun registerCurrentToken(session: SessionState.SignedIn) {
        if (restoreRegistration(session)) return
        if (!tokens.ensureFirebase()) return
        val token = tokens.getFcmTokenOrNull() ?: return
        register(session, token)
    }

    /**
     * The persisted registration is for [session] and this device's current FCM token (e.g. after
     * a process restart): the server already has it, so nothing is sent. Returns whether so.
     */
    private suspend fun restoreRegistration(session: SessionState.SignedIn): Boolean {
        val last = attempt { registrations.get() }.getOrNull() ?: return false
        if (last.session != session.key || !tokens.ensureFirebaseFromCache()) return false
        val token = tokens.getFcmTokenOrNull() ?: return false
        if (token != last.fcmToken) return false
        lock.withLock {
            if (isCurrent(session)) {
                registeredToken = token
                _registeredFor.value = session.key
            }
        }
        return true
    }

    private suspend fun forgetRegistration() {
        attempt { registrations.set(null) }
    }

    /** The session may have changed while Firebase was getting ready. */
    private fun isCurrent(session: SessionState.SignedIn): Boolean =
        (sessions.value as? SessionState.SignedIn)?.key == session.key && session.token != closedBearer

    private suspend fun register(session: SessionState.SignedIn, token: String) = lock.withLock {
        if (!isCurrent(session)) return@withLock
        val accepted = attempt { api.registerFcmToken(session.token, FcmTokenParams(token)) }
            .fold(
                onSuccess = { it.success || it.errorCode == HTTP_CONFLICT },
                onFailure = { it is HttpException && it.code() == HTTP_CONFLICT },
            )
        if (accepted) {
            registeredToken = token
            _registeredFor.value = session.key
            attempt { registrations.set(PushRegistration(session.key, token)) }
        }
    }

    private companion object {
        /** The server already has this token for this user. */
        const val HTTP_CONFLICT = 409
    }
}
