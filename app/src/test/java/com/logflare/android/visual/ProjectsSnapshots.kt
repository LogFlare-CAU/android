package com.logflare.android.visual

import androidx.compose.runtime.Composable
import com.logflare.android.data.store.model.ProjectItem
import com.logflare.android.feature.project.ProjectCreateScreenContent
import com.logflare.android.feature.project.ProjectSort
import com.logflare.android.feature.project.ProjectsContent
import com.logflare.android.feature.project.ProjectsUiState
import com.logflare.android.visual.ds.DsSnapshotTest
import java.time.Instant
import org.junit.Test

class ProjectsSnapshots : DsSnapshotTest() {

    @Composable
    private fun Projects(state: ProjectsUiState) = ProjectsContent(
        state = state,
        now = ProjectsFixtures.NOW,
        onToggleSearch = {},
        onSearch = {},
        onOpenSort = {},
        onCloseSort = {},
        onSelectSort = {},
        onRefresh = {},
        onOpenProject = {},
        onCreateProject = {},
    )

    @Test fun projects_populated_light() = capture("projects_populated", false) { Projects(ProjectsFixtures.populated) }
    @Test fun projects_populated_dark() = capture("projects_populated", true) { Projects(ProjectsFixtures.populated) }
    @Test fun projects_member_light() = capture("projects_member", false) { Projects(ProjectsFixtures.member) }
    @Test fun projects_member_dark() = capture("projects_member", true) { Projects(ProjectsFixtures.member) }
    @Test fun projects_empty_light() = capture("projects_empty", false) { Projects(ProjectsFixtures.empty) }
    @Test fun projects_empty_dark() = capture("projects_empty", true) { Projects(ProjectsFixtures.empty) }
    @Test fun projects_empty_member_light() = capture("projects_empty_member", false) { Projects(ProjectsFixtures.emptyMember) }
    @Test fun projects_empty_member_dark() = capture("projects_empty_member", true) { Projects(ProjectsFixtures.emptyMember) }
    @Test fun projects_search_no_match_light() = capture("projects_search_no_match", false) { Projects(ProjectsFixtures.noMatch) }
    @Test fun projects_search_no_match_dark() = capture("projects_search_no_match", true) { Projects(ProjectsFixtures.noMatch) }
    @Test fun projects_long_text_light() = capture("projects_long_text", false) { Projects(ProjectsFixtures.longText) }
    @Test fun projects_long_text_dark() = capture("projects_long_text", true) { Projects(ProjectsFixtures.longText) }
    @Test fun projects_sort_sheet_light() = capture("projects_sort_sheet", false) { Projects(ProjectsFixtures.sortSheet) }
    @Test fun projects_sort_sheet_dark() = capture("projects_sort_sheet", true) { Projects(ProjectsFixtures.sortSheet) }
    @Test fun projects_initial_load_light() = capture("projects_initial_load", false) { Projects(ProjectsUiState()) }
    @Test fun projects_initial_load_dark() = capture("projects_initial_load", true) { Projects(ProjectsUiState()) }
    @Test fun projects_load_failed_light() = capture("projects_load_failed", false) { Projects(ProjectsFixtures.loadFailed) }
    @Test fun projects_load_failed_dark() = capture("projects_load_failed", true) { Projects(ProjectsFixtures.loadFailed) }
}

object ProjectsFixtures {
    val NOW: Instant = Instant.parse("2026-10-18T12:00:00Z")

    private fun project(id: Int, name: String, errors: Int = 0, minutesAgo: Long? = null) = ProjectItem(
        id = id,
        name = name,
        logFiles = emptyList(),
        errorCount24h = errors,
        warningCount24h = 0,
        lastErrorAt = minutesAgo?.let { NOW.minusSeconds(it * 60) },
    )

    private val projects = listOf(
        project(1, "api-server", errors = 8, minutesAgo = 3),
        project(2, "worker", errors = 4, minutesAgo = 18),
        project(3, "auth-service", errors = 1, minutesAgo = 140),
        project(4, "web-frontend"),
        project(5, "batch-jobs"),
    )

    private fun state(items: List<ProjectItem> = projects, total: Int = items.size) = ProjectsUiState(
        items = items,
        total = total,
        canCreate = true,
        initialLoad = false,
    )

    val populated = state()

    val member = state().copy(canCreate = false)

    val empty = state(emptyList())

    val emptyMember = empty.copy(canCreate = false)

    val noMatch = state(emptyList(), total = projects.size).copy(search = "paymnts", searchOpen = true)

    private const val LONG_NAME = "payments-gateway-eu-west-1-production-canary-blue-green-shadow-traffic-mirror-replica"
    private const val LONG_UNBROKEN = "projectnamewithoutanyspacesorseparatorsatall0123456789abcdefghijklmnopqrstuvwxyz"

    val longText = state(
        listOf(
            project(11, LONG_NAME, errors = 1234, minutesAgo = 5),
            project(12, LONG_UNBROKEN, errors = 12, minutesAgo = 61),
        ) + projects.take(2),
    )

    val sortSheet = populated.copy(sortSheetOpen = true, sort = ProjectSort.RecentErrors)

    val loadFailed = ProjectsUiState(initialLoad = false, loadFailed = true)
}

class ProjectCreateSnapshots : VisualSnapshotTest() {
    @Test fun project_create_initial_light() {
        capture("project_create_initial", darkTheme = false) {
            ProjectCreateScreenContent(
                uiState = SnapshotFixtures.projectEditor(),
                onAction = { _ -> },
            )
        }
    }

    @Test fun project_create_initial_dark() {
        capture("project_create_initial", darkTheme = true) {
            ProjectCreateScreenContent(
                uiState = SnapshotFixtures.projectEditor(),
                onAction = { _ -> },
            )
        }
    }

    @Test fun project_create_invalid_light() {
        capture("project_create_invalid", darkTheme = false) {
            ProjectCreateScreenContent(
                uiState = SnapshotFixtures.projectEditor(invalid = true),
                onAction = { _ -> },
            )
        }
    }

    @Test fun project_create_invalid_dark() {
        capture("project_create_invalid", darkTheme = true) {
            ProjectCreateScreenContent(
                uiState = SnapshotFixtures.projectEditor(invalid = true),
                onAction = { _ -> },
            )
        }
    }

    @Test fun project_create_saved_light() {
        capture("project_create_saved", darkTheme = false) {
            ProjectCreateScreenContent(
                uiState = SnapshotFixtures.projectEditor(saved = true),
                onAction = { _ -> },
            )
        }
    }

    @Test fun project_create_saved_dark() {
        capture("project_create_saved", darkTheme = true) {
            ProjectCreateScreenContent(
                uiState = SnapshotFixtures.projectEditor(saved = true),
                onAction = { _ -> },
            )
        }
    }

    @Test fun project_create_loading_light() {
        capture("project_create_loading", darkTheme = false) {
            ProjectCreateScreenContent(
                uiState = SnapshotFixtures.projectEditor(loading = true),
                onAction = { _ -> },
            )
        }
    }

    @Test fun project_create_loading_dark() {
        capture("project_create_loading", darkTheme = true) {
            ProjectCreateScreenContent(
                uiState = SnapshotFixtures.projectEditor(loading = true),
                onAction = { _ -> },
            )
        }
    }
}
