package com.lojia.shiftreport.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.lojia.shiftreport.AppNavState
import com.lojia.shiftreport.R
import com.lojia.shiftreport.data.AppLanguage
import com.lojia.shiftreport.data.AppModule
import com.lojia.shiftreport.data.BusinessProfile
import com.lojia.shiftreport.data.UserProfile
import com.lojia.shiftreport.ui.theme.*

@Composable
fun MainAppDrawer(
    activeModule: AppModule = AppModule.SHIFT_REPORT,
    navState: AppNavState,
    currentLanguage: AppLanguage,
    businessProfile: BusinessProfile?,
    userProfile: UserProfile?,
    onNavigate: (AppNavState) -> Unit,
    onOpenReportMenu: (String) -> Unit = {},
    onCloseDrawer: () -> Unit,
    onLockApp: () -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .width(Dimens.DrawerWidth)
            .fillMaxHeight()
            .testTag("main_navigation_drawer")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = Dimens.SpacingLg)
        ) {
            // Compact Header - 140dp
            LojiaGradientHeader(
                shape = RectangleShape,
                contentPadding = PaddingValues(Dimens.SpacingNone)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(Dimens.DrawerHeaderCompactHeight)
                        .padding(Dimens.SpacingLg)
                ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingMd)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(Dimens.LogoTileSize)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(Dimens.IconLg)
                            )
                        }

                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = userProfile?.fullName ?: stringResource(R.string.default_shift_manager),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    text = stringResource(R.string.nav_reports),
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = Color.White.copy(alpha = 0.2f),
                                labelColor = Color.White
                            ),
                            border = BorderStroke(Dimens.RowDividerWidth, Color.White.copy(alpha = 0.35f)),
                            modifier = Modifier.height(Dimens.DrawerChipHeight)
                        )

                        val rawRole = userProfile?.currentRole
                        val roleBadgeText = when {
                            rawRole.isNullOrBlank() || rawRole.equals("ADMIN", ignoreCase = true) ->
                                stringResource(R.string.role_admin)
                            rawRole.equals("CASHIER", ignoreCase = true) ->
                                stringResource(R.string.cashier_1)
                            else -> rawRole
                        }
                        AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    text = roleBadgeText,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = RoleBadgeGreen,
                                labelColor = Color.White
                            ),
                            border = null,
                            modifier = Modifier.height(Dimens.DrawerChipHeight)
                        )
                    }
                }
                }
            }

            Spacer(modifier = Modifier.height(Dimens.SpacingSm))

            // ===============================================
            // Group A — NO header
            // ===============================================
            // 0. Home Dashboard
            DrawerMenuItem(
                title = stringResource(R.string.nav_home),
                icon = Icons.Outlined.Home,
                activeColor = PrimaryIndigoLight,
                isSelected = navState is AppNavState.ShiftReportState.Home,
                modifier = Modifier
                    .padding(top = Dimens.SpacingSm)
                    .testTag("drawer_item_home"),
                onClick = {
                    onNavigate(AppNavState.ShiftReportState.Home)
                    onCloseDrawer()
                }
            )

            // 1. Analytics
            DrawerMenuItem(
                title = stringResource(R.string.nav_analytics),
                icon = Icons.Outlined.Insights,
                activeColor = PrimaryIndigoLight,
                isSelected = navState is AppNavState.ShiftReportState.Analytics,
                onClick = {
                    onNavigate(AppNavState.ShiftReportState.Analytics)
                    onCloseDrawer()
                }
            )

            // 2. Shift Report
            DrawerMenuItem(
                title = stringResource(R.string.nav_reports),
                icon = Icons.Outlined.Assessment,
                activeColor = PrimaryIndigoLight,
                isSelected = navState is AppNavState.ShiftReportState.Reports,
                onClick = {
                    onNavigate(AppNavState.ShiftReportState.Reports)
                    onCloseDrawer()
                }
            )

            // 2.5 Document Scanner
            DrawerMenuItem(
                title = stringResource(R.string.document_scanner),
                icon = Icons.Outlined.DocumentScanner,
                activeColor = PrimaryIndigoLight,
                isSelected = navState is AppNavState.ShiftReportState.DocumentScanner,
                onClick = {
                    onNavigate(AppNavState.ShiftReportState.DocumentScanner)
                    onCloseDrawer()
                }
            )

            // 3. Cashier
            DrawerMenuItem(
                title = stringResource(R.string.nav_cashier),
                icon = Icons.Outlined.Badge,
                activeColor = PrimaryIndigoLight,
                isSelected = navState is AppNavState.ShiftReportState.SettingsDetail && navState.section == "cashiers",
                onClick = {
                    onOpenReportMenu("cashiers")
                    onNavigate(AppNavState.ShiftReportState.SettingsDetail("cashiers"))
                    onCloseDrawer()
                }
            )

            // Employee
            DrawerMenuItem(
                title = stringResource(R.string.nav_employee),
                icon = Icons.Outlined.Groups,
                activeColor = PrimaryIndigoLight,
                isSelected = navState is AppNavState.ShiftReportState.SettingsDetail && navState.section == "employees",
                onClick = {
                    onOpenReportMenu("employees")
                    onNavigate(AppNavState.ShiftReportState.SettingsDetail("employees"))
                    onCloseDrawer()
                }
            )

            // 4. Profile
            DrawerMenuItem(
                title = stringResource(R.string.nav_profile),
                icon = Icons.Outlined.Person,
                activeColor = PrimaryIndigoLight,
                isSelected = navState is AppNavState.ShiftReportState.SettingsDetail && navState.section == "profile",
                onClick = {
                    onOpenReportMenu("profile")
                    onNavigate(AppNavState.ShiftReportState.SettingsDetail("profile"))
                    onCloseDrawer()
                }
            )

            // 5. Security
            DrawerMenuItem(
                title = stringResource(R.string.nav_security),
                icon = Icons.Outlined.Shield,
                activeColor = PrimaryIndigoLight,
                isSelected = navState is AppNavState.ShiftReportState.SettingsDetail && navState.section == "security",
                onClick = {
                    onOpenReportMenu("security")
                    onNavigate(AppNavState.ShiftReportState.SettingsDetail("security"))
                    onCloseDrawer()
                }
            )

            // 6. Language
            DrawerMenuItem(
                title = stringResource(R.string.nav_language),
                icon = Icons.Outlined.Language,
                activeColor = PrimaryIndigoLight,
                isSelected = navState is AppNavState.ShiftReportState.SettingsDetail && navState.section == "language",
                onClick = {
                    onOpenReportMenu("language")
                    onNavigate(AppNavState.ShiftReportState.SettingsDetail("language"))
                    onCloseDrawer()
                }
            )

            // 7. Backup
            DrawerMenuItem(
                title = stringResource(R.string.nav_backup),
                icon = Icons.Outlined.CloudUpload,
                activeColor = PrimaryIndigoLight,
                isSelected = navState is AppNavState.ShiftReportState.SettingsDetail && navState.section == "backup",
                onClick = {
                    onOpenReportMenu("backup")
                    onNavigate(AppNavState.ShiftReportState.SettingsDetail("backup"))
                    onCloseDrawer()
                }
            )

            // 7.5 Printer
            DrawerMenuItem(
                title = stringResource(R.string.nav_printer),
                icon = Icons.Outlined.Print,
                activeColor = PrimaryIndigoLight,
                isSelected = navState is AppNavState.ShiftReportState.SettingsDetail &&
                    com.lojia.shiftreport.printer.PrinterNavigationConfig.isPrinterSection(navState.section),
                modifier = Modifier.testTag("drawer_item_printer"),
                onClick = {
                    val target = com.lojia.shiftreport.printer.PrinterNavigationConfig.SECTION_PRINTER
                    onOpenReportMenu(target)
                    onNavigate(AppNavState.ShiftReportState.SettingsDetail(target))
                    onCloseDrawer()
                }
            )

            // 8. Support
            if (com.lojia.shiftreport.util.SupportContact.isSupportConfigured()) {
                DrawerMenuItem(
                    title = stringResource(R.string.nav_support),
                    icon = Icons.Outlined.HeadsetMic,
                    activeColor = PrimaryIndigoLight,
                    isSelected = navState is AppNavState.ShiftReportState.SettingsDetail && navState.section == "support",
                    onClick = {
                        onOpenReportMenu("support")
                        onNavigate(AppNavState.ShiftReportState.SettingsDetail("support"))
                        onCloseDrawer()
                    }
                )
            }

            // 9. About
            DrawerMenuItem(
                title = stringResource(R.string.nav_about),
                icon = Icons.Outlined.Info,
                activeColor = PrimaryIndigoLight,
                isSelected = navState is AppNavState.ShiftReportState.SettingsDetail && navState.section == "about",
                onClick = {
                    onOpenReportMenu("about")
                    onNavigate(AppNavState.ShiftReportState.SettingsDetail("about"))
                    onCloseDrawer()
                }
            )

            // 10. Log Out
            DrawerMenuItem(
                title = stringResource(R.string.nav_logout),
                icon = Icons.AutoMirrored.Filled.Logout,
                activeColor = ErrorRedLight,
                isSelected = false,
                isDanger = true,
                onClick = {
                    onCloseDrawer()
                    onLockApp()
                }
            )
        }
    }
}

