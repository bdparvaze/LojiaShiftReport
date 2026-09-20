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



import androidx.compose.animation.*


import androidx.compose.animation.core.*


import androidx.compose.foundation.Canvas


import androidx.compose.foundation.background


import androidx.compose.foundation.border


import androidx.compose.foundation.clickable


import androidx.compose.foundation.gestures.detectDragGestures


import androidx.compose.foundation.gestures.detectTapGestures


import androidx.compose.foundation.layout.*


import androidx.compose.foundation.shape.CircleShape


import androidx.compose.foundation.shape.RoundedCornerShape


import androidx.compose.material.icons.Icons


import androidx.compose.material.icons.automirrored.filled.ArrowBack


import androidx.compose.material.icons.automirrored.filled.ArrowForward


import androidx.compose.material.icons.filled.*


import androidx.compose.material.icons.outlined.*


import androidx.compose.material3.*


import androidx.compose.ui.res.stringResource



import androidx.compose.runtime.*


import androidx.compose.ui.Alignment


import androidx.compose.ui.Modifier


import androidx.compose.ui.draw.clip


import androidx.compose.ui.draw.shadow


import androidx.compose.ui.geometry.Offset


import androidx.compose.ui.geometry.Size


import androidx.compose.ui.graphics.*


import androidx.compose.ui.graphics.drawscope.DrawScope


import androidx.compose.ui.graphics.drawscope.Stroke


import androidx.compose.ui.input.pointer.pointerInput


import androidx.compose.ui.platform.testTag


import androidx.compose.ui.text.font.FontWeight


import androidx.compose.ui.text.style.TextAlign


import androidx.compose.ui.unit.dp


import androidx.compose.ui.unit.sp

import com.lojia.pos.data.AppLanguage

import com.lojia.pos.data.POSSale

import com.lojia.pos.data.ShiftReport


import java.text.SimpleDateFormat

import java.util.*

import kotlin.math.max

data class DaySalesData(
    val dayNumber: Int,
    val dateString: String,
    val dayName: String,
    val shiftSales: Double,
    val posSales: Double,
    val totalRevenue: Double,
    val transactionsCount: Int,
    val cashAmount: Double,
    val madaAmount: Double,
    val walletAmount: Double
)

enum class ChartType {
    AREA_TREND,
    BAR_CHART
}

