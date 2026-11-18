package com.lojia.shiftreport

import com.lojia.shiftreport.data.StaffAdvanceItem
import com.lojia.shiftreport.report.ShiftReportSerializer
import com.lojia.shiftreport.report.staffAdvanceMinor
import org.junit.Assert.assertEquals
import org.junit.Test

class Stage23GSingleSourceUnitTest {

    @Test
    fun staffAdvanceMinor_threeDigitCurrency() {
        assertEquals(1500L, staffAdvanceMinor(1.5, 3))
    }

    @Test
    fun staffAdvanceMinor_twoDigitCurrency() {
        assertEquals(2500L, staffAdvanceMinor(25.0, 2))
    }

    @Test
    fun advancesJson_roundTrip_keepsMinorAmounts() {
        val original = listOf(
            StaffAdvanceItem(
                id = "adv-1",
                staffName = "Hasan",
                amount = 1500L,
                reason = "",
                paymentMode = "CASH"
            )
        )
        val json = ShiftReportSerializer.serializeAdvances(original)
        val parsed = ShiftReportSerializer.deserializeAdvances(json)

        assertEquals(1, parsed.size)
        assertEquals("Hasan", parsed[0].staffName)
        assertEquals(1500L, parsed[0].amount)
        assertEquals("CASH", parsed[0].paymentMode)
    }
}
