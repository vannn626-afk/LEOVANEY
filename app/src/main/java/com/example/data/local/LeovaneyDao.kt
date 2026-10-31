package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LeovaneyDao {

    // --- ACCOUNTS ---
    @Query("SELECT * FROM accounts ORDER BY isArchived ASC, createdAt ASC")
    fun observeAllAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts ORDER BY createdAt ASC")
    suspend fun getAllAccountsOnce(): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun getAccountById(id: Long): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(accounts: List<AccountEntity>)

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Delete
    suspend fun deleteAccount(account: AccountEntity)

    // --- CATEGORIES ---
    @Query("SELECT * FROM categories ORDER BY isArchived ASC, type ASC, id ASC")
    fun observeAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY id ASC")
    suspend fun getAllCategoriesOnce(): List<CategoryEntity>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCategoryCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    // --- TRANSACTIONS ---
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC, id DESC")
    fun observeAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC, id DESC")
    suspend fun getAllTransactionsOnce(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    // --- BUDGETS ---
    @Query("SELECT * FROM budgets ORDER BY yearMonth DESC, categoryId ASC")
    fun observeAllBudgets(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets ORDER BY id ASC")
    suspend fun getAllBudgetsOnce(): List<BudgetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgets(budgets: List<BudgetEntity>)

    @Update
    suspend fun updateBudget(budget: BudgetEntity)

    @Delete
    suspend fun deleteBudget(budget: BudgetEntity)

    // --- SAVINGS GOALS & CONTRIBUTIONS ---
    @Query("SELECT * FROM savings_goals ORDER BY isCompleted ASC, createdAt DESC")
    fun observeAllSavingsGoals(): Flow<List<SavingsGoalEntity>>

    @Query("SELECT * FROM savings_goals ORDER BY id ASC")
    suspend fun getAllSavingsGoalsOnce(): List<SavingsGoalEntity>

    @Query("SELECT * FROM savings_goals WHERE id = :id LIMIT 1")
    suspend fun getSavingsGoalById(id: Long): SavingsGoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavingsGoal(goal: SavingsGoalEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavingsGoals(goals: List<SavingsGoalEntity>)

    @Update
    suspend fun updateSavingsGoal(goal: SavingsGoalEntity)

    @Delete
    suspend fun deleteSavingsGoal(goal: SavingsGoalEntity)

    @Query("SELECT * FROM savings_contributions ORDER BY timestamp DESC")
    fun observeAllSavingsContributions(): Flow<List<SavingsContributionEntity>>

    @Query("SELECT * FROM savings_contributions ORDER BY id ASC")
    suspend fun getAllSavingsContributionsOnce(): List<SavingsContributionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavingsContribution(contribution: SavingsContributionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavingsContributions(contributions: List<SavingsContributionEntity>)

    @Transaction
    suspend fun recordSavingsContribution(
        goalId: Long,
        amount: Long,
        isWithdrawal: Boolean,
        note: String,
        timestamp: Long
    ) {
        val goal = getSavingsGoalById(goalId) ?: return
        val delta = if (isWithdrawal) -amount else amount
        val updatedAmount = (goal.currentAmount + delta).coerceAtLeast(0L)
        val completed = updatedAmount >= goal.targetAmount && goal.targetAmount > 0L
        insertSavingsContribution(
            SavingsContributionEntity(
                goalId = goalId,
                amount = amount,
                isWithdrawal = isWithdrawal,
                timestamp = timestamp,
                note = note
            )
        )
        updateSavingsGoal(
            goal.copy(
                currentAmount = updatedAmount,
                isCompleted = completed
            )
        )
    }

    // --- BILLS ---
    @Query("SELECT * FROM bills ORDER BY isPaid ASC, dueDate ASC")
    fun observeAllBills(): Flow<List<BillEntity>>

    @Query("SELECT * FROM bills ORDER BY id ASC")
    suspend fun getAllBillsOnce(): List<BillEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: BillEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBills(bills: List<BillEntity>)

    @Update
    suspend fun updateBill(bill: BillEntity)

    @Delete
    suspend fun deleteBill(bill: BillEntity)

    @Transaction
    suspend fun markBillPaidAndRecordExpense(
        bill: BillEntity,
        accountId: Long?,
        paidTimestamp: Long,
        nextDueDate: Long?
    ) {
        if (accountId != null) {
            insertTransaction(
                TransactionEntity(
                    type = TransactionType.EXPENSE,
                    amount = bill.amount,
                    categoryId = bill.categoryId,
                    accountId = accountId,
                    timestamp = paidTimestamp,
                    title = "Pembayaran Tagihan: ${bill.name}",
                    note = bill.note,
                    billId = bill.id
                )
            )
        }
        if (bill.frequency == RecurrenceFrequency.ONCE || nextDueDate == null) {
            updateBill(bill.copy(isPaid = true, lastPaidAt = paidTimestamp))
        } else {
            updateBill(
                bill.copy(
                    dueDate = nextDueDate,
                    isPaid = false,
                    lastPaidAt = paidTimestamp
                )
            )
        }
    }

    // --- RECURRING TRANSACTIONS ---
    @Query("SELECT * FROM recurring_transactions ORDER BY isActive DESC, nextRunDate ASC")
    fun observeAllRecurringTransactions(): Flow<List<RecurringTransactionEntity>>

    @Query("SELECT * FROM recurring_transactions ORDER BY id ASC")
    suspend fun getAllRecurringTransactionsOnce(): List<RecurringTransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringTransaction(item: RecurringTransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringTransactions(items: List<RecurringTransactionEntity>)

    @Update
    suspend fun updateRecurringTransaction(item: RecurringTransactionEntity)

    @Delete
    suspend fun deleteRecurringTransaction(item: RecurringTransactionEntity)

    // --- DEBTS & DEBT PAYMENTS ---
    @Query("SELECT * FROM debts ORDER BY isSettled ASC, dueDate ASC, startDate DESC")
    fun observeAllDebts(): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debts ORDER BY id ASC")
    suspend fun getAllDebtsOnce(): List<DebtEntity>

    @Query("SELECT * FROM debts WHERE id = :id LIMIT 1")
    suspend fun getDebtById(id: Long): DebtEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: DebtEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebts(debts: List<DebtEntity>)

    @Update
    suspend fun updateDebt(debt: DebtEntity)

    @Delete
    suspend fun deleteDebt(debt: DebtEntity)

    @Query("SELECT * FROM debt_payments ORDER BY timestamp DESC")
    fun observeAllDebtPayments(): Flow<List<DebtPaymentEntity>>

    @Query("SELECT * FROM debt_payments ORDER BY id ASC")
    suspend fun getAllDebtPaymentsOnce(): List<DebtPaymentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebtPayment(payment: DebtPaymentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebtPayments(payments: List<DebtPaymentEntity>)

    @Transaction
    suspend fun recordDebtPaymentTransaction(
        debtId: Long,
        principalPaid: Long,
        interestOrFeePaid: Long,
        accountId: Long?,
        timestamp: Long,
        note: String
    ) {
        val debt = getDebtById(debtId) ?: return
        val newRemaining = (debt.remainingPrincipal - principalPaid).coerceAtLeast(0L)
        val settled = newRemaining == 0L
        insertDebtPayment(
            DebtPaymentEntity(
                debtId = debtId,
                principalPaid = principalPaid,
                interestOrFeePaid = interestOrFeePaid,
                accountId = accountId,
                timestamp = timestamp,
                note = note
            )
        )
        updateDebt(
            debt.copy(
                remainingPrincipal = newRemaining,
                isSettled = settled
            )
        )
        // If there is interest/fee paid from an account, record only the interest/fee as new expense/income
        // so principal is never double-counted as a second new expense when obligation was already recorded.
        if (accountId != null && interestOrFeePaid > 0L) {
            val txType = if (debt.type == DebtType.DEBT) TransactionType.EXPENSE else TransactionType.INCOME
            val txTitle = if (debt.type == DebtType.DEBT) {
                "Bunga/Biaya Utang: ${debt.personName}"
            } else {
                "Bunga/Bagi Hasil Piutang: ${debt.personName}"
            }
            insertTransaction(
                TransactionEntity(
                    type = txType,
                    amount = interestOrFeePaid,
                    accountId = accountId,
                    timestamp = timestamp,
                    title = txTitle,
                    note = note
                )
            )
        }
    }

    // --- ASSETS & LIABILITIES ---
    @Query("SELECT * FROM assets ORDER BY acquiredDate DESC")
    fun observeAllAssets(): Flow<List<AssetEntity>>

    @Query("SELECT * FROM assets ORDER BY id ASC")
    suspend fun getAllAssetsOnce(): List<AssetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAsset(asset: AssetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssets(assets: List<AssetEntity>)

    @Update
    suspend fun updateAsset(asset: AssetEntity)

    @Delete
    suspend fun deleteAsset(asset: AssetEntity)

    @Query("SELECT * FROM liabilities ORDER BY id DESC")
    fun observeAllLiabilities(): Flow<List<LiabilityEntity>>

    @Query("SELECT * FROM liabilities ORDER BY id ASC")
    suspend fun getAllLiabilitiesOnce(): List<LiabilityEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLiability(liability: LiabilityEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLiabilities(liabilities: List<LiabilityEntity>)

    @Update
    suspend fun updateLiability(liability: LiabilityEntity)

    @Delete
    suspend fun deleteLiability(liability: LiabilityEntity)

    // --- CLEAR ALL DATA (WITHIN TRANSACTION FOR RESET OR REPLACE RESTORE) ---
    @Query("DELETE FROM savings_contributions")
    suspend fun clearSavingsContributions()

    @Query("DELETE FROM savings_goals")
    suspend fun clearSavingsGoals()

    @Query("DELETE FROM debt_payments")
    suspend fun clearDebtPayments()

    @Query("DELETE FROM debts")
    suspend fun clearDebts()

    @Query("DELETE FROM transactions")
    suspend fun clearTransactions()

    @Query("DELETE FROM budgets")
    suspend fun clearBudgets()

    @Query("DELETE FROM bills")
    suspend fun clearBills()

    @Query("DELETE FROM recurring_transactions")
    suspend fun clearRecurringTransactions()

    @Query("DELETE FROM assets")
    suspend fun clearAssets()

    @Query("DELETE FROM liabilities")
    suspend fun clearLiabilities()

    @Query("DELETE FROM categories")
    suspend fun clearCategories()

    @Query("DELETE FROM accounts")
    suspend fun clearAccounts()

    @Transaction
    suspend fun deleteAllFinancialData() {
        clearSavingsContributions()
        clearSavingsGoals()
        clearDebtPayments()
        clearDebts()
        clearTransactions()
        clearBudgets()
        clearBills()
        clearRecurringTransactions()
        clearAssets()
        clearLiabilities()
        clearCategories()
        clearAccounts()
    }
}
