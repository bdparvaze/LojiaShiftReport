package com.lojia.shiftreport.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.lojia.shiftreport.data.BusinessProfile
import com.lojia.shiftreport.data.Employee
import com.lojia.shiftreport.data.EmployerAdvanceTransaction
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object EmployerAdvancePdfGenerator {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 36f

    fun generateEmployeeAdvanceReport(
        context: Context,
        employee: Employee,
        transactions: List<EmployerAdvanceTransaction>,
        dateRange: String,
        businessProfile: BusinessProfile? = null
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#191C20")
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#44474E")
            textSize = 11f
        }
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#191C20")
            textSize = 10f
        }
        val boldCellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#191C20")
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bgHeaderPaint = Paint().apply {
            color = Color.parseColor("#284777")
        }
        val linePaint = Paint().apply {
            color = Color.parseColor("#E2E2E9")
            strokeWidth = 1f
        }

        var y = MARGIN + 10f

        // Store Header
        val storeName = businessProfile?.businessName?.ifBlank { "Lojia POS" } ?: "Lojia POS"
        canvas.drawText(storeName, MARGIN, y, titlePaint)
        y += 18f
        canvas.drawText("Employee Advance & Payroll Ledger", MARGIN, y, subtitlePaint)
        y += 16f
        canvas.drawText("Period: $dateRange", MARGIN, y, subtitlePaint)
        y += 22f

        // Employee Info Box
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
        y += 16f
        canvas.drawText("Employee: ${employee.name}", MARGIN, y, boldCellPaint)
        canvas.drawText(
            String.format(Locale.US, "Monthly Salary: %.2f SAR", employee.monthlySalary),
            PAGE_WIDTH - MARGIN - 180f,
            y,
            boldCellPaint
        )
        y += 16f
        if (!employee.phone.isNullOrBlank()) {
            canvas.drawText("Phone: ${employee.phone}", MARGIN, y, subtitlePaint)
            y += 16f
        }
        y += 8f

        // Table Header
        val tableTop = y
        canvas.drawRect(MARGIN, tableTop, PAGE_WIDTH - MARGIN, tableTop + 24f, bgHeaderPaint)
        val colDate = MARGIN + 6f
        val colShift = MARGIN + 78f
        val colAmount = MARGIN + 135f
        val colType = MARGIN + 215f
        val colReason = MARGIN + 295f
        val colBalance = PAGE_WIDTH - MARGIN - 75f

        val headerTextY = tableTop + 16f
        canvas.drawText("Date", colDate, headerTextY, headerPaint)
        canvas.drawText("Shift", colShift, headerTextY, headerPaint)
        canvas.drawText("Amount", colAmount, headerTextY, headerPaint)
        canvas.drawText("Type", colType, headerTextY, headerPaint)
        canvas.drawText("Reason", colReason, headerTextY, headerPaint)
        canvas.drawText("Balance", colBalance, headerTextY, headerPaint)

        y = tableTop + 24f

        val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
        val sortedAsc = transactions.sortedBy { it.createdAt }
        var runningAdvances = 0.0

        for (tx in sortedAsc) {
            if (y > PAGE_HEIGHT - 140f) break
            runningAdvances += tx.amount
            val balanceAfter = employee.monthlySalary - runningAdvances
            val rowY = y + 16f

            canvas.drawText(dateFormat.format(Date(tx.createdAt)), colDate, rowY, cellPaint)
            canvas.drawText(tx.shiftId.ifBlank { "-" }, colShift, rowY, cellPaint)
            val amtStr = if (tx.amount >= 0) {
                String.format(Locale.US, "+%.2f", tx.amount)
            } else {
                String.format(Locale.US, "%.2f", tx.amount)
            }
            canvas.drawText(amtStr, colAmount, rowY, cellPaint)
            val typeLabel = if (tx.type == "CORRECTION") "CORR" else tx.paymentMethod
            canvas.drawText(typeLabel, colType, rowY, cellPaint)
            val reasonShort = if (tx.reason.length > 18) tx.reason.take(16) + ".." else tx.reason.ifBlank { "-" }
            canvas.drawText(reasonShort, colReason, rowY, cellPaint)
            canvas.drawText(String.format(Locale.US, "%.2f", balanceAfter), colBalance, rowY, cellPaint)

            y += 22f
            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
        }

        y += 24f
        val totalAdvance = transactions.sumOf { it.amount }
        val remaining = employee.monthlySalary - totalAdvance

        canvas.drawText(
            String.format(Locale.US, "Total Advances Taken: %.2f SAR", totalAdvance),
            MARGIN,
            y,
            boldCellPaint
        )
        y += 18f
        if (remaining >= 0) {
            canvas.drawText(
                String.format(Locale.US, "Remaining Salary Payable: %.2f SAR", remaining),
                MARGIN,
                y,
                boldCellPaint
            )
        } else {
            canvas.drawText(
                String.format(Locale.US, "Overpaid Balance: %.2f SAR", -remaining),
                MARGIN,
                y,
                boldCellPaint
            )
        }

        // QR Code at bottom-right
        val qrPayload = "LOJIA-PAYROLL|EMP:${employee.name}|SAL:${employee.monthlySalary}|ADV:$totalAdvance|BAL:$remaining"
        val qrBitmap = createSimpleQrBitmap(qrPayload, 84)
        if (qrBitmap != null) {
            canvas.drawBitmap(qrBitmap, PAGE_WIDTH - MARGIN - 84f, PAGE_HEIGHT - MARGIN - 84f, null)
        }

        document.finishPage(page)

        val outDir = File(context.cacheDir, "advance_reports").apply { mkdirs() }
        val safeName = employee.name.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val outFile = File(outDir, "advance_${safeName}_${System.currentTimeMillis()}.pdf")
        FileOutputStream(outFile).use { document.writeTo(it) }
        document.close()
        return outFile
    }

    fun generateAllEmployeesSummaryReport(
        context: Context,
        employees: List<Employee>,
        allTransactions: List<EmployerAdvanceTransaction>,
        dateRange: String,
        businessProfile: BusinessProfile? = null
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#191C20")
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#44474E")
            textSize = 11f
        }
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#191C20")
            textSize = 10.5f
        }
        val boldCellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#191C20")
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bgHeaderPaint = Paint().apply {
            color = Color.parseColor("#284777")
        }
        val linePaint = Paint().apply {
            color = Color.parseColor("#E2E2E9")
            strokeWidth = 1f
        }

        var y = MARGIN + 10f
        val storeName = businessProfile?.businessName?.ifBlank { "Lojia POS" } ?: "Lojia POS"
        canvas.drawText(storeName, MARGIN, y, titlePaint)
        y += 18f
        canvas.drawText("All Employees Payroll & Advances Summary", MARGIN, y, subtitlePaint)
        y += 16f
        canvas.drawText("Period: $dateRange", MARGIN, y, subtitlePaint)
        y += 22f

        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 24f, bgHeaderPaint)
        val colName = MARGIN + 8f
        val colSalary = MARGIN + 190f
        val colAdvances = MARGIN + 310f
        val colBalance = PAGE_WIDTH - MARGIN - 95f

        canvas.drawText("Employee", colName, y + 16f, headerPaint)
        canvas.drawText("Salary (SAR)", colSalary, y + 16f, headerPaint)
        canvas.drawText("Advances (SAR)", colAdvances, y + 16f, headerPaint)
        canvas.drawText("Balance (SAR)", colBalance, y + 16f, headerPaint)
        y += 24f

        val txByEmp = allTransactions.groupBy { it.employeeId }
        var grandSalary = 0.0
        var grandAdvances = 0.0

        for (emp in employees) {
            if (y > PAGE_HEIGHT - 100f) break
            val empAdv = txByEmp[emp.id]?.sumOf { it.amount } ?: 0.0
            val bal = emp.monthlySalary - empAdv
            grandSalary += emp.monthlySalary
            grandAdvances += empAdv

            val rowY = y + 16f
            canvas.drawText(emp.name.take(24), colName, rowY, cellPaint)
            canvas.drawText(String.format(Locale.US, "%.2f", emp.monthlySalary), colSalary, rowY, cellPaint)
            canvas.drawText(String.format(Locale.US, "%.2f", empAdv), colAdvances, rowY, cellPaint)
            canvas.drawText(String.format(Locale.US, "%.2f", bal), colBalance, rowY, cellPaint)
            y += 22f
            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
        }

        y += 22f
        canvas.drawText(
            String.format(
                Locale.US,
                "Total Payroll: %.2f SAR  |  Total Advances: %.2f SAR  |  Net Payable: %.2f SAR",
                grandSalary,
                grandAdvances,
                grandSalary - grandAdvances
            ),
            MARGIN,
            y,
            boldCellPaint
        )

        document.finishPage(page)

        val outDir = File(context.cacheDir, "advance_reports").apply { mkdirs() }
        val outFile = File(outDir, "all_employees_advances_${System.currentTimeMillis()}.pdf")
        FileOutputStream(outFile).use { document.writeTo(it) }
        document.close()
        return outFile
    }

    private fun createSimpleQrBitmap(content: String, size: Int): Bitmap? {
        return try {
            val matrix = NativeQrCodeEncoder.encode(content)
            val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val scale = size.toFloat() / matrix.width.toFloat()
            val canvas = Canvas(bmp)
            canvas.drawColor(Color.WHITE)
            val p = Paint().apply { color = Color.BLACK }
            for (y in 0 until matrix.width) {
                for (x in 0 until matrix.width) {
                    if (matrix.get(x, y)) {
                        canvas.drawRect(x * scale, y * scale, (x + 1) * scale, (y + 1) * scale, p)
                    }
                }
            }
            bmp
        } catch (_: Exception) {
            null
        }
    }
}
