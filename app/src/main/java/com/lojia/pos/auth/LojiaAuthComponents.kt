package com.lojia.pos.auth

import com.lojia.pos.R
import com.lojia.pos.data.*
import com.lojia.pos.util.*
import com.lojia.pos.ui.common.*
import com.lojia.pos.ui.theme.*
import com.lojia.pos.auth.*
import com.lojia.pos.report.*
import com.lojia.pos.settings.*

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun LojiaHeader(
    isBn: Boolean = false,
    modifier: Modifier = Modifier,
    headerHeight: Dp? = null,
    onLanguageClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(if (headerHeight != null) Modifier.height(headerHeight) else Modifier.height(260.dp))
            .background(PrimaryBlue)
            .testTag("lojiaHeader"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                // Stylized white 'L' with 'ojia' nested inside
                Box(
                    contentAlignment = Alignment.BottomStart
                ) {
                    Canvas(
                        modifier = Modifier
                            .height(64.dp)
                            .width(92.dp)
                    ) {
                        val strokeW = 12.dp.toPx()
                        // Vertical bar of L
                        drawRect(
                            color = Color.White,
                            topLeft = Offset(0f, 0f),
                            size = Size(strokeW, size.height)
                        )
                        // Bottom horizontal bar of L
                        drawRect(
                            color = Color.White,
                            topLeft = Offset(0f, size.height - strokeW),
                            size = Size(size.width, strokeW)
                        )
                    }

                    Text(
                        text = "ojia",
                        fontFamily = com.lojia.pos.ui.theme.PoppinsFontFamily, fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(start = 16.dp, bottom = 12.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = "system",
                    fontFamily = com.lojia.pos.ui.theme.PoppinsFontFamily, fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFACC15),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "SECURE BUSINESS LEDGER",
                fontFamily = com.lojia.pos.ui.theme.PoppinsFontFamily, fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.95f),
                letterSpacing = 2.sp
            )
        }
    }
}

@Composable
fun LojiaCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(18.dp),
                spotColor = Color.Black.copy(alpha = 0.10f),
                ambientColor = Color.Black.copy(alpha = 0.05f)
            ),
        shape = RoundedCornerShape(18.dp),
        color = LojiaColors.White
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 18.dp),
            content = content
        )
    }
}

@Composable
fun LojiaSectionHeader(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 15.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 13.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(LojiaColors.P100),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = LojiaColors.P500,
                    modifier = Modifier.size(15.dp)
                )
            }
            Spacer(modifier = Modifier.width(9.dp))
            Column {
                Text(
                    text = title,
                    fontFamily = com.lojia.pos.ui.theme.PoppinsFontFamily, fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = LojiaColors.N900
                )
                Text(
                    text = subtitle,
                    fontFamily = com.lojia.pos.ui.theme.PoppinsFontFamily, fontSize = 10.8.sp,
                    color = LojiaColors.N500,
                    modifier = Modifier.padding(top = 1.dp)
                )
            }
        }
        // Border bottom 1px solid #F3F4F6
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(LojiaColors.N100)
        )
    }
}

