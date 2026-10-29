package com.lojia.shiftreport.settings.ProfileImageCropper

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.lojia.shiftreport.ui.theme.PrimaryIndigo

/**
 * Custom interactive cropper viewport composable.
 * Supports fluid pinch-to-zoom (1.0x to 4.0x) and 2D pan dragging gestures
 * with a high-contrast circular guide overlay.
 */
@Composable
fun CropImageView(
    bitmap: Bitmap,
    zoom: Float,
    onZoomChange: (Float) -> Unit,
    panOffset: Offset,
    onPanChange: (Offset) -> Unit,
    onDimensionsMeasured: (viewportSize: IntSize, cropDiameterPx: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(340.dp)
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, gestureZoom, _ ->
                    val newZoom = (zoom * gestureZoom).coerceIn(1.0f, 4.0f)
                    onZoomChange(newZoom)

                    val maxPanX = 600f * newZoom
                    val maxPanY = 600f * newZoom
                    val newPan = Offset(
                        x = (panOffset.x + pan.x).coerceIn(-maxPanX, maxPanX),
                        y = (panOffset.y + pan.y).coerceIn(-maxPanY, maxPanY)
                    )
                    onPanChange(newPan)
                }
            }
            .onGloballyPositioned { coordinates ->
                val size = coordinates.size
                val diameter = minOf(size.width, size.height) * 0.76f
                onDimensionsMeasured(size, diameter)
            },
        contentAlignment = Alignment.Center
    ) {
        val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }

        // 1. Transforming Image Layer
        Image(
            bitmap = imageBitmap,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = zoom
                    scaleY = zoom
                    translationX = panOffset.x
                    translationY = panOffset.y
                }
        )

        // 2. High-contrast Circular Mask & Guidelines Overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val cropDiameter = minOf(size.width, size.height) * 0.76f
            val radius = cropDiameter / 2f

            val holePath = Path().apply {
                addOval(
                    androidx.compose.ui.geometry.Rect(
                        left = center.x - radius,
                        top = center.y - radius,
                        right = center.x + radius,
                        bottom = center.y + radius
                    )
                )
            }

            // Dim outer region
            clipPath(holePath, clipOp = ClipOp.Difference) {
                drawRect(Color.Black.copy(alpha = 0.68f))
            }

            // Circular boundary border
            drawCircle(
                color = PrimaryIndigo,
                radius = radius,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5.dp.toPx())
            )

            // Inner subtle rule-of-thirds grid lines
            val step = cropDiameter / 3f
            val linePaint = Color.White.copy(alpha = 0.35f)
            val strokeW = 1.dp.toPx()

            clipPath(holePath) {
                // Vertical grid lines
                drawLine(
                    color = linePaint,
                    start = Offset(center.x - radius + step, center.y - radius),
                    end = Offset(center.x - radius + step, center.y + radius),
                    strokeWidth = strokeW
                )
                drawLine(
                    color = linePaint,
                    start = Offset(center.x - radius + (step * 2f), center.y - radius),
                    end = Offset(center.x - radius + (step * 2f), center.y + radius),
                    strokeWidth = strokeW
                )

                // Horizontal grid lines
                drawLine(
                    color = linePaint,
                    start = Offset(center.x - radius, center.y - radius + step),
                    end = Offset(center.x + radius, center.y - radius + step),
                    strokeWidth = strokeW
                )
                drawLine(
                    color = linePaint,
                    start = Offset(center.x - radius, center.y - radius + (step * 2f)),
                    end = Offset(center.x + radius, center.y - radius + (step * 2f)),
                    strokeWidth = strokeW
                )
            }
        }
    }
}
