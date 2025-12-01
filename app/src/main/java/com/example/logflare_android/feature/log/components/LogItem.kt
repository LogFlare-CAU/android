package com.example.logflare_android.feature.log.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.logflare.core.model.ErrorlogDTO
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Log item component displaying individual log entry.
 * Shows level, timestamp, error type, and message.
 */
@Composable
fun LogItem(
    log: ErrorlogDTO,
    modifier: Modifier = Modifier
) {
    val levelColor = when (log.level.uppercase()) {
        "DEBUG" -> Color(0xFF2196F3) // Blue
        "INFO" -> Color(0xFF4CAF50) // Green
        "WARN" -> Color(0xFFFFC107) // Amber
        "ERROR" -> Color(0xFFF44336) // Red
        "FATAL" -> Color(0xFF9C27B0) // Purple
        else -> MaterialTheme.colorScheme.onSurface
    }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Level and Timestamp
        Text(
            text = "[${log.level.uppercase()}] ${formatTimestamp(log.timestamp)}",
            style = MaterialTheme.typography.labelMedium,
            color = levelColor
        )
        
        // Error Type
        log.errortype?.let { errorType ->
            Text(
                text = errorType,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        // Message
        Text(
            text = log.message,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatTimestamp(timestamp: String?): String {
    if (timestamp == null) return "N/A"
    
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        val date = parser.parse(timestamp)
        val formatter = SimpleDateFormat("MM/dd HH:mm:ss", Locale.getDefault())
        formatter.format(date ?: Date())
    } catch (e: Exception) {
        timestamp
    }
}
