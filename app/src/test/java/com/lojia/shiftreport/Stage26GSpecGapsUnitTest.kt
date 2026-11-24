package com.lojia.shiftreport

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojia.shiftreport.ui.theme.BadgeBlueText
import com.lojia.shiftreport.ui.theme.CashWellBg
import com.lojia.shiftreport.ui.theme.DepthTokens
import com.lojia.shiftreport.ui.theme.DepthTypeTokens
import com.lojia.shiftreport.ui.theme.DialCodeTextColor
import com.lojia.shiftreport.ui.theme.Dimens
import com.lojia.shiftreport.ui.theme.OnSurfaceVariantLight
import com.lojia.shiftreport.ui.theme.OutlineLight
import com.lojia.shiftreport.ui.theme.OutlineVariantLight
import com.lojia.shiftreport.ui.theme.PrimaryIndigoLight
import com.lojia.shiftreport.ui.theme.PureWhite
import com.lojia.shiftreport.ui.theme.RecoveryInfoBorder
import com.lojia.shiftreport.ui.theme.RegisterNoteFill
import com.lojia.shiftreport.ui.theme.ReportInputBorder
import com.lojia.shiftreport.ui.theme.ReportReadOnlyFill
import com.lojia.shiftreport.ui.theme.TextPlaceholderReport
import com.lojia.shiftreport.ui.theme.TextPrimaryReport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class Stage26GSpecGapsUnitTest {

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
            File(dir, "src/main/java/com/lojia/shiftreport/auth/LojiaRegisterScreen.kt").isFile
        } ?: error("Unable to locate app module directory from ${File(".").absolutePath}")
    }

    @Test
    fun stage26G_exactSpecGapColorGradientAndAlphaTokensMatch() {
        // Gap 1: Register dial-code button (Stage 29A flat UI)
        assertEquals(Color(0xFF334155), DialCodeTextColor)
        assertEquals(DialCodeTextColor, DepthTokens.DialButtonTextColor)
        assertEquals(Color(0xFFD5DBE6), DepthTokens.DialButtonBorderColor)
        assertEquals(PureWhite, DepthTokens.DialButtonLipColor)
        assertEquals(PureWhite, DepthTokens.DialButtonGradientStart)
        assertEquals(PureWhite, DepthTokens.DialButtonGradientEnd)

        // Gap 2: Register note box (Stage 29A flat UI)
        assertEquals(Color(0xFFF1F5FB), RegisterNoteFill)
        assertEquals(RegisterNoteFill, DepthTokens.RegisterNoteBackground)
        assertEquals(OutlineVariantLight, DepthTokens.RegisterNoteBorderColor)
        assertEquals(0.0f, DepthTokens.REGISTER_NOTE_SHADOW_ALPHA, 1e-6f)

        // Gap 3: Password recovery info block & 46dp icon tile (Stage 29A flat UI)
        assertEquals(Color(0xFFEEF3FF), CashWellBg)
        assertEquals(CashWellBg, DepthTokens.RecoveryInfoBackground)
        assertEquals(Color(0xFFD0DDFF), RecoveryInfoBorder)
        assertEquals(RecoveryInfoBorder, DepthTokens.RecoveryInfoBorderColor)
        assertEquals(CashWellBg, DepthTokens.RecoveryTileGradientStart)
        assertEquals(CashWellBg, DepthTokens.RecoveryTileGradientEnd)

        // Gap 4: Report Entry read-only/disabled numeric wells (Stage 29A flat UI)
        assertEquals(Color(0xFFEEF1F7), ReportReadOnlyFill)
        assertEquals(ReportReadOnlyFill, DepthTokens.ReportReadOnlyBackground)
        assertEquals(Color(0xFFD5DBE6), DepthTokens.ReportReadOnlyBorderColor)
        assertEquals(Color(0xFF64748B), DepthTokens.ReportReadOnlyTextColor)
        assertEquals(0.0f, DepthTokens.REPORT_READ_ONLY_INSET_ALPHA, 1e-6f)
    }

    @Test
    fun stage26G_exactSpecGapGeometryAndTypographyTokensMatch() {
        // Gap 1: Dial-code button geometry & typography (Stage 29A flat UI)
        assertEquals(78.dp, Dimens.DialButtonWidth)
        assertEquals(46.dp, Dimens.DialButtonHeight)
        assertEquals(10.dp, Dimens.RadiusInputRegister)
        assertEquals(0.dp, Dimens.DialButtonLipHeight)
        assertEquals(10.dp, Dimens.DialButtonPaddingStart)
        assertEquals(8.dp, Dimens.DialButtonPaddingEnd)
        assertEquals(13.5.sp, DepthTypeTokens.DialButtonTextSize)

        // Gap 2: Register note box geometry (Stage 29A flat UI)
        assertEquals(12.dp, Dimens.RegisterNotePadding)
        assertEquals(0.dp, Dimens.RegisterNoteShadowOffsetY)
        assertEquals(0.dp, Dimens.RegisterNoteShadowBlur)

        // Gap 3: Recovery info block & tile geometry (Stage 29A flat UI)
        assertEquals(10.dp, Dimens.RecoveryInfoPadding)
        assertEquals(46.dp, Dimens.RecoveryTileSize)
        assertEquals(15.dp, Dimens.RadiusRecoveryTile)
        assertEquals(1.dp, Dimens.RecoveryTileShadowOffsetY)
        assertEquals(4.dp, Dimens.RecoveryTileShadowBlur)

        // Gap 4: Report read-only well geometry (Stage 29A flat UI)
        assertEquals(52.dp, Dimens.TextFieldHeightReport)
        assertEquals(14.dp, Dimens.RadiusReportInput)
        assertEquals(14.dp, Dimens.InsetFieldPaddingHorizontal)
        assertEquals(0.dp, Dimens.ReportReadOnlyShadowOffsetY)
        assertEquals(0.dp, Dimens.ReportReadOnlyShadowBlur)
        assertEquals(16.sp, DepthTypeTokens.ReportFieldTextSize)
    }

    @Test
    fun stage26G_colorPairsMeetWcagContrastRequirements() {
        val aaNormal = 4.5
        val normalPairs = listOf(
            Triple("DialCodeTextColor on PureWhite", DialCodeTextColor, PureWhite),
            Triple("DialCodeTextColor on DialButtonGradientEnd", DialCodeTextColor, DepthTokens.DialButtonGradientEnd),
            Triple("TextPrimaryReport on RegisterNoteFill", TextPrimaryReport, RegisterNoteFill),
            Triple("OnSurfaceVariantLight on RegisterNoteFill", OnSurfaceVariantLight, RegisterNoteFill),
            Triple("TextPrimaryReport on RecoveryInfoBackground", TextPrimaryReport, DepthTokens.RecoveryInfoBackground),
            Triple("PrimaryIndigoLight on RecoveryInfoBackground", PrimaryIndigoLight, DepthTokens.RecoveryInfoBackground),
            Triple("BadgeBlueText on RecoveryInfoBackground", BadgeBlueText, DepthTokens.RecoveryInfoBackground),
            Triple("TextPrimaryReport on ReportReadOnlyFill", TextPrimaryReport, ReportReadOnlyFill)
        )
        normalPairs.forEach { (label, fg, bg) ->
            val ratio = wcagContrastRatio(fg, bg)
            assertTrue("$label must meet WCAG AA >= 4.5:1 (was $ratio)", ratio >= aaNormal)
        }

        // Disabled/placeholder muted ink on #EEF1F7 meets WCAG >= 3.0:1 graphical/large contrast
        val readOnlyMutedRatio = wcagContrastRatio(DepthTokens.ReportReadOnlyTextColor, ReportReadOnlyFill)
        assertTrue(
            "ReportReadOnlyTextColor on ReportReadOnlyFill must be legible >= 3.0:1 (was $readOnlyMutedRatio)",
            readOnlyMutedRatio >= 3.0
        )
    }

    @Test
    fun stage26G_zeroColorLiteralsOutsideThemeAndAllGapsWiredInScreens() {
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

        // Gap 1 & 2: LojiaRegisterScreen
        val registerSrc = File(appDir, "src/main/java/com/lojia/shiftreport/auth/LojiaRegisterScreen.kt").readText()
        assertTrue(
            "LojiaRegisterScreen must use Stage 26G dial-code button and note box tokens",
            registerSrc.contains("DialButtonWidth") &&
                registerSrc.contains("DialButtonHeight") &&
                registerSrc.contains("DialButtonLipHeight") &&
                registerSrc.contains("DialButtonPaddingStart") &&
                registerSrc.contains("DialButtonPaddingEnd") &&
                registerSrc.contains("DialButtonTextSize") &&
                registerSrc.contains("DialButtonTextColor") &&
                registerSrc.contains("depthNoteBox") &&
                registerSrc.contains("RegisterNotePadding")
        )

        // Gap 3: LojiaPasswordRecoveryDialog in BiometricManager.kt
        val biometricSrc = File(appDir, "src/main/java/com/lojia/shiftreport/auth/BiometricManager.kt").readText()
        assertTrue(
            "LojiaPasswordRecoveryDialog must use depthInfoBlock and RecoveryInfoPadding",
            biometricSrc.contains("depthInfoBlock") && biometricSrc.contains("RecoveryInfoPadding")
        )

        // Gap 4: NumberField & EmployerAdvanceSection read-only wells
        val shiftCompSrc = File(appDir, "src/main/java/com/lojia/shiftreport/report/ShiftReportComponents.kt").readText()
        assertTrue(
            "NumberField disabled state must use ReportReadOnlyBackground, BorderColor, TextColor, ShadowOffsetY, ShadowBlur",
            shiftCompSrc.contains("ReportReadOnlyBackground") &&
                shiftCompSrc.contains("ReportReadOnlyBorderColor") &&
                shiftCompSrc.contains("ReportReadOnlyTextColor") &&
                shiftCompSrc.contains("ReportReadOnlyShadowOffsetY") &&
                shiftCompSrc.contains("ReportReadOnlyShadowBlur") &&
                shiftCompSrc.contains("REPORT_READ_ONLY_INSET_ALPHA")
        )
        val reportEntrySrc = File(appDir, "src/main/java/com/lojia/shiftreport/report/ReportEntryTab.kt").readText()
        assertTrue(
            "EmployerAdvanceSection disabled well must use ReportReadOnly tokens",
            reportEntrySrc.contains("ReportReadOnlyBackground") &&
                reportEntrySrc.contains("ReportReadOnlyBorderColor") &&
                reportEntrySrc.contains("ReportReadOnlyTextColor")
        )
        assertFalse(
            "ReportEntryTab reset confirmation must use LojiaConfirmDialog instead of raw AlertDialog(",
            reportEntrySrc.contains("AlertDialog(")
        )

        // Gap 5: WalkoutLedgerTab & ShiftReportArchivesTab Paid Out parity
        val secondaryLedgerSrc = File(appDir, "src/main/java/com/lojia/shiftreport/report/SecondaryLedgerTabs.kt").readText()
        val walkoutBody = secondaryLedgerSrc.substring(secondaryLedgerSrc.indexOf("fun WalkoutLedgerTab"))
        assertTrue(
            "WalkoutLedgerTab must use reportPageBackground, CardCornerRadius, and Paid Out shared helpers",
            walkoutBody.contains("reportPageBackground") &&
                walkoutBody.contains("CardCornerRadius") &&
                walkoutBody.contains("LedgerRoseIconTile") &&
                walkoutBody.contains("LedgerKpiSummaryCard") &&
                walkoutBody.contains("LedgerSectionHeaderRow") &&
                walkoutBody.contains("LedgerEmptyStateCard")
        )

        val archivesSrc = File(appDir, "src/main/java/com/lojia/shiftreport/report/ShiftReportArchivesTab.kt").readText()
        assertTrue(
            "ShiftReportArchivesTab must use Paid Out parity styling",
            archivesSrc.contains("reportPageBackground") &&
                archivesSrc.contains("CardCornerRadius") &&
                archivesSrc.contains("depthRaisedIconTile") &&
                archivesSrc.contains("listDividerColor") &&
                archivesSrc.contains("RadiusReportInput")
        )

        // Gap 6: Remaining dialogs aligned with LojiaDialog / BottomSheetCornerRadius
        val ledgerTabsSrc = File(appDir, "src/main/java/com/lojia/shiftreport/report/ShiftReportLedgerTabs.kt").readText()
        assertFalse(
            "ShiftReportLedgerTabs must use LojiaDialog instead of raw AlertDialog(",
            ledgerTabsSrc.contains("AlertDialog(")
        )
        assertTrue(
            "AdvanceStatementPeriodDialog must use LojiaDialog",
            ledgerTabsSrc.contains("LojiaDialog(")
        )

        val printerDialogSrc = File(appDir, "src/main/java/com/lojia/shiftreport/printer/PrinterSetupDialog.kt").readText()
        assertFalse(
            "PrinterSetupDialog must use LojiaDialog instead of raw Dialog(",
            Regex("""\bDialog\(""").containsMatchIn(printerDialogSrc)
        )
        assertTrue(
            "PrinterSetupDialog must wrap content in LojiaDialog",
            printerDialogSrc.contains("LojiaDialog(")
        )

        val addEntrySrc = File(appDir, "src/main/java/com/lojia/shiftreport/report/AddEntryDialog.kt").readText()
        assertFalse(
            "AddEntryDialog must not hardcode RoundedCornerShape(10.dp)",
            addEntrySrc.contains("RoundedCornerShape(10.dp)")
        )

        val permissionSrc = File(appDir, "src/main/java/com/lojia/shiftreport/permission/PermissionManager.kt").readText()
        assertFalse(
            "PermissionRationaleDialog must use LojiaDialog instead of raw AlertDialog(",
            permissionSrc.contains("AlertDialog(")
        )
        assertTrue(
            "PermissionRationaleDialog must use LojiaDialog",
            permissionSrc.contains("LojiaDialog(")
        )

        listOf(
            "src/main/java/com/lojia/shiftreport/settings/RegionalPreferencesSection.kt",
            "src/main/java/com/lojia/shiftreport/settings/AddEditAddressScreen.kt",
            "src/main/java/com/lojia/shiftreport/settings/ProfileImageCropper/ProfileImageCropperDialog.kt",
            "src/main/java/com/lojia/shiftreport/scanner/DocumentEditorScreen.kt"
        ).forEach { relPath ->
            val src = File(appDir, relPath).readText()
            assertTrue(
                "$relPath ModalBottomSheet must use Dimens.BottomSheetCornerRadius",
                src.contains("BottomSheetCornerRadius")
            )
        }
    }
}
