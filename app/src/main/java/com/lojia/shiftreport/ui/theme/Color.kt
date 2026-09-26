package com.lojia.shiftreport.ui.theme

import androidx.compose.ui.graphics.Color

// =========================================================================
// LOJIA SHIFT REPORT - REFACTORED COLOR SYSTEM (Synchronized with colors.xml)
// =========================================================================

// ============ PRIMARY (Brand Blue) ============
val PrimaryIndigoLight = Color(0xFF1D4ED8)
val PrimaryIndigoDark = Color(0xFF1E3A8A)
val PrimaryLight = Color(0xFF3B82F6)
val PrimaryContainerLight = Color(0xFFDBEAFE)
val OnPrimaryLight = Color(0xFFFFFFFF)
val OnPrimaryContainerLight = Color(0xFF1E3A8A)

// ============ SECONDARY (Teal) ============
val SecondaryTealLight = Color(0xFF0F766E)
val SecondaryContainerLight = Color(0xFFCCFBF1)
val OnSecondaryLight = Color(0xFFFFFFFF)
val OnSecondaryContainerLight = Color(0xFF134E4A)

// ============ TERTIARY (Purple) ============
val TertiaryCyanLight = Color(0xFF7C3AED)
val TertiaryContainerLight = Color(0xFFEDE9FE)
val OnTertiaryLight = Color(0xFFFFFFFF)
val OnTertiaryContainerLight = Color(0xFF4C1D95)

// ============ STATUS COLORS ============
val SuccessGreen = Color(0xFF047857)
val SuccessContainer = Color(0xFFD1FAE5)
val OnSuccessLight = Color(0xFFFFFFFF)

val WarningOrange = Color(0xFFB45309)
val WarningContainer = Color(0xFFFEF3C7)
val OnWarningLight = Color(0xFFFFFFFF)

val ErrorRedLight = Color(0xFFB91C1C)
val ErrorContainerLight = Color(0xFFFEE2E2)
val OnErrorLight = Color(0xFFFFFFFF)
val OnErrorContainerLight = Color(0xFF7F1D1D)

val InfoBlue = Color(0xFF0369A1)
val InfoContainer = Color(0xFFE0F2FE)
val OnInfoLight = Color(0xFFFFFFFF)

// ============ BACKGROUND / SURFACE ============
val BackgroundLight = Color(0xFFF8FAFC)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceVariantLight = Color(0xFFF1F5F9)
val SurfaceElevatedLight = Color(0xFFFFFFFF)
val OutlineLight = Color(0xFFCBD5E1)
val OutlineVariantLight = Color(0xFFE2E8F0)
val OverlayScrim = Color(0x80000000)

// ============ TEXT ============
val OnBackgroundLight = Color(0xFF0F172A)
val OnSurfaceLight = Color(0xFF0F172A)
val OnSurfaceVariantLight = Color(0xFF475569)
val TextHintColor = Color(0xFF64748B)
val TextDisabledColor = Color(0xFF94A3B8)
val TextOnDark = Color(0xFFFFFFFF)
val TextOnPrimary = Color(0xFFFFFFFF)

// ============ POS SEMANTIC COLORS ============
val PosCashGreen = Color(0xFF059669)
val PosCardBlue = Color(0xFF2563EB)
val PosDueOrange = Color(0xFFEA580C)
val PosExpenseRed = Color(0xFFDC2626)
val PosShortageRed = Color(0xFFDC2626)
val PosSurplusGreen = Color(0xFF059669)
val PosVarianceNeutral = Color(0xFF64748B)

// ============ CHART COLORS ============
val ChartBlue = Color(0xFF2563EB)
val ChartGreen = Color(0xFF059669)
val ChartOrange = Color(0xFFEA580C)
val ChartPurple = Color(0xFF7C3AED)
val ChartTeal = Color(0xFF0F766E)

