package com.lojia.shiftreport.settings

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
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
import com.lojia.shiftreport.data.AppCountry
import com.lojia.shiftreport.data.AppLanguage
import com.lojia.shiftreport.data.BusinessProfile
import com.lojia.shiftreport.report.ReportViewModel
import com.lojia.shiftreport.ui.common.LojiaDimens
import com.lojia.shiftreport.ui.common.LojiaOutlinedButton
import com.lojia.shiftreport.ui.common.LojiaPrimaryButton
import com.lojia.shiftreport.ui.common.LojiaSectionHeader
import com.lojia.shiftreport.ui.common.LojiaSettingsCard
import com.lojia.shiftreport.ui.theme.*

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
    if (currencyCode.isBlank()) return "$"
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

/**
 * Metadata helper for rich international language display.
 */
private data class LanguageInternationalMeta(
    val flag: String,
    val nativeScript: String,
    val internationalTitle: String,
    val regionSubtitle: String,
    val directionLabel: String,
    val isoBadge: String
)

private fun getLanguageMeta(lang: AppLanguage): LanguageInternationalMeta {
    return when (lang) {
        AppLanguage.ENGLISH -> LanguageInternationalMeta(
            flag = "🇺🇸",
            nativeScript = "English",
            internationalTitle = "English (International)",
            regionSubtitle = "United States & Global • Standard POS",
            directionLabel = "LTR",
            isoBadge = "EN-US"
        )
        AppLanguage.BENGALI -> LanguageInternationalMeta(
            flag = "🇧🇩",
            nativeScript = "বাংলা",
            internationalTitle = "বাংলা • Bengali",
            regionSubtitle = "Bangladesh & South Asia • বাংলা সংস্করণ",
            directionLabel = "LTR",
            isoBadge = "BN-BD"
        )
        AppLanguage.ARABIC -> LanguageInternationalMeta(
            flag = "🇸🇦",
            nativeScript = "العربية",
            internationalTitle = "العربية • Arabic",
            regionSubtitle = "Saudi Arabia & GCC • الشرق الأوسط",
            directionLabel = "RTL",
            isoBadge = "AR-SA"
        )
    }
}

/**
 * Groups countries into international regions for quick filtering.
 */
private enum class WorldRegionFilter(val label: String) {
    ALL("All Regions"),
    SOUTH_ASIA("South Asia"),
    MIDDLE_EAST("Middle East & GCC"),
    EUROPE_AMERICAS("Europe & Americas"),
    ASIA_PACIFIC("Asia Pacific")
}

private fun getCountryRegion(country: AppCountry): WorldRegionFilter {
    return when (country) {
        AppCountry.BANGLADESH, AppCountry.INDIA, AppCountry.PAKISTAN -> WorldRegionFilter.SOUTH_ASIA
        AppCountry.SAUDI_ARABIA, AppCountry.UNITED_ARAB_EMIRATES, AppCountry.QATAR,
        AppCountry.KUWAIT, AppCountry.OMAN, AppCountry.BAHRAIN,
        AppCountry.TURKEY, AppCountry.EGYPT -> WorldRegionFilter.MIDDLE_EAST
        AppCountry.UNITED_STATES, AppCountry.UNITED_KINGDOM,
        AppCountry.EUROPEAN_UNION, AppCountry.CANADA -> WorldRegionFilter.EUROPE_AMERICAS
        AppCountry.MALAYSIA, AppCountry.SINGAPORE, AppCountry.AUSTRALIA,
        AppCountry.JAPAN, AppCountry.CHINA, AppCountry.INDONESIA -> WorldRegionFilter.ASIA_PACIFIC
    }
}

/**
 * Modern "Language & Regional Preferences" card for the main settings overview.
 * Displays 2 primary options:
 * 1. Language Select
 * 2. Country & Currency
 */
