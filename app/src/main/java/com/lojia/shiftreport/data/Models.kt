package com.lojia.shiftreport.data

import androidx.annotation.StringRes
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.lojia.shiftreport.R
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
    val status: String = "OPEN", // OPEN, CLOSED
    val openedAt: Long = System.currentTimeMillis(),
    val closedAt: Long? = null,
    val startingCash: Double = 500.0,
    val cashSales: Double = 0.0,
    val cardSales: Double = 0.0,
    val digitalSales: Double = 0.0,
    val totalPayIn: Double = 0.0,
    val totalPayOut: Double = 0.0,
    val expectedCash: Double = 500.0,
    val actualCashCount: Double = 0.0,
    val variance: Double = 0.0,
    val notes: String = ""
)

@Entity(tableName = "cash_movements")
data class CashMovement(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val shiftSessionId: Int = 0,
    val type: String, // PAY_IN, PAY_OUT
    val amount: Double,
    val reason: String,
    val cashierName: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "cashiers")
data class Cashier(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val pin: String = SecurityUtils.hashSecret("1111"),
    val role: String = "CASHIER", // ADMIN, CASHIER
    val active: Boolean = true
)

@Entity(tableName = "shift_reports")
data class ShiftReport(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val cashierName: String,
    val shift: String = "Day", // "Day", "Night", "Morning", "Evening"
    val dateInMillis: Long = System.currentTimeMillis(),
    val grossCash: Double = 0.0,
    val madaPayments: Double = 0.0,
    val digitalWallet: Double = 0.0,
    val staffMealsCount: Int = 0,
    val totalExpenses: Double = 0.0,
    val muasselQty: Double = 0.0,
    val outdoorShishaQty: Double = 0.0,
    val dueCreditEntriesJson: String = "[]",
    val previousDueCollectionsJson: String = "[]",
    val staffAdvancesJson: String = "[]",
    val unpaidBillsJson: String = "[]",
    val purchasedItemsJson: String = "[]",
    val notes: String = ""
) {
    // 1. Total Sales (by payment method only - cash + card/mada + digital wallet)
    // Note: Due/Credit sales are NOT added to Sales cash figures. They are tracked as Receivables.
    val totalSales: Double
        get() = grossCash + madaPayments + digitalWallet

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
                        amount = obj.optDouble("amount", 0.0),
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
                        amount = obj.optDouble("amount", 0.0),
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
                        amount = obj.optDouble("amount", 0.0),
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
                val qty = obj.optDouble("quantity", obj.optDouble("qty", 1.0))
                val unitPrice = obj.optDouble("unitPrice", obj.optDouble("price", 0.0))
                val total = obj.optDouble("totalAmount", obj.optDouble("total", qty * unitPrice))
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
                        amount = obj.optDouble("amount", 0.0),
                        reason = obj.optString("reason", "")
                    )
                )
            }
            list
        } catch (e: Exception) { emptyList() }

    // Due / Receivables Tracking
    val totalDueCredit: Double
        get() = dueCreditEntriesList.sumOf { it.amount }

    val totalDueIssued: Double
        get() = totalDueCredit

    val totalPreviousDueCash: Double
        get() = previousDueCollectionsList.filter { it.paymentMode.equals("CASH", ignoreCase = true) }.sumOf { it.amount }

    val totalDueCollectedCash: Double
        get() = totalPreviousDueCash

    val totalPreviousDueBank: Double
        get() = previousDueCollectionsList.filter { !it.paymentMode.equals("CASH", ignoreCase = true) }.sumOf { it.amount }

    val totalDueCollectedBank: Double
        get() = totalPreviousDueBank

    val totalDueCollected: Double
        get() = previousDueCollectionsList.sumOf { it.amount }

    // Staff Advances
    val totalStaffAdvances: Double
        get() = staffAdvancesList.sumOf { it.amount }

    val totalStaffAdvancesAmount: Double
        get() = totalStaffAdvances

    // Purchases
    val totalCashPurchases: Double
        get() = purchasedItemsList.filter { it.paidVia.equals("CASH", ignoreCase = true) }.sumOf { it.totalAmount }

    val totalPurchasedCash: Double
        get() = totalCashPurchases

    val totalPurchasedBank: Double
        get() = purchasedItemsList.filter { !it.paidVia.equals("CASH", ignoreCase = true) }.sumOf { it.totalAmount }

    val totalPurchasedAll: Double
        get() = purchasedItemsList.sumOf { it.totalAmount }

    val totalUnpaidLoss: Double
        get() = unpaidBillsList.sumOf { it.amount }

    // 2. Cash Inflow & Outflow Formulas
    val totalCashIn: Double
        get() = grossCash + totalPreviousDueCash

    val totalCashOut: Double
        get() = totalExpenses + totalStaffAdvances + totalCashPurchases

    // 3. Expected Cash in Drawer
    val expectedCashInDrawer: Double
        get() = totalCashIn - totalCashOut

    val netCash: Double
        get() = expectedCashInDrawer

    fun variance(actualCashCount: Double): Double = actualCashCount - expectedCashInDrawer

    val netCardAndDigital: Double
        get() = madaPayments + digitalWallet + totalPreviousDueBank
}

