package com.lojia.shiftreport.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Schedules periodic and one-time device backup snapshots via WorkManager
 * without network or charging constraints.
 */
object AutoBackupScheduler {
    private const val UNIQUE_PERIODIC_AUTO_BACKUP = "lojia_periodic_auto_backup_work"
    private const val UNIQUE_ONE_TIME_AUTO_BACKUP = "lojia_one_time_auto_backup_work"

    fun schedulePeriodicAutoBackup(context: Context) {
        try {
            val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(12L, TimeUnit.HOURS)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30L, TimeUnit.SECONDS)
                .addTag("auto_backup_periodic")
                .build()

            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC_AUTO_BACKUP,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        } catch (_: Exception) {
            // Safe guard for JVM unit test environments without initialized WorkManager.
        }
    }

    fun enqueueOneTimeAutoBackup(context: Context) {
        try {
            val request = OneTimeWorkRequestBuilder<AutoBackupWorker>()
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15L, TimeUnit.SECONDS)
                .addTag("auto_backup_one_time")
                .build()

            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                UNIQUE_ONE_TIME_AUTO_BACKUP,
                ExistingWorkPolicy.REPLACE,
                request
            )
        } catch (_: Exception) {
            // Safe guard for JVM unit test environments without initialized WorkManager.
        }
    }
}
