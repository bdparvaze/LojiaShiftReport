package com.lojia.shiftreport.printer

import org.junit.Assert.*
import org.junit.Test

class ThermalBitmapRendererTest {

    @Test
    fun testContainsNonLatin() {
        assertFalse(ThermalBitmapRenderer.containsNonLatin("123 ABC $15.00"))
        assertTrue(ThermalBitmapRenderer.containsNonLatin("নগদ বিক্রয়")) // Bengali
        assertTrue(ThermalBitmapRenderer.containsNonLatin("إجمالي المبيعات")) // Arabic
    }

    @Test
    fun testContainsArabic() {
        assertFalse(ThermalBitmapRenderer.containsArabic("Hello World"))
        assertFalse(ThermalBitmapRenderer.containsArabic("বাংলা"))
        assertTrue(ThermalBitmapRenderer.containsArabic("فاتورة ضريبية"))
    }

    @Test
    fun testSplitColumns() {
        val twoCol = ThermalBitmapRenderer.splitColumns("[L]Subtotal[R]$100.00")
        assertEquals("Subtotal", twoCol.left)
        assertEquals("$100.00", twoCol.right)
        assertNull(twoCol.center)

        val threeCol = ThermalBitmapRenderer.splitColumns("[L]Item[C]x2[R]$50.00")
        assertEquals("Item", threeCol.left)
        assertEquals("x2", threeCol.center)
        assertEquals("$50.00", threeCol.right)

        val centerOnly = ThermalBitmapRenderer.splitColumns("[C]THANK YOU")
        assertEquals("THANK YOU", centerOnly.center)
        assertTrue(centerOnly.isCenterOnly)

        val rightOnly = ThermalBitmapRenderer.splitColumns("[R]Page 1")
        assertEquals("Page 1", rightOnly.right)
        assertTrue(rightOnly.isRightOnly)

        val plainLine = ThermalBitmapRenderer.splitColumns("Regular Line")
        assertEquals("Regular Line", plainLine.left)
    }
}
