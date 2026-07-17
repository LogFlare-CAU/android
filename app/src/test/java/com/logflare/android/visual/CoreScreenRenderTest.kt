package com.logflare.android.visual

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.logflare.android.feature.auth.AuthUiState
import com.logflare.android.feature.auth.LoginFormState
import com.logflare.android.feature.auth.LoginScreenContent
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

    @Test fun loginRendersWithoutViewModel() {
        compose.setContent {
            LogflareandroidTheme(false) {
                LoginScreenContent(
                    uiState = AuthUiState(),
                    form = LoginFormState(),
                    onFormChange = { _ -> },
                    onSignIn = {},
                )
            }
        }
        compose.onNodeWithTag(VisualQaTags.Login).assertExists()
    }

    @Test fun logsEmptyRendersWithoutViewModel() {
        compose.setContent {
            LogflareandroidTheme(false) {
                LogListScreenContent(LogsUiState(), onAction = { _ -> })
            }
        }
        compose.onNodeWithTag(VisualQaTags.Logs).assertExists()
    }
}
