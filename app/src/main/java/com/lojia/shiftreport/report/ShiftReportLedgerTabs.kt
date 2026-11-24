package com.lojia.shiftreport.report

import com.lojia.shiftreport.ui.common.LojiaTextField
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import com.lojia.shiftreport.R
import com.lojia.shiftreport.auth.AdminAuthDialog
import com.lojia.shiftreport.data.AppLanguage
import com.lojia.shiftreport.data.BusinessProfile
import com.lojia.shiftreport.data.Employee
import com.lojia.shiftreport.data.ShiftReport
import com.lojia.shiftreport.ui.common.EmployerAdvanceStatementPreviewDialog
import com.lojia.shiftreport.ui.common.LojiaDialog
import com.lojia.shiftreport.ui.theme.*
import com.lojia.shiftreport.util.DateTimeFormatUtils
import com.lojia.shiftreport.util.MoneyFormat
import com.lojia.shiftreport.util.isUnspecifiedReceipt
import com.lojia.shiftreport.util.normalizeReceiptNumber
import com.lojia.shiftreport.util.summarizeDueMinor
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

/* ----------------------------------------------------------------------
 * DATA PARSERS & DEDICATED LEDGER TABS
 * ---------------------------------------------------------------------- */
data class DueDetailEntry(
    val receiptNo: String,
    val amountMinor: Long,
    val dateInMillis: Long,
    val cashierName: String,
    val shift: String,
    val paymentMode: String = "CASH",
    val isCollection: Boolean = false
)

fun parseDueSales(reports: List<ShiftReport>): List<DueDetailEntry> {
    val list = mutableListOf<DueDetailEntry>()
    reports.forEach { report ->
        try {
            val array = org.json.JSONArray(report.dueCreditEntriesJson)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val rawRef = obj.optString("receiptNo").ifBlank { obj.optString("customerName") }
                val cleanRef = normalizeReceiptNumber(rawRef)
                val amt = obj.optLong("amount", 0L)
                if (amt > 0L) {
                    list.add(
                        DueDetailEntry(
                            receiptNo = cleanRef,
                            amountMinor = amt,
                            dateInMillis = report.dateInMillis,
                            cashierName = report.cashierName,
                            shift = report.shift,
                            isCollection = false
                        )
                    )
                }
            }
        } catch (_: Exception) {}
    }
    return list
}

fun parseDueCollections(reports: List<ShiftReport>): List<DueDetailEntry> {
    val list = mutableListOf<DueDetailEntry>()
    reports.forEach { report ->
        try {
            val array = org.json.JSONArray(report.previousDueCollectionsJson)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val rawRef = obj.optString("receiptNo").ifBlank { obj.optString("customerName") }
                val cleanRef = normalizeReceiptNumber(rawRef)
                val amt = obj.optLong("amount", 0L)
                val mode = obj.optString("paymentMode", "CASH")
                if (amt > 0L) {
                    list.add(
                        DueDetailEntry(
                            receiptNo = cleanRef,
                            amountMinor = amt,
                            dateInMillis = report.dateInMillis,
                            cashierName = report.cashierName,
                            shift = report.shift,
                            paymentMode = mode,
                            isCollection = true
                        )
                    )
                }
            }
        } catch (_: Exception) {}
    }
    return list
}

data class DueReceiptSummary(
    val receiptNo: String,
    val initialDueMinor: Long,
    val totalCollectedMinor: Long
) {
    val remainingDueMinor: Long get() = initialDueMinor - totalCollectedMinor
}

/* ----------------------------------------------------------------------
 * TAB 1: DUE HISTORY LEDGER (Credit Sales)
 * ---------------------------------------------------------------------- */
