package com.example.logflare.core.designsystem.v2

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.dp

@Immutable
data class LfShapes(
    val card: RoundedCornerShape = RoundedCornerShape(24.dp),
    val sheet: RoundedCornerShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    val input: RoundedCornerShape = RoundedCornerShape(14.dp),
    val button: RoundedCornerShape = RoundedCornerShape(16.dp),
)
