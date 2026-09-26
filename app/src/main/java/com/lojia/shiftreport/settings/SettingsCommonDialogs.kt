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
        AlertDialog(
            onDismissRequest = {
                enteredPin = ""
                pinError = false
                onDismissAdminPin()
            },
            containerColor = SurfaceLight,
            icon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text(stringResource(R.string.admin_authorization_required), fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()).imePadding()) {
                    Text(
                        stringResource(R.string.this_setting_is_protected),
                        fontSize = 13.sp,
                        color = TextSecondaryLight
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Column {
                        Text(stringResource(R.string.msg_admin_pin_required), style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        LojiaTextField(
                            value = enteredPin,
                            onValueChange = { 
                                if (it.length <= 6) {
                                    enteredPin = it
                                    pinError = false
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("admin_pin_input")
                        )
                    }
                    if (pinError) {
                        Text(stringResource(R.string.incorrect_pin_try_again), color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val expectedPin = userProfile?.pin.orEmpty()
                        if (SecurityUtils.verifySecret(enteredPin, expectedPin)) {
                            enteredPin = ""
                            pinError = false
                            onAdminPinVerified()
                        } else {
                            pinError = true
                        }
                    },
                    modifier = Modifier.testTag("admin_pin_confirm")
                ) {
                    Text(stringResource(R.string.unlock))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    enteredPin = ""
                    pinError = false
                    onDismissAdminPin()
                }) {
                    Text(stringResource(R.string.cancel_18))
                }
            }
        )
    }

    // Terms & Conditions Dialog
    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = onDismissTerms,
            containerColor = SurfaceLight,
            icon = { Icon(Icons.Default.Gavel, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text(stringResource(R.string.terms_conditions_1), fontWeight = FontWeight.Bold) },
            text = {
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
            },
            confirmButton = {
                Button(onClick = onDismissTerms) {
                    Text(stringResource(R.string.i_understand))
                }
            }
        )
    }

    // Privacy Policy Dialog
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = onDismissPrivacy,
            containerColor = SurfaceLight,
            icon = { Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text(stringResource(R.string.privacy_policy_2), fontWeight = FontWeight.Bold) },
            text = {
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
            },
            confirmButton = {
                Button(onClick = onDismissPrivacy) {
                    Text(stringResource(R.string.close_7))
                }
            }
        )
    }

    // Support Ticket Dialog
    if (showSupportTicketDialog) {
        var subject by remember { mutableStateOf("") }
        var details by remember { mutableStateOf("") }
        var ticketSubmitted by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = onDismissSupportTicket,
            containerColor = SurfaceLight,
            title = { Text(stringResource(R.string.submit_support_ticket), fontWeight = FontWeight.Bold) },
            text = {
                if (ticketSubmitted) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(stringResource(R.string.ticket_tk8892_created), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(stringResource(R.string.our_technical_support_team), fontSize = 13.sp, color = TextSecondaryLight)
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.verticalScroll(rememberScrollState()).imePadding()
                    ) {
                        Text(stringResource(R.string.need_help_with_pos), fontSize = 13.sp, color = TextSecondaryLight)
                        LojiaTextField(
                            value = subject,
                            onValueChange = { subject = it },
                            label = { Text(stringResource(R.string.subject_eg_printer_setup)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        LojiaMultilineTextField(
                            value = details,
                            onValueChange = { details = it },
                            label = { Text(stringResource(R.string.describe_the_issue)) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp)
                        )
                    }
                }
            },
            confirmButton = {
                if (ticketSubmitted) {
                    Button(onClick = onDismissSupportTicket) { Text(stringResource(R.string.done_1)) }
                } else {
                    Button(
                        onClick = {
                            if (subject.isNotBlank()) {
                                ticketSubmitted = true
                                NotificationHelper.sendTestPushNotification(
                                    context,
                                    "🎫 Support Ticket Submitted",
                                    "Your request '$subject' has been received by Lojia Support Desk."
                                )
                            }
                        }
                    ) {
                        Text(stringResource(R.string.submit_ticket_1))
                    }
                }
            },
            dismissButton = {
                if (!ticketSubmitted) {
                    TextButton(onClick = onDismissSupportTicket) { Text(stringResource(R.string.cancel_18)) }
                }
            }
        )
    }

    // Thermal Printer Setup & Test Dialog
    if (showTestPrintDialog) {
        com.lojia.shiftreport.printer.PrinterSetupDialog(
            onDismiss = onDismissTestPrint
        )
    }
}
