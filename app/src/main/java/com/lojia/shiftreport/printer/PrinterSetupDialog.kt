package com.lojia.shiftreport.printer

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.lojia.shiftreport.data.PreferencesRepository
import com.lojia.shiftreport.permission.AppFeaturePermission
import com.lojia.shiftreport.permission.rememberPermissionRequester
import com.lojia.shiftreport.ui.common.LojiaTextField
import com.lojia.shiftreport.ui.theme.AccentEmerald
import com.lojia.shiftreport.ui.theme.PrimaryBlue
import com.lojia.shiftreport.ui.theme.PureWhite
import kotlinx.coroutines.launch

/**
 * Dialog for setting up and testing ESC/POS thermal receipt printers (Bluetooth & Network TCP).
 * Gracefully handles runtime dangerous permissions for Bluetooth scanning & connecting.
 */
@Composable
fun PrinterSetupDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val printerManager = remember(context) { BluetoothPrinterManager(context) }
    val prefs = remember(context) { PreferencesRepository.getInstance(context) }

    var connectionType by remember { mutableStateOf(printerManager.getPrinterConnectionType()) }
    var selectedAddress by remember { mutableStateOf(printerManager.getSavedPrinterAddress()) }
    var selectedPaperWidth by remember { mutableStateOf(printerManager.getSavedPaperWidthMm()) }

    var networkIp by remember { mutableStateOf(prefs.getPrinterNetworkIp()) }
    var networkPort by remember { mutableStateOf(prefs.getPrinterNetworkPort().toString()) }

    var pairedPrinters by remember { mutableStateOf<List<BluetoothPrinterDevice>>(emptyList()) }
    var isScanning by remember { mutableStateOf(false) }
    var isTestingPrint by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isErrorMessage by remember { mutableStateOf(false) }

    // Reusable Bluetooth Permission Requester
    val bluetoothPermissionRequester = rememberPermissionRequester(
        feature = AppFeaturePermission.BLUETOOTH_PRINTER,
        onGranted = {
            isScanning = true
            scope.launch {
                val list = printerManager.getPairedPrinters()
                pairedPrinters = list
                isScanning = false
                if (list.isEmpty()) {
                    statusMessage = "No paired Bluetooth printers found. Pair in Android Settings first."
                    isErrorMessage = false
                }
            }
        },
        onDenied = {
            statusMessage = "Bluetooth permission is required to discover thermal printers."
            isErrorMessage = true
        }
    )

    fun loadBluetoothPrinters() {
        bluetoothPermissionRequester.launch {
            isScanning = true
            scope.launch {
                val list = printerManager.getPairedPrinters()
                pairedPrinters = list
                isScanning = false
                if (list.isEmpty()) {
                    statusMessage = "No paired Bluetooth printers found. Pair in Android Settings first."
                    isErrorMessage = false
                }
            }
        }
    }

    LaunchedEffect(connectionType) {
        if (connectionType == "bluetooth") {
            loadBluetoothPrinters()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFECFDF5)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Print,
                                contentDescription = null,
                                tint = AccentEmerald,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Thermal Printer Setup",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "ESC/POS Receipt & Z-Report",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF94A3B8)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Connection Mode Selector Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF1F5F9))
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (connectionType == "bluetooth") Color.White else Color.Transparent)
                            .clickable {
                                connectionType = "bluetooth"
                                statusMessage = null
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Bluetooth",
                            fontSize = 13.sp,
                            fontWeight = if (connectionType == "bluetooth") FontWeight.Bold else FontWeight.Medium,
                            color = if (connectionType == "bluetooth") AccentEmerald else Color(0xFF64748B)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (connectionType == "network") Color.White else Color.Transparent)
                            .clickable {
                                connectionType = "network"
                                statusMessage = null
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Wi-Fi / LAN (TCP)",
                            fontSize = 13.sp,
                            fontWeight = if (connectionType == "network") FontWeight.Bold else FontWeight.Medium,
                            color = if (connectionType == "network") AccentEmerald else Color(0xFF64748B)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Content for Bluetooth
                if (connectionType == "bluetooth") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Paired Bluetooth Printers",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF334155)
                        )

                        TextButton(
                            onClick = { loadBluetoothPrinters() },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            if (isScanning) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = if (isScanning) "Searching..." else "Refresh",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentEmerald
                            )
                        }
                    }

                    if (pairedPrinters.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF8FAFC))
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isScanning) "Searching for paired devices..." else "No paired Bluetooth printers.\nPair your thermal printer in Android Bluetooth Settings first.",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 140.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(pairedPrinters) { device ->
                                val isSelected = device.address == selectedAddress
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            selectedAddress = device.address
                                            statusMessage = null
                                        },
                                    color = if (isSelected) Color(0xFFECFDF5) else Color(0xFFF8FAFC),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) AccentEmerald else Color(0xFFE2E8F0)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                text = device.name,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0F172A)
                                            )
                                            Text(
                                                text = device.address,
                                                fontSize = 11.sp,
                                                color = Color(0xFF64748B)
                                            )
                                        }

                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { selectedAddress = device.address },
                                            colors = RadioButtonDefaults.colors(selectedColor = AccentEmerald)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Content for Network TCP
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        LojiaTextField(
                            value = networkIp,
                            onValueChange = { networkIp = it },
                            label = { Text("Printer IP Address") },
                            placeholder = { Text("e.g. 192.168.1.100") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        LojiaTextField(
                            value = networkPort,
                            onValueChange = { networkPort = it },
                            label = { Text("Port (Default: 9100)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Paper Width Selector
                Text(
                    text = "Paper Roll Width",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF334155)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = selectedPaperWidth == 58,
                        onClick = { selectedPaperWidth = 58 },
                        label = { Text("58 mm (Standard Receipt)") },
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = selectedPaperWidth == 80,
                        onClick = { selectedPaperWidth = 80 },
                        label = { Text("80 mm (Wide Receipt)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Status Message Feedback
                if (statusMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = statusMessage!!,
                        fontSize = 12.sp,
                        color = if (isErrorMessage) Color(0xFFDC2626) else AccentEmerald,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Action Buttons: Test Print & Save
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            isTestingPrint = true
                            statusMessage = null
                            isErrorMessage = false

                            val executeTest = {
                                scope.launch {
                                    val testReceipt = """
                                        [C]<b>LOJIA POS</b>
                                        [C]Shift Report System
                                        [C]================================
                                        [L]<b>TEST PRINT SUCCESSFUL</b>
                                        [L]Hardware: ${if (connectionType == "bluetooth") selectedAddress else "$networkIp:$networkPort"}
                                        [L]Width: ${selectedPaperWidth}mm
                                        [C]--------------------------------
                                        [C]Printer connection operational!
                                        [C]================================
                                    """.trimIndent()

                                    val result = if (connectionType == "network") {
                                        printerManager.testNetworkConnection(
                                            networkIp,
                                            networkPort.toIntOrNull() ?: 9100
                                        )
                                    } else {
                                        printerManager.testConnection(selectedAddress)
                                    }

                                    isTestingPrint = false
                                    result.fold(
                                        onSuccess = {
                                            statusMessage = "Test connection successful!"
                                            isErrorMessage = false
                                        },
                                        onFailure = { e ->
                                            statusMessage = "Test failed: ${e.message}"
                                            isErrorMessage = true
                                        }
                                    )
                                }
                            }

                            if (connectionType == "bluetooth") {
                                bluetoothPermissionRequester.launch {
                                    executeTest()
                                }
                            } else {
                                executeTest()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isTestingPrint && (connectionType == "network" || selectedAddress.isNotBlank()),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isTestingPrint) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test", fontSize = 13.sp)
                        }
                    }

                    Button(
                        onClick = {
                            if (connectionType == "bluetooth") {
                                printerManager.savePrinterConfig(selectedAddress, selectedPaperWidth)
                            } else {
                                printerManager.saveNetworkPrinterConfig(
                                    networkIp,
                                    networkPort.toIntOrNull() ?: 9100,
                                    selectedPaperWidth
                                )
                            }
                            Toast.makeText(context, "Printer configuration saved!", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald),
                        shape = RoundedCornerShape(12.dp),
                        enabled = connectionType == "bluetooth" && selectedAddress.isNotBlank() || connectionType == "network" && networkIp.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
