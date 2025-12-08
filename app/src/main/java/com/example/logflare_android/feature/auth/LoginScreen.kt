package com.example.logflare_android.feature.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.logflare.core.designsystem.AppTheme
import com.example.logflare.core.designsystem.components.button.PrimaryButton
import com.example.logflare.core.designsystem.components.input.LogFlareTextField
import com.example.logflare.core.designsystem.R as CoreR

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
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var serverUrl by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Vertical logo (icon + text) centered at the top
        Image(
            painter = painterResource(id = CoreR.drawable.ic_logflare_logo_vertical),
            contentDescription = "LogFlare Logo",
            modifier = Modifier
                .width(106.dp)
                .height(107.dp)
        )

        // Space between logo and first input (s8)
        Spacer(modifier = Modifier.height(AppTheme.spacing.s8))

        // Username, Password, Server URL inputs (s2 between each)
        Column(
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.s2),
            modifier = Modifier.fillMaxWidth()
        ) {
            LogFlareTextField(
                value = username,   
                onValueChange = { username = it },
                label = null,
                placeholder = "Username",
                modifier = Modifier.fillMaxWidth()
            )

            LogFlareTextField(
                value = password,
                onValueChange = { password = it },
                label = null,
                placeholder = "Password",
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation()
            )

            LogFlareTextField(
                value = serverUrl,
                onValueChange = { serverUrl = it },
                label = null,
                placeholder = "http://your-server:port",
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Space between inputs block and Sign In button (s8)
        Spacer(modifier = Modifier.height(AppTheme.spacing.s8))

        // Sign In button (primary CTA, design system button)
        PrimaryButton(
            text = "Sign In",
            onClick = {
                if (serverUrl.isNotBlank()) {
                    viewModel.login(serverUrl, username, password, onSuccess = onLoginSuccess)
                } else {
                    viewModel.login(username, password, onSuccess = onLoginSuccess)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = username.isNotBlank() && password.isNotBlank()
        )
    }
}
