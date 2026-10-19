package com.logflare.android.feature.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.logflare.core.designsystem.v2.Lf
import com.example.logflare.core.designsystem.v2.components.AppTextField
import com.example.logflare.core.designsystem.v2.components.PasswordField
import com.example.logflare.core.designsystem.v2.components.PrimaryButton
import com.example.logflare.core.designsystem.v2.components.SelectionSheet
import com.example.logflare.core.designsystem.v2.components.SheetOption
import com.logflare.android.R
import com.logflare.android.ui.VisualQaTags

@Composable
private fun LoginError?.text(): String? = when (this) {
    null -> null
    LoginError.EnterUrl -> stringResource(R.string.login_error_enter_url)
    LoginError.EnterUsername -> stringResource(R.string.login_error_enter_username)
    LoginError.EnterPassword -> stringResource(R.string.login_error_enter_password)
    LoginError.UrlScheme -> stringResource(R.string.login_error_url_scheme)
    LoginError.NeedsHttps -> stringResource(R.string.login_error_needs_https)
    LoginError.InvalidCredentials -> stringResource(R.string.login_error_invalid_credentials)
    LoginError.RateLimited -> stringResource(R.string.login_error_rate_limited)
    LoginError.Unreachable -> stringResource(R.string.login_error_unreachable)
}

@Composable
fun LoginContent(
    state: LoginUiState,
    onUrl: (String) -> Unit,
    onUsername: (String) -> Unit,
    onPassword: (String) -> Unit,
    onOpenRecent: () -> Unit,
    onPickRecent: (String) -> Unit,
    onDismissRecent: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val submit = {
        focusManager.clearFocus()
        keyboard?.hide()
        onSubmit()
    }
    Column(
        modifier
            .fillMaxSize()
            .background(Lf.colors.surface)
            .testTag(VisualQaTags.Login)
            .focusable()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(start = 24.dp, end = 24.dp, top = 40.dp, bottom = 16.dp),
    ) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(36.dp),
        ) {
            Header()
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                AppTextField(
                    value = state.url,
                    onValueChange = onUrl,
                    label = stringResource(R.string.login_server_url),
                    placeholder = stringResource(R.string.login_server_url_placeholder),
                    modifier = Modifier.testTag(VisualQaTags.LoginServerUrl),
                    error = state.urlError.text(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Next,
                    ),
                    trailing = if (state.recentServers.isEmpty()) null else {
                        { RecentButton(onOpenRecent) }
                    },
                )
                AppTextField(
                    value = state.username,
                    onValueChange = onUsername,
                    label = stringResource(R.string.login_username),
                    placeholder = stringResource(R.string.login_username),
                    modifier = Modifier.testTag(VisualQaTags.LoginUsername),
                    error = state.usernameError.text(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Next,
                    ),
                )
                PasswordField(
                    value = state.password,
                    onValueChange = onPassword,
                    label = stringResource(R.string.login_password),
                    placeholder = stringResource(R.string.login_password),
                    modifier = Modifier.testTag(VisualQaTags.LoginPassword),
                    error = state.passwordError.text(),
                    imeAction = ImeAction.Done,
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                )
            }
        }
        state.formError.text()?.let {
            Text(
                it,
                style = Lf.type.secondary.copy(fontWeight = FontWeight.SemiBold),
                color = Lf.colors.error,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        PrimaryButton(
            text = stringResource(R.string.login_sign_in),
            onClick = submit,
            loading = state.submitting,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    if (state.recentSheetOpen) {
        val current = state.url.trim().trimEnd('/')
        SelectionSheet(
            title = stringResource(R.string.login_recent_servers),
            options = state.recentServers.map { SheetOption(it, it) },
            selected = state.recentServers.firstOrNull { it.trimEnd('/') == current },
            onSelect = { it?.let(onPickRecent) },
            onDismiss = onDismissRecent,
        )
    }
}

@Composable
private fun Header() {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Box(
            Modifier.size(56.dp).clip(Lf.shapes.button).background(Lf.colors.primaryAction),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.logo),
                contentDescription = null,
                modifier = Modifier.size(width = 28.dp, height = 36.dp),
                colorFilter = ColorFilter.tint(Lf.colors.onPrimaryAction),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.login_title), style = Lf.type.largeTitle, color = Lf.colors.textPrimary)
            Text(stringResource(R.string.login_subtitle), style = Lf.type.body, color = Lf.colors.textMuted)
        }
    }
}

@Composable
private fun RecentButton(onClick: () -> Unit) {
    Box(
        Modifier
            .defaultMinSize(minWidth = 44.dp, minHeight = 44.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            stringResource(R.string.login_recent),
            style = Lf.type.secondary.copy(fontWeight = FontWeight.Bold),
            color = Lf.colors.textSecondary,
        )
    }
}
