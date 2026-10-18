package com.logflare.android.feature.log

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.logflare.core.designsystem.v2.LfLevel
import com.example.logflare.core.designsystem.v2.components.SelectionSheet
import com.example.logflare.core.designsystem.v2.components.SheetOption
import com.logflare.android.R
import com.logflare.android.data.store.model.LogQuery
import com.logflare.android.data.store.model.LogSortOrder
import com.logflare.android.data.store.model.ProjectItem

/** The picker behind one filter chip; each closes itself after a choice. */
@Composable
internal fun LogFilterSheet(
    sheet: LogsSheet,
    query: LogQuery,
    projects: List<ProjectItem>,
    onSelectProject: (Int?) -> Unit,
    onSelectLevel: (LfLevel?) -> Unit,
    onSelectSort: (LogSortOrder) -> Unit,
    onDismiss: () -> Unit,
) {
    when (sheet) {
        LogsSheet.Project -> SelectionSheet(
            title = stringResource(R.string.logs_sheet_project),
            options = listOf(SheetOption<Int?>(null, stringResource(R.string.logs_all_projects))) +
                projects.map { SheetOption<Int?>(it.id, it.name) },
            selected = query.projectId,
            onSelect = onSelectProject,
            onDismiss = onDismiss,
        )
        LogsSheet.Level -> SelectionSheet(
            title = stringResource(R.string.logs_sheet_level),
            options = listOf(
                SheetOption<LfLevel?>(null, stringResource(R.string.logs_all_levels)),
                SheetOption<LfLevel?>(LfLevel.Warning, stringResource(R.string.logs_level_warning_up)),
                SheetOption<LfLevel?>(LfLevel.Error, stringResource(R.string.logs_level_error_up)),
                SheetOption<LfLevel?>(LfLevel.Critical, stringResource(R.string.logs_level_critical_only)),
            ),
            selected = query.minLevel,
            onSelect = onSelectLevel,
            onDismiss = onDismiss,
        )
        LogsSheet.Sort -> SelectionSheet(
            title = stringResource(R.string.logs_sheet_sort),
            options = LogSortOrder.entries.map { SheetOption(it, sortLabel(it)) },
            selected = query.sort,
            onSelect = onSelectSort,
            onDismiss = onDismiss,
        )
    }
}

@Composable
internal fun sortLabel(order: LogSortOrder): String = stringResource(
    when (order) {
        LogSortOrder.Newest -> R.string.logs_sort_newest
        LogSortOrder.Oldest -> R.string.logs_sort_oldest
        LogSortOrder.Highest -> R.string.logs_sort_highest
        LogSortOrder.Lowest -> R.string.logs_sort_lowest
    },
)
