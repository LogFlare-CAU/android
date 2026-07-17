package com.logflare.android.feature.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import com.example.logflare.core.designsystem.GreenDefault
import com.example.logflare.core.designsystem.Neutral10
import com.example.logflare.core.designsystem.Neutral20
import com.logflare.android.R
import com.logflare.android.ui.VisualQaTags

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
 */
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.ui.collectAsState()
    var form by remember { mutableStateOf(LoginFormState()) }

    LoginScreenContent(
        uiState = uiState,
        form = form,
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
    )
}

@Composable
fun LoginScreenContent(
    uiState: AuthUiState,
    form: LoginFormState,
    onFormChange: (LoginFormState) -> Unit,
    onSignIn: () -> Unit,
) {
    val isServerValid = form.serverUrlError == null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag(VisualQaTags.Login)
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
            placeholder = { Text("http://your-server:port") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (form.serverUrlError != null) AppTheme.colors.red.default else AppTheme.colors.onSurface,
                unfocusedBorderColor = if (form.serverUrlError != null) AppTheme.colors.red.default else AppTheme.colors.outline,
                errorBorderColor = AppTheme.colors.red.default,
                cursorColor = AppTheme.colors.onSurface
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = form.serverUrlError ?: " ",
            color = if (form.serverUrlError != null) AppTheme.colors.red.default else Color.Transparent,
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
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppTheme.colors.onSurface,
                unfocusedBorderColor = AppTheme.colors.outline,
                cursorColor = AppTheme.colors.onSurface
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = uiState.loginError ?: " ",
            color = if (uiState.loginError != null) AppTheme.colors.red.default else Color.Transparent,
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
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppTheme.colors.onSurface,
                unfocusedBorderColor = AppTheme.colors.outline,
                cursorColor = AppTheme.colors.onSurface
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = uiState.loginError ?: " ",
            color = if (uiState.loginError != null) AppTheme.colors.red.default else Color.Transparent,
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
            colors = ButtonColors(
                containerColor = GreenDefault,
                contentColor = AppTheme.colors.onPrimary,
                disabledContainerColor = Neutral10,
                disabledContentColor = Neutral20
            )
        ) {
            Text("Sign In")
        }
    }
}
