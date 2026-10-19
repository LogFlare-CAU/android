package com.logflare.android.visual

import androidx.compose.runtime.Composable
import com.logflare.android.feature.auth.LoginContent
import com.logflare.android.feature.auth.LoginError
import com.logflare.android.feature.auth.LoginMode
import com.logflare.android.feature.auth.LoginStep
import com.logflare.android.feature.auth.LoginUiState
import com.logflare.android.visual.ds.DsSnapshotTest
import org.junit.Test
import org.robolectric.annotation.Config

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
        // The camera is a flat placeholder: Robolectric has no preview, and the frame is what is compared.
        camera = {},
    )

    private val empty = LoginUiState(recentServers = recent)
    private val errors = LoginUiState(
        url = "logs.example.com",
        urlError = LoginError.UrlScheme,
        usernameError = LoginError.EnterUsername,
        passwordError = LoginError.EnterPassword,
        recentServers = recent,
    )
    private val formError = LoginUiState(
        url = "https://logs.example.com/",
        username = "admin",
        password = "wrong",
        formError = LoginError.InvalidCredentials,
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

    private val server = "https://logflare.code0987.me"
    private val bothModes = listOf(LoginMode.Password, LoginMode.Access)
    private val passwordMode = LoginUiState(url = server, username = "admin", modes = bothModes, recentServers = recent)
    private val accessMode = passwordMode.copy(selectedMode = LoginMode.Access, username = "")
    private val noModes = LoginUiState(url = server, modes = emptyList(), recentServers = recent)
    private val scan = accessMode.copy(step = LoginStep.Scan)
    private val scanWrong = scan.copy(scanError = LoginError.QrInvalid)
    private val confirm = accessMode.copy(
        step = LoginStep.Confirm,
        accessServer = "logflare.code0987.me",
        accessClientMasked = "ad3f…6e70",
    )
    private val confirmRejected = confirm.copy(formError = LoginError.AccessRejected)
    private val confirmUnreachable = confirm.copy(formError = LoginError.Unreachable, keepLoggedIn = false)
    private val manual = accessMode.copy(step = LoginStep.Manual)
    private val manualErrors = manual.copy(
        manualIdError = LoginError.EnterClientId,
        manualSecretError = LoginError.EnterClientSecret,
    )

    @Test fun login_password_mode_light() = capture("login_password_mode", false) { Login(passwordMode) }
    @Test fun login_password_mode_dark() = capture("login_password_mode", true) { Login(passwordMode) }
    @Test fun login_access_mode_light() = capture("login_access_mode", false) { Login(accessMode) }
    @Test fun login_access_mode_dark() = capture("login_access_mode", true) { Login(accessMode) }
    @Config(qualifiers = "+ko") @Test fun login_access_mode_ko_light() = capture("login_access_mode_ko", false) { Login(accessMode) }
    @Test fun login_no_modes_light() = capture("login_no_modes", false) { Login(noModes) }
    @Test fun login_no_modes_dark() = capture("login_no_modes", true) { Login(noModes) }
    @Test fun login_scan_light() = capture("login_scan", false) { Login(scan) }
    @Test fun login_scan_dark() = capture("login_scan", true) { Login(scan) }
    @Test fun login_scan_wrong_qr_light() = capture("login_scan_wrong_qr", false) { Login(scanWrong) }
    @Config(qualifiers = "+ko") @Test fun login_scan_wrong_qr_ko_light() = capture("login_scan_wrong_qr_ko", false) { Login(scanWrong) }
    @Test fun login_confirm_light() = capture("login_confirm", false) { Login(confirm) }
    @Test fun login_confirm_dark() = capture("login_confirm", true) { Login(confirm) }
    @Test fun login_confirm_rejected_light() = capture("login_confirm_rejected", false) { Login(confirmRejected) }
    @Test fun login_confirm_rejected_dark() = capture("login_confirm_rejected", true) { Login(confirmRejected) }
    @Config(qualifiers = "+ko") @Test fun login_confirm_rejected_ko_light() = capture("login_confirm_rejected_ko", false) { Login(confirmRejected) }
    @Test fun login_confirm_unreachable_light() = capture("login_confirm_unreachable", false) { Login(confirmUnreachable) }
    @Test fun login_manual_light() = capture("login_manual", false) { Login(manual) }
    @Test fun login_manual_dark() = capture("login_manual", true) { Login(manual) }
    @Test fun login_manual_errors_light() = capture("login_manual_errors", false) { Login(manualErrors) }

    @Test fun login_empty_light() = capture("login_empty", false) { Login(empty) }
    @Test fun login_empty_dark() = capture("login_empty", true) { Login(empty) }
    // Korean (app language 한국어): long labels must not truncate or overflow.
    @Config(qualifiers = "+ko") @Test fun login_empty_ko_light() = capture("login_empty_ko", false) { Login(empty) }
    @Test fun login_errors_light() = capture("login_errors", false) { Login(errors) }
    @Test fun login_errors_dark() = capture("login_errors", true) { Login(errors) }
    @Test fun login_form_error_light() = capture("login_form_error", false) { Login(formError) }
    @Test fun login_form_error_dark() = capture("login_form_error", true) { Login(formError) }
    @Test fun login_submitting_light() = capture("login_submitting", false) { Login(submitting) }
    @Test fun login_submitting_dark() = capture("login_submitting", true) { Login(submitting) }
    @Test fun login_recent_sheet_light() = capture("login_recent_sheet", false) { Login(sheet) }
    @Test fun login_recent_sheet_dark() = capture("login_recent_sheet", true) { Login(sheet) }
}
