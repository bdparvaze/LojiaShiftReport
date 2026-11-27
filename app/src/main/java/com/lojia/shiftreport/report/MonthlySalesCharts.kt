package com.lojia.shiftreport.report

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import com.lojia.shiftreport.ui.theme.*

private const val DEFAULT_CHART_MIN_REVENUE = 100.0
private const val CHART_Y_AXIS_HEADROOM = 1.15

@Composable
fun MonthlyKpiCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(Dimens.CardCornerRadius),
        color = Color.Transparent,
        modifier = modifier
            .defaultMinSize(minHeight = Dimens.MonthlyKpiCardMinHeight)
            .depthCard(shape = RoundedCornerShape(Dimens.CardCornerRadius))
    ) {
        Column(
            modifier = Modifier.padding(Dimens.CardPadding),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondaryLight,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Box(
                    modifier = Modifier
                        .size(Dimens.KpiIconTileSize)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(Dimens.IconMd)
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.SpacingSm))

            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineLarge.asMoneyTabular().copy(
                        fontWeight = FontWeight.Bold,
                        color = OnBackgroundLight
                    ),
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(Dimens.SpacingXxs))

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondaryLight
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun LegendItem(
    label: String,
    color: Color,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .heightIn(min = Dimens.MinTouchTarget)
            .clip(RoundedCornerShape(Dimens.RadiusSm))
            .clickable { onClick() }
            .padding(horizontal = Dimens.SpacingXs, vertical = Dimens.SpacingXs)
    ) {
        Box(
            modifier = Modifier
                .size(Dimens.HomeBreakdownSwatchSize)
                .clip(CircleShape)
                .background(if (isActive) color else color.copy(alpha = 0.3f))
        )
        Spacer(modifier = Modifier.width(Dimens.SpacingXs))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isActive) TextPrimaryLight else TextSecondaryLight
        )
    }
}

/**
 * Recharts-Styled Smooth Curved Area Canvas with Horizontal Gridlines & Interactive Touch Drag
 */
@Composable
fun RechartsAreaTrendCanvas(
    data: List<DaySalesData>,
    currency: String,
    hoveredIndex: Int?,
    showShiftBreakdown: Boolean,
    onHoverIndexChange: (Int?) -> Unit
) {
    val maxRevenue = remember(data) {
        (data.maxOfOrNull { it.totalRevenue } ?: DEFAULT_CHART_MIN_REVENUE)
            .coerceAtLeast(DEFAULT_CHART_MIN_REVENUE) * CHART_Y_AXIS_HEADROOM
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(data) {
                detectTapGestures(
                    onPress = { offset ->
                        val itemWidth = size.width / data.size.toFloat()
                        val index = (offset.x / itemWidth).toInt().coerceIn(0, data.size - 1)
                        onHoverIndexChange(index)
                    }
                )
            }
            .pointerInput(data) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val itemWidth = size.width / data.size.toFloat()
                        val index = (offset.x / itemWidth).toInt().coerceIn(0, data.size - 1)
                        onHoverIndexChange(index)
                    },
                    onDrag = { change, _ ->
                        val itemWidth = size.width / data.size.toFloat()
                        val index = (change.position.x / itemWidth).toInt().coerceIn(0, data.size - 1)
                        onHoverIndexChange(index)
                    },
                    onDragEnd = {},
                    onDragCancel = {}
                )
            }
    ) {
        val width = size.width
        val height = size.height
        val paddingBottom = Dimens.SpacingXl.toPx()
        val paddingTop = Dimens.SpacingMd.toPx()
        val chartHeight = height - paddingBottom - paddingTop
        val n = data.size
        val stepX = width / (n - 1).coerceAtLeast(1)

        val gridLinesCount = 4
        for (i in 0 until gridLinesCount) {
            val y = paddingTop + (chartHeight / (gridLinesCount - 1)) * i
            drawLine(
                color = OutlineLight.copy(alpha = 0.8f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = Dimens.FlatBorderWidth.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
            )
        }

        fun getPoint(index: Int, value: Double): Offset {
            val x = index * stepX
            val y = paddingTop + chartHeight - (value / maxRevenue).toFloat() * chartHeight
            return Offset(x, y.coerceIn(paddingTop, paddingTop + chartHeight))
        }

        if (showShiftBreakdown) {
            val shiftPath = Path()
            data.forEachIndexed { idx, d ->
                val pt = getPoint(idx, d.shiftSales)
                if (idx == 0) shiftPath.moveTo(pt.x, pt.y) else shiftPath.lineTo(pt.x, pt.y)
            }
            drawPath(
                path = shiftPath,
                color = AccentGold.copy(alpha = 0.5f),
                style = Stroke(width = Dimens.FlatFocusedBorderWidth.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f))
            )
        }

        val strokePath = Path()
        val fillPath = Path()

        val points = data.mapIndexed { idx, d -> getPoint(idx, d.totalRevenue) }

        if (points.isNotEmpty()) {
            strokePath.moveTo(points.first().x, points.first().y)
            fillPath.moveTo(points.first().x, paddingTop + chartHeight)
            fillPath.lineTo(points.first().x, points.first().y)

            for (i in 0 until points.size - 1) {
                val p0 = points[i]
                val p1 = points[i + 1]
                val controlX = (p0.x + p1.x) / 2
                strokePath.cubicTo(controlX, p0.y, controlX, p1.y, p1.x, p1.y)
                fillPath.cubicTo(controlX, p0.y, controlX, p1.y, p1.x, p1.y)
            }

            fillPath.lineTo(points.last().x, paddingTop + chartHeight)
            fillPath.close()

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        PrimaryBlue.copy(alpha = 0.40f),
                        PrimaryBlue.copy(alpha = 0.12f),
                        PrimaryBlue.copy(alpha = 0.01f)
                    ),
                    startY = paddingTop,
                    endY = paddingTop + chartHeight
                )
            )

            drawPath(
                path = strokePath,
                color = PrimaryBlue,
                style = Stroke(
                    width = Dimens.ChartStrokeMedium.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }

        data.forEachIndexed { idx, d ->
            if (d.dayNumber == 1 || d.dayNumber % 5 == 0 || d.dayNumber == data.size) {
                val pt = getPoint(idx, 0.0)
                drawCircle(
                    color = OutlineLight,
                    radius = Dimens.ChartAxisDotRadius.toPx(),
                    center = Offset(pt.x, paddingTop + chartHeight + Dimens.SpacingXs.toPx())
                )
            }
        }

        hoveredIndex?.let { idx ->
            if (idx in points.indices) {
                val targetPt = points[idx]

                drawLine(
                    color = PrimaryBlue.copy(alpha = 0.6f),
                    start = Offset(targetPt.x, paddingTop),
                    end = Offset(targetPt.x, paddingTop + chartHeight),
                    strokeWidth = Dimens.FlatFocusedBorderWidth.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                )

                drawCircle(
                    color = PrimaryBlue.copy(alpha = 0.25f),
                    radius = Dimens.ChartHighlightHaloRadius.toPx(),
                    center = targetPt
                )

                drawCircle(
                    color = PureWhite,
                    radius = Dimens.ChartHighlightDotRadius.toPx(),
                    center = targetPt
                )
                drawCircle(
                    color = PrimaryBlue,
                    radius = Dimens.ChartHighlightCenterRadius.toPx(),
                    center = targetPt
                )
            }
        }
    }
}

