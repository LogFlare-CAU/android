package com.logflare.android.data.session

import com.logflare.android.data.store.CacheStore
import com.logflare.android.data.store.attempt
import com.logflare.android.push.NotificationHistory
import com.logflare.android.push.PushRegistrar
import javax.inject.Inject
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/**
 * Signs out and removes the session's local data (spec §5): cache, notification history and the
 * session's alert settings (stored in the cache). Device settings — theme, recent servers, the
 * server URL — stay.
 */
class SignOutUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val pushRegistrar: PushRegistrar,
    private val notificationHistory: NotificationHistory,
    private val cacheStore: CacheStore,
) {
    /** Runs to the end even if the caller is cancelled; takes at most ~3 s for the server. */
    suspend operator fun invoke() = withContext(NonCancellable) {
        val session = sessionRepository.state.value as? SessionState.SignedIn
        // Best effort, before the token is gone: the DELETE needs the session's bearer. Pushes that
        // still arrive after a failed unregister are dropped while signed out.
        if (session != null) pushRegistrar.unregister()
        sessionRepository.signOut()
        // Cleared after sign-out: once signed out, no store starts a fetch for this key that could
        // write it back.
        if (session != null) {
            attempt { notificationHistory.clear(session.key) }
            attempt { cacheStore.clear(session.key) }
        }
    }
}