@Composable
fun LojiaInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    isRequired: Boolean = false,
    isValid: Boolean = false,
    validationState: FieldValidationState = FieldValidationState.DEFAULT,
    errorMessage: String? = null,
    successMessage: String? = null,
    hintMessage: String? = null,
    infoTooltip: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    enabled: Boolean = true,
    testTag: String = ""
) {
    var isFocused by remember { mutableStateOf(false) }
    var showTooltip by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        // Label with Required Asterisk & Tooltip
        if (label.isNotBlank()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 5.dp, start = 2.dp)
            ) {
                Text(
                    text = label,
                    fontFamily = com.lojia.pos.ui.theme.PoppinsFontFamily, fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LojiaColors.N600
                )
                if (isRequired) {
                    Text(
                        text = " *",
                        fontFamily = com.lojia.pos.ui.theme.PoppinsFontFamily, fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isValid) LojiaColors.G500 else LojiaColors.R500
                    )
                }
                if (!infoTooltip.isNullOrBlank()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .size(13.dp)
                            .clip(CircleShape)
                            .background(LojiaColors.N200)
                            .clickable { showTooltip = !showTooltip },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "?",
                            fontFamily = com.lojia.pos.ui.theme.PoppinsFontFamily, fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = LojiaColors.N600
                        )
                    }
                }
            }
        }

        // Info Tooltip Box
        if (showTooltip && !infoTooltip.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(5.dp),
                color = LojiaColors.N900,
                modifier = Modifier
                    .padding(bottom = 5.dp)
                    .clickable { showTooltip = false }
            ) {
                Text(
                    text = infoTooltip,
                    fontFamily = com.lojia.pos.ui.theme.PoppinsFontFamily, fontSize = 10.sp,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // Dynamic Border and Background matching Section 16
        val (borderColor, bgColor) = when (validationState) {
            FieldValidationState.ERROR -> LojiaColors.R500 to LojiaColors.R100
            FieldValidationState.SUCCESS -> LojiaColors.G500 to LojiaColors.OkBg
            FieldValidationState.DEFAULT -> if (isFocused) {
                LojiaColors.P500 to LojiaColors.White
            } else {
                LojiaColors.N300 to LojiaColors.N50
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(bgColor)
                .border(
                    width = 1.5.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(10.dp)
                )
                .padding(horizontal = 11.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = if (isFocused) LojiaColors.P500 else LojiaColors.N400,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(9.dp))
                }

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = Color(0xFF9CA3AF),
                            fontFamily = com.lojia.pos.ui.theme.PoppinsFontFamily, fontSize = 13.5.sp,
                            lineHeight = 18.sp
                        )
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        singleLine = true,
                        enabled = enabled,
                        visualTransformation = visualTransformation,
                        keyboardOptions = keyboardOptions,
                        keyboardActions = keyboardActions,
                        textStyle = TextStyle(
                            color = Color(0xFF111827),
                            fontFamily = com.lojia.pos.ui.theme.PoppinsFontFamily, fontSize = 13.5.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        cursorBrush = SolidColor(Color(0xFF4F3EE8)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { isFocused = it.isFocused }
                            .testTag(testTag)
                    )
                }

                if (trailingIcon != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    trailingIcon()
                }
            }
        }

        // Messages below input
        if (validationState == FieldValidationState.ERROR && !errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage,
                color = Color(0xFFEF4444),
                fontFamily = com.lojia.pos.ui.theme.PoppinsFontFamily, fontSize = 11.sp,
                modifier = Modifier.padding(top = 3.dp, start = 2.dp)
            )
        } else if (validationState == FieldValidationState.SUCCESS && !successMessage.isNullOrBlank()) {
            Text(
                text = successMessage,
                color = Color(0xFF10B981),
                fontFamily = com.lojia.pos.ui.theme.PoppinsFontFamily, fontSize = 11.sp,
                modifier = Modifier.padding(top = 3.dp, start = 2.dp)
            )
        } else if (!hintMessage.isNullOrBlank()) {
            Text(
                text = hintMessage,
                color = Color(0xFF6B7280),
                fontFamily = com.lojia.pos.ui.theme.PoppinsFontFamily, fontSize = 11.sp,
                modifier = Modifier.padding(top = 3.dp, start = 2.dp)
            )
        }
    }
}

@Composable
fun LojiaGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    testTag: String = ""
) {
    val buttonBrush = if (enabled) {
        Brush.linearGradient(
            colors = listOf(LojiaColors.P700, LojiaColors.P500),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    } else {
        SolidColor(LojiaColors.P500.copy(alpha = 0.45f))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .shadow(
                elevation = if (enabled) 4.dp else 0.dp,
                shape = RoundedCornerShape(10.dp),
                spotColor = LojiaColors.P500.copy(alpha = 0.26f)
            )
            .clip(RoundedCornerShape(10.dp))
            .background(buttonBrush)
            .clickable(enabled = enabled && !isLoading, onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = text,
                    color = Color.White,
                    fontFamily = com.lojia.pos.ui.theme.PoppinsFontFamily, fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.20f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(11.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = text,
                    color = Color.White,
                    fontFamily = com.lojia.pos.ui.theme.PoppinsFontFamily, fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.4.sp
                )
            }
        }
    }
}

