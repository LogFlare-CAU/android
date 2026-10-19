package com.logflare.android.feature.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.logflare.core.designsystem.v2.Lf
import com.example.logflare.core.designsystem.v2.components.AppTextField
import com.example.logflare.core.designsystem.v2.components.PasswordField
import com.example.logflare.core.designsystem.v2.components.PrimaryButton
import com.example.logflare.core.designsystem.v2.components.SegmentedControl
import com.example.logflare.core.designsystem.v2.components.SelectionSheet
import com.example.logflare.core.designsystem.v2.components.SheetOption
import com.example.logflare.core.designsystem.v2.components.SubpageTopBar
import com.logflare.android.R
import com.logflare.android.ui.VisualQaTags

/** Env var an admin sets on the server to enable sign-in methods; shown bold in the no-methods message. */
private const val AUTH_MODES_ENV = "LOGFLARE_AUTH_MODES"

@Composable
private fun LoginError?.text(): String? = when (this) {
    null -> null
    LoginError.EnterUrl -> stringResource(R.string.login_error_enter_url)
    LoginError.EnterUsername -> stringResource(R.string.login_error_enter_username)
    LoginError.EnterPassword -> stringResource(R.string.login_error_enter_password)
    LoginError.EnterClientId -> stringResource(R.string.login_error_enter_client_id)
    LoginError.EnterClientSecret -> stringResource(R.string.login_error_enter_client_secret)
    LoginError.UrlScheme -> stringResource(R.string.login_error_url_scheme)
    LoginError.NeedsHttps -> stringResource(R.string.login_error_needs_https)
    LoginError.InvalidCredentials -> stringResource(R.string.login_error_invalid_credentials)
    LoginError.RateLimited -> stringResource(R.string.login_error_rate_limited)
    LoginError.Unreachable -> stringResource(R.string.login_error_unreachable)
    LoginError.AccessRejected -> stringResource(R.string.login_error_access_rejected)
    LoginError.AccessUnavailable -> stringResource(R.string.login_error_access_unavailable)
    LoginError.QrInvalid -> stringResource(R.string.login_error_qr_invalid)
}

/**
 * The login flow: [LoginStep.Form] and the Access steps (scan, manual entry, confirm).
 * [camera] draws the live preview inside the scan viewfinder; tests pass a placeholder.
 */
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
    onSelectMode: (LoginMode) -> Unit = {},
    onCommitUrl: () -> Unit = {},
    onScan: () -> Unit = {},
    onEnterManually: () -> Unit = {},
    onQr: (String) -> Unit = {},
    onManualContinue: (clientId: String, secret: String) -> Unit = { _, _ -> },
    onManualEdit: () -> Unit = {},
    onManualServer: (String) -> Unit = {},
    onKeepLoggedIn: (Boolean) -> Unit = {},
    onConfirmAccess: () -> Unit = {},
    onBack: () -> Unit = {},
    camera: @Composable (onCode: (String) -> Unit) -> Unit = { AccessQrScanner(onCode = it, modifier = Modifier.fillMaxSize()) },
    modifier: Modifier = Modifier,
) {
    when (state.step) {
        LoginStep.Scan -> {
            ScanStep(state, onQr, onBack, camera, modifier)
            return
        }
        LoginStep.Manual -> {
            ManualStep(state, onManualContinue, onManualEdit, onManualServer, onBack, modifier)
            return
        }
        LoginStep.Confirm -> {
            ConfirmStep(state, onKeepLoggedIn, onConfirmAccess, onBack, modifier)
            return
        }
        LoginStep.Form -> Unit
    }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val submit = {
        focusManager.clearFocus()
        keyboard?.hide()
        onSubmit()
    }
    val noModes = state.modes?.isEmpty() == true
    val access = state.selectedMode == LoginMode.Access
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
                ModePicker(state.selectedMode, onSelectMode)
                if (access) {
                    AccessCard(onScan, onEnterManually)
                } else {
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
                        keyboardActions = KeyboardActions(onNext = { onCommitUrl() }),
                        trailing = if (state.recentServers.isEmpty()) null else {
                            { RecentButton(onOpenRecent) }
                        },
                    )
                    if (noModes) {
                        NoModesBox()
                    } else {
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
            }
        }
        if (!access && state.passwordBlocked) {
            Text(
                stringResource(R.string.login_error_password_not_allowed),
                style = Lf.type.secondary.copy(fontWeight = FontWeight.SemiBold),
                color = Lf.colors.error,
                modifier = Modifier.padding(top = 12.dp).testTag(VisualQaTags.LoginPasswordBlocked),
            )
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
        when {
            !access && noModes -> Unit
            access -> Text(
                stringResource(R.string.login_access_caption),
                style = Lf.type.caption,
                color = Lf.colors.icon,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            else -> PrimaryButton(
                text = stringResource(R.string.login_sign_in),
                onClick = submit,
                loading = state.submitting || state.modesLoading,
                modifier = Modifier.fillMaxWidth().testTag(VisualQaTags.LoginSubmit),
            )
        }
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

/** The two top-level sign-in methods. Always shown: Access does not depend on any server lookup. */
@Composable
private fun ModePicker(selected: LoginMode, onSelectMode: (LoginMode) -> Unit) {
    val modes = listOf(LoginMode.Password, LoginMode.Access)
    SegmentedControl(
        options = listOf(stringResource(R.string.login_mode_password), stringResource(R.string.login_mode_access)),
        selectedIndex = modes.indexOf(selected),
        onSelect = { index -> modes.getOrNull(index)?.let(onSelectMode) },
        modifier = Modifier.testTag(VisualQaTags.LoginModes),
    )
}

/** Access mode body: what it is, scan the setup QR, or type the token. No bottom button. */
@Composable
private fun AccessCard(onScan: () -> Unit, onEnterManually: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Lf.colors.background)
            .padding(18.dp)
            .testTag(VisualQaTags.LoginAccessCard),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            Icon(
                AuthIcons.Shield,
                contentDescription = null,
                tint = Lf.colors.textPrimary,
                modifier = Modifier.padding(top = 1.dp).size(22.dp),
            )
            Text(
                stringResource(R.string.login_access_card),
                style = Lf.type.body.copy(fontSize = 14.sp, lineHeight = 21.sp),
                color = Lf.colors.textSecondary,
            )
        }
        OutlinedActionButton(
            text = stringResource(R.string.login_scan_qr),
            onClick = onScan,
            border = Lf.colors.textPrimary,
            content = Lf.colors.textPrimary,
            background = Lf.colors.surface,
            shape = RoundedCornerShape(14.dp),
            icon = AuthIcons.QrFrame,
            modifier = Modifier.testTag(VisualQaTags.LoginScan),
        )
        TextActionButton(
            stringResource(R.string.login_enter_manually),
            onEnterManually,
            Modifier.align(Alignment.CenterHorizontally).testTag(VisualQaTags.LoginManualEntry),
        )
    }
}

