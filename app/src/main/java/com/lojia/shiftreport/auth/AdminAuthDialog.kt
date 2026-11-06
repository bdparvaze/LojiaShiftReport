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
import com.lojia.shiftreport.data.AppDatabase
import com.lojia.shiftreport.util.SecurityUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val DialogBlue = Color(0xFF2563EB)
private val DialogGreen = Color(0xFF10B981)
private val DialogLightGray = Color(0xFFE5E7EB)
private val DialogBoxEmptyBg = Color(0xFFF9FAFB)
private val DialogTextBlack = Color(0xFF111827)

@Composable
fun AdminAuthDialog(
    onDismissRequest: () -> Unit,
    onAuthSuccess: () -> Unit,
    title: String = "Admin Confirmation",
    subtitle: String = "Confirm Action"
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

        val isValid = try {
            val profile = db.reportDao().getUserProfileOnce()
            val pinMatched = if (!passwordMode) {
                SecureStorageManager.verifyQuickPin(context, input) ||
                    (profile?.pin?.isNotBlank() == true &&
                        SecurityUtils.verifySecretWithUpgrade(input, profile.pin).isMatch)
            } else {
                false
            }
            val passwordMatched = profile?.passwordHash?.isNotBlank() == true &&
                SecurityUtils.verifySecretWithUpgrade(input, profile.passwordHash).isMatch

            pinMatched || passwordMatched
        } catch (_: Exception) {
            false
        }

        if (isValid) {
            isVerifying = false
            isSuccess = true
            isError = false
            errorMessage = null
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            delay(500L)
            Toast.makeText(context, "✓ PIN Confirmed", Toast.LENGTH_SHORT).show()
            onAuthSuccess()
        } else {
            attempts += 1
            isError = true
            isSuccess = false
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            triggerShake()
            if (attempts >= AuthPinConstants.MAX_ATTEMPTS) {
                lockoutSeconds = AuthPinConstants.LOCKOUT_SECONDS
                errorMessage = "Too many attempts. Try again in ${lockoutSeconds}s"
            } else {
                val left = AuthPinConstants.MAX_ATTEMPTS - attempts
                errorMessage = if (passwordMode) {
                    "❌ Invalid Admin PIN or Password ($left left)"
                } else {
                    "❌ Wrong PIN - Try again ($left left)"
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
                .background(Color.Black.copy(alpha = 0.60f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismissRequest
                ),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .widthIn(max = 420.dp)
                    .wrapContentHeight()
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
                            .background(DialogBlue.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = DialogBlue,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Title
                    Text(
                        text = title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = DialogTextBlack,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Dynamic Subtitle
                    Text(
                        text = subtitle,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DialogBlue,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Info Card
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DialogBlue.copy(alpha = 0.08f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = DialogBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Admin authentication required to continue. This action will be logged.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF4B5563)
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
                                    val borderColor = when {
                                        isSuccess -> DialogGreen
                                        isError -> Color.Red
                                        isFilled || isFocused -> DialogBlue
                                        else -> DialogLightGray
                                    }
                                    val borderWidth = if (isSuccess || isError || isFilled || isFocused) 2.dp else 1.5.dp

                                    Box(
                                        modifier = Modifier
                                            .size(width = 44.dp, height = 56.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isFilled) Color.White else DialogBoxEmptyBg)
                                            .border(
                                                BorderStroke(borderWidth, borderColor),
                                                RoundedCornerShape(12.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isFilled) {
                                            Text(
                                                text = "●",
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = DialogTextBlack
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Admin Password Field
                        OutlinedTextField(
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
                            label = { Text("Admin Password") },
                            singleLine = true,
                            isError = isError,
                            visualTransformation = if (passwordVisible) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) {
                                            Icons.Default.VisibilityOff
                                        } else {
                                            Icons.Default.Visibility
                                        },
                                        contentDescription = if (passwordVisible) {
                                            "Hide password"
                                        } else {
                                            "Show password"
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
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset { IntOffset(shakeAnim.value.roundToInt(), 0) }
                        )
                    }

                    // Helper / Attempts Indicator
                    Text(
                        text = if (isPasswordMode) {
                            "Enter Admin Password (min 6 chars)"
                        } else {
                            "Enter 6-digit Admin PIN"
                        },
                        fontSize = 12.sp,
                        color = if (isError) Color.Red else Color.Gray,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    // Instant Green Success Card
                    if (isSuccess) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFD1FAE5)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF065F46),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "✓ PIN Confirmed - Access Granted",
                                    color = Color(0xFF065F46),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Instant Red Error Card
                    val displayError = if (lockoutSeconds > 0) {
                        "Too many attempts. Try again in ${lockoutSeconds}s"
                    } else {
                        errorMessage
                    }
                    if (displayError != null && !isSuccess) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = null,
                                    tint = Color.Red,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = displayError,
                                    color = Color.Red,
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
                        }
                    ) {
                        Text(
                            text = if (isPasswordMode) {
                                "Or use 6-digit Admin PIN"
                            } else {
                                "Or use Admin Password"
                            },
                            color = DialogBlue,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Bottom Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismissRequest) {
                            Text(
                                text = "Cancel",
                                color = Color(0xFF6B7280),
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val input = if (isPasswordMode) password else pin
                                    performVerification(input, passwordMode = isPasswordMode)
                                }
                            },
                            enabled = isConfirmEnabled,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = when {
                                    isSuccess -> DialogGreen
                                    isError -> Color.Red
                                    isVerifying || isConfirmEnabled -> DialogBlue
                                    else -> DialogLightGray
                                },
                                contentColor = if (isSuccess || isError || isVerifying || isConfirmEnabled) {
                                    Color.White
                                } else {
                                    Color(0xFF9CA3AF)
                                },
                                disabledContainerColor = if (isVerifying) DialogBlue else DialogLightGray,
                                disabledContentColor = if (isVerifying) Color.White else Color(0xFF9CA3AF)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .widthIn(min = 140.dp)
                        ) {
                            when {
                                isVerifying -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Verifying...", fontWeight = FontWeight.Bold)
                                }
                                isSuccess -> {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Confirmed ✓", fontWeight = FontWeight.Bold)
                                }
                                isError -> {
                                    Icon(
                                        imageVector = Icons.Default.Error,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Retry", fontWeight = FontWeight.Bold)
                                }
                                isPasswordMode || isPinComplete -> {
                                    Text("Confirm", fontWeight = FontWeight.Bold)
                                }
                                else -> {
                                    Text(
                                        text = "Enter ${AuthPinConstants.PIN_LENGTH - pin.length} more",
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
