package com.lojia.shiftreport.ui.theme

import androidx.compose.ui.graphics.Color

// =========================================================================
// LOJIA SHIFT REPORT - REFACTORED COLOR SYSTEM (Synchronized with colors.xml)
// =========================================================================

// ============ PRIMARY (Brand Blue) ============
val PrimaryBlueLight = Color(0xFF1D4FD8)
val PrimaryBlueDark = Color(0xFF1E3A8A)
val PrimaryLight = Color(0xFF3B82F6)
val PrimaryContainerLight = Color(0xFFDBEAFE)
val OnPrimaryLight = Color(0xFFFFFFFF)
val OnPrimaryContainerLight = Color(0xFF1E3A8A)

// Primary Aliases
val PrimaryIndigoLight = PrimaryBlueLight
val PrimaryIndigoDark = PrimaryBlueDark

// ============ SECONDARY (Teal) ============
val SecondaryTealLight = Color(0xFF0F766E)
val SecondaryContainerLight = Color(0xFFCCFBF1)
val OnSecondaryLight = Color(0xFFFFFFFF)
val OnSecondaryContainerLight = Color(0xFF134E4A)

// ============ TERTIARY (Purple) ============
val TertiaryPurpleLight = Color(0xFF7C3AED)
val TertiaryContainerLight = Color(0xFFEDE9FE)
val OnTertiaryLight = Color(0xFFFFFFFF)
val OnTertiaryContainerLight = Color(0xFF4C1D95)

// Tertiary Aliases
val TertiaryCyanLight = TertiaryPurpleLight

// ============ STATUS COLORS ============
val SuccessGreen = Color(0xFF047857)
val SuccessContainer = Color(0xFFD1FAE5)
val OnSuccessLight = Color(0xFFFFFFFF)

val WarningOrange = Color(0xFFB45309)
val WarningContainer = Color(0xFFFEF3C7)
val OnWarningLight = Color(0xFFFFFFFF)
val WarningText = Color(0xFF78350F)

val ErrorRedLight = Color(0xFFB91C1C)
val ErrorContainerLight = Color(0xFFFEE2E2)
val OnErrorLight = Color(0xFFFFFFFF)
val OnErrorContainerLight = Color(0xFF7F1D1D)

val InfoBlue = Color(0xFF0369A1)
val InfoContainer = Color(0xFFE0F2FE)
val OnInfoLight = Color(0xFFFFFFFF)

// ============ BACKGROUND / SURFACE ============
val BackgroundLight = Color(0xFFF4F7FD)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceVariantLight = Color(0xFFF1F5F9)
val SurfaceElevatedLight = Color(0xFFFFFFFF)
val OutlineLight = Color(0xFFCBD5E1)
val OutlineVariantLight = Color(0xFFE2E8F0)
val OverlayScrim = Color(0x80000000)

// ============ TEXT ============
val OnBackgroundLight = Color(0xFF0F172A)
val OnSurfaceLight = Color(0xFF0F172A)
val OnSurfaceVariantLight = Color(0xFF475467)
val TextHintColor = Color(0xFF64748B)
val TextDisabledColor = Color(0xFF94A3B8)
val TextOnDark = Color(0xFFFFFFFF)
val TextOnPrimary = Color(0xFFFFFFFF)

// ============ POS SEMANTIC COLORS (WCAG AA Compliant) ============
val PosCashGreen = Color(0xFF047857)
val PosCashGreenFill = Color(0xFF059669)
val PosCardBlue = Color(0xFF2563EB)
val PosDueOrange = Color(0xFFC2410C)
val PosDueOrangeFill = Color(0xFFEA580C)
val PosExpenseRed = ErrorRedLight
val PosExpenseRedFill = Color(0xFFDC2626)
val PosShortageRed = PosExpenseRed
val PosSurplusGreen = Color(0xFF047857)
val PosVarianceNeutral = Color(0xFF64748B)

// ============ CHART COLORS ============
val ChartBlue = Color(0xFF2563EB)
val ChartGreen = Color(0xFF059669)
val ChartOrange = Color(0xFFEA580C)
val ChartPurple = Color(0xFF7C3AED)
val ChartTeal = Color(0xFF0F766E)
val ChartAmber = Color(0xFFD97706)
val ChartIndigo = Color(0xFF6366F1)

