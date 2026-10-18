package com.logflare.android.visual.ds

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.logflare.core.designsystem.v2.LfLevel
import com.example.logflare.core.designsystem.v2.components.Avatar
import com.example.logflare.core.designsystem.v2.components.CountBadge
import com.example.logflare.core.designsystem.v2.components.EmptyState
import com.example.logflare.core.designsystem.v2.components.GroupedCard
import com.example.logflare.core.designsystem.v2.components.LevelBadge
import com.example.logflare.core.designsystem.v2.components.LevelIcon
import com.example.logflare.core.designsystem.v2.components.LfScaffold
import com.example.logflare.core.designsystem.v2.components.ListRow
import com.example.logflare.core.designsystem.v2.components.RoleChip
import com.example.logflare.core.designsystem.v2.components.SkeletonCard
import com.example.logflare.core.designsystem.v2.components.StatItem
import com.example.logflare.core.designsystem.v2.components.StatStrip
import com.example.logflare.core.designsystem.v2.components.StatTone
import com.example.logflare.core.designsystem.v2.components.rememberLfToastState
import org.junit.Test

class DsDisplaySnapshots : DsSnapshotTest() {

    @Composable
    private fun Gallery() {
        val toast = rememberLfToastState()
        LaunchedEffect(Unit) { toast.show("Server address copied", durationMs = 60_000) }
        LfScaffold(toastHost = toast) {
            Column(
                Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatStrip(
                    listOf(
                        StatItem("12", "Errors · 24h", StatTone.Error, onClick = {}, selected = true),
                        StatItem("34", "Warnings", StatTone.Warning, onClick = {}),
                        StatItem("4", "Projects", onClick = {}),
                    ),
                )
                GroupedCard {
                    LfLevel.entries.forEach { level ->
                        ListRow(
                            title = "${level.name} message",
                            subtitle = "api-server · 2m ago",
                            leading = { LevelIcon(level) },
                            trailing = {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    LevelBadge(level)
                                }
                            },
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    CountBadge(3, StatTone.Neutral)
                    CountBadge(12, StatTone.Error)
                    CountBadge(5, StatTone.Warning)
                    RoleChip("Owner")
                    RoleChip("Member")
                    Avatar("k")
                    Avatar("m", strong = true)
                }
                SkeletonCard(rows = 2)
                EmptyState("No logs match", "Try another project, level or search term.", "Clear filters", onAction = {})
            }
        }
    }

    @Test fun gallery_light() = capture("ds_display_gallery", false) { Gallery() }
    @Test fun gallery_dark() = capture("ds_display_gallery", true) { Gallery() }
}
