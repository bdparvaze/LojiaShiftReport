package com.lojia.shiftreport.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import com.lojia.shiftreport.ui.theme.Dimens
import com.lojia.shiftreport.ui.theme.OnSurfaceLight
import com.lojia.shiftreport.ui.theme.OnSurfaceVariantLight
import com.lojia.shiftreport.ui.theme.PrimaryBlueLight
import com.lojia.shiftreport.ui.theme.PrimaryContainerLight
import com.lojia.shiftreport.ui.theme.SurfaceVariantLight
import com.lojia.shiftreport.ui.theme.TextDisabledColor
import com.lojia.shiftreport.ui.theme.TextHintColor

/**
 * Compact business info and action row used in grouped cards across Profile and Settings.
 */
@Composable
fun LojiaInfoRow(
    title: String,
    modifier: Modifier = Modifier,
    value: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = PrimaryBlueLight,
    iconContainerColor: Color = PrimaryContainerLight,
    valueMaxLines: Int = 1,
    valueFontFamily: FontFamily? = null,
    locked: Boolean = false,
    lockedLabel: String? = null,
    lockedContentDescription: String? = null,
    onCopy: (() -> Unit)? = null,
    copyContentDescription: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = Dimens.ListRowMinHeight)
            .then(
                if (onClick != null) {
                    Modifier.clickable(enabled = enabled, onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(
                horizontal = Dimens.ListRowPaddingHorizontal,
                vertical = Dimens.ListRowPaddingVertical
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(Dimens.IconTile)
                    .clip(RoundedCornerShape(Dimens.RadiusMd))
                    .background(iconContainerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) iconTint else TextDisabledColor,
                    modifier = Modifier.size(Dimens.IconMd)
                )
            }
            Spacer(modifier = Modifier.width(Dimens.SpacingMd))
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = Dimens.SpacingSm),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpacingXxs)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) OnSurfaceLight else TextDisabledColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (locked && !lockedLabel.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(Dimens.RadiusSm))
                            .background(SurfaceVariantLight)
                            .padding(
                                horizontal = Dimens.SpacingSm,
                                vertical = Dimens.SpacingXxs
                            )
                    ) {
                        Text(
                            text = lockedLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = OnSurfaceVariantLight,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            if (!value.isNullOrBlank()) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = valueFontFamily,
                    color = if (enabled) OnSurfaceVariantLight else TextDisabledColor,
                    maxLines = valueMaxLines,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (locked) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = lockedContentDescription,
                tint = TextHintColor,
                modifier = Modifier.size(Dimens.IconSm)
            )
        }

        if (onCopy != null) {
            if (locked) {
                Spacer(modifier = Modifier.width(Dimens.SpacingXs))
            }
            IconButton(
                onClick = onCopy,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = copyContentDescription,
                    tint = PrimaryBlueLight,
                    modifier = Modifier.size(Dimens.IconMd)
                )
            }
        } else if (trailing != null) {
            trailing()
        } else if (onClick != null && !locked) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = TextHintColor,
                modifier = Modifier.size(Dimens.IconMd)
            )
        }
    }
}
