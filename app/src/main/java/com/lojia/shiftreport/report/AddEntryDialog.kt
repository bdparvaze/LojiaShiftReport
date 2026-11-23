package com.lojia.shiftreport.report

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojia.shiftreport.R
import com.lojia.shiftreport.ui.common.LojiaDialog
import com.lojia.shiftreport.ui.common.LojiaTextField
import com.lojia.shiftreport.ui.theme.*
import com.lojia.shiftreport.util.MoneyFormat
import com.lojia.shiftreport.util.MoneyInput
import com.lojia.shiftreport.util.RECEIPT_UNSPECIFIED
import com.lojia.shiftreport.util.classifyMoneyInput

private const val MAX_ITEM_DESCRIPTION_LENGTH = 100

enum class PayType { CASH, BANK }

enum class ModalType { NONE, CREDIT, OLD_DUE, STAFF, WALKOUT, ITEM }

data class ModalResult(
    val receipt: String = "",
    val name: String = "",
    val amount: Double = 0.0,
    val qty: Int = 0,
    val unitPrice: Double = 0.0,
    val type: PayType = PayType.CASH
)

/* ----------------------------------------------------------------------
 * ADD-ENTRY MODAL (covers Credit / Old Due / Staff / Walkout / Item)
 * ---------------------------------------------------------------------- */
