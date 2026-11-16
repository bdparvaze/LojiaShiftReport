package com.lojia.shiftreport.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

/**
 * Unified currency formatting utility for the entire application.
 * Merges duplicate formatting logic into a single high-precision function.
 */
object CurrencyFormatter {

    /**
     * Single function to format monetary amounts using BigDecimal precision and ISO currency code.
     */
    fun formatMoney(
        amount: BigDecimal,
        currencyCode: String = MoneyFormat.DEFAULT_CURRENCY_CODE,
        locale: Locale = Locale.getDefault(),
        digits: Int? = null
    ): String {
        val effectiveDigits = (digits ?: MoneyFormat.resolveDigitsForFormatting(currencyCode)).coerceAtLeast(0)
        val rounded = amount.setScale(effectiveDigits, RoundingMode.HALF_UP)
        val symbol = MoneyFormat.resolveSymbol(currencyCode, locale)
        val numberFormat = NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = effectiveDigits
            maximumFractionDigits = effectiveDigits
        }
        val formattedNumber = numberFormat.format(rounded)

        return when {
            symbol == "$" || symbol == "€" || symbol == "£" || symbol == "¥" -> "$symbol$formattedNumber"
            symbol.length <= 2 && symbol.any { !it.isLetter() } -> "$symbol $formattedNumber"
            symbol.equals(currencyCode, ignoreCase = true) -> "$formattedNumber $currencyCode"
            else -> "$symbol $formattedNumber"
        }
    }

    fun formatMoney(
        amount: Double,
        currencyCode: String = MoneyFormat.DEFAULT_CURRENCY_CODE,
        locale: Locale = Locale.getDefault(),
        digits: Int? = null
    ): String {
        return formatMoney(BigDecimal.valueOf(amount), currencyCode, locale, digits)
    }

    fun formatMoney(
        amountMinor: Long,
        currencyCode: String = MoneyFormat.DEFAULT_CURRENCY_CODE,
        locale: Locale = Locale.getDefault(),
        digits: Int? = null
    ): String {
        val effectiveDigits = (digits ?: MoneyFormat.resolveDigitsForFormatting(currencyCode)).coerceAtLeast(0)
        val major = BigDecimal.valueOf(amountMinor)
            .divide(BigDecimal.valueOf(MoneyFormat.minorFactor(effectiveDigits)), effectiveDigits, RoundingMode.HALF_UP)
        return formatMoney(major, currencyCode, locale, effectiveDigits)
    }
}
