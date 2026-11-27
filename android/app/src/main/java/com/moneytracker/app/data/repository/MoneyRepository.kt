package com.moneytracker.app.data.repository

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.moneytracker.app.data.local.SessionStore
import com.moneytracker.app.data.local.WidgetSnapshotStore
import com.moneytracker.app.data.model.AuthSession
import com.moneytracker.app.data.model.Category
import com.moneytracker.app.data.model.CategoryInsert
import com.moneytracker.app.data.model.DashboardSummary
import com.moneytracker.app.data.model.DashboardTimeframe
import com.moneytracker.app.data.model.Expense
import com.moneytracker.app.data.model.ExpenseInsert
import com.moneytracker.app.data.model.ExpenseUpdate
import com.moneytracker.app.data.model.Income
import com.moneytracker.app.data.model.IncomeInsert
import com.moneytracker.app.data.model.IncomeUpdate
import com.moneytracker.app.data.model.PaymentMethod
import com.moneytracker.app.data.model.PaymentMethodInsert
import com.moneytracker.app.data.model.RecurringInsert
import com.moneytracker.app.data.model.RecurringTransaction
import com.moneytracker.app.data.model.RecurringUpdate
import com.moneytracker.app.data.model.SignUpResult
import com.moneytracker.app.data.model.Wallet
import com.moneytracker.app.data.model.WalletInsert
import com.moneytracker.app.data.model.WidgetSnapshot
import com.moneytracker.app.data.model.WidgetWalletBalance
import com.moneytracker.app.data.remote.SupabaseClient
import com.moneytracker.app.data.remote.UnauthorizedException
import com.moneytracker.app.domain.FinanceMath
import com.moneytracker.app.widget.WalletGlanceWidget
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.min

data class SeedCategoriesResult(
    val addedCount: Int,
    val removedCount: Int
)

data class DashboardBundle(
    val summary: DashboardSummary,
    val monthExpenses: List<Expense>,
    val monthIncome: List<Income>,
    val recurring: List<RecurringTransaction>,
    val wallets: List<Wallet>,
    val categories: List<Category>,
    val paymentMethods: List<PaymentMethod>
)

