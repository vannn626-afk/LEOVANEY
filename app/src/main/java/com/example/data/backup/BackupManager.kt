package com.example.data.backup

import androidx.room.withTransaction
import com.example.data.local.AccountEntity
import com.example.data.local.AccountType
import com.example.data.local.AssetEntity
import com.example.data.local.AssetType
import com.example.data.local.BillCategoryType
import com.example.data.local.BillEntity
import com.example.data.local.BudgetEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.CategoryType
import com.example.data.local.DebtEntity
import com.example.data.local.DebtPaymentEntity
import com.example.data.local.DebtType
import com.example.data.local.LeovaneyDatabase
import com.example.data.local.LiabilityEntity
import com.example.data.local.LiabilityType
import com.example.data.local.RecurrenceFrequency
import com.example.data.local.RecurringTransactionEntity
import com.example.data.local.SavingsContributionEntity
import com.example.data.local.SavingsGoalEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.data.preferences.UserPreferencesRepository
import com.example.domain.FinanceCalculator
import org.json.JSONArray
import org.json.JSONObject

enum class RestoreMode(val label: String, val description: String) {
    REPLACE(
        "Ganti Semua Data",
        "Menghapus seluruh data lokal saat ini dan menggantinya secara utuh dengan isi file backup."
    ),
    MERGE(
        "Gabungkan Data",
        "Mempertahankan data lokal yang ada dan menggabungkan entri dari file backup."
    )
}

data class BackupPayload(
    val backupVersion: Int,
    val appIdentifier: String,
    val exportedAt: Long,
    val userName: String,
    val currencyCode: String,
    val accounts: List<AccountEntity>,
    val categories: List<CategoryEntity>,
    val transactions: List<TransactionEntity>,
    val budgets: List<BudgetEntity>,
    val savingsGoals: List<SavingsGoalEntity>,
    val savingsContributions: List<SavingsContributionEntity>,
    val bills: List<BillEntity>,
    val recurringTransactions: List<RecurringTransactionEntity>,
    val debts: List<DebtEntity>,
    val debtPayments: List<DebtPaymentEntity>,
    val assets: List<AssetEntity>,
    val liabilities: List<LiabilityEntity>
)

sealed class BackupValidationResult {
    data class Valid(val payload: BackupPayload) : BackupValidationResult()
    data class Invalid(val errorMessage: String) : BackupValidationResult()
}

object BackupManager {
    const val CURRENT_BACKUP_VERSION = 1
    const val APP_IDENTIFIER = "LEOVANEY_ANDROID_NATIVE"

    suspend fun createBackupJson(
        database: LeovaneyDatabase,
        preferencesRepository: UserPreferencesRepository
    ): String {
        val dao = database.dao()
        val prefs = preferencesRepository.getSnapshot()
        val payload = BackupPayload(
            backupVersion = CURRENT_BACKUP_VERSION,
            appIdentifier = APP_IDENTIFIER,
            exportedAt = System.currentTimeMillis(),
            userName = prefs.userName,
            currencyCode = prefs.currencyCode,
            accounts = dao.getAllAccountsOnce(),
            categories = dao.getAllCategoriesOnce(),
            transactions = dao.getAllTransactionsOnce(),
            budgets = dao.getAllBudgetsOnce(),
            savingsGoals = dao.getAllSavingsGoalsOnce(),
            savingsContributions = dao.getAllSavingsContributionsOnce(),
            bills = dao.getAllBillsOnce(),
            recurringTransactions = dao.getAllRecurringTransactionsOnce(),
            debts = dao.getAllDebtsOnce(),
            debtPayments = dao.getAllDebtPaymentsOnce(),
            assets = dao.getAllAssetsOnce(),
            liabilities = dao.getAllLiabilitiesOnce()
        )
        return serializePayload(payload)
    }

