package com.lojia.shiftreport.report

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojia.shiftreport.R
import com.lojia.shiftreport.ui.theme.*

/* ----------------------------------------------------------------------
 * SECTION HEADER (pill with tone color, optional "+ Add" button)
 * ---------------------------------------------------------------------- */
@Composable
fun SectionHeader(
    icon: String,
    title: String,
    bg: Color,
    borderColor: Color,
    accent: Color,
    onAdd: (() -> Unit)? = null
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val pillShape = RoundedCornerShape(Dimens.RadiusReportInput)
    val (bannerColors, bannerBorder, tileColors) = when (borderColor) {
        ShiftColors.Brass -> Triple(
            listOf(SectionAmberGradientStart, SectionAmberGradientEnd),
            SectionAmberBorder,
            listOf(PureWhite, BadgeAmberBg)
        )
        ShiftColors.Danger -> Triple(
            listOf(SectionRoseGradientStart, SectionRoseGradientEnd),
            SectionRoseBorder,
            listOf(PureWhite, ErrorContainerLight)
        )
        else -> Triple(
            listOf(SectionPurpleGradientStart, SectionPurpleGradientEnd),
            SectionPurpleBorder,
            listOf(PureWhite, TertiaryContainerLight)
        )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Dimens.SpacingMd, bottom = Dimens.SpacingXs)
            .defaultMinSize(minHeight = Dimens.SectionHeaderHeight)
            .depthSectionBanner(
                gradientColors = bannerColors,
                borderColor = bannerBorder,
                cornerRadius = Dimens.RadiusReportInput
            )
            .padding(horizontal = Dimens.SpacingMd, vertical = Dimens.SpacingSm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(Dimens.SectionIconTileSize)
                    .depthRaisedTile(
                        gradientColors = tileColors,
                        cornerRadius = Dimens.RadiusSectionIconTile
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = DepthTypeTokens.SectionTitleSize)
            }
            Spacer(Modifier.width(Dimens.ReportGridGap))
            Text(
                title,
                color = TextPrimaryReport,
                fontWeight = FontWeight.SemiBold,
                fontSize = DepthTypeTokens.SectionTitleSize,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (onAdd != null) {
            OutlinedButton(
                onClick = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    onAdd()
                },
                shape = RoundedCornerShape(Dimens.RadiusMd),
                border = null,
                elevation = null,
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = accent
                ),
                contentPadding = PaddingValues(horizontal = Dimens.ReportGridGap, vertical = Dimens.SpacingXs),
                modifier = Modifier
                    .heightIn(min = Dimens.MinTouchTarget)
                    .depthSecondaryButton(
                        borderColor = accent,
                        cornerRadius = Dimens.RadiusMd
                    )
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(Dimens.IconXs))
                Spacer(Modifier.width(Dimens.SpacingXs))
                Text(
                    stringResource(R.string.add),
                    fontSize = DepthTypeTokens.SecondaryButtonTextSize,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }
    }
}

/* ----------------------------------------------------------------------
 * DYNAMIC ENTRY ROW
 * ---------------------------------------------------------------------- */
@Composable
fun EntryRow(left: String, right: String, onRemove: () -> Unit) {
    val rowShape = RoundedCornerShape(Dimens.RadiusReportInput)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = Dimens.SpacingXs)
            .depthCard(shape = rowShape, cornerRadius = Dimens.RadiusReportInput)
            .padding(start = Dimens.SpacingMd, end = Dimens.SpacingXxs, top = Dimens.SpacingXxs, bottom = Dimens.SpacingXxs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(left, fontSize = DepthTypeTokens.ReportFieldLabelSize, color = TextPrimaryReport, modifier = Modifier.weight(1f))
        Text(right, fontSize = DepthTypeTokens.ReportFieldLabelSize, color = TextPrimaryReport, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(Dimens.SpacingXs))
        IconButton(
            onClick = onRemove,
            modifier = Modifier.minimumInteractiveComponentSize()
        ) {
            Box(
                modifier = Modifier
                    .size(Dimens.IconLg)
                    .clip(RoundedCornerShape(Dimens.RadiusSm))
                    .background(ShiftColors.DangerLight)
                    .border(Dimens.CardBorderWidth, ErrorContainerLight, RoundedCornerShape(Dimens.RadiusSm)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = stringResource(R.string.remove),
                    tint = ShiftColors.Danger,
                    modifier = Modifier.size(Dimens.IconXs)
                )
            }
        }
    }
}

/* ----------------------------------------------------------------------
 * FORM PRIMITIVES
 * ---------------------------------------------------------------------- */
