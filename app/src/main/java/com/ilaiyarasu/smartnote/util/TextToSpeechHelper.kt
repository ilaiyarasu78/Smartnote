package com.ilaiyarasu.smartnote.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class TextToSpeechHelper(context: Context) {

    private var tts: TextToSpeech? = null

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        tts = TextToSpeech(context) { status ->
            if (status != TextToSpeech.SUCCESS) {
                _error.value = "Text-to-speech engine failed to initialize"
            }
        }
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) { _isSpeaking.value = true }
            override fun onDone(utteranceId: String?) { _isSpeaking.value = false }
            override fun onError(utteranceId: String?) { _isSpeaking.value = false }
        })
    }

    fun speak(text: String, languageTag: String) {
        val engine = tts ?: return
        val locale = Locale.forLanguageTag(languageTag)
        val result = engine.setLanguage(locale)

        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            _error.value = "This language isn't installed for text-to-speech on this device"
            return
        }

        _error.value = null
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "note_read_aloud")
    }

    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
    }

    fun destroy() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
