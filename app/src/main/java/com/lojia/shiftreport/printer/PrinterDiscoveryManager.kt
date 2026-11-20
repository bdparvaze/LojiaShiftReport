package com.lojia.shiftreport.printer

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import androidx.annotation.RequiresApi
import com.lojia.shiftreport.R
import com.lojia.shiftreport.util.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.Inet4Address
import java.net.InetAddress
import java.util.concurrent.Executors
import java.util.regex.Pattern

/**
 * Discovers network receipt printers using Android Network Service Discovery (NSD/mDNS/Bonjour).
 * Uses NsdManager.registerServiceInfoCallback on Android 14+ (API 34+) and resolveService on older APIs.
 * Acquires WifiManager.MulticastLock only during active discovery and releases it on stop/cleanup.
 */
class PrinterDiscoveryManager(private val context: Context) {

    companion object {
        private const val TAG = "PrinterDiscovery"
        private const val MULTICAST_LOCK_TAG = "lojia_printer_nsd_lock"
        const val SERVICE_TYPE_PRINTER = "_printer._tcp."
        const val SERVICE_TYPE_PDL = "_pdl-datastream._tcp."

        private val IP_PATTERN = Pattern.compile(
            "^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$"
        )

        fun isValidIpAddress(ip: String): Boolean {
            return ip.isNotBlank() && IP_PATTERN.matcher(ip.trim()).matches()
        }

        /**
         * Pure helper for merging and deduplicating discovered printers by (address, port).
         */
        fun mergeDiscoveredPrinter(
            current: List<PrinterDevice>,
            incoming: PrinterDevice
        ): List<PrinterDevice> {
            val cleanAddress = incoming.address.trim()
            if (cleanAddress.isEmpty()) return current
            val cleanName = incoming.name.trim().ifEmpty { cleanAddress }
            val normalized = incoming.copy(name = cleanName, address = cleanAddress)
            val existingIdx = current.indexOfFirst {
                it.address == normalized.address && it.port == normalized.port
            }
            return if (existingIdx >= 0) {
                current.toMutableList().apply { set(existingIdx, normalized) }
            } else {
                current + normalized
            }
        }

        /**
         * Pure helper for removing a lost NSD service from the discovered printer list.
         */
        fun removeLostPrinter(
            current: List<PrinterDevice>,
            serviceName: String
        ): List<PrinterDevice> {
            val cleanName = serviceName.trim()
            return current.filterNot { it.name == cleanName }
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
    private var multicastLock: WifiManager.MulticastLock? = null
    private val activeServiceCallbacks = mutableListOf<Any>()
    private val callbackExecutor by lazy { Executors.newSingleThreadExecutor() }

    /**
     * Starts discovering printers on the local WiFi network.
     */
    fun startDiscovery() {
        if (_isDiscovering.value || nsdManager == null) return
        _isDiscovering.value = true
        _discoveredPrinters.value = emptyList()
        acquireMulticastLock()
        AppLogger.i(TAG, "Starting network printer discovery (NSD)...")

        printerListener = createDiscoveryListener(SERVICE_TYPE_PRINTER)
        pdlListener = createDiscoveryListener(SERVICE_TYPE_PDL)

        var startedAny = false
        try {
            nsdManager.discoverServices(SERVICE_TYPE_PRINTER, NsdManager.PROTOCOL_DNS_SD, printerListener)
            startedAny = true
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to start discovery for $SERVICE_TYPE_PRINTER: ${e.message}")
            printerListener = null
        }

        try {
            nsdManager.discoverServices(SERVICE_TYPE_PDL, NsdManager.PROTOCOL_DNS_SD, pdlListener)
            startedAny = true
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to start discovery for $SERVICE_TYPE_PDL: ${e.message}")
            pdlListener = null
        }

        if (!startedAny) {
            _isDiscovering.value = false
            releaseMulticastLock()
        }
    }

    /**
     * Stops network discovery and always releases MulticastLock and API 34+ info callbacks.
     */
    fun stopDiscovery() {
        try {
            if (nsdManager != null) {
                printerListener?.let {
                    try {
                        nsdManager.stopServiceDiscovery(it)
                    } catch (e: Exception) {
                        AppLogger.w(TAG, "Error stopping printer discovery: ${e.message}")
                    }
                }
                pdlListener?.let {
                    try {
                        nsdManager.stopServiceDiscovery(it)
                    } catch (e: Exception) {
                        AppLogger.w(TAG, "Error stopping PDL discovery: ${e.message}")
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    unregisterAllServiceInfoCallbacks(nsdManager)
                }
            }
        } finally {
            printerListener = null
            pdlListener = null
            activeServiceCallbacks.clear()
            _isDiscovering.value = false
            releaseMulticastLock()
        }
    }

    private fun acquireMulticastLock() {
        if (multicastLock?.isHeld == true) return
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val lock = wifiManager?.createMulticastLock(MULTICAST_LOCK_TAG)?.apply {
                setReferenceCounted(false)
                acquire()
            }
            multicastLock = lock
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to acquire WifiManager.MulticastLock: ${e.message}")
        }
    }

    private fun releaseMulticastLock() {
        try {
            if (multicastLock?.isHeld == true) {
                multicastLock?.release()
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to release WifiManager.MulticastLock: ${e.message}")
        } finally {
            multicastLock = null
        }
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
                _discoveredPrinters.value = removeLostPrinter(_discoveredPrinters.value, service.serviceName)
            }

            override fun onDiscoveryStopped(serviceType: String) {
                AppLogger.d(TAG, "Discovery stopped: $serviceType")
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                AppLogger.e(TAG, "Discovery start failed ($errorCode) for $serviceType")
                _isDiscovering.value = false
                releaseMulticastLock()
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                AppLogger.e(TAG, "Discovery stop failed ($errorCode) for $serviceType")
                _isDiscovering.value = false
                releaseMulticastLock()
            }
        }
    }

    private fun resolveService(serviceInfo: NsdServiceInfo) {
        val manager = nsdManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            registerServiceInfoCallbackApi34(manager, serviceInfo)
        } else {
            resolveServiceLegacy(manager, serviceInfo)
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun registerServiceInfoCallbackApi34(manager: NsdManager, serviceInfo: NsdServiceInfo) {
        val callback = object : NsdManager.ServiceInfoCallback {
            override fun onServiceInfoCallbackRegistrationFailed(errorCode: Int) {
                AppLogger.w(TAG, "ServiceInfoCallback registration failed ($errorCode) for ${serviceInfo.serviceName}")
            }

            override fun onServiceUpdated(updatedInfo: NsdServiceInfo) {
                val hosts = updatedInfo.hostAddresses
                val preferredHost = hosts.firstOrNull { it is Inet4Address } ?: hosts.firstOrNull()
                val hostAddress = preferredHost?.hostAddress ?: return
                val port = if (updatedInfo.port > 0) updatedInfo.port else WifiPrinterManager.DEFAULT_PORT
                val printerName = updatedInfo.serviceName?.takeIf { it.isNotBlank() }
                    ?: context.getString(R.string.printer_wifi_default_name)

                AppLogger.i(TAG, "Resolved printer (API 34+): $printerName at $hostAddress:$port")
                _discoveredPrinters.value = mergeDiscoveredPrinter(
                    _discoveredPrinters.value,
                    PrinterDevice(
                        name = printerName,
                        address = hostAddress,
                        type = PrinterType.WIFI,
                        port = port
                    )
                )
            }

            override fun onServiceLost() {
                val name = serviceInfo.serviceName ?: return
                _discoveredPrinters.value = removeLostPrinter(_discoveredPrinters.value, name)
            }

            override fun onServiceInfoCallbackUnregistered() {
                AppLogger.d(TAG, "ServiceInfoCallback unregistered for ${serviceInfo.serviceName}")
            }
        }

        try {
            synchronized(activeServiceCallbacks) {
                activeServiceCallbacks.add(callback)
            }
            manager.registerServiceInfoCallback(serviceInfo, callbackExecutor, callback)
        } catch (e: Exception) {
            AppLogger.w(TAG, "Error registering ServiceInfoCallback for ${serviceInfo.serviceName}: ${e.message}")
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun unregisterAllServiceInfoCallbacks(manager: NsdManager) {
        val callbacksCopy = synchronized(activeServiceCallbacks) {
            activeServiceCallbacks.toList().also { activeServiceCallbacks.clear() }
        }
        for (cb in callbacksCopy) {
            if (cb is NsdManager.ServiceInfoCallback) {
                try {
                    manager.unregisterServiceInfoCallback(cb)
                } catch (e: Exception) {
                    AppLogger.w(TAG, "Error unregistering ServiceInfoCallback: ${e.message}")
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun resolveServiceLegacy(manager: NsdManager, serviceInfo: NsdServiceInfo) {
        val resolverListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                AppLogger.w(TAG, "Resolve failed ($errorCode) for ${serviceInfo.serviceName}")
            }

            override fun onServiceResolved(resolvedService: NsdServiceInfo) {
                val host: InetAddress? = resolvedService.host
                val hostAddress = host?.hostAddress ?: return
                val port = if (resolvedService.port > 0) resolvedService.port else WifiPrinterManager.DEFAULT_PORT
                val printerName = resolvedService.serviceName?.takeIf { it.isNotBlank() }
                    ?: context.getString(R.string.printer_wifi_default_name)

                AppLogger.i(TAG, "Resolved printer: $printerName at $hostAddress:$port")
                _discoveredPrinters.value = mergeDiscoveredPrinter(
                    _discoveredPrinters.value,
                    PrinterDevice(
                        name = printerName,
                        address = hostAddress,
                        type = PrinterType.WIFI,
                        port = port
                    )
                )
            }
        }

        try {
            manager.resolveService(serviceInfo, resolverListener)
        } catch (e: Exception) {
            AppLogger.w(TAG, "Error resolving service ${serviceInfo.serviceName}: ${e.message}")
        }
    }
}
