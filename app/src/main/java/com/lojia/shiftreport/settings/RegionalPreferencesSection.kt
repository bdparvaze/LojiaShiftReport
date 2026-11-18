package com.lojia.shiftreport.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojia.shiftreport.R
import com.lojia.shiftreport.auth.AdminAuthDialog
import com.lojia.shiftreport.data.AppCountry
import com.lojia.shiftreport.data.AppLanguage
import com.lojia.shiftreport.data.BusinessProfile
import com.lojia.shiftreport.report.ReportViewModel
import com.lojia.shiftreport.ui.common.LojiaDimens
import com.lojia.shiftreport.ui.theme.*
import com.lojia.shiftreport.util.AppConstants
import com.lojia.shiftreport.util.MoneyFormat
import java.util.Currency
import java.util.Locale

/**
 * Converts a 2-letter Country ISO code (e.g., "BD", "SA", "US", "AE") into its corresponding Flag Emoji.
 */
fun countryCodeToFlagEmoji(countryCode: String): String {
    if (countryCode.isBlank()) return "🌐"
    val code = countryCode.trim().uppercase()
    if (code == "EU") return "🇪🇺"
    if (code.length != 2) return "🌐"
    val firstChar = code[0]
    val secondChar = code[1]
    if (firstChar !in 'A'..'Z' || secondChar !in 'A'..'Z') return "🌐"
    val firstLetter = Character.codePointAt(code, 0) - 0x41 + 0x1F1E6
    val secondLetter = Character.codePointAt(code, 1) - 0x41 + 0x1F1E6
    return String(Character.toChars(firstLetter)) + String(Character.toChars(secondLetter))
}

/**
 * Resolves the actual Currency Symbol (e.g. ﷼, ৳, $, €, £, ₹) based on a currency code.
 */
fun getCurrencySymbol(currencyCode: String): String {
    if (currencyCode.isBlank()) return AppConstants.DEFAULT_CURRENCY_SYMBOL
    val upper = currencyCode.trim().uppercase()
    return when (upper) {
        "SAR", "QAR", "OMR" -> "﷼"
        "BDT" -> "৳"
        "USD", "CAD", "AUD", "SGD" -> "$"
        "EUR" -> "€"
        "GBP" -> "£"
        "INR" -> "₹"
        "PKR" -> "Rs"
        "AED" -> "د.إ"
        "KWD" -> "د.ك"
        "BHD" -> "د.ب"
        "MYR" -> "RM"
        "TRY" -> "₺"
        "EGP" -> "E£"
        "JPY", "CNY" -> "¥"
        "IDR" -> "Rp"
        else -> try {
            java.util.Currency.getInstance(upper).symbol
        } catch (e: Exception) {
            upper
        }
    }
}

private data class IsoCurrencyInfo(
    val code: String,
    val symbol: String,
    val fullName: String,
    val digits: Int
)

private val COMMON_CURRENCY_ORDER = listOf(
    "USD", "SAR", "BDT", "AED", "QAR", "KWD", "BHD", "OMR",
    "EUR", "GBP", "INR", "PKR", "EGP", "MYR", "SGD", "CAD",
    "AUD", "TRY", "JPY", "CNY", "IDR"
)

private val COMMON_CURRENCY_NAMES = mapOf(
    "BDT" to "Bangladeshi Taka",
    "SAR" to "Saudi Riyal",
    "AED" to "UAE Dirham",
    "QAR" to "Qatari Riyal",
    "KWD" to "Kuwaiti Dinar",
    "OMR" to "Omani Rial",
    "BHD" to "Bahraini Dinar",
    "USD" to "US Dollar",
    "GBP" to "British Pound",
    "EUR" to "Euro",
    "INR" to "Indian Rupee",
    "PKR" to "Pakistani Rupee",
    "MYR" to "Malaysian Ringgit",
    "SGD" to "Singapore Dollar",
    "CAD" to "Canadian Dollar",
    "AUD" to "Australian Dollar",
    "TRY" to "Turkish Lira",
    "EGP" to "Egyptian Pound",
    "JPY" to "Japanese Yen",
    "CNY" to "Chinese Yuan",
    "IDR" to "Indonesian Rupiah"
)

