package com.lojia.shiftreport.sync

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.lojia.shiftreport.data.*
import com.lojia.shiftreport.util.MoneyFormat
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
    val uploadedReports: Int = 0,
    val downloadedReports: Int = 0,
    val lastErrorMessage: String? = null
)

/**
 * Summary outcome of a single synchronization pass.
 */
data class FirebaseSyncResult(
    val success: Boolean,
    val message: String,
    val reportsSynced: Int = 0,
    val isCloudReachable: Boolean = true,
    val isOfflineMode: Boolean = false
)

/**
 * Centralized Firebase Cloud Sync Manager for Shift Reports.
 */
object FirebaseCloudSyncManager {
    private const val TAG = "FirebaseCloudSync"
    private const val PREFS_NAME = "lojia_firebase_sync_prefs"
    private const val KEY_SYNC_ENABLED = "firebase_sync_enabled"
    private const val KEY_LAST_SYNC_TIME = "firebase_last_sync_time"
    private const val KEY_LAST_SYNC_SUMMARY = "firebase_last_sync_summary"
    private const val KEY_CUSTOM_FIREBASE_URL = "firebase_custom_url"
    private const val KEY_PROJECT_ID = "firebase_project_id"

    const val DEFAULT_PROJECT_ID = "gen-lang-client-0290392339"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _syncState = MutableStateFlow(FirebaseSyncStatusState())
    val syncState: StateFlow<FirebaseSyncStatusState> = _syncState.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // Clean up any legacy database secret stored previously
        if (prefs.contains("firebase_auth_secret")) {
            prefs.edit().remove("firebase_auth_secret").apply()
        }

        if (!com.lojia.shiftreport.BuildConfig.CLOUD_SYNC_ENABLED) {
            _syncState.value = FirebaseSyncStatusState(
                isEnabled = false,
                lastSyncTimestamp = 0L,
                lastSyncSummary = "",
                statusMessage = "Cloud sync is not available in this version"
            )
            return
        }

        val enabled = prefs.getBoolean(KEY_SYNC_ENABLED, false)
        val lastTime = prefs.getLong(KEY_LAST_SYNC_TIME, 0L)
        val summary = prefs.getString(KEY_LAST_SYNC_SUMMARY, "") ?: ""

