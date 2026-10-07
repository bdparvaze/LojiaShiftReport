package com.lojia.shiftreport.report

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojia.shiftreport.R
import com.lojia.shiftreport.ui.common.LojiaTextField
import com.lojia.shiftreport.ui.theme.*
import com.lojia.shiftreport.util.MoneyFormat
import java.util.Locale

/* ----------------------------------------------------------------------
 * 4-CARD INTERNATIONAL POS RECONCILIATION SUMMARY (SQUARE/LOYVERSE STYLE)
 * ---------------------------------------------------------------------- */
@Composable
fun PosReconciliationSummary(
    startingCash: Double,
    cashSales: Double,
    madaSales: Double,
    digitalWalletSales: Double,
    totalDiscounts: Double = 0.0,
    salesReturns: Double = 0.0,
    totalExpenses: Double,
    totalDueCredit: Double,
    totalDueCollectedCash: Double,
    totalDueCollectedBank: Double,
    totalStaffAdvanceCash: Double,
    totalStaffAdvanceBank: Double,
    totalWalkout: Double,
    totalPurchasesCash: Double,
    staffMealsCount: Int,
    muasselCount: Int,
    outdoorMuasselCount: Int,
    actualCashCount: String,
    onActualCashCountChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    currentCurrency: String = MoneyFormat.DEFAULT_CURRENCY_CODE
) {
    val currency = currentCurrency
    val totalSales = MoneyFormat.add(cashSales, madaSales, digitalWalletSales)
    val netSales = MoneyFormat.subtract(MoneyFormat.subtract(totalSales, totalDiscounts), salesReturns).coerceAtLeast(0.0)
    val totalCashIn = MoneyFormat.add(startingCash, cashSales, totalDueCollectedCash)
    val totalCashOut = MoneyFormat.add(totalExpenses, totalStaffAdvanceCash, totalPurchasesCash)
    val expectedCash = MoneyFormat.subtract(totalCashIn, totalCashOut)
    val actualCountVal = actualCashCount.toDoubleOrNull()
    val variance = actualCountVal?.let { MoneyFormat.calculateVariance(it, expectedCash) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ==========================================
        // CARD 1: SALES SUMMARY (Payment Methods & Adjustments)
        // ==========================================
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, ShiftColors.Border),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(Modifier.fillMaxWidth().height(4.dp).background(ShiftColors.Brass))
            Column(Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🏷️", fontSize = 16.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            stringResource(R.string.sales_summary),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = ShiftColors.Charcoal
                        )
                    }
                    Surface(
                        color = ShiftColors.BrassLight,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "POS Revenue Breakdown",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ShiftColors.Brass,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                DashedDivider(Modifier.padding(vertical = 10.dp))

                ReconciliationRow(stringResource(R.string.cash_sales), cashSales, currency)
                ReconciliationRow(stringResource(R.string.mada_bank), madaSales, currency)
                if (digitalWalletSales > 0) {
                    ReconciliationRow(stringResource(R.string.digital_wallet), digitalWalletSales, currency)
                }

                DashedDivider(Modifier.padding(vertical = 8.dp), color = ShiftColors.Border)

                // TOTAL GROSS SALES (Prominent)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(WarningContainer)
                        .border(1.dp, WarningOrange.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.total_sales),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = WarningOrange
                    )
                    Text(
                        "%.2f %s".format(totalSales, currency),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = WarningOrange
                    )
                }

                if (totalDiscounts > 0 || salesReturns > 0) {
                    Spacer(Modifier.height(8.dp))
                    if (totalDiscounts > 0) {
                        ReconciliationRow(
                            label = "- ${stringResource(R.string.total_discounts_label)}",
                            value = totalDiscounts,
                            currency = currency,
                            valueColor = ShiftColors.Danger
                        )
                    }
                    if (salesReturns > 0) {
                        ReconciliationRow(
                            label = "- ${stringResource(R.string.sales_returns_label)}",
                            value = salesReturns,
                            currency = currency,
                            valueColor = ShiftColors.Danger
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SuccessContainer)
                            .border(1.dp, PosCashGreen.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.net_sales_label),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = PosCashGreen
                        )
                        Text(
                            "%.2f %s".format(netSales, currency),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.5.sp,
                            color = PosCashGreen
                        )
                    }
                }
            }
        }

        // ==========================================
        // CARD 2: CASH DRAWER RECONCILIATION
        // ==========================================
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, ShiftColors.Border),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(Modifier.fillMaxWidth().height(4.dp).background(ShiftColors.NetCashGreen))
            Column(Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("💵", fontSize = 16.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Cash Drawer Reconciliation",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = ShiftColors.Charcoal
                        )
                    }
                }

                DashedDivider(Modifier.padding(vertical = 10.dp))

                // Float / Inflow
                if (startingCash > 0) {
                    ReconciliationRow(stringResource(R.string.starting_cash), startingCash, currency)
                }
                ReconciliationRow("+ ${stringResource(R.string.cash_sales)}", cashSales, currency, valueColor = PosCashGreen)
                if (totalDueCollectedCash > 0) {
                    ReconciliationRow("+ ${stringResource(R.string.due_collected_cash)}", totalDueCollectedCash, currency, valueColor = PosCashGreen)
                }

                // Inflow subtotal
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.total_cash_in), fontSize = 11.5.sp, color = PosCashGreen, fontWeight = FontWeight.SemiBold)
                    Text("+ %.2f %s".format(totalCashIn, currency), fontSize = 12.sp, color = PosCashGreen, fontWeight = FontWeight.Bold)
                }

                DashedDivider(Modifier.padding(vertical = 6.dp))

                // Outflow
                if (totalExpenses > 0) {
                    ReconciliationRow("- ${stringResource(R.string.op_expenses)}", totalExpenses, currency, valueColor = ShiftColors.Danger)
                }
                if (totalStaffAdvanceCash > 0) {
                    ReconciliationRow("- ${stringResource(R.string.employer_cash)}", totalStaffAdvanceCash, currency, valueColor = ShiftColors.Danger)
                }
                if (totalPurchasesCash > 0) {
                    ReconciliationRow("- ${stringResource(R.string.paid_out)}", totalPurchasesCash, currency, valueColor = ShiftColors.Danger)
                }

                // Outflow subtotal
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.total_cash_out), fontSize = 11.5.sp, color = PosExpenseRed, fontWeight = FontWeight.SemiBold)
                    Text("- %.2f %s".format(totalCashOut, currency), fontSize = 12.sp, color = PosExpenseRed, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(8.dp))

                // EXPECTED CASH CALLOUT
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SuccessContainer)
                        .border(1.dp, PosCashGreen.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                stringResource(R.string.cash_in_drawer),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PosCashGreen
                            )
                            Text(
                                "Starting Float + Inflows - Outflows",
                                fontSize = 9.5.sp,
                                color = PosCashGreen
                            )
                        }
                        Text(
                            "%.2f %s".format(expectedCash, currency),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PosCashGreen
                        )
                    }
                }
            }
        }

        // ==========================================
        // CARD 3: ACTUAL COUNT & VARIANCE
        // ==========================================
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, ShiftColors.Border),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(Modifier.fillMaxWidth().height(4.dp).background(ShiftColors.Primary))
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⚖️", fontSize = 16.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.variance_over_short),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = ShiftColors.Charcoal
                    )
                }

                DashedDivider(Modifier.padding(vertical = 10.dp))

                // Actual Cash Input Field
                NumberField(
                    label = stringResource(R.string.actual_cash_count_currency, currency),
                    value = actualCashCount,
                    onChange = onActualCashCountChange,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(10.dp))

                // Variance Status Banner
                if (variance == null) {
                    Surface(
                        color = ShiftColors.Bg,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ShiftColors.Border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("ℹ️", fontSize = 14.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Enter physical cash in drawer to calculate variance",
                                fontSize = 11.5.sp,
                                color = ShiftColors.TextMuted
                            )
                        }
                    }
                } else {
                    val isBalanced = Math.abs(variance) < 0.001
                    val isOver = variance > 0
                    val bgColor = when {
                        isBalanced -> SuccessContainer
                        isOver -> SuccessContainer
                        else -> ErrorContainerLight
                    }
                    val borderColor = when {
                        isBalanced -> PosCashGreen.copy(alpha = 0.4f)
                        isOver -> PosCashGreen.copy(alpha = 0.4f)
                        else -> PosExpenseRed.copy(alpha = 0.4f)
                    }
                    val textColor = when {
                        isBalanced -> PosCashGreen
                        isOver -> PosCashGreen
                        else -> PosExpenseRed
                    }
                    val iconSymbol = when {
                        isBalanced -> "✓"
                        isOver -> "▲"
                        else -> "▼"
                    }
                    val statusText = when {
                        isBalanced -> stringResource(R.string.balanced)
                        isOver -> "Cash Over"
                        else -> "Cash Short"
                    }

                    Surface(
                        color = bgColor,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(iconSymbol, fontWeight = FontWeight.Bold, color = textColor, fontSize = 14.sp)
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        statusText,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = textColor
                                    )
                                }
                                Text(
                                    "${if (variance > 0) "+" else ""}${String.format(Locale.US, "%.2f", variance)} $currency",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = textColor
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Expected: %.2f %s • Actual: %.2f %s".format(
                                    expectedCash, currency, actualCountVal ?: 0.0, currency
                                ),
                                fontSize = 10.5.sp,
                                color = textColor.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // CARD 4: SECONDARY & OPERATIONAL TRACKING
        // ==========================================
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, ShiftColors.Border),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(Modifier.fillMaxWidth().height(4.dp).background(ShiftColors.Purple))
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📋", fontSize = 16.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Operational & Receivables Tracking",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = ShiftColors.Charcoal
                    )
                }

                DashedDivider(Modifier.padding(vertical = 10.dp))

                ReconciliationRow(
                    label = stringResource(R.string.due_sales_credit_entries),
                    value = totalDueCredit,
                    currency = currency,
                    note = "Tracked as Receivables • Excluded from sales & drawer"
                )

                if (staffMealsCount > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(stringResource(R.string.pdf_staff_meals), fontSize = 12.5.sp, color = ShiftColors.TextMuted, fontWeight = FontWeight.Medium)
                            Text(stringResource(R.string.complimentary_non_revenue_metric), fontSize = 9.5.sp, color = ShiftColors.TextMuted)
                        }
                        Text(stringResource(R.string.people_count, staffMealsCount), fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = ShiftColors.Text)
                    }
                }

                if (totalWalkout > 0) {
                    ReconciliationRow(
                        label = stringResource(R.string.walk_out),
                        value = totalWalkout,
                        currency = currency,
                        valueColor = ShiftColors.Danger
                    )
                }

                if (muasselCount > 0 || outdoorMuasselCount > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(R.string.muassel), fontSize = 12.5.sp, color = ShiftColors.TextMuted, fontWeight = FontWeight.Medium)
                        Text("${muasselCount + outdoorMuasselCount} pcs", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = ShiftColors.Purple)
                    }
                }

                // Optional Notes Input
                Spacer(Modifier.height(8.dp))
                FieldLabel(stringResource(R.string.closing_notes))
                Spacer(Modifier.height(4.dp))
                LojiaTextField(
                    value = notes,
                    onValueChange = onNotesChange,
                    placeholder = { Text(stringResource(R.string.closing_notes)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun ReconciliationRow(
    label: String,
    value: Double,
    currency: String,
    bold: Boolean = false,
    valueColor: Color = ShiftColors.Text,
    note: String? = null
) {
    if (value <= 0) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(label, fontSize = 12.5.sp, color = ShiftColors.TextMuted, fontWeight = FontWeight.Medium)
            if (!note.isNullOrBlank()) {
                Text(note, fontSize = 9.5.sp, color = ShiftColors.TextMuted)
            }
        }
        Text(
            "%.2f %s".format(value, currency),
            fontSize = 13.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold,
            color = valueColor
        )
    }
}

@Composable
fun DashedDivider(modifier: Modifier = Modifier, color: Color = ShiftColors.Border) {
    Canvas(modifier = modifier.fillMaxWidth().height(1.dp)) {
        drawLine(
            color = color,
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
        )
    }
}
