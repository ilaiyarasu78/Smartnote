package com.ilaiyarasu.smartnote.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class VoiceLanguage(val code: String, val label: String)

val supportedVoiceLanguages = listOf(
    VoiceLanguage("en-US", "English"),
    VoiceLanguage("hi-IN", "Hindi"),
    VoiceLanguage("ta-IN", "Tamil"),
    VoiceLanguage("te-IN", "Telugu"),
    VoiceLanguage("kn-IN", "Kannada"),
    VoiceLanguage("ml-IN", "Malayalam"),
    VoiceLanguage("es-ES", "Spanish"),
    VoiceLanguage("fr-FR", "French")
)

class SpeechRecognizerHelper(private val context: Context) {

    private var recognizer: SpeechRecognizer? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    // 0f (silent) to ~10f (loud). Used to detect "speaking too quietly".
    private val _rmsLevel = MutableStateFlow(0f)
    val rmsLevel: StateFlow<Float> = _rmsLevel.asStateFlow()

    private val _isTooQuiet = MutableStateFlow(false)
    val isTooQuiet: StateFlow<Boolean> = _isTooQuiet.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Threshold below which we consider speech "too quiet". Tune as needed.
    private val quietThreshold = 2.5f
    private var quietFrameCount = 0
    private val quietFrameLimit = 15 // consecutive quiet frames before warning

    fun startListening(languageCode: String, onFinalResult: (String) -> Unit) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _error.value = "Speech recognition not available on this device"
            return
        }

        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _isListening.value = true
                    _error.value = null
                    quietFrameCount = 0
                }

                override fun onRmsChanged(rmsdB: Float) {
                    _rmsLevel.value = rmsdB
                    if (rmsdB < quietThreshold) {
                        quietFrameCount++
                        if (quietFrameCount >= quietFrameLimit) {
                            _isTooQuiet.value = true
                        }
                    } else {
                        quietFrameCount = 0
                        _isTooQuiet.value = false
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    matches?.firstOrNull()?.let { _partialText.value = it }
                }

                override fun onResults(results: Bundle?) {
                    val matches = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull().orEmpty()
                    _isListening.value = false
                    _isTooQuiet.value = false
                    if (text.isNotBlank()) onFinalResult(text)
                }

                override fun onError(errorCode: Int) {
                    _isListening.value = false
                    _isTooQuiet.value = false
                    android.util.Log.d("VoiceDebug", "SpeechRecognizer error code: $errorCode")
                    _error.value = when (errorCode) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "Didn't catch that, try again"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission needed"
                        SpeechRecognizer.ERROR_NETWORK -> "Network error — check your internet connection"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout — check your connection"
                        SpeechRecognizer.ERROR_SERVER -> "Speech server error, try again"
                        SpeechRecognizer.ERROR_CLIENT -> "Voice input error, try again"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Voice input busy, wait a moment"
                        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> "Language not supported on this device"
                        SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "Voice input for this language needs to be downloaded"
                        else -> "Voice input error ($errorCode), try again"
                    }
                }

                override fun onBeginningOfSpeech() {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { _isListening.value = false }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageCode)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            // Longer silence tolerance helps users who speak softly/slowly
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1500)
        }

        recognizer?.startListening(intent)
    }

    fun stopListening() {
        recognizer?.stopListening()
    }

    fun destroy() {
        recognizer?.destroy()
        recognizer = null
    }
}