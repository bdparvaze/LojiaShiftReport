package com.lojia.shiftreport.ui.theme

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

    // ============ BRAND GRADIENTS (HEADER & PRIMARY BUTTONS) ============
    /** Top gradient stop for full-width headers, reusing existing PrimaryBlueLight. */
    val BrandHeaderGradientStart: Color = PrimaryBlueLight

    /** Bottom gradient stop for full-width headers, reusing existing PrimaryBlueDark. */
    val BrandHeaderGradientEnd: Color = PrimaryBlueDark

    /** Top gradient stop for raised primary action buttons (#3B6BF0). */
    val BrandButtonGradientStart: Color = Color(0xFF3B6BF0)

    /** Bottom gradient stop for raised primary action buttons (#1D4FD8). */
    val BrandButtonGradientEnd: Color = PrimaryBlueLight

    // ============ SURFACE & CARD GRADIENTS ============
    /** Top gradient stop for standard depth cards (crisp white surface #FFFFFF). */
    val CardGradientStart: Color = SurfaceLight

    /** Bottom gradient stop for standard depth cards (#F9FAFF). */
    val CardGradientEnd: Color = Color(0xFFF9FAFF)

    /** Top gradient stop for highlighted/accent depth cards (soft ice blue). */
    val CardAccentGradientStart: Color = Color(0xFFEFF6FF)

    /** Bottom gradient stop for highlighted/accent depth cards, reusing PrimaryContainerLight. */
    val CardAccentGradientEnd: Color = PrimaryContainerLight

    // ============ RAISED CARD SHADOW & BORDER TOKENS ============
    /** Close ambient shadow tint rgba(16, 24, 40, ...) beneath raised cards. */
    val CardShadowCloseColor: Color = ShadowSlateColor

    /** Far directional brand shadow tint rgba(29, 79, 216, ...) beneath raised cards. */
    val RaisedCardShadowColor: Color = PrimaryBlueLight

    /** 1dp crisp perimeter border color for raised cards (#E5E9F5). */
    val RaisedCardBorderColor: Color = DepthCardBorder

    /** Border color for highlighted accent depth cards. */
    val RaisedCardAccentBorderColor: Color = PrimaryLight

    /** Close ambient shadow alpha rgba(16, 24, 40, 0.06). */
    const val RAISED_CARD_AMBIENT_ALPHA: Float = 0.06f

    /** Far directional shadow alpha rgba(29, 79, 216, 0.12). */
    const val RAISED_CARD_SPOT_ALPHA: Float = 0.12f

    /** Hairline border alpha for standard raised cards (solid 1.0f). */
    const val RAISED_CARD_BORDER_ALPHA: Float = 1.0f

    /** Accent card border alpha for highlighted KPI and summary cards. */
    const val RAISED_CARD_ACCENT_BORDER_ALPHA: Float = 0.35f

    /** Top 1dp white inner highlight alpha along the upper edge of raised cards. */
    const val RAISED_CARD_TOP_SHEEN_ALPHA: Float = 1.0f

    // ============ RAISED BUTTON SHADOW & LIP TOKENS ============
    /** Lower 3D "lip" color anchored beneath enabled primary depth buttons (#153AA8). */
    val RaisedButtonLipColor: Color = Color(0xFF153AA8)

    /** Top gradient stop for disabled depth buttons. */
    val RaisedButtonDisabledStart: Color = SurfaceVariantLight

    /** Bottom gradient stop for disabled depth buttons. */
    val RaisedButtonDisabledEnd: Color = OutlineVariantLight

    /** Lower lip color anchored beneath disabled depth buttons. */
    val RaisedButtonLipDisabledColor: Color = OutlineLight

    /** Elevation shadow tint beneath enabled raised buttons (#1D4FD8). */
    val RaisedButtonShadowColor: Color = PrimaryBlueLight

    /** Opacity applied to the lower lip on enabled raised buttons. */
    const val RAISED_BUTTON_LIP_ALPHA: Float = 1.0f

    /** Opacity applied to the lower lip on disabled raised buttons. */
    const val RAISED_BUTTON_LIP_DISABLED_ALPHA: Float = 0.70f

    /** Drop-shadow opacity rgba(29, 79, 216, 0.30) beneath enabled raised buttons. */
    const val RAISED_BUTTON_SHADOW_ALPHA: Float = 0.30f

    /** Inner 1dp top white highlight alpha on enabled primary buttons. */
    const val RAISED_BUTTON_TOP_HIGHLIGHT_ALPHA: Float = 0.30f

    // ============ SECONDARY & DESTRUCTIVE BUTTON TOKENS ============
    /** Top gradient stop for raised secondary/outlined buttons (#FFFFFF). */
    val SecondaryButtonGradientStart: Color = PureWhite

    /** Bottom gradient stop for raised secondary/outlined buttons (#F1F5FF). */
    val SecondaryButtonGradientEnd: Color = Color(0xFFF1F5FF)

    /** Lower lip color beneath enabled secondary buttons (#C5D5FB). */
    val SecondaryButtonLipColor: Color = Color(0xFFC5D5FB)

    /** Perimeter border color for secondary raised buttons (#1D4FD8). */
    val SecondaryButtonBorderColor: Color = PrimaryBlueLight

    /** Soft shadow tint beneath enabled secondary buttons (#1D4FD8). */
    val SecondaryButtonShadowColor: Color = PrimaryBlueLight

    /** Top gradient stop for disabled secondary buttons. */
    val SecondaryButtonDisabledStart: Color = SurfaceVariantLight

    /** Bottom gradient stop for disabled secondary buttons. */
    val SecondaryButtonDisabledEnd: Color = BackgroundLight

    /** Shadow opacity beneath enabled secondary buttons. */
    const val SECONDARY_BUTTON_SHADOW_ALPHA: Float = 0.12f

    /** Lip opacity beneath enabled secondary buttons. */
    const val SECONDARY_BUTTON_LIP_ALPHA: Float = 1.0f

    /** Lip opacity beneath disabled secondary buttons. */
    const val SECONDARY_BUTTON_LIP_DISABLED_ALPHA: Float = 0.50f

    /** Top gradient stop for destructive raised buttons. */
    val DestructiveButtonGradientStart: Color = PosExpenseRedFill

    /** Bottom gradient stop for destructive raised buttons. */
    val DestructiveButtonGradientEnd: Color = ErrorRedLight

    /** Lower lip color anchored beneath destructive raised buttons. */
    val DestructiveButtonLipColor: Color = OnErrorContainerLight

    /** Elevation shadow tint beneath destructive raised buttons. */
    val DestructiveButtonShadowColor: Color = ErrorRedLight

    // ============ INSET FIELD TOKENS (PRESSED-IN SURFACES) ============
    /** Top gradient stop for standard inset text fields (#FFFFFF). */
    val InsetFieldGradientStart: Color = PureWhite

    /** Bottom gradient stop for standard inset text fields and empty PIN cells (#F7F9FD). */
    val InsetFieldBackground: Color = Color(0xFFF7F9FD)

    /** Recessed bottom gradient stop when an inset text field holds focus (#F4F7FD). */
    val InsetFieldFocusedBackground: Color = PageBackground

    /** Recessed fill background when an inset text field is in error state. */
    val InsetFieldErrorBackground: Color = NegativeBannerSurface

    /** Recessed fill background when an inset text field is disabled. */
    val InsetFieldDisabledBackground: Color = SurfaceVariantLight

    /** Inner top shadow tint rgba(16, 24, 40, ...) cast inside inset input wells. */
    val InsetFieldShadowColor: Color = ShadowSlateColor

    /** Perimeter stroke color for unfocused inset input wells (#CBD5E1). */
    val InsetFieldBorderColor: Color = OutlineLight

    /** Perimeter stroke color when an inset input well holds focus (#1D4FD8). */
    val InsetFieldFocusedBorderColor: Color = PrimaryBlueLight

    /** Perimeter stroke color when an inset input well is in error state. */
    val InsetFieldErrorBorderColor: Color = ErrorRedLight

    /** Perimeter stroke color when an inset input well is disabled. */
    val InsetFieldDisabledBorderColor: Color = OutlineVariantLight

    /** Crisp 1dp lower-edge white bevel highlight color on inset wells (#FFFFFF). */
    val InsetFieldHighlightColor: Color = PureWhite

    /** Standard inset icon color (#475467). */
    val InsetIconColor: Color = OnSurfaceVariantLight

    /** Standard inset placeholder color (#64748B). */
    val InsetPlaceholderColor: Color = TextHintColor

    /** Register compact field icon color (#94A3B8). */
    val RegisterIconColor: Color = TextDisabledColor

    /** Register compact field placeholder color (#94A3B8). */
    val RegisterPlaceholderColor: Color = TextDisabledColor

    /** Inner top-edge shadow alpha rgba(16, 24, 40, 0.07) creating pressed-in depth. */
    const val INSET_FIELD_TOP_SHADOW_ALPHA: Float = 0.07f

    /** Subtle side/edge shadow alpha inside inset input fields. */
    const val INSET_FIELD_SIDE_SHADOW_ALPHA: Float = 0.04f

    /** Border opacity for inset input wells. */
    const val INSET_FIELD_BORDER_ALPHA: Float = 1.0f

    /** Bottom 1dp white bevel highlight opacity for inset input wells. */
    const val INSET_FIELD_BOTTOM_HIGHLIGHT_ALPHA: Float = 1.0f

    // ============ LOGO TILE, PIN CELLS & KEYPAD TOKENS ============
    /** Top-left stop for the 145-degree diagonal app logo tile gradient (#FFFFFF). */
    val LogoTileGradientStart: Color = PureWhite

    /** Bottom-right stop for the 145-degree diagonal app logo tile gradient (#DCE6FF). */
    val LogoTileGradientEnd: Color = Color(0xFFDCE6FF)

    /** Drop shadow color beneath the 48dp app logo tile (#1D4FD8 @ 0.25f). */
    val LogoTileShadowColor: Color = PrimaryBlueLight

    /** Drop shadow opacity beneath the 48dp app logo tile. */
    const val LOGO_TILE_SHADOW_ALPHA: Float = 0.25f

    /** Inner bottom bevel edge opacity on the app logo tile rgba(16, 24, 40, 0.07). */
    const val LOGO_TILE_BOTTOM_EDGE_ALPHA: Float = 0.07f

    /** Top gradient stop for filled Quick PIN cells (#FFFFFF). */
    val PinCellFilledGradientStart: Color = PureWhite

    /** Bottom gradient stop for filled Quick PIN cells (#F1F5FF). */
    val PinCellFilledGradientEnd: Color = Color(0xFFF1F5FF)

    /** Aspect ratio multiplier defining Quick PIN cell height = width * 1.15f. */
    const val PIN_CELL_HEIGHT_MULTIPLIER: Float = 1.15f

    /** Width-to-height ratio (width / height = 1 / 1.15) for Compose Modifier.aspectRatio. */
    const val PIN_CELL_ASPECT_RATIO: Float = 1f / PIN_CELL_HEIGHT_MULTIPLIER

    /** Top gradient stop for Quick PIN square keypad keys (#FFFFFF). */
    val KeypadButtonGradientStart: Color = PureWhite

    /** Bottom gradient stop for Quick PIN square keypad keys (#F1F4FB). */
    val KeypadButtonGradientEnd: Color = Color(0xFFF1F4FB)

    /** Lower 4dp lip color beneath Quick PIN keypad keys (#D5DCEB). */
    val KeypadButtonLipColor: Color = Color(0xFFD5DCEB)

    /** Drop shadow tint beneath Quick PIN keypad keys (#101828 @ 0.10f). */
    val KeypadButtonShadowColor: Color = ShadowSlateColor

    /** Drop shadow opacity rgba(16, 24, 40, 0.10) beneath Quick PIN keypad keys. */
    const val KEYPAD_BUTTON_SHADOW_ALPHA: Float = 0.10f

    // ============ TAB BAR TOKENS ============
    /** Top gradient stop for raised tab bar containers. */
    val TabBarGradientStart: Color = PureWhite

    /** Bottom gradient stop for raised tab bar containers. */
    val TabBarGradientEnd: Color = Color(0xFFF8FAFF)

    /** Start stop for active tab indicator pill gradient. */
    val TabIndicatorGradientStart: Color = PrimaryLight

    /** End stop for active tab indicator pill gradient. */
    val TabIndicatorGradientEnd: Color = PrimaryBlueLight

    // ============ HEADER & LIVE PULSE TOKENS ============
    /** Specular sheen highlight alpha along the top of brand gradient headers. */
    const val HEADER_SHEEN_ALPHA: Float = 0.14f

    /** Active dot color for LIVE status indicators. */
    val LiveDotColor: Color = SuccessGreen

    /** Starting halo alpha when a LIVE pulse expands outward. */
    const val LIVE_HALO_START_ALPHA: Float = 0.45f

    /** Ending halo alpha as a LIVE pulse dissipates. */
    const val LIVE_HALO_END_ALPHA: Float = 0.0f

    /** Steady halo alpha shown when reduced motion disables animation. */
    const val LIVE_HALO_STEADY_ALPHA: Float = 0.28f

    /** Peak diameter multiplier for the expanding LIVE pulse ring. */
    const val LIVE_HALO_MAX_SCALE: Float = 1.85f
}

