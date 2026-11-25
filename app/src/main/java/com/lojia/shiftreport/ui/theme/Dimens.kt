package com.lojia.shiftreport.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Lojia Design System Dimensions & Sizing Tokens.
 * Enforces compact, professional, and accessible UI sizing across all mobile screens and dialogs.
 */
object Dimens {

    // =========================================================================
    // SPACING TOKENS (8-dp / 4-dp Grid)
    // =========================================================================
    val SpacingNone: Dp = 0.dp
    val SpacingXxs: Dp = 2.dp
    val SpacingXs: Dp = 4.dp
    val SpacingSm: Dp = 8.dp
    val SpacingMd: Dp = 12.dp
    val SpacingLg: Dp = 16.dp
    val SpacingXl: Dp = 20.dp
    val SpacingXxl: Dp = 24.dp
    val SpacingHuge: Dp = 32.dp

    // =========================================================================
    // CORNER RADIUS TOKENS
    // =========================================================================
    val RadiusXs: Dp = 4.dp
    val RadiusSm: Dp = 8.dp
    /** Radius for register compact inset fields (10.dp). */
    val RadiusInputRegister: Dp = 10.dp
    val RadiusMd: Dp = 12.dp
    /** Radius for report inputs, section accordions, statement tiles, and Quick PIN keys (14.dp). */
    val RadiusReportInput: Dp = 14.dp
    /** Radius for segmented period selector track (15.dp). */
    val RadiusSegmentTrack: Dp = 15.dp
    /** Radius for active segmented selector thumb (11.dp). */
    val RadiusSegmentThumb: Dp = 11.dp
    /** Radius for Quick PIN square keypad keys (14.dp). */
    val RadiusKeypad: Dp = 14.dp
    /** Radius for app logo tile (15.dp). */
    val RadiusLogoTile: Dp = 15.dp
    val RadiusLg: Dp = 16.dp
    /** Radius for KPI and ledger breakdown cards (18.dp). */
    val RadiusKpiCard: Dp = 18.dp
    /** Radius for icon tiles inside section banners (10.dp). */
    val RadiusSectionIconTile: Dp = 10.dp
    val RadiusXl: Dp = 20.dp
    /** Radius for raised auth and surface cards (22.dp). */
    val CardCornerRadius: Dp = 22.dp
    val RadiusPill: Dp = 24.dp
    val RadiusFull: Dp = 999.dp

    // =========================================================================
    // CONTROL & INTERACTIVE COMPONENT HEIGHTS
    // =========================================================================
    val ButtonHeightCompact: Dp = 40.dp
    val ButtonHeightStandard: Dp = 44.dp
    val ButtonHeightComfortable: Dp = 48.dp
    /** Primary and secondary depth button height (50.dp). */
    val ButtonHeightPrimary: Dp = 50.dp
    /** Report / Statement primary CTA height (52.dp). */
    val ButtonHeightReportPrimary: Dp = 52.dp
    val MinTouchTarget: Dp = 48.dp

    /** Minimum height for compact register inset fields (46.dp). */
    val TextFieldHeightCompact: Dp = 46.dp
    /** Standard report entry / search inset field height (52.dp). */
    val TextFieldHeightReport: Dp = 52.dp
    /** Standard inset field height for LojiaTextField (56.dp). */
    val TextFieldHeightStandard: Dp = 56.dp

    // =========================================================================
    // DIALOG SIZING RULES
    // =========================================================================
    val DialogMaxWidth: Dp = 400.dp
    val DialogSmallMaxWidth: Dp = 340.dp
    val DialogMaxHeight: Dp = 520.dp
    val DialogPadding: Dp = 16.dp
    val DialogPaddingComfortable: Dp = 20.dp
    val DialogCornerRadius: Dp = 22.dp
    val DialogElevation: Dp = 10.dp

    // =========================================================================
    // ICON SIZES
    // =========================================================================
    val IconXs: Dp = 14.dp
    val IconSm: Dp = 18.dp
    /** Standard leading/trailing field icon size (20.dp). */
    val IconField: Dp = 20.dp
    val IconMd: Dp = 22.dp
    val IconLg: Dp = 28.dp
    val IconXl: Dp = 36.dp

