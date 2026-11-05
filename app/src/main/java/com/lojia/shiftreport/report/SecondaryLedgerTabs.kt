package com.lojia.shiftreport.report

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import kotlinx.coroutines.launch
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

/* ----------------------------------------------------------------------
 * TAB 5: EMPLOYER ADVANCES & PAYROLL LEDGER (Foodics + SAP Standard)
 * ---------------------------------------------------------------------- */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun EmployerAdvancesTab(
    onNavigateToEmployees: () -> Unit = {},
    onViewShift: (String) -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val db = remember(context) { com.lojia.shiftreport.data.AppDatabase.getInstance(context) }
    val employeeDao = remember(db) { db.employeeDao() }
    val advanceDao = remember(db) { db.employerAdvanceDao() }
    val prefs = remember(context) { com.lojia.shiftreport.data.PreferencesRepository.getInstance(context) }
    val adminId = remember(prefs) { prefs.getSavedUsername().ifBlank { "admin" } }

    val employees by employeeDao.getAllActive().collectAsState(initial = emptyList())
    val allTransactions by advanceDao.getAllActive().collectAsState(initial = emptyList())

    var selectedEmployeeId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(employees) {
        if (selectedEmployeeId == null && employees.isNotEmpty()) {
            selectedEmployeeId = employees.first().id
        } else if (selectedEmployeeId != null && employees.none { it.id == selectedEmployeeId }) {
            selectedEmployeeId = employees.firstOrNull()?.id
        }
    }

    val selectedEmployee = remember(employees, selectedEmployeeId) {
        employees.find { it.id == selectedEmployeeId }
    }

    // Current month range
    val (monthStart, monthEnd, monthLabel) = remember {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        val end = cal.timeInMillis - 1
        val label = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(start))
        Triple(start, end, label)
    }

    val monthTransactions = remember(allTransactions, monthStart, monthEnd) {
        allTransactions.filter { it.createdAt in monthStart..monthEnd }
    }
    val totalMonthAdvances = remember(monthTransactions) {
        monthTransactions.sumOf { it.amount }
    }
    val cashMonthAdvances = remember(monthTransactions) {
        monthTransactions.filter { it.paymentMethod == "CASH" }.sumOf { it.amount }
    }
    val bankMonthAdvances = remember(monthTransactions) {
        monthTransactions.filter { it.paymentMethod == "BANK" }.sumOf { it.amount }
    }

    val txByEmployee = remember(allTransactions) {
        allTransactions.groupBy { it.employeeId }
    }

    // Long-press action states
    var actionTargetTx by remember { mutableStateOf<com.lojia.shiftreport.data.EmployerAdvanceTransaction?>(null) }
    var correctionTargetTx by remember { mutableStateOf<com.lojia.shiftreport.data.EmployerAdvanceTransaction?>(null) }
    var pendingCorrectionData by remember { mutableStateOf<Pair<Double, String>?>(null) }
    var showAdminAuthForCorrection by remember { mutableStateOf(false) }
    var voidTargetTx by remember { mutableStateOf<com.lojia.shiftreport.data.EmployerAdvanceTransaction?>(null) }
    var showAdminAuthForVoid by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Section: Row of 2 Summary Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Total Advances",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = String.format(Locale.US, "%.2f SAR", totalMonthAdvances),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = monthLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                    )
                }
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Cash / Bank Breakdown",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = String.format(Locale.US, "Cash: %.2f SAR", cashMonthAdvances),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format(Locale.US, "Bank: %.2f SAR", bankMonthAdvances),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (employees.isEmpty()) {
            Card(
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = "Add your first employee",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Button(onClick = onNavigateToEmployees) {
                        Text("Manage Employees")
                    }
                }
            }
        } else {
            // Middle Section: LazyRow of Employee Cards
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(employees.size) { index ->
                    val emp = employees[index]
                    val empTxs = txByEmployee[emp.id].orEmpty()
                    val totalTaken = empTxs.sumOf { it.amount }
                    val remaining = emp.monthlySalary - totalTaken
                    val isSelected = emp.id == selectedEmployeeId

                    Card(
                        onClick = { selectedEmployeeId = emp.id },
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        ),
                        modifier = Modifier.width(185.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = emp.name.take(1).uppercase(Locale.getDefault()),
                                        color = androidx.compose.ui.graphics.Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                                Text(
                                    text = emp.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Text(
                                text = String.format(Locale.US, "Salary: %.0f SAR", emp.monthlySalary),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = String.format(Locale.US, "Taken: %.2f SAR", totalTaken),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = String.format(Locale.US, "Balance: %.2f SAR", remaining),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (remaining >= 0) PosCashGreen else ErrorRedLight
                            )
                        }
                    }
                }
            }

            // Print Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val emp = selectedEmployee ?: return@OutlinedButton
                        val empTxs = txByEmployee[emp.id].orEmpty()
                        coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            val pdfFile = com.lojia.shiftreport.util.EmployerAdvancePdfGenerator.generateEmployeeAdvanceReport(
                                context = context,
                                employee = emp,
                                transactions = empTxs,
                                dateRange = monthLabel
                            )
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                android.widget.Toast.makeText(
                                    context,
                                    "PDF saved: ${pdfFile.name}",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    enabled = selectedEmployee != null,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Print Employee", fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                OutlinedButton(
                    onClick = {
                        coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            val pdfFile = com.lojia.shiftreport.util.EmployerAdvancePdfGenerator.generateAllEmployeesSummaryReport(
                                context = context,
                                employees = employees,
                                allTransactions = allTransactions,
                                dateRange = monthLabel
                            )
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                android.widget.Toast.makeText(
                                    context,
                                    "Summary PDF saved: ${pdfFile.name}",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Print All Summary", fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            // Bottom Section: Per-Employee Detailed Ledger
            selectedEmployee?.let { emp ->
                val empTxs = txByEmployee[emp.id].orEmpty()
                Text(
                    text = "${emp.name}'s Advance Transactions (${empTxs.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                if (empTxs.isEmpty()) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No advances for ${emp.name}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    // Compute running balance per transaction
                    val sortedAsc = empTxs.sortedBy { it.createdAt }
                    val balanceMap = mutableMapOf<String, Double>()
                    var running = 0.0
                    for (t in sortedAsc) {
                        running += t.amount
                        balanceMap[t.id] = emp.monthlySalary - running
                    }

                    val dateFmt = remember { SimpleDateFormat("dd MMM", Locale.getDefault()) }

                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(empTxs.size) { idx ->
                            val tx = empTxs[idx]
                            val balAfter = balanceMap[tx.id] ?: emp.monthlySalary
                            val isCorrection = tx.type == "CORRECTION"

                            Card(
                                shape = RoundedCornerShape(12.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {},
                                        onLongClick = { actionTargetTx = tx }
                                    )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = dateFmt.format(Date(tx.createdAt)),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            if (tx.shiftId.isNotBlank()) {
                                                Text(
                                                    text = "#${tx.shiftId.take(6)}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            AssistChip(
                                                onClick = {},
                                                label = {
                                                    Text(
                                                        text = if (isCorrection) "Correction" else tx.paymentMethod,
                                                        fontSize = 10.sp
                                                    )
                                                },
                                                modifier = Modifier.height(22.dp)
                                            )
                                        }
                                        if (tx.reason.isNotBlank()) {
                                            Text(
                                                text = tx.reason,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = if (tx.amount >= 0) {
                                                String.format(Locale.US, "+%.2f SAR", tx.amount)
                                            } else {
                                                String.format(Locale.US, "%.2f SAR", tx.amount)
                                            },
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (tx.amount >= 0) PosCashGreen else ErrorRedLight
                                        )
                                        Text(
                                            text = String.format(Locale.US, "Bal: %.2f", balAfter),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Long-press Options Dialog
    actionTargetTx?.let { tx ->
        AlertDialog(
            onDismissRequest = { actionTargetTx = null },
            title = { Text("Transaction Options", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            actionTargetTx = null
                            correctionTargetTx = tx
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add Correction (-Amount)")
                    }
                    TextButton(
                        onClick = {
                            actionTargetTx = null
                            voidTargetTx = tx
                            showAdminAuthForVoid = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Void Transaction", color = MaterialTheme.colorScheme.error)
                    }
                    if (tx.shiftId.isNotBlank()) {
                        TextButton(
                            onClick = {
                                actionTargetTx = null
                                onViewShift(tx.shiftId)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("View Shift #${tx.shiftId.take(6)}")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { actionTargetTx = null }) {
                    Text("Close")
                }
            }
        )
    }

    // Correction Input Dialog
    correctionTargetTx?.let { origTx ->
        var corrAmountText by remember { mutableStateOf("-50") }
        var corrReason by remember { mutableStateOf("") }
        var corrError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { correctionTargetTx = null },
            title = { Text("Add Payroll Correction", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = corrAmountText,
                        onValueChange = { corrAmountText = it; corrError = null },
                        label = { Text("Correction Amount (e.g. -50)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = corrReason,
                        onValueChange = { corrReason = it },
                        label = { Text("Reason") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (corrError != null) {
                        Text(corrError ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = corrAmountText.trim().toDoubleOrNull()
                        if (amt == null || amt == 0.0) {
                            corrError = "Enter a valid non-zero amount (e.g. -50)"
                            return@Button
                        }
                        val normalized = if (amt > 0) -amt else amt
                        pendingCorrectionData = normalized to corrReason.trim().ifBlank { "Payroll Correction" }
                        showAdminAuthForCorrection = true
                    }
                ) {
                    Text("Verify & Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { correctionTargetTx = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAdminAuthForCorrection && correctionTargetTx != null && pendingCorrectionData != null) {
        val origTx = correctionTargetTx!!
        val (corrAmt, corrReason) = pendingCorrectionData!!
        com.lojia.shiftreport.auth.AdminAuthDialog(
            onDismissRequest = { showAdminAuthForCorrection = false },
            onAuthSuccess = {
                showAdminAuthForCorrection = false
                correctionTargetTx = null
                pendingCorrectionData = null
                coroutineScope.launch {
                    advanceDao.insert(
                        com.lojia.shiftreport.data.EmployerAdvanceTransaction(
                            employeeId = origTx.employeeId,
                            employeeName = origTx.employeeName,
                            shiftId = origTx.shiftId,
                            amount = corrAmt,
                            paymentMethod = origTx.paymentMethod,
                            type = com.lojia.shiftreport.data.AdvanceTransactionType.CORRECTION.name,
                            reason = corrReason,
                            originalTransactionId = origTx.id,
                            createdByAdminId = adminId
                        )
                    )
                }
            }
        )
    }

    if (showAdminAuthForVoid && voidTargetTx != null) {
        val target = voidTargetTx!!
        com.lojia.shiftreport.auth.AdminAuthDialog(
            onDismissRequest = {
                showAdminAuthForVoid = false
                voidTargetTx = null
            },
            onAuthSuccess = {
                showAdminAuthForVoid = false
                voidTargetTx = null
                coroutineScope.launch {
                    advanceDao.voidTransaction(target.id)
                }
            }
        )
    }
}
