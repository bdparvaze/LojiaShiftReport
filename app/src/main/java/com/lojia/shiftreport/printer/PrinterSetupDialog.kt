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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.lojia.shiftreport.R
import com.lojia.shiftreport.ui.theme.Dimens
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
    var isTestingDrawer by remember { mutableStateOf(false) }
    var showAdminAuthDialog by remember { mutableStateOf(false) }
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
                    statusMessage = context.getString(R.string.printer_no_paired_found)
                    isErrorMessage = false
                }
            }
        },
        onDenied = {
            statusMessage = context.getString(R.string.printer_permission_required)
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
                    statusMessage = context.getString(R.string.printer_no_paired_found)
                    isErrorMessage = false
                }
            }
        }
    }

    fun isValidIp(ip: String): Boolean {
        val parts = ip.trim().split(".")
        if (parts.size != 4) return false
        return parts.all { part ->
            val num = part.toIntOrNull()
            num != null && num in 0..255
        }
    }

    fun isValidPort(portStr: String): Boolean {
        val p = portStr.trim().toIntOrNull()
        return p != null && p in 1..65535
    }

    LaunchedEffect(connectionType) {
        if (connectionType == "bluetooth") {
            loadBluetoothPrinters()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = modifier
                .widthIn(max = Dimens.DialogMaxWidth)
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(horizontal = Dimens.SpacingLg),
            shape = RoundedCornerShape(Dimens.DialogCornerRadius),
            color = Color.White,
            tonalElevation = Dimens.DialogElevation
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
                                text = stringResource(R.string.printer_setup_title),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = stringResource(R.string.printer_setup_subtitle),
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.close),
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
                            text = stringResource(R.string.printer_conn_bluetooth),
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
                            text = stringResource(R.string.printer_conn_network),
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
                            text = stringResource(R.string.printer_paired_devices),
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
                                text = if (isScanning) stringResource(R.string.printer_searching) else stringResource(R.string.printer_refresh),
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
                                text = if (isScanning) stringResource(R.string.printer_searching) else stringResource(R.string.printer_no_devices_hint),
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
                            onValueChange = {
                                networkIp = it
                                statusMessage = null
                            },
                            label = { Text(stringResource(R.string.printer_ip_label)) },
                            placeholder = { Text("e.g. 192.168.1.100") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        LojiaTextField(
                            value = networkPort,
                            onValueChange = {
                                networkPort = it
                                statusMessage = null
                            },
                            label = { Text(stringResource(R.string.printer_port_label)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Paper Width Selector
                Text(
                    text = stringResource(R.string.printer_paper_width_label),
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
                        label = { Text(stringResource(R.string.printer_width_58mm)) },
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = selectedPaperWidth == 80,
                        onClick = { selectedPaperWidth = 80 },
                        label = { Text(stringResource(R.string.printer_width_80mm)) },
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

                // Action Buttons: Test Print, Test Cash Drawer & Save
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (connectionType == "network") {
                                    if (!isValidIp(networkIp)) {
                                        statusMessage = context.getString(R.string.printer_invalid_ip)
                                        isErrorMessage = true
                                        return@OutlinedButton
                                    }
                                    if (!isValidPort(networkPort)) {
                                        statusMessage = context.getString(R.string.printer_invalid_port)
                                        isErrorMessage = true
                                        return@OutlinedButton
                                    }
                                }

                                isTestingPrint = true
                                statusMessage = null
                                isErrorMessage = false

                                val executeTest = {
                                    scope.launch {
                                        val result = if (connectionType == "network") {
                                            printerManager.testNetworkConnection(
                                                networkIp.trim(),
                                                networkPort.trim().toIntOrNull() ?: 9100
                                            )
                                        } else {
                                            printerManager.testConnection(selectedAddress)
                                        }

                                        isTestingPrint = false
                                        result.fold(
                                            onSuccess = {
                                                statusMessage = context.getString(R.string.printer_test_success)
                                                isErrorMessage = false
                                            },
                                            onFailure = { e ->
                                                statusMessage = context.getString(R.string.printer_test_failed, e.message ?: "")
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
                            enabled = !isTestingPrint && !isTestingDrawer && (connectionType == "network" || selectedAddress.isNotBlank()),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isTestingPrint) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.printer_btn_test), fontSize = 13.sp)
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                if (connectionType == "network") {
                                    if (!isValidIp(networkIp)) {
                                        statusMessage = context.getString(R.string.printer_invalid_ip)
                                        isErrorMessage = true
                                        return@OutlinedButton
                                    }
                                    if (!isValidPort(networkPort)) {
                                        statusMessage = context.getString(R.string.printer_invalid_port)
                                        isErrorMessage = true
                                        return@OutlinedButton
                                    }
                                }
                                showAdminAuthDialog = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isTestingPrint && !isTestingDrawer && (connectionType == "network" || selectedAddress.isNotBlank()),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isTestingDrawer) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.printer_btn_test_drawer), fontSize = 12.sp)
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (connectionType == "bluetooth") {
                                printerManager.savePrinterConfig(selectedAddress, selectedPaperWidth)
                            } else {
                                if (!isValidIp(networkIp)) {
                                    statusMessage = context.getString(R.string.printer_invalid_ip)
                                    isErrorMessage = true
                                    return@Button
                                }
                                if (!isValidPort(networkPort)) {
                                    statusMessage = context.getString(R.string.printer_invalid_port)
                                    isErrorMessage = true
                                    return@Button
                                }
                                printerManager.saveNetworkPrinterConfig(
                                    networkIp.trim(),
                                    networkPort.trim().toIntOrNull() ?: 9100,
                                    selectedPaperWidth
                                )
                            }
                            Toast.makeText(context, context.getString(R.string.printer_config_saved), Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald),
                        shape = RoundedCornerShape(12.dp),
                        enabled = connectionType == "bluetooth" && selectedAddress.isNotBlank() || connectionType == "network" && networkIp.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.save), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        if (showAdminAuthDialog) {
            com.lojia.shiftreport.auth.AdminAuthDialog(
                actionTitle = stringResource(R.string.printer_btn_test_drawer),
                actionDescription = stringResource(R.string.printer_test_drawer_admin_desc),
                onDismissRequest = { showAdminAuthDialog = false },
                onAuthSuccess = {
                    showAdminAuthDialog = false
                    isTestingDrawer = true
                    statusMessage = null
                    isErrorMessage = false

                    val executeDrawerKick = {
                        scope.launch {
                            val result = printerManager.openCashDrawer()
                            isTestingDrawer = false
                            result.fold(
                                onSuccess = {
                                    statusMessage = context.getString(R.string.printer_drawer_kick_success)
                                    isErrorMessage = false
                                },
                                onFailure = { e ->
                                    statusMessage = context.getString(R.string.printer_test_failed, e.message ?: "")
                                    isErrorMessage = true
                                }
                            )
                        }
                    }

                    if (connectionType == "bluetooth") {
                        bluetoothPermissionRequester.launch {
                            executeDrawerKick()
                        }
                    } else {
                        executeDrawerKick()
                    }
                }
            )
        }
    }
}
