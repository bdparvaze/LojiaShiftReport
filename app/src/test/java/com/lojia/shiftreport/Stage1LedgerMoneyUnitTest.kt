package com.lojia.shiftreport

import com.lojia.shiftreport.data.ShiftReport
import com.lojia.shiftreport.report.*
import com.lojia.shiftreport.util.MoneyFormat
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Stage 1 Unit Test: Verifies that ledger parsers in ShiftReportLedgerTabs and SecondaryLedgerTabs
 * read JSON values as Long minor units (1 SAR = 100) and format with MoneyFormat.formatMinor,
 * avoiding the 100x display magnification bug.
 */
class Stage1LedgerMoneyUnitTest {

    @Test
    fun parseEmployerAdvances_readsMinorUnitsAndFormatsCorrectly() {
        val report = ShiftReport(
            id = 1,
            cashierName = "Cashier 1",
            staffAdvancesJson = """[{"staffName":"HASAN","amount":20000,"paymentMode":"CASH"}]"""
        )
        val items = parseEmployerAdvances(listOf(report))
        assertEquals(1, items.size)
        val item = items[0]
        assertEquals("HASAN", item.staffName)
        assertEquals(20000L, item.amountMinor)
        assertEquals("CASH", item.paymentMode)

        val formatted = MoneyFormat.formatMinor(item.amountMinor, "SAR")
        assertEquals("200.00 SAR", formatted)
    }

    @Test
    fun parseDueSalesAndCollections_readsMinorUnitsAndComputesRemaining() {
        val report = ShiftReport(
            id = 2,
            cashierName = "Cashier 1",
            dueCreditEntriesJson = """[{"receiptNo":"1001","customerName":"Ahmed","amount":5000}]""",
            previousDueCollectionsJson = """[{"receiptNo":"1001","customerName":"Ahmed","amount":2000,"paymentMode":"CASH"}]"""
        )

        val sales = parseDueSales(listOf(report))
        assertEquals(1, sales.size)
        assertEquals(5000L, sales[0].amountMinor)
        assertEquals("50.00 SAR", MoneyFormat.formatMinor(sales[0].amountMinor, "SAR"))

        val collections = parseDueCollections(listOf(report))
        assertEquals(1, collections.size)
        assertEquals(2000L, collections[0].amountMinor)
        assertEquals("20.00 SAR", MoneyFormat.formatMinor(collections[0].amountMinor, "SAR"))

        val summary = DueReceiptSummary(
            receiptNo = "1001",
            initialDueMinor = sales[0].amountMinor,
            totalCollectedMinor = collections[0].amountMinor
        )
        assertEquals(3000L, summary.remainingDueMinor)
        assertEquals("30.00 SAR", MoneyFormat.formatMinor(summary.remainingDueMinor, "SAR"))
    }

    @Test
    fun parsePaidOutItems_readsMinorUnitsAndPreservesQuantity() {
        val report = ShiftReport(
            id = 3,
            cashierName = "Cashier 1",
            purchasedItemsJson = """[{"itemName":"Milk","quantity":2,"unitPrice":1500,"totalAmount":3000}]"""
        )
        val items = parsePaidOutItems(listOf(report))
        assertEquals(1, items.size)
        val item = items[0]
        assertEquals("Milk", item.itemName)
        assertEquals(2, item.qty)
        assertEquals(1500L, item.unitPriceMinor)
        assertEquals(3000L, item.totalAmountMinor)

        assertEquals("15.00 SAR", MoneyFormat.formatMinor(item.unitPriceMinor, "SAR"))
        assertEquals("30.00 SAR", MoneyFormat.formatMinor(item.totalAmountMinor, "SAR"))
    }

    @Test
    fun parseWalkoutItems_readsMinorUnitsAndFormatsCorrectly() {
        val report = ShiftReport(
            id = 4,
            cashierName = "Cashier 1",
            unpaidBillsJson = """[{"tableOrOrderRef":"Table 5","amount":7500}]"""
        )
        val items = parseWalkoutItems(listOf(report))
        assertEquals(1, items.size)
        val item = items[0]
        assertEquals("Table 5", item.description)
        assertEquals(7500L, item.amountMinor)
        assertEquals("75.00 SAR", MoneyFormat.formatMinor(item.amountMinor, "SAR"))
    }

