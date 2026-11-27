package com.lojia.shiftreport.ui.common



import com.lojia.shiftreport.ui.common.LojiaTextField
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext


import com.lojia.shiftreport.R
import com.lojia.shiftreport.util.SecurityUtils
import com.lojia.shiftreport.util.isPinUsable




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
import androidx.compose.material.icons.outlined.ClearAll


import androidx.compose.material.icons.filled.*
import androidx.compose.ui.semantics.clearAndSetSemantics


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
    title: String = stringResource(R.string.delete_shift_report_title),
    itemDescription: String = stringResource(R.string.delete_report_confirm),
    userProfile: UserProfile?,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember(context) { com.lojia.shiftreport.data.PreferencesRepository.getInstance(context) }
    val coroutineScope = rememberCoroutineScope()
    var passwordInput by remember { mutableStateOf("") }
    var answerInput by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val actualPasswordHash = userProfile?.passwordHash.orEmpty()
    val actualQuestion = userProfile?.securityQuestion?.trim().orEmpty()
        .ifBlank { prefs.getRecoveryQuestion().trim() }
    val storedAnswer = userProfile?.securityAnswer?.trim().orEmpty()
        .ifBlank { prefs.getRecoveryAnswerHash().trim() }
    val recoveryConfigured = com.lojia.shiftreport.auth.SecurityRecoveryVerifier.isRecoveryConfigured(
        actualQuestion,
        storedAnswer
    )

    val reportSubtitle = remember(itemDescription, context) {
        Regex("#(\\d+)").find(itemDescription)?.let { context.getString(R.string.shift_report_number_fmt, it.groupValues[1]) }
            ?: itemDescription
    }

    LojiaDialog(
        onDismissRequest = onDismiss,
        size = DialogSize.Medium,
        contentPadding = PaddingValues(Dimens.SpacingNone),
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
                        .padding(horizontal = Dimens.SpacingLg, vertical = Dimens.SpacingLg),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Warning icon in circle
                    Box(
                        modifier = Modifier
                            .size(Dimens.LogoTileSize)
                            .clip(CircleShape)
                            .background(ErrorContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteForever,
                            contentDescription = null,
                            tint = ErrorRedLight,
                            modifier = Modifier.size(Dimens.IconLg)
                        )
                    }
                    Spacer(modifier = Modifier.height(Dimens.SpacingMd))
                    Text(
                        text = stringResource(R.string.delete_shift_report_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceLight,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(Dimens.SpacingXs))
                    Text(
                        text = reportSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = PrimaryIndigoDark,
                        textAlign = TextAlign.Center
                    )
                }

                HorizontalDivider(thickness = Dimens.RowDividerWidth, color = OutlineVariantLight)

                // Body block
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.SpacingLg, vertical = Dimens.SpacingLg)
                ) {
                    // Warning banner
                    Surface(
                        shape = RoundedCornerShape(Dimens.RadiusLg),
                        color = ErrorContainer.copy(alpha = 0.6f),
                        border = BorderStroke(Dimens.FlatBorderWidth, ErrorRedLight.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Dimens.CardPadding),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                tint = ErrorRedLight,
                                modifier = Modifier.size(Dimens.IconSm)
                            )
                            Spacer(modifier = Modifier.width(Dimens.SpacingSm))
                            Text(
                                text = stringResource(R.string.delete_shift_report_confirm_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnSurfaceLight
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(Dimens.SpacingLg))

                    Text(
                        text = stringResource(R.string.security_question_label),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = TextHintColor
                    )
                    Spacer(modifier = Modifier.height(Dimens.SpacingXs))

                    // Question
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(Dimens.RadiusMd),
                        color = SurfaceVariantLight
                    ) {
                        Text(
                            text = if (recoveryConfigured) {
                                actualQuestion
                            } else {
                                stringResource(R.string.auth_recovery_unavailable)
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = PrimaryIndigoDark,
                            modifier = Modifier.padding(Dimens.CardPadding)
                        )
                    }
                    Spacer(modifier = Modifier.height(Dimens.SpacingLg))

                    // Password field
                    LojiaTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it; errorMsg = null },
                        label = { Text(stringResource(R.string.password), style = MaterialTheme.typography.bodyMedium) },
                        placeholder = { Text(stringResource(R.string.auth_ph_password), style = MaterialTheme.typography.bodyMedium) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = OnSurfaceVariantLight,
                                modifier = Modifier.size(Dimens.IconField)
                            )
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("delete_auth_password_input"),
                        shape = RoundedCornerShape(Dimens.RadiusMd)
                    )
                    Spacer(modifier = Modifier.height(Dimens.SpacingMd))

                    // Security answer field
                    LojiaTextField(
                        value = answerInput,
                        onValueChange = { answerInput = it; errorMsg = null },
                        label = { Text(stringResource(R.string.title_security_answer), style = MaterialTheme.typography.bodyMedium) },
                        placeholder = { Text(stringResource(R.string.title_security_answer), style = MaterialTheme.typography.bodyMedium) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.HelpOutline,
                                contentDescription = null,
                                tint = OnSurfaceVariantLight,
                                modifier = Modifier.size(Dimens.IconField)
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("delete_auth_answer_input"),
                        shape = RoundedCornerShape(Dimens.RadiusMd)
                    )

                    // Error message (if any)
                    if (errorMsg != null) {
                        Spacer(modifier = Modifier.height(Dimens.SpacingSm))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.ErrorOutline,
                                contentDescription = null,
                                tint = ErrorRedLight,
                                modifier = Modifier.size(Dimens.IconXs)
                            )
                            Spacer(modifier = Modifier.width(Dimens.SpacingXs))
                            Text(
                                text = errorMsg!!,
                                style = MaterialTheme.typography.bodySmall,
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
                        .padding(horizontal = Dimens.SpacingLg, vertical = Dimens.SpacingMd),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)
                ) {
                    // Cancel — raised secondary
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = Dimens.MinTouchTarget)
                            .depthSecondaryButton(
                                borderColor = PrimaryIndigoLight,
                                cornerRadius = Dimens.RadiusMd
                            ),
                        shape = RoundedCornerShape(Dimens.RadiusMd),
                        border = null,
                        elevation = null,
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = PrimaryIndigoLight
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.cancel),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryIndigoLight
                        )
                    }

                    // Delete — filled raised destructive
                    Button(
                        onClick = {
                            if (!recoveryConfigured) {
                                errorMsg = context.getString(R.string.auth_recovery_unavailable)
                                return@Button
                            }
                            val passValid = isPinUsable(passwordInput) &&
                                isPinUsable(actualPasswordHash) &&
                                SecurityUtils.verifySecret(passwordInput, actualPasswordHash)
                            val ansResult = com.lojia.shiftreport.auth.SecurityRecoveryVerifier.verifyAnswerWithUpgrade(
                                enteredAnswer = answerInput,
                                storedAnswerOrHash = storedAnswer
                            )
                            if (passValid && ansResult.isMatch) {
                                if (ansResult.newHashToStore != null) {
                                    prefs.saveRecoveryAnswerHash(ansResult.newHashToStore)
                                    coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                        try {
                                            val db = com.lojia.shiftreport.data.AppDatabase.getInstance(context)
                                            val existing = db.reportDao().getUserProfileOnce()
                                            if (existing != null) {
                                                db.reportDao().saveUserProfile(
                                                    existing.copy(securityAnswer = ansResult.newHashToStore)
                                                )
                                            }
                                        } catch (_: Exception) {}
                                    }
                                }
                                onConfirmDelete()
                                onDismiss()
                            } else {
                                errorMsg = context.getString(R.string.wrong_password_or_security_answer)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = Dimens.MinTouchTarget)
                            .depthRaisedButton(
                                enabled = true,
                                isDestructive = true,
                                cornerRadius = Dimens.RadiusMd
                            )
                            .testTag("confirm_secure_delete_btn"),
                        shape = RoundedCornerShape(Dimens.RadiusMd),
                        elevation = null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = PureWhite
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = null,
                            tint = PureWhite,
                            modifier = Modifier.size(Dimens.IconSm)
                        )
                        Spacer(modifier = Modifier.width(Dimens.SpacingXs))
                        Text(
                            text = stringResource(R.string.delete),
                            style = MaterialTheme.typography.labelLarge,
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
    val cardShape = RoundedCornerShape(Dimens.RadiusLg)
    val usesStandardSurface = backgroundColor == MaterialTheme.colorScheme.surface ||
        backgroundColor == SurfaceLight ||
        backgroundColor == PureWhite
    val styledModifier = modifier
        .fillMaxWidth()
        .then(
            if (usesStandardSurface) {
                Modifier.depthCard(shape = cardShape)
            } else {
                Modifier.depthSemanticCard(
                    backgroundColor = backgroundColor,
                    borderColor = borderColor,
                    shape = cardShape
                )
            }
        )

    val finalModifier = if (onClick != null) {
        styledModifier.clickable { onClick() }
    } else {
        styledModifier
    }

    Column(
        modifier = finalModifier.padding(horizontal = Dimens.CardPadding, vertical = Dimens.SpacingSm),
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
    val cardShape = RoundedCornerShape(Dimens.RadiusMd)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .depthCard(shape = cardShape),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.SpacingNone),
        border = null,
        shape = cardShape
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.CardPadding, vertical = Dimens.SpacingSm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(Dimens.TextFieldHeightCompact)
                    .depthInset(shape = RoundedCornerShape(Dimens.RadiusKpiCard)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(Dimens.IconAction)
                )
            }
            Spacer(modifier = Modifier.width(Dimens.SpacingMd))
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
                    val normalizedInput = input.replace('\u060C', ',')
                    var hasDot = false
                    buildString {
                        for (char in normalizedInput) {
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
                { Text(suffixText, style = MaterialTheme.typography.labelLarge, color = TextSecondaryLight, modifier = Modifier.padding(end = Dimens.SpacingMd)) }
            } else null,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            isError = isError,
            singleLine = true,
            shape = RoundedCornerShape(Dimens.RadiusLg),
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
                modifier = Modifier.padding(start = Dimens.SpacingMd, top = Dimens.SpacingXs)
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
            .padding(vertical = Dimens.SpacingSm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = PrimaryIndigo,
                    modifier = Modifier.size(Dimens.IconAction)
                )
                Spacer(modifier = Modifier.width(Dimens.SpacingSm))
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

@Suppress("UNUSED_PARAMETER")
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
    com.lojia.shiftreport.auth.AdminAuthDialog(
        onDismissRequest = onDismiss,
        onAuthSuccess = onSuccess,
        title = title ?: stringResource(R.string.confirm_pin_number),
        subtitle = subtitle ?: stringResource(R.string.auth_confirm_action)
    )
}

@Composable
fun LoyverseMenuItemRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    tileColors: List<Color> = listOf(PureWhite, BadgeBlueBg),
    iconTint: Color = PrimaryBlueLight,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit = {}
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = Dimens.MinTouchTarget)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = Dimens.ReportCardInnerPadding,
                    vertical = Dimens.ListRowPaddingVertical
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(Dimens.SettingsIconTileSize)
                    .depthRaisedIconTile(
                        gradientColors = tileColors,
                        cornerRadius = Dimens.RadiusSettingsIconTile
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(Dimens.IconField)
                )
            }
            Spacer(modifier = Modifier.width(Dimens.SettingsIconTitleGap))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = DepthTypeTokens.ListRowTitleSize,
                    color = TextPrimaryReport,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(Dimens.SpacingXxs))
                    Text(
                        text = subtitle,
                        fontSize = DepthTypeTokens.ListRowSubtitleSize,
                        color = OnSurfaceVariantLight,
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
                    tint = OnSurfaceVariantLight,
                    modifier = Modifier.size(Dimens.IconField)
                )
            }
        }
    }
}




