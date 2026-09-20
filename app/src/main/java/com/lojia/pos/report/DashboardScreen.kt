package com.lojia.pos.report

import com.lojia.pos.R
import com.lojia.pos.data.*
import com.lojia.pos.util.*
import com.lojia.pos.ui.common.*
import com.lojia.pos.ui.theme.*
import com.lojia.pos.auth.*
import com.lojia.pos.pos.*
import com.lojia.pos.report.*
import com.lojia.pos.settings.*



import androidx.compose.foundation.background


import androidx.compose.foundation.border


import androidx.compose.foundation.clickable


import androidx.compose.foundation.layout.*


import androidx.compose.foundation.lazy.LazyColumn


import androidx.compose.foundation.shape.RoundedCornerShape


import androidx.compose.material.icons.Icons


import androidx.compose.material.icons.filled.*


import androidx.compose.material3.*


import androidx.compose.runtime.*

import android.widget.Toast


import androidx.compose.ui.Alignment


import androidx.compose.ui.Modifier


import androidx.compose.ui.draw.clip


import androidx.compose.ui.graphics.Color


import androidx.compose.ui.platform.LocalContext


import androidx.compose.ui.platform.testTag


import androidx.compose.ui.text.font.FontWeight


import androidx.compose.ui.unit.dp


import androidx.compose.ui.unit.sp

import com.lojia.pos.data.AppLanguage


import com.lojia.pos.util.PdfReportGenerator


import androidx.compose.ui.res.stringResource


