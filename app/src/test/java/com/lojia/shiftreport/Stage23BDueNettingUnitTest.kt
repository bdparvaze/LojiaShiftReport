package com.lojia.shiftreport

import com.lojia.shiftreport.report.DueReceiptSummary
import com.lojia.shiftreport.util.DueNettingResult
import com.lojia.shiftreport.util.summarizeDueMinor
import org.junit.Assert.assertEquals
import org.junit.Test

class Stage23BDueNettingUnitTest {

    @Test
    fun remainingDue_isSignedPerReceipt() {
        val outstandingReceipt = DueReceiptSummary(
            receiptNo = "126486",
            initialDueMinor = 2500L,
            totalCollectedMinor = 0L
        )
        val overpaidReceipt = DueReceiptSummary(
            receiptNo = "724397",
            initialDueMinor = 0L,
            totalCollectedMinor = 3000L
        )

        assertEquals(2500L, outstandingReceipt.remainingDueMinor)
        assertEquals(-3000L, overpaidReceipt.remainingDueMinor)
    }

    @Test
    fun summarize_overpaymentOfOneReceiptDoesNotReduceAnotherOutstanding() {
        assertEquals(
            DueNettingResult(outstandingMinor = 2500L, extraCollectedMinor = 3000L),
            summarizeDueMinor(listOf(2500L, -3000L))
        )
    }

    @Test
    fun summarize_partialCollection() {
        assertEquals(
            DueNettingResult(outstandingMinor = 1500L, extraCollectedMinor = 0L),
            summarizeDueMinor(listOf(1500L))
        )
    }

    @Test
    fun summarize_allOverpaid_outstandingIsZero() {
        assertEquals(
            DueNettingResult(outstandingMinor = 0L, extraCollectedMinor = 3500L),
            summarizeDueMinor(listOf(-3000L, -500L))
        )
    }

    @Test
    fun summarize_emptyList_isZero() {
        assertEquals(
            DueNettingResult(outstandingMinor = 0L, extraCollectedMinor = 0L),
            summarizeDueMinor(emptyList())
        )
    }
}
