package com.lojia.shiftreport

import com.lojia.shiftreport.util.MoneyInput
import com.lojia.shiftreport.util.classifyMoneyInput
import org.junit.Assert.assertEquals
import org.junit.Test

class Stage23IDigitsUnitTest {

    @Test
    fun classifyMoneyInput_kwd_threeDecimals() {
        assertEquals(MoneyInput.Valid(1500L), classifyMoneyInput("1.500", 3))
    }

    @Test
    fun classifyMoneyInput_jpy_noDecimals() {
        assertEquals(MoneyInput.Valid(1000L), classifyMoneyInput("1000", 0))
    }

    @Test
    fun classifyMoneyInput_usd_twoDecimals() {
        assertEquals(MoneyInput.Valid(2500L), classifyMoneyInput("25.00", 2))
    }
}
