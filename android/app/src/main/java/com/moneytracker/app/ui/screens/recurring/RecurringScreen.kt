package com.moneytracker.app.ui.screens.recurring

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moneytracker.app.data.model.RecurringTransaction
import com.moneytracker.app.domain.FinanceMath
import com.moneytracker.app.ui.components.CategoryIcon
import com.moneytracker.app.ui.components.ConfirmDeleteDialog
import com.moneytracker.app.ui.components.EmptyStateCard
import com.moneytracker.app.ui.components.KpiStatTile
import com.moneytracker.app.ui.components.ObsidianSurfaceCard
import com.moneytracker.app.ui.components.RecurringFormSheet
import com.moneytracker.app.ui.components.parseHexColor
import com.moneytracker.app.ui.theme.MonoStyle
import com.moneytracker.app.ui.theme.ObsidianColors
import com.moneytracker.app.viewmodel.RecurringViewModel

@Composable
fun RecurringScreen(
    viewModel: RecurringViewModel,
    onSnackbar: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var showFormSheet by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<RecurringTransaction?>(null) }
    var pendingDelete by remember { mutableStateOf<RecurringTransaction?>(null) }

    LaunchedEffect(state.message, state.error) {
        val msg = state.message ?: state.error
        if (msg != null) {
            onSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianColors.ObsidianCanvas)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiStatTile(
                        label = "Monthly Income",
                        value = FinanceMath.formatPhpCurrency(state.summary.monthlyIncome),
                        valueColor = ObsidianColors.EmeraldPrimary,
                        badge = if (state.summary.hasInterval) "Est. 30d" else null,
                        icon = Icons.Rounded.TrendingUp,
                        iconTint = ObsidianColors.EmeraldPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    KpiStatTile(
                        label = "Monthly Expenses",
                        value = FinanceMath.formatPhpCurrency(state.summary.monthlyExpenses),
                        valueColor = ObsidianColors.Danger,
                        badge = if (state.summary.hasInterval) "Est. 30d" else null,
                        icon = Icons.Rounded.ReceiptLong,
                        iconTint = ObsidianColors.ElectricViolet,
                        modifier = Modifier.weight(1f)
                    )
                }

                KpiStatTile(
                    label = "Active Rules",
                    value = "${state.summary.activeCount} / ${state.summary.totalCount}",
                    valueColor = ObsidianColors.TextPrimary,
                    badge = state.summary.nextDueDate?.let {
                        "Next ${FinanceMath.formatDateDisplay(it, "MMM d")}"
                    },
                    icon = Icons.Rounded.Repeat,
                    iconTint = ObsidianColors.EmeraldPrimary,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (!state.isLoading && state.items.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Rounded.Repeat,
                    title = "No recurring rules",
                    subtitle = "Set up monthly or interval rules for salaries, rent, and subscriptions.",
                    actionLabel = "Add Rule",
                    onAction = {
                        editingItem = null
                        showFormSheet = true
                    }
                )
            }
        } else {
            items(
                items = state.items,
                key = { it.id }
            ) { item ->
                RecurringRuleCard(
                    item = item,
                    onToggleActive = { next -> viewModel.toggleActive(item.id, next) },
                    onEdit = {
                        editingItem = item
                        showFormSheet = true
                    },
                    onDelete = { pendingDelete = item }
                )
            }
        }
    }

        FloatingActionButton(
            onClick = {
                editingItem = null
                showFormSheet = true
            },
            containerColor = ObsidianColors.EmeraldPrimary,
            contentColor = ObsidianColors.TextOnAccent,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "Add recurring rule"
            )
        }
    }

    if (showFormSheet) {
        RecurringFormSheet(
            editingRecurring = editingItem,
            wallets = state.wallets,
            categories = state.categories,
            paymentMethods = state.paymentMethods,
            onDismiss = {
                showFormSheet = false
                editingItem = null
            },
            onSave = { type, amount, description, walletId, categoryId, paymentMethodId, frequencyType, dayOfMonth, startDate, intervalDays, isActive ->
                viewModel.saveRecurring(
                    editingId = editingItem?.id,
                    type = type,
                    amount = amount,
                    description = description,
                    walletId = walletId,
                    categoryId = categoryId,
                    paymentMethodId = paymentMethodId,
                    frequencyType = frequencyType,
                    dayOfMonth = dayOfMonth,
                    startDate = startDate,
                    intervalDays = intervalDays,
                    isActive = isActive
                ) { success ->
                    if (success) {
                        showFormSheet = false
                        editingItem = null
                    }
                }
            }
        )
    }

    pendingDelete?.let { target ->
        val label = target.description?.takeIf { it.isNotBlank() }
            ?: target.categories?.name
            ?: "recurring rule"
        ConfirmDeleteDialog(
            title = "Delete Recurring Rule",
            message = "Remove \"$label\" (${FinanceMath.formatPhpCurrency(target.amount)})?",
            onConfirm = {
                viewModel.deleteRecurring(target.id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        )
    }
}

@Composable
private fun RecurringRuleCard(
    item: RecurringTransaction,
    onToggleActive: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isIncome = item.type.equals("income", ignoreCase = true)
    val isInterval = item.frequencyType.equals("interval", ignoreCase = true)
    val intervalDays = (item.intervalDays ?: 30).coerceAtLeast(1)
    val accentColor = if (isIncome) ObsidianColors.EmeraldPrimary else ObsidianColors.Danger

    ObsidianSurfaceCard(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (item.isActive) 1f else 0.72f),
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = item.description?.takeIf { it.isNotBlank() }
                        ?: item.categories?.name
                        ?: "Recurring Rule",
                    style = MaterialTheme.typography.titleMedium,
                    color = ObsidianColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = ObsidianColors.JadeElevated,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(parseHexColor(item.wallets?.color))
                            )
                            Text(
                                text = item.wallets?.name ?: "Wallet",
                                style = MaterialTheme.typography.labelSmall,
                                color = ObsidianColors.TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Surface(
                        color = accentColor.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (isIncome) "Income" else "Expense",
                            style = MaterialTheme.typography.labelSmall,
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            maxLines = 1
                        )
                    }

                    item.categories?.let { cat ->
                        Surface(
                            color = ObsidianColors.JadeElevated,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                CategoryIcon(
                                    categoryName = cat.name,
                                    storedIcon = cat.icon,
                                    tint = ObsidianColors.TextSecondary,
                                    size = 13.dp
                                )
                                Text(
                                    text = cat.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ObsidianColors.TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = "${if (isIncome) "+" else "-"}${FinanceMath.formatPhpCurrency(item.amount)}",
                style = MonoStyle.copy(fontWeight = FontWeight.Bold),
                color = accentColor,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(ObsidianColors.JadeElevated)
                .border(1.dp, ObsidianColors.Hairline, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Repeat,
                    contentDescription = null,
                    tint = ObsidianColors.ElectricViolet,
                    modifier = Modifier.size(15.dp)
                )
                val scheduleText = if (isInterval) {
                    val unit = if (intervalDays == 1) "day" else "days"
                    val startLabel = item.startDate?.let {
                        FinanceMath.formatDateDisplay(it, "MMM d, yyyy").takeIf { s -> s.isNotEmpty() }
                    }
                    if (startLabel != null) {
                        "Every $intervalDays $unit • from $startLabel"
                    } else {
                        "Every $intervalDays $unit"
                    }
                } else {
                    "Every month on the ${FinanceMath.getOrdinalDay(item.dayOfMonth ?: 1)}"
                }
                Text(
                    text = scheduleText,
                    style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                    color = ObsidianColors.TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Schedule,
                    contentDescription = null,
                    tint = ObsidianColors.TextMuted,
                    modifier = Modifier.size(14.dp)
                )
                val lastPostedLabel = item.lastPostedDate?.let {
                    FinanceMath.formatDateDisplay(it, "MMM d, yyyy").takeIf { s -> s.isNotEmpty() }
                } ?: "Never"
                Text(
                    text = "Last posted: $lastPostedLabel",
                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                    color = ObsidianColors.TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Switch(
                    checked = item.isActive,
                    onCheckedChange = onToggleActive,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ObsidianColors.TextOnAccent,
                        checkedTrackColor = ObsidianColors.EmeraldPrimary,
                        uncheckedThumbColor = ObsidianColors.TextSecondary,
                        uncheckedTrackColor = ObsidianColors.JadeMuted
                    )
                )
                Text(
                    text = if (item.isActive) "Active" else "Paused",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (item.isActive) ObsidianColors.EmeraldPrimary else ObsidianColors.TextMuted,
                    maxLines = 1
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Edit recurring rule",
                        tint = ObsidianColors.TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Delete recurring rule",
                        tint = ObsidianColors.Danger,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
