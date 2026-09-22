package com.lojia.shiftreport.report

import androidx.compose.foundation.layout.imePadding
import com.lojia.shiftreport.ui.common.LojiaTextField
import com.lojia.shiftreport.R
import com.lojia.shiftreport.data.*
import com.lojia.shiftreport.util.*
import com.lojia.shiftreport.ui.common.*
import com.lojia.shiftreport.ui.theme.*
import com.lojia.shiftreport.auth.*
import com.lojia.shiftreport.report.*
import com.lojia.shiftreport.settings.*


import android.widget.Toast


import androidx.compose.foundation.Canvas


import androidx.compose.foundation.background


import androidx.compose.foundation.border


import androidx.compose.foundation.clickable


import androidx.compose.foundation.layout.*


import androidx.compose.foundation.lazy.LazyColumn


import androidx.compose.foundation.lazy.items


import androidx.compose.foundation.rememberScrollState


import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.TextStyle


import androidx.compose.foundation.verticalScroll


import androidx.compose.material.icons.Icons


import androidx.compose.material.icons.automirrored.filled.ListAlt


import androidx.compose.material.icons.filled.*


import androidx.compose.material.icons.outlined.*


import androidx.compose.material3.*


import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset


import androidx.compose.runtime.*


import androidx.compose.runtime.saveable.rememberSaveable


import androidx.compose.ui.Alignment


import androidx.compose.ui.Modifier


import androidx.compose.ui.draw.clip


import androidx.compose.ui.geometry.Offset


import androidx.compose.ui.graphics.Color


import androidx.compose.ui.graphics.PathEffect


import androidx.compose.ui.graphics.vector.ImageVector


import androidx.compose.foundation.gestures.detectTapGestures


import androidx.compose.ui.input.pointer.pointerInput


import androidx.compose.ui.platform.LocalContext


import androidx.compose.ui.platform.LocalFocusManager


import androidx.compose.ui.platform.LocalSoftwareKeyboardController


import androidx.compose.ui.platform.testTag


import androidx.compose.ui.text.font.FontWeight


import androidx.compose.ui.text.input.KeyboardType


import androidx.compose.ui.text.style.TextAlign


import androidx.compose.ui.text.style.TextOverflow


import androidx.compose.ui.unit.dp


import androidx.compose.ui.unit.sp


import java.text.SimpleDateFormat

import java.util.*


import androidx.compose.ui.res.stringResource


/* ----------------------------------------------------------------------
 * DATA MODELS FOR ENTRY FORM
 * ---------------------------------------------------------------------- */
enum class PayType { CASH, BANK }

data class CreditEntry(val receiptNo: String, val amount: Double)
data class OldDueEntry(val receiptNo: String, val amount: Double, val type: PayType)
data class StaffEntry(val name: String, val amount: Double, val type: PayType)
data class WalkoutEntry(val description: String, val amount: Double)
data class ItemEntry(val name: String, val qty: Int, val unitPrice: Double) {
    val total get() = qty * unitPrice
}

/** Snapshot handed to onSave — plug this into Room / SharedPreferences / your API. */
data class ShiftReportData(
    val cashier: String,
    val shift: String,
    val date: String,
    val cashReceipts: Double,
    val madaPayments: Double,
    val digitalWallet: Double = 0.0,
    val startingCash: Double = 0.0,
    val actualCash: Double? = null,
    val notes: String = "",
    val staffCount: Int,
    val totalExpenses: Double,
    val muassel: Int,
    val outdoorMuassel: Int,
    val creditEntries: List<CreditEntry>,
    val oldDueEntries: List<OldDueEntry>,
    val staffEntries: List<StaffEntry>,
    val walkoutEntries: List<WalkoutEntry>,
    val itemEntries: List<ItemEntry>,
    val netCash: Double,
    val netMada: Double,
    val dateMillis: Long = System.currentTimeMillis()
)

private enum class ModalType { NONE, CREDIT, OLD_DUE, STAFF, WALKOUT, ITEM }

/* ----------------------------------------------------------------------
 * OVERLOAD FOR VIEWMODEL & NAVIGATION INTEGRATION (WITH TOP 3 TABS)
 * ---------------------------------------------------------------------- */
@Composable
fun ShiftReportScreen(
    viewModel: ReportViewModel,
    language: AppLanguage = AppLanguage.ENGLISH,
    onPreviewPdf: (ShiftReport) -> Unit = {}
) {
    val context = LocalContext.current
    val userProfile by viewModel.userProfile.collectAsState()
    val shiftReports by viewModel.shiftReports.collectAsState()
    val activeShiftSession by viewModel.activeShiftSession.collectAsState()
    val cashMovements by viewModel.cashMovements.collectAsState()
    val cashiersState by viewModel.cashiers.collectAsState()

    val availableCashiers = remember(cashiersState) {
        val names = cashiersState.map { it.name }.filter { it.isNotBlank() }
        if (names.isNotEmpty()) names else listOf("Noora", "Hassan", "Athar")
    }

    ShiftReportScreenContent(
        shiftReports = shiftReports,
        userProfile = userProfile,
        activeShiftSession = activeShiftSession,
        cashMovements = cashMovements,
        cashierOptions = availableCashiers,
        language = language,
        onPreviewPdf = onPreviewPdf,
        onSaveReportData = { data ->
            val dueJson = org.json.JSONArray().apply {
                data.creditEntries.forEach { e ->
                    put(org.json.JSONObject().apply {
                        put("receiptNo", e.receiptNo)
                        put("customerName", "Receipt #${e.receiptNo}")
                        put("amount", e.amount)
                    })
                }
            }.toString()

            val prevDueJson = org.json.JSONArray().apply {
                data.oldDueEntries.forEach { e ->
                    put(org.json.JSONObject().apply {
                        put("receiptNo", e.receiptNo)
                        put("customerName", "Receipt #${e.receiptNo}")
                        put("amount", e.amount)
                        put("paymentMode", e.type.name)
                    })
                }
            }.toString()

            val staffAdvJson = org.json.JSONArray().apply {
                data.staffEntries.forEach { e ->
                    put(org.json.JSONObject().apply {
                        put("staffName", e.name)
                        put("amount", e.amount)
                        put("paymentMode", e.type.name)
                    })
                }
            }.toString()

            val walkoutJson = org.json.JSONArray().apply {
                data.walkoutEntries.forEach { e ->
                    put(org.json.JSONObject().apply {
                        put("tableOrOrderRef", e.description)
                        put("amount", e.amount)
                    })
                }
            }.toString()

            val itemsJson = org.json.JSONArray().apply {
                data.itemEntries.forEach { e ->
                    put(org.json.JSONObject().apply {
                        put("itemName", e.name)
                        put("quantity", e.qty.toDouble())
                        put("unitPrice", e.unitPrice)
                        put("totalAmount", e.total)
                    })
                }
            }.toString()

            val noteParts = mutableListOf<String>()
            if (data.startingCash > 0) {
                noteParts.add("Starting Float: %.2f".format(Locale.US, data.startingCash))
            }
            if (data.actualCash != null) {
                noteParts.add("Actual Cash Count: %.2f".format(Locale.US, data.actualCash))
            }
            if (data.notes.isNotBlank() && data.notes != "Saved from Shift Closing Ledger") {
                noteParts.add(data.notes)
            }
            val formattedNotes = if (noteParts.isNotEmpty()) noteParts.joinToString(" | ") else "Saved from Shift Closing Ledger"

            val report = ShiftReport(
                cashierName = data.cashier.ifBlank { "Standard Cashier" },
                shift = data.shift,
                dateInMillis = data.dateMillis,
                grossCash = data.cashReceipts,
                madaPayments = data.madaPayments,
                digitalWallet = data.digitalWallet,
                staffMealsCount = data.staffCount,
                totalExpenses = data.totalExpenses,
                muasselQty = data.muassel.toDouble(),
                outdoorShishaQty = data.outdoorMuassel.toDouble(),
                dueCreditEntriesJson = dueJson,
                previousDueCollectionsJson = prevDueJson,
                staffAdvancesJson = staffAdvJson,
                unpaidBillsJson = walkoutJson,
                purchasedItemsJson = itemsJson,
                notes = formattedNotes
            )

            viewModel.saveShiftReportDirect(report) { savedReport ->
                onPreviewPdf(savedReport)
            }
            Toast.makeText(context, context.getString(R.string.shift_report_saved_success), Toast.LENGTH_SHORT).show()
        },
        onOpenShift = { cashier, shiftName, startingCash ->
            viewModel.openShift(cashier, shiftName, startingCash)
        },
        onCloseShift = { session, actualCash, notes ->
            viewModel.closeShiftWithZReport(session, actualCash, notes) { savedReport ->
                onPreviewPdf(savedReport)
            }
        },
        onDeleteReport = { report ->
            viewModel.deleteReport(report)
        }
    )
}

