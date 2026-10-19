package com.logflare.android.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.logflare.android.data.ServerConfigRepository
import com.logflare.android.data.session.AuthModesLoader
import com.logflare.android.data.session.AuthModesResult
import com.logflare.android.data.session.RecentServersRepository
import com.logflare.android.data.session.SignInResult
import com.logflare.android.data.session.SignInUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.net.URI
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Error texts are keys the screen resolves from strings.xml, so the VM stays free of Android resources. */
enum class LoginError {
    EnterUrl,
    EnterUsername,
    EnterPassword,
    EnterClientId,
    EnterClientSecret,
    UrlScheme,
    NeedsHttps,
    InvalidCredentials,
    RateLimited,
    Unreachable,
    AccessRejected,
    AccessUnavailable,
    QrInvalid,
}

enum class LoginMode { Password, Access }

/** Form is the login screen; Scan and Manual are the two ways to get an Access token; Confirm follows either. */
enum class LoginStep { Form, Scan, Manual, Confirm }

/**
 * The Access client secret is deliberately not here: it lives in a private field of the
 * [LoginViewModel] (QR) or in the manual-entry fields of the screen, so this object is safe to log.
 */
data class LoginUiState(
    val url: String = "",
    val username: String = "",
    val password: String = "",
    val urlError: LoginError? = null,
    val usernameError: LoginError? = null,
    val passwordError: LoginError? = null,
    val submitting: Boolean = false,
    val modesLoading: Boolean = false,
    val formError: LoginError? = null,
    val recentServers: List<String> = emptyList(),
    val recentSheetOpen: Boolean = false,
    /** Null until this URL's modes have been read (or when the lookup failed). An empty list means no sign-in method. */
    val modes: List<LoginMode>? = null,
    val selectedMode: LoginMode = LoginMode.Password,
    val step: LoginStep = LoginStep.Form,
    /** Host (and non-default port) of the server the Access token will be used on. */
    val accessServer: String = "",
    val accessClientMasked: String = "",
    val keepLoggedIn: Boolean = true,
    val scanError: LoginError? = null,
    /** Server of the manual-entry step; starts as the password tab's URL. */
    val manualServer: String = "",
    val manualServerError: LoginError? = null,
    val manualIdError: LoginError? = null,
    val manualSecretError: LoginError? = null,
) {
    /** The server answered, and password sign-in is not among its methods (an empty list is the no-methods state). */
    val passwordBlocked: Boolean get() = modes != null && modes.isNotEmpty() && LoginMode.Password !in modes
}

sealed interface LoginEvent {
    /** Sign-in succeeded. The session flip already redirects to Home; this is for routes that navigate themselves. */
    data object SignedIn : LoginEvent
}

/** Wait after the last keystroke before asking the server which sign-in methods it offers. */
internal const val MODES_DEBOUNCE_MS = 500L

