package com.example.logflare.core.designsystem.v2.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.logflare.core.designsystem.v2.Lf
import com.example.logflare.core.designsystem.v2.LfLevel

/** Stroke icons ported from the prototype SVGs (24x24 viewBox, 2.2 stroke). */
private fun levelVector(name: String, block: PathBuilder.() -> Unit): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2.2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = block,
        )
    }.build()

private fun PathBuilder.circle(cx: Float, cy: Float, r: Float) {
    moveTo(cx - r, cy)
    arcTo(r, r, 0f, true, true, cx + r, cy)
    arcTo(r, r, 0f, true, true, cx - r, cy)
    close()
}

private val ErrorVector = levelVector("LfError") {
    circle(12f, 12f, 9f); moveTo(12f, 7f); lineTo(12f, 13f); moveTo(12f, 17f); lineTo(12.01f, 17f)
}
private val WarningVector = levelVector("LfWarning") {
    moveTo(12f, 4f); lineTo(2.5f, 20f); lineTo(21.5f, 20f); close()
    moveTo(12f, 10f); lineTo(12f, 14f); moveTo(12f, 17f); lineTo(12.01f, 17f)
}
private val InfoVector = levelVector("LfInfo") {
    circle(12f, 12f, 9f); moveTo(12f, 11f); lineTo(12f, 17f); moveTo(12f, 7.5f); lineTo(12.01f, 7.5f)
}

@Composable
private fun levelColors(level: LfLevel): Pair<Color, Color> {
    val c = Lf.colors
    return when (level) {
        LfLevel.Error -> c.errorContainer to c.error
        LfLevel.Warning -> c.warningContainer to c.warning
        LfLevel.Critical -> c.critical to c.onCritical
        LfLevel.Info, LfLevel.Debug -> c.fill to c.textSecondary
    }
}

@Composable
fun LevelIcon(level: LfLevel, size: Dp = 40.dp, modifier: Modifier = Modifier) {
    val (bg, fg) = levelColors(level)
    val vector = when (level) {
        LfLevel.Error, LfLevel.Critical -> ErrorVector
        LfLevel.Warning -> WarningVector
        LfLevel.Info, LfLevel.Debug -> InfoVector
    }
    Box(modifier.size(size).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
        Icon(vector, contentDescription = null, tint = fg, modifier = Modifier.size(size / 2))
    }
}

@Composable
fun LevelBadge(level: LfLevel, modifier: Modifier = Modifier) {
    val (bg, fg) = levelColors(level)
    Text(
        level.name.uppercase(),
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        style = Lf.type.caption.copy(fontWeight = FontWeight.Bold),
        color = fg,
    )
}
