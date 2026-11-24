package com.lojia.shiftreport.report

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocalMall
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonOff
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojia.shiftreport.R
import com.lojia.shiftreport.data.ShiftReport
import com.lojia.shiftreport.ui.theme.*
import com.lojia.shiftreport.util.DateTimeFormatUtils
import com.lojia.shiftreport.util.MoneyFormat
import java.util.*

/* ----------------------------------------------------------------------
 * TAB 3: PAID OUT / SHOPPING LEDGER (Expenses & Purchases)
 * ---------------------------------------------------------------------- */
data class PaidOutShoppingItem(
    val itemName: String,
    val qty: Int,
    val unitPriceMinor: Long,
    val totalAmountMinor: Long,
    val dateInMillis: Long,
    val cashierName: String
)

fun parsePaidOutItems(reports: List<ShiftReport>): List<PaidOutShoppingItem> {
    val list = mutableListOf<PaidOutShoppingItem>()
    reports.forEach { report ->
        try {
            val array = org.json.JSONArray(report.purchasedItemsJson)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val name = obj.optString("itemName").ifBlank { obj.optString("name", "Item") }
                val qty = obj.optInt("quantity", obj.optDouble("quantity", 0.0).toInt())
                val price = obj.optLong("unitPrice", 0L)
                val total = obj.optLong("totalAmount", 0L)
                if (total > 0L || name.isNotBlank()) {
                    list.add(
                        PaidOutShoppingItem(
                            itemName = name,
                            qty = qty,
                            unitPriceMinor = price,
                            totalAmountMinor = total,
                            dateInMillis = report.dateInMillis,
                            cashierName = report.cashierName
                        )
                    )
                }
            }
        } catch (_: Exception) {}
    }
    return list
}

