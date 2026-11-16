package com.lojia.shiftreport.util

import javax.crypto.SecretKey

class BackupPasswordRequiredException(message: String) : Exception(message)

object BackupFileDecoder {

    fun decodeToJson(
        bytes: ByteArray,
        kekFor: (tier: Int, salt: ByteArray) -> SecretKey,
        legacyDecrypt: (ByteArray) -> String,
    ): String {
        if (backupTierOf(bytes) != null) {
            return String(decryptBackup(bytes, kekFor), Charsets.UTF_8)
        }
        if (looksLikePlainJsonObject(bytes)) {
            return String(bytes, Charsets.UTF_8)
        }
        return legacyDecrypt(bytes)
    }

    private fun looksLikePlainJsonObject(bytes: ByteArray): Boolean {
        var index = 0
        if (bytes.size >= 3 &&
            bytes[0] == 0xEF.toByte() &&
            bytes[1] == 0xBB.toByte() &&
            bytes[2] == 0xBF.toByte()
        ) {
            index = 3
        }
        while (index < bytes.size) {
            val b = bytes[index]
            if (b == 0x20.toByte() || b == 0x09.toByte() || b == 0x0A.toByte() || b == 0x0D.toByte()) {
                index++
            } else {
                break
            }
        }
        return index < bytes.size && bytes[index] == '{'.code.toByte()
    }
}
