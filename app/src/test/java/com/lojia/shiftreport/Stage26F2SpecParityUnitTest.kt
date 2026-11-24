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
import com.lojia.shiftreport.ui.theme.CashWellBg
import com.lojia.shiftreport.ui.theme.DepthCardBorder
import com.lojia.shiftreport.ui.theme.DepthStyle
import com.lojia.shiftreport.ui.theme.DepthTokens
import com.lojia.shiftreport.ui.theme.DepthTypeTokens
import com.lojia.shiftreport.ui.theme.Dimens
import com.lojia.shiftreport.ui.theme.ErrorRedLight
import com.lojia.shiftreport.ui.theme.FormWellBorder
import com.lojia.shiftreport.ui.theme.FormWellFill
import com.lojia.shiftreport.ui.theme.ListDividerGray
import com.lojia.shiftreport.ui.theme.OnSurfaceVariantLight
import com.lojia.shiftreport.ui.theme.OnTertiaryContainerLight
import com.lojia.shiftreport.ui.theme.PrimaryBlueLight
import com.lojia.shiftreport.ui.theme.PureWhite
import com.lojia.shiftreport.ui.theme.ReportCanvasBackground
import com.lojia.shiftreport.ui.theme.ReportInputBorder
import com.lojia.shiftreport.ui.theme.SecondaryTealLight
import com.lojia.shiftreport.ui.theme.SectionPurpleGradientEnd
import com.lojia.shiftreport.ui.theme.SectionRoseGradientEnd
import com.lojia.shiftreport.ui.theme.TextPrimaryReport
import com.lojia.shiftreport.ui.theme.TileTealEnd
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class Stage26F2SpecParityUnitTest {

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
            File(dir, "src/main/java/com/lojia/shiftreport/report/SecondaryLedgerTabs.kt").isFile
        } ?: error("Unable to locate app module directory from ${File(".").absolutePath}")
    }

    @Test
    fun stage26F2_exactBoardColorGradientAndDividerTokensMatch() {
        assertEquals(Color(0xFFF4F7FD), BackgroundLight)
        assertEquals(Color(0xFFEEF2FB), ReportCanvasBackground)
        assertEquals(Color(0xFF101828), TextPrimaryReport)
        assertEquals(Color(0xFF475467), OnSurfaceVariantLight)
        assertEquals(Color(0xFFC9D3E8), ReportInputBorder)
        assertEquals(Color(0xFFE5E9F5), DepthCardBorder)

        assertEquals(Color(0xFFEAECF0), ListDividerGray)
        assertEquals(ListDividerGray, DepthTokens.ListDividerColor)
        assertEquals(Color(0xFFE0F2F1), TileTealEnd)
        assertEquals(Color(0xFFF4F6FB), FormWellFill)
        assertEquals(FormWellFill, DepthTokens.FormWellBackground)
        assertEquals(Color(0xFFD0D5DD), FormWellBorder)
        assertEquals(FormWellBorder, DepthTokens.FormWellStrokeColor)

        assertEquals(0.06f, DepthTokens.ICON_TILE_SHADOW_ALPHA, 1e-6f)
        assertEquals(0.0f, DepthTokens.ICON_TILE_TOP_HIGHLIGHT_ALPHA, 1e-6f)
        assertEquals(0.0f, DepthTokens.ICON_TILE_BOTTOM_BEVEL_ALPHA, 1e-6f)
        assertEquals(0.0f, DepthTokens.INSET_WELL_TOP_SHADOW_ALPHA, 1e-6f)

        val style = DepthStyle()
        assertEquals(DepthTokens.ListDividerColor, style.listDividerColor)
        assertEquals(DepthTokens.ReportPageBackground, style.reportPageBackground)
    }

    @Test
    fun stage26F2_exactBoardGeometryAndTypographyTokensMatch() {
        assertEquals(40.dp, Dimens.PaidOutIconTileSize)
        assertEquals(42.dp, Dimens.SettingsIconTileSize)
        assertEquals(44.dp, Dimens.EmployeeAvatarTileSize)
        assertEquals(13.dp, Dimens.RadiusSettingsIconTile)
        assertEquals(14.dp, Dimens.RadiusEmployeeAvatarTile)
        assertEquals(10.dp, Dimens.RadiusActionIconTile)
        assertEquals(1.dp, Dimens.IconTileShadowOffsetY)
        assertEquals(4.dp, Dimens.IconTileShadowBlur)
        assertEquals(0.dp, Dimens.IconTileBottomBevelHeight)
        assertEquals(1.dp, Dimens.RowDividerWidth)
        assertEquals(12.dp, Dimens.SettingsIconTitleGap)
        assertEquals(12.dp, Dimens.ListRowPaddingVertical)
        assertEquals(20.dp, Dimens.BottomSheetCornerRadius)
        assertEquals(22.dp, Dimens.CardCornerRadius)
        assertEquals(14.dp, Dimens.RadiusReportInput)

        assertEquals(12.sp, DepthTypeTokens.SettingsSectionHeaderSize)
        assertEquals(0.48.sp, DepthTypeTokens.SettingsSectionHeaderTracking)
        assertEquals(14.sp, DepthTypeTokens.ListRowTitleSize)
        assertEquals(12.sp, DepthTypeTokens.ListRowSubtitleSize)
        assertEquals(15.sp, DepthTypeTokens.CardSectionHeadingSize)
        assertEquals(14.sp, DepthTypeTokens.PrimaryCtaSmallSize)
        assertEquals(12.sp, DepthTypeTokens.ChipLabelSize)
        assertEquals(13.sp, DepthTypeTokens.SegmentedChipTextSize)
        assertEquals(18.sp, DepthTypeTokens.StatCountSize)
        assertEquals(16.sp, DepthTypeTokens.EmptyStateTitleSize)
    }

    @Test
    fun stage26F2_settingsPaidOutAndEmployeeColorPairsMeetWcagAA() {
        val aaNormalText = 4.5
        val pairs = listOf(
            Triple("TextPrimaryReport on ReportCanvasBackground", TextPrimaryReport, ReportCanvasBackground),
            Triple("TextPrimaryReport on PureWhite", TextPrimaryReport, PureWhite),
            Triple("OnSurfaceVariantLight on ReportCanvasBackground", OnSurfaceVariantLight, ReportCanvasBackground),
            Triple("OnSurfaceVariantLight on PureWhite", OnSurfaceVariantLight, PureWhite),
            Triple("PrimaryBlueLight on PureWhite", PrimaryBlueLight, PureWhite),
            Triple("PrimaryBlueLight on BadgeBlueBg", PrimaryBlueLight, BadgeBlueBg),
            Triple("BadgeBlueText on CashWellBg", BadgeBlueText, CashWellBg),
            Triple("BadgeBlueText on BadgeBlueBg", BadgeBlueText, BadgeBlueBg),
            Triple("BadgeGreenText on TileTealEnd", BadgeGreenText, TileTealEnd),
            Triple("BadgeGreenText on BadgeGreenBg", BadgeGreenText, BadgeGreenBg),
            Triple("SecondaryTealLight on TileTealEnd", SecondaryTealLight, TileTealEnd),
            Triple("BadgeAmberAccent on BadgeAmberBg", BadgeAmberAccent, BadgeAmberBg),
            Triple("BadgeAmberTextDark on BadgeAmberBg", BadgeAmberTextDark, BadgeAmberBg),
            Triple("ErrorRedLight on SectionRoseGradientEnd", ErrorRedLight, SectionRoseGradientEnd),
            Triple("OnTertiaryContainerLight on SectionPurpleGradientEnd", OnTertiaryContainerLight, SectionPurpleGradientEnd)
        )
        pairs.forEach { (label, fg, bg) ->
            val ratio = wcagContrastRatio(fg, bg)
            assertTrue("$label must meet WCAG AA >= 4.5:1 (was $ratio)", ratio >= aaNormalText)
        }
    }

    @Test
    fun stage26F2_zeroColorLiteralsOutsideThemeAndScreenContractsPreserved() {
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
            "MainActivity must apply reportPageBackground to SettingsDetail",
            mainActivitySrc.contains("AppNavState.ShiftReportState.SettingsDetail") &&
                mainActivitySrc.contains("reportPageBackground")
        )

        val secondaryLedgerSrc = File(appDir, "src/main/java/com/lojia/shiftreport/report/SecondaryLedgerTabs.kt").readText()
        assertTrue(
            "PaidOutShoppingLedgerTab must use depthRaisedIconTile and listDividerColor",
            secondaryLedgerSrc.contains("depthRaisedIconTile") && secondaryLedgerSrc.contains("listDividerColor")
        )
        val walkoutIndex = secondaryLedgerSrc.indexOf("fun WalkoutLedgerTab")
        assertTrue("WalkoutLedgerTab must exist", walkoutIndex > 0)
        val walkoutBody = secondaryLedgerSrc.substring(walkoutIndex)
        assertTrue(
            "WalkoutLedgerTab must remain untouched by Stage 26F-2 icon tile restyle",
            !walkoutBody.contains("depthRaisedIconTile")
        )

        val settingsReportSrc = File(appDir, "src/main/java/com/lojia/shiftreport/settings/SettingsReportSection.kt").readText()
        assertTrue(
            "SettingsReportSection must use depthRaisedIconTile and listDividerColor",
            settingsReportSrc.contains("depthRaisedIconTile") && settingsReportSrc.contains("listDividerColor")
        )

        val regionalSrc = File(appDir, "src/main/java/com/lojia/shiftreport/settings/RegionalPreferencesSection.kt").readText()
        assertTrue(
            "RegionalPreferencesSection must use depthRaisedIconTile and RadiusReportInput",
            regionalSrc.contains("depthRaisedIconTile") && regionalSrc.contains("RadiusReportInput")
        )

        val employeeSrc = File(appDir, "src/main/java/com/lojia/shiftreport/settings/EmployeeManagementSection.kt").readText()
        assertTrue(
            "EmployeeManagementSection must use reportPageBackground, EmployeeAvatarTileSize, and depthRaisedIconTile",
            employeeSrc.contains("reportPageBackground") &&
                employeeSrc.contains("EmployeeAvatarTileSize") &&
                employeeSrc.contains("depthRaisedIconTile")
        )
    }
}
