package com.moneytracker.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneytracker.app.data.model.Wallet
import com.moneytracker.app.domain.FinanceMath
import com.moneytracker.app.ui.theme.MonoStyle
import com.moneytracker.app.ui.theme.ObsidianColors
import java.time.Month
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

private val SHORT_MONTH_LABELS = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
)

/**
 * Safely parses a hex color string (#RRGGBB or #AARRGGBB) into a Compose Color.
 */
fun parseHexColor(hex: String?, fallback: Color = ObsidianColors.EmeraldPrimary): Color {
    if (hex.isNullOrBlank()) return fallback
    val cleaned = hex.trim().let { if (it.startsWith("#")) it else "#$it" }
    return runCatching { Color(android.graphics.Color.parseColor(cleaned)) }.getOrDefault(fallback)
}

/**
 * Clean Obsidian surface card with 1dp hairline border and zero specular sheen or drop shadow.
 */
@Composable
fun ObsidianSurfaceCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    containerColor: Color = ObsidianColors.JadeSurface,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    Surface(
        modifier = modifier.clip(shape),
        shape = shape,
        color = containerColor,
        contentColor = ObsidianColors.TextPrimary,
        border = BorderStroke(1.dp, ObsidianColors.Hairline),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}

/**
 * Primary solid emerald CTA button with >=48dp touch target.
 */
@Composable
fun EmeraldPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    val shape = RoundedCornerShape(12.dp)
    val bgColor = if (enabled) ObsidianColors.EmeraldPrimary else ObsidianColors.JadeMuted
    val contentColor = if (enabled) ObsidianColors.TextOnAccent else ObsidianColors.TextMuted

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clip(shape)
            .background(color = bgColor, shape = shape)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Capsule month/year selector enforcing a floor at [minYear]/[minMonth] (1-based 1..12).
 */
@Composable
fun MonthSelectorPill(
    month: Int,
    year: Int,
    minMonth: Int = FinanceMath.MIN_MONTH,
    minYear: Int = FinanceMath.MIN_YEAR,
    hideMonth: Boolean = false,
    onChange: (month: Int, year: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPickerDialog by remember { mutableStateOf(false) }

    val safeMonth = month.coerceIn(1, 12)
    val canGoPrev = if (hideMonth) {
        year > minYear
    } else {
        year > minYear || (year == minYear && safeMonth > minMonth)
    }

    val displayText = if (hideMonth) {
        year.toString()
    } else {
        val monthName = Month.of(safeMonth).getDisplayName(JavaTextStyle.SHORT, Locale.US)
        "$monthName $year"
    }

    Surface(
        modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(999.dp),
        color = ObsidianColors.JadeElevated,
        border = BorderStroke(1.dp, ObsidianColors.Hairline)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = {
                    if (!canGoPrev) return@IconButton
                    if (hideMonth) {
                        val nextYear = (year - 1).coerceAtLeast(minYear)
                        val clampedMonth = if (nextYear == minYear && safeMonth < minMonth) minMonth else safeMonth
                        onChange(clampedMonth, nextYear)
                    } else {
                        val (nextM, nextY) = if (safeMonth == 1) 12 to (year - 1) else (safeMonth - 1) to year
                        val (cy, cm) = FinanceMath.clampToMonthFloor(nextY, nextM)
                        onChange(cm, cy)
                    }
                },
                enabled = canGoPrev,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.ChevronLeft,
                    contentDescription = if (hideMonth) "Previous year" else "Previous month",
                    tint = if (canGoPrev) ObsidianColors.TextPrimary else ObsidianColors.TextMuted.copy(alpha = 0.4f)
                )
            }

            Row(
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .clickable(role = Role.Button) { showPickerDialog = true }
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.CalendarMonth,
                    contentDescription = null,
                    tint = ObsidianColors.EmeraldPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = displayText,
                    style = MonoStyle.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    ),
                    color = ObsidianColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = {
                    if (hideMonth) {
                        onChange(safeMonth, year + 1)
                    } else {
                        val (nextM, nextY) = if (safeMonth == 12) 1 to (year + 1) else (safeMonth + 1) to year
                        onChange(nextM, nextY)
                    }
                },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = if (hideMonth) "Next year" else "Next month",
                    tint = ObsidianColors.TextPrimary
                )
            }
        }
    }

    if (showPickerDialog) {
        MonthYearPickerDialog(
            initialMonth = safeMonth,
            initialYear = year,
            minMonth = minMonth,
            minYear = minYear,
            hideMonth = hideMonth,
            onSelect = { m, y ->
                showPickerDialog = false
                onChange(m, y)
            },
            onDismiss = { showPickerDialog = false }
        )
    }
}

