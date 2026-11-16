package com.lojia.shiftreport.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.lojia.shiftreport.data.AppDatabase
import com.lojia.shiftreport.util.AppLogger
import com.lojia.shiftreport.util.OfflineBackupManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Background WorkManager worker that creates encrypted device-tier automatic snapshots
 * (and a portable copy when the backup password vault is available).
 */
class AutoBackupWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getInstance(applicationContext)
            val result = OfflineBackupManager.createAutoBackup(applicationContext, db)
            if (result.isSuccess) {
                Result.success(workDataOf("status" to "SUCCESS"))
            } else {
                val error = result.exceptionOrNull()
                if (error?.message == "device key unavailable") {
                    AppLogger.w(TAG, "Auto-backup skipped: error_code=DEVICE_KEY_UNAVAILABLE")
                    Result.success(workDataOf("status" to "DEVICE_KEY_UNAVAILABLE"))
                } else {
                    AppLogger.w(TAG, "Auto-backup failed: error_code=BACKUP_WRITE_FAILED")
                    if (runAttemptCount < 2) {
                        Result.retry()
                    } else {
                        Result.failure(workDataOf("error_code" to "BACKUP_WRITE_FAILED"))
                    }
                }
            }
        } catch (_: Exception) {
            AppLogger.w(TAG, "Auto-backup failed: error_code=UNEXPECTED_ERROR")
            if (runAttemptCount < 2) {
                Result.retry()
            } else {
                Result.failure(workDataOf("error_code" to "UNEXPECTED_ERROR"))
            }
        }
    }

    companion object {
        private const val TAG = "AutoBackupWorker"
    }
}