@Composable
fun DueLedgerTab(
    reports: List<ShiftReport>,
    currency: String = MoneyFormat.DEFAULT_CURRENCY_CODE
) {
    var searchQuery by remember { mutableStateOf("") }

    val salesList = remember(reports) { parseDueSales(reports) }
    val collectionsList = remember(reports) { parseDueCollections(reports) }

    val receiptMap = remember(salesList, collectionsList) {
        val map = mutableMapOf<String, Pair<Long, Long>>()
        salesList.forEach { s ->
            val curr = map[s.receiptNo] ?: Pair(0L, 0L)
            map[s.receiptNo] = Pair(curr.first + s.amountMinor, curr.second)
        }
        collectionsList.forEach { c ->
            val curr = map[c.receiptNo] ?: Pair(0L, 0L)
            map[c.receiptNo] = Pair(curr.first, curr.second + c.amountMinor)
        }
        map.map { (ref, pair) -> DueReceiptSummary(ref, pair.first, pair.second) }
            .sortedByDescending { it.remainingDueMinor }
    }

    val totalIssuedDue = salesList.sumOf { it.amountMinor }
    val totalCollectedDue = collectionsList.sumOf { it.amountMinor }
    val dueSummary = remember(receiptMap) {
        summarizeDueMinor(receiptMap.map { it.remainingDueMinor })
    }
    val netOutstandingDue = dueSummary.outstandingMinor
    val extraCollectedDue = dueSummary.extraCollectedMinor

    val filteredReceipts = remember(receiptMap, searchQuery) {
        if (searchQuery.isBlank()) receiptMap
        else receiptMap.filter { it.receiptNo.contains(searchQuery, ignoreCase = true) }
    }
    val count = filteredReceipts.size
    val depthStyle = LocalDepthStyle.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(depthStyle.reportPageBackground)
            .padding(Dimens.SpacingLg)
            .verticalScroll(rememberScrollState())
    ) {
        // Section 1 — Search Bar
        LojiaTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            textStyle = LocalTextStyle.current.copy(
                fontSize = DepthTypeTokens.ReportFieldTextSize,
                fontWeight = FontWeight.Medium,
                color = depthStyle.reportFieldTextColor
            ),
            placeholder = {
                Text(
                    text = stringResource(R.string.search_receipt_number),
                    fontSize = DepthTypeTokens.ReportFieldTextSize,
                    color = depthStyle.reportFieldPlaceholderColor
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = OnSurfaceVariantLight,
                    modifier = Modifier.size(Dimens.IconField)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.btn_clear),
                            tint = OnSurfaceVariantLight,
                            modifier = Modifier.size(Dimens.IconSm)
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Dimens.TextFieldHeightReport),
            shape = RoundedCornerShape(Dimens.RadiusReportInput),
            minHeight = Dimens.TextFieldHeightReport,
            borderColorOverride = depthStyle.reportFieldBorderColor,
            topShadowAlphaOverride = depthStyle.reportFieldInsetAlpha,
            placeholderColor = depthStyle.reportFieldPlaceholderColor,
            placeholderFontSize = DepthTypeTokens.ReportFieldTextSize,
            singleLine = true
        )

        Spacer(Modifier.height(Dimens.SpacingMd))

        // Section 2 — KPI Row (2 cards, side by side)
        Row(
            horizontalArrangement = Arrangement.spacedBy(Dimens.ReportGridGap),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Card 1 — Total Issued Due
            Card(
                modifier = Modifier
                    .weight(1f)
                    .cardRise(0)
                    .depthGradientCard(
                        gradientColors = listOf(KpiDueCardStart, KpiDueCardEnd),
                        borderColor = KpiDueCardBorder,
                        cornerRadius = Dimens.RadiusKpiCard
                    ),
                shape = RoundedCornerShape(Dimens.RadiusKpiCard),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Dimens.InsetFieldPaddingHorizontal),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(Dimens.KpiIconTileSize)
                            .depthRaisedTile(
                                gradientColors = listOf(PureWhite, ErrorContainerLight),
                                cornerRadius = Dimens.RadiusSectionIconTile
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ReceiptLong,
                            contentDescription = null,
                            tint = ErrorRedLight,
                            modifier = Modifier.size(Dimens.IconField)
                        )
                    }
                    Spacer(Modifier.width(Dimens.ReportGridGap))
                    Column {
                        Text(
                            text = stringResource(R.string.total_issued_due),
                            fontSize = DepthTypeTokens.LogoSubtitleSize,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSlate700,
                            letterSpacing = 0.4.sp,
                            maxLines = 1
                        )
                        Spacer(Modifier.height(Dimens.SpacingXxs))
                        Text(
                            text = MoneyFormat.formatMinor(totalIssuedDue, currency),
                            fontSize = DepthTypeTokens.SectionTitleSize,
                            fontWeight = FontWeight.Bold,
                            color = ErrorRedLight,
                            maxLines = 1
                        )
                    }
                }
            }

            // Card 2 — Total Collected
            Card(
                modifier = Modifier
                    .weight(1f)
                    .cardRise(1)
                    .depthGradientCard(
                        gradientColors = listOf(KpiCollectedCardStart, KpiCollectedCardEnd),
                        borderColor = KpiCollectedCardBorder,
                        cornerRadius = Dimens.RadiusKpiCard
                    ),
                shape = RoundedCornerShape(Dimens.RadiusKpiCard),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Dimens.InsetFieldPaddingHorizontal),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(Dimens.KpiIconTileSize)
                            .depthRaisedTile(
                                gradientColors = listOf(PureWhite, BadgeGreenBg),
                                cornerRadius = Dimens.RadiusSectionIconTile
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = BadgeGreenText,
                            modifier = Modifier.size(Dimens.IconField)
                        )
                    }
                    Spacer(Modifier.width(Dimens.ReportGridGap))
                    Column {
                        Text(
                            text = stringResource(R.string.total_collected),
                            fontSize = DepthTypeTokens.LogoSubtitleSize,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSlate700,
                            letterSpacing = 0.4.sp,
                            maxLines = 1
                        )
                        Spacer(Modifier.height(Dimens.SpacingXxs))
                        Text(
                            text = MoneyFormat.formatMinor(totalCollectedDue, currency),
                            fontSize = DepthTypeTokens.SectionTitleSize,
                            fontWeight = FontWeight.Bold,
                            color = BadgeGreenText,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Section 3 — Net Outstanding Highlight Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Dimens.SpacingMd)
                .cardRise(2)
                .depthGradientCard(
                    gradientColors = listOf(SectionAmberGradientStart, SectionAmberGradientEnd),
                    borderColor = SectionAmberBorder,
                    cornerRadius = Dimens.RadiusKpiCard
                ),
            shape = RoundedCornerShape(Dimens.RadiusKpiCard),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.InsetFieldPaddingHorizontal)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(Dimens.KpiIconTileSize)
                            .depthRaisedTile(
                                gradientColors = listOf(PureWhite, BadgeAmberBg),
                                cornerRadius = Dimens.RadiusSectionIconTile
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.HourglassEmpty,
                            contentDescription = null,
                            tint = BadgeAmberAccent,
                            modifier = Modifier.size(Dimens.IconField)
                        )
                    }
                    Spacer(Modifier.width(Dimens.SpacingMd))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.net_outstanding_due),
                            fontSize = DepthTypeTokens.RegisterLabelSize,
                            fontWeight = FontWeight.Bold,
                            color = BadgeAmberTextDark,
                            letterSpacing = 0.3.sp,
                            maxLines = 1
                        )
                        Spacer(Modifier.height(Dimens.SpacingXxs))
                        Text(
                            text = stringResource(R.string.deduct_auto_hint),
                            fontSize = DepthTypeTokens.LogoSubtitleSize,
                            color = BadgeAmberTextDark,
                            maxLines = 2
                        )
                    }
                    Spacer(Modifier.width(Dimens.SpacingSm))
                    // Value — never wraps
                    Text(
                        text = MoneyFormat.formatMinor(netOutstandingDue, currency),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = BadgeAmberAccent,
                        maxLines = 1,
                        softWrap = false
                    )
                }
                if (extraCollectedDue > 0L) {
                    Spacer(Modifier.height(Dimens.ReportGridGap))
                    HorizontalDivider(thickness = 0.5.dp, color = SectionAmberBorder)
                    Spacer(Modifier.height(Dimens.SpacingSm))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.due_extra_collected_label),
                            fontSize = DepthTypeTokens.RegisterLabelSize,
                            fontWeight = FontWeight.SemiBold,
                            color = BadgeGreenText,
                            maxLines = 1
                        )
                        Text(
                            text = MoneyFormat.formatMinor(extraCollectedDue, currency),
                            fontSize = DepthTypeTokens.SecondaryButtonTextSize,
                            fontWeight = FontWeight.Bold,
                            color = BadgeGreenText,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        // Section 4 — Section Header (with accent bar)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Dimens.SpacingXl, bottom = Dimens.ReportGridGap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = Dimens.LedgerSectionAccentWidth, height = Dimens.LedgerSectionAccentHeight)
                    .background(PrimaryBlueLight, RoundedCornerShape(Dimens.SpacingXxs))
            )
            Spacer(Modifier.width(Dimens.SpacingSm))
            Text(
                text = stringResource(R.string.ledger_receipt_breakdown_title),
                fontSize = DepthTypeTokens.ReportFieldTextSize,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryReport
            )
            Spacer(Modifier.width(Dimens.SpacingXs))
            Text(
                text = "($count)",
                fontSize = DepthTypeTokens.ReportFieldLabelSize,
                fontWeight = FontWeight.Medium,
                color = OnSurfaceVariantLight
            )
            Spacer(Modifier.weight(1f))
        }

        // Section 5 & 6 — Receipt Cards or Empty State
        if (filteredReceipts.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp, horizontal = Dimens.SpacingXxl),
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
                        imageVector = Icons.Outlined.ReceiptLong,
                        contentDescription = null,
                        tint = OnSurfaceVariantLight,
                        modifier = Modifier.size(40.dp)
                    )
                }
                Spacer(Modifier.height(Dimens.SpacingXl))
                Text(
                    text = stringResource(R.string.empty_due_title),
                    fontSize = DepthTypeTokens.ReportFieldTextSize,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimaryReport
                )
                Spacer(Modifier.height(Dimens.SpacingXs))
                Text(
                    text = stringResource(R.string.empty_due_hint),
                    fontSize = DepthTypeTokens.ReportFieldLabelSize,
                    color = OnSurfaceVariantLight,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            }
        } else {
            filteredReceipts.forEachIndexed { idx, item ->
                val matchedEntry = salesList.firstOrNull { it.receiptNo == item.receiptNo }
                    ?: collectionsList.firstOrNull { it.receiptNo == item.receiptNo }
                val (statusText, statusBg, statusFg) = when {
                    item.initialDueMinor > 0L && item.totalCollectedMinor >= item.initialDueMinor ->
                        Triple(stringResource(R.string.paid_in_full), BadgeGreenBg, BadgeGreenText)
                    item.totalCollectedMinor > 0L && item.remainingDueMinor > 0L ->
                        Triple(stringResource(R.string.partially_paid), BadgeAmberBg, BadgeAmberAccent)
                    else ->
                        Triple(stringResource(R.string.unpaid_due), ErrorContainerLight, ErrorRedLight)
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = Dimens.ReportGridGap)
                        .cardRise(idx + 3)
                        .depthCard(
                            shape = RoundedCornerShape(Dimens.RadiusKpiCard),
                            cornerRadius = Dimens.RadiusKpiCard
                        ),
                    shape = RoundedCornerShape(Dimens.RadiusKpiCard),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(Dimens.InsetFieldPaddingHorizontal)) {
                        // Header row: receipt # + status chip
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Receipt,
                                contentDescription = null,
                                tint = PrimaryBlueLight,
                                modifier = Modifier.size(Dimens.IconXs + Dimens.SpacingXxs)
                            )
                            Spacer(Modifier.width(Dimens.SpacingXs))
                            val normReceipt = normalizeReceiptNumber(item.receiptNo)
                            if (normReceipt.isNotEmpty()) {
                                val receiptLabel = if (isUnspecifiedReceipt(normReceipt)) {
                                    stringResource(R.string.receipt_unspecified)
                                } else {
                                    stringResource(R.string.receipt_num_tag, normReceipt)
                                }
                                Text(
                                    text = receiptLabel,
                                    fontSize = DepthTypeTokens.SecondaryButtonTextSize,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimaryReport,
                                    maxLines = 1
                                )
                            }
                            Spacer(Modifier.weight(1f))

                            // Status chip
                            Surface(
                                shape = RoundedCornerShape(Dimens.RadiusFull),
                                color = statusBg
                            ) {
                                Text(
                                    text = statusText,
                                    fontSize = DepthTypeTokens.LogoSubtitleSize,
                                    fontWeight = FontWeight.Bold,
                                    color = statusFg,
                                    modifier = Modifier.padding(horizontal = Dimens.ReportGridGap, vertical = Dimens.SpacingXs)
                                )
                            }
                        }

                        if (matchedEntry != null) {
                            Spacer(Modifier.height(Dimens.SpacingXs))
                            // Cashier + Date (small, secondary)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Person,
                                    contentDescription = null,
                                    tint = OnSurfaceVariantLight,
                                    modifier = Modifier.size(Dimens.SpacingMd)
                                )
                                Spacer(Modifier.width(Dimens.SpacingXxs))
                                Text(
                                    text = matchedEntry.cashierName.replace("(", "").replace(")", ""),
                                    fontSize = DepthTypeTokens.RegisterLabelSize,
                                    color = OnSurfaceVariantLight,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(Modifier.width(Dimens.SpacingSm))
                                Text(
                                    text = "•",
                                    fontSize = DepthTypeTokens.RegisterLabelSize,
                                    color = OnSurfaceVariantLight
                                )
                                Spacer(Modifier.width(Dimens.SpacingSm))
                                Text(
                                    text = DateTimeFormatUtils.formatDate(matchedEntry.dateInMillis),
                                    fontSize = DepthTypeTokens.RegisterLabelSize,
                                    color = OnSurfaceVariantLight,
                                    maxLines = 1
                                )
                            }
                        }

                        Spacer(Modifier.height(Dimens.ReportGridGap))
                        // Divider
                        HorizontalDivider(thickness = 0.5.dp, color = DepthCardBorder)
                        Spacer(Modifier.height(Dimens.ReportGridGap))

                        // 3-column amount breakdown
                        Row(modifier = Modifier.fillMaxWidth()) {
                            // Column 1 — Issued Due
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.issued_due),
                                    fontSize = DepthTypeTokens.LogoSubtitleSize,
                                    fontWeight = FontWeight.Medium,
                                    color = OnSurfaceVariantLight
                                )
                                Spacer(Modifier.height(Dimens.SpacingXxs))
                                Text(
                                    text = MoneyFormat.formatMinor(item.initialDueMinor, currency),
                                    fontSize = DepthTypeTokens.ReportFieldLabelSize,
                                    fontWeight = FontWeight.Bold,
                                    color = ErrorRedLight,
                                    maxLines = 1
                                )
                            }

                            // Column 2 — Collected
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = stringResource(R.string.collected_minus),
                                    fontSize = DepthTypeTokens.LogoSubtitleSize,
                                    fontWeight = FontWeight.Medium,
                                    color = OnSurfaceVariantLight
                                )
                                Spacer(Modifier.height(Dimens.SpacingXxs))
                                Text(
                                    text = MoneyFormat.formatMinor(item.totalCollectedMinor, currency),
                                    fontSize = DepthTypeTokens.ReportFieldLabelSize,
                                    fontWeight = FontWeight.Bold,
                                    color = BadgeGreenText,
                                    maxLines = 1
                                )
                            }

                            // Column 3 — Net Remaining
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = stringResource(R.string.net_remaining),
                                    fontSize = DepthTypeTokens.LogoSubtitleSize,
                                    fontWeight = FontWeight.Medium,
                                    color = OnSurfaceVariantLight
                                )
                                Spacer(Modifier.height(Dimens.SpacingXxs))
                                Text(
                                    text = MoneyFormat.formatMinor(item.remainingDueMinor, currency),
                                    fontSize = DepthTypeTokens.ReportFieldLabelSize,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryReport,
                                    maxLines = 1
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
 * TAB 2: EMPLOYER ADVANCE LEDGER (Staff Advances)
 * ---------------------------------------------------------------------- */
data class EmployerAdvanceItem(
    val staffName: String,
    val amountMinor: Long,
    val paymentMode: String,
    val dateInMillis: Long,
    val cashierName: String,
    val employeeId: String? = null
)

fun parseEmployerAdvances(reports: List<ShiftReport>): List<EmployerAdvanceItem> {
    val list = mutableListOf<EmployerAdvanceItem>()
    reports.forEach { report ->
        try {
            val array = org.json.JSONArray(report.staffAdvancesJson)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val name = obj.optString("staffName").ifBlank { obj.optString("name", "") }
                val amt = obj.optLong("amount", 0L)
                val mode = obj.optString("paymentMode", "CASH")
                val empId = obj.optString("employeeId", "").ifBlank { null }
                if (amt > 0L) {
                    list.add(
                        EmployerAdvanceItem(
                            staffName = name,
                            amountMinor = amt,
                            paymentMode = mode,
                            dateInMillis = report.dateInMillis,
                            cashierName = report.cashierName,
                            employeeId = empId
                        )
                    )
                }
            }
        } catch (_: Exception) {}
    }
    return list
}

