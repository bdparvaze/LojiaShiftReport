package com.lojia.shiftreport.report

import com.lojia.shiftreport.data.Employee
import com.lojia.shiftreport.util.MoneyFormat
import com.lojia.shiftreport.util.balanceMinor
import com.lojia.shiftreport.util.currentMonthRange
import com.lojia.shiftreport.util.normalizeRestoreName
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

data class EmployerAdvanceHistoryEntry(
    val serialNumber: Int,
    val staffName: String,
    val amountMinor: Long,
    val paymentMode: String,
    val dateInMillis: Long,
    val cashierName: String,
    val employeeId: String? = null
)

data class EmployerEmployeeAdvanceProfile(
    val employeeId: String,
    val employeeName: String,
    val monthlySalaryMinor: Long,
    val advancesTotalMinor: Long,
    val cashTotalMinor: Long,
    val bankTotalMinor: Long,
    val remainingMinor: Long,
    val excessMinor: Long,
    val advanceCount: Int,
    val isActive: Boolean,
    val history: List<EmployerAdvanceHistoryEntry>
)

data class EmployerOtherAdvanceBucket(
    val advancesTotalMinor: Long,
    val cashTotalMinor: Long,
    val bankTotalMinor: Long,
    val advanceCount: Int,
    val history: List<EmployerAdvanceHistoryEntry>
)

data class EmployerAdvancePeriodReport(
    val periodRange: LongRange,
    val totalAdvancesMinor: Long,
    val cashTotalMinor: Long,
    val bankTotalMinor: Long,
    val advanceCount: Int,
    val profiles: List<EmployerEmployeeAdvanceProfile>,
    val otherBucket: EmployerOtherAdvanceBucket?
)

internal fun isCashAdvanceMode(paymentMode: String): Boolean =
    paymentMode.trim().equals("CASH", ignoreCase = true)

internal fun isBankAdvanceMode(paymentMode: String): Boolean =
    paymentMode.trim().equals("BANK", ignoreCase = true)

fun formatEmployerAdvancePeriodLabel(
    nowMillis: Long = System.currentTimeMillis(),
    locale: Locale = Locale.getDefault(),
    zone: ZoneId = ZoneId.systemDefault()
): String {
    val zoned = Instant.ofEpochMilli(nowMillis).atZone(zone)
    val monthName = zoned.month.getDisplayName(TextStyle.FULL_STANDALONE, locale)
    val yearFormatter = NumberFormat.getIntegerInstance(locale).apply {
        isGroupingUsed = false
    }
    return "$monthName ${yearFormatter.format(zoned.year)}"
}

private data class IndexedEmployerAdvance(
    val index: Int,
    val item: EmployerAdvanceItem
)

