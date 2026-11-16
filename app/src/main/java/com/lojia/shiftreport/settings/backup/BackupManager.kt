package com.lojia.shiftreport.settings.backup

import android.content.Context
import com.lojia.shiftreport.data.AppDatabase
import com.lojia.shiftreport.util.AppLogger
import com.lojia.shiftreport.util.BackupPasswordRequiredException
import com.lojia.shiftreport.util.OfflineBackupManager
import com.lojia.shiftreport.util.canCreatePortableBackup
import com.lojia.shiftreport.util.withVaultPassword
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Universal multi-cloud backup orchestrator for Lojia POS.
 * Coordinates Local Storage, Google Drive, and Microsoft OneDrive destinations.
 * Supports WhatsApp-like "Both" mode (Local Device + Cloud simultaneously).
 */
class BackupManager(private val context: Context) {

    companion object {
        private const val TAG = "BackupManager"

        @Volatile
        private var INSTANCE: BackupManager? = null

        fun getInstance(context: Context): BackupManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BackupManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val prefs = context.getSharedPreferences(BackupPreferences.PREFS_NAME, Context.MODE_PRIVATE)

    val localProvider = LocalBackupProvider(context)
    val googleDriveProvider = GoogleDriveBackupProvider(context)
    val oneDriveProvider = OneDriveBackupProvider(context)

    fun getProvider(location: BackupLocation): BackupProvider {
        return when (location) {
            BackupLocation.LOCAL -> localProvider
            BackupLocation.GOOGLE_DRIVE -> googleDriveProvider
            BackupLocation.ONEDRIVE -> oneDriveProvider
            BackupLocation.BOTH -> localProvider
        }
    }

    fun getSelectedLocation(): BackupLocation {
        val id = prefs.getString(BackupPreferences.KEY_SELECTED_LOCATION, BackupLocation.LOCAL.id)
        return BackupLocation.fromId(id)
    }

    fun setSelectedLocation(location: BackupLocation) {
        prefs.edit().putString(BackupPreferences.KEY_SELECTED_LOCATION, location.id).apply()
        AppLogger.i(TAG, "Selected backup location updated to: ${location.name}")
    }

    fun isAutoBackupEnabled(): Boolean {
        return prefs.getBoolean(BackupPreferences.KEY_AUTO_BACKUP_ENABLED, false)
    }

    fun setAutoBackupEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(BackupPreferences.KEY_AUTO_BACKUP_ENABLED, enabled).apply()
    }

    fun getAutoBackupFrequency(): String {
        return prefs.getString(BackupPreferences.KEY_AUTO_BACKUP_FREQUENCY, "daily") ?: "daily"
    }

    fun setAutoBackupFrequency(frequency: String) {
        prefs.edit().putString(BackupPreferences.KEY_AUTO_BACKUP_FREQUENCY, frequency).apply()
    }

    /**
     * Executes backup across chosen location(s).
     * Requires a backup password vault and always emits portable LSB2 payloads.
     */
    suspend fun backupNow(
        location: BackupLocation = getSelectedLocation(),
        database: AppDatabase = AppDatabase.getInstance(context)
    ): Map<BackupLocation, Result<Boolean>> = withContext(Dispatchers.IO) {
        val results = mutableMapOf<BackupLocation, Result<Boolean>>()
        AppLogger.i(TAG, "Starting backup execution for location: ${location.name}")

        var tempFile: File? = null
        try {
            if (!canCreatePortableBackup(context)) {
                val err = BackupPasswordRequiredException("Set a backup password first")
                results[location] = Result.failure(err)
                return@withContext results
            }

            val encryptedPair = withVaultPassword(context) { vaultPassword ->
                OfflineBackupManager.createPortableBackupBytes(
                    database = database,
                    password = vaultPassword,
                    isCloudBackup = (location != BackupLocation.LOCAL)
                )
            }.getOrElse { err ->
                results[location] = Result.failure(err)
                return@withContext results
            }

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val outFile = File(context.cacheDir, "lojia_backup_$timeStamp.lsb")
            tempFile = outFile
            FileOutputStream(outFile).use { out ->
                out.write(encryptedPair.first)
            }

            // Dispatch encrypted LSB2 archive to destination provider(s)
            when (location) {
                BackupLocation.LOCAL -> {
                    results[BackupLocation.LOCAL] = localProvider.backup(outFile)
                }

                BackupLocation.GOOGLE_DRIVE -> {
                    results[BackupLocation.GOOGLE_DRIVE] = googleDriveProvider.backup(outFile)
                }

                BackupLocation.ONEDRIVE -> {
                    results[BackupLocation.ONEDRIVE] = oneDriveProvider.backup(outFile)
                }

                BackupLocation.BOTH -> {
                    results[BackupLocation.LOCAL] = localProvider.backup(outFile)

                    if (googleDriveProvider.isConnected()) {
                        results[BackupLocation.GOOGLE_DRIVE] = googleDriveProvider.backup(outFile)
                    }

                    if (oneDriveProvider.isConnected()) {
                        results[BackupLocation.ONEDRIVE] = oneDriveProvider.backup(outFile)
                    }
                }
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Global backup failure: ${e.message}", e)
            results[location] = Result.failure(e)
        } finally {
            tempFile?.delete()
        }

        results
    }

    /**
     * Resolves the latest backup file from the specified destination for preview and restoration.
     */
    suspend fun restoreLatest(location: BackupLocation = getSelectedLocation()): File? = withContext(Dispatchers.IO) {
        when (location) {
            BackupLocation.LOCAL -> localProvider.restore()
            BackupLocation.GOOGLE_DRIVE -> googleDriveProvider.restore()
            BackupLocation.ONEDRIVE -> oneDriveProvider.restore()
            BackupLocation.BOTH -> {
                // Try local first, fall back to Google Drive or OneDrive
                localProvider.restore()
                    ?: (if (googleDriveProvider.isConnected()) googleDriveProvider.restore() else null)
                    ?: (if (oneDriveProvider.isConnected()) oneDriveProvider.restore() else null)
            }
        }
    }

    fun getBackupStatus(location: BackupLocation): BackupStatus {
        val provider = getProvider(location)
        val lastTime = provider.getLastBackupTime()
        val isConn = provider.isConnected()
        val fileSize = if (location == BackupLocation.LOCAL) localProvider.getTotalBackupSize() else 0L

        return BackupStatus(
            location = location,
            lastBackupTime = lastTime,
            fileSize = fileSize,
            isSuccess = lastTime > 0L,
            isConnected = isConn
        )
    }

    /**
     * Returns the latest on-device backup file, or null if none exist.
     */
    fun getLatestLocalBackupFile(): File? {
        return localProvider.getLatestBackupFile()
    }
}
