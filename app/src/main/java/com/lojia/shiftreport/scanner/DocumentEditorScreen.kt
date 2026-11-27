package com.lojia.shiftreport.scanner

import com.lojia.shiftreport.R
import com.lojia.shiftreport.ui.theme.*
import androidx.compose.ui.res.stringResource
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.lojia.shiftreport.ui.common.DialogSize
import com.lojia.shiftreport.ui.common.LojiaDialog
import com.lojia.shiftreport.ui.common.LojiaTextField
import com.lojia.shiftreport.util.DateTimeFormatUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

/** Machine default filename prefix for newly scanned/cropped documents. */
private const val DEFAULT_DOCUMENT_TITLE_PREFIX = "Doc_"

private val ScannerEmerald = PosCashGreenFill
private val ScannerEditorBg = OnBackgroundLight
private val ScannerCardBg = OnSurfaceVariantLight

enum class EditorStep {
    CROP_BOUNDARIES,
    FILTER_AND_ADJUST
}

/**
 * Document Filter & Enhancement Screen adhering to Material 3 design and Google ML Kit processing.
 * Features:
 * - Horizontally scrollable row containing filter thumbnails (Original, Lighten, Smart Color, Gray Mode, B&W)
 * - Intuitive top bar with "Back" icon and prominent "Save/Done" checkmark
 * - Manual Brightness, Contrast, and Detail adjustment sliders
 * - Built-in Google ML Kit OCR text extraction and PDF generation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentEditorScreen(
    initialBitmap: Bitmap,
    viewModel: DocumentScannerViewModel,
    onFinish: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    initialDetectedCorners: DocumentCorners? = null,
    autoCropImmediately: Boolean = false
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    androidx.activity.compose.BackHandler(enabled = true) { onCancel() }

    var currentStep by remember { mutableStateOf(EditorStep.CROP_BOUNDARIES) }
    var workingBitmap by remember { mutableStateOf(initialBitmap) }
    var initialCorners by remember { mutableStateOf(initialDetectedCorners) }
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
        DEFAULT_DOCUMENT_TITLE_PREFIX + DateTimeFormatUtils.formatFileTimestamp()
    }
    var documentTitle by remember { mutableStateOf(defaultTitle) }
    var isEditingTitle by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    // OCR State
    var showOcrSheet by remember { mutableStateOf(false) }
    var isOcrRunning by remember { mutableStateOf(false) }
    var ocrExtractedText by remember { mutableStateOf("") }

    // Function to re-apply filter and adjustments
    fun updatePreview(base: Bitmap, filter: ScannerFilterType, adj: FilterAdjustments) {
        coroutineScope.launch {
            isFilterProcessing = true
            val result = DocumentImageFilter.applyFilter(base, filter, adj)
            displayBitmap = result
            isFilterProcessing = false
        }
    }

    suspend fun performCropAndOpenFilterStep(cornersToCrop: DocumentCorners) {
        val cropped = PerspectiveTransformHelper.cropAndStraighten(
            workingBitmap,
            cornersToCrop
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

    // Auto-crop immediately if last detected corners exist from camera preview;
    // otherwise detect/pre-position corners and show manual crop screen
    LaunchedEffect(workingBitmap) {
        if (autoCropImmediately && initialDetectedCorners != null && workingBitmap === initialBitmap) {
            initialCorners = initialDetectedCorners
            performCropAndOpenFilterStep(initialDetectedCorners)
        } else {
            val openCvPoints = withContext(Dispatchers.Default) {
                EdgeDetector.detectDocumentCorners(workingBitmap)
            }
            val detected = if (openCvPoints != null) {
                EdgeDetector.toDocumentCorners(
                    points = openCvPoints,
                    srcWidth = workingBitmap.width,
                    srcHeight = workingBitmap.height
                )
            } else {
                null
            } ?: PerspectiveTransformHelper.detectDocumentCorners(workingBitmap)
            initialCorners = detected
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

            DocumentCropCanvasView(
                bitmap = workingBitmap,
                initialCorners = corners,
                onConfirmCrop = { confirmedCorners ->
                    coroutineScope.launch {
                        performCropAndOpenFilterStep(confirmedCorners)
                    }
                },
                onAutoDetect = {
                    coroutineScope.launch {
                        val openCvPoints = withContext(Dispatchers.Default) {
                            EdgeDetector.detectDocumentCorners(workingBitmap)
                        }
                        val detected = if (openCvPoints != null) {
                            EdgeDetector.toDocumentCorners(
                                points = openCvPoints,
                                srcWidth = workingBitmap.width,
                                srcHeight = workingBitmap.height
                            )
                        } else {
                            null
                        } ?: PerspectiveTransformHelper.detectDocumentCorners(workingBitmap)
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
                containerColor = ScannerEditorBg,
                topBar = {
                    Surface(
                        color = ScannerCardBg,
                        shadowElevation = Dimens.FlatCardShadowBlur
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = Dimens.SpacingMd, vertical = Dimens.SpacingSm),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Back Icon
                            IconButton(onClick = { currentStep = EditorStep.CROP_BOUNDARIES }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                    contentDescription = stringResource(R.string.cd_back_to_crop),
                                    tint = Color.White
                                )
                            }

                            // Document Title (Editable)
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = Dimens.SpacingSm)
                                    .clip(RoundedCornerShape(Dimens.RadiusMd))
                                    .minimumInteractiveComponentSize()
                                    .clickable { isEditingTitle = true }
                                    .padding(horizontal = Dimens.SpacingSm, vertical = Dimens.SpacingXs),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = documentTitle,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.width(Dimens.SpacingXs))
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = stringResource(R.string.cd_rename),
                                    tint = TextDisabledColor,
                                    modifier = Modifier.size(Dimens.IconXs)
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
                                    contentDescription = stringResource(R.string.cd_ocr_text),
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
                                        Toast.makeText(context, context.getString(R.string.doc_saved_as_pdf), Toast.LENGTH_SHORT).show()
                                        onFinish()
                                    }
                                },
                                enabled = !isSaving,
                                colors = ButtonDefaults.buttonColors(containerColor = ScannerEmerald),
                                shape = RoundedCornerShape(Dimens.RadiusLg),
                                contentPadding = PaddingValues(horizontal = Dimens.SpacingMd, vertical = Dimens.SpacingSm),
                                modifier = Modifier.heightIn(min = Dimens.MinTouchTarget)
                            ) {
                                if (isSaving) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(Dimens.IconMd),
                                        strokeWidth = Dimens.ProgressStrokeWidth,
                                        color = Color.White
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = stringResource(R.string.cd_save),
                                        tint = Color.White,
                                        modifier = Modifier.size(Dimens.IconMd)
                                    )
                                    Spacer(modifier = Modifier.width(Dimens.SpacingXs))
                                    Text(
                                        text = stringResource(R.string.done),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                },
                bottomBar = {
                    Surface(
                        color = ScannerCardBg,
                        shape = RoundedCornerShape(topStart = Dimens.BottomSheetCornerRadius, topEnd = Dimens.BottomSheetCornerRadius),
                        shadowElevation = Dimens.FlatCardShadowBlur,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(vertical = Dimens.SpacingMd)
                        ) {
                            // Section Header with Adjust Toggle
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = Dimens.SpacingLg, vertical = Dimens.SpacingXs),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (showAdjustSliders) stringResource(R.string.manual_fine_tuning) else stringResource(R.string.enhancement_filters),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextDisabledColor
                                )

                                // Sliders Toggle Button
                                TextButton(
                                    onClick = { showAdjustSliders = !showAdjustSliders },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = if (showAdjustSliders) ScannerEmerald else Color.White
                                    ),
                                    contentPadding = PaddingValues(horizontal = Dimens.SpacingSm, vertical = Dimens.SpacingXxs),
                                    modifier = Modifier.heightIn(min = Dimens.MinTouchTarget)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Tune,
                                        contentDescription = null,
                                        modifier = Modifier.size(Dimens.IconSm)
                                    )
                                    Spacer(modifier = Modifier.width(Dimens.SpacingXs))
                                    Text(
                                        text = if (showAdjustSliders) stringResource(R.string.btn_filters) else stringResource(R.string.btn_adjust),
                                        style = MaterialTheme.typography.titleSmall,
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
                                        .padding(horizontal = Dimens.SpacingLg, vertical = Dimens.SpacingSm)
                                ) {
                                    // Brightness Slider
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = stringResource(R.string.brightness_label),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White,
                                            modifier = Modifier.width(Dimens.ScannerSliderLabelWidth)
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
                                                thumbColor = ScannerEmerald,
                                                activeTrackColor = ScannerEmerald,
                                                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "${adjustments.brightness.toInt()}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = TextDisabledColor,
                                            modifier = Modifier.width(Dimens.IconXl)
                                        )
                                    }

                                    // Contrast Slider
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = stringResource(R.string.contrast_label),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White,
                                            modifier = Modifier.width(Dimens.ScannerSliderLabelWidth)
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
                                                thumbColor = ScannerEmerald,
                                                activeTrackColor = ScannerEmerald,
                                                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = String.format(DateTimeFormatUtils.getAppLocale(), context.getString(R.string.contrast_multiplier_fmt), adjustments.contrast),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = TextDisabledColor,
                                            modifier = Modifier.width(Dimens.IconXl)
                                        )
                                    }

                                    // Detail / Sharpness Slider
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = stringResource(R.string.detail_label),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White,
                                            modifier = Modifier.width(Dimens.ScannerSliderLabelWidth)
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
                                                thumbColor = ScannerEmerald,
                                                activeTrackColor = ScannerEmerald,
                                                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "${(adjustments.detail * 100).toInt()}%",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = TextDisabledColor,
                                            modifier = Modifier.width(Dimens.IconXl)
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
                                            },
                                            modifier = Modifier.heightIn(min = Dimens.MinTouchTarget)
                                        ) {
                                            Text(
                                                text = stringResource(R.string.btn_reset),
                                                color = TextDisabledColor,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
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
                                    .padding(horizontal = Dimens.SpacingLg, vertical = Dimens.SpacingXs),
                                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingMd)
                            ) {
                                ScannerFilterType.entries.forEach { filter ->
                                    val isSelected = selectedFilter == filter
                                    val thumbBitmap = miniThumbnails[filter]

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .width(Dimens.ScannerFilterItemWidth)
                                            .minimumInteractiveComponentSize()
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
                                                .size(Dimens.ScannerFilterThumbSize)
                                                .clip(RoundedCornerShape(Dimens.RadiusLg))
                                                .background(OnSurfaceVariantLight)
                                                .border(
                                                    width = if (isSelected) Dimens.CropperStrokeWidth else Dimens.FlatBorderWidth,
                                                    color = if (isSelected) ScannerEmerald else Color.White.copy(alpha = 0.15f),
                                                    shape = RoundedCornerShape(Dimens.RadiusLg)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (thumbBitmap != null) {
                                                Image(
                                                    bitmap = thumbBitmap.asImageBitmap(),
                                                    contentDescription = stringResource(filter.titleRes),
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(Dimens.IconSm),
                                                    strokeWidth = Dimens.ProgressStrokeWidth,
                                                    color = ScannerEmerald
                                                )
                                            }

                                            if (isSelected) {
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(Dimens.SpacingXs)
                                                        .size(Dimens.IconSm)
                                                        .background(ScannerEmerald, CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Check,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(Dimens.SpacingMd)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(Dimens.SpacingXs))

                                        Text(
                                            text = stringResource(filter.titleRes),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) ScannerEmerald else OutlineLight,
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
                        .padding(Dimens.SpacingLg),
                    contentAlignment = Alignment.Center
                ) {
                    val previewBitmap = displayBitmap ?: croppedBitmap
                    if (previewBitmap != null) {
                        Surface(
                            shape = RoundedCornerShape(Dimens.RadiusLg),
                            shadowElevation = Dimens.FlatCardShadowBlur,
                            color = Color.Black,
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(Dimens.RadiusLg))
                        ) {
                            Image(
                                bitmap = previewBitmap.asImageBitmap(),
                                contentDescription = stringResource(R.string.share_shift_report_title),
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    if (isFilterProcessing) {
                        Box(
                            modifier = Modifier
                                .size(Dimens.ScannerProcessingOverlaySize)
                                .background(Color.Black.copy(alpha = 0.65f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = ScannerEmerald,
                                strokeWidth = Dimens.ChartStrokeMedium,
                                modifier = Modifier.size(Dimens.SectionIconTileSize)
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
        val style = LocalDepthStyle.current
        LojiaDialog(
            onDismissRequest = { isEditingTitle = false },
            size = DialogSize.Small
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMd)
            ) {
                Text(
                    text = stringResource(R.string.doc_rename_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceLight
                )
                LojiaTextField(
                    value = tempTitle,
                    onValueChange = { tempTitle = it },
                    label = { Text(stringResource(R.string.doc_title_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { isEditingTitle = false },
                        border = null,
                        elevation = null,
                        shape = RoundedCornerShape(Dimens.RadiusMd),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = PrimaryIndigoDark
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = Dimens.MinTouchTarget)
                            .depthSecondaryButton(
                                borderColor = style.secondaryButtonBorderColor,
                                cornerRadius = Dimens.RadiusMd
                            )
                    ) {
                        Text(stringResource(R.string.cancel), fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = {
                            if (tempTitle.isNotBlank()) {
                                documentTitle = tempTitle.trim()
                            }
                            isEditingTitle = false
                        },
                        elevation = null,
                        shape = RoundedCornerShape(Dimens.RadiusMd),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = PureWhite
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = Dimens.MinTouchTarget)
                            .depthRaisedButton(cornerRadius = Dimens.RadiusMd)
                    ) {
                        Text(stringResource(R.string.ok), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // OCR Bottom Sheet
    if (showOcrSheet) {
        val style = LocalDepthStyle.current
        ModalBottomSheet(
            onDismissRequest = { showOcrSheet = false },
            containerColor = PureWhite,
            shape = RoundedCornerShape(
                topStart = Dimens.BottomSheetCornerRadius,
                topEnd = Dimens.BottomSheetCornerRadius
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.SpacingLg)
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
                            tint = ScannerEmerald,
                            modifier = Modifier.size(Dimens.IconLg)
                        )
                        Spacer(modifier = Modifier.width(Dimens.SpacingSm))
                        Text(
                            text = stringResource(R.string.cd_ocr_text),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = OnBackgroundLight
                        )
                    }

                    if (ocrExtractedText.isNotBlank()) {
                        IconButton(
                            onClick = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText(context.getString(R.string.scan_clipboard_label), ocrExtractedText)
                                cm.setPrimaryClip(clip)
                                Toast.makeText(context, context.getString(R.string.doc_toast_text_copied), Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Outlined.ContentCopy, contentDescription = stringResource(R.string.cd_copy))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Dimens.SpacingMd))

                if (isOcrRunning) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(Dimens.ScannerOcrBoxHeight),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = ScannerEmerald)
                            Spacer(modifier = Modifier.height(Dimens.SpacingSm))
                            Text(
                                text = stringResource(R.string.extracting_text_progress),
                                color = OnSurfaceVariantLight,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                } else if (ocrExtractedText.isBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(Dimens.ScannerOcrBoxHeight),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.no_readable_text_detected),
                            color = OnSurfaceVariantLight,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = Dimens.ScannerOcrScrollMaxHeight)
                            .depthInset()
                            .padding(Dimens.SpacingMd)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = ocrExtractedText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = OnBackgroundLight
                        )
                    }

                    Spacer(modifier = Modifier.height(Dimens.SpacingLg))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, ocrExtractedText)
                                }
                                context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_extracted_text_title)))
                            },
                            border = null,
                            elevation = null,
                            shape = RoundedCornerShape(Dimens.RadiusMd),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.Transparent,
                                contentColor = PrimaryIndigoDark
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = Dimens.MinTouchTarget)
                                .depthSecondaryButton(
                                    borderColor = style.secondaryButtonBorderColor,
                                    cornerRadius = Dimens.RadiusMd
                                )
                        ) {
                            Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(Dimens.IconSm))
                            Spacer(modifier = Modifier.width(Dimens.SpacingXs))
                            Text(stringResource(R.string.btn_share_text), fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = { showOcrSheet = false },
                            elevation = null,
                            shape = RoundedCornerShape(Dimens.RadiusMd),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = PureWhite
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = Dimens.MinTouchTarget)
                                .depthRaisedButton(cornerRadius = Dimens.RadiusMd)
                        ) {
                            Text(stringResource(R.string.done), fontWeight = FontWeight.Bold)
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
