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
        shape = RoundedCornerShape(Dimens.CardCornerRadius),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(Dimens.SpacingNone),
        modifier = modifier.depthCard(cornerRadius = Dimens.CardCornerRadius)
    ) {
        Row(
            modifier = Modifier.padding(Dimens.ReportCardInnerPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(Dimens.PaidOutIconTileSize)
                    .depthRaisedIconTile(
                        gradientColors = listOf(PureWhite, bgColor),
                        cornerRadius = Dimens.RadiusSettingsIconTile
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(Dimens.IconField))
            }
            Spacer(Modifier.width(Dimens.SettingsIconTitleGap))
            Column {
                Text(
                    text = title,
                    fontSize = DepthTypeTokens.ListRowSubtitleSize,
                    color = OnSurfaceVariantLight
                )
                Text(
                    text = amount,
                    fontSize = DepthTypeTokens.CardSectionHeadingSize,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryReport
                )
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
    val depthStyle = LocalDepthStyle.current

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
            .background(depthStyle.reportPageBackground)
            .padding(horizontal = Dimens.SpacingLg, vertical = Dimens.SpacingMd)
    ) {
        // Search Bar (52dp/14dp depth inset well matching Paid Out & Report spec)
        var isSearchFocused by remember { mutableStateOf(false) }
        val searchShape = RoundedCornerShape(Dimens.RadiusReportInput)
        Surface(
            shape = searchShape,
            color = Color.Transparent,
            shadowElevation = Dimens.SpacingNone,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Dimens.TextFieldHeightReport)
                .depthInset(
                    isFocused = isSearchFocused,
                    shape = searchShape,
                    cornerRadius = Dimens.RadiusReportInput,
                    borderColorOverride = if (!isSearchFocused) depthStyle.reportFieldBorderColor else null,
                    topShadowAlphaOverride = depthStyle.reportFieldInsetAlpha
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Dimens.TextFieldHeightReport)
                    .padding(horizontal = Dimens.InsetFieldPaddingHorizontal),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = OnSurfaceVariantLight,
                    modifier = Modifier.size(Dimens.IconField)
                )
                Spacer(Modifier.width(Dimens.SpacingSm))
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = stringResource(R.string.search_cashier_shift),
                            fontSize = DepthTypeTokens.ListRowTitleSize,
                            color = depthStyle.reportFieldPlaceholderColor
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = DepthTypeTokens.ListRowTitleSize,
                            color = depthStyle.reportFieldTextColor
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
                            modifier = Modifier.size(Dimens.IconSm)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(Dimens.SpacingMd))

        if (filteredReports.isEmpty()) {
            Card(
                shape = RoundedCornerShape(Dimens.CardCornerRadius),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = Dimens.SpacingNone),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Dimens.SpacingXl)
                    .cardRise(0)
                    .depthCard(cornerRadius = Dimens.CardCornerRadius)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Dimens.SpacingHuge, horizontal = Dimens.SpacingXxl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(Dimens.PaidOutIconTileSize + Dimens.SpacingLg)
                            .depthRaisedIconTile(
                                gradientColors = listOf(PureWhite, BadgeBlueBg),
                                cornerRadius = Dimens.RadiusEmployeeAvatarTile
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(Dimens.IconLg),
                            tint = PrimaryBlueLight
                        )
                    }
                    Spacer(Modifier.height(Dimens.SpacingLg))
                    Text(
                        text = stringResource(R.string.no_archived_reports),
                        fontSize = DepthTypeTokens.EmptyStateTitleSize,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimaryReport,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = Dimens.KeypadButtonHeight)
            ) {
                itemsIndexed(filteredReports, key = { _, it -> it.id }) { idx, report ->
                    Card(
                        shape = RoundedCornerShape(Dimens.CardCornerRadius),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.SpacingNone),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = Dimens.ReportGridGap)
                            .cardRise(idx)
                            .depthCard(cornerRadius = Dimens.CardCornerRadius)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Dimens.ReportCardInnerPadding)
                        ) {
                            // Row 1 (header row — top of card)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(Dimens.AvatarBadgeSize)
                                        .depthRaisedIconTile(
                                            gradientColors = listOf(PureWhite, BadgeBlueBg),
                                            cornerRadius = Dimens.RadiusEmployeeAvatarTile
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = report.cashierName.take(1).uppercase(),
                                        fontSize = DepthTypeTokens.SectionTitleSize,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryBlueLight
                                    )
                                }

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = Dimens.SettingsIconTitleGap, end = Dimens.SpacingSm)
                                ) {
                                    Text(
                                        text = report.cashierName,
                                        fontSize = DepthTypeTokens.ListRowTitleSize,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimaryReport,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.height(Dimens.SpacingXxs))
                                    Text(
                                        text = DateTimeFormatUtils.formatDateTime(report.dateInMillis),
                                        fontSize = DepthTypeTokens.ListRowSubtitleSize,
                                        color = OnSurfaceVariantLight,
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
                                    shiftNormalized.equals("Morning", ignoreCase = true) -> BadgeGreenBg
                                    shiftNormalized.equals("Evening", ignoreCase = true) -> BadgeAmberBg
                                    shiftNormalized.equals("Night", ignoreCase = true) -> BadgeBlueBg
                                    else -> SegmentTrackBg
                                }
                                val shiftFg = when {
                                    shiftNormalized.equals("Morning", ignoreCase = true) -> BadgeGreenText
                                    shiftNormalized.equals("Evening", ignoreCase = true) -> BadgeAmberTextDark
                                    shiftNormalized.equals("Night", ignoreCase = true) -> BadgeBlueText
                                    else -> OnSurfaceVariantLight
                                }

                                Surface(
                                    shape = RoundedCornerShape(Dimens.RadiusFull),
                                    color = shiftBg,
                                    modifier = Modifier.wrapContentWidth()
                                ) {
                                    Text(
                                        text = shiftDisplayName,
                                        fontSize = DepthTypeTokens.ChipLabelSize,
                                        fontWeight = FontWeight.SemiBold,
                                        color = shiftFg,
                                        modifier = Modifier.padding(
                                            horizontal = Dimens.ReportGridGap,
                                            vertical = Dimens.SpacingXs
                                        ),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }

                                Spacer(Modifier.width(Dimens.SpacingXs))

                                Surface(
                                    shape = RoundedCornerShape(Dimens.RadiusFull),
                                    color = BadgeBlueBg
                                ) {
                                    Text(
                                        text = "#${report.id}",
                                        fontSize = DepthTypeTokens.ChipLabelSize,
                                        fontWeight = FontWeight.SemiBold,
                                        color = BadgeBlueText,
                                        modifier = Modifier.padding(
                                            horizontal = Dimens.ReportGridGap,
                                            vertical = Dimens.SpacingXs
                                        ),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            Spacer(Modifier.height(Dimens.SpacingMd))
                            HorizontalDivider(
                                thickness = Dimens.RowDividerWidth,
                                color = depthStyle.listDividerColor
                            )

                            // Row 2 (stat row — mid of card)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = Dimens.ListRowPaddingVertical),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    Text(
                                        text = stringResource(R.string.gross_revenue).uppercase(),
                                        fontSize = DepthTypeTokens.SettingsSectionHeaderSize,
                                        fontWeight = FontWeight.Medium,
                                        color = OnSurfaceVariantLight,
                                        letterSpacing = DepthTypeTokens.SettingsSectionHeaderTracking,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.height(Dimens.SpacingXxs))
                                    Text(
                                        text = MoneyFormat.formatMinor(report.totalSales, currentCurrency),
                                        fontSize = DepthTypeTokens.ListRowTitleSize,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimaryReport,
                                        maxLines = 1
                                    )
                                }

                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = stringResource(R.string.mada_bank).uppercase(),
                                        fontSize = DepthTypeTokens.SettingsSectionHeaderSize,
                                        fontWeight = FontWeight.Medium,
                                        color = OnSurfaceVariantLight,
                                        letterSpacing = DepthTypeTokens.SettingsSectionHeaderTracking,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.height(Dimens.SpacingXxs))
                                    Text(
                                        text = MoneyFormat.formatMinor(report.madaPayments, currentCurrency),
                                        fontSize = DepthTypeTokens.ListRowTitleSize,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryBlueLight,
                                        maxLines = 1
                                    )
                                }

                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = stringResource(R.string.net_cash_1).uppercase(),
                                        fontSize = DepthTypeTokens.SettingsSectionHeaderSize,
                                        fontWeight = FontWeight.Medium,
                                        color = OnSurfaceVariantLight,
                                        letterSpacing = DepthTypeTokens.SettingsSectionHeaderTracking,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.height(Dimens.SpacingXxs))
                                    Text(
                                        text = MoneyFormat.formatMinor(report.netCash, currentCurrency),
                                        fontSize = DepthTypeTokens.ListRowTitleSize,
                                        fontWeight = FontWeight.Bold,
                                        color = BadgeGreenText,
                                        maxLines = 1
                                    )
                                }
                            }

                            HorizontalDivider(
                                thickness = Dimens.RowDividerWidth,
                                color = depthStyle.listDividerColor
                            )

                            // Row 3 (action row — bottom of card)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = Dimens.SpacingMd),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { reportToDelete = report },
                                    shape = RoundedCornerShape(Dimens.RadiusReportInput),
                                    border = null,
                                    elevation = null,
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = Color.Transparent,
                                        contentColor = ErrorRedLight
                                    ),
                                    contentPadding = PaddingValues(
                                        horizontal = Dimens.SpacingMd,
                                        vertical = Dimens.SpacingSm
                                    ),
                                    modifier = Modifier
                                        .heightIn(min = Dimens.MinTouchTarget)
                                        .depthSecondaryButton(
                                            borderColor = ErrorRedLight,
                                            cornerRadius = Dimens.RadiusReportInput
                                        )
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.DeleteOutline,
                                        contentDescription = null,
                                        modifier = Modifier.size(Dimens.IconSm),
                                        tint = ErrorRedLight
                                    )
                                    Spacer(Modifier.width(Dimens.SpacingXs))
                                    Text(
                                        text = stringResource(R.string.delete),
                                        fontSize = DepthTypeTokens.PrimaryCtaSmallSize,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ErrorRedLight,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }

                                Spacer(Modifier.width(Dimens.SpacingSm))

                                Button(
                                    onClick = { onPreviewPdf(report) },
                                    shape = RoundedCornerShape(Dimens.RadiusReportInput),
                                    elevation = null,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Transparent,
                                        contentColor = PureWhite
                                    ),
                                    contentPadding = PaddingValues(
                                        horizontal = Dimens.SpacingLg,
                                        vertical = Dimens.SpacingSm
                                    ),
                                    modifier = Modifier
                                        .heightIn(min = Dimens.MinTouchTarget)
                                        .depthRaisedButton(cornerRadius = Dimens.RadiusReportInput)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.PictureAsPdf,
                                        contentDescription = null,
                                        modifier = Modifier.size(Dimens.IconSm),
                                        tint = PureWhite
                                    )
                                    Spacer(Modifier.width(Dimens.SpacingXs))
                                    Text(
                                        text = stringResource(R.string.preview_pdf),
                                        fontSize = DepthTypeTokens.PrimaryCtaSmallSize,
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
