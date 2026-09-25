package com.optimusprime.orbit.ui.screens.session

import android.app.Application
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.camera.view.PreviewView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.optimusprime.orbit.MainActivity
import com.optimusprime.orbit.R
import com.optimusprime.orbit.OrbitApplication
import com.optimusprime.orbit.data.db.SessionEntity
import com.optimusprime.orbit.engine.PresenceDetector
import com.optimusprime.orbit.engine.SessionPhase
import com.optimusprime.orbit.engine.StudyClock
import com.optimusprime.orbit.engine.StudySessionService
import com.optimusprime.orbit.engine.VoiceCommand
import com.optimusprime.orbit.engine.VoiceCoordinator
import com.optimusprime.orbit.util.formatDurationWords
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

data class SessionUiState(
    val phase: SessionPhase = SessionPhase.IDLE,
    val elapsedMillis: Long = 0L,
    val questionsSolved: Int = 0,
    val isPresent: Boolean = false,
    val isListening: Boolean = false,
    val lastVoiceHeard: String? = null,
    val distractionCount: Int = 0,
    /** Raw, unsmoothed per-frame reading — for the in-session debug camera panel only. */
    val rawFaceDetected: Boolean = false
)

private const val ABSENCE_THRESHOLD_MS = 8_000L
private const val RETURN_DETECT_MS     = 3_000L
/** After tapping Resume, ignore absence signals for this long. */
private const val RESUME_GRACE_MS      = 6_000L
/** Checkpoint-save interval — protects against process kills. */
private const val CHECKPOINT_INTERVAL_MS = 60_000L

private const val AWAY_NOTIFICATION_ID = 1002
private const val NOTIFICATION_CHANNEL = "orbit_session"

class SessionViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as OrbitApplication).container.repository

    val presenceDetector = PresenceDetector(application)
    private val voice = VoiceCoordinator(application)
    private val clock = StudyClock()

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    private var tickerJob: Job? = null
    private var hasPromptedReturn    = false
    private var questionsSolvedThisSession = 0
    private var sessionStartWallClock      = 0L
    private var resumedAtMs                = 0L   // time of last manual resume → grace window
    private var lastCheckpointMs           = 0L

    init {
        voice.initTts()
        tickerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                tick()
            }
        }
        viewModelScope.launch {
            voice.isListening.collect { listening ->
                _uiState.update { it.copy(isListening = listening) }
            }
        }
        viewModelScope.launch {
            voice.lastHeard.collect { heard ->
                _uiState.update { it.copy(lastVoiceHeard = heard) }
            }
        }
        // Raw per-frame reading for the debug camera panel — separate from
        // the smoothed/confirmed presence value tick() computes below.
        viewModelScope.launch {
            presenceDetector.liveFaceDetected.collect { detected ->
                _uiState.update { it.copy(rawFaceDetected = detected) }
            }
        }
    }

    // ── Camera ──────────────────────────────────────────────────────────────

    /** [previewView] is optional — pass one only while the debug camera panel is expanded. */
    fun bindCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView? = null) {
        presenceDetector.start(lifecycleOwner, previewView)
    }

    fun unbindCamera() {
        presenceDetector.stop()
    }

    // ── Tick ─────────────────────────────────────────────────────────────────

    private fun tick() {
        val now = System.currentTimeMillis()

        // A face counts only if the camera has fired at least once (lastFaceSeenAtMs > 0).
        val lastSeen      = presenceDetector.lastFaceSeenAtMs
        val hasFiredOnce  = lastSeen > 0L
        val sinceLastSeen = if (hasFiredOnce) now - lastSeen else Long.MAX_VALUE

        val inGracePeriod = resumedAtMs > 0L && (now - resumedAtMs) < RESUME_GRACE_MS

        when (clock.phase) {
            SessionPhase.RUNNING -> {
                // Don't auto-pause if: camera hasn't fired yet, OR we're in the
                // post-resume grace window, OR the user is clearly present.
                if (!inGracePeriod && hasFiredOnce && sinceLastSeen > ABSENCE_THRESHOLD_MS) {
                    clock.pauseForAbsence(now)
                    hasPromptedReturn = false
                    sendAwayNotification()
                }
            }
            SessionPhase.PAUSED_AWAY -> {
                if (hasFiredOnce && sinceLastSeen < RETURN_DETECT_MS) {
                    if (!hasPromptedReturn) {
                        hasPromptedReturn = true
                        voice.speak("Welcome back. Tap Resume when you're ready.")
                    }
                } else {
                    hasPromptedReturn = false
                }
            }
            else -> Unit
        }

        // Checkpoint-save every minute so data survives a process kill.
        if (clock.phase == SessionPhase.RUNNING &&
            sessionStartWallClock > 0L &&
            now - lastCheckpointMs >= CHECKPOINT_INTERVAL_MS
        ) {
            lastCheckpointMs = now
            saveCheckpoint(now)
        }

        _uiState.update {
            it.copy(
                phase         = clock.phase,
                elapsedMillis = clock.elapsedMillis(now),
                isPresent     = hasFiredOnce && sinceLastSeen < RETURN_DETECT_MS,
                distractionCount = clock.distractionCount
            )
        }
    }

    // ── Session controls ─────────────────────────────────────────────────────

    fun startSession() {
        val now = System.currentTimeMillis()
        sessionStartWallClock = now
        questionsSolvedThisSession = 0
        hasPromptedReturn = false
        resumedAtMs = now  // grace from the very start
        lastCheckpointMs = now
        clock.start(now)
        _uiState.update { SessionUiState(phase = clock.phase, isPresent = false) }
        StudySessionService.startSession(getApplication())
    }

    fun pauseManual() {
        clock.pauseManual(System.currentTimeMillis())
        _uiState.update { it.copy(phase = clock.phase) }
    }

    fun resume() {
        val now = System.currentTimeMillis()
        resumedAtMs = now                    // grant grace period
        presenceDetector.resetLastSeen()     // camera gets a fresh start
        clock.resume(now)
        _uiState.update { it.copy(phase = clock.phase) }
        cancelAwayNotification()
    }

    fun endSession() {
        val now = System.currentTimeMillis()
        val active = clock.finish(now)
        doSave(now, active)

        clock.reset()
        presenceDetector.stop()
        _uiState.value = SessionUiState()
        StudySessionService.stopSession(getApplication())
        cancelAwayNotification()
    }

    fun logQuestionSolved() {
        questionsSolvedThisSession++
        _uiState.update { it.copy(questionsSolved = questionsSolvedThisSession) }
        voice.speak("Good job. That's $questionsSolvedThisSession so far.")
    }

    // ── Voice ────────────────────────────────────────────────────────────────

    fun onMicTap() {
        voice.startListening { command, _ ->
            when (command) {
                VoiceCommand.START -> {
                    if (clock.phase == SessionPhase.IDLE) startSession() else resume()
                }
                VoiceCommand.PAUSE -> {
                    if (clock.phase == SessionPhase.RUNNING) pauseManual()
                }
                VoiceCommand.LOG_QUESTION -> logQuestionSolved()
                VoiceCommand.STATUS -> {
                    val words = formatDurationWords(clock.elapsedMillis(System.currentTimeMillis()))
                    voice.speak("You've studied for $words, with $questionsSolvedThisSession questions solved.")
                }
                VoiceCommand.UNKNOWN ->
                    voice.speak("Didn't catch that. Try saying start, pause, or status.")
            }
        }
    }

    // ── Persistence ──────────────────────────────────────────────────────────

    private fun saveCheckpoint(now: Long) {
        val active = clock.elapsedMillis(now)
        if (active > 0L) doSave(now, active)
    }

    private fun doSave(endMs: Long, activeMs: Long) {
        if (activeMs <= 0L || sessionStartWallClock <= 0L) return
        val startDate = Instant.ofEpochMilli(sessionStartWallClock)
            .atZone(ZoneId.systemDefault()).toLocalDate()
        viewModelScope.launch {
            repository.saveSession(
                SessionEntity(
                    startEpochMillis     = sessionStartWallClock,
                    endEpochMillis       = endMs,
                    activeDurationMillis = activeMs,
                    dateEpochDay         = startDate.toEpochDay(),
                    questionsSolved      = questionsSolvedThisSession,
                    distractionCount     = clock.distractionCount
                )
            )
        }
    }

    // ── Notifications ────────────────────────────────────────────────────────

    private fun sendAwayNotification() {
        val ctx = getApplication<Application>()
        val tapIntent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val tapPending = PendingIntent.getActivity(
            ctx, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(ctx, NOTIFICATION_CHANNEL)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Study paused")
            .setContentText("Orbit paused your session — tap to return.")
            .setContentIntent(tapPending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(AWAY_NOTIFICATION_ID, notification)
    }

    private fun cancelAwayNotification() {
        val nm = getApplication<Application>()
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(AWAY_NOTIFICATION_ID)
    }

    // ── Lifecycle ────────────────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        tickerJob?.cancel()
        // Auto-save any in-progress session so data isn't lost on navigation.
        val now = System.currentTimeMillis()
        val active = clock.elapsedMillis(now)
        if (active > 0L && sessionStartWallClock > 0L) {
            doSave(now, active)
        }
        StudySessionService.stopSession(getApplication())
        presenceDetector.release()
        voice.release()
    }
}