package com.lojia.shiftreport.report

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Drafts
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.PostAdd
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.lojia.shiftreport.R
import com.lojia.shiftreport.data.AppLanguage
import com.lojia.shiftreport.data.ShiftReport
import com.lojia.shiftreport.ui.theme.BadgeAmberAccent
import com.lojia.shiftreport.ui.theme.BadgeAmberTextDark
import com.lojia.shiftreport.ui.theme.BadgeBlueText
import com.lojia.shiftreport.ui.theme.BadgeGreenText
import com.lojia.shiftreport.ui.theme.DepthCardBorder
import com.lojia.shiftreport.ui.theme.DepthMotionTokens
import com.lojia.shiftreport.ui.theme.DepthTokens
import com.lojia.shiftreport.ui.theme.DepthTypeTokens
import com.lojia.shiftreport.ui.theme.Dimens
import com.lojia.shiftreport.ui.theme.LocalDepthStyle
import com.lojia.shiftreport.ui.theme.LojiaGradientHeader
import com.lojia.shiftreport.ui.theme.LojiaLiveDot
import com.lojia.shiftreport.ui.theme.PosExpenseRed
import com.lojia.shiftreport.ui.theme.PosSurplusGreen
import com.lojia.shiftreport.ui.theme.PrimaryBlueLight
import com.lojia.shiftreport.ui.theme.PureWhite
import com.lojia.shiftreport.ui.theme.SegmentTrackBg
import com.lojia.shiftreport.ui.theme.TextPlaceholderReport
import com.lojia.shiftreport.ui.theme.TextPrimaryReport
import com.lojia.shiftreport.ui.theme.TextSlate700
import com.lojia.shiftreport.ui.theme.TileAmberEnd
import com.lojia.shiftreport.ui.theme.TileBlueEnd
import com.lojia.shiftreport.ui.theme.TileGreenEnd
import com.lojia.shiftreport.ui.theme.TileMintEnd
import com.lojia.shiftreport.ui.theme.TileRoseEnd
import com.lojia.shiftreport.ui.theme.WarningText
import com.lojia.shiftreport.ui.theme.cardRise
import com.lojia.shiftreport.ui.theme.depthCard
import com.lojia.shiftreport.ui.theme.depthChartTodayBarGlow
import com.lojia.shiftreport.ui.theme.depthDraftBanner
import com.lojia.shiftreport.ui.theme.depthInset
import com.lojia.shiftreport.ui.theme.depthLiveBadge
import com.lojia.shiftreport.ui.theme.depthRaisedButton
import com.lojia.shiftreport.ui.theme.depthRaisedIconTile
import com.lojia.shiftreport.ui.theme.depthSecondaryButton
import com.lojia.shiftreport.ui.theme.depthSegmentActiveThumb
import com.lojia.shiftreport.ui.theme.rememberShouldAnimate
import com.lojia.shiftreport.util.MoneyFormat
import kotlinx.coroutines.delay
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

@Composable
fun HomeDashboardScreen(
    reportViewModel: ReportViewModel,
    language: AppLanguage,
    onOpenDrawer: () -> Unit,
    onNavigateToReportTab: (ShiftReportTab, ModalType) -> Unit,
    onPreviewPdf: (ShiftReport) -> Unit
) {
    val shiftReports by reportViewModel.shiftReports.collectAsState()
    val activeShiftSession by reportViewModel.activeShiftSession.collectAsState()
    val employees by reportViewModel.employees.collectAsState()
    val currentRole by reportViewModel.currentRole.collectAsState()
    val currentCurrency by reportViewModel.currentCurrency.collectAsState()
    val draftFormState by reportViewModel.draftFormState.collectAsState()
    val cashiersState by reportViewModel.cashiers.collectAsState()
    val activeCashier by reportViewModel.activeCashier.collectAsState()

    val activeLocale = remember(language) { Locale.forLanguageTag(language.code) }
    val zone = remember { ZoneId.systemDefault() }
    val nowMillis = remember(shiftReports, activeShiftSession, draftFormState) {
        System.currentTimeMillis()
    }
    val liveDraft = remember(draftFormState, currentCurrency) {
        buildHomeLiveDraftInput(draftFormState, currentCurrency)
    }
    val summary = remember(
        shiftReports,
        activeShiftSession,
        employees,
        currentRole,
        nowMillis,
        zone,
        activeLocale,
        liveDraft
    ) {
        buildHomeDashboardSummary(
            reports = shiftReports,
            activeSession = activeShiftSession,
            advances = parseEmployerAdvances(shiftReports),
            employees = employees,
            role = currentRole,
            nowMillis = nowMillis,
            zone = zone,
            locale = activeLocale,
            liveDraft = liveDraft
        )
    }

    val cashierOptions = remember(cashiersState) {
        cashiersState
            .filter { !it.isArchived && it.isActive && it.active }
            .map { it.name }
            .filter { it.isNotBlank() }
    }

    var showOpenShiftDialog by remember { mutableStateOf(false) }
    var showCloseShiftDialog by remember { mutableStateOf(false) }

    HomeDashboardContent(
        summary = summary,
        currency = currentCurrency,
        locale = activeLocale,
        zone = zone,
        onOpenDrawer = onOpenDrawer,
        onOpenShiftClick = { showOpenShiftDialog = true },
        onCloseShiftClick = {
            if (activeShiftSession != null) {
                showCloseShiftDialog = true
            }
        },
        onContinueReportClick = { onNavigateToReportTab(ShiftReportTab.REPORT_ENTRY, ModalType.NONE) },
        onOpenDuesClick = { onNavigateToReportTab(ShiftReportTab.DUE_HISTORY, ModalType.NONE) },
        onOpenAdvancesClick = { onNavigateToReportTab(ShiftReportTab.EMPLOYER_ADVANCES, ModalType.NONE) },
        onOpenPaidOutClick = { onNavigateToReportTab(ShiftReportTab.PAID_OUT, ModalType.NONE) },
        onOpenAddDueClick = { onNavigateToReportTab(ShiftReportTab.REPORT_ENTRY, ModalType.CREDIT) }
    )

    if (showOpenShiftDialog) {
        HomeOpenShiftDialog(
            cashierOptions = cashierOptions,
            defaultCashier = draftFormState.cashier.ifBlank { activeCashier },
            currency = currentCurrency,
            onDismiss = { showOpenShiftDialog = false },
            onConfirmOpen = { cashier, shiftName, startingCash ->
                showOpenShiftDialog = false
                reportViewModel.openShift(cashier, shiftName, startingCash)
            }
        )
    }

    val openSession = activeShiftSession
    if (showCloseShiftDialog && openSession != null) {
        HomeCloseShiftDialog(
            session = openSession,
            expectedDrawerCashMinor = summary.expectedDrawerCashMinor,
            currency = currentCurrency,
            locale = activeLocale,
            onDismiss = { showCloseShiftDialog = false },
            onConfirmClose = { actualCash, notes ->
                showCloseShiftDialog = false
                reportViewModel.closeShiftWithZReport(openSession, actualCash, notes, onPreviewPdf)
            }
        )
    }
}

