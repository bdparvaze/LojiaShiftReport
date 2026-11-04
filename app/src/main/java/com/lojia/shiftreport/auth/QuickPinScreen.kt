package com.lojia.shiftreport.auth

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojia.shiftreport.R
import com.lojia.shiftreport.data.PreferencesRepository
import com.lojia.shiftreport.ui.theme.*
import kotlinx.coroutines.delay

enum class QuickPinMode {
    ENTER_PIN,
    CREATE_PIN,
    CONFIRM_PIN
}

@Composable
fun QuickPinScreen(
    isBn: Boolean,
    preferencesRepository: PreferencesRepository,
    onAuthenticated: () -> Unit,
    onFallbackToLogin: () -> Unit,
    onTriggerBiometric: () -> Unit
) {
    val context = LocalContext.current
    val hasPin = remember {
        preferencesRepository.hasPinConfigured() && !preferencesRepository.getStoredPinHash().isNullOrBlank()
    }
    var currentMode by remember {
        mutableStateOf(if (hasPin) QuickPinMode.ENTER_PIN else QuickPinMode.CREATE_PIN)
    }

    var enteredPin by remember { mutableStateOf("") }
    var createdPinTemp by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isMasked by remember { mutableStateOf(true) }

    // When user completes typing 6 digits
    LaunchedEffect(enteredPin) {
        if (enteredPin.length == AuthPinConstants.PIN_LENGTH) {
            when (currentMode) {
                QuickPinMode.ENTER_PIN -> {
                    val isVerified = preferencesRepository.verifyPin(enteredPin)
                    if (isVerified) {
                        pinError = false
                        errorMessage = null
                        onAuthenticated()
                    } else {
                        pinError = true
                        errorMessage = context.getString(R.string.quick_pin_incorrect)
                        delay(600)
                        enteredPin = ""
                    }
                }
                QuickPinMode.CREATE_PIN -> {
                    createdPinTemp = enteredPin
                    enteredPin = ""
                    pinError = false
                    errorMessage = null
                    currentMode = QuickPinMode.CONFIRM_PIN
                }
                QuickPinMode.CONFIRM_PIN -> {
                    if (enteredPin == createdPinTemp) {
                        pinError = false
                        errorMessage = null
                        preferencesRepository.setQuickPin(enteredPin)
                        preferencesRepository.setQuickLoginEnabled(true)
                        Toast.makeText(context, context.getString(R.string.settings_quick_pin_saved), Toast.LENGTH_SHORT).show()
                        onAuthenticated()
                    } else {
                        pinError = true
                        errorMessage = if (isBn) "পিন মেলেনি, আবার চেষ্টা করুন" else "PINs do not match. Please try again."
                        delay(700)
                        enteredPin = ""
                    }
                }
            }
        } else {
            if (pinError && enteredPin.isEmpty()) {
                // keep error message until user types
            } else if (enteredPin.isNotEmpty()) {
                pinError = false
            }
        }
    }

    val titleText = when (currentMode) {
        QuickPinMode.ENTER_PIN -> stringResource(R.string.quick_pin_enter_pin)
        QuickPinMode.CREATE_PIN -> if (isBn) "নতুন ৬-সংখ্যার পিন তৈরি করুন" else "Create 6-Digit PIN"
        QuickPinMode.CONFIRM_PIN -> if (isBn) "পিন নিশ্চিত করুন" else "Confirm 6-Digit PIN"
    }

    val subtitleText = when (currentMode) {
        QuickPinMode.ENTER_PIN -> stringResource(R.string.quick_pin_subtitle)
        QuickPinMode.CREATE_PIN -> if (isBn) "অ্যাপ সহজে আনলক করতে ৬-সংখ্যার পিন সেট করুন" else "Set a 6-digit PIN for instant secure access"
        QuickPinMode.CONFIRM_PIN -> if (isBn) "নিশ্চিত করতে একই ৬-সংখ্যার পিন আবার লিখুন" else "Re-enter the 6-digit PIN to confirm"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .padding(horizontal = 24.dp)
            .imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 380.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = titleText,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = OnBackgroundLight,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = subtitleText,
                fontSize = 13.5.sp,
                color = OnSurfaceVariantLight,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // 6-box OTP Input
            SixDigitPinInput(
                pin = enteredPin,
                onPinChange = { enteredPin = it },
                isMasked = isMasked,
                isError = pinError,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("six_digit_pin_screen_input")
            )

            // Visibility toggle & Error Display
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_pin_error_text")
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                IconButton(
                    onClick = { isMasked = !isMasked },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isMasked) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (isMasked) "Show PIN" else "Hide PIN",
                        tint = OnSurfaceVariantLight,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Biometric fallback button below PIN boxes
            if (preferencesRepository.isBiometricEnabled() && currentMode == QuickPinMode.ENTER_PIN) {
                OutlinedButton(
                    onClick = onTriggerBiometric,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_biometric_fallback"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null,
                        tint = PrimaryIndigoLight,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.btn_auth_with_biometrics),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryIndigoLight
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // On-screen Numpad
            NumpadView(
                onNumberClick = { num ->
                    if (enteredPin.length < AuthPinConstants.PIN_LENGTH) {
                        enteredPin += num
                    }
                },
                onDeleteClick = {
                    if (enteredPin.isNotEmpty()) {
                        enteredPin = enteredPin.dropLast(1)
                    }
                },
                showBiometric = preferencesRepository.isBiometricEnabled() && currentMode == QuickPinMode.ENTER_PIN,
                onBiometricClick = onTriggerBiometric
            )

            Spacer(modifier = Modifier.height(20.dp))

            TextButton(
                onClick = onFallbackToLogin,
                modifier = Modifier.testTag("btn_fallback_to_login")
            ) {
                Text(
                    text = stringResource(R.string.quick_pin_use_password),
                    color = PrimaryIndigoLight,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun NumpadView(
    onNumberClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    showBiometric: Boolean,
    onBiometricClick: () -> Unit
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9")
    )

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        for (row in rows) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (num in row) {
                    NumpadButton(text = num, onClick = { onNumberClick(num) }, modifier = Modifier.weight(1f))
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showBiometric) {
                Box(
                    modifier = Modifier.weight(1f).aspectRatio(1.2f),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = onBiometricClick) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = stringResource(R.string.cd_biometric),
                            tint = PrimaryIndigoLight,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            NumpadButton(text = "0", onClick = { onNumberClick("0") }, modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier.weight(1f).aspectRatio(1.2f),
                contentAlignment = Alignment.Center
            ) {
                TextButton(onClick = onDeleteClick) {
                    Text(
                        text = stringResource(R.string.btn_numpad_del),
                        color = OnSurfaceVariantLight,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun NumpadButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.aspectRatio(1.2f),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = OnBackgroundLight
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
    ) {
        Text(
            text = text,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
