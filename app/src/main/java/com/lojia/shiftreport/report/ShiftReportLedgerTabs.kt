package com.lojia.shiftreport.report

import com.lojia.shiftreport.ui.common.LojiaTextField
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojia.shiftreport.R
import com.lojia.shiftreport.data.ShiftReport
import com.lojia.shiftreport.ui.theme.ShiftColors
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
fun DueLedgerTab(reports: List<ShiftReport>) {
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        LojiaTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(stringResource(R.string.search_receipt_number)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ShiftColors.TextMuted) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.btn_clear), tint = ShiftColors.TextMuted)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ShiftColors.Primary,
                unfocusedBorderColor = ShiftColors.Border,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            MetricCard(
                title = stringResource(R.string.total_issued_due),
                amount = "%.2f ${stringResource(R.string.currency_unit)}".format(totalIssuedDue),
                icon = Icons.Default.ReceiptLong,
                color = ShiftColors.Danger,
                bgColor = ShiftColors.DangerLight,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = stringResource(R.string.total_collected),
                amount = "%.2f ${stringResource(R.string.currency_unit)}".format(totalCollectedDue),
                icon = Icons.Default.PriceCheck,
                color = ShiftColors.NetCashGreen,
                bgColor = Color(0xFFECFDF5),
                modifier = Modifier.weight(1f)
            )
        }

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = ShiftColors.BrassLight),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        stringResource(R.string.net_outstanding_due),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ShiftColors.Brass
                    )
                    Text(
                        stringResource(R.string.deduct_auto_hint),
                        fontSize = 10.sp,
                        color = ShiftColors.TextMuted
                    )
                }
                Text(
                    "%.2f ${stringResource(R.string.currency_unit)}".format(netOutstandingDue),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ShiftColors.Brass
                )
            }
        }

        Text(
            stringResource(R.string.receipt_breakdown_count, filteredReceipts.size),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = ShiftColors.Charcoal
        )

        if (filteredReceipts.isEmpty()) {
            Text(
                stringResource(R.string.no_due_records),
                fontSize = 13.sp,
                color = ShiftColors.TextMuted,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        } else {
            filteredReceipts.forEach { item ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(R.string.receipt_num_tag, item.receiptNo),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = ShiftColors.Charcoal
                            )

                            val (statusText, statusBg, statusFg) = when {
                                item.initialDue > 0 && item.totalCollected >= item.initialDue ->
                                    Triple(stringResource(R.string.paid_in_full), Color(0xFFECFDF5), ShiftColors.NetCashGreen)
                                item.totalCollected > 0 && item.remainingDue > 0 ->
                                    Triple(stringResource(R.string.partially_paid), ShiftColors.BrassLight, ShiftColors.Brass)
                                else ->
                                    Triple(stringResource(R.string.unpaid_due), ShiftColors.DangerLight, ShiftColors.Danger)
                            }

                            Surface(shape = RoundedCornerShape(6.dp), color = statusBg) {
                                Text(
                                    statusText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = statusFg,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = ShiftColors.Border)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(stringResource(R.string.issued_due), fontSize = 10.sp, color = ShiftColors.TextMuted)
                                Text("%.2f ${stringResource(R.string.currency_unit)}".format(item.initialDue), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = ShiftColors.Danger)
                            }
                            Column {
                                Text(stringResource(R.string.collected_minus), fontSize = 10.sp, color = ShiftColors.TextMuted)
                                Text("%.2f ${stringResource(R.string.currency_unit)}".format(item.totalCollected), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = ShiftColors.NetCashGreen)
                            }
                            Column {
                                Text(stringResource(R.string.net_remaining), fontSize = 10.sp, color = ShiftColors.TextMuted)
                                Text("%.2f ${stringResource(R.string.currency_unit)}".format(item.remainingDue), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = ShiftColors.Charcoal)
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
fun EmployerLedgerTab(reports: List<ShiftReport>) {
    val list = remember(reports) { parseEmployerAdvances(reports) }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    val totalAmount = list.sumOf { it.amount }
    val cashAmount = list.filter { it.paymentMode.equals("CASH", ignoreCase = true) }.sumOf { it.amount }
    val bankAmount = list.filter { it.paymentMode.equals("BANK", ignoreCase = true) }.sumOf { it.amount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            MetricCard(
                title = stringResource(R.string.total_advances),
                amount = "%.2f ${stringResource(R.string.currency_unit)}".format(totalAmount),
                icon = Icons.Default.AccountBalanceWallet,
                color = ShiftColors.Purple,
                bgColor = ShiftColors.PurpleLight,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = stringResource(R.string.cash_bank_label),
                amount = "%.0f / %.0f".format(cashAmount, bankAmount),
                icon = Icons.Default.Payments,
                color = ShiftColors.NetMadaBlue,
                bgColor = Color(0xFFEFF6FF),
                modifier = Modifier.weight(1f)
            )
        }

        Text(
            stringResource(R.string.employer_advances_count, list.size),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = ShiftColors.Charcoal
        )

        if (list.isEmpty()) {
            Text(
                stringResource(R.string.no_employer_advance_records),
                fontSize = 13.sp,
                color = ShiftColors.TextMuted,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        } else {
            list.forEach { item ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(item.staffName.ifEmpty { stringResource(R.string.staff_label) }, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ShiftColors.Charcoal)
                            Text("${item.cashierName} • ${dateFormat.format(Date(item.dateInMillis))}", fontSize = 11.sp, color = ShiftColors.TextMuted)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("%.2f ${stringResource(R.string.currency_unit)}".format(item.amount), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ShiftColors.Purple)
                            Text(item.paymentMode, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = ShiftColors.TextMuted)
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
                val qty = obj.optInt("quantity", obj.optDouble("quantity", 1.0).toInt())
                val price = obj.optDouble("unitPrice", 0.0)
                val total = obj.optDouble("totalAmount", qty * price)
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
fun PaidOutShoppingLedgerTab(reports: List<ShiftReport>) {
    val list = remember(reports) { parsePaidOutItems(reports) }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val totalExpense = list.sumOf { it.totalAmount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        MetricCard(
            title = stringResource(R.string.total_shopping_expense),
            amount = "%.2f ${stringResource(R.string.currency_unit)}".format(totalExpense),
            icon = Icons.Default.ShoppingCart,
            color = ShiftColors.Danger,
            bgColor = ShiftColors.DangerLight,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            stringResource(R.string.outdoor_shopping_expenses_count, list.size),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = ShiftColors.Charcoal
        )

        if (list.isEmpty()) {
            Text(
                stringResource(R.string.no_shopping_records),
                fontSize = 13.sp,
                color = ShiftColors.TextMuted,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        } else {
            list.forEach { item ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(item.itemName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ShiftColors.Charcoal)
                            Text(stringResource(R.string.pcs_at_price, item.qty, item.unitPrice, item.cashierName), fontSize = 11.sp, color = ShiftColors.TextMuted)
                            Text(dateFormat.format(Date(item.dateInMillis)), fontSize = 10.sp, color = ShiftColors.TextMuted)
                        }
                        Text("%.2f ${stringResource(R.string.currency_unit)}".format(item.totalAmount), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ShiftColors.Danger)
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
fun WalkoutLedgerTab(reports: List<ShiftReport>) {
    val list = remember(reports) { parseWalkoutItems(reports) }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val totalWalkout = list.sumOf { it.amount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        MetricCard(
            title = stringResource(R.string.total_walkout_loss),
            amount = "%.2f ${stringResource(R.string.currency_unit)}".format(totalWalkout),
            icon = Icons.Default.DirectionsWalk,
            color = ShiftColors.Danger,
            bgColor = ShiftColors.DangerLight,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            stringResource(R.string.walkout_unpaid_bills_count, list.size),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = ShiftColors.Charcoal
        )

        if (list.isEmpty()) {
            Text(
                stringResource(R.string.no_walkout_records),
                fontSize = 13.sp,
                color = ShiftColors.TextMuted,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        } else {
            list.forEach { item ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(item.description, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ShiftColors.Charcoal)
                            Text("${item.cashierName} • ${dateFormat.format(Date(item.dateInMillis))}", fontSize = 11.sp, color = ShiftColors.TextMuted)
                        }
                        Text("%.2f ${stringResource(R.string.currency_unit)}".format(item.amount), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ShiftColors.Danger)
                    }
                }
            }
        }
    }
}
