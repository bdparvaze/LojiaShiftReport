package com.lojia.shiftreport.report

import com.lojia.shiftreport.data.Employee
import com.lojia.shiftreport.data.ShiftReport
import com.lojia.shiftreport.util.MoneyFormat
import com.lojia.shiftreport.util.MoneyInput
import com.lojia.shiftreport.util.classifyMoneyInput
import com.lojia.shiftreport.util.currentMonthRange
import com.lojia.shiftreport.util.summarizeDueMinor
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale

enum class HomeSalesPeriod {
    TODAY,
    THIS_WEEK,
    THIS_MONTH
}

enum class HomeVarianceStatus {
    NONE,
    BALANCED,
    OVER,
    SHORT
}

data class HomePeriodSalesSummary(
    val cashMinor: Long = 0L,
    val bankMinor: Long = 0L,
    val walletMinor: Long = 0L,
    val discountsMinor: Long = 0L,
    val returnsMinor: Long = 0L
) {
    val grossSalesMinor: Long
        get() = MoneyFormat.addMinor(cashMinor, bankMinor, walletMinor)

    val netSalesMinor: Long
        get() = MoneyFormat.calculateNetSalesMinor(grossSalesMinor, discountsMinor, returnsMinor)
}

data class HomeDaySalesPoint(
    val date: LocalDate,
    val weekdayLabel: String,
    val netSalesMinor: Long,
    val isToday: Boolean
)

data class HomeLiveDraftInput(
    val dateInMillis: Long = 0L,
    val openingCashMinor: Long = 0L,
    val cashSalesMinor: Long = 0L,
    val bankSalesMinor: Long = 0L,
    val walletSalesMinor: Long = 0L,
    val discountsMinor: Long = 0L,
    val returnsMinor: Long = 0L,
    val expensesMinor: Long = 0L,
    val staffAdvancesCashMinor: Long = 0L,
    val previousDueCashMinor: Long = 0L,
    val purchasedCashMinor: Long = 0L,
    val purchasedAllMinor: Long = 0L,
    val purchasedCount: Int = 0,
    val expectedCashMinor: Long = 0L,
    val hasAnyInput: Boolean = false
) {
    val grossSalesMinor: Long
        get() = MoneyFormat.addMinor(cashSalesMinor, bankSalesMinor, walletSalesMinor)

    val netSalesMinor: Long
        get() = MoneyFormat.calculateNetSalesMinor(grossSalesMinor, discountsMinor, returnsMinor)
}

data class HomeDashboardSummary(
    val todayLocalDate: LocalDate,
    val hasUnsavedDraft: Boolean,
    val todaySales: HomePeriodSalesSummary,
    val thisWeekSales: HomePeriodSalesSummary,
    val thisMonthSales: HomePeriodSalesSummary,
    val expectedDrawerCashMinor: Long,
    val hasLastShiftReport: Boolean,
    val lastShiftVarianceMinor: Long,
    val lastShiftVarianceStatus: HomeVarianceStatus,
    val outstandingDuesMinor: Long,
    val openReceiptsCount: Int,
    val advancesThisMonthTotalMinor: Long,
    val advancesThisMonthCashMinor: Long,
    val advancesThisMonthBankMinor: Long,
    val expensesTodayMinor: Long,
    val expensesTodayCount: Int,
    val showOwnerOnlyParts: Boolean,
    val last7Days: List<HomeDaySalesPoint>
) {
    fun salesFor(period: HomeSalesPeriod): HomePeriodSalesSummary = when (period) {
        HomeSalesPeriod.TODAY -> todaySales
        HomeSalesPeriod.THIS_WEEK -> thisWeekSales
        HomeSalesPeriod.THIS_MONTH -> thisMonthSales
    }
}

private const val LAST_DAYS_CHART_COUNT = 7
private const val ROLE_ADMIN = "ADMIN"

private fun minorFromClassified(input: MoneyInput): Long = when (input) {
    is MoneyInput.Valid -> input.minor
    MoneyInput.Empty, MoneyInput.Invalid -> 0L
}