@Entity(tableName = "draft_reports")
data class DraftReport(
    @PrimaryKey val id: Int = 1,
    val cashierName: String = "",
    val shift: String = "Day",
    val dateInMillis: Long = System.currentTimeMillis(),
    val grossCash: Double = 0.0,
    val madaPayments: Double = 0.0,
    val digitalWallet: Double = 0.0,
    val staffMealsCount: Int = 0,
    val totalExpenses: Double = 0.0,
    val muasselQty: Double = 0.0,
    val outdoorShishaQty: Double = 0.0,
    val dueCreditEntriesJson: String = "[]",
    val previousDueCollectionsJson: String = "[]",
    val staffAdvancesJson: String = "[]",
    val unpaidBillsJson: String = "[]",
    val purchasedItemsJson: String = "[]",
    val notes: String = ""
) {
    val totalSales: Double
        get() = grossCash + madaPayments + digitalWallet
}

data class DueCreditItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val customerName: String,
    val amount: Double,
    val note: String = "",
    val phone: String = ""
)

data class PreviousDueCollectionItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val customerName: String,
    val amount: Double,
    val paymentMode: String = "CASH", // CASH, CARD
    val note: String = ""
)

data class StaffAdvanceItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val staffName: String,
    val amount: Double,
    val reason: String = ""
)

data class UnpaidBillItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val tableOrOrderRef: String,
    val amount: Double,
    val reason: String = ""
)

data class PurchasedInventoryItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val itemName: String,
    val quantity: Double,
    val unitPrice: Double,
    val totalAmount: Double = quantity * unitPrice,
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

enum class AppModule(val key: String, @StringRes val titleRes: Int) {
    SHIFT_REPORT("shift_report", R.string.shift_report_module)
}

enum class AppCountry(
    val code: String,
    val displayName: String,
    @StringRes val nameRes: Int,
    val currencyCode: String,
    val currencySymbol: String,
    val defaultVatRate: Double = 15.0
) {
    BANGLADESH("BD", "Bangladesh", R.string.country_bangladesh, "BDT", "BDT", 15.0),
    SAUDI_ARABIA("SA", "Saudi Arabia", R.string.country_saudi_arabia, "SAR", "SAR", 15.0),
    UNITED_ARAB_EMIRATES("AE", "United Arab Emirates", R.string.country_uae, "AED", "AED", 5.0),
    QATAR("QA", "Qatar", R.string.country_qatar, "QAR", "QAR", 0.0),
    KUWAIT("KW", "Kuwait", R.string.country_kuwait, "KWD", "KWD", 0.0),
    OMAN("OM", "Oman", R.string.country_oman, "OMR", "OMR", 5.0),
    BAHRAIN("BH", "Bahrain", R.string.country_bahrain, "BHD", "BHD", 10.0),
    UNITED_STATES("US", "United States", R.string.country_usa, "USD", "$", 8.25),
    UNITED_KINGDOM("GB", "United Kingdom", R.string.country_uk, "GBP", "£", 20.0),
    EUROPEAN_UNION("EU", "European Union", R.string.country_eu, "EUR", "€", 19.0),
    INDIA("IN", "India", R.string.country_india, "INR", "INR", 18.0),
    PAKISTAN("PK", "Pakistan", R.string.country_pakistan, "PKR", "PKR", 17.0),
    MALAYSIA("MY", "Malaysia", R.string.country_malaysia, "MYR", "RM", 6.0),
    SINGAPORE("SG", "Singapore", R.string.country_singapore, "SGD", "S$", 9.0),
    CANADA("CA", "Canada", R.string.country_canada, "CAD", "C$", 13.0),
    AUSTRALIA("AU", "Australia", R.string.country_australia, "AUD", "A$", 10.0),
    TURKEY("TR", "Turkey", R.string.country_turkey, "TRY", "TRY", 20.0),
    EGYPT("EG", "Egypt", R.string.country_egypt, "EGP", "EGP", 14.0),
    JAPAN("JP", "Japan", R.string.country_japan, "JPY", "¥", 10.0),
    CHINA("CN", "China", R.string.country_china, "CNY", "¥", 13.0),
    INDONESIA("ID", "Indonesia", R.string.country_indonesia, "IDR", "Rp", 11.0);

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
