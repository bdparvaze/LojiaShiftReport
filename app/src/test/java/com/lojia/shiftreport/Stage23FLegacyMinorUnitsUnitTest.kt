package com.lojia.shiftreport

import com.lojia.shiftreport.data.AppDatabase
import com.lojia.shiftreport.util.MoneyFormat
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Test

class Stage23FLegacyMinorUnitsUnitTest {

    @Test
    fun legacyMajorToMinor_threeDigits() {
        assertEquals(1500L, MoneyFormat.majorDoubleToMinor(1.5, 3))
    }

    @Test
    fun legacyMajorToMinor_zeroDigits() {
        assertEquals(1000L, MoneyFormat.majorDoubleToMinor(1000.0, 0))
    }

    @Test
    fun legacyArraySingleAmount_threeDigits() {
        val migrated = AppDatabase.migrateJsonArraySingleAmount("""[{"amount":1.5}]""", 3)
        val arr = JSONArray(migrated)
        assertEquals(1500L, arr.getJSONObject(0).getLong("amount"))
    }
}
