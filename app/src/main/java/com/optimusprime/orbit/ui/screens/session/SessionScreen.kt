package com.optimusprime.orbit.ui.screens.session

import androidx.camera.view.PreviewView
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.optimusprime.orbit.engine.SessionPhase
import com.optimusprime.orbit.ui.components.FlameIndicator
import com.optimusprime.orbit.ui.components.FlamePose
import com.optimusprime.orbit.ui.components.TimerDial
import com.optimusprime.orbit.ui.components.OrbitButton
import com.optimusprime.orbit.ui.components.OrbitButtonStyle
import com.optimusprime.orbit.ui.theme.OrbitColors
import com.optimusprime.orbit.ui.theme.OrbitType
import com.optimusprime.orbit.util.formatClock

@Composable
fun SessionScreen(viewModel: SessionViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    // Debug camera preview — off by default so the student doesn't have to
    // stare at their own face; toggled from the small text link below the
    // status line. Always the *same* PreviewView instance so toggling it
    // just shows/hides the composable rather than tearing anything down.
    var debugExpanded by remember { mutableStateOf(false) }
    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }

    val cameraShouldRun = state.phase == SessionPhase.RUNNING || state.phase == SessionPhase.PAUSED_AWAY
    DisposableEffect(cameraShouldRun, debugExpanded) {
        if (cameraShouldRun) {
            viewModel.bindCamera(lifecycleOwner, if (debugExpanded) previewView else null)
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
            .background(OrbitColors.ink)
            // windowInsetsPadding(statusBars) instead of a hardcoded Spacer so
            // the heading clears the status bar on every device and notch height.
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(12.dp))
        Text(
            text = statusLine,
            style = OrbitType.title,
            color = if (state.phase == SessionPhase.PAUSED_AWAY) OrbitColors.oxide else OrbitColors.ash,
            textAlign = TextAlign.Center
        )

        if (cameraShouldRun) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (debugExpanded) "Hide camera debug" else "Show camera debug",
                style = OrbitType.caption,
                color = OrbitColors.brassDim,
                modifier = Modifier
                    .clickable { debugExpanded = !debugExpanded }
                    .padding(4.dp)
            )
            if (debugExpanded) {
                Spacer(Modifier.height(8.dp))
                CameraDebugPanel(
                    previewView = previewView,
                    rawDetected = state.rawFaceDetected,
                    confirmedPresent = state.isPresent
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Box(contentAlignment = Alignment.Center) {
            val hourFraction = (state.elapsedMillis % 3_600_000L) / 3_600_000f
            TimerDial(hourFraction = hourFraction)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FlameIndicator(pose = pose)
                Spacer(Modifier.height(4.dp))
                Text(text = formatClock(state.elapsedMillis), style = OrbitType.display, color = OrbitColors.bone)
            }
        }

        Spacer(Modifier.height(28.dp))

        QuestionCounter(count = state.questionsSolved, onAdd = viewModel::logQuestionSolved)

        Spacer(Modifier.height(32.dp))

        when (state.phase) {
            SessionPhase.IDLE -> {
                OrbitButton(text = "Start studying", onClick = viewModel::startSession, modifier = Modifier.fillMaxWidth())
            }
            SessionPhase.RUNNING -> {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OrbitButton(
                        text = "Pause",
                        onClick = viewModel::pauseManual,
                        style = OrbitButtonStyle.SECONDARY,
                        modifier = Modifier.weight(1f)
                    )
                    OrbitButton(
                        text = "End session",
                        onClick = viewModel::endSession,
                        style = OrbitButtonStyle.GHOST,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            SessionPhase.PAUSED_MANUAL, SessionPhase.PAUSED_AWAY -> {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OrbitButton(
                        text = "Resume",
                        onClick = viewModel::resume,
                        modifier = Modifier.weight(1f)
                    )
                    OrbitButton(
                        text = "End session",
                        onClick = viewModel::endSession,
                        style = OrbitButtonStyle.GHOST,
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
            style = OrbitType.caption,
            color = OrbitColors.ashFaint,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * The live front-camera feed plus two readouts: [rawDetected] (this exact
 * frame, no smoothing) and [confirmedPresent] (the majority-vote value that
 * actually drives the auto-pause timer). Seeing them side by side is the
 * fastest way to tell "the camera genuinely can't see me" apart from "it saw
 * me, the debounce is just doing its job" while debugging detection issues.
 */
@Composable
private fun CameraDebugPanel(
    previewView: PreviewView,
    rawDetected: Boolean,
    confirmedPresent: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .width(130.dp)
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(14.dp))
                .border(
                    width = 2.dp,
                    color = if (confirmedPresent) OrbitColors.moss else OrbitColors.oxide,
                    shape = RoundedCornerShape(14.dp)
                )
        ) {
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "raw: ${if (rawDetected) "face" else "none"} \u00B7 confirmed: ${if (confirmedPresent) "present" else "away"}",
            style = OrbitType.caption,
            color = OrbitColors.ashFaint,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun QuestionCounter(count: Int, onAdd: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = "Questions solved: ", style = OrbitType.body, color = OrbitColors.ash)
        Text(text = count.toString(), style = OrbitType.bodyStrong, color = OrbitColors.bone)
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(OrbitColors.brassSoft)
                .clickable(onClick = onAdd),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "+", style = OrbitType.bodyStrong, color = OrbitColors.brass)
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
            .background(if (isListening) OrbitColors.brass else OrbitColors.surfaceRaised)
            .clickable(onClick = onTap),
        contentAlignment = Alignment.Center
    ) {
        MicGlyph(
            color = if (isListening) OrbitColors.ink else OrbitColors.bone,
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