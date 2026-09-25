package com.optimusprime.wick.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.optimusprime.wick.ui.theme.WickColors
import com.optimusprime.wick.ui.theme.WickType

/**
 * One goal or homework line: a hand-drawn checkmark rather than a Material
 * checkbox, a strikethrough on completion, and a plain "x" to remove it —
 * no trash-can icon font needed for a single glyph.
 */
@Composable
fun GoalRow(
    title: String,
    typeLabel: String,
    isDone: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CheckCircle(
            isDone = isDone,
            onToggle = onToggle
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = if (isDone) {
                    WickType.body.copy(textDecoration = TextDecoration.LineThrough)
                } else {
                    WickType.body
                },
                color = if (isDone) WickColors.ashFaint else WickColors.bone
            )
            Text(text = typeLabel, style = WickType.caption, color = WickColors.ashFaint)
        }
        Text(
            text = "\u00D7",
            style = WickType.title,
            color = WickColors.ashFaint,
            modifier = Modifier
                .clickable(onClick = onDelete)
                .padding(8.dp)
        )
    }
}

@Composable
private fun CheckCircle(isDone: Boolean, onToggle: () -> Unit) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(if (isDone) WickColors.moss else Color.Transparent)
            .border(1.5.dp, if (isDone) WickColors.moss else WickColors.hairline, CircleShape)
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center
    ) {
        if (isDone) {
            Canvas(modifier = Modifier.size(12.dp)) {
                val path = Path().apply {
                    moveTo(size.width * 0.06f, size.height * 0.55f)
                    lineTo(size.width * 0.4f, size.height * 0.86f)
                    lineTo(size.width * 0.94f, size.height * 0.16f)
                }
                drawPath(
                    path,
                    color = WickColors.ink,
                    style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }
    }
}
