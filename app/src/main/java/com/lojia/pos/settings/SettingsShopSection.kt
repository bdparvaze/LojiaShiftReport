package com.lojia.pos.settings

import androidx.compose.foundation.layout.imePadding
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

import com.lojia.pos.util.AppLanguageManager
import com.lojia.pos.util.SecurityUtils


import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import kotlinx.coroutines.launch


import androidx.compose.animation.*


import androidx.compose.animation.core.*


import androidx.compose.foundation.BorderStroke


import androidx.compose.foundation.background


import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll


import androidx.compose.foundation.layout.*


import androidx.compose.foundation.lazy.LazyColumn


import androidx.compose.foundation.lazy.items


import androidx.compose.foundation.shape.RoundedCornerShape


import androidx.compose.foundation.shape.CircleShape


import androidx.compose.material.icons.Icons
import androidx.fragment.app.FragmentActivity


import androidx.compose.material.icons.automirrored.filled.*


import androidx.compose.material.icons.automirrored.outlined.*


import androidx.compose.material.icons.filled.*


import androidx.compose.material.icons.outlined.*


import androidx.compose.material3.*


import androidx.compose.runtime.*


import androidx.compose.ui.Alignment


import androidx.compose.ui.Modifier


import androidx.compose.ui.graphics.Color


import androidx.compose.ui.graphics.vector.ImageVector


import androidx.compose.ui.platform.LocalContext


import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp


import androidx.compose.ui.unit.sp


import java.text.SimpleDateFormat

import java.util.*


