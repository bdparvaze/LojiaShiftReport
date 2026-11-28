package com.lojia.shiftreport

import com.lojia.shiftreport.util.OfflineBackupManager
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class Stage35NavigationLanguageUnitTest {

    private fun resolveAppModuleDir(): File {
        val candidates = listOf(File("."), File("app"), File(".."), File("../app"))
        return candidates.firstOrNull { dir ->
            File(dir, "src/main/java/com/lojia/shiftreport/settings/SettingsBackupSubSection.kt").isFile
        } ?: error("Unable to locate app module directory from ${File(".").absolutePath}")
    }

    private fun parseStringValues(xmlFile: File): Map<String, String> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlFile)
        val nodes = doc.getElementsByTagName("string")
        val map = linkedMapOf<String, String>()
        for (i in 0 until nodes.length) {
            val node = nodes.item(i)
            val name = node.attributes?.getNamedItem("name")?.nodeValue
            if (!name.isNullOrBlank()) {
                map[name] = node.textContent.orEmpty()
            }
        }
        return map
    }

    @Test
    fun backupScreen_hasNoInnerHeaderOrBackArrow() {
        val failureMessage =
            "The Backup screen has no inner header or back arrow by owner decision; do not restore it"
        val appDir = resolveAppModuleDir()
        val backupSrc = File(
            appDir,
            "src/main/java/com/lojia/shiftreport/settings/SettingsBackupSubSection.kt"
        ).readText()

        assertFalse(failureMessage, backupSrc.contains("btn_backup_back"))
        assertFalse(failureMessage, backupSrc.contains("ArrowBack"))
        assertFalse(failureMessage, backupSrc.contains("BackHandler"))
        assertFalse(failureMessage, backupSrc.contains("R.string.backup_settings"))
        assertFalse(failureMessage, backupSrc.contains("R.string.backup_subtitle"))
        assertFalse(failureMessage, Regex("""\bR\.string\.back\b""").containsMatchIn(backupSrc))

        val baselineLine = File(appDir, "src/test/resources/stage26c_ui_baseline.txt")
            .readLines()
            .firstOrNull { it.startsWith("settings/SettingsBackupSubSection.kt:") }
            .orEmpty()
        assertTrue("Expected baseline entry for settings/SettingsBackupSubSection.kt", baselineLine.isNotEmpty())
        assertFalse(failureMessage, baselineLine.contains("testTag(\"btn_backup_back\")"))
        assertFalse(failureMessage, Regex("""\bR\.string\.back\b""").containsMatchIn(baselineLine))
        assertFalse(failureMessage, baselineLine.contains("R.string.backup_settings"))
        assertFalse(failureMessage, baselineLine.contains("R.string.backup_subtitle"))
    }

    @Test
    fun resolveBackTarget_enforcesOneBackRuleForAllEntries() {
        // (a) Drawer-opened Backup + Back, empty stack -> GoHome
        assertEquals(
            BackTarget.GoHome,
            resolveBackTarget(
                isDrawerOrMenuOpen = false,
                navState = AppNavState.ShiftReportState.SettingsDetail("backup"),
                settingsEntry = SettingsEntry.FromDrawer,
                selectedSettingsMenu = "backup",
                backStackIsEmpty = true
            )
        )

        // (b) Drawer-opened Backup with non-empty stack -> PopBackStack
        assertEquals(
            BackTarget.PopBackStack,
            resolveBackTarget(
                isDrawerOrMenuOpen = false,
                navState = AppNavState.ShiftReportState.SettingsDetail("backup"),
                settingsEntry = SettingsEntry.FromDrawer,
                selectedSettingsMenu = "backup",
                backStackIsEmpty = false
            )
        )

        // (c) Settings-root-opened Backup + Back -> GoToSettingsRoot
        assertEquals(
            BackTarget.GoToSettingsRoot,
            resolveBackTarget(
                isDrawerOrMenuOpen = false,
                navState = AppNavState.ShiftReportState.SettingsDetail("backup"),
                settingsEntry = SettingsEntry.FromSettingsRoot,
                selectedSettingsMenu = "backup",
                backStackIsEmpty = false
            )
        )

        // (d) Settings-root-opened any other sub-screen + Back -> GoToSettingsRoot
        listOf("profile", "cashiers", "employees", "security", "regional", "printer", "about", "support")
            .forEach { section ->
                assertEquals(
                    "Sub-screen '$section' opened from Settings root must return to Settings root",
                    BackTarget.GoToSettingsRoot,
                    resolveBackTarget(
                        isDrawerOrMenuOpen = false,
                        navState = AppNavState.ShiftReportState.SettingsDetail(section),
                        settingsEntry = SettingsEntry.FromSettingsRoot,
                        selectedSettingsMenu = section,
                        backStackIsEmpty = true
                    )
                )
            }

        // (e) Settings root itself + Back, empty stack -> GoHome (non-empty -> PopBackStack)
        assertEquals(
            BackTarget.GoHome,
            resolveBackTarget(
                isDrawerOrMenuOpen = false,
                navState = AppNavState.ShiftReportState.SettingsDetail("root"),
                settingsEntry = SettingsEntry.FromSettingsRoot,
                selectedSettingsMenu = "root",
                backStackIsEmpty = true
            )
        )
        assertEquals(
            BackTarget.PopBackStack,
            resolveBackTarget(
                isDrawerOrMenuOpen = false,
                navState = AppNavState.ShiftReportState.SettingsDetail("root"),
                settingsEntry = SettingsEntry.FromDrawer,
                selectedSettingsMenu = "root",
                backStackIsEmpty = false
            )
        )

        // (f) Drawer or top menu open -> CloseOverlays first
        assertEquals(
            BackTarget.CloseOverlays,
            resolveBackTarget(
                isDrawerOrMenuOpen = true,
                navState = AppNavState.ShiftReportState.SettingsDetail("backup"),
                settingsEntry = SettingsEntry.FromSettingsRoot,
                selectedSettingsMenu = "backup",
                backStackIsEmpty = false
            )
        )

        // (g) Home with empty stack -> NotHandled (exit app)
        assertEquals(
            BackTarget.NotHandled,
            resolveBackTarget(
                isDrawerOrMenuOpen = false,
                navState = AppNavState.ShiftReportState.Home,
                settingsEntry = SettingsEntry.FromDrawer,
                selectedSettingsMenu = "",
                backStackIsEmpty = true
            )
        )
    }

    @Test
    fun sourceScans_settingsSubScreensHaveNoLeftoverCallbacksOrBackHandlers() {
        val appDir = resolveAppModuleDir()
        val backupSrc = File(
            appDir,
            "src/main/java/com/lojia/shiftreport/settings/SettingsBackupSubSection.kt"
        ).readText()
        assertFalse("SettingsBackupSubSection.kt must not have onNavigateBack", backupSrc.contains("onNavigateBack"))
        assertFalse("SettingsBackupSubSection.kt must not have ArrowBack", backupSrc.contains("ArrowBack"))
        assertFalse("SettingsBackupSubSection.kt must not have BackHandler", backupSrc.contains("BackHandler"))

        val settingsReportSrc = File(
            appDir,
            "src/main/java/com/lojia/shiftreport/settings/SettingsReportSection.kt"
        ).readText()
        assertFalse(
            "SettingsReportSection.kt must not pass root-back callback to SettingsBackupSubSection",
            settingsReportSrc.contains("selectReportSettingsMenu(\"root\")")
        )

        val settingsDir = File(appDir, "src/main/java/com/lojia/shiftreport/settings")
        val allowedBackHandlerFiles = setOf(
            "SavedAddressesScreen.kt",
            "AddEditAddressScreen.kt",
            "RegionalPreferencesSection.kt"
        )
        settingsDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { file ->
                val text = file.readText()
                assertFalse(
                    "${file.name} must not call selectReportSettingsMenu(\"root\") inside BackHandler",
                    text.contains("BackHandler") && text.contains("selectReportSettingsMenu(\"root\")")
                )
                if (file.name !in allowedBackHandlerFiles) {
                    assertFalse(
                        "${file.name} must not declare BackHandler (only MainActivity handles settings navigation)",
                        text.contains("BackHandler")
                    )
                }
            }
    }

    @Test
    fun backupAndSettingsStrings_usePlainLanguageWithoutTechnicalJargonAcrossLocales() {
        val appDir = resolveAppModuleDir()
        val localeFiles = listOf(
            "values" to File(appDir, "src/main/res/values/strings.xml"),
            "values-bn" to File(appDir, "src/main/res/values-bn/strings.xml"),
            "values-ar" to File(appDir, "src/main/res/values-ar/strings.xml")
        )

        val forbiddenRegex = Regex(
            """room|sqlite|json|multi-cloud|architecture|archive|database|ডাটাবেজ|ডাটাবেস|ডেটাবেস|আর্কাইভ|আর্কিটেকচার|মাল্টি-ক্লাউড|রুম|এসকিউলাইট|জেএসওএন|قاعدة البيانات|قواعد البيانات|أرشيف|معمارية|بنية|سحابية متعددة""",
            RegexOption.IGNORE_CASE
        )

        val plainLanguageKeys = listOf(
            "backup_subtitle",
            "backup_select_location_desc",
            "backup_data_desc",
            "backup_local_archives_title",
            "backup_total_archives_size",
            "backup_gdrive_none_found",
            "backup_delete_dialog_warning",
            "cloud_backup_success_notif_body",
            "currency_locked_message",
            "title_system_architecture",
            "desc_system_architecture",
            "msg_1_data_collectionnlojia_system",
            "title_shift_reports_pdf_archives"
        )

        val localeMaps = localeFiles.associate { (label, file) -> label to parseStringValues(file) }
        val enKeys = localeMaps.getValue("values").keys
        assertEquals(enKeys, localeMaps.getValue("values-bn").keys)
        assertEquals(enKeys, localeMaps.getValue("values-ar").keys)

        localeMaps.forEach { (locale, map) ->
            plainLanguageKeys.forEach { key ->
                val value = map[key].orEmpty()
                assertTrue("Expected non-blank value for $key in $locale", value.isNotBlank())
                assertFalse(
                    "Found technical jargon in $locale [$key]: $value",
                    forbiddenRegex.containsMatchIn(value)
                )
            }
        }
    }

    @Test
    fun invariants_schemaBackupVersionAndThemeRulesPreserved() {
        val appDir = resolveAppModuleDir()
        val dbSrc = File(appDir, "src/main/java/com/lojia/shiftreport/data/AppDatabase.kt").readText()
        assertTrue(dbSrc.contains("version = 20"))
        assertEquals(2, OfflineBackupManager.BACKUP_FORMAT_VERSION)

        val schemaJson = JSONObject(File(appDir, "schemas/com.lojia.shiftreport.data.AppDatabase/20.json").readText())
        val dbJson = schemaJson.getJSONObject("database")
        assertEquals(20, dbJson.getInt("version"))
        assertEquals("17f0f4d4037600babcf1824104954458", dbJson.getString("identityHash"))
    }
}
