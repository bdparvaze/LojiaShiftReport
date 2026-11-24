package com.lojia.shiftreport.report

internal fun normalizeOpenShiftCashier(cashierName: String): String? =
    cashierName.trim().takeIf { it.isNotBlank() }

internal fun canConfirmOpenShift(isFloatError: Boolean, cashierName: String): Boolean =
    !isFloatError && cashierName.isNotBlank()
