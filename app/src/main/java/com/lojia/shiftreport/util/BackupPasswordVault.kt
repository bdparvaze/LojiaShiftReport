package com.lojia.shiftreport.util

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

const val VAULT_VERSION: Byte = 1

private const val VAULT_IV_SIZE = 12
private const val VAULT_TAG_BITS = 128
private const val VAULT_TAG_BYTES = 16
private const val VAULT_HEADER_SIZE = 1 + VAULT_IV_SIZE // 13 bytes
private const val VAULT_FILE_NAME = "backup_password.vault"

private val VAULT_AAD = "lojia-backup-password-v1".toByteArray(Charsets.UTF_8)
private val vaultRandom = SecureRandom()

/**
 * Encrypts [password] under [kek] using AES-GCM with a random 12-byte IV and AAD.
 * Converts [password] to UTF-8 bytes and zeroes the intermediate byte array; leaves [password] intact.
 */
fun seal(password: CharArray, kek: SecretKey): ByteArray {
    val charBuffer = CharBuffer.wrap(password)
    val encodedBuffer = Charsets.UTF_8.encode(charBuffer)
    val passwordBytes = ByteArray(encodedBuffer.remaining())
    encodedBuffer.get(passwordBytes)
    if (encodedBuffer.hasArray()) {
        encodedBuffer.array().fill(0)
    }

    return try {
        val iv = ByteArray(VAULT_IV_SIZE).apply { vaultRandom.nextBytes(this) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, kek, GCMParameterSpec(VAULT_TAG_BITS, iv))
        cipher.updateAAD(VAULT_AAD)
        val ciphertext = cipher.doFinal(passwordBytes)

        ByteArray(VAULT_HEADER_SIZE + ciphertext.size).apply {
            this[0] = VAULT_VERSION
            System.arraycopy(iv, 0, this, 1, VAULT_IV_SIZE)
            System.arraycopy(ciphertext, 0, this, VAULT_HEADER_SIZE, ciphertext.size)
        }
    } finally {
        passwordBytes.fill(0)
    }
}

/**
 * Decrypts a vault [blob] with [kek] and returns a new [CharArray] containing the password.
 * Zeroes intermediate plain byte and char buffers before returning.
 */
fun open(blob: ByteArray, kek: SecretKey): CharArray {
    if (blob.size < VAULT_HEADER_SIZE + VAULT_TAG_BYTES) {
        throw IllegalArgumentException("Vault blob is too short")
    }
    if (blob[0] != VAULT_VERSION) {
        throw IllegalArgumentException("Unsupported vault version: ${blob[0]}")
    }

    val iv = blob.copyOfRange(1, VAULT_HEADER_SIZE)
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.DECRYPT_MODE, kek, GCMParameterSpec(VAULT_TAG_BITS, iv))
    cipher.updateAAD(VAULT_AAD)

    var plainBytes: ByteArray? = null
    var charBuffer: CharBuffer? = null
    return try {
        plainBytes = cipher.doFinal(blob, VAULT_HEADER_SIZE, blob.size - VAULT_HEADER_SIZE)
        charBuffer = Charsets.UTF_8.decode(ByteBuffer.wrap(plainBytes))
        CharArray(charBuffer.remaining()).also { result ->
            charBuffer.get(result)
        }
    } finally {
        plainBytes?.fill(0)
        if (charBuffer != null && charBuffer.hasArray()) {
            charBuffer.array().fill('\u0000')
        }
    }
}

/**
 * Atomically persists the encrypted vault [blob] to `filesDir/backup_password.vault`.
 */
fun saveVault(context: Context, blob: ByteArray): Boolean {
    return try {
        val dir = context.filesDir
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val target = File(dir, VAULT_FILE_NAME)
        val temp = File(dir, "$VAULT_FILE_NAME.tmp")
        FileOutputStream(temp).use { fos ->
            fos.write(blob)
            fos.fd.sync()
        }
        if (!temp.renameTo(target)) {
            target.delete()
            if (!temp.renameTo(target)) {
                temp.delete()
                return false
            }
        }
        true
    } catch (_: Exception) {
        false
    }
}

/**
 * Loads the encrypted vault bytes from `filesDir/backup_password.vault`, or returns null if absent.
 */
fun loadVault(context: Context): ByteArray? {
    return try {
        val file = File(context.filesDir, VAULT_FILE_NAME)
        if (!file.exists() || !file.isFile) {
            return null
        }
        file.readBytes()
    } catch (_: Exception) {
        null
    }
}

/**
 * Deletes `filesDir/backup_password.vault` and any temporary vault file.
 */
fun deleteVault(context: Context): Boolean {
    return try {
        val dir = context.filesDir
        val temp = File(dir, "$VAULT_FILE_NAME.tmp")
        if (temp.exists()) {
            temp.delete()
        }
        val file = File(dir, VAULT_FILE_NAME)
        !file.exists() || file.delete()
    } catch (_: Exception) {
        false
    }
}
