package com.lojia.shiftreport.util

import android.graphics.Color

/**
 * Centralized color palette for ALL PDF generation in Lojia POS.
 * Matches the UI design system (Color.kt) — do NOT invent new colors.
 * All PDF files MUST reference these constants, never hardcoded RGB.
 */
object PdfPalette {
    // Brand
    val PRIMARY           = Color.rgb(29, 78, 216)    // #1D4ED8
    val PRIMARY_DARK      = Color.rgb(30, 58, 138)    // #1E3A8A
    val PRIMARY_CONTAINER = Color.rgb(219, 234, 254)  // #DBEAFE
    val ON_PRIMARY        = Color.WHITE

    // Status
    val SUCCESS           = Color.rgb(4, 120, 87)     // #047857
    val SUCCESS_CONTAINER = Color.rgb(209, 250, 229)  // #D1FAE5
    val WARNING           = Color.rgb(180, 83, 9)     // #B45309
    val WARNING_CONTAINER = Color.rgb(254, 243, 199)  // #FEF3C7
    val ERROR             = Color.rgb(185, 28, 28)    // #B91C1C
    val ERROR_CONTAINER   = Color.rgb(254, 226, 226)  // #FEE2E2
    val INFO              = Color.rgb(3, 105, 161)    // #0369A1

    // Surfaces
    val SURFACE           = Color.WHITE
    val SURFACE_VARIANT   = Color.rgb(241, 245, 249)  // #F1F5F9
    val BACKGROUND        = Color.rgb(248, 250, 252)  // #F8FAFC
    val BORDER            = Color.rgb(203, 213, 225)  // #CBD5E1
    val DIVIDER           = Color.rgb(226, 232, 240)  // #E2E8F0

    // Text
    val TEXT_PRIMARY      = Color.rgb(15, 23, 42)     // #0F172A
    val TEXT_SECONDARY    = Color.rgb(71, 85, 105)    // #475569
    val TEXT_HINT         = Color.rgb(100, 116, 139)  // #64748B

    // QR (charcoal)
    val QR_DARK           = Color.rgb(30, 41, 59)
}