private fun resolveIsoCurrencyInfo(code: String?, locale: Locale = Locale.getDefault()): IsoCurrencyInfo {
    val upper = code?.trim()?.uppercase(Locale.US).orEmpty().ifEmpty { MoneyFormat.DEFAULT_CURRENCY_CODE }
    val digits = MoneyFormat.minorUnitDigits(upper)
    val symbol = getCurrencySymbol(upper)
    val displayName = COMMON_CURRENCY_NAMES[upper] ?: try {
        Currency.getInstance(upper).getDisplayName(locale)
    } catch (_: Exception) {
        upper
    }
    return IsoCurrencyInfo(
        code = upper,
        symbol = symbol,
        fullName = displayName,
        digits = digits
    )
}

private fun allIso4217Currencies(locale: Locale = Locale.getDefault()): List<IsoCurrencyInfo> {
    val nonCirculating = setOf("XXX", "XTS", "XBA", "XBB", "XBC", "XBD", "XAU", "XAG", "XPD", "XPT", "XSU", "XUA")
    val availableCodes = Currency.getAvailableCurrencies()
        .asSequence()
        .map { it.currencyCode.uppercase(Locale.US) }
        .filter { it.length == 3 && it.all { ch -> ch in 'A'..'Z' } && it !in nonCirculating }
        .toMutableSet()
    availableCodes.addAll(COMMON_CURRENCY_ORDER)
    val priorityIndex = COMMON_CURRENCY_ORDER.withIndex().associate { (i, c) -> c to i }
    return availableCodes
        .map { resolveIsoCurrencyInfo(it, locale) }
        .sortedWith(compareBy<IsoCurrencyInfo> { priorityIndex[it.code] ?: Int.MAX_VALUE }.thenBy { it.code })
}

private data class LanguageDisplayInfo(
    val flag: String,
    val nativeLabel: String,
    val englishLabel: String
)

private fun getLanguageDisplayInfo(lang: AppLanguage): LanguageDisplayInfo {
    return when (lang) {
        AppLanguage.ENGLISH -> LanguageDisplayInfo(
            flag = "🇺🇸",
            nativeLabel = "English",
            englishLabel = "English"
        )
        AppLanguage.BENGALI -> LanguageDisplayInfo(
            flag = "🇧🇩",
            nativeLabel = "বাংলা",
            englishLabel = "Bengali"
        )
        AppLanguage.ARABIC -> LanguageDisplayInfo(
            flag = "🇸🇦",
            nativeLabel = "العربية",
            englishLabel = "Arabic"
        )
    }
}

private enum class RegionalSheetType {
    COUNTRY,
    LANGUAGE,
    CURRENCY
}

/**
 * Clean 3-row Regional Preferences summary card for the main Settings overview.
 * Displays Country, Language, and Currency as 3 separate rows.
 */
@Composable
fun RegionalPreferencesCard(
    currentCountry: AppCountry,
    currentLanguage: AppLanguage,
    businessProfile: BusinessProfile?,
    onOpenRegionalMenu: (initialOption: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeCurrencyCode = businessProfile?.currency?.ifBlank { currentCountry.currencyCode } ?: currentCountry.currencyCode
    val activeCurrency = resolveIsoCurrencyInfo(activeCurrencyCode)
    val countryFlag = countryCodeToFlagEmoji(currentCountry.code)
    val langInfo = getLanguageDisplayInfo(currentLanguage)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, OutlineVariantLight),
        modifier = modifier
            .fillMaxWidth()
            .testTag("regional_preferences_card")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Row 1: Country
            SimpleRegionalSelectorRow(
                icon = Icons.Outlined.Public,
                label = "Country",
                valueText = "$countryFlag  ${stringResource(currentCountry.nameRes)}",
                onClick = { onOpenRegionalMenu(1) },
                testTag = "regional_row_country_currency"
            )

            HorizontalDivider(color = OutlineVariantLight, thickness = 1.dp)

            // Row 2: Language
            SimpleRegionalSelectorRow(
                icon = Icons.Outlined.Translate,
                label = "Language",
                valueText = "${langInfo.flag}  ${langInfo.nativeLabel}",
                onClick = { onOpenRegionalMenu(0) },
                testTag = "regional_row_language"
            )

            HorizontalDivider(color = OutlineVariantLight, thickness = 1.dp)

            // Row 3: Currency
            SimpleRegionalSelectorRow(
                icon = Icons.Outlined.Payments,
                label = "Currency",
                valueText = "${activeCurrency.code} (${activeCurrency.symbol})",
                onClick = { onOpenRegionalMenu(2) },
                testTag = "regional_row_currency"
            )
        }
    }
}