@Composable
fun LojiaPasswordStrengthMeter(
    password: String,
    isBn: Boolean
) {
    if (password.isEmpty()) return

    val hasLen = password.length >= 8
    val hasCase = password.any { it.isUpperCase() } && password.any { it.isLowerCase() }
    val hasNum = password.any { it.isDigit() }
    val hasSym = password.any { !it.isLetterOrDigit() }

    var score = 0
    if (hasLen) score++
    if (hasCase) score++
    if (hasNum) score++
    if (hasSym) score++
    score = score.coerceIn(1, 4)

    val (badgeText, badgeColor, badgeBg) = when (score) {
        1 -> Triple(LojiaStrings.get("weak", isBn), LojiaColors.R500, LojiaColors.R100)
        2 -> Triple(LojiaStrings.get("fair", isBn), Color(0xFFF59E0B), Color(0xFFFEF3C7))
        3 -> Triple(LojiaStrings.get("good", isBn), Color(0xFF3B82F6), Color(0xFFDBEAFE))
        else -> Triple(LojiaStrings.get("strong", isBn), LojiaColors.G500, LojiaColors.G100)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
    ) {
        // 4-segment bars
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (i in 1..4) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(if (i <= score) badgeColor else LojiaColors.N300)
                )
            }
        }

        // Strength label & Badge
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = LojiaStrings.get("pwStrengthLabel", isBn),
                fontFamily = com.lojia.pos.ui.theme.PoppinsFontFamily, fontSize = 11.sp,
                color = LojiaColors.N500
            )
            Surface(
                shape = RoundedCornerShape(99.dp),
                color = badgeBg
            ) {
                Text(
                    text = badgeText,
                    fontFamily = com.lojia.pos.ui.theme.PoppinsFontFamily, fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                )
            }
        }

        // Checklist Requirements
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReqIndicator(label = LojiaStrings.get("reqLen", isBn), isMet = hasLen)
            ReqIndicator(label = LojiaStrings.get("reqCase", isBn), isMet = hasCase)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReqIndicator(label = LojiaStrings.get("reqNum", isBn), isMet = hasNum)
            ReqIndicator(label = LojiaStrings.get("reqSym", isBn), isMet = hasSym)
        }
    }
}

@Composable
private fun ReqIndicator(label: String, isMet: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = if (isMet) "✓ " else "✗ ",
            fontFamily = com.lojia.pos.ui.theme.PoppinsFontFamily, fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (isMet) LojiaColors.G500 else LojiaColors.N400
        )
        Text(
            text = label,
            fontFamily = com.lojia.pos.ui.theme.PoppinsFontFamily, fontSize = 10.sp,
            color = if (isMet) LojiaColors.G500 else LojiaColors.N400
        )
    }
}

@Composable
fun LojiaSkeletonLoader() {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            LojiaColors.N200,
            LojiaColors.N100,
            LojiaColors.N200
        ),
        start = Offset(translateAnim - 1000f, 0f),
        end = Offset(translateAnim, 0f)
    )

    LojiaCard(modifier = Modifier.padding(top = 12.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.4f)
                .height(10.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(shimmerBrush)
        )
        Spacer(modifier = Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(shimmerBrush)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(shimmerBrush)
        )
        Spacer(modifier = Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(10.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(shimmerBrush)
        )
    }
}

@Composable
fun LojiaCountryPickerDialog(
    countries: List<LojiaCountry>,
    selectedDial: String,
    onSelectCountry: (LojiaCountry) -> Unit,
    onDismiss: () -> Unit,
    isBn: Boolean
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(searchQuery, countries) {
        if (searchQuery.isBlank()) countries
        else {
            val q = searchQuery.lowercase().trim()
            countries.filter {
                it.name.lowercase().contains(q) ||
                        it.dial.contains(q) ||
                        it.code.lowercase().contains(q)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = LojiaColors.White,
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.75f)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header & Search
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isBn) "দেশ ও কোড নির্বাচন করুন" else "Select Country & Dial Code",
                        fontFamily = com.lojia.pos.ui.theme.PoppinsFontFamily, fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = LojiaColors.N900
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = LojiaColors.N500
                        )
                    }
                }

                // Search Input
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(LojiaColors.N50)
                        .border(1.dp, LojiaColors.N200, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = LojiaColors.N400,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = TextStyle(color = LojiaColors.N900, fontFamily = com.lojia.pos.ui.theme.PoppinsFontFamily, fontSize = 13.sp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                HorizontalDivider(
                    color = LojiaColors.N100,
                    modifier = Modifier.padding(top = 10.dp)
                )

                // List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(filtered, key = { it.code + it.dial }) { country ->
                        val isSelected = country.dial == selectedDial
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectCountry(country)
                                }
                                .background(if (isSelected) LojiaColors.P100 else Color.Transparent)
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = country.flag, fontFamily = com.lojia.pos.ui.theme.PoppinsFontFamily, fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = country.dial,
                                fontFamily = com.lojia.pos.ui.theme.PoppinsFontFamily, fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = LojiaColors.N900,
                                modifier = Modifier.width(52.dp)
                            )
                            Text(
                                text = country.name,
                                fontFamily = com.lojia.pos.ui.theme.PoppinsFontFamily, fontSize = 13.sp,
                                color = LojiaColors.N700,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = LojiaColors.P500,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        HorizontalDivider(color = LojiaColors.N100, thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}
