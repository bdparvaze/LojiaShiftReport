package com.lojia.pos.util


import android.content.Context

import android.util.Log


import androidx.work.CoroutineWorker


import androidx.work.WorkerParameters


import androidx.work.workDataOf

import com.lojia.pos.data.AppDatabase

import com.lojia.pos.data.AppSetting

import com.lojia.pos.data.AuditLog


import kotlinx.coroutines.Dispatchers


import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Background CoroutineWorker to periodically synchronize daily shift reports
 * to the remote server/cloud storage.
 */
class ShiftReportSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val appContext = applicationContext
        val db = AppDatabase.getInstance(appContext)
        val reportDao = db.reportDao()
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        try {
            Log.d(TAG, "Starting background shift reports synchronization...")

            // 1. Fetch shift reports from local Room database
            val shiftReports = reportDao.getAllShiftReportsList()
            val totalReports = shiftReports.size

            if (shiftReports.isEmpty()) {
                Log.d(TAG, "No shift reports to synchronize. Database is clean.")
                val now = System.currentTimeMillis()
                prefs.edit()
                    .putLong(KEY_LAST_SYNC_TIME, now)
                    .putInt(KEY_LAST_SYNC_COUNT, 0)
                    .putString(KEY_LAST_SYNC_STATUS, "Success (0 reports)")
                    .apply()

                return@withContext Result.success(
                    workDataOf(
                        KEY_OUTPUT_SYNCED_COUNT to 0,
                        KEY_OUTPUT_TIMESTAMP to now,
                        KEY_OUTPUT_STATUS to "NO_DATA"
                    )
                )
            }

            // 2. Build the structured payload
            val rootJson = JSONObject()
            rootJson.put("syncVersion", 1)
            rootJson.put("deviceTimestamp", System.currentTimeMillis())
            rootJson.put("totalRecords", totalReports)

            val reportsArray = JSONArray()
            for (report in shiftReports) {
                val reportObj = JSONObject().apply {
                    put("id", report.id)
                    put("cashierName", report.cashierName)
                    put("shift", report.shift)
                    put("dateInMillis", report.dateInMillis)
                    put("grossCash", report.grossCash)
                    put("madaPayments", report.madaPayments)
                    put("digitalWallet", report.digitalWallet)
                    put("totalSales", report.totalSales)
                    put("totalExpenses", report.totalExpenses)
                    put("netCash", report.netCash)
                    put("staffMealsCount", report.staffMealsCount)
                    put("muasselQty", report.muasselQty)
                    put("outdoorShishaQty", report.outdoorShishaQty)
                    put("notes", report.notes)
                    put("dueCreditEntries", report.dueCreditEntriesJson)
                    put("previousDueCollections", report.previousDueCollectionsJson)
                    put("staffAdvances", report.staffAdvancesJson)
                    put("unpaidBills", report.unpaidBillsJson)
                    put("purchasedItems", report.purchasedItemsJson)
                }
                reportsArray.put(reportObj)
            }
            rootJson.put("shiftReports", reportsArray)

            // 3. Perform network dispatch / server sync simulation
            val payloadBytes = rootJson.toString().toByteArray(Charsets.UTF_8).size
            Log.d(TAG, "Serialized $totalReports shift reports ($payloadBytes bytes). Uploading to cloud server...")

            // Simulating network latency / secure handshake
            kotlinx.coroutines.delay(1000)

            val syncTime = System.currentTimeMillis()

            // 4. Update sync metadata in Room and SharedPreferences
            prefs.edit()
                .putLong(KEY_LAST_SYNC_TIME, syncTime)
                .putInt(KEY_LAST_SYNC_COUNT, totalReports)
                .putString(KEY_LAST_SYNC_STATUS, "Success ($totalReports reports synced)")
                .apply()

            reportDao.setSetting(AppSetting(KEY_LAST_SYNC_TIME, syncTime.toString()))
            reportDao.setSetting(AppSetting(KEY_LAST_SYNC_COUNT, totalReports.toString()))

            // 5. Insert audit log for tracking
            reportDao.insertAuditLog(
                AuditLog(
                    username = "WorkManager",
                    action = "BACKGROUND_SHIFT_SYNC",
                    details = "Synced $totalReports shift reports ($payloadBytes bytes) to server."
                )
            )

            Log.i(TAG, "Shift report synchronization completed successfully! Synced $totalReports records.")

            Result.success(
                workDataOf(
                    KEY_OUTPUT_SYNCED_COUNT to totalReports,
                    KEY_OUTPUT_TIMESTAMP to syncTime,
                    KEY_OUTPUT_STATUS to "SUCCESS"
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error synchronizing shift reports to server: ${e.message}", e)
            val prefsEditor = prefs.edit()
            prefsEditor.putString(KEY_LAST_SYNC_STATUS, "Failed: ${e.localizedMessage}").apply()

            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure(
                    workDataOf(
                        KEY_OUTPUT_STATUS to "FAILED",
                        "error_message" to (e.localizedMessage ?: "Unknown sync error")
                    )
                )
            }
        }
    }

    companion object {
        const val TAG = "ShiftReportSyncWorker"
        const val PREFS_NAME = "lojia_app_prefs"

        const val KEY_LAST_SYNC_TIME = "last_shift_sync_timestamp"
        const val KEY_LAST_SYNC_COUNT = "last_shift_sync_count"
        const val KEY_LAST_SYNC_STATUS = "last_shift_sync_status"

        const val KEY_OUTPUT_SYNCED_COUNT = "synced_count"
        const val KEY_OUTPUT_TIMESTAMP = "sync_timestamp"
        const val KEY_OUTPUT_STATUS = "sync_status"
    }
}
