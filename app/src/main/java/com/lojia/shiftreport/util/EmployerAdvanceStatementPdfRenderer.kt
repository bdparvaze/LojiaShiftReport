package com.lojia.shiftreport.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.BidiFormatter
import android.text.TextUtils
import android.view.View
import androidx.core.content.FileProvider
import com.lojia.shiftreport.R
import com.lojia.shiftreport.data.AppLanguage
import com.lojia.shiftreport.data.BusinessProfile
import com.lojia.shiftreport.report.EmployerAdvanceHistoryEntry
import com.lojia.shiftreport.report.EmployerAdvanceStatement
import com.lojia.shiftreport.report.buildStatementPdfFileName
import com.lojia.shiftreport.report.isBankAdvanceMode
import com.lojia.shiftreport.report.isCashAdvanceMode
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

internal object EmployerAdvanceStatementPdfRenderer {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 36f
    private const val CONTENT_WIDTH = PAGE_WIDTH - (MARGIN * 2f)
    private const val PAGE_BOTTOM_LIMIT = PAGE_HEIGHT - 44f

    private enum class ColAlign { START, END }

    private data class ColumnSpec(
        val offset: Float,
        val width: Float,
        val align: ColAlign
    )

    private val ENTRY_COLUMNS = listOf(
        ColumnSpec(offset = 0f, width = 36f, align = ColAlign.START),    // Serial
        ColumnSpec(offset = 36f, width = 114f, align = ColAlign.START),  // Date
        ColumnSpec(offset = 150f, width = 76f, align = ColAlign.START),  // Mode
        ColumnSpec(offset = 226f, width = 122f, align = ColAlign.END),   // Amount
        ColumnSpec(offset = 348f, width = 175f, align = ColAlign.START)  // Cashier
    )

