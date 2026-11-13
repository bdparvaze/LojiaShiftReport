package com.lojia.shiftreport

import com.lojia.shiftreport.util.isUnspecifiedReceipt
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Stage 10 Unit Test: Verifies the pure placeholder receipt detection rule
 * used to decide when to render localized receipt_unspecified without "#" or "Receipt #".
 */
class Stage10PlaceholderUnitTest {

    @Test
    fun isUnspecifiedReceipt_unspecifiedLiteral_returnsTrue() {
        assertTrue(isUnspecifiedReceipt("Unspecified"))
    }

    @Test
    fun isUnspecifiedReceipt_receiptNumber_returnsFalse() {
        assertFalse(isUnspecifiedReceipt("12"))
    }

    @Test
    fun isUnspecifiedReceipt_emptyString_returnsFalse() {
        assertFalse(isUnspecifiedReceipt(""))
    }
}
