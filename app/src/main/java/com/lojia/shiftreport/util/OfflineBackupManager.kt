package com.lojia.shiftreport.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.room.withTransaction
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
        isCloudBackup: Boolean = false,
        currentUserId: String? = null
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

        val ownerId = currentUserId ?: userProfile?.id?.toString() ?: userProfile?.username ?: "anonymous"
        rootJson.put("ownerId", ownerId)

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
                put("isActive", c.isActive)
                put("isArchived", c.isArchived)
                c.archivedAt?.let { put("archivedAt", it) }
                put("createdByAdminId", c.createdByAdminId)
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
        rootJson.put("shifts", shiftReportsArr)

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

    private const val BACKUP_MAGIC_HEADER = "LOJIA_ENC_V1"
    private const val LEGACY_READ_ONLY_SECRET = "LojiaSecureMasterKey@2026#Aes256"

    private fun legacyDecryptPayload(payloadBytes: ByteArray, password: String): String {
        val headerBytes = BACKUP_MAGIC_HEADER.toByteArray(Charsets.UTF_8)
        if (payloadBytes.size >= headerBytes.size + 16 + 12 &&
            payloadBytes.take(headerBytes.size).toByteArray().contentEquals(headerBytes)) {
            val salt = payloadBytes.copyOfRange(headerBytes.size, headerBytes.size + 16)
            val iv = payloadBytes.copyOfRange(headerBytes.size + 16, headerBytes.size + 16 + 12)
            val ciphertext = payloadBytes.copyOfRange(headerBytes.size + 16 + 12, payloadBytes.size)
            val keySpec = javax.crypto.spec.PBEKeySpec(password.toCharArray(), salt, 10000, 256)
            val secretKey = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(keySpec)
            val aesKey = javax.crypto.spec.SecretKeySpec(secretKey.encoded, "AES")
            val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(javax.crypto.Cipher.DECRYPT_MODE, aesKey, javax.crypto.spec.GCMParameterSpec(128, iv))
            return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        }
        return String(payloadBytes, Charsets.UTF_8)
    }

    internal fun decodeBackupToJson(bytes: ByteArray, password: String?): String {
        val passwordChars = password?.toCharArray()
        return try {
            BackupFileDecoder.decodeToJson(
                bytes = bytes,
                kekFor = { tier, salt ->
                    when (tier) {
                        TIER_DEVICE -> getOrCreateDeviceKek()
                            ?: throw BackupFormatException("device key unavailable")
                        TIER_PORTABLE -> {
                            val chars = passwordChars
                                ?: throw BackupPasswordRequiredException("password required")
                            deriveBackupKek(chars.copyOf(), salt)
                        }
                        else -> throw BackupFormatException("unknown tier")
                    }
                },
                legacyDecrypt = { raw ->
                    legacyDecryptPayload(raw, password ?: LEGACY_READ_ONLY_SECRET)
                }
            )
        } finally {
            passwordChars?.fill('\u0000')
        }
    }

    internal suspend fun createPortableBackupBytes(
        database: AppDatabase,
        password: CharArray,
        isCloudBackup: Boolean = false
    ): Pair<ByteArray, Int> = withContext(Dispatchers.IO) {
        val (jsonString, totalRecords) = generateBackupJson(database, isCloudBackup = isCloudBackup)
        val plainJsonBytes = jsonString.toByteArray(Charsets.UTF_8)
        val salt = generateBackupSalt()
        val kek = deriveBackupKek(password.copyOf(), salt)
        val encryptedBytes = encryptBackup(plainJsonBytes, kek, TIER_PORTABLE, salt)
        Pair(encryptedBytes, totalRecords)
    }

    fun isBackupPasswordOrFormatError(error: Throwable?): Boolean {
        return generateSequence(error) { it.cause }.any { cause ->
            cause is BackupPasswordRequiredException ||
                cause is BackupFormatException ||
                cause is javax.crypto.AEADBadTagException ||
                cause is javax.crypto.BadPaddingException ||
                cause is java.security.GeneralSecurityException
        }
    }

    suspend fun createBackup(
        context: Context,
        database: AppDatabase,
        password: CharArray? = null
    ): Result<BackupInfo> = withContext(Dispatchers.IO) {
        try {
            if (password == null) {
                return@withContext Result.failure(BackupPasswordRequiredException("password required"))
            }
            val (jsonString, totalRecords) = generateBackupJson(database, isCloudBackup = false)
            val plainJsonBytes = jsonString.toByteArray(Charsets.UTF_8)
            val salt = ByteArray(16).apply { java.security.SecureRandom().nextBytes(this) }
            val kek = deriveBackupKek(password.copyOf(), salt)
            val encryptedBytes = encryptBackup(plainJsonBytes, kek, TIER_PORTABLE, salt)

            val timeStampStr = DateTimeFormatUtils.formatFileTimestampDash()
            val fileName = "Lojia_ShiftReport_Backup_$timeStampStr.lsb"

            var savedUri: Uri? = null
            var savedPath = ""

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Lojia")
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { os ->
                        os.write(encryptedBytes)
                        os.flush()
                    }
                    savedUri = uri
                    savedPath = "Downloads/Lojia/$fileName"
                }
            } else {
                val downloadsDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Lojia").apply { mkdirs() }
                val targetFile = File(downloadsDir, fileName)
                FileOutputStream(targetFile).use { fos ->
                    fos.write(encryptedBytes)
                    fos.flush()
                }
                savedPath = targetFile.absolutePath
            }

            val cacheExportDir = File(context.cacheDir, "backups").apply { mkdirs() }
            val cacheFile = File(cacheExportDir, fileName)
            cacheFile.writeBytes(encryptedBytes)
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

    /**
     * Resolves the dedicated Backups directory inside the app reports folder.
     */
    fun getBackupsDirectory(context: Context): File {
        val baseDir = PdfShareUtils.getAppReportsDirectory(context)
        val backupsDir = File(baseDir, "Backups")
        if (!backupsDir.exists()) {
            backupsDir.mkdirs()
        }
        return backupsDir
    }

    private fun rotateAutoBackups(backupsDir: File) {
        try {
            val autoFiles = backupsDir.listFiles { f ->
                f.isFile && (
                    (f.name.startsWith("portable_backup_") && f.name.endsWith(".lsb")) ||
                        (f.name.startsWith("lojia_auto_backup_") && f.name.endsWith(".json"))
                    )
            }?.sortedByDescending { it.lastModified() } ?: emptyList()

            if (autoFiles.size > 5) {
                for (oldFile in autoFiles.drop(5)) {
                    oldFile.delete()
                }
            }
        } catch (_: Exception) {}
    }

    private fun rotateDeviceSnapshots(snapshotDir: File) {
        try {
            val snapshotFiles = snapshotDir.listFiles { f ->
                f.isFile && f.name.startsWith("device_snapshot_") && f.name.endsWith(".lsb")
            }?.sortedByDescending { it.lastModified() } ?: emptyList()

            if (snapshotFiles.size > 3) {
                for (oldFile in snapshotFiles.drop(3)) {
                    oldFile.delete()
                }
            }
        } catch (_: Exception) {}
    }

    /**
     * Stores metadata in an external file to verify preservation across uninstalls.
     */
    fun saveReinstallMeta(context: Context, preserve: Boolean) {
        try {
            val baseDir = PdfShareUtils.getAppReportsDirectory(context)
            val metaFile = File(baseDir, ".lojia_meta")
            val obj = JSONObject().apply {
                put("has_run_before", true)
                put("preserve_data_on_uninstall", preserve)
                put("last_active_time", System.currentTimeMillis())
            }
            metaFile.writeText(obj.toString())
        } catch (_: Exception) {}
    }

    /**
     * Reads metadata stored across uninstalls.
     */
    fun readReinstallMeta(context: Context): Pair<Boolean, Boolean> {
        return try {
            val baseDir = PdfShareUtils.getAppReportsDirectory(context)
            val metaFile = File(baseDir, ".lojia_meta")
            if (metaFile.exists()) {
                val obj = JSONObject(metaFile.readText())
                val hasRun = obj.optBoolean("has_run_before", false)
                val preserve = obj.optBoolean("preserve_data_on_uninstall", true)
                Pair(hasRun, preserve)
            } else {
                Pair(false, true)
            }
        } catch (_: Exception) {
            Pair(false, true)
        }
    }

    /**
     * Creates a device-encrypted snapshot in app-private storage and, if the password vault
     * can be opened, also writes a portable backup copy to the uninstall Backups folder.
     */
    suspend fun createAutoBackup(
        context: Context,
        database: AppDatabase
    ): Result<BackupInfo> = withContext(Dispatchers.IO) {
        try {
            val deviceKek = getOrCreateDeviceKek()
                ?: return@withContext Result.failure(IllegalStateException("device key unavailable"))
            val (jsonString, totalRecords) = generateBackupJson(database, isCloudBackup = false)
            val plainJsonBytes = jsonString.toByteArray(Charsets.UTF_8)

            val timeStampStr = DateTimeFormatUtils.formatFileTimestamp()
            val snapshotDir = File(context.filesDir, "backups").apply { mkdirs() }
            val snapshotFile = File(snapshotDir, "device_snapshot_$timeStampStr.lsb")
            val deviceBytes = encryptBackup(plainJsonBytes, deviceKek, TIER_DEVICE, null)
            snapshotFile.writeBytes(deviceBytes)
            rotateDeviceSnapshots(snapshotDir)

            val vaultBlob = loadVault(context)
            if (vaultBlob != null) {
                var passwordChars: CharArray? = null
                try {
                    passwordChars = open(vaultBlob, deviceKek)
                    val salt = ByteArray(16).apply { java.security.SecureRandom().nextBytes(this) }
                    val portableKek = deriveBackupKek(passwordChars.copyOf(), salt)
                    val portableBytes = encryptBackup(plainJsonBytes, portableKek, TIER_PORTABLE, salt)
                    val backupsDir = getBackupsDirectory(context)
                    val portableFile = File(backupsDir, "portable_backup_$timeStampStr.lsb")
                    portableFile.writeBytes(portableBytes)
                    saveReinstallMeta(context, true)
                    rotateAutoBackups(backupsDir)
                } catch (_: Exception) {
                    // Vault missing/unreadable in Stage 19B skips portable copy silently without deleting vault.
                } finally {
                    passwordChars?.fill('\u0000')
                }
            }

            Result.success(
                BackupInfo(
                    fileName = snapshotFile.name,
                    savedPath = snapshotFile.absolutePath,
                    uri = null,
                    totalRecords = totalRecords,
                    timestamp = System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Locates the latest available JSON backup file in the Backups or Reports directories.
     */
    fun findLatestBackupFile(context: Context): File? {
        return try {
            val backupsDir = getBackupsDirectory(context)
            val filesInBackups = backupsDir.listFiles { f ->
                f.isFile && (f.name.endsWith(".lsb") || f.name.endsWith(".json"))
            }?.toList() ?: emptyList()

            val baseDir = PdfShareUtils.getAppReportsDirectory(context)
            val filesInBase = baseDir.listFiles { f ->
                f.isFile && (f.name.endsWith(".lsb") || f.name.endsWith(".json"))
            }?.toList() ?: emptyList()

            val allFiles = (filesInBackups + filesInBase).distinctBy { it.absolutePath }
                .sortedByDescending { it.lastModified() }

            allFiles.firstOrNull()
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Restores backup from a local file.
     */
    suspend fun restoreBackupFromFile(
        context: Context,
        database: AppDatabase,
        backupFile: File,
        password: String? = null
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val bytes = backupFile.readBytes()
            val jsonString = decodeBackupToJson(bytes, password)
            restoreBackupFromString(jsonString, database, backupFile.name)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Clears all database tables and resets preferences to start fresh.
     */
    suspend fun wipeAllData(
        context: Context,
        database: AppDatabase
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.clearAllTables()
            PreferencesRepository.getInstance(context).clearAll()
            saveReinstallMeta(context, false)
            ConfigurationSyncManager.getInstance(context).refreshCurrencyLock()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun writeBackupToUri(
        context: Context,
        database: AppDatabase,
        targetUri: Uri,
        password: CharArray? = null
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            if (password == null) {
                return@withContext Result.failure(BackupPasswordRequiredException("password required"))
            }
            val (jsonString, totalRecords) = generateBackupJson(database, isCloudBackup = false)
            val plainJsonBytes = jsonString.toByteArray(Charsets.UTF_8)
            val salt = ByteArray(16).apply { java.security.SecureRandom().nextBytes(this) }
            val kek = deriveBackupKek(password.copyOf(), salt)
            val encryptedBytes = encryptBackup(plainJsonBytes, kek, TIER_PORTABLE, salt)

            context.contentResolver.openOutputStream(targetUri)?.use { os ->
                os.write(encryptedBytes)
                os.flush()
            } ?: throw IllegalStateException("Could not open backup output stream")

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

    fun parseBackupSummaryFromBytes(bytes: ByteArray): Result<BackupSummary> {
        return try {
            if (backupTierOf(bytes) == TIER_PORTABLE) {
                return Result.failure(BackupPasswordRequiredException("password required"))
            }
            val jsonString = decodeBackupToJson(bytes, password = null)
            parseBackupSummaryFromString(jsonString)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun parseBackupSummary(
        context: Context,
        uri: Uri
    ): Result<BackupSummary> = withContext(Dispatchers.IO) {
        try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw IllegalArgumentException("Could not read backup file")
            parseBackupSummaryFromBytes(bytes)
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
        put("cashierId", r.cashierId)
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
            cashierId = if (!obj.has("cashierId") || obj.isNull("cashierId")) "" else obj.optString("cashierId", ""),
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

            val restoredCount = database.withTransaction {
                var count = 0

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
                        logoUri = obj.optString("logoUri", "")
                    )
                    reportDao.saveBusinessProfile(bp)
                    count++
                }

                // 2. User Profile (Preserve current password & PIN if not in backup)
                if (root.has("userProfile")) {
                    val obj = root.getJSONObject("userProfile")
                    val existing = reportDao.getUserProfileOnce()
                    val backupPasswordHash = if (obj.has("passwordHash") && !obj.isNull("passwordHash")) obj.getString("passwordHash") else null
                    val backupPin = if (obj.has("pin") && !obj.isNull("pin")) obj.getString("pin") else null
                    val passwordHash = mergePinHash(existing?.passwordHash, backupPasswordHash).orEmpty()
                    val pinVal = mergePinHash(existing?.pin, backupPin).orEmpty()

                    val isRegistered = if (existing != null) {
                        existing.isRegistered || (obj.optBoolean("isRegistered", true) && (pinVal.isNotBlank() || passwordHash.isNotBlank()))
                    } else {
                        obj.optBoolean("isRegistered", true) && (pinVal.isNotBlank() || passwordHash.isNotBlank())
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
                        isRegistered = isRegistered
                    )
                    reportDao.saveUserProfile(up)
                    count++
                }

                // 3. Receipt Config
                if (root.has("shopReceiptConfig")) {
                    val obj = root.getJSONObject("shopReceiptConfig")
                    val rc = ShopReceiptConfig(
                        id = obj.optInt("id", 1),
                        shopLogo = obj.optString("shopLogo", "store_logo_default"),
                        customHeader = obj.optString("customHeader", "Welcome"),
                        customFooterText = obj.optString("customFooterText", "Thank you, visit again!"),
                        showCashierName = obj.optBoolean("showCashierName", true),
                        showBarcode = obj.optBoolean("showBarcode", true),
                        showCustomerMemo = obj.optBoolean("showCustomerMemo", true)
                    )
                    reportDao.saveReceiptConfig(rc)
                    count++
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
                    count += list.size
                }

                // 5. Cashiers (Match ONLY by trimmed case-insensitive name; never match by id)
                val cashierIdMap = mutableMapOf<Int, Int>()
                val cashiersArr = root.optJSONArray("cashiers")
                if (cashiersArr != null) {
                    val existingCashiers = reportDao.getAllCashiersList()
                    val existingById = existingCashiers.associateBy { it.id }
                    val localIdToName = existingCashiers.associate { it.id to it.name }
                    val backupIds = mutableListOf<Int>()
                    val list = mutableListOf<Cashier>()
                    for (i in 0 until cashiersArr.length()) {
                        val obj = cashiersArr.getJSONObject(i)
                        val backupId = obj.optInt("id", 0)
                        val name = obj.getString("name")
                        val matchedLocalId = matchLocalIdByName(name, localIdToName)
                        val existing = matchedLocalId?.let { existingById[it] }
                        val backupPin = if (obj.has("pin") && !obj.isNull("pin")) obj.getString("pin") else null
                        val pinVal = mergePinHash(existing?.pin, backupPin).orEmpty()
                        val backupIsActive: Boolean? = when {
                            obj.has("isActive") && !obj.isNull("isActive") -> obj.getBoolean("isActive")
                            obj.has("active") && !obj.isNull("active") -> obj.getBoolean("active")
                            else -> null
                        }
                        val resolvedIsActive = resolveRestoredIsActive(existing?.isActive, backupIsActive)
                        backupIds.add(backupId)
                        list.add(
                            Cashier(
                                id = matchedLocalId ?: 0,
                                name = name,
                                pin = pinVal,
                                role = obj.optString("role", existing?.role ?: "CASHIER"),
                                isActive = resolvedIsActive,
                                isArchived = obj.optBoolean("isArchived", existing?.isArchived ?: false),
                                archivedAt = if (obj.has("archivedAt") && !obj.isNull("archivedAt")) obj.optLong("archivedAt") else existing?.archivedAt,
                                createdByAdminId = obj.optString("createdByAdminId", existing?.createdByAdminId ?: "")
                            )
                        )
                    }
                    val insertedIds = reportDao.insertCashiers(list)
                    for (i in list.indices) {
                        val bId = backupIds[i]
                        val newId = insertedIds.getOrNull(i)?.toInt() ?: list[i].id
                        if (bId > 0 && newId > 0) {
                            cashierIdMap[bId] = newId
                        }
                    }
                    count += list.size
                }

                // 6. Users (Match ONLY by trimmed case-insensitive username; preserve local password/PIN if omitted)
                val userIdMap = mutableMapOf<Int, Int>()
                val usersArr = root.optJSONArray("users")
                if (usersArr != null) {
                    val existingUsers = reportDao.getAllUsersList()
                    val existingById = existingUsers.associateBy { it.id }
                    val localIdToUsername = existingUsers.associate { it.id to it.username }
                    val backupIds = mutableListOf<Int>()
                    val list = mutableListOf<User>()
                    for (i in 0 until usersArr.length()) {
                        val obj = usersArr.getJSONObject(i)
                        val backupId = obj.optInt("id", 0)
                        val username = obj.getString("username")
                        val matchedLocalId = matchLocalIdByName(username, localIdToUsername)
                        val existing = matchedLocalId?.let { existingById[it] }
                        val backupPasswordHash = if (obj.has("passwordHash") && !obj.isNull("passwordHash")) obj.getString("passwordHash") else null
                        val backupPin = if (obj.has("pin") && !obj.isNull("pin")) obj.getString("pin") else null
                        val passVal = mergePinHash(existing?.passwordHash, backupPasswordHash).orEmpty()
                        val pinVal = mergePinHash(existing?.pin, backupPin).orEmpty()
                        backupIds.add(backupId)
                        list.add(
                            User(
                                id = matchedLocalId ?: 0,
                                username = username,
                                passwordHash = passVal,
                                pin = pinVal,
                                role = obj.optString("role", existing?.role ?: "ADMIN"),
                                createdAt = obj.optLong("createdAt", existing?.createdAt ?: System.currentTimeMillis())
                            )
                        )
                    }
                    val insertedIds = reportDao.insertUsers(list)
                    for (i in list.indices) {
                        val bId = backupIds[i]
                        val newId = insertedIds.getOrNull(i)?.toInt() ?: list[i].id
                        if (bId > 0 && newId > 0) {
                            userIdMap[bId] = newId
                        }
                    }
                    count += list.size
                }

                // 7. Shift Reports
                val reportsArr = root.optJSONArray("shiftReports")
                if (reportsArr != null) {
                    val list = mutableListOf<ShiftReport>()
                    for (i in 0 until reportsArr.length()) {
                        val obj = reportsArr.getJSONObject(i)
                        val parsedReport = jsonToShiftReport(obj, isLegacyBackup)
                        val remappedCashierId = mapShiftReportCashierId(parsedReport.cashierId, cashierIdMap)
                        list.add(
                            if (remappedCashierId != parsedReport.cashierId) {
                                parsedReport.copy(cashierId = remappedCashierId)
                            } else {
                                parsedReport
                            }
                        )
                    }
                    reportDao.insertShiftReports(list)
                    count += list.size
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
                    count += list.size
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
                    count += list.size
                }

                // Audit Log
                reportDao.insertAuditLog(
                    AuditLog(
                        username = adminUser,
                        action = "RESTORE_DATA",
                        details = "Restored $count items from: $sourceDescription"
                    )
                )

                count
            }

            Result.success(restoredCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreBackup(
        context: Context,
        uri: Uri,
        database: AppDatabase,
        adminUser: String = "Admin",
        password: String? = null
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw IllegalArgumentException("Could not read backup file")
            val jsonString = decodeBackupToJson(bytes, password)
            restoreBackupFromString(jsonString, database, uri.lastPathSegment ?: "backup.json", adminUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun JSONObject.optBarcodeOrDefault(): Boolean {
        return if (has("showBarcode")) optBoolean("showBarcode", true) else true
    }
}
