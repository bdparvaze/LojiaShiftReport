package com.lojia.shiftreport

import com.lojia.shiftreport.data.AppDatabase
import com.lojia.shiftreport.data.ShiftReport
import com.lojia.shiftreport.data.ShiftSession
import com.lojia.shiftreport.util.MoneyFormat
import com.lojia.shiftreport.util.OfflineBackupManager
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for ShiftReport and ShiftSession backup export/restore round-trip fidelity,
 * legacy migration conversion (schemaVersion < 15), and getMoneyVal default unit handling.
 */
class BackupRoundTripTest {

    @Test
    fun shiftReport_roundTrip_allFieldsPreserved() {
        val original = ShiftReport(
            id = 42,
            cashierName = "Zayd Ali",
            shift = "Night",
            dateInMillis = 1712345678000L,
            openingCash = 125000L,
            closingCash = 345000L,
            grossCash = 220000L,
            madaPayments = 150000L,
            digitalWallet = 50000L,
            totalDiscounts = 1500L,
            salesReturns = 2500L,
            staffMealsCount = 4,
            totalExpenses = 8000L,
            muasselQty = 12.5,
            outdoorShishaQty = 6.0,
            dueCreditEntriesJson = """[{"id":"due1","customerName":"Customer A","amount":5000,"note":"Pending","phone":"+966500000000"}]""",
            previousDueCollectionsJson = """[{"id":"prev1","customerName":"Customer B","amount":3000,"paymentMode":"CASH","note":"Paid"}]""",
            staffAdvancesJson = """[{"id":"adv1","staffName":"Ahmed","amount":2000,"reason":"Emergency"}]""",
            unpaidBillsJson = """[{"id":"unpaid1","tableOrOrderRef":"Table 4","amount":4500,"reason":"Guest left"}]""",
            purchasedItemsJson = """[{"id":"purch1","itemName":"Coffee Beans","quantity":5.0,"unitPrice":1000,"totalAmount":5000,"paidVia":"CASH","supplier":"Supplier X"}]""",
            notes = "Full shift audit completed cleanly",
            isLocked = true,
            managerSignedBy = "Manager Sarah",
            managerSignTime = 1712349999000L
        )

        val json = OfflineBackupManager.shiftReportToJson(original)
        val restored = OfflineBackupManager.jsonToShiftReport(json, isLegacyBackup = false)

        assertEquals("Restored ShiftReport must exactly equal original", original, restored)
    }

    @Test
    fun shiftSession_roundTrip_allFieldsPreserved() {
        val original = ShiftSession(
            id = 99,
            cashierName = "Fatima Omar",
            shiftName = "Evening",
            status = "LOCKED",
            openedAt = 1712300000000L,
            closedAt = 1712340000000L,
            startingCash = 60000L,
            cashSales = 120000L,
            cardSales = 85000L,
            digitalSales = 30000L,
            totalDiscounts = 2000L,
            salesReturns = 1000L,
            totalPayIn = 5000L,
            totalPayOut = 3000L,
            expectedCash = 177000L,
            actualCashCount = 177000L,
            variance = 0L,
            notes = "Evening shift handover notes",
            isLocked = true,
            managerSignedBy = "Manager Karim",
            managerSignTime = 1712341000000L
        )

        val json = OfflineBackupManager.shiftSessionToJson(original)
        val restored = OfflineBackupManager.jsonToShiftSession(json, isLegacyBackup = false)

        assertEquals("Restored ShiftSession must exactly equal original", original, restored)
    }

