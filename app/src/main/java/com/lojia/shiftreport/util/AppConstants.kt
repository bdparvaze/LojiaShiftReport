package com.lojia.shiftreport.util

/**
 * Single source of truth for application constants, default configuration values,
 * MIME types, timeouts, intent actions, and storage keys.
 */
object AppConstants {

    // =========================================================================
    // 1. DATABASE & STORAGE
    // =========================================================================
    const val DATABASE_NAME = "lojia_system_database"
    const val DATABASE_SCHEMA_VERSION = 16
    const val PREFS_NAME = "lojia_secure_prefs"
    const val LANGUAGE_PREFS_NAME = "language_prefs"
    const val LANGUAGE_PREF_KEY = "selected_language"

    const val DEFAULT_REPORTS_FOLDER_NAME = "Lojia Reports"
    const val FOLDER_BACKUPS = "Backups"
    const val REINSTALL_META_FILE_NAME = ".lojia_meta"
    const val MAX_AUTO_BACKUPS_RETAINED = 5

    // =========================================================================
    // 2. SUPPORTED LOCALES
    // =========================================================================
    const val LOCALE_EN = "en"
    const val LOCALE_BN = "bn"
    const val LOCALE_AR = "ar"

    // =========================================================================
    // 3. CURRENCY & FINANCIAL DEFAULTS
    // =========================================================================
    const val DEFAULT_CURRENCY_CODE = "USD"
    const val DEFAULT_CURRENCY_SYMBOL = "$"
    const val DEFAULT_TAX_RATE_PERCENT = 15.0
    const val MINOR_UNIT_SCALE = 100L

    // =========================================================================
    // 4. TIMEOUTS & INTERVALS
    // =========================================================================
    const val DEFAULT_AUTO_LOCK_MINUTES = 5
    const val IDLE_CHECK_INTERVAL_MILLIS = 10_000L
    const val ONE_DAY_MILLIS = 86_400_000L
    const val ONE_HOUR_MILLIS = 3_600_000L

    // =========================================================================
    // 5. HARDWARE & PRINTER DEFAULTS
    // =========================================================================
    const val DEFAULT_PRINTER_PORT = 9100
    const val DEFAULT_PRINTER_PAPER_WIDTH_MM = 58
    const val PRINTER_CONN_BLUETOOTH = "bluetooth"
    const val PRINTER_CONN_NETWORK = "network"

    // =========================================================================
    // 6. MIME TYPES & INTENT EXTRAS
    // =========================================================================
    const val MIME_TYPE_PDF = "application/pdf"
    const val MIME_TYPE_JSON = "application/json"
    const val MIME_TYPE_DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    const val MIME_TYPE_IMAGE_ALL = "image/*"
    const val MIME_TYPE_ALL = "*/*"

    const val FILE_PROVIDER_AUTHORITY_SUFFIX = ".fileprovider"
    const val OAUTH_REDIRECT_SCHEME = "com.lojia.shiftreport"
    const val OAUTH_REDIRECT_HOST = "oauth2callback"

    // =========================================================================
    // 7. SECURITY & CRYPTOGRAPHY
    // =========================================================================
    const val PBKDF2_ITERATIONS = 210_000
    const val PBKDF2_KEY_LENGTH_BITS = 256
    const val SALT_LENGTH_BYTES = 16
    const val MPIN_LENGTH = 6
}
