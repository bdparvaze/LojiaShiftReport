package com.lojia.shiftreport.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojia.shiftreport.ui.theme.OnSurfaceLight
import com.lojia.shiftreport.ui.theme.OnSurfaceVariantLight
import com.lojia.shiftreport.ui.theme.OutlineLight
import com.lojia.shiftreport.ui.theme.PrimaryIndigoLight
import com.lojia.shiftreport.ui.theme.SurfaceLight
import com.lojia.shiftreport.ui.theme.TextHintColor

// ---- LojiaDimens (as an object) ----
object LojiaDimens {
    val ScreenPadding       = 16.dp
    val ScreenTopPadding    = 16.dp
    val ScreenBottomPadding = 32.dp
    val SectionSpacing      = 24.dp
    val CardSpacing         = 8.dp
    val CardPadding         = 16.dp
    val IconTextGap         = 16.dp
    val CardRadius          = 12.dp
    val ButtonRadius        = 12.dp
    val InputRadius         = 10.dp
    val ChipRadius          = 999.dp
    val DrawerHeaderRadius  = 24.dp
    val DrawerItemHeight    = 52.dp
    val DrawerHeaderHeight  = 180.dp
    val IconSize            = 24.dp
    val AvatarSmall         = 48.dp
    val AvatarLarge         = 96.dp
    val FabSize             = 56.dp
    val MinTouchTarget      = 48.dp
}

// ---- Section header ----
@Composable
fun LojiaSectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 8.dp),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.15.sp,
        color = TextHintColor
    )
}

// ---- Settings card wrapper ----
@Composable
fun LojiaSettingsCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, OutlineLight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

// ---- Settings item row (icon + title + subtitle + trailing) ----
@Composable
fun LojiaSettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    LojiaSettingsCard(onClick = onClick) {
        Icon(
            icon,
            contentDescription = title,
            tint = OnSurfaceVariantLight,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = OnSurfaceLight
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, fontSize = 13.sp, color = OnSurfaceVariantLight)
            }
        }
        if (trailing != null) trailing()
    }
}

// ---- Primary button ----
@Composable
fun LojiaPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = PrimaryIndigoLight,
            contentColor = Color.White
        )
    ) {
        Text(
            text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// ---- Outlined button ----
@Composable
fun LojiaOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, PrimaryIndigoLight),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = PrimaryIndigoLight
        )
    ) {
        Text(
            text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
