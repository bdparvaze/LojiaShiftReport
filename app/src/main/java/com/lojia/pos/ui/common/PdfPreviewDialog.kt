package com.lojia.pos.ui.common


import android.content.Context

import android.content.Intent

import android.net.Uri

import android.widget.Toast


import androidx.compose.foundation.background


import androidx.compose.foundation.border


import androidx.compose.foundation.layout.*


import androidx.compose.foundation.rememberScrollState


import androidx.compose.foundation.shape.RoundedCornerShape


import androidx.compose.foundation.verticalScroll


import androidx.compose.material.icons.Icons


import androidx.compose.material.icons.filled.*


import androidx.compose.material3.*


import androidx.compose.runtime.*


import androidx.compose.ui.Alignment


import androidx.compose.ui.Modifier


import androidx.compose.ui.graphics.Color


import androidx.compose.ui.platform.LocalContext


import androidx.compose.ui.platform.testTag


import androidx.compose.ui.text.font.FontFamily


import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.text.style.TextOverflow

import androidx.compose.ui.unit.dp


import androidx.compose.ui.unit.sp

import com.lojia.pos.data.*
import com.lojia.pos.ui.theme.*
import com.lojia.pos.util.MoneyFormat
import com.lojia.pos.util.PdfReportGenerator

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import java.text.SimpleDateFormat

import java.util.*


import androidx.compose.ui.res.stringResource

import com.lojia.pos.R

