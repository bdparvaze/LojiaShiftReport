package com.lojia.shiftreport

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojia.shiftreport.data.Employee
import com.lojia.shiftreport.data.ShiftReport
import com.lojia.shiftreport.data.ShiftSession
import com.lojia.shiftreport.report.HomeSalesPeriod
import com.lojia.shiftreport.report.HomeVarianceStatus
import com.lojia.shiftreport.report.ShiftFormState
import com.lojia.shiftreport.report.buildEmployerAdvancePeriodReport
import com.lojia.shiftreport.report.buildHomeDashboardSummary
import com.lojia.shiftreport.report.buildHomeLiveDraftInput
import com.lojia.shiftreport.report.parseEmployerAdvances
import com.lojia.shiftreport.ui.theme.ChartBarInactiveEnd
import com.lojia.shiftreport.ui.theme.ChartBarInactiveStart
import com.lojia.shiftreport.ui.theme.DepthMotionTokens
import com.lojia.shiftreport.ui.theme.DepthTokens
import com.lojia.shiftreport.ui.theme.DepthTypeTokens
import com.lojia.shiftreport.ui.theme.Dimens
import com.lojia.shiftreport.ui.theme.HomeGreetingTextColor
import com.lojia.shiftreport.ui.theme.LiveBadgeGradientEnd
import com.lojia.shiftreport.ui.theme.LiveBadgeGradientStart
import com.lojia.shiftreport.ui.theme.SegmentAmberEnd
import com.lojia.shiftreport.ui.theme.SegmentAmberStart
import com.lojia.shiftreport.ui.theme.SegmentTealEnd
import com.lojia.shiftreport.ui.theme.SegmentTealStart
import com.lojia.shiftreport.ui.theme.TileAmberEnd
import com.lojia.shiftreport.ui.theme.TileBlueEnd
import com.lojia.shiftreport.ui.theme.TileGreenEnd
import com.lojia.shiftreport.ui.theme.TileMintEnd
import com.lojia.shiftreport.ui.theme.TileRoseEnd
import com.lojia.shiftreport.ui.theme.shouldAnimate
import com.lojia.shiftreport.util.MoneyFormat
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale

class Stage27AHomeDashboardUnitTest {

    private fun resolveAppModuleDir(): File {
        val candidates = listOf(File("."), File("app"), File(".."), File("../app"))
        return candidates.firstOrNull { dir ->
            File(dir, "src/main/java/com/lojia/shiftreport/report/HomeDashboardScreen.kt").isFile
        } ?: error("Unable to locate app module directory from ${File(".").absolutePath}")
    }

    private fun millisAt(year: Int, month: Int, day: Int, hour: Int, minute: Int, zone: ZoneId): Long {
        return LocalDate.of(year, month, day)
            .atTime(hour, minute)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
    }

    @Test
    fun stage27A_emptyDatabaseReturnsZerosAndClosedShift() {
        val zone = ZoneId.of("Asia/Riyadh")
        val nowMillis = millisAt(2025, 5, 11, 14, 0, zone)

        val summary = buildHomeDashboardSummary(
            reports = emptyList(),
            activeSession = null,
            role = "ADMIN",
            nowMillis = nowMillis,
            zone = zone,
            locale = Locale.US
        )

        assertFalse(summary.isShiftOpen)
        assertFalse(summary.hasUnsavedDraft)
        assertEquals(0L, summary.todaySales.netSalesMinor)
        assertEquals(0L, summary.thisWeekSales.netSalesMinor)
        assertEquals(0L, summary.thisMonthSales.netSalesMinor)
        assertEquals(0L, summary.expectedDrawerCashMinor)
        assertFalse(summary.hasLastShiftReport)
        assertEquals(HomeVarianceStatus.NONE, summary.lastShiftVarianceStatus)
        assertEquals(0L, summary.outstandingDuesMinor)
        assertEquals(0, summary.openReceiptsCount)
        assertEquals(0L, summary.advancesThisMonthTotalMinor)
        assertEquals(0L, summary.expensesTodayMinor)
        assertEquals(0, summary.expensesTodayCount)
        assertTrue(summary.showOwnerOnlyParts)
        assertEquals(7, summary.last7Days.size)
        assertTrue(summary.last7Days.all { it.netSalesMinor == 0L })
        assertTrue(summary.last7Days.last().isToday)
    }

