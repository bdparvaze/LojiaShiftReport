package com.lojia.shiftreport.ui.common

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import com.lojia.shiftreport.ui.theme.*

@Composable
fun lojiaDefaultTextFieldColors(): TextFieldColors = TextFieldDefaults.colors(
    focusedTextColor = OnSurfaceLight,
    unfocusedTextColor = OnSurfaceLight,
    disabledTextColor = OnSurfaceVariantLight,
    errorTextColor = ErrorRedLight,
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    disabledContainerColor = Color.Transparent,
    errorContainerColor = Color.Transparent,
    focusedLabelColor = PrimaryIndigoLight,
    unfocusedLabelColor = DepthTokens.InsetIconColor,
    disabledLabelColor = TextHintColor,
    errorLabelColor = ErrorRedLight,
    focusedPlaceholderColor = DepthTokens.InsetPlaceholderColor,
    unfocusedPlaceholderColor = DepthTokens.InsetPlaceholderColor,
    disabledPlaceholderColor = TextDisabledColor,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
    errorIndicatorColor = Color.Transparent,
    focusedLeadingIconColor = PrimaryIndigoLight,
    unfocusedLeadingIconColor = DepthTokens.InsetIconColor,
    focusedTrailingIconColor = PrimaryIndigoLight,
    unfocusedTrailingIconColor = DepthTokens.InsetIconColor,
    cursorColor = PrimaryIndigoLight
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LojiaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current.copy(
        color = OnSurfaceLight,
        fontWeight = FontWeight.Medium
    ),
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = RoundedCornerShape(Dimens.RadiusMd),
    colors: TextFieldColors = lojiaDefaultTextFieldColors(),
    supportingText: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    minHeight: androidx.compose.ui.unit.Dp = Dimens.TextFieldHeightStandard,
    borderColorOverride: Color? = null,
    topShadowAlphaOverride: Float? = null,
    placeholderColor: Color = DepthTokens.InsetPlaceholderColor,
    placeholderFontSize: androidx.compose.ui.unit.TextUnit = DepthTypeTokens.InsetPlaceholderSize,
    disabledBackgroundOverride: Color? = null,
    disabledBorderColorOverride: Color? = null,
    insetShadowOffsetYOverride: androidx.compose.ui.unit.Dp? = null,
    insetShadowBlurOverride: androidx.compose.ui.unit.Dp? = null,
    disabledTextColor: Color = OnSurfaceVariantLight
) {
    val isFocused by interactionSource.collectIsFocusedAsState()
    val effectiveTextStyle = textStyle.copy(
        color = when {
            !enabled -> disabledTextColor
            isError -> ErrorRedLight
            else -> textStyle.color
        }
    )

    // Show exactly one static hint inside the field (placeholder wins if both are supplied).
    val effectiveHint: (@Composable () -> Unit)? = placeholder ?: label
    val styledPlaceholder: (@Composable () -> Unit)? = effectiveHint?.let { hint ->
        {
            ProvideTextStyle(
                value = LocalTextStyle.current.copy(
                    fontSize = placeholderFontSize,
                    color = placeholderColor
                )
            ) {
                hint()
            }
        }
    }

    val paddedLeadingIcon: (@Composable () -> Unit)? = leadingIcon?.let { icon ->
        {
            Box(modifier = Modifier.padding(start = Dimens.SpacingXxs, end = Dimens.SpacingXs)) {
                icon()
            }
        }
    }

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.defaultMinSize(minHeight = minHeight),
        enabled = enabled,
        readOnly = readOnly,
        textStyle = effectiveTextStyle,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        visualTransformation = visualTransformation,
        interactionSource = interactionSource,
        cursorBrush = SolidColor(if (isError) ErrorRedLight else PrimaryIndigoLight),
        decorationBox = { innerTextField ->
            Box {
                // Keep label composable purely for TalkBack semantics without visual rendering.
                if (label != null && (placeholder != null || value.isNotEmpty())) {
                    Box(
                        modifier = Modifier
                            .size(Dimens.SpacingNone)
                            .drawWithContent {}
                    ) {
                        label()
                    }
                }
                TextFieldDefaults.DecorationBox(
                    value = value,
                    innerTextField = innerTextField,
                    enabled = enabled,
                    singleLine = singleLine,
                    visualTransformation = visualTransformation,
                    interactionSource = interactionSource,
                    isError = isError,
                    label = null,
                    placeholder = styledPlaceholder,
                    leadingIcon = paddedLeadingIcon,
                    trailingIcon = trailingIcon,
                    suffix = suffix,
                    supportingText = supportingText,
                    shape = shape,
                    colors = colors,
                    contentPadding = PaddingValues(
                        horizontal = Dimens.InsetFieldPaddingHorizontal,
                        vertical = Dimens.SpacingMd
                    ),
                    container = {
                        val resolvedBorderOverride = when {
                            !enabled && !isError && disabledBorderColorOverride != null -> disabledBorderColorOverride
                            enabled && !isError && !isFocused -> borderColorOverride
                            else -> null
                        }
                        Box(
                            modifier = Modifier.depthInset(
                                enabled = enabled,
                                isError = isError,
                                isFocused = isFocused,
                                shape = shape,
                                borderColorOverride = resolvedBorderOverride,
                                topShadowAlphaOverride = topShadowAlphaOverride,
                                disabledBackgroundOverride = disabledBackgroundOverride,
                                insetShadowOffsetYOverride = insetShadowOffsetYOverride,
                                insetShadowBlurOverride = insetShadowBlurOverride
                            )
                        )
                    }
                )
            }
        }
    )
}

@Composable
fun LojiaMultilineTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current.copy(
        color = OnSurfaceLight,
        fontWeight = FontWeight.Medium
    ),
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = RoundedCornerShape(Dimens.RadiusMd),
    colors: TextFieldColors = lojiaDefaultTextFieldColors(),
    supportingText: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null
) {
    LojiaTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle,
        label = label,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        isError = isError,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = false,
        maxLines = 5,
        minLines = 3,
        interactionSource = interactionSource,
        shape = shape,
        colors = colors,
        supportingText = supportingText,
        suffix = suffix
    )
}
