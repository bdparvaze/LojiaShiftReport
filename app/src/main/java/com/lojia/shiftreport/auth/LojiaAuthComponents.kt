package com.lojia.shiftreport.auth
import androidx.compose.ui.text.input.KeyboardType

import com.lojia.shiftreport.R
import androidx.compose.ui.res.stringResource
import com.lojia.shiftreport.data.*
import com.lojia.shiftreport.util.*
import com.lojia.shiftreport.ui.common.*
import com.lojia.shiftreport.ui.theme.*
import com.lojia.shiftreport.auth.*
import com.lojia.shiftreport.report.*
import com.lojia.shiftreport.settings.*

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import java.util.Locale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun LojiaHeader(
    isBn: Boolean = false,
    modifier: Modifier = Modifier,
    headerHeight: Dp? = null,
    onLanguageClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (headerHeight != null) Modifier.height(headerHeight) else Modifier.wrapContentHeight())
            .testTag("lojiaHeader"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.depthLogoTile(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Assessment,
                contentDescription = stringResource(R.string.cd_lojia_logo),
                tint = PrimaryBlueLight,
                modifier = Modifier.size(Dimens.LogoTileIconSize)
            )
        }

        Spacer(modifier = Modifier.height(Dimens.SpacingSm))

        Text(
            text = stringResource(R.string.app_name),
            fontFamily = PoppinsFontFamily,
            fontSize = DepthTypeTokens.LogoTitleSize,
            fontWeight = FontWeight.ExtraBold,
            color = PrimaryBlueLight,
            letterSpacing = DepthTypeTokens.LogoTitleLetterSpacing
        )

        Spacer(modifier = Modifier.height(Dimens.SpacingXxs))

        Text(
            text = stringResource(R.string.auth_tagline),
            fontFamily = PoppinsFontFamily,
            fontSize = DepthTypeTokens.LogoSubtitleSize,
            fontWeight = FontWeight.SemiBold,
            color = OnSurfaceVariantLight,
            letterSpacing = DepthTypeTokens.LogoSubtitleLetterSpacing
        )
    }
}

@Composable
fun LojiaCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardShape = RoundedCornerShape(Dimens.CardCornerRadius)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .depthCard(shape = cardShape, cornerRadius = Dimens.CardCornerRadius),
        shape = cardShape,
        color = Color.Transparent,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(Dimens.CardPaddingAuth),
            content = content
        )
    }
}

@Composable
fun LojiaSectionHeader(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 15.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 13.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(LojiaColors.P100),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = LojiaColors.P500,
                    modifier = Modifier.size(15.dp)
                )
            }
            Spacer(modifier = Modifier.width(9.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontFamily = com.lojia.shiftreport.ui.theme.PoppinsFontFamily, fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = LojiaColors.N900,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    fontFamily = com.lojia.shiftreport.ui.theme.PoppinsFontFamily, fontSize = 10.8.sp,
                    color = LojiaColors.N500,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 1.dp)
                )
            }
        }
        // Border bottom 1px solid #F3F4F6
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(LojiaColors.N100)
        )
    }
}

