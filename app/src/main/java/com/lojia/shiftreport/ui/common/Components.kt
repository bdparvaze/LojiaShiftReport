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


import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.lojia.shiftreport.ui.theme.Dimens
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
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.SwapHoriz
import kotlinx.coroutines.launch
import com.lojia.shiftreport.data.AppModule
import com.lojia.shiftreport.ui.theme.*

import com.lojia.shiftreport.data.UserProfile


import androidx.compose.ui.text.input.PasswordVisualTransformation

@Composable
fun SecureDeleteModal(
    title: String = "Delete Shift Report",
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

    val reportSubtitle = remember(itemDescription) {
        Regex("#(\\d+)").find(itemDescription)?.let { "Shift Report #${it.groupValues[1]}" }
            ?: itemDescription
    }

    LojiaDialog(
        onDismissRequest = onDismiss,
        maxWidth = Dimens.DialogMaxWidth,
        shape = RoundedCornerShape(Dimens.DialogCornerRadius),
        containerColor = SurfaceLight
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            // Header block (top of dialog)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Warning icon in circle
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(ErrorContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteForever,
                            contentDescription = null,
                            tint = ErrorRedLight,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Delete Shift Report",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceLight,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = reportSubtitle,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = PrimaryIndigoDark,
                        textAlign = TextAlign.Center
                    )
                }

                HorizontalDivider(thickness = 0.5.dp, color = OutlineVariantLight)

                // Body block
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    // Warning banner
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = ErrorContainer.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, ErrorRedLight.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                tint = ErrorRedLight,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "This action cannot be undone. Enter your security credentials to confirm.",
                                fontSize = 12.sp,
                                color = OnSurfaceLight,
                                lineHeight = 16.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // Security question label (English only — no bilingual mixing)
                    Text(
                        text = "Security Question",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextHintColor,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // Question
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = SurfaceVariantLight
                    ) {
                        Text(
                            text = actualQuestion,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = PrimaryIndigoDark,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // Password field
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it; errorMsg = null },
                        label = { Text(stringResource(R.string.password), fontSize = 12.sp, color = OnSurfaceVariantLight) },
                        placeholder = { Text(stringResource(R.string.auth_ph_password), fontSize = 12.sp, color = TextHintColor) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = OnSurfaceVariantLight,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("delete_auth_password_input"),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryIndigoLight,
                            unfocusedBorderColor = OutlineLight,
                            focusedContainerColor = SurfaceLight,
                            unfocusedContainerColor = SurfaceLight,
                            cursorColor = PrimaryIndigoLight
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Security answer field
                    OutlinedTextField(
                        value = answerInput,
                        onValueChange = { answerInput = it; errorMsg = null },
                        label = { Text(stringResource(R.string.title_security_answer), fontSize = 12.sp, color = OnSurfaceVariantLight) },
                        placeholder = { Text(stringResource(R.string.title_security_answer), fontSize = 12.sp, color = TextHintColor) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.HelpOutline,
                                contentDescription = null,
                                tint = OnSurfaceVariantLight,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("delete_auth_answer_input"),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryIndigoLight,
                            unfocusedBorderColor = OutlineLight,
                            focusedContainerColor = SurfaceLight,
                            unfocusedContainerColor = SurfaceLight,
                            cursorColor = PrimaryIndigoLight
                        )
                    )

                    // Error message (if any)
                    if (errorMsg != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.ErrorOutline,
                                contentDescription = null,
                                tint = ErrorRedLight,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = errorMsg!!,
                                fontSize = 11.sp,
                                color = ErrorRedLight,
                                maxLines = 2
                            )
                        }
                    }
                }

                // Footer (buttons)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Cancel — outlined
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(Dimens.ButtonHeightStandard),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, PrimaryIndigoLight),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = PrimaryIndigoLight
                        )
                    ) {
                        Text(
                            text = "Cancel",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryIndigoLight
                        )
                    }

                    // Delete — filled error
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
                        modifier = Modifier
                            .weight(1f)
                            .height(Dimens.ButtonHeightStandard)
                            .testTag("confirm_secure_delete_btn"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ErrorRedLight,
                            contentColor = PureWhite
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = null,
                            tint = PureWhite,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Delete",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PureWhite
                        )
                    }
                }
            }
        }
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
    val targetBadgeBg = PrimaryContainerLight

    LojiaDialog(
        onDismissRequest = onDismiss,
        maxWidth = 380.dp,
        shape = RoundedCornerShape(Dimens.RadiusXl),
        containerColor = PureWhite,
        border = BorderStroke(1.dp, OutlineVariantLight)
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
                        .background(SurfaceVariantLight, CircleShape)
                        .testTag("btn_close_pin_modal")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.close),
                        tint = TextHintColor,
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
                                tint = PureWhite,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Primary title
                    if (title != null) {
                        Text(
                            text = title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = OnBackgroundLight,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.confirm_pin_number),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = OnBackgroundLight,
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
                                Text(
                                    text = stringResource(R.string.switch_to),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextHintColor
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = targetIcon,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(targetNameRes),
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor
                                )
                            }
                        }
                    } else if (!subtitle.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
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
                                    isError -> ErrorContainerLight
                                    isFilled -> accentColor
                                    isActive -> accentColor.copy(alpha = 0.15f)
                                    else -> SurfaceVariantLight
                                },
                                label = "modal_dot_bg"
                            )

                            val dotBorderColor by animateColorAsState(
                                targetValue = when {
                                    isError -> ErrorRedLight
                                    isFilled -> accentColor
                                    isActive -> accentColor
                                    else -> OutlineLight
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
                                tint = ErrorRedLight,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = errorMsg ?: "",
                                color = ErrorRedLight,
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
                                        item.isClear -> ErrorContainerLight.copy(alpha = 0.5f)
                                        item.isDelete -> BackgroundLight
                                        else -> BackgroundLight
                                    }
                                    val borderColor = when {
                                        item.isClear -> ErrorRedLight.copy(alpha = 0.3f)
                                        item.isDelete -> OutlineVariantLight
                                        else -> OutlineVariantLight
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
                                                        tint = OnSurfaceVariantLight,
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                }
                                                item.isClear -> {
                                                    Text(
                                                        text = item.key,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 19.sp,
                                                        color = ErrorRedLight
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
                                                            color = OnBackgroundLight
                                                        )
                                                        if (item.subText.isNotEmpty()) {
                                                            Text(
                                                                text = item.subText,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 8.5.sp,
                                                                letterSpacing = 1.sp,
                                                                color = TextHintColor
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
                        Text(
                            text = stringResource(R.string.cancel_18),
                            color = TextHintColor,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium
                        )
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
                    .background(SurfaceVariantLight, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = OnSurfaceVariantLight,
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
                    color = OnBackgroundLight,
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
                        color = TextHintColor,
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
                    tint = TextHintColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}