@Composable
fun PaidOutShoppingLedgerTab(
    reports: List<ShiftReport>,
    currency: String = MoneyFormat.DEFAULT_CURRENCY_CODE
) {
    val list = remember(reports) { parsePaidOutItems(reports) }
    val totalExpense = list.sumOf { it.totalAmountMinor }
    val depthStyle = LocalDepthStyle.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(depthStyle.reportPageBackground)
            .padding(Dimens.SpacingLg)
            .verticalScroll(rememberScrollState())
    ) {
        // Section 1 — Top KPI Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .cardRise(0)
                .depthCard(cornerRadius = Dimens.CardCornerRadius),
            shape = RoundedCornerShape(Dimens.CardCornerRadius),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = Dimens.SpacingNone)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.ReportCardInnerPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(Dimens.PaidOutIconTileSize)
                        .depthRaisedIconTile(
                            gradientColors = listOf(PureWhite, SectionRoseGradientEnd),
                            cornerRadius = Dimens.RadiusSettingsIconTile
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ShoppingCart,
                        contentDescription = null,
                        tint = ErrorRedLight,
                        modifier = Modifier.size(Dimens.IconField)
                    )
                }
                Spacer(Modifier.width(Dimens.SettingsIconTitleGap))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.paid_out_total_title),
                        fontSize = DepthTypeTokens.ListRowSubtitleSize,
                        fontWeight = FontWeight.SemiBold,
                        color = OnSurfaceVariantLight,
                        letterSpacing = DepthTypeTokens.SettingsSectionHeaderTracking
                    )
                    Spacer(Modifier.height(Dimens.SpacingXxs))
                    Text(
                        text = MoneyFormat.formatMinor(totalExpense, currency),
                        fontSize = DepthTypeTokens.TopBarTitleSize,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryReport
                    )
                }

                // Right badge — record count
                Surface(
                    shape = RoundedCornerShape(Dimens.RadiusFull),
                    color = BadgeBlueBg
                ) {
                    Text(
                        text = stringResource(R.string.paid_out_entries_count_fmt, list.size),
                        fontSize = DepthTypeTokens.ChipLabelSize,
                        fontWeight = FontWeight.SemiBold,
                        color = BadgeBlueText,
                        modifier = Modifier.padding(
                            horizontal = Dimens.ReportGridGap,
                            vertical = Dimens.SpacingXs
                        )
                    )
                }
            }
        }

        // Section 2 — Section Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Dimens.SpacingXl, bottom = Dimens.ReportGridGap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(
                        width = Dimens.LedgerSectionAccentWidth,
                        height = Dimens.LedgerSectionAccentHeight
                    )
                    .background(PrimaryBlueLight, RoundedCornerShape(Dimens.SpacingXxs))
            )
            Spacer(Modifier.width(Dimens.SpacingSm))
            Text(
                text = stringResource(R.string.paid_out_section_header),
                fontSize = DepthTypeTokens.CardSectionHeadingSize,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimaryReport
            )
            Spacer(Modifier.width(Dimens.SpacingXs))
            Text(
                text = "(${list.size})",
                fontSize = DepthTypeTokens.ListRowSubtitleSize,
                fontWeight = FontWeight.Medium,
                color = OnSurfaceVariantLight
            )
            Spacer(Modifier.weight(1f))
        }

        // Section 3 & 4 — Entries or Empty State
        if (list.isEmpty()) {
            LedgerEmptyStateCard(
                icon = Icons.Outlined.ShoppingBag,
                title = stringResource(R.string.paid_out_empty_title),
                description = stringResource(R.string.paid_out_empty_desc)
            )
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .cardRise(1)
                    .depthCard(cornerRadius = Dimens.CardCornerRadius),
                shape = RoundedCornerShape(Dimens.CardCornerRadius),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = Dimens.SpacingNone)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = Dimens.ReportCardInnerPadding,
                            vertical = Dimens.SpacingXs
                        )
                ) {
                    list.forEachIndexed { idx, item ->
                        if (idx > 0) {
                            HorizontalDivider(
                                thickness = Dimens.RowDividerWidth,
                                color = depthStyle.listDividerColor
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = Dimens.ListRowPaddingVertical),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LedgerRoseIconTile(icon = Icons.Outlined.LocalMall)
                            Spacer(Modifier.width(Dimens.SettingsIconTitleGap))

                            // Middle — item details
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.itemName,
                                    fontSize = DepthTypeTokens.ListRowTitleSize,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimaryReport,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (item.qty > 0 && item.unitPriceMinor > 0L) {
                                    Spacer(Modifier.height(Dimens.SpacingXxs))
                                    Text(
                                        text = stringResource(
                                            R.string.paid_out_qty_x_price_fmt,
                                            item.qty,
                                            MoneyFormat.formatMinor(item.unitPriceMinor, currency)
                                        ),
                                        fontSize = DepthTypeTokens.ListRowSubtitleSize,
                                        color = OnSurfaceVariantLight,
                                        maxLines = 1
                                    )
                                } else if (item.qty > 0) {
                                    Spacer(Modifier.height(Dimens.SpacingXxs))
                                    Text(
                                        text = stringResource(R.string.paid_out_qty_pcs_fmt, item.qty),
                                        fontSize = DepthTypeTokens.ListRowSubtitleSize,
                                        color = OnSurfaceVariantLight,
                                        maxLines = 1
                                    )
                                }
                                Spacer(Modifier.height(Dimens.SpacingXxs))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.Person,
                                        contentDescription = null,
                                        tint = OnSurfaceVariantLight,
                                        modifier = Modifier.size(Dimens.SpacingMd)
                                    )
                                    Spacer(Modifier.width(Dimens.SpacingXs))
                                    Text(
                                        text = item.cashierName,
                                        fontSize = DepthTypeTokens.ListRowSubtitleSize,
                                        color = OnSurfaceVariantLight,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Spacer(Modifier.width(Dimens.SpacingXs))
                                    Text(
                                        text = "•",
                                        fontSize = DepthTypeTokens.ListRowSubtitleSize,
                                        color = OnSurfaceVariantLight
                                    )
                                    Spacer(Modifier.width(Dimens.SpacingXs))
                                    Text(
                                        text = DateTimeFormatUtils.formatDate(item.dateInMillis),
                                        fontSize = DepthTypeTokens.ListRowSubtitleSize,
                                        color = OnSurfaceVariantLight,
                                        maxLines = 1
                                    )
                                }
                            }
                            Spacer(Modifier.width(Dimens.SpacingSm))

                            // Right — amount
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = MoneyFormat.formatMinor(item.totalAmountMinor, currency),
                                    fontSize = DepthTypeTokens.CardSectionHeadingSize,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryReport
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LedgerRoseIconTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Box(
        modifier = Modifier
            .size(Dimens.PaidOutIconTileSize)
            .depthRaisedIconTile(
                gradientColors = listOf(PureWhite, SectionRoseGradientEnd),
                cornerRadius = Dimens.RadiusSettingsIconTile
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = ErrorRedLight,
            modifier = Modifier.size(Dimens.IconField)
        )
    }
}

