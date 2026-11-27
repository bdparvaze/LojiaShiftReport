package com.lojia.shiftreport.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Central color, gradient, shadow, and alpha tokens for Lojia's soft-depth visual system.
 * All depth values live exclusively in `ui/theme/` so screens inherit updates from one source.
 */
object DepthTokens {

    // ============ PAGE BACKGROUND ============
    /** Soft blue-tinted canvas background behind auth, register, Quick PIN, and lock screens (#F4F7FD). */
    val PageBackground: Color = BackgroundLight

    /** Report Entry, Due History, and Employer Advances canvas background (#EEF2FB). */
    val ReportPageBackground: Color = ReportCanvasBackground

    // ============ BRAND SOLID COLORS (HEADER & PRIMARY BUTTONS) ============
    /** Flat solid primary header color (no gradient). */
    val BrandHeaderGradientStart: Color = PrimaryBlueLight

    /** Flat solid primary header color (no gradient). */
    val BrandHeaderGradientEnd: Color = PrimaryBlueLight

    /** Flat solid primary button color (#1D4FD8). */
    val BrandButtonGradientStart: Color = PrimaryBlueLight

    /** Flat solid primary button color (#1D4FD8). */
    val BrandButtonGradientEnd: Color = PrimaryBlueLight

    // ============ FLAT SURFACE & CARD TOKENS ============
    /** Solid flat white fill for standard cards (#FFFFFF). */
    val FlatCardFill: Color = PureWhite

    /** Solid flat accent fill for highlighted cards (#EFF6FF). */
    val FlatCardAccentSurface: Color = FlatCardAccentFill

    /** Single soft shadow tint for flat elevated cards and primary buttons (#000000). */
    val FlatCardShadowColor: Color = PureBlack

    /** Single soft shadow opacity (6% black) beneath flat elevated cards and primary buttons. */
    const val FLAT_CARD_SHADOW_ALPHA: Float = 0.06f

    /** Flat solid primary button fill and 8% darkened pressed fill. */
    val FlatPrimaryButtonFill: Color = PrimaryBlueLight
    val FlatPrimaryButtonPressedFill: Color = FlatPrimaryPressed

    /** Flat solid destructive button fill and 8% darkened pressed fill. */
    val FlatDestructiveButtonFill: Color = ErrorRedLight
    val FlatDestructiveButtonPressedFill: Color = FlatDestructivePressed

    /** Pressed fill tint alpha (6%) over white for secondary flat buttons. */
    const val FLAT_SECONDARY_PRESSED_ALPHA: Float = 0.06f

    /** Solid card fill matching FlatCardFill (#FFFFFF). */
    val CardGradientStart: Color = FlatCardFill

    /** Solid card fill matching FlatCardFill (#FFFFFF). */
    val CardGradientEnd: Color = FlatCardFill

    /** Solid fill for highlighted/accent cards (#EFF6FF). */
    val CardAccentGradientStart: Color = FlatCardAccentSurface

    /** Solid fill for highlighted/accent cards (#EFF6FF). */
    val CardAccentGradientEnd: Color = FlatCardAccentSurface

    // ============ FLAT CARD SHADOW & BORDER TOKENS ============
    /** Single soft shadow tint rgba(0, 0, 0, 0.06) beneath flat cards. */
    val CardShadowCloseColor: Color = FlatCardShadowColor

    /** Secondary shadow color aliased to FlatCardShadowColor (spot alpha = 0). */
    val RaisedCardShadowColor: Color = FlatCardShadowColor

    /** 1dp crisp perimeter border color for flat cards (#E3E8F0). */
    val RaisedCardBorderColor: Color = FlatBorder

    /** Border color for highlighted accent flat cards. */
    val RaisedCardAccentBorderColor: Color = PrimaryLight

    /** Single soft card shadow alpha rgba(0, 0, 0, 0.06). */
    const val RAISED_CARD_AMBIENT_ALPHA: Float = FLAT_CARD_SHADOW_ALPHA

    /** Second directional card shadow disabled in Stage 29A flat UI (0.0f). */
    const val RAISED_CARD_SPOT_ALPHA: Float = 0.0f

    /** Hairline border alpha for standard flat cards (solid 1.0f). */
    const val RAISED_CARD_BORDER_ALPHA: Float = 1.0f

    /** Accent card border alpha for highlighted KPI and summary cards. */
    const val RAISED_CARD_ACCENT_BORDER_ALPHA: Float = 0.35f

    /** Top white highlight removed in Stage 29A flat UI (0.0f). */
    const val RAISED_CARD_TOP_SHEEN_ALPHA: Float = 0.0f

    // ============ FLAT PRIMARY BUTTON TOKENS ============
    /** Lip removed in Stage 29A flat UI; kept matching FlatPrimaryButtonFill for binary compatibility. */
    val RaisedButtonLipColor: Color = FlatPrimaryButtonFill

    /** Flat disabled fill for primary buttons. */
    val RaisedButtonDisabledStart: Color = SurfaceVariantLight

    /** Flat disabled fill for primary buttons. */
    val RaisedButtonDisabledEnd: Color = SurfaceVariantLight

    /** Disabled button lip color (unused in flat UI). */
    val RaisedButtonLipDisabledColor: Color = OutlineLight

