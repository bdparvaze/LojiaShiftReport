package com.lojia.shiftreport.printer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.util.Log
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import com.dantsu.escposprinter.connection.tcp.TcpConnection
import com.dantsu.escposprinter.textparser.PrinterTextParserImg
import com.lojia.shiftreport.R
import com.lojia.shiftreport.data.PreferencesRepository
import com.lojia.shiftreport.util.DateTimeFormatUtils
import com.lojia.shiftreport.util.MoneyFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
 * BluetoothPrinterManager handles:
 * 1. Discovering paired Bluetooth devices
 * 2. Connecting to selected ESC/POS Bluetooth Thermal Printer
 * 3. Generating formatted receipt text with DantSu ESCPOS-ThermalPrinter-Android syntax
 * 4. Exception handling ensuring POS operations are NEVER blocked if printer is offline or fails
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
            Log.w(TAG, "Bluetooth permissions not granted when querying paired devices.")
            return emptyList()
        }
        return try {
            val connections = BluetoothPrintersConnections.selectFirstPaired()
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
            Log.e(TAG, "Error fetching paired Bluetooth printers", e)
            emptyList()
        }
    }

    /**
     * Connection type management ("bluetooth" or "network")
     */
    fun getPrinterConnectionType(): String {
        return prefsRepository.getPrinterConnectionType()
    }

    fun savePrinterConnectionType(type: String) {
        prefsRepository.savePrinterConnectionType(type)
    }

    /**
     * Saves selected printer hardware address and paper width setting in preferences.
     */
    fun savePrinterConfig(address: String, widthMm: Int) {
        prefsRepository.savePrinterAddress(address)
        prefsRepository.savePrinterPaperWidth(widthMm)
        prefsRepository.savePrinterConnectionType("bluetooth")
    }

    /**
     * Saves network printer IP, port and paper width in preferences.
     */
    fun saveNetworkPrinterConfig(ip: String, port: Int = 9100, widthMm: Int = 80) {
        prefsRepository.savePrinterNetworkIp(ip)
        prefsRepository.savePrinterNetworkPort(port)
        prefsRepository.savePrinterPaperWidth(widthMm)
        prefsRepository.savePrinterConnectionType("network")
    }

    /**
     * Gets currently saved printer address or empty string if unconfigured.
     */
    fun getSavedPrinterAddress(): String {
        return prefsRepository.getSelectedPrinterAddress()
    }

    fun getSavedNetworkIp(): String {
        return prefsRepository.getPrinterNetworkIp()
    }

    fun getSavedNetworkPort(): Int {
        return prefsRepository.getPrinterNetworkPort()
    }

    /**
     * Gets currently saved printer paper width in mm (58 or 80).
     */
    fun getSavedPaperWidthMm(): Int {
        return prefsRepository.getPrinterPaperWidth()
    }

    /**
     * Checks if a printer address or IP has been configured in settings.
     */
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
            Log.e(TAG, "Network printer test connection failed ($ip:$port): ${e.message}", e)
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

            val connection = BluetoothConnection(device)
            connection.connect()
            val connected = connection.isConnected
            connection.disconnect()

            if (connected) {
                Result.success(true)
            } else {
                Result.failure(IllegalStateException("Could not establish Bluetooth socket connection."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Test connection failed to $address: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Prepares raw receipt text for the ESC/POS thermal printer.
     * If the text contains Bengali, Arabic, or other non-Latin characters, those specific lines
     * are rendered into high-contrast monochrome bitmaps and embedded via DantSu <img> tags,
     * ensuring 100% correct glyph and RTL rendering without '????' question mark artifacts.
     */
    fun prepareTextForPrinter(
        formattedText: String,
        printer: EscPosPrinter,
        paperWidth: PrinterPaperWidth
    ): String {
        if (!ThermalBitmapRenderer.containsNonLatin(formattedText)) {
            return formattedText
        }

        val widthPx = if (paperWidth == PrinterPaperWidth.MM_80) 576 else 384
        val lines = formattedText.split("\n")
        val resultSb = StringBuilder()

        for (line in lines) {
            if (line.isBlank()) {
                resultSb.append("\n")
                continue
            }

            // Keep separators and existing images as-is
            if (line.startsWith("[C]---") || line.startsWith("[C]===") || line.startsWith("---") || line.startsWith("===") || line.contains("<img>")) {
                resultSb.append(line).append("\n")
                continue
            }

            // If this line contains non-Latin text, render it cleanly to a bitmap
            if (ThermalBitmapRenderer.containsNonLatin(line)) {
                try {
                    val bitmap = ThermalBitmapRenderer.renderLineToBitmap(line, widthPx = widthPx)
                    val hexImg = PrinterTextParserImg.bitmapToHexadecimalString(printer, bitmap)
                    resultSb.append("[C]<img>$hexImg</img>\n")
                } catch (e: Exception) {
                    Log.w(TAG, "Bitmap render fallback for non-Latin line: $line", e)
                    resultSb.append(line).append("\n")
                }
            } else {
                resultSb.append(line).append("\n")
            }
        }

        return resultSb.toString()
    }

    /**
     * Prints formatted ESC/POS text asynchronously over Bluetooth or Network TCP.
     * Guaranteed NOT to block POS sales if printing fails or printer is disconnected.
     *
     * @param formattedText Raw ESC/POS formatted string using DantSu printer formatting tags.
     * @return Result.success(Unit) or Result.failure(Throwable)
     */
    suspend fun printFormattedText(formattedText: String): Result<Unit> = withContext(Dispatchers.IO) {
        val connectionType = getPrinterConnectionType()
        val paperWidth = PrinterPaperWidth.fromWidthMm(getSavedPaperWidthMm())

        if (connectionType == "network") {
            val ip = getSavedNetworkIp()
            val port = getSavedNetworkPort()
            if (ip.isBlank()) {
                return@withContext Result.failure(IllegalStateException("No Network printer IP configured in settings."))
            }

            return@withContext try {
                val tcpConnection = TcpConnection(ip, port, 4000)
                val printer = EscPosPrinter(
                    tcpConnection,
                    paperWidth.dpi,
                    paperWidth.widthMm.toFloat(),
                    paperWidth.charsPerLine
                )

                val processedText = prepareTextForPrinter(formattedText, printer, paperWidth)
                printer.printFormattedText(processedText)
                printer.disconnectPrinter()
                Log.d(TAG, "Receipt successfully printed over TCP to $ip:$port")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to print to Network ESC/POS thermal printer ($ip:$port): ${e.message}", e)
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
            val connection = BluetoothPrintersConnections.selectFirstPaired()
                ?: return@withContext Result.failure(IllegalStateException("No paired Bluetooth printer found."))

            // Search for connection matching target address
            var targetConnection: BluetoothConnection? = null
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter != null && bluetoothAdapter.isEnabled) {
                @SuppressLint("MissingPermission")
                val device = bluetoothAdapter.getRemoteDevice(printerAddress)
                if (device != null) {
                    targetConnection = BluetoothConnection(device)
                }
            }

            val activeConnection = targetConnection ?: connection

            // Initialize ESC/POS Printer with selected DPI, width, and characters per line
            val printer = EscPosPrinter(
                activeConnection,
                paperWidth.dpi,
                paperWidth.widthMm.toFloat(),
                paperWidth.charsPerLine
            )

            val processedText = prepareTextForPrinter(formattedText, printer, paperWidth)
            printer.printFormattedText(processedText)
            printer.disconnectPrinter()
            Log.d(TAG, "Receipt successfully printed to $printerAddress")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to print to ESC/POS thermal printer: ${e.message}", e)
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
        items: List<Pair<String, Pair<Double, Double>>>, // Name, Quantity, LineTotal
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
                sb.append("[L]$name[C]x$qtyStr[R]$currencySymbol$formattedTotal\n")
            }
        }
        sb.append("[C]$lineSeparator\n")

        // Totals (Formatted with Locale.US for column alignment)
        sb.append("[L]$labelSubtotal[R]$currencySymbol${String.format(Locale.US, "%.2f", subtotal)}\n")
        if (discount > 0) {
            sb.append("[L]$labelDiscount[R]-$currencySymbol${String.format(Locale.US, "%.2f", discount)}\n")
        }
        if (tax > 0) {
            sb.append("[L]$labelTaxVat[R]$currencySymbol${String.format(Locale.US, "%.2f", tax)}\n")
        }
        sb.append("[L]<b>$labelTotal</b>[R]<b>$currencySymbol${String.format(Locale.US, "%.2f", grandTotal)}</b>\n")
        sb.append("[L]$labelPaymentMethod[R]$paymentMethod\n")
        sb.append("[C]$lineSeparator\n")

        // Footer
        val footerText = customFooterText.ifBlank { defaultFooter }
        footerText.split("\n").forEach { line ->
            if (line.isNotBlank()) {
                sb.append("[C]$line\n")
            }
        }
        sb.append("\n\n\n")

        return sb.toString()
    }

    /**
     * Builds international Z-Report / Shift Close thermal receipt string with localized labels.
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
    ): String {
        val paperWidth = PrinterPaperWidth.fromWidthMm(getSavedPaperWidthMm())
        val is58 = paperWidth == PrinterPaperWidth.MM_58
        val lineSeparator = "-".repeat(paperWidth.charsPerLine)
        val formattedDate = DateTimeFormatUtils.formatDateTime(report.dateInMillis)

        val startingCash = com.lojia.shiftreport.util.PdfReportGenerator.extractStartingCashFromNotes(report)
        val actualCashCount = com.lojia.shiftreport.util.PdfReportGenerator.extractActualCashFromNotes(report)
        val cashIn = report.totalDueCollectedCash
        val cashOut = report.totalCashOut
        val expectedCash = startingCash + report.grossCash + cashIn - cashOut
        val variance = actualCashCount?.let { it - expectedCash }

        val sb = StringBuilder()

        // Header
        sb.append("[C]<b><font size='big'>$businessName</font></b>\n")
        if (businessAddress.isNotBlank()) {
            sb.append("[C]$businessAddress\n")
        }
        if (businessPhone.isNotBlank()) {
            sb.append("[C]Tel: $businessPhone\n")
        }
        if (vatNumber.isNotBlank()) {
            sb.append("[C]VAT ID: $vatNumber\n")
        }
        sb.append("[C]$lineSeparator\n")
        sb.append("[C]<b><font size='big'>${context.getString(R.string.zreport_title)}</font></b>\n")
        sb.append("[C]$lineSeparator\n")

        // Shift Metadata
        sb.append("[L]${context.getString(R.string.zreport_shift_date)}[R]$formattedDate\n")
        sb.append("[L]${context.getString(R.string.zreport_shift)}[R]${report.shift}\n")
        sb.append("[L]${context.getString(R.string.zreport_cashier)}[R]${report.cashierName}\n")
        sb.append("[C]$lineSeparator\n")

        // 1. SALES BY PAYMENT METHOD
        sb.append("[L]<b>${context.getString(R.string.zreport_sec_sales)}</b>\n")
        sb.append("[L]${context.getString(R.string.zreport_cash_sales)}[R]$currencySymbol${String.format(Locale.US, "%.2f", report.grossCash)}\n")
        sb.append("[L]${context.getString(R.string.zreport_card_mada)}[R]$currencySymbol${String.format(Locale.US, "%.2f", report.madaPayments)}\n")
        if (report.digitalWallet > 0) {
            sb.append("[L]${context.getString(R.string.zreport_digital_wallet)}[R]$currencySymbol${String.format(Locale.US, "%.2f", report.digitalWallet)}\n")
        }
        sb.append("[L]<b>${context.getString(R.string.zreport_total_revenue)}</b>[R]<b>$currencySymbol${String.format(Locale.US, "%.2f", report.totalSales)}</b>\n")
        sb.append("[C]$lineSeparator\n")

        // 2. CASH DRAWER RECONCILIATION
        sb.append("[L]<b>${context.getString(R.string.zreport_sec_cash_recon)}</b>\n")
        if (startingCash > 0) {
            sb.append("[L]${context.getString(R.string.zreport_starting_float)}[R]$currencySymbol${String.format(Locale.US, "%.2f", startingCash)}\n")
        }
        sb.append("[L](+) ${context.getString(R.string.zreport_cash_sales)}[R]$currencySymbol${String.format(Locale.US, "%.2f", report.grossCash)}\n")
        if (cashIn > 0) {
            sb.append("[L]${context.getString(R.string.zreport_cash_in_dues)}[R]$currencySymbol${String.format(Locale.US, "%.2f", cashIn)}\n")
        }
        if (cashOut > 0) {
            sb.append("[L]${context.getString(R.string.zreport_cash_out)}[R]$currencySymbol${String.format(Locale.US, "%.2f", cashOut)}\n")
        }
        sb.append("[L]<b>${context.getString(R.string.zreport_expected_cash)}</b>[R]<b>$currencySymbol${String.format(Locale.US, "%.2f", expectedCash)}</b>\n")

        if (actualCashCount != null) {
            sb.append("[L]<b>${context.getString(R.string.zreport_actual_cash)}</b>[R]<b>$currencySymbol${String.format(Locale.US, "%.2f", actualCashCount)}</b>\n")
            val varFormatted = when {
                variance == null -> "N/A"
                Math.abs(variance) < 0.01 -> "${currencySymbol}0.00 (${context.getString(R.string.zreport_balanced)})"
                variance > 0 -> "+$currencySymbol${String.format(Locale.US, "%.2f", variance)} (${context.getString(R.string.zreport_over)})"
                else -> "-$currencySymbol${String.format(Locale.US, "%.2f", Math.abs(variance))} (${context.getString(R.string.zreport_short)})"
            }
            sb.append("[L]<b>${context.getString(R.string.zreport_variance)}</b>[R]<b>$varFormatted</b>\n")
        }
        sb.append("[C]$lineSeparator\n")

        // 3. TAX & VAT SUMMARY
        val (netTaxable, vatAmount, grossTotal) = if (isTaxEnabled && vatRate > 0.0) {
            if (isTaxIncluded) {
                val divisor = 1.0 + (vatRate / 100.0)
                val net = report.totalSales / divisor
                val vat = report.totalSales - net
                Triple(net, vat, report.totalSales)
            } else {
                val net = report.totalSales
                val vat = net * (vatRate / 100.0)
                Triple(net, vat, net + vat)
            }
        } else {
            Triple(report.totalSales, 0.0, report.totalSales)
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
            sb.append("[L]${context.getString(R.string.zreport_net_total)}[R]$currencySymbol${String.format(Locale.US, "%.2f", report.totalSales)}\n")
        }
        sb.append("[C]$lineSeparator\n")

        // 4. OTHER TRACKING
        if (report.totalDueCredit > 0 || report.staffMealsCount > 0) {
            sb.append("[L]<b>${context.getString(R.string.zreport_sec_other)}</b>\n")
            if (report.totalDueCredit > 0) {
                sb.append("[L]${context.getString(R.string.zreport_credit_sales)}[R]$currencySymbol${String.format(Locale.US, "%.2f", report.totalDueCredit)}\n")
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
     * Sends ESC/POS pulse signal (ESC p 0 25 250) over Bluetooth or TCP Network connection to kick open cash drawer.
     */
    suspend fun openCashDrawer(): Result<Unit> = withContext(Dispatchers.IO) {
        val openDrawerBytes = byteArrayOf(0x1B.toByte(), 0x70.toByte(), 0x00.toByte(), 0x19.toByte(), 0xFA.toByte())

        if (getPrinterConnectionType() == "network") {
            val ip = getSavedNetworkIp()
            val port = getSavedNetworkPort()
            if (ip.isBlank()) {
                return@withContext Result.failure(IllegalStateException("No Network printer IP configured."))
            }
            return@withContext try {
                val tcpConnection = TcpConnection(ip, port, 3000)
                tcpConnection.connect()
                if (tcpConnection.isConnected) {
                    tcpConnection.write(openDrawerBytes)
                    tcpConnection.disconnect()
                    Log.d(TAG, "Sent open cash drawer command over TCP to $ip:$port")
                    Result.success(Unit)
                } else {
                    Result.failure(IllegalStateException("Could not connect to network printer at $ip:$port"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open cash drawer over network: ${e.message}", e)
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

            val connection = BluetoothConnection(device)
            connection.connect()
            if (connection.isConnected) {
                connection.write(openDrawerBytes)
                connection.disconnect()
                Log.d(TAG, "Sent open cash drawer command to $printerAddress")
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("Could not connect to printer for cash drawer kick."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open cash drawer: ${e.message}", e)
            Result.failure(e)
        }
    }
}