@Composable
fun HomeDashboardContent(
    summary: HomeDashboardSummary,
    currency: String,
    locale: Locale,
    zone: ZoneId = ZoneId.systemDefault(),
    onOpenDrawer: () -> Unit,
    onOpenShiftClick: () -> Unit,
    onCloseShiftClick: () -> Unit,
    onContinueReportClick: () -> Unit,
    onOpenDuesClick: () -> Unit,
    onOpenAdvancesClick: () -> Unit,
    onOpenPaidOutClick: () -> Unit,
    onOpenAddDueClick: () -> Unit
) {
    val depthStyle = LocalDepthStyle.current
    val animateMotion = rememberShouldAnimate()
    var selectedPeriod by rememberSaveable { mutableStateOf(HomeSalesPeriod.TODAY) }
    val dateHeaderText = remember(summary.todayLocalDate, locale) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale).format(summary.todayLocalDate)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(depthStyle.reportPageBackground)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .testTag("home_dashboard_screen")
    ) {
        LojiaGradientHeader(
            shape = RoundedCornerShape(
                bottomStart = Dimens.RadiusXl,
                bottomEnd = Dimens.RadiusXl
            ),
            cornerRadiusForShadow = Dimens.RadiusXl,
            contentPadding = PaddingValues(0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(
                        start = Dimens.SpacingSm,
                        end = Dimens.SpacingLg,
                        top = Dimens.SpacingSm,
                        bottom = Dimens.HomeHeaderBottomPadding
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onOpenDrawer,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("drawer_menu_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = stringResource(R.string.title_drawer_menu),
                        tint = PureWhite
                    )
                }
                Spacer(modifier = Modifier.width(Dimens.SpacingXs))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.home_welcome_back),
                        fontSize = DepthTypeTokens.HomeGreetingSize,
                        fontWeight = FontWeight.Medium,
                        color = DepthTokens.HomeHeaderGreetingColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(Dimens.SpacingXxs))
                    Text(
                        text = dateHeaderText,
                        fontSize = DepthTypeTokens.HomeDateTitleSize,
                        fontWeight = FontWeight.SemiBold,
                        color = PureWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = -Dimens.HomeHeaderOverlapOffset)
                .zIndex(1f)
                .padding(horizontal = Dimens.SpacingLg),
            verticalArrangement = Arrangement.spacedBy(Dimens.HomeSectionGap)
        ) {
            HomeShiftStatusCard(
                summary = summary,
                locale = locale,
                zone = zone,
                onOpenShiftClick = onOpenShiftClick,
                onCloseShiftClick = onCloseShiftClick,
                onContinueReportClick = onContinueReportClick,
                modifier = Modifier.cardRise(0)
            )

            HomeSalesSummaryCard(
                summary = summary,
                selectedPeriod = selectedPeriod,
                onSelectPeriod = { selectedPeriod = it },
                currency = currency,
                locale = locale,
                animateMotion = animateMotion,
                modifier = Modifier.cardRise(1)
            )

            HomeKpiGridSection(
                summary = summary,
                currency = currency,
                locale = locale,
                onOpenDuesClick = onOpenDuesClick,
                onOpenAdvancesClick = onOpenAdvancesClick,
                onOpenPaidOutClick = onOpenPaidOutClick
            )

            if (summary.hasUnsavedDraft) {
                HomeDraftReviewBanner(
                    onClick = onContinueReportClick,
                    modifier = Modifier.cardRise(4)
                )
            }

            HomeQuickActionsCard(
                onNewReportClick = onContinueReportClick,
                onAddDueClick = onOpenAddDueClick,
                onAdvanceClick = onOpenAdvancesClick,
                onPaidOutClick = onOpenPaidOutClick,
                modifier = Modifier.cardRise(if (summary.hasUnsavedDraft) 5 else 4)
            )

            if (summary.showOwnerOnlyParts) {
                HomeLast7DaysChartCard(
                    points = summary.last7Days,
                    currency = currency,
                    locale = locale,
                    animateMotion = animateMotion,
                    modifier = Modifier.cardRise(if (summary.hasUnsavedDraft) 6 else 5)
                )
            }

            Spacer(modifier = Modifier.height(Dimens.SpacingXs))
        }
    }
}

