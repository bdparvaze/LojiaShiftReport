package com.lojia.shiftreport.util

import com.lojia.shiftreport.data.AppCountry
import java.util.Locale

/**
 * Maximum total digits (country calling code + national significant number)
 * allowed by ITU-T E.164.
 */
const val ITU_E164_MAX_DIGITS = 15

/**
 * Shortest national significant subscriber number length in active international
 * numbering plans (ITU-T E.164).
 */
const val MIN_LOCAL_NUMBER_DIGITS = 4

private const val ISO_COUNTRY_CODE_LENGTH = 2

internal fun extractAsciiDigits(raw: String): String {
    val sb = StringBuilder(raw.length)
    for (ch in raw) {
        if (ch in '0'..'9') {
            sb.append(ch)
        } else if (ch.isDigit()) {
            val numeric = Character.getNumericValue(ch)
            if (numeric in 0..9) {
                sb.append(('0'.code + numeric).toChar())
            }
        }
    }
    return sb.toString()
}

/**
 * Normalizes a local mobile number input to ASCII digits only, stripping spaces,
 * dashes, brackets, and any pasted leading `+` or dial code matching [country].
 */
fun normalizeLocalInput(input: String, country: AppCountry): String {
    val strippedFormatting = input.trim()
        .filterNot { it == ' ' || it == '-' || it == '(' || it == ')' }
    val hadPlus = strippedFormatting.startsWith("+")
    var digits = extractAsciiDigits(strippedFormatting)
    val countryDigits = extractAsciiDigits(country.dialCode)
    if (hadPlus && countryDigits.isNotEmpty() && digits.startsWith(countryDigits)) {
        digits = digits.removePrefix(countryDigits)
    }
    return digits
}

/**
 * Formats a normalized E.164 phone string ("<dialCode><localDigits>") for storage,
 * or returns empty string when [localInput] contains no digits.
 */
fun buildE164Phone(country: AppCountry, localInput: String): String {
    val localDigits = normalizeLocalInput(localInput, country)
    if (localDigits.isEmpty()) return ""
    return "${country.dialCode}$localDigits"
}

/**
 * Validates a local number against [country] using ITU-T E.164 bounds:
 * at least [MIN_LOCAL_NUMBER_DIGITS] local digits and at most [ITU_E164_MAX_DIGITS]
 * total digits (country dial code + local number).
 */
fun isValidPhone(country: AppCountry, local: String): Boolean {
    val localDigits = normalizeLocalInput(local, country)
    val countryDigits = extractAsciiDigits(country.dialCode)
    val totalDigits = countryDigits.length + localDigits.length
    return localDigits.length >= MIN_LOCAL_NUMBER_DIGITS && totalDigits <= ITU_E164_MAX_DIGITS
}

/**
 * Resolves a 2-letter uppercase ISO 3166-1 alpha-2 country code from device signals
 * in priority order: SIM ISO -> Network ISO -> Locale ISO -> [fallbackIso].
 */
fun resolveCountryIso(
    simIso: String?,
    networkIso: String?,
    localeIso: String?,
    fallbackIso: String
): String {
    val candidates = listOf(simIso, networkIso, localeIso, fallbackIso)
    for (candidate in candidates) {
        val normalized = candidate?.trim()?.uppercase(Locale.ROOT).orEmpty()
        if (normalized.length == ISO_COUNTRY_CODE_LENGTH && normalized.all { it in 'A'..'Z' }) {
            return normalized
        }
    }
    return fallbackIso.trim().uppercase(Locale.ROOT)
}
