package com.lojia.shiftreport.auth

object AuthPinConstants {
    const val PIN_LENGTH = 6
    const val MIN_PASSWORD_LENGTH = 6
    const val MAX_ATTEMPTS = 3
    const val LOCKOUT_SECONDS = 30
    const val PIN_REGEX = "^[0-9]{6}$"
}
