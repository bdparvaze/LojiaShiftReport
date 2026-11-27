package com.lojia.shiftreport

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class Stage35NavigationLanguageUnitTest {

    private fun resolveAppModuleDir(): File {
        val candidates = listOf(File("."), File("app"), File(".."), File("../app"))
        return candidates.firstOrNull { dir ->
            File(dir, "src/main/java/com/lojia/shiftreport/settings/SettingsBackupSubSection.kt").isFile
        } ?: error("Unable to locate app module directory from ${File(".").absolutePath}")
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
}