    @Test
    fun stage27A_todayWeekMonthWindowsRespectTwoWeekFieldsLocalesAndNonUtcZones() {
        // Sunday 2025-05-11:
        // - Locale.US (week starts Sunday): week contains ONLY May 11.
        // - Locale.GERMANY (week starts Monday): week contains May 5..11.
        val zone = ZoneId.of("America/New_York")
        val nowMillis = millisAt(2025, 5, 11, 15, 30, zone)

        val todayReport = ShiftReport(
            id = 1,
            cashierName = "A",
            dateInMillis = millisAt(2025, 5, 11, 9, 0, zone),
            grossCash = 100_00L,
            madaPayments = 50_00L,
            digitalWallet = 25_00L
        )
        val fridayReport = ShiftReport(
            id = 2,
            cashierName = "A",
            dateInMillis = millisAt(2025, 5, 9, 20, 0, zone),
            grossCash = 200_00L
        )
        val earlyMayReport = ShiftReport(
            id = 3,
            cashierName = "A",
            dateInMillis = millisAt(2025, 5, 2, 12, 0, zone),
            grossCash = 400_00L
        )
        val aprilBoundaryBeforeMidnight = ShiftReport(
            id = 4,
            cashierName = "A",
            // 2025-04-30 23:30 America/New_York is already May 1 03:30 UTC! Must be excluded from NY May.
            dateInMillis = millisAt(2025, 4, 30, 23, 30, zone),
            grossCash = 900_00L
        )

        val reports = listOf(todayReport, fridayReport, earlyMayReport, aprilBoundaryBeforeMidnight)

        val usSummary = buildHomeDashboardSummary(
            reports = reports,
            activeSession = null,
            nowMillis = nowMillis,
            zone = zone,
            locale = Locale.US
        )
        assertEquals(175_00L, usSummary.todaySales.netSalesMinor)
        assertEquals(175_00L, usSummary.thisWeekSales.netSalesMinor)
        assertEquals(775_00L, usSummary.thisMonthSales.netSalesMinor)

        val deSummary = buildHomeDashboardSummary(
            reports = reports,
            activeSession = null,
            nowMillis = nowMillis,
            zone = zone,
            locale = Locale.GERMANY
        )
        assertEquals(175_00L, deSummary.todaySales.netSalesMinor)
        assertEquals(375_00L, deSummary.thisWeekSales.netSalesMinor)
        assertEquals(775_00L, deSummary.thisMonthSales.netSalesMinor)

        // Exact UTC comparison checks timezone boundary sensitivity cleanly
        val utcSummary = buildHomeDashboardSummary(
            reports = reports,
            activeSession = null,
            nowMillis = nowMillis,
            zone = ZoneOffset.UTC,
            locale = Locale.US
        )
        // April 30 23:30 NY is May 1 03:30 UTC, so it counts in UTC May
        assertEquals(1675_00L, utcSummary.thisMonthSales.netSalesMinor)
    }

    @Test
    fun stage27A_multiDecimalCurrenciesJpyUsdKwdPreserveLongMinorUnits() {
        val zone = ZoneOffset.UTC
        val nowMillis = millisAt(2025, 6, 10, 12, 0, zone)

        // KWD (3 decimals): cash 12.345 + bank 2.005 + wallet 0.650 = 15.000 KWD (15000 fils)
        val kwdDraft = buildHomeLiveDraftInput(
            ShiftFormState(
                cashier = "KWD Cashier",
                dateMillis = nowMillis,
                startingCashInput = "10.000",
                cashReceipts = "12.345",
                madaPayments = "2.005",
                digitalWalletInput = "0.650",
                totalExpenses = "1.125"
            ),
            currencyCode = "KWD"
        )
        assertEquals(12_345L, kwdDraft.cashSalesMinor)
        assertEquals(2_005L, kwdDraft.bankSalesMinor)
        assertEquals(650L, kwdDraft.walletSalesMinor)
        assertEquals(15_000L, kwdDraft.netSalesMinor)
        assertEquals(21_220L, kwdDraft.expectedCashMinor)
        assertEquals(
            kwdDraft.cashSalesMinor + kwdDraft.bankSalesMinor + kwdDraft.walletSalesMinor,
            kwdDraft.netSalesMinor
        )
        assertTrue(MoneyFormat.formatMinor(kwdDraft.netSalesMinor, "KWD", Locale.US).contains("15.000"))

        // JPY (0 decimals): cash 1500 + bank 2500 = 4000 JPY
        val jpyDraft = buildHomeLiveDraftInput(
            ShiftFormState(
                cashier = "JPY Cashier",
                dateMillis = nowMillis,
                cashReceipts = "1500",
                madaPayments = "2500"
            ),
            currencyCode = "JPY"
        )
        assertEquals(1500L, jpyDraft.cashSalesMinor)
        assertEquals(2500L, jpyDraft.bankSalesMinor)
        assertEquals(4000L, jpyDraft.netSalesMinor)
        val formattedJpy = MoneyFormat.formatMinor(jpyDraft.netSalesMinor, "JPY", Locale.JAPAN)
        assertFalse(formattedJpy.contains("."))

        // USD (2 decimals): net sales with discount & returns
        val usdSummary = buildHomeDashboardSummary(
            reports = listOf(
                ShiftReport(
                    id = 1,
                    cashierName = "USD",
                    dateInMillis = nowMillis,
                    grossCash = 100_25L,
                    madaPayments = 50_50L,
                    digitalWallet = 24_25L,
                    totalDiscounts = 5_00L,
                    salesReturns = 10_00L
                )
            ),
            activeSession = null,
            nowMillis = nowMillis,
            zone = zone,
            locale = Locale.US
        )
        assertEquals(175_00L, usdSummary.todaySales.grossSalesMinor)
        assertEquals(160_00L, usdSummary.todaySales.netSalesMinor)
        assertEquals(
            usdSummary.salesFor(HomeSalesPeriod.TODAY).cashMinor +
                usdSummary.salesFor(HomeSalesPeriod.TODAY).bankMinor +
                usdSummary.salesFor(HomeSalesPeriod.TODAY).walletMinor - 15_00L,
            usdSummary.salesFor(HomeSalesPeriod.TODAY).netSalesMinor
        )
    }

