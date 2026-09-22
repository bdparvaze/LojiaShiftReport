package com.lojia.pos.ui.common

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import com.lojia.pos.util.TranslationEngine
import com.lojia.pos.util.LocalAppLanguage
import com.lojia.pos.util.UiText
import com.lojia.pos.util.TranslationRepository
import androidx.compose.ui.platform.LocalContext

import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource

/**
 * Universal AutoText Composable:
 * Drop-in replacement for standard Jetpack Compose Text.
 * Accepts a plain string or string resource, checks the currently selected target language from LocalAppLanguage,
 * and dynamically translates it using Google Translate API / ML Kit + local Room Cache and reactive updates.
 */
@Composable
fun AutoText(
    @StringRes id: Int,
    vararg formatArgs: Any,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    style: TextStyle = LocalTextStyle.current
) {
    val rawText = if (formatArgs.isNotEmpty()) stringResource(id, *formatArgs) else stringResource(id)
    AutoText(
        text = rawText,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        onTextLayout = onTextLayout,
        style = style
    )
}

@Composable
fun AutoText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    style: TextStyle = LocalTextStyle.current
) {
    val currentLanguage = LocalAppLanguage.current
    // Observe translation engine updates so when remote translation lands, UI recomposes instantly
    val updateKey by TranslationEngine.translationUpdates.collectAsState()
    val translatedText = TranslationEngine.translate(text, currentLanguage.code)

    Text(
        text = translatedText,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        onTextLayout = onTextLayout,
        style = style
    )
}

/**
 * Backward-compatible DynamicText alias for AutoText.
 */
@Composable
fun DynamicText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    style: TextStyle = LocalTextStyle.current
) {
    AutoText(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        onTextLayout = onTextLayout,
        style = style
    )
}

/**
 * Hook to retrieve reactively resolved UiText in any Composable.
 */
@Composable
fun rememberUiText(uiText: UiText): String {
    val currentLanguage = LocalAppLanguage.current
    val context = LocalContext.current
    // Observe translation engine updates
    val updateKey by TranslationEngine.translationUpdates.collectAsState()
    return TranslationRepository.resolve(context, uiText, currentLanguage.code)
}

/**
 * Hook to retrieve the real-time dynamically translated string in any Composable.
 */
@Composable
fun rememberTranslatedString(text: String): String {
    val currentLanguage = LocalAppLanguage.current
    // Observe translation engine updates
    val updateKey by TranslationEngine.translationUpdates.collectAsState()
    return TranslationEngine.translate(text, currentLanguage.code)
}

/**
 * Hook to retrieve the real-time dynamically translated string resource in any Composable.
 */
@Composable
fun rememberTranslatedString(@StringRes id: Int, vararg formatArgs: Any): String {
    val rawText = if (formatArgs.isNotEmpty()) stringResource(id, *formatArgs) else stringResource(id)
    return rememberTranslatedString(rawText)
}

