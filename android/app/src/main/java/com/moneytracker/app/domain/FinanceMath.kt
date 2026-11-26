package com.moneytracker.app.domain

import com.moneytracker.app.data.model.CategoryAverage
import com.moneytracker.app.data.model.CategorySpend
import com.moneytracker.app.data.model.DashboardSummary
import com.moneytracker.app.data.model.DashboardTimeframe
import com.moneytracker.app.data.model.Expense
import com.moneytracker.app.data.model.ExpenseInsert
import com.moneytracker.app.data.model.Income
import com.moneytracker.app.data.model.PaceDeltaTone
import com.moneytracker.app.data.model.PaceSummary
import com.moneytracker.app.data.model.RecurringSummary
import com.moneytracker.app.data.model.RecurringTransaction
import com.moneytracker.app.data.model.TrendPoint
import com.moneytracker.app.data.model.Wallet
import com.moneytracker.app.data.model.WalletDashboardData
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class DefaultCategorySeed(
    val name: String,
    val icon: String,
    val sortOrder: Int
)

object FinanceMath {

    /** Month floor: August 2026 (1-based month = 8). */
    const val MIN_YEAR: Int = 2026
    const val MIN_MONTH: Int = 8

    val ISO_DATE: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val MONTH_SHORT_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM", Locale.US)
    private val YMD_PREFIX_REGEX = Regex("""^(\d{4})-(\d{2})-(\d{2})""")

    val WALLET_COLOR_PRESETS: List<Pair<String, String>> = listOf(
        "#28C991" to "Emerald",
        "#8E7CF8" to "Electric Violet",
        "#38BDF8" to "Sky Blue",
        "#C8F865" to "Chartreuse",
        "#F0B44A" to "Amber",
        "#F06478" to "Rose",
        "#14B8A6" to "Teal"
    )

    val DEFAULT_CATEGORIES: List<DefaultCategorySeed> = listOf(
        DefaultCategorySeed("Rent", "🏠", 1),
        DefaultCategorySeed("Restaurant", "🍔", 2),
        DefaultCategorySeed("Grocery", "🛒", 3),
        DefaultCategorySeed("Subscription", "🔄", 4),
        DefaultCategorySeed("Transportation", "🚌", 5),
        DefaultCategorySeed("Work/School", "💼", 6),
        DefaultCategorySeed("Bills", "🧾", 7),
        DefaultCategorySeed("Household Items", "🧻", 8),
        DefaultCategorySeed("Technology", "💻", 9),
        DefaultCategorySeed("Healthcare", "🏥", 10),
        DefaultCategorySeed("Service", "🛠️", 11),
        DefaultCategorySeed("Clothing", "👕", 12)
    )

    fun isBeforeMonthFloor(year: Int, month: Int): Boolean {
        return year < MIN_YEAR || (year == MIN_YEAR && month < MIN_MONTH)
    }

    fun clampToMonthFloor(year: Int, month: Int): Pair<Int, Int> {
        return if (isBeforeMonthFloor(year, month)) {
            MIN_YEAR to MIN_MONTH
        } else {
            year to month.coerceIn(1, 12)
        }
    }

    /** Dashboard & Widget exclude any wallet named "Alex" (case-insensitive). */
    fun isExcludedFromDashboard(walletName: String): Boolean {
        return walletName.trim().equals("alex", ignoreCase = true)
    }