@Composable
private fun MonthYearPickerDialog(
    initialMonth: Int,
    initialYear: Int,
    minMonth: Int,
    minYear: Int,
    hideMonth: Boolean,
    onSelect: (month: Int, year: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var pickerYear by remember(initialYear) { mutableIntStateOf(initialYear.coerceAtLeast(minYear)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianColors.JadeElevated,
        titleContentColor = ObsidianColors.TextPrimary,
        textContentColor = ObsidianColors.TextSecondary,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { if (pickerYear > minYear) pickerYear-- },
                    enabled = pickerYear > minYear,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ChevronLeft,
                        contentDescription = "Previous year",
                        tint = if (pickerYear > minYear) ObsidianColors.TextPrimary else ObsidianColors.TextMuted.copy(alpha = 0.4f)
                    )
                }
                Text(
                    text = pickerYear.toString(),
                    style = MonoStyle.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                    color = ObsidianColors.TextPrimary
                )
                IconButton(
                    onClick = { pickerYear++ },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = "Next year",
                        tint = ObsidianColors.TextPrimary
                    )
                }
            }
        },
        text = {
            if (hideMonth) {
                val years = (minYear..(minYear + 5)).toList()
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    years.chunked(3).forEach { rowYears ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowYears.forEach { y ->
                                val isSelected = y == initialYear
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            val clampedM = if (y == minYear && initialMonth < minMonth) minMonth else initialMonth
                                            onSelect(clampedM, y)
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) ObsidianColors.EmeraldPrimary else ObsidianColors.JadeSurface,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) ObsidianColors.EmeraldBright else ObsidianColors.Hairline
                                    )
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 12.dp)) {
                                        Text(
                                            text = y.toString(),
                                            style = MonoStyle.copy(fontWeight = FontWeight.SemiBold),
                                            color = if (isSelected) ObsidianColors.TextOnAccent else ObsidianColors.TextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    (1..12).chunked(3).forEach { rowMonths ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowMonths.forEach { m ->
                                val disabled = pickerYear < minYear || (pickerYear == minYear && m < minMonth)
                                val isSelected = pickerYear == initialYear && m == initialMonth
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable(enabled = !disabled) { onSelect(m, pickerYear) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = when {
                                        isSelected -> ObsidianColors.EmeraldPrimary
                                        disabled -> ObsidianColors.ObsidianCanvas.copy(alpha = 0.5f)
                                        else -> ObsidianColors.JadeSurface
                                    },
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) ObsidianColors.EmeraldBright else ObsidianColors.Hairline
                                    )
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 12.dp)) {
                                        Text(
                                            text = SHORT_MONTH_LABELS[m - 1],
                                            style = MaterialTheme.typography.labelLarge,
                                            color = when {
                                                isSelected -> ObsidianColors.TextOnAccent
                                                disabled -> ObsidianColors.TextMuted.copy(alpha = 0.45f)
                                                else -> ObsidianColors.TextPrimary
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Close", color = ObsidianColors.TextSecondary)
            }
        }
    )
}

/**
 * Horizontal wallet filter pills ("All Wallets" + each wallet with its color swatch).
 */
@Composable
fun WalletFilterChips(
    wallets: List<Wallet>,
    selectedWalletId: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val isAllSelected = selectedWalletId.isNullOrBlank() || selectedWalletId == "all"
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WalletChipItem(
            label = "All Wallets",
            dotColor = ObsidianColors.EmeraldPrimary,
            showDot = false,
            selected = isAllSelected,
            onClick = { onSelect(null) }
        )
        wallets.forEach { wallet ->
            WalletChipItem(
                label = wallet.name,
                dotColor = parseHexColor(wallet.color),
                showDot = true,
                selected = selectedWalletId == wallet.id,
                onClick = { onSelect(wallet.id) }
            )
        }
    }
}

