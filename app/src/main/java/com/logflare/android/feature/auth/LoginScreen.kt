package com.logflare.android.feature.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.logflare.core.designsystem.AppTheme
import com.logflare.android.R
import com.logflare.android.data.ThemePreference
import com.logflare.android.ui.VisualQaTags
import com.logflare.android.ui.theme.logflareOutlinedTextFieldColors
import com.logflare.android.viewmodel.ThemeViewModel

data class LoginFormState(
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val serverUrlError: String? = null,
)

internal fun validateServerUrl(input: String): String? =
    if (input.isBlank() || Regex("""^https?://[A-Za-z0-9.\-]+(:\d+)?(/.*)?$""").matches(input.trim())) {
        null
    } else {
        "Invalid URL format"
    }

/**
 * Login screen following the design specifications.
 * Features:
 * - Logo placement at top
 * - Username and Password input fields
 * - Sign In button
 * - Light/Dark theme toggle
 */
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.ui.collectAsState()
    var form by remember { mutableStateOf(LoginFormState()) }
    val systemDark = isSystemInDarkTheme()
    val preference by themeViewModel.preference.collectAsState()
    val isDark = when (preference) {
        ThemePreference.SYSTEM -> systemDark
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
    }

    LoginScreenContent(
        uiState = uiState,
        form = form,
        isDarkTheme = isDark,
        onFormChange = { updated ->
            form = if (updated.serverUrl != form.serverUrl) {
                updated.copy(serverUrlError = validateServerUrl(updated.serverUrl))
            } else {
                updated
            }
        },
        onSignIn = {
            if (form.serverUrl.isNotBlank()) {
                viewModel.login(form.serverUrl, form.username, form.password, onSuccess = onLoginSuccess)
            } else {
                viewModel.login(form.username, form.password, onSuccess = onLoginSuccess)
            }
        },
        onToggleTheme = { themeViewModel.toggleLightDark(systemDark) },
    )
}

@Composable
fun LoginScreenContent(
    uiState: AuthUiState,
    form: LoginFormState,
    onFormChange: (LoginFormState) -> Unit,
    onSignIn: () -> Unit,
    isDarkTheme: Boolean = false,
    onToggleTheme: (() -> Unit)? = null,
) {
    val isServerValid = form.serverUrlError == null
    val colors = AppTheme.colors

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag(VisualQaTags.Login),
    ) {
        if (onToggleTheme != null) {
            TextButton(
                onClick = onToggleTheme,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(8.dp),
            ) {
                Text(
                    text = if (isDarkTheme) "Light" else "Dark",
                    color = colors.onBackground,
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.logowithlabel),
                contentDescription = "LogFlare Logo",
                modifier = Modifier.size(140.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            OutlinedTextField(
                value = form.serverUrl,
                onValueChange = { onFormChange(form.copy(serverUrl = it)) },
                isError = form.serverUrlError != null,
                label = { Text("Server URL") },
                placeholder = { Text("https://your-server") },
                modifier = Modifier.fillMaxWidth(),
                colors = logflareOutlinedTextFieldColors(isError = form.serverUrlError != null),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = form.serverUrlError ?: " ",
                color = if (form.serverUrlError != null) colors.red.default else Color.Transparent,
                fontSize = 11.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = form.username,
                onValueChange = { onFormChange(form.copy(username = it)) },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth(),
                colors = logflareOutlinedTextFieldColors(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = uiState.loginError ?: " ",
                color = if (uiState.loginError != null) colors.red.default else Color.Transparent,
                fontSize = 11.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = form.password,
                onValueChange = { onFormChange(form.copy(password = it)) },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                colors = logflareOutlinedTextFieldColors(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = uiState.loginError ?: " ",
                color = if (uiState.loginError != null) colors.red.default else Color.Transparent,
                fontSize = 11.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onSignIn,
                modifier = Modifier.fillMaxWidth(),
                enabled = form.username.isNotBlank() &&
                        form.password.isNotBlank() &&
                        isServerValid &&
                        !uiState.loading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary.default,
                    contentColor = colors.onPrimary,
                    disabledContainerColor = colors.primary.disabled,
                    disabledContentColor = colors.onPrimary,
                )
            ) {
                Text(if (uiState.loading) "Signing In…" else "Sign In")
            }
        }
    }
}
