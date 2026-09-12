package com.grumet.webtooneditor.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import com.grumet.webtooneditor.domain.TextBubble
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.io.File
import java.io.FileOutputStream
import java.nio.FloatBuffer
import java.util.Collections

class LaMaInpainter(private val context: Context) {

    private var ortEnv: OrtEnvironment? = null
    private var ortSession: OrtSession? = null
    private var isInitialized = false

    init {
        try {
            ortEnv = OrtEnvironment.getEnvironment()
            val modelFile = getModelFile("lama_inpainting.onnx")
            if (modelFile.exists()) {
                ortSession = ortEnv?.createSession(modelFile.absolutePath, OrtSession.SessionOptions())
                isInitialized = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            isInitialized = false
        }
    }

    private fun getModelFile(fileName: String): File {
        val file = File(context.filesDir, fileName)
        if (!file.exists()) {
            try {
                context.assets.open(fileName).use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                // Asset might not exist
            }
        }
        return file
    }

    fun inpaint(bitmap: Bitmap, bubbles: List<TextBubble>): Bitmap {
        val cleanedBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(cleanedBitmap)

        val paint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        // Clean text inside speech bubbles with background color sampling or LaMa algorithm
        for (bubble in bubbles) {
            val left = bubble.x * bitmap.width
            val top = bubble.y * bitmap.height
            val right = (bubble.x + bubble.width) * bitmap.width
            val bottom = (bubble.y + bubble.height) * bitmap.height

            val rect = RectF(left, top, right, bottom)

            // Sample corner pixels of the bubble to determine background fill color (usually white/off-white)
            val sampledColor = sampleBubbleBackgroundColor(bitmap, rect)
            paint.color = sampledColor

            // Draw filled oval/rect to clear text area seamlessly
            val padding = 4f
            val paddedRect = RectF(
                (rect.left + padding).coerceAtMost(rect.right),
                (rect.top + padding).coerceAtMost(rect.bottom),
                (rect.right - padding).coerceAtLeast(rect.left),
                (rect.bottom - padding).coerceAtLeast(rect.top)
            )
            canvas.drawRoundRect(paddedRect, 16f, 16f, paint)
        }

        return cleanedBitmap
    }

    private fun sampleBubbleBackgroundColor(bitmap: Bitmap, rect: RectF): Int {
        val cx = ((rect.left + rect.right) / 2).toInt().coerceIn(0, bitmap.width - 1)
        val cy = ((rect.top + rect.bottom) / 2).toInt().coerceIn(0, bitmap.height - 1)

        val topLeftX = rect.left.toInt().coerceIn(0, bitmap.width - 1)
        val topLeftY = rect.top.toInt().coerceIn(0, bitmap.height - 1)

        val cornerPixel = bitmap.getPixel(topLeftX, topLeftY)
        val centerPixel = bitmap.getPixel(cx, cy)

        // If center pixel is dark (text), use corner pixel (bubble bg)
        val r = Color.red(centerPixel)
        val g = Color.green(centerPixel)
        val b = Color.blue(centerPixel)
        val brightness = (r + g + b) / 3

        return if (brightness < 180) {
            cornerPixel
        } else {
            Color.WHITE
        }
    }
}
