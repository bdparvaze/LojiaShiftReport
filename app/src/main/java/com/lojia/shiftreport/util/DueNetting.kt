package com.lojia.shiftreport.util

import kotlin.math.abs

data class DueNettingResult(
    val outstandingMinor: Long,
    val extraCollectedMinor: Long
)

fun summarizeDueMinor(remainingMinors: List<Long>): DueNettingResult {
    var outstanding = 0L
    var extra = 0L
    for (remaining in remainingMinors) {
        when {
            remaining > 0L -> outstanding += remaining
            remaining < 0L -> extra += abs(remaining)
        }
    }
    return DueNettingResult(
        outstandingMinor = outstanding,
        extraCollectedMinor = extra
    )
}
