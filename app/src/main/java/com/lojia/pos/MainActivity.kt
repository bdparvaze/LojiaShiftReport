package com.lojia.pos

import com.lojia.pos.R
import com.lojia.pos.auth.*
import com.lojia.pos.data.*
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.lojia.pos.data.AppLanguage
import com.lojia.pos.data.AppModule
import com.lojia.pos.data.ShiftReport
import com.lojia.pos.util.TranslationEngine
import com.lojia.pos.util.UniversalLocalizationProvider

import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource

/**
 * Sealed navigation state hierarchy for Shift Report application.
 */
sealed interface AppNavState {
    val titleKey: String
    val icon: ImageVector

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

        handleAuthIntent(intent)

        setContent {
            val currentLanguage by reportViewModel.currentLanguage.collectAsState()
            val userProfile by reportViewModel.userProfile.collectAsState()
            val activeModule by reportViewModel.currentModule.collectAsState()
            val businessProfile by reportViewModel.businessProfile.collectAsState()

            var navState by remember {
                mutableStateOf<AppNavState>(AppNavState.ShiftReportState.Reports)
            }

            var previewReport by remember { mutableStateOf<ShiftReport?>(null) }
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

            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            val scope = rememberCoroutineScope()

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
                            val isRootScreen = navState is AppNavState.ShiftReportState.Reports

                            // Handle back button smoothly to close drawer or return from sub-screens to main view
                            BackHandler(enabled = drawerState.isOpen || !isRootScreen) {
                                if (drawerState.isOpen) {
                                    scope.launch { drawerState.close() }
                                } else if (!isRootScreen) {
                                    focusManager.clearFocus()
                                    navState = AppNavState.ShiftReportState.Reports
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
                                        onSwitchModule = { },
                                        onOpenShopMenu = { },
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
                                        val topBarBg = PrimaryIndigo

                                        val screenTitle = when (val state = navState) {
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
                                                    // Left Navigation Icon: Drawer Hamburger menu icon
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
                                                            fontSize = 18.sp,
                                                            maxLines = 1
                                                        )
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
                                        AppNavigationHost(
                                            navState = navState,
                                            reportViewModel = reportViewModel,
                                            language = currentLanguage,
                                            onPreviewPdf = { report -> previewReport = report }
                                        )

                                        // Shift Report Preview Dialog
                                        previewReport?.let { report ->
                                            ShiftReportPreviewDialog(
                                                report = report,
                                                businessProfile = businessProfile,
                                                language = currentLanguage,
                                                onDismiss = { previewReport = null },
                                                onOpenPrinterSettings = {
                                                    navState = AppNavState.ShiftReportState.SettingsDetail("profile")
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
 * Isolated Navigation Host for Shift Report module.
 */
@Composable
fun AppNavigationHost(
    navState: AppNavState,
    reportViewModel: ReportViewModel,
    language: AppLanguage,
    onPreviewPdf: (ShiftReport) -> Unit,
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
                    onPreviewPdf = onPreviewPdf
                )
            }
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
    language: AppLanguage,
    onPreviewPdf: (ShiftReport) -> Unit
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
                language = language
            )
        }
        is AppNavState.ShiftReportState.SettingsDetail -> {
            LaunchedEffect(destination.section) {
                reportViewModel.selectReportSettingsMenu(destination.section)
            }
            SettingsScreen(
                reportViewModel = reportViewModel,
                activeModule = AppModule.SHIFT_REPORT
            )
        }
    }
}
