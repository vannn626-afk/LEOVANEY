package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.backup.BackupPayload
import com.example.data.backup.BackupValidationResult
import com.example.data.backup.RestoreMode
import com.example.data.local.AccountEntity
import com.example.data.local.AssetEntity
import com.example.data.local.BillEntity
import com.example.data.local.BudgetEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.DateFilterPeriod
import com.example.data.local.DebtEntity
import com.example.data.local.DebtPaymentEntity
import com.example.data.local.LiabilityEntity
import com.example.data.local.RecurringTransactionEntity
import com.example.data.local.SavingsContributionEntity
import com.example.data.local.SavingsGoalEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.data.preferences.UserPreferences
import com.example.data.repository.LeovaneyRepository
import com.example.domain.AccountBalanceSummary
import com.example.domain.BudgetStatusSummary
import com.example.domain.CategoryBreakdownItem
import com.example.domain.DailyBalancePoint
import com.example.domain.FinanceCalculator
import com.example.domain.MonthlyComparisonPoint
import com.example.domain.NetWorthBreakdown
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class TransactionSortOrder(val label: String) {
    DATE_DESC("Terbaru"),
    DATE_ASC("Terlama"),
    AMOUNT_DESC("Nominal Tertinggi"),
    AMOUNT_ASC("Nominal Terendah")
}

data class TransactionFilterState(
    val searchQuery: String = "",
    val selectedPeriod: DateFilterPeriod = DateFilterPeriod.THIS_MONTH,
    val customStartDate: Long? = null,
    val customEndDate: Long? = null,
    val selectedType: TransactionType? = null,
    val selectedCategoryId: Long? = null,
    val selectedAccountId: Long? = null,
    val sortOrder: TransactionSortOrder = TransactionSortOrder.DATE_DESC,
    val filterByPeriod: Boolean = false
)

data class LeovaneyUiState(
    val isInitialized: Boolean = false,
    val preferences: UserPreferences = UserPreferences(),
    val selectedDashboardPeriod: DateFilterPeriod = DateFilterPeriod.THIS_MONTH,
    val customPeriodStart: Long? = null,
    val customPeriodEnd: Long? = null,
    val selectedBudgetYearMonth: String = FinanceCalculator.currentYearMonth(),
    val accounts: List<AccountEntity> = emptyList(),
    val accountBalances: List<AccountBalanceSummary> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val allTransactions: List<TransactionEntity> = emptyList(),
    val periodTransactions: List<TransactionEntity> = emptyList(),
    val filteredTransactions: List<TransactionEntity> = emptyList(),
    val transactionFilter: TransactionFilterState = TransactionFilterState(),
    val periodIncome: Long = 0L,
    val periodExpense: Long = 0L,
    val periodNetCashFlow: Long = 0L,
    val netWorth: NetWorthBreakdown = NetWorthBreakdown(0, 0, 0, 0, 0, 0, 0, 0),
    val budgets: List<BudgetEntity> = emptyList(),
    val budgetSummaries: List<BudgetStatusSummary> = emptyList(),
    val expenseByCategory: List<CategoryBreakdownItem> = emptyList(),
    val incomeByCategory: List<CategoryBreakdownItem> = emptyList(),
    val balanceTrend: List<DailyBalancePoint> = emptyList(),
    val monthlyComparisons: List<MonthlyComparisonPoint> = emptyList(),
    val savingsGoals: List<SavingsGoalEntity> = emptyList(),
    val savingsContributions: List<SavingsContributionEntity> = emptyList(),
    val bills: List<BillEntity> = emptyList(),
    val recurringTransactions: List<RecurringTransactionEntity> = emptyList(),
    val debts: List<DebtEntity> = emptyList(),
    val debtPayments: List<DebtPaymentEntity> = emptyList(),
    val assets: List<AssetEntity> = emptyList(),
    val liabilities: List<LiabilityEntity> = emptyList(),
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    val isSaving: Boolean = false
)

