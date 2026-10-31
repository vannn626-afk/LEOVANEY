package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class LeovaneyTypeConverters {
    @TypeConverter
    fun fromAccountType(value: AccountType): String = value.name

    @TypeConverter
    fun toAccountType(value: String): AccountType =
        runCatching { AccountType.valueOf(value) }.getOrDefault(AccountType.LAINNYA)

    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType =
        runCatching { TransactionType.valueOf(value) }.getOrDefault(TransactionType.EXPENSE)

    @TypeConverter
    fun fromCategoryType(value: CategoryType): String = value.name

    @TypeConverter
    fun toCategoryType(value: String): CategoryType =
        runCatching { CategoryType.valueOf(value) }.getOrDefault(CategoryType.EXPENSE)

    @TypeConverter
    fun fromBillCategoryType(value: BillCategoryType): String = value.name

    @TypeConverter
    fun toBillCategoryType(value: String): BillCategoryType =
        runCatching { BillCategoryType.valueOf(value) }.getOrDefault(BillCategoryType.LAINNYA)

    @TypeConverter
    fun fromRecurrenceFrequency(value: RecurrenceFrequency): String = value.name

    @TypeConverter
    fun toRecurrenceFrequency(value: String): RecurrenceFrequency =
        runCatching { RecurrenceFrequency.valueOf(value) }.getOrDefault(RecurrenceFrequency.MONTHLY)

    @TypeConverter
    fun fromDebtType(value: DebtType): String = value.name

    @TypeConverter
    fun toDebtType(value: String): DebtType =
        runCatching { DebtType.valueOf(value) }.getOrDefault(DebtType.DEBT)

    @TypeConverter
    fun fromAssetType(value: AssetType): String = value.name

    @TypeConverter
    fun toAssetType(value: String): AssetType =
        runCatching { AssetType.valueOf(value) }.getOrDefault(AssetType.LAINNYA)

    @TypeConverter
    fun fromLiabilityType(value: LiabilityType): String = value.name

    @TypeConverter
    fun toLiabilityType(value: String): LiabilityType =
        runCatching { LiabilityType.valueOf(value) }.getOrDefault(LiabilityType.LAINNYA)
}

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        SavingsGoalEntity::class,
        SavingsContributionEntity::class,
        BillEntity::class,
        RecurringTransactionEntity::class,
        DebtEntity::class,
        DebtPaymentEntity::class,
        AssetEntity::class,
        LiabilityEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(LeovaneyTypeConverters::class)
abstract class LeovaneyDatabase : RoomDatabase() {
    abstract fun dao(): LeovaneyDao

    companion object {
        @Volatile
        private var INSTANCE: LeovaneyDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Safe non-destructive forward migration placeholder for future schema versions
            }
        }

        fun getInstance(context: Context): LeovaneyDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LeovaneyDatabase::class.java,
                    "leovaney_finance.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }

        fun defaultCategories(): List<CategoryEntity> = listOf(
            // Pemasukan
            CategoryEntity(name = "Gaji", type = CategoryType.INCOME, iconName = "work"),
            CategoryEntity(name = "Bisnis", type = CategoryType.INCOME, iconName = "store"),
            CategoryEntity(name = "Freelance", type = CategoryType.INCOME, iconName = "laptop"),
            CategoryEntity(name = "Investasi", type = CategoryType.INCOME, iconName = "trending_up"),
            CategoryEntity(name = "Bonus", type = CategoryType.INCOME, iconName = "redeem"),
            CategoryEntity(name = "Lainnya", type = CategoryType.INCOME, iconName = "payments"),
            // Pengeluaran
            CategoryEntity(name = "Makanan", type = CategoryType.EXPENSE, iconName = "restaurant"),
            CategoryEntity(name = "Transportasi", type = CategoryType.EXPENSE, iconName = "directions_car"),
            CategoryEntity(name = "Belanja", type = CategoryType.EXPENSE, iconName = "shopping_bag"),
            CategoryEntity(name = "Tagihan", type = CategoryType.EXPENSE, iconName = "receipt_long"),
            CategoryEntity(name = "Tempat Tinggal", type = CategoryType.EXPENSE, iconName = "home"),
            CategoryEntity(name = "Kesehatan", type = CategoryType.EXPENSE, iconName = "medical_services"),
            CategoryEntity(name = "Pendidikan", type = CategoryType.EXPENSE, iconName = "school"),
            CategoryEntity(name = "Hiburan", type = CategoryType.EXPENSE, iconName = "movie"),
            CategoryEntity(name = "Langganan", type = CategoryType.EXPENSE, iconName = "subscriptions"),
            CategoryEntity(name = "Lainnya", type = CategoryType.EXPENSE, iconName = "category")
        )

        fun defaultAccounts(): List<AccountEntity> = listOf(
            AccountEntity(
                name = "Dompet Tunai",
                type = AccountType.TUNAI,
                initialBalance = 0L,
                note = "Rekening tunai utama"
            ),
            AccountEntity(
                name = "Rekening Bank Utama",
                type = AccountType.BANK,
                initialBalance = 0L,
                note = "Rekening bank harian"
            ),
            AccountEntity(
                name = "E-Wallet",
                type = AccountType.E_WALLET,
                initialBalance = 0L,
                note = "Dompet digital"
            )
        )
    }
}
