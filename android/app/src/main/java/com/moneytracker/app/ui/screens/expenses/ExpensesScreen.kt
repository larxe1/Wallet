package com.moneytracker.app.ui.screens.expenses

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moneytracker.app.data.model.Category
import com.moneytracker.app.data.model.Expense
import com.moneytracker.app.data.model.PaymentMethod
import com.moneytracker.app.data.model.TransactionSortOrder
import com.moneytracker.app.data.model.Wallet
import com.moneytracker.app.domain.FinanceMath
import com.moneytracker.app.ui.components.CategoryIcon
import com.moneytracker.app.ui.components.ConfirmDeleteDialog
import com.moneytracker.app.ui.components.EmeraldPrimaryButton
import com.moneytracker.app.ui.components.EmptyStateCard
import com.moneytracker.app.ui.components.ExpenseFormSheet
import com.moneytracker.app.ui.components.KpiStatTile
import com.moneytracker.app.ui.components.MonthSelectorPill
import com.moneytracker.app.ui.components.ObsidianSurfaceCard
import com.moneytracker.app.ui.components.WalletFilterChips
import com.moneytracker.app.ui.components.parseHexColor
import com.moneytracker.app.ui.theme.MonoStyle
import com.moneytracker.app.ui.theme.ObsidianColors
import com.moneytracker.app.viewmodel.ExpensesViewModel

private fun TransactionSortOrder.displayLabel(): String = when (this) {
    TransactionSortOrder.DATE_DESC -> "Newest"
    TransactionSortOrder.DATE_ASC -> "Oldest"
    TransactionSortOrder.AMOUNT_DESC -> "Highest"
    TransactionSortOrder.AMOUNT_ASC -> "Lowest"
}

