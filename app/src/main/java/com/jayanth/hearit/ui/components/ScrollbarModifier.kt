package com.jayanth.hearit.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Draws a slim Material 3 vertical scrollbar thumb whenever [state] has overflow (#11).
 */
fun Modifier.verticalScrollbar(
    state: ScrollState,
    width: Dp = 4.dp,
    color: Color? = null,
): Modifier = composed {
    val thumbColor = color ?: MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    drawWithContent {
        drawContent()
        val maxScroll = state.maxValue
        if (maxScroll > 0 && size.height > 0f) {
            val viewportHeight = size.height
            val totalHeight = viewportHeight + maxScroll
            val thumbHeight = ((viewportHeight / totalHeight) * viewportHeight)
                .coerceIn(24.dp.toPx(), viewportHeight * 0.85f)
            val fraction = state.value.toFloat() / maxScroll.toFloat()
            val topOffset = fraction * (viewportHeight - thumbHeight)
            val barWidth = width.toPx()
            drawRoundRect(
                color = thumbColor,
                topLeft = Offset(x = size.width - barWidth, y = topOffset),
                size = Size(width = barWidth, height = thumbHeight),
                cornerRadius = CornerRadius(x = barWidth / 2f, y = barWidth / 2f),
            )
        }
    }
}
