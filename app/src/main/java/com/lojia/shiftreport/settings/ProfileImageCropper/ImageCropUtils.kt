package com.lojia.shiftreport.settings.ProfileImageCropper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.lojia.shiftreport.util.AppLogger
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.max

/**
 * Image processing utilities for POS profile and store logo cropping.
 * Produces crisp 512x512 circular avatars at 90% JPEG quality with automatic
 * EXIF rotation correction.
 */
object ImageCropUtils {

    private const val TAG = "ImageCropUtils"
    const val TARGET_AVATAR_SIZE = 512
    const val JPEG_QUALITY = 90

    /**
     * Decodes and scales an image from Uri with EXIF rotation compensation.
     */
    fun decodeSampledBitmap(context: Context, uri: Uri, maxDimension: Int = 1400): Bitmap? {
        return try {
            // 1. Decode dimensions only
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            var inSampleSize = 1
            if (options.outHeight > maxDimension || options.outWidth > maxDimension) {
                val halfHeight = options.outHeight / 2
                val halfWidth = options.outWidth / 2
                while ((halfHeight / inSampleSize) >= maxDimension && (halfWidth / inSampleSize) >= maxDimension) {
                    inSampleSize *= 2
                }
            }

            // 2. Decode sampled bitmap
            options.inJustDecodeBounds = false
            options.inSampleSize = inSampleSize
            val rawBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            } ?: return null

            // 3. Compensate for camera EXIF rotation
            val rotationDegrees = getExifOrientationDegrees(context, uri)
            if (rotationDegrees != 0) {
                val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
            } else {
                rawBitmap
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error decoding bitmap from URI: ${e.message}", e)
            null
        }
    }

    private fun getExifOrientationDegrees(context: Context, uri: Uri): Int {
        var inputStream: InputStream? = null
        return try {
            inputStream = context.contentResolver.openInputStream(uri) ?: return 0
            val exif = ExifInterface(inputStream)
            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } catch (e: Exception) {
            0
        } finally {
            try {
                inputStream?.close()
            } catch (ignored: Exception) {}
        }
    }

    /**
     * Crops the original bitmap into a pristine 512x512 circular avatar based on
     * viewport dimensions, zoom level, pan offset, and crop circle diameter.
     */
    fun cropToCircleBitmap(
        originalBitmap: Bitmap,
        cropDiameterPx: Float,
        viewportWidth: Float,
        viewportHeight: Float,
        userZoom: Float,
        panX: Float,
        panY: Float,
        targetSize: Int = TARGET_AVATAR_SIZE
    ): Bitmap {
        val target = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(target)

        // 1. Clip canvas to circle
        val circlePath = Path().apply {
            val r = targetSize / 2f
            addCircle(r, r, r, Path.Direction.CW)
        }
        canvas.clipPath(circlePath)

        // 2. Base scale: fit image to cover the crop circle
        val baseScale = max(
            cropDiameterPx / originalBitmap.width.toFloat(),
            cropDiameterPx / originalBitmap.height.toFloat()
        )
        val totalScale = baseScale * userZoom

        val displayedWidth = originalBitmap.width * totalScale
        val displayedHeight = originalBitmap.height * totalScale

        val imageLeftInViewport = (viewportWidth / 2f + panX) - (displayedWidth / 2f)
        val imageTopInViewport = (viewportHeight / 2f + panY) - (displayedHeight / 2f)

        val cropLeftInViewport = (viewportWidth - cropDiameterPx) / 2f
        val cropTopInViewport = (viewportHeight - cropDiameterPx) / 2f

        // 3. Transformation matrix mapping to target 512x512
        val matrix = Matrix().apply {
            postScale(totalScale, totalScale)
            postTranslate(imageLeftInViewport - cropLeftInViewport, imageTopInViewport - cropTopInViewport)
            val scaleToTarget = targetSize.toFloat() / cropDiameterPx
            postScale(scaleToTarget, scaleToTarget)
        }

        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        canvas.drawBitmap(originalBitmap, matrix, paint)

        return target
    }

    /**
     * Overload accepting RectF crop rect.
     */
    fun cropToCircleBitmap(
        originalBitmap: Bitmap,
        cropRect: RectF,
        zoom: Float
    ): Bitmap {
        val targetSize = TARGET_AVATAR_SIZE
        val target = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(target)

        val circlePath = Path().apply {
            val r = targetSize / 2f
            addCircle(r, r, r, Path.Direction.CW)
        }
        canvas.clipPath(circlePath)

        val safeLeft = cropRect.left.coerceIn(0f, originalBitmap.width.toFloat()).toInt()
        val safeTop = cropRect.top.coerceIn(0f, originalBitmap.height.toFloat()).toInt()
        val safeWidth = cropRect.width().coerceAtMost(originalBitmap.width.toFloat() - safeLeft).toInt().coerceAtLeast(1)
        val safeHeight = cropRect.height().coerceAtMost(originalBitmap.height.toFloat() - safeTop).toInt().coerceAtLeast(1)

        val croppedSub = Bitmap.createBitmap(originalBitmap, safeLeft, safeTop, safeWidth, safeHeight)
        val matrix = Matrix().apply {
            postScale(targetSize.toFloat() / safeWidth, targetSize.toFloat() / safeHeight)
        }
        canvas.drawBitmap(croppedSub, matrix, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
        return target
    }

    /**
     * Compresses and writes the circular avatar to a target file at 90% JPEG quality.
     */
    fun saveCroppedBitmap(bitmap: Bitmap, destFile: File, quality: Int = JPEG_QUALITY): Boolean {
        return try {
            if (destFile.exists()) {
                destFile.delete()
            }
            destFile.parentFile?.mkdirs()
            FileOutputStream(destFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
                out.flush()
            }
            true
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to save cropped bitmap: ${e.message}", e)
            false
        }
    }
}
