package com.lojia.shiftreport

import com.lojia.shiftreport.util.HeaderMetaLabelKind
import com.lojia.shiftreport.util.chooseHeaderMetaLabelKind
import com.lojia.shiftreport.util.vatNumberForPrint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Stage 4 & 4B Unit Test: Verifies VAT printing normalization and label selection.
 */
class Stage4SafetyUnitTest {

    @Test
    fun vatNumberForPrint_null_returnsNull() {
        assertNull(vatNumberForPrint(null))
    }

    @Test
    fun vatNumberForPrint_empty_returnsNull() {
        assertNull(vatNumberForPrint(""))
    }

    @Test
    fun vatNumberForPrint_whitespaceOnly_returnsNull() {
        assertNull(vatNumberForPrint("   "))
    }

    @Test
    fun vatNumberForPrint_paddedNumber_returnsTrimmed() {
        assertEquals("300123456789003", vatNumberForPrint(" 300123456789003 "))
    }

    @Test
    fun chooseHeaderMetaLabelKind_bothPresent_returnsVatAndTel() {
        assertEquals(
            HeaderMetaLabelKind.VAT_AND_TEL,
            chooseHeaderMetaLabelKind("300123456789003", "+966500000000")
        )
    }

    @Test
    fun chooseHeaderMetaLabelKind_onlyVat_returnsVatOnly() {
        assertEquals(
            HeaderMetaLabelKind.VAT_ONLY,
            chooseHeaderMetaLabelKind("300123456789003", "   ")
        )
    }

    @Test
    fun chooseHeaderMetaLabelKind_onlyPhone_returnsTelOnly() {
        assertEquals(
            HeaderMetaLabelKind.TEL_ONLY,
            chooseHeaderMetaLabelKind("   ", "+966500000000")
        )
    }

    @Test
    fun chooseHeaderMetaLabelKind_neitherPresent_returnsNone() {
        assertEquals(
            HeaderMetaLabelKind.NONE,
            chooseHeaderMetaLabelKind(null, "")
        )
    }
}