    /** Elevation shadow tint beneath enabled flat primary buttons (#000000 @ 0.06f). */
    val RaisedButtonShadowColor: Color = FlatCardShadowColor

    /** Lip opacity disabled in Stage 29A flat UI (0.0f). */
    const val RAISED_BUTTON_LIP_ALPHA: Float = 0.0f

    /** Disabled lip opacity disabled in Stage 29A flat UI (0.0f). */
    const val RAISED_BUTTON_LIP_DISABLED_ALPHA: Float = 0.0f

    /** Soft drop-shadow opacity rgba(0, 0, 0, 0.06) beneath enabled flat primary buttons. */
    const val RAISED_BUTTON_SHADOW_ALPHA: Float = FLAT_CARD_SHADOW_ALPHA

    /** Top white highlight disabled in Stage 29A flat UI (0.0f). */
    const val RAISED_BUTTON_TOP_HIGHLIGHT_ALPHA: Float = 0.0f

    // ============ SECONDARY & DESTRUCTIVE FLAT BUTTON TOKENS ============
    /** Flat white fill for secondary buttons (#FFFFFF). */
    val SecondaryButtonGradientStart: Color = PureWhite

    /** Flat white fill for secondary buttons (#FFFFFF). */
    val SecondaryButtonGradientEnd: Color = PureWhite

    /** Secondary lip color (unused in Stage 29A flat UI). */
    val SecondaryButtonLipColor: Color = PureWhite

    /** 1dp perimeter border color for secondary flat buttons (#1D4FD8). */
    val SecondaryButtonBorderColor: Color = PrimaryBlueLight

    /** Secondary button shadow color (alpha = 0). */
    val SecondaryButtonShadowColor: Color = FlatCardShadowColor

    /** Flat fill for disabled secondary buttons. */
    val SecondaryButtonDisabledStart: Color = SurfaceVariantLight

    /** Flat fill for disabled secondary buttons. */
    val SecondaryButtonDisabledEnd: Color = SurfaceVariantLight

    /** Secondary buttons have no drop shadow in Stage 29A flat UI. */
    const val SECONDARY_BUTTON_SHADOW_ALPHA: Float = 0.0f

    /** Secondary buttons have no lower lip in Stage 29A flat UI. */
    const val SECONDARY_BUTTON_LIP_ALPHA: Float = 0.0f

    /** Disabled secondary buttons have no lower lip in Stage 29A flat UI. */
    const val SECONDARY_BUTTON_LIP_DISABLED_ALPHA: Float = 0.0f

    /** Solid fill for destructive flat buttons (#B91C1C). */
    val DestructiveButtonGradientStart: Color = FlatDestructiveButtonFill

    /** Solid fill for destructive flat buttons (#B91C1C). */
    val DestructiveButtonGradientEnd: Color = FlatDestructiveButtonFill

    /** Destructive lip color (unused in Stage 29A flat UI). */
    val DestructiveButtonLipColor: Color = FlatDestructiveButtonFill

    /** Elevation shadow tint beneath destructive flat buttons (#000000 @ 0.06f). */
    val DestructiveButtonShadowColor: Color = FlatCardShadowColor

    // ============ FLAT INSET FIELD TOKENS ============
    /** Solid white fill for flat input fields (#FFFFFF). */
    val InsetFieldGradientStart: Color = PureWhite

    /** Solid white fill for flat input fields (#FFFFFF). */
    val InsetFieldBackground: Color = PureWhite

    /** Solid white fill when a flat input field holds focus (#FFFFFF). */
    val InsetFieldFocusedBackground: Color = PureWhite

    /** Fill background when an input field is in error state. */
    val InsetFieldErrorBackground: Color = NegativeBannerSurface

    /** Fill background when an input field is disabled. */
    val InsetFieldDisabledBackground: Color = SurfaceVariantLight

    /** Inner shadow color (unused in Stage 29A flat UI; alpha = 0). */
    val InsetFieldShadowColor: Color = FlatCardShadowColor

    /** Perimeter 1dp stroke color for unfocused flat input fields (#D5DBE6). */
    val InsetFieldBorderColor: Color = FlatInsetBorder

    /** Perimeter 1.5dp stroke color when an input field holds focus (#1D4FD8). */
    val InsetFieldFocusedBorderColor: Color = PrimaryBlueLight

    /** Perimeter 1.5dp stroke color when an input field is in error state. */
    val InsetFieldErrorBorderColor: Color = ErrorRedLight

    /** Perimeter 1dp stroke color when an input field is disabled. */
    val InsetFieldDisabledBorderColor: Color = OutlineVariantLight

    /** Bottom bevel highlight color (unused in Stage 29A flat UI; alpha = 0). */
    val InsetFieldHighlightColor: Color = PureWhite

    /** Standard inset icon color (#475467). */
    val InsetIconColor: Color = OnSurfaceVariantLight

    /** Accessible grey hint/placeholder color (#64748B, WCAG >= 4.5:1 on white). */
    val InsetPlaceholderColor: Color = TextHintColor

    /** Accessible grey icon color for Register compact fields (#64748B). */
    val RegisterIconColor: Color = TextHintColor

    /** Accessible grey placeholder color for Register compact fields (#64748B, WCAG >= 4.5:1 on white). */
    val RegisterPlaceholderColor: Color = TextHintColor