private sealed class EmployerAdvanceHistoryTarget {
    data class EmployeeTarget(val employeeId: String) : EmployerAdvanceHistoryTarget()
    data object OtherTarget : EmployerAdvanceHistoryTarget()
}

private data class StatementPreviewPayload(
    val statement: EmployerAdvanceStatement,
    val periodLabel: String
)

@Composable
fun EmployerLedgerTab(
    reports: List<ShiftReport>,
    employees: List<Employee> = emptyList(),
    currency: String = MoneyFormat.DEFAULT_CURRENCY_CODE,
    businessProfile: BusinessProfile? = null,
    language: AppLanguage = AppLanguage.ENGLISH
) {
    val configuration = LocalConfiguration.current
    val activeLocale = remember(configuration, language) {
        Locale.forLanguageTag(language.code)
    }
    val zone = remember { ZoneId.systemDefault() }
    val nowMillis = remember { System.currentTimeMillis() }
    val periodLabel = remember(nowMillis, activeLocale, zone) {
        formatEmployerAdvancePeriodLabel(nowMillis = nowMillis, locale = activeLocale, zone = zone)
    }
    val entryDateFormatter = remember(activeLocale) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(activeLocale)
    }

    val parsedAdvances = remember(reports) { parseEmployerAdvances(reports) }
    val periodReport = remember(parsedAdvances, employees, nowMillis, zone) {
        buildEmployerAdvancePeriodReport(
            advances = parsedAdvances,
            employees = employees,
            nowMillis = nowMillis,
            zone = zone
        )
    }

    var selectedTarget by remember { mutableStateOf<EmployerAdvanceHistoryTarget?>(null) }
    var showOpenStatementAuth by remember { mutableStateOf(false) }
    var showPeriodChooser by remember { mutableStateOf(false) }
    var activeStatementPreview by remember { mutableStateOf<StatementPreviewPayload?>(null) }

    val selectedProfile = remember(selectedTarget, periodReport) {
        when (val target = selectedTarget) {
            is EmployerAdvanceHistoryTarget.EmployeeTarget ->
                periodReport.profiles.firstOrNull { it.employeeId == target.employeeId }
            EmployerAdvanceHistoryTarget.OtherTarget, null -> null
        }
    }
    val selectedOther = remember(selectedTarget, periodReport) {
        if (selectedTarget == EmployerAdvanceHistoryTarget.OtherTarget) {
            periodReport.otherBucket
        } else {
            null
        }
    }

    LaunchedEffect(selectedTarget, selectedProfile, selectedOther) {
        if (selectedTarget != null && selectedProfile == null && selectedOther == null) {
            selectedTarget = null
        }
    }

    if (selectedProfile != null) {
        BackHandler { selectedTarget = null }
        EmployerEmployeeHistoryView(
            profile = selectedProfile,
            periodLabel = periodLabel,
            currency = currency,
            locale = activeLocale,
            zone = zone,
            dateFormatter = entryDateFormatter,
            onBack = { selectedTarget = null }
        )
    } else if (selectedOther != null) {
        BackHandler { selectedTarget = null }
        EmployerOtherHistoryView(
            bucket = selectedOther,
            periodLabel = periodLabel,
            currency = currency,
            locale = activeLocale,
            zone = zone,
            dateFormatter = entryDateFormatter,
            onBack = { selectedTarget = null }
        )
    } else {
        EmployerAdvanceSummaryAndProfilesView(
            report = periodReport,
            periodLabel = periodLabel,
            currency = currency,
            locale = activeLocale,
            onOpenStatement = { showOpenStatementAuth = true },
            onSelectEmployee = { employeeId ->
                selectedTarget = EmployerAdvanceHistoryTarget.EmployeeTarget(employeeId)
            },
            onSelectOther = {
                selectedTarget = EmployerAdvanceHistoryTarget.OtherTarget
            }
        )
    }

    if (showOpenStatementAuth) {
        AdminAuthDialog(
            onDismissRequest = { showOpenStatementAuth = false },
            onAuthSuccess = {
                showOpenStatementAuth = false
                showPeriodChooser = true
            }
        )
    }

    if (showPeriodChooser) {
        AdvanceStatementPeriodDialog(
            locale = activeLocale,
            zone = zone,
            dateFormatter = entryDateFormatter,
            onDismiss = { showPeriodChooser = false },
            onConfirm = { chosenRange, chosenLabel ->
                showPeriodChooser = false
                val statement = buildEmployerAdvanceStatement(
                    advances = parsedAdvances,
                    employees = employees,
                    range = chosenRange,
                    zone = zone
                )
                activeStatementPreview = StatementPreviewPayload(
                    statement = statement,
                    periodLabel = chosenLabel
                )
            }
        )
    }

    activeStatementPreview?.let { payload ->
        EmployerAdvanceStatementPreviewDialog(
            statement = payload.statement,
            businessProfile = businessProfile,
            periodLabel = payload.periodLabel,
            language = language,
            onDismiss = { activeStatementPreview = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdvanceStatementPeriodDialog(
    locale: Locale,
    zone: ZoneId,
    dateFormatter: DateTimeFormatter,
    onDismiss: () -> Unit,
    onConfirm: (LongRange?, String) -> Unit
) {
    val context = LocalContext.current
    var selectedOption by remember { mutableStateOf(AdvanceStatementPeriodOption.THIS_MONTH) }
    var customStartDate by remember { mutableStateOf<LocalDate?>(null) }
    var customEndDate by remember { mutableStateOf<LocalDate?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDateRangePicker by remember { mutableStateOf(false) }

    val customRangeText = remember(customStartDate, customEndDate, dateFormatter) {
        val s = customStartDate
        val e = customEndDate
        if (s != null && e != null) {
            context.getString(
                R.string.statement_date_range_fmt,
                s.format(dateFormatter),
                e.format(dateFormatter)
            )
        } else {
            null
        }
    }

    val options = listOf(
        AdvanceStatementPeriodOption.THIS_MONTH to stringResource(R.string.statement_period_this_month),
        AdvanceStatementPeriodOption.LAST_MONTH to stringResource(R.string.statement_period_last_month),
        AdvanceStatementPeriodOption.ALL_TIME to stringResource(R.string.statement_period_all_time),
        AdvanceStatementPeriodOption.CUSTOM_RANGE to stringResource(R.string.statement_period_custom_range)
    )

    LojiaDialog(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.statement_select_period_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryReport,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = Dimens.DialogPaddingComfortable,
                        top = Dimens.DialogPaddingComfortable,
                        end = Dimens.DialogPaddingComfortable,
                        bottom = Dimens.SpacingSm
                    )
            )
            HorizontalDivider(color = ListDividerGray, thickness = Dimens.RowDividerWidth)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(Dimens.DialogPaddingComfortable),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .depthInset(
                            shape = RoundedCornerShape(Dimens.RadiusSegmentTrack),
                            cornerRadius = Dimens.RadiusSegmentTrack,
                            gradientColors = listOf(SegmentTrackBg, SegmentTrackBg),
                            borderColorOverride = DepthCardBorder,
                            topShadowAlphaOverride = DepthTokens.SEGMENT_TRACK_INSET_ALPHA
                        )
                        .padding(Dimens.SpacingXs)
                        .selectableGroup(),
                    verticalArrangement = Arrangement.spacedBy(Dimens.SpacingXs)
                ) {
                    options.forEach { (option, label) ->
                        val isSelected = selectedOption == option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = Dimens.SegmentThumbHeight.coerceAtLeast(Dimens.MinTouchTarget))
                                .then(
                                    if (isSelected) {
                                        Modifier.depthSegmentActiveThumb(cornerRadius = Dimens.RadiusSegmentThumb)
                                    } else {
                                        Modifier.clip(RoundedCornerShape(Dimens.RadiusSegmentThumb))
                                    }
                                )
                                .selectable(
                                    selected = isSelected,
                                    onClick = {
                                        selectedOption = option
                                        errorMessage = null
                                    },
                                    role = Role.RadioButton
                                )
                                .padding(horizontal = Dimens.SpacingSm, vertical = Dimens.SpacingXs),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Dimens.ReportGridGap)
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = PureWhite,
                                    unselectedColor = TextSlate700
                                )
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (isSelected) PureWhite else TextSlate700
                            )
                        }
                    }
                }

                if (selectedOption == AdvanceStatementPeriodOption.CUSTOM_RANGE) {
                    OutlinedButton(
                        onClick = { showDateRangePicker = true },
                        shape = RoundedCornerShape(Dimens.RadiusReportInput),
                        border = null,
                        elevation = null,
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = PrimaryBlueLight
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = Dimens.MinTouchTarget)
                            .depthSecondaryButton(
                                borderColor = PrimaryBlueLight,
                                cornerRadius = Dimens.RadiusReportInput
                            )
                            .testTag("statement_pick_range_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarMonth,
                            contentDescription = null,
                            tint = PrimaryBlueLight,
                            modifier = Modifier.size(Dimens.IconSm)
                        )
                        Spacer(modifier = Modifier.width(Dimens.SpacingSm))
                        Text(
                            text = customRangeText ?: stringResource(R.string.statement_pick_date_range),
                            style = MaterialTheme.typography.labelLarge,
                            color = PrimaryBlueLight,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (customRangeText == null) {
                        Text(
                            text = stringResource(R.string.statement_custom_range_prompt),
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariantLight,
                            modifier = Modifier.padding(top = Dimens.SpacingXs, start = Dimens.SpacingXs)
                        )
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = ErrorRedLight,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .padding(top = Dimens.SpacingXs)
                            .testTag("statement_period_error_text")
                    )
                }
            }

            HorizontalDivider(color = ListDividerGray, thickness = Dimens.RowDividerWidth)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.DialogPaddingComfortable),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(Dimens.RadiusReportInput),
                    border = null,
                    elevation = null,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = OnSurfaceVariantLight
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Dimens.ButtonHeightReportPrimary)
                        .depthSecondaryButton(
                            cornerRadius = Dimens.RadiusReportInput,
                            minHeight = Dimens.ButtonHeightReportPrimary
                        )
                ) {
                    Text(
                        text = stringResource(R.string.cancel),
                        color = OnSurfaceVariantLight,
                        fontSize = DepthTypeTokens.PrimaryCtaSmallSize,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Button(
                    onClick = {
                        val now = System.currentTimeMillis()
                        when (selectedOption) {
                            AdvanceStatementPeriodOption.THIS_MONTH -> {
                                val range = com.lojia.shiftreport.util.currentMonthRange(now, zone)
                                val lbl = formatEmployerAdvancePeriodLabel(now, locale, zone)
                                onConfirm(range, lbl)
                            }
                            AdvanceStatementPeriodOption.LAST_MONTH -> {
                                val range = previousMonthRange(now, zone)
                                val lbl = formatEmployerAdvancePeriodLabel(range.first, locale, zone)
                                onConfirm(range, lbl)
                            }
                            AdvanceStatementPeriodOption.ALL_TIME -> {
                                onConfirm(null, context.getString(R.string.statement_period_all_time))
                            }
                            AdvanceStatementPeriodOption.CUSTOM_RANGE -> {
                                val start = customStartDate
                                val end = customEndDate
                                val range = if (start != null && end != null) {
                                    customDayRange(start, end, zone)
                                } else {
                                    null
                                }
                                if (range == null || start == null || end == null) {
                                    errorMessage = context.getString(R.string.statement_invalid_date_range_error)
                                } else {
                                    val lbl = context.getString(
                                        R.string.statement_date_range_fmt,
                                        start.format(dateFormatter),
                                        end.format(dateFormatter)
                                    )
                                    onConfirm(range, lbl)
                                }
                            }
                        }
                    },
                    elevation = null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        contentColor = PureWhite,
                        disabledContentColor = OnSurfaceVariantLight
                    ),
                    shape = RoundedCornerShape(Dimens.RadiusReportInput),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Dimens.ButtonHeightReportPrimary)
                        .depthRaisedButton(
                            cornerRadius = Dimens.RadiusReportInput,
                            minHeight = Dimens.ButtonHeightReportPrimary
                        )
                        .testTag("statement_confirm_period_btn")
                ) {
                    Text(
                        text = stringResource(R.string.statement_generate_preview_btn),
                        fontSize = DepthTypeTokens.PrimaryCtaSmallSize,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }

    if (showDateRangePicker) {
        val rangePickerState = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { showDateRangePicker = false },
            colors = DatePickerDefaults.colors(containerColor = SurfaceLight),
            confirmButton = {
                TextButton(
                    onClick = {
                        val startMillis = rangePickerState.selectedStartDateMillis
                        val endMillis = rangePickerState.selectedEndDateMillis
                        if (startMillis != null && endMillis != null) {
                            val startLocal = utcMillisToLocalDate(startMillis)
                            val endLocal = utcMillisToLocalDate(endMillis)
                            if (startLocal.isAfter(endLocal)) {
                                errorMessage = context.getString(R.string.statement_invalid_date_range_error)
                            } else {
                                customStartDate = startLocal
                                customEndDate = endLocal
                                errorMessage = null
                            }
                        }
                        showDateRangePicker = false
                    },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(
                        text = stringResource(R.string.ok),
                        color = PrimaryIndigoLight,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDateRangePicker = false },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(
                        text = stringResource(R.string.cancel),
                        color = OnSurfaceVariantLight
                    )
                }
            }
        ) {
            DateRangePicker(
                state = rangePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = SurfaceLight,
                    titleContentColor = OnSurfaceLight,
                    headlineContentColor = OnSurfaceLight,
                    weekdayContentColor = OnSurfaceVariantLight,
                    subheadContentColor = OnSurfaceLight,
                    dayContentColor = OnSurfaceLight,
                    selectedDayContainerColor = PrimaryIndigoLight,
                    selectedDayContentColor = PureWhite
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(440.dp)
            )
        }
    }
}

