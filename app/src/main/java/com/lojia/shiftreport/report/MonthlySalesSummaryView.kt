package com.lojia.shiftreport.report

import com.lojia.shiftreport.R
import com.lojia.shiftreport.data.*
import com.lojia.shiftreport.util.*
import com.lojia.shiftreport.ui.common.*
import com.lojia.shiftreport.ui.theme.*
import com.lojia.shiftreport.auth.*
import com.lojia.shiftreport.report.*
import com.lojia.shiftreport.settings.*



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

import com.lojia.shiftreport.data.AppLanguage
import com.lojia.shiftreport.data.ShiftReport


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
    currency: String,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    // Current active calendar for navigation
    var calendarMonthOffset by remember { mutableIntStateOf(0) }
    var selectedChartType by remember { mutableStateOf(ChartType.AREA_TREND) }
    var hoveredDayIndex by remember { mutableStateOf<Int?>(null) }
    var showShiftBreakdown by remember { mutableStateOf(true) }

    // Target Calendar based on offset
    val targetCalendar = remember(calendarMonthOffset) {
        Calendar.getInstance().apply {
            add(Calendar.MONTH, calendarMonthOffset)
        }
    }

    val currentYear = targetCalendar.get(Calendar.YEAR)
    val currentMonth = targetCalendar.get(Calendar.MONTH)
    val daysInMonth = targetCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    val activeLocale = remember(language) { Locale.forLanguageTag(language.code) }
    val monthNameFormatter = remember(language) {
        SimpleDateFormat("MMMM yyyy", activeLocale)
    }
    val currentMonthDisplayName = remember(targetCalendar, language) {
        monthNameFormatter.format(targetCalendar.time)
    }

    val isCurrentCalendarMonth = remember(calendarMonthOffset) { calendarMonthOffset == 0 }

    val summaryDigits = MoneyFormat.minorUnitDigits(currency)

    // Calculate aggregated daily data for the selected month
    val dailyDataList = remember(shiftReports, currentYear, currentMonth, daysInMonth, activeLocale, summaryDigits) {
        val list = mutableListOf<DaySalesData>()
        val cal = Calendar.getInstance()
        val dayNameFmt = SimpleDateFormat("EEE", activeLocale)
        val dateFmt = SimpleDateFormat("MMM dd", activeLocale)

        for (day in 1..daysInMonth) {
            cal.set(currentYear, currentMonth, day, 0, 0, 0)
            cal.set(Calendar.MILLISECOND, 0)

            // Filter shifts falling in this day
            val shiftsOnDay = shiftReports.filter { report ->
                val rCal = Calendar.getInstance().apply { timeInMillis = report.dateInMillis }
                rCal.get(Calendar.YEAR) == currentYear &&
                        rCal.get(Calendar.MONTH) == currentMonth &&
                        rCal.get(Calendar.DAY_OF_MONTH) == day
            }

            val shiftTotal = shiftsOnDay.sumOf { it.totalSales }
            val combined = shiftTotal

            val cash = shiftsOnDay.sumOf { it.grossCash }
            val mada = shiftsOnDay.sumOf { it.madaPayments }
            val wallet = shiftsOnDay.sumOf { it.digitalWallet }

            cal.set(currentYear, currentMonth, day)
            list.add(
                DaySalesData(
                    dayNumber = day,
                    dateString = dateFmt.format(cal.time),
                    dayName = dayNameFmt.format(cal.time),
                    shiftSales = MoneyFormat.toMajorUnits(shiftTotal, summaryDigits),
                    posSales = 0.0,
                    totalRevenue = MoneyFormat.toMajorUnits(combined, summaryDigits),
                    transactionsCount = shiftsOnDay.size,
                    cashAmount = MoneyFormat.toMajorUnits(cash, summaryDigits),
                    madaAmount = MoneyFormat.toMajorUnits(mada, summaryDigits),
                    walletAmount = MoneyFormat.toMajorUnits(wallet, summaryDigits)
                )
            )
        }
        list
    }

    // Monthly aggregates
    val totalMonthRevenue = dailyDataList.sumOf { it.totalRevenue }
    val totalMonthShiftsSales = dailyDataList.sumOf { it.shiftSales }
    val totalMonthPosSales = dailyDataList.sumOf { it.posSales }
    val actualActiveDays = dailyDataList.count { it.totalRevenue > 0 }
    val dailyAverageRevenue = if (actualActiveDays > 0) totalMonthRevenue / actualActiveDays else 0.0
    val peakDay = dailyDataList.maxByOrNull { it.totalRevenue }

    // Mathematically sound monthly projection:
    // Past month: finalized actual revenue (month has already concluded).
    // Future month: 0.0 (no sales recorded yet).
    // Current month: run-rate projection based on calendar days elapsed (daily run-rate * daysInMonth).
    val projectedMonthClose = when {
        calendarMonthOffset < 0 -> totalMonthRevenue
        calendarMonthOffset > 0 -> 0.0
        else -> {
            val todayDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH).coerceIn(1, daysInMonth)
            if (totalMonthRevenue > 0.0 && todayDay > 0) {
                (totalMonthRevenue / todayDay) * daysInMonth
            } else {
                0.0
            }
        }
    }

    // Comprehensive Monthly Automatic Calculations from Shift Reports in target month
    val monthShiftReports = remember(shiftReports, currentYear, currentMonth) {
        shiftReports.filter { report ->
            val cal = Calendar.getInstance().apply { timeInMillis = report.dateInMillis }
            cal.get(Calendar.YEAR) == currentYear && cal.get(Calendar.MONTH) == currentMonth
        }
    }

    val monthTotalIssuedDue = remember(monthShiftReports, summaryDigits) {
        val minorSum = monthShiftReports.sumOf { report ->
            try {
                val arr = org.json.JSONArray(report.dueCreditEntriesJson)
                var sum = 0L
                for (i in 0 until arr.length()) {
                    sum += arr.getJSONObject(i).optLong("amount", 0L)
                }
                sum
            } catch (e: Exception) { 0L }
        }
        MoneyFormat.toMajorUnits(minorSum, summaryDigits)
    }

    val monthTotalCollectedDue = remember(monthShiftReports, summaryDigits) {
        val minorSum = monthShiftReports.sumOf { report ->
            try {
                val arr = org.json.JSONArray(report.previousDueCollectionsJson)
                var sum = 0L
                for (i in 0 until arr.length()) {
                    sum += arr.getJSONObject(i).optLong("amount", 0L)
                }
                sum
            } catch (e: Exception) { 0L }
        }
        MoneyFormat.toMajorUnits(minorSum, summaryDigits)
    }

    val monthTotalExpenses = remember(monthShiftReports, summaryDigits) {
        val minorSum = monthShiftReports.sumOf { it.totalExpenses }
        MoneyFormat.toMajorUnits(minorSum, summaryDigits)
    }

    val monthTotalPurchases = remember(monthShiftReports, summaryDigits) {
        val minorSum = monthShiftReports.sumOf { report ->
            try {
                val arr = org.json.JSONArray(report.purchasedItemsJson)
                var sum = 0L
                for (i in 0 until arr.length()) {
                    sum += arr.getJSONObject(i).optLong("totalAmount", 0L)
                }
                sum
            } catch (e: Exception) { 0L }
        }
        MoneyFormat.toMajorUnits(minorSum, summaryDigits)
    }

    val monthTotalStaffAdvances = remember(monthShiftReports, summaryDigits) {
        val minorSum = monthShiftReports.sumOf { report ->
            try {
                val arr = org.json.JSONArray(report.staffAdvancesJson)
                var sum = 0L
                for (i in 0 until arr.length()) {
                    sum += arr.getJSONObject(i).optLong("amount", 0L)
                }
                sum
            } catch (e: Exception) { 0L }
        }
        MoneyFormat.toMajorUnits(minorSum, summaryDigits)
    }

    val monthTotalCashIn = dailyDataList.sumOf { it.cashAmount }
    val monthTotalBankIn = dailyDataList.sumOf { it.madaAmount } + dailyDataList.sumOf { it.walletAmount }
    val monthNetCashflow = (monthTotalCashIn + monthTotalBankIn + monthTotalCollectedDue) - (monthTotalExpenses + monthTotalPurchases + monthTotalStaffAdvances)

    val week1Label = stringResource(R.string.monthly_week_1_label)
    val week2Label = stringResource(R.string.monthly_week_2_label)
    val week3Label = stringResource(R.string.monthly_week_3_label)
    val week4Label = stringResource(R.string.monthly_week_4_label)
    val week5Label = stringResource(R.string.monthly_week_5_fmt, dailyDataList.size)

    // Weekly breakdowns (Week 1..5)
    val weeklyBreakdowns = remember(dailyDataList, week1Label, week2Label, week3Label, week4Label, week5Label) {
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
        weeks.add(week1Label to week1)
        weeks.add(week2Label to week2)
        weeks.add(week3Label to week3)
        weeks.add(week4Label to week4)
        if (week5 > 0 || dailyDataList.size > 28) {
            weeks.add(week5Label to week5)
        }
        weeks
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("monthly_sales_summary_view"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Month Navigator Card (~56dp, 14dp radius, white bg, 1dp border)
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = PureWhite,
            border = androidx.compose.foundation.BorderStroke(1.dp, OutlineLight),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { calendarMonthOffset -= 1 },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceVariantLight)
                        .testTag("prev_month_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_previous_month),
                        tint = OnBackgroundLight,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { calendarMonthOffset = 0 }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = currentMonthDisplayName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = OnBackgroundLight
                        )
                    )
                    if (isCurrentCalendarMonth) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF0FDF4),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF16A34A))
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.current_month),
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF166534)
                                )
                            }
                        }
                    }
                }

                IconButton(
                    onClick = { calendarMonthOffset += 1 },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceVariantLight)
                        .testTag("next_month_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = stringResource(R.string.cd_next_month),
                        tint = OnBackgroundLight,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // 2. Primary KPI Highlights Grid (4 Cards in 2x2 Grid)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MonthlyKpiCard(
                title = stringResource(R.string.total_revenue),
                value = "${MoneyFormat.toPlainDecimal(MoneyFormat.toMinorUnits(totalMonthRevenue, summaryDigits), summaryDigits)} $currency",
                subtitle = stringResource(R.string.sales_shifts_count_fmt, dailyDataList.sumOf { it.transactionsCount }),
                icon = Icons.Default.Paid,
                accentColor = Color(0xFF059669),
                modifier = Modifier.weight(1f)
            )
            MonthlyKpiCard(
                title = stringResource(R.string.daily_average),
                value = "${MoneyFormat.toPlainDecimal(MoneyFormat.toMinorUnits(dailyAverageRevenue, summaryDigits), summaryDigits)} $currency",
                subtitle = stringResource(R.string.across_active_days_fmt, actualActiveDays),
                icon = Icons.Default.TrendingUp,
                accentColor = Color(0xFF2563EB),
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MonthlyKpiCard(
                title = stringResource(R.string.peak_sales_day),
                value = if (peakDay != null && peakDay.totalRevenue > 0) "${MoneyFormat.toPlainDecimal(MoneyFormat.toMinorUnits(peakDay.totalRevenue, summaryDigits), summaryDigits)} $currency" else "${MoneyFormat.zeroPlaceholder(summaryDigits)} $currency",
                subtitle = if (peakDay != null && peakDay.totalRevenue > 0) "${peakDay.dateString} (${peakDay.dayName})" else stringResource(R.string.no_data_available),
                icon = Icons.Default.Star,
                accentColor = Color(0xFFD97706),
                modifier = Modifier.weight(1f)
            )
            val projectedSubtitle = when {
                calendarMonthOffset < 0 -> stringResource(R.string.finalized_month_revenue)
                calendarMonthOffset > 0 -> stringResource(R.string.no_data_available)
                else -> stringResource(R.string.subtitle_estimated_30_day_run)
            }
            MonthlyKpiCard(
                title = stringResource(R.string.projected_monthly),
                value = "${MoneyFormat.toPlainDecimal(MoneyFormat.toMinorUnits(projectedMonthClose, summaryDigits), summaryDigits)} $currency",
                subtitle = projectedSubtitle,
                icon = Icons.Default.QueryStats,
                accentColor = Color(0xFF6366F1),
                modifier = Modifier.weight(1f)
            )
        }

        // 3. Chart Hero Card (14dp radius, 14dp padding, white bg, 1dp border)
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = PureWhite,
            border = androidx.compose.foundation.BorderStroke(1.dp, OutlineLight),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
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
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.5.sp,
                                color = OnBackgroundLight
                            )
                        )
                        Text(
                            text = stringResource(R.string.interactive_curve_with_tooltips),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondaryLight,
                                fontSize = 11.sp
                            )
                        )
                    }

                    // Chart Mode Selector (Area Curve vs Bar Chart)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SurfaceVariantLight,
                        border = androidx.compose.foundation.BorderStroke(1.dp, OutlineLight)
                    ) {
                        Row(modifier = Modifier.padding(2.dp)) {
                            IconButton(
                                onClick = { selectedChartType = ChartType.AREA_TREND },
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (selectedChartType == ChartType.AREA_TREND) PureWhite else Color.Transparent)
                                    .testTag("chart_mode_area")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShowChart,
                                    contentDescription = stringResource(R.string.cd_area_trend),
                                    tint = if (selectedChartType == ChartType.AREA_TREND) PrimaryBlue else TextSecondaryLight,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = { selectedChartType = ChartType.BAR_CHART },
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (selectedChartType == ChartType.BAR_CHART) PureWhite else Color.Transparent)
                                    .testTag("chart_mode_bar")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BarChart,
                                    contentDescription = stringResource(R.string.cd_bar_chart),
                                    tint = if (selectedChartType == ChartType.BAR_CHART) PrimaryBlue else TextSecondaryLight,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Stream Breakdown Toggles / Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendItem(
                        label = stringResource(R.string.label_total_combined),
                        color = PrimaryBlue,
                        isActive = true,
                        onClick = {}
                    )
                    LegendItem(
                        label = stringResource(R.string.shift_cashier_sales),
                        color = AccentGold,
                        isActive = showShiftBreakdown,
                        onClick = { showShiftBreakdown = !showShiftBreakdown }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                val hasSalesData = dailyDataList.any { it.totalRevenue > 0 }

                // Interactive Chart Rendering Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .testTag("recharts_canvas_box"),
                    contentAlignment = Alignment.Center
                ) {
                    if (!hasSalesData) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(SurfaceVariantLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BarChart,
                                    contentDescription = null,
                                    tint = TextDisabledColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.no_data_available),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondaryLight,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    } else if (selectedChartType == ChartType.AREA_TREND) {
                        RechartsAreaTrendCanvas(
                            data = dailyDataList,
                            currency = currency,
                            hoveredIndex = hoveredDayIndex,
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
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = OnBackgroundLight,
                            modifier = Modifier.fillMaxWidth()
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
                                        color = TextDisabledColor,
                                        fontSize = 11.sp
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (showShiftBreakdown && d.shiftSales > 0) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "${stringResource(R.string.shift_2)}: ${MoneyFormat.toPlainDecimal(MoneyFormat.toMinorUnits(d.shiftSales, summaryDigits), summaryDigits)}",
                                                color = AccentGold,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = PrimaryBlue.copy(alpha = 0.25f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.6f))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = stringResource(R.string.total_revenue_1),
                                                color = PrimaryLight,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = "${MoneyFormat.toPlainDecimal(MoneyFormat.toMinorUnits(d.totalRevenue, summaryDigits), summaryDigits)} $currency",
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

        // 4. Automated Full Monthly Financial Breakdown Section
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
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(PrimaryBlue.copy(alpha = 0.10f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.auto_monthly_financial_breakdown_fmt, currentMonthDisplayName),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.5.sp,
                                color = OnBackgroundLight
                            )
                        )
                        Text(
                            text = stringResource(R.string.auto_calculated_financial_summary),
                            fontSize = 11.sp,
                            color = TextSecondaryLight
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Row 1: Cash Inflow
                MonthlyFinancialRowItem(
                    icon = Icons.Default.Payments,
                    iconTint = Color(0xFF059669),
                    label = stringResource(R.string.total_cash_in),
                    detail = stringResource(R.string.bank_mada_fmt, MoneyFormat.toPlainDecimal(MoneyFormat.toMinorUnits(monthTotalBankIn, summaryDigits), summaryDigits), currency),
                    amount = "${MoneyFormat.toPlainDecimal(MoneyFormat.toMinorUnits(monthTotalCashIn, summaryDigits), summaryDigits)} $currency",
                    amountColor = Color(0xFF059669)
                )
                HorizontalDivider(color = OutlineVariantLight, thickness = 0.5.dp)

                // Row 2: Digital / Bank Card
                MonthlyFinancialRowItem(
                    icon = Icons.Default.CreditCard,
                    iconTint = Color(0xFF2563EB),
                    label = stringResource(R.string.mada_payments) + " / " + stringResource(R.string.digital_wallet),
                    detail = stringResource(R.string.monthly_mada_digital_settlements),
                    amount = "${MoneyFormat.toPlainDecimal(MoneyFormat.toMinorUnits(monthTotalBankIn, summaryDigits), summaryDigits)} $currency",
                    amountColor = Color(0xFF2563EB)
                )
                HorizontalDivider(color = OutlineVariantLight, thickness = 0.5.dp)

                // Row 3: Due Credit Issued & Collected
                MonthlyFinancialRowItem(
                    icon = Icons.Default.AccountBalanceWallet,
                    iconTint = Color(0xFFEA580C),
                    label = stringResource(R.string.total_issued_due),
                    detail = stringResource(R.string.due_collected_fmt, MoneyFormat.toPlainDecimal(MoneyFormat.toMinorUnits(monthTotalCollectedDue, summaryDigits), summaryDigits), currency),
                    amount = "${MoneyFormat.toPlainDecimal(MoneyFormat.toMinorUnits(monthTotalIssuedDue, summaryDigits), summaryDigits)} $currency",
                    amountColor = Color(0xFFEA580C)
                )
                HorizontalDivider(color = OutlineVariantLight, thickness = 0.5.dp)

                // Row 4: Expenses
                MonthlyFinancialRowItem(
                    icon = Icons.Default.ReceiptLong,
                    iconTint = Color(0xFFDC2626),
                    label = stringResource(R.string.total_expenses),
                    detail = stringResource(R.string.operational_costs),
                    amount = "${MoneyFormat.toPlainDecimal(MoneyFormat.toMinorUnits(monthTotalExpenses, summaryDigits), summaryDigits)} $currency",
                    amountColor = Color(0xFFDC2626)
                )
                HorizontalDivider(color = OutlineVariantLight, thickness = 0.5.dp)

                // Row 5: Product Purchases
                MonthlyFinancialRowItem(
                    icon = Icons.Default.ShoppingBag,
                    iconTint = Color(0xFF7C3AED),
                    label = stringResource(R.string.product_purchases),
                    detail = stringResource(R.string.inventory_investment),
                    amount = "${MoneyFormat.toPlainDecimal(MoneyFormat.toMinorUnits(monthTotalPurchases, summaryDigits), summaryDigits)} $currency",
                    amountColor = Color(0xFF7C3AED)
                )
                HorizontalDivider(color = OutlineVariantLight, thickness = 0.5.dp)

                // Row 6: Staff Advances
                MonthlyFinancialRowItem(
                    icon = Icons.Default.People,
                    iconTint = Color(0xFFD97706),
                    label = stringResource(R.string.staff_advances_fmt, MoneyFormat.toPlainDecimal(MoneyFormat.toMinorUnits(monthTotalStaffAdvances, summaryDigits), summaryDigits), currency).substringBefore(":"),
                    detail = stringResource(R.string.monthly_salary_advance_payouts),
                    amount = "${MoneyFormat.toPlainDecimal(MoneyFormat.toMinorUnits(monthTotalStaffAdvances, summaryDigits), summaryDigits)} $currency",
                    amountColor = Color(0xFFD97706)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Net Monthly Cashflow Card
                val isNetPositive = monthNetCashflow >= 0
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isNetPositive) Color(0xFFF0FDF4) else Color(0xFFFEF2F2),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isNetPositive) Color(0xFFBBF7D0) else Color(0xFFFECACA)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.net_monthly_financial_cashflow),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isNetPositive) Color(0xFF067647) else Color(0xFFB42318)
                            )
                            Text(
                                text = stringResource(R.string.net_cashflow_formula),
                                fontSize = 10.sp,
                                color = if (isNetPositive) Color(0xFF067647).copy(alpha = 0.8f) else Color(0xFFB42318).copy(alpha = 0.8f)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${MoneyFormat.toPlainDecimal(MoneyFormat.toMinorUnits(monthNetCashflow, summaryDigits), summaryDigits)} $currency",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isNetPositive) Color(0xFF067647) else Color(0xFFB42318)
                        )
                    }
                }
            }
        }

        // 4. Weekly Breakdown Progress Cards
        WeeklyBreakdownCard(
            weeklyBreakdowns = weeklyBreakdowns,
            totalMonthRevenue = totalMonthRevenue,
            currency = currency
        )

        // 5. Top 5 Revenue Days Leaderboard in the Current Month
        TopRevenueDaysCard(
            dailyDataList = dailyDataList,
            currency = currency
        )
    }
}

@Composable
private fun MonthlyFinancialRowItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    label: String,
    detail: String,
    amount: String,
    amountColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(15.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = label,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = OnBackgroundLight
                )
                Text(
                    text = detail,
                    fontSize = 10.5.sp,
                    color = TextSecondaryLight
                )
            }
        }

        Text(
            text = amount,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            color = amountColor
        )
    }
}