    @Test
    fun stage27A_roleGatingAdminVsCashierAndLast7DaysOrder() {
        val zone = ZoneId.of("Asia/Riyadh")
        val nowMillis = millisAt(2025, 7, 15, 18, 0, zone)

        val reports = listOf(
            // 8 days ago (must be excluded from last 7 days)
            ShiftReport(id = 1, cashierName = "A", dateInMillis = millisAt(2025, 7, 7, 12, 0, zone), grossCash = 999_00L),
            // 6 days ago (index 0)
            ShiftReport(id = 2, cashierName = "A", dateInMillis = millisAt(2025, 7, 9, 12, 0, zone), grossCash = 120_00L),
            // Today (index 6)
            ShiftReport(id = 3, cashierName = "A", dateInMillis = millisAt(2025, 7, 15, 10, 0, zone), grossCash = 300_00L)
        )
        val openSession = ShiftSession(
            id = 9,
            cashierName = "Nadia",
            shiftName = "Night",
            status = "OPEN",
            openedAt = nowMillis,
            startingCash = 100_00L,
            cashSales = 50_00L,
            cardSales = 25_00L,
            expectedCash = 150_00L
        )

        val adminSummary = buildHomeDashboardSummary(
            reports = reports,
            activeSession = openSession,
            role = "ADMIN",
            nowMillis = nowMillis,
            zone = zone,
            locale = Locale.US
        )
        assertTrue(adminSummary.isShiftOpen)
        assertTrue(adminSummary.showOwnerOnlyParts)
        assertEquals(7, adminSummary.last7Days.size)
        assertEquals(LocalDate.of(2025, 7, 9), adminSummary.last7Days.first().date)
        assertEquals(LocalDate.of(2025, 7, 15), adminSummary.last7Days.last().date)
        assertEquals(120_00L, adminSummary.last7Days[0].netSalesMinor)
        assertEquals(0L, adminSummary.last7Days[1].netSalesMinor)
        // Today combines closed report (300_00) + open session live (50_00 + 25_00) = 375_00
        assertEquals(375_00L, adminSummary.last7Days[6].netSalesMinor)
        assertTrue(adminSummary.last7Days[6].isToday)
        assertEquals(150_00L, adminSummary.expectedDrawerCashMinor)

        val cashierSummary = buildHomeDashboardSummary(
            reports = reports,
            activeSession = openSession,
            role = "CASHIER",
            nowMillis = nowMillis,
            zone = zone,
            locale = Locale.US
        )
        assertFalse(cashierSummary.showOwnerOnlyParts)
        assertTrue(cashierSummary.last7Days.isEmpty())
        assertEquals(375_00L, cashierSummary.todaySales.netSalesMinor)
    }

