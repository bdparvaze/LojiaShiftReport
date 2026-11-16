package com.lojia.shiftreport

import com.lojia.shiftreport.printer.BluetoothPrinterManager
import com.lojia.shiftreport.printer.EscPosNativeProtocol
import com.lojia.shiftreport.util.paperWidthDots
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Stage 13 Unit Test: Verifies printer paper-width dot mapping and
 * pure Bluetooth print fallback outcome reporting.
 */
class Stage13PrintingUnitTest {

    @Test
    fun paperWidthDots_mapsSupportedAndFallbackWidths() {
        assertEquals(576, paperWidthDots(80))
        assertEquals(384, paperWidthDots(58))
        assertEquals(384, paperWidthDots(null))
        assertEquals(384, paperWidthDots(0))
    }

    @Test
    fun resolvePrintOutcome_distinguishesFullSuccessFallbackAndFailure() {
        val fullSuccess = BluetoothPrinterManager.resolvePrintOutcome(usedFallback = false)
        assertTrue(fullSuccess.isSuccess)
        assertEquals(true, fullSuccess.getOrNull())

        val fallback = BluetoothPrinterManager.resolvePrintOutcome(usedFallback = true)
        assertTrue(fallback.isSuccess)
        assertEquals(false, fallback.getOrNull())

        val failure = BluetoothPrinterManager.resolvePrintOutcome(
            usedFallback = true,
            error = IllegalStateException("IO failed")
        )
        assertTrue(failure.isFailure)
    }

    @Test
    fun encodeFormattedTextWithStatus_reportsFallbackWhenBitmapFails() {
        val normal = EscPosNativeProtocol.encodeFormattedTextWithStatus(
            text = "[C]টেস্ট",
            charsPerLine = 32,
            widthPx = 384,
            bitmapLineRasterizer = { _, _ -> byteArrayOf(0x01, 0x02) }
        )
        assertFalse(normal.usedTextFallback)

        val fallback = EscPosNativeProtocol.encodeFormattedTextWithStatus(
            text = "[C]টেস্ট",
            charsPerLine = 32,
            widthPx = 384,
            bitmapLineRasterizer = { _, _ -> throw RuntimeException("Bitmap render error") }
        )
        assertTrue(fallback.usedTextFallback)
    }
}
