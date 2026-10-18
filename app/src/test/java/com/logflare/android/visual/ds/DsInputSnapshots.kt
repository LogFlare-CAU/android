package com.logflare.android.visual.ds

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.example.logflare.core.designsystem.v2.LfLevel
import com.example.logflare.core.designsystem.v2.components.AppTextField
import com.example.logflare.core.designsystem.v2.components.DestructiveTextButton
import com.example.logflare.core.designsystem.v2.components.GroupedCard
import com.example.logflare.core.designsystem.v2.components.KeywordChipInput
import com.example.logflare.core.designsystem.v2.components.LfFilterChip
import com.example.logflare.core.designsystem.v2.components.PasswordField
import com.example.logflare.core.designsystem.v2.components.PrimaryButton
import com.example.logflare.core.designsystem.v2.components.SecondaryButton
import com.example.logflare.core.designsystem.v2.components.SegmentedControl
import com.example.logflare.core.designsystem.v2.components.ThresholdLevelSelector
import com.example.logflare.core.designsystem.v2.components.TokenField
import org.junit.Test

class DsInputSnapshots : DsSnapshotTest() {

    @Composable
    private fun Gallery() {
        val focus = remember { FocusRequester() }
        LaunchedEffect(Unit) { focus.requestFocus() }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GroupedCard {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AppTextField("", {}, "Email", placeholder = "you@example.com")
                    AppTextField("payment-api", {}, "Focused", modifier = Modifier.focusRequester(focus), helper = "Lowercase letters and dashes")
                    AppTextField("pay", {}, "Error", error = "Name must be at least 4 characters")
                    PasswordField("secret123", {}, "Password")
                    SegmentedControl(listOf("Member", "Admin"), 0, {})
                }
            }
        }
    }

    @Composable
    private fun ChipsGallery() {
        Column(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GroupedCard {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ThresholdLevelSelector(LfLevel.Warning, {}, "Stores and alerts on Warning, Error and Critical.")
                    KeywordChipInput(listOf("healthcheck", "pool"), {}, {}, "Enter keyword", {})
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LfFilterChip("All projects", false, {})
                LfFilterChip("Error+", true, {})
            }
        }
    }

    @Composable
    private fun ActionsGallery() {
        Column(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GroupedCard {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    TokenField("lf_abcdef1234567890wxyz", false, {}, {}, {})
                    TokenField("lf_abcdef1234567890wxyz", true, {}, {}, null)
                }
            }
            PrimaryButton("Create project", {}, modifier = Modifier.fillMaxWidth())
            PrimaryButton("Create project", {}, enabled = false, modifier = Modifier.fillMaxWidth())
            PrimaryButton("Signing in", {}, loading = true, modifier = Modifier.fillMaxWidth())
            SecondaryButton("Keep editing", {}, modifier = Modifier.fillMaxWidth())
            DestructiveTextButton("Delete project", {})
        }
    }

    @Test fun fields_light() = capture("ds_input_fields", false) { Gallery() }
    @Test fun fields_dark() = capture("ds_input_fields", true) { Gallery() }
    @Test fun chips_light() = capture("ds_input_chips", false) { ChipsGallery() }
    @Test fun chips_dark() = capture("ds_input_chips", true) { ChipsGallery() }
    @Test fun actions_light() = capture("ds_input_actions", false) { ActionsGallery() }
    @Test fun actions_dark() = capture("ds_input_actions", true) { ActionsGallery() }
}
