package com.lojia.shiftreport

import com.lojia.shiftreport.util.MoneyInput
import com.lojia.shiftreport.util.classifyMoneyInput
import org.junit.Assert.assertEquals
import org.junit.Test

class Stage16MoneyInputUnitTest {

    @Test
    fun classifyMoneyInput_nullIsEmpty() {
        assertEquals(MoneyInput.Empty, classifyMoneyInput(null))
    }

    @Test
    fun classifyMoneyInput_emptyStringIsEmpty() {
        assertEquals(MoneyInput.Empty, classifyMoneyInput(""))
    }

    @Test
    fun classifyMoneyInput_whitespaceIsEmpty() {
        assertEquals(MoneyInput.Empty, classifyMoneyInput("   "))
    }

    @Test
    fun classifyMoneyInput_wholeNumberIsValid() {
        assertEquals(MoneyInput.Valid(2500L), classifyMoneyInput("25"))
    }

    @Test
    fun classifyMoneyInput_decimalIsValid() {
        assertEquals(MoneyInput.Valid(2550L), classifyMoneyInput("25.5"))
    }

    @Test
    fun classifyMoneyInput_threeDecimalsIsInvalid() {
        assertEquals(MoneyInput.Invalid, classifyMoneyInput("10.125"))
    }

    @Test
    fun classifyMoneyInput_nonNumericIsInvalid() {
        assertEquals(MoneyInput.Invalid, classifyMoneyInput("abc"))
    }

    @Test
    fun classifyMoneyInput_negativeIsInvalid() {
        assertEquals(MoneyInput.Invalid, classifyMoneyInput("-5"))
    }

    @Test
    fun classifyMoneyInput_mixedSeparatorsIsInvalid() {
        assertEquals(MoneyInput.Invalid, classifyMoneyInput("1.250,50"))
    }
}
