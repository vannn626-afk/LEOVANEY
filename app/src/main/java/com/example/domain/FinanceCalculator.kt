package com.example.domain

import com.example.data.local.AccountEntity
import com.example.data.local.AssetEntity
import com.example.data.local.BudgetEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.DateFilterPeriod
import com.example.data.local.DebtEntity
import com.example.data.local.DebtType
import com.example.data.local.LiabilityEntity
import com.example.data.local.RecurrenceFrequency
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class AccountBalanceSummary(
    val account: AccountEntity,
    val totalIncome: Long,
    val totalExpense: Long,
    val transferIn: Long,
    val transferOut: Long,
    val currentBalance: Long
)

data class BudgetStatusSummary(
    val budget: BudgetEntity,
    val categoryName: String,
    val limitAmount: Long,
    val spentAmount: Long,
    val remainingAmount: Long,
    val usagePercentage: Int,
    val isNearLimit: Boolean,
    val isExceeded: Boolean
)

data class CategoryBreakdownItem(
    val categoryId: Long?,
    val categoryName: String,
    val totalAmount: Long,
    val percentage: Float,
    val transactionCount: Int
)

data class DailyBalancePoint(
    val dateLabel: String,
    val timestamp: Long,
    val income: Long,
    val expense: Long,
    val cumulativeBalance: Long
)

data class MonthlyComparisonPoint(
    val yearMonth: String,
    val label: String,
    val income: Long,
    val expense: Long,
    val netCashFlow: Long
)

data class NetWorthBreakdown(
    val activeAccountsTotal: Long,
    val manualAssetsTotal: Long,
    val activeReceivablesTotal: Long,
    val totalAssets: Long,
    val manualLiabilitiesTotal: Long,
    val activeDebtsTotal: Long,
    val totalLiabilities: Long,
    val netWorth: Long
)

/**
 * Single source of truth for all LEOVANEY financial calculations and formatting.
 * Uses Long arithmetic in IDR to avoid floating-point inaccuracies.
 */
object FinanceCalculator {

