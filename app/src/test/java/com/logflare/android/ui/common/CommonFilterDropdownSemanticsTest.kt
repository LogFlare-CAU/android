package com.logflare.android.ui.common

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.logflare.android.ui.theme.LogflareandroidTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CommonFilterDropdownSemanticsTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun interactionTagClickOpensDropdown() {
        compose.setContent {
            LogflareandroidTheme(false) {
                CommonFilterDropdown(
                    title = "Log Level",
                    interactionTag = "filter_log_level",
                ) {
                    Text("ERROR_OPTION")
                }
            }
        }
        compose.onNodeWithTag("filter_log_level").performClick()
        compose.onNodeWithText("ERROR_OPTION").assertExists()
    }
}
