package com.lojia.shiftreport

import com.lojia.shiftreport.data.AppCountry
import com.lojia.shiftreport.util.AppConstants
import com.lojia.shiftreport.util.MoneyFormat
import org.junit.Assert.assertEquals
import org.junit.Test

class Stage23EHardcodingUnitTest {

    @Test
    fun country_nullCode_returnsDefaultCountry() {
        assertEquals(AppConstants.DEFAULT_COUNTRY_CODE, AppCountry.fromCode(null).code)
        assertEquals(AppConstants.DEFAULT_COUNTRY_CODE, AppCountry.fromCode("").code)
        assertEquals(AppConstants.DEFAULT_COUNTRY_CODE, AppCountry.fromCode("ZZ").code)
    }

    @Test
    fun minorDigits_blankCode_usesIsoDefault() {
        assertEquals(AppConstants.ISO_DEFAULT_MINOR_DIGITS, MoneyFormat.minorUnitDigits(null))
        assertEquals(AppConstants.ISO_DEFAULT_MINOR_DIGITS, MoneyFormat.minorUnitDigits(""))
        assertEquals(AppConstants.ISO_DEFAULT_MINOR_DIGITS, MoneyFormat.minorUnitDigits("   "))
        assertEquals(AppConstants.ISO_DEFAULT_MINOR_DIGITS, MoneyFormat.minorUnitDigits("XYZ"))
    }

    @Test
    fun minorDigits_isoExceptions() {
        assertEquals(3, MoneyFormat.minorUnitDigits("KWD"))
        assertEquals(3, MoneyFormat.minorUnitDigits("BHD"))
        assertEquals(3, MoneyFormat.minorUnitDigits("OMR"))
        assertEquals(0, MoneyFormat.minorUnitDigits("JPY"))
        assertEquals(2, MoneyFormat.minorUnitDigits("USD"))
        assertEquals(2, MoneyFormat.minorUnitDigits("SAR"))
        assertEquals(2, MoneyFormat.minorUnitDigits("BDT"))
    }
}
