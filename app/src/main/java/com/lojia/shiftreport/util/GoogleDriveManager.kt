package com.lojia.shiftreport.util

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.util.Base64
import com.lojia.shiftreport.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.*

data class DriveBackupItem(
    val id: String,
    val name: String,
    val size: Long,
    val modifiedTime: String,
    val description: String = ""
)

data class DriveAccountInfo(
    val isConnected: Boolean,
    val email: String? = null,
    val displayName: String? = null,
    val photoUrl: String? = null
)

object GoogleDriveManager {

    private const val PREFS_NAME = "google_drive_prefs"
    private const val KEY_ACCESS_TOKEN = "drive_access_token"
    private const val KEY_REFRESH_TOKEN = "drive_refresh_token"
    private const val KEY_TOKEN_EXPIRY = "drive_token_expiry"
    private const val KEY_ACCOUNT_EMAIL = "drive_account_email"
    private const val KEY_ACCOUNT_NAME = "drive_account_name"
    private const val KEY_CODE_VERIFIER = "drive_code_verifier"

    // OAuth configuration
    const val CLIENT_ID = "654989706304-6knvt1b4gravtsu1a59snfg1acgjre74.apps.googleusercontent.com"
    const val REDIRECT_URI = "com.lojia.shiftreport:/oauth2callback"
    private const val SCOPES = "https://www.googleapis.com/auth/drive.appdata https://www.googleapis.com/auth/drive.file https://www.googleapis.com/auth/userinfo.email"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isConnected(context: Context): Boolean {
        val prefs = getPrefs(context)
        val token = prefs.getString(KEY_ACCESS_TOKEN, null)
        val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null)
        return !token.isNullOrEmpty() || !refreshToken.isNullOrEmpty()
    }

    fun getAccountInfo(context: Context): DriveAccountInfo {
        val prefs = getPrefs(context)
        val connected = isConnected(context)
        val email = prefs.getString(KEY_ACCOUNT_EMAIL, null)
        val name = prefs.getString(KEY_ACCOUNT_NAME, null)
        return DriveAccountInfo(
            isConnected = connected,
            email = email,
            displayName = name
        )
    }

    fun disconnect(context: Context) {
        getPrefs(context).edit().clear().apply()
    }

    /**
     * PKCE Authorization Code generation
     */
    private fun generateCodeVerifier(): String {
        val secureRandom = SecureRandom()
        val code = ByteArray(32)
        secureRandom.nextBytes(code)
        return Base64.encodeToString(code, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    private fun generateCodeChallenge(verifier: String): String {
        val bytes = verifier.toByteArray(StandardCharsets.US_ASCII)
        val md = MessageDigest.getInstance("SHA-256")
        md.update(bytes, 0, bytes.size)
        val digest = md.digest()
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    fun buildAuthUrl(context: Context): String {
        val verifier = generateCodeVerifier()
        getPrefs(context).edit().putString(KEY_CODE_VERIFIER, verifier).apply()
        val challenge = generateCodeChallenge(verifier)

        return "https://accounts.google.com/o/oauth2/v2/auth?" +
                "client_id=" + URLEncoder.encode(CLIENT_ID, "UTF-8") +
                "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, "UTF-8") +
                "&response_type=code" +
                "&scope=" + URLEncoder.encode(SCOPES, "UTF-8") +
                "&code_challenge=" + URLEncoder.encode(challenge, "UTF-8") +
                "&code_challenge_method=S256" +
                "&access_type=offline" +
                "&prompt=consent"
    }

    fun launchAuthorization(context: Context) {
        val url = buildAuthUrl(context)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    suspend fun handleOAuthCallback(context: Context, code: String): Result<DriveAccountInfo> = withContext(Dispatchers.IO) {
        try {
            val prefs = getPrefs(context)
            val verifier = prefs.getString(KEY_CODE_VERIFIER, "") ?: ""

            val tokenUrl = URL("https://oauth2.googleapis.com/token")
            val conn = tokenUrl.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

            val params = "client_id=" + URLEncoder.encode(CLIENT_ID, "UTF-8") +
                    "&code=" + URLEncoder.encode(code, "UTF-8") +
                    "&code_verifier=" + URLEncoder.encode(verifier, "UTF-8") +
                    "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, "UTF-8") +
                    "&grant_type=authorization_code"

            OutputStreamWriter(conn.outputStream).use { it.write(params) }

            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                val accessToken = json.getString("access_token")
                val expiresIn = json.optLong("expires_in", 3600L)
                val refreshToken = json.optString("refresh_token", "")
                val expiryTime = System.currentTimeMillis() + (expiresIn * 1000)

                val editor = prefs.edit()
                    .putString(KEY_ACCESS_TOKEN, accessToken)
                    .putLong(KEY_TOKEN_EXPIRY, expiryTime)

                if (refreshToken.isNotEmpty()) {
                    editor.putString(KEY_REFRESH_TOKEN, refreshToken)
                }
                editor.apply()

                // Fetch Account info
                val accountInfo = fetchDriveUserInfo(accessToken)
                editor.putString(KEY_ACCOUNT_EMAIL, accountInfo.email)
                    .putString(KEY_ACCOUNT_NAME, accountInfo.displayName)
                    .apply()

                Result.success(accountInfo)
            } else {
                val errorText = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $responseCode"
                Result.failure(Exception("OAuth exchange failed ($responseCode): $errorText"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun connectWithToken(context: Context, token: String): Result<DriveAccountInfo> = withContext(Dispatchers.IO) {
        try {
            val accountInfo = fetchDriveUserInfo(token)
            val prefs = getPrefs(context)
            prefs.edit()
                .putString(KEY_ACCESS_TOKEN, token)
                .putLong(KEY_TOKEN_EXPIRY, System.currentTimeMillis() + (3600 * 1000))
                .putString(KEY_ACCOUNT_EMAIL, accountInfo.email)
                .putString(KEY_ACCOUNT_NAME, accountInfo.displayName)
                .apply()
            Result.success(accountInfo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun fetchDriveUserInfo(accessToken: String): DriveAccountInfo {
        return try {
            val url = URL("https://www.googleapis.com/drive/v3/about?fields=user")
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("Authorization", "Bearer $accessToken")
            conn.requestMethod = "GET"

            if (conn.responseCode in 200..299) {
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(text)
                val user = root.optJSONObject("user")
                val email = user?.optString("emailAddress", "Google Drive User") ?: "Google Drive User"
                val name = user?.optString("displayName", "User") ?: "User"
                val photo = user?.optString("photoLink", null)
                DriveAccountInfo(isConnected = true, email = email, displayName = name, photoUrl = photo)
            } else {
                DriveAccountInfo(isConnected = true, email = "Google Drive Connected", displayName = "User")
            }
        } catch (e: Exception) {
            DriveAccountInfo(isConnected = true, email = "Google Drive Connected", displayName = "User")
        }
    }

    private suspend fun getValidAccessToken(context: Context): String? = withContext(Dispatchers.IO) {
        val prefs = getPrefs(context)
        val token = prefs.getString(KEY_ACCESS_TOKEN, null) ?: return@withContext null
        val expiry = prefs.getLong(KEY_TOKEN_EXPIRY, 0L)
        val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null)

        // If token expires in less than 2 minutes, refresh it if refresh_token exists
        if (System.currentTimeMillis() > (expiry - 120_000) && !refreshToken.isNullOrEmpty()) {
            try {
                val tokenUrl = URL("https://oauth2.googleapis.com/token")
                val conn = tokenUrl.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

                val params = "client_id=" + URLEncoder.encode(CLIENT_ID, "UTF-8") +
                        "&refresh_token=" + URLEncoder.encode(refreshToken, "UTF-8") +
                        "&grant_type=refresh_token"

                OutputStreamWriter(conn.outputStream).use { it.write(params) }

                if (conn.responseCode in 200..299) {
                    val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(responseText)
                    val newAccessToken = json.getString("access_token")
                    val expiresIn = json.optLong("expires_in", 3600L)
                    val newExpiry = System.currentTimeMillis() + (expiresIn * 1000)

                    prefs.edit()
                        .putString(KEY_ACCESS_TOKEN, newAccessToken)
                        .putLong(KEY_TOKEN_EXPIRY, newExpiry)
                        .apply()

                    return@withContext newAccessToken
                }
            } catch (e: Exception) {
                // Return existing token as fallback
            }
        }
        token
    }

    /**
     * Uploads a business data backup to Google Drive App Folder.
     * Note: Requirement 5 states passwords and PINs must never be uploaded.
     */
    suspend fun uploadBackup(
        context: Context,
        database: AppDatabase
    ): Result<DriveBackupItem> = withContext(Dispatchers.IO) {
        try {
            val token = getValidAccessToken(context)
                ?: return@withContext Result.failure(IllegalStateException("Google Drive is not connected"))

            // Generate clean business-only backup json (plain passwords/PINs stripped)
            val (jsonString, totalRecords) = OfflineBackupManager.generateBackupJson(database, isCloudBackup = true)
            val jsonBytes = jsonString.toByteArray(StandardCharsets.UTF_8)

            val timeStampStr = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.US).format(Date())
            val fileName = "Lojia_Backup_$timeStampStr.json"

            // Multipart upload to Google Drive v3
            val boundary = "-------LojiaDriveBoundary" + System.currentTimeMillis()
            val url = URL("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")

            val metadata = JSONObject().apply {
                put("name", fileName)
                put("mimeType", "application/json")
                put("description", "Lojia POS Cloud Backup (Records: $totalRecords)")
                put("parents", JSONArray().put("appDataFolder"))
            }.toString()

            val os = conn.outputStream
            val writer = OutputStreamWriter(os, StandardCharsets.UTF_8)

            // Part 1: Metadata
            writer.write("--$boundary\r\n")
            writer.write("Content-Type: application/json; charset=UTF-8\r\n\r\n")
            writer.write(metadata)
            writer.write("\r\n")
            writer.flush()

            // Part 2: Media content
            writer.write("--$boundary\r\n")
            writer.write("Content-Type: application/json\r\n\r\n")
            writer.flush()
            os.write(jsonBytes)
            os.flush()

            // End boundary
            writer.write("\r\n--$boundary--\r\n")
            writer.flush()
            writer.close()

            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val respJson = JSONObject(responseText)
                val item = DriveBackupItem(
                    id = respJson.getString("id"),
                    name = respJson.optString("name", fileName),
                    size = jsonBytes.size.toLong(),
                    modifiedTime = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()),
                    description = "$totalRecords records"
                )
                Result.success(item)
            } else {
                val errorMsg = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $responseCode"
                Result.failure(Exception("Drive upload failed ($responseCode): $errorMsg"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Lists backup files found in Google Drive appDataFolder and drive.
     */
    suspend fun listBackups(context: Context): Result<List<DriveBackupItem>> = withContext(Dispatchers.IO) {
        try {
            val token = getValidAccessToken(context)
                ?: return@withContext Result.failure(IllegalStateException("Google Drive is not connected"))

            val query = "trashed=false and (name contains 'Lojia_Backup' or mimeType='application/json')"
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val urlString = "https://www.googleapis.com/drive/v3/files?" +
                    "spaces=appDataFolder,drive" +
                    "&q=$encodedQuery" +
                    "&fields=files(id,name,size,createdTime,modifiedTime,description)" +
                    "&orderBy=" + URLEncoder.encode("createdTime desc", "UTF-8")

            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Authorization", "Bearer $token")

            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(text)
                val filesArr = root.optJSONArray("files") ?: JSONArray()
                val list = mutableListOf<DriveBackupItem>()

                for (i in 0 until filesArr.length()) {
                    val obj = filesArr.getJSONObject(i)
                    val id = obj.getString("id")
                    val name = obj.optString("name", "Backup")
                    val size = obj.optLong("size", 0L)
                    val modTime = obj.optString("modifiedTime", obj.optString("createdTime", ""))
                    val desc = obj.optString("description", "")
                    list.add(DriveBackupItem(id = id, name = name, size = size, modifiedTime = modTime, description = desc))
                }
                Result.success(list)
            } else {
                val errorMsg = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $responseCode"
                Result.failure(Exception("Failed to list backups ($responseCode): $errorMsg"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Downloads backup file content from Google Drive.
     */
    suspend fun downloadBackup(context: Context, fileId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val token = getValidAccessToken(context)
                ?: return@withContext Result.failure(IllegalStateException("Google Drive is not connected"))

            val url = URL("https://www.googleapis.com/drive/v3/files/$fileId?alt=media")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Authorization", "Bearer $token")

            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                Result.success(text)
            } else {
                val errorMsg = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $responseCode"
                Result.failure(Exception("Failed to download backup ($responseCode): $errorMsg"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
