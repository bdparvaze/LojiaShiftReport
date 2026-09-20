package com.lojia.pos.report

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojia.pos.ui.theme.*

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
        shape = RoundedCornerShape(14.dp),
        color = PureWhite,
        border = androidx.compose.foundation.BorderStroke(1.dp, OutlineLight),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondaryLight,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.5.sp
                    ),
                    maxLines = 1
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.5.sp,
                    color = TextPrimaryLight
                ),
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondaryLight,
                    fontSize = 10.5.sp
                ),
                maxLines = 1
            )
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
            .clip(RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(if (isActive) color else color.copy(alpha = 0.3f))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
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
    showPosBreakdown: Boolean,
    showShiftBreakdown: Boolean,
    onHoverIndexChange: (Int?) -> Unit
) {
    val maxRevenue = remember(data) {
        (data.maxOfOrNull { it.totalRevenue } ?: 100.0).coerceAtLeast(100.0) * 1.15
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
        val paddingBottom = 24.dp.toPx()
        val paddingTop = 12.dp.toPx()
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
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
            )
        }

        fun getPoint(index: Int, value: Double): Offset {
            val x = index * stepX
            val y = paddingTop + chartHeight - (value / maxRevenue).toFloat() * chartHeight
            return Offset(x, y.coerceIn(paddingTop, paddingTop + chartHeight))
        }

        if (showPosBreakdown) {
            val posPath = Path()
            data.forEachIndexed { idx, d ->
                val pt = getPoint(idx, d.posSales)
                if (idx == 0) posPath.moveTo(pt.x, pt.y) else posPath.lineTo(pt.x, pt.y)
            }
            drawPath(
                path = posPath,
                color = AccentEmerald.copy(alpha = 0.5f),
                style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f))
            )
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
                style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f))
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
                    width = 3.dp.toPx(),
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
                    radius = 2.dp.toPx(),
                    center = Offset(pt.x, paddingTop + chartHeight + 4.dp.toPx())
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
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                )

                drawCircle(
                    color = PrimaryBlue.copy(alpha = 0.25f),
                    radius = 8.dp.toPx(),
                    center = targetPt
                )

                drawCircle(
                    color = PureWhite,
                    radius = 4.5.dp.toPx(),
                    center = targetPt
                )
                drawCircle(
                    color = PrimaryBlue,
                    radius = 3.dp.toPx(),
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
        (data.maxOfOrNull { it.totalRevenue } ?: 100.0).coerceAtLeast(100.0) * 1.15
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
        val paddingBottom = 20.dp.toPx()
        val paddingTop = 12.dp.toPx()
        val chartHeight = height - paddingBottom - paddingTop
        val n = data.size
        val barSlotWidth = width / n.toFloat()
        val barWidth = (barSlotWidth * 0.65f).coerceIn(4.dp.toPx(), 18.dp.toPx())

        val gridLinesCount = 4
        for (i in 0 until gridLinesCount) {
            val y = paddingTop + (chartHeight / (gridLinesCount - 1)) * i
            drawLine(
                color = OutlineLight.copy(alpha = 0.8f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
            )
        }

        data.forEachIndexed { idx, d ->
            val barHeight = ((d.totalRevenue / maxRevenue).toFloat() * chartHeight).coerceAtLeast(3.dp.toPx())
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
                        radiusX = 4.dp.toPx(),
                        radiusY = 4.dp.toPx()
                    )
                )
            }

            drawPath(
                path = barPath,
                brush = Brush.verticalGradient(
                    colors = if (isSelected) {
                        listOf(PrimaryBlue, Color(0xFF1D4ED8))
                    } else if (d.totalRevenue > 0) {
                        listOf(Color(0xFF3B82F6), Color(0xFF60A5FA).copy(alpha = 0.7f))
                    } else {
                        listOf(Color(0xFFCBD5E1), Color(0xFFE2E8F0))
                    },
                    startY = top,
                    endY = paddingTop + chartHeight
                )
            )
        }
    }
}
