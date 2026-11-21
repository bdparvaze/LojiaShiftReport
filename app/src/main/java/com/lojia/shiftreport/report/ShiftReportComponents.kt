package com.lojia.shiftreport.report

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
    val pillShape = RoundedCornerShape(Dimens.RadiusSm)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 6.dp)
            .depthSemanticCard(
                backgroundColor = bg,
                borderColor = borderColor.copy(alpha = 0.4f),
                shape = pillShape
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Text(icon, fontSize = 12.sp)
            Spacer(Modifier.width(6.dp))
            Text(
                title,
                color = ShiftColors.Charcoal,
                fontWeight = FontWeight.Bold,
                fontSize = 11.5.sp,
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
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier
                    .heightIn(min = Dimens.MinTouchTarget)
                    .depthSecondaryButton(
                        borderColor = accent,
                        cornerRadius = Dimens.RadiusMd
                    )
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.add), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
    }
}

/* ----------------------------------------------------------------------
 * DYNAMIC ENTRY ROW
 * ---------------------------------------------------------------------- */
@Composable
fun EntryRow(left: String, right: String, onRemove: () -> Unit) {
    val rowShape = RoundedCornerShape(Dimens.RadiusSm)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 5.dp)
            .depthCard(shape = rowShape)
            .padding(start = 10.dp, end = 2.dp, top = 2.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(left, fontSize = 12.5.sp, color = ShiftColors.Text, modifier = Modifier.weight(1f))
        Text(right, fontSize = 12.5.sp, color = ShiftColors.Text, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(4.dp))
        IconButton(
            onClick = onRemove,
            modifier = Modifier.minimumInteractiveComponentSize()
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(ShiftColors.DangerLight)
                    .border(1.dp, ErrorContainerLight, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = stringResource(R.string.remove),
                    tint = ShiftColors.Danger,
                    modifier = Modifier.size(14.dp)
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
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = ShiftColors.TextMuted,
        modifier = Modifier.padding(bottom = 3.dp),
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
    val insetShape = RoundedCornerShape(Dimens.RadiusSm)
    Column(modifier) {
        FieldLabel(label)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = Dimens.TextFieldHeightCompact)
                .depthInset(
                    enabled = enabled,
                    isError = hasError,
                    isFocused = isFocused,
                    shape = insetShape
                )
                .padding(horizontal = 10.dp, vertical = 6.dp),
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
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (enabled) ShiftColors.Charcoal else TextDisabledColor
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
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                color = TextHintColor
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
        if (displayMessage != null) {
            Spacer(Modifier.height(3.dp))
            Text(
                text = displayMessage,
                color = ErrorRedLight,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                modifier = Modifier.padding(start = 2.dp)
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
    val insetShape = RoundedCornerShape(Dimens.RadiusSm)
    Column {
        FieldLabel(label)
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            Box(
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = Dimens.MinTouchTarget)
                    .depthInset(isFocused = expanded, shape = insetShape)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selected,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = ShiftColors.Charcoal,
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
                border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariantLight)
            ) {
                options.forEach { opt ->
                    val isSelected = opt == selected
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = opt,
                                fontSize = 13.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) ShiftColors.Primary else OnBackgroundLight
                            )
                        },
                        onClick = {
                            onSelected(opt)
                            expanded = false
                        },
                        colors = MenuDefaults.itemColors(
                            textColor = OnBackgroundLight,
                            leadingIconColor = OnBackgroundLight,
                            trailingIconColor = OnBackgroundLight
                        ),
                        modifier = Modifier.background(
                            if (isSelected) PrimaryContainerLight else PureWhite
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PayTypeSelector(selected: PayType, onChange: (PayType) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(R.string.type),
            fontSize = 12.sp,
            color = ShiftColors.TextMuted,
            modifier = Modifier.padding(end = 12.dp)
        )
        FilterChip(
            selected = selected == PayType.CASH,
            onClick = { onChange(PayType.CASH) },
            label = { Text(stringResource(R.string.cash)) },
            elevation = FilterChipDefaults.filterChipElevation(elevation = Dimens.SecondaryButtonElevation),
            colors = FilterChipDefaults.filterChipColors(
                containerColor = LocalDepthStyle.current.cardGradientStart,
                selectedContainerColor = PrimaryContainerLight,
                selectedLabelColor = PrimaryIndigoDark
            ),
            modifier = Modifier.heightIn(min = Dimens.MinTouchTarget)
        )
        Spacer(Modifier.width(8.dp))
        FilterChip(
            selected = selected == PayType.BANK,
            onClick = { onChange(PayType.BANK) },
            label = { Text(stringResource(R.string.bank)) },
            elevation = FilterChipDefaults.filterChipElevation(elevation = Dimens.SecondaryButtonElevation),
            colors = FilterChipDefaults.filterChipColors(
                containerColor = LocalDepthStyle.current.cardGradientStart,
                selectedContainerColor = PrimaryContainerLight,
                selectedLabelColor = PrimaryIndigoDark
            ),
            modifier = Modifier.heightIn(min = Dimens.MinTouchTarget)
        )
    }
}
