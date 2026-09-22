package com.lojia.shiftreport.util



import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLayoutDirection


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

/**
 * Universal Localization and Real-Time Translation Provider for Jetpack Compose.
 * Automatically wraps hierarchy, updates LayoutDirection and re-triggers on translation events.
 */
@Composable
fun UniversalLocalizationProvider(
    currentLanguage: AppLanguage,
    content: @Composable () -> Unit
) {
    val layoutDirection = LocaleManager.getLayoutDirection(currentLanguage)
    // Subscribe to translation cache updates to ensure all dynamic components re-render when translations land
    val updateTrigger by TranslationEngine.translationUpdates.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current
    val currentConfig = androidx.compose.ui.platform.LocalConfiguration.current

    val locale = androidx.compose.runtime.remember(currentLanguage.code) {
        try {
            java.util.Locale.forLanguageTag(currentLanguage.code)
        } catch (_: Exception) {
            java.util.Locale(currentLanguage.code)
        }
    }

    val localizedConfig = androidx.compose.runtime.remember(currentLanguage, locale, currentConfig) {
        android.content.res.Configuration(currentConfig).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
    }

    val localizedContext = androidx.compose.runtime.remember(currentLanguage, locale, localizedConfig, context) {
        val conf = android.content.res.Configuration(context.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        context.createConfigurationContext(conf)
    }

    val currentRegistryOwner = androidx.activity.compose.LocalActivityResultRegistryOwner.current
        ?: run {
            var ctx: android.content.Context? = context
            while (ctx is android.content.ContextWrapper) {
                if (ctx is androidx.activity.result.ActivityResultRegistryOwner) {
                    return@run ctx
                }
                ctx = ctx.baseContext
            }
            null
        }

    val currentOnBackPressedDispatcherOwner = androidx.activity.compose.LocalOnBackPressedDispatcherOwner.current
        ?: run {
            var ctx: android.content.Context? = context
            while (ctx is android.content.ContextWrapper) {
                if (ctx is androidx.activity.OnBackPressedDispatcherOwner) {
                    return@run ctx
                }
                ctx = ctx.baseContext
            }
            null
        }

    val providedLocals = remember(
        currentLanguage,
        layoutDirection,
        localizedConfig,
        localizedContext,
        currentRegistryOwner,
        currentOnBackPressedDispatcherOwner
    ) {
        val list = mutableListOf<androidx.compose.runtime.ProvidedValue<*>>(
            LocalAppLanguage provides currentLanguage,
            LocalLayoutDirection provides layoutDirection,
            androidx.compose.ui.platform.LocalConfiguration provides localizedConfig,
            androidx.compose.ui.platform.LocalContext provides localizedContext
        )
        if (currentRegistryOwner != null) {
            list.add(androidx.activity.compose.LocalActivityResultRegistryOwner provides currentRegistryOwner)
        }
        if (currentOnBackPressedDispatcherOwner != null) {
            list.add(androidx.activity.compose.LocalOnBackPressedDispatcherOwner provides currentOnBackPressedDispatcherOwner)
        }
        list
    }

    CompositionLocalProvider(
        values = providedLocals.toTypedArray()
    ) {
        content()
    }
}
