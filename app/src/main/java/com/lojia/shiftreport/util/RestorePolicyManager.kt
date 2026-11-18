package com.lojia.shiftreport.util

import com.lojia.shiftreport.data.PreferencesRepository
import com.lojia.shiftreport.settings.backup.BackupManager
import java.io.File
import org.json.JSONObject

private const val JSON_PREVIEW_FALLBACK_CHAR_LIMIT = 2000

class RestorePolicyManager(private val prefs: PreferencesRepository, private val backupManager: BackupManager) {
    data class RestoreDecision(
        val shouldShowDialog: Boolean,
        val backupFile: File?,
        val previewText: String,
        val isOwnedByCurrentUser: Boolean
    )

    suspend fun evaluate(currentUserId: String, isNewAccount: Boolean): RestoreDecision {
        if (prefs.getHasSeenRestoreDialog()) return RestoreDecision(false, null, "", false)
        if (isNewAccount) {
            prefs.setHasSeenRestoreDialog(true)
            return RestoreDecision(false, null, "", false)
        }
        val latestFile = backupManager.getLatestLocalBackupFile() ?: return RestoreDecision(false, null, "", false)
        return try {
            val bytes = latestFile.readBytes()
            if (backupTierOf(bytes) == TIER_PORTABLE) {
                return RestoreDecision(true, latestFile, "Encrypted backup found", true)
            }
            val fullText = OfflineBackupManager.decodeBackupToJson(bytes, password = null)
            val json = try {
                JSONObject(fullText)
            } catch (_: Exception) {
                JSONObject(fullText.take(JSON_PREVIEW_FALLBACK_CHAR_LIMIT))
            }
            val ownerId = json.optString("ownerId", "anonymous")
            if (ownerId != "anonymous" && ownerId != currentUserId) return RestoreDecision(false, null, "", false)
            val shiftCount = json.optJSONArray("shifts")?.length()
                ?: json.optJSONArray("shiftReports")?.length()
                ?: 0
            val preview = if (shiftCount > 0) "$shiftCount shifts ready to restore" else "Backup found"
            RestoreDecision(true, latestFile, preview, true)
        } catch (e: Exception) {
            RestoreDecision(false, null, "", false)
        }
    }
}
