package com.lojia.shiftreport.auth

import com.lojia.shiftreport.util.CryptoUtils
import java.util.Locale

/**
 * Normalizes and verifies security recovery questions & answers using PBKDF2-HMAC-SHA256.
 * Never falls back to hardcoded answers or default questions when recovery is unconfigured.
 */
object SecurityRecoveryVerifier {

    fun normalizeAnswer(answer: String): String =
        answer.trim().lowercase(Locale.ROOT)

    fun isRecoveryConfigured(question: String?, storedAnswer: String?): Boolean {
        return !question.isNullOrBlank() && !storedAnswer.isNullOrBlank()
    }

    fun hashNormalizedAnswer(answer: String): String {
        val normalized = normalizeAnswer(answer)
        if (normalized.isEmpty()) return ""
        return CryptoUtils.hashSecret(normalized)
    }

    fun verifyAnswerWithUpgrade(
        enteredAnswer: String,
        storedAnswerOrHash: String
    ): CryptoUtils.VerifyResult {
        val cleanEntered = enteredAnswer.trim()
        val cleanStored = storedAnswerOrHash.trim()
        if (cleanEntered.isEmpty() || cleanStored.isEmpty()) {
            return CryptoUtils.VerifyResult(isMatch = false)
        }

        val normalizedEntered = normalizeAnswer(cleanEntered)

        // 1. Primary path: verify normalized (trimmed + lowercased) answer against stored hash
        val normalizedResult = CryptoUtils.verifySecretWithUpgrade(normalizedEntered, cleanStored)
        if (normalizedResult.isMatch) {
            return normalizedResult
        }

        // 2. Legacy exact-case hash compatibility: upgrade to normalized lowercase PBKDF2 hash on match
        if (cleanEntered != normalizedEntered) {
            val exactCaseHashResult = CryptoUtils.verifySecretWithUpgrade(cleanEntered, cleanStored)
            if (exactCaseHashResult.isMatch) {
                return CryptoUtils.VerifyResult(
                    isMatch = true,
                    newHashToStore = hashNormalizedAnswer(normalizedEntered)
                )
            }
        }

        // 3. Legacy plaintext answer compatibility: compare case-insensitively and upgrade to PBKDF2 hash
        val isHashFormatted = cleanStored.startsWith("pbkdf2:sha256:") ||
            (cleanStored.length == 64 && cleanStored.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' })
        if (!isHashFormatted && cleanEntered.equals(cleanStored, ignoreCase = true)) {
            return CryptoUtils.VerifyResult(
                isMatch = true,
                newHashToStore = hashNormalizedAnswer(normalizedEntered)
            )
        }

        return CryptoUtils.VerifyResult(isMatch = false)
    }
}
