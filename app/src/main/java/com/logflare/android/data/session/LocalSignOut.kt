package com.logflare.android.data.session

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.logflare.android.data.store.CacheStore
import com.logflare.android.data.store.attempt
import com.logflare.android.push.NotificationHistory
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/**
 * The on-device half of signing out (spec §5): ends the session and removes its cache and
 * notification history (the session's alert settings live in the cache). Device settings —
 * theme, recent servers, the server URL — stay. Never calls the server, so it is also what a
 * 401 does ([UnauthorizedSignOut]); [SignOutUseCase] adds the push unregister in front.
 */
class LocalSignOut internal constructor(
    private val sessionRepository: SessionRepository,
    private val notificationHistory: NotificationHistory,
    private val cacheStore: CacheStore,
    private val cancelNotifications: () -> Unit,
) {
    @Inject
    constructor(
        sessionRepository: SessionRepository,
        notificationHistory: NotificationHistory,
        cacheStore: CacheStore,
        @ApplicationContext context: Context,
    ) : this(sessionRepository, notificationHistory, cacheStore, {
        // Posted notifications carry the previous session's errors; their taps must not outlive it.
        NotificationManagerCompat.from(context).cancelAll()
    })

    /**
     * With [onlyIfToken], does nothing unless that is still the signed-in token, so a late 401
     * for a previous token can't end a newer session. Runs to the end even if the caller is
     * cancelled. Returns whether a session was signed out.
     */
    suspend operator fun invoke(onlyIfToken: String? = null): Boolean = withContext(NonCancellable) {
        val session = sessionRepository.state.value as? SessionState.SignedIn
        if (onlyIfToken != null && session?.token != onlyIfToken) return@withContext false
        val signedOut = sessionRepository.signOut(onlyIfToken)
        // Cleared after sign-out: once signed out, no store starts a fetch for this key that could
        // write it back.
        if (session != null && (signedOut || onlyIfToken == null)) {
            attempt { notificationHistory.clear(session.key) }
                .onFailure { Log.w(TAG, "Clearing notification history failed", it) }
            attempt { cacheStore.clear(session.key) }
                .onFailure { Log.w(TAG, "Clearing session cache failed", it) }
            attempt { cancelNotifications() }
                .onFailure { Log.w(TAG, "Cancelling notifications failed", it) }
        }
        signedOut
    }

    private companion object {
        const val TAG = "LocalSignOut"
    }
}
