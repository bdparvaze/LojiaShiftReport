package com.lojia.shiftreport.util



import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.lojia.shiftreport.data.AppLanguage

object AppLanguageManager {
    fun changeLanguage(context: Context, language: AppLanguage) {
        changeLanguage(context, language.code)
    }

    fun changeLanguage(context: Context, languageCode: String) {
        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(appLocale)
        LanguagePreferences.saveLanguage(context, languageCode)
    }
    
    fun getCurrentLanguageCode(): String {
        return AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore('-').ifEmpty { AppConstants.LOCALE_EN }
    }

    fun getCurrentLanguage(): AppLanguage {
        return AppLanguage.fromCode(getCurrentLanguageCode())
    }
}
