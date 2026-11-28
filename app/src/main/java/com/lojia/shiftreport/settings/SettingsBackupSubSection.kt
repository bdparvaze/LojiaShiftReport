package com.lojia.shiftreport.settings

import android.content.Context
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
import com.lojia.shiftreport.R
import com.lojia.shiftreport.auth.AdminAuthDialog
import com.lojia.shiftreport.data.AppDatabase
import com.lojia.shiftreport.report.ReportViewModel
import com.lojia.shiftreport.settings.backup.BackupLocation
import com.lojia.shiftreport.settings.backup.BackupManager
import com.lojia.shiftreport.ui.common.*
import com.lojia.shiftreport.ui.theme.*
import com.lojia.shiftreport.util.BackupSummary
import com.lojia.shiftreport.util.DateTimeFormatUtils
import com.lojia.shiftreport.util.DriveBackupItem
import com.lojia.shiftreport.util.OfflineBackupManager
import com.lojia.shiftreport.util.PdfShareUtils
import com.lojia.shiftreport.util.TIER_PORTABLE
import com.lojia.shiftreport.util.backupTierOf
import com.lojia.shiftreport.util.canCreatePortableBackup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

private sealed interface PendingPasswordRestoreTarget {
    val label: String?

    data class LocalFile(val file: File) : PendingPasswordRestoreTarget {
        override val label: String get() = file.name
    }

    data class PickedUri(val uri: Uri, override val label: String?) : PendingPasswordRestoreTarget

    class DriveBytes(val item: DriveBackupItem, val bytes: ByteArray) : PendingPasswordRestoreTarget {
        override val label: String get() = item.name
    }
}

