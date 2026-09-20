package com.lojia.pos.sync

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.lojia.pos.data.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * UI State describing the current Firebase Cloud Sync status.
 */
data class FirebaseSyncStatusState(
    val isEnabled: Boolean = false,
    val isSyncing: Boolean = false,
    val lastSyncTimestamp: Long = 0L,
    val statusMessage: String = "Sync is turned off (Offline-only mode)",
    val lastSyncSummary: String = "",
    val isCloudReachable: Boolean = true,
    val isOnline: Boolean = true,
    val uploadedProducts: Int = 0,
    val downloadedProducts: Int = 0,
    val uploadedCategories: Int = 0,
    val downloadedCategories: Int = 0,
    val uploadedReports: Int = 0,
    val downloadedReports: Int = 0,
    val uploadedSales: Int = 0,
    val downloadedSales: Int = 0,
    val lastErrorMessage: String? = null
)

/**
 * Summary outcome of a single synchronization pass.
 */
data class FirebaseSyncResult(
    val success: Boolean,
    val message: String,
    val productsSynced: Int = 0,
    val categoriesSynced: Int = 0,
    val reportsSynced: Int = 0,
    val salesSynced: Int = 0,
    val isCloudReachable: Boolean = true,
    val isOfflineMode: Boolean = false
)

/**
 * Centralized Firebase Cloud Sync Manager.
 *
 * Architecture Principles:
 * 1. Offline-first: Room database is the authoritative primary source of truth on-device.
 * 2. Non-blocking: All POS checkout, inventory, and shift actions operate solely through Room.
 * 3. Bidirectional Sync: Uploads and downloads Products, Categories, Shift Reports, Sales, Business Profile.
 * 4. Conflict Handling: Last-updated timestamp comparison (Last-Write-Wins with Room precedence on tie).
 * 5. Privacy: Strictly filters out all authentication secrets, user passwords, and cashier PINs.
 * 6. Cloud Resilience: If Firebase is disabled, down, or returning 403/404, gracefully switches to local
 *    buffered sync without ever blocking the UI or throwing unhandled errors.
 */
object FirebaseCloudSyncManager {
    private const val TAG = "FirebaseCloudSync"
    private const val PREFS_NAME = "lojia_firebase_sync_prefs"
    private const val KEY_SYNC_ENABLED = "firebase_sync_enabled"
    private const val KEY_LAST_SYNC_TIME = "firebase_last_sync_time"
    private const val KEY_LAST_SYNC_SUMMARY = "firebase_last_sync_summary"
    private const val KEY_CUSTOM_FIREBASE_URL = "firebase_custom_url"
    private const val KEY_PROJECT_ID = "firebase_project_id"

    // Default configuration from firebase-applet-config.json
    const val DEFAULT_PROJECT_ID = "gen-lang-client-0290392339"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _syncState = MutableStateFlow(FirebaseSyncStatusState())
    val syncState: StateFlow<FirebaseSyncStatusState> = _syncState.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean(KEY_SYNC_ENABLED, false)
        val lastTime = prefs.getLong(KEY_LAST_SYNC_TIME, 0L)
        val summary = prefs.getString(KEY_LAST_SYNC_SUMMARY, "") ?: ""

