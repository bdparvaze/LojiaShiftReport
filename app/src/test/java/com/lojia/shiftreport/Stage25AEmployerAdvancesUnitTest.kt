package com.lojia.shiftreport

import com.lojia.shiftreport.data.Employee
import com.lojia.shiftreport.data.ShiftReport
import com.lojia.shiftreport.data.StaffAdvanceItem
import com.lojia.shiftreport.report.EmployerAdvanceItem
import com.lojia.shiftreport.report.ShiftReportSerializer
import com.lojia.shiftreport.report.buildEmployerAdvancePeriodReport
import com.lojia.shiftreport.report.parseEmployerAdvances
import com.lojia.shiftreport.util.MoneyFormat
import com.lojia.shiftreport.util.advancesInMonthMinor
import com.lojia.shiftreport.util.currentMonthRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.Currency
import java.util.Locale

class Stage25AEmployerAdvancesUnitTest {

    private val testZone = ZoneOffset.UTC
    private val midMonthMillis = ZonedDateTime.of(2026, 11, 15, 10, 0, 0, 0, testZone)
        .toInstant()
        .toEpochMilli()

    @Test
    fun periodSummary_splitsCashAndBankTotalsAndTotalAdvances() {
        val empA = Employee(id = "e-1", name = "Person A", monthlySalaryMinor = 200_000L)
        val empB = Employee(id = "e-2", name = "Person B", monthlySalaryMinor = 150_000L)
        val advances = listOf(
            EmployerAdvanceItem("Person A", 25_000L, "CASH", midMonthMillis, "Cashier", "e-1"),
            EmployerAdvanceItem("Person A", 15_000L, "bank", midMonthMillis + 1_000L, "Cashier", "e-1"),
            EmployerAdvanceItem("Person B", 10_000L, " cash ", midMonthMillis + 2_000L, "Cashier", "e-2"),
            EmployerAdvanceItem("Walk-in", 5_000L, "BANK", midMonthMillis + 3_000L, "Cashier", null)
        )

        val report = buildEmployerAdvancePeriodReport(
            advances = advances,
            employees = listOf(empA, empB),
            nowMillis = midMonthMillis,
            zone = testZone
        )

        assertEquals(55_000L, report.totalAdvancesMinor)
        assertEquals(35_000L, report.cashTotalMinor)
        assertEquals(20_000L, report.bankTotalMinor)
        assertEquals(4, report.advanceCount)
    }

    @Test
    fun employeeProfile_computesSignedRemainingAndPositiveExcess() {
        val empWithinSalary = Employee(id = "e-1", name = "Person A", monthlySalaryMinor = 100_000L)
        val empExactSalary = Employee(id = "e-2", name = "Person B", monthlySalaryMinor = 80_000L)
        val empOverSalary = Employee(id = "e-3", name = "Person C", monthlySalaryMinor = 50_000L)

        val advances = listOf(
            EmployerAdvanceItem("Person A", 35_000L, "CASH", midMonthMillis, "Cashier", "e-1"),
            EmployerAdvanceItem("Person B", 80_000L, "BANK", midMonthMillis, "Cashier", "e-2"),
            EmployerAdvanceItem("Person C", 45_000L, "CASH", midMonthMillis, "Cashier", "e-3"),
            EmployerAdvanceItem("Person C", 20_000L, "BANK", midMonthMillis + 5_000L, "Cashier", "e-3")
        )

        val report = buildEmployerAdvancePeriodReport(
            advances = advances,
            employees = listOf(empWithinSalary, empExactSalary, empOverSalary),
            nowMillis = midMonthMillis,
            zone = testZone
        )

        val profileA = report.profiles.first { it.employeeId == "e-1" }
        assertEquals(100_000L, profileA.monthlySalaryMinor)
        assertEquals(35_000L, profileA.advancesTotalMinor)
        assertEquals(65_000L, profileA.remainingMinor)
        assertEquals(0L, profileA.excessMinor)

        val profileB = report.profiles.first { it.employeeId == "e-2" }
        assertEquals(0L, profileB.remainingMinor)
        assertEquals(0L, profileB.excessMinor)

        val profileC = report.profiles.first { it.employeeId == "e-3" }
        assertEquals(65_000L, profileC.advancesTotalMinor)
        assertEquals(-15_000L, profileC.remainingMinor)
        assertEquals(15_000L, profileC.excessMinor)
    }

