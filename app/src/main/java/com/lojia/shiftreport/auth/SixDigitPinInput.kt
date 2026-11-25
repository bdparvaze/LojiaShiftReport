package com.lojia.shiftreport.auth

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lojia.shiftreport.ui.common.PinCells
import com.lojia.shiftreport.ui.common.PinCellsState

/**
 * Compatibility forwarding wrapper around shared [PinCells].
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
    PinCells(
        value = pin,
        onValueChange = onPinChange,
        modifier = modifier,
        cellCount = AuthPinConstants.PIN_LENGTH,
        masked = isMasked,
        state = if (isError) PinCellsState.Error else PinCellsState.Normal,
        autoFocus = autoFocus,
        onDone = onImeAction
    )
}
