package com.lojia.shiftreport.scanner

import android.graphics.Bitmap
import android.graphics.PointF
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.hypot

enum class HandleType {
    CORNER_TL,
    CORNER_TR,
    CORNER_BR,
    CORNER_BL,
    MID_TOP,
    MID_RIGHT,
    MID_BOTTOM,
    MID_LEFT
}

private val CamScannerGreen = Color(0xFF10B981)
private val CamScannerDarkBg = Color(0xFF0B132B)
private val CamScannerSurfaceDark = Color(0xFF1C2541)

/**
 * Custom Magnetic 8-Point Crop Canvas mimicking CamScanner's exact UI and UX.
 * Features 4 corner handles + 4 mid-edge handles and a precision Top-Corner Magnifying Glass (Loupe).
 */
@Composable
fun CamScannerCropView(
    bitmap: Bitmap,
    initialCorners: DocumentCorners,
    onConfirmCrop: (DocumentCorners) -> Unit,
    onAutoDetect: () -> Unit,
    onRotate: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    var containerWidth by remember { mutableFloatStateOf(0f) }
    var containerHeight by remember { mutableFloatStateOf(0f) }

    val imgRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
    val displaySize by remember(containerWidth, containerHeight, imgRatio) {
        derivedStateOf {
            if (containerWidth <= 0f || containerHeight <= 0f) {
                Size.Zero
            } else {
                val containerRatio = containerWidth / containerHeight
                if (imgRatio > containerRatio) {
                    val w = containerWidth
                    val h = containerWidth / imgRatio
                    Size(w, h)
                } else {
                    val h = containerHeight
                    val w = containerHeight * imgRatio
                    Size(w, h)
                }
            }
        }
    }

    val displayOffset by remember(containerWidth, containerHeight, displaySize) {
        derivedStateOf {
            Offset(
                (containerWidth - displaySize.width) / 2f,
                (containerHeight - displaySize.height) / 2f
            )
        }
    }

    // 4 Corner Positions in UI Display Space
    var uiTL by remember { mutableStateOf(Offset.Zero) }
    var uiTR by remember { mutableStateOf(Offset.Zero) }
    var uiBR by remember { mutableStateOf(Offset.Zero) }
    var uiBL by remember { mutableStateOf(Offset.Zero) }

    // Map initial corners from Bitmap coordinate space to UI space
    LaunchedEffect(initialCorners, displaySize, displayOffset) {
        if (displaySize.width > 0f && displaySize.height > 0f) {
            val scaleX = displaySize.width / bitmap.width.toFloat()
            val scaleY = displaySize.height / bitmap.height.toFloat()

            uiTL = Offset(
                displayOffset.x + initialCorners.topLeft.x * scaleX,
                displayOffset.y + initialCorners.topLeft.y * scaleY
            )
            uiTR = Offset(
                displayOffset.x + initialCorners.topRight.x * scaleX,
                displayOffset.y + initialCorners.topRight.y * scaleY
            )
            uiBR = Offset(
                displayOffset.x + initialCorners.bottomRight.x * scaleX,
                displayOffset.y + initialCorners.bottomRight.y * scaleY
            )
            uiBL = Offset(
                displayOffset.x + initialCorners.bottomLeft.x * scaleX,
                displayOffset.y + initialCorners.bottomLeft.y * scaleY
            )
        }
    }

    // Active drag handle & active touch location for the top-corner magnifying loupe
    var activeHandle by remember { mutableStateOf<HandleType?>(null) }
    var activeTouchOffset by remember { mutableStateOf<Offset?>(null) }

    val handleTouchRadiusPx = with(density) { 38.dp.toPx() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CamScannerDarkBg)
    ) {
        // Top Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onCancel) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Text(
                text = "Crop & Magnetic Adjust",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 17.sp
                )
            )

            IconButton(onClick = onRotate) {
                Icon(
                    imageVector = Icons.Outlined.Rotate90DegreesCw,
                    contentDescription = "Rotate",
                    tint = Color.White
                )
            }
        }

        // Main Crop Viewport with 8 draggable handles & Loupe
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .onGloballyPositioned { coordinates ->
                    containerWidth = coordinates.size.width.toFloat()
                    containerHeight = coordinates.size.height.toFloat()
                }
                .pointerInput(displaySize, displayOffset) {
                    detectDragGestures(
                        onDragStart = { startOffset ->
                            activeTouchOffset = startOffset

                            val midT = Offset((uiTL.x + uiTR.x) / 2f, (uiTL.y + uiTR.y) / 2f)
                            val midR = Offset((uiTR.x + uiBR.x) / 2f, (uiTR.y + uiBR.y) / 2f)
                            val midB = Offset((uiBR.x + uiBL.x) / 2f, (uiBR.y + uiBL.y) / 2f)
                            val midL = Offset((uiBL.x + uiTL.x) / 2f, (uiBL.y + uiTL.y) / 2f)

                            val distTL = hypot((startOffset.x - uiTL.x).toDouble(), (startOffset.y - uiTL.y).toDouble())
                            val distTR = hypot((startOffset.x - uiTR.x).toDouble(), (startOffset.y - uiTR.y).toDouble())
                            val distBR = hypot((startOffset.x - uiBR.x).toDouble(), (startOffset.y - uiBR.y).toDouble())
                            val distBL = hypot((startOffset.x - uiBL.x).toDouble(), (startOffset.y - uiBL.y).toDouble())

                            val distMidT = hypot((startOffset.x - midT.x).toDouble(), (startOffset.y - midT.y).toDouble())
                            val distMidR = hypot((startOffset.x - midR.x).toDouble(), (startOffset.y - midR.y).toDouble())
                            val distMidB = hypot((startOffset.x - midB.x).toDouble(), (startOffset.y - midB.y).toDouble())
                            val distMidL = hypot((startOffset.x - midL.x).toDouble(), (startOffset.y - midL.y).toDouble())

                            val handleDistances = listOf(
                                distTL to HandleType.CORNER_TL,
                                distTR to HandleType.CORNER_TR,
                                distBR to HandleType.CORNER_BR,
                                distBL to HandleType.CORNER_BL,
                                distMidT to HandleType.MID_TOP,
                                distMidR to HandleType.MID_RIGHT,
                                distMidB to HandleType.MID_BOTTOM,
                                distMidL to HandleType.MID_LEFT
                            )

                            val closest = handleDistances.minByOrNull { it.first }
                            if (closest != null && closest.first <= handleTouchRadiusPx * 1.6f) {
                                activeHandle = closest.second
                            }
                        },
                        onDragEnd = {
                            activeHandle = null
                            activeTouchOffset = null
                        },
                        onDragCancel = {
                            activeHandle = null
                            activeTouchOffset = null
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            activeTouchOffset = change.position

                            val minX = displayOffset.x
                            val maxX = displayOffset.x + displaySize.width
                            val minY = displayOffset.y
                            val maxY = displayOffset.y + displaySize.height

                            when (activeHandle) {
                                HandleType.CORNER_TL -> {
                                    uiTL = Offset(
                                        (uiTL.x + dragAmount.x).coerceIn(minX, maxX),
                                        (uiTL.y + dragAmount.y).coerceIn(minY, maxY)
                                    )
                                }
                                HandleType.CORNER_TR -> {
                                    uiTR = Offset(
                                        (uiTR.x + dragAmount.x).coerceIn(minX, maxX),
                                        (uiTR.y + dragAmount.y).coerceIn(minY, maxY)
                                    )
                                }
                                HandleType.CORNER_BR -> {
                                    uiBR = Offset(
                                        (uiBR.x + dragAmount.x).coerceIn(minX, maxX),
                                        (uiBR.y + dragAmount.y).coerceIn(minY, maxY)
                                    )
                                }
                                HandleType.CORNER_BL -> {
                                    uiBL = Offset(
                                        (uiBL.x + dragAmount.x).coerceIn(minX, maxX),
                                        (uiBL.y + dragAmount.y).coerceIn(minY, maxY)
                                    )
                                }
                                // Mid-Edge Magnetic Adjustments: shifts both edge vertices
                                HandleType.MID_TOP -> {
                                    val newTL = (uiTL.y + dragAmount.y).coerceIn(minY, maxY)
                                    val newTR = (uiTR.y + dragAmount.y).coerceIn(minY, maxY)
                                    uiTL = Offset(uiTL.x, newTL)
                                    uiTR = Offset(uiTR.x, newTR)
                                }
                                HandleType.MID_RIGHT -> {
                                    val newTR = (uiTR.x + dragAmount.x).coerceIn(minX, maxX)
                                    val newBR = (uiBR.x + dragAmount.x).coerceIn(minX, maxX)
                                    uiTR = Offset(newTR, uiTR.y)
                                    uiBR = Offset(newBR, uiBR.y)
                                }
                                HandleType.MID_BOTTOM -> {
                                    val newBL = (uiBL.y + dragAmount.y).coerceIn(minY, maxY)
                                    val newBR = (uiBR.y + dragAmount.y).coerceIn(minY, maxY)
                                    uiBL = Offset(uiBL.x, newBL)
                                    uiBR = Offset(uiBR.x, newBR)
                                }
                                HandleType.MID_LEFT -> {
                                    val newTL = (uiTL.x + dragAmount.x).coerceIn(minX, maxX)
                                    val newBL = (uiBL.x + dragAmount.x).coerceIn(minX, maxX)
                                    uiTL = Offset(newTL, uiTL.y)
                                    uiBL = Offset(newBL, uiBL.y)
                                }
                                null -> Unit
                            }
                        }
                    )
                }
        ) {
            val composeImageBitmap = remember(bitmap) { bitmap.asImageBitmap() }

            Canvas(modifier = Modifier.fillMaxSize()) {
                if (displaySize.width > 0 && displaySize.height > 0) {
                    // 1. Draw source photo
                    drawImage(
                        image = composeImageBitmap,
                        dstOffset = IntOffset(displayOffset.x.toInt(), displayOffset.y.toInt()),
                        dstSize = IntSize(displaySize.width.toInt(), displaySize.height.toInt())
                    )

                    // 2. Crop polygon path connecting 4 corners
                    val cropPath = Path().apply {
                        moveTo(uiTL.x, uiTL.y)
                        lineTo(uiTR.x, uiTR.y)
                        lineTo(uiBR.x, uiBR.y)
                        lineTo(uiBL.x, uiBL.y)
                        close()
                    }

                    // 3. Darkened background outside crop boundary
                    clipPath(path = cropPath, clipOp = androidx.compose.ui.graphics.ClipOp.Difference) {
                        drawRect(
                            color = Color.Black.copy(alpha = 0.58f),
                            topLeft = displayOffset,
                            size = displaySize
                        )
                    }

                    // 4. Boundary border lines
                    drawPath(
                        path = cropPath,
                        color = CamScannerGreen,
                        style = Stroke(width = 2.5.dp.toPx())
                    )

                    // 5. Four Corner Handles (CamScanner style with outer white & inner teal circle)
                    val cornerHandles = listOf(
                        uiTL to HandleType.CORNER_TL,
                        uiTR to HandleType.CORNER_TR,
                        uiBR to HandleType.CORNER_BR,
                        uiBL to HandleType.CORNER_BL
                    )
                    cornerHandles.forEach { (pos, type) ->
                        val isCurrent = activeHandle == type
                        val radius = if (isCurrent) 17.dp.toPx() else 13.dp.toPx()

                        // Outer ring
                        drawCircle(color = Color.White, radius = radius, center = pos)
                        // Inner colored dot
                        drawCircle(
                            color = if (isCurrent) Color(0xFF047857) else CamScannerGreen,
                            radius = radius - 3.5.dp.toPx(),
                            center = pos
                        )
                    }

                    // 6. Four Mid-Edge Handles (pill / capsule style)
                    val midT = Offset((uiTL.x + uiTR.x) / 2f, (uiTL.y + uiTR.y) / 2f)
                    val midR = Offset((uiTR.x + uiBR.x) / 2f, (uiTR.y + uiBR.y) / 2f)
                    val midB = Offset((uiBR.x + uiBL.x) / 2f, (uiBR.y + uiBL.y) / 2f)
                    val midL = Offset((uiBL.x + uiTL.x) / 2f, (uiBL.y + uiTL.y) / 2f)

                    val midHandles = listOf(
                        midT to HandleType.MID_TOP,
                        midR to HandleType.MID_RIGHT,
                        midB to HandleType.MID_BOTTOM,
                        midL to HandleType.MID_LEFT
                    )
                    midHandles.forEach { (pos, type) ->
                        val isCurrent = activeHandle == type
                        val r = if (isCurrent) 8.dp.toPx() else 6.dp.toPx()
                        drawCircle(color = Color.White, radius = r, center = pos)
                        drawCircle(color = CamScannerGreen, radius = r - 2.dp.toPx(), center = pos)
                    }
                }
            }

            // ==============================================================
            // 🔍 TOP-CORNER MAGNIFYING GLASS (LOUPE)
            // As requested by user: "Loupe effect that appears in the top corner
            // whenever the user drags a crop handle"
            // ==============================================================
            if (activeHandle != null) {
                // Determine targeted point for loupe inspection
                val inspectionPoint = when (activeHandle) {
                    HandleType.CORNER_TL -> uiTL
                    HandleType.CORNER_TR -> uiTR
                    HandleType.CORNER_BR -> uiBR
                    HandleType.CORNER_BL -> uiBL
                    HandleType.MID_TOP -> Offset((uiTL.x + uiTR.x) / 2f, (uiTL.y + uiTR.y) / 2f)
                    HandleType.MID_RIGHT -> Offset((uiTR.x + uiBR.x) / 2f, (uiTR.y + uiBR.y) / 2f)
                    HandleType.MID_BOTTOM -> Offset((uiBR.x + uiBL.x) / 2f, (uiBR.y + uiBL.y) / 2f)
                    HandleType.MID_LEFT -> Offset((uiBL.x + uiTL.x) / 2f, (uiBL.y + uiTL.y) / 2f)
                    null -> Offset.Zero
                }

                // If dragging on the top-right side, show loupe in top-left to avoid finger occlusion; vice versa.
                val showOnLeft = inspectionPoint.x > containerWidth / 2f
                val loupeSizeDp = 120.dp
                val loupeSizePx = with(density) { loupeSizeDp.toPx() }

                val scaleX = bitmap.width.toFloat() / displaySize.width
                val scaleY = bitmap.height.toFloat() / displaySize.height
                val bmpX = ((inspectionPoint.x - displayOffset.x) * scaleX).coerceIn(0f, bitmap.width.toFloat())
                val bmpY = ((inspectionPoint.y - displayOffset.y) * scaleY).coerceIn(0f, bitmap.height.toFloat())

                Box(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(if (showOnLeft) Alignment.TopStart else Alignment.TopEnd)
                        .size(loupeSizeDp)
                        .shadow(16.dp, CircleShape)
                        .clip(CircleShape)
                        .border(3.dp, CamScannerGreen, CircleShape)
                        .background(Color.Black)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val zoomFactor = 2.8f
                        val zoomedDstWidth = displaySize.width * zoomFactor
                        val zoomedDstHeight = displaySize.height * zoomFactor

                        val zoomOffsetX = (loupeSizePx / 2f) - (bmpX / bitmap.width.toFloat()) * zoomedDstWidth
                        val zoomOffsetY = (loupeSizePx / 2f) - (bmpY / bitmap.height.toFloat()) * zoomedDstHeight

                        drawImage(
                            image = composeImageBitmap,
                            dstOffset = IntOffset(zoomOffsetX.toInt(), zoomOffsetY.toInt()),
                            dstSize = IntSize(zoomedDstWidth.toInt(), zoomedDstHeight.toInt())
                        )

                        // Crosshairs for exact pixel-perfect corner alignment
                        val center = Offset(loupeSizePx / 2f, loupeSizePx / 2f)
                        drawLine(
                            color = CamScannerGreen,
                            start = Offset(center.x - 18.dp.toPx(), center.y),
                            end = Offset(center.x + 18.dp.toPx(), center.y),
                            strokeWidth = 2.dp.toPx()
                        )
                        drawLine(
                            color = CamScannerGreen,
                            start = Offset(center.x, center.y - 18.dp.toPx()),
                            end = Offset(center.x, center.y + 18.dp.toPx()),
                            strokeWidth = 2.dp.toPx()
                        )

                        // Center indicator circle
                        drawCircle(
                            color = CamScannerGreen,
                            radius = 3.dp.toPx(),
                            center = center
                        )
                    }
                }
            }
        }

        // Bottom Controls Bar (CamScanner styled)
        Surface(
            color = CamScannerSurfaceDark,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Auto Detect Button
                OutlinedButton(
                    onClick = onAutoDetect,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Color.White.copy(alpha = 0.3f))),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = CamScannerGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Auto Detect", fontSize = 13.sp, color = Color.White)
                }

                // Full Screen / Select All
                TextButton(
                    onClick = {
                        uiTL = displayOffset
                        uiTR = Offset(displayOffset.x + displaySize.width, displayOffset.y)
                        uiBR = Offset(displayOffset.x + displaySize.width, displayOffset.y + displaySize.height)
                        uiBL = Offset(displayOffset.x, displayOffset.y + displaySize.height)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFCBD5E1))
                ) {
                    Text("Select All", fontSize = 13.sp)
                }

                // Next / Confirm Button
                Button(
                    onClick = {
                        val scaleX = bitmap.width.toFloat() / displaySize.width
                        val scaleY = bitmap.height.toFloat() / displaySize.height

                        val finalCorners = DocumentCorners(
                            topLeft = PointF(
                                ((uiTL.x - displayOffset.x) * scaleX).coerceIn(0f, bitmap.width.toFloat()),
                                ((uiTL.y - displayOffset.y) * scaleY).coerceIn(0f, bitmap.height.toFloat())
                            ),
                            topRight = PointF(
                                ((uiTR.x - displayOffset.x) * scaleX).coerceIn(0f, bitmap.width.toFloat()),
                                ((uiTR.y - displayOffset.y) * scaleY).coerceIn(0f, bitmap.height.toFloat())
                            ),
                            bottomRight = PointF(
                                ((uiBR.x - displayOffset.x) * scaleX).coerceIn(0f, bitmap.width.toFloat()),
                                ((uiBR.y - displayOffset.y) * scaleY).coerceIn(0f, bitmap.height.toFloat())
                            ),
                            bottomLeft = PointF(
                                ((uiBL.x - displayOffset.x) * scaleX).coerceIn(0f, bitmap.width.toFloat()),
                                ((uiBL.y - displayOffset.y) * scaleY).coerceIn(0f, bitmap.height.toFloat())
                            )
                        )
                        onConfirmCrop(finalCorners)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CamScannerGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Next", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
