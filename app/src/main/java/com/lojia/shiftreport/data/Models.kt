package com.lojia.shiftreport.data

import androidx.annotation.StringRes
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.lojia.shiftreport.R
import com.lojia.shiftreport.util.MoneyFormat
import com.lojia.shiftreport.util.SecurityUtils

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val username: String,
    val passwordHash: String = "",
    val role: String = "ADMIN", // ADMIN, CASHIER
    val pin: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val fullName: String = "Store Owner",
    val username: String = "",
    val email: String = "",
    @ColumnInfo(name = "passwordHash")
    val passwordHash: String = "",
    val securityQuestion: String = "",
    val securityAnswer: String = "",
    val phone: String = "",
    val designation: String = "Store Owner & Manager",
    val nationalIdOrPassport: String = "",
    val address: String = "",
    val mapLat: Double = 0.0,
    val mapLng: Double = 0.0,
    val profilePictureUri: String = "",
    val avatarIndex: Int = 0,
    val dateOfBirthOrJoin: String = "",
    val emergencyContact: String = "",
    val pin: String = "",
    val isBiometricEnabled: Boolean = false,
    val autoLockMinutes: Int = 5, // 0 = Never, 1, 5, 15, 30
    val currentRole: String = "ADMIN", // ADMIN, CASHIER
    val registeredAt: Long = System.currentTimeMillis(),
    val isRegistered: Boolean = false
)

@Entity(tableName = "shop_receipt_config")
data class ShopReceiptConfig(
    @PrimaryKey val id: Int = 1,
    val shopLogo: String = "store_logo_default",
    val customHeader: String = "Welcome",
    val customFooterText: String = "Thank you, visit again!",
    val showTaxNumber: Boolean = true,
    val showCashierName: Boolean = true,
    val showBarcode: Boolean = true,
    val showCustomerMemo: Boolean = true
)

@Entity(tableName = "shift_sessions")
data class ShiftSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val cashierName: String = "Staff",
    val shiftName: String = "Morning", // Morning, Evening, Night
    val status: String = "OPEN", // OPEN, CLOSED, LOCKED
    val openedAt: Long = System.currentTimeMillis(),
    val closedAt: Long? = null,
    val startingCash: Long = 50000L,
    val cashSales: Long = 0L,
    val cardSales: Long = 0L,
    val digitalSales: Long = 0L,
    val totalDiscounts: Long = 0L,
    val salesReturns: Long = 0L,
    val totalPayIn: Long = 0L,
    val totalPayOut: Long = 0L,
    val expectedCash: Long = 50000L,
    val actualCashCount: Long = 0L,
    val variance: Long = 0L,
    val notes: String = "",
    val isLocked: Boolean = false,
    val managerSignedBy: String? = null,
    val managerSignTime: Long? = null
)

@Entity(tableName = "cash_movements")
data class CashMovement(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val shiftSessionId: Int = 0,
    val type: String, // PAY_IN, PAY_OUT
    val amount: Long,
    val reason: String,
    val cashierName: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "cashiers")
data class Cashier(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val pin: String = "",
    val role: String = "CASHIER", // ADMIN, CASHIER
    val active: Boolean = true
)

