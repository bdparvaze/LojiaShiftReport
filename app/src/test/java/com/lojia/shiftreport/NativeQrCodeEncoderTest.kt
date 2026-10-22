package com.lojia.shiftreport

import com.lojia.shiftreport.util.NativeQrCodeEncoder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeQrCodeEncoderTest {

    @Test
    fun testQrEncodingBasicString() {
        val text = "LOJIA-SHIFT-REPORT-TEST-12345"
        val matrix = NativeQrCodeEncoder.encode(text, NativeQrCodeEncoder.EcLevel.M)
        assertNotNull(matrix)
        assertTrue(matrix.width >= 21)
        assertEquals(matrix.width, matrix.height)

        // Verify top-left finder pattern center (row 3, col 3) is true
        assertTrue(matrix.get(3, 3))
    }

    @Test
    fun testQrEncodingLongUtf8String() {
        val text = "Shift Report | Lojia Store | Date: 2026-10-21 | Total Sales: $12,450.00 | VAT: 15% | Cashier: Ahmed"
        val matrix = NativeQrCodeEncoder.encode(text, NativeQrCodeEncoder.EcLevel.H)
        assertNotNull(matrix)
        assertTrue(matrix.width > 21)
        assertEquals(matrix.width, matrix.height)
    }
}