    @Test
    fun periodBoundary_includesFirstAndLastMsAndExcludesOutsideMonth() {
        val emp = Employee(id = "e-1", name = "Person A", monthlySalaryMinor = 100_000L)
        val startOfMonth = ZonedDateTime.of(2026, 11, 1, 0, 0, 0, 0, testZone)
            .toInstant()
            .toEpochMilli()
        val lastMsOfPrevMonth = startOfMonth - 1L
        val startOfNextMonth = ZonedDateTime.of(2026, 12, 1, 0, 0, 0, 0, testZone)
            .toInstant()
            .toEpochMilli()
        val lastMsOfMonth = startOfNextMonth - 1L

        val advances = listOf(
            EmployerAdvanceItem("Person A", 1_000L, "CASH", lastMsOfPrevMonth, "Cashier", "e-1"),
            EmployerAdvanceItem("Person A", 2_500L, "CASH", startOfMonth, "Cashier", "e-1"),
            EmployerAdvanceItem("Person A", 3_500L, "BANK", lastMsOfMonth, "Cashier", "e-1"),
            EmployerAdvanceItem("Person A", 4_000L, "CASH", startOfNextMonth, "Cashier", "e-1")
        )

        val report = buildEmployerAdvancePeriodReport(
            advances = advances,
            employees = listOf(emp),
            nowMillis = midMonthMillis,
            zone = testZone
        )

        assertEquals(6_000L, report.totalAdvancesMinor)
        assertEquals(2_500L, report.cashTotalMinor)
        assertEquals(3_500L, report.bankTotalMinor)
        assertEquals(2, report.advanceCount)
        assertEquals(1, report.profiles.size)
        assertEquals(2, report.profiles[0].history.size)
    }

    @Test
    fun employeesWithZeroAdvancesInPeriod_areExcludedFromProfiles() {
        val prevMonthMs = ZonedDateTime.of(2026, 10, 20, 12, 0, 0, 0, testZone)
            .toInstant()
            .toEpochMilli()
        val empWithAdvance = Employee(id = "e-1", name = "Person A", monthlySalaryMinor = 120_000L)
        val empPrevMonthOnly = Employee(id = "e-2", name = "Person B", monthlySalaryMinor = 120_000L)
        val empNoAdvanceEver = Employee(id = "e-3", name = "Person C", monthlySalaryMinor = 120_000L)

        val advances = listOf(
            EmployerAdvanceItem("Person A", 10_000L, "CASH", midMonthMillis, "Cashier", "e-1"),
            EmployerAdvanceItem("Person B", 20_000L, "CASH", prevMonthMs, "Cashier", "e-2")
        )

        val report = buildEmployerAdvancePeriodReport(
            advances = advances,
            employees = listOf(empWithAdvance, empPrevMonthOnly, empNoAdvanceEver),
            nowMillis = midMonthMillis,
            zone = testZone
        )

        assertEquals(1, report.profiles.size)
        assertEquals("e-1", report.profiles[0].employeeId)
    }

    @Test
    fun employeeIdWinsOverName_keepsSameNameEmployeesSeparateAndDeletedIdGoesToOther() {
        val empOne = Employee(id = "id-one", name = "Duplicate Name", monthlySalaryMinor = 100_000L)
        val empTwo = Employee(id = "id-two", name = "Duplicate Name", monthlySalaryMinor = 200_000L)

        val advances = listOf(
            EmployerAdvanceItem("Duplicate Name", 10_000L, "CASH", midMonthMillis, "Cashier", "id-one"),
            EmployerAdvanceItem("Duplicate Name", 40_000L, "BANK", midMonthMillis + 1_000L, "Cashier", "id-two"),
            EmployerAdvanceItem("Duplicate Name", 5_000L, "CASH", midMonthMillis + 2_000L, "Cashier", "deleted-id")
        )

        val report = buildEmployerAdvancePeriodReport(
            advances = advances,
            employees = listOf(empOne, empTwo),
            nowMillis = midMonthMillis,
            zone = testZone
        )

        assertEquals(2, report.profiles.size)
        val p1 = report.profiles.first { it.employeeId == "id-one" }
        val p2 = report.profiles.first { it.employeeId == "id-two" }
        assertEquals(10_000L, p1.advancesTotalMinor)
        assertEquals(90_000L, p1.remainingMinor)
        assertEquals(40_000L, p2.advancesTotalMinor)
        assertEquals(160_000L, p2.remainingMinor)

        val other = report.otherBucket
        assertNotNull(other)
        assertEquals(5_000L, other?.advancesTotalMinor)
        assertEquals(1, other?.advanceCount)

        val range = currentMonthRange(midMonthMillis, testZone)
        assertEquals(10_000L, advancesInMonthMinor(advances, "id-one", "Duplicate Name", range))
        assertEquals(40_000L, advancesInMonthMinor(advances, "id-two", "Duplicate Name", range))
    }

