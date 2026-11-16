package com.lojia.shiftreport.util

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

const val DEVICE_KEK_ALIAS = "lojia_backup_device_kek_v1"
const val MIN_BACKUP_PASSWORD_LENGTH = 8

private const val ANDROID_KEYSTORE_PROVIDER = "AndroidKeyStore"

/**
 * Returns true only if [password] has at least [MIN_BACKUP_PASSWORD_LENGTH] Unicode code points
 * and contains at least one non-whitespace character. Does not trim or mutate [password].
 */
fun isAcceptableBackupPassword(password: CharArray): Boolean {
    if (Character.codePointCount(password, 0, password.size) < MIN_BACKUP_PASSWORD_LENGTH) {
        return false
    }
    var i = 0
    while (i < password.size) {
        val cp = Character.codePointAt(password, i)
        if (!Character.isWhitespace(cp) && !Character.isSpaceChar(cp)) {
            return true
        }
        i += Character.charCount(cp)
    }
    return false
}

/**
 * Retrieves the existing non-exportable AES-256-GCM device KEK from AndroidKeyStore,
 * or creates it if absent. Returns null on any KeyStore failure without logging sensitive details.
 */
fun getOrCreateDeviceKek(): SecretKey? {
    return try {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE_PROVIDER)
        keyStore.load(null)
        if (keyStore.containsAlias(DEVICE_KEK_ALIAS)) {
            val existing = keyStore.getKey(DEVICE_KEK_ALIAS, null) as? SecretKey
            if (existing != null) {
                return existing
            }
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE_PROVIDER
        )
        val spec = KeyGenParameterSpec.Builder(
            DEVICE_KEK_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(false)
            .setUserAuthenticationRequired(false)
            .build()
        keyGenerator.init(spec)
        keyGenerator.generateKey()
    } catch (_: Exception) {
        null
    }
}
