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

data class PrinterDevice(
    val name: String,
    val address: String, // IPv4 address for WiFi, MAC address for Bluetooth
    val type: PrinterType,
    val port: Int = 9100
)