    /** Inner shadows and bevels removed in Stage 29A flat UI (0.0f). */
    const val INSET_FIELD_TOP_SHADOW_ALPHA: Float = 0.0f
    const val INSET_FIELD_SIDE_SHADOW_ALPHA: Float = 0.0f
    const val INSET_FIELD_BORDER_ALPHA: Float = 1.0f
    const val INSET_FIELD_BOTTOM_HIGHLIGHT_ALPHA: Float = 0.0f

    // ============ LOGO TILE, PIN CELLS & KEYPAD TOKENS ============
    /** Solid white fill for the 48dp flat app logo tile (#FFFFFF). */
    val LogoTileGradientStart: Color = FlatCardFill

    /** Solid white fill for the 48dp flat app logo tile (#FFFFFF). */
    val LogoTileGradientEnd: Color = FlatCardFill

    /** Soft drop shadow tint beneath the 48dp app logo tile (#000000 @ 0.06f). */
    val LogoTileShadowColor: Color = FlatCardShadowColor

    /** Soft drop shadow opacity beneath the 48dp app logo tile. */
    const val LOGO_TILE_SHADOW_ALPHA: Float = FLAT_CARD_SHADOW_ALPHA

    /** Inner bottom bevel removed in Stage 29A flat UI (0.0f). */
    const val LOGO_TILE_BOTTOM_EDGE_ALPHA: Float = 0.0f

    /** Solid fill for filled Quick PIN cells (#FFFFFF). */
    val PinCellFilledGradientStart: Color = PureWhite

    /** Solid accent tint for filled Quick PIN cells (#EFF6FF). */
    val PinCellFilledGradientEnd: Color = FlatCardAccentSurface

    /** Aspect ratio multiplier defining Quick PIN cell height = width * 1.15f. */
    const val PIN_CELL_HEIGHT_MULTIPLIER: Float = 1.15f

    /** Width-to-height ratio (width / height = 1 / 1.15) for Compose Modifier.aspectRatio. */
    const val PIN_CELL_ASPECT_RATIO: Float = 1f / PIN_CELL_HEIGHT_MULTIPLIER

    /** Solid white fill for Quick PIN square keypad keys (#FFFFFF). */
    val KeypadButtonGradientStart: Color = PureWhite

    /** Solid white fill for Quick PIN square keypad keys (#FFFFFF). */
    val KeypadButtonGradientEnd: Color = PureWhite

    /** Border tint used for flat keypad keys (#E3E8F0). */
    val KeypadButtonLipColor: Color = FlatBorder

    /** Soft drop shadow tint beneath Quick PIN keypad keys (#000000 @ 0.06f). */
    val KeypadButtonShadowColor: Color = FlatCardShadowColor

    /** Soft drop shadow opacity rgba(0, 0, 0, 0.06) beneath Quick PIN keypad keys. */
    const val KEYPAD_BUTTON_SHADOW_ALPHA: Float = FLAT_CARD_SHADOW_ALPHA

    // ============ TAB BAR TOKENS ============
    /** Solid fill for flat tab bar containers (#FFFFFF). */
    val TabBarGradientStart: Color = PureWhite

    /** Solid fill for flat tab bar containers (#FFFFFF). */
    val TabBarGradientEnd: Color = PureWhite

    /** Solid color for active tab indicator bar (#1D4FD8). */
    val TabIndicatorGradientStart: Color = PrimaryBlueLight

    /** Solid color for active tab indicator bar (#1D4FD8). */
    val TabIndicatorGradientEnd: Color = PrimaryBlueLight

    // ============ REPORT & LEDGER FLAT TOKENS ============
    /** Top header drop shadow disabled in Stage 29A flat UI (alpha = 0). */
    val HeaderShadowColor: Color = FlatCardShadowColor
    const val HEADER_SHADOW_ALPHA: Float = 0.0f

    /** Tab bar drop shadow disabled in Stage 29A flat UI (alpha = 0). */
    val TabBarShadowColor: Color = FlatCardShadowColor
    const val TAB_BAR_SHADOW_ALPHA: Float = 0.0f

    /** Active tab indicator glow disabled in Stage 29A flat UI (alpha = 0). */
    val TabIndicatorGlowColor: Color = PrimaryBlueLight
    const val TAB_INDICATOR_GLOW_ALPHA: Float = 0.0f

    /** Unfocused 1dp border color on Report Entry & search fields (#D5DBE6). */
    val ReportFieldBorderColor: Color = FlatInsetBorder

    /** Primary ink on Report Entry & Ledger screens (#101828). */
    val ReportFieldTextColor: Color = TextPrimaryReport

    /** Slate 700 label color on Report Entry & Ledger screens (#344054). */
    val ReportFieldLabelColor: Color = TextSlate700

    /** Report placeholder ink (#64748B). */
    val ReportFieldPlaceholderColor: Color = TextHintColor

    /** Inner shadows removed on report fields & wells in Stage 29A flat UI (0.0f). */
    const val INSET_REPORT_TOP_SHADOW_ALPHA: Float = 0.0f

    /** Soft flat card shadow alpha for section accordion headers (0.06f). */
    const val SECTION_PILL_SHADOW_ALPHA: Float = FLAT_CARD_SHADOW_ALPHA

