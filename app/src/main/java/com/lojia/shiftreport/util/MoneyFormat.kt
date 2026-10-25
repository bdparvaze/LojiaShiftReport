package com.lojia.shiftreport.util

import com.lojia.shiftreport.data.AppCountry
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Single source of truth for currency and monetary amount formatting.
 *
 * Adheres to ISO 4217 currency standard with locale-aware number formatting.
 * Default store currency is ISO 4217 "USD" ($).
 */
object MoneyFormat {
    const val DEFAULT_CURRENCY_CODE = AppConstants.DEFAULT_CURRENCY_CODE
    const val DEFAULT_CURRENCY_SYMBOL = AppConstants.DEFAULT_CURRENCY_SYMBOL

    /**
     * Converts major unit (e.g. Double 10.25) to minor unit (e.g. Long 1025)
     */
    fun toMinorUnits(major: Double): Long {
        return java.math.BigDecimal.valueOf(major)
            .multiply(java.math.BigDecimal.valueOf(100))
            .setScale(0, java.math.RoundingMode.HALF_UP)
            .toLong()
    }

    /**
     * Converts major BigDecimal unit to minor unit (e.g. Long 1025)
     */
    fun toMinorUnits(major: java.math.BigDecimal): Long {
        return major
            .multiply(java.math.BigDecimal.valueOf(100))
            .setScale(0, java.math.RoundingMode.HALF_UP)
            .toLong()
    }

    /**
     * Converts minor unit (e.g. Long 1025) to major unit (e.g. Double 10.25)
     */
    fun toMajorUnits(minor: Long): Double {
        return java.math.BigDecimal.valueOf(minor)
            .divide(java.math.BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP)
            .toDouble()
    }

    /**
     * Formats a minor Long value (e.g. 1025 cents) into a display string (e.g. "$10.25")
     */
    fun formatMinor(
        minor: Long,
        currencyCodeOrSymbol: String? = null,
        locale: Locale = Locale.getDefault()
    ): String {
        return format(toMajorUnits(minor), currencyCodeOrSymbol, locale)
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

    fun calculateVarianceMinor(actualCash: Long, expectedCash: Long): Long {
        return actualCash - expectedCash
    }

    fun calculateTaxMinor(amountMinor: Long, taxRatePercent: Double, isTaxInclusive: Boolean): Long {
        if (taxRatePercent <= 0.0 || amountMinor <= 0L) return 0L
        val amtBD = java.math.BigDecimal.valueOf(amountMinor)
        val rateBD = java.math.BigDecimal.valueOf(taxRatePercent)
        val hundredBD = java.math.BigDecimal.valueOf(100.0)

        val tax = if (isTaxInclusive) {
            val divisor = hundredBD.add(rateBD)
            amtBD.multiply(rateBD).divide(divisor, 4, java.math.RoundingMode.HALF_UP)
        } else {
            amtBD.multiply(rateBD).divide(hundredBD, 4, java.math.RoundingMode.HALF_UP)
        }
        return tax.setScale(0, java.math.RoundingMode.HALF_UP).toLong()
    }

    /**
     * Formats a monetary BigDecimal value with exact financial precision using CurrencyFormatter.
     */
    fun format(
        amount: java.math.BigDecimal,
        currencyCodeOrSymbol: String? = null,
        locale: Locale = Locale.getDefault()
    ): String {
        val curr = resolveCurrency(currencyCodeOrSymbol)
        return CurrencyFormatter.formatMoney(amount, curr)
    }

    /**
     * Exact 2-decimal financial rounding to prevent IEEE-754 floating point inaccuracies.
     */
    fun round2Decimals(value: Double): Double {
        return java.math.BigDecimal.valueOf(value)
            .setScale(2, java.math.RoundingMode.HALF_UP)
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
        val curr = resolveCurrency(currencyCodeOrSymbol)
        return CurrencyFormatter.formatMoney(amount, curr)
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
    fun resolveSymbol(currencyCodeOrSymbol: String?): String {
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
            val currency = Currency.getInstance(trimmed.uppercase())
            currency.getSymbol(Locale.getDefault())
        } catch (_: Exception) {
            trimmed
        }
    }
}