@Composable
private fun DrawerSectionHeader(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = Dimens.SpacingLg,
                end = Dimens.SpacingLg,
                top = Dimens.SpacingLg,
                bottom = Dimens.SpacingSm
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Accent bar
        Box(
            modifier = Modifier
                .width(Dimens.AccentBarWidth)
                .height(Dimens.AccentBarHeight)
                .clip(RoundedCornerShape(Dimens.AccentBarCorner))
                .background(PrimaryIndigoLight)
        )
        Spacer(modifier = Modifier.width(Dimens.SpacingSm))
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = TextHintColor
        )
    }
}

/**
 * Drawer item representing a menu entry with icon and title.
 */
@Composable
private fun DrawerMenuItem(
    title: String,
    icon: ImageVector,
    activeColor: Color,
    isSelected: Boolean = false,
    isDanger: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) PrimaryContainerLight
                          else Color.Transparent

    val iconTint = when {
        isDanger -> ErrorRedLight
        isSelected -> PrimaryIndigoLight
        else -> OnSurfaceVariantLight
    }

    val textColor = when {
        isDanger -> ErrorRedLight
        isSelected -> PrimaryIndigoLight
        else -> OnSurfaceLight
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.SpacingMd, vertical = Dimens.SpacingXxs)
            .height(Dimens.DrawerItemHeight)
            .clip(RoundedCornerShape(Dimens.RadiusLg))
            .background(backgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(
                    color = if (isSelected) PrimaryIndigoLight.copy(alpha = 0.2f)
                            else OnSurfaceVariantLight.copy(alpha = 0.15f)
                ),
                onClick = onClick
            )
            .padding(horizontal = Dimens.SpacingMd),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left accent bar (only when selected)
        if (isSelected) {
            Box(
                modifier = Modifier
                    .width(Dimens.AccentBarWidth)
                    .height(Dimens.IconAction)
                    .clip(RoundedCornerShape(Dimens.AccentBarCorner))
                    .background(PrimaryIndigoLight)
            )
            Spacer(modifier = Modifier.width(Dimens.DrawerAccentGap))
        } else {
            Spacer(modifier = Modifier.width(Dimens.DrawerUnselectedIndent))
        }

        // Icon
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = iconTint,
            modifier = Modifier.size(Dimens.IconAction)
        )

        Spacer(modifier = Modifier.width(Dimens.SpacingMd))

        // Text
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isSelected) FontWeight.SemiBold
                         else FontWeight.Medium,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