@Composable
private fun EmployerAdvanceSummaryAndProfilesView(
    report: EmployerAdvancePeriodReport,
    periodLabel: String,
    currency: String,
    locale: Locale,
    onOpenStatement: () -> Unit,
    onSelectEmployee: (String) -> Unit,
    onSelectOther: () -> Unit
) {
    val depthStyle = LocalDepthStyle.current
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(depthStyle.reportPageBackground),
        contentPadding = PaddingValues(Dimens.SpacingLg),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMd)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.ledger_employer_advances_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryReport,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(Dimens.SpacingSm))
                OutlinedButton(
                    onClick = onOpenStatement,
                    shape = RoundedCornerShape(Dimens.RadiusReportInput),
                    border = null,
                    elevation = null,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = PrimaryBlueLight
                    ),
                    contentPadding = PaddingValues(horizontal = Dimens.SpacingMd, vertical = Dimens.SpacingSm),
                    modifier = Modifier
                        .heightIn(min = Dimens.MinTouchTarget)
                        .depthSecondaryButton(
                            borderColor = PrimaryBlueLight,
                            cornerRadius = Dimens.RadiusReportInput
                        )
                        .testTag("employer_statement_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Description,
                        contentDescription = stringResource(R.string.statement_cd_button),
                        tint = PrimaryBlueLight,
                        modifier = Modifier.size(Dimens.IconSm)
                    )
                    Spacer(modifier = Modifier.width(Dimens.SpacingXs))
                    Text(
                        text = stringResource(R.string.advance_statement_btn),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryBlueLight,
                        maxLines = 1
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .cardRise(0)
                    .depthCard(
                        accent = true,
                        shape = RoundedCornerShape(Dimens.CardCornerRadius),
                        cornerRadius = Dimens.CardCornerRadius
                    )
                    .testTag("employer_period_summary_card"),
                shape = RoundedCornerShape(Dimens.CardCornerRadius),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Dimens.ReportCardInnerPadding),
                    verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMd)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(Dimens.RadiusFull),
                            color = BadgeBlueBg
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = Dimens.SpacingMd, vertical = Dimens.SpacingXs),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingXs)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CalendarMonth,
                                    contentDescription = null,
                                    tint = BadgeBlueText,
                                    modifier = Modifier.size(Dimens.IconXs + Dimens.SpacingXxs)
                                )
                                Text(
                                    text = periodLabel,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BadgeBlueText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Text(
                            text = stringResource(R.string.advance_entries_count_fmt, report.advanceCount),
                            style = MaterialTheme.typography.labelMedium,
                            color = OnSurfaceVariantLight,
                            maxLines = 1
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpacingXxs)) {
                        Text(
                            text = stringResource(R.string.ledger_total_advances),
                            style = MaterialTheme.typography.labelMedium,
                            color = OnSurfaceVariantLight
                        )
                        Text(
                            text = MoneyFormat.formatMinor(report.totalAdvancesMinor, currency, locale),
                            fontSize = DepthTypeTokens.HeroAmountSize,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryReport,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.ReportGridGap)
                    ) {
                        EmployerModeBreakdownTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.Payments,
                            label = stringResource(R.string.cash),
                            amountText = MoneyFormat.formatMinor(report.cashTotalMinor, currency, locale),
                            containerColor = CashWellBg,
                            accentColor = BadgeBlueText
                        )
                        EmployerModeBreakdownTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.AccountBalance,
                            label = stringResource(R.string.bank),
                            amountText = MoneyFormat.formatMinor(report.bankTotalMinor, currency, locale),
                            containerColor = BankWellBg,
                            accentColor = BankWellText
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Dimens.SpacingXs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)
            ) {
                Box(
                    modifier = Modifier
                        .size(width = Dimens.LedgerSectionAccentWidth, height = Dimens.LedgerSectionAccentHeight)
                        .background(PrimaryBlueLight, RoundedCornerShape(Dimens.SpacingXxs))
                )
                Text(
                    text = stringResource(R.string.advance_period_profiles_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryReport,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (report.profiles.isEmpty() && report.otherBucket == null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .cardRise(1)
                        .depthCard(
                            shape = RoundedCornerShape(Dimens.RadiusKpiCard),
                            cornerRadius = Dimens.RadiusKpiCard
                        ),
                    shape = RoundedCornerShape(Dimens.RadiusKpiCard),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 36.dp, horizontal = Dimens.SpacingXxl),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(SurfaceVariantLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AccountBalanceWallet,
                                contentDescription = null,
                                tint = OnSurfaceVariantLight,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                        Spacer(Modifier.height(Dimens.SpacingXs))
                        Text(
                            text = stringResource(R.string.empty_advance_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimaryReport,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = stringResource(R.string.advance_profiles_empty_period),
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceVariantLight,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            itemsIndexed(report.profiles, key = { _, it -> it.employeeId }) { idx, profile ->
                EmployerEmployeeProfileCard(
                    profile = profile,
                    currency = currency,
                    locale = locale,
                    modifier = Modifier.cardRise(idx + 1),
                    onClick = { onSelectEmployee(profile.employeeId) }
                )
            }

            report.otherBucket?.let { otherBucket ->
                item(key = "other_bucket") {
                    EmployerOtherBucketCard(
                        bucket = otherBucket,
                        currency = currency,
                        locale = locale,
                        modifier = Modifier.cardRise(report.profiles.size + 1),
                        onClick = onSelectOther
                    )
                }
            }
        }
    }
}

@Composable
private fun EmployerModeBreakdownTile(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    amountText: String,
    containerColor: Color,
    accentColor: Color
) {
    val style = LocalDepthStyle.current
    Box(
        modifier = modifier
            .depthInset(
                shape = RoundedCornerShape(Dimens.RadiusReportInput),
                cornerRadius = Dimens.RadiusReportInput,
                gradientColors = listOf(containerColor, containerColor),
                borderColorOverride = accentColor.copy(alpha = 0.25f),
                topShadowAlphaOverride = style.reportFieldInsetAlpha
            )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = Dimens.SpacingMd, vertical = Dimens.ReportGridGap),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpacingXs)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingXs)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(Dimens.IconXs)
                )
                Text(
                    text = label,
                    fontSize = DepthTypeTokens.RegisterLabelSize,
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = amountText,
                fontSize = DepthTypeTokens.ReportFieldTextSize,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryReport,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EmployerEmployeeProfileCard(
    profile: EmployerEmployeeAdvanceProfile,
    currency: String,
    locale: Locale,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val displayName = profile.employeeName.ifBlank { stringResource(R.string.staff_label) }
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .depthCard(
                shape = RoundedCornerShape(Dimens.RadiusKpiCard),
                cornerRadius = Dimens.RadiusKpiCard
            )
            .testTag("employer_profile_card_${profile.employeeId}"),
        shape = RoundedCornerShape(Dimens.RadiusKpiCard),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.InsetFieldPaddingHorizontal),
            verticalArrangement = Arrangement.spacedBy(Dimens.ReportGridGap)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingMd)
            ) {
                Box(
                    modifier = Modifier
                        .size(Dimens.AvatarBadgeSize)
                        .clip(CircleShape)
                        .background(BadgeBlueBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = displayName.firstOrNull()?.uppercase()
                            ?: stringResource(R.string.ledger_staff_initial_fallback),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BadgeBlueText
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimaryReport,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.advance_entries_count_fmt, profile.advanceCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariantLight,
                        maxLines = 1
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.advance_cd_open_history),
                    tint = OnSurfaceVariantLight
                )
            }

            HorizontalDivider(color = DepthCardBorder)

            EmployerFigureRow(
                label = stringResource(R.string.advance_monthly_salary_label),
                value = MoneyFormat.formatMinor(profile.monthlySalaryMinor, currency, locale),
                valueColor = TextPrimaryReport
            )
            EmployerFigureRow(
                label = stringResource(R.string.advance_taken_label),
                value = stringResource(
                    R.string.advance_minus_amount_fmt,
                    MoneyFormat.formatMinor(profile.advancesTotalMinor, currency, locale)
                ),
                valueColor = BadgeAmberAccent
            )

            EmployerBalanceStatusPill(
                remainingMinor = profile.remainingMinor,
                excessMinor = profile.excessMinor,
                currency = currency,
                locale = locale
            )
        }
    }
}

