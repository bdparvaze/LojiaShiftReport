package com.lojia.shiftreport.report

import com.lojia.shiftreport.util.DateTimeFormatUtils
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojia.shiftreport.R
import com.lojia.shiftreport.ui.common.SecureDeleteModal
import com.lojia.shiftreport.data.ShiftReport
import com.lojia.shiftreport.ui.theme.*
import com.lojia.shiftreport.util.MoneyFormat

@Composable
fun MetricCard(
    title: String,
    amount: String,
    icon: ImageVector,
    color: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = modifier.depthCard(shape = RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(title, fontSize = 11.sp, color = ShiftColors.TextMuted)
                Text(amount, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ShiftColors.Charcoal)
            }
        }
    }
}

/* ----------------------------------------------------------------------
 * TAB 6: SHIFT REPORT ARCHIVES VIEW
 * ---------------------------------------------------------------------- */
@Composable
fun ShiftReportArchivesTab(
    reports: List<ShiftReport>,
    userProfile: com.lojia.shiftreport.data.UserProfile? = null,
    currentCurrency: String = MoneyFormat.DEFAULT_CURRENCY_CODE,
    onPreviewPdf: (ShiftReport) -> Unit,
    onDeleteReport: (ShiftReport) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var reportToDelete by remember { mutableStateOf<ShiftReport?>(null) }

    val filteredReports = remember(reports, searchQuery) {
        if (searchQuery.isBlank()) reports
        else reports.filter {
            it.cashierName.contains(searchQuery, ignoreCase = true) ||
            it.shift.contains(searchQuery, ignoreCase = true) ||
            it.id.toString().contains(searchQuery)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Search Bar (depth inset well with >= 48.dp touch target)
        var isSearchFocused by remember { mutableStateOf(false) }
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color.Transparent,
            shadowElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Dimens.MinTouchTarget)
                .depthInset(
                    isFocused = isSearchFocused,
                    shape = RoundedCornerShape(10.dp)
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Dimens.MinTouchTarget)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = OnSurfaceVariantLight,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = stringResource(R.string.search_cashier_shift),
                            fontSize = 13.sp,
                            color = TextHintColor
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 13.sp,
                            color = OnSurfaceLight
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { isSearchFocused = it.isFocused }
                    )
                }
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { searchQuery = "" },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.btn_clear),
                            tint = OnSurfaceVariantLight,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        if (filteredReports.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
                    .cardRise(0)
                    .depthCard(shape = RoundedCornerShape(12.dp))
            ) {
                Column(
                    modifier = Modifier.padding(36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = BackgroundLight,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = OnSurfaceVariantLight
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = stringResource(R.string.no_archived_reports),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OnSurfaceLight,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                itemsIndexed(filteredReports, key = { _, it -> it.id }) { idx, report ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .cardRise(idx)
                            .depthCard(shape = RoundedCornerShape(12.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            // Row 1 (header row — top of card):
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryContainerLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = report.cashierName.take(1).uppercase(),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryIndigoLight
                                    )
                                }

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 10.dp, end = 8.dp)
                                ) {
                                    Text(
                                        text = report.cashierName,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = OnSurfaceLight,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = DateTimeFormatUtils.formatDateTime(report.dateInMillis),
                                        fontSize = 11.sp,
                                        color = TextHintColor,
                                        maxLines = 1
                                    )
                                }

                                val shiftNormalized = report.shift.trim()
                                val shiftDisplayName = when (shiftNormalized.lowercase()) {
                                    "morning" -> stringResource(R.string.shift_morning)
                                    "evening" -> stringResource(R.string.shift_evening)
                                    "night" -> stringResource(R.string.shift_night)
                                    "day" -> stringResource(R.string.shift_day)
                                    else -> report.shift
                                }
                                val shiftBg = when {
                                    shiftNormalized.equals("Morning", ignoreCase = true) -> SuccessContainer
                                    shiftNormalized.equals("Evening", ignoreCase = true) -> WarningContainer
                                    shiftNormalized.equals("Night", ignoreCase = true) -> PrimaryContainerLight
                                    else -> SurfaceVariantLight
                                }
                                val shiftFg = when {
                                    shiftNormalized.equals("Morning", ignoreCase = true) -> SuccessGreen
                                    shiftNormalized.equals("Evening", ignoreCase = true) -> WarningOrange
                                    shiftNormalized.equals("Night", ignoreCase = true) -> PrimaryIndigoDark
                                    else -> OnSurfaceVariantLight
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = shiftBg,
                                    modifier = Modifier
                                        .wrapContentWidth()
                                        .widthIn(min = 60.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = shiftDisplayName,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = shiftFg,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }

                                Spacer(Modifier.width(6.dp))

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = SurfaceVariantLight,
                                    border = BorderStroke(0.5.dp, OutlineLight)
                                ) {
                                    Text(
                                        text = "#${report.id}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = OnSurfaceVariantLight,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            // Row 2 (stat row — mid of card, top padding 12.dp)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    Text(
                                        text = stringResource(R.string.gross_revenue).uppercase(),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextHintColor,
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = MoneyFormat.formatMinor(report.totalSales, currentCurrency),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OnSurfaceLight,
                                        maxLines = 1
                                    )
                                }

                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = stringResource(R.string.mada_bank).uppercase(),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextHintColor
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = MoneyFormat.formatMinor(report.madaPayments, currentCurrency),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryIndigoLight,
                                        maxLines = 1
                                    )
                                }

                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = stringResource(R.string.net_cash_1).uppercase(),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextHintColor
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = MoneyFormat.formatMinor(report.netCash, currentCurrency),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SuccessGreen,
                                        maxLines = 1
                                    )
                                }
                            }

                            // Row 3 (action row — bottom of card, top padding 12.dp)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { reportToDelete = report },
                                    shape = RoundedCornerShape(8.dp),
                                    border = null,
                                    elevation = null,
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = Color.Transparent,
                                        contentColor = ErrorRedLight
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .heightIn(min = Dimens.MinTouchTarget)
                                        .depthSecondaryButton(
                                            borderColor = ErrorRedLight,
                                            cornerRadius = 8.dp
                                        )
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.DeleteOutline,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = ErrorRedLight
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(R.string.delete),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = ErrorRedLight,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }

                                Spacer(Modifier.width(8.dp))

                                Button(
                                    onClick = { onPreviewPdf(report) },
                                    shape = RoundedCornerShape(8.dp),
                                    elevation = null,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Transparent,
                                        contentColor = PureWhite
                                    ),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .heightIn(min = Dimens.MinTouchTarget)
                                        .depthRaisedButton(cornerRadius = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.PictureAsPdf,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = PureWhite
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(R.string.preview_pdf),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = PureWhite,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    reportToDelete?.let { report ->
        SecureDeleteModal(
            title = stringResource(R.string.delete_shift_report_title),
            itemDescription = stringResource(R.string.delete_shift_report_desc, report.id.toString(), report.cashierName),
            userProfile = userProfile,
            onDismiss = { reportToDelete = null },
            onConfirmDelete = {
                onDeleteReport(report)
                reportToDelete = null
            }
        )
    }
}

