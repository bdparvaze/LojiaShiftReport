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
    // DEPTH ELEVATION, BORDER & COMPONENT TOKENS
    // =========================================================================
    val CardElevation: Dp = 10.dp
    val CardAccentElevation: Dp = 12.dp
    /** Inner content padding for auth cards (18.dp). */
    val CardPaddingAuth: Dp = 18.dp
    /** Close ambient card shadow Y-offset and blur (1.dp / 2.dp). */
    val CardShadowCloseOffsetY: Dp = 1.dp
    val CardShadowCloseBlur: Dp = 2.dp
    /** Far directional card shadow Y-offset and blur (10.dp / 26.dp). */
    val CardShadowFarOffsetY: Dp = 10.dp
    val CardShadowFarBlur: Dp = 26.dp

    val RaisedButtonElevation: Dp = 8.dp
    val ButtonLipHeight: Dp = 4.dp
    /** Primary button shadow Y-offset and blur (8.dp / 16.dp). */
    val ButtonShadowOffsetY: Dp = 8.dp
    val ButtonShadowBlur: Dp = 16.dp
    /** Vertical press offset when raised/keypad buttons are pressed (2.dp). */
    val ButtonPressedTranslationY: Dp = 2.dp

    val CardBorderWidth: Dp = 1.dp
    val InsetBorderWidth: Dp = 1.dp
    /** Border width for compact register inset fields (1.5.dp). */
    val InsetRegisterBorderWidth: Dp = 1.5.dp
    /** Border width for focused and error inset fields (2.dp). */
    val InsetFocusedBorderWidth: Dp = 2.dp
    val InsetShadowHeight: Dp = 4.dp
    /** Inner top shadow offset and blur for inset wells (2.dp / 4.dp). */
    val InsetShadowOffsetY: Dp = 2.dp
    val InsetShadowBlur: Dp = 4.dp
    /** Horizontal padding inside standard inset fields (14.dp). */
    val InsetFieldPaddingHorizontal: Dp = 14.dp
    /** Horizontal padding inside register inset fields (12.dp). */
    val RegisterFieldPaddingHorizontal: Dp = 12.dp
    /** Horizontal gap between leading icon and text in inset fields (12.dp). */
    val InsetIconTextGap: Dp = 12.dp

    val CardRiseTranslationY: Dp = 16.dp
    val LiveDotSize: Dp = 8.dp
    val SecondaryButtonElevation: Dp = 4.dp
    val SecondaryButtonLipHeight: Dp = 4.dp
    /** Border width for secondary depth buttons (1.5.dp). */
    val SecondaryButtonBorderWidth: Dp = 1.5.dp
    val TopBarHeight: Dp = 64.dp
    val HeaderShadowOffsetY: Dp = 4.dp
    val HeaderShadowBlur: Dp = 14.dp
    val TabBarMinHeight: Dp = 56.dp
    val TabBarShadowOffsetY: Dp = 3.dp
    val TabBarShadowBlur: Dp = 8.dp
    val TabIndicatorHeight: Dp = 3.dp
    val TabIndicatorGlowOffsetY: Dp = (-2).dp
    val TabIndicatorGlowBlur: Dp = 8.dp
    val TopAccentStripHeight: Dp = 4.dp

    // =========================================================================
    // REPORT & LEDGER STAGE 26F-1 PARITY TOKENS
    // =========================================================================
    val SectionHeaderHeight: Dp = 52.dp
    val SectionIconTileSize: Dp = 32.dp
    val KpiIconTileSize: Dp = 38.dp
    val SectionTileShadowOffsetY: Dp = 2.dp
    val SectionTileShadowBlur: Dp = 5.dp
    val SectionPillShadowOffsetY: Dp = 3.dp
    val SectionPillShadowBlur: Dp = 8.dp
    val SegmentTrackHeight: Dp = 52.dp
    val SegmentThumbHeight: Dp = 44.dp
    val SegmentActiveShadowOffsetY: Dp = 3.dp
    val SegmentActiveShadowBlur: Dp = 8.dp
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
    /** Corner radius for 40dp/42dp SH1 gradient icon tiles (13.dp). */
    val RadiusSettingsIconTile: Dp = 13.dp
    /** Corner radius for 44dp SH1 gradient employee avatar tiles (14.dp). */
    val RadiusEmployeeAvatarTile: Dp = 14.dp
    /** Corner radius for 36dp secondary edit icon tiles (10.dp). */
    val RadiusActionIconTile: Dp = 10.dp
    /** Outer drop shadow Y-offset and blur for SH1 raised icon tiles (4.dp / 8.dp). */
    val IconTileShadowOffsetY: Dp = 4.dp
    val IconTileShadowBlur: Dp = 8.dp
    /** Inner bottom bevel thickness for SH1 raised icon tiles (2.dp). */
    val IconTileBottomBevelHeight: Dp = 2.dp
    /** Hairline row divider thickness inside grouped cards (1.dp). */
    val RowDividerWidth: Dp = 1.dp
    /** Horizontal gap between SH1 icon tile and row label column (12.dp). */
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
    val LogoTileShadowOffsetY: Dp = 6.dp
    val LogoTileShadowBlur: Dp = 14.dp

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
    val KeypadShadowOffsetY: Dp = 8.dp
    val KeypadShadowBlur: Dp = 14.dp
}

