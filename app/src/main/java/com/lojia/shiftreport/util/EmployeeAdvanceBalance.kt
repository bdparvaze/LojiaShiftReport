package com.lojia.shiftreport.util

import com.lojia.shiftreport.report.EmployerAdvanceItem
import java.time.Instant
import java.time.ZoneId

data class EmployeeAdvanceSummary(
    val salaryMinor: Long,
    val advancesMinor: Long,
    val balanceMinor: Long
)

fun currentMonthRange(nowMillis: Long, zone: ZoneId): LongRange {
    val firstDayOfMonth = Instant.ofEpochMilli(nowMillis)
        .atZone(zone)
        .toLocalDate()
        .withDayOfMonth(1)
    val startMillis = firstDayOfMonth
        .atStartOfDay(zone)
        .toInstant()
        .toEpochMilli()
    val endMillis = firstDayOfMonth
        .plusMonths(1)
        .atStartOfDay(zone)
        .toInstant()
        .toEpochMilli() - 1L
    return startMillis..endMillis
}

fun advancesInMonthMinor(
    items: List<EmployerAdvanceItem>,
    employeeName: String,
    range: LongRange
): Long {
    val targetName = normalizeRestoreName(employeeName)
    if (targetName.isEmpty()) return 0L
    var total = 0L
    for (item in items) {
        if (item.dateInMillis in range && normalizeRestoreName(item.staffName) == targetName) {
            total += item.amountMinor
        }
    }
    return total
}

fun advancesInMonthMinor(
    items: List<EmployerAdvanceItem>,
    employeeId: String,
    employeeName: String,
    range: LongRange
): Long {
    val targetId = employeeId.trim()
    val targetName = normalizeRestoreName(employeeName)
    var total = 0L
    for (item in items) {
        if (item.dateInMillis !in range) continue
        val itemId = item.employeeId?.trim().orEmpty()
        val matched = if (itemId.isNotEmpty()) {
            targetId.isNotEmpty() && itemId == targetId
        } else {
            targetName.isNotEmpty() && normalizeRestoreName(item.staffName) == targetName
        }
        if (matched) {
            total += item.amountMinor
        }
    }
    return total
}

fun balanceMinor(salaryMinor: Long, advancesMinor: Long): Long = salaryMinor - advancesMinor
