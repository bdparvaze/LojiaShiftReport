package com.lojia.shiftreport

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertTrue
import org.junit.Test

class Stage23QFullLocalizationUnitTest {

    @Test
    fun completedFiles_haveNoLiteralTextWithLetters() {
        val violations = mutableListOf<String>()

        for (relativePath in COMPLETED_FILES) {
            val file = appFile(relativePath)
            assertTrue("Expected file to exist: $relativePath", file.exists())
            val lines = file.readLines()
            var inBlockComment = false
            lines.forEachIndexed { index, rawLine ->
                val cleaned = stripCommentsOutsideQuotes(rawLine, inBlockComment)
                inBlockComment = cleaned.inBlockCommentAtEnd
                val code = cleaned.code

                checkPattern(code, TEXT_CALL_REGEX, relativePath, index + 1, violations)
                checkPattern(code, NAMED_UI_ARG_REGEX, relativePath, index + 1, violations)
            }
        }

        // DocumentEditorScreen.kt clipboard label check
        val docEditor = appFile(DOCUMENT_EDITOR_FILE)
        assertTrue("Expected file to exist: $DOCUMENT_EDITOR_FILE", docEditor.exists())
        val clipboardLines = docEditor.readLines()
            .mapIndexed { idx, line -> (idx + 1) to line }
            .filter { (_, line) -> line.contains("newPlainText(") }
        assertTrue("Expected ClipData.newPlainText call in $DOCUMENT_EDITOR_FILE", clipboardLines.isNotEmpty())
        for ((lineNumber, line) in clipboardLines) {
            var pos = 0
            while (pos < line.length) {
                val openQuote = line.indexOf('"', pos)
                if (openQuote == -1) break
                val extracted = extractBalancedQuotedContent(line, openQuote) ?: break
                val stripped = stripTemplates(extracted.content)
                if (LETTER_REGEX.containsMatchIn(stripped)) {
                    violations.add("$DOCUMENT_EDITOR_FILE:$lineNumber -> \"${extracted.content}\"")
                }
                pos = extracted.nextIndex
            }
        }

        assertTrue(
            "Found unexpected user-visible string literals with letters:\n${violations.joinToString("\n")}",
            violations.isEmpty()
        )
    }