// ============ ROLE & BANNER SEMANTIC TOKENS ============
val RoleBadgeGreen = Color(0xFF2E7D32)
val PositiveBannerSurface = Color(0xFFF0FDF4)
val PositiveBannerBorder = Color(0xFFBBF7D0)
val LiveDotGreen = Color(0xFF16A34A)
val PositiveBannerTextDark = Color(0xFF166534)
val PositiveBannerText = Color(0xFF067647)
val NegativeBannerSurface = Color(0xFFFEF2F2)
val NegativeBannerBorder = Color(0xFFFECACA)
val NegativeBannerText = Color(0xFFB42318)

// --- Common Token Aliases & System Compatibility ---
val BgLightGrey = SurfaceVariantLight
val BgWhite = SurfaceLight
val CardWhite = SurfaceLight
val BorderLight = OutlineLight

val PrimaryBlue = PrimaryBlueLight
val PrimaryContainer = PrimaryContainerLight
val PrimaryIndigo = PrimaryBlueLight

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

// ============ STAGE 26C EXTENDED SEMANTIC, PROVIDER, LOGO & MAP TOKENS ============
val SuccessTextDark = Color(0xFF065F46)

// Auth Palette Supporting Tones (all text pairings meet WCAG AA >= 4.5:1)
val AuthPrimaryDeep = Color(0xFF001B3E)
val AuthPrimaryDark = Color(0xFF284777)
val AuthPrimarySoft = Color(0xFF5D7BAE)
val AuthSuccessMint = Color(0xFF72F8DC)

// Cloud Backup Provider Brand Accents
val GoogleDriveBlue = Color(0xFF1A73E8)
val OneDriveBlue = Color(0xFF0078D4)

// Business Logo Preset Gradients (White foreground on gradient stops)
val PresetLogoSuperstoreStart = PrimaryBlueDark
val PresetLogoSuperstoreEnd = OnBackgroundLight
val PresetLogoRetailStart = Color(0xFF10B981)
val PresetLogoRetailEnd = PosCashGreenFill
val PresetLogoCafeStart = WarningOrange
val PresetLogoCafeEnd = WarningText
val PresetLogoRestaurantStart = Color(0xFFF97316)
val PresetLogoRestaurantEnd = PosDueOrange
val PresetLogoFashionStart = Color(0xFFEC4899)
val PresetLogoFashionEnd = Color(0xFFBE185D)
val PresetLogoPharmacyStart = Color(0xFF06B6D4)
val PresetLogoPharmacyEnd = Color(0xFF0E7490)
val PresetLogoTechStart = ChartIndigo
val PresetLogoTechEnd = Color(0xFF4338CA)

// Interactive Map Canvas Surface & Vector Colors
val MapLandBg = Color(0xFFF1F3F4)
val MapWaterBlue = Color(0xFFC4E8FD)
val MapWaterTint = Color(0xFFD2E3FC)
val MapParkGreen = Color(0xFFCEEAD6)
val MapUrbanBlock = Color(0xFFE8EAED)
val MapHighwayBorder = Color(0xFFF9AB00)
val MapHighwayFill = Color(0xFFFEEFC3)

// ============ STAGE 26D DESIGN BOARD PARITY TOKENS ============
/** Required field asterisk red matching Stage 26D board specification. */
val RequiredStarRed = NegativeBannerText // 0xFFB42318

/** Crisp hairline border tint for raised cards matching #E5E9F5. */
val DepthCardBorder = Color(0xFFE5E9F5)

/** Deep slate shadow tint rgba(16, 24, 40, ...) used for card, inset, and keypad shadows. */
val ShadowSlateColor = Color(0xFF101828)

// ============ STAGE 26F-1 DESIGN BOARD PARITY TOKENS ============
/** Screen canvas background behind Report Entry, Due History, and Employer Advances (#EEF2FB). */
val ReportCanvasBackground = Color(0xFFEEF2FB)