@HiltViewModel
class LoginViewModel internal constructor(
    private val signIn: suspend (serverUrl: String, username: String, password: String) -> SignInResult,
    private val loadModes: suspend (serverUrl: String) -> AuthModesResult = { AuthModesResult.Ready(listOf("password")) },
    private val signInAccess: suspend (serverUrl: String, clientId: String, secret: String, keepLoggedIn: Boolean) -> SignInResult =
        { _, _, _, _ -> SignInResult.InvalidCredentials },
    savedServerUrl: Flow<String?>,
    recentServers: Flow<List<String>>,
) : ViewModel() {

    @Inject
    constructor(
        signIn: SignInUseCase,
        modes: AuthModesLoader,
        serverConfig: ServerConfigRepository,
        recentServers: RecentServersRepository,
    ) : this(
        signIn::invoke,
        modes::load,
        { url, id, secret, keep -> signIn.signInWithAccess(url, id, secret, keep) },
        serverConfig.serverUrl,
        recentServers.servers,
    )

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state

    private val _events = Channel<LoginEvent>(Channel.BUFFERED)
    val events: Flow<LoginEvent> = _events.receiveAsFlow()

    /** Held off [LoginUiState] so the secret is not in the state object. */
    private var pendingQr: AccessQr? = null

    /** Where the back arrow on the Confirm step returns to. */
    private var confirmOrigin = LoginStep.Scan
    private var modesFor: String? = null
    private var modesJob: Job? = null

    init {
        viewModelScope.launch {
            recentServers.collect { list -> _state.update { it.copy(recentServers = list) } }
        }
        viewModelScope.launch {
            val saved = savedServerUrl.first()
            if (!saved.isNullOrBlank() && _state.value.url.isEmpty()) {
                _state.update { it.copy(url = saved) }
                scheduleModes(0)
            }
        }
    }

    fun onUrl(value: String) {
        pendingQr = null
        modesFor = null
        modesJob?.cancel()
        _state.update {
            it.copy(
                url = value,
                urlError = null,
                formError = null,
                modes = null,
                modesLoading = false,
                step = LoginStep.Form,
                scanError = null,
                accessServer = "",
                accessClientMasked = "",
            )
        }
        scheduleModes(MODES_DEBOUNCE_MS)
    }

    fun onUsername(value: String) = _state.update { it.copy(username = value, usernameError = null, formError = null) }

    fun onPassword(value: String) = _state.update { it.copy(password = value, passwordError = null, formError = null) }

    fun openRecent() = _state.update { it.copy(recentSheetOpen = true) }

    fun dismissRecent() = _state.update { it.copy(recentSheetOpen = false) }

    fun pickRecent(url: String) {
        onUrl(url)
        _state.update { it.copy(recentSheetOpen = false) }
        scheduleModes(0)
    }

    fun selectMode(mode: LoginMode) {
        _state.update { it.copy(selectedMode = mode, formError = null, usernameError = null, passwordError = null) }
    }

    /** URL field finished (IME next): no need to wait for the debounce. Shows a URL problem, if any. */
    fun commitUrl() {
        val urlError = urlProblem(_state.value.url.trim())
        if (urlError != null) {
            _state.update { it.copy(urlError = urlError) }
            return
        }
        scheduleModes(0)
    }

    fun scan() = enterAccessStep(LoginStep.Scan)

    fun enterManual() = enterAccessStep(LoginStep.Manual)

    private fun enterAccessStep(step: LoginStep) {
        val current = _state.value
        if (current.submitting || current.selectedMode != LoginMode.Access) return
        pendingQr = null
        _state.update {
            it.copy(
                step = step,
                scanError = null,
                formError = null,
                // The password tab's URL (typed or saved) is only a starting point; it can be changed here.
                manualServer = if (step == LoginStep.Manual && it.manualServer.isEmpty()) it.url else it.manualServer,
                manualServerError = null,
                manualIdError = null,
                manualSecretError = null,
            )
        }
    }

    /** One step back: Confirm returns to where it came from, Scan and Manual return to the form. */
    fun back() {
        if (_state.value.submitting) return
        pendingQr = null
        _state.update {
            when (it.step) {
                LoginStep.Form -> it
                LoginStep.Confirm -> it.copy(
                    step = confirmOrigin,
                    scanError = null,
                    formError = null,
                    accessServer = "",
                    accessClientMasked = "",
                )
                LoginStep.Scan, LoginStep.Manual -> it.copy(
                    step = LoginStep.Form,
                    scanError = null,
                    formError = null,
                    manualServerError = null,
                    manualIdError = null,
                    manualSecretError = null,
                )
            }
        }
    }

    fun setKeepLoggedIn(value: Boolean) = _state.update { it.copy(keepLoggedIn = value) }

    fun onManualEdit() = _state.update { it.copy(manualServerError = null, manualIdError = null, manualSecretError = null) }

    fun onManualServer(value: String) = _state.update { it.copy(manualServer = value, manualServerError = null) }

    /** Manual entry. The server is the step's own field; the secret goes straight into a private field. */
    fun submitManual(clientId: String, secret: String) {
        val current = _state.value
        if (current.step != LoginStep.Manual) return
        val id = clientId.trim()
        val secretValue = secret.trim()
        val server = current.manualServer.trim()
        val serverError = urlProblem(server)
        val idError = if (id.isEmpty()) LoginError.EnterClientId else null
        val secretError = if (secretValue.isEmpty()) LoginError.EnterClientSecret else null
        if (serverError != null || idError != null || secretError != null) {
            _state.update {
                it.copy(manualServerError = serverError, manualIdError = idError, manualSecretError = secretError)
            }
            return
        }
        showConfirm(AccessQr(server.trimEnd('/'), id, secretValue), LoginStep.Manual)
    }

    fun onQr(raw: String) {
        if (_state.value.step != LoginStep.Scan) return
        val parsed = parseAccessQr(raw)
        if (parsed == null) {
            _state.update { it.copy(scanError = LoginError.QrInvalid) }
            return
        }
        showConfirm(parsed, LoginStep.Scan)
    }

    private fun showConfirm(qr: AccessQr, origin: LoginStep) {
        pendingQr = qr
        confirmOrigin = origin
        _state.update {
            it.copy(
                step = LoginStep.Confirm,
                accessServer = hostOf(qr.server),
                accessClientMasked = qr.maskedClientId(),
                scanError = null,
                formError = null,
            )
        }
    }

    fun confirmAccess() {
        val qr = pendingQr ?: return
        val current = _state.value
        if (current.submitting) return
        _state.update { it.copy(submitting = true, formError = null) }
        viewModelScope.launch {
            val result = signInAccess(qr.server, qr.clientId, qr.secret, current.keepLoggedIn)
            val formError = result.toAccessError()
            if (result == SignInResult.Success) pendingQr = null
            _state.update { it.copy(submitting = false, formError = formError) }
            if (result == SignInResult.Success) _events.send(LoginEvent.SignedIn)
        }
    }

    fun submit() {
        val current = _state.value
        if (current.submitting || current.modesLoading) return
        val url = current.url.trim()
        val urlError = urlProblem(url)
        if (urlError != null) {
            _state.update {
                it.copy(
                    urlError = urlError,
                    usernameError = if (current.username.trim().isEmpty()) LoginError.EnterUsername else null,
                    passwordError = if (current.password.isEmpty()) LoginError.EnterPassword else null,
                )
            }
            return
        }
        modesJob?.cancel()
        _state.update { it.copy(submitting = true, formError = null, urlError = null) }
        viewModelScope.launch {
            val modes = fetchModes(url, reportFailure = true) ?: return@launch
            if (LoginMode.Password !in modes) {
                // Shown by the form (LoginUiState.passwordBlocked); an empty list shows the no-methods box.
                _state.update { it.copy(submitting = false) }
                return@launch
            }
            if (_state.value.selectedMode != LoginMode.Password) {
                _state.update { it.copy(submitting = false) }
                return@launch
            }
            val username = _state.value.username.trim()
            val usernameError = if (username.isEmpty()) LoginError.EnterUsername else null
            val passwordError = if (_state.value.password.isEmpty()) LoginError.EnterPassword else null
            if (usernameError != null || passwordError != null) {
                _state.update { it.copy(submitting = false, usernameError = usernameError, passwordError = passwordError) }
                return@launch
            }
            val result = signIn(url, username, _state.value.password)
            val formError = when (result) {
                SignInResult.Success -> null
                SignInResult.InvalidCredentials -> LoginError.InvalidCredentials
                SignInResult.RateLimited -> LoginError.RateLimited
                SignInResult.AccessUnavailable -> LoginError.Unreachable
                is SignInResult.ServerUnreachable -> LoginError.Unreachable
            }
            _state.update { it.copy(submitting = false, formError = formError) }
            if (result == SignInResult.Success) _events.send(LoginEvent.SignedIn)
        }
    }

    /** Looks the modes up after [delayMs], for the URL as it is now. A URL that is not usable yet is left alone. */
    private fun scheduleModes(delayMs: Long) {
        modesJob?.cancel()
        val url = _state.value.url.trim()
        if (urlProblem(url) != null) return
        modesJob = viewModelScope.launch {
            if (delayMs > 0) delay(delayMs)
            fetchModes(url, reportFailure = false)
        }
    }

    /**
     * Modes for [url], from the cache or the server. Returns null when it failed, or when the URL
     * field changed meanwhile: that answer belongs to a URL the user has left, so it is dropped
     * and the newer lookup owns [LoginUiState.modesLoading].
     */
    private suspend fun fetchModes(url: String, reportFailure: Boolean): List<LoginMode>? {
        val known = _state.value.modes
        if (known != null && modesFor == url) return known
        _state.update { it.copy(modesLoading = true) }
        val result = try {
            loadModes(url)
        } catch (e: CancellationException) {
            _state.update { if (it.url.trim() == url) it.copy(modesLoading = false) else it }
            throw e
        }
        if (_state.value.url.trim() != url) return null
        return when (result) {
            AuthModesResult.Unreachable -> {
                // Unknown modes: the password form is tried anyway. Not cached, so the next lookup asks again.
                _state.update {
                    it.copy(
                        modesLoading = false,
                        submitting = if (reportFailure) false else it.submitting,
                        formError = if (reportFailure) LoginError.Unreachable else it.formError,
                    )
                }
                null
            }
            is AuthModesResult.Ready -> {
                val modes = result.modes.mapNotNull { it.toLoginMode() }.distinct()
                modesFor = url
                _state.update { it.copy(modesLoading = false, modes = modes) }
                modes
            }
        }
    }

    private fun SignInResult.toAccessError(): LoginError? = when (this) {
        SignInResult.Success -> null
        SignInResult.InvalidCredentials -> LoginError.AccessRejected
        SignInResult.RateLimited -> LoginError.RateLimited
        SignInResult.AccessUnavailable -> LoginError.AccessUnavailable
        is SignInResult.ServerUnreachable -> LoginError.Unreachable
    }
}

private fun String.toLoginMode(): LoginMode? = when (this) {
    "password" -> LoginMode.Password
    "access" -> LoginMode.Access
    else -> null
}

/** `logflare.example.com` or `logflare.example.com:8443`: what the user needs to recognise the server. */
internal fun hostOf(server: String): String = try {
    val uri = URI(server.trim())
    val host = uri.host
    if (host.isNullOrBlank()) server.trim() else if (uri.port != -1) "$host:${uri.port}" else host
} catch (_: Exception) {
    server.trim()
}

internal fun urlProblem(url: String): LoginError? = when {
    url.isEmpty() || url.trimEnd('/').let { it.equals("http:", true) || it.equals("https:", true) } -> LoginError.EnterUrl
    !url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true) -> LoginError.UrlScheme
    url.startsWith("http://", ignoreCase = true) -> LoginError.NeedsHttps
    else -> null
}
