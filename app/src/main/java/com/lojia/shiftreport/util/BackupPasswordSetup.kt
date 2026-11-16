package com.lojia.shiftreport.util

enum class PasswordSetupResult {
    OK,
    BLANK,
    TOO_SHORT,
    MISMATCH
}

/**
 * Pure JVM-safe validation for backup password setup and change.
 * Checks blankness (empty or whitespace-only) before length (in Unicode code points) and match.
 * Does not mutate or zero [newPassword] or [confirm].
 */
fun checkPasswordSetup(newPassword: CharArray, confirm: CharArray): PasswordSetupResult {
    var hasNonWhitespace = false
    var i = 0
    while (i < newPassword.size) {
        val cp = Character.codePointAt(newPassword, i)
        if (!Character.isWhitespace(cp) && !Character.isSpaceChar(cp)) {
            hasNonWhitespace = true
            break
        }
        i += Character.charCount(cp)
    }
    if (!hasNonWhitespace) {
        return PasswordSetupResult.BLANK
    }
    if (Character.codePointCount(newPassword, 0, newPassword.size) < MIN_BACKUP_PASSWORD_LENGTH) {
        return PasswordSetupResult.TOO_SHORT
    }
    if (!newPassword.contentEquals(confirm)) {
        return PasswordSetupResult.MISMATCH
    }
    return PasswordSetupResult.OK
}
