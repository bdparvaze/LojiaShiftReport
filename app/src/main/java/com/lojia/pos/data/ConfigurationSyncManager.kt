package com.lojia.pos.data

import com.lojia.pos.BuildConfig


import android.content.Context

import android.content.SharedPreferences

import android.util.Log


import kotlinx.coroutines.CoroutineScope


import kotlinx.coroutines.Dispatchers


import kotlinx.coroutines.SupervisorJob


import kotlinx.coroutines.flow.*


import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import com.lojia.pos.util.SecurityUtils

/**
 * Sync status descriptor for UI feedback across modules
 */
data class ConfigurationSyncState(
    val isSynchronized: Boolean = true,
    val lastSyncTime: Long = System.currentTimeMillis(),
    val totalCashiers: Int = 0,
    val activeLanguage: AppLanguage = AppLanguage.ENGLISH,
    val activeCountry: AppCountry = AppCountry.SAUDI_ARABIA,
    val activeCashier: String = "Lojia Manager",
    val syncStatusMessage: String = "All settings synchronized across modules"
)

/**
 * Centralized Configuration Synchronization Manager.
 * Ensures that language preferences, country/currency, active module state, and cashier rosters
 * remain 100% consistent across different application modules (POS, Shift Reports,
 * Settings, Cash Management) and persist across app sessions / restarts.
 */
