package com.lojia.shiftreport.printer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import com.lojia.shiftreport.util.AppLogger
import com.lojia.shiftreport.R
import com.lojia.shiftreport.data.PreferencesRepository
import com.lojia.shiftreport.util.DateTimeFormatUtils
import com.lojia.shiftreport.util.MoneyFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.util.*

/**
 * Data model for Bluetooth Printer device representation in UI.
 */
data class BluetoothPrinterDevice(
    val name: String,
    val address: String,
    val isSelected: Boolean = false
)

/**
 * Paper width options for ESC/POS Thermal Printers.
 */
enum class PrinterPaperWidth(val widthMm: Int, val dpi: Int, val charsPerLine: Int) {
    MM_58(58, 203, 32),
    MM_80(80, 203, 48);

    companion object {
        fun fromWidthMm(mm: Int): PrinterPaperWidth = when (mm) {
            80 -> MM_80
            else -> MM_58
        }
    }
}

/**
 * 100% Native Google / Android ESC/POS Thermal Printer Protocol Encoder.
 * Replaces third-party printer libraries with standard Android Bluetooth RFCOMM sockets
 * and standard ESC/POS binary command streams.
 */
object EscPosNativeProtocol {
    val INIT = byteArrayOf(0x1B, 0x40)
    val ALIGN_LEFT = byteArrayOf(0x1B, 0x61, 0x00)
    val ALIGN_CENTER = byteArrayOf(0x1B, 0x61, 0x01)
    val ALIGN_RIGHT = byteArrayOf(0x1B, 0x61, 0x02)
    val BOLD_ON = byteArrayOf(0x1B, 0x45, 0x01)
    val BOLD_OFF = byteArrayOf(0x1B, 0x45, 0x00)
    val DOUBLE_SIZE_ON = byteArrayOf(0x1D, 0x21, 0x11)
    val DOUBLE_SIZE_OFF = byteArrayOf(0x1D, 0x21, 0x00)
    val LINE_FEED = byteArrayOf(0x0A)
    val PAPER_CUT = byteArrayOf(0x1D, 0x56, 0x42, 0x00)
    val OPEN_DRAWER = byteArrayOf(0x1B, 0x70, 0x00, 0x19, 0xFA.toByte())

    // Standard Bluetooth Serial Port Profile (SPP) UUID
    val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    /**
     * Converts a monochrome [Bitmap] into an ESC/POS raster bit-image command (GS v 0).
     */
    fun bitmapToRasterEscPos(bitmap: Bitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height
        val widthBytes = (width + 7) / 8
        val out = ByteArrayOutputStream()

        out.write(ALIGN_CENTER)
        // GS v 0 m xL xH yL yH
        out.write(
            byteArrayOf(
                0x1D, 0x76, 0x30, 0x00,
                (widthBytes and 0xFF).toByte(),
                ((widthBytes shr 8) and 0xFF).toByte(),
                (height and 0xFF).toByte(),
                ((height shr 8) and 0xFF).toByte()
            )
        )

        for (y in 0 until height) {
            for (x in 0 until widthBytes) {
                var b = 0
                for (bit in 0 until 8) {
                    val px = x * 8 + bit
                    if (px < width) {
                        val pixel = bitmap.getPixel(px, y)
                        val r = (pixel shr 16) and 0xFF
                        val g = (pixel shr 8) and 0xFF
                        val bl = pixel and 0xFF
                        val lum = (299 * r + 587 * g + 114 * bl) / 1000
                        if (lum < 160) {
                            b = b or (1 shl (7 - bit))
                        }
                    }
                }
                out.write(b)
            }
        }
        out.write(LINE_FEED)
        return out.toByteArray()
    }

    /**
     * Encodes a receipt text formatted with [L], [C], [R], <b>, <font> tags into native ESC/POS bytes.
     */
    fun encodeFormattedText(text: String, charsPerLine: Int, widthPx: Int): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(INIT)

        val lines = text.split("\n")
        for (rawLine in lines) {
            val line = rawLine.trimEnd('\r')
            if (line.isBlank()) {
                out.write(LINE_FEED)
                continue
            }

            // Check if line contains non-Latin text requiring bitmap rendering
            if (ThermalBitmapRenderer.containsNonLatin(line)) {
                try {
                    val bmp = ThermalBitmapRenderer.renderLineToBitmap(line, widthPx = widthPx)
                    out.write(bitmapToRasterEscPos(bmp))
                } catch (_: Exception) {
                    writeFormattedTextLine(out, line, charsPerLine)
                }
            } else {
                writeFormattedTextLine(out, line, charsPerLine)
            }
        }

