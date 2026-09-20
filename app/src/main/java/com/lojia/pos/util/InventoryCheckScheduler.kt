package com.lojia.pos.util

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Helper to schedule and manage periodic and one-time WorkManager background execution
 * for inventory level monitoring.
 */
object InventoryCheckScheduler {

    private const val TAG = "InventoryScheduler"

    /**
     * Schedules periodic background execution to check inventory levels.
     * Default interval is 60 minutes.
     */
    fun schedulePeriodicCheck(
        context: Context,
        intervalMinutes: Long = 60L,
        requireBatteryNotLow: Boolean = false
    ) {
        val constraints = Constraints.Builder()
            .apply {
                if (requireBatteryNotLow) setRequiresBatteryNotLow(true)
            }
            .build()

        val periodicWorkRequest = PeriodicWorkRequestBuilder<InventoryCheckWorker>(
            intervalMinutes, TimeUnit.MINUTES,
            15, TimeUnit.MINUTES // 15 min flex interval
        )
            .setConstraints(constraints)
            .addTag(InventoryCheckWorker.TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            InventoryCheckWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWorkRequest
        )
        Log.i(TAG, "Scheduled periodic background inventory check every $intervalMinutes minutes.")
    }

    /**
     * Triggers an immediate one-time inventory level check.
     */
    fun triggerImmediateCheck(context: Context) {
        val oneTimeWorkRequest = OneTimeWorkRequestBuilder<InventoryCheckWorker>()
            .addTag("${InventoryCheckWorker.TAG}_immediate")
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "${InventoryCheckWorker.WORK_NAME}_immediate",
            ExistingWorkPolicy.REPLACE,
            oneTimeWorkRequest
        )
        Log.i(TAG, "Enqueued immediate background inventory check.")
    }

    /**
     * Cancels the periodic inventory background worker.
     */
    fun cancelPeriodicCheck(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(InventoryCheckWorker.WORK_NAME)
        Log.i(TAG, "Cancelled periodic inventory check worker.")
    }
}