fun buildHomeLiveDraftInput(
    formState: ShiftFormState,
    currencyCode: String
): HomeLiveDraftInput {
    val digits = MoneyFormat.minorUnitDigits(currencyCode)
    val openingMinor = minorFromClassified(classifyMoneyInput(formState.startingCashInput, digits))
    val cashMinor = minorFromClassified(classifyMoneyInput(formState.cashReceipts, digits))
    val bankMinor = minorFromClassified(classifyMoneyInput(formState.madaPayments, digits))
    val walletMinor = minorFromClassified(classifyMoneyInput(formState.digitalWalletInput, digits))
    val discountsMinor = minorFromClassified(classifyMoneyInput(formState.totalDiscountsInput, digits))
    val returnsMinor = minorFromClassified(classifyMoneyInput(formState.salesReturnsInput, digits))
    val expensesMinor = minorFromClassified(classifyMoneyInput(formState.totalExpenses, digits))
    val actualCashMinor = minorFromClassified(classifyMoneyInput(formState.actualCashCountInput, digits))

    val dueCashMinor = MoneyFormat.sumOfMinor(
        formState.previousDueCollections
            .filter { it.paymentMode.equals("CASH", ignoreCase = true) }
            .map { it.amount }
    )
    val staffCashMinor = MoneyFormat.sumOfMinor(
        formState.staffAdvances
            .filter { it.paymentMode.equals("CASH", ignoreCase = true) }
            .map { it.amount }
    )
    val purchasedCashMinor = MoneyFormat.sumOfMinor(
        formState.purchasedItems
            .filter { it.paidVia.equals("CASH", ignoreCase = true) }
            .map { it.totalAmount }
    )
    val purchasedAllMinor = MoneyFormat.sumOfMinor(formState.purchasedItems.map { it.totalAmount })
    val totalCashOutMinor = MoneyFormat.addMinor(expensesMinor, staffCashMinor, purchasedCashMinor)
    val expectedCashMinor = MoneyFormat.calculateExpectedCashMinor(
        startingCash = openingMinor,
        cashSales = cashMinor,
        payIn = dueCashMinor,
        payOut = totalCashOutMinor
    )

    val hasAnyInput = formState.cashier.isNotBlank() ||
        openingMinor > 0L ||
        cashMinor > 0L ||
        bankMinor > 0L ||
        walletMinor > 0L ||
        discountsMinor > 0L ||
        returnsMinor > 0L ||
        actualCashMinor > 0L ||
        expensesMinor > 0L ||
        formState.staffCount > 0 ||
        formState.muassel.isNotBlank() ||
        formState.outdoorMuassel.isNotBlank() ||
        formState.notesInput.isNotBlank() ||
        formState.dueCreditItems.isNotEmpty() ||
        formState.previousDueCollections.isNotEmpty() ||
        formState.staffAdvances.isNotEmpty() ||
        formState.unpaidBills.isNotEmpty() ||
        formState.purchasedItems.isNotEmpty()

    return HomeLiveDraftInput(
        dateInMillis = formState.dateMillis,
        openingCashMinor = openingMinor,
        cashSalesMinor = cashMinor,
        bankSalesMinor = bankMinor,
        walletSalesMinor = walletMinor,
        discountsMinor = discountsMinor,
        returnsMinor = returnsMinor,
        expensesMinor = expensesMinor,
        staffAdvancesCashMinor = staffCashMinor,
        previousDueCashMinor = dueCashMinor,
        purchasedCashMinor = purchasedCashMinor,
        purchasedAllMinor = purchasedAllMinor,
        purchasedCount = formState.purchasedItems.count { it.totalAmount > 0L },
        expectedCashMinor = expectedCashMinor,
        hasAnyInput = hasAnyInput
    )
}

