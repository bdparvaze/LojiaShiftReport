package com.lojia.shiftreport.report.invoice

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.lojia.shiftreport.util.PdfFontManager
import com.lojia.shiftreport.util.PdfLogoRenderer
import com.lojia.shiftreport.util.PdfPalette

/**
 * Renders the top 20% header of Foodics POS style invoices.
 * Displays business logo (circular badge), store name in 3 languages (EN, AR, BN),
 * VAT registration number, store address, and invoice metadata.
 */
object InvoiceHeaderRenderer {

    fun drawHeader(
        canvas: Canvas,
        context: Context,
        data: InvoiceData,
        pageWidth: Float,
        startY: Float
    ): Float {
        var currentY = startY + 16f
        val isThermal = data.paperSize.isThermal
        val margin = if (isThermal) 12f else 32f
        val contentWidth = pageWidth - (margin * 2)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = PdfPalette.TEXT_PRIMARY
        }

        // 1. Logo & Business Title
        val logoSize = if (isThermal) 36f else 52f
        val logoX = margin
        val logoY = currentY

        PdfLogoRenderer.drawBusinessLogoBadge(
            canvas = canvas,
            context = context,
            logoUri = data.logoUri,
            bizName = data.storeName,
            x = logoX,
            y = logoY,
            size = logoSize
        )

        // Store Names in English, Arabic, and Bengali
        val titleStartX = logoX + logoSize + 12f
        var textY = currentY + (if (isThermal) 14f else 18f)

        // English Name
        textPaint.textSize = if (isThermal) 13f else 18f
        PdfFontManager.applyTypeface(textPaint, context, data.storeName, isBold = true)
        textPaint.color = PdfPalette.PRIMARY_DARK
        canvas.drawText(data.storeName, titleStartX, textY, textPaint)

        // Arabic Subtitle (if available)
        val arName = data.storeNameArabic.ifBlank { "تقرير الوردية والمبيعات" }
        textY += if (isThermal) 13f else 16f
        textPaint.textSize = if (isThermal) 10.5f else 13f
        PdfFontManager.applyTypeface(textPaint, context, arName, isBold = false)
        textPaint.color = PdfPalette.TEXT_SECONDARY
        canvas.drawText(arName, titleStartX, textY, textPaint)

        // Bengali Subtitle (if available)
        val bnName = data.storeNameBengali.ifBlank { "শিফট রিপোর্ট ও ইনভয়েস" }
        textY += if (isThermal) 12f else 15f
        textPaint.textSize = if (isThermal) 9.5f else 12f
        PdfFontManager.applyTypeface(textPaint, context, bnName, isBold = false)
        textPaint.color = PdfPalette.TEXT_HINT
        canvas.drawText(bnName, titleStartX, textY, textPaint)

        currentY = maxOf(currentY + logoSize + 10f, textY + 12f)

        // 2. VAT & Store Address Badge
        if (data.vatNo.isNotBlank()) {
            val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = PdfPalette.PRIMARY_CONTAINER
                style = Paint.Style.FILL
            }
            val badgeHeight = if (isThermal) 18f else 22f
            val badgeRect = RectF(margin, currentY, margin + contentWidth, currentY + badgeHeight)
            canvas.drawRoundRect(badgeRect, 4f, 4f, badgePaint)

            val vatText = "VAT / الرقم الضريبي: ${data.vatNo}"
            val vatPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = PdfPalette.PRIMARY_DARK
                textSize = if (isThermal) 9f else 10.5f
            }
            PdfFontManager.applyTypeface(vatPaint, context, vatText, isBold = true)
            val vatY = currentY + (badgeHeight / 2f) - ((vatPaint.descent() + vatPaint.ascent()) / 2f)
            canvas.drawText(vatText, margin + 8f, vatY, vatPaint)

            currentY += badgeHeight + 8f
        }

        // Store Address (if available)
        if (data.storeAddress.isNotBlank()) {
            val addrPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = PdfPalette.TEXT_HINT
                textSize = if (isThermal) 8.5f else 10f
            }
            PdfFontManager.applyTypeface(addrPaint, context, data.storeAddress, isBold = false)
            canvas.drawText(data.storeAddress, margin, currentY + 10f, addrPaint)
            currentY += 16f
        }

        // 3. Invoice & Shift Metadata Bar
        val metaBoxY = currentY + 4f
        val metaBoxHeight = if (isThermal) 36f else 46f
        val metaBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = PdfPalette.SURFACE_VARIANT
            style = Paint.Style.FILL
        }
        val metaRect = RectF(margin, metaBoxY, margin + contentWidth, metaBoxY + metaBoxHeight)
        canvas.drawRoundRect(metaRect, 6f, 6f, metaBgPaint)

        val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = PdfPalette.TEXT_SECONDARY
            textSize = if (isThermal) 8.5f else 10.5f
        }

        val col1X = margin + 10f
        val col2X = margin + (contentWidth / 2f)

        // Line 1: Invoice No & Date
        val metaY1 = metaBoxY + (if (isThermal) 14f else 18f)
        PdfFontManager.applyTypeface(metaPaint, context, "Inv: #${data.invoiceNo}", isBold = true)
        canvas.drawText("Inv: #${data.invoiceNo}", col1X, metaY1, metaPaint)

        PdfFontManager.applyTypeface(metaPaint, context, "Date: ${data.date}", isBold = false)
        canvas.drawText("Date: ${data.date}", col2X, metaY1, metaPaint)

        // Line 2: Cashier & Shift ID
        val metaY2 = metaY1 + (if (isThermal) 14f else 18f)
        PdfFontManager.applyTypeface(metaPaint, context, "Cashier: ${data.cashier}", isBold = false)
        canvas.drawText("Cashier: ${data.cashier}", col1X, metaY2, metaPaint)

        val shiftLabel = if (data.shiftId.isNotBlank()) "Shift: #${data.shiftId}" else "Status: Closed"
        PdfFontManager.applyTypeface(metaPaint, context, shiftLabel, isBold = false)
        canvas.drawText(shiftLabel, col2X, metaY2, metaPaint)

        currentY = metaBoxY + metaBoxHeight + 14f

        // Separator Line
        val divPaint = Paint().apply {
            color = PdfPalette.DIVIDER
            strokeWidth = 1f
        }
        canvas.drawLine(margin, currentY, margin + contentWidth, currentY, divPaint)

        return currentY + 10f
    }
}
