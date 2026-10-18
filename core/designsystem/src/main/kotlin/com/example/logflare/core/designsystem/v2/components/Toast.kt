package com.example.logflare.core.designsystem.v2.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.logflare.core.designsystem.v2.Lf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Single-message toast. A new [show] replaces the current message and restarts the timer. */
class LfToastState(private val scope: CoroutineScope) {
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message
    private var hideJob: Job? = null

    fun show(text: String, durationMs: Long = 1800) {
        hideJob?.cancel()
        _message.value = text
        hideJob = scope.launch {
            delay(durationMs)
            _message.value = null
        }
    }
}

@Composable
fun rememberLfToastState(): LfToastState {
    val scope = rememberCoroutineScope()
    return remember(scope) { LfToastState(scope) }
}

@Composable
fun LfToastHost(state: LfToastState, modifier: Modifier = Modifier) {
    val text by state.message.collectAsState()
    val current = text ?: return
    Box(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            current,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { liveRegion = LiveRegionMode.Polite }
                .clip(Lf.shapes.input)
                .background(Lf.colors.primaryAction)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            style = Lf.type.body.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
            color = Lf.colors.onPrimaryAction,
        )
    }
}
