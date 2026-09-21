package com.lojia.pos.settings

import com.lojia.pos.ui.common.LojiaTextField
import com.lojia.pos.R
import com.lojia.pos.data.*
import com.lojia.pos.util.*
import com.lojia.pos.ui.common.*
import com.lojia.pos.ui.theme.*
import com.lojia.pos.auth.*
import com.lojia.pos.report.*
import com.lojia.pos.settings.*

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.SwapHoriz
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojia.pos.data.AppLanguage
import com.lojia.pos.util.AppLanguageManager
import com.lojia.pos.util.LanguagePreferenceData
import com.lojia.pos.util.LanguagePreferences
import kotlinx.coroutines.launch

/**
 * Modern Material 3 Settings Component powered by Jetpack DataStore Preferences
 * for viewing, storing, and toggling the application's display language.
 *
 * Features:
 * - Real-time observation of DataStore Preferences (`selected_language_tag` key)
 * - Quick Bilingual Toggle Switch (e.g. English ⇄ Arabic / Bengali)
 * - Horizontal quick-action pills for top business languages
 * - Searchable catalog of 37+ world languages with RTL indicators
 * - Visual DataStore persistence status badge
 */
@Composable
fun LanguageSettingsComponent(
    currentLanguage: AppLanguage,
    onLanguageChanged: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = LoyverseGreenPrimary
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
            .testTag("language_settings_component"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // =========================================================================
        // LANGUAGE CATALOG LIST (English, Bengali, Arabic)
        // =========================================================================
        val availableLanguages = remember { AppLanguage.entries }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
 * Individual Language Item Row with Flag, Native Name, English Name, RTL Badge, and DataStore indicator.
 */
@Composable
private fun LanguageOptionRow(
    language: AppLanguage,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) accentColor.copy(alpha = 0.12f) else Color.Transparent,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)) else null,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .testTag("language_row_${language.code}")
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
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = language.flag,
                    fontSize = 22.sp
                )

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = language.nativeName,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 15.sp,
                            color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface
                        )
                        if (language.isRtl) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFF59E0B).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "RTL",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB45309),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "${language.displayName} (${language.code})",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = stringResource(R.string.cd_active_language_datastore),
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
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
    accentColor: Color = LoyverseGreenPrimary,
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

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("compact_language_toggle_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column {
                    Text(
                        text = "Display Language",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "${currentLang.flag} ${currentLang.displayName} (DataStore)",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
                    checkedThumbColor = Color.White,
                    checkedTrackColor = accentColor
                )
            )
        }
    }
}
