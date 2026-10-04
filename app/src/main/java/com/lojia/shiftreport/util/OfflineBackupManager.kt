package com.lojia.shiftreport.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.lojia.shiftreport.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.*

data class BackupInfo(
    val fileName: String,
    val savedPath: String,
    val uri: Uri?,
    val totalRecords: Int,
    val timestamp: Long
)

data class BackupSummary(
    val exportDate: String,
    val shiftReportsCount: Int,
    val shiftSessionsCount: Int,
    val cashiersCount: Int,
    val totalRecords: Int,
    val hasBusinessProfile: Boolean,
    val hasUserProfile: Boolean
)

object OfflineBackupManager {

    suspend fun generateBackupJson(
        database: AppDatabase,
        isCloudBackup: Boolean = false
    ): Pair<String, Int> = withContext(Dispatchers.IO) {
        val reportDao = database.reportDao()

        val businessProfile = reportDao.getBusinessProfileOnce()
        val userProfile = reportDao.getUserProfileOnce()
        val receiptConfig = reportDao.getReceiptConfigOnce()
        val settings = reportDao.getAllSettingsList()
        val cashiers = reportDao.getAllCashiersList()
        val users = reportDao.getAllUsersList()
        val shiftReports = reportDao.getAllShiftReportsList()
        val shiftSessions = reportDao.getAllShiftSessionsList()
        val cashMovements = reportDao.getAllCashMovementsList()
        val auditLogs = reportDao.getAllAuditLogsList()

        val rootJson = JSONObject()
        rootJson.put("app", "Lojia Shift Report")
        rootJson.put("version", 1)
        rootJson.put("exportDate", DateTimeFormatUtils.formatIsoDateTimeSeconds())
        rootJson.put("timestamp", System.currentTimeMillis())
        rootJson.put("isCloudBackup", isCloudBackup)

        // Business Profile
        if (businessProfile != null) {
            val bpObj = JSONObject().apply {
                put("id", businessProfile.id)
                put("businessName", businessProfile.businessName)
                put("vatNumber", businessProfile.vatNumber)
                put("phone", businessProfile.phone)
                put("email", businessProfile.email)
                put("address", businessProfile.address)
                put("workingHours", businessProfile.workingHours)
                put("currency", businessProfile.currency)
                put("country", businessProfile.country)
                put("vatRate", businessProfile.vatRate)
                put("isTaxEnabled", businessProfile.isTaxEnabled)
                put("isTaxIncluded", businessProfile.isTaxIncluded)
                put("logoUri", businessProfile.logoUri)
            }
            rootJson.put("businessProfile", bpObj)
        }

        // User Profile (Never upload plain passwords or PINs in cloud backups)
        if (userProfile != null) {
            val upObj = JSONObject().apply {
                put("id", userProfile.id)
                put("fullName", userProfile.fullName)
                put("username", userProfile.username)
                put("email", userProfile.email)
                if (!isCloudBackup) {
                    put("passwordHash", userProfile.passwordHash)
                    put("pin", userProfile.pin)
                }
                put("phone", userProfile.phone)
                put("designation", userProfile.designation)
                put("address", userProfile.address)
                put("isBiometricEnabled", userProfile.isBiometricEnabled)
                put("autoLockMinutes", userProfile.autoLockMinutes)
                put("currentRole", userProfile.currentRole)
                put("registeredAt", userProfile.registeredAt)
                put("isRegistered", userProfile.isRegistered)
            }
            rootJson.put("userProfile", upObj)
        }

        // Receipt Config
        if (receiptConfig != null) {
            val rcObj = JSONObject().apply {
                put("id", receiptConfig.id)
                put("shopLogo", receiptConfig.shopLogo)
                put("customHeader", receiptConfig.customHeader)
                put("customFooterText", receiptConfig.customFooterText)
                put("showTaxNumber", receiptConfig.showTaxNumber)
                put("showCashierName", receiptConfig.showCashierName)
                put("showBarcode", receiptConfig.showBarcode)
                put("showCustomerMemo", receiptConfig.showCustomerMemo)
            }
            rootJson.put("shopReceiptConfig", rcObj)
        }

        // App Settings
        val settingsArr = JSONArray()
        settings.forEach { s ->
            settingsArr.put(JSONObject().apply {
                put("key", s.key)
                put("value", s.value)
            })
        }
        rootJson.put("settings", settingsArr)

        // Cashiers (Never upload PINs in cloud backups)
        val cashiersArr = JSONArray()
        cashiers.forEach { c ->
            cashiersArr.put(JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
                if (!isCloudBackup) {
                    put("pin", c.pin)
                }
                put("role", c.role)
                put("active", c.active)
            })
        }
        rootJson.put("cashiers", cashiersArr)

