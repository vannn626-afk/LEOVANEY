package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.backup.BackupManager
import com.example.data.backup.BackupPayload
import com.example.data.backup.BackupValidationResult
import com.example.data.backup.RestoreMode
import com.example.data.local.AccountEntity
import com.example.data.local.AssetEntity
import com.example.data.local.BillEntity
import com.example.data.local.BudgetEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.DebtEntity
import com.example.data.local.DebtPaymentEntity
import com.example.data.local.LeovaneyDatabase
import com.example.data.local.LiabilityEntity
import com.example.data.local.RecurringTransactionEntity
import com.example.data.local.SavingsContributionEntity
import com.example.data.local.SavingsGoalEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.data.preferences.UserPreferences
import com.example.data.preferences.UserPreferencesRepository
import com.example.domain.FinanceCalculator
import kotlinx.coroutines.flow.Flow

class LeovaneyRepository(
    private val database: LeovaneyDatabase,
    private val preferencesRepository: UserPreferencesRepository
) {
    private val dao = database.dao()

    val preferencesFlow: Flow<UserPreferences> = preferencesRepository.preferencesFlow
    val accountsFlow: Flow<List<AccountEntity>> = dao.observeAllAccounts()
    val categoriesFlow: Flow<List<CategoryEntity>> = dao.observeAllCategories()
    val transactionsFlow: Flow<List<TransactionEntity>> = dao.observeAllTransactions()
    val budgetsFlow: Flow<List<BudgetEntity>> = dao.observeAllBudgets()
    val savingsGoalsFlow: Flow<List<SavingsGoalEntity>> = dao.observeAllSavingsGoals()
    val savingsContributionsFlow: Flow<List<SavingsContributionEntity>> = dao.observeAllSavingsContributions()
    val billsFlow: Flow<List<BillEntity>> = dao.observeAllBills()
    val recurringTransactionsFlow: Flow<List<RecurringTransactionEntity>> = dao.observeAllRecurringTransactions()
    val debtsFlow: Flow<List<DebtEntity>> = dao.observeAllDebts()
    val debtPaymentsFlow: Flow<List<DebtPaymentEntity>> = dao.observeAllDebtPayments()
    val assetsFlow: Flow<List<AssetEntity>> = dao.observeAllAssets()
    val liabilitiesFlow: Flow<List<LiabilityEntity>> = dao.observeAllLiabilities()

    /**
     * Seeds default categories and baseline accounts (with 0 balance, never fake transactions)
     * on first open if none exist yet, and processes any due recurring transactions locally.
     */
    suspend fun initializeDefaultsAndProcessRecurring(nowMillis: Long = System.currentTimeMillis()): Int {
        database.withTransaction {
            if (dao.getCategoryCount() == 0) {
                dao.insertCategories(LeovaneyDatabase.defaultCategories())
            }
            if (dao.getAllAccountsOnce().isEmpty()) {
                dao.insertAccounts(LeovaneyDatabase.defaultAccounts())
            }
        }
        return processDueRecurringTransactions(nowMillis)
    }

    /**
     * Processes due recurring transactions locally when the app is opened.
     * Stores lastProcessedDate and advances nextRunDate inside a single database transaction
     * so duplicate transactions are never created.
     */
    suspend fun processDueRecurringTransactions(nowMillis: Long = System.currentTimeMillis()): Int {
        var processedCount = 0
        database.withTransaction {
            val allRecurring = dao.getAllRecurringTransactionsOnce()
            for (item in allRecurring) {
                if (!item.isActive) continue
                var nextRun = item.nextRunDate
                var lastProcessed = item.lastProcessedDate
                var safetyCounter = 0

                while (nextRun <= nowMillis && safetyCounter < 36) {
                    // Prevent duplicate processing for the same scheduled timestamp
                    if (lastProcessed != null && lastProcessed >= nextRun) {
                        nextRun = FinanceCalculator.advanceNextOccurrence(nextRun, item.frequency)
                        continue
                    }
                    dao.insertTransaction(
                        TransactionEntity(
                            type = item.type,
                            amount = item.amount,
                            categoryId = item.categoryId,
                            accountId = item.accountId,
                            destinationAccountId = item.destinationAccountId,
                            timestamp = nextRun,
                            title = "${item.title} (Otomatis)",
                            note = item.note,
                            recurringId = item.id
                        )
                    )
                    lastProcessed = nextRun
                    processedCount++
                    val advanced = FinanceCalculator.advanceNextOccurrence(nextRun, item.frequency)
                    if (advanced <= nextRun) break
                    nextRun = advanced
                    safetyCounter++
                }

                if (lastProcessed != item.lastProcessedDate || nextRun != item.nextRunDate) {
                    dao.updateRecurringTransaction(
                        item.copy(
                            nextRunDate = nextRun,
                            lastProcessedDate = lastProcessed
                        )
                    )
                }
            }
        }
        return processedCount
    }

    // --- ONBOARDING & PREFERENCES ---
    suspend fun completeOnboarding(userName: String): Result<String> =
        preferencesRepository.saveUserNameAndCompleteOnboarding(userName)

    suspend fun updateUserName(userName: String): Result<String> =
        preferencesRepository.updateUserName(userName)

    suspend fun updateCurrencyCode(code: String) =
        preferencesRepository.updateCurrencyCode(code)

    // --- TRANSACTIONS ---
    suspend fun saveTransaction(transaction: TransactionEntity): Result<Long> {
        if (transaction.amount <= 0L) {
            return Result.failure(IllegalArgumentException("Nominal transaksi harus lebih besar dari Rp0."))
        }
        if (transaction.title.isBlank()) {
            return Result.failure(IllegalArgumentException("Keterangan transaksi tidak boleh kosong."))
        }
        if (transaction.type == TransactionType.TRANSFER) {
            val dest = transaction.destinationAccountId
                ?: return Result.failure(IllegalArgumentException("Pilih rekening tujuan untuk transfer."))
            if (dest == transaction.accountId) {
                return Result.failure(IllegalArgumentException("Rekening sumber dan tujuan tidak boleh sama."))
            }
        }
        return runCatching {
            database.withTransaction {
                if (transaction.id == 0L) {
                    dao.insertTransaction(transaction)
                } else {
                    dao.updateTransaction(transaction)
                    transaction.id
                }
            }
        }
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        dao.deleteTransaction(transaction)
    }

    // --- ACCOUNTS ---
    suspend fun saveAccount(account: AccountEntity): Result<Long> {
        if (account.name.isBlank()) {
            return Result.failure(IllegalArgumentException("Nama rekening tidak boleh kosong."))
        }
        return runCatching {
            if (account.id == 0L) dao.insertAccount(account) else {
                dao.updateAccount(account)
                account.id
            }
        }
    }

    suspend fun toggleArchiveAccount(account: AccountEntity) {
        dao.updateAccount(account.copy(isArchived = !account.isArchived))
    }

    // --- CATEGORIES ---
    suspend fun saveCategory(category: CategoryEntity): Result<Long> {
        if (category.name.isBlank()) {
            return Result.failure(IllegalArgumentException("Nama kategori tidak boleh kosong."))
        }
        return runCatching {
            if (category.id == 0L) dao.insertCategory(category) else {
                dao.updateCategory(category)
                category.id
            }
        }
    }

    suspend fun toggleArchiveCategory(category: CategoryEntity) {
        dao.updateCategory(category.copy(isArchived = !category.isArchived))
    }

    // --- BUDGETS ---
    suspend fun saveBudget(budget: BudgetEntity): Result<Long> {
        if (budget.amountLimit <= 0L) {
            return Result.failure(IllegalArgumentException("Batas anggaran harus lebih besar dari Rp0."))
        }
        return runCatching {
            if (budget.id == 0L) dao.insertBudget(budget) else {
                dao.updateBudget(budget)
                budget.id
            }
        }
    }

    suspend fun deleteBudget(budget: BudgetEntity) {
        dao.deleteBudget(budget)
    }

    // --- SAVINGS GOALS ---
    suspend fun saveSavingsGoal(goal: SavingsGoalEntity): Result<Long> {
        if (goal.name.isBlank()) {
            return Result.failure(IllegalArgumentException("Nama target tabungan tidak boleh kosong."))
        }
        if (goal.targetAmount <= 0L) {
            return Result.failure(IllegalArgumentException("Nominal target harus lebih besar dari Rp0."))
        }
        return runCatching {
            val isCompleted = goal.currentAmount >= goal.targetAmount
            val adjusted = goal.copy(isCompleted = isCompleted)
            if (adjusted.id == 0L) dao.insertSavingsGoal(adjusted) else {
                dao.updateSavingsGoal(adjusted)
                adjusted.id
            }
        }
    }

    suspend fun addSavingsContribution(
        goalId: Long,
        amount: Long,
        isWithdrawal: Boolean,
        note: String,
        timestamp: Long = System.currentTimeMillis()
    ): Result<Unit> {
        if (amount <= 0L) {
            return Result.failure(IllegalArgumentException("Nominal harus lebih besar dari Rp0."))
        }
        return runCatching {
            dao.recordSavingsContribution(goalId, amount, isWithdrawal, note, timestamp)
        }
    }

    suspend fun markSavingsGoalCompleted(goal: SavingsGoalEntity, completed: Boolean) {
        dao.updateSavingsGoal(goal.copy(isCompleted = completed))
    }

    suspend fun deleteSavingsGoal(goal: SavingsGoalEntity) {
        dao.deleteSavingsGoal(goal)
    }

    // --- BILLS & RECURRING ---
    suspend fun saveBill(bill: BillEntity): Result<Long> {
        if (bill.name.isBlank()) {
            return Result.failure(IllegalArgumentException("Nama tagihan tidak boleh kosong."))
        }
        if (bill.amount <= 0L) {
            return Result.failure(IllegalArgumentException("Nominal tagihan harus lebih besar dari Rp0."))
        }
        return runCatching {
            if (bill.id == 0L) dao.insertBill(bill) else {
                dao.updateBill(bill)
                bill.id
            }
        }
    }

    suspend fun payBill(bill: BillEntity, accountId: Long?, paidTimestamp: Long = System.currentTimeMillis()) {
        val nextDue = FinanceCalculator.advanceNextOccurrence(bill.dueDate, bill.frequency)
        dao.markBillPaidAndRecordExpense(bill, accountId, paidTimestamp, nextDue)
    }

    suspend fun deleteBill(bill: BillEntity) {
        dao.deleteBill(bill)
    }

    suspend fun saveRecurringTransaction(item: RecurringTransactionEntity): Result<Long> {
        if (item.title.isBlank()) {
            return Result.failure(IllegalArgumentException("Judul jadwal berulang tidak boleh kosong."))
        }
        if (item.amount <= 0L) {
            return Result.failure(IllegalArgumentException("Nominal harus lebih besar dari Rp0."))
        }
        return runCatching {
            if (item.id == 0L) dao.insertRecurringTransaction(item) else {
                dao.updateRecurringTransaction(item)
                item.id
            }
        }
    }

    suspend fun toggleRecurringActive(item: RecurringTransactionEntity) {
        dao.updateRecurringTransaction(item.copy(isActive = !item.isActive))
    }

    suspend fun deleteRecurringTransaction(item: RecurringTransactionEntity) {
        dao.deleteRecurringTransaction(item)
    }

    // --- DEBTS & RECEIVABLES ---
    suspend fun saveDebt(debt: DebtEntity): Result<Long> {
        if (debt.personName.isBlank()) {
            return Result.failure(IllegalArgumentException("Nama pihak tidak boleh kosong."))
        }
        if (debt.principalAmount <= 0L) {
            return Result.failure(IllegalArgumentException("Nominal pokok harus lebih besar dari Rp0."))
        }
        return runCatching {
            if (debt.id == 0L) dao.insertDebt(debt) else {
                dao.updateDebt(debt)
                debt.id
            }
        }
    }

    suspend fun recordDebtPayment(
        debtId: Long,
        principalPaid: Long,
        interestOrFeePaid: Long,
        accountId: Long?,
        note: String,
        timestamp: Long = System.currentTimeMillis()
    ): Result<Unit> {
        if (principalPaid <= 0L && interestOrFeePaid <= 0L) {
            return Result.failure(IllegalArgumentException("Nominal pembayaran harus lebih besar dari Rp0."))
        }
        return runCatching {
            dao.recordDebtPaymentTransaction(debtId, principalPaid, interestOrFeePaid, accountId, timestamp, note)
        }
    }

    suspend fun deleteDebt(debt: DebtEntity) {
        dao.deleteDebt(debt)
    }

    // --- ASSETS & LIABILITIES ---
    suspend fun saveAsset(asset: AssetEntity): Result<Long> {
        if (asset.name.isBlank()) {
            return Result.failure(IllegalArgumentException("Nama aset tidak boleh kosong."))
        }
        if (asset.value < 0L) {
            return Result.failure(IllegalArgumentException("Nilai aset tidak boleh negatif."))
        }
        return runCatching {
            if (asset.id == 0L) dao.insertAsset(asset) else {
                dao.updateAsset(asset)
                asset.id
            }
        }
    }

    suspend fun deleteAsset(asset: AssetEntity) {
        dao.deleteAsset(asset)
    }

    suspend fun saveLiability(liability: LiabilityEntity): Result<Long> {
        if (liability.name.isBlank()) {
            return Result.failure(IllegalArgumentException("Nama kewajiban tidak boleh kosong."))
        }
        if (liability.amount < 0L) {
            return Result.failure(IllegalArgumentException("Nominal kewajiban tidak boleh negatif."))
        }
        return runCatching {
            if (liability.id == 0L) dao.insertLiability(liability) else {
                dao.updateLiability(liability)
                liability.id
            }
        }
    }

    suspend fun deleteLiability(liability: LiabilityEntity) {
        dao.deleteLiability(liability)
    }

    // --- BACKUP, RESTORE, CSV & RESET ---
    suspend fun exportBackupJson(): String =
        BackupManager.createBackupJson(database, preferencesRepository)

    fun validateBackupJson(rawJson: String): BackupValidationResult =
        BackupManager.validateAndParseBackup(rawJson)

    suspend fun restoreFromBackup(payload: BackupPayload, mode: RestoreMode): Result<Unit> =
        BackupManager.restoreBackup(database, preferencesRepository, payload, mode)

    suspend fun exportTransactionsCsv(): String {
        val txs = dao.getAllTransactionsOnce()
        val accs = dao.getAllAccountsOnce()
        val cats = dao.getAllCategoriesOnce()
        return BackupManager.generateTransactionsCsv(txs, accs, cats)
    }

    /**
     * Resets financial data only when explicitly confirmed by user in Settings.
     * Preserves user profile in DataStore and re-seeds default categories & accounts.
     */
    suspend fun resetAllFinancialDataConfirmed() {
        database.withTransaction {
            dao.deleteAllFinancialData()
            dao.insertCategories(LeovaneyDatabase.defaultCategories())
            dao.insertAccounts(LeovaneyDatabase.defaultAccounts())
        }
    }
}
