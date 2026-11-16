package com.lojia.shiftreport.util

import com.lojia.shiftreport.data.AppCountry
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

sealed class MoneyInput {
    object Empty : MoneyInput()
    data class Valid(val minor: Long) : MoneyInput()
    object Invalid : MoneyInput()
}

fun classifyMoneyInput(raw: String?, digits: Int = MoneyFormat.activeDigits): MoneyInput {
    if (raw.isNullOrBlank()) return MoneyInput.Empty
    val minor = MoneyFormat.parseMoneyInput(raw, digits)
    return if (minor != null) MoneyInput.Valid(minor) else MoneyInput.Invalid
}

fun toPlainDecimal(minor: Long, digits: Int = MoneyFormat.activeDigits): String =
    MoneyFormat.toPlainDecimal(minor, digits)

/**
 * Single source of truth for currency and monetary amount formatting.
 *
 * Adheres to ISO 4217 currency standard with locale-aware number formatting.
 * Default store currency is ISO 4217 "USD" ($).
 */
object MoneyFormat {
    const val DEFAULT_CURRENCY_CODE = AppConstants.DEFAULT_CURRENCY_CODE
    const val DEFAULT_CURRENCY_SYMBOL = AppConstants.DEFAULT_CURRENCY_SYMBOL

    @Volatile
    var activeDigits: Int = 2

    /**
     * Single source of truth for decimal places (minor unit digits) by ISO 4217 currency code.
     * KWD, BHD, OMR = 3; JPY = 0; blank/null or unknown = 2; others use ISO default.
     */
    fun minorUnitDigits(currencyCode: String?): Int {
        if (currencyCode.isNullOrBlank()) return 2
        return when (val upper = currencyCode.trim().uppercase(Locale.US)) {
            "KWD", "BHD", "OMR" -> 3
            "JPY" -> 0
            else -> try {
                val fractionDigits = Currency.getInstance(upper).defaultFractionDigits
                if (fractionDigits < 0) 2 else fractionDigits
            } catch (_: Exception) {
                2
            }
        }
    }

    /**
     * Returns 10^digits as the multiplier/divisor between major and minor units.
     */
    fun minorFactor(digits: Int = activeDigits): Long {
        val safe = digits.coerceAtLeast(0)
        return when (safe) {
            0 -> 1L
            1 -> 10L
            2 -> 100L
            3 -> 1000L
            4 -> 10000L
            else -> java.math.BigDecimal.TEN.pow(safe).toLong()
        }
    }

    fun zeroPlaceholder(digits: Int = activeDigits): String {
        val safe = digits.coerceAtLeast(0)
        return if (safe == 0) "0" else "0." + "0".repeat(safe)
    }

    internal fun resolveDigitsForFormatting(currencyCodeOrSymbol: String?): Int {
        if (currencyCodeOrSymbol.isNullOrBlank()) return activeDigits
        val trimmed = currencyCodeOrSymbol.trim()
        val matchedCountry = AppCountry.entries.find {
            it.currencyCode.equals(trimmed, ignoreCase = true) || it.currencySymbol == trimmed
        }
        if (matchedCountry != null) {
            return minorUnitDigits(matchedCountry.currencyCode)
        }
        if (trimmed.length == 3 && trimmed.all { it.isLetter() }) {
            return minorUnitDigits(trimmed)
        }
        return try {
            val d = Currency.getInstance(trimmed.uppercase(Locale.US)).defaultFractionDigits
            if (d < 0) activeDigits else d
        } catch (_: Exception) {
            activeDigits
        }
    }

    fun classifyMoneyInput(raw: String?, digits: Int = activeDigits): MoneyInput =
        com.lojia.shiftreport.util.classifyMoneyInput(raw, digits)

    /**
     * Converts major unit (e.g. Double 10.25) to minor unit (e.g. Long 1025)
     */
    fun toMinorUnits(major: Double, digits: Int = activeDigits): Long {
        return java.math.BigDecimal.valueOf(major)
            .multiply(java.math.BigDecimal.valueOf(minorFactor(digits)))
            .setScale(0, java.math.RoundingMode.HALF_UP)
            .toLong()
    }

    /**
     * Converts major BigDecimal unit to minor unit (e.g. Long 1025)
     */
    fun toMinorUnits(major: java.math.BigDecimal, digits: Int = activeDigits): Long {
        return major
            .multiply(java.math.BigDecimal.valueOf(minorFactor(digits)))
            .setScale(0, java.math.RoundingMode.HALF_UP)
            .toLong()
    }

