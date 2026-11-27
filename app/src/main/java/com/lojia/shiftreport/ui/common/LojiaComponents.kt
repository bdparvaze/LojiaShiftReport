package com.lojia.shiftreport.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.lojia.shiftreport.ui.theme.BadgeBlueBg
import com.lojia.shiftreport.ui.theme.Dimens
import com.lojia.shiftreport.ui.theme.OnSurfaceVariantLight
import com.lojia.shiftreport.ui.theme.PrimaryBlueLight
import com.lojia.shiftreport.ui.theme.PrimaryIndigoLight
import com.lojia.shiftreport.ui.theme.PureWhite
import com.lojia.shiftreport.ui.theme.TextPrimaryReport
import com.lojia.shiftreport.ui.theme.depthCard
import com.lojia.shiftreport.ui.theme.depthRaisedButton
import com.lojia.shiftreport.ui.theme.depthRaisedIconTile
import com.lojia.shiftreport.ui.theme.depthSecondaryButton

// ---- LojiaDimens (mapped to central Dimens tokens) ----
object LojiaDimens {
    val ScreenPadding       = Dimens.SpacingLg
    val ScreenTopPadding    = Dimens.SpacingLg
    val ScreenBottomPadding = Dimens.SpacingHuge
    val SectionSpacing      = Dimens.SpacingXl
    val CardSpacing         = Dimens.SpacingSm
    val CardPadding         = Dimens.CardPadding
    val IconTextGap         = Dimens.SettingsIconTitleGap
    val CardRadius          = Dimens.CardCornerRadius
    val ButtonRadius        = Dimens.RadiusMd
    val InputRadius         = Dimens.RadiusInputRegister
    val ChipRadius          = Dimens.RadiusFull
    val DrawerHeaderRadius  = Dimens.RadiusPill
    val DrawerItemHeight    = Dimens.DrawerItemHeight
    val DrawerHeaderHeight  = Dimens.DrawerHeaderHeight
    val IconSize            = Dimens.IconLg
    val AvatarSmall         = Dimens.MinTouchTarget
    val AvatarLarge         = Dimens.ProfileAvatarSize
    val FabSize             = Dimens.FabSize
    val MinTouchTarget      = Dimens.MinTouchTarget
}

// ---- Shared grouped card wrapper ----
@Composable
fun LojiaCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    accent: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(Dimens.CardPadding),
    content: @Composable ColumnScope.() -> Unit
) {
    val cardShape = RoundedCornerShape(Dimens.CardCornerRadius)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .depthCard(accent = accent, shape = cardShape, cornerRadius = Dimens.CardCornerRadius)
            .then(
                if (onClick != null) {
                    Modifier
                        .defaultMinSize(minHeight = Dimens.MinTouchTarget)
                        .clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        shape = cardShape,
        color = Color.Transparent,
        shadowElevation = Dimens.SpacingNone
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            content = content
        )
    }
}

// ---- Section header with compact primary accent bar ----
@Composable
fun LojiaSectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    uppercase: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = Dimens.SpacingMd, bottom = Dimens.SpacingXs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = Dimens.AccentBarWidth, height = Dimens.AccentBarHeight)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(Dimens.AccentBarCorner)
                )
        )
        Spacer(modifier = Modifier.width(Dimens.SpacingSm))
        Text(
            text = if (uppercase) text.uppercase() else text,
            style = MaterialTheme.typography.titleSmall,
            color = OnSurfaceVariantLight,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ---- Settings card wrapper ----
@Composable
fun LojiaSettingsCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    val cardShape = RoundedCornerShape(LojiaDimens.CardRadius)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = Dimens.SpacingSm)
            .defaultMinSize(minHeight = Dimens.MinTouchTarget)
            .depthCard(shape = cardShape, cornerRadius = LojiaDimens.CardRadius)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.SpacingNone),
        border = null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.CardPadding),
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
    tileColors: List<Color> = listOf(PureWhite, BadgeBlueBg),
    iconTint: Color = PrimaryBlueLight,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    LojiaSettingsCard(onClick = onClick) {
        Box(
            modifier = Modifier
                .size(Dimens.SettingsIconTileSize)
                .depthRaisedIconTile(
                    gradientColors = tileColors,
                    cornerRadius = Dimens.RadiusSettingsIconTile
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(Dimens.IconField)
            )
        }
        Spacer(Modifier.width(Dimens.SettingsIconTitleGap))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimaryReport
            )
            if (subtitle != null) {
                Spacer(Modifier.height(Dimens.SpacingXxs))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariantLight
                )
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
    val buttonShape = RoundedCornerShape(LojiaDimens.ButtonRadius)
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.ButtonHeightPrimary)
            .depthRaisedButton(enabled = enabled, cornerRadius = LojiaDimens.ButtonRadius),
        enabled = enabled,
        shape = buttonShape,
        elevation = null,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            contentColor = PureWhite,
            disabledContentColor = OnSurfaceVariantLight
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
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
    val buttonShape = RoundedCornerShape(LojiaDimens.ButtonRadius)
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = Dimens.ButtonHeightPrimary)
            .depthSecondaryButton(
                enabled = true,
                borderColor = PrimaryIndigoLight,
                cornerRadius = LojiaDimens.ButtonRadius
            ),
        shape = buttonShape,
        border = null,
        elevation = null,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            contentColor = PrimaryIndigoLight
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}
