package com.logflare.android.visual

import androidx.compose.runtime.Composable
import com.logflare.android.feature.auth.LoginContent
import com.logflare.android.feature.auth.LoginError
import com.logflare.android.feature.auth.LoginUiState
import com.logflare.android.visual.ds.DsSnapshotTest
import org.junit.Test

class LoginSnapshots : DsSnapshotTest() {
    private val recent = listOf("https://logs.example.com/", "https://staging-logs.example.com/", "http://10.0.2.2:8000/")

    @Composable
    private fun Login(state: LoginUiState) = LoginContent(
        state = state,
        onUrl = {},
        onUsername = {},
        onPassword = {},
        onOpenRecent = {},
        onPickRecent = {},
        onDismissRecent = {},
        onSubmit = {},
    )

    private val empty = LoginUiState(recentServers = recent)
    private val errors = LoginUiState(
        url = "logs.example.com",
        urlError = LoginError.UrlScheme,
        usernameError = LoginError.EnterUsername,
        passwordError = LoginError.EnterPassword,
        recentServers = recent,
    )
    private val submitting = LoginUiState(
        url = "https://logs.example.com/",
        username = "admin",
        password = "secret",
        submitting = true,
        recentServers = recent,
    )
    private val sheet = empty.copy(url = "https://logs.example.com/", recentSheetOpen = true)

    @Test fun login_empty_light() = capture("login_empty", false) { Login(empty) }
    @Test fun login_empty_dark() = capture("login_empty", true) { Login(empty) }
    @Test fun login_errors_light() = capture("login_errors", false) { Login(errors) }
    @Test fun login_errors_dark() = capture("login_errors", true) { Login(errors) }
    @Test fun login_submitting_light() = capture("login_submitting", false) { Login(submitting) }
    @Test fun login_submitting_dark() = capture("login_submitting", true) { Login(submitting) }
    @Test fun login_recent_sheet_light() = capture("login_recent_sheet", false) { Login(sheet) }
    @Test fun login_recent_sheet_dark() = capture("login_recent_sheet", true) { Login(sheet) }
}
