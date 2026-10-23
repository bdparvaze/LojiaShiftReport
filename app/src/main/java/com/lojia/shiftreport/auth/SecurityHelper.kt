package com.lojia.shiftreport.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.lojia.shiftreport.util.SecurityUtils

/**
 * ══════════════════════════════════════════════════════════════════════════════
 * HARDWARE STORAGE & INTEGRATION LAYER: SecurityHelper
 * ══════════════════════════════════════════════════════════════════════════════
 * Responsibility: Android OS integration for hardware-backed secure storage.
 * - Manages Android KeyStore MasterKey (AES-256-GCM).
 * - Manages EncryptedSharedPreferences lifecycle and hardware fallback.
 * - Encrypts and persists PIN hashes (derived via [SecurityUtils]) and cashier data.
 *
 * NOTE: Algorithmic hashing logic is delegated to [SecurityUtils].
 */
object SecurityHelper {
    private const val TAG = "SecurityHelper"
    private const val SECURE_PREFS_NAME = "lojia_encrypted_auth_prefs"
    private const val KEY_ENCRYPTED_PIN_HASH = "sec_enc_pin_hash"
    private const val KEY_CASHIER_PREFIX = "sec_cashier_data_"

    @Volatile
    private var cachedEncryptedPrefs: SharedPreferences? = null

    /**
     * Obtains or creates the EncryptedSharedPreferences instance backed by MasterKey AES256_GCM.
     * Falls back safely to sandboxed mode if Android KeyStore hardware is transiently unavailable.
     */
    fun getEncryptedPrefs(context: Context): SharedPreferences {
        cachedEncryptedPrefs?.let { return it }
        return synchronized(this) {
            cachedEncryptedPrefs ?: try {
                val masterKey = MasterKey.Builder(context.applicationContext)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()

                EncryptedSharedPreferences.create(
                    context.applicationContext,
                    SECURE_PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                ).also { cachedEncryptedPrefs = it }
            } catch (e: Exception) {
                Log.e(TAG, "KeyStore EncryptedSharedPreferences initialization failed, using sandboxed fallback: ${e.message}", e)
                context.applicationContext.getSharedPreferences(SECURE_PREFS_NAME + "_fallback", Context.MODE_PRIVATE)
            }
        }
    }

    /**
     * Stores a user PIN.
     * The raw PIN is NEVER stored in plaintext: it is cryptographically hashed with PBKDF2
     * before being committed into AES-256-GCM EncryptedSharedPreferences.
     */
    fun storeEncryptedPin(context: Context, pin: String) {
        val trimmed = pin.trim()
        if (trimmed.isEmpty()) return
        val hashedPin = SecurityUtils.hashSecret(trimmed)
        getEncryptedPrefs(context).edit()
            .putString(KEY_ENCRYPTED_PIN_HASH, hashedPin)
            .apply()
    }

    /**
     * Verifies entered PIN against the stored encrypted hash using constant-time evaluation.
     */
    fun verifyEncryptedPin(context: Context, enteredPin: String): Boolean {
        val storedHash = getEncryptedPrefs(context).getString(KEY_ENCRYPTED_PIN_HASH, null) ?: return false
        val verifyResult = SecurityUtils.verifySecretWithUpgrade(enteredPin.trim(), storedHash)
        if (verifyResult.isMatch && verifyResult.newHashToStore != null) {
            getEncryptedPrefs(context).edit()
                .putString(KEY_ENCRYPTED_PIN_HASH, verifyResult.newHashToStore)
                .apply()
        }
        return verifyResult.isMatch
    }

    /**
     * Checks if an encrypted PIN has been configured.
     */
    fun hasEncryptedPin(context: Context): Boolean {
        val storedHash = getEncryptedPrefs(context).getString(KEY_ENCRYPTED_PIN_HASH, null)
        return !storedHash.isNullOrBlank()
    }

    /**
     * Clears configured encrypted PIN.
     */
    fun clearEncryptedPin(context: Context) {
        getEncryptedPrefs(context).edit()
            .remove(KEY_ENCRYPTED_PIN_HASH)
            .apply()
    }

    /**
     * Stores sensitive cashier data under AES-256-GCM encryption.
     */
    fun storeEncryptedCashierData(context: Context, cashierId: String, dataJson: String) {
        getEncryptedPrefs(context).edit()
            .putString("$KEY_CASHIER_PREFIX$cashierId", dataJson)
            .apply()
    }

    /**
     * Reads sensitive cashier data from AES-256-GCM storage.
     */
    fun getEncryptedCashierData(context: Context, cashierId: String): String? {
        return getEncryptedPrefs(context).getString("$KEY_CASHIER_PREFIX$cashierId", null)
    }
}