    // =========================================================================
    // DEPTH ELEVATION, BORDER & COMPONENT TOKENS (FLATTENED IN STAGE 29A)
    // =========================================================================
    /** Standard 1dp border width for flat cards, buttons, and inputs. */
    val FlatBorderWidth: Dp = 1.dp
    /** Border width for focused and error flat input fields (1.5.dp). */
    val FlatFocusedBorderWidth: Dp = 1.5.dp
    /** Single soft drop shadow Y-offset (1.dp) and blur (4.dp) for flat elevated cards and primary buttons. */
    val FlatCardShadowOffsetY: Dp = 1.dp
    val FlatCardShadowBlur: Dp = 4.dp

    val CardElevation: Dp = 10.dp
    val CardAccentElevation: Dp = 12.dp
    /** Inner content padding for auth cards (18.dp). */
    val CardPaddingAuth: Dp = 18.dp
    /** Single soft ambient card shadow Y-offset and blur (1.dp / 4.dp). */
    val CardShadowCloseOffsetY: Dp = FlatCardShadowOffsetY
    val CardShadowCloseBlur: Dp = FlatCardShadowBlur
    /** Far directional card shadow removed in Stage 29A flat UI (0.dp / 0.dp). */
    val CardShadowFarOffsetY: Dp = 0.dp
    val CardShadowFarBlur: Dp = 0.dp

    val RaisedButtonElevation: Dp = 8.dp
    val ButtonLipHeight: Dp = 0.dp
    /** Primary button soft shadow Y-offset and blur matching flat cards (1.dp / 4.dp). */
    val ButtonShadowOffsetY: Dp = FlatCardShadowOffsetY
    val ButtonShadowBlur: Dp = FlatCardShadowBlur
    /** Vertical press offset removed in Stage 29A flat UI (0.dp). */
    val ButtonPressedTranslationY: Dp = 0.dp

    val CardBorderWidth: Dp = FlatBorderWidth
    val InsetBorderWidth: Dp = FlatBorderWidth
    /** Border width for compact register inset fields (1.dp). */
    val InsetRegisterBorderWidth: Dp = FlatBorderWidth
    /** Border width for focused and error inset fields (1.5.dp). */
    val InsetFocusedBorderWidth: Dp = FlatFocusedBorderWidth
    val InsetShadowHeight: Dp = 0.dp
    /** Inner top shadow removed in Stage 29A flat UI (0.dp / 0.dp). */
    val InsetShadowOffsetY: Dp = 0.dp
    val InsetShadowBlur: Dp = 0.dp
    /** Horizontal padding inside standard inset fields (14.dp). */
    val InsetFieldPaddingHorizontal: Dp = 14.dp
    /** Horizontal padding inside register inset fields (12.dp). */
    val RegisterFieldPaddingHorizontal: Dp = 12.dp
    /** Horizontal gap between leading icon and text in inset fields (12.dp). */
    val InsetIconTextGap: Dp = 12.dp

    /** Card-rise vertical translation removed in Stage 29A (alpha fade only). */
    val CardRiseTranslationY: Dp = 0.dp
    val LiveDotSize: Dp = 8.dp
    val SecondaryButtonElevation: Dp = 0.dp
    val SecondaryButtonLipHeight: Dp = 0.dp
    /** Border width for secondary flat buttons (1.dp). */
    val SecondaryButtonBorderWidth: Dp = FlatBorderWidth
    val TopBarHeight: Dp = 64.dp
    val HeaderShadowOffsetY: Dp = 0.dp
    val HeaderShadowBlur: Dp = 0.dp
    val TabBarMinHeight: Dp = 56.dp
    val TabBarShadowOffsetY: Dp = 0.dp
    val TabBarShadowBlur: Dp = 0.dp
    val TabIndicatorHeight: Dp = 3.dp
    val TabIndicatorGlowOffsetY: Dp = 0.dp
    val TabIndicatorGlowBlur: Dp = 0.dp
    val TopAccentStripHeight: Dp = 4.dp

