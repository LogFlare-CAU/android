package com.logflare.android.data.session

import com.logflare.android.push.PushRegistrar
import javax.inject.Inject
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/**
 * Signs out (the user asked to): unregisters this device's push token, then [LocalSignOut]
 * removes the session and its local data (spec §5).
 */
class SignOutUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val pushRegistrar: PushRegistrar,
    private val localSignOut: LocalSignOut,
) {
    /** Runs to the end even if the caller is cancelled; takes at most ~3 s for the server. */
    suspend operator fun invoke() {
        withContext(NonCancellable) {
            // Best effort, before the token is gone: the DELETE needs the session's bearer. Pushes
            // that still arrive after a failed unregister are dropped while signed out.
            if (sessionRepository.state.value is SessionState.SignedIn) pushRegistrar.unregister()
            localSignOut()
        }
    }
}
