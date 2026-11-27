package com.lojia.shiftreport.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import com.lojia.shiftreport.R
import com.lojia.shiftreport.data.AppCountry
import com.lojia.shiftreport.ui.common.DialogSize
import com.lojia.shiftreport.ui.common.LojiaDialog
import com.lojia.shiftreport.ui.common.LojiaSectionHeader
import com.lojia.shiftreport.ui.common.LojiaTextField
import com.lojia.shiftreport.ui.theme.Dimens
import com.lojia.shiftreport.ui.theme.OnSurfaceLight
import com.lojia.shiftreport.ui.theme.OnSurfaceVariantLight
import com.lojia.shiftreport.ui.theme.OutlineVariantLight
import com.lojia.shiftreport.ui.theme.PrimaryContainerLight
import com.lojia.shiftreport.ui.theme.PrimaryIndigoDark
import com.lojia.shiftreport.ui.theme.PrimaryIndigoLight
import com.lojia.shiftreport.ui.theme.SurfaceVariantLight
import com.lojia.shiftreport.ui.theme.TextHintColor
import com.lojia.shiftreport.ui.theme.depthCard

// =============================================================================
// SECTION HEADER & INLINE EDITABLE PROFILE ROW
// =============================================================================

@Composable
fun ProfileSectionHeader(title: String) {
    LojiaSectionHeader(text = title, uppercase = true)
}

@Composable
fun InlineEditableProfileRow(
    icon: ImageVector,
    label: String,
    currentValue: String,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconTint: Color = OnSurfaceVariantLight,
    testTag: String = "row_saved_addresses"
) {
    InlineEditableProfileRow(
        icon = icon,
        iconTint = iconTint,
        title = label,
        value = currentValue,
        isExpanded = false,
        onToggle = onEditClick,
        modifier = modifier,
        testTag = testTag,
        editContent = {}
    )
}

@Composable
fun InlineEditableProfileRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    value: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    valueColor: Color = OnSurfaceVariantLight,
    valueFontSize: TextUnit = TextUnit.Unspecified,
    valueFontWeight: FontWeight = FontWeight.Normal,
    valueFontFamily: FontFamily? = null,
    valueMaxLines: Int = 1,
    testTag: String = "",
    editContent: @Composable () -> Unit
) {
    val resolvedValueStyle = if (valueFontSize == TextUnit.Unspecified) {
        MaterialTheme.typography.bodyMedium
    } else {
        MaterialTheme.typography.bodyMedium.copy(fontSize = valueFontSize)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = Dimens.SpacingSm)
            .then(if (testTag.isNotBlank()) Modifier.testTag(testTag) else Modifier)
            .depthCard(
                accent = isExpanded,
                shape = RoundedCornerShape(Dimens.CardCornerRadius)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.CardPadding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = Dimens.MinTouchTarget)
                    .clickable { onToggle() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(Dimens.IconMd)
                )

                Spacer(modifier = Modifier.width(Dimens.SpacingMd))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = OnSurfaceLight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(Dimens.SpacingXxs))
                    Text(
                        text = value.ifBlank { stringResource(R.string.not_set) },
                        style = resolvedValueStyle,
                        fontWeight = valueFontWeight,
                        fontFamily = valueFontFamily,
                        color = valueColor,
                        maxLines = valueMaxLines,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = if (isExpanded) stringResource(R.string.cd_collapse) else stringResource(R.string.cd_expand),
                    tint = if (isExpanded) PrimaryIndigoLight else TextHintColor,
                    modifier = Modifier.size(Dimens.IconMd)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = tween(220)) + fadeIn(animationSpec = tween(220)),
                exit = shrinkVertically(animationSpec = tween(180)) + fadeOut(animationSpec = tween(180))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(
                        thickness = Dimens.RowDividerWidth,
                        color = OutlineVariantLight,
                        modifier = Modifier.padding(vertical = Dimens.SpacingMd)
                    )
                    editContent()
                }
            }
        }
    }
}

// =============================================================================
// Helper Components & Text Fields
// =============================================================================

