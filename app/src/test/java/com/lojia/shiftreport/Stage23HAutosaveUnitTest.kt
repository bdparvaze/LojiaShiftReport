package com.lojia.shiftreport

import com.lojia.shiftreport.util.shouldWriteDraft
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Stage23HAutosaveUnitTest {

    @Test
    fun shouldWriteDraft_emptyForm_false() {
        assertFalse(shouldWriteDraft(isEmpty = true, snapshotChanged = true))
    }

    @Test
    fun shouldWriteDraft_unchanged_false() {
        assertFalse(shouldWriteDraft(isEmpty = false, snapshotChanged = false))
    }

    @Test
    fun shouldWriteDraft_filledAndChanged_true() {
        assertTrue(shouldWriteDraft(isEmpty = false, snapshotChanged = true))
    }
}
