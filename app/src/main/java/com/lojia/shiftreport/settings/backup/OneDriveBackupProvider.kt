package com.lojia.shiftreport.settings.backup

import android.app.Activity
import android.content.Context
import com.lojia.shiftreport.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * Microsoft OneDrive Cloud backup provider using Microsoft Graph REST API and MSAL.
 * Stores POS backups under /Apps/LojiaShiftReport/
 * Gracefully handles offline and disconnected states.
 */
class OneDriveBackupProvider(private val context: Context) : BackupProvider {

    companion object {
        private const val TAG = "OneDriveBackupProvider"
        const val GRAPH_BASE_URL = "https://graph.microsoft.com/v1.0"
        const val BACKUP_FOLDER_PATH = "Apps/LojiaShiftReport"
    }

    override val displayName: String = "Microsoft OneDrive"
    override val location: BackupLocation = BackupLocation.ONEDRIVE

    private val prefs = context.getSharedPreferences(BackupPreferences.PREFS_NAME, Context.MODE_PRIVATE)

    override fun isConnected(): Boolean {
        return prefs.getBoolean(BackupPreferences.KEY_ONEDRIVE_CONNECTED, false) &&
                getStoredAccessToken().isNotBlank()
    }

    override fun getLastBackupTime(): Long {
        return prefs.getLong(BackupPreferences.KEY_ONEDRIVE_LAST_BACKUP, 0L)
    }

    fun getConnectedAccountEmail(): String? {
        return prefs.getString(BackupPreferences.KEY_ONEDRIVE_ACCOUNT, null)
    }

    private fun getStoredAccessToken(): String {
        return prefs.getString("onedrive_access_token", "") ?: ""
    }

    fun setConnectedAccount(email: String, token: String) {
        prefs.edit()
            .putBoolean(BackupPreferences.KEY_ONEDRIVE_CONNECTED, true)
            .putString(BackupPreferences.KEY_ONEDRIVE_ACCOUNT, email)
            .putString("onedrive_access_token", token)
            .apply()
        AppLogger.i(TAG, "OneDrive account connected: $email")
    }

    fun disconnect() {
        prefs.edit()
            .putBoolean(BackupPreferences.KEY_ONEDRIVE_CONNECTED, false)
            .remove(BackupPreferences.KEY_ONEDRIVE_ACCOUNT)
            .remove("onedrive_access_token")
            .apply()
        AppLogger.i(TAG, "OneDrive disconnected.")
    }

    /**
     * Uploads the backup JSON file to Microsoft Graph Drive under /Apps/LojiaShiftReport/
     */
    override suspend fun backup(file: File): Result<Boolean> = withContext(Dispatchers.IO) {
        if (!isConnected()) {
            return@withContext Result.failure(IllegalStateException("Microsoft OneDrive is not connected."))
        }

        val token = getStoredAccessToken()
        try {
            val endpoint = "$GRAPH_BASE_URL/me/drive/root:/$BACKUP_FOLDER_PATH/${file.name}:/content"
            val url = URL(endpoint)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "PUT"
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 15000
            connection.readTimeout = 20000

            file.inputStream().use { input ->
                connection.outputStream.use { output ->
                    input.copyTo(output)
                }
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..204) {
                val now = System.currentTimeMillis()
                prefs.edit().putLong(BackupPreferences.KEY_ONEDRIVE_LAST_BACKUP, now).apply()
                AppLogger.i(TAG, "OneDrive backup upload successful: ${file.name}")
                Result.success(true)
            } else {
                val errorMsg = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $responseCode"
                AppLogger.e(TAG, "OneDrive upload failed with HTTP $responseCode: $errorMsg")
                Result.failure(Exception("OneDrive upload failed: $errorMsg"))
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "OneDrive backup exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Restores latest backup file from Microsoft OneDrive /Apps/LojiaShiftReport folder.
     */
    override suspend fun restore(): File? = withContext(Dispatchers.IO) {
        if (!isConnected()) return@withContext null
        val token = getStoredAccessToken()

        try {
            val listEndpoint = "$GRAPH_BASE_URL/me/drive/root:/$BACKUP_FOLDER_PATH:/children?\$orderby=lastModifiedDateTime%20desc&\$top=1"
            val url = URL(listEndpoint)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.connectTimeout = 15000

            if (connection.responseCode !in 200..299) return@withContext null

            val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(jsonString)
            val values = root.optJSONArray("value") ?: return@withContext null
            if (values.length() == 0) return@withContext null

            val item = values.getJSONObject(0)
            val downloadUrl = item.optString("@microsoft.graph.downloadUrl")
            val fileName = item.optString("name", "onedrive_backup.json")

            if (downloadUrl.isNotBlank()) {
                val dlConn = URL(downloadUrl).openConnection() as HttpURLConnection
                val cacheFile = File(context.cacheDir, "onedrive_restore_$fileName")
                FileOutputStream(cacheFile).use { out ->
                    dlConn.inputStream.use { input ->
                        input.copyTo(out)
                    }
                }
                cacheFile
            } else {
                null
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to restore from OneDrive: ${e.message}", e)
            null
        }
    }
}
