package com.example.logflare_android.feature.log.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.logflare_android.feature.log.LogLevel

/**
 * Log filter component with chips for each log level.
 * Allows users to filter logs by severity.
 */
@Composable
fun LogFilter(
    selectedLevel: LogLevel?,
    onLevelSelected: (LogLevel?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // "All" chip to clear filter
        FilterChip(
            selected = selectedLevel == null,
            onClick = { onLevelSelected(null) },
            label = { Text("All") }
        )
        
        // Individual level chips
        LogLevel.entries.forEach { level ->
            FilterChip(
                selected = selectedLevel == level,
                onClick = { onLevelSelected(level) },
                label = { Text(level.name) }
            )
        }
    }
}
