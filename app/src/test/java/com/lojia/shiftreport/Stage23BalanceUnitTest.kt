package com.lojia.shiftreport

import com.lojia.shiftreport.report.EmployerAdvanceItem
import com.lojia.shiftreport.util.advancesInMonthMinor
import com.lojia.shiftreport.util.balanceMinor
import com.lojia.shiftreport.util.currentMonthRange
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class Stage23BalanceUnitTest {

    @Test
    fun balanceMinor_isSalaryMinusAdvances() {
        assertEquals(65050L, balanceMinor(150050L, 85000L))
    }

    @Test
    fun balanceMinor_isNegativeWhenAdvancesExceedSalary() {
        assertEquals(-20000L, balanceMinor(100000L, 120000L))
    }

    @Test
    fun advancesInMonthMinor_countsOnlyCurrentCalendarMonth() {
        val zone = ZoneId.of("Asia/Riyadh")
        val nowMillis = ZonedDateTime.of(2026, 11, 16, 12, 0, 0, 0, zone).toInstant().toEpochMilli()
        val range = currentMonthRange(nowMillis, zone)

        val startOfMonth = ZonedDateTime.of(2026, 11, 1, 0, 0, 0, 0, zone).toInstant().toEpochMilli()
        val lastMsOfPrevMonth = startOfMonth - 1L
        val startOfNextMonth = ZonedDateTime.of(2026, 12, 1, 0, 0, 0, 0, zone).toInstant().toEpochMilli()
        val lastMsOfMonth = startOfNextMonth - 1L

        assertEquals(startOfMonth, range.first)
        assertEquals(lastMsOfMonth, range.last)

        val items = listOf(
            EmployerAdvanceItem(
                staffName = "Hasan",
                amountMinor = 1000L,
                paymentMode = "CASH",
                dateInMillis = lastMsOfPrevMonth,
                cashierName = "Cashier"
            ),
            EmployerAdvanceItem(
                staffName = "Hasan",
                amountMinor = 2500L,
                paymentMode = "CASH",
                dateInMillis = startOfMonth,
                cashierName = "Cashier"
            ),
            EmployerAdvanceItem(
                staffName = "Hasan",
                amountMinor = 3500L,
                paymentMode = "BANK",
                dateInMillis = lastMsOfMonth,
                cashierName = "Cashier"
            ),
            EmployerAdvanceItem(
                staffName = "Hasan",
                amountMinor = 4000L,
                paymentMode = "CASH",
                dateInMillis = startOfNextMonth,
                cashierName = "Cashier"
            )
        )

        assertEquals(6000L, advancesInMonthMinor(items, "Hasan", range))
    }

    @Test
    fun advancesInMonthMinor_matchesNameIgnoringCaseAndSpaces() {
        val zone = ZoneId.of("Asia/Riyadh")
        val nowMillis = ZonedDateTime.of(2026, 11, 16, 12, 0, 0, 0, zone).toInstant().toEpochMilli()
        val range = currentMonthRange(nowMillis, zone)

        val items = listOf(
            EmployerAdvanceItem(
                staffName = "Hasan",
                amountMinor = 4500L,
                paymentMode = "CASH",
                dateInMillis = nowMillis,
                cashierName = "Cashier"
            ),
            EmployerAdvanceItem(
                staffName = "Other",
                amountMinor = 9000L,
                paymentMode = "CASH",
                dateInMillis = nowMillis,
                cashierName = "Cashier"
            )
        )

        assertEquals(4500L, advancesInMonthMinor(items, "  hasan ", range))
    }
}
