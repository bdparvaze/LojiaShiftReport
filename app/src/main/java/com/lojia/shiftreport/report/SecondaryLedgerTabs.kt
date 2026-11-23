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
import com.lojia.shiftreport.util.MoneyFormat
import java.text.SimpleDateFormat
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
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
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
                            imageVector = Icons.Outlined.ShoppingBag,
                            contentDescription = null,
                            tint = PrimaryBlueLight,
                            modifier = Modifier.size(Dimens.IconLg)
                        )
                    }
                    Spacer(Modifier.height(Dimens.SpacingLg))
                    Text(
                        text = stringResource(R.string.paid_out_empty_title),
                        fontSize = DepthTypeTokens.EmptyStateTitleSize,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimaryReport
                    )
                    Spacer(Modifier.height(Dimens.SpacingXs))
                    Text(
                        text = stringResource(R.string.paid_out_empty_desc),
                        fontSize = DepthTypeTokens.ListRowSubtitleSize,
                        color = OnSurfaceVariantLight,
                        textAlign = TextAlign.Center,
                        maxLines = 3
                    )
                }
            }
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
                            // Left — SH1 raised item icon tile
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
                                    imageVector = Icons.Outlined.LocalMall,
                                    contentDescription = null,
                                    tint = ErrorRedLight,
                                    modifier = Modifier.size(Dimens.IconField)
                                )
                            }
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
                                        text = dateFormat.format(Date(item.dateInMillis)),
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
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val totalWalkout = list.sumOf { it.amountMinor }
    val count = list.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Section 1 — KPI Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .cardRise(0)
                .depthSemanticCard(
                    backgroundColor = ErrorContainer,
                    borderColor = ErrorRedLight.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp)
                ),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon circle
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(ErrorRedLight.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PersonOff,
                        contentDescription = null,
                        tint = ErrorRedLight,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.total_walkout_loss),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextHintColor,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = MoneyFormat.formatMinor(totalWalkout, currency),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = ErrorRedLight
                    )
                }

                // Count badge on the right
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = SurfaceLight.copy(alpha = 0.7f),
                    border = BorderStroke(0.5.dp, ErrorRedLight.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = if (count == 1) stringResource(R.string.walkout_bill_singular_fmt, count) else stringResource(R.string.walkout_bills_plural_fmt, count),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = ErrorRedLight,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Section 2 — Section Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 3.dp, height = 14.dp)
                    .background(ErrorRedLight, RoundedCornerShape(2.dp))
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.walkout_bills_section_header),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryIndigoDark,
                letterSpacing = 0.3.sp
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "($count)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TextHintColor
            )
            Spacer(Modifier.weight(1f))
        }

        // Section 3 & 4 — Entries or Empty State
        if (list.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp, horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(SurfaceVariantLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PersonOff,
                        contentDescription = null,
                        tint = TextHintColor,
                        modifier = Modifier.size(40.dp)
                    )
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    text = stringResource(R.string.no_walkout_records),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = OnSurfaceLight
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.walkout_empty_desc),
                    fontSize = 12.sp,
                    color = OnSurfaceVariantLight,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    maxLines = 3
                )
            }
        } else {
            list.forEachIndexed { idx, item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .cardRise(idx + 1)
                        .depthCard(shape = RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left — warning icon
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(ErrorContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ReceiptLong,
                                contentDescription = null,
                                tint = ErrorRedLight,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))

                        // Middle — record details
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.description,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = OnSurfaceLight,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(4.dp))

                            // Cashier + Date row (inline, bullet-separated)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Person,
                                    contentDescription = null,
                                    tint = TextHintColor,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    text = item.cashierName.replace("(", "").replace(")", ""),
                                    fontSize = 11.sp,
                                    color = TextHintColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "•",
                                    fontSize = 11.sp,
                                    color = TextHintColor
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = dateFormat.format(Date(item.dateInMillis)),
                                    fontSize = 11.sp,
                                    color = TextHintColor,
                                    maxLines = 1
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))

                        // Right — amount
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = MoneyFormat.formatMinor(item.amountMinor, currency),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = ErrorRedLight
                            )
                        }
                    }
                }
            }
        }
    }
}

