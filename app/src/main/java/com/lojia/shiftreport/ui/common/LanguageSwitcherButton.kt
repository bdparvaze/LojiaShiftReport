package com.lojia.shiftreport.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.lojia.shiftreport.R
import com.lojia.shiftreport.data.AppLanguage
import com.lojia.shiftreport.data.supportedAppLanguages
import com.lojia.shiftreport.ui.theme.Dimens
import com.lojia.shiftreport.ui.theme.depthCard
import com.lojia.shiftreport.util.AppLanguageManager
import com.lojia.shiftreport.util.LanguagePreferences
import kotlinx.coroutines.launch

/**
 * Compact, RTL-aware language switcher button for pre-authentication screens
 * (Login, Register, Password Recovery). Displays a globe icon and the active
 * language's native name, and opens a compact [LojiaDialog] listing all supported
 * languages dynamically from [supportedAppLanguages].
 */
@Composable
fun LanguageSwitcherButton(
    currentLanguage: AppLanguage,
    modifier: Modifier = Modifier,
    testTag: String = "btn_language_switcher",
    onLanguageSelected: (AppLanguage) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showDialog by rememberSaveable { mutableStateOf(false) }

    Surface(
        onClick = { showDialog = true },
        shape = RoundedCornerShape(Dimens.RadiusFull),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(Dimens.BorderThin, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .defaultMinSize(minWidth = Dimens.MinTouchTarget, minHeight = Dimens.MinTouchTarget)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Dimens.SpacingMd, vertical = Dimens.SpacingSm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingXs)
        ) {
            Icon(
                imageVector = Icons.Outlined.Language,
                contentDescription = stringResource(R.string.auth_change_language_title),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(Dimens.IconSm)
            )
            Text(
                text = currentLanguage.nativeName,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    if (showDialog) {
        val languages = supportedAppLanguages()
        LojiaDialog(
            onDismissRequest = { showDialog = false },
            size = DialogSize.Small
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dialog_language_switcher")
            ) {
                Text(
                    text = stringResource(R.string.auth_change_language_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(Dimens.SpacingMd))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = Dimens.DialogListMaxHeight)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)
                ) {
                    languages.forEach { lang ->
                        val isSelected = lang == currentLanguage
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = Dimens.MinTouchTarget)
                                .depthCard(
                                    accent = isSelected,
                                    shape = RoundedCornerShape(Dimens.RadiusMd)
                                )
                                .clickable {
                                    coroutineScope.launch {
                                        LanguagePreferences.saveLanguageDataStore(context, lang.code)
                                        AppLanguageManager.changeLanguage(context, lang)
                                        showDialog = false
                                        onLanguageSelected(lang)
                                    }
                                }
                                .padding(
                                    horizontal = Dimens.ListRowPaddingHorizontal,
                                    vertical = Dimens.ListRowPaddingVertical
                                )
                                .testTag("btn_language_option_${lang.code}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = lang.nativeName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = stringResource(R.string.cd_selected),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(Dimens.IconMd)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