@Composable
private fun EmployerOtherBucketCard(
    bucket: EmployerOtherAdvanceBucket,
    currency: String,
    locale: Locale,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .depthCard(
                shape = RoundedCornerShape(Dimens.RadiusKpiCard),
                cornerRadius = Dimens.RadiusKpiCard
            )
            .testTag("employer_profile_other_card"),
        shape = RoundedCornerShape(Dimens.RadiusKpiCard),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.InsetFieldPaddingHorizontal),
            verticalArrangement = Arrangement.spacedBy(Dimens.ReportGridGap)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingMd)
            ) {
                Box(
                    modifier = Modifier
                        .size(Dimens.AvatarBadgeSize)
                        .clip(CircleShape)
                        .background(SurfaceVariantLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Group,
                        contentDescription = null,
                        tint = OnSurfaceVariantLight,
                        modifier = Modifier.size(Dimens.IconField)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.advance_other_bucket_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimaryReport,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.advance_other_bucket_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariantLight,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.advance_cd_open_history),
                    tint = OnSurfaceVariantLight
                )
            }

            HorizontalDivider(color = DepthCardBorder)

            EmployerFigureRow(
                label = stringResource(R.string.advance_taken_label),
                value = stringResource(
                    R.string.advance_minus_amount_fmt,
                    MoneyFormat.formatMinor(bucket.advancesTotalMinor, currency, locale)
                ),
                valueColor = BadgeAmberAccent
            )
            Text(
                text = stringResource(R.string.advance_entries_count_fmt, bucket.advanceCount),
                style = MaterialTheme.typography.labelMedium,
                color = OnSurfaceVariantLight
            )
        }
    }
}

