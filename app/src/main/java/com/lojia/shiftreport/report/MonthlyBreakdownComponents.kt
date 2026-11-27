package com.lojia.shiftreport.report

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.lojia.shiftreport.R
import com.lojia.shiftreport.ui.theme.*
import com.lojia.shiftreport.util.MoneyFormat

@Composable
fun WeeklyBreakdownCard(
    weeklyBreakdowns: List<Pair<String, Double>>,
    totalMonthRevenue: Double,
    currency: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(Dimens.CardCornerRadius),
        color = Color.Transparent,
        modifier = modifier
            .fillMaxWidth()
            .cardRise(5)
            .depthCard(shape = RoundedCornerShape(Dimens.CardCornerRadius))
    ) {
        Column(modifier = Modifier.padding(Dimens.CardPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ViewWeek,
                        contentDescription = null,
                        tint = PrimaryIndigo,
                        modifier = Modifier.size(Dimens.IconAction)
                    )
                    Spacer(modifier = Modifier.width(Dimens.SpacingSm))
                    Text(
                        text = stringResource(R.string.weekly_breakdown),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Text(
                    text = stringResource(R.string.msg_5week_distribution),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondaryLight
                )
            }

            Spacer(modifier = Modifier.height(Dimens.SpacingMd))

            weeklyBreakdowns.forEach { (weekLabel, weekAmount) ->
                val percentage = if (totalMonthRevenue > 0) (weekAmount / totalMonthRevenue * 100).toInt() else 0

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Dimens.SpacingXs)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = weekLabel,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = TextPrimaryLight
                        )
                        val digits = MoneyFormat.minorUnitDigits(currency)
                        val amountStr = MoneyFormat.toPlainDecimal(MoneyFormat.toMinorUnits(weekAmount, digits), digits)
                        Text(
                            text = "$amountStr $currency ($percentage%)",
                            style = MaterialTheme.typography.bodyMedium.asMoneyTabular().copy(
                                fontWeight = FontWeight.Bold,
                                color = if (weekAmount > 0) PrimaryIndigo else TextSecondaryLight
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(Dimens.SpacingXs))

                    LinearProgressIndicator(
                        progress = { (percentage / 100f).coerceIn(0f, 1f) },
                        color = PrimaryIndigo,
                        trackColor = BgLightGrey,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(Dimens.ProgressBarHeight)
                            .clip(RoundedCornerShape(Dimens.RadiusXs))
                    )
                }
            }
        }
    }
}

@Composable
fun TopRevenueDaysCard(
    dailyDataList: List<DaySalesData>,
    currency: String,
    modifier: Modifier = Modifier
) {
    val topDays = remember(dailyDataList) {
        dailyDataList.filter { it.totalRevenue > 0 }.sortedByDescending { it.totalRevenue }.take(5)
    }

    if (topDays.isEmpty()) return

    Surface(
        shape = RoundedCornerShape(Dimens.CardCornerRadius),
        color = Color.Transparent,
        modifier = modifier
            .fillMaxWidth()
            .cardRise(6)
            .depthCard(shape = RoundedCornerShape(Dimens.CardCornerRadius))
    ) {
        Column(modifier = Modifier.padding(Dimens.CardPadding)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Leaderboard,
                    contentDescription = null,
                    tint = AccentGold,
                    modifier = Modifier.size(Dimens.IconAction)
                )
                Spacer(modifier = Modifier.width(Dimens.SpacingSm))
                Text(
                    text = stringResource(R.string.top_revenue_days),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(Dimens.SpacingMd))

            topDays.forEachIndexed { rank, dayData ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Dimens.SpacingXs),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = when (rank) {
                                0 -> WarningContainer
                                1 -> SurfaceVariantLight
                                2 -> WarningContainer
                                else -> BgLightGrey
                            },
                            modifier = Modifier.size(Dimens.RankBadgeSize)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "#${rank + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = when (rank) {
                                        0 -> WarningOrange
                                        1 -> OnSurfaceVariantLight
                                        2 -> PosDueOrange
                                        else -> TextSecondaryLight
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(Dimens.SpacingSm))

                        Column {
                            Text(
                                text = "${dayData.dateString} (${dayData.dayName})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimaryLight
                            )
                            Text(
                                text = stringResource(R.string.cash_0f_mada_0f).format(dayData.cashAmount, dayData.madaAmount),
                                style = MaterialTheme.typography.bodySmall.asMoneyTabular(),
                                color = TextSecondaryLight
                            )
                        }
                    }

                    val dayDigits = MoneyFormat.minorUnitDigits(currency)
                    Text(
                        text = "${MoneyFormat.toPlainDecimal(MoneyFormat.toMinorUnits(dayData.totalRevenue, dayDigits), dayDigits)} $currency",
                        style = MaterialTheme.typography.bodyLarge.asMoneyTabular(),
                        fontWeight = FontWeight.Bold,
                        color = AccentEmerald
                    )
                }
                if (rank < topDays.size - 1) {
                    HorizontalDivider(color = OutlineLight.copy(alpha = 0.5f), thickness = Dimens.RowDividerWidth)
                }
            }
        }
    }
}