private fun TransactionSortOrder.menuLabel(): String = when (this) {
    TransactionSortOrder.DATE_DESC -> "Date (Newest First)"
    TransactionSortOrder.DATE_ASC -> "Date (Oldest First)"
    TransactionSortOrder.AMOUNT_DESC -> "Amount (Highest First)"
    TransactionSortOrder.AMOUNT_ASC -> "Amount (Lowest First)"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    viewModel: ExpensesViewModel,
    onSnackbar: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var isFormOpen by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<Expense?>(null) }
    var pendingDeleteExpense by remember { mutableStateOf<Expense?>(null) }

    var showCategoryFilterSheet by remember { mutableStateOf(false) }
    var showPaymentFilterSheet by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.message, uiState.error) {
        val msg = uiState.error ?: uiState.message
        if (msg != null) {
            onSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    val walletMap = remember(uiState.wallets) { uiState.wallets.associateBy { it.id } }
    val categoryMap = remember(uiState.categories) { uiState.categories.associateBy { it.id } }
    val paymentMethodMap = remember(uiState.paymentMethods) { uiState.paymentMethods.associateBy { it.id } }

    Box(modifier = Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = uiState.isLoading && uiState.rawExpenses.isNotEmpty(),
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Month Selector
                item(key = "header") {
                    MonthSelectorPill(
                        month = uiState.selectedMonth,
                        year = uiState.selectedYear,
                        onChange = { month, year -> viewModel.setMonthYear(year, month) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

            // 3 KPI Tiles (1 full-width primary + 2-column secondary for clean phone legibility)
            item(key = "kpis") {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiStatTile(
                        label = "Filtered Total",
                        value = FinanceMath.formatPhpCurrency(uiState.totalAmount),
                        valueColor = ObsidianColors.Danger,
                        icon = Icons.Rounded.Receipt,
                        iconTint = ObsidianColors.Danger,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KpiStatTile(
                            label = "Entry Count",
                            value = uiState.filteredExpenses.size.toString(),
                            valueColor = ObsidianColors.TextPrimary,
                            badge = if (uiState.hasActiveFilters &&
                                uiState.rawExpenses.size != uiState.filteredExpenses.size
                            ) {
                                "of ${uiState.rawExpenses.size}"
                            } else {
                                null
                            },
                            icon = Icons.Rounded.Layers,
                            iconTint = ObsidianColors.EmeraldPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        KpiStatTile(
                            label = "Avg Spend",
                            value = FinanceMath.formatPhpCurrency(
                                amount = uiState.averageAmount,
                                compact = uiState.averageAmount >= 1_000_000.0
                            ),
                            valueColor = ObsidianColors.TextPrimary,
                            icon = Icons.Rounded.TrendingDown,
                            iconTint = ObsidianColors.ElectricViolet,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Wallet Filter Chips
            item(key = "wallet_filter") {
                WalletFilterChips(
                    wallets = uiState.wallets,
                    selectedWalletId = uiState.selectedWalletId,
                    onSelect = { viewModel.setWalletFilter(it) }
                )
            }

            // Search + Multi-select Category & Payment Method + Sort + Reset
            item(key = "search_and_filters") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = {
                            Text(
                                text = "Search description, category, wallet...",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null,
                                tint = ObsidianColors.TextSecondary
                            )
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { viewModel.setSearchQuery("") },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Clear search",
                                        tint = ObsidianColors.TextSecondary
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ObsidianColors.JadeSurface,
                            unfocusedContainerColor = ObsidianColors.JadeSurface,
                            focusedBorderColor = ObsidianColors.EmeraldPrimary,
                            unfocusedBorderColor = ObsidianColors.Hairline,
                            focusedTextColor = ObsidianColors.TextPrimary,
                            unfocusedTextColor = ObsidianColors.TextPrimary,
                            cursorColor = ObsidianColors.EmeraldPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item(key = "cat_chip") {
                            val catCount = uiState.selectedCategoryIds.size
                            FilterChip(
                                selected = catCount > 0,
                                onClick = { showCategoryFilterSheet = true },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.FilterList,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = if (catCount > 0) "Categories ($catCount)" else "Categories",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = ObsidianColors.JadeSurface,
                                    labelColor = ObsidianColors.TextSecondary,
                                    iconColor = ObsidianColors.TextSecondary,
                                    selectedContainerColor = ObsidianColors.EmeraldPrimary.copy(alpha = 0.16f),
                                    selectedLabelColor = ObsidianColors.EmeraldBright,
                                    selectedLeadingIconColor = ObsidianColors.EmeraldBright
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = catCount > 0,
                                    borderColor = ObsidianColors.Hairline,
                                    selectedBorderColor = ObsidianColors.EmeraldPrimary
                                ),
                                modifier = Modifier.heightIn(min = 48.dp)
                            )
                        }

                        item(key = "pm_chip") {
                            val pmCount = uiState.selectedPaymentMethodIds.size
                            FilterChip(
                                selected = pmCount > 0,
                                onClick = { showPaymentFilterSheet = true },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.FilterList,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = if (pmCount > 0) "Methods ($pmCount)" else "Payment Methods",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = ObsidianColors.JadeSurface,
                                    labelColor = ObsidianColors.TextSecondary,
                                    iconColor = ObsidianColors.TextSecondary,
                                    selectedContainerColor = ObsidianColors.ElectricViolet.copy(alpha = 0.18f),
                                    selectedLabelColor = ObsidianColors.TextPrimary,
                                    selectedLeadingIconColor = ObsidianColors.ElectricViolet
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = pmCount > 0,
                                    borderColor = ObsidianColors.Hairline,
                                    selectedBorderColor = ObsidianColors.ElectricViolet
                                ),
                                modifier = Modifier.heightIn(min = 48.dp)
                            )
                        }

                        item(key = "sort_chip") {
                            Box {
                                FilterChip(
                                    selected = uiState.sortOrder != TransactionSortOrder.DATE_DESC,
                                    onClick = { showSortMenu = true },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.SwapVert,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = "Sort: ${uiState.sortOrder.displayLabel()}",
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = ObsidianColors.JadeSurface,
                                        labelColor = ObsidianColors.TextSecondary,
                                        iconColor = ObsidianColors.TextSecondary,
                                        selectedContainerColor = ObsidianColors.JadeElevated,
                                        selectedLabelColor = ObsidianColors.TextPrimary,
                                        selectedLeadingIconColor = ObsidianColors.EmeraldPrimary
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = uiState.sortOrder != TransactionSortOrder.DATE_DESC,
                                        borderColor = ObsidianColors.Hairline,
                                        selectedBorderColor = ObsidianColors.HairlineHover
                                    ),
                                    modifier = Modifier.heightIn(min = 48.dp)
                                )

                                DropdownMenu(
                                    expanded = showSortMenu,
                                    onDismissRequest = { showSortMenu = false },
                                    containerColor = ObsidianColors.JadeElevated
                                ) {
                                    TransactionSortOrder.entries.forEach { order ->
                                        val isCurrent = order == uiState.sortOrder
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = order.menuLabel(),
                                                    color = if (isCurrent) {
                                                        ObsidianColors.EmeraldBright
                                                    } else {
                                                        ObsidianColors.TextPrimary
                                                    },
                                                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal
                                                )
                                            },
                                            onClick = {
                                                viewModel.setSortOrder(order)
                                                showSortMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        if (uiState.hasActiveFilters) {
                            item(key = "reset_chip") {
                                FilterChip(
                                    selected = false,
                                    onClick = { viewModel.resetFilters() },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.RestartAlt,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = "Reset",
                                            maxLines = 1
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = ObsidianColors.Danger.copy(alpha = 0.12f),
                                        labelColor = ObsidianColors.Danger,
                                        iconColor = ObsidianColors.Danger
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = false,
                                        borderColor = ObsidianColors.Danger.copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier.heightIn(min = 48.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Loading / Empty / List States
            if (uiState.isLoading && uiState.rawExpenses.isEmpty()) {
                items(5, key = { "skeleton-$it" }) {
                    ExpenseSkeletonCard()
                }
            } else if (uiState.filteredExpenses.isEmpty()) {
                item(key = "empty") {
                    EmptyStateCard(
                        icon = Icons.Rounded.Receipt,
                        title = "No expenses found",
                        subtitle = if (uiState.hasActiveFilters) {
                            "No entries match your active search or filter criteria."
                        } else {
                            "No expenses recorded for this month yet."
                        },
                        actionLabel = if (uiState.hasActiveFilters) "Clear filters" else "Add expense",
                        onAction = {
                            if (uiState.hasActiveFilters) {
                                viewModel.resetFilters()
                            } else {
                                editingExpense = null
                                isFormOpen = true
                            }
                        }
                    )
                }
            } else {
                items(uiState.filteredExpenses, key = { it.id }) { expense ->
                    val category = categoryMap[expense.categoryId]
                    val wallet = walletMap[expense.walletId]
                    val method = paymentMethodMap[expense.paymentMethodId]

                    ExpenseLedgerCard(
                        expense = expense,
                        category = category,
                        wallet = wallet,
                        paymentMethod = method,
                        onEdit = {
                            editingExpense = expense
                            isFormOpen = true
                        },
                        onDelete = { pendingDeleteExpense = expense }
                    )
                }

                item(key = "footer_total") {
                    ObsidianSurfaceCard(
                        containerColor = ObsidianColors.JadeElevated,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val count = uiState.filteredExpenses.size
                            Text(
                                text = "Total ($count ${if (count == 1) "entry" else "entries"})",
                                style = MaterialTheme.typography.titleSmall,
                                color = ObsidianColors.TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = FinanceMath.formatPhpCurrency(uiState.totalAmount),
                                style = MonoStyle.copy(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = ObsidianColors.Danger,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }

        FloatingActionButton(
            onClick = {
                editingExpense = null
                isFormOpen = true
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
                contentDescription = "Add expense"
            )
        }
    }

    if (showCategoryFilterSheet) {
        MultiSelectFilterSheet(
            title = "Filter Categories",
            items = uiState.categories.map { c ->
                FilterOption(
                    id = c.id,
                    label = c.name,
                    tag = null,
                    categoryName = c.name,
                    categoryIcon = c.icon
                )
            },
            selectedIds = uiState.selectedCategoryIds,
            onToggle = { id -> viewModel.toggleCategoryFilter(id) },
            onClearAll = {
                uiState.selectedCategoryIds.toList().forEach { viewModel.toggleCategoryFilter(it) }
            },
            onDismiss = { showCategoryFilterSheet = false }
        )
    }

    if (showPaymentFilterSheet) {
        MultiSelectFilterSheet(
            title = "Filter Payment Methods",
            items = uiState.paymentMethods.map { pm ->
                FilterOption(id = pm.id, label = pm.label, tag = pm.type.uppercase())
            },
            selectedIds = uiState.selectedPaymentMethodIds,
            onToggle = { id -> viewModel.togglePaymentMethodFilter(id) },
            onClearAll = {
                uiState.selectedPaymentMethodIds.toList().forEach { viewModel.togglePaymentMethodFilter(it) }
            },
            onDismiss = { showPaymentFilterSheet = false }
        )
    }

    pendingDeleteExpense?.let { target ->
        val catName = categoryMap[target.categoryId]?.name ?: target.categories?.name
        val label = target.description?.takeIf { it.isNotBlank() } ?: catName ?: "this expense"
        ConfirmDeleteDialog(
            title = "Delete Expense",
            message = "Remove \"$label\" (${FinanceMath.formatPhpCurrency(target.amount)})? This action cannot be undone.",
            onConfirm = {
                viewModel.deleteExpense(target.id)
                pendingDeleteExpense = null
            },
            onDismiss = { pendingDeleteExpense = null }
        )
    }

    if (isFormOpen) {
        ExpenseFormSheet(
            editingExpense = editingExpense,
            wallets = uiState.wallets,
            categories = uiState.categories,
            paymentMethods = uiState.paymentMethods,
            defaultWalletId = uiState.selectedWalletId,
            onDismiss = {
                isFormOpen = false
                editingExpense = null
            },
            onSave = { walletId, categoryId, paymentMethodId, amount, description, expenseDate, installmentMonths ->
                val currentEditing = editingExpense
                if (currentEditing != null) {
                    viewModel.updateExpense(
                        id = currentEditing.id,
                        walletId = walletId,
                        categoryId = categoryId,
                        paymentMethodId = paymentMethodId,
                        amount = amount,
                        description = description,
                        expenseDate = expenseDate
                    ) { success ->
                        if (success) {
                            isFormOpen = false
                            editingExpense = null
                        }
                    }
                } else {
                    viewModel.addExpense(
                        walletId = walletId,
                        categoryId = categoryId,
                        paymentMethodId = paymentMethodId,
                        amount = amount,
                        description = description,
                        expenseDate = expenseDate,
                        installmentMonths = installmentMonths
                    ) { success ->
                        if (success) {
                            isFormOpen = false
                            editingExpense = null
                        }
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExpenseLedgerCard(
    expense: Expense,
    category: Category?,
    wallet: Wallet?,
    paymentMethod: PaymentMethod?,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val catIcon = category?.icon ?: expense.categories?.icon ?: "tag"
    val catName = category?.name ?: expense.categories?.name ?: "Uncategorized"
    val walletName = wallet?.name ?: expense.wallets?.name ?: "Unknown"
    val walletColor = parseHexColor(wallet?.color ?: expense.wallets?.color)
    val pmType = paymentMethod?.type ?: expense.paymentMethods?.type
    val pmLabel = paymentMethod?.label ?: expense.paymentMethods?.label
    val titleText = expense.description?.takeIf { it.isNotBlank() } ?: catName

    ObsidianSurfaceCard(
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
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
                    text = titleText,
                    style = MaterialTheme.typography.titleMedium,
                    color = ObsidianColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Category pill
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = ObsidianColors.JadeMuted,
                        border = BorderStroke(1.dp, ObsidianColors.Hairline)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            CategoryIcon(
                                categoryName = catName,
                                storedIcon = catIcon,
                                tint = ObsidianColors.TextSecondary,
                                size = 13.dp
                            )
                            Text(
                                text = catName,
                                style = MaterialTheme.typography.labelSmall,
                                color = ObsidianColors.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Wallet pill with color dot
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = ObsidianColors.JadeMuted,
                        border = BorderStroke(1.dp, ObsidianColors.Hairline)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(walletColor)
                            )
                            Text(
                                text = walletName,
                                style = MaterialTheme.typography.labelSmall,
                                color = ObsidianColors.TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Payment method tag
                    if (!pmLabel.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = ObsidianColors.ElectricViolet.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, ObsidianColors.ElectricViolet.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (!pmType.isNullOrBlank()) {
                                    Text(
                                        text = pmType.uppercase(),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = ObsidianColors.ElectricViolet,
                                        maxLines = 1
                                    )
                                }
                                Text(
                                    text = pmLabel,
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
                text = FinanceMath.formatPhpCurrency(expense.amount),
                style = MonoStyle.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = ObsidianColors.TextPrimary,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = FinanceMath.formatDateDisplay(expense.expenseDate, "MMM dd, yyyy")
                    .ifBlank { expense.expenseDate },
                style = MonoStyle.copy(fontSize = 12.sp),
                color = ObsidianColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Edit expense",
                        tint = ObsidianColors.TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Delete expense",
                        tint = ObsidianColors.Danger,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpenseSkeletonCard() {
    ObsidianSurfaceCard(
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .height(16.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(ObsidianColors.JadeMuted)
            )
            Box(
                modifier = Modifier
                    .width(72.dp)
                    .height(16.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(ObsidianColors.JadeMuted)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(ObsidianColors.JadeMuted.copy(alpha = 0.7f))
        )
    }
}

private data class FilterOption(
    val id: String,
    val label: String,
    val tag: String?,
    val categoryName: String? = null,
    val categoryIcon: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MultiSelectFilterSheet(
    title: String,
    items: List<FilterOption>,
    selectedIds: Set<String>,
    onToggle: (String) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ObsidianColors.JadeElevated,
        contentColor = ObsidianColors.TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = ObsidianColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (selectedIds.isNotEmpty()) {
                    TextButton(
                        onClick = onClearAll,
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                    ) {
                        Text("Clear", color = ObsidianColors.Danger)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (items.isEmpty()) {
                Text(
                    text = "No options available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ObsidianColors.TextMuted,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(items, key = { it.id }) { option ->
                        val checked = selectedIds.contains(option.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onToggle(option.id) }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { onToggle(option.id) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = ObsidianColors.EmeraldPrimary,
                                    checkmarkColor = ObsidianColors.TextOnAccent,
                                    uncheckedColor = ObsidianColors.TextSecondary
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            if (option.categoryName != null || option.categoryIcon != null) {
                                CategoryIcon(
                                    categoryName = option.categoryName,
                                    storedIcon = option.categoryIcon,
                                    tint = ObsidianColors.TextSecondary,
                                    size = 18.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = ObsidianColors.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            if (!option.tag.isNullOrBlank()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = option.tag,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ObsidianColors.ElectricViolet,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            EmeraldPrimaryButton(
                text = "Done",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
