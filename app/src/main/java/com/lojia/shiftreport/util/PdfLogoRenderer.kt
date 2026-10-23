package com.lojia.shiftreport.util

import android.content.Context
import android.graphics.*
import android.net.Uri
import java.io.File

/**
 * Handles business logo rendering, preset badge generation, and circular cropping for PDF and print engines.
 */
object PdfLogoRenderer {

    fun generatePresetLogoBitmap(presetKey: String, size: Int = 200): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val r = size / 2f
        val padding = size * 0.05f

        val (colorStart, colorEnd, iconSymbol) = when (presetKey) {
            "preset:cafe" -> Triple(Color.rgb(180, 83, 9), Color.rgb(120, 53, 15), "☕")
            "preset:retail" -> Triple(Color.rgb(16, 185, 129), Color.rgb(5, 150, 105), "🛍️")
            "preset:fashion" -> Triple(Color.rgb(236, 72, 153), Color.rgb(190, 24, 93), "💎")
            "preset:pharmacy" -> Triple(Color.rgb(6, 182, 212), Color.rgb(14, 116, 144), "⚕️")
            "preset:tech" -> Triple(Color.rgb(99, 102, 241), Color.rgb(67, 56, 202), "⚡")
            "preset:restaurant" -> Triple(Color.rgb(249, 115, 22), Color.rgb(194, 65, 12), "🍴")
            else -> Triple(Color.rgb(30, 58, 138), Color.rgb(15, 23, 42), "🏪") // superstore
        }

        val shader = LinearGradient(0f, 0f, size.toFloat(), size.toFloat(), colorStart, colorEnd, Shader.TileMode.CLAMP)
        paint.shader = shader
        canvas.drawCircle(r, r, r - padding, paint)

        paint.shader = null
        paint.textSize = size * 0.46f
        paint.textAlign = Paint.Align.CENTER
        val textY = r - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText(iconSymbol, r, textY, paint)

        return bmp
    }

    fun getCircularBitmap(bitmap: Bitmap, diameter: Int): Bitmap {
        val output = Bitmap.createBitmap(diameter, diameter, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
        }
        val rectF = RectF(0f, 0f, diameter.toFloat(), diameter.toFloat())

        canvas.drawARGB(0, 0, 0, 0)
        canvas.drawCircle(diameter / 2f, diameter / 2f, diameter / 2f, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)

        val srcRect = if (bitmap.width > bitmap.height) {
            val offset = (bitmap.width - bitmap.height) / 2
            Rect(offset, 0, offset + bitmap.height, bitmap.height)
        } else {
            val offset = (bitmap.height - bitmap.width) / 2
            Rect(0, offset, bitmap.width, offset + bitmap.width)
        }
        canvas.drawBitmap(bitmap, srcRect, rectF, paint)
        return output
    }

    fun loadBusinessLogoBitmap(context: Context, logoUri: String?, targetSize: Int = 200): Bitmap? {
        if (logoUri.isNullOrBlank()) return null
        return try {
            if (logoUri.startsWith("preset:")) {
                generatePresetLogoBitmap(logoUri, targetSize)
            } else if (logoUri.startsWith("content://") || logoUri.startsWith("file://")) {
                val uri = Uri.parse(logoUri)
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            } else {
                val file = File(logoUri)
                if (file.exists()) {
                    BitmapFactory.decodeFile(file.absolutePath)
                } else null
            }
        } catch (e: Exception) {
            AppLogger.e("PdfLogoRenderer", "Failed to load business logo: ${e.message}", e)
            null
        }
    }

    fun drawBusinessLogoBadge(
        canvas: Canvas,
        context: Context,
        logoUri: String?,
        bizName: String,
        x: Float,
        y: Float,
        size: Float = 54f
    ) {
        val radius = size / 2f
        val centerX = x + radius
        val centerY = y + radius

        val bgPaint = Paint().apply {
            color = PdfPalette.SURFACE
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        canvas.drawCircle(centerX, centerY, radius, bgPaint)

        val borderPaint = Paint().apply {
            color = PdfPalette.DIVIDER
            strokeWidth = 1.5f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        canvas.drawCircle(centerX, centerY, radius, borderPaint)

        val bmp = loadBusinessLogoBitmap(context, logoUri, 250)
        if (bmp != null) {
            val innerPadding = 2.5f
            val innerDiameter = ((radius - innerPadding) * 2f).toInt().coerceAtLeast(1)
            val circularBmp = getCircularBitmap(bmp, innerDiameter)
            val drawLeft = centerX - (innerDiameter / 2f)
            val drawTop = centerY - (innerDiameter / 2f)
            canvas.drawBitmap(circularBmp, drawLeft, drawTop, null)
        } else {
            // Draw placeholder initials
            val initials = if (bizName.isNotBlank()) {
                val words = bizName.trim().split(" ")
                if (words.size > 1) {
                    "${words[0].take(1)}${words[1].take(1)}".uppercase()
                } else {
                    bizName.take(2).uppercase()
                }
            } else "LR"

            val initialBgPaint = Paint().apply {
                color = PdfPalette.PRIMARY_CONTAINER
                isAntiAlias = true
                style = Paint.Style.FILL
            }
            canvas.drawCircle(centerX, centerY, radius - 2f, initialBgPaint)

            val textPaint = Paint().apply {
                color = PdfPalette.PRIMARY_DARK
                textSize = size * 0.42f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            val textY = centerY - (textPaint.descent() + textPaint.ascent()) / 2f
            canvas.drawText(initials, centerX, textY, textPaint)
        }
    }
}
