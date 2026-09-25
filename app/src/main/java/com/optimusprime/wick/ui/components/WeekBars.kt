package com.optimusprime.wick.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import com.optimusprime.wick.ui.theme.WickColors
import com.optimusprime.wick.ui.theme.WickType

data class DayBar(val label: String, val minutes: Float, val isToday: Boolean)

/**
 * A week of study minutes, drawn as bars rather than handed to a generic
 * chart library — seven values never needed one.
 */
@Composable
fun WeekBars(days: List<DayBar>, modifier: Modifier = Modifier) {
    val maxMinutes = (days.maxOfOrNull { it.minutes } ?: 0f).coerceAtLeast(1f)
    Column(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
            val slotWidth = size.width / days.size
            val barWidth = slotWidth * 0.42f
            days.forEachIndexed { index, day ->
                val fraction = (day.minutes / maxMinutes).coerceIn(0f, 1f)
                val barHeight = (size.height * fraction).coerceAtLeast(4.dp.toPx())
                val slotCenterX = slotWidth * index + slotWidth / 2f
                val left = slotCenterX - barWidth / 2f
                val top = size.height - barHeight
                val color = if (day.isToday) WickColors.brass else WickColors.brassSoft
                val radius = (barWidth / 2f).coerceAtMost(8.dp.toPx())
                val path = Path().apply {
                    addRoundRect(
                        RoundRect(
                            rect = Rect(offset = Offset(left, top), size = Size(barWidth, barHeight)),
                            topLeft = CornerRadius(radius, radius),
                            topRight = CornerRadius(radius, radius),
                            bottomLeft = CornerRadius.Zero,
                            bottomRight = CornerRadius.Zero
                        )
                    )
                }
                drawPath(path, color = color)
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            days.forEach { day ->
                Text(
                    text = day.label,
                    style = WickType.caption,
                    color = if (day.isToday) WickColors.brass else WickColors.ashFaint
                )
            }
        }
    }
}