@Entity(tableName = "shift_reports")
data class ShiftReport(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val cashierName: String,
    val shift: String = "Day", // "Day", "Night", "Morning", "Evening"
    val dateInMillis: Long = System.currentTimeMillis(),
    val openingCash: Long = 0L,
    val closingCash: Long = 0L,
    val grossCash: Long = 0L,
    val madaPayments: Long = 0L,
    val digitalWallet: Long = 0L,
    val totalDiscounts: Long = 0L,
    val salesReturns: Long = 0L,
    val staffMealsCount: Int = 0,
    val totalExpenses: Long = 0L,
    val muasselQty: Double = 0.0,
    val outdoorShishaQty: Double = 0.0,
    val dueCreditEntriesJson: String = "[]",
    val previousDueCollectionsJson: String = "[]",
    val staffAdvancesJson: String = "[]",
    val unpaidBillsJson: String = "[]",
    val purchasedItemsJson: String = "[]",
    val notes: String = "",
    val isLocked: Boolean = false,
    val managerSignedBy: String? = null,
    val managerSignTime: Long? = null
) {
    // 1. Gross Sales (by payment method only - cash + card/mada + digital wallet)
    val grossSales: Long
        get() = MoneyFormat.addMinor(grossCash, madaPayments, digitalWallet)

    // Total Sales (Gross Sales)
    val totalSales: Long
        get() = grossSales

    // Net Sales (Gross Sales minus Discounts and Returns)
    val netSales: Long
        get() = MoneyFormat.subtractMinor(MoneyFormat.subtractMinor(grossSales, totalDiscounts), salesReturns).coerceAtLeast(0L)

    // Parsed JSON Collection Lists
    val previousDueCollectionsList: List<PreviousDueCollectionItem>
        get() = try {
            val arr = org.json.JSONArray(previousDueCollectionsJson)
            val list = mutableListOf<PreviousDueCollectionItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    PreviousDueCollectionItem(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        customerName = obj.optString("customerName", obj.optString("receiptNo", "")),
                        amount = obj.optLong("amount", 0L),
                        paymentMode = obj.optString("paymentMode", obj.optString("type", "CASH")),
                        note = obj.optString("note", "")
                    )
                )
            }
            list
        } catch (e: Exception) { emptyList() }

    val dueCreditEntriesList: List<DueCreditItem>
        get() = try {
            val arr = org.json.JSONArray(dueCreditEntriesJson)
            val list = mutableListOf<DueCreditItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    DueCreditItem(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        customerName = obj.optString("customerName", obj.optString("receiptNo", "")),
                        amount = obj.optLong("amount", 0L),
                        note = obj.optString("note", ""),
                        phone = obj.optString("phone", "")
                    )
                )
            }
            list
        } catch (e: Exception) { emptyList() }

    val staffAdvancesList: List<StaffAdvanceItem>
        get() = try {
            val arr = org.json.JSONArray(staffAdvancesJson)
            val list = mutableListOf<StaffAdvanceItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    StaffAdvanceItem(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        staffName = obj.optString("staffName", obj.optString("name", "")),
                        amount = obj.optLong("amount", 0L),
                        reason = obj.optString("reason", obj.optString("type", "CASH"))
                    )
                )
            }
            list
        } catch (e: Exception) { emptyList() }

    val purchasedItemsList: List<PurchasedInventoryItem>
        get() = try {
            val arr = org.json.JSONArray(purchasedItemsJson)
            val list = mutableListOf<PurchasedInventoryItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val qty = obj.optDouble("quantity", obj.optDouble("qty", 0.0))
                val unitPrice = obj.optLong("unitPrice", obj.optLong("price", 0L))
                val total = obj.optLong("totalAmount", obj.optLong("total", 0L))
                list.add(
                    PurchasedInventoryItem(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        itemName = obj.optString("itemName", obj.optString("name", "")),
                        quantity = qty,
                        unitPrice = unitPrice,
                        totalAmount = total,
                        paidVia = obj.optString("paidVia", "CASH"),
                        supplier = obj.optString("supplier", "")
                    )
                )
            }
            list
        } catch (e: Exception) { emptyList() }

    val unpaidBillsList: List<UnpaidBillItem>
        get() = try {
            val arr = org.json.JSONArray(unpaidBillsJson)
            val list = mutableListOf<UnpaidBillItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    UnpaidBillItem(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        tableOrOrderRef = obj.optString("tableOrOrderRef", obj.optString("description", "")),
                        amount = obj.optLong("amount", 0L),
                        reason = obj.optString("reason", "")
                    )
                )
            }
            list
        } catch (e: Exception) { emptyList() }

    // Due / Receivables Tracking
    val totalDueCredit: Long
        get() = MoneyFormat.sumOfMinor(dueCreditEntriesList.map { it.amount })

    val totalDueIssued: Long
        get() = totalDueCredit

    val totalPreviousDueCash: Long
        get() = MoneyFormat.sumOfMinor(previousDueCollectionsList.filter { it.paymentMode.equals("CASH", ignoreCase = true) }.map { it.amount })

    val totalDueCollectedCash: Long
        get() = totalPreviousDueCash

    val totalPreviousDueBank: Long
        get() = MoneyFormat.sumOfMinor(previousDueCollectionsList.filter { !it.paymentMode.equals("CASH", ignoreCase = true) }.map { it.amount })

    val totalDueCollectedBank: Long
        get() = totalPreviousDueBank

    val totalDueCollected: Long
        get() = MoneyFormat.sumOfMinor(previousDueCollectionsList.map { it.amount })

    // Staff Advances
    val totalStaffAdvances: Long
        get() = MoneyFormat.sumOfMinor(staffAdvancesList.map { it.amount })

    val totalStaffAdvancesAmount: Long
        get() = totalStaffAdvances

    // Purchases
    val totalCashPurchases: Long
        get() = MoneyFormat.sumOfMinor(purchasedItemsList.filter { it.paidVia.equals("CASH", ignoreCase = true) }.map { it.totalAmount })

    val totalPurchasedCash: Long
        get() = totalCashPurchases

    val totalPurchasedBank: Long
        get() = MoneyFormat.sumOfMinor(purchasedItemsList.filter { !it.paidVia.equals("CASH", ignoreCase = true) }.map { it.totalAmount })

    val totalPurchasedAll: Long
        get() = MoneyFormat.sumOfMinor(purchasedItemsList.map { it.totalAmount })

    val totalUnpaidLoss: Long
        get() = MoneyFormat.sumOfMinor(unpaidBillsList.map { it.amount })

    // 2. Cash Inflow & Outflow Formulas
    val totalCashIn: Long
        get() = MoneyFormat.addMinor(openingCash, grossCash, totalPreviousDueCash)

    val totalCashOut: Long
        get() = MoneyFormat.addMinor(totalExpenses, totalStaffAdvances, totalCashPurchases)

    // 3. Expected Cash in Drawer
    val expectedCashInDrawer: Long
        get() = MoneyFormat.subtractMinor(totalCashIn, totalCashOut)

    val netCash: Long
        get() = expectedCashInDrawer

    // Cash Shortage / Excess: Actual Closing Cash minus Expected Cash
    fun variance(actualCashCount: Long): Long = MoneyFormat.calculateVarianceMinor(actualCashCount, expectedCashInDrawer)

    val cashDiscrepancy: Long
        get() = MoneyFormat.subtractMinor(closingCash, expectedCashInDrawer)

    val isExcess: Boolean
        get() = cashDiscrepancy > 0L

    val isShortage: Boolean
        get() = cashDiscrepancy < 0L

    val netCardAndDigital: Long
        get() = MoneyFormat.addMinor(madaPayments, digitalWallet, totalPreviousDueBank)
}