    /** Soft flat card shadow alpha for 32dp section icon tiles (0.06f). */
    const val SECTION_ICON_SHADOW_ALPHA: Float = FLAT_CARD_SHADOW_ALPHA

    /** Inner top highlight removed on section accordion headers in Stage 29A flat UI (0.0f). */
    const val SECTION_PILL_HIGHLIGHT_ALPHA: Float = 0.0f

    /** Inner top shadow removed on segmented period track in Stage 29A flat UI (0.0f). */
    const val SEGMENT_TRACK_INSET_ALPHA: Float = 0.0f

    /** Glow shadow removed on active segmented thumb in Stage 29A flat UI (0.0f). */
    const val SEGMENT_ACTIVE_SHADOW_ALPHA: Float = 0.0f

    // ============ PAID OUT, SETTINGS & EMPLOYEES FLAT TOKENS ============
    /** Hairline row divider gray (#EAECF0) inside grouped list & settings cards. */
    val ListDividerColor: Color = ListDividerGray

    /** Secondary recessed form well fill (#F4F6FB) and perimeter border (#D0D5DD). */
    val FormWellBackground: Color = FormWellFill
    val FormWellStrokeColor: Color = FormWellBorder

    /** Outer flat card shadow alpha beneath icon tiles rgba(0, 0, 0, 0.06). */
    const val ICON_TILE_SHADOW_ALPHA: Float = FLAT_CARD_SHADOW_ALPHA

    /** Specular highlight & bottom bevel removed on flat icon tiles in Stage 29A (0.0f). */
    const val ICON_TILE_TOP_HIGHLIGHT_ALPHA: Float = 0.0f
    const val ICON_TILE_BOTTOM_BEVEL_ALPHA: Float = 0.0f

    /** Inner top shadow removed for flat form wells in Stage 29A (0.0f). */
    const val INSET_WELL_TOP_SHADOW_ALPHA: Float = 0.0f

    // ============ HEADER & LIVE INDICATOR TOKENS ============
    /** Specular sheen removed from flat headers in Stage 29A (0.0f). */
    const val HEADER_SHEEN_ALPHA: Float = 0.0f

    /** Active dot color for LIVE status indicators. */
    val LiveDotColor: Color = LiveDotGlowGreen

    /** Pulse halo disabled in Stage 29A flat UI (static clean dot). */
    const val LIVE_HALO_START_ALPHA: Float = 0.0f
    const val LIVE_HALO_END_ALPHA: Float = 0.0f
    const val LIVE_HALO_STEADY_ALPHA: Float = 0.0f
    const val LIVE_HALO_MAX_SCALE: Float = 1.0f

    // ============ STAGE 26G SPEC GAP FLAT TOKENS ============
    /** Register dial-code button flat white fill, text ink (#334155), and 1dp border (#D5DBE6). */
    val DialButtonTextColor: Color = DialCodeTextColor
    val DialButtonBorderColor: Color = FlatInsetBorder
    val DialButtonLipColor: Color = PureWhite
    val DialButtonGradientStart: Color = PureWhite
    val DialButtonGradientEnd: Color = PureWhite

    /** Register info/privacy note block fill (#F1F5FB), border (#E2E8F0); inset shadow disabled (0.0f). */
    val RegisterNoteBackground: Color = RegisterNoteFill
    val RegisterNoteBorderColor: Color = OutlineVariantLight
    const val REGISTER_NOTE_SHADOW_ALPHA: Float = 0.0f

    /** Password recovery info block fill (#EEF3FF), border (#D0DDFF), and flat 46dp icon tile fill. */
    val RecoveryInfoBackground: Color = CashWellBg
    val RecoveryInfoBorderColor: Color = RecoveryInfoBorder
    val RecoveryTileGradientStart: Color = CashWellBg
    val RecoveryTileGradientEnd: Color = CashWellBg

    /** Report Entry read-only/disabled box fill (#EEF1F7), border (#D5DBE6), text ink (#64748B); inset disabled (0.0f). */
    val ReportReadOnlyBackground: Color = ReportReadOnlyFill
    val ReportReadOnlyBorderColor: Color = FlatInsetBorder
    val ReportReadOnlyTextColor: Color = TextHintColor
    const val REPORT_READ_ONLY_INSET_ALPHA: Float = 0.0f

    // ============ STAGE 27A HOME DASHBOARD FLAT TOKENS ============
    /** Greeting text color over primary brand header (#DCE6FF). */
    val HomeHeaderGreetingColor: Color = HomeGreetingTextColor

    /** LIVE badge flat emerald pill (#D1FADF), border (#BBF7D0), ink (#05603A), glow disabled (0.0f). */
    val LiveBadgeBgStart: Color = BadgeGreenBg
    val LiveBadgeBgEnd: Color = BadgeGreenBg
    val LiveBadgeBorder: Color = PositiveBannerBorder
    val LiveBadgeText: Color = BadgeGreenText
    const val LIVE_BADGE_SHADOW_ALPHA: Float = 0.0f

    /** Flat payment split bar fills for Bank (solid teal #0E9384) and Wallet (solid amber #F79009). */
    val BankSegmentGradientStart: Color = SegmentTealEnd
    val BankSegmentGradientEnd: Color = SegmentTealEnd
    val WalletSegmentGradientStart: Color = SegmentAmberEnd
    val WalletSegmentGradientEnd: Color = SegmentAmberEnd

