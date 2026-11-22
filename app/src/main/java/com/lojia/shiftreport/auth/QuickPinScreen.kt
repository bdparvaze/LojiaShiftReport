package com.lojia.shiftreport.auth

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojia.shiftreport.R
import com.lojia.shiftreport.data.PreferencesRepository
import com.lojia.shiftreport.ui.theme.*
import com.lojia.shiftreport.util.isPinUsable
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
            (preferencesRepository.hasPinConfigured() && isPinUsable(preferencesRepository.getStoredPinHash()))
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
                    val isVerified = isPinUsable(enteredPin) && (
                        SecureStorageManager.verifyQuickPin(context, enteredPin) ||
                            preferencesRepository.verifyPin(enteredPin)
                        )
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
                            errorMessage = context.getString(R.string.quick_pin_lockout_prompt)
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
                        errorMessage = context.getString(R.string.quick_pin_mismatch_error)
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
        QuickPinMode.CREATE_PIN -> stringResource(R.string.quick_pin_create_title)
        QuickPinMode.CONFIRM_PIN -> stringResource(R.string.quick_pin_confirm_title)
    }

    val subtitleText = when (currentMode) {
        QuickPinMode.ENTER_PIN -> stringResource(R.string.quick_pin_subtitle)
        QuickPinMode.CREATE_PIN -> stringResource(R.string.quick_pin_create_subtitle)
        QuickPinMode.CONFIRM_PIN -> stringResource(R.string.quick_pin_confirm_subtitle)
    }

    val isBiometricAvailable = remember(context) {
        BiometricManager.isBiometricAvailable(context)
    }
    val showBiometric = isBiometricAvailable &&
        preferencesRepository.isBiometricEnabled() &&
        currentMode == QuickPinMode.ENTER_PIN

    val style = LocalDepthStyle.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(style.pageBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = Dimens.QuickPinMaxWidth + Dimens.QuickPinSidePadding * 2)
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = Dimens.QuickPinSidePadding, vertical = Dimens.SpacingLg)
                .cardRise(0),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = titleText,
                fontFamily = PoppinsFontFamily,
                fontSize = DepthTypeTokens.QuickPinTitleSize,
                fontWeight = FontWeight.Bold,
                color = OnBackgroundLight,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(Dimens.SpacingSm))

            Text(
                text = subtitleText,
                fontFamily = PoppinsFontFamily,
                fontSize = DepthTypeTokens.QuickPinSubtitleSize,
                color = OnSurfaceVariantLight,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // 6-box PIN indicator: pressed-in inset wells with dots (●)
            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimens.PinCellGap, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("six_digit_pin_screen_input")
            ) {
                for (index in 0 until AuthPinConstants.PIN_LENGTH) {
                    val isFilled = index < enteredPin.length
                    val isActive = index == enteredPin.length && !isLockedOut

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(DepthTokens.PIN_CELL_ASPECT_RATIO)
                            .depthInset(
                                enabled = !isLockedOut,
                                isError = pinError || isLockedOut,
                                isFocused = isActive,
                                shape = RoundedCornerShape(Dimens.RadiusMd),
                                cornerRadius = Dimens.RadiusMd,
                                gradientColors = if (isFilled) {
                                    listOf(style.pinCellFilledStart, style.pinCellFilledEnd)
                                } else {
                                    null
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isFilled) {
                            Text(
                                text = "●",
                                fontSize = DepthTypeTokens.QuickPinDotSize,
                                fontWeight = FontWeight.Bold,
                                color = if (pinError || isLockedOut) ErrorRedLight else OnBackgroundLight,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Error / 30s Lockout message
            if (errorMessage != null || isLockedOut) {
                Spacer(modifier = Modifier.height(Dimens.SpacingMd))
                val displayError = if (isLockedOut) {
                    stringResource(R.string.quick_pin_lockout_countdown_fmt, lockoutSeconds)
                } else {
                    errorMessage.orEmpty()
                }
                Text(
                    text = displayError,
                    color = ErrorRedLight,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("quick_pin_error_text")
                )
            }

            Spacer(modifier = Modifier.height(Dimens.SpacingLg))

            // 4 rows x 3 columns raised square keypad (always LTR per international keypad standard)
            val padRows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("BIOMETRIC", "0", "DELETE")
            )

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Dimens.KeypadColumnGap)
                ) {
                    padRows.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Dimens.KeypadColumnGap)
                        ) {
                            rowItems.forEach { item ->
                                when (item) {
                                    "BIOMETRIC" -> {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(Dimens.KeypadButtonHeight),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (showBiometric) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(Dimens.KeypadButtonHeight)
                                                        .depthSecondaryButton(
                                                            enabled = !isLockedOut,
                                                            borderColor = PrimaryBlueLight,
                                                            cornerRadius = Dimens.RadiusKeypad,
                                                            minHeight = Dimens.KeypadButtonHeight
                                                        )
                                                        .clickable(
                                                            enabled = !isLockedOut,
                                                            onClick = onTriggerBiometric
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Fingerprint,
                                                        contentDescription = stringResource(R.string.cd_biometric),
                                                        tint = PrimaryBlueLight,
                                                        modifier = Modifier.size(Dimens.IconLg)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    "DELETE" -> {
                                        val canDelete = !isLockedOut && enteredPin.isNotEmpty()
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .depthKeypadKey(enabled = canDelete)
                                                .clickable(
                                                    enabled = canDelete,
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
                                                contentDescription = stringResource(R.string.cd_backspace),
                                                tint = OnSurfaceVariantLight,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                    else -> {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .depthKeypadKey(enabled = !isLockedOut)
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
                                                fontFamily = PoppinsFontFamily,
                                                fontSize = DepthTypeTokens.KeypadDigitSize,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isLockedOut) OnSurfaceVariantLight else OnBackgroundLight,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimens.SpacingMd))

            TextButton(
                onClick = onFallbackToLogin,
                modifier = Modifier
                    .defaultMinSize(minHeight = Dimens.MinTouchTarget)
                    .testTag("btn_fallback_to_login")
            ) {
                Text(
                    text = stringResource(R.string.quick_pin_use_password),
                    fontFamily = PoppinsFontFamily,
                    color = PrimaryBlueLight,
                    fontSize = DepthTypeTokens.QuickPinFallbackTextSize,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
