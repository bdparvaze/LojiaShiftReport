package com.lojia.shiftreport.util

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

const val BACKUP_MAGIC = "LSB2"
const val BACKUP_VERSION = 2
const val TIER_DEVICE = 0
const val TIER_PORTABLE = 1
const val PBKDF2_ITERATIONS = 600_000

private const val SALT_SIZE = 16
private const val IV_SIZE = 12
private const val DEK_SIZE = 32
private const val GCM_TAG_BITS = 128
private const val GCM_TAG_BYTES = 16
private const val WRAPPED_DEK_OFFSET = 34
private const val WRAPPED_DEK_SIZE = DEK_SIZE + GCM_TAG_BYTES // 48 bytes
private const val DATA_IV_OFFSET = 82
private const val HEADER_SIZE = 94

private val MAGIC_BYTES = BACKUP_MAGIC.toByteArray(Charsets.US_ASCII)
private val secureRandom = SecureRandom()

class BackupFormatException(message: String) : Exception(message)

/**
 * Generates a cryptographically secure 16-byte random salt for portable backups.
 */
fun generateBackupSalt(): ByteArray = ByteArray(SALT_SIZE).apply { secureRandom.nextBytes(this) }

/**
 * Derives a 256-bit AES key-encryption key (KEK) from [password] and [salt] using
 * PBKDF2WithHmacSHA256 with 600,000 iterations.
 *
 * Overwrites [password] with zero chars after derivation.
 */
fun deriveBackupKek(password: CharArray, salt: ByteArray): SecretKey {
    require(salt.size == SALT_SIZE) { "Salt must be $SALT_SIZE bytes" }
    val spec = PBEKeySpec(password, salt, PBKDF2_ITERATIONS, 256)
    var keyBytes: ByteArray? = null
    return try {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        keyBytes = factory.generateSecret(spec).encoded
        SecretKeySpec(keyBytes, "AES")
    } finally {
        spec.clearPassword()
        password.fill('\u0000')
        keyBytes?.fill(0)
    }
}

/**
 * Encrypts [plain] using envelope encryption:
 * - Random 256-bit DEK encrypted with [kek] (AES-256-GCM, 12-byte IV, 128-bit tag).
 * - Payload encrypted with the DEK (AES-256-GCM, 12-byte IV, 128-bit tag).
 * - Header bytes (offsets 0..93) passed as AAD to both GCM operations.
 */
fun encryptBackup(plain: ByteArray, kek: SecretKey, tier: Int, salt: ByteArray?): ByteArray {
    require(tier == TIER_DEVICE || tier == TIER_PORTABLE) {
        "Unsupported backup tier: $tier"
    }
    if (tier == TIER_PORTABLE) {
        require(salt != null && salt.size == SALT_SIZE) {
            "Portable tier requires a $SALT_SIZE-byte salt"
        }
    }

    val dekBytes = ByteArray(DEK_SIZE)
    val wrapIv = ByteArray(IV_SIZE)
    val dataIv = ByteArray(IV_SIZE)

    return try {
        secureRandom.nextBytes(dekBytes)
        secureRandom.nextBytes(wrapIv)
        secureRandom.nextBytes(dataIv)

        val header = ByteArray(HEADER_SIZE)
        System.arraycopy(MAGIC_BYTES, 0, header, 0, MAGIC_BYTES.size)
        header[4] = BACKUP_VERSION.toByte()
        header[5] = tier.toByte()
        if (tier == TIER_PORTABLE && salt != null) {
            System.arraycopy(salt, 0, header, 6, SALT_SIZE)
        }
        System.arraycopy(wrapIv, 0, header, 22, IV_SIZE)
        // Offsets 34..81 remain zero in header when wrapping DEK so the 94-byte header AAD is reproducible on decrypt.
        System.arraycopy(dataIv, 0, header, DATA_IV_OFFSET, IV_SIZE)

        val wrapCipher = Cipher.getInstance("AES/GCM/NoPadding")
        wrapCipher.init(Cipher.ENCRYPT_MODE, kek, GCMParameterSpec(GCM_TAG_BITS, wrapIv))
        wrapCipher.updateAAD(header)
        val wrappedDek = wrapCipher.doFinal(dekBytes)
        require(wrappedDek.size == WRAPPED_DEK_SIZE) {
            "Unexpected wrapped DEK length: ${wrappedDek.size}"
        }
        System.arraycopy(wrappedDek, 0, header, WRAPPED_DEK_OFFSET, WRAPPED_DEK_SIZE)

        val dekKey = SecretKeySpec(dekBytes, "AES")
        val dataCipher = Cipher.getInstance("AES/GCM/NoPadding")
        dataCipher.init(Cipher.ENCRYPT_MODE, dekKey, GCMParameterSpec(GCM_TAG_BITS, dataIv))
        dataCipher.updateAAD(header)
        val ciphertext = dataCipher.doFinal(plain)

        ByteArray(HEADER_SIZE + ciphertext.size).apply {
            System.arraycopy(header, 0, this, 0, HEADER_SIZE)
            System.arraycopy(ciphertext, 0, this, HEADER_SIZE, ciphertext.size)
        }
    } finally {
        dekBytes.fill(0)
    }
}

