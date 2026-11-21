package com.lojia.shiftreport

import com.lojia.shiftreport.data.Employee
import com.lojia.shiftreport.report.EmployerAdvanceItem
import com.lojia.shiftreport.report.buildEmployerAdvanceStatement
import com.lojia.shiftreport.report.buildStatementPdfFileName
import com.lojia.shiftreport.report.customDayRange
import com.lojia.shiftreport.report.isSingleCalendarMonth
import com.lojia.shiftreport.report.previousMonthRange
import com.lojia.shiftreport.report.sanitizePdfFileNameSegment
import com.lojia.shiftreport.util.MoneyFormat
import com.lojia.shiftreport.util.currentMonthRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.Currency
import java.util.Locale

class Stage25BEmployerAdvanceStatementUnitTest {

    private val testZone = ZoneOffset.UTC
    private val refNowMillis = ZonedDateTime.of(2026, 11, 15, 10, 0, 0, 0, testZone)
        .toInstant()
        .toEpochMilli()

    @Test
    fun allTime_includesEveryMonthAndSumsTotals() {
        val empA = Employee(id = "e-1", name = "Staff One", monthlySalaryMinor = 100_000L)
        val empB = Employee(id = "e-2", name = "Staff Two", monthlySalaryMinor = 150_000L)

        val janMs = ZonedDateTime.of(2026, 1, 10, 9, 0, 0, 0, testZone).toInstant().toEpochMilli()
        val octMs = ZonedDateTime.of(2026, 10, 20, 14, 0, 0, 0, testZone).toInstant().toEpochMilli()
        val novMs = ZonedDateTime.of(2026, 11, 5, 18, 0, 0, 0, testZone).toInstant().toEpochMilli()

        val advances = listOf(
            EmployerAdvanceItem("Staff One", 10_000L, "CASH", janMs, "Cashier A", "e-1"),
            EmployerAdvanceItem("Staff One", 25_000L, "BANK", octMs, "Cashier B", "e-1"),
            EmployerAdvanceItem("Staff Two", 40_000L, "CASH", novMs, "Cashier A", "e-2"),
            EmployerAdvanceItem("Legacy", 5_000L, "BANK", novMs + 1_000L, "Cashier C", null)
        )

        val statement = buildEmployerAdvanceStatement(
            advances = advances,
            employees = listOf(empA, empB),
            range = null,
            zone = testZone
        )

        assertNull(statement.range)
        assertFalse(statement.isSingleCalendarMonth)
        assertEquals(80_000L, statement.totalAdvancesMinor)
        assertEquals(50_000L, statement.totalCashMinor)
        assertEquals(30_000L, statement.totalBankMinor)
        assertEquals(4, statement.totalEntryCount)
        assertEquals(2, statement.employees.size)
        assertNotNull(statement.otherBucket)
    }

    @Test
    fun customRange_isInclusiveAtBothEndsAndExcludesOutsideByOneMs() {
        val emp = Employee(id = "e-1", name = "Staff One", monthlySalaryMinor = 200_000L)
        val startDate = LocalDate.of(2026, 11, 10)
        val endDate = LocalDate.of(2026, 11, 12)
        val range = customDayRange(startDate, endDate, testZone)!!

        val justBefore = range.first - 1L
        val exactStart = range.first
        val exactEnd = range.last
        val justAfter = range.last + 1L

        val advances = listOf(
            EmployerAdvanceItem("Staff One", 1_000L, "CASH", justBefore, "Cashier", "e-1"),
            EmployerAdvanceItem("Staff One", 2_500L, "CASH", exactStart, "Cashier", "e-1"),
            EmployerAdvanceItem("Staff One", 3_500L, "BANK", exactEnd, "Cashier", "e-1"),
            EmployerAdvanceItem("Staff One", 4_000L, "CASH", justAfter, "Cashier", "e-1")
        )

        val statement = buildEmployerAdvanceStatement(
            advances = advances,
            employees = listOf(emp),
            range = range,
            zone = testZone
        )

        assertEquals(6_000L, statement.totalAdvancesMinor)
        assertEquals(2_500L, statement.totalCashMinor)
        assertEquals(3_500L, statement.totalBankMinor)
        assertEquals(2, statement.totalEntryCount)
    }

