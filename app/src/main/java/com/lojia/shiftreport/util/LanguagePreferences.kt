package com.lojia.shiftreport.util

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lojia.shiftreport.data.AppLanguage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.Locale

val Context.languageDataStore: DataStore<Preferences> by preferencesDataStore(name = "language_preferences")

data class LanguagePreferenceData(
    val currentCode: String = LanguagePreferences.DEFAULT_PRIMARY_LANGUAGE_CODE,
    val secondaryCode: String = LanguagePreferences.DEFAULT_SECONDARY_LANGUAGE_CODE,
    val lastUpdatedMillis: Long = System.currentTimeMillis()
)

object LanguagePreferences {
    /** ISO 639-1 fallback primary language code when no explicit user preference is stored. */
    const val DEFAULT_PRIMARY_LANGUAGE_CODE = "en"

    /** ISO 639-1 fallback secondary toggle language code when no explicit toggle preference is stored. */
    const val DEFAULT_SECONDARY_LANGUAGE_CODE = "ar"

    private const val PREFS_NAME = "language_prefs"
    private const val KEY_LANGUAGE = "selected_language"
    private const val USER_LANGUAGE_KEY_PREFIX = "user_language_"

    val LANGUAGE_KEY = stringPreferencesKey("selected_language_tag")
    val SECONDARY_LANGUAGE_KEY = stringPreferencesKey("secondary_language_tag")
    val LAST_UPDATED_KEY = longPreferencesKey("language_last_updated")

    private val scope = CoroutineScope(Dispatchers.IO)

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun userLanguageKey(userId: String): Preferences.Key<String> =
        stringPreferencesKey("$USER_LANGUAGE_KEY_PREFIX${userId.trim()}")

    fun resolveUserId(username: String?, userProfileId: Int? = null): String? {
        val normalizedUsername = username?.trim()?.lowercase(Locale.ROOT)?.ifBlank { null }
        if (normalizedUsername != null) return normalizedUsername
        return if (userProfileId != null && userProfileId > 0) "profile_$userProfileId" else null
    }

    fun resolveInitialLanguage(
        systemLanguageTags: List<String>,
        supportedCodes: List<String>,
        storedCode: String?
    ): String {
        val normalizedStored = storedCode?.trim()
        if (!normalizedStored.isNullOrBlank()) {
            val matchedStored = supportedCodes.firstOrNull { it.equals(normalizedStored, ignoreCase = true) }
            if (matchedStored != null) return matchedStored
        }
        for (tag in systemLanguageTags) {
            val baseCode = tag.trim()
                .substringBefore('-')
                .substringBefore('_')
                .lowercase(Locale.ROOT)
            if (baseCode.isNotBlank()) {
                val matchedSystem = supportedCodes.firstOrNull { it.equals(baseCode, ignoreCase = true) }
                if (matchedSystem != null) return matchedSystem
            }
        }
        return supportedCodes.firstOrNull {
            it.equals(DEFAULT_PRIMARY_LANGUAGE_CODE, ignoreCase = true)
        } ?: DEFAULT_PRIMARY_LANGUAGE_CODE
    }

    fun getSystemLanguageTags(context: Context): List<String> {
        val tags = mutableListOf<String>()
        val resLocales = context.resources.configuration.locales
        for (i in 0 until resLocales.size()) {
            val tag = resLocales.get(i)?.toLanguageTag()
            if (!tag.isNullOrBlank()) tags.add(tag)
        }
        val defaultTag = Locale.getDefault().toLanguageTag()
        if (defaultTag.isNotBlank() && defaultTag !in tags) {
            tags.add(defaultTag)
        }
        return tags
    }

    private fun persistFastPrefSync(context: Context, languageCode: String, now: Long) {
        getPrefs(context).edit()
            .putString(KEY_LANGUAGE, languageCode)
            .putLong("last_updated", now)
            .commit()
    }

    /**
     * Persist selected language code to both fast SharedPreferences (synchronously) and Jetpack DataStore Preferences.
     */
    fun saveLanguage(context: Context, languageCode: String) {
        val code = AppLanguage.fromCode(languageCode).code
        val now = System.currentTimeMillis()
        persistFastPrefSync(context, code, now)

        scope.launch {
            context.languageDataStore.edit { preferences ->
                preferences[LANGUAGE_KEY] = code
                preferences[LAST_UPDATED_KEY] = now
            }
        }
    }

