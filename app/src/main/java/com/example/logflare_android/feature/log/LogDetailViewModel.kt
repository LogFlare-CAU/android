package com.example.logflare_android.feature.log

import androidx.lifecycle.ViewModel
import com.example.logflare_android.ui.common.LogCardInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LogDetailViewModel @Inject constructor(
    store: PendingLogDetailStore,
) : ViewModel() {
    private val logDetail: LogCardInfo? = store.takePending()

    fun getLogDetail(): LogCardInfo? = logDetail
}