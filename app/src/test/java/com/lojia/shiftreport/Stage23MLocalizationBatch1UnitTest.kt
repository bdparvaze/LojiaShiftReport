package com.lojia.shiftreport

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertTrue
import org.junit.Test

class Stage23MLocalizationBatch1UnitTest {

    @Test
    fun tableLiterals_areGoneFromFiles() {
        val regex = Regex("Text\\(\\s*(?:text\\s*=\\s*)?\"([^\"]*)\"")
        val violations = mutableListOf<String>()

        for (relativePath in BATCH_FILES) {
            val file = appFile(relativePath)
            assertTrue("Expected file to exist: $relativePath", file.exists())
            val content = file.readText()
            for (match in regex.findAll(content)) {
                val captured = match.groupValues[1]
                if (captured in IN_SCOPE_ENGLISH_TEXTS) {
                    violations.add("$relativePath: \"$captured\"")
                }
            }
        }

        assertTrue(
            "Found unlocalized in-scope Text literals: ${violations.joinToString()}",
            violations.isEmpty()
        )
    }

    @Test
    fun tableKeys_existInAllLocales() {
        val localeFiles = listOf(
            "src/main/res/values/strings.xml",
            "src/main/res/values-bn/strings.xml",
            "src/main/res/values-ar/strings.xml"
        )

        val missingByLocale = mutableListOf<String>()
        for (localePath in localeFiles) {
            val file = appFile(localePath)
            assertTrue("Expected locale file to exist: $localePath", file.exists())
            val keys = loadStringKeys(file)
            val missing = RESOLVED_KEYS.filterNot { it in keys }
            if (missing.isNotEmpty()) {
                missingByLocale.add("$localePath missing ${missing.joinToString()}")
            }
        }

        assertTrue(
            "Missing resolved Batch 1 keys: ${missingByLocale.joinToString("; ")}",
            missingByLocale.isEmpty()
        )
    }

    private fun appFile(relativePath: String): File {
        val direct = File(relativePath)
        return if (direct.exists()) direct else File("app", relativePath)
    }

    private fun loadStringKeys(xmlFile: File): Set<String> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlFile)
        val nodes = doc.getElementsByTagName("string")
        val keys = HashSet<String>(nodes.length)
        for (i in 0 until nodes.length) {
            val item = nodes.item(i)
            val nameAttr = item.attributes?.getNamedItem("name")?.nodeValue
            if (!nameAttr.isNullOrBlank()) {
                keys.add(nameAttr)
            }
        }
        return keys
    }

    companion object {
        private val BATCH_FILES = listOf(
            "src/main/java/com/lojia/shiftreport/settings/EmployeeManagementSection.kt",
            "src/main/java/com/lojia/shiftreport/report/ReportEntryTab.kt",
            "src/main/java/com/lojia/shiftreport/auth/AdminAuthDialog.kt",
            "src/main/java/com/lojia/shiftreport/settings/SettingsCommonDialogs.kt",
            "src/main/java/com/lojia/shiftreport/settings/ProfileImageCropper/ProfileImageCropperDialog.kt"
        )

        private val IN_SCOPE_ENGLISH_TEXTS = setOf(
            "%1\$d employees • %2\$d active",
            "Total",
            "Active",
            "Inactive",
            "Search by name or phone...",
            "Employee Name *",
            "Monthly Salary *",
            "e.g., monthly amount",
            "e.g., 1500",
            "Phone (Optional)",
            "Active Status",
            "Employee",
            "Select from %1\$d employees",
            "No employees - Add in Settings > Employees",
            "Advance Amount",
            "Cash",
            "Bank",
            "Admin Password",
            "Verifying...",
            "Confirmed ✓",
            "Retry",
            "Confirm",
            "New Email Address",
            "Confirm Email Address",
            "Save Photo",
            "Save",
            "Cancel",
            "Add Employee"
        )

        private val RESOLVED_KEYS = listOf(
            "employees_count_format",
            "total",
            "active",
            "inactive",
            "employee_search_hint",
            "employee_name_label",
            "monthly_salary_label",
            "employee_salary_hint",
            "phone_optional_label",
            "active_status_label",
            "nav_employee",
            "select_from_employees_format",
            "no_employees_add_hint",
            "advance_amount_label",
            "cash",
            "bank",
            "admin_password_label",
            "verifying_label",
            "confirmed_label",
            "retry_label",
            "confirm",
            "new_email_label",
            "confirm_email_label",
            "save_photo_label",
            "save",
            "cancel",
            "empty_employee_add_button"
        )
    }
}
