package com.lojia.shiftreport.sync

import android.content.Context
import androidx.work.*
import com.lojia.shiftreport.util.AppLogger
import java.util.concurrent.TimeUnit

/**
 * Utility to schedule and manage periodic background Firebase Cloud Sync with WorkManager.
 */
object FirebaseCloudSyncScheduler {
    private const val TAG = "FirebaseSyncScheduler"
    private const val UNIQUE_PERIODIC_WORK_NAME = "periodic_firebase_cloud_sync_work"
    private const val UNIQUE_ONE_TIME_WORK_NAME = "immediate_firebase_cloud_sync_work"

    /**
     * Schedules periodic background cloud sync.
     * Enforces network connectivity constraint so it only runs when online.
     */
    fun schedulePeriodicSync(
        context: Context,
        intervalMinutes: Long = 30L
    ) {
        if (!com.lojia.shiftreport.BuildConfig.CLOUD_SYNC_ENABLED) {
            AppLogger.i(TAG, "Cloud sync is disabled in BuildConfig. Skipping periodic sync scheduling.")
            return
        }

        val effectiveInterval = maxOf(intervalMinutes, 15L) // WorkManager minimum is 15 mins

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val periodicWorkRequest = PeriodicWorkRequestBuilder<FirebaseCloudSyncWorker>(
            effectiveInterval, TimeUnit.MINUTES,
            5L, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                30L,
                TimeUnit.SECONDS
            )
            .addTag("firebase_cloud_sync")
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicWorkRequest
        )

        AppLogger.i(TAG, "Periodic Firebase Cloud Sync scheduled every $effectiveInterval minutes (requires network).")
    }

    /**
     * Cancels any scheduled periodic background sync tasks.
     * Called when the user turns Sync OFF from Settings.
     */
    fun cancelPeriodicSync(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_PERIODIC_WORK_NAME)
        AppLogger.i(TAG, "Periodic Firebase Cloud Sync cancelled.")
    }

    /**
     * Triggers an immediate one-time background sync task.
     */
    fun triggerImmediateSync(context: Context): Operation? {
        if (!com.lojia.shiftreport.BuildConfig.CLOUD_SYNC_ENABLED) {
            AppLogger.i(TAG, "Cloud sync is disabled in BuildConfig. Skipping immediate sync.")
            return null
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val oneTimeWorkRequest = OneTimeWorkRequestBuilder<FirebaseCloudSyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15L, TimeUnit.SECONDS)
            .addTag("firebase_cloud_sync_immediate")
            .build()

        val operation = WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_ONE_TIME_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            oneTimeWorkRequest
        )

        AppLogger.i(TAG, "Immediate Firebase Cloud Sync triggered.")
        return operation
    }
}
