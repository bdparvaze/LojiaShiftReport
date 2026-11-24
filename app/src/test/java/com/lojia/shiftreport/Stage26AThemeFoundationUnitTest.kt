package com.lojia.shiftreport

import androidx.compose.ui.graphics.Color
import com.lojia.shiftreport.ui.theme.BackgroundLight
import com.lojia.shiftreport.ui.theme.DepthMotionTokens
import com.lojia.shiftreport.ui.theme.DepthStyle
import com.lojia.shiftreport.ui.theme.DepthTokens
import com.lojia.shiftreport.ui.theme.ErrorContainerLight
import com.lojia.shiftreport.ui.theme.ErrorRedLight
import com.lojia.shiftreport.ui.theme.LojiaShapes
import com.lojia.shiftreport.ui.theme.OnErrorContainerLight
import com.lojia.shiftreport.ui.theme.OnPrimaryContainerLight
import com.lojia.shiftreport.ui.theme.OnPrimaryLight
import com.lojia.shiftreport.ui.theme.OnSurfaceLight
import com.lojia.shiftreport.ui.theme.OnSurfaceVariantLight
import com.lojia.shiftreport.ui.theme.PosCashGreen
import com.lojia.shiftreport.ui.theme.PrimaryIndigoLight
import com.lojia.shiftreport.ui.theme.ShiftColors
import com.lojia.shiftreport.ui.theme.cardRiseDelayMillis
import com.lojia.shiftreport.ui.theme.shouldAnimate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class Stage26AThemeFoundationUnitTest {

    private fun linearizeChannel(channel: Float): Double {
        val c = channel.toDouble().coerceIn(0.0, 1.0)
        return if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }

    private fun relativeLuminance(color: Color): Double {
        val r = linearizeChannel(color.red)
        val g = linearizeChannel(color.green)
        val b = linearizeChannel(color.blue)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun wcagContrastRatio(foreground: Color, background: Color): Double {
        val l1 = relativeLuminance(foreground)
        val l2 = relativeLuminance(background)
        val lighter = max(l1, l2)
        val darker = min(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    @Test
    fun wcagContrast_textOnCardAndBrandGradientsAndErrorContainerMeetsAA() {
        val aaNormalText = 4.5

        // Text on card gradient start & end
        assertTrue(
            "OnSurfaceLight on CardGradientStart must meet 4.5:1",
            wcagContrastRatio(OnSurfaceLight, DepthTokens.CardGradientStart) >= aaNormalText
        )
        assertTrue(
            "OnSurfaceLight on CardGradientEnd must meet 4.5:1",
            wcagContrastRatio(OnSurfaceLight, DepthTokens.CardGradientEnd) >= aaNormalText
        )
        assertTrue(
            "OnSurfaceVariantLight on CardGradientStart must meet 4.5:1",
            wcagContrastRatio(OnSurfaceVariantLight, DepthTokens.CardGradientStart) >= aaNormalText
        )
        assertTrue(
            "OnSurfaceVariantLight on CardGradientEnd must meet 4.5:1",
            wcagContrastRatio(OnSurfaceVariantLight, DepthTokens.CardGradientEnd) >= aaNormalText
        )

        // Text on card-accent gradient start & end
        assertTrue(
            "OnPrimaryContainerLight on CardAccentGradientStart must meet 4.5:1",
            wcagContrastRatio(OnPrimaryContainerLight, DepthTokens.CardAccentGradientStart) >= aaNormalText
        )
        assertTrue(
            "OnPrimaryContainerLight on CardAccentGradientEnd must meet 4.5:1",
            wcagContrastRatio(OnPrimaryContainerLight, DepthTokens.CardAccentGradientEnd) >= aaNormalText
        )

        // White text on header brand gradient stops
        assertTrue(
            "White text on BrandHeaderGradientStart must meet 4.5:1",
            wcagContrastRatio(OnPrimaryLight, DepthTokens.BrandHeaderGradientStart) >= aaNormalText
        )
        assertTrue(
            "White text on BrandHeaderGradientEnd must meet 4.5:1",
            wcagContrastRatio(OnPrimaryLight, DepthTokens.BrandHeaderGradientEnd) >= aaNormalText
        )

        // White text on primary button brand gradient stops
        assertTrue(
            "White text on BrandButtonGradientStart must meet 4.5:1",
            wcagContrastRatio(OnPrimaryLight, DepthTokens.BrandButtonGradientStart) >= aaNormalText
        )
        assertTrue(
            "White text on BrandButtonGradientEnd must meet 4.5:1",
            wcagContrastRatio(OnPrimaryLight, DepthTokens.BrandButtonGradientEnd) >= aaNormalText
        )

        // Error text on error container
        assertTrue(
            "ErrorRedLight on ErrorContainerLight must meet 4.5:1",
            wcagContrastRatio(ErrorRedLight, ErrorContainerLight) >= aaNormalText
        )
        assertTrue(
            "OnErrorContainerLight on ErrorContainerLight must meet 4.5:1",
            wcagContrastRatio(OnErrorContainerLight, ErrorContainerLight) >= aaNormalText
        )
    }

    @Test
    fun gradientsAreOrderedAndDistinct_andAlphaConstantsInUnitRange() {
        // Stage 29A: flat solid fills unify paired start/end surface tokens.
        assertEquals(DepthTokens.BrandHeaderGradientStart, DepthTokens.BrandHeaderGradientEnd)
        assertEquals(DepthTokens.BrandButtonGradientStart, DepthTokens.BrandButtonGradientEnd)
        assertEquals(DepthTokens.CardGradientStart, DepthTokens.CardGradientEnd)
        assertEquals(DepthTokens.CardAccentGradientStart, DepthTokens.CardAccentGradientEnd)

        assertEquals(
            relativeLuminance(DepthTokens.BrandHeaderGradientStart),
            relativeLuminance(DepthTokens.BrandHeaderGradientEnd),
            1e-6
        )
        assertEquals(
            relativeLuminance(DepthTokens.BrandButtonGradientStart),
            relativeLuminance(DepthTokens.BrandButtonGradientEnd),
            1e-6
        )
        assertEquals(
            relativeLuminance(DepthTokens.CardGradientStart),
            relativeLuminance(DepthTokens.CardGradientEnd),
            1e-6
        )
        assertEquals(
            relativeLuminance(DepthTokens.CardAccentGradientStart),
            relativeLuminance(DepthTokens.CardAccentGradientEnd),
            1e-6
        )

        val alphas = listOf(
            DepthTokens.RAISED_CARD_AMBIENT_ALPHA,
            DepthTokens.RAISED_CARD_SPOT_ALPHA,
            DepthTokens.RAISED_CARD_BORDER_ALPHA,
            DepthTokens.RAISED_CARD_ACCENT_BORDER_ALPHA,
            DepthTokens.RAISED_CARD_TOP_SHEEN_ALPHA,
            DepthTokens.RAISED_BUTTON_LIP_ALPHA,
            DepthTokens.RAISED_BUTTON_LIP_DISABLED_ALPHA,
            DepthTokens.RAISED_BUTTON_SHADOW_ALPHA,
            DepthTokens.INSET_FIELD_TOP_SHADOW_ALPHA,
            DepthTokens.INSET_FIELD_SIDE_SHADOW_ALPHA,
            DepthTokens.INSET_FIELD_BORDER_ALPHA,
            DepthTokens.INSET_FIELD_BOTTOM_HIGHLIGHT_ALPHA,
            DepthTokens.HEADER_SHEEN_ALPHA,
            DepthTokens.LIVE_HALO_START_ALPHA,
            DepthTokens.LIVE_HALO_END_ALPHA,
            DepthTokens.LIVE_HALO_STEADY_ALPHA
        )
        alphas.forEach { alpha ->
            assertTrue("Alpha $alpha must be in 0..1", alpha in 0f..1f)
        }
    }

    @Test
    fun motionTokens_reducedMotionAndStaggerDelayCap() {
        assertFalse(shouldAnimate(0f))
        assertFalse(shouldAnimate(-1f))
        assertFalse(shouldAnimate(Float.NaN))
        assertTrue(shouldAnimate(0.5f))
        assertTrue(shouldAnimate(1f))
        assertTrue(shouldAnimate(1.5f))

        val step = DepthMotionTokens.CARD_RISE_STAGGER_STEP_MS
        val maxDelay = DepthMotionTokens.CARD_RISE_MAX_DELAY_MS
        assertTrue(step > 0)
        assertTrue(maxDelay >= step)

        assertEquals(0, cardRiseDelayMillis(-3))
        assertEquals(0, cardRiseDelayMillis(0))
        assertEquals(step, cardRiseDelayMillis(1))
        assertEquals(2 * step, cardRiseDelayMillis(2))
        assertEquals(maxDelay, cardRiseDelayMillis(50))
        assertEquals(maxDelay, cardRiseDelayMillis(10_000))
    }

    @Test
    fun existingThemeTokensAndDepthDefaultsResolve() {
        assertNotEquals(Color.Unspecified, PrimaryIndigoLight)
        assertEquals(PrimaryIndigoLight, ShiftColors.Primary)
        assertNotEquals(Color.Unspecified, BackgroundLight)
        assertNotEquals(Color.Unspecified, PosCashGreen)

        val defaultStyle = DepthStyle()
        assertNotNull(defaultStyle)
        assertNotNull(LojiaShapes.medium)
    }
}
