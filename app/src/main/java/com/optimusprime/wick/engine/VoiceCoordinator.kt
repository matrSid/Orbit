package com.optimusprime.wick.engine

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
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
 * [startListening] is push-to-talk (tap the mic → one utterance → result).
 *
 * Improvements vs the original:
 *  - TTS is stopped before the mic opens, so the recognition engine doesn't
 *    accidentally pick up the synthesised voice.
 *  - Debounce: a second tap within 800 ms of the first is ignored (prevents
 *    double-fire on accidental double-taps).
 *  - Faster silence timeout: 2 s of silence ends the capture automatically,
 *    rather than waiting for the system default (~7 s).
 *  - `EXTRA_MAX_RESULTS = 3` so the parser has the top alternatives to pick from.
 *  - TTS language is forced to English (US) so words like "Nice" are never
 *    mispronounced.
 */
class VoiceCoordinator(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var recognizer: SpeechRecognizer? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _lastHeard = MutableStateFlow<String?>(null)
    val lastHeard: StateFlow<String?> = _lastHeard.asStateFlow()

    /** Debounce: timestamp of the last startListening call. */
    private var lastListenRequestMs = 0L
    private val DEBOUNCE_MS = 800L

    fun initTts() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(Locale.ENGLISH)
                }
                tts?.setSpeechRate(0.92f)
                tts?.setPitch(1.0f)
            }
        }
    }

    fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "wick_utterance")
    }

    fun startListening(onCommand: (VoiceCommand, String) -> Unit) {
        // Debounce — ignore taps that come too quickly after the previous one.
        val now = System.currentTimeMillis()
        if (now - lastListenRequestMs < DEBOUNCE_MS) return
        lastListenRequestMs = now

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onCommand(VoiceCommand.UNKNOWN, "")
            return
        }

        // Stop TTS so the mic doesn't hear the synthesised voice.
        tts?.stop()

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
                    // On error, give the user a soft hint rather than silence.
                    if (error == SpeechRecognizer.ERROR_NO_MATCH ||
                        error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                        onCommand(VoiceCommand.UNKNOWN, "")
                    }
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
        tts?.stop()
        tts?.shutdown()
        tts = null
        recognizer?.destroy()
        recognizer = null
    }
}