@Composable
fun RegionalPreferencesCard(
    currentCountry: AppCountry,
    currentLanguage: AppLanguage,
    businessProfile: BusinessProfile?,
    onOpenRegionalMenu: (initialOption: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeCurrency = businessProfile?.currency?.ifBlank { currentCountry.currencyCode } ?: currentCountry.currencyCode
    val activeSymbol = getCurrencySymbol(activeCurrency)
    val countryFlag = countryCodeToFlagEmoji(currentCountry.code)
    val langMeta = getLanguageMeta(currentLanguage)

    Card(
        shape = RoundedCornerShape(LojiaDimens.CardRadius),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, OutlineLight),
        modifier = modifier
            .fillMaxWidth()
            .testTag("regional_preferences_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(LojiaDimens.CardPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Card Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(PrimaryContainerLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Language,
                        contentDescription = null,
                        tint = PrimaryIndigoLight,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.regional_interface_section),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceLight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "International language, store country & POS currency",
                        fontSize = 12.sp,
                        color = OnSurfaceVariantLight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            HorizontalDivider(color = OutlineVariantLight, thickness = 1.dp)

            // 1. Language Select Row
            RegionalPreferenceItemRow(
                emojiText = langMeta.flag,
                iconBgColor = PrimaryContainerLight,
                iconTint = PrimaryIndigoLight,
                title = "Language Select",
                subtitle = "${langMeta.internationalTitle} • ${langMeta.directionLabel}",
                badgeText = langMeta.isoBadge,
                badgeBgColor = PrimaryContainerLight,
                badgeTextColor = OnPrimaryContainerLight,
                onClick = { onOpenRegionalMenu(0) },
                testTag = "regional_row_language"
            )

            HorizontalDivider(color = OutlineVariantLight, thickness = 1.dp)

            // 2. Country & Currency Row
            RegionalPreferenceItemRow(
                emojiText = countryFlag,
                iconBgColor = SuccessContainer,
                iconTint = SuccessGreen,
                title = "Country & Currency",
                subtitle = "${stringResource(currentCountry.nameRes)} • $activeCurrency ($activeSymbol)",
                badgeText = "$activeSymbol $activeCurrency",
                badgeBgColor = SuccessContainer,
                badgeTextColor = SuccessGreen,
                onClick = { onOpenRegionalMenu(1) },
                testTag = "regional_row_country_currency"
            )
        }
    }
}

/**
 * Reusable single preference row for settings overview.
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
    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(LojiaDimens.CardRadius),
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 54.dp)
            .clip(RoundedCornerShape(LojiaDimens.CardRadius))
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 4.dp),
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
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(iconBgColor)
                        .border(1.dp, OutlineLight, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (emojiText != null) {
                        Text(text = emojiText, fontSize = 20.sp)
                    } else if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = OnSurfaceLight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 13.sp,
                        color = OnSurfaceVariantLight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeBgColor)
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = TextHintColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * International-Standard Language & Regional Preferences Screen.
 *
 * Features two sleek, inline-expandable cards:
 * 1. Language Select -> Clicking opens a downward inline accordion with rich international language options + Cancel/Save.
 * 2. Country & Currency -> Clicking opens a downward inline accordion with region filters, search, and international currency cards + Cancel/Save.
 */
