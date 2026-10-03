package com.lojia.shiftreport.report

/* ----------------------------------------------------------------------
 * DATA MODELS FOR SHIFT REPORT ENTRY FORM
 * ---------------------------------------------------------------------- */
data class CreditEntry(val receiptNo: String, val amount: Double)
data class OldDueEntry(val receiptNo: String, val amount: Double, val type: PayType)
data class StaffEntry(val name: String, val amount: Double, val type: PayType)
data class WalkoutEntry(val description: String, val amount: Double)
data class ItemEntry(
    val name: String,
    val qty: Int = 0,
    val unitPrice: Double = 0.0,
    val total: Double = 0.0
)

/** Snapshot handed to onSave. */
data class ShiftReportData(
    val cashier: String,
    val shift: String,
    val date: String,
    val cashReceipts: Double,
    val madaPayments: Double,
    val digitalWallet: Double = 0.0,
    val startingCash: Double = 0.0,
    val openingCash: Double = 0.0,
    val closingCash: Double = 0.0,
    val totalDiscounts: Double = 0.0,
    val salesReturns: Double = 0.0,
    val actualCash: Double? = null,
    val notes: String = "",
    val staffCount: Int,
    val totalExpenses: Double,
    val muassel: Int,
    val outdoorMuassel: Int,
    val creditEntries: List<CreditEntry>,
    val oldDueEntries: List<OldDueEntry>,
    val staffEntries: List<StaffEntry>,
    val walkoutEntries: List<WalkoutEntry>,
    val itemEntries: List<ItemEntry>,
    val netCash: Double,
    val netMada: Double,
    val isLocked: Boolean = false,
    val managerSignedBy: String? = null,
    val managerSignTime: Long? = null,
    val dateMillis: Long = System.currentTimeMillis()
)
