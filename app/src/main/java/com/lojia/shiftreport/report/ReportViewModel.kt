package com.lojia.shiftreport.report

import com.lojia.shiftreport.R
import com.lojia.shiftreport.data.*
import com.lojia.shiftreport.util.*
import com.lojia.shiftreport.ui.common.*
import com.lojia.shiftreport.ui.theme.*
import com.lojia.shiftreport.auth.*
import com.lojia.shiftreport.report.*
import com.lojia.shiftreport.settings.*
import com.lojia.shiftreport.sync.*


import android.app.Application


import androidx.lifecycle.AndroidViewModel

import com.lojia.shiftreport.util.UiText



import androidx.lifecycle.viewModelScope

import com.lojia.shiftreport.util.SecurityUtils

import com.lojia.shiftreport.util.NotificationHelper


import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal fun draftFieldText(minor: Long, digits: Int = MoneyFormat.activeDigits): String =
    MoneyFormat.toPlainDecimal(minor, digits)

internal fun draftQtyText(qty: Double): String =
    if (qty <= 0.0) "" else if (qty % 1.0 == 0.0) qty.toInt().toString() else qty.toString()

fun staffAdvanceMinor(amount: Double, digits: Int): Long = MoneyFormat.toMinorUnits(amount, digits)

class ReportViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val db = AppDatabase.getInstance(application)
    private val reportDao = db.reportDao()
    private val employeeDao = db.employeeDao()
    private val savedAddressDao = db.savedAddressDao()
    private val repository = ShiftReportRepository(reportDao)
    private val serialUpdater = SerialUpdater()

    val employees: StateFlow<List<Employee>> = employeeDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedAddresses: StateFlow<List<SavedAddress>> =
        savedAddressDao.getAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shiftReports: StateFlow<List<ShiftReport>> = repository.allReports
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeShiftSession: StateFlow<ShiftSession?> = reportDao.getActiveShiftSession()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allShiftSessions: StateFlow<List<ShiftSession>> = reportDao.getAllShiftSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cashMovements: StateFlow<List<CashMovement>> = reportDao.getAllCashMovements()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Centralized Configuration Sync Manager
    val syncManager = ConfigurationSyncManager.getInstance(application)
    val preferencesRepository = PreferencesRepository.getInstance(application)
    private val prefs = application.getSharedPreferences("lojia_app_prefs", android.content.Context.MODE_PRIVATE)

    val cashiers: StateFlow<List<Cashier>> = syncManager.cashiers

    val businessProfile: StateFlow<BusinessProfile?> = reportDao.getBusinessProfile()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val userProfile: StateFlow<UserProfile?> = reportDao.getUserProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val receiptConfig: StateFlow<ShopReceiptConfig?> = reportDao.getReceiptConfig()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val auditLogs: StateFlow<List<AuditLog>> = reportDao.getAllAuditLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Centralized Synchronized Language State
    val currentLanguage: StateFlow<AppLanguage> = syncManager.currentLanguage

    // Centralized Synchronized Country & Currency State
    val currentCountry: StateFlow<AppCountry> = syncManager.currentCountry
    val currentCurrency: StateFlow<String> = syncManager.currentCurrency
    val isCurrencyLocked: StateFlow<Boolean> = syncManager.isCurrencyLocked

    init {
        MoneyFormat.activeDigits = MoneyFormat.minorUnitDigits(syncManager.currentCurrency.value)
        viewModelScope.launch {
            syncManager.currentCurrency.collect { curr ->
                MoneyFormat.activeDigits = MoneyFormat.minorUnitDigits(curr)
            }
        }
    }

    fun setLanguage(language: AppLanguage) {
        syncManager.setLanguage(language)
        viewModelScope.launch {
            _uiMessage.emit(UiText.StringResource(R.string.toast_lang_changed, language.displayName))
        }
    }

    fun setCountry(country: AppCountry) {
        viewModelScope.launch {
            val result = syncManager.setCountry(country)
            if (result.isSuccess) {
                _uiMessage.emit(
                    UiText.StringResource(
                        R.string.toast_country_set,
                        country.displayNameEn,
                        country.currencyCode,
                        country.currencySymbol
                    )
                )
            } else {
                _uiMessage.emit(UiText.StringResource(R.string.currency_locked_message))
            }
        }
    }

    fun setCurrency(currencyCode: String) {
        viewModelScope.launch {
            val result = syncManager.setCurrency(currencyCode)
            if (result.isSuccess) {
                _uiMessage.emit(UiText.StringResource(R.string.toast_currency_set, currencyCode.trim().uppercase(Locale.US)))
            } else {
                _uiMessage.emit(UiText.StringResource(R.string.currency_locked_message))
            }
        }
    }

    // Centralized Dual Module System State (SHOPPING or SHIFT_REPORT)
    val currentModule: StateFlow<AppModule> = syncManager.currentModule

    // Active Cashier Selection State
    val activeCashier: StateFlow<String> = syncManager.activeCashier
    val syncState: StateFlow<ConfigurationSyncState> = syncManager.syncState

    fun setActiveCashier(cashierName: String) {
        syncManager.setActiveCashier(cashierName)
        selectedCashier.value = cashierName
        viewModelScope.launch {
            _uiMessage.emit(UiText.StringResource(R.string.toast_active_cashier_set, cashierName))
        }
    }

    fun forceSyncConfiguration() {
        syncManager.forceSync { _, msg ->
            viewModelScope.launch {
                _uiMessage.emit(UiText.DynamicString(msg))
            }
        }
    }

    suspend fun exportConfigJson(): String {
        return syncManager.exportConfigurationJson()
    }

    suspend fun importConfigJson(json: String): Boolean {
        val success = syncManager.importConfigurationJson(json)
        if (success) {
            _uiMessage.emit(UiText.StringResource(R.string.configuration_successfully_restored_and))
        } else {
            _uiMessage.emit(UiText.StringResource(R.string.failed_to_import_configuration))
        }
        return success
    }

    // Active Shift Report Settings Menu Key (profile, security, backup, language, about, support, switch_module)
    private val _selectedReportSettingsMenu = MutableStateFlow<String?>("profile")
    val selectedReportSettingsMenu: StateFlow<String?> = _selectedReportSettingsMenu.asStateFlow()

    fun selectReportSettingsMenu(menuKey: String?) {
        _selectedReportSettingsMenu.value = menuKey
    }

    fun switchModule(module: AppModule) {
        syncManager.switchModule(module)
        viewModelScope.launch {
            _uiMessage.emit(UiText.StringResource(R.string.toast_switched_module, module.name))
        }
    }

    // Role-Based Access Control State
    val currentRole: StateFlow<String> = userProfile.map { it?.currentRole ?: "ADMIN" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "ADMIN")

    fun switchUserRole(
        role: String,
        enteredPin: String? = null,
        authorizedByAdmin: Boolean = false,
        onSuccess: () -> Unit = {},
        onFailure: () -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val profile = userProfile.value ?: UserProfile()
            if (role == "ADMIN" && profile.currentRole == "CASHIER") {
                // Switching from Cashier to Admin requires Admin PIN check or AdminAuthDialog verification
                if (authorizedByAdmin || (isPinUsable(enteredPin) && isPinUsable(profile.pin) &&
                    SecurityUtils.verifySecret(enteredPin.orEmpty(), profile.pin))
                ) {
                    reportDao.saveUserProfile(profile.copy(currentRole = "ADMIN"))
                    _uiMessage.emit(UiText.StringResource(R.string.authenticated_as_administrator))
                    withContext(Dispatchers.Main) { onSuccess() }
                } else {
                    _uiMessage.emit(UiText.StringResource(R.string.invalid_admin_pin))
                    withContext(Dispatchers.Main) { onFailure() }
                }
            } else {
                reportDao.saveUserProfile(profile.copy(currentRole = role))
                _uiMessage.emit(UiText.StringResource(R.string.toast_role_switched, role))
                withContext(Dispatchers.Main) { onSuccess() }
            }
        }
    }

    var googleAccount: MutableStateFlow<String> = MutableStateFlow(
        prefs.getString("google_account", "Not Connected") ?: "Not Connected"
    )

    val backupManager = ReportBackupManager(
        application = getApplication(),
        scope = viewModelScope,
        db = db,
        reportDao = reportDao,
        syncManager = syncManager,
        prefs = prefs,
        googleAccount = googleAccount,
        emitUiMessage = { _uiMessage.emit(it) }
    )

    // Backup State & Progress
    val isBackingUp: StateFlow<Boolean> = backupManager.isBackingUp
    val backupProgress: StateFlow<Float> = backupManager.backupProgress

    // Google Drive Cloud Backup State
    val driveAccountInfo: StateFlow<DriveAccountInfo> = backupManager.driveAccountInfo
    val isDriveUploading: StateFlow<Boolean> = backupManager.isDriveUploading
    val isDriveDownloading: StateFlow<Boolean> = backupManager.isDriveDownloading
    val isDriveLoadingList: StateFlow<Boolean> = backupManager.isDriveLoadingList
    val driveBackupsList: StateFlow<List<DriveBackupItem>> = backupManager.driveBackupsList
    val firebaseSyncState: StateFlow<com.lojia.shiftreport.sync.FirebaseSyncStatusState> = backupManager.firebaseSyncState

    val autoBackupFrequency: MutableStateFlow<String> get() = backupManager.autoBackupFrequency
    val backupUsingCellular: MutableStateFlow<Boolean> get() = backupManager.backupUsingCellular
    val lastBackupTime: MutableStateFlow<Long> get() = backupManager.lastBackupTime
    val lastServerSyncTime: MutableStateFlow<Long> get() = backupManager.lastServerSyncTime
    val lastServerSyncStatus: MutableStateFlow<String> get() = backupManager.lastServerSyncStatus

    fun refreshDriveAccount() = backupManager.refreshDriveAccount()
    fun connectGoogleDrive(activityContext: android.content.Context) = backupManager.connectGoogleDrive(activityContext)
    fun disconnectGoogleDrive() = backupManager.disconnectGoogleDrive()
    fun handleDriveAuthRedirect(uri: android.net.Uri, state: String? = null, onComplete: (Boolean) -> Unit = {}) =
        backupManager.handleDriveAuthRedirect(uri, state, onComplete)
    fun backupToGoogleDrive(onSuccess: (DriveBackupItem) -> Unit = {}, onFailure: (String) -> Unit = {}) =
        backupManager.backupToGoogleDrive(onSuccess, onFailure)
    fun fetchGoogleDriveBackups(onSuccess: (List<DriveBackupItem>) -> Unit = {}, onFailure: (String) -> Unit = {}) =
        backupManager.fetchGoogleDriveBackups(onSuccess, onFailure)
    fun prepareGoogleDriveRestore(
        item: DriveBackupItem,
        onReady: (bytes: ByteArray, requiresPassword: Boolean) -> Unit,
        onFailure: (String) -> Unit = {}
    ) = backupManager.prepareGoogleDriveRestore(item, onReady, onFailure)

    fun restoreGoogleDriveBytes(
        item: DriveBackupItem,
        bytes: ByteArray,
        password: String? = null,
        onSuccess: (Int) -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) = backupManager.restoreGoogleDriveBytes(item, bytes, password, onSuccess, onFailure)

    fun restoreFromGoogleDrive(
        item: DriveBackupItem,
        password: String? = null,
        onSuccess: (Int) -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) = backupManager.restoreFromGoogleDrive(item, password, onSuccess, onFailure)

    fun parseDriveBackupSummary(item: DriveBackupItem, onResult: (BackupSummary?) -> Unit) =
        backupManager.parseDriveBackupSummary(item, onResult)
    fun toggleFirebaseCloudSync(enabled: Boolean) = backupManager.toggleFirebaseCloudSync(enabled)
    fun syncFirebaseNow(onSuccess: () -> Unit = {}, onFailure: (String) -> Unit = {}) =
        backupManager.syncFirebaseNow(onSuccess, onFailure)
    fun updateFirebaseCustomUrl(url: String) = backupManager.updateFirebaseCustomUrl(url)
    fun triggerServerSyncNow() = backupManager.triggerServerSyncNow()
    fun updatePeriodicSyncInterval(intervalMinutes: Long, wifiOnly: Boolean = false) =
        backupManager.updatePeriodicSyncInterval(intervalMinutes, wifiOnly)
    fun triggerCloudBackup() = backupManager.triggerCloudBackup()
    fun createOfflineBackup(onSuccess: (BackupInfo) -> Unit = {}, onFailure: (String) -> Unit = {}) =
        backupManager.createOfflineBackup(onSuccess, onFailure)
    fun parseBackupSummary(uri: android.net.Uri, onResult: (BackupSummary?) -> Unit) =
        backupManager.parseBackupSummary(uri, onResult)
    fun restoreOfflineBackup(
        uri: android.net.Uri,
        password: String? = null,
        onSuccess: (Int) -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) = backupManager.restoreOfflineBackup(uri, password, onSuccess, onFailure)
    fun clearAllBusinessData(onSuccess: () -> Unit = {}, onFailure: (String) -> Unit = {}) =
        backupManager.clearAllBusinessData(onSuccess, onFailure)

    // Shift Form State
    var selectedCashier = MutableStateFlow("")
    var selectedShift = MutableStateFlow("Day")
    var selectedDateInMillis = MutableStateFlow(System.currentTimeMillis())
    var openingCashInput = MutableStateFlow("")
    var closingCashInput = MutableStateFlow("")
    var grossCashInput = MutableStateFlow("")
    var madaPaymentsInput = MutableStateFlow("")
    var digitalWalletInput = MutableStateFlow("")
    var totalDiscountsInput = MutableStateFlow("")
    var salesReturnsInput = MutableStateFlow("")
    var staffMealsCountInput = MutableStateFlow("0")
    var totalExpensesInput = MutableStateFlow("")
    var muasselQtyInput = MutableStateFlow("")
    var outdoorShishaQtyInput = MutableStateFlow("")
    var notesInput = MutableStateFlow("")

    var startingCashInput = MutableStateFlow("")
    var actualCashCountInput = MutableStateFlow("")

    // Manager Sign-Off State
    var isShiftLocked = MutableStateFlow(false)
    var managerSignedBy = MutableStateFlow<String?>(null)
    var managerSignTime = MutableStateFlow<Long?>(null)

    val openingCash: StateFlow<Double> = combine(openingCashInput, startingCashInput) { open, start ->
        MoneyFormat.parseMoneyInput(open)?.let { MoneyFormat.toMajorUnits(it) }
            ?: MoneyFormat.parseMoneyInput(start)?.let { MoneyFormat.toMajorUnits(it) }
            ?: 0.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val closingCash: StateFlow<Double> = combine(closingCashInput, actualCashCountInput) { close, actual ->
        MoneyFormat.parseMoneyInput(close)?.let { MoneyFormat.toMajorUnits(it) }
            ?: MoneyFormat.parseMoneyInput(actual)?.let { MoneyFormat.toMajorUnits(it) }
            ?: 0.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalDiscounts: StateFlow<Double> = totalDiscountsInput
        .map { MoneyFormat.parseMoneyInput(it)?.let { m -> MoneyFormat.toMajorUnits(m) } ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val salesReturns: StateFlow<Double> = salesReturnsInput
        .map { MoneyFormat.parseMoneyInput(it)?.let { m -> MoneyFormat.toMajorUnits(m) } ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val startingCash: StateFlow<Double> = openingCash
    val actualCashCount: StateFlow<Double> = closingCash
    val isActualCashEntered: StateFlow<Boolean> = closingCashInput.combine(actualCashCountInput) { close, actual ->
        (close.isNotBlank() && MoneyFormat.parseMoneyInput(close) != null) ||
            (actual.isNotBlank() && MoneyFormat.parseMoneyInput(actual) != null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Dynamic Section Lists
    val dueCreditItems = MutableStateFlow<List<DueCreditItem>>(emptyList())
    val previousDueCollections = MutableStateFlow<List<PreviousDueCollectionItem>>(emptyList())
    val staffAdvances = MutableStateFlow<List<StaffAdvanceItem>>(emptyList())
    val unpaidBills = MutableStateFlow<List<UnpaidBillItem>>(emptyList())
    val purchasedItems = MutableStateFlow<List<PurchasedInventoryItem>>(emptyList())

    fun isValidDecimal(input: String): Boolean {
        if (input.isBlank()) return true
        return MoneyFormat.parseMoneyInput(input) != null
    }

    fun isValidInteger(input: String): Boolean {
        if (input.isBlank()) return true
        val value = input.toIntOrNull()
        return value != null && value >= 0
    }

    val isGrossCashValid: StateFlow<Boolean> = grossCashInput.map { isValidDecimal(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isMadaValid: StateFlow<Boolean> = madaPaymentsInput.map { isValidDecimal(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isDigitalWalletValid: StateFlow<Boolean> = digitalWalletInput.map { isValidDecimal(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isExpensesValid: StateFlow<Boolean> = totalExpensesInput.map { isValidDecimal(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isStaffMealsValid: StateFlow<Boolean> = staffMealsCountInput.map { isValidInteger(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isMuasselQtyValid: StateFlow<Boolean> = muasselQtyInput.map { isValidDecimal(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isOutdoorShishaValid: StateFlow<Boolean> = outdoorShishaQtyInput.map { isValidDecimal(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val grossCash = grossCashInput.map { MoneyFormat.parseMoneyInput(it)?.let { m -> MoneyFormat.toMajorUnits(m) } ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val madaPayments = madaPaymentsInput.map { MoneyFormat.parseMoneyInput(it)?.let { m -> MoneyFormat.toMajorUnits(m) } ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val digitalWallet = digitalWalletInput.map { MoneyFormat.parseMoneyInput(it)?.let { m -> MoneyFormat.toMajorUnits(m) } ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val totalExpenses = totalExpensesInput.map { MoneyFormat.parseMoneyInput(it)?.let { m -> MoneyFormat.toMajorUnits(m) } ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Dynamic Lists Sums
    val totalDueCreditAmount: StateFlow<Double> = dueCreditItems.map { list ->
        MoneyFormat.toMajorUnits(list.sumOf { it.amount })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalPreviousDueCollected: StateFlow<Double> = previousDueCollections.map { list ->
        MoneyFormat.toMajorUnits(list.sumOf { it.amount })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalPreviousDueCash: StateFlow<Double> = previousDueCollections.map { list ->
        MoneyFormat.toMajorUnits(list.filter { it.paymentMode.equals("CASH", ignoreCase = true) }.sumOf { it.amount })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalPreviousDueCard: StateFlow<Double> = previousDueCollections.map { list ->
        MoneyFormat.toMajorUnits(list.filter { !it.paymentMode.equals("CASH", ignoreCase = true) }.sumOf { it.amount })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalStaffAdvancesCash: StateFlow<Double> = staffAdvances.map { list ->
        MoneyFormat.toMajorUnits(list.filter { it.paymentMode.equals("CASH", ignoreCase = true) }.sumOf { it.amount })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalStaffAdvancesBank: StateFlow<Double> = staffAdvances.map { list ->
        MoneyFormat.toMajorUnits(list.filter { !it.paymentMode.equals("CASH", ignoreCase = true) }.sumOf { it.amount })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalStaffAdvances: StateFlow<Double> = staffAdvances.map { list ->
        MoneyFormat.toMajorUnits(list.sumOf { it.amount })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalUnpaidLoss: StateFlow<Double> = unpaidBills.map { list ->
        MoneyFormat.toMajorUnits(list.sumOf { it.amount })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalPurchasedCash: StateFlow<Double> = purchasedItems.map { list ->
        MoneyFormat.toMajorUnits(list.filter { it.paidVia.equals("CASH", ignoreCase = true) }.sumOf { it.totalAmount })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalPurchasedAll: StateFlow<Double> = purchasedItems.map { list ->
        MoneyFormat.toMajorUnits(list.sumOf { it.totalAmount })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Automatic Live Calculations following International Standards
    // 1. Total Sales (payment methods only: Cash + Card/Mada + Digital Wallet)
    val totalSales: StateFlow<Double> = combine(
        grossCash,
        madaPayments,
        digitalWallet
    ) { cash, mada, wallet ->
        val digits = MoneyFormat.minorUnitDigits(currentCurrency.value)
        val sumMinor = MoneyFormat.addMinor(
            MoneyFormat.toMinorUnits(cash, digits),
            MoneyFormat.toMinorUnits(mada, digits),
            MoneyFormat.toMinorUnits(wallet, digits)
        )
        MoneyFormat.toMajorUnits(sumMinor, digits)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val calculatedTotalGrossSales: StateFlow<Double> = totalSales
    val totalRevenue: StateFlow<Double> = totalSales

    // Net Sales = Total Sales - Discounts - Returns
    val netSales: StateFlow<Double> = combine(
        totalSales,
        totalDiscounts,
        salesReturns
    ) { sales, disc, ret ->
        val digits = MoneyFormat.minorUnitDigits(currentCurrency.value)
        val s = MoneyFormat.toMinorUnits(sales, digits)
        val d = MoneyFormat.toMinorUnits(disc, digits)
        val r = MoneyFormat.toMinorUnits(ret, digits)
        val netMinor = MoneyFormat.calculateNetSalesMinor(s, d, r)
        MoneyFormat.toMajorUnits(netMinor, digits)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // 2. Cash In & Outflows
    val totalCashIn: StateFlow<Double> = combine(
        openingCash,
        grossCash,
        totalPreviousDueCash
    ) { open, cashSales, prevDueCash ->
        val digits = MoneyFormat.minorUnitDigits(currentCurrency.value)
        val sumMinor = MoneyFormat.addMinor(
            MoneyFormat.toMinorUnits(open, digits),
            MoneyFormat.toMinorUnits(cashSales, digits),
            MoneyFormat.toMinorUnits(prevDueCash, digits)
        )
        MoneyFormat.toMajorUnits(sumMinor, digits)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalCashOut: StateFlow<Double> = combine(
        totalExpenses,
        totalStaffAdvancesCash,
        totalPurchasedCash
    ) { exp, advCash, purchCash ->
        val digits = MoneyFormat.minorUnitDigits(currentCurrency.value)
        val sumMinor = MoneyFormat.addMinor(
            MoneyFormat.toMinorUnits(exp, digits),
            MoneyFormat.toMinorUnits(advCash, digits),
            MoneyFormat.toMinorUnits(purchCash, digits)
        )
        MoneyFormat.toMajorUnits(sumMinor, digits)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val calculatedTotalExpenses: StateFlow<Double> = totalCashOut

    // 3. Expected Cash in Drawer & Net Cash
    // Note: totalCashIn already includes openingCash (start float)
    val expectedCashInDrawer: StateFlow<Double> = combine(
        totalCashIn,
        totalCashOut
    ) { cin, cout ->
        val digits = MoneyFormat.minorUnitDigits(currentCurrency.value)
        val expectedMinor = MoneyFormat.calculateExpectedCashMinor(
            totalCashIn = MoneyFormat.toMinorUnits(cin, digits),
            totalCashOut = MoneyFormat.toMinorUnits(cout, digits)
        )
        MoneyFormat.toMajorUnits(expectedMinor, digits)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val calculatedCashInDrawer: StateFlow<Double> = expectedCashInDrawer
    val netCash: StateFlow<Double> = expectedCashInDrawer

    // 4. Over / Short Cash Variance
    val cashVariance: StateFlow<Double> = combine(
        actualCashCount,
        expectedCashInDrawer
    ) { actual, expected ->
        val digits = MoneyFormat.minorUnitDigits(currentCurrency.value)
        val varMinor = MoneyFormat.calculateVarianceMinor(
            MoneyFormat.toMinorUnits(actual, digits),
            MoneyFormat.toMinorUnits(expected, digits)
        )
        MoneyFormat.toMajorUnits(varMinor, digits)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val calculatedCardAndDigital: StateFlow<Double> = combine(
        madaPayments,
        totalPreviousDueCard,
        digitalWallet
    ) { mada, prevCard, wallet ->
        val digits = MoneyFormat.minorUnitDigits(currentCurrency.value)
        val sumMinor = MoneyFormat.addMinor(
            MoneyFormat.toMinorUnits(mada, digits),
            MoneyFormat.toMinorUnits(prevCard, digits),
            MoneyFormat.toMinorUnits(wallet, digits)
        )
        MoneyFormat.toMajorUnits(sumMinor, digits)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val calculatedNetBalance: StateFlow<Double> = combine(
        totalSales,
        totalCashOut,
        totalUnpaidLoss
    ) { sales, exp, unpaid ->
        val digits = MoneyFormat.minorUnitDigits(currentCurrency.value)
        val s = MoneyFormat.toMinorUnits(sales, digits)
        val e = MoneyFormat.toMinorUnits(exp, digits)
        val u = MoneyFormat.toMinorUnits(unpaid, digits)
        MoneyFormat.toMajorUnits(s - e - u, digits)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    private val _uiMessage = MutableSharedFlow<com.lojia.shiftreport.util.UiText>()
    val uiMessage = _uiMessage.asSharedFlow()

    private val _draftSaveError = MutableSharedFlow<String>()
    val draftSaveError = _draftSaveError.asSharedFlow()

    private val _showDraftRestoredNotice = MutableStateFlow(false)
    val showDraftRestoredNotice: StateFlow<Boolean> = _showDraftRestoredNotice.asStateFlow()

    private val draftMutex = Mutex()
    private var lastSavedSnapshot: DraftFormSnapshot? = null
    private var autosaveJob: Job? = null
    @Volatile
    private var isRestoringDraft = false
    private var hasShownInitialRestoreNotice = false

    private data class DraftMetaSnapshot(
        val cashier: String,
        val shift: String,
        val dateInMillis: Long,
        val notes: String,
        val staffMealsCount: String,
        val muasselQty: String,
        val outdoorShishaQty: String
    )

    private data class DraftAmountsASnapshot(
        val openingCash: String,
        val startingCash: String,
        val closingCash: String,
        val actualCashCount: String,
        val grossCash: String
    )

    private data class DraftAmountsBSnapshot(
        val madaPayments: String,
        val digitalWallet: String,
        val totalDiscounts: String,
        val salesReturns: String,
        val totalExpenses: String
    )

    private data class DraftListsSnapshot(
        val dueCredits: List<DueCreditItem>,
        val previousDues: List<PreviousDueCollectionItem>,
        val staffAdvances: List<StaffAdvanceItem>,
        val unpaidBills: List<UnpaidBillItem>,
        val purchasedItems: List<PurchasedInventoryItem>
    )

    data class DraftFormSnapshot(
        val cashierName: String,
        val shift: String,
        val dateInMillis: Long,
        val openingCashInput: String,
        val startingCashInput: String,
        val closingCashInput: String,
        val actualCashCountInput: String,
        val grossCashInput: String,
        val madaPaymentsInput: String,
        val digitalWalletInput: String,
        val totalDiscountsInput: String,
        val salesReturnsInput: String,
        val staffMealsCountInput: String,
        val totalExpensesInput: String,
        val muasselQtyInput: String,
        val outdoorShishaQtyInput: String,
        val notesInput: String,
        val dueCreditItems: List<DueCreditItem>,
        val previousDueCollections: List<PreviousDueCollectionItem>,
        val staffAdvances: List<StaffAdvanceItem>,
        val unpaidBills: List<UnpaidBillItem>,
        val purchasedItems: List<PurchasedInventoryItem>
    ) {
        fun isEmpty(): Boolean =
            cashierName.isBlank() &&
                openingCashInput.isBlank() &&
                startingCashInput.isBlank() &&
                closingCashInput.isBlank() &&
                actualCashCountInput.isBlank() &&
                grossCashInput.isBlank() &&
                madaPaymentsInput.isBlank() &&
                digitalWalletInput.isBlank() &&
                totalDiscountsInput.isBlank() &&
                salesReturnsInput.isBlank() &&
                (staffMealsCountInput.toIntOrNull() ?: 0) == 0 &&
                totalExpensesInput.isBlank() &&
                muasselQtyInput.isBlank() &&
                outdoorShishaQtyInput.isBlank() &&
                notesInput.isBlank() &&
                dueCreditItems.isEmpty() &&
                previousDueCollections.isEmpty() &&
                staffAdvances.isEmpty() &&
                unpaidBills.isEmpty() &&
                purchasedItems.isEmpty()

        fun toShiftFormState(): ShiftFormState = ShiftFormState(
            cashier = cashierName,
            shift = shift,
            dateMillis = dateInMillis,
            startingCashInput = openingCashInput,
            cashReceipts = grossCashInput,
            madaPayments = madaPaymentsInput,
            digitalWalletInput = digitalWalletInput,
            totalDiscountsInput = totalDiscountsInput,
            salesReturnsInput = salesReturnsInput,
            actualCashCountInput = closingCashInput,
            notesInput = notesInput,
            staffCount = staffMealsCountInput.toIntOrNull() ?: 0,
            totalExpenses = totalExpensesInput,
            muassel = muasselQtyInput,
            outdoorMuassel = outdoorShishaQtyInput,
            dueCreditItems = dueCreditItems,
            previousDueCollections = previousDueCollections,
            staffAdvances = staffAdvances,
            unpaidBills = unpaidBills,
            purchasedItems = purchasedItems
        )
    }

    private fun currentFormSnapshot(): DraftFormSnapshot = DraftFormSnapshot(
        cashierName = selectedCashier.value,
        shift = selectedShift.value,
        dateInMillis = selectedDateInMillis.value,
        openingCashInput = openingCashInput.value,
        startingCashInput = startingCashInput.value,
        closingCashInput = closingCashInput.value,
        actualCashCountInput = actualCashCountInput.value,
        grossCashInput = grossCashInput.value,
        madaPaymentsInput = madaPaymentsInput.value,
        digitalWalletInput = digitalWalletInput.value,
        totalDiscountsInput = totalDiscountsInput.value,
        salesReturnsInput = salesReturnsInput.value,
        staffMealsCountInput = staffMealsCountInput.value,
        totalExpensesInput = totalExpensesInput.value,
        muasselQtyInput = muasselQtyInput.value,
        outdoorShishaQtyInput = outdoorShishaQtyInput.value,
        notesInput = notesInput.value,
        dueCreditItems = dueCreditItems.value,
        previousDueCollections = previousDueCollections.value,
        staffAdvances = staffAdvances.value,
        unpaidBills = unpaidBills.value,
        purchasedItems = purchasedItems.value
    )

    private fun formSnapshotFlow(): Flow<DraftFormSnapshot> {
        val metaBasic = combine(
            selectedCashier,
            selectedShift,
            selectedDateInMillis,
            notesInput
        ) { cashier, shift, date, notes ->
            DraftMetaSnapshot(
                cashier = cashier,
                shift = shift,
                dateInMillis = date,
                notes = notes,
                staffMealsCount = "0",
                muasselQty = "",
                outdoorShishaQty = ""
            )
        }
        val metaFull = combine(
            metaBasic,
            staffMealsCountInput,
            muasselQtyInput,
            outdoorShishaQtyInput
        ) { meta, meals, muassel, outdoor ->
            meta.copy(
                staffMealsCount = meals,
                muasselQty = muassel,
                outdoorShishaQty = outdoor
            )
        }
        val amountsA = combine(
            openingCashInput,
            startingCashInput,
            closingCashInput,
            actualCashCountInput,
            grossCashInput
        ) { open, start, close, actual, gross ->
            DraftAmountsASnapshot(open, start, close, actual, gross)
        }
        val amountsB = combine(
            madaPaymentsInput,
            digitalWalletInput,
            totalDiscountsInput,
            salesReturnsInput,
            totalExpensesInput
        ) { mada, wallet, discounts, returns, expenses ->
            DraftAmountsBSnapshot(mada, wallet, discounts, returns, expenses)
        }
        val lists = combine(
            dueCreditItems,
            previousDueCollections,
            staffAdvances,
            unpaidBills,
            purchasedItems
        ) { credits, prevDues, advances, unpaid, purchased ->
            DraftListsSnapshot(credits, prevDues, advances, unpaid, purchased)
        }
        return combine(metaFull, amountsA, amountsB, lists) { meta, a, b, l ->
            DraftFormSnapshot(
                cashierName = meta.cashier,
                shift = meta.shift,
                dateInMillis = meta.dateInMillis,
                openingCashInput = a.openingCash,
                startingCashInput = a.startingCash,
                closingCashInput = a.closingCash,
                actualCashCountInput = a.actualCashCount,
                grossCashInput = a.grossCash,
                madaPaymentsInput = b.madaPayments,
                digitalWalletInput = b.digitalWallet,
                totalDiscountsInput = b.totalDiscounts,
                salesReturnsInput = b.salesReturns,
                staffMealsCountInput = meta.staffMealsCount,
                totalExpensesInput = b.totalExpenses,
                muasselQtyInput = meta.muasselQty,
                outdoorShishaQtyInput = meta.outdoorShishaQty,
                notesInput = meta.notes,
                dueCreditItems = l.dueCredits,
                previousDueCollections = l.previousDues,
                staffAdvances = l.staffAdvances,
                unpaidBills = l.unpaidBills,
                purchasedItems = l.purchasedItems
            )
        }
    }

    val draftFormState: StateFlow<ShiftFormState> = formSnapshotFlow()
        .map { it.toShiftFormState() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ShiftFormState())

    fun consumeDraftRestoredNotice() {
        _showDraftRestoredNotice.value = false
    }

    fun discardRestoredDraft() {
        _showDraftRestoredNotice.value = false
        clearForm()
    }

    // Add / Remove Handlers for Dynamic Sections
    fun addDueCreditItem(
        customerName: String,
        amount: Double,
        digits: Int = MoneyFormat.minorUnitDigits(currentCurrency.value),
        note: String = "",
        phone: String = ""
    ) {
        if (amount <= 0) return
        dueCreditItems.value = dueCreditItems.value + DueCreditItem(
            customerName = customerName.trim(),
            amount = MoneyFormat.toMinorUnits(amount, digits),
            note = note.trim(),
            phone = phone.trim()
        )
    }

    fun removeDueCreditItem(id: String) {
        dueCreditItems.value = dueCreditItems.value.filter { it.id != id }
    }

    fun addPreviousDueCollection(
        customerName: String,
        amount: Double,
        digits: Int = MoneyFormat.minorUnitDigits(currentCurrency.value),
        paymentMode: String = "CASH",
        note: String = ""
    ) {
        if (amount <= 0) return
        previousDueCollections.value = previousDueCollections.value + PreviousDueCollectionItem(
            customerName = customerName.trim(),
            amount = MoneyFormat.toMinorUnits(amount, digits),
            paymentMode = paymentMode,
            note = note.trim()
        )
    }

    fun removePreviousDueCollection(id: String) {
        previousDueCollections.value = previousDueCollections.value.filter { it.id != id }
    }

    fun addStaffAdvance(
        staffName: String,
        amount: Double,
        digits: Int,
        reason: String = "",
        paymentMode: String = "CASH",
        employeeId: String? = null
    ) {
        if (staffName.isBlank() || amount <= 0) return
        staffAdvances.value = staffAdvances.value + StaffAdvanceItem(
            staffName = staffName.trim(),
            amount = staffAdvanceMinor(amount, digits),
            reason = reason.trim(),
            paymentMode = paymentMode,
            employeeId = employeeId?.trim()?.ifBlank { null }
        )
    }

    fun removeStaffAdvance(id: String) {
        staffAdvances.value = staffAdvances.value.filter { it.id != id }
    }

    fun clearStaffAdvances() {
        staffAdvances.value = emptyList()
    }

    fun addUnpaidBill(
        tableOrOrderRef: String,
        amount: Double,
        digits: Int = MoneyFormat.minorUnitDigits(currentCurrency.value),
        reason: String = ""
    ) {
        if (tableOrOrderRef.isBlank() || amount <= 0) return
        unpaidBills.value = unpaidBills.value + UnpaidBillItem(
            tableOrOrderRef = tableOrOrderRef.trim(),
            amount = MoneyFormat.toMinorUnits(amount, digits),
            reason = reason.trim()
        )
    }

    fun removeUnpaidBill(id: String) {
        unpaidBills.value = unpaidBills.value.filter { it.id != id }
    }

    fun addPurchasedItem(
        itemName: String,
        quantity: Double = 0.0,
        unitPrice: Double = 0.0,
        paidVia: String = "CASH",
        supplier: String = "",
        totalAmount: Double = 0.0,
        digits: Int = MoneyFormat.minorUnitDigits(currentCurrency.value)
    ) {
        if (itemName.isBlank() || totalAmount <= 0) return
        val resolvedUnitPrice = if (unitPrice > 0) unitPrice else if (quantity > 0) (totalAmount / quantity) else 0.0
        purchasedItems.value = purchasedItems.value + PurchasedInventoryItem(
            itemName = itemName.trim(),
            quantity = quantity,
            unitPrice = MoneyFormat.toMinorUnits(resolvedUnitPrice, digits),
            totalAmount = MoneyFormat.toMinorUnits(totalAmount, digits),
            paidVia = paidVia,
            supplier = supplier.trim()
        )
    }

    fun removePurchasedItem(id: String) {
        purchasedItems.value = purchasedItems.value.filter { it.id != id }
    }

    init {
        loadDraft()
        FirebaseCloudSyncManager.init(context)
        if (com.lojia.shiftreport.BuildConfig.CLOUD_SYNC_ENABLED && FirebaseCloudSyncManager.isSyncEnabled(context)) {
            FirebaseCloudSyncScheduler.schedulePeriodicSync(context)
        }
        viewModelScope.launch {
            userProfile.collect { profile ->
                if (profile != null) {
                    preferencesRepository.syncWithUserProfile(profile)
                }
            }
        }
    }

    fun serializeDueCredits(list: List<DueCreditItem>): String =
        ShiftReportSerializer.serializeDueCredits(list)

    fun deserializeDueCredits(json: String): List<DueCreditItem> =
        ShiftReportSerializer.deserializeDueCredits(json)

    fun serializePrevDues(list: List<PreviousDueCollectionItem>): String =
        ShiftReportSerializer.serializePrevDues(list)

    fun deserializePrevDues(json: String): List<PreviousDueCollectionItem> =
        ShiftReportSerializer.deserializePrevDues(json)

    fun serializeAdvances(list: List<StaffAdvanceItem>): String =
        ShiftReportSerializer.serializeAdvances(list)

    fun deserializeAdvances(json: String): List<StaffAdvanceItem> =
        ShiftReportSerializer.deserializeAdvances(json)

    fun serializeUnpaid(list: List<UnpaidBillItem>): String =
        ShiftReportSerializer.serializeUnpaid(list)

    fun deserializeUnpaid(json: String): List<UnpaidBillItem> =
        ShiftReportSerializer.deserializeUnpaid(json)

    fun serializePurchased(list: List<PurchasedInventoryItem>): String =
        ShiftReportSerializer.serializePurchased(list)

    fun deserializePurchased(json: String): List<PurchasedInventoryItem> =
        ShiftReportSerializer.deserializePurchased(json)


    @OptIn(FlowPreview::class)
    private fun startDraftAutosave() {
        autosaveJob?.cancel()
        autosaveJob = viewModelScope.launch(Dispatchers.IO) {
            formSnapshotFlow()
                .drop(1)
                .debounce(AppConstants.DRAFT_SAVE_DEBOUNCE_MS)
                .distinctUntilChanged()
                .collect { snapshot ->
                    if (!isRestoringDraft) {
                        saveDraftSilently(snapshot)
                    }
                }
        }
    }

    fun loadDraft() {
        viewModelScope.launch(Dispatchers.IO) {
            isRestoringDraft = true
            try {
                val draft = repository.getDraft()
                if (draft != null) {
                    val digits = MoneyFormat.minorUnitDigits(currentCurrency.value)
                    selectedCashier.value = draft.cashierName
                    selectedShift.value = draft.shift.ifBlank { "Day" }
                    selectedDateInMillis.value = draft.dateInMillis
                    openingCashInput.value = if (draft.openingCash > 0) draftFieldText(draft.openingCash, digits) else ""
                    closingCashInput.value = if (draft.closingCash > 0) draftFieldText(draft.closingCash, digits) else ""
                    grossCashInput.value = if (draft.grossCash > 0) draftFieldText(draft.grossCash, digits) else ""
                    madaPaymentsInput.value = if (draft.madaPayments > 0) draftFieldText(draft.madaPayments, digits) else ""
                    digitalWalletInput.value = if (draft.digitalWallet > 0) draftFieldText(draft.digitalWallet, digits) else ""
                    totalDiscountsInput.value = if (draft.totalDiscounts > 0) draftFieldText(draft.totalDiscounts, digits) else ""
                    salesReturnsInput.value = if (draft.salesReturns > 0) draftFieldText(draft.salesReturns, digits) else ""
                    staffMealsCountInput.value = draft.staffMealsCount.toString()
                    totalExpensesInput.value = if (draft.totalExpenses > 0) draftFieldText(draft.totalExpenses, digits) else ""
                    muasselQtyInput.value = draftQtyText(draft.muasselQty)
                    outdoorShishaQtyInput.value = draftQtyText(draft.outdoorShishaQty)
                    notesInput.value = draft.notes
                    dueCreditItems.value = deserializeDueCredits(draft.dueCreditEntriesJson)
                    previousDueCollections.value = deserializePrevDues(draft.previousDueCollectionsJson)
                    staffAdvances.value = deserializeAdvances(draft.staffAdvancesJson)
                    unpaidBills.value = deserializeUnpaid(draft.unpaidBillsJson)
                    purchasedItems.value = deserializePurchased(draft.purchasedItemsJson)
                    startingCashInput.value = ""
                    actualCashCountInput.value = ""

                    val restoredSnapshot = currentFormSnapshot()
                    draftMutex.withLock {
                        lastSavedSnapshot = restoredSnapshot
                    }
                    if (!hasShownInitialRestoreNotice && !restoredSnapshot.isEmpty()) {
                        hasShownInitialRestoreNotice = true
                        _showDraftRestoredNotice.value = true
                    }
                }
            } finally {
                isRestoringDraft = false
                startDraftAutosave()
            }
        }
    }

    fun validateAll(): Boolean {
        return classifyMoneyInput(openingCashInput.value) != MoneyInput.Invalid &&
                classifyMoneyInput(startingCashInput.value) != MoneyInput.Invalid &&
                classifyMoneyInput(closingCashInput.value) != MoneyInput.Invalid &&
                classifyMoneyInput(actualCashCountInput.value) != MoneyInput.Invalid &&
                classifyMoneyInput(grossCashInput.value) != MoneyInput.Invalid &&
                classifyMoneyInput(madaPaymentsInput.value) != MoneyInput.Invalid &&
                classifyMoneyInput(digitalWalletInput.value) != MoneyInput.Invalid &&
                classifyMoneyInput(totalDiscountsInput.value) != MoneyInput.Invalid &&
                classifyMoneyInput(salesReturnsInput.value) != MoneyInput.Invalid &&
                classifyMoneyInput(totalExpensesInput.value) != MoneyInput.Invalid &&
                isValidInteger(staffMealsCountInput.value) &&
                isValidDecimal(muasselQtyInput.value) &&
                isValidDecimal(outdoorShishaQtyInput.value)
    }

    private fun minorFromValidOrEmpty(raw: String, digits: Int = MoneyFormat.activeDigits): Long =
        when (val classified = classifyMoneyInput(raw, digits)) {
            is MoneyInput.Valid -> classified.minor
            MoneyInput.Empty, MoneyInput.Invalid -> 0L
        }

    private fun majorFromValidOrEmpty(raw: String, digits: Int = MoneyFormat.activeDigits): Double =
        when (val classified = classifyMoneyInput(raw, digits)) {
            is MoneyInput.Valid -> MoneyFormat.toMajorUnits(classified.minor, digits)
            MoneyInput.Empty, MoneyInput.Invalid -> 0.0
        }

    suspend fun saveDraftSilently(snapshot: DraftFormSnapshot = currentFormSnapshot()) {
        draftMutex.withLock {
            val empty = snapshot.isEmpty()
            val changed = snapshot != lastSavedSnapshot
            if (!shouldWriteDraft(isEmpty = empty, snapshotChanged = changed)) {
                return
            }
            val digits = MoneyFormat.minorUnitDigits(currentCurrency.value)
            val openingMinor = (classifyMoneyInput(snapshot.openingCashInput, digits) as? MoneyInput.Valid)?.minor
                ?: minorFromValidOrEmpty(snapshot.startingCashInput, digits)
            val closingMinor = (classifyMoneyInput(snapshot.closingCashInput, digits) as? MoneyInput.Valid)?.minor
                ?: minorFromValidOrEmpty(snapshot.actualCashCountInput, digits)
            val draft = DraftReport(
                id = 1,
                cashierName = snapshot.cashierName,
                shift = snapshot.shift,
                dateInMillis = snapshot.dateInMillis,
                openingCash = openingMinor,
                closingCash = closingMinor,
                grossCash = minorFromValidOrEmpty(snapshot.grossCashInput, digits),
                madaPayments = minorFromValidOrEmpty(snapshot.madaPaymentsInput, digits),
                digitalWallet = minorFromValidOrEmpty(snapshot.digitalWalletInput, digits),
                totalDiscounts = minorFromValidOrEmpty(snapshot.totalDiscountsInput, digits),
                salesReturns = minorFromValidOrEmpty(snapshot.salesReturnsInput, digits),
                staffMealsCount = snapshot.staffMealsCountInput.toIntOrNull() ?: 0,
                totalExpenses = minorFromValidOrEmpty(snapshot.totalExpensesInput, digits),
                muasselQty = snapshot.muasselQtyInput.toDoubleOrNull() ?: 0.0,
                outdoorShishaQty = snapshot.outdoorShishaQtyInput.toDoubleOrNull() ?: 0.0,
                dueCreditEntriesJson = serializeDueCredits(snapshot.dueCreditItems),
                previousDueCollectionsJson = serializePrevDues(snapshot.previousDueCollections),
                staffAdvancesJson = serializeAdvances(snapshot.staffAdvances),
                unpaidBillsJson = serializeUnpaid(snapshot.unpaidBills),
                purchasedItemsJson = serializePurchased(snapshot.purchasedItems),
                notes = snapshot.notesInput
            )
            try {
                repository.saveDraft(draft)
                lastSavedSnapshot = snapshot
            } catch (e: Exception) {
                AppLogger.e("ReportViewModel", "Failed to save draft", e)
                val errorText = e.localizedMessage ?: context.getString(R.string.err_failed_save_draft)
                _draftSaveError.emit(errorText)
                if (e.localizedMessage != null) {
                    _uiMessage.emit(UiText.DynamicString(errorText))
                } else {
                    _uiMessage.emit(UiText.StringResource(R.string.err_failed_save_draft))
                }
            }
        }
    }

    suspend fun flushDraft() {
        autosaveJob?.cancel()
        val snapshot = currentFormSnapshot()
        withContext(Dispatchers.IO + NonCancellable) {
            saveDraftSilently(snapshot)
        }
        startDraftAutosave()
    }

    fun clearForm() {
        selectedCashier.value = ""
        selectedShift.value = "Day"
        selectedDateInMillis.value = System.currentTimeMillis()
        openingCashInput.value = ""
        closingCashInput.value = ""
        grossCashInput.value = ""
        madaPaymentsInput.value = ""
        digitalWalletInput.value = ""
        totalDiscountsInput.value = ""
        salesReturnsInput.value = ""
        staffMealsCountInput.value = "0"
        totalExpensesInput.value = ""
        muasselQtyInput.value = ""
        outdoorShishaQtyInput.value = ""
        notesInput.value = ""
        startingCashInput.value = ""
        actualCashCountInput.value = ""
        isShiftLocked.value = false
        managerSignedBy.value = null
        managerSignTime.value = null
        dueCreditItems.value = emptyList()
        previousDueCollections.value = emptyList()
        staffAdvances.value = emptyList()
        unpaidBills.value = emptyList()
        purchasedItems.value = emptyList()
        _showDraftRestoredNotice.value = false
        viewModelScope.launch(Dispatchers.IO) {
            draftMutex.withLock {
                repository.clearDraft()
                lastSavedSnapshot = null
            }
        }
    }

    fun saveShiftReportDirect(report: ShiftReport, onSuccess: (ShiftReport) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertReport(report)
            draftMutex.withLock {
                repository.clearDraft()
                lastSavedSnapshot = null
            }
            staffAdvances.value = emptyList()
            _showDraftRestoredNotice.value = false
            reportDao.insertAuditLog(
                AuditLog(
                    username = report.cashierName,
                    action = "SUBMIT_REPORT",
                    details = "Submitted ${report.shift} shift report. Total sales: ${report.totalSales}, Net Cash: ${report.netCash}"
                )
            )

            val curr = syncManager.currentCurrency.value
            try {
                NotificationHelper.sendShiftSummaryNotification(
                    getApplication(),
                    report.cashierName,
                    report.shift,
                    MoneyFormat.toMajorUnits(report.totalSales),
                    MoneyFormat.toMajorUnits(report.netCash),
                    curr
                )
            } catch (_: Exception) {}

            _uiMessage.emit(UiText.StringResource(R.string.shift_report_submitted_successfully_1))

            if (preferencesRepository.isPreserveDataOnUninstall()) {
                try {
                    AutoBackupScheduler.enqueueOneTimeAutoBackup(context)
                } catch (_: Exception) {}
            }

            withContext(Dispatchers.Main) { onSuccess(report) }
        }
    }

    fun deleteReport(report: ShiftReport) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteReport(report)
            reportDao.insertAuditLog(
                AuditLog(
                    username = "Admin",
                    action = "DELETE_REPORT",
                    details = "Deleted shift report #${report.id} for ${report.cashierName}"
                )
            )
            _uiMessage.emit(UiText.StringResource(R.string.report_deleted))
        }
    }

    // Shift Drawer Operations (Open Shift / Close Shift / Pay In / Pay Out)
    fun openShift(cashierName: String, shiftName: String, startingCash: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val curr = syncManager.currentCurrency.value
            val digits = MoneyFormat.minorUnitDigits(curr)
            serialUpdater.update {
                val startingCashMinor = MoneyFormat.toMinorUnits(startingCash, digits)
                val newSession = ShiftSession(
                    cashierName = cashierName.ifBlank { "Cashier" },
                    shiftName = shiftName,
                    status = "OPEN",
                    openedAt = System.currentTimeMillis(),
                    startingCash = startingCashMinor,
                    expectedCash = startingCashMinor
                )
                reportDao.insertShiftSession(newSession)
                reportDao.insertAuditLog(
                    AuditLog(
                        username = cashierName.ifBlank { "Cashier" },
                        action = "OPEN_SHIFT",
                        details = "Opened $shiftName shift. Starting Float: $startingCash"
                    )
                )
            }
            NotificationHelper.sendShiftOpenedNotification(context, cashierName, startingCash, curr)
            _uiMessage.emit(
                UiText.StringResource(
                    R.string.toast_shift_opened_cash,
                    MoneyFormat.toPlainDecimal(MoneyFormat.toMinorUnits(startingCash, digits), digits),
                    curr
                )
            )
        }
    }

    fun closeShift(session: ShiftSession, actualCashCount: Double, notes: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val curr = syncManager.currentCurrency.value
            val digits = MoneyFormat.minorUnitDigits(curr)
            val (latestSession, varianceMinor, varianceMajor) = serialUpdater.update {
                val currentSession = reportDao.getActiveShiftSessionOnce()?.takeIf { it.id == session.id } ?: session
                val actualCashCountMinor = MoneyFormat.toMinorUnits(actualCashCount, digits)
                val vMinor = MoneyFormat.calculateVarianceMinor(actualCashCountMinor, currentSession.expectedCash)
                val vMajor = MoneyFormat.toMajorUnits(vMinor, digits)
                val updated = currentSession.copy(
                    status = "CLOSED",
                    closedAt = System.currentTimeMillis(),
                    actualCashCount = actualCashCountMinor,
                    variance = vMinor,
                    notes = notes
                )
                reportDao.updateShiftSession(updated)
                Triple(currentSession, vMinor, vMajor)
            }
            reportDao.insertAuditLog(
                AuditLog(
                    username = latestSession.cashierName,
                    action = "CLOSE_SHIFT",
                    details = "Closed ${latestSession.shiftName} shift. Expected Cash: ${MoneyFormat.toMajorUnits(latestSession.expectedCash)}, Actual Count: $actualCashCount, Variance: $varianceMajor. Notes: $notes"
                )
            )
            if (varianceMinor != 0L) {
                reportDao.insertAuditLog(
                    AuditLog(
                        username = latestSession.cashierName,
                        action = "VARIANCE_RECORDED",
                        details = "Shift Cash Variance: $varianceMajor (${if (varianceMinor > 0) "Over" else "Short"})"
                    )
                )
            }
            NotificationHelper.sendShiftClosedNotification(context, latestSession.cashierName, varianceMajor, curr)
            _uiMessage.emit(
                UiText.StringResource(
                    R.string.toast_shift_closed_variance,
                    MoneyFormat.toPlainDecimal(varianceMinor, digits),
                    curr
                )
            )

            if (preferencesRepository.isPreserveDataOnUninstall()) {
                try {
                    AutoBackupScheduler.enqueueOneTimeAutoBackup(context)
                } catch (_: Exception) {}
            }
        }
    }

    fun closeShiftWithZReport(
        session: ShiftSession,
        actualCashCount: Double,
        notes: String,
        onReportCreated: (ShiftReport) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val curr = syncManager.currentCurrency.value
            val digits = MoneyFormat.minorUnitDigits(curr)
            val (latestSession, actualCashCountMinor, variancePair) = serialUpdater.update {
                val currentSession = reportDao.getActiveShiftSessionOnce()?.takeIf { it.id == session.id } ?: session
                val actualMinor = MoneyFormat.toMinorUnits(actualCashCount, digits)
                val vMinor = MoneyFormat.calculateVarianceMinor(actualMinor, currentSession.expectedCash)
                val vMajor = MoneyFormat.toMajorUnits(vMinor, digits)
                val updated = currentSession.copy(
                    status = "CLOSED",
                    closedAt = System.currentTimeMillis(),
                    actualCashCount = actualMinor,
                    variance = vMinor,
                    notes = notes
                )
                reportDao.updateShiftSession(updated)
                Triple(currentSession, actualMinor, Pair(vMinor, vMajor))
            }
            val (varianceMinor, varianceMajor) = variancePair

            val report = ShiftReport(
                cashierName = latestSession.cashierName,
                shift = latestSession.shiftName,
                dateInMillis = System.currentTimeMillis(),
                openingCash = latestSession.startingCash,
                closingCash = actualCashCountMinor,
                grossCash = latestSession.cashSales,
                madaPayments = latestSession.cardSales,
                digitalWallet = latestSession.digitalSales,
                totalDiscounts = latestSession.totalDiscounts,
                salesReturns = latestSession.salesReturns,
                totalExpenses = latestSession.totalPayOut,
                notes = notes
            )
            val id = repository.insertReport(report)
            val finalReport = report.copy(id = id.toInt())

            reportDao.insertAuditLog(
                AuditLog(
                    username = latestSession.cashierName,
                    action = "CLOSE_SHIFT_Z_REPORT",
                    details = "Closed ${latestSession.shiftName} shift with Z-Report #${finalReport.id}. Total Sales: ${MoneyFormat.toMajorUnits(finalReport.totalSales)}, Expected Cash: ${MoneyFormat.toMajorUnits(latestSession.expectedCash)}, Actual Count: $actualCashCount, Variance: $varianceMajor"
                )
            )

            if (varianceMinor != 0L) {
                reportDao.insertAuditLog(
                    AuditLog(
                        username = latestSession.cashierName,
                        action = "VARIANCE_RECORDED",
                        details = "Z-Report #${finalReport.id} Cash Variance: $varianceMajor (${if (varianceMinor > 0) "Over" else "Short"})"
                    )
                )
            }

            NotificationHelper.sendShiftClosedNotification(context, latestSession.cashierName, varianceMajor, curr)
            _uiMessage.emit(
                UiText.StringResource(
                    R.string.toast_shift_closed_variance,
                    MoneyFormat.toPlainDecimal(varianceMinor, digits),
                    curr
                )
            )

            if (preferencesRepository.isPreserveDataOnUninstall()) {
                try {
                    AutoBackupScheduler.enqueueOneTimeAutoBackup(context)
                } catch (_: Exception) {}
            }

            withContext(Dispatchers.Main) { onReportCreated(finalReport) }
        }
    }

    fun addCashMovement(type: String, amount: Double, reason: String, cashierName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val digits = MoneyFormat.minorUnitDigits(syncManager.currentCurrency.value)
            serialUpdater.update {
                val active = reportDao.getActiveShiftSessionOnce() ?: activeShiftSession.value
                val sessionId = active?.id ?: 0
                val amountMinor = MoneyFormat.toMinorUnits(amount, digits)
                reportDao.insertCashMovement(
                    CashMovement(
                        shiftSessionId = sessionId,
                        type = type,
                        amount = amountMinor,
                        reason = reason,
                        cashierName = cashierName
                    )
                )
                if (active != null) {
                    val newPayIn = if (type == "PAY_IN") MoneyFormat.addMinor(active.totalPayIn, amountMinor) else active.totalPayIn
                    val newPayOut = if (type == "PAY_OUT") MoneyFormat.addMinor(active.totalPayOut, amountMinor) else active.totalPayOut
                    val newExpected = MoneyFormat.calculateExpectedCashMinor(
                        startingCash = active.startingCash,
                        cashSales = active.cashSales,
                        payIn = newPayIn,
                        payOut = newPayOut
                    )
                    reportDao.updateShiftSession(active.copy(totalPayIn = newPayIn, totalPayOut = newPayOut, expectedCash = newExpected))
                }
            }
            _uiMessage.emit(
                UiText.StringResource(
                    R.string.toast_movement_recorded,
                    type,
                    MoneyFormat.toPlainDecimal(MoneyFormat.toMinorUnits(amount, digits), digits)
                )
            )
        }
    }

    fun addCashier(name: String, pin: String, role: String = "CASHIER") {
        if (name.isBlank()) return
        syncManager.addCashier(name, pin, role) {
            viewModelScope.launch {
                _uiMessage.emit(UiText.StringResource(R.string.toast_staff_added, name))
            }
        }
    }

    fun updateCashier(cashier: Cashier) {
        syncManager.updateCashier(cashier) {
            viewModelScope.launch {
                _uiMessage.emit(
                    UiText.StringResource(
                        if (cashier.active) R.string.toast_cashier_activated else R.string.toast_cashier_deactivated,
                        cashier.name
                    )
                )
            }
        }
    }

    fun archiveCashier(cashier: Cashier) {
        syncManager.archiveCashier(cashier) {
            viewModelScope.launch {
                _uiMessage.emit(UiText.StringResource(R.string.toast_cashier_archived, cashier.name))
            }
        }
    }

    fun unarchiveCashier(cashier: Cashier) {
        syncManager.unarchiveCashier(cashier) {
            viewModelScope.launch {
                _uiMessage.emit(UiText.StringResource(R.string.toast_cashier_restored, cashier.name))
            }
        }
    }

    suspend fun getShiftCountByCashierId(cashierId: String): Int {
        return repository.getShiftCountByCashierId(cashierId)
    }

    fun deleteCashier(cashier: Cashier) {
        syncManager.deleteCashier(cashier) {
            viewModelScope.launch {
                _uiMessage.emit(UiText.StringResource(R.string.toast_staff_removed, cashier.name))
            }
        }
    }

    fun saveBusinessProfile(profile: BusinessProfile) {
        viewModelScope.launch(Dispatchers.IO) {
            reportDao.saveBusinessProfile(profile)
            _uiMessage.emit(UiText.StringResource(R.string.business_settings_updated))
        }
    }

    fun updateBusiness(request: UpdateBusinessRequest) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentBp = reportDao.getBusinessProfileOnce() ?: BusinessProfile()
            val updatedBp = currentBp.copy(
                businessName = request.businessName ?: currentBp.businessName,
                phone = request.phone ?: currentBp.phone,
                email = request.email ?: currentBp.email,
                address = if (request.address.isNotBlank()) request.address else currentBp.address,
                mapLat = if (request.mapLat != 0.0) request.mapLat else currentBp.mapLat,
                mapLng = if (request.mapLng != 0.0) request.mapLng else currentBp.mapLng
            )
            reportDao.saveBusinessProfile(updatedBp)

            val currentUp = reportDao.getUserProfileOnce() ?: UserProfile()
            val updatedUp = currentUp.copy(
                address = if (request.address.isNotBlank()) request.address else currentUp.address,
                mapLat = if (request.mapLat != 0.0) request.mapLat else currentUp.mapLat,
                mapLng = if (request.mapLng != 0.0) request.mapLng else currentUp.mapLng
            )
            reportDao.saveUserProfile(updatedUp)

            _uiMessage.emit(UiText.StringResource(R.string.toast_location_updated))
        }
    }

    fun saveUserProfile(profile: UserProfile) {
        viewModelScope.launch(Dispatchers.IO) {
            reportDao.saveUserProfile(profile)
            preferencesRepository.syncWithUserProfile(profile)
            reportDao.insertAuditLog(
                AuditLog(
                    username = profile.username,
                    action = "UPDATE_PROFILE",
                    details = "Updated profile for ${profile.fullName} (${profile.email})"
                )
            )
            _uiMessage.emit(UiText.StringResource(R.string.profile_updated_successfully))
        }
    }

    fun saveReceiptConfig(config: ShopReceiptConfig) {
        viewModelScope.launch(Dispatchers.IO) {
            reportDao.saveReceiptConfig(config)
            _uiMessage.emit(UiText.StringResource(R.string.receipt_customization_updated))
        }
    }

    suspend fun saveSavedAddress(address: SavedAddress) {
        val existingCount = savedAddressDao.count()
        val finalAddress = if (existingCount == 0) address.copy(isDefault = true) else address
        if (finalAddress.isDefault) savedAddressDao.clearDefaults()
        savedAddressDao.upsert(finalAddress)
        if (finalAddress.isDefault) {
            syncDefaultAddressToProfile(finalAddress)
        }
    }

    suspend fun deleteSavedAddress(address: SavedAddress) {
        savedAddressDao.delete(address)
    }

    suspend fun setDefaultAddress(id: String) {
        savedAddressDao.clearDefaults()
        savedAddressDao.markAsDefault(id)
        savedAddressDao.getById(id)?.let { syncDefaultAddressToProfile(it) }
    }

    fun setAsDefault(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            setDefaultAddress(id)
        }
    }

    fun saveAddress(address: SavedAddress) {
        viewModelScope.launch(Dispatchers.IO) {
            saveSavedAddress(address)
        }
    }

    fun upsertSavedAddress(address: SavedAddress) {
        viewModelScope.launch(Dispatchers.IO) {
            saveSavedAddress(address)
        }
    }

    suspend fun getSavedAddressById(id: String): SavedAddress? {
        return savedAddressDao.getById(id)
    }

    private suspend fun syncDefaultAddressToProfile(address: SavedAddress) {
        val currentBp = reportDao.getBusinessProfileOnce() ?: BusinessProfile()
        reportDao.saveBusinessProfile(
            currentBp.copy(
                address = address.addressLine,
                mapLat = address.mapLat,
                mapLng = address.mapLng
            )
        )
        val currentUp = reportDao.getUserProfileOnce() ?: UserProfile()
        reportDao.saveUserProfile(
            currentUp.copy(
                address = address.addressLine,
                mapLat = address.mapLat,
                mapLng = address.mapLng
            )
        )
    }
}
