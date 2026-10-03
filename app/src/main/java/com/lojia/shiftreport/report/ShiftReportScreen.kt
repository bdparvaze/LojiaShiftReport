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
import androidx.compose.foundation.BorderStroke


import androidx.compose.foundation.background


import androidx.compose.foundation.border


import androidx.compose.foundation.clickable


import androidx.compose.foundation.layout.*


import androidx.compose.foundation.lazy.LazyColumn


import androidx.compose.foundation.lazy.items


import androidx.compose.foundation.rememberScrollState


import androidx.compose.foundation.shape.CircleShape
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


import java.text.NumberFormat
import java.text.SimpleDateFormat

import java.util.*


import androidx.compose.ui.res.stringResource


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
    val currentCurrency by viewModel.currentCurrency.collectAsState()
    val businessProfile by viewModel.businessProfile.collectAsState()

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
        currentCurrency = currentCurrency,
        onPreviewPdf = onPreviewPdf,
        onSaveReportData = { data ->
            val dueJson = org.json.JSONArray().apply {
                data.creditEntries.forEach { e ->
                    put(org.json.JSONObject().apply {
                        put("receiptNo", e.receiptNo)
                        put("customerName", "Receipt #${e.receiptNo}")
                        put("amount", MoneyFormat.toMinorUnits(e.amount))
                    })
                }
            }.toString()

            val prevDueJson = org.json.JSONArray().apply {
                data.oldDueEntries.forEach { e ->
                    put(org.json.JSONObject().apply {
                        put("receiptNo", e.receiptNo)
                        put("customerName", "Receipt #${e.receiptNo}")
                        put("amount", MoneyFormat.toMinorUnits(e.amount))
                        put("paymentMode", e.type.name)
                    })
                }
            }.toString()

            val staffAdvJson = org.json.JSONArray().apply {
                data.staffEntries.forEach { e ->
                    put(org.json.JSONObject().apply {
                        put("staffName", e.name)
                        put("amount", MoneyFormat.toMinorUnits(e.amount))
                        put("paymentMode", e.type.name)
                    })
                }
            }.toString()

            val walkoutJson = org.json.JSONArray().apply {
                data.walkoutEntries.forEach { e ->
                    put(org.json.JSONObject().apply {
                        put("tableOrOrderRef", e.description)
                        put("amount", MoneyFormat.toMinorUnits(e.amount))
                    })
                }
            }.toString()

            val itemsJson = org.json.JSONArray().apply {
                data.itemEntries.forEach { e ->
                    put(org.json.JSONObject().apply {
                        put("itemName", e.name)
                        put("quantity", e.qty.toDouble())
                        put("unitPrice", MoneyFormat.toMinorUnits(e.unitPrice))
                        put("totalAmount", MoneyFormat.toMinorUnits(e.total))
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
                openingCash = MoneyFormat.toMinorUnits(if (data.openingCash > 0) data.openingCash else data.startingCash),
                closingCash = MoneyFormat.toMinorUnits(if (data.closingCash > 0) data.closingCash else (data.actualCash ?: 0.0)),
                grossCash = MoneyFormat.toMinorUnits(data.cashReceipts),
                madaPayments = MoneyFormat.toMinorUnits(data.madaPayments),
                digitalWallet = MoneyFormat.toMinorUnits(data.digitalWallet),
                totalDiscounts = MoneyFormat.toMinorUnits(data.totalDiscounts),
                salesReturns = MoneyFormat.toMinorUnits(data.salesReturns),
                staffMealsCount = data.staffCount,
                totalExpenses = MoneyFormat.toMinorUnits(data.totalExpenses),
                muasselQty = data.muassel.toDouble(),
                outdoorShishaQty = data.outdoorMuassel.toDouble(),
                dueCreditEntriesJson = dueJson,
                previousDueCollectionsJson = prevDueJson,
                staffAdvancesJson = staffAdvJson,
                unpaidBillsJson = walkoutJson,
                purchasedItemsJson = itemsJson,
                notes = formattedNotes,
                isLocked = data.isLocked,
                managerSignedBy = data.managerSignedBy,
                managerSignTime = data.managerSignTime
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
    currentCurrency: String = MoneyFormat.DEFAULT_CURRENCY_CODE,
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
            .background(Color.White)
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                })
            }
    ) {
        // ---- SCROLLABLE OPTIONS TAB ROW ----
        Surface(
            color = SurfaceLight,
            shadowElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = SurfaceLight,
                contentColor = PrimaryIndigoDark,
                edgePadding = 8.dp,
                modifier = Modifier.height(44.dp),
                indicator = { tabPositions ->
                    Box(
                        Modifier
                            .tabIndicatorOffset(tabPositions[selectedTab])
                            .height(3.dp)
                            .background(
                                color = PrimaryIndigoLight,
                                shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
                            )
                    )
                },
                divider = { HorizontalDivider(color = OutlineLight) }
            ) {
                tabTitles.forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    Tab(
                        selected = isSelected,
                        modifier = Modifier.height(44.dp),
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
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isSelected) PrimaryIndigoDark else OnSurfaceVariantLight
                                )
                                Spacer(Modifier.width(5.dp))
                                AutoText(
                                    text = title,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) PrimaryIndigoDark else OnSurfaceVariantLight,
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
                    currentCurrency = currentCurrency,
                    onSave = onSaveReportData
                )
                1 -> DueLedgerTab(
                    reports = shiftReports,
                    currency = currentCurrency
                )
                2 -> EmployerLedgerTab(
                    reports = shiftReports,
                    currency = currentCurrency
                )
                3 -> PaidOutShoppingLedgerTab(
                    reports = shiftReports,
                    currency = currentCurrency
                )
                4 -> WalkoutLedgerTab(
                    reports = shiftReports,
                    currency = currentCurrency
                )
                5 -> ShiftReportArchivesTab(
                    reports = shiftReports,
                    userProfile = userProfile,
                    currentCurrency = currentCurrency,
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
    currentCurrency: String = MoneyFormat.DEFAULT_CURRENCY_CODE,
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
    var totalDiscountsInput by rememberSaveable { mutableStateOf("") }
    var salesReturnsInput by rememberSaveable { mutableStateOf("") }
    var actualCashCountInput by rememberSaveable { mutableStateOf("") }
    var notesInput by rememberSaveable { mutableStateOf("") }

    var isShiftLocked by rememberSaveable { mutableStateOf(false) }
    var managerSignedBy by rememberSaveable { mutableStateOf<String?>(null) }
    var managerSignTime by rememberSaveable { mutableStateOf<Long?>(null) }
    var showManagerLockDialog by rememberSaveable { mutableStateOf(false) }

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
    val totalDiscounts = d(totalDiscountsInput)
    val salesReturns = d(salesReturnsInput)
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
    val netSales = (totalSales - totalDiscounts - salesReturns).coerceAtLeast(0.0)
    val totalCashIn = startingCash + cash + totalOldDueCash
    val totalCashOut = expenses + totalStaffCash + totalItems
    val expectedCash = totalCashIn - totalCashOut
    val variance = actualCashVal?.let { it - expectedCash }
    val netMada = mada + digitalWallet + totalOldDueBank - totalStaffBank

    val hasEnteredData = cashier.isNotBlank() || startingCashInput.isNotBlank() ||
            cashReceipts.isNotBlank() || madaPayments.isNotBlank() || digitalWalletInput.isNotBlank() ||
            totalDiscountsInput.isNotBlank() || salesReturnsInput.isNotBlank() ||
            actualCashCountInput.isNotBlank() || notesInput.isNotBlank() ||
            totalExpenses.isNotBlank() || staffCount > 0 ||
            muassel.isNotBlank() || outdoorMuassel.isNotBlank() ||
            creditEntries.isNotEmpty() || oldDueEntries.isNotEmpty() ||
            staffEntries.isNotEmpty() || walkoutEntries.isNotEmpty() ||
            itemEntries.isNotEmpty()

    val canSave = cashier.isNotBlank() && (
            cash > 0.0 || mada > 0.0 || digitalWallet > 0.0 || expenses > 0.0 || startingCash > 0.0 ||
            totalDiscounts > 0.0 || salesReturns > 0.0 ||
            (muassel.toIntOrNull() ?: 0) > 0 || (outdoorMuassel.toIntOrNull() ?: 0) > 0 ||
            creditEntries.isNotEmpty() || oldDueEntries.isNotEmpty() ||
            staffEntries.isNotEmpty() || walkoutEntries.isNotEmpty() ||
            itemEntries.isNotEmpty()
    )

    fun resetAll() {
        cashier = ""; shift = "Day"; dateMillis = System.currentTimeMillis()
        startingCashInput = ""; cashReceipts = ""; madaPayments = ""; digitalWalletInput = ""
        totalDiscountsInput = ""; salesReturnsInput = ""
        actualCashCountInput = ""; notesInput = ""
        isShiftLocked = false; managerSignedBy = null; managerSignTime = null
        staffCount = 0; totalExpenses = ""
        muassel = ""; outdoorMuassel = ""
        creditEntries.clear(); oldDueEntries.clear(); staffEntries.clear()
        walkoutEntries.clear(); itemEntries.clear()
    }

    val dateLabel = remember(dateMillis) {
        SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date(dateMillis))
    }

    Scaffold(
        containerColor = Color.White,
        bottomBar = {
            StickySummaryBar(expectedCash = expectedCash, variance = variance, currentCurrency = currentCurrency)
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(padding)
                .imePadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(Alignment.CenterVertically)
                    .widthIn(max = 680.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .testTag("shift_report_dashboard"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
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
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NumberField(stringResource(R.string.total_discounts_label), totalDiscountsInput, { totalDiscountsInput = it }, Modifier.weight(1f))
                            NumberField(stringResource(R.string.sales_returns_label), salesReturnsInput, { salesReturnsInput = it }, Modifier.weight(1f))
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
                                right = "%.2f $currentCurrency".format(e.amount),
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
                                right = "%.2f $currentCurrency (${e.type.name.lowercase()})".format(e.amount),
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
                                right = "%.2f $currentCurrency (${e.type.name.lowercase()})".format(e.amount),
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
                                right = "%.2f $currentCurrency".format(e.amount),
                                onRemove = { walkoutEntries.removeAt(i) }
                            )
                        }

                        // ---- Paid Out ----
                        SectionHeader("🛒", stringResource(R.string.paid_out), ShiftColors.DangerLight, ShiftColors.Danger, ShiftColors.Danger) {
                            modalType = ModalType.ITEM
                        }
                        itemEntries.forEachIndexed { i, e ->
                            val leftText = if (e.qty > 0) "${e.name} (${e.qty} pcs)" else e.name
                            EntryRow(
                                left = leftText,
                                right = "%.2f $currentCurrency".format(e.total),
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
                            totalDiscounts = totalDiscounts,
                            salesReturns = salesReturns,
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
                            onNotesChange = { notesInput = it },
                            currentCurrency = currentCurrency
                        )

                        Spacer(Modifier.height(16.dp))

                        // ---- Enterprise Manager Sign-Off & Lock Card ----
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isShiftLocked) SuccessContainer else BackgroundLight
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isShiftLocked) PosCashGreen else ShiftColors.Border
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(if (isShiftLocked) "🔒" else "🛡️", fontSize = 16.sp)
                                        Spacer(Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = stringResource(R.string.manager_sign_off_title),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.5.sp,
                                                color = ShiftColors.Charcoal
                                            )
                                            Text(
                                                text = if (isShiftLocked) "Approved by ${managerSignedBy ?: "Manager"}" else "Optional audit lock & verification",
                                                fontSize = 11.sp,
                                                color = if (isShiftLocked) PosCashGreen else ShiftColors.TextMuted
                                            )
                                        }
                                    }

                                    Surface(
                                        color = if (isShiftLocked) SuccessContainer else SurfaceVariantLight,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = stringResource(if (isShiftLocked) R.string.shift_locked_badge else R.string.shift_draft_badge),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isShiftLocked) PosCashGreen else OnSurfaceVariantLight,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Spacer(Modifier.height(8.dp))

                                if (!isShiftLocked) {
                                    OutlinedButton(
                                        onClick = { showManagerLockDialog = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, ShiftColors.Primary),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ShiftColors.Primary)
                                    ) {
                                        Text("🔐 " + stringResource(R.string.lock_and_finalize_btn), fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
                                    }
                                } else {
                                    Text(
                                        text = "✓ Shift report is verified and locked against edits.",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = PosCashGreen
                                    )
                                }
                            }
                        }

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
                                            openingCash = startingCash,
                                            closingCash = actualCashVal ?: 0.0,
                                            totalDiscounts = totalDiscounts,
                                            salesReturns = salesReturns,
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
                                            isLocked = isShiftLocked,
                                            managerSignedBy = managerSignedBy,
                                            managerSignTime = managerSignTime,
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
}

    // ---- Reset confirmation ----
    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            containerColor = Color.White,
            titleContentColor = Color(0xFF0F172A),
            textContentColor = Color(0xFF1E293B),
            title = { Text(stringResource(R.string.clean_data), color = Color(0xFF0F172A), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.confirm_clean_data), color = Color(0xFF1E293B)) },
            confirmButton = {
                TextButton(onClick = { resetAll(); showResetConfirm = false }) { Text(stringResource(R.string.yes), color = ShiftColors.Danger, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text(stringResource(R.string.cancel), color = Color(0xFF475569), maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
        )
    }

    // ---- Date picker ----
    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            colors = DatePickerDefaults.colors(containerColor = Color.White),
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { dateMillis = it }
                    showDatePicker = false
                }) { Text(stringResource(R.string.ok), color = ShiftColors.Primary, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel), color = Color(0xFF475569), maxLines = 1, overflow = TextOverflow.Ellipsis) } }
        ) {
            DatePicker(
                state = state,
                colors = DatePickerDefaults.colors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF0F172A),
                    headlineContentColor = Color(0xFF0F172A),
                    weekdayContentColor = Color(0xFF475569),
                    subheadContentColor = Color(0xFF0F172A),
                    dayContentColor = Color(0xFF0F172A),
                    selectedDayContainerColor = ShiftColors.Primary,
                    selectedDayContentColor = Color.White
                )
            )
        }
    }

    // ---- Add-entry modal ----
    if (modalType != ModalType.NONE) {
        AddEntryDialog(
            type = modalType,
            currentCurrency = currentCurrency,
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
                    ModalType.ITEM -> itemEntries.add(
                        ItemEntry(
                            name = result.name,
                            qty = result.qty,
                            unitPrice = result.unitPrice,
                            total = result.amount
                        )
                    )
                    ModalType.NONE -> {}
                }
                modalType = ModalType.NONE
            }
        )
    }

    // ---- Manager Lock / Sign-off Dialog ----
    if (showManagerLockDialog) {
        val prefs = PreferencesRepository.getInstance(LocalContext.current)
        var mgrName by remember { mutableStateOf("Shift Manager") }
        var mgrPin by remember { mutableStateOf("") }
        var pinError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showManagerLockDialog = false },
            containerColor = Color.White,
            titleContentColor = Color(0xFF0F172A),
            textContentColor = Color(0xFF1E293B),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🛡️", fontSize = 18.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.manager_sign_off_title), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        stringResource(R.string.manager_sign_off_desc),
                        fontSize = 12.5.sp,
                        color = ShiftColors.TextMuted
                    )
                    LojiaTextField(
                        value = mgrName,
                        onValueChange = { mgrName = it },
                        label = { Text(stringResource(R.string.manager_name_label)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    LojiaTextField(
                        value = mgrPin,
                        onValueChange = { mgrPin = it; pinError = null },
                        label = { Text(stringResource(R.string.manager_pin_label)) },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                        ),
                        isError = pinError != null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (pinError != null) {
                        Text(pinError ?: "", color = ShiftColors.Danger, fontSize = 11.5.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val isPinValid = if (prefs.hasPinConfigured() || prefs.isQuickLoginEnabled()) {
                            prefs.verifyPin(mgrPin)
                        } else {
                            mgrPin.length >= 4
                        }
                        if (isPinValid) {
                            isShiftLocked = true
                            managerSignedBy = mgrName.ifBlank { "Shift Manager" }
                            managerSignTime = System.currentTimeMillis()
                            showManagerLockDialog = false
                        } else {
                            pinError = "Invalid Manager PIN. Please verify credentials."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ShiftColors.Primary)
                ) {
                    Text(stringResource(R.string.lock_and_finalize_btn), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showManagerLockDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
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
    currentCurrency: String = MoneyFormat.DEFAULT_CURRENCY_CODE,
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
                                        text = "%.2f $currentCurrency".format(activeSession.startingCash),
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
                amount = "0.00 $currentCurrency",
                icon = Icons.Default.TrendingUp,
                color = ShiftColors.NetCashGreen,
                bgColor = Color(0xFFECFDF5),
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = stringResource(R.string.total_cash_out),
                amount = "0.00 $currentCurrency",
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
                                    text = "${if (move.type == "PAY_IN") "+" else "-"}%.2f $currentCurrency".format(move.amount),
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
            containerColor = Color.White,
            titleContentColor = Color(0xFF0F172A),
            textContentColor = Color(0xFF1E293B),
            shape = RoundedCornerShape(18.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = ShiftColors.Primary, modifier = Modifier.size(22.dp))
                    Text(
                        text = stringResource(R.string.open_shift_session),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF0F172A)
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
        val currency = currentCurrency
        val actualCount = actualCountText.toDoubleOrNull()
        val variance = if (actualCount != null) actualCount - activeSession.expectedCash else null

        AlertDialog(
            onDismissRequest = { showCloseShiftModal = false },
            containerColor = Color.White,
            titleContentColor = Color(0xFF0F172A),
            textContentColor = Color(0xFF1E293B),
            shape = RoundedCornerShape(18.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = ShiftColors.Danger, modifier = Modifier.size(22.dp))
                    Text(
                        text = stringResource(R.string.close_shift_session),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF0F172A)
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
                            isBalanced -> PosCashGreen
                            isOver -> PosCashGreen
                            else -> PosShortageRed
                        }
                        val varBg = when {
                            isBalanced -> SuccessContainer
                            isOver -> SuccessContainer
                            else -> ErrorContainerLight
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
