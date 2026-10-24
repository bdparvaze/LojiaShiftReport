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
            if (data.startingCash > java.math.BigDecimal.ZERO) {
                noteParts.add("Starting Float: %.2f".format(Locale.US, data.startingCash.toDouble()))
            }
            if (data.actualCash != null) {
                noteParts.add("Actual Cash Count: %.2f".format(Locale.US, data.actualCash.toDouble()))
            }
            if (data.notes.isNotBlank() && data.notes != "Saved from Shift Closing Ledger") {
                noteParts.add(data.notes)
            }
            val formattedNotes = if (noteParts.isNotEmpty()) noteParts.joinToString(" | ") else "Saved from Shift Closing Ledger"

            val report = ShiftReport(
                cashierName = data.cashier.ifBlank { "Standard Cashier" },
                shift = data.shift,
                dateInMillis = data.dateMillis,
                openingCash = MoneyFormat.toMinorUnits(if (data.openingCash > java.math.BigDecimal.ZERO) data.openingCash else data.startingCash),
                closingCash = MoneyFormat.toMinorUnits(if (data.closingCash > java.math.BigDecimal.ZERO) data.closingCash else (data.actualCash ?: java.math.BigDecimal.ZERO)),
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
                                Text(
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

    val totalCredit = creditEntries.sumOf { it.amountDouble }
    val totalOldDueCash = oldDueEntries.filter { it.type == PayType.CASH }.sumOf { it.amountDouble }
    val totalOldDueBank = oldDueEntries.filter { it.type == PayType.BANK }.sumOf { it.amountDouble }
    val totalStaffCash = staffEntries.filter { it.type == PayType.CASH }.sumOf { it.amountDouble }
    val totalStaffBank = staffEntries.filter { it.type == PayType.BANK }.sumOf { it.amountDouble }
    val totalWalkout = walkoutEntries.sumOf { it.amountDouble }
    val totalItems = itemEntries.sumOf { it.totalDouble }

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
            StickySummaryBar(
                expectedCash = MoneyFormat.toMinorUnits(expectedCash),
                variance = variance?.let { MoneyFormat.toMinorUnits(it) },
                currentCurrency = currentCurrency
            )
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
                                            .border(1.dp, OutlineLight, RoundedCornerShape(8.dp))
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
                            startingCash = MoneyFormat.toMinorUnits(startingCash),
                            cashSales = MoneyFormat.toMinorUnits(cash),
                            madaSales = MoneyFormat.toMinorUnits(mada),
                            digitalWalletSales = MoneyFormat.toMinorUnits(digitalWallet),
                            totalDiscounts = MoneyFormat.toMinorUnits(totalDiscounts),
                            salesReturns = MoneyFormat.toMinorUnits(salesReturns),
                            totalExpenses = MoneyFormat.toMinorUnits(expenses),
                            totalDueCredit = MoneyFormat.toMinorUnits(totalCredit),
                            totalDueCollectedCash = MoneyFormat.toMinorUnits(totalOldDueCash),
                            totalDueCollectedBank = MoneyFormat.toMinorUnits(totalOldDueBank),
                            totalStaffAdvanceCash = MoneyFormat.toMinorUnits(totalStaffCash),
                            totalStaffAdvanceBank = MoneyFormat.toMinorUnits(totalStaffBank),
                            totalWalkout = MoneyFormat.toMinorUnits(totalWalkout),
                            totalPurchasesCash = MoneyFormat.toMinorUnits(totalItems),
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
            titleContentColor = OnBackgroundLight,
            textContentColor = OnSurfaceVariantLight,
            title = { Text(stringResource(R.string.clean_data), color = OnBackgroundLight, fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.confirm_clean_data), color = OnSurfaceVariantLight) },
            confirmButton = {
                TextButton(onClick = { resetAll(); showResetConfirm = false }) { Text(stringResource(R.string.yes), color = ShiftColors.Danger, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text(stringResource(R.string.cancel), color = OnSurfaceVariantLight, maxLines = 1, overflow = TextOverflow.Ellipsis) }
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
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel), color = OnSurfaceVariantLight, maxLines = 1, overflow = TextOverflow.Ellipsis) } }
        ) {
            DatePicker(
                state = state,
                colors = DatePickerDefaults.colors(
                    containerColor = Color.White,
                    titleContentColor = OnBackgroundLight,
                    headlineContentColor = OnBackgroundLight,
                    weekdayContentColor = OnSurfaceVariantLight,
                    subheadContentColor = OnBackgroundLight,
                    dayContentColor = OnBackgroundLight,
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
                            unitPriceDouble = result.unitPrice,
                            totalDouble = result.amount
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
            titleContentColor = OnBackgroundLight,
            textContentColor = OnSurfaceVariantLight,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🛡️", fontSize = 18.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.manager_sign_off_title), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = OnBackgroundLight)
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