@Composable
private fun WalletChipItem(
    label: String,
    dotColor: Color,
    showDot: Boolean,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) ObsidianColors.EmeraldPrimary else ObsidianColors.JadeElevated,
        animationSpec = tween(160),
        label = "walletChipBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) ObsidianColors.TextOnAccent else ObsidianColors.TextSecondary,
        animationSpec = tween(160),
        label = "walletChipFg"
    )
    val borderColor = if (selected) ObsidianColors.EmeraldBright else ObsidianColors.Hairline

    Box(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(999.dp))
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = bgColor,
            border = BorderStroke(1.dp, borderColor)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (showDot) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (selected) ObsidianColors.TextOnAccent else dotColor)
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Compact executive KPI stat tile with tabular currency/numeric readout and optional badge pill.
 */
@Composable
fun KpiStatTile(
    label: String,
    value: String,
    valueColor: Color = ObsidianColors.TextPrimary,
    badge: String? = null,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    ObsidianSurfaceCard(
        modifier = modifier,
        cornerRadius = 16.dp,
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = ObsidianColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (!badge.isNullOrBlank()) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(iconTint.copy(alpha = 0.14f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = badge,
                        style = MonoStyle.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = iconTint,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = value,
            style = MonoStyle.copy(
                fontSize = 19.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Bold
            ),
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Material 3 destructive confirmation dialog.
 */
@Composable
fun ConfirmDeleteDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianColors.JadeElevated,
        titleContentColor = ObsidianColors.TextPrimary,
        textContentColor = ObsidianColors.TextSecondary,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = ObsidianColors.TextPrimary
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = ObsidianColors.TextSecondary
            )
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text(
                    text = "Cancel",
                    color = ObsidianColors.TextSecondary,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier.heightIn(min = 48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ObsidianColors.Danger,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Delete",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    )
}

/**
 * Clean Obsidian empty state card with optional primary CTA.
 */
@Composable
fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    ObsidianSurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 28.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ObsidianColors.TextSecondary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = ObsidianColors.TextPrimary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ObsidianColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (!actionLabel.isNullOrBlank() && onAction != null) {
                Spacer(modifier = Modifier.height(18.dp))
                EmeraldPrimaryButton(
                    text = actionLabel,
                    onClick = onAction
                )
            }
        }
    }
}

/**
 * Reusable capsule segmented pill control matching Obsidian Ledger's dark jade wells.
 */
@Composable
fun <T> ObsidianSegmentedPill(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = ObsidianColors.ObsidianCanvas
) {
    Surface(
        modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(999.dp),
        color = containerColor,
        border = BorderStroke(1.dp, ObsidianColors.Hairline)
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEach { (value, label) ->
                val isSelected = value == selected
                val bg by animateColorAsState(
                    targetValue = if (isSelected) ObsidianColors.EmeraldPrimary else Color.Transparent,
                    animationSpec = tween(160),
                    label = "segBg"
                )
                val fg by animateColorAsState(
                    targetValue = if (isSelected) ObsidianColors.TextOnAccent else ObsidianColors.TextSecondary,
                    animationSpec = tween(160),
                    label = "segFg"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 40.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(bg)
                        .clickable(role = Role.Tab) { onSelect(value) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = fg,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Clean geometric brand mark with crisp ledger pulse line.
 */
@Composable
fun MoneyTrackerLogoMark(
    size: Dp = 36.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val radius = w * 0.26f

        drawRoundRect(
            color = ObsidianColors.EmeraldPrimary,
            cornerRadius = CornerRadius(radius, radius)
        )

        val pulsePath = Path().apply {
            moveTo(w * 0.22f, h * 0.64f)
            lineTo(w * 0.40f, h * 0.64f)
            lineTo(w * 0.50f, h * 0.42f)
            lineTo(w * 0.60f, h * 0.56f)
            lineTo(w * 0.78f, h * 0.30f)
        }
        drawPath(
            path = pulsePath,
            color = ObsidianColors.TextOnAccent,
            style = Stroke(
                width = w * 0.085f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}
