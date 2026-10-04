package com.lojia.shiftreport.auth

import android.widget.Toast
import kotlinx.coroutines.launch
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lojia.shiftreport.R
import com.lojia.shiftreport.ui.common.LojiaDialog
import com.lojia.shiftreport.ui.theme.*

/**
 * Enterprise Admin Re-Authentication Dialog
 * Mandated for critical operations (e.g. deleting cashiers or changing system permissions).
 */
@Composable
fun AdminAuthDialog(
    actionTitle: String = "Delete Cashier",
    actionDescription: String = "Admin re-authentication required to complete this critical operation.",
    onDismissRequest: () -> Unit,
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    var adminPinInput by remember { mutableStateOf("") }
    var authError by remember { mutableStateOf<String?>(null) }
    var isAuthenticating by remember { mutableStateOf(false) }

    LojiaDialog(
        onDismissRequest = onDismissRequest,
        maxWidth = Dimens.DialogMaxWidth,
        shape = RoundedCornerShape(Dimens.DialogCornerRadius),
        containerColor = SurfaceLight,
        border = BorderStroke(1.dp, ErrorContainerLight),
        modifier = Modifier.testTag("adminAuthDialog")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(Dimens.DialogPaddingComfortable),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMd)
        ) {
            // Header with Admin Security Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ErrorContainerLight)
                            .border(1.dp, ErrorRedLight.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AdminPanelSettings,
                            contentDescription = null,
                            tint = ErrorRedLight,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Admin Re-Authentication",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnBackgroundLight,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = actionTitle,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ErrorRedLight,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(SurfaceVariantLight)
                        .testTag("btnDismissAdminAuth")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.cancel_18),
                        tint = TextHintColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Audit warning container (High contrast warning tokens)
            Surface(
                color = WarningContainer,
                shape = RoundedCornerShape(Dimens.RadiusSm),
                border = BorderStroke(1.dp, WarningOrange.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Shield,
                        contentDescription = null,
                        tint = WarningOrange,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = actionDescription,
                        fontSize = 11.5.sp,
                        color = Color(0xFF78350F), // High contrast dark amber (passes WCAG AAA)
                        lineHeight = 15.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // PIN / Password Input
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Enter Admin Security PIN or Password",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OnSurfaceVariantLight
                )

                OutlinedTextField(
                    value = adminPinInput,
                    onValueChange = {
                        adminPinInput = it
                        if (authError != null) authError = null
                    },
                    placeholder = {
                        Text(stringResource(R.string.enter_pin_or_password), style = MaterialTheme.typography.bodySmall, color = TextHintColor)
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = if (authError != null) ErrorRedLight else PrimaryIndigoLight,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    isError = authError != null,
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryIndigoLight,
                        unfocusedBorderColor = OutlineVariantLight,
                        focusedContainerColor = BackgroundLight,
                        unfocusedContainerColor = BackgroundLight,
                        errorBorderColor = ErrorRedLight
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("etAdminPin")
                )

                if (authError != null) {
                    Text(
                        text = authError ?: "",
                        fontSize = 11.sp,
                        color = ErrorRedLight,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Biometric quick option
            val activity = context as? androidx.fragment.app.FragmentActivity
            OutlinedButton(
                onClick = {
                    if (activity != null) {
                        BiometricAuthManager.showBiometricPrompt(
                            activity = activity,
                            title = "Admin Authentication",
                            subtitle = "Verify fingerprint or face to authorize",
                            description = actionDescription
                        ) { result ->
                            when (result) {
                                is BiometricAuthResult.Success -> {
                                    Toast.makeText(context, "Admin authenticated via biometrics.", Toast.LENGTH_SHORT).show()
                                    onAuthSuccess()
                                }
                                is BiometricAuthResult.Error -> {
                                    authError = "Biometric error: ${result.errString}"
                                }
                                is BiometricAuthResult.Failed -> {
                                    authError = "Biometric verification failed"
                                }
                                else -> {}
                            }
                        }
                    } else {
                        authError = "Biometric authentication not supported in this window"
                    }
                },
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, OutlineLight),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = BackgroundLight,
                    contentColor = OnBackgroundLight
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.ButtonHeightCompact)
                    .testTag("btnAdminBiometric")
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Fingerprint,
                        contentDescription = null,
                        tint = PrimaryIndigoLight,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Authenticate with Biometrics",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            HorizontalDivider(color = OutlineVariantLight, thickness = 1.dp)

            val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDismissRequest,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, OutlineLight),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = SurfaceLight,
                        contentColor = OnSurfaceVariantLight
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(Dimens.ButtonHeightStandard)
                        .testTag("btnCancelAdminAuth")
                ) {
                    Text(
                        text = stringResource(R.string.cancel_18),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Button(
                    onClick = {
                        val trimmedPin = adminPinInput.trim()
                        if (trimmedPin.isBlank()) {
                            authError = "Admin PIN or Password is required"
                        } else {
                            isAuthenticating = true
                            coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                val repo = com.lojia.shiftreport.data.ShiftReportRepository.getInstance(context)
                                val isMatch = repo.verifyAdminCredentials(context, trimmedPin)

                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    isAuthenticating = false
                                    if (isMatch) {
                                        Toast.makeText(context, "Admin authenticated. Audit log generated.", Toast.LENGTH_SHORT).show()
                                        onAuthSuccess()
                                    } else {
                                        authError = "Invalid Admin Credentials"
                                    }
                                }
                            }
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ErrorRedLight,
                        contentColor = PureWhite
                    ),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(Dimens.ButtonHeightStandard)
                        .testTag("btnConfirmAdminAuth")
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Confirm",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
