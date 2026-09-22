package com.lojia.shiftreport.auth

import com.lojia.shiftreport.R
import com.lojia.shiftreport.data.*
import com.lojia.shiftreport.util.*
import com.lojia.shiftreport.ui.common.*
import com.lojia.shiftreport.ui.theme.*
import com.lojia.shiftreport.auth.*
import com.lojia.shiftreport.report.*
import com.lojia.shiftreport.settings.*




import android.content.Context

import android.os.Build


import androidx.biometric.BiometricManager


import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG


import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL


import androidx.biometric.BiometricPrompt


import androidx.core.content.ContextCompat


import androidx.fragment.app.FragmentActivity

enum class BiometricStatus {
    AVAILABLE,
    NOT_ENROLLED,
    NO_HARDWARE,
    UNAVAILABLE
}

sealed class BiometricAuthResult {
    object Success : BiometricAuthResult()
    data class Error(val errorCode: Int, val errString: CharSequence) : BiometricAuthResult()
    object Failed : BiometricAuthResult()
    object UserCanceled : BiometricAuthResult()
    object UsePinFallback : BiometricAuthResult()
}

object BiometricAuthManager {

    fun checkBiometricAvailability(context: Context): BiometricStatus {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NOT_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NO_HARDWARE
            else -> BiometricStatus.UNAVAILABLE
        }
    }

    fun getStatusDescription(status: BiometricStatus): String {
        return when (status) {
            BiometricStatus.AVAILABLE -> "Fingerprint & Face Recognition Ready"
            BiometricStatus.NOT_ENROLLED -> "No Biometrics Enrolled on Device"
            BiometricStatus.NO_HARDWARE -> "Biometric Sensor Not Detected"
            BiometricStatus.UNAVAILABLE -> "Biometrics Currently Unavailable"
        }
    }

    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String = "App Privacy & Security",
        subtitle: String = "Verify your fingerprint or face to unlock",
        description: String = "Biometric authentication secures your shift reports, sales data, and business configuration.",
        onResult: (BiometricAuthResult) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onResult(BiometricAuthResult.Success)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (errorCode == BiometricPrompt.ERROR_USER_CANCELED || errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    onResult(BiometricAuthResult.UserCanceled)
                } else if (errorCode == BiometricPrompt.ERROR_CANCELED) {
                    onResult(BiometricAuthResult.UserCanceled)
                } else {
                    onResult(BiometricAuthResult.Error(errorCode, errString))
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onResult(BiometricAuthResult.Failed)
            }
        }

        val biometricPrompt = BiometricPrompt(activity, executor, callback)

        val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setDescription(description)

        // Set allowed authenticators or negative button based on device capabilities
        val biometricManager = BiometricManager.from(activity)
        val canDeviceCred = biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && canDeviceCred) {
            promptInfoBuilder.setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
        } else {
            promptInfoBuilder.setAllowedAuthenticators(BIOMETRIC_STRONG)
            promptInfoBuilder.setNegativeButtonText(activity.getString(R.string.use_security_pin))
        }

        try {
            biometricPrompt.authenticate(promptInfoBuilder.build())
        } catch (e: Exception) {
            onResult(BiometricAuthResult.Error(-1, e.message ?: "Authentication initialization error"))
        }
    }
}
