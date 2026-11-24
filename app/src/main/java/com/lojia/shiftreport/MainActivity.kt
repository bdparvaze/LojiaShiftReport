package com.lojia.shiftreport

import com.lojia.shiftreport.R
import com.lojia.shiftreport.auth.*
import com.lojia.shiftreport.data.*
import com.lojia.shiftreport.report.*
import com.lojia.shiftreport.settings.*
import com.lojia.shiftreport.ui.common.*
import com.lojia.shiftreport.ui.theme.*
import com.lojia.shiftreport.util.*

import android.os.Bundle

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import com.lojia.shiftreport.data.AppLanguage
import com.lojia.shiftreport.data.AppModule
import com.lojia.shiftreport.data.ShiftReport

import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource

/**
 * Sealed navigation state hierarchy for Shift Report application.
 */
sealed interface AppNavState {
    val titleKey: String
    val icon: ImageVector

    sealed class ShiftReportState(override val titleKey: String, override val icon: ImageVector) : AppNavState {
        data object Home : ShiftReportState("nav_home", Icons.Default.Home)
        data object Reports : ShiftReportState("nav_reports", Icons.Default.Assessment)
        data object Analytics : ShiftReportState("nav_analytics", Icons.Default.BarChart)
        data object DocumentScanner : ShiftReportState("nav_document_scanner", Icons.Outlined.DocumentScanner)
        data class SettingsDetail(val section: String = "profile") : ShiftReportState("nav_settings", Icons.Default.Settings)

        companion object {
            val primaryTabs: List<ShiftReportState> get() = listOf(Home, Reports, Analytics)
        }
    }
}