    /** Draft / Review amber banner soft card shadow alpha (0.06f). */
    const val DRAFT_BANNER_SHADOW_ALPHA: Float = FLAT_CARD_SHADOW_ALPHA

    /** Last 7 days bar chart flat solid prior-day fill (#D3E0FF) and today glow disabled (0.0f). */
    val ChartBarInactiveGradientStart: Color = ChartBarInactiveStart
    val ChartBarInactiveGradientEnd: Color = ChartBarInactiveStart
    const val CHART_TODAY_GLOW_ALPHA: Float = 0.0f
}

/**
 * Motion duration, stagger, press scale, and cap constants for Lojia transitions.
 */
object DepthMotionTokens {
    /** Duration (ms) for a single one-shot card-rise fade-in entrance animation. */
    const val CARD_RISE_DURATION_MS: Int = 550

    /** Incremental stagger delay (ms) per list card index. */
    const val CARD_RISE_STAGGER_STEP_MS: Int = 100

    /** Maximum total stagger delay (ms) so long lists never stall before appearing. */
    const val CARD_RISE_MAX_DELAY_MS: Int = 600

    /** Easing curve (0.2, 0.8, 0.2, 1) for card-rise fade animations. */
    val CardRiseEasing: CubicBezierEasing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)

    /** Color press transition duration (ms) for flat buttons. */
    const val PRESS_DURATION_MS: Int = 150

    /** Scale disabled in Stage 29A flat UI (1.0f). */
    const val BUTTON_PRESSED_SCALE: Float = 1.0f

    /** Period constant retained for binary compatibility (LIVE dot is static in Stage 29A). */
    const val LIVE_PULSE_PERIOD_MS: Int = 1400

    /** Hero net sales one-shot count-up duration (ms) when animations are enabled. */
    const val HERO_COUNTUP_DURATION_MS: Int = 650

    /** 7-day bar chart bar grow-up duration (ms) and per-bar stagger (ms). */
    const val CHART_BAR_GROW_DURATION_MS: Int = 550
    const val CHART_BAR_STAGGER_MS: Int = 45
}

/**
 * Exact type scale constants matching the approved Stage 26D & Stage 26F-1 design boards.
 */
object DepthTypeTokens {
    val LogoTitleSize: TextUnit = 22.sp
    val LogoTitleLetterSpacing: TextUnit = 0.2.sp
    val LogoSubtitleSize: TextUnit = 10.5.sp
    val LogoSubtitleLetterSpacing: TextUnit = 1.5.sp

    val InsetPlaceholderSize: TextUnit = 13.5.sp
    val RegisterPlaceholderSize: TextUnit = 13.sp
    val RegisterLabelSize: TextUnit = 12.sp

    val PrimaryButtonTextSize: TextUnit = 15.sp
    val SecondaryButtonTextSize: TextUnit = 14.sp
    val AuthRowLinkTextSize: TextUnit = 12.5.sp
    val AuthFooterTextSize: TextUnit = 13.sp

    val QuickPinTitleSize: TextUnit = 22.sp
    val QuickPinSubtitleSize: TextUnit = 13.5.sp
    val QuickPinDotSize: TextUnit = 22.sp
    val KeypadDigitSize: TextUnit = 28.sp
    val KeypadDeleteTextSize: TextUnit = 16.sp
    val QuickPinFallbackTextSize: TextUnit = 14.sp

    val TopBarTitleSize: TextUnit = 16.sp
    val TabLabelSize: TextUnit = 13.sp
    val ReportFieldLabelSize: TextUnit = 12.sp
    val ReportFieldTextSize: TextUnit = 14.sp
    val SectionTitleSize: TextUnit = 13.sp
    val HeroAmountSize: TextUnit = 20.sp

    val SettingsSectionHeaderSize: TextUnit = 12.sp
    val SettingsSectionHeaderTracking: TextUnit = 0.48.sp
    val ListRowTitleSize: TextUnit = 13.sp
    val ListRowSubtitleSize: TextUnit = 12.sp
    val CardSectionHeadingSize: TextUnit = 13.sp
    val PrimaryCtaSmallSize: TextUnit = 13.sp
    val ChipLabelSize: TextUnit = 11.sp
    val SegmentedChipTextSize: TextUnit = 12.sp
    val StatCountSize: TextUnit = 18.sp
    val EmptyStateTitleSize: TextUnit = 16.sp
    /** Register dial-code button text size (13.5.sp). */
    val DialButtonTextSize: TextUnit = 13.5.sp

    // ============ STAGE 27A HOME DASHBOARD TYPOGRAPHY ============
    val HomeGreetingSize: TextUnit = 14.sp
    val HomeDateTitleSize: TextUnit = 18.sp
    val HomeHeroNetSalesSize: TextUnit = 34.sp
    val HomeKpiValueSize: TextUnit = 20.sp
    val HomeLiveBadgeSize: TextUnit = 11.sp
    val HomeLiveBadgeTracking: TextUnit = 0.44.sp
    val HomeChartValueSize: TextUnit = 10.5.sp
    val HomeChartDayLabelSize: TextUnit = 11.sp

    // ============ BATCH A: LOGO PICKER AVATAR & PRESET EMOJI ============
    val BusinessLogoInitialSize: TextUnit = 36.sp
    val PresetLogoEmojiSize: TextUnit = 22.sp

