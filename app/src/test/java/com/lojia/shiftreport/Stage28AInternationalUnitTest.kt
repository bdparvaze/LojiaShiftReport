package com.lojia.shiftreport

import com.lojia.shiftreport.report.canConfirmOpenShift
import com.lojia.shiftreport.report.normalizeOpenShiftCashier
import com.lojia.shiftreport.util.DateTimeFormatUtils
import com.lojia.shiftreport.util.GoogleDriveManager
import com.lojia.shiftreport.util.LanguagePreferences
import com.lojia.shiftreport.util.OfflineBackupManager
import java.io.File
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Stage28AInternationalUnitTest {

    private fun appFile(relativePath: String): File {
        val direct = File(relativePath)
        return if (direct.exists()) direct else File("app", relativePath)
    }

    private fun parseStringsXml(relativePath: String): Map<String, String> {
        val file = appFile(relativePath)
        assertTrue("Expected file to exist: $relativePath", file.exists())
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val nodes = doc.getElementsByTagName("string")
        val map = LinkedHashMap<String, String>()
        for (i in 0 until nodes.length) {
            val node = nodes.item(i)
            val name = node.attributes?.getNamedItem("name")?.nodeValue ?: continue
            map[name] = node.textContent ?: ""
        }
        return map
    }

    private fun extractPlaceholders(text: String): List<String> {
        val regex = Regex("""%(\d+\$)?[-#+ 0,(]*\d*(\.\d+)?[a-zA-Z%]""")
        return regex.findAll(text).map { it.value }.toList()
    }

    @Test
    fun taskA_cardLabelsHaveNoMadaInAnyLocaleAndPreservePlaceholders() {
        val locales = listOf(
            "src/main/res/values/strings.xml",
            "src/main/res/values-bn/strings.xml",
            "src/main/res/values-ar/strings.xml"
        )
        val forbiddenTerms = listOf("mada", "مدى", "مادا", "মাদা")
        val expectedUpdatedKeys = listOf(
            "card_mada_payments",
            "card_payment_opt",
            "card",
            "mada_payments",
            "mada_required",
            "net_mada_bank",
            "mada_bank",
            "cash_0f_mada_0f",
            "madacard",
            "pdf_payment_split",
            "mada_cards_label",
            "mada_bank_cards",
            "bank_mada_fmt",
            "pdf_mada_bank",
            "pdf_net_mada_bank",
            "pdf_card_mada_sales_label",
            "zreport_card_mada",
            "export_csv_header",
            "monthly_mada_digital_settlements",
            "contrast_multiplier_fmt"
        )

        val mapsByLocale = locales.associateWith { parseStringsXml(it) }

        for ((path, strings) in mapsByLocale) {
            for ((key, value) in strings) {
                val lower = value.lowercase(Locale.ROOT)
                for (forbidden in forbiddenTerms) {
                    assertFalse(
                        "Forbidden term '$forbidden' found in $path key '$key': $value",
                        lower.contains(forbidden)
                    )
                }
            }
            for (key in expectedUpdatedKeys) {
                assertFalse("Key '$key' missing or blank in $path", strings[key].isNullOrBlank())
            }
        }

        val baseStrings = mapsByLocale.getValue("src/main/res/values/strings.xml")
        for (key in expectedUpdatedKeys) {
            val basePlaceholders = extractPlaceholders(baseStrings.getValue(key))
            for ((path, strings) in mapsByLocale) {
                assertEquals(
                    "Placeholder mismatch for key '$key' in $path",
                    basePlaceholders,
                    extractPlaceholders(strings.getValue(key))
                )
            }
        }
    }

    @Test
    fun taskB_displayDateTimeFormattingFollowsLocalizedStylesAcrossEnArBn() {
        val sampleInstantMs = 1_747_059_000_000L // 2025-05-12T14:10:00Z
        val zone = ZoneId.systemDefault()
        val zoned = Instant.ofEpochMilli(sampleInstantMs).atZone(zone)
        val yearMonth = YearMonth.of(2025, 5)
        val localDate = LocalDate.of(2025, 5, 12)

        val testLocales = listOf(
            Locale.forLanguageTag("en"),
            Locale.forLanguageTag("ar"),
            Locale.forLanguageTag("bn")
        )

        for (locale in testLocales) {
            val expectedMediumDate = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                .withLocale(locale)
                .format(zoned)
            val expectedShortDate = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
                .withLocale(locale)
                .format(zoned)
            val expectedMediumDateTime = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                .withLocale(locale)
                .format(zoned)
            val expectedShortTime = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
                .withLocale(locale)
                .format(zoned)

            assertEquals(expectedMediumDate, DateTimeFormatUtils.formatDate(sampleInstantMs, locale))
            assertEquals(expectedShortDate, DateTimeFormatUtils.formatDateShort(sampleInstantMs, locale))
            assertEquals(expectedMediumDate, DateTimeFormatUtils.formatDateOnly(sampleInstantMs, locale))
            assertEquals(expectedMediumDateTime, DateTimeFormatUtils.formatDateTime(sampleInstantMs, locale))
            assertEquals(expectedMediumDateTime, DateTimeFormatUtils.formatDateTimeWithAt(sampleInstantMs, locale))
            assertEquals(expectedShortTime, DateTimeFormatUtils.formatTime(sampleInstantMs, locale))
            assertEquals(expectedShortTime, DateTimeFormatUtils.formatTime24(sampleInstantMs, locale))

            val intFmt = NumberFormat.getIntegerInstance(locale).apply { isGroupingUsed = false }
            val expectedMonthYear = "${yearMonth.month.getDisplayName(TextStyle.FULL_STANDALONE, locale)} ${intFmt.format(yearMonth.year)}"
            val expectedMonthDay = "${localDate.month.getDisplayName(TextStyle.SHORT, locale)} ${intFmt.format(localDate.dayOfMonth)}"
            val expectedWeekday = DayOfWeek.MONDAY.getDisplayName(TextStyle.SHORT, locale)

            assertEquals(expectedMonthYear, DateTimeFormatUtils.formatMonthYear(yearMonth, locale))
            assertEquals(expectedMonthDay, DateTimeFormatUtils.formatMonthDay(localDate, locale))
            assertEquals(expectedWeekday, DateTimeFormatUtils.formatShortWeekday(DayOfWeek.MONDAY, locale))
        }
    }

    @Test
    fun taskB_isoHelpersUseAsciiDigitsAndIsoShapesUnderNonLatinDefaultLocales() {
        val sampleInstantMs = 1_747_059_000_000L
        val originalDefault = Locale.getDefault()
        try {
            for (tag in listOf("ar-EG", "bn-BD", "en-US")) {
                Locale.setDefault(Locale.forLanguageTag(tag))
                val isoDate = DateTimeFormatUtils.formatIsoDate(sampleInstantMs)
                val isoDateTime = DateTimeFormatUtils.formatIsoDateTime(sampleInstantMs)
                val isoDateTimeSeconds = DateTimeFormatUtils.formatIsoDateTimeSeconds(sampleInstantMs)
                val fileTs = DateTimeFormatUtils.formatFileTimestamp(sampleInstantMs)
                val fileTsDash = DateTimeFormatUtils.formatFileTimestampDash(sampleInstantMs)

                assertTrue("Expected yyyy-MM-dd for $tag: $isoDate", isoDate.matches(Regex("""^\d{4}-\d{2}-\d{2}$""")))
                assertTrue("Expected yyyy-MM-dd HH:mm for $tag: $isoDateTime", isoDateTime.matches(Regex("""^\d{4}-\d{2}-\d{2} \d{2}:\d{2}$""")))
                assertTrue("Expected yyyy-MM-dd HH:mm:ss for $tag: $isoDateTimeSeconds", isoDateTimeSeconds.matches(Regex("""^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$""")))
                assertTrue("Expected yyyyMMdd_HHmmss for $tag: $fileTs", fileTs.matches(Regex("""^\d{8}_\d{6}$""")))
                assertTrue("Expected yyyy-MM-dd_HH-mm for $tag: $fileTsDash", fileTsDash.matches(Regex("""^\d{4}-\d{2}-\d{2}_\d{2}-\d{2}$""")))
            }
        } finally {
            Locale.setDefault(originalDefault)
        }
    }

    @Test
    fun taskB_mainSourceHasZeroSimpleDateFormatZeroLocaleUsAndOnlyIsoOfPattern() {
        val mainJavaDir = appFile("src/main/java")
        assertTrue(mainJavaDir.isDirectory)

        val simpleDateFormatHits = mutableListOf<String>()
        val localeUsHits = mutableListOf<String>()
        val unexpectedOfPatternHits = mutableListOf<String>()

        mainJavaDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { file ->
                val relPath = file.relativeTo(mainJavaDir).path.replace('\\', '/')
                file.readLines().forEachIndexed { idx, line ->
                    val lineNo = idx + 1
                    if (line.contains("SimpleDateFormat")) {
                        simpleDateFormatHits.add("$relPath:$lineNo -> $line")
                    }
                    if (Regex("""\bLocale\.US\b""").containsMatchIn(line)) {
                        localeUsHits.add("$relPath:$lineNo -> $line")
                    }
                    if (line.contains("ofPattern(")) {
                        val isAllowedIsoHelper = relPath == "com/lojia/shiftreport/util/DateTimeFormatUtils.kt" &&
                            line.contains("Locale.ROOT")
                        if (!isAllowedIsoHelper) {
                            unexpectedOfPatternHits.add("$relPath:$lineNo -> $line")
                        }
                    }
                }
            }

        assertTrue("SimpleDateFormat still present:\n${simpleDateFormatHits.joinToString("\n")}", simpleDateFormatHits.isEmpty())
        assertTrue("Locale.US still present:\n${localeUsHits.joinToString("\n")}", localeUsHits.isEmpty())
        assertTrue("Non-ISO ofPattern still present:\n${unexpectedOfPatternHits.joinToString("\n")}", unexpectedOfPatternHits.isEmpty())
    }

    @Test
    fun taskC_openShiftRejectsBlankCashierAndDisablesConfirmation() {
        assertNull(normalizeOpenShiftCashier(""))
        assertNull(normalizeOpenShiftCashier("   \t\n "))
        assertEquals("Amina", normalizeOpenShiftCashier("  Amina  "))

        assertFalse(canConfirmOpenShift(isFloatError = false, cashierName = ""))
        assertFalse(canConfirmOpenShift(isFloatError = false, cashierName = "   "))
        assertFalse(canConfirmOpenShift(isFloatError = true, cashierName = "Amina"))
        assertTrue(canConfirmOpenShift(isFloatError = false, cashierName = "Amina"))

        val vmSource = appFile("src/main/java/com/lojia/shiftreport/report/ReportViewModel.kt").readText()
        val openShiftIdx = vmSource.indexOf("fun openShift(")
        assertTrue("openShift function not found", openShiftIdx >= 0)
        val openShiftBody = vmSource.substring(openShiftIdx, minOf(vmSource.length, openShiftIdx + 1200))
        assertTrue("openShift must validate cashier via normalizeOpenShiftCashier", openShiftBody.contains("normalizeOpenShiftCashier(cashierName)"))
        assertTrue("openShift must emit cashier_name_required when blank", openShiftBody.contains("R.string.cashier_name_required"))
        assertFalse("openShift must not fall back to literal Cashier", openShiftBody.contains("\"Cashier\""))
    }

    @Test
    fun taskC_googleDriveFallbackHasNoEnglishPlaceholdersAndProtocolConstantsExist() {
        val emptyUserResponse = GoogleDriveManager.parseDriveUserInfoResponse(200, """{"user":{}}""")
        assertTrue(emptyUserResponse.isConnected)
        assertEquals("", emptyUserResponse.email)
        assertEquals("", emptyUserResponse.displayName)

        val errorResponse = GoogleDriveManager.parseDriveUserInfoResponse(500, null)
        assertTrue(errorResponse.isConnected)
        assertEquals("", errorResponse.email)
        assertEquals("", errorResponse.displayName)

        val validResponse = GoogleDriveManager.parseDriveUserInfoResponse(
            200,
            """{"user":{"emailAddress":"owner@example.com","displayName":"Store Owner"}}"""
        )
        assertEquals("owner@example.com", validResponse.email)
        assertEquals("Store Owner", validResponse.displayName)

        val driveSource = appFile("src/main/java/com/lojia/shiftreport/util/GoogleDriveManager.kt").readText()
        assertFalse(driveSource.contains("Google Drive User"))
        assertFalse(driveSource.contains("Google Drive Connected"))

        assertEquals("anonymous", OfflineBackupManager.DEFAULT_BACKUP_OWNER_ID)
        assertEquals("CASHIER", OfflineBackupManager.DEFAULT_CASHIER_ROLE)
        assertEquals("en", LanguagePreferences.DEFAULT_PRIMARY_LANGUAGE_CODE)
        assertEquals("ar", LanguagePreferences.DEFAULT_SECONDARY_LANGUAGE_CODE)
        assertEquals(2, OfflineBackupManager.BACKUP_FORMAT_VERSION)
    }
}
