package com.moneytracker.app

import com.moneytracker.app.data.model.CategorySummaryRef
import com.moneytracker.app.data.model.DashboardTimeframe
import com.moneytracker.app.data.model.Expense
import com.moneytracker.app.data.model.ExpenseInsert
import com.moneytracker.app.data.model.Income
import com.moneytracker.app.data.model.PaceDeltaTone
import com.moneytracker.app.data.model.RecurringTransaction
import com.moneytracker.app.data.model.TrendPoint
import com.moneytracker.app.data.model.Wallet
import com.moneytracker.app.data.model.WidgetSnapshot
import com.moneytracker.app.data.model.WidgetWalletBalance
import com.moneytracker.app.domain.FinanceMath
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FinanceMathTest {

    @Test
    fun formatPhpCurrency_standardCompactAndMasked() {
        assertEquals("••••••", FinanceMath.formatPhpCurrency(12345.67, masked = true))
        assertEquals("••••••", FinanceMath.formatPhpCurrency(12345.67, compact = true, masked = true))

        assertEquals("₱0.00", FinanceMath.formatPhpCurrency(0.0))
        assertEquals("₱1,234.56", FinanceMath.formatPhpCurrency(1234.56))
        assertEquals("-₱500.50", FinanceMath.formatPhpCurrency(-500.5))

        assertEquals("₱1.2K", FinanceMath.formatPhpCurrency(1200.0, compact = true))
        assertEquals("₱1.2M", FinanceMath.formatPhpCurrency(1_200_000.0, compact = true))
        assertEquals("-₱2.5K", FinanceMath.formatPhpCurrency(-2500.0, compact = true))
        assertEquals("₱500", FinanceMath.formatPhpCurrency(500.0, compact = true))
    }

    @Test
    fun evaluateCalcExpression_precedenceParenthesesPercentAndErrors() {
        assertEquals(14.0, FinanceMath.evaluateCalcExpression("2 + 3 * 4")!!, 1e-6)
        assertEquals(20.0, FinanceMath.evaluateCalcExpression("(2 + 3) * 4")!!, 1e-6)
        assertEquals(15.0, FinanceMath.evaluateCalcExpression("30 ÷ 2")!!, 1e-6)
        assertEquals(12.5, FinanceMath.evaluateCalcExpression("2.5 × 5")!!, 1e-6)
        assertEquals(-5.0, FinanceMath.evaluateCalcExpression("-(2 + 3)")!!, 1e-6)
        assertEquals(0.5, FinanceMath.evaluateCalcExpression("50%")!!, 1e-6)
        assertEquals(20.0, FinanceMath.evaluateCalcExpression("200 * 10%")!!, 1e-6)
        assertEquals(220.0, FinanceMath.evaluateCalcExpression("200 + 10%")!!, 1e-6)
        assertEquals(85.0, FinanceMath.evaluateCalcExpression("100 - 15%")!!, 1e-6)

        // Error cases
        assertNull(FinanceMath.evaluateCalcExpression(""))
        assertNull(FinanceMath.evaluateCalcExpression("   "))
        assertNull(FinanceMath.evaluateCalcExpression("10 / 0"))
        assertNull(FinanceMath.evaluateCalcExpression("(2 + 3"))
        assertNull(FinanceMath.evaluateCalcExpression("2 + * 3"))
        assertNull(FinanceMath.evaluateCalcExpression("abc"))
    }

    @Test
    fun buildInstallmentBatch_splitsAcrossMonthsWithExactLabels() {
        val base = ExpenseInsert(
            userId = "u1",
            walletId = "w1",
            categoryId = "c1",
            paymentMethodId = "pm1",
            amount = 1500.0,
            description = "Laptop",
            expenseDate = "2026-08-31"
        )

        val single = FinanceMath.buildInstallmentBatch(base, 1)
        assertEquals(1, single.size)
        assertEquals("Laptop", single[0].description)

        val batch = FinanceMath.buildInstallmentBatch(base, 3)
        assertEquals(3, batch.size)
        assertEquals("Laptop (Installment 1/3)", batch[0].description)
        assertEquals("2026-08-31", batch[0].expenseDate)
        assertEquals("Laptop (Installment 2/3)", batch[1].description)
        assertEquals("2026-09-30", batch[1].expenseDate)
        assertEquals("Laptop (Installment 3/3)", batch[2].description)
        assertEquals("2026-10-31", batch[2].expenseDate)

        // Blank description falls back to "Expense"
        val blankBatch = FinanceMath.buildInstallmentBatch(base.copy(description = "  "), 2)
        assertEquals("Expense (Installment 1/2)", blankBatch[0].description)
        assertEquals("Expense (Installment 2/2)", blankBatch[1].description)
    }

    @Test
    fun dashboardAggregation_excludesAlexCaseInsensitivelyAndComputesTrends() {
        val wallets = listOf(
            Wallet(id = "w-alex", name = "Alex", color = "#F06478"),
            Wallet(id = "w-alex2", name = "  ALEX ", color = "#F06478"),
            Wallet(id = "w-main", name = "BPI Savings", color = "#28C991"),
            Wallet(id = "w-cash", name = "Cash", color = "#8E7CF8")
        )

        val expenses = listOf(
            Expense(
                id = "e1",
                walletId = "w-alex",
                categoryId = "c1",
                amount = 9999.0,
                expenseDate = "2026-09-10",
                categories = CategorySummaryRef("Rent", "🏠")
            ),
            Expense(
                id = "e2",
                walletId = "w-main",
                categoryId = "c1",
                amount = 1000.0,
                expenseDate = "2026-09-05",
                categories = CategorySummaryRef("Rent", "🏠")
            ),
            Expense(
                id = "e3",
                walletId = "w-cash",
                categoryId = "c2",
                amount = 500.0,
                expenseDate = "2026-09-12",
                categories = CategorySummaryRef("Grocery", "🛒")
            ),
            Expense(
                id = "e4",
                walletId = "w-main",
                categoryId = "c1",
                amount = 600.0,
                expenseDate = "2026-08-15",
                categories = CategorySummaryRef("Rent", "🏠")
            )
        )

        val income = listOf(
            Income(id = "i1", walletId = "w-alex", amount = 50000.0, incomeDate = "2026-09-01"),
            Income(id = "i2", walletId = "w-main", amount = 10000.0, incomeDate = "2026-09-01"),
            Income(id = "i3", walletId = "w-cash", amount = 2000.0, incomeDate = "2026-09-02")
        )

        val summary = FinanceMath.computeDashboardByWallet(
            wallets = wallets,
            allExpenses = expenses,
            allIncome = income,
            year = 2026,
            month = 9,
            timeframe = DashboardTimeframe.MONTHLY
        )

        // Alex excluded completely
        assertEquals(2, summary.walletsData.size)
        assertTrue(summary.walletsData.none { it.wallet.name.trim().equals("alex", ignoreCase = true) })
        assertEquals(12000.0, summary.totalIncome, 1e-6)
        assertEquals(2100.0, summary.totalExpenses, 1e-6)
        assertEquals(9900.0, summary.netBalance, 1e-6)

        // 6-month trend ending Sep 2026
        assertEquals(6, summary.trendData.size)
        assertEquals("Aug", summary.trendData[4].label)
        assertEquals(600.0, summary.trendData[4].total, 1e-6)
        assertEquals("Sep", summary.trendData[5].label)
        assertEquals(1500.0, summary.trendData[5].total, 1e-6)

        // Pace delta: (1500 - 600) / 600 = +150%
        val pace = FinanceMath.computePaceDelta(summary.trendData)
        assertEquals("+150%", pace.deltaText)
        assertEquals(PaceDeltaTone.UP, pace.deltaTone)
    }

    @Test
    fun projectRecurringForMonth_handlesMonthlyClampingAndIntervals() {
        val rules = listOf(
            RecurringTransaction(
                id = "r-monthly",
                type = "expense",
                amount = 12000.0,
                description = "Rent",
                walletId = "w1",
                frequencyType = "monthly",
                dayOfMonth = 31,
                isActive = true
            ),
            RecurringTransaction(
                id = "r-interval",
                type = "income",
                amount = 15000.0,
                description = "Biweekly Salary",
                walletId = "w1",
                frequencyType = "interval",
                startDate = "2026-08-01",
                intervalDays = 14,
                isActive = true
            ),
            RecurringTransaction(
                id = "r-paused",
                type = "expense",
                amount = 500.0,
                description = "Paused Gym",
                walletId = "w1",
                frequencyType = "monthly",
                dayOfMonth = 5,
                isActive = false
            )
        )

        // September 2026 has 30 days -> dayOfMonth 31 clamps to 2026-09-30
        val (projExp, projInc) = FinanceMath.projectRecurringForMonth(rules, year = 2026, month = 9)
        assertEquals(1, projExp.size)
        assertEquals("2026-09-30", projExp[0].expenseDate)
        assertTrue(projExp[0].isProjected)

        // Aug 1 + 14 = Aug 15, +14 = Aug 29, +14 = Sep 12, +14 = Sep 26
        assertEquals(2, projInc.size)
        assertEquals("2026-09-12", projInc[0].incomeDate)
        assertEquals("2026-09-26", projInc[1].incomeDate)
        assertTrue(projInc[0].isProjected)
    }

    @Test
    fun computeNextRecurringDate_monthlyAndIntervalWithLastPostedToday() {
        val today = LocalDate.of(2026, 9, 15)

        val monthlySameDayUnposted = RecurringTransaction(
            id = "r1",
            type = "expense",
            amount = 500.0,
            walletId = "w1",
            frequencyType = "monthly",
            dayOfMonth = 15,
            lastPostedDate = "2026-08-15"
        )
        assertEquals(LocalDate.of(2026, 9, 15), FinanceMath.computeNextRecurringDate(monthlySameDayUnposted, today))

        val monthlySameDayPosted = monthlySameDayUnposted.copy(lastPostedDate = "2026-09-15")
        assertEquals(LocalDate.of(2026, 10, 15), FinanceMath.computeNextRecurringDate(monthlySameDayPosted, today))

        val intervalRule = RecurringTransaction(
            id = "r2",
            type = "income",
            amount = 5000.0,
            walletId = "w1",
            frequencyType = "interval",
            startDate = "2026-09-01",
            intervalDays = 7,
            lastPostedDate = "2026-09-15"
        )
        // Sep 1 -> Sep 8 -> Sep 15 (already posted today) -> Sep 22
        assertEquals(LocalDate.of(2026, 9, 22), FinanceMath.computeNextRecurringDate(intervalRule, today))
    }

    @Test
    fun paymentMethodLabelFormatting_matchesWebParity() {
        assertEquals(
            "Credit • BPI Visa 4242",
            FinanceMath.formatPaymentMethodLabel("credit", "4242", bank = "BPI", network = "Visa")
        )
        assertEquals(
            "Credit • Mastercard 1234",
            FinanceMath.formatPaymentMethodLabel("credit", "1234", bank = "", network = "Mastercard")
        )
        assertEquals(
            "Debit (UnionBank)",
            FinanceMath.formatPaymentMethodLabel("debit", "UnionBank")
        )
        assertEquals(
            "GCash",
            FinanceMath.formatPaymentMethodLabel("cash", "GCash")
        )
    }

    @Test
    fun monthFloorAndDefaultCategories_matchSpec() {
        assertTrue(FinanceMath.isBeforeMonthFloor(2026, 7))
        assertFalse(FinanceMath.isBeforeMonthFloor(2026, 8))
        assertEquals(2026 to 8, FinanceMath.clampToMonthFloor(2025, 12))
        assertEquals(2026 to 10, FinanceMath.clampToMonthFloor(2026, 10))
        assertEquals(12, FinanceMath.DEFAULT_CATEGORIES.size)
        assertEquals("Rent", FinanceMath.DEFAULT_CATEGORIES.first().name)
        assertEquals("🏠", FinanceMath.DEFAULT_CATEGORIES.first().icon)
        assertEquals("Clothing", FinanceMath.DEFAULT_CATEGORIES.last().name)
        assertEquals("👕", FinanceMath.DEFAULT_CATEGORIES.last().icon)
    }

    @Test
    fun widgetSnapshot_jsonRoundtripAndDefaultPrivacyMasked() {
        val defaultSnap = WidgetSnapshot()
        assertTrue(defaultSnap.isPrivacyMasked)
        assertEquals("All Wallets", defaultSnap.displayedWalletName)

        val snapshot = WidgetSnapshot(
            isAuthenticated = true,
            isPrivacyMasked = true,
            selectedWalletIndex = 1,
            wallets = listOf(
                WidgetWalletBalance("w1", "BPI", "#28C991", totalIncome = 25000.0, totalExpenses = 5000.0),
                WidgetWalletBalance("w2", "Cash", "#8E7CF8", totalIncome = 3000.0, totalExpenses = 1000.0)
            ),
            totalNetBalance = 22000.0,
            totalIncome = 28000.0,
            totalExpenses = 6000.0,
            thisMonthSpent = 4200.0,
            updatedAtEpochMs = 1760000000000L,
            userEmail = "user@example.com"
        )

        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        val encoded = json.encodeToString(snapshot)
        val decoded = json.decodeFromString<WidgetSnapshot>(encoded)

        assertEquals(snapshot, decoded)
        assertNotNull(decoded.activeWallet)
        assertEquals("BPI", decoded.displayedWalletName)
        assertEquals(20000.0, decoded.displayedBalance, 1e-6)
        assertEquals(3, decoded.cycleCount)
    }

    @Test
    fun computePaceDelta_handlesDownSteadyAndActive() {
        val down = FinanceMath.computePaceDelta(listOf(TrendPoint("Aug", 1000.0), TrendPoint("Sep", 750.0)))
        assertEquals("-25%", down.deltaText)
        assertEquals(PaceDeltaTone.DOWN, down.deltaTone)

        val active = FinanceMath.computePaceDelta(listOf(TrendPoint("Aug", 0.0), TrendPoint("Sep", 400.0)))
        assertEquals("Active", active.deltaText)
        assertEquals(PaceDeltaTone.UP, active.deltaTone)

        val steady = FinanceMath.computePaceDelta(listOf(TrendPoint("Aug", 0.0), TrendPoint("Sep", 0.0)))
        assertEquals("Steady", steady.deltaText)
        assertEquals(PaceDeltaTone.NEUTRAL, steady.deltaTone)
    }
}
