package com.lojia.shiftreport

import com.lojia.shiftreport.data.AppCountry
import com.lojia.shiftreport.data.BusinessProfile
import com.lojia.shiftreport.data.defaultCountryDisplayName
import com.lojia.shiftreport.util.AppConstants
import org.junit.Assert.assertEquals
import org.junit.Test

class Stage23LCountryDefaultUnitTest {

    @Test
    fun defaultCountryDisplayName_matchesDefaultCountryCode() {
        assertEquals(
            AppCountry.fromCode(AppConstants.DEFAULT_COUNTRY_CODE).displayNameEn,
            defaultCountryDisplayName()
        )
    }

    @Test
    fun businessProfileDefault_usesDefaultCountry() {
        assertEquals(
            defaultCountryDisplayName(),
            BusinessProfile().country
        )
    }
}
