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
                return Math.round(d * 100)
            }
            return value.toLong()
        }
        return default
    }

    private fun parseDueCreditEntries(jsonStr: String): List<DueCreditEntry> {
        val list = mutableListOf<DueCreditEntry>()
        try {
            if (jsonStr.isBlank() || jsonStr == "[]") return list
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    DueCreditEntry(
                        receiptNo = obj.optString("receiptNo", obj.optString("receipt", "-")),
                        customerName = obj.optString("customerName", "Receipt #${obj.optString("receiptNo", "")}"),
                        amount = getLongAmount(obj, "amount", 0L)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun parseDueCollectionEntries(jsonStr: String): List<DueCollectionEntry> {
        val list = mutableListOf<DueCollectionEntry>()
        try {
            if (jsonStr.isBlank() || jsonStr == "[]") return list
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    DueCollectionEntry(
                        receiptNo = obj.optString("receiptNo", obj.optString("receipt", "-")),
                        customerName = obj.optString("customerName", "Receipt #${obj.optString("receiptNo", "")}"),
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

    private fun getPdfStrings(context: Context, language: AppLanguage): PdfStrings {
        val locale = try {
            java.util.Locale.forLanguageTag(language.code)
        } catch (_: Exception) {
            java.util.Locale(language.code)
        }
        val config = android.content.res.Configuration(context.resources.configuration)
        config.setLocale(locale)
        val localizedContext = context.createConfigurationContext(config)

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
    fun generatePresetLogoBitmap(presetKey: String, size: Int = 200): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val r = size / 2f
        val padding = size * 0.05f

        val (colorStart, colorEnd, iconSymbol) = when (presetKey) {
            "preset:cafe" -> Triple(Color.rgb(180, 83, 9), Color.rgb(120, 53, 15), "☕")
            "preset:retail" -> Triple(Color.rgb(16, 185, 129), Color.rgb(5, 150, 105), "🛍️")
            "preset:fashion" -> Triple(Color.rgb(236, 72, 153), Color.rgb(190, 24, 93), "💎")
            "preset:pharmacy" -> Triple(Color.rgb(6, 182, 212), Color.rgb(14, 116, 144), "⚕️")
            "preset:tech" -> Triple(Color.rgb(99, 102, 241), Color.rgb(67, 56, 202), "⚡")
            "preset:restaurant" -> Triple(Color.rgb(249, 115, 22), Color.rgb(194, 65, 12), "🍴")
            else -> Triple(Color.rgb(30, 58, 138), Color.rgb(15, 23, 42), "🏪") // superstore
        }

        val shader = LinearGradient(0f, 0f, size.toFloat(), size.toFloat(), colorStart, colorEnd, Shader.TileMode.CLAMP)
        paint.shader = shader
        canvas.drawCircle(r, r, r - padding, paint)

        paint.shader = null
        paint.textSize = size * 0.46f
        paint.textAlign = Paint.Align.CENTER
        val textY = r - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText(iconSymbol, r, textY, paint)

        return bmp
    }

    fun getCircularBitmap(bitmap: Bitmap, diameter: Int): Bitmap {
        val output = Bitmap.createBitmap(diameter, diameter, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
        }
        val rectF = RectF(0f, 0f, diameter.toFloat(), diameter.toFloat())

        canvas.drawARGB(0, 0, 0, 0)
        canvas.drawCircle(diameter / 2f, diameter / 2f, diameter / 2f, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)

        val srcRect = if (bitmap.width > bitmap.height) {
            val offset = (bitmap.width - bitmap.height) / 2
            Rect(offset, 0, offset + bitmap.height, bitmap.height)
        } else {
            val offset = (bitmap.height - bitmap.width) / 2
            Rect(0, offset, bitmap.width, offset + bitmap.width)
        }
        canvas.drawBitmap(bitmap, srcRect, rectF, paint)
        return output
    }

    fun loadBusinessLogoBitmap(context: Context, logoUri: String?, targetSize: Int = 200): Bitmap? {
        if (logoUri.isNullOrBlank()) return null
        return try {
            if (logoUri.startsWith("preset:")) {
                generatePresetLogoBitmap(logoUri, targetSize)
            } else if (logoUri.startsWith("content://") || logoUri.startsWith("file://")) {
                val uri = Uri.parse(logoUri)
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            } else {
                val file = File(logoUri)
                if (file.exists()) {
                    BitmapFactory.decodeFile(file.absolutePath)
                } else null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun drawBusinessLogoBadge(
        canvas: Canvas,
        context: Context,
        logoUri: String?,
        bizName: String,
        x: Float,
        y: Float,
        size: Float = 54f
    ) {
        val radius = size / 2f
        val centerX = x + radius
        val centerY = y + radius

        val bgPaint = Paint().apply {
            color = PdfPalette.SURFACE
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        canvas.drawCircle(centerX, centerY, radius, bgPaint)

        val borderPaint = Paint().apply {
            color = PdfPalette.DIVIDER
            strokeWidth = 1.5f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        canvas.drawCircle(centerX, centerY, radius, borderPaint)

        val bmp = loadBusinessLogoBitmap(context, logoUri, 250)
        if (bmp != null) {
            val innerPadding = 2.5f
            val innerDiameter = ((radius - innerPadding) * 2f).toInt().coerceAtLeast(1)
            val circularBmp = getCircularBitmap(bmp, innerDiameter)
            val drawLeft = centerX - (innerDiameter / 2f)
            val drawTop = centerY - (innerDiameter / 2f)
            canvas.drawBitmap(circularBmp, drawLeft, drawTop, Paint().apply { isAntiAlias = true; isFilterBitmap = true })
        } else {
            val innerCirclePaint = Paint().apply {
                color = PdfPalette.PRIMARY_CONTAINER
                isAntiAlias = true
            }
            canvas.drawCircle(centerX, centerY, radius - 2.5f, innerCirclePaint)

            val initial = bizName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "L"
            val textPaint = Paint().apply {
                color = PdfPalette.PRIMARY_DARK
                textSize = size * 0.44f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            val textY = centerY - (textPaint.descent() + textPaint.ascent()) / 2f
            canvas.drawText(initial, centerX, textY, textPaint)
        }
    }

    private fun generateStyledQrCodeBitmap(text: String, targetPixelSize: Int = 400): Bitmap? {
        return try {
            val hints = mapOf(
                com.google.zxing.EncodeHintType.MARGIN to 1,
                com.google.zxing.EncodeHintType.ERROR_CORRECTION to com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.H,
                com.google.zxing.EncodeHintType.CHARACTER_SET to "UTF-8"
            )
            val writer = com.google.zxing.qrcode.QRCodeWriter()
            val bitMatrix = writer.encode(text, com.google.zxing.BarcodeFormat.QR_CODE, 0, 0, hints)
            val matrixWidth = bitMatrix.width
            val matrixHeight = bitMatrix.height

            val bmp = Bitmap.createBitmap(targetPixelSize, targetPixelSize, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            canvas.drawColor(PdfPalette.SURFACE)

            val moduleSize = targetPixelSize.toFloat() / matrixWidth

            // Charcoal dark slate minimalist color matching modern design guidelines
            val primaryPaint = Paint().apply {
                color = PdfPalette.QR_DARK
                isAntiAlias = true
                style = Paint.Style.FILL
            }

            fun isFinderPattern(x: Int, y: Int): Boolean {
                if (x < 7 && y < 7) return true
                if (x >= matrixWidth - 7 && y < 7) return true
                if (x < 7 && y >= matrixHeight - 7) return true
                return false
            }

            val centerX = matrixWidth / 2
            val centerY = matrixHeight / 2
            val logoRadiusModules = 3.5 // Clear center area for logo

            fun isCenterArea(x: Int, y: Int): Boolean {
                val dx = x - centerX
                val dy = y - centerY
                return (dx * dx + dy * dy) <= (logoRadiusModules * logoRadiusModules)
            }

            // 1. Draw organic connected rounded capsule pill modules
            val r = moduleSize * 0.45f
            for (y in 0 until matrixHeight) {
                for (x in 0 until matrixWidth) {
                    if (isFinderPattern(x, y) || isCenterArea(x, y)) continue
                    if (bitMatrix.get(x, y)) {
                        val hasTop = (y > 0 && bitMatrix.get(x, y - 1) && !isFinderPattern(x, y - 1) && !isCenterArea(x, y - 1))
                        val hasBottom = (y < matrixHeight - 1 && bitMatrix.get(x, y + 1) && !isFinderPattern(x, y + 1) && !isCenterArea(x, y + 1))
                        val hasLeft = (x > 0 && bitMatrix.get(x - 1, y) && !isFinderPattern(x - 1, y) && !isCenterArea(x - 1, y))
                        val hasRight = (x < matrixWidth - 1 && bitMatrix.get(x + 1, y) && !isFinderPattern(x + 1, y) && !isCenterArea(x + 1, y))

                        val left = x * moduleSize
                        val top = y * moduleSize
                        val right = left + moduleSize
                        val bottom = top + moduleSize

                        val tl = if (!hasTop && !hasLeft) r else 0f
                        val tr = if (!hasTop && !hasRight) r else 0f
                        val br = if (!hasBottom && !hasRight) r else 0f
                        val bl = if (!hasBottom && !hasLeft) r else 0f

                        val radii = floatArrayOf(tl, tl, tr, tr, br, br, bl, bl)
                        val path = Path()
                        path.addRoundRect(RectF(left, top, right, bottom), radii, Path.Direction.CW)
                        canvas.drawPath(path, primaryPaint)
                    }
                }
            }

            // 2. Draw modern squircle Finder Eye Patterns at 3 corners
            fun drawFinderEye(startX: Int, startY: Int) {
                val left = startX * moduleSize
                val top = startY * moduleSize
                val eyeSize = 7 * moduleSize

                // Outer 7x7 rounded squircle
                val outerRect = RectF(left, top, left + eyeSize, top + eyeSize)
                val outerRadius = eyeSize * 0.32f
                canvas.drawRoundRect(outerRect, outerRadius, outerRadius, primaryPaint)

                // Inner white 5x5 cutout
                val whiteRect = RectF(left + moduleSize, top + moduleSize, left + 6 * moduleSize, top + 6 * moduleSize)
                val whitePaint = Paint().apply { color = PdfPalette.SURFACE; isAntiAlias = true; style = Paint.Style.FILL }
                val whiteRadius = (5 * moduleSize) * 0.28f
                canvas.drawRoundRect(whiteRect, whiteRadius, whiteRadius, whitePaint)

                // Center 3x3 pupil squircle
                val pupilRect = RectF(left + 2 * moduleSize, top + 2 * moduleSize, left + 5 * moduleSize, top + 5 * moduleSize)
                val pupilRadius = (3 * moduleSize) * 0.35f
                canvas.drawRoundRect(pupilRect, pupilRadius, pupilRadius, primaryPaint)
            }

            drawFinderEye(0, 0)
            drawFinderEye(matrixWidth - 7, 0)
            drawFinderEye(0, matrixHeight - 7)

            // 3. Draw central aesthetic logo emblem overlay (matching reference design)
            val centerPxX = targetPixelSize / 2f
            val centerPxY = targetPixelSize / 2f
            val badgeRadius = logoRadiusModules.toFloat() * moduleSize

            // Outer white mask circle
            canvas.drawCircle(centerPxX, centerPxY, badgeRadius, Paint().apply { color = PdfPalette.SURFACE; isAntiAlias = true })

            // Outer dark ring
            val ringPaint = Paint().apply {
                color = PdfPalette.QR_DARK
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeWidth = moduleSize * 0.7f
            }
            canvas.drawCircle(centerPxX, centerPxY, badgeRadius - (moduleSize * 0.35f), ringPaint)

            // Inner dark pupil circle / logo emblem core
            canvas.drawCircle(centerPxX, centerPxY, badgeRadius * 0.45f, primaryPaint)

            bmp
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun extractStartingCashFromNotes(report: ShiftReport): Double {
        if (report.openingCash > 0L) return MoneyFormat.toMajorUnits(report.openingCash)
        val regex = Regex("""(?:Starting Cash|Starting Float|Float|Opening Cash)[:=]\s*([0-9]+(?:\.[0-9]+)?)""", RegexOption.IGNORE_CASE)
        return regex.find(report.notes)?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
    }

    fun extractStartingCashFromNotes(notes: String): Double? {
        val regex = Regex("""(?:Starting Cash|Starting Float|Float|Opening Cash)[:=]\s*([0-9]+(?:\.[0-9]+)?)""", RegexOption.IGNORE_CASE)
        return regex.find(notes)?.groupValues?.get(1)?.toDoubleOrNull()
    }

    fun extractActualCashFromNotes(report: ShiftReport): Double? {
        if (report.closingCash > 0L) return MoneyFormat.toMajorUnits(report.closingCash)
        val regex = Regex("""(?:Actual Cash Count|Actual Cash|Actual Count|Actual)[:=]\s*([0-9]+(?:\.[0-9]+)?)""", RegexOption.IGNORE_CASE)
        return regex.find(report.notes)?.groupValues?.get(1)?.toDoubleOrNull()
    }

    fun extractActualCashFromNotes(notes: String): Double? {
        val regex = Regex("""(?:Actual Cash Count|Actual Cash|Actual Count|Actual)[:=]\s*([0-9]+(?:\.[0-9]+)?)""", RegexOption.IGNORE_CASE)
        return regex.find(notes)?.groupValues?.get(1)?.toDoubleOrNull()
    }

    fun extractStartingCashMinor(report: ShiftReport): Long {
        if (report.openingCash > 0L) return report.openingCash
        val regex = Regex("""(?:Starting Cash|Starting Float|Float|Opening Cash)[:=]\s*([0-9]+(?:\.[0-9]+)?)""", RegexOption.IGNORE_CASE)
        val doubleVal = regex.find(report.notes)?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
        return MoneyFormat.toMinorUnits(doubleVal)
    }

    fun extractStartingCashMinor(notes: String): Long? {
        val regex = Regex("""(?:Starting Cash|Starting Float|Float|Opening Cash)[:=]\s*([0-9]+(?:\.[0-9]+)?)""", RegexOption.IGNORE_CASE)
        val doubleVal = regex.find(notes)?.groupValues?.get(1)?.toDoubleOrNull() ?: return null
        return MoneyFormat.toMinorUnits(doubleVal)
    }

    fun extractActualCashMinor(report: ShiftReport): Long? {
        if (report.closingCash > 0L) return report.closingCash
        val regex = Regex("""(?:Actual Cash Count|Actual Cash|Actual Count|Actual)[:=]\s*([0-9]+(?:\.[0-9]+)?)""", RegexOption.IGNORE_CASE)
        val doubleVal = regex.find(report.notes)?.groupValues?.get(1)?.toDoubleOrNull() ?: return null
        return MoneyFormat.toMinorUnits(doubleVal)
    }

    fun extractActualCashMinor(notes: String): Long? {
        val regex = Regex("""(?:Actual Cash Count|Actual Cash|Actual Count|Actual)[:=]\s*([0-9]+(?:\.[0-9]+)?)""", RegexOption.IGNORE_CASE)
        val doubleVal = regex.find(notes)?.groupValues?.get(1)?.toDoubleOrNull() ?: return null
        return MoneyFormat.toMinorUnits(doubleVal)
    }

    fun cleanDisplayNotes(notes: String): String {
        return notes
            .replace(Regex("""(?:Starting Cash|Starting Float|Float|Opening Cash)[:=]\s*[0-9]+(?:\.[0-9]+)?""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""(?:Actual Cash Count|Actual Cash|Actual Count|Actual)[:=]\s*[0-9]+(?:\.[0-9]+)?""", RegexOption.IGNORE_CASE), "")
            .split("|")
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.equals("Saved from Shift Closing Ledger", ignoreCase = true) }
            .joinToString(" | ")
    }

    private fun buildShiftReportQrText(
        context: Context,
        report: ShiftReport,
        businessProfile: BusinessProfile?,
        currency: String,
        language: AppLanguage = AppLanguage.fromCode(LanguagePreferences.getLanguage(context))
    ): String {
        val bizName = businessProfile?.businessName ?: context.getString(R.string.default_business_name)
        val vatNo = businessProfile?.vatNumber.orEmpty()
        val pStr = getPdfStrings(context, language)

        val startingCash = extractStartingCashFromNotes(report)
        val actualCashCount = extractActualCashFromNotes(report)
        val cashIn = MoneyFormat.toMajorUnits(report.totalDueCollectedCash)
        val cashOut = MoneyFormat.toMajorUnits(report.totalCashOut)
        val expectedCashInDrawer = startingCash + MoneyFormat.toMajorUnits(report.grossCash) + cashIn - cashOut
        val variance = actualCashCount?.let { it - expectedCashInDrawer }

        val dueCreditEntries = parseDueCreditEntries(report.dueCreditEntriesJson)
        val displayNotes = cleanDisplayNotes(report.notes)

        return buildString {
            appendLine("=== $bizName ===")
            if (vatNo.isNotBlank()) {
                appendLine("VAT ID: $vatNo")
            }
            appendLine("${pStr.reportId}: #${report.id.toString().padStart(6, '0')}")
            appendLine("${pStr.date}: ${DateTimeFormatUtils.formatIsoDateTime(report.dateInMillis)}")
            appendLine("${pStr.shift}: ${getLocalizedShiftName(language, report.shift)} | ${pStr.cashier}: ${report.cashierName}")
            appendLine("--------------------------------")
            appendLine("[1. SALES SUMMARY]")
            appendLine("${pStr.cashSales}: ${MoneyFormat.formatMinor(report.grossCash, currency)}")
            appendLine("${pStr.cardMadaSales}: ${MoneyFormat.formatMinor(report.madaPayments, currency)}")
            if (report.digitalWallet > 0L) {
                appendLine("${pStr.digitalWallet}: ${MoneyFormat.formatMinor(report.digitalWallet, currency)}")
            }
            appendLine("${pStr.grossTotalSales}: ${MoneyFormat.formatMinor(report.totalSales, currency)}")
            appendLine("--------------------------------")
            appendLine("[2. CASH DRAWER RECONCILIATION]")
            if (startingCash > 0) {
                appendLine("${pStr.startingCash}: ${MoneyFormat.format(startingCash, currency)}")
            }
            appendLine("(+) ${pStr.cashSales}: ${MoneyFormat.formatMinor(report.grossCash, currency)}")
            if (cashIn > 0) {
                appendLine("(+) ${pStr.cashIn}: ${MoneyFormat.format(cashIn, currency)}")
            }
            appendLine("(-) ${pStr.cashOut}: ${MoneyFormat.format(cashOut, currency)}")
            appendLine("${pStr.expectedCashInDrawer}: ${MoneyFormat.format(expectedCashInDrawer, currency)}")
            if (actualCashCount != null) {
                appendLine("${pStr.actualCashCount}: ${MoneyFormat.format(actualCashCount, currency)}")
                val varStr = if (variance != null) {
                    when {
                        variance == 0.0 -> "0.00 (Balanced)"
                        variance > 0.0 -> "+${MoneyFormat.format(variance, currency)} (Over)"
                        else -> "-${MoneyFormat.format(Math.abs(variance), currency)} (Short)"
                    }
                } else "N/A"
                appendLine("${pStr.variance}: $varStr")
            }
            appendLine("--------------------------------")
            appendLine("[3. OTHER TRACKING]")
            appendLine("${pStr.dueSales}: ${MoneyFormat.formatMinor(report.totalDueCredit, currency)} (${dueCreditEntries.size})")
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
        val vatNo = businessProfile?.vatNumber.orEmpty()
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
        canvas.drawText(context.getString(R.string.pdf_vat_tel, vatNo, phone), margin + 14f, currentY + 40f, subheaderTextPaint)
        canvas.drawText(context.getString(R.string.pdf_address, businessProfile?.address ?: context.getString(R.string.pdf_default_address)), margin + 14f, currentY + 54f, subheaderTextPaint)

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
        canvas.drawText(context.getString(R.string.detailed_shift_records), margin, currentY, textBoldPaint)
        currentY += 8f

        currentY = drawSummaryTableHeader(currentY)

        // Draw ALL rows with multi-page pagination
        reports.forEachIndexed { index, report ->
            if (currentY + 18f > PAGE_HEIGHT - 55f) {
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

            val cashValStr = context.getString(R.string.msg_2f_2).format(report.grossCash)
            canvas.drawText(cashValStr, colCashRight - textPaint.measureText(cashValStr), rowY, textPaint)

            val madaValStr = context.getString(R.string.msg_2f_2).format(report.madaPayments + report.digitalWallet)
            canvas.drawText(madaValStr, colMadaRight - textPaint.measureText(madaValStr), rowY, textPaint)

            val expValStr = context.getString(R.string.msg_2f_2).format(report.totalExpenses)
            canvas.drawText(expValStr, colExpRight - textPaint.measureText(expValStr), rowY, textPaint)

            val totValStr = context.getString(R.string.msg_2f_2).format(report.totalSales)
            canvas.drawText(totValStr, colTotalRight - textBoldPaint.measureText(totValStr), rowY, textBoldPaint)

            canvas.drawLine(margin, currentY + 18f, PAGE_WIDTH - margin, currentY + 18f, linePaint)
            currentY += 18f
        }

        // Ensure space for Totals + Operational Metrics + Signatures
        if (currentY + 130f > PAGE_HEIGHT - 45f) {
            drawSummaryFooter()
            pdfDocument.finishPage(page)

            pageNum++
            pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            currentY = margin
        }

        // Totals Footer Row
        val totalRect = RectF(margin, currentY + 4f, PAGE_WIDTH - margin, currentY + 26f)
        canvas.drawRoundRect(totalRect, 4f, 4f, totalBoxPaint)

        val totalY = currentY + 19f
        canvas.drawText(context.getString(R.string.grand_totals), colDate, totalY, textBoldPaint)

        val totCashStr = context.getString(R.string.msg_2f_2).format(MoneyFormat.toMajorUnits(totalGrossCash))
        canvas.drawText(totCashStr, colCashRight - textBoldPaint.measureText(totCashStr), totalY, textBoldPaint)

        val totMadaStr = context.getString(R.string.msg_2f_2).format(MoneyFormat.toMajorUnits(totalMada + totalWallet))
        canvas.drawText(totMadaStr, colMadaRight - textBoldPaint.measureText(totMadaStr), totalY, textBoldPaint)

        val totExpStr = context.getString(R.string.msg_2f_2).format(MoneyFormat.toMajorUnits(totalExp))
        canvas.drawText(totExpStr, colExpRight - textBoldPaint.measureText(totExpStr), totalY, textBoldPaint)

        val totRevStr = context.getString(R.string.msg_2f_s_21).format(MoneyFormat.toMajorUnits(totalGrossRevenue), currency)
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
     * Generates an individual Shift Report PDF document following international retail POS standards
     * (Square / Loyverse / Lightspeed style), with clean 3-section layout, audit-ready reconciliation,
     * multi-entry breakdown tables, multi-language support, and multi-page pagination.
     */
    fun generateSingleShiftReportPdf(
        context: Context,
        report: ShiftReport,
        businessProfile: BusinessProfile?,
        language: AppLanguage = AppLanguage.ENGLISH
    ): ExportResult {
        val pStr = getPdfStrings(context, language)
        val currency = MoneyFormat.resolveCurrency(businessProfile?.currency)

        val startingCash = extractStartingCashMinor(report)
        val actualCashCount = extractActualCashMinor(report)
        val cashIn = report.totalDueCollectedCash
        val cashOut = report.totalCashOut
        val expectedCashInDrawer = startingCash + report.grossCash + cashIn - cashOut
        val variance = actualCashCount?.let { it - expectedCashInDrawer }

        val dueCreditEntries = parseDueCreditEntries(report.dueCreditEntriesJson)
        val dueCollectionEntries = parseDueCollectionEntries(report.previousDueCollectionsJson)
        val staffAdvanceEntries = parseStaffAdvanceEntries(report.staffAdvancesJson)
        val walkoutEntries = parseWalkoutEntries(report.unpaidBillsJson)
        val purchasedItems = parsePurchasedItems(report.purchasedItemsJson)
        val displayNotes = cleanDisplayNotes(report.notes)

        val pdfDocument = PdfDocument()
        var pageNum = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        val primaryPaint = Paint().apply {
            color = PdfPalette.PRIMARY_DARK
            isAntiAlias = true
        }
        val headerTextPaint = Paint().apply {
            color = PdfPalette.ON_PRIMARY
            textSize = 14f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        val subheaderTextPaint = Paint().apply {
            color = PdfPalette.PRIMARY_CONTAINER
            textSize = 9f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }
        val sectionTitlePaint = Paint().apply {
            color = PdfPalette.TEXT_PRIMARY
            textSize = 11.5f
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
        val secondaryPaint = Paint().apply {
            color = PdfPalette.TEXT_HINT
            textSize = 8.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }
        val linePaint = Paint().apply {
            color = PdfPalette.DIVIDER
            strokeWidth = 1f
            isAntiAlias = true
        }
        val boxBgPaint = Paint().apply {
            color = PdfPalette.BACKGROUND
            isAntiAlias = true
        }
        val highlightBgPaint = Paint().apply {
            color = PdfPalette.SUCCESS_CONTAINER
            isAntiAlias = true
        }
        val warningBgPaint = Paint().apply {
            color = PdfPalette.WARNING_CONTAINER
            isAntiAlias = true
        }
        val errorBgPaint = Paint().apply {
            color = PdfPalette.ERROR_CONTAINER
            isAntiAlias = true
        }
        val emeraldPaint = Paint().apply {
            color = PdfPalette.SUCCESS
            textSize = 10.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        val warningPaint = Paint().apply {
            color = PdfPalette.WARNING
            textSize = 10.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        val rosePaint = Paint().apply {
            color = PdfPalette.ERROR
            textSize = 10.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
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

        val margin = 36f
        var currentY = margin

        fun drawFooter() {
            val footerY = PAGE_HEIGHT - 22f
            canvas.drawLine(margin, footerY - 8f, PAGE_WIDTH - margin, footerY - 8f, linePaint)
            val genTimeStr = "${context.getString(R.string.pdf_date)}: ${DateTimeFormatUtils.formatIsoDateTimeSeconds()}"
            canvas.drawText(genTimeStr, margin, footerY + 2f, secondaryPaint)
            val noticeStr = pStr.computerGeneratedNotice
            val noticeWidth = secondaryPaint.measureText(noticeStr)
            canvas.drawText(noticeStr, (PAGE_WIDTH - noticeWidth) / 2f, footerY + 2f, secondaryPaint)
            val pageStr = "${pStr.page} $pageNum"
            val pageWidth = secondaryPaint.measureText(pageStr)
            canvas.drawText(pageStr, PAGE_WIDTH - margin - pageWidth, footerY + 2f, secondaryPaint)
        }

        fun ensureSpace(neededHeight: Float) {
            if (currentY + neededHeight > PAGE_HEIGHT - 45f) {
                drawFooter()
                pdfDocument.finishPage(page)

                pageNum++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas

                currentY = margin

                // Draw Top Mini Header on new page
                val miniBox = RectF(margin, currentY, PAGE_WIDTH - margin, currentY + 24f)
                canvas.drawRoundRect(miniBox, 4f, 4f, primaryPaint)
                canvas.drawText(
                    "${businessProfile?.businessName ?: context.getString(R.string.default_business_name)} — ${pStr.title} (${pStr.page} $pageNum)",
                    margin + 10f,
                    currentY + 16f,
                    headerTextPaint.apply { textSize = 10.5f }
                )
                headerTextPaint.textSize = 14f
                currentY += 34f
            }
        }

        // ==========================================
        // 1. HEADER (Business Info, Logo, Metadata)
        // ==========================================
        val headerHeight = 72f
        val headerRect = RectF(margin, currentY, PAGE_WIDTH - margin, currentY + headerHeight)
        canvas.drawRoundRect(headerRect, 8f, 8f, primaryPaint)

        val bizName = businessProfile?.businessName ?: context.getString(R.string.default_business_name)
        val vatNo = businessProfile?.vatNumber.orEmpty()
        val phone = businessProfile?.phone.orEmpty()

        val logoSize = 52f
        val logoX = PAGE_WIDTH - margin - logoSize - 12f
        val logoY = currentY + (headerHeight - logoSize) / 2f
        drawBusinessLogoBadge(canvas, context, businessProfile?.logoUri, bizName, logoX, logoY, logoSize)

        canvas.drawText(bizName, margin + 14f, currentY + 24f, headerTextPaint)
        val metaParts = mutableListOf<String>()
        if (vatNo.isNotBlank()) metaParts.add("VAT: $vatNo")
        if (phone.isNotBlank()) metaParts.add("Tel: $phone")
        val metaLine = metaParts.joinToString(" · ")
        if (metaLine.isNotBlank()) {
            canvas.drawText(metaLine, margin + 14f, currentY + 42f, subheaderTextPaint)
        }
        val address = businessProfile?.address.orEmpty()
        if (address.isNotBlank()) {
            canvas.drawText(address, margin + 14f, currentY + 58f, subheaderTextPaint)
        }

        val badgeText = pStr.officialReport
        val badgeWidth = subheaderTextPaint.measureText(badgeText)
        canvas.drawText(badgeText, logoX - badgeWidth - 12f, currentY + 24f, subheaderTextPaint)

        currentY += headerHeight + 14f

        // Shift Metadata Card + QR Code
        val infoBoxHeight = 78f
        val infoBox = RectF(margin, currentY, PAGE_WIDTH - margin, currentY + infoBoxHeight)
        canvas.drawRoundRect(infoBox, 6f, 6f, boxBgPaint)
        canvas.drawRoundRect(infoBox, 6f, 6f, Paint().apply { color = PdfPalette.DIVIDER; style = Paint.Style.STROKE; strokeWidth = 1f; isAntiAlias = true })

        // Left Shift info
        canvas.drawText("${pStr.reportId}: #${report.id.toString().padStart(6, '0')}", margin + 12f, currentY + 20f, textBoldPaint)
        canvas.drawText("${pStr.shift}: ${getLocalizedShiftName(language, report.shift)}", margin + 12f, currentY + 40f, textBoldPaint)
        canvas.drawText("${pStr.cashier}: ${report.cashierName}", margin + 12f, currentY + 60f, textBoldPaint)

        // Middle Shift info
        canvas.drawText("${pStr.date}: ${DateTimeFormatUtils.formatIsoDateTimeSeconds(report.dateInMillis)}", margin + 170f, currentY + 20f, textPaint)
        canvas.drawText(context.getString(R.string.official_digital_verification), margin + 170f, currentY + 40f, secondaryPaint)

        // Right QR Container Card
        val qrCardSize = 68f
        val qrCardRight = PAGE_WIDTH - margin - 6f
        val qrCardLeft = qrCardRight - qrCardSize
        val qrCardTop = currentY + (infoBoxHeight - qrCardSize) / 2f
        val qrCardBottom = qrCardTop + qrCardSize

        val qrCardBg = RectF(qrCardLeft, qrCardTop, qrCardRight, qrCardBottom)
        canvas.drawRoundRect(qrCardBg, 5f, 5f, Paint().apply { color = PdfPalette.SURFACE; style = Paint.Style.FILL; isAntiAlias = true })
        canvas.drawRoundRect(qrCardBg, 5f, 5f, Paint().apply { color = PdfPalette.BORDER; style = Paint.Style.STROKE; strokeWidth = 1f; isAntiAlias = true })

        val qrText = buildShiftReportQrText(context, report, businessProfile, currency, language)
        val qrBitmap = generateStyledQrCodeBitmap(qrText, 350)
        if (qrBitmap != null) {
            val qrPadding = 4f
            val qrImageSize = qrCardSize - (qrPadding * 2f) - 8f
            val qrImageLeft = qrCardLeft + (qrCardSize - qrImageSize) / 2f
            val qrImageTop = qrCardTop + qrPadding

            val srcRect = Rect(0, 0, qrBitmap.width, qrBitmap.height)
            val dstRectF = RectF(qrImageLeft, qrImageTop, qrImageLeft + qrImageSize, qrImageTop + qrImageSize)
            canvas.drawBitmap(qrBitmap, srcRect, dstRectF, null)

            val captionPaint = Paint().apply {
                color = PdfPalette.PRIMARY_DARK
                textSize = 5f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                isAntiAlias = true
            }
            val scanLabel = context.getString(R.string.scan_data_label)
            val scanWidth = captionPaint.measureText(scanLabel)
            canvas.drawText(scanLabel, qrCardLeft + (qrCardSize - scanWidth) / 2f, qrCardBottom - 2.5f, captionPaint)
        }

        currentY += infoBoxHeight + 14f

        // Helper to draw section header pill
        fun drawSectionHeader(sectionNum: String, title: String) {
            ensureSpace(28f)
            val pillWidth = 24f
            val pillRect = RectF(margin, currentY, margin + pillWidth, currentY + 16f)
            canvas.drawRoundRect(pillRect, 3f, 3f, primaryPaint)
            val pillNumPaint = Paint().apply { color = PdfPalette.ON_PRIMARY; textSize = 8.5f; typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD); isAntiAlias = true }
            val numW = pillNumPaint.measureText(sectionNum)
            canvas.drawText(sectionNum, margin + (pillWidth - numW) / 2f, currentY + 11.5f, pillNumPaint)

            canvas.drawText(title, margin + pillWidth + 8f, currentY + 12.5f, sectionTitlePaint)
            currentY += 22f
        }

        // Helper to draw clean financial table row
        fun drawDataRow(
            label: String,
            valueStr: String,
            isBold: Boolean = false,
            isHighlight: Boolean = false,
            customPaint: Paint? = null,
            customBgPaint: Paint? = null,
            indent: Float = 0f
        ) {
            ensureSpace(19f)
            val rowRect = RectF(margin, currentY, PAGE_WIDTH - margin, currentY + 18f)
            if (customBgPaint != null) {
                canvas.drawRoundRect(rowRect, 3f, 3f, customBgPaint)
            } else if (isHighlight) {
                canvas.drawRoundRect(rowRect, 3f, 3f, highlightBgPaint)
            } else if (isBold) {
                canvas.drawRect(rowRect, tableHeaderPaint)
            }
            canvas.drawLine(margin, currentY + 18f, PAGE_WIDTH - margin, currentY + 18f, linePaint)

            val lp = if (isBold) textBoldPaint else textPaint
            val vp = customPaint ?: if (isHighlight) emeraldPaint else lp

            canvas.drawText(label, margin + 10f + indent, currentY + 12.5f, lp)
            val vWidth = vp.measureText(valueStr)
            canvas.drawText(valueStr, PAGE_WIDTH - margin - vWidth - 10f, currentY + 12.5f, vp)
            currentY += 18f
        }

        // ==========================================
        // SECTION 1: SALES SUMMARY (Payment Methods Only)
        // ==========================================
        drawSectionHeader("1", pStr.salesSummary)
        drawDataRow(pStr.cashSales, MoneyFormat.formatMinor(report.grossCash, currency))
        drawDataRow(pStr.cardMadaSales, MoneyFormat.formatMinor(report.madaPayments, currency))
        if (report.digitalWallet > 0L) {
            drawDataRow(pStr.digitalWallet, MoneyFormat.formatMinor(report.digitalWallet, currency))
        }
        drawDataRow(pStr.grossTotalSales, MoneyFormat.formatMinor(report.totalSales, currency), isBold = true, isHighlight = true)

        ensureSpace(14f)
        val salesNote = context.getString(R.string.pdf_sales_exclusion_note)
        canvas.drawText(salesNote, margin + 10f, currentY + 9f, secondaryPaint)
        currentY += 16f

        // ==========================================
        // SECTION 2: CASH DRAWER RECONCILIATION
        // ==========================================
        drawSectionHeader("2", pStr.cashDrawerReconciliation)
        drawDataRow(pStr.startingCash, MoneyFormat.formatMinor(startingCash, currency))
        drawDataRow("(+) ${pStr.cashSales}", "+ " + MoneyFormat.formatMinor(report.grossCash, currency))
        if (cashIn > 0L) {
            drawDataRow("(+) ${pStr.cashIn}", "+ " + MoneyFormat.formatMinor(cashIn, currency))
        }
        if (report.totalExpenses > 0L) {
            drawDataRow("   • ${pStr.expenses}", "- " + MoneyFormat.formatMinor(report.totalExpenses, currency), indent = 8f)
        }
        if (report.totalStaffAdvancesAmount > 0L) {
            drawDataRow("   • ${pStr.employerAdvances}", "- " + MoneyFormat.formatMinor(report.totalStaffAdvancesAmount, currency), indent = 8f)
        }
        if (report.totalPurchasedCash > 0L) {
            drawDataRow("   • ${pStr.paidOutItems}", "- " + MoneyFormat.formatMinor(report.totalPurchasedCash, currency), indent = 8f)
        }
        drawDataRow("(-) ${pStr.cashOut}", "- " + MoneyFormat.formatMinor(cashOut, currency), isBold = (cashOut > 0L && report.totalExpenses == 0L && report.totalStaffAdvancesAmount == 0L && report.totalPurchasedCash == 0L))

        // Expected Cash in Drawer
        drawDataRow(pStr.expectedCashInDrawer, MoneyFormat.formatMinor(expectedCashInDrawer, currency), isBold = true, isHighlight = true)

        // Actual Cash Count & Variance
        if (actualCashCount != null) {
            drawDataRow(pStr.actualCashCount, MoneyFormat.formatMinor(actualCashCount, currency), isBold = true)
            val varianceStr = when {
                variance == null -> "N/A"
                variance == 0L -> "${MoneyFormat.formatMinor(0L, currency)} (Balanced)"
                variance > 0L -> "+${MoneyFormat.formatMinor(variance, currency)} (Over)"
                else -> "-${MoneyFormat.formatMinor(Math.abs(variance), currency)} (Short)"
            }
            val varPaint = when {
                variance == null -> textPaint
                variance == 0L -> emeraldPaint
                variance > 0L -> warningPaint
                else -> rosePaint
            }
            val varBgPaint = when {
                variance == null -> null
                variance == 0L -> highlightBgPaint
                variance > 0L -> warningBgPaint
                else -> errorBgPaint
            }
            drawDataRow(pStr.variance, varianceStr, isBold = true, customPaint = varPaint, customBgPaint = varBgPaint)
        } else {
            drawDataRow(pStr.actualCashCount, "[ _______________________ ]", isBold = false)
            drawDataRow(pStr.variance, "[ _______________________ ] (Pending Count)", isBold = false)
        }

        currentY += 12f

        // ==========================================
        // SECTION 3: TAX & VAT SUMMARY
        // ==========================================
        val taxTitle = context.getString(R.string.pdf_tax_vat_summary_title)
        val netTaxableStr = context.getString(R.string.pdf_net_taxable_sales_label)
        val vatStr = context.getString(R.string.pdf_vat_amount_label)
        val grossTaxStr = context.getString(R.string.pdf_gross_total_sales_label)

        val isTax = businessProfile?.isTaxEnabled ?: false
        val isTaxIncluded = businessProfile?.isTaxIncluded ?: true
        val vatRate = if (isTax) (businessProfile?.vatRate ?: 15.0) else 0.0
        val (netTaxableVal, vatVal, grossTaxVal) = if (isTax && vatRate > 0.0) {
            if (isTaxIncluded) {
                val vat = MoneyFormat.calculateTaxMinor(report.totalSales, vatRate, true)
                val net = MoneyFormat.subtractMinor(report.totalSales, vat)
                Triple(net, vat, report.totalSales)
            } else {
                val vat = MoneyFormat.calculateTaxMinor(report.totalSales, vatRate, false)
                val gross = MoneyFormat.addMinor(report.totalSales, vat)
                Triple(report.totalSales, vat, gross)
            }
        } else {
            Triple(report.totalSales, 0L, report.totalSales)
        }

        drawSectionHeader("3", taxTitle)
        drawDataRow(netTaxableStr, MoneyFormat.formatMinor(netTaxableVal, currency))
        drawDataRow(vatStr, MoneyFormat.formatMinor(vatVal, currency))
        drawDataRow(grossTaxStr, MoneyFormat.formatMinor(grossTaxVal, currency), isBold = true, isHighlight = true)

        currentY += 12f

        // ==========================================
        // SECTION 4: OTHER TRACKING & OPERATIONAL METRICS
        // ==========================================
        drawSectionHeader("4", pStr.otherTracking)

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
            ensureSpace(26f)
            val noteBox = RectF(margin, currentY, PAGE_WIDTH - margin, currentY + 22f)
            canvas.drawRoundRect(noteBox, 4f, 4f, boxBgPaint)
            canvas.drawRoundRect(noteBox, 4f, 4f, Paint().apply { color = PdfPalette.DIVIDER; style = Paint.Style.STROKE; strokeWidth = 1f; isAntiAlias = true })
            canvas.drawText("${pStr.notes}: $displayNotes", margin + 10f, currentY + 14f, textPaint)
            currentY += 28f
        } else {
            currentY += 12f
        }

        // ==========================================
        // DETAIL BREAKDOWN TABLES (Multi-Page Safe)
        // ==========================================
        fun drawSectionTable(title: String, headers: List<String>, rows: List<List<String>>, totalAmountStr: String) {
            if (rows.isEmpty()) return
            ensureSpace(58f)

            canvas.drawText(title, margin, currentY, textBoldPaint)
            currentY += 6f

            val col1X = margin + 10f
            val col2X = margin + 180f
            val col3RightX = PAGE_WIDTH - margin - 10f
            val headerP = Paint().apply { color = PdfPalette.ON_PRIMARY; textSize = 8.5f; typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD); isAntiAlias = true }

            fun drawDetailHeader() {
                val tableH = RectF(margin, currentY, PAGE_WIDTH - margin, currentY + 17f)
                canvas.drawRect(tableH, primaryPaint)
                canvas.drawText(headers[0], col1X, currentY + 12f, headerP)
                if (headers.size > 1 && headers[1].isNotBlank()) {
                    canvas.drawText(headers[1], col2X, currentY + 12f, headerP)
                }
                if (headers.size > 2 && headers[2].isNotBlank()) {
                    val h3W = headerP.measureText(headers[2])
                    canvas.drawText(headers[2], col3RightX - h3W, currentY + 12f, headerP)
                }
                currentY += 17f
            }

            drawDetailHeader()

            // Data Rows
            rows.forEachIndexed { idx, row ->
                val pageBefore = pageNum
                ensureSpace(18f)
                if (pageNum != pageBefore) {
                    drawDetailHeader()
                }
                if (idx % 2 == 1) {
                    canvas.drawRect(RectF(margin, currentY, PAGE_WIDTH - margin, currentY + 17f), zebraPaint)
                }
                canvas.drawText(row[0], col1X, currentY + 12f, textPaint)
                if (row.size > 1 && row[1].isNotBlank()) {
                    canvas.drawText(row[1], col2X, currentY + 12f, textPaint)
                }
                if (row.size > 2) {
                    val w = textBoldPaint.measureText(row[2])
                    canvas.drawText(row[2], col3RightX - w, currentY + 12f, textBoldPaint)
                }
                canvas.drawLine(margin, currentY + 17f, PAGE_WIDTH - margin, currentY + 17f, linePaint)
                currentY += 17f
            }

            // Subtotal row
            ensureSpace(19f)
            canvas.drawRect(RectF(margin, currentY, PAGE_WIDTH - margin, currentY + 18f), tableHeaderPaint)
            canvas.drawText("${pStr.total}:", col1X, currentY + 13f, textBoldPaint)
            val totW = emeraldPaint.measureText(totalAmountStr)
            canvas.drawText(totalAmountStr, col3RightX - totW, currentY + 13f, emeraldPaint)
            canvas.drawLine(margin, currentY + 18f, PAGE_WIDTH - margin, currentY + 18f, linePaint)
            currentY += 22f
        }

        // Table 1: Due Sales (Credit Entries)
        drawSectionTable(
            title = pStr.dueSales,
            headers = listOf(pStr.receiptNo, pStr.customer, pStr.amount),
            rows = dueCreditEntries.map { listOf(it.receiptNo, it.customerName, MoneyFormat.formatMinor(it.amount, currency)) },
            totalAmountStr = MoneyFormat.formatMinor(dueCreditEntries.sumOf { it.amount }, currency)
        )

        // Table 2: Due Collection
        drawSectionTable(
            title = pStr.dueCollection,
            headers = listOf(pStr.receiptNo, pStr.mode, pStr.amount),
            rows = dueCollectionEntries.map { listOf(it.receiptNo, it.paymentMode, MoneyFormat.formatMinor(it.amount, currency)) },
            totalAmountStr = MoneyFormat.formatMinor(dueCollectionEntries.sumOf { it.amount }, currency)
        )

        // Table 3: Employer Advances
        drawSectionTable(
            title = pStr.employerAdvances,
            headers = listOf(pStr.staffName, pStr.mode, pStr.amount),
            rows = staffAdvanceEntries.map { listOf(it.staffName, it.paymentMode, MoneyFormat.formatMinor(it.amount, currency)) },
            totalAmountStr = MoneyFormat.formatMinor(staffAdvanceEntries.sumOf { it.amount }, currency)
        )

        // Table 4: Walkout Bills
        drawSectionTable(
            title = pStr.walkoutBills,
            headers = listOf(pStr.customer, "", pStr.amount),
            rows = walkoutEntries.map { listOf(it.tableOrOrderRef, "", MoneyFormat.formatMinor(it.amount, currency)) },
            totalAmountStr = MoneyFormat.formatMinor(walkoutEntries.sumOf { it.amount }, currency)
        )

        // Table 5: Paid Out Items / Purchases
        drawSectionTable(
            title = pStr.paidOutItems,
            headers = listOf(pStr.item, pStr.qty, pStr.total),
            rows = purchasedItems.map { listOf(it.itemName, if (it.quantity > 0) "${it.quantity.toInt()}x" else "—", MoneyFormat.formatMinor(it.totalAmount, currency)) },
            totalAmountStr = MoneyFormat.formatMinor(purchasedItems.sumOf { it.totalAmount }, currency)
        )

        // ==========================================
        // SIGNATURES & AUTHORIZATION
        // ==========================================
        ensureSpace(65f)
        val signY = currentY + 30f
        canvas.drawLine(margin + 20f, signY, margin + 200f, signY, linePaint)
        canvas.drawText("${pStr.cashierSign}: ${report.cashierName}", margin + 20f, signY + 14f, textBoldPaint)

        canvas.drawLine(PAGE_WIDTH - margin - 200f, signY, PAGE_WIDTH - margin - 20f, signY, linePaint)
        canvas.drawText(pStr.supervisorSign, PAGE_WIDTH - margin - 200f, signY + 14f, textBoldPaint)

        drawFooter()
        pdfDocument.finishPage(page)

        val fileName = "Shift_Report_${report.cashierName.replace(" ", "_")}_${report.shift}_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.pdf"
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