@Composable
private fun HomeShiftStatusCard(
    summary: HomeDashboardSummary,
    locale: Locale,
    zone: ZoneId,
    onOpenShiftClick: () -> Unit,
    onCloseShiftClick: () -> Unit,
    onContinueReportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shiftCardCd = stringResource(R.string.home_cd_shift_card)
    val cardShape = RoundedCornerShape(Dimens.CardCornerRadius)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = shiftCardCd }
            .depthCard(shape = cardShape, cornerRadius = Dimens.CardCornerRadius)
            .padding(Dimens.HomeCardPaddingLarge)
            .testTag("home_shift_status_card"),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMd)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.home_shift_status),
                fontSize = DepthTypeTokens.SectionTitleSize,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryReport
            )
            if (summary.isShiftOpen) {
                Row(
                    modifier = Modifier
                        .heightIn(min = Dimens.HomeStatusBadgeHeight)
                        .depthLiveBadge()
                        .padding(horizontal = Dimens.SpacingMd, vertical = Dimens.SpacingXs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingXs)
                ) {
                    LojiaLiveDot()
                    Text(
                        text = stringResource(R.string.home_shift_open_badge),
                        fontSize = DepthTypeTokens.HomeLiveBadgeSize,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = DepthTypeTokens.HomeLiveBadgeTracking,
                        color = BadgeGreenText
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .heightIn(min = Dimens.HomeStatusBadgeHeight)
                        .clip(RoundedCornerShape(Dimens.RadiusFull))
                        .background(DepthTokens.ClosedBadgeBackground)
                        .border(
                            width = Dimens.CardBorderWidth,
                            color = DepthTokens.ClosedBadgeBorderColor,
                            shape = RoundedCornerShape(Dimens.RadiusFull)
                        )
                        .padding(horizontal = Dimens.SpacingMd, vertical = Dimens.SpacingXs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.home_shift_closed_badge),
                        fontSize = DepthTypeTokens.HomeLiveBadgeSize,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = DepthTypeTokens.HomeLiveBadgeTracking,
                        color = DepthTokens.ClosedBadgeTextColor
                    )
                }
            }
        }

        if (summary.isShiftOpen) {
            val openedTimeText = remember(summary.openShiftOpenedAtMillis, locale, zone) {
                val millis = summary.openShiftOpenedAtMillis ?: 0L
                DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
                    .withLocale(locale)
                    .format(Instant.ofEpochMilli(millis).atZone(zone))
            }
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpacingXs)) {
                Text(
                    text = stringResource(
                        R.string.home_open_shift_detail_fmt,
                        summary.openShiftName.ifBlank { stringResource(R.string.shift_day) },
                        summary.openShiftCashier.ifBlank { stringResource(R.string.cashier) },
                        openedTimeText
                    ),
                    fontSize = DepthTypeTokens.ListRowTitleSize,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimaryReport
                )
                if (summary.hasUnsavedDraft) {
                    Text(
                        text = stringResource(R.string.home_report_in_progress),
                        fontSize = DepthTypeTokens.ListRowSubtitleSize,
                        color = BadgeGreenText,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.HomeKpiGridGap)
            ) {
                OutlinedButton(
                    onClick = onContinueReportClick,
                    shape = RoundedCornerShape(Dimens.RadiusMd),
                    border = null,
                    elevation = null,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = PrimaryBlueLight
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Dimens.ButtonHeightPrimary)
                        .depthSecondaryButton(cornerRadius = Dimens.RadiusMd)
                        .testTag("home_continue_report_btn")
                ) {
                    Text(
                        text = stringResource(R.string.home_continue_report),
                        fontSize = DepthTypeTokens.PrimaryCtaSmallSize,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Button(
                    onClick = onCloseShiftClick,
                    shape = RoundedCornerShape(Dimens.RadiusMd),
                    elevation = null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = PureWhite
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Dimens.ButtonHeightPrimary)
                        .depthRaisedButton(isDestructive = false, cornerRadius = Dimens.RadiusMd)
                        .testTag("home_close_shift_btn")
                ) {
                    Text(
                        text = stringResource(R.string.close_shift),
                        fontSize = DepthTypeTokens.PrimaryCtaSmallSize,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpacingXxs)) {
                Text(
                    text = stringResource(R.string.home_no_open_shift),
                    fontSize = DepthTypeTokens.ListRowTitleSize,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimaryReport
                )
                Text(
                    text = stringResource(R.string.home_no_open_shift_desc),
                    fontSize = DepthTypeTokens.ListRowSubtitleSize,
                    color = TextPlaceholderReport
                )
            }
            Button(
                onClick = onOpenShiftClick,
                shape = RoundedCornerShape(Dimens.RadiusMd),
                elevation = null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = PureWhite
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Dimens.ButtonHeightPrimary)
                    .depthRaisedButton(cornerRadius = Dimens.RadiusMd)
                    .testTag("home_open_shift_btn")
            ) {
                Text(
                    text = stringResource(R.string.open_shift),
                    fontSize = DepthTypeTokens.PrimaryButtonTextSize,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun HomeSalesSummaryCard(
    summary: HomeDashboardSummary,
    selectedPeriod: HomeSalesPeriod,
    onSelectPeriod: (HomeSalesPeriod) -> Unit,
    currency: String,
    locale: Locale,
    animateMotion: Boolean,
    modifier: Modifier = Modifier
) {
    val activePeriodSales = remember(summary, selectedPeriod) {
        summary.salesFor(selectedPeriod)
    }
    val targetNetMinor = activePeriodSales.netSalesMinor
    val animatedNetMinor = remember {
        Animatable(if (animateMotion) 0f else targetNetMinor.toFloat())
    }
    LaunchedEffect(targetNetMinor, animateMotion) {
        if (animateMotion) {
            animatedNetMinor.animateTo(
                targetValue = targetNetMinor.toFloat(),
                animationSpec = tween(
                    durationMillis = DepthMotionTokens.HERO_COUNTUP_DURATION_MS,
                    easing = DepthMotionTokens.CardRiseEasing
                )
            )
        } else {
            animatedNetMinor.snapTo(targetNetMinor.toFloat())
        }
    }
    val displayedNetMinor = if (animateMotion) animatedNetMinor.value.roundToLong() else targetNetMinor

    val cardShape = RoundedCornerShape(Dimens.CardCornerRadius)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .depthCard(shape = cardShape, cornerRadius = Dimens.CardCornerRadius)
            .padding(Dimens.HomeCardPaddingLarge)
            .testTag("home_sales_summary_card"),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMd)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.sales_summary),
                fontSize = DepthTypeTokens.SectionTitleSize,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryReport
            )
            if (summary.isShiftOpen) {
                Row(
                    modifier = Modifier
                        .heightIn(min = Dimens.HomeStatusBadgeHeight)
                        .depthLiveBadge()
                        .padding(horizontal = Dimens.SpacingMd, vertical = Dimens.SpacingXs)
                        .testTag("home_live_badge"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingXs)
                ) {
                    LojiaLiveDot()
                    Text(
                        text = stringResource(R.string.home_live_badge),
                        fontSize = DepthTypeTokens.HomeLiveBadgeSize,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = DepthTypeTokens.HomeLiveBadgeTracking,
                        color = BadgeGreenText
                    )
                }
            }
        }

        // 3-period inset segmented track matching SH18/SH19
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Dimens.SegmentTrackHeight)
                .depthInset(
                    cornerRadius = Dimens.RadiusSegmentTrack,
                    gradientColors = listOf(SegmentTrackBg, SegmentTrackBg),
                    borderColorOverride = DepthCardBorder,
                    topShadowAlphaOverride = DepthTokens.SEGMENT_TRACK_INSET_ALPHA
                )
                .padding(Dimens.SpacingXs)
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingXs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(
                HomeSalesPeriod.TODAY to stringResource(R.string.home_period_today),
                HomeSalesPeriod.THIS_WEEK to stringResource(R.string.home_period_this_week),
                HomeSalesPeriod.THIS_MONTH to stringResource(R.string.home_period_this_month)
            ).forEach { (period, label) ->
                val isSelected = selectedPeriod == period
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Dimens.SegmentThumbHeight.coerceAtLeast(Dimens.MinTouchTarget))
                        .clip(RoundedCornerShape(Dimens.RadiusSegmentThumb))
                        .then(
                            if (isSelected) {
                                Modifier.depthSegmentActiveThumb(cornerRadius = Dimens.RadiusSegmentThumb)
                            } else {
                                Modifier
                            }
                        )
                        .selectable(
                            selected = isSelected,
                            onClick = { onSelectPeriod(period) },
                            role = Role.RadioButton
                        )
                        .testTag("home_period_${period.name.lowercase(Locale.ROOT)}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = DepthTypeTokens.SegmentedChipTextSize,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) PureWhite else TextSlate700,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpacingXxs)) {
            Text(
                text = stringResource(R.string.home_net_sales),
                fontSize = DepthTypeTokens.ReportFieldLabelSize,
                fontWeight = FontWeight.Medium,
                color = TextPlaceholderReport
            )
            Text(
                text = MoneyFormat.formatMinor(displayedNetMinor, currency, locale),
                fontSize = DepthTypeTokens.HomeHeroNetSalesSize,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryReport,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("home_hero_net_sales")
            )
            Text(
                text = stringResource(R.string.home_updates_as_you_enter),
                fontSize = DepthTypeTokens.ListRowSubtitleSize,
                color = TextPlaceholderReport
            )
        }

        // Proportional Cash / Bank / Wallet horizontal bar
        HomePaymentSplitBar(
            cashMinor = activePeriodSales.cashMinor.coerceAtLeast(0L),
            bankMinor = activePeriodSales.bankMinor.coerceAtLeast(0L),
            walletMinor = activePeriodSales.walletMinor.coerceAtLeast(0L)
        )

        Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)) {
            HomeBreakdownRow(
                label = stringResource(R.string.cash),
                amountText = MoneyFormat.formatMinor(activePeriodSales.cashMinor, currency, locale),
                swatchColors = listOf(
                    DepthTokens.BrandButtonGradientStart,
                    DepthTokens.BrandButtonGradientEnd
                )
            )
            HomeBreakdownRow(
                label = stringResource(R.string.mada_bank),
                amountText = MoneyFormat.formatMinor(activePeriodSales.bankMinor, currency, locale),
                swatchColors = listOf(
                    DepthTokens.BankSegmentGradientStart,
                    DepthTokens.BankSegmentGradientEnd
                )
            )
            HomeBreakdownRow(
                label = stringResource(R.string.digital_wallet),
                amountText = MoneyFormat.formatMinor(activePeriodSales.walletMinor, currency, locale),
                swatchColors = listOf(
                    DepthTokens.WalletSegmentGradientStart,
                    DepthTokens.WalletSegmentGradientEnd
                )
            )
        }
    }
}

