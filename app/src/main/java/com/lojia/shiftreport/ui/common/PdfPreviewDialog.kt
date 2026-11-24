package com.lojia.shiftreport.ui.common

import android.content.Intent
import android.net.Uri
import android.widget.Toast

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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

import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import com.lojia.shiftreport.R
import com.lojia.shiftreport.auth.AdminAuthDialog
import com.lojia.shiftreport.data.*
import com.lojia.shiftreport.report.EmployerAdvanceStatement
import com.lojia.shiftreport.report.isBankAdvanceMode
import com.lojia.shiftreport.report.isCashAdvanceMode
import com.lojia.shiftreport.ui.theme.*
import com.lojia.shiftreport.util.MoneyFormat
import com.lojia.shiftreport.util.PdfLogoRenderer
import com.lojia.shiftreport.util.PdfReportGenerator
import com.lojia.shiftreport.util.PdfShareUtils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
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
    val formattedDate = remember(report.dateInMillis) {
        com.lojia.shiftreport.util.DateTimeFormatUtils.formatIsoDateTime(report.dateInMillis)
    }

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

    val resolvedCurrencyCode = MoneyFormat.resolveCurrency(currencyCode)
    val currencyDigits = MoneyFormat.minorUnitDigits(resolvedCurrencyCode)
    val startingCash = PdfReportGenerator.extractStartingCashMinor(report, currencyDigits)
    val actualCashCount = PdfReportGenerator.extractActualCashMinor(report, currencyDigits)
    val cashIn = report.totalDueCollectedCash
    val cashOut = report.totalCashOut
    val expectedCashInDrawer = MoneyFormat.calculateExpectedCashMinor(
        startingCash = startingCash,
        cashSales = report.grossCash,
        payIn = cashIn,
        payOut = cashOut
    )
    val variance = actualCashCount?.let { MoneyFormat.calculateVarianceMinor(it, expectedCashInDrawer) }

    val defaultBizName = stringResource(R.string.default_business_name)
    val businessName = businessProfile?.businessName?.ifBlank { defaultBizName } ?: defaultBizName
    val cleanNotes = PdfReportGenerator.cleanDisplayNotes(report.notes)

    val style = LocalDepthStyle.current

    LojiaDialog(
        onDismissRequest = onDismiss,
        maxWidth = Dimens.DialogMaxWidth
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.DialogPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = Dimens.MinTouchTarget),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = null,
                        tint = PrimaryIndigoLight,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.report_preview_title),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OnSurfaceLight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.close),
                        tint = OnSurfaceVariantLight,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            HorizontalDivider(color = OutlineVariantLight)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            ) {
                // Company header block (depth gradient banner)
                LojiaGradientHeader(
                    shape = RoundedCornerShape(Dimens.RadiusMd),
                    contentPadding = PaddingValues(12.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
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
                            text = titleLabel.uppercase(),
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
                PreviewSection(title = stringResource(R.string.pdf_sales_summary_title)) {
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
                PreviewSection(title = stringResource(R.string.cash_drawer_recon_title)) {
                    if (startingCash > 0L) {
                        PreviewRow(
                            label = stringResource(R.string.pdf_starting_cash_short),
                            value = MoneyFormat.formatMinor(startingCash, currencyCode)
                        )
                    }
                    PreviewRow(
                        label = stringResource(R.string.gross_cash_label),
                        value = "+${MoneyFormat.formatMinor(report.grossCash, currencyCode)}",
                        color = SuccessGreen
                    )
                    if (cashIn > 0L) {
                        PreviewRow(
                            label = stringResource(R.string.cash_in_label),
                            value = "+${MoneyFormat.formatMinor(cashIn, currencyCode)}",
                            color = SuccessGreen
                        )
                    }
                    PreviewRow(
                        label = stringResource(R.string.cash_out_label),
                        value = "-${MoneyFormat.formatMinor(cashOut, currencyCode)}",
                        color = ErrorRedLight
                    )
                    PreviewDivider()
                    PreviewRow(
                        label = netCashLabel,
                        value = MoneyFormat.formatMinor(expectedCashInDrawer, currencyCode),
                        bold = true,
                        highlight = SuccessGreen
                    )
                    if (actualCashCount != null) {
                        PreviewRow(
                            label = stringResource(R.string.pdf_actual_cash_count_label),
                            value = MoneyFormat.formatMinor(actualCashCount, currencyCode)
                        )
                        val (varText, varColor) = when {
                            variance == null -> stringResource(R.string.not_available_short) to OnSurfaceLight
                            variance == 0L -> "${MoneyFormat.formatMinor(0L, currencyCode)} (${stringResource(R.string.zreport_balanced)})" to SuccessGreen
                            variance > 0L -> "+${MoneyFormat.formatMinor(variance, currencyCode)} (${stringResource(R.string.zreport_over)})" to SuccessGreen
                            else -> "-${MoneyFormat.formatMinor(Math.abs(variance), currencyCode)} (${stringResource(R.string.zreport_short)})" to ErrorRedLight
                        }
                        PreviewRow(
                            label = stringResource(R.string.pdf_variance_over_short),
                            value = varText,
                            bold = true,
                            color = varColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Other Tracking section
                PreviewSection(title = stringResource(R.string.other_tracking_title)) {
                    PreviewRow(
                        label = dueSalesLabel,
                        value = MoneyFormat.formatMinor(report.totalDueCredit, currencyCode)
                    )
                    PreviewRow(
                        label = staffMealsLabel,
                        value = stringResource(R.string.pdf_staff_meals_persons_fmt, report.staffMealsCount)
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
                    text = stringResource(R.string.pdf_computer_generated_footer),
                    fontSize = 9.sp,
                    color = OnSurfaceVariantLight,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            HorizontalDivider(color = OutlineVariantLight)

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Warning banner (if printer error)
                if (printerErrorMessage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .depthSemanticCard(
                                backgroundColor = ErrorContainer,
                                borderColor = ErrorRedLight.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(Dimens.RadiusSm)
                            )
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
                                    text = stringResource(R.string.printer_warning_title),
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
                                    border = null,
                                    elevation = null,
                                    shape = RoundedCornerShape(Dimens.RadiusSm),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = Color.Transparent,
                                        contentColor = PrimaryIndigoDark
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .align(Alignment.End)
                                        .heightIn(min = Dimens.MinTouchTarget)
                                        .depthSecondaryButton(
                                            borderColor = PrimaryIndigoLight,
                                            cornerRadius = Dimens.RadiusSm
                                        )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = null,
                                        tint = PrimaryIndigoDark,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(R.string.open_printer_settings),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = PrimaryIndigoDark
                                    )
                                }
                            }
                        }
                    }
                }

                // Success banner (if PDF exported)
                if (exportSuccessMessage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .depthSemanticCard(
                                backgroundColor = SuccessContainer,
                                borderColor = SuccessGreen.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(Dimens.RadiusSm)
                            )
                            .clickable {
                                val reportsDir = com.lojia.shiftreport.util.PdfShareUtils.getAppReportsDirectory(context)
                                com.lojia.shiftreport.util.PdfShareUtils.openFolderInFileManager(context, reportsDir)
                            }
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = SuccessTextDark,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.pdf_exported_success),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SuccessTextDark
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = exportSuccessMessage ?: "",
                                fontSize = 11.sp,
                                color = OnSurfaceLight
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.tap_to_open_folder),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SuccessTextDark
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
                                            businessName = businessProfile?.businessName?.ifBlank { defaultBizName } ?: defaultBizName,
                                            businessAddress = businessProfile?.address ?: "",
                                            businessPhone = businessProfile?.phone ?: "",
                                            vatNumber = businessProfile?.vatNumber ?: "",
                                            report = report,
                                            currencySymbol = resolvedCurrencyCode,
                                            currencyCode = resolvedCurrencyCode
                                        )

                                        val res = printerManager.printFormattedText(zReportText)
                                        isPrinting = false
                                        res.fold(
                                            onSuccess = { fullSuccess ->
                                                if (fullSuccess) {
                                                    Toast.makeText(context, context.getString(R.string.printer_zreport_sent), Toast.LENGTH_SHORT).show()
                                                } else {
                                                    printerErrorMessage = context.getString(R.string.printer_warning_message)
                                                }
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
                            containerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            contentColor = PureWhite,
                            disabledContentColor = OnSurfaceVariantLight
                        ),
                        elevation = null,
                        shape = RoundedCornerShape(Dimens.RadiusMd),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        enabled = !isPrinting,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = Dimens.MinTouchTarget)
                            .depthRaisedButton(enabled = !isPrinting, cornerRadius = Dimens.RadiusMd)
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
                                val folderName = try {
                                    context.getString(R.string.app_reports_folder_name)
                                } catch (e: Exception) {
                                    "Lojia Reports"
                                }
                                exportSuccessMessage = context.getString(R.string.pdf_saved_to_folder, folderName)
                                Toast.makeText(context, exportSuccessMessage ?: "", Toast.LENGTH_LONG).show()
                                result.uri?.let { uri ->
                                    PdfReportGenerator.openPdfFile(context, uri)
                                }
                            } else {
                                Toast.makeText(context, context.getString(R.string.export_failed_msg, result.errorMessage), Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            contentColor = PureWhite,
                            disabledContentColor = OnSurfaceVariantLight
                        ),
                        elevation = null,
                        shape = RoundedCornerShape(Dimens.RadiusMd),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        enabled = !isExporting,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = Dimens.MinTouchTarget)
                            .depthRaisedButton(enabled = !isExporting, cornerRadius = Dimens.RadiusMd)
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
                                text = stringResource(R.string.btn_export),
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
                                    val shareIntent = Intent.createChooser(sendIntent, context.getString(R.string.share_shift_report_title)).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(shareIntent)
                                }
                            }
                        },
                        shape = RoundedCornerShape(Dimens.RadiusMd),
                        border = null,
                        elevation = null,
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = PrimaryIndigoDark
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = Dimens.MinTouchTarget)
                            .depthSecondaryButton(borderColor = style.secondaryButtonBorderColor, cornerRadius = Dimens.RadiusMd)
                            .testTag("share_report_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = stringResource(R.string.share),
                                tint = PrimaryIndigoDark,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.btn_share),
                                maxLines = 1,
                                softWrap = false,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryIndigoDark,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
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

