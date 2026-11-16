package com.lojia.shiftreport.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Checks whether the encrypted backup password vault exists and can be opened
 * with the device Keystore KEK on [Dispatchers.IO].
 *
 * Deletes any stale/unreadable vault (such as one left behind after reinstall when the
 * Keystore key is absent) and returns false.
 */
suspend fun canCreatePortableBackup(context: Context): Boolean = withContext(Dispatchers.IO) {
    val blob = loadVault(context) ?: return@withContext false
    val deviceKek = getOrCreateDeviceKek() ?: return@withContext false
    var opened: CharArray? = null
    return@withContext try {
        opened = open(blob, deviceKek)
        true
    } catch (_: Exception) {
        deleteVault(context)
        false
    } finally {
        opened?.fill('\u0000')
    }
}

/**
 * Opens the stored backup password from the vault on [Dispatchers.IO] for creating a portable backup,
 * passes it to [block], and always zeroes the password [CharArray] in a finally block.
 *
 * Never intended for restore paths.
 */
internal suspend fun <T> withVaultPassword(
    context: Context,
    block: suspend (CharArray) -> T
): Result<T> = withContext(Dispatchers.IO) {
    val blob = loadVault(context)
        ?: return@withContext Result.failure(BackupPasswordRequiredException("Backup password not set"))
    val deviceKek = getOrCreateDeviceKek()
        ?: return@withContext Result.failure(IllegalStateException("Device encryption key unavailable"))
    var passwordChars: CharArray? = null
    try {
        passwordChars = try {
            open(blob, deviceKek)
        } catch (_: Exception) {
            deleteVault(context)
            return@withContext Result.failure(BackupPasswordRequiredException("Backup password not set"))
        }
        Result.success(block(passwordChars))
    } catch (e: Exception) {
        Result.failure(e)
    } finally {
        passwordChars?.fill('\u0000')
    }
}
