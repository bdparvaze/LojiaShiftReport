package com.lojia.shiftreport.auth

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Biometric authentication helper integrated with EncryptedSharedPreferences.
 */
object BiometricHelper {
    private const val KEY_BIOMETRIC_PREF_ENABLED = "biometric_auth_pref_enabled"

    fun canAuthenticate(context: Context): Int {
        val manager = BiometricManager.from(context)
        return manager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        )
    }

    fun isHardwareAvailable(context: Context): Boolean {
        return canAuthenticate(context) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun isBiometricEnabled(context: Context): Boolean {
        return SecurityHelper.getEncryptedPrefs(context).getBoolean(KEY_BIOMETRIC_PREF_ENABLED, false)
    }

    fun setBiometricEnabled(context: Context, enabled: Boolean) {
        SecurityHelper.getEncryptedPrefs(context).edit()
            .putBoolean(KEY_BIOMETRIC_PREF_ENABLED, enabled)
            .apply()
    }

    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String? = null,
        negativeButtonText: String,
        onSuccess: () -> Unit,
        onError: (errorCode: Int, errString: CharSequence) -> Unit,
        onFailed: () -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onError(errorCode, errString)
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onFailed()
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder().apply {
            setTitle(title)
            subtitle?.let { setSubtitle(it) }
            setNegativeButtonText(negativeButtonText)
            setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
            )
        }.build()

        prompt.authenticate(promptInfo)
    }
}
