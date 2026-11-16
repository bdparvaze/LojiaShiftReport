package com.lojia.shiftreport.util

enum class BackupDestination {
    DEVICE_SNAPSHOT,
    UNINSTALL_COPY,
    FILE_EXPORT,
    CLOUD_UPLOAD,
    MANUAL_EXPORT
}

fun tierFor(destination: BackupDestination): Int = when (destination) {
    BackupDestination.DEVICE_SNAPSHOT -> TIER_DEVICE
    BackupDestination.UNINSTALL_COPY,
    BackupDestination.FILE_EXPORT,
    BackupDestination.CLOUD_UPLOAD,
    BackupDestination.MANUAL_EXPORT -> TIER_PORTABLE
}

fun requiresPassword(destination: BackupDestination): Boolean =
    tierFor(destination) == TIER_PORTABLE
