package com.lojia.shiftreport.util

import com.lojia.shiftreport.BuildConfig

/**
 * Externalized customer-support contact configuration backed by [BuildConfig].
 */
object SupportContact {

    /**
     * Documented WhatsApp click-to-chat base URL requiring digits-only international number.
     */
    const val WHATSAPP_CLICK_TO_CHAT_BASE_URL = "https://wa.me/"

    /**
     * Telephone URI scheme per RFC 3966.
     */
    private const val TEL_URI_SCHEME = "tel:"

    val SUPPORT_PHONE: String
        get() = BuildConfig.SUPPORT_PHONE

    val SUPPORT_WHATSAPP: String
        get() = BuildConfig.SUPPORT_WHATSAPP

    fun isSupportConfigured(
        phone: String = SUPPORT_PHONE,
        whatsapp: String = SUPPORT_WHATSAPP
    ): Boolean = phone.isNotBlank() || whatsapp.isNotBlank()

    fun buildDialUri(number: String): String {
        val trimmed = number.trim()
        val digits = extractAsciiDigits(trimmed)
        if (digits.isEmpty()) return ""
        return if (trimmed.startsWith("+")) "$TEL_URI_SCHEME+$digits" else "$TEL_URI_SCHEME$digits"
    }

    fun buildWhatsAppUri(number: String): String {
        val digits = extractAsciiDigits(number)
        if (digits.isEmpty()) return ""
        return "$WHATSAPP_CLICK_TO_CHAT_BASE_URL$digits"
    }
}
