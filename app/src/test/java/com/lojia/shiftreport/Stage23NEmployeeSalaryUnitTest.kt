package com.lojia.shiftreport

import com.lojia.shiftreport.util.MoneyFormat
import com.lojia.shiftreport.util.MoneyInput
import com.lojia.shiftreport.util.isValidMonthlySalary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Stage23NEmployeeSalaryUnitTest {

    @Test
    fun zeroSalary_isRejected() {
        assertFalse(isValidMonthlySalary(0L))
    }

    @Test
    fun positiveSalary_isAccepted() {
        assertTrue(isValidMonthlySalary(1L))
    }

    @Test
    fun zeroTypedInput_isBlocked() {
        val twoDigits = MoneyFormat.minorUnitDigits("USD")
        val threeDigits = MoneyFormat.minorUnitDigits("KWD")

        val zeroTwoDigit = MoneyFormat.classifyMoneyInput("0", twoDigits)
        val zeroThreeDigit = MoneyFormat.classifyMoneyInput("0.000", threeDigits)
        val emptyInput = MoneyFormat.classifyMoneyInput("", twoDigits)

        assertEquals(MoneyInput.Valid(0L), zeroTwoDigit)
        assertEquals(MoneyInput.Valid(0L), zeroThreeDigit)
        assertFalse(isValidMonthlySalary((zeroTwoDigit as MoneyInput.Valid).minor))
        assertFalse(isValidMonthlySalary((zeroThreeDigit as MoneyInput.Valid).minor))
        assertEquals(MoneyInput.Empty, emptyInput)
    }
}
