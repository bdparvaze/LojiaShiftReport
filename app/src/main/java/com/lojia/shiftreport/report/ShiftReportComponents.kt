package com.lojia.shiftreport.report

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 6.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, borderColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 12.sp)
            Spacer(Modifier.width(6.dp))
            Text(title, color = ShiftColors.Charcoal, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
        }
        if (onAdd != null) {
            OutlinedButton(
                onClick = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    onAdd()
                },
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, accent),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = accent),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.defaultMinSize(minHeight = 28.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(2.dp))
                Text(stringResource(R.string.add), fontSize = 10.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
    }
}

/* ----------------------------------------------------------------------
 * DYNAMIC ENTRY ROW
 * ---------------------------------------------------------------------- */
@Composable
fun EntryRow(left: String, right: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 5.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(ShiftColors.Bg)
            .border(1.dp, ShiftColors.Border, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(left, fontSize = 12.5.sp, color = ShiftColors.Text, modifier = Modifier.weight(1f))
        Text(right, fontSize = 12.5.sp, color = ShiftColors.Text, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(ShiftColors.DangerLight)
                .border(1.dp, Color(0xFFFECACA), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.remove), tint = ShiftColors.Danger, modifier = Modifier.size(12.dp))
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
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    disabledContainerColor = BackgroundLight,
    focusedBorderColor = ShiftColors.Primary,
    unfocusedBorderColor = OutlineLight,
    focusedLabelColor = ShiftColors.Primary,
    unfocusedLabelColor = OnSurfaceVariantLight,
    focusedPlaceholderColor = TextHintColor,
    unfocusedPlaceholderColor = TextHintColor,
    cursorColor = ShiftColors.Primary
)

@Composable
fun NumberField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isInteger: Boolean = false
) {
    Column(modifier) {
        FieldLabel(label)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 46.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (enabled) Color.White else SurfaceVariantLight)
                .border(
                    width = 1.dp,
                    color = if (enabled) OutlineLight else OutlineVariantLight,
                    shape = RoundedCornerShape(8.dp)
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
                        var hasDot = false
                        buildString {
                            for (char in new) {
                                if (char.isDigit()) {
                                    append(char)
                                } else if (char == '.' || char == ',') {
                                    if (!hasDot) {
                                        append('.')
                                        hasDot = true
                                    }
                                }
                            }
                        }
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
                keyboardOptions = KeyboardOptions(keyboardType = if (isInteger) KeyboardType.Number else KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = @Composable { innerTextField: @Composable () -> Unit ->
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (value.isEmpty()) {
                            Text(
                                text = if (isInteger) "0" else "0.00",
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
    Column {
        FieldLabel(label)
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            Box(
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .border(1.dp, OutlineLight, RoundedCornerShape(8.dp))
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
                containerColor = Color.White,
                shape = RoundedCornerShape(10.dp),
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
                            if (isSelected) PrimaryContainerLight else Color.White
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
        Text(stringResource(R.string.type), fontSize = 12.sp, color = ShiftColors.TextMuted, modifier = Modifier.padding(end = 12.dp))
        FilterChip(selected = selected == PayType.CASH, onClick = { onChange(PayType.CASH) }, label = { Text(stringResource(R.string.cash)) })
        Spacer(Modifier.width(8.dp))
        FilterChip(selected = selected == PayType.BANK, onClick = { onChange(PayType.BANK) }, label = { Text(stringResource(R.string.bank)) })
    }
}
