package com.lojia.shiftreport.report

import com.lojia.shiftreport.R
import com.lojia.shiftreport.data.*
import com.lojia.shiftreport.util.*
import com.lojia.shiftreport.ui.common.*
import com.lojia.shiftreport.ui.theme.*
import com.lojia.shiftreport.auth.*
import com.lojia.shiftreport.report.*
import com.lojia.shiftreport.settings.*



import androidx.compose.foundation.background


import androidx.compose.foundation.border


import androidx.compose.foundation.clickable


import androidx.compose.foundation.layout.*


import androidx.compose.foundation.lazy.LazyColumn


import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape


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

import com.lojia.shiftreport.data.AppLanguage


import com.lojia.shiftreport.util.PdfReportGenerator


import androidx.compose.ui.res.stringResource


@Composable
fun DashboardScreen(
    reportViewModel: ReportViewModel,
    language: AppLanguage
) {
    val shiftReports by reportViewModel.shiftReports.collectAsState()
    val businessProfile by reportViewModel.businessProfile.collectAsState()

    var dashboardViewMode by remember { mutableIntStateOf(0) } // 0: Monthly Trends (Recharts), 1: Shift & Payment Breakdown

    val currency = businessProfile?.currency ?: MoneyFormat.DEFAULT_CURRENCY_CODE

    val totalShiftSales = shiftReports.sumOf { it.totalSales }
    val combinedGrossSales = totalShiftSales

    val totalCash = shiftReports.sumOf { it.grossCash }
    val totalMada = shiftReports.sumOf { it.madaPayments }
    val totalWallet = shiftReports.sumOf { it.digitalWallet }

    val totalExpenses = shiftReports.sumOf { it.totalExpenses }
    val avgSalesPerShift = if (shiftReports.isNotEmpty()) totalShiftSales / shiftReports.size else 0L

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                val context = LocalContext.current
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(PrimaryBlue.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Analytics,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.analytics_overview),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = OnBackgroundLight
                                )
                            )
                            Text(
                                text = stringResource(R.string.dashboard_analytics_subtitle),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    color = TextSecondaryLight
                                )
                            )
                        }
                    }

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
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = PureWhite,
                                contentColor = PrimaryBlue
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, OutlineLight),
                            modifier = Modifier.wrapContentWidth().testTag("export_analytics_pdf_btn")
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(5.dp))
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
                    shape = RoundedCornerShape(14.dp),
                    color = SurfaceVariantLight,
                    border = androidx.compose.foundation.BorderStroke(1.dp, OutlineLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (dashboardViewMode == 0) PureWhite else Color.Transparent,
                            border = if (dashboardViewMode == 0) androidx.compose.foundation.BorderStroke(1.dp, OutlineLight) else null,
                            shadowElevation = if (dashboardViewMode == 0) 1.dp else 0.dp,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { dashboardViewMode = 0 }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 9.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timeline,
                                    contentDescription = null,
                                    tint = if (dashboardViewMode == 0) PrimaryBlue else TextSecondaryLight,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(7.dp))
                                Text(
                                    text = stringResource(R.string.monthly_trends),
                                    fontSize = 13.sp,
                                    fontWeight = if (dashboardViewMode == 0) FontWeight.Bold else FontWeight.Medium,
                                    color = if (dashboardViewMode == 0) PrimaryBlue else TextSecondaryLight
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (dashboardViewMode == 1) PureWhite else Color.Transparent,
                            border = if (dashboardViewMode == 1) androidx.compose.foundation.BorderStroke(1.dp, OutlineLight) else null,
                            shadowElevation = if (dashboardViewMode == 1) 1.dp else 0.dp,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { dashboardViewMode = 1 }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 9.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PieChart,
                                    contentDescription = null,
                                    tint = if (dashboardViewMode == 1) PrimaryBlue else TextSecondaryLight,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(7.dp))
                                Text(
                                    text = stringResource(R.string.payment_breakdown),
                                    fontSize = 13.sp,
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
                        currency = currency,
                        language = language
                    )
                }
            } else {
                // Summary Metric Stat Cards Row 1
                item {
                    val dashDigits = MoneyFormat.minorUnitDigits(currency)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MonthlyKpiCard(
                            title = stringResource(R.string.total_revenue),
                            value = "${MoneyFormat.toPlainDecimal(combinedGrossSales, dashDigits)} $currency",
                            subtitle = stringResource(R.string.shifts_recorded_fmt, shiftReports.size),
                            icon = Icons.Default.MonetizationOn,
                            accentColor = Color(0xFF059669),
                            modifier = Modifier.weight(1f)
                        )
                        MonthlyKpiCard(
                            title = stringResource(R.string.average_shift_sales),
                            value = "${MoneyFormat.toPlainDecimal(avgSalesPerShift, dashDigits)} $currency",
                            subtitle = stringResource(R.string.per_shift_average),
                            icon = Icons.Default.ShowChart,
                            accentColor = Color(0xFF2563EB),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Summary Metric Stat Cards Row 2
                item {
                    val dashDigits = MoneyFormat.minorUnitDigits(currency)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MonthlyKpiCard(
                            title = stringResource(R.string.total_expenses),
                            value = "${MoneyFormat.toPlainDecimal(totalExpenses, dashDigits)} $currency",
                            subtitle = stringResource(R.string.operational_costs),
                            icon = Icons.Default.MoneyOff,
                            accentColor = Color(0xFFDC2626),
                            modifier = Modifier.weight(1f)
                        )
                        MonthlyKpiCard(
                            title = stringResource(R.string.total_reports_count),
                            value = "${shiftReports.size}",
                            subtitle = stringResource(R.string.completed_shifts),
                            icon = Icons.Default.Assessment,
                            accentColor = Color(0xFFD97706),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Payment Method Breakdown Progress Bars
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = PureWhite,
                        border = androidx.compose.foundation.BorderStroke(1.dp, OutlineLight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryBlue.copy(alpha = 0.10f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PieChart,
                                        contentDescription = null,
                                        tint = PrimaryBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = stringResource(R.string.payment_distribution),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.5.sp,
                                        color = OnBackgroundLight
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))

                            val sumPayments = (totalCash + totalMada + totalWallet).coerceAtLeast(1L)
                            val cashPct = ((totalCash.toDouble() / sumPayments.toDouble()) * 100).toInt()
                            val madaPct = ((totalMada.toDouble() / sumPayments.toDouble()) * 100).toInt()
                            val walletPct = ((totalWallet.toDouble() / sumPayments.toDouble()) * 100).toInt()

                            // Cash Bar
                            PaymentProgressBar(
                                label = stringResource(R.string.gross_cash),
                                amount = MoneyFormat.toMajorUnits(totalCash),
                                percent = cashPct,
                                currency = currency,
                                color = Color(0xFF059669)
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            // Mada Bar
                            PaymentProgressBar(
                                label = stringResource(R.string.mada_payments),
                                amount = MoneyFormat.toMajorUnits(totalMada),
                                percent = madaPct,
                                currency = currency,
                                color = Color(0xFF2563EB)
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            // Digital Wallet Bar
                            PaymentProgressBar(
                                label = stringResource(R.string.digital_wallet),
                                amount = MoneyFormat.toMajorUnits(totalWallet),
                                percent = walletPct,
                                currency = currency,
                                color = Color(0xFFD97706)
                            )
                        }
                    }
                }

                // Shift Comparison Card (Morning vs Evening)
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = PureWhite,
                        border = androidx.compose.foundation.BorderStroke(1.dp, OutlineLight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryBlue.copy(alpha = 0.10f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CompareArrows,
                                        contentDescription = null,
                                        tint = PrimaryBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = stringResource(R.string.shift_comparison),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.5.sp,
                                        color = OnBackgroundLight
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))

                            val morningShifts = shiftReports.filter { it.shift.equals("Morning", ignoreCase = true) }
                            val eveningShifts = shiftReports.filter { it.shift.equals("Evening", ignoreCase = true) }

                            val morningTotal = morningShifts.sumOf { it.totalSales }
                            val eveningTotal = eveningShifts.sumOf { it.totalSales }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = SurfaceVariantLight,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariantLight),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = stringResource(R.string.shift_morning),
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = OnBackgroundLight
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(PrimaryBlue)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = stringResource(R.string.shifts_count, morningShifts.size),
                                            fontSize = 11.sp,
                                            color = TextSecondaryLight
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        val shiftDigits = MoneyFormat.minorUnitDigits(currency)
                                        Text(
                                            text = "${MoneyFormat.toPlainDecimal(morningTotal, shiftDigits)} $currency",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryBlue
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = SurfaceVariantLight,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariantLight),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = stringResource(R.string.shift_evening),
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = OnBackgroundLight
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFD97706))
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = stringResource(R.string.shifts_count, eveningShifts.size),
                                            fontSize = 11.sp,
                                            color = TextSecondaryLight
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        val shiftDigits = MoneyFormat.minorUnitDigits(currency)
                                        Text(
                                            text = "${MoneyFormat.toPlainDecimal(eveningTotal, shiftDigits)} $currency",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFD97706)
                                        )
                                    }
                                }
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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = OnBackgroundLight
                    )
                )
            }
            val barDigits = MoneyFormat.minorUnitDigits(currency)
            val amountStr = MoneyFormat.toPlainDecimal(MoneyFormat.toMinorUnits(amount, barDigits), barDigits)
            Text(
                text = "$amountStr $currency ($percent%)",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnBackgroundLight
                )
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { (percent / 100f).coerceIn(0f, 1f) },
            color = color,
            trackColor = SurfaceVariantLight,
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(4.dp))
        )
    }
}
