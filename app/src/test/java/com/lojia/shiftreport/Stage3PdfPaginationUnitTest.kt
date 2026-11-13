package com.lojia.shiftreport

import com.lojia.shiftreport.util.needsNewPage
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Stage 3 Unit Test: Verifies the pure PDF pagination boundary helper needsNewPage.
 */
class Stage3PdfPaginationUnitTest {

    @Test
    fun stage3_caseA_plentyOfSpace_returnsFalse() {
        assertFalse(needsNewPage(currentY = 100f, requiredHeight = 20f, pageBottomLimit = 800f))
    }

    @Test
    fun stage3_caseB_exceedsBottomLimit_returnsTrue() {
        assertTrue(needsNewPage(currentY = 790f, requiredHeight = 20f, pageBottomLimit = 800f))
    }

    @Test
    fun stage3_caseC_exactBoundary_returnsFalse() {
        assertFalse(needsNewPage(currentY = 780f, requiredHeight = 20f, pageBottomLimit = 800f))
    }

    @Test
    fun stage3_caseD_oneUnitPastBoundary_returnsTrue() {
        assertTrue(needsNewPage(currentY = 781f, requiredHeight = 20f, pageBottomLimit = 800f))
    }
}
