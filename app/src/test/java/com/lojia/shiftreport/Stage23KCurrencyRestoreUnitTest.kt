package com.lojia.shiftreport

import com.lojia.shiftreport.util.CurrencyRestoreDecision
import com.lojia.shiftreport.util.decideCurrencyRestore
import org.junit.Assert.assertEquals
import org.junit.Test

class Stage23KCurrencyRestoreUnitTest {

    @Test
    fun unlockedLocal_validBackupCurrency_returnsAllow() {
        val result = decideCurrencyRestore(
            localCode = "USD",
            localLocked = false,
            backupCode = "KWD"
        )
        assertEquals(CurrencyRestoreDecision.Allow, result)
    }

    @Test
    fun lockedLocal_differentBackupCurrency_returnsRejectMismatch() {
        val result = decideCurrencyRestore(
            localCode = "USD",
            localLocked = true,
            backupCode = "SAR"
        )
        assertEquals(CurrencyRestoreDecision.RejectMismatch("SAR", "USD"), result)
    }

    @Test
    fun lockedLocal_matchingCurrencyWithCaseAndWhitespace_returnsAllow() {
        val result = decideCurrencyRestore(
            localCode = "KWD",
            localLocked = true,
            backupCode = " kwd "
        )
        assertEquals(CurrencyRestoreDecision.Allow, result)
    }

    @Test
    fun invalidOrMissingBackupCurrency_returnsRejectInvalid() {
        assertEquals(
            CurrencyRestoreDecision.RejectInvalid,
            decideCurrencyRestore(localCode = "USD", localLocked = false, backupCode = null)
        )
        assertEquals(
            CurrencyRestoreDecision.RejectInvalid,
            decideCurrencyRestore(localCode = "USD", localLocked = true, backupCode = "   ")
        )
        // "ZZZ" is a 3-letter code rejected by java.util.Currency.getInstance
        assertEquals(
            CurrencyRestoreDecision.RejectInvalid,
            decideCurrencyRestore(localCode = "USD", localLocked = false, backupCode = "ZZZ")
        )
    }
}