        _syncState.value = FirebaseSyncStatusState(
            isEnabled = enabled,
            lastSyncTimestamp = lastTime,
            lastSyncSummary = summary,
            statusMessage = if (enabled) {
                if (lastTime > 0) "Synchronized • Last synced ${formatTimestamp(lastTime)}"
                else "Sync is active • Waiting for initial sync"
            } else {
                "Sync is turned off (Offline-only mode)"
            }
        )
    }

    fun isSyncEnabled(context: Context): Boolean {
        if (!com.lojia.shiftreport.BuildConfig.CLOUD_SYNC_ENABLED) return false
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_SYNC_ENABLED, false)
    }

    fun setSyncEnabled(context: Context, enabled: Boolean) {
        if (!com.lojia.shiftreport.BuildConfig.CLOUD_SYNC_ENABLED) {
            _syncState.value = _syncState.value.copy(
                isEnabled = false,
                statusMessage = "Cloud sync is not available in this version"
            )
            return
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_SYNC_ENABLED, enabled).apply()

        _syncState.value = _syncState.value.copy(
            isEnabled = enabled,
            statusMessage = if (enabled) "Cloud sync activated" else "Sync is turned off (Offline-only mode)"
        )

        if (enabled) {
            triggerSyncNow(context)
        }
    }

    fun getFirebaseProjectId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_PROJECT_ID, DEFAULT_PROJECT_ID) ?: DEFAULT_PROJECT_ID
    }

    fun setFirebaseProjectId(context: Context, projectId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PROJECT_ID, projectId.trim()).apply()
    }

    fun getCustomFirebaseUrl(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CUSTOM_FIREBASE_URL, "") ?: ""
    }

    fun setCustomFirebaseUrl(context: Context, url: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CUSTOM_FIREBASE_URL, url.trim()).apply()
    }

    /**
     * Retrieves the Firebase Auth ID token.
     * Structured so a future version can plug in a Firebase Auth ID token.
     */
    fun getAuthToken(): String? {
        return null
    }

    /**
     * Resolves the authenticated user UID from the token provided by [getAuthToken].
     * Returns null when no user is authenticated or available.
     */
    fun getAuthUserUid(): String? {
        val token = getAuthToken()?.trim() ?: return null
        if (token.isEmpty()) return null
        return try {
            val parts = token.split(".")
            if (parts.size >= 2) {
                val payloadBytes = android.util.Base64.decode(
                    parts[1],
                    android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP
                )
                val json = JSONObject(String(payloadBytes, Charsets.UTF_8))
                val uid = json.optString("user_id").ifEmpty { json.optString("sub") }
                uid.ifEmpty { null }
            } else {
                token.ifEmpty { null }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse user uid from auth token: ${e.message}")
            null
        }
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val activeNet = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNet) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun triggerSyncNow(context: Context, onResult: ((FirebaseSyncResult) -> Unit)? = null) {
        if (!com.lojia.shiftreport.BuildConfig.CLOUD_SYNC_ENABLED) {
            val res = FirebaseSyncResult(
                success = false,
                message = "Cloud sync is not available in this version",
                isCloudReachable = false,
                isOfflineMode = true
            )
            onResult?.invoke(res)
            return
        }

        if (getAuthUserUid().isNullOrBlank()) {
            val res = FirebaseSyncResult(
                success = false,
                message = "Cloud sync unavailable: no authenticated user.",
                isCloudReachable = false,
                isOfflineMode = true
            )
            onResult?.invoke(res)
            return
        }

        scope.launch {
            val res = performBidirectionalSync(context)
            withContext(Dispatchers.Main) {
                onResult?.invoke(res)
            }
        }
    }

    suspend fun performSync(context: Context, isManual: Boolean = false): FirebaseSyncResult {
        if (!com.lojia.shiftreport.BuildConfig.CLOUD_SYNC_ENABLED) {
            return FirebaseSyncResult(
                success = false,
                message = "Cloud sync is not available in this version",
                isCloudReachable = false,
                isOfflineMode = true
            )
        }
        if (getAuthUserUid().isNullOrBlank()) {
            return FirebaseSyncResult(
                success = false,
                message = "Cloud sync unavailable: no authenticated user.",
                isCloudReachable = false,
                isOfflineMode = true
            )
        }
        return performBidirectionalSync(context)
    }

    suspend fun performBidirectionalSync(context: Context): FirebaseSyncResult = withContext(Dispatchers.IO) {
        if (!com.lojia.shiftreport.BuildConfig.CLOUD_SYNC_ENABLED) {
            return@withContext FirebaseSyncResult(
                success = false,
                message = "Cloud sync is not available in this version",
                isCloudReachable = false,
                isOfflineMode = true
            )
        }

        val uid = getAuthUserUid()
        if (uid.isNullOrBlank()) {
            Log.d(TAG, "No authenticated user available. Skipping cloud sync.")
            _syncState.value = _syncState.value.copy(
                isSyncing = false,
                statusMessage = "Cloud sync unavailable — sign-in required",
                lastErrorMessage = "No authenticated user"
            )
            return@withContext FirebaseSyncResult(
                success = false,
                message = "Cloud sync unavailable: no authenticated user.",
                isCloudReachable = false,
                isOfflineMode = true
            )
        }
        val isEnabled = isSyncEnabled(context)
        val isOnline = isNetworkAvailable(context)

        if (!isEnabled) {
            _syncState.value = _syncState.value.copy(
                isSyncing = false,
                isEnabled = false,
                isOnline = isOnline,
                statusMessage = "Offline Mode: Sync disabled"
            )
            return@withContext FirebaseSyncResult(
                success = true,
                message = "Offline Mode: Cloud sync is disabled.",
                isOfflineMode = true
            )
        }

        _syncState.value = _syncState.value.copy(
            isSyncing = true,
            statusMessage = "Synchronizing with Cloud..."
        )

        val db = AppDatabase.getInstance(context)
        val reportDao = db.reportDao()

        var uploadedReports = 0
        var downloadedReports = 0
        var cloudReachable = true
        var failureReason: String? = null

        try {
            val localReports = reportDao.getAllShiftReportsList()
            val localBusinessProfile = reportDao.getBusinessProfileOnce() ?: BusinessProfile()

            val localReportMap = localReports.associateBy { it.id }.toMutableMap()

            val remoteDataResult = fetchRemoteData(context)
            val remoteJson = remoteDataResult.first
            cloudReachable = remoteDataResult.second

            val remoteReports = remoteJson?.optJSONArray("shiftReports") ?: JSONArray()
            val remoteProfileObj = remoteJson?.optJSONObject("businessProfile")

            val schemaVersion = remoteJson?.optInt("schemaVersion", 1) ?: 1
            val isLegacy = schemaVersion < 15

            fun getMoneyVal(obj: JSONObject, key: String, defaultMinorVal: Long = 0L): Long {
                if (!obj.has(key) || obj.isNull(key)) return defaultMinorVal
                return if (isLegacy) {
                    val value = obj.optDouble(key, MoneyFormat.toMajorUnits(defaultMinorVal))
                    MoneyFormat.toMinorUnits(value)
                } else {
                    obj.optLong(key, defaultMinorVal)
                }
            }

            val reportsToUpdateInRoom = mutableListOf<ShiftReport>()
            val remoteReportIdsSeen = mutableSetOf<Int>()

            for (i in 0 until remoteReports.length()) {
                val repObj = remoteReports.getJSONObject(i)
                val repId = repObj.getInt("id")
                remoteReportIdsSeen.add(repId)

                val local = localReportMap[repId]
                val rDate = repObj.optLong("dateInMillis", 0L)

                val rawDueCredit = repObj.optString("dueCreditEntriesJson", local?.dueCreditEntriesJson ?: "[]")
                val rawPrevDue = repObj.optString("previousDueCollectionsJson", local?.previousDueCollectionsJson ?: "[]")
                val rawStaff = repObj.optString("staffAdvancesJson", local?.staffAdvancesJson ?: "[]")
                val rawUnpaid = repObj.optString("unpaidBillsJson", local?.unpaidBillsJson ?: "[]")
                val rawPurchased = repObj.optString("purchasedItemsJson", local?.purchasedItemsJson ?: "[]")

                val dueCreditJson = if (isLegacy && repObj.has("dueCreditEntriesJson")) AppDatabase.migrateJsonArraySingleAmount(rawDueCredit) else rawDueCredit
                val prevDueJson = if (isLegacy && repObj.has("previousDueCollectionsJson")) AppDatabase.migrateJsonArraySingleAmount(rawPrevDue) else rawPrevDue
                val staffJson = if (isLegacy && repObj.has("staffAdvancesJson")) AppDatabase.migrateJsonArraySingleAmount(rawStaff) else rawStaff
                val unpaidJson = if (isLegacy && repObj.has("unpaidBillsJson")) AppDatabase.migrateJsonArraySingleAmount(rawUnpaid) else rawUnpaid
                val purchasedJson = if (isLegacy && repObj.has("purchasedItemsJson")) AppDatabase.migrateJsonArrayPurchased(rawPurchased) else rawPurchased

                if (local == null) {
                    val newReport = ShiftReport(
                        id = repId,
                        cashierName = repObj.optString("cashierName", "Staff"),
                        shift = repObj.optString("shift", "Day"),
                        dateInMillis = rDate,
                        openingCash = getMoneyVal(repObj, "openingCash", 0L),
                        closingCash = getMoneyVal(repObj, "closingCash", 0L),
                        grossCash = getMoneyVal(repObj, "grossCash", 0L),
                        madaPayments = getMoneyVal(repObj, "madaPayments", 0L),
                        digitalWallet = getMoneyVal(repObj, "digitalWallet", 0L),
                        totalDiscounts = getMoneyVal(repObj, "totalDiscounts", 0L),
                        salesReturns = getMoneyVal(repObj, "salesReturns", 0L),
                        staffMealsCount = repObj.optInt("staffMealsCount", 0),
                        totalExpenses = getMoneyVal(repObj, "totalExpenses", 0L),
                        muasselQty = repObj.optDouble("muasselQty", 0.0),
                        outdoorShishaQty = repObj.optDouble("outdoorShishaQty", 0.0),
                        dueCreditEntriesJson = if (isLegacy) AppDatabase.migrateJsonArraySingleAmount(repObj.optString("dueCreditEntriesJson", "[]")) else repObj.optString("dueCreditEntriesJson", "[]"),
                        previousDueCollectionsJson = if (isLegacy) AppDatabase.migrateJsonArraySingleAmount(repObj.optString("previousDueCollectionsJson", "[]")) else repObj.optString("previousDueCollectionsJson", "[]"),
                        staffAdvancesJson = if (isLegacy) AppDatabase.migrateJsonArraySingleAmount(repObj.optString("staffAdvancesJson", "[]")) else repObj.optString("staffAdvancesJson", "[]"),
                        unpaidBillsJson = if (isLegacy) AppDatabase.migrateJsonArraySingleAmount(repObj.optString("unpaidBillsJson", "[]")) else repObj.optString("unpaidBillsJson", "[]"),
                        purchasedItemsJson = if (isLegacy) AppDatabase.migrateJsonArrayPurchased(repObj.optString("purchasedItemsJson", "[]")) else repObj.optString("purchasedItemsJson", "[]"),
                        notes = repObj.optString("notes", ""),
                        isLocked = repObj.optBoolean("isLocked", false),
                        managerSignedBy = if (repObj.has("managerSignedBy") && !repObj.isNull("managerSignedBy")) repObj.getString("managerSignedBy") else null,
                        managerSignTime = if (repObj.has("managerSignTime") && !repObj.isNull("managerSignTime")) repObj.getLong("managerSignTime") else null
                    )
                    reportsToUpdateInRoom.add(newReport)
                    downloadedReports++
                } else if (rDate > local.dateInMillis) {
                    val updatedReport = local.copy(
                        cashierName = repObj.optString("cashierName", local.cashierName),
                        shift = repObj.optString("shift", local.shift),
                        dateInMillis = rDate,
                        openingCash = getMoneyVal(repObj, "openingCash", local.openingCash),
                        closingCash = getMoneyVal(repObj, "closingCash", local.closingCash),
                        grossCash = getMoneyVal(repObj, "grossCash", local.grossCash),
                        madaPayments = getMoneyVal(repObj, "madaPayments", local.madaPayments),
                        digitalWallet = getMoneyVal(repObj, "digitalWallet", local.digitalWallet),
                        totalDiscounts = getMoneyVal(repObj, "totalDiscounts", local.totalDiscounts),
                        salesReturns = getMoneyVal(repObj, "salesReturns", local.salesReturns),
                        staffMealsCount = if (repObj.has("staffMealsCount")) repObj.optInt("staffMealsCount", local.staffMealsCount) else local.staffMealsCount,
                        totalExpenses = getMoneyVal(repObj, "totalExpenses", local.totalExpenses),
                        muasselQty = if (repObj.has("muasselQty")) repObj.optDouble("muasselQty", local.muasselQty) else local.muasselQty,
                        outdoorShishaQty = if (repObj.has("outdoorShishaQty")) repObj.optDouble("outdoorShishaQty", local.outdoorShishaQty) else local.outdoorShishaQty,
                        dueCreditEntriesJson = dueCreditJson,
                        previousDueCollectionsJson = prevDueJson,
                        staffAdvancesJson = staffJson,
                        unpaidBillsJson = unpaidJson,
                        purchasedItemsJson = purchasedJson,
                        notes = repObj.optString("notes", local.notes),
                        isLocked = if (repObj.has("isLocked")) repObj.optBoolean("isLocked", local.isLocked) else local.isLocked,
                        managerSignedBy = if (repObj.has("managerSignedBy")) (if (repObj.isNull("managerSignedBy")) null else repObj.getString("managerSignedBy")) else local.managerSignedBy,
                        managerSignTime = if (repObj.has("managerSignTime")) (if (repObj.isNull("managerSignTime")) null else repObj.getLong("managerSignTime")) else local.managerSignTime
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

            // Conflict Handling: Business Profile
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

            // Compose Upload Payload
            val freshReports = reportDao.getAllShiftReportsList()
            val freshProfile = reportDao.getBusinessProfileOnce() ?: BusinessProfile()

            val uploadPayload = JSONObject()
            uploadPayload.put("syncVersion", 2)
            uploadPayload.put("lastSyncTimestamp", System.currentTimeMillis())
            uploadPayload.put("projectId", getFirebaseProjectId(context))

            val repArr = JSONArray()
            for (r in freshReports) {
                repArr.put(JSONObject().apply {
                    put("id", r.id)
                    put("cashierName", r.cashierName)
                    put("shift", r.shift)
                    put("dateInMillis", r.dateInMillis)
                    put("openingCash", r.openingCash)
                    put("closingCash", r.closingCash)
                    put("grossCash", r.grossCash)
                    put("madaPayments", r.madaPayments)
                    put("digitalWallet", r.digitalWallet)
                    put("totalDiscounts", r.totalDiscounts)
                    put("salesReturns", r.salesReturns)
                    put("staffMealsCount", r.staffMealsCount)
                    put("totalExpenses", r.totalExpenses)
                    put("muasselQty", r.muasselQty)
                    put("outdoorShishaQty", r.outdoorShishaQty)
                    put("dueCreditEntriesJson", r.dueCreditEntriesJson)
                    put("previousDueCollectionsJson", r.previousDueCollectionsJson)
                    put("staffAdvancesJson", r.staffAdvancesJson)
                    put("unpaidBillsJson", r.unpaidBillsJson)
                    put("purchasedItemsJson", r.purchasedItemsJson)
                    put("notes", r.notes)
                    put("isLocked", r.isLocked)
                    if (r.managerSignedBy != null) put("managerSignedBy", r.managerSignedBy)
                    if (r.managerSignTime != null) put("managerSignTime", r.managerSignTime)
                })
            }
            uploadPayload.put("shiftReports", repArr)
            uploadPayload.put("schemaVersion", 15)

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

            val uploadSuccess = dispatchUploadToFirebase(context, uploadPayload)
            if (!uploadSuccess) {
                cloudReachable = false
            }

            val now = System.currentTimeMillis()
            val totalReports = freshReports.size
            val summaryStr = "Synced: $totalReports shift reports"

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putLong(KEY_LAST_SYNC_TIME, now)
                .putString(KEY_LAST_SYNC_SUMMARY, summaryStr)
                .apply()

            reportDao.setSetting(AppSetting(KEY_LAST_SYNC_TIME, now.toString()))
            reportDao.setSetting(AppSetting(KEY_LAST_SYNC_SUMMARY, summaryStr))

            val statusMsg = if (cloudReachable) {
                "Synchronized • Shift reports up to date"
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
                uploadedReports = uploadedReports,
                downloadedReports = downloadedReports,
                lastErrorMessage = null
            )

            Log.i(TAG, "Firebase Cloud Sync completed: $summaryStr (cloudReachable=$cloudReachable)")

            return@withContext FirebaseSyncResult(
                success = true,
                message = summaryStr,
                reportsSynced = totalReports,
                isCloudReachable = cloudReachable
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error during Firebase Cloud Sync: ${e.message}", e)
            failureReason = e.localizedMessage ?: "Unknown sync error"

            _syncState.value = _syncState.value.copy(
                isSyncing = false,
                statusMessage = "Cloud server unreachable — Local Room data safe and unaffected",
                lastErrorMessage = failureReason,
                isCloudReachable = false
            )

            return@withContext FirebaseSyncResult(
                success = false,
                message = "Cloud currently unreachable: $failureReason. Local Room data is unaffected.",
                isCloudReachable = false
            )
        }
    }

    private fun fetchRemoteData(context: Context): Pair<JSONObject?, Boolean> {
        if (!com.lojia.shiftreport.BuildConfig.CLOUD_SYNC_ENABLED) {
            return Pair(null, false)
        }
        val uid = getAuthUserUid() ?: return Pair(null, false)

        val customUrl = getCustomFirebaseUrl(context).trim()
        val projectId = getFirebaseProjectId(context)

        val targetUrl = if (customUrl.isNotEmpty()) {
            val trimmed = customUrl.trimEnd('/')
            if (trimmed.endsWith(".json")) {
                if (trimmed.endsWith("/shift_report_cloud_data.json")) {
                    trimmed.removeSuffix("/shift_report_cloud_data.json") + "/users/$uid/shift_report_cloud_data.json"
                } else {
                    trimmed
                }
            } else {
                "$trimmed/users/$uid/shift_report_cloud_data.json"
            }
        } else {
            "https://$projectId-default-rtdb.firebaseio.com/users/$uid/shift_report_cloud_data.json"
        }

        val authToken = getAuthToken()

        try {
            val url = URL(targetUrl)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 4000
                setRequestProperty("Accept", "application/json")
                if (!authToken.isNullOrBlank()) {
                    setRequestProperty("Authorization", "Bearer $authToken")
                }
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
                    saveLocalCloudBuffer(context, jsonObj)
                    return Pair(jsonObj, true)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Network attempt to Firebase failed (${e.message}). Reading from local buffer.")
        }

        val bufferObj = loadLocalCloudBuffer(context)
        return Pair(bufferObj, false)
    }

    private fun dispatchUploadToFirebase(context: Context, payload: JSONObject): Boolean {
        if (!com.lojia.shiftreport.BuildConfig.CLOUD_SYNC_ENABLED) {
            return false
        }
        val uid = getAuthUserUid() ?: return false

        saveLocalCloudBuffer(context, payload)

        val customUrl = getCustomFirebaseUrl(context).trim()
        val projectId = getFirebaseProjectId(context)

        val targetUrl = if (customUrl.isNotEmpty()) {
            val trimmed = customUrl.trimEnd('/')
            if (trimmed.endsWith(".json")) {
                if (trimmed.endsWith("/shift_report_cloud_data.json")) {
                    trimmed.removeSuffix("/shift_report_cloud_data.json") + "/users/$uid/shift_report_cloud_data.json"
                } else {
                    trimmed
                }
            } else {
                "$trimmed/users/$uid/shift_report_cloud_data.json"
            }
        } else {
            "https://$projectId-default-rtdb.firebaseio.com/users/$uid/shift_report_cloud_data.json"
        }

        val authToken = getAuthToken()

        return try {
            val url = URL(targetUrl)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "PUT"
                connectTimeout = 4000
                readTimeout = 4000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
                if (!authToken.isNullOrBlank()) {
                    setRequestProperty("Authorization", "Bearer $authToken")
                }
            }

            val writer = OutputStreamWriter(conn.outputStream, "UTF-8")
            writer.write(payload.toString())
            writer.flush()
            writer.close()

            val responseCode = conn.responseCode
            responseCode in 200..299
        } catch (e: Exception) {
            Log.w(TAG, "Upload to Firebase failed (${e.message}). Local buffer updated successfully.")
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

    private fun getEntityTimestamp(context: Context, entityType: String, id: Int, defaultTs: Long): Long {
        val prefs = context.getSharedPreferences("entity_timestamps", Context.MODE_PRIVATE)
        return prefs.getLong("${entityType}_$id", defaultTs)
    }

    private fun recordEntityUpdated(context: Context, entityType: String, id: Int, timestamp: Long) {
        val prefs = context.getSharedPreferences("entity_timestamps", Context.MODE_PRIVATE)
        prefs.edit().putLong("${entityType}_$id", timestamp).apply()
    }

    fun formatSyncTime(timestamp: Long): String {
        return formatTimestamp(timestamp)
    }

    private fun formatTimestamp(timestamp: Long): String {
        return try {
            com.lojia.shiftreport.util.DateTimeFormatUtils.formatDateTime(timestamp)
        } catch (e: Exception) {
            "Recently"
        }
    }
}
