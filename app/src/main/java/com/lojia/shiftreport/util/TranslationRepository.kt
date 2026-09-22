package com.lojia.shiftreport.util

import android.content.Context
import androidx.annotation.StringRes

/**
 * Universal Dynamic Translation Repository.
 * 100% Dynamic runtime translation backed by TranslationEngine (Google Translate / ML Kit + Room Cache).
 * Eliminates the need for values-[code]/strings.xml for new languages.
 */
object TranslationRepository {

    /**
     * Resolves a UiText structure into a dynamically translated string for any target language code.
     */
    fun resolve(context: Context, uiText: UiText, languageCode: String): String {
        return when (uiText) {
            is UiText.DynamicString -> {
                if (languageCode.equals("en", ignoreCase = true) || uiText.value.isBlank()) {
                    uiText.value
                } else {
                    TranslationEngine.translate(uiText.value, languageCode)
                }
            }
            is UiText.StringResource -> {
                val rawString = uiText.asString(context)
                if (languageCode.equals("en", ignoreCase = true) || rawString.isBlank()) {
                    rawString
                } else {
                    TranslationEngine.translate(rawString, languageCode)
                }
            }
        }
    }

    /**
     * Translates any plain text to target language code dynamically.
     */
    fun translate(text: String, languageCode: String): String {
        return TranslationEngine.translate(text, languageCode)
    }
}