/**
 * Motion duration, stagger, press scale, and cap constants for Lojia's depth transitions.
 */
object DepthMotionTokens {
    /** Duration (ms) for a single one-shot card-rise entrance animation. */
    const val CARD_RISE_DURATION_MS: Int = 260

    /** Incremental stagger delay (ms) per list card index. */
    const val CARD_RISE_STAGGER_STEP_MS: Int = 40

    /** Maximum total stagger delay (ms) so long lists never stall before appearing. */
    const val CARD_RISE_MAX_DELAY_MS: Int = 280

    /** Tactile press compression duration (ms) for raised buttons. */
    const val PRESS_DURATION_MS: Int = 90

    /** Tactile scale factor when a primary raised button is pressed (0.98f). */
    const val BUTTON_PRESSED_SCALE: Float = 0.98f

    /** Full cycle period (ms) for a LIVE status indicator pulse. */
    const val LIVE_PULSE_PERIOD_MS: Int = 1400
}

/**
 * Exact type scale constants matching the approved Stage 26D design boards.
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
 * Consolidated soft-depth theme style provided via [LocalDepthStyle] inside [LojiaTheme].
 */
@Immutable
data class DepthStyle(
    val pageBackground: Color = DepthTokens.PageBackground,
    val headerGradientStart: Color = DepthTokens.BrandHeaderGradientStart,
    val headerGradientEnd: Color = DepthTokens.BrandHeaderGradientEnd,
    val headerSheenAlpha: Float = DepthTokens.HEADER_SHEEN_ALPHA,
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
    val tabIndicatorGradientStart: Color = DepthTokens.TabIndicatorGradientStart,
    val tabIndicatorGradientEnd: Color = DepthTokens.TabIndicatorGradientEnd,
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