fun buildEmployerAdvancePeriodReport(
    advances: List<EmployerAdvanceItem>,
    employees: List<Employee>,
    nowMillis: Long = System.currentTimeMillis(),
    zone: ZoneId = ZoneId.systemDefault()
): EmployerAdvancePeriodReport {
    val periodRange = currentMonthRange(nowMillis, zone)

    val employeeById = LinkedHashMap<String, Employee>(employees.size)
    val employeeByNormalizedName = LinkedHashMap<String, Employee>(employees.size)
    val orderedEmployees = employees.sortedWith(
        compareByDescending<Employee> { it.isActive }
            .thenBy { it.name.lowercase(Locale.ROOT) }
            .thenBy { it.id }
    )
    for (employee in orderedEmployees) {
        val trimmedId = employee.id.trim()
        if (trimmedId.isNotEmpty()) {
            employeeById.putIfAbsent(trimmedId, employee)
        }
        val normName = normalizeRestoreName(employee.name)
        if (normName.isNotEmpty()) {
            employeeByNormalizedName.putIfAbsent(normName, employee)
        }
    }

    val periodAdvances = ArrayList<IndexedEmployerAdvance>()
    val advancesByEmployeeId = LinkedHashMap<String, MutableList<IndexedEmployerAdvance>>()
    val otherAdvances = ArrayList<IndexedEmployerAdvance>()

    advances.forEachIndexed { idx, item ->
        if (item.dateInMillis in periodRange && item.amountMinor > 0L) {
            val indexed = IndexedEmployerAdvance(idx, item)
            periodAdvances.add(indexed)

            val rawEmpId = item.employeeId?.trim().orEmpty()
            val matchedEmployee = if (rawEmpId.isNotEmpty()) {
                // When an explicit employeeId is present, match strictly by ID so same-name employees
                // or deleted IDs never silently merge into another employee.
                employeeById[rawEmpId]
            } else {
                val normName = normalizeRestoreName(item.staffName)
                if (normName.isNotEmpty()) employeeByNormalizedName[normName] else null
            }

            if (matchedEmployee != null) {
                advancesByEmployeeId
                    .getOrPut(matchedEmployee.id) { ArrayList() }
                    .add(indexed)
            } else {
                otherAdvances.add(indexed)
            }
        }
    }

    val totalAdvancesMinor = MoneyFormat.sumOfMinor(periodAdvances.map { it.item.amountMinor })
    val cashTotalMinor = MoneyFormat.sumOfMinor(
        periodAdvances.filter { isCashAdvanceMode(it.item.paymentMode) }.map { it.item.amountMinor }
    )
    val bankTotalMinor = MoneyFormat.sumOfMinor(
        periodAdvances.filter { isBankAdvanceMode(it.item.paymentMode) }.map { it.item.amountMinor }
    )

    val profiles = advancesByEmployeeId.entries.mapNotNull { (empId, entries) ->
        val emp = employeeById[empId] ?: return@mapNotNull null
        if (entries.isEmpty()) return@mapNotNull null

        val sortedHistory = entries
            .sortedWith(compareBy<IndexedEmployerAdvance> { it.item.dateInMillis }.thenBy { it.index })
            .mapIndexed { idx, indexed ->
                val item = indexed.item
                EmployerAdvanceHistoryEntry(
                    serialNumber = idx + 1,
                    staffName = item.staffName.ifBlank { emp.name },
                    amountMinor = item.amountMinor,
                    paymentMode = item.paymentMode,
                    dateInMillis = item.dateInMillis,
                    cashierName = item.cashierName,
                    employeeId = item.employeeId
                )
            }

        val empTotalMinor = MoneyFormat.sumOfMinor(entries.map { it.item.amountMinor })
        val empCashMinor = MoneyFormat.sumOfMinor(
            entries.filter { isCashAdvanceMode(it.item.paymentMode) }.map { it.item.amountMinor }
        )
        val empBankMinor = MoneyFormat.sumOfMinor(
            entries.filter { isBankAdvanceMode(it.item.paymentMode) }.map { it.item.amountMinor }
        )
        val signedRemaining = balanceMinor(emp.monthlySalaryMinor, empTotalMinor)
        val positiveExcess = if (signedRemaining < 0L) -signedRemaining else 0L

        EmployerEmployeeAdvanceProfile(
            employeeId = emp.id,
            employeeName = emp.name,
            monthlySalaryMinor = emp.monthlySalaryMinor,
            advancesTotalMinor = empTotalMinor,
            cashTotalMinor = empCashMinor,
            bankTotalMinor = empBankMinor,
            remainingMinor = signedRemaining,
            excessMinor = positiveExcess,
            advanceCount = sortedHistory.size,
            isActive = emp.isActive,
            history = sortedHistory
        )
    }.sortedWith(
        compareByDescending<EmployerEmployeeAdvanceProfile> { it.advancesTotalMinor }
            .thenBy { it.employeeName.lowercase(Locale.ROOT) }
            .thenBy { it.employeeId }
    )

    val otherBucket = if (otherAdvances.isNotEmpty()) {
        val sortedOtherHistory = otherAdvances
            .sortedWith(compareBy<IndexedEmployerAdvance> { it.item.dateInMillis }.thenBy { it.index })
            .mapIndexed { idx, indexed ->
                val item = indexed.item
                EmployerAdvanceHistoryEntry(
                    serialNumber = idx + 1,
                    staffName = item.staffName,
                    amountMinor = item.amountMinor,
                    paymentMode = item.paymentMode,
                    dateInMillis = item.dateInMillis,
                    cashierName = item.cashierName,
                    employeeId = item.employeeId
                )
            }
        val otherTotal = MoneyFormat.sumOfMinor(otherAdvances.map { it.item.amountMinor })
        val otherCash = MoneyFormat.sumOfMinor(
            otherAdvances.filter { isCashAdvanceMode(it.item.paymentMode) }.map { it.item.amountMinor }
        )
        val otherBank = MoneyFormat.sumOfMinor(
            otherAdvances.filter { isBankAdvanceMode(it.item.paymentMode) }.map { it.item.amountMinor }
        )
        EmployerOtherAdvanceBucket(
            advancesTotalMinor = otherTotal,
            cashTotalMinor = otherCash,
            bankTotalMinor = otherBank,
            advanceCount = sortedOtherHistory.size,
            history = sortedOtherHistory
        )
    } else {
        null
    }

    return EmployerAdvancePeriodReport(
        periodRange = periodRange,
        totalAdvancesMinor = totalAdvancesMinor,
        cashTotalMinor = cashTotalMinor,
        bankTotalMinor = bankTotalMinor,
        advanceCount = periodAdvances.size,
        profiles = profiles,
        otherBucket = otherBucket
    )
}
