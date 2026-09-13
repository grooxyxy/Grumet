package com.grumet.webtooneditor

import com.grumet.webtooneditor.domain.ApiSettings
import com.grumet.webtooneditor.domain.Language
import com.grumet.webtooneditor.domain.TextBubble
import com.grumet.webtooneditor.domain.TextStyleConfig
import com.grumet.webtooneditor.domain.TranslationProvider
import org.junit.Assert.*
import org.junit.Test

class DomainModelTest {

    @Test
    fun testDefaultTextStyleConfig() {
        val style = TextStyleConfig()
        assertEquals("Default Style", style.name)
        assertEquals(16f, style.fontSizeSp, 0.01f)
        assertEquals("#000000", style.textColorHex)
        assertFalse(style.isBold)
        assertFalse(style.isItalic)
    }

    @Test
    fun testTextBubbleInitialization() {
        val bubble = TextBubble(x = 0.1f, y = 0.2f, width = 0.5f, height = 0.3f, originalText = "Hello")
        assertEquals(0.1f, bubble.x, 0.001f)
        assertEquals(0.2f, bubble.y, 0.001f)
        assertEquals(0.5f, bubble.width, 0.001f)
        assertEquals(0.3f, bubble.height, 0.001f)
        assertEquals("Hello", bubble.originalText)
    }

    @Test
    fun testApiSettingsDefaults() {
        val settings = ApiSettings()
        assertEquals(TranslationProvider.ML_KIT, settings.provider)
        assertEquals(Language.KOREAN, settings.sourceLanguage)
        assertEquals(Language.INDONESIAN, settings.targetLanguage)
    }
}
