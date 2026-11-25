package com.lojia.shiftreport

import androidx.compose.ui.unit.dp
import com.lojia.shiftreport.data.BusinessProfile
import com.lojia.shiftreport.ui.common.DialogSize
import com.lojia.shiftreport.ui.common.PinCellsState
import com.lojia.shiftreport.ui.common.calculateDialogWidth
import com.lojia.shiftreport.ui.common.calculatePinCellSize
import com.lojia.shiftreport.ui.theme.Dimens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class Stage33DialogSystemUnitTest {

    private fun resolveAppModuleDir(): File {
        val candidates = listOf(
            File("."),
            File("app"),
            File(".."),
            File("../app")
        )
        return candidates.firstOrNull { dir ->
            File(dir, "src/main/java/com/lojia/shiftreport/ui/common/LojiaDialog.kt").isFile
        } ?: error("Unable to locate app module directory from ${File(".").absolutePath}")
    }

    private fun extractStringKeys(xmlFile: File): Set<String> {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(xmlFile)
        val nodes = doc.getElementsByTagName("string")
        val keys = linkedSetOf<String>()
        for (i in 0 until nodes.length) {
            val name = nodes.item(i).attributes?.getNamedItem("name")?.nodeValue
            if (!name.isNullOrBlank()) keys.add(name)
        }
        return keys
    }

    @Test
    fun calculateDialogWidth_respectsMarginAndPresets() {
        // 280dp screen -> 280 - 24 = 256dp across all presets
        assertEquals(256.dp, calculateDialogWidth(280.dp, DialogSize.Small))
        assertEquals(256.dp, calculateDialogWidth(280.dp, DialogSize.Medium))
        assertEquals(256.dp, calculateDialogWidth(280.dp, DialogSize.Large))

        // 320dp screen -> 320 - 24 = 296dp across all presets
        assertEquals(296.dp, calculateDialogWidth(320.dp, DialogSize.Small))
        assertEquals(296.dp, calculateDialogWidth(320.dp, DialogSize.Medium))
        assertEquals(296.dp, calculateDialogWidth(320.dp, DialogSize.Large))

        // 360dp screen -> 336dp usable width caps Small at 320dp and Medium/Large at 336dp
        assertEquals(320.dp, calculateDialogWidth(360.dp, DialogSize.Small))
        assertEquals(336.dp, calculateDialogWidth(360.dp, DialogSize.Medium))
        assertEquals(336.dp, calculateDialogWidth(360.dp, DialogSize.Large))

        // 600dp screen -> exact preset caps (320 / 360 / 400)
        assertEquals(320.dp, calculateDialogWidth(600.dp, DialogSize.Small))
        assertEquals(360.dp, calculateDialogWidth(600.dp, DialogSize.Medium))
        assertEquals(400.dp, calculateDialogWidth(600.dp, DialogSize.Large))
    }

    @Test
    fun calculatePinCellSize_clampsWithinMinAndMaxAcrossWidths() {
        val counts = listOf(4, 6, 8)
        val widths = listOf(280.dp, 320.dp, 360.dp, 400.dp)

        counts.forEach { count ->
            widths.forEach { width ->
                val size = calculatePinCellSize(availableWidth = width, cellCount = count)
                assertTrue(
                    "Pin cell size $size for count=$count width=$width must be >= ${Dimens.PinCellMin}",
                    size >= Dimens.PinCellMin
                )
                assertTrue(
                    "Pin cell size $size for count=$count width=$width must be <= ${Dimens.PinCellMax}",
                    size <= Dimens.PinCellMax
                )
            }
        }

        // Exact check for 6 digits at 280dp usable dialog width (320dp Small dialog - 40dp padding)
        // (280 - 5 * 8) / 6 = 240 / 6 = 40dp == PinCellMax
        assertEquals(40.dp, calculatePinCellSize(availableWidth = 280.dp, cellCount = 6))
        // Tight width (256dp - 40dp padding = 216dp) -> (216 - 40) / 6 = 29.33dp -> clamped to 32dp
        assertEquals(32.dp, calculatePinCellSize(availableWidth = 216.dp, cellCount = 6))
    }

    @Test
    fun pinCells_contractAndOtpRemovalVerified() {
        val appDir = resolveAppModuleDir()
        val otpFile = File(appDir, "src/main/java/com/lojia/shiftreport/ui/common/OtpInputField.kt")
        assertFalse("OtpInputField.kt must be deleted", otpFile.exists())

        val pinCellsSrc = File(appDir, "src/main/java/com/lojia/shiftreport/ui/common/PinCells.kt").readText()
        assertTrue(pinCellsSrc.contains("CircleShape"))
        assertTrue(pinCellsSrc.contains("LayoutDirection.Ltr"))
        assertTrue(pinCellsSrc.contains("""testTag("six_digit_hidden_input")"""))
        assertTrue(pinCellsSrc.contains("""testTag("pin_box_${'$'}i")"""))
        assertEquals(setOf("Normal", "Error", "Success", "Locked"), PinCellsState.entries.map { it.name }.toSet())

        val sixDigitSrc = File(appDir, "src/main/java/com/lojia/shiftreport/auth/SixDigitPinInput.kt").readText()
        assertTrue("SixDigitPinInput must delegate to PinCells", sixDigitSrc.contains("PinCells("))
        assertFalse("SixDigitPinInput must not draw its own digit boxes", sixDigitSrc.contains("BasicTextField"))
    }

    @Test
    fun legacyDialogTokens_removedAcrossMainSource() {
        val appDir = resolveAppModuleDir()
        val mainJava = File(appDir, "src/main/java")
        val legacyTokens = listOf(
            "DialogMaxWidth",
            "DialogSmallMaxWidth",
            "DialogPadding",
            "DialogPaddingComfortable"
        )
        val hits = mutableListOf<String>()
        mainJava.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { file ->
                val text = file.readText()
                legacyTokens.forEach { token ->
                    if (Regex("""\b$token\b""").containsMatchIn(text)) {
                        hits.add("${file.relativeTo(appDir).path}:$token")
                    }
                }
            }
        assertEquals("Legacy dialog tokens must not remain in src/main: $hits", emptyList<String>(), hits)
    }

    @Test
    fun unifiedVerificationDialog_andSecurityPinModalDelegationVerified() {
        val appDir = resolveAppModuleDir()
        val adminAuthSrc = File(appDir, "src/main/java/com/lojia/shiftreport/auth/AdminAuthDialog.kt").readText()
        assertTrue(adminAuthSrc.contains("LojiaDialog("))
        assertTrue(adminAuthSrc.contains("size = DialogSize.Small"))
        assertTrue(adminAuthSrc.contains("PinCells("))
        assertFalse(adminAuthSrc.contains("OverlayScrim"))

        val componentsSrc = File(appDir, "src/main/java/com/lojia/shiftreport/ui/common/Components.kt").readText()
        val modalBody = componentsSrc.substringAfter("fun SecurityPinModal(").substringBefore("/**")
        assertTrue("SecurityPinModal must delegate to AdminAuthDialog", modalBody.contains("AdminAuthDialog("))
        assertFalse("SecurityKeypadItem must be removed", componentsSrc.contains("SecurityKeypadItem"))
    }

    @Test
    fun businessProfile_blankDefaultAndFallbacksVerified() {
        assertEquals("", BusinessProfile().businessName)

        val appDir = resolveAppModuleDir()
        val dbSrc = File(appDir, "src/main/java/com/lojia/shiftreport/data/AppDatabase.kt").readText()
        assertTrue("Database version must remain 20", dbSrc.contains("version = 20"))
        assertFalse("AppDatabase seed must not default businessName to My Store", dbSrc.contains("""businessName = "My Store""""))
        assertTrue("AppDatabase seed must initialize businessName as empty string", dbSrc.contains("""businessName = """""))

        val backupSrc = File(appDir, "src/main/java/com/lojia/shiftreport/util/OfflineBackupManager.kt").readText()
        assertTrue("BACKUP_FORMAT_VERSION must stay 2", backupSrc.contains("const val BACKUP_FORMAT_VERSION = 2"))
        assertTrue(
            "OfflineBackupManager restore fallback must stay untouched",
            backupSrc.contains("""optString("businessName", "My Store")""")
        )

        val pdfGenSrc = File(appDir, "src/main/java/com/lojia/shiftreport/util/PdfReportGenerator.kt").readText()
        assertFalse("PdfReportGenerator must not reference default_business_name", pdfGenSrc.contains("default_business_name"))
        assertTrue("PdfReportGenerator must use business_name_not_set", pdfGenSrc.contains("R.string.business_name_not_set"))

        val pdfPreviewSrc = File(appDir, "src/main/java/com/lojia/shiftreport/ui/common/PdfPreviewDialog.kt").readText()
        assertFalse("PdfPreviewDialog must not reference default_business_name", pdfPreviewSrc.contains("default_business_name"))
        assertTrue("PdfPreviewDialog must use business_name_not_set", pdfPreviewSrc.contains("R.string.business_name_not_set"))
    }

    @Test
    fun themeAndStringParity_enforcedForStage33() {
        val appDir = resolveAppModuleDir()
        val javaRoot = File(appDir, "src/main/java")
        val rawHexColorRegex = Regex("""Color\(0x[0-9A-Fa-f]+\)""")
        val colorViolations = mutableListOf<String>()
        javaRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { file ->
                val normalized = file.path.replace(File.separatorChar, '/')
                if (!normalized.contains("/ui/theme/")) {
                    val matches = rawHexColorRegex.findAll(file.readText()).map { it.value }.toList()
                    if (matches.isNotEmpty()) colorViolations.add("${file.relativeTo(appDir).path}: $matches")
                }
            }
        assertEquals(emptyList<String>(), colorViolations)

        val enKeys = extractStringKeys(File(appDir, "src/main/res/values/strings.xml"))
        val bnKeys = extractStringKeys(File(appDir, "src/main/res/values-bn/strings.xml"))
        val arKeys = extractStringKeys(File(appDir, "src/main/res/values-ar/strings.xml"))
        assertEquals(enKeys, bnKeys)
        assertEquals(enKeys, arKeys)
        assertTrue(enKeys.contains("pin_cells_cd_fmt"))
        assertTrue(enKeys.contains("business_name_not_set"))
    }
}