    fun formatPhpCurrency(
        amount: Double,
        compact: Boolean = false,
        masked: Boolean = false
    ): String {
        if (masked) return "••••••"
        val safeVal = if (amount.isFinite()) amount else 0.0
        val sign = if (safeVal < 0.0) "-" else ""
        val absVal = abs(safeVal)

        if (compact) {
            return when {
                absVal >= 1_000_000_000.0 -> "${sign}₱${formatCompactDecimal(absVal / 1_000_000_000.0)}B"
                absVal >= 1_000_000.0 -> "${sign}₱${formatCompactDecimal(absVal / 1_000_000.0)}M"
                absVal >= 1_000.0 -> "${sign}₱${formatCompactDecimal(absVal / 1_000.0)}K"
                else -> {
                    val fmt = DecimalFormat("#,##0.#", DecimalFormatSymbols(Locale.US))
                    "${sign}₱${fmt.format(absVal)}"
                }
            }
        }

        val fmt = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))
        return "${sign}₱${fmt.format(absVal)}"
    }

    private fun formatCompactDecimal(value: Double): String {
        val rounded = (value * 10.0).roundToInt() / 10.0
        val fmt = DecimalFormat("0.#", DecimalFormatSymbols(Locale.US))
        return fmt.format(rounded)
    }

    fun parseLocalDate(dateStr: String?): LocalDate? {
        if (dateStr.isNullOrBlank()) return null
        val trimmed = dateStr.trim()
        val match = YMD_PREFIX_REGEX.find(trimmed) ?: return null
        val year = match.groupValues[1].toIntOrNull() ?: return null
        val month = match.groupValues[2].toIntOrNull() ?: return null
        val day = match.groupValues[3].toIntOrNull() ?: return null
        return runCatching { LocalDate.of(year, month, day) }.getOrNull()
    }

    fun formatDateDisplay(dateStr: String?, pattern: String = "MMM dd, yyyy"): String {
        val date = parseLocalDate(dateStr) ?: return ""
        return date.format(DateTimeFormatter.ofPattern(pattern, Locale.US))
    }

    fun getOrdinalDay(day: Int): String {
        val num = max(1, day)
        val mod100 = num % 100
        if (mod100 in 11..13) return "${num}th"
        return when (num % 10) {
            1 -> "${num}st"
            2 -> "${num}nd"
            3 -> "${num}rd"
            else -> "${num}th"
        }
    }

    /**
     * Formats payment method labels identically to web Settings.jsx.
     */
    fun formatPaymentMethodLabel(
        type: String,
        details: String,
        bank: String = "",
        network: String = "Visa"
    ): String {
        val trimmedDetails = details.trim()
        return when (type.lowercase(Locale.US)) {
            "credit" -> {
                val bankPart = bank.trim().let { if (it.isNotEmpty()) "$it " else "" }
                "Credit • $bankPart${network.trim()} $trimmedDetails".trim()
            }
            "debit" -> "Debit ($trimmedDetails)"
            else -> trimmedDetails
        }
    }

    /**
     * Splits a single expense payload into N monthly installment rows (N in 2..60).
     * If months <= 1, returns the unmodified base payload in a single-element list.
     */
    fun buildInstallmentBatch(base: ExpenseInsert, months: Int): List<ExpenseInsert> {
        if (months <= 1) return listOf(base)
        val count = months.coerceIn(2, 60)
        val startDate = parseLocalDate(base.expenseDate) ?: LocalDate.now()
        val baseDesc = base.description?.trim().orEmpty().ifBlank { "Expense" }

        return List(count) { index ->
            val installmentDate = startDate.plusMonths(index.toLong()).format(ISO_DATE)
            base.copy(
                description = "$baseDesc (Installment ${index + 1}/$count)",
                expenseDate = installmentDate
            )
        }
    }

    /**
     * Safe arithmetic expression evaluator for Quick Log / Expense / Income calculator keypad.
     * Supports +, -, *, /, ×, ÷, parentheses, unary signs, decimals, and postfix %:
     * - In `a + b%` or `a - b%`, `b%` is `a * (b / 100.0)`
     * - In `a * b%`, `a / b%`, or standalone `b%`, `b%` is `b / 100.0`
     * Returns null on syntax error, empty input, division by zero, or non-finite result.
     */
    fun evaluateCalcExpression(expression: String): Double? {
        val normalized = expression
            .replace('×', '*')
            .replace('÷', '/')
            .replace('−', '-')
            .replace(",", "")
            .trim()
        if (normalized.isEmpty()) return null

        val parser = CalcParser(normalized)
        val value = parser.parseExpression() ?: return null
        parser.skipWhitespace()
        if (!parser.isAtEnd()) return null
        if (!value.isFinite()) return null
        return if (value == -0.0) 0.0 else value
    }

    private data class TermResult(val value: Double, val wasPercent: Boolean)

    private class CalcParser(private val input: String) {
        private var pos = 0

        fun isAtEnd(): Boolean = pos >= input.length

        fun skipWhitespace() {
            while (pos < input.length && input[pos].isWhitespace()) pos++
        }

        fun parseExpression(): Double? {
            var left = parseTerm()?.value ?: return null
            while (true) {
                skipWhitespace()
                if (isAtEnd()) break
                val op = input[pos]
                if (op != '+' && op != '-') break
                pos++
                val right = parseTerm() ?: return null
                val rhsValue = if (right.wasPercent) left * right.value else right.value
                left = if (op == '+') left + rhsValue else left - rhsValue
            }
            return left
        }

        private fun parseTerm(): TermResult? {
            val first = parseUnary() ?: return null
            var left = first.value
            var isPurePercent = first.wasPercent

            while (true) {
                skipWhitespace()
                if (isAtEnd()) break
                val op = input[pos]
                if (op != '*' && op != '/') break
                pos++
                val right = parseUnary() ?: return null
                isPurePercent = false
                if (op == '*') {
                    left *= right.value
                } else {
                    if (right.value == 0.0) return null
                    left /= right.value
                }
            }
            return TermResult(left, isPurePercent)
        }

        private fun parseUnary(): TermResult? {
            skipWhitespace()
            if (isAtEnd()) return null
            return when (input[pos]) {
                '+' -> {
                    pos++
                    parseUnary()
                }
                '-' -> {
                    pos++
                    val inner = parseUnary() ?: return null
                    TermResult(-inner.value, inner.wasPercent)
                }
                else -> parsePrimaryWithPostfix()
            }
        }

        private fun parsePrimaryWithPostfix(): TermResult? {
            skipWhitespace()
            if (isAtEnd()) return null
            var value = if (input[pos] == '(') {
                pos++
                val inside = parseExpression() ?: return null
                skipWhitespace()
                if (isAtEnd() || input[pos] != ')') return null
                pos++
                inside
            } else {
                parseNumber() ?: return null
            }

            var hasPercent = false
            while (true) {
                skipWhitespace()
                if (!isAtEnd() && input[pos] == '%') {
                    pos++
                    value /= 100.0
                    hasPercent = true
                } else {
                    break
                }
            }
            return TermResult(value, hasPercent)
        }

        private fun parseNumber(): Double? {
            skipWhitespace()
            val start = pos
            var hasDigit = false
            var hasDot = false
            while (pos < input.length) {
                val c = input[pos]
                when {
                    c.isDigit() -> {
                        hasDigit = true
                        pos++
                    }
                    c == '.' && !hasDot -> {
                        hasDot = true
                        pos++
                    }
                    else -> break
                }
            }
            if (!hasDigit) return null
            return input.substring(start, pos).toDoubleOrNull()
        }
    }

    /**
     * Projects active recurring rules onto a target calendar month (1-based month).
     * Exact parity with web Calendar.jsx.
     */
    fun projectRecurringForMonth(
        recurring: List<RecurringTransaction>,
        year: Int,
        month: Int,
        walletId: String? = null
    ): Pair<List<Expense>, List<Income>> {
        val ym = YearMonth.of(year, month.coerceIn(1, 12))
        val daysInMonth = ym.lengthOfMonth()
        val monthStart = ym.atDay(1)
        val projectedExpenses = mutableListOf<Expense>()
        val projectedIncome = mutableListOf<Income>()

        recurring
            .filter { it.isActive && (walletId == null || it.walletId == walletId) }
            .forEach { rule ->
                val occurrences = mutableListOf<LocalDate>()
                val freq = rule.frequencyType.lowercase(Locale.US)

                if (freq == "interval") {
                    val startDate = parseLocalDate(rule.startDate)
                    val intervalDays = rule.intervalDays ?: 0
                    if (startDate != null && intervalDays > 0) {
                        val step = max(1, intervalDays).toLong()
                        var cursor: LocalDate = startDate
                        var safety = 0
                        while (cursor.isBefore(monthStart) && safety < 20000) {
                            cursor = cursor.plusDays(step)
                            safety++
                        }
                        while (cursor.year == year && cursor.monthValue == month && safety < 20000) {
                            occurrences.add(cursor)
                            cursor = cursor.plusDays(step)
                            safety++
                        }
                    }
                } else {
                    val day = (rule.dayOfMonth ?: 1).coerceIn(1, daysInMonth)
                    occurrences.add(LocalDate.of(year, month, day))
                }

                occurrences.forEachIndexed { idx, occDate ->
                    val dateStr = occDate.format(ISO_DATE)
                    if (rule.type.equals("income", ignoreCase = true)) {
                        projectedIncome.add(
                            Income(
                                id = "proj-${rule.id}-$idx",
                                userId = rule.userId,
                                walletId = rule.walletId,
                                amount = rule.amount,
                                description = rule.description,
                                incomeDate = dateStr,
                                wallets = rule.wallets,
                                isProjected = true
                            )
                        )
                    } else {
                        projectedExpenses.add(
                            Expense(
                                id = "proj-${rule.id}-$idx",
                                userId = rule.userId,
                                walletId = rule.walletId,
                                categoryId = rule.categoryId,
                                paymentMethodId = rule.paymentMethodId,
                                amount = rule.amount,
                                description = rule.description,
                                expenseDate = dateStr,
                                isRecurring = true,
                                recurringId = rule.id,
                                wallets = rule.wallets,
                                categories = rule.categories,
                                paymentMethods = rule.paymentMethods,
                                isProjected = true
                            )
                        )
                    }
                }
            }

        return projectedExpenses to projectedIncome
    }

    /**
     * Computes the next due date for a recurring transaction on or after [today].
     * Exact parity with web Dashboard.jsx.
     */
    fun computeNextRecurringDate(
        item: RecurringTransaction,
        today: LocalDate = LocalDate.now()
    ): LocalDate {
        val todayYmd = today.format(ISO_DATE)

        if (item.frequencyType.equals("interval", ignoreCase = true) &&
            !item.startDate.isNullOrBlank() &&
            (item.intervalDays ?: 0) > 0
        ) {
            val parsedStart = parseLocalDate(item.startDate)
            if (parsedStart != null) {
                var cursor: LocalDate = parsedStart
                val step = max(1, item.intervalDays ?: 1).toLong()
                var safety = 0
                while ((cursor.isBefore(today) || (cursor == today && item.lastPostedDate == todayYmd)) &&
                    safety < 20000
                ) {
                    cursor = cursor.plusDays(step)
                    safety++
                }
                return cursor
            }
        }

        val dom = max(1, item.dayOfMonth ?: 1)
        val curMonth = YearMonth.of(today.year, today.monthValue)
        var candidate = curMonth.atDay(min(dom, curMonth.lengthOfMonth()))

        if (candidate.isBefore(today) || (candidate == today && item.lastPostedDate == todayYmd)) {
            val nextMonth = curMonth.plusMonths(1)
            candidate = nextMonth.atDay(min(dom, nextMonth.lengthOfMonth()))
        }
        return candidate
    }

    fun computeRecurringSummary(
        items: List<RecurringTransaction>,
        today: LocalDate = LocalDate.now(),
        walletIdFilter: String? = null,
        visibleWalletIds: Set<String>? = null
    ): RecurringSummary {
        var monthlyIncome = 0.0
        var monthlyExpenses = 0.0
        var activeCount = 0
        var hasInterval = false

        val scopedItems = items.filter { item ->
            when {
                walletIdFilter != null && walletIdFilter != "all" -> item.walletId == walletIdFilter
                visibleWalletIds != null && visibleWalletIds.isNotEmpty() -> visibleWalletIds.contains(item.walletId)
                else -> true
            }
        }

        val activeWithNext = mutableListOf<Pair<RecurringTransaction, LocalDate>>()

        scopedItems.forEach { item ->
            if (!item.isActive) return@forEach
            activeCount++
            val isInterval = item.frequencyType.equals("interval", ignoreCase = true) &&
                (item.intervalDays ?: 0) > 0
            if (isInterval) hasInterval = true

            val normalized = if (isInterval) {
                item.amount * (30.0 / (item.intervalDays ?: 30))
            } else {
                item.amount
            }

            if (item.type.equals("income", ignoreCase = true)) {
                monthlyIncome += normalized
            } else {
                monthlyExpenses += normalized
            }

            activeWithNext.add(item to computeNextRecurringDate(item, today))
        }

        val nextPair = activeWithNext.minByOrNull { it.second }
        return RecurringSummary(
            activeCount = activeCount,
            totalCount = scopedItems.size,
            monthlyIncome = monthlyIncome,
            monthlyExpenses = monthlyExpenses,
            hasInterval = hasInterval,
            nextItem = nextPair?.first,
            nextDueDate = nextPair?.second?.format(ISO_DATE)
        )
    }

    /**
     * Computes full per-wallet and aggregated Dashboard metrics, excluding any wallet named "Alex".
     * Exact parity with web useDashboard.js + Dashboard.jsx.
     */
    fun computeDashboardByWallet(
        wallets: List<Wallet>,
        allExpenses: List<Expense>,
        allIncome: List<Income>,
        year: Int,
        month: Int,
        timeframe: DashboardTimeframe = DashboardTimeframe.MONTHLY,
        selectedWalletId: String? = null,
        excludedCategoryIds: Set<String> = emptySet()
    ): DashboardSummary {
        val safeMonth = month.coerceIn(1, 12)
        val visibleWallets = wallets
            .filterNot { isExcludedFromDashboard(it.name) }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })

        val visibleIds = visibleWallets.map { it.id }.toSet()

        val periodStart: LocalDate
        val periodEnd: LocalDate
        if (timeframe == DashboardTimeframe.MONTHLY) {
            val ym = YearMonth.of(year, safeMonth)
            periodStart = ym.atDay(1)
            periodEnd = ym.atEndOfMonth()
        } else {
            periodStart = LocalDate.of(year, 1, 1)
            periodEnd = LocalDate.of(year, 12, 31)
        }

        // Pre-parse expense dates once
        val parsedExpenses = allExpenses
            .filter { visibleIds.contains(it.walletId) }
            .mapNotNull { exp ->
                val date = parseLocalDate(exp.expenseDate) ?: return@mapNotNull null
                exp to date
            }

        val visibleIncome = allIncome.filter { visibleIds.contains(it.walletId) }

        val trendWindows: List<Pair<String, YearMonth>> = if (timeframe == DashboardTimeframe.MONTHLY) {
            val anchor = YearMonth.of(year, safeMonth)
            (5 downTo 0).map { offset ->
                val ym = anchor.minusMonths(offset.toLong())
                ym.atDay(1).format(MONTH_SHORT_FMT) to ym
            }
        } else {
            (1..12).map { m ->
                val ym = YearMonth.of(year, m)
                ym.atDay(1).format(MONTH_SHORT_FMT) to ym
            }
        }

        val avgStart: LocalDate
        val avgEndExclusive: LocalDate
        val avgDivisor: Double
        if (timeframe == DashboardTimeframe.MONTHLY) {
            avgStart = periodStart.minusMonths(3)
            avgEndExclusive = periodStart
            avgDivisor = 3.0
        } else {
            avgStart = periodStart
            avgEndExclusive = periodEnd.plusDays(1)
            avgDivisor = 12.0
        }

        val walletsData = visibleWallets.map { wallet ->
            val walletExps = parsedExpenses.filter { it.first.walletId == wallet.id }
            val walletInc = visibleIncome.filter { it.walletId == wallet.id }

            val totalIncome = walletInc.sumOf { it.amount }
            val totalExpenses = walletExps.sumOf { it.first.amount }

            // Period category breakdown
            val catMap = linkedMapOf<String, CategorySpend>()
            walletExps.forEach { (exp, date) ->
                if (!date.isBefore(periodStart) && !date.isAfter(periodEnd)) {
                    val key = exp.categoryId ?: "__unknown__"
                    val existing = catMap[key]
                    if (existing == null) {
                        catMap[key] = CategorySpend(
                            categoryId = exp.categoryId,
                            name = exp.categories?.name ?: "Unknown",
                            icon = exp.categories?.icon ?: "❓",
                            totalAmount = exp.amount
                        )
                    } else {
                        catMap[key] = existing.copy(totalAmount = existing.totalAmount + exp.amount)
                    }
                }
            }
            val categorySpends = catMap.values.sortedByDescending { it.totalAmount }

            // Trend data
            val trendData = trendWindows.map { (label, ym) ->
                val total = walletExps.sumOf { (exp, date) ->
                    if (date.year == ym.year && date.monthValue == ym.monthValue) exp.amount else 0.0
                }
                TrendPoint(label = label, total = total)
            }

            // Averages top 5
            val avgMap = linkedMapOf<String, CategoryAverage>()
            walletExps.forEach { (exp, date) ->
                if (!date.isBefore(avgStart) && date.isBefore(avgEndExclusive)) {
                    val key = exp.categoryId ?: "__unknown__"
                    val existing = avgMap[key]
                    val contribution = exp.amount / avgDivisor
                    if (existing == null) {
                        avgMap[key] = CategoryAverage(
                            categoryId = exp.categoryId,
                            categoryName = exp.categories?.name ?: "Unknown",
                            amount = contribution
                        )
                    } else {
                        avgMap[key] = existing.copy(amount = existing.amount + contribution)
                    }
                }
            }
            val topAverages = avgMap.values.sortedByDescending { it.amount }.take(5)

            WalletDashboardData(
                wallet = wallet,
                expenses = categorySpends,
                trendData = trendData,
                averages = topAverages,
                totalIncome = totalIncome,
                totalExpenses = totalExpenses
            )
        }

        val allTotalIncome = walletsData.sumOf { it.totalIncome }
        val allTotalExpenses = walletsData.sumOf { it.totalExpenses }

        val activeWalletEntry = if (selectedWalletId.isNullOrBlank() || selectedWalletId == "all") {
            null
        } else {
            walletsData.find { it.wallet.id == selectedWalletId }
        }

        if (activeWalletEntry != null) {
            val filteredCats = activeWalletEntry.expenses.filterNot {
                it.categoryId != null && excludedCategoryIds.contains(it.categoryId)
            }
            return DashboardSummary(
                walletsData = walletsData,
                activeWalletName = activeWalletEntry.wallet.name,
                totalIncome = activeWalletEntry.totalIncome,
                totalExpenses = activeWalletEntry.totalExpenses,
                allTotalIncome = allTotalIncome,
                allTotalExpenses = allTotalExpenses,
                rawCategorySpends = activeWalletEntry.expenses,
                filteredCategorySpends = filteredCats,
                trendData = activeWalletEntry.trendData,
                averages = activeWalletEntry.averages
            )
        }

        // Combined across all non-Alex wallets
        val combinedCatMap = linkedMapOf<String, CategorySpend>()
        walletsData.forEach { wData ->
            wData.expenses.forEach { spend ->
                val key = spend.categoryId ?: spend.name
                val existing = combinedCatMap[key]
                if (existing == null) {
                    combinedCatMap[key] = spend
                } else {
                    combinedCatMap[key] = existing.copy(totalAmount = existing.totalAmount + spend.totalAmount)
                }
            }
        }
        val combinedSpends = combinedCatMap.values.sortedByDescending { it.totalAmount }
        val filteredCombinedSpends = combinedSpends.filterNot {
            it.categoryId != null && excludedCategoryIds.contains(it.categoryId)
        }

        val templateTrend = walletsData.firstOrNull()?.trendData ?: trendWindows.map { TrendPoint(it.first, 0.0) }
        val combinedTrend = templateTrend.mapIndexed { idx, point ->
            TrendPoint(
                label = point.label,
                total = walletsData.sumOf { it.trendData.getOrNull(idx)?.total ?: 0.0 }
            )
        }

        val combinedAvgMap = linkedMapOf<String, CategoryAverage>()
        walletsData.forEach { wData ->
            wData.averages.forEach { avg ->
                val key = avg.categoryId ?: avg.categoryName
                val existing = combinedAvgMap[key]
                if (existing == null) {
                    combinedAvgMap[key] = avg
                } else {
                    combinedAvgMap[key] = existing.copy(amount = existing.amount + avg.amount)
                }
            }
        }
        val combinedAverages = combinedAvgMap.values.sortedByDescending { it.amount }.take(5)

        return DashboardSummary(
            walletsData = walletsData,
            activeWalletName = "All Wallets",
            totalIncome = allTotalIncome,
            totalExpenses = allTotalExpenses,
            allTotalIncome = allTotalIncome,
            allTotalExpenses = allTotalExpenses,
            rawCategorySpends = combinedSpends,
            filteredCategorySpends = filteredCombinedSpends,
            trendData = combinedTrend,
            averages = combinedAverages
        )
    }

    /**
     * Computes spending pace delta between latest and previous trend buckets.
     * Exact parity with web Dashboard.jsx.
     */
    fun computePaceDelta(trendData: List<TrendPoint>): PaceSummary {
        val latest = trendData.lastOrNull()?.total ?: 0.0
        val prev = if (trendData.size >= 2) trendData[trendData.size - 2].total else 0.0

        var deltaPct: Int? = null
        var deltaText = "Steady"
        var deltaTone = PaceDeltaTone.NEUTRAL

        if (prev > 0.0) {
            val pct = (((latest - prev) / prev) * 100.0).roundToInt()
            deltaPct = pct
            when {
                pct > 0 -> {
                    deltaText = "+$pct%"
                    deltaTone = PaceDeltaTone.UP
                }
                pct < 0 -> {
                    deltaText = "$pct%"
                    deltaTone = PaceDeltaTone.DOWN
                }
            }
        } else if (latest > 0.0) {
            deltaText = "Active"
            deltaTone = PaceDeltaTone.UP
        }

        return PaceSummary(
            latest = latest,
            previous = prev,
            deltaPercent = deltaPct,
            deltaText = deltaText,
            deltaTone = deltaTone
        )
    }
}
