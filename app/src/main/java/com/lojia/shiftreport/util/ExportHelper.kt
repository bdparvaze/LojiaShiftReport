package com.lojia.shiftreport.util

import com.lojia.shiftreport.R
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.lojia.shiftreport.data.ShiftReport
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

object ExportHelper {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 36f

    fun exportShiftReportsToCsv(
        context: Context,
        reports: List<ShiftReport>,
        currency: String = MoneyFormat.DEFAULT_CURRENCY_CODE
    ): File {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val csvFile = File(exportDir, "Shift_Reports_$timeStamp.csv")

        FileWriter(csvFile).use { writer ->
            writer.append("Report ID,Cashier Name,Shift,Date,Gross Cash ($currency),Mada/Card ($currency),Digital Wallet ($currency),Staff Meals,Expenses ($currency),Net Cash ($currency),Total Sales ($currency),Notes\n")
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            for (r in reports) {
                val dateStr = dateFormat.format(Date(r.dateInMillis))
                val sanitizedNotes = r.notes.replace("\"", "\"\"")
                writer.append("${r.id},\"${r.cashierName}\",${r.shift},$dateStr,${r.grossCash},${r.madaPayments},${r.digitalWallet},${r.staffMealsCount},${r.totalExpenses},${r.netCash},${r.totalSales},\"$sanitizedNotes\"\n")
            }
            writer.flush()
        }
        return csvFile
    }

