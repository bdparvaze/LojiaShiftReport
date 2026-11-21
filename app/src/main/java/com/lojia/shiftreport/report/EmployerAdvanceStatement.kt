package com.lojia.shiftreport.report

import com.lojia.shiftreport.data.Employee
import com.lojia.shiftreport.util.MoneyFormat
import com.lojia.shiftreport.util.balanceMinor
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

enum class AdvanceStatementPeriodOption {
    THIS_MONTH,
    LAST_MONTH,
    ALL_TIME,
    CUSTOM_RANGE
}

data class EmployerAdvanceStatementEmployeeSection(
    val employeeId: String,
    val employeeName: String,
    val entryCount: Int,
    val cashTotalMinor: Long,
    val bankTotalMinor: Long,
    val grandTotalMinor: Long,
    val monthlySalaryMinor: Long?,
    val advancesInMonthMinor: Long?,
    val remainingMinor: Long?,
    val excessMinor: Long?,
    val entries: List<EmployerAdvanceHistoryEntry>
)

data class EmployerAdvanceStatement(
    val range: LongRange?,
    val isSingleCalendarMonth: Boolean,
    val employees: List<EmployerAdvanceStatementEmployeeSection>,
    val otherBucket: EmployerOtherAdvanceBucket?,
    val totalAdvancesMinor: Long,
    val totalCashMinor: Long,
    val totalBankMinor: Long,
    val totalEntryCount: Int
)

fun previousMonthRange(nowMillis: Long, zone: ZoneId): LongRange {
    val firstDayLastMonth = Instant.ofEpochMilli(nowMillis)
        .atZone(zone)
        .toLocalDate()
        .withDayOfMonth(1)
        .minusMonths(1)
    val startMillis = firstDayLastMonth
        .atStartOfDay(zone)
        .toInstant()
        .toEpochMilli()
    val endMillis = firstDayLastMonth
        .plusMonths(1)
        .atStartOfDay(zone)
        .toInstant()
        .toEpochMilli() - 1L
    return startMillis..endMillis
}

fun customDayRange(
    startDate: LocalDate,
    endDate: LocalDate,
    zone: ZoneId
): LongRange? {
    if (startDate.isAfter(endDate)) return null
    val startMillis = startDate.atStartOfDay(zone).toInstant().toEpochMilli()
    val endMillis = endDate.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1L
    return startMillis..endMillis
}

fun utcMillisToLocalDate(utcMillis: Long): LocalDate =
    Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate()

fun isSingleCalendarMonth(range: LongRange?, zone: ZoneId): Boolean {
    if (range == null || range.first > range.last) return false
    val firstDayOfStartMonth = Instant.ofEpochMilli(range.first)
        .atZone(zone)
        .toLocalDate()
        .withDayOfMonth(1)
    val expectedMonthStart = firstDayOfStartMonth
        .atStartOfDay(zone)
        .toInstant()
        .toEpochMilli()
    val expectedMonthEnd = firstDayOfStartMonth
        .plusMonths(1)
        .atStartOfDay(zone)
        .toInstant()
        .toEpochMilli() - 1L
    return range.first == expectedMonthStart && range.last == expectedMonthEnd
}

fun buildEmployerAdvanceStatement(
    advances: List<EmployerAdvanceItem>,
    employees: List<Employee>,
    range: LongRange?,
    zone: ZoneId = ZoneId.systemDefault()
): EmployerAdvanceStatement {
    val aggregated = aggregateEmployerAdvances(
        advances = advances,
        employees = employees,
        range = range
    )
    val singleMonth = isSingleCalendarMonth(range, zone)

    val employeeSections = aggregated.employeeGroups.map { group ->
        val empSalary = if (singleMonth) group.employee.monthlySalaryMinor else null
        val empAdvances = if (singleMonth) group.totalMinor else null
        val signedRemaining = if (singleMonth) {
            balanceMinor(group.employee.monthlySalaryMinor, group.totalMinor)
        } else {
            null
        }
        val positiveExcess = if (signedRemaining != null) {
            if (signedRemaining < 0L) -signedRemaining else 0L
        } else {
            null
        }
        EmployerAdvanceStatementEmployeeSection(
            employeeId = group.employee.id,
            employeeName = group.employee.name,
            entryCount = group.history.size,
            cashTotalMinor = group.cashMinor,
            bankTotalMinor = group.bankMinor,
            grandTotalMinor = group.totalMinor,
            monthlySalaryMinor = empSalary,
            advancesInMonthMinor = empAdvances,
            remainingMinor = signedRemaining,
            excessMinor = positiveExcess,
            entries = group.history
        )
    }

    val other = aggregated.otherBucket
    val sumEmployeeGrandTotals = MoneyFormat.sumOfMinor(employeeSections.map { it.grandTotalMinor })
    val totalAdvances = MoneyFormat.addMinor(sumEmployeeGrandTotals, other?.advancesTotalMinor ?: 0L)
    val sumEmployeeCash = MoneyFormat.sumOfMinor(employeeSections.map { it.cashTotalMinor })
    val totalCash = MoneyFormat.addMinor(sumEmployeeCash, other?.cashTotalMinor ?: 0L)
    val sumEmployeeBank = MoneyFormat.sumOfMinor(employeeSections.map { it.bankTotalMinor })
    val totalBank = MoneyFormat.addMinor(sumEmployeeBank, other?.bankTotalMinor ?: 0L)
    val totalEntries = employeeSections.sumOf { it.entryCount } + (other?.advanceCount ?: 0)

    return EmployerAdvanceStatement(
        range = range,
        isSingleCalendarMonth = singleMonth,
        employees = employeeSections,
        otherBucket = other,
        totalAdvancesMinor = totalAdvances,
        totalCashMinor = totalCash,
        totalBankMinor = totalBank,
        totalEntryCount = totalEntries
    )
}

private val UNSAFE_FILE_CHARS_REGEX = Regex("""[\\/:*?"<>|\u0000-\u001F\s]+""")
private val MULTI_UNDERSCORE_REGEX = Regex("""_+""")

fun sanitizePdfFileNameSegment(raw: String): String {
    return raw.trim()
        .replace(UNSAFE_FILE_CHARS_REGEX, "_")
        .replace(MULTI_UNDERSCORE_REGEX, "_")
        .trim('_', '.')
}

fun buildStatementPdfFileName(
    statementWord: String,
    businessName: String?,
    range: LongRange?,
    nowMillis: Long = System.currentTimeMillis(),
    zone: ZoneId = ZoneId.systemDefault()
): String {
    val parts = ArrayList<String>(4)
    val safeBiz = businessName?.let(::sanitizePdfFileNameSegment).orEmpty()
    if (safeBiz.isNotEmpty()) {
        parts.add(safeBiz)
    }
    val safeWord = sanitizePdfFileNameSegment(statementWord)
    if (safeWord.isNotEmpty()) {
        parts.add(safeWord)
    }
    val dateSegment = if (range != null) {
        val startDate = Instant.ofEpochMilli(range.first).atZone(zone).toLocalDate()
            .format(DateTimeFormatter.ISO_LOCAL_DATE)
        val endDate = Instant.ofEpochMilli(range.last).atZone(zone).toLocalDate()
            .format(DateTimeFormatter.ISO_LOCAL_DATE)
        if (startDate == endDate) startDate else "${startDate}_${endDate}"
    } else {
        Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
            .format(DateTimeFormatter.ISO_LOCAL_DATE)
    }
    parts.add(dateSegment)
    val base = parts.joinToString("_").take(120).trimEnd('_', '.')
    return "$base.pdf"
}
