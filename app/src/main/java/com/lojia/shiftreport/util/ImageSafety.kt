package com.lojia.shiftreport.util

/**
 * Pure sampling calculation for two-pass BitmapFactory decoding to avoid OOM on large images.
 */
fun calculateInSampleSize(width: Int, height: Int, maxDimension: Int = 2048): Int {
    if (width <= 0 || height <= 0 || maxDimension <= 0) return 1
    if (width <= maxDimension && height <= maxDimension) return 1
    var sample = 1
    while ((width / 2) / sample >= maxDimension && (height / 2) / sample >= maxDimension) {
        sample *= 2
    }
    return sample
}
