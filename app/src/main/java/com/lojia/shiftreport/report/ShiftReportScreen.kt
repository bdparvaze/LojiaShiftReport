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
