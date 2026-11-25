package com.lojia.shiftreport

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lojia.shiftreport.ui.theme.AuthPrimaryDark
import com.lojia.shiftreport.ui.theme.AuthPrimaryDeep
import com.lojia.shiftreport.ui.theme.DepthMotionTokens
import com.lojia.shiftreport.ui.theme.DepthTokens
import com.lojia.shiftreport.ui.theme.Dimens
import com.lojia.shiftreport.ui.theme.GoogleDriveBlue
import com.lojia.shiftreport.ui.theme.OnSurfaceLight
import com.lojia.shiftreport.ui.theme.OnSurfaceVariantLight
import com.lojia.shiftreport.ui.theme.OneDriveBlue
import com.lojia.shiftreport.ui.theme.PositiveBannerSurface
import com.lojia.shiftreport.ui.theme.PresetLogoCafeEnd
import com.lojia.shiftreport.ui.theme.PresetLogoCafeStart
import com.lojia.shiftreport.ui.theme.PresetLogoFashionEnd
import com.lojia.shiftreport.ui.theme.PresetLogoPharmacyEnd
import com.lojia.shiftreport.ui.theme.PresetLogoSuperstoreEnd
import com.lojia.shiftreport.ui.theme.PresetLogoSuperstoreStart
import com.lojia.shiftreport.ui.theme.PresetLogoTechEnd
import com.lojia.shiftreport.ui.theme.PrimaryBlueDark
import com.lojia.shiftreport.ui.theme.PrimaryContainerLight
import com.lojia.shiftreport.ui.theme.PureWhite
import com.lojia.shiftreport.ui.theme.SuccessContainer
import com.lojia.shiftreport.ui.theme.SuccessTextDark
import com.lojia.shiftreport.ui.theme.WarningContainer
import com.lojia.shiftreport.ui.theme.WarningText
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

