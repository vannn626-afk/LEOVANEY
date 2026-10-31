package com.example

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.backup.BackupValidationResult
import com.example.data.backup.RestoreMode
import com.example.data.local.BudgetEntity
import com.example.data.local.LeovaneyDatabase
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.LeovaneyRepository
import com.example.domain.FinanceCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var database: LeovaneyDatabase
    private lateinit var prefsRepository: UserPreferencesRepository
    private lateinit var repository: LeovaneyRepository
    private lateinit var tempPrefsFile: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, LeovaneyDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        tempPrefsFile = File(context.cacheDir, "test_prefs_${UUID.randomUUID()}.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
            produceFile = { tempPrefsFile }
        )
        prefsRepository = UserPreferencesRepository(dataStore)
        repository = LeovaneyRepository(database, prefsRepository)
    }

    @After
    fun tearDown() {
        database.close()
        if (tempPrefsFile.exists()) {
            tempPrefsFile.delete()
        }
    }

    @Test
    fun `app_name resource is LEOVANEY`() {
        assertEquals("LEOVANEY", context.getString(R.string.app_name))
    }

    @Test
    fun `first install defaults to van and onboarding flow persists profile across restarts`() = runBlocking {
        // 1. Instalasi pertama menampilkan nama default "van" dan onboarding belum selesai
        val initialPrefs = prefsRepository.getSnapshot()
        assertEquals("van", initialPrefs.userName)
        assertFalse(initialPrefs.isOnboardingCompleted)

        // Tolak nama kosong setelah trim
        val emptyResult = repository.completeOnboarding("   ")
        assertTrue(emptyResult.isFailure)

        // Simpan nama "van" dan tandai onboarding selesai
        val saveVanResult = repository.completeOnboarding("van")
        assertTrue(saveVanResult.isSuccess)

        val afterOnboarding = prefsRepository.getSnapshot()
        assertEquals("van", afterOnboarding.userName)
        assertTrue(afterOnboarding.isOnboardingCompleted)

        // 2. Mengubah nama menjadi nama lain menyimpan perubahan tanpa mengulang onboarding
        val renameResult = repository.updateUserName("Leo Van")
        assertTrue(renameResult.isSuccess)

        val afterRename = prefsRepository.getSnapshot()
        assertEquals("Leo Van", afterRename.userName)
        assertTrue(afterRename.isOnboardingCompleted)
    }

    @Test
    fun `transactions transfers edits deletions backup restore and reset work end to end`() = runBlocking {
        repository.initializeDefaultsAndProcessRecurring()
        repository.completeOnboarding("van")

        val accounts = repository.accountsFlow.first()
        assertEquals(3, accounts.size)
        val cashAccount = accounts[0]
        val bankAccount = accounts[1]

        // 5. Transaksi pemasukan memperbarui saldo
        val incomeRes = repository.saveTransaction(
            TransactionEntity(
                type = TransactionType.INCOME,
                amount = 5_000_000L,
                accountId = bankAccount.id,
                title = "Gaji Bulanan"
            )
        )
        assertTrue(incomeRes.isSuccess)
        val incomeId = incomeRes.getOrThrow()

        // 6. Transaksi pengeluaran memperbarui saldo dan anggaran
        val categories = repository.categoriesFlow.first()
        val foodCategory = categories.first { it.name == "Makanan" }
        val ym = FinanceCalculator.currentYearMonth()
        repository.saveBudget(
            BudgetEntity(
                categoryId = foodCategory.id,
                yearMonth = ym,
                amountLimit = 1_000_000L
            )
        )
        val expenseRes = repository.saveTransaction(
            TransactionEntity(
                type = TransactionType.EXPENSE,
                amount = 300_000L,
                categoryId = foodCategory.id,
                accountId = bankAccount.id,
                title = "Belanja Makanan"
            )
        )
        assertTrue(expenseRes.isSuccess)
        val expenseId = expenseRes.getOrThrow()

        // 7. Transfer memperbarui dua rekening dengan benar
        val transferRes = repository.saveTransaction(
            TransactionEntity(
                type = TransactionType.TRANSFER,
                amount = 700_000L,
                accountId = bankAccount.id,
                destinationAccountId = cashAccount.id,
                title = "Tarik Tunai ATM"
            )
        )
        assertTrue(transferRes.isSuccess)

        var allTxs = repository.transactionsFlow.first()
        var balances = FinanceCalculator.calculateAccountBalances(accounts, allTxs)
        // Bank: 5.000.000 - 300.000 - 700.000 = 4.000.000
        assertEquals(4_000_000L, balances.first { it.account.id == bankAccount.id }.currentBalance)
        // Cash: 0 + 700.000 = 700.000
        assertEquals(700_000L, balances.first { it.account.id == cashAccount.id }.currentBalance)

        // 8. Mengedit atau menghapus transaksi memperbarui laporan
        val existingExpense = allTxs.first { it.id == expenseId }
        repository.saveTransaction(existingExpense.copy(amount = 450_000L))
        allTxs = repository.transactionsFlow.first()
        val (_, updatedExpenseTotal, updatedNet) = FinanceCalculator.calculatePeriodCashFlow(allTxs)
        assertEquals(450_000L, updatedExpenseTotal)
        assertEquals(4_550_000L, updatedNet)

        // 10. Backup dapat diekspor dan dipulihkan
        val backupJson = repository.exportBackupJson()
        val validation = repository.validateBackupJson(backupJson)
        assertTrue(validation is BackupValidationResult.Valid)

        // 12. Reset data menghapus transaksi tetapi mempertahankan profil
        repository.resetAllFinancialDataConfirmed()
        assertTrue(repository.transactionsFlow.first().isEmpty())
        assertEquals("van", prefsRepository.getSnapshot().userName)

        // Pulihkan kembali dari backup JSON
        val payload = (validation as BackupValidationResult.Valid).payload
        val restoreRes = repository.restoreFromBackup(payload, RestoreMode.REPLACE)
        assertTrue(restoreRes.isSuccess)

        val restoredTxs = repository.transactionsFlow.first()
        assertEquals(3, restoredTxs.size)
        val restoredBalances = FinanceCalculator.calculateAccountBalances(
            repository.accountsFlow.first(),
            restoredTxs
        )
        // Bank: 5.000.000 - 450.000 - 700.000 = 3.850.000
        assertEquals(3_850_000L, restoredBalances.first { it.account.id == bankAccount.id }.currentBalance)
    }
}
