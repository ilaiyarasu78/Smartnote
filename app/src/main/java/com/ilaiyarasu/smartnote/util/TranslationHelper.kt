package com.ilaiyarasu.smartnote.util

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class AppLanguage(
    val code: String,       // e.g. "ta" - used as key/UI value
    val label: String,      // e.g. "Tamil" - shown in dropdown
    val mlKitCode: String,  // e.g. TranslateLanguage.TAMIL
    val ttsTag: String      // e.g. "ta-IN" - used by TextToSpeechHelper.speak()
)

val supportedAppLanguages = listOf(
    AppLanguage("en", "English", TranslateLanguage.ENGLISH, "en-US"),
    AppLanguage("ta", "Tamil", TranslateLanguage.TAMIL, "ta-IN"),
    AppLanguage("hi", "Hindi", TranslateLanguage.HINDI, "hi-IN"),
    AppLanguage("te", "Telugu", TranslateLanguage.TELUGU, "te-IN")
)

object TranslationHelper {

    // Cache translators per language pair, e.g. "en->ta", "ta->en", "hi->te"
    private val translatorCache = mutableMapOf<String, Translator>()

    private fun getTranslator(sourceMlKitCode: String, targetMlKitCode: String): Translator {
        val key = "$sourceMlKitCode->$targetMlKitCode"
        return translatorCache.getOrPut(key) {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(sourceMlKitCode)
                .setTargetLanguage(targetMlKitCode)
                .build()
            Translation.getClient(options)
        }
    }

    suspend fun ensureModelDownloaded(
        sourceMlKitCode: String,
        targetMlKitCode: String
    ): Result<Unit> = suspendCancellableCoroutine { cont ->
        val translator = getTranslator(sourceMlKitCode, targetMlKitCode)
        // Allowing mobile data to fix "not working" if user is not on WiFi
        val conditions = DownloadConditions.Builder().build()

        android.util.Log.d("TranslateDebug", "Checking if model needs download: $sourceMlKitCode->$targetMlKitCode")
        translator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener {
                android.util.Log.d("TranslateDebug", "Model ready: $sourceMlKitCode->$targetMlKitCode")
                if (cont.isActive) cont.resume(Result.success(Unit))
            }
            .addOnFailureListener { e ->
                android.util.Log.e("TranslateDebug", "Model download failed", e)
                if (cont.isActive) cont.resume(Result.failure(e))
            }
    }

    suspend fun translate(
        text: String,
        sourceMlKitCode: String,
        targetMlKitCode: String
    ): Result<String> = suspendCancellableCoroutine { cont ->
        if (sourceMlKitCode == targetMlKitCode) {
            if (cont.isActive) cont.resume(Result.success(text))
            return@suspendCancellableCoroutine
        }
        
        val translator = getTranslator(sourceMlKitCode, targetMlKitCode)
        android.util.Log.d("TranslateDebug", "Starting translation: $sourceMlKitCode->$targetMlKitCode")
        translator.translate(text)
            .addOnSuccessListener { translatedText ->
                android.util.Log.d("TranslateDebug", "Translation succeeded")
                if (cont.isActive) cont.resume(Result.success(translatedText))
            }
            .addOnFailureListener { e ->
                android.util.Log.e("TranslateDebug", "Translation failed", e)
                if (cont.isActive) cont.resume(Result.failure(e))
            }
    }

    fun closeAll() {
        translatorCache.values.forEach { it.close() }
        translatorCache.clear()
    }
}
