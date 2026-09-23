package com.lojia.shiftreport.util

import android.content.Context
import com.lojia.shiftreport.data.AppDatabase
import com.lojia.shiftreport.data.TranslationCacheEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

/**
 * High-Performance Dynamic Translation Helper with Offline Caching.
 *
 * Multi-Tier Architecture:
 * 1. In-Memory Concurrent Cache (0ms instant lookup)
 * 2. Room Database Persistent Cache (persists dynamically cached translations across app restarts)
 * 3. Configurable translation endpoints for expanded localization
 */
object TranslationEngine {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val memoryCache = ConcurrentHashMap<String, String>()
    private val pendingTranslations = ConcurrentHashMap.newKeySet<String>()

    @Volatile
    private var database: AppDatabase? = null

    private val _translationUpdates = MutableStateFlow(0L)
    val translationUpdates: StateFlow<Long> = _translationUpdates.asStateFlow()

    fun init(context: Context) {
        if (database == null) {
            database = AppDatabase.getInstance(context)
            scope.launch {
                try {
                    val allCached = database?.translationDao()?.getAllTranslationsForLanguage("") ?: emptyList()
                    for (item in allCached) {
                        memoryCache[item.cacheKey] = item.translatedText
                    }
                } catch (_: Exception) {}
            }
        }
    }

    /**
     * Translates any plain text string into the requested target language code.
     * 1. If English or empty, returns instantly.
     * 2. Checks In-Memory Cache.
     * 3. Queries Room Database and Translation API asynchronously.
     */
    fun translate(sourceText: String, targetLanguageCode: String): String {
        val cleanText = sourceText.trim()
        if (cleanText.isBlank() || targetLanguageCode.equals("en", ignoreCase = true)) {
            return sourceText
        }
        val cacheKey = "${cleanText}_$targetLanguageCode"

        // 1. In-Memory Cache hit
        val cached = memoryCache[cacheKey]
        if (cached != null) {
            return cached
        }

        // 2. Queue asynchronous Room lookup and Remote Fetch
        if (pendingTranslations.add(cacheKey)) {
            scope.launch {
                fetchAndCache(cleanText, targetLanguageCode, cacheKey)
            }
        }

        return sourceText
    }

    /**
     * Synchronous blocking translate helper for non-composable environments.
     */
    suspend fun translateSync(sourceText: String, targetLanguageCode: String): String {
        val cleanText = sourceText.trim()
        if (cleanText.isBlank() || targetLanguageCode.equals("en", ignoreCase = true)) {
            return sourceText
        }
        val cacheKey = "${cleanText}_$targetLanguageCode"

        val cached = memoryCache[cacheKey]
        if (cached != null) return cached

        val dbHit = withContext(Dispatchers.IO) {
            try {
                database?.translationDao()?.getTranslation(cacheKey)
            } catch (e: Exception) {
                null
            }
        }
        if (!dbHit.isNullOrBlank()) {
            memoryCache[cacheKey] = dbHit
            return dbHit
        }

        val remoteHit = withContext(Dispatchers.IO) {
            fetchFromGoogleTranslate(cleanText, targetLanguageCode)
        }
        if (!remoteHit.isNullOrBlank()) {
            memoryCache[cacheKey] = remoteHit
            withContext(Dispatchers.IO) {
                try {
                    database?.translationDao()?.saveTranslation(
                        TranslationCacheEntity(
                            cacheKey = cacheKey,
                            sourceText = cleanText,
                            targetLanguage = targetLanguageCode,
                            translatedText = remoteHit
                        )
                    )
                } catch (_: Exception) {}
            }
            _translationUpdates.value = System.currentTimeMillis()
            return remoteHit
        }

        return sourceText
    }

    private suspend fun fetchAndCache(sourceText: String, targetLanguageCode: String, cacheKey: String) {
        try {
            // Check Room Database
            val localDbTranslation = withContext(Dispatchers.IO) {
                try {
                    database?.translationDao()?.getTranslation(cacheKey)
                } catch (e: Exception) {
                    null
                }
            }

            if (!localDbTranslation.isNullOrBlank()) {
                memoryCache[cacheKey] = localDbTranslation
                pendingTranslations.remove(cacheKey)
                _translationUpdates.value = System.currentTimeMillis()
                return
            }

            // Remote translation request to Google Translate API
            val remoteTranslated = withContext(Dispatchers.IO) {
                fetchFromGoogleTranslate(sourceText, targetLanguageCode)
            }

            if (!remoteTranslated.isNullOrBlank()) {
                memoryCache[cacheKey] = remoteTranslated
                // Persist to Room Database for offline reuse
                withContext(Dispatchers.IO) {
                    try {
                        database?.translationDao()?.saveTranslation(
                            TranslationCacheEntity(
                                cacheKey = cacheKey,
                                sourceText = sourceText,
                                targetLanguage = targetLanguageCode,
                                translatedText = remoteTranslated
                            )
                        )
                    } catch (_: Exception) {}
                }
                _translationUpdates.value = System.currentTimeMillis()
            }
        } catch (_: Exception) {
        } finally {
            pendingTranslations.remove(cacheKey)
        }
    }

    private fun fetchFromGoogleTranslate(text: String, targetLang: String): String? {
        var connection: HttpURLConnection? = null
        return try {
            val encodedText = URLEncoder.encode(text, "UTF-8")
            val url = URL("https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=$targetLang&dt=t&q=$encodedText")
            connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 4000
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(response)
                val sentences = jsonArray.getJSONArray(0)
                val builder = StringBuilder()
                for (i in 0 until sentences.length()) {
                    builder.append(sentences.getJSONArray(i).getString(0))
                }
                builder.toString()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }
}
