package com.optimusprime.orbit.engine

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class VoiceCommand { START, PAUSE, STATUS, LOG_QUESTION, UNKNOWN }

/**
 * Keyword matching on purpose — no network, no API key, fully explainable.
 *
 * Rule ordering matters: "stop" is listed under PAUSE, "start"/"resume"
 * under START, "question" under LOG_QUESTION, everything time-related under
 * STATUS. This avoids the "stop studying" ambiguity — "stop" maps to PAUSE,
 * not to an unknown command that confuses the user.
 */
fun parseVoiceCommand(heard: String): VoiceCommand {
    val text = heard.lowercase(Locale.getDefault())
    return when {
        "pause" in text || "stop" in text                                             -> VoiceCommand.PAUSE
        "resume" in text || "start" in text || "continue" in text || "go" in text    -> VoiceCommand.START
        "question" in text || "solved" in text || "done" in text                     -> VoiceCommand.LOG_QUESTION
        "how" in text || "status" in text || "progress" in text || "time" in text ||
                "long" in text || "much" in text                                              -> VoiceCommand.STATUS
        else                                                                          -> VoiceCommand.UNKNOWN
    }
}

/**
 * Thin wrapper around the two platform voice APIs.
 *
 * v2 — the old version dropped commands on the floor in ways that read as
 * "voice detection just doesn't work sometimes":
 *
 *  1. [speak] called before [TextToSpeech]'s async init callback had fired
 *     did nothing at all, silently. The very first "Good job, that's 1 so
 *     far" right after starting a session was a classic time to hit this —
 *     init and the first question log could race. An utterance said before
 *     the engine is ready is now queued and flushed the moment it's ready.
 *  2. [startListening]'s error handling only ever resolved 2 of the ~8
 *     possible [SpeechRecognizer] error codes (no-match, timeout). Every
 *     other error (busy engine, client error, audio error, network) left
 *     [isListening] stuck true with **no callback fired at all** — from
 *     the outside that's indistinguishable from "I tapped the mic and
 *     nothing happened." Every error code now resolves the callback, and
 *     the two genuinely transient ones (engine busy / client) get one
 *     silent retry with a fresh recognizer before giving up.
 *  3. A short delay is inserted between stopping TTS and opening the mic —
 *     stopping playback doesn't release the audio focus instantly, so the
 *     recognizer could occasionally catch the tail end of the app's own
 *     voice and mis-hear it as the command.
 *
 * [isSpeaking] is new — lets the UI (or future logic) know when the TTS
 * engine is actually mid-utterance, not just when the mic isn't listening.
 */
class VoiceCoordinator(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var recognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private var ttsReady = false
    private var pendingUtterance: String? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _lastHeard = MutableStateFlow<String?>(null)
    val lastHeard: StateFlow<String?> = _lastHeard.asStateFlow()

    /** Debounce: timestamp of the last startListening call. */
    private var lastListenRequestMs = 0L
    private val DEBOUNCE_MS = 800L

    /** How long to wait after tts.stop() before opening the mic. */
    private val TTS_RELEASE_DELAY_MS = 150L

    private var hasRetriedThisRequest = false

    fun initTts() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(Locale.ENGLISH)
                }
                tts?.setSpeechRate(0.92f)
                tts?.setPitch(1.0f)
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }
                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                    }
                })
                ttsReady = true
                pendingUtterance?.let { queued ->
                    pendingUtterance = null
                    speakNow(queued)
                }
            }
        }
    }

    /**
     * Speaks [text] immediately if the engine is ready, otherwise queues it
     * (keeping only the most recent request) to be spoken the moment
     * [initTts]'s callback lands.
     */
    fun speak(text: String) {
        if (ttsReady) {
            speakNow(text)
        } else {
            pendingUtterance = text
        }
    }

    private fun speakNow(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "orbit_utterance")
    }

    fun startListening(onCommand: (VoiceCommand, String) -> Unit) {
        // Debounce — ignore taps that come too quickly after the previous one.
        val now = System.currentTimeMillis()
        if (now - lastListenRequestMs < DEBOUNCE_MS) return
        lastListenRequestMs = now
        hasRetriedThisRequest = false

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onCommand(VoiceCommand.UNKNOWN, "")
            return
        }

        // Stop TTS so the mic doesn't hear the synthesised voice.
        tts?.stop()
        _isSpeaking.value = false

        // Give the audio focus a moment to actually release before opening
        // the mic, so the recognizer can't catch the tail of our own TTS.
        mainHandler.postDelayed({ beginListening(onCommand) }, TTS_RELEASE_DELAY_MS)
    }

    private fun beginListening(onCommand: (VoiceCommand, String) -> Unit) {
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _isListening.value = true
                }

                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit

                override fun onEndOfSpeech() {
                    _isListening.value = false
                }

                override fun onError(error: Int) {
                    _isListening.value = false

                    // Engine-busy / client errors are usually transient — most
                    // often a previous recognizer session that hadn't fully
                    // torn down yet. Worth exactly one silent retry with a
                    // fresh recognizer before surfacing it as unknown.
                    val isTransient = error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY ||
                            error == SpeechRecognizer.ERROR_CLIENT
                    if (isTransient && !hasRetriedThisRequest) {
                        hasRetriedThisRequest = true
                        mainHandler.postDelayed({ beginListening(onCommand) }, 250L)
                        return
                    }

                    // Every other error previously fell through with no callback
                    // at all — from the outside that looked like a dead mic
                    // button. Always resolve the callback now.
                    onCommand(VoiceCommand.UNKNOWN, "")
                }

                override fun onResults(results: Bundle?) {
                    _isListening.value = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    // Try all alternatives — use the first one that maps to a known command.
                    val bestMatch = matches?.firstOrNull { parseVoiceCommand(it) != VoiceCommand.UNKNOWN }
                        ?: matches?.firstOrNull().orEmpty()
                    _lastHeard.value = bestMatch
                    onCommand(parseVoiceCommand(bestMatch), bestMatch)
                }

                override fun onPartialResults(partialResults: Bundle?) = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toString())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            // 2 s of silence → done (vs the ~7 s system default).
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
        }
        recognizer?.startListening(intent)
    }

    fun release() {
        mainHandler.removeCallbacksAndMessages(null)
        tts?.stop()
        tts?.shutdown()
        tts = null
        recognizer?.destroy()
        recognizer = null
    }
}