    @Test
    fun stage1B_monthlyCalculations_minorUnitsAndNetCashflow() {
        // Shift with stage 1 test data:
        // due sale 25 (2500L), due collection 20 (2000L),
        // employer advances cash 200 (20000L) + bank 100 (10000L) = 300 (30000L),
        // paid out 50 (5000L), walk-out 25 (2500L)
        val report = ShiftReport(
            id = 5,
            cashierName = "Cashier 1",
            grossCash = 50000L, // 500.00 SAR
            madaPayments = 20000L, // 200.00 SAR
            digitalWallet = 10000L, // 100.00 SAR
            totalExpenses = 0L,
            dueCreditEntriesJson = """[{"receiptNo":"R1","amount":2500}]""",
            previousDueCollectionsJson = """[{"receiptNo":"R1","amount":2000,"paymentMode":"CASH"}]""",
            staffAdvancesJson = """[{"staffName":"A","amount":20000,"paymentMode":"CASH"},{"staffName":"B","amount":10000,"paymentMode":"BANK"}]""",
            purchasedItemsJson = """[{"itemName":"Supplies","quantity":1,"unitPrice":5000,"totalAmount":5000}]""",
            unpaidBillsJson = """[{"tableOrOrderRef":"T1","amount":2500}]"""
        )
        val monthShiftReports = listOf(report)

        // 1. Issued Due: read with optLong, convert to major once
        val minorIssuedDue = monthShiftReports.sumOf { r ->
            val arr = org.json.JSONArray(r.dueCreditEntriesJson)
            var sum = 0L
            for (i in 0 until arr.length()) sum += arr.getJSONObject(i).optLong("amount", 0L)
            sum
        }
        val monthTotalIssuedDue = MoneyFormat.toMajorUnits(minorIssuedDue)
        assertEquals(25.00, monthTotalIssuedDue, 0.001)

        // 2. Collected Due: read with optLong, convert to major once
        val minorCollectedDue = monthShiftReports.sumOf { r ->
            val arr = org.json.JSONArray(r.previousDueCollectionsJson)
            var sum = 0L
            for (i in 0 until arr.length()) sum += arr.getJSONObject(i).optLong("amount", 0L)
            sum
        }
        val monthTotalCollectedDue = MoneyFormat.toMajorUnits(minorCollectedDue)
        assertEquals(20.00, monthTotalCollectedDue, 0.001)

        // 3. Staff Advances: read with optLong, convert to major once
        val minorStaffAdvances = monthShiftReports.sumOf { r ->
            val arr = org.json.JSONArray(r.staffAdvancesJson)
            var sum = 0L
            for (i in 0 until arr.length()) sum += arr.getJSONObject(i).optLong("amount", 0L)
            sum
        }
        val monthTotalStaffAdvances = MoneyFormat.toMajorUnits(minorStaffAdvances)
        assertEquals(300.00, monthTotalStaffAdvances, 0.001)

        // 4. Product Purchases: read with optLong, convert to major once
        val minorPurchases = monthShiftReports.sumOf { r ->
            val arr = org.json.JSONArray(r.purchasedItemsJson)
            var sum = 0L
            for (i in 0 until arr.length()) sum += arr.getJSONObject(i).optLong("totalAmount", 0L)
            sum
        }
        val monthTotalPurchases = MoneyFormat.toMajorUnits(minorPurchases)
        assertEquals(50.00, monthTotalPurchases, 0.001)

        // 5. Walkout: read with optLong, format with formatMinor
        val walkoutItems = parseWalkoutItems(monthShiftReports)
        val totalWalkoutMinor = walkoutItems.sumOf { it.amountMinor }
        assertEquals(2500L, totalWalkoutMinor)
        assertEquals(25.00, MoneyFormat.toMajorUnits(totalWalkoutMinor), 0.001)

        // 6. Cash In & Bank In (already major Double in dailyDataList)
        val monthTotalCashIn = MoneyFormat.toMajorUnits(report.grossCash) // 500.00
        val monthTotalBankIn = MoneyFormat.toMajorUnits(report.madaPayments + report.digitalWallet) // 300.00
        val monthTotalExpenses = MoneyFormat.toMajorUnits(report.totalExpenses) // 0.00

        // 7. Net Monthly Cashflow: (Cash + Bank + Recovery) - (Expenses + Purchases + Advances)
        val totalInflow = monthTotalCashIn + monthTotalBankIn + monthTotalCollectedDue
        val totalOutflow = monthTotalExpenses + monthTotalPurchases + monthTotalStaffAdvances
        val monthNetCashflow = totalInflow - totalOutflow

        assertEquals(820.00, totalInflow, 0.001) // 500 + 300 + 20
        assertEquals(350.00, totalOutflow, 0.001) // 0 + 50 + 300
        assertEquals(470.00, monthNetCashflow, 0.001) // 820 - 350
    }
}
