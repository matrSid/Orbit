package com.optimusprime.wick.engine

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
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Answers one question every ~200 ms: "is a forward-facing person in front
 * of the front camera right now?"
 *
 * Key design choices vs the original:
 *  - [lastFaceSeenAtMs] starts at 0 (not currentTimeMillis) so the app
 *    never assumes a face is present before the camera has fired even once.
 *  - Frames are throttled to ~5 FPS so ML Kit inference doesn't saturate
 *    a background thread for a use-case that only needs "is someone there?"
 *  - Faces are validated by head orientation — a face turned >45° yaw or
 *    >40° pitch is ignored so a photo on the desk or a far-away person
 *    doesn't keep the session running.
 *  - Tracking is enabled for more stable detection across frames.
 *  - [stop] resets lastFaceSeenAtMs so the next session starts clean.
 */
class PresenceDetector(private val context: Context) {

    /** 0 means "never seen". Callers must check `> 0` before trusting the value. */
    @Volatile
    var lastFaceSeenAtMs: Long = 0L
        private set

    private var cameraProvider: ProcessCameraProvider? = null
    private val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    /** Throttle: only run ML Kit at ~5 FPS (every 200 ms). */
    @Volatile
    private var lastAnalysisMs: Long = 0L

    private val detector: FaceDetector by lazy {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            // No landmarks or smile/blink — we only need presence + orientation.
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            // Tracking gives us stable IDs across frames — reduces false "face gone"
            // when ML Kit momentarily misses a detection.
            .enableTracking()
            .build()
        FaceDetection.getClient(options)
    }

    /**
     * Binds the camera to [lifecycleOwner]. Pass a [previewView] only when the
     * UI wants to show what the sensor sees (the setup-check screen) — during
     * a real study session no preview is bound, only the analysis pipeline.
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
                if (validFace) {
                    lastFaceSeenAtMs = System.currentTimeMillis()
                }
            }
            .addOnCompleteListener {
                proxy.close()
            }
    }

    fun stop() {
        cameraProvider?.unbindAll()
        // Reset so the next session doesn't inherit stale "face was here" state.
        lastFaceSeenAtMs = 0L
        lastAnalysisMs = 0L
    }

    /** Exposed so SessionViewModel can grant a grace period after manual resume. */
    fun resetLastSeen() {
        lastFaceSeenAtMs = 0L
    }

    fun release() {
        stop()
        analysisExecutor.shutdown()
        detector.close()
    }
}