@Composable
fun ProfileDialogTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
    testTag: String = ""
) {
    LojiaTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariantLight,
                fontWeight = FontWeight.Medium
            )
        },
        placeholder = if (placeholder.isNotBlank()) {
            { Text(text = placeholder, style = MaterialTheme.typography.bodyMedium, color = TextHintColor) }
        } else null,
        singleLine = singleLine,
        isError = isError,
        supportingText = supportingText,
        visualTransformation = visualTransformation,
        trailingIcon = trailingIcon,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = OnSurfaceLight,
            fontWeight = FontWeight.Normal
        ),
        shape = RoundedCornerShape(Dimens.RadiusMd),
        modifier = modifier
            .fillMaxWidth()
            .then(if (testTag.isNotBlank()) Modifier.testTag(testTag) else Modifier)
    )
}

// =============================================================================
// Helper Functions & Dialogs for Phone & Country Selection
// =============================================================================

fun parsePhoneNumberWithCountry(rawPhone: String, defaultCountry: AppCountry): Pair<AppCountry, String> {
    val clean = rawPhone.trim()
    if (clean.isBlank()) return Pair(defaultCountry, "")

    val orderedCountries = AppCountry.entries.sortedWith(
        compareByDescending<AppCountry> { it.dialCode.length }
            .thenByDescending { it == defaultCountry }
    )

    if (clean.startsWith("+")) {
        val digits = clean.filter { it.isDigit() }
        for (country in orderedCountries) {
            val countryDigits = country.dialCode.filter { it.isDigit() }
            if (countryDigits.isNotEmpty() && digits.startsWith(countryDigits)) {
                return Pair(country, digits.removePrefix(countryDigits))
            }
        }
        return Pair(defaultCountry, digits)
    }

    return Pair(defaultCountry, clean.filter { it.isDigit() })
}

@Composable
fun CountryCodePickerDialog(
    selectedCountry: AppCountry,
    onCountrySelected: (AppCountry) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    val countries = remember { AppCountry.entries }
    val filteredCountries = remember(searchQuery) {
        if (searchQuery.isBlank()) countries else {
            countries.filter { country ->
                val localizedName = context.getString(country.nameRes)
                localizedName.contains(searchQuery, ignoreCase = true) ||
                    country.displayName.contains(searchQuery, ignoreCase = true) ||
                    country.code.contains(searchQuery, ignoreCase = true) ||
                    country.dialCode.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    LojiaDialog(
        onDismissRequest = onDismiss,
        size = DialogSize.Large,
        modifier = Modifier.testTag("dialog_select_country_code")
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.auth_select_country_code),
                    style = MaterialTheme.typography.headlineSmall,
                    color = OnSurfaceLight
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.cd_close),
                        tint = OnSurfaceVariantLight,
                        modifier = Modifier.size(Dimens.IconMd)
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.SpacingSm))

            LojiaTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = stringResource(R.string.search_country),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextHintColor
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Search,
                        contentDescription = null,
                        tint = OnSurfaceVariantLight,
                        modifier = Modifier.size(Dimens.IconField)
                    )
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = OnSurfaceLight),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_country_input")
            )

            Spacer(modifier = Modifier.height(Dimens.SpacingSm))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = Dimens.DialogListMaxHeight),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpacingXs)
            ) {
                items(filteredCountries) { country ->
                    val isSelected = country == selectedCountry
                    val flag = countryCodeToFlagEmoji(country.code)
                    val countryName = stringResource(country.nameRes)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = Dimens.MinTouchTarget)
                            .depthCard(
                                accent = isSelected,
                                shape = RoundedCornerShape(Dimens.RadiusMd)
                            )
                            .clickable {
                                onCountrySelected(country)
                                onDismiss()
                            }
                            .testTag("country_option_${country.code}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = Dimens.ListRowPaddingHorizontal,
                                    vertical = Dimens.ListRowPaddingVertical
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = flag,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = OnSurfaceLight
                                )
                                Column {
                                    Text(
                                        text = countryName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) PrimaryIndigoLight else OnSurfaceLight
                                    )
                                    Text(
                                        text = "$countryName (${country.code})",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = OnSurfaceVariantLight
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(Dimens.RadiusMd),
                                color = if (isSelected) PrimaryContainerLight else SurfaceVariantLight
                            ) {
                                Text(
                                    text = country.dialCode,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) PrimaryIndigoDark else OnSurfaceVariantLight,
                                    modifier = Modifier.padding(
                                        horizontal = Dimens.SpacingSm,
                                        vertical = Dimens.SpacingXs
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
