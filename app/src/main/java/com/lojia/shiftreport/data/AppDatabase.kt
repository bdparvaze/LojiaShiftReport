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
        ScannedDocument::class
    ],
    version = 12,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun reportDao(): ReportDao
    abstract fun translationDao(): TranslationDao
    abstract fun documentScannerDao(): DocumentScannerDao

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
                    MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12
                )
                .fallbackToDestructiveMigration()

                builder.addCallback(DatabaseCallback(scope))
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
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(database.reportDao())
                }
            }
        }

        suspend fun populateInitialData(reportDao: ReportDao) {
            if (BuildConfig.DEBUG) {
                reportDao.saveUserProfile(
                    UserProfile(
                        id = 1,
                        fullName = "Demo Owner",
                        username = "demo",
                        email = "demo@example.com",
                        passwordHash = SecurityUtils.hashSecret("demo123"),
                        securityQuestion = "What is your primary store location?",
                        securityAnswer = "demo",
                        phone = "+1 555 0199",
                        designation = "Store Owner & Manager",
                        nationalIdOrPassport = "",
                        address = "123 Commercial Avenue",
                        profilePictureUri = "",
                        avatarIndex = 0,
                        dateOfBirthOrJoin = "01 Jan 2024",
                        emergencyContact = "",
                        pin = "",
                        isBiometricEnabled = false,
                        autoLockMinutes = 5,
                        currentRole = "ADMIN",
                        registeredAt = System.currentTimeMillis(),
                        isRegistered = true
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

                reportDao.insertUser(
                    User(
                        username = "demo",
                        passwordHash = SecurityUtils.hashSecret("demo123"),
                        role = "ADMIN",
                        pin = ""
                    )
                )

                reportDao.insertCashier(Cashier(name = "Manager (Demo)", pin = SecurityUtils.hashSecret("123456"), role = "ADMIN"))
                reportDao.insertCashier(Cashier(name = "Cashier 1 (Demo)", pin = SecurityUtils.hashSecret("1111"), role = "CASHIER"))
                reportDao.insertCashier(Cashier(name = "Cashier 2 (Demo)", pin = SecurityUtils.hashSecret("2222"), role = "CASHIER"))

                val activeSessionId = reportDao.insertShiftSession(
                    ShiftSession(
                        cashierName = "Demo Staff",
                        shiftName = "Morning",
                        status = "OPEN",
                        openedAt = System.currentTimeMillis() - 14400000L,
                        startingCash = 1000.0,
                        cashSales = 4500.0,
                        cardSales = 6800.0,
                        digitalSales = 2100.0,
                        totalPayIn = 200.0,
                        totalPayOut = 150.0,
                        expectedCash = 5550.0,
                        actualCashCount = 0.0,
                        variance = 0.0,
                        notes = "Morning shift running smoothly"
                    )
                )

                reportDao.insertCashMovement(
                    CashMovement(
                        shiftSessionId = activeSessionId.toInt(),
                        type = "PAY_IN",
                        amount = 200.0,
                        reason = "Added small change coins to drawer",
                        cashierName = "Demo Staff"
                    )
                )

                reportDao.insertCashMovement(
                    CashMovement(
                        shiftSessionId = activeSessionId.toInt(),
                        type = "PAY_OUT",
                        amount = 150.0,
                        reason = "Cleaning supplies cash purchase",
                        cashierName = "Demo Staff"
                    )
                )

                val now = System.currentTimeMillis()
                val oneDay = 86400000L
                reportDao.insertShiftReport(ShiftReport(cashierName = "Ahmed Al-Harbi", shift = "Morning", dateInMillis = now - (3 * oneDay), grossCash = 14500.0, madaPayments = 32000.0, digitalWallet = 8500.0, staffMealsCount = 4, totalExpenses = 1800.0, muasselQty = 0.0, notes = "Smooth morning shift"))
                reportDao.insertShiftReport(ShiftReport(cashierName = "Fahad Al-Otaibi", shift = "Evening", dateInMillis = now - (3 * oneDay), grossCash = 21000.0, madaPayments = 51000.0, digitalWallet = 13500.0, staffMealsCount = 6, totalExpenses = 3200.0, muasselQty = 0.0, notes = "High footfall evening"))
                reportDao.insertShiftReport(ShiftReport(cashierName = "Sultan Al-Ghamdi", shift = "Morning", dateInMillis = now - (2 * oneDay), grossCash = 16800.0, madaPayments = 34500.0, digitalWallet = 9200.0, staffMealsCount = 3, totalExpenses = 1500.0, muasselQty = 0.0, notes = "Morning rush handled"))
                reportDao.insertShiftReport(ShiftReport(cashierName = "Demo Staff", shift = "Evening", dateInMillis = now - (1 * oneDay), grossCash = 27500.0, madaPayments = 64000.0, digitalWallet = 18500.0, staffMealsCount = 8, totalExpenses = 4500.0, muasselQty = 0.0, notes = "Superb sales volume"))

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
