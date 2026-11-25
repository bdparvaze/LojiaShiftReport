package com.lojia.shiftreport.auth

import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import com.lojia.shiftreport.R
import com.lojia.shiftreport.data.AppDatabase
import com.lojia.shiftreport.ui.common.DialogSize
import com.lojia.shiftreport.ui.common.LojiaDialog
import com.lojia.shiftreport.ui.common.LojiaTextField
import com.lojia.shiftreport.ui.common.PinCells
import com.lojia.shiftreport.ui.common.PinCellsState
import com.lojia.shiftreport.ui.theme.AuthSuccessMint
import com.lojia.shiftreport.ui.theme.DepthTypeTokens
import com.lojia.shiftreport.ui.theme.Dimens
import com.lojia.shiftreport.ui.theme.ErrorRedLight
import com.lojia.shiftreport.ui.theme.NegativeBannerBorder
import com.lojia.shiftreport.ui.theme.NegativeBannerSurface
import com.lojia.shiftreport.ui.theme.OnSurfaceLight
import com.lojia.shiftreport.ui.theme.OnSurfaceVariantLight
import com.lojia.shiftreport.ui.theme.PrimaryBlueLight
import com.lojia.shiftreport.ui.theme.PrimaryContainerLight
import com.lojia.shiftreport.ui.theme.PrimaryIndigoLight
import com.lojia.shiftreport.ui.theme.PureWhite
import com.lojia.shiftreport.ui.theme.SuccessContainer
import com.lojia.shiftreport.ui.theme.SuccessGreen
import com.lojia.shiftreport.ui.theme.SuccessTextDark
import com.lojia.shiftreport.ui.theme.depthRaisedButton
import com.lojia.shiftreport.ui.theme.depthSecondaryButton
import com.lojia.shiftreport.ui.theme.depthSemanticCard
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

    LojiaDialog(
        onDismissRequest = onDismissRequest,
        size = DialogSize.Small
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Lock Icon Circle
            Box(
                modifier = Modifier
                    .size(Dimens.VerifyIconCircleSize)
                    .clip(CircleShape)
                    .background(PrimaryContainerLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = PrimaryIndigoLight,
                    modifier = Modifier.size(Dimens.VerifyIconSize)
                )
            }

            Spacer(modifier = Modifier.height(Dimens.SpacingMd))

            // Title
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = OnSurfaceLight,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(Dimens.SpacingXs))

            // Dynamic Subtitle (max 2 lines)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = PrimaryIndigoLight,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(Dimens.SpacingLg))

            // Fixed-height input container holding either PinCells or Admin Password LojiaTextField
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Dimens.VerifyInputSlotMinHeight),
                contentAlignment = Alignment.Center
            ) {
                if (!isPasswordMode) {
                    PinCells(
                        value = pin,
                        onValueChange = { digits ->
                            if (lockoutSeconds == 0 && !isVerifying && !isSuccess) {
                                pin = digits
                                if (digits.isNotEmpty()) {
                                    errorMessage = null
                                    isError = false
                                }
                            }
                        },
                        masked = true,
                        state = when {
                            isSuccess -> PinCellsState.Success
                            isError -> PinCellsState.Error
                            lockoutSeconds > 0 || isVerifying -> PinCellsState.Locked
                            else -> PinCellsState.Normal
                        },
                        autoFocus = true,
                        focusRequester = pinFocusRequester,
                        onDone = {
                            if (pin.length == AuthPinConstants.PIN_LENGTH) {
                                coroutineScope.launch {
                                    performVerification(pin, passwordMode = false)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset { IntOffset(shakeAnim.value.roundToInt(), 0) }
                    )
                } else {
                    val adminPasswordLabel = stringResource(R.string.admin_password_label)
                    LojiaTextField(
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
                        label = { Text(adminPasswordLabel) },
                        contentDescription = adminPasswordLabel,
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
            }

            // Helper Text
            Text(
                text = if (isPasswordMode) {
                    stringResource(R.string.auth_enter_admin_password_hint)
                } else {
                    stringResource(R.string.auth_enter_admin_pin_hint)
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (isError) ErrorRedLight else OnSurfaceVariantLight,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = Dimens.SpacingSm)
            )

            // Compact Plain Info Note below field
            Text(
                text = stringResource(R.string.auth_info_banner),
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariantLight,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = Dimens.SpacingXs)
            )

            // Instant Green Success Card
            if (isSuccess) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Dimens.SpacingMd)
                        .depthSemanticCard(
                            backgroundColor = SuccessContainer,
                            borderColor = AuthSuccessMint,
                            shape = RoundedCornerShape(Dimens.RadiusSm)
                        )
                ) {
                    Row(
                        modifier = Modifier.padding(Dimens.SpacingMd),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SuccessTextDark,
                            modifier = Modifier.size(Dimens.IconXs)
                        )
                        Spacer(modifier = Modifier.width(Dimens.SpacingSm))
                        Text(
                            text = stringResource(R.string.auth_pin_access_granted),
                            style = MaterialTheme.typography.bodySmall,
                            color = SuccessTextDark,
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
                        .padding(top = Dimens.SpacingMd)
                        .depthSemanticCard(
                            backgroundColor = NegativeBannerSurface,
                            borderColor = NegativeBannerBorder,
                            shape = RoundedCornerShape(Dimens.RadiusSm)
                        )
                ) {
                    Row(
                        modifier = Modifier.padding(Dimens.SpacingMd),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = ErrorRedLight,
                            modifier = Modifier.size(Dimens.IconXs)
                        )
                        Spacer(modifier = Modifier.width(Dimens.SpacingSm))
                        Text(
                            text = displayError,
                            style = MaterialTheme.typography.bodySmall,
                            color = ErrorRedLight,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimens.SpacingSm))

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
                    fontSize = DepthTypeTokens.AuthFooterTextSize,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(Dimens.SpacingSm))

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
                        .minimumInteractiveComponentSize()
                        .heightIn(min = Dimens.ButtonHeightStandard)
                        .depthSecondaryButton(
                            cornerRadius = Dimens.RadiusMd,
                            minHeight = Dimens.ButtonHeightStandard
                        )
                ) {
                    Text(
                        text = stringResource(R.string.cancel),
                        fontSize = DepthTypeTokens.SecondaryButtonTextSize,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
                        .minimumInteractiveComponentSize()
                        .heightIn(min = Dimens.ButtonHeightStandard)
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
                                    minHeight = Dimens.ButtonHeightStandard
                                )
                            }
                        )
                ) {
                    when {
                        isVerifying -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(Dimens.IconXs),
                                strokeWidth = Dimens.PinCursorWidth,
                                color = PureWhite
                            )
                            Spacer(modifier = Modifier.width(Dimens.SpacingXs))
                            Text(
                                text = stringResource(R.string.verifying_label),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        isSuccess -> {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(Dimens.IconSm)
                            )
                            Spacer(modifier = Modifier.width(Dimens.SpacingXs))
                            Text(
                                text = stringResource(R.string.confirmed_label),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        isError -> {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                modifier = Modifier.size(Dimens.IconSm)
                            )
                            Spacer(modifier = Modifier.width(Dimens.SpacingXs))
                            Text(
                                text = stringResource(R.string.retry_label),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        isPasswordMode || isPinComplete -> {
                            Text(
                                text = stringResource(R.string.confirm),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        else -> {
                            Text(
                                text = stringResource(R.string.auth_enter_more_format, AuthPinConstants.PIN_LENGTH - pin.length),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
