package com.lojia.shiftreport.settings.backup

import android.content.Context
import com.lojia.shiftreport.util.AppLogger
import com.lojia.shiftreport.util.GoogleDriveManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Google Drive Cloud backup provider.
 * Wraps GoogleDriveManager to store backups in the user's Google Drive app space.
 */
class GoogleDriveBackupProvider(private val context: Context) : BackupProvider {

    companion object {
        private const val TAG = "GoogleDriveBackupProvider"
    }

    override val displayName: String = "Google Drive"
    override val location: BackupLocation = BackupLocation.GOOGLE_DRIVE

    private val prefs = context.getSharedPreferences(BackupPreferences.PREFS_NAME, Context.MODE_PRIVATE)

    override fun isConnected(): Boolean {
        return try {
            GoogleDriveManager.getAccountInfo(context).isConnected
        } catch (_: Exception) {
            false
        }
    }

    override fun getLastBackupTime(): Long {
        return try {
            prefs.getLong(BackupPreferences.KEY_DRIVE_LAST_BACKUP, 0L)
        } catch (_: Exception) {
            0L
        }
    }

    override suspend fun backup(file: File): Result<Boolean> = withContext(Dispatchers.IO) {
        if (!isConnected()) {
            return@withContext Result.failure(IllegalStateException("Google Drive account is not connected."))
        }
        if (!com.lojia.shiftreport.util.canCreatePortableBackup(context)) {
            return@withContext Result.failure(com.lojia.shiftreport.util.BackupPasswordRequiredException("Set a backup password first"))
        }

        try {
            val uploadResult = if (file.exists()) {
                val bytes = file.readBytes()
                if (com.lojia.shiftreport.util.backupTierOf(bytes) == com.lojia.shiftreport.util.TIER_PORTABLE) {
                    GoogleDriveManager.uploadBackupBytes(context, bytes, totalRecords = 0)
                } else {
                    val database = com.lojia.shiftreport.data.AppDatabase.getInstance(context)
                    GoogleDriveManager.uploadBackup(context = context, database = database)
                }
            } else {
                val database = com.lojia.shiftreport.data.AppDatabase.getInstance(context)
                GoogleDriveManager.uploadBackup(context = context, database = database)
            }

            if (uploadResult.isSuccess) {
                val now = System.currentTimeMillis()
                prefs.edit().putLong(BackupPreferences.KEY_DRIVE_LAST_BACKUP, now).apply()
                AppLogger.i(TAG, "Uploaded backup to Google Drive successfully.")
                Result.success(true)
            } else {
                Result.failure(uploadResult.exceptionOrNull() ?: Exception("Google Drive upload failed."))
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Google Drive backup exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun restore(): File? = withContext(Dispatchers.IO) {
        if (!isConnected()) return@withContext null

        try {
            val backupsResult = GoogleDriveManager.listBackups(context)
            val list = backupsResult.getOrNull() ?: return@withContext null
            val latestItem = list.firstOrNull() ?: return@withContext null

            val downloadResult = GoogleDriveManager.downloadBackupBytes(context, latestItem.id)
            val payloadBytes = downloadResult.getOrNull() ?: return@withContext null

            val cacheFile = File(context.cacheDir, "drive_restore_${latestItem.name}")
            FileOutputStream(cacheFile).use { out ->
                out.write(payloadBytes)
            }
            cacheFile
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to restore from Google Drive: ${e.message}", e)
            null
        }
    }
}
