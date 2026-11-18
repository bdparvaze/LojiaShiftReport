package com.lojia.shiftreport

import com.lojia.shiftreport.data.Employee
import com.lojia.shiftreport.settings.isDuplicateEmployeeName
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Stage23FixUpUnitTest {

    private val existing = listOf(
        Employee(id = "emp-1", name = "Hasan", monthlySalaryMinor = 150000L),
        Employee(id = "emp-2", name = "Noor", monthlySalaryMinor = 120000L)
    )

    @Test
    fun isDuplicateEmployeeName_sameIdSameNameIsAllowed() {
        assertFalse(isDuplicateEmployeeName("emp-1", "Hasan", existing))
        assertFalse(isDuplicateEmployeeName("emp-1", "  hasan ", existing))
    }

    @Test
    fun isDuplicateEmployeeName_differentIdSameNormalizedNameIsBlocked() {
        assertTrue(isDuplicateEmployeeName("emp-2", "  hAsAn ", existing))
        assertTrue(isDuplicateEmployeeName(null, "  hAsAn ", existing))
    }

    @Test
    fun isDuplicateEmployeeName_differentIdDifferentNameIsAllowed() {
        assertFalse(isDuplicateEmployeeName("emp-2", "Karim", existing))
        assertFalse(isDuplicateEmployeeName(null, "Karim", existing))
    }
}
