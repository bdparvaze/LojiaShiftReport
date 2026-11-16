package com.lojia.shiftreport

import com.lojia.shiftreport.util.MoneyFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class Stage22BNetSalesUnitTest {

    @Test
    fun netSales_isNegativeWhenDeductionsExceedGross() {
        val net = MoneyFormat.calculateNetSalesMinor(
            grossSalesMinor = 5000L,
            totalDiscountsMinor = 4000L,
            salesReturnsMinor = 3500L
        )
        assertEquals(-2500L, net)
    }

    @Test
    fun netSales_isZeroWhenGrossEqualsDeductions() {
        val net = MoneyFormat.calculateNetSalesMinor(
            grossSalesMinor = 10000L,
            totalDiscountsMinor = 6000L,
            salesReturnsMinor = 4000L
        )
        assertEquals(0L, net)
    }

    @Test
    fun netSales_isUnchangedWhenGrossExceedsDeductions() {
        val net = MoneyFormat.calculateNetSalesMinor(
            grossSalesMinor = 30000L,
            totalDiscountsMinor = 2000L,
            salesReturnsMinor = 1000L
        )
        assertEquals(27000L, net)
    }

    @Test
    fun negativeNetSales_formatsWithMinusSignViaMoneyFormat() {
        val net = MoneyFormat.calculateNetSalesMinor(
            grossSalesMinor = 1000L,
            totalDiscountsMinor = 4000L,
            salesReturnsMinor = 2000L
        )
        val formatted = MoneyFormat.formatMinor(net, "USD", Locale.US)
        assertTrue("Expected minus sign in formatted string: $formatted", formatted.contains("-"))
        assertTrue("Expected formatted value in string: $formatted", formatted.contains("50.00"))
    }
}
