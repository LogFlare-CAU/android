package com.logflare.android.data.store

import com.logflare.android.data.session.SessionKey
import com.logflare.android.data.session.SessionState
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** The server answered with `success: false`. [message] is the server's message. */
class ApiException(message: String, val errorCode: Int?) : IOException(message)

/** A store was used while nobody is signed in, or the session changed during the call. */
class NotSignedInException : IllegalStateException("Not signed in")

/** Returns `data` of a successful envelope, or throws [ApiException]. */
internal fun <T> requireData(success: Boolean, message: String, errorCode: Int?, data: T?): T {
    requireSuccess(success, message, errorCode)
    return data ?: throw ApiException("Empty response: $message", errorCode)
}

internal fun requireSuccess(success: Boolean, message: String, errorCode: Int?) {
    if (!success) throw ApiException(message, errorCode)
}

internal fun <A, B> Resource<A>.mapData(transform: (A) -> B): Resource<B> =
    Resource(data?.let(transform), refreshing, error, fetchedAt)

/** Like [runCatching], but never swallows cancellation. */
internal inline fun <T> attempt(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

/**
 * The signed-in session's flow from [open], restarted only when the session's [SessionKey]
 * changes. While signed out it emits a single empty [Resource].
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun <T> StateFlow<SessionState>.whileSignedIn(open: (SessionKey) -> Flow<Resource<T>>): Flow<Resource<T>> =
    map { (it as? SessionState.SignedIn)?.key }
        .distinctUntilChanged()
        .flatMapLatest { key -> if (key == null) flowOf(Resource(null, false, null, null)) else open(key) }

/**
 * Pull-to-refresh: runs [refresh] for the signed-in session and returns its outcome, or a
 * [NotSignedInException] failure when signed out.
 */
internal suspend fun StateFlow<SessionState>.refreshSignedIn(
    refresh: suspend (SessionKey) -> Result<Unit>,
): Result<Unit> {
    val session = (value as? SessionState.SignedIn)?.key ?: return Result.failure(NotSignedInException())
    return refresh(session)
}

/** The current session; throws [NotSignedInException] when signed out. */
internal fun StateFlow<SessionState>.signedIn(): SessionState.SignedIn =
    value as? SessionState.SignedIn ?: throw NotSignedInException()

/**
 * Runs [block] for [session] and checks the session before and after: if the user signed out or
 * switched accounts meanwhile, the result is rejected ([NotSignedInException]) rather than cached
 * under [session]. The interceptor adds the header of whoever is signed in when the request
 * goes out.
 */
internal inline fun <T> StateFlow<SessionState>.fetchAs(session: SessionKey, block: () -> T): T {
    requireSignedInAs(session)
    return block().also { requireSignedInAs(session) }
}

/**
 * Throws [NotSignedInException] when [session] is no longer the signed-in one, so a late fetch
 * never goes out (or is cached) under another user's token.
 */
internal fun StateFlow<SessionState>.requireSignedInAs(session: SessionKey) {
    if (signedIn().key != session) throw NotSignedInException()
}