    /**
     * Suspend function to save language directly into DataStore preferences.
     */
    suspend fun saveLanguageDataStore(context: Context, languageCode: String) {
        val code = AppLanguage.fromCode(languageCode).code
        val now = System.currentTimeMillis()
        persistFastPrefSync(context, code, now)

        context.languageDataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = code
            preferences[LAST_UPDATED_KEY] = now
        }
    }

    /**
     * Save language to BOTH device-wide DataStore key and the per-user DataStore key (if userId is present).
     */
    suspend fun saveDeviceAndUserLanguage(
        context: Context,
        userId: String?,
        languageCode: String
    ) {
        val code = AppLanguage.fromCode(languageCode).code
        val now = System.currentTimeMillis()
        persistFastPrefSync(context, code, now)
        saveDeviceAndUserLanguage(context.languageDataStore, userId, code, now)
    }

    internal suspend fun saveDeviceAndUserLanguage(
        dataStore: DataStore<Preferences>,
        userId: String?,
        languageCode: String,
        timestampMillis: Long = System.currentTimeMillis()
    ) {
        val code = AppLanguage.fromCode(languageCode).code
        val cleanUserId = userId?.trim()?.ifBlank { null }
        dataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = code
            preferences[LAST_UPDATED_KEY] = timestampMillis
            if (cleanUserId != null) {
                preferences[userLanguageKey(cleanUserId)] = code
            }
        }
    }

    /**
     * Save per-user preferred language in DataStore only (never Room DB, never backup).
     */
    suspend fun saveUserLanguage(context: Context, userId: String, languageCode: String) {
        saveUserLanguage(context.languageDataStore, userId, languageCode)
    }

    internal suspend fun saveUserLanguage(
        dataStore: DataStore<Preferences>,
        userId: String,
        languageCode: String
    ) {
        val cleanUserId = userId.trim()
        if (cleanUserId.isEmpty()) return
        val code = AppLanguage.fromCode(languageCode).code
        dataStore.edit { preferences ->
            preferences[userLanguageKey(cleanUserId)] = code
        }
    }

    suspend fun getUserLanguage(context: Context, userId: String): String? {
        return getUserLanguage(context.languageDataStore, userId)
    }

    internal suspend fun getUserLanguage(
        dataStore: DataStore<Preferences>,
        userId: String
    ): String? {
        val cleanUserId = userId.trim()
        if (cleanUserId.isEmpty()) return null
        val raw = dataStore.data.map { prefs -> prefs[userLanguageKey(cleanUserId)] }.first()
        return raw?.takeIf { it.isNotBlank() }?.let { AppLanguage.fromCode(it).code }
    }

    fun getUserLanguageFlow(context: Context, userId: String): Flow<String?> {
        return getUserLanguageFlow(context.languageDataStore, userId)
    }

    internal fun getUserLanguageFlow(
        dataStore: DataStore<Preferences>,
        userId: String
    ): Flow<String?> {
        val cleanUserId = userId.trim()
        if (cleanUserId.isEmpty()) return flowOf(null)
        return dataStore.data.map { preferences ->
            val raw = preferences[userLanguageKey(cleanUserId)]
            raw?.takeIf { it.isNotBlank() }?.let { AppLanguage.fromCode(it).code }
        }
    }

    /**
     * Apply user's saved language upon login only if one is saved and differs from current language.
     */
    suspend fun applyUserLanguageOnLogin(
        context: Context,
        userId: String?,
        currentCode: String? = null
    ): String? {
        val cleanUserId = userId?.trim()?.ifBlank { null } ?: return null
        val userLang = getUserLanguage(context, cleanUserId) ?: return null
        val activeCode = currentCode?.takeIf { it.isNotBlank() } ?: getLanguage(context)
        if (!userLang.equals(activeCode, ignoreCase = true)) {
            saveLanguageDataStore(context, userLang)
            AppLanguageManager.applyLocaleIfNeeded(userLang)
        }
        return userLang
    }

    /**
     * Save the secondary toggle language to DataStore.
     */
    suspend fun saveSecondaryLanguageDataStore(context: Context, secondaryCode: String) {
        context.languageDataStore.edit { preferences ->
            preferences[SECONDARY_LANGUAGE_KEY] = secondaryCode
        }
    }

    /**
     * Toggle the application's display language between primary (DEFAULT_PRIMARY_LANGUAGE_CODE)
     * and secondary toggle target using DataStore preferences.
     * Returns the newly activated AppLanguage.
     */
    suspend fun toggleLanguage(context: Context): AppLanguage {
        val prefs = context.languageDataStore.data.first()
        val current = prefs[LANGUAGE_KEY] ?: getLanguage(context)
        val secondary = prefs[SECONDARY_LANGUAGE_KEY] ?: DEFAULT_SECONDARY_LANGUAGE_CODE

        // If currently on secondary, toggle back to primary; otherwise toggle to secondary
        val targetCode = if (current.equals(secondary, ignoreCase = true)) {
            DEFAULT_PRIMARY_LANGUAGE_CODE
        } else {
            secondary
        }

        saveLanguageDataStore(context, targetCode)
        return AppLanguage.fromCode(targetCode)
    }

    /**
     * Returns stored device language code if one was ever persisted, or null on genuine first launch.
     */
    suspend fun getStoredLanguageOrNull(context: Context): String? {
        val dsCode = runCatching {
            context.languageDataStore.data.map { prefs -> prefs[LANGUAGE_KEY] }.first()
        }.getOrNull()
        val raw = dsCode?.takeIf { it.isNotBlank() }
            ?: getPrefs(context).getString(KEY_LANGUAGE, null)?.takeIf { it.isNotBlank() }
            ?: return null
        return AppLanguage.fromCode(raw).code
    }

    fun getStoredLanguageSyncOrNull(context: Context): String? {
        val fastPref = getPrefs(context).getString(KEY_LANGUAGE, null)?.takeIf { it.isNotBlank() }
        if (fastPref != null) {
            return AppLanguage.fromCode(fastPref).code
        }
        val dsCode = try {
            runBlocking {
                context.languageDataStore.data.map { prefs -> prefs[LANGUAGE_KEY] }.first()
            }
        } catch (_: Exception) {
            null
        }
        return dsCode?.takeIf { it.isNotBlank() }?.let { AppLanguage.fromCode(it).code }
    }

    /**
     * Synchronous retrieval with DataStore first-check fallback.
     * Safely sanitizes the language to only supported codes, falling back to DEFAULT_PRIMARY_LANGUAGE_CODE.
     */
    fun getLanguage(context: Context): String {
        val rawCode = try {
            runBlocking {
                context.languageDataStore.data.map { prefs -> prefs[LANGUAGE_KEY] }.first()
            } ?: getPrefs(context).getString(KEY_LANGUAGE, DEFAULT_PRIMARY_LANGUAGE_CODE) ?: DEFAULT_PRIMARY_LANGUAGE_CODE
        } catch (e: Exception) {
            getPrefs(context).getString(KEY_LANGUAGE, DEFAULT_PRIMARY_LANGUAGE_CODE) ?: DEFAULT_PRIMARY_LANGUAGE_CODE
        }
        return AppLanguage.fromCode(rawCode).code
    }

    /**
     * Reactive Flow observing the active language tag from DataStore Preferences.
     */
    fun getLanguageFlow(context: Context): Flow<String> {
        return context.languageDataStore.data.map { preferences ->
            val raw = preferences[LANGUAGE_KEY] ?: getPrefs(context).getString(KEY_LANGUAGE, DEFAULT_PRIMARY_LANGUAGE_CODE) ?: DEFAULT_PRIMARY_LANGUAGE_CODE
            AppLanguage.fromCode(raw).code
        }
    }

    /**
     * Full reactive flow for LanguagePreferenceData from DataStore.
     */
    fun getLanguagePreferenceDataFlow(context: Context): Flow<LanguagePreferenceData> {
        return context.languageDataStore.data.map { preferences ->
            val rawCode = preferences[LANGUAGE_KEY] ?: getPrefs(context).getString(KEY_LANGUAGE, DEFAULT_PRIMARY_LANGUAGE_CODE) ?: DEFAULT_PRIMARY_LANGUAGE_CODE
            val rawSecondary = preferences[SECONDARY_LANGUAGE_KEY] ?: DEFAULT_SECONDARY_LANGUAGE_CODE
            val validCode = AppLanguage.fromCode(rawCode).code
            val validSecondary = AppLanguage.fromCode(rawSecondary).code
            val updated = preferences[LAST_UPDATED_KEY] ?: System.currentTimeMillis()
            LanguagePreferenceData(
                currentCode = validCode,
                secondaryCode = validSecondary,
                lastUpdatedMillis = updated
            )
        }
    }
}