/* ----------------------------------------------------------------------
 * MAIN SCREEN CONTENT WITH SCROLLABLE OPTIONS TABS
 * ---------------------------------------------------------------------- */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftReportScreenContent(
    shiftReports: List<ShiftReport> = emptyList(),
    userProfile: com.lojia.shiftreport.data.UserProfile? = null,
    activeShiftSession: ShiftSession? = null,
    cashMovements: List<CashMovement> = emptyList(),
    cashierOptions: List<String> = listOf("Noora", "Hassan", "Athar"),
    language: AppLanguage = AppLanguage.BENGALI,
    onPreviewPdf: (ShiftReport) -> Unit = {},
    onSaveReportData: (ShiftReportData) -> Unit = {},
    onOpenShift: (String, String, Double) -> Unit = { _, _, _ -> },
    onCloseShift: (ShiftSession, Double, String) -> Unit = { _, _, _ -> },
    onDeleteReport: (ShiftReport) -> Unit = {}
) {
    // 0 = Report Entry, 1 = Due History, 2 = Employer, 3 = Shopping/Paid Out, 4 = Walkout, 5 = Archives
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val tabTitles = listOf(
        stringResource(R.string.tab_report_entry),
        stringResource(R.string.tab_due_history),
        stringResource(R.string.tab_employer),
        stringResource(R.string.tab_paid_out),
        stringResource(R.string.tab_walkout),
        stringResource(R.string.tab_archives, shiftReports.size)
    )

    val tabIcons = listOf(
        Icons.AutoMirrored.Filled.ListAlt,
        Icons.Default.ReceiptLong,
        Icons.Default.AccountBalanceWallet,
        Icons.Default.ShoppingCart,
        Icons.Default.DirectionsWalk,
        Icons.Default.FolderZip
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ShiftColors.Bg)
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                })
            }
    ) {
        // ---- SCROLLABLE OPTIONS TAB ROW ----
        Surface(
            color = Color.White,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = ShiftColors.Primary,
                edgePadding = 8.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = ShiftColors.PrimaryDark,
                        height = 3.dp
                    )
                },
                divider = { HorizontalDivider(color = ShiftColors.Border) }
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            selectedTab = index
                        },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = tabIcons[index],
                                    contentDescription = null,
                                    modifier = Modifier.size(17.dp),
                                    tint = if (selectedTab == index) ShiftColors.PrimaryDark else ShiftColors.TextMuted
                                )
                                Spacer(Modifier.width(5.dp))
                                AutoText(
                                    text = title,
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTab == index) ShiftColors.PrimaryDark else ShiftColors.TextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    )
                }
            }
        }

        // ---- TAB CONTENT ----
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> ReportEntryTab(
                    cashierOptions = cashierOptions,
                    onSave = onSaveReportData
                )
                1 -> DueLedgerTab(
                    reports = shiftReports
                )
                2 -> EmployerLedgerTab(
                    reports = shiftReports
                )
                3 -> PaidOutShoppingLedgerTab(
                    reports = shiftReports
                )
                4 -> WalkoutLedgerTab(
                    reports = shiftReports
                )
                5 -> ShiftReportArchivesTab(
                    reports = shiftReports,
                    userProfile = userProfile,
                    onPreviewPdf = onPreviewPdf,
                    onDeleteReport = onDeleteReport
                )
            }
        }
    }
}

