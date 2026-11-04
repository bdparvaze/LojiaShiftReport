package com.lojia.shiftreport.auth

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Reusable 6-digit PIN input composable conforming to Material 3 POS and Banking standards.
 * - 6 individual 48dp x 48dp boxes in a Row (minimum 48dp touch targets).
 * - Outlined container styling with active, filled, and error state transitions.
 * - Numeric keyboard only, auto-advancing across cells.
 * - Dot masking option for privacy.
 */
@Composable
fun SixDigitPinInput(
    pin: String,
    onPinChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isMasked: Boolean = true,
    isError: Boolean = false,
    autoFocus: Boolean = true,
    onImeAction: () -> Unit = {}
) {
    val focusRequester = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }

    LaunchedEffect(Unit) {
        if (autoFocus) {
            try {
                focusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    Box(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                try {
                    focusRequester.requestFocus()
                } catch (_: Exception) {}
            },
        contentAlignment = Alignment.Center
    ) {
        // Transparent soft keyboard driver
        BasicTextField(
            value = pin,
            onValueChange = { input ->
                val clean = input.filter { it.isDigit() }.take(AuthPinConstants.PIN_LENGTH)
                onPinChange(clean)
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { onImeAction() }
            ),
            cursorBrush = SolidColor(Color.Transparent),
            modifier = Modifier
                .size(1.dp)
                .focusRequester(focusRequester)
                .testTag("six_digit_hidden_input")
        )

        // Visual 6-box OTP layout
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until AuthPinConstants.PIN_LENGTH) {
                if (i > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                }

                val isFilled = i < pin.length
                val isCurrent = i == pin.length

                val borderColor by animateColorAsState(
                    targetValue = when {
                        isError -> MaterialTheme.colorScheme.error
                        isCurrent -> MaterialTheme.colorScheme.primary
                        isFilled -> MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                        else -> MaterialTheme.colorScheme.outline
                    },
                    animationSpec = tween(150),
                    label = "pin_box_border"
                )

                val containerColor by animateColorAsState(
                    targetValue = when {
                        isError -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                        isCurrent -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                        isFilled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        else -> MaterialTheme.colorScheme.surface
                    },
                    animationSpec = tween(150),
                    label = "pin_box_container"
                )

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(containerColor)
                        .border(
                            width = if (isCurrent) 2.dp else 1.5.dp,
                            color = borderColor,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .testTag("pin_box_$i"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isFilled) {
                        if (isMasked) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isError) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                            )
                        } else {
                            Text(
                                text = pin[i].toString(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else if (isCurrent) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(20.dp)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }
        }
    }
}
