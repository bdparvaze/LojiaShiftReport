package com.lojia.shiftreport.settings

import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.lojia.shiftreport.ui.common.LojiaTextField
import com.lojia.shiftreport.R
import com.lojia.shiftreport.data.*
import com.lojia.shiftreport.util.*
import com.lojia.shiftreport.ui.common.*
import com.lojia.shiftreport.ui.theme.*
import com.lojia.shiftreport.auth.*
import com.lojia.shiftreport.report.*
import com.lojia.shiftreport.settings.*
import com.lojia.shiftreport.sync.*

import com.lojia.shiftreport.util.AppLanguageManager
import com.lojia.shiftreport.util.SecurityUtils
import androidx.compose.ui.text.style.TextOverflow


import android.content.Intent

import android.net.Uri

import android.widget.Toast


import androidx.compose.animation.*


import androidx.compose.animation.core.*


import androidx.compose.foundation.BorderStroke


import androidx.compose.foundation.background


import androidx.compose.foundation.clickable


import androidx.compose.foundation.layout.*


import androidx.compose.foundation.lazy.LazyColumn


import androidx.compose.foundation.lazy.items


import androidx.compose.foundation.shape.RoundedCornerShape


import androidx.compose.foundation.shape.CircleShape


import androidx.compose.material.icons.Icons
import androidx.fragment.app.FragmentActivity


import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight


import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.platform.testTag


import androidx.compose.material3.*


import androidx.compose.runtime.*


import androidx.compose.ui.Alignment


import androidx.compose.ui.Modifier


import androidx.compose.ui.graphics.Color


import androidx.compose.ui.platform.LocalContext


import androidx.compose.foundation.text.KeyboardOptions


import androidx.compose.ui.text.font.FontWeight


import androidx.compose.ui.text.input.KeyboardType


import androidx.compose.ui.text.input.PasswordVisualTransformation


import androidx.compose.ui.unit.dp


import androidx.compose.ui.unit.sp


import java.text.SimpleDateFormat

import java.util.*


