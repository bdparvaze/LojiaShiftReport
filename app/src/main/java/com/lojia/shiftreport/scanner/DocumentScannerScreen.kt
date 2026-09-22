package com.lojia.shiftreport.scanner

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.lojia.shiftreport.R
import com.lojia.shiftreport.ui.common.LojiaTextField
import com.lojia.shiftreport.ui.theme.PrimaryIndigo
import com.lojia.shiftreport.ui.theme.PureWhite
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DocumentScannerScreen(
    activity: Activity? = null,
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: DocumentScannerViewModel = viewModel()
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val documents by viewModel.documents.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showSaveTitleDialog by remember { mutableStateOf(false) }
    var showScannerFallbackPrompt by remember { mutableStateOf(false) }
    var customDocumentTitle by remember { mutableStateOf("") }
    var pendingScanResult by remember { mutableStateOf<GmsDocumentScanningResult?>(null) }
    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var tempCameraImageUri by remember { mutableStateOf<Uri?>(null) }

    // Selection mode state
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedDocs = remember { mutableStateListOf<ScannedDocument>() }

    // Dialogs state
    var documentToRename by remember { mutableStateOf<ScannedDocument?>(null) }
    var documentToDelete by remember { mutableStateOf<ScannedDocument?>(null) }
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }
    var documentForOcr by remember { mutableStateOf<ScannedDocument?>(null) }
    var ocrLoading by remember { mutableStateOf(false) }
    var ocrTextResult by remember { mutableStateOf("") }
    var documentForPasswordProtect by remember { mutableStateOf<ScannedDocument?>(null) }

    val filteredDocs = remember(documents, searchQuery) {
        if (searchQuery.isBlank()) {
            documents
        } else {
            documents.filter { it.title.contains(searchQuery, ignoreCase = true) }
        }
    }

    // Launchers
    val galleryFallbackLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            customDocumentTitle = "Doc_$timeStamp"
            pendingScanResult = null
            pendingPhotoUri = uri
            showSaveTitleDialog = true
        }
    }

    val cameraFallbackLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraImageUri != null) {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            customDocumentTitle = "Doc_$timeStamp"
            pendingScanResult = null
            pendingPhotoUri = tempCameraImageUri
            showSaveTitleDialog = true
        }
    }

    val startCameraCapture: () -> Unit = {
        try {
            val tempFile = File.createTempFile("camera_doc_", ".jpg", context.cacheDir).apply {
                createNewFile()
                deleteOnExit()
            }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile
            )
            tempCameraImageUri = uri
            cameraFallbackLauncher.launch(uri)
        } catch (e: Exception) {
            Log.e("DocScanner", "Camera fallback failed to launch, trying gallery", e)
            Toast.makeText(context, "Camera launch error: ${e.javaClass.simpleName} - ${e.message}", Toast.LENGTH_SHORT).show()
            galleryFallbackLauncher.launch("image/*")
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCameraCapture()
        } else {
            Toast.makeText(
                context,
                "Camera permission is required to capture documents",
                Toast.LENGTH_SHORT
            ).show()
            galleryFallbackLauncher.launch("image/*")
        }
    }

    val launchFallbackCapture = {
        val hasCameraPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasCameraPermission) {
            startCameraCapture()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        try {
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
                if (scanResult != null && (scanResult.pdf != null || !scanResult.pages.isNullOrEmpty())) {
                    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                    customDocumentTitle = "Scan_$timeStamp"
                    pendingScanResult = scanResult
                    pendingPhotoUri = null
                    showSaveTitleDialog = true
                } else {
                    Log.w("DocScanner", "Scan returned RESULT_OK but scanResult content is null/empty")
                    showScannerFallbackPrompt = true
                }
            } else if (result.resultCode == Activity.RESULT_CANCELED) {
                Log.d("DocScanner", "Scan was cancelled by user")
            } else {
                Log.w("DocScanner", "Scan returned resultCode: ${result.resultCode}")
                showScannerFallbackPrompt = true
            }
        } catch (e: Throwable) {
            Log.e("DocScanner", "Error handling scanner result", e)
            showScannerFallbackPrompt = true
        }
    }

    val startScannerFlow: () -> Unit = {
        val targetActivity = activity ?: context.findActivity()
        if (targetActivity == null || targetActivity.isFinishing || targetActivity.isDestroyed) {
            Log.w("DocScanner", "Target activity unavailable, direct fallback to camera")
            launchFallbackCapture()
        } else {
            try {
                val options = GmsDocumentScannerOptions.Builder()
                    .setGalleryImportAllowed(true)
                    .setPageLimit(25)
                    .setResultFormats(
                        GmsDocumentScannerOptions.RESULT_FORMAT_JPEG,
                        GmsDocumentScannerOptions.RESULT_FORMAT_PDF
                    )
                    .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_BASE)
                    .build()

                GmsDocumentScanning.getClient(options)
                    .getStartScanIntent(targetActivity)
                    .addOnSuccessListener { intentSender ->
                        val request = IntentSenderRequest.Builder(intentSender).build()
                        scannerLauncher.launch(request)
                    }
                    .addOnFailureListener { e ->
                        Log.e("DocScanner", "Scanner start failed", e)
                        showScannerFallbackPrompt = true
                    }
            } catch (e: Throwable) {
                Log.e("DocScanner", "Scanner initialization failed", e)
                showScannerFallbackPrompt = true
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Header card
            DocumentScannerHeaderCard(
                onScanClick = startScannerFlow,
                onGalleryClick = { galleryFallbackLauncher.launch("image/*") },
                onCameraFallbackClick = launchFallbackCapture
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Search Bar & Selection Mode Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LojiaTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(R.string.search), fontSize = 13.5.sp) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = Color(0xFF64748B)) },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Outlined.Close, contentDescription = "Clear", tint = Color(0xFF64748B))
                            }
                        }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                if (documents.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            isSelectionMode = !isSelectionMode
                            if (!isSelectionMode) selectedDocs.clear()
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (isSelectionMode) PrimaryIndigo.copy(alpha = 0.15f) else PureWhite
                        )
                    ) {
                        Icon(
                            imageVector = if (isSelectionMode) Icons.Outlined.ChecklistRtl else Icons.Outlined.Checklist,
                            contentDescription = "Select Mode",
                            tint = if (isSelectionMode) PrimaryIndigo else Color(0xFF64748B)
                        )
                    }
                }
            }

            // Batch selection actions bar
            if (isSelectionMode && selectedDocs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = PrimaryIndigo.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${selectedDocs.size} selected",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = PrimaryIndigo
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick = {
                                    if (selectedDocs.size == filteredDocs.size) {
                                        selectedDocs.clear()
                                    } else {
                                        selectedDocs.clear()
                                        selectedDocs.addAll(filteredDocs)
                                    }
                                }
                            ) {
                                Text(
                                    text = if (selectedDocs.size == filteredDocs.size) "Deselect All" else "Select All",
                                    fontSize = 12.sp,
                                    color = PrimaryIndigo
                                )
                            }

                            Button(
                                onClick = { showBatchDeleteConfirm = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Delete", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Document List
            if (filteredDocs.isEmpty()) {
                DocumentScannerEmptyState(onScanClick = startScannerFlow)
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(filteredDocs, key = { it.id }) { doc ->
                        ScannedDocumentItemCard(
                            document = doc,
                            isSelectionMode = isSelectionMode,
                            isSelected = selectedDocs.contains(doc),
                            onToggleSelect = {
                                if (selectedDocs.contains(doc)) {
                                    selectedDocs.remove(doc)
                                } else {
                                    selectedDocs.add(doc)
                                }
                            },
                            onViewClick = { viewModel.viewDocument(context, doc) },
                            onSharePdfClick = { viewModel.shareDocument(context, doc) },
                            onShareDocxClick = { viewModel.shareDocx(context, doc) },
                            onExtractTextClick = {
                                documentForOcr = doc
                                ocrLoading = true
                                ocrTextResult = doc.ocrText
                                viewModel.extractTextAndGenerateDocx(doc) { text, _ ->
                                    ocrTextResult = text
                                    ocrLoading = false
                                }
                            },
                            onPasswordProtectClick = { documentForPasswordProtect = doc },
                            onRenameClick = { documentToRename = doc },
                            onDeleteClick = { documentToDelete = doc }
                        )
                    }
                }
            }
        }
    }

    // Dialogs
    if (showSaveTitleDialog) {
        SaveDocumentTitleDialog(
            title = customDocumentTitle,
            onTitleChange = { customDocumentTitle = it },
            onConfirm = {
                showSaveTitleDialog = false
                val scanRes = pendingScanResult
                val photoUri = pendingPhotoUri
                if (scanRes != null) {
                    viewModel.saveScannedResult(scanRes, customDocumentTitle)
                } else if (photoUri != null) {
                    viewModel.saveImageAsScannedDocument(photoUri, customDocumentTitle)
                }
                pendingScanResult = null
                pendingPhotoUri = null
                Toast.makeText(context, "Document saved successfully", Toast.LENGTH_SHORT).show()
            },
            onDismiss = {
                showSaveTitleDialog = false
                pendingScanResult = null
                pendingPhotoUri = null
            }
        )
    }

    if (showScannerFallbackPrompt) {
        ScannerFallbackPromptDialog(
            onCameraCapture = launchFallbackCapture,
            onGalleryPick = { galleryFallbackLauncher.launch("image/*") },
            onDismiss = { showScannerFallbackPrompt = false }
        )
    }

    if (documentToRename != null) {
        val doc = documentToRename!!
        RenameDocumentDialog(
            currentTitle = doc.title,
            onRename = { newTitle ->
                viewModel.renameDocument(doc, newTitle)
                documentToRename = null
                Toast.makeText(context, "Document renamed", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { documentToRename = null }
        )
    }

    if (documentToDelete != null) {
        val doc = documentToDelete!!
        DeleteDocumentConfirmDialog(
            documentTitle = doc.title,
            onConfirmDelete = {
                viewModel.deleteDocument(doc)
                documentToDelete = null
                Toast.makeText(context, "Document deleted", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { documentToDelete = null }
        )
    }

    if (showBatchDeleteConfirm && selectedDocs.isNotEmpty()) {
        BatchDeleteConfirmDialog(
            count = selectedDocs.size,
            onConfirmDelete = {
                val count = selectedDocs.size
                viewModel.deleteDocuments(selectedDocs.toList())
                selectedDocs.clear()
                isSelectionMode = false
                showBatchDeleteConfirm = false
                Toast.makeText(context, "$count documents deleted", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showBatchDeleteConfirm = false }
        )
    }

    if (documentForOcr != null) {
        val doc = documentForOcr!!
        OcrTextPreviewDialog(
            documentTitle = doc.title,
            extractedText = ocrTextResult,
            isLoading = ocrLoading,
            onCopyText = { text ->
                clipboardManager.setText(AnnotatedString(text))
                Toast.makeText(context, "Text copied to clipboard", Toast.LENGTH_SHORT).show()
            },
            onShareWord = {
                viewModel.shareDocx(context, doc)
            },
            onDismiss = { documentForOcr = null }
        )
    }

    if (documentForPasswordProtect != null) {
        val doc = documentForPasswordProtect!!
        PasswordProtectDialog(
            documentTitle = doc.title,
            onProtect = { password ->
                viewModel.protectPdfWithPassword(context, doc, password) { result ->
                    result.onSuccess { protectedFile ->
                        Toast.makeText(context, "Protected PDF created: ${protectedFile.name}", Toast.LENGTH_SHORT).show()
                        viewModel.shareProtectedPdf(context, protectedFile)
                    }.onFailure { err ->
                        Toast.makeText(context, "Encryption error: ${err.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
                documentForPasswordProtect = null
            },
            onDismiss = { documentForPasswordProtect = null }
        )
    }
}

// Utility extension to find Activity from Context
fun Context.findActivity(): Activity? {
    var currentContext = this
    while (currentContext is android.content.ContextWrapper) {
        if (currentContext is Activity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return null
}
