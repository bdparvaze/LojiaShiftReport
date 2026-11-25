package com.lojia.shiftreport

import com.lojia.shiftreport.data.AppCountry
import com.lojia.shiftreport.data.SavedAddress
import com.lojia.shiftreport.settings.parsePhoneNumberWithCountry
import com.lojia.shiftreport.ui.common.splitPhoneNumber
import com.lojia.shiftreport.util.ITU_E164_MAX_DIGITS
import com.lojia.shiftreport.util.MIN_LOCAL_NUMBER_DIGITS
import com.lojia.shiftreport.util.OfflineBackupManager
import com.lojia.shiftreport.util.SupportContact
import com.lojia.shiftreport.util.buildE164Phone
import com.lojia.shiftreport.util.isValidPhone
import com.lojia.shiftreport.util.normalizeLocalInput
import com.lojia.shiftreport.util.resolveCountryIso
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class Stage30PhoneAndCountryUnitTest {

    private fun resolveAppModuleDir(): File {
        val candidates = listOf(
            File("."),
            File("app"),
            File(".."),
            File("../app")
        )
        return candidates.firstOrNull { dir ->
            File(dir, "src/main/java/com/lojia/shiftreport/util/PhoneNumberFormat.kt").isFile
        } ?: error("Unable to locate app module directory from ${File(".").absolutePath}")
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
    fun buildE164Phone_formatsMultipleCountriesCleanly() {
        assertEquals("+8801711000000", buildE164Phone(AppCountry.BANGLADESH, "1711000000"))
        assertEquals("+966501234567", buildE164Phone(AppCountry.SAUDI_ARABIA, "50 123-4567"))
        assertEquals("+12025550123", buildE164Phone(AppCountry.UNITED_STATES, "(202) 555-0123"))
        assertEquals("+447911123456", buildE164Phone(AppCountry.UNITED_KINGDOM, "+44 7911 123456"))
        assertEquals("", buildE164Phone(AppCountry.UNITED_ARAB_EMIRATES, "  --  "))
    }

    @Test
    fun normalizeLocalInput_stripsFormattingAndMatchingDialCode() {
        assertEquals("501234567", normalizeLocalInput("+966 50 123-4567", AppCountry.SAUDI_ARABIA))
        assertEquals("501234567", normalizeLocalInput("(+966) 50-123 4567", AppCountry.SAUDI_ARABIA))
        assertEquals("1711000000", normalizeLocalInput("+880 1711-000000", AppCountry.BANGLADESH))
        assertEquals("2025550123", normalizeLocalInput("(202) 555-0123", AppCountry.UNITED_STATES))
        assertEquals("9123456789", normalizeLocalInput("91234-56789", AppCountry.INDIA))
    }

    @Test
    fun isValidPhone_enforcesMinLocalAndItuE164MaxDigits() {
        assertEquals(4, MIN_LOCAL_NUMBER_DIGITS)
        assertEquals(15, ITU_E164_MAX_DIGITS)

        // Reject below min local digits
        assertFalse(isValidPhone(AppCountry.UNITED_STATES, ""))
        assertFalse(isValidPhone(AppCountry.UNITED_STATES, "123"))
        assertTrue(isValidPhone(AppCountry.UNITED_STATES, "1234"))

        // Saudi (+966 = 3 digits): 12 local digits -> 15 total (valid); 13 local -> 16 (invalid)
        assertTrue(isValidPhone(AppCountry.SAUDI_ARABIA, "123456789012"))
        assertFalse(isValidPhone(AppCountry.SAUDI_ARABIA, "1234567890123"))

        // US (+1 = 1 digit): 14 local digits -> 15 total (valid); 15 local -> 16 (invalid)
        assertTrue(isValidPhone(AppCountry.UNITED_STATES, "12345678901234"))
        assertFalse(isValidPhone(AppCountry.UNITED_STATES, "123456789012345"))
    }

    @Test
    fun parsePhoneNumberWithCountry_roundTripsAndHandlesOldFormatsWithoutCrashing() {
        val roundTripCases = listOf(
            AppCountry.BANGLADESH to "1711000000",
            AppCountry.SAUDI_ARABIA to "501234567",
            AppCountry.UNITED_ARAB_EMIRATES to "501234567",
            AppCountry.UNITED_STATES to "2025550123",
            AppCountry.CANADA to "4165550100",
            AppCountry.INDIA to "9876543210"
        )
        for ((country, local) in roundTripCases) {
            val e164 = buildE164Phone(country, local)
            val (parsedCountry, parsedLocal) = parsePhoneNumberWithCountry(e164, country)
            assertEquals(country, parsedCountry)
            assertEquals(local, parsedLocal)
        }

        // Old formats: spaces, no plus, empty
        val (spCountry, spLocal) = parsePhoneNumberWithCountry("+966 50 123 4567", AppCountry.UNITED_STATES)
        assertEquals(AppCountry.SAUDI_ARABIA, spCountry)
        assertEquals("501234567", spLocal)

        val (noPlusCountry, noPlusLocal) = parsePhoneNumberWithCountry("050 123-4567", AppCountry.EGYPT)
        assertEquals(AppCountry.EGYPT, noPlusCountry)
        assertEquals("0501234567", noPlusLocal)

        val (emptyCountry, emptyLocal) = parsePhoneNumberWithCountry("   ", AppCountry.JAPAN)
        assertEquals(AppCountry.JAPAN, emptyCountry)
        assertEquals("", emptyLocal)
    }

    @Test
    fun resolveCountryIso_followsSimNetworkLocaleFallbackPriority() {
        assertEquals("BD", resolveCountryIso("bd", "ae", "us", "US"))
        assertEquals("AE", resolveCountryIso(null, "ae", "us", "US"))
        assertEquals("AE", resolveCountryIso("   ", "ae", "us", "US"))
        assertEquals("SA", resolveCountryIso("", "12", "sa", "US"))
        assertEquals("GB", resolveCountryIso("x", "abc", "gb", "US"))
        assertEquals("US", resolveCountryIso(null, "", "1A", "US"))
    }

    @Test
    fun splitPhoneNumber_usesExplicitDefaultDialCodeWhenUnmatched() {
        val codes = AppCountry.entries.map { it.dialCode }
        assertEquals("+44" to "07911123456", splitPhoneNumber("07911123456", codes, "+44"))
        assertEquals("+880" to "", splitPhoneNumber("", codes, "+880"))
        assertEquals("+971" to "501234567", splitPhoneNumber("+971501234567", codes, "+1"))
    }

    @Test
    fun savedAddress_defaultsAreEmptyStrings() {
        val address = SavedAddress()
        assertEquals("", address.countryCode)
        assertEquals("", address.dialCode)
    }

    @Test
    fun supportContact_configurationAndUriBuildersWorkCleanly() {
        assertFalse(SupportContact.isSupportConfigured(phone = "", whatsapp = ""))
        assertFalse(SupportContact.isSupportConfigured(phone = "   ", whatsapp = "  "))
        assertTrue(SupportContact.isSupportConfigured(phone = "+1 555-0100", whatsapp = ""))
        assertTrue(SupportContact.isSupportConfigured(phone = "", whatsapp = "+1 555-0100"))

        assertEquals("tel:+15550100", SupportContact.buildDialUri("+1 (555) 010-0"))
        assertEquals("tel:15550100", SupportContact.buildDialUri("1555-0100"))
        assertEquals("", SupportContact.buildDialUri("  "))

        assertEquals("https://wa.me/15550100", SupportContact.buildWhatsAppUri("+1 (555) 010-0"))
        assertEquals("", SupportContact.buildWhatsAppUri(""))
    }

    @Test
    fun settingsReportSection_hasNoHardcodedFakeSupportNumber() {
        val appDir = resolveAppModuleDir()
        val settingsSource = File(
            appDir,
            "src/main/java/com/lojia/shiftreport/settings/SettingsReportSection.kt"
        ).readText()
        assertFalse(settingsSource.contains("1700-000000"))
        assertFalse(settingsSource.contains("8801700000000"))
        assertFalse(settingsSource.contains("wa.me/"))
    }

    private fun stripCommentsOutsideStrings(source: String): String {
        val sb = StringBuilder(source.length)
        var i = 0
        var inQuotes = false
        var inBlockComment = false
        while (i < source.length) {
            val c = source[i]
            val next = if (i + 1 < source.length) source[i + 1] else '\u0000'
            when {
                inBlockComment -> {
                    if (c == '*' && next == '/') {
                        inBlockComment = false
                        i += 2
                    } else {
                        if (c == '\n') sb.append('\n')
                        i++
                    }
                }
                inQuotes -> {
                    sb.append(c)
                    if (c == '\\' && i + 1 < source.length) {
                        sb.append(next)
                        i += 2
                    } else {
                        if (c == '"') inQuotes = false
                        i++
                    }
                }
                c == '/' && next == '*' -> {
                    inBlockComment = true
                    i += 2
                }
                c == '/' && next == '/' -> {
                    while (i < source.length && source[i] != '\n') i++
                }
                c == '"' -> {
                    inQuotes = true
                    sb.append(c)
                    i++
                }
                else -> {
                    sb.append(c)
                    i++
                }
            }
        }
        return sb.toString()
    }

    @Test
    fun mainJavaSources_haveNoUnexpectedDialCodeOrCountryDefaults() {
        val appDir = resolveAppModuleDir()
        val javaRoot = File(appDir, "src/main/java")
        val dialLiteralRegex = Regex("\"\\+[0-9]{1,4}[^\"]*\"")
        val countryDefaultRegex = Regex("\"(SA|BD)\"")

        val allowedDialFiles = setOf(
            "com/lojia/shiftreport/data/Models.kt",
            "com/lojia/shiftreport/auth/LojiaAuthModels.kt"
        )
        val allowedCountryDefaultFiles = setOf(
            "com/lojia/shiftreport/data/Models.kt",
            "com/lojia/shiftreport/auth/LojiaAuthModels.kt"
        )

        val dialViolations = mutableListOf<String>()
        val countryViolations = mutableListOf<String>()

        javaRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { file ->
                val rel = file.relativeTo(javaRoot).path.replace(File.separatorChar, '/')
                val codeOnly = stripCommentsOutsideStrings(file.readText())
                if (rel !in allowedDialFiles && dialLiteralRegex.containsMatchIn(codeOnly)) {
                    dialViolations.add(rel)
                }
                if (rel !in allowedCountryDefaultFiles && countryDefaultRegex.containsMatchIn(codeOnly)) {
                    countryViolations.add(rel)
                }
            }

        assertEquals("Unexpected dial code literals in: $dialViolations", emptyList<String>(), dialViolations)
        assertEquals("Unexpected SA/BD string literals in: $countryViolations", emptyList<String>(), countryViolations)
    }

    @Test
    fun roomSchema20AndBackupVersion_remainUnchanged() {
        val appDir = resolveAppModuleDir()
        val dbSource = File(appDir, "src/main/java/com/lojia/shiftreport/data/AppDatabase.kt").readText()
        assertTrue("Expected AppDatabase version = 20", dbSource.contains("version = 20"))
        assertEquals(2, OfflineBackupManager.BACKUP_FORMAT_VERSION)

        val schemaFile = File(appDir, "schemas/com.lojia.shiftreport.data.AppDatabase/20.json")
        assertTrue("Expected 20.json schema file to exist", schemaFile.isFile)
        val schemaJson = JSONObject(schemaFile.readText())
        val dbJson = schemaJson.getJSONObject("database")
        assertEquals(20, dbJson.getInt("version"))
        assertEquals("17f0f4d4037600babcf1824104954458", dbJson.getString("identityHash"))
    }

    @Test
    fun profilePhoneStrings_presentAcrossLocalesAndOldKeyRemoved() {
        val appDir = resolveAppModuleDir()
        val resDir = File(appDir, "src/main/res")
        val localeDirs = listOf("values", "values-bn", "values-ar")
        for (dir in localeDirs) {
            val keys = loadStringKeys(File(resDir, "$dir/strings.xml"))
            assertTrue("Missing profile_phone_invalid in $dir", keys.contains("profile_phone_invalid"))
            assertTrue("Missing profile_phone_mobile_hint in $dir", keys.contains("profile_phone_mobile_hint"))
            assertFalse("Removed profile_phone_digits_error still present in $dir", keys.contains("profile_phone_digits_error"))
        }
    }
}
