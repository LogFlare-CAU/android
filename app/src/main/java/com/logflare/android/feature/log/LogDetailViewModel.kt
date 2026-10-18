package com.logflare.android.feature.log

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.logflare.android.data.store.LogStore
import com.logflare.android.data.store.model.LogEventDetail
import com.logflare.android.ui.common.LogCardInfo
import com.logflare.android.ui.nav.Destinations
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Bridge for the old detail screen until Task 5: loads the event named by the `logs/{errorId}`
 * route and shapes it into the old card. The project shows as its id (no name lookup).
 */
@HiltViewModel
class LogDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    logStore: LogStore,
) : ViewModel() {
    private val errorId: Int = checkNotNull(savedStateHandle[Destinations.ErrorIdArg])

    val log: StateFlow<LogCardInfo?> = logStore.event(errorId)
        .map { it.data?.toCardInfo() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

private fun LogEventDetail.toCardInfo() = LogCardInfo(
    level = rowLevel.name,
    timestamp = occurredAt.toString(),
    message = event.message,
    prefix = "Project #${event.projectId}",
    suffix = event.type ?: event.logFileName ?: "Unknown",
)