    @Test
    fun newKeys_existInAllLocales() {
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
            val missingOrBlank = VERIFIED_STAGE23Q_KEYS.filter { key ->
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

    private fun checkPattern(
        codeLine: String,
        regex: Regex,
        relativePath: String,
        lineNumber: Int,
        violations: MutableList<String>
    ) {
        for (match in regex.findAll(codeLine)) {
            val openingQuoteIndex = codeLine.indexOf('"', match.range.first)
            val literal = if (openingQuoteIndex != -1) {
                extractBalancedQuotedContent(codeLine, openingQuoteIndex)?.content ?: match.groupValues[1]
            } else {
                match.groupValues[1]
            }
            val stripped = stripTemplates(literal)
            if (LETTER_REGEX.containsMatchIn(stripped) && literal !in ALLOWED_INTERNAL_LITERALS) {
                violations.add("$relativePath:$lineNumber -> \"$literal\"")
            }
        }
    }

    private data class QuotedExtract(val content: String, val nextIndex: Int)

    private fun extractBalancedQuotedContent(codeLine: String, openingQuoteIndex: Int): QuotedExtract? {
        if (openingQuoteIndex < 0 || openingQuoteIndex >= codeLine.length || codeLine[openingQuoteIndex] != '"') {
            return null
        }
        val start = openingQuoteIndex + 1
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
                return QuotedExtract(codeLine.substring(start, j), j + 1)
            }
            j++
        }
        return null
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
        private val TEXT_CALL_REGEX = Regex("""Text\(\s*(?:text\s*=\s*)?"([^"]*)"""")
        private val NAMED_UI_ARG_REGEX = Regex("""\b(?:text|label|title|placeholder|supportingText|contentDescription)\s*=\s*"([^"]*)"""")
        private val LETTER_REGEX = Regex("""[\p{L}&&[^\u2139]]""")
        private val TEMPLATE_BLOCK_REGEX = Regex("""\$\{[^}]*\}""")
        private val TEMPLATE_IDENT_REGEX = Regex("""\$[A-Za-z_][A-Za-z0-9_]*""")
        private val ESCAPE_SEQ_REGEX = Regex("""\\[nrtbf'"\\$]""")

        private const val DOCUMENT_EDITOR_FILE =
            "src/main/java/com/lojia/shiftreport/scanner/DocumentEditorScreen.kt"

        // All completed UI/ViewModel/Helper files from Stage 23Q + Stage 23O (11 full files)
        private val COMPLETED_FILES = listOf(
            // Stage 23Q Task B (Language, Country & Currency)
            "src/main/java/com/lojia/shiftreport/settings/RegionalPreferencesSection.kt",

            // Stage 23Q Task C (Remaining UI, Auth, Report, Scanner & Helper files)
            "src/main/java/com/lojia/shiftreport/MainActivity.kt",
            "src/main/java/com/lojia/shiftreport/settings/LanguageSettingsComponent.kt",
            "src/main/java/com/lojia/shiftreport/settings/SettingsReportSection.kt",
            "src/main/java/com/lojia/shiftreport/settings/AddEditAddressScreen.kt",
            "src/main/java/com/lojia/shiftreport/settings/GoogleMapsLocationPickerModal.kt",
            "src/main/java/com/lojia/shiftreport/settings/BusinessLogoPickerCard.kt",
            "src/main/java/com/lojia/shiftreport/settings/CashierManagementSection.kt",
            "src/main/java/com/lojia/shiftreport/settings/ProfileComponents.kt",
            "src/main/java/com/lojia/shiftreport/settings/AddCashierDialog.kt",
            "src/main/java/com/lojia/shiftreport/settings/BackupPasswordDialogs.kt",
            "src/main/java/com/lojia/shiftreport/settings/DataPreservationDialogs.kt",
            "src/main/java/com/lojia/shiftreport/settings/SavedAddressesScreen.kt",
            "src/main/java/com/lojia/shiftreport/settings/SettingsScreen.kt",
            "src/main/java/com/lojia/shiftreport/auth/AuthScreen.kt",
            "src/main/java/com/lojia/shiftreport/auth/QuickPinScreen.kt",
            "src/main/java/com/lojia/shiftreport/auth/BiometricManager.kt",
            "src/main/java/com/lojia/shiftreport/auth/AdminAuthManager.kt",
            "src/main/java/com/lojia/shiftreport/auth/LojiaAuthComponents.kt",
            "src/main/java/com/lojia/shiftreport/auth/SixDigitPinInput.kt",
            "src/main/java/com/lojia/shiftreport/report/AddEntryDialog.kt",
            "src/main/java/com/lojia/shiftreport/report/DashboardScreen.kt",
            "src/main/java/com/lojia/shiftreport/report/SecondaryLedgerTabs.kt",
            "src/main/java/com/lojia/shiftreport/report/ShiftReportArchivesTab.kt",
            "src/main/java/com/lojia/shiftreport/report/ShiftReportLedgerTabs.kt",
            "src/main/java/com/lojia/shiftreport/report/ReportBackupManager.kt",
            "src/main/java/com/lojia/shiftreport/report/ReportViewModel.kt",
            "src/main/java/com/lojia/shiftreport/report/MonthlyBreakdownComponents.kt",
            "src/main/java/com/lojia/shiftreport/report/MonthlySalesCharts.kt",
            "src/main/java/com/lojia/shiftreport/report/MonthlySalesSummaryView.kt",
            "src/main/java/com/lojia/shiftreport/report/ShiftReportComponents.kt",
            "src/main/java/com/lojia/shiftreport/report/ShiftReportScreen.kt",
            "src/main/java/com/lojia/shiftreport/ui/common/AppDrawer.kt",
            "src/main/java/com/lojia/shiftreport/ui/common/Components.kt",
            "src/main/java/com/lojia/shiftreport/ui/common/LojiaComponents.kt",
            "src/main/java/com/lojia/shiftreport/ui/common/LojiaDialog.kt",
            "src/main/java/com/lojia/shiftreport/ui/common/LojiaTextField.kt",
            "src/main/java/com/lojia/shiftreport/ui/common/PinCells.kt",
            "src/main/java/com/lojia/shiftreport/ui/common/PhoneNumberHint.kt",
            "src/main/java/com/lojia/shiftreport/ui/common/PdfPreviewDialog.kt",
            "src/main/java/com/lojia/shiftreport/util/PdfShareUtils.kt",
            "src/main/java/com/lojia/shiftreport/util/PdfReportGenerator.kt",
            "src/main/java/com/lojia/shiftreport/printer/PrinterSetupDialog.kt",
            "src/main/java/com/lojia/shiftreport/scanner/CameraXScannerView.kt",
            "src/main/java/com/lojia/shiftreport/scanner/DocumentScannerComponents.kt",
            "src/main/java/com/lojia/shiftreport/scanner/DocumentScannerDialogs.kt",
            "src/main/java/com/lojia/shiftreport/scanner/DocumentScannerScreen.kt",
            "src/main/java/com/lojia/shiftreport/scanner/DocumentScannerViewModel.kt",

            // Stage 23O 11 full files
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

        // Small documented allowlist for non-user-visible internal Compose animation/sentinel literals
        private val ALLOWED_INTERNAL_LITERALS = setOf(
            "AppNavHostTransition", // AnimatedContent label in MainActivity.kt
            "pulse",                // rememberInfiniteTransition label in GoogleMapsLocationPickerModal.kt
            "pulseRadius",          // animateFloat label in GoogleMapsLocationPickerModal.kt
            "pulseAlpha",           // animateFloat label in GoogleMapsLocationPickerModal.kt
            "pin_box_border",       // animateColorAsState label in SixDigitPinInput.kt
            "pin_box_container",    // animateColorAsState label in SixDigitPinInput.kt
            "modal_dot_size",       // animateDpAsState label in Components.kt
            "modal_dot_bg",         // animateColorAsState label in Components.kt
            "modal_dot_border",     // animateColorAsState label in Components.kt
            "shutterScale",         // animateFloatAsState label in CameraXScannerView.kt
            "error"                 // internal failure sentinel in ScannedDocument(title = "error")
        )

        // All 122 new keys added in Stage 23Q + all reused keys across Task B & Task C
        private val VERIFIED_STAGE23Q_KEYS = listOf(
            // New Stage 23Q keys (122)
            "regional_country_label",
            "regional_currency_label",
            "regional_select_country",
            "regional_select_language",
            "regional_select_currency",
            "regional_search_currency_hint",
            "application_language_header",
            "lang_rtl_badge",
            "lang_display_language",
            "lang_datastore_value_fmt",
            "lang_native_en",
            "lang_native_bn",
            "lang_native_ar",
            "settings_employees_subtitle",
            "settings_thermal_printer_title",
            "settings_thermal_printer_subtitle",
            "settings_section_pin_biometric",
            "settings_unlock_pos_pin_desc",
            "settings_tap_update_pin_desc",
            "settings_section_biometric",
            "settings_fingerprint_face_id",
            "settings_section_app_info",
            "settings_section_legal",
            "settings_section_contact_support",
            "address_tap_edit_map",
            "address_dial_code_label",
            "address_mobile_required",
            "address_select_country_region",
            "maps_locating_area",
            "maps_store_location",
            "maps_selected_area",
            "maps_pinned_store_location",
            "maps_pinned_coords_fmt",
            "maps_near_coords_fmt",
            "maps_permanent_address",
            "maps_release_to_pin",
            "maps_resolving_address",
            "maps_google_maps_brand",
            "maps_live_badge",
            "maps_pin_permanent_subtitle",
            "maps_verified_location",
            "maps_coordinates_fmt",
            "city_riyadh",
            "city_jeddah",
            "city_dubai",
            "city_dhaka",
            "city_chittagong",
            "address_tag_store",
            "address_tag_warehouse",
            "address_tag_office",
            "address_tag_branch",
            "logo_camera_error_fmt",
            "logo_take_photo_camera",
            "cashier_empty_subtitle",
            "cashier_archived_audit_desc",
            "cashier_no_archived",
            "cashier_archived_date_fmt",
            "item_description_required_fmt",
            "add_entry_item_desc_hint",
            "add_entry_total_amount_req_fmt",
            "add_entry_bill_amount_support",
            "add_entry_quantity_optional",
            "add_entry_quantity_hint",
            "add_entry_unit_price_optional",
            "add_entry_auto_filled_hint",
            "add_entry_auto_filled_arrow",
            "add_entry_btn",
            "dashboard_analytics_subtitle",
            "paid_out_total_title",
            "paid_out_entries_count_fmt",
            "paid_out_section_header",
            "paid_out_empty_title",
            "paid_out_empty_desc",
            "paid_out_qty_x_price_fmt",
            "paid_out_qty_pcs_fmt",
            "walkout_bill_singular_fmt",
            "walkout_bills_plural_fmt",
            "walkout_bills_section_header",
            "walkout_empty_desc",
            "archive_variance_label_fmt",
            "ledger_receipt_breakdown_title",
            "ledger_total_advances",
            "ledger_employer_advances_title",
            "ledger_staff_initial_fallback",
            "shift_report_number_fmt",
            "delete_shift_report_confirm_desc",
            "components_incorrect_pin_try_again",
            "pdf_starting_cash_short",
            "not_available_short",
            "pdf_variance_over_short",
            "pdf_staff_meals_persons_fmt",
            "pdf_computer_generated_footer",
            "auth_remember_me_selected_cd",
            "auth_authorized_personnel_only",
            "quick_pin_lockout_prompt",
            "quick_pin_mismatch_error",
            "quick_pin_create_title",
            "quick_pin_confirm_title",
            "quick_pin_create_subtitle",
            "quick_pin_confirm_subtitle",
            "quick_pin_lockout_countdown_fmt",
            "biometric_auth_failed",
            "biometric_auth_cancelled",
            "biometric_auth_error",
            "biometric_auth_init_error",
            "pdf_chooser_open_report",
            "pdf_chooser_share_report",
            "pdf_chooser_email_report",
            "pdf_email_subject",
            "pdf_email_body",
            "scanner_share_protected_pdf",
            "scanner_open_protected_pdf",
            "scanner_share_document",
            "scanner_open_pdf",
            "pdf_write_failed_error",
            "pdf_audited_by_fmt",
            "toast_currency_set",
            "toast_cashier_activated",
            "toast_cashier_deactivated",
            "toast_cashier_archived",
            "toast_cashier_restored",
            "err_failed_save_draft",
            "backup_shifts_ready_fmt",
            "backup_found_label",
            "monthly_week_1_label",
            "monthly_week_2_label",
            "monthly_week_3_label",
            "monthly_week_4_label",
            "monthly_week_5_fmt",
            "monthly_mada_digital_settlements",
            "monthly_salary_advance_payouts",
            "phone_hint_none_found",
            "phone_hint_error_fmt",
            "phone_hint_picker_unavailable_fmt",
            "pdf_pending_count",
            "pdf_signed_by_fmt",
            "backup_auth_cancelled",
            "drive_auth_failed",
            "drive_upload_failed",
            "drive_list_failed",
            "drive_download_failed",
            "drive_restore_complete_notif_title",
            "drive_restore_complete_notif_body",
            "sync_status_workmanager",
            "cloud_backup_success_notif_title",
            "cloud_backup_success_notif_body",
            "backup_create_failed",
            "backup_restored_notif_title",
            "backup_restored_notif_body",
            "clear_data_failed",
            "role_admin",

            // Reused keys in Stage 23Q
            "language",
            "search_country_hint",
            "profile",
            "cashiers",
            "security",
            "backup",
            "about",
            "nav_support",
            "biometric_login",
            "edit_address",
            "add_new_address",
            "save",
            "all_info_encrypted",
            "country_region_label",
            "address",
            "edit_item",
            "street_address_required",
            "street_building_district_hint",
            "apt_suite_optional",
            "address_label_optional",
            "suggestion_label",
            "contact_information_header",
            "profile_first_name_label",
            "profile_last_name_label",
            "cd_change_photo",
            "cd_hide_password",
            "cd_show_password",
            "auth_admin_confirmation",
            "auth_confirm_action",
            "item_description",
            "add",
            "total_walkout_loss",
            "search_cashier_shift",
            "gross_revenue",
            "mada_bank",
            "net_cash_1",
            "delete",
            "preview_pdf",
            "balanced",
            "zreport_over",
            "zreport_short",
            "zreport_balanced",
            "search_receipt_number",
            "total_issued_due",
            "total_collected",
            "net_outstanding_due",
            "deduct_auto_hint",
            "issued_due",
            "collected_minus",
            "net_remaining",
            "cash_payment_opt",
            "bank",
            "app_name",
            "default_shift_manager",
            "nav_reports",
            "delete_shift_report_title",
            "security_question_label",
            "cancel",
            "report_preview_title",
            "shift_closing_revenue_cert",
            "pdf_sales_summary_title",
            "cash_drawer_recon_title",
            "gross_cash_label",
            "cash_in_label",
            "cash_out_label",
            "pdf_actual_cash_count_label",
            "other_tracking_title",
            "open_printer_settings",
            "btn_export",
            "btn_share",
            "share_shift_report_title",
            "shift_morning",
            "shift_evening",
            "shift_night",
            "shift_day",
            "doc_btn_share_docx",
            "not_set",
            "google_account",
            "cashier_1"
        )
    }
}
