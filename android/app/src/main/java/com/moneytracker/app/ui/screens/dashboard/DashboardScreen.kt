package com.moneytracker.app.ui.screens.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moneytracker.app.data.model.Category
import com.moneytracker.app.data.model.DashboardTimeframe
import com.moneytracker.app.data.model.PaceDeltaTone
import com.moneytracker.app.data.model.RecentTxItem
import com.moneytracker.app.domain.FinanceMath
import com.moneytracker.app.ui.components.CategoryAveragesList
import com.moneytracker.app.ui.components.CategoryDonutChart
import com.moneytracker.app.ui.components.ExpenseFormSheet
import com.moneytracker.app.ui.components.IncomeFormSheet
import com.moneytracker.app.ui.components.KpiStatTile
import com.moneytracker.app.ui.components.MonthSelectorPill
import com.moneytracker.app.ui.components.ObsidianSegmentedPill
import com.moneytracker.app.ui.components.ObsidianSurfaceCard
import com.moneytracker.app.ui.components.SpendingPaceSparkline
import com.moneytracker.app.ui.components.TrendBarChart
import com.moneytracker.app.ui.components.WalletCardStack
import com.moneytracker.app.ui.components.WalletFilterChips
import com.moneytracker.app.ui.theme.MonoStyle
import com.moneytracker.app.ui.theme.ObsidianColors
import com.moneytracker.app.viewmodel.DashboardChartTab
import com.moneytracker.app.viewmodel.DashboardViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToRecurring: () -> Unit,
    onNavigateToExpenses: () -> Unit,
    onSnackbar: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var showCategoryFilterSheet by remember { mutableStateOf(false) }
    var quickAddType by remember { mutableStateOf<String?>(null) }
    var quickAddWalletId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (state.summary == null && !state.isLoading) {
            viewModel.refresh()
        }
    }

    LaunchedEffect(state.message, state.error) {
        val msg = state.message ?: state.error
        if (!msg.isNullOrBlank()) {
            onSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    val summary = state.summary
    val visibleWallets = remember(summary) {
        summary?.walletsData?.map { it.wallet }.orEmpty()
    }

    val totalIncome = summary?.totalIncome ?: 0.0
    val totalExpenses = summary?.totalExpenses ?: 0.0
    val allIncome = summary?.allTotalIncome ?: 0.0

    val incomeBadge = remember(totalIncome, totalExpenses, allIncome, state.selectedWalletId) {
        if (state.selectedWalletId != "all" && allIncome > 0.0) {
            "${((totalIncome / allIncome) * 100.0).roundToInt()}% share"
        } else if (totalIncome > 0.0) {
            val savedPct = (((totalIncome - totalExpenses) / totalIncome) * 100.0).roundToInt()
            if (savedPct >= 0) "+$savedPct% net" else "$savedPct% net"
        } else {
            null
        }
    }

    val expenseBadge = remember(totalIncome, totalExpenses) {
        if (totalIncome > 0.0) {
            "${((totalExpenses / totalIncome) * 100.0).roundToInt()}% outflow"
        } else {
            null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Title + Privacy Toggle + Refresh
            item(key = "header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "My Dashboard",
                        style = MaterialTheme.typography.headlineLarge,
                        color = ObsidianColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PrivacyTogglePill(
                            isPrivacyMode = state.isPrivacyMode,
                            onToggle = { viewModel.togglePrivacyMode() }
                        )
                        IconButton(
                            onClick = { viewModel.refresh() },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Refresh dashboard",
                                tint = ObsidianColors.TextSecondary
                            )
                        }
                    }
                }
            }

            if (state.isLoading) {
                item(key = "loading") {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(999.dp)),
                        color = ObsidianColors.EmeraldPrimary,
                        trackColor = ObsidianColors.JadeElevated
                    )
                }
            }

            // Period & Filter Controls
            item(key = "controls") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    MonthSelectorPill(
                        month = state.selectedMonth,
                        year = state.selectedYear,
                        hideMonth = state.timeframe == DashboardTimeframe.ANNUAL,
                        onChange = { m, y -> viewModel.setMonthYear(y, m) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ObsidianSegmentedPill(
                            options = listOf(
                                DashboardTimeframe.MONTHLY to "Monthly",
                                DashboardTimeframe.ANNUAL to "Annual"
                            ),
                            selected = state.timeframe,
                            onSelect = { viewModel.setTimeframe(it) },
                            modifier = Modifier.weight(1f),
                            containerColor = ObsidianColors.JadeElevated
                        )

                        CategoryExclusionTrigger(
                            excludedCount = state.excludedCategoryIds.size,
                            onClick = { showCategoryFilterSheet = true }
                        )
                    }

                    if (visibleWallets.isNotEmpty()) {
                        WalletFilterChips(
                            wallets = visibleWallets,
                            selectedWalletId = state.selectedWalletId.takeUnless { it == "all" },
                            onSelect = { id -> viewModel.setSelectedWallet(id ?: "all") }
                        )
                    }
                }
            }

            // Signature 3D Wallet Card Stack
            item(key = "wallet_stack") {
                WalletCardStack(
                    walletsData = summary?.walletsData.orEmpty(),
                    selectedWalletId = state.selectedWalletId,
                    onSelectWallet = { viewModel.setSelectedWallet(it) },
                    isPrivacyMode = state.isPrivacyMode,
                    onQuickAdd = { type, walletId ->
                        quickAddWalletId = walletId
                        quickAddType = type
                    },
                    onManageWallets = onNavigateToSettings
                )
            }

            // Stacked Income & Expense KPI Tiles
            item(key = "kpi_row") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiStatTile(
                        label = "Income",
                        value = FinanceMath.formatPhpCurrency(
                            amount = totalIncome,
                            compact = totalIncome >= 1_000_000.0,
                            masked = state.isPrivacyMode
                        ),
                        valueColor = ObsidianColors.EmeraldPrimary,
                        badge = incomeBadge,
                        icon = Icons.Rounded.ArrowDownward,
                        iconTint = ObsidianColors.EmeraldPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    KpiStatTile(
                        label = "Expenses",
                        value = FinanceMath.formatPhpCurrency(
                            amount = totalExpenses,
                            compact = totalExpenses >= 1_000_000.0,
                            masked = state.isPrivacyMode
                        ),
                        valueColor = ObsidianColors.Danger,
                        badge = expenseBadge,
                        icon = Icons.Rounded.ArrowUpward,
                        iconTint = ObsidianColors.Danger,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Primary Analytics Chart Card (Flow / Categories / Averages)
            item(key = "chart_card") {
                ObsidianSurfaceCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 22.dp,
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (state.activeChartTab) {
                                DashboardChartTab.TREND -> "Cashflow"
                                DashboardChartTab.CATEGORIES -> "Categories"
                                DashboardChartTab.AVERAGES -> "Monthly Averages"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            color = ObsidianColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (state.excludedCategoryIds.isNotEmpty() && state.activeChartTab == DashboardChartTab.CATEGORIES) {
                            Text(
                                text = "${state.excludedCategoryIds.size} hidden",
                                style = MonoStyle.copy(fontSize = 11.sp),
                                color = ObsidianColors.ElectricViolet,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    ObsidianSegmentedPill(
                        options = listOf(
                            DashboardChartTab.TREND to "Flow",
                            DashboardChartTab.CATEGORIES to "Categories",
                            DashboardChartTab.AVERAGES to "Averages"
                        ),
                        selected = state.activeChartTab,
                        onSelect = { viewModel.setActiveChartTab(it) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    when (state.activeChartTab) {
                        DashboardChartTab.TREND -> TrendBarChart(
                            trendData = summary?.trendData.orEmpty(),
                            isPrivacyMode = state.isPrivacyMode
                        )
                        DashboardChartTab.CATEGORIES -> CategoryDonutChart(
                            categories = summary?.filteredCategorySpends.orEmpty(),
                            isPrivacyMode = state.isPrivacyMode
                        )
                        DashboardChartTab.AVERAGES -> CategoryAveragesList(
                            averages = summary?.averages.orEmpty(),
                            isPrivacyMode = state.isPrivacyMode
                        )
                    }
                }
            }

            // 2-Column Mini Insight Cards: Recurring + Spending Pace
            item(key = "insights_row") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Recurring Mini Card
                    val rec = state.recurringSummary
                    ObsidianSurfaceCard(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(role = Role.Button, onClick = onNavigateToRecurring),
                        cornerRadius = 20.dp,
                        contentPadding = PaddingValues(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ObsidianColors.ElectricViolet.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Repeat,
                                    contentDescription = null,
                                    tint = ObsidianColors.ElectricViolet,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = "${rec?.activeCount ?: 0} active",
                                style = MonoStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                                color = ObsidianColors.ElectricViolet,
                                maxLines = 1
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Recurring",
                            style = MaterialTheme.typography.titleSmall,
                            color = ObsidianColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = FinanceMath.formatPhpCurrency(
                                amount = rec?.monthlyExpenses ?: 0.0,
                                compact = true,
                                masked = state.isPrivacyMode
                            ),
                            style = MonoStyle.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold),
                            color = ObsidianColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val nextLabel = rec?.nextItem?.description?.takeIf { it.isNotBlank() }
                            ?: rec?.nextItem?.categories?.name
                            ?: "No upcoming"
                        val nextDate = FinanceMath.formatDateDisplay(rec?.nextDueDate, "MMM dd")
                        Text(
                            text = if (nextDate.isNotBlank()) "$nextDate · $nextLabel" else nextLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = ObsidianColors.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Spending Pace Mini Card
                    val pace = state.paceSummary
                    val deltaColor = when (pace?.deltaTone) {
                        PaceDeltaTone.UP -> ObsidianColors.Danger
                        PaceDeltaTone.DOWN -> ObsidianColors.EmeraldPrimary
                        else -> ObsidianColors.TextSecondary
                    }
                    ObsidianSurfaceCard(
                        modifier = Modifier.weight(1f),
                        cornerRadius = 20.dp,
                        contentPadding = PaddingValues(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ObsidianColors.EmeraldPrimary.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.TrendingUp,
                                    contentDescription = null,
                                    tint = ObsidianColors.EmeraldPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(deltaColor.copy(alpha = 0.14f))
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = pace?.deltaText ?: "Steady",
                                    style = MonoStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                                    color = deltaColor,
                                    maxLines = 1
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Spending Pace",
                            style = MaterialTheme.typography.titleSmall,
                            color = ObsidianColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = FinanceMath.formatPhpCurrency(
                                amount = pace?.latest ?: 0.0,
                                compact = true,
                                masked = state.isPrivacyMode
                            ),
                            style = MonoStyle.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold),
                            color = ObsidianColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        SpendingPaceSparkline(
                            trendData = summary?.trendData.orEmpty(),
                            modifier = Modifier.height(40.dp)
                        )
                    }
                }
            }

            // Recent Transactions Card (up to 5 items)
            item(key = "recent_tx") {
                ObsidianSurfaceCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 22.dp,
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Transactions",
                            style = MaterialTheme.typography.titleLarge,
                            color = ObsidianColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = onNavigateToExpenses,
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text(
                                text = "View all",
                                style = MaterialTheme.typography.labelLarge,
                                color = ObsidianColors.EmeraldPrimary
                            )
                            Icon(
                                imageVector = Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = ObsidianColors.EmeraldPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    if (state.recentTransactions.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 22.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No transactions recorded this month",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ObsidianColors.TextMuted
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.height(4.dp))
                        state.recentTransactions.take(5).forEachIndexed { idx, item ->
                            RecentTxRow(
                                item = item,
                                isPrivacyMode = state.isPrivacyMode
                            )
                            if (idx < state.recentTransactions.take(5).lastIndex) {
                                HorizontalDivider(
                                    color = ObsidianColors.Hairline,
                                    thickness = 1.dp,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    // Category Exclusion Filter Bottom Sheet
    if (showCategoryFilterSheet) {
        CategoryExclusionSheet(
            categories = state.categories,
            excludedIds = state.excludedCategoryIds,
            onToggle = { viewModel.toggleExcludedCategory(it) },
            onClearAll = { viewModel.clearExcludedCategories() },
            onDismiss = { showCategoryFilterSheet = false }
        )
    }

    // Quick-Add Expense Sheet
    if (quickAddType == "expense") {
        ExpenseFormSheet(
            editingExpense = null,
            wallets = state.wallets,
            categories = state.categories,
            paymentMethods = state.paymentMethods,
            defaultWalletId = quickAddWalletId,
            defaultDate = null,
            onDismiss = { quickAddType = null },
            onSave = { walletId, categoryId, paymentMethodId, amount, description, expenseDate, installmentMonths ->
                viewModel.quickAddExpense(
                    walletId = walletId,
                    categoryId = categoryId,
                    paymentMethodId = paymentMethodId,
                    amount = amount,
                    description = description,
                    expenseDate = expenseDate,
                    installmentMonths = installmentMonths
                ) { ok ->
                    if (ok) quickAddType = null
                }
            }
        )
    }

    // Quick-Add Income Sheet
    if (quickAddType == "income") {
        IncomeFormSheet(
            editingIncome = null,
            wallets = state.wallets,
            defaultWalletId = quickAddWalletId,
            defaultDate = null,
            onDismiss = { quickAddType = null },
            onSave = { walletId, amount, description, incomeDate ->
                viewModel.quickAddIncome(
                    walletId = walletId,
                    amount = amount,
                    description = description,
                    incomeDate = incomeDate
                ) { ok ->
                    if (ok) quickAddType = null
                }
            }
        )
    }
}

@Composable
private fun PrivacyTogglePill(
    isPrivacyMode: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(999.dp))
            .clickable(role = Role.Switch, onClick = onToggle),
        shape = RoundedCornerShape(999.dp),
        color = if (isPrivacyMode) ObsidianColors.JadeElevated else ObsidianColors.EmeraldPrimary.copy(alpha = 0.16f),
        border = BorderStroke(
            1.dp,
            if (isPrivacyMode) ObsidianColors.HairlineHover else ObsidianColors.EmeraldPrimary.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = if (isPrivacyMode) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                contentDescription = if (isPrivacyMode) "Show balances" else "Hide balances",
                tint = if (isPrivacyMode) ObsidianColors.TextSecondary else ObsidianColors.EmeraldPrimary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = if (isPrivacyMode) "Private" else "Visible",
                style = MaterialTheme.typography.labelLarge,
                color = if (isPrivacyMode) ObsidianColors.TextPrimary else ObsidianColors.EmeraldPrimary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun CategoryExclusionTrigger(
    excludedCount: Int,
    onClick: () -> Unit
) {
    val active = excludedCount > 0
    Surface(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(999.dp))
            .clickable(role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = if (active) ObsidianColors.ElectricViolet.copy(alpha = 0.18f) else ObsidianColors.JadeElevated,
        border = BorderStroke(
            1.dp,
            if (active) ObsidianColors.ElectricViolet.copy(alpha = 0.45f) else ObsidianColors.Hairline
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.FilterList,
                contentDescription = "Filter categories",
                tint = if (active) ObsidianColors.ElectricViolet else ObsidianColors.TextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = if (active) "Categories ($excludedCount)" else "Categories",
                style = MaterialTheme.typography.labelLarge,
                color = if (active) ObsidianColors.TextPrimary else ObsidianColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RecentTxRow(
    item: RecentTxItem,
    isPrivacyMode: Boolean
) {
    val isIncome = item.type.equals("income", ignoreCase = true)
    val amountColor = if (isIncome) ObsidianColors.EmeraldPrimary else ObsidianColors.Danger
    val prefix = if (isPrivacyMode) "" else if (isIncome) "+" else "-"
    val dateLabel = FinanceMath.formatDateDisplay(item.dateStr, "MMM dd")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isIncome) ObsidianColors.EmeraldPrimary.copy(alpha = 0.14f)
                    else ObsidianColors.JadeElevated
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isIncome) {
                Icon(
                    imageVector = Icons.Rounded.ArrowDownward,
                    contentDescription = null,
                    tint = ObsidianColors.EmeraldPrimary,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Text(
                    text = item.categoryIcon?.takeIf { it.isNotBlank() } ?: "🧾",
                    fontSize = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = ObsidianColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val meta = listOfNotNull(
                item.walletName.takeIf { it.isNotBlank() },
                dateLabel.takeIf { it.isNotBlank() }
            ).joinToString(" · ")
            if (meta.isNotBlank()) {
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = ObsidianColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = "$prefix${FinanceMath.formatPhpCurrency(item.amount, masked = isPrivacyMode)}",
            style = MonoStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
            color = amountColor,
            maxLines = 1
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryExclusionSheet(
    categories: List<Category>,
    excludedIds: Set<String>,
    onToggle: (String) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ObsidianColors.JadeSurface,
        contentColor = ObsidianColors.TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Exclude Categories",
                    style = MaterialTheme.typography.titleLarge,
                    color = ObsidianColors.TextPrimary
                )
                if (excludedIds.isNotEmpty()) {
                    TextButton(
                        onClick = onClearAll,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text(
                            text = "Clear All",
                            color = ObsidianColors.EmeraldPrimary,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (categories.isEmpty()) {
                Text(
                    text = "No categories available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ObsidianColors.TextMuted,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(categories.size) { index ->
                        val cat = categories[index]
                        val excluded = excludedIds.contains(cat.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (excluded) ObsidianColors.ElectricViolet.copy(alpha = 0.12f)
                                    else Color.Transparent
                                )
                                .clickable { onToggle(cat.id) }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = excluded,
                                onCheckedChange = { onToggle(cat.id) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = ObsidianColors.ElectricViolet,
                                    uncheckedColor = ObsidianColors.TextMuted,
                                    checkmarkColor = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = cat.icon,
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = cat.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (excluded) ObsidianColors.TextPrimary else ObsidianColors.TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}
