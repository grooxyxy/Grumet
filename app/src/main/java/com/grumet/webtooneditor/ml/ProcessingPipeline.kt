package com.grumet.webtooneditor.ml

import android.content.Context
import com.grumet.webtooneditor.domain.ApiSettings
import com.grumet.webtooneditor.domain.ProcessStatus
import com.grumet.webtooneditor.domain.WebtoonPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class ProcessingPipeline(private val context: Context) {

    private val bubbleDetector = OnnxBubbleDetector(context)
    private val inpainter = TeleaInpainter(context)
    private val translationEngine = TranslationEngine(context)

    private val _pagesFlow = MutableStateFlow<List<WebtoonPage>>(emptyList())
    val pagesFlow: StateFlow<List<WebtoonPage>> = _pagesFlow.asStateFlow()

    fun setPages(pages: List<WebtoonPage>) {
        _pagesFlow.value = pages
    }

    suspend fun processAllPages(settings: ApiSettings) = withContext(Dispatchers.Default) {
        val currentPages = _pagesFlow.value.toMutableList()

        for (i in currentPages.indices) {
            val page = currentPages[i]
            if (page.status == ProcessStatus.COMPLETED) continue

            page.status = ProcessStatus.PROCESSING
            page.progress = 0.1f
            _pagesFlow.value = currentPages.toList()

            try {
                // Step 1: Detect speech bubbles with ogkalu YOLO model
                val detectedBubbles = bubbleDetector.detectBubbles(page.originalBitmap)
                page.bubbles.clear()
                page.bubbles.addAll(detectedBubbles)
                page.progress = 0.4f
                _pagesFlow.value = currentPages.toList()

                // Step 2: Auto Inpainting text inside speech bubbles with Telea algorithm
                val cleanedBitmap = inpainter.inpaint(page.originalBitmap, page.bubbles)
                page.cleanedBitmap = cleanedBitmap
                page.progress = 0.7f
                _pagesFlow.value = currentPages.toList()

                // Step 3: Auto Translation
                val translatedBubbles = translationEngine.translateBubbles(page.bubbles, settings)
                page.bubbles.clear()
                page.bubbles.addAll(translatedBubbles)
                page.progress = 1.0f
                page.status = ProcessStatus.COMPLETED

            } catch (e: Exception) {
                e.printStackTrace()
                page.status = ProcessStatus.FAILED
                page.errorMessage = e.localizedMessage
            }

            _pagesFlow.value = currentPages.toList()
        }
    }
}
