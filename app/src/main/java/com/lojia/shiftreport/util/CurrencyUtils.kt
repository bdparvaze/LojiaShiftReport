package com.lojia.shiftreport.util

/**
 * Currency utility methods delegating to [MoneyFormat] as the single source of truth.
 */
object CurrencyUtils {
    /**
     * Formats an amount with the provided currency code or symbol using [MoneyFormat].
     */
    fun formatMoney(amount: Double, currency: String): String {
        return MoneyFormat.format(amount, currency)
    }

    /**
     * Normalizes currency string. If empty, defaults to [defaultCurrency] or "USD".
     */
    fun resolveCurrency(businessCurrency: String?, defaultCurrency: String = MoneyFormat.DEFAULT_CURRENCY_CODE): String {
        val trimmed = businessCurrency?.trim().orEmpty()
        return if (trimmed.isNotEmpty()) trimmed else defaultCurrency
    }
}

