package com.lojia.shiftreport.printer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface

/**
 * Utility for rendering non-Latin (Bengali, Arabic, etc.) receipt lines to high-contrast
 * monochrome bitmaps for ESC/POS thermal printers, avoiding ???? question mark artifacts.
 */
object ThermalBitmapRenderer {

    fun containsNonLatin(text: String): Boolean {
        for (i in 0 until text.length) {
            val code = text[i].code
            if (code in 0x0600..0x06FF || // Arabic
                code in 0x0750..0x077F || // Arabic Supplement
                code in 0x08A0..0x08FF || // Arabic Extended-A
                code in 0xFB50..0xFDFF || // Arabic Presentation Forms-A
                code in 0xFE70..0xFEFF || // Arabic Presentation Forms-B
                code in 0x0980..0x09FF || // Bengali
                (code > 0x024F && code !in 0x2000..0x206F && code !in 0x2190..0x21FF) // Non-Latin scripts
            ) {
                return true
            }
        }
        return false
    }

    /**
     * Renders a single receipt line or multi-column line into a monochrome bitmap.
     */
    fun renderLineToBitmap(
        rawLine: String,
        widthPx: Int = 384,
        fontSizePx: Float = 24f,
        typeface: Typeface = Typeface.DEFAULT
    ): Bitmap {
        val isBold = rawLine.contains("<b>", ignoreCase = true)
        val isBig = rawLine.contains("<font size='big'>", ignoreCase = true)
        val actualFontSize = if (isBig) fontSizePx * 1.35f else fontSizePx
        val actualTypeface = if (isBold) Typeface.create(typeface, Typeface.BOLD) else typeface

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = actualFontSize
            this.typeface = actualTypeface
        }

        // Remove XML/HTML style formatting tags
        val clean = rawLine
            .replace(Regex("<font[^>]*>"), "")
            .replace("</font>", "")
            .replace("<b>", "")
            .replace("</b>", "")
            .trimEnd('\r', '\n')

        val fontMetrics = paint.fontMetrics
        val lineHeight = (fontMetrics.bottom - fontMetrics.top)
        val heightPx = (lineHeight + 8f).toInt().coerceAtLeast(24)

        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val baseline = 4f - fontMetrics.top

        when {
            // Two column [L]...[R]...
            clean.startsWith("[L]") && clean.contains("[R]") -> {
                val afterL = clean.substring(3)
                val parts = afterL.split("[R]", limit = 2)
                val left = parts[0]
                val right = if (parts.size > 1) parts[1] else ""

                canvas.drawText(left, 0f, baseline, paint)
                val rightWidth = paint.measureText(right)
                canvas.drawText(right, (widthPx - rightWidth).coerceAtLeast(0f), baseline, paint)
            }
            // Three column [L]...[C]...[R]...
            clean.startsWith("[L]") && clean.contains("[C]") && clean.contains("[R]") -> {
                val afterL = clean.substring(3)
                val partsC = afterL.split("[C]", limit = 2)
                val left = partsC[0]
                val partsR = if (partsC.size > 1) partsC[1].split("[R]", limit = 2) else listOf("")
                val center = partsR[0]
                val right = if (partsR.size > 1) partsR[1] else ""

                canvas.drawText(left, 0f, baseline, paint)
                val centerWidth = paint.measureText(center)
                canvas.drawText(center, (widthPx - centerWidth) / 2f, baseline, paint)
                val rightWidth = paint.measureText(right)
                canvas.drawText(right, (widthPx - rightWidth).coerceAtLeast(0f), baseline, paint)
            }
            // Center [C]...
            clean.startsWith("[C]") -> {
                val text = clean.substring(3)
                val textWidth = paint.measureText(text)
                canvas.drawText(text, ((widthPx - textWidth) / 2f).coerceAtLeast(0f), baseline, paint)
            }
            // Right [R]...
            clean.startsWith("[R]") -> {
                val text = clean.substring(3)
                val textWidth = paint.measureText(text)
                canvas.drawText(text, (widthPx - textWidth).coerceAtLeast(0f), baseline, paint)
            }
            // Left [L]... or plain
            else -> {
                val text = if (clean.startsWith("[L]")) clean.substring(3) else clean
                canvas.drawText(text, 0f, baseline, paint)
            }
        }

        return bitmap
    }
}
