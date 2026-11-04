package com.lojia.shiftreport.auth

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojia.shiftreport.R
import com.lojia.shiftreport.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Enterprise Admin Re-Authentication Dialog (Foodics POS / Banking App standard).
 * Uses Material 3 AlertDialog with 28dp corner radius and SixDigitPinInput.
 */
@Composable
fun AdminAuthDialog(
    actionTitle: String = stringResource(R.string.admin_reauth_default_title),
    actionDescription: String = stringResource(R.string.admin_reauth_default_desc),
    onDismissRequest: () -> Unit,
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var adminPinInput by remember { mutableStateOf("") }
    var authError by remember { mutableStateOf<String?>(null) }
    var isAuthenticating by remember { mutableStateOf(false) }

    fun performPinVerification(pin: String) {
        val trimmed = pin.trim()
        if (trimmed.length != AuthPinConstants.PIN_LENGTH) {
            authError = context.getString(R.string.pin_must_be_4_digits)
            return
        }
        isAuthenticating = true
        coroutineScope.launch(Dispatchers.IO) {
            val secureStorage = SecureStorageManager
            val isPinMatch = AdminAuthManager.requireAdminAuth(BiometricManager, secureStorage, trimmed)
            val isRepoMatch = if (!isPinMatch) {
                com.lojia.shiftreport.data.ShiftReportRepository.getInstance(context)
                    .verifyAdminCredentials(context, trimmed)
            } else true

            withContext(Dispatchers.Main) {
                isAuthenticating = false
                if (isPinMatch || isRepoMatch) {
                    Toast.makeText(context, context.getString(R.string.toast_admin_authenticated), Toast.LENGTH_SHORT).show()
                    onAuthSuccess()
                } else {
                    authError = context.getString(R.string.invalid_admin_credentials)
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(28.dp),
        containerColor = SurfaceLight,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(PrimaryContainerLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AdminPanelSettings,
                        contentDescription = null,
                        tint = PrimaryIndigoLight,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = stringResource(R.string.admin_reauth_title),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnBackgroundLight
                    )
                    Text(
                        text = actionTitle,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = PrimaryIndigoLight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Description badge
                Surface(
                    color = SurfaceVariantLight,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, OutlineLight)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Shield,
                            contentDescription = null,
                            tint = PrimaryIndigoLight,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = actionDescription,
                            fontSize = 11.5.sp,
                            color = OnSurfaceVariantLight,
                            lineHeight = 15.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Biometric quick button
                val activity = context as? androidx.fragment.app.FragmentActivity
                OutlinedButton(
                    onClick = {
                        if (activity != null) {
                            BiometricAuthManager.showBiometricPrompt(
                                activity = activity,
                                title = context.getString(R.string.biometric_authentication),
                                subtitle = context.getString(R.string.biometric_prompt_default_subtitle),
                                description = actionDescription
                            ) { result ->
                                when (result) {
                                    is BiometricAuthResult.Success -> {
                                        Toast.makeText(context, context.getString(R.string.toast_admin_authenticated), Toast.LENGTH_SHORT).show()
                                        onAuthSuccess()
                                    }
                                    is BiometricAuthResult.Error -> {
                                        authError = result.errString.toString()
                                    }
                                    is BiometricAuthResult.Failed -> {
                                        authError = context.getString(R.string.biometric_verification_failed)
                                    }
                                    else -> {}
                                }
                            }
                        } else {
                            authError = context.getString(R.string.biometric_not_supported_window)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, OutlineLight),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("btnAdminBiometric")
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Fingerprint,
                            contentDescription = null,
                            tint = PrimaryIndigoLight,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(R.string.btn_auth_with_biometrics),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryIndigoLight
                        )
                    }
                }

                HorizontalDivider(color = OutlineVariantLight)

                // 6-digit PIN input
                Text(
                    text = stringResource(R.string.enter_admin_security_pin),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OnSurfaceVariantLight,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                SixDigitPinInput(
                    pin = adminPinInput,
                    onPinChange = {
                        adminPinInput = it
                        if (authError != null) authError = null
                        if (it.length == AuthPinConstants.PIN_LENGTH) {
                            performPinVerification(it)
                        }
                    },
                    isMasked = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("six_digit_admin_pin_input")
                )

                if (authError != null) {
                    Text(
                        text = authError ?: "",
                        fontSize = 12.sp,
                        color = ErrorRedLight,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { performPinVerification(adminPinInput) },
                enabled = adminPinInput.length == AuthPinConstants.PIN_LENGTH && !isAuthenticating,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigoLight),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("btnConfirmAdminAuth")
            ) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest,
                modifier = Modifier.testTag("btnCancelAdminAuth")
            ) {
                Text(stringResource(R.string.cancel_18))
            }
        },
        modifier = Modifier.testTag("adminAuthDialog")
    )
}
