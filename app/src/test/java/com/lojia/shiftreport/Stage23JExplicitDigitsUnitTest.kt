package com.lojia.shiftreport

import com.lojia.shiftreport.data.ShiftReport
import com.lojia.shiftreport.util.OfflineBackupManager
import com.lojia.shiftreport.util.PdfNotesParser
import com.lojia.shiftreport.util.PdfReportGenerator
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class Stage23JExplicitDigitsUnitTest {

    @Test
    fun extractStartingCashMinor_usesGivenDigits() {
        val report = ShiftReport(
            id = 1,
            cashierName = "Cashier",
            shift = "Day",
            dateInMillis = 1700000000000L,
            openingCash = 0L,
            notes = "Starting Cash: 1.5"
        )
        assertEquals(1500L, PdfNotesParser.extractStartingCashMinor(report, 3))
    }

    @Test
    fun getLongAmount_usesGivenDigits() {
        val obj = JSONObject().put("amount", 1.5)
        assertEquals(1500L, PdfReportGenerator.getLongAmount(obj, "amount", 0L, 3))
    }

    @Test
    fun getMoneyVal_zeroDigits() {
        val obj = JSONObject().put("grossCash", 1000.0)
        assertEquals(1000L, OfflineBackupManager.getMoneyVal(obj, "grossCash", 0L, isLegacy = true, digits = 0))
    }
}
