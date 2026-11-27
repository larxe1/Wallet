package com.moneytracker.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneytracker.app.data.model.CategoryAverage
import com.moneytracker.app.data.model.CategorySpend
import com.moneytracker.app.data.model.TrendPoint
import com.moneytracker.app.domain.FinanceMath
import com.moneytracker.app.ui.theme.MonoStyle
import com.moneytracker.app.ui.theme.ObsidianColors
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Custom Canvas bar chart with rounded top caps, diagonal hatched inactive bars,
 * electric-violet active/focused bar highlight, and tap-to-inspect readout.
 */
@Composable
fun TrendBarChart(
    trendData: List<TrendPoint>,
    isPrivacyMode: Boolean,
    modifier: Modifier = Modifier
) {
    if (trendData.isEmpty()) {
        ChartEmptyPlaceholder(label = "No trend data yet", modifier = modifier)
        return
    }

    val defaultSelected = remember(trendData) {
        trendData.indexOfLast { it.total > 0.0 }.takeIf { it >= 0 } ?: trendData.lastIndex
    }
    var selectedIndex by remember(trendData) { mutableIntStateOf(defaultSelected) }
    val safeSelected = selectedIndex.coerceIn(0, trendData.lastIndex)
    val selectedPoint = trendData[safeSelected]
    val maxVal = remember(trendData) { (trendData.maxOfOrNull { it.total } ?: 0.0).coerceAtLeast(1.0) }

    Column(modifier = modifier.fillMaxWidth()) {
        // Selected period tooltip pill
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(ObsidianColors.JadeElevated)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(ObsidianColors.ElectricViolet)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = selectedPoint.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = ObsidianColors.TextSecondary,
                    maxLines = 1
                )
            }
            Text(
                text = FinanceMath.formatPhpCurrency(
                    amount = selectedPoint.total,
                    masked = isPrivacyMode
                ),
                style = MonoStyle.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                color = ObsidianColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Canvas bar plot
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(164.dp)
                .pointerInput(trendData) {
                    detectTapGestures { offset ->
                        val count = trendData.size
                        if (count > 0 && size.width > 0) {
                            val slotW = size.width.toFloat() / count
                            val idx = (offset.x / slotW).toInt().coerceIn(0, count - 1)
                            selectedIndex = idx
                        }
                    }
                }
        ) {
            val w = size.width
            val h = size.height
            val topPad = 10.dp.toPx()
            val bottomPad = 6.dp.toPx()
            val chartH = (h - topPad - bottomPad).coerceAtLeast(1f)
            val count = trendData.size
            val slotW = w / count
            val barW = (slotW * 0.58f).coerceIn(10.dp.toPx(), 40.dp.toPx())
            val topR = 10.dp.toPx().coerceAtMost(barW * 0.45f)
            val bottomR = 4.dp.toPx().coerceAtMost(barW * 0.25f)

            // Subtle dashed horizontal grid lines
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 6.dp.toPx()), 0f)
            listOf(0f, 0.5f, 1f).forEach { frac ->
                val y = topPad + chartH * frac
                drawLine(
                    color = ObsidianColors.Hairline,
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = if (frac < 1f) dashEffect else null
                )
            }

            trendData.forEachIndexed { idx, point ->
                val centerX = slotW * idx + slotW * 0.5f
                val ratio = (point.total / maxVal).toFloat().coerceIn(0f, 1f)
                val barH = if (point.total > 0.0) {
                    (chartH * ratio).coerceAtLeast(10.dp.toPx())
                } else {
                    4.dp.toPx()
                }
                val left = centerX - barW * 0.5f
                val top = topPad + chartH - barH
                val right = left + barW
                val bottom = topPad + chartH

                val barPath = Path().apply {
                    addRoundRect(
                        RoundRect(
                            rect = Rect(left, top, right, bottom),
                            topLeft = CornerRadius(topR, topR),
                            topRight = CornerRadius(topR, topR),
                            bottomRight = CornerRadius(bottomR, bottomR),
                            bottomLeft = CornerRadius(bottomR, bottomR)
                        )
                    )
                }

                val isSelected = idx == safeSelected
                if (isSelected) {
                    drawPath(
                        path = barPath,
                        color = ObsidianColors.ElectricViolet
                    )
                } else {
                    drawPath(
                        path = barPath,
                        color = ObsidianColors.JadeMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // X-Axis month labels
        Row(modifier = Modifier.fillMaxWidth()) {
            trendData.forEachIndexed { idx, point ->
                val isSelected = idx == safeSelected
                Text(
                    text = if (trendData.size > 8) point.label.take(1) else point.label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MonoStyle.copy(
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (isSelected) ObsidianColors.ElectricViolet else ObsidianColors.TextMuted,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Custom Canvas rounded-cap donut chart with Obsidian categorical palette and interactive legend.
 */
@Composable
fun CategoryDonutChart(
    categories: List<CategorySpend>,
    isPrivacyMode: Boolean,
    modifier: Modifier = Modifier
) {
    if (categories.isEmpty()) {
        ChartEmptyPlaceholder(label = "No expenses in this period", modifier = modifier)
        return
    }

    val total = remember(categories) { categories.sumOf { it.totalAmount }.coerceAtLeast(0.01) }
    var selectedIdx by remember(categories) { mutableIntStateOf(0) }
    val safeSelected = selectedIdx.coerceIn(0, categories.lastIndex)
    val highlighted = categories[safeSelected]

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(184.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(176.dp)) {
                val strokeWidth = 22.dp.toPx()
                val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)

                // Track ring
                drawArc(
                    color = ObsidianColors.JadeMuted,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth)
                )

                val gapAngle = if (categories.size > 1) 7f else 0f
                val usableAngle = (360f - gapAngle * categories.size).coerceAtLeast(60f)
                var currentStart = -90f

                categories.forEachIndexed { idx, cat ->
                    val share = (cat.totalAmount / total).toFloat().coerceIn(0f, 1f)
                    val sweep = (usableAngle * share).coerceAtLeast(3f)
                    val sliceColor = ObsidianColors.ChartPalette[idx % ObsidianColors.ChartPalette.size]
                    val isSelected = idx == safeSelected

                    drawArc(
                        color = if (isSelected) sliceColor else sliceColor.copy(alpha = 0.78f),
                        startAngle = currentStart,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(
                            width = if (isSelected) strokeWidth + 3.dp.toPx() else strokeWidth,
                            cap = StrokeCap.Round
                        )
                    )
                    currentStart += sweep + gapAngle
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Text(
                    text = highlighted.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = ObsidianColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = FinanceMath.formatPhpCurrency(
                        amount = highlighted.totalAmount,
                        compact = true,
                        masked = isPrivacyMode
                    ),
                    style = MonoStyle.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                    color = ObsidianColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val pct = ((highlighted.totalAmount / total) * 100.0).roundToInt()
                Text(
                    text = "$pct%",
                    style = MonoStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                    color = ObsidianColors.ChartPalette[safeSelected % ObsidianColors.ChartPalette.size],
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Legend rows
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            categories.take(6).forEachIndexed { idx, cat ->
                val sliceColor = ObsidianColors.ChartPalette[idx % ObsidianColors.ChartPalette.size]
                val isSelected = idx == safeSelected
                val pct = ((cat.totalAmount / total) * 100.0).roundToInt()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) ObsidianColors.JadeElevated else Color.Transparent)
                        .clickable { selectedIdx = idx }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(sliceColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        CategoryIcon(
                            categoryName = cat.name,
                            storedIcon = cat.icon,
                            tint = ObsidianColors.TextSecondary,
                            size = 15.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = cat.name,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            ),
                            color = ObsidianColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$pct%",
                        style = MonoStyle.copy(fontSize = 12.sp),
                        color = ObsidianColors.TextMuted,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = FinanceMath.formatPhpCurrency(
                            amount = cat.totalAmount,
                            compact = true,
                            masked = isPrivacyMode
                        ),
                        style = MonoStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                        color = ObsidianColors.TextPrimary,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * Top category averages with proportional emerald-to-violet rounded progress bars.
 */
@Composable
fun CategoryAveragesList(
    averages: List<CategoryAverage>,
    isPrivacyMode: Boolean,
    modifier: Modifier = Modifier
) {
    if (averages.isEmpty()) {
        ChartEmptyPlaceholder(label = "No category averages yet", modifier = modifier)
        return
    }

    val maxAvg = remember(averages) { (averages.maxOfOrNull { it.amount } ?: 1.0).coerceAtLeast(1.0) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        averages.forEachIndexed { idx, avg ->
            val ratio = (avg.amount / maxAvg).toFloat().coerceIn(0.04f, 1f)
            val barColor = ObsidianColors.ChartPalette[idx % ObsidianColors.ChartPalette.size]

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = avg.categoryName,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = ObsidianColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "${FinanceMath.formatPhpCurrency(avg.amount, masked = isPrivacyMode)}/mo",
                        style = MonoStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                        color = ObsidianColors.TextSecondary,
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(ObsidianColors.JadeMuted)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(ratio)
                            .height(6.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(barColor)
                    )
                }
            }
        }
    }
}

/**
 * Smooth cubic Bezier Canvas sparkline with translucent emerald area fill and terminal vertex dot.
 */
@Composable
fun SpendingPaceSparkline(
    trendData: List<TrendPoint>,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
    ) {
        val w = size.width
        val h = size.height
        val padX = 6.dp.toPx()
        val padTop = 8.dp.toPx()
        val padBottom = 6.dp.toPx()
        val plotW = (w - padX * 2).coerceAtLeast(1f)
        val plotH = (h - padTop - padBottom).coerceAtLeast(1f)

        val values = trendData.map { max(0.0, it.total) }
        if (values.isEmpty()) {
            val y = h - padBottom - 8.dp.toPx()
            drawLine(
                color = ObsidianColors.EmeraldPrimary.copy(alpha = 0.45f),
                start = Offset(padX, y),
                end = Offset(w - padX, y),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
            return@Canvas
        }

        val maxVal = values.maxOrNull() ?: 0.0
        val minVal = values.minOrNull() ?: 0.0
        val span = maxVal - minVal

        val points = values.mapIndexed { idx, value ->
            val x = if (values.size == 1) {
                w * 0.5f
            } else {
                padX + (idx.toFloat() / (values.size - 1)) * plotW
            }
            val norm = when {
                maxVal == 0.0 -> 0f
                span == 0.0 -> 0.5f
                else -> ((value - minVal) / span).toFloat()
            }
            val y = padTop + plotH - norm * plotH
            Offset(x, y)
        }

        val linePath = Path()
        if (points.size == 1) {
            val y = points.first().y
            linePath.moveTo(padX, y)
            linePath.lineTo(w - padX, y)
        } else {
            linePath.moveTo(points.first().x, points.first().y)
            for (i in 0 until points.lastIndex) {
                val curr = points[i]
                val next = points[i + 1]
                val midX = (curr.x + next.x) * 0.5f
                linePath.cubicTo(midX, curr.y, midX, next.y, next.x, next.y)
            }
        }

        val lastPt = if (points.size == 1) Offset(w - padX, points.first().y) else points.last()
        val firstX = if (points.size == 1) padX else points.first().x

        val areaPath = Path().apply {
            addPath(linePath)
            lineTo(lastPt.x, h)
            lineTo(firstX, h)
            close()
        }

        drawPath(
            path = areaPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    ObsidianColors.EmeraldPrimary.copy(alpha = 0.14f),
                    ObsidianColors.EmeraldPrimary.copy(alpha = 0.0f)
                ),
                startY = padTop,
                endY = h
            )
        )

        drawPath(
            path = linePath,
            color = ObsidianColors.EmeraldPrimary,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )

        drawCircle(
            color = ObsidianColors.EmeraldPrimary,
            radius = 3.dp.toPx(),
            center = lastPt
        )
    }
}

@Composable
private fun ChartEmptyPlaceholder(
    label: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = ObsidianColors.TextMuted,
            textAlign = TextAlign.Center
        )
    }
}
