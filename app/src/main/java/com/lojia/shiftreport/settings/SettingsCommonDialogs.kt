package com.lojia.shiftreport.settings

import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.lojia.shiftreport.ui.common.LojiaTextField
import com.lojia.shiftreport.R
import com.lojia.shiftreport.data.*
import com.lojia.shiftreport.util.*
import com.lojia.shiftreport.ui.common.*
import com.lojia.shiftreport.ui.theme.*
import com.lojia.shiftreport.auth.*
import com.lojia.shiftreport.report.*
import com.lojia.shiftreport.settings.*



import androidx.compose.ui.res.stringResource






import androidx.compose.foundation.background


import androidx.compose.foundation.clickable


import androidx.compose.foundation.layout.*


import androidx.compose.foundation.shape.CircleShape


import androidx.compose.foundation.shape.RoundedCornerShape


import androidx.compose.material.icons.Icons


import androidx.compose.material.icons.filled.*


import androidx.compose.material.icons.outlined.*


import androidx.compose.material3.*


import androidx.compose.runtime.*


import androidx.compose.ui.Alignment


import androidx.compose.ui.Modifier


import androidx.compose.ui.draw.clip


import androidx.compose.ui.graphics.Color


import androidx.compose.ui.platform.LocalContext


import androidx.compose.ui.platform.testTag


import androidx.compose.foundation.text.KeyboardOptions


import androidx.compose.ui.text.font.FontWeight


import androidx.compose.ui.text.input.KeyboardType


import androidx.compose.ui.text.input.PasswordVisualTransformation


import androidx.compose.ui.unit.dp


import androidx.compose.ui.unit.sp



import com.lojia.shiftreport.util.ExportHelper
import com.lojia.shiftreport.util.NotificationHelper
import com.lojia.shiftreport.util.SecurityUtils
import com.lojia.shiftreport.data.UserProfile

