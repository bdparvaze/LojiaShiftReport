package com.lojia.shiftreport.report

import com.lojia.shiftreport.data.DueCreditItem
import com.lojia.shiftreport.data.PreviousDueCollectionItem
import com.lojia.shiftreport.data.PurchasedInventoryItem
import com.lojia.shiftreport.data.StaffAdvanceItem
import com.lojia.shiftreport.data.UnpaidBillItem
import java.math.BigDecimal

/* ----------------------------------------------------------------------
 * DATA MODELS FOR SHIFT REPORT ENTRY FORM (HIGH FINANCIAL PRECISION)
 * ---------------------------------------------------------------------- */
data class CreditEntry(val receiptNo: String, val amount: BigDecimal) {
    constructor(receiptNo: String, amountDouble: Double) : this(receiptNo, BigDecimal.valueOf(amountDouble))
    val amountDouble: Double get() = amount.toDouble()
}

data class OldDueEntry(val receiptNo: String, val amount: BigDecimal, val type: PayType) {
    constructor(receiptNo: String, amountDouble: Double, type: PayType) : this(receiptNo, BigDecimal.valueOf(amountDouble), type)
    val amountDouble: Double get() = amount.toDouble()
}

data class WalkoutEntry(val description: String, val amount: BigDecimal) {
    constructor(description: String, amountDouble: Double) : this(description, BigDecimal.valueOf(amountDouble))
    val amountDouble: Double get() = amount.toDouble()
}

data class ItemEntry(
    val name: String,
    val qty: Int = 0,
    val unitPrice: BigDecimal = BigDecimal.ZERO,
    val total: BigDecimal = BigDecimal.ZERO,
    val type: PayType = PayType.CASH
) {
    constructor(
        name: String,
        qty: Int,
        unitPriceDouble: Double,
        totalDouble: Double,
        type: PayType = PayType.CASH
    ) : this(
        name, qty, BigDecimal.valueOf(unitPriceDouble), BigDecimal.valueOf(totalDouble), type
    )
    val unitPriceDouble: Double get() = unitPrice.toDouble()
    val totalDouble: Double get() = total.toDouble()
}

/** Snapshot handed to onSave with exact financial BigDecimal fields. */
data class ShiftReportData(
    val cashier: String,
    val shift: String,
    val date: String,
    val cashReceipts: BigDecimal,
    val madaPayments: BigDecimal,
    val digitalWallet: BigDecimal = BigDecimal.ZERO,
    val startingCash: BigDecimal = BigDecimal.ZERO,
    val openingCash: BigDecimal = BigDecimal.ZERO,
    val closingCash: BigDecimal = BigDecimal.ZERO,
    val totalDiscounts: BigDecimal = BigDecimal.ZERO,
    val salesReturns: BigDecimal = BigDecimal.ZERO,
    val actualCash: BigDecimal? = null,
    val notes: String = "",
    val staffCount: Int,
    val totalExpenses: BigDecimal,
    val muassel: Int,
    val outdoorMuassel: Int,
    val creditEntries: List<CreditEntry>,
    val oldDueEntries: List<OldDueEntry>,
    val walkoutEntries: List<WalkoutEntry>,
    val itemEntries: List<ItemEntry>,
    val netCash: BigDecimal,
    val netMada: BigDecimal,
    val isLocked: Boolean = false,
    val managerSignedBy: String? = null,
    val managerSignTime: Long? = null,
    val dateMillis: Long = System.currentTimeMillis()
) {
    constructor(
        cashier: String,
        shift: String,
        date: String,
        cashReceipts: Double,
        madaPayments: Double,
        digitalWallet: Double = 0.0,
        startingCash: Double = 0.0,
        openingCash: Double = 0.0,
        closingCash: Double = 0.0,
        totalDiscounts: Double = 0.0,
        salesReturns: Double = 0.0,
        actualCash: Double? = null,
        notes: String = "",
        staffCount: Int,
        totalExpenses: Double,
        muassel: Int,
        outdoorMuassel: Int,
        creditEntries: List<CreditEntry>,
        oldDueEntries: List<OldDueEntry>,
        walkoutEntries: List<WalkoutEntry>,
        itemEntries: List<ItemEntry>,
        netCash: Double,
        netMada: Double,
        isLocked: Boolean = false,
        managerSignedBy: String? = null,
        managerSignTime: Long? = null,
        dateMillis: Long = System.currentTimeMillis()
    ) : this(
        cashier = cashier,
        shift = shift,
        date = date,
        cashReceipts = BigDecimal.valueOf(cashReceipts),
        madaPayments = BigDecimal.valueOf(madaPayments),
        digitalWallet = BigDecimal.valueOf(digitalWallet),
        startingCash = BigDecimal.valueOf(startingCash),
        openingCash = BigDecimal.valueOf(openingCash),
        closingCash = BigDecimal.valueOf(closingCash),
        totalDiscounts = BigDecimal.valueOf(totalDiscounts),
        salesReturns = BigDecimal.valueOf(salesReturns),
        actualCash = actualCash?.let { BigDecimal.valueOf(it) },
        notes = notes,
        staffCount = staffCount,
        totalExpenses = BigDecimal.valueOf(totalExpenses),
        muassel = muassel,
        outdoorMuassel = outdoorMuassel,
        creditEntries = creditEntries,
        oldDueEntries = oldDueEntries,
        walkoutEntries = walkoutEntries,
        itemEntries = itemEntries,
        netCash = BigDecimal.valueOf(netCash),
        netMada = BigDecimal.valueOf(netMada),
        isLocked = isLocked,
        managerSignedBy = managerSignedBy,
        managerSignTime = managerSignTime,
        dateMillis = dateMillis
    )
}