        // Final line feeds and paper cut
        out.write(LINE_FEED)
        out.write(LINE_FEED)
        out.write(LINE_FEED)
        out.write(PAPER_CUT)
        return out.toByteArray()
    }

    private fun writeFormattedTextLine(out: ByteArrayOutputStream, line: String, charsPerLine: Int) {
        var cleanLine = line
        var isBold = false
        var isBig = false

        if (cleanLine.contains("<b>")) {
            isBold = true
            cleanLine = cleanLine.replace("<b>", "").replace("</b>", "")
        }
        if (cleanLine.contains("<font size='big'>") || cleanLine.contains("<font size=\"big\">")) {
            isBig = true
            cleanLine = cleanLine.replace(Regex("<font size=['\"]big['\"]>"), "").replace("</font>", "")
        }

        if (isBold) out.write(BOLD_ON)
        if (isBig) out.write(DOUBLE_SIZE_ON)

        when {
            cleanLine.startsWith("[C]") -> {
                out.write(ALIGN_CENTER)
                val text = cleanLine.removePrefix("[C]")
                out.write(text.toByteArray(Charsets.UTF_8))
                out.write(LINE_FEED)
            }
            cleanLine.contains("[L]") && cleanLine.contains("[R]") && cleanLine.contains("[C]") -> {
                out.write(ALIGN_LEFT)
                val parts = cleanLine.split("[C]", "[R]")
                val left = parts.getOrNull(0)?.removePrefix("[L]")?.trim().orEmpty()
                val center = parts.getOrNull(1)?.trim().orEmpty()
                val right = parts.getOrNull(2)?.trim().orEmpty()

                val effectiveChars = if (isBig) charsPerLine / 2 else charsPerLine
                val totalLen = left.length + center.length + right.length
                val spaces = (effectiveChars - totalLen).coerceAtLeast(1)
                val leftSpace = spaces / 2
                val rightSpace = spaces - leftSpace
                val combined = left + " ".repeat(leftSpace) + center + " ".repeat(rightSpace) + right
                out.write(combined.toByteArray(Charsets.UTF_8))
                out.write(LINE_FEED)
            }
            cleanLine.contains("[L]") && cleanLine.contains("[R]") -> {
                out.write(ALIGN_LEFT)
                val parts = cleanLine.split("[R]")
                val left = parts.getOrNull(0)?.removePrefix("[L]")?.trim().orEmpty()
                val right = parts.getOrNull(1)?.trim().orEmpty()

                val effectiveChars = if (isBig) charsPerLine / 2 else charsPerLine
                val spaces = (effectiveChars - (left.length + right.length)).coerceAtLeast(1)
                val combined = left + " ".repeat(spaces) + right
                out.write(combined.toByteArray(Charsets.UTF_8))
                out.write(LINE_FEED)
            }
            cleanLine.startsWith("[R]") -> {
                out.write(ALIGN_RIGHT)
                val text = cleanLine.removePrefix("[R]")
                out.write(text.toByteArray(Charsets.UTF_8))
                out.write(LINE_FEED)
            }
            else -> {
                out.write(ALIGN_LEFT)
                val text = cleanLine.removePrefix("[L]")
                out.write(text.toByteArray(Charsets.UTF_8))
                out.write(LINE_FEED)
            }
        }

        if (isBig) out.write(DOUBLE_SIZE_OFF)
        if (isBold) out.write(BOLD_OFF)
    }
}

/**
 * Native Bluetooth & Network ESC/POS Thermal Printer Manager.
 * 100% Native Google / Android implementation replacing third-party DantSu ESCPOS library.
 */
class BluetoothPrinterManager(private val context: Context) {

    private val prefsRepository = PreferencesRepository.getInstance(context)

    companion object {
        private const val TAG = "BluetoothPrinterManager"
    }