/**
 * Decrypts a backup [blob] using the KEK provided by [kekFor] for the blob's tier and salt.
 */
fun decryptBackup(blob: ByteArray, kekFor: (tier: Int, salt: ByteArray) -> SecretKey): ByteArray {
    if (blob.size < MAGIC_BYTES.size || !hasBackupMagic(blob)) {
        throw BackupFormatException("legacy or unknown format")
    }
    if (blob.size < 5) {
        throw BackupFormatException("truncated")
    }
    val version = blob[4].toInt() and 0xFF
    if (version != BACKUP_VERSION) {
        throw BackupFormatException("unsupported version")
    }
    if (blob.size < HEADER_SIZE + GCM_TAG_BYTES) {
        throw BackupFormatException("truncated")
    }

    val tier = blob[5].toInt() and 0xFF
    if (tier != TIER_DEVICE && tier != TIER_PORTABLE) {
        throw BackupFormatException("unsupported tier")
    }

    val salt = blob.copyOfRange(6, 6 + SALT_SIZE)
    val wrapIv = blob.copyOfRange(22, 22 + IV_SIZE)
    val wrappedDek = blob.copyOfRange(WRAPPED_DEK_OFFSET, WRAPPED_DEK_OFFSET + WRAPPED_DEK_SIZE)
    val dataIv = blob.copyOfRange(DATA_IV_OFFSET, DATA_IV_OFFSET + IV_SIZE)
    val header = blob.copyOfRange(0, HEADER_SIZE)
    val unwrapAad = header.copyOf().apply {
        fill(0, WRAPPED_DEK_OFFSET, WRAPPED_DEK_OFFSET + WRAPPED_DEK_SIZE)
    }

    val kek = kekFor(tier, salt)
    var dekBytes: ByteArray? = null
    return try {
        val unwrapCipher = Cipher.getInstance("AES/GCM/NoPadding")
        unwrapCipher.init(Cipher.DECRYPT_MODE, kek, GCMParameterSpec(GCM_TAG_BITS, wrapIv))
        unwrapCipher.updateAAD(unwrapAad)
        dekBytes = unwrapCipher.doFinal(wrappedDek)
        if (dekBytes.size != DEK_SIZE) {
            throw BackupFormatException("invalid wrapped key")
        }

        val dekKey = SecretKeySpec(dekBytes, "AES")
        val dataCipher = Cipher.getInstance("AES/GCM/NoPadding")
        dataCipher.init(Cipher.DECRYPT_MODE, dekKey, GCMParameterSpec(GCM_TAG_BITS, dataIv))
        dataCipher.updateAAD(header)
        dataCipher.doFinal(blob, HEADER_SIZE, blob.size - HEADER_SIZE)
    } finally {
        dekBytes?.fill(0)
    }
}

/**
 * Returns the backup tier byte if [blob] starts with "LSB2" and is at least 6 bytes long, or null otherwise.
 */
fun backupTierOf(blob: ByteArray): Int? {
    if (blob.size < 6 || !hasBackupMagic(blob)) return null
    return blob[5].toInt() and 0xFF
}

private fun hasBackupMagic(blob: ByteArray): Boolean {
    if (blob.size < MAGIC_BYTES.size) return false
    for (i in MAGIC_BYTES.indices) {
        if (blob[i] != MAGIC_BYTES[i]) return false
    }
    return true
}