class MoneyRepository(
    private val context: Context,
    val sessionStore: SessionStore,
    val widgetSnapshotStore: WidgetSnapshotStore,
    val supabase: SupabaseClient
) {
    private val appContext = context.applicationContext
    private val autoPostMutex = Mutex()
    private var autoPostedForUserId: String? = null

    private val _dataVersion = MutableStateFlow(0L)
    val dataVersion: StateFlow<Long> = _dataVersion.asStateFlow()

    val sessionFlow: StateFlow<AuthSession?> = sessionStore.sessionFlow

    private fun requireUserId(): String {
        return sessionStore.getSession()?.user?.id ?: throw UnauthorizedException()
    }

    private fun bumpVersion() {
        _dataVersion.value = _dataVersion.value + 1L
    }

    // --- Authentication ---

    suspend fun signIn(email: String, password: String): AuthSession {
        val session = supabase.signIn(email, password)
        autoPostedForUserId = null
        runAutoPostIfNeeded()
        runCatching { refreshWidgetSnapshot() }
        bumpVersion()
        return session
    }

    suspend fun signUp(email: String, password: String): SignUpResult {
        val result = supabase.signUp(email, password)
        if (result.session != null) {
            autoPostedForUserId = null
            runAutoPostIfNeeded()
            runCatching { refreshWidgetSnapshot() }
            bumpVersion()
        }
        return result
    }

    suspend fun signOut() {
        supabase.signOut()
        autoPostedForUserId = null
        val current = widgetSnapshotStore.readSnapshot()
        widgetSnapshotStore.saveSnapshot(
            WidgetSnapshot(
                isAuthenticated = false,
                isPrivacyMasked = current.isPrivacyMasked,
                selectedWalletIndex = 0,
                updatedAtEpochMs = System.currentTimeMillis()
            )
        )
        updateWidgetsSafely()
        bumpVersion()
    }

    // --- AutoPostManager (once per authenticated session) ---

    suspend fun runAutoPostIfNeeded(): Int = autoPostMutex.withLock {
        val session = sessionStore.getSession() ?: return 0
        val userId = session.user.id
        if (autoPostedForUserId == userId) return 0
        autoPostedForUserId = userId

        var postedCount = 0
        runCatching {
            val today = LocalDate.now()
            val currentMonthStart = today.withDayOfMonth(1).format(FinanceMath.ISO_DATE)

            // 1. Deduplicate current-month income by description|income_date|wallet_id
            val recentIncome = supabase.getIncome(userId = userId, startDate = currentMonthStart)
            val duplicateIncomeIds = recentIncome
                .groupBy { "${it.description.orEmpty()}|${it.incomeDate}|${it.walletId}" }
                .values
                .flatMap { group -> if (group.size > 1) group.drop(1).map { it.id } else emptyList() }
            if (duplicateIncomeIds.isNotEmpty()) {
                supabase.deleteIncomeByIds(duplicateIncomeIds, userId)
            }

            // 2. Deduplicate current-month expenses by description|expense_date|wallet_id
            val recentExpenses = supabase.getExpenses(userId = userId, startDate = currentMonthStart)
            val duplicateExpenseIds = recentExpenses
                .groupBy { "${it.description.orEmpty()}|${it.expenseDate}|${it.walletId}" }
                .values
                .flatMap { group -> if (group.size > 1) group.drop(1).map { it.id } else emptyList() }
            if (duplicateExpenseIds.isNotEmpty()) {
                supabase.deleteExpensesByIds(duplicateExpenseIds, userId)
            }

            // 3. Post due active recurring transactions
            val activeRecurring = supabase.getRecurring(userId = userId, activeOnly = true)
            for (item in activeRecurring) {
                val freq = item.frequencyType.lowercase()
                if (freq == "interval") {
                    val startDate = FinanceMath.parseLocalDate(item.startDate) ?: continue
                    val intervalDays = item.intervalDays ?: continue
                    if (intervalDays <= 0) continue

                    val lastPostedParsed = FinanceMath.parseLocalDate(item.lastPostedDate)
                    var nextDue = if (lastPostedParsed != null) {
                        lastPostedParsed.plusDays(intervalDays.toLong())
                    } else {
                        startDate
                    }

                    var lastPostedStr: String? = null
                    var safety = 0
                    while (!nextDue.isAfter(today) && safety < 500) {
                        val postDateStr = nextDue.format(FinanceMath.ISO_DATE)
                        postRecurringInstance(userId, item, postDateStr)
                        lastPostedStr = postDateStr
                        postedCount++
                        nextDue = nextDue.plusDays(intervalDays.toLong())
                        safety++
                    }

                    if (lastPostedStr != null) {
                        supabase.updateRecurringLastPosted(item.id, userId, lastPostedStr)
                    }
                } else {
                    val dom = (item.dayOfMonth ?: 1).coerceAtLeast(1)
                    val isDue = dom <= today.dayOfMonth
                    val lastDate = FinanceMath.parseLocalDate(item.lastPostedDate)
                    val alreadyPostedThisMonth = lastDate != null &&
                        lastDate.year == today.year &&
                        lastDate.monthValue == today.monthValue

                    if (isDue && !alreadyPostedThisMonth) {
                        val clampedDay = min(dom, today.lengthOfMonth())
                        val postDateStr = LocalDate.of(today.year, today.monthValue, clampedDay)
                            .format(FinanceMath.ISO_DATE)
                        postRecurringInstance(userId, item, postDateStr)
                        supabase.updateRecurringLastPosted(item.id, userId, postDateStr)
                        postedCount++
                    }
                }
            }

            if (postedCount > 0 || duplicateIncomeIds.isNotEmpty() || duplicateExpenseIds.isNotEmpty()) {
                refreshWidgetSnapshot()
                bumpVersion()
            }
        }
        postedCount
    }

    private suspend fun postRecurringInstance(
        userId: String,
        item: RecurringTransaction,
        postDateStr: String
    ) {
        if (item.type.equals("income", ignoreCase = true)) {
            supabase.insertIncome(
                IncomeInsert(
                    userId = userId,
                    walletId = item.walletId,
                    amount = item.amount,
                    description = item.description?.takeIf { it.isNotBlank() } ?: "Recurring Income",
                    incomeDate = postDateStr
                )
            )
        } else {
            supabase.insertExpenses(
                listOf(
                    ExpenseInsert(
                        userId = userId,
                        walletId = item.walletId,
                        categoryId = item.categoryId,
                        paymentMethodId = item.paymentMethodId,
                        amount = item.amount,
                        description = item.description?.takeIf { it.isNotBlank() } ?: "Recurring Expense",
                        expenseDate = postDateStr,
                        isRecurring = true,
                        recurringId = item.id
                    )
                )
            )
        }
    }

    // --- Dashboard ---

    suspend fun fetchDashboardBundle(
        year: Int,
        month: Int,
        timeframe: DashboardTimeframe,
        selectedWalletId: String? = null,
        excludedCategoryIds: Set<String> = emptySet()
    ): DashboardBundle = coroutineScope {
        val userId = requireUserId()
        val ym = YearMonth.of(year, month.coerceIn(1, 12))
        val monthStart = ym.atDay(1).format(FinanceMath.ISO_DATE)
        val monthEnd = ym.atEndOfMonth().format(FinanceMath.ISO_DATE)

        val walletsDef = async { supabase.getWallets(userId) }
        val categoriesDef = async { supabase.getCategories(userId) }
        val pmDef = async { supabase.getPaymentMethods(userId) }
        val allExpensesDef = async { supabase.getExpenses(userId) }
        val allIncomeDef = async { supabase.getIncome(userId) }
        val recurringDef = async { supabase.getRecurring(userId) }

        val wallets = walletsDef.await()
        val categories = categoriesDef.await()
        val paymentMethods = pmDef.await()
        val allExpenses = allExpensesDef.await()
        val allIncome = allIncomeDef.await()
        val recurring = recurringDef.await()

        val summary = FinanceMath.computeDashboardByWallet(
            wallets = wallets,
            allExpenses = allExpenses,
            allIncome = allIncome,
            year = year,
            month = month,
            timeframe = timeframe,
            selectedWalletId = selectedWalletId,
            excludedCategoryIds = excludedCategoryIds
        )

        val monthExpenses = allExpenses.filter {
            it.expenseDate >= monthStart && it.expenseDate <= monthEnd
        }
        val monthIncome = allIncome.filter {
            it.incomeDate >= monthStart && it.incomeDate <= monthEnd
        }

        // Keep widget snapshot fresh from this fetch at zero extra network cost
        runCatching {
            saveWidgetSnapshotFromData(
                wallets = wallets,
                allExpenses = allExpenses,
                allIncome = allIncome
            )
        }

        DashboardBundle(
            summary = summary,
            monthExpenses = monthExpenses,
            monthIncome = monthIncome,
            recurring = recurring,
            wallets = wallets,
            categories = categories,
            paymentMethods = paymentMethods
        )
    }

    // --- Wallets ---

    suspend fun getWallets(): List<Wallet> = supabase.getWallets(requireUserId())

    suspend fun addWallet(name: String, color: String, sortOrder: Int = 0): Wallet {
        val userId = requireUserId()
        val created = supabase.insertWallet(
            WalletInsert(
                userId = userId,
                name = name.trim(),
                color = color,
                sortOrder = sortOrder,
                balance = 0.0
            )
        )
        afterMutation()
        return created
    }

    suspend fun deleteWallet(id: String) {
        supabase.deleteWallet(id, requireUserId())
        afterMutation()
    }

    // --- Categories ---

    suspend fun getCategories(): List<Category> = supabase.getCategories(requireUserId())

    suspend fun addCategory(name: String, icon: String, sortOrder: Int = 0): Category {
        val userId = requireUserId()
        val created = supabase.insertCategory(
            CategoryInsert(
                userId = userId,
                name = name.trim(),
                icon = icon.ifBlank { "tag" },
                sortOrder = sortOrder
            )
        )
        bumpVersion()
        return created
    }

    suspend fun deleteCategory(id: String) {
        supabase.deleteCategory(id, requireUserId())
        bumpVersion()
    }

    suspend fun seedDefaultCategories(): SeedCategoriesResult {
        val userId = requireUserId()
        val existing = supabase.getCategories(userId)

        var removedCount = 0
        val grouped = existing.groupBy { it.name.trim().lowercase() }
        for ((_, copies) in grouped) {
            if (copies.size > 1) {
                copies.drop(1).forEach { dup ->
                    runCatching {
                        supabase.deleteCategory(dup.id, userId)
                        removedCount++
                    }
                }
            }
        }

        val existingNames = existing.map { it.name.trim().lowercase() }.toSet()
        var addedCount = 0
        for (seed in FinanceMath.DEFAULT_CATEGORIES) {
            if (!existingNames.contains(seed.name.lowercase())) {
                runCatching {
                    supabase.insertCategory(
                        CategoryInsert(
                            userId = userId,
                            name = seed.name,
                            icon = seed.icon,
                            sortOrder = seed.sortOrder
                        )
                    )
                    addedCount++
                }
            }
        }

        if (addedCount > 0 || removedCount > 0) {
            bumpVersion()
        }
        return SeedCategoriesResult(addedCount = addedCount, removedCount = removedCount)
    }

    // --- Payment Methods ---

    suspend fun getPaymentMethods(): List<PaymentMethod> = supabase.getPaymentMethods(requireUserId())

    suspend fun addPaymentMethod(
        type: String,
        details: String,
        bank: String = "",
        network: String = "Visa",
        sortOrder: Int = 0
    ): PaymentMethod {
        val userId = requireUserId()
        val label = FinanceMath.formatPaymentMethodLabel(
            type = type,
            details = details,
            bank = bank,
            network = network
        )
        val created = supabase.insertPaymentMethod(
            PaymentMethodInsert(
                userId = userId,
                type = type,
                label = label,
                identifier = details.trim().takeIf { it.isNotEmpty() },
                sortOrder = sortOrder
            )
        )
        bumpVersion()
        return created
    }

    suspend fun deletePaymentMethod(id: String) {
        supabase.deletePaymentMethod(id, requireUserId())
        bumpVersion()
    }

    suspend fun reorderPaymentMethods(orderedIds: List<String>) = coroutineScope {
        val userId = requireUserId()
        orderedIds.mapIndexed { index, id ->
            async { supabase.updatePaymentMethodSortOrder(id, userId, index + 1) }
        }.awaitAll()
        bumpVersion()
    }

    // --- Expenses ---

    suspend fun getExpensesForMonth(year: Int, month: Int): List<Expense> {
        val userId = requireUserId()
        val ym = YearMonth.of(year, month.coerceIn(1, 12))
        return supabase.getExpenses(
            userId = userId,
            startDate = ym.atDay(1).format(FinanceMath.ISO_DATE),
            endDate = ym.atEndOfMonth().format(FinanceMath.ISO_DATE)
        )
    }

    suspend fun addExpense(
        walletId: String,
        categoryId: String?,
        paymentMethodId: String?,
        amount: Double,
        description: String?,
        expenseDate: String,
        installmentMonths: Int = 1
    ): List<Expense> {
        val userId = requireUserId()
        val base = ExpenseInsert(
            userId = userId,
            walletId = walletId,
            categoryId = categoryId?.takeIf { it.isNotBlank() },
            paymentMethodId = paymentMethodId?.takeIf { it.isNotBlank() },
            amount = amount,
            description = description?.trim()?.takeIf { it.isNotEmpty() },
            expenseDate = expenseDate.ifBlank { LocalDate.now().format(FinanceMath.ISO_DATE) }
        )
        val batch = FinanceMath.buildInstallmentBatch(base, installmentMonths)
        val created = supabase.insertExpenses(batch)
        afterMutation()
        return created
    }

    suspend fun updateExpense(
        id: String,
        walletId: String,
        categoryId: String?,
        paymentMethodId: String?,
        amount: Double,
        description: String?,
        expenseDate: String
    ): Expense {
        val userId = requireUserId()
        val updated = supabase.updateExpense(
            id = id,
            userId = userId,
            payload = ExpenseUpdate(
                walletId = walletId,
                categoryId = categoryId?.takeIf { it.isNotBlank() },
                paymentMethodId = paymentMethodId?.takeIf { it.isNotBlank() },
                amount = amount,
                description = description?.trim()?.takeIf { it.isNotEmpty() },
                expenseDate = expenseDate
            )
        )
        afterMutation()
        return updated
    }

    suspend fun deleteExpense(id: String) {
        supabase.deleteExpense(id, requireUserId())
        afterMutation()
    }

    // --- Income ---

    suspend fun getIncomeForMonth(year: Int, month: Int): List<Income> {
        val userId = requireUserId()
        val ym = YearMonth.of(year, month.coerceIn(1, 12))
        return supabase.getIncome(
            userId = userId,
            startDate = ym.atDay(1).format(FinanceMath.ISO_DATE),
            endDate = ym.atEndOfMonth().format(FinanceMath.ISO_DATE)
        )
    }

    suspend fun addIncome(
        walletId: String,
        amount: Double,
        description: String?,
        incomeDate: String
    ): Income {
        val userId = requireUserId()
        val created = supabase.insertIncome(
            IncomeInsert(
                userId = userId,
                walletId = walletId,
                amount = amount,
                description = description?.trim()?.takeIf { it.isNotEmpty() },
                incomeDate = incomeDate.ifBlank { LocalDate.now().format(FinanceMath.ISO_DATE) }
            )
        )
        afterMutation()
        return created
    }

    suspend fun updateIncome(
        id: String,
        walletId: String,
        amount: Double,
        description: String?,
        incomeDate: String
    ): Income {
        val userId = requireUserId()
        val updated = supabase.updateIncome(
            id = id,
            userId = userId,
            payload = IncomeUpdate(
                walletId = walletId,
                amount = amount,
                description = description?.trim()?.takeIf { it.isNotEmpty() },
                incomeDate = incomeDate
            )
        )
        afterMutation()
        return updated
    }

    suspend fun deleteIncome(id: String) {
        supabase.deleteIncome(id, requireUserId())
        afterMutation()
    }

    // --- Recurring Transactions ---

    suspend fun getRecurring(): List<RecurringTransaction> = supabase.getRecurring(requireUserId())

    suspend fun addRecurring(
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
        isActive: Boolean
    ): RecurringTransaction {
        val userId = requireUserId()
        val isIncome = type.equals("income", ignoreCase = true)
        val isMonthly = !frequencyType.equals("interval", ignoreCase = true)
        val created = supabase.insertRecurring(
            RecurringInsert(
                userId = userId,
                type = if (isIncome) "income" else "expense",
                amount = amount,
                description = description?.trim()?.takeIf { it.isNotEmpty() },
                walletId = walletId,
                categoryId = if (isIncome) null else categoryId?.takeIf { it.isNotBlank() },
                paymentMethodId = if (isIncome) null else paymentMethodId?.takeIf { it.isNotBlank() },
                frequencyType = if (isMonthly) "monthly" else "interval",
                dayOfMonth = if (isMonthly) (dayOfMonth ?: 1).coerceIn(1, 31) else null,
                startDate = if (!isMonthly) startDate else null,
                intervalDays = if (!isMonthly) (intervalDays ?: 30).coerceAtLeast(1) else null,
                isActive = isActive
            )
        )
        bumpVersion()
        return created
    }

    suspend fun updateRecurring(
        id: String,
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
        isActive: Boolean
    ): RecurringTransaction {
        val userId = requireUserId()
        val isIncome = type.equals("income", ignoreCase = true)
        val isMonthly = !frequencyType.equals("interval", ignoreCase = true)
        val updated = supabase.updateRecurring(
            id = id,
            userId = userId,
            payload = RecurringUpdate(
                type = if (isIncome) "income" else "expense",
                amount = amount,
                description = description?.trim()?.takeIf { it.isNotEmpty() },
                walletId = walletId,
                categoryId = if (isIncome) null else categoryId?.takeIf { it.isNotBlank() },
                paymentMethodId = if (isIncome) null else paymentMethodId?.takeIf { it.isNotBlank() },
                frequencyType = if (isMonthly) "monthly" else "interval",
                dayOfMonth = if (isMonthly) (dayOfMonth ?: 1).coerceIn(1, 31) else null,
                startDate = if (!isMonthly) startDate else null,
                intervalDays = if (!isMonthly) (intervalDays ?: 30).coerceAtLeast(1) else null,
                isActive = isActive
            )
        )
        bumpVersion()
        return updated
    }

    suspend fun toggleRecurringActive(id: String, isActive: Boolean): RecurringTransaction {
        val updated = supabase.updateRecurringActive(id, requireUserId(), isActive)
        bumpVersion()
        return updated
    }

    suspend fun deleteRecurring(id: String) {
        supabase.deleteRecurring(id, requireUserId())
        bumpVersion()
    }

    // --- Widget Snapshot Sync ---

    suspend fun refreshWidgetSnapshot(): WidgetSnapshot = coroutineScope {
        val session = sessionStore.getSession()
        val existing = widgetSnapshotStore.readSnapshot()
        if (session == null) {
            val signedOut = WidgetSnapshot(
                isAuthenticated = false,
                isPrivacyMasked = existing.isPrivacyMasked,
                selectedWalletIndex = 0,
                updatedAtEpochMs = System.currentTimeMillis()
            )
            widgetSnapshotStore.saveSnapshot(signedOut)
            updateWidgetsSafely()
            return@coroutineScope signedOut
        }

        val userId = session.user.id
        val walletsDef = async { supabase.getWallets(userId) }
        val expensesDef = async { supabase.getExpenses(userId) }
        val incomeDef = async { supabase.getIncome(userId) }

        saveWidgetSnapshotFromData(
            wallets = walletsDef.await(),
            allExpenses = expensesDef.await(),
            allIncome = incomeDef.await()
        )
    }

    private suspend fun saveWidgetSnapshotFromData(
        wallets: List<Wallet>,
        allExpenses: List<Expense>,
        allIncome: List<Income>
    ): WidgetSnapshot {
        val session = sessionStore.getSession()
        val existing = widgetSnapshotStore.readSnapshot()

        val visibleWallets = wallets
            .filterNot { FinanceMath.isExcludedFromDashboard(it.name) }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
        val visibleIds = visibleWallets.map { it.id }.toSet()

        val incByWallet = mutableMapOf<String, Double>()
        allIncome.forEach { inc ->
            if (visibleIds.contains(inc.walletId)) {
                incByWallet[inc.walletId] = (incByWallet[inc.walletId] ?: 0.0) + inc.amount
            }
        }

        val expByWallet = mutableMapOf<String, Double>()
        val today = LocalDate.now()
        var thisMonthSpent = 0.0
        allExpenses.forEach { exp ->
            if (visibleIds.contains(exp.walletId)) {
                expByWallet[exp.walletId] = (expByWallet[exp.walletId] ?: 0.0) + exp.amount
                val d = FinanceMath.parseLocalDate(exp.expenseDate)
                if (d != null && d.year == today.year && d.monthValue == today.monthValue) {
                    thisMonthSpent += exp.amount
                }
            }
        }

        val walletBalances = visibleWallets.map { w ->
            WidgetWalletBalance(
                id = w.id,
                name = w.name,
                color = w.color,
                totalIncome = incByWallet[w.id] ?: 0.0,
                totalExpenses = expByWallet[w.id] ?: 0.0
            )
        }

        val totalInc = walletBalances.sumOf { it.totalIncome }
        val totalExp = walletBalances.sumOf { it.totalExpenses }
        val clampedIdx = existing.selectedWalletIndex.coerceIn(0, walletBalances.size)

        val snapshot = WidgetSnapshot(
            isAuthenticated = session != null,
            isPrivacyMasked = existing.isPrivacyMasked,
            selectedWalletIndex = clampedIdx,
            wallets = walletBalances,
            totalNetBalance = totalInc - totalExp,
            totalIncome = totalInc,
            totalExpenses = totalExp,
            thisMonthSpent = thisMonthSpent,
            updatedAtEpochMs = System.currentTimeMillis(),
            userEmail = session?.user?.email
        )

        widgetSnapshotStore.saveSnapshot(snapshot)
        updateWidgetsSafely()
        return snapshot
    }

    private suspend fun afterMutation() {
        bumpVersion()
        runCatching { refreshWidgetSnapshot() }
    }

    private suspend fun updateWidgetsSafely() {
        runCatching { WalletGlanceWidget().updateAll(appContext) }
    }
}
