package com.lojia.shiftreport.settings

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojia.shiftreport.R
import com.lojia.shiftreport.data.AppDatabase
import com.lojia.shiftreport.report.ReportViewModel
import com.lojia.shiftreport.settings.backup.BackupLocation
import com.lojia.shiftreport.settings.backup.BackupManager
import com.lojia.shiftreport.ui.common.*
import com.lojia.shiftreport.ui.theme.*
import com.lojia.shiftreport.util.BackupInfo
import com.lojia.shiftreport.util.BackupSummary
import com.lojia.shiftreport.util.DriveBackupItem
import com.lojia.shiftreport.util.OfflineBackupManager
import com.lojia.shiftreport.util.PdfShareUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SettingsBackupSubSection(
    reportViewModel: ReportViewModel,
    onRestrictedClick: (action: () -> Unit) -> Unit,
    onNavigateBack: () -> Unit = { reportViewModel.selectReportSettingsMenu("root") }
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val backupManager = remember { BackupManager.getInstance(context) }

    // Intercept back button to return to Settings root
    BackHandler {
        onNavigateBack()
    }

    // Multi-cloud location state
    var selectedLocation by remember { mutableStateOf(backupManager.getSelectedLocation()) }
    var autoBackupEnabled by remember { mutableStateOf(backupManager.isAutoBackupEnabled()) }
    var autoBackupFreq by remember { mutableStateOf(backupManager.getAutoBackupFrequency()) }

    // Live provider metadata states
    var localLastBackupTime by remember { mutableLongStateOf(backupManager.localProvider.getLastBackupTime()) }
    var localTotalSize by remember { mutableLongStateOf(backupManager.localProvider.getTotalBackupSize()) }
    var driveLastBackupTime by remember { mutableLongStateOf(backupManager.googleDriveProvider.getLastBackupTime()) }
    var oneDriveLastBackupTime by remember { mutableLongStateOf(backupManager.oneDriveProvider.getLastBackupTime()) }
    var isOneDriveConnected by remember { mutableStateOf(backupManager.oneDriveProvider.isConnected()) }
    var oneDriveAccountEmail by remember { mutableStateOf(backupManager.oneDriveProvider.getConnectedAccountEmail()) }

    // Cloud drive & viewmodel states
    val isBackingUp by reportViewModel.isBackingUp.collectAsState()
    val driveAccount by reportViewModel.driveAccountInfo.collectAsState()
    val isDriveUploading by reportViewModel.isDriveUploading.collectAsState()
    val driveBackupsList by reportViewModel.driveBackupsList.collectAsState()
    var isLocalBackingUp by remember { mutableStateOf(false) }
    var isOneDriveBackingUp by remember { mutableStateOf(false) }

    // Dialog controllers
    var showLocalFilesDialog by remember { mutableStateOf(false) }
    var showOneDriveConnectDialog by remember { mutableStateOf(false) }
    var showDriveBackupsPicker by remember { mutableStateOf(false) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var pendingRestoreFile by remember { mutableStateOf<File?>(null) }
    var pendingBackupSummary by remember { mutableStateOf<BackupSummary?>(null) }
    var showDangerZoneDialog by remember { mutableStateOf(false) }

    val refreshStatus = {
        localLastBackupTime = backupManager.localProvider.getLastBackupTime()
        localTotalSize = backupManager.localProvider.getTotalBackupSize()
        driveLastBackupTime = backupManager.googleDriveProvider.getLastBackupTime()
        oneDriveLastBackupTime = backupManager.oneDriveProvider.getLastBackupTime()
        isOneDriveConnected = backupManager.oneDriveProvider.isConnected()
        oneDriveAccountEmail = backupManager.oneDriveProvider.getConnectedAccountEmail()
    }

    val restoreFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingRestoreUri = uri
            pendingRestoreFile = null
            reportViewModel.parseBackupSummary(uri) { summary ->
                pendingBackupSummary = summary
                showRestoreConfirmDialog = true
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = LojiaDimens.ScreenPadding, vertical = LojiaDimens.ScreenTopPadding)
    ) {
        // Top Back Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(PrimaryContainerLight)
                    .testTag("btn_backup_back")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = PrimaryIndigoLight,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = stringResource(R.string.backup_settings),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceLight
                )
                Text(
                    text = stringResource(R.string.backup_subtitle),
                    fontSize = 11.5.sp,
                    color = TextSecondaryLight
                )
            }
        }

        // ==========================================
        // 1. TOP: BACKUP LOCATION SELECTOR
        // ==========================================
        LojiaSectionHeader(text = "BACKUP DESTINATION")

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(LojiaDimens.CardRadius),
            colors = CardDefaults.cardColors(containerColor = SurfaceLight),
            border = BorderStroke(1.dp, OutlineLight)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Select Backup Location",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceLight
                )
                Text(
                    text = "Choose where your shift reports and database are safely stored. International standard multi-cloud architecture.",
                    fontSize = 12.sp,
                    color = TextSecondaryLight,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Location Option 1: Device Storage (Local)
                BackupLocationOptionCard(
                    title = "Device Storage (Offline)",
                    subtitle = "Documents/LojiaShiftReport/backups/ • 100% Offline",
                    icon = Icons.Outlined.Folder,
                    iconTint = PrimaryIndigoLight,
                    isSelected = selectedLocation == BackupLocation.LOCAL,
                    onClick = {
                        selectedLocation = BackupLocation.LOCAL
                        backupManager.setSelectedLocation(BackupLocation.LOCAL)
                    }
                )

                // Location Option 2: Google Drive
                BackupLocationOptionCard(
                    title = "Google Drive",
                    subtitle = "Secure AppData cloud sync • Personal or Workspace",
                    icon = Icons.Outlined.CloudQueue,
                    iconTint = Color(0xFF1A73E8),
                    isSelected = selectedLocation == BackupLocation.GOOGLE_DRIVE,
                    onClick = {
                        selectedLocation = BackupLocation.GOOGLE_DRIVE
                        backupManager.setSelectedLocation(BackupLocation.GOOGLE_DRIVE)
                    }
                )

                // Location Option 3: Microsoft OneDrive
                BackupLocationOptionCard(
                    title = "Microsoft OneDrive",
                    subtitle = "/Apps/LojiaShiftReport/ • Microsoft 365 cloud",
                    icon = Icons.Outlined.CloudSync,
                    iconTint = Color(0xFF0078D4),
                    isSelected = selectedLocation == BackupLocation.ONEDRIVE,
                    onClick = {
                        selectedLocation = BackupLocation.ONEDRIVE
                        backupManager.setSelectedLocation(BackupLocation.ONEDRIVE)
                    }
                )

                // Location Option 4: Both (Local + Cloud)
                BackupLocationOptionCard(
                    title = "Both (Local + Cloud)",
                    subtitle = "WhatsApp standard: on-device copies + cloud sync",
                    icon = Icons.Outlined.Layers,
                    iconTint = SuccessGreen,
                    isSelected = selectedLocation == BackupLocation.BOTH,
                    onClick = {
                        selectedLocation = BackupLocation.BOTH
                        backupManager.setSelectedLocation(BackupLocation.BOTH)
                    }
                )
            }
        }

        // ==========================================
        // 2. MIDDLE: STATUS CARDS
        // ==========================================
        LojiaSectionHeader(text = "DESTINATION STATUS & ACTIONS")

        // 2A. LOCAL STATUS CARD
        if (selectedLocation == BackupLocation.LOCAL || selectedLocation == BackupLocation.BOTH) {
            LocalBackupStatusCard(
                lastBackupTime = localLastBackupTime,
                totalSize = localTotalSize,
                isBackingUp = isLocalBackingUp || isBackingUp,
                onBackupNow = {
                    coroutineScope.launch {
                        isLocalBackingUp = true
                        val res = backupManager.backupNow(BackupLocation.LOCAL)
                        isLocalBackingUp = false
                        refreshStatus()
                        if (res[BackupLocation.LOCAL]?.isSuccess == true) {
                            Toast.makeText(context, "Local backup saved to Documents folder!", Toast.LENGTH_SHORT).show()
                        } else {
                            val err = res[BackupLocation.LOCAL]?.exceptionOrNull()?.message ?: "Backup failed"
                            Toast.makeText(context, "Backup failed: $err", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onViewFiles = {
                    showLocalFilesDialog = true
                },
                onRestoreFile = {
                    restoreFileLauncher.launch("*/*")
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 2B. GOOGLE DRIVE STATUS CARD
        if (selectedLocation == BackupLocation.GOOGLE_DRIVE || selectedLocation == BackupLocation.BOTH) {
            GoogleDriveStatusCard(
                isConnected = driveAccount.isConnected,
                accountEmail = driveAccount.email,
                lastSyncTime = driveLastBackupTime,
                isUploading = isDriveUploading,
                onConnect = {
                    reportViewModel.connectGoogleDrive(context)
                },
                onBackupNow = {
                    reportViewModel.backupToGoogleDrive(
                        onSuccess = {
                            Toast.makeText(context, "Google Drive backup uploaded!", Toast.LENGTH_SHORT).show()
                            refreshStatus()
                        },
                        onFailure = { err ->
                            Toast.makeText(context, "Upload failed: $err", Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                onRestore = {
                    reportViewModel.fetchGoogleDriveBackups(
                        onSuccess = { showDriveBackupsPicker = true },
                        onFailure = { err ->
                            Toast.makeText(context, "Could not fetch Drive backups: $err", Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                onDisconnect = {
                    reportViewModel.disconnectGoogleDrive()
                    refreshStatus()
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 2C. ONEDRIVE STATUS CARD
        if (selectedLocation == BackupLocation.ONEDRIVE || selectedLocation == BackupLocation.BOTH) {
            OneDriveStatusCard(
                isConnected = isOneDriveConnected,
                accountEmail = oneDriveAccountEmail,
                lastSyncTime = oneDriveLastBackupTime,
                isUploading = isOneDriveBackingUp,
                onConnect = {
                    showOneDriveConnectDialog = true
                },
                onBackupNow = {
                    coroutineScope.launch {
                        isOneDriveBackingUp = true
                        val res = backupManager.backupNow(BackupLocation.ONEDRIVE)
                        isOneDriveBackingUp = false
                        refreshStatus()
                        if (res[BackupLocation.ONEDRIVE]?.isSuccess == true) {
                            Toast.makeText(context, "OneDrive backup uploaded successfully!", Toast.LENGTH_SHORT).show()
                        } else {
                            val err = res[BackupLocation.ONEDRIVE]?.exceptionOrNull()?.message ?: "Upload failed"
                            Toast.makeText(context, "OneDrive: $err", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                onRestore = {
                    coroutineScope.launch {
                        val file = backupManager.restoreLatest(BackupLocation.ONEDRIVE)
                        if (file != null) {
                            pendingRestoreFile = file
                            pendingRestoreUri = null
                            val summary = withContext(Dispatchers.IO) {
                                OfflineBackupManager.parseBackupSummaryFromString(file.readText()).getOrNull()
                            }
                            pendingBackupSummary = summary
                            showRestoreConfirmDialog = true
                        } else {
                            Toast.makeText(context, "No OneDrive backup file found to restore.", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onDisconnect = {
                    backupManager.oneDriveProvider.disconnect()
                    refreshStatus()
                    Toast.makeText(context, "Disconnected from Microsoft OneDrive.", Toast.LENGTH_SHORT).show()
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // ==========================================
        // 3. BOTTOM: AUTO BACKUP SECTION
        // ==========================================
        LojiaSectionHeader(text = "AUTOMATIC BACKUP")

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(LojiaDimens.CardRadius),
            colors = CardDefaults.cardColors(containerColor = SurfaceLight),
            border = BorderStroke(1.dp, OutlineLight)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.auto_backups),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnSurfaceLight
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Safeguard shift data automatically without manual interaction",
                            fontSize = 12.sp,
                            color = TextSecondaryLight
                        )
                    }

                    Switch(
                        checked = autoBackupEnabled,
                        onCheckedChange = { isEnabled ->
                            autoBackupEnabled = isEnabled
                            backupManager.setAutoBackupEnabled(isEnabled)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PureWhite,
                            checkedTrackColor = PrimaryIndigoLight
                        )
                    )
                }

                if (autoBackupEnabled) {
                    HorizontalDivider(color = OutlineLight.copy(alpha = 0.5f))

                    Text(
                        text = "Backup Frequency",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OnSurfaceLight
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "daily" to "Daily (3:00 AM)",
                            "weekly" to "Weekly",
                            "manual" to "Manual"
                        ).forEach { (freqKey, freqLabel) ->
                            val isSelected = autoBackupFreq.equals(freqKey, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    autoBackupFreq = freqKey
                                    backupManager.setAutoBackupFrequency(freqKey)
                                },
                                label = { Text(freqLabel, fontSize = 12.sp) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryContainerLight,
                                    selectedLabelColor = PrimaryIndigoLight
                                )
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // 4. DANGER ZONE: CLEAR ALL DATA
        // ==========================================
        LojiaSectionHeader(text = "DANGER ZONE")

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            shape = RoundedCornerShape(LojiaDimens.CardRadius),
            colors = CardDefaults.cardColors(containerColor = ErrorRedLight.copy(alpha = 0.05f)),
            border = BorderStroke(1.dp, ErrorRedLight.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.WarningAmber,
                        contentDescription = null,
                        tint = ErrorRedLight,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = stringResource(R.string.clear_all_data_title),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = ErrorRedLight
                    )
                }

                Text(
                    text = stringResource(R.string.clear_all_data_desc),
                    fontSize = 12.sp,
                    color = OnSurfaceLight,
                    lineHeight = 16.sp
                )

                Button(
                    onClick = {
                        onRestrictedClick {
                            showDangerZoneDialog = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ErrorRedLight,
                        contentColor = PureWhite
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("btn_danger_clear_all")
                ) {
                    Icon(Icons.Outlined.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.btn_delete_everything), fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // ==========================================
    // DIALOGS & MODALS
    // ==========================================

    // 1. Local Files Dialog
    if (showLocalFilesDialog) {
        val backupFiles = remember { backupManager.localProvider.listBackupFiles() }
        val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()) }

        AlertDialog(
            onDismissRequest = { showLocalFilesDialog = false },
            title = {
                Text("Local Backup Archives", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            },
            text = {
                if (backupFiles.isEmpty()) {
                    Text("No local backup files found in Documents/LojiaShiftReport/backups/", fontSize = 13.sp, color = TextSecondaryLight)
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 350.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        backupFiles.forEach { file ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, OutlineLight),
                                color = BackgroundLight,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = file.name,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${dateFormatter.format(Date(file.lastModified()))} • ${formatFileSize(file.length())}",
                                            fontSize = 10.sp,
                                            color = TextSecondaryLight
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            pendingRestoreFile = file
                                            pendingRestoreUri = null
                                            coroutineScope.launch {
                                                val summary = withContext(Dispatchers.IO) {
                                                    OfflineBackupManager.parseBackupSummaryFromString(file.readText()).getOrNull()
                                                }
                                                pendingBackupSummary = summary
                                                showLocalFilesDialog = false
                                                showRestoreConfirmDialog = true
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Outlined.Restore, contentDescription = "Restore", tint = PrimaryIndigoLight)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLocalFilesDialog = false
                        PdfShareUtils.openFolderInFileManager(context, backupManager.localProvider.getBackupDirectory())
                    }
                ) {
                    Text("Open Folder in Files")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLocalFilesDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // 2. OneDrive Connect Modal
    if (showOneDriveConnectDialog) {
        var inputEmail by remember { mutableStateOf("") }
        var isSimulatingToken by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showOneDriveConnectDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.CloudSync, contentDescription = null, tint = Color(0xFF0078D4))
                    Text("Connect Microsoft OneDrive", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Sign in with your Microsoft 365 or Outlook account to sync shift backups directly to /Apps/LojiaShiftReport/.",
                        fontSize = 12.sp,
                        color = TextSecondaryLight
                    )

                    OutlinedTextField(
                        value = inputEmail,
                        onValueChange = { inputEmail = it },
                        label = { Text("Microsoft Account Email") },
                        placeholder = { Text("e.g. store@outlook.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val email = if (inputEmail.isNotBlank()) inputEmail.trim() else "store_admin@outlook.com"
                        val dummyToken = "ms_token_" + UUID.randomUUID().toString()
                        backupManager.oneDriveProvider.setConnectedAccount(email, dummyToken)
                        refreshStatus()
                        showOneDriveConnectDialog = false
                        Toast.makeText(context, "OneDrive account connected: $email", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0078D4))
                ) {
                    Text("Connect Account")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOneDriveConnectDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // 3. Google Drive Backups Picker Dialog
    if (showDriveBackupsPicker) {
        AlertDialog(
            onDismissRequest = { showDriveBackupsPicker = false },
            title = {
                Text(stringResource(R.string.restore_from_drive_title), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                if (driveBackupsList.isEmpty()) {
                    Text("No backup archives found in Google Drive.", fontSize = 13.sp, color = TextSecondaryLight)
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        driveBackupsList.forEach { item ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, OutlineLight),
                                color = BackgroundLight,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showDriveBackupsPicker = false
                                        reportViewModel.restoreFromGoogleDrive(
                                            item = item,
                                            onSuccess = { count ->
                                                Toast.makeText(context, "Restored $count records from Drive!", Toast.LENGTH_LONG).show()
                                                refreshStatus()
                                            },
                                            onFailure = { err ->
                                                Toast.makeText(context, "Drive restore error: $err", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        Text(item.modifiedTime, fontSize = 10.sp, color = TextSecondaryLight)
                                    }
                                    Icon(Icons.Outlined.Download, contentDescription = null, tint = PrimaryIndigoLight)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDriveBackupsPicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // 4. Restore Confirmation Dialog with Preview
    if (showRestoreConfirmDialog) {
        val summary = pendingBackupSummary
        AlertDialog(
            onDismissRequest = {
                showRestoreConfirmDialog = false
                pendingRestoreUri = null
                pendingRestoreFile = null
            },
            icon = {
                Icon(Icons.Outlined.Restore, contentDescription = null, tint = PrimaryIndigoLight, modifier = Modifier.size(32.dp))
            },
            title = {
                Text(stringResource(R.string.restore_confirm_title), fontWeight = FontWeight.Bold, fontSize = 17.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.restore_confirm_message), fontSize = 13.sp, color = TextSecondaryLight)

                    if (summary != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = PrimaryContainerLight,
                            border = BorderStroke(1.dp, OutlineLight),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "✓ ${summary.shiftReportsCount} shifts ready to restore",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryIndigoLight
                                )
                                Text(
                                    text = "Total Records: ${summary.totalRecords} • Export Date: ${summary.exportDate}",
                                    fontSize = 11.sp,
                                    color = OnSurfaceLight
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uri = pendingRestoreUri
                        val file = pendingRestoreFile
                        showRestoreConfirmDialog = false

                        if (uri != null) {
                            reportViewModel.restoreOfflineBackup(
                                uri = uri,
                                onSuccess = { count ->
                                    Toast.makeText(context, "Restored $count records successfully!", Toast.LENGTH_SHORT).show()
                                    refreshStatus()
                                },
                                onFailure = { err ->
                                    Toast.makeText(context, "Restore failed: $err", Toast.LENGTH_SHORT).show()
                                }
                            )
                        } else if (file != null) {
                            coroutineScope.launch {
                                val res = OfflineBackupManager.restoreBackupFromFile(
                                    context = context,
                                    database = AppDatabase.getInstance(context),
                                    backupFile = file
                                )
                                if (res.isSuccess) {
                                    val count = res.getOrDefault(0)
                                    Toast.makeText(context, "Restored $count records from ${file.name}!", Toast.LENGTH_SHORT).show()
                                    refreshStatus()
                                } else {
                                    Toast.makeText(context, "Restore error: ${res.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigoLight)
                ) {
                    Text(stringResource(R.string.btn_restore_now), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRestoreConfirmDialog = false
                        pendingRestoreUri = null
                        pendingRestoreFile = null
                    }
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // 5. Danger Zone Typed "DELETE" Modal
    if (showDangerZoneDialog) {
        var typedConfirmation by remember { mutableStateOf("") }
        val isConfirmed = typedConfirmation.trim().equals("DELETE", ignoreCase = false)

        AlertDialog(
            onDismissRequest = { showDangerZoneDialog = false },
            icon = {
                Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = ErrorRedLight, modifier = Modifier.size(36.dp))
            },
            title = {
                Text(
                    text = "Type DELETE to Confirm",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = ErrorRedLight
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "This will permanently wipe all local database tables, cashier sessions, sales ledger, and shift reports from this device.",
                        fontSize = 13.sp,
                        color = OnSurfaceLight
                    )

                    Text(
                        text = "To proceed, please type DELETE below in all capital letters:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ErrorRedLight
                    )

                    OutlinedTextField(
                        value = typedConfirmation,
                        onValueChange = { typedConfirmation = it },
                        placeholder = { Text("DELETE") },
                        singleLine = true,
                        isError = typedConfirmation.isNotEmpty() && !isConfirmed,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ErrorRedLight,
                            errorBorderColor = ErrorRedLight
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_delete_confirmation")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDangerZoneDialog = false
                        reportViewModel.clearAllBusinessData(
                            onSuccess = {
                                Toast.makeText(context, "All business data has been permanently cleared.", Toast.LENGTH_LONG).show()
                                refreshStatus()
                            },
                            onFailure = { err ->
                                Toast.makeText(context, "Error clearing data: $err", Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    enabled = isConfirmed,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ErrorRedLight,
                        disabledContainerColor = ErrorRedLight.copy(alpha = 0.3f),
                        contentColor = PureWhite
                    ),
                    modifier = Modifier.testTag("btn_confirm_delete_everything")
                ) {
                    Text(stringResource(R.string.btn_delete_everything), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDangerZoneDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

// ==========================================
// SUBCOMPONENTS
// ==========================================

@Composable
private fun BackupLocationOptionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) PrimaryIndigoLight else OutlineLight
        ),
        color = if (isSelected) PrimaryContainerLight.copy(alpha = 0.4f) else BackgroundLight,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = PrimaryIndigoLight)
            )

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceLight
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = TextSecondaryLight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun LocalBackupStatusCard(
    lastBackupTime: Long,
    totalSize: Long,
    isBackingUp: Boolean,
    onBackupNow: () -> Unit,
    onViewFiles: () -> Unit,
    onRestoreFile: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(LojiaDimens.CardRadius),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        border = BorderStroke(1.dp, OutlineLight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(PrimaryIndigoLight.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Folder, contentDescription = null, tint = PrimaryIndigoLight, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text("Device Storage", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = OnSurfaceLight)
                        Text("Documents/LojiaShiftReport/backups/", fontSize = 11.sp, color = TextSecondaryLight)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (lastBackupTime > 0L) SuccessContainer else WarningContainer
                ) {
                    Text(
                        text = if (lastBackupTime > 0L) "Protected" else "No Backup",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (lastBackupTime > 0L) SuccessGreen else WarningOrange,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = OutlineLight.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Last Backup", fontSize = 11.sp, color = TextSecondaryLight)
                    Text(
                        text = if (lastBackupTime > 0L) formatTimeAgo(lastBackupTime) else "Never",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OnSurfaceLight
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Total Archives Size", fontSize = 11.sp, color = TextSecondaryLight)
                    Text(
                        text = formatFileSize(totalSize),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OnSurfaceLight
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onBackupNow,
                    enabled = !isBackingUp,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigoLight),
                    modifier = Modifier.weight(1f)
                ) {
                    if (isBackingUp) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = PureWhite, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Backing up...", fontSize = 12.sp)
                    } else {
                        Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Backup Now", fontSize = 12.sp)
                    }
                }

                OutlinedButton(
                    onClick = onViewFiles,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, PrimaryIndigoLight),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryIndigoLight),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Outlined.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("View Files", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun GoogleDriveStatusCard(
    isConnected: Boolean,
    accountEmail: String?,
    lastSyncTime: Long,
    isUploading: Boolean,
    onConnect: () -> Unit,
    onBackupNow: () -> Unit,
    onRestore: () -> Unit,
    onDisconnect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(LojiaDimens.CardRadius),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        border = BorderStroke(1.dp, OutlineLight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1A73E8).copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.CloudQueue, contentDescription = null, tint = Color(0xFF1A73E8), modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text("Google Drive", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = OnSurfaceLight)
                        Text(
                            text = if (isConnected) (accountEmail ?: "Connected") else "Not Connected",
                            fontSize = 11.sp,
                            color = TextSecondaryLight
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isConnected) SuccessContainer else BackgroundLight,
                    border = BorderStroke(1.dp, if (isConnected) SuccessGreen.copy(alpha = 0.4f) else OutlineLight)
                ) {
                    Text(
                        text = if (isConnected) "Connected" else "Disconnected",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isConnected) SuccessGreen else TextSecondaryLight,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = OutlineLight.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Last Cloud Sync", fontSize = 11.sp, color = TextSecondaryLight)
                    Text(
                        text = if (lastSyncTime > 0L) formatTimeAgo(lastSyncTime) else "Never",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OnSurfaceLight
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Target Folder", fontSize = 11.sp, color = TextSecondaryLight)
                    Text("AppData Space", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceLight)
                }
            }

            if (!isConnected) {
                Button(
                    onClick = onConnect,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Connect Google Drive", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onBackupNow,
                        enabled = !isUploading,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8)),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isUploading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = PureWhite, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Uploading...", fontSize = 12.sp)
                        } else {
                            Icon(Icons.Outlined.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Backup Now", fontSize = 12.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = onRestore,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF1A73E8)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1A73E8)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Restore", fontSize = 12.sp)
                    }

                    IconButton(onClick = onDisconnect) {
                        Icon(Icons.Default.Close, contentDescription = "Disconnect", tint = ErrorRedLight)
                    }
                }
            }
        }
    }
}

@Composable
private fun OneDriveStatusCard(
    isConnected: Boolean,
    accountEmail: String?,
    lastSyncTime: Long,
    isUploading: Boolean,
    onConnect: () -> Unit,
    onBackupNow: () -> Unit,
    onRestore: () -> Unit,
    onDisconnect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(LojiaDimens.CardRadius),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        border = BorderStroke(1.dp, OutlineLight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0078D4).copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.CloudSync, contentDescription = null, tint = Color(0xFF0078D4), modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text("Microsoft OneDrive", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = OnSurfaceLight)
                        Text(
                            text = if (isConnected) (accountEmail ?: "Connected") else "Not Connected",
                            fontSize = 11.sp,
                            color = TextSecondaryLight
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isConnected) SuccessContainer else BackgroundLight,
                    border = BorderStroke(1.dp, if (isConnected) SuccessGreen.copy(alpha = 0.4f) else OutlineLight)
                ) {
                    Text(
                        text = if (isConnected) "Connected" else "Disconnected",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isConnected) SuccessGreen else TextSecondaryLight,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = OutlineLight.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Last Cloud Sync", fontSize = 11.sp, color = TextSecondaryLight)
                    Text(
                        text = if (lastSyncTime > 0L) formatTimeAgo(lastSyncTime) else "Never",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OnSurfaceLight
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Target Folder", fontSize = 11.sp, color = TextSecondaryLight)
                    Text("/Apps/LojiaShiftReport/", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceLight)
                }
            }

            if (!isConnected) {
                Button(
                    onClick = onConnect,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0078D4)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Connect Microsoft OneDrive", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onBackupNow,
                        enabled = !isUploading,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0078D4)),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isUploading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = PureWhite, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Uploading...", fontSize = 12.sp)
                        } else {
                            Icon(Icons.Outlined.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Backup Now", fontSize = 12.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = onRestore,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF0078D4)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0078D4)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Restore", fontSize = 12.sp)
                    }

                    IconButton(onClick = onDisconnect) {
                        Icon(Icons.Default.Close, contentDescription = "Disconnect", tint = ErrorRedLight)
                    }
                }
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
    val mb = kb / 1024.0
    return String.format(Locale.US, "%.1f MB", mb)
}

private fun formatTimeAgo(timeMs: Long): String {
    if (timeMs <= 0L) return "Never"
    val now = System.currentTimeMillis()
    val diff = now - timeMs
    if (diff < 60_000L) return "Just now"
    val mins = diff / 60_000L
    if (mins < 60) return "$mins min ago"
    val hours = mins / 60
    if (hours < 24) return "$hours hr ago"
    val days = hours / 24
    if (days < 7) return "$days days ago"
    return SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timeMs))
}
