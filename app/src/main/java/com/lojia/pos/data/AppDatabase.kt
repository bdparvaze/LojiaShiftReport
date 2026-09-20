package com.lojia.pos.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.lojia.pos.BuildConfig
import com.lojia.pos.util.SecurityUtils
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
        POSCategory::class,
        POSProduct::class,
        POSModifier::class,
        POSDiscount::class,
        POSSale::class,
        POSSaleItem::class,
        POSCustomer::class,
        POSEmployee::class,
        POSSupplier::class,
        POSStockAdjustment::class,
        TranslationCacheEntity::class,
        ShopReceiptConfig::class
    ],
    version = 6,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun reportDao(): ReportDao
    abstract fun posDao(): POSDao
    abstract fun translationDao(): TranslationDao

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
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pos_sales ADD COLUMN isVoided INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE pos_sales ADD COLUMN voidReason TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val builder = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lojia_system_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)

                if (BuildConfig.DEBUG) {
                    builder.fallbackToDestructiveMigration()
                }

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
                    populateInitialData(database.reportDao(), database.posDao())
                }
            }
        }

        suspend fun populateInitialData(reportDao: ReportDao, posDao: POSDao) {
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

                val catDrinks = posDao.insertCategory(POSCategory(name = "Beverages / المشروبات", iconName = "Coffee", colorHex = "#3B82F6"))
                val catFood = posDao.insertCategory(POSCategory(name = "Food & Bakery / المأكولات", iconName = "Restaurant", colorHex = "#F59E0B"))
                val catRetail = posDao.insertCategory(POSCategory(name = "Specialty & Retail / منتجات عامة", iconName = "Category", colorHex = "#10B981"))

                // Beverages
                posDao.insertProduct(POSProduct(name = "Espresso / اسبريسو", categoryId = catDrinks.toInt(), price = 3.50, costPrice = 0.80, stockQuantity = 300.0, minStockAlert = 20.0, barcode = "101", unit = "cup"))
                posDao.insertProduct(POSProduct(name = "Cappuccino / كابتشينو", categoryId = catDrinks.toInt(), price = 4.50, costPrice = 1.20, stockQuantity = 250.0, minStockAlert = 20.0, barcode = "102", unit = "cup"))
                posDao.insertProduct(POSProduct(name = "Iced Latte / لاتيه مثلج", categoryId = catDrinks.toInt(), price = 5.00, costPrice = 1.40, stockQuantity = 200.0, minStockAlert = 15.0, barcode = "103", unit = "cup"))
                posDao.insertProduct(POSProduct(name = "Turkish Tea / شاي تركي", categoryId = catDrinks.toInt(), price = 2.50, costPrice = 0.50, stockQuantity = 400.0, minStockAlert = 25.0, barcode = "104", unit = "cup"))
                posDao.insertProduct(POSProduct(name = "Mineral Water / مياه معدنية", categoryId = catDrinks.toInt(), price = 1.50, costPrice = 0.40, stockQuantity = 500.0, minStockAlert = 30.0, barcode = "105", unit = "bottle"))

                // Food
                posDao.insertProduct(POSProduct(name = "Club Sandwich / كلوب ساندويتش", categoryId = catFood.toInt(), price = 6.50, costPrice = 2.20, stockQuantity = 80.0, minStockAlert = 10.0, barcode = "201", unit = "pcs"))
                posDao.insertProduct(POSProduct(name = "Butter Croissant / كرواسون", categoryId = catFood.toInt(), price = 3.00, costPrice = 0.90, stockQuantity = 100.0, minStockAlert = 15.0, barcode = "202", unit = "pcs"))
                posDao.insertProduct(POSProduct(name = "Cheese Muffin / مافن جبن", categoryId = catFood.toInt(), price = 3.50, costPrice = 1.00, stockQuantity = 90.0, minStockAlert = 10.0, barcode = "203", unit = "pcs"))
                posDao.insertProduct(POSProduct(name = "Caesar Salad / سلطة سيزر", categoryId = catFood.toInt(), price = 7.00, costPrice = 2.50, stockQuantity = 60.0, minStockAlert = 10.0, barcode = "204", unit = "bowl"))

                // Retail
                posDao.insertProduct(POSProduct(name = "Coffee Beans 250g / بن قهوة مختصة", categoryId = catRetail.toInt(), price = 14.00, costPrice = 6.50, stockQuantity = 50.0, minStockAlert = 8.0, barcode = "301", unit = "pack"))
                posDao.insertProduct(POSProduct(name = "Premium Tea Tin 100g / علبة شاي فاخر", categoryId = catRetail.toInt(), price = 12.00, costPrice = 5.00, stockQuantity = 45.0, minStockAlert = 8.0, barcode = "302", unit = "can"))
                posDao.insertProduct(POSProduct(name = "Reusable Travel Mug / كوب حراري", categoryId = catRetail.toInt(), price = 15.00, costPrice = 6.00, stockQuantity = 35.0, minStockAlert = 5.0, barcode = "303", unit = "pcs"))

                posDao.insertModifier(POSModifier(name = "Extra Shot / جرعة إضافية", optionGroup = "Coffee Options", extraPrice = 1.0))
                posDao.insertModifier(POSModifier(name = "Oat Milk / حليب شوفان", optionGroup = "Milk Options", extraPrice = 0.75))
                posDao.insertModifier(POSModifier(name = "Vanilla Syrup / سيروب فانيلا", optionGroup = "Syrup", extraPrice = 0.50))

                posDao.insertDiscount(POSDiscount(name = "Staff 20% / خصم موظفين", percentage = 20.0, isPercentage = true, code = "STAFF20"))
                posDao.insertDiscount(POSDiscount(name = "Promo 10% / عرض ترويجي", percentage = 10.0, isPercentage = true, code = "PROMO10"))
                posDao.insertDiscount(POSDiscount(name = "Flat $5 / خصم مباشر", fixedAmount = 5.0, isPercentage = false, code = "FLAT5"))

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

                reportDao.insertAuditLog(AuditLog(username = "System", action = "INITIALIZATION", details = "Lojia database initialized with dual Shop & Shift Report modules"))
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