@Composable
private fun LedgerKpiSummaryCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    formattedAmount: String,
    badgeText: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .cardRise(0)
            .depthCard(cornerRadius = Dimens.CardCornerRadius),
        shape = RoundedCornerShape(Dimens.CardCornerRadius),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.SpacingNone)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.ReportCardInnerPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LedgerRoseIconTile(icon = icon)
            Spacer(Modifier.width(Dimens.SettingsIconTitleGap))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = DepthTypeTokens.ListRowSubtitleSize,
                    fontWeight = FontWeight.SemiBold,
                    color = OnSurfaceVariantLight,
                    letterSpacing = DepthTypeTokens.SettingsSectionHeaderTracking
                )
                Spacer(Modifier.height(Dimens.SpacingXxs))
                Text(
                    text = formattedAmount,
                    fontSize = DepthTypeTokens.TopBarTitleSize,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryReport
                )
            }

            Surface(
                shape = RoundedCornerShape(Dimens.RadiusFull),
                color = BadgeBlueBg
            ) {
                Text(
                    text = badgeText,
                    fontSize = DepthTypeTokens.ChipLabelSize,
                    fontWeight = FontWeight.SemiBold,
                    color = BadgeBlueText,
                    modifier = Modifier.padding(
                        horizontal = Dimens.ReportGridGap,
                        vertical = Dimens.SpacingXs
                    )
                )
            }
        }
    }
}

@Composable
private fun LedgerSectionHeaderRow(
    title: String,
    count: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Dimens.SpacingXl, bottom = Dimens.ReportGridGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(
                    width = Dimens.LedgerSectionAccentWidth,
                    height = Dimens.LedgerSectionAccentHeight
                )
                .background(PrimaryBlueLight, RoundedCornerShape(Dimens.SpacingXxs))
        )
        Spacer(Modifier.width(Dimens.SpacingSm))
        Text(
            text = title,
            fontSize = DepthTypeTokens.CardSectionHeadingSize,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimaryReport
        )
        Spacer(Modifier.width(Dimens.SpacingXs))
        Text(
            text = "($count)",
            fontSize = DepthTypeTokens.ListRowSubtitleSize,
            fontWeight = FontWeight.Medium,
            color = OnSurfaceVariantLight
        )
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun LedgerEmptyStateCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .cardRise(1)
            .depthCard(cornerRadius = Dimens.CardCornerRadius),
        shape = RoundedCornerShape(Dimens.CardCornerRadius),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.SpacingNone)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Dimens.SpacingHuge, horizontal = Dimens.SpacingXxl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(Dimens.PaidOutIconTileSize + Dimens.SpacingLg)
                    .depthRaisedIconTile(
                        gradientColors = listOf(PureWhite, BadgeBlueBg),
                        cornerRadius = Dimens.RadiusEmployeeAvatarTile
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = PrimaryBlueLight,
                    modifier = Modifier.size(Dimens.IconLg)
                )
            }
            Spacer(Modifier.height(Dimens.SpacingLg))
            Text(
                text = title,
                fontSize = DepthTypeTokens.EmptyStateTitleSize,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimaryReport
            )
            Spacer(Modifier.height(Dimens.SpacingXs))
            Text(
                text = description,
                fontSize = DepthTypeTokens.ListRowSubtitleSize,
                color = OnSurfaceVariantLight,
                textAlign = TextAlign.Center,
                maxLines = 3
            )
        }
    }
}

/* ----------------------------------------------------------------------
 * TAB 4: WALKOUT LEDGER (Unpaid Invoices / Walkouts)
 * ---------------------------------------------------------------------- */
data class WalkoutLogItem(
    val description: String,
    val amountMinor: Long,
    val dateInMillis: Long,
    val cashierName: String
)