/* ----------------------------------------------------------------------
 * STICKY BOTTOM BAR
 * ---------------------------------------------------------------------- */
@Composable
fun StickySummaryBar(
    expectedCash: Long,
    variance: Long? = null,
    currentCurrency: String = MoneyFormat.DEFAULT_CURRENCY_CODE
) {
    val currency = currentCurrency
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = Dimens.CardElevation)
            .background(PureWhite)
            .border(width = Dimens.CardBorderWidth, color = DepthCardBorder)
            .padding(horizontal = Dimens.SpacingLg, vertical = Dimens.ReportGridGap),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                stringResource(R.string.cash_in_drawer),
                fontSize = DepthTypeTokens.RegisterLabelSize,
                color = OnSurfaceVariantLight,
                fontWeight = FontWeight.Medium
            )
            Text(
                MoneyFormat.formatMinor(expectedCash, currency),
                fontSize = DepthTypeTokens.ReportFieldTextSize,
                fontWeight = FontWeight.ExtraBold,
                color = PosCashGreen
            )
        }
        if (variance != null) {
            val isOver = variance > 0L
            val isBalanced = variance == 0L
            val textColor = when {
                isBalanced -> BadgeGreenText
                isOver -> BadgeGreenText
                else -> ErrorRedLight
            }
            val label = when {
                isBalanced -> stringResource(R.string.balanced)
                isOver -> stringResource(R.string.zreport_over)
                else -> stringResource(R.string.zreport_short)
            }
            Surface(
                color = when {
                    isBalanced -> BadgeGreenBg
                    isOver -> BadgeGreenBg
                    else -> ErrorContainerLight
                },
                shape = RoundedCornerShape(Dimens.RadiusFull),
                border = BorderStroke(Dimens.CardBorderWidth, textColor.copy(alpha = 0.25f))
            ) {
                Text(
                    text = stringResource(
                        R.string.archive_variance_label_fmt,
                        label,
                        if (variance > 0L) "+" else "",
                        MoneyFormat.formatMinor(variance, currency)
                    ),
                    fontSize = DepthTypeTokens.RegisterLabelSize,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    modifier = Modifier.padding(horizontal = Dimens.ReportGridGap, vertical = Dimens.SpacingXs)
                )
            }
        }
    }
}
