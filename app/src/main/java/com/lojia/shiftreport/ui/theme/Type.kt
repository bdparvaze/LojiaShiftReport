package com.lojia.shiftreport.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Extra line-height multiplier applied to Bengali ("bn") and Arabic ("ar")
 * to prevent vertical diacritic / ascender clipping while staying compact.
 */
const val LineHeightFactorBnAr: Float = 1.15f

private fun resolvedLanguageCode(languageTag: String): String =
    languageTag.lowercase().substringBefore('-').ifEmpty { "en" }

private fun isExtendedLineHeightLanguage(languageTag: String): Boolean {
    val lang = resolvedLanguageCode(languageTag)
    return lang == "bn" || lang == "ar"
}

/**
 * Returns compact Material 3 Typography for the given [languageTag].
 * All languages (en, bn, ar) use the device system font ([FontFamily.Default]).
 */
fun typographyFor(languageTag: String): Typography {
    val factor = if (isExtendedLineHeightLanguage(languageTag)) LineHeightFactorBnAr else 1f
    fun lh(baseSp: Float) = (baseSp * factor).sp

    return Typography(
        displayLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            lineHeight = lh(26f),
            letterSpacing = (-0.2).sp
        ),
        displayMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            lineHeight = lh(24f),
            letterSpacing = (-0.1).sp
        ),
        displaySmall = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            lineHeight = lh(22f),
            letterSpacing = 0.sp
        ),
        headlineLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            lineHeight = lh(24f),
            letterSpacing = 0.sp
        ),
        headlineMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            lineHeight = lh(22f),
            letterSpacing = 0.sp
        ),
        headlineSmall = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            lineHeight = lh(22f),
            letterSpacing = 0.sp
        ),
        titleLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            lineHeight = lh(20f),
            letterSpacing = 0.sp
        ),
        titleMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            lineHeight = lh(18f),
            letterSpacing = 0.sp
        ),
        titleSmall = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            lineHeight = lh(16f),
            letterSpacing = 0.2.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = lh(20f),
            letterSpacing = 0.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = lh(17f),
            letterSpacing = 0.1.sp
        ),
        bodySmall = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 11.sp,
            lineHeight = lh(15f),
            letterSpacing = 0.2.sp
        ),
        labelLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            lineHeight = lh(18f),
            letterSpacing = 0.1.sp
        ),
        labelMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = lh(15f),
            letterSpacing = 0.2.sp
        ),
        labelSmall = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = lh(15f),
            letterSpacing = 0.2.sp
        )
    )
}

/**
 * Dedicated KPI figure text style (20/26sp Bold, tabular figures `tnum`).
 */
fun kpiValueTextStyle(languageTag: String = "en"): TextStyle {
    val factor = if (isExtendedLineHeightLanguage(languageTag)) LineHeightFactorBnAr else 1f
    return TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = (26f * factor).sp,
        fontFeatureSettings = "tnum"
    )
}

/**
 * Applies tabular numerals (`tnum`) to a text style used for money amounts and KPIs.
 */
fun TextStyle.asMoneyTabular(): TextStyle = copy(fontFeatureSettings = "tnum")

val Typography = typographyFor("en")
