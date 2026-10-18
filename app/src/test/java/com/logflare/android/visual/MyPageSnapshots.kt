package com.logflare.android.visual

import com.logflare.android.feature.mypage.AddMemberScreenContent
import com.logflare.android.feature.mypage.EditMemberScreenContent
import org.junit.Test

/** The old Add/Edit member screens, until their rework replaces them. */
class MyPageSnapshots : VisualSnapshotTest() {
    @Test fun add_member_empty_light() {
        capture("add_member_empty", darkTheme = false) {
            AddMemberScreenContent(
                uiState = SnapshotFixtures.addMember(),
                onBack = {},
                onUsernameChange = { _ -> },
                onPasswordChange = { _ -> },
                onValidateUsername = {},
                onValidatePassword = {},
                onPermissionChange = { _ -> },
                onSubmit = {},
                onDismissMessage = {},
            )
        }
    }

    @Test fun add_member_empty_dark() {
        capture("add_member_empty", darkTheme = true) {
            AddMemberScreenContent(
                uiState = SnapshotFixtures.addMember(),
                onBack = {},
                onUsernameChange = { _ -> },
                onPasswordChange = { _ -> },
                onValidateUsername = {},
                onValidatePassword = {},
                onPermissionChange = { _ -> },
                onSubmit = {},
                onDismissMessage = {},
            )
        }
    }

    @Test fun add_member_validation_error_light() {
        capture("add_member_validation_error", darkTheme = false) {
            AddMemberScreenContent(
                uiState = SnapshotFixtures.addMember(validationError = true),
                onBack = {},
                onUsernameChange = { _ -> },
                onPasswordChange = { _ -> },
                onValidateUsername = {},
                onValidatePassword = {},
                onPermissionChange = { _ -> },
                onSubmit = {},
                onDismissMessage = {},
            )
        }
    }

    @Test fun add_member_validation_error_dark() {
        capture("add_member_validation_error", darkTheme = true) {
            AddMemberScreenContent(
                uiState = SnapshotFixtures.addMember(validationError = true),
                onBack = {},
                onUsernameChange = { _ -> },
                onPasswordChange = { _ -> },
                onValidateUsername = {},
                onValidatePassword = {},
                onPermissionChange = { _ -> },
                onSubmit = {},
                onDismissMessage = {},
            )
        }
    }

    @Test fun add_member_valid_light() {
        capture("add_member_valid", darkTheme = false) {
            AddMemberScreenContent(
                uiState = SnapshotFixtures.addMember(valid = true),
                onBack = {},
                onUsernameChange = { _ -> },
                onPasswordChange = { _ -> },
                onValidateUsername = {},
                onValidatePassword = {},
                onPermissionChange = { _ -> },
                onSubmit = {},
                onDismissMessage = {},
            )
        }
    }

    @Test fun add_member_valid_dark() {
        capture("add_member_valid", darkTheme = true) {
            AddMemberScreenContent(
                uiState = SnapshotFixtures.addMember(valid = true),
                onBack = {},
                onUsernameChange = { _ -> },
                onPasswordChange = { _ -> },
                onValidateUsername = {},
                onValidatePassword = {},
                onPermissionChange = { _ -> },
                onSubmit = {},
                onDismissMessage = {},
            )
        }
    }

    @Test fun add_member_loading_light() {
        capture("add_member_loading", darkTheme = false) {
            AddMemberScreenContent(
                uiState = SnapshotFixtures.addMember(valid = true, loading = true),
                onBack = {},
                onUsernameChange = { _ -> },
                onPasswordChange = { _ -> },
                onValidateUsername = {},
                onValidatePassword = {},
                onPermissionChange = { _ -> },
                onSubmit = {},
                onDismissMessage = {},
            )
        }
    }

    @Test fun add_member_loading_dark() {
        capture("add_member_loading", darkTheme = true) {
            AddMemberScreenContent(
                uiState = SnapshotFixtures.addMember(valid = true, loading = true),
                onBack = {},
                onUsernameChange = { _ -> },
                onPasswordChange = { _ -> },
                onValidateUsername = {},
                onValidatePassword = {},
                onPermissionChange = { _ -> },
                onSubmit = {},
                onDismissMessage = {},
            )
        }
    }

    @Test fun add_member_error_light() {
        capture("add_member_error", darkTheme = false) {
            AddMemberScreenContent(
                uiState = SnapshotFixtures.addMember(valid = true, error = "Unable to add member"),
                onBack = {},
                onUsernameChange = { _ -> },
                onPasswordChange = { _ -> },
                onValidateUsername = {},
                onValidatePassword = {},
                onPermissionChange = { _ -> },
                onSubmit = {},
                onDismissMessage = {},
            )
        }
    }

    @Test fun add_member_error_dark() {
        capture("add_member_error", darkTheme = true) {
            AddMemberScreenContent(
                uiState = SnapshotFixtures.addMember(valid = true, error = "Unable to add member"),
                onBack = {},
                onUsernameChange = { _ -> },
                onPasswordChange = { _ -> },
                onValidateUsername = {},
                onValidatePassword = {},
                onPermissionChange = { _ -> },
                onSubmit = {},
                onDismissMessage = {},
            )
        }
    }

    @Test fun edit_member_normal_light() {
        capture("edit_member_normal", darkTheme = false) {
            EditMemberScreenContent(
                uiState = SnapshotFixtures.editMember(),
                onBack = {},
                onUsernameChange = { _ -> },
                onPasswordChange = { _ -> },
                onValidateUsername = {},
                onValidatePassword = {},
                onPermissionChange = { _ -> },
                onSave = {},
                onDeleteRequest = {},
                onDeleteConfirm = {},
                onDeleteDismiss = {},
                onDismissMessage = {},
            )
        }
    }

