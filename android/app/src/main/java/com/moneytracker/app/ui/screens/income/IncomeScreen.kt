package com.moneytracker.app.ui.screens.income

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import com.moneytracker.app.data.model.Income
import com.moneytracker.app.data.model.TransactionSortOrder
import com.moneytracker.app.data.model.Wallet
import com.moneytracker.app.domain.FinanceMath
import com.moneytracker.app.ui.components.ConfirmDeleteDialog
import com.moneytracker.app.ui.components.EmeraldPrimaryButton
import com.moneytracker.app.ui.components.EmptyStateCard
import com.moneytracker.app.ui.components.IncomeFormSheet
import com.moneytracker.app.ui.components.KpiStatTile
import com.moneytracker.app.ui.components.MonthSelectorPill
import com.moneytracker.app.ui.components.ObsidianSurfaceCard
import com.moneytracker.app.ui.components.WalletFilterChips
import com.moneytracker.app.ui.components.parseHexColor
import com.moneytracker.app.ui.theme.MonoStyle
import com.moneytracker.app.ui.theme.ObsidianColors
import com.moneytracker.app.viewmodel.IncomeViewModel

private fun TransactionSortOrder.incomeDisplayLabel(): String = when (this) {
    TransactionSortOrder.DATE_DESC -> "Newest"
    TransactionSortOrder.DATE_ASC -> "Oldest"
    TransactionSortOrder.AMOUNT_DESC -> "Highest"
    TransactionSortOrder.AMOUNT_ASC -> "Lowest"
}

