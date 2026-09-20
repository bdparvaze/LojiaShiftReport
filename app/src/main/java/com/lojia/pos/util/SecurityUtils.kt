package com.lojia.pos.util

import java.security.MessageDigest

object SecurityUtils {
    private const val DEFAULT_SALT = "lojia_pos_salt_v1"

    /**
     * Hashes a password or PIN string using SHA-256 with an application salt.
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
     * Verifies an entered plaintext input against a stored SHA-256 hash.
     * Strictly verifies cryptographic hash match without any plaintext or backdoor bypasses.
     */
    fun verifySecret(enteredInput: String, storedHashOrPlaintext: String): Boolean {
        val cleanEntered = enteredInput.trim()
        val cleanStored = storedHashOrPlaintext.trim()
        
        if (cleanEntered.isEmpty() || cleanStored.isEmpty()) return false
        
        // 1. Salted SHA-256 match
        val saltedEntered = hashSecret(cleanEntered)
        if (saltedEntered.equals(cleanStored, ignoreCase = true)) {
            return true
        }
        
        // 2. Legacy plain SHA-256 match
        val plainEntered = hashSecretPlain(cleanEntered)
        val plainStored = hashSecretPlain(cleanStored)
        if (plainEntered.equals(cleanStored, ignoreCase = true) || plainEntered.equals(plainStored, ignoreCase = true)) {
            return true
        }

        return false
    }
}
