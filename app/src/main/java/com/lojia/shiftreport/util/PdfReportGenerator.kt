package com.lojia.shiftreport.util


import com.lojia.shiftreport.R


import android.content.ContentValues

import android.content.Context

import android.content.Intent

import android.graphics.*

import android.graphics.pdf.PdfDocument

import android.net.Uri

import android.os.Build

import android.os.Environment

import android.provider.MediaStore

import android.widget.Toast


import androidx.core.content.FileProvider

import com.lojia.shiftreport.data.AppLanguage

import com.lojia.shiftreport.data.BusinessProfile

import com.lojia.shiftreport.data.ShiftReport
import org.json.JSONArray

import java.io.File

import java.io.FileOutputStream

import java.io.OutputStream

import java.text.SimpleDateFormat
import java.util.*

object PdfReportGenerator {
    private fun getLocalizedShiftName(language: AppLanguage, shift: String): String {
        val code = language.code
        return when (shift.lowercase()) {
            "morning" -> if (code == "bn") "সকাল" else if (code == "ar") "صباحية" else "Morning"
            "evening" -> if (code == "bn") "সন্ধ্যা" else if (code == "ar") "مسائية" else "Evening"
            "night" -> if (code == "bn") "রাত" else if (code == "ar") "ليلية" else "Night"
            "day" -> if (code == "bn") "দিন" else if (code == "ar") "نهارية" else "Day"
            else -> shift
        }
    }


    private const val PAGE_WIDTH = 595 // A4 standard width in points (72 dpi)
    private const val PAGE_HEIGHT = 842 // A4 standard height in points (72 dpi)

    private data class DueCreditEntry(val receiptNo: String, val customerName: String, val amount: Long)
    private data class DueCollectionEntry(val receiptNo: String, val customerName: String, val amount: Long, val paymentMode: String)
    private data class StaffAdvanceEntry(val staffName: String, val amount: Long, val paymentMode: String)
    private data class WalkoutEntry(val tableOrOrderRef: String, val amount: Long)
    private data class PurchasedItemEntry(val itemName: String, val quantity: Double, val unitPrice: Long, val totalAmount: Long)

    private fun getLongAmount(obj: org.json.JSONObject, key: String, default: Long = 0L): Long {
        if (!obj.has(key)) return default
        val value = obj.get(key)
        if (value is Number) {
            val d = value.toDouble()
            if (value is Double && d % 1.0 != 0.0) {
                return MoneyFormat.toMinorUnits(d)
            }
            return value.toLong()
        }
        return default
    }