fun buildHomeDashboardSummary(
    reports: List<ShiftReport>,
    advances: List<EmployerAdvanceItem> = parseEmployerAdvances(reports),
    employees: List<Employee> = emptyList(),
    role: String = ROLE_ADMIN,
    nowMillis: Long = System.currentTimeMillis(),
    zone: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault(),
    liveDraft: HomeLiveDraftInput? = null
): HomeDashboardSummary {
    val todayDate = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
    val todayStart = todayDate.atStartOfDay(zone).toInstant().toEpochMilli()
    val todayEnd = todayDate.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1L
    val todayRange = todayStart..todayEnd

    val firstDayOfWeek = WeekFields.of(locale).firstDayOfWeek
    val weekStartDate = todayDate.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
    val weekStart = weekStartDate.atStartOfDay(zone).toInstant().toEpochMilli()
    val weekEnd = weekStartDate.plusDays(7).atStartOfDay(zone).toInstant().toEpochMilli() - 1L
    val weekRange = weekStart..weekEnd

    val monthRange = currentMonthRange(nowMillis, zone)

    // In-progress live sales come solely from the Shift Report tab draft ("Updates as you enter").
    val inProgressTimestamp: Long?
    val inProgressCash: Long
    val inProgressBank: Long
    val inProgressWallet: Long
    val inProgressDiscounts: Long
    val inProgressReturns: Long
    if (liveDraft != null && liveDraft.hasAnyInput) {
        inProgressTimestamp = if (liveDraft.dateInMillis > 0L) liveDraft.dateInMillis else nowMillis
        inProgressCash = liveDraft.cashSalesMinor
        inProgressBank = liveDraft.bankSalesMinor
        inProgressWallet = liveDraft.walletSalesMinor
        inProgressDiscounts = liveDraft.discountsMinor
        inProgressReturns = liveDraft.returnsMinor
    } else {
        inProgressTimestamp = null
        inProgressCash = 0L
        inProgressBank = 0L
        inProgressWallet = 0L
        inProgressDiscounts = 0L
        inProgressReturns = 0L
    }

    fun summarizePeriod(range: LongRange): HomePeriodSalesSummary {
        var cash = 0L
        var bank = 0L
        var wallet = 0L
        var discounts = 0L
        var returns = 0L

        for (report in reports) {
            if (report.dateInMillis in range) {
                cash += report.grossCash
                bank += report.madaPayments
                wallet += report.digitalWallet
                discounts += report.totalDiscounts
                returns += report.salesReturns
            }
        }

        if (inProgressTimestamp != null && inProgressTimestamp in range) {
            cash += inProgressCash
            bank += inProgressBank
            wallet += inProgressWallet
            discounts += inProgressDiscounts
            returns += inProgressReturns
        }

        return HomePeriodSalesSummary(
            cashMinor = cash,
            bankMinor = bank,
            walletMinor = wallet,
            discountsMinor = discounts,
            returnsMinor = returns
        )
    }

    val todaySales = summarizePeriod(todayRange)
    val thisWeekSales = summarizePeriod(weekRange)
    val thisMonthSales = summarizePeriod(monthRange)

    val latestReport = reports.maxWithOrNull(
        compareBy<ShiftReport> { it.dateInMillis }.thenBy { it.id }
    )

    val expectedDrawerCashMinor = if (
        liveDraft != null &&
        liveDraft.hasAnyInput &&
        (liveDraft.openingCashMinor != 0L ||
            liveDraft.cashSalesMinor != 0L ||
            liveDraft.expensesMinor != 0L ||
            liveDraft.staffAdvancesCashMinor != 0L ||
            liveDraft.previousDueCashMinor != 0L ||
            liveDraft.purchasedCashMinor != 0L)
    ) {
        liveDraft.expectedCashMinor
    } else {
        latestReport?.expectedCashInDrawer ?: 0L
    }

    val hasLastShiftReport = latestReport != null
    val lastShiftVarianceMinor = latestReport?.cashDiscrepancy ?: 0L
    val lastShiftVarianceStatus = when {
        latestReport == null -> HomeVarianceStatus.NONE
        lastShiftVarianceMinor > 0L -> HomeVarianceStatus.OVER
        lastShiftVarianceMinor < 0L -> HomeVarianceStatus.SHORT
        else -> HomeVarianceStatus.BALANCED
    }

    // Outstanding dues using existing DueLedgerTab receipt netting logic
    val dueSales = parseDueSales(reports)
    val dueCollections = parseDueCollections(reports)
    val receiptMap = LinkedHashMap<String, Pair<Long, Long>>()
    for (sale in dueSales) {
        val current = receiptMap[sale.receiptNo] ?: (0L to 0L)
        receiptMap[sale.receiptNo] = (current.first + sale.amountMinor) to current.second
    }
    for (collection in dueCollections) {
        val current = receiptMap[collection.receiptNo] ?: (0L to 0L)
        receiptMap[collection.receiptNo] = current.first to (current.second + collection.amountMinor)
    }
    val receiptSummaries = receiptMap.map { (ref, totals) ->
        DueReceiptSummary(
            receiptNo = ref,
            initialDueMinor = totals.first,
            totalCollectedMinor = totals.second
        )
    }
    val dueNetting = summarizeDueMinor(receiptSummaries.map { it.remainingDueMinor })
    val openReceiptsCount = receiptSummaries.count { it.remainingDueMinor > 0L }

    // Advances this month — matches buildEmployerAdvancePeriodReport for the current month
    val advancePeriodReport = buildEmployerAdvancePeriodReport(
        advances = advances,
        employees = employees,
        nowMillis = nowMillis,
        zone = zone
    )

    // Expenses today: operational expenses + paid-out purchases in today's range (plus live draft)
    var expensesTodayMinor = 0L
    var expensesTodayCount = 0
    for (report in reports) {
        if (report.dateInMillis in todayRange) {
            if (report.totalExpenses > 0L) {
                expensesTodayMinor += report.totalExpenses
                expensesTodayCount += 1
            }
            val paidOutItems = report.purchasedItemsList.filter { it.totalAmount > 0L }
            expensesTodayMinor += MoneyFormat.sumOfMinor(paidOutItems.map { it.totalAmount })
            expensesTodayCount += paidOutItems.size
        }
    }
    if (liveDraft != null && liveDraft.hasAnyInput && (liveDraft.dateInMillis <= 0L || liveDraft.dateInMillis in todayRange)) {
        if (liveDraft.expensesMinor > 0L) {
            expensesTodayMinor += liveDraft.expensesMinor
            expensesTodayCount += 1
        }
        if (liveDraft.purchasedAllMinor > 0L) {
            expensesTodayMinor += liveDraft.purchasedAllMinor
            expensesTodayCount += liveDraft.purchasedCount
        }
    }

    val showOwnerOnlyParts = role.equals(ROLE_ADMIN, ignoreCase = true)
    val last7Days = if (showOwnerOnlyParts) {
        val windowStartDate = todayDate.minusDays((LAST_DAYS_CHART_COUNT - 1).toLong())
        val totalsByDate = HashMap<LocalDate, Long>(LAST_DAYS_CHART_COUNT)
        for (report in reports) {
            val reportDate = Instant.ofEpochMilli(report.dateInMillis).atZone(zone).toLocalDate()
            if (!reportDate.isBefore(windowStartDate) && !reportDate.isAfter(todayDate)) {
                totalsByDate[reportDate] = (totalsByDate[reportDate] ?: 0L) + report.netSales
            }
        }
        if (inProgressTimestamp != null) {
            val liveDate = Instant.ofEpochMilli(inProgressTimestamp).atZone(zone).toLocalDate()
            if (!liveDate.isBefore(windowStartDate) && !liveDate.isAfter(todayDate)) {
                val liveNet = MoneyFormat.calculateNetSalesMinor(
                    MoneyFormat.addMinor(inProgressCash, inProgressBank, inProgressWallet),
                    inProgressDiscounts,
                    inProgressReturns
                )
                totalsByDate[liveDate] = (totalsByDate[liveDate] ?: 0L) + liveNet
            }
        }

        (0 until LAST_DAYS_CHART_COUNT).map { offset ->
            val day = windowStartDate.plusDays(offset.toLong())
            HomeDaySalesPoint(
                date = day,
                weekdayLabel = day.dayOfWeek.getDisplayName(TextStyle.SHORT, locale),
                netSalesMinor = totalsByDate[day] ?: 0L,
                isToday = day == todayDate
            )
        }
    } else {
        emptyList()
    }

    return HomeDashboardSummary(
        todayLocalDate = todayDate,
        hasUnsavedDraft = liveDraft?.hasAnyInput == true,
        todaySales = todaySales,
        thisWeekSales = thisWeekSales,
        thisMonthSales = thisMonthSales,
        expectedDrawerCashMinor = expectedDrawerCashMinor,
        hasLastShiftReport = hasLastShiftReport,
        lastShiftVarianceMinor = lastShiftVarianceMinor,
        lastShiftVarianceStatus = lastShiftVarianceStatus,
        outstandingDuesMinor = dueNetting.outstandingMinor,
        openReceiptsCount = openReceiptsCount,
        advancesThisMonthTotalMinor = advancePeriodReport.totalAdvancesMinor,
        advancesThisMonthCashMinor = advancePeriodReport.cashTotalMinor,
        advancesThisMonthBankMinor = advancePeriodReport.bankTotalMinor,
        expensesTodayMinor = expensesTodayMinor,
        expensesTodayCount = expensesTodayCount,
        showOwnerOnlyParts = showOwnerOnlyParts,
        last7Days = last7Days
    )
}
