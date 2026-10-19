package com.logflare.android.push

import com.logflare.android.data.session.SessionKey
import com.logflare.android.data.store.CacheStore
import kotlinx.serialization.KSerializer

/** [CacheStore] that can simulate a sign-out cleanup ([clear]) racing the next guarded write. */
class ClearingCacheStore(private val delegate: CacheStore) : CacheStore by delegate {
    var clearBeforeNextWrite = false

    override suspend fun <T> write(
        session: SessionKey,
        key: String,
        value: T,
        serializer: KSerializer<T>,
        fetchedAt: Long,
        generation: Long?,
    ): Boolean {
        if (clearBeforeNextWrite) {
            clearBeforeNextWrite = false
            delegate.clear(session)
        }
        return delegate.write(session, key, value, serializer, fetchedAt, generation)
    }
}
