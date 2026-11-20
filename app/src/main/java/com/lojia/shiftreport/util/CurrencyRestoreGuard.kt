package com.lojia.shiftreport.util

import java.util.Currency
import java.util.Locale

sealed interface CurrencyRestoreDecision {
    data object Allow : CurrencyRestoreDecision
    data class RejectMismatch(val backupCode: String, val localCode: String) : CurrencyRestoreDecision
    data object RejectInvalid : CurrencyRestoreDecision
}

class BackupCurrencyMismatchException(
    val backupCode: String,
    val localCode: String
) : Exception("currency_mismatch:$backupCode:$localCode")

class BackupCurrencyInvalidException : Exception("currency_invalid")

fun decideCurrencyRestore(
    localCode: String,
    localLocked: Boolean,
    backupCode: String?
): CurrencyRestoreDecision {
    val normalizedBackup = backupCode?.trim()?.uppercase(Locale.US)
    if (normalizedBackup.isNullOrBlank()) {
        return CurrencyRestoreDecision.RejectInvalid
    }
    try {
        Currency.getInstance(normalizedBackup)
    } catch (_: IllegalArgumentException) {
        return CurrencyRestoreDecision.RejectInvalid
    }
    if (!localLocked) {
        return CurrencyRestoreDecision.Allow
    }
    val normalizedLocal = localCode.trim().uppercase(Locale.US)
    return if (normalizedBackup != normalizedLocal) {
        CurrencyRestoreDecision.RejectMismatch(
            backupCode = normalizedBackup,
            localCode = normalizedLocal
        )
    } else {
        CurrencyRestoreDecision.Allow
    }
}