@Entity(tableName = "draft_reports")
data class DraftReport(
    @PrimaryKey val id: Int = 1,
    val cashierName: String = "",
    val shift: String = "Day",
    val dateInMillis: Long = System.currentTimeMillis(),
    val openingCash: Long = 0L,
    val closingCash: Long = 0L,
    val grossCash: Long = 0L,
    val madaPayments: Long = 0L,
    val digitalWallet: Long = 0L,
    val totalDiscounts: Long = 0L,
    val salesReturns: Long = 0L,
    val staffMealsCount: Int = 0,
    val totalExpenses: Long = 0L,
    val muasselQty: Double = 0.0,
    val outdoorShishaQty: Double = 0.0,
    val dueCreditEntriesJson: String = "[]",
    val previousDueCollectionsJson: String = "[]",
    val staffAdvancesJson: String = "[]",
    val unpaidBillsJson: String = "[]",
    val purchasedItemsJson: String = "[]",
    val notes: String = ""
) {
    val totalSales: Long
        get() = MoneyFormat.addMinor(grossCash, madaPayments, digitalWallet)

    val netSales: Long
        get() = MoneyFormat.subtractMinor(MoneyFormat.subtractMinor(totalSales, totalDiscounts), salesReturns).coerceAtLeast(0L)
}

data class DueCreditItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val customerName: String,
    val amount: Long,
    val note: String = "",
    val phone: String = ""
)

data class PreviousDueCollectionItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val customerName: String,
    val amount: Long,
    val paymentMode: String = "CASH", // CASH, CARD
    val note: String = ""
)

data class StaffAdvanceItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val staffName: String,
    val amount: Long,
    val reason: String = ""
)

data class UnpaidBillItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val tableOrOrderRef: String,
    val amount: Long,
    val reason: String = ""
)

data class PurchasedInventoryItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val itemName: String,
    val quantity: Double = 0.0,
    val unitPrice: Long = 0L,
    val totalAmount: Long = 0L,
    val paidVia: String = "CASH", // CASH, BANK
    val supplier: String = ""
)

@Entity(tableName = "audit_logs")
data class AuditLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val username: String,
    val action: String,
    val details: String
)

@Entity(tableName = "business_profile")
data class BusinessProfile(
    @PrimaryKey val id: Int = 1,
    val businessName: String = "My Store",
    val vatNumber: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val mapLat: Double = 0.0,
    val mapLng: Double = 0.0,
    val workingHours: String = "08:00 AM - 10:00 PM",
    val currency: String = "USD",
    val country: String = "United States",
    val vatRate: Double = 0.0,
    val isTaxEnabled: Boolean = false,
    val isTaxIncluded: Boolean = true,
    val logoUri: String = ""
) {
    @get:Ignore
    val taxEnabled: Boolean get() = isTaxEnabled
    @get:Ignore
    val taxRatePercent: Double get() = vatRate
    @get:Ignore
    val taxInclusive: Boolean get() = isTaxIncluded
}

data class UpdateBusinessRequest(
    val businessName: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val address: String = "",
    val mapLat: Double = 0.0,
    val mapLng: Double = 0.0
)