/**
 * Maintained for compatibility with any external callers.
 */
@Composable
fun RegionalPreferenceItemRow(
    icon: ImageVector? = null,
    emojiText: String? = null,
    iconBgColor: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    badgeText: String,
    badgeBgColor: Color,
    badgeTextColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    SimpleRegionalSelectorRow(
        icon = icon ?: Icons.Outlined.Public,
        label = title,
        valueText = subtitle,
        onClick = onClick,
        modifier = modifier,
        testTag = testTag
    )
}

/**
 * Clean, simple, professional Localization screen with THREE plain rows stacked vertically:
 * - Row 1: Country selector (shows current country + flag)
 * - Row 2: Language selector (shows current language)
 * - Row 3: Currency selector (shows current currency)
 *
 * Each row opens a ModalBottomSheet when tapped.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegionalPreferencesDetailView(
    reportViewModel: ReportViewModel,
    initialTab: Int = -1,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentLanguage by reportViewModel.currentLanguage.collectAsState()
    val currentCountry by reportViewModel.currentCountry.collectAsState()
    val currentCurrency by reportViewModel.currentCurrency.collectAsState()
    val isCurrencyLocked by reportViewModel.isCurrencyLocked.collectAsState()

    val activeCurrencyCode = currentCurrency.ifBlank { currentCountry.currencyCode }
    val activeCurrency = resolveIsoCurrencyInfo(activeCurrencyCode)
    val countryFlag = countryCodeToFlagEmoji(currentCountry.code)
    val langInfo = getLanguageDisplayInfo(currentLanguage)
    val allCurrencies = remember(currentLanguage) { allIso4217Currencies() }

    var activeSheet by remember { mutableStateOf<RegionalSheetType?>(null) }
    var pendingAuthSubtitle by remember { mutableStateOf<String?>(null) }
    var pendingAuthAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    LaunchedEffect(isCurrencyLocked) {
        if (isCurrencyLocked && (activeSheet == RegionalSheetType.COUNTRY || activeSheet == RegionalSheetType.CURRENCY)) {
            activeSheet = null
        }
    }

    BackHandler(enabled = activeSheet != null) {
        activeSheet = null
    }

    if (pendingAuthAction != null) {
        AdminAuthDialog(
            title = stringResource(R.string.admin_authorization_required),
            subtitle = pendingAuthSubtitle ?: stringResource(R.string.admin_reauth_default_title),
            onDismissRequest = {
                pendingAuthSubtitle = null
                pendingAuthAction = null
            },
            onAuthSuccess = {
                val action = pendingAuthAction
                pendingAuthSubtitle = null
                pendingAuthAction = null
                action?.invoke()
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(BackgroundLight)
            .testTag("regional_preferences_detail_view"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(1.dp, OutlineVariantLight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Row 1: Country selector (disabled when financial data exists)
                    SimpleRegionalSelectorRow(
                        icon = Icons.Outlined.Public,
                        label = "Country",
                        valueText = "$countryFlag  ${stringResource(currentCountry.nameRes)}",
                        enabled = !isCurrencyLocked,
                        onClick = { if (!isCurrencyLocked) activeSheet = RegionalSheetType.COUNTRY },
                        testTag = "row_select_country"
                    )

                    HorizontalDivider(color = OutlineVariantLight, thickness = 1.dp)

                    // Row 2: Language selector (always unlocked)
                    SimpleRegionalSelectorRow(
                        icon = Icons.Outlined.Translate,
                        label = "Language",
                        valueText = "${langInfo.flag}  ${langInfo.nativeLabel}",
                        enabled = true,
                        onClick = { activeSheet = RegionalSheetType.LANGUAGE },
                        testTag = "row_select_language"
                    )

                    HorizontalDivider(color = OutlineVariantLight, thickness = 1.dp)

                    // Row 3: Currency selector (disabled when financial data exists)
                    SimpleRegionalSelectorRow(
                        icon = Icons.Outlined.Payments,
                        label = "Currency",
                        valueText = "${activeCurrency.symbol}  ${activeCurrency.code} — ${activeCurrency.fullName}",
                        enabled = !isCurrencyLocked,
                        onClick = { if (!isCurrencyLocked) activeSheet = RegionalSheetType.CURRENCY },
                        testTag = "row_select_currency"
                    )
                }
            }

            if (isCurrencyLocked) {
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = WarningContainer),
                    border = BorderStroke(1.dp, WarningOrange.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("currency_locked_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = WarningOrange,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(R.string.currency_locked_message),
                            fontSize = 12.5.sp,
                            lineHeight = 17.sp,
                            color = OnSurfaceLight
                        )
                    }
                }
            }
        }
    }

    // =========================================================================
    // BOTTOM SHEET 1: COUNTRY SELECTOR
    // =========================================================================
    if (activeSheet == RegionalSheetType.COUNTRY) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var searchQuery by remember { mutableStateOf("") }
        val filteredCountries = remember(searchQuery, currentLanguage) {
            AppCountry.entries.filter { country ->
                if (searchQuery.isBlank()) {
                    true
                } else {
                    val localizedName = context.getString(country.nameRes)
                    localizedName.contains(searchQuery, ignoreCase = true) ||
                        country.displayName.contains(searchQuery, ignoreCase = true) ||
                        country.code.contains(searchQuery, ignoreCase = true)
                }
            }
        }

        ModalBottomSheet(
            onDismissRequest = { activeSheet = null },
            sheetState = sheetState,
            containerColor = SurfaceLight,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp)
            ) {
                // Sheet Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Select Country",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceLight
                    )
                    IconButton(
                        onClick = { activeSheet = null },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.cd_close),
                            tint = OnSurfaceVariantLight,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Search Box
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search country...",
                            fontSize = 14.sp,
                            color = TextHintColor
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = OnSurfaceVariantLight,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.cd_clear),
                                    tint = OnSurfaceVariantLight,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = OnSurfaceLight,
                        unfocusedTextColor = OnSurfaceLight,
                        focusedContainerColor = BackgroundLight,
                        unfocusedContainerColor = BackgroundLight,
                        focusedBorderColor = PrimaryIndigoLight,
                        unfocusedBorderColor = OutlineVariantLight,
                        cursorColor = PrimaryIndigoLight
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("input_search_country_currency")
                )

                HorizontalDivider(color = OutlineVariantLight, thickness = 1.dp)

                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(filteredCountries, key = { it.code }) { country ->
                        val isSelected = country == currentCountry
                        val flag = countryCodeToFlagEmoji(country.code)
                        val countryName = stringResource(country.nameRes)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    pendingAuthSubtitle = countryName
                                    pendingAuthAction = {
                                        // Select country (also synchronizes its matching currency)
                                        reportViewModel.setCountry(country)
                                        activeSheet = null
                                    }
                                }
                                .background(if (isSelected) PrimaryContainerLight else SurfaceLight)
                                .padding(horizontal = 12.dp, vertical = 14.dp)
                                .testTag("option_country_${country.code}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = flag,
                                    fontSize = 22.sp,
                                    color = OnSurfaceLight
                                )
                                Text(
                                    text = countryName,
                                    fontSize = 15.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) PrimaryIndigoLight else OnSurfaceLight,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = stringResource(R.string.cd_selected),
                                    tint = PrimaryIndigoLight,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        HorizontalDivider(color = OutlineVariantLight, thickness = 0.5.dp)
                    }
                }
            }
        }
    }

    // =========================================================================
    // BOTTOM SHEET 2: LANGUAGE SELECTOR
    // =========================================================================
    if (activeSheet == RegionalSheetType.LANGUAGE) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { activeSheet = null },
            sheetState = sheetState,
            containerColor = SurfaceLight,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp)
            ) {
                // Sheet Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Select Language",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceLight
                    )
                    IconButton(
                        onClick = { activeSheet = null },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.cd_close),
                            tint = OnSurfaceVariantLight,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                HorizontalDivider(color = OutlineVariantLight, thickness = 1.dp)

                AppLanguage.entries.forEach { lang ->
                    val isSelected = lang == currentLanguage
                    val info = getLanguageDisplayInfo(lang)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                reportViewModel.setLanguage(lang)
                                activeSheet = null
                            }
                            .background(if (isSelected) PrimaryContainerLight else SurfaceLight)
                            .padding(horizontal = 12.dp, vertical = 14.dp)
                            .testTag("option_language_${lang.code}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = info.flag,
                                fontSize = 22.sp,
                                color = OnSurfaceLight
                            )
                            Column {
                                Text(
                                    text = info.nativeLabel,
                                    fontSize = 15.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                    color = if (isSelected) PrimaryIndigoLight else OnSurfaceLight
                                )
                                if (info.nativeLabel != info.englishLabel) {
                                    Text(
                                        text = info.englishLabel,
                                        fontSize = 12.sp,
                                        color = OnSurfaceVariantLight
                                    )
                                }
                            }
                        }

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = stringResource(R.string.cd_selected),
                                tint = PrimaryIndigoLight,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    HorizontalDivider(color = OutlineVariantLight, thickness = 0.5.dp)
                }
            }
        }
    }

    // =========================================================================
    // BOTTOM SHEET 3: CURRENCY SELECTOR
    // =========================================================================
    if (activeSheet == RegionalSheetType.CURRENCY && !isCurrencyLocked) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var searchQuery by remember { mutableStateOf("") }
        val filteredCurrencies = remember(searchQuery, allCurrencies) {
            allCurrencies.filter { currency ->
                if (searchQuery.isBlank()) {
                    true
                } else {
                    currency.code.contains(searchQuery, ignoreCase = true) ||
                        currency.fullName.contains(searchQuery, ignoreCase = true) ||
                        currency.symbol.contains(searchQuery, ignoreCase = true)
                }
            }
        }

        ModalBottomSheet(
            onDismissRequest = { activeSheet = null },
            sheetState = sheetState,
            containerColor = SurfaceLight,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp)
            ) {
                // Sheet Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Select Currency",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceLight
                    )
                    IconButton(
                        onClick = { activeSheet = null },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.cd_close),
                            tint = OnSurfaceVariantLight,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Search Box
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search currency...",
                            fontSize = 14.sp,
                            color = TextHintColor
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = OnSurfaceVariantLight,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.cd_clear),
                                    tint = OnSurfaceVariantLight,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = OnSurfaceLight,
                        unfocusedTextColor = OnSurfaceLight,
                        focusedContainerColor = BackgroundLight,
                        unfocusedContainerColor = BackgroundLight,
                        focusedBorderColor = PrimaryIndigoLight,
                        unfocusedBorderColor = OutlineVariantLight,
                        cursorColor = PrimaryIndigoLight
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("input_search_currency")
                )

                HorizontalDivider(color = OutlineVariantLight, thickness = 1.dp)

                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(filteredCurrencies, key = { it.code }) { currency ->
                        val isSelected = currency.code.equals(activeCurrency.code, ignoreCase = true)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    pendingAuthSubtitle = currency.code
                                    pendingAuthAction = {
                                        reportViewModel.setCurrency(currency.code)
                                        activeSheet = null
                                    }
                                }
                                .background(if (isSelected) PrimaryContainerLight else SurfaceLight)
                                .padding(horizontal = 12.dp, vertical = 12.dp)
                                .testTag("option_currency_${currency.code}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) SurfaceLight else SurfaceVariantLight)
                                        .border(
                                            1.dp,
                                            if (isSelected) PrimaryIndigoLight else OutlineVariantLight,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = currency.symbol,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) PrimaryIndigoLight else OnSurfaceLight
                                    )
                                }

                                Column {
                                    Text(
                                        text = currency.code,
                                        fontSize = 15.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                        color = if (isSelected) PrimaryIndigoLight else OnSurfaceLight
                                    )
                                    Text(
                                        text = currency.fullName,
                                        fontSize = 12.sp,
                                        color = OnSurfaceVariantLight,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = stringResource(R.string.cd_selected),
                                    tint = PrimaryIndigoLight,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        HorizontalDivider(color = OutlineVariantLight, thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

/**
 * Single plain row used in the 3-row Localization screen.
 */
@Composable
private fun SimpleRegionalSelectorRow(
    icon: ImageVector,
    label: String,
    valueText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    testTag: String = ""
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 60.dp)
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (enabled) OnSurfaceVariantLight else TextDisabledColor,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = if (enabled) OnSurfaceLight else TextDisabledColor
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = valueText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (enabled) OnSurfaceVariantLight else TextDisabledColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(
                imageVector = if (enabled) Icons.Outlined.ChevronRight else Icons.Outlined.Lock,
                contentDescription = null,
                tint = TextHintColor,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