@Composable
fun FieldLabel(text: String) {
    val style = LocalDepthStyle.current
    Text(
        text = text,
        fontSize = DepthTypeTokens.ReportFieldLabelSize,
        fontWeight = FontWeight.SemiBold,
        color = style.reportFieldLabelColor,
        modifier = Modifier.padding(bottom = Dimens.SpacingXs),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = OnBackgroundLight,
    unfocusedTextColor = OnBackgroundLight,
    disabledTextColor = OnSurfaceVariantLight,
    focusedContainerColor = DepthTokens.InsetFieldFocusedBackground,
    unfocusedContainerColor = DepthTokens.InsetFieldBackground,
    disabledContainerColor = DepthTokens.InsetFieldDisabledBackground,
    focusedBorderColor = DepthTokens.InsetFieldFocusedBorderColor,
    unfocusedBorderColor = DepthTokens.InsetFieldBorderColor,
    focusedLabelColor = ShiftColors.Primary,
    unfocusedLabelColor = OnSurfaceVariantLight,
    focusedPlaceholderColor = TextHintColor,
    unfocusedPlaceholderColor = TextHintColor,
    cursorColor = ShiftColors.Primary
)

private val COMMA_THREE_DIGITS_REGEX = Regex("[,\\u060C]\\d{3}(?!\\d)")
private val DOT_THREE_DIGITS_REGEX = Regex("\\.\\d{3}(?!\\d)")

internal fun shouldShowAmountCommaHint(
    value: String,
    isInteger: Boolean,
    errorText: String?,
    commaUsedAsSeparator: Boolean
): Boolean {
    if (isInteger) return false
    val normalizedValue = value.replace('\u060C', ',')
    return COMMA_THREE_DIGITS_REGEX.containsMatchIn(normalizedValue) ||
        (errorText != null && commaUsedAsSeparator && DOT_THREE_DIGITS_REGEX.containsMatchIn(normalizedValue))
}

@Composable
fun NumberField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isInteger: Boolean = false,
    errorText: String? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    var commaUsedAsSeparator by remember { mutableStateOf(false) }
    val showCommaHint = shouldShowAmountCommaHint(
        value = value,
        isInteger = isInteger,
        errorText = errorText,
        commaUsedAsSeparator = commaUsedAsSeparator
    )
    val displayMessage = if (showCommaHint) stringResource(R.string.amount_comma_hint) else errorText
    val hasError = displayMessage != null
    val style = LocalDepthStyle.current
    val insetShape = RoundedCornerShape(Dimens.RadiusReportInput)
    Column(modifier) {
        FieldLabel(label)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = Dimens.TextFieldHeightReport)
                .depthInset(
                    enabled = enabled,
                    isError = hasError,
                    isFocused = isFocused,
                    shape = insetShape,
                    cornerRadius = Dimens.RadiusReportInput,
                    borderColorOverride = if (!hasError && !isFocused) {
                        if (enabled) style.reportFieldBorderColor else DepthTokens.ReportReadOnlyBorderColor
                    } else null,
                    topShadowAlphaOverride = if (enabled) style.reportFieldInsetAlpha else DepthTokens.REPORT_READ_ONLY_INSET_ALPHA,
                    disabledBackgroundOverride = if (!enabled) DepthTokens.ReportReadOnlyBackground else null,
                    insetShadowOffsetYOverride = if (!enabled) Dimens.ReportReadOnlyShadowOffsetY else null,
                    insetShadowBlurOverride = if (!enabled) Dimens.ReportReadOnlyShadowBlur else null
                )
                .padding(horizontal = Dimens.InsetFieldPaddingHorizontal, vertical = Dimens.SpacingSm),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = value,
                onValueChange = { new: String ->
                    val filtered = if (isInteger) {
                        new.filter { it.isDigit() }
                    } else {
                        val normalizedNew = new.replace('\u060C', ',')
                        if (COMMA_THREE_DIGITS_REGEX.containsMatchIn(normalizedNew) ||
                            (normalizedNew.contains(',') && !value.contains('.'))
                        ) {
                            commaUsedAsSeparator = true
                        }
                        var hasDot = false
                        val result = buildString {
                            for (char in normalizedNew) {
                                if (char.isDigit()) {
                                    append(char)
                                } else if (char == '.' || char == ',' || char == '\u066B') {
                                    if (!hasDot) {
                                        append('.')
                                        hasDot = true
                                    }
                                }
                            }
                        }
                        if (!hasDot) {
                            commaUsedAsSeparator = false
                        }
                        result
                    }
                    onChange(filtered)
                },
                enabled = enabled,
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = DepthTypeTokens.ReportFieldTextSize,
                    lineHeight = DepthTypeTokens.LogoTitleSize,
                    fontWeight = if (enabled) FontWeight.Medium else FontWeight.Normal,
                    color = if (enabled) style.reportFieldTextColor else DepthTokens.ReportReadOnlyTextColor
                ),
                cursorBrush = SolidColor(if (hasError) ErrorRedLight else ShiftColors.Primary),
                keyboardOptions = KeyboardOptions(keyboardType = if (isInteger) KeyboardType.Number else KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isFocused = it.isFocused },
                decorationBox = @Composable { innerTextField: @Composable () -> Unit ->
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (value.isEmpty()) {
                            Text(
                                text = if (isInteger) "0" else com.lojia.shiftreport.util.MoneyFormat.zeroPlaceholder(),
                                fontSize = DepthTypeTokens.ReportFieldTextSize,
                                lineHeight = DepthTypeTokens.LogoTitleSize,
                                fontWeight = FontWeight.Normal,
                                color = if (enabled) style.reportFieldPlaceholderColor else DepthTokens.ReportReadOnlyTextColor
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
        if (displayMessage != null) {
            Spacer(Modifier.height(Dimens.SpacingXxs))
            Text(
                text = displayMessage,
                color = ErrorRedLight,
                fontSize = DepthTypeTokens.RegisterLabelSize,
                lineHeight = DepthTypeTokens.SectionTitleSize,
                modifier = Modifier.padding(start = Dimens.SpacingXxs)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabeledDropdown(
    label: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val style = LocalDepthStyle.current
    val insetShape = RoundedCornerShape(Dimens.RadiusReportInput)
    Column {
        FieldLabel(label)
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            Box(
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = Dimens.TextFieldHeightReport)
                    .depthInset(
                        isFocused = expanded,
                        shape = insetShape,
                        cornerRadius = Dimens.RadiusReportInput,
                        borderColorOverride = if (!expanded) style.reportFieldBorderColor else null,
                        topShadowAlphaOverride = style.reportFieldInsetAlpha
                    )
                    .padding(horizontal = Dimens.InsetFieldPaddingHorizontal, vertical = Dimens.SpacingSm),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selected,
                        fontSize = DepthTypeTokens.ReportFieldTextSize,
                        fontWeight = FontWeight.Medium,
                        color = style.reportFieldTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
            }
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = PureWhite,
                shape = RoundedCornerShape(Dimens.RadiusMd),
                border = androidx.compose.foundation.BorderStroke(Dimens.CardBorderWidth, DepthCardBorder)
            ) {
                options.forEach { opt ->
                    val isSelected = opt == selected
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = opt,
                                fontSize = DepthTypeTokens.InsetPlaceholderSize,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) ShiftColors.Primary else style.reportFieldTextColor
                            )
                        },
                        onClick = {
                            onSelected(opt)
                            expanded = false
                        },
                        colors = MenuDefaults.itemColors(
                            textColor = style.reportFieldTextColor,
                            leadingIconColor = style.reportFieldTextColor,
                            trailingIconColor = style.reportFieldTextColor
                        ),
                        modifier = Modifier.background(
                            if (isSelected) BadgeBlueBg else PureWhite
                        ),
                        contentPadding = PaddingValues(horizontal = Dimens.SpacingMd, vertical = Dimens.SpacingSm)
                    )
                }
            }
        }
    }
}

