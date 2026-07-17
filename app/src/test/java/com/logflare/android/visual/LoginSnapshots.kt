package com.logflare.android.visual

import com.logflare.android.feature.auth.LoginScreenContent
import org.junit.Test

class LoginSnapshots : VisualSnapshotTest() {
    @Test fun login_empty_light() {
        capture("login_empty", darkTheme = false) {
            LoginScreenContent(
                uiState = SnapshotFixtures.auth(),
                form = SnapshotFixtures.loginForm(),
                onFormChange = { _ -> },
                onSignIn = {},
            )
        }
    }

    @Test fun login_empty_dark() {
        capture("login_empty", darkTheme = true) {
            LoginScreenContent(
                uiState = SnapshotFixtures.auth(),
                form = SnapshotFixtures.loginForm(),
                onFormChange = { _ -> },
                onSignIn = {},
            )
        }
    }

    @Test fun login_validation_error_light() {
        capture("login_validation_error", darkTheme = false) {
            LoginScreenContent(
                uiState = SnapshotFixtures.auth(),
                form = SnapshotFixtures.loginForm(validationError = true),
                onFormChange = { _ -> },
                onSignIn = {},
            )
        }
    }

    @Test fun login_validation_error_dark() {
        capture("login_validation_error", darkTheme = true) {
            LoginScreenContent(
                uiState = SnapshotFixtures.auth(),
                form = SnapshotFixtures.loginForm(validationError = true),
                onFormChange = { _ -> },
                onSignIn = {},
            )
        }
    }

    @Test fun login_loading_light() {
        capture("login_loading", darkTheme = false) {
            LoginScreenContent(
                uiState = SnapshotFixtures.auth(loading = true),
                form = SnapshotFixtures.loginForm(),
                onFormChange = { _ -> },
                onSignIn = {},
            )
        }
    }

    @Test fun login_loading_dark() {
        capture("login_loading", darkTheme = true) {
            LoginScreenContent(
                uiState = SnapshotFixtures.auth(loading = true),
                form = SnapshotFixtures.loginForm(),
                onFormChange = { _ -> },
                onSignIn = {},
            )
        }
    }
}
