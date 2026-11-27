package com.moneytracker.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class AuthUser(
    val id: String,
    val email: String? = null
)

@Serializable
data class AuthSession(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: Long = 3600L,
    @SerialName("expires_at") val expiresAtEpochSec: Long = 0L,
    val user: AuthUser
)

data class SignUpResult(
    val session: AuthSession?,
    val requiresEmailConfirmation: Boolean
)

@Serializable
data class WalletSummaryRef(
    val name: String,
    val color: String? = null
)

@Serializable
data class CategorySummaryRef(
    val name: String,
    val icon: String? = null
)

@Serializable
data class PaymentMethodSummaryRef(
    val label: String,
    val type: String? = null
)

@Serializable
data class Wallet(
    val id: String,
    @SerialName("user_id") val userId: String = "",
    val name: String,
    val color: String = "#28C991",
    @SerialName("sort_order") val sortOrder: Int = 0,
    @SerialName("is_default") val isDefault: Boolean = false,
    val balance: Double? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class Category(
    val id: String,
    @SerialName("user_id") val userId: String = "",
    val name: String,
    val icon: String = "tag",
    @SerialName("sort_order") val sortOrder: Int = 0,
    @SerialName("is_default") val isDefault: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class PaymentMethod(
    val id: String,
    @SerialName("user_id") val userId: String = "",
    val type: String = "cash",
    val identifier: String? = null,
    val label: String,
    @SerialName("sort_order") val sortOrder: Int = 0,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class Expense(
    val id: String,
    @SerialName("user_id") val userId: String = "",
    @SerialName("wallet_id") val walletId: String,
    @SerialName("category_id") val categoryId: String? = null,
    @SerialName("payment_method_id") val paymentMethodId: String? = null,
    val amount: Double,
    val description: String? = null,
    @SerialName("expense_date") val expenseDate: String,
    @SerialName("is_recurring") val isRecurring: Boolean? = null,
    @SerialName("recurring_id") val recurringId: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val wallets: WalletSummaryRef? = null,
    val categories: CategorySummaryRef? = null,
    @SerialName("payment_methods") val paymentMethods: PaymentMethodSummaryRef? = null,
    @Transient val isProjected: Boolean = false
)

@Serializable
data class Income(
    val id: String,
    @SerialName("user_id") val userId: String = "",
    @SerialName("wallet_id") val walletId: String,
    val amount: Double,
    val description: String? = null,
    @SerialName("income_date") val incomeDate: String,
    @SerialName("created_at") val createdAt: String? = null,
    val wallets: WalletSummaryRef? = null,
    @Transient val isProjected: Boolean = false
)

@Serializable
data class RecurringTransaction(
    val id: String,
    @SerialName("user_id") val userId: String = "",
    val type: String = "expense",
    val amount: Double,
    val description: String? = null,
    @SerialName("wallet_id") val walletId: String,
    @SerialName("category_id") val categoryId: String? = null,
    @SerialName("payment_method_id") val paymentMethodId: String? = null,
    @SerialName("frequency_type") val frequencyType: String = "monthly",
    @SerialName("day_of_month") val dayOfMonth: Int? = null,
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("interval_days") val intervalDays: Int? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("last_posted_date") val lastPostedDate: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val wallets: WalletSummaryRef? = null,
    val categories: CategorySummaryRef? = null,
    @SerialName("payment_methods") val paymentMethods: PaymentMethodSummaryRef? = null
)

// --- Mutation Payloads ---

@Serializable
data class WalletInsert(
    @SerialName("user_id") val userId: String,
    val name: String,
    val color: String,
    @SerialName("sort_order") val sortOrder: Int = 0,
    val balance: Double = 0.0
)

@Serializable
data class CategoryInsert(
    @SerialName("user_id") val userId: String,
    val name: String,
    val icon: String,
    @SerialName("sort_order") val sortOrder: Int = 0
)

@Serializable
data class PaymentMethodInsert(
    @SerialName("user_id") val userId: String,
    val type: String,
    val label: String,
    val identifier: String? = null,
    @SerialName("sort_order") val sortOrder: Int = 0
)

@Serializable
data class ExpenseInsert(
    @SerialName("user_id") val userId: String,
    @SerialName("wallet_id") val walletId: String,
    @SerialName("category_id") val categoryId: String? = null,
    @SerialName("payment_method_id") val paymentMethodId: String? = null,
    val amount: Double,
    val description: String? = null,
    @SerialName("expense_date") val expenseDate: String,
    @SerialName("is_recurring") val isRecurring: Boolean? = null,
    @SerialName("recurring_id") val recurringId: String? = null
)

@Serializable
data class ExpenseUpdate(
    @SerialName("wallet_id") val walletId: String,
    @SerialName("category_id") val categoryId: String? = null,
    @SerialName("payment_method_id") val paymentMethodId: String? = null,
    val amount: Double,
    val description: String? = null,
    @SerialName("expense_date") val expenseDate: String
)

@Serializable
data class IncomeInsert(
    @SerialName("user_id") val userId: String,
    @SerialName("wallet_id") val walletId: String,
    val amount: Double,
    val description: String? = null,
    @SerialName("income_date") val incomeDate: String
)

@Serializable
data class IncomeUpdate(
    @SerialName("wallet_id") val walletId: String,
    val amount: Double,
    val description: String? = null,
    @SerialName("income_date") val incomeDate: String
)

@Serializable
data class RecurringInsert(
    @SerialName("user_id") val userId: String,
    val type: String,
    val amount: Double,
    val description: String? = null,
    @SerialName("wallet_id") val walletId: String,
    @SerialName("category_id") val categoryId: String? = null,
    @SerialName("payment_method_id") val paymentMethodId: String? = null,
    @SerialName("frequency_type") val frequencyType: String,
    @SerialName("day_of_month") val dayOfMonth: Int? = null,
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("interval_days") val intervalDays: Int? = null,
    @SerialName("is_active") val isActive: Boolean = true
)

@Serializable
data class RecurringUpdate(
    val type: String,
    val amount: Double,
    val description: String? = null,
    @SerialName("wallet_id") val walletId: String,
    @SerialName("category_id") val categoryId: String? = null,
    @SerialName("payment_method_id") val paymentMethodId: String? = null,
    @SerialName("frequency_type") val frequencyType: String,
    @SerialName("day_of_month") val dayOfMonth: Int? = null,
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("interval_days") val intervalDays: Int? = null,
    @SerialName("is_active") val isActive: Boolean = true
)

// --- Domain & UI Aggregation Models ---

enum class DashboardTimeframe {
    MONTHLY,
    ANNUAL
}

enum class TransactionSortOrder {
    DATE_DESC,
    DATE_ASC,
    AMOUNT_DESC,
    AMOUNT_ASC
}

enum class PaceDeltaTone {
    UP,
    DOWN,
    NEUTRAL
}

data class CategorySpend(
    val categoryId: String?,
    val name: String,
    val icon: String,
    val totalAmount: Double
)

data class TrendPoint(
    val label: String,
    val total: Double
)

data class CategoryAverage(
    val categoryId: String?,
    val categoryName: String,
    val amount: Double
)

data class WalletDashboardData(
    val wallet: Wallet,
    val expenses: List<CategorySpend>,
    val trendData: List<TrendPoint>,
    val averages: List<CategoryAverage>,
    val totalIncome: Double,
    val totalExpenses: Double
) {
    val balance: Double get() = totalIncome - totalExpenses
}

data class DashboardSummary(
    val walletsData: List<WalletDashboardData>,
    val activeWalletName: String,
    val totalIncome: Double,
    val totalExpenses: Double,
    val allTotalIncome: Double,
    val allTotalExpenses: Double,
    val rawCategorySpends: List<CategorySpend>,
    val filteredCategorySpends: List<CategorySpend>,
    val trendData: List<TrendPoint>,
    val averages: List<CategoryAverage>
) {
    val netBalance: Double get() = totalIncome - totalExpenses
}

data class RecentTxItem(
    val id: String,
    val type: String,
    val amount: Double,
    val description: String,
    val dateStr: String,
    val createdAt: String,
    val categoryIcon: String?,
    val walletName: String
)

data class RecurringSummary(
    val activeCount: Int,
    val totalCount: Int,
    val monthlyIncome: Double,
    val monthlyExpenses: Double,
    val hasInterval: Boolean,
    val nextItem: RecurringTransaction?,
    val nextDueDate: String?
)

data class PaceSummary(
    val latest: Double,
    val previous: Double,
    val deltaPercent: Int?,
    val deltaText: String,
    val deltaTone: PaceDeltaTone
)

// --- Home Screen Widget Snapshot Models ---

@Serializable
data class WidgetWalletBalance(
    val id: String,
    val name: String,
    val color: String = "#28C991",
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0
) {
    val balance: Double get() = totalIncome - totalExpenses
}

@Serializable
data class WidgetSnapshot(
    val isAuthenticated: Boolean = false,
    val isPrivacyMasked: Boolean = true,
    val selectedWalletIndex: Int = 0,
    val wallets: List<WidgetWalletBalance> = emptyList(),
    val totalNetBalance: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val thisMonthSpent: Double = 0.0,
    val updatedAtEpochMs: Long = 0L,
    val userEmail: String? = null
) {
    val cycleCount: Int get() = if (wallets.isEmpty()) 1 else wallets.size + 1

    val activeWallet: WidgetWalletBalance?
        get() {
            val safeIdx = selectedWalletIndex.coerceIn(0, wallets.size)
            return if (safeIdx == 0) null else wallets.getOrNull(safeIdx - 1)
        }

    val displayedWalletName: String
        get() = activeWallet?.name ?: "All Wallets"

    val displayedWalletColor: String
        get() = activeWallet?.color ?: "#28C991"

    val displayedBalance: Double
        get() = activeWallet?.balance ?: totalNetBalance

    val displayedWalletId: String?
        get() = activeWallet?.id
}
