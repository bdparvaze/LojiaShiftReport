package com.lojia.shiftreport.ui.common



import com.lojia.shiftreport.ui.common.LojiaTextField
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext


import com.lojia.shiftreport.R
import com.lojia.shiftreport.util.SecurityUtils




import androidx.compose.animation.AnimatedVisibility


import androidx.compose.animation.animateColorAsState


import androidx.compose.animation.core.animateDpAsState


import androidx.compose.foundation.BorderStroke


import androidx.compose.foundation.background


import androidx.compose.foundation.border


import androidx.compose.foundation.clickable


import androidx.compose.foundation.layout.*


import androidx.compose.foundation.shape.CircleShape


import androidx.compose.foundation.shape.RoundedCornerShape


import androidx.compose.foundation.text.KeyboardOptions


import androidx.compose.material.icons.Icons


import androidx.compose.material.icons.automirrored.outlined.Backspace


import androidx.compose.material.icons.filled.*


import androidx.compose.material3.*


import androidx.compose.runtime.*


import androidx.compose.ui.Alignment


import androidx.compose.ui.Modifier


import androidx.compose.ui.draw.clip


import androidx.compose.ui.graphics.Color


import androidx.compose.ui.graphics.vector.ImageVector


import androidx.compose.ui.platform.testTag


import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow


import androidx.compose.ui.text.style.TextAlign


import androidx.compose.ui.unit.dp


import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.SwapHoriz
import kotlinx.coroutines.launch
import com.lojia.shiftreport.data.AppModule
import com.lojia.shiftreport.ui.theme.*

import com.lojia.shiftreport.data.UserProfile


import androidx.compose.ui.text.input.PasswordVisualTransformation

@Composable
fun SecureDeleteModal(
    title: String = stringResource(R.string.delete),
    itemDescription: String = stringResource(R.string.delete_report_confirm),
    userProfile: UserProfile?,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    val context = LocalContext.current
    var passwordInput by remember { mutableStateOf("") }
    var answerInput by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val actualPasswordHash = userProfile?.passwordHash.orEmpty()
    val defaultQuestion = stringResource(R.string.default_security_question)
    val actualQuestion = userProfile?.securityQuestion?.ifBlank { defaultQuestion } ?: defaultQuestion
    val actualAnswer = userProfile?.securityAnswer?.ifBlank { "Lojia" } ?: "Lojia"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFEE2E2),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF991B1B))
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = itemDescription,
                    fontSize = 12.sp,
                    color = TextSecondaryLight
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Security Question Box
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = stringResource(R.string.security_question),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryIndigo
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = actualQuestion,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ShiftColors.Charcoal
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LojiaTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it; errorMsg = null },
                    label = { Text(stringResource(R.string.password_1)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("delete_auth_password_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                LojiaTextField(
                    value = answerInput,
                    onValueChange = { answerInput = it; errorMsg = null },
                    label = { Text(stringResource(R.string.security_answer)) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("delete_auth_answer_input")
                )

                if (errorMsg != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMsg!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val passValid = SecurityUtils.verifySecret(passwordInput, actualPasswordHash)
                    val ansValid = answerInput.trim().equals(actualAnswer.trim(), ignoreCase = true)
                    if (passValid && ansValid) {
                        onConfirmDelete()
                        onDismiss()
                    } else {
                        errorMsg = context.getString(R.string.wrong_password_or_security_answer)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                modifier = Modifier.testTag("confirm_secure_delete_btn")
            ) {
                Text(stringResource(R.string.verify_and_delete), color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_18), color = TextSecondaryLight)
            }
        }
    )
}

@Composable
fun SolidCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val baseModifier = modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .background(backgroundColor)
        .border(1.dp, borderColor, RoundedCornerShape(16.dp))

    val finalModifier = if (onClick != null) {
        baseModifier.clickable { onClick() }
    } else {
        baseModifier
    }

    Column(
        modifier = finalModifier.padding(horizontal = 14.dp, vertical = 10.dp),
        content = content
    )
}

