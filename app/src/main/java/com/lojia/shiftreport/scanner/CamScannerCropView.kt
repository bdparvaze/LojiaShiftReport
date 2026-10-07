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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojia.shiftreport.R
import com.lojia.shiftreport.ui.theme.*
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

private val CamScannerGreen = PosCashGreenFill
private val CamScannerDarkBg = OnBackgroundLight
private val CamScannerSurfaceDark = OnSurfaceVariantLight

/**
 * Custom Magnetic 8-Point Crop Canvas mimicking CamScanner's exact UI and UX.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CamScannerCropView(
    bitmap: Bitmap,
    initialCorners: DocumentCorners?,
    onConfirmCrop: (DocumentCorners) -> Unit,
    onAutoDetect: (() -> Unit)? = null,
    onRotate: (() -> Unit)? = null,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var displaySize by remember { mutableStateOf(Size.Zero) }
    var displayOffset by remember { mutableStateOf(Offset.Zero) }

    var uiTL by remember { mutableStateOf(Offset.Zero) }
    var uiTR by remember { mutableStateOf(Offset.Zero) }
    var uiBR by remember { mutableStateOf(Offset.Zero) }
    var uiBL by remember { mutableStateOf(Offset.Zero) }

    var isInitialized by remember { mutableStateOf(false) }
    var activeHandle by remember { mutableStateOf<HandleType?>(null) }
    var magnifyingPosition by remember { mutableStateOf<Offset?>(null) }

    val density = LocalDensity.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CamScannerDarkBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.doc_crop_adjust_title),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CamScannerDarkBg)
            )

            // Interactive Crop Canvas Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .onGloballyPositioned { layoutCoordinates ->
                        val containerSize = Size(
                            layoutCoordinates.size.width.toFloat(),
                            layoutCoordinates.size.height.toFloat()
                        )
                        if (containerSize.width > 0 && containerSize.height > 0) {
                            val bmpWidth = bitmap.width.toFloat()
                            val bmpHeight = bitmap.height.toFloat()

                            val scale = minOf(
                                containerSize.width / bmpWidth,
                                containerSize.height / bmpHeight
                            )

                            val fitW = bmpWidth * scale
                            val fitH = bmpHeight * scale

                            val offsetX = (containerSize.width - fitW) / 2f
                            val offsetY = (containerSize.height - fitH) / 2f

                            displaySize = Size(fitW, fitH)
                            displayOffset = Offset(offsetX, offsetY)

                            if (!isInitialized) {
                                if (initialCorners != null) {
                                    val scaleX = fitW / bmpWidth
                                    val scaleY = fitH / bmpHeight

                                    uiTL = Offset(offsetX + initialCorners.topLeft.x * scaleX, offsetY + initialCorners.topLeft.y * scaleY)
                                    uiTR = Offset(offsetX + initialCorners.topRight.x * scaleX, offsetY + initialCorners.topRight.y * scaleY)
                                    uiBR = Offset(offsetX + initialCorners.bottomRight.x * scaleX, offsetY + initialCorners.bottomRight.y * scaleY)
                                    uiBL = Offset(offsetX + initialCorners.bottomLeft.x * scaleX, offsetY + initialCorners.bottomLeft.y * scaleY)
                                } else {
                                    val padX = fitW * 0.08f
                                    val padY = fitH * 0.08f
                                    uiTL = Offset(offsetX + padX, offsetY + padY)
                                    uiTR = Offset(offsetX + fitW - padX, offsetY + padY)
                                    uiBR = Offset(offsetX + fitW - padX, offsetY + fitH - padY)
                                    uiBL = Offset(offsetX + padX, offsetY + fitH - padY)
                                }
                                isInitialized = true
                            }
                        }
                    }
            ) {
                if (isInitialized && displaySize.width > 0) {
                    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(displaySize, displayOffset) {
                                detectDragGestures(
                                    onDragStart = { touch ->
                                        val handleRadius = 36.dp.toPx()
                                        activeHandle = when {
                                            hypot(touch.x - uiTL.x, touch.y - uiTL.y) < handleRadius -> HandleType.CORNER_TL
                                            hypot(touch.x - uiTR.x, touch.y - uiTR.y) < handleRadius -> HandleType.CORNER_TR
                                            hypot(touch.x - uiBR.x, touch.y - uiBR.y) < handleRadius -> HandleType.CORNER_BR
                                            hypot(touch.x - uiBL.x, touch.y - uiBL.y) < handleRadius -> HandleType.CORNER_BL
                                            else -> null
                                        }
                                        if (activeHandle != null) {
                                            magnifyingPosition = touch
                                        }
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        activeHandle?.let { handle ->
                                            val current = when (handle) {
                                                HandleType.CORNER_TL -> uiTL
                                                HandleType.CORNER_TR -> uiTR
                                                HandleType.CORNER_BR -> uiBR
                                                HandleType.CORNER_BL -> uiBL
                                                else -> Offset.Zero
                                            }
                                            val nextX = (current.x + dragAmount.x).coerceIn(displayOffset.x, displayOffset.x + displaySize.width)
                                            val nextY = (current.y + dragAmount.y).coerceIn(displayOffset.y, displayOffset.y + displaySize.height)
                                            val newPos = Offset(nextX, nextY)

                                            when (handle) {
                                                HandleType.CORNER_TL -> uiTL = newPos
                                                HandleType.CORNER_TR -> uiTR = newPos
                                                HandleType.CORNER_BR -> uiBR = newPos
                                                HandleType.CORNER_BL -> uiBL = newPos
                                                else -> {}
                                            }
                                            magnifyingPosition = newPos
                                        }
                                    },
                                    onDragEnd = {
                                        activeHandle = null
                                        magnifyingPosition = null
                                    },
                                    onDragCancel = {
                                        activeHandle = null
                                        magnifyingPosition = null
                                    }
                                )
                            }
                    ) {
                        // Draw scaled bitmap
                        drawImage(
                            image = imageBitmap,
                            dstOffset = IntOffset(displayOffset.x.toInt(), displayOffset.y.toInt()),
                            dstSize = IntSize(displaySize.width.toInt(), displaySize.height.toInt())
                        )

                        // Polygon path
                        val polyPath = Path().apply {
                            moveTo(uiTL.x, uiTL.y)
                            lineTo(uiTR.x, uiTR.y)
                            lineTo(uiBR.x, uiBR.y)
                            lineTo(uiBL.x, uiBL.y)
                            close()
                        }

                        // Dark overlay outside polygon
                        clipPath(polyPath, clipOp = androidx.compose.ui.graphics.ClipOp.Difference) {
                            drawRect(
                                color = Color.Black.copy(alpha = 0.55f),
                                topLeft = displayOffset,
                                size = displaySize
                            )
                        }

                        // Quad lines
                        drawPath(
                            path = polyPath,
                            color = CamScannerGreen,
                            style = Stroke(width = 2.5.dp.toPx())
                        )

                        // Handles
                        val handles = listOf(
                            uiTL to HandleType.CORNER_TL,
                            uiTR to HandleType.CORNER_TR,
                            uiBR to HandleType.CORNER_BR,
                            uiBL to HandleType.CORNER_BL
                        )

                        for ((pos, type) in handles) {
                            val isCurrent = activeHandle == type
                            val radius = if (isCurrent) 16.dp.toPx() else 12.dp.toPx()

                            drawCircle(color = Color.White, radius = radius, center = pos)
                            drawCircle(
                                color = if (isCurrent) PosCashGreen else CamScannerGreen,
                                radius = radius - 3.5.dp.toPx(),
                                center = pos
                            )
                        }
                    }
                }
            }

            // Bottom Action Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CamScannerDarkBg)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Auto Detect Button
                OutlinedButton(
                    onClick = {
                        if (onAutoDetect != null) {
                            onAutoDetect()
                        } else {
                            val padX = displaySize.width * 0.05f
                            val padY = displaySize.height * 0.05f
                            uiTL = Offset(displayOffset.x + padX, displayOffset.y + padY)
                            uiTR = Offset(displayOffset.x + displaySize.width - padX, displayOffset.y + padY)
                            uiBR = Offset(displayOffset.x + displaySize.width - padX, displayOffset.y + displaySize.height - padY)
                            uiBL = Offset(displayOffset.x + padX, displayOffset.y + displaySize.height - padY)
                        }
                    },
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
                    Text(stringResource(R.string.doc_btn_auto_detect), fontSize = 13.sp, color = Color.White)
                }

                // Full Screen / Select All
                TextButton(
                    onClick = {
                        uiTL = displayOffset
                        uiTR = Offset(displayOffset.x + displaySize.width, displayOffset.y)
                        uiBR = Offset(displayOffset.x + displaySize.width, displayOffset.y + displaySize.height)
                        uiBL = Offset(displayOffset.x, displayOffset.y + displaySize.height)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = OutlineLight)
                ) {
                    Text(stringResource(R.string.doc_select_all), fontSize = 13.sp)
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
                    Text(stringResource(R.string.btn_next), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
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
