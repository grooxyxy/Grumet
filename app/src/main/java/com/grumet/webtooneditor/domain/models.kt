package com.grumet.webtooneditor.domain

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import java.util.UUID

enum class Language(val code: String, val displayName: String) {
    KOREAN("ko", "Korean (한국어)"),
    CHINESE("zh", "Chinese (中文)"),
    JAPANESE("ja", "Japanese (日本語)"),
    ENGLISH("en", "English"),
    INDONESIAN("id", "Indonesian (Bahasa Indonesia)")
}

enum class TranslationProvider {
    ML_KIT,
    AI_API
}

data class ApiSettings(
    val provider: TranslationProvider = TranslationProvider.ML_KIT,
    val apiKey: String = "",
    val customEndpoint: String = "https://api.openai.com/v1/chat/completions",
    val modelName: String = "gpt-4o-mini",
    val sourceLanguage: Language = Language.KOREAN,
    val targetLanguage: Language = Language.INDONESIAN
)

data class TextStyleConfig(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Default Style",
    val fontSizeSp: Float = 16f,
    val textColorHex: String = "#000000",
    val fontPath: String? = null, // null for default, or path to custom .ttf
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val strokeWidthPx: Float = 0f,
    val strokeColorHex: String = "#FFFFFF",
    val shadowBlurPx: Float = 0f,
    val shadowOffsetX: Float = 0f,
    val shadowOffsetY: Float = 0f,
    val shadowColorHex: String = "#000000",
    val lineSpacingMultiplier: Float = 1.0f,
    val useGradient: Boolean = false,
    val gradientStartColorHex: String = "#FF0000",
    val gradientEndColorHex: String = "#0000FF"
)

data class TextBubble(
    val id: String = UUID.randomUUID().toString(),
    var x: Float, // normalize 0.0 - 1.0
    var y: Float, // normalize 0.0 - 1.0
    var width: Float, // normalize 0.0 - 1.0
    var height: Float, // normalize 0.0 - 1.0
    var originalText: String = "",
    var translatedText: String = "",
    var style: TextStyleConfig = TextStyleConfig()
)

enum class ProcessStatus {
    QUEUED,
    PROCESSING,
    COMPLETED,
    FAILED
}

data class WebtoonPage(
    val id: String = UUID.randomUUID().toString(),
    val originalBitmap: Bitmap,
    var cleanedBitmap: Bitmap? = null,
    val bubbles: MutableList<TextBubble> = mutableListOf(),
    var status: ProcessStatus = ProcessStatus.QUEUED,
    var errorMessage: String? = null,
    var progress: Float = 0f
)
