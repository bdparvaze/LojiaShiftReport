package com.lojia.shiftreport.util

/**
 * Pure pagination helper for PDF layout and table rendering.
 * Returns true when drawing [requiredHeight] at [currentY] would cross [pageBottomLimit].
 */
fun needsNewPage(currentY: Float, requiredHeight: Float, pageBottomLimit: Float): Boolean {
    return currentY + requiredHeight > pageBottomLimit
}
