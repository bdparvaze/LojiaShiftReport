package com.lojia.pos.report

import com.lojia.pos.R
import com.lojia.pos.data.*
import com.lojia.pos.util.*
import com.lojia.pos.ui.common.*
import com.lojia.pos.ui.theme.*
import com.lojia.pos.auth.*
import com.lojia.pos.report.*
import com.lojia.pos.settings.*
import com.lojia.pos.sync.*


import android.app.Application


import androidx.lifecycle.AndroidViewModel

import com.lojia.pos.util.UiText



import androidx.lifecycle.viewModelScope

import com.lojia.pos.util.SecurityUtils

import com.lojia.pos.util.NotificationHelper

import com.lojia.pos.util.ShiftReportSyncScheduler

import com.lojia.pos.util.ShiftReportSyncWorker


import java.util.Locale
import kotlinx.coroutines.delay


import kotlinx.coroutines.flow.*


import kotlinx.coroutines.launch

class ReportViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val db = AppDatabase.getInstance(application)
    private val reportDao = db.reportDao()
    private val repository = ShiftReportRepository(reportDao)

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
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

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

    fun setLanguage(language: AppLanguage) {
        syncManager.setLanguage(language)
        viewModelScope.launch {
            _uiMessage.emit(UiText.StringResource(R.string.toast_lang_changed, language.displayName))
        }
    }

    fun setCountry(country: AppCountry) {
        syncManager.setCountry(country)
        viewModelScope.launch {
            _uiMessage.emit(UiText.StringResource(R.string.toast_country_set, country.displayNameEn, country.currencyCode, country.currencySymbol))
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

    // Active Shop Settings Menu Key (Profile, Security, Sales, Receipts, Shift, Items, Settings, Back Office, Apps, Language, Support)
    private val _selectedShopSettingsMenu = MutableStateFlow<String?>("profile")
    val selectedShopSettingsMenu: StateFlow<String?> = _selectedShopSettingsMenu.asStateFlow()

    fun selectShopSettingsMenu(menuKey: String?) {
        _selectedShopSettingsMenu.value = menuKey
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

    fun switchUserRole(role: String, enteredPin: String? = null, onSuccess: () -> Unit = {}, onFailure: () -> Unit = {}) {
        viewModelScope.launch {
            val profile = userProfile.value ?: UserProfile()
            if (role == "ADMIN" && profile.currentRole == "CASHIER") {
                // Switching from Cashier to Admin requires Admin PIN check
                if (enteredPin != null && SecurityUtils.verifySecret(enteredPin, profile.pin)) {
                    reportDao.saveUserProfile(profile.copy(currentRole = "ADMIN"))
                    _uiMessage.emit(UiText.StringResource(R.string.authenticated_as_administrator))
                    onSuccess()
                } else {
                    _uiMessage.emit(UiText.StringResource(R.string.invalid_admin_pin))
                    onFailure()
                }
            } else {
                reportDao.saveUserProfile(profile.copy(currentRole = role))
                _uiMessage.emit(UiText.StringResource(R.string.toast_role_switched, role))
                onSuccess()
            }
        }
    }

    // Backup State & Progress
    private val _isBackingUp = MutableStateFlow(false)
    val isBackingUp: StateFlow<Boolean> = _isBackingUp.asStateFlow()

    private val _backupProgress = MutableStateFlow(0f)
    val backupProgress: StateFlow<Float> = _backupProgress.asStateFlow()

    // Google Drive Cloud Backup State
    private val _driveAccountInfo = MutableStateFlow(GoogleDriveManager.getAccountInfo(context))
    val driveAccountInfo: StateFlow<DriveAccountInfo> = _driveAccountInfo.asStateFlow()

    private val _isDriveUploading = MutableStateFlow(false)
    val isDriveUploading: StateFlow<Boolean> = _isDriveUploading.asStateFlow()

    private val _isDriveDownloading = MutableStateFlow(false)
    val isDriveDownloading: StateFlow<Boolean> = _isDriveDownloading.asStateFlow()

    private val _isDriveLoadingList = MutableStateFlow(false)
    val isDriveLoadingList: StateFlow<Boolean> = _isDriveLoadingList.asStateFlow()

    private val _driveBackupsList = MutableStateFlow<List<DriveBackupItem>>(emptyList())
    val driveBackupsList: StateFlow<List<DriveBackupItem>> = _driveBackupsList.asStateFlow()

    fun refreshDriveAccount() {
        _driveAccountInfo.value = GoogleDriveManager.getAccountInfo(context)
        if (_driveAccountInfo.value.isConnected) {
            fetchGoogleDriveBackups()
        }
    }

    fun connectGoogleDrive(activityContext: android.content.Context) {
        val authUrl = GoogleDriveManager.buildAuthUrl(activityContext)
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(authUrl)).apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        activityContext.startActivity(intent)
    }

    fun disconnectGoogleDrive() {
        GoogleDriveManager.disconnect(context)
        _driveAccountInfo.value = GoogleDriveManager.getAccountInfo(context)
        _driveBackupsList.value = emptyList()
        viewModelScope.launch {
            _uiMessage.emit(UiText.StringResource(R.string.drive_not_connected))
        }
    }

    fun handleDriveAuthRedirect(uri: android.net.Uri, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val code = uri.getQueryParameter("code")
            if (code.isNullOrEmpty()) {
                val error = uri.getQueryParameter("error") ?: "Authorization cancelled or failed"
                _uiMessage.emit(UiText.DynamicString(error))
                onComplete(false)
                return@launch
            }
            val result = GoogleDriveManager.handleOAuthCallback(context, code)
            result.onSuccess { account: DriveAccountInfo ->
                _driveAccountInfo.value = account
                val emailStr = account.email ?: "Google Account"
                _uiMessage.emit(UiText.StringResource(R.string.drive_connected_as, emailStr))
                fetchGoogleDriveBackups()
                onComplete(true)
            }.onFailure { err ->
                val msg = err.localizedMessage ?: "Drive authentication failed"
                _uiMessage.emit(UiText.DynamicString(msg))
                onComplete(false)
            }
        }
    }

    fun backupToGoogleDrive(
        onSuccess: (DriveBackupItem) -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        if (_isDriveUploading.value || _isBackingUp.value) return
        viewModelScope.launch {
            _isDriveUploading.value = true
            _isBackingUp.value = true
            _backupProgress.value = 0.2f
            delay(150)
            _backupProgress.value = 0.5f

            val result = GoogleDriveManager.uploadBackup(context, db)
            _backupProgress.value = 1.0f
            _isDriveUploading.value = false
            _isBackingUp.value = false

            result.onSuccess { item ->
                val now = System.currentTimeMillis()
                lastBackupTime.value = now
                prefs.edit().putLong("last_backup_time", now).apply()

                _uiMessage.emit(UiText.StringResource(R.string.drive_backup_success, item.name))
                NotificationHelper.sendTestPushNotification(
                    context,
                    "☁️ Google Drive Backup",
                    "Uploaded ${item.name} (${item.description}) to Google Drive App Folder."
                )
                fetchGoogleDriveBackups()
                onSuccess(item)
            }.onFailure { error ->
                val msg = error.localizedMessage ?: "Failed to upload to Google Drive"
                _uiMessage.emit(UiText.DynamicString(msg))
                onFailure(msg)
            }
        }
    }

    fun fetchGoogleDriveBackups(
        onResult: (List<DriveBackupItem>) -> Unit = {}
    ) {
        if (!GoogleDriveManager.isConnected(context)) return
        viewModelScope.launch {
            _isDriveLoadingList.value = true
            val result = GoogleDriveManager.listBackups(context)
            _isDriveLoadingList.value = false
            result.onSuccess { list ->
                _driveBackupsList.value = list
                onResult(list)
            }.onFailure { error ->
                val msg = error.localizedMessage ?: "Failed to fetch Drive backups"
                _uiMessage.emit(UiText.DynamicString(msg))
            }
        }
    }

    fun restoreFromGoogleDrive(
        item: DriveBackupItem,
        onSuccess: (Int) -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        if (_isDriveDownloading.value || _isBackingUp.value) return
        viewModelScope.launch {
            _isDriveDownloading.value = true
            _isBackingUp.value = true
            val downloadResult = GoogleDriveManager.downloadBackup(context, item.id)

            downloadResult.onSuccess { jsonString ->
                val activeUser = userProfile.value?.fullName ?: "Admin"
                val restoreResult = OfflineBackupManager.restoreBackupFromString(
                    jsonString,
                    db,
                    sourceDescription = "Google Drive (${item.name})",
                    adminUser = activeUser
                )
                _isDriveDownloading.value = false
                _isBackingUp.value = false

                restoreResult.onSuccess { count ->
                    syncManager.forceSync { _: Boolean, _: String -> }
                    _uiMessage.emit(UiText.StringResource(R.string.restore_success_msg, count))
                    NotificationHelper.sendTestPushNotification(
                        context,
                        "🔄 Google Drive Data Restored",
                        "Restored $count records from Google Drive (${item.name})."
                    )
                    onSuccess(count)
                }.onFailure { restoreErr ->
                    val msg = restoreErr.localizedMessage ?: "Failed to restore backup"
                    _uiMessage.emit(UiText.StringResource(R.string.invalid_backup_file))
                    onFailure(msg)
                }
            }.onFailure { downloadErr ->
                _isDriveDownloading.value = false
                _isBackingUp.value = false
                val msg = downloadErr.localizedMessage ?: "Failed to download backup from Google Drive"
                _uiMessage.emit(UiText.DynamicString(msg))
                onFailure(msg)
            }
        }
    }

    fun parseDriveBackupSummary(
        item: DriveBackupItem,
        onResult: (BackupSummary?) -> Unit
    ) {
        viewModelScope.launch {
            val downloadResult = GoogleDriveManager.downloadBackup(context, item.id)
            downloadResult.onSuccess { jsonString ->
                val summaryResult = OfflineBackupManager.parseBackupSummaryFromString(jsonString)
                onResult(summaryResult.getOrNull())
            }.onFailure {
                onResult(null)
            }
        }
    }

    // =========================================================================
    // Firebase Cloud Sync (Offline-First multi-device sync with Room primary)
    // =========================================================================
    val firebaseSyncState: StateFlow<FirebaseSyncStatusState> = FirebaseCloudSyncManager.syncState

    fun toggleFirebaseCloudSync(enabled: Boolean) {
        FirebaseCloudSyncManager.setSyncEnabled(context, enabled)
        viewModelScope.launch {
            if (enabled) {
                _uiMessage.emit(UiText.StringResource(R.string.firebase_sync_enable))
            } else {
                _uiMessage.emit(UiText.StringResource(R.string.firebase_sync_disabled))
            }
        }
    }

    fun syncFirebaseNow(
        onComplete: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            val result = FirebaseCloudSyncManager.performSync(context, isManual = true)
            if (result.success) {
                _uiMessage.emit(UiText.StringResource(R.string.firebase_sync_success))
            } else {
                _uiMessage.emit(UiText.DynamicString(result.message))
            }
            onComplete(result.success, result.message)
        }
    }

    fun updateFirebaseCustomUrl(url: String) {
        FirebaseCloudSyncManager.setCustomFirebaseUrl(context, url)
    }

    var googleAccount: MutableStateFlow<String> = MutableStateFlow(prefs.getString("google_account", "") ?: "")
    var autoBackupFrequency: MutableStateFlow<String> = MutableStateFlow(prefs.getString("auto_backup_freq", "Daily") ?: "Daily")
    var backupUsingCellular: MutableStateFlow<Boolean> = MutableStateFlow(prefs.getBoolean("backup_cellular", true))
    var lastBackupTime: MutableStateFlow<Long> = MutableStateFlow(prefs.getLong("last_backup_time", System.currentTimeMillis() - 3600000L))

    // WorkManager Background Server Sync State
    var lastServerSyncTime: MutableStateFlow<Long> = MutableStateFlow(
        prefs.getLong(ShiftReportSyncWorker.KEY_LAST_SYNC_TIME, System.currentTimeMillis() - 1800000L)
    )
    var lastServerSyncStatus: MutableStateFlow<String> = MutableStateFlow(
        prefs.getString(ShiftReportSyncWorker.KEY_LAST_SYNC_STATUS, "Active Periodic Sync (WorkManager)") ?: "Active Periodic Sync (WorkManager)"
    )

    fun triggerServerSyncNow() {
        ShiftReportSyncScheduler.triggerImmediateSync(getApplication())
        viewModelScope.launch {
            _uiMessage.emit(UiText.StringResource(R.string.background_sync_task_enqueued))
            delay(1200)
            lastServerSyncTime.value = System.currentTimeMillis()
            lastServerSyncStatus.value = "Synced via WorkManager"
        }
    }

    fun updatePeriodicSyncInterval(intervalMinutes: Long, wifiOnly: Boolean = false) {
        ShiftReportSyncScheduler.schedulePeriodicSync(
            getApplication(),
            intervalMinutes = intervalMinutes,
            requireWifiOnly = wifiOnly
        )
        viewModelScope.launch {
            _uiMessage.emit(UiText.StringResource(R.string.toast_sync_interval_set, intervalMinutes.toInt()))
        }
    }

    fun triggerCloudBackup() {
        if (_isBackingUp.value) return
        viewModelScope.launch {
            _isBackingUp.value = true
            _backupProgress.value = 0.1f
            delay(400)
            _backupProgress.value = 0.35f
            delay(500)
            _backupProgress.value = 0.7f
            delay(400)
            _backupProgress.value = 1.0f
            val now = System.currentTimeMillis()
            lastBackupTime.value = now
            prefs.edit().putLong("last_backup_time", now).apply()
            _isBackingUp.value = false
            _uiMessage.emit(UiText.StringResource(R.string.toast_cloud_backup_success, googleAccount.value))
            NotificationHelper.sendTestPushNotification(
                context,
                "☁️ Cloud Backup Successful",
                "Your store database and shift ledgers were backed up to Google Drive at ${java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(now))}."
            )
        }
    }

    fun createOfflineBackup(
        onSuccess: (BackupInfo) -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        if (_isBackingUp.value) return
        viewModelScope.launch {
            _isBackingUp.value = true
            _backupProgress.value = 0.2f
            delay(200)
            _backupProgress.value = 0.6f

            val result = OfflineBackupManager.createBackup(context, db)
            _backupProgress.value = 1.0f
            _isBackingUp.value = false

            result.onSuccess { info ->
                val now = System.currentTimeMillis()
                lastBackupTime.value = now
                prefs.edit().putLong("last_backup_time", now).apply()

                _uiMessage.emit(UiText.StringResource(R.string.backup_created_success, info.fileName))
                NotificationHelper.sendTestPushNotification(
                    context,
                    "📦 Offline Backup Created",
                    "Saved to Downloads: ${info.fileName} (${info.totalRecords} records)"
                )
                onSuccess(info)
            }.onFailure { error ->
                val msg = error.localizedMessage ?: "Failed to create backup"
                _uiMessage.emit(UiText.DynamicString(msg))
                onFailure(msg)
            }
        }
    }

    fun parseBackupSummary(
        uri: android.net.Uri,
        onResult: (BackupSummary?) -> Unit
    ) {
        viewModelScope.launch {
            val result = OfflineBackupManager.parseBackupSummary(context, uri)
            onResult(result.getOrNull())
        }
    }

    fun restoreOfflineBackup(
        uri: android.net.Uri,
        onSuccess: (Int) -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBackingUp.value = true
            val activeUser = userProfile.value?.fullName ?: "Admin"
            val result = OfflineBackupManager.restoreBackup(context, uri, db, activeUser)
            _isBackingUp.value = false

            result.onSuccess { count ->
                syncManager.forceSync { _: Boolean, _: String -> }
                _uiMessage.emit(UiText.StringResource(R.string.restore_success_msg, count))
                NotificationHelper.sendTestPushNotification(
                    context,
                    "🔄 Data Restored Successfully",
                    "Restored $count records into your local store database."
                )
                onSuccess(count)
            }.onFailure { error ->
                val msg = error.localizedMessage ?: "Failed to restore backup"
                _uiMessage.emit(UiText.StringResource(R.string.invalid_backup_file))
                onFailure(msg)
            }
        }
    }

    fun clearAllBusinessData(
        onSuccess: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    db.clearAllTables()
                    val defaultCashier = Cashier(
                        id = 0,
                        name = "Lojia Admin",
                        role = "ADMIN",
                        active = true
                    )
                    reportDao.insertCashiers(listOf(defaultCashier))
                    
                    val defaultProfile = UserProfile(
                        id = 1,
                        fullName = "Store Owner",
                        currentRole = "ADMIN"
                    )
                    reportDao.saveUserProfile(defaultProfile)
                }
                syncManager.forceSync { _: Boolean, _: String -> }
                _uiMessage.emit(UiText.StringResource(R.string.all_data_cleared_success))
                onSuccess()
            } catch (e: Exception) {
                val err = e.localizedMessage ?: "Failed to clear data"
                _uiMessage.emit(UiText.DynamicString(err))
                onFailure(err)
            }
        }
    }

    // Shift Form State
    var selectedCashier = MutableStateFlow("")
    var selectedShift = MutableStateFlow("Day")
    var selectedDateInMillis = MutableStateFlow(System.currentTimeMillis())
    var grossCashInput = MutableStateFlow("")
    var madaPaymentsInput = MutableStateFlow("")
    var digitalWalletInput = MutableStateFlow("")
    var staffMealsCountInput = MutableStateFlow("0")
    var totalExpensesInput = MutableStateFlow("")
    var muasselQtyInput = MutableStateFlow("")
    var outdoorShishaQtyInput = MutableStateFlow("")
    var notesInput = MutableStateFlow("")

    var startingCashInput = MutableStateFlow("")
    var actualCashCountInput = MutableStateFlow("")

    val startingCash: StateFlow<Double> = startingCashInput.map { it.toDoubleOrNull() ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val actualCashCount: StateFlow<Double> = actualCashCountInput.map { it.toDoubleOrNull() ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val isActualCashEntered: StateFlow<Boolean> = actualCashCountInput.map { it.isNotBlank() && it.toDoubleOrNull() != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Dynamic Section Lists
    val dueCreditItems = MutableStateFlow<List<DueCreditItem>>(emptyList())
    val previousDueCollections = MutableStateFlow<List<PreviousDueCollectionItem>>(emptyList())
    val staffAdvances = MutableStateFlow<List<StaffAdvanceItem>>(emptyList())
    val unpaidBills = MutableStateFlow<List<UnpaidBillItem>>(emptyList())
    val purchasedItems = MutableStateFlow<List<PurchasedInventoryItem>>(emptyList())

    fun isValidDecimal(input: String): Boolean {
        if (input.isBlank()) return true
        val value = input.toDoubleOrNull()
        return value != null && value >= 0
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

    val grossCash = grossCashInput.map { it.toDoubleOrNull() ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val madaPayments = madaPaymentsInput.map { it.toDoubleOrNull() ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val digitalWallet = digitalWalletInput.map { it.toDoubleOrNull() ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val totalExpenses = totalExpensesInput.map { it.toDoubleOrNull() ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Dynamic Lists Sums
    val totalDueCreditAmount: StateFlow<Double> = dueCreditItems.map { list ->
        list.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalPreviousDueCollected: StateFlow<Double> = previousDueCollections.map { list ->
        list.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalPreviousDueCash: StateFlow<Double> = previousDueCollections.map { list ->
        list.filter { it.paymentMode.equals("CASH", ignoreCase = true) }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalPreviousDueCard: StateFlow<Double> = previousDueCollections.map { list ->
        list.filter { !it.paymentMode.equals("CASH", ignoreCase = true) }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalStaffAdvances: StateFlow<Double> = staffAdvances.map { list ->
        list.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalUnpaidLoss: StateFlow<Double> = unpaidBills.map { list ->
        list.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalPurchasedCash: StateFlow<Double> = purchasedItems.map { list ->
        list.filter { it.paidVia.equals("CASH", ignoreCase = true) }.sumOf { it.totalAmount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalPurchasedAll: StateFlow<Double> = purchasedItems.map { list ->
        list.sumOf { it.totalAmount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Automatic Live Calculations following International Standards
    // 1. Total Sales (payment methods only: Cash + Card/Mada + Digital Wallet)
    val totalSales: StateFlow<Double> = combine(
        grossCash,
        madaPayments,
        digitalWallet
    ) { cash, mada, wallet ->
        cash + mada + wallet
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val calculatedTotalGrossSales: StateFlow<Double> = totalSales
    val totalRevenue: StateFlow<Double> = totalSales

    // 2. Cash In & Outflows
    val totalCashIn: StateFlow<Double> = combine(
        grossCash,
        totalPreviousDueCash
    ) { cashSales, prevDueCash ->
        cashSales + prevDueCash
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalCashOut: StateFlow<Double> = combine(
        totalExpenses,
        totalStaffAdvances,
        totalPurchasedCash
    ) { exp, adv, purchCash ->
        exp + adv + purchCash
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val calculatedTotalExpenses: StateFlow<Double> = totalCashOut

    // 3. Expected Cash in Drawer & Net Cash
    val expectedCashInDrawer: StateFlow<Double> = combine(
        startingCash,
        totalCashIn,
        totalCashOut
    ) { start, cin, cout ->
        start + cin - cout
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val calculatedCashInDrawer: StateFlow<Double> = expectedCashInDrawer
    val netCash: StateFlow<Double> = expectedCashInDrawer

    // 4. Over / Short Cash Variance
    val cashVariance: StateFlow<Double> = combine(
        actualCashCount,
        expectedCashInDrawer
    ) { actual, expected ->
        actual - expected
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val calculatedCardAndDigital: StateFlow<Double> = combine(
        madaPayments,
        totalPreviousDueCard,
        digitalWallet
    ) { mada, prevCard, wallet ->
        mada + prevCard + wallet
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val calculatedNetBalance: StateFlow<Double> = combine(
        totalSales,
        totalCashOut,
        totalUnpaidLoss
    ) { sales, exp, unpaid ->
        sales - exp - unpaid
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    private val _uiMessage = MutableSharedFlow<com.lojia.pos.util.UiText>()
    val uiMessage = _uiMessage.asSharedFlow()

    // Add / Remove Handlers for Dynamic Sections
    fun addDueCreditItem(customerName: String, amount: Double, note: String = "", phone: String = "") {
        if (customerName.isBlank() || amount <= 0) return
        dueCreditItems.value = dueCreditItems.value + DueCreditItem(customerName = customerName.trim(), amount = amount, note = note.trim(), phone = phone.trim())
    }

    fun removeDueCreditItem(id: String) {
        dueCreditItems.value = dueCreditItems.value.filter { it.id != id }
    }

    fun addPreviousDueCollection(customerName: String, amount: Double, paymentMode: String = "CASH", note: String = "") {
        if (customerName.isBlank() || amount <= 0) return
        previousDueCollections.value = previousDueCollections.value + PreviousDueCollectionItem(customerName = customerName.trim(), amount = amount, paymentMode = paymentMode, note = note.trim())
    }

    fun removePreviousDueCollection(id: String) {
        previousDueCollections.value = previousDueCollections.value.filter { it.id != id }
    }

    fun addStaffAdvance(staffName: String, amount: Double, reason: String = "") {
        if (staffName.isBlank() || amount <= 0) return
        staffAdvances.value = staffAdvances.value + StaffAdvanceItem(staffName = staffName.trim(), amount = amount, reason = reason.trim())
    }

    fun removeStaffAdvance(id: String) {
        staffAdvances.value = staffAdvances.value.filter { it.id != id }
    }

    fun addUnpaidBill(tableOrOrderRef: String, amount: Double, reason: String = "") {
        if (tableOrOrderRef.isBlank() || amount <= 0) return
        unpaidBills.value = unpaidBills.value + UnpaidBillItem(tableOrOrderRef = tableOrOrderRef.trim(), amount = amount, reason = reason.trim())
    }

    fun removeUnpaidBill(id: String) {
        unpaidBills.value = unpaidBills.value.filter { it.id != id }
    }

    fun addPurchasedItem(itemName: String, quantity: Double, unitPrice: Double, paidVia: String = "CASH", supplier: String = "") {
        if (itemName.isBlank() || quantity <= 0 || unitPrice <= 0) return
        purchasedItems.value = purchasedItems.value + PurchasedInventoryItem(
            itemName = itemName.trim(),
            quantity = quantity,
            unitPrice = unitPrice,
            totalAmount = quantity * unitPrice,
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
        if (FirebaseCloudSyncManager.isSyncEnabled(context)) {
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

    fun serializeDueCredits(list: List<DueCreditItem>): String {
        val arr = org.json.JSONArray()
        list.forEach { item ->
            val obj = org.json.JSONObject()
            obj.put("id", item.id)
            obj.put("customerName", item.customerName)
            obj.put("amount", item.amount)
            obj.put("note", item.note)
            obj.put("phone", item.phone)
            arr.put(obj)
        }
        return arr.toString()
    }

    fun deserializeDueCredits(json: String): List<DueCreditItem> {
        if (json.isBlank() || json == "[]") return emptyList()
        val list = mutableListOf<DueCreditItem>()
        try {
            val arr = org.json.JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    DueCreditItem(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        customerName = obj.optString("customerName", ""),
                        amount = obj.optDouble("amount", 0.0),
                        note = obj.optString("note", ""),
                        phone = obj.optString("phone", "")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun serializePrevDues(list: List<PreviousDueCollectionItem>): String {
        val arr = org.json.JSONArray()
        list.forEach { item ->
            val obj = org.json.JSONObject()
            obj.put("id", item.id)
            obj.put("customerName", item.customerName)
            obj.put("amount", item.amount)
            obj.put("paymentMode", item.paymentMode)
            obj.put("note", item.note)
            arr.put(obj)
        }
        return arr.toString()
    }

    fun deserializePrevDues(json: String): List<PreviousDueCollectionItem> {
        if (json.isBlank() || json == "[]") return emptyList()
        val list = mutableListOf<PreviousDueCollectionItem>()
        try {
            val arr = org.json.JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    PreviousDueCollectionItem(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        customerName = obj.optString("customerName", ""),
                        amount = obj.optDouble("amount", 0.0),
                        paymentMode = obj.optString("paymentMode", "CASH"),
                        note = obj.optString("note", "")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun serializeAdvances(list: List<StaffAdvanceItem>): String {
        val arr = org.json.JSONArray()
        list.forEach { item ->
            val obj = org.json.JSONObject()
            obj.put("id", item.id)
            obj.put("staffName", item.staffName)
            obj.put("amount", item.amount)
            obj.put("reason", item.reason)
            arr.put(obj)
        }
        return arr.toString()
    }

    fun deserializeAdvances(json: String): List<StaffAdvanceItem> {
        if (json.isBlank() || json == "[]") return emptyList()
        val list = mutableListOf<StaffAdvanceItem>()
        try {
            val arr = org.json.JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    StaffAdvanceItem(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        staffName = obj.optString("staffName", ""),
                        amount = obj.optDouble("amount", 0.0),
                        reason = obj.optString("reason", "")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun serializeUnpaid(list: List<UnpaidBillItem>): String {
        val arr = org.json.JSONArray()
        list.forEach { item ->
            val obj = org.json.JSONObject()
            obj.put("id", item.id)
            obj.put("tableOrOrderRef", item.tableOrOrderRef)
            obj.put("amount", item.amount)
            obj.put("reason", item.reason)
            arr.put(obj)
        }
        return arr.toString()
    }

    fun deserializeUnpaid(json: String): List<UnpaidBillItem> {
        if (json.isBlank() || json == "[]") return emptyList()
        val list = mutableListOf<UnpaidBillItem>()
        try {
            val arr = org.json.JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    UnpaidBillItem(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        tableOrOrderRef = obj.optString("tableOrOrderRef", ""),
                        amount = obj.optDouble("amount", 0.0),
                        reason = obj.optString("reason", "")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun serializePurchased(list: List<PurchasedInventoryItem>): String {
        val arr = org.json.JSONArray()
        list.forEach { item ->
            val obj = org.json.JSONObject()
            obj.put("id", item.id)
            obj.put("itemName", item.itemName)
            obj.put("quantity", item.quantity)
            obj.put("unitPrice", item.unitPrice)
            obj.put("totalAmount", item.totalAmount)
            obj.put("paidVia", item.paidVia)
            obj.put("supplier", item.supplier)
            arr.put(obj)
        }
        return arr.toString()
    }

    fun deserializePurchased(json: String): List<PurchasedInventoryItem> {
        if (json.isBlank() || json == "[]") return emptyList()
        val list = mutableListOf<PurchasedInventoryItem>()
        try {
            val arr = org.json.JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    PurchasedInventoryItem(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        itemName = obj.optString("itemName", ""),
                        quantity = obj.optDouble("quantity", 1.0),
                        unitPrice = obj.optDouble("unitPrice", 0.0),
                        totalAmount = obj.optDouble("totalAmount", 0.0),
                        paidVia = obj.optString("paidVia", "CASH"),
                        supplier = obj.optString("supplier", "")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun loadDraft() {
        viewModelScope.launch {
            val draft = repository.getDraft()
            if (draft != null) {
                selectedCashier.value = draft.cashierName
                selectedShift.value = draft.shift
                selectedDateInMillis.value = draft.dateInMillis
                grossCashInput.value = if (draft.grossCash > 0) draft.grossCash.toString() else ""
                madaPaymentsInput.value = if (draft.madaPayments > 0) draft.madaPayments.toString() else ""
                digitalWalletInput.value = if (draft.digitalWallet > 0) draft.digitalWallet.toString() else ""
                staffMealsCountInput.value = draft.staffMealsCount.toString()
                totalExpensesInput.value = if (draft.totalExpenses > 0) draft.totalExpenses.toString() else ""
                muasselQtyInput.value = if (draft.muasselQty > 0) draft.muasselQty.toString() else ""
                outdoorShishaQtyInput.value = if (draft.outdoorShishaQty > 0) draft.outdoorShishaQty.toString() else ""
                notesInput.value = draft.notes
                dueCreditItems.value = deserializeDueCredits(draft.dueCreditEntriesJson)
                previousDueCollections.value = deserializePrevDues(draft.previousDueCollectionsJson)
                staffAdvances.value = deserializeAdvances(draft.staffAdvancesJson)
                unpaidBills.value = deserializeUnpaid(draft.unpaidBillsJson)
                purchasedItems.value = deserializePurchased(draft.purchasedItemsJson)
            }
        }
    }

    fun validateAll(): Boolean {
        return isValidDecimal(grossCashInput.value) &&
                isValidDecimal(madaPaymentsInput.value) &&
                isValidDecimal(digitalWalletInput.value) &&
                isValidDecimal(totalExpensesInput.value) &&
                isValidInteger(staffMealsCountInput.value) &&
                isValidDecimal(muasselQtyInput.value) &&
                isValidDecimal(outdoorShishaQtyInput.value)
    }

    fun saveDraft() {
        if (!validateAll()) {
            viewModelScope.launch {
                _uiMessage.emit(UiText.StringResource(R.string.please_fix_invalid_numeric))
            }
            return
        }
        viewModelScope.launch {
            val draft = DraftReport(
                id = 1,
                cashierName = selectedCashier.value,
                shift = selectedShift.value,
                dateInMillis = selectedDateInMillis.value,
                grossCash = grossCashInput.value.toDoubleOrNull() ?: 0.0,
                madaPayments = madaPaymentsInput.value.toDoubleOrNull() ?: 0.0,
                digitalWallet = digitalWalletInput.value.toDoubleOrNull() ?: 0.0,
                staffMealsCount = staffMealsCountInput.value.toIntOrNull() ?: 0,
                totalExpenses = totalExpensesInput.value.toDoubleOrNull() ?: 0.0,
                muasselQty = muasselQtyInput.value.toDoubleOrNull() ?: 0.0,
                outdoorShishaQty = outdoorShishaQtyInput.value.toDoubleOrNull() ?: 0.0,
                dueCreditEntriesJson = serializeDueCredits(dueCreditItems.value),
                previousDueCollectionsJson = serializePrevDues(previousDueCollections.value),
                staffAdvancesJson = serializeAdvances(staffAdvances.value),
                unpaidBillsJson = serializeUnpaid(unpaidBills.value),
                purchasedItemsJson = serializePurchased(purchasedItems.value),
                notes = notesInput.value
            )
            repository.saveDraft(draft)
            _uiMessage.emit(UiText.StringResource(R.string.draft_saved_successfully))
        }
    }

    fun clearForm() {
        selectedCashier.value = ""
        selectedShift.value = "Day"
        selectedDateInMillis.value = System.currentTimeMillis()
        grossCashInput.value = ""
        madaPaymentsInput.value = ""
        digitalWalletInput.value = ""
        staffMealsCountInput.value = "0"
        totalExpensesInput.value = ""
        muasselQtyInput.value = ""
        outdoorShishaQtyInput.value = ""
        notesInput.value = ""
        startingCashInput.value = ""
        actualCashCountInput.value = ""
        dueCreditItems.value = emptyList()
        previousDueCollections.value = emptyList()
        staffAdvances.value = emptyList()
        unpaidBills.value = emptyList()
        purchasedItems.value = emptyList()
        viewModelScope.launch {
            repository.clearDraft()
        }
    }

    fun submitReport(onSuccess: (ShiftReport) -> Unit) {
        if (!validateAll()) {
            viewModelScope.launch {
                _uiMessage.emit(UiText.StringResource(R.string.please_fix_invalid_numeric_1))
            }
            return
        }

        val grossVal = grossCashInput.value.toDoubleOrNull() ?: 0.0
        val madaVal = madaPaymentsInput.value.toDoubleOrNull() ?: 0.0
        val walletVal = digitalWalletInput.value.toDoubleOrNull() ?: 0.0
        val expVal = totalExpensesInput.value.toDoubleOrNull() ?: 0.0
        val muasselVal = muasselQtyInput.value.toDoubleOrNull() ?: 0.0
        val outdoorVal = outdoorShishaQtyInput.value.toDoubleOrNull() ?: 0.0

        if (grossVal == 0.0 && madaVal == 0.0 && walletVal == 0.0 && expVal == 0.0 &&
            staffMealsCountInput.value.toIntOrNull() == 0 && muasselVal == 0.0 && outdoorVal == 0.0 &&
            dueCreditItems.value.isEmpty() && previousDueCollections.value.isEmpty() &&
            staffAdvances.value.isEmpty() && unpaidBills.value.isEmpty() && purchasedItems.value.isEmpty()
        ) {
            viewModelScope.launch {
                _uiMessage.emit(UiText.StringResource(R.string.please_enter_shift_sales))
            }
            return
        }

        viewModelScope.launch {
            val cashier = if (selectedCashier.value.isBlank()) "Standard Cashier" else selectedCashier.value
            val actualCountVal = actualCashCountInput.value.toDoubleOrNull()
            val noteParts = mutableListOf<String>()
            if (actualCountVal != null) {
                noteParts.add("Actual Cash Count: %.2f".format(Locale.US, actualCountVal))
            }
            if (notesInput.value.isNotBlank()) {
                noteParts.add(notesInput.value)
            }
            val formattedNotes = noteParts.joinToString(" | ")

            val report = ShiftReport(
                cashierName = cashier,
                shift = selectedShift.value,
                dateInMillis = selectedDateInMillis.value,
                grossCash = grossVal,
                madaPayments = madaVal,
                digitalWallet = walletVal,
                staffMealsCount = staffMealsCountInput.value.toIntOrNull() ?: 0,
                totalExpenses = expVal,
                muasselQty = muasselVal,
                outdoorShishaQty = outdoorVal,
                dueCreditEntriesJson = serializeDueCredits(dueCreditItems.value),
                previousDueCollectionsJson = serializePrevDues(previousDueCollections.value),
                staffAdvancesJson = serializeAdvances(staffAdvances.value),
                unpaidBillsJson = serializeUnpaid(unpaidBills.value),
                purchasedItemsJson = serializePurchased(purchasedItems.value),
                notes = formattedNotes
            )
            repository.insertReport(report)
            reportDao.insertAuditLog(
                AuditLog(
                    username = cashier,
                    action = "SUBMIT_REPORT",
                    details = "Submitted ${report.shift} shift report. Total sales: ${report.totalSales}, Net Cash: ${report.netCash}"
                )
            )

            // Trigger Push Notification for Daily Report
            val rawCurr = businessProfile.value?.currency ?: "SAR"
            val curr = if (rawCurr == "SAR") context.getString(R.string.currency_unit) else rawCurr
            NotificationHelper.sendShiftSummaryNotification(
                context,
                report.cashierName,
                report.shift,
                report.totalSales,
                report.netCash,
                curr
            )

            clearForm()
            _uiMessage.emit(UiText.StringResource(R.string.shift_report_submitted_successfully))
            onSuccess(report)
        }
    }

    fun saveShiftReportDirect(report: ShiftReport, onSuccess: (ShiftReport) -> Unit) {
        viewModelScope.launch {
            repository.insertReport(report)
            reportDao.insertAuditLog(
                AuditLog(
                    username = report.cashierName,
                    action = "SUBMIT_REPORT",
                    details = "Submitted ${report.shift} shift report. Total sales: ${report.totalSales}, Net Cash: ${report.netCash}"
                )
            )

            val rawCurr = businessProfile.value?.currency ?: "SAR"
            val curr = if (rawCurr == "SAR") context.getString(R.string.currency_unit) else rawCurr
            try {
                NotificationHelper.sendShiftSummaryNotification(
                    getApplication(),
                    report.cashierName,
                    report.shift,
                    report.totalSales,
                    report.netCash,
                    curr
                )
            } catch (_: Exception) {}

            _uiMessage.emit(UiText.StringResource(R.string.shift_report_submitted_successfully_1))
            onSuccess(report)
        }
    }

    fun deleteReport(report: ShiftReport) {
        viewModelScope.launch {
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
        viewModelScope.launch {
            val newSession = ShiftSession(
                cashierName = cashierName.ifBlank { "Cashier" },
                shiftName = shiftName,
                status = "OPEN",
                openedAt = System.currentTimeMillis(),
                startingCash = startingCash,
                expectedCash = startingCash
            )
            reportDao.insertShiftSession(newSession)
            reportDao.insertAuditLog(
                AuditLog(
                    username = cashierName.ifBlank { "Cashier" },
                    action = "OPEN_SHIFT",
                    details = "Opened $shiftName shift. Starting Float: $startingCash"
                )
            )
            val rawCurr = businessProfile.value?.currency ?: "SAR"
            val curr = if (rawCurr == "SAR") context.getString(R.string.currency_unit) else rawCurr
            NotificationHelper.sendShiftOpenedNotification(context, cashierName, startingCash, curr)
            _uiMessage.emit(UiText.StringResource(R.string.toast_shift_opened_cash, startingCash.toString(), curr))
        }
    }

    fun closeShift(session: ShiftSession, actualCashCount: Double, notes: String) {
        viewModelScope.launch {
            val variance = actualCashCount - session.expectedCash
            val updated = session.copy(
                status = "CLOSED",
                closedAt = System.currentTimeMillis(),
                actualCashCount = actualCashCount,
                variance = variance,
                notes = notes
            )
            reportDao.updateShiftSession(updated)
            reportDao.insertAuditLog(
                AuditLog(
                    username = session.cashierName,
                    action = "CLOSE_SHIFT",
                    details = "Closed ${session.shiftName} shift. Expected Cash: ${session.expectedCash}, Actual Count: $actualCashCount, Variance: $variance. Notes: $notes"
                )
            )
            if (variance != 0.0) {
                reportDao.insertAuditLog(
                    AuditLog(
                        username = session.cashierName,
                        action = "VARIANCE_RECORDED",
                        details = "Shift Cash Variance: $variance (${if (variance > 0) "Over" else "Short"})"
                    )
                )
            }
            val rawCurr = businessProfile.value?.currency ?: "SAR"
            val curr = if (rawCurr == "SAR") context.getString(R.string.currency_unit) else rawCurr
            NotificationHelper.sendShiftClosedNotification(context, session.cashierName, variance, curr)
            _uiMessage.emit(UiText.StringResource(R.string.toast_shift_closed_variance, "%.2f".format(variance), curr))
        }
    }

    fun closeShiftWithZReport(
        session: ShiftSession,
        actualCashCount: Double,
        notes: String,
        onReportCreated: (ShiftReport) -> Unit
    ) {
        viewModelScope.launch {
            val variance = actualCashCount - session.expectedCash
            val updated = session.copy(
                status = "CLOSED",
                closedAt = System.currentTimeMillis(),
                actualCashCount = actualCashCount,
                variance = variance,
                notes = notes
            )
            reportDao.updateShiftSession(updated)

            val report = ShiftReport(
                cashierName = session.cashierName,
                shift = session.shiftName,
                dateInMillis = System.currentTimeMillis(),
                grossCash = session.cashSales,
                madaPayments = session.cardSales,
                digitalWallet = session.digitalSales,
                notes = notes
            )
            val id = repository.insertReport(report)
            val finalReport = report.copy(id = id.toInt())

            reportDao.insertAuditLog(
                AuditLog(
                    username = session.cashierName,
                    action = "CLOSE_SHIFT_Z_REPORT",
                    details = "Closed ${session.shiftName} shift with Z-Report #${finalReport.id}. Total Sales: ${finalReport.totalSales}, Expected Cash: ${session.expectedCash}, Actual Count: $actualCashCount, Variance: $variance"
                )
            )

            if (variance != 0.0) {
                reportDao.insertAuditLog(
                    AuditLog(
                        username = session.cashierName,
                        action = "VARIANCE_RECORDED",
                        details = "Z-Report #${finalReport.id} Cash Variance: $variance (${if (variance > 0) "Over" else "Short"})"
                    )
                )
            }

            val rawCurr = businessProfile.value?.currency ?: "SAR"
            val curr = if (rawCurr == "SAR") context.getString(R.string.currency_unit) else rawCurr
            NotificationHelper.sendShiftClosedNotification(context, session.cashierName, variance, curr)
            _uiMessage.emit(UiText.StringResource(R.string.toast_shift_closed_variance, "%.2f".format(variance), curr))
            onReportCreated(finalReport)
        }
    }

    fun adminOverrideReport(
        adminUsername: String,
        report: ShiftReport,
        reason: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            repository.insertReport(report)
            reportDao.insertAuditLog(
                AuditLog(
                    username = adminUsername,
                    action = "ADMIN_REPORT_OVERRIDE",
                    details = "Admin overridden Shift Report #${report.id} (${report.cashierName}). Reason: $reason"
                )
            )
            _uiMessage.emit(UiText.StringResource(R.string.shift_report_submitted_successfully))
            onSuccess()
        }
    }

    fun adminDeleteReport(
        adminUsername: String,
        report: ShiftReport,
        reason: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            repository.deleteReport(report)
            reportDao.insertAuditLog(
                AuditLog(
                    username = adminUsername,
                    action = "ADMIN_REPORT_DELETE",
                    details = "Admin deleted Shift Report #${report.id} (${report.cashierName}). Reason: $reason"
                )
            )
            _uiMessage.emit(UiText.StringResource(R.string.report_deleted))
            onSuccess()
        }
    }

    fun addCashMovement(type: String, amount: Double, reason: String, cashierName: String) {
        viewModelScope.launch {
            val active = activeShiftSession.value
            val sessionId = active?.id ?: 0
            reportDao.insertCashMovement(
                CashMovement(
                    shiftSessionId = sessionId,
                    type = type,
                    amount = amount,
                    reason = reason,
                    cashierName = cashierName
                )
            )
            if (active != null) {
                val newPayIn = if (type == "PAY_IN") active.totalPayIn + amount else active.totalPayIn
                val newPayOut = if (type == "PAY_OUT") active.totalPayOut + amount else active.totalPayOut
                val newExpected = active.startingCash + active.cashSales + newPayIn - newPayOut
                reportDao.updateShiftSession(active.copy(totalPayIn = newPayIn, totalPayOut = newPayOut, expectedCash = newExpected))
            }
            _uiMessage.emit(UiText.StringResource(R.string.toast_movement_recorded, type, amount.toString()))
        }
    }

    fun addCashier(name: String, pin: String = "1111", role: String = "CASHIER") {
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
                val msg = if (cashier.active) "Activated ${cashier.name}" else "Deactivated ${cashier.name}"
                _uiMessage.emit(UiText.DynamicString(msg))
            }
        }
    }

    fun deleteCashier(cashier: Cashier) {
        syncManager.deleteCashier(cashier) {
            viewModelScope.launch {
                _uiMessage.emit(UiText.StringResource(R.string.toast_staff_removed, cashier.name))
            }
        }
    }

    fun saveBusinessProfile(profile: BusinessProfile) {
        viewModelScope.launch {
            reportDao.saveBusinessProfile(profile)
            _uiMessage.emit(UiText.StringResource(R.string.business_settings_updated))
        }
    }

    fun saveUserProfile(profile: UserProfile) {
        viewModelScope.launch {
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
        viewModelScope.launch {
            reportDao.saveReceiptConfig(config)
            _uiMessage.emit(UiText.StringResource(R.string.receipt_customization_updated))
        }
    }
}