/* ----------------------------------------------------------------------
 * TAB 0: REPORT ENTRY (SHIFT CLOSING LEDGER FORM)
 * ---------------------------------------------------------------------- */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportEntryTab(
    cashierOptions: List<String>,
    onSave: (ShiftReportData) -> Unit
) {
    val shiftOptions = listOf(stringResource(R.string.shift_day), stringResource(R.string.shift_night_option))

    var cashier by rememberSaveable { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var shift by rememberSaveable { mutableStateOf("Day") }
    var dateMillis by rememberSaveable { mutableStateOf(System.currentTimeMillis()) }

    var startingCashInput by rememberSaveable { mutableStateOf("") }
    var cashReceipts by rememberSaveable { mutableStateOf("") }
    var madaPayments by rememberSaveable { mutableStateOf("") }
    var digitalWalletInput by rememberSaveable { mutableStateOf("") }
    var actualCashCountInput by rememberSaveable { mutableStateOf("") }
    var notesInput by rememberSaveable { mutableStateOf("") }

    var staffCount by rememberSaveable { mutableStateOf(0) }
    var totalExpenses by rememberSaveable { mutableStateOf("") }

    var muassel by rememberSaveable { mutableStateOf("") }
    var outdoorMuassel by rememberSaveable { mutableStateOf("") }

    val creditEntries = remember { mutableStateListOf<CreditEntry>() }
    val oldDueEntries = remember { mutableStateListOf<OldDueEntry>() }
    val staffEntries = remember { mutableStateListOf<StaffEntry>() }
    val walkoutEntries = remember { mutableStateListOf<WalkoutEntry>() }
    val itemEntries = remember { mutableStateListOf<ItemEntry>() }

    var modalType by remember { mutableStateOf(ModalType.NONE) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    fun d(s: String) = s.toDoubleOrNull() ?: 0.0

    val startingCash = d(startingCashInput)
    val cash = d(cashReceipts)
    val mada = d(madaPayments)
    val digitalWallet = d(digitalWalletInput)
    val actualCashVal = actualCashCountInput.toDoubleOrNull()
    val expenses = d(totalExpenses)

    val totalCredit = creditEntries.sumOf { it.amount }
    val totalOldDueCash = oldDueEntries.filter { it.type == PayType.CASH }.sumOf { it.amount }
    val totalOldDueBank = oldDueEntries.filter { it.type == PayType.BANK }.sumOf { it.amount }
    val totalStaffCash = staffEntries.filter { it.type == PayType.CASH }.sumOf { it.amount }
    val totalStaffBank = staffEntries.filter { it.type == PayType.BANK }.sumOf { it.amount }
    val totalWalkout = walkoutEntries.sumOf { it.amount }
    val totalItems = itemEntries.sumOf { it.total }

    // International Standard Formulas
    val totalSales = cash + mada + digitalWallet
    val totalCashIn = cash + totalOldDueCash
    val totalCashOut = expenses + totalStaffCash + totalItems
    val expectedCash = startingCash + totalCashIn - totalCashOut
    val variance = actualCashVal?.let { it - expectedCash }
    val netMada = mada + digitalWallet + totalOldDueBank - totalStaffBank

    val hasEnteredData = cashier.isNotBlank() || startingCashInput.isNotBlank() ||
            cashReceipts.isNotBlank() || madaPayments.isNotBlank() || digitalWalletInput.isNotBlank() ||
            actualCashCountInput.isNotBlank() || notesInput.isNotBlank() ||
            totalExpenses.isNotBlank() || staffCount > 0 ||
            muassel.isNotBlank() || outdoorMuassel.isNotBlank() ||
            creditEntries.isNotEmpty() || oldDueEntries.isNotEmpty() ||
            staffEntries.isNotEmpty() || walkoutEntries.isNotEmpty() ||
            itemEntries.isNotEmpty()

    val canSave = cashier.isNotBlank() && (
            cash > 0.0 || mada > 0.0 || digitalWallet > 0.0 || expenses > 0.0 || startingCash > 0.0 ||
            (muassel.toIntOrNull() ?: 0) > 0 || (outdoorMuassel.toIntOrNull() ?: 0) > 0 ||
            creditEntries.isNotEmpty() || oldDueEntries.isNotEmpty() ||
            staffEntries.isNotEmpty() || walkoutEntries.isNotEmpty() ||
            itemEntries.isNotEmpty()
    )

    fun resetAll() {
        cashier = ""; shift = "Day"; dateMillis = System.currentTimeMillis()
        startingCashInput = ""; cashReceipts = ""; madaPayments = ""; digitalWalletInput = ""
        actualCashCountInput = ""; notesInput = ""
        staffCount = 0; totalExpenses = ""
        muassel = ""; outdoorMuassel = ""
        creditEntries.clear(); oldDueEntries.clear(); staffEntries.clear()
        walkoutEntries.clear(); itemEntries.clear()
    }

    val dateLabel = remember(dateMillis) {
        SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date(dateMillis))
    }

    Scaffold(
        containerColor = ShiftColors.Bg,
        bottomBar = {
            StickySummaryBar(expectedCash = expectedCash, variance = variance)
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .testTag("shift_report_dashboard")
        ) {
            // ---- Header card ----
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = ShiftColors.Card),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 14.dp)) {
                        val selectCashierText = stringResource(R.string.select_cashier)
                        val dayShiftText = stringResource(R.string.shift_day)
                        LabeledDropdown(
                            label = stringResource(R.string.employee_cashier_name),
                            options = listOf(selectCashierText) + cashierOptions,
                            selected = cashier.ifEmpty { selectCashierText },
                            onSelected = { cashier = if (it == selectCashierText) "" else it }
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.weight(1f)) {
                                LabeledDropdown(
                                    label = stringResource(R.string.shift),
                                    options = shiftOptions,
                                    selected = if (shift == "Day") dayShiftText else stringResource(R.string.shift_night_option),
                                    onSelected = { shift = if (it == dayShiftText) "Day" else "Night" }
                                )
                            }
                            Box(Modifier.weight(1f)) {
                                Column {
                                    FieldLabel(stringResource(R.string.date))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .defaultMinSize(minHeight = 40.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White)
                                            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                                            .clickable { showDatePicker = true }
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = dateLabel,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = ShiftColors.Charcoal,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.DateRange,
                                                contentDescription = stringResource(R.string.select_date),
                                                tint = ShiftColors.Primary,
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // ---- Starting Cash & Payment Methods ----
                        SectionHeader("💰", stringResource(R.string.sales_summary), ShiftColors.BrassLight, ShiftColors.Brass, ShiftColors.Brass)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NumberField(stringResource(R.string.starting_cash), startingCashInput, { startingCashInput = it }, Modifier.weight(1f))
                            NumberField(stringResource(R.string.cash), cashReceipts, { cashReceipts = it }, Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NumberField(stringResource(R.string.mada_bank), madaPayments, { madaPayments = it }, Modifier.weight(1f))
                            NumberField(stringResource(R.string.digital_wallet), digitalWalletInput, { digitalWalletInput = it }, Modifier.weight(1f))
                        }

                        // ---- Operational Expenses ----
                        SectionHeader("🧾", stringResource(R.string.operational_expenses), ShiftColors.DangerLight, ShiftColors.Danger, ShiftColors.Danger)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.weight(1f)) {
                                val personText = stringResource(R.string.person)
                                LabeledDropdown(
                                    label = stringResource(R.string.staff),
                                    options = (0..5).map { if (it == 0) personText else stringResource(R.string.people_count, it) },
                                    selected = if (staffCount == 0) personText else stringResource(R.string.people_count, staffCount),
                                    onSelected = { sel ->
                                        val n = sel.filter { it.isDigit() }.toIntOrNull() ?: 0
                                        staffCount = n
                                        if (n == 0) totalExpenses = ""
                                    }
                                )
                            }
                            Box(Modifier.weight(1f)) {
                                NumberField(
                                    stringResource(R.string.total_expense), totalExpenses, { totalExpenses = it },
                                    Modifier.fillMaxWidth(), enabled = staffCount != 0
                                )
                            }
                        }

                        // ---- Mu'assel ----
                        SectionHeader("📦", stringResource(R.string.sale_of_muassel), ShiftColors.PurpleLight, ShiftColors.Purple, ShiftColors.Purple)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NumberField(stringResource(R.string.muassel), muassel, { muassel = it }, Modifier.weight(1f), isInteger = true)
                            NumberField(stringResource(R.string.outdoor_muassel), outdoorMuassel, { outdoorMuassel = it }, Modifier.weight(1f), isInteger = true)
                        }

                        // ---- Due Sales (credit) ----
                        SectionHeader("📒", stringResource(R.string.due_sales), ShiftColors.DangerLight, ShiftColors.Danger, ShiftColors.Danger) {
                            modalType = ModalType.CREDIT
                        }
                        creditEntries.forEachIndexed { i, e ->
                            EntryRow(
                                left = "#${e.receiptNo}",
                                right = "%.2f ${stringResource(R.string.currency_unit)}".format(e.amount),
                                onRemove = { creditEntries.removeAt(i) }
                            )
                        }

                        // ---- Due Collection ----
                        SectionHeader("💵", stringResource(R.string.due_collection), ShiftColors.PurpleLight, ShiftColors.Purple, ShiftColors.Purple) {
                            modalType = ModalType.OLD_DUE
                        }
                        oldDueEntries.forEachIndexed { i, e ->
                            EntryRow(
                                left = "#${e.receiptNo}",
                                right = "%.2f ${stringResource(R.string.currency_unit)} (${e.type.name.lowercase()})".format(e.amount),
                                onRemove = { oldDueEntries.removeAt(i) }
                            )
                        }

                        // ---- Employer Advance ----
                        SectionHeader("👤", stringResource(R.string.employer_advance), ShiftColors.PurpleLight, ShiftColors.Purple, ShiftColors.Purple) {
                            modalType = ModalType.STAFF
                        }
                        staffEntries.forEachIndexed { i, e ->
                            EntryRow(
                                left = e.name,
                                right = "%.2f ${stringResource(R.string.currency_unit)} (${e.type.name.lowercase()})".format(e.amount),
                                onRemove = { staffEntries.removeAt(i) }
                            )
                        }

                        // ---- Walk-out ----
                        SectionHeader("🚪", stringResource(R.string.walk_out), ShiftColors.DangerLight, ShiftColors.Danger, ShiftColors.Danger) {
                            modalType = ModalType.WALKOUT
                        }
                        walkoutEntries.forEachIndexed { i, e ->
                            EntryRow(
                                left = e.description,
                                right = "%.2f ${stringResource(R.string.currency_unit)}".format(e.amount),
                                onRemove = { walkoutEntries.removeAt(i) }
                            )
                        }

                        // ---- Paid Out ----
                        SectionHeader("🛒", stringResource(R.string.paid_out), ShiftColors.DangerLight, ShiftColors.Danger, ShiftColors.Danger) {
                            modalType = ModalType.ITEM
                        }
                        itemEntries.forEachIndexed { i, e ->
                            EntryRow(
                                left = "${e.name} (${e.qty} pcs)",
                                right = "%.2f ${stringResource(R.string.currency_unit)}".format(e.total),
                                onRemove = { itemEntries.removeAt(i) }
                            )
                        }

                        Spacer(Modifier.height(24.dp))

                        // ---- Standard 4-Card International POS Reconciliation Summary ----
                        PosReconciliationSummary(
                            startingCash = startingCash,
                            cashSales = cash,
                            madaSales = mada,
                            digitalWalletSales = digitalWallet,
                            totalExpenses = expenses,
                            totalDueCredit = totalCredit,
                            totalDueCollectedCash = totalOldDueCash,
                            totalDueCollectedBank = totalOldDueBank,
                            totalStaffAdvanceCash = totalStaffCash,
                            totalStaffAdvanceBank = totalStaffBank,
                            totalWalkout = totalWalkout,
                            totalPurchasesCash = totalItems,
                            staffMealsCount = staffCount,
                            muasselCount = muassel.toIntOrNull() ?: 0,
                            outdoorMuasselCount = outdoorMuassel.toIntOrNull() ?: 0,
                            actualCashCount = actualCashCountInput,
                            onActualCashCountChange = { actualCashCountInput = it },
                            notes = notesInput,
                            onNotesChange = { notesInput = it }
                        )

                        Spacer(Modifier.height(24.dp))

                        // ---- Action buttons ----
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { showResetConfirm = true },
                                enabled = hasEnteredData,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ShiftColors.Danger,
                                    disabledContainerColor = ShiftColors.Danger.copy(alpha = 0.35f),
                                    contentColor = Color.White,
                                    disabledContentColor = Color.White.copy(alpha = 0.6f)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 46.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text("🧹", fontSize = 13.sp)
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(R.string.clean_data),
                                        color = if (hasEnteredData) Color.White else Color.White.copy(alpha = 0.6f),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    onSave(
                                        ShiftReportData(
                                            cashier = cashier,
                                            shift = shift,
                                            date = dateLabel,
                                            cashReceipts = cash,
                                            madaPayments = mada,
                                            digitalWallet = digitalWallet,
                                            startingCash = startingCash,
                                            actualCash = actualCashVal,
                                            notes = notesInput,
                                            staffCount = staffCount,
                                            totalExpenses = expenses,
                                            muassel = muassel.toIntOrNull() ?: 0,
                                            outdoorMuassel = outdoorMuassel.toIntOrNull() ?: 0,
                                            creditEntries = creditEntries.toList(),
                                            oldDueEntries = oldDueEntries.toList(),
                                            staffEntries = staffEntries.toList(),
                                            walkoutEntries = walkoutEntries.toList(),
                                            itemEntries = itemEntries.toList(),
                                            netCash = expectedCash,
                                            netMada = netMada,
                                            dateMillis = dateMillis
                                        )
                                    )
                                    resetAll()
                                },
                                enabled = canSave,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ShiftColors.SaveBtn,
                                    disabledContainerColor = ShiftColors.SaveBtn.copy(alpha = 0.35f),
                                    contentColor = Color.White,
                                    disabledContentColor = Color.White.copy(alpha = 0.6f)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 46.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text("💾", fontSize = 13.sp)
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(R.string.save_report),
                                        color = if (canSave) Color.White else Color.White.copy(alpha = 0.6f),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(80.dp)) // room for sticky bottom bar
        }
    }

    // ---- Reset confirmation ----
    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text(stringResource(R.string.clean_data)) },
            text = { Text(stringResource(R.string.confirm_clean_data)) },
            confirmButton = {
                TextButton(onClick = { resetAll(); showResetConfirm = false }) { Text(stringResource(R.string.yes), color = ShiftColors.Danger) }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text(stringResource(R.string.cancel), maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
        )
    }

    // ---- Date picker ----
    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { dateMillis = it }
                    showDatePicker = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel), maxLines = 1, overflow = TextOverflow.Ellipsis) } }
        ) { DatePicker(state = state) }
    }

    // ---- Add-entry modal ----
    if (modalType != ModalType.NONE) {
        AddEntryDialog(
            type = modalType,
            onDismiss = {
                focusManager.clearFocus()
                keyboardController?.hide()
                modalType = ModalType.NONE
            },
            onSubmit = { result ->
                focusManager.clearFocus()
                keyboardController?.hide()
                when (modalType) {
                    ModalType.CREDIT -> creditEntries.add(CreditEntry(result.receipt, result.amount))
                    ModalType.OLD_DUE -> oldDueEntries.add(OldDueEntry(result.receipt, result.amount, result.type))
                    ModalType.STAFF -> staffEntries.add(StaffEntry(result.name, result.amount, result.type))
                    ModalType.WALKOUT -> walkoutEntries.add(WalkoutEntry(result.name, result.amount))
                    ModalType.ITEM -> itemEntries.add(ItemEntry(result.name, result.qty, result.amount))
                    ModalType.NONE -> {}
                }
                modalType = ModalType.NONE
            }
        )
    }
}

