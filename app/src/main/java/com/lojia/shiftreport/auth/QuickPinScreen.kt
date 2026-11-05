package com.lojia.shiftreport.auth

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojia.shiftreport.R
import com.lojia.shiftreport.data.PreferencesRepository
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
        SecureStorageManager.hasEncryptedPin(context) ||
            (preferencesRepository.hasPinConfigured() && !preferencesRepository.getStoredPinHash().isNullOrBlank())
    }
    var currentMode by remember {
        mutableStateOf(if (hasPin) QuickPinMode.ENTER_PIN else QuickPinMode.CREATE_PIN)
    }

    var enteredPin by remember { mutableStateOf("") }
    var createdPinTemp by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var failedAttempts by remember { mutableIntStateOf(0) }
    var lockoutSeconds by remember { mutableIntStateOf(0) }

    val isLockedOut = lockoutSeconds > 0

    // 30-second POS security lockout countdown after 3 failed attempts
    LaunchedEffect(lockoutSeconds) {
        if (lockoutSeconds > 0) {
            delay(1000L)
            lockoutSeconds -= 1
            if (lockoutSeconds == 0) {
                failedAttempts = 0
                pinError = false
                errorMessage = null
            }
        }
    }

    // Auto-verify when 6 digits are entered
    LaunchedEffect(enteredPin) {
        if (enteredPin.length == AuthPinConstants.PIN_LENGTH && !isLockedOut) {
            when (currentMode) {
                QuickPinMode.ENTER_PIN -> {
                    val isVerified = SecureStorageManager.verifyQuickPin(context, enteredPin) ||
                        preferencesRepository.verifyPin(enteredPin)
                    if (isVerified) {
                        pinError = false
                        errorMessage = null
                        failedAttempts = 0
                        onAuthenticated()
                    } else {
                        failedAttempts += 1
                        pinError = true
                        if (failedAttempts >= 3) {
                            lockoutSeconds = 30
                            errorMessage = if (isBn) {
                                "পিন ভুলে গেছেন? পাসওয়ার্ড ব্যবহার করুন"
                            } else {
                                "Forgot PIN? Use password"
                            }
                        } else {
                            errorMessage = context.getString(R.string.quick_pin_incorrect)
                        }
                        delay(500L)
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
                        SecureStorageManager.saveQuickPin(context, enteredPin)
                        preferencesRepository.setQuickPin(enteredPin)
                        preferencesRepository.setQuickLoginEnabled(true)
                        Toast.makeText(
                            context,
                            context.getString(R.string.settings_quick_pin_saved),
                            Toast.LENGTH_SHORT
                        ).show()
                        onAuthenticated()
                    } else {
                        pinError = true
                        errorMessage = if (isBn) {
                            "পিন মেলেনি, আবার চেষ্টা করুন"
                        } else {
                            "PINs do not match. Please try again."
                        }
                        delay(600L)
                        enteredPin = ""
                    }
                }
            }
        } else if (enteredPin.isNotEmpty() && !isLockedOut) {
            pinError = false
            errorMessage = null
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

    val isBiometricAvailable = remember(context) {
        BiometricManager.isBiometricAvailable(context)
    }
    val showBiometric = isBiometricAvailable &&
        preferencesRepository.isBiometricEnabled() &&
        currentMode == QuickPinMode.ENTER_PIN

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 360.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = titleText,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = subtitleText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 6-box PIN indicator: always dots (●), no cursor, no visible numbers
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("six_digit_pin_screen_input")
            ) {
                for (index in 0 until AuthPinConstants.PIN_LENGTH) {
                    val isFilled = index < enteredPin.length
                    val isActive = index == enteredPin.length && !isLockedOut
                    val borderColor = when {
                        pinError || isLockedOut -> MaterialTheme.colorScheme.error
                        isActive -> MaterialTheme.colorScheme.primary
                        isFilled -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        else -> MaterialTheme.colorScheme.outline
                    }

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(
                                width = 1.dp,
                                color = borderColor,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isFilled) {
                            Text(
                                text = "●",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Error / 30s Lockout message
            if (errorMessage != null || isLockedOut) {
                Spacer(modifier = Modifier.height(12.dp))
                val displayError = if (isLockedOut) {
                    if (isBn) {
                        "পিন ভুলে গেছেন? পাসওয়ার্ড ব্যবহার করুন (${lockoutSeconds}সে)"
                    } else {
                        "Forgot PIN? Use password (${lockoutSeconds}s)"
                    }
                } else {
                    errorMessage.orEmpty()
                }
                Text(
                    text = displayError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("quick_pin_error_text")
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 3-column Material 3 Number Pad
            val padItems = listOf(
                "1", "2", "3",
                "4", "5", "6",
                "7", "8", "9",
                "BIOMETRIC", "0", "DELETE"
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                userScrollEnabled = false,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.width(248.dp)
            ) {
                items(padItems.size) { idx ->
                    val item = padItems[idx]
                    when (item) {
                        "BIOMETRIC" -> {
                            Box(
                                modifier = Modifier.size(72.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (showBiometric) {
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(CircleShape)
                                            .clickable(
                                                enabled = !isLockedOut,
                                                onClick = onTriggerBiometric
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Fingerprint,
                                            contentDescription = stringResource(R.string.cd_biometric),
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                            }
                        }
                        "DELETE" -> {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .clickable(
                                        enabled = !isLockedOut && enteredPin.isNotEmpty(),
                                        onClick = {
                                            if (enteredPin.isNotEmpty()) {
                                                enteredPin = enteredPin.dropLast(1)
                                            }
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                                    contentDescription = stringResource(R.string.btn_numpad_del),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        else -> {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable(
                                        enabled = !isLockedOut,
                                        onClick = {
                                            if (enteredPin.length < AuthPinConstants.PIN_LENGTH) {
                                                enteredPin += item
                                            }
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = item,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = onFallbackToLogin,
                modifier = Modifier.testTag("btn_fallback_to_login")
            ) {
                Text(
                    text = stringResource(R.string.quick_pin_use_password),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
