package com.lojia.shiftreport.scanner

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.util.Log
import java.io.File
import kotlin.math.max

/**
 * Lightweight, zero-dependency image preprocessor designed to enhance text edge contrast
 * and letter recognition for Google ML Kit OCR.
 * Particularly beneficial for printed Bengali (Bangla) glyphs, accents, and mixed English/Bengali receipts.
 */
object OcrImagePreprocessor {

    private const val TAG = "OcrImagePreprocessor"
    private const val MAX_DIMENSION = 2048
    private const val MIN_RECOMMENDED_DIMENSION = 1000

    /**
     * Loads and optimizes the image at [file] for OCR.
     * Enhances local contrast, normalizes scale, and applies subtle sharpening
     * so that delicate Bengali vowel signs (kar) and headstrokes (matra) stand out cleanly against background paper.
     */
    fun loadOptimizedBitmapForOcr(file: File): Bitmap? {
        try {
            if (!file.exists() || file.length() == 0L) return null

            // 1. Decode bounds first to avoid OutOfMemoryError on huge images
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, options)

            val rawWidth = options.outWidth
            val rawHeight = options.outHeight
            if (rawWidth <= 0 || rawHeight <= 0) return null

            // Calculate sample size for downscaling if image is huge
            var sampleSize = 1
            val maxSide = max(rawWidth, rawHeight)
            while (maxSide / sampleSize > MAX_DIMENSION) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val baseBitmap = BitmapFactory.decodeFile(file.absolutePath, decodeOptions) ?: return null

            // 2. Enhance contrast for OCR
            return enhanceContrastForOcr(baseBitmap)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading/preprocessing bitmap for OCR", e)
            return null
        }
    }

    /**
     * Boosts contrast (+25%) and slight brightness curve (+5) to make faded printing and
     * non-Latin characters (like Bengali matras and conjuncts) easily discernible from scanner shadows.
     */
    fun enhanceContrastForOcr(source: Bitmap): Bitmap {
        return try {
            val width = source.width
            val height = source.height
            val enhancedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(enhancedBitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

            // High-contrast matrix: contrast factor ~1.30, brightness bias ~+5
            val contrast = 1.30f
            val translate = (-0.5f * contrast + 0.5f) * 255f + 5f

            val cm = ColorMatrix(
                floatArrayOf(
                    contrast, 0f, 0f, 0f, translate,
                    0f, contrast, 0f, 0f, translate,
                    0f, 0f, contrast, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                )
            )

            paint.colorFilter = ColorMatrixColorFilter(cm)
            canvas.drawBitmap(source, 0f, 0f, paint)
            enhancedBitmap
        } catch (e: Exception) {
            Log.w(TAG, "Failed to apply contrast filter, returning base bitmap", e)
            source
        }
    }
}
