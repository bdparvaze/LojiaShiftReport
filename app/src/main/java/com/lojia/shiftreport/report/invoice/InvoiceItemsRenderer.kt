package com.lojia.shiftreport.report.invoice

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.lojia.shiftreport.util.PdfFontManager
import com.lojia.shiftreport.util.PdfPalette

/**
 * Renders the invoice line items table with alternating row colors,
 * PRIMARY_DARK header, Arabic RTL alignment support, and page overflow protection.
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
        val rowHeight = if (isThermal) 18f else 24f

        // 1. Table Header Background (PRIMARY_DARK)
        val headerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = PdfPalette.PRIMARY_DARK
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(
            RectF(margin, currentY, margin + tableWidth, currentY + headerHeight),
            4f, 4f, headerBgPaint
        )

        // Header Text Paint (ON_PRIMARY White)
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = PdfPalette.ON_PRIMARY
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

        currentY += headerHeight + 2f

        // 2. Table Body Rows with Alternating Colors
        val rowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = PdfPalette.TEXT_PRIMARY
            textSize = if (isThermal) 8f else 10f
        }

        val altBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = PdfPalette.SURFACE_VARIANT
            style = Paint.Style.FILL
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
            canvas.drawText("Shift summary consolidation (No individual items)", colDescX, currentY + 14f, emptyPaint)
            currentY += 24f
        } else {
            data.items.forEachIndexed { index, item ->
                // Page overflow guard for A4
                if (currentY > 700f && !isThermal) {
                    return currentY
                }

                // Alternating row background
                if (index % 2 == 1) {
                    canvas.drawRect(
                        RectF(margin, currentY, margin + tableWidth, currentY + rowHeight),
                        altBgPaint
                    )
                }

                val rowTextY = currentY + (rowHeight / 2f) - ((rowPaint.descent() + rowPaint.ascent()) / 2f)

                // Item number
                PdfFontManager.applyTypeface(rowPaint, context, "${index + 1}", isBold = false)
                canvas.drawText("${index + 1}", colNoX, rowTextY, rowPaint)

                // Item Description (RTL support if Arabic)
                val desc = when {
                    item.descriptionArabic.isNotBlank() -> "${item.description} / ${item.descriptionArabic}"
                    else -> item.description
                }
                val isArabic = PdfFontManager.isArabic(desc)
                PdfFontManager.applyTypeface(rowPaint, context, desc, isBold = false)

                val truncatedDesc = desc.take(if (isThermal) 16 else 32)
                if (isArabic) {
                    // RTL alignment towards right of description column
                    val descWidth = rowPaint.measureText(truncatedDesc)
                    val rtlStartX = (colDescX + colDescW - 8f - descWidth).coerceAtLeast(colDescX)
                    canvas.drawText(truncatedDesc, rtlStartX, rowTextY, rowPaint)
                } else {
                    canvas.drawText(truncatedDesc, colDescX, rowTextY, rowPaint)
                }

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
            }
        }

        return currentY + 8f
    }
}
