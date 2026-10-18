package com.logflare.android.data

import com.example.logflare.core.network.auth.AuthTokenProvider
import com.logflare.android.di.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Interim [AuthTokenProvider]: mirrors [AuthRepository.token] into memory so the OkHttp
 * interceptor can read it synchronously. Replaced by SessionRepository in a later task.
 */
@Singleton
class AuthRepositoryTokenProvider @Inject constructor(
    auth: AuthRepository,
    @ApplicationScope scope: CoroutineScope,
) : AuthTokenProvider {
    private val state = MutableStateFlow<String?>(null)

    init {
        auth.token.onEach { state.value = it }.launchIn(scope)
    }

    override fun currentToken(): String? = state.value
}
