package com.lojia.shiftreport.scanner

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.lojia.shiftreport.ui.common.LojiaTextField
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

private val CamScannerGreen = Color(0xFF10B981)
private val CamScannerEditorBg = Color(0xFF0F172A)
private val CamScannerCardBg = Color(0xFF1E293B)

enum class EditorStep {
    CROP_BOUNDARIES,
    FILTER_AND_ADJUST
}

/**
 * 100% Visual Clone of CamScanner's Filter & Enhancement Screen.
 * Features:
 * - Horizontally scrollable row containing filter thumbnails (Original, Lighten, Magic Color, Gray Mode, B&W)
 * - Intuitive top bar with "Back" icon and prominent "Save/Done" checkmark
 * - Manual Brightness, Contrast, and Detail adjustment sliders
 * - Built-in OCR text extraction and PDF generation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CamScannerEditorScreen(
    initialBitmap: Bitmap,
    viewModel: DocumentScannerViewModel,
    onFinish: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var currentStep by remember { mutableStateOf(EditorStep.CROP_BOUNDARIES) }
    var workingBitmap by remember { mutableStateOf(initialBitmap) }
    var initialCorners by remember { mutableStateOf<DocumentCorners?>(null) }
    var croppedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Filter & Adjustments State
    var selectedFilter by remember { mutableStateOf(ScannerFilterType.MAGIC_COLOR) }
    var adjustments by remember { mutableStateOf(FilterAdjustments()) }
    var showAdjustSliders by remember { mutableStateOf(false) }
    var isFilterProcessing by remember { mutableStateOf(false) }
    var displayBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Filter mini-thumbnail cache
    val miniThumbnails = remember { mutableStateMapOf<ScannerFilterType, Bitmap>() }

    // Document Title & Saving
    val defaultTitle = remember {
        "Doc_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    }
    var documentTitle by remember { mutableStateOf(defaultTitle) }
    var isEditingTitle by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    // OCR State
    var showOcrSheet by remember { mutableStateOf(false) }
    var isOcrRunning by remember { mutableStateOf(false) }
    var ocrExtractedText by remember { mutableStateOf("") }

    // Auto-detect corners on initial load
    LaunchedEffect(workingBitmap) {
        val detected = PerspectiveTransformHelper.detectDocumentCorners(workingBitmap)
        initialCorners = detected
    }

    // Function to re-apply filter and adjustments
    fun updatePreview(base: Bitmap, filter: ScannerFilterType, adj: FilterAdjustments) {
        coroutineScope.launch {
            isFilterProcessing = true
            val result = DocumentImageFilter.applyFilter(base, filter, adj)
            displayBitmap = result
            isFilterProcessing = false
        }
    }

    when (currentStep) {
        // ==============================================================
        // 📐 SCREEN 2: CUSTOM MAGNETIC 8-POINT CROP CANVAS
        // ==============================================================
        EditorStep.CROP_BOUNDARIES -> {
            val corners = initialCorners ?: DocumentCorners.defaultForDimensions(
                workingBitmap.width.toFloat(),
                workingBitmap.height.toFloat()
            )

            CamScannerCropView(
                bitmap = workingBitmap,
                initialCorners = corners,
                onConfirmCrop = { confirmedCorners ->
                    coroutineScope.launch {
                        val cropped = PerspectiveTransformHelper.cropAndStraighten(
                            workingBitmap,
                            confirmedCorners
                        )
                        croppedBitmap = cropped

                        // Generate small mini-thumbnails for filter preview bar
                        val miniW = 120
                        val miniH = (miniW * (cropped.height.toFloat() / cropped.width.toFloat())).toInt().coerceIn(80, 160)
                        val miniBase = Bitmap.createScaledBitmap(cropped, miniW, miniH, true)

                        ScannerFilterType.entries.forEach { f ->
                            val thumb = DocumentImageFilter.applyFilter(miniBase, f)
                            miniThumbnails[f] = thumb
                        }

                        // Apply default Magic Color filter
                        updatePreview(cropped, selectedFilter, adjustments)
                        currentStep = EditorStep.FILTER_AND_ADJUST
                    }
                },
                onAutoDetect = {
                    coroutineScope.launch {
                        val detected = PerspectiveTransformHelper.detectDocumentCorners(workingBitmap)
                        initialCorners = detected
                    }
                },
                onRotate = {
                    val rotated = PerspectiveTransformHelper.rotateBitmap(workingBitmap, 90f)
                    workingBitmap = rotated
                },
                onCancel = onCancel,
                modifier = modifier
            )
        }

        // ==============================================================
        // ✨ SCREEN 3: FILTER & ENHANCEMENT SCREEN
        // ==============================================================
        EditorStep.FILTER_AND_ADJUST -> {
            Scaffold(
                containerColor = CamScannerEditorBg,
                topBar = {
                    Surface(
                        color = CamScannerCardBg,
                        shadowElevation = 6.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Back Icon
                            IconButton(onClick = { currentStep = EditorStep.CROP_BOUNDARIES }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                    contentDescription = "Back to Crop",
                                    tint = Color.White
                                )
                            }

                            // Document Title (Editable)
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { isEditingTitle = true }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = documentTitle,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = "Rename",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            // OCR Button
                            IconButton(
                                onClick = {
                                    val bmp = displayBitmap ?: croppedBitmap ?: return@IconButton
                                    isOcrRunning = true
                                    showOcrSheet = true
                                    runOcr(bmp) { text ->
                                        isOcrRunning = false
                                        ocrExtractedText = text
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.TextFields,
                                    contentDescription = "OCR Text",
                                    tint = Color.White
                                )
                            }

                            // Prominent Save / Done Checkmark Button
                            Button(
                                onClick = {
                                    val finalBmp = displayBitmap ?: croppedBitmap ?: return@Button
                                    isSaving = true
                                    viewModel.saveBitmapAsScannedDocument(
                                        bitmap = finalBmp,
                                        customTitle = documentTitle,
                                        preExtractedOcr = if (ocrExtractedText.isNotBlank()) ocrExtractedText else null
                                    ) {
                                        isSaving = false
                                        Toast.makeText(context, "Document saved as PDF!", Toast.LENGTH_SHORT).show()
                                        onFinish()
                                    }
                                },
                                enabled = !isSaving,
                                colors = ButtonDefaults.buttonColors(containerColor = CamScannerGreen),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                if (isSaving) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = "Save",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Done",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                },
                bottomBar = {
                    Surface(
                        color = CamScannerCardBg,
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                        shadowElevation = 12.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(vertical = 12.dp)
                        ) {
                            // Section Header with Adjust Toggle
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (showAdjustSliders) "Manual Fine-Tuning" else "Enhancement Filters",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF94A3B8)
                                )

                                // Sliders Toggle Button
                                TextButton(
                                    onClick = { showAdjustSliders = !showAdjustSliders },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = if (showAdjustSliders) CamScannerGreen else Color.White
                                    ),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Tune,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (showAdjustSliders) "Filters" else "Adjust",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // 1. Manual Tuning Sliders (Brightness, Contrast, Detail)
                            AnimatedVisibility(
                                visible = showAdjustSliders,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 8.dp)
                                ) {
                                    // Brightness Slider
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "Brightness",
                                            fontSize = 12.sp,
                                            color = Color.White,
                                            modifier = Modifier.width(76.dp)
                                        )
                                        Slider(
                                            value = adjustments.brightness,
                                            onValueChange = { newVal ->
                                                adjustments = adjustments.copy(brightness = newVal)
                                                val base = croppedBitmap ?: return@Slider
                                                updatePreview(base, selectedFilter, adjustments)
                                            },
                                            valueRange = -50f..50f,
                                            colors = SliderDefaults.colors(
                                                thumbColor = CamScannerGreen,
                                                activeTrackColor = CamScannerGreen,
                                                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "${adjustments.brightness.toInt()}",
                                            fontSize = 11.sp,
                                            color = Color(0xFF94A3B8),
                                            modifier = Modifier.width(32.dp)
                                        )
                                    }

                                    // Contrast Slider
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "Contrast",
                                            fontSize = 12.sp,
                                            color = Color.White,
                                            modifier = Modifier.width(76.dp)
                                        )
                                        Slider(
                                            value = adjustments.contrast,
                                            onValueChange = { newVal ->
                                                adjustments = adjustments.copy(contrast = newVal)
                                                val base = croppedBitmap ?: return@Slider
                                                updatePreview(base, selectedFilter, adjustments)
                                            },
                                            valueRange = 0.5f..2.0f,
                                            colors = SliderDefaults.colors(
                                                thumbColor = CamScannerGreen,
                                                activeTrackColor = CamScannerGreen,
                                                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = String.format(Locale.US, "%.1fx", adjustments.contrast),
                                            fontSize = 11.sp,
                                            color = Color(0xFF94A3B8),
                                            modifier = Modifier.width(32.dp)
                                        )
                                    }

                                    // Detail / Sharpness Slider
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "Detail",
                                            fontSize = 12.sp,
                                            color = Color.White,
                                            modifier = Modifier.width(76.dp)
                                        )
                                        Slider(
                                            value = adjustments.detail,
                                            onValueChange = { newVal ->
                                                adjustments = adjustments.copy(detail = newVal)
                                                val base = croppedBitmap ?: return@Slider
                                                updatePreview(base, selectedFilter, adjustments)
                                            },
                                            valueRange = 0f..1.0f,
                                            colors = SliderDefaults.colors(
                                                thumbColor = CamScannerGreen,
                                                activeTrackColor = CamScannerGreen,
                                                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "${(adjustments.detail * 100).toInt()}%",
                                            fontSize = 11.sp,
                                            color = Color(0xFF94A3B8),
                                            modifier = Modifier.width(32.dp)
                                        )
                                    }

                                    // Reset Sliders
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(
                                            onClick = {
                                                adjustments = FilterAdjustments()
                                                val base = croppedBitmap ?: return@TextButton
                                                updatePreview(base, selectedFilter, adjustments)
                                            }
                                        ) {
                                            Text("Reset", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                        }
                                    }
                                }
                            }

                            // 2. Horizontally Scrollable Row with Filter Thumbnails
                            // (Original, Lighten, Magic Color, Gray Mode, B&W)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                ScannerFilterType.entries.forEach { filter ->
                                    val isSelected = selectedFilter == filter
                                    val thumbBitmap = miniThumbnails[filter]

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .width(74.dp)
                                            .clickable {
                                                if (selectedFilter != filter) {
                                                    selectedFilter = filter
                                                    val base = croppedBitmap ?: return@clickable
                                                    updatePreview(base, filter, adjustments)
                                                }
                                            }
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color(0xFF334155))
                                                .border(
                                                    width = if (isSelected) 2.5.dp else 1.dp,
                                                    color = if (isSelected) CamScannerGreen else Color.White.copy(alpha = 0.15f),
                                                    shape = RoundedCornerShape(12.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (thumbBitmap != null) {
                                                Image(
                                                    bitmap = thumbBitmap.asImageBitmap(),
                                                    contentDescription = filter.label,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(16.dp),
                                                    strokeWidth = 2.dp,
                                                    color = CamScannerGreen
                                                )
                                            }

                                            if (isSelected) {
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(4.dp)
                                                        .size(16.dp)
                                                        .background(CamScannerGreen, CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Check,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Text(
                                            text = filter.label,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) CamScannerGreen else Color(0xFFCBD5E1),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val previewBitmap = displayBitmap ?: croppedBitmap
                    if (previewBitmap != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            shadowElevation = 8.dp,
                            color = Color.Black,
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            Image(
                                bitmap = previewBitmap.asImageBitmap(),
                                contentDescription = "Enhanced Document",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    if (isFilterProcessing) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .background(Color.Black.copy(alpha = 0.65f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = CamScannerGreen,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Rename Document Dialog
    if (isEditingTitle) {
        var tempTitle by remember { mutableStateOf(documentTitle) }
        AlertDialog(
            onDismissRequest = { isEditingTitle = false },
            title = { Text("Rename Document", fontWeight = FontWeight.Bold) },
            text = {
                LojiaTextField(
                    value = tempTitle,
                    onValueChange = { tempTitle = it },
                    label = { Text("Document Title") }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempTitle.isNotBlank()) {
                            documentTitle = tempTitle.trim()
                        }
                        isEditingTitle = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CamScannerGreen)
                ) {
                    Text("OK", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { isEditingTitle = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // OCR Bottom Sheet
    if (showOcrSheet) {
        ModalBottomSheet(
            onDismissRequest = { showOcrSheet = false },
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.TextFields,
                            contentDescription = null,
                            tint = CamScannerGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Extracted Text (OCR)",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (ocrExtractedText.isNotBlank()) {
                        IconButton(
                            onClick = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Scanned Text", ocrExtractedText)
                                cm.setPrimaryClip(clip)
                                Toast.makeText(context, "Text copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (isOcrRunning) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = CamScannerGreen)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Extracting text via Google ML Kit...", color = Color.Gray, fontSize = 13.sp)
                        }
                    }
                } else if (ocrExtractedText.isBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No readable text detected in this document",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                            .background(Color(0xFFF1F5F9), RoundedCornerShape(10.dp))
                            .padding(14.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = ocrExtractedText,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = Color(0xFF1E293B)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, ocrExtractedText)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share Extracted Text"))
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share Text")
                        }

                        Button(
                            onClick = { showOcrSheet = false },
                            colors = ButtonDefaults.buttonColors(containerColor = CamScannerGreen),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Done", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

private fun runOcr(bitmap: Bitmap, onResult: (String) -> Unit) {
    try {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val image = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                onResult(visionText.text)
            }
            .addOnFailureListener {
                onResult("")
            }
    } catch (e: Exception) {
        onResult("")
    }
}
