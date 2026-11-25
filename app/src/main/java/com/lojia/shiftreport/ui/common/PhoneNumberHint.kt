package com.lojia.shiftreport.ui.common

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.auth.api.credentials.Credential
import com.google.android.gms.auth.api.credentials.Credentials
import com.google.android.gms.auth.api.credentials.HintRequest
import com.lojia.shiftreport.R

@Composable
fun rememberPhoneNumberHint(
    onPhoneSelected: (String) -> Unit,
    onError: (String) -> Unit
): () -> Unit {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                @Suppress("DEPRECATION")
                val credential = result.data?.getParcelableExtra<Credential>(
                    Credential.EXTRA_KEY
                )
                val phone = credential?.id
                if (!phone.isNullOrBlank()) {
                    onPhoneSelected(phone)
                } else {
                    onError(context.getString(R.string.phone_hint_none_found))
                }
            } catch (e: Exception) {
                onError(context.getString(R.string.phone_hint_error_fmt, e.message.orEmpty()))
            }
        }
    }

    return {
        try {
            val hintRequest = HintRequest.Builder()
                .setPhoneNumberIdentifierSupported(true)
                .build()

            val client = Credentials.getClient(context)
            val pendingIntent = client.getHintPickerIntent(hintRequest)

            launcher.launch(
                IntentSenderRequest.Builder(pendingIntent.intentSender).build()
            )
        } catch (e: Exception) {
            onError(context.getString(R.string.phone_hint_picker_unavailable_fmt, e.message.orEmpty()))
        }
    }
}

fun splitPhoneNumber(
    fullPhone: String,
    availableDialCodes: List<String>,
    defaultDialCode: String
): Pair<String, String> {
    val sorted = availableDialCodes
        .filter { it.isNotBlank() }
        .sortedByDescending { it.length }
    for (code in sorted) {
        if (fullPhone.startsWith(code)) {
            return code to fullPhone.removePrefix(code)
        }
    }
    return defaultDialCode to fullPhone
}
