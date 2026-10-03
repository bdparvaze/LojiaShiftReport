package com.lojia.shiftreport.ui.common

import android.content.Intent
import android.net.Uri
import android.widget.Toast

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties

import com.lojia.shiftreport.R
import com.lojia.shiftreport.data.*
import com.lojia.shiftreport.ui.theme.*
import com.lojia.shiftreport.util.MoneyFormat
import com.lojia.shiftreport.util.PdfReportGenerator

import kotlinx.coroutines.launch

import java.text.SimpleDateFormat
import java.util.*

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
    val printerManager = remember(context) { com.lojia.shiftreport.printer.BluetoothPrinterManager(context) }
    val currencyCode = businessProfile?.currency
    val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    var isExporting by remember { mutableStateOf(false) }
    var exportedUri by remember { mutableStateOf<Uri?>(null) }
    var exportSuccessMessage by remember { mutableStateOf<String?>(null) }
    var printerErrorMessage by remember { mutableStateOf<String?>(null) }
    var isPrinting by remember { mutableStateOf(false) }

    val bluetoothPermissionRequester = com.lojia.shiftreport.permission.rememberPermissionRequester(
        feature = com.lojia.shiftreport.permission.AppFeaturePermission.BLUETOOTH_PRINTER
    )

    val titleLabel = stringResource(R.string.shift_closing_revenue_cert)
    val grossCashLabel = stringResource(R.string.gross_cash_received)
    val madaLabel = stringResource(R.string.mada_bank_cards)
    val walletLabel = stringResource(R.string.digital_wallet_applepay)
    val totalRevenueLabel = stringResource(R.string.total_revenue_label)
    val netCashLabel = stringResource(R.string.net_cash_in_drawer_label)
    val dueSalesLabel = stringResource(R.string.due_sales_credit_entries)
    val staffMealsLabel = stringResource(R.string.staff_meals_count)
    val notesLabel = stringResource(R.string.notes)

    val startingCash = PdfReportGenerator.extractStartingCashFromNotes(report)
    val actualCashCount = PdfReportGenerator.extractActualCashFromNotes(report)
    val cashIn = MoneyFormat.toMajorUnits(report.totalDueCollectedCash)
    val cashOut = MoneyFormat.toMajorUnits(report.totalCashOut)
    val expectedCashInDrawer = startingCash + MoneyFormat.toMajorUnits(report.grossCash) + cashIn - cashOut
    val variance = actualCashCount?.let { it - expectedCashInDrawer }

    val defaultBizName = stringResource(R.string.default_business_name)
    val businessName = businessProfile?.businessName?.ifBlank { defaultBizName } ?: defaultBizName
    val formattedDate = dateFormatter.format(Date(report.dateInMillis))
    val cleanNotes = PdfReportGenerator.cleanDisplayNotes(report.notes)

    val isTax = businessProfile?.isTaxEnabled ?: false
    val isTaxIncluded = businessProfile?.isTaxIncluded ?: true
    val vatRate = if (isTax) (businessProfile?.vatRate ?: 15.0) else 0.0
    val totalSalesMajor = MoneyFormat.toMajorUnits(report.totalSales)
    val (netTax, vatTax, grossTax) = if (isTax && vatRate > 0.0) {
        if (isTaxIncluded) {
            val divisor = 1.0 + (vatRate / 100.0)
            val net = totalSalesMajor / divisor
            val vat = totalSalesMajor - net
            Triple(net, vat, totalSalesMajor)
        } else {
            val net = totalSalesMajor
            val vat = net * (vatRate / 100.0)
            Triple(net, vat, net + vat)
        }
    } else {
        Triple(totalSalesMajor, 0.0, totalSalesMajor)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.88f)
            .heightIn(max = 560.dp),
        containerColor = SurfaceLight,
        title = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = null,
                        tint = PrimaryIndigoLight,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Report Preview",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OnSurfaceLight
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.close),
                        tint = OnSurfaceVariantLight,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Company header block (banner style)
                Surface(
                    color = PrimaryIndigoLight,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = businessName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "SHIFT CLOSING & REVENUE CERTIFICATE",
                            fontSize = 9.sp,
                            color = PureWhite.copy(alpha = 0.9f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "$formattedDate • ${report.shift} • ${report.cashierName}",
                            fontSize = 8.sp,
                            color = PureWhite.copy(alpha = 0.85f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Sales Summary section
                PreviewSection(title = "Sales Summary") {
                    PreviewRow(
                        label = grossCashLabel,
                        value = MoneyFormat.formatMinor(report.grossCash, currencyCode),
                        bold = false
                    )
                    PreviewRow(
                        label = madaLabel,
                        value = MoneyFormat.formatMinor(report.madaPayments, currencyCode),
                        bold = false
                    )
                    if (report.digitalWallet > 0L) {
                        PreviewRow(
                            label = walletLabel,
                            value = MoneyFormat.formatMinor(report.digitalWallet, currencyCode),
                            bold = false
                        )
                    }
                    PreviewDivider()
                    PreviewRow(
                        label = totalRevenueLabel,
                        value = MoneyFormat.formatMinor(report.totalSales, currencyCode),
                        bold = true,
                        highlight = PrimaryIndigoLight
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Cash Drawer section
                PreviewSection(title = "Cash Drawer Reconciliation") {
                    if (startingCash > 0) {
                        PreviewRow(
                            label = "Starting Cash",
                            value = MoneyFormat.format(startingCash, currencyCode)
                        )
                    }
                    PreviewRow(
                        label = "(+) Gross Cash",
                        value = "+${MoneyFormat.formatMinor(report.grossCash, currencyCode)}",
                        color = SuccessGreen
                    )
                    if (cashIn > 0) {
                        PreviewRow(
                            label = "(+) Cash In",
                            value = "+${MoneyFormat.format(cashIn, currencyCode)}",
                            color = SuccessGreen
                        )
                    }
                    PreviewRow(
                        label = "(-) Cash Out",
                        value = "-${MoneyFormat.format(cashOut, currencyCode)}",
                        color = ErrorRedLight
                    )
                    PreviewDivider()
                    PreviewRow(
                        label = netCashLabel,
                        value = MoneyFormat.format(expectedCashInDrawer, currencyCode),
                        bold = true,
                        highlight = SuccessGreen
                    )
                    if (actualCashCount != null) {
                        PreviewRow(
                            label = "Actual Cash Count",
                            value = MoneyFormat.format(actualCashCount, currencyCode)
                        )
                        val (varText, varColor) = when {
                            variance == null -> "N/A" to OnSurfaceLight
                            variance == 0.0 -> "${MoneyFormat.format(0.0, currencyCode)} (Balanced)" to SuccessGreen
                            variance > 0.0 -> "+${MoneyFormat.format(variance, currencyCode)} (Over)" to SuccessGreen
                            else -> "-${MoneyFormat.format(Math.abs(variance), currencyCode)} (Short)" to ErrorRedLight
                        }
                        PreviewRow(
                            label = "Variance (Over/Short)",
                            value = varText,
                            bold = true,
                            color = varColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Tax & VAT section
                PreviewSection(title = "Tax & VAT Summary") {
                    if (isTax && vatRate > 0.0) {
                        PreviewRow(
                            label = "Net Taxable Sales",
                            value = MoneyFormat.format(netTax, currencyCode)
                        )
                        PreviewRow(
                            label = "VAT Amount (${String.format(Locale.US, "%.1f", vatRate)}%)",
                            value = MoneyFormat.format(vatTax, currencyCode)
                        )
                        val inclLabel = if (isTaxIncluded) "Incl. VAT" else "Excl. VAT"
                        PreviewDivider()
                        PreviewRow(
                            label = "Total Gross ($inclLabel)",
                            value = MoneyFormat.format(grossTax, currencyCode),
                            bold = true
                        )
                    } else {
                        PreviewRow(
                            label = "Tax Status",
                            value = "Non-Taxable / Tax Exempt"
                        )
                        PreviewRow(
                            label = "Net Total Sales",
                            value = MoneyFormat.formatMinor(report.totalSales, currencyCode),
                            bold = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Other Tracking section
                PreviewSection(title = "Other Tracking") {
                    PreviewRow(
                        label = dueSalesLabel,
                        value = MoneyFormat.formatMinor(report.totalDueCredit, currencyCode)
                    )
                    PreviewRow(
                        label = staffMealsLabel,
                        value = "${report.staffMealsCount} persons"
                    )
                    if (cleanNotes.isNotBlank()) {
                        PreviewRow(
                            label = notesLabel,
                            value = cleanNotes,
                            multiline = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Footer
                Text(
                    text = "Computer-generated report • Lojia POS",
                    fontSize = 9.sp,
                    color = TextHintColor,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Warning banner (if printer error)
                if (printerErrorMessage != null) {
                    Surface(
                        color = ErrorContainer,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, ErrorRedLight.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.PrintDisabled,
                                    contentDescription = null,
                                    tint = ErrorRedLight,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Printer Warning",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ErrorRedLight
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = printerErrorMessage ?: "",
                                fontSize = 11.sp,
                                color = OnSurfaceLight
                            )
                            if (onOpenPrinterSettings != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedButton(
                                    onClick = {
                                        onDismiss()
                                        onOpenPrinterSettings()
                                    },
                                    border = BorderStroke(1.dp, PrimaryIndigoLight),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryIndigoLight),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .align(Alignment.End)
                                        .height(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = null,
                                        tint = PrimaryIndigoLight,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Open Printer Settings",
                                        fontSize = 11.sp,
                                        color = PrimaryIndigoLight
                                    )
                                }
                            }
                        }
                    }
                }

                // Success banner (if PDF exported)
                if (exportSuccessMessage != null) {
                    Surface(
                        color = SuccessContainer,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = SuccessGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.pdf_exported_success),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SuccessGreen
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = exportSuccessMessage ?: "",
                                fontSize = 11.sp,
                                color = OnSurfaceVariantLight
                            )
                        }
                    }
                }

                // Compact single button row: Print / Export / Share
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Print Button
                    Button(
                        onClick = {
                            printerErrorMessage = null
                            if (!printerManager.isPrinterConfigured()) {
                                printerErrorMessage = context.getString(R.string.printer_not_configured)
                                return@Button
                            }

                            val executePrint = {
                                isPrinting = true
                                scope.launch {
                                    try {
                                        val zReportText = printerManager.buildZReportText(
                                            businessName = businessProfile?.businessName?.ifBlank { "Lojia Store" } ?: "Lojia Store",
                                            businessAddress = businessProfile?.address ?: "",
                                            businessPhone = businessProfile?.phone ?: "",
                                            vatNumber = businessProfile?.vatNumber ?: "",
                                            report = report,
                                            currencySymbol = currencyCode ?: "$",
                                            vatRate = businessProfile?.vatRate ?: 15.0,
                                            isTaxEnabled = businessProfile?.isTaxEnabled ?: false,
                                            isTaxIncluded = businessProfile?.isTaxIncluded ?: true
                                        )

                                        val res = printerManager.printFormattedText(zReportText)
                                        isPrinting = false
                                        res.fold(
                                            onSuccess = {
                                                Toast.makeText(context, context.getString(R.string.printer_zreport_sent), Toast.LENGTH_SHORT).show()
                                            },
                                            onFailure = { err ->
                                                printerErrorMessage = context.getString(
                                                    R.string.printer_error_prefix,
                                                    err.message ?: context.getString(R.string.printer_could_not_connect)
                                                )
                                            }
                                        )
                                    } catch (e: Exception) {
                                        isPrinting = false
                                        printerErrorMessage = context.getString(
                                            R.string.printer_error_prefix,
                                            e.message ?: context.getString(R.string.printer_could_not_connect)
                                        )
                                    }
                                }
                            }

                            if (printerManager.getPrinterConnectionType() == "bluetooth") {
                                bluetoothPermissionRequester.launch {
                                    executePrint()
                                }
                            } else {
                                executePrint()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SuccessGreen,
                            contentColor = PureWhite
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        enabled = !isPrinting,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("print_z_report_btn")
                    ) {
                        if (isPrinting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = PureWhite,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Print,
                                    contentDescription = stringResource(R.string.print_z_report_btn),
                                    tint = PureWhite,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.print_btn),
                                    maxLines = 1,
                                    softWrap = false,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PureWhite,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Export Button
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
                                result.uri?.let { uri ->
                                    PdfReportGenerator.openPdfFile(context, uri)
                                }
                            } else {
                                Toast.makeText(context, context.getString(R.string.export_failed_msg, result.errorMessage), Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryIndigoLight,
                            contentColor = PureWhite
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        enabled = !isExporting,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("export_pdf_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = stringResource(R.string.export_pdf),
                                tint = PureWhite,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Export",
                                maxLines = 1,
                                softWrap = false,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PureWhite,
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
                                val result = PdfReportGenerator.generateSingleShiftReportPdf(context, report, businessProfile, language)
                                if (result.isSuccess && result.uri != null) {
                                    exportedUri = result.uri
                                    PdfReportGenerator.sharePdfFile(context, result.uri, context.getString(R.string.shift_report_title_fmt, report.cashierName))
                                } else {
                                    val fallbackSummary = "$businessName — $titleLabel ($formattedDate • ${report.shift} • ${report.cashierName}): ${MoneyFormat.formatMinor(report.totalSales, currencyCode)}"
                                    val sendIntent: Intent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, fallbackSummary)
                                        type = "text/plain"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, "Share Shift Report").apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(shareIntent)
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, PrimaryIndigoLight),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryIndigoLight),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("share_report_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = stringResource(R.string.share),
                                tint = PrimaryIndigoLight,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Share",
                                maxLines = 1,
                                softWrap = false,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryIndigoLight,
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
private fun PreviewSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(width = 3.dp, height = 14.dp)
                    .background(PrimaryIndigoLight, RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryIndigoDark,
                letterSpacing = 0.05.sp
            )
        }
        content()
    }
}

@Composable
private fun PreviewRow(
    label: String,
    value: String,
    bold: Boolean = false,
    color: Color? = null,
    highlight: Color? = null,
    multiline: Boolean = false
) {
    val rowContent: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (highlight != null) 4.dp else 0.dp)
                .padding(vertical = if (highlight != null) 0.dp else 3.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = if (multiline) Alignment.Top else Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                color = if (bold) OnSurfaceLight else OnSurfaceVariantLight,
                fontWeight = if (bold) FontWeight.Medium else FontWeight.Normal,
                modifier = if (multiline) Modifier.weight(1f) else Modifier.wrapContentWidth()
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = value,
                fontSize = 11.sp,
                color = highlight ?: color ?: OnSurfaceLight,
                fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
            )
        }
    }

    if (highlight != null) {
        Surface(
            color = highlight.copy(alpha = 0.10f),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
        ) {
            rowContent()
        }
    } else {
        rowContent()
    }
}

@Composable
private fun PreviewDivider() {
    HorizontalDivider(
        thickness = 0.5.dp,
        color = OutlineVariantLight,
        modifier = Modifier.padding(vertical = 2.dp)
    )
}
