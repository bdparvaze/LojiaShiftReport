package com.lojia.shiftreport.util

import androidx.compose.ui.unit.LayoutDirection
import com.lojia.shiftreport.data.AppLanguage

val LocalAppLanguage = androidx.compose.runtime.compositionLocalOf { AppLanguage.ENGLISH }

object LocaleManager {
    // Standard ISO RTL language codes: Arabic, Persian/Farsi, Urdu, Hebrew, Pashto, Yiddish, Sindhi, etc.
    private val rtlCodes = setOf("ar", "fa", "ur", "he", "ps", "yi", "sd", "ug", "ckb", "dv")

    fun isRtl(languageCode: String): Boolean {
        return rtlCodes.contains(languageCode.lowercase().trim())
    }

    fun isRtl(language: AppLanguage): Boolean {
        return language.isRtl || isRtl(language.code)
    }

    fun getLayoutDirection(language: AppLanguage): LayoutDirection {
        return if (isRtl(language)) LayoutDirection.Rtl else LayoutDirection.Ltr
    }
}