@Composable
private fun NoModesBox() {
    val full = stringResource(R.string.login_no_modes, AUTH_MODES_ENV)
    val text = remember(full) {
        buildAnnotatedString {
            append(full)
            val at = full.indexOf(AUTH_MODES_ENV)
            if (at >= 0) addStyle(SpanStyle(fontWeight = FontWeight.Bold), at, at + AUTH_MODES_ENV.length)
        }
    }
    MessageBox(
        text = text,
        container = Lf.colors.errorContainer,
        content = Lf.colors.error,
        shape = RoundedCornerShape(18.dp),
        padding = PaddingValues(18.dp),
        lineHeight = 1.5f,
        modifier = Modifier.testTag(VisualQaTags.LoginNoModes),
    )
}

@Composable
private fun BackArrow(onClick: () -> Unit, tint: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            AuthIcons.Chevron,
            contentDescription = stringResource(R.string.login_back),
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun ScanStep(
    state: LoginUiState,
    onQr: (String) -> Unit,
    onBack: () -> Unit,
    camera: @Composable (onCode: (String) -> Unit) -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(ScanColors.Screen)
            .testTag(VisualQaTags.LoginScanScreen)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            Modifier.padding(start = 12.dp, end = 12.dp, top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BackArrow(onBack, ScanColors.Text)
            Text(
                stringResource(R.string.login_scan_qr),
                style = Lf.type.barTitle.copy(fontWeight = FontWeight.ExtraBold),
                color = ScanColors.Text,
            )
        }
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                ScanViewfinder(Modifier.semantics { contentDescription = "viewfinder" }) { camera(onQr) }
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.login_scan_heading),
                        style = Lf.type.sectionTitle,
                        color = ScanColors.Text,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        stringResource(R.string.login_scan_body),
                        style = Lf.type.body.copy(fontSize = 14.sp, lineHeight = 21.sp),
                        color = ScanColors.Muted,
                        textAlign = TextAlign.Center,
                    )
                }
                state.scanError.text()?.let {
                    MessageBox(
                        text = AnnotatedString(it),
                        container = ScanColors.AlertContainer,
                        content = ScanColors.AlertText,
                        modifier = Modifier.testTag(VisualQaTags.LoginScanError),
                    )
                }
            }
        }
        ScanOutlinedButton(
            text = stringResource(R.string.login_cancel),
            onClick = onBack,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 32.dp).testTag(VisualQaTags.LoginScanCancel),
        )
    }
}

