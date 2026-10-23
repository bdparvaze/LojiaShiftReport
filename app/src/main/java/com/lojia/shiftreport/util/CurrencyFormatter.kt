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
        currencyCode: String = MoneyFormat.DEFAULT_CURRENCY_CODE
    ): String {
        val rounded = amount.setScale(2, RoundingMode.HALF_UP)
        val symbol = MoneyFormat.resolveSymbol(currencyCode)
        val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        val formattedNumber = numberFormat.format(rounded.toDouble())

        return when {
            symbol == "$" || symbol == "€" || symbol == "£" || symbol == "¥" -> "$symbol$formattedNumber"
            symbol.length <= 2 && symbol.any { !it.isLetter() } -> "$symbol $formattedNumber"
            symbol.equals(currencyCode, ignoreCase = true) -> "$formattedNumber $currencyCode"
            else -> "$symbol $formattedNumber"
        }
    }

    fun formatMoney(
        amount: Double,
        currencyCode: String = MoneyFormat.DEFAULT_CURRENCY_CODE
    ): String {
        return formatMoney(BigDecimal.valueOf(amount), currencyCode)
    }

    fun formatMoney(
        amountMinor: Long,
        currencyCode: String = MoneyFormat.DEFAULT_CURRENCY_CODE
    ): String {
        val major = BigDecimal.valueOf(amountMinor)
            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
        return formatMoney(major, currencyCode)
    }
}
