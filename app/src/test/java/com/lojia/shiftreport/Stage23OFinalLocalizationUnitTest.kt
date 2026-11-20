package com.lojia.shiftreport

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertTrue
import org.junit.Test

class Stage23OFinalLocalizationUnitTest {

    @Test
    fun allScopedFiles_haveNoLiteralTextWithLetters() {
        val violations = mutableListOf<String>()

        for (relativePath in FULL_SCOPED_FILES) {
            val file = appFile(relativePath)
            assertTrue("Expected file to exist: $relativePath", file.exists())
            val lines = file.readLines()
            var inBlockComment = false
            lines.forEachIndexed { index, rawLine ->
                val cleanedLine = stripCommentsOutsideQuotes(rawLine, inBlockComment)
                inBlockComment = cleanedLine.inBlockCommentAtEnd
                for (literal in extractStringLiterals(cleanedLine.code)) {
                    val stripped = stripTemplates(literal)
                    if (ASCII_LETTER_REGEX.containsMatchIn(stripped) && literal !in ALLOWED_INTERNAL_LITERALS) {
                        violations.add("$relativePath:${index + 1} -> \"$literal\"")
                    }
                }
            }
        }

        // 12th scoped file: DocumentEditorScreen.kt clipboard label only
        val docEditor = appFile(DOCUMENT_EDITOR_FILE)
        assertTrue("Expected file to exist: $DOCUMENT_EDITOR_FILE", docEditor.exists())
        val clipboardLines = docEditor.readLines()
            .mapIndexed { idx, line -> (idx + 1) to line }
            .filter { (_, line) -> line.contains("newPlainText(") }
        assertTrue("Expected ClipData.newPlainText call in $DOCUMENT_EDITOR_FILE", clipboardLines.isNotEmpty())
        for ((lineNumber, line) in clipboardLines) {
            for (literal in extractStringLiterals(line)) {
                val stripped = stripTemplates(literal)
                if (ASCII_LETTER_REGEX.containsMatchIn(stripped)) {
                    violations.add("$DOCUMENT_EDITOR_FILE:$lineNumber -> \"$literal\"")
                }
            }
        }

        assertTrue(
            "Found unexpected user-visible string literals with letters:\n${violations.joinToString("\n")}",
            violations.isEmpty()
        )
    }

    @Test
    fun allNewKeys_existInAllLocales() {
        val localeFiles = listOf(
            "src/main/res/values/strings.xml",
            "src/main/res/values-bn/strings.xml",
            "src/main/res/values-ar/strings.xml"
        )

        val problems = mutableListOf<String>()
        for (localePath in localeFiles) {
            val file = appFile(localePath)
            assertTrue("Expected locale file to exist: $localePath", file.exists())
            val parsed = loadStringMap(file)
            if (parsed.duplicates.isNotEmpty()) {
                problems.add("$localePath has duplicate keys: ${parsed.duplicates.joinToString()}")
            }
            val missingOrBlank = NEW_KEYS.filter { key ->
                parsed.values[key].isNullOrBlank()
            }
            if (missingOrBlank.isNotEmpty()) {
                problems.add("$localePath missing or blank keys (${missingOrBlank.size}): ${missingOrBlank.joinToString()}")
            }
        }

        assertTrue(
            "Locale verification failed:\n${problems.joinToString("\n")}",
            problems.isEmpty()
        )
    }

    private fun appFile(relativePath: String): File {
        val direct = File(relativePath)
        return if (direct.exists()) direct else File("app", relativePath)
    }

    private data class CleanedLine(val code: String, val inBlockCommentAtEnd: Boolean)

