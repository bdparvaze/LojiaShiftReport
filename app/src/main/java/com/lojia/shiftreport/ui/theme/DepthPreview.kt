package com.lojia.shiftreport.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.lojia.shiftreport.R
import com.lojia.shiftreport.data.AppLanguage

@Composable
private fun DepthFoundationShowcase() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BackgroundLight)
            .padding(bottom = Dimens.SpacingLg),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMd)
    ) {
        LojiaGradientHeader {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    color = OnPrimaryLight
                )
                LojiaLiveDot()
            }
            Spacer(modifier = Modifier.height(Dimens.SpacingXs))
            Text(
                text = stringResource(R.string.nav_reports),
                style = MaterialTheme.typography.bodyMedium,
                color = OnPrimaryLight
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.SpacingLg),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMd)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .cardRise(index = 0)
                    .depthCard(accent = false)
                    .padding(Dimens.SpacingLg)
            ) {
                Text(
                    text = stringResource(R.string.cash),
                    style = MaterialTheme.typography.titleMedium,
                    color = OnSurfaceLight
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .cardRise(index = 1)
                    .depthCard(accent = true)
                    .padding(Dimens.SpacingLg)
            ) {
                Text(
                    text = stringResource(R.string.total),
                    style = MaterialTheme.typography.titleMedium,
                    color = OnPrimaryContainerLight
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .depthInset()
                    .padding(horizontal = Dimens.SpacingMd, vertical = Dimens.SpacingMd)
            ) {
                Text(
                    text = stringResource(R.string.nav_reports),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariantLight
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .depthRaisedButton(enabled = true)
                        .clickable { },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.save),
                        style = MaterialTheme.typography.labelLarge,
                        color = OnPrimaryLight,
                        modifier = Modifier.padding(
                            horizontal = Dimens.SpacingLg,
                            vertical = Dimens.SpacingSm
                        )
                    )
                }

                Spacer(modifier = Modifier.width(Dimens.SpacingXs))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .depthRaisedButton(enabled = false),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.back),
                        style = MaterialTheme.typography.labelLarge,
                        color = TextHintColor,
                        modifier = Modifier.padding(
                            horizontal = Dimens.SpacingLg,
                            vertical = Dimens.SpacingSm
                        )
                    )
                }
            }
        }
    }
}

@Preview(name = "Depth Showcase - English LTR", showBackground = true, locale = "en")
@Composable
internal fun DepthPreviewEnglishLtr() {
    LojiaTheme(language = AppLanguage.ENGLISH) {
        DepthFoundationShowcase()
    }
}

@Preview(name = "Depth Showcase - Bengali LTR", showBackground = true, locale = "bn")
@Composable
internal fun DepthPreviewBengaliLtr() {
    LojiaTheme(language = AppLanguage.BENGALI) {
        DepthFoundationShowcase()
    }
}

@Preview(name = "Depth Showcase - Arabic RTL", showBackground = true, locale = "ar")
@Composable
internal fun DepthPreviewArabicRtl() {
    LojiaTheme(language = AppLanguage.ARABIC) {
        DepthFoundationShowcase()
    }
}
