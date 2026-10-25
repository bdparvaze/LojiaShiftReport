package com.lojia.shiftreport.settings

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.lojia.shiftreport.R
import com.lojia.shiftreport.data.AppCountry
import com.lojia.shiftreport.ui.theme.*


// =============================================================================
// SECTION HEADER & INLINE EDITABLE PROFILE ROW (LojiaSettingsCard Accordion Spec)
// =============================================================================

@Composable
fun ProfileSectionHeader(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 3.dp, height = 14.dp)
                .background(PrimaryIndigoLight, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextHintColor,
            letterSpacing = 0.8.sp
        )
    }
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
    valueFontSize: TextUnit = 13.sp,
    valueFontWeight: FontWeight = FontWeight.Normal,
    valueFontFamily: FontFamily? = null,
    valueMaxLines: Int = 1,
    testTag: String = "",
    editContent: @Composable () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .then(if (testTag.isNotBlank()) Modifier.testTag(testTag) else Modifier),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, OutlineLight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Collapsed Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = OnSurfaceLight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = value.ifBlank { "Not set" },
                        fontSize = valueFontSize,
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
                    modifier = Modifier.size(20.dp)
                )
            }

            // Expanded Inline Editor Section
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = tween(220)) + fadeIn(animationSpec = tween(220)),
                exit = shrinkVertically(animationSpec = tween(180)) + fadeOut(animationSpec = tween(180))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = OutlineVariantLight,
                        modifier = Modifier.padding(vertical = 12.dp)
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
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(
                text = label,
                fontSize = 12.sp,
                color = OnSurfaceVariantLight,
                fontWeight = FontWeight.Medium
            )
        },
        placeholder = if (placeholder.isNotBlank()) {
            { Text(text = placeholder, fontSize = 13.sp, color = TextHintColor) }
        } else null,
        singleLine = singleLine,
        isError = isError,
        supportingText = supportingText,
        visualTransformation = visualTransformation,
        trailingIcon = trailingIcon,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = OnSurfaceLight,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = OnSurfaceLight,
            unfocusedTextColor = OnSurfaceLight,
            focusedContainerColor = PureWhite,
            unfocusedContainerColor = PureWhite,
            disabledContainerColor = SurfaceVariantLight,
            errorContainerColor = ErrorContainer,
            focusedLabelColor = PrimaryIndigoLight,
            unfocusedLabelColor = OnSurfaceVariantLight,
            focusedPlaceholderColor = TextHintColor,
            unfocusedPlaceholderColor = TextHintColor,
            focusedBorderColor = PrimaryIndigoLight,
            unfocusedBorderColor = OutlineLight,
            cursorColor = PrimaryIndigoLight,
            errorBorderColor = ErrorRedLight
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .then(if (testTag.isNotBlank()) Modifier.testTag(testTag) else Modifier)
    )
}

// =============================================================================
// Helper Functions & Dialogs for Phone & Country Selection
// =============================================================================

/**
 * Parses raw stored phone strings into a paired (AppCountry, LocalNumberDigits)
 */
fun parsePhoneNumberWithCountry(rawPhone: String, defaultCountry: AppCountry): Pair<AppCountry, String> {
    val clean = rawPhone.trim()
    if (clean.isBlank()) return Pair(defaultCountry, "")

    // 1. Check exact country dial code prefix (e.g. "+880", "+966", "+1")
    for (country in AppCountry.entries) {
        if (clean.startsWith(country.dialCode)) {
            val local = clean.removePrefix(country.dialCode).trim()
            return Pair(country, local)
        }
    }
    // 2. Check if starts with "+" then digits
    if (clean.startsWith("+")) {
        val digits = clean.removePrefix("+")
        for (country in AppCountry.entries) {
            val countryDigits = country.dialCode.removePrefix("+")
            if (digits.startsWith(countryDigits)) {
                val local = digits.removePrefix(countryDigits).trim()
                return Pair(country, local)
            }
        }
    }
    return Pair(defaultCountry, clean)
}

/**
 * Material 3 Searchable Country Code & Dial Code Picker Dialog
 */
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

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .widthIn(max = Dimens.DialogMaxWidth)
                .fillMaxWidth()
                .padding(horizontal = Dimens.SpacingLg)
                .shadow(Dimens.DialogElevation, RoundedCornerShape(Dimens.DialogCornerRadius))
                .testTag("dialog_select_country_code"),
            shape = RoundedCornerShape(Dimens.DialogCornerRadius),
            colors = CardDefaults.cardColors(containerColor = PureWhite)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.auth_select_country_code),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = OnSurfaceLight,
                            fontSize = 17.sp
                        )
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.cd_close),
                            tint = OnSurfaceVariantLight,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(R.string.search_country), fontSize = 13.5.sp, color = TextHintColor) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = OnSurfaceVariantLight) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = OnSurfaceLight,
                        fontSize = 14.sp
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = OnSurfaceLight,
                        unfocusedTextColor = OnSurfaceLight,
                        focusedContainerColor = BackgroundLight,
                        unfocusedContainerColor = BackgroundLight,
                        focusedBorderColor = PrimaryIndigoLight,
                        unfocusedBorderColor = OutlineLight,
                        cursorColor = PrimaryIndigoLight
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("search_country_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredCountries) { country ->
                        val isSelected = country == selectedCountry
                        val flag = countryCodeToFlagEmoji(country.code)
                        val countryName = stringResource(country.nameRes)

                        Surface(
                            onClick = {
                                onCountrySelected(country)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) PrimaryContainerLight else PureWhite,
                            modifier = Modifier.fillMaxWidth().testTag("country_option_${country.code}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(text = flag, fontSize = 22.sp, color = OnSurfaceLight)
                                    Column {
                                        Text(
                                            text = countryName,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) PrimaryIndigoLight else OnSurfaceLight,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "${country.displayName} (${country.code})",
                                            color = OnSurfaceVariantLight,
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) PrimaryContainerLight else SurfaceVariantLight
                                ) {
                                    Text(
                                        text = country.dialCode,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) PrimaryIndigoDark else OnSurfaceVariantLight,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
