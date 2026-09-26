package com.lojia.shiftreport.settings

import com.lojia.shiftreport.ui.common.LojiaTextField
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
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojia.shiftreport.data.AppLanguage
import com.lojia.shiftreport.util.AppLanguageManager
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
                secondaryCode = if (currentLanguage.code == "ar") "en" else "ar",
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
        LojiaSectionHeader("APPLICATION LANGUAGE")

        // =========================================================================
        // LANGUAGE CATALOG LIST (English, Bengali, Arabic)
        // =========================================================================
        val availableLanguages = remember { AppLanguage.entries }

        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            availableLanguages.forEach { lang ->
                val isSelected = activeAppLanguage == lang
                LanguageOptionRow(
                    language = lang,
                    isSelected = isSelected,
                    accentColor = accentColor,
                    onClick = {
                        coroutineScope.launch {
                            LanguagePreferences.saveLanguageDataStore(context, lang.code)
                            AppLanguageManager.changeLanguage(context, lang.code)
                            onLanguageChanged(lang)
                            Toast.makeText(
                                context,
                                context.getString(R.string.language_saved_datastore, lang.displayName),
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
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(LojiaDimens.CardRadius),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) PrimaryContainerLight else SurfaceLight
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) accentColor else OutlineLight
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = LojiaDimens.CardSpacing)
            .defaultMinSize(minHeight = 56.dp)
            .clip(RoundedCornerShape(LojiaDimens.CardRadius))
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
                horizontalArrangement = Arrangement.spacedBy(LojiaDimens.IconTextGap),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Language,
                    contentDescription = null,
                    tint = if (isSelected) accentColor else OnSurfaceVariantLight,
                    modifier = Modifier.size(LojiaDimens.IconSize)
                )

                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = language.nativeName,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 15.sp,
                            color = if (isSelected) OnPrimaryContainerLight else OnSurfaceLight,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (language.isRtl) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = WarningContainer
                            ) {
                                Text(
                                    text = "RTL",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WarningOrange,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${language.displayName} (${language.code.uppercase()})",
                        fontSize = 13.sp,
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
                    modifier = Modifier.size(22.dp)
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
                secondaryCode = "ar"
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
        Icon(
            imageVector = Icons.Default.Language,
            contentDescription = null,
            tint = OnSurfaceVariantLight,
            modifier = Modifier.size(LojiaDimens.IconSize)
        )

        Spacer(modifier = Modifier.width(LojiaDimens.IconTextGap))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Display Language",
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = OnSurfaceLight
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${currentLang.displayName} (DataStore)",
                fontSize = 13.sp,
                color = OnSurfaceVariantLight
            )
        }

        // Quick Toggle Switch
        Switch(
            checked = currentLang.code == secondaryLang.code,
            onCheckedChange = { _ ->
                coroutineScope.launch {
                    val newLang = LanguagePreferences.toggleLanguage(context)
                    AppLanguageManager.changeLanguage(context, newLang.code)
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
