package com.lojia.shiftreport

import com.lojia.shiftreport.util.MoneyFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class Stage15LocaleInputUnitTest {

    @Test
    fun parseMoneyInput_integerString() {
        assertEquals(2500L, MoneyFormat.parseMoneyInput("25"))
    }

    @Test
    fun parseMoneyInput_dotDecimal() {
        assertEquals(2550L, MoneyFormat.parseMoneyInput("25.5"))
    }

    @Test
    fun parseMoneyInput_commaDecimal() {
        assertEquals(2550L, MoneyFormat.parseMoneyInput("25,5"))
    }

    @Test
    fun parseMoneyInput_commaThousandsSeparator() {
        assertEquals(125000L, MoneyFormat.parseMoneyInput("1,250"))
    }

    @Test
    fun parseMoneyInput_commaThousandsAndDotDecimal() {
        assertEquals(125050L, MoneyFormat.parseMoneyInput("1,250.50"))
    }

    @Test
    fun parseMoneyInput_dotAndCommaReturnsNull() {
        assertNull(MoneyFormat.parseMoneyInput("1.250,50"))
    }

    @Test
    fun parseMoneyInput_arabicIndicDigitsAndDecimalSeparator() {
        assertEquals(1250L, MoneyFormat.parseMoneyInput("\u0661\u0662\u066B\u0665\u0660"))
    }

    @Test
    fun parseMoneyInput_bengaliDigitsAndDotDecimal() {
        assertEquals(1250L, MoneyFormat.parseMoneyInput("\u09E7\u09E8.\u09EB\u09E6"))
    }

    @Test
    fun parseMoneyInput_emptyReturnsNull() {
        assertNull(MoneyFormat.parseMoneyInput(""))
    }

    @Test
    fun parseMoneyInput_nullReturnsNull() {
        assertNull(MoneyFormat.parseMoneyInput(null))
    }

    @Test
    fun parseMoneyInput_nonNumericReturnsNull() {
        assertNull(MoneyFormat.parseMoneyInput("abc"))
    }

    @Test
    fun parseMoneyInput_negativeReturnsNull() {
        assertNull(MoneyFormat.parseMoneyInput("-5"))
    }

    @Test
    fun parseMoneyInput_threeDecimalDigitsReturnsNull() {
        assertNull(MoneyFormat.parseMoneyInput("1.234"))
    }
}
