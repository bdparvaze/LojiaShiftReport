package com.lojia.shiftreport.settings

import android.content.Context
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
            .padding(horizontal = LojiaDimens.ScreenPadding, vertical = LojiaDimens.ScreenTopPadding)
    ) {
        // Top Back Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Dimens.SpacingSm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("btn_backup_back")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = TextPrimaryReport
                )
            }
            Spacer(modifier = Modifier.width(Dimens.SpacingXs))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.backup_settings),
                    fontSize = DepthTypeTokens.SectionTitleSize,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryReport
                )
                Text(
                    text = stringResource(R.string.backup_subtitle),
                    fontSize = DepthTypeTokens.ListRowSubtitleSize,
                    color = OnSurfaceVariantLight
                )
            }
        }

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
                .padding(bottom = 16.dp)
                .cardRise(0)
                .depthCard()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.backup_select_location_title),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceLight
                )
                Text(
                    text = stringResource(R.string.backup_select_location_desc),
                    fontSize = 12.sp,
                    color = TextSecondaryLight,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

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
            Spacer(modifier = Modifier.height(12.dp))
        }

        // ==========================================
        // 3. BOTTOM: AUTO BACKUP SECTION
        // ==========================================
        LojiaSectionHeader(text = stringResource(R.string.backup_section_automatic))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .cardRise(2)
                .depthCard()
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
                            text = stringResource(R.string.backup_auto_desc),
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
                        text = stringResource(R.string.backup_frequency_title),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OnSurfaceLight
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                                label = { Text(freqLabel, fontSize = 12.sp) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
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
                .padding(bottom = 32.dp)
                .cardRise(3)
                .depthSemanticCard(
                    backgroundColor = NegativeBannerSurface,
                    borderColor = NegativeBannerBorder
                )
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

        LojiaDialog(
            onDismissRequest = { showLocalFilesDialog = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.DialogPaddingComfortable),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMd)
            ) {
                Text(stringResource(R.string.backup_local_archives_title), fontWeight = FontWeight.Bold, fontSize = 17.sp, color = OnBackgroundLight)

                if (backupFiles.isEmpty()) {
                    Text(stringResource(R.string.backup_local_none_found), fontSize = 13.sp, color = TextSecondaryLight)
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 350.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                            text = "${DateTimeFormatUtils.formatDateTime(file.lastModified())} • ${formatFileSize(context, file.length())}",
                                            fontSize = 10.sp,
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
            onDismissRequest = { showOneDriveConnectDialog = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(Dimens.DialogPaddingComfortable),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMd)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.CloudSync, contentDescription = null, tint = OneDriveBlue)
                    Text(stringResource(R.string.backup_connect_onedrive), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = OnBackgroundLight)
                }

                Text(
                    text = stringResource(R.string.backup_onedrive_connect_desc),
                    fontSize = 12.sp,
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
            onDismissRequest = { showDriveBackupsPicker = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.DialogPaddingComfortable),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMd)
            ) {
                Text(stringResource(R.string.restore_from_drive_title), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = OnBackgroundLight)

                if (driveBackupsList.isEmpty()) {
                    Text(stringResource(R.string.backup_gdrive_none_found), fontSize = 13.sp, color = TextSecondaryLight)
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
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
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(Dimens.DialogPaddingComfortable),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMd)
            ) {
                Box(
                    modifier = Modifier
                        .size(Dimens.MinTouchTarget)
                        .clip(CircleShape)
                        .background(PrimaryContainerLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Restore, contentDescription = null, tint = PrimaryIndigoLight, modifier = Modifier.size(28.dp))
                }

                Text(
                    text = stringResource(R.string.restore_confirm_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = OnBackgroundLight,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = stringResource(R.string.restore_confirm_message),
                    fontSize = 13.sp,
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
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = PrimaryIndigoLight,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = stringResource(R.string.backup_pw_encrypted_badge),
                                fontSize = 13.sp,
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
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = stringResource(R.string.backup_restore_shifts_count_format, summary.shiftReportsCount),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryIndigoDark
                            )
                            Text(
                                text = stringResource(R.string.backup_restore_summary_format, summary.totalRecords, summary.exportDate),
                                fontSize = 11.sp,
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
            onDismissRequest = { showDangerZoneDialog = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(Dimens.DialogPaddingComfortable),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMd)
            ) {
                Box(
                    modifier = Modifier
                        .size(Dimens.MinTouchTarget)
                        .clip(CircleShape)
                        .background(ErrorContainerLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = ErrorRedLight, modifier = Modifier.size(28.dp))
                }

                Text(
                    text = stringResource(R.string.backup_delete_dialog_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = ErrorRedLight,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = stringResource(R.string.backup_delete_dialog_warning),
                    fontSize = 13.sp,
                    color = OnSurfaceLight
                )

                Text(
                    text = stringResource(R.string.backup_delete_dialog_instruction),
                    fontSize = 12.sp,
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
                        Text(stringResource(R.string.backup_device_storage_title), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = OnSurfaceLight)
                        Text(stringResource(R.string.backup_local_folder_path), fontSize = 11.sp, color = TextSecondaryLight)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (lastBackupTime > 0L) SuccessContainer else WarningContainer
                ) {
                    Text(
                        text = stringResource(if (lastBackupTime > 0L) R.string.backup_status_protected else R.string.backup_status_no_backup),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (lastBackupTime > 0L) SuccessTextDark else WarningText,
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
                    Text(stringResource(R.string.last_backup), fontSize = 11.sp, color = TextSecondaryLight)
                    Text(
                        text = if (lastBackupTime > 0L) formatTimeAgo(context, lastBackupTime) else stringResource(R.string.lock_never),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OnSurfaceLight
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(stringResource(R.string.backup_total_archives_size), fontSize = 11.sp, color = TextSecondaryLight)
                    Text(
                        text = formatFileSize(context, totalSize),
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
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = OnSurfaceVariantLight, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.backup_backing_up), fontSize = 12.sp)
                    } else {
                        Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.backup_now_label), fontSize = 12.sp)
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
                    Icon(Icons.Outlined.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.backup_view_files), fontSize = 12.sp)
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
                            .background(GoogleDriveBlue.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.CloudQueue, contentDescription = null, tint = GoogleDriveBlue, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text(stringResource(R.string.backup_loc_gdrive_title), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = OnSurfaceLight)
                        Text(
                            text = if (isConnected) (accountEmail?.takeIf { it.isNotBlank() } ?: stringResource(R.string.backup_status_connected)) else stringResource(R.string.backup_status_not_connected),
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
                        text = stringResource(if (isConnected) R.string.backup_status_connected else R.string.backup_status_disconnected),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isConnected) SuccessTextDark else TextSecondaryLight,
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
                    Text(stringResource(R.string.backup_last_cloud_sync), fontSize = 11.sp, color = TextSecondaryLight)
                    Text(
                        text = if (lastSyncTime > 0L) formatTimeAgo(context, lastSyncTime) else stringResource(R.string.lock_never),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OnSurfaceLight
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(stringResource(R.string.backup_target_folder), fontSize = 11.sp, color = TextSecondaryLight)
                    Text(stringResource(R.string.backup_appdata_space), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceLight)
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
                    Icon(Icons.Outlined.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.backup_connect_gdrive), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = OnSurfaceVariantLight, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.backup_uploading), fontSize = 12.sp)
                        } else {
                            Icon(Icons.Outlined.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.backup_now_label), fontSize = 12.sp)
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
                        Icon(Icons.Outlined.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.action_unarchive), fontSize = 12.sp)
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
                            .background(OneDriveBlue.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.CloudSync, contentDescription = null, tint = OneDriveBlue, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text(stringResource(R.string.backup_loc_onedrive_title), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = OnSurfaceLight)
                        Text(
                            text = if (isConnected) (accountEmail?.takeIf { it.isNotBlank() } ?: stringResource(R.string.backup_status_connected)) else stringResource(R.string.backup_status_not_connected),
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
                        text = stringResource(if (isConnected) R.string.backup_status_connected else R.string.backup_status_disconnected),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isConnected) SuccessTextDark else TextSecondaryLight,
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
                    Text(stringResource(R.string.backup_last_cloud_sync), fontSize = 11.sp, color = TextSecondaryLight)
                    Text(
                        text = if (lastSyncTime > 0L) formatTimeAgo(context, lastSyncTime) else stringResource(R.string.lock_never),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OnSurfaceLight
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(stringResource(R.string.backup_target_folder), fontSize = 11.sp, color = TextSecondaryLight)
                    Text(stringResource(R.string.backup_onedrive_folder_path), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceLight)
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
                    Icon(Icons.Outlined.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.backup_connect_onedrive), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = OnSurfaceVariantLight, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.backup_uploading), fontSize = 12.sp)
                        } else {
                            Icon(Icons.Outlined.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.backup_now_label), fontSize = 12.sp)
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
                        Icon(Icons.Outlined.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.action_unarchive), fontSize = 12.sp)
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