    // ============ BATCH D: AUTH, REGISTER & RECOVERY PARITY TOKENS ============
    val AuthFieldLabelSize: TextUnit = 13.sp
    val AuthFieldInputSize: TextUnit = 14.sp
    val AuthErrorBannerTextSize: TextUnit = 12.sp
    val AuthSectionHeaderTitleSize: TextUnit = 13.5.sp
    val AuthSectionHeaderSubtitleSize: TextUnit = 10.8.sp
    val AuthInfoDotTextSize: TextUnit = 8.5.sp
    val AuthTooltipTextSize: TextUnit = 10.sp
    val AuthValidationTextSize: TextUnit = 11.sp
    val AuthInputLineHeight: TextUnit = 18.sp
    val AuthCountryHeaderSize: TextUnit = 15.sp
    val RegisterTermsTextSize: TextUnit = 11.5.sp
    val RegisterTermsLineHeight: TextUnit = 17.sp
    val RegisterSslTextSize: TextUnit = 10.sp
    val RegisterSuccessTitleSize: TextUnit = 22.sp
    val RegisterSuccessSubSize: TextUnit = 13.sp
    val RegisterSuccessSubLineHeight: TextUnit = 20.sp
    val RecoveryBodySize: TextUnit = 13.sp
    val RecoveryQuestionHeaderSize: TextUnit = 11.sp
}

/**
 * Pure decision function returning whether animations should run for a system
 * `ANIMATOR_DURATION_SCALE` value. Returns false at 0f (reduced motion) or invalid values.
 */
fun shouldAnimate(animatorDurationScale: Float): Boolean {
    return animatorDurationScale.isFinite() && animatorDurationScale > 0f
}

/**
 * Pure helper computing the bounded card-rise stagger delay (ms) for [index].
 */
fun cardRiseDelayMillis(
    index: Int,
    stepMs: Int = DepthMotionTokens.CARD_RISE_STAGGER_STEP_MS,
    maxDelayMs: Int = DepthMotionTokens.CARD_RISE_MAX_DELAY_MS
): Int {
    if (index <= 0 || stepMs <= 0 || maxDelayMs <= 0) return 0
    return (index.toLong() * stepMs.toLong())
        .coerceAtMost(maxDelayMs.toLong())
        .toInt()
}

/**
 * Material 3 Shape scale derived strictly from [Dimens] corner radius tokens.
 */
val LojiaShapes = Shapes(
    extraSmall = RoundedCornerShape(Dimens.RadiusXs),
    small = RoundedCornerShape(Dimens.RadiusSm),
    medium = RoundedCornerShape(Dimens.RadiusMd),
    large = RoundedCornerShape(Dimens.RadiusLg),
    extraLarge = RoundedCornerShape(Dimens.RadiusXl)
)

/**
 * Consolidated flat-theme style provided via [LocalDepthStyle] inside [LojiaTheme].
 */
