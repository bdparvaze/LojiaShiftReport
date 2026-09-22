package com.lojia.shiftreport.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.lojia.shiftreport.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojia.shiftreport.AppNavState
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
    val headerBgColor = PrimaryIndigoDark

    ModalDrawerSheet(
        drawerContainerColor = PureWhite,
        modifier = Modifier
            .widthIn(min = 240.dp, max = 300.dp)
            .fillMaxHeight()
            .testTag("main_navigation_drawer")
    ) {
        // Top Header Banner
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerBgColor)
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(PureWhite.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Assessment,
                        contentDescription = null,
                        tint = PureWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f, fill = false)) {
                    AutoText(
                        text = "LojiaShiftReport",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        ),
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = userProfile?.fullName?.ifBlank { "Shift Manager" } ?: "Shift Manager",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = PureWhite.copy(alpha = 0.85f),
                            fontSize = 11.sp
                        ),
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Role and Active Module Badges
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = PureWhite.copy(alpha = 0.22f)
                ) {
                    AutoText(
                        text = "Shift Report",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = PureWhite,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.sp
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = PureWhite.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = userProfile?.currentRole ?: "ADMIN",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = PureWhite,
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.sp
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
        }

        // Scrollable Drawer Menu List: Clean Loyverse-style navigation items
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 6.dp, horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // ===============================================
            // 📊 SHIFT REPORT MODULE - PRIMARY SCREENS
            // ===============================================
            // 1. Analytics
            DrawerMenuItem(
                title = "Analytics",
                icon = Icons.Outlined.Insights,
                activeColor = PrimaryIndigo,
                isSelected = navState is AppNavState.ShiftReportState.Analytics,
                onClick = {
                    onNavigate(AppNavState.ShiftReportState.Analytics)
                    onCloseDrawer()
                }
            )

            // 2. Shift Report
            DrawerMenuItem(
                title = "Shift Report",
                icon = Icons.Outlined.Assessment,
                activeColor = PrimaryIndigo,
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
                activeColor = PrimaryIndigo,
                isSelected = navState is AppNavState.ShiftReportState.DocumentScanner,
                onClick = {
                    onNavigate(AppNavState.ShiftReportState.DocumentScanner)
                    onCloseDrawer()
                }
            )

            // 3. Cashier
            DrawerMenuItem(
                title = "Cashier",
                icon = Icons.Outlined.Badge,
                activeColor = PrimaryIndigo,
                isSelected = navState is AppNavState.ShiftReportState.SettingsDetail && navState.section == "cashiers",
                onClick = {
                    onOpenReportMenu("cashiers")
                    onNavigate(AppNavState.ShiftReportState.SettingsDetail("cashiers"))
                    onCloseDrawer()
                }
            )

            // 4. Profile
            DrawerMenuItem(
                title = "Profile",
                icon = Icons.Outlined.Person,
                activeColor = PrimaryIndigo,
                isSelected = navState is AppNavState.ShiftReportState.SettingsDetail && navState.section == "profile",
                onClick = {
                    onOpenReportMenu("profile")
                    onNavigate(AppNavState.ShiftReportState.SettingsDetail("profile"))
                    onCloseDrawer()
                }
            )

            // 5. Security
            DrawerMenuItem(
                title = "Security",
                icon = Icons.Outlined.Shield,
                activeColor = PrimaryIndigo,
                isSelected = navState is AppNavState.ShiftReportState.SettingsDetail && navState.section == "security",
                onClick = {
                    onOpenReportMenu("security")
                    onNavigate(AppNavState.ShiftReportState.SettingsDetail("security"))
                    onCloseDrawer()
                }
            )

            // 6. Language
            DrawerMenuItem(
                title = "Language",
                icon = Icons.Outlined.Language,
                activeColor = PrimaryIndigo,
                isSelected = navState is AppNavState.ShiftReportState.SettingsDetail && navState.section == "language",
                onClick = {
                    onOpenReportMenu("language")
                    onNavigate(AppNavState.ShiftReportState.SettingsDetail("language"))
                    onCloseDrawer()
                }
            )

            // 7. Backup
            DrawerMenuItem(
                title = "Backup",
                icon = Icons.Outlined.CloudUpload,
                activeColor = PrimaryIndigo,
                isSelected = navState is AppNavState.ShiftReportState.SettingsDetail && navState.section == "backup",
                onClick = {
                    onOpenReportMenu("backup")
                    onNavigate(AppNavState.ShiftReportState.SettingsDetail("backup"))
                    onCloseDrawer()
                }
            )

            // 8. Support
            DrawerMenuItem(
                title = "Support",
                icon = Icons.Outlined.HeadsetMic,
                activeColor = PrimaryIndigo,
                isSelected = navState is AppNavState.ShiftReportState.SettingsDetail && navState.section == "support",
                onClick = {
                    onOpenReportMenu("support")
                    onNavigate(AppNavState.ShiftReportState.SettingsDetail("support"))
                    onCloseDrawer()
                }
            )

            // 9. About
            DrawerMenuItem(
                title = "About",
                icon = Icons.Outlined.Info,
                activeColor = PrimaryIndigo,
                isSelected = navState is AppNavState.ShiftReportState.SettingsDetail && navState.section == "about",
                onClick = {
                    onOpenReportMenu("about")
                    onNavigate(AppNavState.ShiftReportState.SettingsDetail("about"))
                    onCloseDrawer()
                }
            )

            // 10. Log Out
            DrawerMenuItem(
                title = "Log Out",
                icon = Icons.AutoMirrored.Filled.ExitToApp,
                activeColor = Color(0xFFDC2626),
                isSelected = false,
                onClick = {
                    onCloseDrawer()
                    onLockApp()
                }
            )
        }
    }
}

/**
 * Drawer item representing a menu entry with icon and title.
 * Automatically translates the title into the active world language using AutoText.
 */
@Composable
private fun DrawerMenuItem(
    title: String,
    icon: ImageVector,
    activeColor: Color,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) activeColor.copy(alpha = 0.12f) else Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 44.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) activeColor else Color(0xFF555555),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            AutoText(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 14.sp,
                    color = if (isSelected) activeColor else Color(0xFF212121)
                ),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
