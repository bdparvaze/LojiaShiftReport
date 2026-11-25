package com.lojia.shiftreport.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import com.lojia.shiftreport.R
import com.lojia.shiftreport.auth.AuthPinConstants
import com.lojia.shiftreport.ui.theme.Dimens
import com.lojia.shiftreport.ui.theme.ErrorRedLight
import com.lojia.shiftreport.ui.theme.OnSurfaceVariantLight
import com.lojia.shiftreport.ui.theme.SuccessGreen
import com.lojia.shiftreport.ui.theme.SuccessTextDark
import com.lojia.shiftreport.ui.theme.depthInset

/**
 * Visual & interactive state for [PinCells].
 */
enum class PinCellsState {
    Normal,
    Error,
    Success,
    Locked
}

/**
 * Computes responsive circular PIN cell diameter from [availableWidth] and [cellCount],
 * subtracting gaps and clamping strictly within [[minCellSize], [maxCellSize]].
 */
fun calculatePinCellSize(
    availableWidth: Dp,
    cellCount: Int,
    gap: Dp = Dimens.PinCellGap,
    minCellSize: Dp = Dimens.PinCellMin,
    maxCellSize: Dp = Dimens.PinCellMax
): Dp {
    if (cellCount <= 0) return minCellSize
    val raw = (availableWidth - gap * (cellCount - 1)) / cellCount
    return raw.coerceIn(minCellSize, maxCellSize)
}

/**
 * Single shared circular PIN entry row for dialogs and forms.
 * Always lays out left-to-right regardless of RTL locale.
 */
@Composable
fun PinCells(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    cellCount: Int = AuthPinConstants.PIN_LENGTH,
    masked: Boolean = true,
    state: PinCellsState = PinCellsState.Normal,
    autoFocus: Boolean = false,
    focusRequester: FocusRequester = remember { FocusRequester() },
    onDone: () -> Unit = {}
) {
    val isEditable = state != PinCellsState.Locked && state != PinCellsState.Success
    val interactionSource = remember { MutableInteractionSource() }
    val cdText = stringResource(R.string.pin_cells_cd_fmt, value.length, cellCount)

    LaunchedEffect(autoFocus, state) {
        if (autoFocus && isEditable) {
            runCatching { focusRequester.requestFocus() }
        }
    }

    Box(
        modifier = modifier
            .heightIn(min = Dimens.MinTouchTarget)
            .clickable(
                enabled = isEditable,
                interactionSource = interactionSource,
                indication = null
            ) {
                runCatching { focusRequester.requestFocus() }
            }
            .semantics(mergeDescendants = true) {
                contentDescription = cdText
            },
        contentAlignment = Alignment.Center
    ) {
        BasicTextField(
            value = value,
            onValueChange = { input ->
                if (isEditable) {
                    val clean = input.filter { it.isDigit() }.take(cellCount)
                    onValueChange(clean)
                }
            },
            enabled = isEditable,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { onDone() }
            ),
            cursorBrush = SolidColor(Color.Transparent),
            modifier = Modifier
                .size(Dimens.FlatBorderWidth)
                .focusRequester(focusRequester)
                .testTag("six_digit_hidden_input")
        )

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Dimens.MinTouchTarget),
                contentAlignment = Alignment.Center
            ) {
                val cellSize = calculatePinCellSize(
                    availableWidth = maxWidth,
                    cellCount = cellCount
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(
                        space = Dimens.PinCellGap,
                        alignment = Alignment.CenterHorizontally
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isError = state == PinCellsState.Error
                    val isSuccess = state == PinCellsState.Success
                    val isLocked = state == PinCellsState.Locked

                    for (i in 0 until cellCount) {
                        val isFilled = i < value.length
                        val isActive = isEditable && i == value.length
                        val borderColorOverride: Color? = when {
                            isSuccess -> SuccessGreen
                            isFilled && !isError && !isActive && !isLocked -> MaterialTheme.colorScheme.primary
                            else -> null
                        }

                        Box(
                            modifier = Modifier
                                .size(cellSize)
                                .depthInset(
                                    enabled = !isLocked,
                                    isError = isError,
                                    isFocused = isActive || isFilled,
                                    shape = CircleShape,
                                    borderColorOverride = borderColorOverride
                                )
                                .testTag("pin_box_$i"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isFilled) {
                                if (masked) {
                                    Box(
                                        modifier = Modifier
                                            .size(Dimens.PinDotSize)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isSuccess -> SuccessTextDark
                                                    isError -> ErrorRedLight
                                                    isLocked -> OnSurfaceVariantLight
                                                    else -> MaterialTheme.colorScheme.onSurface
                                                }
                                            )
                                    )
                                } else {
                                    Text(
                                        text = value[i].toString(),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            isSuccess -> SuccessTextDark
                                            isError -> ErrorRedLight
                                            isLocked -> OnSurfaceVariantLight
                                            else -> MaterialTheme.colorScheme.onSurface
                                        },
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else if (isActive) {
                                Box(
                                    modifier = Modifier
                                        .width(Dimens.PinCursorWidth)
                                        .height(Dimens.PinCursorHeight)
                                        .background(
                                            if (isError) ErrorRedLight else MaterialTheme.colorScheme.primary
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
