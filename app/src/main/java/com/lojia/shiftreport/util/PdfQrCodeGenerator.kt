package com.lojia.shiftreport.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF

/**
 * Generator for high-aesthetic styled QR codes in shift reports.
 * Renders squircle finder eye patterns, organic connected capsule modules, and centered logo masks.
 */
object PdfQrCodeGenerator {

    fun generateStyledQrCodeBitmap(text: String, targetPixelSize: Int = 400): Bitmap? {
        return try {
            val bitMatrix = NativeQrCodeEncoder.encode(text, NativeQrCodeEncoder.EcLevel.H)
            val matrixWidth = bitMatrix.width
            val matrixHeight = bitMatrix.height

            val bmp = Bitmap.createBitmap(targetPixelSize, targetPixelSize, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            canvas.drawColor(PdfPalette.SURFACE)

            val moduleSize = targetPixelSize.toFloat() / matrixWidth

            // Charcoal dark slate minimalist color matching modern design guidelines
            val primaryPaint = Paint().apply {
                color = PdfPalette.QR_DARK
                isAntiAlias = true
                style = Paint.Style.FILL
            }

            fun isFinderPattern(x: Int, y: Int): Boolean {
                if (x < 7 && y < 7) return true
                if (x >= matrixWidth - 7 && y < 7) return true
                if (x < 7 && y >= matrixHeight - 7) return true
                return false
            }

            val centerX = matrixWidth / 2
            val centerY = matrixHeight / 2
            val logoRadiusModules = 3.5 // Clear center area for logo

            fun isCenterArea(x: Int, y: Int): Boolean {
                val dx = x - centerX
                val dy = y - centerY
                return (dx * dx + dy * dy) <= (logoRadiusModules * logoRadiusModules)
            }

            // 1. Draw organic connected rounded capsule pill modules
            val r = moduleSize * 0.45f
            for (y in 0 until matrixHeight) {
                for (x in 0 until matrixWidth) {
                    if (isFinderPattern(x, y) || isCenterArea(x, y)) continue
                    if (bitMatrix.get(x, y)) {
                        val hasTop = (y > 0 && bitMatrix.get(x, y - 1) && !isFinderPattern(x, y - 1) && !isCenterArea(x, y - 1))
                        val hasBottom = (y < matrixHeight - 1 && bitMatrix.get(x, y + 1) && !isFinderPattern(x, y + 1) && !isCenterArea(x, y + 1))
                        val hasLeft = (x > 0 && bitMatrix.get(x - 1, y) && !isFinderPattern(x - 1, y) && !isCenterArea(x - 1, y))
                        val hasRight = (x < matrixWidth - 1 && bitMatrix.get(x + 1, y) && !isFinderPattern(x + 1, y) && !isCenterArea(x + 1, y))

                        val left = x * moduleSize
                        val top = y * moduleSize
                        val right = left + moduleSize
                        val bottom = top + moduleSize

                        val tl = if (!hasTop && !hasLeft) r else 0f
                        val tr = if (!hasTop && !hasRight) r else 0f
                        val br = if (!hasBottom && !hasRight) r else 0f
                        val bl = if (!hasBottom && !hasLeft) r else 0f

                        val radii = floatArrayOf(tl, tl, tr, tr, br, br, bl, bl)
                        val path = Path()
                        path.addRoundRect(RectF(left, top, right, bottom), radii, Path.Direction.CW)
                        canvas.drawPath(path, primaryPaint)
                    }
                }
            }

            // 2. Draw modern squircle Finder Eye Patterns at 3 corners
            fun drawFinderEye(startX: Int, startY: Int) {
                val left = startX * moduleSize
                val top = startY * moduleSize
                val eyeSize = 7 * moduleSize

                // Outer 7x7 rounded squircle
                val outerRect = RectF(left, top, left + eyeSize, top + eyeSize)
                val outerRadius = eyeSize * 0.32f
                canvas.drawRoundRect(outerRect, outerRadius, outerRadius, primaryPaint)

                // Inner white 5x5 cutout
                val whiteRect = RectF(left + moduleSize, top + moduleSize, left + 6 * moduleSize, top + 6 * moduleSize)
                val whitePaint = Paint().apply { color = PdfPalette.SURFACE; isAntiAlias = true; style = Paint.Style.FILL }
                val whiteRadius = (5 * moduleSize) * 0.28f
                canvas.drawRoundRect(whiteRect, whiteRadius, whiteRadius, whitePaint)

                // Center 3x3 pupil squircle
                val pupilRect = RectF(left + 2 * moduleSize, top + 2 * moduleSize, left + 5 * moduleSize, top + 5 * moduleSize)
                val pupilRadius = (3 * moduleSize) * 0.35f
                canvas.drawRoundRect(pupilRect, pupilRadius, pupilRadius, primaryPaint)
            }

            drawFinderEye(0, 0)
            drawFinderEye(matrixWidth - 7, 0)
            drawFinderEye(0, matrixHeight - 7)

            // 3. Draw central aesthetic logo emblem overlay (matching reference design)
            val centerPxX = targetPixelSize / 2f
            val centerPxY = targetPixelSize / 2f
            val badgeRadius = logoRadiusModules.toFloat() * moduleSize

            // Outer white mask circle
            canvas.drawCircle(centerPxX, centerPxY, badgeRadius, Paint().apply { color = PdfPalette.SURFACE; isAntiAlias = true })

            // Outer dark ring
            val ringPaint = Paint().apply {
                color = PdfPalette.QR_DARK
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeWidth = moduleSize * 0.7f
            }
            canvas.drawCircle(centerPxX, centerPxY, badgeRadius - (moduleSize * 0.35f), ringPaint)

            // Inner dark pupil circle / logo emblem core
            canvas.drawCircle(centerPxX, centerPxY, badgeRadius * 0.45f, primaryPaint)

            bmp
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
