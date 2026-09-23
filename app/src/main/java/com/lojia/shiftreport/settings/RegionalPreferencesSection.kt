package com.lojia.shiftreport.settings

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.*
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
import com.lojia.shiftreport.data.AppCountry
import com.lojia.shiftreport.data.AppLanguage
import com.lojia.shiftreport.data.BusinessProfile
import com.lojia.shiftreport.report.ReportViewModel
import com.lojia.shiftreport.ui.common.LojiaTextField
import com.lojia.shiftreport.ui.theme.PrimaryIndigo

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
 * Data holder for currency option items derived from AppCountry or custom presets.
 */
data class CurrencyOption(
    val code: String,
    val symbol: String,
    val countryNameRes: Int
)

/**
 * Modern, unified "Regional & Preferences" card for the main settings overview.
 * Displays Country, Language, and Currency under a single unbreakable section.
 */
@Composable
fun RegionalPreferencesCard(
    currentCountry: AppCountry,
    currentLanguage: AppLanguage,
    businessProfile: BusinessProfile?,
    onOpenRegionalMenu: (initialTab: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeCurrency = businessProfile?.currency ?: currentCountry.currencyCode
    val activeSymbol = getCurrencySymbol(activeCurrency)
    val countryFlag = countryCodeToFlagEmoji(currentCountry.code)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("regional_preferences_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFEEF2FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Public,
                        contentDescription = null,
                        tint = Color(0xFF4F46E5),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.regional_interface_section),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.centralized_sync_desc),
                        fontSize = 11.5.sp,
                        color = Color(0xFF64748B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

            // 1. Country Selector Row
            RegionalPreferenceItemRow(
                emojiText = countryFlag,
                iconBgColor = Color(0xFFEFF6FF),
                iconTint = Color(0xFF2563EB),
                title = stringResource(R.string.country),
                subtitle = stringResource(currentCountry.nameRes),
                badgeText = "$countryFlag ${currentCountry.code}",
                badgeBgColor = Color(0xFFDBEAFE),
                badgeTextColor = Color(0xFF1E40AF),
                onClick = { onOpenRegionalMenu(1) },
                testTag = "regional_row_country"
            )

            HorizontalDivider(color = Color(0xFFF8FAFC), thickness = 0.5.dp)

            // 2. Language Selector Row
            RegionalPreferenceItemRow(
                emojiText = "🌐",
                iconBgColor = Color(0xFFF5F3FF),
                iconTint = Color(0xFF7C3AED),
                title = stringResource(R.string.language),
                subtitle = currentLanguage.displayName,
                badgeText = currentLanguage.code.uppercase(),
                badgeBgColor = Color(0xFFEDE9FE),
                badgeTextColor = Color(0xFF5B21B6),
                onClick = { onOpenRegionalMenu(0) },
                testTag = "regional_row_language"
            )

            HorizontalDivider(color = Color(0xFFF8FAFC), thickness = 0.5.dp)

            // 3. Currency Selector Row
            RegionalPreferenceItemRow(
                emojiText = activeSymbol,
                iconBgColor = Color(0xFFECFDF5),
                iconTint = Color(0xFF059669),
                title = stringResource(R.string.currency),
                subtitle = "$activeCurrency ($activeSymbol)",
                badgeText = "$activeSymbol $activeCurrency",
                badgeBgColor = Color(0xFFD1FAE5),
                badgeTextColor = Color(0xFF065F46),
                onClick = { onOpenRegionalMenu(2) },
                testTag = "regional_row_currency"
            )
        }
    }
}

