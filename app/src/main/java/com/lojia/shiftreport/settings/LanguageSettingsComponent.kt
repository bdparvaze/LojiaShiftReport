package com.lojia.shiftreport.settings

import com.lojia.shiftreport.R
import com.lojia.shiftreport.data.*
import com.lojia.shiftreport.util.*
import com.lojia.shiftreport.ui.common.*
import com.lojia.shiftreport.ui.theme.*
import com.lojia.shiftreport.auth.*
import com.lojia.shiftreport.report.*
import com.lojia.shiftreport.settings.*

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.lojia.shiftreport.data.AppLanguage
import com.lojia.shiftreport.ui.common.LojiaDimens
import com.lojia.shiftreport.ui.common.LojiaSectionHeader
import com.lojia.shiftreport.ui.common.LojiaSettingsCard
import com.lojia.shiftreport.util.LanguagePreferenceData
import com.lojia.shiftreport.util.LanguagePreferences
import kotlinx.coroutines.launch

/**
 * Modern Material 3 Settings Component powered by Jetpack DataStore Preferences
 * for viewing, storing, and toggling the application's display language.
 */
@Composable
fun LanguageSettingsComponent(
    currentLanguage: AppLanguage,
    onLanguageChanged: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = PrimaryIndigoLight
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 1. Reactive observation of Jetpack DataStore Preferences
    val datastoreData by LanguagePreferences.getLanguagePreferenceDataFlow(context)
        .collectAsState(
            initial = LanguagePreferenceData(
                currentCode = currentLanguage.code,
                secondaryCode = if (currentLanguage == AppLanguage.ARABIC) AppLanguage.ENGLISH.code else AppLanguage.ARABIC.code,
                lastUpdatedMillis = System.currentTimeMillis()
            )
        )

    val activeAppLanguage = remember(datastoreData.currentCode) {
        AppLanguage.fromCode(datastoreData.currentCode)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("language_settings_component")
    ) {
        LojiaSectionHeader(stringResource(R.string.application_language_header))

        // =========================================================================
        // LANGUAGE CATALOG LIST (English, Bengali, Arabic)
        // =========================================================================
        val availableLanguages = remember { AppLanguage.entries }

        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            availableLanguages.forEachIndexed { index, lang ->
                val isSelected = activeAppLanguage == lang
                LanguageOptionRow(
                    language = lang,
                    isSelected = isSelected,
                    accentColor = accentColor,
                    index = index,
                    onClick = {
                        coroutineScope.launch {
                            val prefsRepo = PreferencesRepository.getInstance(context)
                            val activeUserId = LanguagePreferences.resolveUserId(prefsRepo.getSavedUsername())
                            LanguagePreferences.saveDeviceAndUserLanguage(context, activeUserId, lang.code)
                            ConfigurationSyncManager.getInstance(context).setLanguage(lang, activeUserId)
                            onLanguageChanged(lang)
                            val nativeNameRes = when (lang) {
                                AppLanguage.ENGLISH -> R.string.lang_native_en
                                AppLanguage.BENGALI -> R.string.lang_native_bn
                                AppLanguage.ARABIC -> R.string.lang_native_ar
                            }
                            Toast.makeText(
                                context,
                                context.getString(R.string.language_saved_datastore, context.getString(nativeNameRes)),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
            }
        }
    }
}

/**
 * Individual Language Item Row with Native Name, English Name, RTL Badge, and Selection indicator.
 */
@Composable
private fun LanguageOptionRow(
    language: AppLanguage,
    isSelected: Boolean,
    accentColor: Color,
    index: Int = 0,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = LojiaDimens.CardSpacing)
            .cardRise(index)
            .defaultMinSize(minHeight = Dimens.MinTouchTarget)
            .depthCard(
                accent = isSelected,
                shape = RoundedCornerShape(LojiaDimens.CardRadius)
            )
            .clickable { onClick() }
            .testTag("language_row_${language.code}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(LojiaDimens.CardPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.SettingsIconTitleGap),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Box(
                    modifier = Modifier
                        .size(Dimens.SettingsIconTileSize)
                        .depthRaisedIconTile(
                            gradientColors = if (isSelected) {
                                listOf(PureWhite, BadgeBlueBg)
                            } else {
                                listOf(PureWhite, CashWellBg)
                            },
                            cornerRadius = Dimens.RadiusSettingsIconTile
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Language,
                        contentDescription = null,
                        tint = if (isSelected) PrimaryBlueLight else OnSurfaceVariantLight,
                        modifier = Modifier.size(Dimens.IconField)
                    )
                }

                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)
                    ) {
                        val nativeName = when (language) {
                            AppLanguage.ENGLISH -> stringResource(R.string.lang_native_en)
                            AppLanguage.BENGALI -> stringResource(R.string.lang_native_bn)
                            AppLanguage.ARABIC -> stringResource(R.string.lang_native_ar)
                        }
                        Text(
                            text = nativeName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) BadgeBlueText else TextPrimaryReport,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (language.isRtl) {
                            Surface(
                                shape = RoundedCornerShape(Dimens.RadiusFull),
                                color = BadgeAmberBg
                            ) {
                                Text(
                                    text = stringResource(R.string.lang_rtl_badge),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = BadgeAmberTextDark,
                                    modifier = Modifier.padding(horizontal = Dimens.SpacingSm, vertical = Dimens.SpacingXxs)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(Dimens.SpacingXxs))
                    Text(
                        text = "${stringResource(language.titleRes)} (${language.code.uppercase()})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariantLight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = stringResource(R.string.cd_active_language_datastore),
                    tint = accentColor,
                    modifier = Modifier.size(Dimens.IconMd)
                )
            }
        }
    }
}

