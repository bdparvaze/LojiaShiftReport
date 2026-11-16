package com.lojia.shiftreport

import com.lojia.shiftreport.data.canChangeCurrency
import com.lojia.shiftreport.util.CurrencyFormatter
import com.lojia.shiftreport.util.MoneyFormat
import com.lojia.shiftreport.util.MoneyInput
import com.lojia.shiftreport.util.classifyMoneyInput
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class Stage20BCurrencyUnitTest {

    @After
    fun tearDown() {
        MoneyFormat.activeDigits = 2
    }

    @Test
    fun minorUnitDigits_twoDecimalCurrencies() {
        assertEquals(2, MoneyFormat.minorUnitDigits("USD"))
        assertEquals(2, MoneyFormat.minorUnitDigits("SAR"))
        assertEquals(2, MoneyFormat.minorUnitDigits("BDT"))
    }

    @Test
    fun minorUnitDigits_threeDecimalCurrencies() {
        assertEquals(3, MoneyFormat.minorUnitDigits("KWD"))
        assertEquals(3, MoneyFormat.minorUnitDigits("BHD"))
        assertEquals(3, MoneyFormat.minorUnitDigits("OMR"))
    }

    @Test
    fun minorUnitDigits_zeroDecimalCurrency() {
        assertEquals(0, MoneyFormat.minorUnitDigits("JPY"))
    }

    @Test
    fun minorUnitDigits_unknownBlankNullDefaultToTwo() {
        assertEquals(2, MoneyFormat.minorUnitDigits("XYZ"))
        assertEquals(2, MoneyFormat.minorUnitDigits(""))
        assertEquals(2, MoneyFormat.minorUnitDigits(null))
    }

    @Test
    fun zeroDecimalCurrency_parseAndFormat() {
        assertEquals(100L, MoneyFormat.parseMoneyInput("100", 0))
        assertNull(MoneyFormat.parseMoneyInput("10.50", 0))
        assertNull(MoneyFormat.parseMoneyInput("10,50", 0))
        assertEquals(MoneyInput.Invalid, classifyMoneyInput("10.50", 0))
        assertEquals("100", MoneyFormat.toPlainDecimal(100L, 0))
        assertEquals("0", MoneyFormat.zeroPlaceholder(0))
    }

    @Test
    fun threeDecimalCurrency_parseAndFormat() {
        assertEquals(10500L, MoneyFormat.parseMoneyInput("10.500", 3))
        assertEquals(10500L, MoneyFormat.parseMoneyInput("10.5", 3))
        assertNull(MoneyFormat.parseMoneyInput("10.1234", 3))
        assertNull(MoneyFormat.parseMoneyInput("10,500", 3))
        assertEquals(MoneyInput.Valid(10500L), classifyMoneyInput("10.500", 3))
        assertEquals("10.500", MoneyFormat.toPlainDecimal(10500L, 3))
        assertEquals("0.000", MoneyFormat.zeroPlaceholder(3))
    }

    @Test
    fun currencyLock_blocksWhenFinancialDataExists() {
        assertTrue(canChangeCurrency(false))
        assertFalse(canChangeCurrency(true))
    }

    @Test
    fun currencyFormatter_respectsIsoDigits() {
        val kwd = CurrencyFormatter.formatMoney(10500L, "KWD", Locale.US)
        assertTrue("Expected 10.500 in KWD format, got $kwd", kwd.contains("10.500"))
        val jpy = CurrencyFormatter.formatMoney(100L, "JPY", Locale.US)
        assertTrue("Expected 100 without decimals in JPY format, got $jpy", jpy.contains("100") && !jpy.contains("."))
    }
}