@Composable
fun DashboardScreen(
    reportViewModel: ReportViewModel,
    posViewModel: PosViewModel,
    language: AppLanguage
) {
    val shiftReports by reportViewModel.shiftReports.collectAsState()
    val salesHistory by posViewModel.salesHistory.collectAsState()
    val businessProfile by reportViewModel.businessProfile.collectAsState()

    var dashboardViewMode by remember { mutableIntStateOf(0) } // 0: Monthly Trends (Recharts), 1: Shift & Payment Breakdown

        val rawCurrency = businessProfile?.currency ?: "SAR"
    val currency = if (rawCurrency == "SAR") stringResource(R.string.currency_unit) else rawCurrency

    val totalShiftSales = shiftReports.sumOf { it.totalSales }
    val totalPosSales = salesHistory.sumOf { it.totalAmount }
    val combinedGrossSales = totalShiftSales + totalPosSales

    val totalCash = shiftReports.sumOf { it.grossCash } + salesHistory.filter { it.paymentMethod == "Cash" }.sumOf { it.totalAmount }
    val totalMada = shiftReports.sumOf { it.madaPayments } + salesHistory.filter { it.paymentMethod == "Mada" }.sumOf { it.totalAmount }
    val totalWallet = shiftReports.sumOf { it.digitalWallet } + salesHistory.filter { it.paymentMethod == "Digital Wallet" }.sumOf { it.totalAmount }

    val totalExpenses = shiftReports.sumOf { it.totalExpenses }
    val avgSalesPerShift = if (shiftReports.isNotEmpty()) totalShiftSales / shiftReports.size else 0.0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            val context = LocalContext.current
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionTitle(
                    title = stringResource(R.string.analytics_overview),
                    icon = Icons.Default.Analytics
                )

                if (shiftReports.isNotEmpty()) {
                    FilledTonalButton(
                        onClick = {
                            val result = PdfReportGenerator.generateShiftSummaryPdf(
                                context = context,
                                reports = shiftReports,
                                businessProfile = businessProfile,
                                reportTitle = context.getString(R.string.monthly_sales_shift_summary_report)
                            )
                            if (result.isSuccess) {
                                Toast.makeText(context, context.getString(R.string.exported_pdf_to, result.displayPath), Toast.LENGTH_SHORT).show()
                                result.uri?.let { uri ->
                                    PdfReportGenerator.openPdfFile(context, uri)
                                }
                            } else {
                                Toast.makeText(context, context.getString(R.string.export_failed_msg, result.errorMessage), Toast.LENGTH_LONG).show()
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = PrimaryBlue.copy(alpha = 0.12f),
                            contentColor = PrimaryBlue
                        ),
                        modifier = Modifier.wrapContentWidth().testTag("export_analytics_pdf_btn")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.export_pdf),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        // View Segmented Switcher (Monthly Sales Trends vs Shift & Payment Overview)
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = BgLightGrey,
                border = androidx.compose.foundation.BorderStroke(1.dp, OutlineLight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        color = if (dashboardViewMode == 0) PureWhite else Color.Transparent,
                        border = if (dashboardViewMode == 0) androidx.compose.foundation.BorderStroke(1.dp, OutlineLight) else null,
                        shadowElevation = if (dashboardViewMode == 0) 1.dp else 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .clickable { dashboardViewMode = 0 }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timeline,
                                contentDescription = null,
                                tint = if (dashboardViewMode == 0) PrimaryBlue else TextSecondaryLight,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.monthly_trends),
                                fontSize = 12.5.sp,
                                fontWeight = if (dashboardViewMode == 0) FontWeight.Bold else FontWeight.Medium,
                                color = if (dashboardViewMode == 0) PrimaryBlue else TextSecondaryLight
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        color = if (dashboardViewMode == 1) PureWhite else Color.Transparent,
                        border = if (dashboardViewMode == 1) androidx.compose.foundation.BorderStroke(1.dp, OutlineLight) else null,
                        shadowElevation = if (dashboardViewMode == 1) 1.dp else 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .clickable { dashboardViewMode = 1 }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PieChart,
                                contentDescription = null,
                                tint = if (dashboardViewMode == 1) PrimaryBlue else TextSecondaryLight,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.payment_breakdown),
                                fontSize = 12.5.sp,
                                fontWeight = if (dashboardViewMode == 1) FontWeight.Bold else FontWeight.Medium,
                                color = if (dashboardViewMode == 1) PrimaryBlue else TextSecondaryLight
                            )
                        }
                    }
                }
            }
        }

        if (dashboardViewMode == 0) {
            // Recharts-Styled Monthly Sales Summary View
            item {
                MonthlySalesSummaryView(
                    shiftReports = shiftReports,
                    salesHistory = salesHistory,
                    currency = currency,
                    language = language
                )
            }
        } else {
            // Summary Metric Stat Cards Row 1
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricStatCard(
                        title = stringResource(R.string.total_revenue),
                        value = "%.2f %s".format(combinedGrossSales, currency),
                        subtitle = stringResource(R.string.shifts_recorded_fmt, shiftReports.size),
                        icon = Icons.Default.MonetizationOn,
                        iconColor = AccentEmerald,
                        iconBgColor = PureWhite,
                        modifier = Modifier.weight(1f)
                    )
                    MetricStatCard(
                        title = stringResource(R.string.average_shift_sales),
                        value = "%.2f %s".format(avgSalesPerShift, currency),
                        subtitle = stringResource(R.string.per_shift_average),
                        icon = Icons.Default.ShowChart,
                        iconColor = PrimaryIndigo,
                        iconBgColor = PureWhite,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Summary Metric Stat Cards Row 2
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricStatCard(
                        title = stringResource(R.string.total_expenses),
                        value = "%.2f %s".format(totalExpenses, currency),
                        subtitle = stringResource(R.string.operational_costs),
                        icon = Icons.Default.MoneyOff,
                        iconColor = AccentRose,
                        iconBgColor = PureWhite,
                        modifier = Modifier.weight(1f)
                    )
                    MetricStatCard(
                        title = stringResource(R.string.total_reports_count),
                        value = "${shiftReports.size}",
                        subtitle = stringResource(R.string.completed_shifts),
                        icon = Icons.Default.Assessment,
                        iconColor = AccentGold,
                        iconBgColor = PureWhite,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Payment Method Breakdown Progress Bars
            item {
                SolidCard {
                    SectionTitle(
                        title = stringResource(R.string.payment_distribution),
                        icon = Icons.Default.PieChart
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val sumPayments = (totalCash + totalMada + totalWallet).coerceAtLeast(1.0)
                    val cashPct = (totalCash / sumPayments * 100).toInt()
                    val madaPct = (totalMada / sumPayments * 100).toInt()
                    val walletPct = (totalWallet / sumPayments * 100).toInt()

                    // Cash Bar
                    PaymentProgressBar(
                        label = stringResource(R.string.gross_cash),
                        amount = totalCash,
                        percent = cashPct,
                        currency = currency,
                        color = AccentEmerald
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Mada Bar
                    PaymentProgressBar(
                        label = stringResource(R.string.mada_payments),
                        amount = totalMada,
                        percent = madaPct,
                        currency = currency,
                        color = PrimaryIndigo
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Digital Wallet Bar
                    PaymentProgressBar(
                        label = stringResource(R.string.digital_wallet),
                        amount = totalWallet,
                        percent = walletPct,
                        currency = currency,
                        color = AccentGold
                    )
                }
            }

            // Shift Comparison Card (Morning vs Evening)
            item {
                SolidCard {
                    SectionTitle(
                        title = stringResource(R.string.shift_comparison),
                        icon = Icons.Default.CompareArrows
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val morningShifts = shiftReports.filter { it.shift.equals("Morning", ignoreCase = true) }
                    val eveningShifts = shiftReports.filter { it.shift.equals("Evening", ignoreCase = true) }

                    val morningTotal = morningShifts.sumOf { it.totalSales }
                    val eveningTotal = eveningShifts.sumOf { it.totalSales }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(stringResource(R.string.shift_morning), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.shifts_count, morningShifts.size), style = MaterialTheme.typography.labelSmall, color = TextSecondaryLight)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(stringResource(R.string.msg_2f_s_21).format(morningTotal, currency), fontWeight = FontWeight.Bold, color = PrimaryIndigo)
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(stringResource(R.string.shift_evening), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.shifts_count, eveningShifts.size), style = MaterialTheme.typography.labelSmall, color = TextSecondaryLight)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(stringResource(R.string.msg_2f_s_21).format(eveningTotal, currency), fontWeight = FontWeight.Bold, color = AccentGold)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun PaymentProgressBar(
    label: String,
    amount: Double,
    percent: Int,
    currency: String,
    color: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(stringResource(R.string.msg_2f_s_d_1).format(amount, currency, percent), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { (percent / 100f).coerceIn(0f, 1f) },
            color = color,
            trackColor = PureWhite,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .border(1.dp, OutlineLight, RoundedCornerShape(4.dp))
        )
    }
}
