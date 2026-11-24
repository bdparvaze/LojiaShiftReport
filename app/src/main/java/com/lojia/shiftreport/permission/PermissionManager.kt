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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.ui.res.stringResource
import com.lojia.shiftreport.R
import com.lojia.shiftreport.ui.common.LojiaDialog
import com.lojia.shiftreport.ui.theme.*

/**
 * Supported features that require runtime dangerous permissions.
 */
enum class AppFeaturePermission(
    @androidx.annotation.StringRes val titleRes: Int,
    @androidx.annotation.StringRes val descriptionRes: Int,
    val icon: ImageVector,
    val permissionsGetter: () -> List<String>
) {
    CAMERA(
        titleRes = R.string.permission_camera_title,
        descriptionRes = R.string.permission_camera_desc,
        icon = Icons.Outlined.CameraAlt,
        permissionsGetter = { listOf(Manifest.permission.CAMERA) }
    ),
    BLUETOOTH_PRINTER(
        titleRes = R.string.permission_bluetooth_title,
        descriptionRes = R.string.permission_bluetooth_desc,
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
        titleRes = R.string.permission_notifications_title,
        descriptionRes = R.string.permission_notifications_desc,
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

    /**
     * Returns true if Android 12+ (SDK 31+) Bluetooth Scan & Connect permissions are granted.
     */
    fun hasBluetoothPermissions(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            hasPermission(context, Manifest.permission.BLUETOOTH_SCAN) &&
                hasPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ||
                hasPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    /**
     * Returns true if Android 13+ (SDK 33+) POST_NOTIFICATIONS permission is granted or not required.
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            true
        }
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
    LojiaDialog(
        onDismissRequest = onDismiss,
        maxWidth = Dimens.DialogSmallMaxWidth,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.DialogPaddingComfortable),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMd)
        ) {
            Box(
                modifier = Modifier
                    .size(Dimens.TextFieldHeightStandard)
                    .depthRaisedIconTile(
                        gradientColors = listOf(PureWhite, TileTealEnd),
                        cornerRadius = Dimens.RadiusEmployeeAvatarTile
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = feature.icon,
                    contentDescription = null,
                    tint = AccentEmerald,
                    modifier = Modifier.size(Dimens.IconLg)
                )
            }

            Text(
                text = stringResource(feature.titleRes),
                fontSize = DepthTypeTokens.QuickPinTitleSize,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryReport,
                textAlign = TextAlign.Center
            )

            Text(
                text = stringResource(feature.descriptionRes),
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariantLight,
                textAlign = TextAlign.Center
            )

            if (isPermanentlyDenied) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .depthSemanticCard(
                            backgroundColor = ErrorContainerLight,
                            borderColor = ErrorRedLight.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(Dimens.RadiusMd)
                        )
                        .padding(Dimens.SpacingMd),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = ErrorRedLight,
                        modifier = Modifier.size(Dimens.IconSm)
                    )
                    Spacer(modifier = Modifier.width(Dimens.SpacingSm))
                    Text(
                        text = stringResource(R.string.permission_permanently_denied_desc),
                        fontSize = DepthTypeTokens.ListRowSubtitleSize,
                        color = ErrorRedLight
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.SpacingXs))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(Dimens.RadiusReportInput),
                    border = null,
                    elevation = null,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = OnSurfaceVariantLight
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Dimens.ButtonHeightPrimary)
                        .depthSecondaryButton(cornerRadius = Dimens.RadiusReportInput)
                ) {
                    Text(
                        text = stringResource(R.string.btn_not_now),
                        fontWeight = FontWeight.SemiBold,
                        color = OnSurfaceVariantLight
                    )
                }

                Button(
                    onClick = {
                        if (isPermanentlyDenied) {
                            onOpenSettingsClick()
                        } else {
                            onGrantClick()
                        }
                    },
                    elevation = null,
                    shape = RoundedCornerShape(Dimens.RadiusReportInput),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = PureWhite
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Dimens.ButtonHeightPrimary)
                        .depthRaisedButton(cornerRadius = Dimens.RadiusReportInput)
                ) {
                    Text(
                        text = if (isPermanentlyDenied) stringResource(R.string.btn_open_settings) else stringResource(R.string.btn_grant_permission),
                        fontWeight = FontWeight.SemiBold,
                        color = PureWhite
                    )
                }
            }
        }
    }
}
