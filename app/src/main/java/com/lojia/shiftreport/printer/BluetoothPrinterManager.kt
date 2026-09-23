package com.lojia.shiftreport.printer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.os.Build
import android.util.Log
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import com.dantsu.escposprinter.connection.tcp.TcpConnection
import com.lojia.shiftreport.data.PreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import java.text.SimpleDateFormat
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

                printer.printFormattedText(formattedText)
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

            printer.printFormattedText(formattedText)
            printer.disconnectPrinter()
            Log.d(TAG, "Receipt successfully printed to $printerAddress")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to print to ESC/POS thermal printer: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Builds standard ESC/POS formatted string for a sale receipt.
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
        val lineSeparator = if (is58) "--------------------------------" else "------------------------------------------------"

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
        sb.append("[L]Receipt #: [R]$receiptId\n")
        sb.append("[L]Date: [R]$dateTimeStr\n")
        if (showCashierName && cashierName.isNotBlank()) {
            sb.append("[L]Cashier: [R]$cashierName\n")
        }
        if (customerName.isNotBlank() && customerName != "Walk-in Customer") {
            sb.append("[L]Customer: [R]$customerName\n")
        }
        sb.append("[C]$lineSeparator\n")

        // Column Headers
        if (is58) {
            sb.append("[L]<b>Item</b>[R]<b>Qty x Price</b>\n")
        } else {
            sb.append("[L]<b>Item Description</b>[C]<b>Qty</b>[R]<b>Total</b>\n")
        }
        sb.append("[C]$lineSeparator\n")

        // Items
        items.forEach { (name, qtyAndTotal) ->
            val qtyDouble = qtyAndTotal.first
            val qtyStr = if (qtyDouble % 1.0 == 0.0) qtyDouble.toInt().toString() else qtyDouble.toString()
            val lineTotal = qtyAndTotal.second
            val formattedTotal = String.format("%.2f", lineTotal)

            if (is58) {
                sb.append("[L]$name\n")
                sb.append("[L]  x$qtyStr[R]$currencySymbol$formattedTotal\n")
            } else {
                sb.append("[L]$name[C]x$qtyStr[R]$currencySymbol$formattedTotal\n")
            }
        }
        sb.append("[C]$lineSeparator\n")

        // Totals
        sb.append("[L]Subtotal:[R]$currencySymbol${String.format("%.2f", subtotal)}\n")
        if (discount > 0) {
            sb.append("[L]Discount:[R]-$currencySymbol${String.format("%.2f", discount)}\n")
        }
        if (tax > 0) {
            sb.append("[L]Tax/VAT:[R]$currencySymbol${String.format("%.2f", tax)}\n")
        }
        sb.append("[L]<b>TOTAL:</b>[R]<b>$currencySymbol${String.format("%.2f", grandTotal)}</b>\n")
        sb.append("[L]Payment Method:[R]$paymentMethod\n")
        sb.append("[C]$lineSeparator\n")

        // Footer
        val footerText = customFooterText.ifBlank { "Thank you for your business!\nPlease come again" }
        footerText.split("\n").forEach { line ->
            if (line.isNotBlank()) {
                sb.append("[C]$line\n")
            }
        }
        sb.append("\n\n\n")

        return sb.toString()
    }

    /**
     * Builds international Z-Report / Shift Close thermal receipt string.
     */
    fun buildZReportText(
        businessName: String,
        businessAddress: String = "",
        businessPhone: String = "",
        vatNumber: String = "",
        report: com.lojia.shiftreport.data.ShiftReport,
        currencySymbol: String = "$"
    ): String {
        val paperWidth = PrinterPaperWidth.fromWidthMm(getSavedPaperWidthMm())
        val is58 = paperWidth == PrinterPaperWidth.MM_58
        val lineSeparator = if (is58) "--------------------------------" else "------------------------------------------------"
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        val startingCash = com.lojia.shiftreport.util.PdfReportGenerator.extractStartingCashFromNotes(report.notes) ?: 0.0
        val actualCashCount = com.lojia.shiftreport.util.PdfReportGenerator.extractActualCashFromNotes(report.notes)
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
        sb.append("[C]<b><font size='big'>Z-REPORT / SHIFT CLOSE</font></b>\n")
        sb.append("[C]$lineSeparator\n")

        // Shift Metadata
        sb.append("[L]Shift Date:[R]${dateFormatter.format(Date(report.dateInMillis))}\n")
        sb.append("[L]Shift:[R]${report.shift}\n")
        sb.append("[L]Cashier:[R]${report.cashierName}\n")
        sb.append("[C]$lineSeparator\n")

        // 1. SALES BY PAYMENT METHOD
        sb.append("[L]<b>1. SALES BY PAYMENT METHOD</b>\n")
        sb.append("[L]Cash Sales:[R]$currencySymbol${String.format("%.2f", report.grossCash)}\n")
        sb.append("[L]Card / Mada:[R]$currencySymbol${String.format("%.2f", report.madaPayments)}\n")
        if (report.digitalWallet > 0) {
            sb.append("[L]Digital Wallet:[R]$currencySymbol${String.format("%.2f", report.digitalWallet)}\n")
        }
        sb.append("[L]<b>TOTAL REVENUE:</b>[R]<b>$currencySymbol${String.format("%.2f", report.totalSales)}</b>\n")
        sb.append("[C]$lineSeparator\n")

        // 2. CASH DRAWER RECONCILIATION
        sb.append("[L]<b>2. CASH RECONCILIATION</b>\n")
        if (startingCash > 0) {
            sb.append("[L]Starting Float:[R]$currencySymbol${String.format("%.2f", startingCash)}\n")
        }
        sb.append("[L](+) Cash Sales:[R]$currencySymbol${String.format("%.2f", report.grossCash)}\n")
        if (cashIn > 0) {
            sb.append("[L](+) Cash In (Dues):[R]$currencySymbol${String.format("%.2f", cashIn)}\n")
        }
        if (cashOut > 0) {
            sb.append("[L](-) Cash Out:[R]$currencySymbol${String.format("%.2f", cashOut)}\n")
        }
        sb.append("[L]<b>EXPECTED CASH:</b>[R]<b>$currencySymbol${String.format("%.2f", expectedCash)}</b>\n")

        if (actualCashCount != null) {
            sb.append("[L]<b>ACTUAL CASH COUNT:</b>[R]<b>$currencySymbol${String.format("%.2f", actualCashCount)}</b>\n")
            val varFormatted = when {
                variance == null -> "N/A"
                Math.abs(variance) < 0.01 -> "${currencySymbol}0.00 (Balanced)"
                variance > 0 -> "+$currencySymbol${String.format("%.2f", variance)} (OVER)"
                else -> "-$currencySymbol${String.format("%.2f", Math.abs(variance))} (SHORT)"
            }
            sb.append("[L]<b>VARIANCE:</b>[R]<b>$varFormatted</b>\n")
        }
        sb.append("[C]$lineSeparator\n")

        // 3. TAX & VAT SUMMARY
        val netTaxable = report.totalSales / 1.15
        val vatAmount = report.totalSales - netTaxable
        sb.append("[L]<b>3. TAX & VAT SUMMARY</b>\n")
        sb.append("[L]Net Taxable Sales:[R]$currencySymbol${String.format("%.2f", netTaxable)}\n")
        sb.append("[L]VAT (15%):[R]$currencySymbol${String.format("%.2f", vatAmount)}\n")
        sb.append("[L]Gross Total (Inc VAT):[R]$currencySymbol${String.format("%.2f", report.totalSales)}\n")
        sb.append("[C]$lineSeparator\n")

        // 4. OTHER TRACKING
        if (report.totalDueCredit > 0 || report.staffMealsCount > 0) {
            sb.append("[L]Credit Sales:[R]$currencySymbol${String.format("%.2f", report.totalDueCredit)}\n")
            if (report.staffMealsCount > 0) {
                sb.append("[L]Staff Meals Count:[R]${report.staffMealsCount}\n")
            }
            sb.append("[C]$lineSeparator\n")
        }

        // Clean Notes
        val cleanNotes = com.lojia.shiftreport.util.PdfReportGenerator.cleanDisplayNotes(report.notes)
        if (cleanNotes.isNotBlank()) {
            sb.append("[L]Notes:\n")
            cleanNotes.split("\n").forEach { line ->
                if (line.isNotBlank()) {
                    sb.append("[L]  $line\n")
                }
            }
            sb.append("[C]$lineSeparator\n")
        }

        // Footer
        sb.append("[C]Computer Generated Z-Report\n")
        sb.append("[C]Lojia POS System\n")
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
