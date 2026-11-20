package com.lojia.shiftreport.printer

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojia.shiftreport.R
import com.lojia.shiftreport.data.PreferencesRepository
import com.lojia.shiftreport.ui.theme.*
import com.lojia.shiftreport.util.paperWidthDots
import kotlinx.coroutines.launch

/**
 * Universal POS Thermal Receipt Printer Setup Screen.
 * Provides dual-tab configuration:
 * 1. WiFi / Network Socket (Raw TCP Port 9100 standard) with mDNS / NSD Auto-Discovery
 * 2. Bluetooth RFCOMM SPP Printer
 * Includes per-printer cut mode, paper width selection, live connection testing,
 * and multilingual receipt sample printing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrinterSetupScreen(
    onClose: () -> Unit = {},
    showTopBar: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember(context) { PreferencesRepository.getInstance(context) }

    val wifiPrinterManager = remember(context) { WifiPrinterManager(context) }
    val bluetoothPrinterManager = remember(context) { BluetoothPrinterManager(context) }
    val discoveryManager = remember(context) { PrinterDiscoveryManager(context) }

    var selectedTabIndex by remember {
        mutableIntStateOf(
            if (prefs.getPrinterConnectionType() == PrinterConfigHelper.CONNECTION_NETWORK) 0 else 1
        )
    }

    // WiFi Tab States (no hardcoded fallback IP)
    var networkNameInput by remember {
        mutableStateOf(prefs.getPrinterNetworkName())
    }
    var ipInput by remember {
        mutableStateOf(prefs.getPrinterNetworkIp())
    }
    var portInput by remember {
        mutableStateOf(prefs.getPrinterNetworkPort().toString())
    }

    val discoveredPrinters by discoveryManager.discoveredPrinters.collectAsState()
    val isDiscovering by discoveryManager.isDiscovering.collectAsState()

    // Bluetooth Tab States
    var pairedPrinters by remember { mutableStateOf(bluetoothPrinterManager.getPairedPrinters()) }
    var selectedBluetoothAddress by remember {
        mutableStateOf(prefs.getSelectedPrinterAddress())
    }

    // Shared Paper Width & Per-Printer Cut Mode States
    var selectedPaperWidthMm by remember {
        mutableIntStateOf(PrinterPaperWidth.fromWidthMm(prefs.getPrinterPaperWidth()).widthMm)
    }

    val activeConnectionType = if (selectedTabIndex == 0) {
        PrinterConfigHelper.CONNECTION_NETWORK
    } else {
        PrinterConfigHelper.CONNECTION_BLUETOOTH
    }
    val activePrinterIdentifier = if (selectedTabIndex == 0) {
        PrinterConfigHelper.buildNetworkIdentifier(
            ipInput,
            portInput.toIntOrNull() ?: WifiPrinterManager.DEFAULT_PORT
        )
    } else {
        selectedBluetoothAddress
    }

    var selectedCutMode by remember {
        mutableStateOf(prefs.getPrinterCutMode(activeConnectionType, activePrinterIdentifier))
    }

    LaunchedEffect(activeConnectionType, activePrinterIdentifier) {
        selectedCutMode = prefs.getPrinterCutMode(activeConnectionType, activePrinterIdentifier)
    }

    // Connection & Print States
    var connectionStatus by remember { mutableStateOf(PrinterConnectionStatus.DISCONNECTED) }
    var connectedDevice by remember { mutableStateOf<PrinterDevice?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isOperating by remember { mutableStateOf(false) }

    // Clean up discovery and active sockets on dispose
    DisposableEffect(Unit) {
        onDispose {
            discoveryManager.stopDiscovery()
            wifiPrinterManager.disconnect()
            bluetoothPrinterManager.disconnect()
        }
    }

    Scaffold(
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Print,
                                contentDescription = null,
                                tint = PrimaryIndigo,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.printer_setup_title),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.testTag("btnClosePrinterSetup")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        bottomBar = {
            Surface(
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Status Badge Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when (connectionStatus) {
                                    PrinterConnectionStatus.CONNECTED -> SuccessGreen.copy(alpha = 0.12f)
                                    PrinterConnectionStatus.CONNECTING -> PrimaryIndigo.copy(alpha = 0.12f)
                                    PrinterConnectionStatus.FAILED -> ErrorRed.copy(alpha = 0.12f)
                                    PrinterConnectionStatus.DISCONNECTED -> Color.Gray.copy(alpha = 0.12f)
                                }
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when (connectionStatus) {
                                PrinterConnectionStatus.CONNECTED -> Icons.Default.CheckCircle
                                PrinterConnectionStatus.CONNECTING -> Icons.Default.Sync
                                PrinterConnectionStatus.FAILED -> Icons.Default.Error
                                PrinterConnectionStatus.DISCONNECTED -> Icons.Default.CloudOff
                            },
                            contentDescription = null,
                            tint = when (connectionStatus) {
                                PrinterConnectionStatus.CONNECTED -> SuccessGreen
                                PrinterConnectionStatus.CONNECTING -> PrimaryIndigo
                                PrinterConnectionStatus.FAILED -> ErrorRed
                                PrinterConnectionStatus.DISCONNECTED -> TextHintColor
                            },
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (connectionStatus) {
                                PrinterConnectionStatus.CONNECTED -> stringResource(
                                    R.string.printer_status_connected_format,
                                    connectedDevice?.name ?: connectedDevice?.address ?: "",
                                    connectedDevice?.type?.toString() ?: ""
                                )
                                PrinterConnectionStatus.CONNECTING -> stringResource(R.string.printer_status_connecting)
                                PrinterConnectionStatus.FAILED -> statusMessage ?: stringResource(R.string.printer_status_failed)
                                PrinterConnectionStatus.DISCONNECTED -> stringResource(R.string.printer_status_disconnected)
                            },
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = when (connectionStatus) {
                                PrinterConnectionStatus.CONNECTED -> SuccessGreen
                                PrinterConnectionStatus.CONNECTING -> PrimaryIndigo
                                PrinterConnectionStatus.FAILED -> ErrorRed
                                PrinterConnectionStatus.DISCONNECTED -> TextHintColor
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Test Print Button
                        Button(
                            onClick = {
                                scope.launch {
                                    isOperating = true
                                    prefs.savePrinterPaperWidth(selectedPaperWidthMm)
                                    val result = if (selectedTabIndex == 0) {
                                        val cleanIp = ipInput.trim()
                                        if (cleanIp.isEmpty()) {
                                            Result.failure(
                                                IllegalStateException(context.getString(R.string.printer_error_no_network_ip))
                                            )
                                        } else {
                                            val port = portInput.toIntOrNull() ?: WifiPrinterManager.DEFAULT_PORT
                                            val resolvedName = networkNameInput.trim().ifBlank {
                                                context.getString(R.string.printer_wifi_default_name)
                                            }
                                            val device = PrinterDevice(
                                                name = resolvedName,
                                                address = cleanIp,
                                                type = PrinterType.WIFI,
                                                port = port
                                            )
                                            prefs.savePrinterNetworkName(resolvedName)
                                            prefs.savePrinterNetworkIp(cleanIp)
                                            prefs.savePrinterNetworkPort(port)
                                            prefs.savePrinterConnectionType(PrinterConfigHelper.CONNECTION_NETWORK)
                                            prefs.savePrinterCutMode(
                                                PrinterConfigHelper.CONNECTION_NETWORK,
                                                PrinterConfigHelper.buildNetworkIdentifier(cleanIp, port),
                                                selectedCutMode
                                            )

                                            val needConnect = !wifiPrinterManager.isConnected() ||
                                                wifiPrinterManager.getConnectedDevice()?.address != cleanIp ||
                                                wifiPrinterManager.getConnectedDevice()?.port != port
                                            val connectRes = if (needConnect) {
                                                connectionStatus = PrinterConnectionStatus.CONNECTING
                                                wifiPrinterManager.connect(device)
                                            } else {
                                                Result.success(Unit)
                                            }

                                            if (connectRes.isFailure) {
                                                connectionStatus = PrinterConnectionStatus.FAILED
                                                statusMessage = connectRes.exceptionOrNull()?.localizedMessage
                                                    ?: context.getString(R.string.printer_connection_timeout)
                                                connectRes
                                            } else {
                                                connectionStatus = PrinterConnectionStatus.CONNECTED
                                                connectedDevice = device
                                                statusMessage = null
                                                wifiPrinterManager.testPrint()
                                            }
                                        }
                                    } else {
                                        prefs.savePrinterConnectionType(PrinterConfigHelper.CONNECTION_BLUETOOTH)
                                        if (selectedBluetoothAddress.isNotBlank()) {
                                            prefs.savePrinterCutMode(
                                                PrinterConfigHelper.CONNECTION_BLUETOOTH,
                                                selectedBluetoothAddress,
                                                selectedCutMode
                                            )
                                        }
                                        val testText = "Lojia Test - تم اختبار الطباعة - টেস্ট"
                                        val bmp = ThermalBitmapRenderer.renderLineToBitmap(
                                            rawLine = "<b>[C]$testText</b>",
                                            widthPx = paperWidthDots(selectedPaperWidthMm),
                                            fontSizePx = 28f
                                        )
                                        val printRes = bluetoothPrinterManager.printBitmap(bmp)
                                        if (printRes.isSuccess) {
                                            connectionStatus = PrinterConnectionStatus.CONNECTED
                                            connectedDevice = bluetoothPrinterManager.getConnectedDevice()
                                            statusMessage = null
                                        } else {
                                            connectionStatus = PrinterConnectionStatus.FAILED
                                            statusMessage = printRes.exceptionOrNull()?.localizedMessage
                                        }
                                        printRes
                                    }

                                    isOperating = false
                                    result.onSuccess {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.printer_test_printed_toast),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }.onFailure { err ->
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.printer_print_failed_toast, err.message ?: ""),
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            },
                            enabled = !isOperating,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("btnTestPrint")
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                stringResource(R.string.printer_test_print_btn),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp
                            )
                        }

                        // Disconnect Button
                        if (connectionStatus == PrinterConnectionStatus.CONNECTED) {
                            OutlinedButton(
                                onClick = {
                                    wifiPrinterManager.disconnect()
                                    bluetoothPrinterManager.disconnect()
                                    connectionStatus = PrinterConnectionStatus.DISCONNECTED
                                    connectedDevice = null
                                    statusMessage = null
                                },
                                modifier = Modifier
                                    .weight(0.6f)
                                    .height(46.dp)
                            ) {
                                Text(stringResource(R.string.btn_disconnect_drive), fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Top Tabs: WIFI | BLUETOOTH
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = PrimaryIndigo
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text(stringResource(R.string.printer_tab_wifi), fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(20.dp)) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = {
                        selectedTabIndex = 1
                        pairedPrinters = bluetoothPrinterManager.getPairedPrinters()
                    },
                    text = { Text(stringResource(R.string.printer_tab_bluetooth), fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(20.dp)) }
                )
            }

            // Tab Content
            when (selectedTabIndex) {
                0 -> {
                    // ==========================================
                    // WIFI / NETWORK PRINTER TAB
                    // ==========================================
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = stringResource(R.string.printer_direct_network_header),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    OutlinedTextField(
                                        value = networkNameInput,
                                        onValueChange = { networkNameInput = it },
                                        label = { Text(stringResource(R.string.printer_name_label)) },
                                        placeholder = { Text(stringResource(R.string.printer_name_placeholder)) },
                                        singleLine = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag(PrinterTestTags.INPUT_PRINTER_NAME)
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = ipInput,
                                            onValueChange = { ipInput = it },
                                            label = { Text(stringResource(R.string.printer_ip_label)) },
                                            placeholder = { Text(stringResource(R.string.printer_network_ip_placeholder)) },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("inputPrinterIp")
                                        )

                                        OutlinedTextField(
                                            value = portInput,
                                            onValueChange = { portInput = it },
                                            label = { Text(stringResource(R.string.printer_port_short_label)) },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier
                                                .width(96.dp)
                                                .testTag("inputPrinterPort")
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Auto Discover Button
                                        OutlinedButton(
                                            onClick = {
                                                if (isDiscovering) {
                                                    discoveryManager.stopDiscovery()
                                                } else {
                                                    discoveryManager.startDiscovery()
                                                }
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag(PrinterTestTags.BTN_AUTO_DISCOVER)
                                        ) {
                                            Icon(
                                                imageVector = if (isDiscovering) Icons.Default.Stop else Icons.Default.Search,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                if (isDiscovering) stringResource(R.string.printer_stop_scan_btn)
                                                else stringResource(R.string.printer_auto_discover_btn)
                                            )
                                        }

                                        // Connect Button
                                        Button(
                                            onClick = {
                                                scope.launch {
                                                    isOperating = true
                                                    val port = portInput.toIntOrNull() ?: WifiPrinterManager.DEFAULT_PORT
                                                    val resolvedName = networkNameInput.trim().ifBlank {
                                                        context.getString(R.string.printer_wifi_default_name)
                                                    }
                                                    val dev = PrinterDevice(
                                                        name = resolvedName,
                                                        address = ipInput.trim(),
                                                        type = PrinterType.WIFI,
                                                        port = port
                                                    )
                                                    prefs.savePrinterNetworkName(dev.name)
                                                    prefs.savePrinterNetworkIp(dev.address)
                                                    prefs.savePrinterNetworkPort(dev.port)
                                                    prefs.savePrinterPaperWidth(selectedPaperWidthMm)
                                                    prefs.savePrinterConnectionType(PrinterConfigHelper.CONNECTION_NETWORK)
                                                    prefs.savePrinterCutMode(
                                                        PrinterConfigHelper.CONNECTION_NETWORK,
                                                        PrinterConfigHelper.buildNetworkIdentifier(dev.address, dev.port),
                                                        selectedCutMode
                                                    )

                                                    val res = wifiPrinterManager.connect(dev)
                                                    isOperating = false
                                                    res.onSuccess {
                                                        connectionStatus = PrinterConnectionStatus.CONNECTED
                                                        connectedDevice = dev
                                                        statusMessage = null
                                                        Toast.makeText(
                                                            context,
                                                            context.getString(
                                                                R.string.printer_connected_addr_toast,
                                                                "${dev.address}:${dev.port}"
                                                            ),
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }.onFailure { err ->
                                                        connectionStatus = PrinterConnectionStatus.FAILED
                                                        statusMessage = err.localizedMessage
                                                            ?: context.getString(R.string.printer_connection_timeout)
                                                        Toast.makeText(
                                                            context,
                                                            context.getString(R.string.printer_failed_msg_toast, err.message ?: ""),
                                                            Toast.LENGTH_LONG
                                                        ).show()
                                                    }
                                                }
                                            },
                                            enabled = !isOperating && ipInput.isNotBlank(),
                                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag(PrinterTestTags.BTN_CONNECT_NETWORK)
                                        ) {
                                            if (isOperating) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(18.dp),
                                                    color = Color.White,
                                                    strokeWidth = 2.dp
                                                )
                                            } else {
                                                Text(stringResource(R.string.printer_connect_btn))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Paper Width & Paper Cut Mode Options
                        item {
                            PrinterOptionsCard(
                                selectedPaperWidthMm = selectedPaperWidthMm,
                                onPaperWidthChange = { widthMm ->
                                    selectedPaperWidthMm = widthMm
                                    prefs.savePrinterPaperWidth(widthMm)
                                },
                                selectedCutMode = selectedCutMode,
                                onCutModeChange = { mode ->
                                    selectedCutMode = mode
                                    prefs.savePrinterCutMode(activeConnectionType, activePrinterIdentifier, mode)
                                }
                            )
                        }

                        // Scanning Indicator
                        if (isDiscovering) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = stringResource(R.string.printer_scanning_network),
                                        fontSize = 12.sp,
                                        color = TextHintColor
                                    )
                                }
                            }
                        }

                        // Discovered Printers List
                        item {
                            Text(
                                text = stringResource(
                                    R.string.printer_discovered_network_format,
                                    discoveredPrinters.size
                                ),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }

                        if (discoveredPrinters.isEmpty() && !isDiscovering) {
                            item {
                                Text(
                                    text = stringResource(R.string.printer_no_network_found),
                                    fontSize = 12.5.sp,
                                    color = TextHintColor,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }

                        items(discoveredPrinters) { device ->
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, OutlineLight),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        networkNameInput = device.name
                                        ipInput = device.address
                                        portInput = device.port.toString()
                                        prefs.savePrinterNetworkName(device.name)
                                        prefs.savePrinterNetworkIp(device.address)
                                        prefs.savePrinterNetworkPort(device.port)
                                        prefs.savePrinterConnectionType(PrinterConfigHelper.CONNECTION_NETWORK)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(PrimaryIndigoLight),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Print,
                                            contentDescription = null,
                                            tint = PrimaryIndigo,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(device.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        Text("${device.address}:${device.port}", fontSize = 12.sp, color = TextHintColor)
                                    }
                                    FilledTonalButton(
                                        onClick = {
                                            networkNameInput = device.name
                                            ipInput = device.address
                                            portInput = device.port.toString()
                                            prefs.savePrinterNetworkName(device.name)
                                            prefs.savePrinterNetworkIp(device.address)
                                            prefs.savePrinterNetworkPort(device.port)
                                            prefs.savePrinterConnectionType(PrinterConfigHelper.CONNECTION_NETWORK)
                                            scope.launch {
                                                isOperating = true
                                                val res = wifiPrinterManager.connect(device)
                                                isOperating = false
                                                res.onSuccess {
                                                    connectionStatus = PrinterConnectionStatus.CONNECTED
                                                    connectedDevice = device
                                                    statusMessage = null
                                                    Toast.makeText(
                                                        context,
                                                        context.getString(
                                                            R.string.printer_connected_addr_toast,
                                                            "${device.address}:${device.port}"
                                                        ),
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }.onFailure { err ->
                                                    connectionStatus = PrinterConnectionStatus.FAILED
                                                    statusMessage = err.message
                                                }
                                            }
                                        },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Text(stringResource(R.string.printer_connect_btn), fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // ==========================================
                    // BLUETOOTH PRINTER TAB
                    // ==========================================
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            PrinterOptionsCard(
                                selectedPaperWidthMm = selectedPaperWidthMm,
                                onPaperWidthChange = { widthMm ->
                                    selectedPaperWidthMm = widthMm
                                    prefs.savePrinterPaperWidth(widthMm)
                                },
                                selectedCutMode = selectedCutMode,
                                onCutModeChange = { mode ->
                                    selectedCutMode = mode
                                    prefs.savePrinterCutMode(activeConnectionType, activePrinterIdentifier, mode)
                                }
                            )
                        }

                        item {
                            Text(
                                text = stringResource(R.string.printer_paired_bt_format, pairedPrinters.size),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }

                        if (pairedPrinters.isEmpty()) {
                            item {
                                Text(
                                    text = stringResource(R.string.printer_no_paired_bt_found),
                                    fontSize = 12.5.sp,
                                    color = TextHintColor,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                            }
                        }

                        items(pairedPrinters) { device ->
                            val isSelected = device.address == selectedBluetoothAddress
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) PrimaryIndigo else OutlineLight
                                ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) {
                                        PrimaryIndigoLight.copy(alpha = 0.5f)
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedBluetoothAddress = device.address
                                        prefs.savePrinterAddress(device.address)
                                        prefs.saveSelectedPrinterName(device.name)
                                        prefs.savePrinterConnectionType(PrinterConfigHelper.CONNECTION_BLUETOOTH)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bluetooth,
                                        contentDescription = null,
                                        tint = if (isSelected) PrimaryIndigo else TextHintColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(device.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        Text(device.address, fontSize = 12.sp, color = TextHintColor)
                                    }
                                    if (isSelected) {
                                        FilledTonalButton(
                                            onClick = {
                                                scope.launch {
                                                    isOperating = true
                                                    prefs.savePrinterPaperWidth(selectedPaperWidthMm)
                                                    prefs.savePrinterCutMode(
                                                        PrinterConfigHelper.CONNECTION_BLUETOOTH,
                                                        device.address,
                                                        selectedCutMode
                                                    )
                                                    val btDevice = PrinterDevice(
                                                        device.name,
                                                        device.address,
                                                        PrinterType.BLUETOOTH
                                                    )
                                                    val res = bluetoothPrinterManager.connect(btDevice)
                                                    isOperating = false
                                                    res.onSuccess {
                                                        connectionStatus = PrinterConnectionStatus.CONNECTED
                                                        connectedDevice = btDevice
                                                        statusMessage = null
                                                        Toast.makeText(
                                                            context,
                                                            context.getString(R.string.printer_connected_addr_toast, device.name),
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }.onFailure { err ->
                                                        connectionStatus = PrinterConnectionStatus.FAILED
                                                        statusMessage = err.message
                                                        Toast.makeText(
                                                            context,
                                                            context.getString(
                                                                R.string.printer_connection_failed_toast,
                                                                err.message ?: ""
                                                            ),
                                                            Toast.LENGTH_LONG
                                                        ).show()
                                                    }
                                                }
                                            },
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                        ) {
                                            Text(stringResource(R.string.printer_connect_btn), fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrinterOptionsCard(
    selectedPaperWidthMm: Int,
    onPaperWidthChange: (Int) -> Unit,
    selectedCutMode: PrinterCutMode,
    onCutModeChange: (PrinterCutMode) -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.printer_paper_width_label),
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedPaperWidthMm == 58,
                    onClick = { onPaperWidthChange(58) },
                    label = { Text(stringResource(R.string.printer_paper_width_58mm)) },
                    modifier = Modifier.testTag(PrinterTestTags.CHIP_PAPER_WIDTH_58)
                )
                FilterChip(
                    selected = selectedPaperWidthMm == 80,
                    onClick = { onPaperWidthChange(80) },
                    label = { Text(stringResource(R.string.printer_paper_width_80mm)) },
                    modifier = Modifier.testTag(PrinterTestTags.CHIP_PAPER_WIDTH_80)
                )
            }

            Text(
                text = stringResource(R.string.printer_cut_mode_label),
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedCutMode == PrinterCutMode.NONE,
                    onClick = { onCutModeChange(PrinterCutMode.NONE) },
                    label = { Text(stringResource(R.string.printer_cut_mode_none)) },
                    modifier = Modifier.testTag(PrinterTestTags.CHIP_CUT_MODE_NONE)
                )
                FilterChip(
                    selected = selectedCutMode == PrinterCutMode.PARTIAL,
                    onClick = { onCutModeChange(PrinterCutMode.PARTIAL) },
                    label = { Text(stringResource(R.string.printer_cut_mode_partial)) },
                    modifier = Modifier.testTag(PrinterTestTags.CHIP_CUT_MODE_PARTIAL)
                )
                FilterChip(
                    selected = selectedCutMode == PrinterCutMode.FULL,
                    onClick = { onCutModeChange(PrinterCutMode.FULL) },
                    label = { Text(stringResource(R.string.printer_cut_mode_full)) },
                    modifier = Modifier.testTag(PrinterTestTags.CHIP_CUT_MODE_FULL)
                )
            }
        }
    }
}
