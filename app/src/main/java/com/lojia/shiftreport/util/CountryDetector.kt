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
        var simCountry: String? = null
        var netCountry: String? = null
        try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            simCountry = tm?.simCountryIso
            netCountry = tm?.networkCountryIso
        } catch (_: Exception) {}

        val localeCountry = try {
            val defaultLocaleCountry = Locale.getDefault().country
            if (!defaultLocaleCountry.isNullOrBlank() && defaultLocaleCountry.trim().length == 2) {
                defaultLocaleCountry
            } else {
                context.resources.configuration.locales[0]?.country
            }
        } catch (_: Exception) {
            null
        }

        return resolveCountryIso(
            simIso = simCountry,
            networkIso = netCountry,
            localeIso = localeCountry,
            fallbackIso = AppConstants.DEFAULT_COUNTRY_CODE
        )
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
