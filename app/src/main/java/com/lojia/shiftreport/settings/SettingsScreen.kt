package com.lojia.shiftreport.settings

import com.lojia.shiftreport.R
import com.lojia.shiftreport.data.*
import com.lojia.shiftreport.util.*
import com.lojia.shiftreport.ui.common.*
import com.lojia.shiftreport.ui.theme.*
import com.lojia.shiftreport.auth.*
import com.lojia.shiftreport.report.*
import com.lojia.shiftreport.settings.*



import androidx.compose.foundation.background


import androidx.compose.foundation.layout.*


import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll


import androidx.compose.foundation.shape.RoundedCornerShape


import androidx.compose.material.icons.filled.*


import androidx.compose.material.icons.outlined.*


import androidx.compose.material3.*


import androidx.compose.runtime.*


import androidx.compose.ui.Alignment


import androidx.compose.ui.Modifier


import androidx.compose.ui.graphics.Color


import androidx.compose.ui.platform.LocalContext


import androidx.compose.ui.platform.testTag


import androidx.compose.ui.text.font.FontWeight


import androidx.compose.ui.unit.dp


import androidx.compose.ui.unit.sp


import androidx.compose.ui.res.stringResource


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    reportViewModel: ReportViewModel,
    activeModule: AppModule = AppModule.SHIFT_REPORT,
    onSwitchModule: (AppModule) -> Unit = {},
    onNavigateToSection: (String) -> Unit = {},
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentLanguage by reportViewModel.currentLanguage.collectAsState()
    val userProfile by reportViewModel.userProfile.collectAsState()
    val currentRole by reportViewModel.currentRole.collectAsState()
    val isAdmin = currentRole == "ADMIN"

    // Dialog trigger states
    var pendingAdminAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var pendingAdminSubtitle by remember { mutableStateOf<String?>(null) }
    var showRoleSwitchDialog by remember { mutableStateOf(false) }
    var showLanguageDropdown by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showSupportTicketDialog by remember { mutableStateOf(false) }
    var showTestPrintDialog by remember { mutableStateOf(false) }

    val handleRestrictedClick: (action: () -> Unit) -> Unit = { action ->
        pendingAdminSubtitle = null
        pendingAdminAction = action
    }

    val selectedReportMenu by reportViewModel.selectedReportSettingsMenu.collectAsState()
    val isPrinterSection = com.lojia.shiftreport.printer.PrinterNavigationConfig.isPrinterSection(selectedReportMenu)
    val depthStyle = LocalDepthStyle.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(depthStyle.reportPageBackground)
    ) {
        if (isPrinterSection) {
            Box(modifier = Modifier.fillMaxSize()) {
                SettingsReportSection(
                    reportViewModel = reportViewModel,
                    language = currentLanguage,
                    isAdmin = isAdmin,
                    onRestrictedClick = handleRestrictedClick,
                    onSwitchModule = onSwitchModule,
                    onConfigurePrinterClick = { showTestPrintDialog = true },
                    onNavigateToSection = onNavigateToSection,
                    onNavigateBack = onNavigateBack
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .testTag("settings_scrollable_list"),
                verticalArrangement = Arrangement.Top
            ) {
                SettingsReportSection(
                    reportViewModel = reportViewModel,
                    language = currentLanguage,
                    isAdmin = isAdmin,
                    onRestrictedClick = handleRestrictedClick,
                    onSwitchModule = onSwitchModule,
                    onConfigurePrinterClick = { showTestPrintDialog = true },
                    onNavigateToSection = onNavigateToSection,
                    onNavigateBack = onNavigateBack
                )

                // Bottom Spacing
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Role Switcher Dialog
    if (showRoleSwitchDialog) {
        var roleTarget by remember { mutableStateOf(if (isAdmin) "CASHIER" else "ADMIN") }

        com.lojia.shiftreport.ui.common.LojiaDialog(
            onDismissRequest = { showRoleSwitchDialog = false },
            size = com.lojia.shiftreport.ui.common.DialogSize.Small
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.switch_role),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceLight
                )
                Text(
                    text = stringResource(R.string.select_target_role_to),
                    fontSize = 13.sp,
                    color = TextSecondaryLight
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = roleTarget == "ADMIN",
                        onClick = { roleTarget = "ADMIN" },
                        label = { Text(stringResource(R.string.administrator)) },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = Dimens.MinTouchTarget)
                    )
                    FilterChip(
                        selected = roleTarget == "CASHIER",
                        onClick = { roleTarget = "CASHIER" },
                        label = { Text(stringResource(R.string.cashier_6)) },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = Dimens.MinTouchTarget)
                    )
                }

                val confirmAction = @Composable {
                    Button(
                        onClick = {
                            showRoleSwitchDialog = false
                            if (roleTarget == "ADMIN") {
                                pendingAdminSubtitle = context.getString(R.string.switch_role)
                                pendingAdminAction = {
                                    reportViewModel.switchUserRole(
                                        role = "ADMIN",
                                        authorizedByAdmin = true
                                    )
                                }
                            } else {
                                reportViewModel.switchUserRole(role = roleTarget)
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
                        Text(stringResource(R.string.apply), fontWeight = FontWeight.Bold)
                    }
                }
                val dismissAction = @Composable {
                    OutlinedButton(
                        onClick = { showRoleSwitchDialog = false },
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

    if (pendingAdminAction != null) {
        AdminAuthDialog(
            title = stringResource(R.string.admin_authorization_required),
            subtitle = pendingAdminSubtitle ?: stringResource(R.string.admin_reauth_default_title),
            onDismissRequest = {
                pendingAdminAction = null
                pendingAdminSubtitle = null
            },
            onAuthSuccess = {
                val action = pendingAdminAction
                pendingAdminAction = null
                pendingAdminSubtitle = null
                action?.invoke()
            }
        )
    }

    // Common Dialogs (Terms, Privacy, Support Ticket, Test Print)
    SettingsCommonDialogs(
        showAdminPinDialog = false,
        onDismissAdminPin = {},
        onAdminPinVerified = {},
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
