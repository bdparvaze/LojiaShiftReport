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
    /** Radius for Quick PIN square keypad keys (14.dp). */
    val RadiusKeypad: Dp = 14.dp
    /** Radius for app logo tile (15.dp). */
    val RadiusLogoTile: Dp = 15.dp
    val RadiusLg: Dp = 16.dp
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
    val MinTouchTarget: Dp = 48.dp

    /** Minimum height for compact register inset fields (46.dp). */
    val TextFieldHeightCompact: Dp = 46.dp
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

    val CardRiseTranslationY: Dp = 14.dp
    val LiveDotSize: Dp = 8.dp
    val SecondaryButtonElevation: Dp = 4.dp
    val SecondaryButtonLipHeight: Dp = 4.dp
    /** Border width for secondary depth buttons (1.5.dp). */
    val SecondaryButtonBorderWidth: Dp = 1.5.dp
    val TabBarMinHeight: Dp = 48.dp
    val TopAccentStripHeight: Dp = 4.dp

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