        // Users (Never upload passwords or PINs in cloud backups)
        val usersArr = JSONArray()
        users.forEach { u ->
            usersArr.put(JSONObject().apply {
                put("id", u.id)
                put("username", u.username)
                if (!isCloudBackup) {
                    put("passwordHash", u.passwordHash)
                    put("pin", u.pin)
                }
                put("role", u.role)
                put("createdAt", u.createdAt)
            })
        }
        rootJson.put("users", usersArr)

        // Shift Reports
        val shiftReportsArr = JSONArray()
        shiftReports.forEach { r ->
            shiftReportsArr.put(shiftReportToJson(r))
        }
        rootJson.put("shiftReports", shiftReportsArr)

        // Shift Sessions
        val shiftSessionsArr = JSONArray()
        shiftSessions.forEach { ss ->
            shiftSessionsArr.put(shiftSessionToJson(ss))
        }
        rootJson.put("shiftSessions", shiftSessionsArr)

        // Cash Movements
        val cashMovementsArr = JSONArray()
        cashMovements.forEach { cm ->
            cashMovementsArr.put(JSONObject().apply {
                put("id", cm.id)
                put("shiftSessionId", cm.shiftSessionId)
                put("cashierName", cm.cashierName)
                put("type", cm.type)
                put("amount", cm.amount)
                put("reason", cm.reason)
                put("timestamp", cm.timestamp)
            })
        }
        rootJson.put("cashMovements", cashMovementsArr)

        // Audit Logs
        val auditLogsArr = JSONArray()
        auditLogs.forEach { al ->
            auditLogsArr.put(JSONObject().apply {
                put("id", al.id)
                put("timestamp", al.timestamp)
                put("username", al.username)
                put("action", al.action)
                put("details", al.details)
            })
        }
        rootJson.put("auditLogs", auditLogsArr)
        rootJson.put("schemaVersion", 15)

