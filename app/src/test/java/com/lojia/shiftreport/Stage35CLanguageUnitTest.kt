package com.lojia.shiftreport

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.lojia.shiftreport.data.AppLanguage
import com.lojia.shiftreport.data.nativeLanguageName
import com.lojia.shiftreport.data.supportedAppLanguages
import com.lojia.shiftreport.util.LanguagePreferences
import com.lojia.shiftreport.util.OfflineBackupManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory

class Stage35CLanguageUnitTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun resolveAppModuleDir(): File {
        val candidates = listOf(File("."), File("app"), File(".."), File("../app"))
        return candidates.firstOrNull { dir ->
            File(dir, "src/main/java/com/lojia/shiftreport/util/LanguagePreferences.kt").isFile
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

    private fun expectedLocaleDisplayName(code: String): String {
        val locale = Locale.forLanguageTag(code)
        val raw = locale.getDisplayLanguage(locale)
        return raw.replaceFirstChar { ch ->
            if (ch.isLowerCase()) ch.titlecase(locale) else ch.toString()
        }
    }

    @Test
    fun partA_nativeLanguageNameUsesStandardLocaleApiAndSupportedListUsesEntries() {
        val supported = supportedAppLanguages()
        assertEquals(AppLanguage.entries, supported)

        for (lang in supported) {
            assertEquals(expectedLocaleDisplayName(lang.code), lang.nativeName)
            assertEquals(expectedLocaleDisplayName(lang.code), nativeLanguageName(lang.code))
        }
        // Arbitrary language code works dynamically without hardcoded switch
        assertEquals(expectedLocaleDisplayName("fr"), nativeLanguageName("fr"))

        val appDir = resolveAppModuleDir()
        val switcherFiles = listOf(
            "src/main/java/com/lojia/shiftreport/ui/common/LanguageSwitcherButton.kt",
            "src/main/java/com/lojia/shiftreport/auth/BiometricManager.kt",
            "src/main/java/com/lojia/shiftreport/auth/LojiaRegisterScreen.kt",
            "src/main/java/com/lojia/shiftreport/auth/AuthScreen.kt"
        )
        val forbiddenLiterals = listOf("\"English\"", "\"Bengali\"", "\"Arabic\"", "\"বাংলা\"", "\"العربية\"")
        switcherFiles.forEach { relPath ->
            val src = File(appDir, relPath).readText()
            forbiddenLiterals.forEach { literal ->
                assertFalse(
                    "$relPath must not hardcode language name literal $literal",
                    src.contains(literal)
                )
            }
        }
    }

    @Test
    fun partB_resolveInitialLanguage_respectsStoredSystemAndFallbackRules() {
        val supported = supportedAppLanguages().map { it.code }

        // First launch: Bengali system -> bn
        assertEquals(
            "bn",
            LanguagePreferences.resolveInitialLanguage(
                systemLanguageTags = listOf("bn-BD", "en-US"),
                supportedCodes = supported,
                storedCode = null
            )
        )

        // First launch: Arabic system with region or underscore -> ar
        assertEquals(
            "ar",
            LanguagePreferences.resolveInitialLanguage(
                systemLanguageTags = listOf("ar-SA"),
                supportedCodes = supported,
                storedCode = null
            )
        )
        assertEquals(
            "bn",
            LanguagePreferences.resolveInitialLanguage(
                systemLanguageTags = listOf("bn_BD"),
                supportedCodes = supported,
                storedCode = ""
            )
        )

        // First launch: secondary system tag matches if first is unsupported
        assertEquals(
            "ar",
            LanguagePreferences.resolveInitialLanguage(
                systemLanguageTags = listOf("fr-FR", "ar-EG"),
                supportedCodes = supported,
                storedCode = null
            )
        )

        // First launch: unsupported system -> DEFAULT_PRIMARY_LANGUAGE_CODE
        assertEquals(
            LanguagePreferences.DEFAULT_PRIMARY_LANGUAGE_CODE,
            LanguagePreferences.resolveInitialLanguage(
                systemLanguageTags = listOf("fr-FR", "de-DE"),
                supportedCodes = supported,
                storedCode = null
            )
        )
        assertEquals(
            LanguagePreferences.DEFAULT_PRIMARY_LANGUAGE_CODE,
            LanguagePreferences.resolveInitialLanguage(
                systemLanguageTags = emptyList(),
                supportedCodes = supported,
                storedCode = null
            )
        )

        // Subsequent launch: stored preference ALWAYS wins over device language
        assertEquals(
            "bn",
            LanguagePreferences.resolveInitialLanguage(
                systemLanguageTags = listOf("ar-SA", "en-US"),
                supportedCodes = supported,
                storedCode = "bn"
            )
        )
        assertEquals(
            "en",
            LanguagePreferences.resolveInitialLanguage(
                systemLanguageTags = listOf("ar-SA"),
                supportedCodes = supported,
                storedCode = "EN"
            )
        )

        // Verify resolveInitialLanguage does not hardcode literal "en"
        val appDir = resolveAppModuleDir()
        val langPrefsSrc = File(appDir, "src/main/java/com/lojia/shiftreport/util/LanguagePreferences.kt").readText()
        val fnStart = langPrefsSrc.indexOf("fun resolveInitialLanguage(")
        assertTrue(fnStart >= 0)
        val fnSlice = langPrefsSrc.substring(fnStart, langPrefsSrc.indexOf("fun getSystemLanguageTags("))
        assertTrue(fnSlice.contains("DEFAULT_PRIMARY_LANGUAGE_CODE"))
        assertFalse("resolveInitialLanguage must not hardcode \"en\"", fnSlice.contains("\"en\""))
    }

    @Test
    fun partD_perUserDataStore_isolatesUsersAndLeavesDeviceLanguageIntact() = runBlocking {
        val dsFile = File(tempFolder.root, "stage35c_test.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = { dsFile }
        )

        val userA = LanguagePreferences.resolveUserId("  OwnerA  ")!!
        val userB = LanguagePreferences.resolveUserId("OwnerB")!!
        assertEquals("ownera", userA)
        assertEquals("ownerb", userB)

        // Before saving, users have no per-user preference
        assertNull(LanguagePreferences.getUserLanguage(dataStore, userA))
        assertNull(LanguagePreferences.getUserLanguageFlow(dataStore, userA).first())

        // User A selects Arabic -> updates device & userA
        LanguagePreferences.saveDeviceAndUserLanguage(dataStore, userA, "ar")
        assertEquals("ar", LanguagePreferences.getUserLanguage(dataStore, userA))
        assertEquals("ar", LanguagePreferences.getUserLanguageFlow(dataStore, userA).first())
        val prefsAfterA: Preferences = dataStore.data.first()
        assertEquals("ar", prefsAfterA[LanguagePreferences.LANGUAGE_KEY])

        // User B selects Bengali -> updates device & userB, without overwriting userA
        LanguagePreferences.saveDeviceAndUserLanguage(dataStore, userB, "bn")
        assertEquals("bn", LanguagePreferences.getUserLanguage(dataStore, userB))
        assertEquals("ar", LanguagePreferences.getUserLanguage(dataStore, userA))
        val prefsAfterB: Preferences = dataStore.data.first()
        assertEquals("bn", prefsAfterB[LanguagePreferences.LANGUAGE_KEY])

        // Unsupported code sanitizes to fallback primary code
        LanguagePreferences.saveUserLanguage(dataStore, "user_c", "xx")
        assertEquals(
            LanguagePreferences.DEFAULT_PRIMARY_LANGUAGE_CODE,
            LanguagePreferences.getUserLanguage(dataStore, "user_c")
        )
    }

    @Test
    fun partC_authScreensAndRecoveryIncludeLanguageSwitcher_andQuickPinDoesNot() {
        val appDir = resolveAppModuleDir()

        val switcherSrc = File(
            appDir,
            "src/main/java/com/lojia/shiftreport/ui/common/LanguageSwitcherButton.kt"
        ).readText()
        assertTrue(switcherSrc.contains("Icons.Outlined.Language"))
        assertTrue(switcherSrc.contains("R.string.auth_change_language_title"))
        assertTrue(switcherSrc.contains("supportedAppLanguages()"))
        assertTrue(switcherSrc.contains("LojiaDialog"))
        assertTrue(switcherSrc.contains("Dimens.MinTouchTarget"))

        val biometricSrc = File(appDir, "src/main/java/com/lojia/shiftreport/auth/BiometricManager.kt").readText()
        val loginStart = biometricSrc.indexOf("AuthScreenPage.LOGIN ->")
        val registerStart = biometricSrc.indexOf("AuthScreenPage.REGISTER ->")
        val recoveryStart = biometricSrc.indexOf("fun LojiaPasswordRecoveryDialog(")
        assertTrue(loginStart > 0 && registerStart > loginStart && recoveryStart > registerStart)
        assertTrue(
            "Login page must show LanguageSwitcherButton",
            biometricSrc.substring(loginStart, registerStart).contains("LanguageSwitcherButton(")
        )
        assertTrue(
            "Password recovery dialog must show LanguageSwitcherButton",
            biometricSrc.substring(recoveryStart).contains("LanguageSwitcherButton(")
        )
        assertTrue(
            "BiometricLockScreen must reactively derive isBn from language.code",
            biometricSrc.contains("val isBn = language.code == \"bn\"")
        )
        assertTrue(
            "BiometricLockScreen must guard remembered username initialization across recreation",
            biometricSrc.contains("hasInitializedRememberedUsername")
        )

        val authScreenSrc = File(appDir, "src/main/java/com/lojia/shiftreport/auth/AuthScreen.kt").readText()
        assertTrue(authScreenSrc.contains("LanguageSwitcherButton("))
        assertTrue(authScreenSrc.contains("rememberSaveable"))

        val registerSrc = File(appDir, "src/main/java/com/lojia/shiftreport/auth/LojiaRegisterScreen.kt").readText()
        assertTrue(registerSrc.contains("LanguageSwitcherButton("))
        assertTrue(registerSrc.contains("rememberSaveable"))
        assertTrue(registerSrc.contains("LanguagePreferences.saveDeviceAndUserLanguage"))

        val quickPinSrc = File(appDir, "src/main/java/com/lojia/shiftreport/auth/QuickPinScreen.kt").readText()
        assertFalse(
            "QuickPinScreen must NOT contain LanguageSwitcherButton",
            quickPinSrc.contains("LanguageSwitcherButton")
        )
        assertFalse(
            "QuickPinScreen must NOT show change language button",
            quickPinSrc.contains("btn_language_switcher")
        )

        val mainActivitySrc = File(appDir, "src/main/java/com/lojia/shiftreport/MainActivity.kt").readText()
        assertTrue(mainActivitySrc.contains("resolveInitialLanguage"))
        assertTrue(mainActivitySrc.contains("LanguagePreferences.getLanguageFlow"))
        assertTrue(mainActivitySrc.contains("applyUserLanguageOnLogin"))
    }

    @Test
    fun partD_noPerUserLanguageInDatabaseOrBackup_andInvariantsPreserved() {
        val appDir = resolveAppModuleDir()

        val dbFiles = listOf(
            "src/main/java/com/lojia/shiftreport/data/Models.kt",
            "src/main/java/com/lojia/shiftreport/data/AppDatabase.kt",
            "src/main/java/com/lojia/shiftreport/data/ReportDao.kt",
            "src/main/java/com/lojia/shiftreport/util/OfflineBackupManager.kt"
        )
        dbFiles.forEach { relPath ->
            val src = File(appDir, relPath).readText()
            assertFalse(
                "$relPath must not contain per-user language storage",
                src.contains("user_language_") || src.contains("saveUserLanguage")
            )
        }

        val dbSrc = File(appDir, "src/main/java/com/lojia/shiftreport/data/AppDatabase.kt").readText()
        assertTrue("Database schema version must stay 20", dbSrc.contains("version = 20"))
        assertEquals(2, OfflineBackupManager.BACKUP_FORMAT_VERSION)

        val schemaJson = JSONObject(File(appDir, "schemas/com.lojia.shiftreport.data.AppDatabase/20.json").readText())
        val dbJson = schemaJson.getJSONObject("database")
        assertEquals(20, dbJson.getInt("version"))
        assertEquals("17f0f4d4037600babcf1824104954458", dbJson.getString("identityHash"))

        val enMap = parseStringValues(File(appDir, "src/main/res/values/strings.xml"))
        val bnMap = parseStringValues(File(appDir, "src/main/res/values-bn/strings.xml"))
        val arMap = parseStringValues(File(appDir, "src/main/res/values-ar/strings.xml"))
        assertEquals(enMap.keys, bnMap.keys)
        assertEquals(enMap.keys, arMap.keys)

        listOf("auth_change_language_title", "cd_selected").forEach { key ->
            assertFalse("Missing $key in en", enMap[key].isNullOrBlank())
            assertFalse("Missing $key in bn", bnMap[key].isNullOrBlank())
            assertFalse("Missing $key in ar", arMap[key].isNullOrBlank())
        }
    }
}