    @Test
    fun stage27A_varianceDuesAdvancesAndExpensesMatchAppSemantics() {
        val zone = ZoneOffset.UTC
        val nowMillis = millisAt(2025, 8, 20, 14, 0, zone)

        val dueJson = JSONArray().apply {
            put(JSONObject().put("receiptNo", "101").put("amount", 100_00L))
            put(JSONObject().put("receiptNo", "102").put("amount", 50_00L))
        }.toString()
        val prevDueJson = JSONArray().apply {
            put(JSONObject().put("receiptNo", "101").put("amount", 100_00L).put("paymentMode", "CASH"))
            put(JSONObject().put("receiptNo", "102").put("amount", 20_00L).put("paymentMode", "BANK"))
        }.toString()
        val advancesCurrentJson = JSONArray().apply {
            put(JSONObject().put("staffName", "Ali").put("amount", 40_00L).put("paymentMode", "CASH").put("employeeId", "e1"))
            put(JSONObject().put("staffName", "Sara").put("amount", 60_00L).put("paymentMode", "BANK").put("employeeId", "e2"))
        }.toString()
        val advancesLastMonthJson = JSONArray().apply {
            put(JSONObject().put("staffName", "Ali").put("amount", 99_00L).put("paymentMode", "CASH").put("employeeId", "e1"))
        }.toString()
        val paidOutJson = JSONArray().apply {
            put(JSONObject().put("itemName", "Ice").put("quantity", 2.0).put("unitPrice", 5_00L).put("totalAmount", 10_00L).put("paidVia", "CASH"))
        }.toString()

        val priorMonthReport = ShiftReport(
            id = 1,
            cashierName = "Old",
            dateInMillis = millisAt(2025, 7, 15, 10, 0, zone),
            staffAdvancesJson = advancesLastMonthJson,
            totalExpenses = 50_00L
        )
        // Latest report: expected = 100 opening + 200 cash + 100 dueCash - (25 expenses + 40 advCash + 10 itemCash) = 325_00
        // closingCash = 335_00 -> OVER +10_00
        val latestReport = ShiftReport(
            id = 2,
            cashierName = "Tariq",
            dateInMillis = millisAt(2025, 8, 20, 11, 0, zone),
            openingCash = 100_00L,
            grossCash = 200_00L,
            closingCash = 335_00L,
            totalExpenses = 25_00L,
            dueCreditEntriesJson = dueJson,
            previousDueCollectionsJson = prevDueJson,
            staffAdvancesJson = advancesCurrentJson,
            purchasedItemsJson = paidOutJson
        )
        val reports = listOf(priorMonthReport, latestReport)
        val employees = listOf(
            Employee(id = "e1", name = "Ali", monthlySalaryMinor = 1000_00L),
            Employee(id = "e2", name = "Sara", monthlySalaryMinor = 1000_00L)
        )
        val advances = parseEmployerAdvances(reports)
        val expectedAdvanceReport = buildEmployerAdvancePeriodReport(
            advances = advances,
            employees = employees,
            nowMillis = nowMillis,
            zone = zone
        )

        val summaryOver = buildHomeDashboardSummary(
            reports = reports,
            activeSession = null,
            advances = advances,
            employees = employees,
            nowMillis = nowMillis,
            zone = zone,
            locale = Locale.US
        )
        assertEquals(30_00L, summaryOver.outstandingDuesMinor)
        assertEquals(1, summaryOver.openReceiptsCount)
        assertEquals(expectedAdvanceReport.totalAdvancesMinor, summaryOver.advancesThisMonthTotalMinor)
        assertEquals(expectedAdvanceReport.cashTotalMinor, summaryOver.advancesThisMonthCashMinor)
        assertEquals(expectedAdvanceReport.bankTotalMinor, summaryOver.advancesThisMonthBankMinor)
        assertEquals(100_00L, summaryOver.advancesThisMonthTotalMinor)
        assertEquals(35_00L, summaryOver.expensesTodayMinor)
        assertEquals(2, summaryOver.expensesTodayCount)
        assertEquals(HomeVarianceStatus.OVER, summaryOver.lastShiftVarianceStatus)
        assertEquals(10_00L, summaryOver.lastShiftVarianceMinor)

        val shortSummary = buildHomeDashboardSummary(
            reports = listOf(latestReport.copy(closingCash = 320_00L)),
            activeSession = null,
            nowMillis = nowMillis,
            zone = zone,
            locale = Locale.US
        )
        assertEquals(HomeVarianceStatus.SHORT, shortSummary.lastShiftVarianceStatus)
        assertEquals(-5_00L, shortSummary.lastShiftVarianceMinor)

        val balancedSummary = buildHomeDashboardSummary(
            reports = listOf(latestReport.copy(closingCash = 325_00L)),
            activeSession = null,
            nowMillis = nowMillis,
            zone = zone,
            locale = Locale.US
        )
        assertEquals(HomeVarianceStatus.BALANCED, balancedSummary.lastShiftVarianceStatus)
        assertEquals(0L, balancedSummary.lastShiftVarianceMinor)
    }

