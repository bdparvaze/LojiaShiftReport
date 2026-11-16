package com.lojia.shiftreport

import com.lojia.shiftreport.util.BackupDestination
import com.lojia.shiftreport.util.BackupFileDecoder
import com.lojia.shiftreport.util.BackupFormatException
import com.lojia.shiftreport.util.TIER_DEVICE
import com.lojia.shiftreport.util.TIER_PORTABLE
import com.lojia.shiftreport.util.deriveBackupKek
import com.lojia.shiftreport.util.encryptBackup
import com.lojia.shiftreport.util.requiresPassword
import com.lojia.shiftreport.util.tierFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.SecureRandom
import javax.crypto.spec.SecretKeySpec

class Stage19BBackupRouterUnitTest {

    private fun randomBytes(size: Int): ByteArray = ByteArray(size).apply { SecureRandom().nextBytes(this) }

    @Test
    fun lsb2PortableBlobWithRightPasswordReturnsOriginalJson() {
        val originalJson = """{"app":"Lojia Shift Report","version":1,"total":12345}"""
        val salt = randomBytes(16)
        val kek = deriveBackupKek("correct-horse-1".toCharArray(), salt)
        val blob = encryptBackup(originalJson.toByteArray(Charsets.UTF_8), kek, TIER_PORTABLE, salt)

        val decoded = BackupFileDecoder.decodeToJson(
            bytes = blob,
            kekFor = { tier, extractedSalt ->
                assertEquals(TIER_PORTABLE, tier)
                deriveBackupKek("correct-horse-1".toCharArray(), extractedSalt)
            },
            legacyDecrypt = { throw AssertionError("legacyDecrypt must not be called") }
        )

        assertEquals(originalJson, decoded)
    }

    @Test
    fun lsb2PortableBlobWithWrongPasswordThrows() {
        val originalJson = """{"app":"Lojia Shift Report","version":1}"""
        val salt = randomBytes(16)
        val kek = deriveBackupKek("correct-horse-1".toCharArray(), salt)
        val blob = encryptBackup(originalJson.toByteArray(Charsets.UTF_8), kek, TIER_PORTABLE, salt)

        assertThrows(Exception::class.java) {
            BackupFileDecoder.decodeToJson(
                bytes = blob,
                kekFor = { _, extractedSalt ->
                    deriveBackupKek("wrong-password-1".toCharArray(), extractedSalt)
                },
                legacyDecrypt = { throw AssertionError("legacyDecrypt must not be called") }
            )
        }
    }

    @Test
    fun lsb2DeviceBlobWithDeviceKeyReturnsOriginalJson() {
        val originalJson = """{"app":"Lojia Shift Report","tier":"device"}"""
        val deviceKek = SecretKeySpec(randomBytes(32), "AES")
        val blob = encryptBackup(originalJson.toByteArray(Charsets.UTF_8), deviceKek, TIER_DEVICE, null)

        val decoded = BackupFileDecoder.decodeToJson(
            bytes = blob,
            kekFor = { tier, _ ->
                assertEquals(TIER_DEVICE, tier)
                deviceKek
            },
            legacyDecrypt = { throw AssertionError("legacyDecrypt must not be called") }
        )

        assertEquals(originalJson, decoded)
    }

    @Test
    fun lsb2DeviceBlobWhereKekForThrowsPropagatesError() {
        val originalJson = """{"app":"Lojia Shift Report","tier":"device"}"""
        val deviceKek = SecretKeySpec(randomBytes(32), "AES")
        val blob = encryptBackup(originalJson.toByteArray(Charsets.UTF_8), deviceKek, TIER_DEVICE, null)

        val error = assertThrows(IllegalStateException::class.java) {
            BackupFileDecoder.decodeToJson(
                bytes = blob,
                kekFor = { _, _ -> throw IllegalStateException("device key missing") },
                legacyDecrypt = { throw AssertionError("legacyDecrypt must not be called") }
            )
        }
        assertEquals("device key missing", error.message)
    }

    @Test
    fun plainJsonReturnedAsIsWithoutCallingLegacyDecrypt() {
        val originalJson = """{"app":"Lojia Shift Report","plain":true}"""
        var legacyCalled = false

        val decoded = BackupFileDecoder.decodeToJson(
            bytes = originalJson.toByteArray(Charsets.UTF_8),
            kekFor = { _, _ -> throw AssertionError("kekFor must not be called") },
            legacyDecrypt = {
                legacyCalled = true
                ""
            }
        )

        assertFalse(legacyCalled)
        assertEquals(originalJson, decoded)
    }

    @Test
    fun plainJsonWithUtf8BomAndLeadingSpacesReturnedAsIs() {
        val rawBytes = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) +
            "   \n\t  {\"app\":\"Lojia Shift Report\",\"bom\":true}".toByteArray(Charsets.UTF_8)
        var legacyCalled = false

        val decoded = BackupFileDecoder.decodeToJson(
            bytes = rawBytes,
            kekFor = { _, _ -> throw AssertionError("kekFor must not be called") },
            legacyDecrypt = {
                legacyCalled = true
                ""
            }
        )

        assertFalse(legacyCalled)
        assertEquals(String(rawBytes, Charsets.UTF_8), decoded)
    }

    @Test
    fun garbageBytesCallsLegacyDecryptOnceAndPropagatesException() {
        val garbage = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07)
        var legacyCallCount = 0

        val error = assertThrows(IllegalArgumentException::class.java) {
            BackupFileDecoder.decodeToJson(
                bytes = garbage,
                kekFor = { _, _ -> throw AssertionError("kekFor must not be called") },
                legacyDecrypt = {
                    legacyCallCount++
                    throw IllegalArgumentException("unrecognized legacy payload")
                }
            )
        }

        assertEquals(1, legacyCallCount)
        assertEquals("unrecognized legacy payload", error.message)
    }

    @Test
    fun truncatedLsb2ThrowsBackupFormatException() {
        val truncated = "LSB2".toByteArray(Charsets.US_ASCII) + ByteArray(10)

        assertThrows(BackupFormatException::class.java) {
            BackupFileDecoder.decodeToJson(
                bytes = truncated,
                kekFor = { _, _ -> SecretKeySpec(randomBytes(32), "AES") },
                legacyDecrypt = { throw AssertionError("legacyDecrypt must not be called") }
            )
        }
    }

    @Test
    fun backupTierPolicyMappingsMatchSpecification() {
        assertEquals(TIER_DEVICE, tierFor(BackupDestination.DEVICE_SNAPSHOT))
        assertFalse(requiresPassword(BackupDestination.DEVICE_SNAPSHOT))

        val portableDestinations = listOf(
            BackupDestination.UNINSTALL_COPY,
            BackupDestination.FILE_EXPORT,
            BackupDestination.CLOUD_UPLOAD,
            BackupDestination.MANUAL_EXPORT
        )
        for (destination in portableDestinations) {
            assertEquals(TIER_PORTABLE, tierFor(destination))
            assertTrue(requiresPassword(destination))
        }
    }
}
