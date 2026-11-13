package com.lojia.shiftreport

import com.lojia.shiftreport.util.isPinUsable
import com.lojia.shiftreport.util.mapShiftReportCashierId
import com.lojia.shiftreport.util.matchLocalIdByName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Stage7FollowUpUnitTest {

    @Test
    fun isPinUsable_rejectsNullEmptyAndWhitespace_acceptsNonBlank() {
        assertFalse(isPinUsable(null))
        assertFalse(isPinUsable(""))
        assertFalse(isPinUsable("   "))
        assertTrue(isPinUsable("1234"))
        assertTrue(isPinUsable("123456"))
    }

    @Test
    fun matchLocalIdByName_and_mapShiftReportCashierId_remapsMatchedAndClearsUnmapped() {
        val localIdToName = mapOf(
            3 to "Ali",
            2 to "Sara"
        )

        val matchedAliId = matchLocalIdByName("  ali ", localIdToName)
        val matchedSaraId = matchLocalIdByName("Sara", localIdToName)
        val unmatchedNadiaId = matchLocalIdByName("Nadia", localIdToName)

        assertEquals(3, matchedAliId)
        assertEquals(2, matchedSaraId)
        assertNull(unmatchedNadiaId)

        val cashierIdMap = mapOf(
            7 to matchedAliId!!,
            2 to matchedSaraId!!
        )

        assertEquals("3", mapShiftReportCashierId("7", cashierIdMap))
        assertEquals("2", mapShiftReportCashierId("2", cashierIdMap))
        assertEquals("", mapShiftReportCashierId("99", cashierIdMap))
        assertEquals("", mapShiftReportCashierId("", cashierIdMap))
        assertEquals("", mapShiftReportCashierId("   ", cashierIdMap))
        assertEquals("", mapShiftReportCashierId("cashier_abc", cashierIdMap))
    }

    @Test
    fun mapShiftReportCashierId_emptyMap_returnsEmptyString() {
        assertEquals("", mapShiftReportCashierId("7", emptyMap()))
        assertEquals("", mapShiftReportCashierId("", emptyMap()))
    }
}
