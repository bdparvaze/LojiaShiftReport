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
import com.lojia.shiftreport.data.ShiftReport
import com.lojia.shiftreport.data.StaffAdvanceItem
import com.lojia.shiftreport.ui.theme.*
import com.lojia.shiftreport.util.EmployeeAdvanceSummary
import com.lojia.shiftreport.util.MoneyFormat
import com.lojia.shiftreport.util.MoneyInput
import com.lojia.shiftreport.util.advancesInMonthMinor
import com.lojia.shiftreport.util.balanceMinor
import com.lojia.shiftreport.util.classifyMoneyInput
import com.lojia.shiftreport.util.currentMonthRange
import com.lojia.shiftreport.util.isUnspecifiedReceipt
import com.lojia.shiftreport.util.normalizeReceiptNumber
import com.lojia.shiftreport.util.normalizeRestoreName
import java.text.SimpleDateFormat
import java.time.ZoneId
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
    shiftReports: List<ShiftReport> = emptyList(),
    formState: ShiftFormState = ShiftFormState(),
    formActions: ShiftFormActions = ShiftFormActions(),
    onSave: (ShiftReportData) -> Unit
) {
    val shiftOptions = listOf(stringResource(R.string.shift_day), stringResource(R.string.shift_night_option))

    val cashier = formState.cashier
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val shift = formState.shift
    val dateMillis = formState.dateMillis

    val startingCashInput = formState.startingCashInput
    val cashReceipts = formState.cashReceipts
    val madaPayments = formState.madaPayments
    val digitalWalletInput = formState.digitalWalletInput
    val totalDiscountsInput = formState.totalDiscountsInput
    val salesReturnsInput = formState.salesReturnsInput
    val actualCashCountInput = formState.actualCashCountInput
    val notesInput = formState.notesInput

    val staffCount = formState.staffCount
    val totalExpenses = formState.totalExpenses

    val muassel = formState.muassel
    val outdoorMuassel = formState.outdoorMuassel

    val dueCreditItems = formState.dueCreditItems
    val previousDueCollections = formState.previousDueCollections
    val staffAdvances = formState.staffAdvances
    val unpaidBills = formState.unpaidBills
    val purchasedItems = formState.purchasedItems

    var modalType by remember { mutableStateOf(ModalType.NONE) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val invalidAmountError = stringResource(R.string.valid_amount_error)
    val entryDigits = MoneyFormat.minorUnitDigits(currentCurrency)

    val startingCashClass = classifyMoneyInput(startingCashInput, entryDigits)
    val cashClass = classifyMoneyInput(cashReceipts, entryDigits)
    val madaClass = classifyMoneyInput(madaPayments, entryDigits)
    val digitalWalletClass = classifyMoneyInput(digitalWalletInput, entryDigits)
    val totalDiscountsClass = classifyMoneyInput(totalDiscountsInput, entryDigits)
    val salesReturnsClass = classifyMoneyInput(salesReturnsInput, entryDigits)
    val actualCashClass = classifyMoneyInput(actualCashCountInput, entryDigits)
    val expensesClass = classifyMoneyInput(totalExpenses, entryDigits)

    fun minorFromClassified(input: MoneyInput): Long = when (input) {
        is MoneyInput.Valid -> input.minor
        MoneyInput.Empty, MoneyInput.Invalid -> 0L
    }

    val startingCashMinor = minorFromClassified(startingCashClass)
    val cashMinor = minorFromClassified(cashClass)
    val madaMinor = minorFromClassified(madaClass)
    val digitalWalletMinor = minorFromClassified(digitalWalletClass)
    val totalDiscountsMinor = minorFromClassified(totalDiscountsClass)
    val salesReturnsMinor = minorFromClassified(salesReturnsClass)
    val actualCashMinor = (actualCashClass as? MoneyInput.Valid)?.minor
    val actualCashVal = actualCashMinor?.let { MoneyFormat.toMajorUnits(it, entryDigits) }
    val expensesMinor = minorFromClassified(expensesClass)

    val startingCash = MoneyFormat.toMajorUnits(startingCashMinor, entryDigits)
    val cash = MoneyFormat.toMajorUnits(cashMinor, entryDigits)
    val mada = MoneyFormat.toMajorUnits(madaMinor, entryDigits)
    val digitalWallet = MoneyFormat.toMajorUnits(digitalWalletMinor, entryDigits)
    val totalDiscounts = MoneyFormat.toMajorUnits(totalDiscountsMinor, entryDigits)
    val salesReturns = MoneyFormat.toMajorUnits(salesReturnsMinor, entryDigits)
    val expenses = MoneyFormat.toMajorUnits(expensesMinor, entryDigits)

    val totalCreditMinor = MoneyFormat.sumOfMinor(dueCreditItems.map { it.amount })
    val totalOldDueCashMinor = MoneyFormat.sumOfMinor(
        previousDueCollections.filter { it.paymentMode.equals("CASH", ignoreCase = true) }.map { it.amount }
    )
    val totalOldDueBankMinor = MoneyFormat.sumOfMinor(
        previousDueCollections.filter { !it.paymentMode.equals("CASH", ignoreCase = true) }.map { it.amount }
    )
    val totalStaffCashMinor = MoneyFormat.sumOfMinor(
        staffAdvances.filter { it.paymentMode.equals("CASH", ignoreCase = true) }.map { it.amount }
    )
    val totalStaffBankMinor = MoneyFormat.sumOfMinor(
        staffAdvances.filter { !it.paymentMode.equals("CASH", ignoreCase = true) }.map { it.amount }
    )
    val totalWalkoutMinor = MoneyFormat.sumOfMinor(unpaidBills.map { it.amount })
    val totalItemsCashMinor = MoneyFormat.sumOfMinor(
        purchasedItems.filter { it.paidVia.equals("CASH", ignoreCase = true) }.map { it.totalAmount }
    )

    // International Standard Formulas
    val totalCashOutMinor = MoneyFormat.addMinor(expensesMinor, totalStaffCashMinor, totalItemsCashMinor)
    val expectedCashMinor = MoneyFormat.calculateExpectedCashMinor(
        startingCash = startingCashMinor,
        cashSales = cashMinor,
        payIn = totalOldDueCashMinor,
        payOut = totalCashOutMinor
    )
    val varianceMinor = actualCashMinor?.let { MoneyFormat.calculateVarianceMinor(it, expectedCashMinor) }
    val netMadaMinor = MoneyFormat.subtractMinor(
        MoneyFormat.addMinor(madaMinor, digitalWalletMinor, totalOldDueBankMinor),
        totalStaffBankMinor
    )

    val hasEnteredData = cashier.isNotBlank() || startingCashInput.isNotBlank() ||
            cashReceipts.isNotBlank() || madaPayments.isNotBlank() || digitalWalletInput.isNotBlank() ||
            totalDiscountsInput.isNotBlank() || salesReturnsInput.isNotBlank() ||
            actualCashCountInput.isNotBlank() || notesInput.isNotBlank() ||
            totalExpenses.isNotBlank() || staffCount > 0 ||
            muassel.isNotBlank() || outdoorMuassel.isNotBlank() ||
            dueCreditItems.isNotEmpty() || previousDueCollections.isNotEmpty() ||
            staffAdvances.isNotEmpty() || unpaidBills.isNotEmpty() ||
            purchasedItems.isNotEmpty()

    val hasInvalidMoneyInput = listOf(
        startingCashClass,
        cashClass,
        madaClass,
        digitalWalletClass,
        totalDiscountsClass,
        salesReturnsClass,
        actualCashClass,
        expensesClass
    ).any { it is MoneyInput.Invalid }

    val canSave = cashier.isNotBlank() && !hasInvalidMoneyInput && (
            cash > 0.0 || mada > 0.0 || digitalWallet > 0.0 || expenses > 0.0 || startingCash > 0.0 ||
            totalDiscounts > 0.0 || salesReturns > 0.0 ||
            (muassel.toIntOrNull() ?: 0) > 0 || (outdoorMuassel.toIntOrNull() ?: 0) > 0 ||
            dueCreditItems.isNotEmpty() || previousDueCollections.isNotEmpty() ||
            staffAdvances.isNotEmpty() || unpaidBills.isNotEmpty() ||
            purchasedItems.isNotEmpty()
    )

    fun resetAll() {
        formActions.onClearForm()
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
                expectedCash = expectedCashMinor,
                variance = varianceMinor,
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
                            onSelected = { formActions.onCashierChange(if (it == selectCashierText) "" else it) }
                        )
                        if (cashierOptions.isEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.no_cashiers_dropdown_hint),
                                fontSize = 11.sp,
                                color = ShiftColors.TextMuted
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.weight(1f)) {
                                LabeledDropdown(
                                    label = stringResource(R.string.shift),
                                    options = shiftOptions,
                                    selected = if (shift == "Day") dayShiftText else stringResource(R.string.shift_night_option),
                                    onSelected = { formActions.onShiftChange(if (it == dayShiftText) "Day" else "Night") }
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
                            NumberField(
                                stringResource(R.string.starting_cash),
                                startingCashInput,
                                formActions.onStartingCashChange,
                                Modifier.weight(1f),
                                errorText = if (startingCashClass is MoneyInput.Invalid) invalidAmountError else null
                            )
                            NumberField(
                                stringResource(R.string.cash),
                                cashReceipts,
                                formActions.onCashReceiptsChange,
                                Modifier.weight(1f),
                                errorText = if (cashClass is MoneyInput.Invalid) invalidAmountError else null
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NumberField(
                                stringResource(R.string.mada_bank),
                                madaPayments,
                                formActions.onMadaPaymentsChange,
                                Modifier.weight(1f),
                                errorText = if (madaClass is MoneyInput.Invalid) invalidAmountError else null
                            )
                            NumberField(
                                stringResource(R.string.digital_wallet),
                                digitalWalletInput,
                                formActions.onDigitalWalletChange,
                                Modifier.weight(1f),
                                errorText = if (digitalWalletClass is MoneyInput.Invalid) invalidAmountError else null
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NumberField(
                                stringResource(R.string.total_discounts_label),
                                totalDiscountsInput,
                                formActions.onTotalDiscountsChange,
                                Modifier.weight(1f),
                                errorText = if (totalDiscountsClass is MoneyInput.Invalid) invalidAmountError else null
                            )
                            NumberField(
                                stringResource(R.string.sales_returns_label),
                                salesReturnsInput,
                                formActions.onSalesReturnsChange,
                                Modifier.weight(1f),
                                errorText = if (salesReturnsClass is MoneyInput.Invalid) invalidAmountError else null
                            )
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
                                        formActions.onStaffCountChange(n)
                                        if (n == 0) formActions.onTotalExpensesChange("")
                                    }
                                )
                            }
                            Box(Modifier.weight(1f)) {
                                NumberField(
                                    stringResource(R.string.total_expense), totalExpenses, formActions.onTotalExpensesChange,
                                    Modifier.fillMaxWidth(), enabled = staffCount != 0,
                                    errorText = if (expensesClass is MoneyInput.Invalid) invalidAmountError else null
                                )
                            }
                        }

                        // ---- Mu'assel ----
                        SectionHeader("📦", stringResource(R.string.sale_of_muassel), ShiftColors.PurpleLight, ShiftColors.Purple, ShiftColors.Purple)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NumberField(stringResource(R.string.muassel), muassel, formActions.onMuasselChange, Modifier.weight(1f), isInteger = true)
                            NumberField(stringResource(R.string.outdoor_muassel), outdoorMuassel, formActions.onOutdoorMuasselChange, Modifier.weight(1f), isInteger = true)
                        }

                        // ---- Due Sales (credit) ----
                        SectionHeader("📒", stringResource(R.string.due_sales), ShiftColors.DangerLight, ShiftColors.Danger, ShiftColors.Danger) {
                            modalType = ModalType.CREDIT
                        }
                        dueCreditItems.forEach { item ->
                            val normReceipt = normalizeReceiptNumber(item.customerName)
                            val receiptLabel = when {
                                isUnspecifiedReceipt(normReceipt) -> stringResource(R.string.receipt_unspecified)
                                normReceipt.isNotEmpty() -> "#$normReceipt"
                                else -> ""
                            }
                            EntryRow(
                                left = receiptLabel,
                                right = "${MoneyFormat.toPlainDecimal(item.amount, entryDigits)} $currentCurrency",
                                onRemove = { formActions.onRemoveDueCredit(item.id) }
                            )
                        }

                        // ---- Due Collection ----
                        SectionHeader("💵", stringResource(R.string.due_collection), ShiftColors.PurpleLight, ShiftColors.Purple, ShiftColors.Purple) {
                            modalType = ModalType.OLD_DUE
                        }
                        previousDueCollections.forEach { item ->
                            val normReceipt = normalizeReceiptNumber(item.customerName)
                            val receiptLabel = when {
                                isUnspecifiedReceipt(normReceipt) -> stringResource(R.string.receipt_unspecified)
                                normReceipt.isNotEmpty() -> "#$normReceipt"
                                else -> ""
                            }
                            EntryRow(
                                left = receiptLabel,
                                right = "${MoneyFormat.toPlainDecimal(item.amount, entryDigits)} $currentCurrency (${item.paymentMode.lowercase(Locale.ROOT)})",
                                onRemove = { formActions.onRemovePreviousDue(item.id) }
                            )
                        }

                        // ---- Employer Advance (Dropdown-only Foodics POS Standard) ----
                        val context = LocalContext.current
                        val db = remember(context) { com.lojia.shiftreport.data.AppDatabase.getInstance(context) }
                        val employeeDao = remember(db) { db.employeeDao() }

                        val activeEmployees by employeeDao.getAllActive().collectAsState(initial = emptyList())
                        val savedAdvanceItems = remember(shiftReports) {
                            parseEmployerAdvances(shiftReports)
                        }
                        val advancesByEmployee by remember(activeEmployees, savedAdvanceItems, staffAdvances, entryDigits) {
                            derivedStateOf {
                                val range = currentMonthRange(System.currentTimeMillis(), ZoneId.systemDefault())
                                val unsavedByName = mutableMapOf<String, Long>()
                                for (item in staffAdvances) {
                                    val key = normalizeRestoreName(item.staffName)
                                    if (key.isNotEmpty()) {
                                        unsavedByName[key] = (unsavedByName[key] ?: 0L) + item.amount
                                    }
                                }
                                activeEmployees.associate { emp ->
                                    val saved = advancesInMonthMinor(savedAdvanceItems, emp.name, range)
                                    val unsaved = unsavedByName[normalizeRestoreName(emp.name)] ?: 0L
                                    val advances = saved + unsaved
                                    val salary = emp.monthlySalaryMinor
                                    emp.id to EmployeeAdvanceSummary(
                                        salaryMinor = salary,
                                        advancesMinor = advances,
                                        balanceMinor = balanceMinor(salary, advances)
                                    )
                                }
                            }
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
                            currency = currentCurrency,
                            onAddAdvance = {
                                when (val classified = classifyMoneyInput(advanceAmountText, entryDigits)) {
                                    is MoneyInput.Valid -> {
                                        val emp = selectedAdvanceEmployee
                                        val amountMinor = classified.minor
                                        if (emp == null || amountMinor <= 0L || amountMinor > emp.monthlySalaryMinor) {
                                            val formattedSalary = MoneyFormat.formatMinor(emp?.monthlySalaryMinor ?: 0L, currentCurrency)
                                            android.widget.Toast.makeText(
                                                context,
                                                context.getString(R.string.advance_over_salary_error, formattedSalary),
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            showAdminAuthForAdvance = true
                                        }
                                    }
                                    MoneyInput.Empty, MoneyInput.Invalid -> {
                                        android.widget.Toast.makeText(
                                            context,
                                            invalidAmountError,
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        )

                        if (showAdminAuthForAdvance && selectedAdvanceEmployee != null) {
                            val emp = selectedAdvanceEmployee!!
                            val advanceClassified = classifyMoneyInput(advanceAmountText, entryDigits)
                            val isBank = advancePaymentType == "Bank"
                            com.lojia.shiftreport.auth.AdminAuthDialog(
                                onDismissRequest = { showAdminAuthForAdvance = false },
                                onAuthSuccess = {
                                    showAdminAuthForAdvance = false
                                    if (advanceClassified is MoneyInput.Valid) {
                                        val amt = MoneyFormat.toMajorUnits(advanceClassified.minor, entryDigits)
                                        formActions.onAddStaffAdvance(
                                            emp.name,
                                            amt,
                                            entryDigits,
                                            if (isBank) "BANK" else "CASH"
                                        )
                                        advanceAmountText = ""
                                        android.widget.Toast.makeText(
                                            context,
                                            context.getString(
                                                R.string.report_advance_added_toast,
                                                MoneyFormat.format(amt, currentCurrency),
                                                emp.name
                                            ),
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            )
                        }

                        staffAdvances.forEach { item ->
                            EntryRow(
                                left = item.staffName,
                                right = "${MoneyFormat.toPlainDecimal(item.amount, entryDigits)} $currentCurrency (${item.paymentMode.lowercase(Locale.ROOT)})",
                                onRemove = { formActions.onRemoveStaffAdvance(item.id) }
                            )
                        }

                        // ---- Walk-out ----
                        SectionHeader("🚪", stringResource(R.string.walk_out), ShiftColors.DangerLight, ShiftColors.Danger, ShiftColors.Danger) {
                            modalType = ModalType.WALKOUT
                        }
                        unpaidBills.forEach { item ->
                            EntryRow(
                                left = item.tableOrOrderRef,
                                right = "${MoneyFormat.toPlainDecimal(item.amount, entryDigits)} $currentCurrency",
                                onRemove = { formActions.onRemoveUnpaidBill(item.id) }
                            )
                        }

                        // ---- Paid Out ----
                        SectionHeader("🛒", stringResource(R.string.paid_out), ShiftColors.DangerLight, ShiftColors.Danger, ShiftColors.Danger) {
                            modalType = ModalType.ITEM
                        }
                        purchasedItems.forEach { item ->
                            val qtyInt = item.quantity.toInt()
                            val leftText = if (qtyInt > 0) stringResource(R.string.report_item_qty_format, item.itemName, qtyInt) else item.itemName
                            EntryRow(
                                left = leftText,
                                right = "${MoneyFormat.toPlainDecimal(item.totalAmount, entryDigits)} $currentCurrency (${item.paidVia.lowercase(Locale.ROOT)})",
                                onRemove = { formActions.onRemovePurchasedItem(item.id) }
                            )
                        }

                        Spacer(Modifier.height(24.dp))

                        // ---- Standard 4-Card International POS Reconciliation Summary ----
                        PosReconciliationSummary(
                            startingCash = startingCashMinor,
                            cashSales = cashMinor,
                            madaSales = madaMinor,
                            digitalWalletSales = digitalWalletMinor,
                            totalDiscounts = totalDiscountsMinor,
                            salesReturns = salesReturnsMinor,
                            totalExpenses = expensesMinor,
                            totalDueCredit = totalCreditMinor,
                            totalDueCollectedCash = totalOldDueCashMinor,
                            totalDueCollectedBank = totalOldDueBankMinor,
                            totalStaffAdvanceCash = totalStaffCashMinor,
                            totalStaffAdvanceBank = totalStaffBankMinor,
                            totalWalkout = totalWalkoutMinor,
                            totalPurchasesCash = totalItemsCashMinor,
                            staffMealsCount = staffCount,
                            muasselCount = muassel.toIntOrNull() ?: 0,
                            outdoorMuasselCount = outdoorMuassel.toIntOrNull() ?: 0,
                            actualCashCount = actualCashCountInput,
                            onActualCashCountChange = formActions.onActualCashCountChange,
                            notes = notesInput,
                            onNotesChange = formActions.onNotesChange,
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
                                    if (hasInvalidMoneyInput) return@Button
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
                                            creditEntries = dueCreditItems.map {
                                                CreditEntry(
                                                    it.customerName,
                                                    MoneyFormat.toMajorUnits(it.amount, entryDigits)
                                                )
                                            },
                                            oldDueEntries = previousDueCollections.map {
                                                OldDueEntry(
                                                    it.customerName,
                                                    MoneyFormat.toMajorUnits(it.amount, entryDigits),
                                                    if (it.paymentMode.equals("BANK", ignoreCase = true)) PayType.BANK else PayType.CASH
                                                )
                                            },
                                            walkoutEntries = unpaidBills.map {
                                                WalkoutEntry(
                                                    it.tableOrOrderRef,
                                                    MoneyFormat.toMajorUnits(it.amount, entryDigits)
                                                )
                                            },
                                            itemEntries = purchasedItems.map {
                                                ItemEntry(
                                                    name = it.itemName,
                                                    qty = it.quantity.toInt(),
                                                    unitPriceDouble = MoneyFormat.toMajorUnits(it.unitPrice, entryDigits),
                                                    totalDouble = MoneyFormat.toMajorUnits(it.totalAmount, entryDigits),
                                                    type = if (it.paidVia.equals("BANK", ignoreCase = true)) PayType.BANK else PayType.CASH
                                                )
                                            },
                                            netCash = MoneyFormat.toMajorUnits(expectedCashMinor, entryDigits),
                                            netMada = MoneyFormat.toMajorUnits(netMadaMinor, entryDigits),
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
                    state.selectedDateMillis?.let { formActions.onDateMillisChange(it) }
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
                    ModalType.CREDIT -> formActions.onAddDueCredit(result.receipt, result.amount, entryDigits)
                    ModalType.OLD_DUE -> formActions.onAddPreviousDue(
                        result.receipt,
                        result.amount,
                        entryDigits,
                        result.type.name
                    )
                    ModalType.STAFF -> formActions.onAddStaffAdvance(
                        result.name,
                        result.amount,
                        entryDigits,
                        result.type.name
                    )
                    ModalType.WALKOUT -> formActions.onAddUnpaidBill(result.name, result.amount, entryDigits)
                    ModalType.ITEM -> formActions.onAddPurchasedItem(
                        result.name,
                        result.qty,
                        result.unitPrice,
                        result.amount,
                        entryDigits,
                        result.type.name
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
    advancesByEmployee: Map<String, EmployeeAdvanceSummary> = emptyMap(),
    selectedEmployee: com.lojia.shiftreport.data.Employee?,
    onEmployeeSelected: (com.lojia.shiftreport.data.Employee) -> Unit,
    amount: String,
    onAmountChange: (String) -> Unit,
    selectedType: String,
    onTypeChange: (String) -> Unit,
    onAddAdvance: () -> Unit,
    currency: String = MoneyFormat.DEFAULT_CURRENCY_CODE,
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
                value = selectedEmployee?.name ?: stringResource(R.string.report_select_employee),
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
                label = { Text(stringResource(R.string.nav_employee)) },
                placeholder = { Text(stringResource(R.string.select_from_employees_format, employees.size)) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                if (employees.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.no_employees_add_hint)) },
                        onClick = {
                            expanded = false
                            onNavigateToSettings()
                        }
                    )
                } else {
                    employees.forEach { employee ->
                        val summary = advancesByEmployee[employee.id] ?: EmployeeAdvanceSummary(
                            salaryMinor = employee.monthlySalaryMinor,
                            advancesMinor = 0L,
                            balanceMinor = balanceMinor(employee.monthlySalaryMinor, 0L)
                        )
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = employee.name,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = stringResource(
                                            R.string.employee_advance_subtitle,
                                            MoneyFormat.formatMinor(summary.salaryMinor, currency),
                                            MoneyFormat.formatMinor(summary.advancesMinor, currency),
                                            MoneyFormat.formatMinor(summary.balanceMinor, currency)
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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

        val advanceAmountClass = classifyMoneyInput(amount, MoneyFormat.minorUnitDigits(currency))
        val isAdvanceAmountInvalid = advanceAmountClass is MoneyInput.Invalid

        OutlinedTextField(
            value = amount,
            onValueChange = onAmountChange,
            label = { Text(stringResource(R.string.advance_amount_label)) },
            enabled = selectedEmployee != null,
            isError = isAdvanceAmountInvalid,
            supportingText = if (isAdvanceAmountInvalid) {
                { Text(stringResource(R.string.valid_amount_error), color = ErrorRedLight) }
            } else null,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
            ),
            suffix = { Text(currency) },
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
                label = { Text(stringResource(R.string.cash)) }
            )
            FilterChip(
                selected = selectedType == "Bank",
                onClick = { onTypeChange("Bank") },
                label = { Text(stringResource(R.string.bank)) }
            )
        }

        Button(
            onClick = onAddAdvance,
            enabled = selectedEmployee != null && amount.isNotEmpty() && !isAdvanceAmountInvalid,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(
                    R.string.report_add_advance_button_format,
                    amount.ifBlank { "0" },
                    currency,
                    selectedEmployee?.name ?: stringResource(R.string.nav_employee)
                ),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
