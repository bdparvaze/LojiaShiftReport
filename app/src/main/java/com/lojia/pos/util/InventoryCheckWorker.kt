package com.lojia.pos.util

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.lojia.pos.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Background CoroutineWorker to periodically check inventory levels in the Room database
 * and post system notifications when item stock falls below minStockAlert.
 */
class InventoryCheckWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting background inventory check...")
            val database = AppDatabase.getInstance(context)
            val posDao = database.posDao()

            // Fetch products where stock <= predefined minStockAlert threshold
            val lowStockProducts = posDao.getLowStockProductsOnce()
            Log.d(TAG, "Found ${lowStockProducts.size} items below threshold.")

            for (product in lowStockProducts) {
                NotificationHelper.sendLowStockNotification(
                    context = context,
                    productName = product.name,
                    currentStock = product.stockQuantity,
                    minStockAlert = product.minStockAlert,
                    unit = product.unit.ifBlank { "pcs" }
                )
            }

            // Save last execution status to preferences
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putLong(KEY_LAST_CHECK_TIME, System.currentTimeMillis())
                .putInt(KEY_LOW_STOCK_COUNT, lowStockProducts.size)
                .putString(KEY_LAST_CHECK_STATUS, "SUCCESS (${lowStockProducts.size} items low)")
                .apply()

            val outputData = workDataOf(
                KEY_LOW_STOCK_COUNT to lowStockProducts.size,
                KEY_CHECK_TIMESTAMP to System.currentTimeMillis()
            )

            Result.success(outputData)
        } catch (e: Exception) {
            Log.e(TAG, "Inventory check background worker failed: ${e.message}", e)
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString(KEY_LAST_CHECK_STATUS, "FAILED: ${e.localizedMessage}")
                .apply()
            Result.retry()
        }
    }

    companion object {
        const val TAG = "InventoryCheckWorker"
        const val WORK_NAME = "lojia_inventory_check_work"
        const val PREF_NAME = "lojia_inventory_worker_prefs"
        const val KEY_LAST_CHECK_TIME = "key_last_check_time"
        const val KEY_LOW_STOCK_COUNT = "key_low_stock_count"
        const val KEY_LAST_CHECK_STATUS = "key_last_check_status"
        const val KEY_CHECK_TIMESTAMP = "check_timestamp"
    }
}
