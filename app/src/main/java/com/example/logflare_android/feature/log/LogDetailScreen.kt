package com.example.logflare_android.feature.log

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.logflare_android.R
import com.example.logflare.core.designsystem.AppTheme
import com.example.logflare_android.ui.common.EmptyState
import com.example.logflare_android.ui.common.GlobalLogCard
import com.example.logflare_android.ui.common.LogCardInfo
import com.example.logflare_android.ui.common.TopTitle

@Composable
fun LogDetailScreen(
    onBack: () -> Unit,
    vm: LogDetailViewModel = hiltViewModel(),
) {
    val log = vm.getLogDetail() ?: return EmptyState(true)
    LogDetailScreenContent(
        onBack = onBack,
        log = log
    )
}

@Composable
fun LogDetailScreenContent(
    onBack: () -> Unit,
    log: LogCardInfo,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.neutral.white)
            .padding(bottom = 16.dp)
    ) {
        TopTitle("Log Details", onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(state = rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "Log Info",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp).fillMaxWidth())
            GlobalLogCard(log = log)
            Spacer(modifier = Modifier.height(12.dp).fillMaxWidth())
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "Raw Data",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp).fillMaxWidth())
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(color = AppTheme.colors.neutral.s20)
                    .padding(24.dp),
                text = log.message,
                fontFamily = Cascadia,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

val Cascadia = FontFamily(
    Font(R.font.cascdiacode, weight = FontWeight.Normal)
)