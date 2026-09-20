package com.lojia.pos

import com.lojia.pos.R
import com.lojia.pos.auth.*
import com.lojia.pos.data.*
import com.lojia.pos.pos.*
import com.lojia.pos.report.*
import com.lojia.pos.settings.*
import com.lojia.pos.ui.common.*
import com.lojia.pos.ui.theme.*
import com.lojia.pos.util.*


import android.os.Bundle


import androidx.activity.compose.BackHandler


import androidx.activity.compose.setContent


import androidx.activity.enableEdgeToEdge


import androidx.activity.viewModels


import androidx.compose.animation.Crossfade
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
//


import androidx.compose.animation.core.FastOutSlowInEasing


import androidx.compose.animation.core.tween


import androidx.compose.foundation.background


import androidx.compose.foundation.layout.*


import androidx.compose.material.icons.Icons


import androidx.compose.material.icons.filled.*


import androidx.compose.material3.*


import androidx.compose.runtime.*


import androidx.compose.ui.Alignment


import androidx.compose.ui.Modifier


import androidx.compose.ui.graphics.vector.ImageVector


import androidx.compose.ui.input.pointer.PointerEventPass


import androidx.compose.ui.input.pointer.pointerInput


import androidx.compose.ui.platform.LocalFocusManager


import androidx.compose.ui.platform.testTag


import androidx.compose.ui.text.font.FontWeight


import androidx.compose.ui.unit.dp


import androidx.compose.ui.unit.sp


import androidx.fragment.app.FragmentActivity

import com.lojia.pos.data.AppLanguage

import com.lojia.pos.data.AppModule

import com.lojia.pos.data.POSSale

import com.lojia.pos.data.ShiftReport

import com.lojia.pos.ui.*


import com.lojia.pos.util.LocaleManager

import com.lojia.pos.util.TranslationEngine

import com.lojia.pos.util.UniversalLocalizationProvider


import kotlinx.coroutines.delay


import kotlinx.coroutines.isActive


import kotlinx.coroutines.launch


import androidx.compose.ui.res.stringResource


/**
 * Sealed navigation state hierarchy to strictly isolate Shop and ShiftReport modules.
 */
sealed interface AppNavState {
    val titleKey: String
    val icon: ImageVector

    sealed class Shop(override val titleKey: String, override val icon: ImageVector) : AppNavState {
        data object Pos : Shop("nav_pos", Icons.Default.PointOfSale)
        data object Inventory : Shop("nav_inventory", Icons.Default.Inventory2)
        data object Analytics : Shop("nav_analytics", Icons.Default.BarChart)
        data class SettingsDetail(val section: String = "profile") : Shop("nav_settings", Icons.Default.Settings)

        companion object {
            val primaryTabs: List<Shop> get() = listOf(Pos, Inventory, Analytics)
        }
    }

    sealed class ShiftReportState(override val titleKey: String, override val icon: ImageVector) : AppNavState {
        data object Reports : ShiftReportState("nav_reports", Icons.Default.Assessment)
        data object Analytics : ShiftReportState("nav_analytics", Icons.Default.BarChart)
        data class SettingsDetail(val section: String = "profile") : ShiftReportState("nav_settings", Icons.Default.Settings)