@Composable
private fun HomePaymentSplitBar(
    cashMinor: Long,
    bankMinor: Long,
    walletMinor: Long
) {
    val segments = remember(cashMinor, bankMinor, walletMinor) {
        buildList {
            if (cashMinor > 0L) {
                add(
                    cashMinor.toFloat() to listOf(
                        DepthTokens.BrandButtonGradientStart,
                        DepthTokens.BrandButtonGradientEnd
                    )
                )
            }
            if (bankMinor > 0L) {
                add(
                    bankMinor.toFloat() to listOf(
                        DepthTokens.BankSegmentGradientStart,
                        DepthTokens.BankSegmentGradientEnd
                    )
                )
            }
            if (walletMinor > 0L) {
                add(
                    walletMinor.toFloat() to listOf(
                        DepthTokens.WalletSegmentGradientStart,
                        DepthTokens.WalletSegmentGradientEnd
                    )
                )
            }
        }
    }

    val trackShape = RoundedCornerShape(Dimens.HomeBreakdownTrackRadius)
    if (segments.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimens.HomeBreakdownTrackHeight)
                .clip(trackShape)
                .background(SegmentTrackBg)
        )
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimens.HomeBreakdownTrackHeight)
                .clip(trackShape),
            horizontalArrangement = Arrangement.spacedBy(Dimens.HomeBreakdownSegmentGap)
        ) {
            segments.forEach { (weightValue, colors) ->
                Box(
                    modifier = Modifier
                        .weight(weightValue.coerceAtLeast(1f))
                        .fillMaxHeight()
                        .clip(trackShape)
                        .background(Brush.horizontalGradient(colors = colors))
                )
            }
        }
    }
}

