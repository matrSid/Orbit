package com.optimusprime.orbit.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.optimusprime.orbit.ui.theme.OrbitColors
import androidx.compose.runtime.getValue

enum class FlamePose { LIT, RESTING, AWAY }

// Coordinates lifted straight from the app icon's flame mark (a 108-unit
// box); [unit] remaps that box onto whatever pixel size this gets drawn at.
private fun outerFlamePath(unit: Float): Path = Path().apply {
    moveTo(54f * unit, 26f * unit)
    cubicTo(62f * unit, 34f * unit, 68f * unit, 42f * unit, 70f * unit, 56f * unit)
    cubicTo(71f * unit, 66f * unit, 66f * unit, 76f * unit, 60f * unit, 80f * unit)
    cubicTo(58f * unit, 81.5f * unit, 56f * unit, 82f * unit, 54f * unit, 82f * unit)
    cubicTo(52f * unit, 82f * unit, 50f * unit, 81.5f * unit, 48f * unit, 80f * unit)
    cubicTo(42f * unit, 76f * unit, 37f * unit, 66f * unit, 38f * unit, 56f * unit)
    cubicTo(40f * unit, 42f * unit, 46f * unit, 34f * unit, 54f * unit, 26f * unit)
    close()
}

private fun innerFlamePath(unit: Float): Path = Path().apply {
    moveTo(54f * unit, 40f * unit)
    cubicTo(59f * unit, 48f * unit, 62f * unit, 54f * unit, 61f * unit, 62f * unit)
    cubicTo(60.5f * unit, 68f * unit, 57f * unit, 72f * unit, 54f * unit, 72f * unit)
    cubicTo(51f * unit, 72f * unit, 47.5f * unit, 68f * unit, 47f * unit, 62f * unit)
    cubicTo(46f * unit, 54f * unit, 49f * unit, 48f * unit, 54f * unit, 40f * unit)
    close()
}

/**
 * The same orbit mark as the app icon, reused as a living status light: it
 * flickers gently while the camera can see the student, sits still and
 * dim on a manual break, and turns the colour of a snuffed-out candle once
 * the student has stepped away.
 */
@Composable
fun FlameIndicator(pose: FlamePose, modifier: Modifier = Modifier, size: Dp = 28.dp) {
    val infinite = rememberInfiniteTransition(label = "flame")
    val flicker by infinite.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flicker"
    )

    val outerColor: Color
    val innerColor: Color
    val breathe: Float
    when (pose) {
        FlamePose.LIT -> {
            outerColor = OrbitColors.brass
            innerColor = OrbitColors.bone
            breathe = flicker
        }
        FlamePose.RESTING -> {
            outerColor = OrbitColors.ashFaint
            innerColor = OrbitColors.ash
            breathe = 1f
        }
        FlamePose.AWAY -> {
            outerColor = OrbitColors.oxide
            innerColor = OrbitColors.oxideSoft
            breathe = 1f
        }
    }

    Canvas(modifier = modifier.size(size).scale(breathe)) {
        val unit = this.size.minDimension / 108f
        drawPath(outerFlamePath(unit), color = outerColor)
        drawPath(innerFlamePath(unit), color = innerColor)
    }
}
