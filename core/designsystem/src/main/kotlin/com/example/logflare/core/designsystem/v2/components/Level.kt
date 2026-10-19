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
import androidx.compose.ui.graphics.PathFillType
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
import com.example.logflare.core.designsystem.v2.LfColors
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
/** Lucide-style bug: oval body, head, antennae and legs. */
private val DebugVector = levelVector("LfDebug") {
    moveTo(7f, 14.5f); arcTo(5f, 6f, 0f, true, true, 17f, 14.5f); arcTo(5f, 6f, 0f, true, true, 7f, 14.5f); close()
    moveTo(9f, 8.5f); arcTo(3f, 3f, 0f, false, true, 15f, 8.5f)
    moveTo(9.8f, 6.2f); lineTo(8f, 4f); moveTo(14.2f, 6.2f); lineTo(16f, 4f)
    moveTo(7f, 13f); lineTo(3.5f, 12f); moveTo(17f, 13f); lineTo(20.5f, 12f)
    moveTo(7.3f, 17.5f); lineTo(4f, 19.5f); moveTo(16.7f, 17.5f); lineTo(20f, 19.5f)
    moveTo(12f, 12f); lineTo(12f, 20f)
}

/**
 * Filled stop-sign octagon with the "!" knocked out. Solid (not a stroke like Error's circle) so
 * Critical reads differently from Error by shape alone, even at 20dp.
 */
private val CriticalVector: ImageVector =
    ImageVector.Builder("LfCritical", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd) {
            moveTo(8.1f, 2f); lineTo(15.9f, 2f); lineTo(22f, 8.1f); lineTo(22f, 15.9f)
            lineTo(15.9f, 22f); lineTo(8.1f, 22f); lineTo(2f, 15.9f); lineTo(2f, 8.1f); close()
            // "!" bar and dot (holes via even-odd).
            moveTo(10.7f, 6.5f); lineTo(13.3f, 6.5f); lineTo(12.9f, 13.6f); lineTo(11.1f, 13.6f); close()
            moveTo(10.5f, 17f); arcTo(1.5f, 1.5f, 0f, true, true, 13.5f, 17f); arcTo(1.5f, 1.5f, 0f, true, true, 10.5f, 17f); close()
        }
    }.build()

private val InfoVector = levelVector("LfInfo") {
    circle(12f, 12f, 9f); moveTo(12f, 11f); lineTo(12f, 17f); moveTo(12f, 7.5f); lineTo(12.01f, 7.5f)
}

/** (tile, glyph) for [level]; LevelBadge uses the same pair for its label. */
internal fun levelTileColors(c: LfColors, level: LfLevel): Pair<Color, Color> =
    when (level) {
        LfLevel.Error -> c.errorContainer to c.error
        LfLevel.Warning -> c.warningContainer to c.warning
        LfLevel.Critical -> c.critical to c.onCritical
        LfLevel.Info, LfLevel.Debug -> c.fill to c.textSecondary
    }

/** One distinct shape per level: bug, "i" circle, triangle, "!" circle, solid octagon. */
internal fun levelGlyph(level: LfLevel): ImageVector = when (level) {
    LfLevel.Debug -> DebugVector
    LfLevel.Info -> InfoVector
    LfLevel.Warning -> WarningVector
    LfLevel.Error -> ErrorVector
    LfLevel.Critical -> CriticalVector
}

@Composable
fun LevelIcon(level: LfLevel, size: Dp = 40.dp, modifier: Modifier = Modifier) {
    val (bg, fg) = levelTileColors(Lf.colors, level)
    val vector = levelGlyph(level)
    Box(modifier.size(size).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
        Icon(vector, contentDescription = null, tint = fg, modifier = Modifier.size(size / 2))
    }
}

@Composable
fun LevelBadge(level: LfLevel, modifier: Modifier = Modifier) {
    val (bg, fg) = levelTileColors(Lf.colors, level)
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
