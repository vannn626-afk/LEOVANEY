package com.example

import com.example.data.backup.BackupManager
import com.example.data.backup.BackupValidationResult
import com.example.data.local.AccountEntity
import com.example.data.local.AccountType
import com.example.data.local.AssetEntity
import com.example.data.local.AssetType
import com.example.data.local.BudgetEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.CategoryType
import com.example.data.local.DebtEntity
import com.example.data.local.DebtType
import com.example.data.local.LiabilityEntity
import com.example.data.local.LiabilityType
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.domain.FinanceCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleUnitTest {

    @Test
    fun `formatRupiah formats positive and negative IDR values accurately without floating point`() {
        assertEquals("Rp1.250.000", FinanceCalculator.formatRupiah(1_250_000L))
        assertEquals("-Rp500.000", FinanceCalculator.formatRupiah(-500_000L))
        assertEquals("+Rp250.000", FinanceCalculator.formatRupiah(250_000L, includePlusSign = true))
        assertEquals("Rp0", FinanceCalculator.formatRupiah(0L))
    }

    @Test
    fun `income expense and transfer update two accounts and reports accurately without double counting`() {
        val bank = AccountEntity(id = 1L, name = "Bank Utama", type = AccountType.BANK, initialBalance = 1_000_000L)
        val wallet = AccountEntity(id = 2L, name = "E-Wallet", type = AccountType.E_WALLET, initialBalance = 200_000L)
        val now = System.currentTimeMillis()

        val txs = listOf(
            // Income +3.000.000 to Bank
            TransactionEntity(id = 1L, type = TransactionType.INCOME, amount = 3_000_000L, accountId = 1L, timestamp = now, title = "Gaji"),
            // Expense -500.000 from Bank
            TransactionEntity(id = 2L, type = TransactionType.EXPENSE, amount = 500_000L, categoryId = 10L, accountId = 1L, timestamp = now, title = "Makan"),
            // Transfer 400.000 from Bank to E-Wallet
            TransactionEntity(
                id = 3L,
                type = TransactionType.TRANSFER,
                amount = 400_000L,
                accountId = 1L,
                destinationAccountId = 2L,
                timestamp = now,
                title = "Topup E-Wallet"
            )
        )

        val balances = FinanceCalculator.calculateAccountBalances(listOf(bank, wallet), txs)
        val bankSummary = balances.first { it.account.id == 1L }
        val walletSummary = balances.first { it.account.id == 2L }

        // Bank: 1.000.000 + 3.000.000 - 500.000 - 400.000 = 3.100.000
        assertEquals(3_100_000L, bankSummary.currentBalance)
        // Wallet: 200.000 + 400.000 = 600.000
        assertEquals(600_000L, walletSummary.currentBalance)

        // Cash flow: Transfer is NOT income or expense!
        val (income, expense, netCashFlow) = FinanceCalculator.calculatePeriodCashFlow(txs)
        assertEquals(3_000_000L, income)
        assertEquals(500_000L, expense)
        assertEquals(2_500_000L, netCashFlow)

        // Budget calculation: Transfer is NOT budget spending!
        val ym = FinanceCalculator.currentYearMonth(now)
        val budgets = listOf(
            BudgetEntity(id = 1L, categoryId = null, yearMonth = ym, amountLimit = 1_000_000L),
            BudgetEntity(id = 2L, categoryId = 10L, yearMonth = ym, amountLimit = 400_000L)
        )
        val categories = listOf(CategoryEntity(id = 10L, name = "Makanan", type = CategoryType.EXPENSE))
        val budgetSummaries = FinanceCalculator.calculateBudgetSummaries(budgets, categories, txs, ym)
        val totalBudget = budgetSummaries.first { it.budget.categoryId == null }
        val foodBudget = budgetSummaries.first { it.budget.categoryId == 10L }

        assertEquals(500_000L, totalBudget.spentAmount)
        assertEquals(50, totalBudget.usagePercentage)
        assertFalse(totalBudget.isExceeded)

        assertEquals(500_000L, foodBudget.spentAmount)
        assertTrue(foodBudget.isExceeded)

        // Net worth calculation without double counting account balances
        val assets = listOf(AssetEntity(id = 1L, name = "Motor", type = AssetType.KENDARAAN, value = 15_000_000L))
        val liabilities = listOf(LiabilityEntity(id = 1L, name = "Cicilan", type = LiabilityType.CICILAN, amount = 2_000_000L))
        val debts = listOf(
            DebtEntity(id = 1L, type = DebtType.DEBT, personName = "Budi", principalAmount = 1_000_000L, remainingPrincipal = 600_000L),
            DebtEntity(id = 2L, type = DebtType.RECEIVABLE, personName = "Siti", principalAmount = 500_000L, remainingPrincipal = 500_000L)
        )
        val nw = FinanceCalculator.calculateNetWorth(balances, assets, liabilities, debts)
        // Active accounts = 3.100.000 + 600.000 = 3.700.000
        assertEquals(3_700_000L, nw.activeAccountsTotal)
        // Total assets = 3.700.000 + 15.000.000 + 500.000 = 19.200.000
        assertEquals(19_200_000L, nw.totalAssets)
        // Total liabilities = 2.000.000 + 600.000 = 2.600.000
        assertEquals(2_600_000L, nw.totalLiabilities)
        // Net worth = 19.200.000 - 2.600.000 = 16.600.000
        assertEquals(16_600_000L, nw.netWorth)
    }

    @Test
    fun `corrupted or unsupported backup JSON is rejected with clear error message`() {
        val emptyRes = BackupManager.validateAndParseBackup("")
        assertTrue(emptyRes is BackupValidationResult.Invalid)

        val brokenJsonRes = BackupManager.validateAndParseBackup("{invalid_json: true")
        assertTrue(brokenJsonRes is BackupValidationResult.Invalid)

        val wrongVersionJson = """
            {
              "backupVersion": 999,
              "appIdentifier": "LEOVANEY_ANDROID_NATIVE"
            }
        """.trimIndent()
        val versionRes = BackupManager.validateAndParseBackup(wrongVersionJson)
        assertTrue(versionRes is BackupValidationResult.Invalid)
        assertTrue((versionRes as BackupValidationResult.Invalid).errorMessage.contains("Versi format backup"))
    }
}
