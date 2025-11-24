package com.example.logflare_android.feature.log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.logflare_android.feature.log.components.LogFilter
import com.example.logflare_android.feature.log.components.LogItem

/**
 * Log list screen with filtering support.
 * Features:
 * - Filter by log level (Debug, Info, Error, etc.)
 * - Optimized LazyColumn for large datasets (500+ logs)
 * - Real-time updates via FCM (TODO: integrate with LogflareMessagingService)
 * 
 * @param projectId The project ID to show logs for, or null for all/recent logs
 */
@Composable
fun LogListScreen(
    projectId: Int?,
    viewModel: LogViewModel = hiltViewModel()
) {
    val uiState by viewModel.ui.collectAsState()
    
    // Load logs when projectId changes
    LaunchedEffect(projectId) {
        projectId?.let { viewModel.refresh(it) }
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Filter bar
        LogFilter(
            selectedLevel = uiState.filter,
            onLevelSelected = { level -> viewModel.setFilter(level) }
        )
        
        // Log content
        when {
            uiState.loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            uiState.error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Error: ${uiState.error}",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            uiState.items.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (projectId == null) "No logs available" else "No logs for this project",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (uiState.filter != null) {
                            Text(
                                text = "Try adjusting the filter",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = uiState.items,
                        key = { it.id }
                    ) { log ->
                        LogItem(log = log)
                    }
                }
            }
        }
    }
}
