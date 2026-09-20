package com.lojia.pos.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.lojia.pos.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
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
    val productsCount: Int,
    val categoriesCount: Int,
    val salesCount: Int,
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
        val posDao = database.posDao()

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

        val categories = posDao.getAllCategoriesList()
        val products = posDao.getAllProductsList()
        val modifiers = posDao.getAllModifiersList()
        val discounts = posDao.getAllDiscountsList()
        val sales = posDao.getAllSalesList()
        val saleItems = posDao.getAllSaleItemsList()
        val customers = posDao.getAllCustomersList()
        val employees = posDao.getAllEmployeesList()
        val suppliers = posDao.getAllSuppliersList()
        val stockAdjustments = posDao.getAllStockAdjustmentsList()

        val rootJson = JSONObject()
        rootJson.put("app", "Lojia POS")
        rootJson.put("version", 1)
        rootJson.put("exportDate", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date()))
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
            shiftReportsArr.put(JSONObject().apply {
                put("id", r.id)
                put("cashierName", r.cashierName)
                put("shift", r.shift)
                put("dateInMillis", r.dateInMillis)
                put("grossCash", r.grossCash)
                put("madaPayments", r.madaPayments)
                put("digitalWallet", r.digitalWallet)
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
            })
        }
        rootJson.put("shiftReports", shiftReportsArr)

        // Shift Sessions
        val shiftSessionsArr = JSONArray()
        shiftSessions.forEach { ss ->
            shiftSessionsArr.put(JSONObject().apply {
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
                put("totalPayIn", ss.totalPayIn)
                put("totalPayOut", ss.totalPayOut)
                put("expectedCash", ss.expectedCash)
                put("actualCashCount", ss.actualCashCount)
                put("variance", ss.variance)
                put("notes", ss.notes)
            })
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

        // Categories
        val categoriesArr = JSONArray()
        categories.forEach { cat ->
            categoriesArr.put(JSONObject().apply {
                put("id", cat.id)
                put("name", cat.name)
                put("iconName", cat.iconName)
                put("colorHex", cat.colorHex)
            })
        }
        rootJson.put("categories", categoriesArr)

        // Products
        val productsArr = JSONArray()
        products.forEach { p ->
            productsArr.put(JSONObject().apply {
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
            })
        }
        rootJson.put("products", productsArr)

        // Modifiers
        val modifiersArr = JSONArray()
        modifiers.forEach { m ->
            modifiersArr.put(JSONObject().apply {
                put("id", m.id)
                put("name", m.name)
                put("optionGroup", m.optionGroup)
                put("extraPrice", m.extraPrice)
                put("active", m.active)
            })
        }
        rootJson.put("modifiers", modifiersArr)

        // Discounts
        val discountsArr = JSONArray()
        discounts.forEach { d ->
            discountsArr.put(JSONObject().apply {
                put("id", d.id)
                put("name", d.name)
                put("percentage", d.percentage)
                put("fixedAmount", d.fixedAmount)
                put("isPercentage", d.isPercentage)
                put("code", d.code)
                put("active", d.active)
            })
        }
        rootJson.put("discounts", discountsArr)

        // Sales
        val salesArr = JSONArray()
        sales.forEach { s ->
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
        rootJson.put("sales", salesArr)

        // Sale Items
        val saleItemsArr = JSONArray()
        saleItems.forEach { si ->
            saleItemsArr.put(JSONObject().apply {
                put("id", si.id)
                put("saleId", si.saleId)
                put("productId", si.productId)
                put("productName", si.productName)
                put("quantity", si.quantity)
                put("unitPrice", si.unitPrice)
                put("totalPrice", si.totalPrice)
                put("selectedModifiers", si.selectedModifiers)
            })
        }
        rootJson.put("saleItems", saleItemsArr)

        // Customers
        val customersArr = JSONArray()
        customers.forEach { cust ->
            customersArr.put(JSONObject().apply {
                put("id", cust.id)
                put("name", cust.name)
                put("phone", cust.phone)
                put("email", cust.email)
                put("loyaltyPoints", cust.loyaltyPoints)
                put("totalSpent", cust.totalSpent)
            })
        }
        rootJson.put("customers", customersArr)

        // POS Employees (Never upload PINs in cloud backups)
        val employeesArr = JSONArray()
        employees.forEach { emp ->
            employeesArr.put(JSONObject().apply {
                put("id", emp.id)
                put("name", emp.name)
                put("role", emp.role)
                if (!isCloudBackup) {
                    put("pin", emp.pin)
                }
                put("active", emp.active)
            })
        }
        rootJson.put("employees", employeesArr)

        // POS Suppliers
        val suppliersArr = JSONArray()
        suppliers.forEach { sup ->
            suppliersArr.put(JSONObject().apply {
                put("id", sup.id)
                put("name", sup.name)
                put("contactPerson", sup.contactPerson)
                put("phone", sup.phone)
                put("email", sup.email)
            })
        }
        rootJson.put("suppliers", suppliersArr)

        // POS Stock Adjustments
        val stockAdjustmentsArr = JSONArray()
        stockAdjustments.forEach { sa ->
            stockAdjustmentsArr.put(JSONObject().apply {
                put("id", sa.id)
                put("productId", sa.productId)
                put("productName", sa.productName)
                put("quantityChange", sa.quantityChange)
                put("type", sa.type)
                put("reason", sa.reason)
                put("timestamp", sa.timestamp)
                put("username", sa.username)
            })
        }
        rootJson.put("stockAdjustments", stockAdjustmentsArr)

        val totalRecords = shiftReports.size + shiftSessions.size + sales.size + products.size + categories.size + cashiers.size + customers.size + employees.size
        Pair(rootJson.toString(2), totalRecords)
    }

    suspend fun createBackup(
        context: Context,
        database: AppDatabase
    ): Result<BackupInfo> = withContext(Dispatchers.IO) {
        try {
            val (jsonString, totalRecords) = generateBackupJson(database, isCloudBackup = false)
            val jsonBytes = jsonString.toByteArray(Charsets.UTF_8)

            // Format timestamp: YYYY-MM-DD_HH-mm
            val timeStampStr = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.US).format(Date())
            val fileName = "Lojia_Backup_$timeStampStr.json"

            var savedUri: Uri? = null
            var savedPath = ""

            // 1. Save to Downloads via MediaStore (Android 10+) or standard Downloads directory
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

            // 2. Also keep a fallback copy in cache for internal sharing if needed
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
            val productsCount = root.optJSONArray("products")?.length() ?: 0
            val categoriesCount = root.optJSONArray("categories")?.length() ?: 0
            val salesCount = root.optJSONArray("sales")?.length() ?: 0
            val cashiersCount = root.optJSONArray("cashiers")?.length() ?: 0
            val customersCount = root.optJSONArray("customers")?.length() ?: 0
            val employeesCount = root.optJSONArray("employees")?.length() ?: 0

            val totalRecords = shiftReportsCount + shiftSessionsCount + productsCount + categoriesCount + salesCount + cashiersCount + customersCount + employeesCount

            val summary = BackupSummary(
                exportDate = exportDate,
                shiftReportsCount = shiftReportsCount,
                shiftSessionsCount = shiftSessionsCount,
                productsCount = productsCount,
                categoriesCount = categoriesCount,
                salesCount = salesCount,
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

    suspend fun restoreBackupFromString(
        jsonString: String,
        database: AppDatabase,
        sourceDescription: String = "backup.json",
        adminUser: String = "Admin"
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            val reportDao = database.reportDao()
            val posDao = database.posDao()

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

            // 2. User Profile (Preserve local credentials if omitted in backup)
            if (root.has("userProfile")) {
                val obj = root.getJSONObject("userProfile")
                val existingProfile = reportDao.getUserProfileOnce()
                val resolvedPin = if (obj.has("pin") && obj.getString("pin").isNotBlank()) obj.getString("pin") else (existingProfile?.pin ?: SecurityUtils.hashSecret("1111"))
                val resolvedPass = if (obj.has("passwordHash") && obj.getString("passwordHash").isNotBlank()) obj.getString("passwordHash") else (existingProfile?.passwordHash ?: "")

                val up = UserProfile(
                    id = obj.optInt("id", 1),
                    fullName = obj.optString("fullName", existingProfile?.fullName ?: "Store Owner"),
                    username = obj.optString("username", existingProfile?.username ?: ""),
                    email = obj.optString("email", existingProfile?.email ?: ""),
                    passwordHash = resolvedPass,
                    phone = obj.optString("phone", existingProfile?.phone ?: ""),
                    designation = obj.optString("designation", existingProfile?.designation ?: "Store Owner & Manager"),
                    address = obj.optString("address", existingProfile?.address ?: ""),
                    pin = resolvedPin,
                    isBiometricEnabled = obj.optBoolean("isBiometricEnabled", existingProfile?.isBiometricEnabled ?: false),
                    autoLockMinutes = obj.optInt("autoLockMinutes", existingProfile?.autoLockMinutes ?: 5),
                    currentRole = obj.optString("currentRole", existingProfile?.currentRole ?: "ADMIN"),
                    registeredAt = obj.optLong("registeredAt", existingProfile?.registeredAt ?: System.currentTimeMillis()),
                    isRegistered = obj.optBoolean("isRegistered", existingProfile?.isRegistered ?: true)
                )
                reportDao.saveUserProfile(up)
                restoredCount++
            }

            // 3. Shop Receipt Config
            if (root.has("shopReceiptConfig")) {
                val obj = root.getJSONObject("shopReceiptConfig")
                val rc = ShopReceiptConfig(
                    id = obj.optInt("id", 1),
                    shopLogo = obj.optString("shopLogo", "store_logo_default"),
                    customHeader = obj.optString("customHeader", "Welcome"),
                    customFooterText = obj.optString("customFooterText", "Thank you, visit again!"),
                    showTaxNumber = obj.optBoolean("showTaxNumber", true),
                    showCashierName = obj.optBoolean("showCashierName", true),
                    showBarcode = obj.optBarcodeOrDefault(),
                    showCustomerMemo = obj.optBoolean("showCustomerMemo", true)
                )
                reportDao.saveReceiptConfig(rc)
                restoredCount++
            }

            // 4. Settings
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

            // 5. Cashiers (Preserve local PIN if omitted)
            val cashiersArr = root.optJSONArray("cashiers")
            if (cashiersArr != null) {
                val existingCashiers = reportDao.getAllCashiersList().associateBy { it.name }
                val list = mutableListOf<Cashier>()
                for (i in 0 until cashiersArr.length()) {
                    val obj = cashiersArr.getJSONObject(i)
                    val cName = obj.getString("name")
                    val existing = existingCashiers[cName]
                    val pinVal = if (obj.has("pin") && obj.getString("pin").isNotBlank()) obj.getString("pin") else (existing?.pin ?: SecurityUtils.hashSecret("1111"))
                    list.add(
                        Cashier(
                            id = obj.optInt("id", 0),
                            name = cName,
                            pin = pinVal,
                            role = obj.optString("role", "CASHIER"),
                            active = obj.optBoolean("active", true)
                        )
                    )
                }
                reportDao.insertCashiers(list)
                restoredCount += list.size
            }

            // 6. Users (Preserve local password/PIN if omitted)
            val usersArr = root.optJSONArray("users")
            if (usersArr != null) {
                val existingUsers = reportDao.getAllUsersList().associateBy { it.username }
                val list = mutableListOf<User>()
                for (i in 0 until usersArr.length()) {
                    val obj = usersArr.getJSONObject(i)
                    val uName = obj.getString("username")
                    val existing = existingUsers[uName]
                    val passVal = if (obj.has("passwordHash") && obj.getString("passwordHash").isNotBlank()) obj.getString("passwordHash") else (existing?.passwordHash ?: "")
                    val pinVal = if (obj.has("pin") && obj.getString("pin").isNotBlank()) obj.getString("pin") else (existing?.pin ?: "")

                    list.add(
                        User(
                            id = obj.optInt("id", 0),
                            username = uName,
                            passwordHash = passVal,
                            role = obj.optString("role", "ADMIN"),
                            pin = pinVal,
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
                reportDao.insertUsers(list)
                restoredCount += list.size
            }

            // 7. Shift Reports
            val shiftReportsArr = root.optJSONArray("shiftReports")
            if (shiftReportsArr != null) {
                val list = mutableListOf<ShiftReport>()
                for (i in 0 until shiftReportsArr.length()) {
                    val obj = shiftReportsArr.getJSONObject(i)
                    list.add(
                        ShiftReport(
                            id = obj.optInt("id", 0),
                            cashierName = obj.optString("cashierName", "Staff"),
                            shift = obj.optString("shift", "Day"),
                            dateInMillis = obj.optLong("dateInMillis", System.currentTimeMillis()),
                            grossCash = obj.optDouble("grossCash", 0.0),
                            madaPayments = obj.optDouble("madaPayments", 0.0),
                            digitalWallet = obj.optDouble("digitalWallet", 0.0),
                            staffMealsCount = obj.optInt("staffMealsCount", 0),
                            totalExpenses = obj.optDouble("totalExpenses", 0.0),
                            muasselQty = obj.optDouble("muasselQty", 0.0),
                            outdoorShishaQty = obj.optDouble("outdoorShishaQty", 0.0),
                            dueCreditEntriesJson = obj.optString("dueCreditEntriesJson", "[]"),
                            previousDueCollectionsJson = obj.optString("previousDueCollectionsJson", "[]"),
                            staffAdvancesJson = obj.optString("staffAdvancesJson", "[]"),
                            unpaidBillsJson = obj.optString("unpaidBillsJson", "[]"),
                            purchasedItemsJson = obj.optString("purchasedItemsJson", "[]"),
                            notes = obj.optString("notes", "")
                        )
                    )
                }
                reportDao.insertShiftReports(list)
                restoredCount += list.size
            }

            // 8. Shift Sessions
            val shiftSessionsArr = root.optJSONArray("shiftSessions")
            if (shiftSessionsArr != null) {
                val list = mutableListOf<ShiftSession>()
                for (i in 0 until shiftSessionsArr.length()) {
                    val obj = shiftSessionsArr.getJSONObject(i)
                    list.add(
                        ShiftSession(
                            id = obj.optInt("id", 0),
                            cashierName = obj.optString("cashierName", "Staff"),
                            shiftName = obj.optString("shiftName", "Morning"),
                            status = obj.optString("status", "CLOSED"),
                            openedAt = obj.optLong("openedAt", System.currentTimeMillis()),
                            closedAt = if (obj.has("closedAt")) obj.getLong("closedAt") else null,
                            startingCash = obj.optDouble("startingCash", 500.0),
                            cashSales = obj.optDouble("cashSales", 0.0),
                            cardSales = obj.optDouble("cardSales", 0.0),
                            digitalSales = obj.optDouble("digitalSales", 0.0),
                            totalPayIn = obj.optDouble("totalPayIn", 0.0),
                            totalPayOut = obj.optDouble("totalPayOut", 0.0),
                            expectedCash = obj.optDouble("expectedCash", 500.0),
                            actualCashCount = obj.optDouble("actualCashCount", 0.0),
                            variance = obj.optDouble("variance", 0.0),
                            notes = obj.optString("notes", "")
                        )
                    )
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
                            cashierName = obj.optString("cashierName", "Staff"),
                            type = obj.optString("type", "PAY_IN"),
                            amount = obj.getDouble("amount"),
                            reason = obj.optString("reason", ""),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
                reportDao.insertCashMovements(list)
                restoredCount += list.size
            }

            // 10. Categories
            val categoriesArr = root.optJSONArray("categories")
            if (categoriesArr != null) {
                val list = mutableListOf<POSCategory>()
                for (i in 0 until categoriesArr.length()) {
                    val obj = categoriesArr.getJSONObject(i)
                    list.add(
                        POSCategory(
                            id = obj.optInt("id", 0),
                            name = obj.getString("name"),
                            iconName = obj.optString("iconName", obj.optString("icon", "Category")),
                            colorHex = obj.optString("colorHex", obj.optString("color", "#6366F1"))
                        )
                    )
                }
                posDao.insertCategories(list)
                restoredCount += list.size
            }

            // 11. Products
            val productsArr = root.optJSONArray("products")
            if (productsArr != null) {
                val list = mutableListOf<POSProduct>()
                for (i in 0 until productsArr.length()) {
                    val obj = productsArr.getJSONObject(i)
                    list.add(
                        POSProduct(
                            id = obj.optInt("id", 0),
                            name = obj.getString("name"),
                            categoryId = obj.optInt("categoryId", 1),
                            price = obj.getDouble("price"),
                            costPrice = obj.optDouble("costPrice", 0.0),
                            stockQuantity = obj.optDouble("stockQuantity", 100.0),
                            minStockAlert = obj.optDouble("minStockAlert", 10.0),
                            barcode = obj.optString("barcode", ""),
                            sku = obj.optString("sku", ""),
                            unit = obj.optString("unit", "pcs"),
                            colorHex = obj.optString("colorHex", obj.optString("color", "#10B981")),
                            active = obj.optBoolean("active", true)
                        )
                    )
                }
                posDao.insertProducts(list)
                restoredCount += list.size
            }

            // 12. Modifiers
            val modifiersArr = root.optJSONArray("modifiers")
            if (modifiersArr != null) {
                val list = mutableListOf<POSModifier>()
                for (i in 0 until modifiersArr.length()) {
                    val obj = modifiersArr.getJSONObject(i)
                    list.add(
                        POSModifier(
                            id = obj.optInt("id", 0),
                            name = obj.getString("name"),
                            optionGroup = obj.optString("optionGroup", obj.optString("category", "General")),
                            extraPrice = obj.optDouble("extraPrice", obj.optDouble("price", 0.0)),
                            active = obj.optBoolean("active", true)
                        )
                    )
                }
                posDao.insertModifiers(list)
                restoredCount += list.size
            }

            // 13. Discounts
            val discountsArr = root.optJSONArray("discounts")
            if (discountsArr != null) {
                val list = mutableListOf<POSDiscount>()
                for (i in 0 until discountsArr.length()) {
                    val obj = discountsArr.getJSONObject(i)
                    list.add(
                        POSDiscount(
                            id = obj.optInt("id", 0),
                            name = obj.getString("name"),
                            percentage = obj.optDouble("percentage", obj.optDouble("value", 0.0)),
                            fixedAmount = obj.optDouble("fixedAmount", 0.0),
                            isPercentage = obj.optBoolean("isPercentage", obj.optString("type", "PERCENTAGE") == "PERCENTAGE"),
                            code = obj.optString("code", ""),
                            active = obj.optBoolean("active", true)
                        )
                    )
                }
                posDao.insertDiscounts(list)
                restoredCount += list.size
            }

            // 14. Sales
            val salesArr = root.optJSONArray("sales")
            if (salesArr != null) {
                val list = mutableListOf<POSSale>()
                for (i in 0 until salesArr.length()) {
                    val obj = salesArr.getJSONObject(i)
                    list.add(
                        POSSale(
                            id = obj.optInt("id", 0),
                            invoiceNumber = obj.optString("invoiceNumber", "INV-${System.currentTimeMillis()}"),
                            cashierName = obj.optString("cashierName", "Staff"),
                            customerName = obj.optString("customerName", "Walk-in Customer"),
                            subtotal = obj.optDouble("subtotal", 0.0),
                            vatAmount = obj.optDouble("vatAmount", 0.0),
                            totalAmount = obj.getDouble("totalAmount"),
                            paymentMethod = obj.optString("paymentMethod", "CASH"),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            isVoided = obj.optBoolean("isVoided", false),
                            voidReason = obj.optString("voidReason", "")
                        )
                    )
                }
                posDao.insertSales(list)
                restoredCount += list.size
            }

            // 15. Sale Items
            val saleItemsArr = root.optJSONArray("saleItems")
            if (saleItemsArr != null) {
                val list = mutableListOf<POSSaleItem>()
                for (i in 0 until saleItemsArr.length()) {
                    val obj = saleItemsArr.getJSONObject(i)
                    list.add(
                        POSSaleItem(
                            id = obj.optInt("id", 0),
                            saleId = obj.getInt("saleId"),
                            productId = obj.getInt("productId"),
                            productName = obj.optString("productName", "Product"),
                            quantity = obj.getDouble("quantity"),
                            unitPrice = obj.getDouble("unitPrice"),
                            totalPrice = obj.getDouble("totalPrice"),
                            selectedModifiers = obj.optString("selectedModifiers", "")
                        )
                    )
                }
                posDao.insertSaleItems(list)
                restoredCount += list.size
            }

            // 16. Customers
            val customersArr = root.optJSONArray("customers")
            if (customersArr != null) {
                val list = mutableListOf<POSCustomer>()
                for (i in 0 until customersArr.length()) {
                    val obj = customersArr.getJSONObject(i)
                    list.add(
                        POSCustomer(
                            id = obj.optInt("id", 0),
                            name = obj.getString("name"),
                            phone = obj.optString("phone", ""),
                            email = obj.optString("email", ""),
                            loyaltyPoints = obj.optInt("loyaltyPoints", 0),
                            totalSpent = obj.optDouble("totalSpent", 0.0)
                        )
                    )
                }
                posDao.insertCustomers(list)
                restoredCount += list.size
            }

            // 17. Employees (Preserve local PIN if omitted)
            val employeesArr = root.optJSONArray("employees")
            if (employeesArr != null) {
                val list = mutableListOf<POSEmployee>()
                for (i in 0 until employeesArr.length()) {
                    val obj = employeesArr.getJSONObject(i)
                    val pinVal = if (obj.has("pin") && obj.getString("pin").isNotBlank()) obj.getString("pin") else SecurityUtils.hashSecret("1234")
                    list.add(
                        POSEmployee(
                            id = obj.optInt("id", 0),
                            name = obj.getString("name"),
                            role = obj.optString("role", "CASHIER"),
                            pin = pinVal,
                            active = obj.optBoolean("active", true)
                        )
                    )
                }
                posDao.insertEmployees(list)
                restoredCount += list.size
            }

            // 18. Suppliers
            val suppliersArr = root.optJSONArray("suppliers")
            if (suppliersArr != null) {
                val list = mutableListOf<POSSupplier>()
                for (i in 0 until suppliersArr.length()) {
                    val obj = suppliersArr.getJSONObject(i)
                    list.add(
                        POSSupplier(
                            id = obj.optInt("id", 0),
                            name = obj.getString("name"),
                            contactPerson = obj.optString("contactPerson", ""),
                            phone = obj.optString("phone", ""),
                            email = obj.optString("email", "")
                        )
                    )
                }
                posDao.insertSuppliers(list)
                restoredCount += list.size
            }

            // 19. Stock Adjustments
            val stockAdjustmentsArr = root.optJSONArray("stockAdjustments")
            if (stockAdjustmentsArr != null) {
                val list = mutableListOf<POSStockAdjustment>()
                for (i in 0 until stockAdjustmentsArr.length()) {
                    val obj = stockAdjustmentsArr.getJSONObject(i)
                    list.add(
                        POSStockAdjustment(
                            id = obj.optInt("id", 0),
                            productId = obj.getInt("productId"),
                            productName = obj.optString("productName", "Product"),
                            quantityChange = obj.getDouble("quantityChange"),
                            type = obj.optString("type", "ADJUSTMENT"),
                            reason = obj.optString("reason", ""),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            username = obj.optString("username", adminUser)
                        )
                    )
                }
                posDao.insertStockAdjustments(list)
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