    @Test
    fun stage27A_designTokensAndArchitecturalGuardsMatchSpec() {
        assertEquals(Color(0xFFDCE6FF), HomeGreetingTextColor)
        assertEquals(Color(0xFFE3FBEC), LiveBadgeGradientStart)
        assertEquals(Color(0xFFC6F3DA), LiveBadgeGradientEnd)
        assertEquals(Color(0xFF19B3A3), SegmentTealStart)
        assertEquals(Color(0xFF0E9384), SegmentTealEnd)
        assertEquals(Color(0xFFFDB022), SegmentAmberStart)
        assertEquals(Color(0xFFF79009), SegmentAmberEnd)
        assertEquals(Color(0xFFD1FADF), TileGreenEnd)
        assertEquals(Color(0xFFD0DDFF), TileBlueEnd)
        assertEquals(Color(0xFFFDE9B0), TileAmberEnd)
        assertEquals(Color(0xFFC3F2D6), TileMintEnd)
        assertEquals(Color(0xFFFBD3CF), TileRoseEnd)
        assertEquals(Color(0xFFD3E0FF), ChartBarInactiveStart)
        assertEquals(Color(0xFFB3C9FF), ChartBarInactiveEnd)

        assertEquals(22.dp, Dimens.HomeHeaderOverlapOffset)
        assertEquals(36.dp, Dimens.HomeHeaderBottomPadding)
        assertEquals(14.dp, Dimens.HomeSectionGap)
        assertEquals(10.dp, Dimens.HomeKpiGridGap)
        assertEquals(46.dp, Dimens.HomeQuickActionTileSize)
        assertEquals(15.dp, Dimens.HomeQuickActionTileRadius)
        assertEquals(72.dp, Dimens.HomeDraftBannerMinHeight)
        assertEquals(132.dp, Dimens.HomeChartHeight)
        assertEquals(24.dp, Dimens.HomeChartBarWidth)
        assertEquals(8.dp, Dimens.HomeChartBarRadius)
        assertEquals(6.dp, Dimens.HomeChartBarMinHeight)

        assertEquals(34.sp, DepthTypeTokens.HomeHeroNetSalesSize)
        assertEquals(20.sp, DepthTypeTokens.HomeKpiValueSize)
        assertEquals(650, DepthMotionTokens.HERO_COUNTUP_DURATION_MS)
        assertEquals(550, DepthMotionTokens.CHART_BAR_GROW_DURATION_MS)
        assertEquals(45, DepthMotionTokens.CHART_BAR_STAGGER_MS)
        assertFalse(shouldAnimate(0f))
        assertTrue(shouldAnimate(1f))

        val appDir = resolveAppModuleDir()
        val offendingFiles = File(appDir, "src/main/java").walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { !it.invariantSeparatorsPath.contains("/ui/theme/") }
            .filter { it.readText().contains("Color(0x") }
            .map { it.relativeTo(appDir).path }
            .toList()
        assertTrue("Zero Color(0x outside ui/theme/, found: $offendingFiles", offendingFiles.isEmpty())

        val homeScreenSrc = File(appDir, "src/main/java/com/lojia/shiftreport/report/HomeDashboardScreen.kt").readText()
        assertTrue(
            "HomeLast7DaysChartCard must be omitted cleanly when !summary.showOwnerOnlyParts",
            homeScreenSrc.contains("if (summary.showOwnerOnlyParts)") &&
                homeScreenSrc.contains("rememberShouldAnimate()")
        )

        val mainActivitySrc = File(appDir, "src/main/java/com/lojia/shiftreport/MainActivity.kt").readText()
        assertTrue(
            "MainActivity must start on Home destination after login / Quick PIN",
            mainActivitySrc.contains("mutableStateOf<AppNavState>(AppNavState.ShiftReportState.Home)")
        )
        assertTrue(
            "MainActivity must preserve Stage 26F-1 background guards",
            mainActivitySrc.contains("reportPageBackground") &&
                mainActivitySrc.contains("MaterialTheme.colorScheme.background")
        )
        assertTrue(
            "Home tokens check",
            DepthTokens.LIVE_BADGE_SHADOW_ALPHA > 0f
        )
    }
}
