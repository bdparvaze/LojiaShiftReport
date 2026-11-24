package com.lojia.shiftreport

import com.lojia.shiftreport.data.ShiftReport
import com.lojia.shiftreport.data.ShiftSession
import com.lojia.shiftreport.report.ShiftFormState
import com.lojia.shiftreport.report.buildHomeDashboardSummary
import com.lojia.shiftreport.report.buildHomeLiveDraftInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory

class Stage29CHomeShiftUnitTest {

    private fun appFile(relativePath: String): File {
        val direct = File(relativePath)
        return if (direct.exists()) direct else File("app", relativePath)
    }

    private fun parseStringValues(relativePath: String): Map<String, String> {
        val file = appFile(relativePath)
        assertTrue("Expected file to exist: $relativePath", file.exists())
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val nodes = doc.getElementsByTagName("string")
        val map = LinkedHashMap<String, String>(nodes.length)
        for (i in 0 until nodes.length) {
            val node = nodes.item(i)
            val name = node.attributes?.getNamedItem("name")?.nodeValue ?: continue
            map[name] = node.textContent.orEmpty()
        }
        return map
    }

    @Test
    fun homeDashboardScreenHasNoShiftStatusCardDialogsOrShiftSessionReferences() {
        val homeScreenSrc = appFile("src/main/java/com/lojia/shiftreport/report/HomeDashboardScreen.kt").readText()
        val forbiddenInScreen = listOf(
            "home_shift_status_card",
            "home_open_shift_btn",
            "home_close_shift_btn",
            "home_continue_report_btn",
            "HomeShiftStatusCard",
            "HomeOpenShiftDialog",
            "HomeCloseShiftDialog",
            "ShiftSession",
            "activeShiftSession",
            "closeShiftWithZReport"
        )
        forbiddenInScreen.forEach { token ->
            assertFalse(
                "HomeDashboardScreen.kt must not reference $token",
                homeScreenSrc.contains(token)
            )
        }

        val homeSummarySrc = appFile("src/main/java/com/lojia/shiftreport/report/HomeDashboardSummary.kt").readText()
        assertFalse(
            "HomeDashboardSummary.kt must not reference ShiftSession",
            homeSummarySrc.contains("ShiftSession")
        )

        val homeDialogsSrc = appFile("src/main/java/com/lojia/shiftreport/report/HomeDashboardDialogs.kt").readText()
        assertFalse(homeDialogsSrc.contains("HomeOpenShiftDialog"))
        assertFalse(homeDialogsSrc.contains("HomeCloseShiftDialog"))
        assertFalse(homeDialogsSrc.contains("ShiftSession"))
    }

    @Test
    fun buildHomeDashboardSummaryHasNoSessionParamAndMatchesRegardlessOfSessionData() {
        val methods = Class.forName("com.lojia.shiftreport.report.HomeDashboardSummaryKt")
            .declaredMethods
            .filter { it.name == "buildHomeDashboardSummary" }
        assertTrue("Expected buildHomeDashboardSummary method to exist", methods.isNotEmpty())
        methods.forEach { method ->
            assertTrue(
                "buildHomeDashboardSummary must not accept any ShiftSession parameter",
                method.parameterTypes.none { it == ShiftSession::class.java }
            )
        }

        val zone = ZoneOffset.UTC
        val nowMillis = LocalDate.of(2025, 9, 10)
            .atTime(15, 0)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()

        val savedReport = ShiftReport(
            id = 10,
            cashierName = "Sara",
            dateInMillis = nowMillis - 3_600_000L,
            openingCash = 100_00L,
            grossCash = 200_00L,
            madaPayments = 150_00L,
            digitalWallet = 50_00L,
            totalExpenses = 20_00L,
            closingCash = 280_00L
        )

        val first = buildHomeDashboardSummary(
            reports = listOf(savedReport),
            nowMillis = nowMillis,
            zone = zone,
            locale = Locale.US
        )
        val second = buildHomeDashboardSummary(
            reports = listOf(savedReport),
            nowMillis = nowMillis,
            zone = zone,
            locale = Locale.US
        )
        assertEquals(first, second)
        assertEquals(400_00L, first.todaySales.netSalesMinor)
        assertEquals(280_00L, first.expectedDrawerCashMinor)
        assertEquals(20_00L, first.expensesTodayMinor)
    }