@Immutable
data class DepthStyle(
    val pageBackground: Color = DepthTokens.PageBackground,
    val reportPageBackground: Color = DepthTokens.ReportPageBackground,
    val listDividerColor: Color = DepthTokens.ListDividerColor,
    val headerFillColor: Color = PrimaryBlueLight,
    val headerGradientStart: Color = DepthTokens.BrandHeaderGradientStart,
    val headerGradientEnd: Color = DepthTokens.BrandHeaderGradientEnd,
    val headerShadowColor: Color = DepthTokens.HeaderShadowColor,
    val headerShadowAlpha: Float = DepthTokens.HEADER_SHADOW_ALPHA,
    val headerSheenAlpha: Float = DepthTokens.HEADER_SHEEN_ALPHA,
    val buttonFillColor: Color = DepthTokens.FlatPrimaryButtonFill,
    val buttonPressedFillColor: Color = DepthTokens.FlatPrimaryButtonPressedFill,
    val destructiveButtonFillColor: Color = DepthTokens.FlatDestructiveButtonFill,
    val destructiveButtonPressedFillColor: Color = DepthTokens.FlatDestructiveButtonPressedFill,
    val buttonGradientStart: Color = DepthTokens.BrandButtonGradientStart,
    val buttonGradientEnd: Color = DepthTokens.BrandButtonGradientEnd,
    val buttonLipColor: Color = DepthTokens.RaisedButtonLipColor,
    val buttonLipAlpha: Float = DepthTokens.RAISED_BUTTON_LIP_ALPHA,
    val buttonDisabledStart: Color = DepthTokens.RaisedButtonDisabledStart,
    val buttonDisabledEnd: Color = DepthTokens.RaisedButtonDisabledEnd,
    val buttonLipDisabledColor: Color = DepthTokens.RaisedButtonLipDisabledColor,
    val buttonLipDisabledAlpha: Float = DepthTokens.RAISED_BUTTON_LIP_DISABLED_ALPHA,
    val buttonShadowColor: Color = DepthTokens.RaisedButtonShadowColor,
    val buttonShadowAlpha: Float = DepthTokens.RAISED_BUTTON_SHADOW_ALPHA,
    val buttonTopHighlightAlpha: Float = DepthTokens.RAISED_BUTTON_TOP_HIGHLIGHT_ALPHA,
    val buttonElevation: Dp = Dimens.RaisedButtonElevation,
    val buttonShadowOffsetY: Dp = Dimens.ButtonShadowOffsetY,
    val buttonShadowBlur: Dp = Dimens.ButtonShadowBlur,
    val buttonLipHeight: Dp = Dimens.ButtonLipHeight,
    val buttonCornerRadius: Dp = Dimens.RadiusMd,
    val buttonPressedTranslationY: Dp = Dimens.ButtonPressedTranslationY,
    val buttonPressedScale: Float = DepthMotionTokens.BUTTON_PRESSED_SCALE,
    val secondaryButtonGradientStart: Color = DepthTokens.SecondaryButtonGradientStart,
    val secondaryButtonGradientEnd: Color = DepthTokens.SecondaryButtonGradientEnd,
    val secondaryButtonLipColor: Color = DepthTokens.SecondaryButtonLipColor,
    val secondaryButtonBorderColor: Color = DepthTokens.SecondaryButtonBorderColor,
    val secondaryButtonBorderWidth: Dp = Dimens.SecondaryButtonBorderWidth,
    val secondaryButtonShadowColor: Color = DepthTokens.SecondaryButtonShadowColor,
    val secondaryButtonDisabledStart: Color = DepthTokens.SecondaryButtonDisabledStart,
    val secondaryButtonDisabledEnd: Color = DepthTokens.SecondaryButtonDisabledEnd,
    val secondaryButtonShadowAlpha: Float = DepthTokens.SECONDARY_BUTTON_SHADOW_ALPHA,
    val secondaryButtonLipAlpha: Float = DepthTokens.SECONDARY_BUTTON_LIP_ALPHA,
    val secondaryButtonLipDisabledAlpha: Float = DepthTokens.SECONDARY_BUTTON_LIP_DISABLED_ALPHA,
    val secondaryButtonElevation: Dp = Dimens.SecondaryButtonElevation,
    val secondaryButtonLipHeight: Dp = Dimens.SecondaryButtonLipHeight,
    val destructiveButtonGradientStart: Color = DepthTokens.DestructiveButtonGradientStart,
    val destructiveButtonGradientEnd: Color = DepthTokens.DestructiveButtonGradientEnd,
    val destructiveButtonLipColor: Color = DepthTokens.DestructiveButtonLipColor,
    val destructiveButtonShadowColor: Color = DepthTokens.DestructiveButtonShadowColor,
    val cardFillColor: Color = DepthTokens.FlatCardFill,
    val cardAccentFillColor: Color = DepthTokens.FlatCardAccentSurface,
    val cardGradientStart: Color = DepthTokens.CardGradientStart,
    val cardGradientEnd: Color = DepthTokens.CardGradientEnd,
    val cardAccentGradientStart: Color = DepthTokens.CardAccentGradientStart,
    val cardAccentGradientEnd: Color = DepthTokens.CardAccentGradientEnd,
    val cardShadowCloseColor: Color = DepthTokens.CardShadowCloseColor,
    val cardShadowColor: Color = DepthTokens.RaisedCardShadowColor,
    val cardAmbientShadowAlpha: Float = DepthTokens.RAISED_CARD_AMBIENT_ALPHA,
    val cardSpotShadowAlpha: Float = DepthTokens.RAISED_CARD_SPOT_ALPHA,
    val cardShadowCloseOffsetY: Dp = Dimens.CardShadowCloseOffsetY,
    val cardShadowCloseBlur: Dp = Dimens.CardShadowCloseBlur,
    val cardShadowFarOffsetY: Dp = Dimens.CardShadowFarOffsetY,
    val cardShadowFarBlur: Dp = Dimens.CardShadowFarBlur,
    val cardBorderColor: Color = DepthTokens.RaisedCardBorderColor,
    val cardBorderAlpha: Float = DepthTokens.RAISED_CARD_BORDER_ALPHA,
    val cardAccentBorderColor: Color = DepthTokens.RaisedCardAccentBorderColor,
    val cardAccentBorderAlpha: Float = DepthTokens.RAISED_CARD_ACCENT_BORDER_ALPHA,
    val cardTopSheenAlpha: Float = DepthTokens.RAISED_CARD_TOP_SHEEN_ALPHA,
    val cardBorderWidth: Dp = Dimens.CardBorderWidth,
    val cardElevation: Dp = Dimens.CardElevation,
    val cardAccentElevation: Dp = Dimens.CardAccentElevation,
    val cardCornerRadius: Dp = Dimens.CardCornerRadius,
    val insetGradientStart: Color = DepthTokens.InsetFieldGradientStart,
    val insetBackground: Color = DepthTokens.InsetFieldBackground,
    val insetFocusedBackground: Color = DepthTokens.InsetFieldFocusedBackground,
    val insetErrorBackground: Color = DepthTokens.InsetFieldErrorBackground,
    val insetDisabledBackground: Color = DepthTokens.InsetFieldDisabledBackground,
    val insetShadowColor: Color = DepthTokens.InsetFieldShadowColor,
    val insetTopShadowAlpha: Float = DepthTokens.INSET_FIELD_TOP_SHADOW_ALPHA,
    val insetSideShadowAlpha: Float = DepthTokens.INSET_FIELD_SIDE_SHADOW_ALPHA,
    val insetBorderColor: Color = DepthTokens.InsetFieldBorderColor,
    val insetFocusedBorderColor: Color = DepthTokens.InsetFieldFocusedBorderColor,
    val insetErrorBorderColor: Color = DepthTokens.InsetFieldErrorBorderColor,
    val insetDisabledBorderColor: Color = DepthTokens.InsetFieldDisabledBorderColor,
    val insetBorderAlpha: Float = DepthTokens.INSET_FIELD_BORDER_ALPHA,
    val insetBottomHighlightColor: Color = DepthTokens.InsetFieldHighlightColor,
    val insetBottomHighlightAlpha: Float = DepthTokens.INSET_FIELD_BOTTOM_HIGHLIGHT_ALPHA,
    val insetBorderWidth: Dp = Dimens.InsetBorderWidth,
    val insetFocusedBorderWidth: Dp = Dimens.InsetFocusedBorderWidth,
    val insetShadowOffsetY: Dp = Dimens.InsetShadowOffsetY,
    val insetShadowBlur: Dp = Dimens.InsetShadowBlur,
    val insetShadowHeight: Dp = Dimens.InsetShadowHeight,
    val insetCornerRadius: Dp = Dimens.RadiusMd,
    val reportFieldBorderColor: Color = DepthTokens.ReportFieldBorderColor,
    val reportFieldTextColor: Color = DepthTokens.ReportFieldTextColor,
    val reportFieldLabelColor: Color = DepthTokens.ReportFieldLabelColor,
    val reportFieldPlaceholderColor: Color = DepthTokens.ReportFieldPlaceholderColor,
    val reportFieldInsetAlpha: Float = DepthTokens.INSET_REPORT_TOP_SHADOW_ALPHA,
    val pinCellFilledStart: Color = DepthTokens.PinCellFilledGradientStart,
    val pinCellFilledEnd: Color = DepthTokens.PinCellFilledGradientEnd,
    val keypadGradientStart: Color = DepthTokens.KeypadButtonGradientStart,
    val keypadGradientEnd: Color = DepthTokens.KeypadButtonGradientEnd,
    val keypadLipColor: Color = DepthTokens.KeypadButtonLipColor,
    val keypadShadowColor: Color = DepthTokens.KeypadButtonShadowColor,
    val keypadShadowAlpha: Float = DepthTokens.KEYPAD_BUTTON_SHADOW_ALPHA,
    val keypadHeight: Dp = Dimens.KeypadButtonHeight,
    val keypadCornerRadius: Dp = Dimens.RadiusKeypad,
    val keypadShadowOffsetY: Dp = Dimens.KeypadShadowOffsetY,
    val keypadShadowBlur: Dp = Dimens.KeypadShadowBlur,
    val logoTileGradientStart: Color = DepthTokens.LogoTileGradientStart,
    val logoTileGradientEnd: Color = DepthTokens.LogoTileGradientEnd,
    val logoTileShadowColor: Color = DepthTokens.LogoTileShadowColor,
    val logoTileShadowAlpha: Float = DepthTokens.LOGO_TILE_SHADOW_ALPHA,
    val logoTileBottomEdgeAlpha: Float = DepthTokens.LOGO_TILE_BOTTOM_EDGE_ALPHA,
    val tabBarGradientStart: Color = DepthTokens.TabBarGradientStart,
    val tabBarGradientEnd: Color = DepthTokens.TabBarGradientEnd,
    val tabBarShadowColor: Color = DepthTokens.TabBarShadowColor,
    val tabBarShadowAlpha: Float = DepthTokens.TAB_BAR_SHADOW_ALPHA,
    val tabIndicatorGradientStart: Color = DepthTokens.TabIndicatorGradientStart,
    val tabIndicatorGradientEnd: Color = DepthTokens.TabIndicatorGradientEnd,
    val tabIndicatorGlowColor: Color = DepthTokens.TabIndicatorGlowColor,
    val tabIndicatorGlowAlpha: Float = DepthTokens.TAB_INDICATOR_GLOW_ALPHA,
    val cardRiseDurationMs: Int = DepthMotionTokens.CARD_RISE_DURATION_MS,
    val cardRiseStaggerStepMs: Int = DepthMotionTokens.CARD_RISE_STAGGER_STEP_MS,
    val cardRiseMaxDelayMs: Int = DepthMotionTokens.CARD_RISE_MAX_DELAY_MS,
    val cardRiseTranslationY: Dp = Dimens.CardRiseTranslationY,
    val pressDurationMs: Int = DepthMotionTokens.PRESS_DURATION_MS,
    val livePulsePeriodMs: Int = DepthMotionTokens.LIVE_PULSE_PERIOD_MS,
    val liveDotColor: Color = DepthTokens.LiveDotColor,
    val liveDotSize: Dp = Dimens.LiveDotSize,
    val liveHaloStartAlpha: Float = DepthTokens.LIVE_HALO_START_ALPHA,
    val liveHaloEndAlpha: Float = DepthTokens.LIVE_HALO_END_ALPHA,
    val liveHaloSteadyAlpha: Float = DepthTokens.LIVE_HALO_STEADY_ALPHA,
    val liveHaloMaxScale: Float = DepthTokens.LIVE_HALO_MAX_SCALE
)

val LocalDepthStyle = staticCompositionLocalOf { DepthStyle() }

