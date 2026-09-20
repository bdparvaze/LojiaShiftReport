package com.lojia.pos.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "translation_cache")
data class TranslationCacheEntity(
    @PrimaryKey val cacheKey: String, // format: "${sourceText.trim()}_${targetLanguageCode}"
    val sourceText: String,
    val targetLanguage: String,
    val translatedText: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface TranslationDao {
    @Query("SELECT translatedText FROM translation_cache WHERE cacheKey = :key LIMIT 1")
    suspend fun getTranslation(key: String): String?

    @Query("SELECT * FROM translation_cache WHERE targetLanguage = :langCode")
    suspend fun getAllTranslationsForLanguage(langCode: String): List<TranslationCacheEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveTranslation(entity: TranslationCacheEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAllTranslations(entities: List<TranslationCacheEntity>)

    @Query("DELETE FROM translation_cache WHERE cacheKey LIKE '%backup%' OR cacheKey LIKE '%Backup%' OR cacheKey LIKE '%restore%' OR cacheKey LIKE '%Restore%' OR cacheKey LIKE '%পুনরুদ্ধার%' OR translatedText LIKE '%পুনরুদ্ধার%' OR translatedText LIKE '%Restore%' OR translatedText LIKE '%restore%'")
    suspend fun clearBackupTranslations()

    @Query("DELETE FROM translation_cache WHERE cacheKey LIKE '%cashier%' OR cacheKey LIKE '%ক্যাশিয়ার%' OR translatedText LIKE '%ক্যাশিয়ার ব্যবস্থাপনা%' OR translatedText LIKE '%ক্যাশিয়ার ম্যানেজমেন্ট%'")
    suspend fun clearCashierTranslations()

    @Query("DELETE FROM translation_cache WHERE cacheKey LIKE '%daily shift%' OR cacheKey LIKE '%দৈনিক শিফট%' OR translatedText LIKE '%দৈনিক শিফট%'")
    suspend fun clearDailyShiftTranslations()

    @Query("DELETE FROM translation_cache WHERE cacheKey LIKE '%performance analytics%' OR cacheKey LIKE '%পারফরম্যান্স অ্যানালিটিক্স%' OR translatedText LIKE '%পারফরম্যান্স অ্যানালিটিক্স%'")
    suspend fun clearPerformanceAnalyticsTranslations()
}
