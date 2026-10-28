package com.lojia.shiftreport.printer

import android.graphics.Bitmap

/**
 * Common printer abstraction for network socket (WiFi/Ethernet) and Bluetooth thermal printers.
 */
interface PrinterManager {
    suspend fun connect(device: PrinterDevice): Result<Unit>
    suspend fun printBitmap(bitmap: Bitmap): Result<Unit>
    suspend fun printText(text: String): Result<Unit>
    fun disconnect()
    fun isConnected(): Boolean
    fun getConnectedDevice(): PrinterDevice?
}