        val totalRecords = shiftReports.size + shiftSessions.size + cashiers.size + users.size + cashMovements.size
        Pair(rootJson.toString(2), totalRecords)
    }

    suspend fun createBackup(
        context: Context,
        database: AppDatabase
    ): Result<BackupInfo> = withContext(Dispatchers.IO) {
        try {
            val (jsonString, totalRecords) = generateBackupJson(database, isCloudBackup = false)
            val jsonBytes = jsonString.toByteArray(Charsets.UTF_8)

            val timeStampStr = DateTimeFormatUtils.formatFileTimestampDash()
            val fileName = "Lojia_ShiftReport_Backup_$timeStampStr.json"

            var savedUri: Uri? = null
            var savedPath = ""

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Lojia")
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { os ->
                        os.write(jsonBytes)
                        os.flush()
                    }
                    savedUri = uri
                    savedPath = "Downloads/Lojia/$fileName"
                }
            } else {
                val downloadsDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Lojia").apply { mkdirs() }
                val targetFile = File(downloadsDir, fileName)
                FileOutputStream(targetFile).use { fos ->
                    fos.write(jsonBytes)
                    fos.flush()
                }
                savedPath = targetFile.absolutePath
            }

            val cacheExportDir = File(context.cacheDir, "backups").apply { mkdirs() }
            val cacheFile = File(cacheExportDir, fileName)
            cacheFile.writeBytes(jsonBytes)
            if (savedPath.isEmpty()) {
                savedPath = cacheFile.absolutePath
            }

            Result.success(
                BackupInfo(
                    fileName = fileName,
                    savedPath = savedPath,
                    uri = savedUri,
                    totalRecords = totalRecords,
                    timestamp = System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun writeBackupToUri(
        context: Context,
        database: AppDatabase,
        targetUri: Uri
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val (jsonString, totalRecords) = generateBackupJson(database, isCloudBackup = false)
            val jsonBytes = jsonString.toByteArray(Charsets.UTF_8)

            context.contentResolver.openOutputStream(targetUri)?.use { os ->
                os.write(jsonBytes)
                os.flush()
            }

            Result.success(totalRecords)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun parseBackupSummaryFromString(jsonString: String): Result<BackupSummary> {
        return try {
            val root = JSONObject(jsonString)

            val exportDate = root.optString("exportDate", "Unknown Date")
            val shiftReportsCount = root.optJSONArray("shiftReports")?.length() ?: 0
            val shiftSessionsCount = root.optJSONArray("shiftSessions")?.length() ?: 0
            val cashiersCount = root.optJSONArray("cashiers")?.length() ?: 0

            val totalRecords = shiftReportsCount + shiftSessionsCount + cashiersCount

            val summary = BackupSummary(
                exportDate = exportDate,
                shiftReportsCount = shiftReportsCount,
                shiftSessionsCount = shiftSessionsCount,
                cashiersCount = cashiersCount,
                totalRecords = totalRecords,
                hasBusinessProfile = root.has("businessProfile"),
                hasUserProfile = root.has("userProfile")
            )
            Result.success(summary)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun parseBackupSummary(
        context: Context,
        uri: Uri
    ): Result<BackupSummary> = withContext(Dispatchers.IO) {
        try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val jsonString = inputStream?.bufferedReader()?.use { it.readText() } ?: throw IllegalArgumentException("Could not read backup file")
            parseBackupSummaryFromString(jsonString)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getMoneyVal(obj: JSONObject, key: String, defaultMinorVal: Long = 0L, isLegacy: Boolean = false): Long {
        if (!obj.has(key) || obj.isNull(key)) return defaultMinorVal
        return if (isLegacy) {
            val value = obj.optDouble(key, MoneyFormat.toMajorUnits(defaultMinorVal))
            MoneyFormat.toMinorUnits(value)
        } else {
            obj.optLong(key, defaultMinorVal)
        }
    }

    fun shiftReportToJson(r: ShiftReport): JSONObject = JSONObject().apply {
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
    }

    fun jsonToShiftReport(obj: JSONObject, isLegacyBackup: Boolean = false): ShiftReport {
        val rawDueCredit = obj.optString("dueCreditEntriesJson", "[]")
        val rawPrevDue = obj.optString("previousDueCollectionsJson", "[]")
        val rawStaff = obj.optString("staffAdvancesJson", "[]")
        val rawUnpaid = obj.optString("unpaidBillsJson", "[]")
        val rawPurchased = obj.optString("purchasedItemsJson", "[]")

        val dueCreditJson = if (isLegacyBackup) AppDatabase.migrateJsonArraySingleAmount(rawDueCredit) else rawDueCredit
        val prevDueJson = if (isLegacyBackup) AppDatabase.migrateJsonArraySingleAmount(rawPrevDue) else rawPrevDue
        val staffJson = if (isLegacyBackup) AppDatabase.migrateJsonArraySingleAmount(rawStaff) else rawStaff
        val unpaidJson = if (isLegacyBackup) AppDatabase.migrateJsonArraySingleAmount(rawUnpaid) else rawUnpaid
        val purchasedJson = if (isLegacyBackup) AppDatabase.migrateJsonArrayPurchased(rawPurchased) else rawPurchased

        return ShiftReport(
            id = obj.optInt("id", 0),
            cashierName = obj.optString("cashierName", "Staff"),
            shift = obj.optString("shift", "Day"),
            dateInMillis = obj.optLong("dateInMillis", 0L),
            openingCash = getMoneyVal(obj, "openingCash", 0L, isLegacyBackup),
            closingCash = getMoneyVal(obj, "closingCash", 0L, isLegacyBackup),
            grossCash = getMoneyVal(obj, "grossCash", 0L, isLegacyBackup),
            madaPayments = getMoneyVal(obj, "madaPayments", 0L, isLegacyBackup),
            digitalWallet = getMoneyVal(obj, "digitalWallet", 0L, isLegacyBackup),
            totalDiscounts = getMoneyVal(obj, "totalDiscounts", 0L, isLegacyBackup),
            salesReturns = getMoneyVal(obj, "salesReturns", 0L, isLegacyBackup),
            staffMealsCount = obj.optInt("staffMealsCount", 0),
            totalExpenses = getMoneyVal(obj, "totalExpenses", 0L, isLegacyBackup),
            muasselQty = obj.optDouble("muasselQty", 0.0),
            outdoorShishaQty = obj.optDouble("outdoorShishaQty", 0.0),
            dueCreditEntriesJson = dueCreditJson,
            previousDueCollectionsJson = prevDueJson,
            staffAdvancesJson = staffJson,
            unpaidBillsJson = unpaidJson,
            purchasedItemsJson = purchasedJson,
            notes = obj.optString("notes", ""),
            isLocked = obj.optBoolean("isLocked", false),
            managerSignedBy = if (obj.has("managerSignedBy") && !obj.isNull("managerSignedBy")) obj.getString("managerSignedBy") else null,
            managerSignTime = if (obj.has("managerSignTime") && !obj.isNull("managerSignTime")) obj.getLong("managerSignTime") else null
        )
    }

    fun shiftSessionToJson(ss: ShiftSession): JSONObject = JSONObject().apply {
        put("id", ss.id)
        put("cashierName", ss.cashierName)
        put("shiftName", ss.shiftName)
        put("status", ss.status)
        put("openedAt", ss.openedAt)
        if (ss.closedAt != null) put("closedAt", ss.closedAt)
        put("startingCash", ss.startingCash)
        put("cashSales", ss.cashSales)
        put("cardSales", ss.cardSales)
        put("digitalSales", ss.digitalSales)
        put("totalDiscounts", ss.totalDiscounts)
        put("salesReturns", ss.salesReturns)
        put("totalPayIn", ss.totalPayIn)
        put("totalPayOut", ss.totalPayOut)
        put("expectedCash", ss.expectedCash)
        put("actualCashCount", ss.actualCashCount)
        put("variance", ss.variance)
        put("notes", ss.notes)
        put("isLocked", ss.isLocked)
        if (ss.managerSignedBy != null) put("managerSignedBy", ss.managerSignedBy)
        if (ss.managerSignTime != null) put("managerSignTime", ss.managerSignTime)
    }

    fun jsonToShiftSession(obj: JSONObject, isLegacyBackup: Boolean = false): ShiftSession {
        return ShiftSession(
            id = obj.optInt("id", 0),
            cashierName = obj.optString("cashierName", "Staff"),
            shiftName = obj.optString("shiftName", "Morning"),
            status = obj.optString("status", "OPEN"),
            openedAt = obj.optLong("openedAt", 0L),
            closedAt = if (obj.has("closedAt") && !obj.isNull("closedAt")) obj.getLong("closedAt") else null,
            startingCash = getMoneyVal(obj, "startingCash", 50000L, isLegacyBackup),
            cashSales = getMoneyVal(obj, "cashSales", 0L, isLegacyBackup),
            cardSales = getMoneyVal(obj, "cardSales", 0L, isLegacyBackup),
            digitalSales = getMoneyVal(obj, "digitalSales", 0L, isLegacyBackup),
            totalDiscounts = getMoneyVal(obj, "totalDiscounts", 0L, isLegacyBackup),
            salesReturns = getMoneyVal(obj, "salesReturns", 0L, isLegacyBackup),
            totalPayIn = getMoneyVal(obj, "totalPayIn", 0L, isLegacyBackup),
            totalPayOut = getMoneyVal(obj, "totalPayOut", 0L, isLegacyBackup),
            expectedCash = getMoneyVal(obj, "expectedCash", 50000L, isLegacyBackup),
            actualCashCount = getMoneyVal(obj, "actualCashCount", 0L, isLegacyBackup),
            variance = getMoneyVal(obj, "variance", 0L, isLegacyBackup),
            notes = obj.optString("notes", ""),
            isLocked = obj.optBoolean("isLocked", false),
            managerSignedBy = if (obj.has("managerSignedBy") && !obj.isNull("managerSignedBy")) obj.getString("managerSignedBy") else null,
            managerSignTime = if (obj.has("managerSignTime") && !obj.isNull("managerSignTime")) obj.getLong("managerSignTime") else null
        )
    }

    suspend fun restoreBackupFromString(
        jsonString: String,
        database: AppDatabase,
        sourceDescription: String = "backup.json",
        adminUser: String = "Admin"
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            val schemaVersion = root.optInt("schemaVersion", 1)
            val isLegacyBackup = schemaVersion < 15

            val reportDao = database.reportDao()

            var restoredCount = 0

            // 1. Business Profile
            if (root.has("businessProfile")) {
                val obj = root.getJSONObject("businessProfile")
                val bp = BusinessProfile(
                    id = obj.optInt("id", 1),
                    businessName = obj.optString("businessName", "My Store"),
                    vatNumber = obj.optString("vatNumber", ""),
                    phone = obj.optString("phone", ""),
                    email = obj.optString("email", ""),
                    address = obj.optString("address", ""),
                    workingHours = obj.optString("workingHours", "08:00 AM - 10:00 PM"),
                    currency = obj.optString("currency", "USD"),
                    country = obj.optString("country", "United States"),
                    vatRate = obj.optDouble("vatRate", 0.0),
                    isTaxEnabled = obj.optBoolean("isTaxEnabled", false),
                    isTaxIncluded = obj.optBoolean("isTaxIncluded", true),
                    logoUri = obj.optString("logoUri", "")
                )
                reportDao.saveBusinessProfile(bp)
                restoredCount++
            }

            // 2. User Profile (Preserve current password & PIN if not in backup)
            if (root.has("userProfile")) {
                val obj = root.getJSONObject("userProfile")
                val existing = reportDao.getUserProfileOnce()
                val passwordHash = if (obj.has("passwordHash") && obj.getString("passwordHash").isNotBlank()) {
                    obj.getString("passwordHash")
                } else {
                    existing?.passwordHash.orEmpty()
                }
                val pinVal = if (obj.has("pin") && obj.getString("pin").isNotBlank()) {
                    obj.getString("pin")
                } else {
                    existing?.pin.orEmpty()
                }

                val up = UserProfile(
                    id = obj.optInt("id", 1),
                    fullName = obj.optString("fullName", "Store Owner"),
                    username = obj.optString("username", "admin"),
                    email = obj.optString("email", "admin@lojia.com"),
                    passwordHash = passwordHash,
                    pin = pinVal,
                    phone = obj.optString("phone", ""),
                    designation = obj.optString("designation", "Owner"),
                    address = obj.optString("address", ""),
                    isBiometricEnabled = obj.optBoolean("isBiometricEnabled", false),
                    autoLockMinutes = obj.optInt("autoLockMinutes", 5),
                    currentRole = obj.optString("currentRole", "ADMIN"),
                    registeredAt = obj.optLong("registeredAt", System.currentTimeMillis()),
                    isRegistered = obj.optBoolean("isRegistered", true) && pinVal.isNotBlank()
                )
                reportDao.saveUserProfile(up)
                restoredCount++
            }

            // 3. Receipt Config
            if (root.has("shopReceiptConfig")) {
                val obj = root.getJSONObject("shopReceiptConfig")
                val rc = ShopReceiptConfig(
                    id = obj.optInt("id", 1),
                    shopLogo = obj.optString("shopLogo", "store_logo_default"),
                    customHeader = obj.optString("customHeader", "Welcome"),
                    customFooterText = obj.optString("customFooterText", "Thank you, visit again!"),
                    showTaxNumber = obj.optBoolean("showTaxNumber", true),
                    showCashierName = obj.optBoolean("showCashierName", true),
                    showBarcode = obj.optBoolean("showBarcode", true),
                    showCustomerMemo = obj.optBoolean("showCustomerMemo", true)
                )
                reportDao.saveReceiptConfig(rc)
                restoredCount++
            }

            // 4. App Settings
            val settingsArr = root.optJSONArray("settings")
            if (settingsArr != null) {
                val list = mutableListOf<AppSetting>()
                for (i in 0 until settingsArr.length()) {
                    val obj = settingsArr.getJSONObject(i)
                    list.add(AppSetting(key = obj.getString("key"), value = obj.getString("value")))
                }
                reportDao.insertSettings(list)
                restoredCount += list.size
            }

            // 5. Cashiers (Preserve local PIN if omitted in cloud backup)
            val cashiersArr = root.optJSONArray("cashiers")
            if (cashiersArr != null) {
                val list = mutableListOf<Cashier>()
                for (i in 0 until cashiersArr.length()) {
                    val obj = cashiersArr.getJSONObject(i)
                    val pinVal = if (obj.has("pin") && obj.getString("pin").isNotBlank()) obj.getString("pin") else ""
                    list.add(
                        Cashier(
                            id = obj.optInt("id", 0),
                            name = obj.getString("name"),
                            pin = pinVal,
                            role = obj.optString("role", "CASHIER"),
                            active = obj.optBoolean("active", true) && pinVal.isNotBlank()
                        )
                    )
                }
                reportDao.insertCashiers(list)
                restoredCount += list.size
            }

            // 6. Users (Preserve local password/PIN if omitted)
            val usersArr = root.optJSONArray("users")
            if (usersArr != null) {
                val list = mutableListOf<User>()
                for (i in 0 until usersArr.length()) {
                    val obj = usersArr.getJSONObject(i)
                    val passVal = if (obj.has("passwordHash") && obj.getString("passwordHash").isNotBlank()) obj.getString("passwordHash") else ""
                    val pinVal = if (obj.has("pin") && obj.getString("pin").isNotBlank()) obj.getString("pin") else ""
                    list.add(
                        User(
                            id = obj.optInt("id", 0),
                            username = obj.getString("username"),
                            passwordHash = passVal,
                            pin = pinVal,
                            role = obj.optString("role", "ADMIN"),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
                reportDao.insertUsers(list)
                restoredCount += list.size
            }

            // 7. Shift Reports
            val reportsArr = root.optJSONArray("shiftReports")
            if (reportsArr != null) {
                val list = mutableListOf<ShiftReport>()
                for (i in 0 until reportsArr.length()) {
                    val obj = reportsArr.getJSONObject(i)
                    list.add(jsonToShiftReport(obj, isLegacyBackup))
                }
                reportDao.insertShiftReports(list)
                restoredCount += list.size
            }

            // 8. Shift Sessions
            val sessionsArr = root.optJSONArray("shiftSessions")
            if (sessionsArr != null) {
                val list = mutableListOf<ShiftSession>()
                for (i in 0 until sessionsArr.length()) {
                    val obj = sessionsArr.getJSONObject(i)
                    list.add(jsonToShiftSession(obj, isLegacyBackup))
                }
                reportDao.insertShiftSessions(list)
                restoredCount += list.size
            }

            // 9. Cash Movements
            val cashMovementsArr = root.optJSONArray("cashMovements")
            if (cashMovementsArr != null) {
                val list = mutableListOf<CashMovement>()
                for (i in 0 until cashMovementsArr.length()) {
                    val obj = cashMovementsArr.getJSONObject(i)
                    list.add(
                        CashMovement(
                            id = obj.optInt("id", 0),
                            shiftSessionId = obj.getInt("shiftSessionId"),
                            cashierName = obj.getString("cashierName"),
                            type = obj.getString("type"),
                            amount = getMoneyVal(obj, "amount", 0L, isLegacyBackup),
                            reason = obj.optString("reason", ""),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
                reportDao.insertCashMovements(list)
                restoredCount += list.size
            }

            // Audit Log
            reportDao.insertAuditLog(
                AuditLog(
                    username = adminUser,
                    action = "RESTORE_DATA",
                    details = "Restored $restoredCount items from: $sourceDescription"
                )
            )

            Result.success(restoredCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreBackup(
        context: Context,
        uri: Uri,
        database: AppDatabase,
        adminUser: String = "Admin"
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val jsonString = inputStream?.bufferedReader()?.use { it.readText() } ?: throw IllegalArgumentException("Could not read backup file")
            restoreBackupFromString(jsonString, database, uri.lastPathSegment ?: "backup.json", adminUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun JSONObject.optBarcodeOrDefault(): Boolean {
        return if (has("showBarcode")) optBoolean("showBarcode", true) else true
    }
}
