package com.lojia.shiftreport

import com.lojia.shiftreport.util.vatHeaderLines
import com.lojia.shiftreport.util.vatNumberForPrint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Stage22VatRemovalUnitTest {

    @Test
    fun vatNumberForPrint_returnsTrimmedValueWhenSet() {
        assertEquals("300123456789003", vatNumberForPrint("  300123456789003  "))
    }

    @Test
    fun vatNumberForPrint_returnsNullForEmptyString() {
        assertNull(vatNumberForPrint(""))
    }

    @Test
    fun vatNumberForPrint_returnsNullForBlankSpaces() {
        assertNull(vatNumberForPrint("   "))
    }

    @Test
    fun vatHeaderLines_returnsEmptyListForNull() {
        assertEquals(emptyList<String>(), vatHeaderLines(null))
    }

    @Test
    fun vatHeaderLines_returnsOneLineForNonBlankNumber() {
        val lines = vatHeaderLines("  310000123400003 ")
        assertEquals(1, lines.size)
        assertTrue(lines[0].contains("310000123400003"))
        assertEquals("310000123400003", lines[0])
    }
}
