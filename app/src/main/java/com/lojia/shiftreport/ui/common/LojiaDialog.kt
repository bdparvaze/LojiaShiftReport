package com.lojia.shiftreport.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lojia.shiftreport.R
import com.lojia.shiftreport.ui.theme.Dimens

/**
 * Standard reusable modal dialog container for Lojia Shift Report.
 * - Sized for mobile phones: constrained by [maxWidth] (default [Dimens.DialogMaxWidth]) and [Dimens.DialogMaxHeight].
 * - Screen-height aware: bounded by 85% of screen height to ensure usability across all devices.
 * - Supports scrollable content when needed.
 */
@Composable
fun LojiaDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    maxWidth: Dp = Dimens.DialogMaxWidth,
    shape: Shape = RoundedCornerShape(Dimens.DialogCornerRadius),
    containerColor: Color = MaterialTheme.colorScheme.surface,
    border: BorderStroke? = null,
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false),
    content: @Composable ColumnScope.() -> Unit
) {
    val configuration = LocalConfiguration.current
    val maxDialogHeight = (configuration.screenHeightDp.dp * 0.85f)

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties
    ) {
        Surface(
            shape = shape,
            color = containerColor,
            border = border ?: BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shadowElevation = Dimens.DialogElevation,
            modifier = modifier
                .padding(horizontal = Dimens.SpacingLg)
                .widthIn(max = maxWidth)
                .fillMaxWidth()
                .heightIn(max = maxDialogHeight)
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                content()
            }
        }
    }
}

/**
 * Standard compact confirmation dialog for actions (delete, discard, alert).
 */
@Composable
fun LojiaConfirmDialog(
    title: String,
    message: String,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    confirmText: String = stringResource(R.string.action_confirm),
    dismissText: String = stringResource(R.string.action_cancel),
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    isDestructive: Boolean = false,
    confirmTestTag: String = "btn_dialog_confirm",
    dismissTestTag: String = "btn_dialog_dismiss",
    extraContent: (@Composable () -> Unit)? = null
) {
    val configuration = LocalConfiguration.current
    val maxDialogHeight = (configuration.screenHeightDp.dp * 0.85f)

    LojiaDialog(
        onDismissRequest = onDismissRequest,
        maxWidth = Dimens.DialogSmallMaxWidth,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxDialogHeight)
                .verticalScroll(rememberScrollState())
                .padding(Dimens.DialogPaddingComfortable),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMd)
        ) {
            if (icon != null) {
                Surface(
                    shape = CircleShape,
                    color = iconTint.copy(alpha = 0.12f),
                    modifier = Modifier.size(Dimens.MinTouchTarget)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(Dimens.IconLg)
                        )
                    }
                }
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            if (extraContent != null) {
                extraContent()
            }

            Spacer(modifier = Modifier.height(Dimens.SpacingXs))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDismissRequest,
                    shape = RoundedCornerShape(Dimens.RadiusMd),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(Dimens.ButtonHeightStandard)
                        .testTag(dismissTestTag)
                ) {
                    Text(
                        text = dismissText,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Button(
                    onClick = onConfirm,
                    shape = RoundedCornerShape(Dimens.RadiusMd),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(Dimens.ButtonHeightStandard)
                        .testTag(confirmTestTag)
                ) {
                    Text(
                        text = confirmText,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Standard reusable modal form dialog for dialogs with input fields.
 * - Sized for mobile phones: constrained by [maxWidth] (default [Dimens.DialogMaxWidth]) and 85% screen height.
 * - Structure: Fixed title header row, verticalScroll-able body content, and fixed bottom action row.
 */
@Composable
fun LojiaFormDialog(
    title: String,
    onDismissRequest: () -> Unit,
    confirmText: String = stringResource(R.string.action_confirm),
    onConfirm: () -> Unit,
    dismissText: String = stringResource(R.string.action_cancel),
    modifier: Modifier = Modifier,
    isConfirmEnabled: Boolean = true,
    isDestructive: Boolean = false,
    confirmTestTag: String = "btn_form_dialog_confirm",
    dismissTestTag: String = "btn_form_dialog_dismiss",
    maxWidth: Dp = Dimens.DialogMaxWidth,
    content: @Composable ColumnScope.() -> Unit
) {
    val configuration = LocalConfiguration.current
    val maxDialogHeight = (configuration.screenHeightDp.dp * 0.85f)

    LojiaDialog(
        onDismissRequest = onDismissRequest,
        maxWidth = maxWidth,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxDialogHeight)
        ) {
            // 1. Fixed Title Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = Dimens.DialogPaddingComfortable,
                        top = Dimens.DialogPaddingComfortable,
                        end = Dimens.DialogPaddingComfortable,
                        bottom = Dimens.SpacingSm
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 1.dp
            )

            // 2. Scrollable Body Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(Dimens.DialogPaddingComfortable),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMd)
            ) {
                content()
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 1.dp
            )

            // 3. Fixed Bottom Action Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.DialogPaddingComfortable),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDismissRequest,
                    shape = RoundedCornerShape(Dimens.RadiusMd),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(Dimens.ButtonHeightStandard)
                        .testTag(dismissTestTag)
                ) {
                    Text(
                        text = dismissText,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Button(
                    onClick = onConfirm,
                    enabled = isConfirmEnabled,
                    shape = RoundedCornerShape(Dimens.RadiusMd),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(Dimens.ButtonHeightStandard)
                        .testTag(confirmTestTag)
                ) {
                    Text(
                        text = confirmText,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
