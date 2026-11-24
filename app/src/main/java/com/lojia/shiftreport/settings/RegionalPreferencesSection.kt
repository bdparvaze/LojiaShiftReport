package com.lojia.shiftreport.settings

import androidx.activity.compose.BackHandler
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
import com.lojia.shiftreport.ui.common.LojiaTextField
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
fun getCurrencySymbol(currencyCode: String, locale: Locale = Locale.getDefault()): String {
    if (currencyCode.isBlank()) return AppConstants.DEFAULT_CURRENCY_SYMBOL
    val upper = currencyCode.trim().uppercase(Locale.ROOT)
    return try {
        Currency.getInstance(upper).getSymbol(locale).ifBlank { upper }
    } catch (_: Exception) {
        upper
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

private fun resolveIsoCurrencyInfo(code: String?, locale: Locale = Locale.getDefault()): IsoCurrencyInfo {
    val upper = code?.trim()?.uppercase(Locale.ROOT).orEmpty().ifEmpty { MoneyFormat.DEFAULT_CURRENCY_CODE }
    val digits = MoneyFormat.minorUnitDigits(upper)
    val symbol = getCurrencySymbol(upper, locale)
    val displayName = try {
        Currency.getInstance(upper).getDisplayName(locale).ifBlank { upper }
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
        .map { it.currencyCode.uppercase(Locale.ROOT) }
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
    @androidx.annotation.StringRes val nativeLabelRes: Int
)

private fun getLanguageDisplayInfo(lang: AppLanguage): LanguageDisplayInfo {
    return when (lang) {
        AppLanguage.ENGLISH -> LanguageDisplayInfo(
            flag = "🇺🇸",
            nativeLabelRes = R.string.lang_native_en
        )
        AppLanguage.BENGALI -> LanguageDisplayInfo(
            flag = "🇧🇩",
            nativeLabelRes = R.string.lang_native_bn
        )
        AppLanguage.ARABIC -> LanguageDisplayInfo(
            flag = "🇸🇦",
            nativeLabelRes = R.string.lang_native_ar
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
    val appLocale = remember(currentLanguage) { Locale.forLanguageTag(currentLanguage.code) }
    val activeCurrencyCode = businessProfile?.currency?.ifBlank { currentCountry.currencyCode } ?: currentCountry.currencyCode
    val activeCurrency = resolveIsoCurrencyInfo(activeCurrencyCode, appLocale)
    val countryFlag = countryCodeToFlagEmoji(currentCountry.code)
    val langInfo = getLanguageDisplayInfo(currentLanguage)

    val depthStyle = LocalDepthStyle.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .depthCard(cornerRadius = Dimens.CardCornerRadius)
            .testTag("regional_preferences_card")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Row 1: Country
            SimpleRegionalSelectorRow(
                icon = Icons.Outlined.Public,
                label = stringResource(R.string.regional_country_label),
                valueText = "$countryFlag  ${stringResource(currentCountry.nameRes)}",
                tileColors = listOf(PureWhite, BadgeGreenBg),
                iconTint = BadgeGreenText,
                onClick = { onOpenRegionalMenu(1) },
                testTag = "regional_row_country_currency"
            )

            HorizontalDivider(
                color = depthStyle.listDividerColor,
                thickness = Dimens.RowDividerWidth,
                modifier = Modifier.padding(horizontal = Dimens.ReportCardInnerPadding)
            )

            // Row 2: Language
            SimpleRegionalSelectorRow(
                icon = Icons.Outlined.Translate,
                label = stringResource(R.string.nav_language),
                valueText = "${langInfo.flag}  ${stringResource(langInfo.nativeLabelRes)}",
                tileColors = listOf(PureWhite, BadgeBlueBg),
                iconTint = PrimaryBlueLight,
                onClick = { onOpenRegionalMenu(0) },
                testTag = "regional_row_language"
            )

            HorizontalDivider(
                color = depthStyle.listDividerColor,
                thickness = Dimens.RowDividerWidth,
                modifier = Modifier.padding(horizontal = Dimens.ReportCardInnerPadding)
            )

            // Row 3: Currency
            SimpleRegionalSelectorRow(
                icon = Icons.Outlined.Payments,
                label = stringResource(R.string.regional_currency_label),
                valueText = "${activeCurrency.code} (${activeCurrency.symbol})",
                tileColors = listOf(PureWhite, BadgeAmberBg),
                iconTint = BadgeAmberAccent,
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

    val appLocale = remember(currentLanguage) { Locale.forLanguageTag(currentLanguage.code) }
    val activeCurrencyCode = currentCurrency.ifBlank { currentCountry.currencyCode }
    val activeCurrency = resolveIsoCurrencyInfo(activeCurrencyCode, appLocale)
    val countryFlag = countryCodeToFlagEmoji(currentCountry.code)
    val langInfo = getLanguageDisplayInfo(currentLanguage)
    val allCurrencies = remember(appLocale) { allIso4217Currencies(appLocale) }

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

    val depthStyle = LocalDepthStyle.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(depthStyle.reportPageBackground)
            .testTag("regional_preferences_detail_view"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .padding(horizontal = Dimens.SpacingLg, vertical = Dimens.SpacingLg),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMd)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .depthCard(cornerRadius = Dimens.CardCornerRadius)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Row 1: Country selector (disabled when financial data exists)
                    SimpleRegionalSelectorRow(
                        icon = Icons.Outlined.Public,
                        label = stringResource(R.string.regional_country_label),
                        valueText = "$countryFlag  ${stringResource(currentCountry.nameRes)}",
                        tileColors = listOf(PureWhite, BadgeGreenBg),
                        iconTint = BadgeGreenText,
                        enabled = !isCurrencyLocked,
                        onClick = { if (!isCurrencyLocked) activeSheet = RegionalSheetType.COUNTRY },
                        testTag = "row_select_country"
                    )

                    HorizontalDivider(
                        color = depthStyle.listDividerColor,
                        thickness = Dimens.RowDividerWidth,
                        modifier = Modifier.padding(horizontal = Dimens.ReportCardInnerPadding)
                    )

                    // Row 2: Language selector (always unlocked)
                    SimpleRegionalSelectorRow(
                        icon = Icons.Outlined.Translate,
                        label = stringResource(R.string.nav_language),
                        valueText = "${langInfo.flag}  ${stringResource(langInfo.nativeLabelRes)}",
                        tileColors = listOf(PureWhite, BadgeBlueBg),
                        iconTint = PrimaryBlueLight,
                        enabled = true,
                        onClick = { activeSheet = RegionalSheetType.LANGUAGE },
                        testTag = "row_select_language"
                    )

                    HorizontalDivider(
                        color = depthStyle.listDividerColor,
                        thickness = Dimens.RowDividerWidth,
                        modifier = Modifier.padding(horizontal = Dimens.ReportCardInnerPadding)
                    )

                    // Row 3: Currency selector (disabled when financial data exists)
                    SimpleRegionalSelectorRow(
                        icon = Icons.Outlined.Payments,
                        label = stringResource(R.string.regional_currency_label),
                        valueText = "${activeCurrency.symbol}  ${activeCurrency.code} — ${activeCurrency.fullName}",
                        tileColors = listOf(PureWhite, BadgeAmberBg),
                        iconTint = BadgeAmberAccent,
                        enabled = !isCurrencyLocked,
                        onClick = { if (!isCurrencyLocked) activeSheet = RegionalSheetType.CURRENCY },
                        testTag = "row_select_currency"
                    )
                }
            }

            if (isCurrencyLocked) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .depthSemanticCard(
                            backgroundColor = WarningContainer,
                            borderColor = SectionAmberBorder,
                            cornerRadius = Dimens.RadiusReportInput
                        )
                        .testTag("currency_locked_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(
                            horizontal = Dimens.InsetFieldPaddingHorizontal,
                            vertical = Dimens.ListRowPaddingVertical
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.ReportGridGap)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = WarningText,
                            modifier = Modifier.size(Dimens.IconSm)
                        )
                        Text(
                            text = stringResource(R.string.currency_locked_message),
                            fontSize = DepthTypeTokens.ListRowSubtitleSize,
                            color = WarningText
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
            shape = RoundedCornerShape(
                topStart = Dimens.BottomSheetCornerRadius,
                topEnd = Dimens.BottomSheetCornerRadius
            )
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
                        text = stringResource(R.string.regional_select_country),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceLight
                    )
                    IconButton(
                        onClick = { activeSheet = null },
                        modifier = Modifier.minimumInteractiveComponentSize()
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
                LojiaTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.search_country_hint),
                            fontSize = DepthTypeTokens.ListRowTitleSize,
                            color = depthStyle.reportFieldPlaceholderColor
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = OnSurfaceVariantLight,
                            modifier = Modifier.size(Dimens.IconField)
                        )
                    },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.cd_clear),
                                    tint = OnSurfaceVariantLight,
                                    modifier = Modifier.size(Dimens.IconSm)
                                )
                            }
                        }
                    } else null,
                    shape = RoundedCornerShape(Dimens.RadiusReportInput),
                    minHeight = Dimens.TextFieldHeightReport,
                    borderColorOverride = depthStyle.reportFieldBorderColor,
                    topShadowAlphaOverride = depthStyle.reportFieldInsetAlpha,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = Dimens.SpacingMd)
                        .testTag("input_search_country_currency")
                )

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredCountries, key = { it.code }) { country ->
                        val isSelected = country == currentCountry
                        val flag = countryCodeToFlagEmoji(country.code)
                        val countryName = stringResource(country.nameRes)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = Dimens.MinTouchTarget)
                                .depthCard(
                                    accent = isSelected,
                                    shape = RoundedCornerShape(Dimens.RadiusMd)
                                )
                                .clickable {
                                    pendingAuthSubtitle = countryName
                                    pendingAuthAction = {
                                        // Select country (also synchronizes its matching currency)
                                        reportViewModel.setCountry(country)
                                        activeSheet = null
                                    }
                                }
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
            shape = RoundedCornerShape(
                topStart = Dimens.BottomSheetCornerRadius,
                topEnd = Dimens.BottomSheetCornerRadius
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Sheet Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.regional_select_language),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceLight
                    )
                    IconButton(
                        onClick = { activeSheet = null },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.cd_close),
                            tint = OnSurfaceVariantLight,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                AppLanguage.entries.forEach { lang ->
                    val isSelected = lang == currentLanguage
                    val info = getLanguageDisplayInfo(lang)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = Dimens.MinTouchTarget)
                            .depthCard(
                                accent = isSelected,
                                shape = RoundedCornerShape(Dimens.RadiusMd)
                            )
                            .clickable {
                                reportViewModel.setLanguage(lang)
                                activeSheet = null
                            }
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
                            Text(
                                text = stringResource(info.nativeLabelRes),
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (isSelected) PrimaryIndigoLight else OnSurfaceLight
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
            shape = RoundedCornerShape(
                topStart = Dimens.BottomSheetCornerRadius,
                topEnd = Dimens.BottomSheetCornerRadius
            )
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
                        text = stringResource(R.string.regional_select_currency),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceLight
                    )
                    IconButton(
                        onClick = { activeSheet = null },
                        modifier = Modifier.minimumInteractiveComponentSize()
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
                LojiaTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.regional_search_currency_hint),
                            fontSize = DepthTypeTokens.ListRowTitleSize,
                            color = depthStyle.reportFieldPlaceholderColor
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = OnSurfaceVariantLight,
                            modifier = Modifier.size(Dimens.IconField)
                        )
                    },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.cd_clear),
                                    tint = OnSurfaceVariantLight,
                                    modifier = Modifier.size(Dimens.IconSm)
                                )
                            }
                        }
                    } else null,
                    shape = RoundedCornerShape(Dimens.RadiusReportInput),
                    minHeight = Dimens.TextFieldHeightReport,
                    borderColorOverride = depthStyle.reportFieldBorderColor,
                    topShadowAlphaOverride = depthStyle.reportFieldInsetAlpha,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = Dimens.SpacingMd)
                        .testTag("input_search_currency")
                )

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredCurrencies, key = { it.code }) { currency ->
                        val isSelected = currency.code.equals(activeCurrency.code, ignoreCase = true)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = Dimens.MinTouchTarget)
                                .depthCard(
                                    accent = isSelected,
                                    shape = RoundedCornerShape(Dimens.RadiusMd)
                                )
                                .clickable {
                                    pendingAuthSubtitle = currency.code
                                    pendingAuthAction = {
                                        reportViewModel.setCurrency(currency.code)
                                        activeSheet = null
                                    }
                                }
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
    tileColors: List<Color> = listOf(PureWhite, BadgeBlueBg),
    iconTint: Color = PrimaryBlueLight,
    enabled: Boolean = true,
    testTag: String = ""
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = Dimens.MinTouchTarget)
            .clickable(enabled = enabled) { onClick() }
            .padding(
                horizontal = Dimens.ReportCardInnerPadding,
                vertical = Dimens.ListRowPaddingVertical
            )
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.SettingsIconTitleGap),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(Dimens.SettingsIconTileSize)
                    .depthRaisedIconTile(
                        gradientColors = tileColors,
                        cornerRadius = Dimens.RadiusSettingsIconTile
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (enabled) iconTint else TextDisabledColor,
                    modifier = Modifier.size(Dimens.IconField)
                )
            }
            Text(
                text = label,
                fontSize = DepthTypeTokens.ListRowTitleSize,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) TextPrimaryReport else TextDisabledColor
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)
        ) {
            Text(
                text = valueText,
                fontSize = DepthTypeTokens.ListRowSubtitleSize,
                fontWeight = FontWeight.Medium,
                color = if (enabled) OnSurfaceVariantLight else TextDisabledColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(
                imageVector = if (enabled) Icons.Outlined.ChevronRight else Icons.Outlined.Lock,
                contentDescription = null,
                tint = OnSurfaceVariantLight,
                modifier = Modifier.size(Dimens.IconSm)
            )
        }
    }
}
