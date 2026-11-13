package com.lojia.shiftreport

import com.lojia.shiftreport.util.mapShiftReportCashierId
import com.lojia.shiftreport.util.matchLocalIdByName
import com.lojia.shiftreport.util.normalizeRestoreName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class Stage8RestoreIdentityUnitTest {

    @Test
    fun normalizeRestoreName_trimsAndLowercases_handlesNull() {
        assertEquals("ali", normalizeRestoreName("  Ali "))
        assertEquals("", normalizeRestoreName(null))
    }

    @Test
    fun matchLocalIdByName_sameIdDifferentName_returnsNull() {
        assertNull(matchLocalIdByName("ali", mapOf(1 to "Omar")))
    }

    @Test
    fun matchLocalIdByName_caseInsensitiveNameMatch_returnsLocalId() {
        assertEquals(3, matchLocalIdByName("ALI", mapOf(3 to "Ali")))
    }

    @Test
    fun matchLocalIdByName_blankNameNeverMatches() {
        assertNull(matchLocalIdByName("", mapOf(3 to "")))
    }

    @Test
    fun mapShiftReportCashierId_remapsMappedAndClearsInvalidOrUnmapped() {
        assertEquals("7", mapShiftReportCashierId("1", mapOf(1 to 7)))
        assertEquals("", mapShiftReportCashierId("1", emptyMap()))
        assertEquals("", mapShiftReportCashierId("", mapOf(1 to 7)))
        assertEquals("", mapShiftReportCashierId("abc", mapOf(1 to 7)))
    }
}
