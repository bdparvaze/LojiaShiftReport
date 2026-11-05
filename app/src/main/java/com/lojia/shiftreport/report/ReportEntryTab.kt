package com.lojia.shiftreport.report

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojia.shiftreport.R
import com.lojia.shiftreport.data.PreferencesRepository
import com.lojia.shiftreport.ui.theme.*
import com.lojia.shiftreport.util.MoneyFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

/**
 * TAB 0: REPORT ENTRY (SHIFT CLOSING LEDGER FORM)
 * Modularized out of ShiftReportScreen for clean architectural separation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportEntryTab(
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

    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = Color.White,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp)
                    .verticalScroll(scrollState)
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .testTag("shift_report_dashboard"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
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

                        // ---- Employer Advance (Dropdown-only Foodics POS Standard) ----
                        val context = LocalContext.current
                        val coroutineScope = rememberCoroutineScope()
                        val db = remember(context) { com.lojia.shiftreport.data.AppDatabase.getInstance(context) }
                        val employeeDao = remember(db) { db.employeeDao() }
                        val advanceDao = remember(db) { db.employerAdvanceDao() }
                        val prefs = remember(context) { PreferencesRepository.getInstance(context) }
                        val adminId = remember(prefs) { prefs.getSavedUsername().ifBlank { "admin" } }

                        val activeEmployees by employeeDao.getAllActive().collectAsState(initial = emptyList())
                        val allAdvances by advanceDao.getAllActive().collectAsState(initial = emptyList())
                        val advancesByEmployee = remember(allAdvances) {
                            allAdvances.groupBy { it.employeeId }.mapValues { (_, list) -> list.sumOf { it.amount } }
                        }

                        var selectedAdvanceEmployee by remember { mutableStateOf<com.lojia.shiftreport.data.Employee?>(null) }
                        var advanceAmountText by remember { mutableStateOf("") }
                        var advancePaymentType by remember { mutableStateOf("Cash") }
                        var showAdminAuthForAdvance by remember { mutableStateOf(false) }

                        Text(
                            text = "👤 " + stringResource(R.string.employer_advance),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = ShiftColors.Purple,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 4.dp)
                        )

                        EmployerAdvanceSection(
                            employees = activeEmployees,
                            advancesByEmployee = advancesByEmployee,
                            selectedEmployee = selectedAdvanceEmployee,
                            onEmployeeSelected = { selectedAdvanceEmployee = it },
                            amount = advanceAmountText,
                            onAmountChange = { advanceAmountText = it },
                            selectedType = advancePaymentType,
                            onTypeChange = { advancePaymentType = it },
                            onAddAdvance = {
                                val amt = advanceAmountText.trim().toDoubleOrNull()
                                if (amt == null || amt <= 0.0 || amt > 5000.0) {
                                    android.widget.Toast.makeText(
                                        context,
                                        "Enter valid amount (1 - 5000 SAR)",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    showAdminAuthForAdvance = true
                                }
                            }
                        )

                        if (showAdminAuthForAdvance && selectedAdvanceEmployee != null) {
                            val emp = selectedAdvanceEmployee!!
                            val amt = advanceAmountText.trim().toDoubleOrNull() ?: 0.0
                            val isBank = advancePaymentType == "Bank"
                            com.lojia.shiftreport.auth.AdminAuthDialog(
                                onDismissRequest = { showAdminAuthForAdvance = false },
                                onAuthSuccess = {
                                    showAdminAuthForAdvance = false
                                    coroutineScope.launch {
                                        advanceDao.insert(
                                            com.lojia.shiftreport.data.EmployerAdvanceTransaction(
                                                employeeId = emp.id,
                                                employeeName = emp.name,
                                                amount = amt,
                                                paymentMethod = if (isBank) "BANK" else "CASH",
                                                type = com.lojia.shiftreport.data.AdvanceTransactionType.ADVANCE.name,
                                                createdByAdminId = adminId
                                            )
                                        )
                                        staffEntries.add(
                                            StaffEntry(
                                                name = emp.name,
                                                amount = java.math.BigDecimal.valueOf(amt),
                                                type = if (isBank) PayType.BANK else PayType.CASH
                                            )
                                        )
                                        advanceAmountText = ""
                                        android.widget.Toast.makeText(
                                            context,
                                            "Added $amt SAR to ${emp.name}",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            )
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
                                            isLocked = false,
                                            managerSignedBy = null,
                                            managerSignTime = null,
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployerAdvanceSection(
    employees: List<com.lojia.shiftreport.data.Employee>,
    advancesByEmployee: Map<String, Double> = emptyMap(),
    selectedEmployee: com.lojia.shiftreport.data.Employee?,
    onEmployeeSelected: (com.lojia.shiftreport.data.Employee) -> Unit,
    amount: String,
    onAmountChange: (String) -> Unit,
    selectedType: String,
    onTypeChange: (String) -> Unit,
    onAddAdvance: () -> Unit,
    onNavigateToSettings: () -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedEmployee?.name ?: "Select Employee",
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null
                    )
                },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
                label = { Text("Employee") },
                placeholder = { Text("Select from ${employees.size} employees") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                if (employees.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("No employees - Add in Settings > Employees") },
                        onClick = {
                            expanded = false
                            onNavigateToSettings()
                        }
                    )
                } else {
                    employees.forEach { employee ->
                        val taken = advancesByEmployee[employee.id] ?: 0.0
                        val balance = employee.monthlySalary - taken
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = employee.name,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = String.format(
                                            Locale.US,
                                            "Salary: %.0f SAR | Balance: %.2f SAR",
                                            employee.monthlySalary,
                                            balance
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray
                                    )
                                }
                            },
                            onClick = {
                                onEmployeeSelected(employee)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        OutlinedTextField(
            value = amount,
            onValueChange = onAmountChange,
            label = { Text("Advance Amount") },
            enabled = selectedEmployee != null,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
            ),
            suffix = { Text("SAR") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = selectedType == "Cash",
                onClick = { onTypeChange("Cash") },
                label = { Text("Cash") }
            )
            FilterChip(
                selected = selectedType == "Bank",
                onClick = { onTypeChange("Bank") },
                label = { Text("Bank") }
            )
        }

        Button(
            onClick = onAddAdvance,
            enabled = selectedEmployee != null && amount.isNotEmpty(),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Add ${amount.ifBlank { "0" }} SAR to ${selectedEmployee?.name ?: "Employee"}",
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
