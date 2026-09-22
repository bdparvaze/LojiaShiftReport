package com.lojia.shiftreport.util

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Standardized locale-aware Date and Time formatting utility.
 */
object DateTimeFormatUtils {

    /**
     * Formats date with localized medium style (e.g. "MMM dd, yyyy").
     */
    fun formatDate(timestampMs: Long, locale: Locale = Locale.getDefault()): String {
        val formatter = DateFormat.getDateInstance(DateFormat.MEDIUM, locale)
        return formatter.format(Date(timestampMs))
    }

    /**
     * Formats date and time with localized medium/short style (e.g. "MMM dd, yyyy, hh:mm a").
     */
    fun formatDateTime(timestampMs: Long, locale: Locale = Locale.getDefault()): String {
        val formatter = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale)
        return formatter.format(Date(timestampMs))
    }

    /**
     * Formats time only with localized short style (e.g. "hh:mm a").
     */
    fun formatTime(timestampMs: Long, locale: Locale = Locale.getDefault()): String {
        val formatter = DateFormat.getTimeInstance(DateFormat.SHORT, locale)
        return formatter.format(Date(timestampMs))
    }

    /**
     * Formats filename timestamp in ISO-safe format (e.g. "yyyyMMdd_HHmmss").
     */
    fun formatFileTimestamp(date: Date = Date()): String {
        val formatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        return formatter.format(date)
    }
}
