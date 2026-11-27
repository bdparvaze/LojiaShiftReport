package com.lojia.shiftreport

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojia.shiftreport.ui.theme.Dimens
import com.lojia.shiftreport.ui.theme.LineHeightFactorBnAr
import com.lojia.shiftreport.ui.theme.asMoneyTabular
import com.lojia.shiftreport.ui.theme.kpiValueTextStyle
import com.lojia.shiftreport.ui.theme.typographyFor
import com.lojia.shiftreport.util.OfflineBackupManager
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class Stage34ACompactScaleUnitTest {

    private fun resolveAppModuleDir(): File {
        val candidates = listOf(
            File("."),
            File("app"),
            File(".."),
            File("../app")
        )
        return candidates.firstOrNull { dir ->
            File(dir, "src/main/java/com/lojia/shiftreport/ui/theme/Type.kt").isFile
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

    @Test
    fun systemFont_poppinsRemovedAndAllLocalesUseDefault() {
        val appDir = resolveAppModuleDir()
        val fontFile = File(appDir, "src/main/res/font/poppins.ttf")
        assertFalse("res/font/poppins.ttf must be deleted", fontFile.exists())

        val typeSrc = File(appDir, "src/main/java/com/lojia/shiftreport/ui/theme/Type.kt").readText()
        assertFalse("Type.kt must not define PoppinsFontFamily", typeSrc.contains("PoppinsFontFamily"))
        assertFalse("Type.kt must not reference R.font", typeSrc.contains("R.font"))

        val mainSrcRoot = File(appDir, "src/main")
        val poppinsRefs = mainSrcRoot.walkTopDown()
            .filter { it.isFile && (it.extension == "kt" || it.extension == "xml") }
            .filter {
                val txt = it.readText()
                txt.contains("PoppinsFontFamily") || txt.contains("R.font")
            }
            .map { it.relativeTo(appDir).path }
            .toList()
        assertEquals("Zero PoppinsFontFamily or R.font references allowed in src/main", emptyList<String>(), poppinsRefs)

        listOf("en", "bn", "ar").forEach { tag ->
            val typo = typographyFor(tag)
            assertEquals(FontFamily.Default, typo.headlineSmall.fontFamily)
            assertEquals(FontFamily.Default, typo.titleMedium.fontFamily)
            assertEquals(FontFamily.Default, typo.titleSmall.fontFamily)
            assertEquals(FontFamily.Default, typo.bodyLarge.fontFamily)
            assertEquals(FontFamily.Default, typo.bodyMedium.fontFamily)
            assertEquals(FontFamily.Default, typo.bodySmall.fontFamily)
            assertEquals(FontFamily.Default, typo.labelLarge.fontFamily)
            assertEquals(FontFamily.Default, typo.labelMedium.fontFamily)
        }
    }

    @Test
    fun centralTypeScale_exactSizesWeightsTabularAndBnArLineHeight() {
        assertEquals(1.15f, LineHeightFactorBnAr, 1e-6f)

        val en = typographyFor("en")
        assertEquals(16.sp, en.headlineSmall.fontSize)
        assertEquals(22.sp, en.headlineSmall.lineHeight)
        assertEquals(FontWeight.SemiBold, en.headlineSmall.fontWeight)

        assertEquals(13.sp, en.titleMedium.fontSize)
        assertEquals(18.sp, en.titleMedium.lineHeight)
        assertEquals(FontWeight.Medium, en.titleMedium.fontWeight)

        assertEquals(12.sp, en.titleSmall.fontSize)
        assertEquals(16.sp, en.titleSmall.lineHeight)
        assertEquals(FontWeight.SemiBold, en.titleSmall.fontWeight)

        assertEquals(14.sp, en.bodyLarge.fontSize)
        assertEquals(20.sp, en.bodyLarge.lineHeight)
        assertEquals(FontWeight.Medium, en.bodyLarge.fontWeight)

        assertEquals(12.sp, en.bodyMedium.fontSize)
        assertEquals(17.sp, en.bodyMedium.lineHeight)
        assertEquals(FontWeight.Normal, en.bodyMedium.fontWeight)

        assertEquals(11.sp, en.bodySmall.fontSize)
        assertEquals(15.sp, en.bodySmall.lineHeight)
        assertEquals(FontWeight.Normal, en.bodySmall.fontWeight)

        assertEquals(13.sp, en.labelLarge.fontSize)
        assertEquals(18.sp, en.labelLarge.lineHeight)
        assertEquals(FontWeight.Medium, en.labelLarge.fontWeight)

        assertEquals(11.sp, en.labelMedium.fontSize)
        assertEquals(15.sp, en.labelMedium.lineHeight)
        assertEquals(FontWeight.Medium, en.labelMedium.fontWeight)

        val allEnStyles = listOf(
            en.displayLarge, en.displayMedium, en.displaySmall,
            en.headlineLarge, en.headlineMedium, en.headlineSmall,
            en.titleLarge, en.titleMedium, en.titleSmall,
            en.bodyLarge, en.bodyMedium, en.bodySmall,
            en.labelLarge, en.labelMedium, en.labelSmall
        )
        assertTrue("All typography styles must be >= 11.sp", allEnStyles.all { it.fontSize >= 11.sp })

        val kpi = kpiValueTextStyle("en")
        assertEquals(20.sp, kpi.fontSize)
        assertEquals(26.sp, kpi.lineHeight)
        assertEquals(FontWeight.Bold, kpi.fontWeight)
        assertEquals("tnum", kpi.fontFeatureSettings)

        assertEquals("tnum", en.bodyLarge.asMoneyTabular().fontFeatureSettings)

        listOf("bn", "ar").forEach { tag ->
            val ext = typographyFor(tag)
            assertEquals(en.headlineSmall.lineHeight.value * LineHeightFactorBnAr, ext.headlineSmall.lineHeight.value, 1e-4f)
            assertEquals(en.titleMedium.lineHeight.value * LineHeightFactorBnAr, ext.titleMedium.lineHeight.value, 1e-4f)
            assertEquals(en.titleSmall.lineHeight.value * LineHeightFactorBnAr, ext.titleSmall.lineHeight.value, 1e-4f)
            assertEquals(en.bodyLarge.lineHeight.value * LineHeightFactorBnAr, ext.bodyLarge.lineHeight.value, 1e-4f)
            assertEquals(en.bodyMedium.lineHeight.value * LineHeightFactorBnAr, ext.bodyMedium.lineHeight.value, 1e-4f)
            assertEquals(en.bodySmall.lineHeight.value * LineHeightFactorBnAr, ext.bodySmall.lineHeight.value, 1e-4f)
            assertEquals(en.labelLarge.lineHeight.value * LineHeightFactorBnAr, ext.labelLarge.lineHeight.value, 1e-4f)
            assertEquals(en.labelMedium.lineHeight.value * LineHeightFactorBnAr, ext.labelMedium.lineHeight.value, 1e-4f)
            assertEquals(26f * LineHeightFactorBnAr, kpiValueTextStyle(tag).lineHeight.value, 1e-4f)
        }
    }

    @Test
    fun centralDimensTokens_matchCompactBusinessScale() {
        assertEquals(48.dp, Dimens.MinTouchTarget)
        assertEquals(56.dp, Dimens.TopBarHeight)
        assertEquals(44.dp, Dimens.TabBarMinHeight)
        assertEquals(10.dp, Dimens.CardCornerRadius)
        assertEquals(14.dp, Dimens.DialogCornerRadius)
        assertEquals(14.dp, Dimens.BottomSheetCornerRadius)
        assertEquals(40.dp, Dimens.TextFieldHeightCompact)
        assertEquals(44.dp, Dimens.TextFieldHeightStandard)
        assertEquals(44.dp, Dimens.TextFieldHeightReport)
        assertEquals(36.dp, Dimens.ButtonHeightCompact)
        assertEquals(40.dp, Dimens.ButtonHeightStandard)
        assertEquals(44.dp, Dimens.ButtonHeightPrimary)
        assertEquals(48.dp, Dimens.ListRowMinHeight)
        assertEquals(32.dp, Dimens.IconTile)
        assertEquals(18.dp, Dimens.IconMd)
        assertEquals(64.dp, Dimens.ProfileAvatarSize)
        assertEquals(24.dp, Dimens.ProfileAvatarBadgeSize)
        assertEquals(0.5.dp, Dimens.RowDividerWidth)
        assertEquals(12.dp, Dimens.CardPadding)
    }

    @Test
    fun migratedProfileSettingsAndSharedComponents_haveZeroRawDpOrSpLiterals() {
        val appDir = resolveAppModuleDir()
        val migratedFiles = listOf(
            "src/main/java/com/lojia/shiftreport/settings/ProfileScreen.kt",
            "src/main/java/com/lojia/shiftreport/settings/ProfileComponents.kt",
            "src/main/java/com/lojia/shiftreport/settings/SettingsScreen.kt",
            "src/main/java/com/lojia/shiftreport/settings/LanguageSettingsComponent.kt",
            "src/main/java/com/lojia/shiftreport/settings/SettingsReportSection.kt",
            "src/main/java/com/lojia/shiftreport/settings/RegionalPreferencesSection.kt",
            "src/main/java/com/lojia/shiftreport/settings/SettingsCommonDialogs.kt",
            "src/main/java/com/lojia/shiftreport/ui/common/LojiaInfoRow.kt",
            "src/main/java/com/lojia/shiftreport/ui/common/ProfileAvatar.kt",
            "src/main/java/com/lojia/shiftreport/ui/common/LojiaComponents.kt"
        )
        val rawUnitRegex = Regex("""\b[0-9]+(\.[0-9]+)?\.(dp|sp)\b""")
        val unitImportRegex = Regex("""import\s+androidx\.compose\.ui\.unit\.(dp|sp)\b""")

        migratedFiles.forEach { relPath ->
            val src = File(appDir, relPath).readText()
            val literalHits = rawUnitRegex.findAll(src).map { it.value }.toList()
            assertEquals("Expected zero raw .dp/.sp literals in $relPath, found: $literalHits", emptyList<String>(), literalHits)
            assertFalse("Expected no dp/sp import in $relPath", unitImportRegex.containsMatchIn(src))
        }
    }

    @Test
    fun profileAvatarAndInfoRow_sharedComponentsContractVerified() {
        val appDir = resolveAppModuleDir()
        val avatarSrc = File(appDir, "src/main/java/com/lojia/shiftreport/ui/common/ProfileAvatar.kt").readText()
        assertTrue(avatarSrc.contains("Dimens.ProfileAvatarSize"))
        assertTrue(avatarSrc.contains("Dimens.ProfileAvatarBadgeSize"))
        assertTrue(avatarSrc.contains("Icons.Default.Edit"))
        assertTrue(avatarSrc.contains("Icons.Default.Add"))
        assertTrue(avatarSrc.contains("R.string.profile_add_picture_cd"))
        assertTrue(avatarSrc.contains("R.string.profile_edit_picture_cd"))
        assertTrue(avatarSrc.contains("minimumInteractiveComponentSize()"))

        val infoRowSrc = File(appDir, "src/main/java/com/lojia/shiftreport/ui/common/LojiaInfoRow.kt").readText()
        assertTrue(infoRowSrc.contains("Dimens.ListRowMinHeight"))
        assertTrue(infoRowSrc.contains("Dimens.IconTile"))
        assertTrue(infoRowSrc.contains("KeyboardArrowRight"))
        assertTrue(infoRowSrc.contains("minimumInteractiveComponentSize()"))

        val profileSrc = File(appDir, "src/main/java/com/lojia/shiftreport/settings/ProfileScreen.kt").readText()
        assertTrue(profileSrc.contains("ProfileAvatar("))
        assertTrue(profileSrc.contains("LojiaCard"))
        assertTrue(profileSrc.contains("LojiaInfoRow("))
    }

    @Test
    fun invariants_colorsRoomSchemaBackupAndStringParity() {
        val appDir = resolveAppModuleDir()
        val javaRoot = File(appDir, "src/main/java")
        val rawHexColorRegex = Regex("""Color\(0x[0-9A-Fa-f]+\)""")
        val colorViolations = javaRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { !it.invariantSeparatorsPath.contains("/ui/theme/") }
            .filter { rawHexColorRegex.containsMatchIn(it.readText()) }
            .map { it.relativeTo(appDir).path }
            .toList()
        assertEquals(emptyList<String>(), colorViolations)

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
        assertTrue(enKeys.contains("profile_add_picture_cd"))
    }
}
