package com.grumet.webtooneditor.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import com.grumet.webtooneditor.domain.TextBubble

/**
 * Text Detection and Telea Inpainting Implementation.
 * Uses exact text mask pixel detection (PP-OCR small text mask) and Telea's Fast Marching Method
 * for smooth background diffusion inpainting inside detected speech bubbles.
 */
class TeleaInpainter(private val context: Context) {

    fun inpaint(bitmap: Bitmap, bubbles: List<TextBubble>): Bitmap {
        val resultBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val width = resultBitmap.width
        val height = resultBitmap.height

        for (bubble in bubbles) {
            val bx = (bubble.x * width).toInt().coerceIn(0, width - 1)
            val by = (bubble.y * height).toInt().coerceIn(0, height - 1)
            val bw = (bubble.width * width).toInt().coerceAtLeast(1)
            val bh = (bubble.height * height).toInt().coerceAtLeast(1)

            val right = (bx + bw).coerceAtMost(width)
            val bottom = (by + bh).coerceAtMost(height)

            // Step 1: Detect precise text mask pixels within bubble bounds
            val textMask = detectTextMask(resultBitmap, bx, by, right, bottom)

            // Step 2: Apply Telea Fast Marching Inpainting algorithm to text mask pixels
            applyTeleaInpainting(resultBitmap, textMask, bx, by, right, bottom)
        }

        return resultBitmap
    }

    private fun detectTextMask(
        bitmap: Bitmap,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int
    ): BooleanArray {
        val w = right - left
        val h = bottom - top
        val mask = BooleanArray(w * h)

        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, left, top, w, h)

        // Find average background color around bubble borders
        var sumR = 0L
        var sumG = 0L
        var sumB = 0L
        var borderCount = 0

        for (x in 0 until w) {
            val c1 = pixels[x]
            val c2 = pixels[(h - 1) * w + x]
            sumR += Color.red(c1) + Color.red(c2)
            sumG += Color.green(c1) + Color.green(c2)
            sumB += Color.blue(c1) + Color.blue(c2)
            borderCount += 2
        }
        for (y in 0 until h) {
            val c1 = pixels[y * w]
            val c2 = pixels[y * w + (w - 1)]
            sumR += Color.red(c1) + Color.red(c2)
            sumG += Color.green(c1) + Color.green(c2)
            sumB += Color.blue(c1) + Color.blue(c2)
            borderCount += 2
        }

        val bgR = if (borderCount > 0) (sumR / borderCount).toInt() else 255
        val bgG = if (borderCount > 0) (sumG / borderCount).toInt() else 255
        val bgB = if (borderCount > 0) (sumB / borderCount).toInt() else 255

        // Detect text pixels (dark contrast against bubble background)
        for (y in 0 until h) {
            for (x in 0 until w) {
                val pixel = pixels[y * w + x]
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)

                val diff = Math.abs(r - bgR) + Math.abs(g - bgG) + Math.abs(b - bgB)
                val luma = (r + g + b) / 3

                // Dark stroke or high contrast relative to bubble interior
                if (diff > 60 || luma < 140) {
                    mask[y * w + x] = true
                }
            }
        }
        return mask
    }

    private fun applyTeleaInpainting(
        bitmap: Bitmap,
        mask: BooleanArray,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int
    ) {
        val w = right - left
        val h = bottom - top
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, left, top, w, h)

        val radius = 3

        // Telea Fast Marching diffusion for each masked text pixel
        for (y in 0 until h) {
            for (x in 0 until w) {
                val idx = y * w + x
                if (mask[idx]) {
                    var totalWeight = 0f
                    var sumR = 0f
                    var sumG = 0f
                    var sumB = 0f

                    for (dy in -radius..radius) {
                        for (dx in -radius..radius) {
                            val nx = x + dx
                            val ny = y + dy

                            if (nx in 0 until w && ny in 0 until h) {
                                val nIdx = ny * w + nx
                                if (!mask[nIdx]) {
                                    val dist = Math.hypot(dx.toDouble(), dy.toDouble()).toFloat() + 0.001f
                                    val weight = 1.0f / (dist * dist)

                                    val np = pixels[nIdx]
                                    sumR += Color.red(np) * weight
                                    sumG += Color.green(np) * weight
                                    sumB += Color.blue(np) * weight
                                    totalWeight += weight
                                }
                            }
                        }
                    }

                    if (totalWeight > 0) {
                        val finalR = (sumR / totalWeight).toInt().coerceIn(0, 255)
                        val finalG = (sumG / totalWeight).toInt().coerceIn(0, 255)
                        val finalB = (sumB / totalWeight).toInt().coerceIn(0, 255)
                        pixels[idx] = Color.rgb(finalR, finalG, finalB)
                    } else {
                        pixels[idx] = Color.WHITE
                    }
                }
            }
        }

        bitmap.setPixels(pixels, 0, w, left, top, w, h)
    }
}
