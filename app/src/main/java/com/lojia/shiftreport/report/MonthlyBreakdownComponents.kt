package com.lojia.shiftreport.report

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojia.shiftreport.R
import com.lojia.shiftreport.ui.theme.*

@Composable
fun WeeklyBreakdownCard(
    weeklyBreakdowns: List<Pair<String, Double>>,
    totalMonthRevenue: Double,
    currency: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = PureWhite,
        border = BorderStroke(1.dp, OutlineLight),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
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

            Spacer(modifier = Modifier.height(14.dp))

            weeklyBreakdowns.forEach { (weekLabel, weekAmount) ->
                val percentage = if (totalMonthRevenue > 0) (weekAmount / totalMonthRevenue * 100).toInt() else 0

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
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
                        Text(
                            text = stringResource(R.string.msg_2f_s_d_1).format(weekAmount, currency, percentage),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (weekAmount > 0) PrimaryIndigo else TextSecondaryLight
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { (percentage / 100f).coerceIn(0f, 1f) },
                        color = PrimaryIndigo,
                        trackColor = BgLightGrey,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(4.dp))
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
        shape = RoundedCornerShape(16.dp),
        color = PureWhite,
        border = BorderStroke(1.dp, OutlineLight),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Leaderboard,
                    contentDescription = null,
                    tint = AccentGold,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.top_revenue_days),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            topDays.forEachIndexed { rank, dayData ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
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
                            modifier = Modifier.size(26.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "#${rank + 1}",
                                    fontSize = 11.sp,
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

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = "${dayData.dateString} (${dayData.dayName})",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.5.sp,
                                color = TextPrimaryLight
                            )
                            Text(
                                text = stringResource(R.string.cash_0f_mada_0f).format(dayData.cashAmount, dayData.madaAmount),
                                fontSize = 11.sp,
                                color = TextSecondaryLight
                            )
                        }
                    }

                    Text(
                        text = stringResource(R.string.msg_2f_s_21).format(dayData.totalRevenue, currency),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = AccentEmerald
                    )
                }
                if (rank < topDays.size - 1) {
                    HorizontalDivider(color = OutlineLight.copy(alpha = 0.5f), thickness = 0.5.dp)
                }
            }
        }
    }
}
