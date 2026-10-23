package com.lojia.shiftreport.report

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

data class StaffEntry(val name: String, val amount: BigDecimal, val type: PayType) {
    constructor(name: String, amountDouble: Double, type: PayType) : this(name, BigDecimal.valueOf(amountDouble), type)
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
    val total: BigDecimal = BigDecimal.ZERO
) {
    constructor(name: String, qty: Int, unitPriceDouble: Double, totalDouble: Double) : this(
        name, qty, BigDecimal.valueOf(unitPriceDouble), BigDecimal.valueOf(totalDouble)
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
    val staffEntries: List<StaffEntry>,
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
        staffEntries: List<StaffEntry>,
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
        staffEntries = staffEntries,
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