    // =========================================================================
    // REPORT & LEDGER STAGE 26F-1 PARITY TOKENS
    // =========================================================================
    val SectionHeaderHeight: Dp = 52.dp
    val SectionIconTileSize: Dp = 32.dp
    val KpiIconTileSize: Dp = 38.dp
    val SectionTileShadowOffsetY: Dp = FlatCardShadowOffsetY
    val SectionTileShadowBlur: Dp = FlatCardShadowBlur
    val SectionPillShadowOffsetY: Dp = FlatCardShadowOffsetY
    val SectionPillShadowBlur: Dp = FlatCardShadowBlur
    val SegmentTrackHeight: Dp = 52.dp
    val SegmentThumbHeight: Dp = 44.dp
    val SegmentActiveShadowOffsetY: Dp = 0.dp
    val SegmentActiveShadowBlur: Dp = 0.dp
    val ReportGridGap: Dp = 10.dp
    val ReportCardInnerPadding: Dp = 16.dp
    val LedgerSectionAccentWidth: Dp = 3.dp
    val LedgerSectionAccentHeight: Dp = 16.dp
    val AvatarBadgeSize: Dp = 42.dp
    val SerialBadgeSize: Dp = 36.dp

    // =========================================================================
    // PAID OUT, SETTINGS & EMPLOYEES STAGE 26F-2 PARITY TOKENS
    // =========================================================================
    /** Icon tile size for Paid Out KPI & entry rows (40.dp). */
    val PaidOutIconTileSize: Dp = 40.dp
    /** Icon tile size for Settings hub & sub-screen rows (42.dp). */
    val SettingsIconTileSize: Dp = 42.dp
    /** Avatar tile size for Employee list rows (44.dp). */
    val EmployeeAvatarTileSize: Dp = 44.dp
    /** Corner radius for 40dp/42dp icon tiles (13.dp). */
    val RadiusSettingsIconTile: Dp = 13.dp
    /** Corner radius for 44dp employee avatar tiles (14.dp). */
    val RadiusEmployeeAvatarTile: Dp = 14.dp
    /** Corner radius for 36dp secondary edit icon tiles (10.dp). */
    val RadiusActionIconTile: Dp = 10.dp
    /** Single soft card drop shadow Y-offset and blur for icon tiles (1.dp / 4.dp). */
    val IconTileShadowOffsetY: Dp = FlatCardShadowOffsetY
    val IconTileShadowBlur: Dp = FlatCardShadowBlur
    /** Inner bottom bevel removed in Stage 29A flat UI (0.dp). */
    val IconTileBottomBevelHeight: Dp = 0.dp
    /** Hairline row divider thickness inside grouped cards (1.dp). */
    val RowDividerWidth: Dp = 1.dp
    /** Horizontal gap between icon tile and row label column (12.dp). */
    val SettingsIconTitleGap: Dp = 12.dp
    /** Vertical row padding inside grouped list cards (12.dp). */
    val ListRowPaddingVertical: Dp = 12.dp
    /** Bottom sheet top corner radius (20.dp). */
    val BottomSheetCornerRadius: Dp = 20.dp

    // =========================================================================
    // AUTH & QUICK PIN PARITY TOKENS
    // =========================================================================
    val LogoTileSize: Dp = 48.dp
    val LogoTileIconSize: Dp = 24.dp
    val LogoTileShadowOffsetY: Dp = FlatCardShadowOffsetY
    val LogoTileShadowBlur: Dp = FlatCardShadowBlur

    val AuthScreenSidePadding: Dp = 32.dp
    val AuthCardMaxWidth: Dp = 360.dp
    val RegisterHeaderTopPadding: Dp = 28.dp
    val RegisterScreenMaxWidth: Dp = 380.dp
    val RememberCheckboxSize: Dp = 18.dp
    val RememberCheckboxRadius: Dp = 5.dp
    val RememberCheckboxBorderWidth: Dp = 1.5.dp

    val QuickPinMaxWidth: Dp = 380.dp
    val QuickPinSidePadding: Dp = 24.dp
    val PinCellGap: Dp = 8.dp
    val KeypadColumnGap: Dp = 14.dp
    val KeypadButtonHeight: Dp = 84.dp
    val KeypadShadowOffsetY: Dp = FlatCardShadowOffsetY
    val KeypadShadowBlur: Dp = FlatCardShadowBlur

    // =========================================================================
    // STAGE 26G SPEC GAP PARITY TOKENS
    // =========================================================================
    /** Width and height for Register dial-code picker button (78.dp x 46.dp). */
    val DialButtonWidth: Dp = 78.dp
    val DialButtonHeight: Dp = 46.dp
    /** Width for read-only country dial-code selector field (96.dp). */
    val DialCodeFieldWidth: Dp = 96.dp
    /** Bottom lip removed in Stage 29A flat UI (0.dp). */
    val DialButtonLipHeight: Dp = 0.dp
    /** Asymmetric start/end content padding for Register dial-code button (10.dp / 8.dp). */
    val DialButtonPaddingStart: Dp = 10.dp
    val DialButtonPaddingEnd: Dp = 8.dp