/* ----------------------------------------------------------------------
 * TAB 5: LIVE CASHFLOW AUDIT & DRAWER MANAGEMENT
 * ---------------------------------------------------------------------- */
@Composable
private fun LiveCashflowTab(
    activeSession: ShiftSession?,
    cashMovements: List<CashMovement>,
    cashierOptions: List<String>,
    onOpenShift: (String, String, Double) -> Unit,
    onCloseShift: (ShiftSession, Double, String) -> Unit
) {
    var showOpenShiftModal by remember { mutableStateOf(false) }
    var showCloseShiftModal by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Active Shift Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header row with status indicator & title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (activeSession != null) Color(0xFFECFDF5) else ShiftColors.DangerLight,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (activeSession != null) Color(0xFFA7F3D0) else Color(0xFFFECACA)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            color = if (activeSession != null) ShiftColors.NetCashGreen else ShiftColors.Danger,
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                )
                                Text(
                                    text = if (activeSession != null) stringResource(R.string.drawer_open) else stringResource(R.string.drawer_closed),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (activeSession != null) ShiftColors.NetCashGreen else ShiftColors.Danger
                                )
                            }
                        }

                        Text(
                            text = stringResource(R.string.shift_drawer_status),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = ShiftColors.Charcoal
                        )
                    }
                }

                if (activeSession != null) {
                    // Information Container Box
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ShiftColors.Bg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, ShiftColors.Border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.cashier),
                                        fontSize = 11.sp,
                                        color = ShiftColors.TextMuted,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = activeSession.cashierName,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ShiftColors.Charcoal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.shift),
                                        fontSize = 11.sp,
                                        color = ShiftColors.TextMuted,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = activeSession.shiftName,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ShiftColors.Charcoal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Column(modifier = Modifier.weight(1.2f), horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = stringResource(R.string.starting_cash_float),
                                        fontSize = 11.sp,
                                        color = ShiftColors.TextMuted,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = "%.2f ${stringResource(R.string.currency_unit)}".format(activeSession.startingCash),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ShiftColors.NetCashGreen,
                                        maxLines = 1
                                    )
                                }
                            }

                            HorizontalDivider(color = ShiftColors.Border.copy(alpha = 0.6f))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = ShiftColors.TextMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "${stringResource(R.string.opened)}: ${dateFormat.format(Date(activeSession.openedAt))}",
                                    fontSize = 11.sp,
                                    color = ShiftColors.TextMuted,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }
                    }

                    // Prominent, beautiful Close Shift Action Button
                    Button(
                        onClick = { showCloseShiftModal = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ShiftColors.Danger,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 46.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.close_shift_session),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                } else {
                    Text(
                        text = stringResource(R.string.no_active_shift_msg),
                        fontSize = 13.sp,
                        color = ShiftColors.TextMuted,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Button(
                        onClick = { showOpenShiftModal = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ShiftColors.Primary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 46.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.open_shift_session),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Live Cash Audit & Summary Grid
        Text(
            text = stringResource(R.string.live_cashflow_analytics),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = ShiftColors.Charcoal
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            MetricCard(
                title = stringResource(R.string.total_cash_in),
                amount = "0.00 ${stringResource(R.string.currency_unit)}",
                icon = Icons.Default.TrendingUp,
                color = ShiftColors.NetCashGreen,
                bgColor = Color(0xFFECFDF5),
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = stringResource(R.string.total_cash_out),
                amount = "0.00 ${stringResource(R.string.currency_unit)}",
                icon = Icons.Default.TrendingDown,
                color = ShiftColors.Danger,
                bgColor = ShiftColors.DangerLight,
                modifier = Modifier.weight(1f)
            )
        }

        // Drawer Cash Movement Log
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.recent_drawer_movements),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = ShiftColors.Charcoal
                )
                Spacer(Modifier.height(10.dp))

                if (cashMovements.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_cash_movements),
                        fontSize = 12.sp,
                        color = ShiftColors.TextMuted,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    cashMovements.take(5).forEach { move ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (move.type == "PAY_IN") Color(0xFFECFDF5) else ShiftColors.DangerLight,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Icon(
                                            imageVector = if (move.type == "PAY_IN") Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                            contentDescription = null,
                                            tint = if (move.type == "PAY_IN") ShiftColors.NetCashGreen else ShiftColors.Danger,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = move.reason,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ShiftColors.Text,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${move.cashierName} • ${dateFormat.format(Date(move.timestamp))}",
                                        fontSize = 11.sp,
                                        color = ShiftColors.TextMuted
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (move.type == "PAY_IN") Color(0xFFECFDF5) else ShiftColors.DangerLight
                            ) {
                                Text(
                                    text = "${if (move.type == "PAY_IN") "+" else "-"}%.2f ${stringResource(R.string.currency_unit)}".format(move.amount),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (move.type == "PAY_IN") ShiftColors.NetCashGreen else ShiftColors.Danger,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    maxLines = 1
                                )
                            }
                        }
                        HorizontalDivider(color = ShiftColors.Border.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }

    // Modal: Open Shift
    if (showOpenShiftModal) {
        var selectedCashier by remember { mutableStateOf(cashierOptions.firstOrNull() ?: "Noora") }
        var selectedShift by remember { mutableStateOf("Day") }
        var startingCashText by remember { mutableStateOf("100.00") }

        AlertDialog(
            onDismissRequest = { showOpenShiftModal = false },
            shape = RoundedCornerShape(18.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = ShiftColors.Primary, modifier = Modifier.size(22.dp))
                    Text(
                        text = stringResource(R.string.open_shift_session),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                val dayShiftText = stringResource(R.string.shift_day)
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState()).imePadding()
                ) {
                    LabeledDropdown(
                        label = stringResource(R.string.cashier),
                        options = cashierOptions,
                        selected = selectedCashier,
                        onSelected = { selectedCashier = it }
                    )
                    LabeledDropdown(
                        label = stringResource(R.string.shift),
                        options = listOf(dayShiftText, stringResource(R.string.shift_night_option)),
                        selected = if (selectedShift == "Day") dayShiftText else stringResource(R.string.shift_night_option),
                        onSelected = { selectedShift = if (it == dayShiftText) "Day" else "Night" }
                    )
                    NumberField(
                        label = stringResource(R.string.starting_cash_float),
                        value = startingCashText,
                        onChange = { startingCashText = it }
                    )
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = ShiftColors.Primary),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    onClick = {
                        val cash = startingCashText.toDoubleOrNull() ?: 0.0
                        onOpenShift(selectedCashier, selectedShift, cash)
                        showOpenShiftModal = false
                    }
                ) {
                    Text(
                        text = stringResource(R.string.start_shift),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    onClick = { showOpenShiftModal = false }
                ) {
                    Text(
                        text = stringResource(R.string.cancel),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        )
    }

    // Modal: Close Shift & Generate Z-Report
    if (showCloseShiftModal && activeSession != null) {
        var actualCountText by remember { mutableStateOf("") }
        var notesText by remember { mutableStateOf("") }
        val currency = stringResource(R.string.currency_unit)
        val actualCount = actualCountText.toDoubleOrNull()
        val variance = if (actualCount != null) actualCount - activeSession.expectedCash else null

        AlertDialog(
            onDismissRequest = { showCloseShiftModal = false },
            shape = RoundedCornerShape(18.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = ShiftColors.Danger, modifier = Modifier.size(22.dp))
                    Text(
                        text = stringResource(R.string.close_shift_session),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState()).imePadding()
                ) {
                    // 1. Sales Summary Card
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF8FAFC),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = stringResource(R.string.sales_summary),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ShiftColors.Primary
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(stringResource(R.string.cash_sales), fontSize = 12.sp, color = ShiftColors.TextMuted)
                                Text("%.2f %s".format(activeSession.cashSales, currency), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(stringResource(R.string.mada_bank), fontSize = 12.sp, color = ShiftColors.TextMuted)
                                Text("%.2f %s".format(activeSession.cardSales, currency), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            if (activeSession.digitalSales > 0) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(stringResource(R.string.digital_wallet), fontSize = 12.sp, color = ShiftColors.TextMuted)
                                    Text("%.2f %s".format(activeSession.digitalSales, currency), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            HorizontalDivider(color = Color(0xFFE2E8F0), modifier = Modifier.padding(vertical = 2.dp))
                            val totalSales = activeSession.cashSales + activeSession.cardSales + activeSession.digitalSales
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(stringResource(R.string.total_sales), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("%.2f %s".format(totalSales, currency), fontSize = 12.5.sp, fontWeight = FontWeight.ExtraBold, color = ShiftColors.Primary)
                            }
                        }
                    }

                    // 2. Expected Cash Drawer Movement
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = ShiftColors.BrassLight,
                        border = androidx.compose.foundation.BorderStroke(1.dp, ShiftColors.Brass.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = stringResource(R.string.cash_in_drawer),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ShiftColors.Brass
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(stringResource(R.string.starting_cash), fontSize = 12.sp, color = ShiftColors.TextMuted)
                                Text("%.2f %s".format(activeSession.startingCash, currency), fontSize = 12.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("(+) ${stringResource(R.string.cash_sales)}", fontSize = 12.sp, color = ShiftColors.TextMuted)
                                Text("+%.2f %s".format(activeSession.cashSales, currency), fontSize = 12.sp)
                            }
                            if (activeSession.totalPayIn > 0) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("(+) ${stringResource(R.string.pay_in)}", fontSize = 12.sp, color = ShiftColors.TextMuted)
                                    Text("+%.2f %s".format(activeSession.totalPayIn, currency), fontSize = 12.sp)
                                }
                            }
                            if (activeSession.totalPayOut > 0) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("(-) ${stringResource(R.string.pay_out)}", fontSize = 12.sp, color = ShiftColors.TextMuted)
                                    Text("-%.2f %s".format(activeSession.totalPayOut, currency), fontSize = 12.sp)
                                }
                            }
                            HorizontalDivider(color = ShiftColors.Brass.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 2.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(stringResource(R.string.expected_cash_drawer, ""), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("%.2f %s".format(activeSession.expectedCash, currency), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = ShiftColors.NetCashGreen)
                            }
                        }
                    }

                    // 3. Actual Count Input
                    NumberField(
                        label = stringResource(R.string.actual_cash_counted),
                        value = actualCountText,
                        onChange = { actualCountText = it }
                    )

                    // 4. Live Variance Card
                    if (variance != null) {
                        val isBalanced = Math.abs(variance) < 0.001
                        val isOver = variance > 0
                        val varColor = when {
                            isBalanced -> Color(0xFF15803D)
                            isOver -> Color(0xFF047857)
                            else -> Color(0xFFDC2626)
                        }
                        val varBg = when {
                            isBalanced -> Color(0xFFF0FDF4)
                            isOver -> Color(0xFFECFDF5)
                            else -> Color(0xFFFEF2F2)
                        }
                        val statusLabel = when {
                            isBalanced -> stringResource(R.string.balanced)
                            isOver -> "OVER (+%.2f %s)".format(variance, currency)
                            else -> "SHORT (-%.2f %s)".format(Math.abs(variance), currency)
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = varBg,
                            border = androidx.compose.foundation.BorderStroke(1.dp, varColor.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.cash_variance),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = varColor
                                )
                                Text(
                                    text = statusLabel,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = varColor
                                )
                            }
                        }
                    }

                    // 5. Notes
                    LojiaMultilineTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        label = { Text(stringResource(R.string.shift_notes_variance)) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = ShiftColors.Danger),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    onClick = {
                        val count = actualCountText.toDoubleOrNull() ?: 0.0
                        onCloseShift(activeSession, count, notesText)
                        showCloseShiftModal = false
                    }
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.close_shift_session),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    onClick = { showCloseShiftModal = false }
                ) {
                    Text(
                        text = stringResource(R.string.cancel),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        )
    }
}

