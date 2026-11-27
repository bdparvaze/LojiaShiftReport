package com.lojia.shiftreport

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojia.shiftreport.ui.theme.DepthTypeTokens
import com.lojia.shiftreport.ui.theme.Dimens
import com.lojia.shiftreport.ui.theme.typographyFor
import com.lojia.shiftreport.util.OfflineBackupManager
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class Stage34BNoRawUnitsUnitTest {

    private fun resolveAppModuleDir(): File {
        val candidates = listOf(
            File("."),
            File("app"),
            File(".."),
            File("../app")
        )
        return candidates.firstOrNull { dir ->
            File(dir, "src/main/java/com/lojia/shiftreport/ui/theme/Dimens.kt").isFile
        } ?: error("Unable to locate app module directory from ${File(".").absolutePath}")
    }

    private fun extractStringKeys(xmlFile: File): Set<String> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlFile)
        val nodes = doc.getElementsByTagName("string")
        val keys = linkedSetOf<String>()
        for (i in 0 until nodes.length) {
            val name = nodes.item(i).attributes?.getNamedItem("name")?.nodeValue
            if (!name.isNullOrBlank()) keys.add(name)
        }
        return keys
    }

    private fun allMainKotlinFilesOutsideTheme(appDir: File): List<File> {
        val javaRoot = File(appDir, "src/main/java/com/lojia/shiftreport")
        return javaRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { !it.invariantSeparatorsPath.contains("/ui/theme/") }
            .sortedBy { it.invariantSeparatorsPath }
            .toList()
    }

    @Test
    fun allMainFilesOutsideTheme_haveZeroRawDpOrSpLiterals() {
        val appDir = resolveAppModuleDir()
        val rawUnitRegex = Regex("""\b\d+(?:\.\d+)?\.(?:dp|sp)\b""")
        val violations = mutableListOf<String>()

        allMainKotlinFilesOutsideTheme(appDir).forEach { file ->
            file.readLines().forEachIndexed { idx, line ->
                val matches = rawUnitRegex.findAll(line).map { it.value }.toList()
                if (matches.isNotEmpty()) {
                    violations.add("${file.relativeTo(appDir).path}:${idx + 1} -> $matches")
                }
            }
        }

        assertEquals(
            "Expected zero raw .dp/.sp literals outside ui/theme/, found:\n${violations.joinToString("\n")}",
            emptyList<String>(),
            violations
        )
    }

    @Test
    fun allMainFilesOutsideTheme_haveZeroRawFontSizeOrDpSpImports() {
        val appDir = resolveAppModuleDir()
        val rawFontSizeRegex = Regex("""fontSize\s*=\s*\d""")
        val unitImportRegex = Regex("""^\s*import\s+androidx\.compose\.ui\.unit\.(?:dp|sp)\b""")

        val fontSizeViolations = mutableListOf<String>()
        val importViolations = mutableListOf<String>()

        allMainKotlinFilesOutsideTheme(appDir).forEach { file ->
            val relPath = file.relativeTo(appDir).path
            file.readLines().forEachIndexed { idx, line ->
                if (rawFontSizeRegex.containsMatchIn(line)) {
                    fontSizeViolations.add("$relPath:${idx + 1}")
                }
                if (unitImportRegex.containsMatchIn(line)) {
                    importViolations.add("$relPath:${idx + 1}")
                }
            }
        }

        assertEquals(emptyList<String>(), fontSizeViolations)
        assertEquals(emptyList<String>(), importViolations)
    }

    @Test
    fun allMainFilesOutsideTheme_haveZeroRawHexColors() {
        val appDir = resolveAppModuleDir()
        val rawHexColorRegex = Regex("""Color\(0x[0-9A-Fa-f]+""")
        val violations = allMainKotlinFilesOutsideTheme(appDir)
            .filter { rawHexColorRegex.containsMatchIn(it.readText()) }
            .map { it.relativeTo(appDir).path }

        assertEquals(emptyList<String>(), violations)
    }

    @Test
    fun compactTypeScaleAndAuthParityTokens_remainExact() {
        assertEquals(48.dp, Dimens.MinTouchTarget)

        val en = typographyFor("en")
        val allStyles = listOf(
            en.displayLarge, en.displayMedium, en.displaySmall,
            en.headlineLarge, en.headlineMedium, en.headlineSmall,
            en.titleLarge, en.titleMedium, en.titleSmall,
            en.bodyLarge, en.bodyMedium, en.bodySmall,
            en.labelLarge, en.labelMedium, en.labelSmall
        )
        assertTrue("All compact typography styles must stay >= 11.sp", allStyles.all { it.fontSize >= 11.sp })

        // Auth, Register, Password Recovery, and Quick PIN exact visual parity tokens
        assertEquals(10.dp, Dimens.AuthFieldGap)
        assertEquals(14.dp, Dimens.AuthSectionGap)
        assertEquals(28.dp, Dimens.QuickPinHeaderBottomGap)
        assertEquals(80.dp, Dimens.AuthSuccessBadgeSize)
        assertEquals(40.dp, Dimens.AuthSuccessIconSize)
        assertEquals(13.sp, DepthTypeTokens.AuthFieldLabelSize)
        assertEquals(14.sp, DepthTypeTokens.AuthFieldInputSize)
        assertEquals(12.sp, DepthTypeTokens.AuthErrorBannerTextSize)
        assertEquals(11.5.sp, DepthTypeTokens.RegisterTermsTextSize)
        assertEquals(22.sp, DepthTypeTokens.RegisterSuccessTitleSize)
        assertEquals(13.sp, DepthTypeTokens.RecoveryBodySize)
    }

    @Test
    fun invariants_roomSchemaBackupVersionAndStringLocalizationPreserved() {
        val appDir = resolveAppModuleDir()
        val dbSrc = File(appDir, "src/main/java/com/lojia/shiftreport/data/AppDatabase.kt").readText()
        assertTrue("Database version must remain 20", dbSrc.contains("version = 20"))
        assertEquals(2, OfflineBackupManager.BACKUP_FORMAT_VERSION)

        val schemaJson = JSONObject(File(appDir, "schemas/com.lojia.shiftreport.data.AppDatabase/20.json").readText())
        val dbJson = schemaJson.getJSONObject("database")
        assertEquals(20, dbJson.getInt("version"))
        assertEquals("17f0f4d4037600babcf1824104954458", dbJson.getString("identityHash"))

        val enKeys = extractStringKeys(File(appDir, "src/main/res/values/strings.xml"))
        val bnKeys = extractStringKeys(File(appDir, "src/main/res/values-bn/strings.xml"))
        val arKeys = extractStringKeys(File(appDir, "src/main/res/values-ar/strings.xml"))
        assertEquals(enKeys, bnKeys)
        assertEquals(enKeys, arKeys)
    }
}