/**
 * Reusable, overflow-safe single preference row.
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
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 52.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(iconBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    if (!emojiText.isNullOrBlank()) {
                        Text(
                            text = emojiText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = iconTint
                        )
                    } else if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E293B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
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
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeBgColor)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Detailed 3-Tab View for Regional & Preferences settings (Language, Country, Currency).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegionalPreferencesDetailView(
    reportViewModel: ReportViewModel,
    initialTab: Int = 0,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentLanguage by reportViewModel.currentLanguage.collectAsState()
    val currentCountry by reportViewModel.currentCountry.collectAsState()
    val businessProfile by reportViewModel.businessProfile.collectAsState()

    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 2)) }
    var searchQuery by remember { mutableStateOf("") }

    val activeCurrency = businessProfile?.currency ?: currentCountry.currencyCode

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("regional_preferences_detail_view")
    ) {
        // Segmented 3-Tab Control
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color(0xFFF8FAFC),
            contentColor = Color(0xFF4F46E5),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Tab 0: Language
            Tab(
                selected = selectedTab == 0,
                onClick = {
                    selectedTab = 0
                    searchQuery = ""
                },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Translate,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.language),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            )

            // Tab 1: Country
            Tab(
                selected = selectedTab == 1,
                onClick = {
                    selectedTab = 1
                    searchQuery = ""
                },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Public,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.country),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            )

            // Tab 2: Currency
            Tab(
                selected = selectedTab == 2,
                onClick = {
                    selectedTab = 2
                    searchQuery = ""
                },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Payments,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.currency),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Medium
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (selectedTab) {
            // ==========================================
            // 🌐 TAB 0: LANGUAGE SELECTION
            // ==========================================
            0 -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    LanguageSettingsComponent(
                        currentLanguage = currentLanguage,
                        accentColor = PrimaryIndigo,
                        onLanguageChanged = { newLang ->
                            reportViewModel.setLanguage(newLang)
                        }
                    )
                }
            }

            // ==========================================
            // 🌍 TAB 1: COUNTRY SELECTION
            // ==========================================
            1 -> {
                Column(modifier = Modifier.fillMaxWidth()) {
                    LojiaTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(stringResource(R.string.search_country), fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    val allCountries = AppCountry.entries.filter { c ->
                        val localizedName = context.getString(c.nameRes)
                        localizedName.contains(searchQuery, ignoreCase = true) ||
                        c.displayName.contains(searchQuery, ignoreCase = true) ||
                        c.code.contains(searchQuery, ignoreCase = true) ||
                        c.currencyCode.contains(searchQuery, ignoreCase = true)
                    }

                    allCountries.forEach { country ->
                        val isSelected = country == currentCountry
                        val displayName = stringResource(country.nameRes)

                        Surface(
                            color = if (isSelected) Color(0xFFF5F3FF) else Color.White,
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 56.dp)
                                .clickable {
                                    reportViewModel.setCountry(country)
                                    Toast.makeText(
                                        context,
                                        context.getString(
                                            R.string.country_set_toast_fmt,
                                            displayName,
                                            country.currencyCode,
                                            country.currencySymbol
                                        ),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    val flagEmoji = countryCodeToFlagEmoji(country.code)
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF1F5F9)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = flagEmoji,
                                            fontSize = 20.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = displayName,
                                            fontSize = 14.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                            color = if (isSelected) Color(0xFF4F46E5) else Color(0xFF1E293B),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${country.currencyCode} (${country.currencySymbol})",
                                            fontSize = 12.sp,
                                            color = if (isSelected) Color(0xFF6366F1) else Color(0xFF64748B),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = stringResource(R.string.cd_selected),
                                        tint = Color(0xFF4F46E5),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 0.5.dp)
                    }
                }
            }

            // ==========================================
            // 💰 TAB 2: CURRENCY SELECTION
            // ==========================================
            2 -> {
                Column(modifier = Modifier.fillMaxWidth()) {
                    LojiaTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(stringResource(R.string.search_country), fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    val currencyList = remember {
                        AppCountry.entries.map { c ->
                            CurrencyOption(
                                code = c.currencyCode,
                                symbol = c.currencySymbol,
                                countryNameRes = c.nameRes
                            )
                        }.distinctBy { it.code }
                    }

                    val filteredCurrencies = currencyList.filter { cur ->
                        val countryName = context.getString(cur.countryNameRes)
                        cur.code.contains(searchQuery, ignoreCase = true) ||
                        cur.symbol.contains(searchQuery, ignoreCase = true) ||
                        countryName.contains(searchQuery, ignoreCase = true)
                    }

                    filteredCurrencies.forEach { currency ->
                        val isSelected = currency.code.equals(activeCurrency, ignoreCase = true)
                        val countryName = stringResource(currency.countryNameRes)

                        Surface(
                            color = if (isSelected) Color(0xFFECFDF5) else Color.White,
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 56.dp)
                                .clickable {
                                    reportViewModel.setCurrency(currency.code)
                                    Toast.makeText(
                                        context,
                                        "Currency set to ${currency.code} (${currency.symbol})",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    val currencySymbol = getCurrencySymbol(currency.code)
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) Color(0xFFECFDF5) else Color(0xFFF1F5F9)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = currencySymbol,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color(0xFF059669) else Color(0xFF334155),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${currency.code} (${currency.symbol})",
                                            fontSize = 14.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                            color = if (isSelected) Color(0xFF059669) else Color(0xFF1E293B),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = countryName,
                                            fontSize = 12.sp,
                                            color = if (isSelected) Color(0xFF10B981) else Color(0xFF64748B),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = stringResource(R.string.cd_selected),
                                        tint = Color(0xFF059669),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}
