package com.lojia.shiftreport

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojia.shiftreport.ui.theme.BackgroundLight
import com.lojia.shiftreport.ui.theme.DepthCardBorder
import com.lojia.shiftreport.ui.theme.DepthStyle
import com.lojia.shiftreport.ui.theme.DepthTokens
import com.lojia.shiftreport.ui.theme.DepthTypeTokens
import com.lojia.shiftreport.ui.theme.Dimens
import com.lojia.shiftreport.ui.theme.OnBackgroundLight
import com.lojia.shiftreport.ui.theme.OnSurfaceVariantLight
import com.lojia.shiftreport.ui.theme.OutlineLight
import com.lojia.shiftreport.ui.theme.PrimaryBlueLight
import com.lojia.shiftreport.ui.theme.PureWhite
import com.lojia.shiftreport.ui.theme.RequiredStarRed
import com.lojia.shiftreport.ui.theme.ShadowSlateColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class Stage26DParityUnitTest {

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

    private fun resolveAppModuleDir(): File {
        val candidates = listOf(File("."), File("app"), File(".."), File("../app"))
        return candidates.firstOrNull { dir ->
            File(dir, "src/main/java/com/lojia/shiftreport/auth/LojiaAuthComponents.kt").isFile
        } ?: error("Unable to locate app module directory from ${File(".").absolutePath}")
    }

    @Test
    fun stage26D_exactBoardColorAndDimensionTokensMatch() {
        // 1) Page background
        assertEquals(Color(0xFFF4F7FD), BackgroundLight)
        assertEquals(Color(0xFFF4F7FD), DepthTokens.PageBackground)

        // 2) Auth card tokens (Stage 29A flat UI)
        assertEquals(PureWhite, DepthTokens.CardGradientStart)
        assertEquals(PureWhite, DepthTokens.CardGradientEnd)
        assertEquals(10.dp, Dimens.CardCornerRadius)
        assertEquals(18.dp, Dimens.CardPaddingAuth)
        assertEquals(Color(0xFFE5E9F5), DepthCardBorder)
        assertEquals(1.dp, Dimens.CardShadowCloseOffsetY)
        assertEquals(4.dp, Dimens.CardShadowCloseBlur)
        assertEquals(0.06f, DepthTokens.RAISED_CARD_AMBIENT_ALPHA, 1e-6f)
        assertEquals(0.dp, Dimens.CardShadowFarOffsetY)
        assertEquals(0.dp, Dimens.CardShadowFarBlur)
        assertEquals(0.0f, DepthTokens.RAISED_CARD_SPOT_ALPHA, 1e-6f)
        assertEquals(0.0f, DepthTokens.RAISED_CARD_TOP_SHEEN_ALPHA, 1e-6f)

        // 3) Inset field tokens (Stage 29A flat UI)
        assertEquals(44.dp, Dimens.TextFieldHeightStandard)
        assertEquals(40.dp, Dimens.TextFieldHeightCompact)
        assertEquals(10.dp, Dimens.RadiusInputRegister)
        assertEquals(8.dp, Dimens.RadiusMd)
        assertEquals(1.dp, Dimens.InsetBorderWidth)
        assertEquals(1.dp, Dimens.InsetRegisterBorderWidth)
        assertEquals(1.5.dp, Dimens.InsetFocusedBorderWidth)
        assertEquals(Color(0xFFCBD5E1), OutlineLight)
        assertEquals(Color(0xFF1D4FD8), PrimaryBlueLight)
        assertEquals(PureWhite, DepthTokens.InsetFieldBackground)
        assertEquals(0.dp, Dimens.InsetShadowOffsetY)
        assertEquals(0.dp, Dimens.InsetShadowBlur)
        assertEquals(0.0f, DepthTokens.INSET_FIELD_TOP_SHADOW_ALPHA, 1e-6f)
        assertEquals(12.dp, Dimens.InsetFieldPaddingHorizontal)
        assertEquals(12.dp, Dimens.RegisterFieldPaddingHorizontal)
        assertEquals(12.dp, Dimens.InsetIconTextGap)
        assertEquals(18.dp, Dimens.IconField)
        assertEquals(Color(0xFF475467), DepthTokens.InsetIconColor)
        assertEquals(Color(0xFF64748B), DepthTokens.InsetPlaceholderColor)
        assertEquals(Color(0xFF64748B), DepthTokens.RegisterIconColor)
        assertEquals(Color(0xFF64748B), DepthTokens.RegisterPlaceholderColor)
        assertEquals(Color(0xFFB42318), RequiredStarRed)

        // 4) Primary & secondary flat button tokens (Stage 29A flat UI)
        assertEquals(44.dp, Dimens.ButtonHeightPrimary)
        assertEquals(Color(0xFF1D4FD8), DepthTokens.BrandButtonGradientStart)
        assertEquals(Color(0xFF1D4FD8), DepthTokens.BrandButtonGradientEnd)
        assertEquals(0.dp, Dimens.ButtonLipHeight)
        assertEquals(Color(0xFF1D4FD8), DepthTokens.RaisedButtonLipColor)
        assertEquals(1.dp, Dimens.ButtonShadowOffsetY)
        assertEquals(4.dp, Dimens.ButtonShadowBlur)
        assertEquals(0.06f, DepthTokens.RAISED_BUTTON_SHADOW_ALPHA, 1e-6f)
        assertEquals(0.0f, DepthTokens.RAISED_BUTTON_TOP_HIGHLIGHT_ALPHA, 1e-6f)
        assertEquals(0.dp, Dimens.ButtonPressedTranslationY)

        assertEquals(PureWhite, DepthTokens.SecondaryButtonGradientEnd)
        assertEquals(1.dp, Dimens.SecondaryButtonBorderWidth)
        assertEquals(PureWhite, DepthTokens.SecondaryButtonLipColor)
        assertEquals(0.dp, Dimens.SecondaryButtonLipHeight)

        // 5) Login & logo tile tokens (Stage 29A flat UI)
        assertEquals(48.dp, Dimens.LogoTileSize)
        assertEquals(15.dp, Dimens.RadiusLogoTile)
        assertEquals(24.dp, Dimens.LogoTileIconSize)
        assertEquals(PureWhite, DepthTokens.LogoTileGradientEnd)
        assertEquals(1.dp, Dimens.LogoTileShadowOffsetY)
        assertEquals(4.dp, Dimens.LogoTileShadowBlur)
        assertEquals(0.06f, DepthTokens.LOGO_TILE_SHADOW_ALPHA, 1e-6f)
        assertEquals(0.0f, DepthTokens.LOGO_TILE_BOTTOM_EDGE_ALPHA, 1e-6f)
        assertEquals(32.dp, Dimens.AuthScreenSidePadding)
        assertEquals(360.dp, Dimens.AuthCardMaxWidth)
        assertEquals(28.dp, Dimens.RegisterHeaderTopPadding)
        assertEquals(380.dp, Dimens.RegisterScreenMaxWidth)
        assertEquals(18.dp, Dimens.RememberCheckboxSize)
        assertEquals(5.dp, Dimens.RememberCheckboxRadius)
        assertEquals(1.5.dp, Dimens.RememberCheckboxBorderWidth)

        // 7) Quick PIN & keypad tokens (Stage 29A flat UI)
        assertEquals(380.dp, Dimens.QuickPinMaxWidth)
        assertEquals(24.dp, Dimens.QuickPinSidePadding)
        assertEquals(8.dp, Dimens.PinCellGap)
        assertEquals(14.dp, Dimens.KeypadColumnGap)
        assertEquals(84.dp, Dimens.KeypadButtonHeight)
        assertEquals(14.dp, Dimens.RadiusKeypad)
        assertEquals(1.15f, DepthTokens.PIN_CELL_HEIGHT_MULTIPLIER, 1e-6f)
        assertEquals(Color(0xFFEFF6FF), DepthTokens.PinCellFilledGradientEnd)
        assertEquals(PureWhite, DepthTokens.KeypadButtonGradientEnd)
        assertEquals(Color(0xFFE3E8F0), DepthTokens.KeypadButtonLipColor)
        assertEquals(Color(0xFF101828), ShadowSlateColor)
        assertEquals(1.dp, Dimens.KeypadShadowOffsetY)
        assertEquals(4.dp, Dimens.KeypadShadowBlur)
        assertEquals(0.06f, DepthTokens.KEYPAD_BUTTON_SHADOW_ALPHA, 1e-6f)

        // Typography parity tokens
        assertEquals(22.sp, DepthTypeTokens.LogoTitleSize)
        assertEquals(10.5.sp, DepthTypeTokens.LogoSubtitleSize)
        assertEquals(13.5.sp, DepthTypeTokens.InsetPlaceholderSize)
        assertEquals(13.sp, DepthTypeTokens.RegisterPlaceholderSize)
        assertEquals(12.sp, DepthTypeTokens.RegisterLabelSize)
        assertEquals(15.sp, DepthTypeTokens.PrimaryButtonTextSize)
        assertEquals(14.sp, DepthTypeTokens.SecondaryButtonTextSize)
        assertEquals(12.5.sp, DepthTypeTokens.AuthRowLinkTextSize)
        assertEquals(13.sp, DepthTypeTokens.AuthFooterTextSize)
        assertEquals(22.sp, DepthTypeTokens.QuickPinTitleSize)
        assertEquals(13.5.sp, DepthTypeTokens.QuickPinSubtitleSize)
        assertEquals(22.sp, DepthTypeTokens.QuickPinDotSize)
        assertEquals(28.sp, DepthTypeTokens.KeypadDigitSize)
        assertEquals(14.sp, DepthTypeTokens.QuickPinFallbackTextSize)

        // DepthStyle propagates defaults cleanly
        val style = DepthStyle()
        assertEquals(DepthTokens.PageBackground, style.pageBackground)
        assertEquals(Dimens.KeypadButtonHeight, style.keypadHeight)
    }

    @Test
    fun stage26D_wcagContrastPairsMeetAA() {
        val aaNormalText = 4.5
        val pairs = listOf(
            Triple("Placeholder #64748B on #F7F9FD", DepthTokens.InsetPlaceholderColor, DepthTokens.InsetFieldBackground),
            Triple("PrimaryBlueLight #1D4FD8 on #F4F7FD", PrimaryBlueLight, DepthTokens.PageBackground),
            Triple("PrimaryBlueLight #1D4FD8 on #FFFFFF", PrimaryBlueLight, PureWhite),
            Triple("PrimaryBlueLight #1D4FD8 on #F1F5FF", PrimaryBlueLight, DepthTokens.SecondaryButtonGradientEnd),
            Triple("PureWhite on #3B6BF0", PureWhite, DepthTokens.BrandButtonGradientStart),
            Triple("PureWhite on #1D4FD8", PureWhite, DepthTokens.BrandButtonGradientEnd),
            Triple("DarkSlate #0F172A on Keypad #F1F4FB", OnBackgroundLight, DepthTokens.KeypadButtonGradientEnd),
            Triple("DarkSlate #0F172A on #F4F7FD", OnBackgroundLight, DepthTokens.PageBackground),
            Triple("Slate600 #475467 on #F4F7FD", OnSurfaceVariantLight, DepthTokens.PageBackground),
            Triple("Slate600 #475467 on #FFFFFF", OnSurfaceVariantLight, PureWhite)
        )
        pairs.forEach { (label, fg, bg) ->
            val ratio = wcagContrastRatio(fg, bg)
            assertTrue("$label must meet WCAG AA >= 4.5:1 (was $ratio)", ratio >= aaNormalText)
        }
    }

    @Test
    fun stage26D_removedStage26CExtrasAndQuickPinScrollContract() {
        val appDir = resolveAppModuleDir()
        val authCompSrc = File(appDir, "src/main/java/com/lojia/shiftreport/auth/LojiaAuthComponents.kt").readText()
        val headerSection = authCompSrc.substringAfter("fun LojiaHeader(").substringBefore("fun LojiaCard(")
        val gradientBtnSection = authCompSrc.substringAfter("fun LojiaGradientButton(").substringBefore("fun LojiaPasswordStrengthMeter(")

        assertFalse("Full-width blue LojiaGradientHeader banner must be removed from LojiaHeader", headerSection.contains("LojiaGradientHeader"))
        assertTrue("LojiaHeader must use depthLogoTile()", headerSection.contains("depthLogoTile()"))
        assertFalse("Stage 26C Check icon must be removed from LojiaGradientButton", gradientBtnSection.contains("Icons.Default.Check"))

        val quickPinSrc = File(appDir, "src/main/java/com/lojia/shiftreport/auth/QuickPinScreen.kt").readText()
        assertFalse("QuickPinScreen must use Row/Column instead of LazyVerticalGrid", quickPinSrc.contains("LazyVerticalGrid"))
        assertTrue("QuickPinScreen must wrap content in verticalScroll", quickPinSrc.contains("verticalScroll"))
        assertTrue("QuickPinScreen keypad must use depthKeypadKey", quickPinSrc.contains("depthKeypadKey"))
        assertTrue("QuickPinScreen must keep LTR keypad layout", quickPinSrc.contains("LayoutDirection.Ltr"))
    }
}