    @Test
    fun singleCalendarMonthDetection_distinguishesExactMonthFromPartialOrMultiMonth() {
        val thisMonth = currentMonthRange(refNowMillis, testZone)
        val lastMonth = previousMonthRange(refNowMillis, testZone)
        val exactCustomMonth = customDayRange(
            LocalDate.of(2026, 2, 1),
            LocalDate.of(2026, 2, 28),
            testZone
        )
        val partialMonth = customDayRange(
            LocalDate.of(2026, 11, 2),
            LocalDate.of(2026, 11, 30),
            testZone
        )
        val twoMonths = customDayRange(
            LocalDate.of(2026, 10, 1),
            LocalDate.of(2026, 11, 30),
            testZone
        )

        assertTrue(isSingleCalendarMonth(thisMonth, testZone))
        assertTrue(isSingleCalendarMonth(lastMonth, testZone))
        assertTrue(isSingleCalendarMonth(exactCustomMonth, testZone))
        assertFalse(isSingleCalendarMonth(partialMonth, testZone))
        assertFalse(isSingleCalendarMonth(twoMonths, testZone))
        assertFalse(isSingleCalendarMonth(null, testZone))
    }

    @Test
    fun salaryRemainingAndExcess_presentOnlyForSingleCalendarMonth() {
        val empWithin = Employee(id = "e-1", name = "Staff One", monthlySalaryMinor = 100_000L)
        val empOver = Employee(id = "e-2", name = "Staff Two", monthlySalaryMinor = 50_000L)
        val novMs = ZonedDateTime.of(2026, 11, 10, 12, 0, 0, 0, testZone).toInstant().toEpochMilli()

        val advances = listOf(
            EmployerAdvanceItem("Staff One", 30_000L, "CASH", novMs, "Cashier", "e-1"),
            EmployerAdvanceItem("Staff Two", 65_000L, "BANK", novMs, "Cashier", "e-2")
        )

        val singleMonthRange = currentMonthRange(refNowMillis, testZone)
        val singleStatement = buildEmployerAdvanceStatement(
            advances = advances,
            employees = listOf(empWithin, empOver),
            range = singleMonthRange,
            zone = testZone
        )

        assertTrue(singleStatement.isSingleCalendarMonth)
        val secWithin = singleStatement.employees.first { it.employeeId == "e-1" }
        assertEquals(100_000L, secWithin.monthlySalaryMinor)
        assertEquals(30_000L, secWithin.advancesInMonthMinor)
        assertEquals(70_000L, secWithin.remainingMinor)
        assertEquals(0L, secWithin.excessMinor)

        val secOver = singleStatement.employees.first { it.employeeId == "e-2" }
        assertEquals(50_000L, secOver.monthlySalaryMinor)
        assertEquals(65_000L, secOver.advancesInMonthMinor)
        assertEquals(-15_000L, secOver.remainingMinor)
        assertEquals(15_000L, secOver.excessMinor)

        val allTimeStatement = buildEmployerAdvanceStatement(
            advances = advances,
            employees = listOf(empWithin, empOver),
            range = null,
            zone = testZone
        )
        assertFalse(allTimeStatement.isSingleCalendarMonth)
        allTimeStatement.employees.forEach { sec ->
            assertNull(sec.monthlySalaryMinor)
            assertNull(sec.advancesInMonthMinor)
            assertNull(sec.remainingMinor)
            assertNull(sec.excessMinor)
        }

        val partialStatement = buildEmployerAdvanceStatement(
            advances = advances,
            employees = listOf(empWithin, empOver),
            range = customDayRange(LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 20), testZone),
            zone = testZone
        )
        assertFalse(partialStatement.isSingleCalendarMonth)
        partialStatement.employees.forEach { sec ->
            assertNull(sec.monthlySalaryMinor)
            assertNull(sec.remainingMinor)
            assertNull(sec.excessMinor)
        }
    }

    @Test
    fun totalsConsistencyAndFormatting_workAcrossZeroTwoAndThreeDecimalCurrencies() {
        val available = Currency.getAvailableCurrencies()
        val currencies = listOf(
            available.first { it.defaultFractionDigits == 0 }.currencyCode,
            available.first { it.defaultFractionDigits == 2 }.currencyCode,
            available.first { it.defaultFractionDigits == 3 }.currencyCode
        )

        val empA = Employee(id = "e-1", name = "Staff One", monthlySalaryMinor = 100_000L)
        val empB = Employee(id = "e-2", name = "Staff Two", monthlySalaryMinor = 100_000L)
        val advances = listOf(
            EmployerAdvanceItem("Staff One", 1_235L, "CASH", refNowMillis, "Cashier", "e-1"),
            EmployerAdvanceItem("Staff One", 2_500L, "BANK", refNowMillis + 1_000L, "Cashier", "e-1"),
            EmployerAdvanceItem("Staff Two", 3_333L, "CASH", refNowMillis + 2_000L, "Cashier", "e-2"),
            EmployerAdvanceItem("Unmatched", 932L, "BANK", refNowMillis + 3_000L, "Cashier", null)
        )

        val statement = buildEmployerAdvanceStatement(
            advances = advances,
            employees = listOf(empA, empB),
            range = currentMonthRange(refNowMillis, testZone),
            zone = testZone
        )

        val sumEmployees = statement.employees.sumOf { it.grandTotalMinor }
        val otherTotal = statement.otherBucket?.advancesTotalMinor ?: 0L
        assertEquals(statement.totalAdvancesMinor, sumEmployees + otherTotal)
        assertEquals(8_000L, statement.totalAdvancesMinor)
        assertEquals(4_568L, statement.totalCashMinor)
        assertEquals(3_432L, statement.totalBankMinor)

        currencies.forEach { code ->
            val formattedGrand = MoneyFormat.formatMinor(statement.totalAdvancesMinor, code, Locale.ROOT)
            assertTrue(formattedGrand.isNotBlank())
        }
        assertFalse(MoneyFormat.formatMinor(statement.totalAdvancesMinor, currencies[0], Locale.ROOT).contains("."))
        assertTrue(MoneyFormat.formatMinor(statement.totalAdvancesMinor, currencies[1], Locale.ROOT).contains("80.00"))
        assertTrue(MoneyFormat.formatMinor(statement.totalAdvancesMinor, currencies[2], Locale.ROOT).contains("8.000"))
    }

    @Test
    fun employeeIdMatchesStrictlyFirst_andUnmatchedGoesToOtherBucket() {
        val emp1 = Employee(id = "id-1", name = "Same Name", monthlySalaryMinor = 100_000L)
        val emp2 = Employee(id = "id-2", name = "Same Name", monthlySalaryMinor = 100_000L)

        val advances = listOf(
            EmployerAdvanceItem("Same Name", 10_000L, "CASH", refNowMillis, "Cashier A", "id-1"),
            EmployerAdvanceItem("Same Name", 20_000L, "BANK", refNowMillis + 1_000L, "Cashier B", "id-2"),
            EmployerAdvanceItem("Same Name", 5_000L, "CASH", refNowMillis + 2_000L, "Cashier C", "missing-id")
        )

        val statement = buildEmployerAdvanceStatement(
            advances = advances,
            employees = listOf(emp1, emp2),
            range = null,
            zone = testZone
        )

        assertEquals(2, statement.employees.size)
        assertEquals(10_000L, statement.employees.first { it.employeeId == "id-1" }.grandTotalMinor)
        assertEquals(20_000L, statement.employees.first { it.employeeId == "id-2" }.grandTotalMinor)
        assertEquals(5_000L, statement.otherBucket?.advancesTotalMinor)
    }

    @Test
    fun customDayRange_rejectsStartAfterEnd() {
        val invalid = customDayRange(
            startDate = LocalDate.of(2026, 11, 20),
            endDate = LocalDate.of(2026, 11, 10),
            zone = testZone
        )
        assertNull(invalid)
    }

    @Test
    fun emptyPeriod_returnsZeroTotalsWithoutCrash() {
        val emp = Employee(id = "e-1", name = "Staff One", monthlySalaryMinor = 100_000L)
        val statement = buildEmployerAdvanceStatement(
            advances = emptyList(),
            employees = listOf(emp),
            range = currentMonthRange(refNowMillis, testZone),
            zone = testZone
        )

        assertTrue(statement.employees.isEmpty())
        assertNull(statement.otherBucket)
        assertEquals(0L, statement.totalAdvancesMinor)
        assertEquals(0L, statement.totalCashMinor)
        assertEquals(0L, statement.totalBankMinor)
        assertEquals(0, statement.totalEntryCount)
    }

    @Test
    fun fileNameBuilder_sanitizesForbiddenCharsAndFormatsIsoDates() {
        val cleaned = sanitizePdfFileNameSegment("Store: /A\\B*?\"<>| 01")
        assertEquals("Store_A_B_01", cleaned)
        assertFalse(cleaned.any { it in setOf('\\', '/', ':', '*', '?', '"', '<', '>', '|', ' ') })

        val range = customDayRange(
            LocalDate.of(2026, 11, 1),
            LocalDate.of(2026, 11, 30),
            testZone
        )
        val rangeName = buildStatementPdfFileName(
            statementWord = "Statement",
            businessName = "Shop/One:Branch*",
            range = range,
            nowMillis = refNowMillis,
            zone = testZone
        )
        assertEquals("Shop_One_Branch_Statement_2026-11-01_2026-11-30.pdf", rangeName)

        val allTimeName = buildStatementPdfFileName(
            statementWord = "Statement",
            businessName = null,
            range = null,
            nowMillis = refNowMillis,
            zone = testZone
        )
        assertEquals("Statement_2026-11-15.pdf", allTimeName)
    }

    @Test
    fun entriesWithinSections_areSortedChronologicallyAndNumberedFromOne() {
        val emp = Employee(id = "e-1", name = "Staff One", monthlySalaryMinor = 100_000L)
        val t1 = refNowMillis - 5_000L
        val t2 = refNowMillis
        val t3 = refNowMillis + 5_000L

        val advances = listOf(
            EmployerAdvanceItem("Staff One", 3_000L, "BANK", t3, "Cashier Three", "e-1"),
            EmployerAdvanceItem("Staff One", 1_000L, "CASH", t1, "Cashier One", "e-1"),
            EmployerAdvanceItem("Staff One", 2_000L, "CASH", t2, "Cashier Two", "e-1"),
            EmployerAdvanceItem("Visitor B", 900L, "CASH", t3, "Cashier Three", null),
            EmployerAdvanceItem("Visitor A", 400L, "BANK", t1, "Cashier One", null)
        )

        val statement = buildEmployerAdvanceStatement(
            advances = advances,
            employees = listOf(emp),
            range = null,
            zone = testZone
        )

        val entries = statement.employees[0].entries
        assertEquals(listOf(1, 2, 3), entries.map { it.serialNumber })
        assertEquals(listOf(1_000L, 2_000L, 3_000L), entries.map { it.amountMinor })
        assertEquals(listOf("Cashier One", "Cashier Two", "Cashier Three"), entries.map { it.cashierName })

        val otherEntries = statement.otherBucket!!.history
        assertEquals(listOf(1, 2), otherEntries.map { it.serialNumber })
        assertEquals(listOf(400L, 900L), otherEntries.map { it.amountMinor })
    }
}
