package com.logflare.android.visual

import androidx.compose.runtime.Composable
import com.example.logflare.core.designsystem.v2.LfLevel
import com.logflare.android.feature.project.CreateSheet
import com.logflare.android.feature.project.CreatedProject
import com.logflare.android.feature.project.NameCheck
import com.logflare.android.feature.project.ProjectCreateContent
import com.logflare.android.feature.project.ProjectCreateUiState
import com.logflare.android.visual.ds.DsSnapshotTest
import org.junit.Test

class ProjectCreateSnapshots : DsSnapshotTest() {

    @Composable
    private fun Create(state: ProjectCreateUiState) = ProjectCreateContent(
        state = state,
        onClose = {},
        onName = {},
        onLevel = {},
        onAddKeyword = {},
        onRemoveKeyword = {},
        onKeywordDuplicate = {},
        onCreate = {},
        onToggleReveal = {},
        onCopy = {},
        onOpenRotate = {},
        onConfirmRotate = {},
        onConfirmDiscard = {},
        onDismissSheet = {},
        onGoToProject = {},
    )

    @Test fun project_create_empty_light() = capture("project_create_empty", false) { Create(ProjectCreateFixtures.empty) }
    @Test fun project_create_empty_dark() = capture("project_create_empty", true) { Create(ProjectCreateFixtures.empty) }
    @Test fun project_create_invalid_light() = capture("project_create_invalid", false) { Create(ProjectCreateFixtures.invalid) }
    @Test fun project_create_invalid_dark() = capture("project_create_invalid", true) { Create(ProjectCreateFixtures.invalid) }
    @Test fun project_create_filled_light() = capture("project_create_filled", false) { Create(ProjectCreateFixtures.filled) }
    @Test fun project_create_filled_dark() = capture("project_create_filled", true) { Create(ProjectCreateFixtures.filled) }
    @Test fun project_create_created_light() = capture("project_create_created", false) { Create(ProjectCreateFixtures.created) }
    @Test fun project_create_created_dark() = capture("project_create_created", true) { Create(ProjectCreateFixtures.created) }
    @Test fun project_create_discard_sheet_light() = capture("project_create_discard_sheet", false) { Create(ProjectCreateFixtures.discard) }
    @Test fun project_create_discard_sheet_dark() = capture("project_create_discard_sheet", true) { Create(ProjectCreateFixtures.discard) }
}

object ProjectCreateFixtures {
    val empty = ProjectCreateUiState()

    val invalid = ProjectCreateUiState(
        name = "api/server",
        nameCheck = NameCheck.Invalid("Don't use < > / \\ in names."),
        dirty = true,
    )

    val filled = ProjectCreateUiState(
        name = "payment-api",
        nameCheck = NameCheck.Valid,
        level = LfLevel.Error,
        keywords = listOf("healthcheck", "timeout"),
        dirty = true,
    )

    val created = ProjectCreateUiState(
        name = "payment-api",
        nameCheck = NameCheck.Valid,
        created = CreatedProject(12, "payment-api", "lf_8f2c3b9e71d04a5c9e2fa91e"),
        serverUrl = "https://logs.example.com",
    )

    val discard = filled.copy(sheet = CreateSheet.Discard)
}
