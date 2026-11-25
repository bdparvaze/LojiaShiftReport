package com.lojia.shiftreport

import com.lojia.shiftreport.sync.FirebaseCloudSyncManager
import com.lojia.shiftreport.util.GoogleDriveManager
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class Stage31ReleaseReadinessUnitTest {

    private fun resolveAppModuleDir(): File {
        val candidates = listOf(
            File("."),
            File("app"),
            File(".."),
            File("../app")
        )
        return candidates.firstOrNull { dir ->
            File(dir, "src/main/java/com/lojia/shiftreport/data/AppDatabase.kt").isFile
        } ?: error("Unable to locate app module directory from ${File(".").absolutePath}")
    }

    private fun resolveRootDir(): File {
        val canonicalApp = resolveAppModuleDir().canonicalFile
        val parent = canonicalApp.parentFile
        return if (parent != null && File(parent, "settings.gradle.kts").isFile) {
            parent
        } else {
            canonicalApp
        }
    }

    private fun loadStringKeys(xmlFile: File): Set<String> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlFile)
        val nodes = doc.getElementsByTagName("string")
        val keys = mutableSetOf<String>()
        for (i in 0 until nodes.length) {
            nodes.item(i).attributes?.getNamedItem("name")?.nodeValue?.let { keys.add(it) }
        }
        return keys
    }

    @Test
    fun test1_migration13To14_usesEmptyCountryCodeDefault_andRoomSchema20HashUnchanged() {
        val appDir = resolveAppModuleDir()
        val appDbText = File(appDir, "src/main/java/com/lojia/shiftreport/data/AppDatabase.kt").readText()
        val migStart = appDbText.indexOf("val MIGRATION_13_14")
        val migEnd = appDbText.indexOf("val MIGRATION_14_15")
        assertTrue("MIGRATION_13_14 block must exist", migStart in 0 until migEnd)
        val migBlock = appDbText.substring(migStart, migEnd)
        assertTrue(
            "MIGRATION_13_14 must default countryCode to ''",
            migBlock.contains("`countryCode` TEXT NOT NULL DEFAULT ''")
        )
        assertFalse(
            "MIGRATION_13_14 must not default countryCode to 'SA'",
            migBlock.contains("DEFAULT 'SA'")
        )

        val schema20File = File(appDir, "schemas/com.lojia.shiftreport.data.AppDatabase/20.json")
        assertTrue("Room 20.json schema must exist", schema20File.isFile)
        val dbObj = JSONObject(schema20File.readText()).getJSONObject("database")
        assertEquals(20, dbObj.getInt("version"))
        assertEquals("17f0f4d4037600babcf1824104954458", dbObj.getString("identityHash"))
    }

    @Test
    fun test2_unusedPdfDefaultPhoneRemoved_andAllLocalesMatch() {
        val appDir = resolveAppModuleDir()
        val enKeys = loadStringKeys(File(appDir, "src/main/res/values/strings.xml"))
        val bnKeys = loadStringKeys(File(appDir, "src/main/res/values-bn/strings.xml"))
        val arKeys = loadStringKeys(File(appDir, "src/main/res/values-ar/strings.xml"))

        assertFalse("pdf_default_phone must be removed from values", enKeys.contains("pdf_default_phone"))
        assertFalse("pdf_default_phone must be removed from values-bn", bnKeys.contains("pdf_default_phone"))
        assertFalse("pdf_default_phone must be removed from values-ar", arKeys.contains("pdf_default_phone"))
        assertEquals(enKeys, bnKeys)
        assertEquals(enKeys, arKeys)
        assertEquals(2246, enKeys.size)
    }

    @Test
    fun test3_seedAdminCredentialsAreGuardedByBuildConfigDebug() {
        val appDir = resolveAppModuleDir()
        val appDbText = File(appDir, "src/main/java/com/lojia/shiftreport/data/AppDatabase.kt").readText()
        val callbackStart = appDbText.indexOf("private class DatabaseCallback")
        assertTrue("DatabaseCallback must exist", callbackStart >= 0)
        val callbackText = appDbText.substring(callbackStart)

        val onOpenIdx = callbackText.indexOf("override fun onOpen")
        val popIdx = callbackText.indexOf("suspend fun populateInitialData")
        assertTrue(onOpenIdx in 0 until popIdx)

        val onOpenBlock = callbackText.substring(onOpenIdx, popIdx)
        val popBlock = callbackText.substring(popIdx)

        assertTrue("onOpen seed block must check BuildConfig.DEBUG", onOpenBlock.contains("BuildConfig.DEBUG"))
        assertTrue("populateInitialData seed user must check BuildConfig.DEBUG", popBlock.contains("BuildConfig.DEBUG"))

        val debugGuardInPop = popBlock.indexOf("BuildConfig.DEBUG")
        val saveUserInPop = popBlock.indexOf("reportDao.saveUserProfile")
        assertTrue(
            "BuildConfig.DEBUG guard must precede reportDao.saveUserProfile in populateInitialData",
            debugGuardInPop in 0 until saveUserInPop
        )
    }

    @Test
    fun test4_mainSourcesHaveNoRawPrintStackTraceOrSensitivePiiLogs() {
        val mainJavaDir = File(resolveAppModuleDir(), "src/main/java")
        val forbiddenPrintCalls = Regex("""(\.printStackTrace\s*\(|println\s*\(|System\.out\.|System\.err\.)""")
        val logLinePattern = Regex("""\b(?:Log|AppLogger)\.(?:v|d|i|w|e|wtf)\b""")
        val sensitiveInterpPattern = Regex("""\$\{?(?:email|pin|password|token|secret|answer|phone)\b""", RegexOption.IGNORE_CASE)

        val violations = mutableListOf<String>()
        mainJavaDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { file ->
                file.readLines().forEachIndexed { idx, line ->
                    val lineNum = idx + 1
                    if (forbiddenPrintCalls.containsMatchIn(line)) {
                        violations.add("${file.name}:$lineNum uses raw print/stackTrace: ${line.trim()}")
                    }
                    if (logLinePattern.containsMatchIn(line) && sensitiveInterpPattern.containsMatchIn(line)) {
                        violations.add("${file.name}:$lineNum logs sensitive variable: ${line.trim()}")
                    }
                }
            }

        assertTrue("Logging audit violations found:\n${violations.joinToString("\n")}", violations.isEmpty())
    }

    @Test
    fun test5_manifestReleaseHardening_noDebuggableNoCleartextNoUnusedNearbyWifi() {
        val manifestFile = File(resolveAppModuleDir(), "src/main/AndroidManifest.xml")
        val manifestText = manifestFile.readText()

        assertFalse("Manifest must not set debuggable=true", manifestText.contains("android:debuggable=\"true\""))
        assertTrue("Manifest must disable cleartext traffic", manifestText.contains("android:usesCleartextTraffic=\"false\""))
        assertTrue("Manifest must disable OS cloud/adb allowBackup", manifestText.contains("android:allowBackup=\"false\""))
        assertFalse(
            "Unused NEARBY_WIFI_DEVICES permission must be removed",
            manifestText.contains("android.permission.NEARBY_WIFI_DEVICES")
        )
        assertTrue(
            "FileProvider must remain unexported",
            manifestText.contains("android:exported=\"false\"")
        )
    }

    @Test
    fun test6_cloudClientIdsExternalizedToBuildConfigWithEmptyDefaults() {
        assertEquals("", BuildConfig.GOOGLE_DRIVE_CLIENT_ID)
        assertEquals("", BuildConfig.FIREBASE_PROJECT_ID)
        assertEquals(BuildConfig.GOOGLE_DRIVE_CLIENT_ID, GoogleDriveManager.CLIENT_ID)
        assertEquals(BuildConfig.FIREBASE_PROJECT_ID, FirebaseCloudSyncManager.DEFAULT_PROJECT_ID)

        val mainJavaDir = File(resolveAppModuleDir(), "src/main/java")
        val allMainCode = mainJavaDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .joinToString("\n") { it.readText() }

        assertFalse(
            "Hardcoded Google Drive client ID must not exist in main source",
            allMainCode.contains("654989706304-6knvt1b4gravtsu1a59snfg1acgjre74")
        )
        assertFalse(
            "Hardcoded Firebase project ID must not exist in main source",
            allMainCode.contains("gen-lang-client-0290392339")
        )
    }

    @Test
    fun test7_proguardRulesAndReleaseBuildMinification_configuredCorrectly() {
        val appDir = resolveAppModuleDir()
        val gradleText = File(appDir, "build.gradle.kts").readText()
        assertTrue("Release build must enable minification", gradleText.contains("isMinifyEnabled = true"))
        assertTrue("Release build must enable resource shrinking", gradleText.contains("isShrinkResources = true"))
        assertFalse("CLOUD_SYNC_ENABLED must remain false", BuildConfig.CLOUD_SYNC_ENABLED)

        val proguardText = File(appDir, "proguard-rules.pro").readText()
        assertTrue(proguardText.contains("-keep class com.lojia.shiftreport.data.** { *; }"))
        assertTrue(proguardText.contains("-keep @androidx.room.Entity class * { *; }"))
        assertTrue(proguardText.contains("-keep @androidx.room.Dao interface * { *; }"))
        assertTrue(proguardText.contains("-keep @androidx.room.Database class * { *; }"))
        assertTrue(proguardText.contains("@androidx.room.TypeConverter"))
        assertTrue(proguardText.contains("-keep class * implements androidx.room.migration.Migration"))
        assertTrue(proguardText.contains("-keep class com.lojia.shiftreport.settings.backup.** { *; }"))
        assertTrue(proguardText.contains("-keep class com.lojia.shiftreport.util.DriveBackupItem { *; }"))
        assertTrue(proguardText.contains("-keep class com.lojia.shiftreport.util.DriveAccountInfo { *; }"))
    }

    @Test
    fun test8_releaseSigningConfigAndGitignore_configuredWithoutHardcodedPasswords() {
        val rootDir = resolveRootDir()
        val gitignoreText = File(rootDir, ".gitignore").readText()
        assertTrue(".gitignore must ignore local.properties", gitignoreText.contains("local.properties"))
        assertTrue(".gitignore must ignore *.jks", gitignoreText.contains("*.jks"))
        assertTrue(".gitignore must ignore *.keystore", gitignoreText.contains("*.keystore"))

        val gradleText = File(resolveAppModuleDir(), "build.gradle.kts").readText()
        assertTrue(gradleText.contains("release.keystore.path"))
        assertTrue(gradleText.contains("release.keystore.password"))
        assertTrue(gradleText.contains("release.key.alias"))
        assertTrue(gradleText.contains("release.key.password"))
        assertTrue(gradleText.contains("if (hasReleaseSigning)"))
        assertTrue(gradleText.contains("signingConfig = null"))
        assertTrue(gradleText.contains("WARNING: Release signing is not configured"))
        assertTrue(gradleText.contains("create(\"debugConfig\")"))
    }
}
