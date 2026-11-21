package com.lojia.shiftreport.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

/**
 * Central color, gradient, shadow, and alpha tokens for Lojia's soft-depth visual system.
 * All depth values live exclusively in `ui/theme/` so screens inherit updates from one source.
 */
object DepthTokens {

    // ============ BRAND GRADIENTS (HEADER & PRIMARY BUTTONS) ============
    /** Top gradient stop for full-width headers, reusing existing PrimaryBlueLight. */
    val BrandHeaderGradientStart: Color = PrimaryBlueLight

    /** Bottom gradient stop for full-width headers, reusing existing PrimaryBlueDark. */
    val BrandHeaderGradientEnd: Color = PrimaryBlueDark

    /** Top gradient stop for raised primary action buttons (high-contrast royal blue). */
    val BrandButtonGradientStart: Color = Color(0xFF2563EB)

    /** Bottom gradient stop for raised primary action buttons (deep anchor blue). */
    val BrandButtonGradientEnd: Color = Color(0xFF1E40AF)

    // ============ SURFACE & CARD GRADIENTS ============
    /** Top gradient stop for standard depth cards (crisp white surface). */
    val CardGradientStart: Color = SurfaceLight

    /** Bottom gradient stop for standard depth cards (very light cool blue tint). */
    val CardGradientEnd: Color = Color(0xFFF3F7FF)

    /** Top gradient stop for highlighted/accent depth cards (soft ice blue). */
    val CardAccentGradientStart: Color = Color(0xFFEFF6FF)

    /** Bottom gradient stop for highlighted/accent depth cards, reusing PrimaryContainerLight. */
    val CardAccentGradientEnd: Color = PrimaryContainerLight

    // ============ RAISED CARD SHADOW & BORDER TOKENS ============
    /** Base color used for raised-card ambient and spot shadows. */
    val RaisedCardShadowColor: Color = PrimaryBlueDark

    /** Border color for standard raised depth cards. */
    val RaisedCardBorderColor: Color = OutlineVariantLight

    /** Border color for highlighted accent depth cards. */
    val RaisedCardAccentBorderColor: Color = PrimaryLight

    /** Ambient shadow alpha for raised cards to give soft vertical lift. */
    const val RAISED_CARD_AMBIENT_ALPHA: Float = 0.08f

    /** Directional spot shadow alpha for raised cards. */
    const val RAISED_CARD_SPOT_ALPHA: Float = 0.14f

    /** Hairline border alpha for standard raised cards. */
    const val RAISED_CARD_BORDER_ALPHA: Float = 0.90f

    /** Accent card border alpha for highlighted KPI and summary cards. */
    const val RAISED_CARD_ACCENT_BORDER_ALPHA: Float = 0.35f

    /** Subtle top inner-highlight alpha along the upper edge of raised cards. */
    const val RAISED_CARD_TOP_SHEEN_ALPHA: Float = 0.85f

    // ============ RAISED BUTTON SHADOW & LIP TOKENS ============
    /** Lower 3D "lip" color anchored beneath enabled primary depth buttons. */
    val RaisedButtonLipColor: Color = Color(0xFF162D70)

    /** Top gradient stop for disabled depth buttons. */
    val RaisedButtonDisabledStart: Color = SurfaceVariantLight

    /** Bottom gradient stop for disabled depth buttons. */
    val RaisedButtonDisabledEnd: Color = OutlineVariantLight

    /** Lower lip color anchored beneath disabled depth buttons. */
    val RaisedButtonLipDisabledColor: Color = OutlineLight

    /** Elevation shadow tint beneath enabled raised buttons. */
    val RaisedButtonShadowColor: Color = PrimaryBlueDark

    /** Opacity applied to the lower lip on enabled raised buttons. */
    const val RAISED_BUTTON_LIP_ALPHA: Float = 1.0f

    /** Opacity applied to the lower lip on disabled raised buttons. */
    const val RAISED_BUTTON_LIP_DISABLED_ALPHA: Float = 0.70f

    /** Drop-shadow opacity beneath enabled raised buttons. */
    const val RAISED_BUTTON_SHADOW_ALPHA: Float = 0.24f

