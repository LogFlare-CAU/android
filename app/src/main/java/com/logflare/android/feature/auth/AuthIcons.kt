package com.logflare.android.feature.auth

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Stroke icons of the login design (24x24 viewBox, round caps and joins), drawn from the mock's
 * SVG paths. Black strokes; `Icon` tints them.
 */
internal object AuthIcons {
    val Shield: ImageVector by lazy { stroked("Shield", 2f, "M12 3l7 3v5c0 5-3.5 8.5-7 10-3.5-1.5-7-5-7-10V6z") }

    val QrFrame: ImageVector by lazy {
        stroked(
            "QrFrame",
            2f,
            "M3 7V4h3M21 7V4h-3M3 17v3h3M21 17v3h-3",
            "M7 8h4v4H7zM13 13h4v4h-4M13 8h4M8 15h3",
        )
    }

    val Check: ImageVector by lazy { stroked("Check", 2.4f, "m5 12 5 5L20 7") }

    val Chevron: ImageVector by lazy { stroked("Chevron", 2.2f, "M15 6l-6 6 6 6") }

    private fun stroked(name: String, width: Float, vararg paths: String): ImageVector {
        val builder = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
        paths.forEach {
            builder.addPath(
                addPathNodes(it),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = width,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        return builder.build()
    }
}
