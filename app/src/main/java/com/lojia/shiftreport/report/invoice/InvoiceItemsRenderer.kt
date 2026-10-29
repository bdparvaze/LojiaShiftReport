package com.lojia.shiftreport.report.invoice

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.lojia.shiftreport.util.PdfFontManager
import com.lojia.shiftreport.util.PdfPalette

/**
 * Renders the invoice line items table with RTL support for Arabic descriptions.
 * Columns: No, Description, Qty, Price, Total.
 */
object InvoiceItemsRenderer {

    fun drawItemsTable(
        canvas: Canvas,
        context: Context,
        data: InvoiceData,
        pageWidth: Float,
        startY: Float
    ): Float {
        var currentY = startY
        val isThermal = data.paperSize.isThermal
        val margin = if (isThermal) 12f else 32f
        val tableWidth = pageWidth - (margin * 2)

        val headerHeight = if (isThermal) 20f else 26f
        val rowHeight = if (isThermal) 18f else 22f

        // Table Header Background
        val headerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = PdfPalette.PRIMARY_CONTAINER
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(
            RectF(margin, currentY, margin + tableWidth, currentY + headerHeight),
            4f, 4f, headerBgPaint
        )

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = PdfPalette.PRIMARY_DARK
            textSize = if (isThermal) 8.5f else 10.5f
        }
        PdfFontManager.applyTypeface(headerPaint, context, "Description", isBold = true)

        // Column Layout Calculations
        val colNoW = if (isThermal) 18f else 28f
        val colQtyW = if (isThermal) 24f else 40f
        val colPriceW = if (isThermal) 36f else 65f
        val colTotalW = if (isThermal) 38f else 75f
        val colDescW = tableWidth - (colNoW + colQtyW + colPriceW + colTotalW)

        val colNoX = margin + 4f
        val colDescX = colNoX + colNoW
        val colQtyX = colDescX + colDescW
        val colPriceX = colQtyX + colQtyW
        val colTotalX = colPriceX + colPriceW

        val headerTextY = currentY + (headerHeight / 2f) - ((headerPaint.descent() + headerPaint.ascent()) / 2f)

        canvas.drawText("#", colNoX, headerTextY, headerPaint)
        canvas.drawText("Description / الوصف", colDescX, headerTextY, headerPaint)
        canvas.drawText("Qty", colQtyX, headerTextY, headerPaint)
        canvas.drawText("Price", colPriceX, headerTextY, headerPaint)
        canvas.drawText("Total", colTotalX, headerTextY, headerPaint)

        currentY += headerHeight + 6f

        // Table Body Rows
        val rowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = PdfPalette.TEXT_PRIMARY
            textSize = if (isThermal) 8f else 10f
        }

        val dividerPaint = Paint().apply {
            color = PdfPalette.DIVIDER
            strokeWidth = 0.5f
        }

        if (data.items.isEmpty()) {
            val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = PdfPalette.TEXT_HINT
                textSize = if (isThermal) 8.5f else 10.5f
            }
            PdfFontManager.applyTypeface(emptyPaint, context, "No individual line items registered", isBold = false)
            canvas.drawText("Shift summary consolidation (No individual items)", colDescX, currentY + 12f, emptyPaint)
            currentY += 22f
        } else {
            data.items.forEachIndexed { index, item ->
                val rowTextY = currentY + 13f

                // Item number
                PdfFontManager.applyTypeface(rowPaint, context, "${index + 1}", isBold = false)
                canvas.drawText("${index + 1}", colNoX, rowTextY, rowPaint)

                // Item Description (RTL support if Arabic)
                val desc = when {
                    item.descriptionArabic.isNotBlank() -> "${item.description} / ${item.descriptionArabic}"
                    else -> item.description
                }
                PdfFontManager.applyTypeface(rowPaint, context, desc, isBold = false)
                canvas.drawText(desc.take(if (isThermal) 16 else 32), colDescX, rowTextY, rowPaint)

                // Quantity
                val qtyStr = if (item.qty % 1.0 == 0.0) "${item.qty.toInt()}" else String.format("%.1f", item.qty)
                PdfFontManager.applyTypeface(rowPaint, context, qtyStr, isBold = false)
                canvas.drawText(qtyStr, colQtyX, rowTextY, rowPaint)

                // Price
                val priceStr = String.format("%.2f", item.unitPrice)
                PdfFontManager.applyTypeface(rowPaint, context, priceStr, isBold = false)
                canvas.drawText(priceStr, colPriceX, rowTextY, rowPaint)

                // Total
                val totalStr = String.format("%.2f", item.total)
                PdfFontManager.applyTypeface(rowPaint, context, totalStr, isBold = true)
                canvas.drawText(totalStr, colTotalX, rowTextY, rowPaint)

                currentY += rowHeight
                canvas.drawLine(margin, currentY, margin + tableWidth, currentY, dividerPaint)
                currentY += 2f
            }
        }

        return currentY + 8f
    }
}
