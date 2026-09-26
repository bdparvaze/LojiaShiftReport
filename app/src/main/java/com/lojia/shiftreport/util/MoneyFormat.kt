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
