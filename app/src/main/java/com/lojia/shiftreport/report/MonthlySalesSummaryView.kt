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

import com.lojia.shiftreport.data.AppLanguage
import com.lojia.shiftreport.data.ShiftReport
import com.lojia.shiftreport.util.DateTimeFormatUtils
import java.time.LocalDate
import java.time.YearMonth
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
    val currentYearMonth = remember(currentYear, currentMonth) {
        YearMonth.of(currentYear, currentMonth + 1)
    }
    val currentMonthDisplayName = remember(currentYearMonth, activeLocale) {
        DateTimeFormatUtils.formatMonthYear(currentYearMonth, activeLocale)
    }

    val isCurrentCalendarMonth = remember(calendarMonthOffset) { calendarMonthOffset == 0 }

    val summaryDigits = MoneyFormat.minorUnitDigits(currency)

    // Calculate aggregated daily data for the selected month
    val dailyDataList = remember(shiftReports, currentYear, currentMonth, daysInMonth, activeLocale, summaryDigits) {
        val list = mutableListOf<DaySalesData>()

        for (day in 1..daysInMonth) {
            val localDate = LocalDate.of(currentYear, currentMonth + 1, day)

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

            list.add(
                DaySalesData(
                    dayNumber = day,
                    dateString = DateTimeFormatUtils.formatMonthDay(localDate, activeLocale),
                    dayName = DateTimeFormatUtils.formatShortWeekday(localDate.dayOfWeek, activeLocale),
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
        verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMd)
    ) {
        // 1. Month Navigator Card (~56dp, 10dp radius, depth card)
        Surface(
            shape = RoundedCornerShape(Dimens.CardCornerRadius),
            color = Color.Transparent,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Dimens.TopBarHeight)
                .cardRise(0)
                .depthCard(shape = RoundedCornerShape(Dimens.CardCornerRadius))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.SpacingSm, vertical = Dimens.SpacingXs),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { calendarMonthOffset -= 1 },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("prev_month_btn")
                ) {
                    Box(
                        modifier = Modifier
                            .size(Dimens.AvatarBadgeSize)
                            .clip(RoundedCornerShape(Dimens.RadiusLg))
                            .background(SurfaceVariantLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_previous_month),
                            tint = OnBackgroundLight,
                            modifier = Modifier.size(Dimens.IconMd)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .heightIn(min = Dimens.MinTouchTarget)
                        .clip(RoundedCornerShape(Dimens.RadiusMd))
                        .clickable { calendarMonthOffset = 0 }
                        .padding(horizontal = Dimens.SpacingSm, vertical = Dimens.SpacingXs)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(Dimens.IconAction)
                    )
                    Spacer(modifier = Modifier.width(Dimens.SpacingSm))
                    Text(
                        text = currentMonthDisplayName,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = OnBackgroundLight
                        )
                    )
                    if (isCurrentCalendarMonth) {
                        Spacer(modifier = Modifier.width(Dimens.SpacingSm))
                        Surface(
                            shape = RoundedCornerShape(Dimens.RadiusPill),
                            color = PositiveBannerSurface,
                            border = androidx.compose.foundation.BorderStroke(Dimens.FlatBorderWidth, PositiveBannerBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = Dimens.SpacingXs, vertical = Dimens.SpacingXxs),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                LojiaLiveDot()
                                Spacer(modifier = Modifier.width(Dimens.SpacingXs))
                                Text(
                                    text = stringResource(R.string.current_month),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = PositiveBannerTextDark
                                )
                            }
                        }
                    }
                }

                IconButton(
                    onClick = { calendarMonthOffset += 1 },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("next_month_btn")
                ) {
                    Box(
                        modifier = Modifier
                            .size(Dimens.AvatarBadgeSize)
                            .clip(RoundedCornerShape(Dimens.RadiusLg))
                            .background(SurfaceVariantLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = stringResource(R.string.cd_next_month),
                            tint = OnBackgroundLight,
                            modifier = Modifier.size(Dimens.IconMd)
                        )
                    }
                }
            }
        }

        // 2. Primary KPI Highlights Grid (4 Cards in 2x2 Grid)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingMd)
        ) {
            MonthlyKpiCard(
                title = stringResource(R.string.total_revenue),
                value = "${MoneyFormat.toPlainDecimal(MoneyFormat.toMinorUnits(totalMonthRevenue, summaryDigits), summaryDigits)} $currency",
                subtitle = stringResource(R.string.sales_shifts_count_fmt, dailyDataList.sumOf { it.transactionsCount }),
                icon = Icons.Default.Paid,
                accentColor = PosCashGreenFill,
                modifier = Modifier.weight(1f).cardRise(1)
            )
            MonthlyKpiCard(
                title = stringResource(R.string.daily_average),
                value = "${MoneyFormat.toPlainDecimal(MoneyFormat.toMinorUnits(dailyAverageRevenue, summaryDigits), summaryDigits)} $currency",
                subtitle = stringResource(R.string.across_active_days_fmt, actualActiveDays),
                icon = Icons.Default.TrendingUp,
                accentColor = PosCardBlue,
                modifier = Modifier.weight(1f).cardRise(1)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingMd)
        ) {
            MonthlyKpiCard(
                title = stringResource(R.string.peak_sales_day),
                value = if (peakDay != null && peakDay.totalRevenue > 0) "${MoneyFormat.toPlainDecimal(MoneyFormat.toMinorUnits(peakDay.totalRevenue, summaryDigits), summaryDigits)} $currency" else "${MoneyFormat.zeroPlaceholder(summaryDigits)} $currency",
                subtitle = if (peakDay != null && peakDay.totalRevenue > 0) "${peakDay.dateString} (${peakDay.dayName})" else stringResource(R.string.no_data_available),
                icon = Icons.Default.Star,
                accentColor = ChartAmber,
                modifier = Modifier.weight(1f).cardRise(2)
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
                accentColor = ChartIndigo,
                modifier = Modifier.weight(1f).cardRise(2)
            )
        }

        // 3. Chart Hero Card (10dp radius, 12dp padding, depth card)
        Surface(
            shape = RoundedCornerShape(Dimens.CardCornerRadius),
            color = Color.Transparent,
            modifier = Modifier
                .fillMaxWidth()
                .cardRise(3)
                .depthCard(shape = RoundedCornerShape(Dimens.CardCornerRadius))
        ) {
            Column(modifier = Modifier.padding(Dimens.CardPadding)) {
                // Chart Title & Toggle Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = Dimens.SpacingSm)) {
                        Text(
                            text = stringResource(R.string.monthly_revenue_trend),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = OnBackgroundLight
                            )
                        )
                        Text(
                            text = stringResource(R.string.interactive_curve_with_tooltips),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondaryLight
                            )
                        )
                    }

                    // Chart Mode Selector (Area Curve vs Bar Chart)
                    Surface(
                        shape = RoundedCornerShape(Dimens.RadiusMd),
                        color = Color.Transparent,
                        modifier = Modifier.depthInset(shape = RoundedCornerShape(Dimens.RadiusMd))
                    ) {
                        Row(modifier = Modifier.padding(Dimens.SpacingXxs)) {
                            IconButton(
                                onClick = { selectedChartType = ChartType.AREA_TREND },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("chart_mode_area")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(Dimens.IconTile)
                                        .clip(RoundedCornerShape(Dimens.RadiusSm))
                                        .background(if (selectedChartType == ChartType.AREA_TREND) PureWhite else Color.Transparent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ShowChart,
                                        contentDescription = stringResource(R.string.cd_area_trend),
                                        tint = if (selectedChartType == ChartType.AREA_TREND) PrimaryBlue else TextSecondaryLight,
                                        modifier = Modifier.size(Dimens.IconSm)
                                    )
                                }
                            }
                            IconButton(
                                onClick = { selectedChartType = ChartType.BAR_CHART },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("chart_mode_bar")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(Dimens.IconTile)
                                        .clip(RoundedCornerShape(Dimens.RadiusSm))
                                        .background(if (selectedChartType == ChartType.BAR_CHART) PureWhite else Color.Transparent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.BarChart,
                                        contentDescription = stringResource(R.string.cd_bar_chart),
                                        tint = if (selectedChartType == ChartType.BAR_CHART) PrimaryBlue else TextSecondaryLight,
                                        modifier = Modifier.size(Dimens.IconSm)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Dimens.SpacingSm))

                // Stream Breakdown Toggles / Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingMd),
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

                Spacer(modifier = Modifier.height(Dimens.SpacingMd))

                val hasSalesData = dailyDataList.any { it.totalRevenue > 0 }

                // Interactive Chart Rendering Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimens.MonthlyChartHeight)
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
                                    .size(Dimens.PresetLogoCircleSize)
                                    .clip(CircleShape)
                                    .background(SurfaceVariantLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BarChart,
                                    contentDescription = null,
                                    tint = TextDisabledColor,
                                    modifier = Modifier.size(Dimens.IconLg)
                                )
                            }
                            Spacer(modifier = Modifier.height(Dimens.SpacingSm))
                            Text(
                                text = stringResource(R.string.no_data_available),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = TextSecondaryLight,
                                    fontWeight = FontWeight.Medium
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
                        Spacer(modifier = Modifier.height(Dimens.SpacingSm))
                        Surface(
                            shape = RoundedCornerShape(Dimens.RadiusLg),
                            color = OnBackgroundLight,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = Dimens.SpacingMd, vertical = Dimens.SpacingSm),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "${d.dateString} (${d.dayName})",
                                        fontWeight = FontWeight.Bold,
                                        color = PureWhite,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = stringResource(R.string.day_x_of_y_fmt, d.dayNumber, daysInMonth),
                                        color = TextDisabledColor,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingMd),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (showShiftBreakdown && d.shiftSales > 0) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "${stringResource(R.string.shift_2)}: ${MoneyFormat.toPlainDecimal(MoneyFormat.toMinorUnits(d.shiftSales, summaryDigits), summaryDigits)}",
                                                color = AccentGold,
                                                style = MaterialTheme.typography.bodyMedium.asMoneyTabular(),
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(Dimens.RadiusMd),
                                        color = PrimaryBlue.copy(alpha = 0.25f),
                                        border = androidx.compose.foundation.BorderStroke(Dimens.FlatBorderWidth, PrimaryBlue.copy(alpha = 0.6f))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = Dimens.SpacingSm, vertical = Dimens.SpacingXs),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = stringResource(R.string.total_revenue_1),
                                                color = PrimaryLight,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = "${MoneyFormat.toPlainDecimal(MoneyFormat.toMinorUnits(d.totalRevenue, summaryDigits), summaryDigits)} $currency",
                                                color = PureWhite,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.titleMedium.asMoneyTabular()
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
            shape = RoundedCornerShape(Dimens.CardCornerRadius),
            color = Color.Transparent,
            modifier = Modifier
                .fillMaxWidth()
                .cardRise(4)
                .depthCard(shape = RoundedCornerShape(Dimens.CardCornerRadius))
        ) {
            Column(modifier = Modifier.padding(Dimens.CardPadding)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(Dimens.IconTile)
                            .clip(CircleShape)
                            .background(PrimaryBlue.copy(alpha = 0.10f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(Dimens.IconMd)
                        )
                    }
                    Spacer(modifier = Modifier.width(Dimens.SpacingSm))
                    Column {
                        Text(
                            text = stringResource(R.string.auto_monthly_financial_breakdown_fmt, currentMonthDisplayName),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = OnBackgroundLight
                            )
                        )
                        Text(
                            text = stringResource(R.string.auto_calculated_financial_summary),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryLight
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Dimens.SpacingMd))

                // Row 1: Cash Inflow
                MonthlyFinancialRowItem(
                    icon = Icons.Default.Payments,
                    iconTint = PosCashGreenFill,
                    label = stringResource(R.string.total_cash_in),
                    detail = stringResource(R.string.bank_mada_fmt, MoneyFormat.toPlainDecimal(MoneyFormat.toMinorUnits(monthTotalBankIn, summaryDigits), summaryDigits), currency),
                    amount = "${MoneyFormat.toPlainDecimal(MoneyFormat.toMinorUnits(monthTotalCashIn, summaryDigits), summaryDigits)} $currency",
                    amountColor = PosCashGreen
                )
                HorizontalDivider(color = OutlineVariantLight, thickness = Dimens.RowDividerWidth)

                // Row 2: Digital / Bank Card
                MonthlyFinancialRowItem(
                    icon = Icons.Default.CreditCard,
                    iconTint = PosCardBlue,
                    label = stringResource(R.string.mada_payments) + " / " + stringResource(R.string.digital_wallet),
                    detail = stringResource(R.string.monthly_mada_digital_settlements),
                    amount = "${MoneyFormat.toPlainDecimal(MoneyFormat.toMinorUnits(monthTotalBankIn, summaryDigits), summaryDigits)} $currency",
                    amountColor = PosCardBlue
                )
                HorizontalDivider(color = OutlineVariantLight, thickness = Dimens.RowDividerWidth)

                // Row 3: Due Credit Issued & Collected
                MonthlyFinancialRowItem(
                    icon = Icons.Default.AccountBalanceWallet,
                    iconTint = PosDueOrangeFill,
                    label = stringResource(R.string.total_issued_due),
                    detail = stringResource(R.string.due_collected_fmt, MoneyFormat.toPlainDecimal(MoneyFormat.toMinorUnits(monthTotalCollectedDue, summaryDigits), summaryDigits), currency),
                    amount = "${MoneyFormat.toPlainDecimal(MoneyFormat.toMinorUnits(monthTotalIssuedDue, summaryDigits), summaryDigits)} $currency",
                    amountColor = PosDueOrange
                )
                HorizontalDivider(color = OutlineVariantLight, thickness = Dimens.RowDividerWidth)

                // Row 4: Expenses
                MonthlyFinancialRowItem(
                    icon = Icons.Default.ReceiptLong,
                    iconTint = PosExpenseRedFill,
                    label = stringResource(R.string.total_expenses),
                    detail = stringResource(R.string.operational_costs),
                    amount = "${MoneyFormat.toPlainDecimal(MoneyFormat.toMinorUnits(monthTotalExpenses, summaryDigits), summaryDigits)} $currency",
                    amountColor = PosExpenseRed
                )
                HorizontalDivider(color = OutlineVariantLight, thickness = Dimens.RowDividerWidth)

                // Row 5: Product Purchases
                MonthlyFinancialRowItem(
                    icon = Icons.Default.ShoppingBag,
                    iconTint = ChartPurple,
                    label = stringResource(R.string.product_purchases),
                    detail = stringResource(R.string.inventory_investment),
                    amount = "${MoneyFormat.toPlainDecimal(MoneyFormat.toMinorUnits(monthTotalPurchases, summaryDigits), summaryDigits)} $currency",
                    amountColor = ChartPurple
                )
                HorizontalDivider(color = OutlineVariantLight, thickness = Dimens.RowDividerWidth)

                // Row 6: Staff Advances
                MonthlyFinancialRowItem(
                    icon = Icons.Default.People,
                    iconTint = ChartAmber,
                    label = stringResource(R.string.staff_advances_fmt, MoneyFormat.toPlainDecimal(MoneyFormat.toMinorUnits(monthTotalStaffAdvances, summaryDigits), summaryDigits), currency).substringBefore(":"),
                    detail = stringResource(R.string.monthly_salary_advance_payouts),
                    amount = "${MoneyFormat.toPlainDecimal(MoneyFormat.toMinorUnits(monthTotalStaffAdvances, summaryDigits), summaryDigits)} $currency",
                    amountColor = WarningOrange
                )

                Spacer(modifier = Modifier.height(Dimens.SpacingMd))

                // Net Monthly Cashflow Card
                val isNetPositive = monthNetCashflow >= 0
                Surface(
                    shape = RoundedCornerShape(Dimens.RadiusLg),
                    color = if (isNetPositive) PositiveBannerSurface else NegativeBannerSurface,
                    border = androidx.compose.foundation.BorderStroke(
                        Dimens.FlatBorderWidth,
                        if (isNetPositive) PositiveBannerBorder else NegativeBannerBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = Dimens.MinTouchTarget)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Dimens.SpacingMd, vertical = Dimens.SpacingSm),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.net_monthly_financial_cashflow),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isNetPositive) PositiveBannerText else NegativeBannerText
                            )
                            Text(
                                text = stringResource(R.string.net_cashflow_formula),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isNetPositive) PositiveBannerText.copy(alpha = 0.85f) else NegativeBannerText.copy(alpha = 0.85f)
                            )
                        }
                        Spacer(modifier = Modifier.width(Dimens.SpacingSm))
                        Text(
                            text = "${MoneyFormat.toPlainDecimal(MoneyFormat.toMinorUnits(monthNetCashflow, summaryDigits), summaryDigits)} $currency",
                            style = MaterialTheme.typography.headlineSmall.asMoneyTabular(),
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isNetPositive) PositiveBannerText else NegativeBannerText
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
            .heightIn(min = Dimens.FinancialRowMinHeight)
            .padding(vertical = Dimens.SpacingXs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(Dimens.FinancialIconCircleSize)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(Dimens.IconXs)
                )
            }
            Spacer(modifier = Modifier.width(Dimens.SpacingSm))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = OnBackgroundLight
                )
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryLight
                )
            }
        }

        Text(
            text = amount,
            style = MaterialTheme.typography.titleMedium.asMoneyTabular(),
            fontWeight = FontWeight.Bold,
            color = amountColor
        )
    }
}
