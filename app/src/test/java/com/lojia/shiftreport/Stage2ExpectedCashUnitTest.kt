package com.lojia.shiftreport

import com.lojia.shiftreport.data.PurchasedInventoryItem
import com.lojia.shiftreport.data.ShiftReport
import com.lojia.shiftreport.data.StaffAdvanceItem
import com.lojia.shiftreport.report.ItemEntry
import com.lojia.shiftreport.report.PayType
import com.lojia.shiftreport.report.ShiftReportSerializer
import com.lojia.shiftreport.util.MoneyFormat
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Stage 2 Unit Test: Verifies expected cash in drawer excludes BANK employer advances
 * and BANK paid-out purchases, defaults missing payment modes to CASH, and unifies
 * calculation through MoneyFormat.calculateExpectedCashMinor.
 */
class Stage2ExpectedCashUnitTest {

    @Test
    fun stage2_test1_cashAndBankAdvance_expectedCashIs22700() {
        val report = ShiftReport(
            id = 1,
            cashierName = "Cashier 1",
            openingCash = 0L,
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

        assertEquals(216900L, report.totalSales)
        assertEquals(0L, report.totalDueCredit)
        assertEquals(20000L, report.totalStaffAdvancesCash)
        assertEquals(10000L, report.totalStaffAdvancesBank)
        assertEquals(50700L, report.totalCashIn)
        assertEquals(28000L, report.totalCashOut)
        assertEquals(22700L, report.expectedCashInDrawer)
        assertEquals("227.00 SAR", MoneyFormat.formatMinor(report.expectedCashInDrawer, "SAR"))
    }

    @Test
    fun stage2_test2_missingPaymentModeDefaultsToCash_expectedCashIs12700() {
        val report = ShiftReport(
            id = 2,
            cashierName = "Cashier 1",
            openingCash = 0L,
            grossCash = 48700L,
            madaPayments = 168200L,
            digitalWallet = 0L,
            totalExpenses = 3000L,
            previousDueCollectionsJson = """[{"receiptNo":"1001","customerName":"Ahmed","amount":2000,"paymentMode":"CASH"}]""",
            staffAdvancesJson = """[
                {"staffName":"Hasan","amount":20000,"paymentMode":"CASH"},
                {"staffName":"Ali","amount":10000}
            ]""",
            purchasedItemsJson = """[{"itemName":"Ice","quantity":1,"unitPrice":5000,"totalAmount":5000,"paidVia":"CASH"}]"""
        )

        assertEquals(30000L, report.totalStaffAdvancesCash)
        assertEquals(0L, report.totalStaffAdvancesBank)
        assertEquals(38000L, report.totalCashOut)
        assertEquals(12700L, report.expectedCashInDrawer)
    }

    @Test
    fun stage2_test3_bankPurchaseExcludedFromCashOut_expectedCashIs27700() {
        val report = ShiftReport(
            id = 3,
            cashierName = "Cashier 1",
            openingCash = 0L,
            grossCash = 48700L,
            madaPayments = 168200L,
            digitalWallet = 0L,
            totalExpenses = 3000L,
            previousDueCollectionsJson = """[{"receiptNo":"1001","customerName":"Ahmed","amount":2000,"paymentMode":"CASH"}]""",
            staffAdvancesJson = """[
                {"staffName":"Hasan","amount":20000,"paymentMode":"CASH"},
                {"staffName":"Ali","amount":10000,"paymentMode":"BANK"}
            ]""",
            purchasedItemsJson = """[{"itemName":"Ice","quantity":1,"unitPrice":5000,"totalAmount":5000,"paidVia":"BANK"}]"""
        )

        assertEquals(0L, report.totalCashPurchases)
        assertEquals(5000L, report.totalPurchasedBank)
        assertEquals(23000L, report.totalCashOut)
        assertEquals(27700L, report.expectedCashInDrawer)
    }

    @Test
    fun stage2_test4_moneyFormatCalculateExpectedCashMinor_andSerializerPreservation() {
        val expected5Arg = MoneyFormat.calculateExpectedCashMinor(
            startingCash = 0L,
            cashSales = 48700L,
            payIn = 2000L,
            payOut = 28000L
        )
        assertEquals(22700L, expected5Arg)

        val expected2Arg = MoneyFormat.calculateExpectedCashMinor(
            totalCashIn = 50700L,
            totalCashOut = 28000L
        )
        assertEquals(22700L, expected2Arg)

        val advJson = ShiftReportSerializer.serializeAdvances(
            listOf(
                StaffAdvanceItem(staffName = "Hasan", amount = 20000L, paymentMode = "CASH"),
                StaffAdvanceItem(staffName = "Ali", amount = 10000L, paymentMode = "BANK")
            )
        )
        val parsedAdv = ShiftReportSerializer.deserializeAdvances(advJson)
        assertEquals(2, parsedAdv.size)
        assertEquals("CASH", parsedAdv[0].paymentMode)
        assertEquals("BANK", parsedAdv[1].paymentMode)

        val purchJson = ShiftReportSerializer.serializePurchased(
            listOf(
                PurchasedInventoryItem(itemName = "Ice", quantity = 1.0, unitPrice = 5000L, totalAmount = 5000L, paidVia = "BANK")
            )
        )
        val parsedPurch = ShiftReportSerializer.deserializePurchased(purchJson)
        assertEquals(1, parsedPurch.size)
        assertEquals("BANK", parsedPurch[0].paidVia)

        val defaultEntry = ItemEntry(name = "Milk", qty = 1, unitPriceDouble = 10.0, totalDouble = 10.0)
        assertEquals(PayType.CASH, defaultEntry.type)
        val bankEntry = ItemEntry(name = "Milk", qty = 1, unitPriceDouble = 10.0, totalDouble = 10.0, type = PayType.BANK)
        assertEquals(PayType.BANK, bankEntry.type)
    }

    @Test
    fun stage2_test5_bankDueCollectionExcludedFromExpectedCash_expectedCashIs22700() {
        val report = ShiftReport(
            id = 5,
            cashierName = "Cashier 1",
            openingCash = 0L,
            grossCash = 48700L,
            madaPayments = 168200L,
            digitalWallet = 0L,
            totalExpenses = 3000L,
            previousDueCollectionsJson = """[
                {"receiptNo":"1001","customerName":"Ahmed","amount":2000,"paymentMode":"CASH"},
                {"receiptNo":"1002","customerName":"Omar","amount":3000,"paymentMode":"BANK"}
            ]""",
            staffAdvancesJson = """[
                {"staffName":"Hasan","amount":20000,"paymentMode":"CASH"},
                {"staffName":"Ali","amount":10000,"paymentMode":"BANK"}
            ]""",
            purchasedItemsJson = """[{"itemName":"Ice","quantity":1,"unitPrice":5000,"totalAmount":5000,"paidVia":"CASH"}]"""
        )

        assertEquals(2000L, report.totalPreviousDueCash)
        assertEquals(3000L, report.totalPreviousDueBank)
        assertEquals(22700L, report.expectedCashInDrawer)
    }
}
