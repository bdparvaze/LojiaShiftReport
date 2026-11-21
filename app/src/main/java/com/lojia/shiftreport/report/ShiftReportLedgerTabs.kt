package com.lojia.shiftreport.report

import com.lojia.shiftreport.ui.common.LojiaTextField
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.LocalMall
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonOff
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojia.shiftreport.R
import com.lojia.shiftreport.data.Employee
import com.lojia.shiftreport.data.ShiftReport
import com.lojia.shiftreport.ui.theme.*
import com.lojia.shiftreport.util.MoneyFormat
import com.lojia.shiftreport.util.isUnspecifiedReceipt
import com.lojia.shiftreport.util.normalizeReceiptNumber
import com.lojia.shiftreport.util.summarizeDueMinor
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.Instant
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
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Section 1 — Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text(
                    text = stringResource(R.string.search_receipt_number),
                    fontSize = 13.sp,
                    color = TextHintColor
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = OnSurfaceVariantLight,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.btn_clear),
                            tint = OnSurfaceVariantLight,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryIndigoLight,
                unfocusedBorderColor = OutlineLight,
                focusedContainerColor = SurfaceLight,
                unfocusedContainerColor = SurfaceLight,
                cursorColor = PrimaryIndigoLight
            ),
            singleLine = true
        )

        Spacer(Modifier.height(14.dp))

        // Section 2 — KPI Row (2 cards, side by side)
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Card 1 — Total Issued Due
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = ErrorContainer),
                border = BorderStroke(1.dp, ErrorRedLight.copy(alpha = 0.3f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(ErrorRedLight.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ReceiptLong,
                            contentDescription = null,
                            tint = ErrorRedLight,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.total_issued_due),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextHintColor,
                            letterSpacing = 0.4.sp,
                            maxLines = 1
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = MoneyFormat.formatMinor(totalIssuedDue, currency),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = ErrorRedLight,
                            maxLines = 1
                        )
                    }
                }
            }

            // Card 2 — Total Collected
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SuccessContainer),
                border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.3f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(SuccessGreen.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.total_collected),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextHintColor,
                            letterSpacing = 0.4.sp,
                            maxLines = 1
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = MoneyFormat.formatMinor(totalCollectedDue, currency),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = SuccessGreen,
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
                .padding(top = 12.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = WarningContainer),
            border = BorderStroke(1.dp, WarningOrange.copy(alpha = 0.35f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(WarningOrange.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.HourglassEmpty,
                            contentDescription = null,
                            tint = WarningOrange,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.net_outstanding_due),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = WarningOrange,
                            letterSpacing = 0.3.sp,
                            maxLines = 1
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.deduct_auto_hint),
                            fontSize = 10.sp,
                            color = OnSurfaceVariantLight,
                            maxLines = 2
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    // Value — never wraps
                    Text(
                        text = MoneyFormat.formatMinor(netOutstandingDue, currency),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = WarningOrange,
                        maxLines = 1,
                        softWrap = false
                    )
                }
                if (extraCollectedDue > 0L) {
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(thickness = 0.5.dp, color = WarningOrange.copy(alpha = 0.25f))
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.due_extra_collected_label),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SuccessGreen,
                            maxLines = 1
                        )
                        Text(
                            text = MoneyFormat.formatMinor(extraCollectedDue, currency),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SuccessGreen,
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
                .padding(top = 20.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 3.dp, height = 14.dp)
                    .background(PrimaryIndigoLight, RoundedCornerShape(2.dp))
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.ledger_receipt_breakdown_title),
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

        // Section 5 & 6 — Receipt Cards or Empty State
        if (filteredReceipts.isEmpty()) {
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
                        imageVector = Icons.Outlined.ReceiptLong,
                        contentDescription = null,
                        tint = TextHintColor,
                        modifier = Modifier.size(40.dp)
                    )
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    text = stringResource(R.string.empty_due_title),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = OnSurfaceLight
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.empty_due_hint),
                    fontSize = 12.sp,
                    color = OnSurfaceVariantLight,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            }
        } else {
            filteredReceipts.forEach { item ->
                val matchedEntry = salesList.firstOrNull { it.receiptNo == item.receiptNo }
                    ?: collectionsList.firstOrNull { it.receiptNo == item.receiptNo }
                val (statusText, statusBg, statusFg) = when {
                    item.initialDueMinor > 0L && item.totalCollectedMinor >= item.initialDueMinor ->
                        Triple(stringResource(R.string.paid_in_full), SuccessContainer, SuccessGreen)
                    item.totalCollectedMinor > 0L && item.remainingDueMinor > 0L ->
                        Triple(stringResource(R.string.partially_paid), WarningContainer, WarningOrange)
                    else ->
                        Triple(stringResource(R.string.unpaid_due), ErrorContainer, ErrorRedLight)
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                    border = BorderStroke(1.dp, OutlineLight),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // Header row: receipt # + status chip
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Receipt,
                                contentDescription = null,
                                tint = PrimaryIndigoLight,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            val normReceipt = normalizeReceiptNumber(item.receiptNo)
                            if (normReceipt.isNotEmpty()) {
                                val receiptLabel = if (isUnspecifiedReceipt(normReceipt)) {
                                    stringResource(R.string.receipt_unspecified)
                                } else {
                                    stringResource(R.string.receipt_num_tag, normReceipt)
                                }
                                Text(
                                    text = receiptLabel,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = OnSurfaceLight,
                                    maxLines = 1
                                )
                            }
                            Spacer(Modifier.weight(1f))

                            // Status chip
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = statusBg
                            ) {
                                Text(
                                    text = statusText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = statusFg,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        if (matchedEntry != null) {
                            Spacer(Modifier.height(6.dp))
                            // Cashier + Date (small, secondary)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Person,
                                    contentDescription = null,
                                    tint = TextHintColor,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    text = matchedEntry.cashierName.replace("(", "").replace(")", ""),
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
                                    text = dateFormat.format(Date(matchedEntry.dateInMillis)),
                                    fontSize = 11.sp,
                                    color = TextHintColor,
                                    maxLines = 1
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        // Divider
                        HorizontalDivider(thickness = 0.5.dp, color = OutlineVariantLight)
                        Spacer(Modifier.height(10.dp))

                        // 3-column amount breakdown
                        Row(modifier = Modifier.fillMaxWidth()) {
                            // Column 1 — Issued Due
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.issued_due),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextHintColor,
                                    letterSpacing = 0.3.sp
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = MoneyFormat.formatMinor(item.initialDueMinor, currency),
                                    fontSize = 13.sp,
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
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextHintColor,
                                    letterSpacing = 0.3.sp
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = MoneyFormat.formatMinor(item.totalCollectedMinor, currency),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SuccessGreen,
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
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextHintColor,
                                    letterSpacing = 0.3.sp
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = MoneyFormat.formatMinor(item.remainingDueMinor, currency),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryIndigoDark,
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

@Composable
fun EmployerLedgerTab(
    reports: List<ShiftReport>,
    employees: List<Employee> = emptyList(),
    currency: String = MoneyFormat.DEFAULT_CURRENCY_CODE
) {
    val configuration = LocalConfiguration.current
    val activeLocale = remember(configuration) {
        if (!configuration.locales.isEmpty) configuration.locales[0] else Locale.getDefault()
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
            onSelectEmployee = { employeeId ->
                selectedTarget = EmployerAdvanceHistoryTarget.EmployeeTarget(employeeId)
            },
            onSelectOther = {
                selectedTarget = EmployerAdvanceHistoryTarget.OtherTarget
            }
        )
    }
}

@Composable
private fun EmployerAdvanceSummaryAndProfilesView(
    report: EmployerAdvancePeriodReport,
    periodLabel: String,
    currency: String,
    locale: Locale,
    onSelectEmployee: (String) -> Unit,
    onSelectOther: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("employer_period_summary_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                border = BorderStroke(1.dp, OutlineLight),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = PrimaryContainerLight
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CalendarMonth,
                                    contentDescription = null,
                                    tint = PrimaryIndigoDark,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = periodLabel,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PrimaryIndigoDark,
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

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = stringResource(R.string.ledger_total_advances),
                            style = MaterialTheme.typography.labelMedium,
                            color = OnSurfaceVariantLight
                        )
                        Text(
                            text = MoneyFormat.formatMinor(report.totalAdvancesMinor, currency, locale),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryIndigoDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        EmployerModeBreakdownTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.Payments,
                            label = stringResource(R.string.cash),
                            amountText = MoneyFormat.formatMinor(report.cashTotalMinor, currency, locale),
                            containerColor = SuccessContainer,
                            accentColor = SuccessGreen
                        )
                        EmployerModeBreakdownTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.AccountBalance,
                            label = stringResource(R.string.bank),
                            amountText = MoneyFormat.formatMinor(report.bankTotalMinor, currency, locale),
                            containerColor = InfoContainer,
                            accentColor = InfoBlue
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 3.dp, height = 16.dp)
                        .background(PrimaryIndigoLight, RoundedCornerShape(2.dp))
                )
                Text(
                    text = stringResource(R.string.advance_period_profiles_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryIndigoDark,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (report.profiles.isEmpty() && report.otherBucket == null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                    border = BorderStroke(1.dp, OutlineVariantLight),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 36.dp, horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                tint = TextHintColor,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.empty_advance_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = OnSurfaceLight,
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
            items(report.profiles, key = { it.employeeId }) { profile ->
                EmployerEmployeeProfileCard(
                    profile = profile,
                    currency = currency,
                    locale = locale,
                    onClick = { onSelectEmployee(profile.employeeId) }
                )
            }

            report.otherBucket?.let { otherBucket ->
                item(key = "other_bucket") {
                    EmployerOtherBucketCard(
                        bucket = otherBucket,
                        currency = currency,
                        locale = locale,
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
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = containerColor.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = amountText,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = OnSurfaceLight,
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
    onClick: () -> Unit
) {
    val displayName = profile.employeeName.ifBlank { stringResource(R.string.staff_label) }
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("employer_profile_card_${profile.employeeId}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        border = BorderStroke(1.dp, OutlineLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(PrimaryContainerLight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = displayName.firstOrNull()?.uppercase()
                            ?: stringResource(R.string.ledger_staff_initial_fallback),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryIndigoDark
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = OnSurfaceLight,
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

            HorizontalDivider(color = OutlineVariantLight)

            EmployerFigureRow(
                label = stringResource(R.string.advance_monthly_salary_label),
                value = MoneyFormat.formatMinor(profile.monthlySalaryMinor, currency, locale),
                valueColor = OnSurfaceLight
            )
            EmployerFigureRow(
                label = stringResource(R.string.advance_taken_label),
                value = stringResource(
                    R.string.advance_minus_amount_fmt,
                    MoneyFormat.formatMinor(profile.advancesTotalMinor, currency, locale)
                ),
                valueColor = WarningOrange
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
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("employer_profile_other_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        border = BorderStroke(1.dp, OutlineLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SurfaceVariantLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Group,
                        contentDescription = null,
                        tint = OnSurfaceVariantLight,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.advance_other_bucket_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = OnSurfaceLight,
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

            HorizontalDivider(color = OutlineVariantLight)

            EmployerFigureRow(
                label = stringResource(R.string.advance_taken_label),
                value = stringResource(
                    R.string.advance_minus_amount_fmt,
                    MoneyFormat.formatMinor(bucket.advancesTotalMinor, currency, locale)
                ),
                valueColor = WarningOrange
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
        horizontalArrangement = Arrangement.spacedBy(8.dp),
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
    val containerColor = if (isExcess) ErrorContainerLight else SuccessContainer
    val contentColor = if (isExcess) ErrorRedLight else SuccessGreen
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
        shape = RoundedCornerShape(10.dp),
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = statusIcon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            EmployerHistoryBackRow(onBack = onBack)
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("employer_history_header_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                border = BorderStroke(1.dp, OutlineLight),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                color = OnSurfaceLight,
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
                            shape = RoundedCornerShape(8.dp),
                            color = PrimaryContainerLight
                        ) {
                            Text(
                                text = periodLabel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryIndigoDark,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                maxLines = 1
                            )
                        }
                    }

                    HorizontalDivider(color = OutlineVariantLight)

                    EmployerFigureRow(
                        label = stringResource(R.string.advance_monthly_salary_label),
                        value = MoneyFormat.formatMinor(profile.monthlySalaryMinor, currency, locale),
                        valueColor = OnSurfaceLight
                    )
                    EmployerFigureRow(
                        label = stringResource(R.string.advance_taken_label),
                        value = stringResource(
                            R.string.advance_minus_amount_fmt,
                            MoneyFormat.formatMinor(profile.advancesTotalMinor, currency, locale)
                        ),
                        valueColor = WarningOrange
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

        items(
            items = profile.history,
            key = { "${it.serialNumber}_${it.dateInMillis}_${it.amountMinor}" }
        ) { entry ->
            EmployerAdvanceHistoryRowCard(
                entry = entry,
                showStaffName = false,
                currency = currency,
                locale = locale,
                zone = zone,
                dateFormatter = dateFormatter
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
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            EmployerHistoryBackRow(onBack = onBack)
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("employer_other_history_header_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                border = BorderStroke(1.dp, OutlineLight),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                color = OnSurfaceLight
                            )
                            Text(
                                text = stringResource(R.string.advance_other_bucket_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceVariantLight
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = PrimaryContainerLight
                        ) {
                            Text(
                                text = periodLabel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryIndigoDark,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                maxLines = 1
                            )
                        }
                    }

                    HorizontalDivider(color = OutlineVariantLight)

                    EmployerFigureRow(
                        label = stringResource(R.string.advance_taken_label),
                        value = stringResource(
                            R.string.advance_minus_amount_fmt,
                            MoneyFormat.formatMinor(bucket.advancesTotalMinor, currency, locale)
                        ),
                        valueColor = WarningOrange
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        EmployerModeBreakdownTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.Payments,
                            label = stringResource(R.string.cash),
                            amountText = MoneyFormat.formatMinor(bucket.cashTotalMinor, currency, locale),
                            containerColor = SuccessContainer,
                            accentColor = SuccessGreen
                        )
                        EmployerModeBreakdownTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.AccountBalance,
                            label = stringResource(R.string.bank),
                            amountText = MoneyFormat.formatMinor(bucket.bankTotalMinor, currency, locale),
                            containerColor = InfoContainer,
                            accentColor = InfoBlue
                        )
                    }
                }
            }
        }

        items(
            items = bucket.history,
            key = { "${it.serialNumber}_${it.dateInMillis}_${it.amountMinor}" }
        ) { entry ->
            EmployerAdvanceHistoryRowCard(
                entry = entry,
                showStaffName = true,
                currency = currency,
                locale = locale,
                zone = zone,
                dateFormatter = dateFormatter
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
                tint = PrimaryIndigoDark
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.back),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = PrimaryIndigoDark
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
    dateFormatter: DateTimeFormatter
) {
    val isCash = isCashAdvanceMode(entry.paymentMode)
    val modeColor = if (isCash) SuccessGreen else InfoBlue
    val modeContainer = if (isCash) SuccessContainer else InfoContainer
    val modeIcon = if (isCash) Icons.Outlined.Payments else Icons.Outlined.AccountBalance
    val modeLabel = if (isCash) stringResource(R.string.cash) else stringResource(R.string.bank)
    val formattedDate = remember(entry.dateInMillis, zone, dateFormatter) {
        Instant.ofEpochMilli(entry.dateInMillis).atZone(zone).format(dateFormatter)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("employer_history_row_${entry.serialNumber}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        border = BorderStroke(1.dp, OutlineLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(PrimaryContainerLight),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.advance_serial_fmt, entry.serialNumber),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryIndigoDark
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (showStaffName) {
                    Text(
                        text = entry.staffName.ifBlank { stringResource(R.string.staff_label) },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = OnSurfaceLight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = OnSurfaceLight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = modeContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = modeIcon,
                            contentDescription = null,
                            tint = modeColor,
                            modifier = Modifier.size(13.dp)
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
                color = PrimaryIndigoDark,
                maxLines = 1
            )
        }
    }
}

// Tabs 3 (PaidOutShoppingLedgerTab) and 4 (WalkoutLedgerTab) are extracted into SecondaryLedgerTabs.kt

