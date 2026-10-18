package com.logflare.android.feature.log

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.logflare.android.data.store.ApiException
import com.logflare.android.data.store.LogStore
import com.logflare.android.data.store.ProjectStore
import com.logflare.android.data.store.model.LogEventDetail
import com.logflare.android.ui.nav.Destinations
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import retrofit2.HttpException

enum class LogDetailError {
    /** The server has no such log (deleted, or an old notification). */
    NotFound,

    /** Nothing cached and the load failed. */
    LoadFailed,
}

data class LogDetailUiState(
    val detail: LogEventDetail? = null,
    val projectName: String? = null,
    val rawExpanded: Boolean = false,
    val initialLoad: Boolean = true,
    val error: LogDetailError? = null,
    val shareSheetOpen: Boolean = false,
)

/** One log row (route `logs/{errorId}`) with its group, and the name of its project. */
@HiltViewModel
class LogDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val logStore: LogStore,
    projectStore: ProjectStore,
) : ViewModel() {
    private val errorId: Int = checkNotNull(savedStateHandle[Destinations.ErrorIdArg])

    private val rawExpanded = MutableStateFlow(false)
    private val shareSheetOpen = MutableStateFlow(false)

    val state: StateFlow<LogDetailUiState> = combine(
        logStore.event(errorId),
        projectStore.projects(),
        rawExpanded,
        shareSheetOpen,
    ) { event, projects, expanded, share ->
        val detail = event.data
        LogDetailUiState(
            detail = detail,
            projectName = detail?.let { d -> projects.data?.firstOrNull { it.id == d.event.projectId }?.name },
            rawExpanded = expanded,
            initialLoad = event.isInitialLoad,
            error = if (detail == null) event.error?.toDetailError() else null,
            shareSheetOpen = share,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LogDetailUiState())

    fun toggleRaw() {
        rawExpanded.value = !rawExpanded.value
    }

    fun openShare() {
        shareSheetOpen.value = true
    }

    fun closeShare() {
        shareSheetOpen.value = false
    }

    fun retry() {
        viewModelScope.launch { logStore.refreshEvent(errorId) }
    }
}

private fun Throwable.toDetailError(): LogDetailError {
    val notFound = (this is ApiException && errorCode == 404) || (this is HttpException && code() == 404)
    return if (notFound) LogDetailError.NotFound else LogDetailError.LoadFailed
}