@Composable
fun SettingsBackupSubSection(
    reportViewModel: ReportViewModel,
    onRestrictedClick: (action: () -> Unit) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val backupManager = remember { BackupManager.getInstance(context) }

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

    var hasBackupPassword by remember { mutableStateOf(false) }

    // Dialog controllers
    var showLocalFilesDialog by remember { mutableStateOf(false) }
    var showOneDriveConnectDialog by remember { mutableStateOf(false) }
    var showDriveBackupsPicker by remember { mutableStateOf(false) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var pendingRestoreFile by remember { mutableStateOf<File?>(null) }
    var pendingRestoreRequiresPassword by remember { mutableStateOf(false) }
    var pendingBackupSummary by remember { mutableStateOf<BackupSummary?>(null) }
    var showDangerZoneDialog by remember { mutableStateOf(false) }
    var showBackupPasswordDialog by remember { mutableStateOf(false) }
    var showSetPasswordFirstDialog by remember { mutableStateOf(false) }
    var restorePwTarget by remember { mutableStateOf<PendingPasswordRestoreTarget?>(null) }
    var isRestoreUnlocking by remember { mutableStateOf(false) }
    var restorePasswordError by remember { mutableStateOf<String?>(null) }
    var pendingAdminAuthTitle by remember { mutableStateOf<String?>(null) }
    var pendingAdminAuthAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    LaunchedEffect(Unit) {
        hasBackupPassword = canCreatePortableBackup(context)
    }

    val refreshStatus = {
        localLastBackupTime = backupManager.localProvider.getLastBackupTime()
        localTotalSize = backupManager.localProvider.getTotalBackupSize()
        driveLastBackupTime = backupManager.googleDriveProvider.getLastBackupTime()
        oneDriveLastBackupTime = backupManager.oneDriveProvider.getLastBackupTime()
        isOneDriveConnected = backupManager.oneDriveProvider.isConnected()
        oneDriveAccountEmail = backupManager.oneDriveProvider.getConnectedAccountEmail()
        coroutineScope.launch {
            hasBackupPassword = canCreatePortableBackup(context)
        }
        Unit
    }

    val openAdminForBackupPassword: () -> Unit = {
        pendingAdminAuthTitle = context.getString(
            if (hasBackupPassword) R.string.backup_pw_dialog_change_title
            else R.string.backup_pw_dialog_set_title
        )
        pendingAdminAuthAction = {
            showBackupPasswordDialog = true
        }
    }

    fun runWithPortableBackupGate(action: () -> Unit) {
        coroutineScope.launch {
            val ready = canCreatePortableBackup(context)
            hasBackupPassword = ready
            if (ready) {
                action()
            } else {
                showSetPasswordFirstDialog = true
            }
        }
    }

    val restoreFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingRestoreUri = uri
            pendingRestoreFile = null
            coroutineScope.launch {
                val (requiresPassword, summary) = withContext(Dispatchers.IO) {
                    val bytes = runCatching {
                        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    }.getOrNull()
                    if (bytes == null) {
                        false to null
                    } else {
                        val portable = backupTierOf(bytes) == TIER_PORTABLE
                        val parsedSummary = if (!portable) {
                            OfflineBackupManager.parseBackupSummaryFromBytes(bytes).getOrNull()
                        } else {
                            null
                        }
                        portable to parsedSummary
                    }
                }
                pendingRestoreRequiresPassword = requiresPassword
                pendingBackupSummary = summary
                showRestoreConfirmDialog = true
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.SpacingLg, vertical = Dimens.SpacingMd)
    ) {
        // ==========================================
        // 0. BACKUP ENCRYPTION PASSWORD (ADMIN-GATED)
        // ==========================================
        LojiaSectionHeader(text = stringResource(R.string.backup_pw_section_header))

        BackupPasswordCard(
            hasBackupPassword = hasBackupPassword,
            onManagePasswordClick = openAdminForBackupPassword
        )

        // ==========================================
        // 1. TOP: BACKUP LOCATION SELECTOR
        // ==========================================
        LojiaSectionHeader(text = stringResource(R.string.backup_section_destination))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Dimens.SpacingLg)
                .cardRise(0)
                .depthCard()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.CardPadding),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)
            ) {
                Text(
                    text = stringResource(R.string.backup_select_location_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceLight
                )
                Text(
                    text = stringResource(R.string.backup_select_location_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondaryLight
                )

                Spacer(modifier = Modifier.height(Dimens.SpacingXs))

                // Location Option 1: Device Storage (Local)
                BackupLocationOptionCard(
                    title = stringResource(R.string.backup_loc_device_title),
                    subtitle = stringResource(R.string.backup_loc_device_subtitle),
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
                    title = stringResource(R.string.backup_loc_gdrive_title),
                    subtitle = stringResource(R.string.backup_loc_gdrive_subtitle),
                    icon = Icons.Outlined.CloudQueue,
                    iconTint = GoogleDriveBlue,
                    isSelected = selectedLocation == BackupLocation.GOOGLE_DRIVE,
                    onClick = {
                        selectedLocation = BackupLocation.GOOGLE_DRIVE
                        backupManager.setSelectedLocation(BackupLocation.GOOGLE_DRIVE)
                    }
                )

                // Location Option 3: Microsoft OneDrive
                BackupLocationOptionCard(
                    title = stringResource(R.string.backup_loc_onedrive_title),
                    subtitle = stringResource(R.string.backup_loc_onedrive_subtitle),
                    icon = Icons.Outlined.CloudSync,
                    iconTint = OneDriveBlue,
                    isSelected = selectedLocation == BackupLocation.ONEDRIVE,
                    onClick = {
                        selectedLocation = BackupLocation.ONEDRIVE
                        backupManager.setSelectedLocation(BackupLocation.ONEDRIVE)
                    }
                )

                // Location Option 4: Both (Local + Cloud)
                BackupLocationOptionCard(
                    title = stringResource(R.string.backup_loc_both_title),
                    subtitle = stringResource(R.string.backup_loc_both_subtitle),
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
        LojiaSectionHeader(text = stringResource(R.string.backup_section_status_actions))

        // 2A. LOCAL STATUS CARD
        if (selectedLocation == BackupLocation.LOCAL || selectedLocation == BackupLocation.BOTH) {
            LocalBackupStatusCard(
                lastBackupTime = localLastBackupTime,
                totalSize = localTotalSize,
                isBackingUp = isLocalBackingUp || isBackingUp,
                onBackupNow = {
                    runWithPortableBackupGate {
                        coroutineScope.launch {
                            isLocalBackingUp = true
                            val res = backupManager.backupNow(BackupLocation.LOCAL)
                            isLocalBackingUp = false
                            refreshStatus()
                            if (res[BackupLocation.LOCAL]?.isSuccess == true) {
                                Toast.makeText(context, context.getString(R.string.backup_local_saved_toast), Toast.LENGTH_SHORT).show()
                            } else {
                                val err = res[BackupLocation.LOCAL]?.exceptionOrNull()?.message
                                    ?: context.getString(R.string.backup_failed_generic)
                                Toast.makeText(context, context.getString(R.string.backup_failed_format, err), Toast.LENGTH_SHORT).show()
                            }
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
            Spacer(modifier = Modifier.height(Dimens.SpacingMd))
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
                    runWithPortableBackupGate {
                        reportViewModel.backupToGoogleDrive(
                            onSuccess = {
                                Toast.makeText(context, context.getString(R.string.backup_gdrive_uploaded_toast), Toast.LENGTH_SHORT).show()
                                refreshStatus()
                            },
                            onFailure = { err ->
                                Toast.makeText(context, context.getString(R.string.backup_upload_failed_format, err), Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                },
                onRestore = {
                    reportViewModel.fetchGoogleDriveBackups(
                        onSuccess = { showDriveBackupsPicker = true },
                        onFailure = { err ->
                            Toast.makeText(context, context.getString(R.string.backup_gdrive_fetch_failed_format, err), Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                onDisconnect = {
                    reportViewModel.disconnectGoogleDrive()
                    refreshStatus()
                }
            )
            Spacer(modifier = Modifier.height(Dimens.SpacingMd))
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
                    runWithPortableBackupGate {
                        coroutineScope.launch {
                            isOneDriveBackingUp = true
                            val res = backupManager.backupNow(BackupLocation.ONEDRIVE)
                            isOneDriveBackingUp = false
                            refreshStatus()
                            if (res[BackupLocation.ONEDRIVE]?.isSuccess == true) {
                                Toast.makeText(context, context.getString(R.string.backup_onedrive_uploaded_toast), Toast.LENGTH_SHORT).show()
                            } else {
                                val err = res[BackupLocation.ONEDRIVE]?.exceptionOrNull()?.message
                                    ?: context.getString(R.string.backup_upload_failed_generic)
                                Toast.makeText(context, context.getString(R.string.backup_onedrive_error_format, err), Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                },
                onRestore = {
                    coroutineScope.launch {
                        val file = backupManager.restoreLatest(BackupLocation.ONEDRIVE)
                        if (file != null) {
                            pendingRestoreFile = file
                            pendingRestoreUri = null
                            val (requiresPassword, summary) = withContext(Dispatchers.IO) {
                                val bytes = runCatching { file.readBytes() }.getOrNull()
                                if (bytes == null) {
                                    false to null
                                } else {
                                    val portable = backupTierOf(bytes) == TIER_PORTABLE
                                    val parsedSummary = if (!portable) {
                                        OfflineBackupManager.parseBackupSummaryFromBytes(bytes).getOrNull()
                                    } else {
                                        null
                                    }
                                    portable to parsedSummary
                                }
                            }
                            pendingRestoreRequiresPassword = requiresPassword
                            pendingBackupSummary = summary
                            showRestoreConfirmDialog = true
                        } else {
                            Toast.makeText(context, context.getString(R.string.backup_onedrive_none_found_toast), Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onDisconnect = {
                    backupManager.oneDriveProvider.disconnect()
                    refreshStatus()
                    Toast.makeText(context, context.getString(R.string.backup_onedrive_disconnected_toast), Toast.LENGTH_SHORT).show()
                }
            )
            Spacer(modifier = Modifier.height(Dimens.SpacingMd))
        }

        // ==========================================
        // 3. BOTTOM: AUTO BACKUP SECTION
        // ==========================================
        LojiaSectionHeader(text = stringResource(R.string.backup_section_automatic))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Dimens.SpacingLg)
                .cardRise(2)
                .depthCard()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.CardPadding),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMd)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.auto_backups),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = OnSurfaceLight
                        )
                        Spacer(modifier = Modifier.height(Dimens.SpacingXxs))
                        Text(
                            text = stringResource(R.string.backup_auto_desc),
                            style = MaterialTheme.typography.bodyMedium,
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
                        text = stringResource(R.string.backup_frequency_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = OnSurfaceLight
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)
                    ) {
                        listOf(
                            "daily" to stringResource(R.string.backup_freq_daily_3am),
                            "weekly" to stringResource(R.string.weekly),
                            "manual" to stringResource(R.string.backup_freq_manual)
                        ).forEach { (freqKey, freqLabel) ->
                            val isSelected = autoBackupFreq.equals(freqKey, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    autoBackupFreq = freqKey
                                    backupManager.setAutoBackupFrequency(freqKey)
                                },
                                label = { Text(freqLabel, style = MaterialTheme.typography.bodyMedium) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(Dimens.IconSm)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryContainerLight,
                                    selectedLabelColor = PrimaryIndigoDark
                                ),
                                modifier = Modifier.heightIn(min = Dimens.MinTouchTarget)
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // 4. DANGER ZONE: CLEAR ALL DATA
        // ==========================================
        LojiaSectionHeader(text = stringResource(R.string.backup_section_danger_zone))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Dimens.SpacingHuge)
                .cardRise(3)
                .depthSemanticCard(
                    backgroundColor = NegativeBannerSurface,
                    borderColor = NegativeBannerBorder
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.CardPadding),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.WarningAmber,
                        contentDescription = null,
                        tint = ErrorRedLight,
                        modifier = Modifier.size(Dimens.IconAction)
                    )
                    Text(
                        text = stringResource(R.string.clear_all_data_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = ErrorRedLight
                    )
                }

                Text(
                    text = stringResource(R.string.clear_all_data_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceLight
                )

                Button(
                    onClick = {
                        showDangerZoneDialog = true
                    },
                    elevation = null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = PureWhite
                    ),
                    shape = RoundedCornerShape(Dimens.RadiusMd),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = Dimens.MinTouchTarget)
                        .depthRaisedButton(
                            enabled = true,
                            isDestructive = true,
                            cornerRadius = Dimens.RadiusMd
                        )
                        .testTag("btn_danger_clear_all")
                ) {
                    Icon(Icons.Outlined.DeleteForever, contentDescription = null, modifier = Modifier.size(Dimens.IconMd))
                    Spacer(modifier = Modifier.width(Dimens.SpacingSm))
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

        LojiaDialog(
            onDismissRequest = { showLocalFilesDialog = false },
            size = DialogSize.Large
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMd)
            ) {
                Text(stringResource(R.string.backup_local_archives_title), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall, color = OnBackgroundLight)

                if (backupFiles.isEmpty()) {
                    Text(stringResource(R.string.backup_local_none_found), style = MaterialTheme.typography.titleMedium, color = TextSecondaryLight)
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = Dimens.DialogListMaxHeight)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)
                    ) {
                        backupFiles.forEach { file ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .depthCard(shape = RoundedCornerShape(Dimens.RadiusSm))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(Dimens.SpacingSm),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = file.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${DateTimeFormatUtils.formatDateTime(file.lastModified())} • ${formatFileSize(context, file.length())}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondaryLight
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            pendingRestoreFile = file
                                            pendingRestoreUri = null
                                            coroutineScope.launch {
                                                val (requiresPassword, summary) = withContext(Dispatchers.IO) {
                                                    val bytes = runCatching { file.readBytes() }.getOrNull()
                                                    if (bytes == null) {
                                                        false to null
                                                    } else {
                                                        val portable = backupTierOf(bytes) == TIER_PORTABLE
                                                        val parsedSummary = if (!portable) {
                                                            OfflineBackupManager.parseBackupSummaryFromBytes(bytes).getOrNull()
                                                        } else {
                                                            null
                                                        }
                                                        portable to parsedSummary
                                                    }
                                                }
                                                pendingRestoreRequiresPassword = requiresPassword
                                                pendingBackupSummary = summary
                                                showLocalFilesDialog = false
                                                showRestoreConfirmDialog = true
                                            }
                                        },
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Icon(Icons.Outlined.Restore, contentDescription = stringResource(R.string.action_unarchive), tint = PrimaryIndigoLight)
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            showLocalFilesDialog = false
                            PdfShareUtils.openFolderInFileManager(context, backupManager.localProvider.getBackupDirectory())
                        },
                        elevation = null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = PureWhite
                        ),
                        shape = RoundedCornerShape(Dimens.RadiusMd),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = Dimens.MinTouchTarget)
                            .depthRaisedButton(enabled = true, cornerRadius = Dimens.RadiusMd)
                    ) {
                        Text(
                            text = stringResource(R.string.backup_open_folder_files),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    OutlinedButton(
                        onClick = { showLocalFilesDialog = false },
                        shape = RoundedCornerShape(Dimens.RadiusMd),
                        border = null,
                        elevation = null,
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = OnSurfaceVariantLight
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = Dimens.MinTouchTarget)
                            .depthSecondaryButton(cornerRadius = Dimens.RadiusMd)
                    ) {
                        Text(
                            text = stringResource(R.string.action_cancel),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }

    // 2. OneDrive Connect Modal
    if (showOneDriveConnectDialog) {
        var inputEmail by remember { mutableStateOf("") }
        var isSimulatingToken by remember { mutableStateOf(false) }

        LojiaDialog(
            onDismissRequest = { showOneDriveConnectDialog = false },
            size = DialogSize.Medium
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMd)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)) {
                    Icon(Icons.Outlined.CloudSync, contentDescription = null, tint = OneDriveBlue)
                    Text(stringResource(R.string.backup_connect_onedrive), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall, color = OnBackgroundLight)
                }

                Text(
                    text = stringResource(R.string.backup_onedrive_connect_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondaryLight
                )

                LojiaTextField(
                    value = inputEmail,
                    onValueChange = { inputEmail = it },
                    label = { Text(stringResource(R.string.backup_onedrive_email_label)) },
                    placeholder = { Text(stringResource(R.string.backup_onedrive_email_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            val email = if (inputEmail.isNotBlank()) inputEmail.trim() else "store_admin@outlook.com"
                            val dummyToken = "ms_token_" + UUID.randomUUID().toString()
                            backupManager.oneDriveProvider.setConnectedAccount(email, dummyToken)
                            refreshStatus()
                            showOneDriveConnectDialog = false
                            Toast.makeText(context, context.getString(R.string.backup_onedrive_connected_format, email), Toast.LENGTH_SHORT).show()
                        },
                        elevation = null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = PureWhite
                        ),
                        shape = RoundedCornerShape(Dimens.RadiusMd),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = Dimens.MinTouchTarget)
                            .depthRaisedButton(enabled = true, cornerRadius = Dimens.RadiusMd)
                    ) {
                        Text(
                            text = stringResource(R.string.backup_connect_account_btn),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    OutlinedButton(
                        onClick = { showOneDriveConnectDialog = false },
                        shape = RoundedCornerShape(Dimens.RadiusMd),
                        border = null,
                        elevation = null,
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = OnSurfaceVariantLight
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = Dimens.MinTouchTarget)
                            .depthSecondaryButton(cornerRadius = Dimens.RadiusMd)
                    ) {
                        Text(
                            text = stringResource(R.string.action_cancel),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }

    // 3. Google Drive Backups Picker Dialog
    if (showDriveBackupsPicker) {
        LojiaDialog(
            onDismissRequest = { showDriveBackupsPicker = false },
            size = DialogSize.Large
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMd)
            ) {
                Text(stringResource(R.string.restore_from_drive_title), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall, color = OnBackgroundLight)

                if (driveBackupsList.isEmpty()) {
                    Text(stringResource(R.string.backup_gdrive_none_found), style = MaterialTheme.typography.titleMedium, color = TextSecondaryLight)
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = Dimens.DialogListCompactMaxHeight)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)
                    ) {
                        driveBackupsList.forEach { item ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = Dimens.MinTouchTarget)
                                    .depthCard(shape = RoundedCornerShape(Dimens.RadiusSm))
                                    .clickable {
                                        showDriveBackupsPicker = false
                                        pendingAdminAuthTitle = context.getString(R.string.restore_from_drive_title)
                                        pendingAdminAuthAction = {
                                            reportViewModel.prepareGoogleDriveRestore(
                                                item = item,
                                                onReady = { bytes, requiresPassword ->
                                                    if (requiresPassword) {
                                                        restorePasswordError = null
                                                        restorePwTarget = PendingPasswordRestoreTarget.DriveBytes(item, bytes)
                                                    } else {
                                                        reportViewModel.restoreGoogleDriveBytes(
                                                            item = item,
                                                            bytes = bytes,
                                                            password = null,
                                                            onSuccess = { count ->
                                                                Toast.makeText(context, context.getString(R.string.data_restored_success, count), Toast.LENGTH_LONG).show()
                                                                refreshStatus()
                                                            },
                                                            onFailure = { err ->
                                                                Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                                            }
                                                        )
                                                    }
                                                },
                                                onFailure = { err ->
                                                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(Dimens.CardPadding),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text(item.modifiedTime, style = MaterialTheme.typography.bodySmall, color = TextSecondaryLight)
                                    }
                                    Icon(Icons.Outlined.Download, contentDescription = null, tint = PrimaryIndigoLight)
                                }
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = { showDriveBackupsPicker = false },
                    shape = RoundedCornerShape(Dimens.RadiusMd),
                    border = null,
                    elevation = null,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = OnSurfaceVariantLight
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = Dimens.MinTouchTarget)
                        .depthSecondaryButton(cornerRadius = Dimens.RadiusMd)
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        }
    }

    // 4. Restore Confirmation Dialog with Preview
    if (showRestoreConfirmDialog) {
        val summary = pendingBackupSummary
        LojiaDialog(
            onDismissRequest = {
                showRestoreConfirmDialog = false
                pendingRestoreUri = null
                pendingRestoreFile = null
                pendingRestoreRequiresPassword = false
            },
            size = DialogSize.Small
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMd)
            ) {
                Box(
                    modifier = Modifier
                        .size(Dimens.PinDialogIconSize)
                        .clip(CircleShape)
                        .background(PrimaryContainerLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Restore, contentDescription = null, tint = PrimaryIndigoLight, modifier = Modifier.size(Dimens.IconLg))
                }

                Text(
                    text = stringResource(R.string.restore_confirm_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.headlineSmall,
                    color = OnBackgroundLight,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = stringResource(R.string.restore_confirm_message),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextSecondaryLight,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                if (pendingRestoreRequiresPassword) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .depthSemanticCard(
                                backgroundColor = PrimaryContainerLight,
                                borderColor = OutlineLight,
                                shape = RoundedCornerShape(Dimens.RadiusSm)
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(Dimens.CardPadding),
                            horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = PrimaryIndigoLight,
                                modifier = Modifier.size(Dimens.IconMd)
                            )
                            Text(
                                text = stringResource(R.string.backup_pw_encrypted_badge),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryIndigoDark
                            )
                        }
                    }
                } else if (summary != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .depthSemanticCard(
                                backgroundColor = PrimaryContainerLight,
                                borderColor = OutlineLight,
                                shape = RoundedCornerShape(Dimens.RadiusSm)
                            )
                    ) {
                        Column(modifier = Modifier.padding(Dimens.CardPadding), verticalArrangement = Arrangement.spacedBy(Dimens.SpacingXs)) {
                            Text(
                                text = stringResource(R.string.backup_restore_shifts_count_format, summary.shiftReportsCount),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryIndigoDark
                            )
                            Text(
                                text = stringResource(R.string.backup_restore_summary_format, summary.totalRecords, summary.exportDate),
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceLight
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            val uri = pendingRestoreUri
                            val file = pendingRestoreFile
                            val requiresPassword = pendingRestoreRequiresPassword
                            showRestoreConfirmDialog = false
                            pendingRestoreUri = null
                            pendingRestoreFile = null
                            pendingRestoreRequiresPassword = false
                            pendingAdminAuthTitle = context.getString(R.string.restore_confirm_title)
                            pendingAdminAuthAction = {
                                if (requiresPassword) {
                                    restorePasswordError = null
                                    restorePwTarget = when {
                                        uri != null -> PendingPasswordRestoreTarget.PickedUri(uri, uri.lastPathSegment)
                                        file != null -> PendingPasswordRestoreTarget.LocalFile(file)
                                        else -> null
                                    }
                                } else if (uri != null) {
                                    reportViewModel.restoreOfflineBackup(
                                        uri = uri,
                                        password = null,
                                        onSuccess = { count ->
                                            Toast.makeText(context, context.getString(R.string.data_restored_success, count), Toast.LENGTH_SHORT).show()
                                            refreshStatus()
                                        },
                                        onFailure = { err ->
                                            Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                } else if (file != null) {
                                    coroutineScope.launch {
                                        val res = OfflineBackupManager.restoreBackupFromFile(
                                            context = context,
                                            database = AppDatabase.getInstance(context),
                                            backupFile = file,
                                            password = null
                                        )
                                        if (res.isSuccess) {
                                            val count = res.getOrDefault(0)
                                            Toast.makeText(context, context.getString(R.string.data_restored_success, count), Toast.LENGTH_SHORT).show()
                                            refreshStatus()
                                        } else {
                                            val msg = OfflineBackupManager.resolveRestoreErrorMessage(context, res.exceptionOrNull())
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        },
                        elevation = null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = PureWhite
                        ),
                        shape = RoundedCornerShape(Dimens.RadiusMd),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = Dimens.MinTouchTarget)
                            .depthRaisedButton(enabled = true, cornerRadius = Dimens.RadiusMd)
                    ) {
                        Text(
                            text = stringResource(R.string.btn_restore_now),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            showRestoreConfirmDialog = false
                            pendingRestoreUri = null
                            pendingRestoreFile = null
                            pendingRestoreRequiresPassword = false
                        },
                        shape = RoundedCornerShape(Dimens.RadiusMd),
                        border = null,
                        elevation = null,
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = OnSurfaceVariantLight
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = Dimens.MinTouchTarget)
                            .depthSecondaryButton(cornerRadius = Dimens.RadiusMd)
                    ) {
                        Text(
                            text = stringResource(R.string.action_cancel),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }

    // 5. Danger Zone Typed "DELETE" Modal
    if (showDangerZoneDialog) {
        var typedConfirmation by remember { mutableStateOf("") }
        val isConfirmed = typedConfirmation.trim().equals("DELETE", ignoreCase = false)

        LojiaDialog(
            onDismissRequest = { showDangerZoneDialog = false },
            size = DialogSize.Small
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMd)
            ) {
                Box(
                    modifier = Modifier
                        .size(Dimens.PinDialogIconSize)
                        .clip(CircleShape)
                        .background(ErrorContainerLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = ErrorRedLight, modifier = Modifier.size(Dimens.IconLg))
                }

                Text(
                    text = stringResource(R.string.backup_delete_dialog_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.headlineSmall,
                    color = ErrorRedLight,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = stringResource(R.string.backup_delete_dialog_warning),
                    style = MaterialTheme.typography.titleMedium,
                    color = OnSurfaceLight
                )

                Text(
                    text = stringResource(R.string.backup_delete_dialog_instruction),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = ErrorRedLight
                )

                LojiaTextField(
                    value = typedConfirmation,
                    onValueChange = { typedConfirmation = it },
                    placeholder = { Text(stringResource(R.string.backup_delete_keyword)) },
                    singleLine = true,
                    isError = typedConfirmation.isNotEmpty() && !isConfirmed,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_delete_confirmation")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            showDangerZoneDialog = false
                            pendingAdminAuthTitle = context.getString(R.string.clear_all_data_title)
                            pendingAdminAuthAction = {
                                reportViewModel.clearAllBusinessData(
                                    onSuccess = {
                                        Toast.makeText(context, context.getString(R.string.backup_cleared_all_toast), Toast.LENGTH_LONG).show()
                                        refreshStatus()
                                    },
                                    onFailure = { err ->
                                        Toast.makeText(context, context.getString(R.string.backup_clear_error_format, err), Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        },
                        enabled = isConfirmed,
                        elevation = null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            contentColor = PureWhite,
                            disabledContentColor = OnSurfaceVariantLight
                        ),
                        shape = RoundedCornerShape(Dimens.RadiusMd),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = Dimens.MinTouchTarget)
                            .depthRaisedButton(
                                enabled = isConfirmed,
                                isDestructive = true,
                                cornerRadius = Dimens.RadiusMd
                            )
                            .testTag("btn_confirm_delete_everything")
                    ) {
                        Text(
                            text = stringResource(R.string.btn_delete_everything),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    OutlinedButton(
                        onClick = { showDangerZoneDialog = false },
                        shape = RoundedCornerShape(Dimens.RadiusMd),
                        border = null,
                        elevation = null,
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = OnSurfaceVariantLight
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = Dimens.MinTouchTarget)
                            .depthSecondaryButton(cornerRadius = Dimens.RadiusMd)
                    ) {
                        Text(
                            text = stringResource(R.string.action_cancel),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }

    if (showSetPasswordFirstDialog) {
        BackupPasswordRequiredDialog(
            onDismiss = { showSetPasswordFirstDialog = false },
            onSetPasswordClick = {
                showSetPasswordFirstDialog = false
                openAdminForBackupPassword()
            }
        )
    }

    if (showBackupPasswordDialog) {
        BackupPasswordSetupDialog(
            isChange = hasBackupPassword,
            onDismiss = { showBackupPasswordDialog = false },
            onSaved = {
                showBackupPasswordDialog = false
                hasBackupPassword = true
                refreshStatus()
            }
        )
    }

    restorePwTarget?.let { activeTarget ->
        BackupRestorePasswordDialog(
            fileName = activeTarget.label,
            isRestoring = isRestoreUnlocking,
            errorMessage = restorePasswordError,
            onDismiss = {
                restorePwTarget = null
                restorePasswordError = null
            },
            onSubmitPassword = { enteredPassword ->
                restorePasswordError = null
                when (activeTarget) {
                    is PendingPasswordRestoreTarget.PickedUri -> {
                        isRestoreUnlocking = true
                        reportViewModel.restoreOfflineBackup(
                            uri = activeTarget.uri,
                            password = enteredPassword,
                            onSuccess = { count ->
                                isRestoreUnlocking = false
                                restorePwTarget = null
                                restorePasswordError = null
                                Toast.makeText(context, context.getString(R.string.data_restored_success, count), Toast.LENGTH_LONG).show()
                                refreshStatus()
                            },
                            onFailure = { err ->
                                isRestoreUnlocking = false
                                restorePasswordError = err
                                Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                    is PendingPasswordRestoreTarget.LocalFile -> {
                        isRestoreUnlocking = true
                        coroutineScope.launch {
                            val res = OfflineBackupManager.restoreBackupFromFile(
                                context = context,
                                database = AppDatabase.getInstance(context),
                                backupFile = activeTarget.file,
                                password = enteredPassword
                            )
                            isRestoreUnlocking = false
                            res.onSuccess { count ->
                                restorePwTarget = null
                                restorePasswordError = null
                                Toast.makeText(context, context.getString(R.string.data_restored_success, count), Toast.LENGTH_LONG).show()
                                refreshStatus()
                            }.onFailure { err ->
                                val msg = OfflineBackupManager.resolveRestoreErrorMessage(context, err)
                                restorePasswordError = msg
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    is PendingPasswordRestoreTarget.DriveBytes -> {
                        isRestoreUnlocking = true
                        reportViewModel.restoreGoogleDriveBytes(
                            item = activeTarget.item,
                            bytes = activeTarget.bytes,
                            password = enteredPassword,
                            onSuccess = { count ->
                                isRestoreUnlocking = false
                                restorePwTarget = null
                                restorePasswordError = null
                                Toast.makeText(context, context.getString(R.string.data_restored_success, count), Toast.LENGTH_LONG).show()
                                refreshStatus()
                            },
                            onFailure = { err ->
                                isRestoreUnlocking = false
                                restorePasswordError = err
                                Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        )
    }

    if (pendingAdminAuthAction != null) {
        AdminAuthDialog(
            title = stringResource(R.string.admin_authorization_required),
            subtitle = pendingAdminAuthTitle ?: stringResource(R.string.admin_reauth_default_title),
            onDismissRequest = {
                pendingAdminAuthAction = null
                pendingAdminAuthTitle = null
            },
            onAuthSuccess = {
                val action = pendingAdminAuthAction
                pendingAdminAuthAction = null
                pendingAdminAuthTitle = null
                action?.invoke()
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.MinTouchTarget)
            .depthCard(
                accent = isSelected,
                shape = RoundedCornerShape(Dimens.RadiusMd)
            )
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.CardPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingMd)
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = PrimaryIndigoLight)
            )

            Box(
                modifier = Modifier
                    .size(Dimens.DialogHeaderBadgeSize)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(Dimens.IconAction))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceLight
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
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
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .cardRise(1)
            .depthCard()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.CardPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMd)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)
                ) {
                    Box(
                        modifier = Modifier
                            .size(Dimens.DialogHeaderBadgeSize)
                            .clip(CircleShape)
                            .background(PrimaryIndigoLight.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Folder, contentDescription = null, tint = PrimaryIndigoLight, modifier = Modifier.size(Dimens.IconAction))
                    }
                    Column {
                        Text(stringResource(R.string.backup_device_storage_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = OnSurfaceLight)
                        Text(stringResource(R.string.backup_local_folder_path), style = MaterialTheme.typography.bodySmall, color = TextSecondaryLight)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(Dimens.RadiusLg),
                    color = if (lastBackupTime > 0L) SuccessContainer else WarningContainer
                ) {
                    Text(
                        text = stringResource(if (lastBackupTime > 0L) R.string.backup_status_protected else R.string.backup_status_no_backup),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (lastBackupTime > 0L) SuccessTextDark else WarningText,
                        modifier = Modifier.padding(horizontal = Dimens.SpacingSm, vertical = Dimens.SpacingXs)
                    )
                }
            }

            HorizontalDivider(color = OutlineLight.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(stringResource(R.string.last_backup), style = MaterialTheme.typography.bodySmall, color = TextSecondaryLight)
                    Text(
                        text = if (lastBackupTime > 0L) formatTimeAgo(context, lastBackupTime) else stringResource(R.string.lock_never),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = OnSurfaceLight
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(stringResource(R.string.backup_total_archives_size), style = MaterialTheme.typography.bodySmall, color = TextSecondaryLight)
                    Text(
                        text = formatFileSize(context, totalSize),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = OnSurfaceLight
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)
            ) {
                Button(
                    onClick = onBackupNow,
                    enabled = !isBackingUp,
                    shape = RoundedCornerShape(Dimens.RadiusMd),
                    elevation = null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        contentColor = PureWhite,
                        disabledContentColor = OnSurfaceVariantLight
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Dimens.MinTouchTarget)
                        .depthRaisedButton(enabled = !isBackingUp, cornerRadius = Dimens.RadiusMd)
                ) {
                    if (isBackingUp) {
                        CircularProgressIndicator(modifier = Modifier.size(Dimens.ProgressIndicatorSmSize), color = OnSurfaceVariantLight, strokeWidth = Dimens.ProgressStrokeWidth)
                        Spacer(modifier = Modifier.width(Dimens.SpacingXs))
                        Text(stringResource(R.string.backup_backing_up), style = MaterialTheme.typography.titleSmall)
                    } else {
                        Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(Dimens.IconSm))
                        Spacer(modifier = Modifier.width(Dimens.SpacingXs))
                        Text(stringResource(R.string.backup_now_label), style = MaterialTheme.typography.titleSmall)
                    }
                }

                OutlinedButton(
                    onClick = onViewFiles,
                    shape = RoundedCornerShape(Dimens.RadiusMd),
                    border = null,
                    elevation = null,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = PrimaryIndigoDark
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Dimens.MinTouchTarget)
                        .depthSecondaryButton(
                            borderColor = PrimaryIndigoLight,
                            cornerRadius = Dimens.RadiusMd
                        )
                ) {
                    Icon(Icons.Outlined.FolderOpen, contentDescription = null, modifier = Modifier.size(Dimens.IconSm))
                    Spacer(modifier = Modifier.width(Dimens.SpacingXs))
                    Text(stringResource(R.string.backup_view_files), style = MaterialTheme.typography.titleSmall)
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
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .cardRise(1)
            .depthCard()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.CardPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMd)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)
                ) {
                    Box(
                        modifier = Modifier
                            .size(Dimens.DialogHeaderBadgeSize)
                            .clip(CircleShape)
                            .background(GoogleDriveBlue.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.CloudQueue, contentDescription = null, tint = GoogleDriveBlue, modifier = Modifier.size(Dimens.IconAction))
                    }
                    Column {
                        Text(stringResource(R.string.backup_loc_gdrive_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = OnSurfaceLight)
                        Text(
                            text = if (isConnected) (accountEmail?.takeIf { it.isNotBlank() } ?: stringResource(R.string.backup_status_connected)) else stringResource(R.string.backup_status_not_connected),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryLight
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(Dimens.RadiusLg),
                    color = if (isConnected) SuccessContainer else BackgroundLight,
                    border = BorderStroke(Dimens.BorderThin, if (isConnected) SuccessGreen.copy(alpha = 0.4f) else OutlineLight)
                ) {
                    Text(
                        text = stringResource(if (isConnected) R.string.backup_status_connected else R.string.backup_status_disconnected),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isConnected) SuccessTextDark else TextSecondaryLight,
                        modifier = Modifier.padding(horizontal = Dimens.SpacingSm, vertical = Dimens.SpacingXs)
                    )
                }
            }

            HorizontalDivider(color = OutlineLight.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(stringResource(R.string.backup_last_cloud_sync), style = MaterialTheme.typography.bodySmall, color = TextSecondaryLight)
                    Text(
                        text = if (lastSyncTime > 0L) formatTimeAgo(context, lastSyncTime) else stringResource(R.string.lock_never),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = OnSurfaceLight
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(stringResource(R.string.backup_target_folder), style = MaterialTheme.typography.bodySmall, color = TextSecondaryLight)
                    Text(stringResource(R.string.backup_appdata_space), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = OnSurfaceLight)
                }
            }

            if (!isConnected) {
                Button(
                    onClick = onConnect,
                    shape = RoundedCornerShape(Dimens.RadiusMd),
                    elevation = null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = PureWhite
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = Dimens.MinTouchTarget)
                        .depthRaisedButton(enabled = true, cornerRadius = Dimens.RadiusMd)
                ) {
                    Icon(Icons.Outlined.CloudUpload, contentDescription = null, modifier = Modifier.size(Dimens.IconSm))
                    Spacer(modifier = Modifier.width(Dimens.SpacingXs))
                    Text(stringResource(R.string.backup_connect_gdrive), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onBackupNow,
                        enabled = !isUploading,
                        shape = RoundedCornerShape(Dimens.RadiusMd),
                        elevation = null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            contentColor = PureWhite,
                            disabledContentColor = OnSurfaceVariantLight
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = Dimens.MinTouchTarget)
                            .depthRaisedButton(enabled = !isUploading, cornerRadius = Dimens.RadiusMd)
                    ) {
                        if (isUploading) {
                            CircularProgressIndicator(modifier = Modifier.size(Dimens.ProgressIndicatorSmSize), color = OnSurfaceVariantLight, strokeWidth = Dimens.ProgressStrokeWidth)
                            Spacer(modifier = Modifier.width(Dimens.SpacingXs))
                            Text(stringResource(R.string.backup_uploading), style = MaterialTheme.typography.titleSmall)
                        } else {
                            Icon(Icons.Outlined.CloudUpload, contentDescription = null, modifier = Modifier.size(Dimens.IconSm))
                            Spacer(modifier = Modifier.width(Dimens.SpacingXs))
                            Text(stringResource(R.string.backup_now_label), style = MaterialTheme.typography.titleSmall)
                        }
                    }

                    OutlinedButton(
                        onClick = onRestore,
                        shape = RoundedCornerShape(Dimens.RadiusMd),
                        border = null,
                        elevation = null,
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = GoogleDriveBlue
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = Dimens.MinTouchTarget)
                            .depthSecondaryButton(
                                borderColor = GoogleDriveBlue,
                                cornerRadius = Dimens.RadiusMd
                            )
                    ) {
                        Icon(Icons.Outlined.CloudDownload, contentDescription = null, modifier = Modifier.size(Dimens.IconSm))
                        Spacer(modifier = Modifier.width(Dimens.SpacingXs))
                        Text(stringResource(R.string.action_unarchive), style = MaterialTheme.typography.titleSmall)
                    }

                    IconButton(
                        onClick = onDisconnect,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.btn_disconnect_drive), tint = ErrorRedLight)
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
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .cardRise(1)
            .depthCard()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.CardPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMd)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)
                ) {
                    Box(
                        modifier = Modifier
                            .size(Dimens.DialogHeaderBadgeSize)
                            .clip(CircleShape)
                            .background(OneDriveBlue.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.CloudSync, contentDescription = null, tint = OneDriveBlue, modifier = Modifier.size(Dimens.IconAction))
                    }
                    Column {
                        Text(stringResource(R.string.backup_loc_onedrive_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = OnSurfaceLight)
                        Text(
                            text = if (isConnected) (accountEmail?.takeIf { it.isNotBlank() } ?: stringResource(R.string.backup_status_connected)) else stringResource(R.string.backup_status_not_connected),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryLight
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(Dimens.RadiusLg),
                    color = if (isConnected) SuccessContainer else BackgroundLight,
                    border = BorderStroke(Dimens.BorderThin, if (isConnected) SuccessGreen.copy(alpha = 0.4f) else OutlineLight)
                ) {
                    Text(
                        text = stringResource(if (isConnected) R.string.backup_status_connected else R.string.backup_status_disconnected),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isConnected) SuccessTextDark else TextSecondaryLight,
                        modifier = Modifier.padding(horizontal = Dimens.SpacingSm, vertical = Dimens.SpacingXs)
                    )
                }
            }

            HorizontalDivider(color = OutlineLight.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(stringResource(R.string.backup_last_cloud_sync), style = MaterialTheme.typography.bodySmall, color = TextSecondaryLight)
                    Text(
                        text = if (lastSyncTime > 0L) formatTimeAgo(context, lastSyncTime) else stringResource(R.string.lock_never),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = OnSurfaceLight
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(stringResource(R.string.backup_target_folder), style = MaterialTheme.typography.bodySmall, color = TextSecondaryLight)
                    Text(stringResource(R.string.backup_onedrive_folder_path), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = OnSurfaceLight)
                }
            }

            if (!isConnected) {
                Button(
                    onClick = onConnect,
                    shape = RoundedCornerShape(Dimens.RadiusMd),
                    elevation = null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = PureWhite
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = Dimens.MinTouchTarget)
                        .depthRaisedButton(enabled = true, cornerRadius = Dimens.RadiusMd)
                ) {
                    Icon(Icons.Outlined.CloudUpload, contentDescription = null, modifier = Modifier.size(Dimens.IconSm))
                    Spacer(modifier = Modifier.width(Dimens.SpacingXs))
                    Text(stringResource(R.string.backup_connect_onedrive), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onBackupNow,
                        enabled = !isUploading,
                        shape = RoundedCornerShape(Dimens.RadiusMd),
                        elevation = null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            contentColor = PureWhite,
                            disabledContentColor = OnSurfaceVariantLight
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = Dimens.MinTouchTarget)
                            .depthRaisedButton(enabled = !isUploading, cornerRadius = Dimens.RadiusMd)
                    ) {
                        if (isUploading) {
                            CircularProgressIndicator(modifier = Modifier.size(Dimens.ProgressIndicatorSmSize), color = OnSurfaceVariantLight, strokeWidth = Dimens.ProgressStrokeWidth)
                            Spacer(modifier = Modifier.width(Dimens.SpacingXs))
                            Text(stringResource(R.string.backup_uploading), style = MaterialTheme.typography.titleSmall)
                        } else {
                            Icon(Icons.Outlined.CloudUpload, contentDescription = null, modifier = Modifier.size(Dimens.IconSm))
                            Spacer(modifier = Modifier.width(Dimens.SpacingXs))
                            Text(stringResource(R.string.backup_now_label), style = MaterialTheme.typography.titleSmall)
                        }
                    }

                    OutlinedButton(
                        onClick = onRestore,
                        shape = RoundedCornerShape(Dimens.RadiusMd),
                        border = null,
                        elevation = null,
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = OneDriveBlue
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = Dimens.MinTouchTarget)
                            .depthSecondaryButton(
                                borderColor = OneDriveBlue,
                                cornerRadius = Dimens.RadiusMd
                            )
                    ) {
                        Icon(Icons.Outlined.CloudDownload, contentDescription = null, modifier = Modifier.size(Dimens.IconSm))
                        Spacer(modifier = Modifier.width(Dimens.SpacingXs))
                        Text(stringResource(R.string.action_unarchive), style = MaterialTheme.typography.titleSmall)
                    }

                    IconButton(
                        onClick = onDisconnect,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.btn_disconnect_drive), tint = ErrorRedLight)
                    }
                }
            }
        }
    }
}

private fun formatFileSize(context: Context, bytes: Long): String {
    if (bytes <= 0) return context.getString(R.string.backup_size_zero_bytes)
    val kb = bytes / 1024.0
    val appLocale = DateTimeFormatUtils.getAppLocale()
    if (kb < 1024) return String.format(appLocale, context.getString(R.string.backup_size_kb_format), kb)
    val mb = kb / 1024.0
    return String.format(appLocale, context.getString(R.string.backup_size_mb_format), mb)
}

private fun formatTimeAgo(context: Context, timeMs: Long): String {
    if (timeMs <= 0L) return context.getString(R.string.lock_never)
    val now = System.currentTimeMillis()
    val diff = now - timeMs
    if (diff < 60_000L) return context.getString(R.string.backup_time_just_now)
    val mins = diff / 60_000L
    if (mins < 60) return context.getString(R.string.backup_time_min_ago_format, mins)
    val hours = mins / 60
    if (hours < 24) return context.getString(R.string.backup_time_hr_ago_format, hours)
    val days = hours / 24
    if (days < 7) return context.getString(R.string.backup_time_days_ago_format, days)
    return DateTimeFormatUtils.formatDate(timeMs)
}
