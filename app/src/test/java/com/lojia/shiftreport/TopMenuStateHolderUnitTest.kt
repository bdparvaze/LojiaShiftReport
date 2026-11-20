package com.lojia.shiftreport

import com.lojia.shiftreport.ui.common.TopMenuStateHolder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TopMenuStateHolderUnitTest {

    @Test
    fun outsideTap_dismissesOpenMenu() {
        val state = TopMenuStateHolder()
        assertFalse(state.isOpen)

        assertTrue(state.open())
        assertTrue(state.isOpen)

        assertTrue(state.onDismissOutsideTap())
        assertFalse(state.isOpen)
    }

    @Test
    fun backPress_dismissesOpenMenu() {
        val state = TopMenuStateHolder()
        state.open()
        assertTrue(state.isOpen)

        assertTrue(state.onDismissBackPress())
        assertFalse(state.isOpen)
    }

    @Test
    fun selectingItem_dismissesOpenMenuAndRunsCallback() {
        val state = TopMenuStateHolder()
        state.open()
        var selectedItem = ""

        assertTrue(state.onSelectItem { selectedItem = "item_a" })
        assertFalse(state.isOpen)
        assertEquals("item_a", selectedItem)
    }

    @Test
    fun openingTwice_doesNotBreakState() {
        val state = TopMenuStateHolder()
        assertTrue(state.open())
        assertFalse(state.open())
        assertTrue(state.isOpen)

        assertTrue(state.onDismissOutsideTap())
        assertFalse(state.isOpen)
        assertFalse(state.onDismissBackPress())
    }
}
