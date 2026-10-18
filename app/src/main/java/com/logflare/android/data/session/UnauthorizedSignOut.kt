package com.logflare.android.data.session

import com.example.logflare.core.network.HttpUnauthorizedAction
import com.logflare.android.di.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * What a 401 does: a [LocalSignOut] of the session whose token was rejected. A 401 for any other
 * token (a late answer to a previous session's request) or for a request without one is ignored.
 *
 * Push is not unregistered: the server rejects the token, so the DELETE would only 401 again.
 * [LocalSignOut] is [dagger.Lazy] because it is reached from the OkHttp client it would
 * otherwise have to be built with; it never blocks the OkHttp thread.
 */
@Singleton
class UnauthorizedSignOut @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val localSignOut: dagger.Lazy<LocalSignOut>,
    @ApplicationScope private val scope: CoroutineScope,
) : HttpUnauthorizedAction {
    override fun onUnauthorized(rejectedAuthorization: String?) {
        val rejected = rejectedAuthorization ?: return
        if (rejected != sessionRepository.currentToken()) return
        scope.launch { localSignOut.get()(onlyIfToken = rejected) }
    }
}