    @Test fun edit_member_normal_dark() {
        capture("edit_member_normal", darkTheme = true) {
            EditMemberScreenContent(
                uiState = SnapshotFixtures.editMember(),
                onBack = {},
                onUsernameChange = { _ -> },
                onPasswordChange = { _ -> },
                onValidateUsername = {},
                onValidatePassword = {},
                onPermissionChange = { _ -> },
                onSave = {},
                onDeleteRequest = {},
                onDeleteConfirm = {},
                onDeleteDismiss = {},
                onDismissMessage = {},
            )
        }
    }

    @Test fun edit_member_validation_error_light() {
        capture("edit_member_validation_error", darkTheme = false) {
            EditMemberScreenContent(
                uiState = SnapshotFixtures.editMember(validationError = true),
                onBack = {},
                onUsernameChange = { _ -> },
                onPasswordChange = { _ -> },
                onValidateUsername = {},
                onValidatePassword = {},
                onPermissionChange = { _ -> },
                onSave = {},
                onDeleteRequest = {},
                onDeleteConfirm = {},
                onDeleteDismiss = {},
                onDismissMessage = {},
            )
        }
    }

    @Test fun edit_member_validation_error_dark() {
        capture("edit_member_validation_error", darkTheme = true) {
            EditMemberScreenContent(
                uiState = SnapshotFixtures.editMember(validationError = true),
                onBack = {},
                onUsernameChange = { _ -> },
                onPasswordChange = { _ -> },
                onValidateUsername = {},
                onValidatePassword = {},
                onPermissionChange = { _ -> },
                onSave = {},
                onDeleteRequest = {},
                onDeleteConfirm = {},
                onDeleteDismiss = {},
                onDismissMessage = {},
            )
        }
    }

    @Test fun edit_member_disabled_light() {
        capture("edit_member_disabled", darkTheme = false) {
            EditMemberScreenContent(
                uiState = SnapshotFixtures.editMember(disabled = true),
                onBack = {},
                onUsernameChange = { _ -> },
                onPasswordChange = { _ -> },
                onValidateUsername = {},
                onValidatePassword = {},
                onPermissionChange = { _ -> },
                onSave = {},
                onDeleteRequest = {},
                onDeleteConfirm = {},
                onDeleteDismiss = {},
                onDismissMessage = {},
            )
        }
    }

    @Test fun edit_member_disabled_dark() {
        capture("edit_member_disabled", darkTheme = true) {
            EditMemberScreenContent(
                uiState = SnapshotFixtures.editMember(disabled = true),
                onBack = {},
                onUsernameChange = { _ -> },
                onPasswordChange = { _ -> },
                onValidateUsername = {},
                onValidatePassword = {},
                onPermissionChange = { _ -> },
                onSave = {},
                onDeleteRequest = {},
                onDeleteConfirm = {},
                onDeleteDismiss = {},
                onDismissMessage = {},
            )
        }
    }

    @Test fun edit_member_loading_light() {
        capture("edit_member_loading", darkTheme = false) {
            EditMemberScreenContent(
                uiState = SnapshotFixtures.editMember(loading = true),
                onBack = {},
                onUsernameChange = { _ -> },
                onPasswordChange = { _ -> },
                onValidateUsername = {},
                onValidatePassword = {},
                onPermissionChange = { _ -> },
                onSave = {},
                onDeleteRequest = {},
                onDeleteConfirm = {},
                onDeleteDismiss = {},
                onDismissMessage = {},
            )
        }
    }

    @Test fun edit_member_loading_dark() {
        capture("edit_member_loading", darkTheme = true) {
            EditMemberScreenContent(
                uiState = SnapshotFixtures.editMember(loading = true),
                onBack = {},
                onUsernameChange = { _ -> },
                onPasswordChange = { _ -> },
                onValidateUsername = {},
                onValidatePassword = {},
                onPermissionChange = { _ -> },
                onSave = {},
                onDeleteRequest = {},
                onDeleteConfirm = {},
                onDeleteDismiss = {},
                onDismissMessage = {},
            )
        }
    }

    @Test fun edit_member_delete_dialog_light() {
        capture("edit_member_delete_dialog", darkTheme = false) {
            EditMemberScreenContent(
                uiState = SnapshotFixtures.editMember(showDelete = true),
                onBack = {},
                onUsernameChange = { _ -> },
                onPasswordChange = { _ -> },
                onValidateUsername = {},
                onValidatePassword = {},
                onPermissionChange = { _ -> },
                onSave = {},
                onDeleteRequest = {},
                onDeleteConfirm = {},
                onDeleteDismiss = {},
                onDismissMessage = {},
            )
        }
    }

    @Test fun edit_member_delete_dialog_dark() {
        capture("edit_member_delete_dialog", darkTheme = true) {
            EditMemberScreenContent(
                uiState = SnapshotFixtures.editMember(showDelete = true),
                onBack = {},
                onUsernameChange = { _ -> },
                onPasswordChange = { _ -> },
                onValidateUsername = {},
                onValidatePassword = {},
                onPermissionChange = { _ -> },
                onSave = {},
                onDeleteRequest = {},
                onDeleteConfirm = {},
                onDeleteDismiss = {},
                onDismissMessage = {},
            )
        }
    }
}
