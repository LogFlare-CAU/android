package com.example.logflare.core.designsystem

import androidx.compose.ui.graphics.Color

/**
 * LogFlare color palette.
 * TODO: Define colors based on actual design specifications.
 */
object LogFlareColors {
    // Primary colors
    val Primary = Color(0xFF6200EE)
    val PrimaryVariant = Color(0xFF3700B3)
    val Secondary = Color(0xFF03DAC6)
    
    // Log level colors
    val LogDebug = Color(0xFF2196F3) // Blue
    val LogInfo = Color(0xFF4CAF50) // Green
    val LogWarn = Color(0xFFFFC107) // Amber
    val LogError = Color(0xFFF44336) // Red
    val LogFatal = Color(0xFF9C27B0) // Purple
    
    // Status colors
    val StatusConnected = Color(0xFF4CAF50) // Green
    val StatusDisconnected = Color(0xFFF44336) // Red
    
    // Background colors
    val Surface = Color(0xFFFFFFFF)
    val SurfaceVariant = Color(0xFFF5F5F5)
    val Background = Color(0xFFFAFAFA)
}
