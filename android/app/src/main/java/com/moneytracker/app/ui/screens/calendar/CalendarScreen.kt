package com.moneytracker.app.ui.screens.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moneytracker.app.data.model.Expense
import com.moneytracker.app.data.model.Income
import com.moneytracker.app.domain.FinanceMath
import com.moneytracker.app.ui.components.ConfirmDeleteDialog
import com.moneytracker.app.ui.components.ExpenseFormSheet
import com.moneytracker.app.ui.components.IncomeFormSheet
import com.moneytracker.app.ui.components.MonthSelectorPill
import com.moneytracker.app.ui.components.ObsidianSurfaceCard
import com.moneytracker.app.ui.components.WalletFilterChips
import com.moneytracker.app.ui.theme.MonoStyle
import com.moneytracker.app.ui.theme.ObsidianColors
import com.moneytracker.app.viewmodel.CalendarDayTotals
import com.moneytracker.app.viewmodel.CalendarViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val WEEKDAY_LABELS = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
private val MONTH_HEADER_FMT = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    onSnackbar: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var showExpenseSheet by remember { mutableStateOf(false) }
    var showIncomeSheet by remember { mutableStateOf(false) }
    var pendingDeleteExpense by remember { mutableStateOf<Expense?>(null) }
    var pendingDeleteIncome by remember { mutableStateOf<Income?>(null) }

    LaunchedEffect(state.message, state.error) {
        val msg = state.message ?: state.error
        if (msg != null) {
            onSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    val calendarWeeks = remember(state.selectedYear, state.selectedMonth) {
        buildMonthGridWeeks(state.selectedYear, state.selectedMonth)
    }
    val today = remember { LocalDate.now() }
    val monthTitle = remember(state.selectedYear, state.selectedMonth) {
        YearMonth.of(state.selectedYear, state.selectedMonth).atDay(1).format(MONTH_HEADER_FMT)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianColors.ObsidianCanvas),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MonthSelectorPill(
                    month = state.selectedMonth,
                    year = state.selectedYear,
                    onChange = { m, y -> viewModel.setMonthYear(y, m) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (state.isLoading) {
            item {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = ObsidianColors.EmeraldPrimary,
                    trackColor = ObsidianColors.JadeElevated
                )
            }
        }

        item {
            WalletFilterChips(
                wallets = state.wallets,
                selectedWalletId = state.selectedWalletId,
                onSelect = { viewModel.setWalletFilter(it) }
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionButton(
                    label = "Expense",
                    tint = ObsidianColors.Danger,
                    onClick = { showExpenseSheet = true },
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    label = "Income",
                    tint = ObsidianColors.EmeraldPrimary,
                    onClick = { showIncomeSheet = true },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            ObsidianSurfaceCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = monthTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = ObsidianColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CalendarLegendItem(
                            color = ObsidianColors.EmeraldPrimary,
                            label = "In"
                        )
                        CalendarLegendItem(
                            color = ObsidianColors.Danger,
                            label = "Out"
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Repeat,
                                contentDescription = null,
                                tint = ObsidianColors.ElectricViolet,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "Proj",
                                style = MaterialTheme.typography.labelSmall,
                                color = ObsidianColors.TextMuted,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    WEEKDAY_LABELS.forEach { dayName ->
                        Text(
                            text = dayName,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall,
                            color = ObsidianColors.TextMuted,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    calendarWeeks.forEach { week ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            week.forEach { day ->
                                val dateStr = day.format(FinanceMath.ISO_DATE)
                                val isCurrentMonth =
                                    day.year == state.selectedYear && day.monthValue == state.selectedMonth
                                val isToday = day == today
                                val isSelected = state.selectedDate == dateStr
                                val totals = state.dayTotals[dateStr]

                                CalendarDayCell(
                                    day = day,
                                    isCurrentMonth = isCurrentMonth,
                                    isToday = isToday,
                                    isSelected = isSelected,
                                    totals = totals,
                                    onClick = { viewModel.selectDate(dateStr) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    val activeDate = state.selectedDate
    if (activeDate != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { viewModel.selectDate(null) },
            sheetState = sheetState,
            containerColor = ObsidianColors.JadeElevated,
            contentColor = ObsidianColors.TextPrimary
        ) {
            DayDetailSheetContent(
                dateStr = activeDate,
                dayIncome = state.selectedDayIncome,
                dayExpenses = state.selectedDayExpenses,
                onClose = { viewModel.selectDate(null) },
                onAddExpense = { showExpenseSheet = true },
                onAddIncome = { showIncomeSheet = true },
                onDeleteIncome = { pendingDeleteIncome = it },
                onDeleteExpense = { pendingDeleteExpense = it }
            )
        }
    }

    if (showExpenseSheet) {
        ExpenseFormSheet(
            editingExpense = null,
            wallets = state.wallets,
            categories = state.categories,
            paymentMethods = state.paymentMethods,
            defaultWalletId = state.selectedWalletId,
            defaultDate = state.selectedDate,
            onDismiss = { showExpenseSheet = false },
            onSave = { walletId, categoryId, paymentMethodId, amount, description, expenseDate, installmentMonths ->
                viewModel.addExpense(
                    walletId = walletId,
                    categoryId = categoryId,
                    paymentMethodId = paymentMethodId,
                    amount = amount,
                    description = description,
                    expenseDate = expenseDate,
                    installmentMonths = installmentMonths
                ) { success ->
                    if (success) showExpenseSheet = false
                }
            }
        )
    }

    if (showIncomeSheet) {
        IncomeFormSheet(
            editingIncome = null,
            wallets = state.wallets,
            defaultWalletId = state.selectedWalletId,
            defaultDate = state.selectedDate,
            onDismiss = { showIncomeSheet = false },
            onSave = { walletId, amount, description, incomeDate ->
                viewModel.addIncome(
                    walletId = walletId,
                    amount = amount,
                    description = description,
                    incomeDate = incomeDate
                ) { success ->
                    if (success) showIncomeSheet = false
                }
            }
        )
    }

    pendingDeleteExpense?.let { exp ->
        val label = exp.description?.takeIf { it.isNotBlank() }
            ?: exp.categories?.name
            ?: "Expense"
        ConfirmDeleteDialog(
            title = "Delete Expense",
            message = "Remove \"$label\" (${FinanceMath.formatPhpCurrency(exp.amount)})?",
            onConfirm = {
                viewModel.deleteExpense(exp.id)
                pendingDeleteExpense = null
            },
            onDismiss = { pendingDeleteExpense = null }
        )
    }

    pendingDeleteIncome?.let { inc ->
        val label = inc.description?.takeIf { it.isNotBlank() } ?: "Income"
        ConfirmDeleteDialog(
            title = "Delete Income",
            message = "Remove \"$label\" (${FinanceMath.formatPhpCurrency(inc.amount)})?",
            onConfirm = {
                viewModel.deleteIncome(inc.id)
                pendingDeleteIncome = null
            },
            onDismiss = { pendingDeleteIncome = null }
        )
    }
}

@Composable
private fun QuickActionButton(
    label: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 48.dp),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.45f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = tint.copy(alpha = 0.12f),
            contentColor = tint
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CalendarLegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = ObsidianColors.TextMuted,
            maxLines = 1
        )
    }
}

@Composable
private fun CalendarDayCell(
    day: LocalDate,
    isCurrentMonth: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    totals: CalendarDayTotals?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = when {
        isSelected -> ObsidianColors.EmeraldPrimary.copy(alpha = 0.18f)
        isToday -> ObsidianColors.JadeElevated
        else -> ObsidianColors.JadeMuted.copy(alpha = 0.55f)
    }
    val borderColor = when {
        isSelected -> ObsidianColors.EmeraldPrimary
        isToday -> ObsidianColors.EmeraldPrimary.copy(alpha = 0.55f)
        else -> ObsidianColors.Hairline
    }

    Column(
        modifier = modifier
            .defaultMinSize(minHeight = 58.dp)
            .alpha(if (isCurrentMonth) 1f else 0.36f)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 5.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (totals?.hasProjected == true) {
                Icon(
                    imageVector = Icons.Rounded.Repeat,
                    contentDescription = "Projected recurring",
                    tint = ObsidianColors.ElectricViolet,
                    modifier = Modifier.size(10.dp)
                )
            } else {
                Spacer(modifier = Modifier.size(10.dp))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (isToday) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(ObsidianColors.EmeraldPrimary)
                    )
                }
                Text(
                    text = day.dayOfMonth.toString(),
                    style = MonoStyle.copy(
                        fontSize = 11.sp,
                        fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (isToday || isSelected) {
                        ObsidianColors.EmeraldBright
                    } else {
                        ObsidianColors.TextPrimary
                    },
                    maxLines = 1
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (totals != null && totals.totalIncome > 0.0) {
                DayAmountPill(
                    prefix = "+",
                    amount = totals.totalIncome,
                    tint = ObsidianColors.EmeraldPrimary
                )
            }
            if (totals != null && totals.totalExpense > 0.0) {
                DayAmountPill(
                    prefix = "-",
                    amount = totals.totalExpense,
                    tint = ObsidianColors.Danger
                )
            }
        }
    }
}

@Composable
private fun DayAmountPill(
    prefix: String,
    amount: Double,
    tint: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(tint.copy(alpha = 0.16f))
            .padding(horizontal = 3.dp, vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(4.dp)
                .clip(CircleShape)
                .background(tint)
        )
        Text(
            text = "$prefix${FinanceMath.formatPhpCurrency(amount, compact = true)}",
            style = MonoStyle.copy(fontSize = 8.5.sp, lineHeight = 10.sp),
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DayDetailSheetContent(
    dateStr: String,
    dayIncome: List<Income>,
    dayExpenses: List<Expense>,
    onClose: () -> Unit,
    onAddExpense: () -> Unit,
    onAddIncome: () -> Unit,
    onDeleteIncome: (Income) -> Unit,
    onDeleteExpense: (Expense) -> Unit
) {
    val totalIncome = remember(dayIncome) { dayIncome.sumOf { it.amount } }
    val totalExpense = remember(dayExpenses) { dayExpenses.sumOf { it.amount } }
    val net = totalIncome - totalExpense

    val titleDate = remember(dateStr) {
        FinanceMath.formatDateDisplay(dateStr, "MMM d, yyyy").ifEmpty { dateStr }
    }
    val weekdayText = remember(dateStr) {
        FinanceMath.formatDateDisplay(dateStr, "EEEE")
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titleDate,
                    style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
                    color = ObsidianColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (weekdayText.isNotEmpty()) {
                    Text(
                        text = weekdayText,
                        style = MaterialTheme.typography.bodySmall,
                        color = ObsidianColors.TextSecondary,
                        maxLines = 1
                    )
                }
            }
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Close",
                    tint = ObsidianColors.TextSecondary
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(ObsidianColors.JadeSurface)
                .border(1.dp, ObsidianColors.Hairline, RoundedCornerShape(14.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SummaryMetricRow(
                label = "Income",
                value = "+${FinanceMath.formatPhpCurrency(totalIncome)}",
                color = ObsidianColors.EmeraldPrimary
            )
            SummaryMetricRow(
                label = "Expenses",
                value = "-${FinanceMath.formatPhpCurrency(totalExpense)}",
                color = ObsidianColors.Danger
            )
            HorizontalDivider(color = ObsidianColors.Hairline)
            SummaryMetricRow(
                label = "Net",
                value = "${if (net >= 0.0) "+" else ""}${FinanceMath.formatPhpCurrency(net)}",
                color = if (net >= 0.0) ObsidianColors.EmeraldBright else ObsidianColors.Danger,
                bold = true
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickActionButton(
                label = "Add Expense",
                tint = ObsidianColors.Danger,
                onClick = onAddExpense,
                modifier = Modifier.weight(1f)
            )
            QuickActionButton(
                label = "Add Income",
                tint = ObsidianColors.EmeraldPrimary,
                onClick = onAddIncome,
                modifier = Modifier.weight(1f)
            )
        }

        Text(
            text = "Transactions",
            style = MaterialTheme.typography.titleSmall,
            color = ObsidianColors.TextSecondary
        )

        if (dayIncome.isEmpty() && dayExpenses.isEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(ObsidianColors.JadeSurface)
                    .border(1.dp, ObsidianColors.Hairline, RoundedCornerShape(14.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.CalendarToday,
                    contentDescription = null,
                    tint = ObsidianColors.TextMuted,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "No transactions on this date.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ObsidianColors.TextSecondary
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 60.dp)
                    .height(260.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = dayIncome,
                    key = { "inc-${it.id}" }
                ) { inc ->
                    DayTransactionRow(
                        title = inc.description?.takeIf { it.isNotBlank() } ?: "Income",
                        subtitle = inc.wallets?.name,
                        emoji = null,
                        isIncome = true,
                        isProjected = inc.isProjected,
                        amountText = "+${FinanceMath.formatPhpCurrency(inc.amount)}",
                        onDelete = if (inc.isProjected) null else ({ onDeleteIncome(inc) })
                    )
                }

                items(
                    items = dayExpenses,
                    key = { "exp-${it.id}" }
                ) { exp ->
                    val subtitle = listOfNotNull(
                        exp.categories?.name,
                        exp.wallets?.name
                    ).joinToString(" • ").takeIf { it.isNotBlank() }

                    DayTransactionRow(
                        title = exp.description?.takeIf { it.isNotBlank() }
                            ?: exp.categories?.name
                            ?: "Expense",
                        subtitle = subtitle,
                        emoji = if (exp.isProjected) null else exp.categories?.icon,
                        isIncome = false,
                        isProjected = exp.isProjected,
                        amountText = "-${FinanceMath.formatPhpCurrency(exp.amount)}",
                        onDelete = if (exp.isProjected) null else ({ onDeleteExpense(exp) })
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryMetricRow(
    label: String,
    value: String,
    color: Color,
    bold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (bold) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            color = if (bold) ObsidianColors.TextPrimary else ObsidianColors.TextSecondary
        )
        Text(
            text = value,
            style = MonoStyle.copy(
                fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold
            ),
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DayTransactionRow(
    title: String,
    subtitle: String?,
    emoji: String?,
    isIncome: Boolean,
    isProjected: Boolean,
    amountText: String,
    onDelete: (() -> Unit)?
) {
    val accent = if (isIncome) ObsidianColors.EmeraldPrimary else ObsidianColors.Danger

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ObsidianColors.JadeSurface)
            .border(
                width = 1.dp,
                color = if (isProjected) ObsidianColors.ElectricViolet.copy(alpha = 0.35f) else ObsidianColors.Hairline,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (isProjected) {
                        ObsidianColors.ElectricViolet.copy(alpha = 0.16f)
                    } else {
                        accent.copy(alpha = 0.14f)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            when {
                isProjected -> Icon(
                    imageVector = Icons.Rounded.Repeat,
                    contentDescription = null,
                    tint = ObsidianColors.ElectricViolet,
                    modifier = Modifier.size(18.dp)
                )
                !emoji.isNullOrBlank() -> Text(
                    text = emoji,
                    fontSize = 16.sp
                )
                isIncome -> Icon(
                    imageVector = Icons.Rounded.TrendingUp,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
                else -> Icon(
                    imageVector = Icons.Rounded.ReceiptLong,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = ObsidianColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = ObsidianColors.TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
                if (isProjected) {
                    Surface(
                        color = ObsidianColors.ElectricViolet.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Repeat,
                                contentDescription = null,
                                tint = ObsidianColors.ElectricViolet,
                                modifier = Modifier.size(10.dp)
                            )
                            Text(
                                text = "Recurring",
                                style = MaterialTheme.typography.labelSmall,
                                color = ObsidianColors.ElectricViolet,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = amountText,
            style = MonoStyle.copy(fontWeight = FontWeight.SemiBold),
            color = accent,
            maxLines = 1
        )

        if (onDelete != null) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Delete $title",
                    tint = ObsidianColors.Danger,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}

private fun buildMonthGridWeeks(year: Int, month: Int): List<List<LocalDate>> {
    val ym = YearMonth.of(year, month.coerceIn(1, 12))
    val monthStart = ym.atDay(1)
    val monthEnd = ym.atEndOfMonth()
    val startOffset = monthStart.dayOfWeek.value % 7
    val gridStart = monthStart.minusDays(startOffset.toLong())
    val endOffset = 6 - (monthEnd.dayOfWeek.value % 7)
    val gridEnd = monthEnd.plusDays(endOffset.toLong())

    val days = mutableListOf<LocalDate>()
    var cursor = gridStart
    while (!cursor.isAfter(gridEnd)) {
        days.add(cursor)
        cursor = cursor.plusDays(1)
    }
    return days.chunked(7)
}