class LeovaneyViewModel(
    private val repository: LeovaneyRepository
) : ViewModel() {

    private val _selectedDashboardPeriod = MutableStateFlow(DateFilterPeriod.THIS_MONTH)
    private val _customPeriodRange = MutableStateFlow<Pair<Long?, Long?>>(null to null)
    private val _selectedBudgetYearMonth = MutableStateFlow(FinanceCalculator.currentYearMonth())
    private val _transactionFilter = MutableStateFlow(TransactionFilterState())
    private val _feedbackState = MutableStateFlow<Triple<String?, String?, Boolean>>(Triple(null, null, false))

    private val _pendingBackupPayload = MutableStateFlow<BackupPayload?>(null)
    val pendingBackupPayload: StateFlow<BackupPayload?> = _pendingBackupPayload.asStateFlow()

    init {
        viewModelScope.launch {
            val processedCount = repository.initializeDefaultsAndProcessRecurring()
            if (processedCount > 0) {
                showStatus("$processedCount transaksi berulang otomatis berhasil diproses.")
            }
        }
    }

    private val coreDataFlow = combine(
        repository.preferencesFlow,
        repository.accountsFlow,
        repository.categoriesFlow,
        repository.transactionsFlow,
        repository.budgetsFlow
    ) { prefs, accounts, categories, transactions, budgets ->
        CoreDataSnapshot(prefs, accounts, categories, transactions, budgets)
    }

    private val extendedDataFlow = combine(
        repository.savingsGoalsFlow,
        repository.savingsContributionsFlow,
        repository.billsFlow,
        repository.recurringTransactionsFlow,
        combine(
            repository.debtsFlow,
            repository.debtPaymentsFlow,
            repository.assetsFlow,
            repository.liabilitiesFlow
        ) { debts, debtPayments, assets, liabilities ->
            WealthDataSnapshot(debts, debtPayments, assets, liabilities)
        }
    ) { goals, contributions, bills, recurring, wealth ->
        ExtendedDataSnapshot(goals, contributions, bills, recurring, wealth)
    }

    private val filterControlsFlow = combine(
        _selectedDashboardPeriod,
        _customPeriodRange,
        _selectedBudgetYearMonth,
        _transactionFilter,
        _feedbackState
    ) { period, customRange, budgetYm, txFilter, feedback ->
        FilterControlsSnapshot(period, customRange, budgetYm, txFilter, feedback)
    }

    val uiState: StateFlow<LeovaneyUiState> = combine(
        coreDataFlow,
        extendedDataFlow,
        filterControlsFlow
    ) { core, ext, controls ->
        val (periodStart, periodEnd) = FinanceCalculator.getPeriodRange(
            period = controls.period,
            customStart = controls.customRange.first,
            customEnd = controls.customRange.second
        )

        val accountBalances = FinanceCalculator.calculateAccountBalances(
            accounts = core.accounts,
            allTransactions = core.transactions
        )

        val periodTransactions = core.transactions.filter { tx ->
            tx.timestamp in periodStart..periodEnd
        }

        val (periodIncome, periodExpense, periodNetCashFlow) =
            FinanceCalculator.calculatePeriodCashFlow(periodTransactions)

        val netWorth = FinanceCalculator.calculateNetWorth(
            accountSummaries = accountBalances,
            assets = ext.wealth.assets,
            liabilities = ext.wealth.liabilities,
            debts = ext.wealth.debts
        )

        val budgetSummaries = FinanceCalculator.calculateBudgetSummaries(
            budgets = core.budgets,
            categories = core.categories,
            allTransactions = core.transactions,
            targetYearMonth = controls.budgetYm
        )

        val expenseByCategory = FinanceCalculator.calculateCategoryBreakdown(
            transactions = periodTransactions,
            categories = core.categories,
            type = TransactionType.EXPENSE
        )

        val incomeByCategory = FinanceCalculator.calculateCategoryBreakdown(
            transactions = periodTransactions,
            categories = core.categories,
            type = TransactionType.INCOME
        )

        val balanceTrend = FinanceCalculator.calculateBalanceTrend(
            accounts = core.accounts,
            allTransactions = core.transactions,
            periodStart = periodStart,
            periodEnd = periodEnd
        )

        val monthlyComparisons = FinanceCalculator.calculateMonthlyComparisons(
            allTransactions = core.transactions,
            monthsCount = 6
        )

        val filteredTransactions = applyTransactionFilter(
            allTransactions = core.transactions,
            filter = controls.txFilter
        )

        LeovaneyUiState(
            isInitialized = core.prefs.isLoaded,
            preferences = core.prefs,
            selectedDashboardPeriod = controls.period,
            customPeriodStart = controls.customRange.first,
            customPeriodEnd = controls.customRange.second,
            selectedBudgetYearMonth = controls.budgetYm,
            accounts = core.accounts,
            accountBalances = accountBalances,
            categories = core.categories,
            allTransactions = core.transactions,
            periodTransactions = periodTransactions,
            filteredTransactions = filteredTransactions,
            transactionFilter = controls.txFilter,
            periodIncome = periodIncome,
            periodExpense = periodExpense,
            periodNetCashFlow = periodNetCashFlow,
            netWorth = netWorth,
            budgets = core.budgets,
            budgetSummaries = budgetSummaries,
            expenseByCategory = expenseByCategory,
            incomeByCategory = incomeByCategory,
            balanceTrend = balanceTrend,
            monthlyComparisons = monthlyComparisons,
            savingsGoals = ext.goals,
            savingsContributions = ext.contributions,
            bills = ext.bills,
            recurringTransactions = ext.recurring,
            debts = ext.wealth.debts,
            debtPayments = ext.wealth.debtPayments,
            assets = ext.wealth.assets,
            liabilities = ext.wealth.liabilities,
            statusMessage = controls.feedback.first,
            errorMessage = controls.feedback.second,
            isSaving = controls.feedback.third
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LeovaneyUiState()
    )

    private fun applyTransactionFilter(
        allTransactions: List<TransactionEntity>,
        filter: TransactionFilterState
    ): List<TransactionEntity> {
        val (pStart, pEnd) = FinanceCalculator.getPeriodRange(
            period = filter.selectedPeriod,
            customStart = filter.customStartDate,
            customEnd = filter.customEndDate
        )
        val q = filter.searchQuery.trim().lowercase()

        val filtered = allTransactions.filter { tx ->
            val matchesQuery = q.isEmpty() ||
                tx.title.lowercase().contains(q) ||
                tx.note.lowercase().contains(q) ||
                tx.amount.toString().contains(q)

            val matchesPeriod = !filter.filterByPeriod || (tx.timestamp in pStart..pEnd)
            val matchesType = filter.selectedType == null || tx.type == filter.selectedType
            val matchesCategory = filter.selectedCategoryId == null || tx.categoryId == filter.selectedCategoryId
            val matchesAccount = filter.selectedAccountId == null ||
                tx.accountId == filter.selectedAccountId ||
                tx.destinationAccountId == filter.selectedAccountId

            matchesQuery && matchesPeriod && matchesType && matchesCategory && matchesAccount
        }

        return when (filter.sortOrder) {
            TransactionSortOrder.DATE_DESC -> filtered.sortedWith(compareByDescending<TransactionEntity> { it.timestamp }.thenByDescending { it.id })
            TransactionSortOrder.DATE_ASC -> filtered.sortedWith(compareBy<TransactionEntity> { it.timestamp }.thenBy { it.id })
            TransactionSortOrder.AMOUNT_DESC -> filtered.sortedByDescending { it.amount }
            TransactionSortOrder.AMOUNT_ASC -> filtered.sortedBy { it.amount }
        }
    }

    fun clearMessages() {
        _feedbackState.update { it.copy(first = null, second = null) }
    }

    fun showStatus(msg: String) {
        _feedbackState.value = Triple(msg, null, false)
    }

    fun showError(err: String) {
        _feedbackState.value = Triple(null, err, false)
    }

    // --- PERIOD & FILTER CONTROLS ---
    fun selectDashboardPeriod(period: DateFilterPeriod, customStart: Long? = null, customEnd: Long? = null) {
        _selectedDashboardPeriod.value = period
        if (period == DateFilterPeriod.CUSTOM) {
            _customPeriodRange.value = customStart to customEnd
        }
    }

    fun selectBudgetYearMonth(yearMonth: String) {
        _selectedBudgetYearMonth.value = yearMonth
    }

    fun updateTransactionFilter(transform: (TransactionFilterState) -> TransactionFilterState) {
        _transactionFilter.update(transform)
    }

    fun resetTransactionFilter() {
        _transactionFilter.value = TransactionFilterState()
    }

    // --- ONBOARDING & PROFILE ---
    fun completeOnboarding(name: String, onSuccess: () -> Unit = {}) {
        if (_feedbackState.value.third) return
        viewModelScope.launch {
            _feedbackState.value = Triple(null, null, true)
            val result = repository.completeOnboarding(name)
            result.fold(
                onSuccess = { savedName ->
                    _feedbackState.value = Triple("Selamat datang, $savedName!", null, false)
                    onSuccess()
                },
                onFailure = { e ->
                    _feedbackState.value = Triple(null, e.message ?: "Nama tidak valid.", false)
                }
            )
        }
    }

    fun updateProfileName(name: String, onSuccess: () -> Unit = {}) {
        if (_feedbackState.value.third) return
        viewModelScope.launch {
            _feedbackState.value = Triple(null, null, true)
            val result = repository.updateUserName(name)
            result.fold(
                onSuccess = { savedName ->
                    _feedbackState.value = Triple("Nama profil berhasil diperbarui menjadi $savedName.", null, false)
                    onSuccess()
                },
                onFailure = { e ->
                    _feedbackState.value = Triple(null, e.message ?: "Gagal menyimpan nama.", false)
                }
            )
        }
    }

    // --- TRANSACTIONS ---
    fun saveTransaction(transaction: TransactionEntity, onSuccess: () -> Unit = {}) {
        if (_feedbackState.value.third) return
        viewModelScope.launch {
            _feedbackState.value = Triple(null, null, true)
            val result = repository.saveTransaction(transaction)
            result.fold(
                onSuccess = {
                    _feedbackState.value = Triple("Transaksi berhasil disimpan.", null, false)
                    onSuccess()
                },
                onFailure = { e ->
                    _feedbackState.value = Triple(null, e.message ?: "Gagal menyimpan transaksi.", false)
                }
            )
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            showStatus("Transaksi dihapus.")
        }
    }

    // --- ACCOUNTS ---
    fun saveAccount(account: AccountEntity, onSuccess: () -> Unit = {}) {
        if (_feedbackState.value.third) return
        viewModelScope.launch {
            _feedbackState.value = Triple(null, null, true)
            val result = repository.saveAccount(account)
            result.fold(
                onSuccess = {
                    _feedbackState.value = Triple("Rekening berhasil disimpan.", null, false)
                    onSuccess()
                },
                onFailure = { e ->
                    _feedbackState.value = Triple(null, e.message ?: "Gagal menyimpan rekening.", false)
                }
            )
        }
    }

    fun toggleArchiveAccount(account: AccountEntity) {
        viewModelScope.launch {
            repository.toggleArchiveAccount(account)
            val status = if (!account.isArchived) "diarsipkan" else "diaktifkan kembali"
            showStatus("Rekening ${account.name} $status.")
        }
    }

    // --- CATEGORIES ---
    fun saveCategory(category: CategoryEntity, onSuccess: () -> Unit = {}) {
        if (_feedbackState.value.third) return
        viewModelScope.launch {
            _feedbackState.value = Triple(null, null, true)
            val result = repository.saveCategory(category)
            result.fold(
                onSuccess = {
                    _feedbackState.value = Triple("Kategori berhasil disimpan.", null, false)
                    onSuccess()
                },
                onFailure = { e ->
                    _feedbackState.value = Triple(null, e.message ?: "Gagal menyimpan kategori.", false)
                }
            )
        }
    }

    fun toggleArchiveCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.toggleArchiveCategory(category)
            val status = if (!category.isArchived) "diarsipkan" else "diaktifkan kembali"
            showStatus("Kategori ${category.name} $status.")
        }
    }

    // --- BUDGETS ---
    fun saveBudget(budget: BudgetEntity, onSuccess: () -> Unit = {}) {
        if (_feedbackState.value.third) return
        viewModelScope.launch {
            _feedbackState.value = Triple(null, null, true)
            val result = repository.saveBudget(budget)
            result.fold(
                onSuccess = {
                    _feedbackState.value = Triple("Anggaran berhasil disimpan.", null, false)
                    onSuccess()
                },
                onFailure = { e ->
                    _feedbackState.value = Triple(null, e.message ?: "Gagal menyimpan anggaran.", false)
                }
            )
        }
    }

    fun deleteBudget(budget: BudgetEntity) {
        viewModelScope.launch {
            repository.deleteBudget(budget)
            showStatus("Anggaran dihapus.")
        }
    }

    // --- SAVINGS GOALS ---
    fun saveSavingsGoal(goal: SavingsGoalEntity, onSuccess: () -> Unit = {}) {
        if (_feedbackState.value.third) return
        viewModelScope.launch {
            _feedbackState.value = Triple(null, null, true)
            val result = repository.saveSavingsGoal(goal)
            result.fold(
                onSuccess = {
                    _feedbackState.value = Triple("Target tabungan berhasil disimpan.", null, false)
                    onSuccess()
                },
                onFailure = { e ->
                    _feedbackState.value = Triple(null, e.message ?: "Gagal menyimpan target.", false)
                }
            )
        }
    }

    fun addSavingsContribution(
        goalId: Long,
        amount: Long,
        isWithdrawal: Boolean,
        note: String,
        onSuccess: () -> Unit = {}
    ) {
        if (_feedbackState.value.third) return
        viewModelScope.launch {
            _feedbackState.value = Triple(null, null, true)
            val result = repository.addSavingsContribution(goalId, amount, isWithdrawal, note)
            result.fold(
                onSuccess = {
                    val label = if (isWithdrawal) "Penarikan tabungan" else "Kontribusi tabungan"
                    _feedbackState.value = Triple("$label berhasil dicatat.", null, false)
                    onSuccess()
                },
                onFailure = { e ->
                    _feedbackState.value = Triple(null, e.message ?: "Gagal mencatat kontribusi.", false)
                }
            )
        }
    }

    fun toggleSavingsGoalCompleted(goal: SavingsGoalEntity) {
        viewModelScope.launch {
            repository.markSavingsGoalCompleted(goal, !goal.isCompleted)
            showStatus("Status target tabungan diperbarui.")
        }
    }

    fun deleteSavingsGoal(goal: SavingsGoalEntity) {
        viewModelScope.launch {
            repository.deleteSavingsGoal(goal)
            showStatus("Target tabungan dihapus.")
        }
    }

    // --- BILLS & RECURRING ---
    fun saveBill(bill: BillEntity, onSuccess: () -> Unit = {}) {
        if (_feedbackState.value.third) return
        viewModelScope.launch {
            _feedbackState.value = Triple(null, null, true)
            val result = repository.saveBill(bill)
            result.fold(
                onSuccess = {
                    _feedbackState.value = Triple("Tagihan berhasil disimpan.", null, false)
                    onSuccess()
                },
                onFailure = { e ->
                    _feedbackState.value = Triple(null, e.message ?: "Gagal menyimpan tagihan.", false)
                }
            )
        }
    }

    fun markBillPaid(bill: BillEntity, accountId: Long?) {
        viewModelScope.launch {
            repository.payBill(bill, accountId)
            showStatus("Tagihan ${bill.name} ditandai sudah dibayar.")
        }
    }

    fun deleteBill(bill: BillEntity) {
        viewModelScope.launch {
            repository.deleteBill(bill)
            showStatus("Tagihan dihapus.")
        }
    }

    fun saveRecurringTransaction(item: RecurringTransactionEntity, onSuccess: () -> Unit = {}) {
        if (_feedbackState.value.third) return
        viewModelScope.launch {
            _feedbackState.value = Triple(null, null, true)
            val result = repository.saveRecurringTransaction(item)
            result.fold(
                onSuccess = {
                    _feedbackState.value = Triple("Jadwal transaksi berulang disimpan.", null, false)
                    onSuccess()
                },
                onFailure = { e ->
                    _feedbackState.value = Triple(null, e.message ?: "Gagal menyimpan jadwal.", false)
                }
            )
        }
    }

    fun toggleRecurringActive(item: RecurringTransactionEntity) {
        viewModelScope.launch {
            repository.toggleRecurringActive(item)
            showStatus("Status jadwal berulang diperbarui.")
        }
    }

    fun deleteRecurringTransaction(item: RecurringTransactionEntity) {
        viewModelScope.launch {
            repository.deleteRecurringTransaction(item)
            showStatus("Jadwal berulang dihapus.")
        }
    }

    // --- DEBTS & RECEIVABLES ---
    fun saveDebt(debt: DebtEntity, onSuccess: () -> Unit = {}) {
        if (_feedbackState.value.third) return
        viewModelScope.launch {
            _feedbackState.value = Triple(null, null, true)
            val result = repository.saveDebt(debt)
            result.fold(
                onSuccess = {
                    _feedbackState.value = Triple("Data ${debt.type.label} berhasil disimpan.", null, false)
                    onSuccess()
                },
                onFailure = { e ->
                    _feedbackState.value = Triple(null, e.message ?: "Gagal menyimpan data.", false)
                }
            )
        }
    }

    fun recordDebtPayment(
        debtId: Long,
        principalPaid: Long,
        interestOrFeePaid: Long,
        accountId: Long?,
        note: String,
        onSuccess: () -> Unit = {}
    ) {
        if (_feedbackState.value.third) return
        viewModelScope.launch {
            _feedbackState.value = Triple(null, null, true)
            val result = repository.recordDebtPayment(debtId, principalPaid, interestOrFeePaid, accountId, note)
            result.fold(
                onSuccess = {
                    _feedbackState.value = Triple("Pembayaran berhasil dicatat.", null, false)
                    onSuccess()
                },
                onFailure = { e ->
                    _feedbackState.value = Triple(null, e.message ?: "Gagal mencatat pembayaran.", false)
                }
            )
        }
    }

    fun deleteDebt(debt: DebtEntity) {
        viewModelScope.launch {
            repository.deleteDebt(debt)
            showStatus("Catatan ${debt.type.label} dihapus.")
        }
    }

    // --- ASSETS & LIABILITIES ---
    fun saveAsset(asset: AssetEntity, onSuccess: () -> Unit = {}) {
        if (_feedbackState.value.third) return
        viewModelScope.launch {
            _feedbackState.value = Triple(null, null, true)
            val result = repository.saveAsset(asset)
            result.fold(
                onSuccess = {
                    _feedbackState.value = Triple("Aset berhasil disimpan.", null, false)
                    onSuccess()
                },
                onFailure = { e ->
                    _feedbackState.value = Triple(null, e.message ?: "Gagal menyimpan aset.", false)
                }
            )
        }
    }

    fun deleteAsset(asset: AssetEntity) {
        viewModelScope.launch {
            repository.deleteAsset(asset)
            showStatus("Aset dihapus.")
        }
    }

    fun saveLiability(liability: LiabilityEntity, onSuccess: () -> Unit = {}) {
        if (_feedbackState.value.third) return
        viewModelScope.launch {
            _feedbackState.value = Triple(null, null, true)
            val result = repository.saveLiability(liability)
            result.fold(
                onSuccess = {
                    _feedbackState.value = Triple("Kewajiban berhasil disimpan.", null, false)
                    onSuccess()
                },
                onFailure = { e ->
                    _feedbackState.value = Triple(null, e.message ?: "Gagal menyimpan kewajiban.", false)
                }
            )
        }
    }

    fun deleteLiability(liability: LiabilityEntity) {
        viewModelScope.launch {
            repository.deleteLiability(liability)
            showStatus("Kewajiban dihapus.")
        }
    }

    // --- BACKUP, RESTORE, CSV & RESET ---
    fun generateBackupJson(onResult: (String) -> Unit) {
        viewModelScope.launch {
            _feedbackState.value = Triple(null, null, true)
            runCatching { repository.exportBackupJson() }
                .onSuccess { json ->
                    _feedbackState.value = Triple("Backup JSON siap disimpan.", null, false)
                    onResult(json)
                }
                .onFailure { e ->
                    _feedbackState.value = Triple(null, "Gagal membuat backup: ${e.message}", false)
                }
        }
    }

    fun generateTransactionsCsv(onResult: (String) -> Unit) {
        viewModelScope.launch {
            _feedbackState.value = Triple(null, null, true)
            runCatching { repository.exportTransactionsCsv() }
                .onSuccess { csv ->
                    _feedbackState.value = Triple("File CSV siap disimpan.", null, false)
                    onResult(csv)
                }
                .onFailure { e ->
                    _feedbackState.value = Triple(null, "Gagal mengekspor CSV: ${e.message}", false)
                }
        }
    }

    fun stageBackupForConfirmation(rawJson: String) {
        when (val validation = repository.validateBackupJson(rawJson)) {
            is BackupValidationResult.Valid -> {
                _pendingBackupPayload.value = validation.payload
                clearMessages()
            }
            is BackupValidationResult.Invalid -> {
                _pendingBackupPayload.value = null
                showError(validation.errorMessage)
            }
        }
    }

    fun cancelStagedBackup() {
        _pendingBackupPayload.value = null
    }

    fun confirmRestoreBackup(mode: RestoreMode, onSuccess: () -> Unit = {}) {
        val payload = _pendingBackupPayload.value ?: return
        viewModelScope.launch {
            _feedbackState.value = Triple(null, null, true)
            val res = repository.restoreFromBackup(payload, mode)
            res.fold(
                onSuccess = {
                    _pendingBackupPayload.value = null
                    _feedbackState.value = Triple("Data berhasil dipulihkan (${mode.label}).", null, false)
                    onSuccess()
                },
                onFailure = { e ->
                    _feedbackState.value = Triple(null, "Gagal memulihkan backup: ${e.message}", false)
                }
            )
        }
    }

    fun resetAllFinancialData(onCompleted: () -> Unit = {}) {
        viewModelScope.launch {
            _feedbackState.value = Triple(null, null, true)
            repository.resetAllFinancialDataConfirmed()
            _feedbackState.value = Triple("Seluruh data keuangan telah direset. Profil tetap tersimpan.", null, false)
            onCompleted()
        }
    }

    private data class CoreDataSnapshot(
        val prefs: UserPreferences,
        val accounts: List<AccountEntity>,
        val categories: List<CategoryEntity>,
        val transactions: List<TransactionEntity>,
        val budgets: List<BudgetEntity>
    )

    private data class WealthDataSnapshot(
        val debts: List<DebtEntity>,
        val debtPayments: List<DebtPaymentEntity>,
        val assets: List<AssetEntity>,
        val liabilities: List<LiabilityEntity>
    )

    private data class ExtendedDataSnapshot(
        val goals: List<SavingsGoalEntity>,
        val contributions: List<SavingsContributionEntity>,
        val bills: List<BillEntity>,
        val recurring: List<RecurringTransactionEntity>,
        val wealth: WealthDataSnapshot
    )

    private data class FilterControlsSnapshot(
        val period: DateFilterPeriod,
        val customRange: Pair<Long?, Long?>,
        val budgetYm: String,
        val txFilter: TransactionFilterState,
        val feedback: Triple<String?, String?, Boolean>
    )

    companion object {
        fun provideFactory(repository: LeovaneyRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return LeovaneyViewModel(repository) as T
                }
            }
    }
}
