package com.lojia.shiftreport.scanner

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Standard CamScanner-style enhancement filters.
 */
enum class ScannerFilterType(val label: String) {
    ORIGINAL("Original"),
    LIGHTEN("Lighten"),
    MAGIC_COLOR("Magic Color"),
    GRAY_MODE("Gray Mode"),
    BW("B&W")
}

data class FilterAdjustments(
    val brightness: Float = 0f, // -50f to +50f
    val contrast: Float = 1.0f,  // 0.5f to 2.0f
    val detail: Float = 0f       // 0f to 1.0f
)

object DocumentImageFilter {

    /**
     * Applies the requested [filterType] to [source] bitmap without blocking the UI thread.
     */
    suspend fun applyFilter(
        source: Bitmap,
        filterType: ScannerFilterType,
        adjustments: FilterAdjustments = FilterAdjustments()
    ): Bitmap = withContext(Dispatchers.Default) {
        val baseFiltered = when (filterType) {
            ScannerFilterType.ORIGINAL -> source
            ScannerFilterType.LIGHTEN -> applyLighten(source)
            ScannerFilterType.MAGIC_COLOR -> applyMagicColor(source)
            ScannerFilterType.GRAY_MODE -> applyGrayMode(source)
            ScannerFilterType.BW -> applyBlackAndWhite(source)
        }

        if (adjustments.brightness != 0f || adjustments.contrast != 1.0f || adjustments.detail > 0.05f) {
            applyManualTuning(baseFiltered, adjustments)
        } else {
            baseFiltered
        }
    }

    /**
     * CamScanner "Lighten" filter:
     * Gentle brightness and highlight expansion to lighten shadows on document page.
     */
    private fun applyLighten(source: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val contrast = 1.12f
        val brightness = 35f
        val cm = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, brightness,
                0f, contrast, 0f, 0f, brightness,
                0f, 0f, contrast, 0f, brightness,
                0f, 0f, 0f, 1f, 0f
            )
        )

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(cm)
        }
        canvas.drawBitmap(source, 0f, 0f, paint)
        return output
    }

    /**
     * CamScanner "Magic Color" filter:
     * Lifts paper background to crisp white, deepens text contrast, and preserves ink/stamp colors.
     */
    private fun applyMagicColor(source: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val contrast = 1.35f
        val brightness = 24f

        val cm = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, brightness,
                0f, contrast, 0f, 0f, brightness,
                0f, 0f, contrast, 0f, brightness,
                0f, 0f, 0f, 1f, 0f
            )
        )

        val satMatrix = ColorMatrix()
        satMatrix.setSaturation(1.15f)
        cm.postConcat(satMatrix)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(cm)
        }

        canvas.drawBitmap(source, 0f, 0f, paint)
        return output
    }

    /**
     * Smooth Gray Mode filter with contrast correction for document readability.
     */
    private fun applyGrayMode(source: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val cm = ColorMatrix()
        cm.setSaturation(0f)

        val contrast = 1.25f
        val brightness = 18f
        val contrastMatrix = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, brightness,
                0f, contrast, 0f, 0f, brightness,
                0f, 0f, contrast, 0f, brightness,
                0f, 0f, 0f, 1f, 0f
            )
        )
        cm.postConcat(contrastMatrix)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(cm)
        }

        canvas.drawBitmap(source, 0f, 0f, paint)
        return output
    }

    /**
     * CamScanner B&W (Thresholding/Binarization):
     * Converts the document into pure high-contrast black text on pure white paper.
     */
    private fun applyBlackAndWhite(source: Bitmap): Bitmap {
        val w = source.width
        val h = source.height
        val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)

        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)

        var lumSum = 0L
        val step = (w * h / 5000).coerceAtLeast(1)
        var count = 0
        for (i in 0 until (w * h) step step) {
            val p = pixels[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            lumSum += (r * 299 + g * 587 + b * 114) / 1000
            count++
        }
        val avgLum = if (count > 0) (lumSum / count).toInt().coerceIn(90, 185) else 128
        val threshold = (avgLum * 0.92f).toInt()

        val white = 0xFFFFFFFF.toInt()
        val black = 0xFF000000.toInt()

        for (i in 0 until (w * h)) {
            val p = pixels[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            val lum = (r * 299 + g * 587 + b * 114) / 1000
            pixels[i] = if (lum >= threshold) white else black
        }

        output.setPixels(pixels, 0, w, 0, 0, w, h)
        return output
    }

    /**
     * Applies manual Brightness, Contrast, and Detail adjustments onto a bitmap.
     */
    private fun applyManualTuning(source: Bitmap, adjustments: FilterAdjustments): Bitmap {
        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val contrast = adjustments.contrast.coerceIn(0.5f, 2.0f)
        val brightness = adjustments.brightness.coerceIn(-60f, 60f)

        // ColorMatrix for brightness and contrast
        val cm = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, brightness,
                0f, contrast, 0f, 0f, brightness,
                0f, 0f, contrast, 0f, brightness,
                0f, 0f, 0f, 1f, 0f
            )
        )

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(cm)
        }
        canvas.drawBitmap(source, 0f, 0f, paint)

        // Detail sharpening if detail is dialed up
        if (adjustments.detail > 0.1f) {
            return applySharpen(output, adjustments.detail)
        }

        return output
    }

    /**
     * Fast unsharp-mask approximation to enhance document text edge details.
     */
    private fun applySharpen(source: Bitmap, detailFactor: Float): Bitmap {
        val w = source.width
        val h = source.height
        val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val srcPixels = IntArray(w * h)
        val dstPixels = IntArray(w * h)
        source.getPixels(srcPixels, 0, w, 0, 0, w, h)

        val strength = (detailFactor * 0.8f).coerceIn(0.1f, 0.8f)

        for (y in 1 until h - 1) {
            val yOffset = y * w
            for (x in 1 until w - 1) {
                val idx = yOffset + x
                val center = srcPixels[idx]
                val top = srcPixels[idx - w]
                val bottom = srcPixels[idx + w]
                val left = srcPixels[idx - 1]
                val right = srcPixels[idx + 1]

                val cr = (center shr 16) and 0xFF
                val cg = (center shr 8) and 0xFF
                val cb = center and 0xFF

                val avgR = (((top shr 16) and 0xFF) + ((bottom shr 16) and 0xFF) + ((left shr 16) and 0xFF) + ((right shr 16) and 0xFF)) shr 2
                val avgG = (((top shr 8) and 0xFF) + ((bottom shr 8) and 0xFF) + ((left shr 8) and 0xFF) + ((right shr 8) and 0xFF)) shr 2
                val avgB = ((top and 0xFF) + (bottom and 0xFF) + (left and 0xFF) + (right and 0xFF)) shr 2

                val nr = (cr + (cr - avgR) * strength).toInt().coerceIn(0, 255)
                val ng = (cg + (cg - avgG) * strength).toInt().coerceIn(0, 255)
                val nb = (cb + (cb - avgB) * strength).toInt().coerceIn(0, 255)

                dstPixels[idx] = (0xFF shl 24) or (nr shl 16) or (ng shl 8) or nb
            }
        }

        output.setPixels(dstPixels, 0, w, 0, 0, w, h)
        return output
    }
}
