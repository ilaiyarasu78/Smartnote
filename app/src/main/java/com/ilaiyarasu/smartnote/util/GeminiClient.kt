package com.ilaiyarasu.smartnote.util

import com.ilaiyarasu.smartnote.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object GeminiClient {

    private const val ENDPOINT =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"

    suspend fun analyzeAndExplain(originalText: String): Result<String> {
        val prompt = "Analyze the following note content and provide a very simple, " +
                "easy-to-understand explanation as if teaching a student. " +
                "Keep it concise and clear:\n\n$originalText"
        return callGemini(prompt)
    }

    private suspend fun callGemini(prompt: String): Result<String> = withContext(Dispatchers.IO) {
        val key = BuildConfig.GEMINI_API_KEY
        
        try {
            if (key.isBlank() || key == "YOUR_API_KEY_HERE") {
                return@withContext Result.failure(Exception("API key is missing in local.properties"))
            }

            val requestBody = JSONObject().apply {
                put("contents", JSONArray().put(
                    JSONObject().put("parts", JSONArray().put(
                        JSONObject().put("text", prompt)
                    ))
                ))
            }

            val url = URL(ENDPOINT)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("x-goog-api-key", key)
                doOutput = true
                connectTimeout = 15000
                readTimeout = 20000
            }

            connection.outputStream.use { it.write(requestBody.toString().toByteArray()) }

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                val errorBody = connection.errorStream?.bufferedReader()?.readText()
                return@withContext Result.failure(
                    Exception("Gemini API error ($responseCode): ${errorBody ?: "unknown"}")
                )
            }

            val responseText = connection.inputStream.bufferedReader().readText()
            val json = JSONObject(responseText)
            val resultText = json
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")

            Result.success(resultText.trim())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
