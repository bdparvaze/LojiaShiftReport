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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

val Context.languageDataStore: DataStore<Preferences> by preferencesDataStore(name = "language_preferences")

data class LanguagePreferenceData(
    val currentCode: String = "en",
    val secondaryCode: String = "ar",
    val lastUpdatedMillis: Long = System.currentTimeMillis()
)

object LanguagePreferences {
    private const val PREFS_NAME = "language_prefs"
    private const val KEY_LANGUAGE = "selected_language"
    
    val LANGUAGE_KEY = stringPreferencesKey("selected_language_tag")
    val SECONDARY_LANGUAGE_KEY = stringPreferencesKey("secondary_language_tag")
    val LAST_UPDATED_KEY = longPreferencesKey("language_last_updated")
    
    private val scope = CoroutineScope(Dispatchers.IO)

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Persist selected language code to both Jetpack DataStore Preferences and fast SharedPreferences.
     */
    fun saveLanguage(context: Context, languageCode: String) {
        val now = System.currentTimeMillis()
        // 1. SharedPreferences (instant synchronous lookup for initial startup)
        getPrefs(context).edit()
            .putString(KEY_LANGUAGE, languageCode)
            .putLong("last_updated", now)
            .apply()

        // 2. Jetpack DataStore Preferences (reactive asynchronous persistence)
        scope.launch {
            context.languageDataStore.edit { preferences ->
                preferences[LANGUAGE_KEY] = languageCode
                preferences[LAST_UPDATED_KEY] = now
            }
        }
    }

    /**
     * Suspend function to save language directly into DataStore preferences.
     */
    suspend fun saveLanguageDataStore(context: Context, languageCode: String) {
        val now = System.currentTimeMillis()
        getPrefs(context).edit()
            .putString(KEY_LANGUAGE, languageCode)
            .putLong("last_updated", now)
            .apply()

        context.languageDataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = languageCode
            preferences[LAST_UPDATED_KEY] = now
        }
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
     * Toggle the application's display language between primary (English or custom)
     * and secondary toggle target (Arabic/Bengali/etc.) using DataStore preferences.
     * Returns the newly activated AppLanguage.
     */
    suspend fun toggleLanguage(context: Context): AppLanguage {
        val prefs = context.languageDataStore.data.first()
        val current = prefs[LANGUAGE_KEY] ?: getLanguage(context)
        val secondary = prefs[SECONDARY_LANGUAGE_KEY] ?: "ar"

        // If currently on secondary, toggle back to English ("en"); otherwise toggle to secondary
        val targetCode = if (current.equals(secondary, ignoreCase = true)) {
            "en"
        } else {
            secondary
        }

        saveLanguageDataStore(context, targetCode)
        return AppLanguage.fromCode(targetCode)
    }

    /**
     * Synchronous retrieval with DataStore first-check fallback.
     * Safely sanitizes the language to only supported codes (en, bn, ar), falling back to "en".
     */
    fun getLanguage(context: Context): String {
        val rawCode = try {
            runBlocking {
                context.languageDataStore.data.map { prefs -> prefs[LANGUAGE_KEY] }.first()
            } ?: getPrefs(context).getString(KEY_LANGUAGE, "en") ?: "en"
        } catch (e: Exception) {
            getPrefs(context).getString(KEY_LANGUAGE, "en") ?: "en"
        }
        return AppLanguage.fromCode(rawCode).code
    }

    /**
     * Reactive Flow observing the active language tag from DataStore Preferences.
     */
    fun getLanguageFlow(context: Context): Flow<String> {
        return context.languageDataStore.data.map { preferences ->
            val raw = preferences[LANGUAGE_KEY] ?: getPrefs(context).getString(KEY_LANGUAGE, "en") ?: "en"
            AppLanguage.fromCode(raw).code
        }
    }

    /**
     * Full reactive flow for LanguagePreferenceData from DataStore.
     */
    fun getLanguagePreferenceDataFlow(context: Context): Flow<LanguagePreferenceData> {
        return context.languageDataStore.data.map { preferences ->
            val rawCode = preferences[LANGUAGE_KEY] ?: getPrefs(context).getString(KEY_LANGUAGE, "en") ?: "en"
            val rawSecondary = preferences[SECONDARY_LANGUAGE_KEY] ?: "ar"
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
