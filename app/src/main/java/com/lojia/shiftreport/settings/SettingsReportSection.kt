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
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsReportSection(
    reportViewModel: ReportViewModel,
    language: AppLanguage,
    isAdmin: Boolean,
    onRestrictedClick: (action: () -> Unit) -> Unit,
    onSwitchModule: (AppModule) -> Unit,
    onConfigurePrinterClick: () -> Unit = {},
    onNavigateToSection: (String) -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val userProfile by reportViewModel.userProfile.collectAsState()
    val businessProfile by reportViewModel.businessProfile.collectAsState()
    val googleAccount: String by reportViewModel.googleAccount.collectAsState()

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
                            Text(stringResource(R.string.daily_shift_report), fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = OnSurfaceLight)
                            Text(stringResource(R.string.audit_drawer_cash_create), fontSize = 11.5.sp, color = OnSurfaceVariantLight)
                        }
                    }
                    Button(
                        onClick = { onSwitchModule(AppModule.SHIFT_REPORT) },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigoLight),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(stringResource(R.string.open_2), fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                Text(
                    text = stringResource(R.string.staff_admin_section),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextHintColor,
                    modifier = Modifier.padding(start = 20.dp, top = 6.dp, bottom = 4.dp)
                )

                LoyverseMenuItemRow(
                    icon = Icons.Outlined.Person,
                    title = "Profile",
                    subtitle = stringResource(R.string.profile_subtitle),
                    onClick = { onNavigateToSection("profile") }
                )

                LoyverseMenuItemRow(
                    icon = Icons.Outlined.People,
                    title = "Cashier",
                    subtitle = stringResource(R.string.cashiers_subtitle),
                    onClick = { onNavigateToSection("cashiers") }
                )

                Text(
                    text = stringResource(R.string.security_data_section),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextHintColor,
                    modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 4.dp)
                )

                LoyverseMenuItemRow(
                    icon = Icons.Outlined.Shield,
                    title = "Security",
                    subtitle = stringResource(R.string.security_subtitle),
                    onClick = { onNavigateToSection("security") }
                )

                LoyverseMenuItemRow(
                    icon = Icons.Outlined.CloudUpload,
                    title = "Backup",
                    subtitle = stringResource(R.string.backup_subtitle),
                    onClick = { onNavigateToSection("backup") }
                )

                LoyverseMenuItemRow(
                    icon = Icons.Outlined.Print,
                    title = "Thermal Printer (ESC/POS)",
                    subtitle = "Configure Bluetooth & Network receipt printers",
                    onClick = onConfigurePrinterClick
                )

                Text(
                    text = stringResource(R.string.regional_interface_section),
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
                        onNavigateToSection("regional")
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )

                Text(
                    text = stringResource(R.string.system_support_section),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextHintColor,
                    modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 4.dp)
                )

                LoyverseMenuItemRow(
                    icon = Icons.Outlined.Info,
                    title = "About",
                    subtitle = stringResource(R.string.about_subtitle),
                    onClick = { onNavigateToSection("about") }
                )

                LoyverseMenuItemRow(
                    icon = Icons.Outlined.HeadsetMic,
                    title = "Support",
                    subtitle = stringResource(R.string.support_subtitle),
                    onClick = { onNavigateToSection("support") }
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
                    onLogoutClick = onNavigateBack
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
                            contentDescription = stringResource(R.string.cd_quick_login_pin),
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
                                        Toast.makeText(context, context.getString(R.string.quick_login_enabled), Toast.LENGTH_SHORT).show()
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
                                    Toast.makeText(context, context.getString(R.string.quick_login_disabled), Toast.LENGTH_SHORT).show()
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
                            contentDescription = stringResource(R.string.cd_change_pin),
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
                                        Toast.makeText(context, context.getString(R.string.settings_enable_pin_first_biometric), Toast.LENGTH_LONG).show()
                                        showQuickPinDialog = true
                                        return@Switch
                                    }

                                    val fragActivity = context as? FragmentActivity
                                    val bioStatus = BiometricAuthManager.checkBiometricAvailability(context)
                                    if (fragActivity != null && bioStatus == BiometricStatus.AVAILABLE) {
                                        BiometricAuthManager.showBiometricPrompt(
                                            activity = fragActivity,
                                            title = context.getString(R.string.admin_reauth_default_title),
                                            subtitle = context.getString(R.string.biometric_touch_to_activate),
                                            description = context.getString(R.string.biometric_verifying_security),
                                            onResult = { result ->
                                                when (result) {
                                                    is BiometricAuthResult.Success -> {
                                                        biometricEnabled = true
                                                        preferencesRepository.setBiometricEnabled(true)
                                                        persistProfile(bio = true)
                                                        Toast.makeText(context, context.getString(R.string.biometric_login_enabled), Toast.LENGTH_SHORT).show()
                                                    }
                                                    is BiometricAuthResult.Failed -> {
                                                        Toast.makeText(context, context.getString(R.string.fingerprint_not_recognized), Toast.LENGTH_SHORT).show()
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
                SettingsBackupSubSection(
                    reportViewModel = reportViewModel,
                    onRestrictedClick = onRestrictedClick
                )
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
            userProfile = userProfile,
            onDismissRequest = { showAddCashierDialog = false },
            onConfirmAdd = { name ->
                reportViewModel.addCashier(name, "", "CASHIER")
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
                        label = { Text(stringResource(R.string.four_digit_pin_label)) },
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
                            pinError = context.getString(R.string.pin_must_be_4_digits)
                        } else {
                            preferencesRepository.setQuickPin(tempPin)
                            preferencesRepository.setQuickLoginEnabled(true)
                            pinValue = tempPin
                            persistProfile(p = tempPin)
                            quickLoginEnabled = true
                            showQuickPinDialog = false
                            Toast.makeText(context, context.getString(R.string.settings_quick_pin_saved), Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigoLight)
                ) {
                    Text(stringResource(R.string.save_pin_btn))
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
                    Text(stringResource(R.string.cancel_18), color = OnSurfaceVariantLight)
                }
            }
        )
    }

}

