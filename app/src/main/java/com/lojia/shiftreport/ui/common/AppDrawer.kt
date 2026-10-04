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
        drawerContainerColor = SurfaceLight,
        modifier = Modifier
            .widthIn(min = 280.dp, max = 320.dp)
            .fillMaxHeight()
            .testTag("main_navigation_drawer")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 20.dp)
        ) {
            // Top Header Banner (200.dp height, 28.dp bottomEnd radius, solid PrimaryIndigoDark)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(
                        RoundedCornerShape(
                            bottomEnd = 28.dp,
                            bottomStart = 0.dp
                        )
                    )
                    .background(PrimaryIndigoDark)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                // Logo circle
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(PureWhite.copy(alpha = 0.15f))
                        .border(
                            width = 1.5.dp,
                            color = PureWhite.copy(alpha = 0.4f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Assessment,
                        contentDescription = null,
                        tint = PureWhite,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Title
                Text(
                    text = "Lojia Shift Report",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = PureWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Subtitle (user name)
                Text(
                    text = userProfile?.fullName ?: "Shift Manager",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = PureWhite.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Badge row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Badge 1: Shift Report (current mode)
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = PureWhite.copy(alpha = 0.22f),
                        border = BorderStroke(
                            width = 0.5.dp,
                            color = PureWhite.copy(alpha = 0.35f)
                        )
                    ) {
                        Text(
                            text = "Shift Report",
                            modifier = Modifier.padding(
                                horizontal = 12.dp,
                                vertical = 6.dp
                            ),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite,
                            maxLines = 1,
                            softWrap = false,
                            letterSpacing = 0.3.sp
                        )
                    }

                    // Badge 2: Role (ADMIN)
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = SuccessGreen
                    ) {
                        Text(
                            text = userProfile?.currentRole ?: "ADMIN",
                            modifier = Modifier.padding(
                                horizontal = 12.dp,
                                vertical = 6.dp
                            ),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite,
                            maxLines = 1,
                            softWrap = false,
                            letterSpacing = 0.3.sp
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
