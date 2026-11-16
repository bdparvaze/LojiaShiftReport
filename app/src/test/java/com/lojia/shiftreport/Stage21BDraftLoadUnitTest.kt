package com.lojia.shiftreport

import com.lojia.shiftreport.report.draftFieldText
import org.junit.Assert.assertEquals
import org.junit.Test

class Stage21BDraftLoadUnitTest {
    @Test
    fun formatPositiveMajorWithCents() {
        assertEquals("25.00", draftFieldText(2500L))
    }

    @Test
    fun formatSmallMinorAmount() {
        assertEquals("0.05", draftFieldText(5L))
    }

    @Test
    fun formatZero() {
        assertEquals("0.00", draftFieldText(0L))
    }

    @Test
    fun formatNegativeAmount() {
        assertEquals("-2.50", draftFieldText(-250L))
    }

    @Test
    fun formatLargeAmountWithoutGroupingOrScientificNotation() {
        assertEquals("12345678.90", draftFieldText(1234567890L))
    }
}
