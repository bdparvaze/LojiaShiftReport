package com.lojia.shiftreport.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.lojia.shiftreport.ui.theme.AccentEmerald
import com.lojia.shiftreport.ui.theme.PrimaryBlue

/**
 * Supported features that require runtime dangerous permissions.
 */
enum class AppFeaturePermission(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val permissionsGetter: () -> List<String>
) {
    CAMERA(
        title = "Camera Permission Required",
        description = "To scan physical invoices, shift reports, and documents with auto-edge detection and OCR text extraction, Lojia needs access to your camera.",
        icon = Icons.Outlined.CameraAlt,
        permissionsGetter = { listOf(Manifest.permission.CAMERA) }
    ),
    BLUETOOTH_PRINTER(
        title = "Bluetooth & Nearby Devices",
        description = "To discover, connect, and print receipts / Z-Reports directly to your Bluetooth ESC/POS thermal printer, Lojia needs Bluetooth permissions.",
        icon = Icons.Outlined.Print,
        permissionsGetter = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                listOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            } else {
                listOf(
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            }
        }
    ),
    NOTIFICATIONS(
        title = "Notifications & Reminders",
        description = "Stay informed when daily shift reports are generated, automated backups finish, or stock runs low.",
        icon = Icons.Outlined.Notifications,
        permissionsGetter = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                listOf(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                emptyList()
            }
        }
    );

    val permissions: List<String>
        get() = permissionsGetter()
}

/**
 * General helper utilities for Android permission checking and settings redirection.
 */
object PermissionUtils {

    fun hasPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasAllPermissions(context: Context, permissions: List<String>): Boolean {
        return permissions.all { hasPermission(context, it) }
    }

    fun findActivity(context: Context): Activity? {
        var currentContext = context
        while (currentContext is ContextWrapper) {
            if (currentContext is Activity) {
                return currentContext
            }
            currentContext = currentContext.baseContext
        }
        return null
    }

    fun shouldShowRationale(activity: Activity?, permissions: List<String>): Boolean {
        if (activity == null) return false
        return permissions.any { ActivityCompat.shouldShowRequestPermissionRationale(activity, it) }
    }

    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

/**
 * Handle returned by `rememberPermissionRequester` to trigger permission flows on demand.
 */
class PermissionRequester(
    private val checkAndRequest: (onGranted: (() -> Unit)?) -> Unit
) {
    /**
     * Executes permission check: if granted, executes [onGranted] immediately.
     * Otherwise, presents native prompt or rationale dialog as appropriate.
     */
    fun launch(onGranted: (() -> Unit)? = null) {
        checkAndRequest(onGranted)
    }
}

/**
 * Reusable Jetpack Compose hook that manages permission requesting,
 * rationale dialogs, and app settings fallback.
 */
@Composable
fun rememberPermissionRequester(
    feature: AppFeaturePermission,
    onGranted: () -> Unit = {},
    onDenied: () -> Unit = {}
): PermissionRequester {
    val context = LocalContext.current
    val activity = remember(context) { PermissionUtils.findActivity(context) }

    var showRationaleDialog by remember { mutableStateOf(false) }
    var isPermanentlyDenied by remember { mutableStateOf(false) }
    var pendingOnGrantedAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val permissions = remember(feature) { feature.permissions }

    val multiplePermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { resultsMap ->
        val allGranted = resultsMap.values.all { it }
        if (allGranted) {
            showRationaleDialog = false
            val action = pendingOnGrantedAction ?: onGranted
            pendingOnGrantedAction = null
            action.invoke()
        } else {
            // Check if permanently denied
            val shouldShowRationale = PermissionUtils.shouldShowRationale(activity, permissions)
            isPermanentlyDenied = !shouldShowRationale
            showRationaleDialog = true
            onDenied.invoke()
        }
    }

    val singlePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showRationaleDialog = false
            val action = pendingOnGrantedAction ?: onGranted
            pendingOnGrantedAction = null
            action.invoke()
        } else {
            val shouldShowRationale = PermissionUtils.shouldShowRationale(activity, permissions)
            isPermanentlyDenied = !shouldShowRationale
            showRationaleDialog = true
            onDenied.invoke()
        }
    }

    val requestPermission = {
        if (permissions.size == 1) {
            singlePermissionLauncher.launch(permissions.first())
        } else if (permissions.isNotEmpty()) {
            multiplePermissionsLauncher.launch(permissions.toTypedArray())
        }
    }

    val checkAndRequest: (onGranted: (() -> Unit)?) -> Unit = { callback ->
        pendingOnGrantedAction = callback
        if (permissions.isEmpty() || PermissionUtils.hasAllPermissions(context, permissions)) {
            val action = callback ?: onGranted
            pendingOnGrantedAction = null
            action.invoke()
        } else {
            if (PermissionUtils.shouldShowRationale(activity, permissions)) {
                // Show rationale dialog before requesting
                isPermanentlyDenied = false
                showRationaleDialog = true
            } else {
                // Direct request
                requestPermission()
            }
        }
    }

    // Render Polite Rationale / Settings Redirection Dialog
    if (showRationaleDialog) {
        PermissionRationaleDialog(
            feature = feature,
            isPermanentlyDenied = isPermanentlyDenied,
            onGrantClick = {
                showRationaleDialog = false
                requestPermission()
            },
            onOpenSettingsClick = {
                showRationaleDialog = false
                PermissionUtils.openAppSettings(context)
            },
            onDismiss = {
                showRationaleDialog = false
                pendingOnGrantedAction = null
            }
        )
    }

    return remember(feature) {
        PermissionRequester(checkAndRequest)
    }
}

/**
 * Beautiful, user-friendly Material 3 Dialog explaining permission rationale
 * with direct actions to Grant or Open App Settings.
 */
@Composable
fun PermissionRationaleDialog(
    feature: AppFeaturePermission,
    isPermanentlyDenied: Boolean,
    onGrantClick: () -> Unit,
    onOpenSettingsClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFECFDF5)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = feature.icon,
                    contentDescription = null,
                    tint = AccentEmerald,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = feature.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = feature.description,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = Color(0xFF475569),
                    textAlign = TextAlign.Center
                )

                if (isPermanentlyDenied) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = Color(0xFFFEF2F2),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Permission was previously declined. Please enable it manually in App Settings to use this feature.",
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                color = Color(0xFF991B1B)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isPermanentlyDenied) {
                        onOpenSettingsClick()
                    } else {
                        onGrantClick()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (isPermanentlyDenied) "Open Settings" else "Grant Permission",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF64748B))
            ) {
                Text("Not Now")
            }
        }
    )
}
