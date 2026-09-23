package com.lojia.shiftreport.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors

private val CamScannerGreen = Color(0xFF10B981)
private val CamScannerDark = Color(0xFF000000)
private val CamScannerControlBg = Color(0xCC090D16)

enum class ScanCaptureMode {
    SINGLE,
    BATCH
}

enum class TorchState {
    OFF,
    ON,
    AUTO
}

/**
 * 100% Visual Clone of CamScanner's Custom Camera Capture Screen.
 * Uses CameraX with high-resolution document capture, A4 alignment reticle,
 * transparent top control bar (Flash, Auto-Detect, Grid), "Single" vs "Batch" text toggle,
 * and a prominent circular shutter button.
 */
@Composable
fun CameraXScannerView(
    onImageCaptured: (Bitmap) -> Unit,
    onBatchCaptured: (List<Bitmap>) -> Unit = { list -> if (list.isNotEmpty()) onImageCaptured(list.first()) },
    onGalleryPick: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val view = LocalView.current

    var captureMode by remember { mutableStateOf(ScanCaptureMode.SINGLE) }
    var torchState by remember { mutableStateOf(TorchState.OFF) }
    var isAutoDetectEnabled by remember { mutableStateOf(true) }
    var showGrid by remember { mutableStateOf(false) }

    var isCapturing by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var cameraProviderRef by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    // Batch captured pictures list
    val batchList = remember { mutableStateListOf<Bitmap>() }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setTargetRotation(android.view.Surface.ROTATION_0)
            .build()
    }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            try {
                cameraProviderRef?.unbindAll()
            } catch (e: Exception) {
                Log.e("CamScanner", "Error unbinding camera on dispose", e)
            }
            cameraExecutor.shutdown()
        }
    }

    // Shutter button press animation scale
    val shutterScale by animateFloatAsState(
        targetValue = if (isCapturing) 0.88f else 1f,
        animationSpec = spring(),
        label = "shutterScale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CamScannerDark)
    ) {
        // CameraX Live Preview with TextureView implementation for rock-solid stability
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        cameraProviderRef = cameraProvider
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                        cameraProvider.unbindAll()
                        camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCapture
                        )

                        // Set initial torch
                        camera?.cameraControl?.enableTorch(torchState == TorchState.ON)
                    } catch (e: Exception) {
                        Log.e("CamScanner", "Camera initialization failed", e)
                    }
                }, ctx.mainExecutor)

                previewView
            },
            onRelease = {
                try {
                    cameraProviderRef?.unbindAll()
                } catch (e: Exception) {
                    Log.e("CamScanner", "Error releasing camera provider", e)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Viewfinder Guide: Reticle + Corner Brackets + Optional Grid
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height

            // Standard document aspect ratio A4: ~1:1.414
            val targetW = canvasW * 0.84f
            val targetH = (targetW * 1.414f).coerceAtMost(canvasH * 0.65f)
            val left = (canvasW - targetW) / 2f
            val top = (canvasH - targetH) / 2.3f

            // Optional 3x3 Rule-of-Thirds Grid
            if (showGrid) {
                val stepX = targetW / 3f
                val stepY = targetH / 3f
                for (i in 1..2) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.22f),
                        start = Offset(left + i * stepX, top),
                        end = Offset(left + i * stepX, top + targetH),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.22f),
                        start = Offset(left, top + i * stepY),
                        end = Offset(left + targetW, top + i * stepY),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }

            // Outer Document Frame
            drawRoundRect(
                color = if (isAutoDetectEnabled) CamScannerGreen.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.6f),
                topLeft = Offset(left, top),
                size = Size(targetW, targetH),
                cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx()),
                style = Stroke(width = 1.8.dp.toPx())
            )

            // High-Contrast CamScanner Corner Brackets
            val bracketLength = 26.dp.toPx()
            val bracketStroke = 4.dp.toPx()
            val bracketColor = if (isAutoDetectEnabled) CamScannerGreen else Color.White

            // Top-Left
            drawLine(bracketColor, Offset(left, top), Offset(left + bracketLength, top), bracketStroke)
            drawLine(bracketColor, Offset(left, top), Offset(left, top + bracketLength), bracketStroke)
            // Top-Right
            drawLine(bracketColor, Offset(left + targetW, top), Offset(left + targetW - bracketLength, top), bracketStroke)
            drawLine(bracketColor, Offset(left + targetW, top), Offset(left + targetW, top + bracketLength), bracketStroke)
            // Bottom-Left
            drawLine(bracketColor, Offset(left, top + targetH), Offset(left + bracketLength, top + targetH), bracketStroke)
            drawLine(bracketColor, Offset(left, top + targetH), Offset(left, top + targetH - bracketLength), bracketStroke)
            // Bottom-Right
            drawLine(bracketColor, Offset(left + targetW, top + targetH), Offset(left + targetW - bracketLength, top + targetH), bracketStroke)
            drawLine(bracketColor, Offset(left + targetW, top + targetH), Offset(left + targetW, top + targetH - bracketLength), bracketStroke)
        }

        // ==============================================================
        // 1. TOP TRANSPARENT BAR
        // Icons for: Close, Flash Control, Auto-Detect toggle, Settings (Grid)
        // ==============================================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Close / Cancel
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.Black.copy(alpha = 0.45f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Flash / Torch Toggle
                IconButton(
                    onClick = {
                        val next = when (torchState) {
                            TorchState.OFF -> TorchState.ON
                            TorchState.ON -> TorchState.OFF
                            TorchState.AUTO -> TorchState.OFF
                        }
                        torchState = next
                        camera?.cameraControl?.enableTorch(next == TorchState.ON)
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (torchState == TorchState.ON) Icons.Outlined.FlashOn else Icons.Outlined.FlashOff,
                        contentDescription = "Flash",
                        tint = if (torchState == TorchState.ON) Color(0xFFFBBF24) else Color.White
                    )
                }

                // Auto-Detect Toggle
                Surface(
                    onClick = { isAutoDetectEnabled = !isAutoDetectEnabled },
                    color = if (isAutoDetectEnabled) CamScannerGreen.copy(alpha = 0.9f) else Color.Black.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.height(38.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CropFree,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isAutoDetectEnabled) "Auto Crop ON" else "Auto Crop OFF",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Grid Toggle (Settings)
                IconButton(
                    onClick = { showGrid = !showGrid },
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (showGrid) Icons.Outlined.GridOn else Icons.Outlined.GridOff,
                        contentDescription = "Grid",
                        tint = if (showGrid) CamScannerGreen else Color.White
                    )
                }
            }
        }

        // ==============================================================
        // 2. BOTTOM CONTROL BAR
        // Text toggles for "Single" and "Batch" placed just above the shutter,
        // Prominent circular shutter button, Gallery import, and Batch badge
        // ==============================================================
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(CamScannerControlBg)
                .navigationBarsPadding()
                .padding(bottom = 20.dp, top = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Mode Selector: "Single" vs "Batch" Scan Modes
            Row(
                modifier = Modifier
                    .padding(bottom = 18.dp)
                    .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Single Mode Tab
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (captureMode == ScanCaptureMode.SINGLE) CamScannerGreen else Color.Transparent)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { captureMode = ScanCaptureMode.SINGLE }
                        .padding(horizontal = 18.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Single",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = if (captureMode == ScanCaptureMode.SINGLE) FontWeight.Bold else FontWeight.Normal
                    )
                }

                // Batch Mode Tab
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (captureMode == ScanCaptureMode.BATCH) CamScannerGreen else Color.Transparent)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { captureMode = ScanCaptureMode.BATCH }
                        .padding(horizontal = 18.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Batch",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = if (captureMode == ScanCaptureMode.BATCH) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

            // Bottom Shutter Controls Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Gallery Import Button
                IconButton(
                    onClick = onGalleryPick,
                    modifier = Modifier
                        .size(52.dp)
                        .background(Color.White.copy(alpha = 0.12f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PhotoLibrary,
                        contentDescription = "Gallery Import",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Center: CamScanner Prominent Circular Shutter Button
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .scale(shutterScale)
                        .shadow(12.dp, CircleShape)
                        .border(4.dp, CamScannerGreen, CircleShape)
                        .background(Color.Transparent, CircleShape)
                        .clickable(enabled = !isCapturing) {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            isCapturing = true

                            takePhoto(
                                context = context,
                                imageCapture = imageCapture,
                                executor = cameraExecutor
                            ) { bitmap ->
                                isCapturing = false
                                if (bitmap != null) {
                                    if (captureMode == ScanCaptureMode.SINGLE) {
                                        onImageCaptured(bitmap)
                                    } else {
                                        batchList.add(bitmap)
                                        Toast.makeText(
                                            context,
                                            "Page ${batchList.size} captured",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Failed to capture photo", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Inner shutter circle
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color.White, CircleShape)
                    )
                }

                // Right: Batch Finished / Counter Button (Or Info indicator in Single mode)
                if (captureMode == ScanCaptureMode.BATCH) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(if (batchList.isNotEmpty()) CamScannerGreen else Color.White.copy(alpha = 0.12f))
                            .clickable(enabled = batchList.isNotEmpty()) {
                                onBatchCaptured(batchList.toList())
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (batchList.isNotEmpty()) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${batchList.size}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Done",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Layers,
                                contentDescription = "Batch Mode",
                                tint = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                } else {
                    // Placeholder for visual symmetry
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color.Transparent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Auto",
                            color = CamScannerGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private fun takePhoto(
    context: Context,
    imageCapture: ImageCapture,
    executor: java.util.concurrent.Executor,
    onResult: (Bitmap?) -> Unit
) {
    val outputStream = ByteArrayOutputStream()
    val outputOptions = ImageCapture.OutputFileOptions.Builder(outputStream).build()

    imageCapture.takePicture(
        outputOptions,
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                val bytes = outputStream.toByteArray()
                var bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

                // Rotate portrait if necessary
                if (bmp != null && bmp.width > bmp.height) {
                    val matrix = Matrix().apply { postRotate(90f) }
                    bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
                }

                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    onResult(bmp)
                }
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("CamScanner", "Photo capture failed: ${exception.message}", exception)
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    onResult(null)
                }
            }
        }
    )
}
