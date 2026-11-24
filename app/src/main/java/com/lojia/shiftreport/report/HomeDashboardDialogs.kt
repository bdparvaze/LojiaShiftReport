package com.lojia.shiftreport.report

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import com.lojia.shiftreport.R
import com.lojia.shiftreport.data.ShiftSession
import com.lojia.shiftreport.ui.common.LojiaFormDialog
import com.lojia.shiftreport.ui.common.LojiaTextField
import com.lojia.shiftreport.ui.theme.DepthTokens
import com.lojia.shiftreport.ui.theme.DepthTypeTokens
import com.lojia.shiftreport.ui.theme.Dimens
import com.lojia.shiftreport.ui.theme.ErrorRedLight
import com.lojia.shiftreport.ui.theme.PureWhite
import com.lojia.shiftreport.ui.theme.TextPrimaryReport
import com.lojia.shiftreport.util.MoneyFormat
import com.lojia.shiftreport.util.MoneyInput
import com.lojia.shiftreport.util.classifyMoneyInput
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeOpenShiftDialog(
    cashierOptions: List<String>,
    defaultCashier: String,
    currency: String,
    onDismiss: () -> Unit,
    onConfirmOpen: (cashierName: String, shiftName: String, startingCash: Double) -> Unit
) {
    val dayLabel = stringResource(R.string.shift_day)
    val nightLabel = stringResource(R.string.shift_night_option)
    val digits = remember(currency) { MoneyFormat.minorUnitDigits(currency) }

    var cashier by remember(defaultCashier, cashierOptions) {
        mutableStateOf(defaultCashier.ifBlank { cashierOptions.firstOrNull().orEmpty() })
    }
    var cashierExpanded by remember { mutableStateOf(false) }
    var selectedShift by remember(dayLabel) { mutableStateOf(dayLabel) }
    var startingFloatText by remember { mutableStateOf("") }

    val classifiedFloat = classifyMoneyInput(startingFloatText, digits)
    val isFloatError = startingFloatText.isNotBlank() && classifiedFloat is MoneyInput.Invalid
    val startingMinor = when (classifiedFloat) {
        is MoneyInput.Valid -> classifiedFloat.minor
        MoneyInput.Empty, MoneyInput.Invalid -> 0L
    }
    val canConfirm = !isFloatError && (cashierOptions.isEmpty() || cashier.isNotBlank())

    LojiaFormDialog(
        title = stringResource(R.string.home_open_shift_dialog_title),
        confirmText = stringResource(R.string.open_shift),
        onDismissRequest = onDismiss,
        onConfirm = {
            val major = MoneyFormat.toMajorUnits(startingMinor, digits)
            onConfirmOpen(cashier, selectedShift, major)
        },
        isConfirmEnabled = canConfirm,
        confirmTestTag = "home_dialog_open_shift_confirm",
        dismissTestTag = "home_dialog_open_shift_cancel"
    ) {
        if (cashierOptions.isNotEmpty()) {
            ExposedDropdownMenuBox(
                expanded = cashierExpanded,
                onExpandedChange = { cashierExpanded = !cashierExpanded }
            ) {
                LojiaTextField(
                    value = cashier,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.cashier)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cashierExpanded) },
                    shape = RoundedCornerShape(Dimens.RadiusReportInput),
                    minHeight = Dimens.TextFieldHeightReport,
                    borderColorOverride = DepthTokens.ReportFieldBorderColor,
                    topShadowAlphaOverride = DepthTokens.INSET_REPORT_TOP_SHADOW_ALPHA,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = cashierExpanded,
                    onDismissRequest = { cashierExpanded = false },
                    containerColor = PureWhite
                ) {
                    cashierOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option, color = TextPrimaryReport) },
                            onClick = {
                                cashier = option
                                cashierExpanded = false
                            }
                        )
                    }
                }
            }
        } else {
            LojiaTextField(
                value = cashier,
                onValueChange = { cashier = it },
                label = { Text(stringResource(R.string.cashier)) },
                singleLine = true,
                shape = RoundedCornerShape(Dimens.RadiusReportInput),
                minHeight = Dimens.TextFieldHeightReport,
                borderColorOverride = DepthTokens.ReportFieldBorderColor,
                topShadowAlphaOverride = DepthTokens.INSET_REPORT_TOP_SHADOW_ALPHA,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpacingXs)) {
            Text(
                text = stringResource(R.string.shift),
                fontSize = DepthTypeTokens.ReportFieldLabelSize,
                fontWeight = FontWeight.Medium,
                color = DepthTokens.ReportFieldLabelColor
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)
            ) {
                listOf(dayLabel, nightLabel).forEach { option ->
                    val isSelected = selectedShift == option
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = Dimens.MinTouchTarget)
                            .clip(RoundedCornerShape(Dimens.RadiusMd))
                            .selectable(
                                selected = isSelected,
                                onClick = { selectedShift = option },
                                role = Role.RadioButton
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = isSelected, onClick = null)
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = TextPrimaryReport
                        )
                    }
                }
            }
        }

        LojiaTextField(
            value = startingFloatText,
            onValueChange = { startingFloatText = it },
            label = { Text(stringResource(R.string.home_open_shift_float_label)) },
            placeholder = { Text(MoneyFormat.zeroPlaceholder(digits)) },
            suffix = { Text(currency) },
            isError = isFloatError,
            supportingText = if (isFloatError) {
                { Text(stringResource(R.string.valid_amount_error), color = ErrorRedLight) }
            } else null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            shape = RoundedCornerShape(Dimens.RadiusReportInput),
            minHeight = Dimens.TextFieldHeightReport,
            borderColorOverride = DepthTokens.ReportFieldBorderColor,
            topShadowAlphaOverride = DepthTokens.INSET_REPORT_TOP_SHADOW_ALPHA,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("home_open_shift_float_input")
        )
    }
}