/** High-emphasis report ink matching board #101828. */
val TextPrimaryReport = Color(0xFF101828)

/** Slate 700 label & inactive segment ink matching board #344054. */
val TextSlate700 = Color(0xFF344054)

/** Report numeric placeholder ink matching board #667085. */
val TextPlaceholderReport = Color(0xFF667085)

/** 1dp perimeter border for report inset fields matching board #C9D3E8. */
val ReportInputBorder = Color(0xFFC9D3E8)

/** 135-deg top brand header gradient start stop (#2B5DE8, GR6). */
val BrandHeaderDiagonalStart = Color(0xFF2B5DE8)

/** 135-deg top brand header gradient end stop (#1238A8, GR6). */
val BrandHeaderDiagonalEnd = Color(0xFF1238A8)

/** Soft blue pill background (#E0EAFF) for period chips and light action pills. */
val BadgeBlueBg = Color(0xFFE0EAFF)

/** Deep brand blue ink (#1D3FA8) paired with BadgeBlueBg and CashWellBg. */
val BadgeBlueText = Color(0xFF1D3FA8)

/** Recessed Cash breakdown well fill (#EEF3FF). */
val CashWellBg = Color(0xFFEEF3FF)

/** Recessed Bank breakdown well fill (#E6F7F5). */
val BankWellBg = Color(0xFFE6F7F5)

/** Bank breakdown well ink (#0B6E63) meeting WCAG AA on BankWellBg. */
val BankWellText = Color(0xFF0B6E63)

/** Settled/positive pill fill (#D1FADF) and ink (#05603A). */
val BadgeGreenBg = Color(0xFFD1FADF)
val BadgeGreenText = Color(0xFF05603A)

/** Partial/warning pill fill (#FEF0C7) and dark amber inks (#7A2E0E / #B54708). */
val BadgeAmberBg = Color(0xFFFEF0C7)
val BadgeAmberTextDark = Color(0xFF7A2E0E)
val BadgeAmberAccent = Color(0xFFB54708)

/** Segmented control recessed track fill (#E9EDF6). */
val SegmentTrackBg = Color(0xFFE9EDF6)

/** Report section accordion banner stops (GR13 amber, rose, purple). */
val SectionAmberGradientStart = Color(0xFFFFF6D8)
val SectionAmberGradientEnd = Color(0xFFFEF0C7)
val SectionAmberBorder = Color(0xFFF2D77E)

val SectionRoseGradientStart = Color(0xFFFFEDEC)
val SectionRoseGradientEnd = Color(0xFFFEE4E2)
val SectionRoseBorder = Color(0xFFF1A7A7)

val SectionPurpleGradientStart = Color(0xFFF3F0FF)
val SectionPurpleGradientEnd = Color(0xFFEDE9FE)
val SectionPurpleBorder = Color(0xFFB9A8F0)

/** Due History KPI gradient card stops (GR11 red-tinted, GR10 green-tinted). */
val KpiDueCardStart = Color(0xFFFFFFFF)
val KpiDueCardEnd = Color(0xFFFEF3F2)
val KpiDueCardBorder = Color(0xFFF6C7C3)

val KpiCollectedCardStart = Color(0xFFFFFFFF)
val KpiCollectedCardEnd = Color(0xFFECFDF3)
val KpiCollectedCardBorder = Color(0xFFA6E9C5)

/** Live pulse halo emerald tint (#12B76A, SH15). */
val LiveDotGlowGreen = Color(0xFF12B76A)

// ============ STAGE 26F-2 DESIGN BOARD PARITY TOKENS ============
/** Hairline row divider gray (#EAECF0) inside grouped Paid Out, Settings, and Ledger cards. */
val ListDividerGray = Color(0xFFEAECF0)

/** Soft teal bottom-right tile gradient stop (#E0F2F1) for Printer icon tiles (SH1). */
val TileTealEnd = Color(0xFFE0F2F1)

/** Secondary recessed form well fill (#F4F6FB) and perimeter border (#D0D5DD). */
val FormWellFill = Color(0xFFF4F6FB)
val FormWellBorder = Color(0xFFD0D5DD)


