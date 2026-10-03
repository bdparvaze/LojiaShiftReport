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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lojia.shiftreport.ui.theme.Dimens
import com.lojia.shiftreport.ui.theme.PrimaryIndigoLight
import com.lojia.shiftreport.ui.theme.PureWhite

/**
 * Standard reusable modal dialog container for Lojia Shift Report.
 * - Sized for mobile phones: constrained by [maxWidth] (default 400.dp) and [Dimens.SpacingLg] margin.
 * - Prefers [wrapContentHeight] instead of forced large screen fractions.
 * - Supports scrollable content when needed.
 */
@Composable
fun LojiaDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    maxWidth: Dp = Dimens.DialogMaxWidth,
    shape: Shape = RoundedCornerShape(Dimens.DialogCornerRadius),
    containerColor: Color = PureWhite,
    border: BorderStroke? = BorderStroke(1.dp, Color(0xFFE2E8F0)),
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false),
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties
    ) {
        Surface(
            shape = shape,
            color = containerColor,
            border = border,
            shadowElevation = Dimens.DialogElevation,
            modifier = modifier
                .padding(horizontal = Dimens.SpacingLg)
                .widthIn(max = maxWidth)
                .fillMaxWidth()
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
    confirmText: String = "Confirm",
    dismissText: String = "Cancel",
    icon: ImageVector? = null,
    iconTint: Color = PrimaryIndigoLight,
    isDestructive: Boolean = false,
    confirmTestTag: String = "btn_dialog_confirm",
    dismissTestTag: String = "btn_dialog_dismiss",
    extraContent: (@Composable () -> Unit)? = null
) {
    LojiaDialog(
        onDismissRequest = onDismissRequest,
        maxWidth = Dimens.DialogSmallMaxWidth,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(Dimens.DialogPaddingComfortable),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMd)
        ) {
            if (icon != null) {
                Surface(
                    shape = CircleShape,
                    color = iconTint.copy(alpha = 0.12f),
                    modifier = Modifier.size(48.dp)
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
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = message,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF475569),
                lineHeight = 18.sp,
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
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = PureWhite,
                        contentColor = Color(0xFF475569)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(Dimens.ButtonHeightStandard)
                        .testTag(dismissTestTag)
                ) {
                    Text(
                        text = dismissText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Button(
                    onClick = onConfirm,
                    shape = RoundedCornerShape(Dimens.RadiusMd),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDestructive) Color(0xFFDC2626) else PrimaryIndigoLight,
                        contentColor = PureWhite
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(Dimens.ButtonHeightStandard)
                        .testTag(confirmTestTag)
                ) {
                    Text(
                        text = confirmText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
