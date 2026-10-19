package com.logflare.android.feature.auth

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.logflare.core.designsystem.v2.Lf
import kotlin.math.PI
import kotlin.math.sin

/** The QR screen is a camera screen: it stays dark in both themes, so it does not use [Lf.colors]. */
internal object ScanColors {
    val Screen = Color(0xFF0E1116)
    val Viewfinder = Color(0xFF1A1F27)
    val Text = Color.White
    val Muted = Color(0xFFAEB5BE)
    val Border = Color.White.copy(alpha = 0.35f)
    val ScanLine = Color(0xFF6EE7A8)
    val AlertContainer = Color(0xFFFF5C5C).copy(alpha = 0.16f)
    val AlertText = Color(0xFFFFB3B3)
}

/** Outlined button: 2dp border, transparent or [background] fill, optional leading icon. */
@Composable
internal fun OutlinedActionButton(
    text: String,
    onClick: () -> Unit,
    border: Color,
    content: Color,
    modifier: Modifier = Modifier,
    background: Color = Color.Transparent,
    height: Dp = 52.dp,
    shape: Shape = RoundedCornerShape(16.dp),
    icon: ImageVector? = null,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(background)
            .border(2.dp, border, shape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(20.dp))
            Text(text, style = Lf.type.bodyStrong.copy(fontWeight = FontWeight.Bold), color = content)
        }
    }
}

/** [OutlinedActionButton] in the dark camera-screen palette. */
@Composable
internal fun ScanOutlinedButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, height: Dp = 52.dp) =
    OutlinedActionButton(text, onClick, ScanColors.Border, ScanColors.Text, modifier, height = height)

/** Text-only action, 44dp tall. */
@Composable
internal fun TextActionButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = Lf.type.secondary.copy(fontWeight = FontWeight.Bold),
            color = Lf.colors.textMuted,
            textAlign = TextAlign.Center,
        )
    }
}

/** Message box on a tinted container; a status or alert, not a field error. */
@Composable
internal fun MessageBox(
    text: AnnotatedString,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(14.dp),
    padding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    lineHeight: Float = 1.45f,
) {
    Box(modifier.fillMaxWidth().clip(shape).background(container).padding(padding)) {
        Text(
            text,
            style = Lf.type.body.copy(fontSize = 14.sp, lineHeight = (14 * lineHeight).sp),
            color = content,
        )
    }
}

/** 280dp rounded viewfinder: [camera] clipped inside, white corner marks, a green scan line. */
@Composable
internal fun ScanViewfinder(modifier: Modifier = Modifier, camera: @Composable () -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    val phase by rememberInfiniteTransition(label = "scan-line").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3200, easing = LinearEasing), RepeatMode.Restart),
        label = "scan-line-phase",
    )
    Box(modifier.size(280.dp).clip(shape).background(ScanColors.Viewfinder)) {
        camera()
        Canvas(Modifier.fillMaxSize()) {
            drawCorners()
            drawScanLine(0.5f + 0.32f * sin(2 * PI * phase).toFloat())
        }
    }
}

private fun DrawScope.drawCorners() {
    val inset = 20.dp.toPx()
    val length = 44.dp.toPx()
    val stroke = 4.dp.toPx()
    val outer = 12.dp.toPx()
    // The CSS border sits inside the 44dp box, so the stroke centre is half a stroke in.
    val half = stroke / 2
    val radius = outer - half
    val corner = Path().apply {
        moveTo(inset + half, inset + length)
        lineTo(inset + half, inset + half + radius)
        arcTo(Rect(inset + half, inset + half, inset + half + 2 * radius, inset + half + 2 * radius), 180f, 90f, false)
        lineTo(inset + length, inset + half)
    }
    val style = Stroke(width = stroke, cap = StrokeCap.Butt)
    val center = Offset(size.width / 2, size.height / 2)
    for ((sx, sy) in listOf(1f to 1f, -1f to 1f, 1f to -1f, -1f to -1f)) {
        scale(sx, sy, pivot = center) { drawPath(corner, Color.White, style = style) }
    }
}

private fun DrawScope.drawScanLine(fraction: Float) {
    val y = size.height * fraction
    val x0 = 40.dp.toPx()
    val width = size.width - 2 * x0
    val glow = 14.dp.toPx()
    drawRect(
        Brush.verticalGradient(
            listOf(Color.Transparent, ScanColors.ScanLine.copy(alpha = 0.35f), Color.Transparent),
            startY = y - glow,
            endY = y + glow,
        ),
        topLeft = Offset(x0, y - glow),
        size = Size(width, 2 * glow),
    )
    drawRect(ScanColors.ScanLine, topLeft = Offset(x0, y - 1.dp.toPx()), size = Size(width, 2.dp.toPx()))
}