    fun exportReportsAsCsv(context: Context, reports: List<ShiftReport>, currency: String = MoneyFormat.DEFAULT_CURRENCY_CODE) {
        try {
            val file = exportShiftReportsToCsv(context, reports, currency)
            shareExportedFile(context, file, "text/csv", "Export Shift Reports CSV")
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.error_exporting_csv, e.message ?: ""), Toast.LENGTH_SHORT).show()
        }
    }

    fun exportReportsAsPdf(context: Context, reports: List<ShiftReport>, currency: String = MoneyFormat.DEFAULT_CURRENCY_CODE) {
        try {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val pdfFile = File(exportDir, "Shift_Reports_$timeStamp.pdf")

            val document = PdfDocument()
            var pageNum = 1
            var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create()
            var page = document.startPage(pageInfo)
            var canvas: Canvas = page.canvas

            val primaryDarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = PdfPalette.PRIMARY_DARK
                style = Paint.Style.FILL
            }
            val headerTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = PdfPalette.ON_PRIMARY
                textSize = 15f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val headerSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = PdfPalette.PRIMARY_CONTAINER
                textSize = 9.5f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            }
            val tableHeaderBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = PdfPalette.SURFACE_VARIANT
                style = Paint.Style.FILL
            }
            val zebraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = PdfPalette.BACKGROUND
                style = Paint.Style.FILL
            }
            val totalBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = PdfPalette.PRIMARY_CONTAINER
                style = Paint.Style.FILL
            }
            val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = PdfPalette.DIVIDER
                strokeWidth = 1f
            }
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = PdfPalette.BORDER
                strokeWidth = 1f
            }
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = PdfPalette.TEXT_PRIMARY
                textSize = 9.5f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            }
            val textSecondaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = PdfPalette.TEXT_SECONDARY
                textSize = 9.5f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            }
            val textBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = PdfPalette.TEXT_PRIMARY
                textSize = 9.5f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val successBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = PdfPalette.SUCCESS
                textSize = 10f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = PdfPalette.TEXT_HINT
                textSize = 8.5f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            }

            val colId = MARGIN + 8f
            val colCashier = MARGIN + 52f
            val colShift = MARGIN + 175f
            val colDate = MARGIN + 250f
            val colNetRight = PAGE_WIDTH - MARGIN - 115f
            val colSalesRight = PAGE_WIDTH - MARGIN - 8f

            val generatedStr = context.getString(
                R.string.pdf_generated_at,
                SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            )

            fun drawFooter() {
                val footerY = PAGE_HEIGHT - 22f
                canvas.drawLine(MARGIN, footerY - 8f, PAGE_WIDTH - MARGIN, footerY - 8f, dividerPaint)
                canvas.drawText(context.getString(R.string.lojia_pos_system_v10), MARGIN, footerY + 2f, footerPaint)
                val pageStr = "${context.getString(R.string.pdf_page)} $pageNum"
                val pageW = footerPaint.measureText(pageStr)
                canvas.drawText(pageStr, PAGE_WIDTH - MARGIN - pageW, footerY + 2f, footerPaint)
            }

            fun drawTableHeader(topY: Float): Float {
                val headerRect = RectF(MARGIN, topY, PAGE_WIDTH - MARGIN, topY + 22f)
                canvas.drawRect(headerRect, tableHeaderBgPaint)
                canvas.drawLine(MARGIN, topY + 22f, PAGE_WIDTH - MARGIN, topY + 22f, borderPaint)

                val baselineY = topY + 14.5f
                canvas.drawText(context.getString(R.string.id_upper), colId, baselineY, textBoldPaint)
                canvas.drawText(context.getString(R.string.cashier_6), colCashier, baselineY, textBoldPaint)
                canvas.drawText(context.getString(R.string.shift_2), colShift, baselineY, textBoldPaint)
                canvas.drawText(context.getString(R.string.date_2), colDate, baselineY, textBoldPaint)

                val netHeader = context.getString(R.string.net_cash_1)
                val netW = textBoldPaint.measureText(netHeader)
                canvas.drawText(netHeader, colNetRight - netW, baselineY, textBoldPaint)

                val salesHeader = context.getString(R.string.total_sales)
                val salesW = textBoldPaint.measureText(salesHeader)
                canvas.drawText(salesHeader, colSalesRight - salesW, baselineY, textBoldPaint)

                return topY + 22f
            }

            var y = MARGIN

            // Page 1 Header Banner
            val bannerHeight = 58f
            val bannerRect = RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + bannerHeight)
            canvas.drawRoundRect(bannerRect, 8f, 8f, primaryDarkPaint)

            canvas.drawText(context.getString(R.string.lojia_system_shift_reports), MARGIN + 14f, y + 24f, headerTitlePaint)
            canvas.drawText(generatedStr, MARGIN + 14f, y + 42f, headerSubPaint)

            val recordsCountStr = context.getString(R.string.pdf_total_records, reports.size)
            val recordsW = headerSubPaint.measureText(recordsCountStr)
            canvas.drawText(recordsCountStr, PAGE_WIDTH - MARGIN - recordsW - 14f, y + 24f, headerSubPaint)

            y += bannerHeight + 16f
            y = drawTableHeader(y)

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val rowHeight = 20f

            reports.forEachIndexed { index, r ->
                if (y + rowHeight > PAGE_HEIGHT - 65f) {
                    drawFooter()
                    document.finishPage(page)

                    pageNum++
                    pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create()
                    page = document.startPage(pageInfo)
                    canvas = page.canvas
                    y = MARGIN

                    val miniRect = RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 24f)
                    canvas.drawRoundRect(miniRect, 4f, 4f, primaryDarkPaint)
                    val miniPaint = Paint(headerTitlePaint).apply { textSize = 10.5f }
                    canvas.drawText(
                        "${context.getString(R.string.lojia_system_shift_reports)} (${context.getString(R.string.pdf_page)} $pageNum)",
                        MARGIN + 10f,
                        y + 16f,
                        miniPaint
                    )
                    y += 32f
                    y = drawTableHeader(y)
                }

                if (index % 2 == 1) {
                    canvas.drawRect(RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowHeight), zebraPaint)
                }

                val textY = y + 14f
                canvas.drawText("#${r.id}", colId, textY, textSecondaryPaint)
                val cashierStr = if (r.cashierName.length > 18) r.cashierName.take(16) + ".." else r.cashierName
                canvas.drawText(cashierStr, colCashier, textY, textPaint)
                canvas.drawText(r.shift, colShift, textY, textPaint)
                canvas.drawText(dateFormat.format(Date(r.dateInMillis)), colDate, textY, textSecondaryPaint)

                val netStr = context.getString(R.string.msg_2f_s_21).format(r.netCash, currency)
                val netW = textPaint.measureText(netStr)
                canvas.drawText(netStr, colNetRight - netW, textY, textPaint)

                val salesStr = context.getString(R.string.msg_2f_s_21).format(r.totalSales, currency)
                val salesW = textBoldPaint.measureText(salesStr)
                canvas.drawText(salesStr, colSalesRight - salesW, textY, textBoldPaint)

                canvas.drawLine(MARGIN, y + rowHeight, PAGE_WIDTH - MARGIN, y + rowHeight, dividerPaint)
                y += rowHeight
            }

            // Grand Totals Row
            if (y + 30f > PAGE_HEIGHT - 45f) {
                drawFooter()
                document.finishPage(page)

                pageNum++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                y = MARGIN
            }

            val totalBoxTop = y + 6f
            val totalRect = RectF(MARGIN, totalBoxTop, PAGE_WIDTH - MARGIN, totalBoxTop + 24f)
            canvas.drawRoundRect(totalRect, 4f, 4f, totalBoxPaint)

            val totalBaseline = totalBoxTop + 16f
            canvas.drawText(context.getString(R.string.grand_totals), colId, totalBaseline, textBoldPaint)

            val totalNet = reports.sumOf { it.netCash }
            val totalSales = reports.sumOf { it.totalSales }

            val totalNetStr = context.getString(R.string.msg_2f_s_21).format(totalNet, currency)
            val totalNetW = textBoldPaint.measureText(totalNetStr)
            canvas.drawText(totalNetStr, colNetRight - totalNetW, totalBaseline, textBoldPaint)

            val totalSalesStr = context.getString(R.string.msg_2f_s_21).format(totalSales, currency)
            val totalSalesW = successBoldPaint.measureText(totalSalesStr)
            canvas.drawText(totalSalesStr, colSalesRight - totalSalesW, totalBaseline, successBoldPaint)

            drawFooter()
            document.finishPage(page)
            FileOutputStream(pdfFile).use { out ->
                document.writeTo(out)
            }
            document.close()

            shareExportedFile(context, pdfFile, "application/pdf", "Shift Reports PDF")
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.error_exporting_pdf, e.message ?: ""), Toast.LENGTH_SHORT).show()
        }
    }

    fun shareExportedFile(context: Context, file: File, mimeType: String, title: String) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
