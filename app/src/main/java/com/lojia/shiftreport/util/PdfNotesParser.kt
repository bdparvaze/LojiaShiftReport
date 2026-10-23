package com.lojia.shiftreport.util

import com.lojia.shiftreport.data.ShiftReport

/**
 * Parses cash float, actual cash, and sanitized user notes from shift report text.
 */
object PdfNotesParser {

    private val STARTING_CASH_REGEX = Regex(
        """(?:Starting Cash|Starting Float|Float|Opening Cash)[:=]\s*([0-9]+(?:\.[0-9]+)?)""",
        RegexOption.IGNORE_CASE
    )

    private val ACTUAL_CASH_REGEX = Regex(
        """(?:Actual Cash Count|Actual Cash|Actual Count|Actual)[:=]\s*([0-9]+(?:\.[0-9]+)?)""",
        RegexOption.IGNORE_CASE
    )

    fun extractStartingCashFromNotes(report: ShiftReport): Double {
        if (report.openingCash > 0L) return MoneyFormat.toMajorUnits(report.openingCash)
        return STARTING_CASH_REGEX.find(report.notes)?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
    }

    fun extractStartingCashFromNotes(notes: String): Double? {
        return STARTING_CASH_REGEX.find(notes)?.groupValues?.get(1)?.toDoubleOrNull()
    }

    fun extractActualCashFromNotes(report: ShiftReport): Double? {
        if (report.closingCash > 0L) return MoneyFormat.toMajorUnits(report.closingCash)
        return ACTUAL_CASH_REGEX.find(report.notes)?.groupValues?.get(1)?.toDoubleOrNull()
    }

    fun extractActualCashFromNotes(notes: String): Double? {
        return ACTUAL_CASH_REGEX.find(notes)?.groupValues?.get(1)?.toDoubleOrNull()
    }

    fun extractStartingCashMinor(report: ShiftReport): Long {
        if (report.openingCash > 0L) return report.openingCash
        val doubleVal = STARTING_CASH_REGEX.find(report.notes)?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
        return MoneyFormat.toMinorUnits(doubleVal)
    }

    fun extractStartingCashMinor(notes: String): Long? {
        val doubleVal = STARTING_CASH_REGEX.find(notes)?.groupValues?.get(1)?.toDoubleOrNull() ?: return null
        return MoneyFormat.toMinorUnits(doubleVal)
    }

    fun extractActualCashMinor(report: ShiftReport): Long? {
        if (report.closingCash > 0L) return report.closingCash
        val doubleVal = ACTUAL_CASH_REGEX.find(report.notes)?.groupValues?.get(1)?.toDoubleOrNull() ?: return null
        return MoneyFormat.toMinorUnits(doubleVal)
    }

    fun extractActualCashMinor(notes: String): Long? {
        val doubleVal = ACTUAL_CASH_REGEX.find(notes)?.groupValues?.get(1)?.toDoubleOrNull() ?: return null
        return MoneyFormat.toMinorUnits(doubleVal)
    }

    fun cleanDisplayNotes(notes: String): String {
        return notes
            .replace(STARTING_CASH_REGEX, "")
            .replace(ACTUAL_CASH_REGEX, "")
            .split("|")
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.equals("Saved from Shift Closing Ledger", ignoreCase = true) }
            .joinToString(" | ")
    }
}
