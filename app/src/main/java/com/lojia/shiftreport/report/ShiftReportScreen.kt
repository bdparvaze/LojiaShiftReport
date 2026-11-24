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


import androidx.compose.ui.graphics.Brush
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


enum class ShiftReportTab {
    REPORT_ENTRY,
    DUE_HISTORY,
    EMPLOYER_ADVANCES,
    PAID_OUT,
    WALKOUT,
    ARCHIVES
}

val SHIFT_REPORT_TABS: List<ShiftReportTab> = ShiftReportTab.entries

/* ----------------------------------------------------------------------
 * OVERLOAD FOR VIEWMODEL & NAVIGATION INTEGRATION (WITH TOP 3 TABS)
 * ---------------------------------------------------------------------- */
@Composable
fun ShiftReportScreen(
    viewModel: ReportViewModel,
    language: AppLanguage = AppLanguage.ENGLISH,
    requestedTab: ShiftReportTab = ShiftReportTab.REPORT_ENTRY,
    requestedEntryModal: ModalType = ModalType.NONE,
    routeNonce: Long = 0L,
    onRouteRequestHandled: () -> Unit = {},
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
        businessProfile = businessProfile,
        requestedTab = requestedTab,
        requestedEntryModal = requestedEntryModal,
        routeNonce = routeNonce,
        onRouteRequestHandled = onRouteRequestHandled,
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
    businessProfile: BusinessProfile? = null,
    requestedTab: ShiftReportTab = ShiftReportTab.REPORT_ENTRY,
    requestedEntryModal: ModalType = ModalType.NONE,
    routeNonce: Long = 0L,
    onRouteRequestHandled: () -> Unit = {},
    onPreviewPdf: (ShiftReport) -> Unit = {},
    onSaveReportData: (ShiftReportData) -> Unit = {},
    onOpenShift: (String, String, Double) -> Unit = { _, _, _ -> },
    onCloseShift: (ShiftSession, Double, String) -> Unit = { _, _, _ -> },
    onDeleteReport: (ShiftReport) -> Unit = {}
) {
    // 0 = Report Entry, 1 = Due History, 2 = Employer, 3 = Shopping/Paid Out, 4 = Walkout, 5 = Archives
    var savedTab by rememberSaveable { mutableIntStateOf(0) }
    var entryModalRequest by remember { mutableStateOf(ModalType.NONE) }
    var entryModalNonce by remember { mutableLongStateOf(0L) }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()

    val depthStyle = LocalDepthStyle.current
    val tabTitles = SHIFT_REPORT_TABS.map { tab ->
        when (tab) {
            ShiftReportTab.REPORT_ENTRY -> stringResource(R.string.tab_report_entry)
            ShiftReportTab.DUE_HISTORY -> stringResource(R.string.tab_due_history)
            ShiftReportTab.EMPLOYER_ADVANCES -> stringResource(R.string.tab_employer)
            ShiftReportTab.PAID_OUT -> stringResource(R.string.tab_paid_out)
            ShiftReportTab.WALKOUT -> stringResource(R.string.tab_walkout)
            ShiftReportTab.ARCHIVES -> stringResource(R.string.tab_archives, shiftReports.size)
        }
    }

    val pagerState = rememberPagerState(initialPage = savedTab.coerceIn(0, SHIFT_REPORT_TABS.size - 1)) {
        SHIFT_REPORT_TABS.size
    }

    LaunchedEffect(routeNonce, requestedTab, requestedEntryModal) {
        if (routeNonce > 0L) {
            val targetPage = requestedTab.ordinal.coerceIn(0, SHIFT_REPORT_TABS.size - 1)
            savedTab = targetPage
            pagerState.scrollToPage(targetPage)
            if (requestedEntryModal != ModalType.NONE && requestedTab == ShiftReportTab.REPORT_ENTRY) {
                entryModalRequest = requestedEntryModal
                entryModalNonce = routeNonce
            }
            onRouteRequestHandled()
        }
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

    val tabIcons = SHIFT_REPORT_TABS.map { tab ->
        when (tab) {
            ShiftReportTab.REPORT_ENTRY -> Icons.AutoMirrored.Filled.ListAlt
            ShiftReportTab.DUE_HISTORY -> Icons.Default.ReceiptLong
            ShiftReportTab.EMPLOYER_ADVANCES -> Icons.Default.AccountBalanceWallet
            ShiftReportTab.PAID_OUT -> Icons.Default.ShoppingCart
            ShiftReportTab.WALKOUT -> Icons.Default.DirectionsWalk
            ShiftReportTab.ARCHIVES -> Icons.Default.FolderZip
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(depthStyle.reportPageBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(depthStyle.reportPageBackground)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    })
                }
        ) {
        // ---- SCROLLABLE OPTIONS TAB ROW ----
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .depthTabBarShadow()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(depthStyle.tabBarGradientStart, depthStyle.tabBarGradientEnd)
                    )
                )
        ) {
            ScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = Color.Transparent,
                contentColor = PrimaryBlueLight,
                edgePadding = Dimens.SpacingSm,
                modifier = Modifier.heightIn(min = Dimens.TabBarMinHeight),
                indicator = { tabPositions ->
                    if (pagerState.currentPage < tabPositions.size) {
                        Box(
                            Modifier
                                .tabIndicatorOffset(tabPositions[pagerState.currentPage])
                                .height(Dimens.TabIndicatorHeight)
                                .depthTabIndicatorGlow(cornerRadius = Dimens.TabIndicatorHeight)
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            depthStyle.tabIndicatorGradientStart,
                                            depthStyle.tabIndicatorGradientEnd
                                        )
                                    ),
                                    shape = RoundedCornerShape(
                                        topStart = Dimens.TabIndicatorHeight,
                                        topEnd = Dimens.TabIndicatorHeight
                                    )
                                )
                        )
                    }
                },
                divider = { HorizontalDivider(color = DepthCardBorder) }
            ) {
                tabTitles.forEachIndexed { index, title ->
                    val isSelected = pagerState.currentPage == index
                    Tab(
                        selected = isSelected,
                        modifier = Modifier.heightIn(min = Dimens.TabBarMinHeight),
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
                                    modifier = Modifier.size(Dimens.IconXs + Dimens.SpacingXxs),
                                    tint = if (isSelected) PrimaryBlueLight else OnSurfaceVariantLight
                                )
                                Spacer(Modifier.width(Dimens.SpacingXs))
                                Text(
                                    text = title,
                                    fontSize = DepthTypeTokens.TabLabelSize,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                    color = if (isSelected) PrimaryBlueLight else OnSurfaceVariantLight,
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
            when (SHIFT_REPORT_TABS[page]) {
                ShiftReportTab.REPORT_ENTRY -> ReportEntryTab(
                    cashierOptions = cashierOptions,
                    currentCurrency = currentCurrency,
                    shiftReports = shiftReports,
                    formState = formState,
                    formActions = formActions,
                    requestedModalType = entryModalRequest,
                    modalRequestNonce = entryModalNonce,
                    onRequestedModalHandled = { entryModalRequest = ModalType.NONE },
                    onSave = onSaveReportData
                )
                ShiftReportTab.DUE_HISTORY -> DueLedgerTab(
                    reports = shiftReports,
                    currency = currentCurrency
                )
                ShiftReportTab.EMPLOYER_ADVANCES -> EmployerLedgerTab(
                    reports = shiftReports,
                    employees = employees,
                    currency = currentCurrency,
                    businessProfile = businessProfile,
                    language = language
                )
                ShiftReportTab.PAID_OUT -> PaidOutShoppingLedgerTab(
                    reports = shiftReports,
                    currency = currentCurrency
                )
                ShiftReportTab.WALKOUT -> WalkoutLedgerTab(
                    reports = shiftReports,
                    currency = currentCurrency
                )
                ShiftReportTab.ARCHIVES -> ShiftReportArchivesTab(
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