import androidx.compose.ui.res.stringResource


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsShopSection(
    reportViewModel: ReportViewModel,
    posViewModel: PosViewModel,
    language: AppLanguage,
    isAdmin: Boolean,
    onRestrictedClick: (action: () -> Unit) -> Unit,
    onSwitchModule: (AppModule) -> Unit
) {
    val context = LocalContext.current
    val userProfile by reportViewModel.userProfile.collectAsState()
    val businessProfile by reportViewModel.businessProfile.collectAsState()
    val receiptConfig by reportViewModel.receiptConfig.collectAsState()
    val activeShift by reportViewModel.activeShiftSession.collectAsState()
    val salesHistory by posViewModel.salesHistory.collectAsState()
    val products by posViewModel.products.collectAsState()
    val categories by posViewModel.categories.collectAsState()
    val modifiers by posViewModel.modifiers.collectAsState()
    val discounts by posViewModel.discounts.collectAsState()
        val rawCurrency = businessProfile?.currency ?: "SAR"
    val currency = if (rawCurrency == "SAR") stringResource(R.string.currency_unit) else rawCurrency

    val selectedShopMenuKey by reportViewModel.selectedShopSettingsMenu.collectAsState()
    val currentMenu = selectedShopMenuKey ?: "items"
    val cashiers by reportViewModel.cashiers.collectAsState()
    val currentCountry by posViewModel.currentCountry.collectAsState()
    var showAddCashierDialog by remember { mutableStateOf(false) }
    var cashierToDelete by remember { mutableStateOf<Cashier?>(null) }
    var langCountryTab by remember(currentMenu) { mutableIntStateOf(if (currentMenu == "country") 1 else 0) }
    var countrySearch by remember { mutableStateOf("") }

    // Profile state
    var fullName by remember(userProfile) { mutableStateOf(userProfile?.fullName ?: "Store Owner") }
    var username by remember(userProfile) { mutableStateOf(userProfile?.username ?: "") }
    var email by remember(userProfile) { mutableStateOf(userProfile?.email ?: "") }
    var password by remember(userProfile) { mutableStateOf("") }
    var securityQuestion by remember(userProfile) { mutableStateOf(userProfile?.securityQuestion ?: "") }
    var securityAnswer by remember(userProfile) { mutableStateOf(userProfile?.securityAnswer ?: "") }
    var phone by remember(userProfile) { mutableStateOf(userProfile?.phone ?: "") }
    var address by remember(userProfile) { mutableStateOf(userProfile?.address ?: "") }

    // Security state
    val preferencesRepository = remember(context) { com.lojia.pos.data.PreferencesRepository.getInstance(context) }
    var biometricEnabled by remember { mutableStateOf(preferencesRepository.isBiometricEnabled()) }
    var quickLoginEnabled by remember { mutableStateOf(preferencesRepository.isQuickLoginEnabled() && preferencesRepository.hasPinConfigured()) }
    var showQuickPinDialog by remember { mutableStateOf(false) }
    var pinValue by remember(userProfile) { mutableStateOf(userProfile?.pin.orEmpty()) }
    var showChangePinModal by remember { mutableStateOf(false) }

    // Active sub-dialog / sheet states
    var activeSubDialog by remember { mutableStateOf<String?>(null) }
    var editFieldDialog by remember { mutableStateOf<Pair<String, String>?>(null) }

    // Items dialog states
    var showAddProductDialog by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showAddModifierDialog by remember { mutableStateOf(false) }
    var showAddDiscountDialog by remember { mutableStateOf(false) }
    var newProdName by remember { mutableStateOf("") }
    var newProdPrice by remember { mutableStateOf("") }
    var newProdCategory by remember { mutableStateOf("Beverages") }
    var newCatName by remember { mutableStateOf("") }
    var newModName by remember { mutableStateOf("") }
    var newModPrice by remember { mutableStateOf("") }
    var newDiscName by remember { mutableStateOf("") }
    var newDiscVal by remember { mutableStateOf("") }
    var newDiscIsPercent by remember { mutableStateOf(true) }

    // Shift dialog states
    var payAmount by remember { mutableStateOf("") }
    var payReason by remember { mutableStateOf("") }
    var closeActualCount by remember { mutableStateOf("") }
    var closeNotes by remember { mutableStateOf("") }

    // Taxes & Settings states
    var printerName by remember { mutableStateOf("Thermal Bluetooth 80mm") }
    var autoCut by remember { mutableStateOf(true) }
    var customerDisplayOn by remember { mutableStateOf(true) }
    var customerGreeting by remember { mutableStateOf("Welcome to Lojia Store! 🌟") }
    var taxEnabledInput by remember(businessProfile) { mutableStateOf(businessProfile?.isTaxEnabled ?: true) }
    var taxInclusiveInput by remember(businessProfile) { mutableStateOf(businessProfile?.isTaxIncluded ?: true) }
    var vatRateInput by remember(businessProfile) { mutableStateOf((businessProfile?.vatRate ?: 15.0).toString()) }
    var vatNumberInput by remember(businessProfile) { mutableStateOf(businessProfile?.vatNumber ?: "") }
    var businessNameInput by remember(businessProfile) { mutableStateOf(businessProfile?.businessName ?: "Lojia Store") }

    // Sales History & Void states
    var salesSearchQuery by remember { mutableStateOf("") }
    var salesFilterTab by remember { mutableStateOf(0) } // 0: All, 1: Completed, 2: Voided
    var selectedSaleForDetail by remember { mutableStateOf<POSSale?>(null) }
    var showVoidReasonDialog by remember { mutableStateOf(false) }
    var voidReasonInput by remember { mutableStateOf("") }
    var saleToVoid by remember { mutableStateOf<POSSale?>(null) }
    var showReceiptPreviewForSale by remember { mutableStateOf<POSSale?>(null) }

    // Language Search
    var langSearch by remember { mutableStateOf("") }

    // Save profile helper
    fun persistProfile(
        fName: String = fullName,
        uName: String = username,
        mail: String = email,
        pass: String = password,
        secQ: String = securityQuestion,
        secA: String = securityAnswer,
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
                securityQuestion = secQ,
                securityAnswer = secA,
                phone = ph,
                address = addr,
                isBiometricEnabled = bio,
                pin = SecurityUtils.hashSecret(p)
            )
        )
    }

    val isRootMenu = currentMenu == "root" || currentMenu == "all" || currentMenu == "overview"
    val subMenuTitle = when (currentMenu) {
        "sales" -> "Sales"
        "receipts" -> "Receipts"
        "shift", "daily_shift_report" -> "Shift"
        "items" -> "Items"
        "cashiers", "cashier", "staff", "employees", "employee" -> "Cashier"
        "profile" -> "Profile"
        "security" -> "Security"
        "settings_sub", "settings" -> "Settings"
        "back_office" -> "Back office"
        "apps" -> "Apps"
        "country", "countries", "currency" -> "Country"
        "language" -> "Language"
        "about" -> "About"
        "support" -> "Support"
        "switch_module" -> "Switch Module"
        else -> currentMenu.replace('_', ' ').replaceFirstChar { it.uppercase() }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        when (currentMenu) {
            // =================================================================
            // 0. ROOT / ALL SETTINGS HUB (Overview of all Shop Settings)
            // =================================================================
            "root", "all", "overview" -> {
                // Header Banner
                Surface(
                    color = Color(0xFFF0FDF4),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(Color(0xFF10B981), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Storefront, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = businessProfile?.businessName ?: "Shop Management Hub",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF065F46)
                            )
                            AutoText(
                                id = R.string.select_any_settings_category,
                                fontSize = 12.sp,
                                color = Color(0xFF047857)
                            )
                        }
                    }
                }

                // ==========================================
                // 1. Sales
                // ==========================================
                LoyverseMenuItemRow(
                    icon = Icons.Outlined.TrendingUp,
                    title = "Sales",
                    subtitle = stringResource(R.string.desc_sales_history),
                    onClick = { reportViewModel.selectShopSettingsMenu("sales") }
                )

                // ==========================================
                // 2. Receipts
                // ==========================================
                LoyverseMenuItemRow(
                    icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                    title = "Receipts",
                    subtitle = stringResource(R.string.desc_receipt_settings),
                    onClick = { reportViewModel.selectShopSettingsMenu("receipts") }
                )

                // ==========================================
                // 3. Shift
                // ==========================================
                LoyverseMenuItemRow(
                    icon = Icons.Outlined.LockClock,
                    title = "Shift",
                    subtitle = stringResource(R.string.daily_shift_report_desc),
                    onClick = { reportViewModel.selectShopSettingsMenu("shift") }
                )

                // ==========================================
                // 4. Items
                // ==========================================
                LoyverseMenuItemRow(
                    icon = Icons.Outlined.FormatListBulleted,
                    title = "Items",
                    subtitle = stringResource(R.string.desc_products_categories, products.size, categories.size),
                    onClick = { reportViewModel.selectShopSettingsMenu("items") }
                )

                // ==========================================
                // 5. Cashier
                // ==========================================
                LoyverseMenuItemRow(
                    icon = Icons.Outlined.Badge,
                    title = "Cashier",
                    subtitle = stringResource(R.string.desc_manage_cashiers, cashiers.size),
                    onClick = { reportViewModel.selectShopSettingsMenu("cashiers") }
                )

                // ==========================================
                // 6. Profile
                // ==========================================
                LoyverseMenuItemRow(
                    icon = Icons.Outlined.Person,
                    title = "Profile",
                    subtitle = stringResource(R.string.desc_business_profile),
                    onClick = { reportViewModel.selectShopSettingsMenu("profile") }
                )

                // ==========================================
                // 7. Security
                // ==========================================
                LoyverseMenuItemRow(
                    icon = Icons.Outlined.Shield,
                    title = "Security",
                    subtitle = stringResource(R.string.desc_security_settings),
                    onClick = { reportViewModel.selectShopSettingsMenu("security") }
                )

                // ==========================================
                // 8. Settings
                // ==========================================
                LoyverseMenuItemRow(
                    icon = Icons.Outlined.Settings,
                    title = "Settings",
                    subtitle = stringResource(R.string.desc_hardware_settings),
                    onClick = { reportViewModel.selectShopSettingsMenu("settings_sub") }
                )

                // ==========================================
                // 9. Back office
                // ==========================================
                LoyverseMenuItemRow(
                    icon = Icons.Outlined.Business,
                    title = "Back office",
                    subtitle = stringResource(R.string.desc_cloud_back_office),
                    onClick = { reportViewModel.selectShopSettingsMenu("back_office") }
                )

                // ==========================================
                // 10. Apps
                // ==========================================
                LoyverseMenuItemRow(
                    icon = Icons.Outlined.Apps,
                    title = "Apps",
                    subtitle = stringResource(R.string.desc_kitchen_display_system),
                    onClick = { reportViewModel.selectShopSettingsMenu("apps") }
                )

                // ==========================================
                // 11. Language
                // ==========================================
                LoyverseMenuItemRow(
                    icon = Icons.Outlined.Language,
                    title = "Language",
                    subtitle = stringResource(R.string.desc_app_language, language.displayName),
                    onClick = { reportViewModel.selectShopSettingsMenu("language") }
                )

                // ==========================================
                // 12. Support
                // ==========================================
                LoyverseMenuItemRow(
                    icon = Icons.Outlined.HeadsetMic,
                    title = "Support",
                    subtitle = stringResource(R.string.desc_customer_support),
                    onClick = { reportViewModel.selectShopSettingsMenu("support") }
                )

                // ==========================================
                // 13. About
                // ==========================================
                LoyverseMenuItemRow(
                    icon = Icons.Outlined.Info,
                    title = "About",
                    subtitle = stringResource(R.string.desc_system_about),
                    onClick = { reportViewModel.selectShopSettingsMenu("about") }
                )

                // ==========================================
                // 14. Switch Module
                // ==========================================
                LoyverseMenuItemRow(
                    icon = Icons.Outlined.SwapHoriz,
                    title = "Switch Module",
                    subtitle = stringResource(R.string.desc_switch_module),
                    onClick = { reportViewModel.selectShopSettingsMenu("switch_module") }
                )
            }
            // =================================================================
            // 1. ITEMS (Items, Categories, Modifiers, Discounts)
            // =================================================================
            "items" -> {
                LoyverseMenuItemRow(
                    icon = Icons.Outlined.FormatListBulleted,
                    title = stringResource(R.string.menu_items),
                    onClick = { activeSubDialog = "items_list" }
                )
                LoyverseMenuItemRow(
                    icon = Icons.Outlined.Layers,
                    title = stringResource(R.string.menu_categories),
                    onClick = { activeSubDialog = "categories_list" }
                )
                LoyverseMenuItemRow(
                    icon = Icons.Outlined.FactCheck,
                    title = stringResource(R.string.menu_modifiers),
                    onClick = { activeSubDialog = "modifiers_list" }
                )
                LoyverseMenuItemRow(
                    icon = Icons.Outlined.LocalOffer,
                    title = stringResource(R.string.menu_discounts),
                    onClick = { activeSubDialog = "discounts_list" }
                )
            }

            // =================================================================
            // 2. SHIFT (DAILY SHIFT REPORT GENERATOR, CASH MANAGEMENT, CLOSE SHIFT)
            // =================================================================
            "shift", "daily_shift_report" -> {
                LoyverseMenuItemRow(
                    icon = Icons.Outlined.Assessment,
                    title = stringResource(R.string.menu_daily_shift_report),
                    subtitle = stringResource(R.string.desc_daily_shift_report),
                    trailing = {
                        Button(
                            onClick = { onSwitchModule(AppModule.SHIFT_REPORT) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00796B)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(stringResource(R.string.open_2), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    onClick = { onSwitchModule(AppModule.SHIFT_REPORT) }
                )
                LoyverseMenuItemRow(
                    icon = Icons.Outlined.LockClock,
                    title = stringResource(R.string.menu_close_shift),
                    onClick = { activeSubDialog = "close_shift" }
                )
            }

            // =================================================================
            // 3. SETTINGS (Printers, Customer displays, Taxes, General)
            // =================================================================
            "settings_sub", "settings" -> {
                LoyverseMenuItemRow(
                    icon = Icons.Outlined.Print,
                    title = stringResource(R.string.menu_printers),
                    onClick = { activeSubDialog = "printers" }
                )
                LoyverseMenuItemRow(
                    icon = Icons.Outlined.Tv,
                    title = stringResource(R.string.menu_customer_displays),
                    onClick = { activeSubDialog = "customer_displays" }
                )
                LoyverseMenuItemRow(
                    icon = Icons.Outlined.Percent,
                    title = stringResource(R.string.title_taxes),
                    onClick = { activeSubDialog = "taxes" }
                )
                LoyverseMenuItemRow(
                    icon = Icons.Outlined.Tune,
                    title = stringResource(R.string.title_general),
                    onClick = { activeSubDialog = "general" }
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
            // 4. PROFILE (Full name, Username, Email, Password, Number, Address, Business Logo)
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

                    // Clean List Card for Profile Detail Items (No ADMINISTRATOR / SECURITY header banners)
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
                                icon = Icons.Outlined.HelpOutline,
                                label = stringResource(R.string.title_security_question),
                                value = securityQuestion,
                                onClick = { editFieldDialog = "Security Question" to securityQuestion }
                            )
                            ProfileDetailItemRow(
                                icon = Icons.Outlined.VerifiedUser,
                                label = stringResource(R.string.title_security_answer),
                                value = "••••••••",
                                onClick = { editFieldDialog = "Security Answer" to securityAnswer }
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
            // 5. SECURITY (Biometric & MPIN)
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
            // 6. SALES
            // =================================================================
            "sales" -> {
                LoyverseMenuItemRow(
                    icon = Icons.Outlined.TrendingUp,
                    title = stringResource(R.string.all_sales_count_fmt, salesHistory.size),
                    subtitle = stringResource(R.string.desc_view_complete_sales),
                    onClick = { activeSubDialog = "sales_history" }
                )
                LoyverseMenuItemRow(
                    icon = Icons.Outlined.Summarize,
                    title = stringResource(R.string.sales_summary),
                    subtitle = stringResource(R.string.total_revenue_amount_fmt, "%.2f".format(salesHistory.sumOf { it.totalAmount }), currency),
                    onClick = { activeSubDialog = "sales_history" }
                )
            }

            // =================================================================
            // 7. RECEIPTS
            // =================================================================
            "receipts" -> {
                LoyverseMenuItemRow(
                    icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                    title = stringResource(R.string.title_header_text),
                    subtitle = receiptConfig?.customHeader ?: "Lojia Superstore & Cafe",
                    onClick = {
                        editFieldDialog = "Header Text" to (receiptConfig?.customHeader ?: "")
                    }
                )
                LoyverseMenuItemRow(
                    icon = Icons.Outlined.Notes,
                    title = stringResource(R.string.title_footer_text),
                    subtitle = receiptConfig?.customFooterText ?: "Thank you, visit again!",
                    onClick = {
                        editFieldDialog = "Footer Text" to (receiptConfig?.customFooterText ?: "")
                    }
                )
                LoyverseMenuItemRow(
                    icon = Icons.Outlined.Percent,
                    title = stringResource(R.string.title_show_tax_number),
                    trailing = {
                        Switch(
                            checked = receiptConfig?.showTaxNumber ?: true,
                            onCheckedChange = { enabled ->
                                val conf = receiptConfig ?: ShopReceiptConfig()
                                reportViewModel.saveReceiptConfig(conf.copy(showTaxNumber = enabled))
                            }
                        )
                    },
                    onClick = {}
                )
                LoyverseMenuItemRow(
                    icon = Icons.Outlined.Person,
                    title = stringResource(R.string.title_show_cashier_name),
                    trailing = {
                        Switch(
                            checked = receiptConfig?.showCashierName ?: true,
                            onCheckedChange = { enabled ->
                                val conf = receiptConfig ?: ShopReceiptConfig()
                                reportViewModel.saveReceiptConfig(conf.copy(showCashierName = enabled))
                            }
                        )
                    },
                    onClick = {}
                )
            }

            // =================================================================
            // 8. BACK OFFICE
            // =================================================================
            "back_office" -> {
                LoyverseMenuItemRow(
                    icon = Icons.Outlined.Business,
                    title = stringResource(R.string.title_back_office_portal),
                    subtitle = stringResource(R.string.desc_manage_back_office),
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://loyverse.com")).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    }
                )
                LoyverseMenuItemRow(
                    icon = Icons.Outlined.Sync,
                    title = stringResource(R.string.title_sync_data_back_office),
                    subtitle = stringResource(R.string.desc_sync_data_back_office),
                    onClick = {
                        Toast.makeText(context, context.getString(R.string.sync_complete_msg), Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // =================================================================
            // 9. APPS
            // =================================================================
            "apps" -> {
                LoyverseMenuItemRow(
                    icon = Icons.Outlined.Dashboard,
                    title = stringResource(R.string.title_loyverse_dashboard),
                    subtitle = stringResource(R.string.desc_loyverse_dashboard),
                    onClick = {
                        Toast.makeText(context, context.getString(R.string.opening_dashboard_msg), Toast.LENGTH_SHORT).show()
                    }
                )
                LoyverseMenuItemRow(
                    icon = Icons.Outlined.Kitchen,
                    title = stringResource(R.string.title_kitchen_display_system),
                    subtitle = stringResource(R.string.desc_kitchen_display_system),
                    onClick = {
                        Toast.makeText(context, context.getString(R.string.loyverse_kds_ready), Toast.LENGTH_SHORT).show()
                    }
                )
                LoyverseMenuItemRow(
                    icon = Icons.Outlined.Tv,
                    title = stringResource(R.string.title_customer_display_system),
                    subtitle = stringResource(R.string.desc_customer_display_system),
                    onClick = {
                        Toast.makeText(context, context.getString(R.string.loyverse_cds_connected), Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // =================================================================
            // 10. LANGUAGE / COUNTRY SELECTION
            // =================================================================
            "country", "countries", "currency", "language" -> {
                // Top Segmented Tab Row
                TabRow(
                    selectedTabIndex = langCountryTab,
                    containerColor = Color(0xFFF8FAFC),
                    contentColor = Color(0xFF059669),
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
                            accentColor = LoyverseGreenPrimary,
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
                            color = if (isSelected) Color(0xFFECFDF5) else Color.White,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    posViewModel.setCountry(c)
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
                                            color = if (isSelected) Color(0xFF059669) else Color(0xFF1E293B)
                                        )
                                        Text(
                                            text = "${c.currencyCode} (${c.currencySymbol}) • VAT: ${c.defaultVatRate.toInt()}%",
                                            fontSize = 12.sp,
                                            color = if (isSelected) Color(0xFF10B981) else Color(0xFF64748B)
                                        )
                                    }
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = stringResource(R.string.cd_selected),
                                        tint = Color(0xFF059669),
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
            // 11. ABOUT
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
                    onClick = { activeSubDialog = "general" }
                )
                LoyverseMenuItemRow(
                    icon = Icons.Outlined.PrivacyTip,
                    title = stringResource(R.string.privacy_policy),
                    subtitle = stringResource(R.string.desc_gdpr_cloud_security),
                    onClick = { activeSubDialog = "general" }
                )
            }

            // =================================================================
            // 12. SUPPORT
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
            // 12. SWITCH MODULE (Shift report, Shop)
            // =================================================================
            "switch_module" -> {
                LoyverseMenuItemRow(
                    icon = Icons.Outlined.Assessment,
                    title = stringResource(R.string.title_shift_report_module),
                    subtitle = stringResource(R.string.desc_switch_to_shift_report),
                    onClick = { onSwitchModule(AppModule.SHIFT_REPORT) }
                )
                LoyverseMenuItemRow(
                    icon = Icons.Outlined.Storefront,
                    title = stringResource(R.string.title_shop_module),
                    subtitle = stringResource(R.string.desc_currently_active_shop),
                    trailing = {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = stringResource(R.string.active), tint = Color(0xFF10B981))
                    },
                    onClick = { onSwitchModule(AppModule.SHOPPING) }
                )
            }

            else -> {
                LoyverseMenuItemRow(
                    icon = Icons.Outlined.FormatListBulleted,
                    title = stringResource(R.string.menu_items),
                    onClick = { activeSubDialog = "items_list" }
                )
                LoyverseMenuItemRow(
                    icon = Icons.Outlined.Layers,
                    title = stringResource(R.string.menu_categories),
                    onClick = { activeSubDialog = "categories_list" }
                )
                LoyverseMenuItemRow(
                    icon = Icons.Outlined.FactCheck,
                    title = stringResource(R.string.menu_modifiers),
                    onClick = { activeSubDialog = "modifiers_list" }
                )
                LoyverseMenuItemRow(
                    icon = Icons.Outlined.LocalOffer,
                    title = stringResource(R.string.menu_discounts),
                    onClick = { activeSubDialog = "discounts_list" }
                )
            }
        }
    }

    // =========================================================================
    // SUB-DIALOGS & MANAGERS
    // =========================================================================

    // 1. Items List Dialog
    if (activeSubDialog == "items_list") {
        AlertDialog(
            onDismissRequest = { activeSubDialog = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.items_count_title, products.size), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    IconButton(onClick = { showAddProductDialog = true }) {
                        Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.add_item), tint = Color(0xFF10B981))
                    }
                }
            },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(products) { prod ->
                        val catName = categories.find { it.id == prod.categoryId }?.name ?: "General"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(prod.name, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                Text(stringResource(R.string.in_stock_label, catName, prod.stockQuantity.toInt().toString()), fontSize = 12.sp, color = Color(0xFF757575))
                            }
                            Text(stringResource(R.string.msg_2f_s_21).format(prod.price, currency), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF10B981))
                        }
                        HorizontalDivider(color = Color(0xFFEEEEEE))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { activeSubDialog = null }) { Text(stringResource(R.string.close_7)) }
            }
        )
    }

    // 2. Categories List Dialog
    if (activeSubDialog == "categories_list") {
        AlertDialog(
            onDismissRequest = { activeSubDialog = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.categories_count_title, categories.size), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    IconButton(onClick = { showAddCategoryDialog = true }) {
                        Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.add_category), tint = Color(0xFF10B981))
                    }
                }
            },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(categories) { cat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.Folder, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(cat.name, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        }
                        HorizontalDivider(color = Color(0xFFEEEEEE))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { activeSubDialog = null }) { Text(stringResource(R.string.close_7)) }
            }
        )
    }

    // 3. Modifiers List Dialog
    if (activeSubDialog == "modifiers_list") {
        AlertDialog(
            onDismissRequest = { activeSubDialog = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.modifiers_count_title, modifiers.size), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    IconButton(onClick = { showAddModifierDialog = true }) {
                        Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.add_modifier), tint = Color(0xFF10B981))
                    }
                }
            },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(modifiers) { mod ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(mod.name, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                Text(stringResource(R.string.group_label, mod.optionGroup), fontSize = 12.sp, color = Color(0xFF757575))
                            }
                            Text(stringResource(R.string.msg_2f_s_18).format(mod.extraPrice, currency), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF10B981))
                        }
                        HorizontalDivider(color = Color(0xFFEEEEEE))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { activeSubDialog = null }) { Text(stringResource(R.string.close_7)) }
            }
        )
    }

    // 4. Discounts List Dialog
    if (activeSubDialog == "discounts_list") {
        AlertDialog(
            onDismissRequest = { activeSubDialog = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.discounts_count_title, discounts.size), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    IconButton(onClick = { showAddDiscountDialog = true }) {
                        Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.add_discount), tint = Color(0xFF10B981))
                    }
                }
            },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(discounts) { disc ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(disc.name, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            val discText = if (disc.isPercentage) "${disc.percentage}%" else "%.2f %s".format(disc.fixedAmount, currency)
                            Text(discText, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFE11D48))
                        }
                        HorizontalDivider(color = Color(0xFFEEEEEE))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { activeSubDialog = null }) { Text(stringResource(R.string.close_7)) }
            }
        )
    }

    // 5. Cash Management Dialog (Pay In / Pay Out)
    if (activeSubDialog == "cash_mgmt") {
        var isPayIn by remember { mutableStateOf(true) }
        AlertDialog(
            onDismissRequest = { activeSubDialog = null },
            title = { Text(stringResource(R.string.cash_management_1), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.verticalScroll(rememberScrollState()).imePadding()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = isPayIn,
                            onClick = { isPayIn = true },
                            label = { Text(stringResource(R.string.pay_in_add_cash)) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = !isPayIn,
                            onClick = { isPayIn = false },
                            label = { Text(stringResource(R.string.pay_out_remove_cash)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    LojiaTextField(
                        value = payAmount,
                        onValueChange = { payAmount = it },
                        label = { Text(stringResource(R.string.amount_with_currency, currency)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    LojiaTextField(
                        value = payReason,
                        onValueChange = { payReason = it },
                        label = { Text(stringResource(R.string.reason_note)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = payAmount.toDoubleOrNull() ?: 0.0
                        if (amount > 0) {
                            val type = if (isPayIn) "PAY_IN" else "PAY_OUT"
                            reportViewModel.addCashMovement(
                                type = type,
                                amount = amount,
                                reason = payReason,
                                cashierName = fullName
                            )
                            Toast.makeText(context, context.getString(R.string.movement_recorded_fmt, if (isPayIn) context.getString(R.string.cash_in) else context.getString(R.string.cash_out), amount, currency), Toast.LENGTH_SHORT).show()
                            payAmount = ""
                            payReason = ""
                            activeSubDialog = null
                        }
                    }
                ) {
                    Text(stringResource(R.string.submit))
                }
            },
            dismissButton = {
                TextButton(onClick = { activeSubDialog = null }) { Text(stringResource(R.string.cancel_18)) }
            }
        )
    }

    // 6. Close Shift Dialog
    if (activeSubDialog == "close_shift") {
        AlertDialog(
            onDismissRequest = { activeSubDialog = null },
            title = { Text(stringResource(R.string.close_shift_2), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.verticalScroll(rememberScrollState()).imePadding()) {
                    Text(stringResource(R.string.shift_status_active), fontWeight = FontWeight.Medium, color = Color(0xFF10B981))
                    Text(stringResource(R.string.current_shift_cash_2f).format(activeShift?.startingCash ?: 500.0, currency), fontSize = 13.sp)
                    LojiaTextField(
                        value = closeActualCount,
                        onValueChange = { closeActualCount = it },
                        label = { Text(stringResource(R.string.actual_cash_count_currency, currency)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    LojiaTextField(
                        value = closeNotes,
                        onValueChange = { closeNotes = it },
                        label = { Text(stringResource(R.string.closing_notes)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val actual = closeActualCount.toDoubleOrNull() ?: 0.0
                        val session = activeShift ?: ShiftSession(cashierName = fullName, startingCash = 500.0)
                        reportViewModel.closeShift(session, actual, closeNotes)
                        Toast.makeText(context, context.getString(R.string.shift_closed_successfully), Toast.LENGTH_SHORT).show()
                        activeSubDialog = null
                    }
                ) {
                    Text(stringResource(R.string.close_shift_2))
                }
            },
            dismissButton = {
                TextButton(onClick = { activeSubDialog = null }) { Text(stringResource(R.string.cancel_18)) }
            }
        )
    }

    // 7. Printers Dialog (Bluetooth / Network ESC/POS Thermal Printer Configuration)
    if (activeSubDialog == "printers") {
        val printerManager = remember(context) { com.lojia.pos.printer.BluetoothPrinterManager(context) }
        var connType by remember { mutableStateOf(printerManager.getPrinterConnectionType()) } // "bluetooth" or "network"
        var pairedPrinters by remember { mutableStateOf(printerManager.getPairedPrinters()) }
        var selectedAddress by remember { mutableStateOf(printerManager.getSavedPrinterAddress()) }
        var networkIp by remember { mutableStateOf(printerManager.getSavedNetworkIp()) }
        var networkPortStr by remember { mutableStateOf(printerManager.getSavedNetworkPort().toString()) }
        var selectedWidthMm by remember { mutableIntStateOf(printerManager.getSavedPaperWidthMm()) }
        var isPrintingTest by remember { mutableStateOf(false) }
        var isTestingConn by remember { mutableStateOf(false) }
        var connectionStatusMessage by remember { mutableStateOf<String?>(null) }
        var isConnectedSuccess by remember { mutableStateOf<Boolean?>(null) }

        val scope = rememberCoroutineScope()
        val bluetoothPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
            contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
        ) { _ ->
            pairedPrinters = printerManager.getPairedPrinters()
        }

        AlertDialog(
            onDismissRequest = { activeSubDialog = null },
            title = { Text(stringResource(R.string.printers_configuration), fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                ) {
                    Text(
                        "Configure ESC/POS Thermal Receipt Printer (Bluetooth or Network TCP)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Connection Type Selector
                    Text("Connection Type", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = connType == "bluetooth",
                            onClick = {
                                connType = "bluetooth"
                                printerManager.savePrinterConnectionType("bluetooth")
                                isConnectedSuccess = null
                                connectionStatusMessage = null
                            },
                            label = { Text("Bluetooth (Mobile)") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = connType == "network",
                            onClick = {
                                connType = "network"
                                printerManager.savePrinterConnectionType("network")
                                isConnectedSuccess = null
                                connectionStatusMessage = null
                            },
                            label = { Text("Wi-Fi / LAN (Counter)") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Connection Status Card
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = when (isConnectedSuccess) {
                                true -> Color(0xFFDCFCE7)
                                false -> Color(0xFFFEE2E2)
                                null -> Color(0xFFF1F5F9)
                            }
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = when (isConnectedSuccess) {
                                            true -> Icons.Outlined.CheckCircle
                                            false -> Icons.Outlined.ErrorOutline
                                            null -> Icons.Outlined.BluetoothSearching
                                        },
                                        contentDescription = null,
                                        tint = when (isConnectedSuccess) {
                                            true -> Color(0xFF16A34A)
                                            false -> Color(0xFFDC2626)
                                            null -> Color(0xFF475569)
                                        },
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (connType == "network" && networkIp.isBlank()) "No Network Printer IP"
                                        else if (connType == "bluetooth" && selectedAddress.isBlank()) "No Bluetooth Printer Selected"
                                        else if (isConnectedSuccess == true) "Connected"
                                        else if (isConnectedSuccess == false) "Connection Failed"
                                        else "Status: Configured",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = when (isConnectedSuccess) {
                                            true -> Color(0xFF15803D)
                                            false -> Color(0xFFB91C1C)
                                            null -> Color(0xFF334155)
                                        }
                                    )
                                }

                                val isConfigured = if (connType == "network") networkIp.isNotBlank() else selectedAddress.isNotBlank()
                                if (isConfigured) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        TextButton(
                                            onClick = {
                                                isTestingConn = true
                                                scope.launch {
                                                    val res = printerManager.testConnection()
                                                    isTestingConn = false
                                                    res.fold(
                                                        onSuccess = {
                                                            isConnectedSuccess = true
                                                            connectionStatusMessage = "Connected to printer"
                                                        },
                                                        onFailure = { err ->
                                                            isConnectedSuccess = false
                                                            connectionStatusMessage = "Connection test failed: ${err.message}"
                                                        }
                                                    )
                                                }
                                            },
                                            enabled = !isTestingConn
                                        ) {
                                            if (isTestingConn) {
                                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                            } else {
                                                Text("Test", fontSize = 12.sp)
                                            }
                                        }

                                        TextButton(
                                            onClick = {
                                                if (connType == "network") {
                                                    networkIp = ""
                                                    printerManager.saveNetworkPrinterConfig("", 9100, selectedWidthMm)
                                                } else {
                                                    selectedAddress = ""
                                                    printerManager.savePrinterConfig("", selectedWidthMm)
                                                }
                                                isConnectedSuccess = null
                                                connectionStatusMessage = "Printer disconnected"
                                                Toast.makeText(context, "Printer disconnected", Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Text("Disconnect", fontSize = 12.sp, color = Color(0xFFDC2626))
                                        }
                                    }
                                }
                            }

                            if (connectionStatusMessage != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    connectionStatusMessage ?: "",
                                    fontSize = 12.sp,
                                    color = if (isConnectedSuccess == true) Color(0xFF166534) else Color(0xFF991B1B)
                                )
                            }
                        }
                    }

                    // Paper Width Selection
                    Text("Paper Width", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedWidthMm == 58,
                            onClick = {
                                selectedWidthMm = 58
                                if (connType == "network") {
                                    printerManager.saveNetworkPrinterConfig(networkIp, networkPortStr.toIntOrNull() ?: 9100, 58)
                                } else if (selectedAddress.isNotBlank()) {
                                    printerManager.savePrinterConfig(selectedAddress, 58)
                                }
                            },
                            label = { Text("58 mm (Standard)") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedWidthMm == 80,
                            onClick = {
                                selectedWidthMm = 80
                                if (connType == "network") {
                                    printerManager.saveNetworkPrinterConfig(networkIp, networkPortStr.toIntOrNull() ?: 9100, 80)
                                } else if (selectedAddress.isNotBlank()) {
                                    printerManager.savePrinterConfig(selectedAddress, 80)
                                }
                            },
                            label = { Text("80 mm (Wide)") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    HorizontalDivider(color = Color(0xFFE2E8F0))

                    // Dynamic Section Based on Connection Type
                    if (connType == "network") {
                        Text("Network Printer Setup (TCP / IP)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(
                            "Ensure printer and Android POS device are connected to the same Wi-Fi or local network. Standard ESC/POS network port is 9100. Static IP recommended.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = networkIp,
                            onValueChange = { networkIp = it.trim() },
                            label = { Text("Printer IP Address") },
                            placeholder = { Text("e.g. 192.168.1.100") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = networkPortStr,
                            onValueChange = { networkPortStr = it.filter { c -> c.isDigit() } },
                            label = { Text("Port (Default: 9100)") },
                            placeholder = { Text("9100") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                val portInt = networkPortStr.toIntOrNull() ?: 9100
                                printerManager.saveNetworkPrinterConfig(networkIp, portInt, selectedWidthMm)
                                isTestingConn = true
                                scope.launch {
                                    val res = printerManager.testNetworkConnection(networkIp, portInt)
                                    isTestingConn = false
                                    res.fold(
                                        onSuccess = {
                                            isConnectedSuccess = true
                                            connectionStatusMessage = "Successfully connected to $networkIp:$portInt"
                                            Toast.makeText(context, "Connected to network printer", Toast.LENGTH_SHORT).show()
                                        },
                                        onFailure = { err ->
                                            isConnectedSuccess = false
                                            connectionStatusMessage = "Network printer offline: ${err.message}"
                                        }
                                    )
                                }
                            },
                            enabled = networkIp.isNotBlank() && !isTestingConn,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isTestingConn) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text("Save & Connect Network Printer")
                        }
                    } else {
                        // Bluetooth Section
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Paired Bluetooth Printers", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            IconButton(onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    bluetoothPermissionLauncher.launch(
                                        arrayOf(
                                            android.Manifest.permission.BLUETOOTH_CONNECT,
                                            android.Manifest.permission.BLUETOOTH_SCAN
                                        )
                                    )
                                } else {
                                    pairedPrinters = printerManager.getPairedPrinters()
                                }
                            }) {
                                Icon(Icons.Outlined.Refresh, contentDescription = "Refresh Printers")
                            }
                        }

                        if (pairedPrinters.isEmpty()) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        "No paired Bluetooth printers found.",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF92400E)
                                    )
                                    Text(
                                        "Please pair your thermal printer in Android Bluetooth Settings first, then tap Refresh.",
                                        fontSize = 12.sp,
                                        color = Color(0xFFB45309)
                                    )
                                }
                            }
                        } else {
                            pairedPrinters.forEach { device ->
                                val isSelected = device.address == selectedAddress
                                Card(
                                    onClick = {
                                        selectedAddress = device.address
                                        printerManager.savePrinterConfig(device.address, selectedWidthMm)
                                        isConnectedSuccess = null
                                        connectionStatusMessage = "Selected ${device.name}"
                                        Toast.makeText(context, "Selected ${device.name}", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) Color(0xFFDCFCE7) else Color(0xFFF8FAFC)
                                    ),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) Color(0xFF16A34A) else Color(0xFFCBD5E1)
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(device.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text(device.address, fontSize = 12.sp, color = Color.Gray)
                                        }

                                        Button(
                                            onClick = {
                                                if (isSelected) {
                                                    selectedAddress = ""
                                                    printerManager.savePrinterConfig("", selectedWidthMm)
                                                    isConnectedSuccess = null
                                                    connectionStatusMessage = "Disconnected ${device.name}"
                                                    Toast.makeText(context, "Disconnected ${device.name}", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    selectedAddress = device.address
                                                    printerManager.savePrinterConfig(device.address, selectedWidthMm)
                                                    isTestingConn = true
                                                    scope.launch {
                                                        val res = printerManager.testConnection(device.address)
                                                        isTestingConn = false
                                                        res.fold(
                                                            onSuccess = {
                                                                isConnectedSuccess = true
                                                                connectionStatusMessage = "Connected to ${device.name}"
                                                                Toast.makeText(context, "Connected to ${device.name}", Toast.LENGTH_SHORT).show()
                                                            },
                                                            onFailure = { err ->
                                                                isConnectedSuccess = false
                                                                connectionStatusMessage = "Connection failed: ${err.message}"
                                                            }
                                                        )
                                                    }
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isSelected) Color(0xFFDC2626) else Color(0xFF0284C7)
                                            ),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(if (isSelected) "Disconnect" else "Connect", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Print Test Receipt Button
                    val canPrintTest = if (connType == "network") networkIp.isNotBlank() else selectedAddress.isNotBlank()
                    Button(
                        enabled = canPrintTest && !isPrintingTest,
                        onClick = {
                            isPrintingTest = true
                            scope.launch {
                                val testReceipt = printerManager.buildReceiptText(
                                    businessName = businessProfile?.businessName ?: "Lojia Store",
                                    businessAddress = businessProfile?.address ?: "123 Main St",
                                    businessPhone = businessProfile?.phone ?: "+123456789",
                                    vatNumber = businessProfile?.vatNumber ?: "",
                                    customHeader = receiptConfig?.customHeader ?: "",
                                    customFooterText = receiptConfig?.customFooterText ?: "",
                                    showTaxNumber = receiptConfig?.showTaxNumber ?: true,
                                    showCashierName = receiptConfig?.showCashierName ?: true,
                                    receiptId = "TEST-001",
                                    dateTimeStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()),
                                    cashierName = "Test Cashier",
                                    customerName = "Sample Customer",
                                    items = listOf(
                                        Pair("Thermal Printer Test Item", Pair(1.0, 10.00)),
                                        Pair("Sample Product Demo", Pair(2.0, 15.50))
                                    ),
                                    subtotal = 25.50,
                                    discount = 0.0,
                                    tax = 3.83,
                                    grandTotal = 29.33,
                                    paymentMethod = if (connType == "network") "TCP NETWORK PRINT" else "BLUETOOTH PRINT",
                                    currencySymbol = businessProfile?.currency ?: "$"
                                )

                                val result = printerManager.printFormattedText(testReceipt)
                                isPrintingTest = false
                                result.fold(
                                    onSuccess = {
                                        isConnectedSuccess = true
                                        Toast.makeText(context, "Test receipt printed successfully!", Toast.LENGTH_SHORT).show()
                                    },
                                    onFailure = { err ->
                                        isConnectedSuccess = false
                                        connectionStatusMessage = "Printer error: ${err.message}"
                                        Toast.makeText(context, "Printer error: ${err.message}", Toast.LENGTH_LONG).show()
                                    }
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isPrintingTest) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Printing Test Receipt...")
                        } else {
                            Icon(Icons.Outlined.Print, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.print_test_receipt))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { activeSubDialog = null }) {
                    Text(stringResource(R.string.done_1))
                }
            }
        )
    }

    // 8. Customer Displays Dialog
    if (activeSubDialog == "customer_displays") {
        AlertDialog(
            onDismissRequest = { activeSubDialog = null },
            title = { Text(stringResource(R.string.customer_displays_1), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.verticalScroll(rememberScrollState()).imePadding()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.enable_customer_display), fontSize = 14.sp)
                        Switch(checked = customerDisplayOn, onCheckedChange = { customerDisplayOn = it })
                    }
                    LojiaTextField(
                        value = customerGreeting,
                        onValueChange = { customerGreeting = it },
                        label = { Text(stringResource(R.string.welcome_greeting_message)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { activeSubDialog = null }) { Text(stringResource(R.string.save_5)) }
            }
        )
    }

    // 9. Taxes Dialog
    if (activeSubDialog == "taxes") {
        AlertDialog(
            onDismissRequest = { activeSubDialog = null },
            title = { Text(stringResource(R.string.taxes_vat), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.verticalScroll(rememberScrollState()).imePadding()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.taxes_vat), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Enable tax calculation on POS sales", fontSize = 12.sp, color = TextSecondaryLight)
                        }
                        Switch(
                            checked = taxEnabledInput,
                            onCheckedChange = { taxEnabledInput = it }
                        )
                    }

                    if (taxEnabledInput) {
                        LojiaTextField(
                            value = vatRateInput,
                            onValueChange = { vatRateInput = it },
                            label = { Text(stringResource(R.string.vat_rate)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text("Tax Pricing Mode", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = taxInclusiveInput,
                                onClick = { taxInclusiveInput = true },
                                label = { Text("Tax Included") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = !taxInclusiveInput,
                                onClick = { taxInclusiveInput = false },
                                label = { Text("Tax Added (Exclusive)") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        LojiaTextField(
                            value = vatNumberInput,
                            onValueChange = { vatNumberInput = it },
                            label = { Text(stringResource(R.string.tax_vat_registration_number)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val rate = vatRateInput.toDoubleOrNull() ?: 0.0
                        val current = businessProfile ?: BusinessProfile()
                        reportViewModel.saveBusinessProfile(
                            current.copy(
                                vatRate = rate,
                                vatNumber = vatNumberInput.trim(),
                                isTaxEnabled = taxEnabledInput,
                                isTaxIncluded = taxInclusiveInput
                            )
                        )
                        Toast.makeText(context, context.getString(R.string.tax_settings_saved), Toast.LENGTH_SHORT).show()
                        activeSubDialog = null
                    }
                ) {
                    Text(stringResource(R.string.save_5))
                }
            },
            dismissButton = {
                TextButton(onClick = { activeSubDialog = null }) { Text(stringResource(R.string.cancel_18)) }
            }
        )
    }

    // 9B. Sales History & Void Dialog
    if (activeSubDialog == "sales_history") {
        val filteredSales = remember(salesHistory, salesSearchQuery, salesFilterTab) {
            salesHistory.filter { sale ->
                val matchesQuery = salesSearchQuery.isBlank() ||
                        sale.invoiceNumber.contains(salesSearchQuery, ignoreCase = true) ||
                        sale.cashierName.contains(salesSearchQuery, ignoreCase = true) ||
                        sale.customerName.contains(salesSearchQuery, ignoreCase = true)
                val matchesTab = when (salesFilterTab) {
                    1 -> !sale.isVoided
                    2 -> sale.isVoided
                    else -> true
                }
                matchesQuery && matchesTab
            }
        }

        AlertDialog(
            onDismissRequest = { activeSubDialog = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Sales History & Refunds", fontWeight = FontWeight.Bold)
                    Text(
                        "${filteredSales.size} sales",
                        fontSize = 12.sp,
                        color = TextSecondaryLight
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 500.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LojiaTextField(
                        value = salesSearchQuery,
                        onValueChange = { salesSearchQuery = it },
                        placeholder = { Text("Search invoice #, cashier...") },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                        trailingIcon = {
                            if (salesSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { salesSearchQuery = "" }) {
                                    Icon(Icons.Outlined.Clear, contentDescription = null)
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = salesFilterTab == 0,
                            onClick = { salesFilterTab = 0 },
                            label = { Text("All (${salesHistory.size})", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = salesFilterTab == 1,
                            onClick = { salesFilterTab = 1 },
                            label = { Text("Completed (${salesHistory.count { !it.isVoided }})", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = salesFilterTab == 2,
                            onClick = { salesFilterTab = 2 },
                            label = { Text("Voided (${salesHistory.count { it.isVoided }})", fontSize = 11.sp) }
                        )
                    }

                    if (filteredSales.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No transactions found", color = TextSecondaryLight)
                        }
                    } else {
                        val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredSales, key = { it.id }) { sale ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (sale.isVoided) Color(0xFFFEF2F2) else Color(0xFFF8FAFC),
                                    border = BorderStroke(
                                        1.dp,
                                        if (sale.isVoided) Color(0xFFFCA5A5) else Color(0xFFE2E8F0)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedSaleForDetail = sale }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    sale.invoiceNumber,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = if (sale.isVoided) Color(0xFF991B1B) else Color(0xFF0F172A)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                if (sale.isVoided) {
                                                    Surface(
                                                        color = Color(0xFFEF4444),
                                                        shape = RoundedCornerShape(4.dp)
                                                    ) {
                                                        Text(
                                                            "VOIDED",
                                                            color = Color.White,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                "${dateFormatter.format(Date(sale.timestamp))} • ${sale.cashierName} • ${sale.paymentMethod}",
                                                fontSize = 11.sp,
                                                color = TextSecondaryLight
                                            )
                                            if (sale.isVoided && sale.voidReason.isNotBlank()) {
                                                Text(
                                                    "Reason: ${sale.voidReason}",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFFDC2626),
                                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                                )
                                            }
                                        }

                                        Text(
                                            MoneyFormat.format(sale.totalAmount, currency),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 14.sp,
                                            color = if (sale.isVoided) Color(0xFF991B1B) else LoyverseGreenDark
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { activeSubDialog = null }) { Text("Close") }
            }
        )
    }

    // Detail & Void Modal for a selected sale
    if (selectedSaleForDetail != null) {
        val sale = selectedSaleForDetail!!
        val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }

        AlertDialog(
            onDismissRequest = { selectedSaleForDetail = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(sale.invoiceNumber, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    if (sale.isVoided) {
                        Surface(
                            color = Color(0xFFEF4444),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "VOIDED",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (sale.isVoided) {
                        Surface(
                            color = Color(0xFFFEE2E2),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, Color(0xFFEF4444)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Transaction Voided & Refunded", fontWeight = FontWeight.Bold, color = Color(0xFF991B1B), fontSize = 12.sp)
                                if (sale.voidReason.isNotBlank()) {
                                    Text("Reason: ${sale.voidReason}", color = Color(0xFFB91C1C), fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    Text("Date: ${dateFormatter.format(Date(sale.timestamp))}", fontSize = 12.sp)
                    Text("Cashier: ${sale.cashierName}", fontSize = 12.sp)
                    if (sale.customerName.isNotBlank()) {
                        Text("Customer: ${sale.customerName}", fontSize = 12.sp)
                    }
                    Text("Payment Method: ${sale.paymentMethod}", fontSize = 12.sp)

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Subtotal:", fontSize = 13.sp, color = TextSecondaryLight)
                        Text(MoneyFormat.format(sale.subtotal, currency), fontSize = 13.sp)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Tax / VAT:", fontSize = 13.sp, color = TextSecondaryLight)
                        Text(MoneyFormat.format(sale.vatAmount, currency), fontSize = 13.sp)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Amount:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(MoneyFormat.format(sale.totalAmount, currency), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (sale.isVoided) Color(0xFFDC2626) else LoyverseGreenDark)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = {
                            showReceiptPreviewForSale = sale
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.Receipt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View / Print Receipt")
                    }

                    if (!sale.isVoided) {
                        OutlinedButton(
                            onClick = {
                                onRestrictedClick {
                                    saleToVoid = sale
                                    voidReasonInput = ""
                                    showVoidReasonDialog = true
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                            border = BorderStroke(1.dp, Color(0xFFDC2626)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Outlined.Cancel, contentDescription = null, tint = Color(0xFFDC2626))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Void & Refund Sale")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedSaleForDetail = null }) { Text("Done") }
            }
        )
    }

    // Void Reason Prompt Modal
    if (showVoidReasonDialog && saleToVoid != null) {
        val targetSale = saleToVoid!!
        AlertDialog(
            onDismissRequest = {
                showVoidReasonDialog = false
                saleToVoid = null
            },
            title = { Text("Void & Refund Transaction", fontWeight = FontWeight.Bold, color = Color(0xFFDC2626)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.verticalScroll(rememberScrollState()).imePadding()) {
                    Text(
                        "Are you sure you want to void invoice ${targetSale.invoiceNumber} (${MoneyFormat.format(targetSale.totalAmount, currency)})? Inventory will be restored atomically.",
                        fontSize = 13.sp
                    )

                    Text("Select or enter reason:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilterChip(
                            selected = voidReasonInput == "Customer Return",
                            onClick = { voidReasonInput = "Customer Return" },
                            label = { Text("Return", fontSize = 10.sp) }
                        )
                        FilterChip(
                            selected = voidReasonInput == "Defective Item",
                            onClick = { voidReasonInput = "Defective Item" },
                            label = { Text("Defective", fontSize = 10.sp) }
                        )
                        FilterChip(
                            selected = voidReasonInput == "Cashier Mistake",
                            onClick = { voidReasonInput = "Cashier Mistake" },
                            label = { Text("Mistake", fontSize = 10.sp) }
                        )
                    }

                    LojiaTextField(
                        value = voidReasonInput,
                        onValueChange = { voidReasonInput = it },
                        label = { Text("Reason for void") },
                        placeholder = { Text("e.g. Customer return, wrong item") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val reason = voidReasonInput.ifBlank { "Voided by Admin" }
                        posViewModel.voidSale(
                            saleId = targetSale.id,
                            reason = reason,
                            performedBy = fullName,
                            isAdminUser = isAdmin,
                            onComplete = { success ->
                                if (success) {
                                    showVoidReasonDialog = false
                                    saleToVoid = null
                                    selectedSaleForDetail = null
                                }
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Confirm Void")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showVoidReasonDialog = false
                    saleToVoid = null
                }) {
                    Text(stringResource(R.string.cancel_18))
                }
            }
        )
    }

    // Receipt Preview Dialog from Sales History
    if (showReceiptPreviewForSale != null) {
        SaleReceiptPreviewDialog(
            sale = showReceiptPreviewForSale!!,
            businessProfile = businessProfile,
            language = language,
            onDismiss = { showReceiptPreviewForSale = null }
        )
    }

    // 10. General Settings Dialog
    if (activeSubDialog == "general") {
        AlertDialog(
            onDismissRequest = { activeSubDialog = null },
            title = { Text(stringResource(R.string.general_store_settings), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.verticalScroll(rememberScrollState()).imePadding()) {
                    LojiaTextField(
                        value = businessNameInput,
                        onValueChange = { businessNameInput = it },
                        label = { Text(stringResource(R.string.store_business_name)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(stringResource(R.string.currency_label, currency), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val current = businessProfile ?: BusinessProfile()
                        reportViewModel.saveBusinessProfile(current.copy(businessName = businessNameInput))
                        Toast.makeText(context, context.getString(R.string.store_settings_saved), Toast.LENGTH_SHORT).show()
                        activeSubDialog = null
                    }
                ) {
                    Text(stringResource(R.string.save_5))
                }
            },
            dismissButton = {
                TextButton(onClick = { activeSubDialog = null }) { Text(stringResource(R.string.cancel_18)) }
            }
        )
    }

    // 11. Single field edit dialog (Profile/Receipts)
    if (editFieldDialog != null) {
        val (fieldName, fieldVal) = editFieldDialog!!
        var tempVal by remember(fieldName) { mutableStateOf(fieldVal) }
        AlertDialog(
            onDismissRequest = { editFieldDialog = null },
            title = { Text(stringResource(R.string.edit_field_title, fieldName), fontWeight = FontWeight.Bold) },
            text = {
                LojiaTextField(
                    value = tempVal,
                    onValueChange = { tempVal = it },
                    label = { Text(fieldName) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        when (fieldName) {
                            "Full name" -> { fullName = tempVal; persistProfile(fName = tempVal) }
                            "Username" -> { username = tempVal; persistProfile(uName = tempVal) }
                            "Email" -> { email = tempVal; persistProfile(mail = tempVal) }
                            "Password" -> { password = tempVal; persistProfile(pass = tempVal) }
                            "Security Question" -> { securityQuestion = tempVal; persistProfile(secQ = tempVal) }
                            "Security Answer" -> { securityAnswer = tempVal; persistProfile(secA = tempVal) }
                            "Number" -> { phone = tempVal; persistProfile(ph = tempVal) }
                            "Address" -> { address = tempVal; persistProfile(addr = tempVal) }
                            "Header Text" -> {
                                val conf = receiptConfig ?: ShopReceiptConfig()
                                reportViewModel.saveReceiptConfig(conf.copy(customHeader = tempVal))
                            }
                            "Footer Text" -> {
                                val conf = receiptConfig ?: ShopReceiptConfig()
                                reportViewModel.saveReceiptConfig(conf.copy(customFooterText = tempVal))
                            }
                        }
                        Toast.makeText(context, context.getString(R.string.field_updated_msg, fieldName), Toast.LENGTH_SHORT).show()
                        editFieldDialog = null
                    }
                ) {
                    Text(stringResource(R.string.save_5))
                }
            },
            dismissButton = {
                TextButton(onClick = { editFieldDialog = null }) { Text(stringResource(R.string.cancel_18)) }
            }
        )
    }

    // Add Item Dialog
    if (showAddProductDialog) {
        AlertDialog(
            onDismissRequest = { showAddProductDialog = false },
            title = { Text(stringResource(R.string.add_new_product), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.verticalScroll(rememberScrollState()).imePadding()) {
                    LojiaTextField(value = newProdName, onValueChange = { newProdName = it }, label = { Text(stringResource(R.string.product_name_1)) }, modifier = Modifier.fillMaxWidth())
                    LojiaTextField(value = newProdPrice, onValueChange = { newProdPrice = it }, label = { Text(stringResource(R.string.price_with_currency, currency)) }, modifier = Modifier.fillMaxWidth())
                    LojiaTextField(value = newProdCategory, onValueChange = { newProdCategory = it }, label = { Text(stringResource(R.string.category)) }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    val price = newProdPrice.toDoubleOrNull() ?: 0.0
                    val catId = categories.firstOrNull()?.id ?: 1
                    if (newProdName.isNotBlank()) {
                        posViewModel.addOrUpdateProduct(
                            POSProduct(
                                name = newProdName,
                                categoryId = catId,
                                price = price,
                                stockQuantity = 100.0
                            )
                        )
                        newProdName = ""
                        newProdPrice = ""
                        showAddProductDialog = false
                    }
                }) { Text(stringResource(R.string.add_4)) }
            },
            dismissButton = { TextButton(onClick = { showAddProductDialog = false }) { Text(stringResource(R.string.cancel_18)) } }
        )
    }

    // Add Category Dialog
    if (showAddCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text(stringResource(R.string.add_new_category), fontWeight = FontWeight.Bold) },
            text = {
                LojiaTextField(value = newCatName, onValueChange = { newCatName = it }, label = { Text(stringResource(R.string.category_name)) }, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                Button(onClick = {
                    if (newCatName.isNotBlank()) {
                        posViewModel.addCategory(newCatName, "icon_folder")
                        newCatName = ""
                        showAddCategoryDialog = false
                    }
                }) { Text(stringResource(R.string.add_4)) }
            },
            dismissButton = { TextButton(onClick = { showAddCategoryDialog = false }) { Text(stringResource(R.string.cancel_18)) } }
        )
    }

    // Add Modifier Dialog
    if (showAddModifierDialog) {
        AlertDialog(
            onDismissRequest = { showAddModifierDialog = false },
            title = { Text(stringResource(R.string.add_modifier_1), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.verticalScroll(rememberScrollState()).imePadding()) {
                    LojiaTextField(value = newModName, onValueChange = { newModName = it }, label = { Text(stringResource(R.string.modifier_name)) }, modifier = Modifier.fillMaxWidth())
                    LojiaTextField(value = newModPrice, onValueChange = { newModPrice = it }, label = { Text(stringResource(R.string.extra_price_with_currency, currency)) }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    val extra = newModPrice.toDoubleOrNull() ?: 0.0
                    if (newModName.isNotBlank()) {
                        val optionsStr = context.getString(R.string.title_options)
                        posViewModel.addModifier(newModName, optionsStr, extra)
                        newModName = ""
                        newModPrice = ""
                        showAddModifierDialog = false
                    }
                }) { Text(stringResource(R.string.add_4)) }
            },
            dismissButton = { TextButton(onClick = { showAddModifierDialog = false }) { Text(stringResource(R.string.cancel_18)) } }
        )
    }

    // Add Discount Dialog
    if (showAddDiscountDialog) {
        AlertDialog(
            onDismissRequest = { showAddDiscountDialog = false },
            title = { Text(stringResource(R.string.add_discount_1), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.verticalScroll(rememberScrollState()).imePadding()) {
                    LojiaTextField(value = newDiscName, onValueChange = { newDiscName = it }, label = { Text(stringResource(R.string.discount_name)) }, modifier = Modifier.fillMaxWidth())
                    LojiaTextField(value = newDiscVal, onValueChange = { newDiscVal = it }, label = { Text(stringResource(R.string.value_eg_10)) }, modifier = Modifier.fillMaxWidth())
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = newDiscIsPercent, onCheckedChange = { newDiscIsPercent = it })
                        Text(stringResource(R.string.percentage_discount))
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val v = newDiscVal.toDoubleOrNull() ?: 0.0
                    if (newDiscName.isNotBlank()) {
                        posViewModel.addDiscount(
                            name = newDiscName,
                            percentage = if (newDiscIsPercent) v else 0.0,
                            fixedAmount = if (!newDiscIsPercent) v else 0.0,
                            isPercentage = newDiscIsPercent,
                            code = newDiscName.take(4).uppercase()
                        )
                        newDiscName = ""
                        newDiscVal = ""
                        showAddDiscountDialog = false
                    }
                }) { Text(stringResource(R.string.add_4)) }
            },
            dismissButton = { TextButton(onClick = { showAddDiscountDialog = false }) { Text(stringResource(R.string.cancel_18)) } }
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
        SecureDeleteModal(
            title = stringResource(R.string.delete_cashier),
            itemDescription = stringResource(R.string.confirm_delete_cashier_prompt, target.name, target.role),
            userProfile = userProfile,
            onDismiss = { cashierToDelete = null },
            onConfirmDelete = {
                reportViewModel.deleteCashier(target)
                Toast.makeText(context, context.getString(R.string.user_deleted_msg, target.name), Toast.LENGTH_SHORT).show()
                cashierToDelete = null
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
}


