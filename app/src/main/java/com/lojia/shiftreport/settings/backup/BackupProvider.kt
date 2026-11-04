package com.lojia.shiftreport.settings.backup

import java.io.File

/**
 * Standard interface for all POS data backup destinations (Local, Google Drive, OneDrive).
 */
interface BackupProvider {
    val displayName: String
    val location: BackupLocation

    fun isConnected(): Boolean
    fun getLastBackupTime(): Long

    suspend fun backup(file: File): Result<Boolean>
    suspend fun restore(): File?
}
