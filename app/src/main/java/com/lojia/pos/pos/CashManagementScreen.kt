package com.lojia.pos.pos

import com.lojia.pos.ui.common.LojiaTextField
import com.lojia.pos.R
import com.lojia.pos.data.*
import com.lojia.pos.util.*
import com.lojia.pos.ui.common.*
import com.lojia.pos.ui.theme.*
import com.lojia.pos.auth.*
import com.lojia.pos.pos.*
import com.lojia.pos.report.*
import com.lojia.pos.settings.*



import androidx.compose.ui.res.stringResource





import android.widget.Toast


import androidx.compose.foundation.background


import androidx.compose.foundation.layout.*


import androidx.compose.foundation.lazy.LazyColumn


import androidx.compose.foundation.lazy.items


import androidx.compose.foundation.shape.RoundedCornerShape


import androidx.compose.material.icons.Icons


import androidx.compose.material.icons.filled.*


import androidx.compose.material3.*


import androidx.compose.runtime.*


import androidx.compose.ui.Alignment


import androidx.compose.ui.Modifier


import androidx.compose.ui.graphics.Color


import androidx.compose.ui.platform.LocalContext


import androidx.compose.ui.text.font.FontWeight


import androidx.compose.ui.text.input.KeyboardType


import androidx.compose.ui.unit.dp


import androidx.compose.ui.unit.sp

import com.lojia.pos.data.AppLanguage

import com.lojia.pos.data.CashMovement


import java.text.SimpleDateFormat

import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashManagementScreen(
    viewModel: ReportViewModel,
    language: AppLanguage
) {
    val cashMovements by viewModel.cashMovements.collectAsState()
    val cashiers by viewModel.cashiers.collectAsState()
    val activeCashier by viewModel.activeCashier.collectAsState()
    val businessProfile by viewModel.businessProfile.collectAsState()
        val rawCurrency = businessProfile?.currency ?: "SAR"
    val currency = if (rawCurrency == "SAR") stringResource(R.string.currency_unit) else rawCurrency
    
    var type by remember { mutableStateOf("PAY_IN") }
    var amountInput by remember { mutableStateOf("") }
    var reasonInput by remember { mutableStateOf("") }
    var selectedCashier by remember(activeCashier) { mutableStateOf(activeCashier) }
    var cashierDropdownExpanded by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(2.dp))
            SolidCard {
                SectionTitle(
                    title = stringResource(R.string.record_cash_movement),
                    icon = Icons.Default.AttachMoney
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Type Selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = type == "PAY_IN",
                        onClick = { type = "PAY_IN" },
                        label = { Text(stringResource(R.string.cash_in), fontWeight = if (type == "PAY_IN") FontWeight.Bold else FontWeight.Normal) },
                        leadingIcon = if (type == "PAY_IN") {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentEmerald,
                            selectedLabelColor = PureWhite,
                            selectedLeadingIconColor = PureWhite
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = type == "PAY_OUT",
                        onClick = { type = "PAY_OUT" },
                        label = { Text(stringResource(R.string.cash_out), fontWeight = if (type == "PAY_OUT") FontWeight.Bold else FontWeight.Normal) },
                        leadingIcon = if (type == "PAY_OUT") {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentRose,
                            selectedLabelColor = PureWhite,
                            selectedLeadingIconColor = PureWhite
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))

                // Amount
                FormInputField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = stringResource(R.string.amount),
                    placeholder = "0.00",
                    leadingIcon = Icons.Default.AttachMoney,
                    suffixText = currency,
                    keyboardType = KeyboardType.Decimal
                )
                
                Spacer(modifier = Modifier.height(8.dp))

                // Reason/Notes
                FormInputField(
                    value = reasonInput,
                    onValueChange = { reasonInput = it },
                    label = stringResource(R.string.reason),
                    placeholder = stringResource(R.string.placeholder_cash_management_reason),
                    leadingIcon = Icons.Default.Notes,
                    keyboardType = KeyboardType.Text
                )

                Spacer(modifier = Modifier.height(8.dp))
                
                // Cashier Selection
                ExposedDropdownMenuBox(
                    expanded = cashierDropdownExpanded,
                    onExpandedChange = { cashierDropdownExpanded = !cashierDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LojiaTextField(
                        value = selectedCashier.ifBlank { stringResource(R.string.select_cashier) },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.cashier_6)) },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryIndigo) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cashierDropdownExpanded) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = cashierDropdownExpanded,
                        onDismissRequest = { cashierDropdownExpanded = false }
                    ) {
                        cashiers.forEach { cashier ->
                            DropdownMenuItem(
                                text = { Text(cashier.name) },
                                onClick = {
                                    selectedCashier = cashier.name
                                    viewModel.setActiveCashier(cashier.name)
                                    cashierDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val amount = amountInput.toDoubleOrNull()
                        if (amount != null && amount > 0 && selectedCashier.isNotBlank()) {
                            viewModel.addCashMovement(type, amount, reasonInput, selectedCashier)
                            amountInput = ""
                            reasonInput = ""
                            Toast.makeText(context, context.getString(R.string.movement_recorded_fmt, if (type == "PAY_IN") context.getString(R.string.cash_in) else context.getString(R.string.cash_out), amount, currency), Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, context.getString(R.string.enter_valid_amount), Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (type == "PAY_IN") AccentEmerald else AccentRose
                    )
                ) {
                    Text(
                        text = if (type == "PAY_IN") stringResource(R.string.record_cash_in) else stringResource(R.string.record_cash_out),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
        
        item {
            SectionTitle(
                title = stringResource(R.string.recent_movements),
                icon = Icons.Default.History
            )
        }
        
        items(cashMovements.sortedByDescending { it.timestamp }) { movement ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = BgLightGrey),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isPayIn = movement.type == "PAY_IN"
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = if (isPayIn) AccentEmerald.copy(alpha = 0.15f) else AccentRose.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPayIn) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = if (isPayIn) AccentEmerald else AccentRose
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = movement.reason.ifBlank { if (isPayIn) stringResource(R.string.cash_in) else stringResource(R.string.cash_out) },
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryLight,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "${movement.cashierName} • ${dateFormatter.format(Date(movement.timestamp))}",
                            color = TextSecondaryLight,
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        text = "${if (isPayIn) "+" else "-"}%.2f %s".format(movement.amount, currency),
                        fontWeight = FontWeight.Bold,
                        color = if (isPayIn) AccentEmerald else AccentRose,
                        fontSize = 16.sp
                    )
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
