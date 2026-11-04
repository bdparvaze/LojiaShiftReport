package com.lojia.shiftreport.util

import com.lojia.shiftreport.data.PreferencesRepository
import com.lojia.shiftreport.settings.backup.BackupManager
import java.io.File
import org.json.JSONObject

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
            val fullText = latestFile.readText()
            val json = try {
                JSONObject(fullText)
            } catch (_: Exception) {
                JSONObject(fullText.take(2000))
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
