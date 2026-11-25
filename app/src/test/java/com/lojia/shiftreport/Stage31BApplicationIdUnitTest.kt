package com.lojia.shiftreport

import com.lojia.shiftreport.util.AppConstants
import com.lojia.shiftreport.util.OfflineBackupManager
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class Stage31BApplicationIdUnitTest {

    private fun resolveAppModuleDir(): File {
        val candidates = listOf(
            File("."),
            File("app"),
            File(".."),
            File("../app")
        )
        return candidates.firstOrNull { dir ->
            File(dir, "src/main/AndroidManifest.xml").isFile &&
                File(dir, "build.gradle.kts").isFile
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
    fun test1_gradleApplicationIdAndNamespace_areFinalLojiaShiftReport() {
        val appDir = resolveAppModuleDir()
        val gradleText = File(appDir, "build.gradle.kts").readText()

        assertTrue(
            "build.gradle.kts must set namespace = \"com.lojia.shiftreport\"",
            Regex("""namespace\s*=\s*"com\.lojia\.shiftreport"""").containsMatchIn(gradleText)
        )
        assertTrue(
            "build.gradle.kts must set applicationId = \"com.lojia.shiftreport\"",
            Regex("""applicationId\s*=\s*"com\.lojia\.shiftreport"""").containsMatchIn(gradleText)
        )
        assertEquals("com.lojia.shiftreport", BuildConfig.APPLICATION_ID)
        assertEquals(2, BuildConfig.VERSION_CODE)
        assertEquals("1.1", BuildConfig.VERSION_NAME)
    }

    @Test
    fun test2_noOldAiStudioIdOrDomainAppIdAnywhereInProject() {
        val rootDir = resolveRootDir()
        val forbiddenPrefix = "com." + "aistudio"
        val forbiddenDomain = "lojia." + "com"
        val allowedFallbackEmail = "admin@" + forbiddenDomain
        val ignoredDirs = setOf(".gradle", "build", ".git", ".build-outputs", ".kotlin", ".idea")
        val textExtensions = setOf(
            "kt", "kts", "java", "xml", "pro", "md", "txt", "json", "yml", "yaml", "properties", "gitignore"
        )

        val hits = mutableListOf<String>()
        rootDir.walkTopDown()
            .onEnter { dir -> dir.name !in ignoredDirs }
            .filter { file ->
                file.isFile && (file.extension.lowercase() in textExtensions || file.name.startsWith("."))
            }
            .forEach { file ->
                val rel = file.relativeTo(rootDir).path.replace(File.separatorChar, '/')
                file.readLines().forEachIndexed { idx, line ->
                    if (line.contains(forbiddenPrefix, ignoreCase = true)) {
                        hits.add("$rel:${idx + 1}: ${line.trim()}")
                    }
                    if (line.contains(forbiddenDomain, ignoreCase = true) && !line.contains(allowedFallbackEmail)) {
                        hits.add("$rel:${idx + 1} (domain): ${line.trim()}")
                    }
                }
            }

        assertTrue("Unexpected old applicationId references found:\n${hits.joinToString("\n")}", hits.isEmpty())
    }

    @Test
    fun test3_manifestHasSingleOAuth2CallbackEntry_andFileProviderUsesDynamicAuthority() {
        val appDir = resolveAppModuleDir()
        val manifestFile = File(appDir, "src/main/AndroidManifest.xml")
        val doc = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
        }.newDocumentBuilder().parse(manifestFile)

        val dataNodes = doc.getElementsByTagName("data")
        val oauthEntries = mutableListOf<Pair<String?, String?>>()
        val androidNs = "http://schemas.android.com/apk/res/android"
        for (i in 0 until dataNodes.length) {
            val attrs = dataNodes.item(i).attributes
            val host = attrs.getNamedItemNS(androidNs, "host")?.nodeValue
                ?: attrs.getNamedItem("android:host")?.nodeValue
            val scheme = attrs.getNamedItemNS(androidNs, "scheme")?.nodeValue
                ?: attrs.getNamedItem("android:scheme")?.nodeValue
            if (host == AppConstants.OAUTH_REDIRECT_HOST) {
                oauthEntries.add(scheme to host)
            }
        }

        assertEquals(
            "Manifest must have exactly one oauth2callback data entry",
            listOf(AppConstants.OAUTH_REDIRECT_SCHEME to AppConstants.OAUTH_REDIRECT_HOST),
            oauthEntries
        )

        val manifestText = manifestFile.readText()
        assertTrue(
            "Manifest FileProvider must use \${applicationId}.fileprovider",
            manifestText.contains("android:authorities=\"\${applicationId}.fileprovider\"")
        )

        val fileProviderFiles = listOf(
            "src/main/java/com/lojia/shiftreport/util/PdfReportGenerator.kt",
            "src/main/java/com/lojia/shiftreport/util/EmployerAdvanceStatementPdfRenderer.kt",
            "src/main/java/com/lojia/shiftreport/util/ExportHelper.kt",
            "src/main/java/com/lojia/shiftreport/util/PdfShareUtils.kt",
            "src/main/java/com/lojia/shiftreport/settings/BusinessLogoPickerCard.kt",
            "src/main/java/com/lojia/shiftreport/scanner/DocumentScannerViewModel.kt"
        )
        for (rel in fileProviderFiles) {
            val text = File(appDir, rel).readText()
            assertTrue(
                "$rel must use \"\${context.packageName}.fileprovider\"",
                text.contains("\"\${context.packageName}.fileprovider\"")
            )
        }
    }

    @Test
    fun test4_roomSchemaVersion20HashAndBackupFormatVersion_unchanged() {
        val appDir = resolveAppModuleDir()
        val dbSource = File(appDir, "src/main/java/com/lojia/shiftreport/data/AppDatabase.kt").readText()
        assertTrue("Expected AppDatabase version = 20", dbSource.contains("version = 20"))
        assertEquals(2, OfflineBackupManager.BACKUP_FORMAT_VERSION)

        val schemaFile = File(appDir, "schemas/com.lojia.shiftreport.data.AppDatabase/20.json")
        assertTrue("Expected 20.json schema file to exist", schemaFile.isFile)
        val dbObj = JSONObject(schemaFile.readText()).getJSONObject("database")
        assertEquals(20, dbObj.getInt("version"))
        assertEquals("17f0f4d4037600babcf1824104954458", dbObj.getString("identityHash"))
    }

    @Test
    fun test5_zeroHexColorLiteralsOutsideUiTheme() {
        val mainJavaDir = File(resolveAppModuleDir(), "src/main/java/com/lojia/shiftreport")
        val violations = mutableListOf<String>()
        mainJavaDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { file ->
                val rel = file.relativeTo(mainJavaDir).path.replace(File.separatorChar, '/')
                if (!rel.startsWith("ui/theme/")) {
                    file.readLines().forEachIndexed { idx, line ->
                        if (line.contains("Color(0x")) {
                            violations.add("$rel:${idx + 1}: ${line.trim()}")
                        }
                    }
                }
            }

        assertTrue("Found Color(0x outside ui/theme:\n${violations.joinToString("\n")}", violations.isEmpty())
    }

    @Test
    fun test6_stringKeyParityAcrossEnglishBengaliAndArabic() {
        val resDir = File(resolveAppModuleDir(), "src/main/res")
        val enKeys = loadStringKeys(File(resDir, "values/strings.xml"))
        val bnKeys = loadStringKeys(File(resDir, "values-bn/strings.xml"))
        val arKeys = loadStringKeys(File(resDir, "values-ar/strings.xml"))

        assertEquals(enKeys, bnKeys)
        assertEquals(enKeys, arKeys)
        assertFalse(enKeys.isEmpty())
    }
}
