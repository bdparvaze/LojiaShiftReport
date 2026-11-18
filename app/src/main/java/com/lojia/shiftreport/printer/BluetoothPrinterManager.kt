package com.lojia.shiftreport.printer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import com.lojia.shiftreport.util.AppLogger
import com.lojia.shiftreport.R
import com.lojia.shiftreport.data.PreferencesRepository
import com.lojia.shiftreport.util.DateTimeFormatUtils
import com.lojia.shiftreport.util.MoneyFormat
import com.lojia.shiftreport.util.paperWidthDots
import com.lojia.shiftreport.util.vatHeaderLines
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

    data class EncodedReceiptBytes(
        val bytes: ByteArray,
        val usedTextFallback: Boolean
    )

    /**
     * Encodes a receipt text formatted with [L], [C], [R], <b>, <font> tags into native ESC/POS bytes.
     */
    fun encodeFormattedText(text: String, charsPerLine: Int, widthPx: Int): ByteArray =
        encodeFormattedTextWithStatus(text, charsPerLine, widthPx).bytes

    fun encodeFormattedTextWithStatus(
        text: String,
        charsPerLine: Int,
        widthPx: Int,
        bitmapLineRasterizer: (String, Int) -> ByteArray = { line, width ->
            val bmp = ThermalBitmapRenderer.renderLineToBitmap(line, widthPx = width)
            bitmapToRasterEscPos(bmp)
        }
    ): EncodedReceiptBytes {
        val out = ByteArrayOutputStream()
        out.write(INIT)
        var usedTextFallback = false

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
                    out.write(bitmapLineRasterizer(line, widthPx))
                } catch (_: Exception) {
                    usedTextFallback = true
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
        return EncodedReceiptBytes(bytes = out.toByteArray(), usedTextFallback = usedTextFallback)
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
class BluetoothPrinterManager(private val context: Context) : PrinterManager {

    private val prefsRepository = PreferencesRepository.getInstance(context)

    data class ZReportCashDrawerMinor(
        val expectedCash: Long,
        val variance: Long?
    )

    companion object {
        private const val TAG = "BluetoothPrinterManager"

        fun calculateZReportCashDrawerMinor(
            openingCash: Long,
            grossCash: Long,
            totalPreviousDueCash: Long,
            totalCashOut: Long,
            actualCashCount: Long?
        ): ZReportCashDrawerMinor {
            val expected = MoneyFormat.calculateExpectedCashMinor(
                startingCash = openingCash,
                cashSales = grossCash,
                payIn = totalPreviousDueCash,
                payOut = totalCashOut
            )
            val variance = actualCashCount?.let { MoneyFormat.calculateVarianceMinor(it, expected) }
            return ZReportCashDrawerMinor(expectedCash = expected, variance = variance)
        }

        fun resolvePrintOutcome(usedFallback: Boolean, error: Throwable? = null): Result<Boolean> {
            if (error != null) return Result.failure(error)
            return Result.success(!usedFallback)
        }
    }

    private var activeSocket: BluetoothSocket? = null
    private var connectedBluetoothDevice: PrinterDevice? = null

    override fun isConnected(): Boolean {
        return activeSocket?.isConnected == true
    }

    override fun getConnectedDevice(): PrinterDevice? = connectedBluetoothDevice

    @SuppressLint("MissingPermission")
    override suspend fun connect(device: PrinterDevice): Result<Unit> = withContext(Dispatchers.IO) {
        disconnect()
        if (!hasBluetoothPermissions()) {
            return@withContext Result.failure(SecurityException("Bluetooth permission required"))
        }
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            ?: return@withContext Result.failure(IllegalStateException("Bluetooth adapter unavailable."))
        if (!bluetoothAdapter.isEnabled) {
            return@withContext Result.failure(IllegalStateException("Bluetooth is disabled."))
        }
        try {
            val remoteDevice = bluetoothAdapter.getRemoteDevice(device.address)
                ?: return@withContext Result.failure(IllegalStateException("Device not found at ${device.address}"))
            bluetoothAdapter.cancelDiscovery()
            val socket = remoteDevice.createRfcommSocketToServiceRecord(EscPosNativeProtocol.SPP_UUID)
            socket.connect()
            activeSocket = socket
            connectedBluetoothDevice = device
            prefsRepository.savePrinterAddress(device.address)
            prefsRepository.savePrinterConnectionType("bluetooth")
            AppLogger.i(TAG, "Connected to Bluetooth printer: ${device.name}")
            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to connect to Bluetooth printer ${device.address}", e)
            disconnect()
            Result.failure(e)
        }
    }

    override fun disconnect() {
        try {
            activeSocket?.close()
        } catch (e: Exception) {
            AppLogger.w(TAG, "Error closing Bluetooth socket: ${e.message}")
        } finally {
            activeSocket = null
            connectedBluetoothDevice = null
        }
    }

    override suspend fun printBitmap(bitmap: Bitmap): Result<Unit> = withContext(Dispatchers.IO) {
        val bytes = EscPosNativeProtocol.bitmapToRasterEscPos(bitmap)
        val socket = activeSocket
        if (socket == null || !socket.isConnected) {
            val printerAddress = getSavedPrinterAddress()
            if (printerAddress.isBlank()) {
                return@withContext Result.failure(IllegalStateException("No Bluetooth printer configured."))
            }
            val tempDevice = PrinterDevice("Bluetooth Printer", printerAddress, PrinterType.BLUETOOTH)
            val connRes = connect(tempDevice)
            if (connRes.isFailure) return@withContext connRes
        }
        try {
            activeSocket?.outputStream?.let { out ->
                out.write(EscPosNativeProtocol.INIT)
                out.write(bytes)
                out.write(EscPosNativeProtocol.LINE_FEED)
                out.write(EscPosNativeProtocol.LINE_FEED)
                out.write(EscPosNativeProtocol.PAPER_CUT)
                out.flush()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to print bitmap over Bluetooth", e)
            Result.failure(e)
        }
    }

    override suspend fun printText(text: String): Result<Unit> = withContext(Dispatchers.IO) {
        val res = printFormattedText(text)
        res.fold(
            onSuccess = { fullSuccess ->
                if (fullSuccess) {
                    Result.success(Unit)
                } else {
                    Result.failure(IllegalStateException(context.getString(R.string.printer_warning_message)))
                }
            },
            onFailure = { Result.failure(it) }
        )
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
     * Returns Result.success(true) on full print success, Result.success(false) if bitmap rendering failed
     * and the text fallback was sent instead, or Result.failure on error.
     */
    suspend fun printFormattedText(formattedText: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val connectionType = getPrinterConnectionType()
        if (connectionType == "network") {
            return@withContext WifiPrinterManager(context).printText(formattedText).map { true }
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
            val paperWidth = PrinterPaperWidth.fromWidthMm(getSavedPaperWidthMm())
            val widthPx = paperWidthDots(paperWidth.widthMm)
            val encoded = EscPosNativeProtocol.encodeFormattedTextWithStatus(formattedText, paperWidth.charsPerLine, widthPx)

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
                out.write(encoded.bytes)
                out.flush()
            }
            socket.close()

            if (encoded.usedTextFallback) {
                AppLogger.w(TAG, "Receipt printed over Bluetooth to $printerAddress using text fallback; non-Latin text may be omitted")
            } else {
                AppLogger.d(TAG, "Receipt successfully printed over native Bluetooth to $printerAddress")
            }
            resolvePrintOutcome(usedFallback = encoded.usedTextFallback)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to print to Bluetooth ESC/POS printer: ${e.message}", e)
            resolvePrintOutcome(usedFallback = false, error = e)
        }
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
        currencySymbol: String = "",
        currencyCode: String
    ): String = buildShiftReportText(
        businessName, businessAddress, businessPhone, vatNumber, report, currencySymbol, currencyCode
    )

    fun buildShiftReportText(
        businessName: String,
        businessAddress: String = "",
        businessPhone: String = "",
        vatNumber: String = "",
        report: com.lojia.shiftreport.data.ShiftReport,
        currencySymbol: String = "",
        currencyCode: String
    ): String {
        val paperWidth = PrinterPaperWidth.fromWidthMm(getSavedPaperWidthMm())
        val lineSeparator = "-".repeat(paperWidth.charsPerLine)
        val formattedDate = DateTimeFormatUtils.formatDateTime(report.dateInMillis)
        val reportDigits = MoneyFormat.minorUnitDigits(currencyCode)

        val startingCashMinor = if (report.openingCash > 0L) report.openingCash else com.lojia.shiftreport.util.PdfReportGenerator.extractStartingCashMinor(report, reportDigits)
        val actualCashCountMinor = if (report.closingCash > 0L) report.closingCash else com.lojia.shiftreport.util.PdfReportGenerator.extractActualCashMinor(report, reportDigits)
        val cashInMinor = report.totalPreviousDueCash
        val cashOutMinor = report.totalCashOut
        val grossCashMinor = report.grossCash
        val cashRecon = calculateZReportCashDrawerMinor(
            openingCash = startingCashMinor,
            grossCash = grossCashMinor,
            totalPreviousDueCash = cashInMinor,
            totalCashOut = cashOutMinor,
            actualCashCount = actualCashCountMinor
        )
        val expectedCashMinor = cashRecon.expectedCash
        val varianceMinor = cashRecon.variance

        val sb = StringBuilder()

        // Header
        sb.append("[C]<b><font size='big'>$businessName</font></b>\n")
        if (businessAddress.isNotBlank()) sb.append("[C]$businessAddress\n")
        if (businessPhone.isNotBlank()) sb.append("[C]Tel: $businessPhone\n")
        vatHeaderLines(vatNumber).forEach { sb.append("[C]VAT ID: $it\n") }
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
        sb.append("[L]${context.getString(R.string.zreport_cash_sales)}[R]${MoneyFormat.formatMinor(grossCashMinor, currencySymbol)}\n")
        sb.append("[L]${context.getString(R.string.zreport_card_mada)}[R]$currencySymbol${MoneyFormat.toPlainDecimal(report.madaPayments, reportDigits)}\n")
        if (report.digitalWallet > 0L) {
            sb.append("[L]${context.getString(R.string.zreport_digital_wallet)}[R]$currencySymbol${MoneyFormat.toPlainDecimal(report.digitalWallet, reportDigits)}\n")
        }
        sb.append("[L]<b>${context.getString(R.string.zreport_total_revenue)}</b>[R]<b>$currencySymbol${MoneyFormat.toPlainDecimal(report.totalSales, reportDigits)}</b>\n")
        sb.append("[C]$lineSeparator\n")

        // 2. CASH DRAWER RECONCILIATION
        sb.append("[L]<b>${context.getString(R.string.zreport_sec_cash_recon)}</b>\n")
        if (startingCashMinor > 0L) {
            sb.append("[L]${context.getString(R.string.zreport_starting_float)}[R]${MoneyFormat.formatMinor(startingCashMinor, currencySymbol)}\n")
        }
        sb.append("[L](+) ${context.getString(R.string.zreport_cash_sales)}[R]${MoneyFormat.formatMinor(grossCashMinor, currencySymbol)}\n")
        if (cashInMinor > 0L) {
            sb.append("[L]${context.getString(R.string.zreport_cash_in_dues)}[R]${MoneyFormat.formatMinor(cashInMinor, currencySymbol)}\n")
        }
        if (cashOutMinor > 0L) {
            sb.append("[L]${context.getString(R.string.zreport_cash_out)}[R]${MoneyFormat.formatMinor(cashOutMinor, currencySymbol)}\n")
        }
        sb.append("[L]<b>${context.getString(R.string.zreport_expected_cash)}</b>[R]<b>${MoneyFormat.formatMinor(expectedCashMinor, currencySymbol)}</b>\n")

        if (actualCashCountMinor != null && varianceMinor != null) {
            sb.append("[L]<b>${context.getString(R.string.zreport_actual_cash)}</b>[R]<b>${MoneyFormat.formatMinor(actualCashCountMinor, currencySymbol)}</b>\n")
            val varFormatted = when {
                varianceMinor == 0L -> "${MoneyFormat.formatMinor(0L, currencySymbol)} (${context.getString(R.string.zreport_balanced)})"
                varianceMinor > 0L -> "+${MoneyFormat.formatMinor(varianceMinor, currencySymbol)} (${context.getString(R.string.zreport_over)})"
                else -> "-${MoneyFormat.formatMinor(Math.abs(varianceMinor), currencySymbol)} (${context.getString(R.string.zreport_short)})"
            }
            sb.append("[L]<b>${context.getString(R.string.zreport_variance)}</b>[R]<b>$varFormatted</b>\n")
        }
        sb.append("[C]$lineSeparator\n")

        // 3. OTHER TRACKING
        val totalDueCredit = MoneyFormat.toMajorUnits(report.totalDueCredit, reportDigits)
        if (totalDueCredit > 0.0 || report.staffMealsCount > 0) {
            sb.append("[L]<b>${context.getString(R.string.zreport_sec_other)}</b>\n")
            if (totalDueCredit > 0.0) {
                sb.append("[L]${context.getString(R.string.zreport_credit_sales)}[R]$currencySymbol${MoneyFormat.toPlainDecimal(report.totalDueCredit, reportDigits)}\n")
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
