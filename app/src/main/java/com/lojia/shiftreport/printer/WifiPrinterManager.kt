package com.lojia.shiftreport.printer

import android.content.Context
import android.graphics.Bitmap
import com.lojia.shiftreport.R
import com.lojia.shiftreport.data.PreferencesRepository
import com.lojia.shiftreport.util.AppLogger
import com.lojia.shiftreport.util.paperWidthDots
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Pure JVM-testable TCP print job coordinator ensuring disconnected sessions
 * auto-connect before sending bytes and close cleanly when auto-connected for one-shot jobs.
 */
internal object NetworkPrintCoordinator {
    interface TcpPrintTransport {
        fun isConnected(): Boolean
        fun connect(host: String, port: Int)
        fun writeAndFlush(bytes: ByteArray)
        fun close()
    }

    fun resolveTarget(host: String, port: Int): Pair<String, Int>? {
        val cleanHost = host.trim()
        if (cleanHost.isEmpty() || port !in 1..65535) return null
        return cleanHost to port
    }

    fun sendWithAutoConnect(
        transport: TcpPrintTransport,
        host: String,
        port: Int,
        payload: ByteArray,
        closeAfterAutoConnect: Boolean
    ): Result<Unit> {
        val wasAlreadyConnected = transport.isConnected()
        var didAutoConnect = false
        return try {
            if (!wasAlreadyConnected) {
                transport.connect(host, port)
                didAutoConnect = true
            }
            transport.writeAndFlush(payload)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            if (didAutoConnect && closeAfterAutoConnect) {
                try {
                    transport.close()
                } catch (_: Exception) {
                }
            }
        }
    }
}

/**
 * Primary network POS thermal printer manager using raw TCP socket (Port 9100).
 * Shares [EscPosNativeProtocol] encoding with [BluetoothPrinterManager] so receipt
 * formatting, non-Latin bitmap rendering, and cut modes match across transports.
 */
class WifiPrinterManager(private val context: Context) : PrinterManager {

    private val prefsRepository = PreferencesRepository.getInstance(context)

    companion object {
        private const val TAG = "WifiPrinterManager"
        const val DEFAULT_PORT = 9100 // Raw TCP / AppSocket (HP JetDirect) standard raw-print port
        const val CONNECTION_TIMEOUT_MS = 5000
        const val SOCKET_TIMEOUT_MS = 5000
    }

    private var socket: Socket? = null
    private var outputStream: OutputStream? = null

    private val _status = MutableStateFlow(PrinterConnectionStatus.DISCONNECTED)
    val status: StateFlow<PrinterConnectionStatus> = _status.asStateFlow()

    private val _connectedDevice = MutableStateFlow<PrinterDevice?>(null)
    val connectedDevice: StateFlow<PrinterDevice?> = _connectedDevice.asStateFlow()

    override fun isConnected(): Boolean {
        val s = socket
        return s != null && s.isConnected && !s.isClosed && outputStream != null
    }

    override fun getConnectedDevice(): PrinterDevice? = _connectedDevice.value

    /**
     * Connects to a WiFi / Network POS printer on port 9100 (or configured port) with a 5-second timeout.
     */
    override suspend fun connect(device: PrinterDevice): Result<Unit> = withContext(Dispatchers.IO) {
        val target = NetworkPrintCoordinator.resolveTarget(device.address, device.port)
        if (target == null) {
            val msg = if (device.address.isBlank()) {
                context.getString(R.string.printer_error_no_network_ip)
            } else {
                context.getString(R.string.printer_error_invalid_port)
            }
            _status.value = PrinterConnectionStatus.FAILED
            return@withContext Result.failure(IllegalArgumentException(msg))
        }

        disconnect()
        _status.value = PrinterConnectionStatus.CONNECTING
        AppLogger.i(TAG, "Connecting to WiFi printer at ${target.first}:${target.second}...")

        try {
            val newSocket = Socket()
            newSocket.soTimeout = SOCKET_TIMEOUT_MS
            val socketAddress = InetSocketAddress(target.first, target.second)
            newSocket.connect(socketAddress, CONNECTION_TIMEOUT_MS)

            val out = newSocket.getOutputStream()
            socket = newSocket
            outputStream = out

            // Initialize printer hardware with ESC @
            out.write(EscPosNativeProtocol.INIT)
            out.flush()

            _status.value = PrinterConnectionStatus.CONNECTED
            _connectedDevice.value = device.copy(address = target.first, port = target.second)
            AppLogger.i(TAG, "Successfully connected to WiFi printer: ${target.first}:${target.second}")
            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to connect to WiFi printer at ${target.first}:${target.second}", e)
            disconnect()
            _status.value = PrinterConnectionStatus.FAILED
            val errorMsg = e.localizedMessage?.takeIf { it.isNotBlank() }
                ?: context.getString(R.string.printer_error_network_connect_failed, "${target.first}:${target.second}")
            Result.failure(IllegalStateException(errorMsg, e))
        }
    }