@Composable
private fun EmployerFigureRow(
    label: String,
    value: String,
    valueColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceVariantLight,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = valueColor,
            maxLines = 1
        )
    }
}

@Composable
private fun EmployerBalanceStatusPill(
    remainingMinor: Long,
    excessMinor: Long,
    currency: String,
    locale: Locale
) {
    val isExcess = excessMinor > 0L
    val containerColor = if (isExcess) ErrorContainerLight else BadgeGreenBg
    val contentColor = if (isExcess) ErrorRedLight else BadgeGreenText
    val statusIcon = if (isExcess) Icons.Outlined.WarningAmber else Icons.Outlined.CheckCircle
    val statusText = if (isExcess) {
        stringResource(
            R.string.advance_excess_amount_fmt,
            MoneyFormat.formatMinor(excessMinor, currency, locale)
        )
    } else {
        stringResource(
            R.string.advance_remaining_amount_fmt,
            MoneyFormat.formatMinor(remainingMinor, currency, locale)
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.RadiusMd),
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Dimens.SpacingMd, vertical = Dimens.SpacingSm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)
        ) {
            Icon(
                imageVector = statusIcon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(Dimens.IconSm)
            )
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EmployerEmployeeHistoryView(
    profile: EmployerEmployeeAdvanceProfile,
    periodLabel: String,
    currency: String,
    locale: Locale,
    zone: ZoneId,
    dateFormatter: DateTimeFormatter,
    onBack: () -> Unit
) {
    val displayName = profile.employeeName.ifBlank { stringResource(R.string.staff_label) }
    val depthStyle = LocalDepthStyle.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(depthStyle.reportPageBackground),
        contentPadding = PaddingValues(Dimens.SpacingLg),
        verticalArrangement = Arrangement.spacedBy(Dimens.ReportGridGap)
    ) {
        item {
            EmployerHistoryBackRow(onBack = onBack)
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .cardRise(0)
                    .depthCard(
                        accent = true,
                        shape = RoundedCornerShape(Dimens.CardCornerRadius),
                        cornerRadius = Dimens.CardCornerRadius
                    )
                    .testTag("employer_history_header_card"),
                shape = RoundedCornerShape(Dimens.CardCornerRadius),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Dimens.ReportCardInnerPadding),
                    verticalArrangement = Arrangement.spacedBy(Dimens.ReportGridGap)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryReport,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = stringResource(R.string.advance_history_label),
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceVariantLight
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(Dimens.RadiusFull),
                            color = BadgeBlueBg
                        ) {
                            Text(
                                text = periodLabel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = BadgeBlueText,
                                modifier = Modifier.padding(horizontal = Dimens.ReportGridGap, vertical = Dimens.SpacingXs),
                                maxLines = 1
                            )
                        }
                    }

                    HorizontalDivider(color = DepthCardBorder)

                    EmployerFigureRow(
                        label = stringResource(R.string.advance_monthly_salary_label),
                        value = MoneyFormat.formatMinor(profile.monthlySalaryMinor, currency, locale),
                        valueColor = TextPrimaryReport
                    )
                    EmployerFigureRow(
                        label = stringResource(R.string.advance_taken_label),
                        value = stringResource(
                            R.string.advance_minus_amount_fmt,
                            MoneyFormat.formatMinor(profile.advancesTotalMinor, currency, locale)
                        ),
                        valueColor = BadgeAmberAccent
                    )

                    EmployerBalanceStatusPill(
                        remainingMinor = profile.remainingMinor,
                        excessMinor = profile.excessMinor,
                        currency = currency,
                        locale = locale
                    )
                }
            }
        }

        itemsIndexed(
            items = profile.history,
            key = { _, it -> "${it.serialNumber}_${it.dateInMillis}_${it.amountMinor}" }
        ) { idx, entry ->
            EmployerAdvanceHistoryRowCard(
                entry = entry,
                showStaffName = false,
                currency = currency,
                locale = locale,
                zone = zone,
                dateFormatter = dateFormatter,
                modifier = Modifier.cardRise(idx + 1)
            )
        }
    }
}

