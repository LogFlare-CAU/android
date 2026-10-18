package com.example.logflare.core.designsystem.v2.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.logflare.core.designsystem.v2.Lf

private val EmptyLines = ImageVector.Builder("LfEmptyLines", 24.dp, 24.dp, 24f, 24f).apply {
    path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round) {
        moveTo(4f, 6f); lineTo(20f, 6f); moveTo(7f, 12f); lineTo(17f, 12f); moveTo(10f, 18f); lineTo(14f, 18f)
    }
}.build()

@Composable
fun EmptyState(
    title: String,
    body: String?,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(Lf.shapes.card)
            .background(Lf.colors.surface)
            .padding(horizontal = 24.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier.size(56.dp).clip(CircleShape).background(Lf.colors.background),
            contentAlignment = Alignment.Center,
        ) {
            Icon(EmptyLines, contentDescription = null, tint = Lf.colors.icon, modifier = Modifier.size(26.dp))
        }
        Text(
            title,
            style = Lf.type.barTitle.copy(fontWeight = FontWeight.ExtraBold),
            color = Lf.colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        if (body != null) {
            Text(body, style = Lf.type.body, color = Lf.colors.textMuted, textAlign = TextAlign.Center)
        }
        if (actionLabel != null && onAction != null) {
            Box(
                Modifier
                    .padding(top = 8.dp)
                    .height(44.dp)
                    .clip(Lf.shapes.input)
                    .background(Lf.colors.secondaryAction)
                    .clickable(role = Role.Button, onClick = onAction)
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    actionLabel,
                    style = Lf.type.body.copy(fontWeight = FontWeight.Bold),
                    color = Lf.colors.textPrimary,
                )
            }
        }
    }
}
