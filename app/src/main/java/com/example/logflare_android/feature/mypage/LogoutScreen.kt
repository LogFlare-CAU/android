package com.example.logflare_android.feature.mypage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.logflare_android.feature.mypage.MyPageViewModel
import com.example.logflare_android.ui.components.BottomOutlinedButton
import com.example.logflare_android.ui.components.BottomPrimaryButton

private val ColorNeutralWhite = Color(0xFFFFFFFF)
private val ColorNeutralBlack = Color(0xFF1A1A1A)
private val ColorNeutral20 = Color(0xFFEEEEEE)
private val ColorNeutral60 = Color(0xFF757575)
private val ColorNeutral70 = Color(0xFF616161)
private val ColorPrimaryDefault = Color(0xFF60B176)
private val ColorDanger = Color(0xFFB12B38)

@Composable
fun LogoutScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MyPageViewModel = hiltViewModel()
) {
    val uiState by viewModel.ui.collectAsState()
    var hasConfirmedLogout by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(hasConfirmedLogout) {
        if (hasConfirmedLogout) {
            viewModel.logout(onLogout)
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = ColorNeutralWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            LogoutContent(
                username = uiState.username ?: "User",
                onConfirmLogout = { hasConfirmedLogout = true },
                onCancel = onBack
            )
        }
    }
}


@Composable
private fun LogoutContent(
    username: String,
    onConfirmLogout: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = ColorNeutral20,
            modifier = Modifier.size(100.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = ColorNeutral60,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Log Out?",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = ColorNeutralBlack,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Are you sure you want to log out from your account \"$username\"?",
            fontSize = 14.sp,
            color = ColorNeutral70,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        BottomPrimaryButton(
            text = "Log Out",
            onClick = onConfirmLogout,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            containerColor = ColorDanger
        )

        Spacer(modifier = Modifier.height(12.dp))

        BottomOutlinedButton(
            text = "Cancel",
            onClick = onCancel,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(bottom = 24.dp)
        )
    }
}
