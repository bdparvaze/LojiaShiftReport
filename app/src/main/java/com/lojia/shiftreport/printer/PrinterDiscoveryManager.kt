package com.lojia.shiftreport.printer

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.lojia.shiftreport.util.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.InetAddress
import java.util.regex.Pattern

/**
 * Discovers network receipt printers using Android Network Service Discovery (NSD/mDNS/Bonjour).
 * Searches for _printer._tcp and _pdl-datastream._tcp services commonly broadcast by Epson,
 * Star Micronics, Bixolon, Sunmi, and Xprinter devices.
 */
class PrinterDiscoveryManager(private val context: Context) {

    companion object {
        private const val TAG = "PrinterDiscovery"
        const val SERVICE_TYPE_PRINTER = "_printer._tcp."
        const val SERVICE_TYPE_PDL = "_pdl-datastream._tcp."

        private val IP_PATTERN = Pattern.compile(
            "^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$"
        )

        fun isValidIpAddress(ip: String): Boolean {
            return ip.isNotBlank() && IP_PATTERN.matcher(ip.trim()).matches()
        }
    }

    private val nsdManager: NsdManager? =
        context.getSystemService(Context.NSD_SERVICE) as? NsdManager

    private val _discoveredPrinters = MutableStateFlow<List<PrinterDevice>>(emptyList())
    val discoveredPrinters: StateFlow<List<PrinterDevice>> = _discoveredPrinters.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    private var printerListener: NsdManager.DiscoveryListener? = null
    private var pdlListener: NsdManager.DiscoveryListener? = null

    /**
     * Starts discovering printers on the local WiFi network.
     */
    fun startDiscovery() {
        if (_isDiscovering.value || nsdManager == null) return
        _isDiscovering.value = true
        _discoveredPrinters.value = emptyList()
        AppLogger.i(TAG, "Starting network printer discovery (NSD)...")

        printerListener = createDiscoveryListener(SERVICE_TYPE_PRINTER)
        pdlListener = createDiscoveryListener(SERVICE_TYPE_PDL)

        try {
            nsdManager.discoverServices(SERVICE_TYPE_PRINTER, NsdManager.PROTOCOL_DNS_SD, printerListener)
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to start discovery for $SERVICE_TYPE_PRINTER: ${e.message}")
        }

        try {
            nsdManager.discoverServices(SERVICE_TYPE_PDL, NsdManager.PROTOCOL_DNS_SD, pdlListener)
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to start discovery for $SERVICE_TYPE_PDL: ${e.message}")
        }
    }

    /**
     * Stops network discovery.
     */
    fun stopDiscovery() {
        if (!_isDiscovering.value || nsdManager == null) return
        AppLogger.i(TAG, "Stopping network printer discovery...")

        printerListener?.let {
            try {
                nsdManager.stopServiceDiscovery(it)
            } catch (e: Exception) {
                AppLogger.w(TAG, "Error stopping printer discovery: ${e.message}")
            }
        }
        printerListener = null

        pdlListener?.let {
            try {
                nsdManager.stopServiceDiscovery(it)
            } catch (e: Exception) {
                AppLogger.w(TAG, "Error stopping PDL discovery: ${e.message}")
            }
        }
        pdlListener = null

        _isDiscovering.value = false
    }

    private fun createDiscoveryListener(serviceType: String): NsdManager.DiscoveryListener {
        return object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                AppLogger.d(TAG, "NSD discovery started: $regType")
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                AppLogger.d(TAG, "Service found: ${service.serviceName} (${service.serviceType})")
                resolveService(service)
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                AppLogger.d(TAG, "Service lost: ${service.serviceName}")
                val current = _discoveredPrinters.value.toMutableList()
                current.removeAll { it.name == service.serviceName }
                _discoveredPrinters.value = current
            }

            override fun onDiscoveryStopped(serviceType: String) {
                AppLogger.d(TAG, "Discovery stopped: $serviceType")
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                AppLogger.e(TAG, "Discovery start failed ($errorCode) for $serviceType")
                _isDiscovering.value = false
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                AppLogger.e(TAG, "Discovery stop failed ($errorCode) for $serviceType")
                _isDiscovering.value = false
            }
        }
    }

    private fun resolveService(serviceInfo: NsdServiceInfo) {
        val resolverListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                AppLogger.w(TAG, "Resolve failed ($errorCode) for ${serviceInfo.serviceName}")
            }

            override fun onServiceResolved(resolvedService: NsdServiceInfo) {
                val host: InetAddress? = resolvedService.host
                val hostAddress = host?.hostAddress ?: return
                val port = if (resolvedService.port > 0) resolvedService.port else 9100
                val printerName = resolvedService.serviceName ?: "Network Printer"

                AppLogger.i(TAG, "Resolved printer: $printerName at $hostAddress:$port")

                val device = PrinterDevice(
                    name = printerName,
                    address = hostAddress,
                    type = PrinterType.WIFI,
                    port = port
                )

                val current = _discoveredPrinters.value.toMutableList()
                if (current.none { it.address == device.address }) {
                    current.add(device)
                    _discoveredPrinters.value = current
                }
            }
        }

        try {
            nsdManager?.resolveService(serviceInfo, resolverListener)
        } catch (e: Exception) {
            AppLogger.w(TAG, "Error resolving service ${serviceInfo.serviceName}: ${e.message}")
        }
    }
}