@Composable
fun MonthlySalesSummaryView(
    shiftReports: List<ShiftReport>,
    salesHistory: List<POSSale>,
    currency: String,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    // Current active calendar for navigation
    var calendarMonthOffset by remember { mutableIntStateOf(0) }
    var selectedChartType by remember { mutableStateOf(ChartType.AREA_TREND) }
    var hoveredDayIndex by remember { mutableStateOf<Int?>(null) }
    var showShiftBreakdown by remember { mutableStateOf(true) }
    var showPosBreakdown by remember { mutableStateOf(true) }

    // Target Calendar based on offset
    val targetCalendar = remember(calendarMonthOffset) {
        Calendar.getInstance().apply {
            add(Calendar.MONTH, calendarMonthOffset)
        }
    }

    val currentYear = targetCalendar.get(Calendar.YEAR)
    val currentMonth = targetCalendar.get(Calendar.MONTH)
    val daysInMonth = targetCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    val activeLocale = remember(language) { Locale(language.code) }
    val monthNameFormatter = remember(language) {
        SimpleDateFormat("MMMM yyyy", activeLocale)
    }
    val currentMonthDisplayName = remember(targetCalendar, language) {
        monthNameFormatter.format(targetCalendar.time)
    }

    val isCurrentCalendarMonth = remember(calendarMonthOffset) { calendarMonthOffset == 0 }

    // Calculate aggregated daily data for the selected month
    val dailyDataList = remember(shiftReports, salesHistory, currentYear, currentMonth, daysInMonth, activeLocale) {
        val list = mutableListOf<DaySalesData>()
        val cal = Calendar.getInstance()
        val dayNameFmt = SimpleDateFormat("EEE", activeLocale)
        val dateFmt = SimpleDateFormat("MMM dd", activeLocale)

        for (day in 1..daysInMonth) {
            cal.set(currentYear, currentMonth, day, 0, 0, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val startOfDay = cal.timeInMillis
            cal.set(currentYear, currentMonth, day, 23, 59, 59)
            cal.set(Calendar.MILLISECOND, 999)
            val endOfDay = cal.timeInMillis

            // Filter shifts falling in this day
            val shiftsOnDay = shiftReports.filter { report ->
                val rCal = Calendar.getInstance().apply { timeInMillis = report.dateInMillis }
                rCal.get(Calendar.YEAR) == currentYear &&
                        rCal.get(Calendar.MONTH) == currentMonth &&
                        rCal.get(Calendar.DAY_OF_MONTH) == day
            }

            // Filter POS sales on this day
            val salesOnDay = salesHistory.filter { sale ->
                val sCal = Calendar.getInstance().apply { timeInMillis = sale.timestamp }
                sCal.get(Calendar.YEAR) == currentYear &&
                        sCal.get(Calendar.MONTH) == currentMonth &&
                        sCal.get(Calendar.DAY_OF_MONTH) == day
            }

            val shiftTotal = shiftsOnDay.sumOf { it.totalSales }
            val posTotal = salesOnDay.sumOf { it.totalAmount }
            val combined = shiftTotal + posTotal

            val cash = shiftsOnDay.sumOf { it.grossCash } + salesOnDay.filter { it.paymentMethod == "Cash" }.sumOf { it.totalAmount }
            val mada = shiftsOnDay.sumOf { it.madaPayments } + salesOnDay.filter { it.paymentMethod == "Mada" }.sumOf { it.totalAmount }
            val wallet = shiftsOnDay.sumOf { it.digitalWallet } + salesOnDay.filter { it.paymentMethod == "Digital Wallet" }.sumOf { it.totalAmount }

            cal.set(currentYear, currentMonth, day)
            list.add(
                DaySalesData(
                    dayNumber = day,
                    dateString = dateFmt.format(cal.time),
                    dayName = dayNameFmt.format(cal.time),
                    shiftSales = shiftTotal,
                    posSales = posTotal,
                    totalRevenue = combined,
                    transactionsCount = shiftsOnDay.size + salesOnDay.size,
                    cashAmount = cash,
                    madaAmount = mada,
                    walletAmount = wallet
                )
            )
        }
        list
    }

    // Monthly aggregates
    val totalMonthRevenue = dailyDataList.sumOf { it.totalRevenue }
    val totalMonthShiftsSales = dailyDataList.sumOf { it.shiftSales }
    val totalMonthPosSales = dailyDataList.sumOf { it.posSales }
    val daysWithSalesCount = dailyDataList.count { it.totalRevenue > 0 }.coerceAtLeast(1)
    val dailyAverageRevenue = totalMonthRevenue / daysWithSalesCount
    val peakDay = dailyDataList.maxByOrNull { it.totalRevenue }
    val projectedMonthClose = dailyAverageRevenue * daysInMonth

    // Comprehensive Monthly Automatic Calculations from Shift Reports in target month
    val monthShiftReports = remember(shiftReports, currentYear, currentMonth) {
        shiftReports.filter { report ->
            val cal = Calendar.getInstance().apply { timeInMillis = report.dateInMillis }
            cal.get(Calendar.YEAR) == currentYear && cal.get(Calendar.MONTH) == currentMonth
        }
    }

    val monthTotalIssuedDue = remember(monthShiftReports) {
        monthShiftReports.sumOf { report ->
            try {
                val arr = org.json.JSONArray(report.dueCreditEntriesJson)
                var sum = 0.0
                for (i in 0 until arr.length()) {
                    sum += arr.getJSONObject(i).optDouble("amount", 0.0)
                }
                sum
            } catch (e: Exception) { 0.0 }
        }
    }

    val monthTotalCollectedDue = remember(monthShiftReports) {
        monthShiftReports.sumOf { report ->
            try {
                val arr = org.json.JSONArray(report.previousDueCollectionsJson)
                var sum = 0.0
                for (i in 0 until arr.length()) {
                    sum += arr.getJSONObject(i).optDouble("amount", 0.0)
                }
                sum
            } catch (e: Exception) { 0.0 }
        }
    }

    val monthTotalExpenses = remember(monthShiftReports) {
        monthShiftReports.sumOf { it.totalExpenses }
    }

    val monthTotalPurchases = remember(monthShiftReports) {
        monthShiftReports.sumOf { report ->
            try {
                val arr = org.json.JSONArray(report.purchasedItemsJson)
                var sum = 0.0
                for (i in 0 until arr.length()) {
                    sum += arr.getJSONObject(i).optDouble("totalAmount", 0.0)
                }
                sum
            } catch (e: Exception) { 0.0 }
        }
    }

    val monthTotalStaffAdvances = remember(monthShiftReports) {
        monthShiftReports.sumOf { report ->
            try {
                val arr = org.json.JSONArray(report.staffAdvancesJson)
                var sum = 0.0
                for (i in 0 until arr.length()) {
                    sum += arr.getJSONObject(i).optDouble("amount", 0.0)
                }
                sum
            } catch (e: Exception) { 0.0 }
        }
    }

    val monthTotalCashIn = dailyDataList.sumOf { it.cashAmount }
    val monthTotalBankIn = dailyDataList.sumOf { it.madaAmount } + dailyDataList.sumOf { it.walletAmount }
    val monthNetCashflow = (monthTotalCashIn + monthTotalBankIn + monthTotalCollectedDue) - (monthTotalExpenses + monthTotalPurchases + monthTotalStaffAdvances)

    // Weekly breakdowns (Week 1..5)
    val weeklyBreakdowns = remember(dailyDataList) {
        val weeks = mutableListOf<Pair<String, Double>>()
        var week1 = 0.0
        var week2 = 0.0
        var week3 = 0.0
        var week4 = 0.0
        var week5 = 0.0

        dailyDataList.forEach { d ->
            when (d.dayNumber) {
                in 1..7 -> week1 += d.totalRevenue
                in 8..14 -> week2 += d.totalRevenue
                in 15..21 -> week3 += d.totalRevenue
                in 22..28 -> week4 += d.totalRevenue
                else -> week5 += d.totalRevenue
            }
        }
        weeks.add("Week 1 (1-7)" to week1)
        weeks.add("Week 2 (8-14)" to week2)
        weeks.add("Week 3 (15-21)" to week3)
        weeks.add("Week 4 (22-28)" to week4)
        if (week5 > 0 || dailyDataList.size > 28) {
            weeks.add("Week 5 (29-${dailyDataList.size})" to week5)
        }
        weeks
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("monthly_sales_summary_view"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Month Header & Time Navigation Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = PureWhite,
            border = androidx.compose.foundation.BorderStroke(1.dp, OutlineLight),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Timeline,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.monthly_sales_summary),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = TextPrimaryLight
                                )
                            )
                        }
                        Text(
                            text = if (isCurrentCalendarMonth)
                                "Real-time trends & daily revenue trajectory for the active cycle"
                            else
                                "Historical revenue records & performance trends",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondaryLight,
                                fontSize = 12.sp
                            )
                        )
                    }

                    if (isCurrentCalendarMonth) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFFEFF6FF),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(AccentEmerald)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.current_month),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryBlue
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = OutlineLight, thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                // Month Switcher Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { calendarMonthOffset -= 1 },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(BgLightGrey)
                            .testTag("prev_month_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_previous_month),
                            tint = TextPrimaryLight,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { calendarMonthOffset = 0 }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = currentMonthDisplayName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = TextPrimaryLight
                            )
                        )
                    }

                    IconButton(
                        onClick = { calendarMonthOffset += 1 },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(BgLightGrey)
                            .testTag("next_month_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = stringResource(R.string.cd_next_month),
                            tint = TextPrimaryLight,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // 2. Primary KPI Highlights Grid (4 Cards)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MonthlyKpiCard(
                title = stringResource(R.string.total_revenue),
                value = "%.2f %s".format(totalMonthRevenue, currency),
                subtitle = stringResource(R.string.sales_shifts_count_fmt, dailyDataList.sumOf { it.transactionsCount }),
                icon = Icons.Default.Paid,
                accentColor = AccentEmerald,
                modifier = Modifier.weight(1f)
            )
            MonthlyKpiCard(
                title = stringResource(R.string.daily_average),
                value = "%.2f %s".format(dailyAverageRevenue, currency),
                subtitle = stringResource(R.string.across_active_days_fmt, daysWithSalesCount),
                icon = Icons.Default.TrendingUp,
                accentColor = PrimaryBlue,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MonthlyKpiCard(
                title = stringResource(R.string.peak_sales_day),
                value = if (peakDay != null && peakDay.totalRevenue > 0) "%.2f %s".format(peakDay.totalRevenue, currency) else "0.00 $currency",
                subtitle = if (peakDay != null && peakDay.totalRevenue > 0) "${peakDay.dateString} (${peakDay.dayName})" else stringResource(R.string.no_data_available),
                icon = Icons.Default.Star,
                accentColor = AccentGold,
                modifier = Modifier.weight(1f)
            )
            MonthlyKpiCard(
                title = stringResource(R.string.projected_monthly),
                value = "%.2f %s".format(projectedMonthClose, currency),
                subtitle = stringResource(R.string.subtitle_estimated_30_day_run),
                icon = Icons.Default.QueryStats,
                accentColor = PrimaryBlue,
                modifier = Modifier.weight(1f)
            )
        }

        // 2.5 Automated Full Monthly Financial Breakdown Section
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = PureWhite,
            border = androidx.compose.foundation.BorderStroke(1.dp, OutlineLight),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Calculate, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.auto_monthly_financial_breakdown_fmt, currentMonthDisplayName),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimaryLight)
                    )
                }
                Text(
                    text = stringResource(R.string.auto_calculated_financial_summary),
                    fontSize = 11.sp,
                    color = TextSecondaryLight
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Row 1: Cash & Bank Inflow vs Due Issued/Collected
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF0FDF4),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(stringResource(R.string.total_cash_in), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF166534))
                            Text(stringResource(R.string.msg_2f_s_21).format(monthTotalCashIn, currency), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF15803D))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(stringResource(R.string.bank_mada_fmt, "%.2f".format(monthTotalBankIn), currency), fontSize = 10.sp, color = TextSecondaryLight)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFEF2F2),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(stringResource(R.string.total_issued_due), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF991B1B))
                            Text(stringResource(R.string.msg_2f_s_21).format(monthTotalIssuedDue, currency), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFDC2626))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(stringResource(R.string.due_collected_fmt, "%.2f".format(monthTotalCollectedDue), currency), fontSize = 10.sp, color = Color(0xFF166534))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Row 2: Expenses & Product Purchases
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFFFBEB),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(stringResource(R.string.total_expenses), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF92400E))
                            Text(stringResource(R.string.msg_2f_s_21).format(monthTotalExpenses, currency), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFD97706))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(stringResource(R.string.staff_advances_fmt, "%.2f".format(monthTotalStaffAdvances), currency), fontSize = 10.sp, color = TextSecondaryLight)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF5F3FF),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDDD6FE)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(stringResource(R.string.product_purchases), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5B21B6))
                            Text(stringResource(R.string.msg_2f_s_21).format(monthTotalPurchases, currency), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF7C3AED))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(stringResource(R.string.inventory_investment), fontSize = 10.sp, color = TextSecondaryLight)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Net Monthly Cashflow Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (monthNetCashflow >= 0) Color(0xFFECFDF5) else Color(0xFFFEF2F2),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (monthNetCashflow >= 0) Color(0xFFA7F3D0) else Color(0xFFFECACA)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                stringResource(R.string.net_monthly_financial_cashflow),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (monthNetCashflow >= 0) Color(0xFF065F46) else Color(0xFF991B1B)
                            )
                            Text(
                                stringResource(R.string.net_cashflow_formula),
                                fontSize = 10.sp,
                                color = TextSecondaryLight
                            )
                        }
                        Text(
                            stringResource(R.string.msg_2f_s_21).format(monthNetCashflow, currency),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (monthNetCashflow >= 0) Color(0xFF059669) else Color(0xFFDC2626)
                        )
                    }
                }
            }
        }

        // 3. Recharts-Styled Interactive Chart Canvas Container
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = PureWhite,
            border = androidx.compose.foundation.BorderStroke(1.dp, OutlineLight),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Chart Title & Toggle Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.monthly_revenue_trend),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryLight
                            )
                        )
                        Text(
                            text = stringResource(R.string.interactive_curve_with_tooltips),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondaryLight,
                                fontSize = 11.5.sp
                            )
                        )
                    }

                    // Chart Mode Selector (Area Curve vs Bar Chart)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = BgLightGrey,
                        border = androidx.compose.foundation.BorderStroke(1.dp, OutlineLight)
                    ) {
                        Row(modifier = Modifier.padding(2.dp)) {
                            IconButton(
                                onClick = { selectedChartType = ChartType.AREA_TREND },
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (selectedChartType == ChartType.AREA_TREND) PureWhite else Color.Transparent)
                                    .testTag("chart_mode_area")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShowChart,
                                    contentDescription = stringResource(R.string.cd_area_trend),
                                    tint = if (selectedChartType == ChartType.AREA_TREND) PrimaryBlue else TextSecondaryLight,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = { selectedChartType = ChartType.BAR_CHART },
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (selectedChartType == ChartType.BAR_CHART) PureWhite else Color.Transparent)
                                    .testTag("chart_mode_bar")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BarChart,
                                    contentDescription = stringResource(R.string.cd_bar_chart),
                                    tint = if (selectedChartType == ChartType.BAR_CHART) PrimaryBlue else TextSecondaryLight,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Stream Breakdown Toggles / Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendItem(
                        label = stringResource(R.string.label_total_combined),
                        color = PrimaryBlue,
                        isActive = true,
                        onClick = {}
                    )
                    LegendItem(
                        label = stringResource(R.string.pos_direct_sales),
                        color = AccentEmerald,
                        isActive = showPosBreakdown,
                        onClick = { showPosBreakdown = !showPosBreakdown }
                    )
                    LegendItem(
                        label = stringResource(R.string.shift_cashier_sales),
                        color = AccentGold,
                        isActive = showShiftBreakdown,
                        onClick = { showShiftBreakdown = !showShiftBreakdown }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Interactive Recharts-Styled Chart Rendering Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .testTag("recharts_canvas_box")
                ) {
                    if (selectedChartType == ChartType.AREA_TREND) {
                        RechartsAreaTrendCanvas(
                            data = dailyDataList,
                            currency = currency,
                            hoveredIndex = hoveredDayIndex,
                            showPosBreakdown = showPosBreakdown,
                            showShiftBreakdown = showShiftBreakdown,
                            onHoverIndexChange = { hoveredDayIndex = it }
                        )
                    } else {
                        RechartsBarChartCanvas(
                            data = dailyDataList,
                            currency = currency,
                            hoveredIndex = hoveredDayIndex,
                            onHoverIndexChange = { hoveredDayIndex = it }
                        )
                    }
                }

                // Interactive Hover/Touch Tooltip Card
                hoveredDayIndex?.let { index ->
                    if (index in dailyDataList.indices) {
                        val d = dailyDataList[index]
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF0F172A),
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(6.dp, RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "${d.dateString} (${d.dayName})",
                                        fontWeight = FontWeight.Bold,
                                        color = PureWhite,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = stringResource(R.string.day_x_of_y_fmt, d.dayNumber, daysInMonth),
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.sp
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = stringResource(R.string.pos_2f).format(d.posSales),
                                            color = AccentEmerald,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = stringResource(R.string.shift_2f).format(d.shiftSales),
                                            color = AccentGold,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = PrimaryBlue.copy(alpha = 0.25f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.6f))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = stringResource(R.string.total_revenue_1),
                                                color = Color(0xFF93C5FD),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = stringResource(R.string.msg_2f_s_21).format(d.totalRevenue, currency),
                                                color = PureWhite,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
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

        // 4. Weekly Breakdown Progress Cards (Recharts style progress visualizer)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = PureWhite,
            border = androidx.compose.foundation.BorderStroke(1.dp, OutlineLight),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ViewWeek,
                            contentDescription = null,
                            tint = PrimaryIndigo,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.weekly_breakdown),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Text(
                        text = stringResource(R.string.msg_5week_distribution),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondaryLight
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                weeklyBreakdowns.forEach { (weekLabel, weekAmount) ->
                    val percentage = if (totalMonthRevenue > 0) (weekAmount / totalMonthRevenue * 100).toInt() else 0

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = weekLabel,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = TextPrimaryLight
                            )
                            Text(
                                text = stringResource(R.string.msg_2f_s_d_1).format(weekAmount, currency, percentage),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (weekAmount > 0) PrimaryIndigo else TextSecondaryLight
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LinearProgressIndicator(
                            progress = { (percentage / 100f).coerceIn(0f, 1f) },
                            color = PrimaryIndigo,
                            trackColor = BgLightGrey,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                    }
                }
            }
        }

        // 5. Top 5 Revenue Days Leaderboard in the Current Month
        val topDays = remember(dailyDataList) {
            dailyDataList.filter { it.totalRevenue > 0 }.sortedByDescending { it.totalRevenue }.take(5)
        }

        if (topDays.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = PureWhite,
                border = androidx.compose.foundation.BorderStroke(1.dp, OutlineLight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Leaderboard,
                            contentDescription = null,
                            tint = AccentGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.top_revenue_days),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    topDays.forEachIndexed { rank, dayData ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = when (rank) {
                                        0 -> Color(0xFFFEF3C7)
                                        1 -> Color(0xFFF1F5F9)
                                        2 -> Color(0xFFFFEDD5)
                                        else -> BgLightGrey
                                    },
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "#${rank + 1}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when (rank) {
                                                0 -> Color(0xFFB45309)
                                                1 -> Color(0xFF475569)
                                                2 -> Color(0xFFC2410C)
                                                else -> TextSecondaryLight
                                            }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column {
                                    Text(
                                        text = "${dayData.dateString} (${dayData.dayName})",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.5.sp,
                                        color = TextPrimaryLight
                                    )
                                    Text(
                                        text = stringResource(R.string.cash_0f_mada_0f).format(dayData.cashAmount, dayData.madaAmount),
                                        fontSize = 11.sp,
                                        color = TextSecondaryLight
                                    )
                                }
                            }

                            Text(
                                text = stringResource(R.string.msg_2f_s_21).format(dayData.totalRevenue, currency),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = AccentEmerald
                            )
                        }
                        if (rank < topDays.size - 1) {
                            HorizontalDivider(color = OutlineLight.copy(alpha = 0.5f), thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }
}

