package com.lojia.shiftreport.printer

enum class PrinterType {
    WIFI,
    BLUETOOTH
}

enum class PrinterConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    FAILED
}

enum class PrinterCutMode {
    NONE,
    PARTIAL,
    FULL;

    companion object {
        fun fromStorageValue(raw: String?): PrinterCutMode? = when (raw?.trim()?.uppercase()) {
            "NONE" -> NONE
            "PARTIAL" -> PARTIAL
            "FULL" -> FULL
            else -> null
        }

        fun defaultForConnectionType(connectionType: String): PrinterCutMode =
            if (connectionType.trim().equals("network", ignoreCase = true)) FULL else NONE
    }
}

data class PrinterDevice(
    val name: String,
    val address: String, // IPv4 address for WiFi, MAC address for Bluetooth
    val type: PrinterType,
    val port: Int = WifiPrinterManager.DEFAULT_PORT
)

object PrinterConfigHelper {
    const val CONNECTION_NETWORK = "network"
    const val CONNECTION_BLUETOOTH = "bluetooth"

    fun buildNetworkIdentifier(host: String, port: Int): String {
        val cleanHost = host.trim()
        return if (cleanHost.isEmpty()) "" else "$cleanHost:$port"
    }

    fun buildCutModeStorageKey(connectionType: String, identifier: String): String {
        val normalizedType = if (connectionType.trim().equals(CONNECTION_NETWORK, ignoreCase = true)) {
            CONNECTION_NETWORK
        } else {
            CONNECTION_BLUETOOTH
        }
        val normalizedId = identifier.trim().lowercase()
        return if (normalizedId.isEmpty()) {
            "key_printer_cut_mode_$normalizedType"
        } else {
            "key_printer_cut_mode_${normalizedType}_$normalizedId"
        }
    }

    fun resolveCutMode(storedRawValue: String?, connectionType: String): PrinterCutMode {
        return PrinterCutMode.fromStorageValue(storedRawValue)
            ?: PrinterCutMode.defaultForConnectionType(connectionType)
    }
}

object PrinterTestTags {
    const val INPUT_PRINTER_NAME = "inputPrinterName"
    const val BTN_AUTO_DISCOVER = "btnAutoDiscoverPrinter"
    const val BTN_CONNECT_NETWORK = "btnConnectNetworkPrinter"
    const val CHIP_PAPER_WIDTH_58 = "chipPaperWidth58"
    const val CHIP_PAPER_WIDTH_80 = "chipPaperWidth80"
    const val CHIP_CUT_MODE_NONE = "chipCutModeNone"
    const val CHIP_CUT_MODE_PARTIAL = "chipCutModePartial"
    const val CHIP_CUT_MODE_FULL = "chipCutModeFull"
}
