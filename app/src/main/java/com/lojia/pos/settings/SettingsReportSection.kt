package com.lojia.pos.settings

import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.lojia.pos.ui.common.LojiaTextField
import com.lojia.pos.R
import com.lojia.pos.data.*
import com.lojia.pos.util.*
import com.lojia.pos.ui.common.*
import com.lojia.pos.ui.theme.*
import com.lojia.pos.auth.*
import com.lojia.pos.pos.*
import com.lojia.pos.report.*
import com.lojia.pos.settings.*
import com.lojia.pos.sync.*

import com.lojia.pos.util.AppLanguageManager
import com.lojia.pos.util.SecurityUtils


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
    onSwitchModule: (AppModule) -> Unit
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

    // 2. Security State
    val preferencesRepository = remember(context) { com.lojia.pos.data.PreferencesRepository.getInstance(context) }
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
            .background(Color.White)
    ) {

        // Quick Action Banner: Daily Shift Report Generator (Shown only in root settings overview or shift sub-menu)
        if (isRootMenu || currentMenu == "daily_shift_report") {
            Surface(
                color = Color(0xFFF0FDF4),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
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
                                .background(Color(0xFF00796B), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Assessment, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            AutoText(id = R.string.daily_shift_report, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = Color(0xFF0F172A))
                            AutoText(id = R.string.audit_drawer_cash_create, fontSize = 11.5.sp, color = Color(0xFF475569))
                        }
                    }
                    Button(
                        onClick = { onSwitchModule(AppModule.SHIFT_REPORT) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00796B)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        AutoText(id = R.string.open_2, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))
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
                    color = Color(0xFF9E9E9E),
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
                    color = Color(0xFF9E9E9E),
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

                AutoText(
                    id = R.string.regional_interface_section,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF9E9E9E),
                    modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 4.dp)
                )

                LoyverseMenuItemRow(
                    icon = Icons.Outlined.Language,
                    title = "Language",
                    subtitle = "${stringResource(R.string.language_subtitle)}: ${language.displayName}",
                    onClick = { reportViewModel.selectReportSettingsMenu("language") }
                )

                AutoText(
                    id = R.string.system_support_section,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF9E9E9E),
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
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00796B)),
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    val bProfile = businessProfile ?: com.lojia.pos.data.BusinessProfile()
                    BusinessLogoPickerCard(
                        businessProfile = bProfile,
                        onSaveProfile = { updated ->
                            reportViewModel.saveBusinessProfile(updated)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Clean List Card for Profile Detail Items
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            ProfileDetailItemRow(
                                icon = Icons.Outlined.Badge,
                                label = stringResource(R.string.full_name),
                                value = fullName,
                                onClick = { editFieldDialog = "Full name" to fullName }
                            )
                            ProfileDetailItemRow(
                                icon = Icons.Outlined.AccountCircle,
                                label = stringResource(R.string.username),
                                value = username,
                                onClick = { editFieldDialog = "Username" to username }
                            )
                            ProfileDetailItemRow(
                                icon = Icons.Outlined.Email,
                                label = stringResource(R.string.email),
                                value = email,
                                onClick = { editFieldDialog = "Email" to email }
                            )
                            ProfileDetailItemRow(
                                icon = Icons.Outlined.Lock,
                                label = stringResource(R.string.password),
                                value = "••••••••",
                                onClick = { editFieldDialog = "Password" to password }
                            )
                            ProfileDetailItemRow(
                                icon = Icons.Outlined.Phone,
                                label = stringResource(R.string.phone_number),
                                value = phone,
                                onClick = { editFieldDialog = "Number" to phone }
                            )
                            ProfileDetailItemRow(
                                icon = Icons.Outlined.LocationOn,
                                label = stringResource(R.string.address),
                                value = address,
                                isLastItem = true,
                                onClick = { editFieldDialog = "Address" to address }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // =================================================================
            // 2. SECURITY (Biometric & MPIN)
            // =================================================================
            "security", "mpin", "pin" -> {
                var isMpinExpanded by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Option 1: Quick Login (PIN)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        shadowElevation = 1.dp,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .background(Color(0xFFEFF6FF), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Lock,
                                            contentDescription = "Quick Login",
                                            tint = Color(0xFF3858F6),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "Quick Login",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF1E293B)
                                        )
                                        Text(
                                            text = if (quickLoginEnabled) "Active with 4-digit PIN" else "Login quickly with a 4-digit PIN",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (quickLoginEnabled) Color(0xFF3858F6) else Color(0xFF64748B)
                                        )
                                    }
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
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFF3858F6),
                                        uncheckedThumbColor = Color.White,
                                        uncheckedTrackColor = Color(0xFFCBD5E1)
                                    )
                                )
                            }
                            if (quickLoginEnabled) {
                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = Color(0xFFF1F5F9))
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showQuickPinDialog = true }
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Change 4-Digit PIN",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF3858F6)
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Option 2: Biometric Login
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        shadowElevation = 1.dp,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(Color(0xFFF0FDF4), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Fingerprint,
                                        contentDescription = stringResource(R.string.cd_biometric_icon),
                                        tint = Color(0xFF16A34A),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = stringResource(R.string.biometric_login),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF1E293B)
                                    )
                                    Text(
                                        text = if (!quickLoginEnabled) "Requires Quick Login and PIN as fallback"
                                               else if (biometricEnabled) stringResource(R.string.biometric_enabled_desc)
                                               else stringResource(R.string.biometric_disabled_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (!quickLoginEnabled) Color(0xFF94A3B8)
                                                else if (biometricEnabled) Color(0xFF16A34A)
                                                else Color(0xFF64748B)
                                    )
                                }
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
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF16A34A),
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color(0xFFCBD5E1)
                                )
                            )
                        }
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
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Header
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(R.string.backup_and_storage),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Keep your business data safe on this device",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF64748B)
                        )
                    }

                    // 2. Status Card (Simple, clean, calm)
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(
                                            if (lastBackupTime > 0L) Color(0xFFDCFCE7) else Color(0xFFFEF3C7),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (lastBackupTime > 0L) Icons.Outlined.CheckCircle else Icons.Outlined.Schedule,
                                        contentDescription = null,
                                        tint = if (lastBackupTime > 0L) Color(0xFF16A34A) else Color(0xFFD97706),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = stringResource(R.string.last_backup),
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = Color(0xFF64748B)
                                    )
                                    Text(
                                        text = if (lastBackupTime > 0L) lastBackupFormatted else "Never",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF0F172A)
                                    )
                                }
                            }

                            // Indicator Badge
                            Surface(
                                color = if (lastBackupTime > 0L) Color(0xFFDCFCE7) else Color(0xFFFEF3C7),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text(
                                    text = if (lastBackupTime > 0L) "Protected" else "Action Needed",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (lastBackupTime > 0L) Color(0xFF15803D) else Color(0xFFB45309)
                                )
                            }
                        }
                    }

                    // 3. Primary Actions (Clean, no duplicate buttons)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
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
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("create_backup_btn")
                        ) {
                            if (isBackingUp) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.5.dp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.backup_in_progress),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            } else {
                                Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.backup_data_title),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                restoreFileLauncher.launch("*/*")
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("restore_from_file_btn")
                        ) {
                            Icon(Icons.Outlined.Restore, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.restore_backup_title),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    // Recently Created Backup Banner Card (if active)
                    if (lastCreatedBackupInfo != null) {
                        val info = lastCreatedBackupInfo!!
                        Surface(
                            color = Color(0xFFF0FDF4),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.backup_created_success, info.fileName),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF15803D)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${info.savedPath} • ${info.totalRecords} records",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF166534)
                                    )
                                }
                                IconButton(onClick = { lastCreatedBackupInfo = null }) {
                                    Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color(0xFF166534), modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFFF1F5F9))

                    // 4. Auto Backup (Single Row)
                    Surface(
                        onClick = { showAutoBackupDialog = true },
                        color = Color.White,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(0xFFF1F5F9), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Outlined.Autorenew, contentDescription = null, tint = Color(0xFF475569), modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = stringResource(R.string.auto_backups),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = when (autoBackupFreq) {
                                            "Daily" -> stringResource(R.string.daily)
                                            "Weekly" -> stringResource(R.string.weekly)
                                            "Monthly" -> stringResource(R.string.monthly)
                                            else -> autoBackupFreq ?: stringResource(R.string.daily)
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF94A3B8))
                        }
                    }

                    HorizontalDivider(color = Color(0xFFF1F5F9))

                    // 5. Google Drive (Separate Compact Section)
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(Color(0xFFEFF6FF), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Outlined.CloudQueue, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Google Drive",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = if (driveAccount.isConnected) {
                                                driveAccount.email ?: stringResource(R.string.drive_connected_as, "")
                                            } else {
                                                stringResource(R.string.drive_not_connected)
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (driveAccount.isConnected) Color(0xFF16A34A) else Color(0xFF64748B)
                                        )
                                    }
                                }

                                if (driveAccount.isConnected) {
                                    TextButton(
                                        onClick = { reportViewModel.disconnectGoogleDrive() },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(stringResource(R.string.btn_disconnect_drive), color = Color(0xFFDC2626), fontSize = 12.sp)
                                    }
                                } else {
                                    Button(
                                        onClick = { reportViewModel.connectGoogleDrive(context) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
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
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Backup to Drive
                                    Button(
                                        onClick = { reportViewModel.backupToGoogleDrive() },
                                        enabled = !isDriveUploading,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        if (isDriveUploading) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                        } else {
                                            Icon(Icons.Outlined.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(stringResource(R.string.backup_to_drive_title), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    // Restore from Drive
                                    OutlinedButton(
                                        onClick = {
                                            reportViewModel.fetchGoogleDriveBackups()
                                            showDriveBackupsPicker = true
                                        },
                                        enabled = !isDriveDownloading && !isDriveLoadingList,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        if (isDriveDownloading || isDriveLoadingList) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF2563EB), strokeWidth = 2.dp)
                                        } else {
                                            Icon(Icons.Outlined.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(stringResource(R.string.restore_from_drive_title), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFFF1F5F9))

                    // 6. Cloud Sync / Firebase (Clean Submenu)
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, if (firebaseSyncState.isEnabled) Color(0xFF38BDF8) else Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(if (firebaseSyncState.isEnabled) Color(0xFF0284C7) else Color(0xFF94A3B8), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Outlined.CloudSync, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = stringResource(R.string.firebase_sync_title),
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = if (firebaseSyncState.lastSyncTimestamp > 0L) {
                                                stringResource(R.string.firebase_sync_last_time, FirebaseCloudSyncManager.formatSyncTime(firebaseSyncState.lastSyncTimestamp))
                                            } else {
                                                stringResource(R.string.firebase_sync_never)
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }

                                Switch(
                                    checked = firebaseSyncState.isEnabled,
                                    onCheckedChange = { isChecked ->
                                        reportViewModel.toggleFirebaseCloudSync(isChecked)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFF0284C7)
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
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (firebaseSyncState.isSyncing) Color(0xFF0284C7) else Color(0xFF15803D)
                                    )

                                    Button(
                                        onClick = { reportViewModel.syncFirebaseNow() },
                                        enabled = !firebaseSyncState.isSyncing,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        if (firebaseSyncState.isSyncing) {
                                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Color.White)
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

                    Spacer(modifier = Modifier.height(8.dp))

                    // 7. Danger Zone: Clear Data
                    Surface(
                        color = Color(0xFFFEF2F2),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.clear_all_data_title),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF991B1B)
                                )
                                Text(
                                    text = stringResource(R.string.clear_all_data_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFB91C1C)
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    onRestrictedClick {
                                        showClearAllDataConfirmDialog = true
                                    }
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                                border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(stringResource(R.string.clear_all_data_title), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Auto Backup frequency picker dialog
                if (showAutoBackupDialog) {
                    AlertDialog(
                        onDismissRequest = { showAutoBackupDialog = false },
                        title = {
                            Text(
                                text = stringResource(R.string.auto_backups),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
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
                                        color = if (isSelected) Color(0xFFEFF6FF) else Color.Transparent,
                                        shape = RoundedCornerShape(10.dp),
                                        border = if (isSelected) BorderStroke(1.dp, Color(0xFF3B82F6)) else null,
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
                                                color = if (isSelected) Color(0xFF1D4ED8) else Color(0xFF0F172A)
                                            )
                                            if (isSelected) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF1D4ED8), modifier = Modifier.size(20.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showAutoBackupDialog = false }) {
                                Text(stringResource(R.string.cancel_18))
                            }
                        }
                    )
                }
            }

            // =================================================================
            // 4. LANGUAGE / COUNTRY SELECTION
            // =================================================================
            "country", "countries", "currency", "language" -> {
                // Top Segmented Tab Row
                TabRow(
                    selectedTabIndex = langCountryTab,
                    containerColor = Color(0xFFF8FAFC),
                    contentColor = Color(0xFF4F46E5),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = langCountryTab == 0,
                        onClick = { langCountryTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🌐 ", fontSize = 14.sp)
                                Text(stringResource(R.string.language), fontWeight = if (langCountryTab == 0) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    )
                    Tab(
                        selected = langCountryTab == 1,
                        onClick = { langCountryTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🌍 ", fontSize = 14.sp)
                                Text(stringResource(R.string.country), fontWeight = if (langCountryTab == 1) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    )
                }

                if (langCountryTab == 0) {
                    // ==========================================
                    // 🌐 APP LANGUAGE TAB (POWERED BY DATASTORE)
                    // ==========================================
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        LanguageSettingsComponent(
                            currentLanguage = language,
                            accentColor = PrimaryIndigo,
                            onLanguageChanged = { newLang ->
                                reportViewModel.setLanguage(newLang)
                            }
                        )
                    }
                } else {
                    // ==========================================
                    // 🌍 COUNTRY TAB (RIGHT TAB - NO TOP CARD)
                    // ==========================================
                    LojiaTextField(
                        value = countrySearch,
                        onValueChange = { countrySearch = it },
                        placeholder = { Text(stringResource(R.string.search_country), fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    val allCountries = AppCountry.values().filter {
                        it.displayNameEn.contains(countrySearch, ignoreCase = true) ||
                        it.displayNameBn.contains(countrySearch, ignoreCase = true) ||
                        it.displayNameAr.contains(countrySearch, ignoreCase = true) ||
                        it.currencyCode.contains(countrySearch, ignoreCase = true) ||
                        it.currencySymbol.contains(countrySearch, ignoreCase = true)
                    }

                    allCountries.forEach { c ->
                        val isSelected = c == currentCountry
                        val displayName = c.getLocalizedName(language)

                        Surface(
                            color = if (isSelected) Color(0xFFF5F3FF) else Color.White,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    reportViewModel.setCountry(c)
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.country_set_toast_fmt, displayName, c.currencyCode, c.currencySymbol),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(text = c.flag, fontSize = 24.sp)
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        Text(
                                            text = displayName,
                                            fontSize = 14.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color(0xFF4F46E5) else Color(0xFF1E293B)
                                        )
                                        Text(
                                            text = "${c.currencyCode} (${c.currencySymbol}) • VAT: ${c.defaultVatRate.toInt()}%",
                                            fontSize = 12.sp,
                                            color = if (isSelected) Color(0xFF6366F1) else Color(0xFF64748B)
                                        )
                                    }
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = stringResource(R.string.cd_selected),
                                        tint = Color(0xFF4F46E5),
                                        modifier = Modifier.size(22.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Outlined.RadioButtonUnchecked,
                                        contentDescription = stringResource(R.string.cd_select),
                                        tint = Color(0xFFCBD5E1),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 0.5.dp)
                    }
                }
            }

            // =================================================================
            // 5. ABOUT
            // =================================================================
            "about" -> {
                LoyverseMenuItemRow(
                    icon = Icons.Outlined.Info,
                    title = stringResource(R.string.title_lojia_pos_shift_system),
                    subtitle = stringResource(R.string.desc_app_version_production),
                    onClick = {}
                )
                LoyverseMenuItemRow(
                    icon = Icons.Outlined.Verified,
                    title = stringResource(R.string.title_system_architecture),
                    subtitle = stringResource(R.string.desc_system_architecture),
                    onClick = {}
                )
                LoyverseMenuItemRow(
                    icon = Icons.Outlined.Description,
                    title = stringResource(R.string.terms_conditions),
                    subtitle = stringResource(R.string.desc_review_eula_terms),
                    onClick = { showTermsModal = true }
                )
                LoyverseMenuItemRow(
                    icon = Icons.Outlined.PrivacyTip,
                    title = stringResource(R.string.privacy_policy),
                    subtitle = stringResource(R.string.desc_gdpr_cloud_security),
                    onClick = { showPrivacyModal = true }
                )
            }

            // =================================================================
            // 6. SUPPORT
            // =================================================================
            "support" -> {
                LoyverseMenuItemRow(
                    icon = Icons.Outlined.HeadsetMic,
                    title = stringResource(R.string.title_24_7_helpline),
                    subtitle = "+880 1700-000000",
                    onClick = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+8801700000000")).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    }
                )
                LoyverseMenuItemRow(
                    icon = Icons.Outlined.Chat,
                    title = stringResource(R.string.title_whatsapp_support),
                    subtitle = stringResource(R.string.desc_whatsapp_support),
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/8801700000000")).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    }
                )
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

    // Terms Modal
    if (showTermsModal) {
        AlertDialog(
            onDismissRequest = { showTermsModal = false },
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
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
            title = {
                Text(
                    text = if (preferencesRepository.hasPinConfigured()) "Change 4-Digit Quick PIN" else "Set 4-Digit Quick PIN",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF1E293B)
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter a 4-digit numeric PIN for fast, secure app login.",
                        fontSize = 14.sp,
                        color = Color(0xFF64748B)
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
                            color = Color(0xFFDC2626),
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3858F6))
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
                    Text("Cancel", color = Color(0xFF64748B))
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
            icon = {
                Icon(
                    Icons.Outlined.Restore,
                    contentDescription = null,
                    tint = Color(0xFFD97706),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.restore_confirm_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(R.string.restore_confirm_message),
                        fontSize = 14.sp,
                        color = Color(0xFF334155),
                        lineHeight = 20.sp
                    )

                    if (pendingBackupSummary != null) {
                        val s = pendingBackupSummary!!
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                            border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "📦 Backup Summary",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF92400E)
                                )
                                Text(
                                    text = "• Shift Reports: ${s.shiftReportsCount}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF92400E)
                                )
                                Text(
                                    text = "• Shift Sessions: ${s.shiftSessionsCount} | Cashiers: ${s.cashiersCount}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF92400E)
                                )
                                Text(
                                    text = "• Total Records: ${s.totalRecords} (Date: ${s.exportDate})",
                                    fontSize = 11.sp,
                                    color = Color(0xFF78350F)
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
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
                    Text(stringResource(R.string.cancel_18), color = Color(0xFF64748B))
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
            icon = {
                Icon(
                    Icons.Outlined.CloudDownload,
                    contentDescription = null,
                    tint = Color(0xFF2563EB),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.drive_pick_backup_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
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
                            CircularProgressIndicator(color = Color(0xFF2563EB))
                        }
                    } else if (driveBackupsList.isEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Outlined.FolderOpen, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.drive_no_backups_found),
                                    fontSize = 13.sp,
                                    color = Color(0xFF64748B),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Select a backup file from your Google Drive app folder:",
                            fontSize = 12.sp,
                            color = Color(0xFF475569)
                        )
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(driveBackupsList) { item ->
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
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Outlined.Description, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(28.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.name,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0F172A)
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "${item.description.ifEmpty { "Backup" }} • ${item.modifiedTime.take(16)}",
                                                fontSize = 11.sp,
                                                color = Color(0xFF64748B)
                                            )
                                            if (item.size > 0) {
                                                Text(
                                                    text = "${item.size / 1024} KB",
                                                    fontSize = 10.sp,
                                                    color = Color(0xFF94A3B8)
                                                )
                                            }
                                        }
                                        Icon(
                                            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                            contentDescription = null,
                                            tint = Color(0xFF94A3B8),
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
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color(0xFF2563EB))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reading backup details...", fontSize = 12.sp, color = Color(0xFF2563EB))
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
                    Text(stringResource(R.string.cancel_18), color = Color(0xFF64748B))
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
            icon = {
                Icon(
                    Icons.Outlined.CloudDownload,
                    contentDescription = null,
                    tint = Color(0xFFD97706),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.drive_restore_confirm_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(R.string.drive_restore_confirm_message),
                        fontSize = 14.sp,
                        color = Color(0xFF334155),
                        lineHeight = 20.sp
                    )

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "☁️ Google Drive: ${driveItem.name}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E)
                            )
                            if (pendingDriveBackupSummary != null) {
                                val s = pendingDriveBackupSummary!!
                                Text(
                                    text = "• Shift Reports: ${s.shiftReportsCount}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF92400E)
                                )
                                Text(
                                    text = "• Shift Sessions: ${s.shiftSessionsCount} | Cashiers: ${s.cashiersCount}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF92400E)
                                )
                                Text(
                                    text = "• Total Records: ${s.totalRecords} (Export Date: ${s.exportDate})",
                                    fontSize = 11.sp,
                                    color = Color(0xFF78350F)
                                )
                            } else {
                                Text(
                                    text = "Size: ${driveItem.size / 1024} KB • Modified: ${driveItem.modifiedTime}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF78350F)
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
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
                    Text(stringResource(R.string.cancel_18), color = Color(0xFF64748B))
                }
            }
        )
    }

    // Clear All Business Data Confirmation Dialog (Danger Zone)
    if (showClearAllDataConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDataConfirmDialog = false },
            icon = {
                Icon(
                    Icons.Outlined.WarningAmber,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.delete_all_data_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF1E293B)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(R.string.delete_all_data_message),
                        fontSize = 14.sp,
                        color = Color(0xFF475569),
                        lineHeight = 20.sp
                    )

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.Shield,
                                contentDescription = null,
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.backup_recommended_warning),
                                fontSize = 12.sp,
                                color = Color(0xFF1E40AF),
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
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2563EB))
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
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                    ) {
                        Text(stringResource(R.string.btn_delete_everything), fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearAllDataConfirmDialog = false }
                ) {
                    Text(stringResource(R.string.cancel_18), color = Color(0xFF64748B))
                }
            }
        )
    }
}
