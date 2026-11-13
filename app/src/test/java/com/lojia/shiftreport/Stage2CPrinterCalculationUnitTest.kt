package com.lojia.shiftreport

import com.lojia.shiftreport.data.ShiftReport
import com.lojia.shiftreport.printer.BluetoothPrinterManager
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Stage 2C Unit Test: Verifies the thermal Z-report pure cash-drawer reconciliation helper
 * matches ShiftReport.expectedCashInDrawer and computes exact minor-unit variance.
 */
class Stage2CPrinterCalculationUnitTest {

    @Test
    fun stage2c_printerZReportCashDrawerMinor_matchesShiftReportExpectedCashAndZeroVariance() {
        val result = BluetoothPrinterManager.calculateZReportCashDrawerMinor(
            openingCash = 0L,
            grossCash = 48700L,
            totalPreviousDueCash = 2000L,
            totalCashOut = 28000L,
            actualCashCount = 22700L
        )

        assertEquals(22700L, result.expectedCash)
        assertEquals(0L, result.variance)

        val report = ShiftReport(
            id = 1,
            cashierName = "Cashier 1",
            openingCash = 0L,
            closingCash = 22700L,
            grossCash = 48700L,
            madaPayments = 168200L,
            digitalWallet = 0L,
            totalExpenses = 3000L,
            previousDueCollectionsJson = """[{"receiptNo":"1001","customerName":"Ahmed","amount":2000,"paymentMode":"CASH"}]""",
            staffAdvancesJson = """[
                {"staffName":"Hasan","amount":20000,"paymentMode":"CASH"},
                {"staffName":"Ali","amount":10000,"paymentMode":"BANK"}
            ]""",
            purchasedItemsJson = """[{"itemName":"Ice","quantity":1,"unitPrice":5000,"totalAmount":5000,"paidVia":"CASH"}]"""
        )

        assertEquals(report.expectedCashInDrawer, result.expectedCash)
        assertEquals(report.variance(22700L), result.variance)
    }
}
