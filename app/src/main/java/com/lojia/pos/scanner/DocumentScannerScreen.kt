package com.lojia.pos.scanner

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.lojia.pos.R
import com.lojia.pos.ui.theme.PrimaryIndigo
import com.lojia.pos.ui.theme.PureWhite
import java.io.File
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentScannerScreen(
    activity: Activity,
    modifier: Modifier = Modifier,
    viewModel: DocumentScannerViewModel = viewModel()
) {
    val context = LocalContext.current
    val documents by viewModel.documents.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var pendingScanResult by remember { mutableStateOf<GmsDocumentScanningResult?>(null) }
    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var tempCameraImageUri by remember { mutableStateOf<Uri?>(null) }
    var showSaveTitleDialog by remember { mutableStateOf(false) }
    var showScannerFallbackPrompt by remember { mutableStateOf(false) }
    var customDocumentTitle by remember { mutableStateOf("") }

    var documentToDelete by remember { mutableStateOf<ScannedDocument?>(null) }
    var documentToRename by remember { mutableStateOf<ScannedDocument?>(null) }
    var renameTitleInput by remember { mutableStateOf("") }

    var ocrPreviewDocument by remember { mutableStateOf<ScannedDocument?>(null) }
    var isOcrExtracting by remember { mutableStateOf(false) }
    var displayedOcrText by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current

    // Multi-Select / Batch Delete States
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedDocIds by remember { mutableStateOf(setOf<Int>()) }
    var showBatchDeleteDialog by remember { mutableStateOf(false) }

    // Password Protection States
    var documentToProtect by remember { mutableStateOf<ScannedDocument?>(null) }
    var protectPasswordInput by remember { mutableStateOf("") }
    var confirmPasswordInput by remember { mutableStateOf("") }
    var isProtectPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }
    var protectErrorMessage by remember { mutableStateOf<String?>(null) }
    var isProtectingPdf by remember { mutableStateOf(false) }
    var protectedPdfSuccessFile by remember { mutableStateOf<File?>(null) }

    val exitSelectionMode = {
        isSelectionMode = false
        selectedDocIds = emptySet()
    }

    val toggleDocSelection = { docId: Int ->
        selectedDocIds = if (selectedDocIds.contains(docId)) {
            selectedDocIds - docId
        } else {
            selectedDocIds + docId
        }
    }

    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        try {
            if (result.resultCode == Activity.RESULT_OK) {
                val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
                if (scanResult != null) {
                    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                    customDocumentTitle = "Scan_$timeStamp"
                    pendingScanResult = scanResult
                    pendingPhotoUri = null
                    showSaveTitleDialog = true
                } else {
                    Log.w("DocScanner", "Scan returned RESULT_OK but scanResult is null")
                    showScannerFallbackPrompt = true
                }
            } else if (result.resultCode == Activity.RESULT_CANCELED) {
                Log.d("DocScanner", "Scan was cancelled or dismissed by user")
                showScannerFallbackPrompt = true
            } else {
                Log.w("DocScanner", "Scan returned resultCode: ${result.resultCode}")
                showScannerFallbackPrompt = true
            }
        } catch (e: Exception) {
            Log.e("DocScanner", "Error handling scanner result", e)
            showScannerFallbackPrompt = true
        }
    }

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
                context.getString(R.string.camera_permission_required),
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
            // Header Card
            Card(
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color(0xFFEEF2FF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DocumentScanner,
                            contentDescription = null,
                            tint = PrimaryIndigo,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = stringResource(R.string.doc_scanner_title),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = Color(0xFF0F172A)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = stringResource(R.string.doc_scanner_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            try {
                                val targetActivity = activity ?: context.findActivity()
                                if (targetActivity == null || targetActivity.isFinishing || targetActivity.isDestroyed) {
                                    Log.e("DocumentScanner", "Cannot start scan: activity is null or finishing")
                                    Toast.makeText(context, "Activity is finishing or unavailable", Toast.LENGTH_LONG).show()
                                    return@Button
                                }

                                val options = GmsDocumentScannerOptions.Builder()
                                    .setGalleryImportAllowed(true)
                                    .setPageLimit(10)
                                    .setResultFormats(
                                        GmsDocumentScannerOptions.RESULT_FORMAT_PDF,
                                        GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
                                    )
                                    .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_BASE)
                                    .build()

                                GmsDocumentScanning.getClient(options)
                                    .getStartScanIntent(targetActivity)
                                    .addOnSuccessListener { intentSender ->
                                        Log.d("DocScanner", "Scan intent sender received successfully")
                                        scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("DocScanner", "start failed", e)
                                        val errorClass = e.javaClass.simpleName
                                        val errorMsg = e.message ?: e.localizedMessage ?: "Unknown error"
                                        val causeMsg = e.cause?.message?.let { " [Cause: $it]" } ?: ""
                                        val fullError = "Scanner failure: $errorClass - $errorMsg$causeMsg"
                                        Toast.makeText(context, fullError, Toast.LENGTH_LONG).show()
                                        showScannerFallbackPrompt = true
                                    }
                            } catch (e: Exception) {
                                Log.e("DocScanner", "start failed", e)
                                val errorClass = e.javaClass.simpleName
                                val errorMsg = e.message ?: e.localizedMessage ?: "Unknown error"
                                val causeMsg = e.cause?.message?.let { " [Cause: $it]" } ?: ""
                                Toast.makeText(context, "Could not start scanner: $errorClass - $errorMsg$causeMsg", Toast.LENGTH_LONG).show()
                                showScannerFallbackPrompt = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DocumentScanner,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.scan_now),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                launchFallbackCapture()
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryIndigo),
                            border = BorderStroke(1.5.dp, PrimaryIndigo),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.scan_with_camera_fallback),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                galleryFallbackLauncher.launch("image/*")
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF475569)),
                            border = BorderStroke(1.5.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AddAPhoto,
                                contentDescription = "Gallery",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { Text("Search scanned documents...", color = Color(0xFF94A3B8)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search",
                        tint = Color(0xFF64748B)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = "Clear search",
                                tint = Color(0xFF64748B)
                            )
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = PureWhite,
                    unfocusedContainerColor = PureWhite,
                    focusedBorderColor = PrimaryIndigo,
                    unfocusedBorderColor = Color(0xFFE2E8F0)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Document List Header or Selection Bar
            if (isSelectionMode) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, PrimaryIndigo.copy(alpha = 0.3f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = exitSelectionMode,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Cancel Selection",
                                    tint = Color(0xFF475569)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${selectedDocIds.size} selected",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                ),
                                color = PrimaryIndigo
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            TextButton(
                                onClick = {
                                    selectedDocIds = if (selectedDocIds.size == documents.size) {
                                        emptySet()
                                    } else {
                                        documents.map { it.id }.toSet()
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (selectedDocIds.size == documents.size && documents.isNotEmpty()) "Clear" else "Select All",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = PrimaryIndigo
                                )
                            }

                            Button(
                                onClick = {
                                    if (selectedDocIds.isNotEmpty()) {
                                        showBatchDeleteDialog = true
                                    }
                                },
                                enabled = selectedDocIds.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFEF4444),
                                    disabledContainerColor = Color(0xFFCBD5E1)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Delete (${selectedDocIds.size})",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.scanned_docs_list, documents.size),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        ),
                        color = Color(0xFF334155)
                    )

                    if (documents.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                isSelectionMode = true
                                selectedDocIds = emptySet()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SelectAll,
                                contentDescription = null,
                                tint = PrimaryIndigo,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Select",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = PrimaryIndigo
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (documents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(Color(0xFFEEF2FF), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DocumentScanner,
                                contentDescription = null,
                                tint = PrimaryIndigo,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "No matching documents" else stringResource(R.string.no_scanned_docs),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF1E293B)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "Try searching with a different title keyword." else stringResource(R.string.no_scanned_docs_sub),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(documents, key = { it.id }) { doc ->
                        val isSelected = selectedDocIds.contains(doc.id)
                        ScannedDocumentItemCard(
                            document = doc,
                            isSelectionMode = isSelectionMode,
                            isSelected = isSelected,
                            onToggleSelection = { toggleDocSelection(doc.id) },
                            onLongPress = {
                                if (!isSelectionMode) {
                                    isSelectionMode = true
                                    selectedDocIds = setOf(doc.id)
                                }
                            },
                            onView = { viewModel.viewDocument(context, doc) },
                            onShare = { viewModel.shareDocument(context, doc) },
                            onProtectPdf = {
                                documentToProtect = doc
                                protectPasswordInput = ""
                                confirmPasswordInput = ""
                                protectErrorMessage = null
                                isProtectPasswordVisible = false
                                isConfirmPasswordVisible = false
                            },
                            onOcrText = {
                                ocrPreviewDocument = doc
                                isOcrExtracting = true
                                displayedOcrText = doc.ocrText
                                viewModel.extractTextAndGenerateDocx(doc) { text, _ ->
                                    displayedOcrText = text
                                    isOcrExtracting = false
                                }
                            },
                            onExportWord = {
                                viewModel.shareDocx(context, doc)
                            },
                            onRename = {
                                documentToRename = doc
                                renameTitleInput = doc.title
                            },
                            onDelete = { documentToDelete = doc }
                        )
                    }
                }
            }
        }
    }

    // Save Title Dialog after scanning
    if (showSaveTitleDialog && (pendingScanResult != null || pendingPhotoUri != null)) {
        AlertDialog(
            onDismissRequest = {
                val scanResult = pendingScanResult
                val photoUri = pendingPhotoUri
                if (scanResult != null) {
                    viewModel.saveScannedResult(scanResult)
                } else if (photoUri != null) {
                    viewModel.saveImageAsScannedDocument(photoUri)
                }
                showSaveTitleDialog = false
                pendingScanResult = null
                pendingPhotoUri = null
            },
            title = { Text(text = "Save Scanned Document", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(text = "Enter a title for your document:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = customDocumentTitle,
                        onValueChange = { customDocumentTitle = it },
                        label = { Text("Document Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val scanResult = pendingScanResult
                        val photoUri = pendingPhotoUri
                        if (scanResult != null) {
                            viewModel.saveScannedResult(scanResult, customDocumentTitle)
                            Toast.makeText(context, context.getString(R.string.doc_scanned_success), Toast.LENGTH_SHORT).show()
                        } else if (photoUri != null) {
                            viewModel.saveImageAsScannedDocument(photoUri, customDocumentTitle)
                            Toast.makeText(context, context.getString(R.string.doc_scanned_success), Toast.LENGTH_SHORT).show()
                        }
                        showSaveTitleDialog = false
                        pendingScanResult = null
                        pendingPhotoUri = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                ) {
                    Text("Save Document", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        val scanResult = pendingScanResult
                        val photoUri = pendingPhotoUri
                        if (scanResult != null) {
                            viewModel.saveScannedResult(scanResult)
                        } else if (photoUri != null) {
                            viewModel.saveImageAsScannedDocument(photoUri)
                        }
                        showSaveTitleDialog = false
                        pendingScanResult = null
                        pendingPhotoUri = null
                    }
                ) {
                    Text("Use Default Name")
                }
            }
        )
    }

    if (showScannerFallbackPrompt) {
        AlertDialog(
            onDismissRequest = { showScannerFallbackPrompt = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.CameraAlt,
                    contentDescription = null,
                    tint = PrimaryIndigo,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(text = "Document Scan Incomplete", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(text = "The Google ML Kit scanner was closed or cannot run in this environment. You can scan using your camera directly or select an image from the gallery.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showScannerFallbackPrompt = false
                        launchFallbackCapture()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                ) {
                    Text("Use Camera", color = Color.White)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = {
                            showScannerFallbackPrompt = false
                            galleryFallbackLauncher.launch("image/*")
                        }
                    ) {
                        Text("Gallery")
                    }
                    TextButton(
                        onClick = {
                            showScannerFallbackPrompt = false
                        }
                    ) {
                        Text("Dismiss")
                    }
                }
            }
        )
    }

    // Rename Document Dialog
    if (documentToRename != null) {
        val doc = documentToRename!!
        AlertDialog(
            onDismissRequest = { documentToRename = null },
            title = { Text(text = "Rename Document", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(text = "Enter new document title:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = renameTitleInput,
                        onValueChange = { renameTitleInput = it },
                        label = { Text("Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameTitleInput.isNotBlank()) {
                            viewModel.renameDocument(doc, renameTitleInput)
                            Toast.makeText(context, "Document renamed", Toast.LENGTH_SHORT).show()
                        }
                        documentToRename = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                ) {
                    Text("Save", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { documentToRename = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (documentToDelete != null) {
        val doc = documentToDelete!!
        AlertDialog(
            onDismissRequest = { documentToDelete = null },
            title = { Text(text = stringResource(R.string.delete_doc_title)) },
            text = { Text(text = stringResource(R.string.delete_doc_confirm, doc.title)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteDocument(doc)
                        documentToDelete = null
                        Toast.makeText(context, context.getString(R.string.doc_deleted), Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(text = stringResource(R.string.delete_1), color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { documentToDelete = null }) {
                    Text(text = stringResource(R.string.cancel_2))
                }
            }
        )
    }

    // Batch Delete Confirmation Dialog
    if (showBatchDeleteDialog && selectedDocIds.isNotEmpty()) {
        val count = selectedDocIds.size
        val docsToDelete = documents.filter { selectedDocIds.contains(it.id) }
        AlertDialog(
            onDismissRequest = { showBatchDeleteDialog = false },
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFFFEF2F2), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteSweep,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "Delete $count ${if (count == 1) "Document" else "Documents"}?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete $count selected ${if (count == 1) "document" else "documents"}? All associated PDF, Word, and image files will be removed. This cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF475569)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteDocuments(docsToDelete)
                        showBatchDeleteDialog = false
                        exitSelectionMode()
                        Toast.makeText(context, "$count documents deleted", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Delete ($count)", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchDeleteDialog = false }) {
                    Text(text = stringResource(R.string.cancel_2))
                }
            }
        )
    }

    // OCR Text Preview & Word Export Dialog
    if (ocrPreviewDocument != null) {
        val doc = ocrPreviewDocument!!
        AlertDialog(
            onDismissRequest = { ocrPreviewDocument = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Extracted OCR Text",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF0F172A)
                    )
                    Surface(
                        color = Color(0xFFEEF2FF),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "ML Kit OCR",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryIndigo,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = doc.title,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF64748B)
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Supports English & mixed printed Bengali (Bangla) invoices. Ensure clear lighting for best recognition.",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (isOcrExtracting) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    color = PrimaryIndigo,
                                    strokeWidth = 3.dp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Extracting text using ML Kit OCR...",
                                    fontSize = 13.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    } else if (displayedOcrText.isBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No readable text detected in this document.",
                                fontSize = 13.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    } else {
                        OutlinedTextField(
                            value = displayedOcrText,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp, max = 260.dp),
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFFF8FAFC),
                                unfocusedContainerColor = Color(0xFFF8FAFC),
                                focusedBorderColor = Color(0xFFCBD5E1),
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (displayedOcrText.isNotBlank()) {
                        OutlinedButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(displayedOcrText))
                                Toast.makeText(context, "Text copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, PrimaryIndigo)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = PrimaryIndigo
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy", color = PrimaryIndigo, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                viewModel.shareDocx(context, doc)
                                ocrPreviewDocument = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Description,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Word (.docx)", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { ocrPreviewDocument = null }) {
                    Text("Close")
                }
            }
        )
    }

    // Password Protection Dialog
    if (documentToProtect != null) {
        val doc = documentToProtect!!
        AlertDialog(
            onDismissRequest = {
                if (!isProtectingPdf) {
                    documentToProtect = null
                }
            },
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFFEFF6FF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = PrimaryIndigo,
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "Protect PDF with Password",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Set a password to create an encrypted, password-protected copy of \"${doc.title}\". The original PDF will remain untouched.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Password field
                    OutlinedTextField(
                        value = protectPasswordInput,
                        onValueChange = {
                            protectPasswordInput = it
                            protectErrorMessage = null
                        },
                        label = { Text("Password") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isProtectingPdf,
                        visualTransformation = if (isProtectPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isProtectPasswordVisible = !isProtectPasswordVisible }) {
                                Icon(
                                    imageVector = if (isProtectPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (isProtectPasswordVisible) "Hide password" else "Show password",
                                    tint = Color(0xFF64748B)
                                )
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Confirm Password field
                    OutlinedTextField(
                        value = confirmPasswordInput,
                        onValueChange = {
                            confirmPasswordInput = it
                            protectErrorMessage = null
                        },
                        label = { Text("Confirm Password") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isProtectingPdf,
                        visualTransformation = if (isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isConfirmPasswordVisible = !isConfirmPasswordVisible }) {
                                Icon(
                                    imageVector = if (isConfirmPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (isConfirmPasswordVisible) "Hide password" else "Show password",
                                    tint = Color(0xFF64748B)
                                )
                            }
                        }
                    )

                    if (protectErrorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = protectErrorMessage!!,
                            color = Color(0xFFEF4444),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    if (isProtectingPdf) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = PrimaryIndigo
                            )
                            Text(
                                text = "Encrypting PDF...",
                                style = MaterialTheme.typography.bodySmall,
                                color = PrimaryIndigo,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pass = protectPasswordInput
                        val confirmPass = confirmPasswordInput
                        if (pass.isBlank()) {
                            protectErrorMessage = "Password cannot be blank"
                            return@Button
                        }
                        if (pass != confirmPass) {
                            protectErrorMessage = "Passwords do not match"
                            return@Button
                        }
                        if (pass.length < 4) {
                            protectErrorMessage = "Password must be at least 4 characters"
                            return@Button
                        }

                        isProtectingPdf = true
                        protectErrorMessage = null
                        viewModel.protectPdfWithPassword(context, doc, pass) { result ->
                            isProtectingPdf = false
                            result.onSuccess { protectedFile ->
                                documentToProtect = null
                                protectedPdfSuccessFile = protectedFile
                                Toast.makeText(context, "Password-protected PDF created", Toast.LENGTH_SHORT).show()
                            }.onFailure { err ->
                                protectErrorMessage = "Protection failed: ${err.localizedMessage ?: "Unknown error"}"
                            }
                        }
                    },
                    enabled = !isProtectingPdf,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                ) {
                    Text("Protect PDF", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { documentToProtect = null },
                    enabled = !isProtectingPdf
                ) {
                    Text(text = stringResource(R.string.cancel_2))
                }
            }
        )
    }

    // Protected PDF Ready / Share & Open Dialog
    if (protectedPdfSuccessFile != null) {
        val file = protectedPdfSuccessFile!!
        AlertDialog(
            onDismissRequest = { protectedPdfSuccessFile = null },
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFFECFDF5), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "PDF Protected Successfully",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Your document is now encrypted with 128-bit protection. Opening this file in any viewer will prompt for the password.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF475569)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = file.name,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = Color(0xFF1E293B)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Size: ${formatFileSize(file.length())}",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            viewModel.viewProtectedPdf(context, file)
                        },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, PrimaryIndigo)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Visibility,
                            contentDescription = null,
                            tint = PrimaryIndigo,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Open", color = PrimaryIndigo, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            viewModel.shareProtectedPdf(context, file)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share", color = Color.White, fontSize = 12.sp)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { protectedPdfSuccessFile = null }) {
                    Text("Done")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScannedDocumentItemCard(
    document: ScannedDocument,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelection: () -> Unit = {},
    onLongPress: () -> Unit = {},
    onView: () -> Unit,
    onShare: () -> Unit,
    onProtectPdf: () -> Unit,
    onOcrText: () -> Unit,
    onExportWord: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()) }
    val formattedDate = remember(document.createdAtMillis) {
        dateFormat.format(Date(document.createdAtMillis))
    }
    val formattedSize = remember(document.fileSizeBytes) {
        formatFileSize(document.fileSizeBytes)
    }

    val thumbnailBitmap = remember(document.thumbnailPath) {
        if (document.thumbnailPath.isNotBlank()) {
            val file = File(document.thumbnailPath)
            if (file.exists()) {
                try {
                    BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
                } catch (e: Exception) {
                    null
                }
            } else null
        } else null
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFF1F5F9) else PureWhite
        ),
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) BorderStroke(2.dp, PrimaryIndigo) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        onToggleSelection()
                    } else {
                        onView()
                    }
                },
                onLongClick = {
                    onLongPress()
                }
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Checkbox in Selection Mode
                if (isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelection() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = PrimaryIndigo,
                            checkmarkColor = Color.White
                        ),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }

                // Thumbnail or PDF Icon
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFEF2F2)),
                    contentAlignment = Alignment.Center
                ) {
                    if (thumbnailBitmap != null) {
                        Image(
                            bitmap = thumbnailBitmap,
                            contentDescription = "Document Thumbnail",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.PictureAsPdf,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = document.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        ),
                        color = Color(0xFF0F172A),
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color(0xFFEFF6FF),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "${document.pageCount} ${if (document.pageCount == 1) "Page" else "Pages"}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = PrimaryIndigo,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = formattedSize,
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formattedDate,
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                // Actions (Only visible when NOT in selection mode)
                if (!isSelectionMode) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onView) {
                            Icon(
                                imageVector = Icons.Filled.Visibility,
                                contentDescription = "View PDF",
                                tint = PrimaryIndigo
                            )
                        }
                        IconButton(onClick = onProtectPdf) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = "Protect PDF",
                                tint = PrimaryIndigo
                            )
                        }
                        IconButton(onClick = onShare) {
                            Icon(
                                imageVector = Icons.Filled.Share,
                                contentDescription = "Share PDF",
                                tint = Color(0xFF475569)
                            )
                        }
                        IconButton(onClick = onRename) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Rename",
                                tint = Color(0xFF64748B)
                            )
                        }
                        IconButton(onClick = onDelete) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Delete",
                                tint = Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }

            // Action chips for Protect PDF, OCR & Word Export (Only visible when NOT in selection mode)
            if (!isSelectionMode) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color(0xFFF1F5F9))
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = onProtectPdf,
                        color = Color(0xFFEFF6FF),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = "Protect PDF",
                                tint = PrimaryIndigo,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Protect",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryIndigo
                            )
                        }
                    }

                    Surface(
                        onClick = onOcrText,
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Article,
                                contentDescription = "OCR Text",
                                tint = Color(0xFF334155),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "OCR",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF334155)
                            )
                        }
                    }

                    Surface(
                        onClick = onExportWord,
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Description,
                                contentDescription = "Word Export",
                                tint = Color(0xFF475569),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Word",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF475569)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return DecimalFormat("#,##0.#").format(bytes / Math.pow(1024.0, digitGroups.toDouble())) + " " + units[digitGroups]
}

/**
 * Safely resolves the nearest Activity by unwrapping ContextWrapper hierarchies in Compose.
 */
private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