@Composable
fun MetricStatCard(
    title: String,
    value: String,
    subtitle: String? = null,
    icon: ImageVector,
    iconColor: Color = PrimaryIndigo,
    iconBgColor: Color = PureWhite,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(PureWhite)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun FormInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    leadingIcon: ImageVector? = null,
    suffixText: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    isError: Boolean = false,
    errorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        LojiaTextField(
            value = value,
            onValueChange = { input ->
                val sanitized = if (keyboardType == KeyboardType.Number) {
                    input.filter { it.isDigit() }
                } else if (keyboardType == KeyboardType.Decimal) {
                    var hasDot = false
                    buildString {
                        for (char in input) {
                            if (char.isDigit()) {
                                append(char)
                            } else if (char == '.' || char == ',') {
                                if (!hasDot) {
                                    append('.')
                                    hasDot = true
                                }
                            }
                        }
                    }
                } else {
                    input
                }
                onValueChange(sanitized)
            },
            label = { Text(label) },
            placeholder = { if (placeholder.isNotBlank()) Text(placeholder) },
            leadingIcon = if (leadingIcon != null) {
                { Icon(leadingIcon, contentDescription = null, tint = PrimaryIndigo) }
            } else null,
            trailingIcon = if (suffixText != null) {
                { Text(suffixText, style = MaterialTheme.typography.labelLarge, color = TextSecondaryLight, modifier = Modifier.padding(end = 12.dp)) }
            } else null,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            isError = isError,
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = PrimaryIndigo,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            modifier = Modifier.fillMaxWidth().testTag("input_$label")
        )
        if (isError && !errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage,
                color = AccentRose,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 12.dp, top = 4.dp)
            )
        }
    }
}

@Composable
fun SectionTitle(
    title: String,
    icon: ImageVector? = null,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = PrimaryIndigo,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        if (action != null) {
            action()
        }
    }
}

private data class SecurityKeypadItem(
    val key: String,
    val subText: String = "",
    val isClear: Boolean = false,
    val isDelete: Boolean = false
)

