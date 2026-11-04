package com.lojia.shiftreport.settings.backup

import android.content.Context
import android.os.Environment
import com.lojia.shiftreport.util.AppLogger
import com.lojia.shiftreport.util.OfflineBackupManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Offline-first local on-device backup provider.
 * Stores encrypted POS shift backups in dedicated app storage.
 * Works 100% offline with zero network connectivity required.
 * Completely immune to storage permission issues or SecurityExceptions on Android 10+.
 */
class LocalBackupProvider(private val context: Context) : BackupProvider {

    companion object {
        private const val TAG = "LocalBackupProvider"
    }

    override val displayName: String = "Device Storage (Offline)"
    override val location: BackupLocation = BackupLocation.LOCAL

    private val prefs = context.getSharedPreferences(BackupPreferences.PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Resolves the primary local storage directory with robust fallback order.
     * Guaranteed never to throw SecurityException or crash.
     */
    fun getBackupDirectory(): File {
        return try {
            // Primary: App reports backups directory (registered in FileProvider)
            val baseBackups = OfflineBackupManager.getBackupsDirectory(context)
            if (baseBackups.exists() || baseBackups.mkdirs()) {
                return baseBackups
            }

            // Fallback 1: App-specific external documents folder
            val extDocs = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "backups")
            if (extDocs.exists() || extDocs.mkdirs()) {
                return extDocs
            }

            // Fallback 2: Internal app files directory
            val internalDir = File(context.filesDir, "backups")
            if (!internalDir.exists()) {
                internalDir.mkdirs()
            }
            internalDir
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error resolving backup directory: ${e.message}", e)
            context.filesDir
        }
    }

    override fun isConnected(): Boolean = true // Always available on device

    override fun getLastBackupTime(): Long {
        return try {
            val recordedTime = prefs.getLong(BackupPreferences.KEY_LOCAL_LAST_BACKUP, 0L)
            if (recordedTime > 0L) return recordedTime

            // Fallback to latest file modification time
            val latest = getLatestBackupFile()
            latest?.lastModified() ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    override suspend fun backup(file: File): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val targetDir = getBackupDirectory()
            val destFile = File(targetDir, file.name)

            FileInputStream(file).use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            val now = System.currentTimeMillis()
            prefs.edit().putLong(BackupPreferences.KEY_LOCAL_LAST_BACKUP, now).apply()
            AppLogger.i(TAG, "Local backup saved successfully to ${destFile.absolutePath}")
            Result.success(true)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to perform local backup: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun restore(): File? = withContext(Dispatchers.IO) {
        getLatestBackupFile()
    }

    fun getLatestBackupFile(): File? {
        return try {
            val dir = getBackupDirectory()
            dir.listFiles { f -> f.isFile && f.name.endsWith(".json") }
                ?.maxByOrNull { it.lastModified() }
        } catch (e: Exception) {
            null
        }
    }

    fun listBackupFiles(): List<File> {
        return try {
            val dir = getBackupDirectory()
            dir.listFiles { f -> f.isFile && f.name.endsWith(".json") }
                ?.sortedByDescending { it.lastModified() } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getTotalBackupSize(): Long {
        return try {
            listBackupFiles().sumOf { it.length() }
        } catch (e: Exception) {
            0L
        }
    }
}
