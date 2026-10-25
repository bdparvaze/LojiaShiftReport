package com.lojia.shiftreport.settings

import android.net.Uri
import android.widget.Toast
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojia.shiftreport.R
import com.lojia.shiftreport.data.AppDatabase
import com.lojia.shiftreport.report.ReportViewModel
import com.lojia.shiftreport.sync.FirebaseCloudSyncManager
import com.lojia.shiftreport.ui.common.*
import com.lojia.shiftreport.ui.theme.*
import com.lojia.shiftreport.util.BackupInfo
import com.lojia.shiftreport.util.BackupSummary
import com.lojia.shiftreport.util.DriveBackupItem
import com.lojia.shiftreport.util.OfflineBackupManager
import com.lojia.shiftreport.util.PdfShareUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SettingsBackupSubSection(
    reportViewModel: ReportViewModel,
    onRestrictedClick: (action: () -> Unit) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isUninstallBackingUp by remember { mutableStateOf(false) }

    val isBackingUp by reportViewModel.isBackingUp.collectAsState()
    val backupProgress by reportViewModel.backupProgress.collectAsState()
    val lastBackupTime: Long by reportViewModel.lastBackupTime.collectAsState()
    val autoBackupFreq: String by reportViewModel.autoBackupFrequency.collectAsState()
    val driveAccount by reportViewModel.driveAccountInfo.collectAsState()
    val isDriveUploading by reportViewModel.isDriveUploading.collectAsState()
    val isDriveDownloading by reportViewModel.isDriveDownloading.collectAsState()
    val isDriveLoadingList by reportViewModel.isDriveLoadingList.collectAsState()
    val driveBackupsList by reportViewModel.driveBackupsList.collectAsState()
    val firebaseSyncState by reportViewModel.firebaseSyncState.collectAsState()

    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault()) }
    val lastBackupFormatted = remember(lastBackupTime) {
        if (lastBackupTime > 0L) dateFormatter.format(Date(lastBackupTime)) else "Never"
    }

    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var showClearAllDataConfirmDialog by remember { mutableStateOf(false) }
    var showAutoBackupDialog by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var pendingBackupSummary by remember { mutableStateOf<BackupSummary?>(null) }
    var lastCreatedBackupInfo by remember { mutableStateOf<BackupInfo?>(null) }

    var showDriveBackupsPicker by remember { mutableStateOf(false) }
    var pendingDriveBackupItem by remember { mutableStateOf<DriveBackupItem?>(null) }
    var pendingDriveBackupSummary by remember { mutableStateOf<BackupSummary?>(null) }
    var showDriveRestoreConfirmDialog by remember { mutableStateOf(false) }
    var isFetchingDriveSummary by remember { mutableStateOf(false) }

    val restoreFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingRestoreUri = uri
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
        LojiaSectionHeader("LOCAL BACKUP")

        // 1. Status Card
        LojiaSettingsCard {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (lastBackupTime > 0L) SuccessContainer else WarningContainer,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (lastBackupTime > 0L) Icons.Outlined.CheckCircle else Icons.Outlined.Schedule,
                    contentDescription = null,
                    tint = if (lastBackupTime > 0L) SuccessGreen else WarningOrange,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.last_backup),
                    fontSize = 13.sp,
                    color = OnSurfaceVariantLight
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (lastBackupTime > 0L) lastBackupFormatted else "Never",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceLight
                )
            }

            // Indicator Badge
            Surface(
                color = if (lastBackupTime > 0L) SuccessContainer else WarningContainer,
                shape = RoundedCornerShape(LojiaDimens.ChipRadius)
            ) {
                Text(
                    text = if (lastBackupTime > 0L) "Protected" else "Action Needed",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (lastBackupTime > 0L) SuccessGreen else WarningOrange
                )
            }
        }

        // 2. Primary Actions
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = LojiaDimens.CardSpacing),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    if (!isBackingUp) {
                        reportViewModel.createOfflineBackup(
                            onSuccess = { info -> lastCreatedBackupInfo = info }
                        )
                    }
                },
                enabled = !isBackingUp,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryIndigoLight,
                    contentColor = PureWhite
                ),
                shape = RoundedCornerShape(LojiaDimens.ButtonRadius),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("create_backup_btn")
            ) {
                if (isBackingUp) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.5.dp,
                        color = PureWhite
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.backup_in_progress),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.backup_data_title),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            OutlinedButton(
                onClick = {
                    restoreFileLauncher.launch("*/*")
                },
                shape = RoundedCornerShape(LojiaDimens.ButtonRadius),
                border = BorderStroke(1.dp, PrimaryIndigoLight),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryIndigoLight),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("restore_from_file_btn")
            ) {
                Icon(Icons.Outlined.Restore, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.restore_backup_title),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Before Uninstalling Helper Card
            Surface(
                shape = RoundedCornerShape(LojiaDimens.CardRadius),
                color = BackgroundLight,
                border = BorderStroke(1.dp, OutlineLight),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Security,
                            contentDescription = null,
                            tint = PrimaryIndigoLight,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(R.string.backup_before_uninstall_title),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnSurfaceLight
                        )
                    }
                    Text(
                        text = stringResource(R.string.backup_before_uninstall_desc),
                        fontSize = 12.sp,
                        color = TextSecondaryLight,
                        lineHeight = 16.sp
                    )
                    OutlinedButton(
                        onClick = {
                            if (!isUninstallBackingUp) {
                                isUninstallBackingUp = true
                                coroutineScope.launch {
                                    val result = OfflineBackupManager.createUninstallBackup(
                                        context,
                                        AppDatabase.getInstance(context)
                                    )
                                    isUninstallBackingUp = false
                                    result.onSuccess { info ->
                                        lastCreatedBackupInfo = info
                                        val msg = context.getString(R.string.backup_saved_to_path, info.savedPath)
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    }.onFailure { err ->
                                        Toast.makeText(context, err.localizedMessage ?: "Backup error", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        enabled = !isUninstallBackingUp,
                        shape = RoundedCornerShape(LojiaDimens.ButtonRadius),
                        border = BorderStroke(1.dp, PrimaryIndigoLight),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryIndigoLight),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("btn_create_backup_before_uninstall")
                    ) {
                        Icon(Icons.Outlined.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.btn_create_backup_before_uninstall),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            PdfShareUtils.openFolderInFileManager(
                                context,
                                OfflineBackupManager.getBackupsDirectory(context)
                            )
                        },
                        shape = RoundedCornerShape(LojiaDimens.ButtonRadius),
                        border = BorderStroke(1.dp, OutlineLight),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = OnSurfaceLight),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("btn_browse_backups")
                    ) {
                        Icon(Icons.Outlined.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.btn_browse_backups),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Recently Created Backup Banner Card (if active)
        val currentCreatedBackup = lastCreatedBackupInfo
        if (currentCreatedBackup != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SuccessContainer),
                shape = RoundedCornerShape(LojiaDimens.CardRadius),
                border = BorderStroke(1.dp, SuccessGreen),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = LojiaDimens.CardSpacing)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.backup_created_success, currentCreatedBackup.fileName),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SuccessGreen
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${currentCreatedBackup.savedPath} • ${currentCreatedBackup.totalRecords} records",
                            fontSize = 12.sp,
                            color = OnSurfaceLight
                        )
                    }
                    IconButton(onClick = { lastCreatedBackupInfo = null }) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cd_dismiss), tint = SuccessGreen, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        LojiaSectionHeader("AUTOMATIC BACKUP")

        // 3. Auto Backup Row
        LojiaSettingRow(
            icon = Icons.Outlined.Autorenew,
            title = stringResource(R.string.auto_backups),
            subtitle = when (autoBackupFreq) {
                "Daily" -> stringResource(R.string.daily)
                "Weekly" -> stringResource(R.string.weekly)
                "Monthly" -> stringResource(R.string.monthly)
                else -> autoBackupFreq ?: stringResource(R.string.daily)
            },
            trailing = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = TextHintColor,
                    modifier = Modifier.size(20.dp)
                )
            },
            onClick = { showAutoBackupDialog = true }
        )

        LojiaSectionHeader("CLOUD BACKUP & SYNC")

        // 4. Google Drive Card
        LojiaSettingsCard {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CloudQueue,
                            contentDescription = null,
                            tint = OnSurfaceVariantLight,
                            modifier = Modifier.size(LojiaDimens.IconSize)
                        )
                        Spacer(modifier = Modifier.width(LojiaDimens.IconTextGap))
                        Column {
                            Text(
                                text = "Google Drive",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = OnSurfaceLight
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (driveAccount.isConnected) {
                                    driveAccount.email ?: stringResource(R.string.drive_connected_as, "")
                                } else {
                                    stringResource(R.string.drive_not_connected)
                                },
                                fontSize = 13.sp,
                                color = if (driveAccount.isConnected) SuccessGreen else OnSurfaceVariantLight
                            )
                        }
                    }

                    if (driveAccount.isConnected) {
                        TextButton(
                            onClick = { reportViewModel.disconnectGoogleDrive() },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(stringResource(R.string.btn_disconnect_drive), color = ErrorRedLight, fontSize = 12.sp)
                        }
                    } else {
                        Button(
                            onClick = { reportViewModel.connectGoogleDrive(context) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryIndigoLight,
                                contentColor = PureWhite
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(stringResource(R.string.btn_connect_drive), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (driveAccount.isConnected) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Backup to Drive
                        Button(
                            onClick = { reportViewModel.backupToGoogleDrive() },
                            enabled = !isDriveUploading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryIndigoLight,
                                contentColor = PureWhite
                            ),
                            shape = RoundedCornerShape(LojiaDimens.ButtonRadius),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            if (isDriveUploading) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = PureWhite, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Outlined.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.backup_to_drive_title), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }

                        // Restore from Drive
                        OutlinedButton(
                            onClick = {
                                reportViewModel.fetchGoogleDriveBackups()
                                showDriveBackupsPicker = true
                            },
                            enabled = !isDriveDownloading && !isDriveLoadingList,
                            shape = RoundedCornerShape(LojiaDimens.ButtonRadius),
                            border = BorderStroke(1.dp, PrimaryIndigoLight),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryIndigoLight),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            if (isDriveDownloading || isDriveLoadingList) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = PrimaryIndigoLight, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Outlined.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.restore_from_drive_title), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }

        // 5. Cloud Sync / Firebase Card
        LojiaSettingsCard {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!com.lojia.shiftreport.BuildConfig.CLOUD_SYNC_ENABLED) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CloudSync,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(LojiaDimens.IconSize)
                        )
                        Spacer(modifier = Modifier.width(LojiaDimens.IconTextGap))
                        Column {
                            Text(
                                text = stringResource(R.string.firebase_sync_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(Dimens.SpacingXxs))
                            Text(
                                text = stringResource(R.string.cloud_sync_not_available),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Outlined.CloudSync,
                                contentDescription = null,
                                tint = OnSurfaceVariantLight,
                                modifier = Modifier.size(LojiaDimens.IconSize)
                            )
                            Spacer(modifier = Modifier.width(LojiaDimens.IconTextGap))
                            Column {
                                Text(
                                    text = stringResource(R.string.firebase_sync_title),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = OnSurfaceLight
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (firebaseSyncState.lastSyncTimestamp > 0L) {
                                        stringResource(R.string.firebase_sync_last_time, FirebaseCloudSyncManager.formatSyncTime(firebaseSyncState.lastSyncTimestamp))
                                    } else {
                                        stringResource(R.string.firebase_sync_never)
                                    },
                                    fontSize = 13.sp,
                                    color = OnSurfaceVariantLight
                                )
                            }
                        }

                        Switch(
                            checked = firebaseSyncState.isEnabled,
                            onCheckedChange = { isChecked ->
                                reportViewModel.toggleFirebaseCloudSync(isChecked)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PureWhite,
                                checkedTrackColor = PrimaryIndigoLight,
                                uncheckedThumbColor = PureWhite,
                                uncheckedTrackColor = OutlineLight
                            )
                        )
                    }

                    if (firebaseSyncState.isEnabled) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (firebaseSyncState.isSyncing) "Syncing in background..." else firebaseSyncState.statusMessage,
                                fontSize = 12.sp,
                                color = if (firebaseSyncState.isSyncing) InfoBlue else SuccessGreen
                            )

                            Button(
                                onClick = { reportViewModel.syncFirebaseNow() },
                                enabled = !firebaseSyncState.isSyncing,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PrimaryIndigoLight,
                                    contentColor = PureWhite
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                if (firebaseSyncState.isSyncing) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = PureWhite)
                                } else {
                                    Icon(Icons.Outlined.Sync, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.firebase_sync_now), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        LojiaSectionHeader("DANGER ZONE")

        // 6. Danger Zone: Clear Data
        Card(
            colors = CardDefaults.cardColors(containerColor = ErrorContainer),
            shape = RoundedCornerShape(LojiaDimens.CardRadius),
            border = BorderStroke(1.dp, ErrorRedLight),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(LojiaDimens.CardPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.clear_all_data_title),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = ErrorRedLight
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.clear_all_data_desc),
                        fontSize = 13.sp,
                        color = OnErrorContainerLight
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                OutlinedButton(
                    onClick = {
                        onRestrictedClick {
                            showClearAllDataConfirmDialog = true
                        }
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRedLight),
                    border = BorderStroke(1.dp, ErrorRedLight),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(stringResource(R.string.clear_all_data_title), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Auto Backup frequency picker dialog
    if (showAutoBackupDialog) {
        AlertDialog(
            onDismissRequest = { showAutoBackupDialog = false },
            containerColor = SurfaceLight,
            title = {
                Text(
                    text = stringResource(R.string.auto_backups),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceLight
                    )
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Daily", "Weekly", "Monthly").forEach { freq ->
                        val isSelected = autoBackupFreq == freq
                        Surface(
                            onClick = {
                                reportViewModel.autoBackupFrequency.value = freq
                                showAutoBackupDialog = false
                                Toast.makeText(context, context.getString(R.string.auto_backup_set_to, freq), Toast.LENGTH_SHORT).show()
                            },
                            color = if (isSelected) PrimaryContainerLight else Color.Transparent,
                            shape = RoundedCornerShape(10.dp),
                            border = if (isSelected) BorderStroke(1.dp, PrimaryIndigoLight) else null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = when (freq) {
                                        "Daily" -> stringResource(R.string.daily)
                                        "Weekly" -> stringResource(R.string.weekly)
                                        "Monthly" -> stringResource(R.string.monthly)
                                        else -> freq
                                    },
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isSelected) PrimaryIndigoLight else OnSurfaceLight
                                )
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = PrimaryIndigoLight, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAutoBackupDialog = false }) {
                    Text(stringResource(R.string.cancel_18), color = OnSurfaceVariantLight)
                }
            }
        )
    }

    // Restore Backup Confirmation Dialog
    val summary = pendingBackupSummary
    val restoreUri = pendingRestoreUri
    if (showRestoreConfirmDialog && restoreUri != null) {
        AlertDialog(
            onDismissRequest = {
                showRestoreConfirmDialog = false
                pendingRestoreUri = null
                pendingBackupSummary = null
            },
            containerColor = SurfaceLight,
            icon = {
                Icon(
                    Icons.Outlined.Restore,
                    contentDescription = null,
                    tint = WarningOrange,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.restore_confirm_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = OnSurfaceLight
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(R.string.restore_confirm_message),
                        fontSize = 14.sp,
                        color = OnSurfaceVariantLight,
                        lineHeight = 20.sp
                    )

                    if (summary != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = WarningContainer),
                            border = BorderStroke(1.dp, WarningOrange),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "📦 Backup Summary",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WarningOrange
                                )
                                Text(
                                    text = "• Shift Reports: ${summary.shiftReportsCount}",
                                    fontSize = 12.sp,
                                    color = WarningOrange
                                )
                                Text(
                                    text = "• Shift Sessions: ${summary.shiftSessionsCount} | Cashiers: ${summary.cashiersCount}",
                                    fontSize = 12.sp,
                                    color = WarningOrange
                                )
                                Text(
                                    text = "• Total Records: ${summary.totalRecords} (Date: ${summary.exportDate})",
                                    fontSize = 11.sp,
                                    color = WarningOrange
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreConfirmDialog = false
                        reportViewModel.restoreOfflineBackup(restoreUri)
                        pendingRestoreUri = null
                        pendingBackupSummary = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WarningOrange)
                ) {
                    Text(stringResource(R.string.btn_restore_now), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRestoreConfirmDialog = false
                        pendingRestoreUri = null
                        pendingBackupSummary = null
                    }
                ) {
                    Text(stringResource(R.string.cancel_18), color = OnSurfaceVariantLight)
                }
            }
        )
    }

    // Google Drive Backup Picker Dialog
    if (showDriveBackupsPicker) {
        AlertDialog(
            onDismissRequest = {
                showDriveBackupsPicker = false
                isFetchingDriveSummary = false
            },
            containerColor = SurfaceLight,
            icon = {
                Icon(
                    Icons.Outlined.CloudDownload,
                    contentDescription = null,
                    tint = PrimaryIndigoLight,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.drive_pick_backup_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = OnSurfaceLight
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isDriveLoadingList) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = PrimaryIndigoLight)
                        }
                    } else if (driveBackupsList.isEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = BackgroundLight),
                            border = BorderStroke(1.dp, OutlineLight),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.FolderOpen,
                                    contentDescription = null,
                                    tint = TextHintColor,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = stringResource(R.string.drive_no_backups_found),
                                    fontSize = 16.sp,
                                    color = OnSurfaceVariantLight,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Select a backup file from your Google Drive app folder:",
                            fontSize = 12.sp,
                            color = OnSurfaceVariantLight
                        )
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        ) {
                            driveBackupsList.forEach { item ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            pendingDriveBackupItem = item
                                            isFetchingDriveSummary = true
                                            reportViewModel.parseDriveBackupSummary(item) { summaryRes ->
                                                pendingDriveBackupSummary = summaryRes
                                                isFetchingDriveSummary = false
                                                showDriveBackupsPicker = false
                                                showDriveRestoreConfirmDialog = true
                                            }
                                        },
                                    colors = CardDefaults.cardColors(containerColor = SurfaceVariantLight),
                                    border = BorderStroke(1.dp, OutlineLight),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Outlined.Description, contentDescription = null, tint = PrimaryIndigoLight, modifier = Modifier.size(28.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.name,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = OnSurfaceLight
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "${item.description.ifEmpty { "Backup" }} • ${item.modifiedTime.take(16)}",
                                                fontSize = 11.sp,
                                                color = OnSurfaceVariantLight
                                            )
                                            if (item.size > 0) {
                                                Text(
                                                    text = "${item.size / 1024} KB",
                                                    fontSize = 10.sp,
                                                    color = TextHintColor
                                                )
                                            }
                                        }
                                        Icon(
                                            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                            contentDescription = null,
                                            tint = TextHintColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (isFetchingDriveSummary) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = PrimaryIndigoLight)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.reading_backup_details), fontSize = 12.sp, color = PrimaryIndigoLight)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDriveBackupsPicker = false
                        isFetchingDriveSummary = false
                    }
                ) {
                    Text(stringResource(R.string.cancel_18), color = OnSurfaceVariantLight)
                }
            }
        )
    }

    // Google Drive Restore Confirmation Dialog
    val driveItemToRestore = pendingDriveBackupItem
    val driveSummary = pendingDriveBackupSummary
    if (showDriveRestoreConfirmDialog && driveItemToRestore != null) {
        AlertDialog(
            onDismissRequest = {
                showDriveRestoreConfirmDialog = false
                pendingDriveBackupItem = null
                pendingDriveBackupSummary = null
            },
            containerColor = SurfaceLight,
            icon = {
                Icon(
                    Icons.Outlined.CloudDownload,
                    contentDescription = null,
                    tint = WarningOrange,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.drive_restore_confirm_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = OnSurfaceLight
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(R.string.drive_restore_confirm_message),
                        fontSize = 14.sp,
                        color = OnSurfaceVariantLight,
                        lineHeight = 20.sp
                    )

                    Card(
                        colors = CardDefaults.cardColors(containerColor = WarningContainer),
                        border = BorderStroke(1.dp, WarningOrange),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "☁️ Google Drive: ${driveItemToRestore.name}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = WarningOrange
                            )
                            if (driveSummary != null) {
                                Text(
                                    text = "• Shift Reports: ${driveSummary.shiftReportsCount}",
                                    fontSize = 12.sp,
                                    color = WarningOrange
                                )
                                Text(
                                    text = "• Shift Sessions: ${driveSummary.shiftSessionsCount} | Cashiers: ${driveSummary.cashiersCount}",
                                    fontSize = 12.sp,
                                    color = WarningOrange
                                )
                                Text(
                                    text = "• Total Records: ${driveSummary.totalRecords} (Export Date: ${driveSummary.exportDate})",
                                    fontSize = 11.sp,
                                    color = WarningOrange
                                )
                            } else {
                                Text(
                                    text = "Size: ${driveItemToRestore.size / 1024} KB • Modified: ${driveItemToRestore.modifiedTime}",
                                    fontSize = 11.sp,
                                    color = WarningOrange
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val item = driveItemToRestore
                        showDriveRestoreConfirmDialog = false
                        reportViewModel.restoreFromGoogleDrive(item)
                        pendingDriveBackupItem = null
                        pendingDriveBackupSummary = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WarningOrange)
                ) {
                    Text(stringResource(R.string.btn_restore_now), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDriveRestoreConfirmDialog = false
                        pendingDriveBackupItem = null
                        pendingDriveBackupSummary = null
                    }
                ) {
                    Text(stringResource(R.string.cancel_18), color = OnSurfaceVariantLight)
                }
            }
        )
    }

    // Clear All Business Data Confirmation Dialog (Danger Zone)
    if (showClearAllDataConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDataConfirmDialog = false },
            containerColor = SurfaceLight,
            icon = {
                Icon(
                    Icons.Outlined.WarningAmber,
                    contentDescription = null,
                    tint = ErrorRedLight,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.delete_all_data_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = OnSurfaceLight
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(R.string.delete_all_data_message),
                        fontSize = 14.sp,
                        color = OnSurfaceVariantLight,
                        lineHeight = 20.sp
                    )

                    Card(
                        colors = CardDefaults.cardColors(containerColor = PrimaryContainerLight),
                        border = BorderStroke(1.dp, PrimaryIndigoLight),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.Shield,
                                contentDescription = null,
                                tint = PrimaryIndigoLight,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.backup_recommended_warning),
                                fontSize = 12.sp,
                                color = OnPrimaryContainerLight,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            showClearAllDataConfirmDialog = false
                            reportViewModel.createOfflineBackup(
                                onSuccess = { info -> lastCreatedBackupInfo = info }
                            )
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryIndigoLight),
                        border = BorderStroke(1.dp, PrimaryIndigoLight)
                    ) {
                        Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.btn_backup_first), fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            showClearAllDataConfirmDialog = false
                            reportViewModel.clearAllBusinessData()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ErrorRedLight)
                    ) {
                        Text(stringResource(R.string.btn_delete_everything), fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearAllDataConfirmDialog = false }
                ) {
                    Text(stringResource(R.string.cancel_18), color = OnSurfaceVariantLight)
                }
            }
        )
    }
}