@Composable
fun SecurityPinModal(
    title: String? = null,
    subtitle: String? = null,
    targetModule: AppModule? = null,
    expectedPin: String = "",
    expectedPassword: String = "",
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val shakeOffset = remember { Animatable(0f) }

    val accentColor = PrimaryIndigo
    val targetBadgeBg = Color(0xFFEFF6FF)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = PureWhite,
            shadowElevation = 16.dp,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier
                .widthIn(max = 380.dp)
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.05f),
                                PureWhite,
                                PureWhite
                            )
                        )
                    )
            ) {
                // Top close button
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 16.dp, end = 16.dp)
                        .size(34.dp)
                        .background(Color(0xFFF1F5F9), CircleShape)
                        .testTag("btn_close_pin_modal")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.close),
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Elevated Dual-Ring Security Emblem
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(accentColor.copy(alpha = 0.12f), CircleShape)
                            .border(2.dp, accentColor.copy(alpha = 0.25f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            accentColor,
                                            accentColor.copy(alpha = 0.85f)
                                        )
                                    )
                                )
                                .shadow(4.dp, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Primary title
                    if (title != null) {
                        AutoText(
                            text = title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color(0xFF0F172A),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        AutoText(
                            id = R.string.confirm_pin_number,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color(0xFF0F172A),
                            textAlign = TextAlign.Center
                        )
                    }

                    // Target Module Transition Badge
                    if (targetModule != null) {
                        val (targetNameRes, targetIcon) = Pair(
                            R.string.shift_report_module,
                            Icons.Outlined.Assessment
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = targetBadgeBg,
                            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.SwapHoriz,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                AutoText(
                                    id = R.string.switch_to,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF64748B)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = targetIcon,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                AutoText(
                                    id = targetNameRes,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor
                                )
                            }
                        }
                    } else if (!subtitle.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        AutoText(
                            text = subtitle,
                            fontSize = 13.sp,
                            color = TextSecondaryLight,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 6-Digit PIN Indicators with smooth animations and error shake
                    Row(
                        modifier = Modifier.offset(x = shakeOffset.value.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 0 until 6) {
                            val isFilled = i < enteredPin.length
                            val isActive = i == enteredPin.length
                            val isError = errorMsg != null

                            val dotSize by animateDpAsState(
                                targetValue = if (isActive) 16.dp else if (isFilled) 15.dp else 12.dp,
                                label = "modal_dot_size"
                            )

                            val dotBgColor by animateColorAsState(
                                targetValue = when {
                                    isError -> Color(0xFFFEE2E2)
                                    isFilled -> accentColor
                                    isActive -> accentColor.copy(alpha = 0.15f)
                                    else -> Color(0xFFF1F5F9)
                                },
                                label = "modal_dot_bg"
                            )

                            val dotBorderColor by animateColorAsState(
                                targetValue = when {
                                    isError -> Color(0xFFEF4444)
                                    isFilled -> accentColor
                                    isActive -> accentColor
                                    else -> Color(0xFFCBD5E1)
                                },
                                label = "modal_dot_border"
                            )

                            Box(
                                modifier = Modifier.size(22.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(dotSize)
                                        .clip(CircleShape)
                                        .background(dotBgColor)
                                        .border(
                                            width = if (isActive || isError) 2.dp else if (isFilled) 0.dp else 1.dp,
                                            color = dotBorderColor,
                                            shape = CircleShape
                                        )
                                )
                            }
                        }
                    }

                    // Error Message
                    if (errorMsg != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            AutoText(
                                text = errorMsg ?: "",
                                color = Color(0xFFEF4444),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    // Standard Ergonomic Numeric Keypad (with Sub-letters ABC, DEF, etc.)
                    val padRows = listOf(
                        listOf(
                            SecurityKeypadItem("1"),
                            SecurityKeypadItem("2", "ABC"),
                            SecurityKeypadItem("3", "DEF")
                        ),
                        listOf(
                            SecurityKeypadItem("4", "GHI"),
                            SecurityKeypadItem("5", "JKL"),
                            SecurityKeypadItem("6", "MNO")
                        ),
                        listOf(
                            SecurityKeypadItem("7", "PQRS"),
                            SecurityKeypadItem("8", "TUV"),
                            SecurityKeypadItem("9", "WXYZ")
                        ),
                        listOf(
                            SecurityKeypadItem("C", isClear = true),
                            SecurityKeypadItem("0"),
                            SecurityKeypadItem("DEL", isDelete = true)
                        )
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        padRows.forEach { rowKeys ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                rowKeys.forEach { item ->
                                    val containerColor = when {
                                        item.isClear -> Color(0xFFFEF2F2)
                                        item.isDelete -> Color(0xFFF8FAFC)
                                        else -> Color(0xFFF8FAFC)
                                    }
                                    val borderColor = when {
                                        item.isClear -> Color(0xFFFECDD3)
                                        item.isDelete -> Color(0xFFE2E8F0)
                                        else -> Color(0xFFE2E8F0)
                                    }

                                    Surface(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            when {
                                                item.isClear -> {
                                                    enteredPin = ""
                                                    errorMsg = null
                                                }
                                                item.isDelete -> {
                                                    if (enteredPin.isNotEmpty()) {
                                                        enteredPin = enteredPin.dropLast(1)
                                                        errorMsg = null
                                                    }
                                                }
                                                else -> {
                                                    if (enteredPin.length < 6) {
                                                        val next = enteredPin + item.key
                                                        enteredPin = next
                                                        if (next.length == 6) {
                                                            if (SecurityUtils.verifySecret(next, expectedPin)) {
                                                                onSuccess()
                                                            } else {
                                                                errorMsg = "Incorrect PIN. Try again."
                                                                enteredPin = ""
                                                                coroutineScope.launch {
                                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                    shakeOffset.animateTo(
                                                                        targetValue = 0f,
                                                                        animationSpec = keyframes {
                                                                            durationMillis = 400
                                                                            0f at 0
                                                                            (-14f) at 50
                                                                            14f at 100
                                                                            (-10f) at 150
                                                                            10f at 200
                                                                            (-6f) at 250
                                                                            6f at 300
                                                                            (-2f) at 350
                                                                            0f at 400
                                                                        }
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                        shape = CircleShape,
                                        color = containerColor,
                                        border = BorderStroke(1.dp, borderColor),
                                        shadowElevation = 0.5.dp,
                                        modifier = Modifier
                                            .size(64.dp)
                                            .testTag("pin_key_${item.key}")
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            when {
                                                item.isDelete -> {
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Outlined.Backspace,
                                                        contentDescription = stringResource(R.string.cd_backspace),
                                                        tint = Color(0xFF334155),
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                }
                                                item.isClear -> {
                                                    Text(
                                                        text = item.key,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 19.sp,
                                                        color = Color(0xFFE11D48)
                                                    )
                                                }
                                                else -> {
                                                    Column(
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        verticalArrangement = Arrangement.Center
                                                    ) {
                                                        Text(
                                                            text = item.key,
                                                            fontWeight = FontWeight.SemiBold,
                                                            fontSize = 21.sp,
                                                            color = Color(0xFF0F172A)
                                                        )
                                                        if (item.subText.isNotEmpty()) {
                                                            Text(
                                                                text = item.subText,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 8.5.sp,
                                                                letterSpacing = 1.sp,
                                                                color = Color(0xFF94A3B8)
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
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Cancel text button
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 42.dp)
                            .testTag("btn_cancel_pin_auth")
                    ) {
                        AutoText(
                            id = R.string.cancel_18,
                            color = Color(0xFF64748B),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LoyverseMenuItemRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit = {}
) {
    Surface(
        onClick = onClick,
        color = PureWhite,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFFF1F5F9), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF475569),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    ),
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp
                        ),
                        color = Color(0xFF64748B),
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
            if (trailing != null) {
                trailing()
            } else {
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}