    private fun parseDueCreditEntries(context: Context, jsonStr: String): List<DueCreditEntry> {
        val list = mutableListOf<DueCreditEntry>()
        try {
            if (jsonStr.isBlank() || jsonStr == "[]") return list
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val rawReceipt = obj.optString("receiptNo", obj.optString("receipt", ""))
                val normReceipt = normalizeReceiptNumber(rawReceipt)
                val displayReceipt = when {
                    isUnspecifiedReceipt(normReceipt) -> context.getString(R.string.receipt_unspecified)
                    normReceipt.isNotEmpty() -> context.getString(R.string.receipt_num_tag, normReceipt)
                    else -> ""
                }
                val rawCustomer = obj.optString("customerName", "")
                val trimmedCustomer = rawCustomer.trim()
                val customerLabel = when {
                    rawCustomer.isBlank() -> displayReceipt
                    isUnspecifiedReceipt(trimmedCustomer) -> context.getString(R.string.receipt_unspecified)
                    trimmedCustomer.startsWith("receipt", ignoreCase = true) || trimmedCustomer.startsWith("#") -> {
                        val normCust = normalizeReceiptNumber(rawCustomer)
                        when {
                            isUnspecifiedReceipt(normCust) -> context.getString(R.string.receipt_unspecified)
                            normCust.isNotEmpty() -> context.getString(R.string.receipt_num_tag, normCust)
                            else -> ""
                        }
                    }
                    else -> rawCustomer
                }
                list.add(
                    DueCreditEntry(
                        receiptNo = displayReceipt,
                        customerName = customerLabel,
                        amount = getLongAmount(obj, "amount", 0L)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun parseDueCollectionEntries(context: Context, jsonStr: String): List<DueCollectionEntry> {
        val list = mutableListOf<DueCollectionEntry>()
        try {
            if (jsonStr.isBlank() || jsonStr == "[]") return list
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val rawReceipt = obj.optString("receiptNo", obj.optString("receipt", ""))
                val normReceipt = normalizeReceiptNumber(rawReceipt)
                val displayReceipt = when {
                    isUnspecifiedReceipt(normReceipt) -> context.getString(R.string.receipt_unspecified)
                    normReceipt.isNotEmpty() -> context.getString(R.string.receipt_num_tag, normReceipt)
                    else -> ""
                }
                val rawCustomer = obj.optString("customerName", "")
                val trimmedCustomer = rawCustomer.trim()
                val customerLabel = when {
                    rawCustomer.isBlank() -> displayReceipt
                    isUnspecifiedReceipt(trimmedCustomer) -> context.getString(R.string.receipt_unspecified)
                    trimmedCustomer.startsWith("receipt", ignoreCase = true) || trimmedCustomer.startsWith("#") -> {
                        val normCust = normalizeReceiptNumber(rawCustomer)
                        when {
                            isUnspecifiedReceipt(normCust) -> context.getString(R.string.receipt_unspecified)
                            normCust.isNotEmpty() -> context.getString(R.string.receipt_num_tag, normCust)
                            else -> ""
                        }
                    }
                    else -> rawCustomer
                }
                list.add(
                    DueCollectionEntry(
                        receiptNo = displayReceipt,
                        customerName = customerLabel,
                        amount = getLongAmount(obj, "amount", 0L),
                        paymentMode = obj.optString("paymentMode", obj.optString("type", "CASH"))
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun parseStaffAdvanceEntries(jsonStr: String): List<StaffAdvanceEntry> {
        val list = mutableListOf<StaffAdvanceEntry>()
        try {
            if (jsonStr.isBlank() || jsonStr == "[]") return list
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    StaffAdvanceEntry(
                        staffName = obj.optString("staffName", obj.optString("name", "Staff")),
                        amount = getLongAmount(obj, "amount", 0L),
                        paymentMode = obj.optString("paymentMode", obj.optString("type", "CASH"))
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun parseWalkoutEntries(jsonStr: String): List<WalkoutEntry> {
        val list = mutableListOf<WalkoutEntry>()
        try {
            if (jsonStr.isBlank() || jsonStr == "[]") return list
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    WalkoutEntry(
                        tableOrOrderRef = obj.optString("tableOrOrderRef", obj.optString("name", obj.optString("description", "Walkout"))),
                        amount = getLongAmount(obj, "amount", 0L)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun parsePurchasedItems(jsonStr: String): List<PurchasedItemEntry> {
        val list = mutableListOf<PurchasedItemEntry>()
        try {
            if (jsonStr.isBlank() || jsonStr == "[]") return list
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    PurchasedItemEntry(
                        itemName = obj.optString("itemName", obj.optString("name", "Item")),
                        quantity = obj.optDouble("quantity", obj.optDouble("qty", 0.0)),
                        unitPrice = getLongAmount(obj, "unitPrice", 0L),
                        totalAmount = getLongAmount(obj, "totalAmount", getLongAmount(obj, "amount", 0L))
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private data class PdfStrings(
        val title: String,
        val officialReport: String,
        val reportId: String,
        val cashier: String,
        val shift: String,
        val date: String,
        val financialBreakdown: String,
        val salesSummary: String,
        val cashSales: String,
        val cardMadaSales: String,
        val digitalWallet: String,
        val grossTotalSales: String,
        val cashDrawerReconciliation: String,
        val startingCash: String,
        val cashIn: String,
        val cashOut: String,
        val expenses: String,
        val expectedCashInDrawer: String,
        val actualCashCount: String,
        val variance: String,
        val netMadaBank: String,
        val otherTracking: String,
        val dueSales: String,
        val dueCollection: String,
        val employerAdvances: String,
        val walkoutBills: String,
        val paidOutItems: String,
        val operationalMetrics: String,
        val staffMeals: String,
        val regularMuassel: String,
        val outdoorMuassel: String,
        val receiptNo: String,
        val customer: String,
        val amount: String,
        val mode: String,
        val staffName: String,
        val item: String,
        val qty: String,
        val total: String,
        val notes: String,
        val cashierSign: String,
        val supervisorSign: String,
        val page: String,
        val computerGeneratedNotice: String
    )

    private fun getLocalizedContext(context: Context, language: AppLanguage): Context {
        val locale = try {
            java.util.Locale.forLanguageTag(language.code)
        } catch (_: Exception) {
            java.util.Locale(language.code)
        }
        val config = android.content.res.Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    private fun getPdfStrings(context: Context, language: AppLanguage): PdfStrings {
        val localizedContext = getLocalizedContext(context, language)

        return PdfStrings(
            title = localizedContext.getString(com.lojia.shiftreport.R.string.shift_closing_and_revenue_report),
            officialReport = localizedContext.getString(com.lojia.shiftreport.R.string.pdf_official_report),
            reportId = localizedContext.getString(com.lojia.shiftreport.R.string.pdf_report_id),
            cashier = localizedContext.getString(com.lojia.shiftreport.R.string.pdf_cashier),
            shift = localizedContext.getString(com.lojia.shiftreport.R.string.pdf_shift),
            date = localizedContext.getString(com.lojia.shiftreport.R.string.pdf_date),
            financialBreakdown = localizedContext.getString(com.lojia.shiftreport.R.string.pdf_financial_breakdown),
            salesSummary = localizedContext.getString(com.lojia.shiftreport.R.string.pdf_sales_summary_title),
            cashSales = localizedContext.getString(com.lojia.shiftreport.R.string.pdf_cash_sales_label),
            cardMadaSales = localizedContext.getString(com.lojia.shiftreport.R.string.pdf_card_mada_sales_label),
            digitalWallet = localizedContext.getString(com.lojia.shiftreport.R.string.pdf_digital_wallet),
            grossTotalSales = localizedContext.getString(com.lojia.shiftreport.R.string.pdf_gross_total_sales),
            cashDrawerReconciliation = localizedContext.getString(com.lojia.shiftreport.R.string.pdf_cash_reconciliation_title),
            startingCash = localizedContext.getString(com.lojia.shiftreport.R.string.pdf_starting_cash_label),
            cashIn = localizedContext.getString(com.lojia.shiftreport.R.string.pdf_cash_in_label),
            cashOut = localizedContext.getString(com.lojia.shiftreport.R.string.pdf_cash_out_label),
            expenses = localizedContext.getString(com.lojia.shiftreport.R.string.pdf_expenses),
            expectedCashInDrawer = localizedContext.getString(com.lojia.shiftreport.R.string.pdf_expected_cash_drawer_label),
            actualCashCount = localizedContext.getString(com.lojia.shiftreport.R.string.pdf_actual_cash_count_label),
            variance = localizedContext.getString(com.lojia.shiftreport.R.string.pdf_variance_label),
            netMadaBank = localizedContext.getString(com.lojia.shiftreport.R.string.pdf_net_mada_bank),
            otherTracking = localizedContext.getString(com.lojia.shiftreport.R.string.pdf_other_tracking_title),
            dueSales = localizedContext.getString(com.lojia.shiftreport.R.string.pdf_due_sales),
            dueCollection = localizedContext.getString(com.lojia.shiftreport.R.string.pdf_due_collection),
            employerAdvances = localizedContext.getString(com.lojia.shiftreport.R.string.pdf_employer_advances),
            walkoutBills = localizedContext.getString(com.lojia.shiftreport.R.string.pdf_walkout_bills),
            paidOutItems = localizedContext.getString(com.lojia.shiftreport.R.string.pdf_paid_out_items),
            operationalMetrics = localizedContext.getString(com.lojia.shiftreport.R.string.pdf_operational_metrics),
            staffMeals = localizedContext.getString(com.lojia.shiftreport.R.string.pdf_staff_meals),
            regularMuassel = localizedContext.getString(com.lojia.shiftreport.R.string.pdf_regular_muassel),
            outdoorMuassel = localizedContext.getString(com.lojia.shiftreport.R.string.pdf_outdoor_muassel),
            receiptNo = localizedContext.getString(com.lojia.shiftreport.R.string.pdf_receipt_no),
            customer = localizedContext.getString(com.lojia.shiftreport.R.string.pdf_customer),
            amount = localizedContext.getString(com.lojia.shiftreport.R.string.pdf_amount),
            mode = localizedContext.getString(com.lojia.shiftreport.R.string.pdf_mode),
            staffName = localizedContext.getString(com.lojia.shiftreport.R.string.pdf_staff_name),
            item = localizedContext.getString(com.lojia.shiftreport.R.string.pdf_item),
            qty = localizedContext.getString(com.lojia.shiftreport.R.string.pdf_qty),
            total = localizedContext.getString(com.lojia.shiftreport.R.string.pdf_total),
            notes = localizedContext.getString(com.lojia.shiftreport.R.string.pdf_notes_1),
            cashierSign = localizedContext.getString(com.lojia.shiftreport.R.string.pdf_cashier_sign_1),
            supervisorSign = localizedContext.getString(com.lojia.shiftreport.R.string.pdf_supervisor_sign),
            page = localizedContext.getString(com.lojia.shiftreport.R.string.pdf_page),
            computerGeneratedNotice = localizedContext.getString(com.lojia.shiftreport.R.string.pdf_computer_generated_notice)
        )
    }
    fun generatePresetLogoBitmap(presetKey: String, size: Int = 200): Bitmap =
        PdfLogoRenderer.generatePresetLogoBitmap(presetKey, size)

    fun getCircularBitmap(bitmap: Bitmap, diameter: Int): Bitmap =
        PdfLogoRenderer.getCircularBitmap(bitmap, diameter)

    fun loadBusinessLogoBitmap(context: Context, logoUri: String?, targetSize: Int = 200): Bitmap? =
        PdfLogoRenderer.loadBusinessLogoBitmap(context, logoUri, targetSize)

    fun drawBusinessLogoBadge(
        canvas: Canvas,
        context: Context,
        logoUri: String?,
        bizName: String,
        x: Float,
        y: Float,
        size: Float = 54f
    ) = PdfLogoRenderer.drawBusinessLogoBadge(canvas, context, logoUri, bizName, x, y, size)


    fun generateStyledQrCodeBitmap(text: String, targetPixelSize: Int = 400): Bitmap? =
        PdfQrCodeGenerator.generateStyledQrCodeBitmap(text, targetPixelSize)

    fun extractStartingCashFromNotes(report: ShiftReport): Double =
        PdfNotesParser.extractStartingCashFromNotes(report)

    fun extractStartingCashFromNotes(notes: String): Double? =
        PdfNotesParser.extractStartingCashFromNotes(notes)

    fun extractActualCashFromNotes(report: ShiftReport): Double? =
        PdfNotesParser.extractActualCashFromNotes(report)

    fun extractActualCashFromNotes(notes: String): Double? =
        PdfNotesParser.extractActualCashFromNotes(notes)

    fun extractStartingCashMinor(report: ShiftReport): Long =
        PdfNotesParser.extractStartingCashMinor(report)

    fun extractStartingCashMinor(notes: String): Long? =
        PdfNotesParser.extractStartingCashMinor(notes)

    fun extractActualCashMinor(report: ShiftReport): Long? =
        PdfNotesParser.extractActualCashMinor(report)

    fun extractActualCashMinor(notes: String): Long? =
        PdfNotesParser.extractActualCashMinor(notes)

    fun cleanDisplayNotes(notes: String): String =
        PdfNotesParser.cleanDisplayNotes(notes)


    private fun buildShiftReportQrText(
        context: Context,
        report: ShiftReport,
        businessProfile: BusinessProfile?,
        currency: String,
        language: AppLanguage = AppLanguage.fromCode(LanguagePreferences.getLanguage(context))
    ): String {
        val bizName = businessProfile?.businessName ?: context.getString(R.string.default_business_name)
        val vatNo = vatNumberForPrint(businessProfile?.vatNumber)
        val pStr = getPdfStrings(context, language)

        val startingCash = if (report.openingCash > 0L) report.openingCash else extractStartingCashMinor(report)
        val actualCashCount = if (report.closingCash > 0L) report.closingCash else extractActualCashMinor(report)
        val cashIn = report.totalDueCollectedCash
        val cashOut = report.totalCashOut
        val expectedCashInDrawer = MoneyFormat.calculateExpectedCashMinor(
            startingCash = startingCash,
            cashSales = report.grossCash,
            payIn = cashIn,
            payOut = cashOut
        )
        val variance = actualCashCount?.let { MoneyFormat.calculateVarianceMinor(it, expectedCashInDrawer) }

        val dueCreditEntries = parseDueCreditEntries(context, report.dueCreditEntriesJson)
        val displayNotes = cleanDisplayNotes(report.notes)

        return buildString {
            appendLine("=== $bizName ===")
            if (vatNo != null) {
                appendLine(context.getString(R.string.pdf_vat_id, vatNo))
            }
            appendLine("${pStr.reportId}: #${report.id.toString().padStart(6, '0')}")
            appendLine("${pStr.date}: ${DateTimeFormatUtils.formatIsoDateTime(report.dateInMillis)}")
            appendLine("${pStr.shift}: ${getLocalizedShiftName(language, report.shift)} | ${pStr.cashier}: ${report.cashierName}")
            appendLine("--------------------------------")
            appendLine("[1. SALES SUMMARY]")
            appendLine("${pStr.cashSales}: ${MoneyFormat.formatMinor(report.grossCash, currency, Locale.US)}")
            appendLine("${pStr.cardMadaSales}: ${MoneyFormat.formatMinor(report.madaPayments, currency, Locale.US)}")
            if (report.digitalWallet > 0L) {
                appendLine("${pStr.digitalWallet}: ${MoneyFormat.formatMinor(report.digitalWallet, currency, Locale.US)}")
            }
            appendLine("${pStr.grossTotalSales}: ${MoneyFormat.formatMinor(report.totalSales, currency, Locale.US)}")
            appendLine("--------------------------------")
            appendLine("[2. CASH DRAWER RECONCILIATION]")
            if (startingCash > 0L) {
                appendLine("${pStr.startingCash}: ${MoneyFormat.formatMinor(startingCash, currency, Locale.US)}")
            }
            appendLine("(+) ${pStr.cashSales}: ${MoneyFormat.formatMinor(report.grossCash, currency, Locale.US)}")
            if (cashIn > 0L) {
                appendLine("(+) ${pStr.cashIn}: ${MoneyFormat.formatMinor(cashIn, currency, Locale.US)}")
            }
            appendLine("(-) ${pStr.cashOut}: ${MoneyFormat.formatMinor(cashOut, currency, Locale.US)}")
            appendLine("${pStr.expectedCashInDrawer}: ${MoneyFormat.formatMinor(expectedCashInDrawer, currency, Locale.US)}")
            if (actualCashCount != null) {
                appendLine("${pStr.actualCashCount}: ${MoneyFormat.formatMinor(actualCashCount, currency, Locale.US)}")
                val varStr = if (variance != null) {
                    when {
                        variance == 0L -> "${MoneyFormat.formatMinor(0L, currency, Locale.US)} (Balanced)"
                        variance > 0L -> "+${MoneyFormat.formatMinor(variance, currency, Locale.US)} (Over)"
                        else -> "-${MoneyFormat.formatMinor(Math.abs(variance), currency, Locale.US)} (Short)"
                    }
                } else "N/A"
                appendLine("${pStr.variance}: $varStr")
            }
            appendLine("--------------------------------")
            appendLine("[3. OTHER TRACKING]")
            appendLine("${pStr.dueSales}: ${MoneyFormat.formatMinor(report.totalDueCredit, currency, Locale.US)} (${dueCreditEntries.size})")
            appendLine("${pStr.staffMeals}: ${report.staffMealsCount}")
            if (displayNotes.isNotBlank()) {
                appendLine("${pStr.notes}: $displayNotes")
            }
            appendLine("================================")
            appendLine(context.getString(R.string.verified_by_lojia_pos))
        }
    }

    data class ExportResult(
        val isSuccess: Boolean,
        val file: File?,
        val uri: Uri?,
        val displayPath: String,
        val errorMessage: String? = null
    )

    /**
     * Generates a comprehensive Multi-Entry Shift & Sales Summary PDF from database entries.
     */
    fun generateShiftSummaryPdf(

        context: Context,
        reports: List<ShiftReport>,
        businessProfile: BusinessProfile?,
        reportTitle: String = context.getString(R.string.monthly_sales_shift_summary_report),
        language: AppLanguage = AppLanguage.fromCode(LanguagePreferences.getLanguage(context))
    ): ExportResult {
        val pdfDocument = PdfDocument()
        var pageNum = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        val currency = MoneyFormat.resolveCurrency(businessProfile?.currency)
        val pStr = getPdfStrings(context, language)

        // Paints (100% PdfPalette tokens)
        val primaryPaint = Paint().apply {
            color = PdfPalette.PRIMARY_DARK
            isAntiAlias = true
        }
        val secondaryPaint = Paint().apply {
            color = PdfPalette.TEXT_SECONDARY
            textSize = 8.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }
        val headerTextPaint = Paint().apply {
            color = PdfPalette.ON_PRIMARY
            textSize = 15f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        val subheaderTextPaint = Paint().apply {
            color = PdfPalette.PRIMARY_CONTAINER
            textSize = 9.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }
        val titlePaint = Paint().apply {
            color = PdfPalette.TEXT_PRIMARY
            textSize = 13f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        val textPaint = Paint().apply {
            color = PdfPalette.TEXT_PRIMARY
            textSize = 9.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }
        val textBoldPaint = Paint().apply {
            color = PdfPalette.TEXT_PRIMARY
            textSize = 9.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        val linePaint = Paint().apply {
            color = PdfPalette.DIVIDER
            strokeWidth = 1f
            isAntiAlias = true
        }
        val borderPaint = Paint().apply {
            color = PdfPalette.BORDER
            strokeWidth = 1f
            isAntiAlias = true
        }
        val tableHeaderPaint = Paint().apply {
            color = PdfPalette.SURFACE_VARIANT
            isAntiAlias = true
        }
        val zebraPaint = Paint().apply {
            color = PdfPalette.BACKGROUND
            isAntiAlias = true
        }
        val totalBoxPaint = Paint().apply {
            color = PdfPalette.PRIMARY_CONTAINER
            isAntiAlias = true
        }
        val accentGreenPaint = Paint().apply {
            color = PdfPalette.SUCCESS
            textSize = 9.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }

        val margin = 36f
        var currentY = margin
        val bizName = businessProfile?.businessName ?: context.getString(R.string.default_business_name)
        val vatNo = vatNumberForPrint(businessProfile?.vatNumber)
        val phone = businessProfile?.phone.orEmpty()

        fun drawSummaryFooter() {
            val footerY = PAGE_HEIGHT - 22f
            canvas.drawLine(margin, footerY - 8f, PAGE_WIDTH - margin, footerY - 8f, linePaint)
            canvas.drawText(context.getString(R.string.lojia_pos_system_v10), margin, footerY + 2f, secondaryPaint)
            val pageStr = "${pStr.page} $pageNum"
            val pageW = secondaryPaint.measureText(pageStr)
            canvas.drawText(pageStr, PAGE_WIDTH - margin - pageW, footerY + 2f, secondaryPaint)
        }

        val colDate = margin + 8f
        val colCashier = margin + 80f
        val colShift = margin + 185f
        val colCashRight = margin + 295f
        val colMadaRight = margin + 368f
        val colExpRight = margin + 435f
        val colTotalRight = PAGE_WIDTH - margin - 8f

        fun drawSummaryTableHeader(topY: Float): Float {
            val tableHeaderRect = RectF(margin, topY, PAGE_WIDTH - margin, topY + 20f)
            canvas.drawRect(tableHeaderRect, tableHeaderPaint)
            canvas.drawLine(margin, topY + 20f, PAGE_WIDTH - margin, topY + 20f, borderPaint)

            val headerY = topY + 14f
            canvas.drawText(context.getString(R.string.date_2), colDate, headerY, textBoldPaint)
            canvas.drawText(context.getString(R.string.cashier_6), colCashier, headerY, textBoldPaint)
            canvas.drawText(context.getString(R.string.shift_2), colShift, headerY, textBoldPaint)

            val cashH = context.getString(R.string.cash_1)
            canvas.drawText(cashH, colCashRight - textBoldPaint.measureText(cashH), headerY, textBoldPaint)

            val madaH = context.getString(R.string.madacard)
            canvas.drawText(madaH, colMadaRight - textBoldPaint.measureText(madaH), headerY, textBoldPaint)

            val expH = context.getString(R.string.expenses)
            canvas.drawText(expH, colExpRight - textBoldPaint.measureText(expH), headerY, textBoldPaint)

            val totalH = context.getString(R.string.pdf_total_currency, currency)
            canvas.drawText(totalH, colTotalRight - textBoldPaint.measureText(totalH), headerY, textBoldPaint)

            return topY + 20f
        }

        // 1. Header Banner
        val headerHeight = 72f
        val headerRect = RectF(margin, currentY, PAGE_WIDTH - margin, currentY + headerHeight)
        canvas.drawRoundRect(headerRect, 8f, 8f, primaryPaint)

        val logoSize = 52f
        val logoX = PAGE_WIDTH - margin - logoSize - 12f
        val logoY = currentY + (headerHeight - logoSize) / 2f
        drawBusinessLogoBadge(canvas, context, businessProfile?.logoUri, bizName, logoX, logoY, logoSize)

        canvas.drawText(bizName, margin + 14f, currentY + 24f, headerTextPaint)
        var headerSubY = currentY + 40f
        val metaText = when (chooseHeaderMetaLabelKind(vatNo, phone)) {
            HeaderMetaLabelKind.VAT_AND_TEL -> context.getString(R.string.pdf_vat_tel, vatNo!!, phone)
            HeaderMetaLabelKind.VAT_ONLY -> context.getString(R.string.pdf_vat_only, vatNo!!)
            HeaderMetaLabelKind.TEL_ONLY -> context.getString(R.string.pdf_tel_only, phone)
            HeaderMetaLabelKind.NONE -> null
        }
        if (metaText != null) {
            canvas.drawText(metaText, margin + 14f, headerSubY, subheaderTextPaint)
            headerSubY += 14f
        }
        canvas.drawText(context.getString(R.string.pdf_address, businessProfile?.address ?: context.getString(R.string.pdf_default_address)), margin + 14f, headerSubY, subheaderTextPaint)

        // Right side badge (placed to the left of the logo)
        val dateGen = context.getString(R.string.pdf_generated_at, DateTimeFormatUtils.formatIsoDateTime())
        val textWidth = subheaderTextPaint.measureText(dateGen)
        canvas.drawText(dateGen, logoX - textWidth - 12f, currentY + 24f, subheaderTextPaint)
        val entriesCount = context.getString(R.string.pdf_total_records, reports.size)
        val entriesWidth = subheaderTextPaint.measureText(entriesCount)
        canvas.drawText(entriesCount, logoX - entriesWidth - 12f, currentY + 40f, subheaderTextPaint)

        currentY += headerHeight + 20f

        // 2. Report Subheading
        canvas.drawText(reportTitle, margin, currentY, titlePaint)
        currentY += 16f

        // 3. KPI Highlights Cards (Row of 3 metric boxes)
        val totalGrossRevenue = reports.sumOf { it.totalSales }
        val totalGrossCash = reports.sumOf { it.grossCash }
        val totalMada = reports.sumOf { it.madaPayments }
        val totalWallet = reports.sumOf { it.digitalWallet }
        val totalExp = reports.sumOf { it.totalExpenses }
        val totalNet = reports.sumOf { it.netCash }

        val cardWidth = (PAGE_WIDTH - (margin * 2) - 20f) / 3f
        val cardHeight = 44f

        // Card 1: Total Sales
        drawKpiCard(canvas, margin, currentY, cardWidth, cardHeight, context.getString(R.string.total_revenue_label), MoneyFormat.formatMinor(totalGrossRevenue, currency), PdfPalette.SUCCESS)
        // Card 2: Total Mada / Digital
        drawKpiCard(canvas, margin + cardWidth + 10f, currentY, cardWidth, cardHeight, context.getString(R.string.mada_cards_label), MoneyFormat.formatMinor(totalMada + totalWallet, currency), PdfPalette.PRIMARY)
        // Card 3: Net Cash in Drawer
        drawKpiCard(canvas, margin + (cardWidth * 2) + 20f, currentY, cardWidth, cardHeight, context.getString(R.string.net_cash_in_drawer_label), MoneyFormat.formatMinor(totalNet, currency), PdfPalette.PRIMARY_DARK)

        currentY += cardHeight + 22f

        // 4. Tabular Shift Entries Breakdown
        if (needsNewPage(currentY, 46f, PAGE_HEIGHT - 55f)) {
            drawSummaryFooter()
            pdfDocument.finishPage(page)

            pageNum++
            pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            currentY = margin
        }
        canvas.drawText(context.getString(R.string.detailed_shift_records), margin, currentY, textBoldPaint)
        currentY += 8f

        currentY = drawSummaryTableHeader(currentY)

        // Draw ALL rows with multi-page pagination
        reports.forEachIndexed { index, report ->
            if (needsNewPage(currentY, 18f, PAGE_HEIGHT - 55f)) {
                drawSummaryFooter()
                pdfDocument.finishPage(page)

                pageNum++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                currentY = margin

                val miniBox = RectF(margin, currentY, PAGE_WIDTH - margin, currentY + 24f)
                canvas.drawRoundRect(miniBox, 4f, 4f, primaryPaint)
                headerTextPaint.textSize = 10.5f
                canvas.drawText(
                    "$bizName — $reportTitle (${pStr.page} $pageNum)",
                    margin + 10f,
                    currentY + 16f,
                    headerTextPaint
                )
                headerTextPaint.textSize = 15f
                currentY += 32f
                currentY = drawSummaryTableHeader(currentY)
            }

            val rowY = currentY + 13f
            if (index % 2 == 1) {
                canvas.drawRect(RectF(margin, currentY, PAGE_WIDTH - margin, currentY + 18f), zebraPaint)
            }

            canvas.drawText(DateTimeFormatUtils.formatDateOnly(report.dateInMillis, Locale.US), colDate, rowY, textPaint)
            val truncatedCashier = if (report.cashierName.length > 15) report.cashierName.take(13) + ".." else report.cashierName
            canvas.drawText(truncatedCashier, colCashier, rowY, textPaint)
            canvas.drawText(getLocalizedShiftName(language, report.shift), colShift, rowY, textPaint)

            val digits = MoneyFormat.minorUnitDigits(currency)
            val cashValStr = MoneyFormat.toPlainDecimal(report.grossCash, digits)
            canvas.drawText(cashValStr, colCashRight - textPaint.measureText(cashValStr), rowY, textPaint)

            val madaValStr = MoneyFormat.toPlainDecimal(report.madaPayments + report.digitalWallet, digits)
            canvas.drawText(madaValStr, colMadaRight - textPaint.measureText(madaValStr), rowY, textPaint)

            val expValStr = MoneyFormat.toPlainDecimal(report.totalExpenses, digits)
            canvas.drawText(expValStr, colExpRight - textPaint.measureText(expValStr), rowY, textPaint)

            val totValStr = MoneyFormat.toPlainDecimal(report.totalSales, digits)
            canvas.drawText(totValStr, colTotalRight - textBoldPaint.measureText(totValStr), rowY, textBoldPaint)

            canvas.drawLine(margin, currentY + 18f, PAGE_WIDTH - margin, currentY + 18f, linePaint)
            currentY += 18f
        }

        // Ensure space for Totals + Operational Metrics + Signatures
        if (needsNewPage(currentY, 130f, PAGE_HEIGHT - 45f)) {
            drawSummaryFooter()
            pdfDocument.finishPage(page)

            pageNum++
            pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            currentY = margin

            val miniBox = RectF(margin, currentY, PAGE_WIDTH - margin, currentY + 24f)
            canvas.drawRoundRect(miniBox, 4f, 4f, primaryPaint)
            headerTextPaint.textSize = 10.5f
            canvas.drawText(
                "$bizName — $reportTitle (${pStr.page} $pageNum)",
                margin + 10f,
                currentY + 16f,
                headerTextPaint
            )
            headerTextPaint.textSize = 15f
            currentY += 32f
            currentY = drawSummaryTableHeader(currentY)
        }

        // Totals Footer Row
        val totalRect = RectF(margin, currentY + 4f, PAGE_WIDTH - margin, currentY + 26f)
        canvas.drawRoundRect(totalRect, 4f, 4f, totalBoxPaint)

        val totalY = currentY + 19f
        canvas.drawText(context.getString(R.string.grand_totals), colDate, totalY, textBoldPaint)

        val summaryDigits = MoneyFormat.minorUnitDigits(currency)
        val totCashStr = MoneyFormat.toPlainDecimal(totalGrossCash, summaryDigits)
        canvas.drawText(totCashStr, colCashRight - textBoldPaint.measureText(totCashStr), totalY, textBoldPaint)

        val totMadaStr = MoneyFormat.toPlainDecimal(totalMada + totalWallet, summaryDigits)
        canvas.drawText(totMadaStr, colMadaRight - textBoldPaint.measureText(totMadaStr), totalY, textBoldPaint)

        val totExpStr = MoneyFormat.toPlainDecimal(totalExp, summaryDigits)
        canvas.drawText(totExpStr, colExpRight - textBoldPaint.measureText(totExpStr), totalY, textBoldPaint)

        val totRevStr = "${MoneyFormat.toPlainDecimal(totalGrossRevenue, summaryDigits)} $currency"
        canvas.drawText(totRevStr, colTotalRight - accentGreenPaint.measureText(totRevStr), totalY, accentGreenPaint)

        currentY += 40f

        // 5. Operations & Breakdown Notes
        val totalStaffMeals = reports.sumOf { it.staffMealsCount }
        val totalMuassel = reports.sumOf { it.muasselQty }

        val opsBoxRect = RectF(margin, currentY, PAGE_WIDTH - margin, currentY + 36f)
        canvas.drawRoundRect(opsBoxRect, 6f, 6f, tableHeaderPaint)
        canvas.drawText(context.getString(R.string.pdf_operational_metrics, totalStaffMeals, totalMuassel), margin + 12f, currentY + 16f, textBoldPaint)
        
        val totalGrossRevenueDouble = totalGrossRevenue.toDouble().coerceAtLeast(1.0)
        val cashPct = ((totalGrossCash.toDouble() / totalGrossRevenueDouble) * 100).toInt()
        val madaPct = ((totalMada.toDouble() / totalGrossRevenueDouble) * 100).toInt()
        val walletPct = ((totalWallet.toDouble() / totalGrossRevenueDouble) * 100).toInt()
        canvas.drawText(context.getString(R.string.pdf_payment_split, cashPct, madaPct, walletPct), margin + 12f, currentY + 28f, secondaryPaint)

        // 6. Signatures & Official Seal
        val signY = PAGE_HEIGHT - margin - 50f
        canvas.drawLine(margin + 20f, signY + 25f, margin + 180f, signY + 25f, linePaint)
        canvas.drawText(context.getString(R.string.manager_supervisor_signature), margin + 20f, signY + 38f, secondaryPaint)

        canvas.drawLine(PAGE_WIDTH - margin - 180f, signY + 25f, PAGE_WIDTH - margin - 20f, signY + 25f, linePaint)
        canvas.drawText(context.getString(R.string.finance_auditor_official_seal), PAGE_WIDTH - margin - 180f, signY + 38f, secondaryPaint)

        // 7. Page Footer
        drawSummaryFooter()

        pdfDocument.finishPage(page)

        // Save to Storage
        val fileName = "Shift_Summary_Report_${DateTimeFormatUtils.formatFileTimestamp()}.pdf"
        return savePdfToStorage(context, pdfDocument, fileName)
    }

    /**
     * Generates an individual Shift Report PDF document following international POS and audit standards
     * (Page 1: Executive Summary & Cash Drawer Reconciliation; Page 2+: Detailed Audit Annex if data exists).
     */
    fun generateSingleShiftReportPdf(
        context: Context,
        report: ShiftReport,
        businessProfile: BusinessProfile?,
        language: AppLanguage = AppLanguage.ENGLISH
    ): ExportResult {
        PdfFontManager.init(context)
        val pStr = getPdfStrings(context, language)
        val currency = MoneyFormat.resolveCurrency(businessProfile?.currency)

        // Starting Cash & Actual Cash: prioritize dedicated ShiftReport fields, fallback to notes parser
        val startingCash = if (report.openingCash > 0L) report.openingCash else extractStartingCashMinor(report)
        val actualCashCount = if (report.closingCash > 0L) report.closingCash else extractActualCashMinor(report)
        val cashIn = report.totalDueCollectedCash
        val cashOut = report.totalCashOut
        val expectedCashInDrawer = MoneyFormat.calculateExpectedCashMinor(
            startingCash = startingCash,
            cashSales = report.grossCash,
            payIn = cashIn,
            payOut = cashOut
        )
        val variance = actualCashCount?.let { MoneyFormat.calculateVarianceMinor(it, expectedCashInDrawer) }

        val localizedPdfContext = getLocalizedContext(context, language)
        val dueCreditEntries = parseDueCreditEntries(localizedPdfContext, report.dueCreditEntriesJson)
        val dueCollectionEntries = parseDueCollectionEntries(localizedPdfContext, report.previousDueCollectionsJson)
        val staffAdvanceEntries = parseStaffAdvanceEntries(report.staffAdvancesJson)
        val walkoutEntries = parseWalkoutEntries(report.unpaidBillsJson)
        val purchasedItems = parsePurchasedItems(report.purchasedItemsJson)
        val displayNotes = cleanDisplayNotes(report.notes)

        val hasAnnex = dueCreditEntries.isNotEmpty() ||
                dueCollectionEntries.isNotEmpty() ||
                staffAdvanceEntries.isNotEmpty() ||
                walkoutEntries.isNotEmpty() ||
                purchasedItems.isNotEmpty()

        val pdfDocument = PdfDocument()
        var pageNum = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        // Centralized Paints using strictly PdfPalette tokens
        val primaryPaint = Paint().apply {
            color = PdfPalette.PRIMARY_DARK
            isAntiAlias = true
        }
        val headerTextPaint = Paint().apply {
            color = PdfPalette.ON_PRIMARY
            textSize = 13.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        val subheaderTextPaint = Paint().apply {
            color = PdfPalette.PRIMARY_CONTAINER
            textSize = 8.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }
        val sectionTitlePaint = Paint().apply {
            color = PdfPalette.TEXT_PRIMARY
            textSize = 10.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        val textPaint = Paint().apply {
            color = PdfPalette.TEXT_PRIMARY
            textSize = 9f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }
        val textBoldPaint = Paint().apply {
            color = PdfPalette.TEXT_PRIMARY
            textSize = 9f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        val secondaryPaint = Paint().apply {
            color = PdfPalette.TEXT_HINT
            textSize = 8f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }
        val linePaint = Paint().apply {
            color = PdfPalette.DIVIDER
            strokeWidth = 0.8f
            isAntiAlias = true
        }
        val cardBorderPaint = Paint().apply {
            color = PdfPalette.BORDER
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
            isAntiAlias = true
        }
        val cardBgPaint = Paint().apply {
            color = PdfPalette.BACKGROUND
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val successBgPaint = Paint().apply {
            color = PdfPalette.SUCCESS_CONTAINER
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val warningBgPaint = Paint().apply {
            color = PdfPalette.WARNING_CONTAINER
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val errorBgPaint = Paint().apply {
            color = PdfPalette.ERROR_CONTAINER
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val primaryContainerBgPaint = Paint().apply {
            color = PdfPalette.PRIMARY_CONTAINER
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val tableHeaderBgPaint = Paint().apply {
            color = PdfPalette.SURFACE_VARIANT
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val zebraBgPaint = Paint().apply {
            color = PdfPalette.BACKGROUND
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val successTextPaint = Paint().apply {
            color = PdfPalette.SUCCESS
            textSize = 9.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        val warningTextPaint = Paint().apply {
            color = PdfPalette.WARNING
            textSize = 9.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        val errorTextPaint = Paint().apply {
            color = PdfPalette.ERROR
            textSize = 9.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }

        val margin = 36f
        var currentY = margin

        // Safe text drawing helper with PdfFontManager for multilingual support
        fun drawTextSafe(
            text: String,
            x: Float,
            y: Float,
            paint: Paint,
            isBold: Boolean = false,
            targetCanvas: Canvas = canvas
        ) {
            val origTypeface = paint.typeface
            PdfFontManager.applyTypeface(paint, context, text, isBold)
            targetCanvas.drawText(text, x, y, paint)
            paint.typeface = origTypeface
        }

        fun drawFooter(targetCanvas: Canvas, curPage: Int) {
            val footerY = PAGE_HEIGHT - 22f
            targetCanvas.drawLine(margin, footerY - 8f, PAGE_WIDTH - margin, footerY - 8f, linePaint)
            val genTimeStr = "${context.getString(R.string.pdf_date)}: ${DateTimeFormatUtils.formatIsoDateTimeSeconds()}"
            drawTextSafe(genTimeStr, margin, footerY + 2f, secondaryPaint, targetCanvas = targetCanvas)

            val noticeStr = pStr.computerGeneratedNotice
            val noticeWidth = secondaryPaint.measureText(noticeStr)
            drawTextSafe(noticeStr, (PAGE_WIDTH - noticeWidth) / 2f, footerY + 2f, secondaryPaint, targetCanvas = targetCanvas)

            val pageStr = "${pStr.page} $curPage"
            val pageWidth = secondaryPaint.measureText(pageStr)
            drawTextSafe(pageStr, PAGE_WIDTH - margin - pageWidth, footerY + 2f, secondaryPaint, targetCanvas = targetCanvas)
        }

        // ==========================================
        // PAGE 1 — EXECUTIVE + CASH RECONCILIATION
        // ==========================================

        // 1. Header (Business Info, Logo, Official Badge)
        val headerHeight = 68f
        val headerRect = RectF(margin, currentY, PAGE_WIDTH - margin, currentY + headerHeight)
        canvas.drawRoundRect(headerRect, 8f, 8f, primaryPaint)

        val bizName = businessProfile?.businessName?.ifBlank { context.getString(R.string.default_business_name) }
            ?: context.getString(R.string.default_business_name)
        val vatLines = vatHeaderLines(businessProfile?.vatNumber)
        val phone = businessProfile?.phone.orEmpty()
        val address = businessProfile?.address.orEmpty()

        val logoSize = 48f
        val logoX = PAGE_WIDTH - margin - logoSize - 12f
        val logoY = currentY + (headerHeight - logoSize) / 2f
        drawBusinessLogoBadge(canvas, context, businessProfile?.logoUri, bizName, logoX, logoY, logoSize)

        drawTextSafe(bizName, margin + 14f, currentY + 22f, headerTextPaint, isBold = true)

        var headerSubY = currentY + 36f
        vatLines.forEach { vatNo ->
            drawTextSafe(context.getString(R.string.pdf_vat_only, vatNo), margin + 14f, headerSubY, subheaderTextPaint)
            headerSubY += 12f
        }
        val contactParts = mutableListOf<String>()
        if (phone.isNotBlank()) contactParts.add(context.getString(R.string.pdf_tel_only, phone))
        if (address.isNotBlank()) contactParts.add(address)
        val contactLine = contactParts.joinToString(" · ")
        if (contactLine.isNotBlank()) {
            drawTextSafe(contactLine, margin + 14f, headerSubY, subheaderTextPaint)
        }

        // Official Shift Closing Report badge pill
        val badgeText = pStr.officialReport.uppercase()
        val badgePaint = Paint().apply {
            color = PdfPalette.ON_PRIMARY
            textSize = 7.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        val badgeW = badgePaint.measureText(badgeText)
        val badgePillW = badgeW + 12f
        val badgePillH = 14f
        val badgePillX = logoX - badgePillW - 10f
        val badgePillY = currentY + 14f
        val badgeRect = RectF(badgePillX, badgePillY, badgePillX + badgePillW, badgePillY + badgePillH)
        val pillBgPaint = Paint().apply {
            color = PdfPalette.PRIMARY
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRoundRect(badgeRect, 3f, 3f, pillBgPaint)
        drawTextSafe(badgeText, badgePillX + 6f, badgePillY + 10.5f, badgePaint, isBold = true)

        currentY += headerHeight + 10f

        // 2. Shift Identity Card (Compact Card)
        val infoBoxHeight = 64f
        val infoBox = RectF(margin, currentY, PAGE_WIDTH - margin, currentY + infoBoxHeight)
        canvas.drawRoundRect(infoBox, 6f, 6f, cardBgPaint)
        canvas.drawRoundRect(infoBox, 6f, 6f, cardBorderPaint)

        // Col 1 (left)
        drawTextSafe("${pStr.reportId}: #${report.id.toString().padStart(6, '0')}", margin + 12f, currentY + 18f, textBoldPaint, isBold = true)
        drawTextSafe("${pStr.shift}: ${getLocalizedShiftName(language, report.shift)}", margin + 12f, currentY + 35f, textBoldPaint, isBold = true)
        drawTextSafe("${pStr.cashier}: ${report.cashierName}", margin + 12f, currentY + 52f, textBoldPaint, isBold = true)

        // Col 2 (middle)
        drawTextSafe("${pStr.date}: ${DateTimeFormatUtils.formatIsoDateTimeSeconds(report.dateInMillis)}", margin + 175f, currentY + 18f, textPaint)
        drawTextSafe(context.getString(R.string.official_digital_verification), margin + 175f, currentY + 35f, secondaryPaint)
        if (report.managerSignedBy?.isNotBlank() == true) {
            drawTextSafe("Audited: ${report.managerSignedBy}", margin + 175f, currentY + 52f, secondaryPaint)
        }

        // Col 3 (Right QR Container Card)
        val qrContainerSize = 54f
        val qrContainerRight = PAGE_WIDTH - margin - 6f
        val qrContainerLeft = qrContainerRight - qrContainerSize
        val qrContainerTop = currentY + (infoBoxHeight - qrContainerSize) / 2f
        val qrContainerBottom = qrContainerTop + qrContainerSize

        val qrContainerBg = RectF(qrContainerLeft, qrContainerTop, qrContainerRight, qrContainerBottom)
        canvas.drawRoundRect(qrContainerBg, 4f, 4f, Paint().apply { color = PdfPalette.SURFACE; style = Paint.Style.FILL; isAntiAlias = true })
        canvas.drawRoundRect(qrContainerBg, 4f, 4f, cardBorderPaint)

        val qrText = buildShiftReportQrText(context, report, businessProfile, currency, language)
        val qrBitmap = generateStyledQrCodeBitmap(qrText, 300)
        if (qrBitmap != null) {
            val qrImgPadding = 3f
            val qrImgSize = qrContainerSize - (qrImgPadding * 2f) - 7f
            val qrImgLeft = qrContainerLeft + (qrContainerSize - qrImgSize) / 2f
            val qrImgTop = qrContainerTop + qrImgPadding
            val srcRect = Rect(0, 0, qrBitmap.width, qrBitmap.height)
            val dstRectF = RectF(qrImgLeft, qrImgTop, qrImgLeft + qrImgSize, qrImgTop + qrImgSize)
            canvas.drawBitmap(qrBitmap, srcRect, dstRectF, null)

            val scanPaint = Paint().apply {
                color = PdfPalette.PRIMARY_DARK
                textSize = 5f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                isAntiAlias = true
            }
            val scanLabel = context.getString(R.string.scan_data_label)
            val scanW = scanPaint.measureText(scanLabel)
            drawTextSafe(scanLabel, qrContainerLeft + (qrContainerSize - scanW) / 2f, qrContainerBottom - 2f, scanPaint, isBold = true)
        }

        currentY += infoBoxHeight + 10f

        val mainBottomLimit = PAGE_HEIGHT - 38f
        fun ensureMainPageSpace(neededHeight: Float) {
            if (needsNewPage(currentY, neededHeight, mainBottomLimit)) {
                drawFooter(canvas, pageNum)
                pdfDocument.finishPage(page)

                pageNum++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                currentY = margin
            }
        }

        // Helper to draw section header pill
        fun drawSectionHeader(sectionNum: String, title: String) {
            ensureMainPageSpace(18f + 16f)
            val pillWidth = 20f
            val pillHeight = 14f
            val pillRect = RectF(margin, currentY, margin + pillWidth, currentY + pillHeight)
            canvas.drawRoundRect(pillRect, 3f, 3f, primaryPaint)
            val pillNumPaint = Paint().apply {
                color = PdfPalette.ON_PRIMARY
                textSize = 8f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                isAntiAlias = true
            }
            val numW = pillNumPaint.measureText(sectionNum)
            drawTextSafe(sectionNum, margin + (pillWidth - numW) / 2f, currentY + 10.5f, pillNumPaint, isBold = true)

            drawTextSafe(title, margin + pillWidth + 8f, currentY + 11.5f, sectionTitlePaint, isBold = true)
            currentY += 18f
        }

        // Helper to draw financial data row
        fun drawDataRow(
            label: String,
            valueStr: String,
            isBold: Boolean = false,
            isHighlight: Boolean = false,
            customValuePaint: Paint? = null,
            customBgPaint: Paint? = null,
            indent: Float = 0f,
            rowHeight: Float = 16f
        ) {
            ensureMainPageSpace(rowHeight)
            val rowRect = RectF(margin, currentY, PAGE_WIDTH - margin, currentY + rowHeight)
            if (customBgPaint != null) {
                canvas.drawRoundRect(rowRect, 3f, 3f, customBgPaint)
            } else if (isHighlight) {
                canvas.drawRoundRect(rowRect, 3f, 3f, primaryContainerBgPaint)
            } else if (isBold) {
                canvas.drawRect(rowRect, tableHeaderBgPaint)
            }
            canvas.drawLine(margin, currentY + rowHeight, PAGE_WIDTH - margin, currentY + rowHeight, linePaint)

            val lp = if (isBold) textBoldPaint else textPaint
            val vp = customValuePaint ?: if (isBold) textBoldPaint else textPaint

            drawTextSafe(label, margin + 8f + indent, currentY + (rowHeight * 0.7f), lp, isBold = isBold)
            val vWidth = vp.measureText(valueStr)
            drawTextSafe(valueStr, PAGE_WIDTH - margin - vWidth - 8f, currentY + (rowHeight * 0.7f), vp, isBold = isBold)
            currentY += rowHeight
        }

        // 3. Section 1: Sales Summary
        drawSectionHeader("1", pStr.salesSummary)
        drawDataRow(pStr.cashSales, MoneyFormat.formatMinor(report.grossCash, currency))
        drawDataRow(pStr.cardMadaSales, MoneyFormat.formatMinor(report.madaPayments, currency))
        if (report.digitalWallet > 0L) {
            drawDataRow(pStr.digitalWallet, MoneyFormat.formatMinor(report.digitalWallet, currency))
        }
        drawDataRow(pStr.grossTotalSales, MoneyFormat.formatMinor(report.totalSales, currency), isBold = true, isHighlight = true, rowHeight = 17f)

        val salesNote = context.getString(R.string.pdf_sales_exclusion_note)
        drawTextSafe(salesNote, margin + 8f, currentY + 9f, secondaryPaint)
        currentY += 13f

        // 4. Section 2: Cash Drawer Reconciliation (MOST IMPORTANT BLOCK)
        drawSectionHeader("2", pStr.cashDrawerReconciliation)
        drawDataRow(pStr.startingCash, MoneyFormat.formatMinor(startingCash, currency))
        drawDataRow("(+) ${pStr.cashSales}", "+ " + MoneyFormat.formatMinor(report.grossCash, currency))
        if (cashIn > 0L) {
            drawDataRow("(+) ${pStr.cashIn}", "+ " + MoneyFormat.formatMinor(cashIn, currency))
        }
        val staffAdvancesCashAmount = report.totalStaffAdvancesCash

        if (report.totalExpenses > 0L) {
            drawDataRow("   • ${pStr.expenses}", "- " + MoneyFormat.formatMinor(report.totalExpenses, currency), indent = 6f)
        }
        if (staffAdvancesCashAmount > 0L) {
            drawDataRow("   • ${pStr.employerAdvances}", "- " + MoneyFormat.formatMinor(staffAdvancesCashAmount, currency), indent = 6f)
        }
        if (report.totalPurchasedCash > 0L) {
            drawDataRow("   • ${pStr.paidOutItems}", "- " + MoneyFormat.formatMinor(report.totalPurchasedCash, currency), indent = 6f)
        }
        drawDataRow("(-) ${pStr.cashOut}", "- " + MoneyFormat.formatMinor(cashOut, currency), isBold = true)

        // Expected Cash in Drawer
        val expectedPaint = Paint().apply {
            color = PdfPalette.PRIMARY_DARK
            textSize = 9.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        drawDataRow(pStr.expectedCashInDrawer, MoneyFormat.formatMinor(expectedCashInDrawer, currency), isBold = true, isHighlight = true, customValuePaint = expectedPaint, rowHeight = 18f)

        // Actual Cash Count & Variance
        if (actualCashCount != null) {
            drawDataRow(pStr.actualCashCount, MoneyFormat.formatMinor(actualCashCount, currency), isBold = true, rowHeight = 17f)
            val varianceStr = when {
                variance == null -> "N/A"
                variance == 0L -> "${MoneyFormat.formatMinor(0L, currency)} (Balanced)"
                variance > 0L -> "+${MoneyFormat.formatMinor(variance, currency)} (Over)"
                else -> "-${MoneyFormat.formatMinor(Math.abs(variance), currency)} (Short)"
            }
            val varPaint = when {
                variance == null -> textPaint
                variance == 0L -> successTextPaint
                variance > 0L -> warningTextPaint
                else -> errorTextPaint
            }
            val varBgPaint = when {
                variance == null -> null
                variance == 0L -> successBgPaint
                variance > 0L -> warningBgPaint
                else -> errorBgPaint
            }
            drawDataRow(pStr.variance, varianceStr, isBold = true, customValuePaint = varPaint, customBgPaint = varBgPaint, rowHeight = 18f)
        } else {
            drawDataRow(pStr.actualCashCount, "[ _______________________ ]", isBold = false)
            drawDataRow(pStr.variance, "[ _______________________ ] (Pending Count)", isBold = false)
        }

        currentY += 8f

        // 5. Section 3: Operational Metrics & Key Totals
        drawSectionHeader("3", pStr.otherTracking)

        val dueCountStr = if (dueCreditEntries.isNotEmpty()) " (${dueCreditEntries.size} ${pStr.customer})" else ""
        drawDataRow("${pStr.dueSales}$dueCountStr", MoneyFormat.formatMinor(report.totalDueCredit, currency))
        drawDataRow(pStr.staffMeals, "${report.staffMealsCount}x")
        if (report.muasselQty > 0 || report.outdoorShishaQty > 0) {
            drawDataRow("${pStr.regularMuassel} / ${pStr.outdoorMuassel}", "${report.muasselQty.toInt()} / ${report.outdoorShishaQty.toInt()}")
        }
        if (report.totalUnpaidLoss > 0L) {
            drawDataRow(pStr.walkoutBills, MoneyFormat.formatMinor(report.totalUnpaidLoss, currency))
        }

        if (displayNotes.isNotBlank()) {
            currentY += 4f
            ensureMainPageSpace(18f)
            val noteBox = RectF(margin, currentY, PAGE_WIDTH - margin, currentY + 18f)
            canvas.drawRoundRect(noteBox, 3f, 3f, cardBgPaint)
            canvas.drawRoundRect(noteBox, 3f, 3f, cardBorderPaint)
            drawTextSafe("${pStr.notes}: $displayNotes", margin + 8f, currentY + 12f, textPaint)
            currentY += 22f
        } else {
            currentY += 8f
        }

        // 7. Signature Area
        val signY = PAGE_HEIGHT - margin - 48f
        if (needsNewPage(currentY, 12f, signY)) {
            drawFooter(canvas, pageNum)
            pdfDocument.finishPage(page)

            pageNum++
            pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            currentY = margin
        }
        canvas.drawLine(margin + 16f, signY, margin + 180f, signY, linePaint)
        drawTextSafe("${pStr.cashierSign}: ${report.cashierName}", margin + 16f, signY + 13f, textBoldPaint, isBold = true)

        canvas.drawLine(PAGE_WIDTH - margin - 180f, signY, PAGE_WIDTH - margin - 16f, signY, linePaint)
        drawTextSafe(pStr.supervisorSign, PAGE_WIDTH - margin - 180f, signY + 13f, textBoldPaint, isBold = true)
        if (report.managerSignedBy?.isNotBlank() == true) {
            drawTextSafe("Signed: ${report.managerSignedBy}", PAGE_WIDTH - margin - 180f, signY + 24f, secondaryPaint)
        }

        // Page 1 Footer
        drawFooter(canvas, pageNum)
        pdfDocument.finishPage(page)

        // ==========================================
        // PAGE 2+ — DETAILED AUDIT ANNEX
        // (Only generated if detail data exists)
        // ==========================================
        if (hasAnnex) {
            pageNum++
            pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            currentY = margin

            val annexHeaderPaint = Paint().apply {
                color = PdfPalette.ON_PRIMARY
                textSize = 9.5f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                isAntiAlias = true
            }

            fun drawAnnexTopBanner() {
                val bannerH = 26f
                val bannerRect = RectF(margin, currentY, PAGE_WIDTH - margin, currentY + bannerH)
                canvas.drawRoundRect(bannerRect, 4f, 4f, primaryPaint)

                val bannerTitle = "$bizName — ${pStr.officialReport}: Audit Annex (${pStr.page} $pageNum)"
                drawTextSafe(bannerTitle, margin + 10f, currentY + 17f, annexHeaderPaint, isBold = true)

                val bannerMeta = "${pStr.reportId}: #${report.id.toString().padStart(6, '0')}"
                val metaW = subheaderTextPaint.measureText(bannerMeta)
                drawTextSafe(bannerMeta, PAGE_WIDTH - margin - metaW - 10f, currentY + 17f, subheaderTextPaint)

                currentY += bannerH + 12f
            }

            val annexBottomLimit = PAGE_HEIGHT - 38f
            fun ensureAnnexSpace(neededHeight: Float, reDrawHeader: (() -> Unit)? = null) {
                if (needsNewPage(currentY, neededHeight, annexBottomLimit)) {
                    drawFooter(canvas, pageNum)
                    pdfDocument.finishPage(page)

                    pageNum++
                    pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    currentY = margin

                    drawAnnexTopBanner()
                    reDrawHeader?.invoke()
                }
            }

            drawAnnexTopBanner()

            fun drawAnnexTable(
                title: String,
                headers: List<String>,
                colXOffsets: List<Float>,
                rows: List<List<String>>,
                totalAmountStr: String
            ) {
                if (rows.isEmpty()) return
                ensureAnnexSpace(48f)

                drawTextSafe(title, margin, currentY + 11f, sectionTitlePaint, isBold = true)
                currentY += 16f

                fun drawTableHeader() {
                    val thRect = RectF(margin, currentY, PAGE_WIDTH - margin, currentY + 16f)
                    canvas.drawRect(thRect, primaryPaint)
                    headers.forEachIndexed { i, h ->
                        val isLast = (i == headers.size - 1)
                        if (isLast) {
                            val w = annexHeaderPaint.measureText(h)
                            drawTextSafe(h, PAGE_WIDTH - margin - w - 8f, currentY + 11.5f, annexHeaderPaint, isBold = true)
                        } else {
                            drawTextSafe(h, colXOffsets[i], currentY + 11.5f, annexHeaderPaint, isBold = true)
                        }
                    }
                    currentY += 16f
                }

                drawTableHeader()

                rows.forEachIndexed { idx, row ->
                    ensureAnnexSpace(16f) { drawTableHeader() }
                    val rowRect = RectF(margin, currentY, PAGE_WIDTH - margin, currentY + 16f)
                    if (idx % 2 == 1) {
                        canvas.drawRect(rowRect, zebraBgPaint)
                    }
                    row.forEachIndexed { i, cell ->
                        val isLast = (i == row.size - 1)
                        if (isLast) {
                            val w = textBoldPaint.measureText(cell)
                            drawTextSafe(cell, PAGE_WIDTH - margin - w - 8f, currentY + 11.5f, textBoldPaint, isBold = true)
                        } else {
                            drawTextSafe(cell, colXOffsets[i], currentY + 11.5f, textPaint)
                        }
                    }
                    canvas.drawLine(margin, currentY + 16f, PAGE_WIDTH - margin, currentY + 16f, linePaint)
                    currentY += 16f
                }

                // Subtotal row
                ensureAnnexSpace(17f) { drawTableHeader() }
                val subtotalRect = RectF(margin, currentY, PAGE_WIDTH - margin, currentY + 17f)
                canvas.drawRect(subtotalRect, tableHeaderBgPaint)
                drawTextSafe("${pStr.total}:", colXOffsets[0], currentY + 12f, textBoldPaint, isBold = true)
                val totW = successTextPaint.measureText(totalAmountStr)
                drawTextSafe(totalAmountStr, PAGE_WIDTH - margin - totW - 8f, currentY + 12f, successTextPaint, isBold = true)
                canvas.drawLine(margin, currentY + 17f, PAGE_WIDTH - margin, currentY + 17f, linePaint)
                currentY += 25f
            }

            // Table 1: Due Sales (Credit Entries)
            drawAnnexTable(
                title = pStr.dueSales,
                headers = listOf(pStr.receiptNo, pStr.customer, pStr.amount),
                colXOffsets = listOf(margin + 8f, margin + 120f),
                rows = dueCreditEntries.map { listOf(it.receiptNo, it.customerName, MoneyFormat.formatMinor(it.amount, currency)) },
                totalAmountStr = MoneyFormat.formatMinor(dueCreditEntries.sumOf { it.amount }, currency)
            )

            // Table 2: Due Collection
            drawAnnexTable(
                title = pStr.dueCollection,
                headers = listOf(pStr.receiptNo, pStr.customer, pStr.mode, pStr.amount),
                colXOffsets = listOf(margin + 8f, margin + 110f, margin + 280f),
                rows = dueCollectionEntries.map { listOf(it.receiptNo, it.customerName, it.paymentMode, MoneyFormat.formatMinor(it.amount, currency)) },
                totalAmountStr = MoneyFormat.formatMinor(dueCollectionEntries.sumOf { it.amount }, currency)
            )

            // Table 3: Staff / Employer Advances
            drawAnnexTable(
                title = pStr.employerAdvances,
                headers = listOf(pStr.staffName, pStr.mode, pStr.amount),
                colXOffsets = listOf(margin + 8f, margin + 180f),
                rows = staffAdvanceEntries.map { listOf(it.staffName, it.paymentMode, MoneyFormat.formatMinor(it.amount, currency)) },
                totalAmountStr = MoneyFormat.formatMinor(staffAdvanceEntries.sumOf { it.amount }, currency)
            )

            // Table 4: Walkout Bills
            drawAnnexTable(
                title = pStr.walkoutBills,
                headers = listOf(pStr.customer, pStr.amount),
                colXOffsets = listOf(margin + 8f),
                rows = walkoutEntries.map { listOf(it.tableOrOrderRef, MoneyFormat.formatMinor(it.amount, currency)) },
                totalAmountStr = MoneyFormat.formatMinor(walkoutEntries.sumOf { it.amount }, currency)
            )

            // Table 5: Paid Out Items / Purchases
            drawAnnexTable(
                title = pStr.paidOutItems,
                headers = listOf(pStr.item, pStr.qty, pStr.amount, pStr.total),
                colXOffsets = listOf(margin + 8f, margin + 190f, margin + 280f),
                rows = purchasedItems.map {
                    listOf(
                        it.itemName,
                        if (it.quantity > 0) "${it.quantity.toInt()}x" else "—",
                        if (it.unitPrice > 0L) MoneyFormat.formatMinor(it.unitPrice, currency) else "—",
                        MoneyFormat.formatMinor(it.totalAmount, currency)
                    )
                },
                totalAmountStr = MoneyFormat.formatMinor(purchasedItems.sumOf { it.totalAmount }, currency)
            )

            drawFooter(canvas, pageNum)
            pdfDocument.finishPage(page)
        }

        val fileName = "Shift_Closing_Report_${report.cashierName.replace(" ", "_")}_${report.shift}_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.pdf"
        return savePdfToStorage(context, pdfDocument, fileName)
    }

    /**
     * Saves the generated PdfDocument to device storage (Downloads / Documents) and returns export details.
     */
    private fun savePdfToStorage(
        context: Context,
        pdfDocument: PdfDocument,
        fileName: String
    ): ExportResult {
        var outputStream: OutputStream? = null
        var targetFile: File? = null
        var fileUri: Uri? = null
        var displayPath = fileName

        try {
            val reportsDir = PdfShareUtils.getAppReportsDirectory(context)
            targetFile = File(reportsDir, fileName)
            outputStream = FileOutputStream(targetFile)
            displayPath = targetFile.absolutePath
            fileUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                targetFile
            )

            outputStream?.let {
                pdfDocument.writeTo(it)
                it.flush()
            }
            pdfDocument.close()

            return ExportResult(
                isSuccess = true,
                file = targetFile,
                uri = fileUri,
                displayPath = displayPath
            )
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                pdfDocument.close()
            } catch (_: Exception) {}
            return ExportResult(
                isSuccess = false,
                file = null,
                uri = null,
                displayPath = "",
                errorMessage = e.localizedMessage ?: "Failed to write PDF to storage"
            )
        } finally {
            try {
                outputStream?.close()
            } catch (_: Exception) {}
        }
    }

    private fun drawKpiCard(
        canvas: Canvas,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        title: String,
        value: String,
        accentColor: Int
    ) {
        val rect = RectF(x, y, x + width, y + height)
        val bgPaint = Paint().apply {
            color = PdfPalette.BACKGROUND
            isAntiAlias = true
        }
        val borderPaint = Paint().apply {
            color = PdfPalette.DIVIDER
            style = Paint.Style.STROKE
            strokeWidth = 1f
            isAntiAlias = true
        }
        val accentBarPaint = Paint().apply {
            color = accentColor
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val titlePaint = Paint().apply {
            color = PdfPalette.TEXT_HINT
            textSize = 7.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        val valuePaint = Paint().apply {
            color = accentColor
            textSize = 10.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }

        canvas.drawRoundRect(rect, 6f, 6f, bgPaint)
        canvas.drawRoundRect(rect, 6f, 6f, borderPaint)
        canvas.drawRoundRect(RectF(x, y + 4f, x + 3.5f, y + height - 4f), 2f, 2f, accentBarPaint)

        canvas.drawText(title, x + 10f, y + 16f, titlePaint)
        canvas.drawText(value, x + 10f, y + 33f, valuePaint)
    }

    /**
     * Opens the exported PDF with the system viewer or any installed PDF application.
     */
    fun openPdfFile(context: Context, uri: Uri) = PdfShareUtils.openPdfFile(context, uri)

    /**
     * Shares the exported PDF file via Intent chooser.
     */
    fun sharePdfFile(context: Context, uri: Uri, title: String = "Shift Sales Report PDF") =
        PdfShareUtils.sharePdfFile(context, uri, title)

    /**
     * Sends the exported PDF file as an email attachment using Intent.ACTION_SEND.
     */
    fun sendEmailWithPdf(context: Context, pdfUri: Uri, recipientEmail: String = "") =
        PdfShareUtils.sendEmailWithPdf(context, pdfUri, recipientEmail)

    /**
     * Shares the exported PDF file directly to WhatsApp, falling back to a generic chooser if not installed.
     */
    fun sharePdfToWhatsApp(context: Context, pdfUri: Uri, message: String = "") =
        PdfShareUtils.sharePdfToWhatsApp(context, pdfUri, message)
}
