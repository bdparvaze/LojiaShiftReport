package com.lojia.shiftreport.printer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint

data class ParsedLine(
    val left: String? = null,
    val center: String? = null,
    val right: String? = null,
    val isCenterOnly: Boolean = false,
    val isRightOnly: Boolean = false
)

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

    fun containsArabic(text: String): Boolean {
        for (i in 0 until text.length) {
            val code = text[i].code
            if (code in 0x0600..0x06FF ||
                code in 0x0750..0x077F ||
                code in 0x08A0..0x08FF ||
                code in 0xFB50..0xFDFF ||
                code in 0xFE70..0xFEFF
            ) {
                return true
            }
        }
        return false
    }

    /**
     * Splits a raw line into parsed column parts independent of Android UI classes for easy unit testing.
     */
    fun splitColumns(cleanLine: String): ParsedLine {
        val line = cleanLine.trimEnd('\r', '\n')
        return when {
            // Three column: [L]...[C]...[R]...
            line.startsWith("[L]") && line.contains("[C]") && line.contains("[R]") -> {
                val afterL = line.substring(3)
                val partsC = afterL.split("[C]", limit = 2)
                val left = partsC[0]
                val partsR = if (partsC.size > 1) partsC[1].split("[R]", limit = 2) else listOf("")
                val center = partsR[0]
                val right = if (partsR.size > 1) partsR[1] else ""
                ParsedLine(left = left, center = center, right = right)
            }
            // Two column: [L]...[R]...
            line.startsWith("[L]") && line.contains("[R]") -> {
                val afterL = line.substring(3)
                val parts = afterL.split("[R]", limit = 2)
                val left = parts[0]
                val right = if (parts.size > 1) parts[1] else ""
                ParsedLine(left = left, right = right)
            }
            // Center tag: [C]...
            line.startsWith("[C]") -> {
                ParsedLine(center = line.substring(3), isCenterOnly = true)
            }
            // Right tag: [R]...
            line.startsWith("[R]") -> {
                ParsedLine(right = line.substring(3), isRightOnly = true)
            }
            // Left tag: [L]...
            line.startsWith("[L]") -> {
                ParsedLine(left = line.substring(3))
            }
            // Plain line
            else -> {
                ParsedLine(left = line)
            }
        }
    }

    /**
     * Renders a single receipt line or multi-column line into a monochrome bitmap.
     * Uses Typeface.DEFAULT (or supplied typeface) supporting Bengali and Arabic system glyphs.
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
        val parsed = splitColumns(clean)

        when {
            // Three column [L]...[C]...[R]...
            parsed.left != null && parsed.center != null && parsed.right != null -> {
                canvas.drawText(parsed.left, 0f, baseline, paint)
                val centerWidth = paint.measureText(parsed.center)
                canvas.drawText(parsed.center, (widthPx - centerWidth) / 2f, baseline, paint)
                val rightWidth = paint.measureText(parsed.right)
                canvas.drawText(parsed.right, (widthPx - rightWidth).coerceAtLeast(0f), baseline, paint)
            }
            // Two column [L]...[R]...
            parsed.left != null && parsed.right != null -> {
                canvas.drawText(parsed.left, 0f, baseline, paint)
                val rightWidth = paint.measureText(parsed.right)
                canvas.drawText(parsed.right, (widthPx - rightWidth).coerceAtLeast(0f), baseline, paint)
            }
            // Center [C]...
            parsed.isCenterOnly && parsed.center != null -> {
                val text = parsed.center
                val textWidth = paint.measureText(text)
                canvas.drawText(text, ((widthPx - textWidth) / 2f).coerceAtLeast(0f), baseline, paint)
            }
            // Right [R]...
            parsed.isRightOnly && parsed.right != null -> {
                val text = parsed.right
                val textWidth = paint.measureText(text)
                canvas.drawText(text, (widthPx - textWidth).coerceAtLeast(0f), baseline, paint)
            }
            // Left or plain text line
            else -> {
                val text = parsed.left ?: ""
                if (containsArabic(text)) {
                    // Render Arabic lines RTL using StaticLayout with TextDirectionHeuristics.RTL
                    val textPaint = TextPaint(paint)
                    val layout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        StaticLayout.Builder.obtain(text, 0, text.length, textPaint, widthPx)
                            .setTextDirection(TextDirectionHeuristics.RTL)
                            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                            .build()
                    } else {
                        @Suppress("DEPRECATION")
                        StaticLayout(text, textPaint, widthPx, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false)
                    }
                    canvas.save()
                    canvas.translate(0f, 4f)
                    layout.draw(canvas)
                    canvas.restore()
                } else {
                    canvas.drawText(text, 0f, baseline, paint)
                }
            }
        }

        return bitmap
    }
}