@Composable
private fun EmployerOtherHistoryView(
    bucket: EmployerOtherAdvanceBucket,
    periodLabel: String,
    currency: String,
    locale: Locale,
    zone: ZoneId,
    dateFormatter: DateTimeFormatter,
    onBack: () -> Unit
) {
    val depthStyle = LocalDepthStyle.current
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(depthStyle.reportPageBackground),
        contentPadding = PaddingValues(Dimens.SpacingLg),
        verticalArrangement = Arrangement.spacedBy(Dimens.ReportGridGap)
    ) {
        item {
            EmployerHistoryBackRow(onBack = onBack)
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .cardRise(0)
                    .depthCard(
                        accent = true,
                        shape = RoundedCornerShape(Dimens.CardCornerRadius),
                        cornerRadius = Dimens.CardCornerRadius
                    )
                    .testTag("employer_other_history_header_card"),
                shape = RoundedCornerShape(Dimens.CardCornerRadius),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Dimens.ReportCardInnerPadding),
                    verticalArrangement = Arrangement.spacedBy(Dimens.ReportGridGap)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.advance_other_bucket_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryReport
                            )
                            Text(
                                text = stringResource(R.string.advance_other_bucket_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceVariantLight
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(Dimens.RadiusFull),
                            color = BadgeBlueBg
                        ) {
                            Text(
                                text = periodLabel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = BadgeBlueText,
                                modifier = Modifier.padding(horizontal = Dimens.ReportGridGap, vertical = Dimens.SpacingXs),
                                maxLines = 1
                            )
                        }
                    }

                    HorizontalDivider(color = DepthCardBorder)

                    EmployerFigureRow(
                        label = stringResource(R.string.advance_taken_label),
                        value = stringResource(
                            R.string.advance_minus_amount_fmt,
                            MoneyFormat.formatMinor(bucket.advancesTotalMinor, currency, locale)
                        ),
                        valueColor = BadgeAmberAccent
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.ReportGridGap)
                    ) {
                        EmployerModeBreakdownTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.Payments,
                            label = stringResource(R.string.cash),
                            amountText = MoneyFormat.formatMinor(bucket.cashTotalMinor, currency, locale),
                            containerColor = CashWellBg,
                            accentColor = BadgeBlueText
                        )
                        EmployerModeBreakdownTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.AccountBalance,
                            label = stringResource(R.string.bank),
                            amountText = MoneyFormat.formatMinor(bucket.bankTotalMinor, currency, locale),
                            containerColor = BankWellBg,
                            accentColor = BankWellText
                        )
                    }
                }
            }
        }

        itemsIndexed(
            items = bucket.history,
            key = { _, it -> "${it.serialNumber}_${it.dateInMillis}_${it.amountMinor}" }
        ) { idx, entry ->
            EmployerAdvanceHistoryRowCard(
                entry = entry,
                showStaffName = true,
                currency = currency,
                locale = locale,
                zone = zone,
                dateFormatter = dateFormatter,
                modifier = Modifier.cardRise(idx + 1)
            )
        }
    }
}

