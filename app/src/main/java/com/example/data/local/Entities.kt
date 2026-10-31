package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class AccountType(val label: String) {
    TUNAI("Tunai"),
    BANK("Bank"),
    E_WALLET("E-Wallet"),
    TABUNGAN("Tabungan"),
    LAINNYA("Lainnya")
}

enum class TransactionType(val label: String) {
    INCOME("Pemasukan"),
    EXPENSE("Pengeluaran"),
    TRANSFER("Transfer")
}

enum class CategoryType(val label: String) {
    INCOME("Pemasukan"),
    EXPENSE("Pengeluaran")
}

enum class BillCategoryType(val label: String) {
    LISTRIK("Tagihan Listrik"),
    INTERNET("Internet"),
    AIR("Air"),
    SEWA("Sewa"),
    CICILAN("Cicilan"),
    LANGGANAN("Langganan"),
    LAINNYA("Tagihan Lainnya")
}

enum class RecurrenceFrequency(val label: String) {
    ONCE("Sekali"),
    DAILY("Harian"),
    WEEKLY("Mingguan"),
    MONTHLY("Bulanan"),
    YEARLY("Tahunan")
}

enum class DebtType(val label: String) {
    DEBT("Utang"),
    RECEIVABLE("Piutang")
}

enum class AssetType(val label: String) {
    KENDARAAN("Kendaraan"),
    PROPERTI("Properti"),
    INVESTASI_MANUAL("Investasi Manual"),
    PERALATAN("Peralatan"),
    LAINNYA("Aset Lainnya")
}

enum class LiabilityType(val label: String) {
    PINJAMAN("Pinjaman"),
    CICILAN("Cicilan"),
    LAINNYA("Kewajiban Lainnya")
}

enum class DateFilterPeriod(val label: String) {
    TODAY("Hari Ini"),
    LAST_7_DAYS("7 Hari Terakhir"),
    THIS_MONTH("Bulan Ini"),
    LAST_MONTH("Bulan Lalu"),
    THIS_YEAR("Tahun Ini"),
    CUSTOM("Rentang Khusus")
}

@Entity(
    tableName = "accounts",
    indices = [Index(value = ["isArchived"]), Index(value = ["type"])]
)
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: AccountType,
    val initialBalance: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val note: String = "",
    val isArchived: Boolean = false
)

@Entity(
    tableName = "categories",
    indices = [Index(value = ["type"]), Index(value = ["isArchived"])]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: CategoryType,
    val iconName: String = "category",
    val isArchived: Boolean = false
)

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["accountId"]),
        Index(value = ["destinationAccountId"]),
        Index(value = ["categoryId"]),
        Index(value = ["timestamp"]),
        Index(value = ["type"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: TransactionType,
    val amount: Long,
    val categoryId: Long? = null,
    val accountId: Long,
    val destinationAccountId: Long? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val title: String,
    val note: String = "",
    val recurringId: Long? = null,
    val billId: Long? = null
)

@Entity(
    tableName = "budgets",
    indices = [Index(value = ["yearMonth"]), Index(value = ["categoryId"])]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long? = null, // null = Total Anggaran Bulanan, non-null = Anggaran Kategori
    val yearMonth: String, // Format: "YYYY-MM"
    val amountLimit: Long,
    val alertThresholdPercent: Int = 80,
    val note: String = ""
)

@Entity(
    tableName = "savings_goals",
    indices = [Index(value = ["isCompleted"])]
)
data class SavingsGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val targetAmount: Long,
    val currentAmount: Long = 0L,
    val deadlineTimestamp: Long? = null,
    val linkedAccountId: Long? = null,
    val note: String = "",
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "savings_contributions",
    foreignKeys = [
        ForeignKey(
            entity = SavingsGoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["goalId"]), Index(value = ["timestamp"])]
)
data class SavingsContributionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goalId: Long,
    val amount: Long, // always > 0
    val isWithdrawal: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String = ""
)

@Entity(
    tableName = "bills",
    indices = [Index(value = ["dueDate"]), Index(value = ["isPaid"])]
)
data class BillEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val categoryType: BillCategoryType,
    val categoryId: Long? = null,
    val amount: Long,
    val dueDate: Long,
    val frequency: RecurrenceFrequency = RecurrenceFrequency.MONTHLY,
    val isPaid: Boolean = false,
    val lastPaidAt: Long? = null,
    val note: String = ""
)

@Entity(
    tableName = "recurring_transactions",
    indices = [Index(value = ["nextRunDate"]), Index(value = ["isActive"])]
)
data class RecurringTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val type: TransactionType,
    val amount: Long,
    val categoryId: Long? = null,
    val accountId: Long,
    val destinationAccountId: Long? = null,
    val frequency: RecurrenceFrequency = RecurrenceFrequency.MONTHLY,
    val nextRunDate: Long,
    val lastProcessedDate: Long? = null,
    val isActive: Boolean = true,
    val note: String = ""
)

@Entity(
    tableName = "debts",
    indices = [Index(value = ["type"]), Index(value = ["isSettled"]), Index(value = ["dueDate"])]
)
data class DebtEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: DebtType,
    val personName: String,
    val principalAmount: Long,
    val interestOrFeeAmount: Long = 0L,
    val remainingPrincipal: Long,
    val startDate: Long = System.currentTimeMillis(),
    val dueDate: Long? = null,
    val isSettled: Boolean = false,
    val note: String = ""
)

@Entity(
    tableName = "debt_payments",
    foreignKeys = [
        ForeignKey(
            entity = DebtEntity::class,
            parentColumns = ["id"],
            childColumns = ["debtId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["debtId"]), Index(value = ["timestamp"])]
)
data class DebtPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val debtId: Long,
    val principalPaid: Long,
    val interestOrFeePaid: Long = 0L,
    val accountId: Long? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String = ""
)

@Entity(
    tableName = "assets",
    indices = [Index(value = ["type"])]
)
data class AssetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: AssetType,
    val value: Long,
    val acquiredDate: Long = System.currentTimeMillis(),
    val note: String = ""
)

@Entity(
    tableName = "liabilities",
    indices = [Index(value = ["type"])]
)
data class LiabilityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: LiabilityType,
    val amount: Long,
    val dueDate: Long? = null,
    val note: String = ""
)
