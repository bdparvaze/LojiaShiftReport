package com.lojia.shiftreport.report

import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import kotlinx.coroutines.launch
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner


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
    val employees by viewModel.employees.collectAsState()
    val currentCurrency by viewModel.currentCurrency.collectAsState()
    val businessProfile by viewModel.businessProfile.collectAsState()
    val formState by viewModel.draftFormState.collectAsState()
    val showDraftRestoredNotice by viewModel.showDraftRestoredNotice.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val restoredMessage = stringResource(R.string.draft_restored_message)
    val discardAction = stringResource(R.string.draft_discard_action)

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                coroutineScope.launch {
                    viewModel.flushDraft()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(showDraftRestoredNotice) {
        if (showDraftRestoredNotice) {
            viewModel.consumeDraftRestoredNotice()
            val result = snackbarHostState.showSnackbar(
                message = restoredMessage,
                actionLabel = discardAction,
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.discardRestoredDraft()
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.draftSaveError.collect { errMsg ->
            snackbarHostState.showSnackbar(
                message = errMsg,
                duration = SnackbarDuration.Short
            )
        }
    }

    val formActions = remember(viewModel) {
        ShiftFormActions(
            onCashierChange = { viewModel.selectedCashier.value = it },
            onShiftChange = { viewModel.selectedShift.value = it },
            onDateMillisChange = { viewModel.selectedDateInMillis.value = it },
            onStartingCashChange = { viewModel.openingCashInput.value = it },
            onCashReceiptsChange = { viewModel.grossCashInput.value = it },
            onMadaPaymentsChange = { viewModel.madaPaymentsInput.value = it },
            onDigitalWalletChange = { viewModel.digitalWalletInput.value = it },
            onTotalDiscountsChange = { viewModel.totalDiscountsInput.value = it },
            onSalesReturnsChange = { viewModel.salesReturnsInput.value = it },
            onActualCashCountChange = { viewModel.closingCashInput.value = it },
            onNotesChange = { viewModel.notesInput.value = it },
            onStaffCountChange = { viewModel.staffMealsCountInput.value = it.toString() },
            onTotalExpensesChange = { viewModel.totalExpensesInput.value = it },
            onMuasselChange = { viewModel.muasselQtyInput.value = it },
            onOutdoorMuasselChange = { viewModel.outdoorShishaQtyInput.value = it },
            onAddDueCredit = { receipt, amount, digits ->
                viewModel.addDueCreditItem(customerName = receipt, amount = amount, digits = digits)
            },
            onRemoveDueCredit = { id -> viewModel.removeDueCreditItem(id) },
            onAddPreviousDue = { receipt, amount, digits, paymentMode ->
                viewModel.addPreviousDueCollection(
                    customerName = receipt,
                    amount = amount,
                    digits = digits,
                    paymentMode = paymentMode
                )
            },
            onRemovePreviousDue = { id -> viewModel.removePreviousDueCollection(id) },
            onAddStaffAdvance = { name, amount, digits, paymentMode, employeeId ->
                viewModel.addStaffAdvance(
                    staffName = name,
                    amount = amount,
                    digits = digits,
                    paymentMode = paymentMode,
                    employeeId = employeeId
                )
            },
            onRemoveStaffAdvance = { id -> viewModel.removeStaffAdvance(id) },
            onAddUnpaidBill = { ref, amount, digits ->
                viewModel.addUnpaidBill(tableOrOrderRef = ref, amount = amount, digits = digits)
            },
            onRemoveUnpaidBill = { id -> viewModel.removeUnpaidBill(id) },
            onAddPurchasedItem = { name, qty, unitPrice, total, digits, paidVia ->
                viewModel.addPurchasedItem(
                    itemName = name,
                    quantity = qty.toDouble(),
                    unitPrice = unitPrice,
                    paidVia = paidVia,
                    totalAmount = total,
                    digits = digits
                )
            },
            onRemovePurchasedItem = { id -> viewModel.removePurchasedItem(id) },
            onClearForm = { viewModel.clearForm() }
        )
    }

    val availableCashiers = remember(cashiersState) {
        val names = cashiersState
            .filter { !it.isArchived && it.isActive && it.active }
            .map { it.name }
            .filter { it.isNotBlank() }
        if (names.isNotEmpty()) names else emptyList()
    }

    ShiftReportScreenContent(
        shiftReports = shiftReports,
        formState = formState,
        formActions = formActions,
        snackbarHostState = snackbarHostState,
        userProfile = userProfile,
        activeShiftSession = activeShiftSession,
        cashMovements = cashMovements,
        cashierOptions = availableCashiers,
        employees = employees,
        language = language,
        currentCurrency = currentCurrency,
        onPreviewPdf = onPreviewPdf,
        onSaveReportData = { data ->
            val digits = MoneyFormat.minorUnitDigits(currentCurrency)
            val dueJson = org.json.JSONArray().apply {
                viewModel.dueCreditItems.value.forEach { item ->
                    val normReceipt = normalizeReceiptNumber(item.customerName)
                    val customerLabel = when {
                        isUnspecifiedReceipt(normReceipt) -> context.getString(R.string.receipt_unspecified)
                        normReceipt.isNotEmpty() -> context.getString(R.string.receipt_num_tag, normReceipt)
                        else -> ""
                    }
                    put(org.json.JSONObject().apply {
                        put("receiptNo", item.customerName)
                        put("customerName", customerLabel)
                        put("amount", item.amount)
                    })
                }
            }.toString()

            val prevDueJson = org.json.JSONArray().apply {
                viewModel.previousDueCollections.value.forEach { item ->
                    val normReceipt = normalizeReceiptNumber(item.customerName)
                    val customerLabel = when {
                        isUnspecifiedReceipt(normReceipt) -> context.getString(R.string.receipt_unspecified)
                        normReceipt.isNotEmpty() -> context.getString(R.string.receipt_num_tag, normReceipt)
                        else -> ""
                    }
                    put(org.json.JSONObject().apply {
                        put("receiptNo", item.customerName)
                        put("customerName", customerLabel)
                        put("amount", item.amount)
                        put("paymentMode", item.paymentMode)
                    })
                }
            }.toString()

            val staffAdvJson = viewModel.serializeAdvances(viewModel.staffAdvances.value)

            val walkoutJson = org.json.JSONArray().apply {
                viewModel.unpaidBills.value.forEach { item ->
                    put(org.json.JSONObject().apply {
                        put("tableOrOrderRef", item.tableOrOrderRef)
                        put("amount", item.amount)
                    })
                }
            }.toString()

            val itemsJson = org.json.JSONArray().apply {
                viewModel.purchasedItems.value.forEach { item ->
                    put(org.json.JSONObject().apply {
                        put("itemName", item.itemName)
                        put("quantity", item.quantity)
                        put("unitPrice", item.unitPrice)
                        put("totalAmount", item.totalAmount)
                        put("paidVia", item.paidVia)
                    })
                }
            }.toString()

            val noteParts = mutableListOf<String>()
            if (data.startingCash > java.math.BigDecimal.ZERO) {
                noteParts.add("Starting Float: ${MoneyFormat.toPlainDecimal(MoneyFormat.toMinorUnits(data.startingCash, digits), digits)}")
            }
            if (data.actualCash != null) {
                noteParts.add("Actual Cash Count: ${MoneyFormat.toPlainDecimal(MoneyFormat.toMinorUnits(data.actualCash, digits), digits)}")
            }
            if (data.notes.isNotBlank() && data.notes != "Saved from Shift Closing Ledger") {
                noteParts.add(data.notes)
            }
            val formattedNotes = if (noteParts.isNotEmpty()) noteParts.joinToString(" | ") else "Saved from Shift Closing Ledger"

            val matchedCashier = cashiersState.firstOrNull { it.name.equals(data.cashier, ignoreCase = true) }
            val resolvedCashierId = matchedCashier?.id?.toString() ?: ""

            val report = ShiftReport(
                cashierName = data.cashier.ifBlank { "Standard Cashier" },
                cashierId = resolvedCashierId,
                shift = data.shift,
                dateInMillis = data.dateMillis,
                openingCash = MoneyFormat.toMinorUnits(if (data.openingCash > java.math.BigDecimal.ZERO) data.openingCash else data.startingCash, digits),
                closingCash = MoneyFormat.toMinorUnits(if (data.closingCash > java.math.BigDecimal.ZERO) data.closingCash else (data.actualCash ?: java.math.BigDecimal.ZERO), digits),
                grossCash = MoneyFormat.toMinorUnits(data.cashReceipts, digits),
                madaPayments = MoneyFormat.toMinorUnits(data.madaPayments, digits),
                digitalWallet = MoneyFormat.toMinorUnits(data.digitalWallet, digits),
                totalDiscounts = MoneyFormat.toMinorUnits(data.totalDiscounts, digits),
                salesReturns = MoneyFormat.toMinorUnits(data.salesReturns, digits),
                staffMealsCount = data.staffCount,
                totalExpenses = MoneyFormat.toMinorUnits(data.totalExpenses, digits),
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
    formState: ShiftFormState = ShiftFormState(),
    formActions: ShiftFormActions = ShiftFormActions(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    userProfile: com.lojia.shiftreport.data.UserProfile? = null,
    activeShiftSession: ShiftSession? = null,
    cashMovements: List<CashMovement> = emptyList(),
    cashierOptions: List<String> = emptyList(),
    employees: List<Employee> = emptyList(),
    language: AppLanguage = AppLanguage.BENGALI,
    currentCurrency: String = MoneyFormat.DEFAULT_CURRENCY_CODE,
    onPreviewPdf: (ShiftReport) -> Unit = {},
    onSaveReportData: (ShiftReportData) -> Unit = {},
    onOpenShift: (String, String, Double) -> Unit = { _, _, _ -> },
    onCloseShift: (ShiftSession, Double, String) -> Unit = { _, _, _ -> },
    onDeleteReport: (ShiftReport) -> Unit = {}
) {
    // 0 = Report Entry, 1 = Due History, 2 = Employer, 3 = Shopping/Paid Out, 4 = Walkout, 5 = Archives
    var savedTab by rememberSaveable { mutableIntStateOf(0) }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()

    val tabTitles = listOf(
        stringResource(R.string.tab_report_entry),
        stringResource(R.string.tab_due_history),
        stringResource(R.string.tab_employer),
        stringResource(R.string.tab_paid_out),
        stringResource(R.string.tab_walkout),
        stringResource(R.string.tab_archives, shiftReports.size)
    )

    val pagerState = rememberPagerState(initialPage = savedTab.coerceIn(0, tabTitles.size - 1)) {
        tabTitles.size
    }

    LaunchedEffect(pagerState.currentPage) {
        savedTab = pagerState.currentPage
    }

    LaunchedEffect(pagerState.isScrollInProgress) {
        if (pagerState.isScrollInProgress) {
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }

    val tabIcons = listOf(
        Icons.AutoMirrored.Filled.ListAlt,
        Icons.Default.ReceiptLong,
        Icons.Default.AccountBalanceWallet,
        Icons.Default.ShoppingCart,
        Icons.Default.DirectionsWalk,
        Icons.Default.FolderZip
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
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
                selectedTabIndex = pagerState.currentPage,
                containerColor = SurfaceLight,
                contentColor = PrimaryIndigoDark,
                edgePadding = 8.dp,
                modifier = Modifier.height(44.dp),
                indicator = { tabPositions ->
                    if (pagerState.currentPage < tabPositions.size) {
                        Box(
                            Modifier
                                .tabIndicatorOffset(tabPositions[pagerState.currentPage])
                                .height(3.dp)
                                .background(
                                    color = PrimaryIndigoLight,
                                    shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
                                )
                        )
                    }
                },
                divider = { HorizontalDivider(color = OutlineLight) }
            ) {
                tabTitles.forEachIndexed { index, title ->
                    val isSelected = pagerState.currentPage == index
                    Tab(
                        selected = isSelected,
                        modifier = Modifier.height(44.dp),
                        onClick = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
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

        // ---- TAB CONTENT (HORIZONTAL PAGER) ----
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { page ->
            when (page) {
                0 -> ReportEntryTab(
                    cashierOptions = cashierOptions,
                    currentCurrency = currentCurrency,
                    shiftReports = shiftReports,
                    formState = formState,
                    formActions = formActions,
                    onSave = onSaveReportData
                )
                1 -> DueLedgerTab(
                    reports = shiftReports,
                    currency = currentCurrency
                )
                2 -> EmployerLedgerTab(
                    reports = shiftReports,
                    employees = employees,
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
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 76.dp)
        )
    }
}