@Composable
fun PayTypeSelector(selected: PayType, onChange: (PayType) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpacingXs)
    ) {
        Text(
            text = stringResource(R.string.type),
            fontSize = DepthTypeTokens.ReportFieldLabelSize,
            fontWeight = FontWeight.Medium,
            color = TextSlate700
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Dimens.SegmentTrackHeight)
                .depthInset(
                    cornerRadius = Dimens.RadiusSegmentTrack,
                    gradientColors = listOf(SegmentTrackBg, SegmentTrackBg),
                    borderColorOverride = DepthCardBorder,
                    topShadowAlphaOverride = DepthTokens.SEGMENT_TRACK_INSET_ALPHA
                )
                .padding(Dimens.SpacingXs)
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingXs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(
                PayType.CASH to stringResource(R.string.cash),
                PayType.BANK to stringResource(R.string.bank)
            ).forEach { (type, label) ->
                val isSelected = selected == type
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Dimens.SegmentThumbHeight)
                        .clip(RoundedCornerShape(Dimens.RadiusSegmentThumb))
                        .then(
                            if (isSelected) {
                                Modifier.depthSegmentActiveThumb(cornerRadius = Dimens.RadiusSegmentThumb)
                            } else {
                                Modifier
                            }
                        )
                        .selectable(
                            selected = isSelected,
                            onClick = { onChange(type) },
                            role = Role.RadioButton
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = DepthTypeTokens.SegmentedChipTextSize,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) PureWhite else TextSlate700,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
