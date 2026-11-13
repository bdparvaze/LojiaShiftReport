package com.lojia.shiftreport

import com.lojia.shiftreport.util.normalizeReceiptNumber
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Stage 5 Unit Test: Verifies receipt number normalization prevents doubled "#" or "Receipt" prefixes.
 */
class Stage5ReceiptLabelUnitTest {

    @Test
    fun normalizeReceiptNumber_leadingHash_stripsHash() {
        assertEquals("12", normalizeReceiptNumber("#12"))
    }

    @Test
    fun normalizeReceiptNumber_receiptAndHash_stripsBoth() {
        assertEquals("12", normalizeReceiptNumber("Receipt #12"))
    }

    @Test
    fun normalizeReceiptNumber_receiptAndDoubleHash_stripsAll() {
        assertEquals("12", normalizeReceiptNumber("Receipt ##12"))
    }

    @Test
    fun normalizeReceiptNumber_paddedHashAndSpaces_stripsSpacesAndHash() {
        assertEquals("12", normalizeReceiptNumber("  #  12 "))
    }

    @Test
    fun normalizeReceiptNumber_plainNumber_returnsUnchanged() {
        assertEquals("12", normalizeReceiptNumber("12"))
    }

    @Test
    fun normalizeReceiptNumber_null_returnsEmpty() {
        assertEquals("", normalizeReceiptNumber(null))
    }

    @Test
    fun normalizeReceiptNumber_onlyHash_returnsEmpty() {
        assertEquals("", normalizeReceiptNumber("#"))
    }

    @Test
    fun normalizeReceiptNumber_middleHash_preservesMiddleHash() {
        assertEquals("A#12", normalizeReceiptNumber("A#12"))
    }
}
