package com.optimusprime.wick.ui.screens.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.optimusprime.wick.engine.PresenceDetector
import com.optimusprime.wick.ui.components.WickButton
import com.optimusprime.wick.ui.components.WickCard
import com.optimusprime.wick.ui.theme.WickColors
import com.optimusprime.wick.ui.theme.WickType
import kotlinx.coroutines.delay

private const val PREFS_NAME = "wick_prefs"
private const val KEY_ONBOARDED = "onboarded"

fun hasCompletedOnboarding(context: Context): Boolean =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_ONBOARDED, false)

private fun markOnboardingComplete(context: Context) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean(KEY_ONBOARDED, true).apply()
}

private fun hasPermission(context: Context, permission: String): Boolean =
    PackageManager.PERMISSION_GRANTED ==
        androidx.core.content.ContextCompat.checkSelfPermission(context, permission)

@Composable
fun PermissionsScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    var cameraGranted by remember { mutableStateOf(hasPermission(context, Manifest.permission.CAMERA)) }
    var micGranted by remember { mutableStateOf(hasPermission(context, Manifest.permission.RECORD_AUDIO)) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        cameraGranted = results[Manifest.permission.CAMERA] ?: cameraGranted
        micGranted = results[Manifest.permission.RECORD_AUDIO] ?: micGranted
    }

    Column(
        modifier = Modifier.fillMaxSize().background(WickColors.ink).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (!cameraGranted || !micGranted) {
            Text(text = "Before we begin", style = WickType.headline, color = WickColors.bone, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Wick watches for your face with the front camera to know when you've " +
                    "stepped away, and listens for voice commands like \u201Cpause\u201D or \u201Cstatus\u201D. " +
                    "Everything is processed on your phone — no video or audio ever leaves it.",
                style = WickType.body,
                color = WickColors.ash,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(28.dp))
            WickButton(
                text = "Grant access",
                onClick = {
                    launcher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
                },
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            SetupCheck(onContinue = {
                markOnboardingComplete(context)
                onFinished()
            })
        }
    }
}

@Composable
private fun SetupCheck(onContinue: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val detector = remember { PresenceDetector(context) }
    var faceDetected by remember { mutableStateOf(false) }

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    DisposableEffect(Unit) {
        detector.start(lifecycleOwner, previewView)
        onDispose { detector.release() }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(300)
            faceDetected = System.currentTimeMillis() - detector.lastFaceSeenAtMs < 1500
        }
    }

    Text(text = "Quick check", style = WickType.headline, color = WickColors.bone, textAlign = TextAlign.Center)
    Spacer(Modifier.height(8.dp))
    Text(
        text = "Make sure the camera can see your face — this is the only time you'll see the preview.",
        style = WickType.body,
        color = WickColors.ash,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(20.dp))

    WickCard(padding = 6.dp) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(14.dp))
        ) {
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        }
    }

    Spacer(Modifier.height(14.dp))
    Text(
        text = if (faceDetected) "Face detected \u2713" else "No face detected yet",
        style = WickType.bodyStrong,
        color = if (faceDetected) WickColors.moss else WickColors.ashFaint
    )
    Spacer(Modifier.height(24.dp))
    WickButton(text = "Get started", onClick = onContinue, modifier = Modifier.fillMaxWidth())
}