    // ============ INSET FIELD TOKENS (PRESSED-IN SURFACES) ============
    /** Recessed fill background for inset text fields and input wells. */
    val InsetFieldBackground: Color = Color(0xFFF3F6FB)

    /** Inner shadow color cast along the upper edge of inset input wells. */
    val InsetFieldShadowColor: Color = OnBackgroundLight

    /** Perimeter stroke color for inset input wells. */
    val InsetFieldBorderColor: Color = OutlineLight

    /** Crisp lower-edge highlight color reinforcing the recessed bevel. */
    val InsetFieldHighlightColor: Color = PureWhite

    /** Inner top-edge shadow alpha creating pressed-in depth inside input fields. */
    const val INSET_FIELD_TOP_SHADOW_ALPHA: Float = 0.09f

    /** Subtle side shadow alpha inside inset input fields. */
    const val INSET_FIELD_SIDE_SHADOW_ALPHA: Float = 0.04f

    /** Border opacity for inset input wells. */
    const val INSET_FIELD_BORDER_ALPHA: Float = 0.85f

    /** Bottom edge bevel highlight opacity for inset input wells. */
    const val INSET_FIELD_BOTTOM_HIGHLIGHT_ALPHA: Float = 0.80f

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
 * Motion duration, stagger, and cap constants for Lojia's depth transitions.
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

    /** Full cycle period (ms) for a LIVE status indicator pulse. */
    const val LIVE_PULSE_PERIOD_MS: Int = 1400
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
    val buttonElevation: Dp = Dimens.RaisedButtonElevation,
    val buttonLipHeight: Dp = Dimens.ButtonLipHeight,
    val buttonCornerRadius: Dp = Dimens.RadiusMd,
    val cardGradientStart: Color = DepthTokens.CardGradientStart,
    val cardGradientEnd: Color = DepthTokens.CardGradientEnd,
    val cardAccentGradientStart: Color = DepthTokens.CardAccentGradientStart,
    val cardAccentGradientEnd: Color = DepthTokens.CardAccentGradientEnd,
    val cardShadowColor: Color = DepthTokens.RaisedCardShadowColor,
    val cardAmbientShadowAlpha: Float = DepthTokens.RAISED_CARD_AMBIENT_ALPHA,
    val cardSpotShadowAlpha: Float = DepthTokens.RAISED_CARD_SPOT_ALPHA,
    val cardBorderColor: Color = DepthTokens.RaisedCardBorderColor,
    val cardBorderAlpha: Float = DepthTokens.RAISED_CARD_BORDER_ALPHA,
    val cardAccentBorderColor: Color = DepthTokens.RaisedCardAccentBorderColor,
    val cardAccentBorderAlpha: Float = DepthTokens.RAISED_CARD_ACCENT_BORDER_ALPHA,
    val cardTopSheenAlpha: Float = DepthTokens.RAISED_CARD_TOP_SHEEN_ALPHA,
    val cardBorderWidth: Dp = Dimens.CardBorderWidth,
    val cardElevation: Dp = Dimens.CardElevation,
    val cardAccentElevation: Dp = Dimens.CardAccentElevation,
    val cardCornerRadius: Dp = Dimens.RadiusLg,
    val insetBackground: Color = DepthTokens.InsetFieldBackground,
    val insetShadowColor: Color = DepthTokens.InsetFieldShadowColor,
    val insetTopShadowAlpha: Float = DepthTokens.INSET_FIELD_TOP_SHADOW_ALPHA,
    val insetSideShadowAlpha: Float = DepthTokens.INSET_FIELD_SIDE_SHADOW_ALPHA,
    val insetBorderColor: Color = DepthTokens.InsetFieldBorderColor,
    val insetBorderAlpha: Float = DepthTokens.INSET_FIELD_BORDER_ALPHA,
    val insetBottomHighlightColor: Color = DepthTokens.InsetFieldHighlightColor,
    val insetBottomHighlightAlpha: Float = DepthTokens.INSET_FIELD_BOTTOM_HIGHLIGHT_ALPHA,
    val insetBorderWidth: Dp = Dimens.InsetBorderWidth,
    val insetShadowHeight: Dp = Dimens.InsetShadowHeight,
    val insetCornerRadius: Dp = Dimens.RadiusMd,
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
