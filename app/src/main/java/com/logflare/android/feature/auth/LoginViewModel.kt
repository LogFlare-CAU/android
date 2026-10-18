package com.logflare.android.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.logflare.android.data.ServerConfigRepository
import com.logflare.android.data.session.RecentServersRepository
import com.logflare.android.data.session.SignInResult
import com.logflare.android.data.session.SignInUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Error texts are keys the screen resolves from strings.xml, so the VM stays free of Android resources. */
enum class LoginError { EnterUrl, EnterUsername, EnterPassword, UrlScheme, InvalidCredentials, RateLimited, Unreachable }

data class LoginUiState(
    val url: String = "",
    val username: String = "",
    val password: String = "",
    val urlError: LoginError? = null,
    val usernameError: LoginError? = null,
    val passwordError: LoginError? = null,
    val submitting: Boolean = false,
    val formError: LoginError? = null,
    val recentServers: List<String> = emptyList(),
    val recentSheetOpen: Boolean = false,
)

sealed interface LoginEvent {
    /** Sign-in succeeded. The session flip already redirects to Home; this is for routes that navigate themselves. */
    data object SignedIn : LoginEvent
}

@HiltViewModel
class LoginViewModel internal constructor(
    private val signIn: suspend (serverUrl: String, username: String, password: String) -> SignInResult,
    savedServerUrl: Flow<String?>,
    recentServers: Flow<List<String>>,
) : ViewModel() {

    @Inject
    constructor(
        signIn: SignInUseCase,
        serverConfig: ServerConfigRepository,
        recentServers: RecentServersRepository,
    ) : this(signIn::invoke, serverConfig.serverUrl, recentServers.servers)

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state

    private val _events = Channel<LoginEvent>(Channel.BUFFERED)
    val events: Flow<LoginEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            recentServers.collect { list -> _state.update { it.copy(recentServers = list) } }
        }
        viewModelScope.launch {
            val saved = savedServerUrl.first()
            if (!saved.isNullOrBlank()) _state.update { if (it.url.isEmpty()) it.copy(url = saved) else it }
        }
    }

    fun onUrl(value: String) = _state.update { it.copy(url = value, urlError = null, formError = null) }

    fun onUsername(value: String) = _state.update { it.copy(username = value, usernameError = null, formError = null) }

    fun onPassword(value: String) = _state.update { it.copy(password = value, passwordError = null, formError = null) }

    fun openRecent() = _state.update { it.copy(recentSheetOpen = true) }

    fun dismissRecent() = _state.update { it.copy(recentSheetOpen = false) }

    fun pickRecent(url: String) = _state.update { it.copy(url = url, urlError = null, formError = null, recentSheetOpen = false) }

    fun submit() {
        val current = _state.value
        if (current.submitting) return
        val url = current.url.trim()
        val username = current.username.trim()
        val urlError = when {
            url.isEmpty() -> LoginError.EnterUrl
            !url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true) -> LoginError.UrlScheme
            else -> null
        }
        val usernameError = if (username.isEmpty()) LoginError.EnterUsername else null
        val passwordError = if (current.password.isEmpty()) LoginError.EnterPassword else null
        if (urlError != null || usernameError != null || passwordError != null) {
            _state.update { it.copy(urlError = urlError, usernameError = usernameError, passwordError = passwordError) }
            return
        }
        _state.update { it.copy(submitting = true, formError = null) }
        viewModelScope.launch {
            val result = signIn(url, username, current.password)
            val formError = when (result) {
                SignInResult.Success -> null
                SignInResult.InvalidCredentials -> LoginError.InvalidCredentials
                SignInResult.RateLimited -> LoginError.RateLimited
                is SignInResult.ServerUnreachable -> LoginError.Unreachable
            }
            _state.update { it.copy(submitting = false, formError = formError) }
            if (result == SignInResult.Success) _events.send(LoginEvent.SignedIn)
        }
    }
}