@Composable
private fun ManualStep(
    state: LoginUiState,
    onContinue: (String, String) -> Unit,
    onEdit: () -> Unit,
    onServer: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier,
) {
    // The secret is only ever in these fields and the VM's private field; it is not in LoginUiState.
    var clientId by remember { mutableStateOf("") }
    var secret by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val proceed = {
        focusManager.clearFocus()
        onContinue(clientId, secret)
    }
    Column(
        modifier
            .fillMaxSize()
            .background(Lf.colors.surface)
            .testTag(VisualQaTags.LoginManualScreen)
            .navigationBarsPadding()
            .imePadding(),
    ) {
        SubpageTopBar(title = stringResource(R.string.login_manual_title), onNav = onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(stringResource(R.string.login_manual_hint), style = Lf.type.body, color = Lf.colors.textMuted)
            AppTextField(
                value = state.manualServer,
                onValueChange = onServer,
                label = stringResource(R.string.login_server_url),
                placeholder = stringResource(R.string.login_server_url_placeholder),
                modifier = Modifier.testTag(VisualQaTags.LoginManualServer),
                error = state.manualServerError.text(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Next,
                ),
            )
            AppTextField(
                value = clientId,
                onValueChange = { clientId = it; onEdit() },
                label = stringResource(R.string.login_client_id),
                placeholder = stringResource(R.string.login_client_id),
                modifier = Modifier.testTag(VisualQaTags.LoginManualId),
                error = state.manualIdError.text(),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Next,
                ),
            )
            PasswordField(
                value = secret,
                onValueChange = { secret = it; onEdit() },
                label = stringResource(R.string.login_client_secret),
                placeholder = stringResource(R.string.login_client_secret),
                modifier = Modifier.testTag(VisualQaTags.LoginManualSecret),
                error = state.manualSecretError.text(),
                imeAction = ImeAction.Done,
                keyboardActions = KeyboardActions(onDone = { proceed() }),
            )
            state.formError.text()?.let {
                Text(it, style = Lf.type.secondary.copy(fontWeight = FontWeight.SemiBold), color = Lf.colors.error)
            }
        }
        PrimaryButton(
            text = stringResource(R.string.login_continue),
            onClick = proceed,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 16.dp)
                .testTag(VisualQaTags.LoginManualContinue),
        )
    }
}

@Composable
private fun ConfirmStep(
    state: LoginUiState,
    onKeepLoggedIn: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(Lf.colors.surface)
            .testTag(VisualQaTags.LoginAccessConfirm)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 32.dp),
    ) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            BackArrow(onBack, Lf.colors.textPrimary, Modifier.offset(x = (-12).dp))
            Spacer(Modifier.height(24.dp))
            Box(
                Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(Lf.colors.successContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(AuthIcons.Check, contentDescription = null, tint = Lf.colors.success, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.login_access_confirm_title),
                style = Lf.type.largeTitle.copy(fontSize = 26.sp, letterSpacing = (-0.5).sp),
                color = Lf.colors.textPrimary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.login_access_confirm_subtitle),
                style = Lf.type.body,
                color = Lf.colors.textMuted,
            )
            Column(
                Modifier
                    .padding(top = 28.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Lf.colors.background)
                    .padding(horizontal = 18.dp, vertical = 4.dp),
            ) {
                DetailRow(stringResource(R.string.login_access_server), Modifier.testTag("login_access_server")) {
                    Text(state.accessServer, style = Lf.type.body.copy(fontWeight = FontWeight.Bold), color = Lf.colors.textPrimary)
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(Lf.colors.divider))
                DetailRow(stringResource(R.string.login_access_client)) {
                    Text(
                        state.accessClientMasked,
                        style = Lf.type.body.copy(fontFamily = FontFamily.Monospace),
                        color = Lf.colors.textPrimary,
                        modifier = Modifier.testTag(VisualQaTags.LoginAccessClient),
                    )
                }
            }
            KeepSignedIn(state.keepLoggedIn, onKeepLoggedIn, Modifier.padding(top = 16.dp))
            state.formError.text()?.let {
                MessageBox(
                    text = AnnotatedString(it),
                    container = Lf.colors.errorContainer,
                    content = Lf.colors.error,
                    modifier = Modifier.padding(top = 16.dp).testTag(VisualQaTags.LoginAccessError),
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        PrimaryButton(
            text = stringResource(R.string.login_access_sign_in),
            onClick = onConfirm,
            loading = state.submitting,
            modifier = Modifier.fillMaxWidth().testTag(VisualQaTags.LoginAccessSubmit),
        )
    }
}

@Composable
private fun DetailRow(label: String, modifier: Modifier = Modifier, value: @Composable () -> Unit) {
    Column(modifier.fillMaxWidth().padding(vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = Lf.type.caption.copy(fontWeight = FontWeight.Bold), color = Lf.colors.textMuted)
        value()
    }
}

@Composable
private fun KeepSignedIn(checked: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .heightIn(min = 44.dp)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onChange)
            .testTag(VisualQaTags.LoginAccessKeep),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val shape = RoundedCornerShape(6.dp)
        Box(
            Modifier
                .size(20.dp)
                .clip(shape)
                .let {
                    if (checked) it.background(Lf.colors.primaryAction)
                    else it.border(2.dp, Lf.colors.icon, shape)
                },
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = Lf.colors.onPrimaryAction,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
        Text(
            stringResource(R.string.login_keep_signed_in),
            style = Lf.type.secondary.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
            color = Lf.colors.textPrimary,
        )
    }
}