        companion object {
            val primaryTabs: List<ShiftReportState> get() = listOf(Reports, Analytics)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : androidx.appcompat.app.AppCompatActivity() {
    private val reportViewModel: ReportViewModel by viewModels()
    private val posViewModel: PosViewModel by viewModels()

    private val userInteractionTime = kotlinx.coroutines.flow.MutableStateFlow(System.currentTimeMillis())

    override fun onUserInteraction() {
        super.onUserInteraction()
        userInteractionTime.value = System.currentTimeMillis()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize saved language
        val savedLang = com.lojia.pos.util.LanguagePreferences.getLanguage(this)
        val appLocale = androidx.core.os.LocaleListCompat.forLanguageTags(savedLang)
        androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(appLocale)
        enableEdgeToEdge()

        // Initialize Universal Real-Time Translation Engine
        TranslationEngine.init(applicationContext)

        // Schedule periodic background shift report sync via WorkManager
        com.lojia.pos.util.ShiftReportSyncScheduler.schedulePeriodicSync(
            context = applicationContext,
            intervalMinutes = 60L,
            requireWifiOnly = false
        )

        // Schedule periodic background inventory level check via WorkManager
        com.lojia.pos.util.InventoryCheckScheduler.schedulePeriodicCheck(
            context = applicationContext,
            intervalMinutes = 60L
        )

        handleAuthIntent(intent)

        setContent {
            val currentLanguage by reportViewModel.currentLanguage.collectAsState()
            val userProfile by reportViewModel.userProfile.collectAsState()
            val activeModule by reportViewModel.currentModule.collectAsState()
            val businessProfile by reportViewModel.businessProfile.collectAsState()

            var navState by remember {
                mutableStateOf<AppNavState>(
                    if (activeModule == AppModule.SHOPPING) AppNavState.Shop.Pos else AppNavState.ShiftReportState.Reports
                )
            }

            // Ensure navigation state remains in sync with the active module
            LaunchedEffect(activeModule) {
                if (activeModule == AppModule.SHOPPING && navState !is AppNavState.Shop) {
                    navState = AppNavState.Shop.Pos
                } else if (activeModule == AppModule.SHIFT_REPORT && navState !is AppNavState.ShiftReportState) {
                    navState = AppNavState.ShiftReportState.Reports
                }
            }

            var previewReport by remember { mutableStateOf<ShiftReport?>(null) }
            var previewSale by remember { mutableStateOf<POSSale?>(null) }
            var isAuthenticated by remember { mutableStateOf(false) }
            val lastInteractionTime by userInteractionTime.collectAsState()

            LaunchedEffect(isAuthenticated, lastInteractionTime, userProfile?.autoLockMinutes) {
                val lockMinutes = userProfile?.autoLockMinutes ?: 5
                if (isAuthenticated && lockMinutes > 0) {
                    while (true) {
                        kotlinx.coroutines.delay(10_000)
                        val elapsed = System.currentTimeMillis() - lastInteractionTime
                        if (elapsed >= lockMinutes * 60 * 1000L) {
                            reportViewModel.preferencesRepository.recordLogout(keepQuickLoginState = true)
                            isAuthenticated = false
                            break
                        }
                    }
                }
            }

            val performSecureModuleSwitch: (AppModule) -> Unit = { targetModule ->
                if (targetModule != activeModule) {
                    reportViewModel.switchModule(targetModule)
                    navState = if (targetModule == AppModule.SHOPPING) AppNavState.Shop.Pos else AppNavState.ShiftReportState.Reports
                }
            }

            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            val scope = rememberCoroutineScope()
            val isSettingsActive = navState is AppNavState.Shop.SettingsDetail || navState is AppNavState.ShiftReportState.SettingsDetail

            UniversalLocalizationProvider(currentLanguage = currentLanguage) {
                LojiaTheme {
                    if (!isAuthenticated) {
                        BiometricLockScreen(
                            activity = this@MainActivity,
                            userProfile = userProfile,
                            businessProfile = businessProfile,
                            language = currentLanguage,
                            onAuthenticated = {
                                isAuthenticated = true
                                userInteractionTime.value = System.currentTimeMillis()
                                userProfile?.let {
                                    reportViewModel.preferencesRepository.saveUserSession(
                                        username = it.username,
                                        fullName = it.fullName,
                                        email = it.email
                                    )
                                }
                            },
                            onSaveUserProfile = { updatedProfile ->
                                reportViewModel.saveUserProfile(updatedProfile)
                            },
                            onSaveBusinessProfile = { updatedBiz ->
                                reportViewModel.saveBusinessProfile(updatedBiz)
                            }
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            val focusManager = LocalFocusManager.current
                            val isRootScreen = when (navState) {
                                is AppNavState.Shop.Pos -> true
                                is AppNavState.ShiftReportState.Reports -> true
                                else -> false
                            }

                            // Handle back button smoothly to close drawer or return from sub-screens to main view
                            BackHandler(enabled = drawerState.isOpen || !isRootScreen) {
                                if (drawerState.isOpen) {
                                    scope.launch { drawerState.close() }
                                } else if (!isRootScreen) {
                                    focusManager.clearFocus()
                                    navState = if (activeModule == AppModule.SHOPPING) AppNavState.Shop.Pos else AppNavState.ShiftReportState.Reports
                                }
                            }

                            ModalNavigationDrawer(
                                drawerState = drawerState,
                                drawerContent = {
                                    MainAppDrawer(
                                        activeModule = activeModule,
                                        navState = navState,
                                        currentLanguage = currentLanguage,
                                        businessProfile = businessProfile,
                                        userProfile = userProfile,
                                        onNavigate = { newNavState -> navState = newNavState },
                                        onSwitchModule = performSecureModuleSwitch,
                                        onOpenShopMenu = { menuKey -> reportViewModel.selectShopSettingsMenu(menuKey) },
                                        onOpenReportMenu = { menuKey -> reportViewModel.selectReportSettingsMenu(menuKey) },
                                        onCloseDrawer = { scope.launch { drawerState.close() } },
                                        onLockApp = {
                                            reportViewModel.preferencesRepository.recordLogout(keepQuickLoginState = true)
                                            isAuthenticated = false
                                        }
                                    )
                                }
                            ) {
                                Scaffold(
                                    topBar = {
                                        val isPosScreen = navState is AppNavState.Shop.Pos
                                        val topBarBg = if (isPosScreen) LoyverseTopGreen else if (activeModule == AppModule.SHOPPING) LoyverseGreenDark else PrimaryIndigo
                                        val cartItems by posViewModel.cartItems.collectAsState()
                                        val cartItemCount = cartItems.sumOf { it.quantity.toInt() }

                                        val screenTitle = when (val state = navState) {
                                            is AppNavState.Shop.Pos -> "Ticket"
                                            is AppNavState.Shop.Inventory -> "Inventory"
                                            is AppNavState.Shop.Analytics -> "Analytics"
                                            is AppNavState.Shop.SettingsDetail -> {
                                                when (state.section) {
                                                    "root", "all", "overview" -> "Settings"
                                                    "sales" -> "Sales"
                                                    "receipts" -> "Receipts"
                                                    "shift" -> "Shift"
                                                    "items" -> "Items"
                                                    "cashiers", "cashier" -> "Cashier"
                                                    "profile" -> "Profile"
                                                    "security" -> "Security"
                                                    "settings_sub", "settings" -> "Settings"
                                                    "back_office" -> "Back office"
                                                    "apps" -> "Apps"
                                                    "language" -> "Language"
                                                    "support" -> "Support"
                                                    "about" -> "About"
                                                    "switch_module" -> "Switch Module"
                                                    else -> state.section.replace('_', ' ').replaceFirstChar { it.uppercase() }
                                                }
                                            }
                                            is AppNavState.ShiftReportState.Reports -> "Shift Report"
                                            is AppNavState.ShiftReportState.Analytics -> "Analytics"
                                            is AppNavState.ShiftReportState.SettingsDetail -> {
                                                when (state.section) {
                                                    "root", "all", "overview" -> "Settings"
                                                    "cashiers" -> "Cashier"
                                                    "profile" -> "Profile"
                                                    "security" -> "Security"
                                                    "backup" -> "Backup"
                                                    "country", "countries", "currency" -> "Country"
                                                    "language" -> "Language"
                                                    "about" -> "About"
                                                    "support" -> "Support"
                                                    "switch_module" -> "Switch Module"
                                                    else -> state.section.replace('_', ' ').replaceFirstChar { it.uppercase() }
                                                }
                                            }
                                        }

                                        Surface(
                                            color = topBarBg,
                                            shadowElevation = 4.dp
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .statusBarsPadding()
                                            ) {
                                                // Top Header Row with Back / Drawer Toggle and Screen Title
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(56.dp)
                                                        .padding(horizontal = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    // Left Navigation Icon: Drawer Hamburger menu icon (always on left)
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        IconButton(
                                                            onClick = { scope.launch { drawerState.open() } },
                                                            modifier = Modifier.testTag("drawer_menu_btn")
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Menu,
                                                                contentDescription = rememberTranslatedString(stringResource(R.string.title_drawer_menu)),
                                                                tint = PureWhite
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        DynamicText(
                                                            text = screenTitle,
                                                            color = PureWhite,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = if (isPosScreen) 20.sp else 18.sp,
                                                            maxLines = 1
                                                        )
                                                        if (isPosScreen) {
                                                            Spacer(modifier = Modifier.width(10.dp))
                                                            LoyverseTicketBadge(
                                                                itemCount = cartItemCount,
                                                                onClick = { posViewModel.showTicketSheet.value = true }
                                                            )
                                                        }
                                                    }

                                                    // Right Icons for POS
                                                    if (isPosScreen) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            IconButton(
                                                                onClick = { posViewModel.showCustomerDialog.value = true },
                                                                modifier = Modifier.size(44.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.PersonAdd,
                                                                    contentDescription = rememberTranslatedString(stringResource(R.string.title_customer)),
                                                                    tint = PureWhite,
                                                                    modifier = Modifier.size(24.dp)
                                                                )
                                                            }
                                                            IconButton(
                                                                onClick = { posViewModel.showOptionsMenu.value = true },
                                                                modifier = Modifier.size(44.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.MoreVert,
                                                                    contentDescription = rememberTranslatedString(stringResource(R.string.title_options)),
                                                                    tint = PureWhite,
                                                                    modifier = Modifier.size(24.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    containerColor = MaterialTheme.colorScheme.background
                                ) { innerPadding ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(innerPadding)
                                            .background(MaterialTheme.colorScheme.background)
                                    ) {
                                        // Main Navigation Host managing Shop and ShiftReport states
                                        AppNavigationHost(
                                            navState = navState,
                                            posViewModel = posViewModel,
                                            reportViewModel = reportViewModel,
                                            language = currentLanguage,
                                            onOpenDrawer = { scope.launch { drawerState.open() } },
                                            onPreviewPdf = { report -> previewReport = report },
                                            onSaleCompleted = { sale -> previewSale = sale },
                                            onSwitchModule = performSecureModuleSwitch
                                        )

                                        // Shift Report Preview Dialog
                                        previewReport?.let { report ->
                                            ShiftReportPreviewDialog(
                                                report = report,
                                                businessProfile = businessProfile,
                                                language = currentLanguage,
                                                onDismiss = { previewReport = null },
                                                onOpenPrinterSettings = {
                                                    reportViewModel.switchModule(AppModule.SHOPPING)
                                                    navState = AppNavState.Shop.SettingsDetail("printers")
                                                }
                                            )
                                        }

                                        // POS Receipt Preview Dialog
                                        previewSale?.let { sale ->
                                            SaleReceiptPreviewDialog(
                                                sale = sale,
                                                businessProfile = businessProfile,
                                                language = currentLanguage,
                                                onDismiss = { previewSale = null },
                                                onOpenPrinterSettings = {
                                                    reportViewModel.switchModule(AppModule.SHOPPING)
                                                    navState = AppNavState.Shop.SettingsDetail("printers")
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleAuthIntent(intent)
    }

    private fun handleAuthIntent(intent: android.content.Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme == "com.lojia.pos" && uri.host == "oauth2callback") {
            reportViewModel.handleDriveAuthRedirect(uri)
        }
    }
}

/**
 * Isolated Navigation Host for switching between Shop and Shift Report modules.
 */
@Composable
fun AppNavigationHost(
    navState: AppNavState,
    posViewModel: PosViewModel,
    reportViewModel: ReportViewModel,
    language: AppLanguage,
    onOpenDrawer: () -> Unit,
    onPreviewPdf: (ShiftReport) -> Unit,
    onSaleCompleted: (POSSale) -> Unit,
    onSwitchModule: (AppModule) -> Unit,
    modifier: Modifier = Modifier
) {
    Crossfade(
        targetState = navState,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "AppNavHostTransition",
        modifier = modifier
    ) { destination ->
        when (destination) {
            is AppNavState.Shop -> {
                ShopModuleNavHost(
                    destination = destination,
                    posViewModel = posViewModel,
                    reportViewModel = reportViewModel,
                    language = language,
                    onOpenDrawer = onOpenDrawer,
                    onSaleCompleted = onSaleCompleted,
                    onSwitchModule = onSwitchModule
                )
            }
            is AppNavState.ShiftReportState -> {
                ShiftReportModuleNavHost(
                    destination = destination,
                    reportViewModel = reportViewModel,
                    posViewModel = posViewModel,
                    language = language,
                    onPreviewPdf = onPreviewPdf,
                    onSwitchModule = onSwitchModule
                )
            }
        }
    }
}

/**
 * Isolated Shop Module Container (POS Register, Inventory, Sales Analytics, Shop Settings).
 */
@Composable
private fun ShopModuleNavHost(
    destination: AppNavState.Shop,
    posViewModel: PosViewModel,
    reportViewModel: ReportViewModel,
    language: AppLanguage,
    onOpenDrawer: () -> Unit,
    onSaleCompleted: (POSSale) -> Unit,
    onSwitchModule: (AppModule) -> Unit
) {
    when (destination) {
        is AppNavState.Shop.Pos -> {
            PosScreen(
                posViewModel = posViewModel,
                reportViewModel = reportViewModel,
                language = language,
                onMenuClick = onOpenDrawer,
                onSaleCompleted = onSaleCompleted
            )
        }
        is AppNavState.Shop.Inventory -> {
            InventoryScreen(
                posViewModel = posViewModel,
                reportViewModel = reportViewModel,
                language = language
            )
        }
        is AppNavState.Shop.Analytics -> {
            DashboardScreen(
                reportViewModel = reportViewModel,
                posViewModel = posViewModel,
                language = language
            )
        }
        is AppNavState.Shop.SettingsDetail -> {
            LaunchedEffect(destination.section) {
                reportViewModel.selectShopSettingsMenu(destination.section)
            }
            SettingsScreen(
                reportViewModel = reportViewModel,
                posViewModel = posViewModel,
                activeModule = AppModule.SHOPPING,
                onSwitchModule = onSwitchModule
            )
        }
    }
}

/**
 * Isolated Shift Report Module Container (Shift Reports, Analytics, Report Settings).
 */
@Composable
private fun ShiftReportModuleNavHost(
    destination: AppNavState.ShiftReportState,
    reportViewModel: ReportViewModel,
    posViewModel: PosViewModel,
    language: AppLanguage,
    onPreviewPdf: (ShiftReport) -> Unit,
    onSwitchModule: (AppModule) -> Unit
) {
    when (destination) {
        is AppNavState.ShiftReportState.Reports -> {
            ShiftReportScreen(
                viewModel = reportViewModel,
                language = language,
                onPreviewPdf = onPreviewPdf
            )
        }
        is AppNavState.ShiftReportState.Analytics -> {
            DashboardScreen(
                reportViewModel = reportViewModel,
                posViewModel = posViewModel,
                language = language
            )
        }
        is AppNavState.ShiftReportState.SettingsDetail -> {
            LaunchedEffect(destination.section) {
                reportViewModel.selectReportSettingsMenu(destination.section)
            }
            SettingsScreen(
                reportViewModel = reportViewModel,
                posViewModel = posViewModel,
                activeModule = AppModule.SHIFT_REPORT,
                onSwitchModule = onSwitchModule
            )
        }
    }
    }
 