fun parseWalkoutItems(reports: List<ShiftReport>): List<WalkoutLogItem> {
    val list = mutableListOf<WalkoutLogItem>()
    reports.forEach { report ->
        try {
            val array = org.json.JSONArray(report.unpaidBillsJson)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val desc = obj.optString("tableOrOrderRef").ifBlank { obj.optString("description", "Walkout") }
                val amt = obj.optLong("amount", 0L)
                if (amt > 0L) {
                    list.add(
                        WalkoutLogItem(
                            description = desc,
                            amountMinor = amt,
                            dateInMillis = report.dateInMillis,
                            cashierName = report.cashierName
                        )
                    )
                }
            }
        } catch (_: Exception) {}
    }
    return list
}

@Composable
fun WalkoutLedgerTab(
    reports: List<ShiftReport>,
    currency: String = MoneyFormat.DEFAULT_CURRENCY_CODE
) {
    val list = remember(reports) { parseWalkoutItems(reports) }
    val totalWalkout = list.sumOf { it.amountMinor }
    val count = list.size
    val depthStyle = LocalDepthStyle.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(depthStyle.reportPageBackground)
            .padding(Dimens.SpacingLg)
            .verticalScroll(rememberScrollState())
    ) {
        // Section 1 — KPI Card
        LedgerKpiSummaryCard(
            icon = Icons.Outlined.PersonOff,
            title = stringResource(R.string.total_walkout_loss),
            formattedAmount = MoneyFormat.formatMinor(totalWalkout, currency),
            badgeText = if (count == 1) {
                stringResource(R.string.walkout_bill_singular_fmt, count)
            } else {
                stringResource(R.string.walkout_bills_plural_fmt, count)
            }
        )

        // Section 2 — Section Header
        LedgerSectionHeaderRow(
            title = stringResource(R.string.walkout_bills_section_header),
            count = count
        )

        // Section 3 & 4 — Entries or Empty State
        if (list.isEmpty()) {
            LedgerEmptyStateCard(
                icon = Icons.Outlined.PersonOff,
                title = stringResource(R.string.no_walkout_records),
                description = stringResource(R.string.walkout_empty_desc)
            )
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .cardRise(1)
                    .depthCard(cornerRadius = Dimens.CardCornerRadius),
                shape = RoundedCornerShape(Dimens.CardCornerRadius),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = Dimens.SpacingNone)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = Dimens.ReportCardInnerPadding,
                            vertical = Dimens.SpacingXs
                        )
                ) {
                    list.forEachIndexed { idx, item ->
                        if (idx > 0) {
                            HorizontalDivider(
                                thickness = Dimens.RowDividerWidth,
                                color = depthStyle.listDividerColor
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = Dimens.ListRowPaddingVertical),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left — SH1 raised rose tile
                            LedgerRoseIconTile(icon = Icons.Outlined.ReceiptLong)
                            Spacer(Modifier.width(Dimens.SettingsIconTitleGap))

                            // Middle — record details
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.description,
                                    fontSize = DepthTypeTokens.ListRowTitleSize,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimaryReport,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(Dimens.SpacingXxs))

                                // Cashier + Date row (inline, bullet-separated)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.Person,
                                        contentDescription = null,
                                        tint = OnSurfaceVariantLight,
                                        modifier = Modifier.size(Dimens.SpacingMd)
                                    )
                                    Spacer(Modifier.width(Dimens.SpacingXs))
                                    Text(
                                        text = item.cashierName.replace("(", "").replace(")", ""),
                                        fontSize = DepthTypeTokens.ListRowSubtitleSize,
                                        color = OnSurfaceVariantLight,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Spacer(Modifier.width(Dimens.SpacingXs))
                                    Text(
                                        text = "•",
                                        fontSize = DepthTypeTokens.ListRowSubtitleSize,
                                        color = OnSurfaceVariantLight
                                    )
                                    Spacer(Modifier.width(Dimens.SpacingXs))
                                    Text(
                                        text = DateTimeFormatUtils.formatDate(item.dateInMillis),
                                        fontSize = DepthTypeTokens.ListRowSubtitleSize,
                                        color = OnSurfaceVariantLight,
                                        maxLines = 1
                                    )
                                }
                            }
                            Spacer(Modifier.width(Dimens.SpacingSm))

                            // Right — amount
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = MoneyFormat.formatMinor(item.amountMinor, currency),
                                    fontSize = DepthTypeTokens.CardSectionHeadingSize,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryReport
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
