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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            .width(304.dp)
            .fillMaxHeight()
            .testTag("main_navigation_drawer")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 20.dp)
        ) {
            // Compact Header - 140dp
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
                    .statusBarsPadding()
                    .height(140.dp)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Lojia Shift Report",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = userProfile?.fullName ?: "Shift Manager",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    text = "Shift Report",
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = Color.White.copy(alpha = 0.2f),
                                labelColor = Color.White
                            ),
                            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.35f)),
                            modifier = Modifier.height(28.dp)
                        )

                        AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    text = userProfile?.currentRole ?: "ADMIN",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = Color(0xFF2E7D32),
                                labelColor = Color.White
                            ),
                            border = null,
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ===============================================
            // Group A — NO header
            // ===============================================
            // 1. Analytics
            DrawerMenuItem(
                title = stringResource(R.string.nav_analytics),
                icon = Icons.Outlined.Insights,
                activeColor = PrimaryIndigoLight,
                isSelected = navState is AppNavState.ShiftReportState.Analytics,
                modifier = Modifier.padding(top = 8.dp),
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

            // Employee Management
            DrawerMenuItem(
                title = "Employee Management",
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

            // 8. Support
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
                start = 20.dp,
                end = 20.dp,
                top = 20.dp,
                bottom = 8.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Accent bar
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(PrimaryIndigoLight)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
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
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .height(50.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(
                    color = if (isSelected) PrimaryIndigoLight.copy(alpha = 0.2f)
                            else OnSurfaceVariantLight.copy(alpha = 0.15f)
                ),
                onClick = onClick
            )
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left accent bar (only when selected)
        if (isSelected) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(PrimaryIndigoLight)
            )
            Spacer(modifier = Modifier.width(10.dp))
        } else {
            Spacer(modifier = Modifier.width(13.dp))
        }

        // Icon
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )

        Spacer(modifier = Modifier.width(14.dp))

        // Text
        Text(
            text = title,
            fontSize = 14.5.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold
                         else FontWeight.Medium,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
