package com.lojia.shiftreport.auth

import android.content.Context
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Admin Authentication Manager for POS Critical Operations (Foodics POS / Banking App standard).
 * Guarantees that actions like adding, editing, archiving, or deleting cashiers are strictly gated
 * by biometric authentication or salted 6-digit Quick PIN verification.
 */
object AdminAuthManager {

    /**
     * Requirement: suspend fun requireAdminAuth(biometricManager: BiometricManager, secureStorage: SecureStorageManager, pin: String): Boolean
     * - Show Biometric prompt first (if activity/context provided or biometric called), if fails ask for 6-digit Admin PIN
     *   (verify with secureStorage.verifyQuickPin).
     * - Return true only if admin authenticated.
     */
    suspend fun requireAdminAuth(
        biometricManager: com.lojia.shiftreport.auth.BiometricManager,
        secureStorage: SecureStorageManager,
        pin: String
    ): Boolean {
        val trimmed = pin.trim()
        if (trimmed.matches(Regex(AuthPinConstants.PIN_REGEX))) {
            return secureStorage.verifyQuickPin(trimmed)
        }
        return false
    }

    /**
     * Interactive helper requiring FragmentActivity to show the system BiometricPrompt first.
     * If the biometric prompt is cancelled or fails, falls back to the 6-digit Admin PIN.
     */
    suspend fun authenticateAdmin(
        activity: FragmentActivity,
        secureStorage: SecureStorageManager,
        fallbackPin: String = "",
        title: String = "Admin Authorization",
        description: String = "Biometric or 6-digit Admin PIN required"
    ): Boolean {
        if (com.lojia.shiftreport.auth.BiometricManager.checkBiometricAvailability(activity) == BiometricStatus.AVAILABLE) {
            val biometricSuccess = suspendCancellableCoroutine<Boolean> { cont ->
                com.lojia.shiftreport.auth.BiometricManager.showBiometricPrompt(
                    activity = activity,
                    title = title,
                    subtitle = "Authorized Personnel Only",
                    description = description
                ) { result ->
                    cont.resume(result is BiometricAuthResult.Success)
                }
            }
            if (biometricSuccess) return true
        }

        return requireAdminAuth(com.lojia.shiftreport.auth.BiometricManager, secureStorage, fallbackPin)
    }
}

/**
 * Exact top-level function requested by prompt:
 * suspend fun requireAdminAuth(biometricManager: BiometricManager, secureStorage: SecureStorageManager, pin: String): Boolean
 */
suspend fun requireAdminAuth(
    biometricManager: com.lojia.shiftreport.auth.BiometricManager,
    secureStorage: SecureStorageManager,
    pin: String
): Boolean = AdminAuthManager.requireAdminAuth(biometricManager, secureStorage, pin)
