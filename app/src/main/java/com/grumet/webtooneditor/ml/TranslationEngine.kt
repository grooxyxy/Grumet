package com.grumet.webtooneditor.ml

import android.content.Context
import android.graphics.Bitmap
import com.grumet.webtooneditor.domain.ApiSettings
import com.grumet.webtooneditor.domain.Language
import com.grumet.webtooneditor.domain.TextBubble
import com.grumet.webtooneditor.domain.TranslationProvider
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

class TranslationEngine(private val context: Context) {

    private val httpClient = HttpClient(Android)

    suspend fun translateBubbles(
        bubbles: List<TextBubble>,
        settings: ApiSettings
    ): List<TextBubble> = withContext(Dispatchers.IO) {
        val updatedBubbles = bubbles.map { it.copy() }

        for (bubble in updatedBubbles) {
            val originalText = bubble.originalText.ifBlank {
                // Mock text detection fallback if OCR text empty
                "안녕하세요! 웹툰 translation test."
            }
            bubble.originalText = originalText

            val translatedText = if (settings.provider == TranslationProvider.ML_KIT) {
                translateWithMlKit(originalText, settings.sourceLanguage, settings.targetLanguage)
            } else {
                translateWithAiApi(originalText, settings)
            }
            bubble.translatedText = translatedText
        }

        updatedBubbles
    }

    private suspend fun translateWithMlKit(
        text: String,
        source: Language,
        target: Language
    ): String = withContext(Dispatchers.IO) {
        try {
            val sourceCode = mapLanguageToMlKit(source)
            val targetCode = mapLanguageToMlKit(target)

            val options = TranslatorOptions.Builder()
                .setSourceLanguage(sourceCode)
                .setTargetLanguage(targetCode)
                .build()

            val translator = Translation.getClient(options)
            translator.downloadModelIfNeeded().await()
            val result = translator.translate(text).await()
            translator.close()
            result
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback translation preview if model download delayed
            "[MLKit]: ${text} -> (Terjemahan ${target.displayName})"
        }
    }

    private suspend fun translateWithAiApi(
        text: String,
        settings: ApiSettings
    ): String = withContext(Dispatchers.IO) {
        if (settings.apiKey.isBlank()) {
            return@withContext "[API Key required]: $text"
        }
        try {
            val prompt = "Translate the following webtoon dialog from ${settings.sourceLanguage.displayName} to ${settings.targetLanguage.displayName}. Output ONLY the translated text without extra formatting:\n\n$text"

            val jsonPayload = JsonObject().apply {
                addProperty("model", settings.modelName)
                add("messages", JsonParser.parseString("""
                    [
                      {"role": "system", "content": "You are a professional comic and webtoon translator."},
                      {"role": "user", "content": ${JsonObject().apply { addProperty("t", prompt) }.get("t")} }
                    ]
                """))
                addProperty("temperature", 0.3)
            }

            val response: HttpResponse = httpClient.post(settings.customEndpoint) {
                header(HttpHeaders.Authorization, "Bearer ${settings.apiKey}")
                header(HttpHeaders.ContentType, "application/json")
                setBody(jsonPayload.toString())
            }

            val responseBody = response.bodyAsText()
            val parsedJson = JsonParser.parseString(responseBody).asJsonObject
            val choices = parsedJson.getAsJsonArray("choices")
            if (choices != null && choices.size() > 0) {
                choices.get(0).asJsonObject
                    .getAsJsonObject("message")
                    .get("content").asString.trim()
            } else {
                "[API Error]: Unable to parse response"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "[API Exception]: ${e.localizedMessage}"
        }
    }

    private fun mapLanguageToMlKit(lang: Language): String {
        return when (lang) {
            Language.KOREAN -> TranslateLanguage.KOREAN
            Language.CHINESE -> TranslateLanguage.CHINESE
            Language.JAPANESE -> TranslateLanguage.JAPANESE
            Language.ENGLISH -> TranslateLanguage.ENGLISH
            Language.INDONESIAN -> TranslateLanguage.INDONESIAN
        }
    }
}