class ConfigurationSyncManager private constructor(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val db = AppDatabase.getInstance(context)
    private val reportDao = db.reportDao()
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // 1. Language State
    private val _currentLanguage = MutableStateFlow(loadInitialLanguage())
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    // 2. Country & Currency State
    private val _currentCountry = MutableStateFlow(loadInitialCountry())
    val currentCountry: StateFlow<AppCountry> = _currentCountry.asStateFlow()

    // 3. Cashier Roster State
    val cashiers: StateFlow<List<Cashier>> = reportDao.getAllCashiers()
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    // 4. Active Cashier Selection State
    private val _activeCashier = MutableStateFlow(loadInitialActiveCashier())
    val activeCashier: StateFlow<String> = _activeCashier.asStateFlow()

    // 5. Current App Module State
    private val _currentModule = MutableStateFlow(loadInitialModule())
    val currentModule: StateFlow<AppModule> = _currentModule.asStateFlow()

    // 6. Sync State
    private val _syncState = MutableStateFlow(
        ConfigurationSyncState(
            isSynchronized = true,
            lastSyncTime = System.currentTimeMillis(),
            totalCashiers = 0,
            activeLanguage = _currentLanguage.value,
            activeCountry = _currentCountry.value,
            activeCashier = _activeCashier.value
        )
    )
    val syncState: StateFlow<ConfigurationSyncState> = _syncState.asStateFlow()

    init {
        // Initial setup and DB sync verification
        scope.launch {
            verifyAndSeedDefaultCashiers()
            syncStoredSettingsFromDb()
            observeDatabaseSettings()
        }

        // Keep sync state updated as cashiers / language / country change
        scope.launch {
            combine(_currentLanguage, _currentCountry, cashiers, _activeCashier) { lang, country, cashierList, activeCas ->
                ConfigurationSyncState(
                    isSynchronized = true,
                    lastSyncTime = System.currentTimeMillis(),
                    totalCashiers = cashierList.size,
                    activeLanguage = lang,
                    activeCountry = country,
                    activeCashier = activeCas,
                    syncStatusMessage = "Synchronized: ${cashierList.size} Cashiers • ${lang.displayName} • ${country.flag} ${country.currencyCode}"
                )
            }.collect { newState ->
                _syncState.value = newState
            }
        }
    }

    private fun loadInitialLanguage(): AppLanguage {
        val code = prefs.getString(KEY_SELECTED_LANGUAGE, AppLanguage.ENGLISH.code) ?: AppLanguage.ENGLISH.code
        return AppLanguage.fromCode(code)
    }

    private fun loadInitialCountry(): AppCountry {
        val code = prefs.getString(KEY_SELECTED_COUNTRY, AppCountry.SAUDI_ARABIA.code) ?: AppCountry.SAUDI_ARABIA.code
        return AppCountry.fromCode(code)
    }

    private fun loadInitialActiveCashier(): String {
        return prefs.getString(KEY_ACTIVE_CASHIER, "Lojia Manager") ?: "Lojia Manager"
    }

    private fun loadInitialModule(): AppModule {
        return AppModule.SHIFT_REPORT
    }

    private suspend fun syncStoredSettingsFromDb() {
        try {
            // Restore language from Room DB if present
            val dbLangCode = reportDao.getSetting(KEY_SELECTED_LANGUAGE)
            if (!dbLangCode.isNullOrBlank()) {
                val dbLang = AppLanguage.fromCode(dbLangCode)
                if (dbLang != _currentLanguage.value) {
                    _currentLanguage.value = dbLang
                    prefs.edit().putString(KEY_SELECTED_LANGUAGE, dbLang.code).apply()
                }
            } else {
                // Seed current language to DB
                reportDao.setSetting(AppSetting(KEY_SELECTED_LANGUAGE, _currentLanguage.value.code))
            }

            // Restore country from Room DB if present
            val dbCountryCode = reportDao.getSetting(KEY_SELECTED_COUNTRY)
            if (!dbCountryCode.isNullOrBlank()) {
                val dbCountry = AppCountry.fromCode(dbCountryCode)
                if (dbCountry != _currentCountry.value) {
                    _currentCountry.value = dbCountry
                    prefs.edit().putString(KEY_SELECTED_COUNTRY, dbCountry.code).apply()
                }
            } else {
                reportDao.setSetting(AppSetting(KEY_SELECTED_COUNTRY, _currentCountry.value.code))
            }

            // Restore active cashier from Room DB if present
            val dbCashier = reportDao.getSetting(KEY_ACTIVE_CASHIER)
            if (!dbCashier.isNullOrBlank()) {
                if (dbCashier != _activeCashier.value) {
                    _activeCashier.value = dbCashier
                    prefs.edit().putString(KEY_ACTIVE_CASHIER, dbCashier).apply()
                }
            } else {
                reportDao.setSetting(AppSetting(KEY_ACTIVE_CASHIER, _activeCashier.value))
            }

            // Restore active module
            val dbModule = reportDao.getSetting(KEY_ACTIVE_MODULE)
            if (!dbModule.isNullOrBlank()) {
                val mod = AppModule.SHIFT_REPORT
                if (mod != _currentModule.value) {
                    _currentModule.value = mod
                    prefs.edit().putString(KEY_ACTIVE_MODULE, mod.name).apply()
                }
            } else {
                reportDao.setSetting(AppSetting(KEY_ACTIVE_MODULE, _currentModule.value.name))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing stored settings from DB", e)
        }
    }

    private fun observeDatabaseSettings() {
        scope.launch {
            reportDao.getAllSettings().collect { settingsList ->
                settingsList.forEach { setting ->
                    when (setting.key) {
                        KEY_SELECTED_LANGUAGE -> {
                            val lang = AppLanguage.fromCode(setting.value)
                            if (lang != _currentLanguage.value) {
                                _currentLanguage.value = lang
                                prefs.edit().putString(KEY_SELECTED_LANGUAGE, lang.code).apply()
                            }
                        }
                        KEY_SELECTED_COUNTRY -> {
                            val country = AppCountry.fromCode(setting.value)
                            if (country != _currentCountry.value) {
                                _currentCountry.value = country
                                prefs.edit().putString(KEY_SELECTED_COUNTRY, country.code).apply()
                            }
                        }
                        KEY_ACTIVE_CASHIER -> {
                            if (setting.value.isNotBlank() && setting.value != _activeCashier.value) {
                                _activeCashier.value = setting.value
                                prefs.edit().putString(KEY_ACTIVE_CASHIER, setting.value).apply()
                            }
                        }
                        KEY_ACTIVE_MODULE -> {
                            val mod = AppModule.SHIFT_REPORT
                            if (mod != _currentModule.value) {
                                _currentModule.value = mod
                                prefs.edit().putString(KEY_ACTIVE_MODULE, mod.name).apply()
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun verifyAndSeedDefaultCashiers() {
        if (!BuildConfig.DEBUG) return
        try {
            val count = reportDao.getCashierCount()
            if (count == 0) {
                val defaults = listOf(
                    Cashier(name = "Lojia Manager", pin = SecurityUtils.hashSecret("1234"), role = "ADMIN"),
                    Cashier(name = "Ahmed Al-Harbi", pin = SecurityUtils.hashSecret("1111"), role = "CASHIER"),
                    Cashier(name = "Fahad Al-Otaibi", pin = SecurityUtils.hashSecret("2222"), role = "CASHIER"),
                    Cashier(name = "Sultan Al-Ghamdi", pin = SecurityUtils.hashSecret("3333"), role = "CASHIER")
                )
                defaults.forEach { reportDao.insertCashier(it) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying cashiers", e)
        }
    }

    /**
     * Change and synchronize language setting across all modules and persist to DB & SharedPreferences.
     */
    fun setLanguage(language: AppLanguage) {
        _currentLanguage.value = language
        prefs.edit().putString(KEY_SELECTED_LANGUAGE, language.code).apply()
        com.lojia.pos.util.LanguagePreferences.saveLanguage(context, language.code)
        scope.launch {
            reportDao.setSetting(AppSetting(KEY_SELECTED_LANGUAGE, language.code))
            reportDao.insertAuditLog(
                AuditLog(
                    username = _activeCashier.value,
                    action = "LANGUAGE_CHANGE",
                    details = "Language synchronized to ${language.displayName} (${language.code})"
                )
            )
        }
    }

    /**
     * Change and synchronize country and currency setting across all modules, persist to DB & SharedPreferences,
     * and automatically update the BusinessProfile currency and VAT rate.
     */
    fun setCountry(country: AppCountry) {
        _currentCountry.value = country
        prefs.edit().putString(KEY_SELECTED_COUNTRY, country.code).apply()
        scope.launch {
            reportDao.setSetting(AppSetting(KEY_SELECTED_COUNTRY, country.code))
            // Automatically update BusinessProfile with the selected country and currency
            val currentProfile = reportDao.getBusinessProfileOnce() ?: BusinessProfile()
            val updatedProfile = currentProfile.copy(
                country = country.displayNameEn,
                currency = country.currencyCode,
                vatRate = country.defaultVatRate
            )
            reportDao.saveBusinessProfile(updatedProfile)
            reportDao.insertAuditLog(
                AuditLog(
                    username = _activeCashier.value,
                    action = "COUNTRY_CHANGE",
                    details = "Country and Currency synchronized to ${country.displayNameEn} (${country.currencyCode} ${country.currencySymbol})"
                )
            )
        }
    }

    /**
     * Change and synchronize active cashier across POS, Shift Reports, and Sessions.
     */
    fun setActiveCashier(cashierName: String) {
        val trimmed = cashierName.trim()
        if (trimmed.isBlank()) return
        _activeCashier.value = trimmed
        prefs.edit().putString(KEY_ACTIVE_CASHIER, trimmed).apply()
        scope.launch {
            reportDao.setSetting(AppSetting(KEY_ACTIVE_CASHIER, trimmed))
        }
    }

    /**
     * Switch application module (POS Shopping vs Shift Reports).
     */
    fun switchModule(module: AppModule) {
        _currentModule.value = module
        prefs.edit().putString(KEY_ACTIVE_MODULE, module.name).apply()
        scope.launch {
            reportDao.setSetting(AppSetting(KEY_ACTIVE_MODULE, module.name))
        }
    }

    /**
     * Add new cashier to the centralized database roster.
     */
    fun addCashier(name: String, pin: String = "1111", role: String = "CASHIER", onComplete: (() -> Unit)? = null) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return
        val hashedPin = SecurityUtils.hashSecret(pin)
        scope.launch {
            val existing = reportDao.getCashierByName(cleanName)
            if (existing == null) {
                reportDao.insertCashier(
                    Cashier(
                        name = cleanName,
                        pin = hashedPin,
                        role = role.ifBlank { "CASHIER" },
                        active = true
                    )
                )
                reportDao.insertAuditLog(
                    AuditLog(
                        username = _activeCashier.value,
                        action = "ADD_CASHIER",
                        details = "Added cashier $cleanName with role $role"
                    )
                )
            }
            onComplete?.invoke()
        }
    }

    /**
     * Update cashier status or details in centralized roster.
     */
    fun updateCashier(cashier: Cashier, onComplete: (() -> Unit)? = null) {
        scope.launch {
            reportDao.insertCashier(cashier)
            reportDao.insertAuditLog(
                AuditLog(
                    username = _activeCashier.value,
                    action = if (cashier.active) "ACTIVATE_CASHIER" else "DEACTIVATE_CASHIER",
                    details = "${if (cashier.active) "Activated" else "Deactivated"} cashier ${cashier.name}"
                )
            )
            onComplete?.invoke()
        }
    }

    /**
     * Delete cashier from centralized roster.
     */
    fun deleteCashier(cashier: Cashier, onComplete: (() -> Unit)? = null) {
        scope.launch {
            reportDao.deleteCashier(cashier)
            reportDao.insertAuditLog(
                AuditLog(
                    username = _activeCashier.value,
                    action = "DELETE_CASHIER",
                    details = "Removed cashier ${cashier.name}"
                )
            )
            // If active cashier was deleted, fall back to Lojia Manager
            if (_activeCashier.value == cashier.name) {
                setActiveCashier("Lojia Manager")
            }
            onComplete?.invoke()
        }
    }

    /**
     * Force synchronization across DB, Shared Preferences, and in-memory StateFlows.
     */
    fun forceSync(onResult: ((Boolean, String) -> Unit)? = null) {
        scope.launch {
            try {
                verifyAndSeedDefaultCashiers()
                syncStoredSettingsFromDb()
                val list = reportDao.getAllCashiers().first()
                val currentLang = _currentLanguage.value
                val message = "Successfully synchronized ${list.size} cashiers and ${currentLang.displayName} language."
                _syncState.value = ConfigurationSyncState(
                    isSynchronized = true,
                    lastSyncTime = System.currentTimeMillis(),
                    totalCashiers = list.size,
                    activeLanguage = currentLang,
                    activeCashier = _activeCashier.value,
                    syncStatusMessage = message
                )
                onResult?.invoke(true, message)
            } catch (e: Exception) {
                val errMsg = "Sync error: ${e.localizedMessage}"
                _syncState.value = _syncState.value.copy(
                    isSynchronized = false,
                    syncStatusMessage = errMsg
                )
                onResult?.invoke(false, errMsg)
            }
        }
    }

    /**
     * Export all configuration (Language, Module, Cashiers, Receipt info) as a synchronized JSON bundle.
     */
    suspend fun exportConfigurationJson(): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("timestamp", System.currentTimeMillis())
        root.put("language", _currentLanguage.value.code)
        root.put("activeCashier", _activeCashier.value)
        root.put("activeModule", _currentModule.value.name)

        val cashierArr = JSONArray()
        val allCashiers = reportDao.getAllCashiers().first()
        allCashiers.forEach { c ->
            val obj = JSONObject()
            obj.put("name", c.name)
            obj.put("pin", c.pin)
            obj.put("role", c.role)
            cashierArr.put(obj)
        }
        root.put("cashiers", cashierArr)

        return root.toString(2)
    }

    /**
     * Import configuration bundle to sync settings.
     */
    suspend fun importConfigurationJson(jsonStr: String): Boolean {
        return try {
            val root = JSONObject(jsonStr)
            if (root.has("language")) {
                val langCode = root.getString("language")
                val lang = AppLanguage.fromCode(langCode)
                setLanguage(lang)
            }
            if (root.has("activeCashier")) {
                setActiveCashier(root.getString("activeCashier"))
            }
            if (root.has("cashiers")) {
                val arr = root.getJSONArray("cashiers")
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val name = obj.getString("name")
                    val pin = if (obj.has("pin")) obj.getString("pin") else "1111"
                    val role = if (obj.has("role")) obj.getString("role") else "CASHIER"
                    addCashier(name, pin, role)
                }
            }
            forceSync()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import configuration", e)
            false
        }
    }

    companion object {
        private const val TAG = "ConfigSyncManager"
        private const val PREFS_NAME = "lojia_app_prefs"
        const val KEY_SELECTED_LANGUAGE = "selected_language"
        const val KEY_SELECTED_COUNTRY = "selected_country"
        const val KEY_ACTIVE_CASHIER = "active_cashier_name"
        const val KEY_ACTIVE_MODULE = "active_module"

        @Volatile
        private var INSTANCE: ConfigurationSyncManager? = null

        fun getInstance(context: Context): ConfigurationSyncManager {
            return INSTANCE ?: synchronized(this) {
                val instance = ConfigurationSyncManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