    @Test
    fun draftEntryIsCountedExactlyOnceAlongsideSavedReports() {
        val zone = ZoneOffset.UTC
        val nowMillis = LocalDate.of(2025, 9, 12)
            .atTime(18, 0)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()

        val savedReport = ShiftReport(
            id = 1,
            cashierName = "Ali",
            dateInMillis = nowMillis - 7_200_000L,
            openingCash = 50_00L,
            grossCash = 100_00L,
            madaPayments = 50_00L,
            totalExpenses = 10_00L,
            closingCash = 140_00L
        )

        val liveDraft = buildHomeLiveDraftInput(
            formState = ShiftFormState(
                cashier = "Nadia",
                dateMillis = nowMillis,
                startingCashInput = "100.00",
                cashReceipts = "40.00",
                madaPayments = "30.00",
                digitalWalletInput = "10.00",
                totalExpenses = "5.00"
            ),
            currencyCode = "USD"
        )

        val summary = buildHomeDashboardSummary(
            reports = listOf(savedReport),
            role = "ADMIN",
            nowMillis = nowMillis,
            zone = zone,
            locale = Locale.US,
            liveDraft = liveDraft
        )

        // Saved report (100+50 = 150) + Draft (40+30+10 = 80) = 230.00 (counted once each)
        assertEquals(140_00L, summary.todaySales.cashMinor)
        assertEquals(80_00L, summary.todaySales.bankMinor)
        assertEquals(10_00L, summary.todaySales.walletMinor)
        assertEquals(230_00L, summary.todaySales.netSalesMinor)
        assertEquals(230_00L, summary.thisWeekSales.netSalesMinor)
        assertEquals(230_00L, summary.thisMonthSales.netSalesMinor)
        assertEquals(230_00L, summary.last7Days.last().netSalesMinor)

        // Expected drawer cash uses draft expected cash (100 + 40 - 5 = 135.00)
        assertEquals(135_00L, summary.expectedDrawerCashMinor)

        // Expenses today: saved (10.00, 1 entry) + draft (5.00, 1 entry) = 15.00, 2 entries
        assertEquals(15_00L, summary.expensesTodayMinor)
        assertEquals(2, summary.expensesTodayCount)
        assertTrue(summary.hasUnsavedDraft)
    }

    @Test
    fun reportViewModelAndReportDaoShiftSessionApisRemainIntact() {
        val vmSrc = appFile("src/main/java/com/lojia/shiftreport/report/ReportViewModel.kt").readText()
        assertTrue(vmSrc.contains("fun openShift("))
        assertTrue(vmSrc.contains("fun closeShift("))
        assertTrue(vmSrc.contains("fun closeShiftWithZReport("))
        assertTrue(vmSrc.contains("fun addCashMovement("))

        val daoSrc = appFile("src/main/java/com/lojia/shiftreport/data/ReportDao.kt").readText()
        val requiredDaoMethods = listOf(
            "fun getAllShiftSessions(",
            "suspend fun getAllShiftSessionsList(",
            "suspend fun insertShiftSessions(",
            "fun getActiveShiftSession(",
            "suspend fun getActiveShiftSessionOnce(",
            "suspend fun insertShiftSession(",
            "suspend fun updateShiftSession(",
            "fun observeShiftSessionCount(",
            "suspend fun getShiftSessionCount("
        )
        requiredDaoMethods.forEach { methodSig ->
            assertTrue("ReportDao.kt must preserve $methodSig", daoSrc.contains(methodSig))
        }
    }

    @Test
    fun stringKeysEqualAcrossLocalesAndRemovedHomeShiftKeysAbsent() {
        val enMap = parseStringValues("src/main/res/values/strings.xml")
        val bnMap = parseStringValues("src/main/res/values-bn/strings.xml")
        val arMap = parseStringValues("src/main/res/values-ar/strings.xml")

        assertEquals("values vs values-bn key count mismatch", enMap.size, bnMap.size)
        assertEquals("values vs values-ar key count mismatch", enMap.size, arMap.size)
        assertEquals("values vs values-bn key set mismatch", enMap.keys, bnMap.keys)
        assertEquals("values vs values-ar key set mismatch", enMap.keys, arMap.keys)

        val removedKeys = listOf(
            "home_shift_status",
            "home_shift_open_badge",
            "home_shift_closed_badge",
            "home_no_open_shift",
            "home_no_open_shift_desc",
            "home_open_shift_detail_fmt",
            "home_report_in_progress",
            "home_continue_report",
            "home_open_shift_dialog_title",
            "home_open_shift_float_label",
            "home_close_shift_dialog_title",
            "home_close_shift_actual_label",
            "home_close_shift_notes_label",
            "home_cd_shift_card"
        )
        removedKeys.forEach { key ->
            assertFalse("Removed key $key must not exist in values", enMap.containsKey(key))
            assertFalse("Removed key $key must not exist in values-bn", bnMap.containsKey(key))
            assertFalse("Removed key $key must not exist in values-ar", arMap.containsKey(key))
        }

        assertTrue("home_live_badge must remain in values", enMap.containsKey("home_live_badge"))
    }

    @Test
    fun zeroColorLiteralOutsideUiTheme() {
        val mainJavaDir = appFile("src/main/java")
        val violations = mutableListOf<String>()
        mainJavaDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { file ->
                val relPath = file.relativeTo(mainJavaDir).path.replace('\\', '/')
                if (relPath.startsWith("com/lojia/shiftreport/ui/theme/")) return@forEach
                file.readLines().forEachIndexed { idx, line ->
                    if (line.contains("Color(0x", ignoreCase = false)) {
                        violations.add("$relPath:${idx + 1}")
                    }
                }
            }

        assertTrue(
            "Found hardcoded Color(0x outside ui/theme:\n${violations.joinToString("\n")}",
            violations.isEmpty()
        )
    }
}
