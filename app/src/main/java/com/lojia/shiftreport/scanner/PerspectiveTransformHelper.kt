package com.lojia.shiftreport.scanner

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.hypot
import kotlin.math.max

/**
 * Encapsulates the 4 corner points of a document quadrangle in image coordinates.
 */
data class DocumentCorners(
    val topLeft: PointF,
    val topRight: PointF,
    val bottomRight: PointF,
    val bottomLeft: PointF
) {
    fun toList(): List<PointF> = listOf(topLeft, topRight, bottomRight, bottomLeft)

    companion object {
        /**
         * Creates a default inset box (e.g. 8% margin from edges) for an image of given width & height.
         */
        fun defaultForDimensions(width: Float, height: Float, marginFraction: Float = 0.08f): DocumentCorners {
            val marginX = width * marginFraction
            val marginY = height * marginFraction
            return DocumentCorners(
                topLeft = PointF(marginX, marginY),
                topRight = PointF(width - marginX, marginY),
                bottomRight = PointF(width - marginX, height - marginY),
                bottomLeft = PointF(marginX, height - marginY)
            )
        }
    }
}

object PerspectiveTransformHelper {

    /**
     * Straightens and flattens a quadrilateral document region defined by [corners]
     * into an unwarped rectangular [Bitmap] using 4-point homography (setPolyToPoly).
     */
    suspend fun cropAndStraighten(
        sourceBitmap: Bitmap,
        corners: DocumentCorners
    ): Bitmap = withContext(Dispatchers.Default) {
        val tl = corners.topLeft
        val tr = corners.topRight
        val br = corners.bottomRight
        val bl = corners.bottomLeft

        // Calculate destination width and height based on Euclidean distance
        val widthTop = hypot((tr.x - tl.x).toDouble(), (tr.y - tl.y).toDouble())
        val widthBottom = hypot((br.x - bl.x).toDouble(), (br.y - bl.y).toDouble())
        val destWidth = max(widthTop, widthBottom).toInt().coerceIn(100, 4096)

        val heightLeft = hypot((bl.x - tl.x).toDouble(), (bl.y - tl.y).toDouble())
        val heightRight = hypot((br.x - tr.x).toDouble(), (br.y - tr.y).toDouble())
        val destHeight = max(heightLeft, heightRight).toInt().coerceIn(100, 4096)

        val srcPoints = floatArrayOf(
            tl.x, tl.y,
            tr.x, tr.y,
            br.x, br.y,
            bl.x, bl.y
        )

        val dstPoints = floatArrayOf(
            0f, 0f,
            destWidth.toFloat(), 0f,
            destWidth.toFloat(), destHeight.toFloat(),
            0f, destHeight.toFloat()
        )

        val matrix = Matrix()
        val success = matrix.setPolyToPoly(srcPoints, 0, dstPoints, 0, 4)

        val resultBitmap = Bitmap.createBitmap(destWidth, destHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)

        if (success) {
            canvas.drawBitmap(sourceBitmap, matrix, paint)
        } else {
            // Fallback to bounding box sub-crop
            val minX = listOf(tl.x, bl.x).minOrNull()?.toInt()?.coerceAtLeast(0) ?: 0
            val minY = listOf(tl.y, tr.y).minOrNull()?.toInt()?.coerceAtLeast(0) ?: 0
            val maxX = listOf(tr.x, br.x).maxOrNull()?.toInt()?.coerceAtMost(sourceBitmap.width) ?: sourceBitmap.width
            val maxY = listOf(bl.y, br.y).maxOrNull()?.toInt()?.coerceAtMost(sourceBitmap.height) ?: sourceBitmap.height
            val cropW = (maxX - minX).coerceAtLeast(1)
            val cropH = (maxY - minY).coerceAtLeast(1)
            val cropped = Bitmap.createBitmap(sourceBitmap, minX, minY, cropW, cropH)
            return@withContext cropped
        }

        return@withContext resultBitmap
    }

    /**
     * Automatically estimates document corner boundaries by detecting contrast transitions
     * from edges toward the center (CamScanner smart auto-boundary detection).
     */
    suspend fun detectDocumentCorners(bitmap: Bitmap): DocumentCorners = withContext(Dispatchers.Default) {
        val w = bitmap.width
        val h = bitmap.height

        // Downscale for fast edge analysis
        val scale = 0.25f
        val smallW = (w * scale).toInt().coerceAtLeast(64)
        val smallH = (h * scale).toInt().coerceAtLeast(64)
        val small = Bitmap.createScaledBitmap(bitmap, smallW, smallH, false)

        val pixels = IntArray(smallW * smallH)
        small.getPixels(pixels, 0, smallW, 0, 0, smallW, smallH)
        small.recycle()

        // Grayscale conversion & edge gradient detection
        fun getLuminance(x: Int, y: Int): Int {
            if (x !in 0 until smallW || y !in 0 until smallH) return 128
            val p = pixels[y * smallW + x]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            return (r * 299 + g * 587 + b * 114) / 1000
        }

        // Raycast inward from corners along diagonals to find document paper edges
        fun scanDiagonal(startX: Int, startY: Int, dirX: Int, dirY: Int, steps: Int): PointF {
            var prevLum = getLuminance(startX, startY)
            var bestDist = 0
            var maxGrad = 0

            for (i in 2 until steps) {
                val cx = startX + dirX * i
                val cy = startY + dirY * i
                val curLum = getLuminance(cx, cy)
                val diff = Math.abs(curLum - prevLum)
                if (diff > maxGrad && diff > 25) {
                    maxGrad = diff
                    bestDist = i
                }
                prevLum = curLum
            }

            return if (bestDist > 0) {
                PointF(
                    (startX + dirX * bestDist) / scale,
                    (startY + dirY * bestDist) / scale
                )
            } else {
                PointF(
                    (startX + dirX * (steps / 3)) / scale,
                    (startY + dirY * (steps / 3)) / scale
                )
            }
        }

        val diagSteps = Math.min(smallW, smallH) / 3
        val tl = scanDiagonal(0, 0, 1, 1, diagSteps)
        val tr = scanDiagonal(smallW - 1, 0, -1, 1, diagSteps)
        val br = scanDiagonal(smallW - 1, smallH - 1, -1, -1, diagSteps)
        val bl = scanDiagonal(0, smallH - 1, 1, -1, diagSteps)

        // Safety clamps
        val clampedTl = PointF(tl.x.coerceIn(0f, w * 0.4f), tl.y.coerceIn(0f, h * 0.4f))
        val clampedTr = PointF(tr.x.coerceIn(w * 0.6f, w.toFloat()), tr.y.coerceIn(0f, h * 0.4f))
        val clampedBr = PointF(br.x.coerceIn(w * 0.6f, w.toFloat()), br.y.coerceIn(h * 0.6f, h.toFloat()))
        val clampedBl = PointF(bl.x.coerceIn(0f, w * 0.4f), bl.y.coerceIn(h * 0.6f, h.toFloat()))

        return@withContext DocumentCorners(
            topLeft = clampedTl,
            topRight = clampedTr,
            bottomRight = clampedBr,
            bottomLeft = clampedBl
        )
    }

    /**
     * Rotates a bitmap by 90 degrees clockwise.
     */
    fun rotateBitmap(source: Bitmap, degrees: Float = 90f): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }
}
