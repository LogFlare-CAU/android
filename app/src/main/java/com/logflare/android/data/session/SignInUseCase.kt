package com.logflare.android.data.session

import com.example.logflare.core.model.AccessAuthParams
import com.example.logflare.core.model.StringResponse
import com.example.logflare.core.model.UserAuthParams
import com.example.logflare.core.model.UserDTO
import com.example.logflare.core.network.LogflareApi
import com.example.logflare.core.network.host.MutableBaseUrlProvider
import com.example.logflare.core.network.host.ServerNotConfiguredException
import com.logflare.android.data.ServerConfigRepository
import com.logflare.android.data.store.attempt
import javax.inject.Inject
import retrofit2.HttpException

sealed interface SignInResult {
    data object Success : SignInResult

    data object RateLimited : SignInResult

    data object InvalidCredentials : SignInResult

    /** `POST /user/auth/access` answered 404: Access login is not enabled on this server. */
    data object AccessUnavailable : SignInResult

    /** No answer from the server (network, bad URL) or an answer that isn't a sign-in decision. */
    data class ServerUnreachable(val cause: Throwable) : SignInResult
}

/**
 * Signs in to [serverUrl]: authenticates, loads the profile and stores the session in one write,
 * then remembers the server. Push registration follows in the background ([com.logflare.android.push.PushRegistrar])
 * and is never waited for.
 */
class SignInUseCase @Inject constructor(
    private val api: LogflareApi,
    private val sessionRepository: SessionRepository,
    private val baseUrlProvider: MutableBaseUrlProvider,
    private val recentServers: RecentServersRepository,
    private val signOut: SignOutUseCase,
    private val accessCredentials: AccessCredentialStore,
) {
    suspend operator fun invoke(serverUrl: String, username: String, password: String): SignInResult {
        val url = ServerConfigRepository.normalizeServerUrl(serverUrl)
        if (url.isEmpty()) return SignInResult.ServerUnreachable(ServerNotConfiguredException())

        // Switching accounts: the previous session is signed out fully (unregister + its data)
        // before requests go to the new server. A password login does not keep an Access token.
        if (sessionRepository.state.value is SessionState.SignedIn) signOut()
        accessCredentials.clear()

        val previousUrl = baseUrlProvider.getBaseUrl()
        val result = signIn(url, username, password)
        // A failed attempt doesn't replace the saved server. (With none saved there is nothing to restore.)
        if (result != SignInResult.Success && previousUrl != null && previousUrl != url) {
            attempt { baseUrlProvider.setBaseUrl(previousUrl) }
        }
        return result
    }

    /**
     * Access login. The service token is stored before the request so
     * [com.example.logflare.core.network.auth.AccessHeaderInterceptor] can attach it, and removed
     * again if sign-in does not succeed.
     */
    suspend fun signInWithAccess(
        serverUrl: String,
        clientId: String,
        secret: String,
        keepLoggedIn: Boolean = true,
    ): SignInResult {
        val url = ServerConfigRepository.normalizeServerUrl(serverUrl)
        if (url.isEmpty()) return SignInResult.ServerUnreachable(ServerNotConfiguredException())
        if (sessionRepository.state.value is SessionState.SignedIn) signOut()

        val previousUrl = baseUrlProvider.getBaseUrl()
        // A keystore failure surfaces as a sign-in failure, not a crash.
        attempt { accessCredentials.save(url, clientId, secret) }.onFailure { return SignInResult.ServerUnreachable(it) }
        val result = accessSignIn(url, clientId, keepLoggedIn)
        if (result != SignInResult.Success) {
            accessCredentials.clear()
            if (previousUrl != null && previousUrl != url) {
                attempt { baseUrlProvider.setBaseUrl(previousUrl) }
            }
        }
        return result
    }

    private suspend fun signIn(url: String, username: String, password: String): SignInResult {
        val response = attempt {
            baseUrlProvider.setBaseUrl(url)
            api.authenticate(UserAuthParams(username, password, keep_logged_in = true))
        }.getOrElse { return it.toResult() }
        return establish(url, response, username)
    }

    private suspend fun accessSignIn(url: String, clientId: String, keepLoggedIn: Boolean): SignInResult {
        val response = attempt {
            baseUrlProvider.setBaseUrl(url)
            api.authenticateAccess(AccessAuthParams(keep_logged_in = keepLoggedIn))
        }.getOrElse { return it.toAccessResult() }
        return establish(url, response, clientId)
    }

    private suspend fun establish(url: String, response: StringResponse, usernameFallback: String): SignInResult {
        val jwt = response.data
        if (!response.success || jwt.isNullOrBlank()) {
            return if (response.error_code == HTTP_TOO_MANY_REQUESTS) SignInResult.RateLimited else SignInResult.InvalidCredentials
        }
        val bearer = "Bearer $jwt"

        // Loaded before the session is stored so the session key is final in a single write.
        // Without it the key falls back to [usernameFallback]; sign-in still succeeds.
        val user: UserDTO? = attempt { api.getMeAs(bearer) }.getOrNull()?.takeIf { it.success }?.data
        attempt {
            if (user != null) sessionRepository.signIn(url, bearer, user) else sessionRepository.signIn(url, bearer, usernameFallback)
        }.onFailure { return SignInResult.ServerUnreachable(it) }

        attempt { recentServers.remember(url) }
        return SignInResult.Success
    }

    private fun Throwable.toAccessResult(): SignInResult = when {
        this is HttpException && code() == HTTP_NOT_FOUND -> SignInResult.AccessUnavailable
        else -> toResult()
    }

    private fun Throwable.toResult(): SignInResult = when {
        this is HttpException && code() == HTTP_TOO_MANY_REQUESTS -> SignInResult.RateLimited
        this is HttpException && (code() == HTTP_UNAUTHORIZED || code() == HTTP_FORBIDDEN) -> SignInResult.InvalidCredentials
        else -> SignInResult.ServerUnreachable(this)
    }

    private companion object {
        const val HTTP_UNAUTHORIZED = 401
        const val HTTP_FORBIDDEN = 403
        const val HTTP_NOT_FOUND = 404
        const val HTTP_TOO_MANY_REQUESTS = 429
    }
}
