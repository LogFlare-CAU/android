package com.logflare.android.data.session

import com.example.logflare.core.network.LogflareApi
import com.example.logflare.core.network.host.ProbeBaseUrl
import com.logflare.android.data.ServerConfigRepository
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import retrofit2.HttpException

sealed interface AuthModesResult {
    /** Known mode names, in the server's order. Empty means the server allows no new login. */
    data class Ready(val modes: List<String>) : AuthModesResult

    data object Unreachable : AuthModesResult
}

/**
 * Reads `GET /user/auth/modes` for [serverUrl]. A 404 (server from before the route) is
 * password-only. The call is tagged with [ProbeBaseUrl], so it goes to [serverUrl] (https only)
 * without touching the saved server URL and without the saved session's credentials.
 */
class AuthModesLoader @Inject constructor(
    private val api: LogflareApi,
) {
    suspend fun load(serverUrl: String): AuthModesResult {
        val url = ServerConfigRepository.normalizeServerUrl(serverUrl)
        if (url.isEmpty()) return AuthModesResult.Unreachable
        return try {
            val response = api.authModes(ProbeBaseUrl(url))
            val modes = response.data?.modes.orEmpty().filter { it == "password" || it == "access" }
            if (!response.success && modes.isEmpty()) AuthModesResult.Unreachable else AuthModesResult.Ready(modes)
        } catch (e: HttpException) {
            if (e.code() == HTTP_NOT_FOUND) AuthModesResult.Ready(listOf("password")) else AuthModesResult.Unreachable
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            AuthModesResult.Unreachable
        }
    }

    private companion object {
        const val HTTP_NOT_FOUND = 404
    }
}