@Composable
fun MetricCard(
    title: String,
    amount: String,
    icon: ImageVector,
    color: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(title, fontSize = 11.sp, color = ShiftColors.TextMuted)
                Text(amount, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ShiftColors.Charcoal)
            }
        }
    }
}

/* ----------------------------------------------------------------------
 * TAB 6: SHIFT REPORT ARCHIVES VIEW
 * ---------------------------------------------------------------------- */
@Composable
private fun ShiftReportArchivesTab(
    reports: List<ShiftReport>,
    userProfile: com.lojia.shiftreport.data.UserProfile? = null,
    onPreviewPdf: (ShiftReport) -> Unit,
    onDeleteReport: (ShiftReport) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var reportToDelete by remember { mutableStateOf<ShiftReport?>(null) }

    val filteredReports = remember(reports, searchQuery) {
        if (searchQuery.isBlank()) reports
        else reports.filter {
            it.cashierName.contains(searchQuery, ignoreCase = true) ||
            it.shift.contains(searchQuery, ignoreCase = true) ||
            it.id.toString().contains(searchQuery)
        }
    }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Search Filter Bar with Card container
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, ShiftColors.Border),
            shadowElevation = 1.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp)
        ) {
            LojiaTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = stringResource(R.string.search_cashier_shift),
                        fontSize = 13.sp,
                        color = ShiftColors.TextMuted
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = ShiftColors.Primary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.btn_clear),
                                tint = ShiftColors.TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    cursorColor = ShiftColors.Primary
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (filteredReports.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, ShiftColors.Border),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = ShiftColors.Bg,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = ShiftColors.TextMuted
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = stringResource(R.string.no_archived_reports),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ShiftColors.Charcoal,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(filteredReports, key = { it.id }) { report ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ShiftColors.Border),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Header Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Cashier Avatar
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFFEFF6FF),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDBEAFE)),
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                            Text(
                                                text = report.cashierName.take(1).uppercase(),
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ShiftColors.Primary
                                            )
                                        }
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = report.cashierName,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ShiftColors.Charcoal,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = ShiftColors.BrassLight,
                                                border = androidx.compose.foundation.BorderStroke(1.dp, ShiftColors.Brass.copy(alpha = 0.25f))
                                            ) {
                                                val shiftDisplayName = when (report.shift.lowercase()) {
                                                    "morning" -> stringResource(R.string.shift_morning)
                                                    "evening" -> stringResource(R.string.shift_evening)
                                                    "night" -> stringResource(R.string.shift_night)
                                                    "day" -> stringResource(R.string.shift_day)
                                                    else -> report.shift
                                                }
                                                Text(
                                                    text = shiftDisplayName,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = ShiftColors.Brass,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AccessTime,
                                                contentDescription = null,
                                                tint = ShiftColors.TextMuted,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = dateFormat.format(Date(report.dateInMillis)),
                                                fontSize = 11.sp,
                                                color = ShiftColors.TextMuted
                                            )
                                        }
                                    }
                                }

                                // Report ID & Locked status
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFF1F5F9),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = null,
                                                tint = ShiftColors.TextMuted,
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Spacer(Modifier.width(3.dp))
                                            Text(
                                                text = "Z-REPORT",
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ShiftColors.TextMuted
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = ShiftColors.Bg,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, ShiftColors.Border)
                                    ) {
                                        Text(
                                            text = "#${report.id}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ShiftColors.TextMuted,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            // Financial Metrics Box
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = ShiftColors.Bg,
                                border = androidx.compose.foundation.BorderStroke(1.dp, ShiftColors.Border.copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp, horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(R.string.gross_revenue),
                                            fontSize = 11.sp,
                                            color = ShiftColors.TextMuted,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = "%.2f ${stringResource(R.string.currency_unit)}".format(report.totalSales),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ShiftColors.Text,
                                            maxLines = 1
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(R.string.mada_bank),
                                            fontSize = 11.sp,
                                            color = ShiftColors.TextMuted,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = "%.2f ${stringResource(R.string.currency_unit)}".format(report.madaPayments),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ShiftColors.NetMadaBlue,
                                            maxLines = 1
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1.1f), horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = stringResource(R.string.net_cash),
                                            fontSize = 11.sp,
                                            color = ShiftColors.TextMuted,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = "%.2f ${stringResource(R.string.currency_unit)}".format(report.netCash),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = ShiftColors.NetCashGreen,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }

                            // Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { reportToDelete = report },
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, ShiftColors.Danger.copy(alpha = 0.5f)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = ShiftColors.Danger,
                                        containerColor = ShiftColors.DangerLight.copy(alpha = 0.3f)
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.heightIn(min = 36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(R.string.delete),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }

                                Spacer(Modifier.width(10.dp))

                                Button(
                                    onClick = { onPreviewPdf(report) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = ShiftColors.SaveBtn),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                                    modifier = Modifier.heightIn(min = 36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PictureAsPdf,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp),
                                        tint = Color.White
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.preview_pdf),
                                        fontSize = 12.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    reportToDelete?.let { report ->
        SecureDeleteModal(
            title = stringResource(R.string.delete_shift_report_title),
            itemDescription = stringResource(R.string.delete_shift_report_desc, report.id.toString(), report.cashierName),
            userProfile = userProfile,
            onDismiss = { reportToDelete = null },
            onConfirmDelete = {
                onDeleteReport(report)
                reportToDelete = null
            }
        )
    }
}

