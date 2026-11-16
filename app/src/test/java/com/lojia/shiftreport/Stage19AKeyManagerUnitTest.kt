package com.lojia.shiftreport

import com.lojia.shiftreport.util.TIER_PORTABLE
import com.lojia.shiftreport.util.decryptBackup
import com.lojia.shiftreport.util.deriveBackupKek
import com.lojia.shiftreport.util.encryptBackup
import com.lojia.shiftreport.util.isAcceptableBackupPassword
import com.lojia.shiftreport.util.open
import com.lojia.shiftreport.util.seal
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.SecureRandom
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

class Stage19AKeyManagerUnitTest {

    private fun randomAes256Key(): SecretKey {
        val bytes = ByteArray(32).apply { SecureRandom().nextBytes(this) }
        return SecretKeySpec(bytes, "AES")
    }

    private fun containsSubsequence(haystack: ByteArray, needle: ByteArray): Boolean {
        if (needle.isEmpty() || needle.size > haystack.size) return false
        outer@ for (i in 0..(haystack.size - needle.size)) {
            for (j in needle.indices) {
                if (haystack[i + j] != needle[j]) continue@outer
            }
            return true
        }
        return false
    }

    @Test
    fun passwordLengthAndWhitespacePolicy() {
        assertFalse(isAcceptableBackupPassword("1234567".toCharArray()))
        assertTrue(isAcceptableBackupPassword("12345678".toCharArray()))
        assertFalse(isAcceptableBackupPassword("        ".toCharArray()))
        assertFalse(isAcceptableBackupPassword("".toCharArray()))
    }

    @Test
    fun vaultRoundtripReturnsOriginalPassword() {
        val kek = randomAes256Key()
        val password = "correct-horse-1".toCharArray()
        val blob = seal(password, kek)
        val opened = open(blob, kek)

        assertArrayEquals("correct-horse-1".toCharArray(), opened)
    }

    @Test
    fun vaultWithWrongKekThrows() {
        val kek = randomAes256Key()
        val wrongKek = randomAes256Key()
        val blob = seal("correct-horse-1".toCharArray(), kek)

        assertThrows(Exception::class.java) {
            open(blob, wrongKek)
        }
    }

    @Test
    fun vaultWithTamperedByte20Throws() {
        val kek = randomAes256Key()
        val blob = seal("correct-horse-1".toCharArray(), kek)
        blob[20] = (blob[20].toInt() xor 0x01).toByte()

        assertThrows(Exception::class.java) {
            open(blob, kek)
        }
    }

    @Test
    fun vaultBlobDoesNotContainPlaintextPasswordBytes() {
        val kek = randomAes256Key()
        val password = "correct-horse-1"
        val blob = seal(password.toCharArray(), kek)

        assertFalse(containsSubsequence(blob, password.toByteArray(Charsets.UTF_8)))
    }

    @Test
    fun sealingSamePasswordTwiceProducesDifferentBlobs() {
        val kek = randomAes256Key()
        val password = "correct-horse-1".toCharArray()
        val blob1 = seal(password, kek)
        val blob2 = seal(password, kek)

        assertFalse(blob1.contentEquals(blob2))
    }

    @Test
    fun invalidVaultVersionThrowsIllegalArgumentException() {
        val kek = randomAes256Key()
        val blob = seal("correct-horse-1".toCharArray(), kek)
        blob[0] = 2

        assertThrows(IllegalArgumentException::class.java) {
            open(blob, kek)
        }
    }

    @Test
    fun endToEndVaultOpenToPortableBackupRoundtrip() {
        val vaultKek = randomAes256Key()
        val salt = ByteArray(16).apply { SecureRandom().nextBytes(this) }
        val plain = """{"reports":[{"id":99,"cashier":"Amina"}]}""".toByteArray(Charsets.UTF_8)

        val vaultBlob = seal("correct-horse-1".toCharArray(), vaultKek)
        val openedPassword = open(vaultBlob, vaultKek)
        val backupKek = deriveBackupKek(openedPassword.copyOf(), salt)
        val encryptedBackup = encryptBackup(plain, backupKek, TIER_PORTABLE, salt)

        val decrypted = decryptBackup(encryptedBackup) { _, extractedSalt ->
            deriveBackupKek("correct-horse-1".toCharArray(), extractedSalt)
        }

        assertArrayEquals(plain, decrypted)
    }
}
