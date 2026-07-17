package com.logflare.android.visual

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.logflare.android.ui.VisualQaTags
import com.logflare.android.ui.common.EmptyState
import org.junit.Test

class SharedStateSnapshots : VisualSnapshotTest() {
    @Test fun shared_fullscreen_loading_light() {
        capture("shared_fullscreen_loading", darkTheme = false) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(VisualQaTags.Loading)
                    .padding(top = 48.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
    }

    @Test fun shared_fullscreen_loading_dark() {
        capture("shared_fullscreen_loading", darkTheme = true) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(VisualQaTags.Loading)
                    .padding(top = 48.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
    }

    @Test fun shared_fullscreen_error_light() {
        capture("shared_fullscreen_error", darkTheme = false) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(VisualQaTags.Error)
                    .padding(top = 48.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Error: Network unavailable",
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }

    @Test fun shared_fullscreen_error_dark() {
        capture("shared_fullscreen_error", darkTheme = true) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(VisualQaTags.Error)
                    .padding(top = 48.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Error: Network unavailable",
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }

    @Test fun shared_empty_light() {
        capture("shared_empty", darkTheme = false) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                EmptyState(projectFiltered = true)
            }
        }
    }

    @Test fun shared_empty_dark() {
        capture("shared_empty", darkTheme = true) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                EmptyState(projectFiltered = true)
            }
        }
    }
}
