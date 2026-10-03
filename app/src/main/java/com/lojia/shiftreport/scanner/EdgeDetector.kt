package com.lojia.shiftreport.scanner

import android.graphics.Bitmap
import android.graphics.PointF
import android.util.Log
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.max

/**
 * OpenCV-based real-time document boundary detector.
 * Pipeline: Grayscale -> Gaussian Blur -> Canny Edge Detection -> Find Contours -> approxPolyDP (4-corner quadrilateral).
 */
object EdgeDetector {

    private const val TAG = "EdgeDetector"
    private const val MAX_ANALYSIS_DIM = 500.0

    @Volatile
    private var isOpenCvInitialized = false

    @Synchronized
    fun ensureInitialized(): Boolean {
        if (isOpenCvInitialized) return true
        return try {
            isOpenCvInitialized = OpenCVLoader.initLocal()
            if (!isOpenCvInitialized) {
                Log.w(TAG, "OpenCVLoader.initLocal() returned false")
            }
            isOpenCvInitialized
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to initialize OpenCV native library", t)
            false
        }
    }

    /**
     * Detects the 4 corners of a document in [bitmap] using OpenCV.
     *
     * @return Ordered list of 4 [Point]s `[topLeft, topRight, bottomRight, bottomLeft]`
     *         in [bitmap] coordinate space, or `null` if no document quadrilateral is detected.
     */
    fun detectDocumentCorners(bitmap: Bitmap): List<Point>? {
        if (bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) return null
        if (!ensureInitialized()) return null

        val srcMat = Mat()
        val grayMat = Mat()
        val blurredMat = Mat()
        val edgesMat = Mat()
        val dilatedMat = Mat()
        val hierarchy = Mat()
        val contours = ArrayList<MatOfPoint>()
        var kernel: Mat? = null

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

            Utils.bitmapToMat(scaledBitmap, srcMat)
            if (scaledBitmap !== bitmap) {
                scaledBitmap.recycle()
            }

            // 1. Grayscale
            Imgproc.cvtColor(srcMat, grayMat, Imgproc.COLOR_RGBA2GRAY)

            // 2. Gaussian blur to suppress paper texture noise
            Imgproc.GaussianBlur(grayMat, blurredMat, Size(5.0, 5.0), 0.0)

            // 3. Canny edge detection
            Imgproc.Canny(blurredMat, edgesMat, 75.0, 200.0)

            // Slight dilation to bridge tiny gaps in Canny document borders
            kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(3.0, 3.0))
            Imgproc.dilate(edgesMat, dilatedMat, kernel)

            // 4. Find contours
            Imgproc.findContours(
                dilatedMat,
                contours,
                hierarchy,
                Imgproc.RETR_LIST,
                Imgproc.CHAIN_APPROX_SIMPLE
            )

            if (contours.isEmpty()) return null

            val minArea = procW * procH * 0.10
            val maxArea = procW * procH * 0.98

            // Sort contours by descending area
            val sortedContours = contours
                .map { it to Imgproc.contourArea(it) }
                .filter { (_, area) -> area in minArea..maxArea }
                .sortedByDescending { it.second }

            var detectedQuad: List<Point>? = null

            // 5. approxPolyDP to find largest convex 4-corner quadrilateral
            for ((contour, _) in sortedContours.take(10)) {
                val contour2f = MatOfPoint2f(*contour.toArray())
                val peri = Imgproc.arcLength(contour2f, true)
                val approx = MatOfPoint2f()
                Imgproc.approxPolyDP(contour2f, approx, 0.02 * peri, true)

                if (approx.total() == 4L) {
                    val pointsArray = approx.toArray()
                    val polyMat = MatOfPoint(*pointsArray)
                    val isConvex = Imgproc.isContourConvex(polyMat)
                    polyMat.release()

                    if (isConvex) {
                        detectedQuad = pointsArray.toList()
                        contour2f.release()
                        approx.release()
                        break
                    }
                }

                contour2f.release()
                approx.release()
            }

            val quad = detectedQuad ?: return null
            val ordered = orderCorners(quad)

            val invScaleX = origW / procW.toDouble()
            val invScaleY = origH / procH.toDouble()

            return ordered.map { pt ->
                Point(
                    (pt.x * invScaleX).coerceIn(0.0, origW),
                    (pt.y * invScaleY).coerceIn(0.0, origH)
                )
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Error detecting document corners with OpenCV", t)
            return null
        } finally {
            contours.forEach { it.release() }
            kernel?.release()
            hierarchy.release()
            dilatedMat.release()
            edgesMat.release()
            blurredMat.release()
            grayMat.release()
            srcMat.release()
        }
    }

    /**
     * Orders 4 points into `[topLeft, topRight, bottomRight, bottomLeft]`.
     */
    fun orderCorners(points: List<Point>): List<Point> {
        if (points.size != 4) return points
        // Top-left has smallest sum (x + y); Bottom-right has largest sum (x + y)
        val sortedBySum = points.sortedBy { it.x + it.y }
        val topLeft = sortedBySum.first()
        val bottomRight = sortedBySum.last()

        val remaining = points.filter { it !== topLeft && it !== bottomRight }
        // Between the remaining two, Top-right has smaller (y - x); Bottom-left has larger (y - x)
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
     * Converts a 4-point OpenCV [Point] list from `(srcWidth, srcHeight)` into [DocumentCorners]
     * scaled to `(dstWidth, dstHeight)`.
     */
    fun toDocumentCorners(
        points: List<Point>,
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

        fun Point.toScaledPointF(): PointF = PointF(
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
