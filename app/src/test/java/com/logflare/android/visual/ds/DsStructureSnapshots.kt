package com.logflare.android.visual.ds

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.logflare.core.designsystem.v2.Lf
import com.example.logflare.core.designsystem.v2.components.BottomNavBar
import com.example.logflare.core.designsystem.v2.components.GroupedCard
import com.example.logflare.core.designsystem.v2.components.HomeHeader
import com.example.logflare.core.designsystem.v2.components.LargeTitleHeader
import com.example.logflare.core.designsystem.v2.components.LfScaffold
import com.example.logflare.core.designsystem.v2.components.ListRow
import com.example.logflare.core.designsystem.v2.components.NavIcon
import com.example.logflare.core.designsystem.v2.components.NavItem
import com.example.logflare.core.designsystem.v2.components.SettingRow
import com.example.logflare.core.designsystem.v2.components.SubpageTopBar
import com.example.logflare.core.designsystem.v2.components.SwitchRow
import org.junit.Test

class DsStructureSnapshots : DsSnapshotTest() {

    private val navItems = listOf(
        NavItem("home", "Home", Icons.Outlined.Home, Icons.Filled.Home),
        NavItem("logs", "Logs", Icons.Filled.Menu, Icons.Filled.Menu),
        NavItem("projects", "Projects", Icons.Filled.Settings, Icons.Filled.Settings),
        NavItem("my", "My", Icons.Outlined.Person, Icons.Filled.Person),
    )

    @Composable
    private fun Gallery() {
        LfScaffold(
            bottomBar = { BottomNavBar(navItems, selected = "logs", onSelect = {}) },
        ) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LargeTitleHeader("My")
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    GroupedCard {
                        ListRow("api-server", subtitle = "production - 12 errors", showChevron = true, onClick = {})
                        ListRow("A very long project name that must be truncated with an ellipsis at the end", showChevron = true, onClick = {})
                        ListRow("web-client", subtitle = "staging", onClick = {})
                    }
                    GroupedCard {
                        SettingRow("Push alerts", "Warning and above", onClick = {})
                        SwitchRow("Quiet hours", "23:00-07:00 - alerts are held", checked = true, onCheckedChange = {})
                        SwitchRow("Show previews", null, checked = false, onCheckedChange = {})
                    }
                }
            }
        }
    }

    @Composable
    private fun Headers() {
        Column(Modifier.fillMaxSize()) {
            HomeHeader(logo = painterResource(com.logflare.android.R.drawable.logo)) {
                IconButton(onClick = {}) { Icon(Icons.Filled.Search, "Search", tint = Lf.colors.textSecondary) }
                IconButton(onClick = {}) { Icon(Icons.Filled.Notifications, "Notifications", tint = Lf.colors.textSecondary) }
            }
            SubpageTopBar("api-server", onNav = {}) {
                IconButton(onClick = {}) { Icon(Icons.Filled.Settings, "Settings", tint = Lf.colors.textSecondary) }
            }
            SubpageTopBar("Edit project", onNav = {}, navIcon = NavIcon.Close)
        }
    }

    @Test fun gallery_light() = capture("ds_structure_gallery", false) { Gallery() }
    @Test fun gallery_dark() = capture("ds_structure_gallery", true) { Gallery() }
    @Test fun headers_light() = capture("ds_structure_headers", false) { Headers() }
    @Test fun headers_dark() = capture("ds_structure_headers", true) { Headers() }
}
