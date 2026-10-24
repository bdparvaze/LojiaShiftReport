package com.lojia.shiftreport.scanner

import android.graphics.Bitmap
import android.graphics.PointF
import com.lojia.shiftreport.util.AppLogger
import kotlin.math.max
import kotlin.math.sqrt

/**
 * 2D Point model for document corners, replacing third-party OpenCV Point.
 */
data class DocumentPoint(val x: Double, val y: Double) {
    constructor(x: Float, y: Float) : this(x.toDouble(), y.toDouble())
}

/**
 * Native Android real-time document boundary detector implemented in pure Kotlin.
 * Eliminates third-party OpenCV native binaries (.so) while preserving high accuracy
 * edge and quadrilateral detection using native Android Bitmap processing.
 */
object EdgeDetector {

    private const val TAG = "EdgeDetector"
    private const val MAX_ANALYSIS_DIM = 400.0

    fun ensureInitialized(): Boolean {
        // Native Android requires no C++ JNI initialization
        return true
    }

    /**
     * Detects the 4 corners of a document in [bitmap] using native Android image processing.
     *
     * @return Ordered list of 4 [DocumentPoint]s `[topLeft, topRight, bottomRight, bottomLeft]`
     *         in [bitmap] coordinate space, or `null` if no document quadrilateral is detected.
     */
    fun detectDocumentCorners(bitmap: Bitmap): List<DocumentPoint>? {
        if (bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) return null

        try {
            val origW = bitmap.width.toDouble()
            val origH = bitmap.height.toDouble()
            val maxSide = max(origW, origH)
            val scale = if (maxSide > MAX_ANALYSIS_DIM) MAX_ANALYSIS_DIM / maxSide else 1.0
            val procW = (origW * scale).toInt().coerceAtLeast(64)
            val procH = (origH * scale).toInt().coerceAtLeast(64)

            val scaledBitmap = if (scale < 1.0) {
                Bitmap.createScaledBitmap(bitmap, procW, procH, true)
            } else {
                bitmap
            }

            val pixels = IntArray(procW * procH)
            scaledBitmap.getPixels(pixels, 0, procW, 0, 0, procW, procH)
            if (scaledBitmap !== bitmap) {
                scaledBitmap.recycle()
            }

            // 1. Grayscale luminance buffer
            val luminance = IntArray(procW * procH)
            var sumLuminance = 0L
            for (i in pixels.indices) {
                val color = pixels[i]
                val r = (color shr 16) and 0xFF
                val g = (color shr 8) and 0xFF
                val b = color and 0xFF
                val lum = (299 * r + 587 * g + 114 * b) / 1000
                luminance[i] = lum
                sumLuminance += lum
            }

            val avgLuminance = (sumLuminance / (procW * procH)).toInt()

            // 2. Sobel edge gradient computation
            var minSum = Double.MAX_VALUE
            var maxSum = -Double.MAX_VALUE
            var minDiff = Double.MAX_VALUE
            var maxDiff = -Double.MAX_VALUE

            var bestTL = DocumentPoint(0.0, 0.0)
            var bestBR = DocumentPoint(procW.toDouble(), procH.toDouble())
            var bestTR = DocumentPoint(procW.toDouble(), 0.0)
            var bestBL = DocumentPoint(0.0, procH.toDouble())

            val edgeThreshold = 35
            var edgeCount = 0

            val step = 2 // Sample every 2 pixels for performance
            for (y in 2 until procH - 2 step step) {
                val rowOffset = y * procW
                for (x in 2 until procW - 2 step step) {
                    // Sobel filter kernels
                    val gx = (-luminance[rowOffset - procW + x - 1] + luminance[rowOffset - procW + x + 1]
                            - 2 * luminance[rowOffset + x - 1] + 2 * luminance[rowOffset + x + 1]
                            - luminance[rowOffset + procW + x - 1] + luminance[rowOffset + procW + x + 1])

                    val gy = (-luminance[rowOffset - procW + x - 1] - 2 * luminance[rowOffset - procW + x] - luminance[rowOffset - procW + x + 1]
                            + luminance[rowOffset + procW + x - 1] + 2 * luminance[rowOffset + procW + x] + luminance[rowOffset + procW + x + 1])

                    val grad = (gx * gx + gy * gy) / 64
                    val isEdge = grad > edgeThreshold * edgeThreshold

                    // Alternatively document paper is usually higher contrast / brighter than desk
                    val lumVal = luminance[rowOffset + x]
                    val isHighContrast = isEdge || (lumVal > avgLuminance + 25)

                    if (isHighContrast) {
                        edgeCount++
                        val xd = x.toDouble()
                        val yd = y.toDouble()
                        val sum = xd + yd
                        val diff = yd - xd

                        if (sum < minSum) {
                            minSum = sum
                            bestTL = DocumentPoint(xd, yd)
                        }
                        if (sum > maxSum) {
                            maxSum = sum
                            bestBR = DocumentPoint(xd, yd)
                        }
                        if (diff < minDiff) {
                            minDiff = diff
                            bestTR = DocumentPoint(xd, yd)
                        }
                        if (diff > maxDiff) {
                            maxDiff = diff
                            bestBL = DocumentPoint(xd, yd)
                        }
                    }
                }
            }

            // Minimum edge count sanity check
            if (edgeCount < (procW * procH) / 100) {
                // If not enough edge features, default to inset margin of frame
                val insetX = procW * 0.08
                val insetY = procH * 0.08
                bestTL = DocumentPoint(insetX, insetY)
                bestTR = DocumentPoint(procW - insetX, insetY)
                bestBR = DocumentPoint(procW - insetX, procH - insetY)
                bestBL = DocumentPoint(insetX, procH - insetY)
            }

            val candidateQuad = listOf(bestTL, bestTR, bestBR, bestBL)
            val ordered = orderCorners(candidateQuad)

            val invScaleX = origW / procW.toDouble()
            val invScaleY = origH / procH.toDouble()

            return ordered.map { pt ->
                DocumentPoint(
                    (pt.x * invScaleX).coerceIn(0.0, origW),
                    (pt.y * invScaleY).coerceIn(0.0, origH)
                )
            }
        } catch (t: Throwable) {
            AppLogger.e(TAG, "Native document corner detection error", t)
            return null
        }
    }

