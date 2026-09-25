package com.optimusprime.wick.ui.screens.session

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.optimusprime.wick.engine.SessionPhase
import com.optimusprime.wick.ui.components.FlameIndicator
import com.optimusprime.wick.ui.components.FlamePose
import com.optimusprime.wick.ui.components.TimerDial
import com.optimusprime.wick.ui.components.WickButton
import com.optimusprime.wick.ui.components.WickButtonStyle
import com.optimusprime.wick.ui.theme.WickColors
import com.optimusprime.wick.ui.theme.WickType
import com.optimusprime.wick.util.formatClock

@Composable
fun SessionScreen(viewModel: SessionViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraShouldRun = state.phase == SessionPhase.RUNNING || state.phase == SessionPhase.PAUSED_AWAY
    DisposableEffect(cameraShouldRun) {
        if (cameraShouldRun) {
            viewModel.bindCamera(lifecycleOwner)
        } else {
            viewModel.unbindCamera()
        }
        onDispose { }
    }
    DisposableEffect(Unit) {
        onDispose { viewModel.unbindCamera() }
    }

    val pose = when {
        state.phase == SessionPhase.PAUSED_AWAY                    -> FlamePose.AWAY
        state.phase == SessionPhase.RUNNING && state.isPresent     -> FlamePose.LIT
        else                                                        -> FlamePose.RESTING
    }
    val statusLine = when (state.phase) {
        SessionPhase.IDLE         -> "Ready when you are."
        SessionPhase.RUNNING      -> "Studying."
        SessionPhase.PAUSED_MANUAL-> "On a short break."
        SessionPhase.PAUSED_AWAY  -> "Stepped away — paused for you."
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WickColors.ink)
            // windowInsetsPadding(statusBars) instead of a hardcoded Spacer so
            // the heading clears the status bar on every device and notch height.
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(12.dp))
        Text(
            text = statusLine,
            style = WickType.title,
            color = if (state.phase == SessionPhase.PAUSED_AWAY) WickColors.oxide else WickColors.ash,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))

        Box(contentAlignment = Alignment.Center) {
            val hourFraction = (state.elapsedMillis % 3_600_000L) / 3_600_000f
            TimerDial(hourFraction = hourFraction)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FlameIndicator(pose = pose)
                Spacer(Modifier.height(4.dp))
                Text(text = formatClock(state.elapsedMillis), style = WickType.display, color = WickColors.bone)
            }
        }

        Spacer(Modifier.height(28.dp))

        QuestionCounter(count = state.questionsSolved, onAdd = viewModel::logQuestionSolved)

        Spacer(Modifier.height(32.dp))

        when (state.phase) {
            SessionPhase.IDLE -> {
                WickButton(text = "Start studying", onClick = viewModel::startSession, modifier = Modifier.fillMaxWidth())
            }
            SessionPhase.RUNNING -> {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    WickButton(
                        text = "Pause",
                        onClick = viewModel::pauseManual,
                        style = WickButtonStyle.SECONDARY,
                        modifier = Modifier.weight(1f)
                    )
                    WickButton(
                        text = "End session",
                        onClick = viewModel::endSession,
                        style = WickButtonStyle.GHOST,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            SessionPhase.PAUSED_MANUAL, SessionPhase.PAUSED_AWAY -> {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    WickButton(
                        text = "Resume",
                        onClick = viewModel::resume,
                        modifier = Modifier.weight(1f)
                    )
                    WickButton(
                        text = "End session",
                        onClick = viewModel::endSession,
                        style = WickButtonStyle.GHOST,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(Modifier.height(28.dp))
        MicButton(isListening = state.isListening, onTap = viewModel::onMicTap)
        Spacer(Modifier.height(8.dp))
        Text(
            text = state.lastVoiceHeard?.let { "Heard: \u201C$it\u201D" } ?: "Tap the mic and say start, pause, or status.",
            style = WickType.caption,
            color = WickColors.ashFaint,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun QuestionCounter(count: Int, onAdd: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = "Questions solved: ", style = WickType.body, color = WickColors.ash)
        Text(text = count.toString(), style = WickType.bodyStrong, color = WickColors.bone)
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(WickColors.brassSoft)
                .clickable(onClick = onAdd),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "+", style = WickType.bodyStrong, color = WickColors.brass)
        }
    }
}

@Composable
private fun MicButton(isListening: Boolean, onTap: () -> Unit) {
    val infinite = rememberInfiniteTransition(label = "mic")
    val pulse by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(animation = tween(600), repeatMode = RepeatMode.Reverse),
        label = "micPulse"
    )
    val scale = if (isListening) pulse else 1f

    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(if (isListening) WickColors.brass else WickColors.surfaceRaised)
            .clickable(onClick = onTap),
        contentAlignment = Alignment.Center
    ) {
        MicGlyph(
            color = if (isListening) WickColors.ink else WickColors.bone,
            modifier = Modifier.size(22.dp * scale)
        )
    }
}

@Composable
private fun MicGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.32f, 0f),
            size = Size(w * 0.36f, h * 0.55f),
            cornerRadius = CornerRadius(w * 0.18f, w * 0.18f)
        )
        drawArc(
            color = color,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.12f, h * 0.22f),
            size = Size(w * 0.76f, h * 0.55f),
            style = Stroke(width = w * 0.09f, cap = StrokeCap.Round)
        )
        drawLine(
            color = color,
            start = Offset(w * 0.5f, h * 0.78f),
            end = Offset(w * 0.5f, h * 0.95f),
            strokeWidth = w * 0.09f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(w * 0.28f, h * 0.95f),
            end = Offset(w * 0.72f, h * 0.95f),
            strokeWidth = w * 0.09f,
            cap = StrokeCap.Round
        )
    }
}
