package com.lojia.shiftreport

import com.lojia.shiftreport.util.MoneyFormat
import org.junit.Assert.assertEquals
import org.junit.Test

class Stage20CMoneyMigrationUnitTest {

    @Test
    fun majorDoubleToMinor_tenWithTwoDigits_returns1000() {
        assertEquals(1000L, MoneyFormat.majorDoubleToMinor(10.0, 2))
    }

    @Test
    fun majorDoubleToMinor_halfUpRounding_returns1013() {
        assertEquals(1013L, MoneyFormat.majorDoubleToMinor(10.125, 2))
    }

    @Test
    fun majorDoubleToMinor_noBinaryFloatError_returns29() {
        assertEquals(29L, MoneyFormat.majorDoubleToMinor(0.285, 2))
    }

    @Test
    fun majorDoubleToMinor_kwdThreeDigits_returns1234() {
        assertEquals(1234L, MoneyFormat.majorDoubleToMinor(1.234, 3))
    }

    @Test
    fun majorDoubleToMinor_jpyZeroDigits_returns1000() {
        assertEquals(1000L, MoneyFormat.majorDoubleToMinor(1000.0, 0))
    }

    @Test
    fun majorDoubleToMinor_negativeCorrection_returnsMinus5050() {
        assertEquals(-5050L, MoneyFormat.majorDoubleToMinor(-50.5, 2))
    }

    @Test(expected = IllegalArgumentException::class)
    fun majorDoubleToMinor_nanThrowsIllegalArgumentException() {
        MoneyFormat.majorDoubleToMinor(Double.NaN, 2)
    }
}