@Composable
private fun EmployerHistoryBackRow(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(
            onClick = onBack,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("employer_history_back_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.advance_cd_back_to_profiles),
                tint = PrimaryBlueLight
            )
            Spacer(Modifier.width(Dimens.SpacingXs))
            Text(
                text = stringResource(R.string.back),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = PrimaryBlueLight
            )
        }
    }
}

@Composable
private fun EmployerAdvanceHistoryRowCard(
    entry: EmployerAdvanceHistoryEntry,
    showStaffName: Boolean,
    currency: String,
    locale: Locale,
    zone: ZoneId,
    dateFormatter: DateTimeFormatter,
    modifier: Modifier = Modifier
) {
    val isCash = isCashAdvanceMode(entry.paymentMode)
    val modeColor = if (isCash) BadgeBlueText else BankWellText
    val modeContainer = if (isCash) CashWellBg else BankWellBg
    val modeIcon = if (isCash) Icons.Outlined.Payments else Icons.Outlined.AccountBalance
    val modeLabel = if (isCash) stringResource(R.string.cash) else stringResource(R.string.bank)
    val formattedDate = remember(entry.dateInMillis, zone, dateFormatter) {
        Instant.ofEpochMilli(entry.dateInMillis).atZone(zone).format(dateFormatter)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .depthCard(
                shape = RoundedCornerShape(Dimens.RadiusLg),
                cornerRadius = Dimens.RadiusLg
            )
            .testTag("employer_history_row_${entry.serialNumber}"),
        shape = RoundedCornerShape(Dimens.RadiusLg),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.SpacingMd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingMd)
        ) {
            Box(
                modifier = Modifier
                    .size(Dimens.SerialBadgeSize)
                    .clip(CircleShape)
                    .background(BadgeBlueBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.advance_serial_fmt, entry.serialNumber),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = BadgeBlueText
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpacingXs)
            ) {
                if (showStaffName) {
                    Text(
                        text = entry.staffName.ifBlank { stringResource(R.string.staff_label) },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimaryReport,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimaryReport,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Surface(
                    shape = RoundedCornerShape(Dimens.RadiusFull),
                    color = modeContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = Dimens.SpacingSm, vertical = Dimens.SpacingXs),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingXs)
                    ) {
                        Icon(
                            imageVector = modeIcon,
                            contentDescription = null,
                            tint = modeColor,
                            modifier = Modifier.size(Dimens.IconXs)
                        )
                        Text(
                            text = modeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = modeColor
                        )
                    }
                }
            }

            Text(
                text = MoneyFormat.formatMinor(entry.amountMinor, currency, locale),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryReport,
                maxLines = 1
            )
        }
    }
}

// Tabs 3 (PaidOutShoppingLedgerTab) and 4 (WalkoutLedgerTab) are extracted into SecondaryLedgerTabs.kt

