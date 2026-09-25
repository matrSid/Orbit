package com.optimusprime.orbit.engine

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Answers one question every ~200 ms: "is a forward-facing person in front
 * of the front camera right now?"
 *
 * v2 — hardened against the "sometimes it just doesn't notice you left"
 * bug. The old version trusted a single frame: one stray detection (a
 * reflection, a poster, an edge caught near the lens) reset the whole
 * 8-second absence timer on its own, and the system quietly kept "seeing"
 * a student who had already left. Three changes fix that:
 *
 *  1. [FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE] instead of FAST — a
 *     slightly heavier but meaningfully more precise model. Affordable
 *     since we only ever feed it ~5 frames/sec.
 *  2. A rolling-majority filter ([recordDetection]): a frame only counts
 *     as "someone is really there" once at least [CONFIRMATION_THRESHOLD]
 *     of the last [CONFIRMATION_WINDOW] analyzed frames agree. A single
 *     one-off false positive can no longer reset the absence timer alone.
 *  3. [FaceDetectorOptions.Builder.setMinFaceSize] filters out small
 *     face-shaped artifacts far from the lens (posters, photos on a wall
 *     behind the student) that were previously large enough to register.
 *
 * Carried over from v1:
 *  - [lastFaceSeenAtMs] starts at 0 (not currentTimeMillis) so the app
 *    never assumes a face is present before the camera has fired even once.
 *  - Frames are throttled to ~5 FPS so ML Kit inference doesn't saturate
 *    a background thread for a use-case that only needs "is someone there?"
 *  - Faces are validated by head orientation — a face turned >45° yaw or
 *    >40° pitch is ignored so a photo on the desk or a far-away person
 *    doesn't keep the session running.
 *  - [stop] resets all detection state so the next session starts clean.
 *
 * [liveFaceDetected] is new: the raw, unfiltered per-frame reading (no
 * smoothing), exposed so a debug camera preview can show exactly what the
 * model sees frame-to-frame — useful for telling "the model genuinely
 * can't see me" apart from "the model saw me but the confirmation filter
 * is (correctly) holding off".
 */
class PresenceDetector(private val context: Context) {

    /** 0 means "never seen". Callers must check `> 0` before trusting the value. */
    @Volatile
    var lastFaceSeenAtMs: Long = 0L
        private set

    private val _liveFaceDetected = MutableStateFlow(false)
    /** Raw, per-analyzed-frame reading — no smoothing. For debug UI only. */
    val liveFaceDetected: StateFlow<Boolean> = _liveFaceDetected.asStateFlow()

    private var cameraProvider: ProcessCameraProvider? = null
    private val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    /** Throttle: only run ML Kit at ~5 FPS (every 200 ms). */
    @Volatile
    private var lastAnalysisMs: Long = 0L

    /** Rolling window of the last few raw per-frame detections, oldest first. */
    private val recentDetections = ArrayDeque<Boolean>()

    private val detector: FaceDetector by lazy {
        val options = FaceDetectorOptions.Builder()
            // ACCURATE trades a little latency for meaningfully better
            // detection — worth it since we only run at ~5 FPS anyway.
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            // No landmarks or smile/blink — we only need presence + orientation.
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            // Ignore small face-shaped artifacts far from the lens (posters,
            // photos on a wall) — a real study-session face fills more of frame.
            .setMinFaceSize(MIN_FACE_SIZE)
            // Tracking gives us stable IDs across frames — reduces false "face gone"
            // when ML Kit momentarily misses a detection.
            .enableTracking()
            .build()
        FaceDetection.getClient(options)
    }

    /**
     * Binds the camera to [lifecycleOwner]. Pass a [previewView] only when the
     * UI wants to show what the sensor sees (the setup-check screen, or the
     * in-session debug preview) — when [previewView] is null only the
     * analysis pipeline is bound, with no visible preview.
     */
    fun start(lifecycleOwner: LifecycleOwner, previewView: PreviewView? = null) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener(
            {
                val provider = providerFuture.get()
                cameraProvider = provider

                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(analysisExecutor) { proxy -> analyzeFrame(proxy) }

                val useCases = mutableListOf<UseCase>(analysis)
                if (previewView != null) {
                    val preview = Preview.Builder().build()
                    preview.setSurfaceProvider(previewView.surfaceProvider)
                    useCases.add(preview)
                }

                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    *useCases.toTypedArray()
                )
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    @OptIn(ExperimentalGetImage::class)
    private fun analyzeFrame(proxy: ImageProxy) {
        // Throttle to ~5 FPS — more than enough to detect "are they there?"
        val now = System.currentTimeMillis()
        if (now - lastAnalysisMs < 200L) {
            proxy.close()
            return
        }
        lastAnalysisMs = now

        val mediaImage = proxy.image
        if (mediaImage == null) {
            proxy.close()
            return
        }
        val image = InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees)
        detector.process(image)
            .addOnSuccessListener { faces ->
                // A face counts only if it's roughly forward-facing.
                // Yaw  (Y axis): left/right head turn  — ignore > ±45°
                // Pitch (X axis): looking up/down       — ignore > ±40°
                val validFace = faces.any { face ->
                    val yaw = face.headEulerAngleY
                    val pitch = face.headEulerAngleX
                    kotlin.math.abs(yaw) < 45f && kotlin.math.abs(pitch) < 40f
                }
                recordDetection(validFace)
            }
            .addOnFailureListener {
                // A failed frame is "no data", not "no face" — don't let a
                // transient ML Kit error masquerade as a confirmed absence.
            }
            .addOnCompleteListener {
                proxy.close()
            }
    }

    /**
     * Folds one more raw reading into the rolling window and only advances
     * [lastFaceSeenAtMs] once a majority of the recent window agrees a face
     * is present. This is the fix for "sometimes it doesn't notice I left":
     * previously a single stray positive frame reset the whole 8-second
     * absence timer on its own.
     */
    @Synchronized
    private fun recordDetection(detected: Boolean) {
        recentDetections.addLast(detected)
        while (recentDetections.size > CONFIRMATION_WINDOW) {
            recentDetections.removeFirst()
        }
        val confirmedPresent = recentDetections.count { it } >= CONFIRMATION_THRESHOLD
        if (confirmedPresent) {
            lastFaceSeenAtMs = System.currentTimeMillis()
        }
        _liveFaceDetected.value = detected
    }

    fun stop() {
        cameraProvider?.unbindAll()
        // Reset so the next session doesn't inherit stale "face was here" state.
        lastFaceSeenAtMs = 0L
        lastAnalysisMs = 0L
        recentDetections.clear()
        _liveFaceDetected.value = false
    }

    /** Exposed so SessionViewModel can grant a grace period after manual resume. */
    fun resetLastSeen() {
        lastFaceSeenAtMs = 0L
        recentDetections.clear()
    }

    fun release() {
        stop()
        analysisExecutor.shutdown()
        detector.close()
    }

    private companion object {
        /** Frames considered when deciding whether a detection is "real". */
        const val CONFIRMATION_WINDOW = 3
        /** How many of [CONFIRMATION_WINDOW] must agree before trusting a positive. */
        const val CONFIRMATION_THRESHOLD = 2
        /** Minimum face size as a fraction of the smaller image dimension. */
        const val MIN_FACE_SIZE = 0.18f
    }
}