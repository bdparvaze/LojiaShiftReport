package com.lojia.shiftreport.util

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Enterprise Security Utility providing PBKDF2WithHmacSHA256 password/PIN hashing,
 * constant-time verification, and backwards compatibility with legacy SHA-256 hashes.
 */
object SecurityUtils {
    private const val DEFAULT_SALT = "lojia_pos_salt_v1"
    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val PBKDF2_ITERATIONS = 10000
    private const val PBKDF2_KEY_LENGTH = 256 // bits

    data class VerifyResult(
        val isMatch: Boolean,
        val newHashToStore: String? = null
    )

    /**
     * Hashes a secret string using PBKDF2WithHmacSHA256 with 10,000 iterations and a cryptographically
     * secure 16-byte random salt.
     * Output format: "pbkdf2:sha256:10000:<salt_hex>:<hash_hex>"
     */
    fun hashSecretPbkdf2(
        input: String,
        saltBytes: ByteArray? = null,
        iterations: Int = PBKDF2_ITERATIONS
    ): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return ""

        val actualSalt = saltBytes ?: ByteArray(16).apply { SecureRandom().nextBytes(this) }
        val spec = PBEKeySpec(trimmed.toCharArray(), actualSalt, iterations, PBKDF2_KEY_LENGTH)
        val skf = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        val hashBytes = skf.generateSecret(spec).encoded

        val saltHex = actualSalt.joinToString("") { "%02x".format(it) }
        val hashHex = hashBytes.joinToString("") { "%02x".format(it) }

        return "pbkdf2:sha256:$iterations:$saltHex:$hashHex"
    }

    /**
     * Hashes a password or PIN string using SHA-256 with an application salt (Legacy format).
     * Output is a 64-character lowercase hex string.
     */
    fun hashSecret(input: String, salt: String = DEFAULT_SALT): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return ""
        
        // If already a 64-character hex string (SHA-256), return normalized lowercase
        if (trimmed.length == 64 && trimmed.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) {
            return trimmed.lowercase()
        }
        
        val saltedInput = "$salt:$trimmed"
        val md = MessageDigest.getInstance("SHA-256")
        val hashBytes = md.digest(saltedInput.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Plain SHA-256 hash (without salt) for backwards compatibility.
     */
    fun hashSecretPlain(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return ""
        val md = MessageDigest.getInstance("SHA-256")
        val hashBytes = md.digest(trimmed.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Verifies an entered plaintext input against a stored hash (PBKDF2, Salted SHA-256, or Plain SHA-256).
     * Performs constant-time comparison to prevent timing attacks.
     */
    fun verifySecret(enteredInput: String, storedHashOrPlaintext: String): Boolean {
        return verifySecretWithUpgrade(enteredInput, storedHashOrPlaintext).isMatch
    }

    /**
     * Verifies an entered secret against stored hash and returns whether it matched along with an
     * upgraded PBKDF2 hash string if the stored hash was legacy format.
     */
    fun verifySecretWithUpgrade(enteredInput: String, storedHashOrPlaintext: String): VerifyResult {
        val cleanEntered = enteredInput.trim()
        val cleanStored = storedHashOrPlaintext.trim()

        if (cleanEntered.isEmpty() || cleanStored.isEmpty()) {
            return VerifyResult(isMatch = false)
        }

        // 1. Check if stored hash is PBKDF2 format ("pbkdf2:sha256:10000:<salt_hex>:<hash_hex>")
        if (cleanStored.startsWith("pbkdf2:sha256:")) {
            try {
                val parts = cleanStored.split(":")
                if (parts.size == 5) {
                    val iterations = parts[2].toIntOrNull() ?: PBKDF2_ITERATIONS
                    val saltHex = parts[3]
                    val expectedHashHex = parts[4]

                    val saltBytes = saltHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
                    val computedFull = hashSecretPbkdf2(cleanEntered, saltBytes, iterations)
                    val computedHashHex = computedFull.split(":").lastOrNull().orEmpty()

                    val isMatch = MessageDigest.isEqual(
                        computedHashHex.lowercase().toByteArray(Charsets.UTF_8),
                        expectedHashHex.lowercase().toByteArray(Charsets.UTF_8)
                    )
                    return VerifyResult(isMatch = isMatch, newHashToStore = null)
                }
            } catch (e: Exception) {
                // Fall back to legacy checks if parsing PBKDF2 fails
            }
        }

        // 2. Legacy salted SHA-256 match
        val saltedEntered = hashSecret(cleanEntered)
        if (saltedEntered.equals(cleanStored, ignoreCase = true)) {
            val upgraded = hashSecretPbkdf2(cleanEntered)
            return VerifyResult(isMatch = true, newHashToStore = upgraded)
        }

        // 3. Legacy plain SHA-256 match
        val plainEntered = hashSecretPlain(cleanEntered)
        val plainStored = hashSecretPlain(cleanStored)
        if (plainEntered.equals(cleanStored, ignoreCase = true) || plainEntered.equals(plainStored, ignoreCase = true)) {
            val upgraded = hashSecretPbkdf2(cleanEntered)
            return VerifyResult(isMatch = true, newHashToStore = upgraded)
        }

        // 4. Direct plaintext comparison (legacy fallback)
        if (cleanEntered == cleanStored) {
            val upgraded = hashSecretPbkdf2(cleanEntered)
            return VerifyResult(isMatch = true, newHashToStore = upgraded)
        }

        return VerifyResult(isMatch = false)
    }

    /**
     * Checks if a stored hash is legacy format and needs upgrading to PBKDF2.
     */
    fun needsUpgrade(storedHash: String): Boolean {
        return !storedHash.trim().startsWith("pbkdf2:sha256:")
    }
}