private enum class StatementActionType {
    OPEN_PDF,
    PRINT,
    SHARE
}

@Composable
fun EmployerAdvanceStatementPreviewDialog(
    statement: EmployerAdvanceStatement,
    businessProfile: BusinessProfile?,
    periodLabel: String,
    language: AppLanguage,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val activeLocale = remember(configuration, language) {
        Locale.forLanguageTag(language.code)
    }
    val zone = remember { ZoneId.systemDefault() }
    val generatedMillis = remember { System.currentTimeMillis() }
    val scope = rememberCoroutineScope()

    val currencyCode = MoneyFormat.resolveCurrency(businessProfile?.currency)
    val businessName = businessProfile?.businessName?.trim()?.takeIf { it.isNotEmpty() }
    val logoBitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(
        initialValue = null,
        key1 = businessProfile?.logoUri
    ) {
        val rawUri = businessProfile?.logoUri?.takeIf { it.isNotBlank() }
        value = if (rawUri == null) {
            null
        } else {
            withContext(Dispatchers.IO) {
                PdfLogoRenderer.loadBusinessLogoBitmap(context, rawUri, 160)?.let { loaded ->
                    PdfLogoRenderer.getCircularBitmap(loaded, 120).asImageBitmap()
                }
            }
        }
    }

    val generatedText = remember(generatedMillis, activeLocale, zone) {
        Instant.ofEpochMilli(generatedMillis).atZone(zone)
            .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(activeLocale))
    }
    val entryDateFormatter = remember(activeLocale) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(activeLocale)
    }

    var tempPdfResult by remember { mutableStateOf<PdfReportGenerator.ExportResult?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var pendingAuthAction by remember { mutableStateOf<StatementActionType?>(null) }

    val closeAndCleanup = {
        PdfShareUtils.deleteTempStatementFile(tempPdfResult?.file)
        tempPdfResult = null
        onDismiss()
    }

    DisposableEffect(Unit) {
        onDispose {
            PdfShareUtils.deleteTempStatementFile(tempPdfResult?.file)
        }
    }

    suspend fun ensureTempPdf(): PdfReportGenerator.ExportResult {
        val cached = tempPdfResult
        if (cached != null && cached.isSuccess && cached.file?.exists() == true && cached.uri != null) {
            return cached
        }
        val generated = withContext(Dispatchers.IO) {
            PdfReportGenerator.generateEmployerAdvanceStatementPdf(
                context = context,
                statement = statement,
                businessProfile = businessProfile,
                periodLabel = periodLabel,
                language = language,
                zone = zone,
                nowMillis = generatedMillis
            )
        }
        if (generated.isSuccess) {
            tempPdfResult = generated
        }
        return generated
    }

    fun executeAuthorizedAction(action: StatementActionType) {
        if (isProcessing) return
        isProcessing = true
        scope.launch {
            val result = ensureTempPdf()
            isProcessing = false
            val file = result.file
            val uri = result.uri
            if (!result.isSuccess || file == null || uri == null) {
                Toast.makeText(
                    context,
                    context.getString(
                        R.string.export_failed_msg,
                        result.errorMessage ?: context.getString(R.string.pdf_write_failed_error)
                    ),
                    Toast.LENGTH_LONG
                ).show()
                return@launch
            }
            when (action) {
                StatementActionType.OPEN_PDF -> PdfShareUtils.openPdfFile(context, uri)
                StatementActionType.PRINT -> PdfShareUtils.printPdfFile(
                    context = context,
                    pdfFile = file,
                    jobName = context.getString(R.string.advance_statement_title)
                )
                StatementActionType.SHARE -> PdfShareUtils.sharePdfFile(
                    context = context,
                    uri = uri,
                    title = context.getString(R.string.advance_statement_title)
                )
            }
        }
    }

    val style = LocalDepthStyle.current

    LojiaDialog(
        onDismissRequest = closeAndCleanup,
        maxWidth = Dimens.DialogMaxWidth,
        modifier = Modifier.testTag("employer_statement_preview_dialog")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.DialogPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = PrimaryIndigoLight,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.advance_statement_title),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OnSurfaceLight,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = closeAndCleanup,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.close),
                        tint = OnSurfaceVariantLight,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            HorizontalDivider(color = OutlineVariantLight)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LojiaGradientHeader(
                    shape = RoundedCornerShape(Dimens.RadiusLg),
                    cornerRadiusForShadow = Dimens.RadiusLg,
                    contentPadding = PaddingValues(Dimens.SpacingMd)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            if (businessName != null) {
                                Text(
                                    text = businessName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PureWhite,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = stringResource(R.string.advance_statement_title),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PureWhite.copy(alpha = 0.95f)
                            )
                            Text(
                                text = "${stringResource(R.string.statement_period_label)}: $periodLabel",
                                fontSize = 11.sp,
                                color = PureWhite.copy(alpha = 0.9f)
                            )
                            Text(
                                text = "${stringResource(R.string.statement_generated_at_label)}: $generatedText",
                                fontSize = 10.sp,
                                color = PureWhite.copy(alpha = 0.85f)
                            )
                        }
                        logoBitmap?.let { bmp ->
                            Image(
                                bitmap = bmp,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                            )
                        }
                    }
                }

                if (statement.employees.isEmpty() && statement.otherBucket == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .depthSemanticCard(
                                backgroundColor = DepthTokens.InsetFieldBackground,
                                borderColor = OutlineLight,
                                shape = RoundedCornerShape(Dimens.RadiusMd)
                            )
                            .testTag("statement_empty_period_card")
                    ) {
                        Text(
                            text = stringResource(R.string.statement_empty_period_msg),
                            fontSize = 13.sp,
                            color = OnSurfaceVariantLight,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    statement.employees.forEach { section ->
                        PreviewSection(title = section.employeeName) {
                            PreviewRow(
                                label = stringResource(R.string.cash),
                                value = MoneyFormat.formatMinor(section.cashTotalMinor, currencyCode, activeLocale),
                                color = SuccessGreen
                            )
                            PreviewRow(
                                label = stringResource(R.string.bank),
                                value = MoneyFormat.formatMinor(section.bankTotalMinor, currencyCode, activeLocale),
                                color = InfoBlue
                            )
                            PreviewRow(
                                label = stringResource(R.string.total),
                                value = "${MoneyFormat.formatMinor(section.grandTotalMinor, currencyCode, activeLocale)} (${stringResource(R.string.advance_entries_count_fmt, section.entryCount)})",
                                bold = true,
                                highlight = PrimaryIndigoLight
                            )
                            if (statement.isSingleCalendarMonth) {
                                PreviewRow(
                                    label = stringResource(R.string.advance_monthly_salary_label),
                                    value = MoneyFormat.formatMinor(section.monthlySalaryMinor ?: 0L, currencyCode, activeLocale)
                                )
                                PreviewRow(
                                    label = stringResource(R.string.advance_taken_label),
                                    value = stringResource(
                                        R.string.advance_minus_amount_fmt,
                                        MoneyFormat.formatMinor(section.advancesInMonthMinor ?: 0L, currencyCode, activeLocale)
                                    ),
                                    color = WarningOrange
                                )
                                val exc = section.excessMinor ?: 0L
                                val rem = section.remainingMinor ?: 0L
                                if (exc > 0L) {
                                    PreviewRow(
                                        label = stringResource(R.string.advance_excess_label),
                                        value = MoneyFormat.formatMinor(exc, currencyCode, activeLocale),
                                        bold = true,
                                        color = ErrorRedLight
                                    )
                                } else {
                                    PreviewRow(
                                        label = stringResource(R.string.advance_remaining_label),
                                        value = MoneyFormat.formatMinor(rem, currencyCode, activeLocale),
                                        bold = true,
                                        color = SuccessGreen
                                    )
                                }
                            }
                            PreviewDivider()
                            section.entries.forEach { entry ->
                                val modeText = when {
                                    isCashAdvanceMode(entry.paymentMode) -> stringResource(R.string.cash)
                                    isBankAdvanceMode(entry.paymentMode) -> stringResource(R.string.bank)
                                    else -> entry.paymentMode
                                }
                                val entryDate = Instant.ofEpochMilli(entry.dateInMillis)
                                    .atZone(zone)
                                    .format(entryDateFormatter)
                                val entryLine = "${stringResource(R.string.advance_serial_fmt, entry.serialNumber)} $entryDate • $modeText • ${entry.cashierName}"
                                PreviewRow(
                                    label = entryLine,
                                    value = MoneyFormat.formatMinor(entry.amountMinor, currencyCode, activeLocale),
                                    multiline = true
                                )
                            }
                        }
                    }

                    statement.otherBucket?.let { other ->
                        PreviewSection(title = stringResource(R.string.advance_other_bucket_title)) {
                            PreviewRow(
                                label = stringResource(R.string.cash),
                                value = MoneyFormat.formatMinor(other.cashTotalMinor, currencyCode, activeLocale),
                                color = SuccessGreen
                            )
                            PreviewRow(
                                label = stringResource(R.string.bank),
                                value = MoneyFormat.formatMinor(other.bankTotalMinor, currencyCode, activeLocale),
                                color = InfoBlue
                            )
                            PreviewRow(
                                label = stringResource(R.string.total),
                                value = "${MoneyFormat.formatMinor(other.advancesTotalMinor, currencyCode, activeLocale)} (${stringResource(R.string.advance_entries_count_fmt, other.advanceCount)})",
                                bold = true,
                                highlight = PrimaryIndigoLight
                            )
                            PreviewDivider()
                            other.history.forEach { entry ->
                                val modeText = when {
                                    isCashAdvanceMode(entry.paymentMode) -> stringResource(R.string.cash)
                                    isBankAdvanceMode(entry.paymentMode) -> stringResource(R.string.bank)
                                    else -> entry.paymentMode
                                }
                                val entryDate = Instant.ofEpochMilli(entry.dateInMillis)
                                    .atZone(zone)
                                    .format(entryDateFormatter)
                                val entryLine = "${stringResource(R.string.advance_serial_fmt, entry.serialNumber)} $entryDate • $modeText • ${entry.cashierName}"
                                PreviewRow(
                                    label = entryLine,
                                    value = MoneyFormat.formatMinor(entry.amountMinor, currencyCode, activeLocale),
                                    multiline = true
                                )
                            }
                        }
                    }
                }

                PreviewDivider()
                PreviewSection(title = stringResource(R.string.statement_summary_totals_title)) {
                    PreviewRow(
                        label = stringResource(R.string.cash),
                        value = MoneyFormat.formatMinor(statement.totalCashMinor, currencyCode, activeLocale),
                        color = SuccessGreen
                    )
                    PreviewRow(
                        label = stringResource(R.string.bank),
                        value = MoneyFormat.formatMinor(statement.totalBankMinor, currencyCode, activeLocale),
                        color = InfoBlue
                    )
                    PreviewRow(
                        label = stringResource(R.string.ledger_total_advances),
                        value = "${MoneyFormat.formatMinor(statement.totalAdvancesMinor, currencyCode, activeLocale)} (${stringResource(R.string.advance_entries_count_fmt, statement.totalEntryCount)})",
                        bold = true,
                        highlight = PrimaryIndigoLight
                    )
                }
            }

            HorizontalDivider(color = OutlineVariantLight)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { pendingAuthAction = StatementActionType.OPEN_PDF },
                    enabled = !isProcessing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        contentColor = PureWhite,
                        disabledContentColor = OnSurfaceVariantLight
                    ),
                    elevation = null,
                    shape = RoundedCornerShape(Dimens.RadiusReportInput),
                    contentPadding = PaddingValues(horizontal = Dimens.SpacingSm, vertical = Dimens.SpacingSm),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Dimens.ButtonHeightReportPrimary)
                        .depthRaisedButton(
                            enabled = !isProcessing,
                            cornerRadius = Dimens.RadiusReportInput,
                            minHeight = Dimens.ButtonHeightReportPrimary
                        )
                        .testTag("statement_open_pdf_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = stringResource(R.string.statement_cd_open_pdf),
                        tint = PureWhite,
                        modifier = Modifier.size(Dimens.IconXs + Dimens.SpacingXxs)
                    )
                    Spacer(modifier = Modifier.width(Dimens.SpacingXs))
                    Text(
                        text = stringResource(R.string.statement_open_pdf_btn),
                        maxLines = 1,
                        fontSize = DepthTypeTokens.RegisterLabelSize,
                        fontWeight = FontWeight.SemiBold,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = { pendingAuthAction = StatementActionType.PRINT },
                    enabled = !isProcessing,
                    shape = RoundedCornerShape(Dimens.RadiusReportInput),
                    border = null,
                    elevation = null,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = style.secondaryButtonBorderColor
                    ),
                    contentPadding = PaddingValues(horizontal = Dimens.SpacingSm, vertical = Dimens.SpacingSm),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Dimens.ButtonHeightReportPrimary)
                        .depthSecondaryButton(
                            enabled = !isProcessing,
                            borderColor = style.secondaryButtonBorderColor,
                            cornerRadius = Dimens.RadiusReportInput,
                            minHeight = Dimens.ButtonHeightReportPrimary
                        )
                        .testTag("statement_print_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Print,
                        contentDescription = stringResource(R.string.statement_cd_print),
                        tint = style.secondaryButtonBorderColor,
                        modifier = Modifier.size(Dimens.IconXs + Dimens.SpacingXxs)
                    )
                    Spacer(modifier = Modifier.width(Dimens.SpacingXs))
                    Text(
                        text = stringResource(R.string.print_btn),
                        maxLines = 1,
                        fontSize = DepthTypeTokens.RegisterLabelSize,
                        fontWeight = FontWeight.SemiBold,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Button(
                    onClick = { pendingAuthAction = StatementActionType.SHARE },
                    enabled = !isProcessing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        contentColor = PureWhite,
                        disabledContentColor = OnSurfaceVariantLight
                    ),
                    elevation = null,
                    shape = RoundedCornerShape(Dimens.RadiusReportInput),
                    contentPadding = PaddingValues(horizontal = Dimens.SpacingSm, vertical = Dimens.SpacingSm),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Dimens.ButtonHeightReportPrimary)
                        .depthRaisedButton(
                            enabled = !isProcessing,
                            cornerRadius = Dimens.RadiusReportInput,
                            minHeight = Dimens.ButtonHeightReportPrimary
                        )
                        .testTag("statement_share_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = stringResource(R.string.statement_cd_share),
                        tint = PureWhite,
                        modifier = Modifier.size(Dimens.IconXs + Dimens.SpacingXxs)
                    )
                    Spacer(modifier = Modifier.width(Dimens.SpacingXs))
                    Text(
                        text = stringResource(R.string.share),
                        maxLines = 1,
                        fontSize = DepthTypeTokens.RegisterLabelSize,
                        fontWeight = FontWeight.SemiBold,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }

    pendingAuthAction?.let { action ->
        AdminAuthDialog(
            onDismissRequest = { pendingAuthAction = null },
            onAuthSuccess = {
                val resolvedAction = action
                pendingAuthAction = null
                executeAuthorizedAction(resolvedAction)
            }
        )
    }
}