/**
 * Compact standalone card for embedding in summary settings dashboards or drawers.
 * Uses DataStore Preferences directly to toggle the display language.
 */
@Composable
fun LanguageToggleSettingsCard(
    modifier: Modifier = Modifier,
    accentColor: Color = PrimaryIndigoLight,
    onLanguageChanged: (AppLanguage) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val datastoreData by LanguagePreferences.getLanguagePreferenceDataFlow(context)
        .collectAsState(
            initial = LanguagePreferenceData(
                currentCode = LanguagePreferences.getLanguage(context),
                secondaryCode = AppLanguage.ARABIC.code
            )
        )

    val currentLang = remember(datastoreData.currentCode) {
        AppLanguage.fromCode(datastoreData.currentCode)
    }
    val secondaryLang = remember(datastoreData.secondaryCode) {
        AppLanguage.fromCode(datastoreData.secondaryCode)
    }

    LojiaSettingsCard(
        modifier = modifier.testTag("compact_language_toggle_card")
    ) {
        Box(
            modifier = Modifier
                .size(Dimens.SettingsIconTileSize)
                .depthRaisedIconTile(
                    gradientColors = listOf(PureWhite, BadgeBlueBg),
                    cornerRadius = Dimens.RadiusSettingsIconTile
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Language,
                contentDescription = null,
                tint = PrimaryBlueLight,
                modifier = Modifier.size(Dimens.IconField)
            )
        }

        Spacer(modifier = Modifier.width(Dimens.SettingsIconTitleGap))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.lang_display_language),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimaryReport
            )
            Spacer(modifier = Modifier.height(Dimens.SpacingXxs))
            Text(
                text = stringResource(R.string.lang_datastore_value_fmt, stringResource(currentLang.titleRes)),
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariantLight
            )
        }

        // Quick Toggle Switch
        Switch(
            checked = currentLang.code == secondaryLang.code,
            onCheckedChange = { _ ->
                coroutineScope.launch {
                    val prefsRepo = PreferencesRepository.getInstance(context)
                    val activeUserId = LanguagePreferences.resolveUserId(prefsRepo.getSavedUsername())
                    val newLang = LanguagePreferences.toggleLanguage(context)
                    LanguagePreferences.saveDeviceAndUserLanguage(context, activeUserId, newLang.code)
                    ConfigurationSyncManager.getInstance(context).setLanguage(newLang, activeUserId)
                    onLanguageChanged(newLang)
                }
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = PureWhite,
                checkedTrackColor = accentColor,
                uncheckedThumbColor = PureWhite,
                uncheckedTrackColor = OutlineLight
            )
        )
    }
}