@Entity(tableName = "app_settings")
data class AppSetting(
    @PrimaryKey val key: String,
    val value: String
)

@Entity(tableName = "saved_addresses")
data class SavedAddress(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val firstName: String = "",
    val lastName: String = "",
    val phone: String = "",
    val alternatePhone: String = "",
    val countryCode: String = "SA",
    val dialCode: String = "+966",
    val addressLine: String = "",
    val arabicAddressLine: String = "",
    val aptSuite: String = "",
    val addressLabel: String = "",
    val mapLat: Double = 0.0,
    val mapLng: Double = 0.0,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

enum class AppModule(val key: String, @StringRes val titleRes: Int) {
    SHIFT_REPORT("shift_report", R.string.shift_report_module)
}

enum class AppCountry(
    val code: String,
    val displayName: String,
    @StringRes val nameRes: Int,
    val currencyCode: String,
    val currencySymbol: String,
    val defaultVatRate: Double = 15.0,
    val dialCode: String = "+880"
) {
    BANGLADESH("BD", "Bangladesh", R.string.country_bangladesh, "BDT", "BDT", 15.0, "+880"),
    SAUDI_ARABIA("SA", "Saudi Arabia", R.string.country_saudi_arabia, "SAR", "SAR", 15.0, "+966"),
    UNITED_ARAB_EMIRATES("AE", "United Arab Emirates", R.string.country_uae, "AED", "AED", 5.0, "+971"),
    QATAR("QA", "Qatar", R.string.country_qatar, "QAR", "QAR", 0.0, "+974"),
    KUWAIT("KW", "Kuwait", R.string.country_kuwait, "KWD", "KWD", 0.0, "+965"),
    OMAN("OM", "Oman", R.string.country_oman, "OMR", "OMR", 5.0, "+968"),
    BAHRAIN("BH", "Bahrain", R.string.country_bahrain, "BHD", "BHD", 10.0, "+973"),
    UNITED_STATES("US", "United States", R.string.country_usa, "USD", "$", 8.25, "+1"),
    UNITED_KINGDOM("GB", "United Kingdom", R.string.country_uk, "GBP", "£", 20.0, "+44"),
    EUROPEAN_UNION("EU", "European Union", R.string.country_eu, "EUR", "€", 19.0, "+49"),
    INDIA("IN", "India", R.string.country_india, "INR", "INR", 18.0, "+91"),
    PAKISTAN("PK", "Pakistan", R.string.country_pakistan, "PKR", "PKR", 17.0, "+92"),
    MALAYSIA("MY", "Malaysia", R.string.country_malaysia, "MYR", "RM", 6.0, "+60"),
    SINGAPORE("SG", "Singapore", R.string.country_singapore, "SGD", "S$", 9.0, "+65"),
    CANADA("CA", "Canada", R.string.country_canada, "CAD", "C$", 13.0, "+1"),
    AUSTRALIA("AU", "Australia", R.string.country_australia, "AUD", "A$", 10.0, "+61"),
    TURKEY("TR", "Turkey", R.string.country_turkey, "TRY", "TRY", 20.0, "+90"),
    EGYPT("EG", "Egypt", R.string.country_egypt, "EGP", "EGP", 14.0, "+20"),
    JAPAN("JP", "Japan", R.string.country_japan, "JPY", "¥", 10.0, "+81"),
    CHINA("CN", "China", R.string.country_china, "CNY", "¥", 13.0, "+86"),
    INDONESIA("ID", "Indonesia", R.string.country_indonesia, "IDR", "Rp", 11.0, "+62");

    val displayNameEn: String get() = displayName

    companion object {
        fun fromCode(code: String?): AppCountry {
            if (code.isNullOrBlank()) return BANGLADESH
            return entries.find { it.code.equals(code, ignoreCase = true) || it.currencyCode.equals(code, ignoreCase = true) || it.name.equals(code, ignoreCase = true) } ?: BANGLADESH
        }
    }
}

enum class AppLanguage(
    val code: String,
    val displayName: String,
    @StringRes val titleRes: Int,
    val isRtl: Boolean = false
) {
    ENGLISH("en", "English", R.string.lang_english, isRtl = false),
    BENGALI("bn", "Bengali", R.string.lang_bengali, isRtl = false),
    ARABIC("ar", "Arabic", R.string.lang_arabic, isRtl = true);

    val nativeName: String get() = displayName

    companion object {
        fun fromCode(code: String?): AppLanguage {
            if (code.isNullOrBlank()) return ENGLISH
            return entries.find { it.code.equals(code, ignoreCase = true) || it.name.equals(code, ignoreCase = true) } ?: ENGLISH
        }
    }
}
