package com.lojia.shiftreport.util



import androidx.appcompat.app.AppCompatDelegate


import androidx.core.os.LocaleListCompat

object AppLanguageManager {
    fun changeLanguage(context: android.content.Context, languageCode: String) {
        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(appLocale)
        LanguagePreferences.saveLanguage(context, languageCode)
    }
    
    fun getCurrentLanguageCode(): String {
        return AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore('-').ifEmpty { "en" }
    }
}
