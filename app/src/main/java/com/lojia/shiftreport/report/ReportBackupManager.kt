package com.lojia.shiftreport.report

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import com.lojia.shiftreport.R
import com.lojia.shiftreport.data.*
import com.lojia.shiftreport.sync.FirebaseCloudSyncManager
import com.lojia.shiftreport.sync.FirebaseCloudSyncScheduler
import com.lojia.shiftreport.sync.FirebaseSyncStatusState
import com.lojia.shiftreport.ui.common.*
import com.lojia.shiftreport.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Dedicated manager for Google Drive, Firebase Cloud Sync, and local offline backups.
 * Extracted from ReportViewModel to adhere to Single Responsibility Principle.
 */
class ReportBackupManager(
    private val application: Application,
    private val scope: CoroutineScope,
    private val db: AppDatabase,
    private val reportDao: ReportDao,
    private val syncManager: ConfigurationSyncManager,
    private val prefs: SharedPreferences,
    private val googleAccount: StateFlow<String>,
    private val emitUiMessage: suspend (UiText) -> Unit
) {
    private val context: Context get() = application

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

    val firebaseSyncState: StateFlow<FirebaseSyncStatusState> = FirebaseCloudSyncManager.syncState

    var autoBackupFrequency: MutableStateFlow<String> = MutableStateFlow(
        prefs.getString("auto_backup_frequency", "Daily") ?: "Daily"
    )
    var backupUsingCellular: MutableStateFlow<Boolean> = MutableStateFlow(
        prefs.getBoolean("backup_using_cellular", false)
    )
    var lastBackupTime: MutableStateFlow<Long> = MutableStateFlow(
        prefs.getLong("last_backup_time", 0L)
    )

    var lastServerSyncTime: MutableStateFlow<Long> = MutableStateFlow(
        prefs.getLong("firebase_last_sync_time", System.currentTimeMillis() - 1800000L)
    )
    var lastServerSyncStatus: MutableStateFlow<String> = MutableStateFlow(
        prefs.getString("firebase_last_sync_summary", "Active Periodic Sync (WorkManager)") ?: "Active Periodic Sync (WorkManager)"
    )

    fun refreshDriveAccount() {
        _driveAccountInfo.value = GoogleDriveManager.getAccountInfo(context)
        if (_driveAccountInfo.value.isConnected) {
            fetchGoogleDriveBackups()
        }
    }

    fun connectGoogleDrive(activityContext: Context) {
        val authUrl = GoogleDriveManager.buildAuthUrl(activityContext)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(authUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        activityContext.startActivity(intent)
    }

    fun disconnectGoogleDrive() {
        GoogleDriveManager.disconnect(context)
        _driveAccountInfo.value = GoogleDriveManager.getAccountInfo(context)
        _driveBackupsList.value = emptyList()
        scope.launch {
            emitUiMessage(UiText.StringResource(R.string.drive_not_connected))
        }
    }

    fun handleDriveAuthRedirect(uri: Uri, state: String? = null, onComplete: (Boolean) -> Unit = {}) {
        scope.launch(Dispatchers.IO) {
            val code = uri.getQueryParameter("code")
            val resolvedState = state ?: uri.getQueryParameter("state")
            if (code.isNullOrEmpty()) {
                GoogleDriveManager.clearOAuthState(context)
                val error = uri.getQueryParameter("error") ?: context.getString(R.string.backup_auth_cancelled)
                emitUiMessage(UiText.DynamicString(error))
                withContext(Dispatchers.Main) { onComplete(false) }
                return@launch
            }
            val result = GoogleDriveManager.handleOAuthCallback(context, code, resolvedState)
            result.onSuccess { account: DriveAccountInfo ->
                _driveAccountInfo.value = account
                val emailStr = account.email ?: context.getString(R.string.google_account)
                emitUiMessage(UiText.StringResource(R.string.drive_connected_as, emailStr))
                fetchGoogleDriveBackups()
                withContext(Dispatchers.Main) { onComplete(true) }
            }.onFailure { err ->
                val msg = err.localizedMessage ?: context.getString(R.string.drive_auth_failed)
                emitUiMessage(UiText.DynamicString(msg))
                withContext(Dispatchers.Main) { onComplete(false) }
            }
        }
    }

    fun backupToGoogleDrive(
        onSuccess: (DriveBackupItem) -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        if (_isDriveUploading.value || _isBackingUp.value) return
        scope.launch(Dispatchers.IO) {
            if (!canCreatePortableBackup(context)) {
                val msg = context.getString(R.string.backup_pw_set_first_title)
                emitUiMessage(UiText.DynamicString(msg))
                withContext(Dispatchers.Main) { onFailure(msg) }
                return@launch
            }

            _isDriveUploading.value = true
            _isBackingUp.value = true
            _backupProgress.value = 0.2f
            emitUiMessage(UiText.StringResource(R.string.drive_backup_in_progress))

            val result = GoogleDriveManager.uploadBackup(context, db)

            _isDriveUploading.value = false
            _isBackingUp.value = false

            result.onSuccess { item ->
                val now = System.currentTimeMillis()
                lastBackupTime.value = now
                prefs.edit().putLong("last_backup_time", now).apply()
                emitUiMessage(UiText.StringResource(R.string.drive_backup_success, item.name))
                fetchGoogleDriveBackups()
                withContext(Dispatchers.Main) { onSuccess(item) }
            }.onFailure { err ->
                val msg = err.localizedMessage ?: context.getString(R.string.drive_upload_failed)
                emitUiMessage(UiText.DynamicString(msg))
                withContext(Dispatchers.Main) { onFailure(msg) }
            }
        }
    }

    fun fetchGoogleDriveBackups(
        onSuccess: (List<DriveBackupItem>) -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        scope.launch(Dispatchers.IO) {
            _isDriveLoadingList.value = true
            val result = GoogleDriveManager.listBackups(context)
            _isDriveLoadingList.value = false

            result.onSuccess { list ->
                _driveBackupsList.value = list
                withContext(Dispatchers.Main) { onSuccess(list) }
            }.onFailure { err ->
                val msg = err.localizedMessage ?: context.getString(R.string.drive_list_failed)
                withContext(Dispatchers.Main) { onFailure(msg) }
            }
        }
    }

    fun prepareGoogleDriveRestore(
        item: DriveBackupItem,
        onReady: (bytes: ByteArray, requiresPassword: Boolean) -> Unit,
        onFailure: (String) -> Unit = {}
    ) {
        if (_isDriveDownloading.value) return
        scope.launch(Dispatchers.IO) {
            _isDriveDownloading.value = true
            _isBackingUp.value = true
            _backupProgress.value = 0.25f
            emitUiMessage(UiText.StringResource(R.string.drive_restore_in_progress))

            val downloadResult = GoogleDriveManager.downloadBackupBytes(context, item.id)
            _isDriveDownloading.value = false
            _isBackingUp.value = false

            downloadResult.onSuccess { bytes ->
                val requiresPassword = backupTierOf(bytes) == TIER_PORTABLE
                withContext(Dispatchers.Main) {
                    onReady(bytes, requiresPassword)
                }
            }.onFailure { err ->
                val msg = err.localizedMessage ?: context.getString(R.string.drive_download_failed)
                emitUiMessage(UiText.DynamicString(msg))
                withContext(Dispatchers.Main) { onFailure(msg) }
            }
        }
    }

    fun restoreGoogleDriveBytes(
        item: DriveBackupItem,
        bytes: ByteArray,
        password: String? = null,
        onSuccess: (Int) -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        if (_isDriveDownloading.value) return
        scope.launch(Dispatchers.IO) {
            _isDriveDownloading.value = true
            _isBackingUp.value = true
            _backupProgress.value = 0.6f

            val restoreResult = try {
                val jsonString = OfflineBackupManager.decodeBackupToJson(bytes, password)
                OfflineBackupManager.restoreBackupFromString(jsonString, db, item.name)
            } catch (e: Exception) {
                Result.failure(e)
            }

            _isDriveDownloading.value = false
            _isBackingUp.value = false
            _backupProgress.value = 1.0f

            restoreResult.onSuccess { count ->
                val now = System.currentTimeMillis()
                lastBackupTime.value = now
                prefs.edit().putLong("last_backup_time", now).apply()
                syncManager.forceSync { _: Boolean, _: String -> }
                NotificationHelper.sendTestPushNotification(
                    context,
                    context.getString(R.string.drive_restore_complete_notif_title),
                    context.getString(R.string.drive_restore_complete_notif_body, count, item.name)
                )
                withContext(Dispatchers.Main) { onSuccess(count) }
            }.onFailure { err ->
                val msg = OfflineBackupManager.resolveRestoreErrorMessage(context, err)
                emitUiMessage(UiText.DynamicString(msg))
                withContext(Dispatchers.Main) { onFailure(msg) }
            }
        }
    }

    fun restoreFromGoogleDrive(
        item: DriveBackupItem,
        password: String? = null,
        onSuccess: (Int) -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        if (_isDriveDownloading.value) return
        scope.launch(Dispatchers.IO) {
            _isDriveDownloading.value = true
            _isBackingUp.value = true
            _backupProgress.value = 0.3f
            emitUiMessage(UiText.StringResource(R.string.drive_restore_in_progress))

            val downloadResult = GoogleDriveManager.downloadBackupBytes(context, item.id)

            downloadResult.onSuccess { bytes ->
                val restoreResult = try {
                    val jsonString = OfflineBackupManager.decodeBackupToJson(bytes, password)
                    OfflineBackupManager.restoreBackupFromString(jsonString, db, item.name)
                } catch (e: Exception) {
                    Result.failure(e)
                }
                _isDriveDownloading.value = false
                _isBackingUp.value = false

                restoreResult.onSuccess { count ->
                    val now = System.currentTimeMillis()
                    lastBackupTime.value = now
                    prefs.edit().putLong("last_backup_time", now).apply()
                    syncManager.forceSync { _: Boolean, _: String -> }
                    NotificationHelper.sendTestPushNotification(
                        context,
                        context.getString(R.string.drive_restore_complete_notif_title),
                        context.getString(R.string.drive_restore_complete_notif_body, count, item.name)
                    )
                    withContext(Dispatchers.Main) { onSuccess(count) }
                }.onFailure { err ->
                    val msg = OfflineBackupManager.resolveRestoreErrorMessage(context, err)
                    emitUiMessage(UiText.DynamicString(msg))
                    withContext(Dispatchers.Main) { onFailure(msg) }
                }
            }.onFailure { err ->
                _isDriveDownloading.value = false
                _isBackingUp.value = false
                val msg = err.localizedMessage ?: context.getString(R.string.drive_download_failed)
                emitUiMessage(UiText.DynamicString(msg))
                withContext(Dispatchers.Main) { onFailure(msg) }
            }
        }
    }

    fun parseDriveBackupSummary(
        item: DriveBackupItem,
        onResult: (BackupSummary?) -> Unit
    ) {
        scope.launch(Dispatchers.IO) {
            val downloadResult = GoogleDriveManager.downloadBackupBytes(context, item.id)
            val summary = downloadResult.mapCatching { bytes ->
                OfflineBackupManager.parseBackupSummaryFromBytes(bytes).getOrThrow()
            }.getOrNull()
            withContext(Dispatchers.Main) { onResult(summary) }
        }
    }

    fun toggleFirebaseCloudSync(enabled: Boolean) {
        FirebaseCloudSyncManager.setSyncEnabled(context, enabled)
        if (enabled) {
            FirebaseCloudSyncScheduler.schedulePeriodicSync(context, intervalMinutes = 30)
            syncFirebaseNow()
        } else {
            FirebaseCloudSyncScheduler.cancelPeriodicSync(context)
        }
    }

    fun syncFirebaseNow(
        onSuccess: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        scope.launch(Dispatchers.IO) {
            val result = FirebaseCloudSyncManager.performSync(context, isManual = true)
            if (result.success) {
                emitUiMessage(UiText.DynamicString(result.message))
                withContext(Dispatchers.Main) { onSuccess() }
            } else {
                emitUiMessage(UiText.DynamicString(result.message))
                withContext(Dispatchers.Main) { onFailure(result.message) }
            }
        }
    }

    fun updateFirebaseCustomUrl(url: String) {
        FirebaseCloudSyncManager.setCustomFirebaseUrl(context, url)
    }

    fun triggerServerSyncNow() {
        FirebaseCloudSyncScheduler.triggerImmediateSync(application)
        scope.launch {
            delay(1200)
            lastServerSyncTime.value = System.currentTimeMillis()
            lastServerSyncStatus.value = context.getString(R.string.sync_status_workmanager)
        }
    }

    fun updatePeriodicSyncInterval(intervalMinutes: Long, wifiOnly: Boolean = false) {
        FirebaseCloudSyncScheduler.schedulePeriodicSync(
            application,
            intervalMinutes = intervalMinutes
        )
    }

    fun triggerCloudBackup() {
        if (_isBackingUp.value) return
        scope.launch(Dispatchers.IO) {
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
            NotificationHelper.sendTestPushNotification(
                context,
                context.getString(R.string.cloud_backup_success_notif_title),
                context.getString(
                    R.string.cloud_backup_success_notif_body,
                    SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(now))
                )
            )
        }
    }

    fun createOfflineBackup(
        onSuccess: (BackupInfo) -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        if (_isBackingUp.value) return
        scope.launch(Dispatchers.IO) {
            if (!canCreatePortableBackup(context)) {
                val msg = context.getString(R.string.backup_pw_set_first_title)
                emitUiMessage(UiText.DynamicString(msg))
                withContext(Dispatchers.Main) { onFailure(msg) }
                return@launch
            }

            _isBackingUp.value = true
            _backupProgress.value = 0.1f
            emitUiMessage(UiText.StringResource(R.string.backup_in_progress))

            delay(200)
            _backupProgress.value = 0.4f

            val result = withVaultPassword(context) { passwordChars ->
                OfflineBackupManager.createBackup(context, db, passwordChars).getOrThrow()
            }

            delay(200)
            _backupProgress.value = 1.0f
            _isBackingUp.value = false

            result.onSuccess { info ->
                val now = System.currentTimeMillis()
                lastBackupTime.value = now
                prefs.edit().putLong("last_backup_time", now).apply()
                emitUiMessage(UiText.StringResource(R.string.backup_created_success, info.fileName))
                withContext(Dispatchers.Main) { onSuccess(info) }
            }.onFailure { error ->
                val msg = error.localizedMessage ?: context.getString(R.string.backup_create_failed)
                emitUiMessage(UiText.DynamicString(msg))
                withContext(Dispatchers.Main) { onFailure(msg) }
            }
        }
    }

    fun parseBackupSummary(
        uri: Uri,
        onResult: (BackupSummary?) -> Unit
    ) {
        scope.launch(Dispatchers.IO) {
            val summaryResult = OfflineBackupManager.parseBackupSummary(context, uri)
            val summary = summaryResult.getOrNull()
            withContext(Dispatchers.Main) { onResult(summary) }
        }
    }

    fun restoreOfflineBackup(
        uri: Uri,
        password: String? = null,
        onSuccess: (Int) -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        if (_isBackingUp.value) return
        scope.launch(Dispatchers.IO) {
            _isBackingUp.value = true
            _backupProgress.value = 0.2f

            delay(300)
            _backupProgress.value = 0.6f

            val result = OfflineBackupManager.restoreBackup(context, uri, db, password = password)

            _backupProgress.value = 1.0f
            _isBackingUp.value = false

            result.onSuccess { count ->
                val now = System.currentTimeMillis()
                lastBackupTime.value = now
                prefs.edit().putLong("last_backup_time", now).apply()
                syncManager.forceSync { _: Boolean, _: String -> }
                NotificationHelper.sendTestPushNotification(
                    context,
                    context.getString(R.string.backup_restored_notif_title),
                    context.getString(R.string.backup_restored_notif_body, count)
                )
                withContext(Dispatchers.Main) { onSuccess(count) }
            }.onFailure { error ->
                val msg = OfflineBackupManager.resolveRestoreErrorMessage(context, error)
                emitUiMessage(UiText.DynamicString(msg))
                withContext(Dispatchers.Main) { onFailure(msg) }
            }
        }
    }

    fun clearAllBusinessData(
        onSuccess: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        scope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.IO) {
                    db.clearAllTables()
                    val defaultCashier = Cashier(
                        id = 0,
                        name = "Lojia Admin",
                        role = "ADMIN",
                        isActive = true
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
                emitUiMessage(UiText.StringResource(R.string.all_data_cleared_success))
                withContext(Dispatchers.Main) { onSuccess() }
            } catch (e: Exception) {
                val err = e.localizedMessage ?: context.getString(R.string.clear_data_failed)
                emitUiMessage(UiText.DynamicString(err))
                withContext(Dispatchers.Main) { onFailure(err) }
            }
        }
    }
}
