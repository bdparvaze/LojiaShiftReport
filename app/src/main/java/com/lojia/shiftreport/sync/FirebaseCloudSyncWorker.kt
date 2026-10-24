package com.lojia.shiftreport.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.lojia.shiftreport.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Background WorkManager worker for periodic and opportunistic Firebase Cloud Sync.
 * Runs only when network is connected, never blocks POS checkout or local operations.
 */
class FirebaseCloudSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val appContext = applicationContext

        // Only proceed if cloud sync is enabled in BuildConfig and by the user in Settings
        if (!com.lojia.shiftreport.BuildConfig.CLOUD_SYNC_ENABLED || !FirebaseCloudSyncManager.isSyncEnabled(appContext)) {
            AppLogger.d(TAG, "Cloud sync is disabled. Worker exiting early.")
            return@withContext Result.success(workDataOf("status" to "DISABLED"))
        }

        try {
            AppLogger.d(TAG, "Starting periodic background Firebase Cloud Sync...")
            val syncResult = FirebaseCloudSyncManager.performSync(appContext, isManual = false)

            AppLogger.i(TAG, "Background Firebase Cloud Sync completed: ${syncResult.message}")

            Result.success(
                workDataOf(
                    "status" to if (syncResult.success) "SUCCESS" else "CLOUD_DOWN",
                    "message" to syncResult.message,
                    "reports" to syncResult.reportsSynced,
                    "timestamp" to System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error in FirebaseCloudSyncWorker: ${e.message}", e)
            if (runAttemptCount < 2) {
                Result.retry()
            } else {
                Result.failure(workDataOf("error" to (e.localizedMessage ?: "Unknown sync error")))
            }
        }
    }

    companion object {
        const val TAG = "FirebaseCloudSyncWorker"
    }
}
