package com.example.logflare_android.feature.mypage

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.logflare.core.designsystem.AppTheme
import com.example.logflare.core.designsystem.components.button.ButtonType
import com.example.logflare.core.designsystem.components.button.ButtonVariant
import com.example.logflare.core.designsystem.components.button.LogFlareButton
import com.example.logflare.core.designsystem.components.feedback.LogFlareSnackbar
import com.example.logflare.core.designsystem.components.navigation.LogFlareTopAppBar
import com.example.logflare.core.designsystem.components.navigation.TopAppBarTitleType

@Composable
fun LogoutScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LogoutViewModel = hiltViewModel()
) {
    val uiState by viewModel.ui.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = AppTheme.colors.neutral.white,
        topBar = {
            LogFlareTopAppBar(
                titleType = TopAppBarTitleType.Title,
                titleText = "Log Out",
                onBack = onBack
            )
        },
        bottomBar = {
            LogoutBottomBar(
                isLoading = uiState.isLoading,
                errorMessage = uiState.errorMessage,
                onCancel = onBack,
                onConfirm = { viewModel.performLogout(onLogout) },
                onDismissError = viewModel::clearError
            )
        }
    ) { innerPadding ->
        LogoutBody(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        )
    }
}

@Composable
private fun LogoutBody(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(horizontal = AppTheme.spacing.s6)
            .padding(top = AppTheme.spacing.s8),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Log Out",
            style = AppTheme.typography.bodyLgBold,
            color = AppTheme.colors.neutral.black,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(AppTheme.spacing.s4))

        Text(
            text = "Are you sure you want to log out?\nYou’ll need to sign in again to use LogFlare",
            style = AppTheme.typography.bodySmLight,
            color = AppTheme.colors.neutral.s70,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LogoutBottomBar(
    isLoading: Boolean,
    errorMessage: String?,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    onDismissError: () -> Unit
) {
    Surface(
        color = AppTheme.colors.neutral.white,
        tonalElevation = 4.dp,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = AppTheme.spacing.s4)
                .padding(vertical = AppTheme.spacing.s3),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.s3)
        ) {
            errorMessage?.let { message ->
                LogFlareSnackbar(
                    message = message,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDismissError() }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.s3)
            ) {
                LogFlareButton(
                    text = "Cancel",
                    onClick = onCancel,
                    variant = ButtonVariant.Primary,
                    type = ButtonType.Outline,
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f)
                )

                LogFlareButton(
                    text = if (isLoading) "Logging out..." else "Log Out",
                    onClick = onConfirm,
                    variant = ButtonVariant.Primary,
                    type = ButtonType.Filled,
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