private fun TransactionSortOrder.incomeMenuLabel(): String = when (this) {
    TransactionSortOrder.DATE_DESC -> "Date (Newest First)"
    TransactionSortOrder.DATE_ASC -> "Date (Oldest First)"
    TransactionSortOrder.AMOUNT_DESC -> "Amount (Highest First)"
    TransactionSortOrder.AMOUNT_ASC -> "Amount (Lowest First)"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeScreen(
    viewModel: IncomeViewModel,
    onSnackbar: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var isFormOpen by remember { mutableStateOf(false) }
    var editingIncome by remember { mutableStateOf<Income?>(null) }
    var pendingDeleteIncome by remember { mutableStateOf<Income?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.message, uiState.error) {
        val msg = uiState.error ?: uiState.message
        if (msg != null) {
            onSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    val walletMap = remember(uiState.wallets) { uiState.wallets.associateBy { it.id } }

    Box(modifier = Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = uiState.isLoading && uiState.rawIncome.isNotEmpty(),
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
                        label = "Filtered Income",
                        value = FinanceMath.formatPhpCurrency(uiState.totalAmount),
                        valueColor = ObsidianColors.EmeraldPrimary,
                        icon = Icons.Rounded.TrendingUp,
                        iconTint = ObsidianColors.EmeraldPrimary,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KpiStatTile(
                            label = "Deposits",
                            value = uiState.filteredIncome.size.toString(),
                            valueColor = ObsidianColors.TextPrimary,
                            badge = if (uiState.hasActiveFilters &&
                                uiState.rawIncome.size != uiState.filteredIncome.size
                            ) {
                                "of ${uiState.rawIncome.size}"
                            } else {
                                null
                            },
                            icon = Icons.Rounded.Layers,
                            iconTint = ObsidianColors.ElectricViolet,
                            modifier = Modifier.weight(1f)
                        )
                        KpiStatTile(
                            label = "Avg Deposit",
                            value = FinanceMath.formatPhpCurrency(
                                amount = uiState.averageAmount,
                                compact = uiState.averageAmount >= 1_000_000.0
                            ),
                            valueColor = ObsidianColors.TextPrimary,
                            icon = Icons.Rounded.AccountBalanceWallet,
                            iconTint = ObsidianColors.EmeraldBright,
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

            // Search + Sort + Reset
            item(key = "search_and_sort") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = {
                            Text(
                                text = "Search income description or wallet...",
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
                                            text = "Sort: ${uiState.sortOrder.incomeDisplayLabel()}",
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
                                                    text = order.incomeMenuLabel(),
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
            if (uiState.isLoading && uiState.rawIncome.isEmpty()) {
                items(4, key = { "skeleton-$it" }) {
                    IncomeSkeletonCard()
                }
            } else if (uiState.filteredIncome.isEmpty()) {
                item(key = "empty") {
                    EmptyStateCard(
                        icon = Icons.Rounded.TrendingUp,
                        title = "No income found",
                        subtitle = if (uiState.hasActiveFilters) {
                            "No income records match your active search or wallet filter."
                        } else {
                            "No income recorded for this month yet."
                        },
                        actionLabel = if (uiState.hasActiveFilters) "Clear filters" else "Add income",
                        onAction = {
                            if (uiState.hasActiveFilters) {
                                viewModel.resetFilters()
                            } else {
                                editingIncome = null
                                isFormOpen = true
                            }
                        }
                    )
                }
            } else {
                items(uiState.filteredIncome, key = { it.id }) { inc ->
                    val wallet = walletMap[inc.walletId]
                    IncomeLedgerCard(
                        income = inc,
                        wallet = wallet,
                        onEdit = {
                            editingIncome = inc
                            isFormOpen = true
                        },
                        onDelete = { pendingDeleteIncome = inc }
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
                            val count = uiState.filteredIncome.size
                            Text(
                                text = "Total ($count ${if (count == 1) "deposit" else "deposits"})",
                                style = MaterialTheme.typography.titleSmall,
                                color = ObsidianColors.TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "+${FinanceMath.formatPhpCurrency(uiState.totalAmount)}",
                                style = MonoStyle.copy(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = ObsidianColors.EmeraldPrimary,
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
                editingIncome = null
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
                contentDescription = "Add income"
            )
        }
    }

    pendingDeleteIncome?.let { target ->
        val label = target.description?.takeIf { it.isNotBlank() } ?: "Income"
        ConfirmDeleteDialog(
            title = "Delete Income Record",
            message = "Remove \"$label\" (${FinanceMath.formatPhpCurrency(target.amount)})? This action cannot be undone.",
            onConfirm = {
                viewModel.deleteIncome(target.id)
                pendingDeleteIncome = null
            },
            onDismiss = { pendingDeleteIncome = null }
        )
    }

    if (isFormOpen) {
        IncomeFormSheet(
            editingIncome = editingIncome,
            wallets = uiState.wallets,
            defaultWalletId = uiState.selectedWalletId,
            onDismiss = {
                isFormOpen = false
                editingIncome = null
            },
            onSave = { walletId, amount, description, incomeDate ->
                val currentEditing = editingIncome
                if (currentEditing != null) {
                    viewModel.updateIncome(
                        id = currentEditing.id,
                        walletId = walletId,
                        amount = amount,
                        description = description,
                        incomeDate = incomeDate
                    ) { success ->
                        if (success) {
                            isFormOpen = false
                            editingIncome = null
                        }
                    }
                } else {
                    viewModel.addIncome(
                        walletId = walletId,
                        amount = amount,
                        description = description,
                        incomeDate = incomeDate
                    ) { success ->
                        if (success) {
                            isFormOpen = false
                            editingIncome = null
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun IncomeLedgerCard(
    income: Income,
    wallet: Wallet?,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val walletName = wallet?.name ?: income.wallets?.name ?: "Unknown"
    val walletColor = parseHexColor(wallet?.color ?: income.wallets?.color)
    val titleText = income.description?.takeIf { it.isNotBlank() } ?: "Income"

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

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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

                    Text(
                        text = FinanceMath.formatDateDisplay(income.incomeDate, "MMM dd, yyyy")
                            .ifBlank { income.incomeDate },
                        style = MonoStyle.copy(fontSize = 12.sp),
                        color = ObsidianColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = "+${FinanceMath.formatPhpCurrency(income.amount)}",
                style = MonoStyle.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = ObsidianColors.EmeraldPrimary,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "Edit income",
                    tint = ObsidianColors.TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Delete income",
                    tint = ObsidianColors.Danger,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun IncomeSkeletonCard() {
    ObsidianSurfaceCard(
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(16.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(ObsidianColors.JadeMuted)
            )
            Box(
                modifier = Modifier
                    .width(76.dp)
                    .height(16.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(ObsidianColors.JadeMuted)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.65f)
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(ObsidianColors.JadeMuted.copy(alpha = 0.7f))
        )
    }
}