    fun serializePayload(payload: BackupPayload): String {
        val root = JSONObject()
        root.put("backupVersion", payload.backupVersion)
        root.put("appIdentifier", payload.appIdentifier)
        root.put("exportedAt", payload.exportedAt)
        root.put("userName", payload.userName)
        root.put("currencyCode", payload.currencyCode)

        root.put("accounts", JSONArray().apply {
            payload.accounts.forEach { a ->
                put(JSONObject().apply {
                    put("id", a.id)
                    put("name", a.name)
                    put("type", a.type.name)
                    put("initialBalance", a.initialBalance)
                    put("createdAt", a.createdAt)
                    put("note", a.note)
                    put("isArchived", a.isArchived)
                })
            }
        })

        root.put("categories", JSONArray().apply {
            payload.categories.forEach { c ->
                put(JSONObject().apply {
                    put("id", c.id)
                    put("name", c.name)
                    put("type", c.type.name)
                    put("iconName", c.iconName)
                    put("isArchived", c.isArchived)
                })
            }
        })

        root.put("transactions", JSONArray().apply {
            payload.transactions.forEach { t ->
                put(JSONObject().apply {
                    put("id", t.id)
                    put("type", t.type.name)
                    put("amount", t.amount)
                    if (t.categoryId != null) put("categoryId", t.categoryId) else put("categoryId", JSONObject.NULL)
                    put("accountId", t.accountId)
                    if (t.destinationAccountId != null) put("destinationAccountId", t.destinationAccountId) else put("destinationAccountId", JSONObject.NULL)
                    put("timestamp", t.timestamp)
                    put("title", t.title)
                    put("note", t.note)
                    if (t.recurringId != null) put("recurringId", t.recurringId) else put("recurringId", JSONObject.NULL)
                    if (t.billId != null) put("billId", t.billId) else put("billId", JSONObject.NULL)
                })
            }
        })

        root.put("budgets", JSONArray().apply {
            payload.budgets.forEach { b ->
                put(JSONObject().apply {
                    put("id", b.id)
                    if (b.categoryId != null) put("categoryId", b.categoryId) else put("categoryId", JSONObject.NULL)
                    put("yearMonth", b.yearMonth)
                    put("amountLimit", b.amountLimit)
                    put("alertThresholdPercent", b.alertThresholdPercent)
                    put("note", b.note)
                })
            }
        })

        root.put("savingsGoals", JSONArray().apply {
            payload.savingsGoals.forEach { g ->
                put(JSONObject().apply {
                    put("id", g.id)
                    put("name", g.name)
                    put("targetAmount", g.targetAmount)
                    put("currentAmount", g.currentAmount)
                    if (g.deadlineTimestamp != null) put("deadlineTimestamp", g.deadlineTimestamp) else put("deadlineTimestamp", JSONObject.NULL)
                    if (g.linkedAccountId != null) put("linkedAccountId", g.linkedAccountId) else put("linkedAccountId", JSONObject.NULL)
                    put("note", g.note)
                    put("isCompleted", g.isCompleted)
                    put("createdAt", g.createdAt)
                })
            }
        })

        root.put("savingsContributions", JSONArray().apply {
            payload.savingsContributions.forEach { sc ->
                put(JSONObject().apply {
                    put("id", sc.id)
                    put("goalId", sc.goalId)
                    put("amount", sc.amount)
                    put("isWithdrawal", sc.isWithdrawal)
                    put("timestamp", sc.timestamp)
                    put("note", sc.note)
                })
            }
        })

        root.put("bills", JSONArray().apply {
            payload.bills.forEach { bill ->
                put(JSONObject().apply {
                    put("id", bill.id)
                    put("name", bill.name)
                    put("categoryType", bill.categoryType.name)
                    if (bill.categoryId != null) put("categoryId", bill.categoryId) else put("categoryId", JSONObject.NULL)
                    put("amount", bill.amount)
                    put("dueDate", bill.dueDate)
                    put("frequency", bill.frequency.name)
                    put("isPaid", bill.isPaid)
                    if (bill.lastPaidAt != null) put("lastPaidAt", bill.lastPaidAt) else put("lastPaidAt", JSONObject.NULL)
                    put("note", bill.note)
                })
            }
        })

        root.put("recurringTransactions", JSONArray().apply {
            payload.recurringTransactions.forEach { rt ->
                put(JSONObject().apply {
                    put("id", rt.id)
                    put("title", rt.title)
                    put("type", rt.type.name)
                    put("amount", rt.amount)
                    if (rt.categoryId != null) put("categoryId", rt.categoryId) else put("categoryId", JSONObject.NULL)
                    put("accountId", rt.accountId)
                    if (rt.destinationAccountId != null) put("destinationAccountId", rt.destinationAccountId) else put("destinationAccountId", JSONObject.NULL)
                    put("frequency", rt.frequency.name)
                    put("nextRunDate", rt.nextRunDate)
                    if (rt.lastProcessedDate != null) put("lastProcessedDate", rt.lastProcessedDate) else put("lastProcessedDate", JSONObject.NULL)
                    put("isActive", rt.isActive)
                    put("note", rt.note)
                })
            }
        })

        root.put("debts", JSONArray().apply {
            payload.debts.forEach { d ->
                put(JSONObject().apply {
                    put("id", d.id)
                    put("type", d.type.name)
                    put("personName", d.personName)
                    put("principalAmount", d.principalAmount)
                    put("interestOrFeeAmount", d.interestOrFeeAmount)
                    put("remainingPrincipal", d.remainingPrincipal)
                    put("startDate", d.startDate)
                    if (d.dueDate != null) put("dueDate", d.dueDate) else put("dueDate", JSONObject.NULL)
                    put("isSettled", d.isSettled)
                    put("note", d.note)
                })
            }
        })

        root.put("debtPayments", JSONArray().apply {
            payload.debtPayments.forEach { dp ->
                put(JSONObject().apply {
                    put("id", dp.id)
                    put("debtId", dp.debtId)
                    put("principalPaid", dp.principalPaid)
                    put("interestOrFeePaid", dp.interestOrFeePaid)
                    if (dp.accountId != null) put("accountId", dp.accountId) else put("accountId", JSONObject.NULL)
                    put("timestamp", dp.timestamp)
                    put("note", dp.note)
                })
            }
        })

        root.put("assets", JSONArray().apply {
            payload.assets.forEach { a ->
                put(JSONObject().apply {
                    put("id", a.id)
                    put("name", a.name)
                    put("type", a.type.name)
                    put("value", a.value)
                    put("acquiredDate", a.acquiredDate)
                    put("note", a.note)
                })
            }
        })

        root.put("liabilities", JSONArray().apply {
            payload.liabilities.forEach { l ->
                put(JSONObject().apply {
                    put("id", l.id)
                    put("name", l.name)
                    put("type", l.type.name)
                    put("amount", l.amount)
                    if (l.dueDate != null) put("dueDate", l.dueDate) else put("dueDate", JSONObject.NULL)
                    put("note", l.note)
                })
            }
        })

        return root.toString(2)
    }

