package com.lojia.shiftreport

import com.lojia.shiftreport.util.BACKUP_MAGIC
import com.lojia.shiftreport.util.BACKUP_VERSION
import com.lojia.shiftreport.util.BackupFormatException
import com.lojia.shiftreport.util.TIER_DEVICE
import com.lojia.shiftreport.util.TIER_PORTABLE
import com.lojia.shiftreport.util.backupTierOf
import com.lojia.shiftreport.util.decryptBackup
import com.lojia.shiftreport.util.deriveBackupKek
import com.lojia.shiftreport.util.encryptBackup
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.security.SecureRandom
import javax.crypto.spec.SecretKeySpec

class Stage18BackupCryptoUnitTest {

    private fun randomBytes(size: Int): ByteArray = ByteArray(size).apply { SecureRandom().nextBytes(this) }

    @Test
    fun portableRoundtrip() {
        val plain = """{"reports":[{"id":1,"netSales":12345}]}""".toByteArray(Charsets.UTF_8)
        val salt = randomBytes(16)
        val kek = deriveBackupKek("correct-horse-1".toCharArray(), salt)

        val blob = encryptBackup(plain, kek, TIER_PORTABLE, salt)
        val decrypted = decryptBackup(blob) { tier, extractedSalt ->
            assertEquals(TIER_PORTABLE, tier)
            deriveBackupKek("correct-horse-1".toCharArray(), extractedSalt)
        }

        assertArrayEquals(plain, decrypted)
    }

    @Test
    fun wrongPasswordThrows() {
        val plain = "sensitive-shift-backup".toByteArray(Charsets.UTF_8)
        val salt = randomBytes(16)
        val kek = deriveBackupKek("correct-horse-1".toCharArray(), salt)
        val blob = encryptBackup(plain, kek, TIER_PORTABLE, salt)

        assertThrows(Exception::class.java) {
            decryptBackup(blob) { _, extractedSalt ->
                deriveBackupKek("wrong-password".toCharArray(), extractedSalt)
            }
        }
    }

    @Test
    fun tamperedCiphertextThrows() {
        val plain = "tamper-ciphertext-test".toByteArray(Charsets.UTF_8)
        val salt = randomBytes(16)
        val kek = deriveBackupKek("correct-horse-1".toCharArray(), salt)
        val blob = encryptBackup(plain, kek, TIER_PORTABLE, salt)

        blob[blob.lastIndex] = (blob[blob.lastIndex].toInt() xor 0xFF).toByte()

        assertThrows(Exception::class.java) {
            decryptBackup(blob) { _, extractedSalt ->
                deriveBackupKek("correct-horse-1".toCharArray(), extractedSalt)
            }
        }
    }

    @Test
    fun tamperedHeaderThrows() {
        val plain = "tamper-header-test".toByteArray(Charsets.UTF_8)
        val salt = randomBytes(16)
        val kek = deriveBackupKek("correct-horse-1".toCharArray(), salt)
        val blob = encryptBackup(plain, kek, TIER_PORTABLE, salt)

        blob[6] = (blob[6].toInt() xor 0x01).toByte()

        assertThrows(Exception::class.java) {
            decryptBackup(blob) { _, extractedSalt ->
                deriveBackupKek("correct-horse-1".toCharArray(), extractedSalt)
            }
        }
    }

    @Test
    fun randomnessProducesDistinctBlobs() {
        val plain = "identical-plaintext-payload".toByteArray(Charsets.UTF_8)
        val salt = randomBytes(16)
        val kek = deriveBackupKek("correct-horse-1".toCharArray(), salt)

        val blob1 = encryptBackup(plain, kek, TIER_PORTABLE, salt)
        val blob2 = encryptBackup(plain, kek, TIER_PORTABLE, salt)

        assertFalse(blob1.contentEquals(blob2))
    }

    @Test
    fun layoutMatchesSpecification() {
        val plain = "layout-check-payload".toByteArray(Charsets.UTF_8)
        val salt = randomBytes(16)
        val kek = deriveBackupKek("correct-horse-1".toCharArray(), salt)

        val blob = encryptBackup(plain, kek, TIER_PORTABLE, salt)

        assertEquals(BACKUP_MAGIC, String(blob, 0, 4, Charsets.US_ASCII))
        assertEquals(BACKUP_VERSION.toByte(), blob[4])
        assertEquals(TIER_PORTABLE.toByte(), blob[5])
        assertEquals(94 + plain.size + 16, blob.size)
    }

    @Test
    fun deviceRoundtrip() {
        val plain = "device-tier-backup-payload".toByteArray(Charsets.UTF_8)
        val deviceKek = SecretKeySpec(randomBytes(32), "AES")

        val blob = encryptBackup(plain, deviceKek, TIER_DEVICE, null)
        assertEquals(TIER_DEVICE.toByte(), blob[5])

        val decrypted = decryptBackup(blob) { tier, _ ->
            assertEquals(TIER_DEVICE, tier)
            deviceKek
        }

        assertArrayEquals(plain, decrypted)
    }

    @Test
    fun legacyDetectionReturnsNullTierAndThrowsBackupFormatException() {
        val legacyBlob = """{"schemaVersion":19}""".toByteArray(Charsets.UTF_8)

        assertNull(backupTierOf(legacyBlob))
        assertThrows(BackupFormatException::class.java) {
            decryptBackup(legacyBlob) { _, _ ->
                SecretKeySpec(ByteArray(32), "AES")
            }
        }
    }

    @Test
    fun backupTierOfPortableBlobReturnsOne() {
        val plain = "tier-detection".toByteArray(Charsets.UTF_8)
        val salt = randomBytes(16)
        val kek = deriveBackupKek("correct-horse-1".toCharArray(), salt)
        val blob = encryptBackup(plain, kek, TIER_PORTABLE, salt)

        assertEquals(1, backupTierOf(blob))
    }

    private fun assertFlippedByteAtOffsetThrows(offset: Int) {
        val plain = "tamper-test-payload-with-enough-length".toByteArray(Charsets.UTF_8)
        val salt = randomBytes(16)
        val kek = deriveBackupKek("correct-horse-1".toCharArray(), salt)
        val blob = encryptBackup(plain, kek, TIER_PORTABLE, salt)

        blob[offset] = (blob[offset].toInt() xor 0x01).toByte()

        assertThrows(Exception::class.java) {
            decryptBackup(blob) { _, extractedSalt ->
                deriveBackupKek("correct-horse-1".toCharArray(), extractedSalt)
            }
        }
    }

    @Test
    fun tamperedTierByteThrows() {
        assertFlippedByteAtOffsetThrows(5)
    }

    @Test
    fun tamperedWrapIvByteThrows() {
        assertFlippedByteAtOffsetThrows(22)
    }

    @Test
    fun tamperedWrappedDekByteThrows() {
        assertFlippedByteAtOffsetThrows(40)
    }

    @Test
    fun tamperedDataIvByteThrows() {
        assertFlippedByteAtOffsetThrows(85)
    }

    @Test
    fun tamperedCiphertextMiddleByteThrows() {
        assertFlippedByteAtOffsetThrows(100)
    }
}