class Stage26CRestyleUnitTest {

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
            File(dir, "src/test/resources/stage26c_ui_baseline.txt").isFile
        } ?: error(
            "Unable to locate app module directory from working directory: ${File(".").absolutePath}"
        )
    }

    @Test
    fun noHardcodedColorLiterals_anywhereOutsideUiTheme() {
        val appDir = resolveAppModuleDir()
        val javaRoot = File(appDir, "src/main/java")
        assertTrue("Expected src/main/java to exist: ${javaRoot.path}", javaRoot.isDirectory)

        val rawHexColorRegex = Regex("""Color\(0x[0-9A-Fa-f]+\)""")
        val violations = mutableListOf<String>()

        javaRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { file ->
                val normalized = file.path.replace(File.separatorChar, '/')
                if (!normalized.contains("/ui/theme/")) {
                    val matches = rawHexColorRegex.findAll(file.readText())
                        .map { it.value }
                        .toList()
                    if (matches.isNotEmpty()) {
                        violations.add("${file.relativeTo(appDir).path}: $matches")
                    }
                }
            }

        assertEquals(
            "Expected zero hardcoded Color(0x...) literals outside ui/theme/, found: $violations",
            emptyList<String>(),
            violations
        )
    }

    @Test
    fun baselineUiBaselineContract_exactMatch() {
        val appDir = resolveAppModuleDir()
        val baselineFile = File(appDir, "src/test/resources/stage26c_ui_baseline.txt")
        assertTrue("Expected baseline file to exist", baselineFile.isFile)

        val tokenRegex = Regex("""R\.string\.([A-Za-z0-9_]+)|testTag\(\s*"([^"]*)"\s*\)""")
        val lines = baselineFile.readLines().filter { it.isNotBlank() }
        assertTrue("Expected 33 baseline entries, got ${lines.size}", lines.size >= 33)

        val mismatches = mutableListOf<String>()
        lines.forEach { line ->
            val colonIdx = line.indexOf(':')
            assertTrue("Malformed baseline line: $line", colonIdx > 0)

            val relPath = line.substring(0, colonIdx).trim()
            val expectedRaw = line.substring(colonIdx + 1).trim()
            val rawExpected = if (expectedRaw.isEmpty()) {
                emptyList()
            } else {
                expectedRaw.split(',').map { it.trim() }
            }
            val expected = if (relPath == "settings/ProfileScreen.kt") {
                val oldEditPhoneSlice = listOf(
                    "R.string.profile_edit_phone_title",
                    "R.string.profile_phone_required_label",
                    "R.string.profile_phone_digits_error",
                    "R.string.cancel",
                    "R.string.save"
                )
                val newEditPhoneSlice = listOf(
                    "R.string.profile_edit_phone_title",
                    "R.string.auth_select_country_code",
                    "R.string.profile_phone_mobile_hint",
                    "testTag(\"btn_profile_phone_country_code\")",
                    "testTag(\"input_profile_phone_local\")",
                    "R.string.profile_phone_invalid",
                    "R.string.cancel",
                    "R.string.save"
                )
                val idx = java.util.Collections.indexOfSubList(rawExpected, oldEditPhoneSlice)
                assertTrue("Expected Stage 26C EditPhoneDialog slice in ProfileScreen baseline", idx >= 0)
                rawExpected.subList(0, idx) + newEditPhoneSlice + rawExpected.subList(idx + oldEditPhoneSlice.size, rawExpected.size)
            } else {
                rawExpected
            }

            val sourceFile = File(appDir, "src/main/java/com/lojia/shiftreport/$relPath")
            assertTrue("Expected baseline target file to exist: ${sourceFile.path}", sourceFile.isFile)

            val actual = tokenRegex.findAll(sourceFile.readText()).map { match ->
                val strRes = match.groupValues[1]
                if (strRes.isNotEmpty()) {
                    "R.string.$strRes"
                } else {
                    "testTag(\"${match.groupValues[2]}\")"
                }
            }.toList()

            if (expected != actual) {
                mismatches.add("$relPath: expected=${expected.size} actual=${actual.size}\n  expected=$expected\n  actual=$actual")
            }
        }
        assertEquals(
            "Stage 26C baseline contract mismatches:\n${mismatches.joinToString("\n")}",
            emptyList<String>(),
            mismatches
        )
    }

    @Test
    fun wcagContrast_stage26CTokensMeetAA() {
        val aaNormalText = 4.5

        val pairs = listOf(
            Triple("SuccessTextDark on SuccessContainer", SuccessTextDark, SuccessContainer),
            Triple("SuccessTextDark on PositiveBannerSurface", SuccessTextDark, PositiveBannerSurface),
            Triple("WarningText on WarningContainer", WarningText, WarningContainer),
            Triple("PrimaryBlueDark on PrimaryContainerLight", PrimaryBlueDark, PrimaryContainerLight),
            Triple("PureWhite on AuthPrimaryDeep", PureWhite, AuthPrimaryDeep),
            Triple("PureWhite on AuthPrimaryDark", PureWhite, AuthPrimaryDark),
            Triple("PureWhite on GoogleDriveBlue", PureWhite, GoogleDriveBlue),
            Triple("PureWhite on OneDriveBlue", PureWhite, OneDriveBlue),
            Triple("PureWhite on PresetLogoSuperstoreStart", PureWhite, PresetLogoSuperstoreStart),
            Triple("PureWhite on PresetLogoSuperstoreEnd", PureWhite, PresetLogoSuperstoreEnd),
            Triple("PureWhite on PresetLogoCafeStart", PureWhite, PresetLogoCafeStart),
            Triple("PureWhite on PresetLogoCafeEnd", PureWhite, PresetLogoCafeEnd),
            Triple("PureWhite on PresetLogoFashionEnd", PureWhite, PresetLogoFashionEnd),
            Triple("PureWhite on PresetLogoPharmacyEnd", PureWhite, PresetLogoPharmacyEnd),
            Triple("PureWhite on PresetLogoTechEnd", PureWhite, PresetLogoTechEnd),
            Triple("OnSurfaceLight on CardGradientEnd", OnSurfaceLight, DepthTokens.CardGradientEnd),
            Triple("OnSurfaceVariantLight on CardGradientEnd", OnSurfaceVariantLight, DepthTokens.CardGradientEnd),
            Triple("OnSurfaceLight on InsetFieldBackground", OnSurfaceLight, DepthTokens.InsetFieldBackground),
            Triple("OnSurfaceVariantLight on InsetFieldBackground", OnSurfaceVariantLight, DepthTokens.InsetFieldBackground),
            Triple("OnSurfaceLight on CardAccentGradientEnd", OnSurfaceLight, DepthTokens.CardAccentGradientEnd),
            Triple("OnSurfaceVariantLight on CardAccentGradientEnd", OnSurfaceVariantLight, DepthTokens.CardAccentGradientEnd)
        )

        pairs.forEach { (label, fg, bg) ->
            val ratio = wcagContrastRatio(fg, bg)
            assertTrue(
                "$label must meet WCAG AA >= 4.5:1 (was $ratio)",
                ratio >= aaNormalText
            )
        }
    }

    @Test
    fun accessibilityMotionAndInvariants_verified() {
        val appDir = resolveAppModuleDir()

        assertEquals(48.dp, Dimens.MinTouchTarget)
        assertFalse(shouldAnimate(0f))
        assertTrue(shouldAnimate(1f))
        assertEquals(0, cardRiseDelayMillis(0))
        assertEquals(DepthMotionTokens.CARD_RISE_MAX_DELAY_MS, cardRiseDelayMillis(20))

        val quickPinSource = File(
            appDir,
            "src/main/java/com/lojia/shiftreport/auth/QuickPinScreen.kt"
        ).readText()
        assertTrue(
            "QuickPinScreen number pad must stay LTR",
            quickPinSource.contains("LayoutDirection.Ltr")
        )

        val dbSource = File(
            appDir,
            "src/main/java/com/lojia/shiftreport/data/AppDatabase.kt"
        ).readText()
        assertTrue("Database schema version must stay 20", dbSource.contains("version = 20"))

        val backupSource = File(
            appDir,
            "src/main/java/com/lojia/shiftreport/util/OfflineBackupManager.kt"
        ).readText()
        assertTrue(
            "BACKUP_FORMAT_VERSION must stay 2",
            backupSource.contains("const val BACKUP_FORMAT_VERSION = 2")
        )

        val depthTokenSignals = listOf(
            "depthCard",
            "depthSemanticCard",
            "depthInset",
            "depthRaisedButton",
            "depthSecondaryButton",
            "LojiaDialog",
            "LojiaTextField",
            "LojiaGradientHeader",
            "cardRise"
        )
        val requiredRestyledFiles = listOf(
            "src/main/java/com/lojia/shiftreport/auth/AuthScreen.kt",
            "src/main/java/com/lojia/shiftreport/auth/LojiaRegisterScreen.kt",
            "src/main/java/com/lojia/shiftreport/auth/QuickPinScreen.kt",
            "src/main/java/com/lojia/shiftreport/auth/AdminAuthDialog.kt",
            "src/main/java/com/lojia/shiftreport/settings/CashierManagementSection.kt",
            "src/main/java/com/lojia/shiftreport/settings/SavedAddressesScreen.kt",
            "src/main/java/com/lojia/shiftreport/settings/AddEditAddressScreen.kt",
            "src/main/java/com/lojia/shiftreport/settings/ProfileScreen.kt",
            "src/main/java/com/lojia/shiftreport/settings/GoogleMapsLocationPickerModal.kt",
            "src/main/java/com/lojia/shiftreport/settings/SettingsBackupSubSection.kt",
            "src/main/java/com/lojia/shiftreport/printer/PrinterSetupScreen.kt",
            "src/main/java/com/lojia/shiftreport/scanner/DocumentScannerScreen.kt"
        )

        requiredRestyledFiles.forEach { relPath ->
            val text = File(appDir, relPath).readText()
            assertTrue(
                "Expected depth theme usage in $relPath",
                depthTokenSignals.any { signal -> text.contains(signal) }
            )
        }
    }
}
