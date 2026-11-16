package com.lojia.shiftreport

import com.lojia.shiftreport.util.SerialUpdater
import com.lojia.shiftreport.util.toPlainDecimal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class Stage17FixesUnitTest {

    @Test
    fun toPlainDecimal_largeValueHasNoExponentOrGrouping() {
        assertEquals("12345678.90", toPlainDecimal(1234567890L))
    }

    @Test
    fun toPlainDecimal_singleDigitMinorPadsZero() {
        assertEquals("0.05", toPlainDecimal(5L))
    }

    @Test
    fun toPlainDecimal_zeroFormatsAsTwoDecimals() {
        assertEquals("0.00", toPlainDecimal(0L))
    }

    @Test
    fun toPlainDecimal_negativeFormatsWithMinusAndTwoDecimals() {
        assertEquals("-2.50", toPlainDecimal(-250L))
    }

    @Test
    fun serialUpdater_preventsLostUpdatesAcross50ConcurrentAdds() = runBlocking {
        val updater = SerialUpdater()
        val list = mutableListOf<Int>()

        (1..50).map { item ->
            async(Dispatchers.Default) {
                updater.update {
                    val snapshot = list.toList()
                    delay(1)
                    list.clear()
                    list.addAll(snapshot)
                    list.add(item)
                }
            }
        }.awaitAll()

        assertEquals(50, list.size)
    }
}