@Composable
fun AddEntryDialog(
    type: ModalType,
    currentCurrency: String = MoneyFormat.DEFAULT_CURRENCY_CODE,
    onDismiss: () -> Unit,
    onSubmit: (ModalResult) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var receipt by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var qtyText by remember { mutableStateOf("") }
    var unitPriceText by remember { mutableStateOf("") }
    var showOptionalDetails by remember { mutableStateOf(false) }
    var payType by remember { mutableStateOf(PayType.CASH) }
    var error by remember { mutableStateOf<String?>(null) }
    val digits = MoneyFormat.minorUnitDigits(currentCurrency)
    val invalidAmountError = stringResource(R.string.valid_amount_error)
    val amountClass = classifyMoneyInput(amountText, digits)
    val unitPriceClass = if (type == ModalType.ITEM) classifyMoneyInput(unitPriceText, digits) else MoneyInput.Empty
    val isAmountInvalid = amountClass is MoneyInput.Invalid
    val isUnitPriceInvalid = unitPriceClass is MoneyInput.Invalid

    val title = when (type) {
        ModalType.CREDIT -> stringResource(R.string.add_due_sale_entry)
        ModalType.OLD_DUE -> stringResource(R.string.add_due_collection)
        ModalType.STAFF -> stringResource(R.string.add_employer_advance)
        ModalType.WALKOUT -> stringResource(R.string.add_walkout_bill)
        ModalType.ITEM -> stringResource(R.string.add_paid_out_entry)
        ModalType.NONE -> ""
    }

    LojiaDialog(
        onDismissRequest = {
            focusManager.clearFocus()
            keyboardController?.hide()
            onDismiss()
        }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = title,
                color = OnSurfaceLight,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = Dimens.DialogPaddingComfortable,
                        top = Dimens.DialogPaddingComfortable,
                        end = Dimens.DialogPaddingComfortable,
                        bottom = Dimens.SpacingSm
                    )
            )
            HorizontalDivider(color = ListDividerGray, thickness = Dimens.RowDividerWidth)

            val depthStyle = LocalDepthStyle.current
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(Dimens.DialogPaddingComfortable)
            ) {
                when (type) {
                    ModalType.CREDIT -> {
                        LojiaTextField(
                            value = receipt,
                            onValueChange = { receipt = it },
                            label = { Text(stringResource(R.string.receipt_number)) },
                            shape = RoundedCornerShape(Dimens.RadiusReportInput),
                            minHeight = Dimens.TextFieldHeightReport,
                            borderColorOverride = depthStyle.reportFieldBorderColor,
                            topShadowAlphaOverride = depthStyle.reportFieldInsetAlpha,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(Dimens.ReportGridGap))
                        LojiaTextField(
                            value = amountText,
                            onValueChange = { amountText = it.replace('\u060C', ',').filter { c -> c.isDigit() || c == '.' || c == ',' || c == '\u066B' || c == '\u066C' }; error = null },
                            label = { Text(stringResource(R.string.amount_with_currency, currentCurrency)) },
                            isError = isAmountInvalid,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(Dimens.RadiusReportInput),
                            minHeight = Dimens.TextFieldHeightReport,
                            borderColorOverride = depthStyle.reportFieldBorderColor,
                            topShadowAlphaOverride = depthStyle.reportFieldInsetAlpha,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (isAmountInvalid) {
                            Spacer(Modifier.height(Dimens.SpacingXs))
                            Text(
                                text = invalidAmountError,
                                color = ErrorRedLight,
                                fontSize = DepthTypeTokens.ListRowSubtitleSize,
                                modifier = Modifier.padding(start = Dimens.SpacingXs)
                            )
                        }
                    }
                    ModalType.OLD_DUE -> {
                        LojiaTextField(
                            value = receipt,
                            onValueChange = { receipt = it },
                            label = { Text(stringResource(R.string.receipt_number)) },
                            shape = RoundedCornerShape(Dimens.RadiusReportInput),
                            minHeight = Dimens.TextFieldHeightReport,
                            borderColorOverride = depthStyle.reportFieldBorderColor,
                            topShadowAlphaOverride = depthStyle.reportFieldInsetAlpha,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(Dimens.ReportGridGap))
                        LojiaTextField(
                            value = amountText,
                            onValueChange = { amountText = it.replace('\u060C', ',').filter { c -> c.isDigit() || c == '.' || c == ',' || c == '\u066B' || c == '\u066C' }; error = null },
                            label = { Text(stringResource(R.string.amount_with_currency, currentCurrency)) },
                            isError = isAmountInvalid,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(Dimens.RadiusReportInput),
                            minHeight = Dimens.TextFieldHeightReport,
                            borderColorOverride = depthStyle.reportFieldBorderColor,
                            topShadowAlphaOverride = depthStyle.reportFieldInsetAlpha,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (isAmountInvalid) {
                            Spacer(Modifier.height(Dimens.SpacingXs))
                            Text(
                                text = invalidAmountError,
                                color = ErrorRedLight,
                                fontSize = DepthTypeTokens.ListRowSubtitleSize,
                                modifier = Modifier.padding(start = Dimens.SpacingXs)
                            )
                        }
                        Spacer(Modifier.height(Dimens.ReportGridGap))
                        PayTypeSelector(payType) { payType = it }
                    }
                    ModalType.STAFF -> {
                        LojiaTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text(stringResource(R.string.name_description)) },
                            shape = RoundedCornerShape(Dimens.RadiusReportInput),
                            minHeight = Dimens.TextFieldHeightReport,
                            borderColorOverride = depthStyle.reportFieldBorderColor,
                            topShadowAlphaOverride = depthStyle.reportFieldInsetAlpha,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(Dimens.ReportGridGap))
                        LojiaTextField(
                            value = amountText,
                            onValueChange = { amountText = it.replace('\u060C', ',').filter { c -> c.isDigit() || c == '.' || c == ',' || c == '\u066B' || c == '\u066C' }; error = null },
                            label = { Text(stringResource(R.string.amount_with_currency, currentCurrency)) },
                            isError = isAmountInvalid,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(Dimens.RadiusReportInput),
                            minHeight = Dimens.TextFieldHeightReport,
                            borderColorOverride = depthStyle.reportFieldBorderColor,
                            topShadowAlphaOverride = depthStyle.reportFieldInsetAlpha,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (isAmountInvalid) {
                            Spacer(Modifier.height(Dimens.SpacingXs))
                            Text(
                                text = invalidAmountError,
                                color = ErrorRedLight,
                                fontSize = DepthTypeTokens.ListRowSubtitleSize,
                                modifier = Modifier.padding(start = Dimens.SpacingXs)
                            )
                        }
                        Spacer(Modifier.height(Dimens.ReportGridGap))
                        PayTypeSelector(payType) { payType = it }
                    }
                    ModalType.WALKOUT -> {
                        LojiaTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text(stringResource(R.string.name_description)) },
                            shape = RoundedCornerShape(Dimens.RadiusReportInput),
                            minHeight = Dimens.TextFieldHeightReport,
                            borderColorOverride = depthStyle.reportFieldBorderColor,
                            topShadowAlphaOverride = depthStyle.reportFieldInsetAlpha,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(Dimens.ReportGridGap))
                        LojiaTextField(
                            value = amountText,
                            onValueChange = { amountText = it.replace('\u060C', ',').filter { c -> c.isDigit() || c == '.' || c == ',' || c == '\u066B' || c == '\u066C' }; error = null },
                            label = { Text(stringResource(R.string.amount_with_currency, currentCurrency)) },
                            isError = isAmountInvalid,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(Dimens.RadiusReportInput),
                            minHeight = Dimens.TextFieldHeightReport,
                            borderColorOverride = depthStyle.reportFieldBorderColor,
                            topShadowAlphaOverride = depthStyle.reportFieldInsetAlpha,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (isAmountInvalid) {
                            Spacer(Modifier.height(Dimens.SpacingXs))
                            Text(
                                text = invalidAmountError,
                                color = ErrorRedLight,
                                fontSize = DepthTypeTokens.ListRowSubtitleSize,
                                modifier = Modifier.padding(start = Dimens.SpacingXs)
                            )
                        }
                    }
                    ModalType.ITEM -> {
                        // 1. Item / Description * (Required, max 100 chars)
                        LojiaTextField(
                            value = name,
                            onValueChange = { if (it.length <= MAX_ITEM_DESCRIPTION_LENGTH) name = it },
                            label = {
                                Text(
                                    text = stringResource(R.string.item_description_required_fmt, stringResource(R.string.item_description)),
                                    fontSize = DepthTypeTokens.ReportFieldLabelSize,
                                    color = OnSurfaceVariantLight
                                )
                            },
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.add_entry_item_desc_hint),
                                    fontSize = DepthTypeTokens.RegisterPlaceholderSize,
                                    color = depthStyle.reportFieldPlaceholderColor
                                )
                            },
                            shape = RoundedCornerShape(Dimens.RadiusReportInput),
                            minHeight = Dimens.TextFieldHeightReport,
                            borderColorOverride = depthStyle.reportFieldBorderColor,
                            topShadowAlphaOverride = depthStyle.reportFieldInsetAlpha,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("paid_out_item_name_input")
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.add_entry_item_desc_hint),
                            fontSize = 11.sp,
                            color = TextHintColor,
                            modifier = Modifier.padding(start = 4.dp)
                        )

                        Spacer(Modifier.height(12.dp))

                        // 2. Total Amount (Currency) * (Required — source of truth)
                        LojiaTextField(
                            value = amountText,
                            onValueChange = { raw ->
                                val filtered = raw.replace('\u060C', ',').filter { c -> c.isDigit() || c == '.' || c == ',' || c == '\u066B' || c == '\u066C' }
                                amountText = filtered
                                error = null
                                // Priority: When Total is edited, Total is truth; if Quantity exists, auto-fill Unit Price = Total ÷ Quantity
                                val totalVal = MoneyFormat.parseMoneyInput(filtered, digits)?.let { MoneyFormat.toMajorUnits(it, digits) }
                                val qtyVal = qtyText.toDoubleOrNull()
                                if (totalVal != null && totalVal > 0.0 && qtyVal != null && qtyVal > 0.0) {
                                    unitPriceText = MoneyFormat.toPlainDecimal(MoneyFormat.toMinorUnits(totalVal / qtyVal, digits), digits)
                                }
                            },
                            label = {
                                Text(
                                    text = stringResource(R.string.add_entry_total_amount_req_fmt, currentCurrency),
                                    fontSize = DepthTypeTokens.ReportFieldLabelSize,
                                    color = OnSurfaceVariantLight
                                )
                            },
                            placeholder = {
                                Text(
                                    text = MoneyFormat.zeroPlaceholder(digits),
                                    fontSize = DepthTypeTokens.RegisterPlaceholderSize,
                                    color = depthStyle.reportFieldPlaceholderColor
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            isError = isAmountInvalid,
                            shape = RoundedCornerShape(Dimens.RadiusReportInput),
                            minHeight = Dimens.TextFieldHeightReport,
                            borderColorOverride = depthStyle.reportFieldBorderColor,
                            topShadowAlphaOverride = depthStyle.reportFieldInsetAlpha,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("paid_out_total_amount_input")
                        )
                        if (isAmountInvalid) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = invalidAmountError,
                                color = ErrorRedLight,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.add_entry_bill_amount_support),
                            fontSize = 11.sp,
                            color = TextHintColor,
                            modifier = Modifier.padding(start = 4.dp)
                        )

                        Spacer(Modifier.height(12.dp))
                        PayTypeSelector(payType) { payType = it }

                        Spacer(Modifier.height(14.dp))

                        // Optional details section divider & collapsible toggle
                        HorizontalDivider(color = OutlineVariantLight, thickness = 1.dp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = Dimens.MinTouchTarget)
                                .clickable { showOptionalDetails = !showOptionalDetails }
                                .padding(vertical = 10.dp)
                                .testTag("paid_out_optional_toggle"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.optional_details_label),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = PrimaryIndigoLight
                            )
                            Icon(
                                imageVector = if (showOptionalDetails) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (showOptionalDetails) stringResource(R.string.cd_collapse) else stringResource(R.string.cd_expand),
                                tint = PrimaryIndigoLight,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        HorizontalDivider(color = OutlineVariantLight, thickness = 1.dp)

                        if (showOptionalDetails) {
                            Spacer(Modifier.height(12.dp))

                            // 3. Quantity (Optional)
                            LojiaTextField(
                                value = qtyText,
                                onValueChange = { raw ->
                                    val filtered = raw.filter { c -> c.isDigit() }
                                    qtyText = filtered
                                    // Priority: When Quantity is entered AND Total exists -> Auto-fill Unit Price = Total ÷ Quantity
                                    val qtyVal = filtered.toDoubleOrNull()
                                    val totalVal = MoneyFormat.parseMoneyInput(amountText, digits)?.let { MoneyFormat.toMajorUnits(it, digits) }
                                    if (qtyVal != null && qtyVal > 0.0 && totalVal != null && totalVal > 0.0) {
                                        unitPriceText = MoneyFormat.toPlainDecimal(MoneyFormat.toMinorUnits(totalVal / qtyVal, digits), digits)
                                    } else if (filtered.isEmpty()) {
                                        unitPriceText = ""
                                    }
                                },
                                label = {
                                    Text(
                                        text = stringResource(R.string.add_entry_quantity_optional),
                                        fontSize = DepthTypeTokens.ReportFieldLabelSize,
                                        color = OnSurfaceVariantLight
                                    )
                                },
                                placeholder = {
                                    Text(
                                        text = stringResource(R.string.add_entry_quantity_hint),
                                        fontSize = DepthTypeTokens.RegisterPlaceholderSize,
                                        color = depthStyle.reportFieldPlaceholderColor
                                    )
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(Dimens.RadiusReportInput),
                                minHeight = Dimens.TextFieldHeightReport,
                                borderColorOverride = depthStyle.reportFieldBorderColor,
                                topShadowAlphaOverride = depthStyle.reportFieldInsetAlpha,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("paid_out_quantity_input")
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.add_entry_quantity_hint),
                                fontSize = 11.sp,
                                color = TextHintColor,
                                modifier = Modifier.padding(start = 4.dp)
                            )

                            Spacer(Modifier.height(12.dp))

                            // 4. Unit Price (Optional, auto-calculated)
                            LojiaTextField(
                                value = unitPriceText,
                                onValueChange = { raw ->
                                    val filtered = raw.replace('\u060C', ',').filter { c -> c.isDigit() || c == '.' || c == ',' || c == '\u066B' || c == '\u066C' }
                                    unitPriceText = filtered
                                    // Priority: When Unit Price is manually changed AND Quantity exists -> Auto-update Total = Unit Price × Quantity
                                    val unitVal = MoneyFormat.parseMoneyInput(filtered, digits)?.let { MoneyFormat.toMajorUnits(it, digits) }
                                    val qtyVal = qtyText.toDoubleOrNull()
                                    if (unitVal != null && unitVal > 0.0 && qtyVal != null && qtyVal > 0.0) {
                                        amountText = MoneyFormat.toPlainDecimal(MoneyFormat.toMinorUnits(unitVal * qtyVal, digits), digits)
                                    }
                                },
                                label = {
                                    Text(
                                        text = stringResource(R.string.add_entry_unit_price_optional),
                                        fontSize = DepthTypeTokens.ReportFieldLabelSize,
                                        color = OnSurfaceVariantLight
                                    )
                                },
                                placeholder = {
                                    Text(
                                        text = stringResource(R.string.add_entry_auto_filled_hint),
                                        fontSize = DepthTypeTokens.RegisterPlaceholderSize,
                                        color = depthStyle.reportFieldPlaceholderColor
                                    )
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                isError = isUnitPriceInvalid,
                                shape = RoundedCornerShape(Dimens.RadiusReportInput),
                                minHeight = Dimens.TextFieldHeightReport,
                                borderColorOverride = depthStyle.reportFieldBorderColor,
                                topShadowAlphaOverride = depthStyle.reportFieldInsetAlpha,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("paid_out_unit_price_input")
                            )
                            if (isUnitPriceInvalid) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = invalidAmountError,
                                    color = ErrorRedLight,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.add_entry_auto_filled_arrow),
                                fontSize = 11.sp,
                                color = TextHintColor,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                    ModalType.NONE -> {}
                }
                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = ErrorRedLight, fontSize = 12.sp)
                }
            }

            HorizontalDivider(color = OutlineVariantLight, thickness = 1.dp)

            val confirmEnabled = !isAmountInvalid && !isUnitPriceInvalid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.DialogPaddingComfortable),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        onDismiss()
                    },
                    shape = RoundedCornerShape(Dimens.RadiusReportInput),
                    border = null,
                    elevation = null,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = OnSurfaceVariantLight
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Dimens.ButtonHeightReportPrimary)
                        .depthSecondaryButton(
                            cornerRadius = Dimens.RadiusReportInput,
                            minHeight = Dimens.ButtonHeightReportPrimary
                        )
                ) {
                    Text(
                        text = stringResource(R.string.cancel),
                        color = OnSurfaceVariantLight,
                        fontSize = DepthTypeTokens.PrimaryCtaSmallSize,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Button(
                    enabled = confirmEnabled,
                    elevation = null,
                    shape = RoundedCornerShape(Dimens.RadiusReportInput),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        contentColor = PureWhite,
                        disabledContentColor = OnSurfaceVariantLight
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Dimens.ButtonHeightReportPrimary)
                        .depthRaisedButton(
                            enabled = confirmEnabled,
                            cornerRadius = Dimens.RadiusReportInput,
                            minHeight = Dimens.ButtonHeightReportPrimary
                        ),
                    onClick = {
                        val amount = when (amountClass) {
                            is MoneyInput.Valid -> MoneyFormat.toMajorUnits(amountClass.minor, digits)
                            MoneyInput.Empty, MoneyInput.Invalid -> {
                                error = invalidAmountError
                                return@Button
                            }
                        }
                        if (amount <= 0) {
                            error = invalidAmountError
                            return@Button
                        }
                        val parsedQty = qtyText.toIntOrNull()?.coerceAtLeast(0) ?: 0
                        val parsedUnitPrice = when (unitPriceClass) {
                            is MoneyInput.Valid -> MoneyFormat.toMajorUnits(unitPriceClass.minor, digits)
                            MoneyInput.Empty -> if (parsedQty > 0) (amount / parsedQty) else 0.0
                            MoneyInput.Invalid -> {
                                error = invalidAmountError
                                return@Button
                            }
                        }

                        focusManager.clearFocus()
                        keyboardController?.hide()
                        onSubmit(
                            ModalResult(
                                receipt = receipt.ifBlank { RECEIPT_UNSPECIFIED },
                                name = name.trim().ifBlank { RECEIPT_UNSPECIFIED },
                                amount = amount,
                                qty = parsedQty,
                                unitPrice = parsedUnitPrice,
                                type = payType
                            )
                        )
                    }
                ) {
                    Text(
                        text = stringResource(if (type == ModalType.ITEM) R.string.add_entry_btn else R.string.add),
                        color = if (confirmEnabled) PureWhite else OnSurfaceVariantLight,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