    private val idrSymbols = DecimalFormatSymbols(Locale("id", "ID")).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }

    fun formatRupiah(amount: Long, currencyCode: String = "IDR", includePlusSign: Boolean = false): String {
        val prefix = when (currencyCode.uppercase()) {
            "IDR" -> "Rp"
            "USD" -> "$"
            else -> "$currencyCode "
        }
        val absValue = kotlin.math.abs(amount)
        val formatter = DecimalFormat("#,###", idrSymbols)
        val formattedNumber = formatter.format(absValue)
        return when {
            amount < 0L -> "-$prefix$formattedNumber"
            amount > 0L && includePlusSign -> "+$prefix$formattedNumber"
            else -> "$prefix$formattedNumber"
        }
    }

    fun formatDate(timestamp: Long, pattern: String = "dd MMM yyyy"): String {
        val sdf = SimpleDateFormat(pattern, Locale("id", "ID"))
        return sdf.format(Date(timestamp))
    }

    fun formatDateTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
        return sdf.format(Date(timestamp))
    }

    fun currentYearMonth(nowMillis: Long = System.currentTimeMillis()): String {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        return sdf.format(Date(nowMillis))
    }

    fun getPeriodRange(
        period: DateFilterPeriod,
        nowMillis: Long = System.currentTimeMillis(),
        customStart: Long? = null,
        customEnd: Long? = null
    ): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.timeInMillis = nowMillis

        fun startOfDay(c: Calendar): Long {
            c.set(Calendar.HOUR_OF_DAY, 0)
            c.set(Calendar.MINUTE, 0)
            c.set(Calendar.SECOND, 0)
            c.set(Calendar.MILLISECOND, 0)
            return c.timeInMillis
        }

        fun endOfDay(c: Calendar): Long {
            c.set(Calendar.HOUR_OF_DAY, 23)
            c.set(Calendar.MINUTE, 59)
            c.set(Calendar.SECOND, 59)
            c.set(Calendar.MILLISECOND, 999)
            return c.timeInMillis
        }

        return when (period) {
            DateFilterPeriod.TODAY -> {
                val start = startOfDay(cal.clone() as Calendar)
                val end = endOfDay(cal.clone() as Calendar)
                start to end
            }
            DateFilterPeriod.LAST_7_DAYS -> {
                val end = endOfDay(cal.clone() as Calendar)
                val startCal = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -6) }
                val start = startOfDay(startCal)
                start to end
            }
            DateFilterPeriod.THIS_MONTH -> {
                val startCal = (cal.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
                val endCal = (cal.clone() as Calendar).apply {
                    set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                }
                startOfDay(startCal) to endOfDay(endCal)
            }
            DateFilterPeriod.LAST_MONTH -> {
                val startCal = (cal.clone() as Calendar).apply {
                    add(Calendar.MONTH, -1)
                    set(Calendar.DAY_OF_MONTH, 1)
                }
                val endCal = (startCal.clone() as Calendar).apply {
                    set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                }
                startOfDay(startCal) to endOfDay(endCal)
            }
            DateFilterPeriod.THIS_YEAR -> {
                val startCal = (cal.clone() as Calendar).apply {
                    set(Calendar.MONTH, Calendar.JANUARY)
                    set(Calendar.DAY_OF_MONTH, 1)
                }
                val endCal = (cal.clone() as Calendar).apply {
                    set(Calendar.MONTH, Calendar.DECEMBER)
                    set(Calendar.DAY_OF_MONTH, 31)
                }
                startOfDay(startCal) to endOfDay(endCal)
            }
            DateFilterPeriod.CUSTOM -> {
                val s = customStart ?: startOfDay((cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -30) })
                val e = customEnd ?: endOfDay(cal.clone() as Calendar)
                if (s <= e) s to e else e to s
            }
        }
    }

    /**
     * Calculates exact current balance for every account:
     * initialBalance + INCOME - EXPENSE - TRANSFER_OUT + TRANSFER_IN
     */
    fun calculateAccountBalances(
        accounts: List<AccountEntity>,
        allTransactions: List<TransactionEntity>
    ): List<AccountBalanceSummary> {
        return accounts.map { account ->
            var income = 0L
            var expense = 0L
            var transferIn = 0L
            var transferOut = 0L

            for (tx in allTransactions) {
                when (tx.type) {
                    TransactionType.INCOME -> {
                        if (tx.accountId == account.id) income += tx.amount
                    }
                    TransactionType.EXPENSE -> {
                        if (tx.accountId == account.id) expense += tx.amount
                    }
                    TransactionType.TRANSFER -> {
                        if (tx.accountId == account.id) transferOut += tx.amount
                        if (tx.destinationAccountId == account.id) transferIn += tx.amount
                    }
                }
            }

            val currentBalance = account.initialBalance + income - expense - transferOut + transferIn
            AccountBalanceSummary(
                account = account,
                totalIncome = income,
                totalExpense = expense,
                transferIn = transferIn,
                transferOut = transferOut,
                currentBalance = currentBalance
            )
        }
    }

    /**
     * Calculates period income, expense, and net cash flow.
     * Transfers are strictly excluded from overall income and expense!
     */
    fun calculatePeriodCashFlow(
        transactionsInPeriod: List<TransactionEntity>
    ): Triple<Long, Long, Long> {
        var income = 0L
        var expense = 0L
        for (tx in transactionsInPeriod) {
            when (tx.type) {
                TransactionType.INCOME -> income += tx.amount
                TransactionType.EXPENSE -> expense += tx.amount
                TransactionType.TRANSFER -> Unit // Transfers are internal movements, never income/expense
            }
        }
        val netCashFlow = income - expense
        return Triple(income, expense, netCashFlow)
    }

    /**
     * Calculates Net Worth without double-counting account balances.
     * Total Assets = Active Account Balances + Manual Assets + Unsettled Receivables (Piutang).
     * Total Liabilities = Manual Liabilities + Unsettled Debts (Utang).
     * Net Worth = Total Assets - Total Liabilities.
     */
    fun calculateNetWorth(
        accountSummaries: List<AccountBalanceSummary>,
        assets: List<AssetEntity>,
        liabilities: List<LiabilityEntity>,
        debts: List<DebtEntity>
    ): NetWorthBreakdown {
        val activeAccountsTotal = accountSummaries
            .filter { !it.account.isArchived }
            .sumOf { it.currentBalance }

        val manualAssetsTotal = assets.sumOf { it.value }
        val activeReceivablesTotal = debts
            .filter { it.type == DebtType.RECEIVABLE && !it.isSettled }
            .sumOf { it.remainingPrincipal }

        val totalAssets = activeAccountsTotal + manualAssetsTotal + activeReceivablesTotal

        val manualLiabilitiesTotal = liabilities.sumOf { it.amount }
        val activeDebtsTotal = debts
            .filter { it.type == DebtType.DEBT && !it.isSettled }
            .sumOf { it.remainingPrincipal }

        val totalLiabilities = manualLiabilitiesTotal + activeDebtsTotal
        val netWorth = totalAssets - totalLiabilities

        return NetWorthBreakdown(
            activeAccountsTotal = activeAccountsTotal,
            manualAssetsTotal = manualAssetsTotal,
            activeReceivablesTotal = activeReceivablesTotal,
            totalAssets = totalAssets,
            manualLiabilitiesTotal = manualLiabilitiesTotal,
            activeDebtsTotal = activeDebtsTotal,
            totalLiabilities = totalLiabilities,
            netWorth = netWorth
        )
    }

    /**
     * Calculates budget usage for a specific YYYY-MM month.
     * Only EXPENSE transactions in that YYYY-MM count toward budgets (TRANSFER is excluded).
     */
    fun calculateBudgetSummaries(
        budgets: List<BudgetEntity>,
        categories: List<CategoryEntity>,
        allTransactions: List<TransactionEntity>,
        targetYearMonth: String
    ): List<BudgetStatusSummary> {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        val monthExpenses = allTransactions.filter { tx ->
            tx.type == TransactionType.EXPENSE && sdf.format(Date(tx.timestamp)) == targetYearMonth
        }
        val categoryMap = categories.associateBy { it.id }

        return budgets
            .filter { it.yearMonth == targetYearMonth }
            .map { budget ->
                val spent = if (budget.categoryId == null) {
                    monthExpenses.sumOf { it.amount }
                } else {
                    monthExpenses.filter { it.categoryId == budget.categoryId }.sumOf { it.amount }
                }
                val remaining = budget.amountLimit - spent
                val percentage = if (budget.amountLimit > 0L) {
                    ((spent * 100L) / budget.amountLimit).toInt()
                } else {
                    0
                }
                val isExceeded = spent > budget.amountLimit
                val isNearLimit = !isExceeded && percentage >= budget.alertThresholdPercent

                val catName = if (budget.categoryId == null) {
                    "Total Anggaran Bulanan"
                } else {
                    categoryMap[budget.categoryId]?.name ?: "Kategori #${budget.categoryId}"
                }

                BudgetStatusSummary(
                    budget = budget,
                    categoryName = catName,
                    limitAmount = budget.amountLimit,
                    spentAmount = spent,
                    remainingAmount = remaining,
                    usagePercentage = percentage,
                    isNearLimit = isNearLimit,
                    isExceeded = isExceeded
                )
            }
            .sortedWith(compareBy<BudgetStatusSummary> { it.budget.categoryId != null }.thenByDescending { it.usagePercentage })
    }

    fun calculateCategoryBreakdown(
        transactions: List<TransactionEntity>,
        categories: List<CategoryEntity>,
        type: TransactionType
    ): List<CategoryBreakdownItem> {
        val filtered = transactions.filter { it.type == type }
        val total = filtered.sumOf { it.amount }
        if (total <= 0L) return emptyList()

        val categoryMap = categories.associateBy { it.id }
        return filtered
            .groupBy { it.categoryId }
            .map { (catId, txs) ->
                val amount = txs.sumOf { it.amount }
                val catName = if (catId == null) {
                    "Tanpa Kategori"
                } else {
                    categoryMap[catId]?.name ?: "Lainnya"
                }
                CategoryBreakdownItem(
                    categoryId = catId,
                    categoryName = catName,
                    totalAmount = amount,
                    percentage = (amount.toFloat() / total.toFloat()) * 100f,
                    transactionCount = txs.size
                )
            }
            .sortedByDescending { it.totalAmount }
    }

    fun calculateBalanceTrend(
        accounts: List<AccountEntity>,
        allTransactions: List<TransactionEntity>,
        periodStart: Long,
        periodEnd: Long
    ): List<DailyBalancePoint> {
        val initialActiveBalance = accounts.filter { !it.isArchived }.sumOf { it.initialBalance }
        val activeAccountIds = accounts.filter { !it.isArchived }.map { it.id }.toSet()

        fun netDeltaForActiveAccounts(tx: TransactionEntity): Long {
            return when (tx.type) {
                TransactionType.INCOME -> if (tx.accountId in activeAccountIds) tx.amount else 0L
                TransactionType.EXPENSE -> if (tx.accountId in activeAccountIds) -tx.amount else 0L
                TransactionType.TRANSFER -> {
                    val outDelta = if (tx.accountId in activeAccountIds) -tx.amount else 0L
                    val inDelta = if (tx.destinationAccountId in activeAccountIds) tx.amount else 0L
                    outDelta + inDelta
                }
            }
        }

        val balanceBeforePeriod = initialActiveBalance + allTransactions
            .filter { it.timestamp < periodStart }
            .sumOf { netDeltaForActiveAccounts(it) }

        val periodTransactions = allTransactions
            .filter { it.timestamp in periodStart..periodEnd }
            .sortedBy { it.timestamp }

        val dayFormatter = SimpleDateFormat("dd MMM", Locale("id", "ID"))
        val groupedByDay = periodTransactions.groupBy { dayFormatter.format(Date(it.timestamp)) }

        if (groupedByDay.isEmpty()) {
            return listOf(
                DailyBalancePoint(
                    dateLabel = dayFormatter.format(Date(periodStart)),
                    timestamp = periodStart,
                    income = 0L,
                    expense = 0L,
                    cumulativeBalance = balanceBeforePeriod
                )
            )
        }

        var runningBalance = balanceBeforePeriod
        val points = mutableListOf<DailyBalancePoint>()
        for ((dayLabel, txs) in groupedByDay) {
            val dayIncome = txs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val dayExpense = txs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            runningBalance += txs.sumOf { netDeltaForActiveAccounts(it) }
            points.add(
                DailyBalancePoint(
                    dateLabel = dayLabel,
                    timestamp = txs.last().timestamp,
                    income = dayIncome,
                    expense = dayExpense,
                    cumulativeBalance = runningBalance
                )
            )
        }
        return points
    }

    fun calculateMonthlyComparisons(
        allTransactions: List<TransactionEntity>,
        monthsCount: Int = 6,
        nowMillis: Long = System.currentTimeMillis()
    ): List<MonthlyComparisonPoint> {
        val ymFormatter = SimpleDateFormat("yyyy-MM", Locale.US)
        val labelFormatter = SimpleDateFormat("MMM yy", Locale("id", "ID"))
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.DAY_OF_MONTH, 1)
        }

        val result = mutableListOf<MonthlyComparisonPoint>()
        for (i in (monthsCount - 1) downTo 0) {
            val c = (cal.clone() as Calendar).apply { add(Calendar.MONTH, -i) }
            val ym = ymFormatter.format(c.time)
            val label = labelFormatter.format(c.time)
            val monthTxs = allTransactions.filter { ymFormatter.format(Date(it.timestamp)) == ym }
            val (inc, exp, net) = calculatePeriodCashFlow(monthTxs)
            result.add(
                MonthlyComparisonPoint(
                    yearMonth = ym,
                    label = label,
                    income = inc,
                    expense = exp,
                    netCashFlow = net
                )
            )
        }
        return result
    }

    fun advanceNextOccurrence(currentTimestamp: Long, frequency: RecurrenceFrequency): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = currentTimestamp }
        when (frequency) {
            RecurrenceFrequency.ONCE -> return currentTimestamp
            RecurrenceFrequency.DAILY -> cal.add(Calendar.DAY_OF_YEAR, 1)
            RecurrenceFrequency.WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            RecurrenceFrequency.MONTHLY -> cal.add(Calendar.MONTH, 1)
            RecurrenceFrequency.YEARLY -> cal.add(Calendar.YEAR, 1)
        }
        return cal.timeInMillis
    }
}
