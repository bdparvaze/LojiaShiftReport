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

        // Section 2 — Section Header
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
