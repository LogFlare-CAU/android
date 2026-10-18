package com.logflare.android.feature.mypage

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.logflare.core.designsystem.v2.components.ConfirmSheet
import com.example.logflare.core.designsystem.v2.components.SelectionSheet
import com.example.logflare.core.designsystem.v2.components.SheetOption
import com.logflare.android.R
import com.logflare.android.data.ThemePreference
import com.logflare.android.push.GlobalAlert

/** The open sheet of the My tab, if any. */
@Composable
internal fun MySheets(state: MyUiState, actions: MyActions) {
    when (state.sheet) {
        MySheet.Appearance -> SelectionSheet(
            title = stringResource(R.string.my_appearance),
            options = ThemePreference.entries.map { SheetOption(it, themeLabel(it)) },
            selected = state.theme,
            onSelect = actions.onSelectTheme,
            onDismiss = actions.onDismissSheet,
        )
        MySheet.PushAlerts -> SelectionSheet(
            title = stringResource(R.string.my_push_alerts),
            description = stringResource(R.string.my_push_alerts_body),
            options = GlobalAlert.entries.map { SheetOption(it, globalAlertLabel(it), globalAlertBody(it)) },
            selected = state.globalAlert,
            onSelect = actions.onSelectGlobalAlert,
            onDismiss = actions.onDismissSheet,
        )
        MySheet.Logout -> ConfirmSheet(
            title = stringResource(R.string.my_logout_title),
            body = stringResource(R.string.my_logout_body),
            confirmLabel = stringResource(R.string.my_logout),
            destructive = false,
            onConfirm = actions.onConfirmLogout,
            onDismiss = actions.onDismissSheet,
            confirmEnabled = !state.signingOut,
            dismissLabel = stringResource(R.string.my_logout_cancel),
        )
        null -> Unit
    }
}

@Composable
internal fun themeLabel(theme: ThemePreference): String = stringResource(
    when (theme) {
        ThemePreference.SYSTEM -> R.string.my_theme_system
        ThemePreference.LIGHT -> R.string.my_theme_light
        ThemePreference.DARK -> R.string.my_theme_dark
    },
)

@Composable
internal fun globalAlertLabel(value: GlobalAlert): String = stringResource(
    when (value) {
        GlobalAlert.Off -> R.string.my_push_off
        GlobalAlert.CriticalOnly -> R.string.my_push_critical
        GlobalAlert.ErrorAndAbove -> R.string.my_push_error
        GlobalAlert.WarningAndAbove -> R.string.my_push_warning
        GlobalAlert.Everything -> R.string.my_push_everything
    },
)

@Composable
private fun globalAlertBody(value: GlobalAlert): String = stringResource(
    when (value) {
        GlobalAlert.Off -> R.string.my_push_off_body
        GlobalAlert.CriticalOnly -> R.string.my_push_critical_body
        GlobalAlert.ErrorAndAbove -> R.string.my_push_error_body
        GlobalAlert.WarningAndAbove -> R.string.my_push_warning_body
        GlobalAlert.Everything -> R.string.my_push_everything_body
    },
)
