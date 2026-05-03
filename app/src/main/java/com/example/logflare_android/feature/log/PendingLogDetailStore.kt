package com.example.logflare_android.feature.log

import com.example.logflare_android.ui.common.LogCardInfo
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the log row the user tapped right before navigating to [LogDetailScreen].
 * Not part of data/network layer — avoids stashing UI-only state in [LogsRepository].
 */
@Singleton
class PendingLogDetailStore @Inject constructor() {
    private val lock = Any()
    private var pending: LogCardInfo? = null

    fun setPending(log: LogCardInfo) {
        synchronized(lock) {
            pending = log
        }
    }

    /**
     * Returns the pending row once and clears it so a later detail open does not reuse stale data.
     */
    fun takePending(): LogCardInfo? {
        synchronized(lock) {
            val value = pending
            pending = null
            return value
        }
    }
}
