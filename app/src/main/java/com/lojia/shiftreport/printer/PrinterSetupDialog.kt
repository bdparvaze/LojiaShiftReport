package com.lojia.shiftreport.printer

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lojia.shiftreport.ui.theme.Dimens
import com.lojia.shiftreport.ui.theme.depthCard

/**
 * Dialog wrapper for [PrinterSetupScreen].
 * Provides modal presentation for POS thermal printer configuration.
 */
@Composable
fun PrinterSetupDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        val shape = RoundedCornerShape(Dimens.RadiusLg)
        Surface(
            modifier = modifier
                .fillMaxSize()
                .padding(Dimens.SpacingLg)
                .depthCard(shape = shape),
            shape = shape,
            color = Color.Transparent,
            shadowElevation = 0.dp
        ) {
            PrinterSetupScreen(
                onClose = onDismiss
            )
        }
    }
}
