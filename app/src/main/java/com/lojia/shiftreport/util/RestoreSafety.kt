package com.lojia.shiftreport.util

import java.util.Locale

/**
 * Pure restore helpers used to preserve local credentials and active status
 * when restoring backups (including cloud backups where PINs/passwords are stripped).
 */
fun mergePinHash(localHash: String?, backupHash: String?): String? {
    return if (!backupHash.isNullOrBlank()) backupHash else localHash
}

/**
 * Resolves the restored isActive flag for a cashier/user:
 * - For an existing local record, preserve its local isActive value.
 * - For a record that does not exist locally, use the backup's isActive value, or true if missing.
 * A blank PIN in a backup never forces isActive to false.
 */
fun resolveRestoredIsActive(localIsActive: Boolean?, backupIsActive: Boolean?): Boolean {
    return localIsActive ?: backupIsActive ?: true
}

/**
 * Trims and lowercases a restore identifier (cashier name or username); null becomes "".
 */
fun normalizeRestoreName(name: String?): String {
    return name?.trim()?.lowercase(Locale.ROOT).orEmpty()
}

/**
 * Matches [backupName] against [localIdToName] by normalized name only (never by id).
 * Returns null when [backupName] is blank or no local row matches.
 */
fun matchLocalIdByName(
    backupName: String,
    localIdToName: Map<Int, String>
): Int? {
    val normalizedBackup = normalizeRestoreName(backupName)
    if (normalizedBackup.isEmpty()) return null
    for ((localId, localName) in localIdToName) {
        if (normalizeRestoreName(localName) == normalizedBackup) {
            return localId
        }
    }
    return null
}

/**
 * Remaps a ShiftReport.cashierId string through [cashierIdMap].
 * Returns "" when [cashierId] is null, blank, non-numeric, or not present in [cashierIdMap].
 */
fun mapShiftReportCashierId(
    cashierId: String?,
    cashierIdMap: Map<Int, Int>
): String {
    val trimmed = cashierId?.trim().orEmpty()
    if (trimmed.isEmpty() || cashierIdMap.isEmpty()) return ""
    val parsedId = trimmed.toIntOrNull() ?: return ""
    val mappedId = cashierIdMap[parsedId] ?: return ""
    return mappedId.toString()
}

/**
 * Returns true only when a stored or entered PIN/secret is non-null and contains
 * non-whitespace characters, preventing blank restored credentials from authenticating.
 */
fun isPinUsable(pin: String?): Boolean {
    return !pin.isNullOrBlank()
}

