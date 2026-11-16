package com.lojia.shiftreport

import com.lojia.shiftreport.util.PasswordSetupResult
import com.lojia.shiftreport.util.checkPasswordSetup
import org.junit.Assert.assertEquals
import org.junit.Test

class Stage19CBackupUiLogicUnitTest {

    @Test
    fun checkPasswordSetup_matchingEightChars_returnsOk() {
        val result = checkPasswordSetup("secret12".toCharArray(), "secret12".toCharArray())
        assertEquals(PasswordSetupResult.OK, result)
    }

    @Test
    fun checkPasswordSetup_tooShort_returnsTooShort() {
        val result = checkPasswordSetup("abc".toCharArray(), "abc".toCharArray())
        assertEquals(PasswordSetupResult.TOO_SHORT, result)
    }

    @Test
    fun checkPasswordSetup_mismatch_returnsMismatch() {
        val result = checkPasswordSetup("secret12".toCharArray(), "secret13".toCharArray())
        assertEquals(PasswordSetupResult.MISMATCH, result)
    }

    @Test
    fun checkPasswordSetup_eightSpaces_returnsBlank() {
        val spaces = "        ".toCharArray()
        val result = checkPasswordSetup(spaces, spaces.copyOf())
        assertEquals(PasswordSetupResult.BLANK, result)
    }

    @Test
    fun checkPasswordSetup_eightUnicodeCodePointsWithEmoji_returnsOk() {
        val emojiPass = "abcdefg😀".toCharArray()
        val result = checkPasswordSetup(emojiPass, emojiPass.copyOf())
        assertEquals(PasswordSetupResult.OK, result)
    }
}
