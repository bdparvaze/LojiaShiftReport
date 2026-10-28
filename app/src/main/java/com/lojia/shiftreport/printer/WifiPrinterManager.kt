package com.lojia.shiftreport.printer

import android.content.Context
import android.graphics.Bitmap
import com.lojia.shiftreport.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.charset.StandardCharsets

/**
 * Primary network POS thermal printer manager using raw TCP socket (Port 9100).
 * Implements ESC/POS commands with 5-second connection timeout, identical to Foodics POS.
 */
class WifiPrinterManager(private val context: Context) : PrinterManager {

    companion object {
        private const val TAG = "WifiPrinterManager"
        const val DEFAULT_PORT = 9100
        const val CONNECTION_TIMEOUT_MS = 5000
        const val SOCKET_TIMEOUT_MS = 5000

        // ESC/POS Command Constants
        val ESC_INIT = byteArrayOf(0x1B, 0x40) // ESC @
        val ALIGN_LEFT = byteArrayOf(0x1B, 0x61, 0x00) // ESC a 0
        val ALIGN_CENTER = byteArrayOf(0x1B, 0x61, 0x01) // ESC a 1
        val ALIGN_RIGHT = byteArrayOf(0x1B, 0x61, 0x02) // ESC a 2
        val LINE_FEED = byteArrayOf(0x0A) // LF
        val PAPER_CUT = byteArrayOf(0x1D, 0x56, 0x42, 0x00) // GS V B 0 (Full Cut)
        val PARTIAL_CUT = byteArrayOf(0x1D, 0x56, 0x01) // GS V 1 (Partial Cut)
    }

    private var socket: Socket? = null
    private var outputStream: OutputStream? = null

    private val _status = MutableStateFlow(PrinterConnectionStatus.DISCONNECTED)
    val status: StateFlow<PrinterConnectionStatus> = _status.asStateFlow()

    private val _connectedDevice = MutableStateFlow<PrinterDevice?>(null)
    val connectedDevice: StateFlow<PrinterDevice?> = _connectedDevice.asStateFlow()

    override fun isConnected(): Boolean {
        val s = socket
        return s != null && s.isConnected && !s.isClosed
    }

    override fun getConnectedDevice(): PrinterDevice? = _connectedDevice.value

    /**
     * Connects to a WiFi / Network POS printer on port 9100 with a 5-second timeout.
     */
    override suspend fun connect(device: PrinterDevice): Result<Unit> = withContext(Dispatchers.IO) {
        disconnect()
        _status.value = PrinterConnectionStatus.CONNECTING
        AppLogger.i(TAG, "Connecting to WiFi printer at ${device.address}:${device.port}...")

        try {
            val newSocket = Socket()
            newSocket.soTimeout = SOCKET_TIMEOUT_MS
            val socketAddress = InetSocketAddress(device.address, device.port)
            newSocket.connect(socketAddress, CONNECTION_TIMEOUT_MS)

            socket = newSocket
            outputStream = newSocket.getOutputStream()

            // Initialize printer hardware with ESC @
            outputStream?.write(ESC_INIT)
            outputStream?.flush()

            _status.value = PrinterConnectionStatus.CONNECTED
            _connectedDevice.value = device
            AppLogger.i(TAG, "Successfully connected to WiFi printer: ${device.address}")
            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to connect to WiFi printer at ${device.address}", e)
            disconnect()
            _status.value = PrinterConnectionStatus.FAILED
            Result.failure(e)
        }
    }

    /**
     * Sends a raw monochrome or grayscale bitmap to the printer using ESC/POS raster command (GS v 0).
     */
    override suspend fun printBitmap(bitmap: Bitmap): Result<Unit> = withContext(Dispatchers.IO) {
        val out = outputStream
        if (!isConnected() || out == null) {
            return@withContext Result.failure(IllegalStateException("Printer is not connected"))
        }

        try {
            val rasterBytes = bitmapToRasterEscPos(bitmap)
            out.write(ESC_INIT)
            out.write(ALIGN_CENTER)
            out.write(rasterBytes)
            out.write(LINE_FEED)
            out.write(LINE_FEED)
            out.write(LINE_FEED)
            out.write(PAPER_CUT)
            out.flush()
            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to print bitmap", e)
            Result.failure(e)
        }
    }

    /**
     * Prints formatted or plain text. If non-Latin scripts (Arabic, Bengali) are detected,
     * renders them into high-resolution bitmaps to avoid ?????? question mark artifacts.
     */
    override suspend fun printText(text: String): Result<Unit> = withContext(Dispatchers.IO) {
        val out = outputStream
        if (!isConnected() || out == null) {
            return@withContext Result.failure(IllegalStateException("Printer is not connected"))
        }

        try {
            out.write(ESC_INIT)
            val lines = text.split("\n")
            for (line in lines) {
                if (ThermalBitmapRenderer.containsNonLatin(line)) {
                    val bmp = ThermalBitmapRenderer.renderLineToBitmap(line, widthPx = 384)
                    val raster = bitmapToRasterEscPos(bmp)
                    out.write(raster)
                } else {
                    out.write(ALIGN_LEFT)
                    out.write(line.toByteArray(StandardCharsets.US_ASCII))
                    out.write(LINE_FEED)
                }
            }
            out.write(LINE_FEED)
            out.write(LINE_FEED)
            out.write(PAPER_CUT)
            out.flush()
            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to print text", e)
            Result.failure(e)
        }
    }

    /**
     * Prints the international multilingual test receipt requested:
     * "Lojia Test - تم اختبار الطباعة - টেস্ট"
     */
    suspend fun testPrint(): Result<Unit> = withContext(Dispatchers.IO) {
        val testText = "Lojia Test - تم اختبار الطباعة - টেস্ট"
        AppLogger.i(TAG, "Triggering POS multilingual test print: $testText")

        // Render full test line to bitmap to guarantee pristine Arabic and Bengali typography
        val bmp = ThermalBitmapRenderer.renderLineToBitmap(
            rawLine = "<b>[C]$testText</b>",
            widthPx = 384,
            fontSizePx = 28f
        )
        printBitmap(bmp)
    }

    override fun disconnect() {
        try {
            outputStream?.flush()
            outputStream?.close()
        } catch (e: Exception) {
            AppLogger.w(TAG, "Error closing printer output stream: ${e.message}")
        } finally {
            outputStream = null
        }

        try {
            socket?.close()
        } catch (e: Exception) {
            AppLogger.w(TAG, "Error closing printer socket: ${e.message}")
        } finally {
            socket = null
        }

        _status.value = PrinterConnectionStatus.DISCONNECTED
        _connectedDevice.value = null
    }

    /**
     * Converts an Android Bitmap into standard ESC/POS GS v 0 raster byte image.
     */
    private fun bitmapToRasterEscPos(bitmap: Bitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height
        val widthBytes = (width + 7) / 8
        val out = ByteArrayOutputStream()

        out.write(ALIGN_CENTER)
        // GS v 0 0 xL xH yL yH
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
}
