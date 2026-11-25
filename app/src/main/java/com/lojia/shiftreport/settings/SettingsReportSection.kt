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

import com.lojia.shiftreport.util.SecurityUtils


import android.content.Intent

import android.net.Uri

import android.widget.Toast


import androidx.compose.animation.*


import androidx.compose.animation.core.*


import androidx.compose.foundation.background


import androidx.compose.foundation.layout.*


import androidx.compose.foundation.shape.RoundedCornerShape


import androidx.compose.material.icons.Icons
import androidx.fragment.app.FragmentActivity


import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight


import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.*


import androidx.compose.material3.*


import androidx.compose.runtime.*


import androidx.compose.ui.Alignment


import androidx.compose.ui.Modifier


import androidx.compose.ui.graphics.Color


import androidx.compose.ui.platform.LocalContext


import androidx.compose.ui.text.font.FontWeight


import androidx.compose.ui.unit.dp


import androidx.compose.ui.unit.sp


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
    val defaultOwnerLabel = stringResource(R.string.profile_store_owner)
    var fullName by remember(userProfile, defaultOwnerLabel) { mutableStateOf(userProfile?.fullName ?: defaultOwnerLabel) }
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
    var pendingSecurityAuthSubtitle by remember { mutableStateOf<String?>(null) }
    var pendingSecurityAuthAction by remember { mutableStateOf<(() -> Unit)?>(null) }

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
    val depthStyle = LocalDepthStyle.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(depthStyle.reportPageBackground)
    ) {

        // Quick Action Banner: Daily Shift Report Generator (Shown only in root settings overview or shift sub-menu)
        if (isRootMenu || currentMenu == "daily_shift_report") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.SpacingLg, vertical = Dimens.SpacingMd)
                    .cardRise(0)
                    .depthCard(
                        accent = true,
                        cornerRadius = Dimens.CardCornerRadius
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Dimens.ReportCardInnerPadding),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.SettingsIconTitleGap),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(Dimens.SettingsIconTileSize)
                                .depthRaisedIconTile(
                                    gradientColors = listOf(PureWhite, BadgeBlueBg),
                                    cornerRadius = Dimens.RadiusSettingsIconTile
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.Assessment,
                                contentDescription = null,
                                tint = PrimaryBlueLight,
                                modifier = Modifier.size(Dimens.IconField)
                            )
                        }
                        Column {
                            Text(
                                stringResource(R.string.daily_shift_report),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = DepthTypeTokens.ListRowTitleSize,
                                color = TextPrimaryReport
                            )
                            Text(
                                stringResource(R.string.audit_drawer_cash_create),
                                fontSize = DepthTypeTokens.ListRowSubtitleSize,
                                color = OnSurfaceVariantLight
                            )
                        }
                    }
                    Button(
                        onClick = { onSwitchModule(AppModule.SHIFT_REPORT) },
                        elevation = null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            contentColor = PureWhite,
                            disabledContentColor = OnSurfaceVariantLight
                        ),
                        shape = RoundedCornerShape(Dimens.RadiusReportInput),
                        contentPadding = PaddingValues(
                            horizontal = Dimens.ReportCardInnerPadding,
                            vertical = Dimens.SpacingXs
                        ),
                        modifier = Modifier.depthRaisedButton(
                            cornerRadius = Dimens.RadiusReportInput,
                            minHeight = Dimens.ButtonHeightCompact
                        )
                    ) {
                        Text(
                            stringResource(R.string.open_2),
                            fontSize = DepthTypeTokens.PrimaryCtaSmallSize,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        when (currentMenu) {
            // =================================================================
            // 0. ROOT / ALL SETTINGS HUB (Overview of all Shift Report Settings)
            // =================================================================
            "root", "all", "overview" -> {
                Text(
                    text = stringResource(R.string.staff_admin_section).uppercase(),
                    fontSize = DepthTypeTokens.SettingsSectionHeaderSize,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = DepthTypeTokens.SettingsSectionHeaderTracking,
                    color = OnSurfaceVariantLight,
                    modifier = Modifier.padding(
                        start = Dimens.SpacingXl,
                        end = Dimens.SpacingXl,
                        top = Dimens.SpacingSm,
                        bottom = Dimens.SpacingSm
                    )
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.SpacingLg)
                        .cardRise(1)
                        .depthCard(cornerRadius = Dimens.CardCornerRadius)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        LoyverseMenuItemRow(
                            icon = Icons.Outlined.Person,
                            title = stringResource(R.string.profile),
                            subtitle = stringResource(R.string.profile_subtitle),
                            tileColors = listOf(PureWhite, BadgeBlueBg),
                            iconTint = PrimaryBlueLight,
                            onClick = { onNavigateToSection("profile") }
                        )

                        HorizontalDivider(
                            thickness = Dimens.RowDividerWidth,
                            color = depthStyle.listDividerColor,
                            modifier = Modifier.padding(horizontal = Dimens.ReportCardInnerPadding)
                        )

                        LoyverseMenuItemRow(
                            icon = Icons.Outlined.People,
                            title = stringResource(R.string.cashiers),
                            subtitle = stringResource(R.string.cashiers_subtitle),
                            tileColors = listOf(PureWhite, SectionPurpleGradientEnd),
                            iconTint = OnTertiaryContainerLight,
                            onClick = { onNavigateToSection("cashiers") }
                        )

                        HorizontalDivider(
                            thickness = Dimens.RowDividerWidth,
                            color = depthStyle.listDividerColor,
                            modifier = Modifier.padding(horizontal = Dimens.ReportCardInnerPadding)
                        )

                        LoyverseMenuItemRow(
                            icon = Icons.Outlined.Badge,
                            title = stringResource(R.string.nav_employee),
                            subtitle = stringResource(R.string.settings_employees_subtitle),
                            tileColors = listOf(PureWhite, BadgeGreenBg),
                            iconTint = BadgeGreenText,
                            onClick = { onNavigateToSection("employees") }
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.security_data_section).uppercase(),
                    fontSize = DepthTypeTokens.SettingsSectionHeaderSize,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = DepthTypeTokens.SettingsSectionHeaderTracking,
                    color = OnSurfaceVariantLight,
                    modifier = Modifier.padding(
                        start = Dimens.SpacingXl,
                        end = Dimens.SpacingXl,
                        top = Dimens.SpacingLg,
                        bottom = Dimens.SpacingSm
                    )
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.SpacingLg)
                        .cardRise(2)
                        .depthCard(cornerRadius = Dimens.CardCornerRadius)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        LoyverseMenuItemRow(
                            icon = Icons.Outlined.Shield,
                            title = stringResource(R.string.security),
                            subtitle = stringResource(R.string.security_subtitle),
                            tileColors = listOf(PureWhite, SectionRoseGradientEnd),
                            iconTint = ErrorRedLight,
                            onClick = { onNavigateToSection("security") }
                        )

                        HorizontalDivider(
                            thickness = Dimens.RowDividerWidth,
                            color = depthStyle.listDividerColor,
                            modifier = Modifier.padding(horizontal = Dimens.ReportCardInnerPadding)
                        )

                        LoyverseMenuItemRow(
                            icon = Icons.Outlined.CloudUpload,
                            title = stringResource(R.string.backup),
                            subtitle = stringResource(R.string.backup_subtitle),
                            tileColors = listOf(PureWhite, SectionPurpleGradientEnd),
                            iconTint = OnTertiaryContainerLight,
                            onClick = { onNavigateToSection("backup") }
                        )

                        HorizontalDivider(
                            thickness = Dimens.RowDividerWidth,
                            color = depthStyle.listDividerColor,
                            modifier = Modifier.padding(horizontal = Dimens.ReportCardInnerPadding)
                        )

                        LoyverseMenuItemRow(
                            icon = Icons.Outlined.Print,
                            title = stringResource(R.string.settings_thermal_printer_title),
                            subtitle = stringResource(R.string.settings_thermal_printer_subtitle),
                            tileColors = listOf(PureWhite, TileTealEnd),
                            iconTint = SecondaryTealLight,
                            onClick = onConfigurePrinterClick
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.regional_interface_section).uppercase(),
                    fontSize = DepthTypeTokens.SettingsSectionHeaderSize,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = DepthTypeTokens.SettingsSectionHeaderTracking,
                    color = OnSurfaceVariantLight,
                    modifier = Modifier.padding(
                        start = Dimens.SpacingXl,
                        end = Dimens.SpacingXl,
                        top = Dimens.SpacingLg,
                        bottom = Dimens.SpacingSm
                    )
                )

                RegionalPreferencesCard(
                    currentCountry = currentCountry,
                    currentLanguage = language,
                    businessProfile = businessProfile,
                    onOpenRegionalMenu = { tabIndex ->
                        langCountryTab = tabIndex
                        onNavigateToSection("regional")
                    },
                    modifier = Modifier
                        .padding(horizontal = Dimens.SpacingLg)
                        .cardRise(3)
                )

                Text(
                    text = stringResource(R.string.system_support_section).uppercase(),
                    fontSize = DepthTypeTokens.SettingsSectionHeaderSize,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = DepthTypeTokens.SettingsSectionHeaderTracking,
                    color = OnSurfaceVariantLight,
                    modifier = Modifier.padding(
                        start = Dimens.SpacingXl,
                        end = Dimens.SpacingXl,
                        top = Dimens.SpacingLg,
                        bottom = Dimens.SpacingSm
                    )
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.SpacingLg)
                        .cardRise(4)
                        .depthCard(cornerRadius = Dimens.CardCornerRadius)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        LoyverseMenuItemRow(
                            icon = Icons.Outlined.Info,
                            title = stringResource(R.string.about),
                            subtitle = stringResource(R.string.about_subtitle),
                            tileColors = listOf(PureWhite, BadgeBlueBg),
                            iconTint = PrimaryBlueLight,
                            onClick = { onNavigateToSection("about") }
                        )

                        if (SupportContact.isSupportConfigured()) {
                            HorizontalDivider(
                                thickness = Dimens.RowDividerWidth,
                                color = depthStyle.listDividerColor,
                                modifier = Modifier.padding(horizontal = Dimens.ReportCardInnerPadding)
                            )

                            LoyverseMenuItemRow(
                                icon = Icons.Outlined.HeadsetMic,
                                title = stringResource(R.string.nav_support),
                                subtitle = stringResource(R.string.support_subtitle),
                                tileColors = listOf(PureWhite, SectionRoseGradientEnd),
                                iconTint = ErrorRedLight,
                                onClick = { onNavigateToSection("support") }
                            )
                        }
                    }
                }
            }

            // =================================================================
            // 0. DAILY SHIFT REPORT GENERATOR
            // =================================================================
            "daily_shift_report", "shift_report", "shift" -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.SpacingLg)
                        .depthCard(cornerRadius = Dimens.CardCornerRadius)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        LoyverseMenuItemRow(
                            icon = Icons.Outlined.Assessment,
                            title = stringResource(R.string.daily_shift_report),
                            subtitle = stringResource(R.string.desc_create_shift_handover),
                            tileColors = listOf(PureWhite, BadgeBlueBg),
                            iconTint = PrimaryBlueLight,
                            trailing = {
                                Button(
                                    onClick = { onSwitchModule(AppModule.SHIFT_REPORT) },
                                    elevation = null,
                                    shape = RoundedCornerShape(Dimens.RadiusReportInput),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Transparent,
                                        disabledContainerColor = Color.Transparent,
                                        contentColor = PureWhite,
                                        disabledContentColor = OnSurfaceVariantLight
                                    ),
                                    contentPadding = PaddingValues(
                                        horizontal = Dimens.ReportCardInnerPadding,
                                        vertical = Dimens.SpacingXs
                                    ),
                                    modifier = Modifier.depthRaisedButton(
                                        cornerRadius = Dimens.RadiusReportInput,
                                        minHeight = Dimens.ButtonHeightCompact
                                    )
                                ) {
                                    Text(
                                        stringResource(R.string.launch_1),
                                        fontSize = DepthTypeTokens.PrimaryCtaSmallSize,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            },
                            onClick = { onSwitchModule(AppModule.SHIFT_REPORT) }
                        )

                        HorizontalDivider(
                            thickness = Dimens.RowDividerWidth,
                            color = depthStyle.listDividerColor,
                            modifier = Modifier.padding(horizontal = Dimens.ReportCardInnerPadding)
                        )

                        LoyverseMenuItemRow(
                            icon = Icons.Outlined.PictureAsPdf,
                            title = stringResource(R.string.title_shift_reports_pdf_archives),
                            subtitle = stringResource(R.string.desc_search_shift_reports),
                            tileColors = listOf(PureWhite, BadgeAmberBg),
                            iconTint = BadgeAmberAccent,
                            onClick = { onSwitchModule(AppModule.SHIFT_REPORT) }
                        )
                    }
                }
            }
            // =================================================================
            // EMPLOYEE / CASHIER MANAGEMENT
            // =================================================================
            "employees", "employee" -> {
                EmployeeManagementSection()
            }
            "cashiers", "cashier", "staff" -> {
                CashierManagementSection(
                    cashiers = cashiers,
                    reportViewModel = reportViewModel
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
                        .padding(horizontal = Dimens.SpacingLg, vertical = Dimens.SpacingLg)
                ) {
                    LojiaSectionHeader(stringResource(R.string.settings_section_pin_biometric))

                    // Card 1 — Quick Login
                    LojiaSettingsCard {
                        Box(
                            modifier = Modifier
                                .size(Dimens.SettingsIconTileSize)
                                .depthRaisedIconTile(
                                    gradientColors = listOf(PureWhite, BadgeBlueBg),
                                    cornerRadius = Dimens.RadiusSettingsIconTile
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Pin,
                                contentDescription = stringResource(R.string.cd_quick_login_pin),
                                tint = PrimaryBlueLight,
                                modifier = Modifier.size(Dimens.IconField)
                            )
                        }
                        Spacer(modifier = Modifier.width(Dimens.SettingsIconTitleGap))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.cd_quick_login_pin),
                                fontSize = DepthTypeTokens.ListRowTitleSize,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimaryReport
                            )
                            Spacer(modifier = Modifier.height(Dimens.SpacingXxs))
                            Text(
                                text = stringResource(R.string.settings_unlock_pos_pin_desc),
                                fontSize = DepthTypeTokens.ListRowSubtitleSize,
                                color = OnSurfaceVariantLight
                            )
                        }
                        Switch(
                            checked = quickLoginEnabled,
                            onCheckedChange = { enable ->
                                pendingSecurityAuthSubtitle = context.getString(R.string.quick_login_pin)
                                pendingSecurityAuthAction = {
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
                    LojiaSettingsCard(
                        onClick = {
                            pendingSecurityAuthSubtitle = context.getString(R.string.quick_login_pin)
                            pendingSecurityAuthAction = { showQuickPinDialog = true }
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(Dimens.SettingsIconTileSize)
                                .depthRaisedIconTile(
                                    gradientColors = listOf(PureWhite, BadgeAmberBg),
                                    cornerRadius = Dimens.RadiusSettingsIconTile
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Key,
                                contentDescription = stringResource(R.string.cd_change_pin),
                                tint = BadgeAmberAccent,
                                modifier = Modifier.size(Dimens.IconField)
                            )
                        }
                        Spacer(modifier = Modifier.width(Dimens.SettingsIconTitleGap))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.cd_change_pin),
                                fontSize = DepthTypeTokens.ListRowTitleSize,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimaryReport
                            )
                            Spacer(modifier = Modifier.height(Dimens.SpacingXxs))
                            Text(
                                text = stringResource(R.string.settings_tap_update_pin_desc),
                                fontSize = DepthTypeTokens.ListRowSubtitleSize,
                                color = OnSurfaceVariantLight
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = OnSurfaceVariantLight,
                            modifier = Modifier.size(Dimens.IconField)
                        )
                    }

                    LojiaSectionHeader(stringResource(R.string.settings_section_biometric))

                    // Card 3 — Biometric
                    LojiaSettingsCard {
                        Box(
                            modifier = Modifier
                                .size(Dimens.SettingsIconTileSize)
                                .depthRaisedIconTile(
                                    gradientColors = listOf(PureWhite, BadgeGreenBg),
                                    cornerRadius = Dimens.RadiusSettingsIconTile
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Fingerprint,
                                contentDescription = stringResource(R.string.cd_biometric_icon),
                                tint = BadgeGreenText,
                                modifier = Modifier.size(Dimens.IconField)
                            )
                        }
                        Spacer(modifier = Modifier.width(Dimens.SettingsIconTitleGap))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.biometric_login),
                                fontSize = DepthTypeTokens.ListRowTitleSize,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimaryReport
                            )
                            Spacer(modifier = Modifier.height(Dimens.SpacingXxs))
                            Text(
                                text = stringResource(R.string.settings_fingerprint_face_id),
                                fontSize = DepthTypeTokens.ListRowSubtitleSize,
                                color = OnSurfaceVariantLight
                            )
                        }
                        Switch(
                            checked = biometricEnabled,
                            onCheckedChange = { enable ->
                                pendingSecurityAuthSubtitle = context.getString(R.string.biometric_authentication)
                                pendingSecurityAuthAction = {
                                    if (enable) {
                                        // 3. First check that Quick Login is already enabled and a PIN exists (as fallback)
                                        if (!quickLoginEnabled || !preferencesRepository.hasPinConfigured()) {
                                            Toast.makeText(context, context.getString(R.string.settings_enable_pin_first_biometric), Toast.LENGTH_LONG).show()
                                            showQuickPinDialog = true
                                        } else {
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
                                        }
                                    } else {
                                        biometricEnabled = false
                                        preferencesRepository.setBiometricEnabled(false)
                                        persistProfile(bio = false)
                                        Toast.makeText(context, context.getString(R.string.biometric_login_disabled), Toast.LENGTH_SHORT).show()
                                    }
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
                    onRestrictedClick = onRestrictedClick,
                    onNavigateBack = { reportViewModel.selectReportSettingsMenu("root") }
                )
            }

            // =================================================================
            // 3.5 THERMAL PRINTER SETUP
            // =================================================================
            "printer", "thermal_printer" -> {
                com.lojia.shiftreport.printer.PrinterSetupScreen(
                    onClose = onNavigateBack,
                    showTopBar = false,
                    modifier = Modifier.fillMaxSize()
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
                    LojiaSectionHeader(stringResource(R.string.settings_section_app_info))

                    LojiaSettingRow(
                        icon = Icons.Outlined.Info,
                        title = stringResource(R.string.title_lojia_pos_shift_system),
                        subtitle = stringResource(R.string.desc_app_version_production),
                        tileColors = listOf(PureWhite, BadgeBlueBg),
                        iconTint = PrimaryBlueLight,
                        onClick = {}
                    )

                    LojiaSettingRow(
                        icon = Icons.Outlined.Verified,
                        title = stringResource(R.string.title_system_architecture),
                        subtitle = stringResource(R.string.desc_system_architecture),
                        tileColors = listOf(PureWhite, BadgeGreenBg),
                        iconTint = BadgeGreenText,
                        onClick = {}
                    )

                    LojiaSectionHeader(stringResource(R.string.settings_section_legal))

                    LojiaSettingRow(
                        icon = Icons.Outlined.Description,
                        title = stringResource(R.string.terms_conditions),
                        subtitle = stringResource(R.string.desc_review_eula_terms),
                        tileColors = listOf(PureWhite, SectionPurpleGradientEnd),
                        iconTint = OnTertiaryContainerLight,
                        trailing = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = OnSurfaceVariantLight,
                                modifier = Modifier.size(Dimens.IconField)
                            )
                        },
                        onClick = { showTermsModal = true }
                    )

                    LojiaSettingRow(
                        icon = Icons.Outlined.PrivacyTip,
                        title = stringResource(R.string.privacy_policy),
                        subtitle = stringResource(R.string.desc_gdpr_cloud_security),
                        tileColors = listOf(PureWhite, SectionRoseGradientEnd),
                        iconTint = ErrorRedLight,
                        trailing = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = OnSurfaceVariantLight,
                                modifier = Modifier.size(Dimens.IconField)
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
                if (SupportContact.isSupportConfigured()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = LojiaDimens.ScreenPadding, vertical = LojiaDimens.ScreenTopPadding)
                    ) {
                        LojiaSectionHeader(stringResource(R.string.settings_section_contact_support))

                        if (SupportContact.SUPPORT_PHONE.isNotBlank()) {
                            LojiaSettingRow(
                                icon = Icons.Outlined.HeadsetMic,
                                title = stringResource(R.string.title_24_7_helpline),
                                subtitle = SupportContact.SUPPORT_PHONE,
                                tileColors = listOf(PureWhite, BadgeBlueBg),
                                iconTint = PrimaryBlueLight,
                                trailing = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = OnSurfaceVariantLight,
                                        modifier = Modifier.size(Dimens.IconField)
                                    )
                                },
                                onClick = {
                                    val uri = SupportContact.buildDialUri(SupportContact.SUPPORT_PHONE)
                                    if (uri.isNotBlank()) {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse(uri)).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    }
                                }
                            )
                        }

                        if (SupportContact.SUPPORT_WHATSAPP.isNotBlank()) {
                            LojiaSettingRow(
                                icon = Icons.Outlined.Chat,
                                title = stringResource(R.string.title_whatsapp_support),
                                subtitle = stringResource(R.string.desc_whatsapp_support),
                                tileColors = listOf(PureWhite, BadgeGreenBg),
                                iconTint = BadgeGreenText,
                                trailing = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = OnSurfaceVariantLight,
                                        modifier = Modifier.size(Dimens.IconField)
                                    )
                                },
                                onClick = {
                                    val uri = SupportContact.buildWhatsAppUri(SupportContact.SUPPORT_WHATSAPP)
                                    if (uri.isNotBlank()) {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    }
                                }
                            )
                        }
                    }
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
        when (fieldName) {
            "Password" -> {
                ChangePasswordDialog(
                    currentPasswordHash = userProfile?.passwordHash.orEmpty(),
                    onDismiss = { editFieldDialog = null },
                    onPasswordChanged = { newPw ->
                        val hashed = SecurityUtils.hashSecret(newPw)
                        password = hashed
                        persistProfile(pass = hashed)
                        editFieldDialog = null
                        Toast.makeText(context, context.getString(R.string.password_changed_success), Toast.LENGTH_SHORT).show()
                    }
                )
            }
            "Email" -> {
                ChangeEmailDialog(
                    currentEmail = email,
                    onDismiss = { editFieldDialog = null },
                    onEmailChanged = { newMail ->
                        email = newMail
                        persistProfile(mail = newMail)
                        editFieldDialog = null
                        Toast.makeText(context, context.getString(R.string.toast_email_updated), Toast.LENGTH_SHORT).show()
                    }
                )
            }
            else -> {
                var tempVal by remember(fieldName) { mutableStateOf(fieldVal) }
                LojiaDialog(
                    onDismissRequest = { editFieldDialog = null },
                    size = com.lojia.shiftreport.ui.common.DialogSize.Medium
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .imePadding(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.edit_field_title, fieldName),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = OnSurfaceLight
                        )

                        LojiaTextField(
                            value = tempVal,
                            onValueChange = { tempVal = it },
                            label = { Text(fieldName) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        val confirmAction = @Composable {
                            Button(
                                onClick = {
                                    when (fieldName) {
                                        "Full name" -> { fullName = tempVal; persistProfile(fName = tempVal) }
                                        "Username" -> { username = tempVal; persistProfile(uName = tempVal) }
                                        "Number" -> { phone = tempVal; persistProfile(ph = tempVal) }
                                        "Address" -> { address = tempVal; persistProfile(addr = tempVal) }
                                        "Google Account" -> reportViewModel.googleAccount.value = tempVal
                                    }
                                    Toast.makeText(context, context.getString(R.string.field_updated_msg, fieldName), Toast.LENGTH_SHORT).show()
                                    editFieldDialog = null
                                },
                                shape = RoundedCornerShape(Dimens.RadiusMd),
                                elevation = null,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    contentColor = PureWhite,
                                    disabledContentColor = OnSurfaceVariantLight
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .depthRaisedButton(cornerRadius = Dimens.RadiusMd)
                            ) {
                                Text(stringResource(R.string.save_5), fontWeight = FontWeight.Bold)
                            }
                        }
                        val dismissAction = @Composable {
                            OutlinedButton(
                                onClick = { editFieldDialog = null },
                                shape = RoundedCornerShape(Dimens.RadiusMd),
                                border = null,
                                elevation = null,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = OnSurfaceVariantLight
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .depthSecondaryButton(cornerRadius = Dimens.RadiusMd)
                            ) {
                                Text(stringResource(R.string.cancel_18), fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.weight(1f)) { dismissAction() }
                            Box(modifier = Modifier.weight(1f)) { confirmAction() }
                        }
                    }
                }
            }
        }
    }

    // Google Maps Location Picker Modal
    if (showMapLocationPicker) {
        GoogleMapsLocationPickerModal(
            initialAddress = address,
            initialLat = mapLat,
            initialLng = mapLng,
            onLocationConfirmed = { newAddr, newLat, newLng ->
                showMapLocationPicker = false
                pendingSecurityAuthSubtitle = context.getString(R.string.confirm_location)
                pendingSecurityAuthAction = {
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
                }
            },
            onDismiss = { showMapLocationPicker = false }
        )
    }

    // Terms Modal
    if (showTermsModal) {
        LojiaDialog(
            onDismissRequest = { showTermsModal = false },
            size = com.lojia.shiftreport.ui.common.DialogSize.Small
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.terms_of_service),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceLight
                )
                Text(
                    text = stringResource(R.string.by_using_lojia_shift),
                    fontSize = 13.sp,
                    color = OnSurfaceVariantLight,
                    lineHeight = 18.sp
                )
                Button(
                    onClick = { showTermsModal = false },
                    shape = RoundedCornerShape(Dimens.RadiusMd),
                    elevation = null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        contentColor = PureWhite,
                        disabledContentColor = OnSurfaceVariantLight
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .depthRaisedButton(cornerRadius = Dimens.RadiusMd)
                ) {
                    Text(stringResource(R.string.close_7), fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Privacy Modal
    if (showPrivacyModal) {
        LojiaDialog(
            onDismissRequest = { showPrivacyModal = false },
            size = com.lojia.shiftreport.ui.common.DialogSize.Small
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.privacy_policy_2),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceLight
                )
                Text(
                    text = stringResource(R.string.your_financial_data_shift),
                    fontSize = 13.sp,
                    color = OnSurfaceVariantLight,
                    lineHeight = 18.sp
                )
                Button(
                    onClick = { showPrivacyModal = false },
                    shape = RoundedCornerShape(Dimens.RadiusMd),
                    elevation = null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        contentColor = PureWhite,
                        disabledContentColor = OnSurfaceVariantLight
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .depthRaisedButton(cornerRadius = Dimens.RadiusMd)
                ) {
                    Text(stringResource(R.string.close_7), fontWeight = FontWeight.Bold)
                }
            }
        }
    }


    if (showQuickPinDialog) {
        ChangeQuickPinDialog(
            preferencesRepository = preferencesRepository,
            onDismiss = {
                showQuickPinDialog = false
                if (!preferencesRepository.hasPinConfigured()) {
                    quickLoginEnabled = false
                    preferencesRepository.setQuickLoginEnabled(false)
                }
            },
            onPinChanged = {
                quickLoginEnabled = true
                showQuickPinDialog = false
                Toast.makeText(context, context.getString(R.string.settings_quick_pin_saved), Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (pendingSecurityAuthAction != null) {
        AdminAuthDialog(
            title = stringResource(R.string.admin_authorization_required),
            subtitle = pendingSecurityAuthSubtitle ?: stringResource(R.string.admin_reauth_default_title),
            onDismissRequest = {
                pendingSecurityAuthSubtitle = null
                pendingSecurityAuthAction = null
            },
            onAuthSuccess = {
                val action = pendingSecurityAuthAction
                pendingSecurityAuthSubtitle = null
                pendingSecurityAuthAction = null
                action?.invoke()
            }
        )
    }

}