/**
 * Recharts-Styled Modern Bar Chart with Rounded Cap Bars & Interactive Selection
 */
@Composable
fun RechartsBarChartCanvas(
    data: List<DaySalesData>,
    currency: String,
    hoveredIndex: Int?,
    onHoverIndexChange: (Int?) -> Unit
) {
    val maxRevenue = remember(data) {
        (data.maxOfOrNull { it.totalRevenue } ?: DEFAULT_CHART_MIN_REVENUE)
            .coerceAtLeast(DEFAULT_CHART_MIN_REVENUE) * CHART_Y_AXIS_HEADROOM
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(data) {
                detectTapGestures(
                    onPress = { offset ->
                        val itemWidth = size.width / data.size.toFloat()
                        val index = (offset.x / itemWidth).toInt().coerceIn(0, data.size - 1)
                        onHoverIndexChange(index)
                    }
                )
            }
            .pointerInput(data) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val itemWidth = size.width / data.size.toFloat()
                        val index = (offset.x / itemWidth).toInt().coerceIn(0, data.size - 1)
                        onHoverIndexChange(index)
                    },
                    onDrag = { change, _ ->
                        val itemWidth = size.width / data.size.toFloat()
                        val index = (change.position.x / itemWidth).toInt().coerceIn(0, data.size - 1)
                        onHoverIndexChange(index)
                    },
                    onDragEnd = {},
                    onDragCancel = {}
                )
            }
    ) {
        val width = size.width
        val height = size.height
        val paddingBottom = Dimens.IconAction.toPx()
        val paddingTop = Dimens.SpacingMd.toPx()
        val chartHeight = height - paddingBottom - paddingTop
        val n = data.size
        val barSlotWidth = width / n.toFloat()
        val barWidth = (barSlotWidth * 0.65f).coerceIn(Dimens.ChartBarMinWidth.toPx(), Dimens.ChartBarMaxWidth.toPx())

        val gridLinesCount = 4
        for (i in 0 until gridLinesCount) {
            val y = paddingTop + (chartHeight / (gridLinesCount - 1)) * i
            drawLine(
                color = OutlineLight.copy(alpha = 0.8f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = Dimens.FlatBorderWidth.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
            )
        }

        data.forEachIndexed { idx, d ->
            val barHeight = ((d.totalRevenue / maxRevenue).toFloat() * chartHeight).coerceAtLeast(Dimens.ChartStrokeMedium.toPx())
            val centerX = idx * barSlotWidth + barSlotWidth / 2
            val left = centerX - barWidth / 2
            val top = paddingTop + chartHeight - barHeight

            val isSelected = hoveredIndex == idx

            val barPath = Path().apply {
                addRoundRect(
                    androidx.compose.ui.geometry.RoundRect(
                        left = left,
                        top = top,
                        right = left + barWidth,
                        bottom = paddingTop + chartHeight,
                        radiusX = Dimens.RadiusXs.toPx(),
                        radiusY = Dimens.RadiusXs.toPx()
                    )
                )
            }

            drawPath(
                path = barPath,
                brush = Brush.verticalGradient(
                    colors = if (isSelected) {
                        listOf(PrimaryBlue, PrimaryIndigoDark)
                    } else if (d.totalRevenue > 0) {
                        listOf(PrimaryLight, PrimaryLight.copy(alpha = 0.7f))
                    } else {
                        listOf(OutlineLight, OutlineVariantLight)
                    },
                    startY = top,
                    endY = paddingTop + chartHeight
                )
            )
        }
    }
}
