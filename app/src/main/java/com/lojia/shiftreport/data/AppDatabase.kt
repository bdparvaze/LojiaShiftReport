package com.lojia.shiftreport.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.lojia.shiftreport.BuildConfig
import com.lojia.shiftreport.scanner.DocumentScannerDao
import com.lojia.shiftreport.scanner.ScannedDocument
import com.lojia.shiftreport.util.SecurityUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Database(
    entities = [
        ShiftReport::class,
        DraftReport::class,
        Cashier::class,
        User::class,
        UserProfile::class,
        AuditLog::class,
        BusinessProfile::class,
        AppSetting::class,
        ShiftSession::class,
        CashMovement::class,
        TranslationCacheEntity::class,
        ShopReceiptConfig::class,
        ScannedDocument::class,
        SavedAddress::class
    ],
    version = 15,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun reportDao(): ReportDao
    abstract fun translationDao(): TranslationDao
    abstract fun documentScannerDao(): DocumentScannerDao
    abstract fun savedAddressDao(): SavedAddressDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {}
        }
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {}
        }
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {}
        }
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {}
        }
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {}
        }
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {}
        }
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `scanned_documents` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `pdfUriPath` TEXT NOT NULL,
                        `pageCount` INTEGER NOT NULL,
                        `fileSizeBytes` INTEGER NOT NULL,
                        `createdAtMillis` INTEGER NOT NULL,
                        `notes` TEXT NOT NULL
                    )
                """.trimIndent())
            }
        }
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Table scanned_documents created in MIGRATION_7_8; no-op migration for 8->9
            }
        }
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `scanned_documents` ADD COLUMN `thumbnailPath` TEXT NOT NULL DEFAULT ''")
            }
        }
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `scanned_documents` ADD COLUMN `ocrText` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `scanned_documents` ADD COLUMN `docxUriPath` TEXT NOT NULL DEFAULT ''")
            }
        }
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `mapLat` REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `mapLng` REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE `business_profile` ADD COLUMN `mapLat` REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE `business_profile` ADD COLUMN `mapLng` REAL NOT NULL DEFAULT 0.0")
            }
        }
        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // shift_sessions upgrades
                db.execSQL("ALTER TABLE `shift_sessions` ADD COLUMN `totalDiscounts` REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE `shift_sessions` ADD COLUMN `salesReturns` REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE `shift_sessions` ADD COLUMN `isLocked` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `shift_sessions` ADD COLUMN `managerSignedBy` TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE `shift_sessions` ADD COLUMN `managerSignTime` INTEGER DEFAULT NULL")

                // shift_reports upgrades
                db.execSQL("ALTER TABLE `shift_reports` ADD COLUMN `openingCash` REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE `shift_reports` ADD COLUMN `closingCash` REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE `shift_reports` ADD COLUMN `totalDiscounts` REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE `shift_reports` ADD COLUMN `salesReturns` REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE `shift_reports` ADD COLUMN `isLocked` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `shift_reports` ADD COLUMN `managerSignedBy` TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE `shift_reports` ADD COLUMN `managerSignTime` INTEGER DEFAULT NULL")

                // draft_reports upgrades
                db.execSQL("ALTER TABLE `draft_reports` ADD COLUMN `openingCash` REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE `draft_reports` ADD COLUMN `closingCash` REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE `draft_reports` ADD COLUMN `totalDiscounts` REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE `draft_reports` ADD COLUMN `salesReturns` REAL NOT NULL DEFAULT 0.0")
            }
        }
        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `saved_addresses` (
                        `id` TEXT NOT NULL,
                        `firstName` TEXT NOT NULL DEFAULT '',
                        `lastName` TEXT NOT NULL DEFAULT '',
                        `phone` TEXT NOT NULL DEFAULT '',
                        `alternatePhone` TEXT NOT NULL DEFAULT '',
                        `countryCode` TEXT NOT NULL DEFAULT 'SA',
                        `dialCode` TEXT NOT NULL DEFAULT '+966',
                        `addressLine` TEXT NOT NULL DEFAULT '',
                        `arabicAddressLine` TEXT NOT NULL DEFAULT '',
                        `aptSuite` TEXT NOT NULL DEFAULT '',
                        `addressLabel` TEXT NOT NULL DEFAULT '',
                        `mapLat` REAL NOT NULL DEFAULT 0.0,
                        `mapLng` REAL NOT NULL DEFAULT 0.0,
                        `isDefault` INTEGER NOT NULL DEFAULT 0,
                        `createdAt` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Recreate shift_sessions
                db.execSQL("ALTER TABLE `shift_sessions` RENAME TO `shift_sessions_old`")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `shift_sessions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `cashierName` TEXT NOT NULL,
                        `shiftName` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `openedAt` INTEGER NOT NULL,
                        `closedAt` INTEGER,
                        `startingCash` INTEGER NOT NULL,
                        `cashSales` INTEGER NOT NULL,
                        `cardSales` INTEGER NOT NULL,
                        `digitalSales` INTEGER NOT NULL,
                        `totalDiscounts` INTEGER NOT NULL,
                        `salesReturns` INTEGER NOT NULL,
                        `totalPayIn` INTEGER NOT NULL,
                        `totalPayOut` INTEGER NOT NULL,
                        `expectedCash` INTEGER NOT NULL,
                        `actualCashCount` INTEGER NOT NULL,
                        `variance` INTEGER NOT NULL,
                        `notes` TEXT NOT NULL,
                        `isLocked` INTEGER NOT NULL,
                        `managerSignedBy` TEXT,
                        `managerSignTime` INTEGER
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO `shift_sessions` (
                        id, cashierName, shiftName, status, openedAt, closedAt,
                        startingCash, cashSales, cardSales, digitalSales, totalDiscounts, salesReturns,
                        totalPayIn, totalPayOut, expectedCash, actualCashCount, variance, notes, isLocked,
                        managerSignedBy, managerSignTime
                    ) SELECT 
                        id, cashierName, shiftName, status, openedAt, closedAt,
                        CAST(ROUND(startingCash * 100) AS INTEGER), CAST(ROUND(cashSales * 100) AS INTEGER),
                        CAST(ROUND(cardSales * 100) AS INTEGER), CAST(ROUND(digitalSales * 100) AS INTEGER),
                        CAST(ROUND(totalDiscounts * 100) AS INTEGER), CAST(ROUND(salesReturns * 100) AS INTEGER),
                        CAST(ROUND(totalPayIn * 100) AS INTEGER), CAST(ROUND(totalPayOut * 100) AS INTEGER),
                        CAST(ROUND(expectedCash * 100) AS INTEGER), CAST(ROUND(actualCashCount * 100) AS INTEGER),
                        CAST(ROUND(variance * 100) AS INTEGER), notes, isLocked, managerSignedBy, managerSignTime
                    FROM `shift_sessions_old`
                """.trimIndent())
                db.execSQL("DROP TABLE `shift_sessions_old`")

                // 2. Recreate cash_movements
                db.execSQL("ALTER TABLE `cash_movements` RENAME TO `cash_movements_old`")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `cash_movements` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `shiftSessionId` INTEGER NOT NULL,
                        `type` TEXT NOT NULL,
                        `amount` INTEGER NOT NULL,
                        `reason` TEXT NOT NULL,
                        `cashierName` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO `cash_movements` (id, shiftSessionId, type, amount, reason, cashierName, timestamp)
                    SELECT id, shiftSessionId, type, CAST(ROUND(amount * 100) AS INTEGER), reason, cashierName, timestamp
                    FROM `cash_movements_old`
                """.trimIndent())
                db.execSQL("DROP TABLE `cash_movements_old`")

                // 3. Recreate shift_reports
                db.execSQL("ALTER TABLE `shift_reports` RENAME TO `shift_reports_old`")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `shift_reports` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `cashierName` TEXT NOT NULL,
                        `shift` TEXT NOT NULL,
                        `dateInMillis` INTEGER NOT NULL,
                        `openingCash` INTEGER NOT NULL,
                        `closingCash` INTEGER NOT NULL,
                        `grossCash` INTEGER NOT NULL,
                        `madaPayments` INTEGER NOT NULL,
                        `digitalWallet` INTEGER NOT NULL,
                        `totalDiscounts` INTEGER NOT NULL,
                        `salesReturns` INTEGER NOT NULL,
                        `staffMealsCount` INTEGER NOT NULL,
                        `totalExpenses` INTEGER NOT NULL,
                        `muasselQty` REAL NOT NULL,
                        `outdoorShishaQty` REAL NOT NULL,
                        `dueCreditEntriesJson` TEXT NOT NULL,
                        `previousDueCollectionsJson` TEXT NOT NULL,
                        `staffAdvancesJson` TEXT NOT NULL,
                        `unpaidBillsJson` TEXT NOT NULL,
                        `purchasedItemsJson` TEXT NOT NULL,
                        `notes` TEXT NOT NULL,
                        `isLocked` INTEGER NOT NULL,
                        `managerSignedBy` TEXT,
                        `managerSignTime` INTEGER
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO `shift_reports` (
                        id, cashierName, shift, dateInMillis, openingCash, closingCash, grossCash, madaPayments,
                        digitalWallet, totalDiscounts, salesReturns, staffMealsCount, totalExpenses,
                        muasselQty, outdoorShishaQty, dueCreditEntriesJson, previousDueCollectionsJson,
                        staffAdvancesJson, unpaidBillsJson, purchasedItemsJson, notes, isLocked,
                        managerSignedBy, managerSignTime
                    ) SELECT 
                        id, cashierName, shift, dateInMillis,
                        CAST(ROUND(openingCash * 100) AS INTEGER), CAST(ROUND(closingCash * 100) AS INTEGER),
                        CAST(ROUND(grossCash * 100) AS INTEGER), CAST(ROUND(madaPayments * 100) AS INTEGER),
                        CAST(ROUND(digitalWallet * 100) AS INTEGER), CAST(ROUND(totalDiscounts * 100) AS INTEGER),
                        CAST(ROUND(salesReturns * 100) AS INTEGER), staffMealsCount,
                        CAST(ROUND(totalExpenses * 100) AS INTEGER), muasselQty, outdoorShishaQty,
                        dueCreditEntriesJson, previousDueCollectionsJson, staffAdvancesJson,
                        unpaidBillsJson, purchasedItemsJson, notes, isLocked, managerSignedBy, managerSignTime
                    FROM `shift_reports_old`
                """.trimIndent())
                db.execSQL("DROP TABLE `shift_reports_old`")

                // 4. Recreate draft_reports
                db.execSQL("ALTER TABLE `draft_reports` RENAME TO `draft_reports_old`")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `draft_reports` (
                        `id` INTEGER PRIMARY KEY NOT NULL,
                        `cashierName` TEXT NOT NULL,
                        `shift` TEXT NOT NULL,
                        `dateInMillis` INTEGER NOT NULL,
                        `openingCash` INTEGER NOT NULL,
                        `closingCash` INTEGER NOT NULL,
                        `grossCash` INTEGER NOT NULL,
                        `madaPayments` INTEGER NOT NULL,
                        `digitalWallet` INTEGER NOT NULL,
                        `totalDiscounts` INTEGER NOT NULL,
                        `salesReturns` INTEGER NOT NULL,
                        `staffMealsCount` INTEGER NOT NULL,
                        `totalExpenses` INTEGER NOT NULL,
                        `muasselQty` REAL NOT NULL,
                        `outdoorShishaQty` REAL NOT NULL,
                        `dueCreditEntriesJson` TEXT NOT NULL,
                        `previousDueCollectionsJson` TEXT NOT NULL,
                        `staffAdvancesJson` TEXT NOT NULL,
                        `unpaidBillsJson` TEXT NOT NULL,
                        `purchasedItemsJson` TEXT NOT NULL,
                        `notes` TEXT NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO `draft_reports` (
                        id, cashierName, shift, dateInMillis, openingCash, closingCash, grossCash, madaPayments,
                        digitalWallet, totalDiscounts, salesReturns, staffMealsCount, totalExpenses,
                        muasselQty, outdoorShishaQty, dueCreditEntriesJson, previousDueCollectionsJson,
                        staffAdvancesJson, unpaidBillsJson, purchasedItemsJson, notes
                    ) SELECT 
                        id, cashierName, shift, dateInMillis,
                        CAST(ROUND(openingCash * 100) AS INTEGER), CAST(ROUND(closingCash * 100) AS INTEGER),
                        CAST(ROUND(grossCash * 100) AS INTEGER), CAST(ROUND(madaPayments * 100) AS INTEGER),
                        CAST(ROUND(digitalWallet * 100) AS INTEGER), CAST(ROUND(totalDiscounts * 100) AS INTEGER),
                        CAST(ROUND(salesReturns * 100) AS INTEGER), staffMealsCount,
                        CAST(ROUND(totalExpenses * 100) AS INTEGER), muasselQty, outdoorShishaQty,
                        dueCreditEntriesJson, previousDueCollectionsJson, staffAdvancesJson,
                        unpaidBillsJson, purchasedItemsJson, notes
                    FROM `draft_reports_old`
                """.trimIndent())
                db.execSQL("DROP TABLE `draft_reports_old`")

                // Convert legacy JSON amounts from double to long minor units
                migrateJsonColumns(db)
            }
        }

        private fun migrateJsonColumns(db: SupportSQLiteDatabase) {
            val cursorReports = db.query("SELECT id, dueCreditEntriesJson, previousDueCollectionsJson, staffAdvancesJson, unpaidBillsJson, purchasedItemsJson FROM shift_reports")
            while (cursorReports.moveToNext()) {
                val id = cursorReports.getInt(0)
                val dueCredit = cursorReports.getString(1)
                val prevDue = cursorReports.getString(2)
                val staff = cursorReports.getString(3)
                val unpaid = cursorReports.getString(4)
                val purchased = cursorReports.getString(5)

                val updatedDueCredit = migrateJsonArraySingleAmount(dueCredit)
                val updatedPrevDue = migrateJsonArraySingleAmount(prevDue)
                val updatedStaff = migrateJsonArraySingleAmount(staff)
                val updatedUnpaid = migrateJsonArraySingleAmount(unpaid)
                val updatedPurchased = migrateJsonArrayPurchased(purchased)

                db.execSQL(
                    "UPDATE shift_reports SET dueCreditEntriesJson = ?, previousDueCollectionsJson = ?, staffAdvancesJson = ?, unpaidBillsJson = ?, purchasedItemsJson = ? WHERE id = ?",
                    arrayOf<Any>(updatedDueCredit, updatedPrevDue, updatedStaff, updatedUnpaid, updatedPurchased, id)
                )
            }
            cursorReports.close()

            val cursorDrafts = db.query("SELECT id, dueCreditEntriesJson, previousDueCollectionsJson, staffAdvancesJson, unpaidBillsJson, purchasedItemsJson FROM draft_reports")
            while (cursorDrafts.moveToNext()) {
                val id = cursorDrafts.getInt(0)
                val dueCredit = cursorDrafts.getString(1)
                val prevDue = cursorDrafts.getString(2)
                val staff = cursorDrafts.getString(3)
                val unpaid = cursorDrafts.getString(4)
                val purchased = cursorDrafts.getString(5)

                val updatedDueCredit = migrateJsonArraySingleAmount(dueCredit)
                val updatedPrevDue = migrateJsonArraySingleAmount(prevDue)
                val updatedStaff = migrateJsonArraySingleAmount(staff)
                val updatedUnpaid = migrateJsonArraySingleAmount(unpaid)
                val updatedPurchased = migrateJsonArrayPurchased(purchased)

                db.execSQL(
                    "UPDATE draft_reports SET dueCreditEntriesJson = ?, previousDueCollectionsJson = ?, staffAdvancesJson = ?, unpaidBillsJson = ?, purchasedItemsJson = ? WHERE id = ?",
                    arrayOf<Any>(updatedDueCredit, updatedPrevDue, updatedStaff, updatedUnpaid, updatedPurchased, id)
                )
            }
            cursorDrafts.close()
        }

        private fun migrateJsonArraySingleAmount(jsonStr: String?): String {
            if (jsonStr.isNullOrBlank()) return "[]"
            return try {
                val arr = org.json.JSONArray(jsonStr)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    if (obj.has("amount")) {
                        val amtDouble = obj.getDouble("amount")
                        val amtLong = Math.round(amtDouble * 100)
                        obj.put("amount", amtLong)
                    }
                }
                arr.toString()
            } catch (_: Exception) {
                jsonStr
            }
        }

        private fun migrateJsonArrayPurchased(jsonStr: String?): String {
            if (jsonStr.isNullOrBlank()) return "[]"
            return try {
                val arr = org.json.JSONArray(jsonStr)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    if (obj.has("unitPrice")) {
                        val priceDouble = obj.getDouble("unitPrice")
                        obj.put("unitPrice", Math.round(priceDouble * 100))
                    }
                    if (obj.has("totalAmount")) {
                        val totalDouble = obj.getDouble("totalAmount")
                        obj.put("totalAmount", Math.round(totalDouble * 100))
                    }
                }
                arr.toString()
            } catch (_: Exception) {
                jsonStr
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val builder = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lojia_system_database"
                )
                .addMigrations(
                    MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5,
                    MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9,
                    MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13,
                    MIGRATION_13_14, MIGRATION_14_15
                )
                .fallbackToDestructiveMigrationOnDowngrade()

                builder.addCallback(DatabaseCallback(scope) { INSTANCE })
                val instance = builder.build()
                INSTANCE = instance
                instance
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: getDatabase(context, CoroutineScope(Dispatchers.IO + SupervisorJob()))
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope,
        private val databaseProvider: () -> AppDatabase?
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            scope.launch(Dispatchers.IO) {
                val database = databaseProvider() ?: INSTANCE
                database?.let {
                    populateInitialData(it.reportDao())
                }
            }
        }

        suspend fun populateInitialData(reportDao: ReportDao) {
            if (BuildConfig.DEBUG) {
                reportDao.saveUserProfile(
                    UserProfile(
                        id = 1,
                        fullName = "Store Owner",
                        username = "",
                        email = "",
                        passwordHash = "",
                        securityQuestion = "",
                        securityAnswer = "",
                        phone = "",
                        designation = "Store Owner & Manager",
                        nationalIdOrPassport = "",
                        address = "",
                        profilePictureUri = "",
                        avatarIndex = 0,
                        dateOfBirthOrJoin = "",
                        emergencyContact = "",
                        pin = "",
                        isBiometricEnabled = false,
                        autoLockMinutes = 5,
                        currentRole = "ADMIN",
                        registeredAt = System.currentTimeMillis(),
                        isRegistered = false
                    )
                )

                reportDao.saveBusinessProfile(
                    BusinessProfile(
                        id = 1,
                        businessName = "Demo Store (Debug)",
                        vatNumber = "310000000000003",
                        phone = "+1 555 0100",
                        email = "store@example.com",
                        address = "123 Commercial Avenue",
                        workingHours = "08:00 AM - 10:00 PM",
                        currency = "USD",
                        country = "United States",
                        vatRate = 5.0,
                        isTaxEnabled = false,
                        isTaxIncluded = true,
                        logoUri = ""
                    )
                )

                reportDao.saveReceiptConfig(
                    ShopReceiptConfig(
                        id = 1,
                        shopLogo = "store_logo_default",
                        customHeader = "Demo Store",
                        customFooterText = "Thank you for shopping with us!",
                        showTaxNumber = true,
                        showCashierName = true,
                        showBarcode = true,
                        showCustomerMemo = true
                    )
                )

                val activeSessionId = reportDao.insertShiftSession(
                    ShiftSession(
                        cashierName = "Demo Staff",
                        shiftName = "Morning",
                        status = "OPEN",
                        openedAt = System.currentTimeMillis() - 14400000L,
                        startingCash = 100000L,
                        cashSales = 450000L,
                        cardSales = 680000L,
                        digitalSales = 210000L,
                        totalPayIn = 20000L,
                        totalPayOut = 15000L,
                        expectedCash = 555000L,
                        actualCashCount = 0L,
                        variance = 0L,
                        notes = "Morning shift running smoothly"
                    )
                )

                reportDao.insertCashMovement(
                    CashMovement(
                        shiftSessionId = activeSessionId.toInt(),
                        type = "PAY_IN",
                        amount = 20000L,
                        reason = "Added small change coins to drawer",
                        cashierName = "Demo Staff"
                    )
                )

                reportDao.insertCashMovement(
                    CashMovement(
                        shiftSessionId = activeSessionId.toInt(),
                        type = "PAY_OUT",
                        amount = 15000L,
                        reason = "Cleaning supplies cash purchase",
                        cashierName = "Demo Staff"
                    )
                )

                val now = System.currentTimeMillis()
                val oneDay = 86400000L
                reportDao.insertShiftReport(ShiftReport(cashierName = "Ahmed Al-Harbi", shift = "Morning", dateInMillis = now - (3 * oneDay), grossCash = 1450000L, madaPayments = 3200000L, digitalWallet = 850000L, staffMealsCount = 4, totalExpenses = 180000L, muasselQty = 0.0, notes = "Smooth morning shift"))
                reportDao.insertShiftReport(ShiftReport(cashierName = "Fahad Al-Otaibi", shift = "Evening", dateInMillis = now - (3 * oneDay), grossCash = 2100000L, madaPayments = 5100000L, digitalWallet = 1350000L, staffMealsCount = 6, totalExpenses = 320000L, muasselQty = 0.0, notes = "High footfall evening"))
                reportDao.insertShiftReport(ShiftReport(cashierName = "Sultan Al-Ghamdi", shift = "Morning", dateInMillis = now - (2 * oneDay), grossCash = 1680000L, madaPayments = 3450000L, digitalWallet = 920000L, staffMealsCount = 3, totalExpenses = 150000L, muasselQty = 0.0, notes = "Morning rush handled"))
                reportDao.insertShiftReport(ShiftReport(cashierName = "Demo Staff", shift = "Evening", dateInMillis = now - (1 * oneDay), grossCash = 2750000L, madaPayments = 6400000L, digitalWallet = 1850000L, staffMealsCount = 8, totalExpenses = 450000L, muasselQty = 0.0, notes = "Superb sales volume"))

                reportDao.insertAuditLog(AuditLog(username = "System", action = "INITIALIZATION", details = "LojiaShiftReport database initialized"))
            } else {
                reportDao.saveUserProfile(
                    UserProfile(
                        id = 1,
                        fullName = "",
                        username = "",
                        email = "",
                        passwordHash = "",
                        securityQuestion = "",
                        securityAnswer = "",
                        phone = "",
                        designation = "",
                        nationalIdOrPassport = "",
                        address = "",
                        profilePictureUri = "",
                        avatarIndex = 0,
                        dateOfBirthOrJoin = "",
                        emergencyContact = "",
                        pin = "",
                        isBiometricEnabled = false,
                        autoLockMinutes = 5,
                        currentRole = "ADMIN",
                        registeredAt = System.currentTimeMillis(),
                        isRegistered = false
                    )
                )
                reportDao.saveBusinessProfile(
                    BusinessProfile(
                        id = 1,
                        businessName = "My Store",
                        vatNumber = "",
                        phone = "",
                        email = "",
                        address = "",
                        currency = "USD",
                        country = "United States",
                        vatRate = 0.0,
                        isTaxEnabled = false,
                        isTaxIncluded = true
                    )
                )
                reportDao.saveReceiptConfig(
                    ShopReceiptConfig(
                        id = 1,
                        shopLogo = "store_logo_default",
                        customHeader = "Welcome",
                        customFooterText = "Thank you, visit again!",
                        showTaxNumber = true,
                        showCashierName = true,
                        showBarcode = true,
                        showCustomerMemo = true
                    )
                )
            }
        }
    }
}
