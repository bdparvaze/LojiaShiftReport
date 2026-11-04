package com.lojia.shiftreport.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.lojia.shiftreport.util.AppLogger
import com.lojia.shiftreport.util.CryptoUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ══════════════════════════════════════════════════════════════════════════════
 * HARDWARE STORAGE & INTEGRATION LAYER: SecureStorageManager
 * ══════════════════════════════════════════════════════════════════════════════
 * Responsibility: Android OS integration for hardware-backed secure storage.
 * - Manages Android KeyStore MasterKey (AES-256-GCM).
 * - Manages EncryptedSharedPreferences lifecycle and hardware fallback.
 * - Encrypts and persists PIN hashes (derived via [CryptoUtils]) and cashier data.
 * - Persists remembered operator username (foodics/banking standard: never password).
 *
 * NOTE: Algorithmic hashing logic is delegated to [CryptoUtils].
 */
object SecureStorageManager {
    private const val TAG = "SecureStorageManager"
    private const val SECURE_PREFS_NAME = "lojia_encrypted_auth_prefs"
    private const val KEY_ENCRYPTED_PIN_HASH = "sec_enc_pin_hash"
    private const val KEY_ENCRYPTED_PIN_SALT = "sec_enc_pin_salt"
    private const val KEY_CASHIER_PREFIX = "sec_cashier_data_"
    private const val KEY_REMEMBERED_USERNAME = "remembered_username"


    @Volatile
    private var cachedEncryptedPrefs: SharedPreferences? = null

    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    /**
     * Obtains or creates the EncryptedSharedPreferences instance backed by MasterKey AES256_GCM.
     * Falls back safely to sandboxed mode if Android KeyStore hardware is transiently unavailable.
     */
    fun getEncryptedPrefs(context: Context? = null): SharedPreferences {
        val ctx = context?.applicationContext ?: appContext
            ?: throw IllegalStateException("SecureStorageManager context must be initialized before accessing EncryptedSharedPreferences")
        appContext = ctx
        cachedEncryptedPrefs?.let { return it }
        return synchronized(this) {
            cachedEncryptedPrefs ?: try {
                val masterKey = MasterKey.Builder(ctx)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()

                EncryptedSharedPreferences.create(
                    ctx,
                    SECURE_PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                ).also { cachedEncryptedPrefs = it }
            } catch (e: Exception) {
                AppLogger.e(TAG, "KeyStore EncryptedSharedPreferences initialization failed, using sandboxed fallback: ${e.message}", e)
                ctx.getSharedPreferences(SECURE_PREFS_NAME + "_fallback", Context.MODE_PRIVATE)
            }
        }
    }

    /**
     * Stores a user Quick PIN.
     * Strictly validates that PIN is exactly 6 digits via [AuthPinConstants.PIN_REGEX].
     * The raw PIN is NEVER stored in plaintext: it is cryptographically hashed with PBKDF2 (210,000 iterations)
     * and a cryptographic salt, then saved into AES-256-GCM EncryptedSharedPreferences.
     */
    fun saveQuickPin(pin: String) {
        if (!pin.matches(Regex(AuthPinConstants.PIN_REGEX))) {
            throw IllegalArgumentException("PIN must be exactly 6 digits")
        }
        val (hashedPin, salt) = CryptoUtils.hashWithSalt(pin)
        getEncryptedPrefs().edit()
            .putString(KEY_ENCRYPTED_PIN_HASH, hashedPin)
            .putString(KEY_ENCRYPTED_PIN_SALT, salt)
            .apply()
    }

    fun saveQuickPin(context: Context, pin: String) {
        init(context)
        saveQuickPin(pin)
    }