/* ----------------------------------------------------------------------
 * SECTION HEADER (pill with tone color, optional "+ Add" button)
 * ---------------------------------------------------------------------- */
@Composable
private fun SectionHeader(
    icon: String,
    title: String,
    bg: Color,
    borderColor: Color,
    accent: Color,
    onAdd: (() -> Unit)? = null
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 6.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, borderColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 12.sp)
            Spacer(Modifier.width(6.dp))
            Text(title, color = ShiftColors.Charcoal, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
        }
        if (onAdd != null) {
            OutlinedButton(
                onClick = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    onAdd()
                },
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, accent),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = accent),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.defaultMinSize(minHeight = 28.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(2.dp))
                Text(stringResource(R.string.add), fontSize = 10.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
    }
}

/* ----------------------------------------------------------------------
 * DYNAMIC ENTRY ROW
 * ---------------------------------------------------------------------- */
@Composable
private fun EntryRow(left: String, right: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 5.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(ShiftColors.Bg)
            .border(1.dp, ShiftColors.Border, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(left, fontSize = 12.5.sp, color = ShiftColors.Text, modifier = Modifier.weight(1f))
        Text(right, fontSize = 12.5.sp, color = ShiftColors.Text, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(ShiftColors.DangerLight)
                .border(1.dp, Color(0xFFFECACA), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.remove), tint = ShiftColors.Danger, modifier = Modifier.size(12.dp))
            }
        }
    }
}

/* ----------------------------------------------------------------------
 * 4-CARD INTERNATIONAL POS RECONCILIATION SUMMARY (SQUARE/LOYVERSE STYLE)
 * ---------------------------------------------------------------------- */
