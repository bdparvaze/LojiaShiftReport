package com.lojia.pos.auth

import com.lojia.pos.BuildConfig

/**
 * Development & Debug placeholder references.
 * Strictly empty in Release builds to guarantee zero hardcoded working credentials.
 */
object DevCredentials {
    val DEFAULT_USERNAME: String = if (BuildConfig.DEBUG) "demo" else ""
    val DEFAULT_PASSWORD: String = if (BuildConfig.DEBUG) "demo123" else ""
    val DEFAULT_PIN: String = if (BuildConfig.DEBUG) "123456" else ""
}
