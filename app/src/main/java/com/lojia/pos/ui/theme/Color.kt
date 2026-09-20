package com.lojia.pos.ui.theme

import androidx.compose.ui.graphics.Color

// =========================================================================
// UNIFIED INTERNATIONAL DESIGN TOKEN SYSTEM (Material / Google M3 standard)
// Single Source of Truth
// =========================================================================

// Primary Brand Token
val PrimaryBlue = Color(0xFF2563EB)         // #2563EB - Main buttons, links, selected states
val PrimaryBlueDark = Color(0xFF1D4ED8)     // #1D4ED8
val PrimaryContainer = Color(0xFFEFF6FF)    // #EFF6FF

// Aliases for compatibility
val PrimaryIndigo = PrimaryBlue
val PrimaryIndigoDark = PrimaryBlueDark
val DarkCharcoal = Color(0xFF0F172A)

// Status Tokens
val SuccessGreen = Color(0xFF059669)        // #059669 - Positive indicators, completed states ONLY
val SuccessContainer = Color(0xFFECFDF5)    // #ECFDF5
val AccentEmerald = SuccessGreen

val WarningOrange = Color(0xFFD97706)       // #D97706
val WarningContainer = Color(0xFFFFFBEB)    // #FFFBEB
val AccentGold = WarningOrange

val ErrorRed = Color(0xFFDC2626)            // #DC2626
val ErrorContainer = Color(0xFFFEF2F2)      // #FEF2F2
val AccentRose = ErrorRed

// Neutral Canvas System
val PureWhite = Color(0xFFFFFFFF)           // #FFFFFF
val PureBlack = Color(0xFF000000)           // #000000
val BackgroundLight = Color(0xFFFFFFFF)     // #FFFFFF
val SurfaceLight = Color(0xFFF8FAFC)        // #F8FAFC
val SurfaceVariantLight = Color(0xFFF1F5F9) // #F1F5F9
val OutlineLight = Color(0xFFE2E8F0)        // #E2E8F0
val OutlineVariantLight = Color(0xFFCBD5E1) // #CBD5E1
val BgLightGrey = SurfaceLight

// Text Tokens
val TextPrimaryLight = Color(0xFF0F172A)    // #0F172A
val TextSecondaryLight = Color(0xFF64748B)  // #64748B
val TextTertiaryLight = Color(0xFF94A3B8)   // #94A3B8

// Secondary Accent
val SecondaryTeal = Color(0xFF0284C7)

// Loyverse Theme Color Mapping (Unified to Global Tokens)
val LoyverseTopGreen = PrimaryBlue          // Header uses brand primary
val LoyverseGreenPrimary = PrimaryBlue      // Action buttons use brand primary
val LoyverseGreenDark = PrimaryBlueDark
val LoyverseGreenLight = PrimaryBlue
val LoyverseHeaderButtonGreen = PrimaryBlue // Header buttons use brand primary
val LoyverseItemGrey = SurfaceVariantLight
val LoyverseDividerGrey = OutlineLight
val LoyverseTextDark = TextPrimaryLight
val LoyverseBlue = PrimaryBlue

// ShiftColors System (Unified to Global Tokens)
object ShiftColors {
    val Primary = PrimaryBlue               // #2563EB - Main buttons are Primary Blue
    val PrimaryDark = PrimaryBlueDark       // #1D4ED8
    val Bg = SurfaceLight                  // #F8FAFC
    val Card = PureWhite                   // #FFFFFF
    val Text = TextPrimaryLight            // #0F172A
    val TextMuted = TextSecondaryLight      // #64748B
    val Border = OutlineLight              // #E2E8F0
    val SaveBtn = PrimaryBlue              // #2563EB
    val Danger = ErrorRed                  // #DC2626
    val DangerLight = ErrorContainer        // #FEF2F2
    val Purple = PrimaryBlue               // #2563EB
    val PurpleLight = PrimaryContainer     // #EFF6FF
    val Charcoal = TextPrimaryLight        // #0F172A
    val CharcoalSoft = TextSecondaryLight   // #64748B
    val Brass = WarningOrange              // #D97706
    val BrassLight = WarningContainer       // #FFFBEB
    val NetCashGreen = SuccessGreen        // #059669
    val NetMadaBlue = PrimaryBlue          // #2563EB
}


