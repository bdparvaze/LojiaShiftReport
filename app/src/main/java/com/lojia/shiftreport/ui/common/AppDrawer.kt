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
        drawerContainerColor = SurfaceLight,
        modifier = Modifier
            .widthIn(min = 240.dp, max = 300.dp)
            .fillMaxHeight()
            .testTag("main_navigation_drawer")
    ) {
        // Top Header Banner (Locked 180.dp exact height, 24.dp bottomEnd radius, solid PrimaryIndigoDark)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(LojiaDimens.DrawerHeaderHeight)
                .clip(RoundedCornerShape(bottomEnd = LojiaDimens.DrawerHeaderRadius))
                .background(headerBgColor)
                .statusBarsPadding()
                .padding(20.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            // 1. Logo circle 48.dp
            Box(
                modifier = Modifier
                    .size(LojiaDimens.AvatarSmall)
                    .clip(CircleShape)
                    .background(PureWhite.copy(alpha = 0.2f))
                    .border(1.5.dp, PureWhite.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Assessment,
                    contentDescription = null,
                    tint = PureWhite,
                    modifier = Modifier.size(LojiaDimens.IconSize)
                )
            }

            // 2. Spacer 12.dp
            Spacer(modifier = Modifier.height(12.dp))

            // 3. Text "Lojia Shift Report" — 18.sp, FontWeight.Bold, PureWhite
            AutoText(
                text = "Lojia Shift Report",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = PureWhite
                ),
                maxLines = 1
            )

            // 4. Spacer 4.dp
            Spacer(modifier = Modifier.height(4.dp))

            // 5. Text (userProfile?.fullName ?: "Shift Manager") — 13.sp, PureWhite.copy(alpha = 0.85f)
            Text(
                text = userProfile?.fullName?.ifBlank { "Shift Manager" } ?: "Shift Manager",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = PureWhite.copy(alpha = 0.85f),
                    fontSize = 13.sp
                ),
                maxLines = 1
            )

            // 6. Spacer 12.dp
            Spacer(modifier = Modifier.height(12.dp))

            // 7. Row (spacing 8.dp)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = PureWhite.copy(alpha = 0.20f)
                ) {
                    AutoText(
                        text = "Shift Report",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = PureWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = SuccessGreen
                ) {
                    Text(
                        text = userProfile?.currentRole ?: "ADMIN",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = PureWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Scrollable Drawer Menu List
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // ===============================================
            // Group A — NO header
            // ===============================================
            // 1. Analytics
            DrawerMenuItem(
                title = "Analytics",
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
                title = "Shift Report",
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

            // ===============================================
            // Group B — header "MANAGEMENT"
            // ===============================================
            DrawerSectionHeader("Management")

            // 3. Cashier
            DrawerMenuItem(
                title = "Cashier",
                icon = Icons.Outlined.Badge,
                activeColor = PrimaryIndigoLight,
                isSelected = navState is AppNavState.ShiftReportState.SettingsDetail && navState.section == "cashiers",
                onClick = {
                    onOpenReportMenu("cashiers")
                    onNavigate(AppNavState.ShiftReportState.SettingsDetail("cashiers"))
                    onCloseDrawer()
                }
            )

            // ===============================================
            // Group C — header "ACCOUNT"
            // ===============================================
            DrawerSectionHeader("Account")

            // 4. Profile
            DrawerMenuItem(
                title = "Profile",
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
                title = "Security",
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
                title = "Language",
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
                title = "Backup",
                icon = Icons.Outlined.CloudUpload,
                activeColor = PrimaryIndigoLight,
                isSelected = navState is AppNavState.ShiftReportState.SettingsDetail && navState.section == "backup",
                onClick = {
                    onOpenReportMenu("backup")
                    onNavigate(AppNavState.ShiftReportState.SettingsDetail("backup"))
                    onCloseDrawer()
                }
            )

            // ===============================================
            // Group D — header "SYSTEM"
            // ===============================================
            DrawerSectionHeader("System")

            // 8. Support
            DrawerMenuItem(
                title = "Support",
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
                title = "About",
                icon = Icons.Outlined.Info,
                activeColor = PrimaryIndigoLight,
                isSelected = navState is AppNavState.ShiftReportState.SettingsDetail && navState.section == "about",
                onClick = {
                    onOpenReportMenu("about")
                    onNavigate(AppNavState.ShiftReportState.SettingsDetail("about"))
                    onCloseDrawer()
                }
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                thickness = 1.dp,
                color = OutlineVariantLight
            )

            // 10. Log Out
            DrawerMenuItem(
                title = "Log Out",
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
    Text(
        text = text.uppercase(),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.15.sp,
        color = TextHintColor
    )
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
    isDanger: Boolean = false,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) PrimaryContainerLight else Color.Transparent
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = iconTint,
            modifier = Modifier.size(LojiaDimens.IconSize)
        )
        Spacer(modifier = Modifier.width(LojiaDimens.IconTextGap))
        AutoText(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 14.sp,
                color = textColor
            ),
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}