    /**
     * Checks whether runtime permissions required for Bluetooth thermal printing are granted.
     */
    fun hasBluetoothPermissions(): Boolean {
        return com.lojia.shiftreport.permission.PermissionUtils.hasAllPermissions(
            context,
            com.lojia.shiftreport.permission.AppFeaturePermission.BLUETOOTH_PRINTER.permissions
        )
    }

    /**
     * Retrieves list of paired Bluetooth devices that can act as thermal printers.
     */
    @SuppressLint("MissingPermission")
    fun getPairedPrinters(): List<BluetoothPrinterDevice> {
        if (!hasBluetoothPermissions()) {
            AppLogger.w(TAG, "Bluetooth permissions not granted when querying paired devices.")
            return emptyList()
        }
        return try {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
            if (!bluetoothAdapter.isEnabled) return emptyList()

            val pairedDevices: Set<BluetoothDevice> = bluetoothAdapter.bondedDevices ?: emptySet()
            val savedAddress = prefsRepository.getSelectedPrinterAddress()

            pairedDevices.map { device ->
                BluetoothPrinterDevice(
                    name = device.name ?: "Unknown Printer (${device.address})",
                    address = device.address,
                    isSelected = device.address == savedAddress
                )
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error fetching paired Bluetooth printers", e)
            emptyList()
        }
    }

    fun getPrinterConnectionType(): String = prefsRepository.getPrinterConnectionType()

    fun savePrinterConnectionType(type: String) {
        prefsRepository.savePrinterConnectionType(type)
    }

    fun savePrinterConfig(address: String, widthMm: Int) {
        prefsRepository.savePrinterAddress(address)
        prefsRepository.savePrinterPaperWidth(widthMm)
        prefsRepository.savePrinterConnectionType("bluetooth")
    }

    fun saveNetworkPrinterConfig(ip: String, port: Int = 9100, widthMm: Int = 80) {
        prefsRepository.savePrinterNetworkIp(ip)
        prefsRepository.savePrinterNetworkPort(port)
        prefsRepository.savePrinterPaperWidth(widthMm)
        prefsRepository.savePrinterConnectionType("network")
    }

    fun getSavedPrinterAddress(): String = prefsRepository.getSelectedPrinterAddress()

    fun getSavedNetworkIp(): String = prefsRepository.getPrinterNetworkIp()

    fun getSavedNetworkPort(): Int = prefsRepository.getPrinterNetworkPort()

    fun getSavedPaperWidthMm(): Int = prefsRepository.getPrinterPaperWidth()

    fun isPrinterConfigured(): Boolean {
        return if (getPrinterConnectionType() == "network") {
            getSavedNetworkIp().isNotBlank()
        } else {
            getSavedPrinterAddress().isNotBlank()
        }
    }

    /**
     * Tests Network connection to specified printer IP and Port over TCP socket.
     */
    suspend fun testNetworkConnection(ip: String = getSavedNetworkIp(), port: Int = getSavedNetworkPort()): Result<Boolean> = withContext(Dispatchers.IO) {
        if (ip.isBlank()) {
            return@withContext Result.failure(IllegalStateException("No network printer IP address specified."))
        }
        try {
            val socket = Socket()
            socket.connect(InetSocketAddress(ip, port), 3000)
            val isConnected = socket.isConnected
            socket.close()

            if (isConnected) {
                Result.success(true)
            } else {
                Result.failure(IllegalStateException("Could not connect to printer at $ip:$port"))
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Network printer test connection failed ($ip:$port): ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Tests socket connection to specified printer (Bluetooth or Network).
     */
    @SuppressLint("MissingPermission")
    suspend fun testConnection(address: String = getSavedPrinterAddress()): Result<Boolean> = withContext(Dispatchers.IO) {
        if (getPrinterConnectionType() == "network") {
            return@withContext testNetworkConnection(getSavedNetworkIp(), getSavedNetworkPort())
        }

        if (address.isBlank()) {
            return@withContext Result.failure(IllegalStateException("No printer address specified."))
        }
        try {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                ?: return@withContext Result.failure(IllegalStateException("Bluetooth adapter unavailable."))
            if (!bluetoothAdapter.isEnabled) {
                return@withContext Result.failure(IllegalStateException("Bluetooth is disabled."))
            }

            val device = bluetoothAdapter.getRemoteDevice(address)
                ?: return@withContext Result.failure(IllegalStateException("Device not found."))

            val socket = device.createRfcommSocketToServiceRecord(EscPosNativeProtocol.SPP_UUID)
            bluetoothAdapter.cancelDiscovery()
            socket.connect()
            val connected = socket.isConnected
            socket.close()

            if (connected) {
                Result.success(true)
            } else {
                Result.failure(IllegalStateException("Could not establish Bluetooth socket connection."))
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Test connection failed to $address: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Prints formatted ESC/POS text asynchronously over native Android Bluetooth socket or Network TCP.
     */
    suspend fun printFormattedText(formattedText: String): Result<Unit> = withContext(Dispatchers.IO) {
        val connectionType = getPrinterConnectionType()
        val paperWidth = PrinterPaperWidth.fromWidthMm(getSavedPaperWidthMm())
        val widthPx = if (paperWidth == PrinterPaperWidth.MM_80) 576 else 384
        val bytesToSend = EscPosNativeProtocol.encodeFormattedText(formattedText, paperWidth.charsPerLine, widthPx)

        if (connectionType == "network") {
            val ip = getSavedNetworkIp()
            val port = getSavedNetworkPort()
            if (ip.isBlank()) {
                return@withContext Result.failure(IllegalStateException("No Network printer IP configured in settings."))
            }

            return@withContext try {
                val socket = Socket()
                socket.connect(InetSocketAddress(ip, port), 4000)
                socket.outputStream.use { out ->
                    out.write(bytesToSend)
                    out.flush()
                }
                socket.close()
                AppLogger.d(TAG, "Receipt successfully printed over native TCP to $ip:$port")
                Result.success(Unit)
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to print to Network ESC/POS printer ($ip:$port): ${e.message}", e)
                Result.failure(e)
            }
        }

        val printerAddress = getSavedPrinterAddress()
        if (printerAddress.isBlank()) {
            return@withContext Result.failure(IllegalStateException("No Bluetooth printer selected in settings."))
        }

        if (!hasBluetoothPermissions()) {
            return@withContext Result.failure(
                SecurityException("Bluetooth and Nearby Devices permission is required to print to your thermal printer.")
            )
        }

        try {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                ?: return@withContext Result.failure(IllegalStateException("Bluetooth adapter unavailable."))
            if (!bluetoothAdapter.isEnabled) {
                return@withContext Result.failure(IllegalStateException("Bluetooth is disabled."))
            }

            @SuppressLint("MissingPermission")
            val device = bluetoothAdapter.getRemoteDevice(printerAddress)
                ?: return@withContext Result.failure(IllegalStateException("Device not found at $printerAddress"))

            bluetoothAdapter.cancelDiscovery()
            val socket = device.createRfcommSocketToServiceRecord(EscPosNativeProtocol.SPP_UUID)
            socket.connect()

            socket.outputStream.use { out ->
                out.write(bytesToSend)
                out.flush()
            }
            socket.close()

            AppLogger.d(TAG, "Receipt successfully printed over native Bluetooth to $printerAddress")
            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to print to Bluetooth ESC/POS printer: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Builds standard ESC/POS formatted string for a sale receipt with localized strings.
     */
    fun buildReceiptText(
        businessName: String,
        businessAddress: String = "",
        businessPhone: String = "",
        vatNumber: String = "",
        customHeader: String = "",
        customFooterText: String = "",
        showTaxNumber: Boolean = true,
        showCashierName: Boolean = true,
        receiptId: String,
        dateTimeStr: String,
        cashierName: String,
        customerName: String = "",
        items: List<Pair<String, Pair<Double, Double>>>,
        subtotal: Double,
        discount: Double = 0.0,
        tax: Double = 0.0,
        grandTotal: Double,
        paymentMethod: String,
        currencySymbol: String = "$"
    ): String {
        val paperWidth = PrinterPaperWidth.fromWidthMm(getSavedPaperWidthMm())
        val is58 = paperWidth == PrinterPaperWidth.MM_58
        val lineSeparator = "-".repeat(paperWidth.charsPerLine)

        val labelReceiptNo = context.getString(R.string.receipt_label_no)
        val labelDate = context.getString(R.string.receipt_label_date)
        val labelCashier = context.getString(R.string.receipt_label_cashier)
        val labelCustomer = context.getString(R.string.receipt_label_customer)
        val labelSubtotal = context.getString(R.string.receipt_label_subtotal)
        val labelDiscount = context.getString(R.string.receipt_label_discount)
        val labelTaxVat = context.getString(R.string.receipt_label_tax_vat)
        val labelTotal = context.getString(R.string.receipt_label_total)
        val labelPaymentMethod = context.getString(R.string.receipt_label_payment_method)
        val defaultFooter = context.getString(R.string.receipt_footer_default)

        val sb = StringBuilder()

        // Header
        if (customHeader.isNotBlank()) {
            sb.append("[C]<b>$customHeader</b>\n")
        }
        sb.append("[C]<b><font size='big'>$businessName</font></b>\n")
        if (businessAddress.isNotBlank()) {
            sb.append("[C]$businessAddress\n")
        }
        if (businessPhone.isNotBlank()) {
            sb.append("[C]Tel: $businessPhone\n")
        }
        if (showTaxNumber && vatNumber.isNotBlank()) {
            sb.append("[C]VAT/TAX ID: $vatNumber\n")
        }
        sb.append("[C]$lineSeparator\n")

        // Receipt Meta
        sb.append("[L]$labelReceiptNo [R]$receiptId\n")
        sb.append("[L]$labelDate [R]$dateTimeStr\n")
        if (showCashierName && cashierName.isNotBlank()) {
            sb.append("[L]$labelCashier [R]$cashierName\n")
        }
        if (customerName.isNotBlank() && customerName != "Walk-in Customer") {
            sb.append("[L]$labelCustomer [R]$customerName\n")
        }
        sb.append("[C]$lineSeparator\n")

        // Column Headers
        if (is58) {
            val itemHead = context.getString(R.string.receipt_label_item)
            val qtyPriceHead = context.getString(R.string.receipt_label_qty_x_price)
            sb.append("[L]<b>$itemHead</b>[R]<b>$qtyPriceHead</b>\n")
        } else {
            val itemDescHead = context.getString(R.string.receipt_label_item_desc)
            val qtyHead = context.getString(R.string.receipt_label_qty)
            val totalHead = context.getString(R.string.receipt_label_total).replace(":", "")
            sb.append("[L]<b>$itemDescHead</b>[C]<b>$qtyHead</b>[R]<b>$totalHead</b>\n")
        }
        sb.append("[C]$lineSeparator\n")

        // Items
        items.forEach { (name, qtyAndTotal) ->
            val qtyDouble = qtyAndTotal.first
            val qtyStr = if (qtyDouble % 1.0 == 0.0) qtyDouble.toInt().toString() else String.format(Locale.US, "%.1f", qtyDouble)
            val lineTotal = qtyAndTotal.second
            val formattedTotal = String.format(Locale.US, "%.2f", lineTotal)

            if (is58) {
                sb.append("[L]$name\n")
                sb.append("[L]  x$qtyStr[R]$currencySymbol$formattedTotal\n")
            } else {
                sb.append("[L]$name[C]$qtyStr[R]$currencySymbol$formattedTotal\n")
            }
        }
        sb.append("[C]$lineSeparator\n")

        // Totals
        sb.append("[L]$labelSubtotal[R]$currencySymbol${String.format(Locale.US, "%.2f", subtotal)}\n")
        if (discount > 0.0) {
            sb.append("[L]$labelDiscount[R]-$currencySymbol${String.format(Locale.US, "%.2f", discount)}\n")
        }
        if (tax > 0.0) {
            sb.append("[L]$labelTaxVat[R]$currencySymbol${String.format(Locale.US, "%.2f", tax)}\n")
        }
        sb.append("[C]$lineSeparator\n")
        sb.append("[L]<b>$labelTotal</b>[R]<b>$currencySymbol${String.format(Locale.US, "%.2f", grandTotal)}</b>\n")
        sb.append("[L]$labelPaymentMethod[R]$paymentMethod\n")
        sb.append("[C]$lineSeparator\n")

        // Footer
        val footer = if (customFooterText.isNotBlank()) customFooterText else defaultFooter
        sb.append("[C]<b>$footer</b>\n")
        sb.append("\n\n\n")

        return sb.toString()
    }

    /**
     * Builds standard ESC/POS formatted string for an End-of-Shift Z-Report.
     */
    fun buildZReportText(
        businessName: String,
        businessAddress: String = "",
        businessPhone: String = "",
        vatNumber: String = "",
        report: com.lojia.shiftreport.data.ShiftReport,
        currencySymbol: String = "$",
        vatRate: Double = 15.0,
        isTaxEnabled: Boolean = true,
        isTaxIncluded: Boolean = true
    ): String = buildShiftReportText(
        businessName, businessAddress, businessPhone, vatNumber, report, currencySymbol, vatRate, isTaxEnabled, isTaxIncluded
    )

    fun buildShiftReportText(
        businessName: String,
        businessAddress: String = "",
        businessPhone: String = "",
        vatNumber: String = "",
        report: com.lojia.shiftreport.data.ShiftReport,
        currencySymbol: String = "$",
        vatRate: Double = 15.0,
        isTaxEnabled: Boolean = true,
        isTaxIncluded: Boolean = true
    ): String {
        val paperWidth = PrinterPaperWidth.fromWidthMm(getSavedPaperWidthMm())
        val lineSeparator = "-".repeat(paperWidth.charsPerLine)
        val formattedDate = DateTimeFormatUtils.formatDateTime(report.dateInMillis)

        val startingCash = com.lojia.shiftreport.util.PdfReportGenerator.extractStartingCashFromNotes(report)
        val actualCashCount = com.lojia.shiftreport.util.PdfReportGenerator.extractActualCashFromNotes(report)
        val cashIn = MoneyFormat.toMajorUnits(report.totalDueCollectedCash)
        val cashOut = MoneyFormat.toMajorUnits(report.totalCashOut)
        val grossCash = MoneyFormat.toMajorUnits(report.grossCash)
        val expectedCash = startingCash + grossCash + cashIn - cashOut
        val variance = actualCashCount?.let { it - expectedCash }

        val sb = StringBuilder()

        // Header
        sb.append("[C]<b><font size='big'>$businessName</font></b>\n")
        if (businessAddress.isNotBlank()) sb.append("[C]$businessAddress\n")
        if (businessPhone.isNotBlank()) sb.append("[C]Tel: $businessPhone\n")
        if (vatNumber.isNotBlank()) sb.append("[C]VAT ID: $vatNumber\n")
        sb.append("[C]$lineSeparator\n")
        sb.append("[C]<b><font size='big'>${context.getString(R.string.zreport_title)}</font></b>\n")
        sb.append("[C]$lineSeparator\n")

        // Shift Metadata
        sb.append("[L]${context.getString(R.string.zreport_shift_date)}[R]$formattedDate\n")
        sb.append("[L]${context.getString(R.string.zreport_shift)}[R]${report.shift}\n")
        sb.append("[L]${context.getString(R.string.zreport_cashier)}[R]${report.cashierName}\n")
        sb.append("[C]$lineSeparator\n")

        val madaPayments = MoneyFormat.toMajorUnits(report.madaPayments)
        val digitalWallet = MoneyFormat.toMajorUnits(report.digitalWallet)
        val totalSales = MoneyFormat.toMajorUnits(report.totalSales)

        // 1. SALES BY PAYMENT METHOD
        sb.append("[L]<b>${context.getString(R.string.zreport_sec_sales)}</b>\n")
        sb.append("[L]${context.getString(R.string.zreport_cash_sales)}[R]$currencySymbol${String.format(Locale.US, "%.2f", grossCash)}\n")
        sb.append("[L]${context.getString(R.string.zreport_card_mada)}[R]$currencySymbol${String.format(Locale.US, "%.2f", madaPayments)}\n")
        if (digitalWallet > 0.0) {
            sb.append("[L]${context.getString(R.string.zreport_digital_wallet)}[R]$currencySymbol${String.format(Locale.US, "%.2f", digitalWallet)}\n")
        }
        sb.append("[L]<b>${context.getString(R.string.zreport_total_revenue)}</b>[R]<b>$currencySymbol${String.format(Locale.US, "%.2f", totalSales)}</b>\n")
        sb.append("[C]$lineSeparator\n")

        // 2. CASH DRAWER RECONCILIATION
        sb.append("[L]<b>${context.getString(R.string.zreport_sec_cash_recon)}</b>\n")
        if (startingCash > 0.0) {
            sb.append("[L]${context.getString(R.string.zreport_starting_float)}[R]$currencySymbol${String.format(Locale.US, "%.2f", startingCash)}\n")
        }
        sb.append("[L](+) ${context.getString(R.string.zreport_cash_sales)}[R]$currencySymbol${String.format(Locale.US, "%.2f", grossCash)}\n")
        if (cashIn > 0.0) {
            sb.append("[L]${context.getString(R.string.zreport_cash_in_dues)}[R]$currencySymbol${String.format(Locale.US, "%.2f", cashIn)}\n")
        }
        if (cashOut > 0.0) {
            sb.append("[L]${context.getString(R.string.zreport_cash_out)}[R]$currencySymbol${String.format(Locale.US, "%.2f", cashOut)}\n")
        }
        sb.append("[L]<b>${context.getString(R.string.zreport_expected_cash)}</b>[R]<b>$currencySymbol${String.format(Locale.US, "%.2f", expectedCash)}</b>\n")

        if (actualCashCount != null) {
            sb.append("[L]<b>${context.getString(R.string.zreport_actual_cash)}</b>[R]<b>$currencySymbol${String.format(Locale.US, "%.2f", actualCashCount)}</b>\n")
            val varFormatted = when {
                Math.abs(variance ?: 0.0) < 0.01 -> "${currencySymbol}0.00 (${context.getString(R.string.zreport_balanced)})"
                (variance ?: 0.0) > 0 -> "+$currencySymbol${String.format(Locale.US, "%.2f", variance)} (${context.getString(R.string.zreport_over)})"
                else -> "-$currencySymbol${String.format(Locale.US, "%.2f", Math.abs(variance ?: 0.0))} (${context.getString(R.string.zreport_short)})"
            }
            sb.append("[L]<b>${context.getString(R.string.zreport_variance)}</b>[R]<b>$varFormatted</b>\n")
        }
        sb.append("[C]$lineSeparator\n")

        // 3. TAX & VAT SUMMARY
        val (netTaxable, vatAmount, grossTotal) = if (isTaxEnabled && vatRate > 0.0) {
            if (isTaxIncluded) {
                val divisor = 1.0 + (vatRate / 100.0)
                val net = totalSales / divisor
                val vat = totalSales - net
                Triple(net, vat, totalSales)
            } else {
                val net = totalSales
                val vat = net * (vatRate / 100.0)
                Triple(net, vat, net + vat)
            }
        } else {
            Triple(totalSales, 0.0, totalSales)
        }
        sb.append("[L]<b>${context.getString(R.string.zreport_sec_tax_summary)}</b>\n")
        if (isTaxEnabled && vatRate > 0.0) {
            val vatRateStr = String.format(Locale.US, "%.1f", vatRate)
            val vatLabel = context.getString(R.string.zreport_vat_label, vatRateStr)
            val incExcStr = if (isTaxIncluded) context.getString(R.string.zreport_inc_vat) else context.getString(R.string.zreport_exc_vat)
            val grossLabel = context.getString(R.string.zreport_gross_total, incExcStr)

            sb.append("[L]${context.getString(R.string.zreport_net_taxable)}[R]$currencySymbol${String.format(Locale.US, "%.2f", netTaxable)}\n")
            sb.append("[L]$vatLabel[R]$currencySymbol${String.format(Locale.US, "%.2f", vatAmount)}\n")
            sb.append("[L]$grossLabel[R]$currencySymbol${String.format(Locale.US, "%.2f", grossTotal)}\n")
        } else {
            sb.append("[L]${context.getString(R.string.zreport_tax_status)}[R]${context.getString(R.string.zreport_non_taxable)}\n")
            sb.append("[L]${context.getString(R.string.zreport_net_total)}[R]$currencySymbol${String.format(Locale.US, "%.2f", totalSales)}\n")
        }
        sb.append("[C]$lineSeparator\n")

        // 4. OTHER TRACKING
        val totalDueCredit = MoneyFormat.toMajorUnits(report.totalDueCredit)
        if (totalDueCredit > 0.0 || report.staffMealsCount > 0) {
            sb.append("[L]<b>${context.getString(R.string.zreport_sec_other)}</b>\n")
            if (totalDueCredit > 0.0) {
                sb.append("[L]${context.getString(R.string.zreport_credit_sales)}[R]$currencySymbol${String.format(Locale.US, "%.2f", totalDueCredit)}\n")
            }
            if (report.staffMealsCount > 0) {
                sb.append("[L]${context.getString(R.string.zreport_staff_meals)}[R]${report.staffMealsCount}\n")
            }
            sb.append("[C]$lineSeparator\n")
        }

        // Clean Notes
        val cleanNotes = com.lojia.shiftreport.util.PdfReportGenerator.cleanDisplayNotes(report.notes)
        if (cleanNotes.isNotBlank()) {
            sb.append("[L]${context.getString(R.string.zreport_notes)}\n")
            cleanNotes.split("\n").forEach { line ->
                if (line.isNotBlank()) {
                    sb.append("[L]  $line\n")
                }
            }
            sb.append("[C]$lineSeparator\n")
        }

        // Footer
        sb.append("[C]${context.getString(R.string.zreport_footer_generated)}\n")
        sb.append("[C]${context.getString(R.string.zreport_footer_brand)}\n")
        sb.append("\n\n\n")

        return sb.toString()
    }

    /**
     * Builds a quick test receipt to verify thermal printer connectivity and formatting.
     */
    fun buildTestReceiptText(): String {
        val paperWidth = PrinterPaperWidth.fromWidthMm(getSavedPaperWidthMm())
        val lineSeparator = "-".repeat(paperWidth.charsPerLine)
        val nowFormatted = DateTimeFormatUtils.formatDateTime(System.currentTimeMillis())

        return buildString {
            append("[C]<b><font size='big'>${context.getString(R.string.printer_test_receipt_title)}</font></b>\n")
            append("[C]${context.getString(R.string.printer_test_receipt_subtitle)}\n")
            append("[C]$lineSeparator\n")
            append("[L]Status:[R]CONNECTED\n")
            append("[L]Time:[R]$nowFormatted\n")
            append("[L]Paper:[R]${paperWidth.widthMm}mm (${paperWidth.charsPerLine} CPL)\n")
            append("[C]$lineSeparator\n")
            append("[C]<b>${context.getString(R.string.printer_test_receipt_operational)}</b>\n")
            append("[C]English / বাংলা / العربية\n")
            append("\n\n\n")
        }
    }

    /**
     * Sends ESC/POS pulse signal (ESC p 0 25 250) over Bluetooth or TCP Network connection to kick open cash drawer.
     */
    suspend fun openCashDrawer(): Result<Unit> = withContext(Dispatchers.IO) {
        if (getPrinterConnectionType() == "network") {
            val ip = getSavedNetworkIp()
            val port = getSavedNetworkPort()
            if (ip.isBlank()) {
                return@withContext Result.failure(IllegalStateException("No Network printer IP configured."))
            }
            return@withContext try {
                val socket = Socket()
                socket.connect(InetSocketAddress(ip, port), 3000)
                socket.outputStream.use { out ->
                    out.write(EscPosNativeProtocol.OPEN_DRAWER)
                    out.flush()
                }
                socket.close()
                AppLogger.d(TAG, "Sent open cash drawer command over native TCP to $ip:$port")
                Result.success(Unit)
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to open cash drawer over network: ${e.message}", e)
                Result.failure(e)
            }
        }

        val printerAddress = getSavedPrinterAddress()
        if (printerAddress.isBlank()) {
            return@withContext Result.failure(IllegalStateException("No Bluetooth printer selected."))
        }

        try {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                ?: return@withContext Result.failure(IllegalStateException("Bluetooth adapter unavailable."))
            if (!bluetoothAdapter.isEnabled) {
                return@withContext Result.failure(IllegalStateException("Bluetooth is disabled."))
            }

            @SuppressLint("MissingPermission")
            val device = bluetoothAdapter.getRemoteDevice(printerAddress)
                ?: return@withContext Result.failure(IllegalStateException("Device not found."))

            bluetoothAdapter.cancelDiscovery()
            val socket = device.createRfcommSocketToServiceRecord(EscPosNativeProtocol.SPP_UUID)
            socket.connect()
            socket.outputStream.use { out ->
                out.write(EscPosNativeProtocol.OPEN_DRAWER)
                out.flush()
            }
            socket.close()
            AppLogger.d(TAG, "Sent open cash drawer command to $printerAddress via native Bluetooth")
            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to open cash drawer: ${e.message}", e)
            Result.failure(e)
        }
    }
}