@Composable
fun LojiaInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    leadingIcon: ImageVector? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    isRequired: Boolean = false,
    isValid: Boolean = false,
    validationState: FieldValidationState = FieldValidationState.DEFAULT,
    errorMessage: String? = null,
    successMessage: String? = null,
    hintMessage: String? = null,
    infoTooltip: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    enabled: Boolean = true,
    testTag: String = ""
) {
    var isFocused by remember { mutableStateOf(false) }
    var showTooltip by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        // Label with Required Asterisk & Tooltip
        if (label.isNotBlank()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 5.dp, start = 2.dp)
            ) {
                Text(
                    text = label,
                    fontFamily = PoppinsFontFamily,
                    fontSize = DepthTypeTokens.RegisterLabelSize,
                    fontWeight = FontWeight.SemiBold,
                    color = OnSurfaceVariantLight
                )
                if (isRequired) {
                    Text(
                        text = " *",
                        fontFamily = PoppinsFontFamily,
                        fontSize = DepthTypeTokens.RegisterLabelSize,
                        fontWeight = FontWeight.Bold,
                        color = if (isValid) SuccessGreen else RequiredStarRed
                    )
                }
                if (!infoTooltip.isNullOrBlank()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .size(13.dp)
                            .clip(CircleShape)
                            .background(LojiaColors.N200)
                            .clickable { showTooltip = !showTooltip },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "?",
                            fontFamily = PoppinsFontFamily,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnSurfaceVariantLight
                        )
                    }
                }
            }
        }

        // Info Tooltip Box
        if (showTooltip && !infoTooltip.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(5.dp),
                color = LojiaColors.N900,
                modifier = Modifier
                    .padding(bottom = 5.dp)
                    .clickable { showTooltip = false }
            ) {
                Text(
                    text = infoTooltip,
                    fontFamily = PoppinsFontFamily,
                    fontSize = 10.sp,
                    color = PureWhite,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // Depth inset container with focus, error, disabled, and valid semantic feedback
        val fieldShape = RoundedCornerShape(Dimens.RadiusInputRegister)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = Dimens.TextFieldHeightCompact)
                .depthInset(
                    enabled = enabled,
                    isError = validationState == FieldValidationState.ERROR,
                    isFocused = isFocused,
                    shape = fieldShape,
                    cornerRadius = Dimens.RadiusInputRegister,
                    borderWidth = Dimens.InsetRegisterBorderWidth,
                    focusedBorderWidth = Dimens.InsetFocusedBorderWidth
                )
                .then(
                    if (validationState == FieldValidationState.SUCCESS && enabled) {
                        Modifier.border(
                            width = Dimens.InsetRegisterBorderWidth,
                            color = SuccessGreen,
                            shape = fieldShape
                        )
                    } else {
                        Modifier
                    }
                )
                .padding(horizontal = Dimens.RegisterFieldPaddingHorizontal, vertical = Dimens.SpacingXs),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = when {
                            validationState == FieldValidationState.ERROR -> ErrorRedLight
                            validationState == FieldValidationState.SUCCESS -> SuccessGreen
                            isFocused -> PrimaryBlueLight
                            else -> DepthTokens.RegisterIconColor
                        },
                        modifier = Modifier.size(Dimens.IconField)
                    )
                    Spacer(modifier = Modifier.width(Dimens.InsetIconTextGap))
                }

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = DepthTokens.RegisterPlaceholderColor,
                            fontFamily = PoppinsFontFamily,
                            fontSize = DepthTypeTokens.RegisterPlaceholderSize,
                            lineHeight = 18.sp
                        )
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        singleLine = true,
                        enabled = enabled,
                        visualTransformation = visualTransformation,
                        keyboardOptions = keyboardOptions,
                        keyboardActions = keyboardActions,
                        textStyle = TextStyle(
                            color = OnSurfaceLight,
                            fontFamily = PoppinsFontFamily,
                            fontSize = DepthTypeTokens.RegisterPlaceholderSize,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        cursorBrush = SolidColor(PrimaryBlueLight),
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                            .onFocusChanged { isFocused = it.isFocused }
                            .testTag(testTag)
                    )
                }

                if (trailingIcon != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    trailingIcon()
                }
            }
        }

        // Messages below input
        if (validationState == FieldValidationState.ERROR && !errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage,
                color = LojiaColors.R500,
                fontFamily = com.lojia.shiftreport.ui.theme.PoppinsFontFamily, fontSize = 11.sp,
                modifier = Modifier.padding(top = 3.dp, start = 2.dp)
            )
        } else if (validationState == FieldValidationState.SUCCESS && !successMessage.isNullOrBlank()) {
            Text(
                text = successMessage,
                color = LojiaColors.G500,
                fontFamily = com.lojia.shiftreport.ui.theme.PoppinsFontFamily, fontSize = 11.sp,
                modifier = Modifier.padding(top = 3.dp, start = 2.dp)
            )
        } else if (!hintMessage.isNullOrBlank()) {
            Text(
                text = hintMessage,
                color = LojiaColors.N600,
                fontFamily = com.lojia.shiftreport.ui.theme.PoppinsFontFamily, fontSize = 11.sp,
                modifier = Modifier.padding(top = 3.dp, start = 2.dp)
            )
        }
    }
}

@Composable
fun LojiaGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    testTag: String = ""
) {
    val isInteractive = enabled && !isLoading
    val buttonShape = RoundedCornerShape(Dimens.RadiusMd)
    val contentTint = if (isInteractive || isLoading) PureWhite else OnSurfaceVariantLight

    Surface(
        onClick = onClick,
        enabled = isInteractive,
        shape = buttonShape,
        color = Color.Transparent,
        shadowElevation = 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.ButtonHeightPrimary)
            .depthRaisedButton(
                enabled = enabled || isLoading,
                cornerRadius = Dimens.RadiusMd,
                minHeight = Dimens.ButtonHeightPrimary
            )
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.SpacingLg, vertical = Dimens.SpacingSm),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        color = PureWhite,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = text,
                        color = PureWhite,
                        fontFamily = PoppinsFontFamily,
                        fontSize = DepthTypeTokens.PrimaryButtonTextSize,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                Text(
                    text = text,
                    color = contentTint,
                    fontFamily = PoppinsFontFamily,
                    fontSize = DepthTypeTokens.PrimaryButtonTextSize,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun LojiaPasswordStrengthMeter(
    password: String,
    isBn: Boolean
) {
    if (password.isEmpty()) return

    val length = password.length
    val hasDigits = password.any { it.isDigit() }
    val hasMixedCase = password.any { it.isUpperCase() } && password.any { it.isLowerCase() }
    val hasSymbols = password.any { !it.isLetterOrDigit() }

    val strengthLevel = when {
        length >= 8 && ((hasDigits && hasMixedCase) || (hasDigits && hasSymbols) || (hasMixedCase && hasSymbols) || length >= 12) -> 3
        length >= 6 && (hasDigits || hasMixedCase || hasSymbols) -> 2
        else -> 1
    }

    val activeColor = when (strengthLevel) {
        1 -> ErrorRedLight
        2 -> WarningOrange
        else -> SuccessGreen
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 5.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 1..3) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(3.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(if (i <= strengthLevel) activeColor else LojiaColors.N200)
            )
        }
    }
}

