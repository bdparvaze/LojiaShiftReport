package com.lojia.shiftreport.printer

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.DialogProperties
import com.lojia.shiftreport.ui.common.DialogSize
import com.lojia.shiftreport.ui.common.LojiaDialog
import com.lojia.shiftreport.ui.theme.Dimens

/**
 * Dialog wrapper for [PrinterSetupScreen].
 * Provides modal presentation for POS thermal printer configuration using [LojiaDialog] tokens.
 */
@Composable
fun PrinterSetupDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    LojiaDialog(
        onDismissRequest = onDismiss,
        size = DialogSize.Large,
        contentPadding = PaddingValues(Dimens.SpacingNone),
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        ),
        modifier = modifier
    ) {
        PrinterSetupScreen(
            onClose = onDismiss,
            modifier = Modifier.fillMaxSize()
        )
    }
}