@Composable
fun RegionalPreferencesDetailView(
    reportViewModel: ReportViewModel,
    initialTab: Int = -1,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentLanguage by reportViewModel.currentLanguage.collectAsState()
    val currentCountry by reportViewModel.currentCountry.collectAsState()
    val businessProfile by reportViewModel.businessProfile.collectAsState()
    val bProfile = businessProfile ?: BusinessProfile()

    // null = both collapsed (clean 2-option view), 0 = Language Select expanded, 1 = Country & Currency expanded
    var expandedOption by remember(initialTab) {
        mutableStateOf<Int?>(
            when (initialTab) {
                0 -> 0
                1, 2 -> 1
                else -> null
            }
        )
    }

    // Track initial values when opening an accordion so "Cancel" can revert cleanly
    var languageBeforeExpand by remember { mutableStateOf(currentLanguage) }
    var countryBeforeExpand by remember { mutableStateOf(currentCountry) }

    var countrySearchQuery by remember { mutableStateOf("") }
    var selectedRegionFilter by remember { mutableStateOf(WorldRegionFilter.ALL) }

    val activeCurrency = bProfile.currency.ifBlank { currentCountry.currencyCode }
    val activeSymbol = getCurrencySymbol(activeCurrency)
    val countryFlag = countryCodeToFlagEmoji(currentCountry.code)
    val activeLangMeta = getLanguageMeta(currentLanguage)

    BackHandler {
        if (expandedOption != null) {
            expandedOption = null
        } else {
            reportViewModel.selectReportSettingsMenu("root")
        }
    }

    val allCountries = remember { AppCountry.entries }
    val filteredCountries = remember(countrySearchQuery, selectedRegionFilter, currentLanguage) {
        allCountries.filter { country ->
            val matchesRegion = selectedRegionFilter == WorldRegionFilter.ALL ||
                getCountryRegion(country) == selectedRegionFilter
            val matchesSearch = if (countrySearchQuery.isBlank()) {
                true
            } else {
                val localizedName = context.getString(country.nameRes)
                localizedName.contains(countrySearchQuery, ignoreCase = true) ||
                    country.displayName.contains(countrySearchQuery, ignoreCase = true) ||
                    country.code.contains(countrySearchQuery, ignoreCase = true) ||
                    country.currencyCode.contains(countrySearchQuery, ignoreCase = true) ||
                    country.currencySymbol.contains(countrySearchQuery, ignoreCase = true) ||
                    country.dialCode.contains(countrySearchQuery, ignoreCase = true)
            }
            matchesRegion && matchesSearch
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(PureWhite)
            .testTag("regional_preferences_detail_view"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .padding(horizontal = LojiaDimens.ScreenPadding, vertical = LojiaDimens.ScreenTopPadding)
        ) {
            // =================================================================
            // 1. INTERNATIONAL LOCALIZATION & CURRENCY OVERVIEW CARD
            // =================================================================
            LojiaSettingsCard {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(PrimaryContainerLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Public,
                                    contentDescription = null,
                                    tint = PrimaryIndigoLight,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Localization & Currency",
                                    fontWeight = FontWeight.Bold,
                                    color = OnSurfaceLight,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Configure display language, store country & ISO currency",
                                    color = OnSurfaceVariantLight,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        // Active ISO Badge
                        Surface(
                            shape = RoundedCornerShape(LojiaDimens.ChipRadius),
                            color = PrimaryContainerLight,
                            border = BorderStroke(1.dp, PrimaryIndigoLight)
                        ) {
                            Text(
                                text = "${currentLanguage.code.uppercase()} • $activeCurrency",
                                fontWeight = FontWeight.Bold,
                                color = OnPrimaryContainerLight,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = OutlineVariantLight, thickness = 1.dp)

                    // Two-Column Active Status Strip (Language + Currency Format Preview)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Active Language Pill Box
                        Surface(
                            shape = RoundedCornerShape(LojiaDimens.InputRadius),
                            color = BackgroundLight,
                            border = BorderStroke(1.dp, OutlineLight),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(text = activeLangMeta.flag, fontSize = 20.sp)
                                Column {
                                    Text(
                                        text = "ACTIVE LANGUAGE",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextHintColor,
                                        letterSpacing = 0.4.sp
                                    )
                                    Text(
                                        text = activeLangMeta.nativeScript,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OnSurfaceLight,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // Active Currency & Format Pill Box
                        Surface(
                            shape = RoundedCornerShape(LojiaDimens.InputRadius),
                            color = BackgroundLight,
                            border = BorderStroke(1.dp, OutlineLight),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(text = countryFlag, fontSize = 20.sp)
                                Column {
                                    Text(
                                        text = "POS CURRENCY",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextHintColor,
                                        letterSpacing = 0.4.sp
                                    )
                                    Text(
                                        text = "$activeSymbol 1,250.00 ($activeCurrency)",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SuccessGreen,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            LojiaSectionHeader("REGIONAL PREFERENCES")

            // =================================================================
            // OPTION 1: LANGUAGE SELECT (Downward Expandable International Card)
            // =================================================================
            val isLangExpanded = expandedOption == 0
            val langArrowRotation by animateFloatAsState(
                targetValue = if (isLangExpanded) 180f else 0f,
                animationSpec = tween(durationMillis = 220),
                label = "language_arrow_rotation"
            )

            Card(
                shape = RoundedCornerShape(LojiaDimens.CardRadius),
                colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(
                    width = if (isLangExpanded) 1.5.dp else 1.dp,
                    color = if (isLangExpanded) PrimaryIndigoLight else OutlineLight
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = LojiaDimens.CardSpacing)
                    .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                    .testTag("card_language_select_accordion")
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Clickable Header Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isLangExpanded) {
                                    expandedOption = null
                                } else {
                                    languageBeforeExpand = currentLanguage
                                    expandedOption = 0
                                }
                            }
                            .padding(LojiaDimens.CardPadding),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(LojiaDimens.IconTextGap),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Translate,
                                contentDescription = null,
                                tint = OnSurfaceVariantLight,
                                modifier = Modifier.size(LojiaDimens.IconSize)
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Language Select",
                                    fontWeight = FontWeight.Medium,
                                    color = OnSurfaceLight,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${activeLangMeta.flag} ${activeLangMeta.internationalTitle}",
                                    color = OnSurfaceVariantLight,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // ISO Code Pill
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = PrimaryContainerLight,
                                border = BorderStroke(1.dp, PrimaryIndigoLight)
                            ) {
                                Text(
                                    text = activeLangMeta.isoBadge,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OnPrimaryContainerLight,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isLangExpanded) "Collapse" else "Expand",
                                tint = TextHintColor,
                                modifier = Modifier
                                    .size(20.dp)
                                    .rotate(langArrowRotation)
                            )
                        }
                    }

                    // Downward Expandable Language Selector Panel
                    AnimatedVisibility(
                        visible = isLangExpanded,
                        enter = expandVertically(animationSpec = tween(220)) + fadeIn(animationSpec = tween(220)),
                        exit = shrinkVertically(animationSpec = tween(180)) + fadeOut(animationSpec = tween(180))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(BackgroundLight)
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            HorizontalDivider(color = OutlineVariantLight, modifier = Modifier.padding(bottom = 2.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "SELECT INTERFACE LANGUAGE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextHintColor,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "${AppLanguage.entries.size} Languages Available",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = OnSurfaceVariantLight
                                )
                            }

                            // Language Option Cards
                            AppLanguage.entries.forEach { lang ->
                                val isSelected = lang == currentLanguage
                                val meta = getLanguageMeta(lang)

                                Surface(
                                    onClick = {
                                        reportViewModel.setLanguage(lang)
                                    },
                                    shape = RoundedCornerShape(LojiaDimens.CardRadius),
                                    color = if (isSelected) PrimaryContainerLight else SurfaceLight,
                                    shadowElevation = 0.dp,
                                    border = BorderStroke(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) PrimaryIndigoLight else OutlineLight
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("option_language_${lang.code}")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            // Flag Container
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(
                                                        if (isSelected) SurfaceLight else BackgroundLight
                                                    )
                                                    .border(
                                                        1.dp,
                                                        if (isSelected) PrimaryIndigoLight else OutlineLight,
                                                        RoundedCornerShape(10.dp)
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(text = meta.flag, fontSize = 24.sp)
                                            }

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = meta.internationalTitle,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) OnPrimaryContainerLight else OnSurfaceLight,
                                                    fontSize = 15.sp
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = meta.regionSubtitle,
                                                    fontSize = 13.sp,
                                                    color = OnSurfaceVariantLight,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // Script Direction Badge (LTR / RTL)
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (meta.directionLabel == "RTL") WarningContainer else SurfaceVariantLight
                                            ) {
                                                Text(
                                                    text = meta.directionLabel,
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (meta.directionLabel == "RTL") WarningOrange else OnSurfaceVariantLight,
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                                )
                                            }

                                            // Radio / Checkmark Indicator
                                            if (isSelected) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = PrimaryIndigoLight,
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = "Selected",
                                                            tint = PureWhite,
                                                            modifier = Modifier.size(15.dp)
                                                        )
                                                    }
                                                }
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .clip(CircleShape)
                                                        .border(1.5.dp, OutlineLight, CircleShape)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Cancel & Save Action Footer
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                LojiaOutlinedButton(
                                    text = "Cancel",
                                    onClick = {
                                        reportViewModel.setLanguage(languageBeforeExpand)
                                        expandedOption = null
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                LojiaPrimaryButton(
                                    text = "Save",
                                    onClick = {
                                        expandedOption = null
                                        Toast.makeText(
                                            context,
                                            "Language set to ${currentLanguage.displayName}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // =================================================================
            // OPTION 2: COUNTRY & CURRENCY (Downward Expandable International Card)
            // =================================================================
            val isCountryExpanded = expandedOption == 1
            val countryArrowRotation by animateFloatAsState(
                targetValue = if (isCountryExpanded) 180f else 0f,
                animationSpec = tween(durationMillis = 220),
                label = "country_arrow_rotation"
            )

            Card(
                shape = RoundedCornerShape(LojiaDimens.CardRadius),
                colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(
                    width = if (isCountryExpanded) 1.5.dp else 1.dp,
                    color = if (isCountryExpanded) PrimaryIndigoLight else OutlineLight
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = LojiaDimens.CardSpacing)
                    .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                    .testTag("card_country_currency_accordion")
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Clickable Header Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isCountryExpanded) {
                                    expandedOption = null
                                } else {
                                    countryBeforeExpand = currentCountry
                                    expandedOption = 1
                                }
                            }
                            .padding(LojiaDimens.CardPadding),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(LojiaDimens.IconTextGap),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CurrencyExchange,
                                contentDescription = null,
                                tint = OnSurfaceVariantLight,
                                modifier = Modifier.size(LojiaDimens.IconSize)
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Country & Currency",
                                    fontWeight = FontWeight.Medium,
                                    color = OnSurfaceLight,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$countryFlag ${stringResource(currentCountry.nameRes)} • $activeCurrency ($activeSymbol)",
                                    color = OnSurfaceVariantLight,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Active Currency Pill
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SuccessContainer,
                                border = BorderStroke(1.dp, SuccessGreen)
                            ) {
                                Text(
                                    text = "$activeSymbol $activeCurrency",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SuccessGreen,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isCountryExpanded) "Collapse" else "Expand",
                                tint = TextHintColor,
                                modifier = Modifier
                                    .size(20.dp)
                                    .rotate(countryArrowRotation)
                            )
                        }
                    }

                    // Downward Expandable Country & Currency Selector Panel
                    AnimatedVisibility(
                        visible = isCountryExpanded,
                        enter = expandVertically(animationSpec = tween(220)) + fadeIn(animationSpec = tween(220)),
                        exit = shrinkVertically(animationSpec = tween(180)) + fadeOut(animationSpec = tween(180))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(BackgroundLight)
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            HorizontalDivider(color = OutlineVariantLight, modifier = Modifier.padding(bottom = 2.dp))

                            // Selected Country & Currency Live Summary Bar
                            Surface(
                                shape = RoundedCornerShape(LojiaDimens.CardRadius),
                                color = PrimaryContainerLight,
                                border = BorderStroke(1.dp, PrimaryIndigoLight),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(text = countryFlag, fontSize = 24.sp)
                                        Column {
                                            Text(
                                                text = "${stringResource(currentCountry.nameRes)} (${currentCountry.code})",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.5.sp,
                                                color = OnPrimaryContainerLight
                                            )
                                            Text(
                                                text = "Dial: ${currentCountry.dialCode} • Standard VAT: ${currentCountry.defaultVatRate}%",
                                                fontSize = 11.5.sp,
                                                color = PrimaryIndigoLight
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = SurfaceLight,
                                        border = BorderStroke(1.dp, PrimaryIndigoLight)
                                    ) {
                                        Text(
                                            text = "$activeCurrency ($activeSymbol)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.5.sp,
                                            color = PrimaryIndigoLight,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }

                            // International Search Box
                            OutlinedTextField(
                                value = countrySearchQuery,
                                onValueChange = { countrySearchQuery = it },
                                placeholder = {
                                    Text(
                                        text = "Search country, currency or ISO code (e.g. BD, SAR, USD, $)",
                                        fontSize = 13.sp,
                                        color = TextHintColor
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Search,
                                        contentDescription = null,
                                        tint = OnSurfaceVariantLight
                                    )
                                },
                                trailingIcon = if (countrySearchQuery.isNotEmpty()) {
                                    {
                                        IconButton(onClick = { countrySearchQuery = "" }) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Clear",
                                                tint = OnSurfaceVariantLight,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                } else null,
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = OnSurfaceLight,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = OnSurfaceLight,
                                    unfocusedTextColor = OnSurfaceLight,
                                    focusedContainerColor = SurfaceLight,
                                    unfocusedContainerColor = SurfaceLight,
                                    focusedBorderColor = PrimaryIndigoLight,
                                    unfocusedBorderColor = OutlineLight,
                                    cursorColor = PrimaryIndigoLight
                                ),
                                shape = RoundedCornerShape(LojiaDimens.InputRadius),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_search_country_currency")
                            )

                            // Horizontal World Region Filter Pills
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                WorldRegionFilter.entries.forEach { region ->
                                    val isRegionSelected = selectedRegionFilter == region
                                    Surface(
                                        onClick = { selectedRegionFilter = region },
                                        shape = RoundedCornerShape(LojiaDimens.ChipRadius),
                                        color = if (isRegionSelected) PrimaryContainerLight else SurfaceVariantLight,
                                        border = BorderStroke(
                                            1.dp,
                                            if (isRegionSelected) PrimaryIndigoLight else OutlineLight
                                        )
                                    ) {
                                        Text(
                                            text = region.label,
                                            fontSize = 12.sp,
                                            fontWeight = if (isRegionSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isRegionSelected) PrimaryIndigoLight else OnSurfaceVariantLight,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }

                            // Country & Currency Cards List
                            if (filteredCountries.isEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(LojiaDimens.CardRadius),
                                    color = SurfaceLight,
                                    border = BorderStroke(1.dp, OutlineLight),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.SearchOff,
                                            contentDescription = null,
                                            tint = TextHintColor,
                                            modifier = Modifier.size(64.dp)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "No country found",
                                            fontSize = 16.sp,
                                            color = OnSurfaceVariantLight
                                        )
                                    }
                                }
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    filteredCountries.forEach { country ->
                                        val isSelected = country == currentCountry
                                        val flag = countryCodeToFlagEmoji(country.code)
                                        val sym = getCurrencySymbol(country.currencyCode)
                                        val countryName = stringResource(country.nameRes)

                                        Surface(
                                            onClick = {
                                                reportViewModel.setCountry(country)
                                                reportViewModel.saveBusinessProfile(
                                                    bProfile.copy(
                                                        country = country.displayName,
                                                        currency = country.currencyCode
                                                    )
                                                )
                                            },
                                            shape = RoundedCornerShape(LojiaDimens.CardRadius),
                                            color = if (isSelected) PrimaryContainerLight else SurfaceLight,
                                            shadowElevation = 0.dp,
                                            border = BorderStroke(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) PrimaryIndigoLight else OutlineLight
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("option_country_${country.code}")
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 11.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    // Flag Badge
                                                    Box(
                                                        modifier = Modifier
                                                            .size(42.dp)
                                                            .clip(RoundedCornerShape(10.dp))
                                                            .background(
                                                                if (isSelected) SurfaceLight else BackgroundLight
                                                            )
                                                            .border(
                                                                1.dp,
                                                                if (isSelected) PrimaryIndigoLight else OutlineLight,
                                                                RoundedCornerShape(10.dp)
                                                            ),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(text = flag, fontSize = 22.sp)
                                                    }

                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = countryName,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                            color = if (isSelected) OnPrimaryContainerLight else OnSurfaceLight,
                                                            fontSize = 15.sp,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Text(
                                                            text = "${country.displayName} • ${country.code} (${country.dialCode})",
                                                            fontSize = 13.sp,
                                                            color = OnSurfaceVariantLight,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }

                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    // International Currency Capsule (Symbol + ISO Code)
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = if (isSelected) SurfaceLight else BackgroundLight,
                                                        border = BorderStroke(
                                                            1.dp,
                                                            if (isSelected) PrimaryIndigoLight else OutlineLight
                                                        )
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(20.dp)
                                                                    .clip(CircleShape)
                                                                    .background(
                                                                        if (isSelected) SuccessContainer else PrimaryContainerLight
                                                                    ),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Text(
                                                                    text = sym,
                                                                    fontSize = 10.5.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = if (isSelected) SuccessGreen else PrimaryIndigoLight
                                                                )
                                                            }
                                                            Text(
                                                                text = country.currencyCode,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (isSelected) OnPrimaryContainerLight else OnSurfaceLight,
                                                                fontSize = 12.5.sp
                                                            )
                                                        }
                                                    }

                                                    // Selection Checkmark / Radio Circle
                                                    if (isSelected) {
                                                        Surface(
                                                            shape = CircleShape,
                                                            color = PrimaryIndigoLight,
                                                            modifier = Modifier.size(24.dp)
                                                        ) {
                                                            Box(contentAlignment = Alignment.Center) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Check,
                                                                    contentDescription = "Selected",
                                                                    tint = PureWhite,
                                                                    modifier = Modifier.size(15.dp)
                                                                )
                                                            }
                                                        }
                                                    } else {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(24.dp)
                                                                .clip(CircleShape)
                                                                .border(1.5.dp, OutlineLight, CircleShape)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Cancel & Save Action Footer
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                LojiaOutlinedButton(
                                    text = "Cancel",
                                    onClick = {
                                        reportViewModel.setCountry(countryBeforeExpand)
                                        reportViewModel.saveBusinessProfile(
                                            bProfile.copy(
                                                country = countryBeforeExpand.displayName,
                                                currency = countryBeforeExpand.currencyCode
                                            )
                                        )
                                        expandedOption = null
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                LojiaPrimaryButton(
                                    text = "Save",
                                    onClick = {
                                        expandedOption = null
                                        val countryName = context.getString(currentCountry.nameRes)
                                        Toast.makeText(
                                            context,
                                            "Country & Currency saved: $countryName (${currentCountry.currencyCode})",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