    @Test
    fun unmatchedAdvance_goesToOtherBucketWithoutSalaryOrCrash() {
        val emp = Employee(id = "e-1", name = "Person A", monthlySalaryMinor = 100_000L)
        val advances = listOf(
            EmployerAdvanceItem("Unknown Staff", 12_500L, "CASH", midMonthMillis, "Cashier", null),
            EmployerAdvanceItem("   ", 7_500L, "BANK", midMonthMillis + 1_000L, "Cashier", null)
        )

        val report = buildEmployerAdvancePeriodReport(
            advances = advances,
            employees = listOf(emp),
            nowMillis = midMonthMillis,
            zone = testZone
        )

        assertTrue(report.profiles.isEmpty())
        assertEquals(20_000L, report.totalAdvancesMinor)
        val other = report.otherBucket
        assertNotNull(other)
        assertEquals(20_000L, other!!.advancesTotalMinor)
        assertEquals(12_500L, other.cashTotalMinor)
        assertEquals(7_500L, other.bankTotalMinor)
        assertEquals(2, other.history.size)
        assertEquals(1, other.history[0].serialNumber)
        assertEquals(2, other.history[1].serialNumber)
    }

    @Test
    fun historyEntries_areSortedChronologicallyAndNumberedFromOne_andLegacyNameMatches() {
        val emp = Employee(id = "e-1", name = "Person A", monthlySalaryMinor = 100_000L)
        val t1 = midMonthMillis - 10_000L
        val t2 = midMonthMillis
        val t3 = midMonthMillis + 10_000L

        val advances = listOf(
            EmployerAdvanceItem("Person A", 3_000L, "BANK", t3, "Cashier", "e-1"),
            EmployerAdvanceItem("  person a ", 1_000L, "CASH", t1, "Cashier", null),
            EmployerAdvanceItem("Person A", 2_000L, "CASH", t2, "Cashier", "e-1")
        )

        val report = buildEmployerAdvancePeriodReport(
            advances = advances,
            employees = listOf(emp),
            nowMillis = midMonthMillis,
            zone = testZone
        )

        assertNull(report.otherBucket)
        assertEquals(1, report.profiles.size)
        val history = report.profiles[0].history
        assertEquals(3, history.size)
        assertEquals(1, history[0].serialNumber)
        assertEquals(1_000L, history[0].amountMinor)
        assertEquals("CASH", history[0].paymentMode)
        assertEquals(2, history[1].serialNumber)
        assertEquals(2_000L, history[1].amountMinor)
        assertEquals(3, history[2].serialNumber)
        assertEquals(3_000L, history[2].amountMinor)
        assertEquals("BANK", history[2].paymentMode)
    }

    @Test
    fun moneyFormat_handlesZeroTwoAndThreeMinorUnitDigitsCorrectly() {
        val available = Currency.getAvailableCurrencies()
        val zeroDecimalCurr = available.first { it.defaultFractionDigits == 0 }.currencyCode
        val twoDecimalCurr = available.first { it.defaultFractionDigits == 2 }.currencyCode
        val threeDecimalCurr = available.first { it.defaultFractionDigits == 3 }.currencyCode

        assertEquals(0, MoneyFormat.minorUnitDigits(zeroDecimalCurr))
        assertEquals(2, MoneyFormat.minorUnitDigits(twoDecimalCurr))
        assertEquals(3, MoneyFormat.minorUnitDigits(threeDecimalCurr))

        val formatted0 = MoneyFormat.formatMinor(1500L, zeroDecimalCurr, Locale.ROOT)
        val formatted2 = MoneyFormat.formatMinor(1525L, twoDecimalCurr, Locale.ROOT)
        val formatted3 = MoneyFormat.formatMinor(1525L, threeDecimalCurr, Locale.ROOT)

        assertTrue(formatted0.contains("1,500") || formatted0.contains("1500"))
        assertTrue(!formatted0.contains("."))
        assertTrue(formatted2.contains("15.25"))
        assertTrue(formatted3.contains("1.525"))
    }

    @Test
    fun serializerAndParser_preserveOptionalEmployeeIdAndSupportLegacyEntries() {
        val items = listOf(
            StaffAdvanceItem(
                id = "a1",
                staffName = "Person A",
                amount = 12_345L,
                paymentMode = "CASH",
                employeeId = "emp-1"
            ),
            StaffAdvanceItem(
                id = "a2",
                staffName = "Legacy Person",
                amount = 5_000L,
                paymentMode = "BANK",
                employeeId = null
            )
        )

        val json = ShiftReportSerializer.serializeAdvances(items)
        val roundTripped = ShiftReportSerializer.deserializeAdvances(json)
        assertEquals(2, roundTripped.size)
        assertEquals("emp-1", roundTripped[0].employeeId)
        assertNull(roundTripped[1].employeeId)

        val parsed = parseEmployerAdvances(
            listOf(
                ShiftReport(
                    id = 1,
                    cashierName = "Cashier",
                    dateInMillis = midMonthMillis,
                    staffAdvancesJson = json
                )
            )
        )
        assertEquals(2, parsed.size)
        assertEquals("emp-1", parsed[0].employeeId)
        assertNull(parsed[1].employeeId)
    }
}