    fun generate(
        context: Context,
        statement: EmployerAdvanceStatement,
        businessProfile: BusinessProfile?,
        periodLabel: String,
        language: AppLanguage,
        zone: ZoneId = ZoneId.systemDefault(),
        nowMillis: Long = System.currentTimeMillis()
    ): PdfReportGenerator.ExportResult {
        PdfFontManager.init(context)

        val locale = Locale.forLanguageTag(language.code)
        val localizedContext = createLocalizedContext(context, locale)
        val isRtl = TextUtils.getLayoutDirectionFromLocale(locale) == View.LAYOUT_DIRECTION_RTL
        val bidi = BidiFormatter.getInstance(isRtl)
        val intFmt = NumberFormat.getIntegerInstance(locale).apply { isGroupingUsed = false }

        val currency = MoneyFormat.resolveCurrency(businessProfile?.currency)
        val businessName = businessProfile?.businessName?.trim()?.takeIf { it.isNotEmpty() }
        val logoBitmap = loadLogoIfPresent(context, businessProfile?.logoUri)

        val titleStr = localizedContext.getString(R.string.advance_statement_title)
        val periodPrefix = localizedContext.getString(R.string.statement_period_label)
        val generatedPrefix = localizedContext.getString(R.string.statement_generated_at_label)
        val generatedValue = Instant.ofEpochMilli(nowMillis)
            .atZone(zone)
            .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(locale))
        val entryDateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)

        val cashStr = localizedContext.getString(R.string.cash)
        val bankStr = localizedContext.getString(R.string.bank)
        val colHeaders = listOf(
            localizedContext.getString(R.string.statement_col_serial),
            localizedContext.getString(R.string.date),
            localizedContext.getString(R.string.pdf_mode),
            localizedContext.getString(R.string.amount),
            localizedContext.getString(R.string.cashier)
        )
        val emptyMsg = localizedContext.getString(R.string.statement_empty_period_msg)
        val otherTitle = localizedContext.getString(R.string.advance_other_bucket_title)
        val otherSubtitle = localizedContext.getString(R.string.advance_other_bucket_subtitle)
        val salaryLabel = localizedContext.getString(R.string.advance_monthly_salary_label)
        val advancesLabel = localizedContext.getString(R.string.advance_taken_label)
        val remainingLabel = localizedContext.getString(R.string.advance_remaining_label)
        val excessLabel = localizedContext.getString(R.string.advance_excess_label)
        val summaryTitle = localizedContext.getString(R.string.statement_summary_totals_title)
        val totalAdvancesLabel = localizedContext.getString(R.string.ledger_total_advances)
        val totalLabel = localizedContext.getString(R.string.total)
        val computerNotice = localizedContext.getString(R.string.pdf_computer_generated_footer)
        val pageLabel = localizedContext.getString(R.string.pdf_page)

        val totalPages = renderPass(
            context = context,
            localizedContext = localizedContext,
            pdfDocument = null,
            totalPagesKnown = 1,
            statement = statement,
            businessName = businessName,
            logoBitmap = logoBitmap,
            currency = currency,
            locale = locale,
            isRtl = isRtl,
            bidi = bidi,
            intFmt = intFmt,
            zone = zone,
            titleStr = titleStr,
            periodLine = "$periodPrefix: $periodLabel",
            generatedLine = "$generatedPrefix: $generatedValue",
            entryDateFormatter = entryDateFormatter,
            cashStr = cashStr,
            bankStr = bankStr,
            colHeaders = colHeaders,
            emptyMsg = emptyMsg,
            otherTitle = otherTitle,
            otherSubtitle = otherSubtitle,
            salaryLabel = salaryLabel,
            advancesLabel = advancesLabel,
            remainingLabel = remainingLabel,
            excessLabel = excessLabel,
            summaryTitle = summaryTitle,
            totalAdvancesLabel = totalAdvancesLabel,
            totalLabel = totalLabel,
            computerNotice = computerNotice,
            pageLabel = pageLabel
        )

        val pdfDocument = PdfDocument()
        renderPass(
            context = context,
            localizedContext = localizedContext,
            pdfDocument = pdfDocument,
            totalPagesKnown = totalPages,
            statement = statement,
            businessName = businessName,
            logoBitmap = logoBitmap,
            currency = currency,
            locale = locale,
            isRtl = isRtl,
            bidi = bidi,
            intFmt = intFmt,
            zone = zone,
            titleStr = titleStr,
            periodLine = "$periodPrefix: $periodLabel",
            generatedLine = "$generatedPrefix: $generatedValue",
            entryDateFormatter = entryDateFormatter,
            cashStr = cashStr,
            bankStr = bankStr,
            colHeaders = colHeaders,
            emptyMsg = emptyMsg,
            otherTitle = otherTitle,
            otherSubtitle = otherSubtitle,
            salaryLabel = salaryLabel,
            advancesLabel = advancesLabel,
            remainingLabel = remainingLabel,
            excessLabel = excessLabel,
            summaryTitle = summaryTitle,
            totalAdvancesLabel = totalAdvancesLabel,
            totalLabel = totalLabel,
            computerNotice = computerNotice,
            pageLabel = pageLabel
        )

        val statementWord = localizedContext.getString(R.string.advance_statement_file_prefix)
        val fileName = buildStatementPdfFileName(
            statementWord = statementWord,
            businessName = businessName,
            range = statement.range,
            nowMillis = nowMillis,
            zone = zone
        )
        return writeTempCachePdf(context, pdfDocument, fileName)
    }

    private fun createLocalizedContext(context: Context, locale: Locale): Context {
        val config = android.content.res.Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    private fun loadLogoIfPresent(context: Context, logoUri: String?): Bitmap? {
        if (logoUri.isNullOrBlank()) return null
        val loaded = PdfLogoRenderer.loadBusinessLogoBitmap(context, logoUri, 250) ?: return null
        return PdfLogoRenderer.getCircularBitmap(loaded, 120)
    }

    @Suppress("LongParameterList")
    private fun renderPass(
        context: Context,
        localizedContext: Context,
        pdfDocument: PdfDocument?,
        totalPagesKnown: Int,
        statement: EmployerAdvanceStatement,
        businessName: String?,
        logoBitmap: Bitmap?,
        currency: String,
        locale: Locale,
        isRtl: Boolean,
        bidi: BidiFormatter,
        intFmt: NumberFormat,
        zone: ZoneId,
        titleStr: String,
        periodLine: String,
        generatedLine: String,
        entryDateFormatter: DateTimeFormatter,
        cashStr: String,
        bankStr: String,
        colHeaders: List<String>,
        emptyMsg: String,
        otherTitle: String,
        otherSubtitle: String,
        salaryLabel: String,
        advancesLabel: String,
        remainingLabel: String,
        excessLabel: String,
        summaryTitle: String,
        totalAdvancesLabel: String,
        totalLabel: String,
        computerNotice: String,
        pageLabel: String
    ): Int {
        val primaryDarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = PdfPalette.PRIMARY_DARK
            style = Paint.Style.FILL
        }
        val surfaceBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = PdfPalette.BACKGROUND
            style = Paint.Style.FILL
        }
        val primaryContainerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = PdfPalette.PRIMARY_CONTAINER
            style = Paint.Style.FILL
        }
        val tableHeaderBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = PdfPalette.SURFACE_VARIANT
            style = Paint.Style.FILL
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = PdfPalette.BORDER
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
        }
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = PdfPalette.DIVIDER
            strokeWidth = 0.8f
        }
        val headerTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = PdfPalette.ON_PRIMARY
            textSize = 13.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val headerSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = PdfPalette.PRIMARY_CONTAINER
            textSize = 9f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val sectionTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = PdfPalette.TEXT_PRIMARY
            textSize = 10.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val tableHeaderTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = PdfPalette.ON_PRIMARY
            textSize = 8.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = PdfPalette.TEXT_PRIMARY
            textSize = 8.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val bodyBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = PdfPalette.TEXT_PRIMARY
            textSize = 8.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val secondaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = PdfPalette.TEXT_SECONDARY
            textSize = 8f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val successPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = PdfPalette.SUCCESS
            textSize = 8.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val errorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = PdfPalette.ERROR
            textSize = 8.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        var pageNum = 1
        var currentPage: PdfDocument.Page? = pdfDocument?.startPage(
            PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create()
        )
        var canvas: Canvas? = currentPage?.canvas
        var currentY = MARGIN

        fun fitAndWrapText(raw: String, paint: Paint, maxWidth: Float, isBold: Boolean): String {
            PdfFontManager.applyTypeface(paint, context, raw, isBold)
            val safeMax = maxWidth.coerceAtLeast(16f)
            if (paint.measureText(raw) <= safeMax) return bidi.unicodeWrap(raw)
            val ellipsis = "…"
            val ellipsisW = paint.measureText(ellipsis)
            var end = raw.length
            while (end > 0 && paint.measureText(raw, 0, end) + ellipsisW > safeMax) {
                end--
            }
            val clipped = if (end > 0) raw.substring(0, end) + ellipsis else ellipsis
            return bidi.unicodeWrap(clipped)
        }

        fun drawAlignedText(
            raw: String,
            x: Float,
            y: Float,
            paint: Paint,
            align: Paint.Align,
            isBold: Boolean,
            maxWidth: Float? = null
        ) {
            val c = canvas ?: return
            val origAlign = paint.textAlign
            val origTypeface = paint.typeface
            paint.textAlign = align
            val textToDraw = if (maxWidth != null) {
                fitAndWrapText(raw, paint, maxWidth, isBold)
            } else {
                PdfFontManager.applyTypeface(paint, context, raw, isBold)
                bidi.unicodeWrap(raw)
            }
            c.drawText(textToDraw, x, y, paint)
            paint.textAlign = origAlign
            paint.typeface = origTypeface
        }

        fun drawFooter(curPage: Int) {
            val c = canvas ?: return
            val footerY = PAGE_HEIGHT - 22f
            c.drawLine(MARGIN, footerY - 8f, PAGE_WIDTH - MARGIN, footerY - 8f, dividerPaint)

            val pageNumStr = localizedContext.getString(
                R.string.statement_page_of_fmt,
                intFmt.format(curPage),
                intFmt.format(totalPagesKnown)
            )
            val pageText = "$pageLabel: $pageNumStr"
            if (isRtl) {
                drawAlignedText(computerNotice, PAGE_WIDTH - MARGIN, footerY + 2f, secondaryPaint, Paint.Align.RIGHT, isBold = false)
                drawAlignedText(pageText, MARGIN, footerY + 2f, secondaryPaint, Paint.Align.LEFT, isBold = false)
            } else {
                drawAlignedText(computerNotice, MARGIN, footerY + 2f, secondaryPaint, Paint.Align.LEFT, isBold = false)
                drawAlignedText(pageText, PAGE_WIDTH - MARGIN, footerY + 2f, secondaryPaint, Paint.Align.RIGHT, isBold = false)
            }
        }

        fun drawContinuationHeader() {
            val c = canvas
            val bannerH = 24f
            if (c != null) {
                val rect = RectF(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + bannerH)
                c.drawRoundRect(rect, 4f, 4f, primaryDarkPaint)
                headerSubPaint.textSize = 9f
                val startAlign = if (isRtl) Paint.Align.RIGHT else Paint.Align.LEFT
                val endAlign = if (isRtl) Paint.Align.LEFT else Paint.Align.RIGHT
                val startX = if (isRtl) PAGE_WIDTH - MARGIN - 10f else MARGIN + 10f
                val endX = if (isRtl) MARGIN + 10f else PAGE_WIDTH - MARGIN - 10f
                drawAlignedText(titleStr, startX, currentY + 15.5f, headerSubPaint, startAlign, isBold = true, maxWidth = 260f)
                drawAlignedText(periodLine, endX, currentY + 15.5f, headerSubPaint, endAlign, isBold = false, maxWidth = 220f)
            }
            currentY += bannerH + 10f
        }

        fun startNewPage(onHeaderRepeat: (() -> Unit)? = null) {
            if (pdfDocument != null && currentPage != null) {
                drawFooter(pageNum)
                pdfDocument.finishPage(currentPage)
            }
            pageNum++
            currentPage = pdfDocument?.startPage(
                PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create()
            )
            canvas = currentPage?.canvas
            currentY = MARGIN
            drawContinuationHeader()
            onHeaderRepeat?.invoke()
        }

        fun ensureSpace(neededHeight: Float, onHeaderRepeat: (() -> Unit)? = null) {
            if (needsNewPage(currentY, neededHeight, PAGE_BOTTOM_LIMIT)) {
                startNewPage(onHeaderRepeat)
            }
        }

        fun resolveColLeft(col: ColumnSpec): Float {
            return if (isRtl) {
                PAGE_WIDTH - MARGIN - (col.offset + col.width)
            } else {
                MARGIN + col.offset
            }
        }

        fun resolveColAnchor(col: ColumnSpec): Pair<Float, Paint.Align> {
            val left = resolveColLeft(col)
            val right = left + col.width
            val pad = 6f
            return when (col.align) {
                ColAlign.START -> if (isRtl) (right - pad) to Paint.Align.RIGHT else (left + pad) to Paint.Align.LEFT
                ColAlign.END -> if (isRtl) (left + pad) to Paint.Align.LEFT else (right - pad) to Paint.Align.RIGHT
            }
        }

        fun drawEntriesTableHeader() {
            val rowH = 18f
            val c = canvas
            if (c != null) {
                c.drawRect(RectF(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + rowH), primaryDarkPaint)
                ENTRY_COLUMNS.forEachIndexed { idx, col ->
                    val (anchorX, align) = resolveColAnchor(col)
                    drawAlignedText(
                        raw = colHeaders[idx],
                        x = anchorX,
                        y = currentY + 12.5f,
                        paint = tableHeaderTextPaint,
                        align = align,
                        isBold = true,
                        maxWidth = col.width - 10f
                    )
                }
            }
            currentY += rowH
        }

        fun drawEntriesRows(entries: List<EmployerAdvanceHistoryEntry>) {
            val rowH = 17f
            entries.forEachIndexed { index, entry ->
                ensureSpace(rowH) { drawEntriesTableHeader() }
                val c = canvas
                if (c != null) {
                    if (index % 2 == 1) {
                        c.drawRect(RectF(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + rowH), surfaceBgPaint)
                    }
                    val dateStr = Instant.ofEpochMilli(entry.dateInMillis).atZone(zone).format(entryDateFormatter)
                    val modeStr = when {
                        isCashAdvanceMode(entry.paymentMode) -> cashStr
                        isBankAdvanceMode(entry.paymentMode) -> bankStr
                        else -> entry.paymentMode
                    }
                    val amountStr = MoneyFormat.formatMinor(entry.amountMinor, currency, locale)
                    val cashierStr = entry.cashierName.ifBlank { "-" }
                    val serialStr = intFmt.format(entry.serialNumber)

                    val cells = listOf(serialStr, dateStr, modeStr, amountStr, cashierStr)
                    ENTRY_COLUMNS.forEachIndexed { colIdx, col ->
                        val (anchorX, align) = resolveColAnchor(col)
                        val cellPaint = if (colIdx == 3) bodyBoldPaint else bodyPaint
                        drawAlignedText(
                            raw = cells[colIdx],
                            x = anchorX,
                            y = currentY + 12f,
                            paint = cellPaint,
                            align = align,
                            isBold = (colIdx == 3),
                            maxWidth = col.width - 10f
                        )
                    }
                    c.drawLine(MARGIN, currentY + rowH, PAGE_WIDTH - MARGIN, currentY + rowH, dividerPaint)
                }
                currentY += rowH
            }
        }

        // 1. Page 1 Header Banner
        val hasBusinessName = businessName != null
        val headerHeight = if (hasBusinessName) 74f else 58f
        canvas?.let { c ->
            c.drawRoundRect(RectF(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + headerHeight), 8f, 8f, primaryDarkPaint)

            val logoSize = 46f
            val logoReserve = if (logoBitmap != null) logoSize + 18f else 0f
            if (logoBitmap != null) {
                val logoX = if (isRtl) MARGIN + 12f else PAGE_WIDTH - MARGIN - logoSize - 12f
                val logoY = currentY + (headerHeight - logoSize) / 2f
                c.drawBitmap(logoBitmap, null, RectF(logoX, logoY, logoX + logoSize, logoY + logoSize), null)
            }

            val textX = if (isRtl) PAGE_WIDTH - MARGIN - 14f else MARGIN + 14f
            val textAlign = if (isRtl) Paint.Align.RIGHT else Paint.Align.LEFT
            val maxTextW = CONTENT_WIDTH - 28f - logoReserve

            var textY = currentY + 20f
            if (businessName != null) {
                headerTitlePaint.textSize = 13.5f
                drawAlignedText(businessName, textX, textY, headerTitlePaint, textAlign, isBold = true, maxWidth = maxTextW)
                textY += 16f
            }
            headerTitlePaint.textSize = if (hasBusinessName) 11.5f else 13.5f
            drawAlignedText(titleStr, textX, textY, headerTitlePaint, textAlign, isBold = true, maxWidth = maxTextW)
            textY += 15f
            headerSubPaint.textSize = 8.5f
            drawAlignedText(periodLine, textX, textY, headerSubPaint, textAlign, isBold = false, maxWidth = maxTextW)
            textY += 12f
            drawAlignedText(generatedLine, textX, textY, headerSubPaint, textAlign, isBold = false, maxWidth = maxTextW)
        }
        currentY += headerHeight + 14f

        // 2. Empty state OR Per-employee sections + Other bucket
        if (statement.employees.isEmpty() && statement.otherBucket == null) {
            val emptyBoxH = 54f
            ensureSpace(emptyBoxH)
            canvas?.let { c ->
                val rect = RectF(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + emptyBoxH)
                c.drawRoundRect(rect, 6f, 6f, surfaceBgPaint)
                c.drawRoundRect(rect, 6f, 6f, borderPaint)
                drawAlignedText(
                    raw = emptyMsg,
                    x = PAGE_WIDTH / 2f,
                    y = currentY + 31f,
                    paint = sectionTitlePaint,
                    align = Paint.Align.CENTER,
                    isBold = true,
                    maxWidth = CONTENT_WIDTH - 24f
                )
            }
            currentY += emptyBoxH + 14f
        } else {
            statement.employees.forEach { section ->
                val showSalaryRow = statement.isSingleCalendarMonth
                val boxH = if (showSalaryRow) 56f else 38f
                // Keep employee header card + table header + at least 1 row together on page start
                ensureSpace(boxH + 18f + 17f)

                canvas?.let { c ->
                    val cardRect = RectF(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + boxH)
                    c.drawRoundRect(cardRect, 5f, 5f, primaryContainerBgPaint)
                    c.drawRoundRect(cardRect, 5f, 5f, borderPaint)

                    val startX = if (isRtl) PAGE_WIDTH - MARGIN - 10f else MARGIN + 10f
                    val startAlign = if (isRtl) Paint.Align.RIGHT else Paint.Align.LEFT

                    drawAlignedText(
                        raw = section.employeeName,
                        x = startX,
                        y = currentY + 15f,
                        paint = sectionTitlePaint,
                        align = startAlign,
                        isBold = true,
                        maxWidth = CONTENT_WIDTH - 20f
                    )

                    val totalsSummary = localizedContext.getString(
                        R.string.statement_employee_totals_line_fmt,
                        MoneyFormat.formatMinor(section.cashTotalMinor, currency, locale),
                        MoneyFormat.formatMinor(section.bankTotalMinor, currency, locale),
                        MoneyFormat.formatMinor(section.grandTotalMinor, currency, locale),
                        section.entryCount
                    )
                    drawAlignedText(
                        raw = totalsSummary,
                        x = startX,
                        y = currentY + 30f,
                        paint = secondaryPaint,
                        align = startAlign,
                        isBold = false,
                        maxWidth = CONTENT_WIDTH - 20f
                    )

                    if (showSalaryRow) {
                        val salaryVal = MoneyFormat.formatMinor(section.monthlySalaryMinor ?: 0L, currency, locale)
                        val advVal = MoneyFormat.formatMinor(section.advancesInMonthMinor ?: 0L, currency, locale)
                        val rem = section.remainingMinor ?: 0L
                        val exc = section.excessMinor ?: 0L
                        val balanceStr = if (exc > 0L) {
                            "$excessLabel: ${MoneyFormat.formatMinor(exc, currency, locale)}"
                        } else {
                            "$remainingLabel: ${MoneyFormat.formatMinor(rem, currency, locale)}"
                        }
                        val salaryLine = "$salaryLabel: $salaryVal   •   $advancesLabel: $advVal   •   $balanceStr"
                        val balPaint = if (exc > 0L) errorPaint else successPaint
                        drawAlignedText(
                            raw = salaryLine,
                            x = startX,
                            y = currentY + 46f,
                            paint = balPaint,
                            align = startAlign,
                            isBold = true,
                            maxWidth = CONTENT_WIDTH - 20f
                        )
                    }
                }
                currentY += boxH + 4f

                drawEntriesTableHeader()
                drawEntriesRows(section.entries)
                currentY += 12f
            }

            statement.otherBucket?.let { other ->
                val boxH = 44f
                ensureSpace(boxH + 18f + 17f)

                canvas?.let { c ->
                    val cardRect = RectF(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + boxH)
                    c.drawRoundRect(cardRect, 5f, 5f, tableHeaderBgPaint)
                    c.drawRoundRect(cardRect, 5f, 5f, borderPaint)

                    val startX = if (isRtl) PAGE_WIDTH - MARGIN - 10f else MARGIN + 10f
                    val startAlign = if (isRtl) Paint.Align.RIGHT else Paint.Align.LEFT

                    drawAlignedText(
                        raw = "$otherTitle — $otherSubtitle",
                        x = startX,
                        y = currentY + 15f,
                        paint = sectionTitlePaint,
                        align = startAlign,
                        isBold = true,
                        maxWidth = CONTENT_WIDTH - 20f
                    )
                    val totalsSummary = localizedContext.getString(
                        R.string.statement_employee_totals_line_fmt,
                        MoneyFormat.formatMinor(other.cashTotalMinor, currency, locale),
                        MoneyFormat.formatMinor(other.bankTotalMinor, currency, locale),
                        MoneyFormat.formatMinor(other.advancesTotalMinor, currency, locale),
                        other.advanceCount
                    )
                    drawAlignedText(
                        raw = totalsSummary,
                        x = startX,
                        y = currentY + 32f,
                        paint = secondaryPaint,
                        align = startAlign,
                        isBold = false,
                        maxWidth = CONTENT_WIDTH - 20f
                    )
                }
                currentY += boxH + 4f

                drawEntriesTableHeader()
                drawEntriesRows(other.history)
                currentY += 12f
            }
        }

        // 3. Footer Grand Totals Card
        val totalsBoxH = 66f
        ensureSpace(totalsBoxH)
        canvas?.let { c ->
            val rect = RectF(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + totalsBoxH)
            c.drawRoundRect(rect, 6f, 6f, primaryContainerBgPaint)
            c.drawRoundRect(rect, 6f, 6f, borderPaint)

            val startX = if (isRtl) PAGE_WIDTH - MARGIN - 12f else MARGIN + 12f
            val endX = if (isRtl) MARGIN + 12f else PAGE_WIDTH - MARGIN - 12f
            val startAlign = if (isRtl) Paint.Align.RIGHT else Paint.Align.LEFT
            val endAlign = if (isRtl) Paint.Align.LEFT else Paint.Align.RIGHT

            drawAlignedText(summaryTitle, startX, currentY + 16f, sectionTitlePaint, startAlign, isBold = true, maxWidth = 280f)
            val entriesText = localizedContext.getString(R.string.advance_entries_count_fmt, statement.totalEntryCount)
            drawAlignedText(entriesText, endX, currentY + 16f, secondaryPaint, endAlign, isBold = false, maxWidth = 200f)

            c.drawLine(MARGIN + 10f, currentY + 23f, PAGE_WIDTH - MARGIN - 10f, currentY + 23f, dividerPaint)

            val row1Y = currentY + 38f
            drawAlignedText(totalAdvancesLabel, startX, row1Y, bodyBoldPaint, startAlign, isBold = true, maxWidth = 260f)
            drawAlignedText(
                MoneyFormat.formatMinor(statement.totalAdvancesMinor, currency, locale),
                endX,
                row1Y,
                sectionTitlePaint,
                endAlign,
                isBold = true,
                maxWidth = 220f
            )

            val row2Y = currentY + 54f
            val breakdown = "$cashStr: ${MoneyFormat.formatMinor(statement.totalCashMinor, currency, locale)}   •   " +
                "$bankStr: ${MoneyFormat.formatMinor(statement.totalBankMinor, currency, locale)}   •   " +
                "$totalLabel: ${MoneyFormat.formatMinor(statement.totalAdvancesMinor, currency, locale)}"
            drawAlignedText(breakdown, startX, row2Y, secondaryPaint, startAlign, isBold = false, maxWidth = CONTENT_WIDTH - 24f)
        }
        currentY += totalsBoxH

        if (pdfDocument != null && currentPage != null) {
            drawFooter(pageNum)
            pdfDocument.finishPage(currentPage)
        }

        return pageNum
    }

    private fun writeTempCachePdf(
        context: Context,
        pdfDocument: PdfDocument,
        fileName: String
    ): PdfReportGenerator.ExportResult {
        var outputStream: FileOutputStream? = null
        return try {
            val exportsDir = PdfShareUtils.getTempStatementExportsDirectory(context)
            val targetFile = File(exportsDir, fileName)
            outputStream = FileOutputStream(targetFile)
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            pdfDocument.close()

            val fileUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                targetFile
            )
            PdfReportGenerator.ExportResult(
                isSuccess = true,
                file = targetFile,
                uri = fileUri,
                displayPath = targetFile.absolutePath
            )
        } catch (e: Exception) {
            try {
                pdfDocument.close()
            } catch (_: Exception) {}
            PdfReportGenerator.ExportResult(
                isSuccess = false,
                file = null,
                uri = null,
                displayPath = "",
                errorMessage = e.localizedMessage ?: context.getString(R.string.pdf_write_failed_error)
            )
        } finally {
            try {
                outputStream?.close()
            } catch (_: Exception) {}
        }
    }
}
