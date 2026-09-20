package com.lojia.pos.util


import android.content.Context

import android.util.Log


import androidx.lifecycle.asFlow


import androidx.work.*


import kotlinx.coroutines.flow.Flow

import java.util.concurrent.TimeUnit

/**
 * Utility to schedule and manage periodic background sync tasks with WorkManager.
 */
object ShiftReportSyncScheduler {
    private const val TAG = "ShiftSyncScheduler"
    private const val UNIQUE_PERIODIC_WORK_NAME = "periodic_shift_report_sync_work"
    private const val UNIQUE_ONE_TIME_WORK_NAME = "immediate_shift_report_sync_work"

    /**
     * Schedules periodic shift report synchronization.
     * Minimum interval supported by Android WorkManager is 15 minutes.
     *
     * @param context Application context
     * @param intervalMinutes Interval between sync executions in minutes (minimum 15 mins)
     * @param requireWifiOnly If true, requires unmetered network connection
     * @param requireCharging If true, only sync when device is plugged into power
     */
    fun schedulePeriodicSync(
        context: Context,
        intervalMinutes: Long = 60L,
        requireWifiOnly: Boolean = false,
        requireCharging: Boolean = false
    ) {
        val effectiveInterval = maxOf(intervalMinutes, 15L)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(
                if (requireWifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED
            )
            .setRequiresCharging(requireCharging)
            .setRequiresBatteryNotLow(true)
            .build()

        val periodicWorkRequest = PeriodicWorkRequestBuilder<ShiftReportSyncWorker>(
            effectiveInterval, TimeUnit.MINUTES,
            // 5-minute flex period before interval boundary
            5L, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                15L,
                TimeUnit.SECONDS
            )
            .addTag("shift_report_sync")
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicWorkRequest
        )

        Log.i(TAG, "Periodic shift report sync scheduled: every $effectiveInterval minutes (WiFi-only=$requireWifiOnly).")
    }

    /**
     * Triggers an immediate one-time background sync task.
     */
    fun triggerImmediateSync(context: Context, requireWifiOnly: Boolean = false): Operation {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(
                if (requireWifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED
            )
            .build()

        val oneTimeWorkRequest = OneTimeWorkRequestBuilder<ShiftReportSyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10L, TimeUnit.SECONDS)
            .addTag("shift_report_sync_immediate")
            .build()

        val operation = WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_ONE_TIME_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            oneTimeWorkRequest
        )

        Log.i(TAG, "Immediate one-time shift report sync triggered.")
        return operation
    }

    /**
     * Cancels all scheduled periodic sync tasks.
     */
    fun cancelSync(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_PERIODIC_WORK_NAME)
        Log.i(TAG, "Periodic shift report sync cancelled.")
    }

    /**
     * Observes periodic work status as a Kotlin Flow.
     */
    fun observePeriodicWorkInfo(context: Context): Flow<List<WorkInfo>> {
        return WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkLiveData(UNIQUE_PERIODIC_WORK_NAME)
            .asFlow()
    }
}