@Composable
fun ShiftReportPreviewDialog(
    report: ShiftReport,
    businessProfile: BusinessProfile?,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onOpenPrinterSettings: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val printerManager = remember(context) { com.lojia.pos.printer.BluetoothPrinterManager(context) }
    val currencyCode = businessProfile?.currency
    val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    var isExporting by remember { mutableStateOf(false) }
    var exportedUri by remember { mutableStateOf<Uri?>(null) }
    var exportSuccessMessage by remember { mutableStateOf<String?>(null) }
    var printerErrorMessage by remember { mutableStateOf<String?>(null) }
    var isPrinting by remember { mutableStateOf(false) }

    val titleLabel = stringResource(R.string.shift_closing_revenue_cert)
    val dateLabel = stringResource(R.string.date_2)
    val shiftLabel = stringResource(R.string.shift_2)
    val cashierLabel = stringResource(R.string.cashier_6)
    val grossCashLabel = stringResource(R.string.gross_cash_received)
    val madaLabel = stringResource(R.string.mada_bank_cards)
    val walletLabel = stringResource(R.string.digital_wallet_applepay)
    val totalRevenueLabel = stringResource(R.string.total_revenue_label)
    val expensesLabel = stringResource(R.string.total_expenses_dash)
    val netCashLabel = stringResource(R.string.net_cash_in_drawer_label)
    val netMadaLabel = stringResource(R.string.net_mada_bank)
    val dueSalesLabel = stringResource(R.string.due_sales_credit_entries)
    val dueCollLabel = stringResource(R.string.due_collection_prev_dues)
    val staffAdvLabel = stringResource(R.string.employer_advances_label)
    val walkoutLabel = stringResource(R.string.walkout_bills_unpaid)
    val paidOutLabel = stringResource(R.string.paid_out_items_purchases)
    val staffMealsLabel = stringResource(R.string.staff_meals_count)
    val muasselLabel = stringResource(R.string.muassel_quantity)
    val outdoorMuasselLabel = stringResource(R.string.outdoor_muassel)
    val notesLabel = stringResource(R.string.notes)

    val startingCash = PdfReportGenerator.extractStartingCashFromNotes(report.notes) ?: 0.0
    val actualCashCount = PdfReportGenerator.extractActualCashFromNotes(report.notes)
    val cashIn = report.totalDueCollectedCash
    val cashOut = report.totalCashOut
    val expectedCashInDrawer = startingCash + report.grossCash + cashIn - cashOut
    val variance = actualCashCount?.let { it - expectedCashInDrawer }

    val defaultBizName = stringResource(R.string.default_business_name)
    val reportText = buildString {
        appendLine("========================================")
        appendLine("           ${businessProfile?.businessName ?: defaultBizName}")
        appendLine("         $titleLabel")
        appendLine("========================================")
        appendLine("$dateLabel: ${dateFormatter.format(Date(report.dateInMillis))}")
        appendLine("$shiftLabel: ${report.shift}")
        appendLine("$cashierLabel: ${report.cashierName}")
        appendLine("VAT ID: ${businessProfile?.vatNumber ?: "310123456700003"}")
        appendLine("----------------------------------------")
        appendLine("[1. SALES SUMMARY]")
        appendLine("$grossCashLabel: ${MoneyFormat.format(report.grossCash, currencyCode)}")
        appendLine("$madaLabel: ${MoneyFormat.format(report.madaPayments, currencyCode)}")
        if (report.digitalWallet > 0) {
            appendLine("$walletLabel: ${MoneyFormat.format(report.digitalWallet, currencyCode)}")
        }
        appendLine("$totalRevenueLabel: ${MoneyFormat.format(report.totalSales, currencyCode)}")
        appendLine("----------------------------------------")
        appendLine("[2. CASH DRAWER RECONCILIATION]")
        if (startingCash > 0) {
            appendLine("Starting Cash: ${MoneyFormat.format(startingCash, currencyCode)}")
        }
        appendLine("(+) $grossCashLabel: ${MoneyFormat.format(report.grossCash, currencyCode)}")
        if (cashIn > 0) {
            appendLine("(+) Cash In: ${MoneyFormat.format(cashIn, currencyCode)}")
        }
        appendLine("(-) Cash Out: ${MoneyFormat.format(cashOut, currencyCode)}")
        appendLine("$netCashLabel: ${MoneyFormat.format(expectedCashInDrawer, currencyCode)}")
        if (actualCashCount != null) {
            appendLine("Actual Cash Count: ${MoneyFormat.format(actualCashCount, currencyCode)}")
            val varText = when {
                variance == null -> "N/A"
                variance == 0.0 -> "${MoneyFormat.format(0.0, currencyCode)} (Balanced)"
                variance > 0.0 -> "+${MoneyFormat.format(variance, currencyCode)} (Over)"
                else -> "-${MoneyFormat.format(Math.abs(variance), currencyCode)} (Short)"
            }
            appendLine("Variance (Over/Short): $varText")
        }
        appendLine("----------------------------------------")
        appendLine("[3. TAX & VAT SUMMARY (15% VAT)]")
        val netTax = report.totalSales / 1.15
        val vatTax = report.totalSales - netTax
        appendLine("Net Taxable Sales: ${MoneyFormat.format(netTax, currencyCode)}")
        appendLine("VAT Amount (15%): ${MoneyFormat.format(vatTax, currencyCode)}")
        appendLine("Total Gross (Incl. VAT): ${MoneyFormat.format(report.totalSales, currencyCode)}")
        appendLine("----------------------------------------")
        appendLine("[4. OTHER TRACKING]")
        appendLine("$dueSalesLabel: ${MoneyFormat.format(report.totalDueCredit, currencyCode)}")
        appendLine("$staffMealsLabel: ${report.staffMealsCount}")
        val cleanNotes = PdfReportGenerator.cleanDisplayNotes(report.notes)
        if (cleanNotes.isNotBlank()) {
            appendLine("$notesLabel: $cleanNotes")
        }
        appendLine("========================================")
        appendLine("  This is a computer-generated shift report")
        appendLine("         System: Lojia POS")
        appendLine("========================================")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = AccentRose,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.preview_pdf),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (printerErrorMessage != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.PrintDisabled,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Printer Warning",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = printerErrorMessage ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            if (onOpenPrinterSettings != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        onDismiss()
                                        onOpenPrinterSettings()
                                    },
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Open Printer Settings", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // Success banner if exported
                if (exportSuccessMessage != null) {
                    Surface(
                        color = AccentEmerald.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AccentEmerald.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentEmerald, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.pdf_exported_success),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = AccentEmerald
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = exportSuccessMessage ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Surface(
                    color = PureWhite,
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = reportText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Thermal Print Z-Report Button
                Button(
                    onClick = {
                        printerErrorMessage = null
                        if (!printerManager.isPrinterConfigured()) {
                            printerErrorMessage = "No Bluetooth thermal printer selected or connected. Please configure a printer in Settings."
                        } else {
                            isPrinting = true
                            scope.launch {
                                try {
                                    val zReportText = printerManager.buildZReportText(
                                        businessName = businessProfile?.businessName?.ifBlank { "Lojia Store" } ?: "Lojia Store",
                                        businessAddress = businessProfile?.address ?: "",
                                        businessPhone = businessProfile?.phone ?: "",
                                        vatNumber = businessProfile?.vatNumber ?: "",
                                        report = report,
                                        currencySymbol = currencyCode ?: "$"
                                    )

                                    val res = printerManager.printFormattedText(zReportText)
                                    isPrinting = false
                                    res.fold(
                                        onSuccess = {
                                            Toast.makeText(context, "Z-Report sent to Bluetooth printer!", Toast.LENGTH_SHORT).show()
                                        },
                                        onFailure = { err ->
                                            printerErrorMessage = "Printer error: ${err.message ?: "Could not connect to printer."}"
                                        }
                                    )
                                } catch (e: Exception) {
                                    isPrinting = false
                                    printerErrorMessage = "Print error: ${e.message}"
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isPrinting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("print_z_report_btn")
                ) {
                    if (isPrinting) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = PureWhite, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Printing Z-Report...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Print Z-Report (Shift Close)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Export / Save PDF Button
                    Button(
                        onClick = {
                            isExporting = true
                            val result = PdfReportGenerator.generateSingleShiftReportPdf(
                                context = context,
                                report = report,
                                businessProfile = businessProfile,
                                language = language
                            )
                            isExporting = false
                            if (result.isSuccess) {
                                exportedUri = result.uri
                                exportSuccessMessage = "Saved to: ${result.displayPath}"
                                Toast.makeText(context, context.getString(R.string.pdf_report_exported_success), Toast.LENGTH_SHORT).show()
                                // Auto open if uri available
                                result.uri?.let { uri ->
                                    PdfReportGenerator.openPdfFile(context, uri)
                                }
                            } else {
                                Toast.makeText(context, context.getString(R.string.export_failed_msg, result.errorMessage), Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp)
                            .testTag("export_pdf_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                if (exportedUri != null) Icons.Default.Visibility else Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (exportedUri != null) stringResource(R.string.open_pdf)
                                else stringResource(R.string.export_pdf),
                                maxLines = 1,
                                softWrap = false,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Share Button
                    OutlinedButton(
                        onClick = {
                            if (exportedUri != null) {
                                PdfReportGenerator.sharePdfFile(context, exportedUri!!, context.getString(R.string.shift_report_title_fmt, report.cashierName))
                            } else {
                                // Generate first then share
                                val result = PdfReportGenerator.generateSingleShiftReportPdf(context, report, businessProfile, language)
                                if (result.isSuccess && result.uri != null) {
                                    exportedUri = result.uri
                                    PdfReportGenerator.sharePdfFile(context, result.uri, context.getString(R.string.shift_report_title_fmt, report.cashierName))
                                } else {
                                    val sendIntent: Intent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, reportText)
                                        type = "text/plain"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, "Share Shift Report").apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(shareIntent)
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp)
                            .testTag("share_report_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.share),
                                maxLines = 1,
                                softWrap = false,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        },
        dismissButton = null
    )
}

@Composable
fun SaleReceiptPreviewDialog(
    sale: POSSale,
    businessProfile: BusinessProfile?,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onOpenPrinterSettings: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember(context) { AppDatabase.getInstance(context) }
    val printerManager = remember(context) { com.lojia.pos.printer.BluetoothPrinterManager(context) }
    val receiptConfigState by db.reportDao().getReceiptConfig().collectAsState(initial = null)

    val currencyCode = businessProfile?.currency
    val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    var exportedUri by remember { mutableStateOf<Uri?>(null) }
    var exportSuccessMessage by remember { mutableStateOf<String?>(null) }
    var printerErrorMessage by remember { mutableStateOf<String?>(null) }
    var isPrinting by remember { mutableStateOf(false) }

    val isVoided = sale.isVoided
    val taxInvoiceTitle = if (isVoided) "VOIDED / REFUNDED INVOICE" else stringResource(R.string.simplified_tax_invoice)
    val subtotalLabel = stringResource(R.string.subtotal_exclusive_vat)
    val taxRateStr = if (businessProfile?.vatRate != null && businessProfile.vatRate > 0.0) "${businessProfile.vatRate}%" else ""
    val vatLabel = if (taxRateStr.isNotEmpty()) "Tax/VAT ($taxRateStr)" else stringResource(R.string.vat_amount_15)
    val totalLabel = stringResource(R.string.total_amount_due_inc_vat)
    val defaultBiz = stringResource(R.string.default_business_name)
    val thankYouMsg = stringResource(R.string.pdf_thank_you_visit, businessProfile?.businessName ?: defaultBiz)

    val receiptText = buildString {
        appendLine("========================================")
        appendLine("        ${businessProfile?.businessName ?: defaultBiz}")
        if (!businessProfile?.address.isNullOrBlank()) {
            appendLine("        ${businessProfile?.address}")
        }
        if (!businessProfile?.phone.isNullOrBlank()) {
            appendLine("        Tel: ${businessProfile?.phone}")
        }
        appendLine("        $taxInvoiceTitle")
        appendLine("========================================")
        if (isVoided) {
            appendLine("*** THIS SALE HAS BEEN VOIDED / REFUNDED ***")
            if (sale.voidReason.isNotBlank()) {
                appendLine("Reason: ${sale.voidReason}")
            }
            appendLine("----------------------------------------")
        }
        appendLine("${stringResource(R.string.invoice_number)}: ${sale.invoiceNumber}")
        appendLine("${stringResource(R.string.date_2)}: ${dateFormatter.format(Date(sale.timestamp))}")
        appendLine("${stringResource(R.string.cashier_6)}: ${sale.cashierName}")
        if (sale.customerName.isNotBlank() && sale.customerName != "Walk-in Customer") {
            appendLine("Customer: ${sale.customerName}")
        }
        if (!businessProfile?.vatNumber.isNullOrBlank()) {
            appendLine("Tax ID / VAT: ${businessProfile?.vatNumber}")
        }
        appendLine("${stringResource(R.string.payment_method)}: ${sale.paymentMethod}")
        appendLine("----------------------------------------")
        appendLine("$subtotalLabel:   ${MoneyFormat.format(sale.subtotal, currencyCode)}")
        if (businessProfile?.isTaxEnabled != false || sale.vatAmount > 0.0) {
            appendLine("$vatLabel:              ${MoneyFormat.format(sale.vatAmount, currencyCode)}")
        }
        appendLine("$totalLabel:              ${MoneyFormat.format(sale.totalAmount, currencyCode)}")
        appendLine("========================================")
        appendLine("        $thankYouMsg")
        appendLine("========================================")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = PrimaryIndigo,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.invoice),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (printerErrorMessage != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.PrintDisabled,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Printer Warning",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = printerErrorMessage ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            if (onOpenPrinterSettings != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        onDismiss()
                                        onOpenPrinterSettings()
                                    },
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Open Printer Settings", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                if (exportSuccessMessage != null) {
                    Surface(
                        color = AccentEmerald.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AccentEmerald.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentEmerald, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.pdf_exported_success),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = AccentEmerald
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = exportSuccessMessage ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Surface(
                    color = PureWhite,
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = receiptText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Main Action: Print Receipt
                Button(
                    onClick = {
                        printerErrorMessage = null
                        if (!printerManager.isPrinterConfigured()) {
                            printerErrorMessage = "No Bluetooth thermal printer selected or connected. Please configure a printer in Settings."
                        } else {
                            isPrinting = true
                            scope.launch {
                                try {
                                    val saleItems = withContext(Dispatchers.IO) {
                                        db.posDao().getSaleItems(sale.id)
                                    }

                                    val formattedItems: List<Pair<String, Pair<Double, Double>>> = saleItems.map { item ->
                                        Pair(item.productName, Pair(item.quantity, item.totalPrice))
                                    }

                                    val formattedText = printerManager.buildReceiptText(
                                        businessName = businessProfile?.businessName?.ifBlank { "Lojia Store" } ?: "Lojia Store",
                                        businessAddress = businessProfile?.address ?: "",
                                        businessPhone = businessProfile?.phone ?: "",
                                        vatNumber = businessProfile?.vatNumber ?: "",
                                        customHeader = receiptConfigState?.customHeader ?: "",
                                        customFooterText = receiptConfigState?.customFooterText ?: "",
                                        showTaxNumber = receiptConfigState?.showTaxNumber ?: true,
                                        showCashierName = receiptConfigState?.showCashierName ?: true,
                                        receiptId = sale.invoiceNumber,
                                        dateTimeStr = dateFormatter.format(Date(sale.timestamp)),
                                        cashierName = sale.cashierName,
                                        customerName = sale.customerName,
                                        items = formattedItems,
                                        subtotal = sale.subtotal,
                                        discount = 0.0,
                                        tax = sale.vatAmount,
                                        grandTotal = sale.totalAmount,
                                        paymentMethod = sale.paymentMethod,
                                        currencySymbol = currencyCode ?: "$"
                                    )

                                    val printResult = printerManager.printFormattedText(formattedText)
                                    isPrinting = false
                                    printResult.fold(
                                        onSuccess = {
                                            Toast.makeText(context, "Receipt sent to printer!", Toast.LENGTH_SHORT).show()
                                        },
                                        onFailure = { err ->
                                            printerErrorMessage = "Printer error: ${err.message ?: "Failed to send to printer."}"
                                        }
                                    )
                                } catch (e: Exception) {
                                    isPrinting = false
                                    printerErrorMessage = "Print error: ${e.message}"
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isPrinting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("print_thermal_receipt_btn")
                ) {
                    if (isPrinting) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = PureWhite, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Printing Receipt...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Print Receipt", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            val result = PdfReportGenerator.generateSaleInvoicePdf(
                                context = context,
                                sale = sale,
                                businessProfile = businessProfile
                            )
                            if (result.isSuccess) {
                                exportedUri = result.uri
                                exportSuccessMessage = "Saved to: ${result.displayPath}"
                                Toast.makeText(context, context.getString(R.string.invoice_pdf_saved), Toast.LENGTH_SHORT).show()
                                result.uri?.let { uri ->
                                    PdfReportGenerator.openPdfFile(context, uri)
                                }
                            } else {
                                Toast.makeText(context, context.getString(R.string.export_failed_msg, result.errorMessage), Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp)
                            .testTag("export_invoice_pdf_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                if (exportedUri != null) Icons.Default.Visibility else Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (exportedUri != null) stringResource(R.string.open_pdf)
                                else stringResource(R.string.export_pdf),
                                maxLines = 1,
                                softWrap = false,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            if (exportedUri != null) {
                                PdfReportGenerator.sharePdfFile(context, exportedUri!!, context.getString(R.string.tax_invoice_title_fmt, sale.invoiceNumber))
                            } else {
                                val result = PdfReportGenerator.generateSaleInvoicePdf(context, sale, businessProfile)
                                if (result.isSuccess && result.uri != null) {
                                    exportedUri = result.uri
                                    PdfReportGenerator.sharePdfFile(context, result.uri, context.getString(R.string.tax_invoice_title_fmt, sale.invoiceNumber))
                                } else {
                                    val sendIntent: Intent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, receiptText)
                                        type = "text/plain"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, "Share Receipt").apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(shareIntent)
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.share),
                                maxLines = 1,
                                softWrap = false,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        },
        dismissButton = null
    )
}