    /** Inner padding for Register privacy note box (12.dp); inset shadow removed in Stage 29A. */
    val RegisterNotePadding: Dp = 12.dp
    val RegisterNoteShadowOffsetY: Dp = 0.dp
    val RegisterNoteShadowBlur: Dp = 0.dp

    /** Inner padding for Password Recovery info block (10.dp). */
    val RecoveryInfoPadding: Dp = 10.dp
    /** Password Recovery icon tile size, radius, and flat shadow geometry (46.dp, 15.dp, 1.dp / 4.dp). */
    val RecoveryTileSize: Dp = 46.dp
    val RadiusRecoveryTile: Dp = 15.dp
    val RecoveryTileShadowOffsetY: Dp = FlatCardShadowOffsetY
    val RecoveryTileShadowBlur: Dp = FlatCardShadowBlur

    /** Inner top shadow removed on Report Entry read-only numeric wells in Stage 29A (0.dp / 0.dp). */
    val ReportReadOnlyShadowOffsetY: Dp = 0.dp
    val ReportReadOnlyShadowBlur: Dp = 0.dp

    // =========================================================================
    // STAGE 27A HOME DASHBOARD BOARD PARITY TOKENS
    // =========================================================================
    /** Overlap distance between top brand header and floating Shift status card (22.dp). */
    val HomeHeaderOverlapOffset: Dp = 22.dp
    /** Bottom padding inside top brand header beneath date to accommodate card overlap (36.dp). */
    val HomeHeaderBottomPadding: Dp = 36.dp
    /** Vertical section spacing between Home Dashboard cards (14.dp). */
    val HomeSectionGap: Dp = 14.dp
    /** Grid gap between 2x2 Home KPI cards (10.dp). */
    val HomeKpiGridGap: Dp = 10.dp
    /** Vertical gap between KPI tile label and hero KPI figure (10.dp). */
    val HomeKpiValueTopGap: Dp = 10.dp
    /** Proportional cash/bank/wallet split bar track height (10.dp), radius (7.dp), and gap (3.dp). */
    val HomeBreakdownTrackHeight: Dp = 10.dp
    val HomeBreakdownTrackRadius: Dp = 7.dp
    val HomeBreakdownSegmentGap: Dp = 3.dp
    /** Legend color swatch size (10.dp) and radius (3.dp) on Sales Summary breakdown rows. */
    val HomeBreakdownSwatchSize: Dp = 10.dp
    val HomeBreakdownSwatchRadius: Dp = 3.dp
    /** Draft / Review alert banner minimum height (72.dp). */
    val HomeDraftBannerMinHeight: Dp = 72.dp
    /** Last 7 days bar chart container height (132.dp), bar width (24.dp), radius (8.dp), and min bar height (6.dp). */
    val HomeChartHeight: Dp = 132.dp
    val HomeChartBarWidth: Dp = 24.dp
    val HomeChartBarRadius: Dp = 8.dp
    val HomeChartBarMinHeight: Dp = 6.dp
    /** Fixed vertical header band above chart bars for compact numeric value labels (18.dp). */
    val HomeChartValueLabelHeight: Dp = 18.dp
    /** Minimum height for Shift/LIVE status pills (26.dp). */
    val HomeStatusBadgeHeight: Dp = 26.dp
    /** Large inner padding for hero Sales Summary & Shift status cards (18.dp). */
    val HomeCardPaddingLarge: Dp = 18.dp
    /** Flat shadow tokens for Home Dashboard badges and banners (glows removed). */
    val HomeLiveBadgeShadowOffsetY: Dp = 0.dp
    val HomeLiveBadgeShadowBlur: Dp = 0.dp
    val HomeDraftBannerShadowOffsetY: Dp = FlatCardShadowOffsetY
    val HomeDraftBannerShadowBlur: Dp = FlatCardShadowBlur
    val HomeChartTodayGlowOffsetY: Dp = 0.dp
    val HomeChartTodayGlowBlur: Dp = 0.dp
}
