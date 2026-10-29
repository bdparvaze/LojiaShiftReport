package com.lojia.shiftreport.report.invoice

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.lojia.shiftreport.util.PdfFontManager
import com.lojia.shiftreport.util.PdfPalette

/**
 * Renders the core Lojia POS financial breakdown & payment reconciliation summary:
 * Gross Cash, MADA/Card, Digital Wallet, Expenses, Staff Meal, Muassel, Variance,
 * Net Sales, VAT 15%, and Grand Total Payable.
 */
object InvoicePaymentRenderer {

    fun drawPaymentBreakdown(
        canvas: Canvas,
        context: Context,
        data: InvoiceData,
        pageWidth: Float,
        startY: Float
    ): Float {
        var currentY = startY
        val isThermal = data.paperSize.isThermal
        val margin = if (isThermal) 12f else 32f
        val boxWidth = pageWidth - (margin * 2)
        val sym = data.currencySymbol

        val p = data.payment

        val rowHeight = if (isThermal) 16f else 20f

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = PdfPalette.TEXT_SECONDARY
            textSize = if (isThermal) 8.5f else 10f
        }

        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = PdfPalette.TEXT_PRIMARY
            textSize = if (isThermal) 8.5f else 10.5f
            textAlign = Paint.Align.RIGHT
        }

        fun drawRow(label: String, amount: Double, isBold: Boolean = false, overrideColor: Int? = null) {
            val y = currentY + 12f
            labelPaint.color = overrideColor ?: PdfPalette.TEXT_SECONDARY
            PdfFontManager.applyTypeface(labelPaint, context, label, isBold = isBold)
            canvas.drawText(label, margin + 4f, y, labelPaint)

            valuePaint.color = overrideColor ?: (if (isBold) PdfPalette.TEXT_PRIMARY else PdfPalette.TEXT_SECONDARY)
            val valStr = String.format("%.2f %s", amount, sym)
            PdfFontManager.applyTypeface(valuePaint, context, valStr, isBold = isBold)
            canvas.drawText(valStr, margin + boxWidth - 4f, y, valuePaint)

            currentY += rowHeight
        }

        // Section Title
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = PdfPalette.PRIMARY_DARK
            textSize = if (isThermal) 9.5f else 12f
        }
        val sectionTitle = "Payment Reconciliation / ملخص المدفوعات"
        PdfFontManager.applyTypeface(titlePaint, context, sectionTitle, isBold = true)
        canvas.drawText(sectionTitle, margin, currentY + 12f, titlePaint)
        currentY += 18f

        // Draw Ledger Rows
        drawRow("Gross Cash / النقد المقبوض", p.grossCash)
        drawRow("MADA & Card / مدى والبطاقات", p.madaCard)
        drawRow("Digital Wallet / المحفظة الرقمية", p.digitalWallet)
        drawRow("Daily Expenses / المصروفات اليومية", -p.expenses, overrideColor = PdfPalette.ERROR)
        drawRow("Staff Meal / وجبات الموظفين", -p.staffMeal, overrideColor = PdfPalette.WARNING)
        drawRow("Muassel / مبيعات المعسل", p.muassel)

        // Variance / Cash Discrepancy
        val varColor = if (p.variance >= 0) PdfPalette.SUCCESS else PdfPalette.ERROR
        val varLabel = if (p.variance >= 0) "Cash Surplus / فائض نقدي (+)" else "Cash Shortage / عجز نقدي (-)"
        drawRow(varLabel, p.variance, isBold = true, overrideColor = varColor)

        // Divider
        val divPaint = Paint().apply {
            color = PdfPalette.DIVIDER
            strokeWidth = 1f
        }
        currentY += 4f
        canvas.drawLine(margin, currentY, margin + boxWidth, currentY, divPaint)
        currentY += 8f

        // Subtotal & VAT (15%)
        drawRow("Net Sales (Excl. VAT) / صافي المبيعات", p.netSales, isBold = true)
        val vatPctStr = "${(p.vatRate * 100).toInt()}%"
        drawRow("VAT ($vatPctStr) / ضريبة القيمة المضافة", p.vatAmount, isBold = false)

        currentY += 6f

        // Grand Total Banner
        val totalBannerHeight = if (isThermal) 26f else 34f
        val totalRect = RectF(margin, currentY, margin + boxWidth, currentY + totalBannerHeight)
        val bannerBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = PdfPalette.PRIMARY
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(totalRect, 6f, 6f, bannerBg)

        val totalTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = PdfPalette.ON_PRIMARY
            textSize = if (isThermal) 10f else 13f
        }
        val totalLabel = "Grand Total / المجموع الكلي"
        PdfFontManager.applyTypeface(totalTextPaint, context, totalLabel, isBold = true)
        val bannerCenterY = currentY + (totalBannerHeight / 2f) - ((totalTextPaint.descent() + totalTextPaint.ascent()) / 2f)
        canvas.drawText(totalLabel, margin + 8f, bannerCenterY, totalTextPaint)

        val totalValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = PdfPalette.ON_PRIMARY
            textSize = if (isThermal) 11f else 14.5f
            textAlign = Paint.Align.RIGHT
        }
        val totalValStr = String.format("%.2f %s", p.totalPayable, sym)
        PdfFontManager.applyTypeface(totalValPaint, context, totalValStr, isBold = true)
        canvas.drawText(totalValStr, margin + boxWidth - 8f, bannerCenterY, totalValPaint)

        return currentY + totalBannerHeight + 14f
    }
}