private val AppNavStateSaver = androidx.compose.runtime.saveable.Saver<AppNavState, String>(
    save = { state ->
        when (state) {
            is AppNavState.ShiftReportState.Home -> "home"
            is AppNavState.ShiftReportState.Reports -> "reports"
            is AppNavState.ShiftReportState.Analytics -> "analytics"
            is AppNavState.ShiftReportState.DocumentScanner -> "scanner"
            is AppNavState.ShiftReportState.SettingsDetail -> "settings:${state.section}"
            else -> "home"
        }
    },
    restore = { key ->
        when {
            key == "home" -> AppNavState.ShiftReportState.Home
            key == "reports" -> AppNavState.ShiftReportState.Reports
            key == "analytics" -> AppNavState.ShiftReportState.Analytics
            key == "scanner" -> AppNavState.ShiftReportState.DocumentScanner
            key.startsWith("settings:") -> AppNavState.ShiftReportState.SettingsDetail(key.removePrefix("settings:"))
            else -> AppNavState.ShiftReportState.Home
        }
    }
)

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : androidx.appcompat.app.AppCompatActivity() {
    private val reportViewModel: ReportViewModel by viewModels()
    private val userInteractionTime = kotlinx.coroutines.flow.MutableStateFlow(System.currentTimeMillis())

    override fun onUserInteraction() {
        super.onUserInteraction()
        userInteractionTime.value = System.currentTimeMillis()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // POS-style security: prevent screenshots in release builds.
        // Disabled in DEBUG so development streaming emulator preview displays properly.
        if (!BuildConfig.DEBUG) {
            window.setFlags(
                android.view.WindowManager.LayoutParams.FLAG_SECURE,
                android.view.WindowManager.LayoutParams.FLAG_SECURE
            )
        }

        // Initialize POS session manager (screen-off & minimization instant lock)
        AppSessionManager.init(applicationContext)

        // Initialize saved language
        val savedLang = com.lojia.shiftreport.util.LanguagePreferences.getLanguage(this)
        val currentLocales = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales()
        if (currentLocales.isEmpty || currentLocales.toLanguageTags() != savedLang) {
            val appLocale = androidx.core.os.LocaleListCompat.forLanguageTags(savedLang)
            androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(appLocale)
        }
        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = androidx.activity.SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )

        // Schedule periodic background shift report sync via WorkManager
        if (com.lojia.shiftreport.BuildConfig.CLOUD_SYNC_ENABLED) {
            com.lojia.shiftreport.sync.FirebaseCloudSyncScheduler.schedulePeriodicSync(
                context = applicationContext,
                intervalMinutes = 60L
            )
        }

        handleAuthIntent(intent)

        setContent {
            val currentLanguage by reportViewModel.currentLanguage.collectAsState()
            val userProfile by reportViewModel.userProfile.collectAsState()
            val activeModule by reportViewModel.currentModule.collectAsState()
            val businessProfile by reportViewModel.businessProfile.collectAsState()

            var navState by rememberSaveable(stateSaver = AppNavStateSaver) {
                mutableStateOf<AppNavState>(AppNavState.ShiftReportState.Home)
            }
            var requestedReportTab by rememberSaveable { mutableStateOf(ShiftReportTab.REPORT_ENTRY) }
            var requestedEntryModal by remember { mutableStateOf(ModalType.NONE) }
            var reportRouteNonce by remember { mutableLongStateOf(0L) }

            val navBackStack = remember { mutableStateListOf<AppNavState>() }

            fun navigateTo(newState: AppNavState) {
                if (newState != navState) {
                    navBackStack.add(navState)
                    navState = newState
                }
            }

            LaunchedEffect(navState) {
                when (val state = navState) {
                    is AppNavState.ShiftReportState.SettingsDetail -> {
                        reportViewModel.selectReportSettingsMenu(state.section)
                    }
                    else -> {
                        reportViewModel.selectReportSettingsMenu("")
                    }
                }
            }

            var previewReport by remember { mutableStateOf<ShiftReport?>(null) }
            val context = androidx.compose.ui.platform.LocalContext.current
            val scope = rememberCoroutineScope()
            val prefsRepo = remember { PreferencesRepository.getInstance(context) }
            val backupManager = remember { com.lojia.shiftreport.settings.backup.BackupManager.getInstance(context) }
            val restorePolicyManager = remember { RestorePolicyManager(prefsRepo, backupManager) }
            var restoreDecision by remember { mutableStateOf<RestorePolicyManager.RestoreDecision?>(null) }
            val reinstallMeta = remember { OfflineBackupManager.readReinstallMeta(context) }

            var showPreservationSetup by rememberSaveable {
                mutableStateOf(!prefsRepo.isFirstRunDataPreservationShown() && !reinstallMeta.first)
            }

            var isAuthenticated by rememberSaveable { 
                mutableStateOf(reportViewModel.preferencesRepository.isLoggedIn()) 
            }

            val currentUserId = remember(userProfile, prefsRepo) {
                userProfile?.id?.toString() ?: userProfile?.username ?: prefsRepo.getSavedUsername().ifBlank { "anonymous" }
            }

            LaunchedEffect(isAuthenticated, currentUserId) {
                if (isAuthenticated) {
                    val isNewAccount = prefsRepo.isNewAccount()
                    val decision = restorePolicyManager.evaluate(currentUserId, isNewAccount)
                    restoreDecision = decision
                    if (isNewAccount) {
                        prefsRepo.setNewAccount(false)
                    }
                }
            }

            var pendingMainRestoreAction by remember { mutableStateOf<(() -> Unit)?>(null) }
            var mainRestoreUri by remember { mutableStateOf<android.net.Uri?>(null) }
            var mainRestoreFile by remember { mutableStateOf<java.io.File?>(null) }
            var mainRestoreLabel by remember { mutableStateOf<String?>(null) }
            var isMainRestoreUnlocking by remember { mutableStateOf(false) }
            var mainRestorePasswordError by remember { mutableStateOf<String?>(null) }

            val backupPickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument()
            ) { uri ->
                if (uri != null) {
                    val runRestore = {
                        restoreDecision = null
                        prefsRepo.setFirstRunDataPreservationShown(true)
                        prefsRepo.setHasRunBefore(true)
                        scope.launch {
                            prefsRepo.setHasSeenRestoreDialog(true)
                            val requiresPassword = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                val bytes = runCatching {
                                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                                }.getOrNull()
                                bytes != null && com.lojia.shiftreport.util.backupTierOf(bytes) == com.lojia.shiftreport.util.TIER_PORTABLE
                            }
                            if (requiresPassword) {
                                mainRestorePasswordError = null
                                mainRestoreUri = uri
                                mainRestoreFile = null
                                mainRestoreLabel = uri.lastPathSegment
                            } else {
                                val db = AppDatabase.getInstance(context)
                                val res = OfflineBackupManager.restoreBackup(context, uri, db)
                                res.onSuccess { count ->
                                    val msg = context.getString(R.string.data_restored_success, count)
                                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                                }.onFailure { err ->
                                    val msg = OfflineBackupManager.resolveRestoreErrorMessage(context, err)
                                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        Unit
                    }
                    if (userProfile != null || prefsRepo.hasPinConfigured()) {
                        pendingMainRestoreAction = runRestore
                    } else {
                        runRestore()
                    }
                }
            }

            // POS Instant Session Lock Observer (Screen-off and onStop/Minimization)
            val shouldLockApp by AppSessionManager.shouldLockApp.collectAsState()
            LaunchedEffect(shouldLockApp) {
                if (shouldLockApp) {
                    reportViewModel.preferencesRepository.recordLogout(keepQuickLoginState = true)
                    isAuthenticated = false
                    AppSessionManager.onAppLocked()
                }
            }
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

            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            val topMenuState = remember { TopMenuStateHolder() }

            LaunchedEffect(drawerState.currentValue, drawerState.targetValue) {
                val active = drawerState.currentValue == DrawerValue.Open ||
                    drawerState.targetValue == DrawerValue.Open
                topMenuState.syncExternalState(active)
            }

            val openTopMenu: () -> Unit = {
                topMenuState.open()
                scope.launch { drawerState.open() }
            }

            val closeTopMenu: () -> Unit = {
                topMenuState.dismiss()
                scope.launch { drawerState.close() }
            }

            LojiaTheme(
                language = currentLanguage,
                isLightSystemBarCanvas = !isAuthenticated
            ) {
                if (!isAuthenticated) {
                        BiometricLockScreen(
                            activity = this@MainActivity,
                            userProfile = userProfile,
                            businessProfile = businessProfile,
                            language = currentLanguage,
                            onAuthenticated = {
                                isAuthenticated = true
                                userInteractionTime.value = System.currentTimeMillis()
                                val uName = userProfile?.username?.ifBlank { null }
                                    ?: reportViewModel.preferencesRepository.getSavedUsername()
                                reportViewModel.preferencesRepository.saveUserSession(
                                    username = uName,
                                    fullName = userProfile?.fullName,
                                    email = userProfile?.email
                                )
                            },
                            onSaveUserProfile = { updatedProfile ->
                                reportViewModel.saveUserProfile(updatedProfile)
                                prefsRepo.setNewAccount(true)
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
                            val isRootScreen = navState is AppNavState.ShiftReportState.Home
                            val isScannerActive = navState is AppNavState.ShiftReportState.DocumentScanner

                            // Handle back button smoothly to close drawer, pop back stack, return to Home or exit
                            val canHandleBack = drawerState.isOpen || topMenuState.isOpen || navBackStack.isNotEmpty() || navState != AppNavState.ShiftReportState.Home
                            BackHandler(enabled = !isScannerActive && canHandleBack) {
                                if (drawerState.isOpen || topMenuState.isOpen) {
                                    topMenuState.onDismissBackPress()
                                    scope.launch { drawerState.close() }
                                } else if (navBackStack.isNotEmpty()) {
                                    focusManager.clearFocus()
                                    navState = navBackStack.removeAt(navBackStack.lastIndex)
                                } else if (navState != AppNavState.ShiftReportState.Home) {
                                    focusManager.clearFocus()
                                    navState = AppNavState.ShiftReportState.Home
                                }
                            }

                            val depthStyle = LocalDepthStyle.current
                            val screenCanvasBackground = when (navState) {
                                is AppNavState.ShiftReportState.Home,
                                is AppNavState.ShiftReportState.Reports,
                                is AppNavState.ShiftReportState.SettingsDetail -> depthStyle.reportPageBackground
                                else -> MaterialTheme.colorScheme.background
                            }

                            ModalNavigationDrawer(
                                drawerState = drawerState,
                                gesturesEnabled = drawerState.isOpen || drawerState.isAnimationRunning || topMenuState.isOpen,
                                drawerContent = {
                                    MainAppDrawer(
                                        activeModule = activeModule,
                                        navState = navState,
                                        currentLanguage = currentLanguage,
                                        businessProfile = businessProfile,
                                        userProfile = userProfile,
                                        onNavigate = { newNavState ->
                                            topMenuState.onSelectItem { navigateTo(newNavState) }
                                        },
                                        onOpenReportMenu = { menuKey ->
                                            topMenuState.onSelectItem {
                                                navigateTo(AppNavState.ShiftReportState.SettingsDetail(menuKey))
                                            }
                                        },
                                        onCloseDrawer = closeTopMenu,
                                        onLockApp = {
                                            topMenuState.onSelectItem {
                                                reportViewModel.preferencesRepository.recordLogout(keepQuickLoginState = true)
                                                isAuthenticated = false
                                            }
                                        }
                                    )
                                }
                            ) {
                                Scaffold(
                                    topBar = {
                                        val selectedSettingsMenu by reportViewModel.selectedReportSettingsMenu.collectAsState()
                                        val isFullScreenAddressRoute = navState is AppNavState.ShiftReportState.SettingsDetail &&
                                                selectedSettingsMenu?.startsWith("settings/profile/addresses") == true

                                        val isHomeRoute = navState is AppNavState.ShiftReportState.Home

                                        if (!isFullScreenAddressRoute && !isHomeRoute) {
                                            val screenTitle = when (val state = navState) {
                                                is AppNavState.ShiftReportState.Home -> stringResource(R.string.nav_home)
                                                is AppNavState.ShiftReportState.Reports -> stringResource(R.string.nav_reports)
                                                is AppNavState.ShiftReportState.Analytics -> stringResource(R.string.nav_analytics)
                                                is AppNavState.ShiftReportState.DocumentScanner -> stringResource(R.string.document_scanner)
                                                is AppNavState.ShiftReportState.SettingsDetail -> {
                                                    when (state.section) {
                                                        "root", "all", "overview" -> stringResource(R.string.nav_settings)
                                                        "cashiers" -> stringResource(R.string.cashier_management)
                                                        "profile" -> stringResource(R.string.profile)
                                                        "security" -> stringResource(R.string.security)
                                                        "backup" -> stringResource(R.string.backup)
                                                        "printer", "thermal_printer" -> stringResource(R.string.printer_setup_title)
                                                        "country", "countries", "currency" -> stringResource(R.string.country)
                                                        "language" -> stringResource(R.string.language)
                                                        "about" -> stringResource(R.string.about)
                                                        "support" -> stringResource(R.string.support)
                                                        else -> state.section.replace('_', ' ').replaceFirstChar { it.uppercase() }
                                                    }
                                                }
                                            }

                                            LojiaGradientHeader(
                                                shape = RectangleShape,
                                                contentPadding = PaddingValues(0.dp)
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
                                                            .defaultMinSize(minHeight = Dimens.TopBarHeight)
                                                            .padding(horizontal = Dimens.SpacingSm),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        // Left Navigation Icon: Drawer Hamburger menu icon
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = Modifier.weight(1f)
                                                        ) {
                                                            IconButton(
                                                                onClick = openTopMenu,
                                                                modifier = Modifier.testTag("drawer_menu_btn")
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Menu,
                                                                    contentDescription = stringResource(R.string.title_drawer_menu),
                                                                    tint = PureWhite
                                                                )
                                                            }
                                                            Spacer(modifier = Modifier.width(Dimens.SpacingXs))
                                                            Text(
                                                                text = screenTitle,
                                                                color = PureWhite,
                                                                fontWeight = FontWeight.SemiBold,
                                                                fontSize = DepthTypeTokens.TopBarTitleSize,
                                                                maxLines = 1,
                                                                modifier = Modifier.weight(1f, fill = false)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                                    containerColor = screenCanvasBackground
                                ) { innerPadding ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(innerPadding)
                                            .background(screenCanvasBackground)
                                    ) {
                                        AppNavigationHost(
                                            navState = navState,
                                            reportViewModel = reportViewModel,
                                            language = currentLanguage,
                                            activity = this@MainActivity,
                                            requestedReportTab = requestedReportTab,
                                            requestedEntryModal = requestedEntryModal,
                                            reportRouteNonce = reportRouteNonce,
                                            onReportRouteHandled = { requestedEntryModal = ModalType.NONE },
                                            onOpenDrawer = openTopMenu,
                                            onNavigateToReportTab = { targetTab, targetModal ->
                                                requestedReportTab = targetTab
                                                requestedEntryModal = targetModal
                                                reportRouteNonce += 1L
                                                navigateTo(AppNavState.ShiftReportState.Reports)
                                            },
                                            onNavigateToAnalytics = {
                                                navigateTo(AppNavState.ShiftReportState.Analytics)
                                            },
                                            onPreviewPdf = { report -> previewReport = report },
                                            onNavigateToSection = { section ->
                                                navigateTo(AppNavState.ShiftReportState.SettingsDetail(section))
                                            },
                                            onNavigateBack = {
                                                if (navBackStack.isNotEmpty()) {
                                                    navState = navBackStack.removeAt(navBackStack.lastIndex)
                                                } else {
                                                    navState = AppNavState.ShiftReportState.Home
                                                }
                                            }
                                        )

                                        // Shift Report Preview Dialog
                                        previewReport?.let { report ->
                                            ShiftReportPreviewDialog(
                                                report = report,
                                                businessProfile = businessProfile,
                                                language = currentLanguage,
                                                onDismiss = { previewReport = null },
                                                onOpenPrinterSettings = {
                                                    navigateTo(
                                                        AppNavState.ShiftReportState.SettingsDetail(
                                                            com.lojia.shiftreport.printer.PrinterNavigationConfig.resolvePreviewPrinterSettingsTarget()
                                                        )
                                                    )
                                                }
                                            )
                                        }

                                        // Data Recovery Dialog (evaluated securely by RestorePolicyManager)
                                        if (restoreDecision?.shouldShowDialog == true) {
                                            val decision = restoreDecision!!
                                            DataRecoveryDialog(
                                                backupFile = decision.backupFile,
                                                previewText = decision.previewText,
                                                onRestore = {
                                                    val runFileRestore = {
                                                        restoreDecision = null
                                                        scope.launch {
                                                            prefsRepo.setHasSeenRestoreDialog(true)
                                                            prefsRepo.setFirstRunDataPreservationShown(true)
                                                            prefsRepo.setHasRunBefore(true)
                                                            val fileToRestore = decision.backupFile ?: backupManager.restoreLatest()
                                                            if (fileToRestore != null) {
                                                                val requiresPassword = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                                                    val bytes = runCatching { fileToRestore.readBytes() }.getOrNull()
                                                                    bytes != null && com.lojia.shiftreport.util.backupTierOf(bytes) == com.lojia.shiftreport.util.TIER_PORTABLE
                                                                }
                                                                if (requiresPassword) {
                                                                    mainRestorePasswordError = null
                                                                    mainRestoreFile = fileToRestore
                                                                    mainRestoreUri = null
                                                                    mainRestoreLabel = fileToRestore.name
                                                                } else {
                                                                    val db = AppDatabase.getInstance(context)
                                                                    val res = OfflineBackupManager.restoreBackupFromFile(context, db, fileToRestore)
                                                                    res.onSuccess { count ->
                                                                        val msg = context.getString(R.string.data_restored_success, count)
                                                                        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                                                                    }.onFailure { err ->
                                                                        val msg = OfflineBackupManager.resolveRestoreErrorMessage(context, err)
                                                                        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                                                                    }
                                                                }
                                                            }
                                                        }
                                                        Unit
                                                    }
                                                    if (userProfile != null || prefsRepo.hasPinConfigured()) {
                                                        pendingMainRestoreAction = runFileRestore
                                                    } else {
                                                        runFileRestore()
                                                    }
                                                },
                                                onBrowseBackups = {
                                                    backupPickerLauncher.launch(arrayOf("application/octet-stream", "application/json", "*/*"))
                                                },
                                                onStartFresh = {
                                                    restoreDecision = null
                                                    scope.launch {
                                                        prefsRepo.setHasSeenRestoreDialog(true)
                                                        prefsRepo.setFirstRunDataPreservationShown(true)
                                                        prefsRepo.setHasRunBefore(true)
                                                        // Navigate to Dashboard
                                                        navState = AppNavState.ShiftReportState.Analytics
                                                    }
                                                }
                                            )
                                        }

                                        if (mainRestoreUri != null || mainRestoreFile != null) {
                                            com.lojia.shiftreport.settings.BackupRestorePasswordDialog(
                                                fileName = mainRestoreLabel,
                                                isRestoring = isMainRestoreUnlocking,
                                                errorMessage = mainRestorePasswordError,
                                                onDismiss = {
                                                    mainRestoreUri = null
                                                    mainRestoreFile = null
                                                    mainRestoreLabel = null
                                                    mainRestorePasswordError = null
                                                },
                                                onSubmitPassword = { enteredPassword ->
                                                    val uriTarget = mainRestoreUri
                                                    val fileTarget = mainRestoreFile
                                                    mainRestorePasswordError = null
                                                    isMainRestoreUnlocking = true
                                                    scope.launch {
                                                        val db = AppDatabase.getInstance(context)
                                                        val res = when {
                                                            uriTarget != null -> OfflineBackupManager.restoreBackup(
                                                                context = context,
                                                                uri = uriTarget,
                                                                database = db,
                                                                password = enteredPassword
                                                            )
                                                            fileTarget != null -> OfflineBackupManager.restoreBackupFromFile(
                                                                context = context,
                                                                database = db,
                                                                backupFile = fileTarget,
                                                                password = enteredPassword
                                                            )
                                                            else -> Result.failure(IllegalStateException("No backup selected"))
                                                        }
                                                        isMainRestoreUnlocking = false
                                                        res.onSuccess { count ->
                                                            mainRestoreUri = null
                                                            mainRestoreFile = null
                                                            mainRestoreLabel = null
                                                            mainRestorePasswordError = null
                                                            val msg = context.getString(R.string.data_restored_success, count)
                                                            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                                                        }.onFailure { err ->
                                                            val msg = OfflineBackupManager.resolveRestoreErrorMessage(context, err)
                                                            mainRestorePasswordError = msg
                                                            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                }
                                            )
                                        }

                                        if (pendingMainRestoreAction != null) {
                                            AdminAuthDialog(
                                                title = stringResource(R.string.admin_authorization_required),
                                                subtitle = stringResource(R.string.restore_backup_title),
                                                onDismissRequest = { pendingMainRestoreAction = null },
                                                onAuthSuccess = {
                                                    val action = pendingMainRestoreAction
                                                    pendingMainRestoreAction = null
                                                    action?.invoke()
                                                }
                                            )
                                        }

                                        // Data Preservation First-Run Setup Dialog
                                        if (showPreservationSetup) {
                                            DataPreservationSetupDialog(
                                                onChoice = { preserve ->
                                                    prefsRepo.setPreserveDataOnUninstall(preserve)
                                                    prefsRepo.setFirstRunDataPreservationShown(true)
                                                    prefsRepo.setHasRunBefore(true)
                                                    OfflineBackupManager.saveReinstallMeta(context, preserve)
                                                    showPreservationSetup = false
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

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleAuthIntent(intent)
    }

    private fun handleAuthIntent(intent: android.content.Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme == "com.lojia.shiftreport" && uri.host == "oauth2callback") {
            val state = uri.getQueryParameter("state")
            reportViewModel.handleDriveAuthRedirect(uri, state)
        }
    }

    override fun onDestroy() {
        AppSessionManager.cleanup(applicationContext)
        super.onDestroy()
    }
}

/**
 * Isolated Navigation Host for Shift Report module.
 */
@Composable
fun AppNavigationHost(
    navState: AppNavState,
    reportViewModel: ReportViewModel,
    language: AppLanguage,
    activity: android.app.Activity,
    requestedReportTab: ShiftReportTab = ShiftReportTab.REPORT_ENTRY,
    requestedEntryModal: ModalType = ModalType.NONE,
    reportRouteNonce: Long = 0L,
    onReportRouteHandled: () -> Unit = {},
    onOpenDrawer: () -> Unit = {},
    onNavigateToReportTab: (ShiftReportTab, ModalType) -> Unit = { _, _ -> },
    onNavigateToAnalytics: () -> Unit = {},
    onPreviewPdf: (ShiftReport) -> Unit,
    onNavigateToSection: (String) -> Unit = {},
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Crossfade(
        targetState = navState,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "AppNavHostTransition",
        modifier = modifier
    ) { destination ->
        when (destination) {
            is AppNavState.ShiftReportState -> {
                ShiftReportModuleNavHost(
                    destination = destination,
                    reportViewModel = reportViewModel,
                    language = language,
                    activity = activity,
                    requestedReportTab = requestedReportTab,
                    requestedEntryModal = requestedEntryModal,
                    reportRouteNonce = reportRouteNonce,
                    onReportRouteHandled = onReportRouteHandled,
                    onOpenDrawer = onOpenDrawer,
                    onNavigateToReportTab = onNavigateToReportTab,
                    onNavigateToAnalytics = onNavigateToAnalytics,
                    onPreviewPdf = onPreviewPdf,
                    onNavigateToSection = onNavigateToSection,
                    onNavigateBack = onNavigateBack
                )
            }
        }
    }
}

/**
 * Isolated Shift Report Module Container (Home Dashboard, Shift Reports, Analytics, Report Settings).
 */
@Composable
private fun ShiftReportModuleNavHost(
    destination: AppNavState.ShiftReportState,
    reportViewModel: ReportViewModel,
    language: AppLanguage,
    activity: android.app.Activity,
    requestedReportTab: ShiftReportTab = ShiftReportTab.REPORT_ENTRY,
    requestedEntryModal: ModalType = ModalType.NONE,
    reportRouteNonce: Long = 0L,
    onReportRouteHandled: () -> Unit = {},
    onOpenDrawer: () -> Unit = {},
    onNavigateToReportTab: (ShiftReportTab, ModalType) -> Unit = { _, _ -> },
    onNavigateToAnalytics: () -> Unit = {},
    onPreviewPdf: (ShiftReport) -> Unit,
    onNavigateToSection: (String) -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    when (destination) {
        is AppNavState.ShiftReportState.Home -> {
            HomeDashboardScreen(
                reportViewModel = reportViewModel,
                language = language,
                onOpenDrawer = onOpenDrawer,
                onNavigateToReportTab = onNavigateToReportTab,
                onNavigateToAnalytics = onNavigateToAnalytics
            )
        }
        is AppNavState.ShiftReportState.Reports -> {
            ShiftReportScreen(
                viewModel = reportViewModel,
                language = language,
                requestedTab = requestedReportTab,
                requestedEntryModal = requestedEntryModal,
                routeNonce = reportRouteNonce,
                onRouteRequestHandled = onReportRouteHandled,
                onPreviewPdf = onPreviewPdf
            )
        }
        is AppNavState.ShiftReportState.Analytics -> {
            DashboardScreen(
                reportViewModel = reportViewModel,
                language = language
            )
        }
        is AppNavState.ShiftReportState.DocumentScanner -> {
            com.lojia.shiftreport.scanner.DocumentScannerScreen(activity = activity)
        }
        is AppNavState.ShiftReportState.SettingsDetail -> {
            LaunchedEffect(destination.section) {
                reportViewModel.selectReportSettingsMenu(destination.section)
            }
            val currentRoute = reportViewModel.selectedReportSettingsMenu.collectAsState().value.orEmpty()
            when {
                currentRoute == "settings/profile/addresses" -> {
                    SavedAddressesScreen(
                        viewModel = reportViewModel,
                        onBack = { reportViewModel.selectReportSettingsMenu("profile") },
                        onAddNew = { reportViewModel.selectReportSettingsMenu("settings/profile/addresses/new") },
                        onEdit = { id -> reportViewModel.selectReportSettingsMenu("settings/profile/addresses/edit/$id") }
                    )
                }
                currentRoute == "settings/profile/addresses/new" -> {
                    AddEditAddressScreen(
                        viewModel = reportViewModel,
                        addressId = null,
                        onBack = { reportViewModel.selectReportSettingsMenu("settings/profile/addresses") },
                        onSaved = { reportViewModel.selectReportSettingsMenu("settings/profile/addresses") }
                    )
                }
                currentRoute.startsWith("settings/profile/addresses/edit/") -> {
                    val id = currentRoute.removePrefix("settings/profile/addresses/edit/")
                    AddEditAddressScreen(
                        viewModel = reportViewModel,
                        addressId = id,
                        onBack = { reportViewModel.selectReportSettingsMenu("settings/profile/addresses") },
                        onSaved = { reportViewModel.selectReportSettingsMenu("settings/profile/addresses") }
                    )
                }
                else -> {
                    SettingsScreen(
                        reportViewModel = reportViewModel,
                        activeModule = AppModule.SHIFT_REPORT,
                        onNavigateToSection = onNavigateToSection,
                        onNavigateBack = onNavigateBack
                    )
                }
            }
        }
    }

}