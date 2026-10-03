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
    const val DEFAULT_CURRENCY_CODE = "USD"
    const val DEFAULT_CURRENCY_SYMBOL = "$"

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
     * Formats a monetary BigDecimal value with exact financial precision into a locale-aware display string.
     */
    fun format(
        amount: java.math.BigDecimal,
        currencyCodeOrSymbol: String? = null,
        locale: Locale = Locale.getDefault()
    ): String {
        val rounded = amount.setScale(2, java.math.RoundingMode.HALF_UP)
        return format(rounded.toDouble(), currencyCodeOrSymbol, locale)
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
     * Safely adds multiple monetary values using BigDecimal precision.
     */
    fun add(vararg values: Double): Double {
        var total = java.math.BigDecimal.ZERO
        for (v in values) {
            total = total.add(java.math.BigDecimal.valueOf(v))
        }
        return total.setScale(2, java.math.RoundingMode.HALF_UP).toDouble()
    }

    /**
     * Safely adds a collection of monetary values using BigDecimal precision.
     */
    fun sumOf(values: Iterable<Double>): Double {
        var total = java.math.BigDecimal.ZERO
        for (v in values) {
            total = total.add(java.math.BigDecimal.valueOf(v))
        }
        return total.setScale(2, java.math.RoundingMode.HALF_UP).toDouble()
    }

    /**
     * Safely subtracts b from a using BigDecimal precision.
     */
    fun subtract(a: Double, b: Double): Double {
        return java.math.BigDecimal.valueOf(a)
            .subtract(java.math.BigDecimal.valueOf(b))
            .setScale(2, java.math.RoundingMode.HALF_UP)
            .toDouble()
    }

    /**
     * Safely multiplies a by b using BigDecimal precision.
     */
    fun multiply(a: Double, b: Double): Double {
        return java.math.BigDecimal.valueOf(a)
            .multiply(java.math.BigDecimal.valueOf(b))
            .setScale(2, java.math.RoundingMode.HALF_UP)
            .toDouble()
    }

    /**
     * Safely calculates expected cash in drawer using BigDecimal arithmetic:
     * Expected Cash = Starting Cash + Cash Sales + Pay In - Pay Out - Sales Returns
     */
    fun calculateExpectedCash(
        startingCash: Double,
        cashSales: Double,
        payIn: Double,
        payOut: Double,
        salesReturns: Double = 0.0
    ): Double {
        val start = java.math.BigDecimal.valueOf(startingCash)
        val sales = java.math.BigDecimal.valueOf(cashSales)
        val inAmt = java.math.BigDecimal.valueOf(payIn)
        val outAmt = java.math.BigDecimal.valueOf(payOut)
        val returns = java.math.BigDecimal.valueOf(salesReturns)

        return start.add(sales).add(inAmt).subtract(outAmt).subtract(returns)
            .setScale(2, java.math.RoundingMode.HALF_UP)
            .toDouble()
    }

    /**
     * Safely calculates cash variance (discrepancy) using BigDecimal arithmetic:
     * Variance = Actual Cash - Expected Cash
     */
    fun calculateVariance(actualCash: Double, expectedCash: Double): Double {
        return java.math.BigDecimal.valueOf(actualCash)
            .subtract(java.math.BigDecimal.valueOf(expectedCash))
            .setScale(2, java.math.RoundingMode.HALF_UP)
            .toDouble()
    }

    /**
     * Safely calculates tax using BigDecimal precision and HALF_UP 2-decimal rounding.
     */
    fun calculateTax(amount: Double, taxRatePercent: Double, isTaxInclusive: Boolean): Double {
        if (taxRatePercent <= 0.0 || amount <= 0.0) return 0.0
        val amtBD = java.math.BigDecimal.valueOf(amount)
        val rateBD = java.math.BigDecimal.valueOf(taxRatePercent)
        val hundredBD = java.math.BigDecimal.valueOf(100.0)

        val tax = if (isTaxInclusive) {
            val divisor = hundredBD.add(rateBD)
            amtBD.multiply(rateBD).divide(divisor, 4, java.math.RoundingMode.HALF_UP)
        } else {
            amtBD.multiply(rateBD).divide(hundredBD, 4, java.math.RoundingMode.HALF_UP)
        }
        return tax.setScale(2, java.math.RoundingMode.HALF_UP).toDouble()
    }

    /**
     * Formats a monetary double value into a locale-aware display string.
     * E.g. "$1,234.50" or "1,234.50 USD" or "1,234.50 BDT" based on the currency and system locale.
     */
    fun format(
        amount: Double,
        currencyCodeOrSymbol: String? = null,
        locale: Locale = Locale.getDefault()
    ): String {
        val curr = resolveCurrency(currencyCodeOrSymbol)
        val symbol = resolveSymbol(curr)

        val numberFormat = NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        val formattedNumber = numberFormat.format(amount)

        return when {
            symbol == "$" || symbol == "€" || symbol == "£" || symbol == "¥" -> "$symbol$formattedNumber"
            symbol.length <= 2 && symbol.any { !it.isLetter() } -> "$symbol $formattedNumber"
            symbol.equals(curr, ignoreCase = true) -> "$formattedNumber $curr"
            else -> "$symbol $formattedNumber"
        }
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