import androidx.compose.ui.res.stringResource


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsReportSection(
    reportViewModel: ReportViewModel,
    language: AppLanguage,
    isAdmin: Boolean,
    onRestrictedClick: (action: () -> Unit) -> Unit,
    onSwitchModule: (AppModule) -> Unit,
    onConfigurePrinterClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val userProfile by reportViewModel.userProfile.collectAsState()
    val businessProfile by reportViewModel.businessProfile.collectAsState()
    val isBackingUp by reportViewModel.isBackingUp.collectAsState()
    val backupProgress by reportViewModel.backupProgress.collectAsState()
    val lastBackupTime: Long by reportViewModel.lastBackupTime.collectAsState()
    val googleAccount: String by reportViewModel.googleAccount.collectAsState()
    val autoBackupFreq: String by reportViewModel.autoBackupFrequency.collectAsState()
    val backupCellular: Boolean by reportViewModel.backupUsingCellular.collectAsState()

    val selectedReportMenu by reportViewModel.selectedReportSettingsMenu.collectAsState()
    val currentMenu = selectedReportMenu ?: "backup"
    val cashiers by reportViewModel.cashiers.collectAsState()
    val currentCountry by reportViewModel.currentCountry.collectAsState()
    var showAddCashierDialog by remember { mutableStateOf(false) }
    var cashierToDelete by remember { mutableStateOf<Cashier?>(null) }
    var langCountryTab by remember(currentMenu) { mutableIntStateOf(if (currentMenu == "country") 1 else 0) }
    var countrySearch by remember { mutableStateOf("") }

    // 1. Profile State
    var fullName by remember(userProfile) { mutableStateOf(userProfile?.fullName ?: "Store Owner") }
    var username by remember(userProfile) { mutableStateOf(userProfile?.username ?: "") }
    var email by remember(userProfile) { mutableStateOf(userProfile?.email ?: "") }
    var password by remember(userProfile) { mutableStateOf("") }
    var phone by remember(userProfile) { mutableStateOf(userProfile?.phone ?: "") }
    var address by remember(userProfile) { mutableStateOf(userProfile?.address ?: "") }
    var showMapLocationPicker by remember { mutableStateOf(false) }
    var mapLat by remember(userProfile, businessProfile) { mutableDoubleStateOf(userProfile?.mapLat ?: businessProfile?.mapLat ?: 0.0) }
    var mapLng by remember(userProfile, businessProfile) { mutableDoubleStateOf(userProfile?.mapLng ?: businessProfile?.mapLng ?: 0.0) }

    // 2. Security State
    val preferencesRepository = remember(context) { com.lojia.shiftreport.data.PreferencesRepository.getInstance(context) }
    var biometricEnabled by remember { mutableStateOf(preferencesRepository.isBiometricEnabled()) }
    var quickLoginEnabled by remember { mutableStateOf(preferencesRepository.isQuickLoginEnabled() && preferencesRepository.hasPinConfigured()) }
    var showQuickPinDialog by remember { mutableStateOf(false) }
    var pinValue by remember(userProfile) { mutableStateOf(userProfile?.pin.orEmpty()) }
    var showChangePinModal by remember { mutableStateOf(false) }

    // 3. Backup State
    val dateFormatter = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
    val lastBackupFormatted = remember(lastBackupTime) {
        if (lastBackupTime > 0L) dateFormatter.format(Date(lastBackupTime)) else "Never"
    }

    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var showClearAllDataConfirmDialog by remember { mutableStateOf(false) }
    var showAutoBackupDialog by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var pendingBackupSummary by remember { mutableStateOf<BackupSummary?>(null) }
    var lastCreatedBackupInfo by remember { mutableStateOf<BackupInfo?>(null) }

    val driveAccount by reportViewModel.driveAccountInfo.collectAsState()
    val isDriveUploading by reportViewModel.isDriveUploading.collectAsState()
    val isDriveDownloading by reportViewModel.isDriveDownloading.collectAsState()
    val isDriveLoadingList by reportViewModel.isDriveLoadingList.collectAsState()
    val driveBackupsList by reportViewModel.driveBackupsList.collectAsState()
    val firebaseSyncState by reportViewModel.firebaseSyncState.collectAsState()

    var showDriveBackupsPicker by remember { mutableStateOf(false) }
    var pendingDriveBackupItem by remember { mutableStateOf<DriveBackupItem?>(null) }
    var pendingDriveBackupSummary by remember { mutableStateOf<BackupSummary?>(null) }
    var showDriveRestoreConfirmDialog by remember { mutableStateOf(false) }
    var isFetchingDriveSummary by remember { mutableStateOf(false) }

    val restoreFileLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingRestoreUri = uri
            reportViewModel.parseBackupSummary(uri) { summary ->
                pendingBackupSummary = summary
                showRestoreConfirmDialog = true
            }
        }
    }

    // Active edit dialog
    var editFieldDialog by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showTermsModal by remember { mutableStateOf(false) }
    var showPrivacyModal by remember { mutableStateOf(false) }
    var showSupportTicketDialog by remember { mutableStateOf(false) }
    var langSearch by remember { mutableStateOf("") }

    fun persistProfile(
        fName: String = fullName,
        uName: String = username,
        mail: String = email,
        pass: String = password,
        ph: String = phone,
        addr: String = address,
        bio: Boolean = biometricEnabled,
        p: String = pinValue
    ) {
        val current = userProfile ?: UserProfile()
        reportViewModel.saveUserProfile(
            current.copy(
                fullName = fName,
                username = uName,
                email = mail,
                passwordHash = SecurityUtils.hashSecret(pass),
                phone = ph,
                address = addr,
                isBiometricEnabled = bio,
                pin = SecurityUtils.hashSecret(p)
            )
        )
    }

    val isRootMenu = currentMenu == "root" || currentMenu == "all" || currentMenu == "overview"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceLight)
    ) {

        // Quick Action Banner: Daily Shift Report Generator (Shown only in root settings overview or shift sub-menu)
        if (isRootMenu || currentMenu == "daily_shift_report") {
            Surface(
                color = PrimaryContainerLight,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, PrimaryIndigoLight),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(PrimaryIndigoLight, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Assessment, contentDescription = null, tint = PureWhite, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            AutoText(id = R.string.daily_shift_report, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = OnSurfaceLight)
                            AutoText(id = R.string.audit_drawer_cash_create, fontSize = 11.5.sp, color = OnSurfaceVariantLight)
                        }
                    }
                    Button(
                        onClick = { onSwitchModule(AppModule.SHIFT_REPORT) },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigoLight),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        AutoText(id = R.string.open_2, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            HorizontalDivider(color = OutlineVariantLight)
        }

        when (currentMenu) {
            // =================================================================
            // 0. ROOT / ALL SETTINGS HUB (Overview of all Shift Report Settings)
            // =================================================================
            "root", "all", "overview" -> {
                AutoText(
                    id = R.string.staff_admin_section,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextHintColor,
                    modifier = Modifier.padding(start = 20.dp, top = 6.dp, bottom = 4.dp)
                )

                LoyverseMenuItemRow(
                    icon = Icons.Outlined.Person,
                    title = "Profile",
                    subtitle = stringResource(R.string.profile_subtitle),
                    onClick = { reportViewModel.selectReportSettingsMenu("profile") }
                )

                LoyverseMenuItemRow(
                    icon = Icons.Outlined.People,
                    title = "Cashier",
                    subtitle = stringResource(R.string.cashiers_subtitle),
                    onClick = { reportViewModel.selectReportSettingsMenu("cashiers") }
                )

                AutoText(
                    id = R.string.security_data_section,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextHintColor,
                    modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 4.dp)
                )

                LoyverseMenuItemRow(
                    icon = Icons.Outlined.Shield,
                    title = "Security",
                    subtitle = stringResource(R.string.security_subtitle),
                    onClick = { reportViewModel.selectReportSettingsMenu("security") }
                )

                LoyverseMenuItemRow(
                    icon = Icons.Outlined.CloudUpload,
                    title = "Backup",
                    subtitle = stringResource(R.string.backup_subtitle),
                    onClick = { reportViewModel.selectReportSettingsMenu("backup") }
                )

                LoyverseMenuItemRow(
                    icon = Icons.Outlined.Print,
                    title = "Thermal Printer (ESC/POS)",
                    subtitle = "Configure Bluetooth & Network receipt printers",
                    onClick = onConfigurePrinterClick
                )

                AutoText(
                    id = R.string.regional_interface_section,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextHintColor,
                    modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 4.dp)
                )

                RegionalPreferencesCard(
                    currentCountry = currentCountry,
                    currentLanguage = language,
                    businessProfile = businessProfile,
                    onOpenRegionalMenu = { tabIndex ->
                        langCountryTab = tabIndex
                        reportViewModel.selectReportSettingsMenu("regional")
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )

                AutoText(
                    id = R.string.system_support_section,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextHintColor,
                    modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 4.dp)
                )

                LoyverseMenuItemRow(
                    icon = Icons.Outlined.Info,
                    title = "About",
                    subtitle = stringResource(R.string.about_subtitle),
                    onClick = { reportViewModel.selectReportSettingsMenu("about") }
                )

                LoyverseMenuItemRow(
                    icon = Icons.Outlined.HeadsetMic,
                    title = "Support",
                    subtitle = stringResource(R.string.support_subtitle),
                    onClick = { reportViewModel.selectReportSettingsMenu("support") }
                )
            }

            // =================================================================
            // 0. DAILY SHIFT REPORT GENERATOR
            // =================================================================
            "daily_shift_report", "shift_report", "shift" -> {
                LoyverseMenuItemRow(
                    icon = Icons.Outlined.Assessment,
                    title = stringResource(R.string.daily_shift_report),
                    subtitle = stringResource(R.string.desc_create_shift_handover),
                    trailing = {
                        Button(
                            onClick = { onSwitchModule(AppModule.SHIFT_REPORT) },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigoLight),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(stringResource(R.string.launch_1), fontSize = 12.sp)
                        }
                    },
                    onClick = { onSwitchModule(AppModule.SHIFT_REPORT) }
                )
                LoyverseMenuItemRow(
                    icon = Icons.Outlined.PictureAsPdf,
                    title = stringResource(R.string.title_shift_reports_pdf_archives),
                    subtitle = stringResource(R.string.desc_search_shift_reports),
                    onClick = { onSwitchModule(AppModule.SHIFT_REPORT) }
                )
            }
            // =================================================================
            // EMPLOYEE / CASHIER MANAGEMENT
            // =================================================================
            "cashiers", "cashier", "staff", "employees", "employee" -> {
                CashierManagementSection(
                    cashiers = cashiers,
                    onAddCashierClick = { showAddCashierDialog = true },
                    onDeleteCashierClick = { cashierToDelete = it },
                    onToggleStatusClick = { reportViewModel.updateCashier(it) }
                )
            }

            // =================================================================
            // 1. PROFILE
            // =================================================================
            "profile" -> {
                ProfileScreen(
                    reportViewModel = reportViewModel,
                    onLogoutClick = {
                        reportViewModel.selectReportSettingsMenu("root")
                    }
                )
            }

            // =================================================================
            // 2. SECURITY (Biometric & MPIN)
            // =================================================================
            "security", "mpin", "pin" -> {
                var isMpinExpanded by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    LojiaSectionHeader("PIN & BIOMETRIC")

                    // Card 1 — Quick Login
                    LojiaSettingsCard {
                        Icon(
                            imageVector = Icons.Outlined.Pin,
                            contentDescription = "Quick Login with PIN",
                            tint = OnSurfaceVariantLight,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Quick Login with PIN",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = OnSurfaceLight
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Unlock POS with 4-digit PIN",
                                fontSize = 13.sp,
                                color = OnSurfaceVariantLight
                            )
                        }
                        Switch(
                            checked = quickLoginEnabled,
                            onCheckedChange = { enable ->
                                if (enable) {
                                    if (preferencesRepository.hasPinConfigured() && !preferencesRepository.getStoredPinHash().isNullOrBlank()) {
                                        quickLoginEnabled = true
                                        preferencesRepository.setQuickLoginEnabled(true)
                                        Toast.makeText(context, "Quick Login enabled", Toast.LENGTH_SHORT).show()
                                    } else {
                                        showQuickPinDialog = true
                                    }
                                } else {
                                    quickLoginEnabled = false
                                    preferencesRepository.setQuickLoginEnabled(false)
                                    // Disabling Quick Login also disables Biometric unlock as required
                                    biometricEnabled = false
                                    preferencesRepository.setBiometricEnabled(false)
                                    persistProfile(bio = false)
                                    Toast.makeText(context, "Quick Login disabled", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PureWhite,
                                checkedTrackColor = PrimaryIndigoLight,
                                uncheckedThumbColor = PureWhite,
                                uncheckedTrackColor = OutlineLight
                            )
                        )
                    }

                    // Card 2 — Change PIN row
                    LojiaSettingsCard(onClick = { showQuickPinDialog = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Key,
                            contentDescription = "Change 4-Digit PIN",
                            tint = OnSurfaceVariantLight,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Change 4-Digit PIN",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = OnSurfaceLight
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Tap to update your PIN",
                                fontSize = 13.sp,
                                color = OnSurfaceVariantLight
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = TextHintColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    LojiaSectionHeader("BIOMETRIC")

                    // Card 3 — Biometric
                    LojiaSettingsCard {
                        Icon(
                            imageVector = Icons.Outlined.Fingerprint,
                            contentDescription = stringResource(R.string.cd_biometric_icon),
                            tint = OnSurfaceVariantLight,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Biometric Login",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = OnSurfaceLight
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Fingerprint / Face ID",
                                fontSize = 13.sp,
                                color = OnSurfaceVariantLight
                            )
                        }
                        Switch(
                            checked = biometricEnabled,
                            onCheckedChange = { enable ->
                                if (enable) {
                                    // 3. First check that Quick Login is already enabled and a PIN exists (as fallback)
                                    if (!quickLoginEnabled || !preferencesRepository.hasPinConfigured()) {
                                        Toast.makeText(context, "Please enable Quick Login with a PIN first before activating Biometric.", Toast.LENGTH_LONG).show()
                                        showQuickPinDialog = true
                                        return@Switch
                                    }

                                    val fragActivity = context as? FragmentActivity
                                    val bioStatus = BiometricAuthManager.checkBiometricAvailability(context)
                                    if (fragActivity != null && bioStatus == BiometricStatus.AVAILABLE) {
                                        BiometricAuthManager.showBiometricPrompt(
                                            activity = fragActivity,
                                            title = "Biometric Verification",
                                            subtitle = "Touch sensor to activate Biometric Login",
                                            description = "Verifying biometric security for your account.",
                                            onResult = { result ->
                                                when (result) {
                                                    is BiometricAuthResult.Success -> {
                                                        biometricEnabled = true
                                                        preferencesRepository.setBiometricEnabled(true)
                                                        persistProfile(bio = true)
                                                        Toast.makeText(context, context.getString(R.string.biometric_login_enabled), Toast.LENGTH_SHORT).show()
                                                    }
                                                    is BiometricAuthResult.Failed -> {
                                                        Toast.makeText(context, "Fingerprint not recognized. Try again.", Toast.LENGTH_SHORT).show()
                                                    }
                                                    is BiometricAuthResult.Error -> {
                                                        Toast.makeText(context, result.errString.toString(), Toast.LENGTH_SHORT).show()
                                                    }
                                                    else -> {}
                                                }
                                            }
                                        )
                                    } else {
                                        biometricEnabled = true
                                        preferencesRepository.setBiometricEnabled(true)
                                        persistProfile(bio = true)
                                        Toast.makeText(context, context.getString(R.string.biometric_login_enabled), Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    biometricEnabled = false
                                    preferencesRepository.setBiometricEnabled(false)
                                    persistProfile(bio = false)
                                    Toast.makeText(context, context.getString(R.string.biometric_login_disabled), Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PureWhite,
                                checkedTrackColor = PrimaryIndigoLight,
                                uncheckedThumbColor = PureWhite,
                                uncheckedTrackColor = OutlineLight
                            )
                        )
                    }
                }
            }

            // =================================================================
            // 3. BACKUP & RESTORE
            // =================================================================
            "backup" -> {
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
                    }

                    // Recently Created Backup Banner Card (if active)
                    if (lastCreatedBackupInfo != null) {
                        val info = lastCreatedBackupInfo!!
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
                                        text = stringResource(R.string.backup_created_success, info.fileName),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SuccessGreen
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${info.savedPath} • ${info.totalRecords} records",
                                        fontSize = 12.sp,
                                        color = OnSurfaceLight
                                    )
                                }
                                IconButton(onClick = { lastCreatedBackupInfo = null }) {
                                    Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = SuccessGreen, modifier = Modifier.size(18.dp))
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
            }

            // =================================================================
            // 4. REGIONAL & PREFERENCES SELECTION (LANGUAGE, COUNTRY, CURRENCY)
            // =================================================================
            "country", "countries", "currency", "language", "regional" -> {
                val initialTab = remember(currentMenu) {
                    when (currentMenu) {
                        "country", "countries", "currency" -> 1
                        "language" -> -1
                        else -> langCountryTab
                    }
                }
                RegionalPreferencesDetailView(
                    reportViewModel = reportViewModel,
                    initialTab = initialTab
                )
            }

            // =================================================================
            // 5. ABOUT
            // =================================================================
            "about" -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = LojiaDimens.ScreenPadding, vertical = LojiaDimens.ScreenTopPadding)
                ) {
                    LojiaSectionHeader("APPLICATION INFO")

                    LojiaSettingRow(
                        icon = Icons.Outlined.Info,
                        title = stringResource(R.string.title_lojia_pos_shift_system),
                        subtitle = stringResource(R.string.desc_app_version_production),
                        onClick = {}
                    )

                    LojiaSettingRow(
                        icon = Icons.Outlined.Verified,
                        title = stringResource(R.string.title_system_architecture),
                        subtitle = stringResource(R.string.desc_system_architecture),
                        onClick = {}
                    )

                    LojiaSectionHeader("LEGAL")

                    LojiaSettingRow(
                        icon = Icons.Outlined.Description,
                        title = stringResource(R.string.terms_conditions),
                        subtitle = stringResource(R.string.desc_review_eula_terms),
                        trailing = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = TextHintColor,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = { showTermsModal = true }
                    )

                    LojiaSettingRow(
                        icon = Icons.Outlined.PrivacyTip,
                        title = stringResource(R.string.privacy_policy),
                        subtitle = stringResource(R.string.desc_gdpr_cloud_security),
                        trailing = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = TextHintColor,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = { showPrivacyModal = true }
                    )
                }
            }

            // =================================================================
            // 6. SUPPORT
            // =================================================================
            "support" -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = LojiaDimens.ScreenPadding, vertical = LojiaDimens.ScreenTopPadding)
                ) {
                    LojiaSectionHeader("CONTACT SUPPORT")

                    LojiaSettingRow(
                        icon = Icons.Outlined.HeadsetMic,
                        title = stringResource(R.string.title_24_7_helpline),
                        subtitle = "+880 1700-000000",
                        trailing = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = TextHintColor,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+8801700000000")).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        }
                    )

                    LojiaSettingRow(
                        icon = Icons.Outlined.Chat,
                        title = stringResource(R.string.title_whatsapp_support),
                        subtitle = stringResource(R.string.desc_whatsapp_support),
                        trailing = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = TextHintColor,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/8801700000000")).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        }
                    )
                }
            }

            // =================================================================
            // 7. SWITCH MODULE (Not needed - Single Module)
            // =================================================================
            "switch_module" -> {
                // Handled / Single module mode
            }
        }
    }

    // =========================================================================
    // MODALS
    // =========================================================================

    // Edit field modal
    if (editFieldDialog != null) {
        val (fieldName, fieldVal) = editFieldDialog!!
        var tempVal by remember(fieldName) { mutableStateOf(fieldVal) }
        AlertDialog(
            onDismissRequest = { editFieldDialog = null },
            containerColor = SurfaceLight,
            title = { Text(stringResource(R.string.edit_field_title, fieldName), fontWeight = FontWeight.Bold) },
            text = {
                androidx.compose.foundation.layout.Box(modifier = Modifier.verticalScroll(rememberScrollState()).imePadding()) {
                    LojiaTextField(
                        value = tempVal,
                        onValueChange = { tempVal = it },
                        label = { Text(fieldName) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        when (fieldName) {
                            "Full name" -> { fullName = tempVal; persistProfile(fName = tempVal) }
                            "Username" -> { username = tempVal; persistProfile(uName = tempVal) }
                            "Email" -> { email = tempVal; persistProfile(mail = tempVal) }
                            "Password" -> { password = tempVal; persistProfile(pass = tempVal) }
                            "Number" -> { phone = tempVal; persistProfile(ph = tempVal) }
                            "Address" -> { address = tempVal; persistProfile(addr = tempVal) }
                            "Google Account" -> reportViewModel.googleAccount.value = tempVal
                        }
                        Toast.makeText(context, context.getString(R.string.field_updated_msg, fieldName), Toast.LENGTH_SHORT).show()
                        editFieldDialog = null
                    }
                ) {
                    Text(stringResource(R.string.save_5))
                }
            },
            dismissButton = { TextButton(onClick = { editFieldDialog = null }) { Text(stringResource(R.string.cancel_18)) } }
        )
    }

    // Google Maps Location Picker Modal
    if (showMapLocationPicker) {
        GoogleMapsLocationPickerModal(
            initialAddress = address,
            initialLat = mapLat,
            initialLng = mapLng,
            onLocationConfirmed = { newAddr, newLat, newLng ->
                showMapLocationPicker = false
                if (newAddr.isNotBlank()) {
                    address = newAddr
                }
                mapLat = newLat
                mapLng = newLng
                reportViewModel.updateBusiness(
                    com.lojia.shiftreport.data.UpdateBusinessRequest(
                        address = newAddr,
                        mapLat = newLat,
                        mapLng = newLng
                    )
                )
            },
            onDismiss = { showMapLocationPicker = false }
        )
    }

    // Terms Modal
    if (showTermsModal) {
        AlertDialog(
            onDismissRequest = { showTermsModal = false },
            containerColor = SurfaceLight,
            title = { Text(stringResource(R.string.terms_of_service), fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    stringResource(R.string.by_using_lojia_shift),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = { TextButton(onClick = { showTermsModal = false }) { Text(stringResource(R.string.close_7)) } }
        )
    }

    // Privacy Modal
    if (showPrivacyModal) {
        AlertDialog(
            onDismissRequest = { showPrivacyModal = false },
            containerColor = SurfaceLight,
            title = { Text(stringResource(R.string.privacy_policy_2), fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    stringResource(R.string.your_financial_data_shift),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = { TextButton(onClick = { showPrivacyModal = false }) { Text(stringResource(R.string.close_7)) } }
        )
    }

    // Add Cashier Modal
    if (showAddCashierDialog) {
        AddCashierDialog(
            onDismissRequest = { showAddCashierDialog = false },
            onConfirmAdd = { name, pin ->
                reportViewModel.addCashier(name, pin, "CASHIER")
                Toast.makeText(context, context.getString(R.string.user_added_success_msg, "Cashier", name), Toast.LENGTH_SHORT).show()
                showAddCashierDialog = false
            }
        )
    }

    // Delete Cashier Confirmation Modal
    if (cashierToDelete != null) {
        val target = cashierToDelete!!
        AlertDialog(
            onDismissRequest = { cashierToDelete = null },
            containerColor = SurfaceLight,
            title = { Text(stringResource(R.string.remove_member), fontWeight = FontWeight.Bold) },
            text = {
                Text(stringResource(R.string.confirm_delete_cashier_prompt, target.name, target.role))
            },
            confirmButton = {
                Button(
                    onClick = {
                        reportViewModel.deleteCashier(target)
                        Toast.makeText(context, context.getString(R.string.user_deleted_msg, target.name), Toast.LENGTH_SHORT).show()
                        cashierToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRedLight)
                ) {
                    Text(stringResource(R.string.delete_1))
                }
            },
            dismissButton = {
                TextButton(onClick = { cashierToDelete = null }) { Text(stringResource(R.string.cancel_18)) }
            }
        )
    }

    if (showQuickPinDialog) {
        var tempPin by remember { mutableStateOf("") }
        var pinError by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = {
                showQuickPinDialog = false
                if (!preferencesRepository.hasPinConfigured()) {
                    quickLoginEnabled = false
                    preferencesRepository.setQuickLoginEnabled(false)
                }
            },
            containerColor = SurfaceLight,
            title = {
                Text(
                    text = if (preferencesRepository.hasPinConfigured()) "Change 4-Digit Quick PIN" else "Set 4-Digit Quick PIN",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = OnSurfaceLight
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter a 4-digit numeric PIN for fast, secure app login.",
                        fontSize = 14.sp,
                        color = OnSurfaceVariantLight
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = tempPin,
                        onValueChange = {
                            if (it.length <= 4 && it.all { ch -> ch.isDigit() }) {
                                tempPin = it
                                pinError = null
                            }
                        },
                        label = { Text("4-Digit PIN") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (pinError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = pinError ?: "",
                            color = ErrorRedLight,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempPin.length != 4) {
                            pinError = "PIN must be exactly 4 digits"
                        } else {
                            preferencesRepository.setQuickPin(tempPin)
                            preferencesRepository.setQuickLoginEnabled(true)
                            pinValue = tempPin
                            persistProfile(p = tempPin)
                            quickLoginEnabled = true
                            showQuickPinDialog = false
                            Toast.makeText(context, "Quick PIN saved & Quick Login enabled!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigoLight)
                ) {
                    Text("Save PIN")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showQuickPinDialog = false
                        if (!preferencesRepository.hasPinConfigured()) {
                            quickLoginEnabled = false
                            preferencesRepository.setQuickLoginEnabled(false)
                        }
                    }
                ) {
                    Text("Cancel", color = OnSurfaceVariantLight)
                }
            }
        )
    }

    // Restore Backup Confirmation Dialog
    if (showRestoreConfirmDialog) {
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

                    if (pendingBackupSummary != null) {
                        val s = pendingBackupSummary!!
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
                                    text = "• Shift Reports: ${s.shiftReportsCount}",
                                    fontSize = 12.sp,
                                    color = WarningOrange
                                )
                                Text(
                                    text = "• Shift Sessions: ${s.shiftSessionsCount} | Cashiers: ${s.cashiersCount}",
                                    fontSize = 12.sp,
                                    color = WarningOrange
                                )
                                Text(
                                    text = "• Total Records: ${s.totalRecords} (Date: ${s.exportDate})",
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
                        val uri = pendingRestoreUri
                        showRestoreConfirmDialog = false
                        if (uri != null) {
                            reportViewModel.restoreOfflineBackup(uri)
                        }
                        pendingRestoreUri = null
                        pendingBackupSummary = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WarningOrange)
                ) {
                    Text("Restore Now", fontWeight = FontWeight.Bold)
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
                                            reportViewModel.parseDriveBackupSummary(item) { summary ->
                                                pendingDriveBackupSummary = summary
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
                            Text("Reading backup details...", fontSize = 12.sp, color = PrimaryIndigoLight)
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
    if (showDriveRestoreConfirmDialog && pendingDriveBackupItem != null) {
        val driveItem = pendingDriveBackupItem!!
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
                                text = "☁️ Google Drive: ${driveItem.name}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = WarningOrange
                            )
                            if (pendingDriveBackupSummary != null) {
                                val s = pendingDriveBackupSummary!!
                                Text(
                                    text = "• Shift Reports: ${s.shiftReportsCount}",
                                    fontSize = 12.sp,
                                    color = WarningOrange
                                )
                                Text(
                                    text = "• Shift Sessions: ${s.shiftSessionsCount} | Cashiers: ${s.cashiersCount}",
                                    fontSize = 12.sp,
                                    color = WarningOrange
                                )
                                Text(
                                    text = "• Total Records: ${s.totalRecords} (Export Date: ${s.exportDate})",
                                    fontSize = 11.sp,
                                    color = WarningOrange
                                )
                            } else {
                                Text(
                                    text = "Size: ${driveItem.size / 1024} KB • Modified: ${driveItem.modifiedTime}",
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
                        val itemToRestore = pendingDriveBackupItem
                        showDriveRestoreConfirmDialog = false
                        if (itemToRestore != null) {
                            reportViewModel.restoreFromGoogleDrive(itemToRestore)
                        }
                        pendingDriveBackupItem = null
                        pendingDriveBackupSummary = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WarningOrange)
                ) {
                    Text("Restore Now", fontWeight = FontWeight.Bold)
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
