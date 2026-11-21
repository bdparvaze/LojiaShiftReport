package com.lojia.shiftreport

import androidx.compose.ui.graphics.Color
import com.lojia.shiftreport.report.SHIFT_REPORT_TABS
import com.lojia.shiftreport.report.ShiftReportTab
import com.lojia.shiftreport.ui.theme.DepthTokens
import com.lojia.shiftreport.ui.theme.ErrorRedLight
import com.lojia.shiftreport.ui.theme.InfoContainer
import com.lojia.shiftreport.ui.theme.NegativeBannerSurface
import com.lojia.shiftreport.ui.theme.NegativeBannerText
import com.lojia.shiftreport.ui.theme.OnBackgroundLight
import com.lojia.shiftreport.ui.theme.OnSurfaceVariantLight
import com.lojia.shiftreport.ui.theme.PosCardBlue
import com.lojia.shiftreport.ui.theme.PosCashGreen
import com.lojia.shiftreport.ui.theme.PosDueOrange
import com.lojia.shiftreport.ui.theme.PosExpenseRed
import com.lojia.shiftreport.ui.theme.PositiveBannerSurface
import com.lojia.shiftreport.ui.theme.PositiveBannerText
import com.lojia.shiftreport.ui.theme.PositiveBannerTextDark
import com.lojia.shiftreport.ui.theme.PrimaryContainerLight
import com.lojia.shiftreport.ui.theme.PrimaryIndigoDark
import com.lojia.shiftreport.ui.theme.PureWhite
import com.lojia.shiftreport.ui.theme.SecondaryContainerLight
import com.lojia.shiftreport.ui.theme.ShiftColors
import com.lojia.shiftreport.ui.theme.SuccessContainer
import com.lojia.shiftreport.ui.theme.WarningOrange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class Stage26BDepthApplyUnitTest {

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
        val candidates = listOf(
            File("."),
            File("app"),
            File(".."),
            File("../app")
        )
        return candidates.firstOrNull { dir ->
            File(dir, "src/main/java/com/lojia/shiftreport/report/MonthlySalesSummaryView.kt").isFile
        } ?: error(
            "Unable to locate app module source root from working directory: ${File(".").absolutePath}"
        )
    }

    @Test
    fun noHardcodedColorLiterals_inCleanedStage26BFiles() {
        val appDir = resolveAppModuleDir()
        val rawHexColorRegex = Regex("""Color\(0x[0-9A-Fa-f]+\)""")
        val cleanedFiles = listOf(
            "src/main/java/com/lojia/shiftreport/report/MonthlySalesSummaryView.kt",
            "src/main/java/com/lojia/shiftreport/report/DashboardScreen.kt",
            "src/main/java/com/lojia/shiftreport/ui/common/AppDrawer.kt"
        )

        cleanedFiles.forEach { relativePath ->
            val file = File(appDir, relativePath)
            assertTrue("Expected file to exist: ${file.path}", file.isFile)
            val content = file.readText()
            val matches = rawHexColorRegex.findAll(content).map { it.value }.toList()
            assertEquals(
                "Expected zero hardcoded Color(0x...) literals in $relativePath, found: $matches",
                emptyList<String>(),
                matches
            )
        }
    }

    @Test
    fun shiftReportTabs_orderMatchesContract() {
        assertEquals(
            listOf(
                ShiftReportTab.REPORT_ENTRY,
                ShiftReportTab.DUE_HISTORY,
                ShiftReportTab.EMPLOYER_ADVANCES,
                ShiftReportTab.PAID_OUT,
                ShiftReportTab.WALKOUT,
                ShiftReportTab.ARCHIVES
            ),
            SHIFT_REPORT_TABS
        )
    }

    @Test
    fun wcagContrast_sectionHeaderTextOnTintBackgroundsMeetsAA() {
        val aaNormalText = 4.5
        val tintBackgrounds = mapOf(
            "BrassLight" to ShiftColors.BrassLight,
            "DangerLight" to ShiftColors.DangerLight,
            "PurpleLight" to ShiftColors.PurpleLight,
            "PrimaryContainerLight" to PrimaryContainerLight,
            "SecondaryContainerLight" to SecondaryContainerLight,
            "SuccessContainer" to SuccessContainer,
            "InfoContainer" to InfoContainer
        )

        tintBackgrounds.forEach { (bgName, bgColor) ->
            val ratio = wcagContrastRatio(ShiftColors.Charcoal, bgColor)
            assertTrue(
                "SectionHeader title (ShiftColors.Charcoal) on $bgName must meet 4.5:1 (was $ratio)",
                ratio >= aaNormalText
            )
        }

        assertTrue(
            "ShiftColors.Brass on ShiftColors.BrassLight must meet 4.5:1",
            wcagContrastRatio(ShiftColors.Brass, ShiftColors.BrassLight) >= aaNormalText
        )
        assertTrue(
            "ShiftColors.Danger on ShiftColors.DangerLight must meet 4.5:1",
            wcagContrastRatio(ShiftColors.Danger, ShiftColors.DangerLight) >= aaNormalText
        )
        assertTrue(
            "ShiftColors.Purple on ShiftColors.PurpleLight must meet 4.5:1",
            wcagContrastRatio(ShiftColors.Purple, ShiftColors.PurpleLight) >= aaNormalText
        )
    }

    @Test
    fun wcagContrast_reportAndSummarySemanticTextOnDepthCardSurfacesMeetsAA() {
        val aaNormalText = 4.5
        val cardSurfaces = mapOf(
            "PureWhite" to PureWhite,
            "CardGradientEnd" to DepthTokens.CardGradientEnd
        )
        val foregrounds = mapOf(
            "OnBackgroundLight" to OnBackgroundLight,
            "OnSurfaceVariantLight" to OnSurfaceVariantLight,
            "PosCashGreen" to PosCashGreen,
            "PosDueOrange" to PosDueOrange,
            "PosExpenseRed" to PosExpenseRed,
            "PosCardBlue" to PosCardBlue,
            "WarningOrange" to WarningOrange,
            "ShiftColors.Primary" to ShiftColors.Primary,
            "ShiftColors.Purple" to ShiftColors.Purple
        )

        cardSurfaces.forEach { (surfaceName, surfaceColor) ->
            foregrounds.forEach { (fgName, fgColor) ->
                val ratio = wcagContrastRatio(fgColor, surfaceColor)
                assertTrue(
                    "$fgName on $surfaceName must meet 4.5:1 (was $ratio)",
                    ratio >= aaNormalText
                )
            }
        }
    }

    @Test
    fun wcagContrast_bannersButtonsAndTabBarMeetAA() {
        val aaNormalText = 4.5

        assertTrue(
            "PositiveBannerText on PositiveBannerSurface must meet 4.5:1",
            wcagContrastRatio(PositiveBannerText, PositiveBannerSurface) >= aaNormalText
        )
        assertTrue(
            "PositiveBannerTextDark on PositiveBannerSurface must meet 4.5:1",
            wcagContrastRatio(PositiveBannerTextDark, PositiveBannerSurface) >= aaNormalText
        )
        assertTrue(
            "NegativeBannerText on NegativeBannerSurface must meet 4.5:1",
            wcagContrastRatio(NegativeBannerText, NegativeBannerSurface) >= aaNormalText
        )
        assertTrue(
            "ErrorRedLight on NegativeBannerSurface must meet 4.5:1",
            wcagContrastRatio(ErrorRedLight, NegativeBannerSurface) >= aaNormalText
        )

        assertTrue(
            "PureWhite on BrandButtonGradientStart must meet 4.5:1",
            wcagContrastRatio(PureWhite, DepthTokens.BrandButtonGradientStart) >= aaNormalText
        )
        assertTrue(
            "PureWhite on BrandButtonGradientEnd must meet 4.5:1",
            wcagContrastRatio(PureWhite, DepthTokens.BrandButtonGradientEnd) >= aaNormalText
        )
        assertTrue(
            "PureWhite on DestructiveButtonGradientStart must meet 4.5:1",
            wcagContrastRatio(PureWhite, DepthTokens.DestructiveButtonGradientStart) >= aaNormalText
        )
        assertTrue(
            "PureWhite on DestructiveButtonGradientEnd must meet 4.5:1",
            wcagContrastRatio(PureWhite, DepthTokens.DestructiveButtonGradientEnd) >= aaNormalText
        )

        assertTrue(
            "OnSurfaceVariantLight on RaisedButtonDisabledStart must meet 4.5:1",
            wcagContrastRatio(OnSurfaceVariantLight, DepthTokens.RaisedButtonDisabledStart) >= aaNormalText
        )
        assertTrue(
            "OnSurfaceVariantLight on RaisedButtonDisabledEnd must meet 4.5:1",
            wcagContrastRatio(OnSurfaceVariantLight, DepthTokens.RaisedButtonDisabledEnd) >= aaNormalText
        )

        assertTrue(
            "PrimaryIndigoDark on TabBarGradientEnd must meet 4.5:1",
            wcagContrastRatio(PrimaryIndigoDark, DepthTokens.TabBarGradientEnd) >= aaNormalText
        )
        assertTrue(
            "OnSurfaceVariantLight on TabBarGradientEnd must meet 4.5:1",
            wcagContrastRatio(OnSurfaceVariantLight, DepthTokens.TabBarGradientEnd) >= aaNormalText
        )
    }
}