    /**
     * Converts a major-unit Double (e.g. 10.125) to minor units (Long) using exact decimal scaling.
     */
    fun majorDoubleToMinor(major: Double, digits: Int): Long {
        if (major.isNaN() || major.isInfinite()) {
            throw IllegalArgumentException("Invalid monetary amount: $major")
        }
        return java.math.BigDecimal.valueOf(major)
            .movePointRight(digits)
            .setScale(0, java.math.RoundingMode.HALF_UP)
            .longValueExact()
    }

    /**
     * Converts minor unit (e.g. Long 1025) to major unit (e.g. Double 10.25)
     */
    fun toMajorUnits(minor: Long, digits: Int = activeDigits): Double {
        val safe = digits.coerceAtLeast(0)
        return java.math.BigDecimal.valueOf(minor)
            .divide(java.math.BigDecimal.valueOf(minorFactor(safe)), safe, java.math.RoundingMode.HALF_UP)
            .toDouble()
    }

    /**
     * Converts minor units to a locale-independent plain decimal string with [digits] decimal places,
     * no grouping separators, and no scientific notation.
     */
    fun toPlainDecimal(minor: Long, digits: Int = activeDigits): String {
        val safe = digits.coerceAtLeast(0)
        return java.math.BigDecimal.valueOf(minor)
            .divide(java.math.BigDecimal.valueOf(minorFactor(safe)), safe, java.math.RoundingMode.HALF_UP)
            .toPlainString()
    }

    private val INTEGER_INPUT_REGEX = Regex("^[0-9]+$")
    private val COMMA_THREE_DIGITS_ONLY_REGEX = Regex("^[0-9]+,[0-9]{3}$")

    /**
     * Parses typed monetary input across ASCII, Arabic-Indic, Eastern Arabic-Indic, and Bengali digits
     * with locale-independent rules and returns minor units, or null if invalid.
     */
    fun parseMoneyInput(raw: String?, digits: Int = activeDigits): Long? {
        if (raw.isNullOrBlank()) return null
        val safeDigits = digits.coerceAtLeast(0)

        val sb = StringBuilder(raw.length)
        for (ch in raw) {
            when (ch) {
                in '\u0660'..'\u0669' -> sb.append(('0'.code + (ch.code - '\u0660'.code)).toChar())
                in '\u06F0'..'\u06F9' -> sb.append(('0'.code + (ch.code - '\u06F0'.code)).toChar())
                in '\u09E6'..'\u09EF' -> sb.append(('0'.code + (ch.code - '\u09E6'.code)).toChar())
                '\u066B' -> sb.append('.')
                ' ', '\u066C' -> Unit
                else -> sb.append(ch)
            }
        }
        val cleaned = sb.toString().trim()
        if (cleaned.isEmpty()) return null

        if (safeDigits == 0) {
            if (cleaned.contains('.') || cleaned.contains(',')) return null
            if (!INTEGER_INPUT_REGEX.matches(cleaned)) return null
            return try {
                toMinorUnits(java.math.BigDecimal(cleaned), 0)
            } catch (_: NumberFormatException) {
                null
            }
        }

        val normalized = if (cleaned.contains('.')) {
            if (cleaned.contains(',') && cleaned.indexOf(',') > cleaned.indexOf('.')) {
                return null
            }
            cleaned.replace(",", "")
        } else if (cleaned.contains(',')) {
            if (safeDigits >= 3 && COMMA_THREE_DIGITS_ONLY_REGEX.matches(cleaned)) {
                return null
            }
            val lastComma = cleaned.lastIndexOf(',')
            val tail = cleaned.substring(lastComma + 1)
            val isDecimalComma = (tail.length == 1 || tail.length == 2) && tail.all { it in '0'..'9' }
            if (isDecimalComma) {
                cleaned.substring(0, lastComma).replace(",", "") + "." + tail
            } else {
                cleaned.replace(",", "")
            }
        } else {
            cleaned
        }

        val decimalRegex = Regex("^[0-9]+(\\.[0-9]{1,$safeDigits})?$")
        if (!decimalRegex.matches(normalized)) return null
        return try {
            toMinorUnits(java.math.BigDecimal(normalized), safeDigits)
        } catch (_: NumberFormatException) {
            null
        }
    }

