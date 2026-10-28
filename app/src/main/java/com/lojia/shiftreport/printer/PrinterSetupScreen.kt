package com.lojia.shiftreport.printer

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.outlined.*
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
import kotlinx.coroutines.launch

/**
 * Universal POS Thermal Receipt Printer Setup Screen.
 * Provides dual-tab configuration:
 * 1. WiFi / Network Socket (Port 9100, Foodics POS standard) with mDNS / NSD Auto-Discovery
 * 2. Bluetooth RFCOMM SPP Printer
 * Includes live connection testing and multilingual receipt sample printing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrinterSetupScreen(
    onClose: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember(context) { PreferencesRepository.getInstance(context) }

    val wifiPrinterManager = remember(context) { WifiPrinterManager(context) }
    val bluetoothPrinterManager = remember(context) { BluetoothPrinterManager(context) }
    val discoveryManager = remember(context) { PrinterDiscoveryManager(context) }

    var selectedTabIndex by remember {
        mutableIntStateOf(if (prefs.getPrinterConnectionType() == "network") 0 else 1)
    }

    // WiFi Tab States
    var ipInput by remember {
        mutableStateOf(prefs.getPrinterNetworkIp().ifBlank { "192.168.1.100" })
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

    // Connection & Print States
    var connectionStatus by remember { mutableStateOf(PrinterConnectionStatus.DISCONNECTED) }
    var connectedDevice by remember { mutableStateOf<PrinterDevice?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isOperating by remember { mutableStateOf(false) }

    // Clean up discovery on dispose
    DisposableEffect(Unit) {
        onDispose {
            discoveryManager.stopDiscovery()
            wifiPrinterManager.disconnect()
            bluetoothPrinterManager.disconnect()
        }
    }

    Scaffold(
        topBar = {
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
                                PrinterConnectionStatus.CONNECTED -> "Connected: ${connectedDevice?.name ?: connectedDevice?.address} (${connectedDevice?.type})"
                                PrinterConnectionStatus.CONNECTING -> "Connecting to printer..."
                                PrinterConnectionStatus.FAILED -> statusMessage ?: "Connection Failed"
                                PrinterConnectionStatus.DISCONNECTED -> "Printer Disconnected"
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
                                    val result = if (selectedTabIndex == 0) {
                                        // WiFi Print
                                        val port = portInput.toIntOrNull() ?: 9100
                                        val device = PrinterDevice("WiFi Printer", ipInput.trim(), PrinterType.WIFI, port)
                                        if (!wifiPrinterManager.isConnected()) {
                                            wifiPrinterManager.connect(device)
                                        }
                                        wifiPrinterManager.testPrint()
                                    } else {
                                        // Bluetooth Print
                                        val testText = "Lojia Test - تم اختبار الطباعة - টেস্ট"
                                        val bmp = ThermalBitmapRenderer.renderLineToBitmap(
                                            rawLine = "<b>[C]$testText</b>",
                                            widthPx = 384,
                                            fontSizePx = 28f
                                        )
                                        bluetoothPrinterManager.printBitmap(bmp)
                                    }

                                    isOperating = false
                                    result.onSuccess {
                                        Toast.makeText(context, "Test receipt printed successfully!", Toast.LENGTH_SHORT).show()
                                    }.onFailure { err ->
                                        Toast.makeText(context, "Print failed: ${err.message}", Toast.LENGTH_LONG).show()
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
                            Text("Test Print", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
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
                                Text("Disconnect", fontSize = 13.sp)
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
                    text = { Text("WIFI (Network)", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(20.dp)) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = {
                        selectedTabIndex = 1
                        pairedPrinters = bluetoothPrinterManager.getPairedPrinters()
                    },
                    text = { Text("BLUETOOTH", fontWeight = FontWeight.SemiBold) },
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
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "Direct Network Configuration (TCP Socket)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = ipInput,
                                            onValueChange = { ipInput = it },
                                            label = { Text("Printer IP Address") },
                                            placeholder = { Text("e.g. 192.168.1.100") },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("inputPrinterIp")
                                        )

                                        OutlinedTextField(
                                            value = portInput,
                                            onValueChange = { portInput = it },
                                            label = { Text("Port") },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier
                                                .width(90.dp)
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
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = if (isDiscovering) Icons.Default.Stop else Icons.Default.Search,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(if (isDiscovering) "Stop Scan" else "Auto Discover")
                                        }

                                        // Connect Button
                                        Button(
                                            onClick = {
                                                scope.launch {
                                                    isOperating = true
                                                    val port = portInput.toIntOrNull() ?: 9100
                                                    val dev = PrinterDevice("WiFi Printer", ipInput.trim(), PrinterType.WIFI, port)
                                                    prefs.savePrinterNetworkIp(dev.address)
                                                    prefs.savePrinterNetworkPort(dev.port)
                                                    prefs.savePrinterConnectionType("network")

                                                    val res = wifiPrinterManager.connect(dev)
                                                    isOperating = false
                                                    res.onSuccess {
                                                        connectionStatus = PrinterConnectionStatus.CONNECTED
                                                        connectedDevice = dev
                                                        statusMessage = null
                                                        Toast.makeText(context, "Connected to ${dev.address}:${dev.port}", Toast.LENGTH_SHORT).show()
                                                    }.onFailure { err ->
                                                        connectionStatus = PrinterConnectionStatus.FAILED
                                                        statusMessage = err.localizedMessage ?: "Connection Timeout"
                                                        Toast.makeText(context, "Failed: ${err.message}", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            },
                                            enabled = !isOperating && ipInput.isNotBlank(),
                                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            if (isOperating) {
                                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                            } else {
                                                Text("Connect")
                                            }
                                        }
                                    }
                                }
                            }
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
                                        text = "Scanning local network (_printer._tcp, _pdl-datastream._tcp)...",
                                        fontSize = 12.sp,
                                        color = TextHintColor
                                    )
                                }
                            }
                        }

                        // Discovered Printers List
                        item {
                            Text(
                                text = "Discovered Network Printers (${discoveredPrinters.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }

                        if (discoveredPrinters.isEmpty() && !isDiscovering) {
                            item {
                                Text(
                                    text = "No network printers found. Enter printer IP directly or tap 'Auto Discover'. Ensure your device and printer are on the same WiFi.",
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
                                        ipInput = device.address
                                        portInput = device.port.toString()
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
                                        Icon(Icons.Default.Print, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(device.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        Text("${device.address}:${device.port}", fontSize = 12.sp, color = TextHintColor)
                                    }
                                    FilledTonalButton(
                                        onClick = {
                                            ipInput = device.address
                                            portInput = device.port.toString()
                                            scope.launch {
                                                isOperating = true
                                                val res = wifiPrinterManager.connect(device)
                                                isOperating = false
                                                res.onSuccess {
                                                    connectionStatus = PrinterConnectionStatus.CONNECTED
                                                    connectedDevice = device
                                                    statusMessage = null
                                                }.onFailure { err ->
                                                    connectionStatus = PrinterConnectionStatus.FAILED
                                                    statusMessage = err.message
                                                }
                                            }
                                        },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Text("Connect", fontSize = 12.sp)
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
                            Text(
                                text = "Paired Bluetooth Printers (${pairedPrinters.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }

                        if (pairedPrinters.isEmpty()) {
                            item {
                                Text(
                                    text = "No paired Bluetooth printers detected. Please turn on Bluetooth and pair your ESC/POS thermal printer in Android Settings first.",
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
                                    containerColor = if (isSelected) PrimaryIndigoLight.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedBluetoothAddress = device.address
                                        prefs.savePrinterAddress(device.address)
                                        prefs.savePrinterConnectionType("bluetooth")
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
                                                    val btDevice = PrinterDevice(device.name, device.address, PrinterType.BLUETOOTH)
                                                    val res = bluetoothPrinterManager.connect(btDevice)
                                                    isOperating = false
                                                    res.onSuccess {
                                                        connectionStatus = PrinterConnectionStatus.CONNECTED
                                                        connectedDevice = btDevice
                                                        statusMessage = null
                                                        Toast.makeText(context, "Connected to ${device.name}", Toast.LENGTH_SHORT).show()
                                                    }.onFailure { err ->
                                                        connectionStatus = PrinterConnectionStatus.FAILED
                                                        statusMessage = err.message
                                                        Toast.makeText(context, "Connection failed: ${err.message}", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            },
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                        ) {
                                            Text("Connect", fontSize = 12.sp)
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
