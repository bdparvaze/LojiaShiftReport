package com.lojia.shiftreport.auth

import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lojia.shiftreport.R
import com.lojia.shiftreport.data.AppDatabase
import com.lojia.shiftreport.ui.theme.*
import com.lojia.shiftreport.util.SecurityUtils
import com.lojia.shiftreport.util.isPinUsable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun AdminAuthDialog(
    onDismissRequest: () -> Unit,
    onAuthSuccess: () -> Unit,
    title: String = stringResource(R.string.auth_admin_confirmation),
    subtitle: String = stringResource(R.string.auth_confirm_action)
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val db = remember(context) { AppDatabase.getInstance(context) }

    var isPasswordMode by remember { mutableStateOf(false) }
    var pin by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }
    var attempts by remember { mutableIntStateOf(0) }
    var lockoutSeconds by remember { mutableIntStateOf(0) }
    var isVerifying by remember { mutableStateOf(false) }

    val shakeAnim = remember { Animatable(0f) }
    val pinFocusRequester = remember { FocusRequester() }

    LaunchedEffect(lockoutSeconds) {
        if (lockoutSeconds > 0) {
            delay(1000L)
            lockoutSeconds -= 1
            if (lockoutSeconds == 0) {
                attempts = 0
                isError = false
                errorMessage = null
            }
        }
    }

    LaunchedEffect(isPasswordMode) {
        if (!isPasswordMode) {
            delay(100L)
            runCatching { pinFocusRequester.requestFocus() }
        }
    }

    fun triggerShake() {
        coroutineScope.launch {
            val offsets = listOf(-12f, 12f, -10f, 10f, -6f, 6f, 0f)
            for (offset in offsets) {
                shakeAnim.animateTo(offset, animationSpec = tween(durationMillis = 45))
            }
        }
    }

    suspend fun performVerification(input: String, passwordMode: Boolean) {
        if (lockoutSeconds > 0 || isVerifying || isSuccess) return
        isVerifying = true
        errorMessage = null
        isError = false

        delay(300L)

        val isValid = if (!isPinUsable(input)) {
            false
        } else {
            try {
                val profile = db.reportDao().getUserProfileOnce()
                val pinMatched = if (!passwordMode) {
                    SecureStorageManager.verifyQuickPin(context, input) ||
                        (profile != null && isPinUsable(profile.pin) &&
                            SecurityUtils.verifySecretWithUpgrade(input, profile.pin).isMatch)
                } else {
                    false
                }
                val passwordMatched = profile != null && isPinUsable(profile.passwordHash) &&
                    SecurityUtils.verifySecretWithUpgrade(input, profile.passwordHash).isMatch

                pinMatched || passwordMatched
            } catch (_: Exception) {
                false
            }
        }

        if (isValid) {
            isVerifying = false
            isSuccess = true
            isError = false
            errorMessage = null
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            delay(500L)
            Toast.makeText(context, context.getString(R.string.auth_pin_confirmed_toast), Toast.LENGTH_SHORT).show()
            onAuthSuccess()
        } else {
            attempts += 1
            isError = true
            isSuccess = false
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            triggerShake()
            if (attempts >= AuthPinConstants.MAX_ATTEMPTS) {
                lockoutSeconds = AuthPinConstants.LOCKOUT_SECONDS
                errorMessage = context.getString(R.string.auth_too_many_attempts_format, lockoutSeconds)
            } else {
                val left = AuthPinConstants.MAX_ATTEMPTS - attempts
                errorMessage = if (passwordMode) {
                    context.getString(R.string.auth_invalid_pin_or_password_format, left)
                } else {
                    context.getString(R.string.auth_wrong_pin_format, left)
                }
            }
            delay(500L)
            if (!passwordMode) pin = "" else password = ""
            isVerifying = false
        }
    }

    // Auto-verify as soon as 6th digit is entered
    LaunchedEffect(pin) {
        if (!isPasswordMode && pin.length == AuthPinConstants.PIN_LENGTH && lockoutSeconds == 0) {
            performVerification(pin, passwordMode = false)
        }
    }

    // Auto-verify in password mode after 1s debounce
    LaunchedEffect(password) {
        if (isPasswordMode && password.length >= AuthPinConstants.MIN_PASSWORD_LENGTH && lockoutSeconds == 0) {
            delay(1000L)
            performVerification(password, passwordMode = true)
        }
    }

    val isPinComplete = pin.length == AuthPinConstants.PIN_LENGTH
    val isPasswordValid = password.length >= AuthPinConstants.MIN_PASSWORD_LENGTH
    val isConfirmEnabled = lockoutSeconds == 0 && !isVerifying && (
        isSuccess || if (isPasswordMode) isPasswordValid else isPinComplete
        )

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .background(OverlayScrim)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismissRequest
                ),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(Dimens.DialogCornerRadius),
                color = Color.Transparent,
                shadowElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .widthIn(max = 420.dp)
                    .wrapContentHeight()
                    .depthCard(shape = RoundedCornerShape(Dimens.DialogCornerRadius), cornerRadius = Dimens.DialogCornerRadius)
                    .verticalScroll(rememberScrollState())
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .imePadding(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top Shield + Lock Icon
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(PrimaryContainerLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = PrimaryIndigoLight,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Title
                    Text(
                        text = title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceLight,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Dynamic Subtitle
                    Text(
                        text = subtitle,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryIndigoLight,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Info Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .depthSemanticCard(
                                backgroundColor = DepthTokens.CardAccentGradientStart,
                                borderColor = PrimaryContainerLight,
                                shape = RoundedCornerShape(Dimens.RadiusMd)
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = PrimaryIndigoLight,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = stringResource(R.string.auth_info_banner),
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceVariantLight
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    if (!isPasswordMode) {
                        // 6-Digit Hidden OTP-style PIN Boxes
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset { IntOffset(shakeAnim.value.roundToInt(), 0) }
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    runCatching { pinFocusRequester.requestFocus() }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            BasicTextField(
                                value = pin,
                                onValueChange = { newValue ->
                                    if (lockoutSeconds == 0 && !isVerifying && !isSuccess) {
                                        val digits = newValue.filter { it.isDigit() }
                                            .take(AuthPinConstants.PIN_LENGTH)
                                        pin = digits
                                        if (digits.isNotEmpty()) {
                                            errorMessage = null
                                            isError = false
                                        }
                                    }
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.NumberPassword,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        if (pin.length == AuthPinConstants.PIN_LENGTH) {
                                            coroutineScope.launch {
                                                performVerification(pin, passwordMode = false)
                                            }
                                        }
                                    }
                                ),
                                cursorBrush = SolidColor(Color.Transparent),
                                modifier = Modifier
                                    .size(1.dp)
                                    .focusRequester(pinFocusRequester)
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                repeat(AuthPinConstants.PIN_LENGTH) { index ->
                                    val isFilled = pin.length > index
                                    val isFocused = index == pin.length && lockoutSeconds == 0
                                    val boxShape = RoundedCornerShape(Dimens.RadiusMd)

                                    Box(
                                        modifier = Modifier
                                            .size(width = 44.dp, height = 56.dp)
                                            .depthInset(
                                                enabled = lockoutSeconds == 0,
                                                isError = isError && !isSuccess,
                                                isFocused = isFilled || isFocused,
                                                shape = boxShape,
                                                cornerRadius = Dimens.RadiusMd
                                            )
                                            .then(
                                                if (isSuccess) {
                                                    Modifier.border(Dimens.InsetFocusedBorderWidth, SuccessGreen, boxShape)
                                                } else {
                                                    Modifier
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isFilled) {
                                            Text(
                                                text = "●",
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = when {
                                                    isSuccess -> SuccessTextDark
                                                    isError -> ErrorRedLight
                                                    else -> OnSurfaceLight
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Admin Password Field
                        com.lojia.shiftreport.ui.common.LojiaTextField(
                            value = password,
                            onValueChange = {
                                if (lockoutSeconds == 0 && !isVerifying && !isSuccess) {
                                    password = it
                                    if (it.isNotEmpty()) {
                                        errorMessage = null
                                        isError = false
                                    }
                                }
                            },
                            label = { Text(stringResource(R.string.admin_password_label)) },
                            singleLine = true,
                            isError = isError,
                            visualTransformation = if (passwordVisible) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = { passwordVisible = !passwordVisible },
                                    modifier = Modifier.size(Dimens.MinTouchTarget)
                                ) {
                                    Icon(
                                        imageVector = if (passwordVisible) {
                                            Icons.Default.VisibilityOff
                                        } else {
                                            Icons.Default.Visibility
                                        },
                                        contentDescription = if (passwordVisible) {
                                            stringResource(R.string.cd_hide_password)
                                        } else {
                                            stringResource(R.string.cd_show_password)
                                        }
                                    )
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (password.length >= AuthPinConstants.MIN_PASSWORD_LENGTH) {
                                        coroutineScope.launch {
                                            performVerification(password, passwordMode = true)
                                        }
                                    }
                                }
                            ),
                            shape = RoundedCornerShape(Dimens.RadiusMd),
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset { IntOffset(shakeAnim.value.roundToInt(), 0) }
                        )
                    }

                    // Helper / Attempts Indicator
                    Text(
                        text = if (isPasswordMode) {
                            stringResource(R.string.auth_enter_admin_password_hint)
                        } else {
                            stringResource(R.string.auth_enter_admin_pin_hint)
                        },
                        fontSize = 12.sp,
                        color = if (isError) ErrorRedLight else OnSurfaceVariantLight,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    // Instant Green Success Card
                    if (isSuccess) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                                .depthSemanticCard(
                                    backgroundColor = SuccessContainer,
                                    borderColor = AuthSuccessMint,
                                    shape = RoundedCornerShape(Dimens.RadiusSm)
                                )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = SuccessTextDark,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.auth_pin_access_granted),
                                    color = SuccessTextDark,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Instant Red Error Card
                    val displayError = if (lockoutSeconds > 0) {
                        stringResource(R.string.auth_too_many_attempts_format, lockoutSeconds)
                    } else {
                        errorMessage
                    }
                    if (displayError != null && !isSuccess) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                                .depthSemanticCard(
                                    backgroundColor = NegativeBannerSurface,
                                    borderColor = NegativeBannerBorder,
                                    shape = RoundedCornerShape(Dimens.RadiusSm)
                                )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = null,
                                    tint = ErrorRedLight,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = displayError,
                                    color = ErrorRedLight,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Toggle between 6-digit PIN and Admin Password
                    TextButton(
                        onClick = {
                            isPasswordMode = !isPasswordMode
                            errorMessage = null
                            isError = false
                        },
                        modifier = Modifier.defaultMinSize(minHeight = Dimens.MinTouchTarget)
                    ) {
                        Text(
                            text = if (isPasswordMode) {
                                stringResource(R.string.auth_use_admin_pin_toggle)
                            } else {
                                stringResource(R.string.auth_use_admin_password_toggle)
                            },
                            color = PrimaryIndigoLight,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Bottom Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onDismissRequest,
                            shape = RoundedCornerShape(Dimens.RadiusMd),
                            border = null,
                            elevation = null,
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.Transparent,
                                contentColor = PrimaryBlueLight
                            ),
                            modifier = Modifier
                                .heightIn(min = Dimens.ButtonHeightPrimary)
                                .depthSecondaryButton(
                                    cornerRadius = Dimens.RadiusMd,
                                    minHeight = Dimens.ButtonHeightPrimary
                                )
                        ) {
                            Text(
                                text = stringResource(R.string.cancel),
                                fontSize = DepthTypeTokens.SecondaryButtonTextSize,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        val buttonActive = isSuccess || isError || isVerifying || isConfirmEnabled
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val input = if (isPasswordMode) password else pin
                                    performVerification(input, passwordMode = isPasswordMode)
                                }
                            },
                            enabled = isConfirmEnabled,
                            elevation = null,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSuccess) SuccessGreen else Color.Transparent,
                                contentColor = if (buttonActive) PureWhite else OnSurfaceVariantLight,
                                disabledContainerColor = if (isSuccess) SuccessGreen else Color.Transparent,
                                disabledContentColor = if (isVerifying || isSuccess) PureWhite else OnSurfaceVariantLight
                            ),
                            shape = RoundedCornerShape(Dimens.RadiusMd),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = Dimens.ButtonHeightPrimary)
                                .then(
                                    if (isSuccess) {
                                        Modifier.depthSemanticCard(
                                            backgroundColor = SuccessGreen,
                                            borderColor = SuccessTextDark,
                                            shape = RoundedCornerShape(Dimens.RadiusMd),
                                            cornerRadius = Dimens.RadiusMd
                                        )
                                    } else {
                                        Modifier.depthRaisedButton(
                                            enabled = isConfirmEnabled || isVerifying,
                                            isDestructive = isError,
                                            cornerRadius = Dimens.RadiusMd,
                                            minHeight = Dimens.ButtonHeightPrimary
                                        )
                                    }
                                )
                        ) {
                            when {
                                isVerifying -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = PureWhite
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stringResource(R.string.verifying_label), fontWeight = FontWeight.Bold)
                                }
                                isSuccess -> {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.confirmed_label), fontWeight = FontWeight.Bold)
                                }
                                isError -> {
                                    Icon(
                                        imageVector = Icons.Default.Error,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.retry_label), fontWeight = FontWeight.Bold)
                                }
                                isPasswordMode || isPinComplete -> {
                                    Text(stringResource(R.string.confirm), fontWeight = FontWeight.Bold)
                                }
                                else -> {
                                    Text(
                                        text = stringResource(R.string.auth_enter_more_format, AuthPinConstants.PIN_LENGTH - pin.length),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