// ============ DARK TOKENS (Synchronized with values-night/colors.xml) ============
val OnPrimaryDark = Color(0xFF0F172A)
val PrimaryContainerDark = Color(0xFF1E3A8A)
val OnPrimaryContainerDark = Color(0xFFDBEAFE)
val SecondaryTealDark = Color(0xFF5EEAD4)
val OnSecondaryDark = Color(0xFF0F172A)
val SecondaryContainerDark = Color(0xFF134E4A)
val OnSecondaryContainerDark = Color(0xFFCCFBF1)
val TertiaryCyanDark = Color(0xFFA78BFA)
val OnTertiaryDark = Color(0xFF0F172A)
val TertiaryContainerDark = Color(0xFF4C1D95)
val OnTertiaryContainerDark = Color(0xFFEDE9FE)
val ErrorRedDark = Color(0xFFF87171)
val OnErrorDark = Color(0xFF0F172A)
val ErrorContainerDark = Color(0xFF7F1D1D)
val OnErrorContainerDark = Color(0xFFFEE2E2)
val BackgroundDark = Color(0xFF0F172A)
val OnBackgroundDark = Color(0xFFF1F5F9)
val SurfaceDark = Color(0xFF1E293B)
val OnSurfaceDark = Color(0xFFF1F5F9)
val SurfaceVariantDark = Color(0xFF334155)
val OnSurfaceVariantDark = Color(0xFFCBD5E1)
val OutlineDark = Color(0xFF475569)
val OutlineVariantDark = Color(0xFF334155)

// --- Common Token Aliases & System Compatibility ---
val BgLightGrey = SurfaceVariantLight
val BgWhite = SurfaceLight
val CardWhite = SurfaceLight
val BorderLight = OutlineLight
val BorderDark = OutlineDark

val PrimaryBlue = PrimaryIndigoLight
val PrimaryBlueDark = PrimaryIndigoDark
val PrimaryContainer = PrimaryContainerLight
val PrimaryIndigo = PrimaryIndigoLight

val AccentEmerald = PosCashGreen
val AccentGold = WarningOrange
val ErrorRed = ErrorRedLight
val ErrorContainer = ErrorContainerLight
val AccentRose = ErrorRedLight

val PureWhite = Color(0xFFFFFFFF)
val PureBlack = Color(0xFF000000)
val DarkCharcoal = OnBackgroundLight
val MediumGray = OnSurfaceVariantLight

val TextPrimaryLight = OnBackgroundLight
val TextSecondaryLight = OnSurfaceVariantLight
val TextTertiaryLight = TextHintColor

val SecondaryTeal = SecondaryTealLight

val LoyverseTopGreen = PrimaryIndigoLight
val LoyverseGreenPrimary = PrimaryIndigoLight
val LoyverseGreenDark = PrimaryIndigoDark
val LoyverseGreenLight = PrimaryLight
val LoyverseHeaderButtonGreen = PrimaryIndigoLight
val LoyverseItemGrey = SurfaceVariantLight
val LoyverseDividerGrey = OutlineVariantLight
val LoyverseTextDark = TextPrimaryLight
val LoyverseBlue = PrimaryIndigoLight

// ShiftColors System
object ShiftColors {
    val Primary = PrimaryIndigoLight
    val PrimaryDark = PrimaryIndigoDark
    val Bg = BackgroundLight
    val Card = SurfaceLight
    val Text = OnBackgroundLight
    val TextMuted = OnSurfaceVariantLight
    val Border = OutlineLight
    val SaveBtn = PrimaryIndigoLight
    val Danger = ErrorRedLight
    val DangerLight = ErrorContainerLight
    val Purple = ChartPurple
    val PurpleLight = TertiaryContainerLight
    val Charcoal = OnBackgroundLight
    val CharcoalSoft = OnSurfaceVariantLight
    val Brass = WarningOrange
    val BrassLight = WarningContainer
    val NetCashGreen = PosCashGreen
    val NetMadaBlue = PosCardBlue
}
