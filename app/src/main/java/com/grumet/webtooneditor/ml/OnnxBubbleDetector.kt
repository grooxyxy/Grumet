package com.grumet.webtooneditor.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import com.grumet.webtooneditor.domain.TextBubble
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.io.File
import java.io.FileOutputStream
import java.nio.FloatBuffer
import java.util.Collections

/**
 * Bubble detector tailored for HuggingFace `ogkalu/comic-speech-bubble-detector` ONNX model.
 * Model output format: YOLOv8 / Detection tensor shape [1, 5, 8400] (cx, cy, w, h, confidence)
 */
class OnnxBubbleDetector(private val context: Context) {

    private var ortEnv: OrtEnvironment? = null
    private var ortSession: OrtSession? = null
    private var isInitialized = false

    init {
        try {
            ortEnv = OrtEnvironment.getEnvironment()
            val modelFile = getModelFile("ogkalu_bubble_detector.onnx")
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

    fun detectBubbles(bitmap: Bitmap): List<TextBubble> {
        if (!isInitialized || ortSession == null) {
            return runHeuristicBubbleDetection(bitmap)
        }

        return try {
            val inputSize = 640
            val resized = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
            val floatBuffer = FloatBuffer.allocate(1 * 3 * inputSize * inputSize)

            val intValues = IntArray(inputSize * inputSize)
            resized.getPixels(intValues, 0, inputSize, 0, 0, inputSize, inputSize)

            // Normalize CHW [0.0..1.0]
            for (i in 0 until inputSize * inputSize) {
                val pixel = intValues[i]
                floatBuffer.put(((pixel shr 16 and 0xFF) / 255.0f))
            }
            for (i in 0 until inputSize * inputSize) {
                val pixel = intValues[i]
                floatBuffer.put(((pixel shr 8 and 0xFF) / 255.0f))
            }
            for (i in 0 until inputSize * inputSize) {
                val pixel = intValues[i]
                floatBuffer.put(((pixel and 0xFF) / 255.0f))
            }
            floatBuffer.rewind()

            val inputName = ortSession!!.inputNames.iterator().next()
            val tensor = OnnxTensor.createTensor(ortEnv, floatBuffer, longArrayOf(1, 3, inputSize.toLong(), inputSize.toLong()))
            val results = ortSession!!.run(Collections.singletonMap(inputName, tensor))

            val detected = mutableListOf<TextBubble>()
            if (results != null && results.size() > 0) {
                val outputTensor = results.get(0).value as? Array<Array<FloatArray>>
                if (outputTensor != null) {
                    val rawData = outputTensor[0] // [5][8400]
                    val numDetections = rawData[0].size
                    val confThreshold = 0.35f

                    for (i in 0 until numDetections) {
                        val cx = rawData[0][i] / 640f
                        val cy = rawData[1][i] / 640f
                        val w = rawData[2][i] / 640f
                        val h = rawData[3][i] / 640f
                        val conf = rawData[4][i]

                        if (conf >= confThreshold) {
                            val left = (cx - w / 2f).coerceIn(0f, 1f)
                            val top = (cy - h / 2f).coerceIn(0f, 1f)
                            val right = (cx + w / 2f).coerceIn(0f, 1f)
                            val bottom = (cy + h / 2f).coerceIn(0f, 1f)

                            detected.add(
                                TextBubble(
                                    x = left,
                                    y = top,
                                    width = right - left,
                                    height = bottom - top
                                )
                            )
                        }
                    }
                }
            }

            if (detected.isEmpty()) {
                runHeuristicBubbleDetection(bitmap)
            } else {
                mergeOverlappingBubbles(detected)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            runHeuristicBubbleDetection(bitmap)
        }
    }

    private fun runHeuristicBubbleDetection(bitmap: Bitmap): List<TextBubble> {
        val bubbles = mutableListOf<TextBubble>()
        val sampleW = 300
        val sampleH = (bitmap.height * (300.0 / bitmap.width)).toInt().coerceAtLeast(300)
        val scaled = Bitmap.createScaledBitmap(bitmap, sampleW, sampleH, true)

        val pixels = IntArray(sampleW * sampleH)
        scaled.getPixels(pixels, 0, sampleW, 0, 0, sampleW, sampleH)

        val binary = BooleanArray(sampleW * sampleH)
        for (i in pixels.indices) {
            val color = pixels[i]
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF
            binary[i] = ((r + g + b) / 3) > 215
        }

        val gridRows = 10
        val gridCols = 5
        val cellW = sampleW / gridCols
        val cellH = sampleH / gridRows

        for (r in 0 until gridRows) {
            for (c in 0 until gridCols) {
                var whiteCount = 0
                for (cy in r * cellH until (r + 1) * cellH) {
                    for (cx in c * cellW until (c + 1) * cellW) {
                        if (binary[cy * sampleW + cx]) whiteCount++
                    }
                }
                if (whiteCount.toFloat() / (cellW * cellH) > 0.40f) {
                    bubbles.add(
                        TextBubble(
                            x = (c * cellW).toFloat() / sampleW,
                            y = (r * cellH).toFloat() / sampleH,
                            width = cellW.toFloat() / sampleW,
                            height = cellH.toFloat() / sampleH
                        )
                    )
                }
            }
        }

        if (bubbles.isEmpty()) {
            bubbles.add(TextBubble(x = 0.2f, y = 0.3f, width = 0.6f, height = 0.18f))
        }

        return mergeOverlappingBubbles(bubbles)
    }

    private fun mergeOverlappingBubbles(bubbles: List<TextBubble>): List<TextBubble> {
        val merged = mutableListOf<TextBubble>()
        for (b in bubbles) {
            var isMerged = false
            for (m in merged) {
                val rect1 = RectF(b.x, b.y, b.x + b.width, b.y + b.height)
                val rect2 = RectF(m.x, m.y, m.x + m.width, m.y + m.height)
                if (RectF.intersects(rect1, rect2)) {
                    val newLeft = minOf(rect1.left, rect2.left)
                    val newTop = minOf(rect1.top, rect2.top)
                    val newRight = maxOf(rect1.right, rect2.right)
                    val newBottom = maxOf(rect1.bottom, rect2.bottom)

                    merged.remove(m)
                    merged.add(
                        TextBubble(
                            x = newLeft,
                            y = newTop,
                            width = newRight - newLeft,
                            height = newBottom - newTop
                        )
                    )
                    isMerged = true
                    break
                }
            }
            if (!isMerged) {
                merged.add(b)
            }
        }
        return merged
    }
}
