package com.logflare.android.visual

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.logflare.android.feature.log.LogListScreenContent
import com.logflare.android.feature.log.LogsUiState
import com.logflare.android.ui.VisualQaTags
import com.logflare.android.ui.theme.LogflareandroidTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CoreScreenRenderTest {
    @get:Rule val compose = createComposeRule()

    @Test fun logsEmptyRendersWithoutViewModel() {
        compose.setContent {
            LogflareandroidTheme(false) {
                LogListScreenContent(LogsUiState(), onAction = { _ -> })
            }
        }
        compose.onNodeWithTag(VisualQaTags.Logs).assertExists()
    }
}
