package com.lojia.pos.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojia.pos.R
import com.lojia.pos.ui.common.AutoText

/**
 * Standard Loyverse POS list menu row with left icon, clean typography, and thin divider.
 */
@Composable
fun LoyverseMenuItemRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color(0xFF616161),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(28.dp))
            Column(modifier = Modifier.weight(1f)) {
                AutoText(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF212121)
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    AutoText(
                        text = subtitle,
                        fontSize = 13.sp,
                        color = Color(0xFF757575)
                    )
                }
            }
            if (trailing != null) {
                trailing()
            }
        }
        HorizontalDivider(
            color = Color(0xFFEEEEEE),
            thickness = 0.8.dp,
            modifier = Modifier.padding(start = 72.dp)
        )
    }
}

/**
 * Clean, interactive breadcrumb navigation indicator for settings sub-menus.
 * Provides direct one-tap return to the root settings menu and clear hierarchical context.
 */
@Composable
fun SettingsNavigationIndicator(
    parentTitle: String,
    currentTitle: String,
    onBackToRoot: () -> Unit,
    accentColor: Color = Color(0xFF10B981)
) {
    Surface(
        color = Color(0xFFF8FAFC),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onBackToRoot() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = accentColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_to_settings),
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    AutoText(
                        text = parentTitle,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = accentColor
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier
                            .padding(horizontal = 6.dp)
                            .size(11.dp)
                    )
                    AutoText(
                        text = currentTitle,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                }
            }

            Surface(
                color = accentColor.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    AutoText(
                        text = parentTitle,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = accentColor
                    )
                }
            }
        }
    }
}