/**
 * Reusable LojiaTextField adhering to standard M3 design and auth input patterns.
 */
@Composable
fun LojiaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "",
    placeholder: String = "",
    errorMessage: String? = null,
    validationState: FieldValidationState = FieldValidationState.DEFAULT,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    testTag: String = ""
) {
    LojiaInputField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        placeholder = placeholder,
        errorMessage = errorMessage,
        validationState = validationState,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        testTag = testTag,
        modifier = modifier
    )
}

/**
 * Reusable LojiaPasswordField with password masking.
 */
@Composable
fun LojiaPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "",
    placeholder: String = "",
    errorMessage: String? = null,
    validationState: FieldValidationState = FieldValidationState.DEFAULT,
    keyboardOptions: KeyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    testTag: String = "",
    showEyeToggle: Boolean = true
) {
    var isPasswordVisible by remember { mutableStateOf(false) }

    LojiaInputField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        placeholder = placeholder,
        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = if (showEyeToggle) {
            {
                IconButton(
                    onClick = { isPasswordVisible = !isPasswordVisible },
                    modifier = Modifier.size(Dimens.MinTouchTarget).testTag(if (testTag.isNotBlank()) "${testTag}_toggle" else "password_eye_toggle")
                ) {
                    Icon(
                        imageVector = if (isPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = stringResource(if (isPasswordVisible) R.string.cd_hide_password else R.string.cd_show_password),
                        tint = OnSurfaceVariantLight,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        } else null,
        errorMessage = errorMessage,
        validationState = validationState,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        testTag = testTag,
        modifier = modifier
    )
}

/**
 * Primary reusable action button for authentication and dialog flows.
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    testTag: String = ""
) {
    LojiaGradientButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        isLoading = isLoading,
        testTag = testTag
    )
}

@Composable
fun LojiaCountryPickerDialog(
    countries: List<LojiaCountry>,
    selectedDial: String,
    onSelectCountry: (LojiaCountry) -> Unit,
    onDismiss: () -> Unit,
    isBn: Boolean
) {
    val appLocale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val localizedCountries = remember(countries, appLocale) {
        countries.map { country ->
            val displayName = Locale("", country.code).getDisplayCountry(appLocale).ifBlank { country.name }
            country to displayName
        }
    }
    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(searchQuery, localizedCountries) {
        if (searchQuery.isBlank()) localizedCountries
        else {
            val q = searchQuery.lowercase().trim()
            localizedCountries.filter { (country, displayName) ->
                displayName.lowercase().contains(q) ||
                        country.name.lowercase().contains(q) ||
                        country.dial.contains(q) ||
                        country.code.lowercase().contains(q)
            }
        }
    }

    LojiaDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            // Header & Search
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.auth_select_country_code),
                    fontFamily = PoppinsFontFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceLight
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(Dimens.MinTouchTarget)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.close),
                        tint = OnSurfaceVariantLight
                    )
                }
            }

            // Inset Search Input
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .defaultMinSize(minHeight = Dimens.TextFieldHeightCompact)
                    .depthInset(shape = RoundedCornerShape(Dimens.RadiusMd))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = OnSurfaceVariantLight,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = TextStyle(color = OnSurfaceLight, fontFamily = PoppinsFontFamily, fontSize = 13.sp),
                        cursorBrush = SolidColor(PrimaryIndigoLight),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            HorizontalDivider(
                color = OutlineVariantLight,
                modifier = Modifier.padding(top = 10.dp)
            )

            // List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
            ) {
                items(filtered, key = { (c, _) -> c.code + c.dial }) { (country, displayName) ->
                    val isSelected = country.dial == selectedDial
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = Dimens.MinTouchTarget)
                            .clickable {
                                onSelectCountry(country)
                            }
                            .background(if (isSelected) PrimaryContainerLight else Color.Transparent)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = country.dial,
                            fontFamily = PoppinsFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnSurfaceLight,
                            modifier = Modifier.width(52.dp)
                        )
                        Text(
                            text = displayName,
                            fontFamily = PoppinsFontFamily,
                            fontSize = 13.sp,
                            color = OnSurfaceLight,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = PrimaryIndigoLight,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    HorizontalDivider(color = OutlineVariantLight, thickness = 0.5.dp)
                }
            }
        }
    }
}
