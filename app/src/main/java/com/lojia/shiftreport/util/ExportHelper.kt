package com.lojia.shiftreport.util

import com.lojia.shiftreport.R
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
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

    fun exportShiftReportsToCsv(
        context: Context,
        reports: List<ShiftReport>,
        currency: String = "SAR"
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

    fun exportReportsAsCsv(context: Context, reports: List<ShiftReport>, currency: String = "SAR") {
        try {
            val file = exportShiftReportsToCsv(context, reports, currency)
            shareExportedFile(context, file, "text/csv", "Export Shift Reports CSV")
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.error_exporting_csv, e.message ?: ""), Toast.LENGTH_SHORT).show()
        }
    }

    fun exportReportsAsPdf(context: Context, reports: List<ShiftReport>, currency: String = "SAR") {
        try {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val pdfFile = File(exportDir, "Shift_Reports_$timeStamp.pdf")

            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
            val page = document.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val paint = Paint().apply {
                color = Color.BLACK
                textSize = 14f
                isAntiAlias = true
            }

            val titlePaint = Paint().apply {
                color = Color.rgb(37, 99, 235)
                textSize = 18f
                isFakeBoldText = true
                isAntiAlias = true
            }

            canvas.drawText(context.getString(R.string.lojia_system_shift_reports), 40f, 50f, titlePaint)
            paint.textSize = 11f
            paint.color = Color.DKGRAY
            canvas.drawText(context.getString(R.string.pdf_generated_at, SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())), 40f, 72f, paint)

            paint.color = Color.LTGRAY
            canvas.drawLine(40f, 85f, 555f, 85f, paint)

            var y = 110f
            paint.color = Color.BLACK
            paint.textSize = 11f
            paint.isFakeBoldText = true

            // Table Header
            canvas.drawText(context.getString(R.string.id_upper), 40f, y, paint)
            canvas.drawText(context.getString(R.string.cashier_6), 70f, y, paint)
            canvas.drawText(context.getString(R.string.shift_2), 180f, y, paint)
            canvas.drawText(context.getString(R.string.total_sales), 270f, y, paint)
            canvas.drawText(context.getString(R.string.net_cash_1), 370f, y, paint)
            canvas.drawText(context.getString(R.string.date_2), 470f, y, paint)

            paint.isFakeBoldText = false
            y += 8f
            canvas.drawLine(40f, y, 555f, y, paint)
            y += 18f

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            for (r in reports.take(25)) {
                canvas.drawText("#${r.id}", 40f, y, paint)
                canvas.drawText(r.cashierName.take(16), 70f, y, paint)
                canvas.drawText(r.shift, 180f, y, paint)
                canvas.drawText(context.getString(R.string.msg_2f_s_21).format(r.totalSales, currency), 270f, y, paint)
                canvas.drawText(context.getString(R.string.msg_2f_s_21).format(r.netCash, currency), 370f, y, paint)
                canvas.drawText(dateFormat.format(Date(r.dateInMillis)), 470f, y, paint)
                y += 22f
            }

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
