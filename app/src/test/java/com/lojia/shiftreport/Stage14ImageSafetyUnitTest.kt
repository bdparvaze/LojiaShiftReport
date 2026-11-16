package com.lojia.shiftreport

import com.lojia.shiftreport.util.calculateInSampleSize
import org.junit.Assert.assertEquals
import org.junit.Test

class Stage14ImageSafetyUnitTest {

    @Test
    fun calculateInSampleSize_1000x800_returns1() {
        assertEquals(1, calculateInSampleSize(1000, 800))
    }

    @Test
    fun calculateInSampleSize_4000x3000_returns1() {
        assertEquals(1, calculateInSampleSize(4000, 3000))
    }

    @Test
    fun calculateInSampleSize_8000x6000_returns2() {
        assertEquals(2, calculateInSampleSize(8000, 6000))
    }

    @Test
    fun calculateInSampleSize_16000x12000_returns4() {
        assertEquals(4, calculateInSampleSize(16000, 12000))
    }

    @Test
    fun calculateInSampleSize_0x0_returns1() {
        assertEquals(1, calculateInSampleSize(0, 0))
    }

    @Test
    fun calculateInSampleSize_negative5x100_returns1() {
        assertEquals(1, calculateInSampleSize(-5, 100))
    }
}