    private fun stripCommentsOutsideQuotes(line: String, startsInBlockComment: Boolean): CleanedLine {
        val out = StringBuilder()
        var inBlock = startsInBlockComment
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            val next = line.getOrNull(i + 1)
            if (inBlock) {
                if (ch == '*' && next == '/') {
                    inBlock = false
                    i += 2
                } else {
                    i++
                }
                continue
            }
            if (!inQuotes) {
                if (ch == '/' && next == '/') {
                    break
                }
                if (ch == '/' && next == '*') {
                    inBlock = true
                    i += 2
                    continue
                }
                if (ch == '"') {
                    inQuotes = true
                }
                out.append(ch)
                i++
            } else {
                out.append(ch)
                if (ch == '\\' && i + 1 < line.length) {
                    out.append(line[i + 1])
                    i += 2
                    continue
                }
                if (ch == '"') {
                    inQuotes = false
                }
                i++
            }
        }
        return CleanedLine(out.toString(), inBlock)
    }

    private fun extractStringLiterals(codeLine: String): List<String> {
        val result = mutableListOf<String>()
        var i = 0
        while (i < codeLine.length) {
            if (codeLine[i] == '"') {
                val start = i + 1
                var j = start
                while (j < codeLine.length) {
                    val c = codeLine[j]
                    if (c == '\\') {
                        j += 2
                        continue
                    }
                    if (c == '$' && codeLine.getOrNull(j + 1) == '{') {
                        j += 2
                        var depth = 1
                        var innerQuote = false
                        while (j < codeLine.length && depth > 0) {
                            val ic = codeLine[j]
                            if (innerQuote) {
                                if (ic == '\\') {
                                    j += 2
                                    continue
                                }
                                if (ic == '"') {
                                    innerQuote = false
                                }
                            } else {
                                if (ic == '"') {
                                    innerQuote = true
                                } else if (ic == '{') {
                                    depth++
                                } else if (ic == '}') {
                                    depth--
                                }
                            }
                            j++
                        }
                        continue
                    }
                    if (c == '"') {
                        break
                    }
                    j++
                }
                if (j < codeLine.length) {
                    result.add(codeLine.substring(start, j))
                }
                i = j + 1
            } else {
                i++
            }
        }
        return result
    }

    private fun stripTemplates(literal: String): String {
        return literal
            .replace(TEMPLATE_BLOCK_REGEX, "")
            .replace(TEMPLATE_IDENT_REGEX, "")
            .replace(ESCAPE_SEQ_REGEX, "")
    }

    private data class ParsedStrings(
        val values: Map<String, String>,
        val duplicates: Set<String>
    )

    private fun loadStringMap(xmlFile: File): ParsedStrings {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlFile)
        val nodes = doc.getElementsByTagName("string")
        val map = LinkedHashMap<String, String>(nodes.length)
        val duplicates = LinkedHashSet<String>()
        for (i in 0 until nodes.length) {
            val item = nodes.item(i)
            val nameAttr = item.attributes?.getNamedItem("name")?.nodeValue
            if (!nameAttr.isNullOrBlank()) {
                if (map.containsKey(nameAttr)) {
                    duplicates.add(nameAttr)
                }
                map[nameAttr] = item.textContent.orEmpty()
            }
        }
        return ParsedStrings(map, duplicates)
    }

    companion object {
        private val ASCII_LETTER_REGEX = Regex("[A-Za-z]")
        private val TEMPLATE_BLOCK_REGEX = Regex("\\$\\{[^}]*\\}")
        private val TEMPLATE_IDENT_REGEX = Regex("\\$[A-Za-z_][A-Za-z0-9_]*")
        private val ESCAPE_SEQ_REGEX = Regex("\\\\[nrtbf'\"\\\\$]")

        private const val DOCUMENT_EDITOR_FILE =
            "src/main/java/com/lojia/shiftreport/scanner/DocumentEditorScreen.kt"

        private val FULL_SCOPED_FILES = listOf(
            "src/main/java/com/lojia/shiftreport/settings/EmployeeManagementSection.kt",
            "src/main/java/com/lojia/shiftreport/report/ReportEntryTab.kt",
            "src/main/java/com/lojia/shiftreport/auth/AdminAuthDialog.kt",
            "src/main/java/com/lojia/shiftreport/settings/SettingsCommonDialogs.kt",
            "src/main/java/com/lojia/shiftreport/settings/ProfileImageCropper/ProfileImageCropperDialog.kt",
            "src/main/java/com/lojia/shiftreport/settings/ProfileScreen.kt",
            "src/main/java/com/lojia/shiftreport/settings/SettingsBackupSubSection.kt",
            "src/main/java/com/lojia/shiftreport/printer/PrinterSetupScreen.kt",
            "src/main/java/com/lojia/shiftreport/report/PosReconciliationSummary.kt",
            "src/main/java/com/lojia/shiftreport/util/NotificationHelper.kt",
            "src/main/java/com/lojia/shiftreport/util/ExportHelper.kt"
        )

        // Documented allowlist of non-user-visible internal literals across the 11 full files:
        private val ALLOWED_INTERNAL_LITERALS = setOf(
            // EmployeeManagementSection.kt
            "admin", // internal default admin username

            // ReportEntryTab.kt
            "CASH", // internal paymentMode enum/DB value
            "BANK", // internal paymentMode enum/DB value
            "dd-MM-yyyy", // SimpleDateFormat pattern
            "shift_report_dashboard", // Compose testTag
            "Day", // internal shift identifier matching DB/ViewModel
            "Night", // internal shift identifier matching DB/ViewModel
            "Cash", // internal advancePaymentType toggle state
            "Bank", // internal advancePaymentType toggle state

            // SettingsCommonDialogs.kt testTags & email validation regex
            "admin_pin_input",
            "admin_pin_confirm",
            "pin_step1_input",
            "pin_step2_input",
            "pin_step3_input",
            "pin_dialog_confirm",
            "pin_dialog_dismiss",
            "dialog_current_password",
            "dialog_new_password",
            "dialog_confirm_password",
            "btn_save_password",
            "btn_cancel_password",
            """^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$""",
            "dialog_new_email",
            "dialog_confirm_email",
            "btn_save_email",
            "btn_cancel_email",

            // ProfileImageCropperDialog.kt testTags & internal temp filenames
            "btn_close_cropper",
            "slider_cropper_zoom",
            "btn_cancel_crop",
            "profile_cropped_${'$'}{System.currentTimeMillis()}.jpg",
            "profile_cropped.jpg",
            "btn_save_crop",

            // ProfileScreen.kt internal MIME type & route key
            "image/*",
            "saved_addresses",

            // SettingsBackupSubSection.kt internal keys, testTags, date formats, fallback/token internals
            "root",
            "btn_backup_back",
            "daily",
            "weekly",
            "manual",
            "btn_danger_clear_all",
            "MMM dd, yyyy • hh:mm a",
            "store_admin@outlook.com",
            "ms_token_",
            "DELETE", // exact uppercase confirmation token required in Danger Zone check
            "input_delete_confirmation",
            "btn_confirm_delete_everything",
            "MMM dd, yyyy",

            // PrinterSetupScreen.kt internal mode keys, testTags, thermal print markup & multilingual hardware font test
            "network",
            "bluetooth",
            "btnClosePrinterSetup",
            "Lojia Test - تم اختبار الطباعة - টেস্ট", // intentional multilingual printer charset diagnostic string
            "<b>[C]${'$'}testText</b>", // ESC/POS formatting wrapper around testText
            "btnTestPrint",
            "inputPrinterIp",
            "inputPrinterPort",

            // NotificationHelper.kt notification channel IDs
            "lojia_shift_channel",
            "lojia_inventory_channel",
            "lojia_system_channel",

            // ExportHelper.kt internal directory, filenames, MIME types, and FileProvider authority suffix
            "exports",
            "Shift_Reports_${'$'}timeStamp.csv",
            "text/csv",
            "Shift_Reports_${'$'}timeStamp.pdf",
            "application/pdf",
            "${'$'}{context.packageName}.fileprovider"
        )

        private val NEW_KEYS = listOf(
            // EmployeeManagementSection.kt
            "employee_edit_subtitle_format",
            "employee_updated_toast",
            "employee_saved_toast",
            "employee_salary_per_month_format",
            "employee_edit_label",
            "employee_add_new_title",

            // ReportEntryTab.kt
            "report_advance_added_toast",
            "report_item_qty_format",
            "report_select_employee",
            "report_add_advance_button_format",

            // AdminAuthDialog.kt
            "auth_admin_confirmation",
            "auth_confirm_action",
            "auth_pin_confirmed_toast",
            "auth_too_many_attempts_format",
            "auth_invalid_pin_or_password_format",
            "auth_wrong_pin_format",
            "auth_info_banner",
            "auth_enter_admin_password_hint",
            "auth_enter_admin_pin_hint",
            "auth_pin_access_granted",
            "auth_use_admin_pin_toggle",
            "auth_use_admin_password_toggle",
            "auth_enter_more_format",

            // SettingsCommonDialogs.kt
            "settings_support_ticket_submitted",
            "settings_support_ticket_received_format",
            "settings_pin_step1_title",
            "settings_pin_step2_title",
            "settings_pin_step3_title",
            "settings_pin_step1_desc",
            "settings_pin_step2_desc",
            "settings_pin_step3_desc",
            "settings_step_of_format",
            "settings_pins_do_not_match",
            "settings_pw_rule_length",
            "settings_pw_rule_digit",
            "settings_pw_rule_upper",
            "settings_pw_requirements_error",
            "settings_change_email_title",
            "settings_change_email_subtitle",
            "settings_emails_do_not_match",
            "settings_invalid_email_error",

            // ProfileImageCropperDialog.kt
            "crop_profile_photo_title",
            "crop_unable_to_load_image",
            "crop_instructions",
            "crop_save_failed_toast",
            "crop_error_toast_format",

            // ProfileScreen.kt
            "profile_security_q1",
            "profile_security_q2",
            "profile_security_q3",
            "profile_security_q4",
            "profile_security_q5",
            "profile_store_owner",
            "profile_edit_picture_cd",
            "profile_picture_updated_toast",
            "profile_section_personal",
            "profile_full_name_label",
            "profile_userid_permanent_toast",
            "profile_userid_label",
            "profile_cannot_be_changed",
            "profile_locked_cd",
            "profile_copy_userid_cd",
            "profile_email_label",
            "profile_phone_label",
            "profile_address_label",
            "profile_add_address_hint",
            "profile_section_security",
            "profile_recovery_question_label",
            "profile_set_recovery_hint",
            "profile_auth_update_name",
            "profile_name_updated_toast",
            "profile_auth_update_email",
            "profile_email_updated_toast",
            "profile_auth_update_phone",
            "profile_phone_updated_toast",
            "profile_auth_update_recovery",
            "profile_recovery_updated_toast",
            "profile_auth_change_password",
            "profile_password_changed_toast",
            "profile_edit_full_name_title",
            "profile_first_name_label",
            "profile_first_name_required",
            "profile_last_name_label",
            "profile_last_name_required",
            "profile_full_name_info",
            "profile_edit_email_title",
            "profile_email_required_label",
            "profile_invalid_email_error",
            "profile_email_verification_note",
            "profile_edit_phone_title",
            "profile_phone_required_label",
            "profile_phone_digits_error",
            "profile_edit_recovery_title",
            "profile_answer_label",
            "profile_recovery_info",
            "profile_strength_medium",
            "profile_current_password_label",
            "profile_new_password_label",
            "profile_password_strength_format",
            "profile_confirm_password_label",
            "profile_current_password_incorrect",
            "profile_change_action",

            // SettingsBackupSubSection.kt
            "backup_section_destination",
            "backup_select_location_title",
            "backup_select_location_desc",
            "backup_loc_device_title",
            "backup_loc_device_subtitle",
            "backup_loc_gdrive_title",
            "backup_loc_gdrive_subtitle",
            "backup_loc_onedrive_title",
            "backup_loc_onedrive_subtitle",
            "backup_loc_both_title",
            "backup_loc_both_subtitle",
            "backup_section_status_actions",
            "backup_local_saved_toast",
            "backup_failed_generic",
            "backup_failed_format",
            "backup_gdrive_uploaded_toast",
            "backup_upload_failed_format",
            "backup_gdrive_fetch_failed_format",
            "backup_onedrive_uploaded_toast",
            "backup_upload_failed_generic",
            "backup_onedrive_error_format",
            "backup_onedrive_none_found_toast",
            "backup_onedrive_disconnected_toast",
            "backup_section_automatic",
            "backup_auto_desc",
            "backup_frequency_title",
            "backup_freq_daily_3am",
            "backup_freq_manual",
            "backup_section_danger_zone",
            "backup_local_archives_title",
            "backup_local_none_found",
            "backup_open_folder_files",
            "backup_connect_onedrive",
            "backup_onedrive_connect_desc",
            "backup_onedrive_email_label",
            "backup_onedrive_email_hint",
            "backup_onedrive_connected_format",
            "backup_connect_account_btn",
            "backup_gdrive_none_found",
            "backup_restore_shifts_count_format",
            "backup_restore_summary_format",
            "backup_delete_dialog_title",
            "backup_delete_dialog_warning",
            "backup_delete_dialog_instruction",
            "backup_delete_keyword",
            "backup_cleared_all_toast",
            "backup_clear_error_format",
            "backup_device_storage_title",
            "backup_local_folder_path",
            "backup_status_protected",
            "backup_status_no_backup",
            "backup_total_archives_size",
            "backup_backing_up",
            "backup_now_label",
            "backup_view_files",
            "backup_status_connected",
            "backup_status_not_connected",
            "backup_status_disconnected",
            "backup_last_cloud_sync",
            "backup_target_folder",
            "backup_appdata_space",
            "backup_connect_gdrive",
            "backup_uploading",
            "backup_onedrive_folder_path",
            "backup_size_zero_bytes",
            "backup_size_kb_format",
            "backup_size_mb_format",
            "backup_time_just_now",
            "backup_time_min_ago_format",
            "backup_time_hr_ago_format",
            "backup_time_days_ago_format",

            // PrinterSetupScreen.kt
            "printer_status_connected_format",
            "printer_status_connecting",
            "printer_status_failed",
            "printer_status_disconnected",
            "printer_wifi_default_name",
            "printer_test_printed_toast",
            "printer_print_failed_toast",
            "printer_test_print_btn",
            "printer_tab_wifi",
            "printer_tab_bluetooth",
            "printer_direct_network_header",
            "printer_port_short_label",
            "printer_stop_scan_btn",
            "printer_auto_discover_btn",
            "printer_connected_addr_toast",
            "printer_connection_timeout",
            "printer_failed_msg_toast",
            "printer_connect_btn",
            "printer_scanning_network",
            "printer_discovered_network_format",
            "printer_no_network_found",
            "printer_paired_bt_format",
            "printer_no_paired_bt_found",
            "printer_connection_failed_toast",

            // PosReconciliationSummary.kt
            "recon_pos_revenue_breakdown",
            "recon_drawer_formula",
            "recon_enter_physical_cash_hint",
            "recon_cash_over",
            "recon_cash_short",
            "recon_expected_actual_format",
            "recon_operational_tracking_title",
            "recon_receivables_note",
            "recon_pcs_count_format",

            // NotificationHelper.kt
            "notify_channel_shift_name",
            "notify_channel_shift_desc",
            "notify_channel_inventory_name",
            "notify_channel_inventory_desc",
            "notify_channel_system_name",
            "notify_channel_system_desc",

            // ExportHelper.kt
            "export_csv_header",
            "export_csv_share_title",
            "export_pdf_share_title",

            // DocumentEditorScreen.kt
            "scan_clipboard_label"
        )
    }
}
