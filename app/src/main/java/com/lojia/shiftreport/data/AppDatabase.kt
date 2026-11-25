package com.lojia.shiftreport.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.lojia.shiftreport.scanner.DocumentScannerDao
import com.lojia.shiftreport.scanner.ScannedDocument
import com.lojia.shiftreport.util.AppConstants
import com.lojia.shiftreport.util.MoneyFormat
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
        ShopReceiptConfig::class,
        ScannedDocument::class,
        SavedAddress::class,
        Employee::class,
        EmployerAdvanceTransaction::class
    ],
    version = 20,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun reportDao(): ReportDao
    abstract fun documentScannerDao(): DocumentScannerDao
    abstract fun savedAddressDao(): SavedAddressDao
    abstract fun employeeDao(): EmployeeDao
    abstract fun employerAdvanceDao(): EmployerAdvanceDao

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
                        `countryCode` TEXT NOT NULL DEFAULT '',
                        `dialCode` TEXT NOT NULL DEFAULT '',
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
                val currencyCode = try {
                    db.query("SELECT currency FROM business_profile LIMIT 1").use { cursor ->
                        if (cursor.moveToFirst()) cursor.getString(0) else null
                    }
                } catch (_: Exception) {
                    null
                }
                val digits = if (currencyCode.isNullOrBlank()) {
                    AppConstants.ISO_DEFAULT_MINOR_DIGITS
                } else {
                    MoneyFormat.minorUnitDigits(currencyCode)
                }
                val factor = MoneyFormat.minorFactor(digits)

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
                        CAST(ROUND(startingCash * ?) AS INTEGER), CAST(ROUND(cashSales * ?) AS INTEGER),
                        CAST(ROUND(cardSales * ?) AS INTEGER), CAST(ROUND(digitalSales * ?) AS INTEGER),
                        CAST(ROUND(totalDiscounts * ?) AS INTEGER), CAST(ROUND(salesReturns * ?) AS INTEGER),
                        CAST(ROUND(totalPayIn * ?) AS INTEGER), CAST(ROUND(totalPayOut * ?) AS INTEGER),
                        CAST(ROUND(expectedCash * ?) AS INTEGER), CAST(ROUND(actualCashCount * ?) AS INTEGER),
                        CAST(ROUND(variance * ?) AS INTEGER), notes, isLocked, managerSignedBy, managerSignTime
                    FROM `shift_sessions_old`
                """.trimIndent(), Array<Any>(11) { factor })
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
                    SELECT id, shiftSessionId, type, CAST(ROUND(amount * ?) AS INTEGER), reason, cashierName, timestamp
                    FROM `cash_movements_old`
                """.trimIndent(), arrayOf<Any>(factor))
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
                        CAST(ROUND(openingCash * ?) AS INTEGER), CAST(ROUND(closingCash * ?) AS INTEGER),
                        CAST(ROUND(grossCash * ?) AS INTEGER), CAST(ROUND(madaPayments * ?) AS INTEGER),
                        CAST(ROUND(digitalWallet * ?) AS INTEGER), CAST(ROUND(totalDiscounts * ?) AS INTEGER),
                        CAST(ROUND(salesReturns * ?) AS INTEGER), staffMealsCount,
                        CAST(ROUND(totalExpenses * ?) AS INTEGER), muasselQty, outdoorShishaQty,
                        dueCreditEntriesJson, previousDueCollectionsJson, staffAdvancesJson,
                        unpaidBillsJson, purchasedItemsJson, notes, isLocked, managerSignedBy, managerSignTime
                    FROM `shift_reports_old`
                """.trimIndent(), Array<Any>(8) { factor })
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
                        CAST(ROUND(openingCash * ?) AS INTEGER), CAST(ROUND(closingCash * ?) AS INTEGER),
                        CAST(ROUND(grossCash * ?) AS INTEGER), CAST(ROUND(madaPayments * ?) AS INTEGER),
                        CAST(ROUND(digitalWallet * ?) AS INTEGER), CAST(ROUND(totalDiscounts * ?) AS INTEGER),
                        CAST(ROUND(salesReturns * ?) AS INTEGER), staffMealsCount,
                        CAST(ROUND(totalExpenses * ?) AS INTEGER), muasselQty, outdoorShishaQty,
                        dueCreditEntriesJson, previousDueCollectionsJson, staffAdvancesJson,
                        unpaidBillsJson, purchasedItemsJson, notes
                    FROM `draft_reports_old`
                """.trimIndent(), Array<Any>(8) { factor })
                db.execSQL("DROP TABLE `draft_reports_old`")

                // Convert legacy JSON amounts from double to long minor units
                migrateJsonColumns(db, digits)
            }
        }

        private fun migrateJsonColumns(db: SupportSQLiteDatabase, digits: Int) {
            val cursorReports = db.query("SELECT id, dueCreditEntriesJson, previousDueCollectionsJson, staffAdvancesJson, unpaidBillsJson, purchasedItemsJson FROM shift_reports")
            while (cursorReports.moveToNext()) {
                val id = cursorReports.getInt(0)
                val dueCredit = cursorReports.getString(1)
                val prevDue = cursorReports.getString(2)
                val staff = cursorReports.getString(3)
                val unpaid = cursorReports.getString(4)
                val purchased = cursorReports.getString(5)

                val updatedDueCredit = migrateJsonArraySingleAmount(dueCredit, digits)
                val updatedPrevDue = migrateJsonArraySingleAmount(prevDue, digits)
                val updatedStaff = migrateJsonArraySingleAmount(staff, digits)
                val updatedUnpaid = migrateJsonArraySingleAmount(unpaid, digits)
                val updatedPurchased = migrateJsonArrayPurchased(purchased, digits)

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

                val updatedDueCredit = migrateJsonArraySingleAmount(dueCredit, digits)
                val updatedPrevDue = migrateJsonArraySingleAmount(prevDue, digits)
                val updatedStaff = migrateJsonArraySingleAmount(staff, digits)
                val updatedUnpaid = migrateJsonArraySingleAmount(unpaid, digits)
                val updatedPurchased = migrateJsonArrayPurchased(purchased, digits)

                db.execSQL(
                    "UPDATE draft_reports SET dueCreditEntriesJson = ?, previousDueCollectionsJson = ?, staffAdvancesJson = ?, unpaidBillsJson = ?, purchasedItemsJson = ? WHERE id = ?",
                    arrayOf<Any>(updatedDueCredit, updatedPrevDue, updatedStaff, updatedUnpaid, updatedPurchased, id)
                )
            }
            cursorDrafts.close()
        }

        internal fun migrateJsonArraySingleAmount(jsonStr: String?, digits: Int): String {
            if (jsonStr.isNullOrBlank()) return "[]"
            return try {
                val arr = org.json.JSONArray(jsonStr)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    if (obj.has("amount")) {
                        val amtDouble = obj.getDouble("amount")
                        val amtLong = MoneyFormat.majorDoubleToMinor(amtDouble, digits)
                        obj.put("amount", amtLong)
                    }
                }
                arr.toString()
            } catch (_: Exception) {
                jsonStr
            }
        }

        internal fun migrateJsonArrayPurchased(jsonStr: String?, digits: Int): String {
            if (jsonStr.isNullOrBlank()) return "[]"
            return try {
                val arr = org.json.JSONArray(jsonStr)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    if (obj.has("unitPrice")) {
                        val priceDouble = obj.getDouble("unitPrice")
                        obj.put("unitPrice", MoneyFormat.majorDoubleToMinor(priceDouble, digits))
                    }
                    if (obj.has("totalAmount")) {
                        val totalDouble = obj.getDouble("totalAmount")
                        obj.put("totalAmount", MoneyFormat.majorDoubleToMinor(totalDouble, digits))
                    }
                }
                arr.toString()
            } catch (_: Exception) {
                jsonStr
            }
        }

        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `translation_cache` ")
            }
        }

        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `cashiers` ADD COLUMN `isActive` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("UPDATE `cashiers` SET `isActive` = `active`")
                db.execSQL("ALTER TABLE `cashiers` ADD COLUMN `isArchived` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `cashiers` ADD COLUMN `archivedAt` INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE `cashiers` ADD COLUMN `createdByAdminId` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `shift_reports` ADD COLUMN `cashierId` TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Heuristic removed in Stage 20C. No data change.
            }
        }

        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `employees` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `monthlySalary` REAL NOT NULL,
                        `phone` TEXT,
                        `isActive` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `createdByAdminId` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `advance_transactions` (
                        `id` TEXT NOT NULL,
                        `employeeId` TEXT NOT NULL,
                        `employeeName` TEXT NOT NULL,
                        `shiftId` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `paymentMethod` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `reason` TEXT NOT NULL,
                        `originalTransactionId` TEXT,
                        `isVoid` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `createdByAdminId` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val currencyCode = try {
                    db.query("SELECT currency FROM business_profile LIMIT 1").use { cursor ->
                        if (cursor.moveToFirst()) cursor.getString(0) else null
                    }
                } catch (_: Exception) {
                    null
                }
                val digits = if (currencyCode.isNullOrBlank()) {
                    AppConstants.ISO_DEFAULT_MINOR_DIGITS
                } else {
                    com.lojia.shiftreport.util.MoneyFormat.minorUnitDigits(currencyCode)
                }

                // 1. Rebuild employees table converting monthlySalary (REAL) -> monthlySalaryMinor (INTEGER)
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `employees_new` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `monthlySalaryMinor` INTEGER NOT NULL,
                        `phone` TEXT,
                        `isActive` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `createdByAdminId` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.query("SELECT id, name, monthlySalary, phone, isActive, createdAt, createdByAdminId FROM employees").use { cursor ->
                    while (cursor.moveToNext()) {
                        val id = cursor.getString(0)
                        val name = cursor.getString(1)
                        val salaryMinor = com.lojia.shiftreport.util.MoneyFormat.majorDoubleToMinor(cursor.getDouble(2), digits)
                        val phone: String? = if (cursor.isNull(3)) null else cursor.getString(3)
                        val isActive = cursor.getInt(4)
                        val createdAt = cursor.getLong(5)
                        val createdByAdminId = cursor.getString(6)
                        db.execSQL(
                            "INSERT INTO `employees_new` (`id`, `name`, `monthlySalaryMinor`, `phone`, `isActive`, `createdAt`, `createdByAdminId`) VALUES (?, ?, ?, ?, ?, ?, ?)",
                            arrayOf<Any?>(id, name, salaryMinor, phone, isActive, createdAt, createdByAdminId)
                        )
                    }
                }
                db.execSQL("DROP TABLE `employees`")
                db.execSQL("ALTER TABLE `employees_new` RENAME TO `employees`")

                // 2. Rebuild advance_transactions table converting amount (REAL) -> amountMinor (INTEGER)
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `advance_transactions_new` (
                        `id` TEXT NOT NULL,
                        `employeeId` TEXT NOT NULL,
                        `employeeName` TEXT NOT NULL,
                        `shiftId` TEXT NOT NULL,
                        `amountMinor` INTEGER NOT NULL,
                        `paymentMethod` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `reason` TEXT NOT NULL,
                        `originalTransactionId` TEXT,
                        `isVoid` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `createdByAdminId` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.query("SELECT id, employeeId, employeeName, shiftId, amount, paymentMethod, type, reason, originalTransactionId, isVoid, createdAt, createdByAdminId FROM advance_transactions").use { cursor ->
                    while (cursor.moveToNext()) {
                        val id = cursor.getString(0)
                        val employeeId = cursor.getString(1)
                        val employeeName = cursor.getString(2)
                        val shiftId = cursor.getString(3)
                        val amountMinor = com.lojia.shiftreport.util.MoneyFormat.majorDoubleToMinor(cursor.getDouble(4), digits)
                        val paymentMethod = cursor.getString(5)
                        val type = cursor.getString(6)
                        val reason = cursor.getString(7)
                        val originalTransactionId: String? = if (cursor.isNull(8)) null else cursor.getString(8)
                        val isVoid = cursor.getInt(9)
                        val createdAt = cursor.getLong(10)
                        val createdByAdminId = cursor.getString(11)
                        db.execSQL(
                            "INSERT INTO `advance_transactions_new` (`id`, `employeeId`, `employeeName`, `shiftId`, `amountMinor`, `paymentMethod`, `type`, `reason`, `originalTransactionId`, `isVoid`, `createdAt`, `createdByAdminId`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                            arrayOf<Any?>(id, employeeId, employeeName, shiftId, amountMinor, paymentMethod, type, reason, originalTransactionId, isVoid, createdAt, createdByAdminId)
                        )
                    }
                }
                db.execSQL("DROP TABLE `advance_transactions`")
                db.execSQL("ALTER TABLE `advance_transactions_new` RENAME TO `advance_transactions`")
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val builder = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lojia_system_database"
                )
                // SAFE ZERO-DATA-LOSS MIGRATIONS:
                // fallbackToDestructiveMigration() is strictly omitted to prevent user data loss across updates.
                .addMigrations(
                    MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5,
                    MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9,
                    MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13,
                    MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17,
                    MIGRATION_17_18,
                    MIGRATION_18_19,
                    MIGRATION_19_20
                )

                builder.addCallback(DatabaseCallback(scope) { INSTANCE })
                val instance = builder.build()
                INSTANCE = instance

                com.lojia.shiftreport.sync.AutoBackupScheduler.schedulePeriodicAutoBackup(
                    context.applicationContext
                )

                instance
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return try {
                INSTANCE ?: getDatabase(context, CoroutineScope(Dispatchers.IO + SupervisorJob()))
            } catch (e: Exception) {
                com.lojia.shiftreport.util.AppLogger.e("AppDatabase", "Database initialization or downgrade failure (data preserved): ${e.message}", e)
                throw e
            }
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

        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            try {
                db.execSQL("DELETE FROM employees WHERE monthlySalaryMinor <= 0")
            } catch (e: Exception) {
                com.lojia.shiftreport.util.AppLogger.e(
                    "AppDatabase",
                    "Failed to delete zero-salary employee records on open: ${e.message}",
                    e
                )
            }
            if (com.lojia.shiftreport.BuildConfig.DEBUG) {
                scope.launch(Dispatchers.IO) {
                    val database = databaseProvider() ?: INSTANCE
                    database?.let {
                        val dao = it.reportDao()
                        val existing = dao.getUserProfileOnce()
                        if (existing == null || !existing.isRegistered || existing.username.isBlank() || existing.username == "123456") {
                            dao.saveUserProfile(
                                (existing ?: UserProfile(id = 1, fullName = "Store Owner")).copy(
                                    username = "admin",
                                    passwordHash = com.lojia.shiftreport.util.SecurityUtils.hashSecret("123456"),
                                    pin = com.lojia.shiftreport.util.SecurityUtils.hashSecret("123456"),
                                    currentRole = "ADMIN",
                                    isRegistered = true
                                )
                            )
                        }
                    }
                }
            }
        }

        suspend fun populateInitialData(reportDao: ReportDao) {
            val databaseIsEmpty = reportDao.getAllShiftReportsList().isEmpty() &&
                reportDao.getAllShiftSessionsList().isEmpty() &&
                reportDao.getBusinessProfileOnce() == null
            if (databaseIsEmpty) {
                if (com.lojia.shiftreport.BuildConfig.DEBUG) {
                    reportDao.saveUserProfile(
                        UserProfile(
                            id = 1,
                            fullName = "",
                            username = "admin",
                            email = "",
                            passwordHash = com.lojia.shiftreport.util.SecurityUtils.hashSecret("123456"),
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
                            pin = com.lojia.shiftreport.util.SecurityUtils.hashSecret("123456"),
                            isBiometricEnabled = false,
                            autoLockMinutes = 5,
                            currentRole = "ADMIN",
                            registeredAt = System.currentTimeMillis(),
                            isRegistered = true
                        )
                    )
                }
                reportDao.saveBusinessProfile(
                    BusinessProfile(
                        id = 1,
                        businessName = "",
                        vatNumber = "",
                        phone = "",
                        email = "",
                        address = "",
                        currency = AppConstants.DEFAULT_CURRENCY_CODE,
                        country = defaultCountryDisplayName()
                    )
                )
                reportDao.saveReceiptConfig(
                    ShopReceiptConfig(
                        id = 1,
                        shopLogo = "store_logo_default",
                        customHeader = "Welcome",
                        customFooterText = "Thank you, visit again!",
                        showCashierName = true,
                        showBarcode = true,
                        showCustomerMemo = true
                    )
                )
            }
        }
    }
}
