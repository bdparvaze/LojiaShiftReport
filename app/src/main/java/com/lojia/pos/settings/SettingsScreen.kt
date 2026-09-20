package com.lojia.pos.settings

import com.lojia.pos.ui.common.LojiaTextField
import com.lojia.pos.R
import com.lojia.pos.data.*
import com.lojia.pos.util.*
import com.lojia.pos.ui.common.*
import com.lojia.pos.ui.theme.*
import com.lojia.pos.auth.*
import com.lojia.pos.report.*
import com.lojia.pos.settings.*



import androidx.compose.animation.AnimatedVisibility


import androidx.compose.animation.Crossfade


import androidx.compose.foundation.BorderStroke


import androidx.compose.foundation.background


import androidx.compose.foundation.clickable


import androidx.compose.foundation.layout.*


import androidx.compose.foundation.lazy.LazyColumn


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


import androidx.compose.ui.text.font.FontWeight


import androidx.compose.ui.unit.dp


import androidx.compose.ui.unit.sp



import com.lojia.pos.util.NotificationHelper


import androidx.compose.ui.res.stringResource


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    reportViewModel: ReportViewModel,
    activeModule: AppModule = AppModule.SHIFT_REPORT,
    onSwitchModule: (AppModule) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentLanguage by reportViewModel.currentLanguage.collectAsState()
    val userProfile by reportViewModel.userProfile.collectAsState()
    val currentRole by reportViewModel.currentRole.collectAsState()
    val isAdmin = currentRole == "ADMIN"

    // Dialog trigger states
    var showAdminPinDialog by remember { mutableStateOf(false) }
    var pendingAdminAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showRoleSwitchDialog by remember { mutableStateOf(false) }
    var showLanguageDropdown by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showSupportTicketDialog by remember { mutableStateOf(false) }
    var showTestPrintDialog by remember { mutableStateOf(false) }

    val handleRestrictedClick: (action: () -> Unit) -> Unit = { action ->
        if (isAdmin) {
            action()
        } else {
            pendingAdminAction = action
            showAdminPinDialog = true
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("settings_scrollable_list"),
            verticalArrangement = Arrangement.Top
        ) {
            item {
                SettingsReportSection(
                    reportViewModel = reportViewModel,
                    language = currentLanguage,
                    isAdmin = isAdmin,
                    onRestrictedClick = handleRestrictedClick,
                    onSwitchModule = onSwitchModule
                )
            }

            // Bottom Spacing
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Role Switcher Dialog
    if (showRoleSwitchDialog) {
        var pinToAdmin by remember { mutableStateOf("") }
        var roleTarget by remember { mutableStateOf(if (isAdmin) "CASHIER" else "ADMIN") }
        var isPinError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showRoleSwitchDialog = false },
            title = { Text(stringResource(R.string.switch_role), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.select_target_role_to), fontSize = 13.sp, color = TextSecondaryLight)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = roleTarget == "ADMIN",
                            onClick = { roleTarget = "ADMIN" },
                            label = { Text(stringResource(R.string.administrator)) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = roleTarget == "CASHIER",
                            onClick = { roleTarget = "CASHIER" },
                            label = { Text(stringResource(R.string.cashier_6)) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (roleTarget == "ADMIN" && !isAdmin) {
                        LojiaTextField(
                            value = pinToAdmin,
                            onValueChange = {
                                if (it.length <= 6 && it.all { c -> c.isDigit() }) {
                                    pinToAdmin = it
                                    isPinError = false
                                }
                            },
                            label = { Text(stringResource(R.string.msg_admin_pin_required)) },
                            singleLine = true,
                            isError = isPinError,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (isPinError) {
                            Text(stringResource(R.string.incorrect_pin), color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        reportViewModel.switchUserRole(
                            role = roleTarget,
                            enteredPin = pinToAdmin,
                            onSuccess = { showRoleSwitchDialog = false },
                            onFailure = { isPinError = true }
                        )
                    }
                ) {
                    Text(stringResource(R.string.apply))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRoleSwitchDialog = false }) { Text(stringResource(R.string.cancel_18)) }
            }
        )
    }

    // Common Dialogs (Admin PIN, Terms, Privacy, Support Ticket, Test Print)
    SettingsCommonDialogs(
        showAdminPinDialog = showAdminPinDialog,
        onDismissAdminPin = {
            showAdminPinDialog = false
            pendingAdminAction = null
        },
        onAdminPinVerified = {
            showAdminPinDialog = false
            pendingAdminAction?.invoke()
            pendingAdminAction = null
        },
        showTermsDialog = showTermsDialog,
        onDismissTerms = { showTermsDialog = false },
        showPrivacyDialog = showPrivacyDialog,
        onDismissPrivacy = { showPrivacyDialog = false },
        showSupportTicketDialog = showSupportTicketDialog,
        onDismissSupportTicket = { showSupportTicketDialog = false },
        showTestPrintDialog = showTestPrintDialog,
        onDismissTestPrint = { showTestPrintDialog = false },
        userProfile = userProfile
    )
}
