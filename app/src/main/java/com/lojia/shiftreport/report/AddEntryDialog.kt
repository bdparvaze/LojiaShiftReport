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
import com.lojia.shiftreport.ui.common.LojiaTextField
import com.lojia.shiftreport.ui.theme.*
import com.lojia.shiftreport.util.MoneyFormat
import java.util.Locale

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
    val invalidAmountError = stringResource(R.string.valid_amount_error)

    val title = when (type) {
        ModalType.CREDIT -> stringResource(R.string.add_due_sale_entry)
        ModalType.OLD_DUE -> stringResource(R.string.add_due_collection)
        ModalType.STAFF -> stringResource(R.string.add_employer_advance)
        ModalType.WALKOUT -> stringResource(R.string.add_walkout_bill)
        ModalType.ITEM -> stringResource(R.string.add_paid_out_entry)
        ModalType.NONE -> ""
    }

    AlertDialog(
        onDismissRequest = {
            focusManager.clearFocus()
            keyboardController?.hide()
            onDismiss()
        },
        containerColor = SurfaceLight,
        titleContentColor = OnSurfaceLight,
        textContentColor = OnSurfaceLight,
        title = {
            Text(
                text = title,
                color = OnSurfaceLight,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()).imePadding()) {
                when (type) {
                    ModalType.CREDIT -> {
                        LojiaTextField(receipt, { receipt = it }, label = { Text(stringResource(R.string.receipt_number)) }, shape = RoundedCornerShape(10.dp), colors = fieldColors(), modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(10.dp))
                        LojiaTextField(amountText, { amountText = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text(stringResource(R.string.amount_with_currency, currentCurrency)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), shape = RoundedCornerShape(10.dp), colors = fieldColors(), modifier = Modifier.fillMaxWidth())
                    }
                    ModalType.OLD_DUE -> {
                        LojiaTextField(receipt, { receipt = it }, label = { Text(stringResource(R.string.receipt_number)) }, shape = RoundedCornerShape(10.dp), colors = fieldColors(), modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(10.dp))
                        LojiaTextField(amountText, { amountText = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text(stringResource(R.string.amount_with_currency, currentCurrency)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), shape = RoundedCornerShape(10.dp), colors = fieldColors(), modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(10.dp))
                        PayTypeSelector(payType) { payType = it }
                    }
                    ModalType.STAFF -> {
                        LojiaTextField(name, { name = it }, label = { Text(stringResource(R.string.name_description)) }, shape = RoundedCornerShape(10.dp), colors = fieldColors(), modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(10.dp))
                        LojiaTextField(amountText, { amountText = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text(stringResource(R.string.amount_with_currency, currentCurrency)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), shape = RoundedCornerShape(10.dp), colors = fieldColors(), modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(10.dp))
                        PayTypeSelector(payType) { payType = it }
                    }
                    ModalType.WALKOUT -> {
                        LojiaTextField(name, { name = it }, label = { Text(stringResource(R.string.name_description)) }, shape = RoundedCornerShape(10.dp), colors = fieldColors(), modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(10.dp))
                        LojiaTextField(amountText, { amountText = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text(stringResource(R.string.amount_with_currency, currentCurrency)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), shape = RoundedCornerShape(10.dp), colors = fieldColors(), modifier = Modifier.fillMaxWidth())
                    }
                    ModalType.ITEM -> {
                        // 1. Item / Description * (Required, max 100 chars)
                        LojiaTextField(
                            value = name,
                            onValueChange = { if (it.length <= 100) name = it },
                            label = {
                                Text(
                                    text = "${stringResource(R.string.item_description)} *",
                                    fontSize = 13.sp,
                                    color = OnSurfaceVariantLight
                                )
                            },
                            placeholder = {
                                Text(
                                    text = "e.g., Chicken, Bread, Cleaning supplies",
                                    fontSize = 13.sp,
                                    color = TextHintColor
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = fieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("paid_out_item_name_input")
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "e.g., Chicken, Bread, Cleaning supplies",
                            fontSize = 11.sp,
                            color = TextHintColor,
                            modifier = Modifier.padding(start = 4.dp)
                        )

                        Spacer(Modifier.height(12.dp))

                        // 2. Total Amount (Currency) * (Required — source of truth)
                        LojiaTextField(
                            value = amountText,
                            onValueChange = { raw ->
                                val filtered = raw.filter { c -> c.isDigit() || c == '.' }
                                amountText = filtered
                                error = null
                                // Priority: When Total is edited, Total is truth; if Quantity exists, auto-fill Unit Price = Total ÷ Quantity
                                val totalVal = filtered.toDoubleOrNull()
                                val qtyVal = qtyText.toDoubleOrNull()
                                if (totalVal != null && totalVal > 0.0 && qtyVal != null && qtyVal > 0.0) {
                                    unitPriceText = String.format(Locale.US, "%.2f", totalVal / qtyVal)
                                }
                            },
                            label = {
                                Text(
                                    text = "Total Amount ($currentCurrency) *",
                                    fontSize = 13.sp,
                                    color = OnSurfaceVariantLight
                                )
                            },
                            placeholder = {
                                Text(
                                    text = "0.00",
                                    fontSize = 13.sp,
                                    color = TextHintColor
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(10.dp),
                            colors = fieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("paid_out_total_amount_input")
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "The amount shown on your bill",
                            fontSize = 11.sp,
                            color = TextHintColor,
                            modifier = Modifier.padding(start = 4.dp)
                        )

                        Spacer(Modifier.height(14.dp))

                        // Optional details section divider & collapsible toggle
                        HorizontalDivider(color = OutlineVariantLight, thickness = 1.dp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
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
                                    val totalVal = amountText.toDoubleOrNull()
                                    if (qtyVal != null && qtyVal > 0.0 && totalVal != null && totalVal > 0.0) {
                                        unitPriceText = String.format(Locale.US, "%.2f", totalVal / qtyVal)
                                    } else if (filtered.isEmpty()) {
                                        unitPriceText = ""
                                    }
                                },
                                label = {
                                    Text(
                                        text = "Quantity (Optional)",
                                        fontSize = 13.sp,
                                        color = OnSurfaceVariantLight
                                    )
                                },
                                placeholder = {
                                    Text(
                                        text = "e.g., 1, 2, 3 pcs",
                                        fontSize = 13.sp,
                                        color = TextHintColor
                                    )
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(10.dp),
                                colors = fieldColors(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("paid_out_quantity_input")
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "e.g., 1, 2, 3 pcs",
                                fontSize = 11.sp,
                                color = TextHintColor,
                                modifier = Modifier.padding(start = 4.dp)
                            )

                            Spacer(Modifier.height(12.dp))

                            // 4. Unit Price (Optional, auto-calculated)
                            LojiaTextField(
                                value = unitPriceText,
                                onValueChange = { raw ->
                                    val filtered = raw.filter { c -> c.isDigit() || c == '.' }
                                    unitPriceText = filtered
                                    // Priority: When Unit Price is manually changed AND Quantity exists -> Auto-update Total = Unit Price × Quantity
                                    val unitVal = filtered.toDoubleOrNull()
                                    val qtyVal = qtyText.toDoubleOrNull()
                                    if (unitVal != null && unitVal > 0.0 && qtyVal != null && qtyVal > 0.0) {
                                        amountText = String.format(Locale.US, "%.2f", unitVal * qtyVal)
                                    }
                                },
                                label = {
                                    Text(
                                        text = "Unit Price (Optional, auto-calculated)",
                                        fontSize = 13.sp,
                                        color = OnSurfaceVariantLight
                                    )
                                },
                                placeholder = {
                                    Text(
                                        text = "Auto-filled from Total ÷ Quantity",
                                        fontSize = 13.sp,
                                        color = TextHintColor
                                    )
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                shape = RoundedCornerShape(10.dp),
                                colors = fieldColors(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("paid_out_unit_price_input")
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "→ Auto-filled from Total ÷ Quantity",
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
        },
        confirmButton = {
            Button(
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigoLight),
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (amount <= 0) {
                        error = invalidAmountError
                        return@Button
                    }
                    val parsedQty = qtyText.toIntOrNull()?.coerceAtLeast(0) ?: 0
                    val parsedUnitPrice = unitPriceText.toDoubleOrNull()
                        ?: if (parsedQty > 0) (amount / parsedQty) else 0.0

                    focusManager.clearFocus()
                    keyboardController?.hide()
                    onSubmit(
                        ModalResult(
                            receipt = receipt.ifBlank { "Unspecified" },
                            name = name.trim().ifBlank { "Unspecified" },
                            amount = amount,
                            qty = parsedQty,
                            unitPrice = parsedUnitPrice,
                            type = payType
                        )
                    )
                }
            ) {
                Text(
                    text = if (type == ModalType.ITEM) "Add Entry" else stringResource(R.string.add),
                    color = PureWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    onDismiss()
                }
            ) {
                Text(
                    text = stringResource(R.string.cancel),
                    color = OnSurfaceVariantLight,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    )
}
