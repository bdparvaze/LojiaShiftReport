package com.lojia.shiftreport.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecurityHelper(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_auth_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveAuthToken(token: String) {
        sharedPreferences.edit().putString("auth_token", token).apply()
    }

    fun getAuthToken(): String? {
        return sharedPreferences.getString("auth_token", null)
    }

    fun saveQuickPin(pin: String) {
        sharedPreferences.edit().putString("quick_pin", pin).apply()
    }

    fun getQuickPin(): String? {
        return sharedPreferences.getString("quick_pin", null)
    }

    fun setBiometricEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("biometric_enabled", enabled).apply()
    }

    fun isBiometricEnabled(): Boolean {
        return sharedPreferences.getBoolean("biometric_enabled", false)
    }

    fun setQuickPinEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("quick_pin_enabled", enabled).apply()
    }

    fun isQuickPinEnabled(): Boolean {
        return sharedPreferences.getBoolean("quick_pin_enabled", false)
    }

    fun clearSecureData() {
        sharedPreferences.edit().clear().apply()
    }
}
