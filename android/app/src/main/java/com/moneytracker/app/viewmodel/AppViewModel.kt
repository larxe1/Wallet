package com.moneytracker.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.moneytracker.app.data.local.SessionStore
import com.moneytracker.app.data.local.WidgetSnapshotStore
import com.moneytracker.app.data.model.AuthSession
import com.moneytracker.app.data.model.Category
import com.moneytracker.app.data.model.DashboardSummary
import com.moneytracker.app.data.model.DashboardTimeframe
import com.moneytracker.app.data.model.Expense
import com.moneytracker.app.data.model.Income
import com.moneytracker.app.data.model.PaceSummary
import com.moneytracker.app.data.model.PaymentMethod
import com.moneytracker.app.data.model.RecentTxItem
import com.moneytracker.app.data.model.RecurringSummary
import com.moneytracker.app.data.model.RecurringTransaction
import com.moneytracker.app.data.model.TransactionSortOrder
import com.moneytracker.app.data.model.Wallet
import com.moneytracker.app.data.remote.SupabaseClient
import com.moneytracker.app.data.repository.MoneyRepository
import com.moneytracker.app.domain.FinanceMath
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

/**
 * Simple manual dependency injection container owned by MoneyTrackerApp.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val sessionStore: SessionStore = SessionStore(appContext)
    val widgetSnapshotStore: WidgetSnapshotStore = WidgetSnapshotStore(appContext)
    val supabaseClient: SupabaseClient = SupabaseClient(sessionStore)
    val repository: MoneyRepository = MoneyRepository(
        context = appContext,
        sessionStore = sessionStore,
        widgetSnapshotStore = widgetSnapshotStore,
        supabase = supabaseClient
    )

    val viewModelFactory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when {
                modelClass.isAssignableFrom(AuthViewModel::class.java) -> AuthViewModel(repository) as T
                modelClass.isAssignableFrom(DashboardViewModel::class.java) -> DashboardViewModel(repository) as T
                modelClass.isAssignableFrom(ExpensesViewModel::class.java) -> ExpensesViewModel(repository) as T
                modelClass.isAssignableFrom(IncomeViewModel::class.java) -> IncomeViewModel(repository) as T
                modelClass.isAssignableFrom(CalendarViewModel::class.java) -> CalendarViewModel(repository) as T
                modelClass.isAssignableFrom(RecurringViewModel::class.java) -> RecurringViewModel(repository) as T
                modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(repository) as T
                modelClass.isAssignableFrom(QuickLogViewModel::class.java) -> QuickLogViewModel(repository) as T
                else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }
}

// =====================================================================
// 1. AuthViewModel
// =====================================================================

data class AuthUiState(
    val session: AuthSession? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val infoMessage: String? = null,
    val isSupabaseConfigured: Boolean = true
) {
    val isAuthenticated: Boolean get() = session != null
}

class AuthViewModel(private val repository: MoneyRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AuthUiState(
            session = repository.sessionStore.getSession(),
            isSupabaseConfigured = repository.supabase.isConfigured
        )
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.sessionFlow.collect { session ->
                _uiState.update { it.copy(session = session) }
                if (session != null) {
                    repository.runAutoPostIfNeeded()
                }
            }
        }
    }

    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(error = "Please enter both email and password.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, infoMessage = null) }
            runCatching { repository.signIn(email, password) }
                .onSuccess { session ->
                    _uiState.update { it.copy(session = session, isLoading = false, error = null) }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(isLoading = false, error = err.message ?: "Sign in failed")
                    }
                }
        }
    }

    fun signUp(email: String, password: String) {
        if (email.isBlank() || password.length < 6) {
            _uiState.update { it.copy(error = "Enter a valid email and password (min 6 characters).") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, infoMessage = null) }
            runCatching { repository.signUp(email, password) }
                .onSuccess { res ->
                    _uiState.update {
                        it.copy(
                            session = res.session ?: it.session,
                            isLoading = false,
                            error = null,
                            infoMessage = if (res.requiresEmailConfirmation) {
                                "Account created! Check your email to confirm, then sign in."
                            } else {
                                null
                            }
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(isLoading = false, error = err.message ?: "Sign up failed")
                    }
                }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { repository.signOut() }
            _uiState.update { it.copy(session = null, isLoading = false, error = null, infoMessage = null) }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(error = null, infoMessage = null) }
    }
}

// =====================================================================
// 2. DashboardViewModel
// =====================================================================

enum class DashboardChartTab {
    TREND,
    CATEGORIES,
    AVERAGES
}

data class DashboardUiState(
    val selectedYear: Int = LocalDate.now().year,
    val selectedMonth: Int = LocalDate.now().monthValue,
    val timeframe: DashboardTimeframe = DashboardTimeframe.MONTHLY,
    val isPrivacyMode: Boolean = true,
    val selectedWalletId: String = "all",
    val excludedCategoryIds: Set<String> = emptySet(),
    val activeChartTab: DashboardChartTab = DashboardChartTab.TREND,
    val summary: DashboardSummary? = null,
    val recentTransactions: List<RecentTxItem> = emptyList(),
    val recurringSummary: RecurringSummary? = null,
    val paceSummary: PaceSummary? = null,
    val wallets: List<Wallet> = emptyList(),
    val categories: List<Category> = emptyList(),
    val paymentMethods: List<PaymentMethod> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null
)

class DashboardViewModel(private val repository: MoneyRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var cachedExpenses: List<Expense> = emptyList()
    private var cachedIncome: List<Income> = emptyList()
    private var cachedRecurring: List<RecurringTransaction> = emptyList()
    private var cachedAllExpenses: List<Expense> = emptyList()
    private var cachedAllIncome: List<Income> = emptyList()

    init {
        val (safeYear, safeMonth) = FinanceMath.clampToMonthFloor(
            LocalDate.now().year,
            LocalDate.now().monthValue
        )
        _uiState.update { it.copy(selectedYear = safeYear, selectedMonth = safeMonth) }

        viewModelScope.launch {
            repository.dataVersion.collect {
                if (repository.sessionStore.getSession() != null) {
                    refresh()
                }
            }
        }
    }

    fun refresh() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching {
                repository.fetchDashboardBundle(
                    year = state.selectedYear,
                    month = state.selectedMonth,
                    timeframe = state.timeframe,
                    selectedWalletId = state.selectedWalletId,
                    excludedCategoryIds = state.excludedCategoryIds
                )
            }.onSuccess { bundle ->
                cachedExpenses = bundle.monthExpenses
                cachedIncome = bundle.monthIncome
                cachedRecurring = bundle.recurring
                val userId = repository.sessionStore.getSession()?.user?.id
                // Re-derive recent transactions, recurring summary, and pace summary
                val visibleIds = bundle.summary.walletsData.map { it.wallet.id }.toSet()
                val effWallet = if (bundle.summary.walletsData.any { it.wallet.id == state.selectedWalletId }) {
                    state.selectedWalletId
                } else {
                    "all"
                }

                val recent = buildRecentTransactions(
                    expenses = bundle.monthExpenses,
                    income = bundle.monthIncome,
                    effectiveWalletId = effWallet,
                    visibleWalletIds = visibleIds
                )
                val recSummary = FinanceMath.computeRecurringSummary(
                    items = bundle.recurring,
                    walletIdFilter = effWallet,
                    visibleWalletIds = visibleIds
                )
                val pace = FinanceMath.computePaceDelta(bundle.summary.trendData)

                _uiState.update {
                    it.copy(
                        summary = bundle.summary,
                        recentTransactions = recent,
                        recurringSummary = recSummary,
                        paceSummary = pace,
                        wallets = bundle.wallets,
                        categories = bundle.categories,
                        paymentMethods = bundle.paymentMethods,
                        selectedWalletId = effWallet,
                        isLoading = false,
                        error = null
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(isLoading = false, error = err.message ?: "Unable to load dashboard")
                }
            }
        }
    }

    fun setMonthYear(year: Int, month: Int) {
        val (clampedY, clampedM) = FinanceMath.clampToMonthFloor(year, month)
        if (clampedY == _uiState.value.selectedYear && clampedM == _uiState.value.selectedMonth) return
        _uiState.update { it.copy(selectedYear = clampedY, selectedMonth = clampedM) }
        refresh()
    }

    fun setTimeframe(timeframe: DashboardTimeframe) {
        if (timeframe == _uiState.value.timeframe) return
        _uiState.update { it.copy(timeframe = timeframe) }
        refresh()
    }

    fun togglePrivacyMode() {
        _uiState.update { it.copy(isPrivacyMode = !it.isPrivacyMode) }
    }

    fun setSelectedWallet(walletId: String) {
        if (walletId == _uiState.value.selectedWalletId) return
        _uiState.update { it.copy(selectedWalletId = walletId) }
        refresh()
    }

    fun setActiveChartTab(tab: DashboardChartTab) {
        _uiState.update { it.copy(activeChartTab = tab) }
    }

    fun toggleExcludedCategory(categoryId: String) {
        _uiState.update { state ->
            val next = state.excludedCategoryIds.toMutableSet()
            if (!next.add(categoryId)) next.remove(categoryId)
            val updatedSummary = state.summary?.let { s ->
                s.copy(
                    filteredCategorySpends = s.rawCategorySpends.filterNot {
                        it.categoryId != null && next.contains(it.categoryId)
                    }
                )
            }
            state.copy(excludedCategoryIds = next, summary = updatedSummary)
        }
    }

    fun clearExcludedCategories() {
        _uiState.update { state ->
            val updatedSummary = state.summary?.let { s ->
                s.copy(filteredCategorySpends = s.rawCategorySpends)
            }
            state.copy(excludedCategoryIds = emptySet(), summary = updatedSummary)
        }
    }

    fun quickAddExpense(
        walletId: String,
        categoryId: String?,
        paymentMethodId: String?,
        amount: Double,
        description: String?,
        expenseDate: String,
        installmentMonths: Int = 1,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            runCatching {
                repository.addExpense(
                    walletId = walletId,
                    categoryId = categoryId,
                    paymentMethodId = paymentMethodId,
                    amount = amount,
                    description = description,
                    expenseDate = expenseDate,
                    installmentMonths = installmentMonths
                )
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        message = if (installmentMonths > 1) {
                            "Added $installmentMonths-month installment plan"
                        } else {
                            "Expense added"
                        }
                    )
                }
                onComplete(true)
            }.onFailure { err ->
                _uiState.update { it.copy(error = err.message ?: "Failed to add expense") }
                onComplete(false)
            }
        }
    }

    fun quickAddIncome(
        walletId: String,
        amount: Double,
        description: String?,
        incomeDate: String,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            runCatching {
                repository.addIncome(walletId, amount, description, incomeDate)
            }.onSuccess {
                _uiState.update { it.copy(message = "Income added") }
                onComplete(true)
            }.onFailure { err ->
                _uiState.update { it.copy(error = err.message ?: "Failed to add income") }
                onComplete(false)
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null, error = null) }
    }

    private fun buildRecentTransactions(
        expenses: List<Expense>,
        income: List<Income>,
        effectiveWalletId: String,
        visibleWalletIds: Set<String>
    ): List<RecentTxItem> {
        fun matchesWallet(wId: String): Boolean =
            if (effectiveWalletId == "all") {
                visibleWalletIds.isEmpty() || visibleWalletIds.contains(wId)
            } else {
                wId == effectiveWalletId
            }

        val expItems = expenses.filter { matchesWallet(it.walletId) }.map { e ->
            RecentTxItem(
                id = "exp-${e.id}",
                type = "expense",
                amount = e.amount,
                description = e.description?.takeIf { it.isNotBlank() } ?: e.categories?.name ?: "Expense",
                dateStr = e.expenseDate,
                createdAt = e.createdAt.orEmpty(),
                categoryIcon = e.categories?.icon,
                walletName = e.wallets?.name.orEmpty()
            )
        }

        val incItems = income.filter { matchesWallet(it.walletId) }.map { i ->
            RecentTxItem(
                id = "inc-${i.id}",
                type = "income",
                amount = i.amount,
                description = i.description?.takeIf { it.isNotBlank() } ?: "Income",
                dateStr = i.incomeDate,
                createdAt = i.createdAt.orEmpty(),
                categoryIcon = null,
                walletName = i.wallets?.name.orEmpty()
            )
        }

        return (expItems + incItems)
            .sortedWith(
                compareByDescending<RecentTxItem> { it.dateStr }
                    .thenByDescending { it.createdAt }
            )
            .take(5)
    }
}

// =====================================================================
// 3. ExpensesViewModel
// =====================================================================

data class ExpensesUiState(
    val selectedYear: Int = LocalDate.now().year,
    val selectedMonth: Int = LocalDate.now().monthValue,
    val selectedWalletId: String? = null,
    val selectedCategoryIds: Set<String> = emptySet(),
    val selectedPaymentMethodIds: Set<String> = emptySet(),
    val searchQuery: String = "",
    val sortOrder: TransactionSortOrder = TransactionSortOrder.DATE_DESC,
    val rawExpenses: List<Expense> = emptyList(),
    val filteredExpenses: List<Expense> = emptyList(),
    val wallets: List<Wallet> = emptyList(),
    val categories: List<Category> = emptyList(),
    val paymentMethods: List<PaymentMethod> = emptyList(),
    val totalAmount: Double = 0.0,
    val averageAmount: Double = 0.0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null
) {
    val hasActiveFilters: Boolean
        get() = selectedWalletId != null ||
            selectedCategoryIds.isNotEmpty() ||
            selectedPaymentMethodIds.isNotEmpty() ||
            searchQuery.isNotBlank()
}

class ExpensesViewModel(private val repository: MoneyRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpensesUiState())
    val uiState: StateFlow<ExpensesUiState> = _uiState.asStateFlow()

    init {
        val (safeYear, safeMonth) = FinanceMath.clampToMonthFloor(
            LocalDate.now().year,
            LocalDate.now().monthValue
        )
        _uiState.update { it.copy(selectedYear = safeYear, selectedMonth = safeMonth) }

        viewModelScope.launch {
            repository.dataVersion.collect {
                if (repository.sessionStore.getSession() != null) {
                    refresh()
                }
            }
        }
    }

    fun refresh() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching {
                coroutineScope {
                    val expDef = async { repository.getExpensesForMonth(state.selectedYear, state.selectedMonth) }
                    val walDef = async { repository.getWallets() }
                    val catDef = async { repository.getCategories() }
                    val pmDef = async { repository.getPaymentMethods() }
                    Tuple4(expDef.await(), walDef.await(), catDef.await(), pmDef.await())
                }
            }.onSuccess { (expenses, wallets, categories, pms) ->
                _uiState.update { current ->
                    recomputeFilters(
                        current.copy(
                            rawExpenses = expenses,
                            wallets = wallets,
                            categories = categories,
                            paymentMethods = pms,
                            isLoading = false,
                            error = null
                        )
                    )
                }
            }.onFailure { err ->
                _uiState.update { it.copy(isLoading = false, error = err.message ?: "Failed to load expenses") }
            }
        }
    }

    fun setMonthYear(year: Int, month: Int) {
        val (cy, cm) = FinanceMath.clampToMonthFloor(year, month)
        if (cy == _uiState.value.selectedYear && cm == _uiState.value.selectedMonth) return
        _uiState.update { it.copy(selectedYear = cy, selectedMonth = cm) }
        refresh()
    }

    fun setWalletFilter(walletId: String?) {
        _uiState.update { recomputeFilters(it.copy(selectedWalletId = walletId)) }
    }

    fun toggleCategoryFilter(categoryId: String) {
        _uiState.update { state ->
            val next = state.selectedCategoryIds.toMutableSet()
            if (!next.add(categoryId)) next.remove(categoryId)
            recomputeFilters(state.copy(selectedCategoryIds = next))
        }
    }

    fun togglePaymentMethodFilter(paymentMethodId: String) {
        _uiState.update { state ->
            val next = state.selectedPaymentMethodIds.toMutableSet()
            if (!next.add(paymentMethodId)) next.remove(paymentMethodId)
            recomputeFilters(state.copy(selectedPaymentMethodIds = next))
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { recomputeFilters(it.copy(searchQuery = query)) }
    }

    fun setSortOrder(order: TransactionSortOrder) {
        _uiState.update { recomputeFilters(it.copy(sortOrder = order)) }
    }

    fun resetFilters() {
        _uiState.update {
            recomputeFilters(
                it.copy(
                    selectedWalletId = null,
                    selectedCategoryIds = emptySet(),
                    selectedPaymentMethodIds = emptySet(),
                    searchQuery = ""
                )
            )
        }
    }

    fun addExpense(
        walletId: String,
        categoryId: String?,
        paymentMethodId: String?,
        amount: Double,
        description: String?,
        expenseDate: String,
        installmentMonths: Int = 1,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching {
                repository.addExpense(
                    walletId = walletId,
                    categoryId = categoryId,
                    paymentMethodId = paymentMethodId,
                    amount = amount,
                    description = description,
                    expenseDate = expenseDate,
                    installmentMonths = installmentMonths
                )
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        message = if (installmentMonths > 1) {
                            "Added $installmentMonths monthly installments"
                        } else {
                            "Expense added"
                        }
                    )
                }
                onComplete(true)
            }.onFailure { err ->
                _uiState.update { it.copy(isLoading = false, error = err.message ?: "Failed to add expense") }
                onComplete(false)
            }
        }
    }

    fun updateExpense(
        id: String,
        walletId: String,
        categoryId: String?,
        paymentMethodId: String?,
        amount: Double,
        description: String?,
        expenseDate: String,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching {
                repository.updateExpense(id, walletId, categoryId, paymentMethodId, amount, description, expenseDate)
            }.onSuccess {
                _uiState.update { it.copy(isLoading = false, message = "Expense updated") }
                onComplete(true)
            }.onFailure { err ->
                _uiState.update { it.copy(isLoading = false, error = err.message ?: "Failed to update expense") }
                onComplete(false)
            }
        }
    }

    fun deleteExpense(id: String) {
        viewModelScope.launch {
            runCatching { repository.deleteExpense(id) }
                .onSuccess { _uiState.update { it.copy(message = "Expense deleted") } }
                .onFailure { err -> _uiState.update { it.copy(error = err.message ?: "Failed to delete expense") } }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null, error = null) }
    }

    private fun recomputeFilters(state: ExpensesUiState): ExpensesUiState {
        val q = state.searchQuery.trim().lowercase()
        val catMap = state.categories.associateBy { it.id }
        val walMap = state.wallets.associateBy { it.id }
        val pmMap = state.paymentMethods.associateBy { it.id }

        val filtered = state.rawExpenses.filter { e ->
            val matchWallet = state.selectedWalletId == null || e.walletId == state.selectedWalletId
            val matchCat = state.selectedCategoryIds.isEmpty() ||
                (e.categoryId != null && state.selectedCategoryIds.contains(e.categoryId))
            val matchPm = state.selectedPaymentMethodIds.isEmpty() ||
                (e.paymentMethodId != null && state.selectedPaymentMethodIds.contains(e.paymentMethodId))
            if (!matchWallet || !matchCat || !matchPm) return@filter false

            if (q.isEmpty()) return@filter true
            val haystack = listOfNotNull(
                e.description,
                catMap[e.categoryId]?.name ?: e.categories?.name,
                walMap[e.walletId]?.name ?: e.wallets?.name,
                pmMap[e.paymentMethodId]?.label ?: e.paymentMethods?.label,
                e.amount.toString(),
                FinanceMath.formatPhpCurrency(e.amount)
            ).joinToString(" ").lowercase()
            haystack.contains(q)
        }

        val sorted = when (state.sortOrder) {
            TransactionSortOrder.DATE_DESC -> filtered.sortedByDescending { it.expenseDate }
            TransactionSortOrder.DATE_ASC -> filtered.sortedBy { it.expenseDate }
            TransactionSortOrder.AMOUNT_DESC -> filtered.sortedByDescending { it.amount }
            TransactionSortOrder.AMOUNT_ASC -> filtered.sortedBy { it.amount }
        }

        val total = sorted.sumOf { it.amount }
        val avg = if (sorted.isNotEmpty()) total / sorted.size else 0.0

        return state.copy(
            filteredExpenses = sorted,
            totalAmount = total,
            averageAmount = avg
        )
    }
}

// =====================================================================
// 4. IncomeViewModel
// =====================================================================

data class IncomeUiState(
    val selectedYear: Int = LocalDate.now().year,
    val selectedMonth: Int = LocalDate.now().monthValue,
    val selectedWalletId: String? = null,
    val searchQuery: String = "",
    val sortOrder: TransactionSortOrder = TransactionSortOrder.DATE_DESC,
    val rawIncome: List<Income> = emptyList(),
    val filteredIncome: List<Income> = emptyList(),
    val wallets: List<Wallet> = emptyList(),
    val totalAmount: Double = 0.0,
    val averageAmount: Double = 0.0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null
) {
    val hasActiveFilters: Boolean
        get() = selectedWalletId != null || searchQuery.isNotBlank()
}

class IncomeViewModel(private val repository: MoneyRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(IncomeUiState())
    val uiState: StateFlow<IncomeUiState> = _uiState.asStateFlow()

    init {
        val (safeYear, safeMonth) = FinanceMath.clampToMonthFloor(
            LocalDate.now().year,
            LocalDate.now().monthValue
        )
        _uiState.update { it.copy(selectedYear = safeYear, selectedMonth = safeMonth) }

        viewModelScope.launch {
            repository.dataVersion.collect {
                if (repository.sessionStore.getSession() != null) {
                    refresh()
                }
            }
        }
    }

    fun refresh() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching {
                coroutineScope {
                    val incDef = async { repository.getIncomeForMonth(state.selectedYear, state.selectedMonth) }
                    val walDef = async { repository.getWallets() }
                    incDef.await() to walDef.await()
                }
            }.onSuccess { (incomeList, wallets) ->
                _uiState.update { current ->
                    recomputeFilters(
                        current.copy(
                            rawIncome = incomeList,
                            wallets = wallets,
                            isLoading = false,
                            error = null
                        )
                    )
                }
            }.onFailure { err ->
                _uiState.update { it.copy(isLoading = false, error = err.message ?: "Failed to load income") }
            }
        }
    }

    fun setMonthYear(year: Int, month: Int) {
        val (cy, cm) = FinanceMath.clampToMonthFloor(year, month)
        if (cy == _uiState.value.selectedYear && cm == _uiState.value.selectedMonth) return
        _uiState.update { it.copy(selectedYear = cy, selectedMonth = cm) }
        refresh()
    }

    fun setWalletFilter(walletId: String?) {
        _uiState.update { recomputeFilters(it.copy(selectedWalletId = walletId)) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { recomputeFilters(it.copy(searchQuery = query)) }
    }

    fun setSortOrder(order: TransactionSortOrder) {
        _uiState.update { recomputeFilters(it.copy(sortOrder = order)) }
    }

    fun resetFilters() {
        _uiState.update { recomputeFilters(it.copy(selectedWalletId = null, searchQuery = "")) }
    }

    fun addIncome(
        walletId: String,
        amount: Double,
        description: String?,
        incomeDate: String,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { repository.addIncome(walletId, amount, description, incomeDate) }
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, message = "Income added") }
                    onComplete(true)
                }
                .onFailure { err ->
                    _uiState.update { it.copy(isLoading = false, error = err.message ?: "Failed to add income") }
                    onComplete(false)
                }
        }
    }

    fun updateIncome(
        id: String,
        walletId: String,
        amount: Double,
        description: String?,
        incomeDate: String,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { repository.updateIncome(id, walletId, amount, description, incomeDate) }
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, message = "Income updated") }
                    onComplete(true)
                }
                .onFailure { err ->
                    _uiState.update { it.copy(isLoading = false, error = err.message ?: "Failed to update income") }
                    onComplete(false)
                }
        }
    }

    fun deleteIncome(id: String) {
        viewModelScope.launch {
            runCatching { repository.deleteIncome(id) }
                .onSuccess { _uiState.update { it.copy(message = "Income deleted") } }
                .onFailure { err -> _uiState.update { it.copy(error = err.message ?: "Failed to delete income") } }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null, error = null) }
    }

    private fun recomputeFilters(state: IncomeUiState): IncomeUiState {
        val q = state.searchQuery.trim().lowercase()
        val walMap = state.wallets.associateBy { it.id }

        val filtered = state.rawIncome.filter { inc ->
            val matchWallet = state.selectedWalletId == null || inc.walletId == state.selectedWalletId
            if (!matchWallet) return@filter false
            if (q.isEmpty()) return@filter true
            val haystack = listOfNotNull(
                inc.description,
                walMap[inc.walletId]?.name ?: inc.wallets?.name,
                inc.amount.toString(),
                FinanceMath.formatPhpCurrency(inc.amount)
            ).joinToString(" ").lowercase()
            haystack.contains(q)
        }

        val sorted = when (state.sortOrder) {
            TransactionSortOrder.DATE_DESC -> filtered.sortedByDescending { it.incomeDate }
            TransactionSortOrder.DATE_ASC -> filtered.sortedBy { it.incomeDate }
            TransactionSortOrder.AMOUNT_DESC -> filtered.sortedByDescending { it.amount }
            TransactionSortOrder.AMOUNT_ASC -> filtered.sortedBy { it.amount }
        }

        val total = sorted.sumOf { it.amount }
        val avg = if (sorted.isNotEmpty()) total / sorted.size else 0.0

        return state.copy(
            filteredIncome = sorted,
            totalAmount = total,
            averageAmount = avg
        )
    }
}

// =====================================================================
// 5. CalendarViewModel
// =====================================================================

data class CalendarDayTotals(
    val dateStr: String,
    val totalIncome: Double,
    val totalExpense: Double,
    val hasProjected: Boolean
) {
    val net: Double get() = totalIncome - totalExpense
}

data class CalendarUiState(
    val selectedYear: Int = LocalDate.now().year,
    val selectedMonth: Int = LocalDate.now().monthValue,
    val selectedWalletId: String? = null,
    val selectedDate: String? = null,
    val rawExpenses: List<Expense> = emptyList(),
    val rawIncome: List<Income> = emptyList(),
    val recurring: List<RecurringTransaction> = emptyList(),
    val allExpenses: List<Expense> = emptyList(),
    val allIncome: List<Income> = emptyList(),
    val dayTotals: Map<String, CalendarDayTotals> = emptyMap(),
    val selectedDayExpenses: List<Expense> = emptyList(),
    val selectedDayIncome: List<Income> = emptyList(),
    val wallets: List<Wallet> = emptyList(),
    val categories: List<Category> = emptyList(),
    val paymentMethods: List<PaymentMethod> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null
)

class CalendarViewModel(private val repository: MoneyRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        val (safeYear, safeMonth) = FinanceMath.clampToMonthFloor(
            LocalDate.now().year,
            LocalDate.now().monthValue
        )
        _uiState.update { it.copy(selectedYear = safeYear, selectedMonth = safeMonth) }

        viewModelScope.launch {
            repository.dataVersion.collect {
                if (repository.sessionStore.getSession() != null) {
                    refresh()
                }
            }
        }
    }

    fun refresh() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching {
                coroutineScope {
                    val expDef = async { repository.getExpensesForMonth(state.selectedYear, state.selectedMonth) }
                    val incDef = async { repository.getIncomeForMonth(state.selectedYear, state.selectedMonth) }
                    val recDef = async { repository.getRecurring() }
                    val walDef = async { repository.getWallets() }
                    val catDef = async { repository.getCategories() }
                    val pmDef = async { repository.getPaymentMethods() }
                    Tuple6(
                        expDef.await(),
                        incDef.await(),
                        recDef.await(),
                        walDef.await(),
                        catDef.await(),
                        pmDef.await()
                    )
                }
            }.onSuccess { (expenses, income, recurring, wallets, categories, pms) ->
                _uiState.update { current ->
                    recomputeCalendar(
                        current.copy(
                            rawExpenses = expenses,
                            rawIncome = income,
                            recurring = recurring,
                            wallets = wallets,
                            categories = categories,
                            paymentMethods = pms,
                            isLoading = false,
                            error = null
                        )
                    )
                }
            }.onFailure { err ->
                _uiState.update { it.copy(isLoading = false, error = err.message ?: "Failed to load calendar") }
            }
        }
    }

    fun setMonthYear(year: Int, month: Int) {
        val (cy, cm) = FinanceMath.clampToMonthFloor(year, month)
        if (cy == _uiState.value.selectedYear && cm == _uiState.value.selectedMonth) return
        _uiState.update { it.copy(selectedYear = cy, selectedMonth = cm, selectedDate = null) }
        refresh()
    }

    fun setWalletFilter(walletId: String?) {
        _uiState.update { recomputeCalendar(it.copy(selectedWalletId = walletId)) }
    }

    fun selectDate(dateStr: String?) {
        _uiState.update { recomputeCalendar(it.copy(selectedDate = dateStr)) }
    }

    fun addExpense(
        walletId: String,
        categoryId: String?,
        paymentMethodId: String?,
        amount: Double,
        description: String?,
        expenseDate: String,
        installmentMonths: Int = 1,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            runCatching {
                repository.addExpense(walletId, categoryId, paymentMethodId, amount, description, expenseDate, installmentMonths)
            }.onSuccess {
                _uiState.update { it.copy(message = "Expense added") }
                onComplete(true)
            }.onFailure { err ->
                _uiState.update { it.copy(error = err.message ?: "Failed to add expense") }
                onComplete(false)
            }
        }
    }

    fun addIncome(
        walletId: String,
        amount: Double,
        description: String?,
        incomeDate: String,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            runCatching { repository.addIncome(walletId, amount, description, incomeDate) }
                .onSuccess {
                    _uiState.update { it.copy(message = "Income added") }
                    onComplete(true)
                }
                .onFailure { err ->
                    _uiState.update { it.copy(error = err.message ?: "Failed to add income") }
                    onComplete(false)
                }
        }
    }

    fun deleteExpense(id: String) {
        viewModelScope.launch {
            runCatching { repository.deleteExpense(id) }
                .onSuccess { _uiState.update { it.copy(message = "Expense deleted") } }
                .onFailure { err -> _uiState.update { it.copy(error = err.message ?: "Failed to delete expense") } }
        }
    }

    fun deleteIncome(id: String) {
        viewModelScope.launch {
            runCatching { repository.deleteIncome(id) }
                .onSuccess { _uiState.update { it.copy(message = "Income deleted") } }
                .onFailure { err -> _uiState.update { it.copy(error = err.message ?: "Failed to delete income") } }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null, error = null) }
    }

    private fun recomputeCalendar(state: CalendarUiState): CalendarUiState {
        val filteredExp = state.rawExpenses.filter {
            state.selectedWalletId == null || it.walletId == state.selectedWalletId
        }
        val filteredInc = state.rawIncome.filter {
            state.selectedWalletId == null || it.walletId == state.selectedWalletId
        }
        val (projExp, projInc) = FinanceMath.projectRecurringForMonth(
            recurring = state.recurring,
            year = state.selectedYear,
            month = state.selectedMonth,
            walletId = state.selectedWalletId
        )

        val allExp = filteredExp + projExp
        val allInc = filteredInc + projInc

        val totalsMap = mutableMapOf<String, CalendarDayTotals>()
        allExp.forEach { e ->
            val key = e.expenseDate.take(10)
            val prev = totalsMap[key] ?: CalendarDayTotals(key, 0.0, 0.0, false)
            totalsMap[key] = prev.copy(
                totalExpense = prev.totalExpense + e.amount,
                hasProjected = prev.hasProjected || e.isProjected
            )
        }
        allInc.forEach { i ->
            val key = i.incomeDate.take(10)
            val prev = totalsMap[key] ?: CalendarDayTotals(key, 0.0, 0.0, false)
            totalsMap[key] = prev.copy(
                totalIncome = prev.totalIncome + i.amount,
                hasProjected = prev.hasProjected || i.isProjected
            )
        }

        val dayExp = state.selectedDate?.let { sel ->
            allExp.filter { it.expenseDate.take(10) == sel }
        } ?: emptyList()

        val dayInc = state.selectedDate?.let { sel ->
            allInc.filter { it.incomeDate.take(10) == sel }
        } ?: emptyList()

        return state.copy(
            allExpenses = allExp,
            allIncome = allInc,
            dayTotals = totalsMap,
            selectedDayExpenses = dayExp,
            selectedDayIncome = dayInc
        )
    }
}

// =====================================================================
// 6. RecurringViewModel
// =====================================================================

data class RecurringUiState(
    val items: List<RecurringTransaction> = emptyList(),
    val summary: RecurringSummary = RecurringSummary(0, 0, 0.0, 0.0, false, null, null),
    val wallets: List<Wallet> = emptyList(),
    val categories: List<Category> = emptyList(),
    val paymentMethods: List<PaymentMethod> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null
)

class RecurringViewModel(private val repository: MoneyRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(RecurringUiState())
    val uiState: StateFlow<RecurringUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.dataVersion.collect {
                if (repository.sessionStore.getSession() != null) {
                    refresh()
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching {
                coroutineScope {
                    val recDef = async { repository.getRecurring() }
                    val walDef = async { repository.getWallets() }
                    val catDef = async { repository.getCategories() }
                    val pmDef = async { repository.getPaymentMethods() }
                    Tuple4(recDef.await(), walDef.await(), catDef.await(), pmDef.await())
                }
            }.onSuccess { (items, wallets, categories, pms) ->
                _uiState.update {
                    it.copy(
                        items = items,
                        summary = FinanceMath.computeRecurringSummary(items),
                        wallets = wallets,
                        categories = categories,
                        paymentMethods = pms,
                        isLoading = false,
                        error = null
                    )
                }
            }.onFailure { err ->
                _uiState.update { it.copy(isLoading = false, error = err.message ?: "Failed to load recurring rules") }
            }
        }
    }

    fun saveRecurring(
        editingId: String?,
        type: String,
        amount: Double,
        description: String?,
        walletId: String,
        categoryId: String?,
        paymentMethodId: String?,
        frequencyType: String,
        dayOfMonth: Int?,
        startDate: String?,
        intervalDays: Int?,
        isActive: Boolean,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching {
                if (editingId != null) {
                    repository.updateRecurring(
                        id = editingId,
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
                    )
                } else {
                    repository.addRecurring(
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
                    )
                }
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        message = if (editingId != null) "Recurring rule updated" else "Recurring rule created"
                    )
                }
                onComplete(true)
            }.onFailure { err ->
                _uiState.update { it.copy(isLoading = false, error = err.message ?: "Failed to save recurring rule") }
                onComplete(false)
            }
        }
    }

    fun toggleActive(id: String, nextActive: Boolean) {
        viewModelScope.launch {
            runCatching { repository.toggleRecurringActive(id, nextActive) }
                .onSuccess {
                    _uiState.update {
                        it.copy(message = if (nextActive) "Automation resumed" else "Automation paused")
                    }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(error = err.message ?: "Failed to update status") }
                }
        }
    }

    fun deleteRecurring(id: String) {
        viewModelScope.launch {
            runCatching { repository.deleteRecurring(id) }
                .onSuccess { _uiState.update { it.copy(message = "Recurring rule deleted") } }
                .onFailure { err -> _uiState.update { it.copy(error = err.message ?: "Failed to delete rule") } }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null, error = null) }
    }
}

// =====================================================================
// 7. SettingsViewModel
// =====================================================================

data class SettingsUiState(
    val wallets: List<Wallet> = emptyList(),
    val categories: List<Category> = emptyList(),
    val paymentMethods: List<PaymentMethod> = emptyList(),
    val userEmail: String? = null,
    val isLoading: Boolean = false,
    val isSeeding: Boolean = false,
    val error: String? = null,
    val message: String? = null
)

class SettingsViewModel(private val repository: MoneyRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(userEmail = repository.sessionStore.getSession()?.user?.email)
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.dataVersion.collect {
                if (repository.sessionStore.getSession() != null) {
                    refresh()
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    userEmail = repository.sessionStore.getSession()?.user?.email
                )
            }
            runCatching {
                coroutineScope {
                    val walDef = async { repository.getWallets() }
                    val catDef = async { repository.getCategories() }
                    val pmDef = async { repository.getPaymentMethods() }
                    Triple(walDef.await(), catDef.await(), pmDef.await())
                }
            }.onSuccess { (wallets, categories, pms) ->
                _uiState.update {
                    it.copy(
                        wallets = wallets,
                        categories = categories,
                        paymentMethods = pms,
                        isLoading = false,
                        error = null
                    )
                }
            }.onFailure { err ->
                _uiState.update { it.copy(isLoading = false, error = err.message ?: "Failed to load settings") }
            }
        }
    }

    fun addWallet(name: String, color: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            _uiState.update { it.copy(error = "Enter a wallet name first") }
            return
        }
        viewModelScope.launch {
            val nextOrder = (_uiState.value.wallets.maxOfOrNull { it.sortOrder } ?: 0) + 1
            runCatching { repository.addWallet(trimmed, color, nextOrder) }
                .onSuccess { _uiState.update { it.copy(message = "Wallet \"$trimmed\" added") } }
                .onFailure { err -> _uiState.update { it.copy(error = err.message ?: "Failed to add wallet") } }
        }
    }

    fun deleteWallet(wallet: Wallet) {
        if (wallet.isDefault) return
        viewModelScope.launch {
            runCatching { repository.deleteWallet(wallet.id) }
                .onSuccess { _uiState.update { it.copy(message = "Wallet \"${wallet.name}\" deleted") } }
                .onFailure { err -> _uiState.update { it.copy(error = err.message ?: "Failed to delete wallet") } }
        }
    }

    fun addPaymentMethod(type: String, details: String, bank: String = "", network: String = "Visa") {
        if (details.trim().isEmpty()) {
            _uiState.update {
                it.copy(
                    error = if (type == "credit") "Enter the card last 4 digits" else "Enter a bank or app name"
                )
            }
            return
        }
        viewModelScope.launch {
            val nextOrder = (_uiState.value.paymentMethods.maxOfOrNull { it.sortOrder } ?: 0) + 1
            runCatching { repository.addPaymentMethod(type, details, bank, network, nextOrder) }
                .onSuccess { _uiState.update { it.copy(message = "Payment method added") } }
                .onFailure { err -> _uiState.update { it.copy(error = err.message ?: "Failed to add payment method") } }
        }
    }

    fun deletePaymentMethod(pm: PaymentMethod) {
        viewModelScope.launch {
            runCatching { repository.deletePaymentMethod(pm.id) }
                .onSuccess { _uiState.update { it.copy(message = "Payment method deleted") } }
                .onFailure { err -> _uiState.update { it.copy(error = err.message ?: "Failed to delete payment method") } }
        }
    }

    fun movePaymentMethod(fromIndex: Int, toIndex: Int) {
        val current = _uiState.value.paymentMethods.toMutableList()
        if (fromIndex !in current.indices || toIndex !in current.indices || fromIndex == toIndex) return
        val moved = current.removeAt(fromIndex)
        current.add(toIndex, moved)
        _uiState.update { it.copy(paymentMethods = current) }

        viewModelScope.launch {
            runCatching { repository.reorderPaymentMethods(current.map { it.id }) }
                .onSuccess { _uiState.update { it.copy(message = "Payment method order saved") } }
                .onFailure { err ->
                    _uiState.update { it.copy(error = err.message ?: "Failed to reorder payment methods") }
                    refresh()
                }
        }
    }

    fun addCategory(name: String, icon: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            _uiState.update { it.copy(error = "Enter a category name") }
            return
        }
        viewModelScope.launch {
            val nextOrder = (_uiState.value.categories.maxOfOrNull { it.sortOrder } ?: 0) + 1
            runCatching { repository.addCategory(trimmed, icon, nextOrder) }
                .onSuccess { _uiState.update { it.copy(message = "Category \"$trimmed\" added") } }
                .onFailure { err -> _uiState.update { it.copy(error = err.message ?: "Failed to add category") } }
        }
    }

    fun deleteCategory(cat: Category) {
        if (cat.isDefault) return
        viewModelScope.launch {
            runCatching { repository.deleteCategory(cat.id) }
                .onSuccess { _uiState.update { it.copy(message = "Category \"${cat.name}\" deleted") } }
                .onFailure { err -> _uiState.update { it.copy(error = err.message ?: "Failed to delete category") } }
        }
    }

    fun seedDefaultCategories() {
        if (_uiState.value.isSeeding) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSeeding = true, error = null) }
            runCatching { repository.seedDefaultCategories() }
                .onSuccess { res ->
                    val msg = if (res.addedCount > 0 || res.removedCount > 0) {
                        val parts = buildList {
                            if (res.addedCount > 0) add("${res.addedCount} added")
                            if (res.removedCount > 0) add("${res.removedCount} duplicates cleaned")
                        }
                        "Categories synced (${parts.joinToString(", ")})"
                    } else {
                        "Default categories are already up to date"
                    }
                    _uiState.update { it.copy(isSeeding = false, message = msg) }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(isSeeding = false, error = err.message ?: "Failed to seed defaults") }
                }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null, error = null) }
    }
}

// =====================================================================
// 8. QuickLogViewModel (for Home Widget QuickLogActivity)
// =====================================================================

data class QuickLogUiState(
    val type: String = "expense", // "expense" | "income"
    val expression: String = "",
    val evaluatedAmount: Double? = null,
    val description: String = "",
    val dateStr: String = LocalDate.now().format(FinanceMath.ISO_DATE),
    val selectedWalletId: String = "",
    val selectedCategoryId: String = "",
    val selectedPaymentMethodId: String = "",
    val isInstallment: Boolean = false,
    val installmentMonths: Int = 3,
    val wallets: List<Wallet> = emptyList(),
    val categories: List<Category> = emptyList(),
    val paymentMethods: List<PaymentMethod> = emptyList(),
    val isAuthenticated: Boolean = true,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val savedSuccess: Boolean = false,
    val error: String? = null
)

class QuickLogViewModel(private val repository: MoneyRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(
        QuickLogUiState(isAuthenticated = repository.sessionStore.getSession() != null)
    )
    val uiState: StateFlow<QuickLogUiState> = _uiState.asStateFlow()

    fun initialize(initialType: String = "expense", preselectedWalletId: String? = null) {
        val normalizedType = if (initialType.equals("income", ignoreCase = true)) "income" else "expense"
        val isSignedIn = repository.sessionStore.getSession() != null
        _uiState.update {
            it.copy(
                type = normalizedType,
                isAuthenticated = isSignedIn,
                dateStr = LocalDate.now().format(FinanceMath.ISO_DATE),
                savedSuccess = false,
                error = null
            )
        }
        if (!isSignedIn) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching {
                coroutineScope {
                    val walDef = async { repository.getWallets() }
                    val catDef = async { repository.getCategories() }
                    val pmDef = async { repository.getPaymentMethods() }
                    Triple(walDef.await(), catDef.await(), pmDef.await())
                }
            }.onSuccess { (wallets, categories, pms) ->
                val chosenWallet = preselectedWalletId
                    ?.takeIf { id -> wallets.any { it.id == id } }
                    ?: wallets.firstOrNull { !FinanceMath.isExcludedFromDashboard(it.name) }?.id
                    ?: wallets.firstOrNull()?.id.orEmpty()
                _uiState.update {
                    it.copy(
                        wallets = wallets,
                        categories = categories,
                        paymentMethods = pms,
                        selectedWalletId = chosenWallet,
                        selectedCategoryId = categories.firstOrNull()?.id.orEmpty(),
                        selectedPaymentMethodId = pms.firstOrNull()?.id.orEmpty(),
                        isLoading = false
                    )
                }
            }.onFailure { err ->
                _uiState.update { it.copy(isLoading = false, error = err.message ?: "Failed to load accounts") }
            }
        }
    }

    fun setType(type: String) {
        val normalized = if (type.equals("income", ignoreCase = true)) "income" else "expense"
        _uiState.update { it.copy(type = normalized, error = null) }
    }

    fun setExpression(expr: String) {
        val evaluated = FinanceMath.evaluateCalcExpression(expr)
        _uiState.update { it.copy(expression = expr, evaluatedAmount = evaluated, error = null) }
    }

    fun appendCalcToken(token: String) {
        val next = _uiState.value.expression + token
        setExpression(next)
    }

    fun backspaceCalc() {
        val cur = _uiState.value.expression
        if (cur.isNotEmpty()) {
            setExpression(cur.dropLast(1))
        }
    }

    fun clearCalc() {
        setExpression("")
    }

    fun evaluateAndApplyCalc() {
        val result = FinanceMath.evaluateCalcExpression(_uiState.value.expression)
        if (result != null && result >= 0.0) {
            val formatted = if (result % 1.0 == 0.0) {
                result.toLong().toString()
            } else {
                String.format(java.util.Locale.US, "%.2f", result)
            }
            _uiState.update { it.copy(expression = formatted, evaluatedAmount = result, error = null) }
        }
    }

    fun setDescription(desc: String) {
        _uiState.update { it.copy(description = desc) }
    }

    fun setDate(dateStr: String) {
        _uiState.update { it.copy(dateStr = dateStr) }
    }

    fun setWallet(walletId: String) {
        _uiState.update { it.copy(selectedWalletId = walletId) }
    }

    fun setCategory(categoryId: String) {
        _uiState.update { it.copy(selectedCategoryId = categoryId) }
    }

    fun setPaymentMethod(paymentMethodId: String) {
        _uiState.update { it.copy(selectedPaymentMethodId = paymentMethodId) }
    }

    fun setInstallment(enabled: Boolean, months: Int = _uiState.value.installmentMonths) {
        _uiState.update {
            it.copy(
                isInstallment = enabled,
                installmentMonths = months.coerceIn(2, 60)
            )
        }
    }

    fun submit(onSaved: () -> Unit = {}) {
        val state = _uiState.value
        val amount = state.evaluatedAmount ?: FinanceMath.evaluateCalcExpression(state.expression)
        if (amount == null || amount <= 0.0) {
            _uiState.update { it.copy(error = "Enter a valid amount greater than 0") }
            return
        }
        if (state.selectedWalletId.isBlank()) {
            _uiState.update { it.copy(error = "Select a wallet first") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            runCatching {
                if (state.type == "income") {
                    repository.addIncome(
                        walletId = state.selectedWalletId,
                        amount = amount,
                        description = state.description,
                        incomeDate = state.dateStr
                    )
                } else {
                    repository.addExpense(
                        walletId = state.selectedWalletId,
                        categoryId = state.selectedCategoryId.takeIf { it.isNotBlank() },
                        paymentMethodId = state.selectedPaymentMethodId.takeIf { it.isNotBlank() },
                        amount = amount,
                        description = state.description,
                        expenseDate = state.dateStr,
                        installmentMonths = if (state.isInstallment) state.installmentMonths else 1
                    )
                }
            }.onSuccess {
                _uiState.update { it.copy(isSaving = false, savedSuccess = true, error = null) }
                onSaved()
            }.onFailure { err ->
                _uiState.update { it.copy(isSaving = false, error = err.message ?: "Failed to save transaction") }
            }
        }
    }
}

private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
private data class Tuple6<A, B, C, D, E, F>(val a: A, val b: B, val c: C, val d: D, val e: E, val f: F)