    /**
     * Verifies entered PIN against the stored encrypted hash using constant-time evaluation.
     */
    fun verifyQuickPin(input: String): Boolean {
        if (!input.matches(Regex(AuthPinConstants.PIN_REGEX))) {
            return false
        }
        val prefs = getEncryptedPrefs()
        val storedHash = prefs.getString(KEY_ENCRYPTED_PIN_HASH, null) ?: return false
        val storedSalt = prefs.getString(KEY_ENCRYPTED_PIN_SALT, null).orEmpty()

        return if (storedSalt.isNotBlank()) {
            CryptoUtils.verifyWithSalt(input, storedHash, storedSalt)
        } else {
            val verifyResult = CryptoUtils.verifySecretWithUpgrade(input, storedHash)
            if (verifyResult.isMatch && verifyResult.newHashToStore != null) {
                val (newHash, newSalt) = CryptoUtils.hashWithSalt(input)
                prefs.edit()
                    .putString(KEY_ENCRYPTED_PIN_HASH, newHash)
                    .putString(KEY_ENCRYPTED_PIN_SALT, newSalt)
                    .apply()
            }
            verifyResult.isMatch
        }
    }

    fun verifyQuickPin(context: Context, input: String): Boolean {
        init(context)
        return verifyQuickPin(input)
    }

    fun storeEncryptedPin(context: Context, pin: String) {
        saveQuickPin(context, pin)
    }

    fun verifyEncryptedPin(context: Context, enteredPin: String): Boolean {
        return verifyQuickPin(context, enteredPin)
    }

    /**
     * Checks if an encrypted PIN has been configured.
     */
    fun hasEncryptedPin(context: Context? = null): Boolean {
        val storedHash = try {
            getEncryptedPrefs(context).getString(KEY_ENCRYPTED_PIN_HASH, null)
        } catch (_: Exception) {
            null
        }
        return !storedHash.isNullOrBlank()
    }

    /**
     * Clears configured encrypted PIN.
     */
    fun clearEncryptedPin(context: Context? = null) {
        try {
            getEncryptedPrefs(context).edit()
                .remove(KEY_ENCRYPTED_PIN_HASH)
                .remove(KEY_ENCRYPTED_PIN_SALT)
                .apply()
        } catch (_: Exception) {}
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

    /**
     * Persists the operator/cashier username to EncryptedSharedPreferences.
     * Conforms to Foodics POS and international banking standards: ONLY the username
     * identifier is remembered. Passwords are NEVER persisted.
     */
    suspend fun saveRememberedUsername(username: String) {
        withContext(Dispatchers.IO) {
            getEncryptedPrefs().edit()
                .putString(KEY_REMEMBERED_USERNAME, username.trim())
                .apply()
        }
    }

    /**
     * Retrieves the remembered username from EncryptedSharedPreferences, or null if none saved.
     */
    suspend fun getRememberedUsername(): String? {
        return withContext(Dispatchers.IO) {
            val username = getEncryptedPrefs().getString(KEY_REMEMBERED_USERNAME, null)
            if (username.isNullOrBlank()) null else username
        }
    }

    /**
     * Immediately clears the remembered username from EncryptedSharedPreferences.
     */
    suspend fun clearRememberedUsername() {
        withContext(Dispatchers.IO) {
            getEncryptedPrefs().edit()
                .remove(KEY_REMEMBERED_USERNAME)
                .apply()
        }
    }

    fun saveRememberedUsernameSync(username: String) {
        getEncryptedPrefs().edit()
            .putString(KEY_REMEMBERED_USERNAME, username.trim())
            .apply()
    }

    fun getRememberedUsernameSync(): String? {
        val username = getEncryptedPrefs().getString(KEY_REMEMBERED_USERNAME, null)
        return if (username.isNullOrBlank()) null else username
    }

    fun clearRememberedUsernameSync() {
        getEncryptedPrefs().edit()
            .remove(KEY_REMEMBERED_USERNAME)
            .apply()
    }
}

/**
 * Backward compatibility typealias for [SecureStorageManager].
 */
typealias SecurityHelper = SecureStorageManager
