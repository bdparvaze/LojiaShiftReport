package com.lojia.shiftreport.util

import android.content.Context
import android.telephony.TelephonyManager
import com.lojia.shiftreport.auth.LOJIA_COUNTRIES
import com.lojia.shiftreport.auth.LojiaCountry
import com.lojia.shiftreport.data.AppCountry
import java.util.Locale

object CountryDetector {

    /**
     * Detects the user's country ISO code (2-letter uppercase, e.g. "SA", "BD", "US")
     * in prioritized order:
     * 1. TelephonyManager SIM country ISO
     * 2. TelephonyManager Network country ISO
     * 3. Device System Default Locale country
     */
    fun detectCountryIso(context: Context): String {
        try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager

            // 1. SIM Country ISO
            val simCountry = tm?.simCountryIso?.trim()?.uppercase(Locale.US)
            if (!simCountry.isNullOrEmpty() && simCountry.length == 2) {
                return simCountry
            }

            // 2. Network Country ISO
            val netCountry = tm?.networkCountryIso?.trim()?.uppercase(Locale.US)
            if (!netCountry.isNullOrEmpty() && netCountry.length == 2) {
                return netCountry
            }
        } catch (_: Exception) {}

        // 3. System Locale Fallback
        try {
            val localeCountry = Locale.getDefault().country?.trim()?.uppercase(Locale.US)
            if (!localeCountry.isNullOrEmpty() && localeCountry.length == 2) {
                return localeCountry
            }
        } catch (_: Exception) {}

        // Fallback to primary locale configuration
        try {
            val configCountry = context.resources.configuration.locales[0]?.country?.trim()?.uppercase(Locale.US)
            if (!configCountry.isNullOrEmpty() && configCountry.length == 2) {
                return configCountry
            }
        } catch (_: Exception) {}

        return AppConstants.DEFAULT_COUNTRY_CODE
    }

    /**
     * Maps the detected country ISO code to a LojiaCountry from LOJIA_COUNTRIES list.
     */
    fun detectDefaultLojiaCountry(context: Context): LojiaCountry {
        val iso = detectCountryIso(context)
        val match = LOJIA_COUNTRIES.find { it.code.equals(iso, ignoreCase = true) }
        if (match != null) return match

        return LOJIA_COUNTRIES.find {
            it.code.equals(AppConstants.DEFAULT_COUNTRY_CODE, ignoreCase = true)
        } ?: LOJIA_COUNTRIES.first()
    }

    /**
     * Maps the detected country ISO code to an AppCountry enum.
     */
    fun detectDefaultAppCountry(context: Context): AppCountry {
        val iso = detectCountryIso(context)
        return AppCountry.fromCode(iso)
    }
}
