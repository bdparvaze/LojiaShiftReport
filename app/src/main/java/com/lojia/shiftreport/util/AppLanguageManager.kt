package com.lojia.shiftreport.util



import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.lojia.shiftreport.data.AppLanguage

object AppLanguageManager {
    fun changeLanguage(context: Context, language: AppLanguage) {
        changeLanguage(context, language.code)
    }

    fun changeLanguage(context: Context, languageCode: String) {
        val normalizedCode = AppLanguage.fromCode(languageCode).code
        LanguagePreferences.saveLanguage(context, normalizedCode)
        applyLocaleIfNeeded(normalizedCode)
    }

    fun applyLocaleIfNeeded(languageCode: String) {
        val normalizedCode = AppLanguage.fromCode(languageCode).code
        val currentLocales = AppCompatDelegate.getApplicationLocales()
        val currentTag = if (currentLocales.isEmpty) {
            ""
        } else {
            currentLocales.toLanguageTags().substringBefore('-').substringBefore('_')
        }
        if (currentTag.equals(normalizedCode, ignoreCase = true)) {
            return
        }
        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(normalizedCode)
        if (Looper.myLooper() == Looper.getMainLooper()) {
            AppCompatDelegate.setApplicationLocales(appLocale)
        } else {
            Handler(Looper.getMainLooper()).post {
                AppCompatDelegate.setApplicationLocales(appLocale)
            }
        }
    }

    fun getCurrentLanguageCode(): String {
        return AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore('-').ifEmpty { AppConstants.LOCALE_EN }
    }

    fun getCurrentLanguage(): AppLanguage {
        return AppLanguage.fromCode(getCurrentLanguageCode())
    }
}