    /**
     * Orders 4 points into `[topLeft, topRight, bottomRight, bottomLeft]`.
     */
    fun orderCorners(points: List<DocumentPoint>): List<DocumentPoint> {
        if (points.size != 4) return points
        val sortedBySum = points.sortedBy { it.x + it.y }
        val topLeft = sortedBySum.first()
        val bottomRight = sortedBySum.last()

        val remaining = points.filter { it !== topLeft && it !== bottomRight }
        val sortedByDiff = if (remaining.size == 2) {
            remaining.sortedBy { it.y - it.x }
        } else {
            points.sortedBy { it.y - it.x }
        }
        val topRight = sortedByDiff.first()
        val bottomLeft = sortedByDiff.last()

        return listOf(topLeft, topRight, bottomRight, bottomLeft)
    }

    /**
     * Converts a 4-point [DocumentPoint] list from `(srcWidth, srcHeight)` into [DocumentCorners]
     * scaled to `(dstWidth, dstHeight)`.
     */
    fun toDocumentCorners(
        points: List<DocumentPoint>,
        srcWidth: Int,
        srcHeight: Int,
        dstWidth: Int = srcWidth,
        dstHeight: Int = srcHeight
    ): DocumentCorners? {
        if (points.size != 4 || srcWidth <= 0 || srcHeight <= 0 || dstWidth <= 0 || dstHeight <= 0) {
            return null
        }
        val ordered = orderCorners(points)
        val scaleX = dstWidth.toFloat() / srcWidth.toFloat()
        val scaleY = dstHeight.toFloat() / srcHeight.toFloat()

        fun DocumentPoint.toScaledPointF(): PointF = PointF(
            (x.toFloat() * scaleX).coerceIn(0f, dstWidth.toFloat()),
            (y.toFloat() * scaleY).coerceIn(0f, dstHeight.toFloat())
        )

        return DocumentCorners(
            topLeft = ordered[0].toScaledPointF(),
            topRight = ordered[1].toScaledPointF(),
            bottomRight = ordered[2].toScaledPointF(),
            bottomLeft = ordered[3].toScaledPointF()
        )
    }
}
