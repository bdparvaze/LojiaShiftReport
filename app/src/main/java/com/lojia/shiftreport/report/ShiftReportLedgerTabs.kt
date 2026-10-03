package com.lojia.shiftreport.report

import com.lojia.shiftreport.ui.common.LojiaTextField
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.LocalMall
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonOff
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/* ----------------------------------------------------------------------
 * DATA PARSERS & DEDICATED LEDGER TABS
 * ---------------------------------------------------------------------- */
data class DueDetailEntry(
    val receiptNo: String,
    val amount: Double,
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
                val cleanRef = rawRef.replace("Receipt #", "").trim().ifBlank { "N/A" }
                val amt = obj.optDouble("amount", 0.0)
                if (amt > 0) {
                    list.add(
                        DueDetailEntry(
                            receiptNo = cleanRef,
                            amount = amt,
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
                val cleanRef = rawRef.replace("Receipt #", "").trim().ifBlank { "N/A" }
                val amt = obj.optDouble("amount", 0.0)
                val mode = obj.optString("paymentMode", "CASH")
                if (amt > 0) {
                    list.add(
                        DueDetailEntry(
                            receiptNo = cleanRef,
                            amount = amt,
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
    val initialDue: Double,
    val totalCollected: Double
) {
    val remainingDue: Double get() = (initialDue - totalCollected).coerceAtLeast(0.0)
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
        val map = mutableMapOf<String, Pair<Double, Double>>()
        salesList.forEach { s ->
            val curr = map[s.receiptNo] ?: Pair(0.0, 0.0)
            map[s.receiptNo] = Pair(curr.first + s.amount, curr.second)
        }
        collectionsList.forEach { c ->
            val curr = map[c.receiptNo] ?: Pair(0.0, 0.0)
            map[c.receiptNo] = Pair(curr.first, curr.second + c.amount)
        }
        map.map { (ref, pair) -> DueReceiptSummary(ref, pair.first, pair.second) }
            .sortedByDescending { it.remainingDue }
    }

    val totalIssuedDue = salesList.sumOf { it.amount }
    val totalCollectedDue = collectionsList.sumOf { it.amount }
    val netOutstandingDue = (totalIssuedDue - totalCollectedDue).coerceAtLeast(0.0)

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
                    text = "Search by receipt number...",
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
                            text = "Total Issued Due",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextHintColor,
                            letterSpacing = 0.4.sp,
                            maxLines = 1
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = MoneyFormat.format(totalIssuedDue, currency),
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
                            text = "Total Collected",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextHintColor,
                            letterSpacing = 0.4.sp,
                            maxLines = 1
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = MoneyFormat.format(totalCollectedDue, currency),
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
                        text = "Net Outstanding Due",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = WarningOrange,
                        letterSpacing = 0.3.sp,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Collections automatically deduct from same receipt #",
                        fontSize = 10.sp,
                        color = OnSurfaceVariantLight,
                        maxLines = 2
                    )
                }
                Spacer(Modifier.width(8.dp))
                // Value — never wraps
                Text(
                    text = MoneyFormat.format(netOutstandingDue, currency),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = WarningOrange,
                    maxLines = 1,
                    softWrap = false
                )
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
                text = "Receipt Ledger Breakdown",
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
                    text = "No due records yet",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = OnSurfaceLight
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Receipts with outstanding balances appear here.",
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
                    item.initialDue > 0 && item.totalCollected >= item.initialDue ->
                        Triple(stringResource(R.string.paid_in_full), SuccessContainer, SuccessGreen)
                    item.totalCollected > 0 && item.remainingDue > 0 ->
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
                            Text(
                                text = "Receipt #${item.receiptNo}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = OnSurfaceLight,
                                maxLines = 1
                            )
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
                                    text = "Issued Due",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextHintColor,
                                    letterSpacing = 0.3.sp
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = MoneyFormat.format(item.initialDue, currency),
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
                                    text = "Collected (-)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextHintColor,
                                    letterSpacing = 0.3.sp
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = MoneyFormat.format(item.totalCollected, currency),
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
                                    text = "Net Remaining",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextHintColor,
                                    letterSpacing = 0.3.sp
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = MoneyFormat.format(item.remainingDue, currency),
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
    val amount: Double,
    val paymentMode: String,
    val dateInMillis: Long,
    val cashierName: String
)

fun parseEmployerAdvances(reports: List<ShiftReport>): List<EmployerAdvanceItem> {
    val list = mutableListOf<EmployerAdvanceItem>()
    reports.forEach { report ->
        try {
            val array = org.json.JSONArray(report.staffAdvancesJson)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val name = obj.optString("staffName").ifBlank { obj.optString("name", "") }
                val amt = obj.optDouble("amount", 0.0)
                val mode = obj.optString("paymentMode", "CASH")
                if (amt > 0) {
                    list.add(
                        EmployerAdvanceItem(
                            staffName = name,
                            amount = amt,
                            paymentMode = mode,
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
fun EmployerLedgerTab(
    reports: List<ShiftReport>,
    currency: String = MoneyFormat.DEFAULT_CURRENCY_CODE
) {
    val list = remember(reports) { parseEmployerAdvances(reports) }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    val totalAmount = list.sumOf { it.amount }
    val cashAmount = list.filter { it.paymentMode.equals("CASH", ignoreCase = true) }.sumOf { it.amount }
    val bankAmount = list.filter { it.paymentMode.equals("BANK", ignoreCase = true) }.sumOf { it.amount }
    val count = list.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Section 1 — KPI Row (2 cards side-by-side)
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Card 1 — Total Advances
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = PrimaryContainerLight),
                border = BorderStroke(1.dp, PrimaryIndigoLight.copy(alpha = 0.3f)),
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
                            .background(PrimaryIndigoLight.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AccountBalanceWallet,
                            contentDescription = null,
                            tint = PrimaryIndigoLight,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Total Advances",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextHintColor,
                            letterSpacing = 0.4.sp
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = MoneyFormat.format(totalAmount, currency),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryIndigoDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Card 2 — Cash vs Bank split (stacked mini rows)
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                border = BorderStroke(1.dp, OutlineLight),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    // Cash row
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(SuccessGreen)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Cash",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextHintColor
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = MoneyFormat.format(cashAmount, currency),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SuccessGreen,
                            maxLines = 1
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    // Bank row
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(InfoBlue)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Bank",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextHintColor
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = MoneyFormat.format(bankAmount, currency),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = InfoBlue,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Section 2 — Section Header (with accent bar)
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
                text = "Employer Advance Transactions",
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
                        imageVector = Icons.Outlined.AccountBalanceWallet,
                        contentDescription = null,
                        tint = TextHintColor,
                        modifier = Modifier.size(40.dp)
                    )
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    text = "No advances recorded",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = OnSurfaceLight
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Salary advances given to staff appear here.",
                    fontSize = 12.sp,
                    color = OnSurfaceVariantLight,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            }
        } else {
            list.forEach { item ->
                val displayStaffName = item.staffName.ifEmpty { stringResource(R.string.staff_label) }
                val isCash = item.paymentMode.equals("CASH", ignoreCase = true)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                    border = BorderStroke(1.dp, OutlineLight),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left — staff avatar circle
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(PrimaryContainerLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = displayStaffName.firstOrNull()?.uppercase() ?: "S",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryIndigoLight
                            )
                        }
                        Spacer(Modifier.width(12.dp))

                        // Middle — details
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = displayStaffName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = OnSurfaceLight,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(4.dp))

                            // Row: cashier icon + name + • + date
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

                        // Right — amount + payment mode chip
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = MoneyFormat.format(item.amount, currency),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryIndigoDark,
                                maxLines = 1
                            )
                            Spacer(Modifier.height(4.dp))

                            // Payment mode chip — CASH = green, BANK = blue
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isCash) SuccessContainer else InfoContainer
                            ) {
                                Text(
                                    text = item.paymentMode.uppercase(),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCash) SuccessGreen else InfoBlue,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
 * TAB 3: PAID OUT / SHOPPING LEDGER (Expenses & Purchases)
 * ---------------------------------------------------------------------- */
data class PaidOutShoppingItem(
    val itemName: String,
    val qty: Int,
    val unitPrice: Double,
    val totalAmount: Double,
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
                val price = obj.optDouble("unitPrice", 0.0)
                val total = obj.optDouble("totalAmount", 0.0)
                if (total > 0 || name.isNotBlank()) {
                    list.add(
                        PaidOutShoppingItem(
                            itemName = name,
                            qty = qty,
                            unitPrice = price,
                            totalAmount = total,
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
    val numberFormatter = remember {
        NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
    }
    val totalExpense = list.sumOf { it.totalAmount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Section 1 — Top KPI Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = WarningContainer),
            border = BorderStroke(1.dp, WarningOrange.copy(alpha = 0.35f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(WarningOrange.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ShoppingCart,
                        contentDescription = null,
                        tint = WarningOrange,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Total Shopping Paid Out",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextHintColor,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${numberFormatter.format(totalExpense)} $currency",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = WarningOrange
                    )
                }

                // Right badge — record count
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = SurfaceLight.copy(alpha = 0.7f),
                    border = BorderStroke(0.5.dp, WarningOrange.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = "${list.size} entries",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = WarningOrange,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Section 2 — Section Header (below KPI, top padding 20.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 3.dp, height = 14.dp)
                    .background(WarningOrange, RoundedCornerShape(2.dp))
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Outdoor Shopping Expenses",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryIndigoDark,
                letterSpacing = 0.3.sp
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "(${list.size})",
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
                        imageVector = Icons.Outlined.ShoppingBag,
                        contentDescription = null,
                        tint = TextHintColor,
                        modifier = Modifier.size(40.dp)
                    )
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    text = "No paid out entries yet",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = OnSurfaceLight
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Record outdoor shopping and paid out expenses here.",
                    fontSize = 12.sp,
                    color = OnSurfaceVariantLight,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    maxLines = 3
                )
            }
        } else {
            list.forEach { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                    border = BorderStroke(1.dp, OutlineLight),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left — item avatar / icon
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(WarningContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LocalMall,
                                contentDescription = null,
                                tint = WarningOrange,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))

                        // Middle — item details
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.itemName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = OnSurfaceLight,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (item.qty > 0 && item.unitPrice > 0.0) {
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = "${item.qty} pcs × ${numberFormatter.format(item.unitPrice)} $currency",
                                    fontSize = 12.sp,
                                    color = OnSurfaceVariantLight,
                                    maxLines = 1
                                )
                            } else if (item.qty > 0) {
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = "${item.qty} pcs",
                                    fontSize = 12.sp,
                                    color = OnSurfaceVariantLight,
                                    maxLines = 1
                                )
                            }
                            Spacer(Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Person,
                                    contentDescription = null,
                                    tint = TextHintColor,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    text = item.cashierName,
                                    fontSize = 11.sp,
                                    color = TextHintColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "•",
                                    fontSize = 11.sp,
                                    color = TextHintColor
                                )
                                Spacer(Modifier.width(6.dp))
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
                                text = "${numberFormatter.format(item.totalAmount)} $currency",
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

/* ----------------------------------------------------------------------
 * TAB 4: WALKOUT LEDGER (Unpaid Invoices / Walkouts)
 * ---------------------------------------------------------------------- */
data class WalkoutLogItem(
    val description: String,
    val amount: Double,
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
                val amt = obj.optDouble("amount", 0.0)
                if (amt > 0) {
                    list.add(
                        WalkoutLogItem(
                            description = desc,
                            amount = amt,
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
    val totalWalkout = list.sumOf { it.amount }
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
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = ErrorContainer),
            border = BorderStroke(1.dp, ErrorRedLight.copy(alpha = 0.3f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon circle (larger)
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
                        text = "Total Walk-out Loss",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextHintColor,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = MoneyFormat.format(totalWalkout, currency),
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
                        text = "$count bill${if (count != 1) "s" else ""}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = ErrorRedLight,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Section 2 — Section Header (with accent bar)
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
                text = "Walk-out Unpaid Bills",
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
                    text = "Unpaid table and walk-out bills recorded during shifts will appear here.",
                    fontSize = 12.sp,
                    color = OnSurfaceVariantLight,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    maxLines = 3
                )
            }
        } else {
            list.forEach { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                    border = BorderStroke(1.dp, OutlineLight),
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
                                text = MoneyFormat.format(item.amount, currency),
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
