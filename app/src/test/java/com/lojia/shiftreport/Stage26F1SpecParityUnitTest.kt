package com.lojia.shiftreport

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojia.shiftreport.ui.theme.BackgroundLight
import com.lojia.shiftreport.ui.theme.BadgeAmberAccent
import com.lojia.shiftreport.ui.theme.BadgeAmberBg
import com.lojia.shiftreport.ui.theme.BadgeAmberTextDark
import com.lojia.shiftreport.ui.theme.BadgeBlueBg
import com.lojia.shiftreport.ui.theme.BadgeBlueText
import com.lojia.shiftreport.ui.theme.BadgeGreenBg
import com.lojia.shiftreport.ui.theme.BadgeGreenText
import com.lojia.shiftreport.ui.theme.BankWellBg
import com.lojia.shiftreport.ui.theme.BankWellText
import com.lojia.shiftreport.ui.theme.BrandHeaderDiagonalEnd
import com.lojia.shiftreport.ui.theme.BrandHeaderDiagonalStart
import com.lojia.shiftreport.ui.theme.CashWellBg
import com.lojia.shiftreport.ui.theme.DepthCardBorder
import com.lojia.shiftreport.ui.theme.DepthMotionTokens
import com.lojia.shiftreport.ui.theme.DepthStyle
import com.lojia.shiftreport.ui.theme.DepthTokens
import com.lojia.shiftreport.ui.theme.DepthTypeTokens
import com.lojia.shiftreport.ui.theme.Dimens
import com.lojia.shiftreport.ui.theme.KpiCollectedCardBorder
import com.lojia.shiftreport.ui.theme.KpiCollectedCardEnd
import com.lojia.shiftreport.ui.theme.KpiCollectedCardStart
import com.lojia.shiftreport.ui.theme.KpiDueCardBorder
import com.lojia.shiftreport.ui.theme.KpiDueCardEnd
import com.lojia.shiftreport.ui.theme.KpiDueCardStart
import com.lojia.shiftreport.ui.theme.LiveDotGlowGreen
import com.lojia.shiftreport.ui.theme.PrimaryBlueLight
import com.lojia.shiftreport.ui.theme.PureWhite
import com.lojia.shiftreport.ui.theme.ReportCanvasBackground
import com.lojia.shiftreport.ui.theme.ReportInputBorder
import com.lojia.shiftreport.ui.theme.SectionAmberBorder
import com.lojia.shiftreport.ui.theme.SectionAmberGradientEnd
import com.lojia.shiftreport.ui.theme.SectionAmberGradientStart
import com.lojia.shiftreport.ui.theme.SectionPurpleBorder
import com.lojia.shiftreport.ui.theme.SectionPurpleGradientEnd
import com.lojia.shiftreport.ui.theme.SectionPurpleGradientStart
import com.lojia.shiftreport.ui.theme.SectionRoseBorder
import com.lojia.shiftreport.ui.theme.SectionRoseGradientEnd
import com.lojia.shiftreport.ui.theme.SectionRoseGradientStart
import com.lojia.shiftreport.ui.theme.SegmentTrackBg
import com.lojia.shiftreport.ui.theme.TextPlaceholderReport
import com.lojia.shiftreport.ui.theme.TextPrimaryReport
import com.lojia.shiftreport.ui.theme.TextSlate700
import com.lojia.shiftreport.ui.theme.cardRiseDelayMillis
import com.lojia.shiftreport.ui.theme.shouldAnimate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class Stage26F1SpecParityUnitTest {

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
            File(dir, "src/main/java/com/lojia/shiftreport/report/ShiftReportScreen.kt").isFile
        } ?: error("Unable to locate app module directory from ${File(".").absolutePath}")
    }

    @Test
    fun stage26F1_exactBoardColorGradientAndBorderTokensMatch() {
        assertEquals(Color(0xFFEEF2FB), ReportCanvasBackground)
        assertEquals(Color(0xFFEEF2FB), DepthTokens.ReportPageBackground)
        assertEquals(Color(0xFFF4F7FD), BackgroundLight)

        assertEquals(Color(0xFF2B5DE8), BrandHeaderDiagonalStart)
        assertEquals(Color(0xFF1238A8), BrandHeaderDiagonalEnd)
        assertEquals(PureWhite.copy(red = 0f, green = 0f, blue = 0f), DepthTokens.HeaderShadowColor)
        assertEquals(0.0f, DepthTokens.HEADER_SHADOW_ALPHA, 1e-6f)
        assertEquals(0.0f, DepthTokens.TAB_BAR_SHADOW_ALPHA, 1e-6f)
        assertEquals(0.0f, DepthTokens.TAB_INDICATOR_GLOW_ALPHA, 1e-6f)

        assertEquals(Color(0xFF101828), TextPrimaryReport)
        assertEquals(Color(0xFF344054), TextSlate700)
        assertEquals(Color(0xFF667085), TextPlaceholderReport)
        assertEquals(Color(0xFFC9D3E8), ReportInputBorder)
        assertEquals(Color(0xFFE5E9F5), DepthCardBorder)

        assertEquals(Color(0xFFE0EAFF), BadgeBlueBg)
        assertEquals(Color(0xFF1D3FA8), BadgeBlueText)
        assertEquals(Color(0xFFEEF3FF), CashWellBg)
        assertEquals(Color(0xFFE6F7F5), BankWellBg)
        assertEquals(Color(0xFF0B6E63), BankWellText)
        assertEquals(Color(0xFFD1FADF), BadgeGreenBg)
        assertEquals(Color(0xFF05603A), BadgeGreenText)
        assertEquals(Color(0xFFFEF0C7), BadgeAmberBg)
        assertEquals(Color(0xFF7A2E0E), BadgeAmberTextDark)
        assertEquals(Color(0xFFB54708), BadgeAmberAccent)
        assertEquals(Color(0xFFE9EDF6), SegmentTrackBg)

        assertEquals(Color(0xFFFFF6D8), SectionAmberGradientStart)
        assertEquals(Color(0xFFFEF0C7), SectionAmberGradientEnd)
        assertEquals(Color(0xFFF2D77E), SectionAmberBorder)
        assertEquals(Color(0xFFFFEDEC), SectionRoseGradientStart)
        assertEquals(Color(0xFFFEE4E2), SectionRoseGradientEnd)
        assertEquals(Color(0xFFF1A7A7), SectionRoseBorder)
        assertEquals(Color(0xFFF3F0FF), SectionPurpleGradientStart)
        assertEquals(Color(0xFFEDE9FE), SectionPurpleGradientEnd)
        assertEquals(Color(0xFFB9A8F0), SectionPurpleBorder)

        assertEquals(Color(0xFFFFFFFF), KpiDueCardStart)
        assertEquals(Color(0xFFFEF3F2), KpiDueCardEnd)
        assertEquals(Color(0xFFF6C7C3), KpiDueCardBorder)
        assertEquals(Color(0xFFFFFFFF), KpiCollectedCardStart)
        assertEquals(Color(0xFFECFDF3), KpiCollectedCardEnd)
        assertEquals(Color(0xFFA6E9C5), KpiCollectedCardBorder)
        assertEquals(Color(0xFF12B76A), LiveDotGlowGreen)

        val style = DepthStyle()
        assertEquals(DepthTokens.ReportPageBackground, style.reportPageBackground)
        assertEquals(Color(0xFFD5DBE6), style.reportFieldBorderColor)
        assertEquals(TextPrimaryReport, style.reportFieldTextColor)
        assertEquals(TextSlate700, style.reportFieldLabelColor)
        assertEquals(Color(0xFF64748B), style.reportFieldPlaceholderColor)
        assertEquals(0.0f, style.reportFieldInsetAlpha, 1e-6f)
    }

    @Test
    fun stage26F1_exactBoardGeometryAndTypographyTokensMatch() {
        assertEquals(56.dp, Dimens.TopBarHeight)
        assertEquals(0.dp, Dimens.HeaderShadowOffsetY)
        assertEquals(0.dp, Dimens.HeaderShadowBlur)
        assertEquals(44.dp, Dimens.TabBarMinHeight)
        assertEquals(0.dp, Dimens.TabBarShadowOffsetY)
        assertEquals(0.dp, Dimens.TabBarShadowBlur)
        assertEquals(3.dp, Dimens.TabIndicatorHeight)
        assertEquals(0.dp, Dimens.TabIndicatorGlowOffsetY)
        assertEquals(0.dp, Dimens.TabIndicatorGlowBlur)

        assertEquals(8.dp, Dimens.RadiusReportInput)
        assertEquals(10.dp, Dimens.CardCornerRadius)
        assertEquals(10.dp, Dimens.RadiusKpiCard)
        assertEquals(10.dp, Dimens.RadiusSegmentTrack)
        assertEquals(8.dp, Dimens.RadiusSegmentThumb)
        assertEquals(8.dp, Dimens.RadiusSectionIconTile)
        assertEquals(999.dp, Dimens.RadiusFull)

        assertEquals(44.dp, Dimens.TextFieldHeightReport)
        assertEquals(44.dp, Dimens.ButtonHeightReportPrimary)
        assertEquals(36.dp, Dimens.SectionHeaderHeight)
        assertEquals(28.dp, Dimens.SectionIconTileSize)
        assertEquals(32.dp, Dimens.KpiIconTileSize)
        assertEquals(40.dp, Dimens.SegmentTrackHeight)
        assertEquals(32.dp, Dimens.SegmentThumbHeight)
        assertEquals(8.dp, Dimens.ReportGridGap)
        assertEquals(12.dp, Dimens.ReportCardInnerPadding)
        assertEquals(3.dp, Dimens.LedgerSectionAccentWidth)
        assertEquals(16.dp, Dimens.LedgerSectionAccentHeight)
        assertEquals(36.dp, Dimens.AvatarBadgeSize)
        assertEquals(32.dp, Dimens.SerialBadgeSize)

        assertEquals(16.sp, DepthTypeTokens.TopBarTitleSize)
        assertEquals(13.sp, DepthTypeTokens.TabLabelSize)
        assertEquals(12.sp, DepthTypeTokens.ReportFieldLabelSize)
        assertEquals(14.sp, DepthTypeTokens.ReportFieldTextSize)
        assertEquals(13.sp, DepthTypeTokens.SectionTitleSize)
        assertEquals(20.sp, DepthTypeTokens.HeroAmountSize)
    }

    @Test
    fun stage26F1_motionTokensAndReducedMotionGuardMatch() {
        assertEquals(550, DepthMotionTokens.CARD_RISE_DURATION_MS)
        assertEquals(100, DepthMotionTokens.CARD_RISE_STAGGER_STEP_MS)
        assertEquals(600, DepthMotionTokens.CARD_RISE_MAX_DELAY_MS)
        assertEquals(150, DepthMotionTokens.PRESS_DURATION_MS)
        assertEquals(0.dp, Dimens.CardRiseTranslationY)

        assertFalse(shouldAnimate(0f))
        assertFalse(shouldAnimate(-1f))
        assertFalse(shouldAnimate(Float.NaN))
        assertTrue(shouldAnimate(1f))

        assertEquals(0, cardRiseDelayMillis(0))
        assertEquals(100, cardRiseDelayMillis(1))
        assertEquals(600, cardRiseDelayMillis(6))
        assertEquals(600, cardRiseDelayMillis(10))
    }

    @Test
    fun stage26F1_reportAndLedgerColorPairsMeetWcagAA() {
        val aaNormalText = 4.5
        val pairs = listOf(
            Triple("TextPrimaryReport on ReportCanvasBackground", TextPrimaryReport, ReportCanvasBackground),
            Triple("TextPrimaryReport on PureWhite", TextPrimaryReport, PureWhite),
            Triple("TextSlate700 on PureWhite", TextSlate700, PureWhite),
            Triple("TextSlate700 on ReportCanvasBackground", TextSlate700, ReportCanvasBackground),
            Triple("TextPlaceholderReport on PureWhite", TextPlaceholderReport, PureWhite),
            Triple("TextPlaceholderReport on InsetFieldBackground", TextPlaceholderReport, DepthTokens.InsetFieldBackground),
            Triple("BadgeBlueText on BadgeBlueBg", BadgeBlueText, BadgeBlueBg),
            Triple("BadgeBlueText on CashWellBg", BadgeBlueText, CashWellBg),
            Triple("BankWellText on BankWellBg", BankWellText, BankWellBg),
            Triple("BadgeGreenText on BadgeGreenBg", BadgeGreenText, BadgeGreenBg),
            Triple("BadgeAmberTextDark on BadgeAmberBg", BadgeAmberTextDark, BadgeAmberBg),
            Triple("BadgeAmberAccent on BadgeAmberBg", BadgeAmberAccent, BadgeAmberBg),
            Triple("PureWhite on BrandHeaderDiagonalStart", PureWhite, BrandHeaderDiagonalStart),
            Triple("PureWhite on BrandHeaderDiagonalEnd", PureWhite, BrandHeaderDiagonalEnd),
            Triple("PrimaryBlueLight on PureWhite", PrimaryBlueLight, PureWhite)
        )
        pairs.forEach { (label, fg, bg) ->
            val ratio = wcagContrastRatio(fg, bg)
            assertTrue("$label must meet WCAG AA >= 4.5:1 (was $ratio)", ratio >= aaNormalText)
        }
    }

    @Test
    fun stage26F1_zeroColorLiteralsOutsideThemeAndScreenContractsPreserved() {
        val appDir = resolveAppModuleDir()
        val javaRoot = File(appDir, "src/main/java")
        val offendingFiles = javaRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { !it.invariantSeparatorsPath.contains("/ui/theme/") }
            .filter { it.readText().contains("Color(0x") }
            .map { it.relativeTo(appDir).path }
            .toList()
        assertTrue(
            "Zero Color(0x literals allowed outside ui/theme/, found in: $offendingFiles",
            offendingFiles.isEmpty()
        )

        val mainActivitySrc = File(appDir, "src/main/java/com/lojia/shiftreport/MainActivity.kt").readText()
        assertTrue(
            "MainActivity must apply reportPageBackground when viewing Reports",
            mainActivitySrc.contains("reportPageBackground")
        )
        assertTrue(
            "MainActivity must keep MaterialTheme.colorScheme.background for non-Reports routes",
            mainActivitySrc.contains("MaterialTheme.colorScheme.background")
        )

        val shiftScreenSrc = File(appDir, "src/main/java/com/lojia/shiftreport/report/ShiftReportScreen.kt").readText()
        assertTrue("ShiftReportScreen must use reportPageBackground", shiftScreenSrc.contains("reportPageBackground"))
        assertTrue("ShiftReportScreen must use depthTabBarShadow", shiftScreenSrc.contains("depthTabBarShadow"))
        assertTrue("ShiftReportScreen must use depthTabIndicatorGlow", shiftScreenSrc.contains("depthTabIndicatorGlow"))

        val shiftCompSrc = File(appDir, "src/main/java/com/lojia/shiftreport/report/ShiftReportComponents.kt").readText()
        assertTrue("ShiftReportComponents must use depthSectionBanner", shiftCompSrc.contains("depthSectionBanner"))
        assertTrue("ShiftReportComponents must use depthRaisedTile", shiftCompSrc.contains("depthRaisedTile"))

        val ledgerSrc = File(appDir, "src/main/java/com/lojia/shiftreport/report/ShiftReportLedgerTabs.kt").readText()
        assertTrue("ShiftReportLedgerTabs must use depthGradientCard", ledgerSrc.contains("depthGradientCard"))
        assertTrue("ShiftReportLedgerTabs must use depthSegmentActiveThumb", ledgerSrc.contains("depthSegmentActiveThumb"))
    }
}