@Composable
fun SettingsCommonDialogs(
    showAdminPinDialog: Boolean,
    onDismissAdminPin: () -> Unit,
    onAdminPinVerified: () -> Unit,
    showTermsDialog: Boolean,
    onDismissTerms: () -> Unit,
    showPrivacyDialog: Boolean,
    onDismissPrivacy: () -> Unit,
    showSupportTicketDialog: Boolean,
    onDismissSupportTicket: () -> Unit,
    showTestPrintDialog: Boolean,
    onDismissTestPrint: () -> Unit,
    userProfile: UserProfile? = null
) {
    val context = LocalContext.current
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    // Admin PIN Verification Dialog
    if (showAdminPinDialog) {
        LojiaDialog(
            onDismissRequest = {
                enteredPin = ""
                pinError = false
                onDismissAdminPin()
            },
            size = DialogSize.Small
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(PrimaryContainerLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = stringResource(R.string.admin_authorization_required),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceLight
                    )
                }

                Column {
                    Text(
                        stringResource(R.string.this_setting_is_protected),
                        fontSize = 13.sp,
                        color = TextSecondaryLight
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Column {
                        Text(stringResource(R.string.msg_admin_pin_required), style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        PinCells(
                            value = enteredPin,
                            onValueChange = {
                                enteredPin = it
                                pinError = false
                            },
                            masked = true,
                            state = if (pinError) PinCellsState.Error else PinCellsState.Normal,
                            autoFocus = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_pin_input")
                        )
                    }
                    if (pinError) {
                        Text(
                            stringResource(R.string.incorrect_pin_try_again),
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }
                }

                val confirmAction = @Composable {
                    Button(
                        onClick = {
                            val expectedPin = userProfile?.pin.orEmpty()
                            if (isPinUsable(enteredPin) && isPinUsable(expectedPin) &&
                                SecurityUtils.verifySecret(enteredPin, expectedPin)
                            ) {
                                enteredPin = ""
                                pinError = false
                                onAdminPinVerified()
                            } else {
                                pinError = true
                            }
                        },
                        shape = RoundedCornerShape(Dimens.RadiusMd),
                        elevation = null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            contentColor = PureWhite,
                            disabledContentColor = OnSurfaceVariantLight
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .depthRaisedButton(cornerRadius = Dimens.RadiusMd)
                            .testTag("admin_pin_confirm")
                    ) {
                        Text(stringResource(R.string.unlock), fontWeight = FontWeight.Bold)
                    }
                }
                val dismissAction = @Composable {
                    OutlinedButton(
                        onClick = {
                            enteredPin = ""
                            pinError = false
                            onDismissAdminPin()
                        },
                        shape = RoundedCornerShape(Dimens.RadiusMd),
                        border = null,
                        elevation = null,
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = OnSurfaceVariantLight
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .depthSecondaryButton(cornerRadius = Dimens.RadiusMd)
                    ) {
                        Text(stringResource(R.string.cancel_18), fontWeight = FontWeight.SemiBold)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) { dismissAction() }
                    Box(modifier = Modifier.weight(1f)) { confirmAction() }
                }
            }
        }
    }

    // Terms & Conditions Dialog
    if (showTermsDialog) {
        LojiaDialog(
            onDismissRequest = onDismissTerms,
            size = DialogSize.Small
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(PrimaryContainerLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Gavel,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = stringResource(R.string.terms_conditions_1),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceLight
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = stringResource(R.string.msg_1_license_usagenlojia_system),
                        fontSize = 13.sp,
                        color = TextPrimaryLight,
                        lineHeight = 18.sp
                    )
                }
                Button(
                    onClick = onDismissTerms,
                    shape = RoundedCornerShape(Dimens.RadiusMd),
                    elevation = null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        contentColor = PureWhite,
                        disabledContentColor = OnSurfaceVariantLight
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .depthRaisedButton(cornerRadius = Dimens.RadiusMd)
                ) {
                    Text(stringResource(R.string.i_understand), fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Privacy Policy Dialog
    if (showPrivacyDialog) {
        LojiaDialog(
            onDismissRequest = onDismissPrivacy,
            size = DialogSize.Small
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(PrimaryContainerLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = stringResource(R.string.privacy_policy_2),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceLight
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = stringResource(R.string.msg_1_data_collectionnlojia_system),
                        fontSize = 13.sp,
                        color = TextPrimaryLight,
                        lineHeight = 18.sp
                    )
                }
                Button(
                    onClick = onDismissPrivacy,
                    shape = RoundedCornerShape(Dimens.RadiusMd),
                    elevation = null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        contentColor = PureWhite,
                        disabledContentColor = OnSurfaceVariantLight
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .depthRaisedButton(cornerRadius = Dimens.RadiusMd)
                ) {
                    Text(stringResource(R.string.close_7), fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Support Ticket Dialog
    if (showSupportTicketDialog) {
        var subject by remember { mutableStateOf("") }
        var details by remember { mutableStateOf("") }
        var ticketSubmitted by remember { mutableStateOf(false) }

        LojiaDialog(
            onDismissRequest = onDismissSupportTicket,
            size = DialogSize.Medium
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.submit_support_ticket),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceLight
                )
                if (ticketSubmitted) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(stringResource(R.string.ticket_tk8892_created), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(stringResource(R.string.our_technical_support_team), fontSize = 13.sp, color = TextSecondaryLight)
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.need_help_with_pos), fontSize = 13.sp, color = TextSecondaryLight)
                        val subjectLabel = stringResource(R.string.subject_eg_printer_setup)
                        LojiaTextField(
                            value = subject,
                            onValueChange = { subject = it },
                            label = { Text(subjectLabel) },
                            contentDescription = subjectLabel,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        val issueDetailsLabel = stringResource(R.string.describe_the_issue)
                        LojiaMultilineTextField(
                            value = details,
                            onValueChange = { details = it },
                            label = { Text(issueDetailsLabel) },
                            contentDescription = issueDetailsLabel,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 100.dp)
                        )
                    }
                }

                val confirmAction = @Composable {
                    if (ticketSubmitted) {
                        Button(
                            onClick = onDismissSupportTicket,
                            shape = RoundedCornerShape(Dimens.RadiusMd),
                            elevation = null,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                contentColor = PureWhite,
                                disabledContentColor = OnSurfaceVariantLight
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .depthRaisedButton(cornerRadius = Dimens.RadiusMd)
                        ) {
                            Text(stringResource(R.string.done_1), fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = {
                                if (subject.isNotBlank()) {
                                    ticketSubmitted = true
                                    NotificationHelper.sendTestPushNotification(
                                        context,
                                        context.getString(R.string.settings_support_ticket_submitted),
                                        context.getString(R.string.settings_support_ticket_received_format, subject)
                                    )
                                }
                            },
                            shape = RoundedCornerShape(Dimens.RadiusMd),
                            elevation = null,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                contentColor = PureWhite,
                                disabledContentColor = OnSurfaceVariantLight
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .depthRaisedButton(cornerRadius = Dimens.RadiusMd)
                        ) {
                            Text(stringResource(R.string.submit_ticket_1), fontWeight = FontWeight.Bold)
                        }
                    }
                }
                val dismissAction = @Composable {
                    if (!ticketSubmitted) {
                        OutlinedButton(
                            onClick = onDismissSupportTicket,
                            shape = RoundedCornerShape(Dimens.RadiusMd),
                            border = null,
                            elevation = null,
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.Transparent,
                                contentColor = OnSurfaceVariantLight
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .depthSecondaryButton(cornerRadius = Dimens.RadiusMd)
                        ) {
                            Text(stringResource(R.string.cancel_18), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!ticketSubmitted) {
                        Box(modifier = Modifier.weight(1f)) { dismissAction() }
                    }
                    Box(modifier = Modifier.weight(1f)) { confirmAction() }
                }
            }
        }
    }

    // Thermal Printer Setup & Test Dialog
    if (showTestPrintDialog) {
        com.lojia.shiftreport.printer.PrinterSetupDialog(
            onDismiss = onDismissTestPrint
        )
    }
}

/**
 * 3-Step Material 3 Quick PIN Dialog conforming to POS & Banking standards.
 * Step 1: Verify current 6-digit PIN (skipped if no PIN exists).
 * Step 2: Enter new 6-digit PIN.
 * Step 3: Confirm new 6-digit PIN.
 */
@Composable
fun ChangeQuickPinDialog(
    preferencesRepository: PreferencesRepository,
    onDismiss: () -> Unit,
    onPinChanged: () -> Unit
) {
    val context = LocalContext.current
    val hasExistingPin = remember {
        preferencesRepository.hasPinConfigured() && !preferencesRepository.getStoredPinHash().isNullOrBlank()
    }

    var step by remember { mutableIntStateOf(if (hasExistingPin) 1 else 2) }
    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    val totalSteps = if (hasExistingPin) 3 else 2
    val displayStep = if (hasExistingPin) step else (step - 1)

    val stepTitle = when (step) {
        1 -> stringResource(R.string.settings_pin_step1_title)
        2 -> stringResource(R.string.settings_pin_step2_title)
        else -> stringResource(R.string.settings_pin_step3_title)
    }

    val stepDesc = when (step) {
        1 -> stringResource(R.string.settings_pin_step1_desc)
        2 -> stringResource(R.string.settings_pin_step2_desc)
        else -> stringResource(R.string.settings_pin_step3_desc)
    }

    LojiaDialog(
        onDismissRequest = onDismiss,
        size = DialogSize.Small
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Pin,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stepTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.settings_step_of_format, displayStep, totalSteps),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stepDesc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                when (step) {
                    1 -> {
                        PinCells(
                            value = oldPin,
                            onValueChange = {
                                oldPin = it
                                errorMessage = null
                                isError = false
                                if (it.length == AuthPinConstants.PIN_LENGTH) {
                                    if (preferencesRepository.verifyPin(it)) {
                                        step = 2
                                        errorMessage = null
                                        isError = false
                                    } else {
                                        isError = true
                                        errorMessage = context.getString(R.string.incorrect_pin_try_again)
                                    }
                                }
                            },
                            masked = true,
                            state = if (isError) PinCellsState.Error else PinCellsState.Normal,
                            autoFocus = true,
                            modifier = Modifier.fillMaxWidth().testTag("pin_step1_input")
                        )
                    }
                    2 -> {
                        PinCells(
                            value = newPin,
                            onValueChange = {
                                newPin = it
                                errorMessage = null
                                isError = false
                                if (it.length == AuthPinConstants.PIN_LENGTH) {
                                    step = 3
                                    errorMessage = null
                                    isError = false
                                }
                            },
                            masked = true,
                            state = if (isError) PinCellsState.Error else PinCellsState.Normal,
                            autoFocus = true,
                            modifier = Modifier.fillMaxWidth().testTag("pin_step2_input")
                        )
                    }
                    3 -> {
                        PinCells(
                            value = confirmPin,
                            onValueChange = {
                                confirmPin = it
                                errorMessage = null
                                isError = false
                                if (it.length == AuthPinConstants.PIN_LENGTH) {
                                    val req = PinChangeRequest(oldPin, newPin, it)
                                    if (req.isValid()) {
                                        preferencesRepository.setQuickPin(newPin)
                                        preferencesRepository.setQuickLoginEnabled(true)
                                        onPinChanged()
                                    } else {
                                        isError = true
                                        errorMessage = context.getString(R.string.settings_pins_do_not_match)
                                    }
                                }
                            },
                            masked = true,
                            state = if (isError) PinCellsState.Error else PinCellsState.Normal,
                            autoFocus = true,
                            modifier = Modifier.fillMaxWidth().testTag("pin_step3_input")
                        )
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            val confirmAction = @Composable {
                Button(
                    onClick = {
                        when (step) {
                            1 -> {
                                if (preferencesRepository.verifyPin(oldPin)) {
                                    step = 2
                                    errorMessage = null
                                    isError = false
                                } else {
                                    isError = true
                                    errorMessage = context.getString(R.string.incorrect_pin_try_again)
                                }
                            }
                            2 -> {
                                if (newPin.matches(Regex(AuthPinConstants.PIN_REGEX))) {
                                    step = 3
                                    errorMessage = null
                                    isError = false
                                } else {
                                    isError = true
                                    errorMessage = context.getString(R.string.pin_must_be_4_digits)
                                }
                            }
                            3 -> {
                                val req = PinChangeRequest(oldPin, newPin, confirmPin)
                                if (req.isValid()) {
                                    preferencesRepository.setQuickPin(newPin)
                                    preferencesRepository.setQuickLoginEnabled(true)
                                    onPinChanged()
                                } else {
                                    isError = true
                                    errorMessage = context.getString(R.string.settings_pins_do_not_match)
                                }
                            }
                        }
                    },
                    shape = RoundedCornerShape(Dimens.RadiusMd),
                    elevation = null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        contentColor = PureWhite,
                        disabledContentColor = OnSurfaceVariantLight
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .depthRaisedButton(cornerRadius = Dimens.RadiusMd)
                        .testTag("pin_dialog_confirm")
                ) {
                    Text(
                        text = if (step == 3) stringResource(R.string.save_pin_btn) else stringResource(R.string.btn_next),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            val dismissAction = @Composable {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(Dimens.RadiusMd),
                    border = null,
                    elevation = null,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = OnSurfaceVariantLight
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .depthSecondaryButton(cornerRadius = Dimens.RadiusMd)
                        .testTag("pin_dialog_dismiss")
                ) {
                    Text(stringResource(R.string.cancel_18), fontWeight = FontWeight.SemiBold)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) { dismissAction() }
                Box(modifier = Modifier.weight(1f)) { confirmAction() }
            }
        }
    }
}

/**
 * Material 3 Password Change Dialog with eye icon, rules checklist, and confirm match.
 */
@Composable
fun ChangePasswordDialog(
    currentPasswordHash: String,
    onDismiss: () -> Unit,
    onPasswordChanged: (newPassword: String) -> Unit
) {
    val context = LocalContext.current
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var currentPasswordError by remember { mutableStateOf<String?>(null) }
    var newPasswordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }

    val hasMinLength = newPassword.length >= 8
    val hasDigit = newPassword.any { it.isDigit() }
    val hasUpper = newPassword.any { it.isUpperCase() }

    LojiaDialog(
        onDismissRequest = onDismiss,
        size = DialogSize.Medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.change_password_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            if (currentPasswordHash.isNotBlank()) {
                LojiaPasswordField(
                    value = currentPassword,
                    onValueChange = {
                        currentPassword = it
                        currentPasswordError = null
                    },
                    label = stringResource(R.string.current_password_label),
                    placeholder = stringResource(R.string.placeholder_enter_current_password),
                    errorMessage = currentPasswordError,
                    validationState = if (currentPasswordError != null) FieldValidationState.ERROR else FieldValidationState.DEFAULT,
                    testTag = "dialog_current_password"
                )
            }

            LojiaPasswordField(
                value = newPassword,
                onValueChange = {
                    newPassword = it
                    newPasswordError = null
                    if (confirmPassword.isNotEmpty() && it != confirmPassword) {
                        confirmPasswordError = context.getString(R.string.doc_passwords_do_not_match)
                    } else {
                        confirmPasswordError = null
                    }
                },
                label = stringResource(R.string.new_password_label),
                placeholder = stringResource(R.string.placeholder_enter_new_password),
                errorMessage = newPasswordError,
                validationState = if (newPasswordError != null) FieldValidationState.ERROR else FieldValidationState.DEFAULT,
                testTag = "dialog_new_password"
            )

            // Password rules checklist
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .depthInset(shape = RoundedCornerShape(Dimens.RadiusSm))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                PasswordRuleItem(text = stringResource(R.string.settings_pw_rule_length), isMet = hasMinLength)
                PasswordRuleItem(text = stringResource(R.string.settings_pw_rule_digit), isMet = hasDigit)
                PasswordRuleItem(text = stringResource(R.string.settings_pw_rule_upper), isMet = hasUpper)
            }

            LojiaPasswordField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    if (it.isNotEmpty() && it != newPassword) {
                        confirmPasswordError = context.getString(R.string.doc_passwords_do_not_match)
                    } else {
                        confirmPasswordError = null
                    }
                },
                label = stringResource(R.string.confirm_password_label),
                placeholder = stringResource(R.string.placeholder_reenter_new_password),
                errorMessage = confirmPasswordError,
                validationState = if (confirmPasswordError != null) FieldValidationState.ERROR else FieldValidationState.DEFAULT,
                testTag = "dialog_confirm_password"
            )

            val confirmAction = @Composable {
                Button(
                    onClick = {
                        var hasError = false
                        if (currentPasswordHash.isNotBlank()) {
                            if (currentPassword.isBlank()) {
                                currentPasswordError = context.getString(R.string.error_current_password_required)
                                hasError = true
                            } else if (!SecurityUtils.verifySecret(currentPassword, currentPasswordHash)) {
                                currentPasswordError = context.getString(R.string.error_incorrect_current_password)
                                hasError = true
                            }
                        }
                        if (newPassword.length < 8 || !newPassword.any { it.isDigit() } || !newPassword.any { it.isUpperCase() }) {
                            newPasswordError = context.getString(R.string.settings_pw_requirements_error)
                            hasError = true
                        }
                        if (newPassword != confirmPassword) {
                            confirmPasswordError = context.getString(R.string.doc_passwords_do_not_match)
                            hasError = true
                        }
                        if (!hasError) {
                            onPasswordChanged(newPassword)
                        }
                    },
                    shape = RoundedCornerShape(Dimens.RadiusMd),
                    elevation = null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        contentColor = PureWhite,
                        disabledContentColor = OnSurfaceVariantLight
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .depthRaisedButton(cornerRadius = Dimens.RadiusMd)
                        .testTag("btn_save_password")
                ) {
                    Text(stringResource(R.string.save_5), fontWeight = FontWeight.Bold)
                }
            }
            val dismissAction = @Composable {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(Dimens.RadiusMd),
                    border = null,
                    elevation = null,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = OnSurfaceVariantLight
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .depthSecondaryButton(cornerRadius = Dimens.RadiusMd)
                        .testTag("btn_cancel_password")
                ) {
                    Text(stringResource(R.string.cancel_18), fontWeight = FontWeight.SemiBold)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) { dismissAction() }
                Box(modifier = Modifier.weight(1f)) { confirmAction() }
            }
        }
    }
}

