package com.lojia.shiftreport.settings.backup

/**
 * Storage locations supported by Lojia international backup architecture.
 */
enum class BackupLocation(val id: String) {
    LOCAL("local"),
    GOOGLE_DRIVE("google_drive"),
    ONEDRIVE("onedrive"),
    BOTH("both");

    companion object {
        fun fromId(id: String?): BackupLocation {
            return entries.find { it.id == id } ?: LOCAL
        }
    }
}

/**
 * Live health and metadata status of a backup target.
 */
data class BackupStatus(
    val location: BackupLocation,
    val lastBackupTime: Long,
    val fileSize: Long,
    val isSuccess: Boolean,
    val isConnected: Boolean,
    val statusMessage: String? = null
)

/**
 * Preferences keys for backup location selection and automated schedules.
 */
object BackupPreferences {
    const val PREFS_NAME = "lojia_backup_preferences"
    const val KEY_SELECTED_LOCATION = "selected_backup_location"
    const val KEY_AUTO_BACKUP_ENABLED = "auto_backup_enabled"
    const val KEY_AUTO_BACKUP_FREQUENCY = "auto_backup_frequency" // "daily", "weekly", "manual"
    const val KEY_LOCAL_LAST_BACKUP = "local_last_backup_time"
    const val KEY_DRIVE_LAST_BACKUP = "drive_last_backup_time"
    const val KEY_ONEDRIVE_LAST_BACKUP = "onedrive_last_backup_time"
    const val KEY_ONEDRIVE_CONNECTED = "onedrive_is_connected"
    const val KEY_ONEDRIVE_ACCOUNT = "onedrive_account_email"
}