@Composable
fun HomeCloseShiftDialog(
    session: ShiftSession,
    expectedDrawerCashMinor: Long,
    currency: String,
    locale: Locale,
    onDismiss: () -> Unit,
    onConfirmClose: (actualCashCount: Double, notes: String) -> Unit
) {
    val digits = remember(currency) { MoneyFormat.minorUnitDigits(currency) }
    var actualCashText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }

    val classifiedActual = classifyMoneyInput(actualCashText, digits)
    val isActualError = actualCashText.isNotBlank() && classifiedActual is MoneyInput.Invalid
    val actualMinor = when (classifiedActual) {
        is MoneyInput.Valid -> classifiedActual.minor
        MoneyInput.Empty, MoneyInput.Invalid -> null
    }
    val canConfirm = actualMinor != null && !isActualError

    LojiaFormDialog(
        title = stringResource(R.string.home_close_shift_dialog_title),
        confirmText = stringResource(R.string.close_shift),
        onDismissRequest = onDismiss,
        onConfirm = {
            if (actualMinor != null) {
                onConfirmClose(MoneyFormat.toMajorUnits(actualMinor, digits), notesText.trim())
            }
        },
        isConfirmEnabled = canConfirm,
        isDestructive = true,
        confirmTestTag = "home_dialog_close_shift_confirm",
        dismissTestTag = "home_dialog_close_shift_cancel"
    ) {
        Text(
            text = "${stringResource(R.string.home_kpi_expected_drawer)}: ${MoneyFormat.formatMinor(expectedDrawerCashMinor, currency, locale)}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimaryReport
        )

        Text(
            text = "${session.shiftName} • ${session.cashierName}",
            style = MaterialTheme.typography.bodySmall,
            color = DepthTokens.ReportFieldPlaceholderColor
        )

        LojiaTextField(
            value = actualCashText,
            onValueChange = { actualCashText = it },
            label = { Text(stringResource(R.string.home_close_shift_actual_label)) },
            placeholder = { Text(MoneyFormat.zeroPlaceholder(digits)) },
            suffix = { Text(currency) },
            isError = isActualError,
            supportingText = if (isActualError) {
                { Text(stringResource(R.string.valid_amount_error), color = ErrorRedLight) }
            } else null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            shape = RoundedCornerShape(Dimens.RadiusReportInput),
            minHeight = Dimens.TextFieldHeightReport,
            borderColorOverride = DepthTokens.ReportFieldBorderColor,
            topShadowAlphaOverride = DepthTokens.INSET_REPORT_TOP_SHADOW_ALPHA,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("home_close_shift_actual_input")
        )

        LojiaTextField(
            value = notesText,
            onValueChange = { notesText = it },
            label = { Text(stringResource(R.string.home_close_shift_notes_label)) },
            shape = RoundedCornerShape(Dimens.RadiusReportInput),
            minHeight = Dimens.TextFieldHeightReport,
            borderColorOverride = DepthTokens.ReportFieldBorderColor,
            topShadowAlphaOverride = DepthTokens.INSET_REPORT_TOP_SHADOW_ALPHA,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("home_close_shift_notes_input")
        )
    }
}
