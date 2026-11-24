package com.lojia.shiftreport.util

import androidx.appcompat.app.AppCompatDelegate
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

/**
 * Centralized, standardized locale-aware Date and Time formatting utility using java.time.
 * Display methods exclusively use localized styles; ISO 8601 patterns are restricted to
 * machine-readable exports, audit logs, and file names.
 */
object DateTimeFormatUtils {

    /** ISO 8601 calendar date pattern for machine/CSV export rows. */
    const val ISO_LOCAL_DATE_PATTERN = "yyyy-MM-dd"

    /** ISO 8601 date-time pattern without seconds for machine/receipt/audit strings. */
    const val ISO_DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm"

    /** ISO 8601 date-time pattern with seconds for audit and backup payloads. */
    const val ISO_DATE_TIME_SECONDS_PATTERN = "yyyy-MM-dd HH:mm:ss"

    /** ISO 8601 compact timestamp pattern for exported file names. */
    const val FILE_TIMESTAMP_PATTERN = "yyyyMMdd_HHmmss"

    /** ISO 8601 dashed timestamp pattern for backup file names. */
    const val FILE_TIMESTAMP_DASH_PATTERN = "yyyy-MM-dd_HH-mm"

    /**
     * Resolves active application locale from AppCompatDelegate or default primary language constant.
     */
    fun getAppLocale(): Locale {
        val languageTags = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        val langCode = languageTags.substringBefore('-').ifEmpty { LanguagePreferences.DEFAULT_PRIMARY_LANGUAGE_CODE }
        return Locale.forLanguageTag(langCode)
    }

    /**
     * Formats date with localized medium style for the specified locale.
     */
    fun formatDate(timestampMs: Long, locale: Locale = getAppLocale()): String {
        val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
        return Instant.ofEpochMilli(timestampMs)
            .atZone(ZoneId.systemDefault())
            .format(formatter)
    }

    /**
     * Formats date with localized short style for compact fields and pickers.
     */
    fun formatDateShort(timestampMs: Long, locale: Locale = getAppLocale()): String {
        val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(locale)
        return Instant.ofEpochMilli(timestampMs)
            .atZone(ZoneId.systemDefault())
            .format(formatter)
    }

    /**
     * Formats date-only display text with localized medium style for PDFs and tables.
     */
    fun formatDateOnly(timestampMs: Long, locale: Locale = getAppLocale()): String {
        val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
        return Instant.ofEpochMilli(timestampMs)
            .atZone(ZoneId.systemDefault())
            .format(formatter)
    }

    /**
     * Formats date and time with localized medium date and short time style.
     */
    fun formatDateTime(timestampMs: Long, locale: Locale = getAppLocale()): String {
        val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(locale)
        return Instant.ofEpochMilli(timestampMs)
            .atZone(ZoneId.systemDefault())
            .format(formatter)
    }

    /**
     * Formats date and time with localized medium date and short time style.
     */
    fun formatDateTimeWithAt(timestampMs: Long, locale: Locale = getAppLocale()): String {
        val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(locale)
        return Instant.ofEpochMilli(timestampMs)
            .atZone(ZoneId.systemDefault())
            .format(formatter)
    }

    /**
     * Formats time only with localized short style for the specified locale.
     */
    fun formatTime(timestampMs: Long, locale: Locale = getAppLocale()): String {
        val formatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
        return Instant.ofEpochMilli(timestampMs)
            .atZone(ZoneId.systemDefault())
            .format(formatter)
    }

    /**
     * Formats time only with localized short style; the locale determines 12h or 24h presentation.
     */
    fun formatTime24(timestampMs: Long, locale: Locale = getAppLocale()): String {
        val formatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
        return Instant.ofEpochMilli(timestampMs)
            .atZone(ZoneId.systemDefault())
            .format(formatter)
    }

    /**
     * Formats a localized month and year heading without typed display patterns.
     */
    fun formatMonthYear(yearMonth: YearMonth, locale: Locale = getAppLocale()): String {
        val intFmt = NumberFormat.getIntegerInstance(locale).apply { isGroupingUsed = false }
        val monthName = yearMonth.month.getDisplayName(TextStyle.FULL_STANDALONE, locale)
        return "$monthName ${intFmt.format(yearMonth.year)}"
    }

    /**
     * Formats a localized short weekday name.
     */
    fun formatShortWeekday(dayOfWeek: DayOfWeek, locale: Locale = getAppLocale()): String {
        return dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
    }

    /**
     * Formats a localized short month and day label without typed display patterns.
     */
    fun formatMonthDay(date: LocalDate, locale: Locale = getAppLocale()): String {
        val intFmt = NumberFormat.getIntegerInstance(locale).apply { isGroupingUsed = false }
        val monthShort = date.month.getDisplayName(TextStyle.SHORT, locale)
        return "$monthShort ${intFmt.format(date.dayOfMonth)}"
    }

    /**
     * ISO 8601 calendar date format ("yyyy-MM-dd") with Locale.ROOT digits for CSV and machine data.
     */
    fun formatIsoDate(timestampMs: Long = System.currentTimeMillis()): String {
        val formatter = DateTimeFormatter.ofPattern(ISO_LOCAL_DATE_PATTERN, Locale.ROOT)
        return Instant.ofEpochMilli(timestampMs)
            .atZone(ZoneId.systemDefault())
            .format(formatter)
    }

    /**
     * ISO 8601 date format for receipts and machine timestamps ("yyyy-MM-dd HH:mm") with Locale.ROOT digits.
     */
    fun formatIsoDateTime(timestampMs: Long = System.currentTimeMillis()): String {
        val formatter = DateTimeFormatter.ofPattern(ISO_DATE_TIME_PATTERN, Locale.ROOT)
        return Instant.ofEpochMilli(timestampMs)
            .atZone(ZoneId.systemDefault())
            .format(formatter)
    }

    /**
     * ISO 8601 date format with seconds ("yyyy-MM-dd HH:mm:ss") with Locale.ROOT digits.
     */
    fun formatIsoDateTimeSeconds(timestampMs: Long = System.currentTimeMillis()): String {
        val formatter = DateTimeFormatter.ofPattern(ISO_DATE_TIME_SECONDS_PATTERN, Locale.ROOT)
        return Instant.ofEpochMilli(timestampMs)
            .atZone(ZoneId.systemDefault())
            .format(formatter)
    }

    /**
     * Formats filename timestamp in ISO-safe format ("yyyyMMdd_HHmmss") with Locale.ROOT digits.
     */
    fun formatFileTimestamp(timestampMs: Long = System.currentTimeMillis()): String {
        val formatter = DateTimeFormatter.ofPattern(FILE_TIMESTAMP_PATTERN, Locale.ROOT)
        return Instant.ofEpochMilli(timestampMs)
            .atZone(ZoneId.systemDefault())
            .format(formatter)
    }

    /**
     * Formats filename timestamp with dashes ("yyyy-MM-dd_HH-mm") with Locale.ROOT digits.
     */
    fun formatFileTimestampDash(timestampMs: Long = System.currentTimeMillis()): String {
        val formatter = DateTimeFormatter.ofPattern(FILE_TIMESTAMP_DASH_PATTERN, Locale.ROOT)
        return Instant.ofEpochMilli(timestampMs)
            .atZone(ZoneId.systemDefault())
            .format(formatter)
    }
}