@Composable
private fun PasswordRuleItem(text: String, isMet: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = if (isMet) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (isMet) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = if (isMet) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Material 3 Email Change Dialog with regex validation and confirmation field.
 */
@Composable
fun ChangeEmailDialog(
    currentEmail: String,
    onDismiss: () -> Unit,
    onEmailChanged: (newEmail: String) -> Unit
) {
    val context = LocalContext.current
    var newEmail by remember { mutableStateOf(currentEmail) }
    var confirmEmail by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    var confirmError by remember { mutableStateOf<String?>(null) }

    val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    LojiaDialog(
        onDismissRequest = onDismiss,
        size = DialogSize.Medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Email,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.settings_change_email_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = stringResource(R.string.settings_change_email_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val newEmailMismatchText = stringResource(R.string.settings_emails_do_not_match)
            val newEmailLabel = stringResource(R.string.new_email_label)
            LojiaTextField(
                value = newEmail,
                onValueChange = {
                    newEmail = it
                    emailError = null
                    if (confirmEmail.isNotBlank() && it.trim() != confirmEmail.trim()) {
                        confirmError = newEmailMismatchText
                    } else {
                        confirmError = null
                    }
                },
                label = { Text(newEmailLabel) },
                contentDescription = newEmailLabel,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                isError = emailError != null,
                modifier = Modifier.fillMaxWidth().testTag("dialog_new_email")
            )
            if (emailError != null) {
                Text(
                    text = emailError ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            val confirmEmailMismatchText = stringResource(R.string.settings_emails_do_not_match)
            val confirmEmailLabel = stringResource(R.string.confirm_email_label)
            LojiaTextField(
                value = confirmEmail,
                onValueChange = {
                    confirmEmail = it
                    if (it.trim() != newEmail.trim()) {
                        confirmError = confirmEmailMismatchText
                    } else {
                        confirmError = null
                    }
                },
                label = { Text(confirmEmailLabel) },
                contentDescription = confirmEmailLabel,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                isError = confirmError != null,
                modifier = Modifier.fillMaxWidth().testTag("dialog_confirm_email")
            )
            if (confirmError != null) {
                Text(
                    text = confirmError ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            val confirmAction = @Composable {
                Button(
                    onClick = {
                        val cleanMail = newEmail.trim()
                        var hasErr = false
                        if (!cleanMail.matches(emailRegex)) {
                            emailError = context.getString(R.string.settings_invalid_email_error)
                            hasErr = true
                        }
                        if (confirmEmail.trim() != cleanMail) {
                            confirmError = context.getString(R.string.settings_emails_do_not_match)
                            hasErr = true
                        }
                        if (!hasErr) {
                            onEmailChanged(cleanMail)
                        }
                    },
                    shape = RoundedCornerShape(Dimens.RadiusMd),
                    elevation = null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        contentColor = PureWhite,
                        disabledContentColor = OnSurfaceVariantLight
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .depthRaisedButton(cornerRadius = Dimens.RadiusMd)
                        .testTag("btn_save_email")
                ) {
                    Text(stringResource(R.string.save_5), fontWeight = FontWeight.Bold)
                }
            }
            val dismissAction = @Composable {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(Dimens.RadiusMd),
                    border = null,
                    elevation = null,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = OnSurfaceVariantLight
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .depthSecondaryButton(cornerRadius = Dimens.RadiusMd)
                        .testTag("btn_cancel_email")
                ) {
                    Text(stringResource(R.string.cancel_18), fontWeight = FontWeight.SemiBold)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) { dismissAction() }
                Box(modifier = Modifier.weight(1f)) { confirmAction() }
            }
        }
    }
}
