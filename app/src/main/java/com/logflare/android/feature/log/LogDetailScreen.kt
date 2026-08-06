package com.logflare.android.feature.log

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import com.logflare.android.R
import com.example.logflare.core.designsystem.AppTheme
import com.logflare.android.ui.VisualQaTags
import com.logflare.android.ui.common.EmptyState
import com.logflare.android.ui.common.GlobalLogCard
import com.logflare.android.ui.common.LogCardInfo

@Composable
fun LogDetailScreen(
    onBack: () -> Unit,
    vm: LogDetailViewModel = hiltViewModel(),
) {
    val log = vm.getLogDetail()
    LogDetailScreenContent(
        onBack = onBack,
        log = log,
    )
}

@Composable
fun LogDetailScreenContent(
    onBack: () -> Unit,
    log: LogCardInfo?,
    modifier: Modifier = Modifier
) {
    if (log == null) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .testTag(VisualQaTags.LogDetail)
                .background(AppTheme.colors.surface)
                .padding(AppTheme.roles.layout.screenPadding),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(VisualQaTags.Empty),
                contentAlignment = Alignment.Center,
            ) {
                EmptyState(projectFiltered = true)
            }
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag(VisualQaTags.LogDetail)
            .background(AppTheme.colors.surface)
            .padding(bottom = AppTheme.roles.layout.screenPadding)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(state = rememberScrollState())
                .padding(horizontal = AppTheme.roles.layout.screenPadding),
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "Log Info",
                style = AppTheme.typography.titleSection
            )
            Spacer(modifier = Modifier.height(AppTheme.spacing.s2).fillMaxWidth())
            GlobalLogCard(log = log)
            Spacer(modifier = Modifier.height(AppTheme.roles.layout.contentGap).fillMaxWidth())
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "Raw Data",
                style = AppTheme.typography.titleSection
            )
            Spacer(modifier = Modifier.height(AppTheme.spacing.s2).fillMaxWidth())
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AppTheme.radius.large)
                    .background(color = AppTheme.colors.surfaceVariant)
                    .padding(AppTheme.roles.layout.statePadding),
                text = log.message,
                fontFamily = Cascadia,
                style = AppTheme.typography.bodyMdMedium
            )
        }
    }
}

val Cascadia = FontFamily(
    Font(R.font.cascdiacode, weight = FontWeight.Normal)
)