@Composable
private fun HomeBreakdownRow(
    label: String,
    amountText: String,
    swatchColors: List<Color>
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(Dimens.HomeBreakdownSwatchSize)
                    .clip(RoundedCornerShape(Dimens.HomeBreakdownSwatchRadius))
                    .background(Brush.verticalGradient(colors = swatchColors))
            )
            Text(
                text = label,
                fontSize = DepthTypeTokens.ReportFieldLabelSize,
                fontWeight = FontWeight.Medium,
                color = TextSlate700,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(Dimens.SpacingSm))
        Text(
            text = amountText,
            fontSize = DepthTypeTokens.ListRowTitleSize,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimaryReport,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HomeKpiGridSection(
    summary: HomeDashboardSummary,
    currency: String,
    locale: Locale,
    onOpenDuesClick: () -> Unit,
    onOpenAdvancesClick: () -> Unit,
    onOpenPaidOutClick: () -> Unit
) {
    val varianceText = when (summary.lastShiftVarianceStatus) {
        HomeVarianceStatus.OVER -> stringResource(
            R.string.home_last_shift_over_fmt,
            MoneyFormat.formatMinor(abs(summary.lastShiftVarianceMinor), currency, locale)
        )
        HomeVarianceStatus.SHORT -> stringResource(
            R.string.home_last_shift_short_fmt,
            MoneyFormat.formatMinor(abs(summary.lastShiftVarianceMinor), currency, locale)
        )
        HomeVarianceStatus.BALANCED -> stringResource(R.string.home_last_shift_balanced)
        HomeVarianceStatus.NONE -> stringResource(R.string.home_last_shift_none)
    }
    val varianceColor = when (summary.lastShiftVarianceStatus) {
        HomeVarianceStatus.OVER -> PosSurplusGreen
        HomeVarianceStatus.SHORT -> PosExpenseRed
        HomeVarianceStatus.BALANCED -> PosSurplusGreen
        HomeVarianceStatus.NONE -> TextPlaceholderReport
    }

    val duesSubtitle = if (summary.openReceiptsCount > 0) {
        stringResource(R.string.home_open_receipts_count_fmt, summary.openReceiptsCount)
    } else {
        stringResource(R.string.home_all_receipts_settled)
    }

    val advancesSubtitle = stringResource(
        R.string.home_advances_split_fmt,
        MoneyFormat.formatMinor(summary.advancesThisMonthCashMinor, currency, locale),
        MoneyFormat.formatMinor(summary.advancesThisMonthBankMinor, currency, locale)
    )

    val expensesSubtitle = stringResource(
        R.string.home_expenses_entries_fmt,
        summary.expensesTodayCount
    )

    Column(verticalArrangement = Arrangement.spacedBy(Dimens.HomeKpiGridGap)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(Dimens.HomeKpiGridGap)
        ) {
            HomeKpiCard(
                title = stringResource(R.string.home_kpi_expected_drawer),
                value = MoneyFormat.formatMinor(summary.expectedDrawerCashMinor, currency, locale),
                subtitle = varianceText,
                subtitleColor = varianceColor,
                icon = Icons.Outlined.PointOfSale,
                iconTint = PosSurplusGreen,
                tileColors = listOf(PureWhite, TileGreenEnd),
                onClick = null,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .cardRise(2)
                    .testTag("home_kpi_expected_drawer")
            )
            HomeKpiCard(
                title = stringResource(R.string.home_kpi_outstanding_dues),
                value = MoneyFormat.formatMinor(summary.outstandingDuesMinor, currency, locale),
                subtitle = duesSubtitle,
                subtitleColor = TextPlaceholderReport,
                icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                iconTint = BadgeAmberAccent,
                tileColors = listOf(PureWhite, TileAmberEnd),
                onClick = onOpenDuesClick,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .cardRise(2)
                    .testTag("home_kpi_outstanding_dues")
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(Dimens.HomeKpiGridGap)
        ) {
            HomeKpiCard(
                title = stringResource(R.string.home_kpi_advances_month),
                value = MoneyFormat.formatMinor(summary.advancesThisMonthTotalMinor, currency, locale),
                subtitle = advancesSubtitle,
                subtitleColor = TextPlaceholderReport,
                icon = Icons.Outlined.AccountBalanceWallet,
                iconTint = PrimaryBlueLight,
                tileColors = listOf(PureWhite, TileBlueEnd),
                onClick = onOpenAdvancesClick,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .cardRise(3)
                    .testTag("home_kpi_advances_month")
            )
            HomeKpiCard(
                title = stringResource(R.string.home_kpi_expenses_today),
                value = MoneyFormat.formatMinor(summary.expensesTodayMinor, currency, locale),
                subtitle = expensesSubtitle,
                subtitleColor = TextPlaceholderReport,
                icon = Icons.Outlined.Payments,
                iconTint = PosExpenseRed,
                tileColors = listOf(PureWhite, TileRoseEnd),
                onClick = onOpenPaidOutClick,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .cardRise(3)
                    .testTag("home_kpi_expenses_today")
            )
        }
    }
}

@Composable
private fun HomeKpiCard(
    title: String,
    value: String,
    subtitle: String,
    subtitleColor: Color,
    icon: ImageVector,
    iconTint: Color,
    tileColors: List<Color>,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(Dimens.RadiusKpiCard)
    Column(
        modifier = modifier
            .defaultMinSize(minHeight = Dimens.MinTouchTarget)
            .depthCard(shape = cardShape, cornerRadius = Dimens.RadiusKpiCard)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(Dimens.ReportCardInnerPadding),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)
        ) {
            Box(
                modifier = Modifier
                    .size(Dimens.KpiIconTileSize)
                    .depthRaisedIconTile(
                        gradientColors = tileColors,
                        cornerRadius = Dimens.RadiusSectionIconTile
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(Dimens.IconField)
                )
            }
            Text(
                text = title,
                fontSize = DepthTypeTokens.ListRowSubtitleSize,
                fontWeight = FontWeight.SemiBold,
                color = TextSlate700,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        Column {
            Spacer(modifier = Modifier.height(Dimens.HomeKpiValueTopGap))
            Text(
                text = value,
                fontSize = DepthTypeTokens.HomeKpiValueSize,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryReport,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(Dimens.SpacingXxs))
            Text(
                text = subtitle,
                fontSize = DepthTypeTokens.ListRowSubtitleSize,
                fontWeight = FontWeight.Medium,
                color = subtitleColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HomeDraftReviewBanner(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = Dimens.HomeDraftBannerMinHeight)
            .depthDraftBanner(cornerRadius = Dimens.RadiusKpiCard)
            .clickable(onClick = onClick)
            .padding(Dimens.ReportCardInnerPadding)
            .testTag("home_draft_banner"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingMd)
    ) {
        Box(
            modifier = Modifier
                .size(Dimens.PaidOutIconTileSize)
                .depthRaisedIconTile(
                    gradientColors = listOf(PureWhite, TileAmberEnd),
                    cornerRadius = Dimens.RadiusSettingsIconTile
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Drafts,
                contentDescription = null,
                tint = BadgeAmberAccent,
                modifier = Modifier.size(Dimens.IconField)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.home_draft_banner_title),
                fontSize = DepthTypeTokens.ListRowTitleSize,
                fontWeight = FontWeight.Bold,
                color = BadgeAmberTextDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(Dimens.SpacingXxs))
            Text(
                text = stringResource(R.string.home_draft_banner_subtitle),
                fontSize = DepthTypeTokens.ListRowSubtitleSize,
                color = WarningText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            modifier = Modifier
                .heightIn(min = Dimens.MinTouchTarget)
                .clip(RoundedCornerShape(Dimens.RadiusMd))
                .clickable(onClick = onClick)
                .padding(horizontal = Dimens.SpacingSm)
                .testTag("home_draft_review_btn"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.home_draft_review_action),
                fontSize = DepthTypeTokens.SecondaryButtonTextSize,
                fontWeight = FontWeight.Bold,
                color = BadgeAmberTextDark
            )
        }
    }
}

@Composable
private fun HomeQuickActionsCard(
    onNewReportClick: () -> Unit,
    onAddDueClick: () -> Unit,
    onAdvanceClick: () -> Unit,
    onPaidOutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(Dimens.RadiusKpiCard)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .depthCard(shape = cardShape, cornerRadius = Dimens.RadiusKpiCard)
            .padding(Dimens.ReportCardInnerPadding)
            .testTag("home_quick_actions_card"),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMd)
    ) {
        Text(
            text = stringResource(R.string.home_quick_actions),
            fontSize = DepthTypeTokens.CardSectionHeadingSize,
            fontWeight = FontWeight.Bold,
            color = TextPrimaryReport
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.HomeKpiGridGap)
        ) {
            HomeQuickActionItem(
                label = stringResource(R.string.home_action_new_report),
                icon = Icons.Outlined.PostAdd,
                iconTint = PrimaryBlueLight,
                tileColors = listOf(PureWhite, TileBlueEnd),
                onClick = onNewReportClick,
                modifier = Modifier
                    .weight(1f)
                    .testTag("home_quick_action_new_report")
            )
            HomeQuickActionItem(
                label = stringResource(R.string.home_action_add_due),
                icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                iconTint = BadgeAmberAccent,
                tileColors = listOf(PureWhite, TileAmberEnd),
                onClick = onAddDueClick,
                modifier = Modifier
                    .weight(1f)
                    .testTag("home_quick_action_add_due")
            )
            HomeQuickActionItem(
                label = stringResource(R.string.employer_advance),
                icon = Icons.Outlined.AccountBalanceWallet,
                iconTint = PosSurplusGreen,
                tileColors = listOf(PureWhite, TileMintEnd),
                onClick = onAdvanceClick,
                modifier = Modifier
                    .weight(1f)
                    .testTag("home_quick_action_advance")
            )
            HomeQuickActionItem(
                label = stringResource(R.string.paid_out),
                icon = Icons.Outlined.ShoppingCart,
                iconTint = PosExpenseRed,
                tileColors = listOf(PureWhite, TileRoseEnd),
                onClick = onPaidOutClick,
                modifier = Modifier
                    .weight(1f)
                    .testTag("home_quick_action_paid_out")
            )
        }
    }
}

