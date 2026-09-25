package com.optimusprime.wick.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.optimusprime.wick.ui.theme.WickColors
import kotlin.math.cos
import kotlin.math.sin

/**
 * The hour hand of a clock, not a countdown: [hourFraction] is how far
 * through the current 60-minute face the session is (elapsed % 1 hour),
 * so the ring completes a lap every hour rather than needing a preset goal.
 * It freezes exactly when [hourFraction] stops changing, so a paused
 * session reads as paused with no extra styling needed.
 */
@Composable
fun TimerDial(
    hourFraction: Float,
    modifier: Modifier = Modifier,
    trackColor: Color = WickColors.brassSoft,
    progressColor: Color = WickColors.brass
) {
    Canvas(modifier = modifier.size(240.dp)) {
        val strokeWidth = 12.dp.toPx()
        val diameter = size.minDimension - strokeWidth
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        val arcSize = Size(diameter, diameter)

        drawArc(
            color = trackColor,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        drawArc(
            color = progressColor,
            startAngle = -90f,
            sweepAngle = 360f * hourFraction.coerceIn(0f, 1f),
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Twelve ticks, one for every 5 minutes — a clock face, not a gauge.
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = diameter / 2f
        val outerR = radius + strokeWidth / 2f + 5.dp.toPx()
        val innerR = outerR - 6.dp.toPx()
        for (i in 0 until 12) {
            val angle = Math.toRadians((-90f + i * 30f).toDouble())
            val cosA = cos(angle).toFloat()
            val sinA = sin(angle).toFloat()
            drawLine(
                color = WickColors.ash.copy(alpha = 0.35f),
                start = Offset(center.x + cosA * innerR, center.y + sinA * innerR),
                end = Offset(center.x + cosA * outerR, center.y + sinA * outerR),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}