    fun validateAndParseBackup(rawJson: String): BackupValidationResult {
        if (rawJson.isBlank()) {
            return BackupValidationResult.Invalid("File backup kosong atau tidak terbaca.")
        }
        return try {
            val root = JSONObject(rawJson)
            if (!root.has("backupVersion") || !root.has("appIdentifier")) {
                return BackupValidationResult.Invalid("Format file bukan backup LEOVANEY yang valid.")
            }
            val version = root.getInt("backupVersion")
            if (version < 1 || version > CURRENT_BACKUP_VERSION) {
                return BackupValidationResult.Invalid(
                    "Versi format backup ($version) tidak didukung oleh aplikasi ini (maksimal versi $CURRENT_BACKUP_VERSION)."
                )
            }
            val identifier = root.optString("appIdentifier", "")
            if (identifier != APP_IDENTIFIER) {
                return BackupValidationResult.Invalid("Identitas aplikasi pada file backup tidak cocok ($identifier).")
            }

            val userName = root.optString("userName", "van").trim().ifEmpty { "van" }
            val currencyCode = root.optString("currencyCode", "IDR").trim().ifEmpty { "IDR" }
            val exportedAt = root.optLong("exportedAt", System.currentTimeMillis())

            fun JSONObject.optNullableLong(key: String): Long? =
                if (isNull(key) || !has(key)) null else getLong(key)

            val accountsArray = root.optJSONArray("accounts") ?: JSONArray()
            val accounts = (0 until accountsArray.length()).map { i ->
                val obj = accountsArray.getJSONObject(i)
                AccountEntity(
                    id = obj.getLong("id"),
                    name = obj.getString("name"),
                    type = AccountType.valueOf(obj.getString("type")),
                    initialBalance = obj.getLong("initialBalance"),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    note = obj.optString("note", ""),
                    isArchived = obj.optBoolean("isArchived", false)
                )
            }

            val categoriesArray = root.optJSONArray("categories") ?: JSONArray()
            val categories = (0 until categoriesArray.length()).map { i ->
                val obj = categoriesArray.getJSONObject(i)
                CategoryEntity(
                    id = obj.getLong("id"),
                    name = obj.getString("name"),
                    type = CategoryType.valueOf(obj.getString("type")),
                    iconName = obj.optString("iconName", "category"),
                    isArchived = obj.optBoolean("isArchived", false)
                )
            }

            val transactionsArray = root.optJSONArray("transactions") ?: JSONArray()
            val transactions = (0 until transactionsArray.length()).map { i ->
                val obj = transactionsArray.getJSONObject(i)
                val amount = obj.getLong("amount")
                if (amount <= 0L) {
                    return BackupValidationResult.Invalid("Ditemukan transaksi dengan nominal tidak valid (<= 0) pada file backup.")
                }
                TransactionEntity(
                    id = obj.getLong("id"),
                    type = TransactionType.valueOf(obj.getString("type")),
                    amount = amount,
                    categoryId = obj.optNullableLong("categoryId"),
                    accountId = obj.getLong("accountId"),
                    destinationAccountId = obj.optNullableLong("destinationAccountId"),
                    timestamp = obj.getLong("timestamp"),
                    title = obj.getString("title"),
                    note = obj.optString("note", ""),
                    recurringId = obj.optNullableLong("recurringId"),
                    billId = obj.optNullableLong("billId")
                )
            }

            val budgetsArray = root.optJSONArray("budgets") ?: JSONArray()
            val budgets = (0 until budgetsArray.length()).map { i ->
                val obj = budgetsArray.getJSONObject(i)
                BudgetEntity(
                    id = obj.getLong("id"),
                    categoryId = obj.optNullableLong("categoryId"),
                    yearMonth = obj.getString("yearMonth"),
                    amountLimit = obj.getLong("amountLimit"),
                    alertThresholdPercent = obj.optInt("alertThresholdPercent", 80),
                    note = obj.optString("note", "")
                )
            }

            val goalsArray = root.optJSONArray("savingsGoals") ?: JSONArray()
            val savingsGoals = (0 until goalsArray.length()).map { i ->
                val obj = goalsArray.getJSONObject(i)
                SavingsGoalEntity(
                    id = obj.getLong("id"),
                    name = obj.getString("name"),
                    targetAmount = obj.getLong("targetAmount"),
                    currentAmount = obj.getLong("currentAmount"),
                    deadlineTimestamp = obj.optNullableLong("deadlineTimestamp"),
                    linkedAccountId = obj.optNullableLong("linkedAccountId"),
                    note = obj.optString("note", ""),
                    isCompleted = obj.optBoolean("isCompleted", false),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
            }

            val contribArray = root.optJSONArray("savingsContributions") ?: JSONArray()
            val savingsContributions = (0 until contribArray.length()).map { i ->
                val obj = contribArray.getJSONObject(i)
                SavingsContributionEntity(
                    id = obj.getLong("id"),
                    goalId = obj.getLong("goalId"),
                    amount = obj.getLong("amount"),
                    isWithdrawal = obj.optBoolean("isWithdrawal", false),
                    timestamp = obj.getLong("timestamp"),
                    note = obj.optString("note", "")
                )
            }

            val billsArray = root.optJSONArray("bills") ?: JSONArray()
            val bills = (0 until billsArray.length()).map { i ->
                val obj = billsArray.getJSONObject(i)
                BillEntity(
                    id = obj.getLong("id"),
                    name = obj.getString("name"),
                    categoryType = BillCategoryType.valueOf(obj.getString("categoryType")),
                    categoryId = obj.optNullableLong("categoryId"),
                    amount = obj.getLong("amount"),
                    dueDate = obj.getLong("dueDate"),
                    frequency = RecurrenceFrequency.valueOf(obj.getString("frequency")),
                    isPaid = obj.optBoolean("isPaid", false),
                    lastPaidAt = obj.optNullableLong("lastPaidAt"),
                    note = obj.optString("note", "")
                )
            }

            val recArray = root.optJSONArray("recurringTransactions") ?: JSONArray()
            val recurringTransactions = (0 until recArray.length()).map { i ->
                val obj = recArray.getJSONObject(i)
                RecurringTransactionEntity(
                    id = obj.getLong("id"),
                    title = obj.getString("title"),
                    type = TransactionType.valueOf(obj.getString("type")),
                    amount = obj.getLong("amount"),
                    categoryId = obj.optNullableLong("categoryId"),
                    accountId = obj.getLong("accountId"),
                    destinationAccountId = obj.optNullableLong("destinationAccountId"),
                    frequency = RecurrenceFrequency.valueOf(obj.getString("frequency")),
                    nextRunDate = obj.getLong("nextRunDate"),
                    lastProcessedDate = obj.optNullableLong("lastProcessedDate"),
                    isActive = obj.optBoolean("isActive", true),
                    note = obj.optString("note", "")
                )
            }

            val debtsArray = root.optJSONArray("debts") ?: JSONArray()
            val debts = (0 until debtsArray.length()).map { i ->
                val obj = debtsArray.getJSONObject(i)
                DebtEntity(
                    id = obj.getLong("id"),
                    type = DebtType.valueOf(obj.getString("type")),
                    personName = obj.getString("personName"),
                    principalAmount = obj.getLong("principalAmount"),
                    interestOrFeeAmount = obj.optLong("interestOrFeeAmount", 0L),
                    remainingPrincipal = obj.getLong("remainingPrincipal"),
                    startDate = obj.getLong("startDate"),
                    dueDate = obj.optNullableLong("dueDate"),
                    isSettled = obj.optBoolean("isSettled", false),
                    note = obj.optString("note", "")
                )
            }

            val debtPaymentsArray = root.optJSONArray("debtPayments") ?: JSONArray()
            val debtPayments = (0 until debtPaymentsArray.length()).map { i ->
                val obj = debtPaymentsArray.getJSONObject(i)
                DebtPaymentEntity(
                    id = obj.getLong("id"),
                    debtId = obj.getLong("debtId"),
                    principalPaid = obj.getLong("principalPaid"),
                    interestOrFeePaid = obj.optLong("interestOrFeePaid", 0L),
                    accountId = obj.optNullableLong("accountId"),
                    timestamp = obj.getLong("timestamp"),
                    note = obj.optString("note", "")
                )
            }

            val assetsArray = root.optJSONArray("assets") ?: JSONArray()
            val assets = (0 until assetsArray.length()).map { i ->
                val obj = assetsArray.getJSONObject(i)
                AssetEntity(
                    id = obj.getLong("id"),
                    name = obj.getString("name"),
                    type = AssetType.valueOf(obj.getString("type")),
                    value = obj.getLong("value"),
                    acquiredDate = obj.getLong("acquiredDate"),
                    note = obj.optString("note", "")
                )
            }

            val liabilitiesArray = root.optJSONArray("liabilities") ?: JSONArray()
            val liabilities = (0 until liabilitiesArray.length()).map { i ->
                val obj = liabilitiesArray.getJSONObject(i)
                LiabilityEntity(
                    id = obj.getLong("id"),
                    name = obj.getString("name"),
                    type = LiabilityType.valueOf(obj.getString("type")),
                    amount = obj.getLong("amount"),
                    dueDate = obj.optNullableLong("dueDate"),
                    note = obj.optString("note", "")
                )
            }

            BackupValidationResult.Valid(
                BackupPayload(
                    backupVersion = version,
                    appIdentifier = identifier,
                    exportedAt = exportedAt,
                    userName = userName,
                    currencyCode = currencyCode,
                    accounts = accounts,
                    categories = categories,
                    transactions = transactions,
                    budgets = budgets,
                    savingsGoals = savingsGoals,
                    savingsContributions = savingsContributions,
                    bills = bills,
                    recurringTransactions = recurringTransactions,
                    debts = debts,
                    debtPayments = debtPayments,
                    assets = assets,
                    liabilities = liabilities
                )
            )
        } catch (e: Exception) {
            BackupValidationResult.Invalid("File backup rusak atau tidak sesuai struktur JSON LEOVANEY: ${e.message ?: "Kesalahan parsing"}")
        }
    }

    suspend fun restoreBackup(
        database: LeovaneyDatabase,
        preferencesRepository: UserPreferencesRepository,
        payload: BackupPayload,
        mode: RestoreMode
    ): Result<Unit> {
        return runCatching {
            val dao = database.dao()
            database.withTransaction {
                if (mode == RestoreMode.REPLACE) {
                    dao.deleteAllFinancialData()
                }
                if (payload.accounts.isNotEmpty()) dao.insertAccounts(payload.accounts)
                if (payload.categories.isNotEmpty()) dao.insertCategories(payload.categories)
                if (payload.transactions.isNotEmpty()) dao.insertTransactions(payload.transactions)
                if (payload.budgets.isNotEmpty()) dao.insertBudgets(payload.budgets)
                if (payload.savingsGoals.isNotEmpty()) dao.insertSavingsGoals(payload.savingsGoals)
                if (payload.savingsContributions.isNotEmpty()) dao.insertSavingsContributions(payload.savingsContributions)
                if (payload.bills.isNotEmpty()) dao.insertBills(payload.bills)
                if (payload.recurringTransactions.isNotEmpty()) dao.insertRecurringTransactions(payload.recurringTransactions)
                if (payload.debts.isNotEmpty()) dao.insertDebts(payload.debts)
                if (payload.debtPayments.isNotEmpty()) dao.insertDebtPayments(payload.debtPayments)
                if (payload.assets.isNotEmpty()) dao.insertAssets(payload.assets)
                if (payload.liabilities.isNotEmpty()) dao.insertLiabilities(payload.liabilities)
            }
            preferencesRepository.saveUserNameAndCompleteOnboarding(payload.userName)
            preferencesRepository.updateCurrencyCode(payload.currencyCode)
        }
    }

    fun generateTransactionsCsv(
        transactions: List<TransactionEntity>,
        accounts: List<AccountEntity>,
        categories: List<CategoryEntity>
    ): String {
        val accountMap = accounts.associateBy { it.id }
        val categoryMap = categories.associateBy { it.id }
        val sb = StringBuilder()
        sb.appendLine("ID,Tanggal,Jenis,Nominal (IDR),Keterangan,Kategori,Rekening Sumber,Rekening Tujuan,Catatan")
        for (tx in transactions) {
            val dateStr = FinanceCalculator.formatDateTime(tx.timestamp).replace(",", "")
            val typeStr = tx.type.label
            val catStr = tx.categoryId?.let { categoryMap[it]?.name } ?: "-"
            val accStr = accountMap[tx.accountId]?.name ?: "Rekening #${tx.accountId}"
            val destStr = tx.destinationAccountId?.let { accountMap[it]?.name ?: "Rekening #$it" } ?: "-"
            val cleanTitle = tx.title.replace("\"", "\"\"")
            val cleanNote = tx.note.replace("\"", "\"\"")
            sb.appendLine("${tx.id},\"$dateStr\",\"$typeStr\",${tx.amount},\"$cleanTitle\",\"$catStr\",\"$accStr\",\"$destStr\",\"$cleanNote\"")
        }
        return sb.toString()
    }
}
