package com.lojia.shiftreport

import com.lojia.shiftreport.util.withValidSalary
import org.junit.Assert.assertEquals
import org.junit.Test

class Stage23PZeroSalaryRecordsUnitTest {

    @Test
    fun positiveSalaries_areKept() {
        assertEquals(listOf(1L, 500L), listOf(1L, 500L).withValidSalary { it })
    }

    @Test
    fun zeroAndNegativeSalaries_areRemoved() {
        assertEquals(listOf(1L), listOf(0L, -5L, 1L).withValidSalary { it })
    }
}
