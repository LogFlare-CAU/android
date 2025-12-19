package com.example.logflare_android.feature.auth

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.logflare.core.designsystem.Black
import com.example.logflare.core.designsystem.GreenDefault
import com.example.logflare.core.designsystem.Neutral10
import com.example.logflare.core.designsystem.Neutral20
import com.example.logflare.core.designsystem.Neutral30
import com.example.logflare.core.designsystem.Neutral50
import com.example.logflare.core.designsystem.Neutral60
import com.example.logflare.core.designsystem.Neutral80
import com.example.logflare_android.R

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

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var serverUrl by remember { mutableStateOf("") }
    var serverUrlError by remember { mutableStateOf<String?>(null) }

    fun isValidServerUrl(input: String): Boolean {
        if (input.isBlank()) return true // optional
        return Regex("""^https?://[A-Za-z0-9\.\-]+(:\d+)?(/.*)?$""")
            .matches(input.trim())
    }

    val isServerValid = isValidServerUrl(serverUrl)
    serverUrlError = if (isServerValid) null else "Invalid URL format"

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

        // Server URL input
        OutlinedTextField(
            value = serverUrl,
            onValueChange = { serverUrl = it },
            isError = serverUrlError != null,
            label = { Text("Server URL") },
            placeholder = { Text("http://your-server:port") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (serverUrlError != null) Color.Red else Black,
                unfocusedBorderColor = if (serverUrlError != null) Color.Red else Neutral80,
                errorBorderColor = Color.Red,
                cursorColor = Black
            ),
            singleLine = true
        )

        // 1) 에러가 없어도 공간 유지 + 2) 글자 작게
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = serverUrlError ?: " ",
            color = if (serverUrlError != null) Color.Red else Color.Transparent,
            fontSize = 11.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Username input
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Black,
                unfocusedBorderColor = Neutral80,
                cursorColor = Black
            ),
            singleLine = true
        )

        // 3) 로그인 실패 시 username 밑에 에러 노출 (공간 고정 + 작은 글자)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = uiState.loginError ?: " ",
            color = if (uiState.loginError != null) Color.Red else Color.Transparent,
            fontSize = 11.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Password input
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Black,
                unfocusedBorderColor = Neutral80,
                cursorColor = Black
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // 3) 로그인 실패 시 password 밑에도 같은 에러 노출
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = uiState.loginError ?: " ",
            color = if (uiState.loginError != null) Color.Red else Color.Transparent,
            fontSize = 11.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (serverUrl.isNotBlank()) {
                    viewModel.login(serverUrl, username, password, onSuccess = onLoginSuccess)
                } else {
                    viewModel.login(username, password, onSuccess = onLoginSuccess)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = username.isNotBlank() &&
                    password.isNotBlank() &&
                    isServerValid &&
                    !uiState.loading,
            colors = ButtonColors(
                containerColor = GreenDefault,
                contentColor = Black,
                disabledContainerColor = Neutral10,
                disabledContentColor = Neutral20
            )
        ) {
            Text("Sign In")
        }
    }
}