@Composable
fun PosReconciliationSummary(
    startingCash: Double,
    cashSales: Double,
    madaSales: Double,
    digitalWalletSales: Double,
    totalExpenses: Double,
    totalDueCredit: Double,
    totalDueCollectedCash: Double,
    totalDueCollectedBank: Double,
    totalStaffAdvanceCash: Double,
    totalStaffAdvanceBank: Double,
    totalWalkout: Double,
    totalPurchasesCash: Double,
    staffMealsCount: Int,
    muasselCount: Int,
    outdoorMuasselCount: Int,
    actualCashCount: String,
    onActualCashCountChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit
) {
    val currency = stringResource(R.string.currency_unit)
    val totalSales = cashSales + madaSales + digitalWalletSales
    val totalCashIn = cashSales + totalDueCollectedCash
    val totalCashOut = totalExpenses + totalStaffAdvanceCash + totalPurchasesCash
    val expectedCash = startingCash + totalCashIn - totalCashOut
    val actualCountVal = actualCashCount.toDoubleOrNull()
    val variance = actualCountVal?.let { it - expectedCash }
    val netMada = madaSales + digitalWalletSales + totalDueCollectedBank - totalStaffAdvanceBank

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ==========================================
        // CARD 1: SALES SUMMARY (Payment Methods Only)
        // ==========================================
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, ShiftColors.Border),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(Modifier.fillMaxWidth().height(4.dp).background(ShiftColors.Brass))
            Column(Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🏷️", fontSize = 16.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            stringResource(R.string.sales_summary),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = ShiftColors.Charcoal
                        )
                    }
                    Surface(
                        color = ShiftColors.BrassLight,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Payment Methods Only",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ShiftColors.Brass,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                DashedDivider(Modifier.padding(vertical = 10.dp))

                ReconciliationRow(stringResource(R.string.cash_sales), cashSales, currency)
                ReconciliationRow(stringResource(R.string.mada_bank), madaSales, currency)
                if (digitalWalletSales > 0) {
                    ReconciliationRow(stringResource(R.string.digital_wallet), digitalWalletSales, currency)
                }

                DashedDivider(Modifier.padding(vertical = 8.dp), color = ShiftColors.Border)

                // TOTAL SALES (Prominent)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFFFBEB))
                        .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.total_sales),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = Color(0xFF92400E)
                    )
                    Text(
                        "%.2f %s".format(totalSales, currency),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = Color(0xFF92400E)
                    )
                }
            }
        }

        // ==========================================
        // CARD 2: CASH DRAWER RECONCILIATION
        // ==========================================
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, ShiftColors.Border),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(Modifier.fillMaxWidth().height(4.dp).background(ShiftColors.NetCashGreen))
            Column(Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("💵", fontSize = 16.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Cash Drawer Reconciliation",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = ShiftColors.Charcoal
                        )
                    }
                }

                DashedDivider(Modifier.padding(vertical = 10.dp))

                // Float / Inflow
                if (startingCash > 0) {
                    ReconciliationRow(stringResource(R.string.starting_cash), startingCash, currency)
                }
                ReconciliationRow("+ ${stringResource(R.string.cash_sales)}", cashSales, currency, valueColor = Color(0xFF047857))
                if (totalDueCollectedCash > 0) {
                    ReconciliationRow("+ ${stringResource(R.string.due_collected_cash)}", totalDueCollectedCash, currency, valueColor = Color(0xFF047857))
                }

                // Inflow subtotal
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.total_cash_in), fontSize = 11.5.sp, color = Color(0xFF065F46), fontWeight = FontWeight.SemiBold)
                    Text("+ %.2f %s".format(totalCashIn + startingCash, currency), fontSize = 12.sp, color = Color(0xFF065F46), fontWeight = FontWeight.Bold)
                }

                DashedDivider(Modifier.padding(vertical = 6.dp))

                // Outflow
                if (totalExpenses > 0) {
                    ReconciliationRow("- ${stringResource(R.string.op_expenses)}", totalExpenses, currency, valueColor = ShiftColors.Danger)
                }
                if (totalStaffAdvanceCash > 0) {
                    ReconciliationRow("- ${stringResource(R.string.employer_cash)}", totalStaffAdvanceCash, currency, valueColor = ShiftColors.Danger)
                }
                if (totalPurchasesCash > 0) {
                    ReconciliationRow("- ${stringResource(R.string.paid_out)}", totalPurchasesCash, currency, valueColor = ShiftColors.Danger)
                }

                // Outflow subtotal
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.total_cash_out), fontSize = 11.5.sp, color = Color(0xFFB91C1C), fontWeight = FontWeight.SemiBold)
                    Text("- %.2f %s".format(totalCashOut, currency), fontSize = 12.sp, color = Color(0xFFB91C1C), fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(8.dp))

                // EXPECTED CASH CALLOUT
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFECFDF5))
                        .border(1.dp, Color(0xFFA7F3D0), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                stringResource(R.string.cash_in_drawer),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF065F46)
                            )
                            Text(
                                "Starting Float + Inflows - Outflows",
                                fontSize = 9.5.sp,
                                color = Color(0xFF047857)
                            )
                        }
                        Text(
                            "%.2f %s".format(expectedCash, currency),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF065F46)
                        )
                    }
                }
            }
        }

        // ==========================================
        // CARD 3: ACTUAL COUNT & VARIANCE
        // ==========================================
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, ShiftColors.Border),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(Modifier.fillMaxWidth().height(4.dp).background(ShiftColors.Primary))
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⚖️", fontSize = 16.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.variance_over_short),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = ShiftColors.Charcoal
                    )
                }

                DashedDivider(Modifier.padding(vertical = 10.dp))

                // Actual Cash Input Field
                NumberField(
                    label = stringResource(R.string.actual_cash_count_currency, currency),
                    value = actualCashCount,
                    onChange = onActualCashCountChange,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(10.dp))

                // Variance Status Banner
                if (variance == null) {
                    Surface(
                        color = ShiftColors.Bg,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ShiftColors.Border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("ℹ️", fontSize = 14.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Enter physical cash in drawer to calculate variance",
                                fontSize = 11.5.sp,
                                color = ShiftColors.TextMuted
                            )
                        }
                    }
                } else {
                    val isBalanced = Math.abs(variance) < 0.001
                    val isOver = variance > 0
                    val bgColor = when {
                        isBalanced -> Color(0xFFF0FDF4)
                        isOver -> Color(0xFFECFDF5)
                        else -> Color(0xFFFEF2F2)
                    }
                    val borderColor = when {
                        isBalanced -> Color(0xFFBBF7D0)
                        isOver -> Color(0xFFA7F3D0)
                        else -> Color(0xFFFECACA)
                    }
                    val textColor = when {
                        isBalanced -> Color(0xFF15803D)
                        isOver -> Color(0xFF047857)
                        else -> Color(0xFFB91C1C)
                    }
                    val iconSymbol = when {
                        isBalanced -> "✓"
                        isOver -> "▲"
                        else -> "▼"
                    }
                    val statusText = when {
                        isBalanced -> stringResource(R.string.balanced)
                        isOver -> "Cash Over"
                        else -> "Cash Short"
                    }

                    Surface(
                        color = bgColor,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(iconSymbol, fontWeight = FontWeight.Bold, color = textColor, fontSize = 14.sp)
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        statusText,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = textColor
                                    )
                                }
                                Text(
                                    "${if (variance > 0) "+" else ""}${String.format(Locale.US, "%.2f", variance)} $currency",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = textColor
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Expected: %.2f %s • Actual: %.2f %s".format(
                                    expectedCash, currency, actualCountVal ?: 0.0, currency
                                ),
                                fontSize = 10.5.sp,
                                color = textColor.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // CARD 4: SECONDARY & OPERATIONAL TRACKING
        // ==========================================
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, ShiftColors.Border),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(Modifier.fillMaxWidth().height(4.dp).background(ShiftColors.Purple))
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📋", fontSize = 16.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Operational & Receivables Tracking",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = ShiftColors.Charcoal
                    )
                }

                DashedDivider(Modifier.padding(vertical = 10.dp))

                ReconciliationRow(
                    label = stringResource(R.string.due_sales_credit_entries),
                    value = totalDueCredit,
                    currency = currency,
                    note = "Tracked as Receivables • Excluded from sales & drawer"
                )

                if (staffMealsCount > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(stringResource(R.string.pdf_staff_meals), fontSize = 12.5.sp, color = ShiftColors.TextMuted, fontWeight = FontWeight.Medium)
                            Text("Complimentary • Non-revenue metric", fontSize = 9.5.sp, color = ShiftColors.TextMuted)
                        }
                        Text(stringResource(R.string.people_count, staffMealsCount), fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = ShiftColors.Text)
                    }
                }

                if (totalWalkout > 0) {
                    ReconciliationRow(
                        label = stringResource(R.string.walk_out),
                        value = totalWalkout,
                        currency = currency,
                        valueColor = ShiftColors.Danger
                    )
                }

                if (muasselCount > 0 || outdoorMuasselCount > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(R.string.muassel), fontSize = 12.5.sp, color = ShiftColors.TextMuted, fontWeight = FontWeight.Medium)
                        Text("${muasselCount + outdoorMuasselCount} pcs", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = ShiftColors.Purple)
                    }
                }

                // Optional Notes Input
                Spacer(Modifier.height(8.dp))
                FieldLabel(stringResource(R.string.closing_notes))
                Spacer(Modifier.height(4.dp))
                LojiaTextField(
                    value = notes,
                    onValueChange = onNotesChange,
                    placeholder = { Text(stringResource(R.string.closing_notes)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ReconciliationRow(
    label: String,
    value: Double,
    currency: String,
    bold: Boolean = false,
    valueColor: Color = ShiftColors.Text,
    note: String? = null
) {
    if (value <= 0) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(label, fontSize = 12.5.sp, color = ShiftColors.TextMuted, fontWeight = FontWeight.Medium)
            if (!note.isNullOrBlank()) {
                Text(note, fontSize = 9.5.sp, color = ShiftColors.TextMuted)
            }
        }
        Text(
            "%.2f %s".format(value, currency),
            fontSize = 13.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold,
            color = valueColor
        )
    }
}

/* ----------------------------------------------------------------------
 * LEGACY / RECEIPT SUMMARY OVERLOAD (for compatibility)
 * ---------------------------------------------------------------------- */
@Composable
private fun ReceiptSummary(
    cash: Double, mada: Double, gross: Double, expenses: Double,
    totalCredit: Double, totalOldDueCash: Double, totalOldDueBank: Double,
    totalStaffCash: Double, totalStaffBank: Double, totalWalkout: Double,
    totalItems: Double, netCash: Double, netMada: Double
) {
    PosReconciliationSummary(
        startingCash = 0.0,
        cashSales = cash,
        madaSales = mada,
        digitalWalletSales = 0.0,
        totalExpenses = expenses,
        totalDueCredit = totalCredit,
        totalDueCollectedCash = totalOldDueCash,
        totalDueCollectedBank = totalOldDueBank,
        totalStaffAdvanceCash = totalStaffCash,
        totalStaffAdvanceBank = totalStaffBank,
        totalWalkout = totalWalkout,
        totalPurchasesCash = totalItems,
        staffMealsCount = 0,
        muasselCount = 0,
        outdoorMuasselCount = 0,
        actualCashCount = "",
        onActualCashCountChange = {},
        notes = "",
        onNotesChange = {}
    )
}

@Composable
private fun DashedDivider(modifier: Modifier = Modifier, color: Color = ShiftColors.Border) {
    Canvas(modifier = modifier.fillMaxWidth().height(1.dp)) {
        drawLine(
            color = color,
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
        )
    }
}

/* ----------------------------------------------------------------------
 * STICKY BOTTOM BAR
 * ---------------------------------------------------------------------- */
@Composable
private fun StickySummaryBar(expectedCash: Double, variance: Double? = null) {
    val currency = stringResource(R.string.currency_unit)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .border(width = 1.dp, color = ShiftColors.Border)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(stringResource(R.string.cash_in_drawer), fontSize = 11.sp, color = ShiftColors.TextMuted, fontWeight = FontWeight.Medium)
            Text("%.2f %s".format(expectedCash, currency), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = ShiftColors.NetCashGreen)
        }
        if (variance != null) {
            val isOver = variance > 0
            val isBalanced = Math.abs(variance) < 0.001
            val textColor = when {
                isBalanced -> Color(0xFF15803D)
                isOver -> Color(0xFF047857)
                else -> Color(0xFFB91C1C)
            }
            val label = when {
                isBalanced -> stringResource(R.string.balanced)
                isOver -> "Over"
                else -> "Short"
            }
            Surface(
                color = when {
                    isBalanced -> Color(0xFFF0FDF4)
                    isOver -> Color(0xFFECFDF5)
                    else -> Color(0xFFFEF2F2)
                },
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, textColor.copy(alpha = 0.3f))
            ) {
                Text(
                    text = "$label: ${if (variance > 0) "+" else ""}${String.format(Locale.US, "%.2f", variance)} $currency",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

/* ----------------------------------------------------------------------
 * FORM PRIMITIVES
 * ---------------------------------------------------------------------- */
@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = ShiftColors.TextMuted,
        modifier = Modifier.padding(bottom = 3.dp),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    disabledContainerColor = Color(0xFFF8FAFC),
    focusedBorderColor = ShiftColors.Primary,
    unfocusedBorderColor = Color(0xFFCBD5E1),
    focusedLabelColor = ShiftColors.Primary,
    unfocusedLabelColor = Color(0xFF64748B)
)

@Composable
private fun NumberField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isInteger: Boolean = false
) {
    Column(modifier) {
        FieldLabel(label)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 46.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (enabled) Color.White else Color(0xFFF1F5F9))
                .border(
                    width = 1.dp,
                    color = if (enabled) Color(0xFFCBD5E1) else Color(0xFFE2E8F0),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = value,
                onValueChange = { new: String ->
                    val filtered = if (isInteger) {
                        new.filter { it.isDigit() }
                    } else {
                        var hasDot = false
                        buildString {
                            for (char in new) {
                                if (char.isDigit()) {
                                    append(char)
                                } else if (char == '.' || char == ',') {
                                    if (!hasDot) {
                                        append('.')
                                        hasDot = true
                                    }
                                }
                            }
                        }
                    }
                    onChange(filtered)
                },
                enabled = enabled,
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (enabled) ShiftColors.Charcoal else Color(0xFF94A3B8)
                ),
                keyboardOptions = KeyboardOptions(keyboardType = if (isInteger) KeyboardType.Number else KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = @Composable { innerTextField: @Composable () -> Unit ->
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (value.isEmpty()) {
                            Text(
                                text = if (isInteger) "0" else "0.00",
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LabeledDropdown(
    label: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        FieldLabel(label)
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            Box(
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selected,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = ShiftColors.Charcoal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
            }
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { opt ->
                    DropdownMenuItem(
                        text = { Text(opt, fontSize = 13.sp) },
                        onClick = {
                            onSelected(opt)
                            expanded = false
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

/* ----------------------------------------------------------------------
 * ADD-ENTRY MODAL (covers Credit / Old Due / Staff / Walkout / Item)
 * ---------------------------------------------------------------------- */
private data class ModalResult(
    val receipt: String = "",
    val name: String = "",
    val amount: Double = 0.0,
    val qty: Int = 1,
    val type: PayType = PayType.CASH
)

@Composable
private fun AddEntryDialog(
    type: ModalType,
    onDismiss: () -> Unit,
    onSubmit: (ModalResult) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var receipt by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var qtyText by remember { mutableStateOf("1") }
    var payType by remember { mutableStateOf(PayType.CASH) }
    var error by remember { mutableStateOf<String?>(null) }
    val invalidAmountError = stringResource(R.string.valid_amount_error)

    val title = when (type) {
        ModalType.CREDIT -> stringResource(R.string.add_due_sale_entry)
        ModalType.OLD_DUE -> stringResource(R.string.add_due_collection)
        ModalType.STAFF -> stringResource(R.string.add_employer_advance)
        ModalType.WALKOUT -> stringResource(R.string.add_walkout_bill)
        ModalType.ITEM -> stringResource(R.string.add_paid_out_entry)
        ModalType.NONE -> ""
    }

    AlertDialog(
        onDismissRequest = {
            focusManager.clearFocus()
            keyboardController?.hide()
            onDismiss()
        },
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()).imePadding()) {
                when (type) {
                    ModalType.CREDIT -> {
                        LojiaTextField(receipt, { receipt = it }, label = { Text(stringResource(R.string.receipt_number)) }, shape = RoundedCornerShape(10.dp), colors = fieldColors(), modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(10.dp))
                        LojiaTextField(amountText, { amountText = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text(stringResource(R.string.amount_with_currency, stringResource(R.string.currency_unit))) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), shape = RoundedCornerShape(10.dp), colors = fieldColors(), modifier = Modifier.fillMaxWidth())
                    }
                    ModalType.OLD_DUE -> {
                        LojiaTextField(receipt, { receipt = it }, label = { Text(stringResource(R.string.receipt_number)) }, shape = RoundedCornerShape(10.dp), colors = fieldColors(), modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(10.dp))
                        LojiaTextField(amountText, { amountText = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text(stringResource(R.string.amount_with_currency, stringResource(R.string.currency_unit))) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), shape = RoundedCornerShape(10.dp), colors = fieldColors(), modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(10.dp))
                        PayTypeSelector(payType) { payType = it }
                    }
                    ModalType.STAFF -> {
                        LojiaTextField(name, { name = it }, label = { Text(stringResource(R.string.name_description)) }, shape = RoundedCornerShape(10.dp), colors = fieldColors(), modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(10.dp))
                        LojiaTextField(amountText, { amountText = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text(stringResource(R.string.amount_with_currency, stringResource(R.string.currency_unit))) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), shape = RoundedCornerShape(10.dp), colors = fieldColors(), modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(10.dp))
                        PayTypeSelector(payType) { payType = it }
                    }
                    ModalType.WALKOUT -> {
                        LojiaTextField(name, { name = it }, label = { Text(stringResource(R.string.name_description)) }, shape = RoundedCornerShape(10.dp), colors = fieldColors(), modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(10.dp))
                        LojiaTextField(amountText, { amountText = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text(stringResource(R.string.amount_with_currency, stringResource(R.string.currency_unit))) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), shape = RoundedCornerShape(10.dp), colors = fieldColors(), modifier = Modifier.fillMaxWidth())
                    }
                    ModalType.ITEM -> {
                        LojiaTextField(name, { name = it }, label = { Text(stringResource(R.string.item_description)) }, shape = RoundedCornerShape(10.dp), colors = fieldColors(), modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(10.dp))
                        LojiaTextField(qtyText, { qtyText = it.filter { c -> c.isDigit() } }, label = { Text(stringResource(R.string.quantity)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(10.dp), colors = fieldColors(), modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(10.dp))
                        LojiaTextField(amountText, { amountText = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text(stringResource(R.string.price_with_currency, stringResource(R.string.currency_unit))) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), shape = RoundedCornerShape(10.dp), colors = fieldColors(), modifier = Modifier.fillMaxWidth())
                    }
                    ModalType.NONE -> {}
                }
                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = ShiftColors.Danger, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                colors = ButtonDefaults.buttonColors(containerColor = ShiftColors.Primary),
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (amount <= 0) {
                        error = invalidAmountError
                        return@Button
                    }
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    onSubmit(
                        ModalResult(
                            receipt = receipt.ifBlank { "Unspecified" },
                            name = name.ifBlank { "Unspecified" },
                            amount = amount,
                            qty = qtyText.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                            type = payType
                        )
                    )
                }
            ) { Text(stringResource(R.string.add), maxLines = 1, overflow = TextOverflow.Ellipsis) }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    onDismiss()
                }
            ) { Text(stringResource(R.string.cancel), maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
    )
}

@Composable
private fun PayTypeSelector(selected: PayType, onChange: (PayType) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.type), fontSize = 12.sp, color = ShiftColors.TextMuted, modifier = Modifier.padding(end = 12.dp))
        FilterChip(selected = selected == PayType.CASH, onClick = { onChange(PayType.CASH) }, label = { Text(stringResource(R.string.cash)) })
        Spacer(Modifier.width(8.dp))
        FilterChip(selected = selected == PayType.BANK, onClick = { onChange(PayType.BANK) }, label = { Text(stringResource(R.string.bank)) })
    }
}
