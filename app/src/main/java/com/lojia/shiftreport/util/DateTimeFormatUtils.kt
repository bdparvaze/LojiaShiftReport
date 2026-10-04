package com.lojia.shiftreport.util

import androidx.appcompat.app.AppCompatDelegate
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Centralized, standardized locale-aware Date and Time formatting utility using java.time.DateTimeFormatter.
 */
object DateTimeFormatUtils {

    /**
     * Resolves active application locale from AppCompatDelegate or LanguagePreferences.
     */
    fun getAppLocale(): Locale {
        val languageTags = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        val langCode = languageTags.substringBefore('-').ifEmpty { "en" }
        return Locale.forLanguageTag(langCode)
    }

    /**
     * Formats date with localized medium style (e.g. "dd MMM yyyy").
     */
    fun formatDate(timestampMs: Long, locale: Locale = getAppLocale()): String {
        val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy", locale)
        return Instant.ofEpochMilli(timestampMs)
            .atZone(ZoneId.systemDefault())
            .format(formatter)
    }

    /**
     * Formats date in short numerical format ("dd-MM-yyyy").
     */
    fun formatDateShort(timestampMs: Long, locale: Locale = getAppLocale()): String {
        val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy", locale)
        return Instant.ofEpochMilli(timestampMs)
            .atZone(ZoneId.systemDefault())
            .format(formatter)
    }

    /**
     * Formats date in date-only format ("yyyy-MM-dd").
     */
    fun formatDateOnly(timestampMs: Long, locale: Locale = getAppLocale()): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", locale)
        return Instant.ofEpochMilli(timestampMs)
            .atZone(ZoneId.systemDefault())
            .format(formatter)
    }

    /**
     * Formats date and time with localized pattern (e.g. "dd MMM yyyy, hh:mm a").
     */
    fun formatDateTime(timestampMs: Long, locale: Locale = getAppLocale()): String {
        val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", locale)
        return Instant.ofEpochMilli(timestampMs)
            .atZone(ZoneId.systemDefault())
            .format(formatter)
    }

    /**
     * Formats date and time for setting logs ("MMM dd, yyyy 'at' hh:mm a").
     */
    fun formatDateTimeWithAt(timestampMs: Long, locale: Locale = getAppLocale()): String {
        val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a", locale)
        return Instant.ofEpochMilli(timestampMs)
            .atZone(ZoneId.systemDefault())
            .format(formatter)
    }

    /**
     * Formats time only with localized short style (e.g. "hh:mm a").
     */
    fun formatTime(timestampMs: Long, locale: Locale = getAppLocale()): String {
        val formatter = DateTimeFormatter.ofPattern("hh:mm a", locale)
        return Instant.ofEpochMilli(timestampMs)
            .atZone(ZoneId.systemDefault())
            .format(formatter)
    }

    /**
     * Formats time only in 24-hour style ("HH:mm").
     */
    fun formatTime24(timestampMs: Long, locale: Locale = getAppLocale()): String {
        val formatter = DateTimeFormatter.ofPattern("HH:mm", locale)
        return Instant.ofEpochMilli(timestampMs)
            .atZone(ZoneId.systemDefault())
            .format(formatter)
    }

    /**
     * ISO date format for receipts and PDFs: "yyyy-MM-dd HH:mm" with Locale.US digits.
     */
    fun formatIsoDateTime(timestampMs: Long = System.currentTimeMillis()): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.US)
        return Instant.ofEpochMilli(timestampMs)
            .atZone(ZoneId.systemDefault())
            .format(formatter)
    }

    /**
     * ISO date format with seconds for audit/PDF reports: "yyyy-MM-dd HH:mm:ss" with Locale.US digits.
     */
    fun formatIsoDateTimeSeconds(timestampMs: Long = System.currentTimeMillis()): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.US)
        return Instant.ofEpochMilli(timestampMs)
            .atZone(ZoneId.systemDefault())
            .format(formatter)
    }

    /**
     * Formats filename timestamp in ISO-safe format (e.g. "yyyyMMdd_HHmmss") with Locale.US.
     */
    fun formatFileTimestamp(timestampMs: Long = System.currentTimeMillis()): String {
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss", Locale.US)
        return Instant.ofEpochMilli(timestampMs)
            .atZone(ZoneId.systemDefault())
            .format(formatter)
    }

    /**
     * Formats filename timestamp with dashes (e.g. "yyyy-MM-dd_HH-mm") with Locale.US.
     */
    fun formatFileTimestampDash(timestampMs: Long = System.currentTimeMillis()): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm", Locale.US)
        return Instant.ofEpochMilli(timestampMs)
            .atZone(ZoneId.systemDefault())
            .format(formatter)
    }
}