    private fun resolveTargetDevice(): Result<PrinterDevice> {
        connectedDevice.value?.let { connected ->
            if (connected.address.isNotBlank() && connected.port in 1..65535) {
                return Result.success(connected)
            }
        }
        val savedIp = prefsRepository.getPrinterNetworkIp().trim()
        if (savedIp.isEmpty()) {
            return Result.failure(IllegalStateException(context.getString(R.string.printer_error_no_network_ip)))
        }
        val savedPort = prefsRepository.getPrinterNetworkPort()
        if (savedPort !in 1..65535) {
            return Result.failure(IllegalStateException(context.getString(R.string.printer_error_invalid_port)))
        }
        val savedName = prefsRepository.getPrinterNetworkName().ifBlank {
            context.getString(R.string.printer_wifi_default_name)
        }
        return Result.success(PrinterDevice(savedName, savedIp, PrinterType.WIFI, savedPort))
    }

    private fun activeCutModeFor(device: PrinterDevice): PrinterCutMode {
        val identifier = PrinterConfigHelper.buildNetworkIdentifier(device.address, device.port)
        return prefsRepository.getPrinterCutMode(PrinterConfigHelper.CONNECTION_NETWORK, identifier)
    }

    private suspend fun sendBytesOverNetwork(
        targetDevice: PrinterDevice,
        bytes: ByteArray,
        closeAfterAutoConnect: Boolean
    ): Result<Unit> {
        val wasAlreadyConnected = isConnected() &&
            connectedDevice.value?.address == targetDevice.address &&
            connectedDevice.value?.port == targetDevice.port

        if (!wasAlreadyConnected) {
            val connRes = connect(targetDevice)
            if (connRes.isFailure) return connRes
        }

        return try {
            val out = outputStream
                ?: return Result.failure(IllegalStateException(context.getString(R.string.printer_error_not_connected)))
            out.write(bytes)
            out.flush()
            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed sending print job to ${targetDevice.address}:${targetDevice.port}", e)
            disconnect()
            Result.failure(
                IllegalStateException(
                    e.localizedMessage?.takeIf { it.isNotBlank() }
                        ?: context.getString(
                            R.string.printer_error_network_connect_failed,
                            "${targetDevice.address}:${targetDevice.port}"
                        ),
                    e
                )
            )
        } finally {
            if (!wasAlreadyConnected && closeAfterAutoConnect) {
                disconnect()
            }
        }
    }

    /**
     * Sends a raw monochrome or grayscale bitmap to the printer using ESC/POS raster command (GS v 0).
     * Automatically connects using saved network settings when disconnected.
     */
    override suspend fun printBitmap(bitmap: Bitmap): Result<Unit> = withContext(Dispatchers.IO) {
        val targetRes = resolveTargetDevice()
        val targetDevice = targetRes.getOrElse { return@withContext Result.failure(it) }
        val cutMode = activeCutModeFor(targetDevice)
        val payload = EscPosNativeProtocol.encodeBitmapPrintJob(bitmap, cutMode)
        sendBytesOverNetwork(targetDevice, payload, closeAfterAutoConnect = false)
    }

    /**
     * Prints formatted ESC/POS text using the shared [EscPosNativeProtocol] encoder
     * and the configured per-printer [PrinterCutMode]. Auto-connects when disconnected.
     */
    override suspend fun printText(text: String): Result<Unit> = withContext(Dispatchers.IO) {
        printFormattedTextInternal(text, closeAfterAutoConnect = false).fold(
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

    suspend fun printFormattedTextWithStatus(text: String): Result<Boolean> = withContext(Dispatchers.IO) {
        printFormattedTextInternal(text, closeAfterAutoConnect = true)
    }

    private suspend fun printFormattedTextInternal(
        text: String,
        closeAfterAutoConnect: Boolean
    ): Result<Boolean> {
        val targetRes = resolveTargetDevice()
        val targetDevice = targetRes.getOrElse { return Result.failure(it) }

        val paperWidth = PrinterPaperWidth.fromWidthMm(prefsRepository.getPrinterPaperWidth())
        val widthPx = paperWidthDots(paperWidth.widthMm)
        val cutMode = activeCutModeFor(targetDevice)
        val encoded = EscPosNativeProtocol.encodeFormattedTextWithStatus(
            text = text,
            charsPerLine = paperWidth.charsPerLine,
            widthPx = widthPx,
            cutMode = cutMode
        )

        val sendRes = sendBytesOverNetwork(targetDevice, encoded.bytes, closeAfterAutoConnect)
        return sendRes.fold(
            onSuccess = { BluetoothPrinterManager.resolvePrintOutcome(usedFallback = encoded.usedTextFallback) },
            onFailure = { Result.failure(it) }
        )
    }

    /**
     * Prints the international multilingual test receipt using the configured paper width & cut mode.
     */
    suspend fun testPrint(): Result<Unit> = withContext(Dispatchers.IO) {
        val testText = "Lojia Test - تم اختبار الطباعة - টেস্ট"
        AppLogger.i(TAG, "Triggering POS multilingual test print: $testText")

        val widthPx = paperWidthDots(prefsRepository.getPrinterPaperWidth())
        val bmp = ThermalBitmapRenderer.renderLineToBitmap(
            rawLine = "<b>[C]$testText</b>",
            widthPx = widthPx,
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
}