    @Test
    fun legacyBackup_convertsMajorUnitsAndJsonArraysToMinorUnits() {
        val legacyJson = JSONObject().apply {
            put("id", 10)
            put("cashierName", "Legacy Staff")
            put("shift", "Day")
            put("dateInMillis", 1700000000000L)
            put("openingCash", 50.0) // 50.0 major -> 5000 minor
            put("closingCash", 150.75) // 150.75 -> 15075 minor
            put("grossCash", 100.50) // 100.50 -> 10050 minor
            put("madaPayments", 25.25) // 25.25 -> 2525 minor
            put("digitalWallet", 10.0)
            put("totalDiscounts", 5.0)
            put("salesReturns", 2.50)
            put("staffMealsCount", 2)
            put("totalExpenses", 15.0)
            put("muasselQty", 3.0)
            put("outdoorShishaQty", 1.0)
            put("dueCreditEntriesJson", """[{"id":"1","customerName":"Ali","amount":12.50}]""")
            put("previousDueCollectionsJson", """[{"id":"2","customerName":"Omar","amount":7.25}]""")
            put("staffAdvancesJson", """[{"id":"3","staffName":"Samir","amount":20.00}]""")
            put("unpaidBillsJson", """[{"id":"4","tableOrOrderRef":"T1","amount":8.50}]""")
            put("purchasedItemsJson", """[{"id":"5","itemName":"Milk","quantity":2.0,"unitPrice":3.50,"totalAmount":7.00}]""")
            put("notes", "Legacy test")
            put("isLocked", false)
        }

        val restored = OfflineBackupManager.jsonToShiftReport(legacyJson, isLegacyBackup = true)

        assertEquals(5000L, restored.openingCash)
        assertEquals(15075L, restored.closingCash)
        assertEquals(10050L, restored.grossCash)
        assertEquals(2525L, restored.madaPayments)
        assertEquals(1000L, restored.digitalWallet)
        assertEquals(500L, restored.totalDiscounts)
        assertEquals(250L, restored.salesReturns)
        assertEquals(1500L, restored.totalExpenses)

        // Check JSON array migrations
        assertTrue("dueCreditEntriesList should have minor units", restored.dueCreditEntriesList.isNotEmpty())
        assertEquals(1250L, restored.dueCreditEntriesList[0].amount)

        assertEquals(725L, restored.previousDueCollectionsList[0].amount)
        assertEquals(2000L, restored.staffAdvancesList[0].amount)
        assertEquals(850L, restored.unpaidBillsList[0].amount)
        assertEquals(350L, restored.purchasedItemsList[0].unitPrice)
        assertEquals(700L, restored.purchasedItemsList[0].totalAmount)
    }

    @Test
    fun getMoneyVal_handlesMissingKeyAndDefaultMinorUnitsCorrectly() {
        val emptyObj = JSONObject()
        val defaultMinor = 54321L

        // Non-legacy: missing key returns defaultMinor directly
        val nonLegacyResult = OfflineBackupManager.getMoneyVal(emptyObj, "missingKey", defaultMinor, isLegacy = false)
        assertEquals(54321L, nonLegacyResult)

        // Legacy: missing key returns defaultMinor directly without 100x scaling error
        val legacyResult = OfflineBackupManager.getMoneyVal(emptyObj, "missingKey", defaultMinor, isLegacy = true)
        assertEquals(54321L, legacyResult)

        // Non-legacy with key
        val populatedObj = JSONObject().apply {
            put("grossCash", 98765L)
        }
        val populatedResult = OfflineBackupManager.getMoneyVal(populatedObj, "grossCash", 0L, isLegacy = false)
        assertEquals(98765L, populatedResult)
    }

    @Test
    fun parseBackupSummaryFromString_correctlyParsesStats() {
        val sampleJson = """
            {
                "app": "Lojia Shift Report",
                "exportDate": "2026-10-05 12:00:00",
                "shiftReports": [{}, {}],
                "shiftSessions": [{}],
                "cashiers": [{}, {}, {}],
                "businessProfile": {"businessName": "Test Store"}
            }
        """.trimIndent()

        val summaryResult = OfflineBackupManager.parseBackupSummaryFromString(sampleJson)
        assertTrue(summaryResult.isSuccess)
        val summary = summaryResult.getOrThrow()
        assertEquals(2, summary.shiftReportsCount)
        assertEquals(1, summary.shiftSessionsCount)
        assertEquals(3, summary.cashiersCount)
        assertEquals(6, summary.totalRecords)
        assertTrue(summary.hasBusinessProfile)
        assertFalse(summary.hasUserProfile)
    }
}
