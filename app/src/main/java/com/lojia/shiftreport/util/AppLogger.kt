package com.lojia.shiftreport.util

import android.util.Log
import com.lojia.shiftreport.BuildConfig

/**
 * Centralized Application Logger adhering to Google Play and Android security standards.
 *
 * Rules:
 * 1. DEBUG and VERBOSE logs are strictly stripped in RELEASE builds to prevent leaking
 *    monetary transactions, cashier identities, or security tokens to logcat.
 * 2. Standardized tag prefix "Lojia:" for consistent filtering and log aggregation.
 * 3. WARNING and ERROR logs are retained across all build variants for crash diagnostics.
 */
object AppLogger {
    private const val GLOBAL_PREFIX = "Lojia"

    private fun formatTag(tag: String): String = "$GLOBAL_PREFIX:$tag"

    fun v(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.v(formatTag(tag), message)
        }
    }

    fun d(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(formatTag(tag), message)
        }
    }

    fun i(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.i(formatTag(tag), message)
        }
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        val formatted = formatTag(tag)
        if (throwable != null) {
            Log.w(formatted, message, throwable)
        } else {
            Log.w(formatted, message)
        }
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val formatted = formatTag(tag)
        if (throwable != null) {
            Log.e(formatted, message, throwable)
        } else {
            Log.e(formatted, message)
        }
    }
}