data class ShiftFormState(
    val cashier: String = "",
    val shift: String = "Day",
    val dateMillis: Long = System.currentTimeMillis(),
    val startingCashInput: String = "",
    val cashReceipts: String = "",
    val madaPayments: String = "",
    val digitalWalletInput: String = "",
    val totalDiscountsInput: String = "",
    val salesReturnsInput: String = "",
    val actualCashCountInput: String = "",
    val notesInput: String = "",
    val staffCount: Int = 0,
    val totalExpenses: String = "",
    val muassel: String = "",
    val outdoorMuassel: String = "",
    val dueCreditItems: List<DueCreditItem> = emptyList(),
    val previousDueCollections: List<PreviousDueCollectionItem> = emptyList(),
    val staffAdvances: List<StaffAdvanceItem> = emptyList(),
    val unpaidBills: List<UnpaidBillItem> = emptyList(),
    val purchasedItems: List<PurchasedInventoryItem> = emptyList()
)

data class ShiftFormActions(
    val onCashierChange: (String) -> Unit = {},
    val onShiftChange: (String) -> Unit = {},
    val onDateMillisChange: (Long) -> Unit = {},
    val onStartingCashChange: (String) -> Unit = {},
    val onCashReceiptsChange: (String) -> Unit = {},
    val onMadaPaymentsChange: (String) -> Unit = {},
    val onDigitalWalletChange: (String) -> Unit = {},
    val onTotalDiscountsChange: (String) -> Unit = {},
    val onSalesReturnsChange: (String) -> Unit = {},
    val onActualCashCountChange: (String) -> Unit = {},
    val onNotesChange: (String) -> Unit = {},
    val onStaffCountChange: (Int) -> Unit = {},
    val onTotalExpensesChange: (String) -> Unit = {},
    val onMuasselChange: (String) -> Unit = {},
    val onOutdoorMuasselChange: (String) -> Unit = {},
    val onAddDueCredit: (String, Double, Int) -> Unit = { _, _, _ -> },
    val onRemoveDueCredit: (String) -> Unit = {},
    val onAddPreviousDue: (String, Double, Int, String) -> Unit = { _, _, _, _ -> },
    val onRemovePreviousDue: (String) -> Unit = {},
    val onAddStaffAdvance: (String, Double, Int, String, String?) -> Unit = { _, _, _, _, _ -> },
    val onRemoveStaffAdvance: (String) -> Unit = {},
    val onAddUnpaidBill: (String, Double, Int) -> Unit = { _, _, _ -> },
    val onRemoveUnpaidBill: (String) -> Unit = {},
    val onAddPurchasedItem: (String, Int, Double, Double, Int, String) -> Unit = { _, _, _, _, _, _ -> },
    val onRemovePurchasedItem: (String) -> Unit = {},
    val onClearForm: () -> Unit = {}
)