    /**
     * Formats a minor Long value (e.g. 1025 cents) into a display string (e.g. "$10.25")
     */
    fun formatMinor(
        minor: Long,
        currencyCodeOrSymbol: String? = null,
        locale: Locale = Locale.getDefault()
    ): String {
        val digits = resolveDigitsForFormatting(currencyCodeOrSymbol)
        return CurrencyFormatter.formatMoney(minor, resolveCurrency(currencyCodeOrSymbol), locale, digits)
    }

    fun addMinor(vararg values: Long): Long {
        var total = 0L
        for (v in values) {
            total += v
        }
        return total
    }

    fun sumOfMinor(values: Iterable<Long>): Long {
        var total = 0L
        for (v in values) {
            total += v
        }
        return total
    }

    fun subtractMinor(a: Long, b: Long): Long {
        return a - b
    }

    fun multiplyMinor(minor: Long, factor: Double): Long {
        return java.math.BigDecimal.valueOf(minor)
            .multiply(java.math.BigDecimal.valueOf(factor))
            .setScale(0, java.math.RoundingMode.HALF_UP)
            .toLong()
    }

    fun calculateExpectedCashMinor(
        startingCash: Long,
        cashSales: Long,
        payIn: Long,
        payOut: Long,
        salesReturns: Long = 0L
    ): Long {
        return startingCash + cashSales + payIn - payOut - salesReturns
    }

    fun calculateExpectedCashMinor(
        totalCashIn: Long,
        totalCashOut: Long
    ): Long {
        return totalCashIn - totalCashOut
    }

    fun calculateVarianceMinor(actualCash: Long, expectedCash: Long): Long {
        return actualCash - expectedCash
    }

    fun calculateNetSalesMinor(
        grossSalesMinor: Long,
        totalDiscountsMinor: Long,
        salesReturnsMinor: Long
    ): Long {
        return subtractMinor(subtractMinor(grossSalesMinor, totalDiscountsMinor), salesReturnsMinor)
    }

    /**
     * Formats a monetary BigDecimal value with exact financial precision using CurrencyFormatter.
     */
    fun format(
        amount: java.math.BigDecimal,
        currencyCodeOrSymbol: String? = null,
        locale: Locale = Locale.getDefault()
    ): String {
        val digits = resolveDigitsForFormatting(currencyCodeOrSymbol)
        val curr = resolveCurrency(currencyCodeOrSymbol)
        return CurrencyFormatter.formatMoney(amount, curr, locale, digits)
    }

    /**
     * Exact financial rounding to active decimal precision to prevent IEEE-754 floating point inaccuracies.
     */
    fun round2Decimals(value: Double, digits: Int = activeDigits): Double {
        return java.math.BigDecimal.valueOf(value)
            .setScale(digits.coerceAtLeast(0), java.math.RoundingMode.HALF_UP)
            .toDouble()
    }

    /**
     * Formats a monetary double value into a locale-aware display string using CurrencyFormatter.
     */
    fun format(
        amount: Double,
        currencyCodeOrSymbol: String? = null,
        locale: Locale = Locale.getDefault()
    ): String {
        val digits = resolveDigitsForFormatting(currencyCodeOrSymbol)
        val curr = resolveCurrency(currencyCodeOrSymbol)
        return CurrencyFormatter.formatMoney(amount, curr, locale, digits)
    }

    /**
     * Resolves the ISO currency code or symbol string, falling back to DEFAULT_CURRENCY_CODE ("USD").
     */
    fun resolveCurrency(businessCurrency: String?): String {
        val trimmed = businessCurrency?.trim().orEmpty()
        return if (trimmed.isNotEmpty()) trimmed else DEFAULT_CURRENCY_CODE
    }

    /**
     * Resolves the display symbol for a given currency code.
     */
    fun resolveSymbol(currencyCodeOrSymbol: String?, locale: Locale = Locale.getDefault()): String {
        if (currencyCodeOrSymbol.isNullOrBlank()) return DEFAULT_CURRENCY_SYMBOL
        val trimmed = currencyCodeOrSymbol.trim()

        // 1. Check matching AppCountry
        val matchedCountry = AppCountry.entries.find {
            it.currencyCode.equals(trimmed, ignoreCase = true) || it.currencySymbol == trimmed
        }
        if (matchedCountry != null) {
            return matchedCountry.currencySymbol
        }

        // 2. Check Java ISO Currency
        return try {
            val currency = Currency.getInstance(trimmed.uppercase(Locale.US))
            currency.getSymbol(locale)
        } catch (_: Exception) {
            trimmed
        }
    }
}
