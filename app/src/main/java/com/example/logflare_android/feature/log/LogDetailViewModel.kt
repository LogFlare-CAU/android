package com.example.logflare_android.feature.log

import androidx.lifecycle.ViewModel
import com.example.logflare_android.data.LogsRepository
import com.example.logflare_android.ui.common.LogCardInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LogDetailViewModel @Inject constructor(
    private val repo: LogsRepository
) : ViewModel() {
    fun getLogDetail(): LogCardInfo? {
        return repo.getSelectedLog()
    }
}