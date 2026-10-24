package com.lojia.shiftreport.report

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojia.shiftreport.R
import com.lojia.shiftreport.data.CashMovement
import com.lojia.shiftreport.data.ShiftSession
import com.lojia.shiftreport.ui.common.LojiaMultilineTextField
import com.lojia.shiftreport.ui.theme.*
import com.lojia.shiftreport.util.MoneyFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Live Cashflow Audit & Drawer Management tab component.
 * Modularized out of ShiftReportScreen for clean architectural separation.
 */
@Composable
fun LiveCashflowTab(
    activeSession: ShiftSession?,
    cashMovements: List<CashMovement>,
    cashierOptions: List<String>,
    currentCurrency: String = MoneyFormat.DEFAULT_CURRENCY_CODE,
    onOpenShift: (String, String, Double) -> Unit,
    onCloseShift: (ShiftSession, Double, String) -> Unit
) {
    var showOpenShiftModal by remember { mutableStateOf(false) }
    var showCloseShiftModal by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Active Shift Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header row with status indicator & title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (activeSession != null) SuccessContainer else ShiftColors.DangerLight,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (activeSession != null) PosCashGreen.copy(alpha = 0.3f) else ErrorContainerLight
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            color = if (activeSession != null) ShiftColors.NetCashGreen else ShiftColors.Danger,
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                )
                                Text(
                                    text = if (activeSession != null) stringResource(R.string.drawer_open) else stringResource(R.string.drawer_closed),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (activeSession != null) ShiftColors.NetCashGreen else ShiftColors.Danger
                                )
                            }
                        }

                        Text(
                            text = stringResource(R.string.shift_drawer_status),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = ShiftColors.Charcoal
                        )
                    }
                }

                if (activeSession != null) {
                    // Information Container Box
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ShiftColors.Bg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, ShiftColors.Border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.cashier),
                                        fontSize = 11.sp,
                                        color = ShiftColors.TextMuted,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = activeSession.cashierName,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ShiftColors.Charcoal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.shift),
                                        fontSize = 11.sp,
                                        color = ShiftColors.TextMuted,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = activeSession.shiftName,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ShiftColors.Charcoal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Column(modifier = Modifier.weight(1.2f), horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = stringResource(R.string.starting_cash_float),
                                        fontSize = 11.sp,
                                        color = ShiftColors.TextMuted,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = "%.2f $currentCurrency".format(activeSession.startingCash),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ShiftColors.NetCashGreen,
                                        maxLines = 1
                                    )
                                }
                            }

                            HorizontalDivider(color = ShiftColors.Border.copy(alpha = 0.6f))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = ShiftColors.TextMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "${stringResource(R.string.opened)}: ${dateFormat.format(Date(activeSession.openedAt))}",
                                    fontSize = 11.sp,
                                    color = ShiftColors.TextMuted,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }
                    }

                    // Prominent, beautiful Close Shift Action Button
                    Button(
                        onClick = { showCloseShiftModal = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ShiftColors.Danger,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 46.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.close_shift_session),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                } else {
                    Text(
                        text = stringResource(R.string.no_active_shift_msg),
                        fontSize = 13.sp,
                        color = ShiftColors.TextMuted,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Button(
                        onClick = { showOpenShiftModal = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ShiftColors.Primary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 46.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.open_shift_session),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Live Cash Audit & Summary Grid
        Text(
            text = stringResource(R.string.live_cashflow_analytics),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = ShiftColors.Charcoal
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            MetricCard(
                title = stringResource(R.string.total_cash_in),
                amount = "0.00 $currentCurrency",
                icon = Icons.Default.TrendingUp,
                color = ShiftColors.NetCashGreen,
                bgColor = SuccessContainer,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = stringResource(R.string.total_cash_out),
                amount = "0.00 $currentCurrency",
                icon = Icons.Default.TrendingDown,
                color = ShiftColors.Danger,
                bgColor = ShiftColors.DangerLight,
                modifier = Modifier.weight(1f)
            )
        }

        // Drawer Cash Movement Log
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.recent_drawer_movements),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = ShiftColors.Charcoal
                )
                Spacer(Modifier.height(10.dp))

                if (cashMovements.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_cash_movements),
                        fontSize = 12.sp,
                        color = ShiftColors.TextMuted,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    cashMovements.take(5).forEach { move ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (move.type == "PAY_IN") SuccessContainer else ShiftColors.DangerLight,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Icon(
                                            imageVector = if (move.type == "PAY_IN") Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                            contentDescription = null,
                                            tint = if (move.type == "PAY_IN") ShiftColors.NetCashGreen else ShiftColors.Danger,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = move.reason,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ShiftColors.Text,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${move.cashierName} • ${dateFormat.format(Date(move.timestamp))}",
                                        fontSize = 11.sp,
                                        color = ShiftColors.TextMuted
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (move.type == "PAY_IN") SuccessContainer else ShiftColors.DangerLight
                            ) {
                                Text(
                                    text = "${if (move.type == "PAY_IN") "+" else "-"}%.2f $currentCurrency".format(move.amount),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (move.type == "PAY_IN") ShiftColors.NetCashGreen else ShiftColors.Danger,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    maxLines = 1
                                )
                            }
                        }
                        HorizontalDivider(color = ShiftColors.Border.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }

    // Modal: Open Shift
    if (showOpenShiftModal) {
        var selectedCashier by remember { mutableStateOf(cashierOptions.firstOrNull() ?: "Noora") }
        var selectedShift by remember { mutableStateOf("Day") }
        var startingCashText by remember { mutableStateOf("100.00") }

        AlertDialog(
            onDismissRequest = { showOpenShiftModal = false },
            containerColor = Color.White,
            titleContentColor = OnBackgroundLight,
            textContentColor = OnSurfaceVariantLight,
            shape = RoundedCornerShape(18.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = ShiftColors.Primary, modifier = Modifier.size(22.dp))
                    Text(
                        text = stringResource(R.string.open_shift_session),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = OnBackgroundLight
                    )
                }
            },
            text = {
                val dayShiftText = stringResource(R.string.shift_day)
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState()).imePadding()
                ) {
                    LabeledDropdown(
                        label = stringResource(R.string.cashier),
                        options = cashierOptions,
                        selected = selectedCashier,
                        onSelected = { selectedCashier = it }
                    )
                    LabeledDropdown(
                        label = stringResource(R.string.shift),
                        options = listOf(dayShiftText, stringResource(R.string.shift_night_option)),
                        selected = if (selectedShift == "Day") dayShiftText else stringResource(R.string.shift_night_option),
                        onSelected = { selectedShift = if (it == dayShiftText) "Day" else "Night" }
                    )
                    NumberField(
                        label = stringResource(R.string.starting_cash_float),
                        value = startingCashText,
                        onChange = { startingCashText = it }
                    )
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = ShiftColors.Primary),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    onClick = {
                        val cash = startingCashText.toDoubleOrNull() ?: 0.0
                        onOpenShift(selectedCashier, selectedShift, cash)
                        showOpenShiftModal = false
                    }
                ) {
                    Text(
                        text = stringResource(R.string.start_shift),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    onClick = { showOpenShiftModal = false }
                ) {
                    Text(
                        text = stringResource(R.string.cancel),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        )
    }

    // Modal: Close Shift & Generate Z-Report
    if (showCloseShiftModal && activeSession != null) {
        var actualCountText by remember { mutableStateOf("") }
        var notesText by remember { mutableStateOf("") }
        val currency = currentCurrency
        val actualCount = actualCountText.toDoubleOrNull()
        val variance = if (actualCount != null) actualCount - activeSession.expectedCash else null

        AlertDialog(
            onDismissRequest = { showCloseShiftModal = false },
            containerColor = Color.White,
            titleContentColor = OnBackgroundLight,
            textContentColor = OnSurfaceVariantLight,
            shape = RoundedCornerShape(18.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = ShiftColors.Danger, modifier = Modifier.size(22.dp))
                    Text(
                        text = stringResource(R.string.close_shift_session),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = OnBackgroundLight
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState()).imePadding()
                ) {
                    // 1. Sales Summary Card
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = BackgroundLight,
                        border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariantLight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = stringResource(R.string.sales_summary),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ShiftColors.Primary
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(stringResource(R.string.cash_sales), fontSize = 12.sp, color = ShiftColors.TextMuted)
                                Text("%.2f %s".format(activeSession.cashSales, currency), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(stringResource(R.string.mada_bank), fontSize = 12.sp, color = ShiftColors.TextMuted)
                                Text("%.2f %s".format(activeSession.cardSales, currency), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            if (activeSession.digitalSales > 0) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(stringResource(R.string.digital_wallet), fontSize = 12.sp, color = ShiftColors.TextMuted)
                                    Text("%.2f %s".format(activeSession.digitalSales, currency), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            HorizontalDivider(color = OutlineVariantLight, modifier = Modifier.padding(vertical = 2.dp))
                            val totalSales = activeSession.cashSales + activeSession.cardSales + activeSession.digitalSales
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(stringResource(R.string.total_sales), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("%.2f %s".format(totalSales, currency), fontSize = 12.5.sp, fontWeight = FontWeight.ExtraBold, color = ShiftColors.Primary)
                            }
                        }
                    }

                    // 2. Expected Cash Drawer Movement
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = ShiftColors.BrassLight,
                        border = androidx.compose.foundation.BorderStroke(1.dp, ShiftColors.Brass.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = stringResource(R.string.cash_in_drawer),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ShiftColors.Brass
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(stringResource(R.string.starting_cash), fontSize = 12.sp, color = ShiftColors.TextMuted)
                                Text("%.2f %s".format(activeSession.startingCash, currency), fontSize = 12.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("(+) ${stringResource(R.string.cash_sales)}", fontSize = 12.sp, color = ShiftColors.TextMuted)
                                Text("+%.2f %s".format(activeSession.cashSales, currency), fontSize = 12.sp)
                            }
                            if (activeSession.totalPayIn > 0) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("(+) ${stringResource(R.string.pay_in)}", fontSize = 12.sp, color = ShiftColors.TextMuted)
                                    Text("+%.2f %s".format(activeSession.totalPayIn, currency), fontSize = 12.sp)
                                }
                            }
                            if (activeSession.totalPayOut > 0) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("(-) ${stringResource(R.string.pay_out)}", fontSize = 12.sp, color = ShiftColors.TextMuted)
                                    Text("-%.2f %s".format(activeSession.totalPayOut, currency), fontSize = 12.sp)
                                }
                            }
                            HorizontalDivider(color = ShiftColors.Brass.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 2.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(stringResource(R.string.expected_cash_drawer, ""), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("%.2f %s".format(activeSession.expectedCash, currency), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = ShiftColors.NetCashGreen)
                            }
                        }
                    }

                    // 3. Actual Count Input
                    NumberField(
                        label = stringResource(R.string.actual_cash_counted),
                        value = actualCountText,
                        onChange = { actualCountText = it }
                    )

                    // 4. Live Variance Card
                    if (variance != null) {
                        val isBalanced = Math.abs(variance) < 0.001
                        val isOver = variance > 0
                        val varColor = when {
                            isBalanced -> PosCashGreen
                            isOver -> PosCashGreen
                            else -> PosShortageRed
                        }
                        val varBg = when {
                            isBalanced -> SuccessContainer
                            isOver -> SuccessContainer
                            else -> ErrorContainerLight
                        }
                        val statusLabel = when {
                            isBalanced -> stringResource(R.string.balanced)
                            isOver -> "OVER (+%.2f %s)".format(variance, currency)
                            else -> "SHORT (-%.2f %s)".format(Math.abs(variance), currency)
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = varBg,
                            border = androidx.compose.foundation.BorderStroke(1.dp, varColor.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.cash_variance),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = varColor
                                )
                                Text(
                                    text = statusLabel,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = varColor
                                )
                            }
                        }
                    }

                    // 5. Notes
                    LojiaMultilineTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        label = { Text(stringResource(R.string.shift_notes_variance)) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = ShiftColors.Danger),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    onClick = {
                        val count = actualCountText.toDoubleOrNull() ?: 0.0
                        onCloseShift(activeSession, count, notesText)
                        showCloseShiftModal = false
                    }
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.close_shift_session),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    onClick = { showCloseShiftModal = false }
                ) {
                    Text(
                        text = stringResource(R.string.cancel),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        )
    }
}