@Composable
private fun HomeQuickActionItem(
    label: String,
    icon: ImageVector,
    iconTint: Color,
    tileColors: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .defaultMinSize(minWidth = Dimens.MinTouchTarget, minHeight = Dimens.MinTouchTarget)
            .clip(RoundedCornerShape(Dimens.RadiusMd))
            .clickable(onClick = onClick)
            .padding(vertical = Dimens.SpacingXs, horizontal = Dimens.SpacingXxs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)
    ) {
        Box(
            modifier = Modifier
                .size(Dimens.HomeQuickActionTileSize)
                .depthRaisedIconTile(
                    gradientColors = tileColors,
                    cornerRadius = Dimens.HomeQuickActionTileRadius
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(Dimens.IconMd)
            )
        }
        Text(
            text = label,
            fontSize = DepthTypeTokens.HomeQuickActionLabelSize,
            fontWeight = FontWeight.SemiBold,
            color = TextSlate700,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HomeLast7DaysChartCard(
    points: List<HomeDaySalesPoint>,
    currency: String,
    locale: Locale,
    animateMotion: Boolean,
    modifier: Modifier = Modifier
) {
    val digits = remember(currency) { MoneyFormat.minorUnitDigits(currency) }
    val maxMinor = remember(points) { points.maxOfOrNull { it.netSalesMinor }?.coerceAtLeast(0L) ?: 0L }
    val hasAnySales = maxMinor > 0L
    val cardShape = RoundedCornerShape(Dimens.CardCornerRadius)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .depthCard(shape = cardShape, cornerRadius = Dimens.CardCornerRadius)
            .padding(Dimens.HomeCardPaddingLarge)
            .testTag("home_last_7_days_chart"),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMd)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.home_last_7_days),
                    fontSize = DepthTypeTokens.SectionTitleSize,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryReport
                )
                Text(
                    text = stringResource(R.string.home_last_7_days_subtitle),
                    fontSize = DepthTypeTokens.ListRowSubtitleSize,
                    color = TextPlaceholderReport
                )
            }
            Box(
                modifier = Modifier
                    .size(Dimens.SectionIconTileSize)
                    .depthRaisedIconTile(
                        gradientColors = listOf(PureWhite, TileBlueEnd),
                        cornerRadius = Dimens.RadiusSectionIconTile
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.LockOpen,
                    contentDescription = null,
                    tint = BadgeBlueText,
                    modifier = Modifier.size(Dimens.IconSm)
                )
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimens.HomeChartHeight)
        ) {
            val maxBarAreaHeight = (maxHeight - Dimens.HomeChartValueLabelHeight - Dimens.SpacingLg)
                .coerceAtLeast(Dimens.HomeChartBarMinHeight)
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                points.forEachIndexed { index, point ->
                    val growFraction = remember(animateMotion, index) {
                        Animatable(if (animateMotion) 0f else 1f)
                    }
                    LaunchedEffect(points, animateMotion) {
                        if (animateMotion) {
                            growFraction.snapTo(0f)
                            delay(index.toLong() * DepthMotionTokens.CHART_BAR_STAGGER_MS)
                            growFraction.animateTo(
                                targetValue = 1f,
                                animationSpec = tween(
                                    durationMillis = DepthMotionTokens.CHART_BAR_GROW_DURATION_MS,
                                    easing = DepthMotionTokens.CardRiseEasing
                                )
                            )
                        } else {
                            growFraction.snapTo(1f)
                        }
                    }

                    val targetRatio = if (maxMinor > 0L && point.netSalesMinor > 0L) {
                        (point.netSalesMinor.toFloat() / maxMinor.toFloat()).coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                    val barHeight = if (targetRatio <= 0f) {
                        Dimens.HomeChartBarMinHeight
                    } else {
                        (maxBarAreaHeight * (targetRatio * growFraction.value))
                            .coerceAtLeast(Dimens.HomeChartBarMinHeight)
                    }

                    val barAccessibilityText = stringResource(
                        R.string.home_cd_chart_bar_fmt,
                        point.weekdayLabel,
                        MoneyFormat.formatMinor(point.netSalesMinor, currency, locale)
                    )
                    val compactValueLabel = remember(point.netSalesMinor, digits, locale) {
                        if (point.netSalesMinor <= 0L) {
                            ""
                        } else {
                            formatCompactMajorAmount(point.netSalesMinor, digits, locale)
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .semantics { contentDescription = barAccessibilityText },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Box(
                            modifier = Modifier.height(Dimens.HomeChartValueLabelHeight),
                            contentAlignment = Alignment.Center
                        ) {
                            if (compactValueLabel.isNotEmpty()) {
                                Text(
                                    text = compactValueLabel,
                                    fontSize = DepthTypeTokens.HomeChartValueSize,
                                    fontWeight = if (point.isToday) FontWeight.Bold else FontWeight.Medium,
                                    color = if (point.isToday) PrimaryBlueLight else TextPlaceholderReport,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Clip
                                )
                            }
                        }

                        val barShape = RoundedCornerShape(
                            topStart = Dimens.HomeChartBarRadius,
                            topEnd = Dimens.HomeChartBarRadius
                        )
                        val barGradient = if (point.isToday) {
                            listOf(
                                DepthTokens.BrandButtonGradientStart,
                                DepthTokens.BrandButtonGradientEnd
                            )
                        } else {
                            listOf(
                                DepthTokens.ChartBarInactiveGradientStart,
                                DepthTokens.ChartBarInactiveGradientEnd
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(Dimens.HomeChartBarWidth)
                                .height(barHeight)
                                .then(
                                    if (point.isToday && point.netSalesMinor > 0L) {
                                        Modifier.depthChartTodayBarGlow(cornerRadius = Dimens.HomeChartBarRadius)
                                    } else {
                                        Modifier
                                    }
                                )
                                .clip(barShape)
                                .background(Brush.verticalGradient(colors = barGradient))
                        )

                        Spacer(modifier = Modifier.height(Dimens.SpacingXs))
                        Text(
                            text = point.weekdayLabel,
                            fontSize = DepthTypeTokens.HomeChartDayLabelSize,
                            fontWeight = if (point.isToday) FontWeight.Bold else FontWeight.Medium,
                            color = if (point.isToday) PrimaryBlueLight else TextSlate700,
                            maxLines = 1,
                            overflow = TextOverflow.Clip
                        )
                    }
                }
            }
        }

        if (!hasAnySales) {
            Text(
                text = stringResource(R.string.home_chart_no_sales),
                fontSize = DepthTypeTokens.ListRowSubtitleSize,
                color = TextPlaceholderReport
            )
        }
    }
}

private fun formatCompactMajorAmount(
    minor: Long,
    digits: Int,
    locale: Locale
): String {
    val factor = BigDecimal.valueOf(MoneyFormat.minorFactor(digits))
    val major = BigDecimal.valueOf(minor).divide(factor, digits.coerceAtLeast(0), RoundingMode.HALF_UP)
    val nf = NumberFormat.getNumberInstance(locale).apply {
        val isWhole = minor % MoneyFormat.minorFactor(digits) == 0L
        minimumFractionDigits = 0
        maximumFractionDigits = if (isWhole) 0 else digits.coerceAtLeast(0)
        isGroupingUsed = false
    }
    return nf.format(major)
}