        _syncState.value = FirebaseSyncStatusState(
            isEnabled = enabled,
            isSyncing = false,
            lastSyncTimestamp = lastTime,
            statusMessage = if (enabled) {
                if (lastTime > 0) "Synchronized • Room primary" else "Sync enabled • Waiting for sync"
            } else {
                "Sync is turned off (Offline-only mode)"
            },
            lastSyncSummary = summary,
            isOnline = isNetworkAvailable(context)
        )
    }

    fun isSyncEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_SYNC_ENABLED, false)
    }

    fun setSyncEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_SYNC_ENABLED, enabled).apply()

        val lastTime = prefs.getLong(KEY_LAST_SYNC_TIME, 0L)
        val summary = prefs.getString(KEY_LAST_SYNC_SUMMARY, "") ?: ""

        _syncState.value = _syncState.value.copy(
            isEnabled = enabled,
            statusMessage = if (enabled) {
                if (lastTime > 0) "Synchronized • Room primary" else "Sync enabled • Ready to sync"
            } else {
                "Sync is turned off (Offline-only mode)"
            },
            lastSyncSummary = summary
        )

        if (enabled) {
            FirebaseCloudSyncScheduler.schedulePeriodicSync(context)
            // Trigger an initial sync pass
            scope.launch {
                performSync(context, isManual = true)
            }
        } else {
            FirebaseCloudSyncScheduler.cancelPeriodicSync(context)
        }
    }

    fun getLastSyncTimestamp(context: Context): Long {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_LAST_SYNC_TIME, 0L)
    }

    fun getFirebaseProjectId(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PROJECT_ID, DEFAULT_PROJECT_ID) ?: DEFAULT_PROJECT_ID
    }

    fun getCustomFirebaseUrl(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CUSTOM_FIREBASE_URL, "") ?: ""
    }

    fun setCustomFirebaseUrl(context: Context, url: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CUSTOM_FIREBASE_URL, url.trim())
            .apply()
    }

    fun formatSyncTime(timestamp: Long): String {
        if (timestamp <= 0L) return "Never synced"
        val sdf = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun isNetworkAvailable(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false
            val network = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Records an entity timestamp update locally.
     * Keeps track of when each product, category, or business profile was modified.
     */
    fun recordEntityUpdated(context: Context, entityType: String, id: Int, timestamp: Long = System.currentTimeMillis()) {
        val prefs = context.getSharedPreferences("lojia_entity_timestamps", Context.MODE_PRIVATE)
        prefs.edit().putLong("${entityType}_${id}", timestamp).apply()
    }

    fun getEntityTimestamp(context: Context, entityType: String, id: Int, defaultTime: Long = 0L): Long {
        val prefs = context.getSharedPreferences("lojia_entity_timestamps", Context.MODE_PRIVATE)
        return prefs.getLong("${entityType}_${id}", defaultTime)
    }

    /**
     * Primary Synchronization Method.
     * Never blocks callers or POS transactions. Executes fully on Dispatchers.IO.
     */
    suspend fun performSync(context: Context, isManual: Boolean = false): FirebaseSyncResult = withContext(Dispatchers.IO) {
        if (!isSyncEnabled(context)) {
            return@withContext FirebaseSyncResult(
                success = false,
                message = "Cloud sync is currently disabled in Settings",
                isOfflineMode = true
            )
        }

        if (_syncState.value.isSyncing) {
            Log.d(TAG, "Sync already in progress, skipping concurrent run.")
            return@withContext FirebaseSyncResult(
                success = true,
                message = "Sync already in progress"
            )
        }

        val online = isNetworkAvailable(context)
        if (!online) {
            _syncState.value = _syncState.value.copy(
                isSyncing = false,
                isOnline = false,
                statusMessage = "Offline — Local Room database is primary and up to date"
            )
            return@withContext FirebaseSyncResult(
                success = true,
                message = "Offline: Changes queued in local Room database",
                isOfflineMode = true
            )
        }

        _syncState.value = _syncState.value.copy(
            isSyncing = true,
            isOnline = true,
            statusMessage = "Synchronizing with Firebase cloud..."
        )

        val db = AppDatabase.getInstance(context)
        val posDao = db.posDao()
        val reportDao = db.reportDao()

        var uploadedProducts = 0
        var downloadedProducts = 0
        var uploadedCategories = 0
        var downloadedCategories = 0
        var uploadedReports = 0
        var downloadedReports = 0
        var uploadedSales = 0
        var downloadedSales = 0
        var cloudReachable = true
        var failureReason: String? = null

        try {
            // =================================================================
            // 1. GATHER LOCAL ROOM DATA (PRIMARY SOURCE OF TRUTH)
            // =================================================================
            val localProducts = posDao.getAllProductsList()
            val localCategories = posDao.getAllCategoriesList()
            val localReports = reportDao.getAllShiftReportsList()
            val localSales = posDao.getAllSalesList()
            val localBusinessProfile = reportDao.getBusinessProfileOnce() ?: BusinessProfile()

            // Build Local Snapshot Map for conflict resolution
            val localProductMap = localProducts.associateBy { it.id }.toMutableMap()
            val localCategoryMap = localCategories.associateBy { it.id }.toMutableMap()
            val localReportMap = localReports.associateBy { it.id }.toMutableMap()
            val localSalesMap = localSales.associateBy { it.id }.toMutableMap()

            // =================================================================
            // 2. FETCH REMOTE FIREBASE CLOUD DATA
            // =================================================================
            val remoteDataResult = fetchRemoteData(context)
            val remoteJson = remoteDataResult.first
            cloudReachable = remoteDataResult.second

            val remoteProducts = remoteJson?.optJSONArray("products") ?: JSONArray()
            val remoteCategories = remoteJson?.optJSONArray("categories") ?: JSONArray()
            val remoteReports = remoteJson?.optJSONArray("shiftReports") ?: JSONArray()
            val remoteSales = remoteJson?.optJSONArray("sales") ?: JSONArray()
            val remoteProfileObj = remoteJson?.optJSONObject("businessProfile")

            // =================================================================
            // 3. CONFLICT HANDLING VIA LAST-UPDATED TIMESTAMP: PRODUCTS
            // =================================================================
            val productsToUpdateInRoom = mutableListOf<POSProduct>()
            val remoteProductIdsSeen = mutableSetOf<Int>()

            for (i in 0 until remoteProducts.length()) {
                val pObj = remoteProducts.getJSONObject(i)
                val rId = pObj.getInt("id")
                val rUpdated = pObj.optLong("updatedAt", 0L)
                remoteProductIdsSeen.add(rId)

                val local = localProductMap[rId]
                val localTs = getEntityTimestamp(context, "product", rId, 0L)

                if (local == null) {
                    // New product from cloud -> Download to Room
                    val newProduct = POSProduct(
                        id = rId,
                        name = pObj.getString("name"),
                        categoryId = pObj.optInt("categoryId", 1),
                        price = pObj.optDouble("price", 0.0),
                        costPrice = pObj.optDouble("costPrice", 0.0),
                        stockQuantity = pObj.optDouble("stockQuantity", 100.0),
                        minStockAlert = pObj.optDouble("minStockAlert", 10.0),
                        barcode = pObj.optString("barcode", ""),
                        sku = pObj.optString("sku", ""),
                        unit = pObj.optString("unit", "pcs"),
                        colorHex = pObj.optString("colorHex", "#10B981"),
                        active = pObj.optBoolean("active", true)
                    )
                    productsToUpdateInRoom.add(newProduct)
                    recordEntityUpdated(context, "product", rId, rUpdated)
                    downloadedProducts++
                } else if (rUpdated > localTs) {
                    // Remote is newer -> Update Room
                    val updatedProduct = local.copy(
                        name = pObj.optString("name", local.name),
                        categoryId = pObj.optInt("categoryId", local.categoryId),
                        price = pObj.optDouble("price", local.price),
                        costPrice = pObj.optDouble("costPrice", local.costPrice),
                        stockQuantity = pObj.optDouble("stockQuantity", local.stockQuantity),
                        minStockAlert = pObj.optDouble("minStockAlert", local.minStockAlert),
                        barcode = pObj.optString("barcode", local.barcode),
                        sku = pObj.optString("sku", local.sku),
                        unit = pObj.optString("unit", local.unit),
                        colorHex = pObj.optString("colorHex", local.colorHex),
                        active = pObj.optBoolean("active", local.active)
                    )
                    productsToUpdateInRoom.add(updatedProduct)
                    recordEntityUpdated(context, "product", rId, rUpdated)
                    downloadedProducts++
                } else {
                    // Local is newer or equal -> Will upload to remote
                    uploadedProducts++
                }
            }

            // Products that exist locally but not remotely -> upload
            for (localP in localProducts) {
                if (!remoteProductIdsSeen.contains(localP.id)) {
                    uploadedProducts++
                }
            }

            if (productsToUpdateInRoom.isNotEmpty()) {
                posDao.insertProducts(productsToUpdateInRoom)
            }

            // =================================================================
            // 4. CONFLICT HANDLING VIA LAST-UPDATED TIMESTAMP: CATEGORIES
            // =================================================================
            val categoriesToUpdateInRoom = mutableListOf<POSCategory>()
            val remoteCategoryIdsSeen = mutableSetOf<Int>()

            for (i in 0 until remoteCategories.length()) {
                val cObj = remoteCategories.getJSONObject(i)
                val cId = cObj.getInt("id")
                val rUpdated = cObj.optLong("updatedAt", 0L)
                remoteCategoryIdsSeen.add(cId)

                val local = localCategoryMap[cId]
                val localTs = getEntityTimestamp(context, "category", cId, 0L)

                if (local == null) {
                    val newCat = POSCategory(
                        id = cId,
                        name = cObj.getString("name"),
                        iconName = cObj.optString("iconName", "Category"),
                        colorHex = cObj.optString("colorHex", "#6366F1")
                    )
                    categoriesToUpdateInRoom.add(newCat)
                    recordEntityUpdated(context, "category", cId, rUpdated)
                    downloadedCategories++
                } else if (rUpdated > localTs) {
                    val updatedCat = local.copy(
                        name = cObj.optString("name", local.name),
                        iconName = cObj.optString("iconName", local.iconName),
                        colorHex = cObj.optString("colorHex", local.colorHex)
                    )
                    categoriesToUpdateInRoom.add(updatedCat)
                    recordEntityUpdated(context, "category", cId, rUpdated)
                    downloadedCategories++
                } else {
                    uploadedCategories++
                }
            }

            for (localCat in localCategories) {
                if (!remoteCategoryIdsSeen.contains(localCat.id)) {
                    uploadedCategories++
                }
            }

            if (categoriesToUpdateInRoom.isNotEmpty()) {
                posDao.insertCategories(categoriesToUpdateInRoom)
            }

            // =================================================================
            // 5. CONFLICT HANDLING: SHIFT REPORTS (dateInMillis / timestamp)
            // =================================================================
            val reportsToUpdateInRoom = mutableListOf<ShiftReport>()
            val remoteReportIdsSeen = mutableSetOf<Int>()

            for (i in 0 until remoteReports.length()) {
                val repObj = remoteReports.getJSONObject(i)
                val repId = repObj.getInt("id")
                remoteReportIdsSeen.add(repId)

                val local = localReportMap[repId]
                val rDate = repObj.optLong("dateInMillis", 0L)

                if (local == null) {
                    val newReport = ShiftReport(
                        id = repId,
                        cashierName = repObj.optString("cashierName", "Staff"),
                        shift = repObj.optString("shift", "Day"),
                        dateInMillis = rDate,
                        grossCash = repObj.optDouble("grossCash", 0.0),
                        madaPayments = repObj.optDouble("madaPayments", 0.0),
                        digitalWallet = repObj.optDouble("digitalWallet", 0.0),
                        staffMealsCount = repObj.optInt("staffMealsCount", 0),
                        totalExpenses = repObj.optDouble("totalExpenses", 0.0),
                        muasselQty = repObj.optDouble("muasselQty", 0.0),
                        outdoorShishaQty = repObj.optDouble("outdoorShishaQty", 0.0),
                        dueCreditEntriesJson = repObj.optString("dueCreditEntriesJson", "[]"),
                        previousDueCollectionsJson = repObj.optString("previousDueCollectionsJson", "[]"),
                        staffAdvancesJson = repObj.optString("staffAdvancesJson", "[]"),
                        unpaidBillsJson = repObj.optString("unpaidBillsJson", "[]"),
                        purchasedItemsJson = repObj.optString("purchasedItemsJson", "[]"),
                        notes = repObj.optString("notes", "")
                    )
                    reportsToUpdateInRoom.add(newReport)
                    downloadedReports++
                } else if (rDate > local.dateInMillis) {
                    val updatedReport = local.copy(
                        grossCash = repObj.optDouble("grossCash", local.grossCash),
                        madaPayments = repObj.optDouble("madaPayments", local.madaPayments),
                        digitalWallet = repObj.optDouble("digitalWallet", local.digitalWallet),
                        totalExpenses = repObj.optDouble("totalExpenses", local.totalExpenses),
                        notes = repObj.optString("notes", local.notes)
                    )
                    reportsToUpdateInRoom.add(updatedReport)
                    downloadedReports++
                } else {
                    uploadedReports++
                }
            }

            for (lr in localReports) {
                if (!remoteReportIdsSeen.contains(lr.id)) {
                    uploadedReports++
                }
            }

            if (reportsToUpdateInRoom.isNotEmpty()) {
                reportDao.insertShiftReports(reportsToUpdateInRoom)
            }

            // =================================================================
            // 6. CONFLICT HANDLING: SALES SUMMARIES (timestamp)
            // =================================================================
            val salesToUpdateInRoom = mutableListOf<POSSale>()
            val remoteSaleIdsSeen = mutableSetOf<Int>()

            for (i in 0 until remoteSales.length()) {
                val sObj = remoteSales.getJSONObject(i)
                val sId = sObj.getInt("id")
                remoteSaleIdsSeen.add(sId)

                val local = localSalesMap[sId]
                if (local == null) {
                    val newSale = POSSale(
                        id = sId,
                        invoiceNumber = sObj.optString("invoiceNumber", "INV-$sId"),
                        cashierName = sObj.optString("cashierName", "Staff"),
                        customerName = sObj.optString("customerName", "Customer"),
                        subtotal = sObj.optDouble("subtotal", 0.0),
                        vatAmount = sObj.optDouble("vatAmount", 0.0),
                        totalAmount = sObj.optDouble("totalAmount", 0.0),
                        paymentMethod = sObj.optString("paymentMethod", "CASH"),
                        timestamp = sObj.optLong("timestamp", System.currentTimeMillis()),
                        isVoided = sObj.optBoolean("isVoided", false),
                        voidReason = sObj.optString("voidReason", "")
                    )
                    salesToUpdateInRoom.add(newSale)
                    downloadedSales++
                } else {
                    uploadedSales++
                }
            }

            for (ls in localSales) {
                if (!remoteSaleIdsSeen.contains(ls.id)) {
                    uploadedSales++
                }
            }

            if (salesToUpdateInRoom.isNotEmpty()) {
                posDao.insertSales(salesToUpdateInRoom)
            }

            // =================================================================
            // 7. CONFLICT HANDLING: BUSINESS PROFILE
            // =================================================================
            if (remoteProfileObj != null) {
                val rProfileTs = remoteProfileObj.optLong("updatedAt", 0L)
                val lProfileTs = getEntityTimestamp(context, "business_profile", 1, 0L)

                if (rProfileTs > lProfileTs) {
                    val updatedProfile = localBusinessProfile.copy(
                        businessName = remoteProfileObj.optString("businessName", localBusinessProfile.businessName),
                        vatNumber = remoteProfileObj.optString("vatNumber", localBusinessProfile.vatNumber),
                        phone = remoteProfileObj.optString("phone", localBusinessProfile.phone),
                        email = remoteProfileObj.optString("email", localBusinessProfile.email),
                        address = remoteProfileObj.optString("address", localBusinessProfile.address),
                        workingHours = remoteProfileObj.optString("workingHours", localBusinessProfile.workingHours),
                        currency = remoteProfileObj.optString("currency", localBusinessProfile.currency),
                        country = remoteProfileObj.optString("country", localBusinessProfile.country),
                        vatRate = remoteProfileObj.optDouble("vatRate", localBusinessProfile.vatRate),
                        isTaxEnabled = remoteProfileObj.optBoolean("isTaxEnabled", localBusinessProfile.isTaxEnabled),
                        isTaxIncluded = remoteProfileObj.optBoolean("isTaxIncluded", localBusinessProfile.isTaxIncluded)
                    )
                    reportDao.saveBusinessProfile(updatedProfile)
                    recordEntityUpdated(context, "business_profile", 1, rProfileTs)
                }
            }

            // =================================================================
            // 8. COMPOSE UPLOAD PAYLOAD & DISPATCH TO FIREBASE
            // (Strictly excludes passwords and PINs)
            // =================================================================
            val freshProducts = posDao.getAllProductsList()
            val freshCategories = posDao.getAllCategoriesList()
            val freshReports = reportDao.getAllShiftReportsList()
            val freshSales = posDao.getAllSalesList()
            val freshProfile = reportDao.getBusinessProfileOnce() ?: BusinessProfile()

            val uploadPayload = JSONObject()
            uploadPayload.put("syncVersion", 2)
            uploadPayload.put("lastSyncTimestamp", System.currentTimeMillis())
            uploadPayload.put("projectId", getFirebaseProjectId(context))

            // Products Array (with timestamps)
            val pArr = JSONArray()
            for (p in freshProducts) {
                val pTs = getEntityTimestamp(context, "product", p.id, System.currentTimeMillis())
                pArr.put(JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("categoryId", p.categoryId)
                    put("price", p.price)
                    put("costPrice", p.costPrice)
                    put("stockQuantity", p.stockQuantity)
                    put("minStockAlert", p.minStockAlert)
                    put("barcode", p.barcode)
                    put("sku", p.sku)
                    put("unit", p.unit)
                    put("colorHex", p.colorHex)
                    put("active", p.active)
                    put("updatedAt", pTs)
                })
            }
            uploadPayload.put("products", pArr)

            // Categories Array (with timestamps)
            val cArr = JSONArray()
            for (c in freshCategories) {
                val cTs = getEntityTimestamp(context, "category", c.id, System.currentTimeMillis())
                cArr.put(JSONObject().apply {
                    put("id", c.id)
                    put("name", c.name)
                    put("iconName", c.iconName)
                    put("colorHex", c.colorHex)
                    put("updatedAt", cTs)
                })
            }
            uploadPayload.put("categories", cArr)

            // Shift Reports Array
            val repArr = JSONArray()
            for (r in freshReports) {
                repArr.put(JSONObject().apply {
                    put("id", r.id)
                    put("cashierName", r.cashierName)
                    put("shift", r.shift)
                    put("dateInMillis", r.dateInMillis)
                    put("grossCash", r.grossCash)
                    put("madaPayments", r.madaPayments)
                    put("digitalWallet", r.digitalWallet)
                    put("totalExpenses", r.totalExpenses)
                    put("notes", r.notes)
                    put("dueCreditEntriesJson", r.dueCreditEntriesJson)
                    put("previousDueCollectionsJson", r.previousDueCollectionsJson)
                    put("staffAdvancesJson", r.staffAdvancesJson)
                    put("unpaidBillsJson", r.unpaidBillsJson)
                    put("purchasedItemsJson", r.purchasedItemsJson)
                })
            }
            uploadPayload.put("shiftReports", repArr)

            // Sales Summaries Array
            val salesArr = JSONArray()
            for (s in freshSales) {
                salesArr.put(JSONObject().apply {
                    put("id", s.id)
                    put("invoiceNumber", s.invoiceNumber)
                    put("cashierName", s.cashierName)
                    put("customerName", s.customerName)
                    put("subtotal", s.subtotal)
                    put("vatAmount", s.vatAmount)
                    put("totalAmount", s.totalAmount)
                    put("paymentMethod", s.paymentMethod)
                    put("timestamp", s.timestamp)
                    put("isVoided", s.isVoided)
                    put("voidReason", s.voidReason)
                })
            }
            uploadPayload.put("sales", salesArr)

            // Business Profile Object (Zero passwords/PINs)
            val bpTs = getEntityTimestamp(context, "business_profile", 1, System.currentTimeMillis())
            val bpObj = JSONObject().apply {
                put("businessName", freshProfile.businessName)
                put("vatNumber", freshProfile.vatNumber)
                put("phone", freshProfile.phone)
                put("email", freshProfile.email)
                put("address", freshProfile.address)
                put("workingHours", freshProfile.workingHours)
                put("currency", freshProfile.currency)
                put("country", freshProfile.country)
                put("vatRate", freshProfile.vatRate)
                put("isTaxEnabled", freshProfile.isTaxEnabled)
                put("isTaxIncluded", freshProfile.isTaxIncluded)
                put("updatedAt", bpTs)
            }
            uploadPayload.put("businessProfile", bpObj)

            // Dispatch to cloud (or local cloud buffer fallback)
            val uploadSuccess = dispatchUploadToFirebase(context, uploadPayload)
            if (!uploadSuccess) {
                cloudReachable = false
            }

            // =================================================================
            // 9. PERSIST SYNC METADATA
            // =================================================================
            val now = System.currentTimeMillis()
            val totalProducts = freshProducts.size
            val totalCategories = freshCategories.size
            val totalReports = freshReports.size
            val totalSales = freshSales.size

            val summaryStr = "Synced: $totalProducts products, $totalCategories categories, $totalReports reports, $totalSales sales"

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putLong(KEY_LAST_SYNC_TIME, now)
                .putString(KEY_LAST_SYNC_SUMMARY, summaryStr)
                .apply()

            // Update app_settings in Room
            reportDao.setSetting(AppSetting(KEY_LAST_SYNC_TIME, now.toString()))
            reportDao.setSetting(AppSetting(KEY_LAST_SYNC_SUMMARY, summaryStr))

            val statusMsg = if (cloudReachable) {
                "Synchronized • All business records up to date"
            } else {
                "Cloud server unreachable — Local Room data safe and unaffected"
            }

            _syncState.value = _syncState.value.copy(
                isSyncing = false,
                lastSyncTimestamp = now,
                statusMessage = statusMsg,
                lastSyncSummary = summaryStr,
                isCloudReachable = cloudReachable,
                isOnline = true,
                uploadedProducts = uploadedProducts,
                downloadedProducts = downloadedProducts,
                uploadedCategories = uploadedCategories,
                downloadedCategories = downloadedCategories,
                uploadedReports = uploadedReports,
                downloadedReports = downloadedReports,
                uploadedSales = uploadedSales,
                downloadedSales = downloadedSales,
                lastErrorMessage = null
            )

            Log.i(TAG, "Firebase Cloud Sync completed successfully: $summaryStr (cloudReachable=$cloudReachable)")

            return@withContext FirebaseSyncResult(
                success = true,
                message = summaryStr,
                productsSynced = totalProducts,
                categoriesSynced = totalCategories,
                reportsSynced = totalReports,
                salesSynced = totalSales,
                isCloudReachable = cloudReachable
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error during Firebase Cloud Sync: ${e.message}", e)
            failureReason = e.localizedMessage ?: "Unknown sync error"

            val currentTimestamp = _syncState.value.lastSyncTimestamp
            _syncState.value = _syncState.value.copy(
                isSyncing = false,
                statusMessage = "Cloud server unreachable — Local Room data safe and unaffected",
                lastErrorMessage = failureReason,
                isCloudReachable = false
            )

            return@withContext FirebaseSyncResult(
                success = false,
                message = "Cloud currently unreachable: $failureReason. Local Room data is completely unaffected.",
                isCloudReachable = false
            )
        }
    }

    /**
     * Downloads remote data from Firebase Realtime Database or Firestore REST,
     * or loads from persistent local cloud buffer if remote is offline/disabled.
     */
    private fun fetchRemoteData(context: Context): Pair<JSONObject?, Boolean> {
        val customUrl = getCustomFirebaseUrl(context)
        val projectId = getFirebaseProjectId(context)

        val targetUrl = if (customUrl.isNotEmpty()) {
            if (!customUrl.endsWith(".json")) "$customUrl/pos_cloud_data.json" else customUrl
        } else {
            "https://$projectId-default-rtdb.firebaseio.com/pos_cloud_data.json"
        }

        try {
            val url = URL(targetUrl)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 4000
                setRequestProperty("Accept", "application/json")
            }

            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val sb = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    sb.append(line)
                }
                reader.close()
                val responseStr = sb.toString().trim()
                if (responseStr.isNotEmpty() && responseStr != "null") {
                    val jsonObj = JSONObject(responseStr)
                    // Update local buffer file
                    saveLocalCloudBuffer(context, jsonObj)
                    return Pair(jsonObj, true)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Network attempt to Firebase at $targetUrl failed or service unavailable (${e.message}). Reading from local buffer.")
        }

        // Fallback to local persistent cloud buffer so sync never fails or crashes
        val bufferObj = loadLocalCloudBuffer(context)
        return Pair(bufferObj, false)
    }

    /**
     * Dispatches the synchronized payload to Firebase cloud REST endpoint.
     * Also updates the local persistent cloud buffer.
     */
    private fun dispatchUploadToFirebase(context: Context, payload: JSONObject): Boolean {
        // 1. Always save to local cloud mirror buffer first (guarantee zero data loss)
        saveLocalCloudBuffer(context, payload)

        val customUrl = getCustomFirebaseUrl(context)
        val projectId = getFirebaseProjectId(context)

        val targetUrl = if (customUrl.isNotEmpty()) {
            if (!customUrl.endsWith(".json")) "$customUrl/pos_cloud_data.json" else customUrl
        } else {
            "https://$projectId-default-rtdb.firebaseio.com/pos_cloud_data.json"
        }

        return try {
            val url = URL(targetUrl)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "PUT"
                connectTimeout = 4000
                readTimeout = 4000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
            }

            val writer = OutputStreamWriter(conn.outputStream, "UTF-8")
            writer.write(payload.toString())
            writer.flush()
            writer.close()

            val responseCode = conn.responseCode
            responseCode in 200..299
        } catch (e: Exception) {
            Log.w(TAG, "Upload to Firebase failed (${e.message}). Local buffer updated successfully. Room remains primary truth.")
            false
        }
    }

    private fun getBufferFile(context: Context): File {
        val dir = File(context.filesDir, "firebase_sync")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "cloud_data_buffer.json")
    }

    private fun saveLocalCloudBuffer(context: Context, data: JSONObject) {
        try {
            val file = getBufferFile(context)
            file.writeText(data.toString(2), Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write local cloud buffer: ${e.message}")
        }
    }

    private fun loadLocalCloudBuffer(context: Context): JSONObject? {
        return try {
            val file = getBufferFile(context)
            if (file.exists() && file.length() > 0) {
                JSONObject(file.readText(Charsets.UTF_8))
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read local cloud buffer: ${e.message}")
            null
        }
    }
}
